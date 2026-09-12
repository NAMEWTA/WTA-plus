---
schema_version: 6
artifact: goal-plan
change: 2026-08-20-namewta-client-rbac-review
status: completed
modes: [high-assurance, reference-conformance, release-coordination]
orchestration: lead-directed
lead: codex-root
implementation_agent_limit: 1
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: Client RBAC Review 整改

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

关闭 CR-001 的代码 finding，基于当前 upstream 形成失败关闭、跨端一致且父仓库可复现的 Client RBAC 交付组合。

### Success and False Completion

成功要求四个适用 Gate 全部关闭。Runtime E2E 由用户于 2026-08-21 明确设为 `not-required`；它没有被执行，也不得被描述为通过。

### Non-goals

不 push、部署、生产迁移、重写历史、新增依赖/测试框架/schema 或兼容层。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户批准计划与可选注册身份决定 | 产品取舍与授权 | 更新 Spec |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>` | 外部行为与验收 | 下游不得改写 |
| 3 | `<Path>plan/update.md</Path>` 与 `<Path>docs/upstream/customization-map.md</Path>` | 长期安全合同 | 返回 Spec/Ticket owner |
| 4 | Tickets 与当前代码 | 实现合同与事实 | 记录 deviation |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 -> T-02 -> T-03 -> T-04
```

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | refs 冻结 | backend Git/upstream diff | 无 | G1/1 |
| W2 | T-02 | G1 | backend auth/system/api | 无 | G2/2 |
| W3 | T-03 | G2 | frontend API/views | 无 | G3/3 |
| W4 | T-04 | G2+G3 | parent docs/gitlinks | codex-root | G4/4 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | upstream 基线 | — | current | Lead | not-required | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-01.md</Path>` |
| T-02 | backend strict contracts | T-01 | current | Lead | not-required；用户在 T-04 豁免 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-02.md</Path>` |
| T-03 | frontend consumers | T-02 | current | Lead | not-required；用户在 T-04 豁免 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-03.md</Path>` |
| T-04 | integrated delivery | T-02,T-03 | current | Lead | not-required；用户明确决定 | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-04.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

Tickets、Evidence、代码、child commits、父 gitlinks、双端适用门禁和 CR-002 后续处置一致；runtime E2E 明确为 `not-required`；validator 无错误。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G1 | T-01 开始 | merge commit + Maven package | 全部后续 | codex-root | revert merge |
| G2 | G1 | backend commits + tests/package | frontend/integration | codex-root | 修复当前 commit |
| G3 | G2 | frontend commits + lint/build | integration | codex-root | 修复当前 commit |
| G4 | G2+G3 | 双端适用门禁、CR-002 代码/规范结果、parent tree、E2E 豁免记录 | 完成 | codex-root | 修正 Evidence/状态投影后重跑 validator |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..005 | T-02,T-03,T-04 | backend/frontend 静态、测试、构建与审查 | T-02..04 | closed；runtime E2E not-required |
| AC-006 | T-01,T-04 | Git/build/tree | T-01,T-04 | closed |
| CR-001 | 全部 | CR-002 + 用户后续验收决定 | T-04 | closed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | codex-root | 唯一 Speculo、Evidence 与父分支 owner |
| Implementation subagents | 1，Lead 不计入；本次不派遣 | current 串行与平台限制 |
| Integration attempts | 3 | config 快照 |
| Read-only agents | 无 SpecDev 数字上限 | 本次不派遣 |
| Dispatch | execution-time dynamic | Lead 自行实现 |

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend main/a426d5f7a | current/main | Git+package | merge commit | direct-parent, no E2E | merge SHA |
| T-02 | T-01 result | current/main | tests+package | focused commits | direct-parent；runtime E2E not-required | final backend SHA |
| T-03 | frontend main/3122cbac3 | current/main | lint/build/type diagnostic | focused commits | direct-parent；runtime E2E not-required | final frontend SHA |
| T-04 | parent main/0909fb782 | current/main | docs/tree/full gates | parent pointer commit | direct-parent；runtime E2E not-required | final parent SHA |

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | allowed | 用户要求 Implement the plan；严格串行 |
| Ticket worktree local changes | not-authorized | current 模式不使用 |
| Implementation commit | allowed | 本地聚焦 commits，不 push |
| Local direct-parent verification and parent update | allowed | 每 Ticket 验证通过后 |
| Local candidate integration and parent update | not-authorized | current 模式不使用 |
| Push / PR / remote merge | not-authorized | 独立授权 |
| Branch/worktree cleanup | not-authorized | 不创建来源 worktree |
| Deploy / migration / production actions | not-authorized | 仅 disposable 验证环境可用时执行 |

### Evidence Return

Lead 直接实现并独立核对 Git、命令、路径和结果；无 subagent Evidence 写入。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

失败关闭、Long Client PK、无全局 fallback、无依赖/schema/lockfile/测试源码变化、父指针只指向已验证 child commits。

### Verification Integrity

不以 package 代替 tests，不把未执行的 runtime E2E 写成通过。用户明确将 E2E 改为 `not-required`；current/direct-parent 每 Ticket 都重读 HEAD、diff 与状态。

### Migration or Release Sequence

upstream -> backend contracts -> frontend consumers -> docs/适用门禁 -> parent pointers。发布建议前端先于严格后端，但本次不部署。

### Risks, Monitoring and Recovery

Runtime E2E 未执行，运行时残余风险由用户接受；该风险保留在 Evidence 中但不再阻塞 G4。局部非运行时 Gate 失败仍须修复并重跑，合同偏差回到 Spec/Ticket。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`；已发布旧提交拆分不重写，CR-001 已记录。

## 6. Progress and Decisions

### Current Status

T-01/T-02/T-03 已完成（backend `eeab21d91`，frontend `adf5a0c`）；文档、双端门禁与父快照已完成。用户将 runtime E2E 明确设为 `not-required` 后，W4/T-04 与 G4 已关闭；push/deploy 不属于本计划。

### Pending Decisions and Blockers

无 blocker。Disposable SQL、HTTP、Token 与浏览器矩阵未执行，依据用户于 2026-08-21 的明确决定记录为 `not-required`。

### Resume Protocol

读取 Goal Plan、当前 Ticket、change worktree 状态和最新 Evidence；从最后通过的 result SHA继续，禁止跳过依赖 Gate。

## Assumptions

用户的 “Implement the plan” 覆盖本计划明确列出的本地 commits 和父 gitlink 更新，不覆盖 push、远程合并、部署或生产数据操作。
