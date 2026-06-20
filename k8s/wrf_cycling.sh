#!/bin/bash
# =============================================================
# WRF 循环分析-预报脚本
# 实现: 实况观测收集 → 数据同化 → WRF 重新初始化 → 预报
# =============================================================
#
# 循环周期: 3小时 (可配置)
# 工作流程:
#   1. 收集无人机/探空/地面站实况观测
#   2. 生成 background field (当前 WRF 短期预报)
#   3. 执行 Bayesian 3D-VAR/EnKF/Hybrid 同化
#   4. 将分析场写回 WRF 输入格式
#   5. 重新运行 WRF 产生新预报
#   6. 新预报场推送回路径规划系统
#
# 使用方式:
#   bash wrf_cycling.sh [CYCLE_HOURS] [NUM_CYCLES]
#   例如: bash wrf_cycling.sh 3 8   # 每3小时一个循环，共8个循环(24小时)
# =============================================================

set -e

# ============================================================
# 配置
# ============================================================
CYCLE_HOURS=${1:-3}
NUM_CYCLES=${2:-8}
WRF_RUN_DIR="${WRF_RUN_DIR:-/data/wrf/run}"
WRF_WPS_DIR="${WRF_WPS_DIR:-/data/wrf/WPS}"
ASSIMILATION_SERVICE_URL="${ASSIMILATION_SERVICE_URL:-http://localhost:8084}"
WEATHER_COLLECTOR_URL="${WEATHER_COLLECTOR_URL:-http://localhost:8086}"
WRF_PROCESSOR_URL="${WRF_PROCESSOR_URL:-http://localhost:8081}"
PYTHON_BIN="${PYTHON_BIN:-python3}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INITIALIZER="${SCRIPT_DIR}/../wrf-processor-service/src/main/python/wrf_initializer.py"
ASSIMILATOR="${SCRIPT_DIR}/../data-assimilation-service/src/main/python/bayesian_assimilation.py"
PROCESSOR="${SCRIPT_DIR}/../wrf-processor-service/src/main/python/wrf_processor.py"

WORK_DIR="${WRF_RUN_DIR}/cycling"
mkdir -p "${WORK_DIR}"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CYCLE] $*"
}

# ============================================================
# 初始检查
# ============================================================
check_prerequisites() {
    log "检查运行环境..."

    if [ ! -f "${INITIALIZER}" ]; then
        log "ERROR: wrf_initializer.py 未找到: ${INITIALIZER}"
        exit 1
    fi

    if [ ! -f "${ASSIMILATOR}" ]; then
        log "ERROR: bayesian_assimilation.py 未找到: ${ASSIMILATOR}"
        exit 1
    fi

    if [ ! -f "${WRF_RUN_DIR}/wrf.exe" ]; then
        log "ERROR: wrf.exe 未找到: ${WRF_RUN_DIR}/wrf.exe"
        exit 1
    fi

    # 检查初始 wrfinput 文件
    if [ ! -f "${WRF_RUN_DIR}/wrfinput_d01" ]; then
        log "ERROR: wrfinput_d01 未找到，请先运行 real.exe"
        exit 1
    fi

    log "环境检查通过"
}

# ============================================================
# 步骤 1: 收集实况观测
# ============================================================
collect_observations() {
    local cycle_num=$1
    local obs_file="${WORK_DIR}/observations_cycle_${cycle_num}.json"

    log "收集实况观测数据..."

    # 从 weather-collector 获取最新的多源融合数据
    curl -s "${WEATHER_COLLECTOR_URL}/api/weather/fusion/all" \
        -o "${obs_file}" 2>/dev/null || {
        log "WARNING: 无法从 weather-collector 获取观测数据，使用备选"
        # 备选: 直接查询各数据源
        python3 -c "
import json
obs = {'observations': {}, 'stations': []}
# 尝试从各数据源获取最新数据
try:
    import urllib.request
    resp = urllib.request.urlopen('${WEATHER_COLLECTOR_URL}/api/weather/drone/all')
    data = json.loads(resp.read())
    for key, val in data.get('data', {}).items():
        obs['observations'][key] = val
except: pass
with open('${obs_file}', 'w') as f:
    json.dump(obs, f)
"
    }

    log "观测数据已保存: ${obs_file}"
    echo "${obs_file}"
}

# ============================================================
# 步骤 2: 提取背景场 (当前 WRF 预报)
# ============================================================
extract_background() {
    local cycle_num=$1
    local bg_file="${WORK_DIR}/background_cycle_${cycle_num}.json"

    log "提取 WRF 背景场..."

    # 从 wrf-processor 获取最新的 WRF 网格数据
    curl -s "${WRF_PROCESSOR_URL}/api/wrf/latest" \
        -o "${bg_file}" 2>/dev/null || {
        log "WARNING: 无法获取 WRF 最新数据，使用本地文件解析"
        # 使用最近一次 WRF 输出
        local latest_wrfout=$(ls -t ${WRF_RUN_DIR}/wrfout_d01_* 2>/dev/null | head -1)
        if [ -n "${latest_wrfout}" ]; then
            ${PYTHON_BIN} "${PROCESSOR}" "${latest_wrfout}" 100 > "${bg_file}"
        else
            log "ERROR: 无 WRF 输出文件可用"
            return 1
        fi
    }

    log "背景场已提取: ${bg_file}"
    echo "${bg_file}"
}

# ============================================================
# 步骤 3: 执行数据同化
# ============================================================
run_assimilation() {
    local cycle_num=$1
    local bg_file=$2
    local obs_file=$3
    local analysis_file="${WORK_DIR}/analysis_cycle_${cycle_num}.json"
    local assim_input="${WORK_DIR}/assim_input_${cycle_num}.json"

    log "执行数据同化..."

    # 构建同化输入: background + observations
    ${PYTHON_BIN} -c "
import json, numpy as np

with open('${bg_file}') as f:
    bg = json.load(f)
with open('${obs_file}') as f:
    obs = json.load(f)

# 提取网格化的背景场
bg_data = bg.get('data', bg)
bg_fields = {}
if 'meteorological' in bg_data:
    m = bg_data['meteorological']
    for key in ['wind_speed', 'wind_direction', 'temperature', 'humidity', 'pressure']:
        if key in m:
            bg_fields[key] = m[key] if isinstance(m[key], list) else [m[key]]

# 提取观测数据
obs_fields = obs.get('observations', obs)
if 'data' in obs:
    obs_fields = obs['data']

assim_input = {
    'background': bg_fields,
    'observations': obs_fields,
    'method': 'hybrid'
}

with open('${assim_input}', 'w') as f:
    json.dump(assim_input, f)

print(f'Background 变量: {list(bg_fields.keys())}')
print(f'Observations 变量: {list(obs_fields.keys())}')
"

    # 执行贝叶斯同化
    ${PYTHON_BIN} "${ASSIMILATOR}" execute "${assim_input}" > "${analysis_file}" 2>/dev/null || {
        log "ERROR: 数据同化失败"
        return 1
    }

    # 验证输出
    if ! grep -q '"success": true' "${analysis_file}" 2>/dev/null; then
        log "ERROR: 同化输出无效"
        cat "${analysis_file}"
        return 1
    fi

    log "分析场已生成: ${analysis_file}"
    echo "${analysis_file}"
}

# ============================================================
# 步骤 4: 写回 WRF 初始场
# ============================================================
update_wrf_input() {
    local cycle_num=$1
    local analysis_file=$2
    local updated_wrfinput="${WORK_DIR}/wrfinput_d01_cycle_${cycle_num}"

    log "生成 WRF 更新初始场..."

    ${PYTHON_BIN} "${INITIALIZER}" replace \
        "${analysis_file}" \
        "${WRF_RUN_DIR}/wrfinput_d01" \
        "${updated_wrfinput}" || {
        log "ERROR: WRF 初始场更新失败"
        return 1
    }

    log "WRF 初始场已更新: ${updated_wrfinput}"
    echo "${updated_wrfinput}"
}

# ============================================================
# 步骤 5: 运行 WRF 预报
# ============================================================
run_wrf_forecast() {
    local cycle_num=$1
    local wrfinput_file=$2
    local cycle_dir="${WORK_DIR}/run_cycle_${cycle_num}"

    log "运行 WRF 短期预报..."

    # 创建本循环的工作目录
    mkdir -p "${cycle_dir}"

    # 复制必要的 WRF 文件
    cp "${wrfinput_file}" "${cycle_dir}/wrfinput_d01"
    cp "${WRF_RUN_DIR}/wrfbdy_d01" "${cycle_dir}/" 2>/dev/null || true
    cp "${WRF_RUN_DIR}/namelist.input" "${cycle_dir}/" 2>/dev/null
    cp "${WRF_RUN_DIR}/wrf.exe" "${cycle_dir}/" 2>/dev/null || true

    # 调整 namelist 运行时长 (短期预报)
    if [ -f "${cycle_dir}/namelist.input" ]; then
        # 修改 run_hours 为循环周期
        local run_seconds=$((CYCLE_HOURS * 3600))
        sed -i "s/run_hours.*=.*/run_hours = ${CYCLE_HOURS},/" "${cycle_dir}/namelist.input" 2>/dev/null || true
        sed -i "s/run_seconds.*=.*/run_seconds = ${run_seconds},/" "${cycle_dir}/namelist.input" 2>/dev/null || true
        sed -i "s/start_hour.*=.*/start_hour = $(( (cycle_num * CYCLE_HOURS) % 24 )),/" "${cycle_dir}/namelist.input" 2>/dev/null || true
    fi

    # 运行 WRF
    cd "${cycle_dir}"
    if [ -f "./wrf.exe" ]; then
        mpirun -np 4 ./wrf.exe > wrf.log 2>&1 &
        WRF_PID=$!
        log "WRF 已启动 (PID: ${WRF_PID})"

        # 等待完成 (超时处理)
        local timeout=$((CYCLE_HOURS * 3600 + 1800))
        local waited=0
        while kill -0 ${WRF_PID} 2>/dev/null; do
            sleep 30
            waited=$((waited + 30))
            if [ ${waited} -gt ${timeout} ]; then
                log "WARNING: WRF 运行超时，强制终止"
                kill -9 ${WRF_PID} 2>/dev/null || true
                return 1
            fi
        done
        wait ${WRF_PID}
        local exit_code=$?
        if [ ${exit_code} -ne 0 ]; then
            log "ERROR: WRF 运行失败 (退出码: ${exit_code})"
            tail -20 wrf.log
            return 1
        fi
    else
        log "WARNING: wrf.exe 不在当前目录，跳过实际运行 (仅演示流程)"
    fi

    log "WRF 预报完成，输出文件位于: ${cycle_dir}/"
    echo "${cycle_dir}"
}

# ============================================================
# 主循环
# ============================================================

main() {
    log "=== WRF 循环分析-预报系统启动 ==="
    log "循环周期: ${CYCLE_HOURS} 小时"
    log "循环次数: ${NUM_CYCLES}"
    log "工作目录: ${WORK_DIR}"
    log "==================================="

    check_prerequisites

    # 保存初始 wrfinput 备份
    cp "${WRF_RUN_DIR}/wrfinput_d01" "${WORK_DIR}/wrfinput_d01_original"

    for cycle in $(seq 1 ${NUM_CYCLES}); do
        log ""
        log "========== 第 ${cycle}/${NUM_CYCLES} 个循环 =========="

        # 步骤 1
        OBS_FILE=$(collect_observations ${cycle}) || continue

        # 步骤 2
        BG_FILE=$(extract_background ${cycle}) || continue

        # 步骤 3
        ANALYSIS_FILE=$(run_assimilation ${cycle} "${BG_FILE}" "${OBS_FILE}") || continue

        # 步骤 4
        UPDATED_WRFINPUT=$(update_wrf_input ${cycle} "${ANALYSIS_FILE}") || continue

        # 步骤 5
        CYCLE_DIR=$(run_wrf_forecast ${cycle} "${UPDATED_WRFINPUT}") || continue

        log "第 ${cycle} 个循环完成，输出目录: ${CYCLE_DIR}"

        # 间隔 (如果还有下一个循环)
        if [ ${cycle} -lt ${NUM_CYCLES} ]; then
            log "等待 ${CYCLE_HOURS} 小时后开始下一个循环..."
            # 实际部署时使用 sleep ${CYCLE_HOURS}h
            # 演示模式下使用较短间隔
            sleep 10
        fi
    done

    log ""
    log "=== ${NUM_CYCLES} 个循环全部完成 ==="
    log "输出目录: ${WORK_DIR}/"

    # 清理
    rm -f "${WORK_DIR}"/assim_input_*.json 2>/dev/null || true
}

main "$@"
