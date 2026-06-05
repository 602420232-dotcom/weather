# 气象数据 → 无人机路径 一条龙—— Input / Output / Return

> 覆盖从原始气象数据获取到 MAVLink 飞行指令输出的全链路流程，共 13 个阶段。

---

## 阶段 0: 数据源

*CMA 天资 / 风雷 / FengWu GHR*

- **触发**: 无外部输入，定时触发，每 30 min 一次
- **处理**: `data_pipeline/fetcher.py`
  - `CMAFetcher.fetch_tianzi()` → HTTP 请求 CMA API
  - `CMAFetcher.fetch_fenglei()` → HTTP 请求 CMA API
  - 不可用时回退到模拟数据

**返回**: `xarray.Dataset`

| 变量 | 维度 | 类型 | 含义 |
|------|------|------|------|
| `u10` | (lat, lon) | float32 | 10m 风速 u 分量 [m/s] |
| `v10` | (lat, lon) | float32 | 10m 风速 v 分量 [m/s] |
| `t2m` | (lat, lon) | float32 | 2m 温度 [K] |
| `rh2m` | (lat, lon) | float32 | 2m 相对湿度 [%] |
| `ps` | (lat, lon) | float32 | 地表气压 [Pa] |
| `blh` | (lat, lon) | float32 | 边界层高度 [m] |

属性: `source` (`"tianzi"` / `"fenglei"`), `forecast_time` (ISO 8601)

---

## 阶段 1: 时空配准

*Spatiotemporal Registration — `data_pipeline/registration.py`*

**输入**: `SpatiotemporalRegistrator.register()`

| 来源 | 形状 | 分辨率 |
|------|------|--------|
| tianzi | (6, 25, 25) | 25 km |
| fenglei | (6, 50, 50) | 3 km |
| fengwu | (6, 1, 1) | 25 km |

**处理步骤**:
1. 重采样到统一 3 km 网格（双线性插值）
2. 地形校正（温度 / 气压按 DEM 修正）
3. 时间偏差加权（最新数据权重最大）
4. 加权叠加融合

**返回**: `ndarray(6, 50, 50)` — 统一在 3 km 网格上，覆盖成都平原 150 km × 150 km

```
[0] u10    [1] v10    [2] t2m    [3] rh2m    [4] ps     [5] blh
```

---

## 阶段 2: 异常值检测与修复

*Outlier Detection — `data_pipeline/outlier_detector.py`*

**输入**: `OutlierDetector.detect_and_fix(data: ndarray(6, 50, 50))`

**检测流程**:
1. **物理范围检查** — t2m ∈ [220K, 330K]，风速 ≤ 50 m/s，ps ∈ [50 kPa, 105 kPa]，rh ∈ [0%, 100%]，blh ∈ [0 m, 3000 m]
2. **3σ 统计离群** — 鲁棒 Z-score（基于 M.A.D.）
3. **空间一致性** — 邻域 3×3 均值的 4σ 范围
4. **时序一致性** — 相邻时次跳变 > 5σ

**修复**: 邻域均值填充 + 高斯平滑补全 NaN

**返回**:
- `cleaned`: `ndarray(6, 50, 50)` — 清洗后的气象场
- `self.last_mask`: `ndarray(6, 50, 50) bool` — True 表示曾被标记为异常

---

## 阶段 3: 多模型动态融合

*Dynamic Weight Fusion — `fusion/ensemble.py`*

**输入**: `DynamicWeightFusion.fuse()`

| 模型 | 形状 | 角色 |
|------|------|------|
| fengwu_ghr | (1, 6, 50, 50) | 风乌全局背景 |
| tianzi | (1, 6, 50, 50) | 天资全球模式 |
| fenglei | (1, 6, 50, 50) | 风雷区域模式 |

**处理**:
1. 初始权重：风乌 0.15 + 天资 0.25 + 风雷 0.60
2. 各模型加权求和，不同分辨率自动双线性插值对齐
3. 权重可基于近期 RMSE 自适应更新

**物理约束** (`PhysicsConstraint.forward`):
- t2m ≥ 180 K；ps ≥ 50 000 Pa
- rh2m ∈ [0%, 100%]；blh ≥ 50 m

**返回**: `Tensor(1, 6, 50, 50)` — 物理约束后的融合场

---

## 阶段 4: CNN 空间订正

*Spatial Correction — `cnn_corrector/model.py`*

**输入**: `CNNCorrector.forward()`
- `x`: `Tensor(1, 11, 50, 50)` — 融合场 + 辅助通道
- `dem`: `Tensor(1, 1, 50, 50)` — DEM 高程

**通道布局 (x)**:
```
[0:6]  u10, v10, t2m, rh2m, ps, blh
[6]    DEM 高程
[7:11] 标准化辅助 (u10/10, t2m/300, ps/1000)
```

**模型结构**:
- **SpatialCorrector**（浅层 CNN）: Conv3×3 → BN → ReLU → Dropout → Conv3×3 → BN → ReLU → Conv1×1，含 DEM 编码残差分支
- **LSTMTemporalCorrector**（ConvLSTM 时序）: 多帧输入 → ConvLSTM × 2 层 → 输出单帧

**返回**: `Tensor(1, 6, 50, 50)` — 订正后的 3 km 网格场（已去除系统偏差）

---

## 阶段 5: 概率 U-Net 降尺度

*Probabilistic Downscaling — `unet_downscaler/probabilistic.py`*

**输入**: `ProbabilisticUNet.forward(x: Tensor(1, 6, 50, 50))`

**模型结构**:
- Encoder: DoubleConv → Down × 4 → Bottleneck
- Decoder: Up × 4 → AttentionGate（可选观测同化）
- 上采样: ConvTranspose × 3（50 → 150，×3 倍）
- 双头输出: mean_head + log_var_head

**返回**:

| 输出 | 形状 | 说明 |
|------|------|------|
| `mean` | (1, 6, 150, 150) | 预报均值，1 km 分辨率 |
| `log_var` | (1, 6, 150, 150) | 对数方差，约束在 [-5, 5]，exp(log_var) = σ² |

6 通道: u10, v10, t2m, rh2m, ps, blh — 覆盖成都平原 150 km × 150 km

---

## 阶段 6: EnKF 贝叶斯同化

*Ensemble Kalman Filter — `gpr_risk/enkf.py`*

**输入**: `EnsembleKalmanFilter.assimilate()`

| 参数 | 形状 | 说明 |
|------|------|------|
| `mean` | (6, 150, 150) | U-Net 均值 |
| `var` | (6, 150, 150) | U-Net 方差 |
| `observations` | (M,) | 新观测值（无人机回传 / 气象站） |
| `obs_positions` | (M, 2) | 观测位置 (y, x) |

**处理**:
1. `generate_ensemble(mean, var)` → 20 个集合成员: `ensemble[i] = mean + noise × σ`
2. `assimilate(ensemble, obs, pos)`:
   - a. 计算集合均值 & 扰动矩阵 X'
   - b. 观测算子 H: 网格 → 观测位置插值
   - c. 卡尔曼增益: K = P^f H^T (H P^f H^T + R)⁻¹
   - d. 每个成员: xᵃᵢ = xᶠᵢ + K (y - H xᶠᵢ)
   - e. 协方差膨胀: × 1.05（防止滤波发散）

**返回**:

| 输出 | 形状 | 说明 |
|------|------|------|
| `analysis` | (20, 6, 150, 150) | 同化后集合 |
| `analysis_mean` | (6, 150, 150) | 同化后均值场 |
| `analysis_variance` | (6, 150, 150) | 同化后方差（下降了） |

---

## 阶段 7: GPR 风险场

*Gaussian Process Risk — `gpr_risk/model.py`*

**输入**: `GPRiskEstimator.fit()` / `risk_field()`
- `residual`: `Tensor(1, 6, 150, 150)` — EnKF 同化后残差（真值 - 预测）
- `coords`: `Tensor(N, 2)` — 格点坐标（可选，默认自动生成）

**处理**:
1. 展平残差 → (N,) 训练数据
2. 拟合高斯过程（GPyTorch）:
   - 小数据量: ExactGP（RBF 核）
   - 大数据量: SparseGP（500 诱导点）
3. 预测全场的方差场

**返回**: `Tensor(1, 150, 150)` — 风险方差值，0~1 归一化

高值区域表示预报不确定性大、无人机风险高；低值区域表示预报可信、可安全通行。

---

## 阶段 8: 风险感知代价函数

*Risk-Aware Cost — `path_planning/cost_function.py`*

**输入**: `RiskCostFunction.edge_cost()`

| 参数 | 形状 | 来源 |
|------|------|------|
| `p1`, `p2` | (y, x) 坐标 | 边的两端 |
| `wind_u`, `wind_v` | (H, W) | EnKF 均值场 |
| `risk_map` | (H, W) | GPR 风险场 |
| `tke` | (H, W) | 湍流动能（可选） |
| `restricted_zones` | (H, W) | 禁飞区（可选） |
| `precipitation` | (H, W) | 降水率（可选） |

**代价公式**: Cost = α·Met + β·Energy + γ·Dist + δ·Smooth + ε·Restricted

| 分量 | 权重 | 内容 |
|------|------|------|
| Meteorological | α = 0.35 | 侧风 × 0.30，GPR 风险 × 0.20，湍流 × 0.25，热力 × 0.15，降水 × 0.10 |
| Energy | β = 0.25 | P = (mg + ½ρ·Cd·A·(v+v_headwind)²) × v，归一化到电池容量 |
| Distance | γ = 0.20 | 欧氏距离 |
| Smoothness | δ = 0.10 | 路径曲率 |
| Restricted | ε = 0.10 | 禁飞区 → × 1e6 惩罚 |

硬约束: 风速 > 12 m/s → 代价 × 1e6

**返回**: `float` — p1 → p2 的通行总代价（越小越优）

---

## 阶段 9: 风险感知 A* 路径规划

*Risk-Aware A\* — `path_planning/planner.py`*

**输入**: `GPRPathPlanner.plan()`
- `risk_map`: `ndarray(150, 150)` — GPR 风险场
- `wind_u`, `wind_v`: `ndarray(150, 150)` — 风场
- `start`, `end`: `(float, float)` — 起点 / 终点 (x, y) km

**处理**:
- `_risk_aware_astar`: f = g + h + λ·risk + μ·逆风惩罚，8 方向邻域搜索
- 禁飞区 (risk > 0.9) 标记为不可达
- 找不到路径时回退到直线
- `_bezier_smooth`: 3 阶贝塞尔曲线平滑

**返回**: `List[Waypoint]`

```
Waypoint(x: km, y: km, z: m, risk: 0~1, wind_u: m/s, wind_v: m/s)
```

---

## 阶段 10: MPC 滚动时域优化

*Model Predictive Control — `control/mpc.py`*

**输入**: `ModelPredictiveController`
- 初始化: `init(uav: UAVState, goal: Tuple)`
  - `UAVState(x, y, z, vx, vy, heading, battery, status)`
- 循环: `loop(risk_map, wind_u, wind_v, tke, restricted, precip)` — 每 10 分钟调用一次

**处理**:
1. `_check_termination()`: 到达终点 → REACHED，电量 < 20% → EMERGENCY，进入禁飞区 → EMERGENCY，迭代 > 100 → FAILED
2. `_predict_environment(horizon=6)`: 持续性衰减假设 — risk_{t+n} = risk_t × wⁿ + mean × (1-wⁿ)
3. `_predict_states(horizon)`: 匀速外推 N 步位置
4. `_optimize_trajectory(horizon)`: 每步在预测风险场上 A* 规划子路径，仅执行第一步

**返回**: `MPCTrajectory`

| 字段 | 类型 | 说明 |
|------|------|------|
| `waypoints` | `List[Waypoint]` | 未来 N 步路径 |
| `costs` | `List[float]` | 每步代价 |
| `total_cost` | `float` | 总代价 |
| `expected_arrival_s` | `float` | 预计到达时间 (s) |
| `risk_profile` | `List[float]` | 每步风险值 |

UAVState 更新后: x, y → 第一步目标；vx, vy → 朝向速度分量；battery -= 0.01；status 更新。

---

## 阶段 11: 贝叶斯主动观测决策

*Bayesian Active Observation — `active_obs/bayesian_observer.py`*

**输入**: `BayesianActiveObserver`
- `variance_map`: `ndarray(150, 150)` — GPR / EnKF 方差场
- `existing_sites`: `List[(x, y)]` — 已有观测站位置

**处理**:
1. 生成候选点（间隔 3 km）
2. 排除已有站点附近（2 km 内）
3. 按采集函数排序:
   - `"variance"` (默认): 方差最大
   - `"entropy"`: 信息熵最大
   - `"mutual_info"`: 方差 + 多样性
4. 从 top 中挑分散的 N 个点
5. `update_gpr()`: 同化新观测到 sklearn GPR

**返回**: `List[(x, y)]` — 建议采集的 N 个位置（以成都中心为原点，单位 km）

内部更新: `sampled_positions`, `sampled_values`, GPR 模型重新训练。

---

## 阶段 12: 多机冲突消解

*Multi-UAV Conflict Resolution — `multi_uav/conflict_resolver.py`*

**输入**: `MultiUAVConflictResolver`
- `agents`: `List[UAVAgent]` — 含 id, priority, x, y, z, speed, heading, path

**处理**:
1. **冲突检测**: 水平距离 < 200 m 且垂直 < 30 m，碰撞时间 < 60 s → 按紧迫度排序
2. **冲突消解**（按优先级依次尝试）:
   - a. 高度层分离（50 m 间隔）
   - b. 速度调节（低优先级减速 5 m/s）
   - c. 航向偏移（向右 15°）
3. **编队保持**: 一字纵队 / 三角队形

**返回**: `List[UAVAgent]` — 调整后的无冲突安全轨迹

---

## 阶段 13: MAVLink 输出

*PX4 / ArduPilot — `path_planning/mavlink_output.py`*

**输入**: `export_to_mavlink()`

| 参数 | 类型 | 说明 |
|------|------|------|
| `waypoints` | `List[Waypoint]` | 来自路径规划或 MPC |
| `output_type` | `"plan"` / `"mavlink"` | 输出格式 |
| `speed` | `float` | 巡航速度 m/s |
| `home` | `(lat, lon, alt)` | 起飞点 |

**处理**:
- `GeoConverter.local_to_geo(x_km, y_km, alt_m)`: lat = ref_lat + y × 0.0090，lon = ref_lon + x × 0.0097
- `MissionPlanGenerator.generate_plan()`: QGC `.plan` JSON — HOME → TAKEOFF → WAYPOINT... → TARGET → RTL
- `MAVLinkEncoder`: `goto_waypoint()` → COMMAND_LONG (#76)，`rtl()` → NAV_RETURN_TO_LAUNCH，`heartbeat()` → HEARTBEAT (#0)

**返回**:
- `"plan"` → JSON 字符串，可直接保存为 `.plan` 文件供 QGC 导入
- `"mavlink"` → MAVLink v2 帧序列（hex），可通过串口 / UDP 发送

---

*全链路 13 阶段覆盖：数据获取 → 配准 → 清洗 → 融合 → 订正 → 降尺度 → 同化 → 风险评估 → 代价建模 → 路径规划 → 滚动优化 → 主动观测 → 冲突消解 → 飞控输出*
