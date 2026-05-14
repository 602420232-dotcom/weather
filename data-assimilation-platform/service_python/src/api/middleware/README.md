# middleware

FastAPI 中间件模块，提供 HTTP 请求的横切关注点处理（CORS 跨域、错误处理、请求日志等）。

## 主要文件

| 文件 | 说明 |
|------|------|
| `__init__.py` | 模块导出：`setup_cors`、`setup_error_handlers`、`setup_logging` |
| `cors.py` | CORS 跨域中间件配置 |
| `error_handler.py` | 全局异常处理器，统一 HTTP 错误响应格式 |
| `logging.py` | 请求/响应日志中间件，记录请求耗时和状态 |
| `test_cors.py` | CORS 中间件测试 |
| `test_error_handler.py` | 错误处理测试 |
| `test_logging.py` | 日志中间件测试 |

## 中间件链

```
请求 → GZipMiddleware → CORSMiddleware → 错误处理器 → 日志中间件 → 路由处理
```

## 配置说明

### CORS

默认允许的来源：
```
http://localhost:8088
http://localhost:8080
http://localhost:5173
```

### 错误处理

统一错误响应格式：

```json
{
  "error": true,
  "code": 500,
  "message": "描述信息"
}
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
