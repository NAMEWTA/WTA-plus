#!/usr/bin/env bash
# 本地前台启动。
# 无参数，或数字参数没写全且标准输入是终端：先选前端或后端，再选启动方式；
# 前端再选应用。选完就启动。
# 数字参数按同一顺序补齐。1 是前端，2 是后端；两侧的第二个数字含义不同。
# 文字子命令仍是 start、build、doctor、repair。
# 省略模式的 start 不 clean、不重装、不校验 JAR 哨兵，也不把 server.port 写成 Spring 参数。
# repair、build、doctor 只准备或检查，不启动。
#
# 后端两段式都必须留在 backend 聚合根：
# 根 POM 以 import 引入仓内 wta-common-bom、wta-profile-bom。它们不是模块依赖，
# -am 不会安装它们；进入 wta-admin 后 reactor 看不到这些 BOM。
# spring-boot:run 不能加 -am，否则没有主类的依赖模块也会执行该目标。
# 聚合根没有 spring-boot 插件前缀，所以必须带 -pl wta-admin。
set -euo pipefail

# Non-login Git Bash inherits Windows PATH, so system32/find.exe can shadow GNU find.
case "$(uname -s 2>/dev/null)" in
  MINGW* | MSYS* | CYGWIN*)
    PATH="/usr/bin:${PATH}"
    ;;
esac

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
workspace_root=$(cd -- "${script_dir}/.." && pwd)
frontend_dir="${workspace_root}/frontend"
backend_dir="${workspace_root}/backend"
backend_local_config="wta-admin/src/main/resources/application-local.yml"
backend_port_default="38888"
backend_build_guard="${script_dir}/lib/backend-build-guard.sh"
dev_runtime="${script_dir}/lib/dev-runtime.sh"
pnpm_runner=()
mvnw_cmd=()
backend_port="${backend_port_default}"
frontend_apps=()
dev_usage_command="./scripts/start-dev.sh"
dev_selection_target=""
dev_selection_mode=""
dev_selection_app=""

[[ -r "${dev_runtime}" ]] || {
  echo "错误：缺少本地启动运行时模块：${dev_runtime}" >&2
  exit 1
}
# shellcheck source=lib/dev-runtime.sh
source "${dev_runtime}"

fail() {
  local message=${1}
  local status=${2:-1}
  echo "错误：${message}" >&2
  exit "${status}"
}

require_command() {
  local command_name=${1}
  command -v "${command_name}" >/dev/null 2>&1 || fail "未找到命令 ${command_name}，请先完成本机开发环境配置。"
}

# Git Bash 上 pnpm 经常只有 .cmd 包装。
resolve_windows_or_unix_cmd() {
  local command_name=${1}
  if command -v "${command_name}" >/dev/null 2>&1; then
    command -v "${command_name}"
    return 0
  fi
  if command -v "${command_name}.cmd" >/dev/null 2>&1; then
    command -v "${command_name}.cmd"
    return 0
  fi
  return 1
}

resolve_pnpm_runner() {
  local resolved
  if resolved=$(resolve_windows_or_unix_cmd pnpm); then
    pnpm_runner=("${resolved}")
    return 0
  fi
  if command -v corepack >/dev/null 2>&1; then
    pnpm_runner=(corepack pnpm)
    return 0
  fi
  fail "未找到 pnpm 或 corepack，请先完成本机前端开发环境配置。"
}

resolve_mvnw_cmd() {
  if [[ -x "${backend_dir}/mvnw" ]]; then
    mvnw_cmd=("./mvnw")
    return
  fi
  if [[ -f "${backend_dir}/mvnw" ]]; then
    mvnw_cmd=(bash "./mvnw")
    return
  fi
  fail "后端 Maven Wrapper 不存在或不可执行：${backend_dir}/mvnw"
}

run_mvnw() {
  "${mvnw_cmd[@]}" "$@"
}

trim_env_value() {
  local value=${1}
  value=${value%$'\r'}
  value=${value#${value%%[![:space:]]*}}
  value=${value%${value##*[![:space:]]}}
  value=${value#\"}
  value=${value%\"}
  value=${value#\'}
  value=${value%\'}
  printf '%s' "${value}"
}

# Windows 终端 read 可能带 CR/空格，菜单序号先规范化再校验。
normalize_choice() {
  trim_env_value "${1}"
}

read_menu_choice() {
  local prompt=${1}
  local choice
  printf '%s' "${prompt}" >&2
  if ! IFS= read -r choice; then
    return 1
  fi
  normalize_choice "${choice}"
}

# 从 env 文件读取单个键，忽略注释行；等号两侧允许空格。不打印文件中的其他键。
read_env_key() {
  local file=${1}
  local key=${2}
  local line raw
  [[ -f "${file}" ]] || return 0
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line=${line%$'\r'}
    [[ "${line}" =~ ^[[:space:]]*# ]] && continue
    if [[ "${line}" =~ ^[[:space:]]*${key}[[:space:]]*= ]]; then
      raw=${line#*=}
      trim_env_value "${raw}"
      return 0
    fi
  done < "${file}"
}

# Vite development：后读取的文件覆盖先读取的文件。只返回指定键。
resolve_vite_env_key() {
  local app_dir=${1}
  local key=${2}
  local file value result=""
  local files=(
    "${app_dir}/.env"
    "${app_dir}/.env.local"
    "${app_dir}/.env.development"
    "${app_dir}/.env.development.local"
  )
  for file in "${files[@]}"; do
    value=$(read_env_key "${file}" "${key}") || true
    if [[ -n "${value}" ]]; then
      result="${value}"
    fi
  done
  printf '%s' "${result}"
}

build_visit_url() {
  local port=${1}
  local context=${2}
  if [[ -z "${context}" ]]; then
    context="/"
  elif [[ "${context}" != /* ]]; then
    context="/${context}"
  fi
  printf 'http://127.0.0.1:%s%s' "${port}" "${context}"
}

reject_occupied_port() {
  local port=${1}
  local service_name=${2}
  local listeners=${3}

  echo "错误：${service_name} 端口 ${port} 已被占用，未启动新进程。" >&2
  [[ -n "${listeners}" ]] && echo "${listeners}" >&2
  exit 1
}

ensure_port_available() {
  local port=${1}
  local service_name=${2}
  local listeners=""

  if command -v lsof >/dev/null 2>&1; then
    listeners=$(lsof -nP -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)
    if [[ -n "${listeners}" ]]; then
      reject_occupied_port "${port}" "${service_name}" "${listeners}"
    fi
    return 0
  fi

  if command -v netstat >/dev/null 2>&1; then
    listeners=$(netstat -ano 2>/dev/null | dev_runtime_filter_listening_port "${port}")
    if [[ -n "${listeners}" ]]; then
      reject_occupied_port "${port}" "${service_name}" "${listeners}"
    fi
    return 0
  fi

  if (exec 3<>"/dev/tcp/127.0.0.1/${port}") 2>/dev/null; then
    exec 3<&- || true
    exec 3>&- || true
    fail "${service_name} 端口 ${port} 已被占用，且当前环境无法显示占用进程。"
  fi
}

# Windows 上的 node.exe 不认识 Git Bash 的 /d/... 路径。
to_node_path() {
  local path=${1}
  case "$(uname -s 2>/dev/null)" in
    MINGW* | MSYS* | CYGWIN*)
      if command -v cygpath >/dev/null 2>&1; then
        cygpath -m -- "${path}"
        return 0
      fi
      ;;
  esac
  printf '%s' "${path}"
}

# 有 dev 脚本时把包名写到 stdout；没有 dev 脚本时退出 2。
# Git Bash 会把带换行的 node -e 参数截断，脚本必须保持单行。
read_frontend_package_name() {
  local package_json=${1}
  local node_path
  node_path=$(to_node_path "${package_json}")
  node -e 'const fs=require("fs");const pkg=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));if(!pkg.scripts||typeof pkg.scripts.dev!=="string"||pkg.scripts.dev.length===0){process.exit(2);}if(typeof pkg.name!=="string"||pkg.name.length===0){process.exit(3);}process.stdout.write(pkg.name);' -- "${node_path}"
}

load_frontend_apps() {
  local app_dir package_json status name sorted_names sorted nullglob_was_on=0
  local names=()
  frontend_apps=()
  # shopt -p 在选项关闭时返回 1。不能用 previous=$(shopt -p ...)，否则 set -e 会在清缓存或列应用后直接退出。
  if shopt -q nullglob; then
    nullglob_was_on=1
  fi
  shopt -s nullglob
  for app_dir in "${frontend_dir}/apps"/*; do
    [[ -d "${app_dir}" ]] || continue
    package_json="${app_dir}/package.json"
    [[ -f "${package_json}" ]] || continue
    status=0
    name=$(read_frontend_package_name "${package_json}") || status=$?
    if [[ ${status} -eq 2 ]]; then
      continue
    fi
    if [[ ${status} -ne 0 || -z "${name}" ]]; then
      [[ ${nullglob_was_on} -eq 1 ]] || shopt -u nullglob
      fail "无法读取前端包名：${package_json}"
    fi
    names+=("$(basename -- "${app_dir}")")
  done
  [[ ${nullglob_was_on} -eq 1 ]] || shopt -u nullglob
  [[ ${#names[@]} -gt 0 ]] || fail "未找到可启动的前端应用：${frontend_dir}/apps"
  sorted_names=$(printf '%s\n' "${names[@]}" | LC_ALL=C sort) || fail "无法排序前端应用列表。"
  while IFS= read -r sorted; do
    [[ -n "${sorted}" ]] && frontend_apps+=("${sorted}")
  done <<< "${sorted_names}"
}

choose_frontend_app() {
  local app choice index port package_name
  load_frontend_apps

  # 菜单必须打到 stderr，避免被 $(choose_frontend_app) 当成应用名捕获。
  echo "请选择要启动的前端应用：" >&2
  index=1
  for app in "${frontend_apps[@]}"; do
    package_name=$(read_frontend_package_name "${frontend_dir}/apps/${app}/package.json")
    port=$(resolve_vite_env_key "${frontend_dir}/apps/${app}" VITE_APP_PORT)
    if [[ -n "${port}" ]]; then
      echo "${index}、${app}（${package_name}，${port}）" >&2
    else
      echo "${index}、${app}（${package_name}，端口未配置）" >&2
    fi
    index=$((index + 1))
  done
  choice=$(read_menu_choice "请输入选项 [1-${#frontend_apps[@]}]：") || fail "未读取到前端应用选项。" 2
  [[ "${choice}" =~ ^[0-9]+$ ]] || fail "无效选项：${choice}。" 2
  if (( 10#${choice} < 1 || 10#${choice} > ${#frontend_apps[@]} )); then
    fail "无效选项：${choice}（只能输入 1-${#frontend_apps[@]}）。" 2
  fi
  printf '%s' "${frontend_apps[$((10#${choice} - 1))]}"
}

usage_incomplete() {
  local message=${1}
  echo "错误：${message}" >&2
  echo "标准输入不是终端，请把菜单序号一次写全。例如：" >&2
  echo "  ${dev_usage_command} 1 2 admin-web" >&2
  echo "  ${dev_usage_command} 2 3" >&2
  exit 2
}

print_dev_usage() {
  cat <<EOF
用法：
  ${dev_usage_command}
  ${dev_usage_command} <部分> [启动方式] [前端应用]
  ${dev_usage_command} start frontend [应用] [direct|clean]
  ${dev_usage_command} start backend [direct|install|clean]
  ${dev_usage_command} build backend
  ${dev_usage_command} doctor backend
  ${dev_usage_command} repair frontend [应用]
  ${dev_usage_command} repair backend

无参数时交互选择，选完就前台启动。数字参数按菜单顺序补齐；没写全且标准输入是终端时只问剩余步骤。

部分：1 前端，2 后端。
前端启动方式：1 完全清理后重新安装并启动，2 不清理直接启动。
后端启动方式：1 清理并重新安装后启动，2 增量安装后启动，3 不清理直接启动。
前端应用是第三个参数，可写菜单序号或目录名（如 admin-web）。要稳定指定应用时优先写目录名，序号会随新应用变化。

示例：
  ${dev_usage_command} 1 2 admin-web
  ${dev_usage_command} 2 1
  ${dev_usage_command} 2 3
  ${dev_usage_command} start backend clean

repair、build、doctor 只准备或检查，不启动。省略模式的 start 不清理、不重装、不校验 JAR。
EOF
}

frontend_mode_from_choice() {
  local choice
  choice=$(normalize_choice "${1}")
  case "${choice}" in
    1) printf 'clean' ;;
    2) printf 'direct' ;;
    *) fail "无效选项：${choice}（只能输入 1 或 2）。" 2 ;;
  esac
}

backend_mode_from_choice() {
  local choice
  choice=$(normalize_choice "${1}")
  case "${choice}" in
    1) printf 'clean' ;;
    2) printf 'install' ;;
    3) printf 'direct' ;;
    *) fail "无效选项：${choice}（只能输入 1、2 或 3）。" 2 ;;
  esac
}

normalize_frontend_mode_name() {
  local mode
  mode=$(normalize_choice "${1}")
  case "${mode}" in
    direct) printf 'direct' ;;
    clean | reinstall) printf 'clean' ;;
    *) fail "未知前端启动方式：${mode}。可用 direct 或 clean。" 2 ;;
  esac
}

normalize_backend_mode_name() {
  local mode
  mode=$(normalize_choice "${1}")
  case "${mode}" in
    direct | install | clean) printf '%s' "${mode}" ;;
    *) fail "未知后端启动方式：${mode}。可用 direct、install 或 clean。" 2 ;;
  esac
}

is_menu_index() {
  local value
  value=$(normalize_choice "${1}")
  [[ "${value}" =~ ^[0-9]+$ ]]
}

choose_target() {
  local choice
  echo "请选择要启动的部分：" >&2
  echo "1、前端" >&2
  echo "2、后端" >&2
  choice=$(read_menu_choice "请输入选项 [1-2]：") || fail "未读取到启动部分。" 2
  case "${choice}" in
    1) printf 'frontend' ;;
    2) printf 'backend' ;;
    *) fail "无效选项：${choice}（只能输入 1 或 2）。" 2 ;;
  esac
}

choose_frontend_mode() {
  local choice
  echo "请选择前端启动方式：" >&2
  echo "1、完全清理后重新安装并启动（清理 Vite 缓存和 dist，按 lockfile 重装；不删除 node_modules）" >&2
  echo "2、不清理，直接启动" >&2
  choice=$(read_menu_choice "请输入选项 [1-2]：") || fail "未读取到前端启动方式。" 2
  frontend_mode_from_choice "${choice}"
}

choose_backend_mode() {
  local choice
  echo "请选择后端启动方式：" >&2
  echo "1、清理 Maven target 并重新安装后启动" >&2
  echo "2、增量安装后启动（不 clean；MapStruct 旧生成源可能导致编译失败）" >&2
  echo "3、不清理，直接启动（不重新安装依赖模块，可能仍使用已安装的旧代码）" >&2
  choice=$(read_menu_choice "请输入选项 [1-3]：") || fail "未读取到后端启动方式。" 2
  backend_mode_from_choice "${choice}"
}

# 只删除名单内的缓存路径，且解析后的父目录必须仍在前端根内。
# 不删除 node_modules 本身、pnpm store、源码或锁文件。
remove_frontend_cache_path() {
  local root=${1}
  local target=${2}
  local base parent root_canonical parent_canonical full
  base=$(basename -- "${target}")
  case "${base}" in
    .vite | .cache | .unocss | dist | *.tsbuildinfo) ;;
    *)
      fail "拒绝删除未列入缓存清单的路径：${target}"
      ;;
  esac
  if [[ ! -e "${target}" && ! -L "${target}" ]]; then
    return 0
  fi
  root_canonical=$(cd -- "${root}" && pwd -P) || fail "无法解析前端目录：${root}"
  parent=$(dirname -- "${target}")
  [[ -d "${parent}" ]] || fail "无法解析缓存父目录：${target}"
  parent_canonical=$(cd -- "${parent}" && pwd -P) || fail "无法解析缓存父目录：${target}"
  full="${parent_canonical}/${base}"
  if [[ "${full}" != "${root_canonical}/"* ]]; then
    fail "拒绝删除前端目录之外的缓存：${target}"
  fi
  rm -rf -- "${full}" || fail "无法删除前端缓存：${full}"
  echo "已清理前端缓存：${full}"
}

clear_frontend_dev_caches() {
  local app_dir=${1}
  local include_dist=${2}
  local root_canonical app_canonical target nullglob_was_on=0
  root_canonical=$(cd -- "${frontend_dir}" && pwd -P) || fail "无法解析前端目录：${frontend_dir}"
  app_canonical=$(cd -- "${app_dir}" && pwd -P) || fail "无法解析前端应用目录：${app_dir}"
  if [[ "${app_canonical}" != "${root_canonical}/"* ]]; then
    fail "前端应用不在前端目录内：${app_dir}"
  fi

  remove_frontend_cache_path "${frontend_dir}" "${app_dir}/node_modules/.vite"
  remove_frontend_cache_path "${frontend_dir}" "${app_dir}/node_modules/.cache"
  remove_frontend_cache_path "${frontend_dir}" "${app_dir}/node_modules/.unocss"
  remove_frontend_cache_path "${frontend_dir}" "${app_dir}/.vite"
  remove_frontend_cache_path "${frontend_dir}" "${frontend_dir}/node_modules/.vite"
  remove_frontend_cache_path "${frontend_dir}" "${frontend_dir}/node_modules/.cache"
  remove_frontend_cache_path "${frontend_dir}" "${frontend_dir}/node_modules/.unocss"
  if [[ "${include_dist}" == "yes" ]]; then
    remove_frontend_cache_path "${frontend_dir}" "${app_dir}/dist"
  fi

  if shopt -q nullglob; then
    nullglob_was_on=1
  fi
  shopt -s nullglob
  for target in "${app_dir}"/*.tsbuildinfo; do
    remove_frontend_cache_path "${frontend_dir}" "${target}"
  done
  [[ ${nullglob_was_on} -eq 1 ]] || shopt -u nullglob
}

ensure_frontend_dependencies() {
  local mode=${1}
  case "${mode}" in
    direct)
      if [[ -d "${frontend_dir}/node_modules" ]]; then
        echo "前端依赖已存在，跳过 pnpm install。"
        return 0
      fi
      ;;
  esac
  echo "正在安装前端依赖（严格使用 pnpm lockfile）..."
  "${pnpm_runner[@]}" install --frozen-lockfile || fail "前端依赖安装失败。"
}

start_frontend() {
  local app_name=${1:-}
  local mode=${2:-direct}
  local app_dir app_port app_context package_name visit_url
  local -a vite_args

  case "${mode}" in
    direct) ;;
    clean | reinstall) mode=clean ;;
    *) fail "未知前端启动方式：${mode}。" ;;
  esac
  resolve_pnpm_runner
  require_command node
  [[ -f "${frontend_dir}/package.json" ]] || fail "前端目录不完整：${frontend_dir}"
  [[ -f "${frontend_dir}/pnpm-lock.yaml" ]] || fail "缺少前端 lockfile：${frontend_dir}/pnpm-lock.yaml"
  if [[ -z "${app_name}" ]]; then
    app_name=$(resolve_frontend_app)
  fi
  app_dir="${frontend_dir}/apps/${app_name}"
  package_name=$(read_frontend_package_name "${app_dir}/package.json")
  app_port=$(resolve_vite_env_key "${app_dir}" VITE_APP_PORT)
  app_context=$(resolve_vite_env_key "${app_dir}" VITE_APP_CONTEXT_PATH)
  [[ "${app_port}" =~ ^[0-9]+$ ]] || fail "前端 ${app_name} 未配置有效的 VITE_APP_PORT。"
  if (( 10#${app_port} < 1 || 10#${app_port} > 65535 )); then
    fail "前端 ${app_name} 的 VITE_APP_PORT 超出范围。"
  fi
  visit_url=$(build_visit_url "${app_port}" "${app_context}")

  ensure_port_available "${app_port}" "前端 ${app_name}"
  cd "${frontend_dir}" || fail "无法进入前端目录：${frontend_dir}"
  case "${mode}" in
    direct)
      echo "前端启动方式：保留缓存并直接启动。"
      ;;
    clean)
      echo "正在清理 ${app_name} 的 Vite 缓存和 dist，并准备按 lockfile 重装依赖..."
      clear_frontend_dev_caches "${app_dir}" yes
      ;;
    *)
      fail "未知前端启动方式：${mode}。"
      ;;
  esac
  ensure_frontend_dependencies "${mode}"
  vite_args=(--strictPort)
  if [[ "${mode}" != "direct" ]]; then
    vite_args+=(--force)
  fi
  echo "正在前台启动 ${app_name}（${package_name}），启动后请访问 ${visit_url}，按 Ctrl+C 停止..."
  export BROWSER=none
  # pnpm 10 在 --filter 下会把脚本名后面的参数追加到 dev script。
  # 这里不能再写 --：它会被原样传给 Vite，Vite 将其当作选项结束符，随后忽略 --strictPort。
  exec "${pnpm_runner[@]}" --filter "${package_name}" dev "${vite_args[@]}"
}

resolve_installed_system_jar() {
  local classpath_file
  local classpath_value
  local installed_system_jar

  classpath_file=$(mktemp "${TMPDIR:-/tmp}/namewta-admin-classpath.XXXXXX") || return 1
  # 留在聚合根并用 -pl，仓内 BOM 才能从 reactor 解析。
  # 换行分隔符避免把 Windows 盘符冒号当成 classpath 分隔符。
  if ! run_mvnw -pl wta-admin -q dependency:build-classpath \
    -Dmdep.outputFile="${classpath_file}" \
    -Dmdep.pathSeparator=$'\n'; then
    rm -f -- "${classpath_file}"
    return 1
  fi
  classpath_value=$(<"${classpath_file}")
  rm -f -- "${classpath_file}"

  installed_system_jar=$(dev_runtime_select_system_jar "${classpath_value}") || return 1
  printf '%s\n' "${installed_system_jar}"
}

resolve_target_system_jar() {
  local candidate
  local system_target_dir="${backend_dir}/wta-modules/wta-system/target"
  local target_jars=()

  while IFS= read -r candidate; do
    [[ -n "${candidate}" ]] || continue
    target_jars+=("${candidate}")
  done < <(
    shopt -s nullglob
    for candidate in "${system_target_dir}"/wta-system-*.jar; do
      case "$(dev_runtime_path_basename "${candidate}")" in
        *-sources.jar | *-javadoc.jar) ;;
        *) printf '%s\n' "${candidate}" ;;
      esac
    done
  )

  [[ ${#target_jars[@]} -eq 1 ]] || return 1
  printf '%s\n' "${target_jars[0]}"
}

verify_system_artifacts() {
  local installed_system_jar
  local system_classes="${backend_dir}/wta-modules/wta-system/target/classes"
  local target_system_jar
  local sentinels=(
    org/namewta/system/domain/vo/SysClientVo.class
    org/namewta/system/mapper/SysUserMapper.class
    org/namewta/system/password/PasswordPolicyService.class
    org/namewta/system/service/ISysClientService.class
    org/namewta/system/temporarypassword/TemporaryPasswordService.class
  )

  target_system_jar=$(resolve_target_system_jar) || fail "无法唯一定位 wta-system target JAR，请停止并发构建后重新启动。可改选「清理 Maven target 并重新安装后启动」。"
  installed_system_jar=$(resolve_installed_system_jar) || fail "无法从 wta-admin Maven classpath 唯一定位已安装的 wta-system JAR。可改选「清理 Maven target 并重新安装后启动」。"

  echo "正在校验 wta-system target JAR 完整性..."
  backend_build_verify_module_jar "${system_classes}" "${target_system_jar}" "${sentinels[@]}" || \
    fail "wta-system target JAR 不完整；请停止其他 Maven/IDE 构建后重新运行后端启动。"
  echo "正在校验 wta-admin classpath 中的 wta-system JAR 完整性..."
  backend_build_verify_module_jar "${system_classes}" "${installed_system_jar}" "${sentinels[@]}" || \
    fail "已安装的 wta-system JAR 不完整；请停止其他 Maven/IDE 构建后重新运行后端启动。"
}

# 只读取 application-local.yml 顶层 server.port，不输出文件中的其他配置。
load_backend_port() {
  local config_file="${backend_dir}/${backend_local_config}"
  local line in_server=0 port="" raw
  [[ -f "${config_file}" ]] || fail "未找到后端本地配置：${config_file}"
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line=${line%$'\r'}
    [[ "${line}" =~ ^[[:space:]]*# ]] && continue
    [[ "${line}" =~ ^[[:space:]]*$ ]] && continue
    if [[ ${in_server} -eq 0 ]]; then
      if [[ "${line}" =~ ^server:[[:space:]]*(#.*)?$ ]]; then
        in_server=1
      fi
      continue
    fi
    if [[ "${line}" =~ ^[^[:space:]] ]]; then
      break
    fi
    if [[ "${line}" =~ ^[[:space:]]+port:[[:space:]]*(.*)$ ]]; then
      raw=$(trim_env_value "${BASH_REMATCH[1]}")
      raw=${raw%%#*}
      raw=$(trim_env_value "${raw}")
      if [[ ! "${raw}" =~ ^[0-9]+$ ]]; then
        fail "后端本地配置的顶层 server.port 不是有效端口。"
      fi
      port="${raw}"
      break
    fi
  done < "${config_file}"
  if [[ -z "${port}" ]]; then
    port="${backend_port_default}"
    echo "application-local.yml 未配置顶层 server.port，使用默认端口 ${backend_port_default}。"
  fi
  if (( 10#${port} < 1 || 10#${port} > 65535 )); then
    fail "后端端口超出范围：${port}。"
  fi
  backend_port="${port}"
}

prepare_backend_reactor() {
  local mode=${1}
  case "${mode}" in
    direct)
      echo "跳过后端 Maven clean/install，直接校验已有产物..."
      ;;
    install)
      echo "正在增量安装后端本地 Maven reactor（不 clean，跳过测试，仅 wta-admin 及其依赖）..."
      run_mvnw -pl wta-admin -am -Dmaven.test.skip=true -Plocal install || fail "后端依赖安装失败。"
      ;;
    clean)
      # 必须先 clean：MapStruct Plus 增量编译会在 target/generated-sources 留下旧的
      # AutoMapperConfig__XXXX；新一轮生成的 Mapper 引用新哈希后，javac 会报找不到符号。
      # 若 clean 仍失败（Windows 删不掉 target），多半是 IDE Java 语言服务锁文件，
      # 请确认工作区 java.autobuild.enabled=false 后再重试。
      echo "正在刷新后端本地 Maven reactor（先 clean 再安装，跳过测试，仅 wta-admin 及其依赖）..."
      run_mvnw -pl wta-admin -am -Dmaven.test.skip=true -Plocal clean install || fail "后端依赖安装失败。"
      ;;
    *)
      fail "未知后端启动方式：${mode}。"
      ;;
  esac
}

# 只决定占用检查用的端口。不导出、不覆盖 SERVER_PORT，也不把它传给 Spring。
resolve_checked_port() {
  if [[ -n "${SERVER_PORT+x}" ]]; then
    if [[ ! "${SERVER_PORT}" =~ ^[0-9]+$ ]] || (( 10#${SERVER_PORT} < 1 || 10#${SERVER_PORT} > 65535 )); then
      fail "SERVER_PORT 不是有效端口。"
    fi
    backend_port="${SERVER_PORT}"
    return 0
  fi
  load_backend_port
}

acquire_backend_build_lock() {
  [[ -r "${backend_build_guard}" ]] || fail "缺少后端构建保护模块：${backend_build_guard}"
  # shellcheck source=lib/backend-build-guard.sh
  source "${backend_build_guard}"
  backend_build_lock_install_cleanup_traps
  backend_build_lock_acquire "${backend_dir}" || fail "当前后端工作区无法取得独占构建锁。"
}

# verify=yes 时在释放锁之前校验产物。不能把锁留到 spring-boot:run：exec 不会跑 EXIT trap。
run_locked_backend_prepare() {
  local mode=${1}
  local verify=${2:-no}
  acquire_backend_build_lock
  prepare_backend_reactor "${mode}"
  if [[ "${verify}" == "yes" ]]; then
    verify_system_artifacts
  fi
  backend_build_lock_release
}

start_backend() {
  local mode=${1:-direct}
  local runtime_config
  case "${mode}" in
    direct | install | clean) ;;
    *) fail "未知后端启动方式：${mode}。" ;;
  esac
  require_command java
  require_command jar
  resolve_mvnw_cmd
  [[ -s "${backend_dir}/${backend_local_config}" ]] || fail "缺少非空的本地后端配置：${backend_dir}/${backend_local_config}"
  [[ -f "${backend_dir}/pom.xml" ]] || fail "未找到后端 pom.xml：${backend_dir}/pom.xml"
  [[ -f "${backend_dir}/wta-admin/pom.xml" ]] || fail "未找到 wta-admin 模块：${backend_dir}/wta-admin"
  resolve_checked_port
  ensure_port_available "${backend_port}" "后端"

  cd "${backend_dir}" || fail "无法进入后端目录：${backend_dir}"
  # direct 不安装、不校验。install 和 clean 在接管进程前释放构建锁。
  if [[ "${mode}" != "direct" ]]; then
    run_locked_backend_prepare "${mode}" yes
  fi
  # Private configuration is excluded from classpath resources and release JARs.
  # Keep an operator-provided external location authoritative.
  if [[ -z "${SPRING_CONFIG_ADDITIONAL_LOCATION:-}" ]]; then
    runtime_config="${backend_dir}/${backend_local_config}"
    case "$(uname -s 2>/dev/null)" in
      MINGW* | MSYS* | CYGWIN*) runtime_config=$(cygpath -m -- "${runtime_config}") ;;
    esac
    export SPRING_CONFIG_ADDITIONAL_LOCATION="optional:file:${runtime_config}"
  fi
  echo "正在以前台 dev,local profiles 启动 wta-admin。端口由 SERVER_PORT 或外部配置决定，脚本不覆盖，按 Ctrl+C 停止..."
  exec "${mvnw_cmd[@]}" -pl wta-admin -Dmaven.test.skip=true -Pdev spring-boot:run \
    -Dspring-boot.run.profiles=dev,local
}

build_backend() {
  require_command java
  resolve_mvnw_cmd
  [[ -f "${backend_dir}/pom.xml" ]] || fail "未找到后端 pom.xml：${backend_dir}/pom.xml"
  cd "${backend_dir}" || fail "无法进入后端目录：${backend_dir}"
  # 只增量安装，不校验，也不启动。菜单里的后端 2 会在安装后校验并启动。
  run_locked_backend_prepare install no
}

repair_backend() {
  require_command java
  require_command jar
  resolve_mvnw_cmd
  [[ -f "${backend_dir}/pom.xml" ]] || fail "未找到后端 pom.xml：${backend_dir}/pom.xml"
  cd "${backend_dir}" || fail "无法进入后端目录：${backend_dir}"
  run_locked_backend_prepare clean yes
  echo "repair: 后端已 clean install 并完成产物检查。repair 不会启动；要接着启动请使用 start backend clean，或菜单序号 2 1。"
}

doctor_backend() {
  require_command java
  require_command jar
  resolve_mvnw_cmd
  [[ -f "${backend_dir}/pom.xml" ]] || fail "未找到后端 pom.xml：${backend_dir}/pom.xml"
  cd "${backend_dir}" || fail "无法进入后端目录：${backend_dir}"
  acquire_backend_build_lock
  verify_system_artifacts
  backend_build_lock_release
  echo "doctor: 后端产物检查完成，未安装、未清理、未启动。"
}

repair_frontend() {
  local app_name=${1:-}
  local app_dir
  resolve_pnpm_runner
  require_command node
  [[ -f "${frontend_dir}/package.json" ]] || fail "前端目录不完整：${frontend_dir}"
  [[ -f "${frontend_dir}/pnpm-lock.yaml" ]] || fail "缺少前端 lockfile：${frontend_dir}/pnpm-lock.yaml"
  app_name=$(resolve_frontend_app "${app_name}")
  app_dir="${frontend_dir}/apps/${app_name}"
  [[ -d "${app_dir}" ]] || fail "未找到前端应用：${app_name}"
  cd "${frontend_dir}" || fail "无法进入前端目录：${frontend_dir}"
  echo "repair: 正在清理 ${app_name} 的 Vite 缓存和 dist，并按 lockfile 重装依赖..."
  clear_frontend_dev_caches "${app_dir}" yes
  "${pnpm_runner[@]}" install --frozen-lockfile || fail "前端依赖安装失败。"
  echo "repair: 前端修复完成，未启动。要清理并启动请使用 start frontend ${app_name} clean，或菜单序号 1 1。"
}

resolve_frontend_app() {
  local requested=${1:-}
  local app
  load_frontend_apps
  if [[ -z "${requested}" ]]; then
    if [[ ${#frontend_apps[@]} -eq 1 ]]; then
      printf '%s' "${frontend_apps[0]}"
      return 0
    fi
    if [[ ! -t 0 ]]; then
      usage_incomplete "还没有选择前端应用。"
    fi
    choose_frontend_app
    return 0
  fi
  requested=$(normalize_choice "${requested}")
  if is_menu_index "${requested}"; then
    if (( 10#${requested} < 1 || 10#${requested} > ${#frontend_apps[@]} )); then
      fail "无效前端应用序号：${requested}（只能输入 1-${#frontend_apps[@]}）。" 2
    fi
    printf '%s' "${frontend_apps[$((10#${requested} - 1))]}"
    return 0
  fi
  for app in "${frontend_apps[@]}"; do
    if [[ "${app}" == "${requested}" ]]; then
      printf '%s' "${app}"
      return 0
    fi
  done
  fail "未知前端应用：${requested}。" 2
}

resolve_dev_selection() {
  local target_arg=${1:-}
  local mode_arg=${2:-}
  local app_arg=${3:-}

  if [[ $# -gt 3 ]]; then
    fail "多余参数：${4}。" 2
  fi

  dev_selection_target=""
  dev_selection_mode=""
  dev_selection_app=""

  if [[ -z "${target_arg}" ]]; then
    if [[ ! -t 0 ]]; then
      usage_incomplete "还没有选择前端或后端。"
    fi
    dev_selection_target=$(choose_target)
  else
    target_arg=$(normalize_choice "${target_arg}")
    case "${target_arg}" in
      1) dev_selection_target=frontend ;;
      2) dev_selection_target=backend ;;
      *) fail "无效选项：${target_arg}（只能输入 1 或 2）。" 2 ;;
    esac
  fi

  case "${dev_selection_target}" in
    frontend)
      if [[ -z "${mode_arg}" ]]; then
        if [[ ! -t 0 ]]; then
          usage_incomplete "还没有选择前端启动方式。"
        fi
        dev_selection_mode=$(choose_frontend_mode)
      else
        if ! is_menu_index "${mode_arg}"; then
          fail "无效前端启动方式：${mode_arg}（只能输入 1 或 2）。第二个参数必须是启动方式，不能是应用名。" 2
        fi
        dev_selection_mode=$(frontend_mode_from_choice "${mode_arg}")
      fi
      dev_selection_app=$(resolve_frontend_app "${app_arg}")
      ;;
    backend)
      if [[ -n "${app_arg}" ]]; then
        fail "多余参数：${app_arg}。" 2
      fi
      if [[ -z "${mode_arg}" ]]; then
        if [[ ! -t 0 ]]; then
          usage_incomplete "还没有选择后端启动方式。"
        fi
        dev_selection_mode=$(choose_backend_mode)
      else
        if ! is_menu_index "${mode_arg}"; then
          fail "无效后端启动方式：${mode_arg}（只能输入 1、2 或 3）。" 2
        fi
        dev_selection_mode=$(backend_mode_from_choice "${mode_arg}")
      fi
      ;;
    *)
      fail "未知启动部分：${dev_selection_target}。" 2
      ;;
  esac
}

resolve_start_frontend_args() {
  local token normalized app="" mode=""
  for token in "$@"; do
    normalized=$(normalize_choice "${token}")
    case "${normalized}" in
      direct | clean | reinstall)
        if [[ -n "${mode}" ]]; then
          fail "前端启动方式重复：${normalized}。" 2
        fi
        mode=$(normalize_frontend_mode_name "${normalized}")
        ;;
      *)
        if [[ -n "${app}" ]]; then
          fail "多余的前端参数：${normalized}。" 2
        fi
        app=${normalized}
        ;;
    esac
  done
  if [[ -z "${mode}" ]]; then
    mode=direct
  fi
  dev_selection_target=frontend
  dev_selection_mode=${mode}
  dev_selection_app=$(resolve_frontend_app "${app}")
}

resolve_start_backend_args() {
  local mode=direct
  if [[ $# -gt 1 ]]; then
    fail "多余参数：${2}。" 2
  fi
  if [[ $# -eq 1 ]]; then
    mode=$(normalize_backend_mode_name "${1}")
  fi
  dev_selection_target=backend
  dev_selection_mode=${mode}
  dev_selection_app=""
}

start_dev_main() {
  resolve_dev_selection "$@"
  case "${dev_selection_target}" in
    frontend) start_frontend "${dev_selection_app}" "${dev_selection_mode}" ;;
    backend) start_backend "${dev_selection_mode}" ;;
    *) fail "未知启动部分：${dev_selection_target}。" 2 ;;
  esac
}

dispatch_dev_command() {
  local command service
  command=$(normalize_choice "${1:-}")
  shift || true
  case "${command}" in
    start)
      service=$(normalize_choice "${1:-}")
      if [[ $# -gt 0 ]]; then
        shift
      fi
      case "${service}" in
        backend)
          resolve_start_backend_args "$@"
          start_backend "${dev_selection_mode}"
          ;;
        frontend)
          resolve_start_frontend_args "$@"
          start_frontend "${dev_selection_app}" "${dev_selection_mode}"
          ;;
        *) fail "start 需要 frontend 或 backend。" 2 ;;
      esac
      ;;
    build)
      service=$(normalize_choice "${1:-backend}")
      if [[ $# -gt 0 ]]; then
        shift
      fi
      case "${service}" in
        backend)
          if [[ $# -gt 0 ]]; then
            fail "多余参数：${1}。" 2
          fi
          build_backend
          ;;
        *) fail "build 目前只接受 backend。" 2 ;;
      esac
      ;;
    doctor)
      service=$(normalize_choice "${1:-backend}")
      if [[ $# -gt 0 ]]; then
        shift
      fi
      case "${service}" in
        backend)
          if [[ $# -gt 0 ]]; then
            fail "多余参数：${1}。" 2
          fi
          doctor_backend
          ;;
        *) fail "doctor 目前只接受 backend。" 2 ;;
      esac
      ;;
    repair)
      service=$(normalize_choice "${1:-backend}")
      if [[ $# -gt 0 ]]; then
        shift
      fi
      case "${service}" in
        backend)
          if [[ $# -gt 0 ]]; then
            fail "多余参数：${1}。" 2
          fi
          repair_backend
          ;;
        frontend)
          if [[ $# -gt 1 ]]; then
            fail "多余参数：${2}。" 2
          fi
          repair_frontend "${1:-}"
          ;;
        *) fail "repair 需要 frontend 或 backend。" 2 ;;
      esac
      ;;
    *)
      fail "未知子命令：${command}。可用 start、build、doctor、repair。" 2
      ;;
  esac
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  if [[ $# -eq 0 ]]; then
    start_dev_main
  else
    command_name=$(normalize_choice "${1}")
    case "${command_name}" in
      --help | -h | help)
        print_dev_usage
        exit 0
        ;;
      start | build | doctor | repair)
        dispatch_dev_command "$@"
        ;;
      *)
        if is_menu_index "${command_name}"; then
          start_dev_main "$@"
        else
          fail "未知子命令：${command_name}。可用 start、build、doctor、repair，或菜单序号 1、2。运行 --help 查看用法。" 2
        fi
        ;;
    esac
  fi
fi
