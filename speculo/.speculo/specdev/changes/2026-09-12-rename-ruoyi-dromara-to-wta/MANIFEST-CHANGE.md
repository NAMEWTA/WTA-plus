# MANIFEST-CHANGE — authority, gates, baseline pointer

**Change:** `2026-09-12-rename-ruoyi-dromara-to-wta`  
**Updated:** 2026-09-12T11:42:00+08:00 (Asia/Shanghai)  
**Note:** Packer-generated `MANIFEST.md` inside review zips is ephemeral; **this file** is the durable change-dir authority for gates and precedence.

---

## 1. Authority precedence（highest first）

1. **CTO** written decisions / widget confirmations / explicit gate flips  
2. **ADR.md** (especially ADR-010 / ADR-011 as FINAL topology & freeze/orphan/publication)  
3. **spec.md** (implementation contract, AC families, negative ACs)  
4. **goal-plan.md** (orchestration, wave sequencing, false completion)  
5. **tickets-map.md** / `ticket/*` (execution slices; may not reinterpret ADR/spec)

Conflicts: update the owning higher artifact; do not “fix in tickets.”

---

## 2. Gate fields（defaults）

| Field | Value now | Notes |
|---|---|---|
| `documentation_status` | `iterated-p0-closed` | Was `changes-required` after ChatGPT BLOCKED; local P0 absorbed |
| `review_status` | `local-p0-addressed-awaiting-cto` | Not “approved for implement” |
| `implementation_status` | `not-started` | |
| `implementation_authorized` | **false** | |
| `public_repo_publication_authorized` | **false** | |
| `legacy_repo_mutation_authorized` | **false** | |
| `ready_for_execution` | **false** | |
| `documentation_gate` | P0 locally closed; consistency re-review pending | ChatGPT result was BLOCKED |
| `implementation_gate` | **BLOCKED** | |

**Forbidden:** a single undifferentiated `READY` status that collapses review vs authorization.

**Rule:** `CHANGES REQUIRED` / local doc iteration **≠** authorized.

---

## 3. Source baseline pointer

Canonical baseline:  
`<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/SOURCE-BASELINE.md</Path>`

Triple (see file for dirty details):

```text
docs:     cf9842660df2557439e13a3fca27cc2cac155210
backend:  60c9d31f9419e7d7560706a2b6a41ff44b41d551
frontend: d77b55651e5754a54508e51a79f536afbd2392b8
review_zip_sha256: 2a8c4f5205d27a16a43ca4f7484a130fd5a3c4a77ba03ca12a82b07d86f305a6
```

Baseline drift → review invalidated → return to pre-implementation review.

---

## 4. External-brain pointer

- `external-brain/reply.md` — ChatGPT comprehensive review (BLOCKED / P0 list)  
- `external-brain/meta.md` — zip path, model, chat url  
- `external-brain/notes.md` — local disposition of P0 close  

---

## 5. Non-claims

**Naming (LOG-020):** owned Java/Maven `org.dromara` → **`org.namewta`** (not `org.wta`); `ruoyi`→`wta` module prefix unchanged; third-party KEEP in force.

This manifest does **not** claim: code rename done, repos frozen in GitHub settings, public monorepo created, orphan push done, build/E2E green, or any authorization gate true.
