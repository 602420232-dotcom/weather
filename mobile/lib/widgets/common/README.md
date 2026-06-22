# widgets/common

通用 UI 组件集合，在各业务页面中共享复用。

## 关键文件

| 文件 | 说明 |
|------|------|
| `app_widgets.dart` | 通用组件集，包含以下 Widget： |

### 组件清单

| 组件 | 类型 | 说明 |
|------|------|------|
| `StatusCard` | `ConsumerWidget` | 状态卡片，展示标题/数值/后缀/图标/颜色，支持点击跳转 |
| `LoadingPage` | `StatelessWidget` | 加载中页面，居中显示圆形进度条和提示文字 |
| `ErrorPage` | `StatelessWidget` | 错误页面，显示错误图标和消息，可选重试按钮 |
| `EmptyPage` | `StatelessWidget` | 空状态页面，显示大图标和提示消息 |
| `SectionTitle` | `StatelessWidget` | 分区标题，左侧标题文字 + 可选右侧附加 Widget |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
