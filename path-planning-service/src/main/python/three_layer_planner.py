#!/usr/bin/env python3
"""
三层路径规划服务
集成VRPTW、A*和DWA算法，支持气象风险感知

新增功能：
- 气象风险映射：将风场、湍流映射为飞行风险
- 风险感知路径规划：在路径规划中考虑气象风险
"""

import heapq
import numpy as np
import json
import sys
import os
import logging
import threading
import concurrent.futures
from typing import List, Dict, Optional, Tuple

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 尝试导入风险映射模块
try:
    # 添加data-assimilation-platform的路径
    PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    DATA_ASSIMILATION_PATH = os.path.join(PROJECT_ROOT, '..', 'data-assimilation-platform',
                                          'algorithm_core', 'src')
    if DATA_ASSIMILATION_PATH not in sys.path:
        sys.path.insert(0, DATA_ASSIMILATION_PATH)

    from bayesian_assimilation.utils.risk_mapper import (
        WeatherToRiskMapper,  # type: ignore[assignment]
        RiskAwarePathCostCalculator,  # type: ignore[assignment]
        RiskLevel  # type: ignore[assignment]
    )
    RISK_MAPPER_AVAILABLE = True
    logger.info("风险映射模块加载成功")
except ImportError as e:
    RISK_MAPPER_AVAILABLE = False
    logger.warning(f"风险映射模块不可用: {e}")

    # 定义简化版本
    class RiskLevel:
        LOW = "LOW"
        MEDIUM = "MEDIUM"
        HIGH = "HIGH"
        EXTREME = "EXTREME"

    class WeatherToRiskMapper:
        def __init__(self, grid_resolution=100.0, constraints=None):
            self.grid_resolution = grid_resolution

        def compute_comprehensive_risk(self, assimilation_result, heading=0.0):
            shape = assimilation_result.get('grid', {}).get('shape', (10, 10))
            return {
                'risk_grid': np.zeros(shape),
                'risk_level': np.full(shape, RiskLevel.LOW, dtype=object),
                'summary': {'avg_risk': 0.0, 'max_risk': 0.0}
            }

    class RiskAwarePathCostCalculator:
        def __init__(self, mapper):
            self.mapper = mapper

        def set_risk_field(self, risk_result):
            pass

        def get_risk_at_position(self, position):
            return 0.0

        def compute_segment_risk_cost(self, start, end, steps=10):
            return 0.0

# 缓存机制


class Cache:

    def __init__(self, max_size=1000):
        self.max_size = max_size
        self.cache = {}
        self.lock = threading.Lock()

    def get(self, key):
        with self.lock:
            return self.cache.get(key)

    def set(self, key, value):
        with self.lock:
            if len(self.cache) >= self.max_size:
                # 移除最早的项
                oldest_key = next(iter(self.cache))
                del self.cache[oldest_key]
            self.cache[key] = value


# 全局缓存实例
vrptw_cache = Cache()
astar_cache = Cache()
derrt_cache = Cache()
dwa_cache = Cache()


class Drone:
    """
    无人机类
    """

    def __init__(self, id: str, max_payload: float, max_endurance: float, max_speed: float):
        self.id = id
        self.max_payload = max_payload
        self.max_endurance = max_endurance
        self.max_speed = max_speed
        self.current_payload = 0.0
        self.current_endurance = max_endurance


class Task:
    """
    任务类
    """

    def __init__(self, id: str, location: Tuple[float, float],
                 demand: float, start_time: float, end_time: float):
        self.id = id
        self.location = location
        self.demand = demand
        self.start_time = start_time
        self.end_time = end_time


class Obstacle:
    """
    障碍物类
    """

    def __init__(self, location: Tuple[float, float], radius: float):
        self.location = location
        self.radius = radius


class NoFlyZone:
    """
    禁飞区类
    """

    def __init__(self, location: Tuple[float, float], radius: float):
        self.location = location
        self.radius = radius


class VRPTWPlanner:
    """
    VRPTW规划器
    """

    def __init__(self, drones: List[Drone], tasks: List[Task], weather_data: Optional[Dict] = None):
        self.drones = drones
        self.tasks = tasks
        self.weather_data = weather_data or {}

    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离
        """
        return np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)

    def calculate_time(self, distance: float, speed: float) -> float:
        """
        计算飞行时间
        """
        return distance / speed

    def plan(self) -> Dict:
        """
        执行VRPTW规划
        """
        try:
            # 生成缓存键
            cache_key = str([d.id for d in self.drones]) + \
                str([t.id for t in self.tasks]) + str(self.weather_data)
            # 检查缓存
            cached_result = vrptw_cache.get(cache_key)
            if cached_result:
                logger.info("使用缓存的VRPTW规划结果")
                return cached_result

            # 简化的节约算法实现
            routes = []
            unassigned_tasks = self.tasks.copy()

            for drone in self.drones:
                route = {
                    'drone_id': drone.id,
                    'tasks': [],
                    'total_distance': 0,
                    'total_time': 0,
                    'total_payload': 0
                }

                current_location = (0, 0)  # 假设基地位置
                current_time = 0.0

                while unassigned_tasks and drone.current_endurance > 0:
                    # 选择最近的任务
                    nearest_task = None
                    min_distance = float('inf')

                    for task in unassigned_tasks:
                        distance = self.calculate_distance(current_location, task.location)
                        if (distance < min_distance and
                                drone.current_payload + task.demand <= drone.max_payload):
                            min_distance = distance
                            nearest_task = task

                    if not nearest_task:
                        break

                    # 计算飞行时间
                    flight_time = self.calculate_time(min_distance, drone.max_speed)

                    # 检查时间窗和续航
                    if current_time + flight_time > nearest_task.end_time:
                        continue

                    if flight_time > drone.current_endurance:
                        break

                    # 如果早于最早开始时间，需要等待
                    if current_time + flight_time < nearest_task.start_time:
                        wait_time = nearest_task.start_time - (current_time + flight_time)
                        flight_time += wait_time

                    # 添加任务到路径
                    route['tasks'].append(nearest_task.id)
                    route['total_distance'] += min_distance
                    route['total_time'] += flight_time
                    route['total_payload'] += nearest_task.demand

                    # 更新无人机状态
                    drone.current_payload += nearest_task.demand
                    drone.current_endurance -= flight_time

                    # 更新当前位置和时间
                    current_location = nearest_task.location
                    current_time += flight_time

                    # 从待分配任务中移除
                    unassigned_tasks.remove(nearest_task)

                # 计算返回基地的距离和时间
                return_distance = self.calculate_distance(current_location, (0, 0))
                return_time = self.calculate_time(return_distance, drone.max_speed)

                if return_time <= drone.current_endurance:
                    route['total_distance'] += return_distance
                    route['total_time'] += return_time
                    drone.current_endurance -= return_time

                routes.append(route)

            result = {
                'success': True,
                'routes': routes,
                'unassigned_tasks': [task.id for task in unassigned_tasks]
            }

            # 缓存结果
            vrptw_cache.set(cache_key, result)

            logger.info("VRPTW规划完成")
            return result

        except Exception as e:
            logger.error(f"VRPTW规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }


class AStarPlanner:
    """
    A*路径规划器
    """

    def __init__(
        self,
        weather_data: Optional[Dict] = None,
        obstacles: Optional[List[Obstacle]] = None,
        no_fly_zones: Optional[List[NoFlyZone]] = None
    ):
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []
        self.no_fly_zones = no_fly_zones or []

    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离
        """
        return np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)

    def is_collision(self, location: Tuple[float, float]) -> bool:
        """
        检查是否碰撞
        """
        # 检查障碍物
        for obstacle in self.obstacles:
            distance = self.calculate_distance(location, obstacle.location)
            if distance < obstacle.radius:
                return True

        # 检查禁飞区
        for no_fly_zone in self.no_fly_zones:
            distance = self.calculate_distance(location, no_fly_zone.location)
            if distance < no_fly_zone.radius:
                return True

        return False

    def plan(self, start: Tuple[float, float], goal: Tuple[float, float]) -> Dict:
        """
        执行A*路径规划
        """
        try:
            # 生成缓存键
            cache_key = (
                    str(start) + str(goal) +
                    str([(o.location, o.radius) for o in self.obstacles]) +
                    str([(n.location, n.radius) for n in self.no_fly_zones])
                )
            # 检查缓存
            cached_result = astar_cache.get(cache_key)
            if cached_result:
                logger.info("使用缓存的A*规划结果")
                return cached_result

            # 简化的A*实现
            open_set = {start}
            heap = [(0.0, 0, start)]
            tie_breaker = 1
            came_from = {}
            g_score = {start: 0.0}
            f_score = {start: self.calculate_distance(start, goal)}

            while heap:
                _, _, current = heapq.heappop(heap)
                if current not in open_set:
                    continue

                if current == goal:
                    # 重建路径
                    path = []
                    while current in came_from:
                        path.append(current)
                        current = came_from[current]
                    path.append(start)
                    path.reverse()

                    result = {
                        'success': True,
                        'path': path,
                        'distance': g_score[goal]
                    }

                    # 缓存结果
                    astar_cache.set(cache_key, result)

                    logger.info("A*路径规划完成")
                    return result

                open_set.remove(current)

                # 生成邻居节点
                neighbors = [
                    (current[0] + 1.0, current[1]),
                    (current[0] - 1.0, current[1]),
                    (current[0], current[1] + 1.0),
                    (current[0], current[1] - 1.0),
                    (current[0] + 1.0, current[1] + 1.0),
                    (current[0] + 1.0, current[1] - 1.0),
                    (current[0] - 1.0, current[1] + 1.0),
                    (current[0] - 1.0, current[1] - 1.0)
                ]

                for neighbor in neighbors:
                    # 检查是否碰撞
                    if self.is_collision(neighbor):
                        continue

                    # 计算g_score
                    tentative_g_score = g_score[current] + \
                        self.calculate_distance(current, neighbor)

                    if neighbor not in g_score or tentative_g_score < g_score[neighbor]:
                        came_from[neighbor] = current
                        g_score[neighbor] = tentative_g_score
                        f_score[neighbor] = tentative_g_score + \
                            self.calculate_distance(neighbor, goal)

                        if neighbor not in open_set:
                            open_set.add(neighbor)
                            heapq.heappush(heap, (f_score[neighbor], tie_breaker, neighbor))
                            tie_breaker += 1

            logger.warning("无法找到路径")
            return {
                'success': False,
                'error': '无法找到路径'
            }

        except Exception as e:
            logger.error(f"A*路径规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }


class DERRTStarPlanner:
    """
    DE-RRT*路径规划器
    """

    def __init__(
        self,
        weather_data: Optional[Dict] = None,
        obstacles: Optional[List[Obstacle]] = None,
        no_fly_zones: Optional[List[NoFlyZone]] = None
    ):
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []
        self.no_fly_zones = no_fly_zones or []
        self.max_iterations = 500  # 减少迭代次数以提高速度
        self.goal_bias = 0.2  # 增加目标偏置以提高收敛速度
        self.max_step = 10.0  # 增加步长以提高速度
        self.rewire_radius = 15.0  # 增加重连半径以提高效率

    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离
        """
        return np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)

    def is_collision(self, location: Tuple[float, float]) -> bool:
        """
        检查是否碰撞
        """
        # 检查障碍物
        for obstacle in self.obstacles:
            distance = self.calculate_distance(location, obstacle.location)
            if distance < obstacle.radius:
                return True

        # 检查禁飞区
        for no_fly_zone in self.no_fly_zones:
            distance = self.calculate_distance(location, no_fly_zone.location)
            if distance < no_fly_zone.radius:
                return True

        return False

    def is_collision_free(self, start: Tuple[float, float], end: Tuple[float, float]) -> bool:
        """
        检查线段是否碰撞
        """
        steps = 5  # 减少检查步数以提高速度
        for i in range(steps + 1):
            t = i / steps
            x = start[0] + t * (end[0] - start[0])
            y = start[1] + t * (end[1] - start[1])
            if self.is_collision((x, y)):
                return False
        return True

    def sample(self, goal: Tuple[float, float]) -> Tuple[float, float]:
        """
        采样随机点
        """
        if np.random.rand() < self.goal_bias:
            return goal
        else:
            # 假设环境范围为[-100, 100]
            return (np.random.uniform(-100, 100), np.random.uniform(-100, 100))

    def nearest(self, nodes: List[Tuple[float, float]],
                point: Tuple[float, float]) -> Optional[Tuple[float, float]]:
        """
        找到最近的节点
        """
        min_dist = float('inf')
        nearest_node = None
        for node in nodes:
            dist = self.calculate_distance(node, point)
            if dist < min_dist:
                min_dist = dist
                nearest_node = node
        return nearest_node

    def steer(self, from_node: Tuple[float, float],
              to_point: Tuple[float, float]) -> Tuple[float, float]:
        """
        朝着目标点移动最大步长
        """
        dist = self.calculate_distance(from_node, to_point)
        if dist <= self.max_step:
            return to_point
        else:
            angle = np.arctan2(to_point[1] - from_node[1], to_point[0] - from_node[0])
            return (
                from_node[0] + self.max_step * np.cos(angle),
                from_node[1] + self.max_step * np.sin(angle)
            )

    def rewire(self, nodes: List[Tuple[float, float]],
               new_node: Tuple[float, float], parent_map: Dict, cost_map: Dict):
        """
        重连附近的节点
        """
        for node in nodes:
            if node == new_node:
                continue
            dist = self.calculate_distance(node, new_node)
            if dist <= self.rewire_radius:
                if self.is_collision_free(node, new_node):
                    new_cost = cost_map[node] + dist
                    if new_cost < cost_map.get(new_node, float('inf')):
                        parent_map[new_node] = node
                        cost_map[new_node] = new_cost

    def plan(self, start: Tuple[float, float], goal: Tuple[float, float]) -> Dict:
        """
        执行DE-RRT*路径规划
        """
        try:
            # 生成缓存键
            cache_key = (
                    str(start) + str(goal) +
                    str([(o.location, o.radius) for o in self.obstacles]) +
                    str([(n.location, n.radius) for n in self.no_fly_zones])
                )
            # 检查缓存
            cached_result = derrt_cache.get(cache_key)
            if cached_result:
                logger.info("使用缓存的DE-RRT*规划结果")
                return cached_result

            nodes = [start]
            parent_map: Dict[Tuple[float, float], Optional[Tuple[float, float]]] = {
                start: None
            }
            cost_map: Dict[Tuple[float, float], float] = {start: 0.0}

            for i in range(self.max_iterations):
                # 采样随机点
                sample_point = self.sample(goal)

                # 找到最近的节点
                nearest_node = self.nearest(nodes, sample_point)
                assert nearest_node is not None

                # 朝着采样点移动
                new_node = self.steer(nearest_node, sample_point)

                # 检查碰撞
                if not self.is_collision(new_node) and self.is_collision_free(
                        nearest_node, new_node):
                    # 添加新节点
                    nodes.append(new_node)
                    parent_map[new_node] = nearest_node
                    cost_map[new_node] = cost_map[nearest_node] + \
                        self.calculate_distance(nearest_node, new_node)

                    # 重连附近的节点
                    self.rewire(nodes, new_node, parent_map, cost_map)

                    # 检查是否到达目标
                    if self.calculate_distance(new_node, goal) < 2.0:  # 增加目标阈值以提高速度
                        # 连接到目标
                        if self.is_collision_free(new_node, goal):
                            nodes.append(goal)
                            parent_map[goal] = new_node
                            cost_map[goal] = cost_map[new_node] + \
                                self.calculate_distance(new_node, goal)

                            # 重建路径
                            path = []
                            current = goal
                            while current is not None:
                                path.append(current)
                                current = parent_map[current]
                            path.reverse()

                            result = {
                                'success': True,
                                'path': path,
                                'distance': cost_map[goal]
                            }

                            # 缓存结果
                            derrt_cache.set(cache_key, result)

                            logger.info("DE-RRT*路径规划完成")
                            return result

            logger.warning("无法找到路径")
            return {
                'success': False,
                'error': '无法找到路径'
            }

        except Exception as e:
            logger.error(f"DE-RRT*路径规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }


class DWAPlanner:
    """
    DWA路径规划器
    """

    def __init__(self, weather_data: Optional[Dict] = None,
                 obstacles: Optional[List[Obstacle]] = None):
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []

    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离
        """
        return np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)

    def is_collision(self, location: Tuple[float, float]) -> bool:
        """
        检查是否碰撞
        """
        for obstacle in self.obstacles:
            distance = self.calculate_distance(location, obstacle.location)
            if distance < obstacle.radius:
                return True
        return False

    def plan(self, current_pose: Tuple[float, float, float], goal: Tuple[float, float]) -> Dict:
        """
        执行DWA路径规划
        :param current_pose: 当前位置和朝向 (x, y, theta)
        :param goal: 目标位置
        """
        try:
            # 生成缓存键
            cache_key = str(current_pose) + str(goal) + \
                str([(o.location, o.radius) for o in self.obstacles])
            # 检查缓存
            cached_result = dwa_cache.get(cache_key)
            if cached_result:
                logger.info("使用缓存的DWA规划结果")
                return cached_result

            # 简化的DWA实现
            v_range = [1.0, 2.0, 3.0]  # 减少速度范围以提高速度
            w_range = [-0.5, 0.0, 0.5]  # 减少角速度范围以提高速度

            best_score = -float('inf')
            best_trajectory = []

            for v in v_range:
                for w in w_range:
                    # 预测轨迹
                    trajectory = []
                    x, y, theta = current_pose

                    dt = 0.1  # time step
                    for i in range(5):  # 减少预测步数以提高速度
                        x += v * np.cos(theta) * dt
                        y += v * np.sin(theta) * dt
                        theta += w * dt
                        trajectory.append((x, y))

                    # 计算轨迹评分
                    # 目标距离
                    goal_distance = self.calculate_distance(trajectory[-1], goal)
                    # 障碍物距离
                    min_obstacle_distance = float('inf')
                    for point in trajectory:
                        for obstacle in self.obstacles:
                            distance = self.calculate_distance(point, obstacle.location)
                            min_obstacle_distance = min(min_obstacle_distance, distance)
                    # 速度
                    speed_score = v

                    # 综合评分
                    if min_obstacle_distance == float('inf'):
                        min_obstacle_distance = 10.0  # default safe distance
                    score = -0.5 * goal_distance + 2.0 * min_obstacle_distance + 0.5 * speed_score

                    if score > best_score and not self.is_collision(trajectory[-1]):
                        best_score = score
                        best_trajectory = trajectory

            if best_trajectory:
                result = {
                    'success': True,
                    'trajectory': best_trajectory,
                    'score': best_score
                }

                # 缓存结果
                dwa_cache.set(cache_key, result)

                logger.info("DWA路径规划完成")
                return result
            else:
                logger.warning("无法找到轨迹")
                return {
                    'success': False,
                    'error': '无法找到轨迹'
                }

        except Exception as e:
            logger.error(f"DWA路径规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }


class ThreeLayerPlanner:
    """
    三层路径规划器

    支持气象风险感知功能：
    - 自动从同化结果计算风险场
    - 在路径规划中考虑气象风险
    - 支持风险规避和动态重规划
    """

    def __init__(
        self,
        drones: List[Drone],
        tasks: List[Task],
        weather_data: Optional[Dict] = None,
        obstacles: Optional[List[Obstacle]] = None,
        no_fly_zones: Optional[List[NoFlyZone]] = None,
        assimilation_result: Optional[Dict] = None,  # 新增：同化结果输入
        risk_weight: float = 0.35  # 新增：风险权重
    ):
        self.drones = drones
        self.tasks = tasks
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []
        self.no_fly_zones = no_fly_zones or []
        self.risk_weight = risk_weight

        # 初始化风险映射
        self.risk_mapper: Optional[WeatherToRiskMapper] = None
        self.risk_cost_calculator: Optional[RiskAwarePathCostCalculator] = None
        self.risk_result: Optional[Dict] = None

        if assimilation_result and RISK_MAPPER_AVAILABLE:
            self._init_risk_mapping(assimilation_result)

        self.vrptw = VRPTWPlanner(drones, tasks, weather_data)
        self.a_star = AStarPlanner(weather_data, obstacles, no_fly_zones)
        self.derrt_star = DERRTStarPlanner(weather_data, obstacles, no_fly_zones)
        self.dwa = DWAPlanner(weather_data, obstacles)

    def _init_risk_mapping(self, assimilation_result: Dict):
        """
        初始化风险映射

        Args:
            assimilation_result: 同化结果，格式如下：
                {
                    'variables': {
                        'u_wind': np.ndarray,
                        'v_wind': np.ndarray,
                        ...
                    },
                    'grid': {'shape': (rows, cols)}
                }
        """
        try:
            self.risk_mapper = WeatherToRiskMapper(grid_resolution=10.0)
            self.risk_result = self.risk_mapper.compute_comprehensive_risk(assimilation_result)

            self.risk_cost_calculator = RiskAwarePathCostCalculator(self.risk_mapper)
            self.risk_cost_calculator.set_risk_field(self.risk_result)

            logger.info(
                f"风险映射初始化完成: 平均风险={self.risk_result['summary']['avg_risk']:.3f}, "
                f"安全区域={self.risk_result['summary']['safe_area_ratio']:.1%}"
            )
        except Exception as e:
            logger.warning(f"风险映射初始化失败: {e}")
            self.risk_mapper = None
            self.risk_cost_calculator = None

    def calculate_comprehensive_cost(
        self,
        route: Dict,
        weather_data: Optional[Dict] = None
    ) -> float:
        """
        计算五维综合代价：距离 + 能耗 + 时间 + 气象风险 + 路径风险

        Args:
            route: 包含 total_distance, total_time, total_payload, path 的路径
            weather_data: 气象数据（风速等）

        Returns:
            综合代价（越小越好）
        """
        distance = route.get('total_distance', 0)
        time_cost = route.get('total_time', 0)
        payload = route.get('total_payload', 0)

        # 能耗：与距离和载重成正比
        energy = distance * (1 + payload / 10.0)

        # 基础气象风险
        wind_speed = 0
        if weather_data:
            wind_speed = weather_data.get('wind_speed', 0) or 0
        base_risk = distance * wind_speed / 100.0

        # ⭐ 新增：路径风险（基于风险映射）
        path_risk = 0.0
        if self.risk_cost_calculator and 'path' in route and route['path']:
            path = route['path']
            total_risk_cost = 0.0
            for i in range(len(path) - 1):
                segment_cost = self.risk_cost_calculator.compute_segment_risk_cost(
                    path[i], path[i + 1], steps=5
                )
                total_risk_cost += segment_cost
            # 归一化路径风险
            path_risk = total_risk_cost / max(len(path) - 1, 1)

        # 五维加权综合代价
        w_dist = 0.25
        w_energy = 0.15
        w_time = 0.20
        w_base_risk = 0.15
        w_path_risk = 0.25  # 路径风险权重

        return (w_dist * distance + w_energy * energy + w_time * time_cost +
                w_base_risk * base_risk + w_path_risk * path_risk)

    def plan(self) -> Dict:
        """
        执行完整路径规划（支持气象风险感知）

        如果初始化时提供了同化结果，会自动计算风险场并规避高风险区域
        """
        try:
            # 1. VRPTW任务调度
            vrptw_result = self.vrptw.plan()
            if not vrptw_result['success']:
                return vrptw_result

            # 2. DE-RRT*全局路径规划（并行处理）
            routes = vrptw_result['routes']

            def process_route(route):
                if route['tasks']:
                    # 从基地到第一个任务点
                    start = (0.0, 0.0)
                    route_path = []
                    total_path_risk = 0.0

                    for task_id in route['tasks']:
                        task = next(t for t in self.tasks if t.id == task_id)
                        goal = task.location

                        # ⭐ 新增：风险感知路径规划
                        planned = False
                        if self.risk_cost_calculator:
                            # 尝试风险感知A*规划
                            try:
                                from risk_aware_planner import RiskAwareAStarPlanner
                                risk_astar = RiskAwareAStarPlanner(
                                    risk_result=self.risk_result,
                                    risk_weight=self.risk_weight
                                )
                                astar_result = risk_astar.plan(start, goal)
                                if astar_result['success']:
                                    route_path.extend(astar_result['path'])
                                    total_path_risk += astar_result.get('avg_risk', 0)
                                    start = goal
                                    planned = True
                            except Exception as e:
                                logger.warning(f"风险感知规划失败，回退到标准规划: {e}")

                        if not planned:
                            # 使用DE-RRT*算法
                            derrt_result = self.derrt_star.plan(start, goal)
                            if derrt_result['success']:
                                route_path.extend(derrt_result['path'])
                                start = goal
                            else:
                                # 如果DE-RRT*失败，使用A*作为备选
                                astar_result = self.a_star.plan(start, goal)
                                if astar_result['success']:
                                    route_path.extend(astar_result['path'])
                                    start = goal

                    # 从最后一个任务点返回基地
                    planned = False
                    if self.risk_cost_calculator:
                        try:
                            from risk_aware_planner import RiskAwareAStarPlanner
                            risk_astar = RiskAwareAStarPlanner(
                                risk_result=self.risk_result,
                                risk_weight=self.risk_weight
                            )
                            astar_result = risk_astar.plan(start, (0.0, 0.0))
                            if astar_result['success']:
                                route_path.extend(astar_result['path'])
                                total_path_risk += astar_result.get('avg_risk', 0)
                                planned = True
                        except Exception:
                            pass

                    if not planned:
                        derrt_result = self.derrt_star.plan(start, (0.0, 0.0))
                        if derrt_result['success']:
                            route_path.extend(derrt_result['path'])
                        else:
                            # 如果DE-RRT*失败，使用A*作为备选
                            astar_result = self.a_star.plan(start, (0.0, 0.0))
                            if astar_result['success']:
                                route_path.extend(astar_result['path'])

                    route['path'] = route_path
                    route['path_risk'] = total_path_risk / max(len(route['tasks']), 1)

                    # 计算五维综合代价
                    if self.weather_data or self.risk_cost_calculator:
                        route['comprehensive_cost'] = self.calculate_comprehensive_cost(
                            route, self.weather_data)
                return route

            # 使用并行处理
            max_workers = min(10, len(routes))
            with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
                routes = list(executor.map(process_route, routes))

            # ⭐ 新增：返回风险摘要
            result = {
                'success': True,
                'routes': routes,
                'unassigned_tasks': vrptw_result['unassigned_tasks']
            }

            if self.risk_result:
                result['risk_summary'] = self.risk_result['summary']
                result['risk_weight_used'] = self.risk_weight

            logger.info("三层路径规划完成")
            return result

        except Exception as e:
            logger.error(f"三层路径规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }

    def dynamic_replan(
        self,
        current_route: Dict,
        new_weather_data: Optional[Dict] = None,
        new_obstacles: Optional[List[Obstacle]] = None,
        new_no_fly_zones: Optional[List[NoFlyZone]] = None
    ) -> Dict:
        """
        动态重规划
        :param current_route: 当前路径
        :param new_weather_data: 新的气象数据
        :param new_obstacles: 新的障碍物
        :param new_no_fly_zones: 新的禁飞区
        :return: 重规划结果
        """
        try:
            logger.info("开始动态重规划...")

            # 更新气象数据和障碍物
            if new_weather_data:
                self.weather_data = new_weather_data
                self.derrt_star.weather_data = new_weather_data
                self.dwa.weather_data = new_weather_data

            if new_obstacles:
                self.obstacles = new_obstacles
                self.derrt_star.obstacles = new_obstacles
                self.dwa.obstacles = new_obstacles

            if new_no_fly_zones:
                self.no_fly_zones = new_no_fly_zones
                self.derrt_star.no_fly_zones = new_no_fly_zones

            # 提取当前路径的任务点
            tasks = []
            for task_id in current_route.get('tasks', []):
                task = next((t for t in self.tasks if t.id == task_id), None)
                if task:
                    tasks.append(task)

            if not tasks:
                return {
                    'success': False,
                    'error': '当前路径没有任务点'
                }

            # 重新规划路径（并行处理）
            start = (0.0, 0.0)  # 假设从基地出发
            new_path = []

            def plan_segment_with_start(task, current_start):
                goal = task.location
                # 使用DE-RRT*算法
                derrt_result = self.derrt_star.plan(current_start, goal)
                if derrt_result['success']:
                    return (derrt_result['path'], goal)
                else:
                    # 如果DE-RRT*失败，使用A*作为备选
                    astar_result = self.a_star.plan(current_start, goal)
                    if astar_result['success']:
                        return (astar_result['path'], goal)
                    else:
                        return (None, current_start)

            # 顺序处理任务段（避免竞态条件）
            current_pos = start
            segments = []
            for task in tasks:
                segment, current_pos = plan_segment_with_start(task, current_pos)
                segments.append(segment)

            # 检查是否所有段都成功规划
            for segment in segments:
                if segment is None:
                    return {
                        'success': False,
                        'error': '无法规划到任务点的路径'
                    }
                new_path.extend(segment)

            # 从最后一个任务点返回基地
            derrt_result = self.derrt_star.plan(current_pos, (0.0, 0.0))
            if derrt_result['success']:
                new_path.extend(derrt_result['path'])
            else:
                # 如果DE-RRT*失败，使用A*作为备选
                astar_result = self.a_star.plan(current_pos, (0.0, 0.0))
                if astar_result['success']:
                    new_path.extend(astar_result['path'])
                else:
                    return {
                        'success': False,
                        'error': '无法规划返回基地的路径'
                    }

            # 更新路径
            new_route = current_route.copy()
            new_route['path'] = new_path

            logger.info("动态重规划完成")
            return {
                'success': True,
                'route': new_route
            }

        except Exception as e:
            logger.error(f"动态重规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }


def load_input(file_index):
    """从文件加载JSON输入数据，防止命令注入"""
    if len(sys.argv) <= file_index:
        return {}
    file_path = sys.argv[file_index]
    with open(file_path, 'r') as f:
        return json.load(f)


def main():
    """
    主函数
    """
    if len(sys.argv) < 2:
        logger.info(json.dumps({
            'success': False,
            'error': '缺少命令参数'
        }))
        return

    command = sys.argv[1]

    if command == 'vrptw':
        # VRPTW规划
        if len(sys.argv) < 3:
            logger.info(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return

        try:
            input_data = load_input(2)
            drones = [
                Drone(
                    d['id'], float(
                        d['max_payload']), float(
                        d['max_endurance']), float(
                        d['max_speed'])) for d in input_data.get(
                    'drones', [])]
            tasks = [
                Task(
                    t['id'], (float(
                        t['location'][0]), float(
                        t['location'][1])), float(
                        t['demand']), float(
                        t['start_time']), float(
                            t['end_time'])) for t in input_data.get(
                                'tasks', [])]
            weather_data = input_data.get('weather_data', {})

            vrptw = VRPTWPlanner(drones, tasks, weather_data)
            result = vrptw.plan()
            logger.info(json.dumps(result))

        except Exception as e:
            logger.info(json.dumps({
                'success': False,
                'error': str(e)
            }))

    elif command == 'astar':
        # A*规划
        if len(sys.argv) < 3:
            logger.info(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return

        try:
            input_data = load_input(2)
            start_data = input_data.get('start', (0, 0))
            start = (float(start_data[0]), float(start_data[1]))
            goal_data = input_data.get('goal', (10, 10))
            goal = (float(goal_data[0]), float(goal_data[1]))
            weather_data = input_data.get('weather_data', {})
            obstacles = [
                Obstacle(
                    (float(
                        o['location'][0]), float(
                        o['location'][1])), float(
                        o['radius'])) for o in input_data.get(
                    'obstacles', [])]
            no_fly_zones = [
                NoFlyZone(
                    (float(
                        n['location'][0]), float(
                        n['location'][1])), float(
                        n['radius'])) for n in input_data.get(
                    'no_fly_zones', [])]

            a_star = AStarPlanner(weather_data, obstacles, no_fly_zones)
            result = a_star.plan(start, goal)
            logger.info(json.dumps(result))

        except Exception as e:
            logger.info(json.dumps({
                'success': False,
                'error': str(e)
            }))

    elif command == 'dwa':
        # DWA规划
        if len(sys.argv) < 3:
            logger.info(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return

        try:
            input_data = load_input(2)
            pose_data = input_data.get('current_pose', (0, 0, 0))
            current_pose = (float(pose_data[0]), float(pose_data[1]), float(pose_data[2]))
            goal_data = input_data.get('goal', (10, 10))
            goal = (float(goal_data[0]), float(goal_data[1]))
            weather_data = input_data.get('weather_data', {})
            obstacles = [
                Obstacle(
                    (float(
                        o['location'][0]), float(
                        o['location'][1])), float(
                        o['radius'])) for o in input_data.get(
                    'obstacles', [])]

            dwa = DWAPlanner(weather_data, obstacles)
            result = dwa.plan(current_pose, goal)
            logger.info(json.dumps(result))

        except Exception as e:
            logger.info(json.dumps({
                'success': False,
                'error': str(e)
            }))

    elif command == 'full':
        # 完整路径规划
        if len(sys.argv) < 3:
            logger.info(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return

        try:
            input_data = load_input(2)
            drones = [
                Drone(
                    d['id'], float(
                        d['max_payload']), float(
                        d['max_endurance']), float(
                        d['max_speed'])) for d in input_data.get(
                    'drones', [])]
            tasks = [
                Task(
                    t['id'], (float(
                        t['location'][0]), float(
                        t['location'][1])), float(
                        t['demand']), float(
                        t['start_time']), float(
                            t['end_time'])) for t in input_data.get(
                                'tasks', [])]
            weather_data = input_data.get('weather_data', {})
            obstacles = [
                Obstacle(
                    (float(
                        o['location'][0]), float(
                        o['location'][1])), float(
                        o['radius'])) for o in input_data.get(
                    'obstacles', [])]
            no_fly_zones = [
                NoFlyZone(
                    (float(
                        n['location'][0]), float(
                        n['location'][1])), float(
                        n['radius'])) for n in input_data.get(
                    'no_fly_zones', [])]

            planner = ThreeLayerPlanner(drones, tasks, weather_data, obstacles, no_fly_zones)
            result = planner.plan()
            logger.info(json.dumps(result))

        except Exception as e:
            logger.info(json.dumps({
                'success': False,
                'error': str(e)
            }))

    elif command == 'derrt':
        # DE-RRT*路径规划
        if len(sys.argv) < 3:
            logger.info(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return

        try:
            input_data = load_input(2)
            start_data = input_data.get('start', (0, 0))
            start = (float(start_data[0]), float(start_data[1]))
            goal_data = input_data.get('goal', (10, 10))
            goal = (float(goal_data[0]), float(goal_data[1]))
            weather_data = input_data.get('weather_data', {})
            obstacles = [
                Obstacle(
                    (float(
                        o['location'][0]), float(
                        o['location'][1])), float(
                        o['radius'])) for o in input_data.get(
                    'obstacles', [])]
            no_fly_zones = [
                NoFlyZone(
                    (float(
                        n['location'][0]), float(
                        n['location'][1])), float(
                        n['radius'])) for n in input_data.get(
                    'no_fly_zones', [])]

            derrt_star = DERRTStarPlanner(weather_data, obstacles, no_fly_zones)
            result = derrt_star.plan(start, goal)
            logger.info(json.dumps(result))

        except Exception as e:
            logger.info(json.dumps({
                'success': False,
                'error': str(e)
            }))

    elif command == 'replan':
        # 动态重规划
        if len(sys.argv) < 3:
            logger.info(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return

        try:
            input_data = load_input(2)
            current_route = input_data.get('current_route', {})
            new_weather_data = input_data.get('new_weather_data', {})
            new_obstacles = [
                Obstacle(
                    (float(
                        o['location'][0]), float(
                        o['location'][1])), float(
                        o['radius'])) for o in input_data.get(
                    'new_obstacles', [])]
            new_no_fly_zones = [
                NoFlyZone(
                    (float(
                        n['location'][0]), float(
                        n['location'][1])), float(
                        n['radius'])) for n in input_data.get(
                    'new_no_fly_zones', [])]
            drones = [
                Drone(
                    d['id'], float(
                        d['max_payload']), float(
                        d['max_endurance']), float(
                        d['max_speed'])) for d in input_data.get(
                    'drones', [])]
            tasks = [
                Task(
                    t['id'], (float(
                        t['location'][0]), float(
                        t['location'][1])), float(
                        t['demand']), float(
                        t['start_time']), float(
                            t['end_time'])) for t in input_data.get(
                                'tasks', [])]

            planner = ThreeLayerPlanner(drones, tasks, new_weather_data,
                                        new_obstacles, new_no_fly_zones)
            result = planner.dynamic_replan(
                current_route,
                new_weather_data,
                new_obstacles,
                new_no_fly_zones)
            logger.info(json.dumps(result))

        except Exception as e:
            logger.info(json.dumps({
                'success': False,
                'error': str(e)
            }))

    else:
        logger.info(json.dumps({
            'success': False,
            'error': '未知命令'
        }))


if __name__ == "__main__":
    main()
