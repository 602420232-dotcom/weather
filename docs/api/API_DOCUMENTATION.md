# UAV Path Planning System ?API 文档

## 一API 网关路由| 路由前缀 | 目标服务 | 端口 |
|----------|----------|:----:|
| `/api/v1/auth/**` | backend-spring (认证) | 8083 |
| `/api/v1/data-sources/**` | uav-platform-service | 8080 |
| `/api/v1/real-data/**` | uav-platform-service | 8080 |
| `/api/platform/**` | uav-platform-service | 8080 |
| `/api/wrf/**` | wrf-processor-service | 8081 |
| `/api/assimilation/**` | data-assimilation-service | 8084 |
| `/api/forecast/**` | meteor-forecast-service | 8082 |
| `/api/planning/**` | path-planning-service | 8083 |
| `/api/weather/**` | uav-weather-collector | 8086 |

## 二认证服务(backend-spring :8083)

### POST /api/v1/auth/register
注册新用```json
{"username":"user","password":"pass","email":"user@test.com","fullName":"Test User"}
```

### POST /api/v1/auth/login
登录获取JWT Token
```json
{"username":"user","password":"pass"}
```

## 三数据源管理 (uav-platform-service :8080)

### GET /api/v1/data-sources
获取数据源列表`200 {"code":200,"data":[...]}`

### GET /api/v1/data-sources/{id}
按ID获取数据源详情`200 / 404 DataNotFoundException`

### POST /api/v1/data-sources
创建数据源`200 {"code":200,"data":{...}}`

### PUT /api/v1/data-sources/{id}
更新数据源`200 / 404`

### DELETE /api/v1/data-sources/{id}
删除数据源`200 / 404`

### POST /api/v1/data-sources/test
测试数据源连接`200 {"code":200,"data":{"success":true}}`

### GET /api/v1/data-sources/types
获取数据源类型列表`200 {"code":200,"data":[...]}`

## 四实时数据 (uav-platform-service :8080)

### GET /api/v1/real-data/ground-station
获取地面站实时数据`200 {"code":200,"data":[...]}`

### GET /api/v1/real-data/buoy
获取浮标实时数据 `200 {"code":200,"data":[...]}`

### GET /api/v1/real-data/status
获取数据源状态`200 {"code":200,"data":{...}}`

## 五平台编排(uav-platform-service :8080)

### POST /api/platform/plan
提交完整规划流程WOF解析贝叶斯同化气象预测路径规划```json
{"weatherData":{},"drones":[],"tasks":[]}
```

### GET /api/platform/weather?fileId={id}
获取气象数据 `200 {"success":true,"data":{...}}`

### POST /api/platform/task
任务管理 `200 {"success":true,"message":"任务管理成功"}`

### GET /api/platform/drones
获取无人机列表`200 {"success":true,"data":[]}`

## 六WRF气象处理 (wrf-processor-service :8081)

### POST /api/wrf/upload
上传NetCDF气象文件
- 支持格式: `.nc`, `.netcdf`
- 验证: 文件名不得包含路径遍历字- 内容类型: multipart/form-data

### GET /api/wrf/weather/{fileId}
获取气象数据 `200 {"success":true,"data":{...}}`

### GET /api/wrf/statistics/{fileId}
获取统计信息 `200 {"success":true,"data":{...}}`

## 七数据同(data-assimilation-service :8084)

### POST /api/assimilation/execute
执行3D-Var/4D-Var/EnKF同化
```json
{"algorithm":"3dvar","background":{},"observations":{},"config":{}}
```

### POST /api/assimilation/variance
计算方差### POST /api/assimilation/batch
批量同化处理

## 八气象预(meteor-forecast-service :8082)

### POST /api/forecast/predict
气象预测 (LSTM/XGBoost/Hybrid)
```json
{"method":"lstm","data":{"latitude":39.9,"longitude":116.4},"config":{}}
```

### POST /api/forecast/correct
数据订正

### GET /api/forecast/models
获取可用模型列表

## 九路径规(path-planning-service :8083)

### POST /api/planning/vrptw
VRPTW路径规划
```json
{"algorithm":"vrptw","drones":{},"tasks":{},"weatherData":{}}
```

### POST /api/planning/astar
A*全局路径规划

### POST /api/planning/dwa
DWA局部路径规### POST /api/planning/full
三层完整路径规划全局+局动态避障

## 十气象采(uav-weather-collector :8086)

### POST /api/weather/collect
采集无人机传感器数据

### GET /api/weather/drone/{droneId}
获取无人机气象数### GET /api/weather/history/{droneId}/{minutes}
获取气象历史

### GET /api/weather/fused/{droneId}
获取融合气象数据

### GET /api/weather/sources
获取可用数据源列表

### GET /api/weather/alerts/{droneId}
获取无人机告## 十一错误码

| 状态码 | 含义 | 处理方式 |
|:------:|------|----------|
| 200 | 成功 | 正常处理 |
| 400 | 参数校验失败 | 检查请求体 |
| 401 | 未认| 添加JWT Token |
| 403 | 权限不足 | 检查角色权|
| 404 | 资源不存| 检查ID/路径 |
| 500 | 服务器内部错| 联系运维 |

## 十二通用响应格式

```json
// 成功
{"code": 200, "message": "操作成功", "data": {...}}
// 失败
{"code": 400, "message": "参数错误", "error": "详细错误信息"}
```
---

> **最后更新*: 2026-05-09  
> **版本**: 2.1  
> **维护者*: DITHIOTHREITOL

