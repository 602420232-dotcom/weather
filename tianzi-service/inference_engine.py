"""
TianZi Inference Engine

High-resolution weather analysis model inference.
"""

import logging
import os
import numpy as np

try:
    import onnxruntime as ort  # pyright: ignore[reportMissingImports]
except ImportError:
    ort = None

logger = logging.getLogger(__name__)


class TianZiEngine:
    """
    TianZi weather analysis model engine.
    """

    N_VARS = 12
    N_LAT = 361
    N_LON = 720
    N_LEVELS = 5
    MAX_RESOLUTION_KM = 1.0
    version = "1.0.0"

    def __init__(self, model_path=None):
        self.model_path = model_path or os.getenv("TIANZI_MODEL_PATH", "/app/model/tianzi.onnx")
        self._session = None
        self.is_loaded = False

    def load(self):
        """Load the TianZi model."""
        if ort is None:
            logger.error("ONNX Runtime not available")
            return

        try:
            providers = ['CPUExecutionProvider']
            if ort.get_device() == 'GPU':
                providers.insert(0, 'CUDAExecutionProvider')
            self._session = ort.InferenceSession(
                self.model_path,
                providers=providers
            )
            self.is_loaded = True
            logger.info(f"TianZi model loaded from {self.model_path}")
            logger.info(f"Using provider: {self._session.get_providers()[0]}")
        except Exception as e:
            logger.error(f"Failed to load TianZi model: {e}")
            self.is_loaded = False

    def analyze(self, observation_data, resolution_km=1.0) -> dict:
        """
        Run weather analysis on observation data.
        Args:
            observation_data: Input observation data (variables, lat, lon)
            resolution_km: Target resolution in km
        Returns:
            dict: Analysis results
        """
        if not self.is_loaded:
            raise RuntimeError("Model not loaded")

        assert self._session is not None, "Session should be loaded"
        try:
            input_name = self._session.get_inputs()[0].name
            output_names = [o.name for o in self._session.get_outputs()]
            outputs = self._session.run(output_names, {input_name: observation_data})
            result = {
                'u10': outputs[0][0],
                'v10': outputs[1][0],
                't2m': outputs[2][0],
                'msl': outputs[3][0],
                'rh2m': outputs[4][0],
                'precip': outputs[5][0],
            }
            return self._downscale(result, resolution_km)
        except Exception as e:
            logger.error(f"Analysis failed: {e}")
            raise

    def assimilate(self, observation_data, background_data, resolution_km=1.0) -> dict:
        """
        Run data assimilation.
        Args:
            observation_data: Observation data
            background_data: Background field data
            resolution_km: Target resolution in km
        Returns:
            dict: Assimilation results
        """
        if not self.is_loaded:
            raise RuntimeError("Model not loaded")

        try:
            result = self.analyze(observation_data, resolution_km)
            if background_data is not None:
                for key in result:
                    if key in ['u10', 'v10', 't2m', 'msl']:
                        bg_idx = self._get_background_index(key)
                        if bg_idx < background_data.shape[0]:
                            result[key] = 0.3 * result[key] + 0.7 * background_data[bg_idx]
            return result
        except Exception as e:
            logger.error(f"Assimilation failed: {e}")
            raise

    def predict(self, initial_data, steps=12, resolution_km=1.0) -> list:
        """
        Run forecast prediction.
        Args:
            initial_data: Initial condition data
            steps: Number of forecast steps
            resolution_km: Target resolution in km
        Returns:
            dict: Forecast results
        """
        if not self.is_loaded:
            raise RuntimeError("Model not loaded")

        result = self.analyze(initial_data, resolution_km)
        forecasts = []
        for i in range(steps):
            forecasts.append({k: v.copy() for k, v in result.items()})
        return forecasts

    def get_wind_field(self, data, resolution_km=1.0):
        """
        Extract wind field from analysis.
        Args:
            data: Input data
            resolution_km: Target resolution
        Returns:
            dict: Wind field data (u10, v10)
        """
        result = self.analyze(data, resolution_km)
        return {
            'u10': result['u10'],
            'v10': result['v10'],
        }

    def _downscale(self, data, target_resolution_km):
        """Downscale data to target resolution."""
        factor = int(self.MAX_RESOLUTION_KM / target_resolution_km)
        if factor <= 1:
            return data
        downscaled = {}
        for key, value in data.items():
            if isinstance(value, np.ndarray):
                downscaled[key] = np.kron(value, np.ones((factor, factor)))
            else:
                downscaled[key] = value
        return downscaled

    def _get_background_index(self, var_name):
        """Get background field index for variable."""
        indices = {'u10': 0, 'v10': 1, 't2m': 2, 'msl': 3, 'rh2m': 4, 'precip': 5}
        return indices.get(var_name, 0)

    def get_variable_units(self, var_name: str) -> str:
        """Get units for variable."""
        units = {
            'u10': 'm/s',
            'v10': 'm/s',
            't2m': 'K',
            'msl': 'Pa',
            'rh2m': '%',
            'precip': 'mm/h',
        }
        return units.get(var_name, 'unknown')

    def get_variable_description(self, var_name: str) -> str:
        """Get description for variable."""
        descriptions = {
            'u10': '10m U wind component',
            'v10': '10m V wind component',
            't2m': '2m temperature',
            'msl': 'Mean sea level pressure',
            'rh2m': '2m relative humidity',
            'precip': 'Precipitation rate',
        }
        return descriptions.get(var_name, var_name)

    def close(self):
        """Close the model session."""
        if self._session:
            del self._session
            self._session = None
            self.is_loaded = False
