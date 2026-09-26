# T-37 候选方案预审（只读，2026-09-23）

**输入固定点**：产品 commit `54cf715c2829ff95cf994401ff52f02668c1e171`；Ticket `37-retryable-provider-idempotency.md`、Spec AC-037、ADR-CR-019、`/tmp/wta-t37-audit.md`。本报告没有改仓库、运行 Maven/Docker/服务或读取私有值。T-36 writer 的工作树变化均不作为本报告事实。

## 结论与生产事实

目前 T-37 **需要实现**。`NotifyDispatcher.send` 在 `NotifyIdempotencyStore.Completed` 时直接复用结果；无论失败性质，`complete` 都把结果写为 `COMPLETED`（Dispatcher 73–86、98–114、192–202；Redis store 27–66）。同一 delivery ID 是 request ID 与幂等 key（`DispatchNotificationService` 106–116）；Outbox 对可重试失败转回 READY/PENDING，下一次却只得到缓存的旧异常。旧单测 `NotifyIdempotencyDispatcherUnitTest.shouldReuseFailedResultWithoutCallingProviderAgain`（145–176）固定了这个缺陷。

**可以证明零外部发送的现有生产边界**：SMS planner/配额/请求装配发生在 `notifyClient.send` 前；`DispatchNotificationService` 169–190 把 SMS 该阶段的暂态故障与幂等 `ACQUIRE` 故障记为 `PREPARATION_RETRYABLE`，`NotifyDispatchResultService` 151–164 有界退避。这些故障尚未取得 common Redis claim，故不是 `IN_PROGRESS→RETRYABLE` 的例证。common 层 `NotifyDispatcher` 73–78 在 Adapter 前取得 claim；MAIL Adapter 的本地附件物化（`MailNotifyChannelAdapter` 48–59、92–107）在 `sender.send` 前，可证明邮件未发送；其 I/O 故障是否暂态仍需按具体原因分类，当前抛 `NotifyAttachmentSnapshotException`，并非既有可重试 typed 结果。SMS4J 的参数校验在 `blend.sendMessage` 前，但属于终结输入错误。

**目前没有可信的“供应商拒绝且明确未受理、并可重试”的生产 typed 来源。** SMS4J Resolver 37–49 将一切 `!response.isSuccess()` 合并为 `PROVIDER_REJECTED`，`SmsNotificationReceipt` 只有 success 布尔；SMS Adapter 38–52 再把所有异常压为 `PROVIDER_ERROR`。MAIL Adapter 68–85 对 `sender.send` 后的异常也只给 `PROVIDER_ERROR`。这些失败可能已被上游接收，不能靠 `FAILED`、`PROVIDER_REJECTED` 或错误正文重发。T-37 可先以合成 sender 的显式“未受理且可重试”结果证明状态机和完整跨层调用次数；若要声明真实 SMS/MAIL 供应商拒绝具备自动重试能力，须在适配器以供应商**明确、可核验**的未受理响应类别作 allowlist typed 映射并做相应测试。没有这种证据时，生产拒绝保持 UNKNOWN/保守终结，不伪造 AC 的生产覆盖。MAIL 附件本地暂态失败可另作真实生产零发送 typed 来源，但它不满足“两次实际 Provider 调用”子断言，应分别取证。

## 最小 typed 结果及转译

建议复用 `NotifyTargetResult.status` 字段，扩展公开 `NotifyDeliveryStatus` 为清晰的 `UNSENT_RETRYABLE`、`UNSENT_TERMINAL`、`OUTCOME_UNKNOWN`（现有 `ACCEPTED` 保持）。旧 `FAILED` 仅解释为**未分类/未知**，绝不因其 errorCode 字符串获重试权。使用明确 factory 和固定、无供应商正文的错误码；不改变 `NotifyTargetResult` record 的六参数构造器以免扩大源码/二进制影响。Adapter 的每目标结果不能省略或错序，Dispatcher 验证 `validateAdapterResult` 时拒绝矛盾的状态/字段组合。只有**全部目标**都显式 `UNSENT_RETRYABLE` 且零 ACCEPTED/UNKNOWN 时，Dispatcher 可进行 RETRYABLE 转换；混合目标不得释放整批 claim，因为再次发送会重复已受理目标。单目标 Outbox 可无歧义消费该状态。

`NotifyResult` 保留现有聚合状态与 `NotifyDeliveryException` 入口；runtime 在捕获异常时检查**目标 typed status**而非 `result.status=FAILED` 或自由文本。单目标 `UNSENT_RETRYABLE` → DB `FAILED` + 固定 `PROVIDER_UNSENT_RETRYABLE`，交 `NotifyDispatchResultService` 现有有界 READY/PENDING 分支；`UNSENT_TERMINAL` → 固定终结分类 + DONE；`OUTCOME_UNKNOWN` 与旧 `FAILED` → DB UNKNOWN/WAITING_RECEIPT。MAIL 也须采用未知保守映射；当前 `unknownSmsProviderOutcome` 只管 SMS 的 `PROVIDER_ERROR`（`DispatchNotificationService` 247–249），MAIL generic FAILED 会错误回 READY。所有通向 `notifyClient.send` 之后的非 typed RuntimeException，包括 Redis `COMPLETE` 故障，继续 UNKNOWN；`ACQUIRE` 可证未进入 Adapter，维持 T-35 既有分支。普通配置/校验错误继续明确终结，且 REDACT/FULL 审计副本与内部回执关联不丢失。

这会扩展 common 公共 enum 的运行时/序列化语义，必须绑定 `.agents/skills/java-api-compatibility/SKILL.md`，完整核对构造、JSON、Redis、事件及 admin/notify 消费者；不因仓内一次切换而默许旧 Redis 字节误归类。可考虑单独 typed outcome record，但会改变公开 record 构造和序列化合同，当前枚举扩展更小。具体枚举命名可由 writer 决定，关键是机器可判定事实而非 errorCode。

## Redis 单键状态与 owner CAS

保留同一个 hashed storage key 和 digest。新 claim 需独立随机 owner nonce，**即使 requestId 相同也必须不同**：当前 `StoredState(IN_PROGRESS,digest,requestId,null)` 的序列化值对同一 delivery 每次相同，过期后旧 `Acquired.expectedValue` 会发生 ABA，旧 owner 可能覆盖新 owner。旧 `IN_PROGRESS/COMPLETED` 值兼容只读解析；无证明的旧 `COMPLETED FAILED` 不按错误码自动升级为 RETRYABLE，也不清缓存。

- 初次 key 不存在：原子 set-if-absent `IN_PROGRESS(digest,owner,requestId)`；异 digest 始终 Conflict，活跃同 digest 为 InProgress。
- 仅当前 owner 且全目标显式未发可重试：原子 `IN_PROGRESS→RETRYABLE`，保留 digest、原身份和**原剩余 TTL**；只在转换确认后让 runtime 进入 READY。转换异常、CAS=0、ACK 丢失均 fail closed 为 UNKNOWN/WAITING，不删键、不称已安全释放。
- 同 digest 重新 acquire：只允许 `RETRYABLE→IN_PROGRESS(new nonce)` 单键原子 CAS；旧 owner 的 complete/release/markRetryable 全部 0。异 digest 在 RETRYABLE 态仍 Conflict。新 owner 可以完成 ACCEPTED/终结/UNKNOWN；UNKNOWN 与已受理值完成保留防重。
- 用 Redis 单键原子脚本或等价 CAS 在锁定旧值的同时读取 PTTL 并保留到期时刻，避免读 TTL 后重置整段 window；不得因反复 RETRYABLE 无限延长缓存。现有 `complete` 会设置完整 window；若保留该既有成功窗口语义，须明确与重试态不同。Redis 断连/反序列化失败不可把 key 当不存在。过期后 Redis 不提供持久去重承诺，外部 UNKNOWN 仍由 DB WAITING 拦截；不能声称 exactly-once。

`NotifyIdempotencyStore` 最少新增 owner 限定的 `markRetryable(Acquired)`，`RedisNotifyIdempotencyStore` 实现单键转换，Coordinator 加 typed 包装 phase（如 `RETRYABLE_TRANSITION`）。保留 `release` 给调用前校验/快照失败的原 compare-delete 路径；不要把“已调用 Adapter 后”通用失败交给 release。结果转换与监控发布顺序需保证转换失败不会先对外返回 READY；Provider/Adapter 原始异常正文不得写日志或事件。

## 预登记写集和下游边界

现 Ticket 写集覆盖 common-notify `core/`、`idempotency/`、`model/`、runtime 两 Service、admin/notify 测试及 `notification.md`。在实施前由 Lead 修改 Ticket/Map 的 expected/writable/shared-owner 与 Skill 绑定，至少新增：

1. `backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/{SmsNotifyChannelAdapter,SmsNotificationReceipt,Sms4jNotificationProviderResolver}.java`：Resolver 仅在有供应商明确证据时映射拒绝类别；否则保持保守 UNKNOWN。可仅改 Adapter/Receipt 以接纳注入的显式 typed 测试 sender，但不能称真实 SMS4J 可重试拒绝已实现。
2. `backend/wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/notify/MailNotifyChannelAdapter.java`：供应商进入前/后界线，未知 SMTP 结果防盲发；MAIL 附件本地失败若纳入 typed retry，则同步相关测试。
3. `backend/wta-admin/src/test/java/org/namewta/test/notify/core/{SmsNotifyChannelAdapterUnitTest,MailNotifyChannelAdapterUnitTest}.java` 已在目录写集内；若新增 common 模块本地测试根，需单独预登记。公共 enum 与 store interface 的变更应新增 `java-api-compatibility` Skill 绑定，记录消费者/序列化迁移。
4. `NotificationApplicationRuntimeService.retry` 163–189 当前人工入口会重排 UNKNOWN；T-38/AC-038 已专门拥有该修复。T-37 只能声称**自动 Outbox/common** UNKNOWN 防盲重发；全入口断言必须等 T-38，不能以 Redis 5 分钟窗口掩盖手工入口。T-38/T-42 依赖本票状态合同。

## 必需测试与 T-35 可复用边界（均未执行）

- 先把旧 `shouldReuseFailedResultWithoutCallingProviderAgain` 改成 typed 四类与多目标测试。显式 UNSENT_RETRYABLE→ACCEPTED 同 key/同 digest 两次 Adapter 调用；第三次已接受重复零新调用。旧 FAILED、普通 PROVIDER_REJECTED、抛异常、混合 ACCEPTED+失败都不得转 RETRYABLE。验证监控事件与 REDACT 仍无敏感值。
- `RedisNotifyIdempotencyStoreIntegrationTest` 在一次性 Redis 验证 RETRYABLE CAS、20 线程仅一个新 owner、异 digest Conflict、相同 requestId 新 nonce 防 ABA、旧 owner complete/release/markRetryable 均无效、TTL 不增长、key 过期边界、状态损坏/Redis 中断 fail closed、COMPLETE/转态 ACK 未知。现测试仅成功态/首次并发/TTL，端口缺失会 assumption skip，验收要显式零 skip。
- 复用 `NotifySmsDispatchIntegrationTest` 的隔离六 SQL MySQL + Redis、真实 DAO/Dispatcher/Adapter/结果事务、合成 sender 计数、owned ID 清理。新增供应商第一次显式未受理可重试、第二次接受：DB 首次一 Attempt/PENDING+READY，下一次需通过**真实重新 claim/退避到期**而非直接对 PROCESSING 行调用，实际 senderCalls=2，最终两 Attempt/ACCEPTED+DONE；接受后重复 senderCalls 不增。另测不明确拒绝、供应商抛错、Redis COMPLETE CAS 故障，均 UNKNOWN/WAITING 且不重发；过期 owner、不同 digest、结果事务故障均保持 fence。既有 `supplierIoAmbiguityStaysUnknownAndNeverRequeues`、`redisCompletionCasFailureAfterSupplierRemainsUnknownWithoutResend`、`corruptRedisIdempotencyStateFailsAcquireBeforeSupplierAndRecovers` 可回归复用。
- `/tmp/wta-t35/run-notify-sms-integration.py` 可**作为隔离驱动参考**：随机 loopback MySQL/Redis、六 SQL、Surefire XML 计数、owned containers/process group 清理、redacted result。它当前硬编码 T35 类/env/运行目录，不能原样宣称 T37；Lead 应复制/参数化到 T37 选集，确保输出不含凭据、run 结束端口/容器/进程组归零，并核对实际类数/测试数/skip/固定 SHA。所需命令由实施者在候选 commit 后执行；本预审未运行任何门禁。
