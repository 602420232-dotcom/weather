# schemas

跨服务的 JSON Schema 定义目录，为同化请求和响应提供统一的数据格式校验标准。

## 主要文件

| 文件 | 说明 |
|------|------|
| `assimilation_request.schema.json` | 同化请求的 JSON Schema（JSON Schema Draft-07），定义 algorithm、background、observations、config 字段 |
| `assimilation_response.schema.json` | 同化响应的 JSON Schema，定义分析场、方差场等返回结构 |

## 请求 Schema 结构

```json
{
  "algorithm": "3dvar | 4dvar | enkf | hybrid",
  "background": {
    "grid": {"lat": [], "lon": [], "lev": []},
    "variables": {"<var_name>": []}
  },
  "observations": [{
    "lat": 0, "lon": 0, "lev": 0,
    "variable": "wind_speed",
    "value": 0, "error": 0
  }],
  "config": {
    "max_iterations": 10,
    "tolerance": 1e-6,
    "parallel": false
  }
}
```

## 用途

- 前后端接口契约：确保 API 请求/响应格式一致
- 数据验证：在服务端自动校验请求结构
- 跨语言兼容：Java（Spring 服务）、Python（算法核心）共享同一份规范
- API 文档生成：Schema 可自动生成交互式 API 文档

## 工具集成

```python
import jsonschema

# 验证请求
jsonschema.validate(instance=request_data, schema=assimilation_request_schema)
```

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
