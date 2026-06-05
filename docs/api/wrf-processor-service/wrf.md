# WRF处理服务API

WRF处理服务提供气象数据的解析和处理功能，支持从WRF输出文件中提取低空气象参数。

## 接口列表

### 1. 参数化解析WRF数据

**接口地址**：`POST /api/wrf/parse-params`

**功能**：根据参数化请求解析WRF数据

**请求体**：`application/json`
```json
{
  "filePath": "/path/to/wrf/output.nc",
  "height": 100,
  "bounds": {
    "minLat": 39.0,
    "maxLat": 40.0,
    "minLon": 116.0,
    "maxLon": 117.0
  },
  "data": {}
}
```

**响应**：
```json
{
  "success": true,
  "code": 200,
  "message": "WRF数据解析成功",
  "data": { "temperature": {...}, "wind_speed": {...} }
}
```

### 2. 解析WRF文件

**接口地址**：`POST /api/wrf/parse`

**功能**：解析WRF输出的NetCDF4文件，提取气象数据

**请求参数**：`multipart/form-data`
- `file`：WRF输出的NetCDF4文件（.nc或.netcdf格式）
- `height`：高度值（1-30000米，默认100）

**响应**：
```json
{
  "success": true,
  "data": "{...}",
  "fileId": "wrf_xxx"
}
```

### 3. 高级解析

**接口地址**：`POST /api/wrf/parse/advanced`

**功能**：高级WRF数据解析，支持指定参数类型

**请求体**：`application/json`
```json
{
  "filePath": "/path/to/wrf/output.nc",
  "height": 100,
  "paramType": "all"
}
```

**响应**：
```json
{
  "success": true,
  "data": "{...}"
}
```

### 4. 获取湍流数据

**接口地址**：`GET /api/wrf/turbulence`

**功能**：获取湍流数据

**请求参数**：Query
- `fileId`：文件ID

**响应**：
```json
{
  "success": true,
  "data": { "turbulence": {...} }
}
```

### 5. 获取能见度数据

**接口地址**：`GET /api/wrf/visibility`

**功能**：获取能见度数据

**请求参数**：Query
- `fileId`：文件ID

**响应**：
```json
{
  "success": true,
  "data": { "visibility": {...} }
}
```

### 6. 获取闪电风险评估

**接口地址**：`GET /api/wrf/lightning-risk`

**功能**：获取闪电风险评估

**请求参数**：Query
- `fileId`：文件ID

**响应**：
```json
{
  "success": true,
  "data": { "lightningRisk": {...} }
}
```

### 7. 获取高度分层数据

**接口地址**：`GET /api/wrf/height-layers`

**功能**：获取多个高度层的气象数据

**请求参数**：Query
- `fileId`：文件ID
- `layers`：自定义高度层（逗号分隔，如 "0,100,500,1000"）

**响应**：
```json
{
  "success": true,
  "data": { "layers": [...] }
}
```

### 8. 获取处理后的气象数据

**接口地址**：`GET /api/wrf/data`

**功能**：获取解析后的气象数据

**请求参数**：Query
- `fileId`：文件ID

**响应**：
```json
{
  "success": true,
  "data": { "variable": "temperature", "grid": {...} }
}
```

### 9. 获取数据统计信息

**接口地址**：`GET /api/wrf/stats`

**功能**：获取气象数据的统计信息

**请求参数**：Query
- `fileId`：文件ID

**响应**：
```json
{
  "success": true,
  "data": { "statistics": { "min": 15.2, "max": 25.8, "mean": 20.5 } }
}
```

### 10. 上传WRF数据

**接口地址**：`POST /api/wrf/upload`

**功能**：上传WRF数据记录

**请求体**：`application/json`
```json
{
  "fileName": "wrf_file.nc",
  "filePath": "./data/wrf_file.nc",
  "fileSize": 1048576
}
```

**响应**：
```json
{
  "success": true,
  "message": "WRF数据上传成功",
  "data": { "fileId": "wrf_xxx", "id": 1 }
}
```

### 11. 列出WRF数据

**接口地址**：`GET /api/wrf/list`

**功能**：分页获取WRF数据列表

**请求参数**：Query
- `page`：页码（从0开始）
- `size`：每页数量

**响应**：
```json
{
  "success": true,
  "data": [...],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10
}
```

### 12. 获取WRF数据详情

**接口地址**：`GET /api/wrf/detail`

**功能**：获取单个WRF数据文件详情

**请求参数**：Query
- `id`：数据ID

**响应**：
```json
{
  "success": true,
  "data": { "id": 1, "fileId": "wrf_xxx", "fileName": "...", "status": "..." }
}
```

---

> **最后更新**: 2026-06-05  
> **版本**: 2.2  
> **维护者**: DITHIOTHREITOL
