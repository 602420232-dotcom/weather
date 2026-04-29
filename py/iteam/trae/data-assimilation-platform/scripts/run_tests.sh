#!/bin/bash
# 运行所有演示的脚本

set -e  # 遇到错误退出

echo "开始运行贝叶斯同化系统所有演示..."

# 激活虚拟环境（如果有）
if [ -d "venv" ]; then
    echo "激活虚拟环境..."
    source venv/bin/activate
fi

# 安装依赖
echo "安装依赖..."
pip install -e . > /dev/null 2>&1 || {
    echo "依赖安装失败，尝试使用requirements.txt..."
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt
    fi
}

# 运行基础演示
echo -e "\n运行基础演示..."
cd examples
python -c "
import sys
sys.path.insert(0, '..')
from bayesian_assimilation.core.assimilator import BaseAssimilator
from bayesian_assimilation.core.config import ConfigFactory
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)

config = ConfigFactory.create('base')
assim = BaseAssimilator(config)
assim.initialize_grid((1000, 1000, 200), 50)
print('✅ 基础演示成功')
"

# 运行优化演示
echo -e "\n运行优化演示..."
python -c "
import sys
sys.path.insert(0, '..')
from bayesian_assimilation.core.assimilator import OptimizedAssimilator
from bayesian_assimilation.core.config import ConfigFactory
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)

config = ConfigFactory.create('optimized')
assim = OptimizedAssimilator(config)
assim.initialize_grid((1000, 1000, 200), 50)
print('✅ 优化演示成功')
"

# 运行自适应演示
echo -e "\n运行自适应演示..."
python -c "
import sys
sys.path.insert(0, '..')
from bayesian_assimilation.core.adaptive_assimilator import AdaptiveAssimilator
from bayesian_assimilation.core.config import ConfigFactory
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)

config = ConfigFactory.create('adaptive', use_gpu=False)
assim = AdaptiveAssimilator(config)
assim.initialize_adaptive_grid()
print('✅ 自适应演示成功')
"

# 运行兼容性演示
echo -e "\n运行兼容性演示..."
python -c "
import sys
sys.path.insert(0, '..')
from bayesian_assimilation.core.compatible_assimilator import CompatibleAssimilator
from bayesian_assimilation.core.config import ConfigFactory
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)

config = ConfigFactory.create('compatible', use_gpu=False)
assim = CompatibleAssimilator(config)
assim.initialize_grid_safe()
print('✅ 兼容性演示成功')
"

echo -e "\n🎉 所有演示运行完成！"
echo "详细演示请运行: python examples/all_demos.py"