---
schema_version: 6
artifact: goal-plan
change: 2026-08-27-plus-ui-backend-aligned-domains
status: completed
modes: [migration, high-assurance]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 1
integration_attempt_limit: 7
ticket_workspace_policy: required
integration_gate: candidate-merge
ready_for_execution: false
---

# Goal Plan: 前端领域按后端模块对齐

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

前端 domain/web-domain 可由后端模块和 Controller 资源确定定位，旧语义包完全删除，双 App 产品行为保持不变。

### Success and False Completion

成功要求 canonical 包、资源入口、App 组合、架构检查、全门禁和双 App E2E 同时通过。只完成目录移动、只通过 typecheck 或保留旧 facade 均属于伪完成。

### Non-goals

不修改后端、不激活移动终端、不重做 UI、不推送或部署。

### Authoritative Inputs

用户最新批准的 Plan > 当前 Spec > T-01 > 当前代码事实；事实冲突触发偏差，不静默缩小合同。

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 source worktree -> source commit -> G1 parent-candidate -> plus-ui main result
```

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | 用户批准、main=8a5cd0d | T-01 writable paths | T-01 | G1/1 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 模块/资源可定位且双 App 不回归 | — | `specdev-worktree/2026-08-27-plus-ui-backend-aligned-domains/T-01` | codex:/root | required | `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/evidence/T-01.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

T-01 source clean且已提交；两轴审查通过；G1 全部命令与 Playwright 通过；前端 main 精确推进到 result SHA；Evidence 和状态一致。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G1 | source commit 与两轴 review 通过 | frozen install、architecture、OpenAPI、lint、type、test、双 build、E2E | main 推进 | codex:/root | main 不动，回 source 修复后重建 candidate |

### Contract and Reference Coverage

AC-001 至 AC-006 全由 T-01 和 G1 覆盖，Evidence 写入 `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/evidence/T-01.md</Path>`。

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | codex:/root | 唯一 SpecDev、Evidence 与 main owner |
| Implementation subagents | 1，Lead 不计入；本次不派遣 | 单 Ticket 单写者 |
| Integration attempts | 7 | config 快照 |
| Read-only agents | 不使用 | 用户未要求本次并行代理 |
| Dispatch | execution-time dynamic；本 Ticket 由 Lead 直接实现 | 原子迁移共享路径高度集中，当前无需派遣 |

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | plus-ui main/8a5cd0d | required source worktree / `speculo/2026-08-27-plus-ui-backend-aligned-domains/T-01` | 全部非 E2E；source worktree 不运行 E2E | required | parent-candidate 全门禁 + E2E | 通过后记录 |

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Ticket worktree local changes | allowed | T-01 writable paths |
| Implementation commit | allowed | 用户“执行”及既有明确提交授权 |
| Local candidate integration and parent update | allowed | Lead-only，G1 全绿 |
| Push / PR / remote merge | not-authorized | 本计划不包含 |
| Branch/worktree cleanup | not-authorized | 成功后保留 |
| Deploy / migration / production actions | not-authorized | 本计划不包含 |

### Evidence Return

Lead 独立核对 commit、diff、commands、candidate、E2E 与父分支 result，并写 Evidence。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

无旧包 facade；不改后端合同；不碰主工作区已有 `components.d.ts` 修改；动态 imports、Client/session/permission 语义不变。

### Verification Integrity

禁止通过删测、放宽断言、跳过 App、关闭架构规则或只跑单包制造绿色。

### Migration or Release Sequence

source 完整迁移和收缩 -> 两轴审查 -> 最新 main candidate -> 全门禁/E2E -> fast-forward main。无远端或生产动作。

### Risks, Monitoring and Recovery

候选失败时 main 保持 8a5cd0d；修复只进入 source commit，重新从最新 main 构造 candidate。推进后可按单一 result commit 回退。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。

## 6. Progress and Decisions

### Current Status

W1/T-01 与 G1 已完成；source/candidate/result 为 `d60b807a195336e4430e92d830eef7b707df7f6f`，前端 `main` 已 fast-forward，Playwright `47/47`。

### Pending Decisions and Blockers

无。

### Resume Protocol

读取本 Goal Plan、T-01、change 状态、source worktree Git 状态和最新 Evidence，从最后固定 checkpoint 继续。

## Assumptions

后端模块标准名去除 `ruoyi-` 前缀；资源名按 HTTP base path 转 kebab-case。
