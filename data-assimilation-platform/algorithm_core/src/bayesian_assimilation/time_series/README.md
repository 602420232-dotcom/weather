# time_series

时间序列分析模块，对气象同化数据进行时序建模与分析，支持趋势检测、周期性分析和异常检测。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`TimeSeriesAnalyzer` |
| `analyzer.py` | `TimeSeriesAnalyzer`，时间序列分析器：趋势分析、周期检测、异常点识别、预测 |
| `test_analyzer.py` | 时间序列分析单元测试 |

## 功能列表

- **趋势检测**：线性/非线性趋势提取
- **周期分析**：频谱分析，识别日/季节周期
- **异常检测**：时序异常点自动标记
- **平滑滤波**：移动平均、指数平滑
- **短期预测**：基于 ARIMA/指数平滑的延伸预测

## 使用示例

```python
from bayesian_assimilation.time_series import TimeSeriesAnalyzer

analyzer = TimeSeriesAnalyzer()

# 拟合时间序列
analyzer.fit(timestamps, data_values)

# 趋势分析
trend = analyzer.detect_trend()
print(f"趋势: {trend.slope:.4f} 每时间步")

# 异常检测
anomalies = analyzer.detect_anomalies(threshold=3.0)

# 预测未来值
forecast = analyzer.forecast(steps=10)
```

## 典型应用场景

- WRF 模式输出时序的同化前预处理
- 无人机飞行路径上的气象趋势预判
- 浮标/地面站长期观测的趋势监

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
