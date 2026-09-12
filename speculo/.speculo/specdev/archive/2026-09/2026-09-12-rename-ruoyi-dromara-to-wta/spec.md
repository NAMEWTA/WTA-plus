---
schema_version: 3
artifact: spec
change: 2026-09-12-rename-ruoyi-dromara-to-wta
status: ready
ready_for_tickets: true
documentation_status: iterated-p0-closed
implementation_authorized: false
public_repo_publication_authorized: false
legacy_repo_mutation_authorized: false
sources:
  - CTO-INTENT:2026-09-12-rename-ruoyi-dromara-to-wta
  - ADR-001
  - ADR-002
  - ADR-003
  - ADR-004
  - ADR-005
  - ADR-006
  - ADR-007
  - ADR-008
  - ADR-009
  - ADR-010
  - ADR-011
  - ADR-012
  - ADR-013
  - R-REVIEW:architecture-review.md
  - USER-DECISION:S-spec-local-2026-09-12
  - CHATGPT-REVIEW:20260912-rename-wta-review
  - NOTE:S-spec-round-ChatGPT-upload-Auto-review-blocked-2026-09-12
  - EVIDENCE:evidence/SURVEY.md
  - EVIDENCE:evidence/THIRD-PARTY-KEEP.md
  - EVIDENCE:evidence/SOURCE-BASELINE.md
  - CODE:ruoyi-vue-plus-namewta/pom.xml
  - CODE:plus-ui-namewta/package.json
  - CODE:release-artifacts/docker/docker-compose-backend.yml
---

# Spec: 自有 ruoyi/dromara 命名迁到 wta（Implementation Contract）

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/spec.md</Path>`
- **ADR：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/ADR.md</Path>`
- **CONTEXT：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/CONTEXT.md</Path>`
- **Baseline：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/evidence/SOURCE-BASELINE.md</Path>`
- **R-review：** `<Path>{roots.state}/specdev/changes/2026-09-12-rename-ruoyi-dromara-to-wta/architecture-review.md</Path>`

> **CHANGES REQUIRED / documentation iterated / S-spec ready_for_tickets ≠ implementation authorized.**  
> Authority: CTO > ADR > this spec > goal-plan > tickets.  
> S-spec 本轮 ChatGPT 上传 Auto-review **blocked**（见 `external-brain/notes.md`）；本地定稿**不豁免**实施门禁。

---

## 0. HARD CONSTRAINTS（NON-NEGOTIABLE）

| ID | Constraint |
|---|---|
| HC-01 | First-party product identifiers `ruoyi`/`RuoYi` → `wta`/`WTA` per inventory — **not** blind global replace |
| HC-02 | First-party Java/Maven `org.dromara` (owned) → `org.namewta`; third-party `org.dromara.<product>` **KEEP** |
| HC-03 | Third-party KEEP covers coordinates, packages, npm, upstream URLs, license/NOTICE, middleware product names, protocol ids, copyright, historical attribution |
| HC-04 | Owned module/artifact prefix **SWAP** `ruoyi-` → `wta-` (not bare names) |
| HC-05 | Legacy remotes **freeze** (ADR-010 definition); no force-push / rewrite / default-mainline rename commits |
| HC-06 | Deliver as **new public monorepo** with **orphan** history (ADR-010/011); default slug/layout per DEC-SLUG / DEC-LAYOUT |
| HC-07 | Nacos/runtime naming: **hard-cut no dual-read** (LOG-018); default user `wta`; seed literals owned-rename; mapping table required before cutover |
| HC-08 | **No implementation** until `implementation_authorized=true` |
| HC-09 | **No public push** until publication gate + `public_repo_publication_authorized=true` |
| HC-10 | **No legacy repo mutation** (archive/README/protection changes) until `legacy_repo_mutation_authorized=true` |
| HC-11 | KEEP sms4j / warm-flow / easy-es / mica-mqtt — locked |
| HC-12 | Global zero-match of `ruoyi` / `org.dromara` is **NOT** an acceptance criterion |
| HC-13 | Iron order: **authorization → W0b (freeze/layout/orphan prep) → rename waves**; **forbid** rename-inside-legacy then merge |

---

## 1. 问题与目标

### 问题陈述

NAMEWTA 在前端 `@namewta/*` 与 Docker `namewta-*` 上已部分品牌化，但后端仍以 `org.dromara` + `ruoyi-*` 为主坐标。同时引入 Dromara 生态第三方（sms4j、warm-flow、easy-es、mica-mqtt）。无分类全局替换会破坏第三方依赖与编译；过早公开 push 会泄露未清理内容；在旧三仓内先 rename 再合仓会把双品牌债与误操作面带进新仓。

### 目标用户与场景

- **产品/品牌 owner：** 公开面与运行时坐标统一为 WTA/NAMEWTA，溯源叙述仍可保留「基于 RuoYi-Vue-Plus」。
- **实现/运维：** 在授权后按 inventory + KEEP + W0b→rename 波次迁移；Nacos 双读有界可退出。
- **审阅/CTO：** 文档合同可拆票；三门授权独立翻转；外脑 blocked ≠ 豁免。

### 目标

1. Ownership-aware rename：自有 → `wta` / `org.namewta` / `wta-*`；第三方 KEEP。  
2. 旧三仓 freeze；内容进入全新 public monorepo（orphan）；slug **`NAMEWTA/WTA-plus`**；布局镜像 docs。  
3. Runtime/Nacos 契约一致（**一次性硬切，无双读**）；publication gate 后才公开。

### 成功标准（仅在授权实施并完成验证后）

见 §4 验收合同 / AC families。本轮仅规格定稿；**I-implement 未授权。**

### 非目标

见 §5 OUT / OOS。

---

## 2. 解决方案与外部行为

### 解决方案摘要

1. **Classification-first：** 每个 `ruoyi` / `org.dromara` 命中 MUST KEEP / MUST RENAME / MUST REVIEW（§2.1）。  
2. **Inventory-first：** namespace ownership inventory（T02）后才可自动迁移。  
3. **W0b 先于 rename：** 授权后冻结旧 remote 语义、准备 monorepo 树（`backend/`/`frontend/`/`docs/`/`speculo/`）、orphan 历史；禁止旧仓内先 rename。  
4. **Runtime：** 旧→新对照表 + 自首次双读上线起 **14 个自然日（UTC+8）** 默认兼容期 + 退出勾选清单（§2.3）。  
5. **Publication / legacy：** 清理与门禁通过且显式授权后才 public push / 旧仓 mutation。

### 主要流程

```text
documentation P0 closed → S-spec ready_for_tickets → T-tickets outline
  → (implementation_authorized) W0b: freeze semantics + monorepo layout + orphan prep
  → rename waves (backend/FE/docs/prefix/runtime)
  → residual + verify
  → publication readiness → (public_repo_publication_authorized) orphan push
  → (legacy_repo_mutation_authorized) legacy freeze transition
```

### 边界、失败与稳定错误行为

- KEEP 误伤 → 编译/运行失败；以 AC-KEEP + NAC-01 回滚该波次，不得「修第三方坐标」。  
- Nacos 硬切失败 → 按 checklist **回滚**旧构建+旧 data-id；**完成态禁止运行时双读**（DEC-NACOS / AC-Nacos-Cutover）。  
- Baseline drift（相对 SOURCE-BASELINE）→ 使本轮 review/实施门禁失效，须 re-review。  
- 文档门禁关闭 / Spec ready ≠ 实施授权（AC-Gate / NAC-04）。

### 状态转换与不变量

| 状态 | 不变量 |
|---|---|
| SpecDev / tickets | 产品树未改；三门 `*_authorized=false` |
| W0b | 仅新准备树写面；旧 remote 无 rename 主线提交 |
| Rename waves | 仅 OWNED；KEEP 字节级不变 |
| Dual-read | 写/新环境优先新 id；旧 id 只读窗口有界 |
| Exit dual-read | 退出清单 a–e 全勾选（或 CTO 书面等价） |
| Public | publication gate + `public_repo_publication_authorized` |
| Legacy mutation | 仅 `legacy_repo_mutation_authorized` 后 |

### 2.1 Rename Classification

Every hit of `ruoyi` / `RuoYi` / `org.dromara` MUST be classified before mutation:

#### MUST KEEP

- Maven/npm third-party coordinates for sms4j, warm-flow, easy-es, mica-mqtt (and future same-class)
- Java imports / packages `org.dromara.sms4j.**`, `org.dromara.warm.**`, `org.dromara.easyes.**`, `org.dromara.mica.mqtt.**`
- License / NOTICE / copyright attribution to upstream
- Upstream project URLs when used as attribution
- Third-party product names in protocol/config where the product owns the token
- Historical narrative “based on RuoYi-Vue-Plus” when marked attribution (allowed residual)

#### MUST RENAME

- First-party Maven `groupId=org.dromara` + `ruoyi-*` artifact/module dirs → `org.namewta` + `wta-*`
- First-party Java packages `org.dromara.{common,system,admin,...}` (owned tree) → `org.namewta.*`
- Owned brand titles, container workdirs, skill product prefixes, owned seed literals
- Owned Nacos data-id / application / registration names per runtime contract

#### MUST REVIEW

- Comments mentioning both owned wrapper and third-party product
- Mixed strings (path + attribution)
- `repository.url` / pom `<url>` / author lines（处置：待 CTO，见 §12）
- DB rows in existing deployments（旧库用户策略：待 CTO）
- Ambiguous seeds overlapping third-party demos

**Explicit:** Achieving repository-wide zero matches for `ruoyi` or `org.dromara` is **forbidden as a goal** and **not AC**.

### 2.2 Namespace ownership inventory（required before auto-migrate）

Before any automated package/coord migration:

1. Enumerate occurrences of `org.dromara` / `ruoyi` (scoped excludes: `node_modules`, `target`, `.git`, lockfile noise).  
2. For each class of hit, record: path pattern, example, classification (KEEP / RENAME / REVIEW), owner, disposition.  
3. Only **first-party** entries may be auto-migrated to `org.namewta` / `wta-`.  
4. Inventory evidence lands under `evidence/` (planning → inventory evidence); must cite `SOURCE-BASELINE` SHAs.  
5. Drift from baseline SHAs → re-review before continue.

Ticket: **T02** (blocked until `implementation_authorized` for *execution*; documentation of inventory *format* may proceed in SpecDev).

### 2.3 Nacos / Runtime Naming Contract（ADR-006 量化）

#### In-scope identifiers (must stay mutually consistent after migration)

| Concern | Examples / notes |
|---|---|
| `spring.application.name` | owned app id |
| Nacos Data ID | e.g. `ruoyi-namewta.yml` → `wta-…` |
| Nacos Group (if owned product segment) | per inventory |
| Service registration name | discovery |
| Gateway `lb://` / route service id | must match registration |
| Shared config references | imports of data-id |
| Profile-specific config files | `application-*.yml` owned names |
| Deployment / container / compose service names | Docker **product** services stay `namewta-*` unless inventory says otherwise; **paths** `/ruoyi`→`/wta` when owned |
| Default username | `ruoyi`→`wta` (ADR-007；旧库策略待 CTO) |

#### DEC-NACOS — Hard-cut（LOG-018 / ADR-006）

| Field | Value |
|---|---|
| Strategy | **一次性硬切，无双读** |
| Window | 约定**发版窗口**内人工将 Nacos 配置从旧 data-id **复制/迁移**到新 data-id，再切应用只认新 id |
| Dual-read at runtime | **FORBIDDEN**（完成态） |
| Failure recovery | 按 checklist **回滚**（旧构建 + 旧 data-id）；不引入长期双读 |
| Mapping table | 发版前必须备齐旧→新对照；无对照表不得硬切 |

#### 对照表形状（冻结；行 inventory 实施时补全）

推荐命名规则：自有段 `ruoyi-…` → `wta-…`（前缀 SWAP，非裸名）。

| Kind | Old (known / example) | New (proposed rule) | Compat | Status |
|---|---|---|---|---|
| Nacos data-id | `ruoyi-namewta.yml` | `wta-namewta.yml`（或 inventory 确认的 `wta-*.yml`） | hard-cut window | EXAMPLE — inventory 补全 |
| spring.application.name | *(inventory)* | owned `ruoyi*`→`wta*` | — | TBD inventory |
| Registration / discovery | *(inventory)* | 与 application.name 一致 | — | TBD inventory |
| Gateway lb route | *(inventory)* | 与 registration 一致 | — | TBD inventory |
| Shared config ref | *(inventory)* | 指向新 data-id | hard-cut | TBD inventory |
| Profile config filename | *(inventory)* | owned rename | — | TBD inventory |
| Container workdir | `/ruoyi/...` | `/wta/...` | — | EXAMPLE |
| Compose service name | `namewta-*` | **KEEP** `namewta-*`（默认） | — | **LOCKED** |
| Default user | `ruoyi` | `wta` | 旧库策略待 CTO | ADR-007 |

> 表形状（列：Kind / Old / New / Compat / Status）冻结。已知示例行保留；其余行由 T02/T08 inventory **补全**，不得在无对照表时生产硬切。

#### 发版窗口硬切勾选清单（全部满足才可切流量）

| # | Check | Required |
|---|---|---|
| a | 旧→新 data-id **对照表**已冻结且 Nacos 新 id 内容已人工迁移/核对 | ☑ |
| b | 发版窗口内应用改为**只拉新** data-id | ☑ |
| c | 文档 / compose / 种子**只引用新** id | ☑ |
| d | 回滚步骤演练：切回旧构建 + 旧 data-id | ☑ |
| e | **无**运行时双读逻辑合入主干（完成态禁止双读） | ☑ |

### 2.4 Public slug + monorepo 布局（冻结默认）

#### DEC-SLUG

| Field | Value |
|---|---|
| **Public slug（CTO 锁定）** | `NAMEWTA/WTA-plus`（仓名 **WTA-plus**，ADR-012 / LOG-017） |
| 票与门禁 | 一律按 **`NAMEWTA/WTA-plus`** |
| 清理 | 新仓必须去除 `.gitee` 及同类旧平台残留 |

#### DEC-LAYOUT — Monorepo 顶层（镜像现 docs，ADR-012）

布局**参考当前 `ruoyi-vue-plus-docs`** 聚合结构（前后端子树 + `docs` / `speculo` / `release-artifacts` / `scripts` 等），在新仓 `WTA-plus` 内落地；模块目录/artifact 执行 `ruoyi-`→`wta-` 换前缀。

```text
/   # NAMEWTA/WTA-plus（orphan public）
  <backend-tree>/     # 自 ruoyi-vue-plus-namewta 摘取；模块为 wta-*
  <frontend-tree>/    # 自 plus-ui-namewta 摘取
  docs/
  release-artifacts/
  scripts/
  speculo/
  AGENTS.md, README.md, …
```

- **去 submodule** 作为默认交付；内容合入单仓。  
- **清理** `.gitee` 等旧托管残留（ADR-012）。  
- 精确目录名对照表由 **T04** 按现 docs 树细化；不得再投影「聚合 KEEP / 去前缀裸名」。

### 2.5 Publication gate requirements

**No source is publicly pushed before all of the following pass and `public_repo_publication_authorized=true`:**

| Check | Requirement |
|---|---|
| Source baseline | Matches or supersedes `evidence/SOURCE-BASELINE.md` with documented re-baseline + review |
| Secret scan | No credentials, private keys, `.env` secrets in tree to publish |
| Forbidden files | No `node_modules`, `target`, `.git` objects from legacy, caches, local overrides intended private |
| KEEP verify | AC-KEEP family green |
| License | NOTICE/LICENSE attribution intact for third-party |
| Residual classification | Every remaining `ruoyi`/`org.dromara` hit allowed+classified or defect (AC-RESIDUAL) |
| Explicit auth | Written `public_repo_publication_authorized=true` |

Negative: **no “public first, clean later”.**

---

## 3. 用户故事

- **US-001**：作为品牌/产品 owner，我希望自有坐标与模块统一为 `wta`/`org.namewta`/`wta-*`，以便公开面与构建一致且不误伤第三方。  
- **US-002**：作为运维，我希望 Nacos/服务发现标识有对照表与发版窗口硬切 checklist，以便迁移可验收且无长期双读。  
- **US-003**：作为仓库管理员，我希望旧三仓冻结并以 orphan 新 monorepo（`NAMEWTA/WTA-plus`）交付，以便历史干净且可审计授权。  
- **US-004**：作为 CTO/审阅者，我希望文档门禁与实施/公开/旧仓 mutation 三门独立，以便 Review≠Implement。

---

## 4. 验收合同

| ID | Family | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|---|
| AC-001 | AC-NAME | inventory 已分类 OWNED | 迁移自有品牌/标识 | 自有 RuoYi/ruoyi → WTA/wta per rules | residual report + docs |
| AC-002 | AC-PACKAGE | inventory + KEEP 规则 | 迁移自有包/groupId | 自有 `org.dromara`→`org.namewta`；第三方包不变 | compile + rg KEEP |
| AC-003 | AC-KEEP | THIRD-PARTY-KEEP | 任意 rename 波次后 | sms4j/warm/easyes/mica 坐标与 import **字节级保留** | rg + compile |
| AC-004 | AC-PREFIX | ADR-008 | 模块/artifact 搬迁 | `ruoyi-X`→`wta-X`；**无裸名** | dir/artifact map |
| AC-005 | AC-RUNTIME | §2.3 对照表 | runtime 迁移 | app/discovery/gateway/config 标识一致；硬切按 DEC-NACOS | mapping + config |
| AC-006 | AC-Nacos-Cutover | 发版窗口 | 硬切切换 | 硬切勾选 a–e 全满足；无运行时双读 | cutover checklist |
| AC-007 | AC-RESIDUAL | rename 完成 | 残留扫描 | 每个命中 allowed+classified 或 defect；**非** zero-match | residual report |
| AC-008 | AC-REPOSITORY | orphan 发布 | 检查新仓历史 | ORPH-01…05；无旧三仓祖先 | git rev-list / log |
| AC-009 | AC-PUBLICATION | publication checklist | public push 企图 | 仅当 gate + `public_repo_publication_authorized` | evidence/publication/ |
| AC-010 | AC-LEGACY | legacy auth | 旧仓 README/archive/protection | 仅 `legacy_repo_mutation_authorized` 后 | remote state proof |
| AC-011 | AC-W0b | `implementation_authorized` | 准备 monorepo | 新树含顶层四目录；排除规则；slug/布局与 DEC-SLUG/LAYOUT 一致；orphan prep；**未**在旧仓主线 rename | prep checklist |
| AC-012 | AC-Gate | 任意阶段 | 文档关闭 / Spec ready | **不等于** `implementation_authorized` / publication / legacy auth | status gates |

### AC family narrative（与上表同族，可勾选）

#### AC-NAME
First-party RuoYi/ruoyi identifiers defined by the rename inventory are migrated to WTA/wta per approved naming rules.

#### AC-PACKAGE
Approved first-party `org.dromara` namespaces are migrated to `org.namewta`.

#### AC-KEEP
Third-party packages, dependencies, attribution, licenses, and upstream references remain unchanged unless individually approved. KEEP regression `rg`/compile proves sms4j/warm/easyes/mica intact.

#### AC-PREFIX
Project-owned artifacts/modules requiring project prefix use `wta-` (not bare names).

#### AC-RUNTIME
Application name, discovery registration, consumer reference, gateway route, and configuration identifiers are mutually consistent per §2.3 mapping; cutover respects DEC-NACOS hard-cut.

#### AC-Nacos-Cutover
Hard-cut may proceed only when release-window checklist a–e are all checked. No runtime dual-read in completed state; rollback uses old build + old data-id.

#### AC-RESIDUAL
Every remaining `ruoyi`/`org.dromara` occurrence is either (a) explicitly allowed and classified, or (b) a defect. **Zero-match is not required.**

#### AC-REPOSITORY
New monorepo has independent orphan history and no Git ancestry from the three legacy repositories (ORPH-01…05).

#### AC-PUBLICATION
No source is publicly pushed before publication readiness and explicit `public_repo_publication_authorized`.

#### AC-LEGACY
Legacy repositories transition to approved frozen state only after `legacy_repo_mutation_authorized`.

#### AC-W0b
After `implementation_authorized`, prep delivers new monorepo tree mirroring docs layout, exclusion rules, slug **`NAMEWTA/WTA-plus`**, scrub `.gitee`, and orphan prep — **without** performing owned rename as default-branch commits on legacy remotes.

#### AC-Gate
Documentation gate close / Spec `ready_for_tickets` / consistency pass **does not** authorize implementation, publication, or legacy mutation.

### Negative ACs（必须为真）

| ID | Negative criterion |
|---|---|
| NAC-01 | No blind `sed`/global replace across product trees as the migration method |
| NAC-02 | No public push before publication gate |
| NAC-03 | No old-repo mutation (force-push, rewrite, unprotected mainline commits) without auth |
| NAC-04 | Review `CHANGES REQUIRED` / local doc close / S-spec ready is **not** treated as implementation authorization |
| NAC-05 | Global zero-match of `ruoyi`/`org.dromara` is **not** used as pass/fail |
| NAC-06 | No rename-inside-legacy-then-merge（违反 HC-13 / W0b） |

---

## 5. 范围

### IN

- Ownership-aware rename 合同与验收族  
- W0b monorepo/orphan/freeze 合同与默认 slug/布局  
- Nacos/runtime **硬切** + 发版窗口清单  
- KEEP 规则与 inventory-first  
- Publication / legacy 授权门禁  
- Tickets-map outline（含 W0b）供 T-tickets 拆票  

### REUSE

- `evidence/THIRD-PARTY-KEEP.md`、SOURCE-BASELINE、SURVEY  
- 既有 `@namewta/*`、Docker `namewta-*` 产品商标字符串  
- ADR-001…011 已拍板决策（本 Spec **不改** ADR 决策句）  

### OUT

- **OOS-001**：I-implement / 建仓 push / 代码重命名（无 `implementation_authorized`）  
- **OOS-002**：Cloud Agent、CTO 机器操作  
- **OOS-003**：功能行为变更（纯命名/坐标 + 仓库形态）  
- **OOS-004**：将第三方文案改成虚假品牌；改 sms4j/warm import/坐标  
- **OOS-005**：旧三仓 rewrite / force-push  
- **OOS-006**：把缓存打进新仓或 review zip；以 global zero-match 为 AC  
- **OOS-007**：旧仓 README/archive 落地（可选；需 `legacy_repo_mutation_authorized`）  

---

## 6. 已锁定实现约束

- **DEC-001** OWNED `ruoyi`→`wta`；前端不强制 `@namewta`→`@wta`。来源：ADR-001。  
- **DEC-002** OWNED `org.dromara`→`org.namewta`（非 `org.wta`、非 `com.wta`）。来源：ADR-002 / LOG-020。  
- **DEC-003** 第三方 KEEP 四件套。来源：ADR-003。  
- **DEC-004** 分波次 + hard gates；禁止无闸门 big-bang。来源：ADR-004。  
- **DEC-005** 旧仓不 rewrite；聚合 KEEP / 全局不 orphan **已废止**。来源：ADR-005 superseded + ADR-010/011。  
- **DEC-NACOS** **一次性硬切、无双读**；发版窗口人工迁配置 + 勾选 a–e。来源：ADR-006 / LOG-018 / t89s3。  
- **DEC-007/013** 默认用户 `wta`；**旧库一律迁移**。来源：ADR-007/013 / LOG-017。  
- **DEC-008** 前缀 **SWAP** `ruoyi-`→`wta-`（非去前缀/裸名）。来源：ADR-008。  
- **DEC-009** 自有种子字面量一并改。来源：ADR-009。  
- **DEC-010** freeze + orphan + publication sequencing + 三门授权。来源：ADR-010。  
- **DEC-011** FE+BE+docs → 单一 public monorepo。来源：ADR-011。  
- **DEC-SLUG** `NAMEWTA/WTA-plus`；清 `.gitee`。来源：ADR-012 / LOG-017。  
- **DEC-LAYOUT** 镜像现 `ruoyi-vue-plus-docs` 布局；去 submodule。来源：ADR-012 / LOG-017。  
- **DEC-UPSTREAM** 上游不需要；URL 改自有或移除。来源：ADR-012。  
- **DEC-SMS4J-COMMENT** 注释叙述可改为「上游 SMS4J / Warm-Flow…」；**不得改 import/坐标**。来源：S-spec AMBIGUOUS 锁定。  
- **DEC-GATE** 文档门禁 ≠ 实施授权。来源：P0-02 / AC-Gate。

### Functional requirements（保留）

| ID | Requirement |
|---|---|
| FR-001 | 自有品牌与模块名：`ruoyi-`→`wta-`；`@namewta` 不强制 `@wta` |
| FR-002 | 自有 PackageCoord→`org.namewta`；同步扫描、MyBatis、AutoConfiguration、fm |
| FR-003 | 第三方保护；CI 应用 THIRD-PARTY-KEEP |
| FR-004 | 分波次与闸门；授权→W0b→rename |
| FR-005 | 旧三仓 freeze；orphan 新 public monorepo |
| FR-006 | ChatGPT/本地迭代/一致性 → CTO 三门书面授权分别开启 |
| FR-007 | Inventory-first |
| FR-008 | Runtime contract §2.3 + AC-Nacos-Cutover |

---

## 7. 数据、接口与兼容

- **公共接口变化：** Maven groupId/artifact、Java 包名、Nacos data-id、默认用户名（OWNED）；第三方 SDK 接口不变。  
- **数据模型与持久化：** 种子/示例 OWNED 字面量；旧库用户行策略待 CTO。  
- **兼容要求：** Nacos **硬切**（无双读）；compose 服务名默认 KEEP `namewta-*`。  
- **迁移要求：** inventory → W0b → rename；对照表驱动。  
- **发布或运维影响：** publication gate；旧仓 freeze transition 另授权。

---

## 8. 非功能要求

- **NFR-001 安全与隐私：** 公开前 secret/forbidden 扫描；不把私密 `.env` 入仓。  
- **NFR-002 性能与容量：** 不适用（纯命名/仓库形态）。  
- **NFR-003 可用性与可靠性：** Nacos 硬切依赖发版窗口纪律；失败按回滚 checklist。  
- **NFR-004 可观测性与运营：** 切后只认新 data-id；旧 id 不再作为运行依赖。

---

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| Classified residual vs KEEP preserve | static | AC-KEEP, AC-RESIDUAL, NAC-01/05 | `rg` + THIRD-PARTY-KEEP | evidence/inventory, residual |
| Backend package | build | AC-PACKAGE, AC-PREFIX | `./mvnw -pl wta-admin -am -DskipTests package`（名以票为准） | build logs |
| Frontend architecture | build | AC-NAME | `pnpm architecture:check` | FE logs |
| Compose / paths | config | AC-RUNTIME, AC-PREFIX | compose config | deploy notes |
| Nacos hard-cut checklist | ops | AC-Nacos-Cutover, AC-RUNTIME | cutover evidence | evidence/runtime |
| Orphan / publication | release | AC-REPOSITORY, AC-PUBLICATION, AC-W0b | ORPH-* + publication checklist | evidence/publication |
| SpecDev gates | process | AC-Gate, NAC-04 | `.status.json` auth fields | status / LOG |
| **本轮** | docs | Spec readiness | validate-specdev `--stage spec` | 本 Spec + map；不声称 build/E2E/push |

---

## 10. 风险、假设与未决问题

### 风险

| 风险 | 缓解 |
|---|---|
| 盲替换破坏 sms4j/warm | Classification + AC-KEEP + NAC-01（ADR-003） |
| 包名与扫描不一致 | FR-002；common→admin 子波 |
| Nacos/用户变更导致起不来 | DEC-NACOS 硬切 checklist + 回滚；旧库用户一律迁移（ADR-013） |
| 先 public 后清理 | AC-PUBLICATION + ADR-010 sequencing |
| Baseline drift | SOURCE-BASELINE invalidation → re-review |
| 旧仓内先 rename | HC-13 / AC-W0b / NAC-06 |
| 文档漂移「去前缀 / 聚合 KEEP / Nacos 双读」 | 本 Spec 收敛；权威 ADR-008/010/011/006/012/013 |
| 外脑 blocked 被当成豁免 | notes 明示；不豁免实施门禁 |
| 与其他进行中 change 冲突 | 串行同文件集；current writer=1 |

### 已采用的低影响假设

- Docker 产品服务名默认 KEEP `namewta-*`。  
- 前端 npm 范围继续 `@namewta`。  
- T04 可细化子树对照而不改顶层四目录合同。  
- inventory 补全对照表行不改变 DEC-NACOS 硬切策略。

### 未决问题

无。

## 11. P0-01…P0-08 覆盖表（秘书 LOG-014 → 本 Spec）

> 高影响项已归入 §12 AMBIGUOUS 处置表（锁定默认 / 待 CTO 显式分类 / OOS），**不阻塞** `ready_for_tickets`。待 CTO 项是实施/细票冻结门禁，不是未分类空洞。

| P0 | 标题 | Spec 覆盖（章节 / HC / AC） |
|---|---|---|
| P0-01 | ADR-010/011 Supersedes；废止聚合 KEEP / 去前缀投影 | §2.4 DEC-LAYOUT/SLUG；§6 DEC-005/008；风险表对齐 ADR-008/011；HC-04/06 |
| P0-02 | Authorization Gate 三门 | HC-08/09/10；AC-Gate；NAC-04；§11 状态字段 |
| P0-03 | documentation / review / implementation 状态拆分 | Frontmatter + §11；禁止单一 READY=可实施 |
| P0-04 | Publication sequencing | §2.5；AC-PUBLICATION；NAC-02 |
| P0-05 | KEEP 三类 + zero-match 非 AC | §2.1；HC-03/11/12；AC-KEEP；AC-RESIDUAL；NAC-05 |
| P0-06 | Namespace ownership inventory | §2.2；FR-007；T02 |
| P0-07 | Nacos/runtime 合同 | §2.3；DEC-NACOS 硬切；AC-RUNTIME；AC-Nacos-Cutover |
| P0-08 | SOURCE-BASELINE | Frontmatter sources；§2.5 baseline check；验证策略 |

**P0 已关 ≠ `implementation_authorized`。**

---

## 12. AMBIGUOUS 处置表（锁定 / 待 CTO / OOS）

| 项 | 处置 | 合同要点 |
|---|---|---|
| Nacos 切法 | **锁定硬切** | 无双读；发版窗口人工迁配置；勾选 a–e（ADR-006/LOG-018） |
| public slug | **锁定** | `NAMEWTA/WTA-plus`；清 `.gitee`（ADR-012） |
| monorepo 布局 | **锁定** | 镜像现 `ruoyi-vue-plus-docs`；去 submodule；子树 T04（ADR-012） |
| sms4j 注释口径 | **锁定** | 叙述可改「上游 SMS4J / Warm-Flow…」；**不得改 import/坐标** |
| `repository.url` / 上游 URL | **锁定：不要上游** | 改自有或移除上游指向（ADR-012） |
| 旧库用户迁移 | **锁定：一律→wta** | 含已有库，不只新装（ADR-013） |
| 旧仓 README/archive | **OOS/可选** | 需 `legacy_repo_mutation_authorized` |
| 三门书面授权 | **待 CTO** | 实施仍 HOLD（t84s2）；不阻塞拆票；阻塞 I/建仓/push/旧仓 mutation |

---

## 13. 状态声明

| Field | Value |
|---|---|
| `status` | `ready` |
| `ready_for_tickets` | **`true`**（合同可拆票；待 CTO 项已分类为门禁） |
| `documentation_status` | `iterated-p0-closed` |
| `review_status` | consistency-pass-with-nits；LOG-017/018 拍板已同步进 Spec（硬切 / WTA-plus） |
| `implementation_authorized` | `false` |
| `public_repo_publication_authorized` | `false` |
| `legacy_repo_mutation_authorized` | `false` |

**可开 T-tickets（拆 outline 正式票）。不可开 I-implement / 建仓 / push。**  
**I-implement 未授权。禁止将本 spec 升级误读为可执行授权。**

> **LOG-018/019：** 正文已与 ADR-006 硬切、ADR-012（WTA-plus/docs 布局/无上游）、ADR-013（旧库用户）对齐；实施三门仍 false。
