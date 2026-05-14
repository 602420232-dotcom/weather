# visualization

贝叶斯同化系统的可视化模块，提供同化结果的静态图表、动态动画和交互式仪表盘等多种可视化能力。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：plots、animator、dashboards |
| `plots.py` | 静态图表：同化结果对比图、方差分布图、风险热力图、不确定性分布等 |
| `animator.py` | 动画生成：时间序列同化过程的帧动画演示 |
| `dashboards.py` | 交互式仪表盘：Web 端整合展示多个可视化面板 |
| `test_plots.py` | 图表测试 |
| `test_animator.py` | 动画测试 |
| `test_dashboards.py` | 仪表盘测试 |

## 使用示例

```python
from bayesian_assimilation.visualization import (
    plots, animator, dashboards
)

# 静态图表
plots.plot_comparison(background, analysis, variance, save_path="result.png")

# 动画演示
anim = animator.create_assimilation_animation(
    time_series_background, time_series_analysis, fps=10
)
anim.save("assimilation_evolution.gif")

# 仪表盘
dashboard = dashboards.create_dashboard(analysis_data)
dashboard.serve(port=5000)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
