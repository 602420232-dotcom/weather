#!/bin/bash
# ==============================================
# Maven 依赖修复脚本 (Linux/macOS)
# 从项目根目录运行
# ==============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "[INFO] 开始清理和重新下载 Maven 依赖..."
echo "[INFO] 项目根目录: $PROJECT_ROOT"

cd "$PROJECT_ROOT"

# 确认在项目根目录
if [ ! -f "pom.xml" ]; then
    echo "[ERROR] 未找到 pom.xml，请确认脚本位于 scripts/ 目录"
    exit 1
fi

# 使用根 pom.xml 统一构建（8个子模块）
mvn clean install -U -DskipTests

echo ""
echo "[INFO] =========================================="
echo "[INFO] Maven 依赖重新下载完成！"
echo "[INFO] 包含模块:"
echo "[INFO]   api-gateway"
echo "[INFO]   wrf-processor-service"
echo "[INFO]   meteor-forecast-service"
echo "[INFO]   path-planning-service"
echo "[INFO]   data-assimilation-service"
echo "[INFO]   uav-platform-service"
echo "[INFO]   uav-weather-collector"
echo "[INFO]   backend-spring"
echo "[INFO] =========================================="
