# integration

集成测试目录，验证贝叶斯数据同化系统中多个模块之间的协同工作能力，包括完整同化工作流和端到端流程。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 包初始化 |
| `test_assimilation_workflow.py` | 完整同化工作流集成测试（数据加载 → 质量控制 → 同化 → 输出） |
| `test_workflows.py` | 多工作流集成测试（批处理、流水线、流式处理） |

## 测试覆盖

- **同化工作流**：从原始数据到最终分析结果的完整流水线
- **多算法串联**：3D-VAR、4D-VAR、EnKF 在真实场景下的协作
- **工作流引擎**：BatchAssimilator、Pipeline、StreamingAssimilator 的端到端验证

## 运行方式

```bash
cd algorithm_core

# 运行集成测试
pytest tests/integration/ -v

# 仅集成测试（自动标记）
pytest -v -m integration
```

## 注意事项

- 集成测试通常比单元测试耗时更长，建议在 CI/CD 中单独运行
- 依赖 `conftest.py` 中的共享 fixture
- 测试会自动获得 `@pytest.mark.integration` 标记

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
