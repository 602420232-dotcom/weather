#!/usr/bin/env python3
"""
风险映射模块 - 边缘优化版本
专为 Jetson Orin 优化，支持≤150ms延迟

使用方式:
    python risk_mapper_edge.py --host 0.0.0.0 --port 50051
"""

import numpy as np
import socket
import json
import argparse
import time
from threading import Thread
from collections import deque


# 风险等级常量
class RiskLevel:
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    EXTREME = "EXTREME"


class WeatherToRiskMapperEdge:
    """边缘优化版风险映射器"""

    def __init__(self, grid_resolution=100.0):
        self.grid_resolution = grid_resolution
        self.wind_thresholds = {
            RiskLevel.LOW: 5.0,
            RiskLevel.MEDIUM: 10.0,
            RiskLevel.HIGH: 15.0,
            RiskLevel.EXTREME: 20.0
        }

    def compute_wind_speed(self, u_wind, v_wind):
        """快速计算风速（使用numpy向量化）"""
        return np.sqrt(u_wind ** 2 + v_wind ** 2)

    def compute_risk_grid(self, u_wind, v_wind):
        """
        快速计算风险栅格
        输入: u_wind, v_wind - numpy数组
        输出: 风险栅格 (0-1)
        """
        # 使用FP16量化加速
        u_wind = np.asarray(u_wind, dtype=np.float16)
        v_wind = np.asarray(v_wind, dtype=np.float16)

        # 计算风速
        wind_speed = self.compute_wind_speed(u_wind, v_wind)

        # 风切变估算（简化版）
        du_dx = np.abs(np.diff(u_wind, axis=1, append=0))
        dv_dy = np.abs(np.diff(v_wind, axis=0, append=0))
        shear = (du_dx + dv_dy) / 2

        # 综合风险 = 0.6*风速风险 + 0.4*湍流风险
        wind_risk = np.clip(wind_speed / 20.0, 0, 1)
        turbulence_risk = np.clip(shear / 10.0, 0, 1)

        risk_grid = 0.6 * wind_risk + 0.4 * turbulence_risk
        return np.clip(risk_grid, 0, 1).astype(np.float32)

    def compute_summary(self, risk_grid):
        """计算风险摘要（快速版）"""
        return {
            'avg_risk': float(np.mean(risk_grid)),
            'max_risk': float(np.max(risk_grid)),
            'min_risk': float(np.min(risk_grid)),
            'high_risk_ratio': float(np.sum(risk_grid >= 0.6) / risk_grid.size)
        }

    def process_chunk(self, u_chunk, v_chunk):
        """
        处理数据块（流式处理支持）
        适用于实时数据流场景
        """
        risk = self.compute_risk_grid(u_chunk, v_chunk)
        summary = self.compute_summary(risk)
        return {
            'risk_grid': risk,
            'summary': summary
        }


class RiskService:
    """风险映射边缘服务"""

    def __init__(self, host='0.0.0.0', port=50051):
        self.host = host
        self.port = port
        self.mapper = WeatherToRiskMapperEdge()
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        # 性能监控
        self.latency_history = deque(maxlen=100)
        self.request_count = 0
        self.start_time = time.time()

    def handle_request(self, data):
        """处理单个请求"""
        request_start = time.time()

        try:
            request = json.loads(data.decode('utf-8'))

            # 提取数据
            u_wind = request.get('u_wind', [])
            v_wind = request.get('v_wind', [])

            if not u_wind or not v_wind:
                return json.dumps({
                    'success': False,
                    'error': '缺少风场数据'
                }).encode('utf-8')

            # 快速处理
            result = self.mapper.process_chunk(u_wind, v_wind)

            # 转换为可序列化格式
            response = {
                'success': True,
                'summary': result['summary'],
                'risk_shape': list(result['risk_grid'].shape),
                'risk_flat': result['risk_grid'].flatten().tolist(),
                'processing_time_ms': (time.time() - request_start) * 1000
            }

            # 更新监控
            self.latency_history.append((time.time() - request_start) * 1000)
            self.request_count += 1

            return json.dumps(response).encode('utf-8')

        except Exception as e:
            return json.dumps({
                'success': False,
                'error': str(e),
                'processing_time_ms': (time.time() - request_start) * 1000
            }).encode('utf-8')

    def handle_client(self, conn, addr):
        """处理客户端连接"""
        try:
            data = conn.recv(65536)
            if not data:
                return

            response = self.handle_request(data)
            conn.sendall(response)
        except Exception as e:
            print(f"客户端处理错误 {addr}: {e}")
        finally:
            conn.close()

    def health_check(self):
        """健康检查"""
        return json.dumps({
            'status': 'healthy',
            'version': '1.0.0',
            'uptime': time.time() - self.start_time,
            'request_count': self.request_count,
            'avg_latency_ms': np.mean(self.latency_history) if self.latency_history else 0,
            'max_latency_ms': max(self.latency_history) if self.latency_history else 0
        }).encode('utf-8')

    def run(self):
        """启动服务"""
        self.socket.bind((self.host, self.port))
        self.socket.listen(5)
        print(f"边缘风险服务启动: {self.host}:{self.port}")
        print(f"服务启动时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")

        while True:
            conn, addr = self.socket.accept()

            # 检查是否为健康检查请求
            try:
                data = conn.recv(1024, socket.MSG_PEEK)
                if b'health' in data.lower():
                    conn.sendall(self.health_check())
                    conn.close()
                    continue
            except Exception:
                pass

            # 处理正常请求
            Thread(target=self.handle_client, args=(conn, addr)).start()


def demo():
    """演示边缘风险映射"""
    print("=" * 60)
    print("边缘风险映射模块 - 性能测试")
    print("=" * 60)

    mapper = WeatherToRiskMapperEdge()

    # 测试不同规模数据
    test_sizes = [(10, 10), (30, 30), (50, 50), (100, 100)]

    for rows, cols in test_sizes:
        print(f"\n测试: {rows}x{cols} 栅格 ({rows*cols} 点)")

        # 生成测试数据
        np.random.seed(42)
        u_wind = 8 + 4 * np.random.rand(rows, cols)
        v_wind = 5 + 3 * np.random.rand(rows, cols)

        # 测量时间
        start = time.time()
        for _ in range(10):
            result = mapper.process_chunk(u_wind, v_wind)
        avg_time = (time.time() - start) / 10

        print(f"  平均处理时间: {avg_time * 1000:.1f} ms")
        print(f"  平均风险: {result['summary']['avg_risk']:.3f}")
        print(f"  最大风险: {result['summary']['max_risk']:.3f}")

        # 检查延迟要求
        if avg_time * 1000 <= 150:
            print("  ✓ 满足延迟要求 (<=150ms)")
        else:
            print("  ✗ 未满足延迟要求")

    print("\n" + "=" * 60)
    print("测试完成!")


def main():
    parser = argparse.ArgumentParser(description='边缘风险映射服务')
    parser.add_argument('--host', default='0.0.0.0', help='服务地址')
    parser.add_argument('--port', type=int, default=50051, help='服务端口')
    parser.add_argument('--demo', action='store_true', help='运行性能测试')

    args = parser.parse_args()

    if args.demo:
        demo()
    else:
        service = RiskService(host=args.host, port=args.port)
        service.run()


if __name__ == "__main__":
    main()
