# assets/fonts

自定义字体目录，用于存放应用使用的自定义字体文件（如 `.ttf`、`.otf`）。

> 当前目录仅包含 `.gitkeep` 占位文件，尚未添加自定义字体。

如需使用，将字体文件放入此目录后，在 `pubspec.yaml` 中注册：

```yaml
flutter:
  fonts:
    - family: CustomFont
      fonts:
        - asset: assets/fonts/CustomFont-Regular.ttf
        - asset: assets/fonts/CustomFont-Bold.ttf
          weight: 700
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
