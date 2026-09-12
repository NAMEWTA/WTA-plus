#!/usr/bin/env bash

# Shared helpers for local start-dev runtime checks.
# Windows Maven classpaths use ';' and drive-letter paths like D:\lib\a.jar;
# splitting on ':' would treat the drive letter as a separator.

dev_runtime_path_basename() {
  local path=${1%$'\r'}
  path=${path//\\//}
  path=${path%/}
  printf '%s\n' "${path##*/}"
}

dev_runtime_canonicalize_path() {
  local path=${1%$'\r'}

  if command -v cygpath >/dev/null 2>&1 &&
    { [[ "${path}" == [A-Za-z]:[\\/]* ]] || [[ "${path}" == *\\* ]]; }; then
    cygpath -u -- "${path}"
    return
  fi
  printf '%s\n' "${path}"
}

dev_runtime_split_classpath() {
  local classpath_value=${1//$'\r'/}

  if [[ -z "${classpath_value}" ]]; then
    return 0
  fi
  if [[ "${classpath_value}" == *$'\n'* ]]; then
    printf '%s\n' "${classpath_value}"
    return 0
  fi
  if [[ "${classpath_value}" == *';'* ]]; then
    printf '%s\n' "${classpath_value//;/$'\n'}"
    return 0
  fi
  # A single Windows path still contains ':' after the drive letter.
  if [[ "${classpath_value}" == [A-Za-z]:[\\/]* ]]; then
    printf '%s\n' "${classpath_value}"
    return 0
  fi
  printf '%s\n' "${classpath_value//:/$'\n'}"
}

dev_runtime_is_system_runtime_jar() {
  local name
  name=$(dev_runtime_path_basename "${1}")
  case "${name}" in
    wta-system-*-sources.jar | wta-system-*-javadoc.jar | wta-system-*-tests.jar)
      return 1
      ;;
    wta-system-*.jar)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

dev_runtime_select_system_jar() {
  local classpath_value=${1}
  local classpath_entry
  local selected=()

  while IFS= read -r classpath_entry || [[ -n "${classpath_entry}" ]]; do
    [[ -n "${classpath_entry}" ]] || continue
    if dev_runtime_is_system_runtime_jar "${classpath_entry}"; then
      selected+=("${classpath_entry}")
    fi
  done < <(dev_runtime_split_classpath "${classpath_value}")

  [[ ${#selected[@]} -eq 1 ]] || return 1
  dev_runtime_canonicalize_path "${selected[0]}"
}

dev_runtime_filter_listening_port() {
  local port=${1}

  [[ "${port}" =~ ^[0-9]+$ ]] || return 1
  grep -Ei "[^0-9]${port}[[:space:]].*LISTEN" || true
}
