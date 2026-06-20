# test

自动化测试目录，使用 Flutter 官方测试框架 `flutter_test` 对数据模型和核心服务进行单元测试。

## 关键文件

| 文件 | 说明 |
|------|------|
| `models_test.dart` | 模型和服务测试套件，覆盖 3 个测试组 |

## 测试覆盖

| 测试组 | 测试用例 |
|--------|---------|
| 数据模型测试 | DroneModel / TaskModel / WeatherDataModel / UserModel / DataSourceModel / SystemStatus 的 JSON 序列化和属性计算 |
| 边缘端风险评估测试 | 正常天气、危险天气两种情况下的 `EdgeCoordinatorService.assessWeatherRisk()` 本地评估 |
| 模型 copyWith 测试 | DroneModel / TaskModel 的不可变更新方法 |

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
