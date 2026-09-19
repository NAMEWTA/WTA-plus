#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Linux entry point; release-state owns source isolation, validation and the single rename.
exec python3 "${SCRIPT_DIR}/release-state.py" "$@"
