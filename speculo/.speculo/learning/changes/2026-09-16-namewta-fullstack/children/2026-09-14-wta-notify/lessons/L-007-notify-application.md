---
lesson_id: L-007
objective_ids: [OBJ-07]
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
source_ids: [S-N-07, S-N-09, S-N-10, S-N-11]
---

# Lesson 007：统一通知应用 API：submit / query / retry / cancel

## 学完你能做什么

能口述 `/notify/notification` 四件事，并点名：**HTTP 的 `NotificationController` 注入的是接口 `NotificationApplicationService`**，不是 UseCase 类名；**`NotificationApplicationUseCase` implements `NotificationApplicationService`**，它唯一字段是 `NotificationApplicationRuntimeService runtimeService`。能说明 submit 只写 Intent/Recipient/Delivery/Outbox 并登记唤醒，query/retry/cancel 各自改什么、不改什么。能指出 `SYNC` 不会在 submit 线程里调用供应商。

## 先把宏观地图放在桌上

把应用 API 当成「邮局柜台的正式单据」。业务模块和通知管理页都填同一张 `NotificationCommand`。柜台盖章后给出回执，信件进发件箱，投递员稍后才出门。这就是「提交阶段不执行 Provider I/O」。

```text
业务模块 / 管理页
  POST /notify/notification                      submit
  GET  /notify/notification/{notificationId}     query
  POST /notify/notification/{notificationId}/retry
  POST /notify/notification/{notificationId}/cancel
           |
           v
NotificationController
  字段类型：NotificationApplicationService notificationService
           |
           |  Spring 注入唯一实现
           v
NotificationApplicationUseCase  implements NotificationApplicationService
  字段：NotificationApplicationRuntimeService runtimeService
  submit/retry/cancel 带 @DSTransactional；query 不带
           |
           v
NotificationApplicationRuntimeService
  dao: NotifyNotificationDao
  userService: UserService
  dispatchService: DispatchNotificationService   （submit 里只可能 refreshAggregate）
  events: ApplicationEventPublisher            （requestOutboxWake）
```

**类比失效处：** 柜台比喻不包含租约。领取是 L-008。`DispatchNotificationService` 虽被 Runtime 注入，submit 成功路径调用的是 `requestOutboxWake` 或空 Outbox 时的 `refreshAggregate`，不是 `dispatch(outbox)`。

## 核心概念与机制

### 直觉讲解

规范说：业务只能依赖 `wta-api` 的 `NotificationApplicationService`。Java 完全按这句话接线：Controller 和公告 Publisher、测试发送、监控 snapshot 都依赖接口。实现类把事务留在 UseCase，把校验、幂等、解析用户、写四张表留在 RuntimeService。

小孩子寄信：先在柜台登记收件人复印件（Recipient 上的 `targetSnapshotJson`），每条渠道一张运单（Delivery）。有电话/邮箱的运单才放进待寄篮（Outbox `READY`）。缺地址的运单当场盖 `UNDELIVERABLE`，不进篮子。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 公共应用服务 | NotificationApplicationService | `wta-api` 接口：`submit`/`query`/`retry`/`cancel` |
| 应用用例 | NotificationApplicationUseCase | 模块内唯一实现；委托 Runtime |
| 意图 | intent | `NotifyIntent`：一次提交的业务事实，含 `idempotencyKey`、mode、strategy |
| 投递 | delivery | `NotifyDelivery`：某接收者 × 某渠道一行 |
| 发件箱任务 | outbox | `NotifyOutbox`：可被 Worker claim 的 READY 行 |
| 幂等键 | idempotency key | `(appId, idempotencyKey)` 命中则返回已有回执，不新建 |
| 执行模式 | mode | Command compact 构造默认 `ASYNC`；`SYNC` 仍走 Outbox，只影响回执 `queued` |

规范「UseCase 负责幂等和目标解析」：**冲突（Java 优先）** 这两件事的代码在 `NotificationApplicationRuntimeService.findDuplicate` / `resolveUsers`。UseCase 只加事务并转发。

规范「不得把完整句子写入 `templateParams.content`」针对 MAIL/SMS 正文权威在场景绑定。公告 Publisher 仍把 title/content 放进 params，供 Catalog 变量和 IN_APP 快照使用；真正 SMTP 正文由 `NotifySendPlanner` 渲染绑定表，不在本课 HTTP。

### 机制/因果链

**submit**

1. Controller 权限 `notify:notification:submit`，`@Valid NotificationCommand`。
2. `validate`：appId/sceneCode/templateCode/channels 非空；recipientType 只能是 `ALL`/`USER`/`PHONE`/`EMAIL`（大小写规范化）。ALL 不得同时带 ID；非 ALL 必须有 ID。USER 的 ID 必须是正整数。expires 不能早于 scheduled。
3. `findDuplicate`：有幂等键则 `dao.intentByIdempotency`；命中直接 `receipt`。
4. `resolveUsers`：USER → `selectNotificationUsers`；ALL → `selectAllActiveUsers` 每批 1000，超过 100000 拒绝；PHONE（及解析阶段的 SMS 别名）→ 无 userId 的电话；EMAIL（及 MAIL 别名）→ 邮箱。空列表抛「没有可接收通知的正常用户」。
5. 插入 Intent：`status=QUEUED`，title/content/path 快照取自 templateParams。`DuplicateKeyException` 再查一次幂等。
6. 对每个用户插 Recipient（电话邮箱进 `targetSnapshotJson`）。对每个渠道插 Delivery：`IN_APP` 目标是 userId 字符串，SMS 用 phone，MAIL 用 email。目标 blank → 状态 `UNDELIVERABLE` 并写 `TARGET_UNAVAILABLE`，**不**建 Outbox。否则 `PENDING` + Outbox `READY`，`availableAt` 为 scheduled 或现在，`maxAttempts=5`。
7. 若一个 Outbox 都没有：`dispatchService.refreshAggregate`。否则 `events.publishEvent(new NotifyOutboxWakeRequestedEvent(firstOutboxId))`。真正 Redis 发布在 AFTER_COMMIT（L-008）。
8. 回执：`queued = mode==ASYNC 且存在 PENDING`。`followUpRequired`：非 IN_APP 且状态 ACCEPTED/UNKNOWN。

**query**

9. 权限 `notify:notification:query`。`includeContent` 默认 false。Runtime **不读取**该布尔，构造的 `NotificationSnapshot` 不含正文。

**retry**

10. body 可空：空则 `NotificationRetryCommand(id, null, "manual", null)`。只挑选 Delivery 状态属于 `FAILED`/`UNKNOWN` 的行。`markDeliveryForRetry` 必须更新 1 行（PENDING、清空 error）。然后 `requeueOutbox`；0 行则新建 READY Outbox。意图改回 `QUEUED`。有成功入队则 `requestOutboxWake`。

**cancel**

11. body 可空：空则 reason=`manual`。意图已是 `DELIVERED`/`CANCELLED`/`FAILED` 则拒绝。否则意图 `CANCELLED`，并把该意图下 **PENDING** 投递批量改成 `CANCELLED`。不在这里 finish Outbox；Worker 看到 Delivery 非 PENDING 会把 Outbox 标 DONE（投递服务，下节只点到为止）。

### 图、表或文本图

**图题 / caption：** submit 写哪些行，以及何时唤醒。

```text
NotificationCommand
        |
        v
  duplicate key? --yes--> 返回旧 Receipt
        | no
        v
  resolveUsers()  --> 空? 失败
        |
        v
  insert NotifyIntent (QUEUED)
        |
        +-- 每个用户: insert NotifyRecipient
        |      |
        |      +-- 每个渠道: insert NotifyDelivery
        |             |
        |             +-- 无目标: UNDELIVERABLE（无 Outbox）
        |             +-- 有目标: PENDING + insert NotifyOutbox READY
        v
  outboxes 空? --yes--> refreshAggregate
        | no
        v
  publish NotifyOutboxWakeRequestedEvent(hint)
        |
        v
  Receipt(queued?, followUpRequired?, deliveries)
```

**文字等价物：** 一次 submit 最多写四类行：意图、接收者、投递、发件箱。缺联系方式的渠道停在 UNDELIVERABLE，不进入领取队列。只要有一条 READY Outbox，就在当前事务里登记「提交后请叫醒 Worker」，叫醒动作本身发生在事务提交之后。回执里的 queued 只在 ASYNC 且仍有 PENDING 时为真。

**图的边界：** 不画出 claim SQL、短信 SDK、回调 HMAC。retry/cancel 是另外的入口，不画进这张 submit 图。

### 正例、反例与边界

**正例 1：** 业务 `recipientType=USER`，渠道仅 `IN_APP`。解析出用户 → Delivery PENDING → Outbox READY → 唤醒。Inbox 要等 Worker persist。

**正例 2：** 同一 `appId`+`idempotencyKey` 重试 submit。第一次 insert 成功；第二次 `findDuplicate` 或 DuplicateKey 回收，返回已有 intentId。

**正例 3：** 配置测试（L-003）mode=SYNC。仍写 Outbox。Receipt.queued 为 false（因为 queued 公式要求 ASYNC）。

**反例 1：** `recipientType=USER_TYPE`。`validate` 直接「接收者类型不受支持」。USER_TYPE 只活在公告草稿。

**反例 2：** ALL 同时塞了一批 userIds。校验失败：「全部用户通知不能同时指定接收者编号」。

**反例 3：** 在 Controller 里 `new DispatchNotificationService().dispatch`。分层禁止；submit 注释写明不在业务事务里做 Provider I/O。

**边界：** `resolveUsers` 额外认识 SMS/MAIL 别名，但 `validate` 只放行 ALL/USER/PHONE/EMAIL，所以 HTTP 合法请求进不了别名分支。ALL 分页上限 100000。Outbox `maxAttempts` 写死 5。cancel 不把已 ACCEPTED 的投递改成 CANCELLED，只改 PENDING。

## 变式与迁移

- **变式 A：retry 时 Outbox 仍在 WAITING_RECEIPT。** `requeue` SQL 只接受 `WAITING_RECEIPT` 或 `DEAD_LETTER`，把它变回 READY 并清租约。若没有这种行（例如从未建 Outbox），则 insert 新 Outbox。
- **变式 B：query 的 includeContent=true。** Controller 会把它放进 `NotificationQuery`；Runtime 当前忽略。不要在课上假装正文会返回。
- **迁移到公告：** Publisher 调的就是这个 `submit`，appId=`notify`，scene=`notice-published`，mode=ASYNC。
- **迁移到 Outbox：** `requestOutboxWake` 是 L-008 的上游开关。没有它，仍有 60 秒兜底 poll。

## 常见误区

1. **「Controller 字段是 UseCase。」** 字段类型是 `NotificationApplicationService`。UseCase 是实现。
2. **「SYNC 等于现在发送。」** Runtime submit 无供应商分支；queued 计算才看 mode。
3. **「幂等在 UseCase。」** 在 RuntimeService。规范与代码冲突时跟代码。
4. **「cancel 会删消息。」** 它改意图状态和 PENDING 投递；inbox 已 persist 的行不在这个方法里删除。
5. **「retry 重试所有渠道。」** 过滤器只有 FAILED 与 UNKNOWN，不含 UNDELIVERABLE。缺号码的要先补用户资料再重新 submit。

## 非评分暂停

想一想：一条 MAIL 投递因为用户没有邮箱变成 UNDELIVERABLE。此时 `outboxes.isEmpty()` 可能为真（若这是唯一渠道）。谁负责把 Intent 聚合成失败态？指出 `refreshAggregate` 这一调用点。

再想：为什么 `submit` 要 `@DSTransactional` 而 `query` 不要？若 query 也开事务，监控页会怎样（不必量化，只要说出「没必要写锁」这类原因）？

## 总结、词汇表与下一步

- `NotificationController` → 接口 `NotificationApplicationService` → 实现 `NotificationApplicationUseCase` → `NotificationApplicationRuntimeService`。
- 方法：`submit` 写事实+Outbox+唤醒；`query` 读意图与投递；`retry` 仅 FAILED/UNKNOWN；`cancel` 拒终态、改 PENDING。
- 词汇：intent、delivery、outbox、idempotency key、queued、NotificationApplicationService。

下一步：L-008 看 Worker 如何 claim 这些 READY 行，以及 wake 如何接到这条链。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-07 | `NotificationController.java`、`NotificationApplicationUseCase.java`、`NotificationApplicationService.java` | HTTP、接口实现、事务委托 | `controller/admin`、`usecase`、`wta-api` | 2026-09-14 |
| S-N-09 | `NotificationApplicationRuntimeService.java`、`NotifyNotificationDao.java`、`NotifyOutboxWakeRequestedEvent.java` | 校验、解析、四表、唤醒、retry/cancel | `service/runtime`、`dao`、`support/outbox` | 2026-09-14 |
| S-N-10 | `notification.md` | 只能依赖 public API；提交写意图/接收者/Outbox；默认异步 | 「调用入口」「后端分层」 | 2026-09-14 |
| S-N-11 | 登记表 layered | Controller 不直连 Runtime | 模块事实 | 2026-09-14 |
