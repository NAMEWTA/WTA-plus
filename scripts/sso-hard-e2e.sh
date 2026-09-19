#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ "${1:-}" == --release-origin ]]; then
  shift
  exec python3 "${ROOT}/release-artifacts/tests/fixtures/sso-release-origin.py" "$@"
fi
# Same-session three-gate launcher for AC-001/002/003.
# Requires live MySQL, Redis, backend 18080, admin 4174, home 4175, sso-web 4176.

need_port() {
  python3 - "$1" <<'PY'
import socket, sys
port = int(sys.argv[1])
s = socket.socket()
s.settimeout(1)
try:
    s.connect(("127.0.0.1", port))
except OSError as exc:
    print(f"CLOSED 127.0.0.1:{port} ({exc})", file=sys.stderr)
    sys.exit(1)
finally:
    s.close()
print(f"OPEN 127.0.0.1:{port}")
PY
}

need_port 3306
need_port 6379
need_port 18080
need_port 4174
need_port 4175
need_port 4176

cd "$(git rev-parse --show-toplevel)/frontend"
exec pnpm exec playwright test --config playwright.sso.config.ts
