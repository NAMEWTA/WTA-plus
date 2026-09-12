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

workspace_root=$(git rev-parse --show-toplevel)
artifact="${ADMIN_ARTIFACT:-$workspace_root/wta-vue-plus-namewta/wta-admin/target/wta-admin.jar}"
if [[ ! -f "$artifact" ]]; then
  echo "missing admin artifact: $artifact" >&2
  exit 1
fi

entries=$(jar tf "$artifact")
required=(wta-system wta-common-notify wta-common-oss wta-third)
optional=(wta-job wta-ai wta-demo wta-workflow)

contains_artifact() {
  [[ "$entries" == *"BOOT-INF/lib/$1-"* ]]
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
