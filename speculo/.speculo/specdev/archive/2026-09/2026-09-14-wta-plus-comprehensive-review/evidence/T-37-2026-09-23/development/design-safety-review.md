# T-37 typed/Redis 状态兼容设计预审（只读）

**性质**：实施中风险清单，非固定候选审查或测试通过。产品输入固定 base `38032d24335c52cafea855b19d51fb36295162ef`；读取当前 Packet `evidence/dispatch-T-37.md`、`/tmp/wta-t37-candidate-plan.md`、`/tmp/wta-t37-provider-facts.md`、本地 SMS4J 3.3.5 source jars 与实际调用方。没有改产品/治理，未运行 Maven/Docker/服务或读取密钥。

## 首要闭合点：生产 typed 来源的对象归属

Packet revision158 指定腾讯单号码 30 秒限频**结构化拒绝**为首个生产候选，并要求 Registry 显式关闭 SMS4J 内部无差别重试。这里必须同时证明“本次选中的对象确实由受控 Registry 用 `maxRetries=0` 创建”。`SmsFactory.createSmsBlend(config)` 是 **void**；SDK 内部先调用私有 `create`（厂商工厂→代理），再向全局 `BLENDS` 注册。本地 SDK `SmsFactory.java:48-51,135-154,219-225`。如果 Registry 先调用该 void API、随后按 configId 从 Factory 查询并记录引用，并发外部注册可在两步之间覆盖同 key，导致外部对象被错认成受控对象。仅 configId、supplier、静态“仓内唯一注册点”、`setMaxRetries(0)` 都不能关闭这个窗口。

Lead revision159 的**最小现有 owner 方案成立，前提是实现按下列判据**：在现有 `Sms4jBlendRegistry` 中保留 `BeanFactory.getSmsConfig()` 初始化调用；用 `ProviderFactoryHolder.requireForSupplier(...).createSms(config)` 得到实际厂商对象，`SmsProxyFactory.getProxySmsBlend(actual)` 得到确切代理引用，`SmsFactory.register(proxy)` 注册同一引用，并在 Registry 自己的 map 记录该引用（`==`，不使用动态代理的 `equals`）。SDK 这些方法均 public，且 `SmsFactory.register` 仍执行 `SmsLoad.starConfig(proxy,1)`，与原 `createSmsBlend` 的代理和负载钩子等价；Registry 的 `vendorConfig` 必须显式 `setMaxRetries(0)`。此处没有新增第二套 SMS 全局注册平台或反射 SDK。Registry `remove`/账号替换先撤旧证明，即使 `unregister` 失败也不能保留 trusted 记录；`register` 失败、初始化失败、中途异常均无证明。同账号 upsert/remove 串行，证明只在完整注册后发布。Resolver 从 Factory 选中**对象 A**后，用注入的只读小 SPI 校验 A `==` Registry 当前受控引用，且随后就调用 A；查到 A 却发送 B 不成立。无 SPI 的 common 默认构造、第三方覆盖同 key、配置漂移及未知 supplier 一律不启用 typed allowlist。第三方在校验后覆盖 Factory，不会改变已经持有并发送的 A，故对象 identity 已闭合该次调用的关键风险；不应仅因公共 Factory 可被调用就无穷扩展阻断。仍需覆盖并发 upsert/remove/外部覆盖的正反例。

腾讯候选仍须严格按 `/tmp/wta-t37-provider-facts.md` 的单号码完整响应解析：`supplier=tencent`、完整 `Response`、非空 `RequestId`、无 `Response.Error`、恰一匹配 E.164 PhoneNumber、Code 精确 `LimitExceeded.PhoneNumberThirtySecondLimit`、无 SerialNo、Fee=0。任何缺字段、data 是 errorResp 字符串、异常、多个/错号状态或其它 code 均 UNKNOWN。官方响应语义支持的**生产解析器**与合成原始响应跨层测试可证明仓内生产路径已闭合；不得宣称已对真实供应商联调或端到端 exactly-once。

## Redis 状态、旧数据与故障

1. **同 requestId 的 ABA**：base `RedisNotifyIdempotencyStore.java:30-33` 用 digest+requestId+null result 序列化为占位值。同一 Outbox delivery ID 重试时 requestId 不变，旧 `Acquired.expectedValue` 可能与新占位同字节。新 `IN_PROGRESS` 每次要有独立随机 nonce，即使 requestId 相同；`Acquired` 的期望值要包含它。旧 owner 对新 claim 的 complete、release、markRetryable 均必须 CAS 失败。原 requestId 是审计/冲突身份，不得当 owner nonce。
2. **两次转态保持剩余 TTL**：仅当前 owner 可 `IN_PROGRESS→RETRYABLE`；同 digest 的下一次 acquire 原子 `RETRYABLE→IN_PROGRESS(new nonce)`，两个转换均保留 Redis 现存 PTTL，不按请求 `window` 重置。需单键原子操作同时核对旧值、PTTL、写新值；PTTL≤0、key 缺失、异常或非法状态 fail closed。异 digest 在 RETRYABLE 仍 Conflict；活跃 IN_PROGRESS 同 digest 仍 InProgress。重复可重试不会延长有限窗口；过期后 Redis 不承诺持久防重，DB UNKNOWN/WAITING 与有界 Outbox 才是外部自动重发的另一闸。现有成功 `complete` 重设完整 TTL 是独立既有语义，不能误套给 RETRYABLE。
3. **旧 COMPLETED 失败不升级**：继续读取同一 `notify:idempotency:v1:` key，旧四字段 StoredState 和旧 `NotifyDeliveryStatus.FAILED` 要能解码；旧 `COMPLETED` 即使 `NotifyStatus.FAILED` 或 `PROVIDER_REJECTED` 也只作为既有完成态返回，不凭字符串逆推新 typed 证据。不清缓存、不改 key namespace 使旧项“消失”。旧失败在 runtime 可转 UNKNOWN/人工核对，牺牲活性而不盲发。损坏 JSON、null result、未知 state/枚举要明确 fail closed，不能当 key 不存在。
4. **MARK_RETRYABLE ACK 未知**：Dispatcher 应先完成 owner CAS 转态、确认结果后，才向 runtime 抛带明确 UNSENT_RETRYABLE 的结果并让 Outbox READY。CAS=0、Redis 故障或写入成功但 ACK 丢失都作为 `NotifyIdempotencyUnavailableException` 的专门 phase/未知结果交 runtime UNKNOWN/WAITING；绝不 `release` 删除 key、吞异常后继续 READY。即使实际 Redis 已变 RETRYABLE，DB 停在 WAITING 是保守的活性损失，不能在此故障下假定已获可重试权。原 `COMPLETE` 故障仍在供应商调用后，亦 UNKNOWN。发布事件的成功/失败字段不能越过实际状态转移，REDACT 继续只发布安全副本。

## typed 结果和多目标

建议利用既有公开 `NotifyTargetResult.status` 扩展机器可判定枚举，保留 record 六参数构造、旧 FAILED 枚举及其保守含义。只有**所有**目标都被可信适配器标为 UNSENT_RETRYABLE，且没有 ACCEPTED/UNKNOWN/旧 FAILED，才可转 RETRYABLE。base `NotifyDispatcher.java:251-264` 只验证结果数量和目标相等，必须额外保证 typed 状态/流水号/错误字段不矛盾。部分接受、部分可重试、部分未知均不能释放整个请求；`NotifyStatus.PARTIAL_FAILURE` 无权暗示可重试。通用 Adapter 在返回一半结果后抛异常，也不能把整批转为未发。单目标 Outbox 例外只简化 runtime 判定，不可把 common 的多目标合同删掉。

runtime 消费 `NotifyDeliveryException.result().deliveries()[0].status`，不以 `result.status()==FAILED`、`PROVIDER_REJECTED` 或 errorMessage 判定重试。`UNSENT_RETRYABLE` 进入有界 READY/PENDING，终结未发进入 DONE，旧 FAILED/未知进入 UNKNOWN/WAITING。尤其 base `DispatchNotificationService.java:247-249` 的 `unknownSmsProviderOutcome` 只把 SMS `PROVIDER_ERROR` 归未知，MAIL `MailNotifyChannelAdapter.java:68-85` 的 sender 异常也会产生 `PROVIDER_ERROR`，当前会落回 READY；T37 必须让 MAIL generic UNKNOWN，不可再次 SMTP。SMS 配额获取在 `NotifySendPlanner.java:125-160`、common claim 之前；其 `PREPARATION_RETRYABLE` 保留 T35 的零发送重试，但**不是** Redis RETRYABLE 转态。腾讯明示限频后的第二次尝试会重新走本地账号/模板/收件人配额；Outbox 上限/退避仍控制节奏，不能把配额返回或 `ACCOUNT_QUOTA` 改成供应商已受理或可释放 claim 的证据。终结未发应由 ResultService 显式固定分类关闭，不能因不在 `configFailure` 白名单而意外 READY（`NotifyDispatchResultService.java:151-164`）。

## 公开合同和消费者清单

`NotifyDeliveryStatus`、`NotifyTargetResult`、`NotifyResult`、`NotifyAdapterResult`、`NotifyIdempotencyStore` 均在 common 公开 Java 面；`SmsNotificationReceipt` 与 `SmsNotificationProviderResolver` 在 common-sms 公开面。主要生产读写者：SMS/Mail adapters，`NotifyDispatcher`（聚合、completed reuse、REDACT 审计副本），`NotifyIdempotencyCoordinator`，`RedisNotifyIdempotencyStore.StoredState`（`JsonMapper` 持久值），`NotifyDeliveryException`，`DispatchNotificationService`，Spring `NotifyAutoConfiguration.notifyEventPublisher` 发布的 `NotifyDeliveryEvent`。固定 base 中未发现生产 `NotifyDeliveryEvent` 专用 listener，但事件类型对扩展消费者仍公开；admin/notify 的 Dispatcher/Adapter/附件、Redis Store、SMS 组合测试和 wta-notify Dispatch 单测均直接构造/断言旧模型。旧 `FAILED` 不能删除或偷偷改为 retryable；新 enum JSON 值、旧 StoredState 字节解码、REDACT/FULL 事件字段、六参数 record 构造及有/无新增 SPI 的 Spring 装配都需回归。已登记 `java-api-compatibility` Skill；实现者应记录实际 declaration→callers→serialization 影响，不以“仓内直接切换”省略。

**最小验收反例**：同 requestId ABA、20 并发 RETRYABLE reacquire 仅一个 owner、连续 retry PTTL 单调不增、旧 COMPLETED FAILED 字节、旧 owner 三类 CAS、异 digest 冲突、MARK_RETRYABLE ACK 未知、部分接受+可重试零整批释放、MAIL SMTP 抛异常零二次发送、SMS quota 前置故障零 provider 和 Redis 转态、Registry 同 key 更新/撤销/第三方覆盖以及无 SPI 默认保守、腾讯原始结构 7 类反例。真实 MySQL/Redis/Dispatcher/Adapter/Outbox 组合必须由 Lead 在固定候选上运行，记录测试数/skip、源码与 owned 资源清理；本报告未执行。
