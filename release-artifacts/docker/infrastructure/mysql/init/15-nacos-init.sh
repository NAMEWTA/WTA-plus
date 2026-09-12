#!/usr/bin/env bash
set -euo pipefail

SCHEMA_FILE="/docker-entrypoint-initdb.d/nacos/mysql-schema.sql"
EXPECTED_TABLES=10
EXPECTED_TABLE_NAMES="'config_info','config_info_gray','config_tags_relation','group_capacity','his_config_info','tenant_capacity','tenant_info','users','roles','permissions'"

info() { printf '[INFO] %s\n' "$*" >&2; }
error() { printf '[ERROR] %s\n' "$*" >&2; }

if [[ "${NACOS_MYSQL_INIT_ENABLED:-false}" != true ]]; then
  info "Nacos MySQL initialization is disabled"
  exit 0
fi

for key in NACOS_DB_NAME NACOS_DB_USER NACOS_DB_PASSWORD MYSQL_ROOT_PASSWORD; do
  value="${!key-}"
  [[ -n "${value}" && "${value}" != replace-* ]] || {
    error "${key} is missing or still uses a placeholder"
    exit 1
  }
done

[[ "${NACOS_DB_NAME}" == nacos ]] || { error "NACOS_DB_NAME must be nacos"; exit 1; }
[[ "${NACOS_DB_USER}" =~ ^[A-Za-z0-9_]+$ ]] || { error "invalid NACOS_DB_USER"; exit 1; }
[[ "${NACOS_DB_PASSWORD}" =~ ^[A-Za-z0-9._~-]+$ ]] || {
  error "NACOS_DB_PASSWORD contains unsupported characters"
  exit 1
}
[[ -f "${SCHEMA_FILE}" ]] || { error "Nacos schema not found"; exit 1; }

mysql_root() {
  MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql --protocol=socket -uroot \
    --default-character-set=utf8mb4 "$@"
}

mysql_root <<SQL
CREATE DATABASE IF NOT EXISTS \`${NACOS_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
SQL

table_count="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${NACOS_DB_NAME}'")"
if [[ "${table_count}" == 0 ]]; then
  info "Importing pinned Nacos 2.5.4 schema"
  mysql_root --database="${NACOS_DB_NAME}" --show-warnings < "${SCHEMA_FILE}"
elif [[ "${table_count}" != "${EXPECTED_TABLES}" ]]; then
  error "Nacos database has ${table_count} tables; expected 0 or ${EXPECTED_TABLES}"
  exit 1
fi

mysql_root <<SQL
CREATE USER IF NOT EXISTS '${NACOS_DB_USER}'@'%' IDENTIFIED BY '${NACOS_DB_PASSWORD}';
ALTER USER '${NACOS_DB_USER}'@'%' IDENTIFIED BY '${NACOS_DB_PASSWORD}';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '${NACOS_DB_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${NACOS_DB_NAME}\`.* TO '${NACOS_DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

table_count="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${NACOS_DB_NAME}'")"
[[ "${table_count}" == "${EXPECTED_TABLES}" ]] || {
  error "Nacos schema verification failed"
  exit 1
}
schema_table_count="$(mysql_root --batch --skip-column-names --execute \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='${NACOS_DB_NAME}' AND TABLE_NAME IN (${EXPECTED_TABLE_NAMES})")"
[[ "${schema_table_count}" == "${EXPECTED_TABLES}" ]] || {
  error "Nacos schema table-name verification failed"
  exit 1
}
info "Nacos MySQL initialization completed"
