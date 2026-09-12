# SOURCE-BASELINE — immutable review input baseline

**Captured at:** 2026-09-12T11:11:05+08:00 (Asia/Shanghai) / 2026-09-12T03:11:05Z  
**Purpose:** Prove which source commits / remotes underpinned the ChatGPT pre-implementation review and the local P0 documentation close.  
**Rule:** Any later baseline drift (new commits on these HEADs, unexpected dirty paths in product trees, remote URL change) **invalidates** this review cycle → return to pre-implementation review before any implementation or publication.

> This file is **planning / provenance evidence only**. It does **not** claim freeze, rename, monorepo create, orphan push, build, or E2E completion.

---

## 1. Aggregate / docs repo — `ruoyi-vue-plus-docs`

| Field | Value |
|---|---|
| Path | `/workspace/vp-dev/ruoyi-vue-plus-docs` |
| Remote `origin` (fetch/push) | `https://github.com/NAMEWTA/ruoyi-vue-plus-docs.git` |
| HEAD commit SHA | `cf9842660df2557439e13a3fca27cc2cac155210` |
| HEAD subject / commit time (UTC) | `chore(specdev): archive outbox wake + richtext AutoMapper; bump submodules` @ 2026-09-12 02:10:22 +0000 |
| Dirty? | **YES** |

### `git status --porcelain` summary (root)

```text
 M ruoyi-vue-plus-namewta
 M speculo/.speculo/specdev/status.json
?? speculo/.speculo/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/
```

**Interpretation:** SpecDev change artifacts and status are uncommitted (expected for this documentation wave). Submodule pointer dirty because backend submodule has a local dirty file (see §2). **No product rename commits.**

### Submodules (from `.gitmodules`)

| Path | URL |
|---|---|
| `ruoyi-vue-plus-namewta` | `https://github.com/NAMEWTA/ruoyi-vue-plus-namewta.git` |
| `frontend` | `https://github.com/NAMEWTA/plus-ui-namewta.git` |

---

## 2. Backend submodule — `ruoyi-vue-plus-namewta`

| Field | Value |
|---|---|
| Path | `/workspace/vp-dev/ruoyi-vue-plus-docs/ruoyi-vue-plus-namewta` |
| Remote `origin` | `https://github.com/NAMEWTA/ruoyi-vue-plus-namewta.git` |
| HEAD commit SHA | `60c9d31f9419e7d7560706a2b6a41ff44b41d551` |
| HEAD subject / commit time (UTC) | `fix(demo): add AutoMapper for rich text list SummaryVo` @ 2026-09-11 16:56:02 +0000 |
| Dirty? | **YES** (1 path) |

### Porcelain

```text
 M ruoyi-admin/src/main/resources/application-local.yml
```

**Note:** Local env override dirty; **not** part of rename implementation. Drift of this file alone does not authorize rename; any *new* rename-related dirty or HEAD move invalidates baseline for implementation gate.

---

## 3. Frontend submodule — `frontend`

| Field | Value |
|---|---|
| Path | `/workspace/vp-dev/ruoyi-vue-plus-docs/frontend` |
| Remote `origin` | `https://github.com/NAMEWTA/plus-ui-namewta.git` |
| HEAD commit SHA | `d77b55651e5754a54508e51a79f536afbd2392b8` |
| HEAD subject / commit time (UTC) | `chore(admin-web): regenerate components.d.ts for ElSpace` @ 2026-09-12 02:10:07 +0000 |
| Dirty? | **NO** (`git status --porcelain` empty) |

---

## 4. Review pack / ingest provenance

| Artifact | Path | Notes |
|---|---|---|
| Review zip (meta.zip_path) | `/workspace/vp-dev/ruoyi-vue-plus-docs/temp/chatgpt-packs/20260912T024033Z-P-goal-plan-review.zip` | Used for ChatGPT 6 Pro comprehensive review |
| ZIP SHA-256 | `2a8c4f5205d27a16a43ca4f7484a130fd5a3c4a77ba03ca12a82b07d86f305a6` | Computed at baseline capture |
| Ingest reply | `temp/chatgpt-ingest/20260912-rename-wta-review/reply.md` | Promoted → `external-brain/reply.md` |
| Ingest meta | `temp/chatgpt-ingest/20260912-rename-wta-review/meta.md` | Promoted → `external-brain/meta.md` |
| Chat URL (from meta) | `https://chatgpt.com/c/6aa4bc36-7e54-83ea-be73-a1cb4795305c` | External session reference only |
| Model | GPT-6 Pro (UI showed Pro thinking) | per meta |

---

## 5. Baseline triple (quick copy)

```text
docs:     cf9842660df2557439e13a3fca27cc2cac155210  https://github.com/NAMEWTA/ruoyi-vue-plus-docs.git
backend:  60c9d31f9419e7d7560706a2b6a41ff44b41d551  https://github.com/NAMEWTA/ruoyi-vue-plus-namewta.git
frontend: d77b55651e5754a54508e51a79f536afbd2392b8  https://github.com/NAMEWTA/plus-ui-namewta.git
review_zip_sha256: 2a8c4f5205d27a16a43ca4f7484a130fd5a3c4a77ba03ca12a82b07d86f305a6
```

---

## 6. Invalidation triggers

Invalidate this baseline (and any review that cites it) if any of:

1. HEAD of docs / backend / frontend moves away from the SHAs above without a new documented baseline + re-review.
2. Product trees gain rename/implementation commits or mass dirty renames before `implementation_authorized=true`.
3. Remote URLs for the three legacy remotes change without CTO-authorized freeze transition.
4. Review zip bytes change (SHA-256 mismatch) while still claiming this review result.

**Reminder:** Documentation edits under `speculo/.speculo/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/` are expected and do **not** by themselves invalidate the *product* baseline SHAs above; they do require LOG / status updates.
