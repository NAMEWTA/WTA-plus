# 统一通知规范

本文件是业务通知的唯一实现约定。通知需求应先读取本文件，再按模块加载 `wta-module-guide` 的 Notify 事实地图；不要重新创建消息盒子、站内信表或渠道 SDK 包装。

## 调用入口

业务模块只能依赖 `wta-api` 的 `org.namewta.notify.api.NotificationApplicationService`，通过 `NotificationCommand` 提交通知意图。构造命令时必须明确 `appId`、`sceneCode`、`bizType`、`recipientType`、`recipientIds`、`channels` 和稳定的 `idempotencyKey`；模板参数放入 `templateParams`，不要把手机号、邮箱或供应商密钥写入日志。

```java
notificationService.submit(new NotificationCommand(
    "profile", "person-rebind", "PERSON_REBIND",
    userId.toString(), "USER", List.of(userId.toString()),
    "person-rebind", Map.of("name", displayName),
    List.of(NotificationChannel.IN_APP), NotificationStrategy.ALL,
    NotificationMode.ASYNC, 0, null, null,
    "profile:person-rebind:" + userId + ":" + requestId, Map.of()));
```

`NotificationApplicationService` 还提供查询、失败重试和未完成通知取消。Controller、Workflow 页面和其他业务实现不得直接依赖通知 Mapper、ServiceImpl、Outbox 或渠道客户端。

新通知只支持 `NotificationStrategy.ALL`、`NotificationMode.ASYNC` 和 `priority=0`。历史策略枚举常量只供旧记录读取，不表示已实现顺序回退、升级编排或优先级队列；提交在幂等查询前拒绝其他值，人工重试也拒绝原 Intent 的不支持值。查询和取消历史通知仍可使用原控制面。Worker 对旧任务先在活租约与统一锁序内核对 T-39 截止和外呼来源：确定未发才以固定码关闭，来源不明的外部任务进入 `UNKNOWN/WAITING_RECEIPT` 待核对，已受理与已送达事实不得倒退；站内信先核同 Intent 的消息及本人关系，不新增重复提示或伪造供应商尝试。

人工重试以 URL/命令中的通知主键定位 Intent；指定 `deliveryId` 只能重排该 Intent 所属的投递，取消始终作用整个通知。当前不保存独立的人工重试幂等结果，非空 retry `idempotencyKey` 明确拒绝；重复空键请求由当前状态和 Outbox 行 CAS 得到零排队结果。`RetryReceipt.queuedCount` 是本次实际重排数，零时返回持久聚合状态，不写伪 QUEUED。只有有剩余预算、无活租约且可证明尚未调用供应商的固定本地失败可以复用原 DONE Outbox；外部 UNKNOWN/WAITING_RECEIPT、旧泛 FAILED、已受理和预算耗尽均不可凭人工请求清零次数、删除 Redis 键或重新发送。IN_APP 历史 UNKNOWN 仅在同 Intent、原消息/本人关系可由原子幂等路径核对、单一无租约任务和剩余预算时复用原 WAITING_RECEIPT；再次完成仅对新关系登记提交后提示。

通知 `scheduledAt` 按持久化秒精度向上归整，`expiresAt` 向下归整；提交与人工重试使用原 Intent 的绝对截止，数据库时间达到截止即拒绝新投递。过期 READY 仍由 Worker 领取以便短事务终结，不在领取 SQL 中过滤。新建外部 Outbox 的内部未外呼标记仅在首次 READY/零结果且本次由 READY 领取时可证明未发送；重领 PROCESSING、旧无标记、已进入 Provider 或结果不明都不得伪写 `NOTIFICATION_EXPIRED`，而应留待核对。IN_APP 依据同事务消息与本人关系事实收敛；外部请求一旦进入 Provider，截止时间不能抹掉真实受理或未知结果。验证码命令和缓存共用一次生成的绝对截止，缓存通过 `RedisUtils.setCacheObjectUntil` 原子写入，不在提交后重启相对 TTL；PersonRebind 安全告知不是验证码，不套用其两分钟期限。

## 目标与渠道

- `recipientType=ALL`：发布时解析当前正常且未删除用户，不把全量用户加载到前端。
- `recipientType=USER`：`recipientIds` 必须为用户 ID；服务端在保存和发布时校验数据权限、状态和删除标记。
- `recipientType=PHONE` / `EMAIL`：只用于明确的外部收件人场景，输入必须经过业务授权和格式校验。
- 渠道仅使用已实现的 `IN_APP`、`SMS`、`MAIL`；未知历史值只能按“其他”展示，不能生成投递任务。
- MAIL/SMS 正文权威在通知中心「通知配置」的场景绑定：邮件热配 `${name}` 文案，短信只绑定供应商模板码与参数映射。调用方不得传 `providerKey`，也不得把完整句子写入 `templateParams.content`。
- SMTP 发件账号和短信厂商凭据保存在 `notify_channel_account`；YAML 不再作为发件人、`sms.blends` 或全局 `restricted`/`minute-max`/`account-max` 的运行时来源。
- 业务通知默认异步。站内信、通知收件箱和消息盒子读取同一 `/notify/inbox` 数据源；SSE/WebSocket 只发送刷新事件，不承担持久化。

## 后端分层与状态

`controller -> usecase -> service -> dao -> mapper/XML` 是 `wta-notify` 的固定链路。UseCase 负责事务、目标解析、幂等和状态迁移；Service 负责规则；DAO 封装 MyBatis-Plus Wrapper、分页、锁和批量更新；Provider/Outbox Worker/Callback 只能通过端口或事件接入。

发布必须在同一业务事务内写入通知意图、接收者快照和 Outbox。Worker 使用租约 owner/token 更新，续租失败时禁止继续写入投递结果。当前自定义回执入口必须验证 HMAC 原文、`providerKey`、`eventId` 和时间窗，状态只能单向升级，重复事件不重复刷新聚合状态。原文从入口有界缓存读取，不使用 XSS 改写后的视图验签；回执含精确收件地址，HTTP/操作日志必须只保留元数据及脱敏摘要。`notify_provider_receipt` 按渠道、账号配置标识和事件编号持久去重，凭据与状态、聚合同事务；不同事实复用事件编号必须拒绝，早到未关联不得消费事件，记录不自动过期。账号标识创建后不可变，逻辑删除仍保留唯一命名空间。腾讯/阿里原生推送没有本接口的 HMAC 合同，不能直接接入或宣称已支持；SMTP 受理也不等于送达。

腾讯/阿里的原生送达核对复用账号密钥进行官方只读签名查询，固定HTTPS端点，不通过禁用验签接收原生推送。查询预约只修改Delivery的`receipt_query_at`，不得复用发送租约或重新发送；供应商I/O必须在事务外。确认结果复用持久receipt、状态与聚合事务，内部`native-sms:`事件前缀不得由自定义HTTP回执占用。当前自动核对窗口为71小时；超窗、缺流水号、查询失败/截断/歧义均保持已受理，不推断送达。

外部渠道失败只有在供应商响应明确证明整批目标均未发送、可重试，并且 Redis 幂等当前 owner 原子转为 `RETRYABLE` 后，Outbox 才能按自身退避再次物理调用。已受理、结果未知、混合目标、旧缓存 `FAILED` 和 Redis 转态失败都不得推断为可重发；旧完成态须保留到原窗口结束，不清空缓存。`RETRYABLE` 保留原请求摘要和剩余 TTL，同一请求重取也必须换 owner nonce，旧 owner 不能完成或释放新尝试。当前生产证据仅覆盖由账号注册器创建、明确关闭 SMS4J 内部重试的确切 blend 实例，以及腾讯单号码 30 秒限频的完整结构化拒绝；其他厂商码、第三方覆盖的 blend、邮件传输异常与供应商响应不明均按未知处理。公开结果类型或 SDK 适配器扩展时须保留此来源证明，不得凭通用 `FAILED`、错误消息或供应商文案赋予重发权。

## 前端与权限

通知 Web Domain 提供类型化 API 和目标选择器；App 只组合运行时目录端口。保存草稿与发布分为两个操作，发布前展示标题、渠道和目标范围；所有异步请求需要 loading、错误恢复、重复点击保护和过期响应保护。权限指令只控制可见性，后端仍必须执行权限和数据范围校验。

通知配置页 `notify/config/index` 使用权限 `notify:config:list|query|add|edit|remove|test`。密钥只写不读；短信 TAB 不得提供自由正文框。

## 验证

修改通知域后至少执行：

```bash
./mvnw -pl wta-modules/wta-notify -am test
node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-notify --mode layered
pnpm --dir frontend typecheck
node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs
```

同时验证 OpenAPI、权限菜单、通知收件箱/消息盒子一致性，以及 ALL、USER、USER_TYPE、重复发布、回调重放和租约失效场景。
