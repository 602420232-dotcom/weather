# requirements

Python 微服务的依赖管理目录，按环境分层管理 pip 依赖。

## 主要文件

| 文件 | 说明 |
|------|------|
| `base.txt` | 基础运行依赖：FastAPI、Uvicorn、Pydantic、HTTPx、python-multipart |
| `dev.txt` | 开发环境额外依赖：测试框架、代码检查、调试工具 |
| `prod.txt` | 生产环境额外依赖：性能监控、生产级中间件、日志聚合 |

## 依赖说明

### base.txt（核心运行时）

```
fastapi>=0.100.0       # Web 框架
uvicorn>=0.23.0        # ASGI 服务器
pydantic>=2.0.0        # 数据验证
httpx>=0.24.0          # HTTP 客户端
python-multipart>=0.0.5 # 文件上传支持
```

### 安装方式

```bash
# 基础安装
pip install -r requirements/base.txt

# 开发安装
pip install -r requirements/base.txt -r requirements/dev.txt

# 生产安装
pip install -r requirements/base.txt -r requirements/prod.txt
```

### 与主项目的依赖关系

- `service_python` 依赖 `algorithm_core` 包，需先安装：
  ```bash
  cd algorithm_core && pip install -e .
  cd ../service_python && pip install -r requirements/base.txt
  ```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
