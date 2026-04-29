#!/usr/bin/env python3
"""
优化的路径规划服务
集成并行计算和缓存机制
"""

import numpy as np
import json
import sys
import os
import logging
import concurrent.futures
from typing import List, Dict, Optional, Tuple
from collections import defaultdict, deque

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

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
    def __init__(self, id: str, location: Tuple[float, float], demand: float, start_time: float, end_time: float):
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
    优化的VRPTW规划器
    """
    def __init__(self, drones: List[Drone], tasks: List[Task], weather_data: Optional[Dict] = None):
        self.drones = drones
        self.tasks = tasks
        self.weather_data = weather_data or {}
        self.distance_cache = {}
    
    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离，使用缓存
        """
        key = (loc1, loc2)
        if key not in self.distance_cache:
            self.distance_cache[key] = np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
        return self.distance_cache[key]
    
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
                current_time = 0
                
                while unassigned_tasks and drone.current_endurance > 0:
                    # 选择最近的任务
                    nearest_task = None
                    min_distance = float('inf')
                    
                    for task in unassigned_tasks:
                        distance = self.calculate_distance(current_location, task.location)
                        if distance < min_distance and drone.current_payload + task.demand <= drone.max_payload:
                            min_distance = distance
                            nearest_task = task
                    
                    if not nearest_task:
                        break
                    
                    # 计算飞行时间
                    flight_time = self.calculate_time(min_distance, drone.max_speed)
                    
                    # 检查时间窗和续航
                    if current_time + flight_time >= nearest_task.end_time:
                        break
                    
                    if flight_time > drone.current_endurance:
                        break
                    
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
            
            logger.info("VRPTW规划完成")
            return {
                'success': True,
                'routes': routes,
                'unassigned_tasks': [task.id for task in unassigned_tasks]
            }
            
        except Exception as e:
            logger.error(f"VRPTW规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }

class AStarPlanner:
    """
    优化的A*路径规划器
    """
    def __init__(self, weather_data: Optional[Dict] = None, obstacles: Optional[List[Obstacle]] = None, no_fly_zones: Optional[List[NoFlyZone]] = None):
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []
        self.no_fly_zones = no_fly_zones or []
        self.path_cache = {}
        self.distance_cache = {}
    
    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离，使用缓存
        """
        key = (loc1, loc2)
        if key not in self.distance_cache:
            self.distance_cache[key] = np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
        return self.distance_cache[key]
    
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
        # 检查缓存
        cache_key = (start, goal)
        if cache_key in self.path_cache:
            return self.path_cache[cache_key]
        
        try:
            # 优先队列实现
            open_set = [(self.calculate_distance(start, goal), 0, start, None)]
            came_from = {}
            g_score = {start: 0}
            f_score = {start: self.calculate_distance(start, goal)}
            
            while open_set:
                # 选择f_score最小的节点
                open_set.sort()
                _, current_g, current, prev = open_set.pop(0)
                
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
                    self.path_cache[cache_key] = result
                    
                    logger.info("A*路径规划完成")
                    return result
                
                # 生成邻居节点
                neighbors = [
                    (current[0] + 1, current[1]),
                    (current[0] - 1, current[1]),
                    (current[0], current[1] + 1),
                    (current[0], current[1] - 1),
                    (current[0] + 1, current[1] + 1),
                    (current[0] + 1, current[1] - 1),
                    (current[0] - 1, current[1] + 1),
                    (current[0] - 1, current[1] - 1)
                ]
                
                for neighbor in neighbors:
                    # 检查是否碰撞
                    if self.is_collision(neighbor):
                        continue
                    
                    # 计算g_score
                    tentative_g_score = g_score[current] + self.calculate_distance(current, neighbor)
                    
                    if neighbor not in g_score or tentative_g_score < g_score[neighbor]:
                        came_from[neighbor] = current
                        g_score[neighbor] = tentative_g_score
                        f_score[neighbor] = tentative_g_score + self.calculate_distance(neighbor, goal)
                        
                        # 检查是否已经在开放集合中
                        found = False
                        for i, (_, g, n, _) in enumerate(open_set):
                            if n == neighbor and g > tentative_g_score:
                                open_set[i] = (f_score[neighbor], tentative_g_score, neighbor, current)
                                found = True
                                break
                        
                        if not found:
                            open_set.append((f_score[neighbor], tentative_g_score, neighbor, current))
            
            logger.warning("无法找到路径")
            result = {
                'success': False,
                'error': '无法找到路径'
            }
            
            # 缓存结果
            self.path_cache[cache_key] = result
            
            return result
            
        except Exception as e:
            logger.error(f"A*路径规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }

class DWAPlanner:
    """
    优化的DWA路径规划器
    """
    def __init__(self, weather_data: Optional[Dict] = None, obstacles: Optional[List[Obstacle]] = None):
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []
        self.collision_cache = {}
    
    def calculate_distance(self, loc1: Tuple[float, float], loc2: Tuple[float, float]) -> float:
        """
        计算两点之间的距离
        """
        return np.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
    
    def is_collision(self, location: Tuple[float, float]) -> bool:
        """
        检查是否碰撞，使用缓存
        """
        if location in self.collision_cache:
            return self.collision_cache[location]
        
        for obstacle in self.obstacles:
            distance = self.calculate_distance(location, obstacle.location)
            if distance < obstacle.radius:
                self.collision_cache[location] = True
                return True
        
        self.collision_cache[location] = False
        return False
    
    def plan(self, current_pose: Tuple[float, float, float], goal: Tuple[float, float]) -> Dict:
        """
        执行DWA路径规划
        :param current_pose: 当前位置和朝向 (x, y, theta)
        :param goal: 目标位置
        """
        try:
            # 简化的DWA实现
            v_range = [0, 1, 2, 3]
            w_range = [-0.5, -0.25, 0, 0.25, 0.5]
            
            best_score = -float('inf')
            best_trajectory = []
            
            # 并行计算轨迹评分
            def evaluate_trajectory(v, w):
                # 预测轨迹
                trajectory = []
                x, y, theta = current_pose
                
                for i in range(10):
                    x += v * np.cos(theta)
                    y += v * np.sin(theta)
                    theta += w
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
                score = -0.5 * goal_distance + 2.0 * min_obstacle_distance + 0.5 * speed_score
                
                return score, trajectory
            
            # 使用线程池并行计算
            with concurrent.futures.ThreadPoolExecutor() as executor:
                futures = []
                for v in v_range:
                    for w in w_range:
                        futures.append(executor.submit(evaluate_trajectory, v, w))
                
                for future in concurrent.futures.as_completed(futures):
                    score, trajectory = future.result()
                    if score > best_score and not self.is_collision(trajectory[-1]):
                        best_score = score
                        best_trajectory = trajectory
            
            if best_trajectory:
                logger.info("DWA路径规划完成")
                return {
                    'success': True,
                    'trajectory': best_trajectory,
                    'score': best_score
                }
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

class OptimizedThreeLayerPlanner:
    """
    优化的三层路径规划器
    """
    def __init__(self, drones: List[Drone], tasks: List[Task], weather_data: Optional[Dict] = None, obstacles: Optional[List[Obstacle]] = None, no_fly_zones: Optional[List[NoFlyZone]] = None):
        self.drones = drones
        self.tasks = tasks
        self.weather_data = weather_data or {}
        self.obstacles = obstacles or []
        self.no_fly_zones = no_fly_zones or []
        self.vrptw = VRPTWPlanner(drones, tasks, weather_data)
        self.a_star = AStarPlanner(weather_data, obstacles, no_fly_zones)
        self.dwa = DWAPlanner(weather_data, obstacles)
    
    def plan(self) -> Dict:
        """
        执行完整路径规划
        """
        try:
            # 1. VRPTW任务调度
            vrptw_result = self.vrptw.plan()
            if not vrptw_result['success']:
                return vrptw_result
            
            # 2. A*全局路径规划（并行）
            routes = vrptw_result['routes']
            
            def process_route(route):
                if route['tasks']:
                    # 从基地到第一个任务点
                    start = (0, 0)
                    path = []
                    for task_id in route['tasks']:
                        task = next(t for t in self.tasks if t.id == task_id)
                        goal = task.location
                        astar_result = self.a_star.plan(start, goal)
                        if astar_result['success']:
                            path.extend(astar_result['path'])
                            start = goal
                    # 从最后一个任务点返回基地
                    astar_result = self.a_star.plan(start, (0, 0))
                    if astar_result['success']:
                        path.extend(astar_result['path'])
                    route['path'] = path
                return route
            
            # 并行处理路径
            with concurrent.futures.ThreadPoolExecutor() as executor:
                processed_routes = list(executor.map(process_route, routes))
            
            logger.info("优化的三层路径规划完成")
            return {
                'success': True,
                'routes': processed_routes,
                'unassigned_tasks': vrptw_result['unassigned_tasks']
            }
            
        except Exception as e:
            logger.error(f"优化的三层路径规划失败: {e}")
            return {
                'success': False,
                'error': str(e)
            }

def main():
    """
    主函数
    """
    if len(sys.argv) < 2:
        print(json.dumps({
            'success': False,
            'error': '缺少命令参数'
        }))
        return
    
    command = sys.argv[1]
    
    if command == 'vrptw':
        # VRPTW规划
        if len(sys.argv) < 3:
            print(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return
        
        try:
            input_data = json.loads(sys.argv[2])
            drones = [Drone(d['id'], d['max_payload'], d['max_endurance'], d['max_speed']) for d in input_data.get('drones', [])]
            tasks = [Task(t['id'], tuple(t['location']), t['demand'], t['start_time'], t['end_time']) for t in input_data.get('tasks', [])]
            weather_data = input_data.get('weather_data', {})
            
            vrptw = VRPTWPlanner(drones, tasks, weather_data)
            result = vrptw.plan()
            print(json.dumps(result))
            
        except Exception as e:
            print(json.dumps({
                'success': False,
                'error': str(e)
            }))
            
    elif command == 'astar':
        # A*规划
        if len(sys.argv) < 3:
            print(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return
        
        try:
            input_data = json.loads(sys.argv[2])
            start = tuple(input_data.get('start', (0, 0)))
            goal = tuple(input_data.get('goal', (10, 10)))
            weather_data = input_data.get('weather_data', {})
            obstacles = [Obstacle(tuple(o['location']), o['radius']) for o in input_data.get('obstacles', [])]
            no_fly_zones = [NoFlyZone(tuple(n['location']), n['radius']) for n in input_data.get('no_fly_zones', [])]
            
            a_star = AStarPlanner(weather_data, obstacles, no_fly_zones)
            result = a_star.plan(start, goal)
            print(json.dumps(result))
            
        except Exception as e:
            print(json.dumps({
                'success': False,
                'error': str(e)
            }))
            
    elif command == 'dwa':
        # DWA规划
        if len(sys.argv) < 3:
            print(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return
        
        try:
            input_data = json.loads(sys.argv[2])
            current_pose = tuple(input_data.get('current_pose', (0, 0, 0)))
            goal = tuple(input_data.get('goal', (10, 10)))
            weather_data = input_data.get('weather_data', {})
            obstacles = [Obstacle(tuple(o['location']), o['radius']) for o in input_data.get('obstacles', [])]
            
            dwa = DWAPlanner(weather_data, obstacles)
            result = dwa.plan(current_pose, goal)
            print(json.dumps(result))
            
        except Exception as e:
            print(json.dumps({
                'success': False,
                'error': str(e)
            }))
            
    elif command == 'full':
        # 完整路径规划
        if len(sys.argv) < 3:
            print(json.dumps({
                'success': False,
                'error': '缺少输入数据'
            }))
            return
        
        try:
            input_data = json.loads(sys.argv[2])
            drones = [Drone(d['id'], d['max_payload'], d['max_endurance'], d['max_speed']) for d in input_data.get('drones', [])]
            tasks = [Task(t['id'], tuple(t['location']), t['demand'], t['start_time'], t['end_time']) for t in input_data.get('tasks', [])]
            weather_data = input_data.get('weather_data', {})
            obstacles = [Obstacle(tuple(o['location']), o['radius']) for o in input_data.get('obstacles', [])]
            no_fly_zones = [NoFlyZone(tuple(n['location']), n['radius']) for n in input_data.get('no_fly_zones', [])]
            
            planner = OptimizedThreeLayerPlanner(drones, tasks, weather_data, obstacles, no_fly_zones)
            result = planner.plan()
            print(json.dumps(result))
            
        except Exception as e:
            print(json.dumps({
                'success': False,
                'error': str(e)
            }))
            
    else:
        print(json.dumps({
            'success': False,
            'error': '未知命令'
        }))

if __name__ == "__main__":
    main()
