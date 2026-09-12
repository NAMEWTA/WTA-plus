---
schema_version: 3
artifact: ticket
change: 2026-09-10-notify-channel-config
id: T-06
title: 交付测试发送并同步 Skill 与中文注释
status: ready
planning_depth: standard
planning_depth_reason: 新增测试发送 API 与权限，并关闭文档/Skill 与实现的权威漂移。
ready: true
risk: medium
blocked_by: [T-04, T-05]
contract_ids: [AC-016, AC-018]
owner: unassigned
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
  - "<Path>.agents/skills/engineering-standards/references/notification.md</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/notify/**</Path>"
  - "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>.agents/skills/engineering-standards/**</Path>"
  - "<Path>.agents/skills/ruoyi-module-guide/**</Path>"
  - "<Path>.agents/skills/ruoyi-common-modules-guide/**</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-06: 交付测试发送并同步 Skill 与中文注释

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/06-test-send-and-skills.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent **必须**按顺序：Goal Plan 第 0 节 → Tickets Map 技能矩阵 → 下列 SKILL.md 全文 → 本 Ticket「必须加载的 Skill」→ 本 Ticket 其余章节。未读完不得改代码。Dispatch Packet 必须列出这些 Skill 路径。

**本 Ticket 必须调用：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。

## 1. 战略与来源

- **目标：** 管理员能对账号和模板试发；工程 Skill 与中文注释改为「通知配置 + 数据库权威」。
- **可观察产出：** 账号级/模板级测试发送走真实绑定与相同限额；权限 `notify:config:test`；Skill 不再把 YAML mail/sms 账号当运行时权威。
- **来源：** `US-005`、`AC-016`、`AC-018`、`ADR-008`、`DEC-016`。
- **当前事实：** 无测试发送；Skill 通知规范仍描述业务 submit，未描述渠道配置页。
- **Planning Depth 原因：** 新公共测试 API，但沿用已有发送与限额路径。

## 2. 决策状态

### 已锁定决策

- 账号级试发验证凭据；模板级用已声明样例变量走绑定。
- 测试发送占用与生产相同限额语义，不绕过停用/缺绑定。
- 独立权限 `notify:config:test`。
- 必须更新 notification/notify/common mail-sms 相关 Skill 事实与中文注释。

### 已采用的低影响假设

- 测试发送写入同一套 delivery/监控语义，可用 metadata 标记 TEST。
- 样例变量来自场景播种的 example 值，管理员不可发明新变量名。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`、`<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`、`<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`。
- **停止条件：** 测试发送绕过限额或停用；只改代码不改 Skill。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 测试发送 API/按钮、test 权限、Skill 与注释、必要中文 JavaDoc | T-01–T-05 发送与限额 | 新渠道、SYNC 改造、KEK |

## 4. 要构建什么

管理员在账号行测试发送到指定收件人，在场景绑定上用样例变量试发。失败原因与生产一致。完成后工程 Skill 说明：账号在通知配置，YAML 只可能保留 SMS4J 线程池。

## 5. 实现契约

- **入口或接缝：** POST `/notify/config/test/account` 与 `/notify/config/test/template`（路径可在前缀下微调，前后端必须一致）。
- **输入与输出：** 测试收件人、账号或场景+渠道；返回投递结果摘要，不含 secret。
- **不变量：** 与生产同一路由、模板、限额。
- **错误与失败行为：** 无权限、停用、缺绑定、超限均失败关闭。
- **安全与隐私要求：** `@Log` 不保存完整收件人明文以外的密钥；测试权限独立。

## 6. 执行路线

1. 测试：无 test 权限不能试发；停用账号试发失败。
2. 实现两条 POST 与菜单权限。
3. 配置页按钮。
4. 更新 Skill 与注释，跑 `validate-skill-facts.mjs`。
5. 回归发送与限额。

## 7. 路径访问契约

- **可写范围：** 见 frontmatter。只读 Dispatch 实现，经 UseCase 调用既有发送端口。
- **保留或不动：** 不改发送内核算法，只暴露测试入口。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常试发 | 测试发送 API | 走绑定且进监控语义 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| 失败：停用/超限/无权限 | API | 失败关闭 | 同上 |
| Skill 事实 | validate-skill-facts | 通过且不再写 YAML 账号权威 | 同上 |

- **Workspace checks：** notify 测试；`node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs`；typecheck。
- **E2E disposition：** required：有权限者能从页面触发试发入口；无权限按钮不可用且 API 403。
- **E2E owner/environment：** Lead / current-workspace。
- **Integration evidence：** implementation commit + direct-parent + Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先权限 DML，再 API，再按钮，最后 Skill。
- **兼容窗口：** 不适用。
- **监控信号：** 测试发送可过滤。
- **回滚或前向恢复：** 关闭 test 权限即可停用入口。
- **不可逆操作与批准点：** 无。
- **收缩条件：** Skill 与代码对账号权威描述一致。

## 10. 验收标准

- [ ] `AC-016`：账号级与模板级测试发送。
- [ ] `AC-018`：Skill 与注释同步。
- [ ] 已读取 Map 与适用 Skill。
- [ ] Evidence 完整。
- [ ] 未超出 writable_paths。
- [ ] implementation commit 与父分支 result。
- [ ] E2E required 由 Lead 完成。
- [ ] 无未批准偏差。
