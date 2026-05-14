# accelerators

硬件加速器模块，提供统一的加速器抽象和多后端实现，支持 CPU（多线程/OpenMP）、GPU（CUDA/CuPy）、JAX/TPU 等硬件加速方案。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出 |
| `base.py` | `BaseAccelerator` 抽象基类、`AcceleratorType` 枚举（CPU/OpenMP/CuPy/PyCUDA/JAX/TPU 等） |
| `cpu.py` | CPU 加速器实现（多线程、BLAS 优化） |
| `cuda.py` | CUDA 加速器实现（基于 PyCUDA） |
| `gpu.py` | GPU 加速器实现（基于 CuPy） |
| `jax.py` | JAX 加速器实现（支持 TPU） |
| `test_base.py` | 基类测试 |
| `test_cpu.py` | CPU 加速测试 |
| `test_cuda.py` | CUDA 加速测试 |
| `test_gpu.py` | GPU 加速测试 |
| `test_jax.py` | JAX 加速测试 |

## 加速器类型

| 类型 | 枚举值 | 实现方式 | 适用硬件 |
|------|--------|---------|---------|
| CPU | `AcceleratorType.CPU` | NumPy + threading | 通用 CPU |
| OpenMP | `AcceleratorType.OPENMP` | OpenMP 并行 | 多核 CPU |
| CuPy | `AcceleratorType.CUPY` | CuPy (CUDA 兼容 NumPy) | NVIDIA GPU |
| PyCUDA | `AcceleratorType.PYCUDA` | PyCUDA 内核 | NVIDIA GPU |
| JAX | `AcceleratorType.JAX` | JAX JIT 编译 | CPU/GPU/TPU |

## 使用示例

```python
from bayesian_assimilation.accelerators import (
    AcceleratorType, CpuAccelerator, CudaAccelerator, GpuAccelerator, JaxAccelerator
)

# CPU 加速（多线程）
cpu = CpuAccelerator(config={"num_threads": 8})
cpu.initialize()
result = cpu.compute(data)

# GPU 加速（CuPy）
gpu = GpuAccelerator(config={"device": 0})
gpu.initialize()
result = gpu.compute(data)

# JAX 加速
jax_acc = JaxAccelerator()
jax_acc.initialize()
result = jax_acc.compute(data)

# 统一接口
for acc_type in AcceleratorType:
    acc = accelerator_factory.create(acc_type)
    if acc.initialize():
        result = acc.compute(data)
```

## 配置

```yaml
gpu:
  enabled: true
  device: 0
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
