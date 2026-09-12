#!/usr/bin/env bash
set -euo pipefail

workspace_root=$(git rev-parse --show-toplevel)
cd "$workspace_root"

status=$(git submodule status --recursive)
if [[ -z "$status" ]]; then
  echo "No submodules are configured." >&2
  exit 1
fi

while IFS= read -r line; do
  marker=${line:0:1}
  if [[ "$marker" == "+" || "$marker" == "-" || "$marker" == "U" ]]; then
    echo "Submodule checkout does not match the parent commit:" >&2
    echo "$status" >&2
    exit 1
  fi
done <<<"$status"

while read -r sha path _; do
  recorded=$(git ls-tree HEAD "$path" | awk '{print $3}')
  actual=$(git -C "$path" rev-parse HEAD)
  if [[ "$recorded" != "$actual" || "$sha" != "$actual" ]]; then
    echo "Submodule SHA mismatch for $path: parent=$recorded checkout=$actual" >&2
    exit 1
  fi
  if [[ -n $(git -C "$path" status --porcelain --untracked-files=normal) ]]; then
    echo "Submodule working tree is dirty: $path" >&2
    exit 1
  fi
done <<<"$status"

echo "$status"
