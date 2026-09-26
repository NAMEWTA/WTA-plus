# T-50 实施提纲（只读，2026-09-23）

固定产品核查输入：`6e1d7f8e9f3d67495a361634779b7f7f050e68ab`；最新治理 HEAD `d544f02e1627d76883e9eeaf73a8f2b05002d079` 与该产品内容相同，核查后工作树仍清洁。T-39 C2 真实 8 类 118 项通过是上游既有证据，本轮未重跑。对照 `ticket/50-reject-unimplemented-notify-modes.md`、`/tmp/wta-t50-current-audit.md`、当前 Notify 源码及工程/全栈/模块/API 技能。旧 audit 的产品基线 `7a6f75a` 已过时，下述以当前源码为准。本文件仅为实施派单输入，未改仓库、未读生产数据、未构建或启动服务。

## 已证实的当前缺口和调用面

- `NotificationCommand` 仍保留 `strategy/mode/priority`；Java mode 枚举**只有 `ASYNC`**，strategy 枚举有 `ALL/ORDERED_FALLBACK/ESCALATION`，null 构造参数归一为 `ALL/ASYNC`。`NotificationApplicationRuntimeService.validate()` 目前未限制三字段，`submit()` 在 `findDuplicate` 前仅调用该校验，故不支持值仍可能返回同幂等键旧回执或写新 Intent/Delivery/Outbox。新增严格校验应位于 `findDuplicate` **之前**：仅 `strategy==ALL && mode==ASYNC && priority==0`，包括负优先级拒绝；保留 public record 构造器、枚举常量及 null 默认以兼容既有序列化/二进制形状。HTTP `SYNC` 字符串由枚举绑定失败，不凭空新增 Java SYNC 常量。
- 当前 `NotifyOutboxMapper.xml` 按 `outbox_id` claim，不使用 priority。`DispatchNotificationService.routeDecision()` 仍让旧 `ORDERED_FALLBACK/ESCALATION` 走 `WAIT`，`NotifyDispatchResultService.settle(WAIT)` 重排 `READY+30s`；旧 `mode`/priority 在 Worker 中没有承诺的语义。不能仅在提交端拒绝新值。
- 当前生产源码仍是 **12 处构造、10 个文件**，均已 `ALL/ASYNC` 且 `priority` 非零。归零位置：`AuthController` 20；`CaptchaController` SMS/MAIL 各 80；`MailSendController` 20、`SmsController` 20、`WebSocketController` 10；`NotifyNoticePublisherService` 50、`NotifyTestSendService` 20；`EnterpriseTransferService` 80；`PersonRebindNotificationService` IN_APP/SMS 各 60；`FlwCommonServiceImpl` 40。只改该参数，不改业务幂等键、目标、模板、权限或期限。T-39 的 Captcha 两渠道共用绝对 `expiresAt`/Redis 截止，Enterprise 现有挑战截止非空；PersonRebind 是安全提醒，**不臆造 OTP TTL**。
- 两处 T-39 正向夹具仍使用 80：`NotificationDeadlineRuntimeTest`、`NotifyDeadlineIntegrationTest`；迁移正向样本到 0，另增显式 80 负例，不放宽原时钟/expiry 断言。现有 caller 单测多为捕获命令，可增 `.priority()==0` 等真实参数断言。`NotificationModeOpenApiTest` 当前只断言 ASYNC 枚举，不能证明策略/优先级提交限制。

## 最小写集与实现顺序

1. 现有票写集已覆盖 `wta-api/.../notify/api/`、`wta-notify/.../service/runtime/`、上述 10 个生产文件、admin notify 与 notify 模块测试根、通知规范、OpenAPI 工具/生成目录。更新 `NotificationCommand` 参数/`NotificationApplicationService.submit` 与两个枚举的说明：保留历史枚举值供旧数据反序列化，**新提交仅 ALL/ASYNC/0**；不宣称 priority 有排序或 SYNC 有同步执行。`NotificationApplicationRuntimeService.retry()` 锁定原 Intent 后也要拒绝三字段不支持的历史任务（仅支持值才进入 T-38 requeue 判断）；否则旧 `FAILED`+安全本地码可先被重排、误写 wake。`query/cancel` 的历史查看与取消控制面仍可用，不应因为不能 retry 而屏蔽。先做行为红灯，再校验并迁移全部调用。真实 OpenAPI 用固定后端 JAR/live `/v3/api-docs` fetch→generate→check；不手改生成 TS/JSON，也不增加并不存在的 enum 值。
2. **历史处置无需预扩 Java 路径**：在已授权 `NotifyDispatchResultService.deadlineGateLocked()` 的同一短事务、锁序和活租约 fence 内加 unsupported gate，并沿用已有 `NotifyDispatchResultPort.deadlineGate()`/`NotifyDispatchResultUseCase`。这是比新 port API 更小的实现：当前 gate 已由 `DispatchNotificationService` 在路由之前、Provider 入口之前调用，亦由 IN_APP 的 begin/complete 与 settle 调用。新分支应放在现有 T-39 reclaimed/terminal/到期判断之后、scheduled WAIT 判断之前；直接判锁后 Intent 的 `strategy/mode/priority`，不得以先前普通读快照决定。它返回 false，避免 routeDecision/planChannel/NotifyClient/InApp persist。此方案不需 port、UseCase、DAO、Mapper/XML、Entity、DDL 新写集；若实施时改成独立 `unsupportedGate` API，必须**先**补 `port/NotifyDispatchResultPort.java` 和 `usecase/NotifyDispatchResultUseCase.java` 精确写集并重新审锁序，不得先写。
3. 对仍为 `PENDING` 的旧外部投递，只在**本次从 READY 领取、持有 T-39 `DEADLINE_UNSENT_READY` 标记、Outbox/Delivery 均 0 次、无 providerMessageId/acceptedAt/deliveredAt 等相反事实**时，以固定安全码 `UNSUPPORTED_MODE_UNSENT` 原子写 `FAILED/DONE`；此码不加入 T-38 人工安全重试白名单。无标记、旧 PROCESSING 重领、已有尝试/供应商事实或来源缺失时，用固定 `UNSUPPORTED_MODE_OUTCOME_UNKNOWN` 原子写 `UNKNOWN/WAITING_RECEIPT`，交只读核对，**不能重发、清 Redis 幂等键或伪称未发送**。历史 `ACCEPTED/UNKNOWN/DELIVERED` 不降级；已有非 PENDING 出口只关闭可关闭的 Outbox。所有写回使用现有 `saveDeliveryResult`、`finishOutbox` 行数检查和 `refreshAggregate`，不插伪 Provider Attempt；失败整笔回滚。IN_APP 依 T-36 原子本地事实：本人关系和消息均存在则恢复 DELIVERED、无新 push；关系孤儿为固定本地失败；无本人关系则确定未投递，`FAILED/DONE`，**不**进入外部 WAITING_RECEIPT，也不新增消息或预算。CANCELLED/DELIVERED/EXPIRED Intent 及过期租约沿用 T-39 先行规则，不复活终态。
4. T-39 deadline 优先：过期 READY+新 marker+零结果可由现有 gate 先给 `NOTIFICATION_EXPIRED/DONE`；过期旧无标记或 PROCESSING 重领仍 `UNKNOWN/WAITING_RECEIPT`；未过期但不支持的策略/模式/priority 走上述 unsupported 处置。Provider 已进入后不再用 mode/expiry 抹去其真实 ACCEPTED/UNKNOWN；正常 `ALL/ASYNC/0` 仍按 T-39 原计划/截止与 T-37/T-38 幂等/重试合同执行。上线前停旧 Worker，避免 pre-T39 代码与新 marker 工作混跑。
5. **需要预登记的额外路径：无，前提是复用现有 gate**。若 Lead 要求把只读存量清单做成仓内永久工具/新管理端点，先单独登记该工具/Controller/权限/测试路径；本票可先用获准环境中的只读 SQL 与 `/tmp` 审核记录，不需要改生产 schema 或新增状态平台。不能在本次无服务授权时执行存量查询。

## 历史数据清单与受控处置

只读清单按 `strategy<=>ALL / mode<=>ASYNC / priority<=>0` 的反面筛选，按 Intent ID、渠道、Outbox 与 Delivery 状态、尝试数、截止、标记有无、providerMessageId **有无**、claim 来源是否可证明分组计数；不要导出正文、模板参数、地址、手机号、供应商 ID 值或密钥。注意 `claimedFromReady` 是领取时内存标记，不能从历史静态表推断；只读清单只能把“可能安全”列为待运行时锁后核实。活租约、WAITING_RECEIPT、已经受理、完成和无 Outbox 的 Intent 分开，不能一条 UPDATE 扫表。实施后由 Worker 逐条持锁/fence 收敛可领取的旧任务至 DONE 或 WAITING_RECEIPT；不可领取的未知/受理任务保留原事实、标入人工核对清单。存量是否存在未知，不能提前声称为零，也不对真实数据直接批量迁移。

## 行为红灯与验收矩阵（实施阶段才运行）

| 接缝 | 正反断言 |
|---|---|
| 真实 `NotificationApplicationService.submit/retry` + MySQL | `ALL/ASYNC/0` 正向；ORDERED_FALLBACK、ESCALATION、`priority=1/-1`，含同 app/key 已存在的重复提交，均在任何新持久写入前拒绝；Intent/Recipient/Delivery/Outbox/Attempt 行数不增。已有不支持 Intent 的 `FAILED`+本地安全错误码走 retry 仍明确拒绝，0 requeue/0 wake/不重置预算；query/cancel 原控制面可用。null strategy/mode 归一化仍有效。 |
| 授权 HTTP POST `/notify/notification` | 有权限正向成功；不支持策略/非零优先级明确业务失败且零写；未知 mode 字符串 `SYNC` 绑定失败且零写；无权限请求仍拒绝。StandaloneMockMvc 未装安全拦截器不能冒充鉴权实证。 |
| 12 个生产调用点 | Captcha SMS/MAIL、Auth、Demo 3、Notice、test-send、Enterprise、Person 两支、Workflow 捕获实际 `NotificationCommand.priority()==0`；保留原 `strategy/mode`、稳定幂等键、模板/接收人及既有时效，至少按业务类别做真实提交闭环。 |
| 六 SQL 隔离 MySQL/Redis 的旧策略任务 | 构造以前会 `WAIT` 的前驱状态、旧 mode 字符串与非零 priority；新 marker/无结果给 FAILED/DONE，缺 marker/重领 PROCESSING 给 UNKNOWN/WAITING，供应商调用数 0，不再 `READY+30s`；重复唤醒不产生第二个 Attempt/消息。 |
| 已受理/未知、本地事实与竞争 | `ACCEPTED/UNKNOWN` 外部不倒退/不重发；IN_APP 关系+消息恢复一次且 0 push，孤儿固定本地失败，零关系无消息 0 persist；旧 owner 迟到、lease 过期、锁竞争、末尾写入故障均保持 fence 和整笔回滚。T-38 retry 对 `UNSUPPORTED_MODE_*` 返回 0，不清预算/幂等键。 |
| T-39 交叉 | 截止前/恰到截止、scheduled future、过期 READY marker 与过期旧 PROCESSING、外部 Provider 已进入跨截止，确认原有 deadline 优先级和真实受理结果未被 unsupported gate 覆写。 |

现成开发接缝包括 `NotificationDeadlineRuntimeTest`、`DispatchNotificationServiceTest`、`NotifyAtomicResultIntegrationTest`、`NotifyDeadlineIntegrationTest`、`NotifyWakeIntegrationTest`、`NotificationModeOpenApiTest` 与 `backend/wta-admin/src/test/java/org/namewta/test/notify/caller/`；新增真实类可置已授权 admin notify 测试根，须提供 opt-in 开关、仅环境传密码、隔离六 SQL 与 0 skip 实际计数。基础命令以 cwd=`backend/` 的 `./mvnw -pl wta-modules/wta-notify,wta-admin -am test` 起，再覆盖 Demo/Workflow/Profile 受影响消费者及固定 JAR 的真实 HTTP/MySQL/Redis；仓根 layered gate、Skill facts、正确 pnpm 10 的 OpenAPI check 与适用前端门禁按最终 diff 执行。当前本轮**没有运行**这些命令，也没有声称旧任务已处置。

实现前留意：`NotificationDeadlineRuntimeTest`/`NotifyDeadlineIntegrationTest` 的 80 是旧正向夹具，若校验上线会失败；该失败不能靠删除 T-39 deadline 断言解决。公开 strategy 枚举留存意味着 OpenAPI 类型仍显示旧值，必须在描述与 HTTP 负向合同说明当前支持子集，生成快照只能从真实 live source 更新。上线真实存量处置与人工核对需独立审批和证据，不由本只读提纲授权。
