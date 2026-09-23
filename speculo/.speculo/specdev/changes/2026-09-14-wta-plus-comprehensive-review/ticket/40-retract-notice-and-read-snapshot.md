---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/40-retract-notice-and-read-snapshot.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>"], "outputs": ["T-40的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/40-retract-notice-and-read-snapshot.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>"], "outputs": ["T-40的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/40-retract-notice-and-read-snapshot.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>"], "outputs": ["T-40的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/40-retract-notice-and-read-snapshot.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-40-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:notice-version", "notify:cancel", "ui:admin-inbox", "contract:AC-040"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-40"
title: "公告撤回停止待发且收件人读到正确快照"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-38", "T-39", "T-41"]
contract_ids: ["AC-040"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>", "<Path>frontend/packages/web-domains/notify/src/</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path>", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifyNoticeVersionFence.java</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticePublisherTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/usecase/NotifyNoticeUseCaseTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticeServiceTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/support/NotifyNoticeVersionFenceTest.java</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/runtime/DispatchNotificationServiceTest.java</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>", "<Path>frontend/packages/web-domains/notify/src/</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path>", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifyNoticeVersionFence.java</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticePublisherTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/usecase/NotifyNoticeUseCaseTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticeServiceTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/support/NotifyNoticeVersionFenceTest.java</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/runtime/DispatchNotificationServiceTest.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path>", "<Path>frontend/packages/web-domains/notify/src/</Path>", "<Path>frontend/packages/domains/notify/src/</Path>", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path>", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>frontend/e2e/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifyNoticeVersionFence.java</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticePublisherTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/usecase/NotifyNoticeUseCaseTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticeServiceTest.java</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/support/NotifyNoticeVersionFenceTest.java</Path>", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/runtime/DispatchNotificationServiceTest.java</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyInboxUseCase.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/packages/web-domains/notify/src/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/packages/domains/notify/src/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/apps/admin-web/src/layout/components/notice/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/apps/admin-web/src/utils/push.ts</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/e2e/</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifyNoticeVersionFence.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>frontend/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticePublisherTest.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/usecase/NotifyNoticeUseCaseTest.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/NotifyNoticeServiceTest.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/support/NotifyNoticeVersionFenceTest.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>.agents/skills/wta-module-guide/references/modules/notify/index.md</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>.agents/skills/engineering-standards/references/notification.md</Path> => cors_audit (sole product writer; Lead owns governance and integration)", "<Path>backend/wta-modules/wta-notify/src/test/java/org/namewta/notify/service/runtime/DispatchNotificationServiceTest.java</Path> => cors_audit (sole product writer; Lead owns governance and integration)"]
---

# T-40：公告撤回停止待发且收件人读到正确快照

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。Goal已激活；cors_audit唯一产品writer，Lead治理/提交/隔离验收，两只读审查员；无新worktree。

## 1. 战略与来源

- 来源：R64-N-07；AC-040；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：retract 只改生命周期；发布 path 指向不消费 noticeId 的管理列表。
- 可观察产出：撤回只停止该发布版本未开始发送的任务，保留已送达内容与审计；本人从收件箱读快照，无需公告管理权限。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。用户已授权Goal实施和本地提交；远程操作仍按独立授权。

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

撤回只停止该发布版本未开始发送的任务，保留已送达内容与审计；本人从收件箱读快照，无需公告管理权限。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：真实发布→撤回→Worker→inbox；普通用户浏览器详情与越权。
- 外部行为：撤回只停止该发布版本未开始发送的任务，保留已送达内容与审计；本人从收件箱读快照，无需公告管理权限。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 固定发布后暂停 Worker→撤回→恢复 Worker 的 API/DB 场景及普通收件人详情路径。
2. 让公告状态及该发布版本Intent的持久撤回栅栏在同一 DSTransactional 中提交；不调用通用cancel，重新发布版本不可误取消。
3. 在 dispatch 发起前遵守取消；已进入外部 I/O 的结果按事实记录，不承诺追回或伪造失败。
4. 改跳转为本人收件箱 messageId 定位，详情经用户关系校验；管理侧已发布内容只读。
5. 同步撤回提示、重复撤回幂等、失败回滚、非本人访问拒绝；保留历史快照。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | 真实发布→撤回→Worker→inbox；普通用户浏览器详情与越权 | 撤回先于发送执行权时无新请求，旧快照不变；后续重发布不受前版本取消影响 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-40-replan-2026-09-23.md</Path> |
| 失败/竞争 | 已进入外部 I/O 或已接受提示不可追回，已送达站内信仍可读 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 普通收件人只读本人消息，管理权限不扩大 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-modules/wta-notify,wta-admin -am test`
- `pnpm --dir frontend --filter @namewta/web-domain-notify test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 真实发布→撤回→Worker→inbox；普通用户浏览器详情与越权。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：历史快照与已接受外部请求不删除；详情旧 path 只对已有通知作安全导向，不重发。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地commit/direct-parent已授权；不推断远程push、部署、生产数据操作或归档授权。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-040`：撤回先于发送执行权时无新请求，旧快照不变；后续重发布不受前版本取消影响。
- [ ] `AC-040`：已进入外部 I/O 或已接受提示不可追回，已送达站内信仍可读。
- [ ] `AC-040`：普通收件人只读本人消息，管理权限不扩大。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-40-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision182 — 实施派遣01

revision182：T40开始，base601b9273；13done/2cancelled/1in_progress/34ready。T41已完成，新增为前置。cors_audit唯一产品writer；Lead治理/提交/隔离验收。先现API真实撤回红灯，再版本栅栏、三处链接一致、本人深链与竞争/回滚/浏览器验收。

实施设计与当前事实见 <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-40-design-input.md</Path>；精确写集已在任何产品改动前登记。先test-only红灯checkpoint；不运行构建/服务或自行提交，由Lead串行操作。完整候选最多3次，失败保留并按Goal复盘；分段红灯不是整票验收。

## revision183 — 后端分段反馈

revision183：T40后端分段A2在a638ef48通过16定向单测、6真实MySQL/Redis，0fail/error/skip，源码前后clean、cleanup[]。A769e5d17元数据类型编译失败保留；公共Map<String,String>不变，内部noticeVersion用严格JSON字符串。完整候选attempts0；剩余竞争/重试/旧来源矩阵、前端和浏览器待实施，13done/2cancelled/T40in_progress/34ready。

真实run `ee99f3fbd678653d`，源码 `a638ef485c3819bfc8d67553877fd7c106ef9579`、tree `ceb4a5354641b7327bc65728bfaeb0f6678c52d8`；六SQL103表，2容器/1匿名卷/2端口及Maven进程组已回收。通过原红灯、两静态seed/普通missingIntent拒绝、late Snapshot SQL trigger完整回滚、V1撤回/V2送达、真实MAIL planner外部-only与混合三处path，未使用真实供应商。定向5类16测试均通过。原始失败与当前fresh XML/命令/哈希见 `T-40-current-2026-09-23/backend-a2/manifest.json`。这些是预先声明的后端反馈，未冒充整票验收。

Dispatch01C：cors_audit在原22写集内继续补充真正双连接gate先后/已获发送权真实ACCEPTED与UNKNOWN落库、旧owner、duplicate/retry零唤醒、legacy UNVERIFIED、本次Notice行数/关联冲突回滚等遗漏；保留现有6项绿灯。由Lead固定下一后端候选并串行运行，再移交前端/浏览器。若路径不足先登记；产品writer不自行构建、启动服务或提交。

## revision184 — 后端竞争矩阵与前端派单

revision184：T40后端A4在5fc319ed通过17真实MySQL/Redis测试（含双JDBC连接行锁）；A3共享8类135项通过，均0fail/error/skip、源码前后clean、cleanup[]。A2单测16沿用原始坐标且生产输入等价。13done/2cancelled/T40in_progress/34ready；完整候选attempts0，前端与真实浏览器尚待验收。

原始结果分别为A3 `270bd94e3f59a784929448e2cfa84ae913b686b7` / run `5102c97369fbecac`（16），共享 run `b1d860dd4901770e`（135），A4 `5fc319ed58d9586a92166459b3296618e7dc47dc` / run `526ecc242d191177`（17）。A4 tree `445d0a24596b32360a3b54de94c272599ad866bd`。17项包含真实双连接锁阻塞与提交后栅栏、真实provider第二gate后的三类结果保留、旧lease owner、duplicate/retry、legacy来源、版本隔离及两类晚期SQL回滚。外部provider为测试替身，不声称真实供应商发送。三个隔离run容器/卷/端口/进程组均回收。原始XML、日志、哈希及明确复用说明见 `T-40-current-2026-09-23/backend-a4/manifest.json`；有界A2安全审查不替代最终整票审查。

Dispatch01E：cors_audit作为唯一产品writer，在原22写集内完成宿主懒加载query订阅、本人分页外detail、query/session代际隔离、历史管理链接按本人messageId转换、撤回提示和两条Skill事实reference；补领域/SFC/宿主测试及独立T40真实浏览器驱动。浏览器验证真实HTTP发布→Worker送达→撤回后本人快照，A/B权限隔离与同页query；不得SQL伪造发布证据。Lead独占治理、提交、构建及服务，writer不运行构建/服务或提交。新增路径超范围须先登记。候选交回后冻结源码，再串行运行定向/全量门禁及真实Chrome；本checkpoint不是整票通过。

## revision185 — 前端定向通过，浏览器实施派单

revision185：T40前端分段F4在461b0a43通过112项定向测试及3包typecheck；F1 eb8cefe0真实后端18项全部通过、源码前后clean/cleanup[]，后端自F1未变。F1/F2/F3失败及诊断已保留。13done/2cancelled/T40in_progress/34ready，完整候选attempts0；真实Chrome、完整门禁及两条Skill事实尚待完成。

F1 `eb8cefe0f67895e75832b2a15d8f955b7b01e447` / tree `33ef52d0f1250c6221cb4ad3f586563d84332c67` 的真实run `c7fac20c13a862b2` 为18/0/0/0，新增首次READY MAIL未授权前撤回零外呼和provider ID保留断言。前端首次工具PATH错误127未启动测试；修正环境后111pass/1fail，3包typecheck通过。F2 snapshot正向通过而subscribe失败；F3临时诊断揭示Vitest并发懒导入加载真实App HTTP导致ClientContext缺失。F4仅在测试中等待已取消导入结束，再验证有效订阅，保留全部正负断言；生产未放宽，临时console未提交并已删除。当前 domain7 + webdomain25 + Admin80 =112全部通过，3包typecheck通过。结果/原始失败见 frontend-f1 与 frontend-f4 manifest。SFC缓存页检查调用实际组件注册的生命周期hook，不能冒称真实浏览器KeepAlive交互。

Dispatch01F：cors_audit唯一产品writer，在原22写集内继续独立T40真实Chrome两案例、owned runner及离线安全测试，更新两条已声明Skill reference。依据冻结设计与 `/tmp/wta-t40-browser-design.md`，真实HTTP发布→实际Worker送达→撤回后普通A快照可读，B foreign/absent拒绝、历史管理链接按本人消息导向、同页query与会话迟到隔离。控制HTTP允许，禁止伪造发布/详情响应；SQL fillers明确标识。Lead独占治理/提交/构建/服务；writer不得运行构建、测试服务或提交，超范围先登记。交回完整源码后冻结并串行执行剩余必需门禁。

## revision186 — C1门禁反馈与定向补修写集

revision186：T40完整候选C1 2c8047a2首次验收失败；前端652 Vitest＋108工具测试、全量检查与三App构建通过，浏览器离线15通过；后端默认测试中两项旧notice-published邮件夹具未触发send，待核查修复。完整candidate attempts1；full/core打包及真实Chrome尚未执行，13done/2cancelled/T40in_progress/34ready。

C1前端每步前后均同一clean HEAD/tree，补齐此前F4 clean:false来源限制。两份固定C1静态审查未发现生产合同阻断，但不能代替失败的默认测试与未运行浏览器。全部已执行记录、fresh XML及审查见complete-candidate-c1/manifest.json；token仅按精确JWT模式脱敏并保留原始哈希/替换数，其他字节保留。默认失败为DispatchNotificationServiceTest的recipientMinuteCapIsIsolatedByScene与noticePublishedMailRendersWrapperNotCallerSnapshot，均send未调用。Lead登记该测试文件作为第23写集，先定位并按新noticeVersion合同补齐夹具，保留两项原目的断言，必要时增加缺事实失败关闭负例；不得放宽生产栅栏。cors_audit唯一产品writer，Lead治理与全部服务/构建。

## revision187 — C2默认/构建通过，浏览器前置失败

revision187：T40完整C2 851a6d6e仍未验收；两旧邮件夹具修复＋缺marker零发送负例通过。默认首次OOM137失败保留，同源限定1536MiB重跑877执行通过/216环境skip；full/core打包及全部静态门禁0。真实browser前置探针或登录阶段失败，Chrome未启动，source/JAR一致且cleanup[]。attempts2；下一仅补安全阶段诊断，13done/2cancelled/T40in_progress/34ready。

C2树a6d6fe1e3c5c372372ceb34bae8a191d44c46d47。默认初次未设堆上限，kernel OOM杀死Java PID1142623，fresh237类1009项0fail/error/198skip只属未完成证据；随后同源码JAVA_TOOL_OPTIONS=-Xms128m -Xmx1536m重跑默认260类1093项0fail/error、216skip，877实际执行通过；不修改规则/排除测试，全部fresh XML均在clean前保存。C1前端与浏览器离线按输入完全相同且329产物hash一致复用；F1真实18/A3共享135保留原SHA和明确差异清单。C2 full JAR SHA a6cece4cdbe349f3529870ee39d2397c85477513da1d603c2d40d1eda2c369a1；run19a061800c7e3c0f真实隔离六SQL103表、3容器/2卷/5端口和backend进程全部回收，但固定RuntimeError尚不能区分auth/code探针或WTA/A/B登录失败，不猜根因、不计浏览器通过。core随后完成，target现为core，下一browser必须重新full打包。

Dispatch01G：cors_audit只改已授权frontend/e2e/run-notice-retraction-real.py及test_run_notice_retraction_real.py，补固定阶段白名单、HTTP/R.code受限整数、backend退出码及本脚本帧行号，不持久化任意异常串/HTTP正文/凭据。保留严格真实控制链路、两Chrome案例/0skip/0retry、owned清理与固定source/JAR。加入401/R401/缺token/探针退出/canary/伪阶段与数字边界离线检查。Lead独占所有服务/构建与提交；未定位事实前不改产品行为或放宽验收。
