# T-23 原生短信状态核对合同

Revision126，实施前冻结。沿用 T-23 已登记的 common-sms、Notify、DDL、测试及说明写集，不新增 SDK 依赖或发送框架。发送仍通过 SMS4J；补充原生只读 HTTP 查询，签名算法按官方 TC3 / ACS3 合同实现，并以独立参考值和真实本地 HTTP 验证。

## 官方依据（2026-09-19 核对）

- [腾讯 DescribeSendRecordList](https://cloud.tencent.com/document/api/382/137245)：号码与发送时间范围查询；2021-01-11 API，国内域名 sms.tencentcloudapi.com。历史窗口72小时，单页50、最多1000；SerialNo关联发送流水，状态2送达、4未送达、3等待。1是提交失败，与本地已受理不一致时不擅自重发。
- [腾讯数据结构](https://cloud.tencent.com/document/api/382/52068)和[官方 SDK](https://github.com/TencentCloud/tencentcloud-sdk-java/blob/master/src/main/java/com/tencentcloudapi/sms/v20210111/SmsClient.java)交叉确认字段。未选择有消费语义的全局 PullSmsSendStatus；无须其队列开通条件。
- [阿里 QuerySendDetails](https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-querysenddetails)：BizId、号码、发送日过滤，历史30天；状态1等待、2失败、3成功。响应没有BizId字段，不能忽略请求过滤；多条或号码不一致失败关闭，不读取/记录短信正文。
- [阿里 ACS3](https://help.aliyun.com/zh/sdk/product-overview/v3-request-structure-and-signature)与[腾讯 TC3](https://cloud.tencent.com/document/product/1278/46713)：账号密钥签名请求，HTTPS校验服务端身份；不伪造厂商推送HMAC。

## 实施合同

1. 五个账号仍默认关闭。只有已启用腾讯/阿里账号下 SMS ACCEPTED、存在消息号的投递进入核对。停用/删除账号不触发供应商请求。复用账号密钥；阿里RAM/腾讯CAM需允许对应查询操作。
2. 一次查询只读取一个既有消息号；供应商失败、未找到、未知状态、字段不合法、分页截断或关联歧义均保持原状态。绝不通过状态查询调用发送接口。腾讯按精确SerialNo+实际发送号码匹配，阿里按BizId请求过滤和唯一号码记录匹配。
3. 腾讯查询最多20页/1000条。阿里按本地受理日期及跨午夜前一天查询并要求全局唯一结果；时间统一Asia/Shanghai发送日，数据库时间统一UTC。查不到不假设失败。
4. 自动查询窗口统一保守使用受理后71小时（低于腾讯72小时上限，预留网络/时钟余量）；超窗保留ACCEPTED，监控仍显示ACCEPTED，配置说明明确其未确认语义。此本地核对窗口不是厂商回调重试期限，receipt不因它过期。
5. Notify按数据库UTC领取到期投递并持久保留下一次查询时间；单次有界批次、跨节点CAS避免同一到期记录被同时领取。外部HTTP在事务外；失败和进程退出由下一次到期恢复。领取只调度查询，不改变发送/Outbox租约。
6. 原生查询结果通过独立UseCase事务进入同一个ProviderCallbackService，事件ID使用保留前缀加供应商/消息号/精确target/终态的SHA-256。自定义HTTP事件禁止保留前缀，避免已认证外部调用占用内部核对身份。receipt、状态与聚合原子提交；重复核对/确认丢失可安全重试。
7. 外部请求只允许固定厂商HTTPS域名，禁重定向；连接/读取有界，响应最大256KiB，错误只暴露固定分类。账号密钥、精确号码、短信正文、原始响应不写日志；测试可在包内注入受控loopback传输，不增加产品端点配置。

## 必须验证

签名参考值/Unicode与编码/跨午夜、真实HTTP请求及重定向拒绝/非200/超时/超限、完整分页/截断/重复/错误号码/未知状态、同一事件重复和冲突、真实数据库跨实例领取及失败重试、停用账号零网络调用、查询无发送副作用、事务回滚后相同结果可重试。无真实凭据，供应商响应是协议夹具，不声称真实供应商送达。
