# T-03 / T-23 当前工作树复核

这是 T-30 准备中的只读复核，不是实现或运行验收。17 个实际文件 SHA 见 planning-blocker-source-checkpoint-current.json；旧 planning-blocker-source-review.md 保留其当时证据。两项业务事实问题已向用户发出，尚未收到答复。

## T-03：仍缺合法请求尺寸及目标资源预算

- T-11 已删除浏览器解密包装器；旧记录中的 DecryptRequestBodyWrapper 不再是当前请求链，不能继续据它估算当前副本数。
- RepeatableFilter 注册顺序为 HIGHEST_PRECEDENCE + 1，XssFilter 为 +3；RepeatedlyRequestWrapper 仍使用 IoUtil.readBytes 全量缓存，XSS JSON 包装仍全量读取后创建字符串。机器入口 ReplayableOpenApiRequest 仍 readAllBytes，body() 再做防御性全量复制。
- OpenApiGatewayFilter 检查签名头是否完整、是否混入浏览器认证后，才创建 replayable；但此时还未执行 authenticate 验证签名。完整签名头但错误签名的大正文仍需进入 future 边界矩阵。
- SysLogFilter.prepareRequest 的 IOException / RuntimeException catch 会报告采集失败后放行原请求；未来入口超限异常不能被此诊断降级吞掉。日志 maxBodyBytes 控制前缀，不能冒充入口 cap。
- 当前配置仍是 Jetty form -1、最大线程256、multipart请求20MB、Nginx100m，Compose默认最大堆1GB。这些配置不是普通JSON或机器调用最大合法业务样本，也不是并发峰值实测。

因此不把2MiB测量起点设成生产默认值，不猜测业务限额。待补普通JSON/机器合法最大请求、脱敏代表样本、目标内存与并发预算后，才能确定cap并验证固定长度/chunked的limit-1、limit、limit+1，签名原始字节、失败413、SSE/上传/取消与峰值分配。当前没有执行压测，没有新增限制。

## T-23：仍缺供应商回调合同

- T-22/T-28 已改变当前协作面：ProviderCallbackUseCase 带 DSTransactional；Service 先锁 Intent 再锁 Delivery，并通过 NotifyDispatchResultPort 刷新聚合。不能把旧记录泛化为“完全没有事务或锁”。
- 但去重仍为单实例 ConcurrentHashMap，key 为裸eventId、缓存10分钟；部分分支与聚合前写Map，不随事务回滚，也不跨实例/重启持久化。这仍是T-23未完成范围。
- 入口仍用全局callback-secret验原始payload的HMAC-SHA256；channel在路径中，不包含在该签名输入。±300秒校验是项目协议窗口，不是供应商最长重试期。
- 账号通过channel/configKey/providerKey映射；真实供应商及eventId跨账号/渠道唯一范围不能从配置键猜测。SMS4J成功响应仍只返回accepted()而未提取providerMessageId；邮件一次发送返回的messageId仍分配给全部targets，DAO仍按channel/providerKey/providerMessageId做selectOne。

仍需实际短信/邮件供应商、账号映射、eventId唯一范围和最长重试协议。不能猜测receipt唯一键或自动清理TTL，也不能以项目10分钟缓存代替供应商合同。未来验收必须覆盖双实例、重启、重复/乱序、同event不同payload、跨命名空间、事务回滚与早到回调；供应商messageId真实关联单列。当前未执行供应商调用、未写业务数据或DDL。
