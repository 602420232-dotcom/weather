# core/utils

通用工具类，提供日志输出和防抖等基础能力。

## 关键文件

| 文件 | 说明 |
|------|------|
| `logger.dart` | 日志工具 `LogUtil`，基于 `logger` 包封装，提供 `d`(debug) / `i`(info) / `w`(warning) / `e`(error) 四级静态日志方法 |
| `debouncer.dart` | 防抖器 `Debouncer`，默认延迟 300ms，用于防止按钮重复点击、搜索输入防抖等场景 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
