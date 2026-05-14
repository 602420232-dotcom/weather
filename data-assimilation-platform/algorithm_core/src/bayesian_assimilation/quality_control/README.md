# quality_control

数据质量控制模块，在数据进入同化流程之前对观测数据进行校验和清洗，确保输入数据的可靠性。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`MeteorologicalQualityControl` |
| `validator.py` | `MeteorologicalQualityControl`，气象数据质量控制器：异常值检测、缺失值处理、物理约束校验 |
| `test_validator.py` | 质量控制单元测试 |

## 功能列表

- **异常值检测**：基于统计阈值（Z-score、IQR）识别离群观测
- **缺失值处理**：标记、插值或剔除缺损记录
- **物理一致性校验**：气象要素间的物理关系约束（如风速不能为负）
- **空间一致性检查**：邻近观测点的空间连续性验证

## 使用示例

```python
from bayesian_assimilation.quality_control import MeteorologicalQualityControl

qc = MeteorologicalQualityControl(threshold=3.0)

# 执行质量控制
cleaned_obs, flags = qc.validate(observations, obs_locations)

# 查看被标记的异常数据
outliers = qc.get_outliers()
print(f"检测到 {len(outliers)} 个异常点")
```

## 配置

质量控制阈值通过 `configs/*.yaml` 中的 `quality_control` 段配置：

```yaml
quality_control:
  enabled: true
  outlier_threshold: 3.0
  missing_value_threshold: 0.1
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
