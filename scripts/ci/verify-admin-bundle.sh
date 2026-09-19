#!/usr/bin/env bash
set -euo pipefail

mode=${1:-}
case "$mode" in
  full|core) ;;
  *)
    echo "usage: $0 <full|core>" >&2
    exit 2
    ;;
esac

# Archived release builds have no .git directory. An explicit artifact is self-contained.
if [[ -n "${ADMIN_ARTIFACT:-}" ]]; then
  artifact="${ADMIN_ARTIFACT}"
else
  workspace_root=$(git rev-parse --show-toplevel)
  artifact="$workspace_root/backend/wta-admin/target/wta-admin.jar"
fi
if [[ ! -f "$artifact" ]]; then
  echo "missing admin artifact: $artifact" >&2
  exit 1
fi

entries=$(jar tf "$artifact")
required=(wta-system wta-common-notify wta-common-oss wta-third wta-sso wta-notify wta-profile-person wta-profile-enterprise)
optional=(wta-job wta-ai wta-demo wta-workflow)

contains_artifact() {
  local entry pattern="^BOOT-INF/lib/$1-[0-9][^/]*\\.jar$"
  while IFS= read -r entry; do
    [[ "$entry" =~ $pattern ]] && return 0
  done <<< "$entries"
  return 1
}

for name in "${required[@]}"; do
  if ! contains_artifact "$name"; then
    echo "$mode bundle is missing required artifact: $name" >&2
    exit 1
  fi
done

for name in "${optional[@]}"; do
  if [[ "$mode" == "full" ]] && ! contains_artifact "$name"; then
    echo "full bundle is missing optional artifact: $name" >&2
    exit 1
  fi
  if [[ "$mode" == "core" ]] && contains_artifact "$name"; then
    echo "core bundle unexpectedly contains: $name" >&2
    exit 1
  fi
done

echo "$mode bundle contents verified"
