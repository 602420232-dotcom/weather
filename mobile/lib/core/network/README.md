# core/network

网络通信模块，基于 Dio 封装的 HTTP 客户端，提供统一的请求/响应处理、认证拦截、异常转换。

## 关键文件

| 文件 | 说明 |
|------|------|
| `api_client.dart` | HTTP 客户端单例 `ApiClient`，封装 GET/POST/PUT/DELETE/文件上传，超时配置 15s/30s，自动 JSON 解析、状态码校验和错误转换 |
| `api_interceptor.dart` | Dio 拦截器 `ApiInterceptor`，自动附加 Bearer Token、生成 X-Request-ID、标记客户端类型和版本；401 时清除 Token |
| `api_exception.dart` | 统一异常类 `ApiException`，包含 code 和 message 字段 |

### 错误处理

| HTTP 状态码 | 错误信息 |
|-------------|----------|
| 401 | 认证已过期，请重新登录 |
| 403 | 没有访问权限 |
| 404 | 请求的资源不存在 |
| 5xx | 服务器内部错误 |
| 超时 | 网络连接超时 |
| 连接失败 | 无法连接到服务器 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
