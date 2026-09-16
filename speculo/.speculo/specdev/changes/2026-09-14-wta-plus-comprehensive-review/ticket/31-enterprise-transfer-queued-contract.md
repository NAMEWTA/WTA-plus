---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 .agents/skills/**/SKILL.md；执行前继续按命中scope展开"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "ef0588daecab40e12e49e99d29a12bf107e887ab432e10a790bbc893fc42b0cf", "phase": "plan", "operation": "bind-cross-module-state-and-verification-contract", "inputs": ["B-15真实Notify返回与Profile调用链"], "outputs": ["跨存储状态/失败恢复与调用者映射"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "dd3e42720caa894bcb31ba1503b8211f669b3bbc2bb9ea3e40590a3a9edea395", "phase": "plan", "operation": "bind-cross-module-state-and-verification-contract", "inputs": ["B-15真实Notify返回与Profile调用链"], "outputs": ["跨存储状态/失败恢复与调用者映射"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "f2e7e020552795c02285df65a941609fcd93692298951c012adbc555c20ac87f", "phase": "plan", "operation": "bind-cross-module-state-and-verification-contract", "inputs": ["B-15真实Notify返回与Profile调用链"], "outputs": ["跨存储状态/失败恢复与调用者映射"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["finding:B-15","contract:AC-031"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-31
title: 修复企业转移发码的同步/排队合同阻断
status: draft
planning_depth: deep
planning_depth_reason: "跨 Profile/Notify 状态机、短信外部副作用和测试合同；需真实服务验收"
ready: false
risk: critical
blocked_by: ["T-22", "T-23"]
contract_ids: [AC-031]
owner: user-review
expected_changes: ["<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>", "<Path>frontend/packages/domains/profile/</Path>", "<Path>frontend/packages/web-domains/profile/</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>", "<Path>frontend/packages/domains/profile/</Path>", "<Path>frontend/packages/web-domains/profile/</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>","<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: []
shared_path_owners: []
---

# T-31：修复企业转移发码的同步/排队合同阻断

> 仅为用户审核的实施计划；本轮不修改产品代码。

## 1. 战略与来源

- **Finding：** B-15 confirmed；生产Notify返回QUEUED，Profile transfer只接受ACCEPTED/DELIVERED，合法send被撤销并回滚。
- **目标：** 让Profile与Notify共享一个真实、可恢复的异步状态合同。

## 2. 决策状态

- **状态：** draft/ready=false。ADR-CR-009的可靠异步方向；Profile不再把NotificationMode.SYNC当作已发送。外部短信最终一致性和确认窗口需用户审核。
- **未决：** 用户是否接受最终一致性、短信失败恢复及确认窗口；未批准前不得改为同步网络调用。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| Profile transfer 与 Notify intent/outbox/challenge 状态、真实跨模块测试、前端状态反馈 | 已有权限、Client、PKCE/敏感数据、Outbox wake/lease/fence、Provider adapter | 将短信Provider I/O放进业务事务、删除失败重试、隐藏QUEUED或以测试stub代表生产 |

## 4. 实现契约

- **入口/seam：** <Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/usecase/impl/EnterpriseTransferUseCaseImpl.java</Path> 与 NotificationApplicationService。
- **不变量：** MySQL transfer record、Notify intent、delivery/outbox 在同一业务事务中原子落库；Redis challenge 只是带 TTL 的幂等投影，必须携带 transfer/challenge 版本和消费 token，不能宣称与 MySQL 同事务。challenge 只有可证明的 ACTIVE 才能确认；QUEUED/PENDING_DELIVERY 不可立即确认；同一 transfer/通知幂等；跨 Client/权限不变。
- **错误：** Provider失败、Outbox回滚、超时和重复点击均给出稳定可重试状态，不吞错误；REVOKED/EXPIRED 必须在 MySQL transfer 状态中可追踪，Redis 撤销采用删除或短 TTL 失效并可由状态重放恢复，不能只靠 Redis key 消失推断业务结果。

## 5. 执行路线

1. 用真实 <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path> 确认 SYNC 与 ASYNC 当前都只持久化 QUEUED/PENDING/Outbox，建立当前测试 stub 返回 ACCEPTED 却掩盖生产语义的红灯。
2. 在 Profile transfer contract 中明确可靠异步：将当前仅在发送成功后调用 `recordChallenge` 的时序前移为同一 MySQL 事务持久化 transfer record、Notify intent、recipient/delivery 和 outbox；事务提交前不得激活或消费 Redis challenge，提交后由 outbox/事件驱动 Redis challenge 投影从 PENDING_DELIVERY 激活或撤销；Redis 写失败可由可重放事件恢复，不能把 Redis challenge 与 MySQL 宣称为同一事务。
3. 删除假同步 mode 或对未支持 mode 显式失败；不把 Provider I/O 塞回业务事务，不让测试 mock 制造不存在的 ACCEPTED。同步检查所有仓内 SYNC 消费者：<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java</Path>、<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyTestSendService.java</Path>、<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path> 和 <Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>，逐一决定其排队、验证码缓存和用户提示语义。
4. 同时核对PersonRebindNotificationService把QUEUED判FAILED、CaptchaController忽略receipt及NotifyTestSendService展示；逐一明确排队/投递/失败，不能全局把QUEUED映射为DELIVERED。
5. 补跨 module 真实实现 contract test、Outbox wake/失败回滚、Redis 投影重放/过期及用户确认窗口；确认短信发送、MySQL transfer 状态和 Redis challenge 状态最终一致，并覆盖 Provider 成功后投影失败的恢复序列。
6. 同步前端转移页面的queued/active/failed可观察状态、重试与防重复提交，保留权限、Client和敏感信息规则。

## 6. 路径与所有权

- 仅用户批准后写入 frontmatter paths；Notify shared semantic resources由T-22/T-23 owner协调。
- 发现状态/DDL/接口交集时停止并更新Map，不抢占其他票。

## 7. 验证矩阵

| 风险 | 方法 | 预期 | Evidence |
|---|---|---|---|
| AC-031.1 | 真实 Profile→Notify seam、MySQL/Redis/Provider隔离服务与浏览器 | 合法转移send得到可观察QUEUED并最终按Provider结果激活challenge，而不是立即DELIVERY_FAILED | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |
| AC-031.2 | 真实 Profile→Notify seam、MySQL/Redis/Provider隔离服务与浏览器 | Notify事务回滚/Provider失败时challenge不可确认且可恢复重试，成功只激活一次 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |
| AC-031.3 | 真实 Profile→Notify seam、MySQL/Redis/Provider隔离服务与浏览器 | 真实Notify实现与Profile contract test不使用固定ACCEPTED stub绕过队列语义 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |
| AC-031.4 | 真实 Profile→Notify seam、MySQL/Redis/Provider隔离服务与浏览器 | 用户看到发送中/失败/可重试状态，重复点击不产生多个transfer challenge | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |

- **Workspace checks：** 见 §12 的实际工作目录、命令和隔离验收边界；本轮全部为计划，未执行 Maven、构建或外部服务测试。
- **E2E：** required；由Lead在parent-candidate/current workspace按实际模式执行。

## 8. 迁移、发布与恢复

- 先建立 MySQL transfer 状态迁移与回滚/dry-run，再收缩 SYNC 假合同；旧未完成 challenge 需明确失效或前向恢复，Redis 投影可由 outbox 重放重建。
- Provider成功但数据库失败的重试/人工核对必须可追踪；不承诺外部exactly-once。

## 9. 验收标准

- [ ] 合法转移send得到可观察QUEUED并最终按Provider结果激活challenge，而不是立即DELIVERY_FAILED。
- [ ] Notify事务回滚/Provider失败时challenge不可确认且可恢复重试，成功只激活一次；MySQL 提交失败时 Redis 投影与消费均不发生或可由 outbox 补偿，不能产生“已确认但 transfer 未落库”。
- [ ] 真实Notify实现与Profile contract test不使用固定ACCEPTED stub绕过队列语义。
- [ ] 用户看到发送中/失败/可重试状态，重复点击不产生多个transfer challenge。
- [ ] 真实实现contract test不固定mock成ACCEPTED。
- [ ] Evidence、Map、Spec、ADR状态一致且本票仍由用户批准后再Ready。

## 10. Skill与停止

- 需要读取 `namewta-fullstack-development`、`wta-module-guide` 入口及Notify/Profile事务、通知、API references；入口缺失或验证不可用即block-ticket。
- 任一权限/事务/数据状态不明、环境缺失或测试stub与生产不一致即停止。

## 11. 完成出口

- 只有真实跨模块/E2E/失败回滚证据齐全、父分支结果可回读且无未批准偏差才可done；当前保持draft。

## 12. 最新证据校正与具体检查

ADR-CR-009的可靠异步方向；Profile不再把 NotificationMode.SYNC 当作已发送。外部短信最终一致性和确认窗口需用户审核。当前 challenge 由 Redis 存储；MySQL transfer/Notify intent/outbox 负责事实与恢复，Redis 仅作带版本和消费 token 的幂等投影，必须通过可重放 outbox 明确跨存储一致性。

- 合法转移send得到可观察QUEUED并最终按Provider结果激活challenge，而不是立即DELIVERY_FAILED。
- Notify事务回滚/Provider失败时challenge不可确认且可恢复重试，成功只激活一次。
- 真实Notify实现与Profile contract test不使用固定ACCEPTED stub绕过队列语义。
- 用户看到发送中/失败/可重试状态，重复点击不产生多个transfer challenge。
- Person rebind和通知测试发送正确显示待投递；验证码已有语义经单独验证，不凭QUEUED推断必然失败。

实际命令候选（实施前对照script重新解析，当前not-run）：

- `backend: ./mvnw -pl wta-modules/wta-profile,wta-modules/wta-notify,wta-admin -am test`
- `isolated MySQL/Redis/provider: transfer queued-to-active matrix`
