---
schema_version: 6
artifact: goal-plan
change: 2026-09-10-notify-channel-config
status: ready
modes: [migration, high-assurance]
orchestration: lead-directed
lead: grok:local
implementation_agent_limit: 3
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: true
---

# Goal Plan: 通知中心邮件/短信配置管理

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
4. 当前 Ticket 的「必须加载的 Skill 与工程基线」及 Ticket 点名的 references
5. 当前 Ticket 其余章节

矩阵是最低必读集合，不是 allowlist。实现中新命中的项目 Skill 必须先由 Lead 写入 Map 并重新校验，再继续。

| Ticket | 必须调用的项目 Skill（SKILL.md） | 进入 Ticket 前额外 references |
|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`；`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`；`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>` | 工程规范：`<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>`、`<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>`、`<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>`；notify 事实：`<Path>.agents/skills/ruoyi-module-guide/references/modules/notify/index.md</Path>` |
| T-01 | 上述 ALL + `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | mail/sms 运行时入口；OSS 配置只作交互对照，不复制 system CRUD 到 notify |
| T-02 | ALL 三项 | 全栈合同映射、layered CRUD、权限路由 |
| T-03 | ALL + `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | SMS4J / NotifyTemplateContent；禁止业务直连 SmsFactory |
| T-04 | ALL + `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | RedisUtils 限额；安全与失败关闭 |
| T-05 | ALL 三项 | notify 调用入口；workflow/profile 只改公开 NotificationCommand 构造 |
| T-06 | ALL + `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | 更新 notification.md 与 notify index 事实；跑 `validate-skill-facts.mjs` |

Dispatch Packet（若派 implementation subagent）必须把上述 Skill 路径写入 packet，并写明「Map → Skill → Ticket」。未读 Skill、跳过 references、或用「沿用现有写法」代替硬约束时，Lead 停止该 Ticket。

## 1. Outcome and Authority

### Outcome

在通知中心交付「通知配置」运维面：管理员用邮件/短信 TAB 配置渠道账号、场景绑定、热配文案、三层限额并测试发送。运行时只认数据库。MAIL/SMS 按场景绑定唯一账号发送；调用方只传变量。YAML 不再保存发件人、供应商账号和全局号码拦截。

### Success and False Completion

成功必须同时满足：

- AC-001 至 AC-018 均有 Lead 可复查 Evidence；
- T-01 至 T-06 各有非空 implementation commit、current-workspace 检查和 Lead-owned **Local direct-parent verification and parent update**；
- YAML 无账号段与全局拦截后发送仍只读库；secret 不回显；无绑定/停用/超限失败关闭且不改选账号；
- 项目 Skill 事实与中文注释与代码一致。

以下不算完成：只有配置页没有发送路径、仍读 YAML 账号、调用方仍写死句子、SYNC 被顺便改成同步发送、IN_APP 被扩进本期 TAB、或跳过 Skill 读取。

### Non-goals

- 不开启 worktree / candidate-merge（本计划默认 `current`）；用户若改选需修订本 Goal Plan。
- 不复活 notify_app、KEK、故障切换、不可变模板版本全集。
- 不把 SYNC 改为请求线程内 Provider I/O。
- 不远程 push/PR/merge、不部署生产、不自动清理 worktree。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | 产品取舍、worktree 选择、实现提交授权 | 更新真正拥有该决策的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | 当前 change 架构与术语 | 返回 G-grill |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 永久 ADR-0006 薄 common-notify | 本 change 不得把模板中心放入 common-notify |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为与 AC | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 契约 | Goal Plan 只编排 |
| 6 | 当前代码与 Git `main` @ `1db8c0a1de1092d050da1c3791afcf6e1bd425d1` | 可执行事实 | 冲突则偏差控制 |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 账号+YAML账号退出
  → T-02 邮件绑定/热配
    → T-03 短信模板绑定
      → T-04 三层限额+YAML拦截退出
        → T-05 调用方只传变量
          → T-06 试发+Skill
```

关键路径即全链。current 模式不得把 Wave 当作并发授权。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | Skill 门禁通过；实现提交已授权 | notify 配置账号、mail/sms SPI、YAML 账号段、菜单、前端配置页 | T-01 创建配置页与 Dispatch 失败关闭骨架 | G1 |
| W2 | T-02 | T-01 result | 场景绑定、MAIL 渲染、配置页邮件区 | T-02 接过 Dispatch MAIL | G2 |
| W3 | T-03 | T-02 result | SMS 绑定、Dispatch SMS 模板 | T-03 接过 Dispatch SMS | G2 |
| W4 | T-04 | T-03 result | 限额、YAML 拦截段、Dispatch 发送前检查 | T-04 接过 Dispatch 限额 | G3 |
| W5 | T-05 | T-04 result | Captcha/profile/workflow/notice/demo 调用方 | 无共享 Dispatch | G4 |
| W6 | T-06 | T-05 result | 测试发送 API/按钮、Skill 与注释 | T-06 收口文档 | G5 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 双 TAB 账号 CRUD/启停，YAML 账号退出 | — | `current` | Lead / dynamic dispatch | required：配置页可见 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-02 | 邮件绑定与热配发送 | T-01 | `current` | Lead / dynamic dispatch | required：token 保存/拒绝 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| T-03 | 短信模板绑定发送 | T-02 | `current` | Lead / dynamic dispatch | required：无自由正文框 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| T-04 | 三层限额 | T-03 | `current` | Lead / dynamic dispatch | not-required：Dispatch 可判定 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| T-05 | 调用方只传变量 | T-04 | `current` | Lead / dynamic dispatch | not-required：caller 测试 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| T-06 | 试发 + Skill | T-05 | `current` | Lead / dynamic dispatch | required：试发入口权限 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

全部 Ticket Done；AC 全覆盖；YAML 账号与全局拦截已收缩；Skill 事实通过 `validate-skill-facts.mjs`；无未集成 dirty 实现。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 Skill | 任一 Ticket 开始前 | Map+Skill+Ticket 已读记录在 Evidence | 该 Ticket | Lead | 未读则不准写代码 |
| G1 账号权威 | T-01 commit | YAML 无 blends/from 运行时绑定；账号 API；secret 不回显；无绑定时 MAIL/SMS 失败关闭 | 不得单独发布 | Lead；Deep 需人工批准 | 父分支不推进 |
| G2 绑定发送 | T-02+T-03 | MAIL 模板渲染；SMS NotifyTemplateContent；双账号不误选 | T-04 限额 | Lead | 保留失败 Ticket |
| G3 限额收缩 | T-04 | 三层限额测试；YAML 无全局 minute-max | T-05 | Lead | 不删 YAML 直到测试绿 |
| G4 调用方 | T-05 | 无硬编码 MAIL/SMS 句子；验证码 templateCode 非空 | T-06 | Lead | 扫描残留则重开 T-05 |
| G5 发布收口 | T-06 | 试发 + Skill 校验 | change 完成 | Lead | Skill 失败不 Done |

W1 不得单独发布到生产。Deep Ticket（T-01–T-04）开始实现前需满足 config `deep_ticket_human_approval: true`。

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001–003,014,017 | T-01 | 配置 API/菜单 | T-01 Evidence | planned |
| AC-005,006 | T-02 | MAIL Dispatch/token | T-02 Evidence | planned |
| AC-004,007 | T-03 | SMS Dispatch | T-03 Evidence | planned |
| AC-008–010,015 | T-04 | 限额/YAML | T-04 Evidence | planned |
| AC-011–013 | T-05 | caller | T-05 Evidence | planned |
| AC-016,018 | T-06 | 试发/Skill | T-06 Evidence | planned |
| 永久 ADR-0006 | ALL | 依赖方向 | 各 Ticket | planned |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `grok:local` | 唯一 SpecDev 状态、Evidence 与父分支 owner |
| Implementation subagents | 上限 3，Lead 不计入；**current 同时只允许 1 个 writer** | config `max_implementation_agents=3` |
| Integration attempts | 3 | 本计划快照；config 现为 3 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写状态 |
| Dispatch | execution-time dynamic | provider/模型/派单按 Ticket 事实选择；packet 必须含项目 Skill 列表 |

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | `main` / `1db8c0a1de1092d050da1c3791afcf6e1bd425d1` 起，随后为上一 Ticket result | current / main | 模块测试+layered+typecheck | 每 Ticket 必需 | Lead direct-parent + 配置页 E2E | result_sha = implementation commit |
| T-02 | T-01 result | current / main | notify 测试+typecheck | 必需 | Lead + token E2E | 同上 |
| T-03 | T-02 result | current / main | notify/sms 测试 | 必需 | Lead + 短信 TAB E2E | 同上 |
| T-04 | T-03 result | current / main | 限额测试+YAML 断言 | 必需 | Lead；E2E not-required | 同上 |
| T-05 | T-04 result | current / main | caller 测试 | 必需 | Lead；E2E not-required | 同上 |
| T-06 | T-05 result | current / main | 试发测试+validate-skill-facts | 必需 | Lead + 试发入口 E2E | 同上 |

当 `ticket_workspace_policy: current` 时，Ticket 必须严格串行。Lead 每次只允许一个 implementation owner 写入当前 workspace；完成非 E2E 检查并形成 commit 后，Lead 在同一父分支/current workspace 运行适用集成检查和 E2E，验证通过后将该 Ticket 的 `result_sha` 记录为其 implementation commit，再开始下一个 Ticket。不得创建 source/candidate worktree。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | allowed | 仅 current 模式；严格串行，单一 implementation writer。USER-DECISION:2026-09-10 授权本地实现提交、不开启 worktree |
| Ticket worktree local changes | not-authorized | 用户确认不开启 worktree |
| Implementation commit | allowed | 每 Ticket 必需；范围限于该 Ticket writable_paths 的实现提交，不含 push/PR |
| Local direct-parent verification and parent update | allowed | 仅 current 模式；Lead 核对 Ticket commit 后继续 |
| Local candidate integration and parent update | not-authorized | 本计划不使用 candidate-merge |
| Push / PR / remote merge | not-authorized | 不从本计划本地授权继承 |
| Branch/worktree cleanup | not-authorized | 成功集成不自动继承 |
| Deploy / migration / production actions | not-authorized | 禁止生产 DDL/密钥写入 |

### Evidence Return

subagent 只返回候选事实与 commit；Lead 独立核对并写 Evidence、状态和最终验收。E2E Gate 只属于 Lead。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 必须按第 0 节调用项目 Skill；跳过即停止。
- layered notify；common-notify 不建模板中心。
- GET 查询、POST 变更、POST `@Log`；secret 不回显。
- 无隐式默认账号；超限/缺绑定失败关闭。
- 不单独发布 T-01。
- 不改 SYNC 真同步、不做 IN_APP TAB。

### Verification Integrity

判卷接缝为 Ticket 验证矩阵与 Spec AC。禁止删测试、放宽断言、YAML 双读、或用截图代替 API 契约。current-workspace 跑非 E2E；Lead 在同一 workspace 跑集成与 required E2E。source worktree 不运行 E2E（本计划无 source worktree）。

### Migration or Release Sequence

1. 账号表+API → 删 YAML 账号（T-01）
2. 场景绑定 MAIL → SMS（T-02/T-03）
3. 限额生效 → 删 YAML 全局拦截（T-04）
4. 调用方收缩（T-05）
5. 试发与 Skill（T-06）

### Risks, Monitoring and Recovery

- W1 后生产 MAIL/SMS 失败关闭，直到 G2。
- 限额从全局改按场景，种子默认错误会过宽/过严。
- 工作区当前 dirty：另有 `2026-09-08-richtext-third-oss` 状态文件与 validator 修改，实现前由 Lead 隔离本 change 的提交范围。
- 失败：父分支不推进；保留 Ticket workspace；integration attempts 满 3 次后 Lead 复盘再派。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。

## 6. Progress and Decisions

### Current Status

- Wave/Gate：可执行，尚未开始实现；下一动作 G0 Skill 后 T-01。
- Git 父分支 `main` @ `1db8c0a1de1092d050da1c3791afcf6e1bd425d1`。
- 工作区含本 change 未跟踪文件及其他 change 的 status 修改；实现提交须隔离本 change 范围。
- 无 Ticket implementation SHA。

### Pending Decisions and Blockers

无执行阻塞。USER-DECISION:2026-09-10 授权本地实现提交与 direct-parent 验证；明确不开启 worktree。该决定同时满足 Deep Ticket（T-01–T-04）开始实现的人工批准。Push/PR/部署仍未授权。

### Resume Protocol

恢复时读取 Goal Plan、当前 Ticket、`.status.json` 和最新 Evidence；从最后通过的父分支 result 或待修正 implementation checkpoint 继续。下一 Ticket 开始前重做第 0 节 Skill 门禁。

## Assumptions

- 低影响：菜单图标 `tabler:settings`；测试发送路径在 `/notify/config/test/*` 下前后端一致。
- 无高影响未决假设。存在高影响假设时 `ready_for_execution` 必须为 `false`。
