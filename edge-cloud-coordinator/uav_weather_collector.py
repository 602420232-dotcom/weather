"""
无人机气象信息收集模块
对接多种气象数据源，为路径规划提供实时气象数据
"""
import logging
import time
from typing import Dict, List, Optional, Callable
from dataclasses import dataclass, field
from enum import Enum
from collections import deque

try:
    import requests
except ImportError:
    requests = None

logger = logging.getLogger(__name__)


class WeatherSource(Enum):
    WRF = "wrf"
    UAV_SENSOR = "uav_sensor"
    GROUND_STATION = "ground_station"
    SATELLITE = "satellite"
    BUOY = "buoy"
    GHR = "ghr"
    RADIOSONDE = "radiosonde"


@dataclass
class WeatherRecord:
    source: WeatherSource
    drone_id: str
    timestamp: float
    latitude: float
    longitude: float
    altitude: float
    temperature: float
    humidity: float
    wind_speed: float
    wind_direction: float
    wind_gust: float
    pressure: float
    visibility: float
    turbulence: float
    precipitation: float
    data_quality: float = 1.0


class UAVWeatherCollector:
    """无人机气象数据收集器"""

    def __init__(self, drone_id: str):
        self.drone_id = drone_id
        self.buffer = deque(maxlen=10000)
        self.wrf_cache = {}
        self.sensor_cache = {}
        self.collection_interval = 1.0
        self._running = False
        self.callbacks: List[Callable] = []

    def on_data(self, callback: Callable):
        self.callbacks.append(callback)

    def collect_from_sensor(self, sensor_data: dict):
        """从机载传感器收集气象数据"""
        record = WeatherRecord(
            source=WeatherSource.UAV_SENSOR,
            drone_id=self.drone_id,
            timestamp=time.time(),
            latitude=sensor_data.get("latitude", 0),
            longitude=sensor_data.get("longitude", 0),
            altitude=sensor_data.get("altitude", 0),
            temperature=sensor_data.get("temperature", 25),
            humidity=sensor_data.get("humidity", 60),
            wind_speed=sensor_data.get("wind_speed", 0),
            wind_direction=sensor_data.get("wind_direction", 0),
            wind_gust=sensor_data.get("wind_gust", 0),
            pressure=sensor_data.get("pressure", 1013),
            visibility=sensor_data.get("visibility", 10),
            turbulence=sensor_data.get("turbulence", 0),
            precipitation=sensor_data.get("precipitation", 0),
            data_quality=0.9
        )
        self.buffer.append(record)
        self._notify(record)
        return record

    def collect_from_wrf(self, wrf_data: dict):
        """从WRF模型获取预报数据"""
        record = WeatherRecord(
            source=WeatherSource.WRF,
            drone_id=self.drone_id,
            timestamp=time.time(),
            latitude=wrf_data.get("latitude", 0),
            longitude=wrf_data.get("longitude", 0),
            altitude=wrf_data.get("altitude", 0),
            temperature=wrf_data.get("temperature", 25),
            humidity=wrf_data.get("humidity", 60),
            wind_speed=wrf_data.get("wind_speed", 0),
            wind_direction=wrf_data.get("wind_direction", 0),
            wind_gust=wrf_data.get("wind_gust", 0),
            pressure=wrf_data.get("pressure", 1013),
            visibility=wrf_data.get("visibility", 10),
            turbulence=wrf_data.get("turbulence", 0),
            precipitation=wrf_data.get("precipitation", 0),
            data_quality=0.8
        )
        self.wrf_cache[wrf_data.get("grid_point", "default")] = record
        self.buffer.append(record)
        self._notify(record)
        return record

    def _notify(self, record: WeatherRecord):
        for cb in self.callbacks:
            cb(record)

    def get_current_weather(self) -> Optional[dict]:
        if not self.buffer:
            return None
        latest = self.buffer[-1]
        return {
            "drone_id": self.drone_id,
            "temperature": latest.temperature,
            "humidity": latest.humidity,
            "wind_speed": latest.wind_speed,
            "wind_direction": latest.wind_direction,
            "wind_gust": latest.wind_gust,
            "pressure": latest.pressure,
            "visibility": latest.visibility,
            "turbulence": latest.turbulence,
            "precipitation": latest.precipitation,
            "timestamp": latest.timestamp
        }

    def get_weather_history(self, minutes: int = 10) -> List[dict]:
        cutoff = time.time() - minutes * 60
        return [self._to_dict(r) for r in self.buffer if r.timestamp > cutoff]

    def _to_dict(self, r: WeatherRecord) -> dict:
        return {
            "source": r.source.value, "drone_id": r.drone_id,
            "timestamp": r.timestamp, "temperature": r.temperature,
            "wind_speed": r.wind_speed, "humidity": r.humidity,
            "data_quality": r.data_quality
        }


class MultiSourceFusion:
    """多源气象数据融合"""

    def __init__(self):
        self.collectors: Dict[str, UAVWeatherCollector] = {}

    def register_drone(self, drone_id: str) -> UAVWeatherCollector:
        collector = UAVWeatherCollector(drone_id)
        self.collectors[drone_id] = collector
        return collector

    def fused_weather(self, drone_id: str) -> Optional[dict]:
        if drone_id not in self.collectors:
            return None
        collector = self.collectors[drone_id]
        sensor = None
        wrf = None
        for r in reversed(list(collector.buffer)):
            if r.source == WeatherSource.UAV_SENSOR and not sensor:
                sensor = r
            if r.source == WeatherSource.WRF and not wrf:
                wrf = r
            if sensor and wrf:
                break
        if not sensor:
            return None
        return {
            "temperature": sensor.temperature,
            "humidity": sensor.humidity,
            "wind_speed": sensor.wind_speed * 0.7
            + (wrf.wind_speed if wrf else sensor.wind_speed) * 0.3,
            "wind_direction": sensor.wind_direction,
            "source_fusion": "sensor+wrf" if wrf else "sensor_only",
            "drone_id": drone_id,
            "timestamp": time.time()
        }


class WeatherAlertEngine:
    """气象告警引擎"""

    def __init__(self):
        self.thresholds = {
            "wind_speed_max": 12.0,
            "wind_gust_max": 18.0,
            "visibility_min": 2.0,
            "turbulence_max": 0.8,
            "precipitation_max": 10.0
        }
        self.alerts: List[dict] = []

    def evaluate(self, weather: dict) -> Optional[dict]:
        warnings = []
        if weather.get("wind_speed", 0) > self.thresholds["wind_speed_max"]:
            warnings.append(f"风速告警: {weather['wind_speed']}m/s")
        if weather.get("wind_gust", 0) > self.thresholds["wind_gust_max"]:
            warnings.append(f"阵风告警: {weather['wind_gust']}m/s")
        if weather.get("visibility", 10) < self.thresholds["visibility_min"]:
            warnings.append(f"能见度过低: {weather['visibility']}km")
        if warnings:
            alert = {
                "drone_id": weather.get("drone_id"),
                "timestamp": time.time(),
                "warnings": warnings,
                "level": "HIGH" if len(warnings) >= 2 else "MEDIUM"
            }
            self.alerts.append(alert)
            return alert
        return None


@dataclass
class SoundingLevel:
    """探空单层数据"""
    pressure: float       # hPa
    height: float         # 位势高度 m
    temperature: float    # 摄氏度
    dewpoint: float       # 露点温度 摄氏度
    wind_speed: float     # m/s
    wind_direction: float  # 度
    humidity: float       # %


@dataclass
class SoundingProfile:
    """一次完整的探空垂直廓线"""
    station_id: str
    station_name: str
    longitude: float
    latitude: float
    altitude: float
    launch_time: float    # unix timestamp
    levels: List[SoundingLevel] = field(default_factory=list)
    data_source: str = "IGRA"


class RadiosondeCollector:
    """探空气球数据采集器
    支持从公开数据源（IGRA/UWYO/CMA）拉取，也支持本地上传。
    """

    # IGRA v2 数据URL模板
    IGRA_BASE_URL = "https://www1.ncdc.noaa.gov/pub/data/igra/data/data-por"

    # University of Wyoming 探空数据URL
    UWYO_BASE_URL = "http://weather.uwyo.edu/cgi-bin/sounding"

    def __init__(self, upload_endpoint: Optional[str] = None):
        self.profiles: Dict[str, List[SoundingProfile]] = {}
        self.upload_endpoint = (
            upload_endpoint
            or "http://radiosonde-weather:8091/api/radiosonde/upload"
        )

    def fetch_from_uwyo(self, station_id: str, year: int, month: int,
                        day: int, hour: int = 0) -> Optional[SoundingProfile]:
        """
        从 University of Wyoming 拉取探空数据
        示例: fetch_from_uwyo("50527", 2024, 7, 15, 0) -> 海拉尔站2024-07-15 00Z探空
        """
        if requests is None:
            logger.error("requests库未安装，无法拉取探空数据")
            return None
        params = {
            "region": "seasia",
            "TYPE": "TEXT:LIST",
            "YEAR": year,
            "MONTH": str(month).zfill(2),
            "FROM": f"{str(day).zfill(2)}{str(hour).zfill(2)}",
            "TO": f"{str(day).zfill(2)}{str(hour).zfill(2)}",
            "STNM": station_id
        }
        try:
            resp = requests.get(self.UWYO_BASE_URL, params=params, timeout=30)
            if resp.status_code != 200 or "Can't get" in resp.text:
                logger.warning(f"UWYO探空数据不可用: {station_id} {year}-{month:02d}-{day:02d}")
                return None
            return self._parse_uwyo_text(resp.text, station_id)
        except Exception as e:
            logger.error(f"拉取UWYO探空数据失败: {e}")
            return None

    def _parse_uwyo_text(self, text: str, station_id: str) -> Optional[SoundingProfile]:
        """解析 UWYO 纯文本格式探空数据"""
        lines = text.strip().split("\n")
        station_name = station_id
        lon = lat = alt = 0.0
        levels = []
        in_data = False
        for line in lines:
            line = line.strip()
            if line.startswith("Station identifier:") or line.startswith("Observations at"):
                in_data = False
                continue
            if line.startswith("Station number:"):
                station_name = line.split(":")[1].strip() if ":" in line else station_id
                continue
            if line.startswith("Station latitude:") and ":" in line:
                lat = float(line.split(":")[1].strip().split()[0])
                continue
            if line.startswith("Station longitude:") and ":" in line:
                lon = float(line.split(":")[1].strip().split()[0])
                continue
            if line.startswith("Station elevation:") and ":" in line:
                alt = float(line.split(":")[1].strip().split()[0])
                continue
            if line.startswith(
                "-----------------------------------------------------------------------------"
            ):
                in_data = True
                continue
            if not in_data or not line or line.startswith("Station"):
                continue
            parts = line.split()
            if len(parts) < 8:
                continue
            try:
                pres = float(parts[0])
                hght = float(parts[1])
                temp = float(parts[2])
                dwpt = float(parts[3])
                # parts[4]=RH, parts[5]=MIXR, parts[6]=DRCT, parts[7]=SKNT
                rh = float(parts[4]) if len(parts) > 4 else 0
                drct = float(parts[6]) if len(parts) > 6 else 0
                sknt = float(parts[7]) if len(parts) > 7 else 0
                wdir = drct
                wspd = sknt * 0.514444  # knots -> m/s
                levels.append(SoundingLevel(
                    pressure=pres, height=hght, temperature=temp,
                    dewpoint=dwpt, wind_speed=wspd, wind_direction=wdir, humidity=rh
                ))
            except (ValueError, IndexError):
                continue
        if not levels:
            return None
        return SoundingProfile(
            station_id=station_id, station_name=station_name,
            longitude=lon, latitude=lat, altitude=alt,
            launch_time=time.time(), levels=levels, data_source="UWYO"
        )

    def upload_profile_to_service(self, profile: SoundingProfile) -> bool:
        """将探空廓线上传到 radiosonde-weather-service"""
        if requests is None:
            logger.error("requests库未安装，无法上传探空数据")
            return False
        payload = {
            "stationId": profile.station_id,
            "stationName": profile.station_name,
            "longitude": profile.longitude,
            "latitude": profile.latitude,
            "stationAltitude": profile.altitude,
            "launchTime": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime(profile.launch_time)),
            "dataSource": profile.data_source,
            "levels": []
        }
        for lvl in profile.levels:
            payload["levels"].append({
                "pressureLevel": int(lvl.pressure),
                "geopotentialHeight": lvl.height,
                "temperature": lvl.temperature,
                "dewPoint": lvl.dewpoint,
                "relativeHumidity": lvl.humidity,
                "windSpeed": lvl.wind_speed,
                "windDirection": lvl.wind_direction
            })
        try:
            resp = requests.post(self.upload_endpoint, json=payload, timeout=30)
            if resp.status_code == 200:
                logger.info(f"探空数据上传成功: station={profile.station_id}, levels={len(profile.levels)}")
                return True
            else:
                logger.warning(f"探空数据上传失败: HTTP {resp.status_code}")
                return False
        except Exception as e:
            logger.error(f"探空数据上传异常: {e}")
            return False

    def fetch_and_upload(self, station_id: str, year: int, month: int,
                         day: int, hour: int = 0) -> bool:
        """拉取探空数据并自动上传到服务"""
        profile = self.fetch_from_uwyo(station_id, year, month, day, hour)
        if profile is None:
            return False
        self.profiles.setdefault(station_id, []).append(profile)
        return self.upload_profile_to_service(profile)

    def get_cached_profile(self, station_id: str) -> Optional[SoundingProfile]:
        """获取本地缓存的最近一次探空"""
        profiles = self.profiles.get(station_id)
        return profiles[-1] if profiles else None
