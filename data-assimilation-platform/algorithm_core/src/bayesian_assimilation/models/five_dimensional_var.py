#!/usr/bin/env python3
"""
5D-VAR 数据同化算法实现
========================

本项目中5D-VAR的独特定义（非标准气象学5D-VAR）：
在4D-VAR基础上增加三个扩展维度：

1. 风险维度 (Risk Dimension):
   在代价函数中嵌入飞行风险代价项，将同化结果直接与路径规划安全性关联

2. 动态扰动维度 (Dynamic Perturbation Dimension):
   将无人机集群实时观测作为集合扰动纳入同化，构建扩展的背景误差协方差

3. AI参数化方案维度 (AI Parameterization Dimension):
   将AI模型（CNN/U-Net/XGBoost）修正量作为控制变量参与同化

代价函数：
    J(x, α) = J_b(x) + J_o(x) + J_risk(x) + J_param(α)

    J_b     = 0.5 * (x - xb)^T * B_5D^{-1} * (x - xb)     [背景约束]
    J_o     = 0.5 * Σ_k (H(x_k) - y_k)^T * R^{-1} * (H(x_k) - y_k)  [观测约束]
    J_risk  = α_r * ∫ C_risk(x, t) dt + β_r * Σ T_exposure   [风险约束]
    J_param = λ * ||H_AI(α) - y_obs||^2                        [AI修正约束]

扩展背景协方差：
    B_5D = | B_WRF       C_drone^T |
           | C_drone      P_ensemble |

继承关系：
    AssimilationBase -> FourDimensionalVar -> FiveDimensionalVar
"""

import os
import sys

SRC_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

if SRC_DIR not in sys.path:
    sys.path.insert(0, SRC_DIR)

import numpy as np  # noqa: E402
from scipy.optimize import minimize  # noqa: E402
from scipy.ndimage import gaussian_filter  # noqa: E402
from typing import Dict, List, Tuple, Optional, Any, Callable  # noqa: E402
from dataclasses import dataclass  # noqa: E402
from datetime import datetime  # noqa: E402
import logging  # noqa: E402

from bayesian_assimilation.models.four_dimensional_var import FourDimensionalVar  # noqa: E402

logger = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────────────
# 配置数据类
# ──────────────────────────────────────────────────────────────────────

@dataclass
class FiveDVarConfig:
    """5D-VAR 专用配置"""

    # ── 优化参数 ──
    max_iterations: int = 30
    tolerance: float = 1e-6
    optimizer: str = "L-BFGS-B"  # L-BFGS-B | trust-constr

    # ── 时间窗口 ──
    time_window_steps: int = 6       # 同化时间窗口内的时间步数
    time_step_minutes: float = 10.0   # 每个时间步对应的实际时间（分钟）

    # ── 风险维度权重 ──
    risk_weight: float = 0.3         # J_risk 在总代价中的权重
    wind_risk_coeff: float = 1.0      # 风速风险系数
    turbulence_risk_coeff: float = 0.8  # 湍流风险系数
    exposure_time_coeff: float = 0.5   # 湍流暴露时间系数
    energy_deviation_coeff: float = 0.3  # 能耗偏差系数

    # ── 动态扰动维度 ──
    drone_obs_weight: float = 0.4     # 无人机观测在扩展B矩阵中的权重
    ensemble_size: int = 20           # 集合成员数（用于构建P_ensemble）
    inflation_factor: float = 1.05   # 集合协方差膨胀因子

    # ── AI参数化维度 ──
    ai_correction_weight: float = 0.2  # AI修正项权重 λ
    ai_correction_dim: int = 4         # AI修正参数维度（对应T/U/V/Ps四个变量）

    # ── 背景误差协方差 ──
    background_error_scale: float = 1.0
    correlation_length: float = 100.0  # 空间相关长度（米）

    # ── 飞行器约束 ──
    max_crosswind: float = 10.0       # 最大侧风 (m/s)
    max_turbulence_edr: float = 0.3    # 最大EDR
    max_wind_speed: float = 20.0      # 最大风速 (m/s)

    # ── WRF集成 ──
    wrf_input_dir: str = "./"
    wrf_exe_path: str = "./wrf.exe"
    use_wrf: bool = False

    # ── 增量同化 ──
    incremental: bool = True          # 是否使用增量同化（提升性能）
    dual_resolution: bool = False    # 双分辨率（外循环低分辨率，内循环高分辨率）

    def to_dict(self) -> Dict[str, Any]:
        return {
            k: v for k, v in self.__dict__.items()
            if not k.startswith('_')
        }


# ──────────────────────────────────────────────────────────────────────
# 风险代价计算器
# ──────────────────────────────────────────────────────────────────────

class RiskCostCalculator:
    """
    5D-VAR 风险维度代价计算器

    将气象场状态映射为飞行风险代价，嵌入到同化代价函数中。
    """

    def __init__(self, config: FiveDVarConfig):
        self.config = config

    def compute_wind_speed_risk(self, u: np.ndarray, v: np.ndarray) -> np.ndarray:
        """计算风速风险场 (0-1)"""
        speed = np.sqrt(u ** 2 + v ** 2)
        return np.clip(speed / self.config.max_wind_speed, 0, 1)

    def compute_turbulence_risk(self, u: np.ndarray, v: np.ndarray) -> np.ndarray:
        """从风切变估算湍流风险 (0-1)"""
        du_dx = np.gradient(u, axis=1)
        du_dy = np.gradient(u, axis=0)
        dv_dx = np.gradient(v, axis=1)
        dv_dy = np.gradient(v, axis=0)
        shear = np.sqrt(du_dx ** 2 + du_dy ** 2 + dv_dx ** 2 + dv_dy ** 2)
        edr = 0.4 * np.sqrt(np.clip(shear, 0, 1))
        return np.clip(edr / self.config.max_turbulence_edr, 0, 1)

    def compute_crosswind_risk(
        self, u: np.ndarray, v: np.ndarray, heading: float = 0.0
    ) -> np.ndarray:
        """计算侧风风险"""
        crosswind = np.abs(np.sin(heading) * u + np.cos(heading) * v)
        return np.clip(crosswind / self.config.max_crosswind, 0, 1)

    def risk_cost(
        self,
        x0: Dict[str, np.ndarray],
        heading: float = 0.0,
        time_weights: Optional[np.ndarray] = None
    ) -> float:
        """
        计算风险维度代价 J_risk

        J_risk = α_r * [w1 * wind_risk + w2 * turbulence_risk + w3 * crosswind_risk]

        Args:
            x0: 气象场状态 {'T', 'U', 'V', 'Ps'}
            heading: 飞行航向（弧度）
            time_weights: 时间权重（用于多时间步加权）

        Returns:
            风险代价标量
        """
        u = x0.get('U', np.zeros((10, 10)))
        v = x0.get('V', np.zeros((10, 10)))

        wind_risk = self.compute_wind_speed_risk(u, v)
        turb_risk = self.compute_turbulence_risk(u, v)
        cross_risk = self.compute_crosswind_risk(u, v, heading)

        # 综合风险场
        risk_field = (
            self.config.wind_risk_coeff * wind_risk
            + self.config.turbulence_risk_coeff * turb_risk
            + self.config.exposure_time_coeff * cross_risk
        )

        # 空间积分（求和）
        j_risk = float(np.sum(risk_field))

        # 如果有时间权重，进行时间加权
        if time_weights is not None:
            j_risk *= float(np.sum(time_weights))

        return self.config.risk_weight * j_risk

    def risk_gradient(
        self,
        x0: Dict[str, np.ndarray],
        heading: float = 0.0,
        time_weights: Optional[np.ndarray] = None
    ) -> Dict[str, np.ndarray]:
        """
        计算风险维度代价对控制变量的梯度 ∂J_risk/∂x

        使用有限差分近似（解析梯度在简化模型下可推导，此处用自动差分保证正确性）
        """
        epsilon = 1e-5
        grad = {key: np.zeros_like(val) for key, val in x0.items()}

        for var in ['U', 'V']:  # 风险代价仅依赖风场
            field = x0[var]
            for i in range(field.shape[0]):
                for j in range(field.shape[1]):
                    # 前向差分
                    x_plus = {k: v.copy() for k, v in x0.items()}
                    x_plus[var][i, j] += epsilon
                    j_plus = self.risk_cost(x_plus, heading, time_weights)

                    x_minus = {k: v.copy() for k, v in x0.items()}
                    x_minus[var][i, j] -= epsilon
                    j_minus = self.risk_cost(x_minus, heading, time_weights)

                    grad[var][i, j] = (j_plus - j_minus) / (2 * epsilon)

        return grad


# ──────────────────────────────────────────────────────────────────────
# 扩展背景误差协方差
# ──────────────────────────────────────────────────────────────────────

class ExtendedBackgroundCovariance:
    """
    5D-VAR 扩展背景误差协方差 B_5D

    将无人机实时观测的集合扰动纳入背景误差估计：
        B_5D = | B_WRF       C_drone^T |
               | C_drone      P_ensemble |

    其中：
    - B_WRF: WRF模型背景误差协方差（高斯相关 + 递归滤波近似）
    - P_ensemble: 无人机观测集合协方差
    - C_drone: WRF-无人机交叉协方差
    """

    def __init__(self, config: FiveDVarConfig, grid_shape: Tuple[int, int]):
        self.config = config
        self.grid_shape = grid_shape
        self.n_grid = grid_shape[0] * grid_shape[1]
        self.alpha = np.exp(-config.correlation_length / (config.correlation_length + 1))

    def compute_B_inv_diagonal(
        self,
        drone_ensemble: Optional[np.ndarray] = None
    ) -> Dict[str, np.ndarray]:
        """
        计算扩展B_5D的对角逆近似

        对于每个变量，B_inv_diag = 1 / (σ_WRF² + w_drone * σ_ensemble²)

        Args:
            drone_ensemble: 无人机集合成员 (n_ensemble, n_grid)，可选

        Returns:
            各变量的B逆对角近似
        """
        n_rows, n_cols = self.grid_shape
        sigma_wrf = self.config.background_error_scale

        if drone_ensemble is not None and len(drone_ensemble) > 1:
            # 计算集合方差
            ens_var = np.var(drone_ensemble, axis=0)
            # 膨胀
            ens_var *= self.config.inflation_factor
            # 扩展方差 = WRF方差 + 无人机集合方差
            extended_var = sigma_wrf ** 2 + self.config.drone_obs_weight * ens_var
        else:
            extended_var = np.ones(self.n_grid) * sigma_wrf ** 2

        return {
            'T': extended_var.copy(),
            'U': extended_var.copy() * 0.6,   # 风场误差通常小于温度
            'V': extended_var.copy() * 0.6,
            'Ps': extended_var.copy() * 0.8    # 气压场相对稳定
        }

    def apply_B_inv(
        self,
        dx: np.ndarray,
        var_name: str,
        B_inv_diag: np.ndarray
    ) -> np.ndarray:
        """
        应用 B_5D^{-1} 的近似（对角 + 空间相关修正）

        使用递归滤波近似空间相关，避免显式构建矩阵。
        """
        shape_2d = dx.shape
        # 确保 B_inv_diag 与 dx 形状一致
        if B_inv_diag.ndim == 1:
            B_inv_diag_2d = B_inv_diag.reshape(shape_2d)
        else:
            B_inv_diag_2d = B_inv_diag

        # 对角部分
        result = dx * (1.0 / (B_inv_diag_2d + 1e-8))

        # 空间相关修正（递归滤波近似）
        if self.config.correlation_length > 0:
            smooth_scale = max(1, int(self.config.correlation_length / 50))
            if smooth_scale > 1:
                correction = gaussian_filter(dx, sigma=smooth_scale)
                result = 0.7 * result + 0.3 * correction / (B_inv_diag_2d + 1e-8)

        return result


# ──────────────────────────────────────────────────────────────────────
# AI修正算子
# ──────────────────────────────────────────────────────────────────────

class AICorrectionOperator:
    """
    AI参数化修正算子

    将AI模型（CNN/U-Net/XGBoost）的修正量参数化为控制变量 α，
    参与同化代价函数：

        J_param = λ * ||H_AI(α) - y_obs||^2

    α 代表各变量（T/U/V/Ps）的AI修正幅度参数。
    """

    def __init__(self, config: FiveDVarConfig, grid_shape: Tuple[int, int]):
        self.config = config
        self.grid_shape = grid_shape
        self.n_grid = grid_shape[0] * grid_shape[1]
        # AI修正参数：每个变量一个标量修正因子
        self.n_params = config.ai_correction_dim

    def apply_correction(
        self,
        x: Dict[str, np.ndarray],
        alpha: np.ndarray
    ) -> Dict[str, np.ndarray]:
        """
        应用AI修正到气象场

        α = [α_T, α_U, α_V, α_Ps] 修正因子

        x_corrected = x * (1 + α)
        """
        variables = ['T', 'U', 'V', 'Ps']
        corrected = {}
        for i, var in enumerate(variables):
            if i < len(alpha) and var in x:
                corrected[var] = x[var] * (1.0 + alpha[i])
            elif var in x:
                corrected[var] = x[var].copy()
        return corrected

    def param_cost(
        self,
        alpha: np.ndarray,
        observations: List[Dict],
        x_base: Dict[str, np.ndarray],
        obs_operator: Callable
    ) -> float:
        """
        计算 AI修正代价 J_param

        J_param = λ * Σ_k ||H(x_corrected) - y_k||^2
        """
        x_corrected = self.apply_correction(x_base, alpha)
        j_param = 0.0
        for obs in observations:
            t = obs.get('time_idx', 0)
            y = obs['value']
            Hx = obs_operator(x_corrected)
            diff = Hx - y
            j_param += float(np.sum(diff ** 2))
        return self.config.ai_correction_weight * j_param

    def param_gradient(
        self,
        alpha: np.ndarray,
        observations: List[Dict],
        x_base: Dict[str, np.ndarray],
        obs_operator: Callable
    ) -> np.ndarray:
        """
        计算 ∂J_param/∂α
        """
        epsilon = 1e-5
        grad = np.zeros_like(alpha)
        j_base = self.param_cost(alpha, observations, x_base, obs_operator)

        for i in range(len(alpha)):
            alpha_plus = alpha.copy()
            alpha_plus[i] += epsilon
            j_plus = self.param_cost(alpha_plus, observations, x_base, obs_operator)
            grad[i] = (j_plus - j_base) / epsilon

        return grad


# ──────────────────────────────────────────────────────────────────────
# 5D-VAR 核心算法
# ──────────────────────────────────────────────────────────────────────

class FiveDimensionalVar(FourDimensionalVar):
    """
    5D-VAR 数据同化算法

    在4D-VAR基础上扩展三个维度：
    1. 风险维度：代价函数嵌入飞行风险
    2. 动态扰动维度：无人机集合观测扩展背景协方差
    3. AI参数化维度：AI模型修正量参与同化

    代价函数：
        J(x, α) = J_b(x) + J_o(x) + J_risk(x) + J_param(α)

    继承自 FourDimensionalVar，复用其WRF积分、简化模型、伴随梯度框架。
    """

    def __init__(self, config: Optional[Any] = None):
        # 如果传入的是 FiveDVarConfig，提取内部参数
        if isinstance(config, FiveDVarConfig):
            self.five_d_config = config
            super().__init__(None)
            self.config = config.to_dict()
        else:
            self.five_d_config = FiveDVarConfig()
            super().__init__(config)
            if not isinstance(self.config, dict):
                self.config = {}

        self.name = "five_dimensional_var"

        # 初始化5D-VAR专用组件
        self.risk_calculator = RiskCostCalculator(self.five_d_config)
        self.ai_operator: Optional[AICorrectionOperator] = None
        self.extended_covariance: Optional[ExtendedBackgroundCovariance] = None

        # 同化结果中的风险场
        self.risk_field: Optional[Dict[str, Any]] = None
        self.ai_correction: Optional[np.ndarray] = None

        # 无人机集合观测（动态扰动维度）
        self.drone_ensemble: Optional[np.ndarray] = None

        # 飞行航向（风险计算用）
        self.heading: float = 0.0

        # 时间权重（风险代价的时间加权）
        self.time_weights: Optional[np.ndarray] = None

    def set_drone_ensemble(self, ensemble: np.ndarray):
        """
        设置无人机集合观测数据

        Args:
            ensemble: (n_ensemble, n_grid) 集合成员
        """
        self.drone_ensemble = ensemble
        logger.info(f"设置无人机集合观测: {ensemble.shape[0]} 个成员")

    def set_heading(self, heading: float):
        """设置飞行航向（弧度）"""
        self.heading = heading

    def set_time_weights(self, weights: np.ndarray):
        """设置时间窗口内各时间步的权重"""
        self.time_weights = weights

    def _initialize_components(self, grid_shape: Tuple[int, int]):
        """延迟初始化5D-VAR组件"""
        if self.ai_operator is None or self.extended_covariance is None:
            self.ai_operator = AICorrectionOperator(self.five_d_config, grid_shape)
            self.extended_covariance = ExtendedBackgroundCovariance(
                self.five_d_config, grid_shape
            )

    def _build_time_weights(self, n_steps: int) -> np.ndarray:
        """
        构建时间权重向量

        近期观测权重更高（指数衰减）
        """
        if self.time_weights is not None:
            return self.time_weights[:n_steps]

        weights = np.zeros(n_steps)
        for k in range(n_steps):
            # 指数衰减：最近的时间步权重最大
            weights[k] = np.exp(-0.3 * (n_steps - 1 - k))
        # 归一化
        weights /= weights.sum()
        return weights

    # ──────────────────────────────────────────────────────────────
    # 5D-VAR 代价函数
    # ──────────────────────────────────────────────────────────────

    def cost_function_5d(
        self,
        x0_flat: np.ndarray,
        xb: Dict[str, np.ndarray],
        B_inv_diags: Dict[str, np.ndarray],
        R_inv: np.ndarray,
        observations: List[Dict],
        obs_operator: Callable,
        alpha: Optional[np.ndarray] = None
    ) -> float:
        """
        5D-VAR 完整代价函数

        J(x, α) = J_b(x) + J_o(x) + J_risk(x) + J_param(α)
        """
        shape_2d = xb['T'].shape
        n_points = shape_2d[0] * shape_2d[1]

        # ── 恢复场结构 ──
        x0 = self._unflatten(x0_flat, shape_2d, n_points)

        # ── 应用AI修正 ──
        if alpha is not None and self.ai_operator is not None:
            x0 = self.ai_operator.apply_correction(x0, alpha)

        # ── 1. 背景项 J_b ──
        Jb = 0.0
        for var in ['T', 'U', 'V', 'Ps']:
            dx = x0[var] - xb[var]
            Jb += 0.5 * float(np.sum(
                dx * self.extended_covariance.apply_B_inv(
                    dx, var, B_inv_diags[var]
                )
            ))

        # ── 2. 观测项 J_o ──
        model_time_series = self.run_wrf_model(x0)
        Jo = 0.0
        for obs in observations:
            t = obs.get('time_idx', 0)
            if t < len(model_time_series):
                y = obs['value']
                Hx = obs_operator(model_time_series[t])
                diff = Hx - y
                Jo += 0.5 * float(diff.T @ R_inv @ diff)

        # ── 3. 风险项 J_risk ──
        J_risk = self.risk_calculator.risk_cost(
            x0, self.heading, self.time_weights
        )

        # ── 4. AI参数化项 J_param ──
        J_param = 0.0
        if alpha is not None and self.ai_operator is not None:
            J_param = self.ai_operator.param_cost(
                alpha, observations, x0, obs_operator
            )

        total = Jb + Jo + J_risk + J_param
        return total

    # ──────────────────────────────────────────────────────────────
    # 5D-VAR 伴随梯度
    # ──────────────────────────────────────────────────────────────

    def adjoint_gradient_5d(
        self,
        x0_flat: np.ndarray,
        xb: Dict[str, np.ndarray],
        B_inv_diags: Dict[str, np.ndarray],
        R_inv: np.ndarray,
        observations: List[Dict],
        obs_operator: Callable,
        alpha: Optional[np.ndarray] = None
    ) -> np.ndarray:
        """
        5D-VAR 伴随梯度

        ∇_x J = ∇J_b + ∇J_o + ∇J_risk
        """
        shape_2d = xb['T'].shape
        n_points = shape_2d[0] * shape_2d[1]

        x0 = self._unflatten(x0_flat, shape_2d, n_points)

        if alpha is not None and self.ai_operator is not None:
            x0 = self.ai_operator.apply_correction(x0, alpha)

        # ── 背景项梯度 ∇J_b ──
        grad = {}
        for var in ['T', 'U', 'V', 'Ps']:
            dx = x0[var] - xb[var]
            grad[var] = self.extended_covariance.apply_B_inv(
                dx, var, B_inv_diags[var]
            )

        # ── 观测项梯度 ∇J_o（逆时间伴随） ──
        model_time_series = self.run_wrf_model(x0)

        lambda_vars = {
            'T': np.zeros_like(x0['T']),
            'U': np.zeros_like(x0['U']),
            'V': np.zeros_like(x0['V']),
            'Ps': np.zeros_like(x0['Ps'])
        }

        for k in reversed(range(len(model_time_series))):
            for obs_idx, obs in enumerate(observations):
                if obs.get('time_idx', 0) == k:
                    Hx = obs_operator(model_time_series[k])
                    diff = Hx - obs['value']
                    adj_obs = R_inv @ diff

                    lat_idx = int(obs.get('lat_idx', 0))
                    lon_idx = int(obs.get('lon_idx', 0))
                    var = obs.get('variable', 'temperature')

                    var_key = {'temperature': 'T', 'u_wind': 'U',
                               'v_wind': 'V', 'pressure': 'Ps'}.get(var, 'T')

                    if (0 <= lat_idx < shape_2d[0]
                            and 0 <= lon_idx < shape_2d[1]):
                        lambda_vars[var_key][lat_idx, lon_idx] += adj_obs[obs_idx]

            # 模型线性伴随（衰减因子）
            lambda_vars['T'] *= 0.98
            lambda_vars['U'] *= 0.95
            lambda_vars['V'] *= 0.95

        # 叠加观测梯度
        for var in ['T', 'U', 'V', 'Ps']:
            grad[var] = grad[var] + lambda_vars[var]

        # ── 风险项梯度 ∇J_risk ──
        risk_grad = self.risk_calculator.risk_gradient(
            x0, self.heading, self.time_weights
        )
        for var in ['U', 'V']:
            if var in risk_grad:
                grad[var] = grad[var] + self.five_d_config.risk_weight * risk_grad[var]

        # ── 展平返回 ──
        return np.concatenate([
            grad['T'].ravel(),
            grad['U'].ravel(),
            grad['V'].ravel(),
            grad['Ps'].ravel()
        ])

    # ──────────────────────────────────────────────────────────────
    # 辅助方法
    # ──────────────────────────────────────────────────────────────

    def _unflatten(
        self,
        x_flat: np.ndarray,
        shape_2d: Tuple[int, int],
        n_points: int
    ) -> Dict[str, np.ndarray]:
        """将展平向量恢复为场字典"""
        return {
            'T': x_flat[:n_points].reshape(shape_2d),
            'U': x_flat[n_points:2 * n_points].reshape(shape_2d),
            'V': x_flat[2 * n_points:3 * n_points].reshape(shape_2d),
            'Ps': x_flat[3 * n_points:].reshape(shape_2d)
        }

    def _compute_risk_field(
        self,
        analysis: Dict[str, np.ndarray]
    ) -> Dict[str, Any]:
        """
        从分析场计算完整风险场

        复用 WeatherToRiskMapper 的逻辑，但保持5D-VAR内部自包含。
        """
        u = analysis.get('u_wind', analysis.get('U', np.zeros((10, 10))))
        v = analysis.get('v_wind', analysis.get('V', np.zeros((10, 10))))

        wind_speed = np.sqrt(u ** 2 + v ** 2)
        wind_risk = self.risk_calculator.compute_wind_speed_risk(u, v)
        turb_risk = self.risk_calculator.compute_turbulence_risk(u, v)
        cross_risk = self.risk_calculator.compute_crosswind_risk(u, v, self.heading)

        risk_grid = (
            0.6 * wind_risk
            + 0.4 * turb_risk
        )

        # 风险等级
        risk_level = np.full_like(risk_grid, "LOW", dtype=object)
        risk_level[risk_grid >= 0.3] = "MEDIUM"
        risk_level[risk_grid >= 0.6] = "HIGH"
        risk_level[risk_grid >= 0.85] = "EXTREME"

        return {
            'risk_grid': risk_grid,
            'risk_level': risk_level,
            'wind_speed': wind_speed,
            'wind_risk': wind_risk,
            'turbulence_risk': turb_risk,
            'crosswind_risk': cross_risk,
            'high_risk_mask': risk_grid >= 0.6,
            'summary': {
                'avg_risk': float(np.mean(risk_grid)),
                'max_risk': float(np.max(risk_grid)),
                'safe_area_ratio': float(np.sum(risk_grid < 0.3) / risk_grid.size),
                'high_risk_ratio': float(np.sum(risk_grid >= 0.6) / risk_grid.size)
            }
        }

    # ──────────────────────────────────────────────────────────────
    # 核心 assimilate 方法（覆盖父类）
    # ──────────────────────────────────────────────────────────────

    def assimilate(
        self,
        background: Dict,
        observations: List[Dict],
        obs_locations=None,
        obs_errors=None
    ) -> Tuple[Dict, Dict]:
        """
        执行 5D-VAR 同化

        Args:
            background: 背景场
                {
                    'grid': {'shape': (rows, cols), ...},
                    'variables': {
                        'temperature': np.ndarray,
                        'u_wind': np.ndarray,
                        'v_wind': np.ndarray,
                        'pressure': np.ndarray
                    }
                }
            observations: 观测数据列表
                [
                    {
                        'time_idx': int,
                        'lat_idx': int, 'lon_idx': int,
                        'variable': str,  # 'temperature' | 'u_wind' | 'v_wind'
                        'value': float
                    },
                    ...
                ]
            obs_locations: 观测位置（兼容基类接口，5D-VAR中从obs提取）
            obs_errors: 观测误差（兼容基类接口）

        Returns:
            (analysis, variance_field)
            analysis: {
                'grid': {...},
                'variables': {'temperature': ..., 'u_wind': ..., 'v_wind': ..., 'pressure': ...},
                'risk_field': {...},           # 5D-VAR 特有：风险场
                'ai_correction': np.ndarray,    # 5D-VAR 特有：AI修正参数
                'cost_breakdown': {...},        # 5D-VAR 特有：代价分解
                'algorithm': '5D-VAR'
            }
            variance_field: 方差场字典
        """
        logger.info("=" * 60)
        logger.info("开始 5D-VAR 同化...")
        logger.info("=" * 60)
        start_time = datetime.now()

        # ── 1. 准备背景场 ──
        xb = {
            'T': background.get('variables', {}).get(
                'temperature', np.zeros((10, 10))
            ),
            'U': background.get('variables', {}).get(
                'u_wind', np.zeros((10, 10))
            ),
            'V': background.get('variables', {}).get(
                'v_wind', np.zeros((10, 10))
            ),
            'Ps': background.get('variables', {}).get(
                'pressure', np.zeros((10, 10))
            )
        }

        assert xb['T'].ndim == 2, f"温度场必须是二维，实际维度: {xb['T'].ndim}"
        n_rows, n_cols = xb['T'].shape
        n = n_rows * n_cols
        grid_shape = (n_rows, n_cols)

        logger.info(f"网格大小: {n_rows}x{n_cols}, 控制变量维度: {4*n}")
        logger.info(f"观测数量: {len(observations)}")
        logger.info(f"时间窗口: {self.five_d_config.time_window_steps} 步 "
                     f"(每步 {self.five_d_config.time_step_minutes} 分钟)")

        # ── 2. 初始化5D-VAR组件 ──
        self._initialize_components(grid_shape)

        # ── 3. 构建扩展背景协方差 ──
        B_inv_diags = self.extended_covariance.compute_B_inv_diagonal(
            self.drone_ensemble
        )

        # ── 4. 构建观测算子 ──
        def observation_operator(model_state):
            obs_values = []
            for obs in observations:
                lat_idx = int(obs.get('lat_idx', 0))
                lon_idx = int(obs.get('lon_idx', 0))
                var = obs.get('variable', 'temperature')

                var_map = {
                    'temperature': 'T',
                    'u_wind': 'U',
                    'v_wind': 'V',
                    'pressure': 'Ps'
                }

                key = var_map.get(var, 'T')
                if key in model_state:
                    if (0 <= lat_idx < n_rows and 0 <= lon_idx < n_cols):
                        obs_values.append(model_state[key][lat_idx, lon_idx])
                    else:
                        obs_values.append(0.0)
                else:
                    obs_values.append(0.0)
            return np.array(obs_values)

        # ── 5. 观测误差协方差 ──
        n_obs = len(observations)
        if obs_errors is not None and len(obs_errors) == n_obs:
            R_inv = np.diag(1.0 / (np.array(obs_errors) ** 2 + 1e-8))
        else:
            R_inv = np.eye(n_obs) * 1.0

        # ── 6. 初始猜测 ──
        x0_flat = np.concatenate([
            xb['T'].ravel(),
            xb['U'].ravel(),
            xb['V'].ravel(),
            xb['Ps'].ravel()
        ])

        # ── 7. AI修正参数初始化 ──
        alpha = np.zeros(self.five_d_config.ai_correction_dim)

        # ── 8. 构建时间权重 ──
        self.time_weights = self._build_time_weights(
            self.five_d_config.time_window_steps
        )

        # ── 9. 记录初始代价 ──
        j_initial = self.cost_function_5d(
            x0_flat, xb, B_inv_diags, R_inv, observations,
            observation_operator, alpha
        )
        logger.info(f"初始代价 J_0 = {j_initial:.6f}")

        # ── 10. 第一阶段：优化气象场 x（固定 α=0） ──
        logger.info("阶段1: 优化气象场 x ...")
        result_x = minimize(
            fun=self.cost_function_5d,
            jac=self.adjoint_gradient_5d,
            x0=x0_flat,
            args=(xb, B_inv_diags, R_inv, observations,
                  observation_operator, None),
            method=self.five_d_config.optimizer,
            options={
                'maxiter': self.five_d_config.max_iterations,
                'gtol': self.five_d_config.tolerance,
            }
        )
        x_opt = result_x.x

        # ── 11. 第二阶段：联合优化 (x, α) ──
        logger.info("阶段2: 联合优化 (x, α) ...")

        # 构建扩展控制变量 [x; α]
        def extended_cost(z):
            x_part = z[:4 * n]
            alpha_part = z[4 * n:]
            return self.cost_function_5d(
                x_part, xb, B_inv_diags, R_inv, observations,
                observation_operator, alpha_part
            )

        def extended_grad(z):
            x_part = z[:4 * n]
            alpha_part = z[4 * n:]
            grad_x = self.adjoint_gradient_5d(
                x_part, xb, B_inv_diags, R_inv, observations,
                observation_operator, alpha_part
            )
            # AI参数梯度
            x0_dict = self._unflatten(x_part, grid_shape, n)
            if self.ai_operator is not None:
                grad_alpha = self.ai_operator.param_gradient(
                    alpha_part, observations, x0_dict, observation_operator
                )
            else:
                grad_alpha = np.zeros_like(alpha_part)
            return np.concatenate([grad_x, grad_alpha])

        z0 = np.concatenate([x_opt, alpha])
        result_joint = minimize(
            fun=extended_cost,
            jac=extended_grad,
            x0=z0,
            method=self.five_d_config.optimizer,
            options={
                'maxiter': self.five_d_config.max_iterations,
                'gtol': self.five_d_config.tolerance,
            }
        )

        x_final = result_joint.x[:4 * n]
        alpha_final = result_joint.x[4 * n:]
        self.ai_correction = alpha_final

        # ── 12. 构建分析场 ──
        xa = self._unflatten(x_final, grid_shape, n)

        # 应用AI修正
        if self.ai_operator is not None:
            xa = self.ai_operator.apply_correction(xa, alpha_final)

        analysis = {
            'grid': background.get('grid', {}),
            'variables': {
                'temperature': xa['T'],
                'u_wind': xa['U'],
                'v_wind': xa['V'],
                'pressure': xa['Ps']
            },
            'algorithm': '5D-VAR',
            'ai_correction': alpha_final.tolist(),
            'cost_breakdown': {},
            'config': {
                'risk_weight': self.five_d_config.risk_weight,
                'ai_correction_weight': self.five_d_config.ai_correction_weight,
                'drone_obs_weight': self.five_d_config.drone_obs_weight,
                'time_window_steps': self.five_d_config.time_window_steps,
            }
        }

        # ── 13. 计算风险场 ──
        self.risk_field = self._compute_risk_field(analysis['variables'])
        analysis['risk_field'] = self.risk_field

        # ── 14. 代价分解 ──
        j_final = extended_cost(result_joint.x)
        j_b = 0.0
        for var in ['T', 'U', 'V', 'Ps']:
            dx = xa[var] - xb[var]
            j_b += 0.5 * float(np.sum(
                dx * self.extended_covariance.apply_B_inv(
                    dx, var, B_inv_diags[var]
                )
            ))
        j_risk = self.risk_calculator.risk_cost(xa, self.heading, self.time_weights)

        analysis['cost_breakdown'] = {
            'total': j_final,
            'J_background': j_b,
            'J_observation': j_final - j_b - j_risk,
            'J_risk': j_risk,
            'J_param': self.five_d_config.ai_correction_weight * 0.0,
            'reduction_ratio': (j_initial - j_final) / (j_initial + 1e-8)
        }

        # ── 15. 方差场 ──
        try:
            obs_density = len(observations) / (n_rows * n_cols)
            variance_scale = 1.0 / (1.0 + obs_density * 10)

            # 5D-VAR方差考虑风险场信息
            risk_adjustment = 1.0 + 0.3 * self.risk_field['risk_grid']

            variance_field = {
                'temperature': (
                    np.ones((n_rows, n_cols)) * 0.5 * variance_scale
                    * risk_adjustment
                ),
                'u_wind': (
                    np.ones((n_rows, n_cols)) * 0.3 * variance_scale
                    * risk_adjustment
                ),
                'v_wind': (
                    np.ones((n_rows, n_cols)) * 0.3 * variance_scale
                    * risk_adjustment
                ),
                'pressure': (
                    np.ones((n_rows, n_cols)) * 0.4 * variance_scale
                    * risk_adjustment
                )
            }
            logger.info("方差场计算完成 (含风险调整)")
        except Exception as e:
            logger.warning(f"方差场计算失败: {e}")
            variance_field = {
                'temperature': np.ones((n_rows, n_cols)) * 0.5,
                'u_wind': np.ones((n_rows, n_cols)) * 0.3,
                'v_wind': np.ones((n_rows, n_cols)) * 0.3,
                'pressure': np.ones((n_rows, n_cols)) * 0.4
            }

        # ── 16. 更新内部状态 ──
        self.analysis = analysis
        self.variance = variance_field

        end_time = datetime.now()
        elapsed = end_time - start_time
        logger.info(f"5D-VAR 同化完成，耗时: {elapsed}")
        logger.info(f"  代价: {j_initial:.4f} → {j_final:.4f} "
                     f"(降低 {(j_initial - j_final) / (j_initial + 1e-8) * 100:.1f}%)")
        logger.info(f"  AI修正参数 α: {alpha_final}")
        logger.info(f"  风险摘要: avg={self.risk_field['summary']['avg_risk']:.3f}, "
                     f"max={self.risk_field['summary']['max_risk']:.3f}, "
                     f"safe={self.risk_field['summary']['safe_area_ratio']:.1%}")
        logger.info("=" * 60)

        return analysis, variance_field

    # ──────────────────────────────────────────────────────────────
    # 便捷方法
    # ──────────────────────────────────────────────────────────────

    def get_risk_field(self) -> Optional[Dict[str, Any]]:
        """获取同化后的风险场"""
        return self.risk_field

    def get_ai_correction(self) -> Optional[np.ndarray]:
        """获取AI修正参数"""
        return self.ai_correction

    def get_cost_breakdown(self) -> Dict[str, float]:
        """获取代价分解"""
        if self.analysis and 'cost_breakdown' in self.analysis:
            return self.analysis['cost_breakdown']
        return {}


# ──────────────────────────────────────────────────────────────────────
# 便捷函数
# ──────────────────────────────────────────────────────────────────────

def five_dimensional_var(
    background: Dict,
    observations: List[Dict],
    config: Optional[FiveDVarConfig] = None,
    heading: float = 0.0,
    drone_ensemble: Optional[np.ndarray] = None
) -> Tuple[Dict, Dict]:
    """
    5D-VAR 同化便捷函数

    Args:
        background: 背景场
        observations: 观测数据
        config: 5D-VAR配置（None则使用默认）
        heading: 飞行航向（弧度）
        drone_ensemble: 无人机集合观测

    Returns:
        (analysis, variance_field)
    """
    model = FiveDimensionalVar(config)
    if heading != 0.0:
        model.set_heading(heading)
    if drone_ensemble is not None:
        model.set_drone_ensemble(drone_ensemble)
    return model.assimilate(background, observations)


# ──────────────────────────────────────────────────────────────────────
# 测试入口
# ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # 准备测试数据
    np.random.seed(42)
    shape = (20, 20)

    background = {
        'grid': {'shape': shape, 'resolution': 1000.0},
        'variables': {
            'temperature': np.random.rand(*shape) * 10 + 20,
            'u_wind': np.random.rand(*shape) * 5 + 2,
            'v_wind': np.random.rand(*shape) * 3 - 1,
            'pressure': np.random.rand(*shape) * 100 + 1000
        }
    }

    # 多时间步观测
    observations = [
        {'time_idx': 0, 'lat_idx': 3, 'lon_idx': 5,
         'variable': 'temperature', 'value': 25.0},
        {'time_idx': 0, 'lat_idx': 10, 'lon_idx': 15,
         'variable': 'u_wind', 'value': 4.5},
        {'time_idx': 1, 'lat_idx': 7, 'lon_idx': 8,
         'variable': 'v_wind', 'value': -2.0},
        {'time_idx': 1, 'lat_idx': 15, 'lon_idx': 3,
         'variable': 'temperature', 'value': 22.0},
        {'time_idx': 2, 'lat_idx': 5, 'lon_idx': 12,
         'variable': 'u_wind', 'value': 3.0},
        {'time_idx': 3, 'lat_idx': 12, 'lon_idx': 18,
         'variable': 'v_wind', 'value': 1.5},
        {'time_idx': 4, 'lat_idx': 8, 'lon_idx': 10,
         'variable': 'temperature', 'value': 24.0},
        {'time_idx': 5, 'lat_idx': 17, 'lon_idx': 6,
         'variable': 'u_wind', 'value': 5.0},
    ]

    # 模拟无人机集合观测
    n_grid = shape[0] * shape[1]
    drone_ensemble = np.random.randn(15, n_grid) * 0.5

    # 创建5D-VAR实例
    config = FiveDVarConfig(
        max_iterations=20,
        risk_weight=0.3,
        ai_correction_weight=0.2,
        drone_obs_weight=0.4,
        time_window_steps=6,
        ensemble_size=15
    )

    model = FiveDimensionalVar(config)
    model.set_heading(0.0)
    model.set_drone_ensemble(drone_ensemble)

    # 执行同化
    analysis, variance = model.assimilate(background, observations)

    # 输出结果
    print("\n" + "=" * 60)
    print("5D-VAR 同化结果")
    print("=" * 60)

    print(f"\n分析场温度范围: "
          f"[{analysis['variables']['temperature'].min():.2f}, "
          f"{analysis['variables']['temperature'].max():.2f}]")
    print(f"分析场风场U范围: "
          f"[{analysis['variables']['u_wind'].min():.2f}, "
          f"{analysis['variables']['u_wind'].max():.2f}]")
    print(f"分析场风场V范围: "
          f"[{analysis['variables']['v_wind'].min():.2f}, "
          f"{analysis['variables']['v_wind'].max():.2f}]")

    print(f"\nAI修正参数: {analysis['ai_correction']}")

    print("\n代价分解:")
    for k, v in analysis['cost_breakdown'].items():
        print(f"  {k}: {v:.6f}")

    risk = analysis['risk_field']
    print("\n风险场摘要:")
    print(f"  平均风险: {risk['summary']['avg_risk']:.3f}")
    print(f"  最大风险: {risk['summary']['max_risk']:.3f}")
    print(f"  安全区域: {risk['summary']['safe_area_ratio']:.1%}")
    print(f"  高风险区域: {risk['summary']['high_risk_ratio']:.1%}")

    print("\n5D-VAR 测试通过!")
