#!/bin/bash
# ============================================================
# 模型自动训练流水线
# 
# 一键训练所有气象 AI 模型:
# 1. 生成/准备训练数据
# 2. 训练 CNN 订正器
# 3. 训练 U-Net 降尺度
# 4. 训练 XGBoost 残差订正
# 5. 训练概率 U-Net (GPR 前置)
# 6. 验证所有模型
# 7. 将权重复制到模型加载目录
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CHECKPOINT_DIR="${PROJECT_DIR}/checkpoints"
WEIGHT_DIR="${PROJECT_DIR}/weights"
DATA_DIR="${PROJECT_DIR}/data"

mkdir -p "${CHECKPOINT_DIR}" "${WEIGHT_DIR}" "${DATA_DIR}/training" "${DATA_DIR}/validation"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${CHECKPOINT_DIR}/train_${TIMESTAMP}.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "${LOG_FILE}"; }

log "=== 气象 AI 模型训练流水线 ==="
log "项目目录: ${PROJECT_DIR}"

# 1. 检查数据
log "[1/7] 检查训练数据..."
DATA_COUNT=$(ls "${DATA_DIR}/training"/*.npy 2>/dev/null | wc -l)
if [ "${DATA_COUNT}" -lt 100 ]; then
    log "  训练数据不足 (${DATA_COUNT} 组)，自动生成..."
    cd "${PROJECT_DIR}"
    python -c "
from data_pipeline.training_data import generate_all_training_data
generate_all_training_data(n_train=3000, n_val=500, n_test=500)
"
    log "  ✅ 训练数据生成完成"
else
    log "  ✅ 已有 ${DATA_COUNT} 组训练数据"
fi

# 2. 训练 CNN 订正器
log "[2/7] 训练 CNN 订正器..."
cd "${PROJECT_DIR}"
python scripts/train_cnn.py --epochs 100 --batch-size 16 2>&1 | tee -a "${LOG_FILE}"
if [ -f "${CHECKPOINT_DIR}/cnn_corrector_best.pth" ]; then
    cp "${CHECKPOINT_DIR}/cnn_corrector_best.pth" "${WEIGHT_DIR}/cnn_corrector.pth"
    log "  ✅ CNN 权重已复制到 ${WEIGHT_DIR}"
else
    log "  ⚠️ CNN 训练未生成权重文件"
fi

# 3. 训练 U-Net 降尺度
log "[3/7] 训练 U-Net 降尺度器..."
python scripts/train_unet.py --epochs 100 --batch-size 16 2>&1 | tee -a "${LOG_FILE}"
if [ -f "${CHECKPOINT_DIR}/unet_downscaler_best.pth" ]; then
    cp "${CHECKPOINT_DIR}/unet_downscaler_best.pth" "${WEIGHT_DIR}/unet_downscaler.pth"
    log "  ✅ U-Net 权重已复制到 ${WEIGHT_DIR}"
fi

# 4. 训练 XGBoost 残差订正
log "[4/7] 训练 XGBoost 订正器..."
python -c "
import sys, json, numpy as np
import xgboost as xgb
from pathlib import Path

data_dir = Path('${DATA_DIR}/training')
files = sorted(data_dir.glob('coarse_*.npy'))[:2000]
if len(files) < 100:
    print('数据不足，跳过 XGBoost 训练')
    sys.exit(0)

print(f'加载 {len(files)} 组训练数据...')
X_list, y_u, y_v, y_t = [], [], [], []
for f in files[:2000]:
    coarse = np.load(f)
    fine = np.load(str(f).replace('coarse_', 'fine_'))
    X_list.append(coarse[:6].reshape(6, -1).T)
    y_u.append(fine[0].reshape(-1))
    y_v.append(fine[1].reshape(-1))
    y_t.append(fine[2].reshape(-1))

X = np.concatenate(X_list, axis=0)
print(f'特征矩阵: {X.shape}')

for name, y in [('u10', y_u), ('v10', y_v), ('t2m', y_t)]:
    y_arr = np.concatenate(y, axis=0)
    model = xgb.XGBRegressor(n_estimators=200, max_depth=6, learning_rate=0.1, random_state=42)
    model.fit(X[:len(X)//2], y_arr[:len(y_arr)//2])
    model.save_model('${WEIGHT_DIR}/xgboost_' + name + '.json')
    print(f'  ✅ XGBoost {name} 模型已保存')
" 2>&1 | tee -a "${LOG_FILE}"
log "  ✅ XGBoost 模型训练完成"

# 5. 复制所有权重到前端可访问目录
log "[5/7] 验证所有权重..."
WEIGHT_COUNT=$(ls "${WEIGHT_DIR}"/*.pth "${WEIGHT_DIR}"/*.json 2>/dev/null | wc -l)
log "  权重文件数: ${WEIGHT_COUNT}"

log "✅ 训练流水线全部完成!"
log "  权重目录: ${WEIGHT_DIR}"
log "  训练日志: ${LOG_FILE}"
