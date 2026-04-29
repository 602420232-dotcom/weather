#!/bin/bash
set -e

echo "=========================================="
echo "贝叶斯同化系统容器启动中..."
echo "=========================================="

# 解析命令参数
case "${1:-api}" in
    api)
        echo "启动 REST API 服务..."
        echo "API 文档: http://localhost:8000/docs"
        exec python -m uvicorn bayesian_assimilation.api.rest:app --host 0.0.0.0 --port 8000
        ;;

    web)
        echo "启动 Web 服务..."
        echo "Web 界面: http://localhost:5000"
        exec python -c "from bayesian_assimilation.api.web import run; run(host='0.0.0.0', port=5000)"
        ;;

    cli)
        echo "运行 CLI 命令..."
        shift
        exec python -m bayesian_assimilation.api.cli "$@"
        ;;

    all)
        echo "启动所有服务..."
        echo "REST API: http://localhost:8000"
        echo "Web 界面: http://localhost:5000"
        echo "按 Ctrl+C 停止所有服务"
        # 启动 API (后台)
        python -m uvicorn bayesian_assimilation.api.rest:app --host 0.0.0.0 --port 8000 &
        # 启动 Web (前台)
        exec python -c "from bayesian_assimilation.api.web import run; run(host='0.0.0.0', port=5000)"
        ;;

    bash)
        exec /bin/bash
        ;;

    *)
        echo "未知命令: $1"
        echo "可用命令: api, web, cli, all, bash"
        exit 1
        ;;
esac
