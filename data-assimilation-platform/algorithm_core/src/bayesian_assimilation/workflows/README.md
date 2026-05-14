# workflows

工作流管理模块，提供批处理、流水线和流式处理三种工作流模式，支持大规模数据和连续观测场景下的同化任务编排。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：全部工作流类与函数 |
| `batch.py` | 批处理工作流：`BatchAssimilator`（多任务批量同化）、`batch_assimilate` |
| `pipeline.py` | 流水线工作流：`AssimilationPipeline`（多阶段串行处理）、`DataLoadingStep`、`PreprocessingStep`、`AssimilationStep`、`PostprocessingStep` |
| `streaming.py` | 流式处理工作流：`StreamingAssimilator`（实时流同化）、`ContinuousAssimilator`（持续同化） |
| `test_batch.py` | 批处理测试 |
| `test_pipeline.py` | 流水线测试 |
| `test_streaming.py` | 流式处理测试 |

## 工作流模式对比

| 模式 | 适用场景 | 特点 |
|------|---------|------|
| **批次 (Batch)** | 历史数据回顾同化 | 批量处理所有数据，一次运行 |
| **流水线 (Pipeline)** | 标准化多阶段处理 | 数据加载 → 预处理 → 同化 → 后处理 |
| **流式 (Streaming)** | 实时数据持续同化 | 逐条或小批次处理，支持无限流 |

## 使用示例

### 批处理

```python
from bayesian_assimilation.workflows import BatchAssimilator

batch = BatchAssimilator(config={"method": "3dvar"})
results = batch.process(job_list)
```

### 流水线

```python
from bayesian_assimilation.workflows import (
    create_standard_pipeline
)

pipeline = create_standard_pipeline()
pipeline.add_stage(DataLoadingStep())
pipeline.add_stage(PreprocessingStep())
pipeline.add_stage(AssimilationStep(method="3dvar"))
pipeline.add_stage(PostprocessingStep())

results = pipeline.run(input_data)
```

### 流式处理

```python
from bayesian_assimilation.workflows import (
    StreamingAssimilator, process_data_stream
)

stream = StreamingAssimilator(window_size=100)
for data_chunk in data_generator:
    result = stream.process(data_chunk)

# 或使用便捷函数
process_data_stream(data_generator, method="3dvar")
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
