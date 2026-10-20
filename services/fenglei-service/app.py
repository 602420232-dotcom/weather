"""
FengLei Weather Service API
风雷区域模式数据服务 - 基于 CMA GRAPES_MESO 中尺度模式
提供高分辨率(3km)区域气象预报数据
"""
import os
import logging
from datetime import datetime
from typing import Optional, Dict, List
from pathlib import Path

import numpy as np
from fastapi import FastAPI, HTTPException, Depends
from fastapi.security import APIKeyHeader
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="FengLei Weather Service", version="1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ORIGINS", "*").split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

API_KEY = os.getenv("FENGLEI_API_KEY", "")
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


async def get_api_key(api_key: str = Depends(api_key_header)):
    if not API_KEY:
        return None
    if api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API Key")
    return api_key


# ─── 数据来源真实性（★ 2026-09-25 修复）──────────────────────────────────────
# 此前本服务**每个端点都返回 `np.random.normal` 造的场**，而且坐标对成都
# （30.67N/104.07E）、还标着 `resolution_km: 3`，**看起来完全像真数据** ——
# 包括自称 "current analysis" 的 `/api/v1/analysis`。
# 而它在 `paper/新模型.md` 里的角色是**权重 0.60 的"区域高分辨率核心"**。
#
# ⇒ 现在改成：**没有真实数据源就显式 503**，绝不返回看起来合理的随机场。
#   随机场只在 `FENGLEI_DEMO=1` 时可用，且响应里必须带 `data_source="demo"`。
_DEMO = os.getenv("FENGLEI_DEMO", "") == "1"
_REAL_SOURCE = os.getenv("FENGLEI_SOURCE_URL", "")


def _require_data(what: str) -> str:
    """没有真实源就明确报不可用。**不返回占位数据。**"""
    if _REAL_SOURCE:
        return "real"
    if _DEMO:
        return "demo"
    raise HTTPException(status_code=503, detail={
        "success": False,
        "data_source": "unavailable",
        "reason": "%s：本服务未配置真实数据源，因此**不提供数据**"
                  "（此前这里返回的是 np.random.normal 造的随机场）" % what,
        "how_to_enable": [
            "配置 FENGLEI_SOURCE_URL 指向风雷 / CMA 产品接口",
            "或设 FENGLEI_DEMO=1 显式启用随机场 —— **仅供前后端联调，"
            "不得用于演示、论文或任何结论**",
        ],
    })


class ForecastRequest(BaseModel):
    fcst_hour: int = 0
    variables: Optional[List[str]] = None


class ForecastResponse(BaseModel):
    source: str
    forecast_time: str
    variables: List[str]
    shape: List[int]
    data: Dict[str, List[List[float]]]


def ensure_cache():
    cache_dir = os.getenv("FENGLEI_CACHE_DIR", "/app/cache")
    Path(cache_dir).mkdir(parents=True, exist_ok=True)
    return cache_dir


def generate_mock_data(fcst_hour: int = 0) -> dict:
    """⚠️ **随机场，只在 FENGLEI_DEMO=1 时可达。**

    ★ 保留函数名是为了不破坏既有 import；但它现在**不可能**在非 demo 模式下
      被调用 —— 所有端点都会先走 `_require_data()`。
      响应里也必须带 `data_source="demo"`，让下游一眼看得出。
    """
    if not _DEMO:
        raise HTTPException(status_code=503, detail={
            "success": False, "data_source": "unavailable",
            "reason": "随机场被禁用（未设 FENGLEI_DEMO=1）",
        })
    ny, nx = 50, 50
    np.random.seed(42 + fcst_hour)

    lat_center = 30.67
    lon_center = 104.07

    lat = np.linspace(lat_center - 0.75, lat_center + 0.75, ny)
    lon = np.linspace(lon_center - 0.75, lon_center + 0.75, nx)

    u10 = np.random.normal(1.5, 2.0, (ny, nx)).tolist()
    v10 = np.random.normal(0.5, 1.5, (ny, nx)).tolist()
    t2m = (np.random.normal(20, 5, (ny, nx)) + 273.15).tolist()
    rh2m = np.clip(np.random.normal(70, 15, (ny, nx)), 0, 100).tolist()
    ps = np.random.normal(1013, 5, (ny, nx)).tolist()
    blh = np.clip(np.random.normal(500, 200, (ny, nx)), 50, 3000).tolist()

    return {
        "latitude": lat.tolist(),
        "longitude": lon.tolist(),
        "u10": u10,
        "v10": v10,
        "t2m": t2m,
        "rh2m": rh2m,
        "ps": ps,
        "blh": blh,
    }


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "fenglei-service",
        "timestamp": datetime.now().isoformat(),
    }


@app.get("/health/ready")
async def readiness_check():
    return {
        "status": "ready",
        "service": "fenglei-service",
        "timestamp": datetime.now().isoformat(),
    }


@app.get("/api/v1/status")
async def get_status():
    """
    Get service configuration status.
    Provides information for frontend to display configuration prompts.
    """
    return {
        "service": "fenglei-service",
        "status": "healthy",
        "api_key_configured": bool(API_KEY),
        # ★★ 关键更正：旧版用 `api_key_configured` 来推 mode，
        #   于是"配了 API Key"就显示 production / full functionality ——
        #   **但数据依然是 np.random.normal 造的**。密钥管的是"谁能访问"，
        #   与"数据是真是假"完全无关。现在把这两件事分开报。
        "auth_mode": "production" if API_KEY else "open",
        "data_source": ("real" if _REAL_SOURCE else
                        "demo(随机场)" if _DEMO else "unavailable"),
        "real_source_configured": bool(_REAL_SOURCE),
        "demo_random_enabled": _DEMO,
        "message": ("已配置真实数据源" if _REAL_SOURCE else
                    "⚠️ 未配置真实数据源 ⇒ 数据端点返回 503；"
                    "随机场仅在 FENGLEI_DEMO=1 时可用" if not _DEMO else
                    "⚠️ 处于 DEMO 模式：返回的是**随机场**，"
                    "不得用于演示/论文/任何结论"),
        "action_required": not _REAL_SOURCE,
        "action_message": (None if _REAL_SOURCE else
                           "请配置 FENGLEI_SOURCE_URL 接入风雷/CMA 产品接口"),
        "timestamp": datetime.now().isoformat(),
    }


@app.get("/api/v1/model/info")
async def get_model_info(_: str = Depends(get_api_key)):
    return {
        "model": "GRAPES_MESO",
        "name": "FengLei",
        "resolution_km": 3,
        "variables": ["u10", "v10", "t2m", "rh2m", "ps", "blh"],
        "update_interval_min": int(os.getenv("FENGLEI_UPDATE_INTERVAL", 30)),
        "fcst_hours": [0, 1, 3, 6, 12, 24],
        "levels": ["1000", "925", "850", "700", "500", "300"],
    }


@app.post("/api/v1/forecast")
async def get_forecast(request: ForecastRequest, _: str = Depends(get_api_key)):
    # ★ 必须在 try 之外：HTTPException 也是 Exception，放进 try 会被
    #   下面那个 `except Exception` 吞成 500 + 字符串化的 dict。
    data_source = _require_data("风雷 3km 区域预报")
    try:
        data = generate_mock_data(request.fcst_hour)

        selected_vars = request.variables or ["u10", "v10", "t2m", "rh2m", "ps", "blh"]

        filtered_data = {k: v for k, v in data.items() if k in selected_vars}

        return {
            "source": "fenglei",
            "data_source": data_source,
            "forecast_time": datetime.now().isoformat(),
            "fcst_hour": request.fcst_hour,
            "variables": selected_vars,
            "shape": [50, 50],
            "resolution_km": 3,
            "data": filtered_data,
            "latitude": data["latitude"],
            "longitude": data["longitude"],
        }
    except Exception as e:
        logger.error(f"Forecast error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/forecast/wind-field")
async def get_wind_field(fcst_hour: int = 0, _: str = Depends(get_api_key)):
    data_source = _require_data("风雷风场")
    data = generate_mock_data(fcst_hour)
    return {
        "source": "fenglei",
        "data_source": data_source,
        "forecast_time": datetime.now().isoformat(),
        "fcst_hour": fcst_hour,
        "wind_u": data["u10"],
        "wind_v": data["v10"],
        "latitude": data["latitude"],
        "longitude": data["longitude"],
    }


@app.get("/api/v1/analysis")
async def get_analysis(_: str = Depends(get_api_key)):
    # ★ 这个端点自称 "current analysis"（当前实况），返回的却是随机场 ——
    #   是整个服务里最危险的一个：下游会把它当成实况用。
    data_source = _require_data("风雷当前实况分析（current analysis）")
    data = generate_mock_data(0)
    return {
        "analysis_type": "current",
        "data_source": data_source,
        "timestamp": datetime.now().isoformat(),
        "variables": {
            "temperature_2m": data["t2m"],
            "humidity_2m": data["rh2m"],
            "wind_speed": np.sqrt(np.array(data["u10"]) ** 2 + np.array(data["v10"]) ** 2).tolist(),
        },
        "latitude": data["latitude"],
        "longitude": data["longitude"],
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8091)
