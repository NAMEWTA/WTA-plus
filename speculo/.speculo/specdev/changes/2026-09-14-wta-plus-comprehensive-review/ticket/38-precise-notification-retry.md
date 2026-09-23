---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/38-precise-notification-retry.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>"], "outputs": ["T-38的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/38-precise-notification-retry.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>"], "outputs": ["T-38的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/38-precise-notification-retry.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>"], "outputs": ["T-38的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/38-precise-notification-retry.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>"], "outputs": ["T-38的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/38-precise-notification-retry.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-38-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:retry-api", "notify:outbox", "openapi:notify", "contract:AC-038"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-38"
title: "重试与取消接口准确定位资源和任务"
status: "ready"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-36", "T-37"]
contract_ids: ["AC-038"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyOutboxMapper.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/RetryReceipt.java</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/packages/web-domains/notify/src/NotificationPage.vue</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyOutboxMapper.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/RetryReceipt.java</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/packages/web-domains/notify/src/NotificationPage.vue</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyOutboxMapper.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java</Path>", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/RetryReceipt.java</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/packages/web-domains/notify/src/NotificationPage.vue</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyOutboxMapper.java</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-api/src/main/java/org/namewta/notify/api/RetryReceipt.java</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>frontend/packages/domains/notify/src/</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>frontend/packages/web-domains/notify/src/NotificationPage.vue</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>frontend/packages/api-contracts/</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>frontend/tooling/openapi/</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => single-agent (Lead; exclusive current workspace; T-38 turn only)"]
---

# T-38：重试与取消接口准确定位资源和任务

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。保留单人串行、无子代理/无新worktree。本票计划已Ready；本轮没有实施或重验，等待用户自行激活Goal。

## 1. 战略与来源

- 来源：R64-N-05；AC-038；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：retry 忽略 deliveryId，无任务仍返回 QUEUED；Controller 在有 body 时不使用 path ID。
- 可观察产出：URL 唯一定位 intent；指定 delivery 只重试其所属一项，无可重试任务返回真实状态及零计数；外部 UNKNOWN 明确拒绝自动重发。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。最新用户仅授权计划。

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

URL 唯一定位 intent；指定 delivery 只重试其所属一项，无可重试任务返回真实状态及零计数；外部 UNKNOWN 明确拒绝自动重发。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：HTTP ID 冲突、真实 DB requeue 并发、API/领域合同测试。
- 外部行为：URL 唯一定位 intent；指定 delivery 只重试其所属一项，无可重试任务返回真实状态及零计数；外部 UNKNOWN 明确拒绝自动重发。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 以 Controller/UseCase 组合固定 URL/body 冲突、跨 intent delivery、零任务与重复操作红灯。
2. 将 path ID 与 body ID 一致性验证放 HTTP 边界；保持 GET 查询、POST 变更、安全 @Log。
3. 在锁内按 deliveryId 与归属过滤，只有实际 requeue 成功才更新聚合/唤醒；RetryReceipt 明确 queuedCount。
4. 无业务幂等实现的可选 retry key 采取明确拒绝非空输入，依靠状态 CAS 与唯一活跃任务保证重复重试不增殖。
5. 同步 wta-api、OpenAPI 生成与领域 consumers；外部 UNKNOWN 先核对，IN_APP 可依 T-36 安全重试。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | HTTP ID 冲突、真实 DB requeue 并发、API/领域合同测试 | A/B 失败仅指定 A 排队；外部 UNKNOWN 拒绝，归属错误/ID 冲突拒绝 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-38-replan-2026-09-23.md</Path> |
| 失败/竞争 | 全部完成时状态不变且 queuedCount=0；重复请求无多份活跃 outbox | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 取消返回的对象与 URL 一致；鉴权和日志保持 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-modules/wta-notify,wta-admin -am test`
- `pnpm --dir frontend --filter @namewta/domain-notify test`
- `pnpm --dir frontend --filter @namewta/tooling-openapi openapi:check`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: HTTP ID 冲突、真实 DB requeue 并发、API/领域合同测试。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：同批修改源码、消费者、测试及生成合同；无需新增数据库迁移。涉及已有库时必须Tag差异、备份、隔离演练，不重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。当前均未授权。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-038`：A/B 失败仅指定 A 排队；外部 UNKNOWN 拒绝，归属错误/ID 冲突拒绝。
- [ ] `AC-038`：全部完成时状态不变且 queuedCount=0；重复请求无多份活跃 outbox。
- [ ] `AC-038`：取消返回的对象与 URL 一致；鉴权和日志保持。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-38-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。
