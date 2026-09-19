#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${RELEASE_ROOT}/.." && pwd)"
APP_JAR="${NACOS_E2E_APP_JAR:-${REPO_ROOT}/backend/wta-admin/target/wta-admin.jar}"
WORK_PARENT="${NACOS_E2E_WORK_PARENT:-${TMPDIR:-/tmp}}"
KEEP_ON_FAILURE=false

info() { printf '[INFO] %s\n' "$*" >&2; }
error() { printf '[ERROR] %s\n' "$*" >&2; }
die() { error "$*"; exit 1; }

if [[ "${1:-}" == "--keep-on-failure" ]]; then
  KEEP_ON_FAILURE=true
  shift
fi
[[ $# -eq 0 ]] || die "usage: verify-nacos.sh [--keep-on-failure]"
[[ "${NACOS_E2E_CONFIRM:-}" == 1 ]] || die "set NACOS_E2E_CONFIRM=1 to create an isolated test environment"
command -v docker >/dev/null || die "docker is required"
command -v node >/dev/null || die "Node.js is required"
command -v openssl >/dev/null || die "openssl is required"
[[ -f "${APP_JAR}" ]] || die "application jar not found; set NACOS_E2E_APP_JAR"
[[ -f "${RELEASE_ROOT}/docker/infrastructure/mysql/init/60-cde-nacos.sql" ]] || die "MySQL init assets are incomplete"

mkdir -p "${WORK_PARENT}"
WORK_ROOT="$(mktemp -d "${WORK_PARENT%/}/namewta-nacos-e2e-XXXXXX")"
RUN_ID="$(basename "${WORK_ROOT}" | sed 's/^namewta-nacos-e2e-//')"
[[ "${RUN_ID}" =~ ^[A-Za-z0-9]+$ ]] || die "unsafe generated run id"
PREFIX="namewta-nacos-e2e-${RUN_ID,,}"
NETWORK="${PREFIX}-network"
IMAGE="${PREFIX}-admin:local"
MYSQL_CONTAINER="${PREFIX}-mysql"
REDIS_CONTAINER="${PREFIX}-redis"
NACOS_CONTAINER="${PREFIX}-nacos"
BASE_CONTAINER="${PREFIX}-base"
APP1_CONTAINER="${PREFIX}-app1"
APP2_CONTAINER="${PREFIX}-app2"
CONTAINERS=("${BASE_CONTAINER}" "${APP1_CONTAINER}" "${APP2_CONTAINER}" "${NACOS_CONTAINER}" "${REDIS_CONTAINER}" "${MYSQL_CONTAINER}")
SUCCESS=false

cleanup() {
  local exit_code=$?
  if [[ "${SUCCESS}" != true && "${KEEP_ON_FAILURE}" == true ]]; then
    error "validation failed; isolated resources retained at ${WORK_ROOT} with prefix ${PREFIX}"
    return "${exit_code}"
  fi
  [[ "${PREFIX}" =~ ^namewta-nacos-e2e-[a-z0-9]+$ ]] || { error "refusing unsafe cleanup"; return 1; }
  info "cleaning isolated test resources"
  docker rm -f "${CONTAINERS[@]}" >/dev/null 2>&1 || true
  docker network rm "${NETWORK}" >/dev/null 2>&1 || true
  docker image rm "${IMAGE}" >/dev/null 2>&1 || true
  [[ "$(basename "${WORK_ROOT}")" == namewta-nacos-e2e-* ]] && rm -rf -- "${WORK_ROOT}"
  return "${exit_code}"
}
trap cleanup EXIT

random_hex() { openssl rand -hex "$1"; }
file_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}
MYSQL_ROOT_PASSWORD="$(random_hex 20)"
MYSQL_APP_PASSWORD="$(random_hex 20)"
NACOS_DB_PASSWORD="$(random_hex 20)"
REDIS_PASSWORD="$(random_hex 20)"
NACOS_PASSWORD="A$(random_hex 12)a1"
MONITOR_USERNAME="monitor_${RUN_ID,,}"
MONITOR_PASSWORD="M$(random_hex 12)a1"
NACOS_AUTH_TOKEN="$(openssl rand -base64 48 | tr -d '\n')"
NACOS_AUTH_IDENTITY_KEY="identity_$(random_hex 8)"
NACOS_AUTH_IDENTITY_VALUE="$(random_hex 20)"
NAMESPACE="e2e-${RUN_ID,,}"

install -m 0600 /dev/null "${WORK_ROOT}/runtime.env"
{
  printf 'run_id=%s\n' "${RUN_ID}"
  printf 'namespace=%s\n' "${NAMESPACE}"
  printf 'jar_sha256=%s\n' "$(file_sha256 "${APP_JAR}")"
} >"${WORK_ROOT}/runtime.env"

wait_container() {
  local container=$1 expected=${2:-healthy} attempts=${3:-90}
  local current
  for ((i = 1; i <= attempts; i++)); do
    current="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container}" 2>/dev/null || true)"
    [[ "${current}" == "${expected}" ]] && return 0
    [[ "${current}" == exited || "${current}" == dead ]] && break
    sleep 2
  done
  docker logs --tail 80 "${container}" 2>&1 | sed -E 's/(password|token|secret)([^[:space:]]*)/\1=[REDACTED]/Ig' >&2 || true
  die "${container} did not become ${expected}"
}

free_port() {
  node -e "const server=require('node:net').createServer();server.listen(0,'127.0.0.1',()=>{console.log(server.address().port);server.close()})"
}

common_app_args() {
  printf '%s\0' \
    --network "${NETWORK}" \
    -e TZ=Asia/Shanghai \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e JAVA_OPTS=-Xms256m\ -Xmx512m \
    -e SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL="jdbc:mysql://${MYSQL_CONTAINER}:3306/wta-plus?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true" \
    -e SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=namewta_app \
    -e SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD="${MYSQL_APP_PASSWORD}" \
    -e SPRING_DATA_REDIS_HOST="${REDIS_CONTAINER}" \
    -e SPRING_DATA_REDIS_PORT=6379 \
    -e SPRING_DATA_REDIS_PASSWORD="${REDIS_PASSWORD}" \
    -e SPRING_BOOT_ADMIN_CLIENT_ENABLED=false \
    -e SPRING_BOOT_ADMIN_CLIENT_USERNAME="${MONITOR_USERNAME}" \
    -e SPRING_BOOT_ADMIN_CLIENT_PASSWORD="${MONITOR_PASSWORD}" \
    -e SNAIL_JOB_ENABLED=false \
    -e NACOS_CONFIG_SERVER_ADDR="${NACOS_CONTAINER}:8848" \
    -e NACOS_CONFIG_USERNAME=nacos \
    -e NACOS_CONFIG_PASSWORD="${NACOS_PASSWORD}" \
    -e NACOS_CONFIG_NAMESPACE_PROD="${NAMESPACE}"
}

start_app() {
  local container=$1 nacos_enabled=$2 host_port=$3 captcha_override=${4:-}
  local -a args=()
  while IFS= read -r -d '' item; do args+=("${item}"); done < <(common_app_args)
  args+=(--name "${container}" -p "127.0.0.1:${host_port}:8080" -e "NACOS_CONFIG_ENABLED=${nacos_enabled}")
  [[ -n "${captcha_override}" ]] && args+=(-e "CAPTCHA_ENABLE=${captcha_override}")
  docker run -d "${args[@]}" --health-cmd="bash -c ': >/dev/tcp/127.0.0.1/8080'" \
    --health-interval=5s --health-timeout=5s --health-retries=36 --health-start-period=45s "${IMAGE}" >/dev/null
  wait_container "${container}" healthy 75
}

info "building the isolated application image"
mkdir -p "${WORK_ROOT}/image" "${WORK_ROOT}/mysql" "${WORK_ROOT}/nacos/logs" "${WORK_ROOT}/nacos/data" "${WORK_ROOT}/redis"
cp "${APP_JAR}" "${WORK_ROOT}/image/app.jar"
cp "${RELEASE_ROOT}/docker/backend/images/wta-admin/Dockerfile" "${WORK_ROOT}/image/Dockerfile"
docker build --quiet -t "${IMAGE}" "${WORK_ROOT}/image" >/dev/null
docker network create "${NETWORK}" >/dev/null

info "starting fresh MySQL with the ordered application and Nacos schemas"
docker run -d --name "${MYSQL_CONTAINER}" --network "${NETWORK}" \
  -e TZ=Asia/Shanghai \
  -e MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}" \
  -e MYSQL_DATABASE=wta-plus \
  -e MYSQL_USER=namewta_app \
  -e MYSQL_PASSWORD="${MYSQL_APP_PASSWORD}" \
  -e NACOS_MYSQL_INIT_ENABLED=true \
  -e NACOS_DB_NAME=nacos \
  -e NACOS_DB_USER=nacos \
  -e NACOS_DB_PASSWORD="${NACOS_DB_PASSWORD}" \
  -v "${WORK_ROOT}/mysql:/var/lib/mysql" \
  -v "${RELEASE_ROOT}/docker/infrastructure/mysql/init:/docker-entrypoint-initdb.d:ro" \
  --health-cmd='mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' \
  --health-interval=5s --health-timeout=5s --health-retries=60 --health-start-period=45s \
  mysql:8.4.9 --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci \
  --explicit_defaults_for_timestamp=true --lower_case_table_names=1 >/dev/null
wait_container "${MYSQL_CONTAINER}" healthy 90

info "starting isolated Redis and official Nacos 2.5.4"
docker run -d --name "${REDIS_CONTAINER}" --network "${NETWORK}" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD}" -v "${WORK_ROOT}/redis:/data" \
  --health-cmd='redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping | grep -q PONG' \
  --health-interval=5s --health-timeout=5s --health-retries=30 \
  redis:8.6.3 redis-server --appendonly yes --requirepass "${REDIS_PASSWORD}" >/dev/null
wait_container "${REDIS_CONTAINER}" healthy 45

NACOS_PORT="$(free_port)"
docker run -d --name "${NACOS_CONTAINER}" --network "${NETWORK}" -p "127.0.0.1:${NACOS_PORT}:8848" \
  -e TZ=Asia/Shanghai -e MODE=standalone -e PREFER_HOST_MODE=hostname \
  -e SPRING_DATASOURCE_PLATFORM=mysql -e MYSQL_SERVICE_HOST="${MYSQL_CONTAINER}" -e MYSQL_SERVICE_PORT=3306 \
  -e MYSQL_SERVICE_DB_NAME=nacos -e MYSQL_SERVICE_USER=nacos -e MYSQL_SERVICE_PASSWORD="${NACOS_DB_PASSWORD}" \
  -e 'MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' \
  -e NACOS_AUTH_ENABLE=true -e NACOS_AUTH_TOKEN="${NACOS_AUTH_TOKEN}" \
  -e NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY}" -e NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE}" \
  -e NACOS_AUTH_USER_AGENT_AUTH_WHITE_ENABLE=false -e JVM_XMS=512m -e JVM_XMX=512m -e JVM_XMN=256m \
  -v "${WORK_ROOT}/nacos/logs:/home/nacos/logs" -v "${WORK_ROOT}/nacos/data:/home/nacos/data" \
  --health-cmd='curl --fail --silent http://127.0.0.1:8848/nacos/v1/console/health/readiness >/dev/null' \
  --health-interval=5s --health-timeout=5s --health-retries=48 --health-start-period=60s \
  nacos/nacos-server:v2.5.4 >/dev/null
wait_container "${NACOS_CONTAINER}" healthy 90
NACOS_URL="http://127.0.0.1:${NACOS_PORT}"

info "initializing the one-time Nacos administrator password and namespace"
init_status="$(curl --silent --output /dev/null --write-out '%{http_code}' -X POST \
  --data-urlencode "password=${NACOS_PASSWORD}" "${NACOS_URL}/nacos/v1/auth/users/admin")"
[[ "${init_status}" == 200 ]] || die "Nacos administrator initialization failed with HTTP ${init_status}"
login_json="$(curl --fail --silent -X POST --data-urlencode username=nacos --data-urlencode "password=${NACOS_PASSWORD}" \
  "${NACOS_URL}/nacos/v1/auth/login")"
ACCESS_TOKEN="$(printf '%s' "${login_json}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
[[ -n "${ACCESS_TOKEN}" ]] || die "Nacos login did not return a token"
namespace_result="$(curl --fail --silent -X POST \
  --data-urlencode "customNamespaceId=${NAMESPACE}" --data-urlencode "namespaceName=${NAMESPACE}" \
  --data-urlencode 'namespaceDesc=isolated runtime verification' --data-urlencode "accessToken=${ACCESS_TOKEN}" \
  "${NACOS_URL}/nacos/v1/console/namespaces")"
[[ "${namespace_result}" == true ]] || die "Nacos namespace creation failed"
unset login_json ACCESS_TOKEN namespace_result

info "proving the base application starts with the Nacos client disabled"
BASE_PORT="$(free_port)"
start_app "${BASE_CONTAINER}" false "${BASE_PORT}"
base_info="$(curl --fail --silent --user "${MONITOR_USERNAME}:${MONITOR_PASSWORD}" "http://127.0.0.1:${BASE_PORT}/actuator/info")"
[[ "${base_info}" != *'nacosConfig'* ]] || die "disabled base application unexpectedly exposes Nacos state"
docker rm -f "${BASE_CONTAINER}" >/dev/null

info "starting two independently observable Nacos-enabled instances"
APP1_PORT="$(free_port)"
start_app "${APP1_CONTAINER}" true "${APP1_PORT}" true
APP2_PORT="$(free_port)"
start_app "${APP2_CONTAINER}" true "${APP2_PORT}"

NACOS_RUNTIME_E2E=1 \
NACOS_E2E_NACOS_URL="${NACOS_URL}" \
NACOS_E2E_APP1_URL="http://127.0.0.1:${APP1_PORT}" \
NACOS_E2E_APP2_URL="http://127.0.0.1:${APP2_PORT}" \
NACOS_E2E_APP1_CONTAINER="${APP1_CONTAINER}" \
NACOS_E2E_APP2_CONTAINER="${APP2_CONTAINER}" \
NACOS_E2E_NACOS_CONTAINER="${NACOS_CONTAINER}" \
NACOS_E2E_NACOS_USERNAME=nacos \
NACOS_E2E_NACOS_PASSWORD="${NACOS_PASSWORD}" \
NACOS_E2E_NAMESPACE="${NAMESPACE}" \
NACOS_E2E_MONITOR_USERNAME="${MONITOR_USERNAME}" \
NACOS_E2E_MONITOR_PASSWORD="${MONITOR_PASSWORD}" \
node --test "${RELEASE_ROOT}/tests/nacos-runtime.e2e.mjs"

if [[ -n "${NACOS_E2E_BROWSER_COMMAND:-}" ]]; then
  info "running the explicitly configured browser verification"
  NACOS_E2E_NACOS_URL="${NACOS_URL}" bash -c "${NACOS_E2E_BROWSER_COMMAND}"
fi

info "scanning isolated container logs for generated secrets"
LOG_SCAN="${WORK_ROOT}/container-logs.txt"
install -m 0600 /dev/null "${LOG_SCAN}"
for container in "${CONTAINERS[@]}"; do
  docker logs "${container}" >>"${LOG_SCAN}" 2>&1 || true
done
for secret in "${MYSQL_ROOT_PASSWORD}" "${MYSQL_APP_PASSWORD}" "${NACOS_DB_PASSWORD}" "${REDIS_PASSWORD}" \
  "${NACOS_PASSWORD}" "${MONITOR_PASSWORD}" "${NACOS_AUTH_TOKEN}" "${NACOS_AUTH_IDENTITY_VALUE}"; do
  if grep -Fq -- "${secret}" "${LOG_SCAN}"; then
    die "a generated secret was found in container logs"
  fi
done

SUCCESS=true
info "isolated Nacos release verification passed"
