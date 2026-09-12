---
schema_version: 3
plan_contract_version: 1
plan_revision: 1
requested_deliverables: []
deliverable_policy: "用户未要求额外命名交付物数量；requested_deliverables 为空；本轮仅 SpecDev 正式票（T-tickets），不交付产品构建/公开仓。"
artifact: tickets-map
change: 2026-09-12-rename-ruoyi-dromara-to-wta
status: implementing
implementation_authorized: true
public_repo_publication_authorized: true
legacy_repo_mutation_authorized: true
---

# Tickets Map — 2026-09-12-rename-ruoyi-dromara-to-wta（正式票；CTO HOLD）

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/goal-plan.md</Path>`

> **CTO HOLD：** 三门 `*_authorized=false`。不可开 I-implement / 建仓 / push。  
> **覆盖旧 S-spec 默认（LOG-017/018 / ADR-012/013 / ADR-006 硬切）：** 仓名 **WTA-plus**（`NAMEWTA/WTA-plus`）；布局=**docs 聚合镜像**（非四顶层）；Nacos=**一次性硬切、无双读**（废止 14d/a–e）；上游不需要；旧库用户一律 `wta`（T-17）。  
> **铁律：** 授权 → W0b → rename；禁止旧仓内先 rename。正式 id：`T-00`…`T-17`（T04b→**T-16**）。

## 1. 目标与拆分策略

将 ownership-aware rename + WTA-plus orphan monorepo 合同拆为可授权门禁的垂直切片：Gate/基线 →（授权后）inventory → W0b（布局+freeze）→ rename/runtime/DB → residual/verify → publication → orphan push → legacy transition。

### 总体实施背景

- 权威：CTO > ADR-006/010/011/012/013 > spec > goal-plan > 本 map > tickets。
- 旧三仓 freeze；新 public：**NAMEWTA/WTA-plus**。
- 布局镜像当前 `ruoyi-vue-plus-docs` 聚合树（`ruoyi-vue-plus-namewta` / `plus-ui-namewta` / `docs` / `speculo` / `release-artifacts` / `scripts`…），去 submodule、内容合入。
- Nacos：发版窗口人工迁 data-id 后硬切；**无双读**。
- 前缀 SWAP：`ruoyi-X`→`wta-X`；自有 `org.dromara`→`org.namewta`；KEEP 四件套。
- 写面：HOLD 下仅 SpecDev；授权后仅准备树/批准 remote。
- `implementation_authorized` / `public_repo_publication_authorized` / `legacy_repo_mutation_authorized` = **false**。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 质量门禁与 Java/文档惯例 | Map 后、Ticket 前 | 质量门禁 |
| ALL | `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` | 品牌/合同/前后端边界 | Map 后、Ticket 前 | 合同与品牌 |
| T-05,T-07 | `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | 后端 common；防误伤 sms SDK | 进入 T-05/T-07 前 | KEEP seam |
| T-05,T-06,T-07 | `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>` | 模块地图（实现期或迁名 wta-module-guide） | 进入模块搬迁票前 | 模块地图 |
| T-08 | `<Path>.agents/skills/deploy-namewta-environment/SKILL.md</Path>` | compose/路径/Nacos 硬切窗口 | 进入 T-08 前 | runtime/deploy |

扫描说明：已枚举 `<Path>.agents/skills/*/SKILL.md</Path>`。不适用必读：`upstream-fork-sync` / `project-customization-delivery` / `java-api-compatibility`。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-00 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/00-authorization-gate.md</Path>` | 三门 Gate 投影；Review≠Implement | — | standard | high | yes | unassigned | AC-012, NAC-04 | G-Auth | done |
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/01-source-baseline.md</Path>` | SOURCE-BASELINE 文档可对照 | T-00 | standard | medium | yes | unassigned | AC-009, AC-012 | W-Inv | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/02-ownership-inventory.md</Path>` | evidence/inventory 分类表 | T-01 | standard | high | yes | unassigned | AC-001, AC-002, AC-007, NAC-01 | W-Inv | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/03-keep-inventory.md</Path>` | KEEP 清单可回归 | T-01 | standard | high | yes | unassigned | AC-003, NAC-01, NAC-05 | W-Inv | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/04-monorepo-layout.md</Path>` | docs→WTA-plus 对照+准备树 | T-02,T-03 | deep | critical | yes | unassigned | AC-011, AC-008, NAC-06 | W0b | done |
| T-16 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/16-freeze-old-remotes.md</Path>` | freeze 语义 checklist（alias T04b） | T-00 | deep | critical | yes | unassigned | AC-010, AC-011, NAC-03 | W0b | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/05-backend-org-wta.md</Path>` | 自有 org.namewta；KEEP 绿 | T-04,T-16 | standard | critical | yes | unassigned | AC-002, AC-003, NAC-01, NAC-06 | W-rename | done |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/06-fe-docs-rename.md</Path>` | FE/docs OWNED + 去上游 URL | T-04 | standard | high | yes | unassigned | AC-001, NAC-01 | W-rename | done |
| T-07 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/07-prefix-wta-x.md</Path>` | ruoyi-X→wta-X | T-05 | standard | high | yes | unassigned | AC-004, NAC-01 | W-rename | done |
| T-08 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/08-nacos-hard-cut.md</Path>` | 硬切对照+发版窗口 checklist | T-05,T-06,T-07 | deep | critical | yes | unassigned | AC-005, AC-006 | W-runtime | done |
| T-17 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/17-db-user-migrate.md</Path>` | 旧库用户 ruoyi→wta + 回滚 | T-08 | deep | high | yes | unassigned | AC-001, AC-005 | W-runtime | done |
| T-09 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/09-residual.md</Path>` | residual 分类报告 | T-08 | standard | medium | yes | unassigned | AC-007, NAC-05 | W-verify | done |
| T-10 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/10-build-keep-verify.md</Path>` | build+KEEP 日志 | T-09 | standard | high | yes | unassigned | AC-001, AC-002, AC-003, AC-004 | W-verify | done |
| T-11 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/11-e2e-optional.md</Path>` | E2E 或 waiver | T-10 | standard | medium | yes | unassigned | AC-005 | W-verify | done |
| T-12 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/12-publication-readiness.md</Path>` | publication checklist（含清 .gitee） | T-10,T-11 | standard | critical | yes | unassigned | AC-009 | W-PubReady | done |
| T-13 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/13-pub-auth.md</Path>` | CTO publication auth 记录 | T-12 | standard | critical | yes | unassigned | AC-009, AC-012 | G-Auth-Pub | done |
| T-14 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/14-orphan-wta-plus.md</Path>` | orphan push NAMEWTA/WTA-plus | T-13 | deep | critical | yes | unassigned | AC-008, AC-009, AC-011 | W-Pub | done |
| T-15 | `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ticket/15-legacy-freeze.md</Path>` | 旧仓 freeze transition | T-14 | deep | high | yes | unassigned | AC-010 | W-Legacy | done |

Ticket frontmatter 是状态/依赖/深度/路径权威；本表是同步投影。T-02 起均为 **blocked-by-auth**（`ready: false`）。

## 3. 依赖 DAG

```text
T-00 [READY] Authorization Gate
  |
  +-- T-01 [READY] Source Baseline
  |     |
  |     +-- T-02 [BLOCKED-auth] Ownership Inventory
  |     |
  |     +-- T-03 [BLOCKED-auth] KEEP Inventory
  |
  +-- T-16 [BLOCKED-auth] Freeze-old-remotes (alias T04b)
  |
  [implementation_authorized]
  |
  T-02 + T-03 --> T-04 [BLOCKED-auth] Monorepo layout (docs mirror → WTA-plus)
  T-16 --------/
        |
     [W0b complete — no legacy-mainline rename]
        |
        +-- T-05 Backend org.namewta
        +-- T-06 FE/docs + no upstream URL
        |
        T-05 --> T-07 Prefix wta-X
        |
        T-05 + T-06 + T-07 --> T-08 Nacos hard-cut
                                |
                                +-- T-17 DB user migrate
                                +-- T-09 Residual
                                      |
                                     T-10 Build+KEEP
                                      |
                                      +-- T-11 E2E optional
                                      |
                                     T-10 + T-11 --> T-12 Publication readiness (.gitee)
                                                      |
                                                     T-13 Pub auth (CTO)
                                                      |
                                                     T-14 Orphan NAMEWTA/WTA-plus
                                                      |
                                                     T-15 Legacy freeze transition
```

**铁律边：** `I-auth → (T-04|T-16) → T-05…`；禁止 `T-05` 依赖「旧仓已 rename」。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 (AC-NAME) | T-02,T-06,T-09,T-17 | residual + docs + DB | covered | OWNED 品牌/用户 |
| AC-002 (AC-PACKAGE) | T-05,T-10 | compile + rg | covered | org.namewta（LOG-020；非 org.wta） |
| AC-003 (AC-KEEP) | T-03,T-05,T-10 | KEEP rg | covered | sms4j/warm/… |
| AC-004 (AC-PREFIX) | T-07,T-10 | dir/artifact map | covered | wta-X |
| AC-005 (AC-RUNTIME) | T-08,T-11,T-17 | mapping + hard-cut | covered | 标识一致 |
| AC-006 (硬切门禁) | T-08 | 发版窗口/回滚 | covered | **无双读**；非 a–e |
| AC-007 (AC-RESIDUAL) | T-09 | residual report | covered | 非 zero-match |
| AC-008 (AC-REPOSITORY) | T-04,T-14 | ORPH-* | covered | orphan |
| AC-009 (AC-PUBLICATION) | T-01,T-12,T-13,T-14 | publication evidence | covered | 含清 .gitee |
| AC-010 (AC-LEGACY) | T-16,T-15 | freeze proof | covered | legacy auth |
| AC-011 (AC-W0b) | T-04,T-16,T-14 | prep + WTA-plus | covered | docs 镜像布局 |
| AC-012 (AC-Gate) | T-00,T-13 | status gates | covered | Review≠Implement |
| NAC-01…06 | T-02–T-17 | process | covered | 负向合同 |
| ADR-012 | T-04,T-06,T-12,T-14 | slug/layout/url/.gitee | covered | WTA-plus |
| ADR-013 | T-17 | DB migrate | covered | 旧库用户 |
| ADR-006 硬切 | T-08 | window checklist | covered | LOG-018 |

无 `uncovered` 合同空洞。

## 5. 并行与路径所有权

- SpecDev：单一 writer 改 change 目录；T-00/T-01 仅文档。
- 授权后 W0b：T-04 与 T-16 可有限并行（写面不重叠：准备树 vs freeze 文档/意图）。
- Rename：T-05→T-07 串行优先；T-06 可与后端子波有限并行。
- T-08 后：T-17 与 T-09 可串行（本 map 令 T-17 依赖 T-08；T-09 依赖 T-08）。
- Publication/legacy：T-12→T-15 严格串行。
- current-workspace 默认串行；implementation writer=1。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-02 | T-03 | evidence/inventory 可能交集 | 否（皆依赖 T-01） | HOLD 下均 blocked；授权后串行或划分子路径 |
| T-05 | T-06 | 无强制交集 | 否 | 可有限并行 |
| T-04 | T-16 | 无产品树交集 | 否 | W0b 可并行文档/准备 |

## 6. Gate、Wave 与集成点

- G-Auth：T-00；G-Auth-Pub：T-13；Legacy auth：T-15 前置。
- W0b：T-04 + T-16。
- W-rename：T-05…T-07；W-runtime：T-08/T-17；W-verify：T-09…T-11；W-Pub：T-12…T-14；W-Legacy：T-15。
- E2E：T-11 默认 not-required；Lead/current-workspace 或 parent-candidate。

## 7. 横切契约与风险

- KEEP 回归每 rename 波次；禁止 zero-match AC。
- 硬切失败即停；回滚旧 id。
- Publication 前清 `.gitee`；无上游 URL 依赖。
- Baseline drift → re-review。
- resource_claims 见各票；语义资源冲突时串行。

## 8. 同步规则

- Ticket 状态变化后同步本表；
- 以 Ticket frontmatter 为权威；
- Skill 变更先更新矩阵再校验；
- Goal Plan 存在时 Wave/Gate 以 goal-plan 编排权威；
- 变更后运行 validate-specdev `--stage tickets` 与 ticket-control `--map`。

## 9. 总控与恢复

从本 Map 进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 的 plan/run/resume（**仅授权后**）。先运行 `<Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path>` `--map` 只读检查。

- `requested_deliverables=[]`；deliverable_policy 已记录无额外数量要求。
- HOLD 恢复：保持三门 false；T-02+ 维持 blocked-by-auth。
- Nacos 失败：切回旧 id 停窗口（无双读）。
- Baseline drift：回 T-01/re-review。
- 外脑 T-tickets zip Auto-review blocked **不豁免**实施门禁。

## 10. Status

`status: ready` — 正式 `ticket/*.md` 已落地（18 票：T-00…T-17）。I-implement / publication / legacy mutation **未授权**。仅 T-00/T-01 `ready: true`（文档闸）；其余 blocked-by-auth。
