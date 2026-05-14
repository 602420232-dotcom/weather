# assets/config

应用静态配置资源目录。

## 关键文件

| 文件 | 说明 |
|------|------|
| `default_config.json` | 应用默认配置 JSON，包含： |

### 配置结构

```json
{
  "api_base_url": "服务器地址",
  "edge_server_url": "边缘计算地址",
  "default_location": { "latitude": 39.9042, "longitude": 116.4074, "zoom": 13 },
  "planning": { "default_algorithm": "vrptw", "max_waypoints": 50, "grid_size": 100 },
  "weather": { "refresh_interval_seconds": 300, "wind_speed_threshold": 10.0, "visibility_threshold": 3.0 },
  "offline": { "cache_max_age_hours": 24, "map_cache_dir": "map_tiles" },
  "features": { "dark_mode": false, "biometric_auth": false, "push_notifications": true, "offline_maps": true }
}
```

默认位置锚定为北京市中心（天安门广场附近）。

---
> **最后更新**: 2026-05-14  
> **版本**: 1.0  
> **维护者**: DITHIOTHREITOL
