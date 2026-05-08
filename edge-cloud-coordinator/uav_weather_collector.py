"""
无人机气象信息收集模块
对接多种气象数据源，为路径规划提供实时气象数据
"""
import logging
import time
from typing import Dict, List, Optional, Callable
from dataclasses import dataclass
from enum import Enum
from collections import deque

logger = logging.getLogger(__name__)


class WeatherSource(Enum):
    WRF = "wrf"
    UAV_SENSOR = "uav_sensor"
    GROUND_STATION = "ground_station"
    SATELLITE = "satellite"
    BUOY = "buoy"
    GHR = "ghr"


@dataclass
class WeatherRecord:
    source: WeatherSource
    drone_id: str
    timestamp: float
    latitude: float
    longitude: float
    altitude: float
    temperature: float
    humidity: float
    wind_speed: float
    wind_direction: float
    wind_gust: float
    pressure: float
    visibility: float
    turbulence: float
    precipitation: float
    data_quality: float = 1.0


class UAVWeatherCollector:
    """无人机气象数据收集器"""

    def __init__(self, drone_id: str):
        self.drone_id = drone_id
        self.buffer = deque(maxlen=10000)
        self.wrf_cache = {}
        self.sensor_cache = {}
        self.collection_interval = 1.0
        self._running = False
        self.callbacks: List[Callable] = []

    def on_data(self, callback: Callable):
        self.callbacks.append(callback)

    def collect_from_sensor(self, sensor_data: dict):
        """从机载传感器收集气象数据"""
        record = WeatherRecord(
            source=WeatherSource.UAV_SENSOR,
            drone_id=self.drone_id,
            timestamp=time.time(),
            latitude=sensor_data.get("latitude", 0),
            longitude=sensor_data.get("longitude", 0),
            altitude=sensor_data.get("altitude", 0),
            temperature=sensor_data.get("temperature", 25),
            humidity=sensor_data.get("humidity", 60),
            wind_speed=sensor_data.get("wind_speed", 0),
            wind_direction=sensor_data.get("wind_direction", 0),
            wind_gust=sensor_data.get("wind_gust", 0),
            pressure=sensor_data.get("pressure", 1013),
            visibility=sensor_data.get("visibility", 10),
            turbulence=sensor_data.get("turbulence", 0),
            precipitation=sensor_data.get("precipitation", 0),
            data_quality=0.9
        )
        self.buffer.append(record)
        self._notify(record)
        return record

    def collect_from_wrf(self, wrf_data: dict):
        """从WRF模型获取预报数据"""
        record = WeatherRecord(
            source=WeatherSource.WRF,
            drone_id=self.drone_id,
            timestamp=time.time(),
            latitude=wrf_data.get("latitude", 0),
            longitude=wrf_data.get("longitude", 0),
            altitude=wrf_data.get("altitude", 0),
            temperature=wrf_data.get("temperature", 25),
            humidity=wrf_data.get("humidity", 60),
            wind_speed=wrf_data.get("wind_speed", 0),
            wind_direction=wrf_data.get("wind_direction", 0),
            wind_gust=wrf_data.get("wind_gust", 0),
            pressure=wrf_data.get("pressure", 1013),
            visibility=wrf_data.get("visibility", 10),
            turbulence=wrf_data.get("turbulence", 0),
            precipitation=wrf_data.get("precipitation", 0),
            data_quality=0.8
        )
        self.wrf_cache[wrf_data.get("grid_point", "default")] = record
        self.buffer.append(record)
        self._notify(record)
        return record

    def _notify(self, record: WeatherRecord):
        for cb in self.callbacks:
            cb(record)

    def get_current_weather(self) -> Optional[dict]:
        if not self.buffer:
            return None
        latest = self.buffer[-1]
        return {
            "drone_id": self.drone_id,
            "temperature": latest.temperature,
            "humidity": latest.humidity,
            "wind_speed": latest.wind_speed,
            "wind_direction": latest.wind_direction,
            "wind_gust": latest.wind_gust,
            "pressure": latest.pressure,
            "visibility": latest.visibility,
            "turbulence": latest.turbulence,
            "precipitation": latest.precipitation,
            "timestamp": latest.timestamp
        }

    def get_weather_history(self, minutes: int = 10) -> List[dict]:
        cutoff = time.time() - minutes * 60
        return [self._to_dict(r) for r in self.buffer if r.timestamp > cutoff]

    def _to_dict(self, r: WeatherRecord) -> dict:
        return {
            "source": r.source.value, "drone_id": r.drone_id,
            "timestamp": r.timestamp, "temperature": r.temperature,
            "wind_speed": r.wind_speed, "humidity": r.humidity,
            "data_quality": r.data_quality
        }


class MultiSourceFusion:
    """多源气象数据融合"""

    def __init__(self):
        self.collectors: Dict[str, UAVWeatherCollector] = {}

    def register_drone(self, drone_id: str) -> UAVWeatherCollector:
        collector = UAVWeatherCollector(drone_id)
        self.collectors[drone_id] = collector
        return collector

    def fused_weather(self, drone_id: str) -> Optional[dict]:
        if drone_id not in self.collectors:
            return None
        collector = self.collectors[drone_id]
        sensor = None
        wrf = None
        for r in reversed(list(collector.buffer)):
            if r.source == WeatherSource.UAV_SENSOR and not sensor:
                sensor = r
            if r.source == WeatherSource.WRF and not wrf:
                wrf = r
            if sensor and wrf:
                break
        if not sensor:
            return None
        return {
            "temperature": sensor.temperature,
            "humidity": sensor.humidity,
            "wind_speed": sensor.wind_speed * 0.7 + (wrf.wind_speed if wrf else sensor.wind_speed) * 0.3,
            "wind_direction": sensor.wind_direction,
            "source_fusion": "sensor+wrf" if wrf else "sensor_only",
            "drone_id": drone_id,
            "timestamp": time.time()
        }


class WeatherAlertEngine:
    """气象告警引擎"""

    def __init__(self):
        self.thresholds = {
            "wind_speed_max": 12.0,
            "wind_gust_max": 18.0,
            "visibility_min": 2.0,
            "turbulence_max": 0.8,
            "precipitation_max": 10.0
        }
        self.alerts: List[dict] = []

    def evaluate(self, weather: dict) -> Optional[dict]:
        warnings = []
        if weather.get("wind_speed", 0) > self.thresholds["wind_speed_max"]:
            warnings.append(f"风速告警: {weather['wind_speed']}m/s")
        if weather.get("wind_gust", 0) > self.thresholds["wind_gust_max"]:
            warnings.append(f"阵风告警: {weather['wind_gust']}m/s")
        if weather.get("visibility", 10) < self.thresholds["visibility_min"]:
            warnings.append(f"能见度过低: {weather['visibility']}km")
        if warnings:
            alert = {
                "drone_id": weather.get("drone_id"),
                "timestamp": time.time(),
                "warnings": warnings,
                "level": "HIGH" if len(warnings) >= 2 else "MEDIUM"
            }
            self.alerts.append(alert)
            return alert
        return None
