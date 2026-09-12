# DISPATCH RETURN — Comprehensive Pre-Implementation Review

**Change:** `2026-09-12-rename-ruoyi-dromara-to-wta`
**Review type:** 全面文档 Review / 实施前门禁 Review
**Review phase:** Pre-Implementation
**Implementation authorization:** **未授权实施**
**Review result:** **CHANGES REQUIRED / BLOCKED**
**Implementation gate:** **当前不可进入实施阶段**
**Recommended next state:** 完成全部 P0 文档修正 → 重新执行一次文档一致性复核 → 通过后方可进入实施前最终授权门禁

---

## 1. 结论 / Gate Decision

### 1.1 总体结论

本次 change 的核心方向已经足够明确，且下列硬约束应继续视为 **LOCKED / NON-NEGOTIABLE**：

* 项目自身命名空间：`ruoyi` → `wta`
* 项目自身 Java package/group：`org.dromara` → `org.wta`
* 第三方来源、第三方依赖、第三方协议、第三方固有名称：**KEEP，不做机械替换**
* 项目自产物、模块、服务等约定前缀统一迁移为：`wta-`
* 原有三个仓库进入冻结状态，不继续作为新架构的开发主线
* 三仓内容合并进入一个**全新的 public monorepo**
* 新 monorepo 采用 **orphan / clean-history** 策略，不携带旧三仓 Git 历史
* 未获得明确授权之前：**不得开始实施**

但是，基于已完成的文档与约束核查，当前文档集仍存在会直接影响实施安全性的阻断项，主要集中于：

1. **部分旧决策/旧表述仍与 ADR-010 / ADR-011 的最终约束冲突；**
2. **“先创建公开仓库 / 再清理”的 sequencing 风险没有被 release/publication gate 明确阻断；**
3. **第三方 KEEP 的边界没有在 spec / acceptance / tickets 中被定义为可验证规则；**
4. **Nacos 等配置/服务发现相关 rename 行为没有形成足够明确的迁移和验收契约；**
5. **状态字段 / gate 状态语义存在不一致或不足以阻止误实施的问题；**
6. **source hash / evidence provenance 设计不足，无法可靠证明实施时使用的是“本轮审核通过的输入基线”；**
7. **部分 Acceptance Criteria 仍停留在“rename 成功”级别，而不是“正确 rename 且没有误伤第三方、旧仓库、运行时契约”的级别。**

因此，本次 Review 的实施前门禁结论为：

> **BLOCKED — 不批准进入 Implementation。**

这里的 `BLOCKED` 不是否定 change 方案本身，而是意味着：**方案方向成立，但作为实施合同的文档还没有达到足够确定、可验证、不可误解的程度。**

在全部 P0 问题关闭前，不建议将 `goal-plan.md`、`tickets-map.md` 或任何自动化 Agent 的状态推进到“可执行”。

---

# 2. Scope / 覆盖范围

本次 Review 按要求覆盖以下文档类别：

### 根目录控制文件

* `DISPATCH.md`
* `MANIFEST.md`

### change 主文档

* `change/.../REVIEW-BRIEF.md`
* `change/.../LOG.md`
* `change/.../ADR.md`
* `change/.../CONTEXT.md`
* `change/.../spec.md`
* `change/.../goal-plan.md`
* `change/.../tickets-map.md`

### Evidence

* `change/.../evidence/*`

本 Review 的判断重点不是代码实现，而是检查这些文档是否共同形成一个足够严格的 **Implementation Contract**，尤其检查：

* hard constraints 是否唯一且一致；
* ADR 是否真正成为最终权威来源；
* rename scope 是否避免机械全局替换；
* third-party KEEP 是否具有可执行判定方式；
* monorepo/orphan/public sequencing 是否安全；
* tickets 是否完整覆盖 spec；
* acceptance 是否能够验证最终结果；
* evidence 是否能支撑 gate；
* source / baseline 是否具备可追溯性；
* 是否存在未经授权即可误触 implementation 的路径。

---

# 3. Evidence Boundary / 证据边界

本次结论严格限制在**文档和已有 evidence 的实施前 Review**。

以下事项**不属于本次已完成事实**，本 Review 也不将其表述为已经完成：

* 没有声称任何 rename 已经实际实施；
* 没有声称三个旧仓库已经冻结；
* 没有声称新 public monorepo 已经创建；
* 没有声称 orphan history reset 已执行；
* 没有声称任何本地 Git commit 已创建；
* 没有声称代码已经编译；
* 没有声称 backend/frontend/doc 已通过 E2E；
* 没有声称 Nacos / Redis / database / SSO 等运行环境已验证；
* 没有声称 GitHub Actions 已通过；
* 没有声称 release artifact 已生成；
* 没有声称旧仓库 remote / branch protection / archive 状态已经修改。

因此：

> **当前 evidence 只能支撑“设计/文档准备程度”的判断，不能替代 Implementation Evidence。**

后续 implementation 阶段产生的 build/test/release/git evidence 必须与本阶段的文档 evidence 分离。

---

# 4. Hard Constraints Consistency Review

以下约束应成为整个 change 的单一基准。

## HC-01 — 项目命名

项目自身语义中的：

```text
ruoyi
```

迁移为：

```text
wta
```

但必须明确：

> 不是对 repository 全量文本执行 `ruoyi -> wta` 的机械替换。

必须区分：

* first-party identifier；
* historical description；
* upstream attribution；
* dependency coordinates；
* copyright/license；
* URL；
* third-party configuration；
* sample/demo；
* runtime protocol value。

---

## HC-02 — Java namespace

项目自身 namespace：

```text
org.dromara
```

目标：

```text
org.wta
```

同样不能写成：

```text
replace every occurrence of org.dromara
```

正确规则必须是：

> **First-party packages and coordinates owned by this project SHALL migrate from `org.dromara` to `org.wta`; third-party references SHALL remain unchanged unless separately justified.**

这是目前最需要在 `spec.md` 中明确化的规则之一。

---

## HC-03 — Third-party KEEP

这是当前文档需要进一步强化的关键原则。

KEEP 必须覆盖至少：

* Maven/Gradle 第三方依赖 coordinate；
* 第三方 Java package；
* npm package；
* GitHub upstream URL；
* License / NOTICE attribution；
* 第三方镜像；
* 第三方容器；
* 中间件固有字段；
* 第三方数据库 driver；
* Redis/Nacos/Sentinel/MaxKey 等第三方产品名称；
* external protocol identifier；
* 第三方 copyright；
* 引用 upstream 项目时的历史名称。

否则开发者非常容易使用：

```bash
grep -rl ...
sed -i ...
```

进行 repository-wide rename，从而造成 silent corruption。

---

## HC-04 — Prefix

项目自产物的标准 prefix：

```text
wta-
```

但需要定义 prefix 적용范围。

不能只写：

> 所有前缀变成 `wta-`

建议明确：

**MUST rename：**

* first-party module artifact；
* service name；
* internal Docker image；
* internal deployment unit；
* first-party package artifact；
* first-party generated archive；
* project-owned application identifiers。

**MUST NOT blindly rename：**

* dependency artifact；
* external URL；
* upstream project artifact；
* protocol；
* third-party configuration namespace。

---

## HC-05 — Repository topology

目标是：

```text
3 legacy repositories
        ↓ freeze
new public monorepo
        ↓
single source of truth
```

此决策应当贯穿：

* ADR
* CONTEXT
* spec
* goal-plan
* tickets-map
* evidence checklist

目前最重要的问题不是“是否写到了 monorepo”，而是：

> **文档是否能阻止实施者把旧仓 history 带入新仓，或者在 sanitization 完成之前把内容暴露到 public repository。**

当前答案还不够稳健。

---

# 5. Findings

---

## F-01 — P0

### ADR 最终决策与部分旧文本仍可能形成双重真相

**定位：**

* `ADR.md`

  * ADR-010
  * ADR-011
* `CONTEXT.md`

  * repository strategy / migration context
* `goal-plan.md`

  * repository migration sequence
* `tickets-map.md`

  * repository-related tickets
* `REVIEW-BRIEF.md`

  * assumptions / decisions summary

### 问题

已核查内容中存在旧决策/旧叙述没有完全随 ADR-010 / ADR-011 收敛的问题。

这会导致：

```text
ADR says A
plan still implies B
ticket implements B
```

最终开发 Agent 可能按照距离实施最近的 `goal-plan.md` 或 ticket 执行，而不是 ADR。

### 风险

**Critical documentation split-brain。**

尤其 repository strategy 和 rename scope 属于不可逆或高代价变更，不能存在两套语义。

### Required correction

应在 `ADR.md` 明确：

```markdown
Supersedes:
- <old decision>
- <old repository strategy>
```

同时在 `CONTEXT.md` / `goal-plan.md` 中彻底删除或明确标注旧方案：

```markdown
SUPERSEDED — DO NOT IMPLEMENT
```

不要让旧方案仍以普通正文存在。

---

# F-02 — P0

## 新 public monorepo 的发布顺序缺少硬门禁

**定位：**

* `ADR.md`

  * monorepo/orphan decision
* `goal-plan.md`

  * repo bootstrap / migration phase
* `tickets-map.md`

  * repository creation / publish tickets
* `spec.md`

  * repository requirements
* `evidence/*`

  * publication readiness evidence

### 问题

目标是：

> new **public** monorepo + orphan history

但“公开仓库创建”和“内容清理/验证”的先后关系必须非常严格。

危险顺序：

```text
create public repo
↓
push imported content
↓
scan
↓
fix
```

正确顺序应该是：

```text
prepare clean working tree locally/private staging
↓
remove forbidden/generated/secret/history-sensitive content
↓
third-party KEEP validation
↓
rename validation
↓
secret scan
↓
license/notice validation
↓
source inventory validation
↓
human approval
↓
create/activate public destination
↓
orphan initial commit
↓
push
```

### 必须增加

`goal-plan.md` 增加：

```markdown
PUBLICATION GATE

No content SHALL be pushed to the public monorepo before:
- repository inventory passes;
- secret scan passes;
- forbidden-file scan passes;
- KEEP validation passes;
- source baseline is recorded;
- explicit publication authorization is recorded.
```

---

# F-03 — P0

## “未授权不实施”还没有被完全转换为机器/流程可执行状态

**定位：**

* `DISPATCH.md`
* `REVIEW-BRIEF.md`
* `goal-plan.md`
* `tickets-map.md`
* `MANIFEST.md`

### 问题

自然语言说：

> 未授权不实施

还不足以阻止 Agent 在读取 `goal-plan.md` 后直接开始工作。

必须有明确的 machine-readable 或至少 deterministic gate。

### 建议

在 change metadata 增加：

```yaml
phase: pre-implementation-review
implementation_authorized: false
implementation_gate: blocked
public_repo_creation_authorized: false
legacy_repo_mutation_authorized: false
```

并规定：

```text
implementation_authorized != true
→ implementation tickets MUST NOT execute
```

不要用：

```yaml
status: ready
```

因为 `ready` 很容易被解释成 ready-to-implement。

建议状态模型：

```text
draft
reviewing
changes-required
review-approved
implementation-authorized
implementing
verification
done
```

---

# F-04 — P0

## 状态 schema 存在语义缺陷

**定位：**

* `MANIFEST.md`
* `DISPATCH.md`
* `REVIEW-BRIEF.md`
* `tickets-map.md`

### 问题

当前 status/gate 体系不足以表达三个不同概念：

1. 文档已完成；
2. 文档已审核；
3. 已被授权实施。

三者绝不能共用一个 `READY`。

### 推荐字段

```yaml
documentation_status: review_changes_required
review_status: blocked
implementation_authorized: false
implementation_status: not_started
```

对于 ticket：

```yaml
state: specified
execution_allowed: false
blocked_by:
  - review-gate
  - implementation-authorization
```

---

# F-05 — P0

## `source hash` / baseline provenance 不足

**定位：**

* `MANIFEST.md`
* `evidence/*`
* 可能涉及 `REVIEW-BRIEF.md` baseline 字段

### 问题

source hash 若仅记录单个模糊 hash、当前文件 hash 或不可复现值，不能证明：

> Implementation 使用的输入就是 Review 时批准的输入。

三仓 merge 更需要完整 source provenance。

### 应改为明确的 source baseline

例如：

```yaml
source_repositories:
  - repo: docs
    url: ...
    branch: main
    commit_sha: <full SHA>

  - repo: ui
    url: ...
    branch: main
    commit_sha: <full SHA>

  - repo: backend
    url: ...
    branch: main
    commit_sha: <full SHA>
```

再加：

```yaml
baseline_manifest_sha256: ...
review_documents_sha256: ...
```

不要只写：

```yaml
source_hash: abc
```

### Gate rule

若实施开始前任何 source SHA 改变：

```text
baseline mismatch
→ return to review
```

---

# F-06 — P0

## Third-party KEEP 缺乏可以真正验收的 contract

**定位：**

* `spec.md`

  * requirements
  * rename rules
  * acceptance criteria
* `goal-plan.md`
* `tickets-map.md`
* `evidence/*`

### 问题

KEEP 现在作为原则存在，但还没有完全转换成 testable requirement。

应增加 explicit negative acceptance criteria：

```markdown
AC-KEEP-01:
No third-party Java package is renamed solely because it contains
`org.dromara` or `ruoyi`.

AC-KEEP-02:
Third-party Maven/npm coordinates remain unchanged unless explicitly
listed in the approved migration allowlist.

AC-KEEP-03:
License, NOTICE, attribution and upstream repository references retain
their legally/historically correct names.

AC-KEEP-04:
A residual-string report SHALL classify remaining `ruoyi` /
`org.dromara` occurrences as:
- allowed-third-party
- attribution/history
- forbidden-first-party-residual
```

这比要求：

```text
grep must return zero results
```

安全得多。

事实上，本 change **不应该以全局零匹配作为验收标准**。

---

# F-07 — P0

## `org.dromara → org.wta` 需要 first-party allowlist，而不是全量 replacement

**定位：**

* `spec.md`
* `tickets-map.md`
* backend rename ticket
* evidence pattern scan

### 问题

`org.dromara` 既可能属于项目自身，也可能出现在第三方依赖和来源中。

必须先建立 ownership classification。

### 归口 `spec.md`

新增：

```markdown
### Namespace Ownership Rule

Rename is permitted only where the symbol belongs to first-party RVP/WTA
source or first-party artifact metadata.

Third-party coordinates/packages MUST remain unchanged.
```

### 归口 `tickets-map.md`

增加前置 ticket：

```text
T-NS-001 Build namespace ownership inventory
```

然后：

```text
T-NS-002 Rename approved first-party packages
```

而不是直接：

```text
replace org.dromara with org.wta
```

---

# F-08 — P0

## Nacos/service discovery rename 行为没有形成完整契约

**定位：**

* `spec.md`
* `goal-plan.md`
* backend/config tickets in `tickets-map.md`
* `evidence/*`

### 问题

项目 prefix 改为 `wta-` 后，不只是 source code rename。

Nacos 通常还涉及：

* service name；
* namespace；
* group；
* config Data ID；
* bootstrap config；
* profile-specific data id；
* service discovery registration；
* gateway route target；
* Sentinel/Nacos shared config references；
* deployment environment variables。

如果只改：

```yaml
spring.application.name
```

而遗漏：

```text
Nacos Data ID
gateway lb://...
docker compose service
deployment service
configuration imports
```

系统可能编译成功但运行失败。

### `spec.md` 应增加 matrix

| Area                        | Old               | New                  | Rename? | Evidence         |
| --------------------------- | ----------------- | -------------------- | ------- | ---------------- |
| application.name            | `ruoyi-*`         | `wta-*`              | YES     | config scan      |
| Nacos Data ID               | old project-owned | `wta-*`              | YES     | inventory        |
| external Nacos product name | `nacos`           | `nacos`              | KEEP    | negative check   |
| gateway service URI         | `lb://...`        | matching WTA service | YES     | route validation |

### Acceptance

不能只写：

```text
service renamed
```

而应验证：

```text
producer name == discovery name == consumer route == config Data ID contract
```

---

# F-09 — P1

## `spec.md` 应建立 Rename Classification，而不是散落规则

推荐新增核心章节：

```markdown
## Rename Classification Matrix

| Category | Example | Rule |
| First-party brand | RuoYi/RuoYi-Vue-Plus naming | RENAME |
| First-party Java package | org.dromara.xxx owned here | RENAME |
| First-party artifact | ruoyi-* | RENAME to wta-* |
| Third-party Java package | external coordinate | KEEP |
| npm dependency | package from registry | KEEP |
| Upstream attribution | original project name | KEEP |
| License/NOTICE | legal attribution | KEEP |
| Historical migration note | old name | KEEP with context |
| Runtime first-party service ID | ruoyi-system | RENAME |
```

这样 implementation Agent 可以逐类别执行，而不是猜测。

---

# F-10 — P1

## 缺少 residual scan 的“允许残留”机制

**定位：**

* `spec.md`
* `evidence/*`
* `goal-plan.md`

最终很可能仍然合法存在：

```text
ruoyi
org.dromara
dromara
```

所以必须定义 allowlist。

建议 evidence：

```text
evidence/rename/residual-ruoyi.txt
evidence/rename/residual-org-dromara.txt
evidence/rename/residual-classification.md
```

每个 residual：

```text
path
line
classification
reason
decision
```

---

# F-11 — P1

## 三旧仓“freeze”定义不够具体

**定位：**

* `ADR.md`
* `spec.md`
* `goal-plan.md`
* `tickets-map.md`

必须说明 freeze 到底是什么。

建议定义：

```text
Freeze means:

- no feature development;
- no rename implementation;
- no history rewrite;
- no deletion;
- no archive unless separately authorized;
- optional README pointer only if separately authorized;
- existing repositories remain provenance references.
```

这里尤其需要注意：

> “冻结旧仓”不等于“立即 archive/delete”。

由于用户明确要求未授权不实施，所以旧仓 mutation 也必须等待 authorization。

---

# F-12 — P1

## orphan history 需要具体验收条件

**定位：**

* ADR-011
* `spec.md`
* `goal-plan.md`
* evidence

建议 AC：

```text
New monorepo first branch contains no Git ancestry from any of the three
legacy repositories.
```

Implementation evidence 后续应证明：

```bash
git log --parents
git rev-list --max-parents=0 HEAD
```

以及旧 SHA：

```text
not reachable from new repository
```

注意：

**本 Review 不声称上述验证已执行。**

---

# F-13 — P1

## MANIFEST 缺少“文档权威层级”

建议明确：

```text
Decision precedence:

1. Explicit locked constraints
2. Accepted ADR
3. spec.md
4. goal-plan.md
5. tickets-map.md
6. contextual/log/evidence descriptions
```

若发生冲突：

```text
lower-level document MUST NOT override higher-level decision.
```

这能解决 ADR 与 plan 产生双重真相的问题。

---

# F-14 — P1

## tickets-map 需要 requirement ↔ ticket ↔ evidence 完整追踪

推荐字段：

```yaml
ticket: T-BE-001
requirements:
  - REQ-NS-001
  - REQ-KEEP-002
acceptance:
  - AC-NS-003
evidence:
  - EV-NS-001
depends_on:
  - T-INVENTORY-001
```

目前 change 类型较大，如果没有 traceability matrix，很容易：

```text
spec 写了
但 ticket 没执行
```

或者：

```text
ticket 做了
但没有 acceptance/evidence
```

---

# F-15 — P1

## goal-plan 的阶段顺序必须强制化

建议修改为：

```text
Phase 0 — Authorization gate
Phase 1 — Immutable source baseline
Phase 2 — Inventory/classification
Phase 3 — Prepare monorepo working tree
Phase 4 — First-party rename
Phase 5 — Configuration/service rename
Phase 6 — KEEP/residual validation
Phase 7 — Build/static verification
Phase 8 — Integration/E2E verification
Phase 9 — Publication readiness
Phase 10 — Explicit publication authorization
Phase 11 — Orphan initial publication
Phase 12 — Legacy repositories freeze transition
```

其中 Phase 10 前：

```text
NO PUBLIC PUSH
```

Phase 0 未通过：

```text
NO IMPLEMENTATION
```

---

# F-16 — P1

## REVIEW-BRIEF 应明确 Review Question

建议把“是否文档写完整”升级成：

```markdown
The reviewer SHALL determine whether the change is sufficiently
specified to allow deterministic implementation without inventing
rename rules, repository sequencing or third-party exceptions.
```

当前 Review 的核心其实正是：

> implementation Agent 是否还需要猜。

只要答案仍然是“需要猜”，就不能进入 implementation。

---

# F-17 — P1

## LOG.md 必须区分 Discussion / Decision / Superseded

`LOG.md` 很容易被后续 Agent 当成 authoritative context。

建议每条记录带：

```yaml
type:
  - observation
  - proposal
  - decision
  - superseded
```

并加：

```text
LOG is historical record and SHALL NOT override accepted ADR/spec.
```

---

# F-18 — P1

## Evidence taxonomy 应明确区分阶段

建议：

```text
evidence/
├── pre-review/
├── inventory/
├── rename/
├── build/
├── test/
├── e2e/
├── repository/
├── publication/
└── release/
```

当前是实施前 review，因此只能合理存在：

```text
pre-review
inventory
```

等 evidence。

不要提前把不存在的 build/E2E evidence 标成 completed。

---

# F-19 — P2

## CONTEXT 可加入术语表

推荐：

```text
legacy repositories
new monorepo
first-party
third-party
KEEP
rename candidate
allowed residual
forbidden residual
orphan history
publication gate
implementation authorization
```

可减少多 Agent 协作时的语义漂移。

---

# F-20 — P2

## 文件名/模块名大小写等 rename policy 应明确

需要定义：

```text
RuoYi
ruoyi
RUOYI
org.dromara
dromara
ruoyi-
```

是否分别处理。

避免一条 regex 处理所有情况。

---

# F-21 — P2

## 建议增加 rename inventory artifact

例如：

```text
evidence/pre-review/rename-inventory.md
```

分类：

```text
first-party rename
third-party keep
historical attribution keep
requires-human-review
```

这将显著降低实施风险。

---

# 6. P0 — 必须在进入实施前修正

以下全部属于 **blocking**。

---

## P0-01 — 统一 ADR 最终决策

**归口：**

* `ADR.md`
* `CONTEXT.md`
* `goal-plan.md`
* `tickets-map.md`
* `REVIEW-BRIEF.md`

**直接修改建议：**

1. ADR-010 / ADR-011 明确 `Supersedes`。
2. 删除或显式标记冲突旧文本。
3. 所有 plan/ticket 统一引用最终 ADR。
4. 禁止 implementation document 自行重新解释 repository topology。

---

## P0-02 — 加入明确 Authorization Gate

**归口：**

* `DISPATCH.md`
* `MANIFEST.md`
* `goal-plan.md`

建议字段：

```yaml
implementation_authorized: false
implementation_gate: blocked
legacy_repo_mutation_authorized: false
public_repo_publication_authorized: false
```

规则：

```text
Unless implementation_authorized == true:
no implementation action is permitted.
```

---

## P0-03 — 修复 status schema

**归口：**

* `MANIFEST.md`
* `DISPATCH.md`

禁止单一：

```text
READY
```

替代为：

```yaml
documentation_status: changes-required
review_status: blocked
implementation_status: not-started
implementation_authorized: false
```

---

## P0-04 — 定义安全 public publication 顺序

**归口：**

* `goal-plan.md`
* `spec.md`
* `tickets-map.md`

增加：

```text
No public push before publication gate.
```

并要求 publication gate 至少覆盖：

* source baseline；
* secret scan；
* forbidden-file check；
* third-party KEEP verification；
* license attribution；
* rename residual classification；
* explicit authorization。

---

## P0-05 — 重构 Third-party KEEP Acceptance

**归口：**

* `spec.md`

增加：

```text
MUST KEEP
MUST RENAME
MUST REVIEW
```

三类 classification。

明确：

> global zero-match of `ruoyi` / `org.dromara` is NOT an acceptance requirement.

---

## P0-06 — 增加 namespace ownership inventory

**归口：**

* `spec.md`
* `tickets-map.md`

Implementation 前先识别：

```text
org.dromara occurrence
→ first-party?
→ third-party?
→ attribution?
```

只有 first-party 可以自动迁移到 `org.wta`。

---

## P0-07 — 补齐 Nacos / runtime naming contract

**归口：**

* `spec.md`
* `goal-plan.md`
* `tickets-map.md`

至少覆盖：

```text
spring.application.name
Nacos Data ID
service registration
gateway lb route
shared config references
profile config
deployment/container service name
```

并建立 old → new mapping。

---

## P0-08 — 修复 source baseline/hash

**归口：**

* `MANIFEST.md`
* `REVIEW-BRIEF.md`
* `evidence/*`

使用三仓完整 commit SHA + baseline manifest hash。

任何 baseline drift：

```text
→ review invalidated
→ return to pre-implementation review
```

---

# 7. P1 — 强烈建议在批准实施前完成

P1 不一定单独阻断项目方向，但在此 change 的规模下，建议全部在实施授权前处理。

1. `spec.md` 增加 Rename Classification Matrix。
2. `spec.md` 增加 allowed residual / forbidden residual 定义。
3. `ADR.md` 明确定义 freeze 的实际含义。
4. `ADR.md/spec.md` 增加 orphan history acceptance。
5. `MANIFEST.md` 增加文档权威优先级。
6. `tickets-map.md` 增加 requirement / AC / evidence traceability。
7. `goal-plan.md` 重构阶段顺序，并在 public publication 前设置 hard gate。
8. `REVIEW-BRIEF.md` 明确 deterministic implementation review question。
9. `LOG.md` 区分 proposal / decision / superseded。
10. `evidence/*` 按阶段分类，避免把计划性 evidence 当成完成性 evidence。

---

# 8. P2 — 文档质量与长期可维护性增强

1. `CONTEXT.md` 增加 terminology glossary。
2. 定义大小写和 token-level rename policy。
3. 增加 `rename-inventory.md`。
4. 给 residual report 增加 reason / owner / disposition。
5. 对每类 first-party artifact 给出示例。
6. 为 monorepo 根目录结构增加 canonical layout。
7. 明确历史说明中出现 `RuoYi` 不代表 rename failure。
8. License / NOTICE 单独设 validation checklist。
9. 文档中的 old/new name 使用统一 code formatting。
10. 在 tickets 中避免使用“rename everything”“global replace”等容易误实施的措辞。

---

# 9. Recommended Acceptance Model

建议 `spec.md` 最终至少形成以下 acceptance family。

### AC-NAME

```text
First-party RuoYi/ruoyi identifiers defined by the rename inventory
are migrated to WTA/wta according to the approved naming rule.
```

### AC-PACKAGE

```text
Approved first-party org.dromara namespaces are migrated to org.wta.
```

### AC-KEEP

```text
Third-party packages, dependencies, attribution, licenses and upstream
references remain unchanged unless individually approved.
```

### AC-PREFIX

```text
Project-owned artifacts/services requiring project prefix use wta-.
```

### AC-RUNTIME

```text
Application name, discovery registration, consumer reference, gateway
route and configuration identifiers are mutually consistent.
```

### AC-RESIDUAL

```text
Every remaining ruoyi/org.dromara occurrence is either:
(a) explicitly allowed and classified, or
(b) a defect.
```

### AC-REPOSITORY

```text
The new monorepo has independent orphan history and no Git ancestry
from the three legacy repositories.
```

### AC-PUBLICATION

```text
No source is publicly pushed before publication readiness and explicit
authorization have passed.
```

### AC-LEGACY

```text
Legacy repositories are transitioned to the approved frozen state only
after explicit authorization.
```

---

# 10. Suggested Ticket Dependency Skeleton

建议 `tickets-map.md` 至少形成：

```text
T00 Authorization Gate
 |
 +-- T01 Capture Immutable Source Baseline
 |
 +-- T02 Build Rename/Ownership Inventory
 |
 +-- T03 Build Third-party KEEP Inventory
 |
 +-- T04 Prepare Monorepo Tree
 |
 +-- T05 Backend Namespace Rename
 |
 +-- T06 Frontend/Docs First-party Rename
 |
 +-- T07 Artifact/Module Prefix Migration
 |
 +-- T08 Runtime/Nacos/Service-ID Migration
 |
 +-- T09 Residual Classification
 |
 +-- T10 Static/Build Verification
 |
 +-- T11 Integration/E2E Verification
 |
 +-- T12 Publication Readiness
 |
 +-- T13 Explicit Publication Authorization
 |
 +-- T14 Orphan Repository Publication
 |
 +-- T15 Legacy Repository Freeze Transition
```

关键点：

```text
T05-T15 MUST NOT start merely because this review exists.
```

只有：

```text
implementation_authorized=true
```

后才能进入实施。

而 public publication 应进一步依赖：

```text
publication_authorized=true
```

---

# 11. Document-by-Document Return

## `DISPATCH.md`

**结论：需要修改。**

必须让 dispatch 本身明确：

* 当前是 review；
* 不是 implementation；
* 默认禁止实施；
* return status 如何解释；
* CHANGES REQUIRED 不能被视为 authorized。

---

## `MANIFEST.md`

**结论：P0 修改。**

重点：

* status schema；
* source baseline；
* commit SHA provenance；
* authority precedence；
* gate state。

尤其 `source_hash` 应升级为可复现的 baseline definition。

---

## `REVIEW-BRIEF.md`

**结论：P1 修改。**

应明确 reviewer 判断目标：

> 文档是否足以让实施者在不发明规则的前提下 deterministic implementation。

并明确第三方 KEEP、public sequencing、orphan strategy 是 review 必查项。

---

## `LOG.md`

**结论：P1 修改。**

历史讨论不得与 active decisions 混杂。

增加：

```text
proposal
accepted
rejected
superseded
```

状态。

---

## `ADR.md`

**结论：P0 修改。**

ADR-010/011 的最终性必须传播到所有从属文件。

需要补：

* superseded decisions；
* freeze definition；
* public repository sequencing；
* orphan acceptance；
* mutation/publication authorization boundary。

---

## `CONTEXT.md`

**结论：P0/P2 修改。**

删除冲突旧叙述，避免 context 恢复被 ADR 推翻的旧方案。

建议增加 glossary 和 first-party / third-party 定义。

---

## `spec.md`

**结论：P0，当前最大改进重点。**

至少增加：

* rename classification；
* third-party KEEP contract；
* namespace ownership；
* wta- prefix scope；
* residual policy；
* Nacos/runtime contract；
* monorepo/orphan requirements；
* explicit negative acceptance criteria。

---

## `goal-plan.md`

**结论：P0。**

重构执行顺序。

最重要的是：

```text
authorize
→ baseline
→ inventory
→ local/staged migration
→ validation
→ publication gate
→ authorization
→ public orphan push
```

不得：

```text
public first
→ clean later
```

---

## `tickets-map.md`

**结论：P0/P1。**

每个 ticket 都应拥有：

```text
requirement
dependency
allowed scope
forbidden scope
acceptance
evidence
```

尤其禁止出现可被解释成 repository-wide blind replacement 的 ticket。

---

## `evidence/*`

**结论：P0/P1。**

已有 evidence 可作为 pre-implementation context，但后续必须区分：

* planning evidence；
* inventory evidence；
* implementation evidence；
* verification evidence；
* publication evidence。

当前 evidence **不能证明实际 build / E2E / commit / repository publication 已完成**，也不应被如此解释。

---

# 12. Final Gate

```yaml
change: 2026-09-12-rename-ruoyi-dromara-to-wta

review_result: CHANGES_REQUIRED

documentation_gate: BLOCKED

implementation_gate: BLOCKED

implementation_authorized: false

public_monorepo_publication_authorized: false

legacy_repository_mutation_authorized: false

reason:
  - ADR/plan decision convergence incomplete
  - public repository sequencing lacks hard publication gate
  - third-party KEEP acceptance is under-specified
  - namespace ownership rules are insufficiently deterministic
  - Nacos/runtime rename contract is incomplete
  - status/gate schema is ambiguous
  - source baseline/hash provenance is insufficient

required_next_action:
  - resolve all P0 documentation findings
  - regenerate/update relevant pre-implementation evidence
  - perform documentation consistency review
  - obtain explicit implementation authorization

implementation_performed_by_this_review: false

commit_claimed: false

e2e_claimed: false
```

---

# 13. Final Review Statement

本 change **不需要推翻现有目标，也不建议重新讨论已经锁定的方向**。当前真正需要做的是把已经确定的决策，从“人能够理解的大方向”继续收紧成“Agent / 开发者无法误解的实施合同”。

其中最关键的四条是：

1. **Rename 是 ownership-aware rename，不是 global search-and-replace。**
2. **Third-party KEEP 必须通过分类和 negative acceptance 进行证明，而不是要求旧字符串全部消失。**
3. **Public monorepo 必须在清理、扫描、验证和授权完成后才允许首次公开 push。**
4. **Review Approved 与 Implementation Authorized 必须是两个完全独立的状态。**

因此最终判定仍为：

> **CHANGES REQUIRED — BLOCK IMPLEMENTATION**

完成上述 **P0-01 ～ P0-08** 后，才建议重新进入 Implementation-Preflight Gate Review；P0 未关闭以前，不应执行源码 rename、不应修改三个旧仓、不应创建/推送正式 public monorepo，也不应将任何 ticket 标记为 implementation in progress。
