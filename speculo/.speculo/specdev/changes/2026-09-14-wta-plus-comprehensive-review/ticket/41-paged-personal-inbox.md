---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>"], "outputs": ["T-41的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>"], "outputs": ["T-41的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>"], "outputs": ["T-41的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>"], "outputs": ["T-41的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/41-paged-personal-inbox.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-41-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:inbox-api", "ui:admin-inbox", "openapi:notify", "contract:AC-041"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-41"
title: "本人收件箱提供完整分页与准确读取状态"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-34"]
contract_ids: ["AC-041"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyInboxService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/vo/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyMessageRecipientMapper.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/packages/web-domains/notify/src/InboxPage.vue</Path>", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path>", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/dto/NotifyInboxRow.java</Path>", "<Path>frontend/apps/admin-web/src/store/modules/notice.ts</Path>", "<Path>frontend/apps/admin-web/src/store/modules/notice.test.ts</Path>", "<Path>frontend/apps/admin-web/src/utils/push.test.ts</Path>", "<Path>frontend/packages/web-domains/notify/src/InboxPage.test.ts</Path>", "<Path>frontend/packages/web-domains/notify/src/runtime.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyInboxService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/vo/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyMessageRecipientMapper.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/packages/web-domains/notify/src/InboxPage.vue</Path>", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path>", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/dto/NotifyInboxRow.java</Path>", "<Path>frontend/apps/admin-web/src/store/modules/notice.ts</Path>", "<Path>frontend/apps/admin-web/src/store/modules/notice.test.ts</Path>", "<Path>frontend/apps/admin-web/src/utils/push.test.ts</Path>", "<Path>frontend/packages/web-domains/notify/src/InboxPage.test.ts</Path>", "<Path>frontend/packages/web-domains/notify/src/runtime.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyInboxService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/vo/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyMessageRecipientMapper.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/packages/web-domains/notify/src/InboxPage.vue</Path>", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path>", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path>", "<Path>frontend/packages/api-contracts/</Path>", "<Path>frontend/tooling/openapi/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/dto/NotifyInboxRow.java</Path>", "<Path>frontend/apps/admin-web/src/store/modules/notice.ts</Path>", "<Path>frontend/apps/admin-web/src/store/modules/notice.test.ts</Path>", "<Path>frontend/apps/admin-web/src/utils/push.test.ts</Path>", "<Path>frontend/packages/web-domains/notify/src/InboxPage.test.ts</Path>", "<Path>frontend/packages/web-domains/notify/src/runtime.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyInboxService.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/vo/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyMessageRecipientMapper.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/packages/domains/notify/src/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/packages/web-domains/notify/src/InboxPage.vue</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/packages/api-contracts/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/tooling/openapi/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/e2e/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/dto/NotifyInboxRow.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/apps/admin-web/src/store/modules/notice.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/apps/admin-web/src/store/modules/notice.test.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/apps/admin-web/src/utils/push.test.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/packages/web-domains/notify/src/InboxPage.test.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/packages/web-domains/notify/src/runtime.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/security/BrowserHttpsTransportIntegrationTest.java</Path> => cors_audit (sole product writer; Lead governance/integration)"]
---

# T-41：本人收件箱提供完整分页与准确读取状态

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
用户Goal已授权全部票本地实施、commit/direct-parent及native gpt-6-sol/xhigh协作。base d7d534cb03aa000d603a53c092c4bf1d48bb5cc3，cors_audit唯一产品writer，Lead治理/提交/服务/验收，其他代理只读或私有环境工具；无新worktree。

## 1. 战略与来源

- 来源：R64-N-08；AC-041；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：messageRecipients 查询 limit<=500，REST 为 List；readAll 已操作本人全部未读关系。
- 可观察产出：完整收件箱使用项目 PageQuery/PageResult 分页，稳定 create_time/message_id 排序；本人第501条可取，顶部只取最近摘要。全部已读仍作用本人全部消息。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。本地实施已获Goal授权；不推定远程发布或真实数据副作用授权。

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

完整收件箱使用项目 PageQuery/PageResult 分页，稳定 create_time/message_id 排序；本人第501条可取，顶部只取最近摘要。全部已读仍作用本人全部消息。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：真实 MySQL分页＋HTTP 登录身份过滤＋前端分页组件/浏览器。
- 外部行为：完整收件箱使用项目 PageQuery/PageResult 分页，稳定 create_time/message_id 排序；本人第501条可取，顶部只取最近摘要。全部已读仍作用本人全部消息。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 以501条本人关系和另一用户数据固定分页/计数/越权测试。
2. GET inbox 接受有界 pageNum/pageSize，返回 rows/total，单独在响应带本人 unreadTotal；默认与上限沿用项目分页合同。
3. 详情 GET messageId 必须经当前用户关系过滤；已有 seen/read/readAll 保持 POST，补安全 @Log。
4. 同步 domain/transport/生成 OpenAPI、InboxPage 与顶部读取；不得把 page rows 当总数。
5. 测试同时间排序、翻页后标记、全已读/另一用户隔离、失败重试和 generation；同版部署前后端。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | 真实 MySQL分页＋HTTP 登录身份过滤＋前端分页组件/浏览器 | 501条全部可分页访问、顺序稳定、他人关系不可见不可改 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-41-replan-2026-09-23.md</Path> |
| 失败/竞争 | unreadTotal 为本人真实未读总数；全部已读不限当前页且按钮说明清晰 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 顶部摘要与完整页已读结果同步；API/domain/typecheck 和生成合同一致 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-modules/wta-notify,wta-admin -am test`
- `pnpm --dir frontend --filter @namewta/web-domain-notify test`
- `pnpm --dir frontend --filter @namewta/domain-notify test`
- `pnpm --dir frontend --filter @namewta/tooling-openapi openapi:check`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 真实 MySQL分页＋HTTP 登录身份过滤＋前端分页组件/浏览器。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：同批修改源码、消费者、测试及生成合同；无需新增数据库迁移。涉及已有库时必须Tag差异、备份、隔离演练，不重放基座。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地commit/direct-parent已授权；远程push、部署、真实数据修复及归档另行核对。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-041`：501条全部可分页访问、顺序稳定、他人关系不可见不可改。
- [ ] `AC-041`：unreadTotal 为本人真实未读总数；全部已读不限当前页且按钮说明清晰。
- [ ] `AC-041`：顶部摘要与完整页已读结果同步；API/domain/typecheck 和生成合同一致。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-41-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision173 当前实施合同

T34已完成，T50关闭后 clean base d7d534cb。默认pageNum=1/pageSize=20，上限100；非法参数拒绝，忽略外部排序参数，long偏移且超total短路，不能直接使用PageQuery MAX默认值/getFirstNum。PageResult扩展flat rows/total/unreadTotal；三个查询共享本人有效message JOIN语义，unreadTotal全量read_time为空。按recipient.create_time DESC/message_id DESC稳定排序；公开createTime明确为收件时间，顶部保留服务端顺序不按消息时间重排。三次查询为最终一致，读/推送后刷新，不声称并发强快照。列表不取/返回完整content，详情单次本人JOIN，缺失与他人同形失败。

BE-CRUD-003先类型化MPJ；现有BaseMapperPlus加MPJBaseMapper，fresh wrappers；内部NotifyInboxRow在domain/dto，Service转换公开VO，DAO独占Mapper/Page类型。XML只有真实MPJ不足证据才能采用。无新DDL。本人seen/read/readAll权限/幂等不变，readAll全用户并补不含正文的安全@Log。

顶部取1/10，badge用服务端全局unreadTotal；完整页自有20条分页，全部已读含历史。新增宿主非密钥session epoch port，使同一mounted页切换身份立即清空列表/详情并隔离旧success/error/finally；保留T34 pending/dirty/token/generation。顶栏与全页详情均单独GET并做好关闭/卸载/切换隔离。运行时port及Admin组合入口已加入硬写集，不能把原始token传入domain。

阶段1仅在admin notify测试根写现有API可编译的NotifyInboxPagingIntegrationTest，owned真实MySQL/HTTP：501本人关系（含共享消息）与另一用户，旧GET?pageNum=26&pageSize=20应在rows/total业务断言变红，不以编译、环境或鉴权失败代替。Lead记录后再授权生产实现。

阶段2覆盖所有消费者与旧mock/真实E2E响应迁移，权限/Client既有合同、孤儿JOIN、相同时间逆序ID、巨大页码、全量readAll和同页会话切换负向测试。真实501浏览器零fake API，并重验T34无推送收件。生成OpenAPI仅固定full JAR实际抓取→正式fetch/generate/check。Lead独占服务/构建/commit；所有写生成声明的构建与exact-clean实测严格串行。路径外需要先登记。
