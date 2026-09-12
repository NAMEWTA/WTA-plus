# Evidence: CR-001 fix — dead `docs/upstream` skill refs

- **Tree:** `/workspace/vp-dev/WTA-plus`
- **Do not restore:** `docs/upstream/` remains absent
- **Force-push:** not used

## Rewrites

Auth/permission/Client/menu rows now cite:

- `AGENTS.md`
- `.agents/skills/engineering-standards/` (project + rules)
- `.agents/skills/namewta-fullstack-development/references/{frontend/permission-routing.md,contract-mapping.md,backend/architecture.md}`

Removed `path:docs/upstream/**` routing and DELIVERY-002 fork-sync/submodule playbook.

## Verify

```text
rg 'docs/upstream' .agents/skills   # ZERO
test ! -d docs/upstream             # ABSENT
```

`node --test release-artifacts/tests/wta-rename-keep-contract.test.mjs` — 7/7; walker fails if `.agents/skills` text contains `docs/upstream`.

KEEP + one root README RuoYi-Vue-Plus line unchanged.

## Push

`git push origin main` (no `--force`): `cdf4d10..138fedc`

| Field | Value |
|---|---|
| remote | https://github.com/NAMEWTA/WTA-plus |
| HEAD | `138fedc4b9ccaca9a7e9d74e6099561572e6324d` |
