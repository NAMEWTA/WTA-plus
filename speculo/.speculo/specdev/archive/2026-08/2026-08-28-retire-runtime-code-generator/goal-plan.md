---
schema_version: 6
artifact: goal-plan
change: 2026-08-28-retire-runtime-code-generator
status: completed
modes: [migration, high-assurance]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 1
integration_attempt_limit: 7
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: 完整退役运行时代码生成器

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在不保留兼容面、生产迁移或回滚数据的前提下，从后端、前端、当前 OpenAPI、NAMEWTA schema、菜单权限和当前事实中完整删除运行时代码生成器；父仓库 `<Path>docs/fm/**</Path>` 继续作为唯一可校验 CRUD 静态模板资产。

### Success and False Completion

成功必须同时满足：五张 Ticket 都有非空 implementation commit、通过的 current-workspace/direct-parent Evidence 和父仓库 result SHA；AC-001 至 AC-012 全部通过；本地 MySQL 8.4 fresh 数据目录最终无两张表、九个菜单和角色关系；历史 OpenAPI revisions、冻结 SQL 和 `docs/fm` 模板本体未被破坏。

以下均属于伪完成：只隐藏 UI 或菜单；只删目录但保留 Maven/package/manifest/当前合同引用；直接手改生成 TypeScript；只 review SQL 而不执行 fresh E2E；删除 `docs/fm`；改写冻结 SQL/历史 revision；跳过失败测试或把未运行项记为通过。

### Non-goals

- 不创建替代生成器、CLI、插件、stub、redirect 或 deprecated API。
- 不修改过去生成的业务代码、无关 `generateRoutes`/密码/OpenAPI 生成语义或 `<Path>plus-ui-namewta/tooling/generators/**</Path>`。
- 不执行 push、PR、远端合并、部署、生产数据库操作、branch/worktree 清理或归档。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户关于物理/永久删除、基座无兼容及 2026-08-29 current/direct-parent 授权 | 产品取舍、不可逆批准、workspace 与本地提交权限 | 新决定改变外部行为时返回真正上游 owner |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ADR.md</Path>`、`CONTEXT.md`、`LOG.md` | change 架构决定、术语与共识 | 返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` 修订 |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>`、`<Path>{roots.state}/specdev/context/</Path>` 与项目工程 Skills | 已毕业决定、模块边界、SQL/OpenAPI/交付规范 | 当前 change 替代时必须在 change ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>` | 外部行为、范围、AC-001 至 AC-012 | Goal Plan 不改写 Spec |
| 5 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/</Path>` | 单 Ticket 路径、实现与验证合同 | Goal Plan 只编排，偏差返回 Ticket owner |
| 6 | 当前 Git、源码、工具版本和命令实测 | 基线、可行性与恢复定位 | 冲突时暂停受影响 Gate 并登记偏差 |

## 2. Execution Graph

### DAG and Critical Path

```text
真实 DAG：
T-01 ──→ T-04 ──┐
  └───────────────┤
T-02 ─────────────┼──→ T-05
T-03 ─────────────┘

current 单写者执行队列：
G0 → T-01/G1 → T-02/G2 → T-03/G3 → T-04/G4 → T-05/G5
```

关键路径是 T-01 → T-04 → T-05；T-02 与 T-03 是 T-05 的独立汇合前置。current 模式的 T-01 → T-02 → T-03 串行次序是 writer/integration 队列，不新增 Ticket `blocked_by`。T-02 排在 T-04 前，确保先删除生成合同消费者，再收缩当前 TypeScript 合同。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| 1 | T-01 | G0；后端 `main` 与 dirty 基线复核 | 后端模块/POM/Springdoc/bundle checks | T-01 | G1 / 1 |
| 1 | T-02 | G1；唯一 writer 释放 | 前端 gen packages/Admin/architecture/lockfile/E2E | T-02 | G2 / 2 |
| 1 | T-03 | G2；本地临时 MySQL 8.4 接缝可用 | NAMEWTA DDL/DML | T-03 | G3 / 3 |
| 2 | T-04 | T-01 result；G3 后唯一 writer 释放 | 新 OpenAPI revision/current/generated | T-04 | G4 / 4 |
| 3 | T-05 | T-01 至 T-04 result/Evidence | 列出的父级 Skills/README、Admin README 与恢复模板 | T-05 | G5 / 5 |

逻辑 Wave 不授权并发。所有 Ticket 由 `codex:/root` 承担状态责任；执行时可以选择 Lead 自行实现或按 subagent-delivery 生成原生 Dispatch Packet，但同时只有一个 implementation owner 能写 current workspace。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 后端无生成器模块/API且双 bundle 通过 | — | `current` / backend main + aggregate main | codex:/root 或 execution-time dynamic implementation owner | not-required：源码/构建与 T-04 合同闭合 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-01.md</Path>` |
| T-02 | 前端无 gen packages/Admin 组合 | — | `current` / frontend main + aggregate main | 同上，串行锁 | not-required：architecture/tests/build 直接证明 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-02.md</Path>` |
| T-03 | MySQL 无表、菜单和角色关系 | — | `current` / backend main + aggregate main | 同上，串行锁 | required：Lead 在临时 MySQL 8.4 数据目录执行 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-03.md</Path>` |
| T-04 | 当前 OpenAPI/TS 无生成器合同 | T-01 | `current` / frontend main + aggregate main | 同上，串行锁 | not-required：真实快照 provenance + deterministic check | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-04.md</Path>` |
| T-05 | 当前事实一致且全门禁通过 | T-01,T-02,T-03,T-04 | `current` / parent main + frontend main | 同上，串行锁 | not-required：复核 T-03 Lead E2E，不重复冒充 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-05.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- 所有非 cancelled Ticket 为 `done`，frontmatter、Map、Goal Plan、Evidence、change status 与 Git checkpoint 一致。
- 每张 Ticket 有子仓库 commit（适用时）、父仓库 gitlink/项目文档 implementation commit、通过的 direct-parent 验证和父仓库 `result_sha`；不存在 empty commit。
- AC-001 至 AC-012 全覆盖，无未经批准 deferred；T-03 required E2E 由 Lead 在 current workspace 的临时 MySQL 8.4 环境执行。
- 两个子模块 HEAD 与父仓库 gitlink result 对齐；未把无关 dirty/staged 用户改动混入 scoped commit，也未回退这些改动。
- 没有活动 candidate/worktree、未集成 checkpoint、高影响偏差或被虚报的验证结果。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 Baseline & Authorization | Tickets Ready；用户确认 current/direct-parent 与本地 commits | 根/后端/前端 `main` 初始 SHA、dirty 路径快照、授权审计、工具版本 | 全部 Ticket | codex:/root；批准人 user | 不清理 dirty；修正计划或请求真正缺失授权 |
| G1 Backend Retired | G0；T-01 唯一 writer | T-01 commit set、Maven test/双 bundle、后端残留扫描、root result | T-04 与最终 Gate | codex:/root | 保留当前 checkpoint，T-01 继续修正；父 result 不记录通过 |
| G2 Admin Retired | G1；T-02 唯一 writer | architecture/test/lint/typecheck/build、无 gen package/manifest/lockfile、root result | T-03 执行队列与最终 Gate | codex:/root | 返回 T-02；禁止 stub/alias 或放宽架构规则 |
| G3 Database Retired | G2；T-03 SQL静态前置通过 | append-only diff、临时 MySQL 8.4 fresh init、最终/反向查询、root result | T-04 执行队列与最终 Gate | codex:/root；不可逆批准来自 user | 只丢弃临时数据目录，修正 SQL 后重建；不触碰现有数据库 |
| G4 Current Contract Stable | G3；T-01 result 可作为 backend provenance | 新 revision/hash/provenance、generate/check、历史 revisions 零 diff、root result | T-05 | codex:/root | fetch 失败保持旧 current；修正后新增 revision，不改写历史 |
| G5 Final Acceptance | T-01 至 T-04 done/result/Evidence | `docs/fm` validator、scoped allowlist、全 Maven/pnpm/OpenAPI Gate、Gitlink 对齐、T-05 result | change completion | codex:/root | 产品失败退回原 owner；T-05 不越界修补产品路径 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001/002 后端构建与 HTTP 合同消失 | T-01,T-04 | POM/源码/Maven/current OpenAPI | T-01,T-04 | passed |
| AC-003/004 前端包图、组合与可见能力消失 | T-02 | architecture、Vitest、文案、全构建 | T-02 | passed |
| AC-005/006/007 schema、菜单、权限与 fresh init | T-03 | append-only review、本地 MySQL 8.4 E2E/查询 | T-03 | passed |
| AC-008 当前 OpenAPI revision 与生成合同 | T-04 | fetch/generate/check、hash/provenance | T-04 | passed |
| AC-009/010/011/012 残留、模板、事实与完整质量 | T-05 | allowlist、validator、diff、全门禁 | T-05 | passed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence、E2E 与父分支 owner |
| Implementation subagents | 上限 `1`，Lead 不计入 | config 上限 3；本计划因 current 单写者主动降为 1 |
| Integration attempts | 上限 `7` | config `max_integration_attempts=7` 快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation 只读且不争用可变环境 |
| Dispatch | `execution-time dynamic` | provider/模型/是否派单按 Ticket 开始时事实选择，不预分配 |

以 `operation=plan` 固定 subagent-delivery 合同：允许的 task kind 为 implementation/review/research/test-observation；implementation 只能持有 current workspace 串行锁并在明确 writable/shared owner 范围形成 commit；其他类型只读。任何 subagent 不写 SpecDev 工件、Evidence、状态或父分支，不拥有 E2E Gate。外部网页通道若无独立数据发送授权则不可用。

### Ticket Workspace and Integration

初始 Git vector：aggregate `main@bf67a9872664a985c7ebd8d201e9a2f03eeb846d`、backend `main@42e06c0f713e0d724813800505e5bb5b40ab563b`、frontend `main@07962c7cad9ca4db168b3c423b9e3675f312a874`。每个 Ticket 开始时重读三者并以最近通过的 aggregate `result_sha` 为 parent/base；父 HEAD 漂移则暂停并重算，不使用旧基线。

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | 初始/最近 G0 vector | current；backend main，aggregate main | 后端定向扫描、Maven tests/builds | backend scoped commit + aggregate gitlink commit | Lead 重跑 T-01 门禁；无 E2E | aggregate direct-parent `result_sha` |
| T-02 | G1 result | current；frontend main，aggregate main | architecture、定向 tests、lint/typecheck/build | frontend scoped commit + aggregate gitlink commit | Lead 核对 package/manifest/lockfile 与全门禁；无 E2E | aggregate direct-parent `result_sha` |
| T-03 | G2 result | current；backend main，aggregate main | SQL append-only/固定 ID/顺序 review | backend SQL commit + aggregate gitlink commit | Lead 临时 MySQL 8.4 E2E、最终和非目标反向查询 | aggregate direct-parent `result_sha` |
| T-04 | G3 result + T-01 backend commit | current；frontend main，aggregate main | fetch/generate/check、历史 hash、工具测试 | frontend contract commit + aggregate gitlink commit | Lead 重跑 contract/consumer checks；无 E2E | aggregate direct-parent `result_sha` |
| T-05 | G4 result | current；frontend main，aggregate main | 当前事实 diff、模板/残留/全门禁 | frontend Admin README commit（若有）+ aggregate 文档/gitlink commit | Lead 运行 Final Gate；复核 T-03 E2E | aggregate direct-parent `result_sha` |

`current` 模式不创建 source/candidate worktree。每次只允许一个 implementation writer；子仓库先以精确 pathspec 形成 scoped commit，Lead 核对后在父仓库以精确 pathspec 提交对应 gitlink和该 Ticket 明确拥有的父级文件。父仓库其他 staged/dirty 路径保持原样。Lead 验证通过并记录 result 后才释放 writer 锁进入下一 Ticket。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | allowed | 仅本 Goal Plan 五张 Ticket；严格串行，唯一 implementation writer |
| Ticket worktree local changes | not-authorized | 已选择 current；不得创建 source/candidate worktree |
| Implementation commit | allowed | 每 Ticket 必需；只提交其 writable/shared owner 路径和必要父 gitlink |
| Local direct-parent verification and parent update | allowed | Lead-only；子仓库 commit 通过后更新 aggregate gitlink/result |
| Local candidate integration and parent update | not-authorized | required 模式未选择 |
| Local disposable MySQL 8.4 E2E | allowed | 只使用新建临时数据目录/端口/socket；完成后可停止实例，不触碰现有数据库 |
| Push / PR / remote merge | not-authorized | 本地授权不继承到远端 |
| Branch/worktree cleanup | not-authorized | 本计划不创建 worktree，也不清理现有 branch/worktree |
| Deploy / production migration actions | not-authorized | 不部署，不连接或修改生产/现有数据库 |

### Evidence Return

implementation 返回 Ticket ID、current locator、子仓库 commit、aggregate commit候选、dirty 路径、修改路径、非 E2E 命令/结果、未运行项与停止条件。Lead 重读 Git tree、pathspec、父子 SHA 和命令结果后，独立写 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-NN.md</Path>`；subagent 自报结果在 Lead 核对前均为 candidate，不能成为 Done 或 E2E pass。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 永久删除后端/前端运行生成能力、两张表和九个菜单，不保留兼容 API/UI/data。
- `<Path>docs/fm/**</Path>` 模板本体保留并验证；`ry_vue.sql`、其他方言基线、既有 OpenAPI revisions 和 `tooling/generators` 只读。
- SQL 仅追加 NAMEWTA DDL/DML，子表先于主表、角色关系先于菜单；只命中固定 ID，发现系统工具被复用即停止。
- OpenAPI 必须由 fetch/generate/check 产生，provenance 绑定 T-01 后端 commit；禁止手改生成 TypeScript。
- current workspace 始终单 writer；所有既有用户 dirty/staged 改动保留，越界前停止。

### Verification Integrity

- 判卷使用活动源码/POM/package/manifest、当前 OpenAPI、NAMEWTA final state、构建产物和当前事实文档；冻结历史只作为 allowlist，不证明当前支持。
- 禁止为制造绿色而删除仍有效测试、放宽 architecture/lint/typecheck、跳过失败模块、使用空包/stub/alias 或把静态 SQL review 代替 E2E；随被退役能力一起删除的专用测试必须由对应 owner 和残留扫描证明。
- T-03 当前机器无 Docker，但实测存在 Homebrew MySQL `8.4.11`；Lead 用全新临时 datadir/socket/port，按 Compose 的 `10/20/30/40/50/60` SQL 顺序执行。版本仍满足 AC-007 的 MySQL 8.4 合同。
- Node `22.22.3`、pnpm `11.1.3`、Java `21.0.10`、Maven `3.9.12`、Redis `8.10.0` 已在 G0 实测；未运行的远程 CI、Docker Compose 或浏览器 E2E不得声明通过。
- direct-parent 验证在同一 current workspace/result tree完成；T-03 E2E 只能由 Lead 声明。

### Migration or Release Sequence

1. T-01 删除服务端与构建装配，G1 证明后端合同源已收缩。
2. T-02 删除前端消费者和 App 组合，G2 保持当前旧 OpenAPI 的多余类型无害。
3. T-03 追加硬退役 SQL并在临时 MySQL 8.4 完整执行，G3 固化最终数据状态。
4. T-04 从 T-01 commit 获取新快照并收缩当前合同，G4 证明消费者与 contract 同步。
5. T-05 更新当前事实并运行全套最终 Gate；没有生产发布、观察期或 rollback window。

### Risks, Monitoring and Recovery

- **Dirty tree 混入：** 每次 commit 前比较 Ticket pathspec、暂存区和三个 repo SHA；使用 scoped commit，不清空或回退其他 staged 内容。
- **Maven/前端遗漏：** 定向扫描加全构建双证据；失败留在当前 Ticket，不推进 aggregate result。
- **SQL误删/失败：** 固定 ID与父子前置、临时数据目录、最终/反向查询；失败停止实例并丢弃临时目录后重建，不恢复生成器数据。
- **OpenAPI source 不可达：** current pointer 保持旧值；修复可复现后端 source 后重试。已创建 revision 不改写，只能新增。
- **父 HEAD 漂移：** 立即释放不了 writer 锁，标记当前 integration stale，重读最新 Git vector、核对差异并重跑 Gate；最多 7 次后 blocked。
- **Final Gate 失败：** 按路径 owner 返回 T-01 至 T-04；T-05 只修文档，不跨界掩盖产品问题。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。任何新增消费者、系统工具非目标子菜单、无法获得真实 OpenAPI、验证接缝失效、路径越界或新高影响决定都暂停受影响 Gate；记录事实、影响 AC、最近可信 result 和恢复条件，再返回 Spec/Ticket/用户决定 owner。

## 6. Progress and Decisions

### Current Status

- Goal Plan completed；G0 至 G5 均已关闭，最终产品 result 为 `3ca516d1c2b35ab0d34e0bc38ff7c25c2d2c174d`。
- T-01 至 T-05 全部为 `done`；12 条 AC 均有通过 Evidence。
- 初始 Git vector 已记录；父仓库和两个子仓库均在 `main`，现有 dirty/staged 改动继续保留。

### Pending Decisions and Blockers

无高影响未决问题或 blocker。未执行远端、部署、生产数据库、branch/worktree 清理或归档动作。

### Resume Protocol

恢复时依次读取本 Goal Plan、目标 Ticket、change `.status.json` 的 workspace 记录、最近 Evidence 和三仓 Git vector。从最后通过的 aggregate `result_sha` 或尚待修正的 current implementation checkpoint 继续；不得重做已通过 Gate，也不得以当前 dirty tree猜测完成状态。

## Assumptions

- 本机 MySQL 8.4.11 与 Spec 要求的 MySQL 8.4 具有本 change 所需 DDL/DML 语义；实施时以完整 SQL执行验证，不以版本号推断通过。
- T-04 可从 T-01 的可复现后端运行结果或同 commit 导出的本地 OpenAPI 文件 fetch；provenance 必须记录真实来源和 40 字符 SHA。
- 现有 dirty/staged 内容属于用户；只要未进入 Ticket writable path，就保持原样并不纳入 scoped implementation commit。
