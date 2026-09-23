# T-37 供应商“明确未受理可重试”事实核查（只读，2026-09-23）

范围：当前 `/srv/WTA-plus` 源码、Maven 本地 SMS4J 3.3.5 source jars、阿里云及腾讯云官方 SendSms 文档。未修改仓库、未运行 Maven/Docker/服务、未读取账号密钥。本文的供应商语义是根据官方返回字段作的**保守推论**，不是供应商提供的端到端 exactly-once 保证。

## 核查结论

**腾讯云单号码 `SendSms` 的明确 30 秒限频拒绝，是有官方语义支撑的候选来源；但当前 `SmsBlend` 公共接口无法证明所选实例只有一次 HTTP 尝试，通用 Resolver 不应立即把它映射为 `UNSENT_RETRYABLE`。** 前提是当前短信账号 blend 只执行一次 HTTP 请求，并且收到完整、结构正确、针对本号码的失败响应。当前 SMS4J `BaseConfig.maxRetries` 默认 `0`，项目 `Sms4jBlendRegistry.vendorConfig()` 创建厂商 Config 后未修改它；`NotifyConfigService.loadEnabledSmsAccounts()` 和账号更新走该 Registry。然而 SMS4J `AlibabaSmsImpl`/`TencentSmsImpl` 的失败重试是**无差别重发**，连传输 `SmsBlendException` 也会转失败并重试。因此 T-37 如要宣称此生产能力，应在 Registry 显式 `setMaxRetries(0)` 并以回归锁住；只从项目注册的 Ali/Tencent blend 解析，其他/配置漂移/未知 SDK 版本一律不能归为 `UNSENT_RETRYABLE`。`AbstractSmsBlend.getConfig()` 为 protected，Resolver 不能直接做通用运行时配置读取；应通过受控 Registry/供应商适配合同确认单次请求，而不是猜测。

在单次请求来源已被证明之后，最小建议 allowlist 是**仅腾讯** `LimitExceeded.PhoneNumberThirtySecondLimit`，且同时满足：`blend.getSupplier()=="tencent"`；`response!=null && !response.isSuccess()`；`response.getData()` 是原始 JSON 对象/Map；`Response.Error` 不存在；`Response.RequestId` 非空；`Response.SendStatusSet` **恰好 1 项**，其 `PhoneNumber` 与 SMS4J 实际发送的 E.164 号码一致、`Code` 精确为该常量、`SerialNo` 空、`Fee==0`。这是从官方“短信请求错误码”/“发送失败”字段示例与 30 秒限频代码推得的**未提交短信**判断；任何字段缺失、多个状态、不匹配、错误对象/字符串、其它 code 或异常都保守为 `OUTCOME_UNKNOWN`。`RequestId` 是本次 API 请求 ID，不能当短信受理回执；`SerialNo` 才是发送流水号。只存固定内部分类码，不存供应商 `Message`、原始 JSON、手机号/验证码。

阿里云 `isv.BUSINESS_LIMIT_CONTROL` 是官方列出的频率限制错误；其 `SendSms.Code` 非 `OK` 表示请求失败，`BizId` 是发送回执 ID。它可作为**第二阶段候选**：仅当单次请求、`Code` 精确匹配、`BizId` 为空而 `RequestId` 非空时才考虑未受理。官方并未为该通用限频码给出固定可解除时间，因此若 T-37 的自动重试窗口有限，首版不必把它列入 `UNSENT_RETRYABLE`；可保持 UNKNOWN/终结待裁决。不要将腾讯 `LimitExceeded.DeliveryFrequencyLimit`、日/小时配额、`InternalError.Timeout`、阿里 `isp.SYSTEM_ERROR` 或全部 `!isSuccess()` 泛化为安全可重试；超时尤其可能已受理。阿里云官方明确警告 SendSms **不支持幂等**，超时应先查回执再判断是否重试。

当前产品链**尚未提供**上述 typed 事实：`Sms4jNotificationProviderResolver.send()` 把所有 `!isSuccess()` 压成 `SmsNotificationReceipt.failed("PROVIDER_REJECTED", ...)`，`SmsNotifyChannelAdapter` 把失败压成通用 `NotifyTargetResult.failed("PROVIDER_REJECTED", ...)`，异常压成 `PROVIDER_ERROR`。现有 `SmsResponse` 只有 `success:boolean`、`data:Object`、`configId:String`，但 Ali/Tencent 实现成功或明确失败时将原始 JSON 放在 `data`，传输异常构造 `errorResp(e.message)` 时 `data` 是**任意 String**。必须在 Resolver 丢弃 `data` 前做严格解析并传递 typed 分类，不能按通用错误文本或旧 `FAILED` 推断。公共 `NotifyTargetResult` / `NotifyDeliveryStatus` 扩展仍须按 `/tmp/wta-t37-candidate-plan.md` 的 Java API 兼容规则评审。

## 实际依赖和官方证据

- `backend/pom.xml`：`sms4j.version=3.3.5`。本地源码 `/root/.m2/repository/org/dromara/sms4j/sms4j-provider/3.3.5/sms4j-provider-3.3.5-sources.jar` 的 `BaseConfig`：`maxRetries=0`；`AlibabaSmsImpl` 与 `TencentSmsImpl`：每个失败响应或捕获到的 `SmsBlendException` 都进入 `requestRetry`，直到 `retry >= maxRetries`。两个实现用同一 blend 实例的可变 `retry` 字段，故不应在全局开启 SMS4J 自动重试。
- 同一 source jar 的 `AlibabaSmsImpl.getResponse` 按原始 JSON `Code=="OK"` 决定 success；`TencentSmsImpl.getResponse` 按 `Response.Error` 和任一 `SendStatusSet[].Code!="Ok"` 决定失败。`sms4j-api-3.3.5-sources.jar` 的 `SmsResponse` / `SmsRespUtils` 不含“未受理”强类型。项目 `Sms4jBlendRegistry` 只创建 Ali/Tencent Config，默认重试未覆盖；生产送达路径见 `NotifyConfigService` 和 `Sms4jNotificationProviderResolver`。
- [腾讯云 SendSms API](https://cloud.tencent.com/document/api/382/38778)：返回 `SendStatusSet`；失败示例的 `SerialNo=""`、`Fee=0`、非 `Ok` `Code`，错误码表明确 `LimitExceeded.PhoneNumberThirtySecondLimit` 为单个手机号 30 秒下发上限，并列出 `InternalError.Timeout` 等不可按限频处理的错误。[腾讯云 SendStatus 数据结构](https://cloud.tencent.com/document/api/382/38779)将 `Code` 定义为短信请求错误码、`PhoneNumber` 为 E.164 号码、`SerialNo` 为发送流水号。
- [阿里云 SendSms API](https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendsms)：`Code=OK` 表示请求成功，`BizId` 为发送回执 ID；明确说明超时需先查询状态再决定重试、SendSms 不支持幂等。[阿里云错误码](https://help.aliyun.com/zh/sms/developer-reference/api-error-codes)将 `isv.BUSINESS_LIMIT_CONTROL` 定义为短信发送频率上限。

## MAIL 附件边界及验收建议

`MailNotifyChannelAdapter` 在 `sender.send(message)` 前调用 `Files.createTempDirectory()` 和 `materialize()`，`IOException | InvalidPathException` 被包装为 `NotifyAttachmentSnapshotException("SNAPSHOT_MATERIALIZE_FAILED")`；这是**可靠的零 SMTP 调用边界**，但当前包装混合暂态文件系统 I/O、永久错误路径及“物化器没有生成文件”，不足以全类标记可重试。可以只给明确暂态的本地 I/O 类型单独 typed 状态，保留其他为终结/未知，且要保持快照资源本身和清理语义。此用例只证明再次物化/首次实际 `sender.send`，**不能满足两次实际 Provider 调用**的 AC；该断言可用经官方结构验证的腾讯限频响应 + 第二次接受，通过项目真实 Adapter/Dispatcher/Redis/Outbox 边界验证，而不能把合成 sender 当作真实供应商事实。

建议新增/扩写集：`Sms4jBlendRegistry.java`（固定零 SDK 内部重试并测试）、`Sms4jNotificationProviderResolver.java`、`SmsNotificationReceipt.java`、`SmsNotifyChannelAdapter.java`，对应现有 SMS4J/Adapter 测试根；common typed 结果与 Redis 状态写集仍按 T-37 Ticket/Map 另登记并绑定 `java-api-compatibility`。测试应包含严格结构正例、缺 `RequestId`/错号码/多 status/有 `Response.Error`/有 `SerialNo`/`Fee>0`/字符串 errorResp/抛异常的保守反例；原 `PROVIDER_REJECTED` 和 `FAILED` 永不能直接触发重发。真实隔离测试应证明第一次明确定型未受理，第二次真实 Adapter `sendMessage` 调用被执行并返回接受；随后重复投递不再调用；`ACQUIRE`/`COMPLETE` 未知故障仍不重发。若无法证实供应商测试服务器真实返回该限频码，则对外表述为“官方响应语义支持的生产解析器 + 合成原始响应跨层验收”，不能声称已对真实供应商做过联调。

## 补充：所选 blend 的单次请求来源（本轮窄核查）

**SMS4J 3.3.5 没有可用的现成公共读取 API。** `SmsFactory.getSmsBlend(configId)` 只从全局 `BLENDS` 返回 `SmsBlend`；接口只暴露 `getConfigId()`、`getSupplier()` 和发送方法，没有 `maxRetries` 或配置对象。工厂通过 `SmsProxyFactory.getProxySmsBlend()` 注册 JDK 动态代理，底层 `AbstractSmsBlend.getConfig()` 是 protected，`SmsInvocationHandler.smsBlend` 是 private。`BeanFactory` 只有全局 `SmsConfig`，`SmsBlendConfigAware` 接收的是 starter 的 YAML map，不是动态 DB 注册后选中实例的配置。故 `getSupplier()=="tencent"`、`getConfigId()==DB key`、响应中的 `configId` 均**不证明**该代理内部 `maxRetries=0`；也不要反射代理/受保护字段。

在**当前项目生产源码**范围可作条件性来源证明：`rg 'SmsFactory.(createSmsBlend|register|registerIfAbsent|reload)' backend --glob '*.java' --glob '!**/src/test/**'` 只有 `wta-notify/.../Sms4jBlendRegistry.java:41` 一处创建调用；没有生产 `SmsReadConfig` 实现。`application-dev.yml:146-147` 和 `application-prod.yml:149-150` 均设置 `sms.config-type: interface`，而 SMS4J `SmsBlendsInitializer` 的 YAML 自动注册循环只在 `ConfigType.YAML` 分支。`NotifyConfigService` 启动和账号维护通过 `Sms4jBlendRegistry` 注册受管账号，`vendorConfig()` 新建 BaseConfig 且未更改默认 `maxRetries=0`。这足以说明**现在这个应用按这些配置启动时**的受管账号使用零 SDK 重试；建议仍显式设 0 并锁定回归。但 `SmsFactory.register/createSmsBlend/reload` 是公共、可覆盖同一 key 的全局 API；第三方扩展或不同配置启动时可混入同 supplier、同 configId 的 blend。由于 Resolver 属于可独立复用的 common 模块，上述源码归因**不是对所选对象的运行时证明**。

因此 T-37 的最小安全分界是：没有额外且可信的“此对象由受控零重试注册产生”证明时，`Sms4jNotificationProviderResolver` **不启用腾讯 typed allowlist**，只保留其解析器设计/测试作为以后可开启的候选；普通供应商拒绝仍 UNKNOWN，不以项目目前仅一注册点推导通用安全属性。如果 Lead 要在本票启用，需要先登记一个局部受控 provenance 合同（例如 Registry 保存实际注册的对象 identity，common Resolver 通过小型注入式只读判定接口核对所选对象 identity，而非只对 configId 打标签），并覆盖同 key 被其它代码覆盖后判定失败；这会增加明确写集和装配/兼容测试，不应以静态 `Set<configId>` 代替。该接口不需做新的全局注册平台，但**不是** SMS4J 现有 API。本轮没有实现或验证这一扩展。
