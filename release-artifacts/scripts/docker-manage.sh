#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCKER_ROOT="${RELEASE_ROOT}/docker"
ENV_FILE="${RELEASE_ENV_FILE:-${RELEASE_ROOT}/.env}"

readonly START_ORDER=(infrastructure observability backend frontend)
readonly STOP_ORDER=(frontend backend observability infrastructure)

info() { printf '[INFO] %s\n' "$*" >&2; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
error() { printf '[ERROR] %s\n' "$*" >&2; }

usage() {
  cat <<'EOF'
Usage:
  docker-manage.sh config <category|all> [--profile <name>]
  docker-manage.sh up <category|all> [--profile <name>]
  docker-manage.sh down <category|all> [--profile <name>]
  docker-manage.sh ps <category|all> [--profile <name>]
  docker-manage.sh logs <category> [service]
  docker-manage.sh build-images
  docker-manage.sh menu

Categories: infrastructure, observability, backend, frontend
Profiles: ai (Elasticsearch), tls (LB HTTPS), metrics (Prometheus/Linux exporters)

通过 RELEASE_ENV_FILE 指定 env 文件，默认 release-artifacts/.env。
EOF
}

require_docker() {
  command -v docker >/dev/null 2>&1 || {
    error "缺少 docker 命令"
    return 1
  }
  docker compose version >/dev/null 2>&1 || {
    error "docker compose 不可用"
    return 1
  }
  [[ -f "${ENV_FILE}" ]] || {
    error "缺少运行配置 ${ENV_FILE}，请从 .env.example 创建并填入真实值"
    return 1
  }
}

validate_category() {
  case "$1" in
    all|infrastructure|observability|backend|frontend) ;;
    *) error "未知分类: $1"; return 1 ;;
  esac
}

compose_file() {
  case "$1" in
    infrastructure) printf '%s' "${DOCKER_ROOT}/docker-compose-infrastructure.yml" ;;
    observability) printf '%s' "${DOCKER_ROOT}/docker-compose-observability.yml" ;;
    backend) printf '%s' "${DOCKER_ROOT}/docker-compose-backend.yml" ;;
    frontend) printf '%s' "${DOCKER_ROOT}/docker-compose-frontend.yml" ;;
    *) error "未知分类: $1"; return 1 ;;
  esac
}

env_value() {
  local key="$1"
  local fallback="$2"
  local value
  value="$(awk -F= -v wanted="${key}" '$1 == wanted { sub(/^[^=]*=/, ""); print; exit }' "${ENV_FILE}")"
  printf '%s' "${value:-${fallback}}"
}

ensure_network() {
  local network_name
  network_name="$(env_value NAMEWTA_NETWORK namewta-network)"
  if ! docker network inspect "${network_name}" >/dev/null 2>&1; then
    info "创建共享网络: ${network_name}"
    docker network create "${network_name}" >/dev/null
  fi
}

compose_command() {
  local category="$1"
  shift
  docker compose \
    --env-file "${ENV_FILE}" \
    -p "namewta-${category}" \
    -f "$(compose_file "${category}")" \
    "$@"
}

run_for_category() {
  local action="$1"
  local category="$2"
  local profile="${3:-}"
  local args=()
  if [[ -n "${profile}" ]]; then
    args+=(--profile "${profile}")
  fi

  info "${action}: ${category}${profile:+ (profile=${profile})}"
  case "${action}" in
    config) compose_command "${category}" "${args[@]}" config --quiet ;;
    up) compose_command "${category}" "${args[@]}" up -d --remove-orphans ;;
    down) compose_command "${category}" "${args[@]}" down --remove-orphans ;;
    ps) compose_command "${category}" "${args[@]}" ps ;;
    *) error "不支持的操作: ${action}"; return 1 ;;
  esac
}

run_category_or_all() {
  local action="$1"
  local category="$2"
  local profile="${3:-}"
  local item
  local order=("${START_ORDER[@]}")

  validate_category "${category}"
  [[ "${action}" != down ]] || order=("${STOP_ORDER[@]}")
  if [[ "${category}" == all ]]; then
    for item in "${order[@]}"; do
      run_for_category "${action}" "${item}" "${profile}"
    done
  else
    run_for_category "${action}" "${category}" "${profile}"
  fi
}

build_images() {
  info "构建后端应用镜像"
  compose_command backend build
  info "构建 Monitor Admin 镜像"
  compose_command observability build namewta-monitor-admin
}

show_logs() {
  local category="$1"
  local service="${2:-}"
  validate_category "${category}"
  [[ "${category}" != all ]] || {
    error "logs 需要指定单个分类"
    return 1
  }
  if [[ -n "${service}" ]]; then
    compose_command "${category}" logs -f --tail 200 "${service}"
  else
    compose_command "${category}" logs -f --tail 200
  fi
}

interactive_menu() {
  local choice category
  while true; do
    cat <<'EOF'

NAMEWTA Docker 管理
  1) 校验全部 Compose
  2) 启动全部服务
  3) 停止全部服务
  4) 查看全部状态
  5) 构建应用镜像
  6) 启动单个分类
  7) 停止单个分类
  0) 退出
EOF
    read -r -p '请选择 [0-7]: ' choice || return 0
    case "${choice}" in
      1) run_category_or_all config all ;;
      2) ensure_network; run_category_or_all up all ;;
      3) run_category_or_all down all ;;
      4) run_category_or_all ps all ;;
      5) build_images ;;
      6)
        read -r -p '分类 [infrastructure/observability/backend/frontend]: ' category
        ensure_network
        run_category_or_all up "${category}"
        ;;
      7)
        read -r -p '分类 [infrastructure/observability/backend/frontend]: ' category
        run_category_or_all down "${category}"
        ;;
      0) return 0 ;;
      *) warn "无效选择: ${choice}" ;;
    esac
  done
}

main() {
  local action="${1:-menu}"
  local category=all service=""
  local profile=""
  [[ $# -eq 0 ]] || shift

  case "${action}" in
    config|up|down|ps|logs)
      category="${1:-}"
      [[ -n "${category}" ]] || { error "${action} 需要分类参数"; usage; return 2; }
      shift
      ;;
  esac

  if [[ "${action}" == logs ]]; then
    service="${1:-}"
    [[ $# -eq 0 ]] || shift
    [[ $# -eq 0 ]] || { error "logs 参数过多: $1"; usage; return 2; }
  fi

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --profile)
        [[ -n "${2:-}" ]] || { error "--profile 缺少值"; return 2; }
        profile="$2"
        shift 2
        ;;
      -h|--help) usage; return 0 ;;
      *) error "未知参数: $1"; usage; return 2 ;;
    esac
  done

  case "${action}" in
    -h|--help|help) usage; return 0 ;;
    menu) require_docker; interactive_menu ;;
    config|up|down|ps)
      require_docker
      [[ "${action}" != up ]] || ensure_network
      run_category_or_all "${action}" "${category}" "${profile}"
      ;;
    build-images) require_docker; ensure_network; build_images ;;
    logs) require_docker; show_logs "${category}" "${service}" ;;
    *) error "未知命令: ${action}"; usage; return 2 ;;
  esac
}

main "$@"
