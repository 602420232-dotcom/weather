#!/bin/bash
# ============================================================
# Docker 数据卷备份脚本
# 
# 功能: 备份所有 Docker 命名数据卷 + MySQL 逻辑备份
# 用法: ./backup-volumes.sh [backup-dir]
# 默认备份路径: ./backups/
# ============================================================

set -euo pipefail

BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_PATH="${BACKUP_DIR}/${TIMESTAMP}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
LOG_FILE="${BACKUP_DIR}/backup.log"

mkdir -p "${BACKUP_PATH}"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "${LOG_FILE}"
}

log "=== 开始数据卷备份: ${TIMESTAMP} ==="

# 1. 备份 Docker 命名数据卷
log "[1/3] 备份 Docker 命名数据卷..."
for volume in $(docker volume ls --format '{{.Name}}' | grep -E '^(uav|trae|mysql|redis)'); do
    log "  正在备份数据卷: ${volume}"
    docker run --rm \
        -v "${volume}:/source" \
        -v "${BACKUP_PATH}:/backup" \
        alpine:3.18 \
        tar czf "/backup/volume-${volume}.tar.gz" \
        -C /source . \
        2>/dev/null || log "  ⚠️ 数据卷 ${volume} 备份失败（可能是空卷或不存在）"
done

# 2. MySQL 逻辑备份（需要 MySQL 容器运行中）
log "[2/3] MySQL 逻辑备份..."
MYSQL_CONTAINER=$(docker ps --filter "name=uav-mysql" --format '{{.Names}}' 2>/dev/null || echo "")
if [ -n "${MYSQL_CONTAINER}" ]; then
    log "  发现 MySQL 容器: ${MYSQL_CONTAINER}"
    for db in uav_platform path_planning wrf_processor meteor_forecast data_assimilation uav_weather; do
        log "  正在备份数据库: ${db}"
        docker exec "${MYSQL_CONTAINER}" \
            mysqldump --single-transaction --routines --triggers \
            -u root -p"${MYSQL_ROOT_PASSWORD}" "${db}" \
            2>/dev/null > "${BACKUP_PATH}/mysql-${db}.sql" \
            && log "  ✅ ${db} 备份成功 ($(wc -c < "${BACKUP_PATH}/mysql-${db}.sql") bytes)" \
            || log "  ⚠️ ${db} 备份失败（数据库可能不存在）"
    done
else
    log "  ⚠️ MySQL 容器未运行，跳过逻辑备份"
fi

# 3. 备份 Docker Compose 配置文件
log "[3/3] 备份配置文件..."
CONFIG_DIRS=(
    "./deployments/monitoring/prometheus"
    "./deployments/monitoring/alertmanager"
    "./deployments/kubernetes"
)
for dir in "${CONFIG_DIRS[@]}"; do
    if [ -d "${dir}" ]; then
        tar czf "${BACKUP_PATH}/config-$(basename ${dir}).tar.gz" -C "$(dirname ${dir})" "$(basename ${dir})"
        log "  ✅ 配置备份: ${dir}"
    fi
done

# 4. 生成备份清单
log "生成备份清单..."
cat > "${BACKUP_PATH}/MANIFEST.txt" << EOF
备份时间: $(date)
备份路径: ${BACKUP_PATH}
保留期限: ${RETENTION_DAYS} 天
内容:
$(ls -lh "${BACKUP_PATH}" | grep -v MANIFEST)
EOF

# 5. 清理过期备份
log "清理 ${RETENTION_DAYS} 天前的旧备份..."
find "${BACKUP_DIR}" -maxdepth 1 -type d -name "20*" -mtime "+${RETENTION_DAYS}" -exec rm -rf {} \; 2>/dev/null || true

log "=== 备份完成: ${BACKUP_PATH} ==="
log "总大小: $(du -sh "${BACKUP_PATH}" | cut -f1)"
