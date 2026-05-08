"""
WebSocket 实时状态同步
边缘节点与云端实时双向通信
"""
import logging
import asyncio
import time
from typing import Dict, List, Optional, Callable
from dataclasses import dataclass
from enum import Enum

logger = logging.getLogger(__name__)


class MessageType(Enum):
    DRONE_STATUS = "drone_status"
    WEATHER_UPDATE = "weather_update"
    PATH_UPDATE = "path_update"
    TASK_ASSIGNMENT = "task_assignment"
    ALERT = "alert"
    MODEL_SYNC = "model_sync"
    HEARTBEAT = "heartbeat"


@dataclass
class WSMessage:
    msg_type: MessageType
    drone_id: str
    payload: dict
    timestamp: float
    msg_id: str = ""


class WebSocketSync:
    """WebSocket 实时状态同步"""

    def __init__(self):
        self.connections: Dict[str, object] = {}
        self.subscribers: Dict[MessageType, List[Callable]] = {}
        self.drone_states: Dict[str, dict] = {}
        self.message_buffer: List[WSMessage] = []
        self._handlers_registered = False

    def register_handler(self, msg_type: MessageType, handler: Callable):
        if msg_type not in self.subscribers:
            self.subscribers[msg_type] = []
        self.subscribers[msg_type].append(handler)

    async def connect(self, drone_id: str, ws_conn: object):
        """建立WebSocket连接"""
        self.connections[drone_id] = ws_conn
        self.drone_states[drone_id] = {'status': 'connected', 'last_seen': time.time()}
        logger.info(f"无人机已连接: {drone_id}")
        await self._send(MessageType.HEARTBEAT, drone_id, {'status': 'connected'})

    async def disconnect(self, drone_id: str):
        """断开连接"""
        self.connections.pop(drone_id, None)
        if drone_id in self.drone_states:
            self.drone_states[drone_id]['status'] = 'disconnected'
        logger.info(f"无人机已断开: {drone_id}")

    async def send_status(self, drone_id: str, status_data: dict):
        """发送实时状态"""
        msg = WSMessage(
            msg_type=MessageType.DRONE_STATUS,
            drone_id=drone_id,
            payload=status_data,
            timestamp=time.time()
        )
        self.drone_states[drone_id] = status_data
        await self._send(MessageType.DRONE_STATUS, drone_id, status_data)

    async def broadcast(self, msg_type: MessageType, data: dict):
        """广播消息到所有连接"""
        msg = WSMessage(msg_type=msg_type, drone_id='broadcast', payload=data, timestamp=time.time())
        self.message_buffer.append(msg)
        for cb in self.subscribers.get(msg_type, []):
            if asyncio.iscoroutinefunction(cb):
                await cb(data)
            else:
                cb(data)

    async def _send(self, msg_type: MessageType, drone_id: str, data: dict):
        for cb in self.subscribers.get(msg_type, []):
            try:
                if asyncio.iscoroutinefunction(cb):
                    await cb(data)
                else:
                    cb(data)
            except Exception as e:
                logger.error(f"WebSocket消息发送失败: {e}")

    def get_connected_drones(self) -> List[str]:
        return list(self.connections.keys())

    def get_drone_state(self, drone_id: str) -> Optional[dict]:
        return self.drone_states.get(drone_id)


class WebSocketServer:
    """WebSocket 服务端 - 模拟实现"""

    def __init__(self, host: str = "0.0.0.0", port: int = 8765):
        self.host = host
        self.port = port
        self.sync = WebSocketSync()
        self._server = None

    async def start(self):
        logger.info(f"WebSocket 服务启动: ws://{self.host}:{self.port}")
        self._running = True
        return self

    async def stop(self):
        self._running = False
        logger.info("WebSocket 服务停止")
