# 基于WRF气象驱动的无人机VRP智能路径规划系统

本项目面向城市低空物流、电力巡检、应急救援、农林植保、城市管理等场景，构建一套集高精度低空气象预报、多约束智能路径规划、全栈Web可视化管理于一体的无人机智能调度系统。系统实现气象风险与无人机任务深度耦合，支持多机协同、动态重规划、离线作业、多角色权限管理，满足低空监管与超视距飞行合规要求。

## 系统架构

### 微服务架构

系统采用微服务架构，将不同功能模块拆分为独立的服务，通过API调用进行集成。

### 核心服务

1. **WRF气象数据处理服务** (`wrf-processor-service`)
   - 解析WRF输出的NetCDF4文件
   - 提取低空气象参数（0–1000米三维风场、温度、湿度、湍流、能见度、雷电风险）
   - 提供气象数据统计信息

2. **贝叶斯同化服务** (`data-assimilation-service`)
   - 实现3D-VAR、EnKF和混合方法的贝叶斯同化
   - 生成不确定性方差场与风险热力图
   - 支持多源数据融合（雷达、无人机探空、地面气象站、北斗/GNSS）

3. **气象预测与订正服务** (`meteor-forecast-service`)
   - WRF微尺度气象模拟 + 风乌GHR高分辨率短时预报双预报引擎
   - CLSTM+XGBoost AI智能订正，降低预报误差
   - 5分钟级高频更新，10–15分钟提前预警危险天气
   - 支持模型训练与评估

4. **路径规划服务** (`path-planning-service`)
   - VRPTW多约束规划：多无人机、多任务点、时间窗、载重、续航、气象风险、禁飞区
   - 三层规划架构：
     - 顶层：VRPTW任务分配、航线排序、集群调度
     - 中层：DE-RRT*全局三维最优路径，避让高风险气象区与静态障碍
     - 底层：DWA实时避障、抗风扰动、防局部最优，风场补偿，风险权重自适应
   - 气象突变/空域变更/任务调整时5秒内动态重规划
   - 多目标权重：熵权法自动分配
   - 代价函数：四维代价，距离+能耗+时间+气象风险

5. **主平台服务** (`uav-platform-service`)
   - 集成所有子服务
   - 提供统一的API接口
   - 实现多角色权限管理（管理员、调度员、无人机操作员、普通用户）
   - 管理任务和无人机信息

6. **端侧SDK** (`uav-edge-sdk`)
   - 无网络环境离线路径规划与气象风险判断
   - 对接PX4/ArduPilot等主流飞控
   - 本地DWA避障、航迹修正、飞行指令下发

### 技术栈

- **气象**：WRF + 风乌GHR、贝叶斯同化+高斯过程回归GPR、NetCDF4 → JSON数据格式
- **AI**：CLSTM、XGBoost
- **算法**：VRPTW、DE-RRT*、DWA、熵权法+四维动态代价函数
- **后端**：SpringBoot、MyBatis-Plus、Spring Security、gRPC
- **前端**：Vue3、Leaflet、ECharts、实时面板：气象、无人机、任务、告警
- **存储**：MySQL、Redis
- **部署**：Maven、Tomcat、Docker+微服务、Kubernetes
- **监控**：Prometheus + Grafana

## 部署方法

### 前提条件

- **Docker 和 Docker Compose**：用于容器化部署
- **Java 11+**：后端服务运行环境
- **Python 3.8+**：算法和气象处理运行环境
- **MySQL 8.0+**：业务数据存储
- **Redis 6.2+**：缓存服务
- **Kubernetes** (可选，用于生产环境)：容器编排和管理
- **Node.js 16+**：前端开发和构建环境

### 部署步骤

#### 使用Docker Compose部署（开发环境）

1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd uav-path-planning-system
   ```

2. **配置环境变量**
   - 复制 `.env.example` 文件为 `.env`
   - 根据实际情况修改环境变量，如数据库密码、服务端口等

3. **构建并启动服务**
   ```bash
   docker-compose up -d --build
   ```

4. **验证部署**
   - 前端应用：http://localhost:8080
   - 主平台服务：http://localhost:8080/api
   - WRF处理服务：http://localhost:8081/api/wrf
   - 贝叶斯同化服务：http://localhost:8084/api/assimilation
   - 气象预测服务：http://localhost:8082/api/forecast
   - 路径规划服务：http://localhost:8083/api/planning

5. **查看日志**
   ```bash
   docker-compose logs -f
   ```

#### 使用Kubernetes部署（生产环境）

1. **准备Kubernetes集群**
   - 确保Kubernetes集群已就绪
   - 安装Helm（可选，用于更方便的部署管理）
   - 安装Nginx Ingress Controller（用于流量分发）
   - 安装Prometheus和Grafana（用于监控）

2. **配置Kubernetes Secrets**
   - 编辑 `deployments/kubernetes/secrets.yml` 文件
   - 填充敏感信息，如数据库密码、JWT密钥等

3. **部署服务**
   ```bash
   cd deployments/kubernetes
   kubectl apply -f namespace.yml
   kubectl apply -f secrets.yml
   kubectl apply -f persistent-volumes.yml
   kubectl apply -f wrf-processor-service.yml
   kubectl apply -f meteor-forecast-service.yml
   kubectl apply -f path-planning-service.yml
   kubectl apply -f uav-platform-service.yml
   kubectl apply -f frontend-vue.yml
   kubectl apply -f monitoring.yml
   kubectl apply -f nginx-ingress.yml
   ```

4. **验证部署**
   - 前端应用：http://uav-platform.local
   - API接口：http://uav-platform.local/api
   - 监控面板：http://uav-platform.local/grafana

5. **配置域名和SSL**
   - 在DNS服务器中添加域名解析
   - 配置HTTPS证书

### 本地开发

1. **启动数据库和缓存**
   ```bash
   docker-compose up -d mysql redis
   ```

2. **安装依赖**
   - 后端服务：`mvn install`（在各服务目录下执行）
   - 前端服务：`npm install`（在frontend-vue目录下执行）
   - Python依赖：`pip install -r requirements.txt`（在相关服务目录下执行）

3. **启动各个服务**
   - WRF处理服务：`cd wrf-processor-service && mvn spring-boot:run`
   - 贝叶斯同化服务：`cd data-assimilation-service && mvn spring-boot:run`
   - 气象预测服务：`cd meteor-forecast-service && mvn spring-boot:run`
   - 路径规划服务：`cd path-planning-service && mvn spring-boot:run`
   - 主平台服务：`cd uav-platform-service && mvn spring-boot:run`
   - 前端开发：`cd uav-path-planning-system/frontend-vue && npm run dev`

## 运行与维护

### 日常运行

1. **服务状态监控**
   - 使用Prometheus和Grafana监控服务健康状态
   - 查看服务日志：`docker-compose logs <service-name>`
   - 检查服务状态：`kubectl get pods`（Kubernetes环境）

2. **数据管理**
   - 定期备份数据库：`docker exec -it <mysql-container> mysqldump -u root -p uav_path_planning > backup.sql`
   - 清理过期数据：设置定时任务清理过期的气象数据和路径规划结果
   - 数据验证：定期验证数据一致性和完整性

3. **性能优化**
   - 监控系统资源使用情况
   - 调整服务资源分配：`docker-compose up -d --scale path-planning-service=3`
   - 优化算法参数：根据实际运行情况调整算法参数

### 维护与故障处理

1. **常见问题与解决方案**

   | 问题 | 症状 | 解决方案 |
   |------|------|----------|
   | 服务启动失败 | 服务容器启动后立即退出 | 查看服务日志：`docker-compose logs <service-name>` |
   | API调用失败 | 服务间API调用返回错误 | 检查服务间网络连接和配置 |
   | 性能问题 | 系统响应缓慢 | 检查系统资源使用情况，优化算法参数 |
   | 数据一致性问题 | 不同服务间数据不一致 | 确保服务间使用相同的数据格式，实现数据同步机制 |

2. **服务恢复**
   - 重启服务：`docker-compose restart <service-name>`
   - 重建服务：`docker-compose up -d --build <service-name>`
   - 从备份恢复数据：`docker exec -i <mysql-container> mysql -u root -p uav_path_planning < backup.sql`

3. **安全维护**
   - 定期更新依赖：`mvn dependency:update`（后端）和 `npm update`（前端）
   - 检查安全漏洞：使用安全扫描工具定期检查依赖包漏洞
   - 更新SSL证书：确保HTTPS证书及时更新

4. **版本管理**
   - 服务版本：每个服务独立版本管理
   - 依赖版本：使用固定版本的依赖，确保系统稳定性
   - 配置版本：配置文件版本管理，便于回滚

### 扩展与升级

1. **水平扩展**
   - 增加服务实例数量：`kubectl scale deployment <deployment-name> --replicas=5`
   - 负载均衡：使用Nginx或Kubernetes内置负载均衡

2. **垂直扩展**
   - 增加硬件资源：提升CPU、内存等配置
   - 优化算法：改进核心算法，提高计算效率

3. **功能扩展**
   - 集成新算法：添加新的气象预测和路径规划算法
   - 支持新数据源：集成更多类型的气象数据源
   - 添加新功能：如飞行模拟、任务调度优化等

### 监控与告警

1. **监控指标**
   - 服务健康：各服务的健康状态和响应时间
   - 系统资源：CPU、内存、磁盘使用情况
   - 业务指标：同化计算时间、路径规划时间、预测准确率

2. **告警机制**
   - 服务异常：服务不可用或响应时间过长时告警
   - 资源不足：系统资源使用超过阈值时告警
   - 业务异常：同化失败或路径规划失败时告警

3. **日志管理**
   - 集中化日志：使用ELK Stack或类似工具集中管理日志
   - 日志分析：定期分析日志，识别潜在问题
   - 日志保留：设置合理的日志保留策略

## 部署检查清单

### 部署前
- [ ] 检查Docker和Docker Compose版本
- [ ] 检查系统资源（CPU、内存、磁盘）
- [ ] 配置环境变量和数据库连接
- [ ] 确保所有依赖已安装

### 部署中
- [ ] 构建并启动所有服务
- [ ] 检查服务启动状态
- [ ] 验证API接口可用性
- [ ] 测试完整路径规划流程

### 部署后
- [ ] 监控服务运行状态
- [ ] 检查系统资源使用情况
- [ ] 测试故障恢复能力
- [ ] 文档更新和维护

## 系统功能

### 1. 低空气象预报引擎
- WRF微尺度气象模拟和风乌GHR双预报引擎
- 多源数据同化（雷达、无人机探空、地面气象站、北斗/GNSS）
- CLSTM+XGBoost AI智能订正，降低预报误差
- 贝叶斯同化+高斯过程回归GPR输出不确定性方差场与风险热力图
- 5分钟级高频更新，10–15分钟提前预警危险天气
- NetCDF数据解析、风场矢量图、气象热力图可视化

### 2. 无人机智能路径规划
- VRPTW多约束规划：多无人机、多任务点、时间窗、载重、续航、气象风险、禁飞区
- 三层规划架构：
  - 顶层：VRPTW任务分配、航线排序、集群调度
  - 中层：DE-RRT*全局三维最优路径，避让高风险气象区与静态障碍
  - 底层：DWA实时避障、抗风扰动、防局部最优，风场补偿，风险权重自适应
- 气象突变/空域变更/任务调整时5秒内动态重规划
- 多目标权重：熵权法自动分配
- 代价函数：四维代价，距离+能耗+时间+气象风险
- 多机协同：任务分配、路径防冲突、队形保持
- 适航性评估：飞行前评估、飞行中预警、飞行后复盘

### 3. SpringBoot后端服务
- 多角色权限管理（管理员、调度员、无人机操作员、普通用户）
- 全量CRUD：用户、任务、点位、无人机、气象数据、路径方案、历史记录
- Python算法封装、Java调用、异步计算、任务队列、Redis缓存，gRPC调用Python算法
- MySQL业务存储、RESTful API、日志监控、异常告警
- 第三方系统对接、无人机飞控/低空UTM平台对接

### 4. Vue3 Web前端
- Leaflet地图可视化：任务点、航线、禁飞区、气象热力图、风险等级
- 任务配置：Excel批量导入、时间窗设置、无人机参数配置
- 路径展示：最优航线、航程/耗时/能耗统计、风险标注
- 方案管理：保存、编辑、删除、查询、导出、打印
- 实时数据面板：气象、无人机状态、任务进度、告警信息

### 5. 端侧SDK
- 无网络环境离线路径规划与气象风险判断
- 对接PX4/ArduPilot等主流飞控
- 本地DWA避障、航迹修正、飞行指令下发

## API接口

### 主平台接口
- `POST /api/platform/plan`：完整路径规划流程
- `GET /api/platform/weather`：获取综合气象数据
- `POST /api/platform/task`：任务管理
- `GET /api/platform/drones`：无人机管理
- `POST /api/platform/auth/login`：用户登录
- `GET /api/platform/users`：用户管理

### WRF处理服务接口
- `POST /api/wrf/parse`：解析WRF文件
- `GET /api/wrf/data`：获取处理后的气象数据
- `GET /api/wrf/stats`：获取数据统计信息

### 贝叶斯同化服务接口
- `POST /api/assimilation/execute`：执行贝叶斯同化
- `POST /api/assimilation/variance`：获取方差场
- `POST /api/assimilation/batch`：批量处理

### 气象预测服务接口
- `POST /api/forecast/predict`：执行气象预测
- `POST /api/forecast/correct`：执行气象数据订正
- `POST /api/forecast/fusion`：双预报引擎融合预测
- `POST /api/forecast/risk`：生成风险热力图
- `GET /api/forecast/models`：获取可用模型

### 路径规划服务接口
- `POST /api/planning/vrptw`：执行VRPTW任务调度
- `POST /api/planning/astar`：执行A*全局路径规划
- `POST /api/planning/derrt`：执行DE-RRT*路径规划
- `POST /api/planning/dwa`：执行DWA实时避障
- `POST /api/planning/full`：执行完整路径规划
- `POST /api/planning/replan`：执行动态重规划

## 安全配置

- 基于Spring Security的多角色权限管理
- 密码加密存储
- API接口鉴权
- 操作日志记录
- HTTPS配置
- API认证

## 性能优化

- Redis缓存高频气象数据和规划结果
- 异步处理耗时操作
- 数据库索引优化
- 算法参数调优
- 并行计算路径规划
- 缓存机制减少重复计算
- 水平自动缩放适应负载变化

## 性能指标

- 气象更新频率：5分钟/次
- 路径规划响应时间：≤5秒
- 系统支持并发用户：≥20
- 支持无人机数量：≥10架
- 单机支持任务点：≥200个
- 低空风速预报误差：≤0.5m/s
- 系统全年可用率：≥95%
- 气象风险预警准确率：提升60%以上

## 应用场景

- **城市低空物流配送**：多无人机末端配送，带时间窗和载重约束
- **电力/油气管道巡检**：山区长航线巡检，抗风抗湍流
- **应急救援与防灾减灾**：突发场景快速路径规划，离线作业支持
- **农林植保与测绘**：大面积全覆盖路径规划，多机协同
- **城市管理与公共服务**：定时定点任务，气象风险预警
- **低空智联网（UTM）接入**：为监管平台提供气象与路径规划能力

## 项目亮点

- WRF+AI双引擎，高精度、高时效低空微气象保障
- 三层算法架构，全局最优 + 实时避障 + 任务调度全覆盖
- 全栈Web系统，可视化、多角色、可扩展、易部署
- 云端+端侧一体，满足联网/离线、固定/移动场景
- 合规适配，支撑无人机超视距飞行审定要求

## 版本信息

- **版本**：1.0.0
- **更新日期**：2026-04-15
- **开发者**：成都大学无人机路径规划系统团队
