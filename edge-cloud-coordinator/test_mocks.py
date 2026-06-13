"""
测试专用的 Mock 实现
用于依赖注入的单元测试
支持 FastAPI 的 dependency_overrides 机制
"""

import asyncio
from typing import Dict, List, Optional
from dataclasses import dataclass


class MockEdgeCloudCoordinator:
    """Mock 协调器 - 用于测试"""

    def __init__(self, node_id: str = "mock_edge_001", sync_interval: float = 5.0):
        self.node_id = node_id
        self.sync_interval = sync_interval
        self.task_queue = []
        self.completed_tasks = []
        self.cloud_models: Dict[str, object] = {}
        self.local_models: Dict[str, object] = {}
        self.offline_buffer = []
        self._lock = asyncio.Lock()

        # 跟踪调用
        self.submitted_tasks = []
        self.processed_tasks = []
        self.cancelled_tasks = []

    async def submit_task(self, task):
        """Mock 提交任务"""
        async with self._lock:
            self.task_queue.append(task)
            self.submitted_tasks.append(task)
            return task.task_id

    def process_task(self, task):
        """Mock 处理任务"""
        self.processed_tasks.append(task)
        return {
            "node": "mock",
            "result": {"status": "processed", "task_id": task.task_id}
        }

    async def cancel_task(self, task_id: str) -> bool:
        """Mock 取消任务"""
        async with self._lock:
            for i, task in enumerate(self.task_queue):
                if task.task_id == task_id:
                    self.task_queue.pop(i)
                    self.cancelled_tasks.append(task_id)
                    return True
            return False

    async def sync_cloud_models(self):
        """Mock 同步云端模型"""
        pass

    async def upload_edge_data(self):
        """Mock 上传边缘数据"""
        pass


class MockFederatedLearning:
    """Mock 联邦学习 - 用于测试"""

    def __init__(
        self,
        aggregation_strategy: str = "fedavg",
        min_clients: int = 2
    ):
        self.strategy = aggregation_strategy
        self.min_clients = min_clients
        self.round_id = 0
        self.client_updates = []
        self.global_model = None
        self.round_history = []

        # 跟踪调用
        self.received_updates = []

    def receive_update(self, drone_id: str, weights: dict, n_samples: int, metrics: dict):
        """Mock 接收更新"""
        self.received_updates.append({
            "drone_id": drone_id,
            "weights": weights,
            "n_samples": n_samples,
            "metrics": metrics
        })

        # 模拟聚合
        if len(self.received_updates) >= self.min_clients:
            self.round_id += 1
            self.client_updates = self.received_updates.copy()
            self.received_updates = []
            return True
        return False

    def get_global_model(self):
        """Mock 获取全局模型"""
        return self.global_model


class MockWebSocketSync:
    """Mock WebSocket 同步 - 用于测试"""

    def __init__(self, node_id: str = "mock_coordinator"):
        self.node_id = node_id
        self.connections = {}

        # 跟踪调用
        self.connect_calls = []
        self.disconnect_calls = []
        self.message_handles = []

    async def connect(self, drone_id: str, websocket):
        """Mock 连接"""
        self.connections[drone_id] = websocket
        self.connect_calls.append(drone_id)

    async def disconnect(self, drone_id: str, reason: str = "unknown"):
        """Mock 断开连接"""
        if drone_id in self.connections:
            del self.connections[drone_id]
        self.disconnect_calls.append({"drone_id": drone_id, "reason": reason})

    async def handle_message(self, drone_id: str, data: dict):
        """Mock 处理消息"""
        self.message_handles.append({"drone_id": drone_id, "data": data})

    async def get_health_status(self):
        """Mock 健康状态"""
        return {
            "node_id": self.node_id,
            "active_connections": len(self.connections),
            "connections": list(self.connections.keys())
        }
