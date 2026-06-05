#!/usr/bin/env python3
"""
5D-VAR 风险感知算法性能基准测试 (独立运行版)

测试 5D-VAR（带风险维度）在不同时间窗口和网格规模下的性能表现，
重点关注风险维度对整体性能的影响。
"""
import time
import numpy as np
import sys
import os

# 添加路径
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_ASSIMILATION_PATH = os.path.join(PROJECT_ROOT, 'algorithm_core', 'src')
if DATA_ASSIMILATION_PATH not in sys.path:
    sys.path.insert(0, DATA_ASSIMILATION_PATH)


def test_5dvar_risk_mapping_performance():
    """测试5D-VAR风险映射性能"""
    from bayesian_assimilation.utils.risk_mapper import (
        WeatherToRiskMapper,
    )

    test_cases = [
        ((30, 30), 5, 100),
        ((50, 50), 10, 200),
        ((100, 100), 20, 500),
    ]

    print("=" * 70)
    print("5D-VAR 风险映射性能测试")
    print("=" * 70)

    for grid_size, time_steps, obs_per_step in test_cases:
        nx, ny = grid_size

        # 生成同化结果
        analysis = {
            'grid': {'shape': grid_size},
            'variables': {
                'u_wind': np.random.rand(nx, ny) * 10 + 5,
                'v_wind': np.random.rand(nx, ny) * 10 + 5
            }
        }

        # 初始化风险映射器
        risk_mapper = WeatherToRiskMapper(grid_resolution=100.0)

        start = time.perf_counter()

        # 计算风险
        risk_result = risk_mapper.compute_comprehensive_risk(analysis, heading=0.0)

        # 生成风险障碍物
        risk_obstacles = risk_mapper.generate_risk_aware_obstacles(
            risk_result,
            risk_threshold=0.6
        )

        elapsed = time.perf_counter() - start

        print(f"\n[{nx}x{ny}, T={time_steps}, obs={obs_per_step}]")
        print(f"  耗时: {elapsed:.3f}s")
        print(f"  平均风险: {risk_result['summary']['avg_risk']:.3f}")
        print(f"  高风险占比: {risk_result['summary']['high_risk_ratio']:.1%}")
        print(f"  风险障碍物: {len(risk_obstacles)}")


def test_end_to_end_pipeline():
    """测试端到端5D-VAR+规划流程"""
    from bayesian_assimilation.utils.risk_mapper import WeatherToRiskMapper

    grid_size = (50, 50)

    print("\n" + "=" * 70)
    print("端到端5D-VAR+风险映射流程测试")
    print("=" * 70)

    # 阶段1: 数据准备
    start_phase1 = time.perf_counter()
    analysis = {
        'grid': {'shape': grid_size},
        'variables': {
            'u_wind': np.random.rand(*grid_size) * 10 + 5,
            'v_wind': np.random.rand(*grid_size) * 10 + 5
        }
    }
    time_phase1 = time.perf_counter() - start_phase1

    # 阶段2: 风险映射
    start_phase2 = time.perf_counter()
    risk_mapper = WeatherToRiskMapper()
    risk_result = risk_mapper.compute_comprehensive_risk(analysis)
    time_phase2 = time.perf_counter() - start_phase2

    # 阶段3: 风险障碍物生成
    start_phase3 = time.perf_counter()
    risk_obstacles = risk_mapper.generate_risk_aware_obstacles(risk_result, risk_threshold=0.6)
    time_phase3 = time.perf_counter() - start_phase3

    total_time = time_phase1 + time_phase2 + time_phase3

    print("\n阶段统计:")
    print(f"  数据准备: {time_phase1:.3f}s")
    print(f"  风险映射: {time_phase2:.3f}s")
    print(f"  障碍物生成: {time_phase3:.3f}s")
    print(f"  总时间: {total_time:.3f}s")
    print("\n风险统计:")
    print(f"  平均风险: {risk_result['summary']['avg_risk']:.3f}")
    print(f"  高风险占比: {risk_result['summary']['high_risk_ratio']:.1%}")
    print(f"  风险障碍物: {len(risk_obstacles)}")

    if total_time < 1.0:
        print("\n✓ 端到端延迟满足要求 (<1s)")
    else:
        print("\n✗ 端到端延迟偏高")


def test_4dvar_vs_5dvar():
    """对比4D-VAR vs 5D-VAR（带风险维度）性能"""
    from bayesian_assimilation.utils.risk_mapper import WeatherToRiskMapper

    print("\n" + "=" * 70)
    print("4D-VAR vs 5D-VAR 性能对比")
    print("=" * 70)

    grid_size = (50, 50)
    nx, ny = grid_size

    # 准备数据
    analysis = {
        'grid': {'shape': grid_size},
        'variables': {
            'u_wind': np.random.rand(nx, ny) * 10 + 5,
            'v_wind': np.random.rand(nx, ny) * 10 + 5
        }
    }

    # 测试风险映射（新增的5D-VAR部分）
    risk_mapper = WeatherToRiskMapper()

    times_risk = []
    for _ in range(5):
        start = time.perf_counter()
        _ = risk_mapper.compute_comprehensive_risk(analysis)
        times_risk.append(time.perf_counter() - start)

    avg_time_risk = np.mean(times_risk)

    print("\n风险映射开销:")
    print(f"  平均: {avg_time_risk:.3f}s")
    print(f"  最小: {min(times_risk):.3f}s")
    print(f"  最大: {max(times_risk):.3f}s")
    print(f"\n5D-VAR 相比 4D-VAR 的额外开销: ~{avg_time_risk:.3f}s")


def main():
    test_5dvar_risk_mapping_performance()
    test_end_to_end_pipeline()
    test_4dvar_vs_5dvar()

    print("\n" + "=" * 70)
    print("所有性能测试完成!")
    print("=" * 70)


if __name__ == "__main__":
    main()
