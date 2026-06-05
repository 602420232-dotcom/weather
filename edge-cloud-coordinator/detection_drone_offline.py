"""
探测无人机离线上传集成模块

场景：探测无人机在海上/山区/高空飞行，全程可能无网络。
采集数据全部走 OfflineCache 缓存 → 着陆后触发 processSyncQueue 自动上传。

数据流:
  飞行中 (离线): 传感器 → OfflineCache.cache_weather() → SQLite 本地存储
  着陆后 (网络恢复): NetworkMonitor 检测到网络 → processSyncQueue()
    → POST /api/detection/sample/upload → detection-drone-service

与现有组件的对接点:
  - C++ SDK: uav-edge-sdk (OfflineCache::cache_weather)
  - Flutter App: uav-mobile-app (OfflineManager + NetworkMonitor)
  - Java: detection-drone-service (DetectionDroneController.uploadSample)
"""

import time
import logging
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from collections import deque
import threading

logger = logging.getLogger(__name__)


@dataclass
class OfflineSample:
    """离线样本数据结构"""
    mission_id: int
    drone_id: str
    sequence_num: int = 0
    longitude: float = 0.0
    latitude: float = 0.0
    altitude: float = 0.0
    temperature: Optional[float] = None
    humidity: Optional[float] = None
    pressure: Optional[float] = None
    wind_speed: Optional[float] = None
    wind_direction: Optional[float] = None
    wind_gust: Optional[float] = None
    visibility: Optional[float] = None
    co2: Optional[float] = None
    pm25: Optional[float] = None
    quality_flag: float = 1.0
    sample_time: float = field(default_factory=time.time)

    def to_api_payload(self) -> dict:
        return {k: v for k, v in {
            "longitude": self.longitude,
            "latitude": self.latitude,
            "altitude": self.altitude,
            "temperature": self.temperature,
            "humidity": self.humidity,
            "pressure": self.pressure,
            "windSpeed": self.wind_speed,
            "windDirection": self.wind_direction,
            "windGust": self.wind_gust,
            "visibility": self.visibility,
            "co2": self.co2,
            "pm25": self.pm25,
            "qualityFlag": self.quality_flag,
            "sampleTime": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(self.sample_time))
        }.items() if v is not None}


class OfflineSampleBuffer:
    """离线样本缓冲区
    在飞行期间缓存所有采集样本，着陆后批量上传。
    """

    def __init__(self, mission_id: int, drone_id: str, max_buffer_size: int = 50000):
        self.mission_id = mission_id
        self.drone_id = drone_id
        self.buffer: deque = deque(maxlen=max_buffer_size)
        self.sequence_counter = 0
        self.lock = threading.Lock()
        self._upload_endpoint = "http://detection-drone:8092/api/detection/sample/upload"

    def add_sample(self, sample: OfflineSample) -> int:
        with self.lock:
            self.sequence_counter += 1
            sample.sequence_num = self.sequence_counter
            sample.mission_id = self.mission_id
            sample.drone_id = self.drone_id
            self.buffer.append(sample)
            return self.sequence_counter

    def add_from_sensor(self, sensor_data: dict):
        """从传感器原始数据创建样本并加入缓冲区"""
        sample = OfflineSample(
            mission_id=self.mission_id,
            drone_id=self.drone_id,
            longitude=sensor_data.get("longitude", 0),
            latitude=sensor_data.get("latitude", 0),
            altitude=sensor_data.get("altitude", 0),
            temperature=sensor_data.get("temperature"),
            humidity=sensor_data.get("humidity"),
            pressure=sensor_data.get("pressure"),
            wind_speed=sensor_data.get("wind_speed"),
            wind_direction=sensor_data.get("wind_direction"),
            wind_gust=sensor_data.get("wind_gust"),
            visibility=sensor_data.get("visibility"),
            co2=sensor_data.get("co2"),
            pm25=sensor_data.get("pm25"),
            quality_flag=sensor_data.get("data_quality", 1.0)
        )
        return self.add_sample(sample)

    def get_buffer_size(self) -> int:
        return len(self.buffer)

    def to_batch_payload(self, max_batch_size: int = 1000) -> List[dict]:
        """生成批量上传的 payload，分批次返回"""
        with self.lock:
            samples = list(self.buffer)
        batches = []
        for i in range(0, len(samples), max_batch_size):
            batch = samples[i:i + max_batch_size]
            batches.append({
                "missionId": self.mission_id,
                "droneId": self.drone_id,
                "fromOffline": True,
                "samples": [s.to_api_payload() for s in batch]
            })
        return batches

    def clear(self):
        with self.lock:
            self.buffer.clear()


class DetectionDroneOfflineUploader:
    """探测无人机离线上传管理器
    封装离线缓存 → 网络恢复 → 自动上传的完整链路。
    """

    def __init__(self, upload_endpoint: Optional[str] = None):
        self.buffers: Dict[str, OfflineSampleBuffer] = {}
        self.upload_endpoint = (
            upload_endpoint
            or "http://detection-drone:8092/api/detection/sample/upload"
        )
        self._online = False
        self._pending_upload = False
        self._upload_thread: Optional[threading.Thread] = None

    def start_mission(self, mission_id: int, drone_id: str) -> OfflineSampleBuffer:
        """开始一个新探测任务，创建离线缓冲区"""
        buffer = OfflineSampleBuffer(mission_id, drone_id)
        key = f"{drone_id}_{mission_id}"
        self.buffers[key] = buffer
        logger.info(f"探测任务开始: missionId={mission_id}, droneId={drone_id}")
        return buffer

    def end_mission(self, mission_id: int, drone_id: str):
        """结束任务，标记为待上传"""
        key = f"{drone_id}_{mission_id}"
        buffer = self.buffers.get(key)
        if buffer:
            sample_count = buffer.get_buffer_size()
            logger.info(f"探测任务结束: missionId={mission_id}, samples={sample_count}, 等待网络恢复上传")
            self._pending_upload = True

    def on_network_restored(self):
        """网络恢复回调 — 由 NetworkMonitor 触发"""
        logger.info("网络已恢复，开始上传离线数据...")
        self._online = True
        if self._pending_upload:
            self._upload_thread = threading.Thread(target=self._upload_all_pending, daemon=True)
            self._upload_thread.start()

    def _upload_all_pending(self):
        """批量上传所有待处理的离线数据"""
        try:
            import requests
        except ImportError:
            logger.error("requests 库未安装，无法执行离线上传")
            return

        for key, buffer in list(self.buffers.items()):
            if buffer.get_buffer_size() == 0:
                continue
            batches = buffer.to_batch_payload(max_batch_size=500)
            total_batches = len(batches)
            for i, batch in enumerate(batches):
                try:
                    resp = requests.post(self.upload_endpoint, json=batch, timeout=60)
                    if resp.status_code == 200:
                        logger.info(f"离线数据上传成功: {key}, batch={i + 1}/{total_batches}")
                    else:
                        logger.warning(f"离线数据上传失败: {key}, HTTP {resp.status_code}")
                        break  # 失败则停止，等下次重试
                except Exception as e:
                    logger.error(f"离线数据上传异常: {key}, {e}")
                    break
            else:
                # 全部成功，清除缓冲区
                buffer.clear()
                logger.info(f"离线数据全部上传完成: {key}")

        # 检查是否全部完成
        remaining = sum(b.get_buffer_size() for b in self.buffers.values())
        if remaining == 0:
            self._pending_upload = False
            logger.info("所有离线数据已上传")

    def get_pending_count(self) -> int:
        return sum(b.get_buffer_size() for b in self.buffers.values())

    def get_mission_summary(self) -> List[dict]:
        return [{
            "missionId": buf.mission_id,
            "droneId": buf.drone_id,
            "sampleCount": buf.get_buffer_size(),
            "pending": buf.get_buffer_size() > 0
        } for buf in self.buffers.values()]
