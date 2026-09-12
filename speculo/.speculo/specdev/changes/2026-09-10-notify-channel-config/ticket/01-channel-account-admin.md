---
schema_version: 3
artifact: ticket
change: 2026-09-10-notify-channel-config
id: T-01
title: 交付通知配置入口与可启停的邮件/短信渠道账号
status: ready
planning_depth: deep
planning_depth_reason: 新增配置表与管理 API、密钥不回显、YAML 账号权威退出，并改变 MAIL/SMS 运行时数据源。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-001, AC-002, AC-003, AC-014, AC-017]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/controller/admin/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/src/index.ts</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-prod.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>release-artifacts/tests/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/oss-config/OssConfigPage.vue</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssConfigController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 交付通知配置入口与可启停的邮件/短信渠道账号

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-channel-account-admin.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent **必须**按顺序：Goal Plan 第 0 节 → Tickets Map 技能矩阵 → 下列 SKILL.md 全文 → 本 Ticket「必须加载的 Skill」→ 本 Ticket 其余章节。未读完不得改代码。Dispatch Packet 必须列出这些 Skill 路径。

**本 Ticket 必须调用：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。

## 1. 战略与来源

- **目标：** 让管理员在通知中心配置、启用和停用邮件 SMTP 与短信厂商账号，并使运行时不再从 YAML 读取这些账号。
- **可观察产出：** 有权限者看到「通知配置」双 TAB 账号列表，可新增/编辑/启停；详情不回显 secret；YAML 无发件人与 `sms.blends`；MAIL/SMS 在尚未绑定场景前失败关闭且不读 YAML。
- **来源：** `US-001`、`US-010`、`AC-001`、`AC-002`、`AC-003`、`AC-014`、`AC-017`、`ADR-002`、`ADR-003`、`ADR-004`、`ADR-007`、`DEC-014`。
- **当前事实：** 邮件来自 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/config/MailConfig.java</Path>` 的 YAML `MailAccount`；短信来自 YAML `sms.blends`。通知中心无配置菜单。
- **Planning Depth 原因：** 公共管理 API、密钥、YAML 迁移和发送数据源切换。

## 2. 决策状态

### 已锁定决策

- 控制面在 `ruoyi-notify` layered；common-mail/sms 只接收已解析账号。
- 账号独立启停，可同时启用多个；无隐式默认账号。
- secret 库内明文、VO 不回显、空白保持原值、POST `@Log` 且 `isSaveRequestData=false`。
- 查询 GET、变更 POST；权限 `notify:config:list|query|add|edit|remove`；HTTP 前缀 `/notify/config`。
- YAML 删除 mail 账号字段与 `sms.blends`；不双读。

### 已采用的低影响假设

- 账号表用 `channel + config_key` 唯一；邮件存 host/port/from/user/pass/ssl；短信存 supplier 与 SMS4J 所需凭据字段。
- 账号含每分钟吞吐列，本 Ticket 只持久化，不强制执行限额（T-04）。
- 前端页面 `notify/config/index`，图标 `tabler:settings`。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。
- **目录：** notify 保持 layered；Controller 只注入 UseCase。
- **停止条件：** 把配置放进 system；common-notify 建账号表；YAML 双读；回显 secret；MAIL/SMS 在无账号时回退 YAML。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 账号表、菜单权限、配置页账号 TAB、账号 API、mail/sms 按账号发送 SPI、YAML 账号段删除、无绑定则 MAIL/SMS 失败关闭 | 现有 notify 菜单、OSS 配置交互、NotifyClient、Outbox | 场景文案、短信模板码、限额执行、测试发送、调用方清正文 |

## 4. 要构建什么

管理员打开通知中心「通知配置」，在邮件 TAB 维护 SMTP 账号，在短信 TAB 维护厂商账号。启用后账号可供后续绑定；停用后不得再被新的发送选中。密钥只写不读。本切片完成后，任何 MAIL/SMS 若还没有后续 Ticket 的场景绑定，投递失败关闭，不再使用 YAML 账号把验证码“碰巧发出去”。

## 5. 实现契约

- **入口或接缝：** `/notify/config` 账号 API；Admin `notify/config/index`；Dispatch MAIL/SMS 路径。
- **输入与输出：** 账号 BO/VO；VO 无 pass/secret；启停 POST。
- **公共接口变化：** 新增配置 API 与菜单；mail/sms SPI 改为按账号发送。
- **不变量：** 同时可有多个 ENABLED；secret 不回显；YAML 不是账号源。
- **状态或数据流：** 保存账号 → DB → 运行时读取；发送前若无法解析账号则失败。
- **错误与失败行为：** 无权限 403；被绑定时删除拒绝（若绑定表尚未存在则仅本 Ticket 内未引用即可删）；MAIL/SMS 无账号 → delivery 失败。
- **兼容要求：** 无 YAML 回退窗口。
- **安全与隐私要求：** 密钥不进日志、响应、异常。

## 6. 执行路线

1. 先写失败测试：YAML 无账号时 MAIL/SMS 不得再走占位 SMTP/blend。
2. 增加账号表与菜单 DML，实现 layered 账号 CRUD/启停。
3. 改造 mail/sms 运行时按账号装配发送，删除 YAML 账号段。
4. Dispatch 在无法解析账号时失败关闭。
5. 前端双 TAB 账号页，权限与 manifest 对齐。
6. 运行模块测试、权限/SQL 基线与适用前端检查。

## 7. 路径访问契约

- **预计修改点：** notify 配置控制面、mail/sms SPI、YAML、50/60 SQL、notify 前端。
- **可写范围：** 见 frontmatter。
- **只读上下文：** OSS 配置页与 common-notify 模型。
- **共享路径：** 无。后续 Ticket 串行修改同一配置页与 Dispatch。
- **保留或不动：** IN_APP 投递、公告/收件箱页面。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常：新增并启用账号 | 配置 API | 模块测试 | 列表可见、secret 缺失 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 失败：无权限/空白外回显 | API | 403；详情无密钥 | 同上 |
| 失败：YAML 已删仍发送 | Dispatch 测试 | MAIL/SMS 失败关闭 | 同上 |
| 回归：IN_APP 公告 | 既有 notify 测试 | 站内信不受影响 | 同上 |

- **Workspace checks：** `./mvnw -pl ruoyi-modules/ruoyi-notify -am test`；`node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify --mode layered`；`pnpm --dir plus-ui-namewta typecheck`（若改前端）。
- **E2E disposition：** required：管理员打开通知配置，邮件/短信 TAB 可见账号列表，无权限失败关闭。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit + direct-parent + Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先 DDL/DML 与 API，再删 YAML 账号；删除 YAML 前测试必须证明发送不再读 YAML。
- **兼容窗口：** 无双读。本切片与 T-02 应连续交付，避免生产 MAIL/SMS 长期无账号可用。
- **监控信号：** MAIL/SMS 失败率；配置 API `@Log`。
- **回滚或前向恢复：** 回滚代码/SQL 可恢复 YAML 账号；前向靠管理员重填账号。
- **不可逆操作与批准点：** 删除 YAML 密钥是有意收缩；实现前确认种子不含真实生产密钥。
- **收缩条件：** 代码与配置中不再出现 `sms.blends` 账号与 `mail.from` 运行时绑定。

## 10. 验收标准

- [ ] `AC-001`：通知配置菜单与双 TAB 对有权限可见。
- [ ] `AC-002`：可新增启用账号且 secret 不在 JSON 中。
- [ ] `AC-003`：停用后该账号不能再被发送选中（后续绑定 Ticket 继续强化）。
- [ ] `AC-014`：GET 不回显 secret，空白保持原值。
- [ ] `AC-017`：POST + `@Log` 且不保存密钥请求体。
- [ ] 已读取 Map 与适用 Skill。
- [ ] 验证矩阵记录到 Evidence。
- [ ] 未超出 writable_paths。
- [ ] 形成 implementation commit 与父分支 result。
- [ ] E2E required 由 Lead 在 current-workspace 完成。
- [ ] 无未批准偏差。
