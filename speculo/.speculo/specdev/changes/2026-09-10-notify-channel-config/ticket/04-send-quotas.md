---
schema_version: 3
artifact: ticket
change: 2026-09-10-notify-channel-config
id: T-04
title: 交付账号/模板吞吐与按场景的收件人拦截
status: ready
planning_depth: deep
planning_depth_reason: 限额改变发送是否调用供应商，涉及 Redis 计数、YAML 全局拦截收缩和失败关闭语义。
ready: true
risk: high
blocked_by: [T-03]
contract_ids: [AC-008, AC-009, AC-010, AC-015]
owner: unassigned
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-prod.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>release-artifacts/tests/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 交付账号/模板吞吐与按场景的收件人拦截

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/04-send-quotas.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent **必须**按顺序：Goal Plan 第 0 节 → Tickets Map 技能矩阵 → 下列 SKILL.md 全文 → 本 Ticket「必须加载的 Skill」→ 本 Ticket 其余章节。未读完不得改代码。Dispatch Packet 必须列出这些 Skill 路径。

**本 Ticket 必须调用：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。

## 1. 战略与来源

- **目标：** 按渠道账号和场景绑定执行每分钟吞吐，并按场景配置收件人拦截；超限立即失败。
- **可观察产出：** 账号上限 1 时第二次发送失败；场景 minute-max=1 时同一号码第二次失败且其他场景不受影响；模板上限大于账号上限无法保存；YAML 不再有全局 restricted/minute-max/account-max。
- **来源：** `US-004`、`AC-008`、`AC-009`、`AC-010`、`AC-015`、`ADR-012`、`ADR-014`、`ADR-015`。
- **当前事实：** YAML 全局 `minute-max: 1` / `account-max: 30` 是单号拦截且所有场景共用。
- **Planning Depth 原因：** 安全/滥用防护与发送失败语义。

## 2. 决策状态

### 已锁定决策

- 三层同时生效：账号每分钟、模板每分钟（≤ 账号）、场景收件人分钟/日。
- SMS 按手机号，MAIL 按邮箱。
- 任一用尽立即失败关闭，不改选、不延期。
- YAML 删除这些全局拦截项。
- `auth-captcha` 默认 restricted=true、minute-max=1、account-max=30。

### 已采用的低影响假设

- 计数使用 `RedisUtils`；key 含 channel、账号、场景、收件人、时间窗。
- `restricted=false` 时跳过收件人层，仍检查吞吐。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。
- **停止条件：** 超限改选账号或写入 Outbox 延期；保留 YAML 全局拦截作为运行时权威。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 限额字段 UI、保存校验、发送前计数、YAML 拦截段删除、captcha 默认种子 | T-01 账号列、T-02/T-03 绑定、RedisUtils | 测试发送按钮、调用方清正文、延期队列 |

## 4. 要构建什么

管理员为账号和场景填写每分钟条数，并为每个 MAIL/SMS 绑定配置是否拦截、每收件人每分钟/每天上限。验证码场景默认沿用原来的 1/分钟、30/天。发送前检查三层；用尽则该渠道失败，监控可见原因。YAML 不再出现全局 minute-max。

## 5. 实现契约

- **入口或接缝：** 绑定/账号保存；Dispatch 发送前。
- **输入与输出：** 限额数字；超限失败码可观察。
- **不变量：** 模板上限 ≤ 账号上限；超限不调用供应商。
- **错误与失败行为：** 保存非法上限拒绝；发送超限失败关闭。
- **兼容要求：** 线程池等 SMS4J 进程项可留 YAML。
- **安全与隐私要求：** Redis key 可含脱敏后的收件人指纹，不得把密钥写入 key。

## 6. 执行路线

1. 失败测试：同号两秒内第二次 auth-captcha SMS 必须失败。
2. 完成限额 UI 与保存校验。
3. Dispatch 前计数，超限不调用 Adapter。
4. 删除 YAML 全局拦截。
5. 多种场景隔离测试与 MAIL/SMS 回归。

## 7. 路径访问契约

- **可写范围：** 见 frontmatter。
- **只读上下文：** RedisUtils 实现。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 账号吞吐 | Dispatch | 第二次失败 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| 收件人拦截隔离 | Dispatch | 场景 A 超限不影响场景 B | 同上 |
| 保存校验 | 配置 API | 模板上限 > 账号上限拒绝 | 同上 |
| YAML 收缩 | 配置加载 | 无全局 minute-max 仍只读库 | 同上 |

- **Workspace checks：** notify 测试、YAML 断言、typecheck。
- **E2E disposition：** not-required：限额是发送语义，API/Dispatch 测试可判定；UI 字段在页面回归中覆盖。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit + direct-parent + Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先写入场景默认限额，再删除 YAML 拦截，再启用 Dispatch 检查。
- **兼容窗口：** 无全局拦截并行权威。
- **监控信号：** 超限失败计数。
- **回滚或前向恢复：** 回滚会暂时失去按场景隔离。
- **不可逆操作与批准点：** 删除 YAML 拦截。
- **收缩条件：** 运行时不再读取 `sms.restricted` / `minute-max` / `account-max`。

## 10. 验收标准

- [ ] `AC-008`：账号每分钟上限生效。
- [ ] `AC-009`：场景收件人限额隔离。
- [ ] `AC-010`：非法模板上限拒绝保存。
- [ ] `AC-015`：YAML 无账号且无全局拦截时发送只读库。
- [ ] 已读取 Map 与适用 Skill。
- [ ] Evidence 完整。
- [ ] 未超出 writable_paths。
- [ ] implementation commit 与父分支 result。
- [ ] E2E disposition 已执行。
- [ ] 无未批准偏差。
