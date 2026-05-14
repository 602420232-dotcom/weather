# docs

贝叶斯数据同化平台的文档中心，提供系统架构、开发指南、API 参考和教程等完整说明文档。

## 主要文件

| 文档 | 说明 |
|------|------|
| `index.md` | 文档导航首页，概述平台功能与快速链接 |
| `architecture.md` | 系统架构说明：模块划分、技术选型、数据流设计 |
| `development.md` | 开发指南：本地环境搭建、编码规范、贡献流程 |
| `api.md` | 算法核心 REST API 接口参考文档 |
| `tutorials.md` | 使用教程：从入门到高级的循序渐进指南 |
| `uav_integration.md` | 无人机路径规划系统的集成方法与接口说明 |

## 快速导航

| 需求 | 推荐文档 |
|------|---------|
| 了解系统架构 | [architecture.md](architecture.md) |
| 搭建开发环境 | [development.md](development.md) |
| 查看 API 接口 | [api.md](api.md) |
| 学习使用教程 | [tutorials.md](tutorials.md) |
| 集成无人机系统 | [uav_integration.md](uav_integration.md) |

## 平台核心能力

- **多算法同化**：3D-VAR、4D-VAR、EnKF 及混合同化
- **多源数据融合**：卫星、雷达、地面站观测融合
- **并行计算**：Dask、MPI、Ray 多种后端
- **硬件加速**：CPU、GPU (CUDA)、JAX 加速
- **质量管控**：数据质量控制与风险评估
- **可视化**：同化结果可视化与仪表盘

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
