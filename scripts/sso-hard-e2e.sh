#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ "${1:-}" == --release-origin ]]; then
  shift
  exec node "${ROOT}/release-artifacts/tests/fixtures/sso-release-origin.mjs" "$@"
fi
# Same-session three-gate launcher for AC-001/002/003.
# Requires live MySQL, Redis, backend 38888, admin 4174, home 4175, sso-web 4176.

need_port() {
  node --input-type=module - "$1" <<'JS'
import net from 'node:net';
const port = Number(process.argv[2]);
const socket = net.connect({ host: '127.0.0.1', port });
const timer = setTimeout(() => {
  console.error(`CLOSED 127.0.0.1:${port} (timeout)`);
  socket.destroy();
  process.exit(1);
}, 1000);
socket.on('connect', () => {
  clearTimeout(timer);
  console.log(`OPEN 127.0.0.1:${port}`);
  socket.end();
});
socket.on('error', (error) => {
  clearTimeout(timer);
  console.error(`CLOSED 127.0.0.1:${port} (${error.message})`);
  process.exit(1);
});
JS
}

need_port 3306
need_port 6379
need_port 38888
need_port 4174
need_port 4175
need_port 4176

cd "$(git rev-parse --show-toplevel)/frontend"
exec pnpm exec playwright test --config playwright.sso.config.ts
