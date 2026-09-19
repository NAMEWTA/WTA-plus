# T-03/T-23 实现准备：新增源码证据

本文件只记录当前代码与后续决策输入；不是两票实现Evidence，不关闭G-plan。源文件sha256见planning-blocker-source-checkpoint.json。HEAD为76dbbe84a34624234e379661a57b232529e34ed3。

## T-03

- `RepeatedlyRequestWrapper`使用`IoUtil.readBytes(input,false)`无界读取；`ReplayableOpenApiRequest`使用`readAllBytes`，`body()`另做完整防御性复制；`DecryptRequestBodyWrapper`另外拥有密文byte[]、密文String、明文String、明文byte[]。
- `SysLogProperties.maxBodySize`的1 MiB只控制日志正文截取，不能据此声称入口有硬上限。
- `application.yml`的Jetty form post为-1、最大线程256；multipart最大请求20MB；Nginx模板为100m。它们不提供普通JSON合法最大样本和内存预算证据。
- Compose默认JAVA_OPTS为-Xms512m -Xmx1024m。256个请求各2MiB原始缓存已达512MiB，尚未计算各种视图、复制、业务和日志；这是算术风险估计，不是执行压测结果。
- 下一动作：枚举真实普通JSON/机器正文输入及最大代表样本，记录字节；按单请求所有view峰值和配置内存测量预算，固定独立入口cap与日志prefix，继续保持上传/SSE流式。不可把2MiB候选直接改称生产上限。

## T-23

- 当前唯一入站入口为`ProviderCallbackController`，使用全局`notify.callback-secret`和`X-Notify-Signature`；UseCase执行项目自有HMAC-SHA256原始payload验证及±300秒timestamp校验，再读取providerKey/eventId/providerMessageId/status。当前路径channel未包含在此HMAC输入中。
- `ProviderCallbackService`以裸eventId在进程内去重10分钟，更新后在聚合前写seenEvents；进程重启不保留，事务回滚也不会撤销Map写入。
- `NotifyConfigService`按channel/configKey区分账号并允许编辑配置键。SMS4J resolver用requestedProviderKey找到SmsBlend，并返回其configId；Mail adapter使用request.providerKey选择账号。因此账号配置映射确实存在，不能一概声称完全没有账号身份。
- SMS4J resolver的send只返回`SmsNotificationReceipt.accepted()`，未从SmsResponse提取providerMessageId；不能凭mock成功证明真实短信回调能定位delivery。
- Mail adapter一次send返回一个messageId，并给所有targets写相同messageId；当前DAO按channel/providerKey/providerMessageId执行selectOne。多个delivery共享该键可能产生多结果；需在实际事务测试验证关联策略。
- 没有从上述实际实现获得外部供应商eventId作用域或最长重试期限；300秒签名窗口与10分钟进程缓存不等同于供应商重试合同。
- 下一动作：围绕现有自有HMAC协议明确channel/账号命名空间、消息关联与event身份，验证真实SMS消息ID和邮件多目标；再定义receipt保留策略、同payload幂等及冲突拒绝。没有外部重试证据时不得猜测自动清理TTL。T-23与T-30维持未完成。
