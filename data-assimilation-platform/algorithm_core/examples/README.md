# examples

算法核心的示例代码集合，覆盖从基础用法到高级场景的完整演示，帮助用户快速上手和掌握贝叶斯数据同化系统。

## 主要文件

| 文件 | 说明 |
|------|------|
| `basic_usage.py` | 基础使用示例：3D-VAR 同化流程、网格初始化、数据生成、结果可视化 |
| `advanced_usage.py` | 高级用法示例：多算法切换、参数调优、自定义配置 |
| `demos.py` | 演示集锦：多种同化算法对比展示 |
| `real_world_case.py` | 真实场景案例：WRF 数据加载、完整同化流水线 |
| `real_world_case_refactored.py` | 重构后的真实案例：模块化、可配置的真场景流程 |
| `real_world_case_simple.py` | 简化版真实案例：去除复杂依赖，快速演示 |
| `parallel_demo.py` | 并行计算演示：Dask 分布式同化 |
| `fair_parallel_demo.py` | 公平调度并行演示 |
| `cuda_acceleration.py` | CUDA 加速示例 |
| `gpu_acceleration.py` | GPU 加速示例（基于 CuPy/PyCUDA） |
| `test_adapters.py` | 数据适配器测试演示 |
| `default_config.json` | 示例用 JSON 配置文件 |
| `TESTING.md` | 示例测试说明文档 |
| `Jenkinsfile` | CI/CD 流水线定义 |
| `结果分析.md` | 运行结果分析说明 |

## 子目录

| 目录 | 说明 |
|------|------|
| `jupyter/` | Jupyter Notebook 教程：3D-VAR 理论、4D-VAR 理论、EnKF 演示、方差分析 |
| `output/` | 示例运行输出结果（同化摘要、观测数据 JSON） |
| `templates/` | 测试报告 HTML 模板 |

## Jupyter 教程

```bash
cd examples/jupyter
jupyter notebook

# 按顺序学习：
# 1. first_Three_Dimensional_VAR_theory.ipynb   - 3D-VAR 理论基础
# 2. second_Four_Dimensional_VAR_theory.ipynb  - 4D-VAR 理论基础
# 3. third_enkf_demo.ipynb                     - EnKF 演示
# 4. fourth_variance_analysis.ipynb             - 方差分析
```

## 运行示例

```bash
cd algorithm_core

# 基础示例
python examples/basic_usage.py

# 并行演示
python examples/parallel_demo.py

# GPU 加速示例（需要 CUDA 环境）
python examples/gpu_acceleration.py
```

## 注意事项

- 部分示例依赖可选包（matplotlib、tqdm 等），缺失时会自动降级处理
- `output/` 目录下的 `.json` 文件为历史运行产物，`.gitignore` 建议忽略

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
