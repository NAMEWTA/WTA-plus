---
schema_version: 6
artifact: goal-plan
change: 2026-09-12-richtext-list-automapper
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

# Goal Plan: 富文本 list SummaryVo AutoMapper 修复

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/evidence/</Path>`
- **上游诊断：** `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnosis.md</Path>`

## 0. Mandatory Project Skill Gate

本计划与 T-01 的执行前置条件：未按顺序读完项目 Skill，不得改产品代码、不得宣称 Ticket 开始。

**固定读取顺序（每个 Ticket、每次派单）：**

1. 本 Goal Plan
2. `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/tickets-map.md</Path>` 中「项目 Skill 读取矩阵」
3. 矩阵里适用于 `ALL` 或当前 Ticket ID 的每一个 `<Path>.agents/skills/**/SKILL.md</Path>` **全文**
4. 当前 Ticket 的 skill_bindings / references 与正文
5. 相关上游 Spec/diagnosis 与只读代码事实

矩阵是最低必读集合，不是 allowlist。实现中新命中的项目 Skill 必须先由 Lead 写入 Map 并重新校验，再继续。

| Ticket | 必须调用的项目 Skill（SKILL.md） | 进入 Ticket 前额外 references |
|---|---|---|
| ALL / T-01 | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | `java/core`、`java/crud-query-and-common`（落 AutoMapper 前）；`rules/testing.md`（设计/跑定向回归前） |

Dispatch Packet（若派 implementation subagent）必须把上述 Skill 路径写入 packet，并写明「Map → Skill → Ticket」。未读 Skill、跳过 references、或用「沿用现有写法」代替硬约束时，Lead 停止该 Ticket。

## 1. Outcome and Authority

### Outcome

修复 `TestRichTextSummaryVo` 缺少 `@AutoMapper(target = TestRichText.class)` 导致非空 `GET /demo/rich-text/list` / `selectVoPage` 抛 `ConvertException`（应用码 500、错误编号 `85384381`）的问题。可选但建议同步为 `TestRichTextVo` 补 `@AutoMapper` 与 `contentHtml→html` 的 `@AutoMapping`。交付模块级 list 非空回归（修复前红 / 修复后绿）。不改归档、不改全局 MapStruct/MyBatis 基类、不扩 OSS/三方。

### Success and False Completion

成功必须同时满足：

- `AC-001`–`AC-004` 均有 Lead 可复查 Evidence；
- T-01 有非空 implementation commit、current-workspace 非 E2E 定向回归，以及 Lead-owned **Local direct-parent verification and parent update**；
- SummaryVo 含 `@AutoMapper(target = TestRichText.class)`；非空 list 不再 ConvertException；
- Lead 在 current workspace 完成 **required** E2E：`save→list` 点击/页面验收与诊断症状对齐（create 成功后 list 应用码成功、页面可见摘要）；
- diff 不触 `BaseMapperPlus` / `MapstructUtils`、不扩 OSS/三方、不 reopen 归档 `2026-09-08-richtext-third-oss`。

以下不算完成：只改源码文本未证明 converter/非空转换绿；仅空列表成功；用删测/放宽断言假绿；把 create/get 的手工 `view()` 未经验收改走 convert；或扩 scope 到全局工具/归档/前端登录模型。

### Non-goals

- 不改 `BaseMapperPlus` / `MapstructUtils` 全局行为。
- 不扩 OSS CORS / 三方 observability；不 reopen 归档 change `2026-09-08-richtext-third-oss`。
- 不改前端页面与登录/Client 绑定模型（E2E 仅作验收观察，不改产品前端）。
- 不开启 Ticket worktree / candidate-merge（本计划固定 `ticket_workspace_policy: current` + `integration_gate: direct-parent`）。
- 不远程 push/PR/merge、不部署生产、不自动清理 worktree。
- **实现授权已落盘**（真源 `.status.json`：`implementation_commit` + `local_candidate_integration` = authorized，`granted_at=2026-09-12T00:46:28+08:00`）；`source_cleanup` 仍 not-authorized。本 Plan 升 ready 后由 Lead 调度 I；规划岗不自启实现、不改写授权字段。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定（含 Lead：workspace=current、E2E required、正式 Goal Plan） | 产品取舍、workspace、E2E、实现授权 | 更新真正拥有该决策的工件；授权只写 `.status.json` 的 `execution_authorization`，本 Plan 不得私自改写 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/ADR.md</Path>` 与 CONTEXT（若存在） | 当前 change 架构决定 | 返回 G-grill；本 change 无独立 ADR 时以 Spec/diagnosis 为准 |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业永久知识 | 本 change 不引入永久 ADR 变更 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/spec.md</Path>` | 外部行为与 AC | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/ticket/</Path>` | 单 Ticket 契约 | Goal Plan 只编排；E2E disposition 以本 Goal Plan 为准（Lead 升为 required） |
| 6 | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/diagnosis.md</Path>` 与当前代码/运行事实 | 已验证根因与红灯基线 | 冲突时触发偏差并返回真正 owner |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 [READY]  SummaryVo AutoMapper + 可选 Vo/AutoMapping + list 非空回归
```

关键路径即单票 `T-01`。无真实阻塞边、无写路径并行扇出。`current` 模式下 Wave 仅作依赖投影，不得当作并发写授权——同一时间只有一个 implementation writer。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | Skill 门禁通过；实现提交与 direct-parent 已授权 | `TestRichTextSummaryVo` / 可选 `TestRichTextVo`；`ruoyi-demo` 测试目录下 list 非空回归 | 无 shared_paths；语义资源 `demo.richtext.summary-vo.automapper` / `demo.richtext.list.nonempty-convert-regression` 归 T-01 | G1 |

与并行 active change `2026-09-10-notify-channel-config`：写集不相交（本 change 限 demo 富文本 VO/测试）。Lead 禁止两 change 同时争用同一可变测试环境以外的无关写路径冲突。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 非空富文本 list 不再 ConvertException；SummaryVo 有 AutoMapper；回归前红后绿 | — | `current` | Lead / dynamic dispatch | **required**：save→list 点击/页面验收与诊断症状对齐（create 后 list 成功、页面可见摘要）；由 Lead 在 current workspace 跑；模块测仍为非 E2E 主接缝 | `<Path>{roots.state}/specdev/changes/2026-09-12-richtext-list-automapper/evidence/T-01.md</Path>` |

说明：Ticket 正文曾写 E2E not-required。Lead 本 Goal 明确建议并锁定为 **required**（对齐诊断 live E2E：POST create → GET list）。模块级 `TestRichTextListAutoMapperTest`（或等价）仍为 AC-002 主回归接缝；E2E 为 Lead Gate 关闭证据，不派给 implementation subagent。

## 3. Gates and Completion Evidence

### Overall Definition of Done

T-01 Done；AC-001–AC-004 有通过 Evidence；非空 list 转换与 SummaryVo 注解已证；Lead 完成 required save→list E2E；有 implementation commit 与 direct-parent result；无未集成 dirty 实现；change 状态/Map/Goal Plan/Evidence/Git 一致。票 Done 不等于 Goal 自动完成——须 Lead 关闭 G1 并做整体验收。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 Skill | T-01 开始前 | Map+Skill+Ticket 已读记录在 Evidence / Dispatch Packet | 该 Ticket | Lead | 未读则不准写代码 |
| G1 注解+回归绿 | T-01 commit + direct-parent；实现授权已获批 | SummaryVo `@AutoMapper`；非空 list 回归前红后绿；AC-003/AC-004 守门；**Lead required E2E** save→list 点击/页面验收通过 | change 完成 | Lead | 父分支不推进；保留 Ticket 继续修；E2E 失败记失败命令与恢复条件 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001 | T-01 | SummaryVo 源码 + 编译/转换 | T-01 Evidence | planned |
| AC-002 | T-01 | list / selectVoPage 非空回归 + Lead E2E save→list | T-01 Evidence | planned |
| AC-003 | T-01 | TestRichTextVo 注解审查（可选） | T-01 Evidence | planned |
| AC-004 | T-01 | diff/范围守门 | T-01 Evidence | planned |
| DEC-001–003 / diagnosis §6 | T-01 | 同模块 AutoMapper 模式 | T-01 Evidence | planned |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00` | 唯一 SpecDev 状态、Evidence、父分支与 E2E owner；Lead 不计入 implementation subagent |
| Implementation subagents | 上限 3，Lead 不计入；**current 同时只允许 1 个 writer** | config `max_implementation_agents=3` 快照；本计划不提高 |
| Integration attempts | 3 | config `max_integration_attempts=3` 快照 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写状态、不争用可变测试环境 |
| Dispatch | execution-time dynamic | provider/模型/派单按 Ticket 事实选择；packet 必须含项目 Skill 列表与 writable/read-only 边界 |

subagent-delivery `operation=plan` 合同：允许 task_kind = implementation | review | research | test-observation；implementation 在 current 模式持有串行 writer 锁；**E2E Gate 永远由 Lead 拥有**（本票 required，在 Lead-owned current workspace 执行）；subagent 不写 Ticket/Map/Goal Plan/Evidence/change status/父分支。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | 工作区基线 `5cf1bbef1d1d8830bc1c4d2bb0954917ee331d74`（`main`）；实现前 Lead 确认父分支与脏工作区隔离 | current / parent branch | `mvn -pl ruoyi-modules/ruoyi-demo -am -Dtest=TestRichTextListAutoMapperTest test`（或等价）+ 注解静态审查 + 范围 diff | 每 Ticket 必需（获授权后） | Lead **Local direct-parent verification and parent update**；**E2E required**：Lead 在 current workspace 跑 save→list 点击/页面验收 | result_sha = implementation commit |

当 `ticket_workspace_policy: current` 时，Ticket 必须严格串行。Lead 每次只允许一个 implementation owner 写入当前 workspace；完成非 E2E 检查并形成 commit 后，Lead 在同一父分支/current workspace 运行适用集成检查和 E2E，验证通过后将该 Ticket 的 `result_sha` 记录为其 implementation commit，再开始下一个 Ticket。不得创建 source/candidate worktree。

**Worktree 策略选择记录：** 创建本 Goal Plan 时固定 **不开启** Ticket worktree（`current` + `direct-parent`）。理由：单票线性、无并行写扇出、Lead 硬约束 current；独立 worktree 无收益。Wave 仅投影依赖，不授权并发写。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | authorized | 与 `implementation_commit=authorized` 对齐；限 Ticket writable_paths；由 Lead 派 I 后写入 |
| Ticket worktree local changes | not-authorized | 本计划不开启 worktree |
| Implementation commit | authorized | 真源：CTO via Lead，`granted_at=2026-09-12T00:46:28+08:00`；本地 Ticket commits（限 writable_paths；不含 push/PR） |
| Local direct-parent verification and parent update | authorized | 真源同 `local_candidate_integration=authorized`；current 模式下 Lead 核对 Ticket commit 后推进 |
| Local candidate integration and parent update | not-authorized | 本计划不使用 candidate-merge（字段授权供 direct-parent 语义） |
| Push / PR / remote merge | not-authorized | 不从本计划本地授权继承 |
| Branch/worktree cleanup | not-authorized | `source_cleanup` 仍 not-authorized；成功集成不自动继承 |
| Deploy / migration / production actions | not-authorized | 无 DB 迁移；禁止生产部署作为本 Plan 副作用 |

说明：授权真源仅为 `.status.json` 的 `execution_authorization`。本 Matrix 是投影；禁止擅自改写授权字段。CTO/Lead 已要求立刻执行 I——调度由 Lead/实现岗负责，规划岗不自启。

### Evidence Return

subagent 只返回候选事实与 commit；Lead 独立核对并写 Evidence、状态和最终验收。E2E Gate 只属于 Lead。source worktree 不运行 E2E（本计划无 source worktree）。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 仅改 demo 富文本 SummaryVo（及可选 Vo）注解与 demo 测试目录；对照只读同模块 Vo、ServiceImpl、归档。
- 主修 SummaryVo `@AutoMapper(target = TestRichText.class)`；若改 Vo 则强制 `contentHtml→html` AutoMapping。
- 不改 `BaseMapperPlus`/`MapstructUtils`；不扩 OSS/三方；不 reopen 归档。
- 必须按第 0 节调用 `engineering-standards`；跳过即停止。
- 路径引用遵守 path-reference-contract：权威工件禁止内部相对链接、裸文件名、机器绝对路径。
- 实现授权已获批；本 Plan 不自启实现，由 Lead 调度 I。

### Verification Integrity

判卷接缝为 Ticket 验证矩阵与 Spec AC，外加 Lead required E2E（save→list）。禁止删测试、放宽断言、仅断言空列表成功、或以静态改字充当 converter 已生成。current-workspace 跑非 E2E；Lead 在同一 workspace 跑集成与 E2E。诊断 `red_command` / diagnostics 作前红基线证据。

### Migration or Release Sequence

无数据/表结构迁移。需重新编译以生成 mapstruct-plus converter。建议：注解+回归同票交付；单独发注解无回归不得宣称 Done。

### Risks, Monitoring and Recovery

- 注解依赖编译期 mapstruct-plus；只改源码未编译可能仍缺 converter → 验证须覆盖编译/转换。
- 误改 Vo 字段映射导致详情 html 丢失 → 若补 AutoMapper 必须带 AutoMapping；保持 `view()` 不动除非验收要求。
- 工作区：`main` @ `5cf1bbef1d1d8830bc1c4d2bb0954917ee331d74`；另有无关 dirty（AGENTS.md、子模块等）——实现前提交范围必须隔离本 change。
- 并行 `2026-09-10-notify-channel-config`：写集不相交，仍避免争用同一可变联调环境。
- 失败：父分支不推进；保留 Ticket；integration attempts 满 3 次后 Lead 复盘再派（须 Evidence 写明改变点）。
- 授权已获批并升 ready；I 由 Lead 调度。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。发现需改全局工具或 reopen 归档时停止并返回契约 owner，不得在本票静默扩 scope。

## 6. Progress and Decisions

### Current Status

- Wave/Gate：W1 / G1（注解+回归绿 + Lead E2E）已写入；`ready_for_execution: true` / `status: ready`（实现授权已落；CTO/Lead 要求立刻执行 I）。
- Git 基线：`main` @ `5cf1bbef1d1d8830bc1c4d2bb0954917ee331d74`。
- 实测：`TestRichTextSummaryVo` 仍无 `@AutoMapper`；对照 `TestDemoVo` 已有注解。
- 无 Ticket implementation SHA；Evidence 目录待填。
- P-goal-plan 编排完成；I 由 Lead 调度，规划岗不自启。

### Pending Decisions and Blockers

1. **已解除：** `implementation_commit` 与 `local_candidate_integration` 已 authorized（`2026-09-12T00:46:28+08:00`）。本 Plan 已升 `ready` / `ready_for_execution: true`。
2. Push/PR/部署/`source_cleanup` 仍未授权（预期）。
3. Ticket 正文 E2E 曾写 not-required：以本 Goal Plan **required**（save→list）为准。
4. 实现前确认：父分支与无关 dirty 工作区隔离。

### Resume Protocol

恢复时读取 Goal Plan、当前 Ticket、`.status.json`、tickets-map 和最新 Evidence；从最后通过的父分支 result 或待修正 implementation checkpoint 继续。下一（本）Ticket 开始前重做第 0 节 Skill 门禁。授权变更后须回读 `.status.json` 真源，再修订本 Plan 的 Authorization Matrix 与 `ready_for_execution`。

## Assumptions

- 低影响：demo 模块可能尚无 `src/test`；测试类名 `TestRichTextListAutoMapperTest`（或同等）与接缝选择由实现按 Ticket 假设落地，以 AC-002 验证。
- 低影响：Vo 的 AutoMapper 可选；未做须 Evidence 声明。
- 无高影响未决假设。存在高影响假设时 `ready_for_execution` 必须为 `false`。
