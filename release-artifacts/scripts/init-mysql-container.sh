#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

ENV_FILE="${RELEASE_ROOT}/.env"
SQL_DIR="${RELEASE_ROOT}/docker/infrastructure/mysql/init"
MYSQL_CONTAINER="namewta-data-mysql"
# The ordered 10/20/30/40/50/60 baseline currently creates 125 tables.
EXPECTED_TABLES=125

readonly SQL_FILES=(
  "10-wta-base.sql"
  "20-ry-job.sql"
  "30-ry-workflow.sql"
  "40-ry-ai.sql"
  "50-namewta-ddl.sql"
  "60-namewta-dml.sql"
)

info() { printf '[INFO] %s\n' "$*" >&2; }
error() { printf '[ERROR] %s\n' "$*" >&2; }

usage() {
  cat <<'EOF'
Usage:
  init-mysql-container.sh [--env-file PATH] [--sql-dir PATH] [--container NAME]

Creates the single ry-namewta database in an existing MySQL container, imports
the six ordered SQL files, creates the application account, and updates the
runtime MinIO rows. The command refuses an existing database or application
account and removes only objects created by a failed run.
EOF
}

read_env_value() {
  local key="$1"
  local env_file="$2"
  awk -v wanted="${key}" '
    /^[[:space:]]*(#|$)/ { next }
    {
      line=$0
      sub(/^[[:space:]]*export[[:space:]]+/, "", line)
      name=line
      sub(/=.*/, "", name)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) {
        sub(/^[^=]*=/, "", line)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
        if (line ~ /^\047.*\047$/ || line ~ /^\".*\"$/) {
          line=substr(line, 2, length(line)-2)
        }
        print line
        exit
      }
    }
  ' "${env_file}"
}

require_value() {
  local key="$1"
  local value="$2"
  [[ -n "${value}" && "${value}" != replace-* ]] || {
    error "${key} is missing or still uses a placeholder"
    return 1
  }
}

mysql_root() {
  docker exec -i "${MYSQL_CONTAINER}" sh -lc \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 "$@"' sh "$@"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file|--sql-dir|--container)
      [[ -n "${2:-}" ]] || { error "$1 requires a value"; exit 2; }
      case "$1" in
        --env-file) ENV_FILE="$2" ;;
        --sql-dir) SQL_DIR="$2" ;;
        --container) MYSQL_CONTAINER="$2" ;;
      esac
      shift 2
      ;;
    -h|--help) usage; exit 0 ;;
    *) error "unknown argument: $1"; usage; exit 2 ;;
  esac
done

[[ -f "${ENV_FILE}" ]] || { error "env file not found: ${ENV_FILE}"; exit 1; }
command -v docker >/dev/null 2>&1 || { error "docker is required"; exit 1; }
docker inspect "${MYSQL_CONTAINER}" >/dev/null 2>&1 || {
  error "MySQL container not found: ${MYSQL_CONTAINER}"
  exit 1
}

database="$(read_env_value MYSQL_DATABASE "${ENV_FILE}")"
app_user="$(read_env_value MYSQL_APP_USER "${ENV_FILE}")"
app_password="$(read_env_value MYSQL_APP_PASSWORD "${ENV_FILE}")"
minio_user="$(read_env_value MINIO_ROOT_USER "${ENV_FILE}")"
minio_password="$(read_env_value MINIO_ROOT_PASSWORD "${ENV_FILE}")"
minio_endpoint="$(read_env_value MINIO_ENDPOINT "${ENV_FILE}")"
minio_bucket="$(read_env_value MINIO_BUCKET "${ENV_FILE}")"

require_value MYSQL_DATABASE "${database}"
require_value MYSQL_APP_USER "${app_user}"
require_value MYSQL_APP_PASSWORD "${app_password}"
require_value MINIO_ROOT_USER "${minio_user}"
require_value MINIO_ROOT_PASSWORD "${minio_password}"
require_value MINIO_ENDPOINT "${minio_endpoint}"
require_value MINIO_BUCKET "${minio_bucket}"

[[ "${database}" == ry-namewta ]] || {
  error "refusing database other than ry-namewta: ${database}"
  exit 1
}
[[ "${app_user}" =~ ^[A-Za-z0-9_]+$ ]] || { error "invalid MYSQL_APP_USER"; exit 1; }
[[ "${app_password}" =~ ^[A-Za-z0-9]+$ ]] || { error "MYSQL_APP_PASSWORD must be alphanumeric"; exit 1; }
[[ "${minio_user}" =~ ^[A-Za-z0-9._-]+$ ]] || { error "invalid MINIO_ROOT_USER"; exit 1; }
[[ "${minio_password}" =~ ^[A-Za-z0-9]+$ ]] || { error "MINIO_ROOT_PASSWORD must be alphanumeric"; exit 1; }
[[ "${minio_endpoint}" =~ ^[A-Za-z0-9.:-]+$ ]] || { error "invalid MINIO_ENDPOINT"; exit 1; }
[[ "${minio_bucket}" =~ ^[a-z0-9][a-z0-9.-]+$ ]] || { error "invalid MINIO_BUCKET"; exit 1; }

for filename in "${SQL_FILES[@]}"; do
  [[ -f "${SQL_DIR}/${filename}" ]] || {
    error "missing SQL file: ${SQL_DIR}/${filename}"
    exit 1
  }
done

database_exists="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${database}'")"
[[ "${database_exists}" == 0 ]] || {
  table_count="$(mysql_root --batch --skip-column-names --execute \
    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${database}'")"
  error "refusing existing database ${database} (tables=${table_count})"
  exit 1
}

user_exists="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM mysql.user WHERE User='${app_user}' AND Host='%'")"
[[ "${user_exists}" == 0 ]] || {
  error "refusing existing account ${app_user}@%"
  exit 1
}

created_database=0
created_user=0
rollback_failed_run() {
  local status=$?
  [[ ${status} -ne 0 ]] || return 0
  set +e
  if [[ ${created_database} -eq 1 ]]; then
    info "removing database created by failed run: ${database}"
    printf 'DROP DATABASE IF EXISTS `%s`;\n' "${database}" | mysql_root
  fi
  if [[ ${created_user} -eq 1 ]]; then
    info "removing account created by failed run: ${app_user}@%"
    printf "DROP USER IF EXISTS '%s'@'%%';\n" "${app_user}" | mysql_root
  fi
  exit "${status}"
}
trap rollback_failed_run EXIT

info "creating database ${database}"
printf 'CREATE DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;\n' \
  "${database}" | mysql_root
created_database=1

info "creating database-scoped application account ${app_user}@%"
printf "CREATE USER '%s'@'%%' IDENTIFIED BY '%s';\nGRANT ALL PRIVILEGES ON \`%s\`.* TO '%s'@'%%';\n" \
  "${app_user}" "${app_password}" "${database}" "${app_user}" | mysql_root
created_user=1

for filename in "${SQL_FILES[@]}"; do
  info "importing ${filename}"
  mysql_root --database="${database}" --show-warnings < "${SQL_DIR}/${filename}"
done

info "updating runtime MinIO configuration with a private default"
printf "UPDATE sys_oss_config SET access_key='%s', secret_key='%s', bucket_name='%s', endpoint='%s', is_https='N', access_policy='0', status=CASE WHEN config_key='minio' THEN 'Y' ELSE 'N' END WHERE config_key IN ('minio','image');\n" \
  "${minio_user}" "${minio_password}" "${minio_bucket}" "${minio_endpoint}" \
  | mysql_root --database="${database}"

table_count="$(mysql_root --batch --skip-column-names --database="${database}" --execute \
  'SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()')"
[[ "${table_count}" == "${EXPECTED_TABLES}" ]] || {
  error "unexpected table count: ${table_count}; expected ${EXPECTED_TABLES}"
  exit 1
}

oss_rows="$(mysql_root --batch --skip-column-names --database="${database}" --execute \
  "SELECT COUNT(*) FROM sys_oss_config WHERE config_key IN ('minio','image') AND bucket_name='${minio_bucket}' AND endpoint='${minio_endpoint}' AND access_policy='0'")"
[[ "${oss_rows}" == 2 ]] || { error "runtime MinIO rows were not updated as PRIVATE"; exit 1; }

private_default_count="$(mysql_root --batch --skip-column-names --database="${database}" --execute \
  "SELECT COUNT(*) FROM sys_oss_config WHERE status='Y' AND config_key='minio' AND access_policy='0'")"
default_count="$(mysql_root --batch --skip-column-names --database="${database}" --execute \
  "SELECT COUNT(*) FROM sys_oss_config WHERE status='Y'")"
[[ "${private_default_count}" == 1 && "${default_count}" == 1 ]] || {
  error "OSS configuration must have exactly one PRIVATE default named minio"
  exit 1
}

trap - EXIT
info "MySQL initialization completed: database=${database}, tables=${table_count}"
