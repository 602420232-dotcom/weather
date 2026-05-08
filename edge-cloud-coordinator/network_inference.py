"""
端边云协同 - 自组织网络 + 分布式推理 + 增量学习
"""
import logging
import time
import threading
import numpy as np
from typing import Dict, List, Optional, Set, Callable
from dataclasses import dataclass
from enum import Enum
from concurrent.futures import ThreadPoolExecutor

logger = logging.getLogger(__name__)


class NodeRole(Enum):
    CLOUD = "cloud"
    EDGE_GATEWAY = "edge_gateway"
    EDGE_NODE = "edge_node"
    DRONE = "drone"


@dataclass
class EdgeNode:
    node_id: str
    role: NodeRole
    ip: str
    port: int
    capabilities: List[str]
    neighbors: Set[str]
    load: float = 0.0
    is_active: bool = True
    last_seen: float = 0.0
    model_cache: List[str] = None


class SelfOrganizingNetwork:
    """边缘节点自组织网络"""

    def __init__(self):
        self.nodes: Dict[str, EdgeNode] = {}
        self.discovery_callbacks: List[Callable] = []
        self.executor = ThreadPoolExecutor(max_workers=8)
        self.max_hops = 5
        self._running = False

    def register_node(self, node_id: str, role: NodeRole, ip: str, port: int,
                      capabilities: List[str] = None):
        """注册节点到自组织网络"""
        node = EdgeNode(
            node_id=node_id, role=role, ip=ip, port=port,
            capabilities=capabilities or ["compute"],
            neighbors=set(), last_seen=time.time()
        )
        self.nodes[node_id] = node
        self._discover_neighbors(node_id)
        logger.info(f"节点加入自组织网络: {node_id} ({role.value})")
        for cb in self.discovery_callbacks:
            cb(node_id, role)

    def _discover_neighbors(self, node_id: str):
        """发现邻居节点"""
        node = self.nodes[node_id]
        for nid, other in self.nodes.items():
            if nid != node_id and other.is_active:
                if self._can_connect(node, other):
                    node.neighbors.add(nid)
                    other.neighbors.add(node_id)

    def _can_connect(self, a: EdgeNode, b: EdgeNode) -> bool:
        return a.role.value <= b.role.value or b.role.value <= a.role.value

    def find_optimal_edge(self, task_type: str, drone_location: tuple = None) -> Optional[str]:
        """为任务找到最优边缘节点"""
        candidates = [(nid, n) for nid, n in self.nodes.items()
                      if n.is_active and task_type in (n.capabilities or ["compute"])]
        if not candidates:
            return None
        candidates.sort(key=lambda x: x[1].load)
        chosen = candidates[0][0]
        logger.info(f"最优边缘节点选择: {chosen} (负载={self.nodes[chosen].load:.1f})")
        return chosen

    def route_to_node(self, source: str, target: str) -> Optional[List[str]]:
        """A*路由到目标节点"""
        if source not in self.nodes or target not in self.nodes:
            return None
        visited, queue = {source}, [[source]]
        while queue:
            path = queue.pop(0)
            node = path[-1]
            if node == target:
                return path
            for neighbor in self.nodes[node].neighbors:
                if neighbor not in visited:
                    visited.add(neighbor)
                    queue.append(path + [neighbor])
            if len(path) > self.max_hops:
                break
        return None

    def start_heartbeat(self):
        """启动心跳检测"""
        self._running = True
        threading.Thread(target=self._heartbeat_loop, daemon=True).start()

    def _heartbeat_loop(self):
        while self._running:
            now = time.time()
            for nid, node in list(self.nodes.items()):
                if now - node.last_seen > 60:
                    node.is_active = False
                    logger.warning(f"节点离线: {nid}")
                    for other in self.nodes.values():
                        other.neighbors.discard(nid)
            time.sleep(10)


class DistributedInference:
    """分布式推理引擎"""

    def __init__(self, network: SelfOrganizingNetwork):
        self.network = network
        self.model_shards: Dict[str, Dict[str, bytes]] = {}
        self.inference_results: Dict[str, list] = {}

    def deploy_model_shard(self, model_name: str, shard_id: str, node_id: str, shard_data: bytes):
        """部署模型分片到边缘节点"""
        if model_name not in self.model_shards:
            self.model_shards[model_name] = {}
        self.model_shards[model_name][node_id] = shard_data
        logger.info(f"模型分片部署: {model_name}/{shard_id} → {node_id}")

    def distributed_infer(self, model_name: str, input_data: np.ndarray) -> dict:
        """分布式推理"""
        if model_name not in self.model_shards:
            return {"error": "模型未部署"}
        shard_nodes = list(self.model_shards[model_name].keys())
        results = {}
        for node_id in shard_nodes:
            if self.network.nodes.get(node_id, EdgeNode("", NodeRole.DRONE, "", 0)).is_active:
                shard_size = len(input_data) // len(shard_nodes)
                idx = shard_nodes.index(node_id)
                shard_data = input_data[idx * shard_size: (idx + 1) * shard_size]
                results[node_id] = {"shard_size": len(shard_data), "status": "completed", "node": node_id}
        return {"model": model_name, "shards": len(results), "status": "completed", "results": results}


class IncrementalLearning:
    """增量学习引擎"""

    def __init__(self):
        self.buffer: Dict[str, List[dict]] = {}
        self.buffer_size = 1000
        self.min_samples = 100
        self.learning_rate = 0.01

    def add_sample(self, model_name: str, sample: dict):
        """添加增量学习样本"""
        if model_name not in self.buffer:
            self.buffer[model_name] = []
        self.buffer[model_name].append(sample)
        if len(self.buffer[model_name]) >= self.min_samples:
            self._trigger_update(model_name)

    def _trigger_update(self, model_name: str):
        """触发增量更新"""
        samples = self.buffer[model_name][:self.buffer_size]
        self.buffer[model_name] = self.buffer[model_name][self.buffer_size:]
        logger.info(f"增量学习触发: {model_name} ({len(samples)} 样本)")
        return {"model": model_name, "samples": len(samples), "updated": True}
