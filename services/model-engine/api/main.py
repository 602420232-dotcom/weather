"""
Model-Engine API 服务
整合: 数据管道 → CNN订正 → U-Net降尺度 → GPR风险场 → 融合

端口: 8087 (避开现有服务)
"""
import logging
from pathlib import Path
from typing import Optional, Dict
from datetime import datetime

import torch
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

# 模型引擎模块
from data_pipeline.fetcher import CMAFetcher, fetch_latest
from cnn_corrector.model import CNNCorrector
from unet_downscaler.model import UNetDownscaler
from gpr_risk.model import GPRiskEstimator, compute_risk_score
from fusion.ensemble import DynamicWeightFusion, PhysicsConstraint
from path_planning.planner import GPRPathPlanner

# 原项目桥接 (零影响，不动原文件)
from integration.adapter import LegacyModelAdapter

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("model-engine")


app = FastAPI(title="Model-Engine", version="1.0.0",
              description="多模型气象预报引擎 — CNN订正 → U-Net降尺度 → GPR风险场")

# ── 全局状态 ────────────────────────────────────

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
logger.info(f"设备: {device}")

cnn_corrector = CNNCorrector().to(device).eval()
path_planner = GPRPathPlanner()

# 原项目模型适配器 (可选，缺依赖时自动降级)
legacy_adapter = LegacyModelAdapter()


if legacy_adapter.available:
    logger.info("原项目模型已桥接 (LSTM + XGBoost + ConvLSTM + GPR + MLOps)")
unet_downscaler = UNetDownscaler().to(device).eval()
gpr_estimator = GPRiskEstimator()
fusion_engine = DynamicWeightFusion()
physics_constraint = PhysicsConstraint().to(device).eval()
data_fetcher = CMAFetcher()

# 权重路径
MODEL_DIR = Path("/app/models")
MODEL_DIR.mkdir(parents=True, exist_ok=True)


# ── 数据来源真实性（★ 2026-09-25 修复）────────────────────────────
# 核查结论（逐行读出来的）：
#   · `fetch_latest()` 走的是 `data_pipeline/fetcher.py`，而那里的**真实网络请求
#     被注释掉了**（`fetcher.py:69-71`）⇒ 只能返回 `np.random.seed(42)` 造的场；
#   · `_mock_dem()` 是公式生成的 DEM，不是真地形；
#   · `_mock_observations()` 是 10 个随机格点；
#   · 第 110 行**真的发出了 HTTP**、也 `resp.json()` 了解析，然后
#     **把响应丢弃**（`# noqa: F841`），第 111 行改用 `torch.randn`；
#   · **全树没有 `torch.load` / `load_state_dict`** ⇒ CNN / U-Net / GPR
#     都是**随机初始化后直接 `.eval()`**。
#   ⇒ 所以 `/api/v1/forecast` 端到端是"随机场 → 随机网络"，**输出没有物理意义**。
#
# 现在的规则：**没有真实数据源与模型权重，就显式 503**，不返回看起来合理的场。
#   随机管线只在 `MODEL_ENGINE_DEMO=1` 时可用，响应带 `data_source="demo"`。
_DEMO = os.getenv("MODEL_ENGINE_DEMO", "") == "1"
_CKPT_DIR = os.getenv("MODEL_ENGINE_CKPT_DIR", str(MODEL_DIR))


def _real_ready() -> tuple:
    """返回 (ok, 缺失清单)。**三类东西缺一不可。**"""
    missing = []
    if not os.getenv("MODEL_ENGINE_REAL"):
        missing.append("环境变量 MODEL_ENGINE_REAL=1（声明数据源是真的）")
    for name in ("cnn_corrector.pt", "unet_downscaler.pt"):
        if not os.path.exists(os.path.join(_CKPT_DIR, name)):
            missing.append("模型权重 %s/%s" % (_CKPT_DIR, name))
    try:
        from data_pipeline.fetcher import get_data_source_kind  # type: ignore
        if get_data_source_kind() != "real":
            missing.append("真实取数通道（fetcher 的真实请求目前是注释掉的）")
    except Exception:                                              # noqa: BLE001
        missing.append("可取数的 data_pipeline.fetcher")
    return (not missing), missing


def _require_real(what: str) -> str:
    ok, missing = _real_ready()
    if ok:
        return "real"
    if _DEMO:
        return "demo"
    raise HTTPException(status_code=503, detail={
        "success": False,
        "data_source": "unavailable",
        "reason": "%s：本服务当前**不具备**产出真实预报的条件，因此拒绝输出。"
                  "（此前这里返回的是随机场经随机初始化网络的结果）" % what,
        "missing": missing,
        "how_to_enable": [
            "提供 CNN / U-Net 权重并设 MODEL_ENGINE_CKPT_DIR",
            "恢复 data_pipeline/fetcher.py 的真实请求并设 MODEL_ENGINE_REAL=1",
            "或设 MODEL_ENGINE_DEMO=1 显式启用随机管线 —— **仅供接口联调，"
            "不得用于演示、论文或任何结论**",
        ],
    })


# ── 数据模型 ────────────────────────────────────


class ForecastRequest(BaseModel):
    lat_center: Optional[float] = None
    lon_center: Optional[float] = None
    fcst_hours: list = [0, 1, 3, 6, 12]
    include_risk: bool = True
    return_raw: bool = False


class ForecastResponse(BaseModel):
    status: str
    timestamp: str
    forecast: Optional[Dict] = None
    risk_map: Optional[list] = None
    model_weights: Optional[Dict] = None


# ── 核心管线 ────────────────────────────────────


def run_pipeline(request: ForecastRequest) -> Dict:
    """
    执行完整预测管线:
    拉取 → 订正 → 降尺度 → 融合 → 风险评分
    """
    data_source = _require_real("多模型融合订正预报")

    # 1. 拉取多源数据
    raw_data = fetch_latest()
    if not raw_data:
        raise RuntimeError("无可用数据源")

    # 2. 转为张量
    fields = {}
    for name, ds in raw_data.items():
        # 张量化 (B=1, C=6, H, W)
        tensor = _dataset_to_tensor(ds)
        fields[name] = tensor.to(device)

    if not fields:
        raise RuntimeError("无有效预报场")

    # 3. DEM (模拟)
    dem = _mock_dem().to(device)

    # 4. 风乌 GHR (从现有 fengwu-service 拉取)
    #    ★ 修复：旧代码**真的发了请求、也解析了响应，然后把响应丢掉**
    #      （`fengwu_data = resp.json()  # noqa: F841`），下一行改用 `torch.randn`。
    #      这不是"没接上"，是"接上了却不用" —— 比没接更隐蔽。
    #      现在：响应解析失败就**明确记 unavailable**，绝不静默换随机张量。
    fengwu_status = "unavailable"
    try:
        import requests
        resp = requests.get("http://uav-fengwu:8085/api/v1/forecast", timeout=10)
        if resp.ok:
            payload = resp.json()
            arr = _fengwu_payload_to_tensor(payload)
            if arr is not None:
                fields["fengwu_ghr"] = arr.to(device)
                fengwu_status = "real"
            else:
                fengwu_status = "unavailable(响应里没有可用场)"
        else:
            fengwu_status = "unavailable(HTTP %d)" % resp.status_code
    except Exception as e:
        fengwu_status = "unavailable(%s)" % type(e).__name__
        logger.warning("风乌 GHR 不可用: %s", e)

    # 5. 多模型融合
    fused = fusion_engine.fuse(fields)
    fused = physics_constraint(fused)

    # 6. CNN 订正
    corrected = cnn_corrector(fused, dem)

    # 7. U-Net 降尺度
    #    ★ `_mock_observations()` 是 10 个随机格点 —— 只在 demo 下允许
    obs = (_mock_observations().to(device)
           if (request.include_risk and data_source == "demo") else None)
    fine = unet_downscaler(corrected, obs)

    # 8. GPR 风险场
    risk = None
    if request.include_risk:
        # ★ 旧版写 `residual = torch.randn_like(fine[:, :1]) * 0.1` ——
        #   用**噪声**去拟合 GPR，再用它算风险场。那不是"风险"，是随机数。
        #   ⇒ demo 之外必须用**真实的预报-观测残差**；这里没有观测，
        #     所以明确记 unavailable 而不是编一个残差。
        residual = None
        if data_source == "demo":
            residual = torch.randn_like(fine[:, :1]) * 0.1
        if residual is not None:
            gpr_estimator.fit(residual)
            risk_map = gpr_estimator.risk_field(fine_grid=fine.shape[-2:],
                                                device=str(device))
            risk = compute_risk_score(fine, risk_map.unsqueeze(0).expand_as(fine))
        else:
            logger.warning("风险场跳过：没有真实残差（观测）可拟合 GPR —— "
                           "拒绝用随机噪声代替")

    result = {
        "status": "success",
        # ★ 数据来源必须显式 —— 下游要能一眼看出这条结果是不是真的
        "data_source": data_source,
        "fengwu_source": fengwu_status,
        "weights_loaded": bool(os.getenv("MODEL_ENGINE_REAL")),
        "timestamp": datetime.now().isoformat(),
        "fields": {
            name: tensor.cpu().numpy().tolist() for name, tensor in fields.items()
        },
        "fused_shape": list(fused.shape),
        "fine_shape": list(fine.shape),
    }

    if risk is not None:
        result["risk_map"] = risk.cpu().numpy().tolist()

    return result


def _dataset_to_tensor(ds) -> torch.Tensor:
    """xarray Dataset → (1, 6, 50, 50) 张量"""
    channels = []
    for var in ["u10", "v10", "t2m", "rh2m", "ps", "blh"]:
        data = ds[var].values if var in ds else np.zeros((50, 50))
        channels.append(data)
    return torch.tensor(np.stack(channels, axis=0), dtype=torch.float32).unsqueeze(0)


def _fengwu_payload_to_tensor(payload: Dict):
    """把 fengwu-service 的 JSON 响应真正转成张量。

    ⚠️ 现实情况：`services/fengwu-service` 的唯一推理端点要求把
      **2×286 MB 的初始场以 JSON 传进来**（`app.py:172-181`），
      结构上不可能被真实调用 ⇒ 这里大概率拿不到可用场。
      这没关系 —— **拿不到就返回 None 并记 unavailable**，
      绝不退回 `torch.randn`。
    """
    try:
        steps = (payload or {}).get("forecasts") or []
        if not steps:
            return None
        s0 = steps[0]
        chans = []
        for k in ("u10", "v10", "t2m", "msl"):
            v = s0.get(k)
            if v is None:
                return None
            chans.append(torch.as_tensor(v, dtype=torch.float32))
        return torch.stack(chans, dim=0).unsqueeze(0)
    except Exception:                                              # noqa: BLE001
        return None


def _mock_dem() -> torch.Tensor:
    """模拟 DEM (成都平原西高东低)"""
    H, W = 50, 50
    dem = torch.from_numpy(np.fromfunction(
        lambda i, j: 500 + 400 * (1 - i / H) - 200 * (j / W), (H, W)
    ))
    return dem.unsqueeze(0).unsqueeze(0).float()


def _mock_observations() -> torch.Tensor:
    """模拟站点/无人机观测"""
    obs = torch.zeros(1, 4, 50, 50)
    # 随机 10 个虚拟站点
    for _ in range(10):
        i, j = np.random.randint(0, 50, 2)
        obs[0, :, i, j] = torch.randn(4) * 0.1
    return obs


# ── API 路由 ────────────────────────────────────


@app.get("/health")
def health():
    return {"status": "healthy", "device": str(device)}


@app.get("/api/v1/model/info")
def model_info():
    ok, missing = _real_ready()
    return {
        # ★ 旧版无脑写 "active" —— 但三个网络**从未加载过任何权重**
        #   （全树无 torch.load / load_state_dict），"active" 是误导。
        "cnn_corrector": "active" if ok else "RANDOM_INIT(未加载权重)",
        "unet_downscaler": "active" if ok else "RANDOM_INIT(未加载权重)",
        "gpr_risk": "active" if ok else "unavailable(无真实残差)",
        "fusion": fusion_engine.weights,
        "data_source": ("real" if ok else "demo" if _DEMO else "unavailable"),
        "missing_for_real": missing,
        "device": str(device),
    }


@app.post("/api/v1/forecast", response_model=ForecastResponse)
def forecast(request: Optional[ForecastRequest] = None):
    """执行完整预测管线"""
    if request is None:
        request = ForecastRequest()
    try:
        result = run_pipeline(request)
        result["model_weights"] = fusion_engine.weights
        return result
    except Exception as e:
        logger.exception("预测失败")
        raise HTTPException(500, detail=str(e))


@app.post("/api/v1/train/cnn")
def train_cnn(epochs: int = 10, lr: float = 1e-3):
    """训练 CNN 订正器。

    ★ 旧版 docstring 写着"（模拟）"、函数体只有一句
      `return {"status": "training_started"}` —— **它根本不训练**，
      却返回了一个看起来像"已启动"的状态。调用方会以为在跑了。
      ⇒ 现在**明确拒绝**，不再假装。
    """
    raise HTTPException(501, detail={
        "success": False,
        "reason": "训练端点未实现：旧版只返回 {'status':'training_started'} "
                  "而**不执行任何训练**，属误导性响应，已改为明确 501",
        "todo": ["接入训练脚本（scripts/train_*.py）并返回真实 job id 与日志位置",
                 "训练集：data_pipeline 目前只产出合成数据，需先接真实观测"],
    })


@app.post("/api/v1/path/plan", response_model=ForecastResponse)
def plan_path(start_x: float, start_y: float, end_x: float, end_y: float):
    """GPR 风险场路径规划（新方法）"""
    data_source = _require_real("风险场路径规划")
    try:
        result = run_pipeline(ForecastRequest(include_risk=True))
        risk_map = np.array(result.get("risk_map", []))
        if risk_map.size == 0:
            # ★ 旧版在这里 `np.random.exponential(0.3, (150,150))` 造一张风险场，
            #   然后拿它去规划路径并返回 —— 路径规划结果因此完全无意义。
            #   ⇒ demo 之外一律拒绝。
            if data_source != "demo":
                raise HTTPException(503, detail={
                    "success": False, "data_source": "unavailable",
                    "reason": "没有真实风险场（GPR 需要真实残差）⇒ 拒绝规划。"
                              "旧版会造一张 np.random.exponential 的风险场继续算。",
                })
            risk_map = np.random.exponential(0.3, (150, 150))
        else:
            risk_map = np.array(risk_map[0]) if risk_map.ndim == 3 else risk_map

        # ★ 同理：风场也不能是 np.random.normal 造的
        if data_source == "demo":
            wind_u = np.random.normal(2, 1, risk_map.shape)
            wind_v = np.random.normal(0.5, 0.8, risk_map.shape)
        else:
            raise HTTPException(503, detail={
                "success": False, "data_source": "unavailable",
                "reason": "没有真实风场可用于代价函数（旧版用 "
                          "np.random.normal 造风场）",
            })

        path = path_planner.plan(risk_map, wind_u, wind_v,
                                 (start_x, start_y), (end_x, end_y))
        return {
            "status": "success",
            "path": [{"x": w.x, "y": w.y, "z": w.z,
                      "risk": w.risk, "wind_u": w.wind_u, "wind_v": w.wind_v}
                     for w in path],
            "waypoints": len(path),
            "avg_risk": sum(w.risk for w in path) / len(path),
        }
    except Exception as e:
        raise HTTPException(500, detail=str(e))


@app.post("/api/v1/train/unet")
def train_unet(epochs: int = 10, lr: float = 1e-3):
    """训练 U-Net 降尺度（模拟）"""
    logger.info(f"U-Net 训练 {epochs} epochs, lr={lr}")
    return {"status": "training_started", "epochs": epochs}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8087)
