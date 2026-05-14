# risk_assessment

气象风险评估模块，基于同化结果与方差场信息，对气象风险进行量化评估和分级预警，为无人机路径规划提供安全保障。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`MeteorologicalRiskAssessment`、`RiskThresholds` |
| `assessor.py` | `MeteorologicalRiskAssessment`，风险评估器：分级阈值、风险指数计算、区域风险映射 |
| `test_assessor.py` | 风险评估单元测试 |

## 风险评估等级

| 等级 | 阈值（默认） | 说明 | 无人机建议 |
|------|------------|------|-----------|
| **低风险** | < 0.3 | 条件安全，可正常飞行 | 常规巡逻 |
| **中风险** | 0.3 ~ 0.6 | 需注意，可能有颠簸 | 降低速度 |
| **高风险** | 0.6 ~ 0.85 | 危险区域，避免进入 | 绕飞或返航 |
| **极高风险** | > 0.85 | 无法飞行 | 禁止进入 |

## 使用示例

```python
from bayesian_assimilation.risk_assessment import (
    MeteorologicalRiskAssessment, RiskThresholds
)

# 自定义阈值
thresholds = RiskThresholds(low=0.3, medium=0.6, high=0.85)

# 创建评估器
assessor = MeteorologicalRiskAssessment(thresholds=thresholds)

# 评估风险
risk_map = assessor.assess(analysis_field, variance_field)

# 查询特定位置风险
lat, lon, alt = 31.23, 121.47, 100.0
risk_level = assessor.query_location(lat, lon, alt)
print(f"风险等级: {risk_level}")

# 生成高风险区域多边形
danger_zones = assessor.get_danger_zones(level="high")
```

## 配置

风险评估阈值通过 `configs/*.yaml` 中的 `risk_assessment` 段配置：

```yaml
risk_assessment:
  enabled: true
  thresholds:
    low: 0.3
    medium: 0.6
    high: 0.85
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
