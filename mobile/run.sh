#!/bin/bash
# ============================================================
# Flutter 无人机路径规划系统 — 运行/构建脚本
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${SCRIPT_DIR}"

echo "=== 无人机路径规划系统 - Flutter 移动端 ==="
echo ""

show_help() {
    echo "用法: ./run.sh [命令]"
    echo ""
    echo "命令:"
    echo "  doctor   检查 Flutter 环境"
    echo "  deps     安装依赖"
    echo "  run      启动开发服务器"
    echo "  web      启动 Web 端（默认: http://localhost:8088）"
    echo "  build    构建 APK（Android）"
    echo "  build-web 构建 Web 部署包"
    echo "  clean    清理构建缓存"
    echo ""
    echo "环境变量:"
    echo "  API_BASE_URL   后端 API 地址 (默认: http://localhost:8088)"
    echo "  RELEASE_KEY    Android 签名密钥路径"
}

case "${1:-help}" in
    doctor)
        flutter doctor
        ;;
    deps)
        flutter pub get
        ;;
    run)
        flutter run \
            --dart-define=API_BASE_URL="${API_BASE_URL:-http://localhost:8088}"
        ;;
    web)
        API="${API_BASE_URL:-http://localhost:8088}"
        echo "启动 Web 端 → API: ${API}"
        flutter run -d chrome \
            --dart-define=API_BASE_URL="${API}"
        ;;
    build)
        echo "构建 Android APK..."
        if [ -n "${RELEASE_KEY:-}" ]; then
            flutter build apk --release \
                --dart-define=API_BASE_URL="${API_BASE_URL:-http://localhost:8088}" \
                --dart-define=APP_VERSION="1.0.0"
        else
            flutter build apk --debug \
                --dart-define=API_BASE_URL="${API_BASE_URL:-http://localhost:8088}"
        fi
        echo "✅ APK: build/app/outputs/flutter-apk/app-debug.apk"
        ;;
    build-web)
        echo "构建 Web 部署包..."
        flutter build web \
            --dart-define=API_BASE_URL="${API_BASE_URL:-http://localhost:8088}"
        echo "✅ Web: build/web/"
        ;;
    clean)
        flutter clean
        flutter pub get
        echo "✅ 清理完成"
        ;;
    *)
        show_help
        ;;
esac
