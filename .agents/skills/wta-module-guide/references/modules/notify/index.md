# Notify 模块索引

`wta-notify` 是当前统一通知控制面，负责公告、通知意图、收件箱、投递记录、Outbox、渠道适配和供应商回调。

## 调用边界

入口统一经过 `controller -> usecase -> service -> dao -> mapper`。Controller 不直接注入 Service、Mapper 或 JSON/Redis 工具；跨模块只使用 `wta-api` 的 `NotificationApplicationService`、`InAppNotificationPort` 和 Common Notify SPI。

## 能力入口

| 能力 | 当前入口 |
|---|---|
| 公告管理 | `controller/admin/NotifyNoticeController`、`usecase/NotifyNoticeUseCase` |
| 收件箱 | `controller/admin/NotifyInboxController`、`usecase/NotifyInboxUseCase` |
| 投递监控 | `controller/admin/NotificationMonitorController`、`usecase/NotificationMonitorUseCase` |
| 供应商回调 | `controller/anonymous/ProviderCallbackController`、`usecase/ProviderCallbackUseCase` |
| 原生短信状态核对 | `adapter/worker/SmsReceiptQueryWorker`、`usecase/SmsReceiptQueryUseCase`、`common-sms/SmsDeliveryQueryClient` |
| 通知配置 | `controller/admin/NotifyConfigController`、`usecase/NotifyConfigUseCase` |
| 统一业务通知 | `org.namewta.notify.api.NotificationApplicationService` |
| 实时推送 | `org.namewta.notify.api.InAppNotificationPort` 与 `common-push` 一次性票据 |

## 公告发送对象与渠道

公告保存与发布是两个独立动作：`POST /notify/notice/save` 保存草稿，`POST /notify/notice/{noticeId}/publish` 才生成发布快照并提交异步通知。草稿与已撤回公告允许编辑；已发布公告不直接改写已投递快照。

| `recipientType` | 目标字段 | 解析入口 |
|---|---|---|
| `ALL` | `recipientIds=[]`、`userTypeIds=[]` | `UserService.selectAllActiveUsers(offset, limit)` 分页读取正常用户 |
| `USER` | 非空 `recipientIds`、`userTypeIds=[]` | `UserService.selectNotificationUsers` 校验数据权限并读取仍处于正常状态的用户 |
| `USER_TYPE` | `recipientIds=[]`、非空 `userTypeIds` | `UserService.selectUsersByUserTypeIds` 将登录域解析为用户，再提交 `USER` 通知 |

`UserService` 与 `UserDTO` 由 `wta-api` 公开；Notify 不查询 System 的表、Mapper 或内部 Service。公告选择 `USER_TYPE` 时保存的是用户类型 ID，实际用户在发布时解析。该公告范围合同与底层 `NotificationCommand` 的直接手机号、邮箱接收者合同分别由各自入口校验。

公告渠道为 `IN_APP`（站内信）、`SMS`（短信）、`MAIL`（邮件），可组合选择，未选择时默认 `IN_APP`。渠道选择只决定投递请求，短信与邮件是否成功仍取决于接收者联系方式和对应供应商配置，不能把提交或 Provider `ACCEPTED` 视为已送达。

每次公告发布用当前公告 ID、快照 ID、版本和 `noticeVersion` 意图元数据精确关联。撤回事务只写该版本持久栅栏与公告生命周期，后续 Worker 在活租约内关闭尚未获得发送权的任务；外部已进入 Provider 的受理或未知结果仍按原回执落库，已送达站内消息及本人关系保留。缺失或损坏版本元数据的历史公告任务按 `NOTICE_VERSION_UNVERIFIED` 失败关闭，不盲发也不冒充已撤回。六 SQL 初始化中仅两个固定、快照身份匹配且无精确键或同业务 Intent 的静态公告允许窄条件生命周期撤回；其他缺失 Intent 的发布记录必须拒绝。含 `IN_APP` 的新公告将快照、Intent 和模板参数路径同事务更新为 `/notify/inbox?messageId=<本人消息ID>`；仅外部渠道保留 `/notify/inbox` 通用入口。旧 `/notify/notice?noticeId=...` 只可凭当前本人消息 ID 转入收件箱，不能让收件人访问公告管理页。

`notify_notice` 保存 `recipient_type`、`recipient_ids_json`、`user_type_ids_json`、`channels_json`；`notify_message.notice_type/channels_json` 保留站内消息的类型与渠道快照。完整初始化基座中的公告显式使用 `ALL`、空目标列表和 `["IN_APP"]`，只初始化公告及快照，不生成外部投递任务。

事实入口：`wta-api/src/main/java/org/namewta/system/api/UserService.java`，Notify 的 `service/NotifyNoticeService.java`、`service/NotifyNoticePublisherService.java`、`usecase/NotificationApplicationUseCase.java`、`service/runtime/NotificationApplicationRuntimeService.java`、`dao/NotifyPersistenceDao.java`，以及父仓库的 `10-cde-base-ddl.sql`、`50-cde-base-dml.sql`。

## 统一通知调用规范

业务模块需要发送站内信、短信或邮件时，只依赖 `wta-api` 的 `org.namewta.notify.api.NotificationApplicationService`，在本模块 Service 中构造 `NotificationCommand`，并提供稳定的 `idempotencyKey`。不要直接注入 Notify Mapper、Notify Entity、`NotifyClient` 或具体渠道 SDK；`NotifyClient` 只由 `wta-notify` 的渠道适配器使用。

- `recipientType=USER`：传递已授权的用户 ID；由 Notify 在提交和发布时再次过滤停用、删除或不存在的用户。
- `recipientType=ALL`：`recipientIds` 必须为空；发布时分页读取正常用户，单次最多 100000 人。
- `recipientType=PHONE/EMAIL`：仅用于明确的外部收件人合同，联系方式允许以明文保存在本次投递快照中。
- `channels` 只允许 `IN_APP`、`SMS`、`MAIL`；未实现渠道不得写入命令。未选择时由公告入口默认 `IN_APP`。
- MAIL/SMS 调用方只传场景编码、模板码和已声明变量，不要传 `providerKey` 或写死完整句子。验证码场景编码为 `auth-captcha`。
- 邮件 SMTP 与短信厂商账号、热配文案、模板码和收件人拦截的运行时权威是通知配置表，不是 YAML。YAML 最多保留 SMS4J 框架项（如 `config-type`）。
- 提交只写入 Intent、Recipient、Delivery 和 Outbox；MAIL 附件另在同一事务写 `notify_intent_attachment` 并由 System 对真实关系主键绑定来源 `sys_oss_ref`。不要在业务事务中等待 Provider I/O。由 Outbox Worker 负责有限重试、幂等和死信，当前自定义回执必须带 `eventId`、短时窗口内的 `timestamp`、账号 `providerKey` 并通过原文 HMAC 验签；这不是腾讯/阿里原生推送格式。缺绑定、账号停用或限额用尽时该渠道失败关闭，不改选其他已启用账号。
- `NotificationCommand.attachmentOssIds` 只接受十进制字符串 ID，去重保序；空列表全链路不读 OSS。附件源由当前已认证 User+Client 授权，Intent 固定原 actor；System 是 `sys_oss`、目标 PRIVATE 配置和远端复制的 owner，Notify 是关系与复制状态的 owner，common 只持快照 SPI。目标 `NOT_READY` 行及真实引用在远端 PUT 前持久化；READY 才能物化，COPY_UNKNOWN 保持人工核对且不能盲重传或释放。默认去重后 20 件、10 MiB/件、25 MiB/意图，配置键见统一通知规范。多条 MAIL delivery 复用同 Intent 快照；取消/截止安全回收需要全部明确未发且无活租约，不能由单 delivery cleanup 删除。物化之后物理 SMTP 前再查 DB 截止与租约。
- `demo-mail` 的中心包装模板只需要 title/content，没有业务 `path`；notice/workflow 仍要求链接变量，不能把演示特例扩散为业务模板规则。
- 站内消息和消息盒子统一读取 `/notify/inbox`；实时提示只能使用 `/resource/message/ticket` 签发的一次性短时票据，禁止把长期 Bearer Token 放入 URL。

## 账号预设与回执事实

- 基座提供腾讯/阿里短信、QQ/163/腾讯企业邮箱 SMTP 五个停用账号，凭据为空、场景未绑定。前端新增账号提供同样的预设；短信仍需用户自己的审核签名、模板码，腾讯还需 SMS SDK AppID。
- `NotifyConfigService` 校验启用凭据和腾讯模板参数位置；账号渠道/configKey 不可改名，已选短信厂商不可替换。`NotifyChannelAccount` 逻辑删除保留唯一配置标识，旧回执命名空间不可分配给新账号。
- common-sms 的 `Sms4jNotificationProviderResolver` 保留阿里 `BizId`、腾讯匹配收件人的 `SerialNo`，不将请求 `RequestId` 当消息号；腾讯参数按连续数字位置排序。
- 腾讯/阿里原生送达通过固定HTTPS端点的TC3/ACS3签名查询取得，不直接接受无本地HMAC合同的原生推送。每次领取一条SMS ACCEPTED，`receipt_query_at` CAS预约15分钟；外部I/O在事务外，结果经`ProviderCallbackUseCase.confirm`和相同持久receipt事务确认。受理71小时后停止自动查询，仍未确认保持ACCEPTED；缺流水号、停用账号、歧义/截断不触发发送。
- `ProviderCallbackService -> NotifyProviderReceiptDao -> NotifyProviderReceiptMapper` 在当前动态事务中写 `notify_provider_receipt`，随后推进 Delivery 与聚合。持久摘要按渠道/账号/事件隔离，不含每次重签的传输时间戳。共享消息号时必须明确目标；查询最多两条，歧义拒绝。无进程内去重缓存或自动清理。
- `/notify/callback/{channel}` 只接受自定义 JSON/HMAC；HTTP 200 且响应 code=200 确认完成或相同事实重复，401 验签失败、409 事件冲突、422 关联歧义/非法参数、503 未关联或可重试业务失败。QQ/163/企业邮箱 SMTP 不提供本接口的送达事件。

## 验证

```bash
node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-notify --mode layered
./mvnw -pl wta-modules/wta-notify -am test
node --test release-artifacts/tests/notify-baseline-contract.test.mjs
```
