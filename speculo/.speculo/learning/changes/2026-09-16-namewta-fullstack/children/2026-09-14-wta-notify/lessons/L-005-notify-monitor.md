---
lesson_id: L-005
objective_ids: [OBJ-05]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-N-05, S-N-07, S-N-09, S-N-10]
---

# Lesson 005：监控快照与投递日志是两条查询链

## 学完你能做什么

能口述 `GET /notify/monitor/snapshot` 和 `GET /notify/monitor/deliveries`。`NotificationMonitorController` 只注入 `NotificationMonitorUseCase`。UseCase 有两个下游字段：`NotificationApplicationService notificationService`（snapshot 走 `query`）和 `NotificationMonitorService monitorService`（deliveries 走 `listDeliveries` 再 `NotificationDeliveryView.from`）。能说明 snapshot 的 `includeContent` 在这条链上被写死为 `false`，以及 View 故意丢掉 `targetValue`。

## 先把宏观地图放在桌上

把监控页当成「邮局大厅的电子看板」。一块板显示某一票件的总状态（snapshot），另一块板按人/渠道/状态刷投递流水（deliveries）。看板不让你改地址，也不给你看完整手机号邮箱。

```text
GET /notify/monitor/snapshot?notificationId=   权限 notify:monitor:query
GET /notify/monitor/deliveries?userId&channel&status  权限 notify:monitor:list
           |
           v
NotificationMonitorController
  注入 NotificationMonitorUseCase monitorUseCase
           |
           v
NotificationMonitorUseCase
  notificationService: NotificationApplicationService
  monitorService:      NotificationMonitorService
           |
           +-- snapshot(id) --> notificationService.query(new NotificationQuery(id, false))
           |                         \--> 运行时实现是 NotificationApplicationUseCase.query
           |                                --> NotificationApplicationRuntimeService.query
           |
           +-- deliveries(...) --> monitorService.listDeliveries
                                      --> dao.monitorDeliveries(..., 500)
                                      --> NotificationDeliveryView.from
```

**类比失效处：** 看板比喻不包含「为什么聚合状态是 PARTIAL_FAILURE」。那是 `NotificationAggregatePolicy` 在投递/回调之后写入 Intent 的事。监控只读已经写好的状态。

## 核心概念与机制

### 直觉讲解

为什么 snapshot 不直接查 DAO？因为对外稳定合同是 `NotificationApplicationService.query`：业务模块和监控页看到同一份 `NotificationSnapshot`。UseCase 选择 `includeContent=false`，监控页即使想看正文也走不通这条参数。

为什么 deliveries 不走同一个 query？query 按**一个** notificationId 返回该意图下的投递回执；监控列表要跨意图、按 userId/channel/status 过滤。所以第二字段 `NotificationMonitorService` 专门 `dao.monitorDeliveries`。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 通知快照 | notification snapshot | `NotificationSnapshot`：意图 ID、聚合状态、创建时间、每条 `DeliveryReceipt`（用户、渠道、状态、providerMessageId） |
| 投递投影 | delivery view | `NotificationDeliveryView` record：从 `NotifyDelivery` 抽出监控安全字段，不含目标地址 |
| 监控查询 | monitor query | `NotificationMonitorService.listDeliveries`：可选 userId/channel/status，最多 500 条，按 createTime 倒序 |
| 公共查询合同 | application query | `NotificationQuery(notificationId, includeContent)`；监控调用时第二参数恒为 false |
| 聚合状态 | aggregate status | Intent 上已持久化的状态；本切片不重新计算 `NotificationAggregatePolicy` |

规范要求监控能看跨渠道发送与供应商状态。Java 用 `providerMessageId` 和 `errorCode` 满足「供应商侧线索」，但 View 注释写明「不暴露内部实体和敏感目标」。

### 机制/因果链

1. **snapshot。** Controller 要求 `notificationId` 请求参数 → `monitorUseCase.snapshot` → `notificationService.query(new NotificationQuery(id, false))`。实现落在 `NotificationApplicationUseCase.query`（无 `@DSTransactional`）→ `RuntimeService.query`：解析正整数 ID，`dao.intent` 找不到则「通知不存在」；然后 `dao.deliveries(intentId)` 映射为 `DeliveryReceipt`。`includeContent` **未被读取**，正文快照不会因为这个参数出现。
2. **deliveries。** 三个过滤参数都 `required=false`。`monitorService.listDeliveries` → `dao.monitorDeliveries(userId, channel, status, 500)`：非空才加 eq 条件，`limit` 再 clamp 到 1..500。UseCase 对每条 `NotificationDeliveryView.from`。
3. **View.from 包含什么。** `deliveryId`、`intentId`、`userId`、`channel`、`status`、`attemptCount`、`providerMessageId`、`errorCode`、`acceptedAt`、`deliveredAt`、`readAt`、`createTime`。**不包含** `targetValue`、`providerKey`、错误长文 `errorMessage`。
4. **和收件箱 readTime 的区别。** View 的 `readAt` 来自投递实体 `NotifyDelivery.readAt`。Inbox 的 `readTime` 来自 `NotifyMessageRecipient`。本切片不把两者对账。

### 图、表或文本图

**图题 / caption：** 同一 UseCase 上的两条只读链。

```text
                 NotificationMonitorUseCase
                    /                      \
                   /                        \
          snapshot()                       deliveries()
               |                                |
               v                                v
 NotificationApplicationService         NotificationMonitorService
   query(NotificationQuery)               listDeliveries
               |                                |
               v                                v
 RuntimeService.query                   NotifyNotificationDao
   intent + deliveries(intentId)          monitorDeliveries(filters, 500)
               |                                |
               v                                v
     NotificationSnapshot              List<NotifyDelivery>
                                               |
                                               v
                                    NotificationDeliveryView
                                    （去掉 targetValue）
```

**文字等价物：** 左边问「这一号通知现在怎样」，必须经过公共应用查询合同，所以和业务模块 `query` 同构。右边问「最近哪些投递符合筛选」，走监控专用 DAO，再投影成不含联系方式的 View。两边都不写库，也都不领取 Outbox。

**图的边界：** 不画出 retry/cancel。那是 L-007 的写路径。不画出 HMAC 回调如何把 status 改成 DELIVERED。

### 正例、反例与边界

**正例 1：** `snapshot?notificationId=1001`。权限 `notify:monitor:query`。得到该意图的聚合状态和每条渠道回执。

**正例 2：** `deliveries?channel=SMS&status=FAILED`。不传 userId，DAO 不加 user 条件，最多 500 条失败短信投递。

**正例 3：** 投递行有手机号在 `targetValue`。监控 JSON 里看不到它，只能看到 `userId` 与 `providerMessageId`。

**反例 1：** 把监控 snapshot 的 ID 当成公告 `noticeId`。公告发布后的意图 ID 是新生成的 `intentId`，只是幂等键里带了 noticeId。

**反例 2：** 调用 `query` 时幻想 `includeContent=true` 能从监控接口出来。Controller 没有该参数；UseCase 写死 false；即便 Runtime 收到 true，当前实现也不读这个标志。

**反例 3：** 在监控 Controller 注入 Mapper 做 update。本类只有两个 GET。

**边界：** 空筛选等于「最近 500 条投递」，不是分页游标。channel/status 空串在 DAO 里被 `isBlank` 当成未筛选。`notificationId` 非正整数会在 Runtime 变成「通知编号必须为正整数」。

**规范冲突记录：** 规范把「查询」列在 `NotificationApplicationService` 上，监控 snapshot 遵守这一点。规范没有单独定义 monitor HTTP；工作树用 `/notify/monitor` 两个 GET。Java 优先。

## 变式与迁移

- **变式 A：只知道用户。** 用 deliveries 的 userId，从 View 的 `intentId` 再去 snapshot。不要用 inbox 的 messageId 当 notificationId——虽然 IN_APP persist 用 intentId 当 messageId，这是巧合式相等，监控仍应走投递/意图 ID。
- **变式 B：对账供应商。** View 提供 `providerMessageId` + `errorCode`，足够去厂商控制台搜；本系统不在监控接口返回密钥。
- **迁移到回调：** 回调成功会 `refreshAggregate`，下一次 snapshot 看到新的 Intent 状态（L-006）。
- **迁移到应用 API：** 管理端 `GET /notify/notification/{id}` 也走同一个 `notificationService.query`，但那里 `includeContent` 可由请求参数传入（L-007）。监控链故意关掉。

## 常见误区

1. **「MonitorUseCase 只包了一个 MonitorService。」** 还有 `NotificationApplicationService` 字段，snapshot 完全走它。
2. **「deliveries 返回实体。」** 返回的是 `NotificationDeliveryView`，经过 `from`。
3. **「500 是分页页大小。」** 它是硬上限，没有 pageNum。
4. **「readAt 就是收件箱已读。」** 字段同名不同表。
5. **「监控可以重试失败。」** 重试在 `NotificationController.retry`（L-007），本课两个 GET 不能改状态。

## 非评分暂停

想一想：若你在 deliveries 里看到 `status=UNDELIVERABLE` 且没有 `providerMessageId`，更可能是提交时缺手机号/邮箱（L-007 不写 Outbox），还是供应商拒信？你还需要哪一个字段（errorCode）来区分？View 里有没有 errorMessage？

再想：为什么 snapshot 要绕一圈公共 `query`，而不是 UseCase 直接 `dao.intent`？

## 总结、词汇表与下一步

- Controller → `NotificationMonitorUseCase` → (`NotificationApplicationService.query` | `NotificationMonitorService.listDeliveries`)。
- snapshot 固定 `NotificationQuery(id, false)`；deliveries 上限 500 且投影脱敏。
- 词汇：snapshot、delivery view、aggregate status、includeContent、monitor query。

下一步：L-006 供应商如何安全地把投递状态写回来。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-05 | `NotificationMonitorController.java`、`NotificationMonitorUseCase.java` | 两个 GET、两个下游字段、Query(false) | `controller/admin`、`usecase` | 2026-09-14 |
| S-N-07 | `NotificationApplicationUseCase.java`、`NotificationApplicationService.java` | snapshot 最终进入 query 合同 | `usecase`、`wta-api` | 2026-09-14 |
| S-N-09 | `NotificationMonitorService.java`、`NotifyNotificationDao.monitorDeliveries`、`NotificationDeliveryView.java`、`NotificationApplicationRuntimeService.query` | 500 条、投影字段、includeContent 未使用 | `service/runtime`、`dao`、`domain/vo` | 2026-09-14 |
| S-N-10 | `notification.md` | 业务应经 public API 查询；不得直连 Mapper | 「调用入口」 | 2026-09-14 |
