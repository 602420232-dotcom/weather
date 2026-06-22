# core/storage

本地持久化存储模块，区分敏感数据与非敏感数据的存储方案。

## 关键文件

| 文件 | 说明 |
|------|------|
| `secure_storage.dart` | 安全存储单例 `SecureStorage`，基于 `flutter_secure_storage`，存储 Token / RefreshToken / 用户名 / 服务器地址等敏感信息 |
| `local_storage.dart` | 本地存储单例 `LocalStorage`，基于 `shared_preferences`，存储主题模式、语言、离线模式、地图类型、自动同步等非敏感配置项 |

### 存储策略

| 模块 | 技术 | 适用场景 |
|------|------|---------|
| `SecureStorage` | `flutter_secure_storage`（Keychain/Keystore 加密） | 认证凭证、服务器地址 |
| `LocalStorage` | `shared_preferences` | 用户偏好设置、离线状态、地图类型 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
