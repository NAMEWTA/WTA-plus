# T-37 实施中安全预审（只读、非固定候选）

输入：base `38032d24335c52cafea855b19d51fb36295162ef`，当前 dirty diff，`evidence/dispatch-T-37.md` revision159、ticket37、`/tmp/wta-t37/{design-safety-review,redis-codec-design-review}.md`、`/tmp/wta-t37-provider-facts.md` 和已有红绿记录。唯一 writer 仍为 cors_audit；本报告没有修改 repo，也没有运行 Maven/Redis/MySQL/HTTP。静态结论仅针对本次读取的源码字节，不能作为 clean candidate 的正式通过。

## 读时快照

读前/读后 SHA-256 相同（以下短前缀足以与完整 `sha256sum` 重查）：`NotifyDispatcher` 0fba318d；`NotifyIdempotencyCoordinator` 34f23cc6；`NotifyIdempotencyStore` ca512aef；`RedisNotifyIdempotencyStore` 48d2267a；`NotifyDeliveryStatus` 2f2ea05a；`NotifyTargetResult` c987b110；`SmsAutoConfiguration` 48527cc5；`Sms4jNotificationProviderResolver` 72f285d7；`SmsNotificationReceipt` 3887afa4；`SmsNotifyChannelAdapter` 981bb72e；新增 `SmsSingleAttemptBlendVerifier` 2552b5fc；`Sms4jBlendRegistry` 2b9741da；`DispatchNotificationService` e195b8c1；`NotifyDispatchResultService` 3a181a18；`MailNotifyChannelAdapter` a3e1e08f。均为 `backend/` 下上述同名 Java 文件。本轮其他测试/治理文件仍由 writer 施工，未作稳定快照承诺。

## 聚焦结论

**在上述静态快照中，没有发现可证明的新增自动盲重发或凭据泄漏阻断项。** 这是实施中预审结论，不是 T-37 验收通过。下面每条有源码事实与尚需实测的边界；任何一处后续 diff 都需按固定 candidate 重审。

- **全目标事实与混合结果。** `NotifyDispatcher.java:99-133,268-310` 先校验目标数/目标顺序，聚合后只有每个目标都是 `UNSENT_RETRYABLE` 才调用 owner 的 `markRetryable`；`ACCEPTED+UNKNOWN`、`UNSENT_RETRYABLE+UNKNOWN`、旧 `FAILED` 均走 `complete`，不会释放整批。无结果或 adapter 抛普通异常变成各目标 `OUTCOME_UNKNOWN`。`markRetryable` 失败/ACK 未知抛 `NotifyIdempotencyUnavailableException(RETRYABLE_TRANSITION)`，不返回 typed 重试结果；runtime :138-150 将该 phase 置 UNKNOWN/WAITING。附件快照的补偿与事件发布不会调用 provider 第二次。未发现 `PROVIDER_REJECTED`/旧 `FAILED` 通过字符串推导重试权。
- **Redis owner/旧值/codec/期限。** `RedisNotifyIdempotencyStore.java:38-70,74-115` 的新 `IN_PROGRESS` 每次使用 UUID nonce，重取 `RETRYABLE` 前同 digest 校验；三种旧 owner complete、release、markRetryable 都以完整期望值 CAS，无法修改新占位。旧四字段 `IN_PROGRESS` 留在占位状态、旧 `COMPLETED FAILED` 复用原结果，未转成 RETRYABLE；损坏、无结果 COMPLETED 和无 nonce RETRYABLE 抛异常，不当空键。单键 Lua 比较编码后的旧值、要求 `PTTL>0`，`SET ... KEEPTTL`；`bucket.getCodec()`、`bucket.getName()` 与 Redisson 4.6.1 `RedissonScript.eval(String key,...)` 一次 NameMapper 和 ARGV value encoder 路径相符，没有在源码上看到双前缀/双编码。`complete` 保留原有成功态重新设定窗口语义；转态/重取保持剩余 TTL。真实 CompositeCodec+前缀、StringCodec、20 并发、旧字节及故障注入仍必须以真实 Redis 零 skip 证明，静态不能宣称 Lua 已实际成功执行。
- **单次短信来源与严格腾讯拒绝。** `Sms4jBlendRegistry.java:42-72,112-121` 先撤旧证明，创建 `maxRetries=0` 的厂商对象，再包代理并把**同一引用**交给 `SmsFactory.register` 和 Registry map；同账号 upsert/remove 同步，失败不留认证。`SmsAutoConfiguration.java:41-47` 以 ObjectProvider 注入 verifier，缺 SPI 时 Resolver 默认否定。`Sms4jNotificationProviderResolver.java:30-85` 固定选中 blend A 用于发送及引用身份检查；仅身份匹配、Tencent、失败响应且 configId 一致、`Response` 无 Error、有 RequestId、单个匹配号码、精确 30 秒码、空 SerialNo、Fee 0 才生成 typed retryable。对照本地 SMS4J 3.3.5 source：`SmsFactory.createSmsBlend` 的内部实现也是 provider factory→proxy→register；Tencent 实现一次 `http.postJson` 后返回 `SmsRespUtils.resp(..., getConfigId())`，默认失败可进入 `requestRetry`，因此显式 `maxRetries=0` 对实际厂商对象是必要条件。其他失败/字符串 errorResp/异常仍走 FAILED 或 UNKNOWN，runtime 对其均保守 UNKNOWN。真实账号配置、选中实例身份及合成原始响应跨层测试待运行；不声称实际腾讯联调。
- **运行时防重。** `DispatchNotificationService.java:117-166,269-297` 对单目标仅 typed `UNSENT_RETRYABLE` 产 `PROVIDER_UNSENT_RETRYABLE`，ResultService :151-164 在有界预算内 READY/退避；终结未发 DONE；其他单目标状态、非全接受的多目标和外部异常进 UNKNOWN/WAITING。SMS `ACQUIRE` 在适配器前，保留既有 `PREPARATION_RETRYABLE`；COMPLETE/RETRYABLE_TRANSITION/MAIL generic failure 没有安全重发权。`MailNotifyChannelAdapter.java:69-84` 的 `sender.send` 异常现在 typed UNKNOWN。T35/T36 配额和 lease/结果事务仍须回归，但本快照没有把 UNKNOWN 当成 READY 的新分支。

## 实施期间应闭合的证据（非本次发现的产品 blocker）

1. `/tmp/wta-t37/red-typed-dispatch.log`：同一个精确单测 1 run/1 failure/0 skip、exit 1；`red-cache-behavior.log`：1 run/1 error/0 skip、exit 1；`green-typed-dispatch-v1.log`：`NotifyIdempotencyDispatcherUnitTest#explicitUnsentRetryableResultPermitsOneNewPhysicalAttempt` 1/1/0 skip、exit 0。绿灯目前只覆盖合成 MemoryStore 一例，不证明 Redis/MySQL/生产 Registry。
2. Dirty 测试源码已有 `RedisNotifyIdempotencyStoreIntegrationTest.retryableOwnerCasRetainsTtlAndReadsLegacyStateWithProductionCompositeCodec`、StringCodec 变体，`NotifySmsDispatchIntegrationTest.exactSingleAttemptTencentRateLimitReclaimsAndCallsTheSelectedBlendTwice`、lost ACK 场景，以及 Registry/Receipt 的合成结构反例。须由 Lead 在固定 clean candidate 上实际执行，记录真实服务、数目/skip、PTTL与 owned key/容器清理。特别要确认上述合成腾讯夹具真实经过 Registry→Resolver→Adapter→Dispatcher→Redis→Outbox，第二次物理 blend 调用只发生于新的合法 claim；不把 fake Tencent 响应叫供应商联调。
3. 后续固定 candidate 需核对公开 enum/record、`NotifyIdempotencyStore` 新方法、旧 Redis StoredState 反序列化与 REDACT/FULL 事件；MAIL 异常、SMS 配额前置、COMPLETE/转态 ACK 未知、混合结果和 T35/T36 回归均不可被一条 MemoryStore 绿灯替代。无 skip 的真实测试结果未提供前，验收状态保持待验证。

## 范围边界

T38 的人工 UNKNOWN 指定 ID 恢复、T42 附件完整合同、任意外部代码篡改 Redis、任意自定义 Adapter 伪造 typed 结果和全球性 exactly-once 均未被本报告冒充 T37 已解决，也不因这些假设要求扩大本票。当前源码通过 public SPI 允许扩展，但生产受管 SMS 路径在本读时快照有实例身份验证和严格解析；只有真实可达的违例才应作为阻断项。没有读私有凭据或运行服务。
