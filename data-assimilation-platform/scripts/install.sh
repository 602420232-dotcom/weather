#!/bin/bash
set -e

echo "=== 贝叶斯同化系统 安装脚本 ==="

# 检查 Python 版本
python_version=$(python3 --version 2>&1 | cut -d' ' -f2)
echo "Python 版本: $python_version"

# 创建虚拟环境
if [ ! -d "venv" ]; then
    echo "创建虚拟环境..."
    python3 -m venv venv
fi

source venv/bin/activate

# 安装核心算法库
echo "安装核心算法库..."
cd algorithm_core
pip install -e .
cd ..

echo ""
echo "安装完成！"
echo "运行以下命令启动:"
echo "  source venv/bin/activate"
echo "  assimilate --help"
