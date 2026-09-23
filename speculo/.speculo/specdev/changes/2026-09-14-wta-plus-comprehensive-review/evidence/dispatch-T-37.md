# Dispatch Packet T-37-20260923-01

operation=dispatch；task_kind=implementation；delivery_channel=native；provider=gpt-6-sol/xhigh；Lead=/root，唯一产品owner=cors_audit。current/main/direct-parent，无新worktree。base `38032d24335c52cafea855b19d51fb36295162ef`；当前治理dirty属于Lead，不回滚。

先完整读取tickets-map→下列Skill与scope references→ticket/37-retryable-provider-idempotency.md/goal-plan/spec/ADR。用户已激活Goal，允许本地实现和逐票commit（commit仅Lead操作）。
- <Path>.agents/skills/engineering-standards/SKILL.md</Path>
- <Path>.agents/skills/wta-common-modules-guide/SKILL.md</Path>
- <Path>.agents/skills/wta-module-guide/SKILL.md</Path>
- <Path>.agents/skills/java-api-compatibility/SKILL.md</Path>
- <Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>

唯一产品写集（目录仅本票相关文件）：
- <Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/</Path>
- <Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/</Path>
- <Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>
- <Path>backend/wta-admin/src/test/java/org/namewta/test/notify/</Path>
- <Path>backend/wta-modules/wta-notify/src/test/</Path>
- <Path>.agents/skills/engineering-standards/references/notification.md</Path>
- <Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/exception/NotifyIdempotencyUnavailableException.java</Path>
- <Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/</Path>
- <Path>backend/wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/notify/MailNotifyChannelAdapter.java</Path>
- <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/provider/Sms4jBlendRegistry.java</Path>
- <Path>backend/wta-common/wta-common-sms/src/test/</Path>

## revision158 当前实施与合同细化

base `38032d24335c52cafea855b19d51fb36295162ef`；cors_audit唯一产品writer，Lead独占治理、commit、服务与E2E。自动Outbox/common覆盖本票；人工retry的UNKNOWN拒绝与精确ID归T38，最终全部入口由T30汇合，不用Redis TTL冒充持久exactly-once。

结果采用机器可判定的未发送可重试、未发送终结、已接受、结果未知事实；旧FAILED/PROVIDER_REJECTED和任何未明确分类异常均无重发权。仅全部目标明确未发送且可重试，当前owner才能CAS为RETRYABLE；保留digest/请求身份及剩余TTL，同requestId每次独立nonce防ABA。新claim仅同digest CAS可重取；旧owner不能complete/release/转态新claim；转态失败/ACK未知失败关闭，不删键或全清缓存。已接受/UNKNOWN保留防重，混合结果不得整批释放。外部SMS与MAIL调用异常均保守UNKNOWN；T35 ACQUIRE前零发送准备路径保持。

生产来源候选为受控单次请求的腾讯单号码30秒限频结构化拒绝；严格校验供应商、响应类型、单匹配号码、固定Code、RequestId、无Error/SerialNo及Fee=0，其他类别不放入allowlist。具体SDK单次请求来源须由源码/API事实证明；不能证明的blend保持未知，不新增旁路安全注册平台。Sms4jBlendRegistry明确关闭SDK内部无差别重试。供应商实际联调不在本票，官方响应语义支持的生产解析器与合成响应跨层验收须分开说明。MAIL附件错误未有暂态类型证明前不整类标为可重试；附件完整合同仍归T42。

新增写集在编辑前登记：common幂等异常阶段、SMS notify目录/本地测试、MAIL Adapter和SMS4J Registry；公共模型/store/receipt签名与序列化消费者按JavaAPI技能同步，仓内直接切换，不恢复兼容桥。notification.md在本票写集，必须同步保守重试与旧缓存处置事实；永久ADR不改。

先可观察失败测试再最小实现；真实Redis检验owner CAS/20并发/digest/TTL/旧状态/损坏，真实MySQL+Redis+Dispatcher+Adapter+Outbox验证拒绝后两次物理调用、真实重新claim与到期退避、接受与未知零重发、COMPLETE/转态故障、租约fence。无skip冒充验收；T35/T36回归按受影响输入执行。禁止全缓存删除、改生产凭据或实际厂商发送。

输入：/tmp/wta-t37-audit.md（旧只读基线）、/tmp/wta-t37-candidate-plan.md、/tmp/wta-t37-provider-facts.md，T35/T36当前Evidence。重读当前生产，不用旧行号改代码。

禁止：治理/相邻OIDC/永久知识/其他Skill写入，生产或私有temp读取/凭据，Docker/真实服务启动，commit/push/部署/归档。允许精确非E2E Maven（先报selector）、分层和facts检查。日志放/tmp/wta-t37；先红灯证据、必要时测试先交Lead真环境红灯，然后实现。越界先报Lead预登记。最终报告路径、源码、测试argv/count/skip、Skill实际操作、已知边界与写锁交还；不自行Done。独立双轴由另两agent固定SHA审查。

## revision159 单次SDK请求的实例事实闭环

编辑前新增精确写集：<Path>backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/config/SmsAutoConfiguration.java</Path>。在现有Sms4jBlendRegistry维护自身以maxRetries=0创建的实际代理实例identity；使用已验证的BaseProviderFactory.createSms→SmsProxyFactory.getProxySmsBlend→SmsFactory.register同一引用，保留原SDK初始化所需钩子，不将void create后再get的可覆盖对象盲认证。remove先撤认证；注册/更新失败不保留认证，按账号并发更新需一致，不暴露配置对象。common-sms notify目录内小型SmsSingleAttemptBlendVerifier SPI由Registry实现，Resolver经AutoConfiguration ObjectProvider注入，缺失/identity不匹配即不把拒绝标成可重试；严格腾讯结构allowlist只有该证明成立才启用。已有Registry拥有这一事实，不新增第二套全局注册平台，不反射SDK，不让common反向依赖业务。固定对象捕获到send，避免查验A发送B。公共SPI/构造/配置方法调用者与测试同步。

### revision159 Redis序列化与剩余TTL实证

生产RedisConfig默认CompositeCodec(StringCodec,TypedJsonJackson3Codec)，旧Store bucket采用client默认codec；SMS真实fixture默认codec，Redis专项fixture显式StringCodec。新单键脚本必须沿用旧值的codec及NameMapper，不能改成StringCodec后让旧key不可读/比较失败。Redisson4.6.1源码的CompareAndSetArgs不指定TTL会SET并清TTL，不能作为保留期限实现；使用与bucket一致编码的原子CAS+PTTL/KEEPTTL，在同脚本内验证key存在且有正剩余TTL。真实回归需涵盖项目CompositeCodec及名称前缀、旧四字段StoredState，不只StringCodec绿色。
