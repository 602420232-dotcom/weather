from typing import Dict, List, Any


def validate_grid(grid: Dict[str, Any]) -> list:
    errors = []
    if "lat" not in grid:
        errors.append("缺少 lat 字段")
    if "lon" not in grid:
        errors.append("缺少 lon 字段")
    if "lev" not in grid:
        errors.append("缺少 lev 字段")
    lat = grid.get("lat", [])
    lon = grid.get("lon", [])
    lev = grid.get("lev", [])
    if len(lat) == 0:
        errors.append("lat 不能为空")
    if len(lon) == 0:
        errors.append("lon 不能为空")
    if len(lev) == 0:
        errors.append("lev 不能为空")
    return errors


def validate_observations(observations: List[Dict]) -> list:
    errors = []
    for i, obs in enumerate(observations):
        if "value" not in obs:
            errors.append(f"观测 {i}: 缺少 value")
        if "lat" not in obs or "lon" not in obs:
            errors.append(f"观测 {i}: 缺少位置信息")
    return errors


def validate_assimilation_request(request: Dict) -> list:
    errors = []
    algorithm = request.get("algorithm")
    valid_algorithms = ["3dvar", "4dvar", "enkf", "hybrid"]
    if algorithm and algorithm.lower() not in valid_algorithms:
        errors.append(f"不支持的算法: {algorithm}，可选: {valid_algorithms}")
    background = request.get("background", {})
    if background:
        errors.extend(validate_grid(background.get("grid", {})))
    observations = request.get("observations", [])
    if observations:
        errors.extend(validate_observations(observations))
    return errors
