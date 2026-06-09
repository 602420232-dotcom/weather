# UAV Path Planning System - 无人机路径规划系统

## 📋 项目概述

基于 WRF 气象驱动的无人机 VRP（车辆路径问题）智能路径规划系统。

**核心特性**:
- 🌦️ **气象驱动**: 集成 WRF 气象模型，实时获取风场、温度、降水等数据
- 🛤️ **智能规划**: 融合 VRPTW、DE-RRT*、DWA 等算法
- ☁️ **数据同化**: 贝叶斯数据同化技术，融合多源气象观测
- 📊 **实时监控**: 可视化监控无人机状态和任务进度
- 🔄 **联邦学习**: Edge-Cloud 协同，支持端侧离线路径规划
- 🌍 **多语言支持**: 支持中文、英文、日语三种语言界面
- 📍 **地理位置服务**: 获取用户位置，显示当地时间和天气信息
- 🌤️ **天气API集成**: 支持 OpenWeatherMap 和和风天气接入

**技术栈**:
- **前端**: Vue 3 + Vite + Element Plus
- **后端**: Spring Boot 3.2 + Java 17
- **数据库**: MySQL 8.0 + Redis
- **算法**: Python 3.8+ (NumPy, SciPy, TensorFlow)
- **容器**: Docker + Kubernetes

---

## ✅ 已完成功能

### 国际化支持
- ✅ 中文 (zh-CN)
- ✅ 英文 (en-US)
- ✅ 日语 (ja-JP)

### 地理位置与天气
- ✅ 浏览器 Geolocation API 获取用户位置
- ✅ 基于位置的实时时间显示
- ✅ 模拟天气数据（演示模式）
- ✅ OpenWeatherMap API 集成
- ✅ 和风天气（QWeather）API 集成
- ✅ API配置页面支持切换天气数据源

### 界面优化
- ✅ 顶部导航栏显示位置、时间、天气信息
- ✅ 修复左侧栏重复标签问题
- ✅ 修复首页Nacos和中国民航局链接404问题
- ✅ 算法参数调优页面优化（下拉列表选择）

### 团队论坛
- ✅ 四大板块：公告通知、技术讨论、任务协作、知识库
- ✅ 发帖、评论、点赞、收藏功能
- ✅ @成员提醒和站内通知
- ✅ 基于角色的发帖权限控制
- ✅ WebSocket 实时通知推送

### 用户统计（仅管理员）
- ✅ 用户活动日志记录
- ✅ 活跃度、发帖、评论、登录统计
- ✅ 地域分布统计
- ✅ 操作类型统计
- ✅ CSV 报表导出功能
- ✅ 地理位置显示（仅管理员可见）

### 气象数据源管理
- ✅ 浮标气象站（buoy-weather-service）
- ✅ 探测无人机气象（detection-drone-service）
- ✅ 地面气象站（ground-station-weather-service）
- ✅ 无线电探空仪（radiosonde-weather-service）
- ✅ 卫星气象数据（satellite-weather-service）
- ✅ 数据源状态监控（在线/离线）
- ✅ 数据质量指标（完整率、延迟、错误率）
- ✅ 数据源配置管理

---

## 📁 项目结构

```
uav-path-planning-system/
├── frontend-vue/          # Vue 3 前端应用
│   ├── src/
│   │   ├── components/    # 通用组件
│   │   ├── views/         # 页面视图
│   │   ├── stores/        # Pinia 状态管理
│   │   ├── locales/       # 国际化语言文件
│   │   └── utils/         # 工具函数
│   └── public/            # 静态资源
├── backend-spring/        # Spring Boot 后端
├── algorithm-core/        # Python 算法核心
├── database/              # 数据库初始化脚本
└── docs/                  # 项目文档
```

---

## 🚀 快速开始

### 前置依赖
- Node.js >= 18.0
- JDK >= 17
- Python >= 3.8
- MySQL >= 8.0
- Redis >= 7.0

### 启动前端开发服务器

```bash
cd frontend-vue
npm install
npm run dev
```

### 启动后端服务

```bash
cd backend-spring
mvn spring-boot:run
```

---

## 🔧 配置说明

### 天气API配置

在系统设置的 **API配置** 页面中可以配置天气数据源：

1. **演示模式**: 使用模拟天气数据（默认）
2. **OpenWeatherMap**: 需申请 API Key
   - 地址: https://openweathermap.org/api
   - 免费版限制: 每天1000次调用
3. **和风天气**: 需申请 API Key
   - 地址: https://dev.qweather.com/
   - 免费版限制: 每天1000次调用

---

## 📝 更新日志

### v2.1.1 (2026-06-09)
- 新增日语语言支持
- 添加顶部导航栏位置、时间、天气显示
- 修复首页404链接（Nacos、中国民航局）
- 修复左侧栏重复标签问题
- 集成 OpenWeatherMap 和和风天气 API
- 添加天气API配置页面

### v2.1 (2026-05-08)
- 完成核心路径规划算法集成
- 添加气象数据可视化
- 实现任务管理系统
- 支持多角色权限控制

---

> **最后更新**: 2026-06-09  
> **版本**: 2.1.1  
> **维护者**: DITHIOTHREITOL