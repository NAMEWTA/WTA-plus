---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-18枚举.agents/skills入口并按本票真实路径/领域绑定；Map为最低集合"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/31-enterprise-transfer-queued-contract.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>"], "outputs": ["T-31的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/31-enterprise-transfer-queued-contract.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>"], "outputs": ["T-31的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "441de2ccc513e09820ed3d7d2faf559eeaa7466202e4fbb0c8dd3eabf09510c9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/31-enterprise-transfer-queued-contract.md</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>"], "outputs": ["T-31的架构/权限/数据边界检查与定向实现diff"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/31-enterprise-transfer-queued-contract.md</Path>", "current-workspace实际diff及本票验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path>：命令、退出码、测试数、AC与Skill Execution Records"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "finding:B-15", "contract:AC-031"]
artifact: ticket
change: 2026-09-14-wta-plus-comprehensive-review
id: T-31
title: 修复企业转移发码的同步/排队合同阻断
status: "ready"
planning_depth: "deep"
planning_depth_reason: "安全/鉴权、公共合同、数据一致性或共享核心路径变更：修复企业转移发码的同步/排队合同阻断"
ready: true
risk: critical
blocked_by: ["T-22"]
contract_ids: [AC-031]
owner: single-agent
expected_changes: ["<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>", "<Path>frontend/packages/domains/profile/</Path>", "<Path>frontend/packages/web-domains/profile/</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>frontend/e2e/</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>", "<Path>frontend/packages/domains/profile/</Path>", "<Path>frontend/packages/web-domains/profile/</Path>", "<Path>frontend/packages/web-domains/notify/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>frontend/e2e/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/</Path>","<Path>{roots.state}/specdev/adr/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path>", "<Path>backend/wta-modules/wta-notify/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/</Path>", "<Path>backend/wta-admin/src/test/</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path>", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>", "<Path>frontend/packages/domains/profile/</Path>", "<Path>frontend/packages/web-domains/profile/</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>frontend/e2e/</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-modules/wta-notify/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/test/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-admin/src/test/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-modules/wta-profile/wta-profile-person/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-api/src/main/java/org/namewta/notify/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java</Path> => single-agent (Lead; serial T-31 turn)", "<Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path> => single-agent (Lead; serial T-31 turn)", "<Path>frontend/packages/domains/profile/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>frontend/packages/web-domains/profile/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> => single-agent (Lead; serial T-31 turn)", "<Path>frontend/packages/api-contracts/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>frontend/tooling/openapi/</Path> => single-agent (Lead; serial T-31 turn)", "<Path>frontend/e2e/</Path> => single-agent (Lead; serial T-31 turn)"]
---

# T-31：修复企业转移发码的同步/排队合同阻断

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先读Map，再按Skill矩阵读取适用入口/引用，再读本票。禁止任何implementation/review/research子代理。Ready仅表示计划合同就绪，不表示已经授权实施或已验证通过。

## 1. 战略与来源

- 目标与可观察产出：真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交。
- 来源：B-15；AC-031；USER-DECISION: 全面完善计划、无兼容、单人串行。
- 当前事实与调用链：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/re-review.md</Path>中T-31行及对应专项报告；源码导航为frontmatter预计修改点。
- 规划深度：deep；安全/鉴权、公共合同、数据一致性或共享核心路径变更：修复企业转移发码的同步/排队合同阻断。

## 2. 决策状态

### 已锁定决策

修正假同步合同，复用Notify公开query按需核验投递状态；不预设新增Profile事件总线、投影Outbox或持久验证码副本。

### 已采用的低影响假设

沿用当前仓库版本与既有模块命名；实施前回读实际源码，路径变化由本票修订，不猜测不存在的实现。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| transfer+notification同事务→QUEUED+challengeId→Notify.query→confirm | 现有Redis PENDING_DELIVERY/ACTIVE、DB CAS、Notify公开query；无额外Outbox/OTP副本 | 本票之外的模块重写、兼容桥、远程发布与重要数据操作 |

## 4. 要构建什么

transfer+notification同事务→QUEUED+challengeId→Notify.query→confirm。调用者可观察到：真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交。失败时：仅已提交归属记录、有效OTP及ACCEPTED/DELIVERED激活；失败/过期可重新发码。

## 5. 实现契约

- 入口、输入输出与数据流：transfer+notification同事务→QUEUED+challengeId→Notify.query→confirm。
- 不变量及失败语义：仅已提交归属记录、有效OTP及ACCEPTED/DELIVERED激活；失败/过期可重新发码。
- 公共合同：本票只按以上行为及执行路线变更；同步全部仓内调用/生成物，沿用已有权限/Client/owner校验。未列出的接口保持原语义。
- 兼容：用户明确无需旧版兼容；仓内一次切换，不加双路由/版本等待。实际供应商协议仍须遵守。
- 安全与隐私：凭据不进入日志/UI证据；越权/过期/无owner拒绝；数据库与资源约束不能为前端成功而放宽。

原方案新增事件、投影Outbox和MySQL挑战副本缺少必要性。Notify已有query入口，按需读取即可保持原先“供应商接受后激活”的安全语义。Profile不能直接访问Notify表/实现；notificationId归属由服务端关联决定，不接受前端任意替换。新字段只修改10-cde-base-ddl.sql；不要求存量迁移或兼容适配。

## 6. 执行路线

1. 真实Profile→NotificationApplicationService提交应返回QUEUED；确认receipt.queued当前还受mode==ASYNC影响，同步删除假SYNC语义及全部仓内消费者。
2. 保留现有Redis challenge的PENDING_DELIVERY/ACTIVE/失效机制。在同一MySQL事务保存transfer记录及Notify intent/outbox，并记录notificationId关联；只有提交成功的transfer记录才可进入后续确认。
3. send返回排队结果和challengeId。用户查看状态或提交验证码时，通过Notify公开query核验该notificationId；QUEUED提示等待，ACCEPTED/DELIVERED才幂等激活且沿用原到期时间，终态失败撤销并允许重新发码。不等待供应商必定提供DELIVERED回执。
4. confirm必须核对当前用户、transfer记录、绑定版本、有效期及Notify状态；未知或未提交transfer拒绝。复用现有DB条件更新与Redis消费token，成功后重复确认不再转移。
5. MySQL回滚而Redis已stage时仅残留不可确认且有TTL的挑战；验证码已消费但确认事务失败时提供重新发码恢复，不声称跨存储原子，也不默认为此建设可重放验证码投影。
6. PersonRebind的排队日志、Captcha的验证码缓存和Notify测试发送分别沿用自身业务语义；不把QUEUED统一改成DELIVERED。前端显示排队/失败/过期及可重试状态。

## 7. 路径访问契约

预计点、可写范围、只读上下文及共享项以frontmatter为唯一权威。共享owner固定single-agent；只有当前票轮次可写，下一票须回读前一结果。跨模块目录只允许本票行为必需的文件，目录授权不意味着重写全部模块。新增测试位于同模块测试目录；未覆盖的新路径先修订本票/Map再写。
保留当前用户未提交改动、永久ADR/context、供应商源码与运行数据；生成物通过正式工具更新。

## 8. 验证矩阵

| 行为或风险 | 验证接缝/步骤 | 预期结果 | Evidence |
|---|---|---|---|
| 正常路径 | transfer+notification同事务→QUEUED+challengeId→Notify.query→confirm；执行下列定向命令及对应场景 | 真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |
| 失败路径 | 仅已提交归属记录、有效OTP及ACCEPTED/DELIVERED激活；失败/过期可重新发码；固定时序/故障注入，记录输入与最终可观察状态 | 无越权、错误状态或部分提交；可按定义恢复 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |
| 回归 | 运行所属包原有测试及受影响调用者；逐项核对下列AC | 只有匹配用户、已提交transfer、有效验证码及ACCEPTED/DELIVERED通知可以确认；QUEUED/失败/过期不可确认；发送或确认事务失败后无错误绑定，Redis残留不可越权且可重新发码恢复；真实跨模块测试不固定mock ACCEPTED；重复确认不重复转移，PersonRebind/Captcha/TestSend状态各自准确 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path> |

Workspace checks：current-workspace；`frontend:`/`backend:`表示先进入该目录，其他命令cwd为仓根，逐条串行执行。新增用例实施时登记精确选择器、实际测试数与跳过项，零测试/required跳过不算通过。

- `backend: ./mvnw -pl wta-modules/wta-profile,wta-modules/wta-notify,wta-admin -am test`

- E2E disposition：required: 真Profile→Notify QUEUED、worker受理→确认及DB/Redis部分失败、重复确认/错用户/绑定变更。
- E2E owner/environment：single-agent（Lead）/current-workspace；使用隔离MySQL/Redis/OSS及必要真实HTTP/浏览器，禁止连生产。场景步骤以上表、本票AC为准；需新用例时在写集内创建后记录精确命令。
- Integration evidence：记录parent before、implementation commit及direct-parent检查；result SHA等于通过验证的implementation commit，candidate不适用。当前全部产品检查not-run。

## 9. 发布、迁移与恢复

- 顺序：前置票产生已验证合同后实施本票；源码、仓内消费者、测试和生成物同批交付。涉及DDL只编辑10-cde-base-ddl.sql，新环境按六文件基座初始化；不增加存量迁移工程。
- 兼容窗口：无；不保留旧接口或数据格式桥。生产部署不是本票自动步骤。
- 监控/诊断：观察本票AC的成功/错误状态、耗时及资源/持久化结果，日志只含安全元数据；复用现有观测入口，不新建监控平台。
- 恢复：保留原到期时间；已消费但DB回滚允许重新发码；不重放已成功企业转移。
- 不可逆批准点：提交、推送、部署、运行数据删除/修复分别需授权；本轮只有计划文档授权。
- 收缩条件：本票替代的旧调用/配置引用归零且仓内回归通过；无被替代入口时不适用，不为凑清单扩大删除范围。

## 10. 验收标准

- [ ] `AC-031`：真实Notify返回QUEUED时send不抛DELIVERY_FAILED，transfer与通知同事务提交。
- [ ] `AC-031`：只有匹配用户、已提交transfer、有效验证码及ACCEPTED/DELIVERED通知可以确认；QUEUED/失败/过期不可确认。
- [ ] `AC-031`：发送或确认事务失败后无错误绑定，Redis残留不可越权且可重新发码恢复。
- [ ] `AC-031`：真实跨模块测试不固定mock ACCEPTED；重复确认不重复转移，PersonRebind/Captcha/TestSend状态各自准确。
- [ ] 按Map→适用Skill→本票完成读取及实际调用；所有required Skill记录passed并可回读。
- [ ] 正常/失败/回归及required E2E均完成，证据写入<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path>，未执行不得标通过。
- [ ] 修改不超出写集，共享项只有single-agent当前票轮次写入。
- [ ] 获得授权后形成非空implementation commit，Lead完成direct-parent验收并记录parent result SHA；未获授权不提交、不标Done。
- [ ] Ticket、Map、Goal与Evidence一致；不存在未批准偏差。

## 11. SKILL 调用计划

frontmatter绑定的项目Skill在implementation阶段接收本票路径和上游合同，产出适用分层、权限、数据/资源边界及实现diff；engineering-standards在verify阶段根据本节命令选择受影响门禁并输出AC/退出码/E2E记录。按入口scope展开引用，不以“已读”代替实际操作。
必需入口缺失或sha256漂移时阻塞本票；Lead回读差异后更新绑定及Map，不能自动接受新摘要。实际记录归<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-31.md</Path>的`## Skill Execution Records`，本次规划不伪造实现调用记录。

## 12. 停止、检查点与交付

交付本票完整可观察行为及验收证据；数量以Map为准。缺依赖/测试环境/Skill、越界或高影响事实变化时停止受影响票，保留checkpoint和失败证据，其他独立票仍可串行推进。恢复先读Goal、Map、本票、状态及最新Evidence；记录实际HEAD/dirty差异，禁止覆盖用户修改。
依赖：T-22。单票完成条件为全部AC、实际Skill证据和获授权的direct-parent出口；仅补文档不能标Done。
