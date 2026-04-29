"""
three_layer_planner.py
三层路径规划算法实现
顶层: VRPTW (带时间窗的车辆路径问题)
中层: A* (全局路径规划)
底层: DWA (动态窗口法实时避障)
"""

import numpy as np
import heapq
from dataclasses import dataclass, field
from typing import List, Tuple, Dict, Optional, Callable
from enum import Enum
import copy
import logging

logger = logging.getLogger(__name__)


class RiskLevel(Enum):
    """气象风险等级"""
    LOW = 1      # 方差 < 1.0
    MEDIUM = 2   # 1.0 <= 方差 < 2.0
    HIGH = 3     # 2.0 <= 方差 < 3.0
    EXTREME = 4  # 方差 >= 3.0


@dataclass
class Task:
    """任务点"""
    id: int
    x: float  # 经度或x坐标
    y: float  # 纬度或y坐标
    z: float = 50  # 高度(m)
    demand: float = 1.0  # 需求量(kg)
    early_time: float = 0  # 时间窗开始(分钟)
    late_time: float = 1440  # 时间窗结束(分钟)
    service_time: float = 5  # 服务时间(分钟)
    priority: int = 1  # 优先级


@dataclass
class Drone:
    """无人机"""
    id: int
    max_speed: float = 15.0  # m/s
    max_capacity: float = 5.0  # kg
    max_battery: float = 30.0  # 分钟飞行时间
    cruise_speed: float = 12.0  # 经济巡航速度
    wind_resistance: float = 10.0  # 最大抗风等级 m/s


@dataclass
class VRPTWSolution:
    """VRPTW解决方案"""
    routes: Dict[int, List[Task]]  # 无人机ID -> 任务序列
    total_distance: float
    total_time: float
    total_risk: float
    feasibility: bool  # 是否满足所有约束


class VarianceField:
    """方差场包装类"""
    
    def __init__(self, variance_map: np.ndarray, resolution: float, 
                 origin: Tuple[float, float] = (0, 0)):
        self.variance = variance_map
        self.resolution = resolution
        self.origin = origin  # 左下角坐标
        self.shape = variance_map.shape
        
    def get_risk_at(self, x: float, y: float) -> Tuple[float, RiskLevel]:
        """获取指定位置的风险值和等级"""
        # 转换为栅格索引
        ix = int((x - self.origin[0]) / self.resolution)
        iy = int((y - self.origin[1]) / self.resolution)
        
        # 边界检查
        if 0 <= ix < self.shape[0] and 0 <= iy < self.shape[1]:
            var = self.variance[ix, iy]
        else:
            var = 0  # 区域外假设无风险
        
        # 确定风险等级
        if var < 1.0:
            level = RiskLevel.LOW
        elif var < 2.0:
            level = RiskLevel.MEDIUM
        elif var < 3.0:
            level = RiskLevel.HIGH
        else:
            level = RiskLevel.EXTREME
            
        return var, level
    
    def get_risk_cost(self, x: float, y: float) -> float:
        """获取风险代价（用于路径规划加权）"""
        var, level = self.get_risk_at(x, y)
        
        # 风险代价系数
        risk_multiplier = {
            RiskLevel.LOW: 1.0,
            RiskLevel.MEDIUM: 1.3,
            RiskLevel.HIGH: 1.8,
            RiskLevel.EXTREME: 10.0  # 几乎不可通行
        }
        
        return risk_multiplier[level]


# ==================== 顶层: VRPTW任务调度 ====================

class VRPTWSolver:
    """
    带时间窗的车辆路径问题求解器
    使用节约算法(Clarke-Wright) + 局部搜索改进
    """
    
    def __init__(self, variance_field: Optional[VarianceField] = None):
        self.variance_field = variance_field
        self.distance_matrix = None
        self.time_matrix = None
        self.risk_matrix = None
        
    def _calculate_distance(self, t1: Task, t2: Task) -> float:
        """计算两点间距离（欧氏距离）"""
        return np.sqrt((t1.x - t2.x)**2 + (t1.y - t2.y)**2 + (t1.z - t2.z)**2)
    
    def _calculate_time(self, t1: Task, t2: Task, drone: Drone) -> float:
        """计算飞行时间（考虑风速影响）"""
        dist = self._calculate_distance(t1, t2)
        
        # 如果有方差场，考虑气象风险对速度的影响
        if self.variance_field:
            mid_x, mid_y = (t1.x + t2.x) / 2, (t1.y + t2.y) / 2
            var, level = self.variance_field.get_risk_at(mid_x, mid_y)
            
            # 高方差区域降低速度
            speed_factor = {
                RiskLevel.LOW: 1.0,
                RiskLevel.MEDIUM: 0.9,
                RiskLevel.HIGH: 0.7,
                RiskLevel.EXTREME: 0.3
            }
            speed = drone.cruise_speed * speed_factor[level]
        else:
            speed = drone.cruise_speed
            
        return dist / speed / 60  # 转换为分钟
    
    def _calculate_risk(self, t1: Task, t2: Task) -> float:
        """计算路径段风险值"""
        if not self.variance_field:
            return 0.0
            
        # 采样路径上的风险点
        n_samples = 5
        total_risk = 0
        
        for i in range(n_samples + 1):
            ratio = i / n_samples
            x = t1.x + ratio * (t2.x - t1.x)
            y = t1.y + ratio * (t2.y - t1.y)
            risk_cost = self.variance_field.get_risk_cost(x, y)
            total_risk += risk_cost
            
        return total_risk / (n_samples + 1)
    
    def build_matrices(self, tasks: List[Task], depot: Task, drones: List[Drone]):
        """构建距离、时间、风险矩阵"""
        all_nodes = [depot] + tasks
        n = len(all_nodes)
        
        self.distance_matrix = np.zeros((n, n))
        self.time_matrix = np.zeros((n, n))
        self.risk_matrix = np.zeros((n, n))
        
        # 使用第一架无人机计算时间（假设机队同质）
        ref_drone = drones[0] if drones else Drone(0)
        
        for i in range(n):
            for j in range(n):
                if i != j:
                    self.distance_matrix[i][j] = self._calculate_distance(all_nodes[i], all_nodes[j])
                    self.time_matrix[i][j] = self._calculate_time(all_nodes[i], all_nodes[j], ref_drone)
                    self.risk_matrix[i][j] = self._calculate_risk(all_nodes[i], all_nodes[j])
    
    def solve(self, tasks: List[Task], drones: List[Drone], 
            depot: Task, time_limit: float = 60.0) -> VRPTWSolution:
        """
        求解VRPTW
        
        策略：
        1. 节约算法生成初始解
        2. 基于方差场进行任务聚类（优先分配低风险区域任务给同一无人机）
        3. 局部搜索改进
        """
        if not tasks or not drones:
            return VRPTWSolution({}, 0, 0, 0, False)
        
        self.build_matrices(tasks, depot, drones)
        
        # 基于方差场的任务聚类（预分配策略）
        task_clusters = self._cluster_tasks_by_risk(tasks, len(drones))
        
        # 为每架无人机分配任务
        routes = {}
        unassigned = set(range(len(tasks)))
        
        for drone_idx, drone in enumerate(drones):
            if drone_idx >= len(task_clusters):
                break
                
            cluster_tasks = [tasks[i] for i in task_clusters[drone_idx] if i < len(tasks)]
            route = self._solve_single_drone(cluster_tasks, drone, depot)
            
            if route:
                routes[drone.id] = route
                # 标记已分配
                for task in route:
                    if task.id != depot.id:
                        task_idx = next(i for i, t in enumerate(tasks) if t.id == task.id)
                        unassigned.discard(task_idx)
        
        # 处理未分配的任务
        if unassigned:
            remaining = [tasks[i] for i in unassigned]
            for drone in drones:
                if drone.id not in routes:
                    routes[drone.id] = []
                
                route = self._solve_single_drone(remaining, drone, depot, routes[drone.id])
                if route:
                    routes[drone.id] = route
                    break
        
        # 计算总指标
        total_dist, total_time, total_risk = 0, 0, 0
        
        # 确保矩阵已正确初始化
        if self.distance_matrix is None or self.time_matrix is None or self.risk_matrix is None:
            logger.error("距离/时间/风险矩阵未初始化")
            return VRPTWSolution(routes, total_dist, total_time, total_risk, False)
        
        all_nodes = [depot] + tasks
        node_index_map = {node.id: idx for idx, node in enumerate(all_nodes)}
        
        for drone_id, route in routes.items():
            for i in range(len(route) - 1):
                current_task = route[i]
                next_task = route[i + 1]
                
                # 使用映射获取正确的索引
                if current_task.id not in node_index_map or next_task.id not in node_index_map:
                    logger.warning(f"任务ID不在节点映射中: {current_task.id} 或 {next_task.id}")
                    continue
                
                idx1 = node_index_map[current_task.id]
                idx2 = node_index_map[next_task.id]
                
                # 边界检查
                if (0 <= idx1 < len(all_nodes) and 0 <= idx2 < len(all_nodes) and
                    idx1 < self.distance_matrix.shape[0] and idx2 < self.distance_matrix.shape[1]):
                    total_dist += self.distance_matrix[idx1][idx2]
                    total_time += self.time_matrix[idx1][idx2]
                    total_risk += self.risk_matrix[idx1][idx2]
                else:
                    logger.warning(f"索引越界: idx1={idx1}, idx2={idx2}, matrix_shape={self.distance_matrix.shape}")
        
        return VRPTWSolution(routes, total_dist, total_time, total_risk, True)
    def _cluster_tasks_by_risk(self, tasks: List[Task], n_clusters: int) -> List[List[int]]:
        """基于方差场将任务聚类到不同风险区域"""
        if not self.variance_field or n_clusters <= 1:
            return [list(range(len(tasks)))]
        
        # 计算每个任务的风险等级
        task_risks = []
        for i, task in enumerate(tasks):
            var, level = self.variance_field.get_risk_at(task.x, task.y)
            task_risks.append((i, var, level))
        
        # 按风险排序并分组
        task_risks.sort(key=lambda x: x[1])
        
        # 均衡分组
        cluster_size = len(tasks) // n_clusters
        clusters = []
        for i in range(n_clusters):
            start = i * cluster_size
            end = start + cluster_size if i < n_clusters - 1 else len(tasks)
            clusters.append([task_risks[j][0] for j in range(start, end)])
        
        return clusters
    
    def _solve_single_drone(self, tasks: List[Task], drone: Drone, 
                           depot: Task, existing_route: Optional[List[Task]] = None) -> List[Task]:
        """为单架无人机求解路径（插入启发式）"""
        if not tasks:
            return [depot, depot]
        
        # 按时间窗排序
        sorted_tasks = sorted(tasks, key=lambda t: t.early_time)
        
        # 构建路径
        route = [depot]
        current_time = 0
        current_load = 0
        
        for task in sorted_tasks:
            # 检查约束
            travel_time = self._calculate_time(route[-1], task, drone) if route else 0
            
            arrival_time = current_time + travel_time
            
            # 时间窗约束
            if arrival_time > task.late_time:
                continue  # 跳过，时间窗不满足
            
            # 容量约束
            if current_load + task.demand > drone.max_capacity:
                continue
            
            # 续航约束
            return_time = self._calculate_time(task, depot, drone) / 60
            if arrival_time + task.service_time + return_time > drone.max_battery:
                continue
            
            # 插入任务
            wait_time = max(0, task.early_time - arrival_time)
            current_time = arrival_time + wait_time + task.service_time
            current_load += task.demand
            route.append(task)
        
        route.append(depot)
        return route


# ==================== 中层: A*全局路径规划 ====================

class AStarPlanner:
    """
    A*算法全局路径规划
    在栅格地图上规划，考虑方差场风险权重
    """
    
    def __init__(self, variance_field: VarianceField, 
                 obstacles: Optional[np.ndarray] = None):
        self.variance_field = variance_field
        self.obstacles = obstacles  # 障碍物栅格图
        self.resolution = variance_field.resolution
        
    def heuristic(self, a: Tuple[int, int], b: Tuple[int, int]) -> float:
        """启发函数：欧氏距离"""
        return np.sqrt((a[0]-b[0])**2 + (a[1]-b[1])**2) * self.resolution
    
    def get_neighbors(self, node: Tuple[int, int]) -> List[Tuple[int, int]]:
        """获取邻居节点（8连通）"""
        x, y = node
        neighbors = []
        
        for dx, dy in [(-1,0), (1,0), (0,-1), (0,1), (-1,-1), (-1,1), (1,-1), (1,1)]:
            nx, ny = x + dx, y + dy
            
            # 边界检查
            if 0 <= nx < self.variance_field.shape[0] and \
               0 <= ny < self.variance_field.shape[1]:
                
                # 障碍物检查
                if self.obstacles is None or not self.obstacles[nx, ny]:
                    neighbors.append((nx, ny))
        
        return neighbors
    
    def get_move_cost(self, from_node: Tuple[int, int], 
                     to_node: Tuple[int, int]) -> float:
        """计算移动代价 = 距离代价 + 风险代价"""
        # 距离代价
        dist = np.sqrt((from_node[0]-to_node[0])**2 + 
                      (from_node[1]-to_node[1])**2) * self.resolution
        
        # 风险代价（使用目标节点位置）
        x = to_node[0] * self.resolution + self.variance_field.origin[0]
        y = to_node[1] * self.resolution + self.variance_field.origin[1]
        risk_cost = self.variance_field.get_risk_cost(x, y)
        
        # 综合代价（可调整权重）
        return dist * risk_cost
    
    def plan(self, start: Tuple[float, float], 
             goal: Tuple[float, float]) -> Optional[List[Tuple[float, float]]]:
        """
        A*路径规划
        
        Args:
            start: (x, y) 起点坐标
            goal: (x, y) 终点坐标
            
        Returns:
            路径点列表，如果无解返回None
        """
        # 转换为栅格索引
        start_idx = (
            int((start[0] - self.variance_field.origin[0]) / self.resolution),
            int((start[1] - self.variance_field.origin[1]) / self.resolution)
        )
        goal_idx = (
            int((goal[0] - self.variance_field.origin[0]) / self.resolution),
            int((goal[1] - self.variance_field.origin[1]) / self.resolution)
        )
        
        # 检查起点终点有效性
        if not (0 <= start_idx[0] < self.variance_field.shape[0] and 
                0 <= start_idx[1] < self.variance_field.shape[1]):
            logger.error("起点在地图外")
            return None
            
        if not (0 <= goal_idx[0] < self.variance_field.shape[0] and 
                0 <= goal_idx[1] < self.variance_field.shape[1]):
            logger.error("终点在地图外")
            return None
        
        # A*算法
        frontier = []
        heapq.heappush(frontier, (0, start_idx))
        came_from = {start_idx: None}
        cost_so_far = {start_idx: 0.0}
        
        while frontier:
            _, current = heapq.heappop(frontier)
            
            if current == goal_idx:
                break
            
            for next_node in self.get_neighbors(current):
                new_cost = cost_so_far[current] + self.get_move_cost(current, next_node)
                
                if next_node not in cost_so_far or new_cost < cost_so_far[next_node]:
                    cost_so_far[next_node] = new_cost
                    priority = new_cost + self.heuristic(next_node, goal_idx)
                    heapq.heappush(frontier, (priority, next_node))
                    came_from[next_node] = current
        
        # 重建路径
        if goal_idx not in came_from:
            logger.warning("未找到可行路径")
            return None
        
        path = []
        current = goal_idx
        while current is not None:
            x = current[0] * self.resolution + self.variance_field.origin[0]
            y = current[1] * self.resolution + self.variance_field.origin[1]
            path.append((x, y))
            current = came_from[current]
        
        path.reverse()
        return path


# ==================== 底层: DWA动态窗口法 ====================

class DWAController:
    """
    动态窗口法(Dynamic Window Approach)
    用于实时避障和抗风扰动
    """
    
    def __init__(self, 
                 max_speed: float = 15.0,
                 max_yaw_rate: float = 1.0,
                 max_accel: float = 3.0,
                 max_dyaw_rate: float = 1.0,
                 velocity_resolution: float = 0.5,
                 yaw_rate_resolution: float = 0.1,
                 dt: float = 0.1,
                 predict_time: float = 3.0,
                 variance_field: Optional[VarianceField] = None):
        
        self.max_speed = max_speed
        self.max_yaw_rate = max_yaw_rate
        self.max_accel = max_accel
        self.max_dyaw_rate = max_dyaw_rate
        self.v_res = velocity_resolution
        self.yaw_res = yaw_rate_resolution
        self.dt = dt
        self.predict_time = predict_time
        self.variance_field = variance_field
        
        # 评价函数权重
        self.weight_heading = 0.8
        self.weight_distance = 1.0
        self.weight_velocity = 0.5
        self.weight_risk = 2.0  # 风险权重最高
    
    def calculate_dynamic_window(self, current_speed: float, 
                                current_yaw_rate: float) -> Tuple[float, float, float, float]:
        """
        计算动态窗口
        基于当前速度和加速度限制
        """
        # 速度限制
        vs = [0, self.max_speed]
        
        # 加速度限制
        vd = [
            current_speed - self.max_accel * self.dt,
            current_speed + self.max_accel * self.dt
        ]
        
        # 角速度限制
        yawd = [
            current_yaw_rate - self.max_dyaw_rate * self.dt,
            current_yaw_rate + self.max_dyaw_rate * self.dt
        ]
        
        # 交集
        dw = [
            max(vs[0], vd[0]),
            min(vs[1], vd[1]),
            max(-self.max_yaw_rate, yawd[0]),
            min(self.max_yaw_rate, yawd[1])
        ]
        
        return (dw[0], dw[1], dw[2], dw[3])
    
    def predict_trajectory(self, x: np.ndarray, v: float, w: float) -> np.ndarray:
        """
        预测轨迹
        
        Args:
            x: 当前状态 [x, y, yaw, speed]
            v: 线速度
            w: 角速度
            
        Returns:
            轨迹点数组 shape: (n_steps, 3) [x, y, yaw]
        """
        trajectory = np.array([x[:3]])
        time = 0
        
        while time <= self.predict_time:
            # 运动学模型
            x[0] += v * np.cos(x[2]) * self.dt
            x[1] += v * np.sin(x[2]) * self.dt
            x[2] += w * self.dt
            x[3] = v
            
            trajectory = np.vstack((trajectory, [x[0], x[1], x[2]]))
            time += self.dt
        
        return trajectory
    
    def calculate_heading_cost(self, trajectory: np.ndarray, 
                              goal: Tuple[float, float]) -> float:
        """朝向目标点的代价"""
        dx = goal[0] - trajectory[-1, 0]
        dy = goal[1] - trajectory[-1, 1]
        goal_yaw = np.arctan2(dy, dx)
        
        error_yaw = goal_yaw - trajectory[-1, 2]
        cost = abs(np.arctan2(np.sin(error_yaw), np.cos(error_yaw)))
        
        return cost
    
    def calculate_distance_cost(self, trajectory: np.ndarray, 
                               obstacles: List[Tuple[float, float, float]]) -> float:
        """障碍物距离代价"""
        if not obstacles:
            return 0
        
        min_dist = float('inf')
        
        for point in trajectory:
            for obs in obstacles:
                dist = np.sqrt((point[0] - obs[0])**2 + (point[1] - obs[1])**2) - obs[2]
                if dist < min_dist:
                    min_dist = dist
        
        # 如果碰撞，返回大代价
        if min_dist < 0:
            return float('inf')
        
        # 距离越近代价越高
        return 1.0 / (min_dist + 0.1)
    
    def calculate_risk_cost(self, trajectory: np.ndarray) -> float:
        """气象风险代价"""
        if not self.variance_field:
            return 0
        
        total_risk = 0
        for point in trajectory:
            risk = self.variance_field.get_risk_cost(point[0], point[1])
            total_risk += risk
        
        return total_risk / len(trajectory)
    
    def calculate_velocity_cost(self, v: float) -> float:
        """速度代价（鼓励高速）"""
        return self.max_speed - v
    
    def compute_control(self, 
                       current_state: np.ndarray,
                       goal: Tuple[float, float],
                       obstacles: List[Tuple[float, float, float]],
                       global_path: Optional[List[Tuple[float, float]]] = None) -> Tuple[float, float]:
        """
        计算控制指令
        
        Args:
            current_state: [x, y, yaw, speed]
            goal: 目标点
            obstacles: 障碍物列表 [(x, y, radius), ...]
            global_path: 全局引导路径（防止局部最优）
            
        Returns:
            (v, w) 线速度和角速度指令
        """
        best_cost = float('inf')
        best_v, best_w = 0, 0
        
        # 计算动态窗口
        dw = self.calculate_dynamic_window(current_state[3], 0)  # 简化：当前角速度设为0
        
        # 遍历速度空间
        for v in np.arange(dw[0], dw[1], self.v_res):
            for w in np.arange(dw[2], dw[3], self.yaw_res):
                # 预测轨迹
                trajectory = self.predict_trajectory(current_state.copy(), v, w)
                
                # 检查是否偏离全局路径太远（如果有）
                if global_path:
                    deviation = self._calculate_path_deviation(trajectory, global_path)
                    if deviation > 20:  # 偏离超过20米
                        continue
                
                # 计算各项代价
                heading_cost = self.calculate_heading_cost(trajectory, goal)
                dist_cost = self.calculate_distance_cost(trajectory, obstacles)
                
                if dist_cost == float('inf'):
                    continue  # 碰撞轨迹跳过
                
                velocity_cost = self.calculate_velocity_cost(v)
                risk_cost = self.calculate_risk_cost(trajectory)
                
                # 总代价
                total_cost = (
                    self.weight_heading * heading_cost +
                    self.weight_distance * dist_cost +
                    self.weight_velocity * velocity_cost +
                    self.weight_risk * risk_cost
                )
                
                if total_cost < best_cost:
                    best_cost = total_cost
                    best_v, best_w = v, w
        
        return best_v, best_w
    
    def _calculate_path_deviation(self, trajectory: np.ndarray, 
                                  global_path: List[Tuple[float, float]]) -> float:
        """计算轨迹偏离全局路径的距离"""
        max_deviation = 0
        
        for point in trajectory:
            # 找到最近的全局路径点
            min_dist = min(
                np.sqrt((point[0] - gp[0])**2 + (point[1] - gp[1])**2)
                for gp in global_path
            )
            max_deviation = max(max_deviation, min_dist)
        
        return max_deviation


# ==================== 集成接口 ====================

class PathPlanningSystem:
    """
    三层路径规划系统集成接口
    """
    
    def __init__(self, variance_field: VarianceField):
        self.variance_field = variance_field
        self.vrptw_solver = VRPTWSolver(variance_field)
        self.astar_planner = None  # 按需初始化
        self.dwa_controller = DWAController(variance_field=variance_field)
        
    def global_planning(self, 
                       tasks: List[Task],
                       drones: List[Drone],
                       depot: Task) -> VRPTWSolution:
        """
        全局任务规划（顶层VRPTW）
        """
        return self.vrptw_solver.solve(tasks, drones, depot)
    
    def local_planning(self,
                      start: Tuple[float, float],
                      goal: Tuple[float, float],
                      obstacles: Optional[np.ndarray] = None) -> Optional[List[Tuple[float, float]]]:
        """
        局部路径规划（中层A*）
        """
        if self.astar_planner is None or obstacles is not None:
            self.astar_planner = AStarPlanner(self.variance_field, obstacles)
        
        return self.astar_planner.plan(start, goal)
    
    def real_time_control(self,
                         current_state: np.ndarray,
                         goal: Tuple[float, float],
                         obstacles: List[Tuple[float, float, float]],
                         global_path: Optional[List[Tuple[float, float]]] = None) -> Tuple[float, float]:
        """
        实时控制（底层DWA）
        """
        return self.dwa_controller.compute_control(
            current_state, goal, obstacles, global_path
        )
    
    def replanning_trigger(self, 
                          previous_variance: np.ndarray,
                          current_variance: np.ndarray,
                          threshold: float = 3.0) -> bool:
        """
        检查是否需要触发重规划
        """
        # 检测方差突变
        if np.any(current_variance > threshold):
            mutation_ratio = np.sum(current_variance > threshold) / current_variance.size
            if mutation_ratio > 0.1:  # 超过10%区域突变
                logger.warning(f"检测到大规模气象突变，触发重规划")
                return True
        
        return False


# 使用示例
if __name__ == "__main__":
    # 创建示例方差场 (1000m × 1000m, 10m分辨率)
    variance_map = np.random.exponential(1.5, (100, 100))
    # 添加一个高风险区域
    variance_map[40:60, 40:60] = 4.0
    
    vf = VarianceField(variance_map, resolution=10.0, origin=(0, 0))
    
    # 初始化规划系统
    planner = PathPlanningSystem(vf)
    
    # 示例任务
    depot = Task(0, 50, 50, 50, 0, 1440, 0)
    tasks = [
        Task(1, 200, 300, 80, demand=2, early_time=0, late_time=60),
        Task(2, 500, 600, 80, demand=1.5, early_time=30, late_time=90),
        Task(3, 800, 200, 80, demand=1, early_time=60, late_time=120),
    ]
    
    drones = [
        Drone(1, max_capacity=5, max_battery=30),
        Drone(2, max_capacity=5, max_battery=30),
    ]
    
    # 1. 全局任务规划
    solution = planner.global_planning(tasks, drones, depot)
    print(f"VRPTW方案: {len(solution.routes)} 架无人机")
    for drone_id, route in solution.routes.items():
        task_ids = [t.id for t in route]
        print(f"  无人机{drone_id}: {task_ids}")
    
    # 2. 局部路径规划 (避开高风险区)
    path = planner.local_planning((50, 50), (500, 600))
    print(f"A*路径点数: {len(path) if path else 0}")
    
    # 3. 实时控制
    state = np.array([50.0, 50.0, 0.0, 10.0])  # x, y, yaw, speed
    obstacles = [(300.0, 400.0, 20.0)]  # 动态障碍物
    v, w = planner.real_time_control(state, (500, 600), obstacles, path)
    print(f"DWA控制指令: 速度={v:.2f}m/s, 角速度={w:.2f}rad/s")