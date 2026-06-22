# config

应用全局配置，定义应用名称、版本号、API 端点、主题色等常量，以及 Material 3 亮色/暗色主题。

## 关键文件

| 文件 | 说明 |
|------|------|
| `app_config.dart` | 全局配置类 `AppConfig`： |

### 配置项

| 配置 | 说明 |
|------|------|
| `appName` | 应用名称：`无人机路径规划系统` |
| `appVersion` | 版本号：`1.0.0` |
| `apiBaseUrl` | API 基础地址：`http://localhost:8088` |
| `apiEndpoints` | API 端点映射（platform / wrf / forecast / planning / assimilation / weather / edge） |
| `primaryColor` / `successColor` / `warningColor` / `errorColor` / `infoColor` | 主题色彩常量 |
| `lightTheme()` | Material 3 亮色主题（含 AppBar / Card / Input / Button / NavigationBar 样式） |
| `darkTheme()` | Material 3 暗色主题（深色背景卡片风格） |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
