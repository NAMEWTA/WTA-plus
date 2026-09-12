#!/usr/bin/env bash
set -euo pipefail

workspace_root=$(git rev-parse --show-toplevel)
guard_module="${workspace_root}/scripts/lib/backend-build-guard.sh"
dev_runtime="${workspace_root}/scripts/lib/dev-runtime.sh"
vscode_settings="${workspace_root}/.vscode/settings.json"
test_root=$(mktemp -d "${TMPDIR:-/tmp}/namewta-build-guard-test.XXXXXX")
backend_fixture="${test_root}/backend"
lock_root="${test_root}/locks"
race_child=""
signal_child=""

cleanup() {
  local status=$?

  trap - EXIT
  for child in "${race_child}" "${signal_child}"; do
    if [[ -n "${child}" ]] && kill -0 "${child}" 2>/dev/null; then
      kill -TERM "${child}" 2>/dev/null || true
      wait "${child}" 2>/dev/null || true
    fi
  done
  rm -rf -- "${test_root}"
  exit "${status}"
}
trap cleanup EXIT

mkdir -p "${backend_fixture}" "${lock_root}"

if [[ ! -r "${guard_module}" ]]; then
  echo "build guard module is unavailable: ${guard_module}" >&2
  exit 1
fi
if [[ ! -r "${dev_runtime}" ]]; then
  echo "dev runtime module is unavailable: ${dev_runtime}" >&2
  exit 1
fi
if [[ ! -r "${vscode_settings}" ]] ||
  ! grep -Eq '"java\.autobuild\.enabled"[[:space:]]*:[[:space:]]*false' "${vscode_settings}"; then
  echo "workspace Java auto build must stay disabled to protect Maven target output" >&2
  exit 1
fi
if git -C "${workspace_root}" check-ignore -q .vscode/settings.json; then
  echo "workspace Java auto-build protection is ignored and cannot be delivered" >&2
  exit 1
fi
# shellcheck source=../lib/backend-build-guard.sh
source "${guard_module}"
# shellcheck source=../lib/dev-runtime.sh
source "${dev_runtime}"

assert_system_jar() {
  local label=${1}
  local classpath_value=${2}
  local expected=${3}
  local selected
  local status

  set +e
  selected=$(dev_runtime_select_system_jar "${classpath_value}")
  status=$?
  set -e
  if [[ ${status} -ne 0 || "${selected}" != "${expected}" ]]; then
    echo "${label}: expected [${expected}], got status=${status} selected=[${selected}]" >&2
    exit 1
  fi
}

assert_system_jar_unresolved() {
  local label=${1}
  local classpath_value=${2}
  local status

  set +e
  dev_runtime_select_system_jar "${classpath_value}" >/dev/null
  status=$?
  set -e
  if [[ ${status} -eq 0 ]]; then
    echo "${label}: uniquely resolved a system JAR from an invalid classpath" >&2
    exit 1
  fi
}

unix_system_jar='/home/u/.m2/repository/org/namewta/wta-system/6.0.0/wta-system-6.0.0.jar'
windows_system_jar='D:\repo\org\namewta\wta-system\6.0.0\wta-system-6.0.0.jar'
windows_other_jar='D:\repo\other-1.0.0.jar'
windows_sources_jar='C:\cache\wta-system-6.0.0-sources.jar'

assert_system_jar "unix classpath" \
  "${unix_system_jar}:/home/u/.m2/other-1.0.0.jar" \
  "${unix_system_jar}"
assert_system_jar "windows semicolon classpath" \
  "${windows_system_jar};${windows_other_jar};${windows_sources_jar}" \
  "$(dev_runtime_canonicalize_path "${windows_system_jar}")"
assert_system_jar "windows single drive-letter path" \
  "${windows_system_jar}" \
  "$(dev_runtime_canonicalize_path "${windows_system_jar}")"
assert_system_jar "newline classpath" \
  $'D:\\repo\\org\\namewta\\wta-system\\6.0.0\\wta-system-6.0.0.jar\nD:\\repo\\other-1.0.0.jar' \
  "$(dev_runtime_canonicalize_path "${windows_system_jar}")"
assert_system_jar_unresolved "empty classpath" ""
assert_system_jar_unresolved "sources-only classpath" "${windows_sources_jar}"
assert_system_jar_unresolved "duplicate runtime jars" \
  "${windows_system_jar};C:\\cache\\wta-system-5.0.0.jar"

netstat_fixture=$'  TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       1234\n  TCP    [::]:8080              [::]:0                 LISTENING       5678\n  TCP    127.0.0.1:18080         0.0.0.0:0              LISTENING       9\n  TCP    172.16.105.25:8631     23.11.38.161:80        CLOSE_WAIT      21288'
netstat_8080=$(printf '%s\n' "${netstat_fixture}" | dev_runtime_filter_listening_port 8080)
netstat_80=$(printf '%s\n' "${netstat_fixture}" | dev_runtime_filter_listening_port 80)
if [[ "${netstat_8080}" != *$':8080'* || "${netstat_8080}" == *$':18080'* ]]; then
  echo "netstat 8080 filter did not keep listening 8080 only: ${netstat_8080}" >&2
  exit 1
fi
if [[ -n "${netstat_80}" ]]; then
  echo "netstat 80 filter matched a non-listening remote port: ${netstat_80}" >&2
  exit 1
fi

NAMEWTA_BUILD_LOCK_ROOT="${lock_root}"
export NAMEWTA_BUILD_LOCK_ROOT

backend_build_lock_acquire "${backend_fixture}"
owned_lock_dir=${backend_build_lock_dir}

set +e
conflict_output=$(bash -c 'source "$1"; backend_build_lock_acquire "$2"' _ "${guard_module}" "${backend_fixture}" 2>&1)
conflict_status=$?
set -e

if [[ ${conflict_status} -eq 0 ]]; then
  echo "lock conflict unexpectedly succeeded" >&2
  exit 1
fi
if [[ "${conflict_output}" != *"PID $$"* ]]; then
  echo "lock conflict did not report owner PID: ${conflict_output}" >&2
  exit 1
fi
if [[ ! -d "${owned_lock_dir}" ]]; then
  echo "lock conflict removed the active owner lock" >&2
  exit 1
fi

nested_backend="${test_root}/nested-backend"
mkdir -p "${nested_backend}"
set +e
nested_output=$(backend_build_lock_acquire "${nested_backend}" 2>&1)
nested_status=$?
set -e
if [[ ${nested_status} -eq 0 ]]; then
  echo "one shell unexpectedly acquired a nested backend lock" >&2
  exit 1
fi
if [[ "${backend_build_lock_dir}" != "${owned_lock_dir}" || ! -d "${owned_lock_dir}" ]]; then
  echo "nested acquisition changed the existing lock ownership" >&2
  exit 1
fi

backend_build_lock_release

partial_backend="${test_root}/partial-owner-backend"
mkdir -p "${partial_backend}"
partial_lock_dir=$(backend_build_lock_path_for "${partial_backend}")
mkdir "${partial_lock_dir}"
backend_build_lock_dir=${partial_lock_dir}
backend_build_lock_canonical_path=$(cd -- "${partial_backend}" && pwd -P)
backend_build_lock_release
if [[ -e "${partial_lock_dir}" ]]; then
  echo "empty partially-created owner lock was not cleaned" >&2
  exit 1
fi

backend_build_lock_acquire "${backend_fixture}"
stale_lock_dir=${backend_build_lock_dir}
stale_pid=999999
while kill -0 "${stale_pid}" 2>/dev/null; do
  stale_pid=$((stale_pid + 1))
done
{
  printf 'pid=%s\n' "${stale_pid}"
  printf 'backend=%s\n' "$(cd -- "${backend_fixture}" && pwd -P)"
  printf 'started_at=2000-01-01T00:00:00Z\n'
} >"${stale_lock_dir}/owner"
backend_build_lock_dir=""
backend_build_lock_canonical_path=""

backend_build_lock_acquire "${backend_fixture}"
if ! grep -q "^pid=$$\$" "${backend_build_lock_dir}/owner"; then
  echo "stale lock was not replaced by the current owner" >&2
  exit 1
fi
backend_build_lock_release

race_backend="${test_root}/race-backend"
race_ready="${test_root}/race-ready"
race_go="${test_root}/race-go"
race_log="${test_root}/race.log"
mkdir -p "${race_backend}"
race_lock_dir=$(backend_build_lock_path_for "${race_backend}")
mkdir "${race_lock_dir}"
race_stale_pid=999999
while kill -0 "${race_stale_pid}" 2>/dev/null; do
  race_stale_pid=$((race_stale_pid + 1))
done
{
  printf 'pid=%s\n' "${race_stale_pid}"
  printf 'backend=%s\n' "$(cd -- "${race_backend}" && pwd -P)"
  printf 'started_at=2000-01-01T00:00:00Z\n'
} >"${race_lock_dir}/owner"

bash -c '
  set -euo pipefail
  source "$1"
  NAMEWTA_BUILD_LOCK_ROOT=$2
  export NAMEWTA_BUILD_LOCK_ROOT
  stale_pid=$4
  ready_file=$5
  go_file=$6
  kill() {
    if [[ $1 == "-0" && $2 == "${stale_pid}" ]]; then
      : >"${ready_file}"
      while [[ ! -e "${go_file}" ]]; do sleep 0.01; done
      return 1
    fi
    command kill "$@"
  }
  backend_build_lock_acquire "$3"
' _ "${guard_module}" "${lock_root}" "${race_backend}" "${race_stale_pid}" "${race_ready}" "${race_go}" >"${race_log}" 2>&1 &
race_child=$!

for _ in {1..100}; do
  [[ -e "${race_ready}" ]] && break
  kill -0 "${race_child}" 2>/dev/null || break
  sleep 0.05
done
if [[ ! -e "${race_ready}" ]]; then
  echo "stale reclaim race fixture did not reach the synchronization point" >&2
  cat "${race_log}" >&2
  exit 1
fi

backend_build_lock_acquire "${race_backend}"
: >"${race_go}"
set +e
wait "${race_child}"
race_status=$?
race_child=""
set -e
if [[ ${race_status} -eq 0 ]]; then
  echo "two contenders both acquired the stale lock" >&2
  exit 1
fi
backend_build_lock_release

verify_signal_cleanup() {
  local expected_status=${2}
  local signal_name=${1}
  local signal_lock_dir
  local signal_log="${test_root}/signal-${signal_name}.log"
  local signal_go="${test_root}/signal-${signal_name}-go"
  local signal_ready="${test_root}/signal-${signal_name}-ready"
  local signal_status

  bash -c '
    set -euo pipefail
    source "$1"
    NAMEWTA_BUILD_LOCK_ROOT=$2
    export NAMEWTA_BUILD_LOCK_ROOT
    backend_build_lock_acquire "$3"
    printf "%s\n" "${backend_build_lock_dir}" >"$4"
    backend_build_lock_install_cleanup_traps
    if [[ $5 == "INT" ]]; then
      while [[ ! -e "$6" ]]; do sleep 0.01; done
      backend_build_lock_signal_exit 130
    fi
    while :; do sleep 1; done
  ' _ "${guard_module}" "${lock_root}" "${backend_fixture}" "${signal_ready}" "${signal_name}" "${signal_go}" >"${signal_log}" 2>&1 &
  signal_child=$!

  for _ in {1..100}; do
    [[ -s "${signal_ready}" ]] && break
    kill -0 "${signal_child}" 2>/dev/null || break
    sleep 0.05
  done
  if [[ ! -s "${signal_ready}" ]]; then
    echo "${signal_name} cleanup fixture did not acquire the lock" >&2
    cat "${signal_log}" >&2
    exit 1
  fi

  signal_lock_dir=$(<"${signal_ready}")
  if [[ "${signal_name}" == "INT" ]]; then
    : >"${signal_go}"
  else
    kill -"${signal_name}" "${signal_child}" 2>/dev/null || true
  fi
  set +e
  wait "${signal_child}"
  signal_status=$?
  signal_child=""
  set -e
  if [[ ${signal_status} -ne ${expected_status} ]]; then
    echo "${signal_name} cleanup returned ${signal_status}, expected ${expected_status}" >&2
    cat "${signal_log}" >&2
    exit 1
  fi
  if [[ -d "${signal_lock_dir}" ]]; then
    echo "${signal_name} cleanup left the owned lock behind: ${signal_lock_dir}" >&2
    cat "${signal_log}" >&2
    exit 1
  fi

  backend_build_lock_acquire "${backend_fixture}"
  backend_build_lock_release
}

verify_signal_cleanup INT 130
verify_signal_cleanup TERM 143

classes_fixture="${test_root}/classes"
complete_jar="${test_root}/complete.jar"
mkdir -p "${classes_fixture}/org/example"
: >"${classes_fixture}/org/example/Present.class"
: >"${classes_fixture}/org/example/Other.class"
jar --create --file "${complete_jar}" -C "${classes_fixture}" .

backend_build_verify_module_jar \
  "${classes_fixture}" \
  "${complete_jar}" \
  "org/example/Present.class"

empty_classes="${test_root}/empty-classes"
mkdir -p "${empty_classes}"
set +e
empty_output=$(backend_build_verify_module_jar "${empty_classes}" "${complete_jar}" 2>&1)
empty_status=$?
missing_jar_output=$(backend_build_verify_module_jar "${classes_fixture}" "${test_root}/missing.jar" 2>&1)
missing_jar_status=$?
set -e
if [[ ${empty_status} -eq 0 || "${empty_output}" != *"没有 class"* ]]; then
  echo "empty classes failure was not enforced: ${empty_output}" >&2
  exit 1
fi
if [[ ${missing_jar_status} -eq 0 || "${missing_jar_output}" != *"JAR 不存在"* ]]; then
  echo "missing module JAR failure was not enforced: ${missing_jar_output}" >&2
  exit 1
fi

partial_jar="${test_root}/partial.jar"
jar --create --file "${partial_jar}" -C "${classes_fixture}" org/example/Present.class
set +e
partial_output=$(backend_build_verify_module_jar "${classes_fixture}" "${partial_jar}" 2>&1)
partial_status=$?
set -e
if [[ ${partial_status} -eq 0 ]]; then
  echo "partial module JAR unexpectedly passed verification" >&2
  exit 1
fi
if [[ "${partial_output}" != *"class 集合不一致"* ]]; then
  echo "partial module JAR failure was not actionable: ${partial_output}" >&2
  exit 1
fi

set +e
sentinel_output=$(backend_build_verify_module_jar \
  "${classes_fixture}" \
  "${complete_jar}" \
  "org/example/Missing.class" 2>&1)
sentinel_status=$?
set -e
if [[ ${sentinel_status} -eq 0 ]]; then
  echo "module JAR without the required sentinel unexpectedly passed verification" >&2
  exit 1
fi
if [[ "${sentinel_output}" != *"class 哨兵"* ]]; then
  echo "missing sentinel failure was not actionable: ${sentinel_output}" >&2
  exit 1
fi

start_workspace="${test_root}/start-workspace"
start_backend="${start_workspace}/backend"
mkdir -p "${start_workspace}/scripts/lib" "${start_backend}"
cp "${workspace_root}/scripts/start-dev.sh" "${start_workspace}/scripts/start-dev.sh"
cp "${guard_module}" "${start_workspace}/scripts/lib/backend-build-guard.sh"
cp "${dev_runtime}" "${start_workspace}/scripts/lib/dev-runtime.sh"
: >"${start_backend}/mvnw"
chmod +x "${start_backend}/mvnw"

backend_build_lock_acquire "${start_backend}"
fake_bin="${test_root}/bin"
mkdir -p "${fake_bin}"
ln -s /usr/bin/false "${fake_bin}/lsof"
set +e
start_conflict_output=$(printf '2\n' | PATH="${fake_bin}:${PATH}" "${start_workspace}/scripts/start-dev.sh" 2>&1)
start_conflict_status=$?
set -e
backend_build_lock_release
if [[ ${start_conflict_status} -eq 0 ]]; then
  echo "start-dev lock conflict unexpectedly succeeded" >&2
  exit 1
fi
if [[ "${start_conflict_output}" != *"PID $$"* ]]; then
  echo "start-dev lock conflict did not report owner PID: ${start_conflict_output}" >&2
  exit 1
fi
if [[ "${start_conflict_output}" == *"正在刷新后端本地 Maven reactor"* ]]; then
  echo "start-dev invoked Maven before rejecting the lock conflict" >&2
  exit 1
fi

mkdir -p "${start_backend}/wta-admin/src/main/resources"
printf 'spring:\n  config:\n    activate:\n      on-profile: local\n' >"${start_backend}/wta-admin/src/main/resources/application-local.yml"
git -C "${start_backend}" init -q
git -C "${start_backend}" add -- wta-admin/src/main/resources/application-local.yml

set +e
tracked_config_output=$(printf '2\n' | PATH="${fake_bin}:${PATH}" "${start_workspace}/scripts/start-dev.sh" 2>&1)
tracked_config_status=$?
set -e
if [[ ${tracked_config_status} -eq 0 ]]; then
  echo "start-dev tracked-config fixture unexpectedly completed" >&2
  exit 1
fi
if [[ "${tracked_config_output}" == *"本地后端配置未被 Git 忽略"* ]]; then
  echo "start-dev rejected a tracked local backend config: ${tracked_config_output}" >&2
  exit 1
fi
if [[ "${tracked_config_output}" != *"正在刷新后端本地 Maven reactor"* ]]; then
  echo "start-dev did not accept a tracked local backend config: ${tracked_config_output}" >&2
  exit 1
fi

echo "workspace Java auto build, backend lock lifecycle, Windows classpath parsing, and module JAR class-set verification passed"
