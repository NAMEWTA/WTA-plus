---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-36的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-36的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-36的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-36的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-36的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/36-atomic-in-app-delivery.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-36-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:dispatch", "notify:result-tx", "table:notify_message", "table:notify_message_recipient", "contract:AC-036"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-36"
title: "站内信落库与结果同事务且可安全重试"
status: "done"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-036"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/event/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/InAppNotificationPort.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/event/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/InAppNotificationPort.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/event/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/InAppNotificationPort.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/event/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/test/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/InAppNotificationPort.java</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)", "<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path> => single-agent (Lead; exclusive current workspace; T-36 turn only)"]
---

# T-36：站内信落库与结果同事务且可安全重试

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。用户已激活Goal并允许gpt-6-sol/xhigh原生子代理；current单产品writer，无新worktree。本票由cors_audit实施，Lead独占治理、commit和真实验收。

## 1. 战略与来源

- 来源：R64-N-03；AC-036；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：共享消息/关系先查后插；已有 uk_notify_message_recipient(message_id,user_id)，无需盲目新增唯一键。persist 在结果事务之前。
- 可观察产出：同 intent 的不同收件人并发投递均可读取且关系唯一；本地消息、关系与投递结果原子提交，实时事件仅提交后发送。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。当前已授权本地实施、逐票commit/direct-parent验收。

### 已确认方案

2026-09-23用户已逐项接受D-002—009并确认整体共识（LOG-010—018）；本票按已接受ADR定稿。 完整决定及来源以<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>为准。

### 已采用的低影响假设

沿用当前模块与测试基座；测试样本为隔离合成数据，不作为生产容量或SLO。

### 执行前置

G整体共识已确认；执行前仍需复核当前HEAD/归属、实际实施与commit授权及必要测试环境。Ready不代替Goal激活。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 本票可观察产出及所列消费者、验证、文档 | 既有wta-api/common、模块模式、事务/权限/生命周期与测试 | 无关模块重写、新队列/锁平台、真实数据修复、远程发布及相邻OIDC实现 |

## 4. 要构建什么

同 intent 的不同收件人并发投递均可读取且关系唯一；本地消息、关系与投递结果原子提交，实时事件仅提交后发送。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：扩展 NotifyAtomicResultIntegrationTest；真实 DSTransactional 代理、MySQL 双连接、提交故障与 AFTER_COMMIT。
- 外部行为：同 intent 的不同收件人并发投递均可读取且关系唯一；本地消息、关系与投递结果原子提交，实时事件仅提交后发送。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 以两真实 DB 连接和屏障复现同 intent 两 delivery 并发；注入关系/Attempt/finish/commit 失败。
2. 复用已有 PK/唯一键，DAO 原子插入或仅捕获明确重复键并核验快照相同，不吞其他 SQL 错误。
3. 在被代理 UseCase 的短 DSTransactional 内完成 IN_APP message/recipient/result/attempt/outbox/aggregate；沿用 Intent→Outbox→Delivery 的一致锁序和 fence。
4. 本地明确回滚可有限重试；确定参数失败终结；禁止 IN_APP 进入供应商 WAITING_RECEIPT。实时通过 DsTxEventListener AFTER_COMMIT 隔离。
5. 形成存量 IN_APP UNKNOWN/缺关系只读清单及按指定 ID 幂等重做操作稿；不执行真实数据修复。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | 扩展 NotifyAtomicResultIntegrationTest；真实 DSTransactional 代理、MySQL 双连接、提交故障与 AFTER_COMMIT | 并发两个用户一条共享消息、每人一条关系；重复任务不增加关系 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-36-replan-2026-09-23.md</Path> |
| 失败/竞争 | 任一 SQL/提交失败不留下消息与结果部分提交，失效租约零写入 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 实时发送失败不回滚或重复已落库消息；本地暂时失败能重试收敛 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下由当前Evidence中实际39类定向门禁与真实隔离命令取代，完整argv和源复用边界见Evidence：

- backend Maven精确39类178项，C2执行并在C3按未变输入复用；最终C3真实Atomic66/Wake1、C2真实SMS8未变输入复用，均零skip。

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 扩展 NotifyAtomicResultIntegrationTest；真实 DSTransactional 代理、MySQL 双连接、提交故障与 AFTER_COMMIT。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：先发布幂等与分类修复，再审批指定 IN_APP 的恢复；外部 MAIL/SMS UNKNOWN 不包含在恢复清单。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。用户已激活Goal并授权本地实施、逐票commit/direct-parent；推送、全应用部署、真实数据修复及归档不由此推断。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [x] `AC-036`：并发两个用户一条共享消息、每人一条关系；重复任务不增加关系。
- [x] `AC-036`：任一 SQL/提交失败不留下消息与结果部分提交，失效租约零写入。
- [x] `AC-036`：实时发送失败不回滚或重复已落库消息；本地暂时失败能重试收敛。
- [x] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [x] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [x] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [x] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-36-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision154 当前实施

base `5118051403de0648540646d98536d8c4f3f9f26d`；cors_audit唯一产品writer，Lead治理、commit、真实隔离环境及E2E。复用Intent→Outbox→Delivery锁序和fence，将IN_APP消息/关系/结果/Attempt/Outbox/聚合放入真实代理短DSTransactional，AFTER_COMMIT才推送；SMS/MAIL外部I/O仍在事务外。

测试使用隔离合成库，修正原夹具固定测试账号为环境注入；六SQL真实装载，双连接、六处SQL失败、提交前断连/提交后ACK丢失、失效租约、重复任务和推送失败需可观察断言。测试快照保留T35的170+8基线；不将条件skip当验收。公共端口语义/调用者须同步；生产worker不在当前写集，确需修改先登记。历史IN_APP异常修复仅提供只读盘点与按ID恢复操作稿，不执行生产修复，不包含MAIL/SMS UNKNOWN。

## revision155 IN_APP有界尝试预算

代码事实确认：原claim不计次数，结果事务回滚也回滚attempt_count，不能宣称现有重领机制有限。采用原有Outbox字段、既有代理端口和统一锁序：确定参数预检后，beginInAppAttempt独立短DSTransactional按Intent→Outbox→Delivery锁及数据库fence消耗一次预算；成功返回才进入消息/关系/结果的原子事务。IN_APP outbox.attempt_count表示已开始尝试（含随后回滚/崩溃），Delivery与Attempt只记录原子提交的结果；其他渠道保持原含义。消息事务不再次消耗预算。

预算耗尽则在锁内将IN_APP Delivery FAILED、Outbox DEAD_LETTER并刷新聚合，零persist；预算事务失败或提交结果不确定时停止，不能猜测已获得预算。消息事务提交ACK丢失时依已持久DONE/DELIVERED抑制重投，DB暂不可读则等待安全恢复。完全不可写期间不能保证提交终态，但不得在未取得持久预算时调用persist；恢复后仍在预算上限内收束。允许SQL失败后保留独立预算，不允许消息/关系/投递结果部分提交。

验证补充：预算先提交后消息回滚、max边界/耗尽零persist、预算提交ACK丢失及失效lease、同lease重入不能越过最大物理次数。既有写集覆盖port/usecase/runtime/DAO/Mapper/XML；不改worker/claim，不新增表/状态机，不转移给T38，外部渠道未知合同不变。

### revision155 预留去重与开发红灯环境补充

同有效lease重复begin仅第一项获准：采用固定内部码IN_APP_ATTEMPT_RESERVED，不把lease token放入错误码/监控。claim SQL成功领取新token时仅清此固定预留码，其他历史错误保留；预算仍在begin事务消耗，worker不改。此处细化前段“不改claim”为不改变领取策略，仅清理上次预留标志。旧owner/newowner及同lease重入必须验证。

首个开发红灯run c8ae375d4258fb06在创建故障trigger时报MySQL1419，未到业务断言；Maven1、1error/0failure/0skip，非行为红灯。隔离MySQL仅调整trust_function_creators启动参数供故障注入，不给应用全局SUPER、不改部署。旧驱动把error归开发red的记录保留并由Lead assessment明确否决；新版要求精确目标方法的1failure/0error。两个owned容器/进程组已清理，32794/32795已关；该开发运行不计正式候选attempt。

## revision156 预算字段语义文档写集

编辑前增加两条精确路径：NotifyOutbox.java字段Javadoc，以及唯一六SQL中的10-cde-base-ddl.sql，仅notify_outbox.attempt_count/last_error_code中文注释。已有DDL“领取次数”不符合旧结果计数也不符合新预算语义，须同步为IN_APP已开始尝试预算、外部渠道已提交结果次数，固定IN_APP_ATTEMPT_RESERVED内部标记。无列/类型/索引/结构变化、不重放存量基座；全新隔离六SQL装载复核仍必需。预算上限指一次自动调度周期；合法人工重试新周期由T38精确API合同负责。

### revision156 持久化摘要长度核查

当前InAppNotificationService把完整content同时写入varchar(1000)的message摘要和longtext正文；公告内容只NotBlank，合法长文会因摘要列溢出失败。现有写集内修正字段映射：message至多1000个Unicode code point、不截断代理对，content保持完整，幂等快照比较仍比较完整content。真实MySQL覆盖长文/emoji边界，不通过扩列、放宽SQL模式或截断正文规避。

## revision157 已验收

最终source/result `64d67d5fb150620b25ada107115ea2207736c039`，formal attempts=3，详见evidence/T-36.md。当前脚本操作稿未执行生产修复；完整最终候选仍归T30。
