#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UAV Edge SDK 单元测试
"""

import sys
import os
import io

# 设置标准输出编码为UTF-8
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

# 添加 edge_sdk 路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from edge_sdk._core import EdgeSDK, create_sdk, plan_path, assess_weather, HAS_CPP_MODULE
from edge_sdk.path_planner_python import PathPlannerFallback
from edge_sdk.risk_assessor_python import RiskAssessorFallback


def run_tests():
    """运行所有测试"""
    print("=" * 60)
    print("UAV Edge SDK Unit Tests")
    print("=" * 60)
    
    passed = 0
    failed = 0
    
    # Test PathPlanner
    print("\n[1] Path Planner Tests")
    print("-" * 40)
    
    try:
        planner = PathPlannerFallback(100, 100, 1.0)
        assert planner is not None
        print("  [PASS] Create path planner")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] Create path planner: {e}")
        failed += 1
    
    try:
        path = planner.plan((0, 0), (5, 5))
        assert len(path) > 0
        print(f"  [PASS] Simple path planning (length: {len(path)})")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] Simple path planning: {e}")
        failed += 1
    
    try:
        obstacles = [(5, 5), (5, 6), (6, 5), (6, 6)]
        path = planner.plan((0, 0), (10, 10), obstacles)
        assert len(path) > 0
        print(f"  [PASS] Obstacle path planning (length: {len(path)})")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] Obstacle path planning: {e}")
        failed += 1
    
    # Test RiskAssessor
    print("\n[2] Risk Assessor Tests")
    print("-" * 40)
    
    try:
        assessor = RiskAssessorFallback()
        print("  [PASS] Create risk assessor")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] Create risk assessor: {e}")
        failed += 1
    
    try:
        weather = {
            'wind_speed': 3.0, 'wind_direction': 180, 'temperature': 25.0,
            'humidity': 50.0, 'visibility': 15.0, 'precipitation': 0.0,
            'has_thunderstorm': False
        }
        result = assessor.assess(weather)
        assert result.level in [0, 1]  # LOW=0, MEDIUM=1
        level_names = {0: 'LOW', 1: 'MEDIUM', 2: 'HIGH', 3: 'SEVERE'}
        print(f"  [PASS] Low risk assessment (level: {level_names.get(result.level, 'UNKNOWN')}, score: {result.score})")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] Low risk assessment: {e}")
        failed += 1
    
    try:
        weather = {
            'wind_speed': 20.0, 'wind_direction': 90, 'temperature': 40.0,
            'humidity': 95.0, 'visibility': 1.0, 'precipitation': 10.0,
            'has_thunderstorm': True
        }
        result = assessor.assess(weather)
        assert result.level in [2, 3]  # HIGH=2, SEVERE=3
        level_names = {0: 'LOW', 1: 'MEDIUM', 2: 'HIGH', 3: 'SEVERE'}
        print(f"  [PASS] High risk assessment (level: {level_names.get(result.level, 'UNKNOWN')}, score: {result.score})")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] High risk assessment: {e}")
        failed += 1
    
    # Test EdgeSDK
    print("\n[3] Edge SDK Integration Tests")
    print("-" * 40)
    
    try:
        sdk = create_sdk({'grid_width': 100, 'grid_height': 100, 'resolution': 1.0})
        print("  [PASS] Create SDK instance")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] Create SDK instance: {e}")
        failed += 1
    
    try:
        path = sdk.plan_path((0, 0), (10, 10))
        assert len(path) > 0
        print(f"  [PASS] SDK path planning (length: {len(path)})")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] SDK path planning: {e}")
        failed += 1
    
    try:
        weather = {
            'wind_speed': 5.0, 'wind_direction': 180, 'temperature': 25.0,
            'humidity': 60.0, 'visibility': 10.0, 'precipitation': 0.0,
            'has_thunderstorm': False
        }
        result = sdk.assess_weather_risk(weather)
        assert 'level' in result and 'score' in result
        print(f"  [PASS] SDK weather assessment (level: {result['level']})")
        passed += 1
    except Exception as e:
        print(f"  [FAIL] SDK weather assessment: {e}")
        failed += 1
    
    # Module Info
    print("\n[4] Module Info")
    print("-" * 40)
    from edge_sdk import __version__
    print(f"  Version: {__version__}")
    print(f"  C++ Module Available: {HAS_CPP_MODULE}")
    
    # Summary
    print("\n" + "=" * 60)
    print(f"Test Results: {passed} passed, {failed} failed")
    print("=" * 60)
    
    return failed == 0


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
