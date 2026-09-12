---
schema_version: 3
artifact: ticket
change: 2026-09-10-notify-channel-config
id: T-02
title: 交付邮件场景绑定、文案热配与按绑定发送
status: ready
planning_depth: deep
planning_depth_reason: 新增场景绑定数据、发送内容权威从调用方快照改为模板渲染，并锁定变量占用位置。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-005, AC-006]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>release-artifacts/tests/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/notify/api/NotificationCommand.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/model/NotifyTemplateContent.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 交付邮件场景绑定、文案热配与按绑定发送

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-mail-scene-binding.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent **必须**按顺序：Goal Plan 第 0 节 → Tickets Map 技能矩阵 → 下列 SKILL.md 全文 → 本 Ticket「必须加载的 Skill」→ 本 Ticket 其余章节。未读完不得改代码。Dispatch Packet 必须列出这些 Skill 路径。

**本 Ticket 必须调用：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`。

## 1. 战略与来源

- **目标：** 播种逻辑场景，让管理员把邮件场景绑到唯一 SMTP 账号并热配主题/正文；发送使用渲染结果和该账号发件人。
- **可观察产出：** 五个场景可见；邮件绑定启用账号后，MAIL 投递使用模板渲染而不是调用方 title/content；缺绑定或账号停用则失败；必填 `${name}` token 不能删改名。
- **来源：** `US-002`、`US-008`、`AC-005`、`AC-006`、`ADR-005`、`ADR-006`、`ADR-010`、`ADR-013`、`ADR-016`。
- **当前事实：** `DispatchNotificationService` 使用 `NotifyTextContent` 快照；场景未入库。
- **Planning Depth 原因：** 发送内容权威与变量契约是共享核心路径。

## 2. 决策状态

### 已锁定决策

- 场景代码播种：`auth-captcha`、`person-rebind`、`enterprise-transfer`、`workflow-task`、`notice-published`。
- 每个 scene+MAIL 绑定唯一账号；调用方不传 providerKey。
- 邮件正文热配，变量只读 token，保存校验必填变量出现。
- `notice-published` / `workflow-task` 为包装模板，必须保留已声明 `${title}` `${content}` `${path}`。
- 绑定缺失或账号停用 → 该渠道失败关闭。

### 已采用的低影响假设

- 绑定表同时预留 SMS 列（模板码、映射、限额），本 Ticket 不实现 SMS 发送（T-03）。
- 变量 schema 由后端播种接口只读返回，不提供管理端 CRUD。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`。
- **停止条件：** 调用方 title/content 仍当 MAIL 权威；允许管理端新建场景或改变量名；无绑定回退任意启用账号。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 场景/绑定表与种子、邮件绑定 API 与页面、MAIL 渲染发送、变量 token 校验 | T-01 账号、NotifyClient、NotificationCommand | SMS 供应商模板发送、限额执行、调用方删硬编码、测试发送 |

## 4. 要构建什么

管理员在邮件 TAB 看到播种场景，为 `auth-captcha` 等绑定一个启用中的 SMTP 账号，编辑带 `${code}` 的主题正文并保存。之后 MAIL 投递使用该文案和发件人。未绑定或账号停用时 MAIL 失败，不改选其他账号。包装场景不能去掉 `${title}` `${content}`。

## 5. 实现契约

- **入口或接缝：** `/notify/config` 场景/绑定 API；Dispatch MAIL。
- **输入与输出：** 场景只读列表；绑定保存；发送渲染快照。
- **公共接口变化：** 配置 API 扩展；`NotificationCommand` 不改字段。
- **不变量：** 一场景一 MAIL 账号；token 名称锁定；发送不读调用方句子。
- **状态或数据流：** submit → 解析 MAIL 绑定 → 渲染 → 按账号发送。
- **错误与失败行为：** 缺绑定/停用/缺必填变量 → delivery 失败。
- **兼容要求：** IN_APP 仍用原快照。
- **安全与隐私要求：** 验证码变量可进渲染，监控保持 REDACT 合同。

## 6. 执行路线

1. 失败测试：无 MAIL 绑定的 submit 不得发出 YAML/任意账号邮件。
2. 播种场景与绑定表，提供只读变量清单。
3. 实现 MAIL 绑定保存与 token 校验。
4. Dispatch MAIL 改为绑定渲染 + 账号发送。
5. 配置页邮件场景编辑器。
6. 定向测试与 notify 回归。

## 7. 路径访问契约

- **可写范围：** 见 frontmatter。在 T-01 已有配置页上增加邮件场景区。
- **只读上下文：** NotificationCommand、NotifyTemplateContent。
- **保留或不动：** 短信实际发送（T-03）；调用方 Java 句子（T-05）。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常：绑定后 MAIL 用模板 | Dispatch | 发出渲染正文与绑定 from | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 失败：无绑定/停用 | Dispatch | 失败关闭 | 同上 |
| 失败：删除必填 token | 配置 API | 拒绝保存 | 同上 |
| 回归：IN_APP | 既有测试 | 不受 MAIL 模板影响 | 同上 |

- **Workspace checks：** notify 模块测试、layered 校验、前端 typecheck。
- **E2E disposition：** required：管理员绑定邮件场景并保存带 token 的正文；去掉必填 token 无法保存。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit + direct-parent + Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先种子场景行，再切换 Dispatch MAIL 权威。
- **兼容窗口：** MAIL 在未绑定前保持 T-01 失败关闭。
- **监控信号：** MAIL delivery 失败码可区分未绑定。
- **回滚或前向恢复：** 回滚 Dispatch 会恢复快照正文，违反本期合同，应前向修绑定。
- **不可逆操作与批准点：** 无数据删除。
- **收缩条件：** MAIL 发送路径不再使用调用方 title/content 作为正文权威。

## 10. 验收标准

- [ ] `AC-005`：未绑定或账号停用时 MAIL 失败关闭。
- [ ] `AC-006`：可移动 token，不能改名或删除必填变量。
- [ ] 已读取 Map 与适用 Skill。
- [ ] Evidence 完整。
- [ ] 未超出 writable_paths。
- [ ] implementation commit 与父分支 result。
- [ ] E2E required 由 Lead 完成。
- [ ] 无未批准偏差。
