#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${RELEASE_ROOT}/.." && pwd)"
BACKEND_ROOT="${REPO_ROOT}/backend"
FRONTEND_ROOT="${REPO_ROOT}/frontend"
BUILDS_DIR="${RELEASE_ROOT}/builds"
BUNDLES_DIR="${RELEASE_ROOT}/bundles"
DEFAULT_ENV_FILE="${RELEASE_ROOT}/.env"

readonly BACKEND_ARTIFACTS=(
  "wta-admin|wta-admin/target/wta-admin.jar"
  "wta-monitor-admin|wta-extend/wta-monitor-admin/target/wta-monitor-admin.jar"
  "wta-snailjob-server|wta-extend/wta-snailjob-server/target/wta-snailjob-server.jar"
  "wta-snailai-server|wta-extend/wta-snailai-server/target/wta-snailai-server.jar"
)

info() { printf '[INFO] %s\n' "$*" >&2; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
error() { printf '[ERROR] %s\n' "$*" >&2; }

usage() {
  cat <<'EOF'
Usage:
  release-manage.sh build --target <frontend|backend|all> --env <dev|prod>
                          [--bundle <full|core>] [--env-file <path>]
  release-manage.sh backup
  release-manage.sh stage-mysql
  release-manage.sh stage --env <dev|prod>
  release-manage.sh bundle --env <dev|prod>
  release-manage.sh menu

默认 env 文件为 release-artifacts/.env。真实密码不会写入构建产物清单。
EOF
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    error "缺少必要命令: $1"
    return 1
  }
}

validate_env_name() {
  case "$1" in
    dev|prod) ;;
    *) error "不支持的构建环境: $1（当前项目仅支持 dev/prod）"; return 1 ;;
  esac
}

validate_target() {
  case "$1" in
    frontend|backend|all) ;;
    *) error "不支持的构建目标: $1"; return 1 ;;
  esac
}

validate_bundle() {
  case "$1" in
    full|core) ;;
    *) error "不支持的后端 bundle: $1"; return 1 ;;
  esac
}

read_env_value() {
  local key="$1"
  local env_file="$2"
  local current_value="${!key-}"
  local value

  if [[ -n "${current_value}" ]]; then
    printf '%s' "${current_value}"
    return 0
  fi
  [[ -f "${env_file}" ]] || return 1

  value="$({
    awk -v wanted="${key}" '
      /^[[:space:]]*(#|$)/ { next }
      {
        line=$0
        sub(/^[[:space:]]*export[[:space:]]+/, "", line)
        split(line, pair, "=")
        name=pair[1]
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
  } || true)"
  [[ -n "${value}" ]] || return 1
  printf '%s' "${value}"
}

app_env_key() {
  printf '%s_PREFIX' "$1" | tr '[:lower:]-' '[:upper:]_'
}

app_prefix() {
  local app="$1"
  local env_file="$2"
  local key
  local prefix

  key="$(app_env_key "${app}")"
  prefix="$(read_env_value "${key}" "${env_file}" || true)"
  [[ -n "${prefix}" ]] || {
    error "缺少 ${key}；请在 ${env_file} 或当前环境中配置"
    return 1
  }
  [[ "${prefix}" != replace-* ]] || {
    error "${key} 仍是占位值，请设置真实路径前缀"
    return 1
  }
  [[ "${prefix}" =~ ^[A-Za-z0-9][A-Za-z0-9-]*$ ]] || {
    error "${key}=${prefix} 非法：只允许字母、数字和连字符，且不含首尾斜杠"
    return 1
  }
  case "${prefix}" in
    admin|monitor|snail-job|snail-ai|dev-api|prod-api|actuator)
      error "${key}=${prefix} 与保留路由冲突"
      return 1
      ;;
  esac
  printf '%s' "${prefix}"
}

copy_dir() {
  local source_dir="$1"
  local target_dir="$2"
  mkdir -p "${target_dir}"
  tar -C "${source_dir}" -cf - . | tar -C "${target_dir}" -xf -
}

remove_tree() {
  local target="$1"
  case "${target}" in
    "${BUILDS_DIR}"/*|"${BUNDLES_DIR}"/*|"${RELEASE_ROOT}/docker/backend/images/"*|"${RELEASE_ROOT}/docker/frontend/nginx/html/"*) ;;
    *) error "拒绝删除非发布目录: ${target}"; return 1 ;;
  esac
  [[ -e "${target}" ]] || return 0
  find "${target}" -depth -mindepth 1 -delete
  rmdir "${target}"
}

frontend_apps() {
  find "${FRONTEND_ROOT}/apps" -mindepth 1 -maxdepth 1 -type d \
    -exec test -f '{}/package.json' \; -print \
    | sed -E 's#^.*/##' \
    | LC_ALL=C sort
}

package_name_for_app() {
  node --input-type=module -e '
    import fs from "node:fs";
    const pkg = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
    if (!pkg.name) process.exit(2);
    process.stdout.write(pkg.name);
  ' "${FRONTEND_ROOT}/apps/$1/package.json"
}

prepare_staging_dir() {
  local env_name="$1"
  local target="$2"
  local current_dir="${BUILDS_DIR}/current_${env_name}"
  local staging_dir

  mkdir -p "${BUILDS_DIR}"
  staging_dir="$(mktemp -d "${BUILDS_DIR}/.staging_${env_name}.XXXXXX")"
  if [[ "${target}" != all && -d "${current_dir}" ]]; then
    copy_dir "${current_dir}" "${staging_dir}"
  fi
  printf '%s' "${staging_dir}"
}

package_backend() {
  local env_name="$1"
  local bundle="$2"
  local staging_dir="$3"
  local maven_profiles="${env_name}"
  local item name source_rel source_file target_dir

  require_cmd java
  [[ -x "${BACKEND_ROOT}/mvnw" ]] || {
    error "缺少 Maven Wrapper: ${BACKEND_ROOT}/mvnw"
    return 1
  }
  [[ "${bundle}" == full ]] || maven_profiles="${env_name},bundle-core"

  info "构建后端：profiles=${maven_profiles}"
  (cd "${BACKEND_ROOT}" && ./mvnw clean package -DskipTests "-P${maven_profiles}")

  for item in "${BACKEND_ARTIFACTS[@]}"; do
    name="${item%%|*}"
    source_rel="${item#*|}"
    source_file="${BACKEND_ROOT}/${source_rel}"
    [[ -f "${source_file}" ]] || {
      error "缺少后端产物: ${source_file}"
      return 1
    }
    target_dir="${staging_dir}/${name}"
    [[ ! -e "${target_dir}" ]] || remove_tree "${target_dir}"
    mkdir -p "${target_dir}"
    install -m 0644 "${source_file}" "${target_dir}/${name}.jar"
  done
}

clear_frontend_artifacts() {
  local staging_dir="$1"
  local child base
  for child in "${staging_dir}"/*; do
    [[ -d "${child}" ]] || continue
    base="${child##*/}"
    case "${base}" in
      wta-admin|wta-monitor-admin|wta-snailjob-server|wta-snailai-server) ;;
      *) remove_tree "${child}" ;;
    esac
  done
}

package_frontend() {
  local env_name="$1"
  local env_file="$2"
  local staging_dir="$3"
  local app package_name prefix dist_dir target_dir
  local found=0

  require_cmd node
  require_cmd pnpm
  clear_frontend_artifacts "${staging_dir}"

  info "校验前端工作区架构"
  (cd "${FRONTEND_ROOT}" && pnpm architecture:check)

  while IFS= read -r app; do
    [[ -n "${app}" ]] || continue
    found=$((found + 1))
    package_name="$(package_name_for_app "${app}")"
    prefix="$(app_prefix "${app}" "${env_file}")"
    info "构建前端 ${app}: package=${package_name}, base=/${prefix}/"
    (
      cd "${FRONTEND_ROOT}"
      VITE_APP_CONTEXT_PATH="/${prefix}/" \
      VITE_APP_BASE_API="/${prefix}/${env_name}-api" \
        pnpm --filter "${package_name}" "build:${env_name}"
    )
    dist_dir="${FRONTEND_ROOT}/apps/${app}/dist"
    [[ -d "${dist_dir}" ]] || {
      error "缺少前端产物目录: ${dist_dir}"
      return 1
    }
    target_dir="${staging_dir}/${app}"
    mkdir -p "${target_dir}"
    copy_dir "${dist_dir}" "${target_dir}"
    printf '%s\n' "${prefix}" > "${target_dir}/.release-prefix"
  done < <(frontend_apps)

  [[ ${found} -gt 0 ]] || {
    error "未发现包含 package.json 的可构建前端 App"
    return 1
  }
}

write_manifest() {
  local staging_dir="$1"
  local env_name="$2"
  local bundle="$3"
  local parent_sha backend_sha frontend_sha

  require_cmd node
  parent_sha="$(git -C "${REPO_ROOT}" rev-parse HEAD)"
  backend_sha="$(git -C "${BACKEND_ROOT}" rev-parse HEAD)"
  frontend_sha="$(git -C "${FRONTEND_ROOT}" rev-parse HEAD)"

  node --input-type=module - \
    "${staging_dir}" "${env_name}" "${bundle}" \
    "${parent_sha}" "${backend_sha}" "${frontend_sha}" <<'NODE'
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const [root, environment, bundle, parentSha, backendSha, frontendSha] = process.argv.slice(2);
const files = [];

function walk(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === 'release-manifest.json') continue;
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) walk(absolute);
    if (entry.isFile()) {
      const relative = path.relative(root, absolute).split(path.sep).join('/');
      const digest = crypto.createHash('sha256').update(fs.readFileSync(absolute)).digest('hex');
      files.push({ path: relative, sha256: digest, size: fs.statSync(absolute).size });
    }
  }
}

walk(root);
files.sort((a, b) => a.path.localeCompare(b.path));
const manifest = {
  schemaVersion: 1,
  generatedAt: new Date().toISOString(),
  environment,
  backendBundle: bundle,
  git: { parent: parentSha, backend: backendSha, frontend: frontendSha },
  files,
};
fs.writeFileSync(path.join(root, 'release-manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
NODE
}

promote_staging() {
  local staging_dir="$1"
  local env_name="$2"
  local current_dir="${BUILDS_DIR}/current_${env_name}"
  local previous_dir="${BUILDS_DIR}/.previous_${env_name}"

  [[ ! -e "${previous_dir}" ]] || remove_tree "${previous_dir}"
  if [[ -d "${current_dir}" ]]; then
    mv "${current_dir}" "${previous_dir}"
  fi
  mv "${staging_dir}" "${current_dir}"
  [[ ! -e "${previous_dir}" ]] || remove_tree "${previous_dir}"
  info "发布产物已生成: ${current_dir}"
}

build_release() {
  local target="$1"
  local env_name="$2"
  local bundle="$3"
  local env_file="$4"
  local staging_dir

  validate_target "${target}"
  validate_env_name "${env_name}"
  validate_bundle "${bundle}"
  staging_dir="$(prepare_staging_dir "${env_name}" "${target}")"
  trap '[[ -z "${staging_dir:-}" || ! -d "${staging_dir}" ]] || remove_tree "${staging_dir}"' RETURN

  case "${target}" in
    backend) package_backend "${env_name}" "${bundle}" "${staging_dir}" ;;
    frontend) package_frontend "${env_name}" "${env_file}" "${staging_dir}" ;;
    all)
      package_backend "${env_name}" "${bundle}" "${staging_dir}"
      package_frontend "${env_name}" "${env_file}" "${staging_dir}"
      ;;
  esac
  write_manifest "${staging_dir}" "${env_name}" "${bundle}"
  promote_staging "${staging_dir}" "${env_name}"
  staging_dir=""
  trap - RETURN
}

backup_current_releases() {
  local source env_name base target index found=0
  shopt -s nullglob
  for source in "${BUILDS_DIR}"/current_*; do
    [[ -d "${source}" ]] || continue
    env_name="${source##*/current_}"
    base="${BUILDS_DIR}/$(date +%Y-%m-%d)_${env_name}"
    target="${base}"
    index=2
    while [[ -e "${target}" ]]; do
      target="${base}-${index}"
      index=$((index + 1))
    done
    mv "${source}" "${target}"
    info "已备份: ${target}"
    found=$((found + 1))
  done
  shopt -u nullglob
  [[ ${found} -gt 0 ]] || warn "没有 current_<env> 可备份"
}

clear_directory_except_gitignore() {
  local directory="$1"
  mkdir -p "${directory}"
  find "${directory}" -mindepth 1 ! -name .gitignore -delete
}

stage_backend_images() {
  local current_dir="$1"
  local item name jar target
  for item in "${BACKEND_ARTIFACTS[@]}"; do
    name="${item%%|*}"
    jar="${current_dir}/${name}/${name}.jar"
    target="${RELEASE_ROOT}/docker/backend/images/${name}/app.jar"
    [[ -f "${jar}" ]] || {
      error "缺少 ${jar}，请先构建后端或 all"
      return 1
    }
    install -m 0644 "${jar}" "${target}"
  done
}

stage_frontend_html() {
  local current_dir="$1"
  local source app target found=0
  for source in "${current_dir}"/*; do
    [[ -d "${source}" && -f "${source}/index.html" ]] || continue
    app="${source##*/}"
    target="${RELEASE_ROOT}/docker/frontend/nginx/html/${app}"
    [[ -f "${RELEASE_ROOT}/docker/frontend/nginx/apps/nginx-${app}.conf.template" ]] || {
      error "${app} 缺少独立 Nginx 模板；请先运行项目 Skill 的 add_app.py"
      return 1
    }
    clear_directory_except_gitignore "${target}"
    copy_dir "${source}" "${target}"
    [[ ! -f "${target}/.release-prefix" ]] || rm "${target}/.release-prefix"
    found=$((found + 1))
  done
  [[ ${found} -gt 0 ]] || warn "当前构建没有前端产物"
}

stage_mysql_init() {
  local target_root="${RELEASE_ROOT}/docker/infrastructure/mysql/init"
  local filename index=0
  local sql_files=(
    "10-cde-base-ddl.sql"
    "20-cde-job.sql"
    "30-cde-workflow.sql"
    "40-cde-ai.sql"
    "50-cde-base-dml.sql"
    "60-cde-nacos.sql"
  )

  for filename in "${sql_files[@]}"; do
    [[ -s "${target_root}/${filename}" && -r "${target_root}/${filename}" ]] || {
      error "缺少、为空或无法读取数据库初始化文件: ${target_root}/${filename}"
      return 1
    }
  done

  while IFS= read -r filename; do
    [[ ${index} -lt ${#sql_files[@]} && "${filename}" == "${sql_files[${index}]}" ]] || {
      error "MySQL 基座文件名称或顺序异常: ${filename}"
      return 1
    }
    index=$((index + 1))
  done < <(find "${target_root}" -mindepth 1 -maxdepth 1 -type f -name '[0-9][0-9]-*.sql' -exec basename {} \; | LC_ALL=C sort)

  [[ ${index} -eq ${#sql_files[@]} ]] || {
    error "MySQL 基座文件数量异常: ${index}，预期 ${#sql_files[@]}"
    return 1
  }
    info "MySQL 基座校验通过：六份 SQL 由 release-artifacts 直接维护，未执行复制或改写"
}

stage_release() {
  local env_name="$1"
  local current_dir="${BUILDS_DIR}/current_${env_name}"
  validate_env_name "${env_name}"
  [[ -d "${current_dir}" ]] || {
    error "缺少 ${current_dir}，请先执行完整构建"
    return 1
  }
  stage_backend_images "${current_dir}"
  stage_frontend_html "${current_dir}"
  stage_mysql_init
  info "Docker 构建上下文与运行资产已准备"
}

bundle_release() {
  local env_name="$1"
  local current_dir="${BUILDS_DIR}/current_${env_name}"
  local timestamp bundle_name bundle_path temp_dir package_root

  stage_release "${env_name}"
  require_cmd tar
  timestamp="$(date +%Y%m%d%H%M%S)"
  bundle_name="namewta-${env_name}-${timestamp}"
  bundle_path="${BUNDLES_DIR}/${bundle_name}.tar.gz"
  temp_dir="$(mktemp -d "${BUNDLES_DIR}/.bundle.XXXXXX")"
  package_root="${temp_dir}/${bundle_name}"
  mkdir -p "${package_root}/builds"
  install -m 0644 "${RELEASE_ROOT}/README.md" "${package_root}/README.md"
  install -m 0644 "${RELEASE_ROOT}/.env.example" "${package_root}/.env.example"
  copy_dir "${RELEASE_ROOT}/docker" "${package_root}/docker"
  copy_dir "${RELEASE_ROOT}/scripts" "${package_root}/scripts"
  copy_dir "${RELEASE_ROOT}/skills" "${package_root}/skills"
  copy_dir "${RELEASE_ROOT}/tests" "${package_root}/tests"
  copy_dir "${current_dir}" "${package_root}/builds/current_${env_name}"
  tar -C "${temp_dir}" -czf "${bundle_path}" "${bundle_name}"
  remove_tree "${temp_dir}"
  info "部署包已生成: ${bundle_path}"
}

interactive_menu() {
  local choice target env_name bundle
  while true; do
    cat <<'EOF'

NAMEWTA Release 管理
  1) 完整构建
  2) 仅构建前端
  3) 仅构建后端
  4) 备份 current_<env>
  5) 准备 Docker 运行资产
  6) 生成完整部署包
  0) 退出
EOF
    read -r -p '请选择 [0-6]: ' choice || return 0
    case "${choice}" in
      1|2|3)
        read -r -p '环境 [dev/prod，默认 prod]: ' env_name
        env_name="${env_name:-prod}"
        target=all
        [[ "${choice}" == 2 ]] && target=frontend
        [[ "${choice}" == 3 ]] && target=backend
        bundle=full
        build_release "${target}" "${env_name}" "${bundle}" "${DEFAULT_ENV_FILE}"
        ;;
      4) backup_current_releases ;;
      5)
        read -r -p '环境 [dev/prod，默认 prod]: ' env_name
        stage_release "${env_name:-prod}"
        ;;
      6)
        read -r -p '环境 [dev/prod，默认 prod]: ' env_name
        bundle_release "${env_name:-prod}"
        ;;
      0) return 0 ;;
      *) warn "无效选择: ${choice}" ;;
    esac
  done
}

main() {
  local command="${1:-menu}"
  local target=all env_name=prod bundle=full env_file="${DEFAULT_ENV_FILE}"
  [[ $# -eq 0 ]] || shift

  case "${command}" in
    build)
      while [[ $# -gt 0 ]]; do
        case "$1" in
          --target|--env|--bundle|--env-file)
            [[ -n "${2:-}" ]] || { error "$1 缺少值"; return 2; }
            case "$1" in
              --target) target="$2" ;;
              --env) env_name="$2" ;;
              --bundle) bundle="$2" ;;
              --env-file) env_file="$2" ;;
            esac
            shift 2
            ;;
          -h|--help) usage; return 0 ;;
          *) error "未知参数: $1"; usage; return 2 ;;
        esac
      done
      build_release "${target}" "${env_name}" "${bundle}" "${env_file}"
      ;;
    backup) backup_current_releases ;;
    stage-mysql) stage_mysql_init ;;
    stage)
      [[ "${1:-}" == --env && -n "${2:-}" ]] || { usage; return 2; }
      stage_release "$2"
      ;;
    bundle)
      [[ "${1:-}" == --env && -n "${2:-}" ]] || { usage; return 2; }
      bundle_release "$2"
      ;;
    menu) interactive_menu ;;
    -h|--help|help) usage ;;
    *) error "未知命令: ${command}"; usage; return 2 ;;
  esac
}

main "$@"
