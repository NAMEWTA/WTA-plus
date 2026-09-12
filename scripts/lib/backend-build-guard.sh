#!/usr/bin/env bash

backend_build_lock_dir=""
backend_build_lock_canonical_path=""
backend_build_reclaim_dir=""
backend_build_reclaim_canonical_path=""

backend_build_lock_path_for() {
  local backend_path=${1}
  local canonical_path
  local lock_key
  local lock_root=${NAMEWTA_BUILD_LOCK_ROOT:-${TMPDIR:-/tmp}}

  canonical_path=$(cd -- "${backend_path}" && pwd -P) || return 1
  lock_key=$(printf '%s\n' "${canonical_path}" | cksum | awk '{print $1}')
  printf '%s/namewta-backend-build-%s.lock\n' "${lock_root%/}" "${lock_key}"
}

backend_build_lock_acquire() {
  local backend_path=${1}
  local canonical_path
  local candidate_lock_dir
  local candidate_reclaim_dir
  local current_owner_backend
  local current_owner_pid
  local lock_root=${NAMEWTA_BUILD_LOCK_ROOT:-${TMPDIR:-/tmp}}
  local owner_backend
  local owner_pid
  local reclaimer_pid

  if [[ -n "${backend_build_lock_dir}" || -n "${backend_build_reclaim_dir}" ]]; then
    echo "错误：当前 shell 已持有后端构建锁，拒绝嵌套获取。" >&2
    return 1
  fi

  canonical_path=$(cd -- "${backend_path}" && pwd -P) || {
    echo "错误：后端目录不存在或不可访问：${backend_path}" >&2
    return 1
  }
  if [[ "${canonical_path}" == *$'\n'* ]]; then
    echo "错误：后端目录不能包含换行符：${canonical_path}" >&2
    return 1
  fi

  mkdir -p -- "${lock_root}" || {
    echo "错误：无法创建后端构建锁根目录：${lock_root}" >&2
    return 1
  }
  candidate_lock_dir=$(backend_build_lock_path_for "${canonical_path}") || return 1
  candidate_reclaim_dir="${candidate_lock_dir}.reclaim"

  if [[ -e "${candidate_reclaim_dir}" ]]; then
    reclaimer_pid=$(awk -F= '$1 == "pid" { print $2; exit }' "${candidate_reclaim_dir}/owner" 2>/dev/null || true)
    echo "错误：后端 stale lock 正由 PID ${reclaimer_pid:-unknown} 回收：${candidate_reclaim_dir}" >&2
    return 1
  fi

  if ! mkdir -- "${candidate_lock_dir}" 2>/dev/null; then
    owner_pid=$(awk -F= '$1 == "pid" { print $2; exit }' "${candidate_lock_dir}/owner" 2>/dev/null || true)
    owner_backend=$(awk -F= '$1 == "backend" { sub(/^[^=]*=/, ""); print; exit }' "${candidate_lock_dir}/owner" 2>/dev/null || true)
    if [[ "${owner_backend}" != "${canonical_path}" ]]; then
      echo "错误：后端构建锁冲突且 owner 元数据不匹配：${candidate_lock_dir}" >&2
      return 1
    fi
    if [[ ! "${owner_pid}" =~ ^[1-9][0-9]*$ ]]; then
      echo "错误：后端构建锁存在但 owner PID 无效：${candidate_lock_dir}" >&2
      return 1
    fi
    if kill -0 "${owner_pid}" 2>/dev/null; then
      echo "错误：后端构建锁已由 PID ${owner_pid} 持有：${owner_backend}" >&2
      return 1
    fi

    if ! mkdir -- "${candidate_reclaim_dir}" 2>/dev/null; then
      reclaimer_pid=$(awk -F= '$1 == "pid" { print $2; exit }' "${candidate_reclaim_dir}/owner" 2>/dev/null || true)
      echo "错误：后端 stale lock 正由 PID ${reclaimer_pid:-unknown} 回收：${candidate_reclaim_dir}" >&2
      return 1
    fi
    backend_build_reclaim_dir=${candidate_reclaim_dir}
    backend_build_reclaim_canonical_path=${canonical_path}
    if ! {
      printf 'pid=%s\n' "$$"
      printf 'backend=%s\n' "${canonical_path}"
    } >"${candidate_reclaim_dir}/owner"; then
      rm -f -- "${candidate_reclaim_dir}/owner"
      rmdir -- "${candidate_reclaim_dir}" 2>/dev/null || true
      backend_build_reclaim_dir=""
      backend_build_reclaim_canonical_path=""
      echo "错误：无法写入 stale lock 回收 owner：${candidate_reclaim_dir}" >&2
      return 1
    fi

    current_owner_pid=$(awk -F= '$1 == "pid" { print $2; exit }' "${candidate_lock_dir}/owner" 2>/dev/null || true)
    current_owner_backend=$(awk -F= '$1 == "backend" { sub(/^[^=]*=/, ""); print; exit }' "${candidate_lock_dir}/owner" 2>/dev/null || true)
    if [[ "${current_owner_pid}" != "${owner_pid}" || "${current_owner_backend}" != "${owner_backend}" ]]; then
      backend_build_reclaim_release || true
      echo "错误：stale lock 检查期间 owner 已变化，拒绝删除新的后端构建锁。" >&2
      return 1
    fi
    if kill -0 "${current_owner_pid}" 2>/dev/null; then
      backend_build_reclaim_release || true
      echo "错误：stale lock 检查期间 PID ${current_owner_pid} 已恢复为活动状态，拒绝删除。" >&2
      return 1
    fi
    if ! rm -f -- "${candidate_lock_dir}/owner" || ! rmdir -- "${candidate_lock_dir}" 2>/dev/null; then
      backend_build_reclaim_release || true
      echo "错误：无法安全清理 stale 后端构建锁：${candidate_lock_dir}" >&2
      return 1
    fi
    if ! mkdir -- "${candidate_lock_dir}" 2>/dev/null; then
      backend_build_reclaim_release || true
      echo "错误：清理 stale lock 后发生新的后端构建锁竞争：${candidate_lock_dir}" >&2
      return 1
    fi
  fi

  backend_build_lock_dir=${candidate_lock_dir}
  backend_build_lock_canonical_path=${canonical_path}
  if ! {
    printf 'pid=%s\n' "$$"
    printf 'backend=%s\n' "${canonical_path}"
    printf 'started_at=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  } >"${candidate_lock_dir}/owner"; then
    rm -f -- "${candidate_lock_dir}/owner"
    rmdir -- "${candidate_lock_dir}" 2>/dev/null || true
    backend_build_lock_dir=""
    backend_build_lock_canonical_path=""
    backend_build_reclaim_release || true
    echo "错误：无法写入后端构建锁 owner：${candidate_lock_dir}" >&2
    return 1
  fi

  if ! backend_build_reclaim_release; then
    backend_build_lock_release || true
    echo "错误：无法释放 stale lock 回收门：${candidate_reclaim_dir}" >&2
    return 1
  fi
}

backend_build_reclaim_release() {
  local owner_backend
  local owner_pid

  [[ -n "${backend_build_reclaim_dir}" ]] || return 0
  owner_pid=$(awk -F= '$1 == "pid" { print $2; exit }' "${backend_build_reclaim_dir}/owner" 2>/dev/null || true)
  owner_backend=$(awk -F= '$1 == "backend" { sub(/^[^=]*=/, ""); print; exit }' "${backend_build_reclaim_dir}/owner" 2>/dev/null || true)
  if [[ -n "${owner_pid}" && "${owner_pid}" != "$$" ]] ||
    [[ -n "${owner_backend}" && "${owner_backend}" != "${backend_build_reclaim_canonical_path}" ]]; then
    echo "错误：拒绝释放不属于当前进程的 stale lock 回收门：${backend_build_reclaim_dir}" >&2
    return 1
  fi

  rm -f -- "${backend_build_reclaim_dir}/owner"
  rmdir -- "${backend_build_reclaim_dir}"
  backend_build_reclaim_dir=""
  backend_build_reclaim_canonical_path=""
}

backend_build_lock_release() {
  local owner_backend
  local owner_pid

  backend_build_reclaim_release || return 1
  [[ -n "${backend_build_lock_dir}" ]] || return 0
  owner_pid=$(awk -F= '$1 == "pid" { print $2; exit }' "${backend_build_lock_dir}/owner" 2>/dev/null || true)
  owner_backend=$(awk -F= '$1 == "backend" { sub(/^[^=]*=/, ""); print; exit }' "${backend_build_lock_dir}/owner" 2>/dev/null || true)
  if [[ -n "${owner_pid}" && "${owner_pid}" != "$$" ]] ||
    [[ -n "${owner_backend}" && "${owner_backend}" != "${backend_build_lock_canonical_path}" ]]; then
    echo "错误：拒绝释放不属于当前进程的后端构建锁：${backend_build_lock_dir}" >&2
    return 1
  fi

  rm -f -- "${backend_build_lock_dir}/owner"
  rmdir -- "${backend_build_lock_dir}"
  backend_build_lock_dir=""
  backend_build_lock_canonical_path=""
}

backend_build_lock_exit_cleanup() {
  local status=$?

  if ! backend_build_lock_release && [[ ${status} -eq 0 ]]; then
    status=1
  fi
  trap - EXIT
  exit "${status}"
}

backend_build_lock_signal_exit() {
  local status=${1}

  trap - INT TERM
  exit "${status}"
}

# This top-level lifecycle hook replaces the caller's EXIT/INT/TERM traps.
backend_build_lock_install_cleanup_traps() {
  trap 'backend_build_lock_exit_cleanup' EXIT
  trap 'backend_build_lock_signal_exit 130' INT
  trap 'backend_build_lock_signal_exit 143' TERM
}

backend_build_verify_module_jar() {
  local classes_dir=${1}
  local module_jar=${2}
  local actual_classes
  local class_count
  local expected_classes
  local extra_classes
  local missing_classes
  local sentinel_class
  local verification_dir

  shift 2

  if [[ ! -d "${classes_dir}" ]]; then
    echo "错误：模块 classes 目录不存在：${classes_dir}" >&2
    return 1
  fi
  class_count=$(find "${classes_dir}" -type f -name '*.class' | wc -l | tr -d '[:space:]')
  if [[ "${class_count}" == "0" ]]; then
    echo "错误：模块 classes 目录中没有 class：${classes_dir}" >&2
    return 1
  fi
  if [[ ! -f "${module_jar}" ]]; then
    echo "错误：模块 JAR 不存在：${module_jar}" >&2
    return 1
  fi
  verification_dir=$(mktemp -d "${TMPDIR:-/tmp}/namewta-module-jar-check.XXXXXX") || {
    echo "错误：无法创建模块 JAR 校验临时目录。" >&2
    return 1
  }
  expected_classes="${verification_dir}/expected"
  actual_classes="${verification_dir}/actual"
  missing_classes="${verification_dir}/missing"
  extra_classes="${verification_dir}/extra"

  if ! (
    cd -- "${classes_dir}"
    find . -type f -name '*.class' -print | sed 's#^\./##' | LC_ALL=C sort
  ) >"${expected_classes}"; then
    rm -f -- "${expected_classes}" "${actual_classes}" "${missing_classes}" "${extra_classes}"
    rmdir -- "${verification_dir}" 2>/dev/null || true
    echo "错误：无法读取模块 classes 集合：${classes_dir}" >&2
    return 1
  fi
  if ! jar tf "${module_jar}" | awk '/\.class$/ { sub(/^\.\//, ""); print }' | LC_ALL=C sort >"${actual_classes}"; then
    rm -f -- "${expected_classes}" "${actual_classes}" "${missing_classes}" "${extra_classes}"
    rmdir -- "${verification_dir}" 2>/dev/null || true
    echo "错误：模块 JAR 无法读取：${module_jar}" >&2
    return 1
  fi
  if ! cmp -s "${expected_classes}" "${actual_classes}"; then
    comm -23 "${expected_classes}" "${actual_classes}" >"${missing_classes}"
    comm -13 "${expected_classes}" "${actual_classes}" >"${extra_classes}"
    echo "错误：模块 JAR 与 classes 的 class 集合不一致：${module_jar}" >&2
    echo "期望 ${class_count} 个，实际 $(wc -l <"${actual_classes}" | tr -d '[:space:]') 个。" >&2
    if [[ -s "${missing_classes}" ]]; then
      echo "JAR 缺失（最多显示 10 项）：" >&2
      sed -n '1,10p' "${missing_classes}" >&2
    fi
    if [[ -s "${extra_classes}" ]]; then
      echo "JAR 额外包含（最多显示 10 项）：" >&2
      sed -n '1,10p' "${extra_classes}" >&2
    fi
    rm -f -- "${expected_classes}" "${actual_classes}" "${missing_classes}" "${extra_classes}"
    rmdir -- "${verification_dir}" 2>/dev/null || true
    return 1
  fi

  for sentinel_class in "$@"; do
    if [[ "${sentinel_class}" != *.class || "${sentinel_class}" == /* || "${sentinel_class}" == ../* || "${sentinel_class}" == */../* ]]; then
      rm -f -- "${expected_classes}" "${actual_classes}" "${missing_classes}" "${extra_classes}"
      rmdir -- "${verification_dir}" 2>/dev/null || true
      echo "错误：无效的 class 哨兵相对路径：${sentinel_class}" >&2
      return 1
    fi
    if ! grep -Fqx -- "${sentinel_class}" "${expected_classes}"; then
      rm -f -- "${expected_classes}" "${actual_classes}" "${missing_classes}" "${extra_classes}"
      rmdir -- "${verification_dir}" 2>/dev/null || true
      echo "错误：模块产物缺少必需的 class 哨兵：${sentinel_class}" >&2
      return 1
    fi
  done

  rm -f -- "${expected_classes}" "${actual_classes}" "${missing_classes}" "${extra_classes}"
  rmdir -- "${verification_dir}"
}
