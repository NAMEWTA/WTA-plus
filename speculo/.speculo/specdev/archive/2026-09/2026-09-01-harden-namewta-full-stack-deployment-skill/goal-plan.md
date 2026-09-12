---
schema_version: 6
artifact: goal-plan
change: 2026-09-01-harden-namewta-full-stack-deployment-skill
status: completed
modes: [high-assurance, reference-conformance]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: null
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: 强化 NAMEWTA 全栈部署 Skill

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

部署 Skill 能以 v2 profile/state 和本地工具阻止本次真实发生的发布错误，并通过精简主 Skill、完整一级 runbook 和私密报告指导后续全栈部署。

### Success and False Completion

成功要求 AC-001..010 全部有自动或静态 Evidence。只增加文档、只增加字段、只检查 HTTP 200、只验证一个实例、把 waiver 设为通用选项或在 stdout 输出 secret 都是伪完成。

### Non-goals

不修改产品、release assets、服务器、数据库或外部系统；不清理其他 change 和失败资产。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | 范围与执行授权 | 更新拥有该决定的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/spec.md</Path>` | 外部行为与验收 | 下游不得改写 |
| 3 | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/ticket/</Path>` | 单 Ticket 合同 | 本计划只编排 |
| 4 | 上一 change T-01/T-02 Evidence 和当前工作树 | 真实故障与实现事实 | 冲突触发偏差 |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 -> G1 -> T-02 -> G2 -> T-03 -> G3 -> T-04 -> G4
```

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | 当前基线 | profile/lib/test | 无 | G1/1 |
| W2 | T-02 | G1 | candidate/state/frontend scripts | 无 | G2/2 |
| W3 | T-03 | G2 | Skill/references | 无 | G3/3 |
| W4 | T-04 | G3 | render/report/templates/test | 无 | G4/4 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | v2 profile | — | current | Lead | not-required: pure JSON | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-01.md</Path>` |
| T-02 | candidate gates | T-01 | current | Lead | not-required: local CLI fixtures | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-02.md</Path>` |
| T-03 | runbook | T-02 | current | Lead | not-required: documentation | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-03.md</Path>` |
| T-04 | render/report | T-03 | current | Lead | not-required: local CLI integration | `<Path>{roots.state}/specdev/changes/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-04.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

四个 Ticket 实现、direct-parent 验证和 Evidence 完成；Node 全套、SpecDev validator、链接/secret/diff 审查通过；没有外部副作用或未批准偏差。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G1 | T-01 开始 | profile 正负向与 v1 回归 | T-02..04 | codex:/root | 修正 T-01 |
| G2 | G1 | candidate/frontend/waiver 矩阵 | T-03..04 | codex:/root | 修正 T-02 |
| G3 | G2 | Skill/reference 静态审查 | T-04 | codex:/root | 修正 T-03 |
| G4 | G3 | render/report、全回归、SpecDev/diff | change 完成 | codex:/root | 修正 T-04 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..002 | T-01 | unit | T-01 | verified |
| AC-003..007 | T-02 | unit/CLI | T-02 | verified |
| AC-009 | T-03 | static | T-03 | verified |
| AC-008, AC-010 | T-04 | integration/regression | T-04 | verified |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev、项目与父分支 owner |
| Implementation subagents | 上限 3，但本计划不派单 | current 单 writer与用户未要求 delegation |
| Integration attempts | `null` | config 快照 |
| Read-only agents | 不使用 | 当前范围可由 Lead 完成 |
| Dispatch | `execution-time dynamic`，本计划当前为 Lead direct | 无 provider 数据发送 |

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01..04 | 前一 Ticket result | current/main | Ticket 定向检查 | 每 Ticket 非空 commit | current direct-parent；E2E not-required | 每 Ticket commit |

所有 Ticket 严格串行；前一 Ticket 的定向和集成检查通过后才开始下一 Ticket。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | allowed | 限 Ticket writable paths |
| Ticket worktree local changes | not-authorized | current 模式不创建 |
| Implementation commit | allowed | `USER-DECISION:2026-09-01-execute-confirmed-plan` |
| Local direct-parent verification and parent update | allowed | Lead 核对后串行推进 |
| Local candidate integration and parent update | not-authorized | required 模式不适用 |
| Push / PR / remote merge | not-authorized | 本地授权不继承 |
| Branch/worktree cleanup | not-authorized | 不创建或清理 |
| Deploy / migration / production actions | not-authorized | 明确 OUT |

### Evidence Return

Lead 直接实现、独立核对 Git/测试并写 Evidence；不向其他 Agent 派单。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 不触碰服务器、数据库、产品源码、release assets 和其他用户改动。
- secret 不进入状态、stdout、Speculo 或 Git。
- 生产不接受 backup waiver；失败门禁不得通过放宽测试制造绿色。

### Verification Integrity

先对每类真实故障建立红灯，再实现最小逻辑并运行旧回归。current/direct-parent 的固定点为每 Ticket 开始前 HEAD 与实现 commit。

### Migration or Release Sequence

只迁移本地 JSON 模板：v1 可读，v2 为新 rollout 合同；无外部发布。

### Risks, Monitoring and Recovery

过严校验通过负向/兼容矩阵修正；任何外部需求触发 release deviation 并停止。代码恢复使用对应 Ticket commit，不删除失败 Evidence。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。

## 6. Progress and Decisions

### Current Status

G4 已关闭：T-04 result `cb561d4`；部署 Skill Node `25/25`、SpecDev implement、链接/JSON/语法/secret/diff 门禁通过。四个 Ticket 与 AC-001..010 全部完成；外部副作用未授权且未执行。

### Pending Decisions and Blockers

无。

### Resume Protocol

读取本 Goal Plan、当前 Ticket、`.status.json`、最新 Evidence 和 `git status`，从最后通过的 direct-parent result 继续。

## Assumptions

- 用户“执行”授权上一轮明确的四 Ticket 本地实现与必要 implementation commits，不授权 push、部署、迁移或 cleanup。
- 当前模式默认不开启 worktree；同一时刻只有 Lead 写本 change 路径。
