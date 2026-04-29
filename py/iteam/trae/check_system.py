#!/usr/bin/env python3
# check_system.py
# 系统检查脚本

import os
import sys

# 检查项目结构
def check_project_structure():
    print("=== 检查项目结构 ===")

    # 检查核心目录（使用连字符格式）
    core_dirs = [
        "data-assimilation-platform",
        "uav-path-planning-system",
        "wrf-processor-service",
        "meteor-forecast-service",
        "path-planning-service",
        "uav-platform-service",
        "deployments",
        "docs",
        "tests"
    ]

    for dir_name in core_dirs:
        if os.path.exists(dir_name):
            print(f"[OK] {dir_name} 目录存在")
        else:
            print(f"[ERROR] {dir_name} 目录不存在")

    # 检查配置文件
    config_files = [
        "docker-compose.yml",
        "data-assimilation-platform/docker-compose.yml",
        "uav-path-planning-system/docker-compose.yml",
        ".env.example"
    ]

    print("\n=== 检查配置文件 ===")
    for config_file in config_files:
        if os.path.exists(config_file):
            print(f"[OK] {config_file} 存在")
        else:
            print(f"[ERROR] {config_file} 不存在")

# 检查Python环境
def check_python_environment():
    print("\n=== 检查Python环境 ===")
    print(f"Python版本: {sys.version}")

    # 检查核心依赖
    try:
        import numpy
        print("[OK] numpy 已安装")
    except ImportError:
        print("[ERROR] numpy 未安装")

    try:
        import scipy
        print("[OK] scipy 已安装")
    except ImportError:
        print("[ERROR] scipy 未安装")

    try:
        import pandas
        print("[OK] pandas 已安装")
    except ImportError:
        print("[ERROR] pandas 未安装")

    try:
        import netCDF4
        print("[OK] netCDF4 已安装")
    except ImportError:
        print("[WARNING] netCDF4 未安装")

    try:
        import xgboost
        print("[OK] xgboost 已安装")
    except ImportError:
        print("[WARNING] xgboost 未安装")

    try:
        import sklearn
        print("[OK] sklearn 已安装")
    except ImportError:
        print("[WARNING] sklearn 未安装")

# 检查Java环境
def check_java_environment():
    print("\n=== 检查Java环境 ===")
    try:
        import subprocess
        result = subprocess.run(["java", "-version"], capture_output=True, text=True)
        print(f"Java版本: {result.stderr.split(chr(10))[0]}")
    except Exception as e:
        print(f"[ERROR] Java未安装或无法访问: {e}")

# 检查Docker环境
def check_docker_environment():
    print("\n=== 检查Docker环境 ===")
    try:
        import subprocess
        result = subprocess.run(["docker", "--version"], capture_output=True, text=True)
        print(f"Docker版本: {result.stdout.strip()}")
    except Exception as e:
        print(f"[ERROR] Docker未安装或无法访问: {e}")

# 检查算法实现
def check_algorithm_implementation():
    print("\n=== 检查算法实现 ===")

    # 检查贝叶斯同化算法
    assimilation_files = [
        "data-assimilation-platform/algorithm_core/src/bayesian_assimilation/models/three_dimensional_var.py",
        "data-assimilation-platform/algorithm_core/src/bayesian_assimilation/models/four_dimensional_var.py",
        "data-assimilation-platform/algorithm_core/src/bayesian_assimilation/models/enkf.py",
        "data-assimilation-platform/algorithm_core/src/bayesian_assimilation/models/enhanced_bayesian.py",
        "data-assimilation-platform/algorithm_core/src/bayesian_assimilation/core/assimilator.py",
        "data-assimilation-platform/algorithm_core/src/bayesian_assimilation/core/base.py"
    ]

    for file_path in assimilation_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
        else:
            print(f"[ERROR] {file_path} 不存在")

    # 检查路径规划算法
    planning_files = [
        "path-planning-service/src/main/python/three_layer_planner.py",
        "path-planning-service/src/main/python/advanced_planners.py",
        "path-planning-service/src/main/python/optimized_planner.py",
        "path-planning-service/src/main/python/reinforcement_learning.py"
    ]

    print("\n=== 检查路径规划算法 ===")
    for file_path in planning_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
        else:
            print(f"[ERROR] {file_path} 不存在")

    # 检查气象预测算法
    meteor_files = [
        "meteor-forecast-service/src/main/python/meteor_forecast.py",
        "uav-path-planning-system/algorithm-core/prediction/meteor_forecast.py",
        "uav-path-planning-system/algorithm-core/prediction/meteor_correction.py"
    ]

    print("\n=== 检查气象预测算法 ===")
    for file_path in meteor_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
        else:
            print(f"[ERROR] {file_path} 不存在")

# 检查前端实现
def check_frontend_implementation():
    print("\n=== 检查前端实现 ===")
    frontend_files = [
        "uav-path-planning-system/frontend-vue/package.json",
        "uav-path-planning-system/frontend-vue/src/App.vue",
        "uav-path-planning-system/frontend-vue/src/views/PathPlanningView.vue",
        "uav-path-planning-system/frontend-vue/src/views/WeatherView.vue",
        "uav-path-planning-system/frontend-vue/src/views/MonitoringView.vue",
        "uav-path-planning-system/frontend-vue/src/views/HistoryView.vue"
    ]

    # 检查文件是否存在（即使路径不存在也检查）
    existing_count = 0
    total_count = len(frontend_files)

    for file_path in frontend_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
            existing_count += 1
        else:
            print(f"[ERROR] {file_path} 不存在")

    if existing_count == 0:
        print(f"[INFO] 前端目录可能不存在或使用了不同的目录结构")

# 检查安全性实现
def check_security_implementation():
    print("\n=== 检查安全性实现 ===")
    security_files = [
        "uav-path-planning-system/backend-spring/src/main/java/com/uav/config/SecurityConfig.java",
        "uav-path-planning-system/backend-spring/src/main/java/com/uav/config/JwtUtil.java",
        "uav-path-planning-system/backend-spring/src/main/java/com/uav/config/JwtFilter.java",
        "uav-path-planning-system/backend-spring/src/main/java/com/uav/model/User.java",
        "uav-path-planning-system/backend-spring/src/main/java/com/uav/model/Role.java",
        "uav-path-planning-system/backend-spring/src/main/java/com/uav/service/CustomUserDetailsService.java"
    ]

    existing_count = 0
    total_count = len(security_files)

    for file_path in security_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
            existing_count += 1
        else:
            print(f"[ERROR] {file_path} 不存在")

    if existing_count == 0:
        print(f"[INFO] 后端Spring目录可能不存在或使用了不同的目录结构")

# 检查部署配置
def check_deployment():
    print("\n=== 检查部署配置 ===")
    deployment_files = [
        "docker-compose.yml",
        "DEPLOYMENT.md",
        "README.md",
        ".env.example"
    ]

    for file_path in deployment_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
        else:
            print(f"[ERROR] {file_path} 不存在")

    # 检查Kubernetes配置
    k8s_files = [
        "deployments/kubernetes/namespace.yml",
        "deployments/kubernetes/secrets.yml"
    ]

    for file_path in k8s_files:
        if os.path.exists(file_path):
            print(f"[OK] {file_path} 存在")
        else:
            print(f"[ERROR] {file_path} 不存在")

# 主函数
def main():
    print("开始检查无人机路径规划系统...")
    print("=" * 60)

    check_project_structure()
    check_python_environment()
    check_java_environment()
    check_docker_environment()
    check_algorithm_implementation()
    check_frontend_implementation()
    check_security_implementation()
    check_deployment()

    print("=" * 60)
    print("系统检查完成！")

if __name__ == "__main__":
    main()
