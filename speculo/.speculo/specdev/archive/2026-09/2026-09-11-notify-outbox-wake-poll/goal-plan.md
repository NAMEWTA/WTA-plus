---
schema_version: 6
artifact: goal-plan
change: 2026-09-11-notify-outbox-wake-poll
status: ready
modes: []
orchestration: lead-directed
lead: rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00
implementation_agent_limit: 3
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: true
---

# Goal Plan: 通知 Outbox 写入唤醒与慢速兜底轮询

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`

## 0. Mandatory Project Skill Gate

本计划与全部 Ticket 的执行前置条件：未按顺序读完项目 Skill，不得改产品代码、不得宣称 Ticket 开始。

**固定读取顺序（每个 Ticket、每次派单）：**

1. 本 Goal Plan
2. `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>` 中「项目 Skill 读取矩阵」
3. 矩阵里适用于 `ALL` 或当前 Ticket ID 的每一个 `<Path>.agents/skills/**/SKILL.md</Path>` **全文**
4. 当前 Ticket 的 skill_bindings / references 与正文
5. 相关上游 Spec/ADR/CONTEXT 与只读代码事实

矩阵是最低必读集合，不是 allowlist。实现中新命中的项目 Skill 必须先由 Lead 写入 Map 并重新校验，再继续。

| Ticket | 必须调用的项目 Skill（SKILL.md） | 进入 Ticket 前额外 references |
|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`；`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`；`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | 通知边界、事务 afterCommit、测试门禁；notify 模块事实；RedisUtils pub/sub 或 list |
| T-01 | ALL 三项 | notification.md；persistence-transactions-and-ddl；notify index；other-utils（Redis） |
| T-02 | ALL 三项 | 同上；Worker 调度不得绕过 lease / 不得直连 Redisson |
| T-03 | ALL 三项 | testing.md；跨进程/并发夹具与禁止假绿 skip |

Dispatch Packet（若派 implementation subagent）必须把上述 Skill 路径写入 packet，并写明「Map → Skill → Ticket」。未读 Skill、跳过 references、或用「沿用现有写法」代替硬约束时，Lead 停止该 Ticket。

## 1. Outcome and Authority

### Outcome

在 `ruoyi-notify` 内把 Outbox Worker 调度从「约 1s 空转 claim」改为「跨进程写入唤醒为主 + 默认 60s 慢速兜底」：业务事务提交成功并写出可 claim 的 `notify_outbox` 行后，经 Redis pub/sub 或 list 唤醒集群内 Worker；Worker 被唤醒或兜底 tick 后只走既有 `claim` + `dispatch`（可连捞到空批），不绕过 lease、不带 outboxId 默认直投。同 JVM afterCommit 辅信号可选，但不得替代跨进程主路径，也不得单独用来宣称 AC-002。失败退避后的 `next_attempt_at` 到期只靠慢速兜底再进 claim。

### Success and False Completion

成功必须同时满足：

- `AC-001`–`AC-006` 均有 Lead 可复查 Evidence；
- T-01–T-03 各有非空 implementation commit、current-workspace 非 E2E 检查，以及 Lead-owned **Local direct-parent verification and parent update**；
- 默认 `notify.outbox.poll-delay-ms=60000`（显式 1000 仍可用，但产品默认不得回落 1000）；
- AC-002 有跨进程（共享 Redis+DB）证据，典型 <1s claim；不得仅同 JVM 路径顶替；
- 并发 claim 下至多一个有效 lease 完成写回；diff 不触 common-notify 同步契约与通知配置页/渠道账号。

以下不算完成：只有同 JVM 唤醒、空闲仍约 1s claim、唤醒绕过 lease/直投、把 Redis 失败升级为提交失败、夹具缺 Redis/DB 时 skip 充当 AC-002 通过、或扩 scope 到配置页/common-notify/MQ Relay。

### Non-goals

- 不开启 Ticket worktree / candidate-merge（本计划固定 `ticket_workspace_policy: current` + `integration_gate: direct-parent`）。
- 不取消 Transactional Outbox；不做 Outbox→MQ Relay；不扩通知配置页；不改 `ruoyi-common-notify` 同步 Dispatcher。
- 不为 `next_attempt_at` 建合同级延迟唤醒或重试专用快轮询。
- 不远程 push/PR/merge、不部署生产、不自动清理 worktree。
- 本 Plan 已与 `.status.json` 授权对齐：`implementation_commit` 与 `local_candidate_integration` 已 authorized；`source_cleanup` 仍 not-authorized。**Lead 明确暂不派 I-implement**——就绪不等于已开跑；未收到 Lead 派单不得自启实现。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定（含 Lead/CTO 锁定项与 worktree/授权） | 产品取舍、workspace 策略、实现提交授权 | 更新真正拥有该决策的工件；授权只写 `.status.json` 的 `execution_authorization`，本 Plan 不得私自改写 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | 当前 change 架构与术语 | 返回 G-grill |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 永久 ADR-0006/0007 薄 common-notify | 本 change 不得把可靠重试塞进 common-notify |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为与 AC | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 契约 | Goal Plan 只编排 |
| 6 | 当前代码与 Git 工作区基线 `6352f7b291aaaf63c977253b7849767b855dd680`（detached HEAD；`main` @ `c4c0dced04a7f88eb7c9a4dffb68fbaee3f32c66`，HEAD 领先 5 commit） | 可执行事实：Worker 仍为 `poll-delay-ms:1000`；无提交后唤醒 | 冲突则偏差控制 |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 [afterCommit 跨进程唤醒发布]
  └─→ T-02 [Worker 订阅 + 默认 60s 兜底 + 统一 claim/dispatch]
        └─→ T-03 [跨进程 <1s / 并发租约 / 范围门禁]
```

关键路径即全链 `T-01 → T-02 → T-03`。无写路径并行扇出。`current` 模式下 Wave 仅作依赖投影，不得当作并发写授权——同一时间只有一个 implementation writer。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | Skill 门禁通过；实现提交已授权 | Runtime 写入唤醒发布、support/outbox、对应测试 | 语义资源 `notify.outbox.wake.signal-contract` / after-commit-publish：T-01 | G1 |
| W2 | T-02 | T-01 result；信号合同稳定 | `adapter/worker/` 订阅与默认 60s、Worker 测试 | 消费 T-01 信号合同（只读发布类型）；`poll-delay` 默认与 claim-dispatch 循环：T-02 | G2 |
| W3 | T-03 | T-01+T-02 result | **仅测试**（跨进程 IT / 并发 / ScopeGate / support） | 无生产 shared path；门禁资源归 T-03 | G3 |

文件级 `shared_paths` 均为空；跨票共享仅为信号合同语义，由 DAG 串行保证唯一写 owner。与并行 change `2026-09-10-notify-channel-config`：本 change 写集收紧到 Outbox Runtime 唤醒、`adapter/worker` 与相关测试，避开配置页/渠道账号；Lead 禁止两 change 同时改同一文件。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 提交后跨进程唤醒发布；Redis 失败不回滚 | — | `current` | Lead / dynamic dispatch | not-required：无 UI/HTTP 新边界；跨进程门禁由 T-03 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-02 | Worker 订阅；默认 60s 兜底；统一 claim/dispatch | T-01 | `current` | Lead / dynamic dispatch | not-required：组件级可证；跨进程 <1s 在 T-03 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| T-03 | 跨进程 <1s、并发不双投、范围门禁 | T-01, T-02 | `current` | Lead / dynamic dispatch | not-required：以模块跨进程/并发 IT 为跨边界证据，不要求 UI E2E；缺 Redis+DB 夹具时阻塞，不得降级仅同 JVM 宣称 AC-002 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

全部非 cancelled Ticket Done；AC-001–AC-006 有通过 Evidence；默认空闲 claim 为 60s 量级；跨进程唤醒主路径与 lease 不双投已证；范围门禁通过；无未集成 dirty 实现；change 状态/Map/Goal Plan/Evidence/Git 一致。票全 done 不等于 Goal 自动完成——须 Lead 关闭 G3 并做整体验收。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 Skill | 任一 Ticket 开始前 | Map+Skill+Ticket 已读记录在 Evidence / Dispatch Packet | 该 Ticket | Lead | 未读则不准写代码 |
| G1 信号合同 | T-01 commit + direct-parent | afterCommit 后 Redis 发布；提交前无信号；Redis 失败不回滚；载荷无 PII；diff 避开配置页/common-notify | 不得声称多实例 <1s 已兑现 | Lead | 父分支不推进；保留 Ticket 继续修 |
| G2 Worker 调度 | T-02 commit + direct-parent；G1 已关 | 默认 60000；唤醒与兜底共用 claim+dispatch；漏唤醒兜底可捞；`next_attempt_at` 未到期不因唤醒领走；触发来源可区分 | T-03 门禁 | Lead | 父分支不推进 |
| G3 跨进程与范围 | T-03 commit + direct-parent；G1+G2 已关 | AC-002 跨进程证据（Redis+DB）；AC-004 并发 lease；AC-006 ScopeGate；无假绿 skip | change 完成 | Lead | 夹具不可用 → Ticket blocked，不降级同 JVM |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001 | T-02 | Worker 默认 delay / 空闲 claim 频率 | T-02 Evidence | planned |
| AC-002 | T-01（写路径）, T-03（跨进程门禁） | Publisher；CrossProcess IT（Redis+DB） | T-01 / T-03 Evidence | planned |
| AC-003 | T-02 | 抑制唤醒后兜底 tick | T-02 Evidence | planned |
| AC-004 | T-03 | 并发 claim/lease | T-03 Evidence | planned |
| AC-005 | T-02 | `next_attempt_at` 定向测试 | T-02 Evidence | planned |
| AC-006 | T-01, T-03 | 路径审查 + ScopeGate | T-01 / T-03 Evidence | planned |
| DEC-002/003/009；I2/I4 | T-01–T-03 | 跨进程主路径 + afterCommit | 各票 | planned |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00` | 唯一 SpecDev 状态、Evidence 与父分支 owner；Lead 不计入 implementation subagent |
| Implementation subagents | 上限 3，Lead 不计入；**current 同时只允许 1 个 writer** | config `max_implementation_agents=3` 快照；本计划不提高 |
| Integration attempts | 3 | config `max_integration_attempts=3` 快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写状态、不争用可变测试环境 |
| Dispatch | execution-time dynamic | provider/模型/派单按 Ticket 事实选择；packet 必须含项目 Skill 列表与 writable/read-only 边界 |

subagent-delivery `operation=plan` 合同：允许 task_kind = implementation | review | research | test-observation；implementation 在 current 模式持有串行 writer 锁；E2E Gate（含 T-03 跨进程 IT 在 Lead 核对后的验收）永远由 Lead 拥有；subagent 不写 Ticket/Map/Goal Plan/Evidence/change status/父分支。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | 工作区基线 `6352f7b291aaaf63c977253b7849767b855dd680` 起；实现前 Lead 确认挂回/对齐 `main` 策略 | current / parent branch | `NotifyOutboxWakePublisherTest` 等定向模块测试 + 路径审查 | 每 Ticket 必需 | Lead **Local direct-parent verification and parent update**；E2E not-required | result_sha = implementation commit |
| T-02 | T-01 result | current / parent | `NotifyOutboxWorkerWakePollTest` | 必需 | Lead direct-parent；E2E not-required | 同上 |
| T-03 | T-02 result | current / parent | CrossProcess IT + Concurrency + ScopeGate（需 Redis+DB） | 必需 | Lead direct-parent；跨进程 IT 由 Lead 在 current workspace 核对；E2E disposition=not-required（非 UI） | 同上 |

当 `ticket_workspace_policy: current` 时，Ticket 必须严格串行。Lead 每次只允许一个 implementation owner 写入当前 workspace；完成非 E2E 检查并形成 commit 后，Lead 在同一父分支/current workspace 运行适用集成检查和 E2E，验证通过后将该 Ticket 的 `result_sha` 记录为其 implementation commit，再开始下一个 Ticket。不得创建 source/candidate worktree。

**Worktree 策略选择记录：** 创建本 Goal Plan 时固定 **不开启** Ticket worktree（`current` + `direct-parent`）。理由：三票 DAG 全串行、写集无真实并行扇出、用户/Lead 默认 current；独立 worktree 无收益。Wave 仅投影依赖，不授权并发写。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | authorized | 与 `.status.json` → `execution_authorization.implementation_commit=authorized` 对齐；限 Ticket writable_paths；仅在 Lead 派 I 后写入 |
| Ticket worktree local changes | not-authorized | 本计划不开启 worktree |
| Implementation commit | authorized | 真源：CTO via Lead，`granted_at=2026-09-11T15:53:14+08:00`；本地 Ticket commits（限 writable_paths；不含 push/PR） |
| Local direct-parent verification and parent update | authorized | 真源同 `local_candidate_integration=authorized`；current 模式下 Lead 核对 Ticket commit 后推进 |
| Local candidate integration and parent update | not-authorized | 本计划不使用 candidate-merge（字段授权供 direct-parent 语义；不启用 candidate worktree） |
| Push / PR / remote merge | not-authorized | 不从本计划本地授权继承 |
| Branch/worktree cleanup | not-authorized | `source_cleanup` 仍 not-authorized；成功集成不自动继承 |
| Deploy / migration / production actions | not-authorized | 无 DB 迁移；禁止生产部署作为本 Plan 副作用 |

说明：授权真源仅为 `.status.json` 的 `execution_authorization`。本 Matrix 是投影；禁止在修订 Goal Plan 时擅自改写授权字段。就绪后仍须等 Lead 派 I-implement。

### Evidence Return

subagent 只返回候选事实与 commit；Lead 独立核对并写 Evidence、状态和最终验收。E2E Gate / 跨进程门禁验收只属于 Lead。source worktree 不运行 E2E（本计划无 source worktree）。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 仅改 `ruoyi-notify` Outbox 写入唤醒 / Worker 调度 / 相关测试；Redis 跨进程唤醒为主，同 JVM 可辅。
- 默认 poll 60s；配置项名 `notify.outbox.poll-delay-ms` 保留。
- 唤醒必须 afterCommit；唤醒后只 `claim`+`dispatch`；禁止 outboxId 默认直投。
- T-03 跨进程夹具依赖 Redis+DB；不得降级仅同 JVM 宣称 AC-002。
- 必须按第 0 节调用项目 Skill；跳过即停止。
- 路径引用遵守 path-reference-contract：权威工件禁止内部相对链接、裸文件名、机器绝对路径。

### Verification Integrity

判卷接缝为 Ticket 验证矩阵与 Spec AC。禁止删测试、放宽断言、静默 skip 缺夹具、或用同 JVM 方法调用顶替跨进程证据。current-workspace 跑非 E2E；Lead 在同一 workspace 跑集成与适用跨进程 IT 核对。禁止伪绿色：缺 Redis/DB 时必须 blocked。

### Migration or Release Sequence

无数据/表结构迁移。建议发布顺序：

1. T-01 信号发布与 T-02 Worker 订阅+60s 默认同波上线（单独 T-01 不得声称 <1s 已兑现；单独 T-02 仅获慢轮询收益）。
2. T-03 验证证据随实现波次，不改变运行时默认。
3. 运维知悉：重试可见性上界约等于兜底间隔（默认 ~60s）；环境需已有 Redis（与现网一致）。

### Risks, Monitoring and Recovery

- Redis pub/sub 无订阅者时丢失 → 依赖慢速兜底；list 需防堆积且提交不依赖 Redis 成功。
- 多实例同时唤醒造成短暂 claim 风暴 → 可接受；`SKIP LOCKED` 限制伤害。
- 与 `2026-09-10-notify-channel-config` 同模块冲突 → 写集隔离；冲突时暂停受影响闭包。
- 工作区：detached HEAD 领先 `main` 5 commit；另有 `application-local.yml` dirty——实现前提交范围必须隔离本 change。
- T-03 环境缺 Redis/DB → blocked，保留失败事实，不假绿。
- 失败：父分支不推进；保留 Ticket；integration attempts 满 3 次后 Lead 复盘再派（须 Evidence 写明改变点）。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。发现 T-01/T-02 生产缺口时 T-03 不得改 main——停止并回退对应票或提 deviation。

## 6. Progress and Decisions

### Current Status

- Wave/Gate：计划已写入；`ready_for_execution: true` / `status: ready`（实现授权已落；**Lead 暂不派 I**）；尚未开始实现。
- Git 工作区基线 `6352f7b291aaaf63c977253b7849767b855dd680`（detached）；`main` @ `c4c0dced04a7f88eb7c9a4dffb68fbaee3f32c66`。
- 实测：`NotifyOutboxWorker` 仍为 `@Scheduled(...poll-delay-ms:1000)`；`RedisUtils.publish/subscribe` 可用。
- 无 Ticket implementation SHA；Evidence 目录待填。

### Pending Decisions and Blockers

1. **已解除：** `implementation_commit` 与 `local_candidate_integration` 已 authorized（CTO via Lead，`2026-09-11T15:53:14+08:00`）。本 Plan 已升 `ready` / `ready_for_execution: true`。
2. **调度门：** Lead **明确暂不派 I-implement**——不得自启实现。
3. **实现前确认：** detached HEAD 与 `main` 对齐/挂接策略（不影响 Plan 文档完备性，影响 run 启动）。
4. Push/PR/部署/`source_cleanup` 仍未授权（预期，非本 Plan 升 ready 的必要条件）。

### Resume Protocol

恢复时读取 Goal Plan、当前 Ticket、`.status.json`、tickets-map 和最新 Evidence；从最后通过的父分支 result 或待修正 implementation checkpoint 继续。下一 Ticket 开始前重做第 0 节 Skill 门禁。授权变更后须回读 `.status.json` 真源，再修订本 Plan 的 Authorization Matrix 与 `ready_for_execution`。

## Assumptions

- 低影响：Redis 通道/list 键名、载荷是否含 outboxId（仅 hint 仍必须 claim）、同 JVM 辅信号是否落地——由实现按 `RedisUtils` 与模块惯例选择，以 AC-002/AC-004 验证。
- 低影响：claim 批次 50、lease 60s 等现网常量本期不强制调整。
- 无高影响未决假设。存在高影响假设时 `ready_for_execution` 必须为 `false`。
