#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${RELEASE_ROOT}/.env"
SCHEMA_FILE="${RELEASE_ROOT}/docker/infrastructure/mysql/init/nacos/mysql-schema.sql"
MYSQL_CONTAINER="namewta-mysql"
EXPECTED_TABLES=10
EXPECTED_TABLE_NAMES="'config_info','config_info_gray','config_tags_relation','group_capacity','his_config_info','tenant_capacity','tenant_info','users','roles','permissions'"

info() { printf '[INFO] %s\n' "$*" >&2; }
error() { printf '[ERROR] %s\n' "$*" >&2; }

usage() {
  cat <<'EOF'
Usage:
  init-nacos-mysql-container.sh [--env-file PATH] [--schema-file PATH] [--container NAME]

Idempotently creates or verifies the independent Nacos database and its
least-privilege account in an existing NAMEWTA MySQL container. Existing
non-Nacos databases are never modified or removed.
EOF
}

read_env_value() {
  local key="$1"
  local env_file="$2"
  local current_value="${!key-}"
  if [[ -n "${current_value}" ]]; then
    printf '%s' "${current_value}"
    return 0
  fi
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
    --env-file|--schema-file|--container)
      [[ -n "${2:-}" ]] || { error "$1 requires a value"; exit 2; }
      case "$1" in
        --env-file) ENV_FILE="$2" ;;
        --schema-file) SCHEMA_FILE="$2" ;;
        --container) MYSQL_CONTAINER="$2" ;;
      esac
      shift 2
      ;;
    -h|--help) usage; exit 0 ;;
    *) error "unknown argument: $1"; usage; exit 2 ;;
  esac
done

[[ -f "${ENV_FILE}" ]] || { error "env file not found: ${ENV_FILE}"; exit 1; }
[[ -f "${SCHEMA_FILE}" ]] || { error "schema file not found: ${SCHEMA_FILE}"; exit 1; }
command -v docker >/dev/null 2>&1 || { error "docker is required"; exit 1; }
docker inspect "${MYSQL_CONTAINER}" >/dev/null 2>&1 || {
  error "MySQL container not found: ${MYSQL_CONTAINER}"
  exit 1
}

database="$(read_env_value NACOS_DB_NAME "${ENV_FILE}")"
db_user="$(read_env_value NACOS_DB_USER "${ENV_FILE}")"
db_password="$(read_env_value NACOS_DB_PASSWORD "${ENV_FILE}")"
require_value NACOS_DB_NAME "${database}"
require_value NACOS_DB_USER "${db_user}"
require_value NACOS_DB_PASSWORD "${db_password}"

[[ "${database}" == nacos ]] || { error "NACOS_DB_NAME must be nacos"; exit 1; }
[[ "${db_user}" =~ ^[A-Za-z0-9_]+$ ]] || { error "invalid NACOS_DB_USER"; exit 1; }
[[ "${db_password}" =~ ^[A-Za-z0-9._~-]+$ ]] || {
  error "NACOS_DB_PASSWORD contains unsupported characters"
  exit 1
}

database_exists="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${database}'")"
if [[ "${database_exists}" == 0 ]]; then
  info "creating independent Nacos database"
  printf 'CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;\n' \
    "${database}" | mysql_root
fi

table_count="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${database}'")"
if [[ "${table_count}" == 0 ]]; then
  info "importing pinned Nacos 2.5.4 schema"
  mysql_root --database="${database}" --show-warnings < "${SCHEMA_FILE}"
elif [[ "${table_count}" != "${EXPECTED_TABLES}" ]]; then
  error "Nacos database has ${table_count} tables; expected 0 or ${EXPECTED_TABLES}"
  exit 1
fi

printf "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s';\nALTER USER '%s'@'%%' IDENTIFIED BY '%s';\nREVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s'@'%%';\nGRANT SELECT, INSERT, UPDATE, DELETE ON \`%s\`.* TO '%s'@'%%';\nFLUSH PRIVILEGES;\n" \
  "${db_user}" "${db_password}" "${db_user}" "${db_password}" "${db_user}" "${database}" "${db_user}" \
  | mysql_root

table_count="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${database}'")"
[[ "${table_count}" == "${EXPECTED_TABLES}" ]] || {
  error "Nacos schema verification failed"
  exit 1
}
schema_table_count="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${database}' AND TABLE_NAME IN (${EXPECTED_TABLE_NAMES})")"
[[ "${schema_table_count}" == "${EXPECTED_TABLES}" ]] || {
  error "Nacos schema table-name verification failed"
  exit 1
}
info "Nacos MySQL initialization completed: database=${database}, tables=${table_count}"
