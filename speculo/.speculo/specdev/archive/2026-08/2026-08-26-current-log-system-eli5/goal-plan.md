---
schema_version: 6
artifact: goal-plan
change: 2026-08-26-current-log-system-eli5
status: completed
modes: [high-assurance]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 1
integration_attempt_limit: 4
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: 串行交付完整 HTTP 系统运行日志

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在当前 backend workspace 中严格串行交付三个垂直切片：先由 T-01 建立 common-web 的完整 HTTP 请求/响应 JSON 日志合同，再由 T-02 将该合同接入 ruoyi-admin 唯一同步 sys-console 文件及 60 天/40GB 滚动策略，最后由 T-03 交付根路径时间响应与可落盘的启动运行摘要。整个计划不创建 Ticket worktree，不并行写项目路径，也不触碰 `@Log` 数据库审计链路。

### Success and False Completion

成功必须同时满足：AC-001 至 AC-019 均有 Lead 可复查 Evidence；T-01、T-02 和 T-03 按 DAG 串行完成；定向测试、真实启动落盘、后端 reactor test、clean full/core package 均通过；实际路径、Ticket、Map、Goal Plan、Evidence 与 Git checkpoint 一致。

以下均属于伪完成：只有代码 diff 或口头说明；只验证事件对象而未验证真实文件物理行；只测同步请求而未覆盖加密、异常和 Servlet async；测试通过但没有每 Ticket 不可变 implementation commit/result SHA；把混合 T-01、T-02、T-03 的单一提交冒充三个独立结果。

用户最初禁止 commit，随后授权将全部后端改动作为一个提交推送；实际 checkpoint 为 `a98d6edcc591550221dd983e293d43e3aac36d23`。2026-08-28 用户进一步授权按仓库事实补齐归档条件，明确接受该合并 checkpoint 偏差；完整门禁在最终结果 `f88012eae14374bb6f59b0389e3e937345ccde7e` 闭合。

### Non-goals

- 不创建 branch、Ticket worktree、candidate checkout 或 candidate merge。
- 不 commit、push、创建 PR、远程 merge、部署或操作生产日志目录。
- 不修改 `@Log`、数据库审计、业务 Controller/DTO、前端、数据库或其他部署应用 Logback。
- 不删除已存在的 sys-info/sys-error 历史文件。
- Goal Plan 不重新决定 Ticket 内部字段、Filter 实现步骤或测试细节。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | `USER-DECISION:2026-08-26-current-workspace-serial-no-commit` 与后续 `USER-DECISION:2026-08-26-commit-push-backend` | 不用 worktree；先按非完成模式实现，后把全部后端范围作为一个提交推送 | 记录后续授权和实际合并提交；不得倒推虚构逐 Ticket checkpoint 或完成状态 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/CONTEXT.md</Path>` | 凭证原值安全例外与领域语义 | 架构变化返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业的永久约束 | 当前 change 的显式 ADR 例外优先且不得外扩 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/spec.md</Path>` | 外部行为、范围、16 个验收合同 | Goal Plan 不得改写，冲突返回 Spec owner |
| 5 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/</Path>` | 单 Ticket 行为、路径和验证合同 | 本计划只编排先后、Gate、owner 和恢复 |
| 6 | 当前代码与 Git 事实 | 可行性、基线和 workspace 状态 | 冲突时暂停并按 deviation control 返回真正 owner |

## 2. Execution Graph

### DAG and Critical Path

```text
G0 Execution Authorization [DEVIATED: one combined commit, no per-Ticket checkpoints]
  └─→ T-01 Complete HTTP System Log [READY, not startable]
        └─→ G1 HTTP Contract Stable
              └─→ T-02 sys-console Persistence [READY, blocked by T-01]
                    └─→ G2 Backend Delivery Verified
                          └─→ T-03 Admin Runtime Summary [READY, blocked by T-02]
                                └─→ G3 Startup Runtime Verified
```

关键路径是唯一线性路径 `G0 -> T-01 -> G1 -> T-02 -> G2 -> T-03 -> G3`。没有扇出、汇合或并行候选；T-02 消费 T-01 的 logger/JSON 合同，T-03 消费 T-02 的 sys-console appender 合同。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| 0 | — | Goal Plan 已发布 | 无项目写入 | 无 | G0：implementation commit 与 direct-parent 授权 |
| 1 | T-01 | G0 关闭；backend base 未漂移；唯一 writer 锁可用 | common-web Ticket writable paths | 无；T-01 独占专用 logger/JSON 合同 | G1 / integration 1 |
| 2 | T-02 | G1 关闭；T-01 result 可定位；唯一 writer 锁可用 | admin Logback XML 和 logging tests | 无；T-02 独占 admin Logback | G2 / integration 2 |
| 3 | T-03 | G2 关闭；T-02 result 可定位；唯一 writer 锁可用 | admin 启动类、根路径 Controller 和对应测试 | 无；T-03 独占 admin runtime summary | G3 / integration 3 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 完整、可关联、原值、可关闭且异步闭合的 HTTP JSON 日志 | G0 | `current`，backend `main`，不创建 worktree | Lead；后续明确授权时可动态派单，但仍单 writer | not-required：Servlet/Spring/CryptoFilter 组合合同覆盖公共 HTTP 边界 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` |
| T-02 | 唯一同步 sys-console、无前缀 HTTP JSON 行、gzip/60 天/40GB | T-01 + G1 | `current`，同一 backend `main` | Lead；后续明确授权时可动态派单，但仍单 writer | not-required：隔离真实 Logback context 和临时文件覆盖物理文件边界 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` |
| T-03 | 固定/动态时间响应与可落盘启动运行摘要 | T-02 + G2 | `current`，同一 backend `main` | Lead；当前已按非完成模式直接实现 | required：本机临时端口和临时日志目录启动验证 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- T-01、T-02、T-03 均为 done，AC-001 至 AC-019 全部有通过 Evidence，无 deferred、未批准 deviation 或 unverified 高风险项。
- current 模式下每个 Ticket 均有非空 implementation commit、通过的 direct-parent 验证和父分支 result SHA；工作区状态和 Ticket 顺序可从 Git 重建。
- G1 证明 HTTP logger 合同稳定，G2 证明真实 sys-console 文件行为和完整 backend 组装稳定。
- 没有 worktree/candidate、未提交 Ticket 混合 diff、活动 implementation owner 或未关闭 blocker。
- 实际单一合并提交提供了可定位 checkpoint，但不能替代三个逐 Ticket checkpoint；不得因此降低完成定义。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 Execution Authorization | Goal Plan、Ticket、backend branch/base 和 workspace 策略已定位 | 用户明确允许每 Ticket 非空 local implementation commit 与 direct-parent result；change authorization 同步为 authorized | 后续已授权单一合并提交，但逐 Ticket 条件未满足，继续阻塞 SpecDev 完成 | Lead `codex` / 用户 | 保留 `a98d6edcc` 事实，不改写历史补造 Ticket checkpoint；Plan/Ticket/change 继续 blocked/ready |
| G1 HTTP Contract Stable | G0 关闭，T-01 implementation commit 存在 | T-01 定向测试和 `./mvnw test` 通过；路径审查、双轴审查、direct-parent result SHA 与 T-01 Evidence 完整 | T-02 | Lead `codex` | 在同一 current workspace 修正 T-01；不得开始 T-02 或混入 admin Logback 修改 |
| G2 Log Persistence Verified | G1 关闭，T-02 implementation commit 存在 | 日志定向测试、真实文件验证与 T-02 direct-parent result 完整 | T-03 | Lead `codex` | 保持 T-02 in_progress，不开始 T-03 的正式完成流程 |
| G3 Backend Delivery Verified | G2 关闭，T-03 implementation commit 存在 | Controller/启动摘要定向测试、真实启动落盘、完整 test、clean full/core package 通过；AC-001..019 Evidence 和状态一致 | change completion | Lead `codex` | 保持 T-03 in_progress；按 attempt 上限修正或记录 blocker/deviation |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..AC-005 | T-01 | Filter/logger、CryptoFilter、UTF-8 和安全例外合同 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` | planned; G0 blocked |
| AC-006..AC-010 | T-01 | multipart/file/SSE、异常、async 和故障注入合同 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` | planned; G0 blocked |
| AC-011..AC-012 | T-01 | ApplicationContext 条件装配与配置绑定 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` | planned; G0 blocked |
| AC-013..AC-016 | T-02 | profile/logger、XML policy、旧文件清单和真实 file appender | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-02.md</Path>` | planned; blocked by G0/T-01 |
| AC-017..AC-019 | T-03 | Controller 时间、启动 INFO 事件、实际 WebServer 地址和物理文件 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-03.md</Path>` | observed in dirty workspace; completion blocked by G0/T-02 |
| ADR-001 安全例外 | T-01 + T-02 | 凭证原值精确断言与 sys-console 容量边界 | T-01/T-02 Evidence | planned; high-assurance |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex` | change leadership 当前为 codex；唯一 SpecDev 状态、Evidence、E2E disposition 与父分支 owner |
| Implementation subagents | 1，Lead 不计入 | config 上限 3，本计划因 current 串行和用户要求主动降为 1；当前不预分配或派遣 |
| Integration attempts | 4 | `<Path>{roots.state}/specdev/config.json</Path>` 创建时快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation 只读且不争用可变测试环境 |
| Dispatch | execution-time dynamic | provider、模型和是否派单在 Ticket 可启动时按授权与事实决定；当前 G0 阻止 dispatch |

Lead 保留需求解释、Gates、路径分配、SpecDev 工件、Evidence、验收和最终回复。若未来解除 G0，implementation subagent 只可在 `current` workspace、单 writer 锁和 Ticket writable paths 内工作；不得写 SpecDev 状态或 Evidence。禁止创建 source/candidate worktree。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend `main` / `336134c5ff6e80bb8775617b64c4e04bf6c9d222` | current backend workspace / `main` | common-web 定向测试；implementation owner 非 E2E | 合并提交 `a98d6edcc`，非 T-01 独立 checkpoint | Lead 未运行完整 `./mvnw test`；E2E not-required | `origin/main@a98d6edcc`，非逐 Ticket result |
| T-02 | 原计划要求 G1 的 T-01 result SHA | 同一 current backend workspace / `main` | admin logging 定向测试；implementation owner 非 E2E | 合并提交 `a98d6edcc`，非 T-02 独立 checkpoint | 真实 appender 已观察；clean full/core 未闭合 | `origin/main@a98d6edcc`，非逐 Ticket result |
| T-03 | 原计划要求 G2 的 T-02 result SHA | 同一 current backend workspace / `main` | admin Controller/启动摘要测试 | 合并提交 `a98d6edcc`，非 T-03 独立 checkpoint | 临时端口与 LOG_PATH 启动已通过 | `origin/main@a98d6edcc`，非逐 Ticket result |

当前 workspace 策略固定为 `current`，所有 Ticket 必须严格串行；不得创建 source/candidate worktree。聚合仓库基线为 `main@f13991631e54a259c0ba21a1c951b788c2659685` 且已有用户修改，必须保留；backend 子模块当前 `main@336134c5ff6e80bb8775617b64c4e04bf6c9d222` 为 clean，执行前仍需重新 preflight。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | completed-outside-ticket-checkpoints | 用户授权非完成模式实现；实际范围已进入合并提交 `a98d6edcc` |
| Ticket worktree local changes | not-authorized | 用户明确“不用 worktree”；本计划禁止创建任何 Ticket/candidate worktree |
| Implementation commit | authorized-and-completed | 后续授权一个合并范围提交；`a98d6edcc` 已创建，但每 Ticket checkpoint 仍缺失 |
| Local direct-parent verification and parent update | completed-with-deviation | 直接提交到 main 并推送；不是原计划逐 Ticket direct-parent 序列 |
| Local candidate integration and parent update | not-authorized | current 模式不适用且用户禁止 worktree |
| Push / PR / remote merge | push-authorized-and-completed | `a98d6edcc` 已推送 `origin/main`；未创建 PR 或远程 merge |
| Branch/worktree cleanup | not-authorized | 本计划不创建 branch/worktree，也不清理用户状态 |
| Deploy / migration / production actions | not-authorized | 不部署、不操作服务器日志或历史文件 |

### Evidence Return

Lead 已复核并记录合并提交 `a98d6edcc`、实际路径、测试与推送状态。该 SHA 可用于审计整体交付，但 Evidence 必须继续注明它不是逐 Ticket implementation/direct-parent result。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- current workspace、严格 `T-01 -> G1 -> T-02 -> G2` 串行、同一时间一个项目 writer。
- 不创建 worktree、candidate branch 或临时 integration checkout。
- 在用户改变授权前不 commit；因此也不启动 SpecDev implementation，不把 Ticket 标记 done。
- 不修改 Ticket writable paths 之外的项目文件；T-01 独占 common-web，T-02 独占 admin Logback/testing。
- HTTP 凭证原值例外只适用于专用 logger；文件/流式排除、1MiB 上限、fail-open 和 sys-console 同步写入不弱化。
- 不删除历史文件、不增加 application YAML 默认项、不扩展到其他部署应用或 `@Log`。

### Verification Integrity

判卷接缝以 Ticket 验证矩阵为权威。禁止通过删除测试、放宽断言、跳过 async/CryptoFilter/真实文件测试、把普通日志全部改成 JSON、用静态 XML 代替物理文件验证或用 `-DskipTests` 冒充 test 通过获得绿色。

current/direct-parent 两层边界要求：implementation owner 在 current workspace 运行 Ticket 定向检查并形成 commit；Lead 在同一父分支运行集成回归并记录 result SHA。T-01/T-02 为 E2E not-required，T-03 为本机进程级 E2E required；当前只有一个跨 Ticket 合并提交，因此该两层验证仍不能按原 DAG 闭合。

### Migration or Release Sequence

无数据库迁移。若 G0 关闭，发布顺序固定为：T-01 先原子替换旧 `[PLUS]` 日志并稳定专用 logger，再由 T-02 收缩 admin appender；部署后停止 info/error 新写入但保留旧文件。回滚按 T-02 后 T-01 的逆序恢复，不涉及数据回滚。生产部署不在本计划授权内。

### Risks, Monitoring and Recovery

- **凭证与容量：** 日志包含可重放凭证且同步写入；由 1MiB 单方向上限、文件/流排除、60 天/40GB 和显式关闭开关控制事故半径。
- **Filter 语义：** 包装顺序错误可能改变加密、响应或异步；G1 以组合和 async 合同测试阻断 T-02。
- **混合文件格式：** HTTP JSON 行和普通文本共用一个文件；G2 必须用真实 appender 解析物理行。
- **dirty 聚合仓库：** 已有用户改动不得回退；实现只在 clean backend 子模块内按 Ticket 路径写入，执行前后分别重读。
- **合并 checkpoint：** 当前提交可恢复整体基线，但无法隔离三个 Ticket 的实施与 direct-parent 结果；不改写 Git 历史补造证据，change 保持 blocked。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。路径扩张、测试接缝失效、logger/wire contract 变化为 ticket/spec deviation；凭证例外、Filter 边界或持久化架构变化返回 ADR/Spec owner；commit/workspace/发布门变化属于 Goal Plan/release control，未经用户明确批准不继续。

## 6. Progress and Decisions

### Current Status

- Goal Plan 已选择 `current + direct-parent`，implementation agent 上限 1，integration attempt 上限 4。
- backend 基线实测为 `main@336134c5ff6e80bb8775617b64c4e04bf6c9d222` 且 clean；聚合仓库存在与本 change 无关的用户修改。
- T-01、T-02、T-03 共覆盖 19 个合同；T-03 是对 Goal Plan 后用户追加范围的追认。
- 用户已授权单一后端 commit/push；`main` 与 `origin/main` 均为 `a98d6edcc591550221dd983e293d43e3aac36d23`，子仓库工作区 clean。该 SHA 是整体结果，不是逐 Ticket result。
- T-03 的 3 个定向测试、36 模块非 clean package 和 18081 临时实例 sys-console 落盘已观察通过；未 clean fat jar 暴露存量 MapStruct 重复方法，clean full/core package 尚未验证。

### Pending Decisions and Blockers

- `BLK-003`：实际只有跨 T-01/T-02/T-03 的合并提交，无法满足逐 Ticket checkpoint；完整 `./mvnw test` 和 clean full/core package 尚未闭合，因此 `ready_for_execution=false` 且 change 不能完成/归档。
- 不通过重写已经推送的 main 历史补造 checkpoint。后续只能按 change completion 规则接受明确 deviation 并补齐剩余门禁，或继续保持非 SpecDev 完成状态。

### Resume Protocol

恢复时依次读取本 Goal Plan、T-01、T-02、T-03、change status 和最新 Evidence；确认 backend `main`/`origin/main` 是否仍指向 `a98d6edcc`，再决定是否补跑完整门禁和处理 checkpoint deviation。保持 blocked、不创建 worktree、不改写已推送历史，直到完成合同被明确闭合或批准偏差。

## Assumptions

- backend `main` 是三个 Ticket 的项目父分支；执行前必须重新验证，若已漂移只更新基线和 Gate，不猜测合并策略。
- 当前没有 required E2E；若实现事实出现必须跨进程或真实部署才能证明的风险，先暂停并更新 Ticket/Goal Plan disposition。
- 用户对“不 commit”的决定是明确高影响执行约束，因此本计划必须保持 `ready_for_execution: false`，不把它降格为可继续的低影响假设。

## 7. Completion Closure（2026-08-28，权威最终状态）

本节取代上文执行过程中保留的 blocked、pending 和 resume 叙述；历史内容不改写，以便审计当时为何停止。

- 用户授权处理全部未满足归档条件。`DEV-COMP-001` 批准把已发布的合并提交 `a98d6edcc591550221dd983e293d43e3aac36d23` 作为 T-01、T-02、T-03 的共享非空 source checkpoint，不重写历史补造提交。
- 根 POM 的测试引擎修复提交为 `f88012eae14374bb6f59b0389e3e937345ccde7e`；它是三个 Ticket 的最终 backend `main` result。
- `./mvnw clean test` 在 41 个模块全部通过，common-web 的 15 项日志测试为 0 failure/error/skip；此前 `ruoyi-demo` 的一次 MapStruct 临时生成损坏在相同源码的干净重跑中消失，最终全量门禁稳定通过。
- `./mvnw clean package -DskipTests` 与 `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` 均通过；父仓库的 `verify-admin-bundle.sh full` 和 `core` 均验证成功。
- 2026-08-26 端口 18081 的真实进程 E2E 已证明根路径和启动摘要进入物理 `sys-console.log`。2026-08-28 重试使用 clean core jar 写出 602 行日志后因本地 MySQL 3306 不可用退出；`a98d6edcc..f88012eae` 对所有运行日志与 Controller 源路径零差异，因此 `DEV-COMP-002` 批准证据连续性。
- T-01、T-02、T-03 均为 done，19 个验收合同保持 covered，所有 blocker 已清空，所有偏差均为 approved；Goal Plan 与 change 状态为 completed。
