---
schema_version: 3
artifact: ticket
change: 2026-09-10-notify-channel-config
id: T-03
title: 交付短信场景的供应商模板绑定与发送
status: ready
planning_depth: deep
planning_depth_reason: 改变 SMS 发送从纯文本快照到供应商模板码，并要求场景绑定唯一短信账号。
ready: true
risk: high
blocked_by: [T-02]
contract_ids: [AC-004, AC-007]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/src/main/java/org/dromara/common/sms/notify/Sms4jNotificationProviderResolver.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>release-artifacts/tests/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/model/NotifyTemplateContent.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/notify/api/NotificationCommand.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 交付短信场景的供应商模板绑定与发送

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/03-sms-template-binding.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent **必须**按顺序：Goal Plan 第 0 节 → Tickets Map 技能矩阵 → 下列 SKILL.md 全文 → 本 Ticket「必须加载的 Skill」→ 本 Ticket 其余章节。未读完不得改代码。Dispatch Packet 必须列出这些 Skill 路径。

**本 Ticket 必须调用：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。

## 1. 战略与来源

- **目标：** 管理员为短信场景绑定唯一厂商账号、供应商模板码和参数映射；发送走 `NotifyTemplateContent`。
- **可观察产出：** 两个启用短信账号时，场景只使用绑定账号 A 的模板码；无绑定/停用失败；没有自由短信编辑器。
- **来源：** `US-003`、`AC-004`、`AC-007`、`ADR-006`、`ADR-013`。
- **当前事实：** `Sms4jNotificationProviderResolver` 支持模板发送，但 Dispatch 从未构造 `NotifyTemplateContent`。
- **Planning Depth 原因：** 共享 Dispatch 与短信数据面合同。

## 2. 决策状态

### 已锁定决策

- 一场景一 SMS 账号；短信页只有模板码 + 映射表。
- 发送使用绑定账号的 providerKey 与 providerTemplateCode。
- 必填逻辑变量必须出现在映射表。
- 不传调用方 providerKey。

### 已采用的低影响假设

- 映射为逻辑变量名 → 厂商参数名或有序位；阿里云用名，腾讯云可用序号，由供应商字段决定。
- T-02 已预留 SMS 列则本 Ticket 填满行为与 UI，不再改表结构。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。
- **停止条件：** 自由短信正文编辑器；无绑定选用默认 blend；业务模块直连 SmsFactory。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| SMS 绑定 API/UI、Dispatch SMS 模板路径、按账号解析 SmsBlend | T-01 账号、T-02 场景表、NotifyTemplateContent | 限额执行、调用方清正文、测试发送按钮 |

## 4. 要构建什么

管理员在短信 TAB 为场景选择启用中的短信账号，填写该账号控制台里的模板码，并把 `code` 等逻辑变量映射到厂商参数。发送时只调用该账号，使用该模板码。另一个启用账号 B 不会被选中。

## 5. 实现契约

- **入口或接缝：** SMS 绑定保存；Dispatch SMS。
- **输入与输出：** 模板码、映射、账号 id；发送结果带解析后的 providerKey。
- **公共接口变化：** 配置 API SMS 绑定；不改 NotificationCommand。
- **不变量：** 不发纯文本 SMS；不改选账号。
- **错误与失败行为：** 缺映射/缺绑定/停用 → 失败关闭。
- **兼容要求：** MAIL 路径保持 T-02。
- **安全与隐私要求：** 模板码不是 secret，但 AK/SK 仍不回显。

## 6. 执行路线

1. 失败测试：SMS 不得再把 content 快照当纯文本发出。
2. 实现 SMS 绑定保存与映射校验。
3. Dispatch 构造 NotifyTemplateContent 并设置 providerKey。
4. 短信 TAB 绑定 UI，无正文编辑器。
5. 双账号路由测试与 MAIL 回归。

## 7. 路径访问契约

- **可写范围：** 见 frontmatter。在 T-02 之后修改 Dispatch。
- **只读上下文：** NotifyTemplateContent、NotificationCommand。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常：绑定账号 A | Dispatch | 使用 A 的模板码 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 失败：无绑定 | Dispatch | 失败、不调用 B | 同上 |
| 回归：MAIL 模板 | T-02 测试 | MAIL 行为不变 | 同上 |

- **Workspace checks：** notify 与 sms 相关测试、layered、typecheck。
- **E2E disposition：** required：短信 TAB 可保存模板码与映射，页面无自由正文框。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit + direct-parent + Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先绑定 API，再切换 Dispatch SMS。
- **兼容窗口：** SMS 在未绑定前失败关闭。
- **监控信号：** SMS delivery 应能看到模板码而非整段自由文本。
- **回滚或前向恢复：** 前向修绑定。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 生产 SMS 路径不再调用 `sendMessage(phone, text)` 纯文本接口。

## 10. 验收标准

- [ ] `AC-004`：只使用绑定账号，不使用其他启用账号。
- [ ] `AC-007`：发送走 NotifyTemplateContent。
- [ ] 已读取 Map 与适用 Skill。
- [ ] Evidence 完整。
- [ ] 未超出 writable_paths。
- [ ] implementation commit 与父分支 result。
- [ ] E2E required 由 Lead 完成。
- [ ] 无未批准偏差。
