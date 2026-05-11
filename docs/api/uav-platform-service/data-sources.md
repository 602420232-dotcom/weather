# 数据源管理接?

数据源管理提供地面站浮标气象卫星等多源数据的CRUD操作?

## 接口列表

### 1. 获取数据源列?

**接口地址**`GET /api/v1/data-sources`

**功能**获取所有数据源

**响应**?
```json
{
  "code": 200,
  "message": "获取数据源列表成?,
  "data": [
    {"id": 1, "name": "地面站数据源", "type": "ground_station", "status": "active"},
    {"id": 2, "name": "浮标数据?, "type": "buoy", "status": "active"}
  ]
}
```

### 2. 获取数据源详?

**接口地址**`GET /api/v1/data-sources/{id}`

### 3. 创建数据?

**接口地址**`POST /api/v1/data-sources`

### 4. 更新数据?

**接口地址**`PUT /api/v1/data-sources/{id}`

### 5. 删除数据?

**接口地址**`DELETE /api/v1/data-sources/{id}`

### 6. 测试数据?

**接口地址**`POST /api/v1/data-sources/test`

### 7. 获取数据源类型列?

**接口地址**`GET /api/v1/data-sources/types`

### 8. 获取地面站数?

**接口地址**`GET /api/v1/real-data/ground-station`

**功能**获取实时地面站观测数据

### 9. 获取浮标数据

**接口地址**`GET /api/v1/real-data/buoy`

**功能**获取实时海洋浮标数?

### 10. 获取数据源状?

**接口地址**`GET /api/v1/real-data/status`

**功能**获取各数据源的连接状态和最新更新时?
---

> **最后更新*: 2026-05-09  
> **版本**: 2.1  
> **维护者*: DITHIOTHREITOL

