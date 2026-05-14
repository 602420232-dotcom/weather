# api

贝叶斯同化系统的对外接口层，提供命令行界面（CLI）、REST API 和 Web 可视化界面的统一入口。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：CLI、REST API、Web 应用 |
| `cli.py` | `AssimilationCLI`，命令行界面，支持 `run`、`quality-control`、`risk-assessment` 等子命令 |
| `rest.py` | `AssimilationAPI`，基于 FastAPI 的 RESTful API 接口 |
| `web.py` | `create_app()`，Web 可视化界面（含仪表盘、图表） |
| `test_rest.py` | REST API 测试 |
| `test_web.py` | Web 界面测试 |

## CLI 使用

```bash
assimilate --help

# 运行同化
assimilate run --config configs/default.yaml

# 质量控制
assimilate quality-control --input data/observations.nc

# 风险评估
assimilate risk-assessment --input data/analysis.nc
```

## REST API 使用

```bash
uvicorn bayesian_assimilation.api.rest:app --host 0.0.0.0 --port 8000
```

```python
import requests

response = requests.post("http://localhost:8000/assimilate", json={
    "background": [...],
    "observations": [...]
})
```

## Web 界面

```python
from bayesian_assimilation.api.web import create_app

app = create_app()
app.run(host="0.0.0.0", port=5000)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
