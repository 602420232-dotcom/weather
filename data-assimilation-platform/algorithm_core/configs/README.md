# configs

算法核心配置文件目录，管理不同运行环境下的参数设定。配置文件采用 YAML 格式，支持分层覆盖和环境切换。

## 主要文件

| 文件 | 说明 |
|------|------|
| `default.yaml` | 默认配置文件，包含所有配置项的完整定义和默认值 |
| `development.yaml` | 开发环境配置，覆盖默认值以适应本地开发场景 |
| `production.yaml` | 生产环境配置，覆盖默认值以适应生产部署场景 |

## 配置结构

```yaml
assimilation:       # 同化核心参数（方法、网格、分辨率、迭代等）
errors:             # 误差配置（背景/观测误差尺度、协方差类型）
parallel:           # 并行计算配置（启用状态、Worker 数、后端选择）
data:               # 数据配置（输入/输出目录、格式、压缩）
quality_control:    # 质量控制（异常值阈值、缺失值阈值）
risk_assessment:    # 风险评估（低/中/高阈值）
visualization:      # 可视化配置（输出格式、DPI、是否显示图形）
logging:            # 日志配置（级别、格式、文件路径）
gpu:                # GPU 加速配置（启用状态、设备编号）
```

## 使用方法

```python
from bayesian_assimilation.utils.config import ConfigFactory

# 加载指定环境配置
config = ConfigFactory.load("production")

# 加载默认配置
config = ConfigFactory.load("default")
```

```bash
# CLI 方式
assimilate run --config configs/production.yaml
```

## 环境切换

通过环境变量 `ASSIMILATION_ENV` 控制加载哪个配置文件，优先级：环境变量 > 命令行参数 > 默认配置。

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
