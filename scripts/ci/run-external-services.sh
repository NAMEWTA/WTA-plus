#!/usr/bin/env bash
set -euo pipefail

workspace_root=$(git rev-parse --show-toplevel)
run_suffix="${GITHUB_RUN_ID:-local}-$$-$RANDOM"
run_suffix=${run_suffix//[^a-zA-Z0-9]/}
network="namewta-ci-$run_suffix"
redis_container="namewta-redis-$run_suffix"
mysql_container="namewta-mysql-$run_suffix"
minio_container="namewta-minio-$run_suffix"
redis_port=${NAMEWTA_CI_REDIS_PORT:-}
mysql_port=${NAMEWTA_CI_MYSQL_PORT:-}
minio_port=${NAMEWTA_CI_MINIO_PORT:-}
mysql_env_file=""
network_id=""
created_containers=()

for port in "$redis_port" "$mysql_port" "$minio_port"; do
  if [[ -n "$port" ]] && { [[ ! "$port" =~ ^[0-9]{1,5}$ ]] || (( 10#$port < 1 || 10#$port > 65535 )); }; then
    echo "CI service port must be empty or between 1 and 65535" >&2
    exit 2
  fi
done

cleanup() {
  local status=$?
  trap - EXIT
  if [[ -n "$mysql_env_file" && -f "$mysql_env_file" ]]; then
    rm -f "$mysql_env_file"
  fi
  # 只清理本轮成功create后取得的ID；名称冲突或start失败都不误删已有资源。
  if (( ${#created_containers[@]} > 0 )); then
    docker rm -fv "${created_containers[@]}" >/dev/null || status=1
  fi
  if [[ -n "$network_id" ]]; then
    docker network rm "$network_id" >/dev/null || status=1
  fi
  exit "$status"
}
trap cleanup EXIT

create_container() {
  local output_variable="$1" container_id
  shift
  container_id=$(docker create --label namewta.test.owner=external-services "$@")
  created_containers+=("$container_id")
  printf -v "$output_variable" '%s' "$container_id"
  docker start "$container_id" >/dev/null
}

network_id=$(docker network create --label namewta.test.owner=external-services "$network")
create_container redis_container --name "$redis_container" --network "$network_id" -p "127.0.0.1:$redis_port:6379" \
  redis:8.6.3 redis-server --save '' --appendonly no
create_container mysql_container --name "$mysql_container" --network "$network_id" -p "127.0.0.1:$mysql_port:3306" \
  -e MYSQL_ROOT_PASSWORD=namewta-ci mysql:8.4.9 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
create_container minio_container --name "$minio_container" --network "$network_id" --network-alias namewta-minio -p "127.0.0.1:$minio_port:9000" \
  -e MINIO_ROOT_USER=namewta -e MINIO_ROOT_PASSWORD=namewta123 \
  quay.io/minio/minio:RELEASE.2025-04-22T22-12-26Z server --address ':9000' /data

redis_port=$(docker port "$redis_container" 6379/tcp)
redis_port=${redis_port##*:}
mysql_port=$(docker port "$mysql_container" 3306/tcp)
mysql_port=${mysql_port##*:}
minio_port=$(docker port "$minio_container" 9000/tcp)
minio_port=${minio_port##*:}

for _ in {1..60}; do
  docker exec "$redis_container" redis-cli ping 2>/dev/null | grep -q PONG && break
  sleep 1
done
docker exec "$redis_container" redis-cli ping | grep -q PONG

for _ in {1..90}; do
  docker exec "$mysql_container" mysql --protocol=TCP -h 127.0.0.1 -uroot -pnamewta-ci --execute "SELECT 1" && break
  sleep 1
done
docker exec "$mysql_container" mysql --protocol=TCP -h 127.0.0.1 -uroot -pnamewta-ci --execute "SELECT 1"

mysql_env_file="$(mktemp "${TMPDIR:-/tmp}/namewta-ci-mysql.XXXXXX")"
chmod 0600 "$mysql_env_file"
printf '%s\n' \
  'MYSQL_DATABASE=wta-plus' \
  'MYSQL_APP_USER=namewta_ci_app' \
  'MYSQL_APP_PASSWORD=namewtaci123' \
  'MINIO_ROOT_USER=namewta' \
  'MINIO_ROOT_PASSWORD=namewta123' \
  'MINIO_ENDPOINT=namewta-minio:9000' \
  'MINIO_BUCKET=wta' > "$mysql_env_file"

bash "$workspace_root/release-artifacts/scripts/init-mysql-container.sh" \
  --container "$mysql_container" \
  --env-file "$mysql_env_file" \
  --sql-dir "$workspace_root/release-artifacts/docker/infrastructure/mysql/init"

docker exec "$mysql_container" sh -lc \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --execute "CREATE DATABASE namewta_ci CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"'

for _ in {1..60}; do
  curl --fail --silent "http://127.0.0.1:$minio_port/minio/health/ready" >/dev/null && break
  sleep 1
done
curl --fail --silent "http://127.0.0.1:$minio_port/minio/health/ready" >/dev/null

cd "$workspace_root/backend"
integration_tests=(
  RedisNotifyIdempotencyStoreIntegrationTest RedisOssUploadTicketStoreIntegrationTest
  NotifyMonitorMySqlIntegrationTest MinioOssClientIntegrationTest
  BusinessMenuRetirementMySqlIntegrationTest ThirdSchemaMySqlIntegrationTest ThirdRedisIntegrationTest
  AiRetirementMySqlIntegrationTest
)
test_selector=$(IFS=,; echo "${integration_tests[*]}")
test_started_ns=$(node -e 'process.stdout.write(String(BigInt(Date.now()) * 1000000n))')
./mvnw -Pdev -pl wta-admin -am test \
  -Dtest="$test_selector" \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dai.retirement.mysql.url="jdbc:mysql://127.0.0.1:$mysql_port/wta-plus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  -Dai.retirement.mysql.username=root \
  -Dai.retirement.mysql.password=namewta-ci \
  -Dnotify.redis.integration.port="$redis_port" \
  -Doss.upload.redis.integration.port="$redis_port" \
  -Dthird.redis.integration.port="$redis_port" \
  -Dnotify.mysql.integration.url="jdbc:mysql://127.0.0.1:$mysql_port/namewta_ci?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  -Dnotify.mysql.integration.username=root \
  -Dnotify.mysql.integration.password=namewta-ci \
  -Dthird.mysql.integration.url="jdbc:mysql://127.0.0.1:$mysql_port/namewta_ci?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  -Dthird.mysql.integration.username=root \
  -Dthird.mysql.integration.password=namewta-ci \
  -Dthird.mysql.integration.exact-schema=true \
  -Doss.minio.integration.endpoint="http://127.0.0.1:$minio_port" \
  -Doss.minio.integration.access-key=namewta \
  -Doss.minio.integration.secret-key=namewta123 \
  -Dnamewta.sql.root="$workspace_root/release-artifacts/docker/infrastructure/mysql/init"
node "$workspace_root/scripts/ci/verify-external-tests.mjs" \
  "$workspace_root/backend" "$test_started_ns" "${integration_tests[@]}"
