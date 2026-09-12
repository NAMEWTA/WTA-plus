# Evidence: T-18 — LOG-026/027 packet (dirs + skill + narrative)

- **Tree:** `/workspace/vp-dev/WTA-plus` → `origin` `https://github.com/NAMEWTA/WTA-plus.git`
- **Force-push:** not used
- **Legacy remotes:** not mutated

## A. Root dirs

`backend/` and `frontend/` (already on `1e4accd`; this wave keeps them). Maven `org.namewta` / `wta-*` unchanged. KEEP `org.dromara.sms4j|warm|easyes|mica` still present.

Negative-path assertions live in `release-artifacts/tests/wta-rename-keep-contract.test.mjs`.

## B. Skill removal

Deleted:

- `.agents/skills/upstream-fork-sync/`
- `speculo/skills/upstream-fork-sync/`

Stripped `AGENTS.md` routing row; filtered `speculo/.speculo/managed.json` (`735 → 729` files).

## C. Narrative

- Root `README.md` has **one** historical line: `溯源：基于 RuoYi-Vue-Plus / Plus-UI；交付不以 git submodule 或上游 URL 为依赖。`
- Removed `docs/upstream/`, `backend/docs/upstream/`, `frontend/docs/upstream/`
- Dropped fork-sync playbooks from frontend/backend README, `docs/namewta-enhancements.md`, engineering-standards profile (no SUPERSEDED stack)

## Verify (scratch)

- `{SCRATCH}/keep-owned-contract.log` — 7 tests, exit 0
- `{SCRATCH}/rg-packet-gates.txt` — old roots only in negative tests; `docs/upstream` ABSENT; KEEP hits
- `{SCRATCH}/readme-narrative.md` — one RuoYi-Vue-Plus line

## Push

`git push origin main` (no `--force`): `1e4accd..cdf4d10`

| Field | Value |
|---|---|
| remote | https://github.com/NAMEWTA/WTA-plus |
| HEAD | `cdf4d10efc76f53a020d64972488e9afde273855` |
