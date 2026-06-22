#!/bin/bash
# ============================================================
# UAV Path Planning System — 自动化备份脚本
# 用法: bash deployments/backup/backup.sh [mysql|nacos|all]
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-${SCRIPT_DIR}/data}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
NACOS_URL="${NACOS_URL:-http://localhost:8848}"

mkdir -p "$BACKUP_DIR"

backup_mysql() {
    echo "[$(date '+%H:%M:%S')] Starting MySQL backup..."
    if [ -z "$MYSQL_PASSWORD" ]; then
        echo "ERROR: MYSQL_PASSWORD is not set. Export it or configure in environment."
        return 1
    fi
    BACKUP_FILE="${BACKUP_DIR}/mysql-${TIMESTAMP}.sql.gz"
    mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" \
        -p"$MYSQL_PASSWORD" --all-databases --single-transaction \
        --routines --triggers | gzip > "$BACKUP_FILE"
    echo "[$(date '+%H:%M:%S')] MySQL backup: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
}

backup_nacos() {
    echo "[$(date '+%H:%M:%S')] Starting Nacos config backup..."
    BACKUP_FILE="${BACKUP_DIR}/nacos-${TIMESTAMP}.json"
    curl -s "${NACOS_URL}/nacos/v1/cs/configs?dataId=&group=&tenant=" \
        -o "$BACKUP_FILE" || echo "WARN: Nacos config backup failed (server may be down)"
    echo "[$(date '+%H:%M:%S')] Nacos backup: $BACKUP_FILE"
}

cleanup_old_backups() {
    echo "[$(date '+%H:%M:%S')] Cleaning backups older than ${RETENTION_DAYS} days..."
    find "$BACKUP_DIR" -name "mysql-*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete
    find "$BACKUP_DIR" -name "nacos-*.json" -mtime +"${RETENTION_DAYS}" -delete
    echo "[$(date '+%H:%M:%S')] Cleanup complete. Remaining backups: $(find "$BACKUP_DIR" -type f | wc -l)"
}

case "${1:-all}" in
    mysql)
        backup_mysql
        cleanup_old_backups
        ;;
    nacos)
        backup_nacos
        cleanup_old_backups
        ;;
    all)
        backup_mysql
        backup_nacos
        cleanup_old_backups
        ;;
    *)
        echo "Usage: $0 [mysql|nacos|all]"
        exit 1
        ;;
esac

echo "[$(date '+%H:%M:%S')] Backup completed successfully."
