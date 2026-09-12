---
schema_version: 6
artifact: goal-plan
change: 2026-09-12-rename-ruoyi-dromara-to-wta
status: draft
modes: [migration, high-assurance]
orchestration: lead-directed
lead: rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00
implementation_agent_limit: 2
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
documentation_status: iterated-p0-closed
review_status: local-p0-addressed-awaiting-cto
implementation_authorized: false
public_repo_publication_authorized: false
legacy_repo_mutation_authorized: false
---

# Goal Plan: 自有 ruoyi/dromara → wta（hard-gated）

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Baseline：** `<Path>{roots.state}/specdev/changes/{change}/evidence/SOURCE-BASELINE.md</Path>`
- **External brain：** `<Path>{roots.state}/specdev/changes/{change}/external-brain/</Path>`

> Authority: **CTO > ADR > spec > this goal-plan > tickets**.  
> ChatGPT `BLOCKED` / `CHANGES REQUIRED` never implies authorization.

## 0. Gate fields（default ALL false / blocked）

| Field | Default | Meaning |
|---|---|---|
| `documentation_status` | was `changes-required` → now **`iterated-p0-closed`** | Local P0 doc close done; consistency re-review still needed |
| `review_status` | **`local-p0-addressed-awaiting-cto`** | Awaiting CTO + optional re-review |
| `implementation_authorized` | **false** | No product rename / monorepo prepare execution |
| `legacy_repo_mutation_authorized` | **false** | No freeze-transition writes on old remotes |
| `public_repo_publication_authorized` | **false** | No public create/push |
| `ready_for_execution` | **false** | Execution-type Ready forbidden |

**Rule:** Unless `implementation_authorized == true`, no implementation action is permitted.  
**Rule:** Unless `public_repo_publication_authorized == true`, no public push.  
**Rule:** **No public push before publication gate.**

## 1. Mandatory Project Skill Gate（预告）

未按顺序读完项目 Skill，不得改产品代码、不得宣称 Ticket / I-implement 开始。

1. 本 Goal Plan  
2. tickets-map Skill 矩阵  
3. 适用 `ALL` / Ticket Skill 全文  
4. Ticket「必须加载的 Skill」  
5. Ticket 其余章节  

| Applies To | Skill（预告） | Purpose |
|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 质量门禁 |
| ALL | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 前后端合同 |
| ALL | `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`（迁后 wta-module-guide） | 模块地图 |
| Backend rename | `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | 防误伤 SDK |
| Docker | `<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>` | compose/路径 |

**本轮只改 SpecDev 文档。**

## 2. Outcome and Authority

### Outcome

1. Ownership-aware rename → `wta`（ruoyi 名型）/ **`org.namewta`**（自有 dromara 包/group；LOG-020）/ `wta-*`；KEEP sms4j/warm-flow/easy-es/mica-mqtt（永不→org.namewta.*）。  
2. Legacy freeze；新 public monorepo orphan（slug 待 CTO）。  
3. Runtime/Nacos 契约；publication gate 后才公开。

### False completion

- Blind global replace / 误伤第三方  
- 只改目录不改 package/扫描  
- 无限期双读 / 未授权改 Nacos 生产  
- 裸名目录而非 `wta-X`  
- 旧仓 filter-repo/force-push  
- 新仓带旧 `.git`/缓存  
- 文档未收敛或未授权就建仓/改代码  
- 把 review BLOCKED 当成 authorized  
- 伪造票糊绿 validate  

### Authoritative Inputs

| Pri | Source | Conflict |
|---|---|---|
| 1 | CTO written decisions / gate flips | Update owning artifact |
| 2 | ADR.md (010/011 FINAL topology) | Supersedes older ADR-005 topology text |
| 3 | spec.md | Downstream must not rewrite AC |
| 4 | evidence SOURCE-BASELINE / SURVEY / KEEP | No invented paths |
| 5 | this goal-plan | Orchestration only |
| 6 | tickets-map / ticket/* | No formal tickets yet |

## 3. Hard sequence（waves with gates）

```text
[Doc] Close P0 docs (this iteration)
  → [Doc] Documentation consistency re-review
  → [Gate] CTO: implementation_authorized?
        ├─ false: STOP (SpecDev only)
        └─ true:
             inventory (T02/T03) using baseline
             → prepare monorepo tree (T04) — still NOT public
             → rename waves (T05–T08)
             → residual classification (T09)
             → verify static/build (+ optional E2E) (T10/T11)
             → publication readiness (T12)
             → [Gate] public_repo_publication_authorized?
                   ├─ false: STOP (artifacts stay private/local)
                   └─ true: orphan public push (T14) after T13 auth record
             → [Gate] legacy_repo_mutation_authorized?
                   └─ true: legacy freeze transition (T15)
```

**Forbidden sequencing:** public first → clean later.

### Wave table

| Wave | Content | Opens when | Hard gate / evidence |
|---|---|---|---|
| W-Doc | P0 close + consistency re-review | always (SpecDev) | documentation_status iterated; review_status updated |
| W-Inv | Ownership + KEEP inventories | `implementation_authorized` | Inventory evidence cites baseline SHAs |
| W-Prep | Prepare monorepo tree (local/private) | W-Inv | Exclude caches; no public remote required yet |
| W-Ren | Backend NS / FE-docs / prefix / runtime | W-Prep | AC-NAME/PACKAGE/PREFIX/RUNTIME; AC-KEEP |
| W-Res | Residual classification | W-Ren | AC-RESIDUAL |
| W-Ver | Static/build/(E2E) | W-Res | Compile + KEEP rg |
| W-PubReady | Publication checklist | W-Ver | Publication gate checklist complete |
| W-Pub | Orphan public push | `public_repo_publication_authorized` + W-PubReady | AC-PUBLICATION / AC-REPOSITORY |
| W-Legacy | Legacy freeze transition | `legacy_repo_mutation_authorized` | AC-LEGACY |

Legacy labels W0/W0b/W1–W5 map into the above; **do not** schedule W0b public push before W-PubReady.

## 4. Work routing

```text
P-goal-plan → R-review-architecture → S-spec → T-tickets
  → (implementation_authorized) I-implement → C-code-review
  → (publication_authorized) publish
  → (legacy_mutation_authorized) legacy transition
```

| Work | Status now |
|---|---|
| P / R | done (local + ChatGPT ingest BLOCKED on docs) |
| S-spec | **iterating** — P0 absorbed into spec/ADR/CONTEXT |
| T-tickets | outline T00–T15 only；无 `ticket/*.md` |
| I / publish / legacy | **blocked** |

## 5. Authorization Matrix（all not-authorized）

| Action | Status |
|---|---|
| Product code rename | not-authorized |
| Prepare monorepo tree execution | not-authorized |
| Create/push public repo | not-authorized |
| Legacy remote mutation | not-authorized |
| Cloud Agent / CTO machine | not-authorized (permanent ban for this change) |
| Old-repo filter-repo / force-push | not-authorized (permanent) |

## 6. Gates detail

| Gate | Open | Close evidence | Blocks |
|---|---|---|---|
| G-Doc-P0 | ChatGPT reply ingested | P0-01…08 addressed in SpecDev | Claiming ready_for_execution |
| G-Doc-Consistency | P0 closed | Re-review pass (pending) | CTO implement ask should follow |
| G-Auth-I | CTO written | `implementation_authorized=true` in status | T04–T12 execution |
| G-PubReady | T12 checklist | evidence/publication/* | T14 |
| G-Auth-Pub | CTO written | `public_repo_publication_authorized=true` | Public push |
| G-Auth-Legacy | CTO written | `legacy_repo_mutation_authorized=true` | T15 |

Publication gate minimum: baseline, secret scan, forbidden-file, KEEP verify, license, residual classification, explicit auth.

## 7. Ticket quick reference

See `tickets-map.md` T00–T15. **T05–T15 MUST NOT start** merely because a review exists. T00 is documentation/gate tracking; implementation tickets blocked on `implementation_authorized`.

## 8. Constraints (non-negotiable)

- KEEP third-party per ADR-003 / spec classification  
- Prefix swap `wta-` only  
- Freeze / orphan / publication sequencing per ADR-010  
- No blind sed  
- Baseline drift invalidates review  

## 9. AMBIGUOUS still needing CTO

1. Exact public slug (`NAMEWTA/wta` vs `NAMEWTA/namewta` / other)  
2. Monorepo canonical layout  
3. Nacos dual-read duration + exit metrics  
4. Old-DB username migrate strategy  
5. Upstream / `repository.url` policy  
6. Written flips of the three auth gates after consistency re-review  

## 10. Progress

| Item | State |
|---|---|
| ChatGPT review | Ingested; **BLOCKED** / CHANGES REQUIRED |
| Local P0 close | **Done** (this iteration) |
| ready_for_execution | false |
| implementation_authorized | false |
| public_repo_publication_authorized | false |
| legacy_repo_mutation_authorized | false |
| Next | Documentation consistency re-review → ask CTO for auth |

## 11. Resume

Read: this plan, `.status.json`, LOG, SOURCE-BASELINE, external-brain notes/reply, tickets-map. Resume from last doc gate — **never** from invented implementation checkpoint.

> **LOG-018：** Nacos 为**一次性硬切（无双读）**；发版窗口人工迁配置。原「兼容期双读/退出门禁」表述以 ADR-006 为准废止。
