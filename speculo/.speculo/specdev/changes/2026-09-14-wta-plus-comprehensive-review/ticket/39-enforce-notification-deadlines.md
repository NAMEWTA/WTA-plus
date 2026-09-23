---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/39-enforce-notification-deadlines.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-39的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/39-enforce-notification-deadlines.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-39的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/39-enforce-notification-deadlines.md</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>"], "outputs": ["T-39的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/39-enforce-notification-deadlines.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-39-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path>"], "outputs": ["T-39 absolute expiry common API/codec/NameMapper and caller verification"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path>"], "outputs": ["T-39 absolute expiry common API/codec/NameMapper and caller verification"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:dispatch", "notify:deadline", "contract:AC-039"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-39"
title: "让验证码与通知截止时间在发送前生效"
status: "in_progress"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: ["T-38"]
contract_ids: ["AC-039"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/test/java/org/namewta/common/redis/utils/RedisUtilsDeadlineIntegrationTest.java</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/other-utils.md</Path>"]
writable_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/test/java/org/namewta/common/redis/utils/RedisUtilsDeadlineIntegrationTest.java</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/other-utils.md</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java</Path>", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path>", "<Path>backend/wta-common/wta-common-redis/src/test/java/org/namewta/common/redis/utils/RedisUtilsDeadlineIntegrationTest.java</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>.agents/skills/wta-common-modules-guide/references/other-utils.md</Path>"]
shared_path_owners: ["<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/profile/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/test/</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/utils/RedisUtils.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>backend/wta-common/wta-common-redis/src/test/java/org/namewta/common/redis/utils/RedisUtilsDeadlineIntegrationTest.java</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>.agents/skills/engineering-standards/references/notification.md</Path> => cors_audit (sole product writer; Lead governance/integration)", "<Path>.agents/skills/wta-common-modules-guide/references/other-utils.md</Path> => cors_audit (sole product writer; Lead governance/integration)"]
---

# T-39：让验证码与通知截止时间在发送前生效

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
用户已激活Goal并授权native gpt-6-sol/xhigh协作、本地commit/direct-parent。base bec94ae442966864a74ff52ffb3e4e305a37441d；cors_audit唯一产品writer，Lead治理/提交/服务/验收，ops/legacy只读及私有驱动。无新worktree。

## 1. 战略与来源

- 来源：R64-N-06-time；AC-039；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：expiresAt 只检查相对 scheduledAt；领取和 dispatch 不按到期阻止 I/O；Captcha command 传 null，EnterpriseTransfer 已传 expiresAt。
- 可观察产出：统一截止时刻驱动验证码 TTL 与 command；提交/重试拒绝到期，Worker 发请求前检查，过期任务结束且 Provider 调用为零。

## 2. 决策状态

### 已锁定决策

保留工程分层、Client/权限、资源owner、安全日志、真实供应商协议和唯一六SQL基座。本地实施、commit/direct-parent已获Goal授权；远程与真实数据副作用不推定授权。

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

统一截止时刻驱动验证码 TTL 与 command；提交/重试拒绝到期，Worker 发请求前检查，过期任务结束且 Provider 调用为零。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：可控时钟 runtime/worker、CaptchaNotifyCallerUnitTest、真实 DB 过期任务终结。
- 外部行为：统一截止时刻驱动验证码 TTL 与 command；提交/重试拒绝到期，Worker 发请求前检查，过期任务结束且 Provider 调用为零。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 用可控 Clock/数据库时间边界测试 scheduled、expiry、队列延迟和重试不续期。
2. Captcha 一次计算绝对截止并原子设置缓存到期；保留 EnterpriseTransfer 现有挑战截止与旧码拒绝。PersonRebind是安全告知而非OTP，不人为施加两分钟期限。
3. 提交与 retry 检查 now>=expiresAt；发送前最终重检并通过现有结果事务记录明确过期失败、关闭 outbox。
4. 避免领取 SQL 排除过期后永久残留 READY：过期项必须可被终结；WAIT 分支也检查截止。
5. 编写无 expiresAt 存量验证码按场景/创建时间的只读处置稿，禁止盲目补未来到期或批量重发。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | 可控时钟 runtime/worker、CaptchaNotifyCallerUnitTest、真实 DB 过期任务终结 | 未来消息不早发；now>=expiresAt 不调用供应商 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-39-replan-2026-09-23.md</Path> |
| 失败/竞争 | 验证码 TTL 与 command 截止一致；重试不延长有效期 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | 过期 Outbox 可终結，不永久 READY/WAIT；转移/绑定确认仍拒绝旧码 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下为实施期命令，本轮未执行：

- `cd backend && ./mvnw -pl wta-modules/wta-notify,wta-admin -am test`

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: 可控时钟 runtime/worker、CaptchaNotifyCallerUnitTest、真实 DB 过期任务终结。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：无截止旧验证码先列清单；真实数据处置单独批准，不能通过重发恢复过期验证码。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。本地实施commit/direct-parent已授权；远程push、部署、真实数据修复、归档另行核对。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [ ] `AC-039`：未来消息不早发；now>=expiresAt 不调用供应商。
- [ ] `AC-039`：验证码 TTL 与 command 截止一致；重试不延长有效期。
- [ ] `AC-039`：过期 Outbox 可终結，不永久 READY/WAIT；转移/绑定确认仍拒绝旧码。
- [ ] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [ ] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [ ] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [ ] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-39-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision167 当前实施合同

当前base bec94ae，T38 done；产品以其最终retry/owner/budget/CAS为准。Notify的DATETIME为秒精度：scheduledAt向上取整、expiresAt向下取整，校验归整后区间以及dbNow<expiry；等于截止视为到期。null保持普通无截止通知。重复幂等与retry读取原持久截止，不能由新命令续期。未来availableAt不早发，过期READY仍可claim并被收敛。

Captcha在提交前算一次整秒绝对截止；command与Redis缓存使用同一时刻。公共RedisUtils新增独立setCacheObjectUntil入口，沿生产codec/NameMapper单键Lua以Redis TIME检查后SET PXAT；保留旧方法。缓存失败/耗尽明确失败，不把提交后的完整TTL重新计时；MySQL提交与Redis缓存不承诺跨资源原子。PersonRebind维持安全通知语义，Enterprise原挑战截止不延长。

结果端口以现有DSTransactional短事务、Intent→Outbox→Delivery锁序、数据库时钟与有效owner/token/lease收敛：确证未发且过期PENDING变FAILED/NOTIFICATION_EXPIRED、Outbox DONE、聚合同时提交，无新Attempt伪称I/O，任一行数异常回滚。IN_APP在预留预算与persist前重检，无消息/关系/push，已预留预算不能重置。WAIT同样复查；取消/终态不得开始新I/O。已进入供应商的结果不能被截止反推为未发。

claim保留领取前READY/PROCESSING的非持久证据，并在dispatch重读时保留本次provenance；它不能替代持久fence。外部READY还需初次零attempt或仅明确未发送历史码证明，不能把任意FAILED当未发；重领PROCESSING/缺失证据保守UNKNOWN/WAITING_RECEIPT且不再次外呼，防止旧worker已发送却崩溃未写结果。先前ACCEPTED/UNKNOWN保持真实事实。具体判定由安全review及真实crash/fence用例证明，不能只以PENDING推断。

先在现有CaptchaNotifyCallerUnitTest用旧API断言非空expiresAt，捕获可编译行为红灯，再进入完整实现。新增真实NotifyDeadlineIntegrationTest（notify.deadline.integration）、RedisUtilsDeadlineIntegrationTest（notify.deadline.redis.integration，类级稳定客户端/独立fork），连同EnterpriseQueuedNotificationIntegrationTest与NotifyWakeIntegrationTest精确启用零skip；T36/T37/T38受影响回归另验。只用任务owned MySQL/Redis合成库，驱动v2先冻结/合成自检。旧无截止验证码只给分类只读处置稿，真实数据不修复或重发。

正式派单见evidence/dispatch-T-39-20260923-01.md；新增port/entity/common精确路径及规范已在动手前登记。Mapper/XML/policy/DDL未扩权；需要时先报Lead。变更不涉及HTTP结构/前端生成物，若实际触及则先修订写集与门禁。
