#!/usr/bin/env python3
"""
5D-VAR 精度对比实验脚本

对比 3D-VAR 与 5D-VAR 在相同模拟气象数据上的精度表现：
- RMSE（均方根误差）
- MAE（平均绝对误差）
- 相关系数
- 计算时间

输出 Markdown 格式报告到 docs/5dvar-accuracy-report.md
"""

from __future__ import annotations

import json
import os
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import numpy as np
import requests

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------
API_BASE_URL = os.environ.get("ALGORITHM_ENGINE_URL", "http://localhost:9095")
GRID_SIZE = 50
N_OBSERVATIONS = 200
OBS_NOISE_STD = 0.5
RANDOM_SEED = 42

# ---------------------------------------------------------------------------
# 模拟气象数据生成
# ---------------------------------------------------------------------------

def generate_synthetic_meteorological_field(shape: tuple[int, ...], seed: int = 42) -> np.ndarray:
    """生成模拟气象场（温度/气压/风场合成标量场）。"""
    rng = np.random.default_rng(seed)
    # 基础大尺度场
    x = np.linspace(0, 4 * np.pi, shape[1])
    y = np.linspace(0, 4 * np.pi, shape[0])
    X, Y = np.meshgrid(x, y)
    field = (
        np.sin(X) * np.cos(Y)
        + 0.5 * np.sin(2 * X + 1) * np.cos(0.5 * Y + 2)
        + 0.3 * rng.random(shape)
    )
    return field


def generate_background_field(true_field: np.ndarray, perturbation_scale: float = 0.3, seed: int = 43) -> np.ndarray:
    """生成背景场 = 真实场 + 已知扰动。"""
    rng = np.random.default_rng(seed)
    perturbation = rng.normal(0, perturbation_scale, true_field.shape)
    return true_field + perturbation


def generate_observations(true_field: np.ndarray, n_obs: int, noise_std: float, seed: int = 44) -> list[dict]:
    """生成带随机噪声的观测数据。"""
    rng = np.random.default_rng(seed)
    shape = true_field.shape
    observations = []
    for _ in range(n_obs):
        i = rng.integers(0, shape[0])
        j = rng.integers(0, shape[1])
        true_value = float(true_field[i, j])
        noise = rng.normal(0, noise_std)
        observations.append({
            "position": [int(i), int(j)],
            "value": true_value + noise,
            "error": noise_std,
        })
    return observations


# ---------------------------------------------------------------------------
# API 调用
# ---------------------------------------------------------------------------

def submit_algorithm(algorithm_id: str, params: dict) -> dict:
    """向 algorithm-engine 提交任务并等待结果。"""
    submit_url = f"{API_BASE_URL}/api/v1/tasks/submit"
    resp = requests.post(submit_url, json={
        "algorithm_id": algorithm_id,
        "params": params,
        "priority": 5,
    }, timeout=30)
    resp.raise_for_status()
    task = resp.json()
    task_id = task["task_id"]

    # 轮询任务状态
    status_url = f"{API_BASE_URL}/api/v1/tasks/{task_id}"
    result_url = f"{API_BASE_URL}/api/v1/tasks/{task_id}/result"
    for _ in range(120):  # 最多等待 120 秒
        time.sleep(0.5)
        status_resp = requests.get(status_url, timeout=10)
        status_resp.raise_for_status()
        status = status_resp.json()
        if status.get("status") == "completed":
            result_resp = requests.get(result_url, timeout=10)
            result_resp.raise_for_status()
            return result_resp.json()
        elif status.get("status") == "failed":
            raise RuntimeError(f"Task {task_id} failed: {status}")

    raise TimeoutError(f"Task {task_id} did not complete in time.")


# ---------------------------------------------------------------------------
# 指标计算
# ---------------------------------------------------------------------------

def compute_rmse(true_field: np.ndarray, analysis_field: np.ndarray) -> float:
    return float(np.sqrt(np.mean((true_field - analysis_field) ** 2)))


def compute_mae(true_field: np.ndarray, analysis_field: np.ndarray) -> float:
    return float(np.mean(np.abs(true_field - analysis_field)))


def compute_correlation(true_field: np.ndarray, analysis_field: np.ndarray) -> float:
    t = true_field.flatten()
    a = analysis_field.flatten()
    return float(np.corrcoef(t, a)[0, 1])


# ---------------------------------------------------------------------------
# 报告生成
# ---------------------------------------------------------------------------

def generate_report(
    results_3d: dict,
    results_5d: dict,
    true_field: np.ndarray,
    output_path: str,
) -> None:
    """生成 Markdown 格式精度对比报告。"""
    analysis_3d = np.array(results_3d["result"]["analysis_field"])
    analysis_5d = np.array(results_5d["result"]["analysis_field"])

    rmse_3d = compute_rmse(true_field, analysis_3d)
    rmse_5d = compute_rmse(true_field, analysis_5d)
    mae_3d = compute_mae(true_field, analysis_3d)
    mae_5d = compute_mae(true_field, analysis_5d)
    corr_3d = compute_correlation(true_field, analysis_3d)
    corr_5d = compute_correlation(true_field, analysis_5d)

    time_3d = results_3d["elapsed_ms"]
    time_5d = results_5d["elapsed_ms"]

    rmse_improvement = (rmse_3d - rmse_5d) / rmse_3d * 100 if rmse_3d > 0 else 0
    mae_improvement = (mae_3d - mae_5d) / mae_3d * 100 if mae_3d > 0 else 0
    corr_improvement = (corr_5d - corr_3d) / abs(corr_3d) * 100 if corr_3d != 0 else 0

    meets_threshold = rmse_improvement >= 15 or mae_improvement >= 15 or corr_improvement >= 15

    md = f"""# 5D-VAR vs 3D-VAR 精度对比实验报告

> 生成时间: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}

## 实验配置

| 参数 | 值 |
|------|-----|
| 网格尺寸 | {GRID_SIZE} x {GRID_SIZE} |
| 观测数量 | {N_OBSERVATIONS} |
| 观测噪声标准差 | {OBS_NOISE_STD} |
| 随机种子 | {RANDOM_SEED} |
| API 地址 | {API_BASE_URL} |

## 精度对比结果

| 指标 | 3D-VAR | 5D-VAR | 提升幅度 |
|------|--------|--------|----------|
| RMSE | {rmse_3d:.6f} | {rmse_5d:.6f} | {rmse_improvement:+.2f}% |
| MAE  | {mae_3d:.6f} | {mae_5d:.6f} | {mae_improvement:+.2f}% |
| 相关系数 | {corr_3d:.6f} | {corr_5d:.6f} | {corr_improvement:+.2f}% |
| 计算时间 (ms) | {time_3d:.1f} | {time_5d:.1f} | {(time_5d - time_3d) / time_3d * 100 if time_3d > 0 else 0:+.2f}% |

## 结论

- **5D-VAR 相比 3D-VAR 的 RMSE 提升**: {rmse_improvement:.2f}%
- **5D-VAR 相比 3D-VAR 的 MAE 提升**: {mae_improvement:.2f}%
- **5D-VAR 相比 3D-VAR 的相关系数提升**: {corr_improvement:.2f}%

### 是否达到 >= 15% 提升阈值?

**{'是 (Yes)' if meets_threshold else '否 (No)'}**

"""
    if meets_threshold:
        md += """5D-VAR 在至少一项关键指标上达到了 >= 15% 的精度提升，建议在生产环境中推广使用。
"""
    else:
        md += """5D-VAR 在本次实验配置下未达到 >= 15% 的综合提升阈值。建议：
1. 增加观测密度或调整观测误差参数
2. 启用 5D-VAR 的 cycling 模式
3. 提供 NMC 集合或气候态场以激活 Hybrid B 矩阵优势
"""

    md += f"""
## 原始结果摘要

### 3D-VAR
```json
{json.dumps({k: v for k, v in results_3d.items() if k != 'result'}, indent=2, ensure_ascii=False)}
```

### 5D-VAR
```json
{json.dumps({k: v for k, v in results_5d.items() if k != 'result'}, indent=2, ensure_ascii=False)}
```
"""

    Path(output_path).parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(md)
    print(f"报告已生成: {output_path}")


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def main() -> int:
    print("=" * 60)
    print("5D-VAR 精度对比实验")
    print("=" * 60)

    # 1. 生成模拟数据
    print("\n[1/4] 生成模拟气象数据...")
    true_field = generate_synthetic_meteorological_field((GRID_SIZE, GRID_SIZE), seed=RANDOM_SEED)
    background_field = generate_background_field(true_field, perturbation_scale=0.3, seed=RANDOM_SEED + 1)
    observations = generate_observations(true_field, N_OBSERVATIONS, OBS_NOISE_STD, seed=RANDOM_SEED + 2)
    print(f"  - 真实场形状: {true_field.shape}")
    print(f"  - 背景场 RMSE(相对真实场): {compute_rmse(true_field, background_field):.4f}")
    print(f"  - 观测数量: {len(observations)}")

    # 2. 检查 API 健康状态
    print(f"\n[2/4] 检查 Algorithm Engine API ({API_BASE_URL})...")
    try:
        health = requests.get(f"{API_BASE_URL}/health", timeout=5)
        health.raise_for_status()
        print(f"  - API 状态: {health.json()}")
    except Exception as exc:
        print(f"  - 警告: 无法连接 API ({exc})")
        print("  - 将降级为本地直接调用算法模块...")
        return run_local_mode(true_field, background_field, observations)

    # 3. 调用 3D-VAR
    print("\n[3/4] 执行 3D-VAR 同化...")
    params_3d = {
        "background_field": background_field.tolist(),
        "observations": observations,
        "grid_shape": list(background_field.shape),
        "resolution": 10.0,
    }
    start = time.perf_counter()
    try:
        result_3d = submit_algorithm("3dvar", params_3d)
        elapsed_3d = (time.perf_counter() - start) * 1000
        print(f"  - 完成，耗时 {elapsed_3d:.1f} ms, 迭代次数: {result_3d.get('iterations', 'N/A')}")
    except Exception as exc:
        print(f"  - 3D-VAR 调用失败: {exc}")
        return 1

    # 4. 调用 5D-VAR
    print("\n[4/4] 执行 5D-VAR 同化...")
    params_5d = {
        "background_field": background_field.tolist(),
        "observations": observations,
        "grid_shape": list(background_field.shape),
        "resolution": 10.0,
        "risk_weight": 0.1,
        "ai_correction": np.zeros_like(background_field).tolist(),
        "mode": "single",
    }
    start = time.perf_counter()
    try:
        result_5d = submit_algorithm("5dvar", params_5d)
        elapsed_5d = (time.perf_counter() - start) * 1000
        print(f"  - 完成，耗时 {elapsed_5d:.1f} ms, 迭代次数: {result_5d.get('iterations', 'N/A')}")
    except Exception as exc:
        print(f"  - 5D-VAR 调用失败: {exc}")
        return 1

    # 5. 生成报告
    results_3d = {"elapsed_ms": elapsed_3d, "result": result_3d}
    results_5d = {"elapsed_ms": elapsed_5d, "result": result_5d}

    repo_root = Path(__file__).resolve().parent.parent
    report_path = repo_root / "docs" / "5dvar-accuracy-report.md"
    generate_report(results_3d, results_5d, true_field, str(report_path))

    print("\n" + "=" * 60)
    print("实验完成")
    print("=" * 60)
    return 0


def run_local_mode(true_field: np.ndarray, background_field: np.ndarray, observations: list) -> int:
    """API 不可用时，本地直接调用算法模块。"""
    algo_engine_path = str(Path(__file__).resolve().parent.parent / "uav-platform-v2" / "python" / "algorithm-engine")
    if algo_engine_path not in sys.path:
        sys.path.insert(0, algo_engine_path)

    from app.algorithms.assimilation.three_dimensional_var import ThreeDimensionalVAR
    from app.algorithms.assimilation.five_dimensional_var import FiveDimensionalVAR

    print("\n[本地模式] 直接调用算法模块...")

    params = {
        "background_field": background_field.tolist(),
        "observations": observations,
        "grid_shape": list(background_field.shape),
        "resolution": 10.0,
    }

    # 3D-VAR
    start = time.perf_counter()
    algo_3d = ThreeDimensionalVAR({"grid_shape": background_field.shape})
    result_3d = algo_3d.assimilate(params)
    elapsed_3d = (time.perf_counter() - start) * 1000
    print(f"  3D-VAR 完成，耗时 {elapsed_3d:.1f} ms")

    # 5D-VAR
    params_5d = {
        **params,
        "risk_weight": 0.1,
        "ai_correction": np.zeros_like(background_field).tolist(),
        "mode": "single",
    }
    start = time.perf_counter()
    algo_5d = FiveDimensionalVAR({"grid_shape": background_field.shape})
    result_5d = algo_5d.assimilate(params_5d)
    elapsed_5d = (time.perf_counter() - start) * 1000
    print(f"  5D-VAR 完成，耗时 {elapsed_5d:.1f} ms")

    results_3d = {"elapsed_ms": elapsed_3d, "result": result_3d}
    results_5d = {"elapsed_ms": elapsed_5d, "result": result_5d}

    repo_root = Path(__file__).resolve().parent.parent
    report_path = repo_root / "docs" / "5dvar-accuracy-report.md"
    generate_report(results_3d, results_5d, true_field, str(report_path))

    print("\n" + "=" * 60)
    print("实验完成 (本地模式)")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    sys.exit(main())
