#!/usr/bin/env bash
set -euo pipefail

base_url=${1:?"usage: openapi-route-check.sh <backend-base-url>"}
url="${base_url%/}/system/openApi/self/interfaces"
body=$(curl -sS --max-time 10 "$url")

printf '%s\n' "$body"
if printf '%s' "$body" | rg -q '"code":404'; then
  printf 'RED: OpenAPI route is not assembled\n' >&2
  exit 1
fi
