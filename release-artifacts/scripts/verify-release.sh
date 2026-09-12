#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${RELEASE_ENV_FILE:-${RELEASE_ROOT}/.env.example}"

info() { printf '[INFO] %s\n' "$*" >&2; }
warn() { printf '[WARN] %s\n' "$*" >&2; }

info "校验 Shell 语法"
for script in "${RELEASE_ROOT}"/scripts/*.sh; do
  bash -n "${script}"
done

info "执行全部发布合同测试"
node --test "${RELEASE_ROOT}"/tests/*.test.mjs

info "执行 Nginx Skill 台账检查"
python3 "${RELEASE_ROOT}/skills/wta-namewta-nginx-config/scripts/add_app.py" \
  --repo-root "${RELEASE_ROOT}" --list >/dev/null

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  info "校验四类 Docker Compose"
  for compose in "${RELEASE_ROOT}"/docker/docker-compose-*.yml; do
    docker compose --env-file "${ENV_FILE}" -f "${compose}" config --quiet
  done
else
  warn "本机没有 Docker Compose，已跳过 Compose 解析；其余验证通过"
fi

info "release-artifacts 验证完成"
