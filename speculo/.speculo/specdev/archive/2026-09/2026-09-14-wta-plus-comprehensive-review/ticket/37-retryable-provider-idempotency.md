---
schema_version: 3
plan_contract_version: 1
skill_scan: "2026-09-23已枚举.agents/skills全部入口；命中scope读取入口，common入口变化已复核，历史Skill Evidence不改写"
skill_bindings: [{"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>"], "outputs": ["T-37的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-common-modules-guide", "path": "<Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>", "sha256": "d7b7e105c37499e0e0f8ad9b1e2dbf379100df5b6a47e0d6affb04483f8b162c", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>"], "outputs": ["T-37的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "wta-module-guide", "path": "<Path>.agents/skills/wta-module-guide/SKILL.md</Path>", "sha256": "bb57a781314abe316f06ba9538f62043f1bd053904c0832969e575c09731a7a3", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>"], "outputs": ["T-37的边界/调用方/持久化与实现检查记录"], "required": true, "on_failure": "block-ticket"}, {"id": "engineering-standards", "path": "<Path>.agents/skills/engineering-standards/SKILL.md</Path>", "sha256": "dbc475149e3588cb840c15e0bb287920b876c0eef4c4004dde078de1c4fca6d9", "phase": "verify", "operation": "verify-affected-contract-and-quality-gates", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path>", "当前diff与验证矩阵"], "outputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-37-replan-2026-09-23.md</Path>"], "required": true, "on_failure": "block-ticket"}, {"id": "java-api-compatibility", "path": "<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>", "sha256": "b90f5592e75b3f757f52649a16f78e92850fee0ccac9f12619d3d7aa94bd7aca", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>"], "outputs": ["T-37公共结果/Redis状态序列化及真实消费者迁移记录"], "required": true, "on_failure": "block-ticket"}, {"id": "namewta-fullstack-development", "path": "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "sha256": "675c053c11d8b22cd394c875f48688242d8e8328dd14e69657f5d5b12f2af68b", "phase": "implement", "operation": "apply-scope-contract", "inputs": ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ticket/37-retryable-provider-idempotency.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>"], "outputs": ["T-37公共结果/Redis状态序列化及真实消费者迁移记录"], "required": true, "on_failure": "block-ticket"}]
resource_claims: ["workspace:current-exclusive", "notify:dispatch", "redis:notify-idempotency", "contract:AC-037"]
artifact: "ticket"
change: "2026-09-14-wta-plus-comprehensive-review"
id: "T-37"
title: "让 Outbox 重试真正重新调用可重试供应商"
status: "done"
kind: "bug"
planning_depth: "deep"
planning_depth_reason: "公共合同/事务/安全/数据及恢复边界"
ready: true
risk: "high"
blocked_by: []
contract_ids: ["AC-037"]
owner: "single-agent"
expected_changes: ["<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/exception/NotifyIdempotencyUnavailableException.java</Path>", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/</Path>", "<Path>backend/wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/notify/MailNotifyChannelAdapter.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/provider/Sms4jBlendRegistry.java</Path>", "<Path>backend/wta-common/wta-common-sms/src/test/</Path>", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>"]
writable_paths: ["<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/exception/NotifyIdempotencyUnavailableException.java</Path>", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/</Path>", "<Path>backend/wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/notify/MailNotifyChannelAdapter.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/provider/Sms4jBlendRegistry.java</Path>", "<Path>backend/wta-common/wta-common-sms/src/test/</Path>", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>"]
read_only_paths: ["<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>", "<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/ADR.md</Path>", "<Path>{roots.state}/specdev/adr/</Path>", "<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/</Path>"]
shared_paths: ["<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>", "<Path>backend/wta-modules/wta-notify/src/test/</Path>", "<Path>.agents/skills/engineering-standards/references/notification.md</Path>", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/exception/NotifyIdempotencyUnavailableException.java</Path>", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/</Path>", "<Path>backend/wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/notify/MailNotifyChannelAdapter.java</Path>", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/provider/Sms4jBlendRegistry.java</Path>", "<Path>backend/wta-common/wta-common-sms/src/test/</Path>", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>"]
shared_path_owners: ["<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-modules/wta-notify/src/test/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>.agents/skills/engineering-standards/references/notification.md</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/exception/NotifyIdempotencyUnavailableException.java</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/notify/MailNotifyChannelAdapter.java</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/provider/Sms4jBlendRegistry.java</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-sms/src/test/</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)", "<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path> => single-agent (Lead; exclusive current workspace; T-37 turn only; delegated sole product writer cors_audit)"]
---

# T-37：让 Outbox 重试真正重新调用可重试供应商

Map：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/tickets-map.md</Path>；Spec：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md</Path>；Goal：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/goal-plan.md</Path>。
唯一执行者先完整读Map→命中项目Skill入口/按scope引用→本票与上游。用户已激活Goal并允许gpt-6-sol/xhigh原生子代理；current单产品writer cors_audit，无新worktree；Lead治理/提交/真实环境验收。

## 1. 战略与来源

- 来源：R64-N-04；AC-037；本轮用户请求与<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/source.md</Path>。
- 当前事实：common Dispatcher 无差别 complete，Redis 失败结果 COMPLETED 被重复复用；delivery ID 同时作 request/idempotency key。
- 可观察产出：Outbox 独占重试次数与节奏；明确未发送的可重试失败允许新一次物理发送，ACCEPTED/DELIVERED/UNKNOWN 保留防重。

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

Outbox 独占重试次数与节奏；明确未发送的可重试失败允许新一次物理发送，ACCEPTED/DELIVERED/UNKNOWN 保留防重。 正常、失败、越权和竞争路径按本票验收断言共同交付，不把前后端或测试分成无价值空票。

## 5. 实现契约

- 入口与输入输出：NotifyIdempotencyDispatcherUnitTest、RedisNotifyIdempotencyStoreIntegrationTest、真实 runtime/Dispatcher 组合。
- 外部行为：Outbox 独占重试次数与节奏；明确未发送的可重试失败允许新一次物理发送，ACCEPTED/DELIVERED/UNKNOWN 保留防重。
- 不变量：current workspace单writer；UseCase→Service→DAO→Mapper；classic保留；公开数据只经wta-api；GET查询/POST变更且安全@Log。Notify外部I/O不在结果事务内，IN_APP按确认后的短事务合同处理。
- 失败边界：不吞SQL/HTTP/Provider错误，不将UNKNOWN当成功或盲目可重试；页面旧响应不覆盖新会话。具体负向断言见第8/10节。
- 兼容：沿用用户此前明确的基座仓内直接切换决定，同步真实消费者/生成物；不放宽第三方协议。公共API技能用于调用方与影响核对，不重新增加已被用户排除的兼容桥。
- 安全：仅隔离合成测试；secret不进日志/截图/证据。对象/Client授权在后端实施，页面隐藏不替代权限。

## 6. 执行路线

1. 真实 Redis store＋Dispatcher＋Outbox 场景固定拒绝→接受的两次 Provider 调用红灯。
2. 定义确定未发送、终结失败、已接受、未知的结果分类；仅确定可重试且持有 owner 的 claim 可释放。
3. 保留 digest 冲突、IN_PROGRESS、Redis 故障失败关闭；未知网络异常不得一刀切释放。
4. 检查附件快照失败及 complete 写 Redis 失败边界，结果不得伪装送达；成功重复消费无二次物理发送。
5. 同步 common-notify 合同与消费者回归，记录对永久 ADR-0007 完成态复用规则的局部替代，永久知识留 A。

## 7. 路径访问契约

frontmatter为预计点、硬写集与共享owner权威。目录写集仅授权本票行为所需文件；新增测试在声明根内，新增生产类须符合已有层次。不存在的新文件为计划创建，不声称已实现。共享物理/语义资源由single-agent在本票轮次独占；不同票不并行，跨change冲突仅暂停相关分支。

本票状态和Evidence仅由Lead写当前change；永久ADR/context及相邻SSO change只读。越界先修订Ticket/Map，禁止先改后报。

## 8. 验证矩阵

| 场景 | 接缝/步骤 | 预期 | Evidence |
|---|---|---|---|
| 正常 | NotifyIdempotencyDispatcherUnitTest、RedisNotifyIdempotencyStoreIntegrationTest、真实 runtime/Dispatcher 组合 | 拒绝且可重试→接受产生两次实际发送，非两次缓存异常 | <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-37-replan-2026-09-23.md</Path> |
| 失败/竞争 | 已接受重复及响应丢失 UNKNOWN 不盲目重发 | 明确失败/安全恢复，无伪成功、越权及部分提交 | 同上，记录故障注入与状态 |
| 回归 | 现有同域测试＋消费者＋适用静态门禁 | Redis complete 故障、过期 owner 和不同 digest 保持可解释且安全 | 同上，记录测试数/skip/源码 |

命令在仓根执行，`cd backend`表示该条命令切cwd；每条独立运行。以下由当前Evidence的精确受影响选集及真实隔离命令落实；完整argv/计数见Evidence：

- 当前C2：backend Maven39类187项、真实SMS10/Redis3、Atomic66/Wake1全部零skip；完整后端最终组合门禁归T30。

- Workspace checks：current-workspace，所列命令加命中工程Skill质量门禁。
- E2E disposition：required: NotifyIdempotencyDispatcherUnitTest、RedisNotifyIdempotencyStoreIntegrationTest、真实 runtime/Dispatcher 组合。
- E2E owner/environment：single-agent（Lead）/current-workspace；真实MySQL/Redis/MinIO必须为本任务隔离资源，必要服务缺失则阻塞对应验收。
- 真实服务启用方法与零skip要求：<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/verification.md</Path>；新增用例必须保存精确选择器与实际计数，不能只运行mock或test list。
- Integration evidence：非空implementation commit、parent before、clean exact HEAD/tree时点的direct-parent和适用E2E、不可变result及父链；required模式不适用，不创建candidate worktree。

## 9. 发布、迁移与恢复

- 迁移顺序：不得清空 Redis 幂等数据。旧 COMPLETED 失败项按结果分类与窗口处理；未知发送事实保留人工核对入口。
- 兼容窗口：基座仓内同步切换，无未声明双写/双协议；外部现有协议保持。
- 监控：记录本票可观察失败/状态/耗时及资源数量，不记录敏感正文；不新增监控平台。
- 恢复：保存上个不可变候选及失败证据；停止受影响任务再核对外部副作用。不得通过恢复已披露secret、放宽权限或重发UNKNOWN恢复。
- 不可逆批准：产品commit/父分支更新及远程push、部署、轮换/真实数据操作、归档分别核对本轮授权。已授权本地实施、逐票提交与direct-parent；推送、部署、真实数据修改及归档不由此推断。
- 收缩条件：旧消费者/废弃字段/不必要配置引用清零且新合同验证通过；不适用的删除不人为增加。

## 10. 验收标准

- [x] `AC-037`：拒绝且可重试→接受产生两次实际发送，非两次缓存异常。
- [x] `AC-037`：已接受重复及响应丢失 UNKNOWN 不盲目重发。
- [x] `AC-037`：Redis complete 故障、过期 owner 和不同 digest 保持可解释且安全。
- [x] 实际调用已绑定Skill，记录摘要/输入/步骤/输出；不是只“读过”。
- [x] 正常、失败、回归和required E2E有当前候选证据，未运行不勾选。
- [x] 写集、共享owner、合同和生成物一致；无未批准偏差。
- [x] 真实commit/direct-parent/result出口已满足或按Goal对历史无需新实施票作有证据的取消裁决。

## 11. SKILL 调用计划

frontmatter每个必需绑定在implement阶段输入本票、真实调用方和diff，按scope执行约束检查与实现；verify阶段由engineering-standards执行适用门禁。实际Skill Execution Records写<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/T-37-replan-2026-09-23.md</Path>，包含id/phase/operation/sha256/status/evidence。入口摘要变化先读diff并重新绑定，不改旧历史记录。当前规划仅完成元数据/入口及相关规范路由，未伪造实施passed。

## 12. 停止、检查点与交付

交付本票完整可观察行为，数量以Map为准，不能以样例替代。缺高影响决定、必需Skill/引用/测试，或owner冲突，停止该票和依赖闭包；无依赖票仅在已获执行授权后继续。保留HEAD、diff、已跑命令、失败类别、待完成动作；相同失败无新证据或达到3次集成尝试先复盘。验收后回交Goal，全部票done仍不等于change可归档。

## revision158 当前实施与合同细化

base `38032d24335c52cafea855b19d51fb36295162ef`；cors_audit唯一产品writer，Lead独占治理、commit、服务与E2E。自动Outbox/common覆盖本票；人工retry的UNKNOWN拒绝与精确ID归T38，最终全部入口由T30汇合，不用Redis TTL冒充持久exactly-once。

结果采用机器可判定的未发送可重试、未发送终结、已接受、结果未知事实；旧FAILED/PROVIDER_REJECTED和任何未明确分类异常均无重发权。仅全部目标明确未发送且可重试，当前owner才能CAS为RETRYABLE；保留digest/请求身份及剩余TTL，同requestId每次独立nonce防ABA。新claim仅同digest CAS可重取；旧owner不能complete/release/转态新claim；转态失败/ACK未知失败关闭，不删键或全清缓存。已接受/UNKNOWN保留防重，混合结果不得整批释放。外部SMS与MAIL调用异常均保守UNKNOWN；T35 ACQUIRE前零发送准备路径保持。

生产来源候选为受控单次请求的腾讯单号码30秒限频结构化拒绝；严格校验供应商、响应类型、单匹配号码、固定Code、RequestId、无Error/SerialNo及Fee=0，其他类别不放入allowlist。具体SDK单次请求来源须由源码/API事实证明；不能证明的blend保持未知，不新增旁路安全注册平台。Sms4jBlendRegistry明确关闭SDK内部无差别重试。供应商实际联调不在本票，官方响应语义支持的生产解析器与合成响应跨层验收须分开说明。MAIL附件错误未有暂态类型证明前不整类标为可重试；附件完整合同仍归T42。

新增写集在编辑前登记：common幂等异常阶段、SMS notify目录/本地测试、MAIL Adapter和SMS4J Registry；公共模型/store/receipt签名与序列化消费者按JavaAPI技能同步，仓内直接切换，不恢复兼容桥。notification.md在本票写集，必须同步保守重试与旧缓存处置事实；永久ADR不改。

先可观察失败测试再最小实现；真实Redis检验owner CAS/20并发/digest/TTL/旧状态/损坏，真实MySQL+Redis+Dispatcher+Adapter+Outbox验证拒绝后两次物理调用、真实重新claim与到期退避、接受与未知零重发、COMPLETE/转态故障、租约fence。无skip冒充验收；T35/T36回归按受影响输入执行。禁止全缓存删除、改生产凭据或实际厂商发送。

## revision159 单次SDK请求的实例事实闭环

编辑前新增精确写集：<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>。在现有Sms4jBlendRegistry维护自身以maxRetries=0创建的实际代理实例identity；使用已验证的BaseProviderFactory.createSms→SmsProxyFactory.getProxySmsBlend→SmsFactory.register同一引用，保留原SDK初始化所需钩子，不将void create后再get的可覆盖对象盲认证。remove先撤认证；注册/更新失败不保留认证，按账号并发更新需一致，不暴露配置对象。common-sms notify目录内小型SmsSingleAttemptBlendVerifier SPI由Registry实现，Resolver经AutoConfiguration ObjectProvider注入，缺失/identity不匹配即不把拒绝标成可重试；严格腾讯结构allowlist只有该证明成立才启用。已有Registry拥有这一事实，不新增第二套全局注册平台，不反射SDK，不让common反向依赖业务。固定对象捕获到send，避免查验A发送B。公共SPI/构造/配置方法调用者与测试同步。

### revision159 Redis序列化与剩余TTL实证

生产RedisConfig默认CompositeCodec(StringCodec,TypedJsonJackson3Codec)，旧Store bucket采用client默认codec；SMS真实fixture默认codec，Redis专项fixture显式StringCodec。新单键脚本必须沿用旧值的codec及NameMapper，不能改成StringCodec后让旧key不可读/比较失败。Redisson4.6.1源码的CompareAndSetArgs不指定TTL会SET并清TTL，不能作为保留期限实现；使用与bucket一致编码的原子CAS+PTTL/KEEPTTL，在同脚本内验证key存在且有正剩余TTL。真实回归需涵盖项目CompositeCodec及名称前缀、旧四字段StoredState，不只StringCodec绿色。

## revision160 已验收

最终source/result `3a87bf71876d92e4afdd227de156045226e52be2`，formal attempts=2，详见evidence/T-37.md。人工retry仍由T38闭合；下一T02优先修复真实token路径日志泄漏。
