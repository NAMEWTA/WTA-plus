#!/usr/bin/env bash
set -euo pipefail

# Non-login Git Bash inherits Windows PATH, so system32/find.exe can shadow GNU find.
case "$(uname -s 2>/dev/null)" in
  MINGW* | MSYS* | CYGWIN*)
    PATH="/usr/bin:${PATH}"
    ;;
esac

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
workspace_root=$(cd -- "${script_dir}/.." && pwd)
frontend_dir="${workspace_root}/plus-ui-namewta"
backend_dir="${workspace_root}/wta-vue-plus-namewta"
backend_local_config="wta-admin/src/main/resources/application-local.yml"
backend_build_guard="${script_dir}/lib/backend-build-guard.sh"
dev_runtime="${script_dir}/lib/dev-runtime.sh"
pnpm_runner=()
mvnw_cmd=()

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

resolve_pnpm_runner() {
  if command -v pnpm >/dev/null 2>&1; then
    pnpm_runner=(pnpm)
  elif command -v corepack >/dev/null 2>&1; then
    pnpm_runner=(corepack pnpm)
  else
    fail "未找到 pnpm 或 corepack，请先完成本机前端开发环境配置。"
  fi
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

reject_occupied_port() {
  local port=${1}
  local service_name=${2}
  local listeners=${3}

  echo "错误：${service_name} 端口 ${port} 已被占用，未启动新进程。" >&2
  echo "${listeners}" >&2
  exit 1
}

ensure_port_available() {
  local port=${1}
  local service_name=${2}
  local listeners

  if command -v lsof >/dev/null 2>&1; then
    listeners=$(lsof -nP -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)
    if [[ -n "${listeners}" ]]; then
      reject_occupied_port "${port}" "${service_name}" "${listeners}"
    fi
    return
  fi

  if command -v netstat >/dev/null 2>&1; then
    listeners=$(netstat -ano 2>/dev/null | dev_runtime_filter_listening_port "${port}")
    if [[ -n "${listeners}" ]]; then
      reject_occupied_port "${port}" "${service_name}" "${listeners}"
    fi
    return
  fi

  if (exec 3<>"/dev/tcp/127.0.0.1/${port}") 2>/dev/null; then
    exec 3<&- || true
    exec 3>&- || true
    fail "${service_name} 端口 ${port} 已被占用，且当前环境无法显示占用进程。"
  fi
}

resolve_installed_system_jar() {
  local classpath_file
  local classpath_value
  local installed_system_jar

  classpath_file=$(mktemp "${TMPDIR:-/tmp}/namewta-admin-classpath.XXXXXX") || return 1
  # Force a newline separator so Windows ';' classpaths and drive-letter ':' stay intact.
  if ! run_mvnw -q -f wta-admin/pom.xml dependency:build-classpath \
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
    org/dromara/system/domain/vo/SysClientVo.class
    org/dromara/system/mapper/SysUserMapper.class
    org/dromara/system/password/PasswordPolicyService.class
    org/dromara/system/service/ISysClientService.class
    org/dromara/system/temporarypassword/TemporaryPasswordService.class
  )

  target_system_jar=$(resolve_target_system_jar) || fail "无法唯一定位 wta-system target JAR，请停止并发构建后重新启动。"
  installed_system_jar=$(resolve_installed_system_jar) || fail "无法从 wta-admin Maven classpath 唯一定位已安装的 wta-system JAR。"

  echo "正在校验 wta-system target JAR 完整性..."
  backend_build_verify_module_jar "${system_classes}" "${target_system_jar}" "${sentinels[@]}" || \
    fail "wta-system target JAR 不完整；请停止其他 Maven/IDE 构建后重新运行后端启动。"
  echo "正在校验 wta-admin classpath 中的 wta-system JAR 完整性..."
  backend_build_verify_module_jar "${system_classes}" "${installed_system_jar}" "${sentinels[@]}" || \
    fail "已安装的 wta-system JAR 不完整；请停止其他 Maven/IDE 构建后重新运行后端启动。"
}

start_frontend() {
  resolve_pnpm_runner
  [[ -f "${frontend_dir}/package.json" ]] || fail "前端目录不完整：${frontend_dir}"
  [[ -f "${frontend_dir}/pnpm-lock.yaml" ]] || fail "缺少前端 lockfile：${frontend_dir}/pnpm-lock.yaml"
  ensure_port_available 80 "前端"

  cd "${frontend_dir}"
  echo "正在安装前端依赖（严格使用 pnpm lockfile）..."
  "${pnpm_runner[@]}" install --frozen-lockfile
  echo "正在前台启动前端，启动后请访问 http://localhost/，按 Ctrl+C 停止..."
  export BROWSER=none
  exec "${pnpm_runner[@]}" dev --strictPort
}

start_backend() {
  require_command java
  require_command jar
  resolve_mvnw_cmd
  [[ -r "${backend_build_guard}" ]] || fail "缺少后端构建保护模块：${backend_build_guard}"

  # shellcheck source=lib/backend-build-guard.sh
  source "${backend_build_guard}"
  backend_build_lock_install_cleanup_traps
  backend_build_lock_acquire "${backend_dir}" || fail "当前后端工作区无法取得独占构建锁。"

  [[ -s "${backend_dir}/${backend_local_config}" ]] || fail "缺少非空的本地后端配置：${backend_dir}/${backend_local_config}"
  ensure_port_available 8080 "后端"

  cd "${backend_dir}"
  echo "正在刷新后端本地 Maven reactor（跳过自动测试）..."
  run_mvnw clean install -Dmaven.test.skip=true -Plocal
  verify_system_artifacts
  backend_build_lock_release
  cd wta-admin
  echo "正在以前台 dev,local profiles 启动后端，按 Ctrl+C 停止..."
  if [[ -x ../mvnw ]]; then
    exec ../mvnw spring-boot:run \
      -Dmaven.test.skip=true \
      -Pdev \
      -Dspring-boot.run.profiles=dev,local
  fi
  exec bash ../mvnw spring-boot:run \
    -Dmaven.test.skip=true \
    -Pdev \
    -Dspring-boot.run.profiles=dev,local
}

start_dev_main() {
  echo "请选择要启动的本地测试服务："
  echo "1、启动前端"
  echo "2、启动后端"
  printf "请输入选项 [1-2]："
  if ! IFS= read -r choice; then
    fail "未读取到启动选项。" 2
  fi

  case "${choice}" in
    1)
      start_frontend
      ;;
    2)
      start_backend
      ;;
    *)
      fail "无效选项：${choice}（只能输入 1 或 2）。" 2
      ;;
  esac
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  start_dev_main
fi
