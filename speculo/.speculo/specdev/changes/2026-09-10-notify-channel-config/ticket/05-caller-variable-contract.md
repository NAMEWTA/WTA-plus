---
schema_version: 3
artifact: ticket
change: 2026-09-10-notify-channel-config
id: T-05
title: 清除 MAIL/SMS 硬编码正文并补齐验证码与包装模板调用
status: ready
planning_depth: standard
planning_depth_reason: 多模块调用方迁移，但不改公共 NotificationCommand 形状；行为已由上游模板路径锁定。
ready: true
risk: medium
blocked_by: [T-04]
contract_ids: [AC-011, AC-012, AC-013]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/service/PersonRebindNotificationService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwCommonServiceImpl.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/caller/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/FlwCommonServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/NotifyNoticePublisherService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/MailSendController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-demo/src/main/java/org/dromara/demo/controller/SmsController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/notify/api/NotificationCommand.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-05: 清除 MAIL/SMS 硬编码正文并补齐验证码与包装模板调用

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/05-caller-variable-contract.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent **必须**按顺序：Goal Plan 第 0 节 → Tickets Map 技能矩阵 → 下列 SKILL.md 全文 → 本 Ticket「必须加载的 Skill」→ 本 Ticket 其余章节。未读完不得改代码。Dispatch Packet 必须列出这些 Skill 路径。

**本 Ticket 必须调用：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`。

## 1. 战略与来源

- **目标：** 生产 MAIL/SMS 调用方只传场景、模板码和变量；验证码不再提交空 templateCode；公告/工作流只填包装变量。
- **可观察产出：** Captcha/换绑/企业转移/工作流/公告的 MAIL/SMS 命令不再带完整句子；验证码 templateCode 稳定非空；公告发出包装渲染而不是裸正文当权威。
- **来源：** `US-006`、`US-007`、`US-008`、`AC-011`、`AC-012`、`AC-013`、`ADR-011`、`ADR-013`、`ADR-016`。
- **当前事实：** Captcha `templateId = ""` 且正文写死；换绑 `SAFE_TEXT`；工作流/公告把 title/content 当发送正文。
- **Planning Depth 原因：** 多模块消费者迁移，公共命令形状不变。

## 2. 决策状态

### 已锁定决策

- 调用方只传变量；IN_APP 本期不改正文。
- 验证码 `templateCode` 与 `auth-captcha` 一致，params 含 `code` 等已声明变量。
- 公告/工作流传 `title`/`content`/`path` 作为变量值，不把它们当发送权威。
- 不改 `NotificationMode.SYNC` 真同步。
- demo MAIL/SMS 改为只传变量或停止纯文本直发。

### 已采用的低影响假设

- 工作流 IN_APP 仍可传 title/content 作为站内信正文，因为 IN_APP 不走邮件模板。
- 企业转移邮件/短信与个人换绑同样只传变量。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`。
- **停止条件：** 在 Java 里新写短信/邮件句子；修改 Dispatch；改 IN_APP 文案热配。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| Captcha、换绑、企业转移、工作流 MAIL/SMS、公告发布、demo 调用方 | NotificationApplicationService、T-02/T-03 模板路径 | 配置页、限额、测试发送、SYNC 改造 |

## 4. 要构建什么

验证码接口提交 `auth-captcha` 与 `code` 变量。换绑/企业转移删除 SAFE_TEXT 等硬编码。公告发布和工作流 MAIL/SMS 只提供包装变量。Demo 不再演示纯文本直发。站内信调用保持原样。

## 5. 实现契约

- **入口或接缝：** 各模块 `NotificationCommand` 构造点。
- **输入与输出：** templateCode 非空；params 仅为变量。
- **公共接口变化：** 无命令字段变化。
- **不变量：** MAIL/SMS 无硬编码句子；IN_APP 可保留正文。
- **错误与失败行为：** 空 templateCode 仍被控制面拒绝，因此 Captcha 必须补齐。
- **兼容要求：** 调用方包结构不变。
- **安全与隐私要求：** 验证码 code 仍走现有 REDACT 元数据。

## 6. 执行路线

1. 先扩展 Captcha 契约测试：禁止空 templateCode 和硬编码整句。
2. 改 Captcha、profile、workflow MAIL/SMS、notice publisher、demo。
3. 扫描 MAIL/SMS submit 点确认无残留句子。
4. 运行 caller 与 notify 回归。

## 7. 路径访问契约

- **可写范围：** 见 frontmatter。只读 Dispatch。
- **保留或不动：** 配置 API、限额、IN_APP 文案产品化。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| Captcha | CaptchaNotifyCallerUnitTest | 非空 templateCode，params 含 code | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 调用方无硬编码句 | 契约/静态 | MAIL/SMS submit 无整句 | 同上 |
| 包装模板 | 公告/工作流测试 | 只传变量 | 同上 |
| 回归 IN_APP | 登录欢迎/公告站内 | 仍可用代码正文 | 同上 |

- **Workspace checks：** 受影响模块测试、Captcha 测试。
- **E2E disposition：** not-required：调用方契约可由单元测试判定。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit + direct-parent + Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 必须在 T-04 完成之后（已含模板路径），否则只传变量会发出空内容。
- **兼容窗口：** 无。
- **监控信号：** 验证码失败中空 templateCode 应消失。
- **回滚或前向恢复：** 回滚调用方会恢复硬编码，禁止与热配并存。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 生产 MAIL/SMS 调用点扫描无硬编码句子。

## 10. 验收标准

- [ ] `AC-011`：生产 MAIL/SMS 调用方只传变量。
- [ ] `AC-012`：验证码稳定 templateCode。
- [ ] `AC-013`：公告/工作流走包装变量。
- [ ] 已读取 Map 与适用 Skill。
- [ ] Evidence 完整。
- [ ] 未超出 writable_paths。
- [ ] implementation commit 与父分支 result。
- [ ] E2E disposition 已执行。
- [ ] 无未批准偏差。
