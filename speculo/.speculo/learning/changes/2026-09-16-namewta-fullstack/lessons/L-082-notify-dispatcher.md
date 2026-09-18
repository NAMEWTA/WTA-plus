---
lesson_id: L-082
objective_ids: [OBJ-82]
claimed_cells:
  - B:NotifyClient.send
  - B:NotifyDispatcher.send
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-doors-and-bean
    minutes: 8
  - segment: send-causal-chain
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-007, S-015, S-L002-01, S-L082-01, S-L082-02, S-L082-03, S-L082-04, S-L082-05, S-L082-06, S-L082-07, S-L082-08]
---

# Lesson 082：宏观同步邮筒——`NotifyClient.send` / `NotifyDispatcher.send` 才是渠道入口

## 学完你能做什么

打开工具间 `backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/` 的 `NotifyClient` 和 `NotifyDispatcher`，你能**口述同步渠道入口**：业务房间把通知意图交给 `wta-api` 的 `NotificationApplicationService.submit`；Outbox 工人领取之后，只有邮件/短信这条河才会喊 `notifyClient.send(request)`；Spring 容器里那只手的类型是 `NotifyClient`，真正跑步的人是 `NotifyDispatcher`。不是 HTTP 窗，不是站内信落箱，不是供应商回调门铃，也不是业务模块自己去 new 短信 SDK。

口试名单就是矩阵 **(b)** 这一行，符号以**磁盘**为准：

1. **`B:NotifyClient.send`**（`@FunctionalInterface`，包 `org.namewta.common.notify.core`）：公开方法只有 `NotifyResult send(NotifyRequest request)`。JavaDoc 写「统一通知同步入口」。Spring 装配的 Bean **类型**是这张口，条件是 `@ConditionalOnMissingBean(NotifyClient.class)`。
2. **`B:NotifyDispatcher.send`**（`public final class NotifyDispatcher implements NotifyClient`）：同一支方法。JavaDoc 写「统一通知同步调度器」。它在调用线程里校验、占幂等坑、按需做附件快照、`registry.require(channel).send(...)`、聚合成 `NotifyResult`、发监控事件。只有 `status == ACCEPTED` 才把结果交还给调用方；否则扔 `NotifyDeliveryException`，失败细节在 `exception.result()`。

OBJ-82 原文：能口述 `NotifyClient.send` / `NotifyDispatcher` **与业务模块的同步入口**。档是**方法性状**。chain 覆盖列写成 `B:NotifyClient.send B:NotifyDispatcher.send`，前置 L-051。2026-09-17 工作树把「业务模块」四个字钉死成两句，不要揉成一句：

- **业务房间（profile / system / captcha / 公告发布）** 的同步入口是 `org.namewta.notify.api.NotificationApplicationService.submit(NotificationCommand)`。它们 **不** 注入 `NotifyClient`。
- **通知基础设施适配层** 的同步入口才是 `NotifyClient.send`。2026-09-17 生产源码里，唯一扣扳机的类是 `wta-notify` 的 `DispatchNotificationService.dispatch`：站内信走 `InAppNotificationPort`，MAIL/SMS 才 `notifyClient.send`。

口试先数门口，再数 `send` 这一枪：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `NotifyDispatcher` 是 Spring Bean 类型，业务 `@Autowired NotifyDispatcher` | **装配口是 `NotifyClient`。** `NotifyAutoConfiguration.notifyClient(...)` 返回 `new NotifyDispatcher(...)`。单测才直接 `new NotifyDispatcher` |
| 业务模块注入 `NotifyClient` 发验证码 | **没有。** `CaptchaController` 走 `NotificationApplicationService.submit`。测试方法名 `emailCaptchaUsesNotifyClientAndCachesOnlyAfterAccepted` **名不副实**，断言的是 `submit` |
| `NotifyClient` 有 `submit` / `query` / `retry` / `cancel` / `dispatch` | **没有。** 函数式接口只有 `send` |
| `NotifyChannel.IN_APP` | **没有这个常量。** 内置只有 `MAIL = "mail"`、`SMS = "sms"`。站内信不进 Dispatcher |
| `NotifyTarget.user("100")` 能发给供应商 | **拒绝。** 码 `LOGICAL_TARGET_NOT_SUPPORTED`，「common-notify 只接受物理目标」，供应商一次都不会被叫 |
| 模板可以不带正文快照 | **拒绝。** `NotifyTemplateContent` 且 `contentSnapshot` 空白 → `CONTENT_SNAPSHOT_REQUIRED`，发生在 Adapter 之前 |
| `send` 异步、自己写 Outbox | **同步。** 调用线程等到 Adapter 返回。Outbox 是 L-051/L-052 的通知楼，不在这间工具间 |
| `SKIPPED_DUPLICATE` 是 `send` 的返回值 | **事件里才有。** 重复命中已完成请求时，`send` 仍 `requireAccepted(原结果)`；监控事件另造一份 `SKIPPED_DUPLICATE` 且 `deliveries` 为空 |
| `wta-common-notify` 自己带测试 | **没有 `src/test`。** 契约测试在 `wta-admin/src/test/java/org/namewta/test/notify/` |
| 附件快照生产实现已经挂上 | **工作树没有** `implements NotifyAttachmentSnapshotService` / `NotifyLogIdGenerator` 的生产类。请求带了 `attachmentOssIds` 会 `ATTACHMENT_SNAPSHOT_NOT_CONFIGURED` |
| Skill 摘要「通知统一走 NotifyClient」= 业务入口 | **以规范 + 磁盘为准。** `notification.md` 与 `wta-module-guide` Notify 事实：业务只依赖 `NotificationApplicationService`；`NotifyClient` 只供通知适配层 |

本课**不宣称**你会拆 `/notify/notification` 四扇（L-051）、Outbox 领取与租约（L-052）、供应商回调验签（L-050）、公告/收件箱/配置窗（L-045…L-049）、浏览器厨房（L-053/L-054）、SMTP/SMS4J 厂商内部、Nacos 热更新幂等窗口（Goal 已推迟 `wta-common-nacos` 叠加）、或 MyBatis/Sa-Token/MySQL 基座（L-083…L-085）。今天只认：**同步渠道这一枪的入参、出参、失败怎么扔、谁允许扣扳机。**

## 先把宏观地图放在桌上

L-002 已经把「通知」拆成三套门口，并写明工具间这扇**不讲 `NotifyDispatcher` 内部**。L-051 把业务柜台钉成 `submit`：同一事务写 Intent/Recipient/Delivery/Outbox，**提交线程不打供应商**。L-052 把领取钉成短事务租约，成功之后才把 Outbox 交给 `DispatchNotificationService.dispatch`。本课走进 **dispatch 之后那一扇同步邮筒**：信封已经写好电话或邮箱，邮筒按渠道找 Adapter，同步把信塞进供应商窗口，再把盖章结果交还工人去改 Delivery。

把大楼想成三层信：

1. **柜台（L-051）。** 业务填 `NotificationCommand`。柜台给回执。信进篮子。
2. **投递员领取（L-052）。** 短事务租约。站内信自己落箱，不经过这只邮筒。
3. **同步邮筒（本课）。** `NotifyClient.send`。只认物理地址。邮件 Adapter 只认 `EMAIL`，短信 Adapter 只认 `PHONE`。

2026-09-17 工作树：权威实现是 `backend/wta-common/wta-common-notify/`。POM 描述「渠道无关通知契约」，显式 common 依赖 **`wta-common-nacos` + `wta-common-core` + `wta-common-redis`**（Skill 模块地图漏写 nacos，口试以 pom 为准）。AutoConfiguration.imports 只登记 `NotifyAutoConfiguration`。生产 `send` 调用点是 `DispatchNotificationService` 第 123 行附近。邮件/短信 Adapter 分别住在 `wta-common-mail` / `wta-common-sms`，由它们自己的 AutoConfiguration 注册进 `NotifyChannelRegistry`。

```text
业务房间 / Captcha / 公告 Publisher
        │
        │  NotificationCommand
        v
NotificationApplicationService.submit     ← L-051 柜台；业务同步入口在这里停
        │  写 Intent / Recipient / Delivery / Outbox READY
        v
NotifyOutboxClaimUseCase                  ← L-052 领取
        │
        v
DispatchNotificationService.dispatch      ← 通知楼工人；事务外
        │
        ├─ IN_APP → InAppNotificationPort.persist / pushRealtime
        │            **不** 喊 NotifyClient
        │
        └─ MAIL / SMS
              NotifyRequest.builder()
                requestId = deliveryId
                channel   = NotifyChannel.of(channel.toLowerCase())  // "MAIL"→mail
                providerKey = 场景绑定账号
                targets = email(...) 或 phone(...)
                content = 渲染后的 Rich / Template
                idempotencyKey = deliveryId
              notifyClient.send(request)          ← 本课格子
                        │
                        v
              ┌─────────────────────────────────────────┐
              │  NotifyClient  (Spring Bean 口)          │
              │    实现 = NotifyDispatcher               │
              │    send(NotifyRequest) → NotifyResult    │
              └─────────────────────────────────────────┘
                        │
         校验 → 附件去重/快照 → 幂等占坑 → Adapter.send
         → 聚合 ACCEPTED/PARTIAL_FAILURE/FAILED
         → 完成幂等 → 发 NotifyDeliveryEvent
         → 非 ACCEPTED 则 NotifyDeliveryException
                        │
                        v
              MailNotifyChannelAdapter  (channel=mail, EMAIL)
              SmsNotifyChannelAdapter   (channel=sms,  PHONE)
                        │
                        v
              工人短事务：Delivery / Attempt / Outbox / 聚合
```

| 符号 | 磁盘 | 拥有什么 | 不拥有什么 |
| --- | --- | --- | --- |
| `NotifyClient` | `core/NotifyClient.java` | 同步 `send` 这一枪 | HTTP、Outbox、用户解析、场景绑定 |
| `NotifyDispatcher` | `core/NotifyDispatcher.java` | 校验 / 幂等 / 快照 / 调度 / 聚合 / 事件 | 自己发 SMTP、自己调 SMS4J |
| `NotifyChannelRegistry` | `registry/` | 按 `NotifyChannel` 找 Adapter；重复 channel 启动失败 | 业务渠道枚举 `NotificationChannel` |
| `NotifyChannelAdapter` | `spi/` | `channel()` + `send(NotifyAdapterRequest)` | 逻辑用户、Outbox 租约 |
| `NotifyRequest` / `NotifyResult` | `model/` | 单渠道物理投递的入参出参 | `NotificationCommand` / `NotificationReceipt` |
| `DispatchNotificationService` | `wta-notify/.../runtime/` | 唯一生产调用方；MAIL/SMS 才 send | 本课不把 `dispatch` 升成第二张 (b) |
| `NotificationApplicationService` | `wta-api` | 业务模块同步入口 | 供应商 I/O |

**图题 / caption：** 宏观同步邮筒。alt：业务停在 submit；领取之后站内信不进邮筒；MAIL/SMS 才 `NotifyClient.send`；Bean 口是接口，跑步的是 Dispatcher。

**文字等价物：** 业务模块把通知单交给 `NotificationApplicationService`。篮子里的邮件/短信任务被领取以后，工人填一张只含电话或邮箱的 `NotifyRequest`，喊 `notifyClient.send`。容器里这只手叫 `NotifyClient`，里面的人叫 `NotifyDispatcher`。Dispatcher 不认用户 ID，不认站内信，不自己写表；它只按渠道名找 Adapter，同步打供应商，全部目标都 ACCEPTED 才把 `NotifyResult` 还回去，否则把同一张结果塞进 `NotifyDeliveryException`。

**类比：** 把 `NotifyClient.send` 想成大楼后门的**同步邮筒**。柜台（submit）不往邮筒塞信。投递员（claim）打开篮子：明信片（站内信）自己塞进楼内信箱；真要出楼的平信/电报，必须写成门牌号或电话号码，投进邮筒。邮筒管理员（Dispatcher）核对信封、看这封是不是刚投过、必要时复印附件，再按「平信 / 电报」找窗口职员（Adapter）。职员盖了章，管理员才把回执交给投递员去改运单。

**类比失效处：**

1. 邮筒**不是**业务房间该摸的门。摸了就是绕过柜台、绕过 Outbox、绕过场景绑定。
2. 「同步」**不是** `NotificationMode.SYNC`。SYNC 仍走 Outbox，只影响回执 `queued`（L-051）。本课「同步」= `send` 线程等到 Adapter 返回。
3. 邮筒**不会**把用户工号翻译成手机号。翻译在通知楼 `resolveUsers` / `NotifySendPlanner`。邮筒看见 `USER` 直接把信退回。
4. 监控事件失败**不会**把已经盖章的信追回来。`publish` 吃掉监听器异常，只打 warn。
5. 幂等重复的监控票写 `SKIPPED_DUPLICATE`，投递员手里拿到的仍是**第一次**那张 `NotifyResult`（成功则返回，失败则仍扔 `NotifyDeliveryException`）。
6. 附件复印机（`NotifyAttachmentSnapshotService`）2026-09-17 **还没装进生产容器**。信封上写了 OSS ID，邮筒会说「附件快照能力尚未装配」。
7. `wta-common-nacos` 依赖只为幂等窗口可被 Nacos 覆盖。本 Goal 把 Nacos 叠加推迟。口试认 pom 有这条边，不把热更新当成本格 covered。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **两张名字，一支枪。** `NotifyClient.send` 和 `NotifyDispatcher.send` 是同一支方法。口试两边都要能指。
2. **Spring 认接口。** 注入 `NotifyClient`。不要找名为 `notifyDispatcher` 的 Bean。
3. **只有 `send`。** 没有异步方法，没有按 ID 查询，没有重试，没有取消。
4. **入参是已经写好的物理信封。** `NotifyRequest`：渠道、目标列表、内容、可选幂等键、可选附件 OSS ID。
5. **渠道名是小写牌子。** `mail` / `sms`，正则 `[a-z][a-z0-9_-]{0,31}`。通知楼库里的 `"MAIL"` 必须 `toLowerCase()` 再 `NotifyChannel.of`。
6. **目标必须是电话 / 邮箱 / openId 这种物理值。** `USER` 是逻辑目标，码 `LOGICAL_TARGET_NOT_SUPPORTED`。
7. **模板必须带完整内容快照。** 空白快照在 Adapter 之前就被拦。纯文本/富文本的快照就是正文本身。
8. **幂等键可空。** 空键不占 Redis。有键但 Redis/Store 不可用 → 失败关闭，不打供应商。
9. **Adapter 抛普通 RuntimeException 不当作调用方崩溃。** Dispatcher 改写成每个目标 `PROVIDER_ERROR`，再按接受数量聚合成 FAILED / PARTIAL_FAILURE，最后仍可能扔 `NotifyDeliveryException`。
10. **只有全部 ACCEPTED 才正常返回。** 部分失败、全失败，都走异常，结果在 `result()`。
11. **生产扣扳机的人只有通知楼工人。** MAIL/SMS；IN_APP 不进邮筒。
12. **业务模块不要抄这支枪。** 规范写在 `notification.md`：只依赖 `NotificationApplicationService`。

**类比补一句：** 邮筒管理员不会帮你查「工号 100 的手机号」，也不会帮你把信存进待寄篮。那些是柜台和投递员的工作。你把没写完的信封塞进去，管理员当场退回，窗口职员连面都不见。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 同步渠道入口 | `NotifyClient.send` | `org.namewta.common.notify.core.NotifyClient` 唯一方法。同步：调用线程等待 Adapter |
| 同步调度器 | `NotifyDispatcher` | `final` 类，`implements NotifyClient`。三个构造器，缺省幂等协调器允许 `store=null` |
| 渠道无关通知契约 | `wta-common-notify` | 工具间 jar。无 HTTP，无 Mapper，无 `src/test`。入口 `NotifyClient` / `NotifyDispatcher` / `NotifyChannelAdapter` / `NotifyRequest` |
| 单渠道请求 | `NotifyRequest` | record：`requestId`（空则 UUID）、`bizType`/`bizId`、`channel`、`providerKey`、`targets`、`content`、`attachmentOssIds`、`auditPolicy`（默认 `FULL`）、`idempotencyKey`/`idempotencyWindow`、`metadata` |
| 同步结果 | `NotifyResult` | `requestId` + `channel` + `providerKey` + `NotifyStatus` + 每目标 `NotifyTargetResult` |
| 逻辑状态 | `NotifyStatus` | `ACCEPTED` / `PARTIAL_FAILURE` / `FAILED` / `SKIPPED_DUPLICATE`。后一个几乎只出现在事件 |
| 目标尝试状态 | `NotifyDeliveryStatus` | 只有 `ACCEPTED` / `FAILED`。没有 DELIVERED |
| 物理目标 | `NotifyTarget` | `(type, value, role)`。工厂：`phone` / `email` / `openId` / `user`。Dispatcher **拒绝** `user` |
| 目标类型常量 | `NotifyTargetType` | `PHONE` / `EMAIL` / `OPEN_ID` / `USER` |
| 渠道牌子 | `NotifyChannel` | 小写规范化 record。内置 `MAIL`、`SMS`。允许插件自定义，但必须有 Adapter |
| 内容 | `NotifyContent` | sealed：`NotifyTextContent` / `NotifyRichContent` / `NotifyTemplateContent`。都要 `contentSnapshot()` |
| 渠道扩展点 | `NotifyChannelAdapter` | `channel()`；默认 `supportedTargetTypes()` 空集=不按类型再滤（仍拒绝 USER）；`send(NotifyAdapterRequest)` |
| 注册表 | `NotifyChannelRegistry` | 构造时 `putIfAbsent`；重复 channel → `IllegalStateException`；找不到 → `UNKNOWN_CHANNEL` |
| 上下文 | `NotifyContext` | `(userId, clientPk, traceId)`。缺省 `empty()`；admin 覆盖为 `RequestNotifyContextResolver` |
| 监控事件 | `NotifyDeliveryEvent` | 请求 + 上下文 + 结果 + 可选 `originalRequestId` / `notifyLogId` / 快照 OSS ID |
| 幂等协调 | `NotifyIdempotencyCoordinator` | 键前缀 `notify:idempotency:v1:` + sha256(channel + `\n` + key)。摘要含渠道、目标、正文、附件、metadata，**不含**原始幂等键明文 |
| 幂等存储 | `NotifyIdempotencyStore` | `Claim`：`Acquired` / `InProgress` / `Completed` / `Conflict`。生产实现 `RedisNotifyIdempotencyStore`，条件是容器里有 `RedissonClient` |
| 幂等窗口 | `notify.idempotency` | 默认 5 分钟；最小 30 秒；最大 24 小时。越界 `IDEMPOTENCY_WINDOW_OUT_OF_RANGE` |
| 未全部接受 | `NotifyDeliveryException` | 消息「通知未被全部目标接受」。携带 `NotifyResult` |
| 进行中 | `NotifyInProgressException` | 同键正在发送。带 `originalRequestId` |
| 摘要冲突 | `NotifyIdempotencyConflictException` | 同键不同 digest |
| 幂等不可用 | `NotifyIdempotencyUnavailableException` | `phase` = `ACQUIRE` 或 `COMPLETE`。有键才触发 |
| 请求不合法 | `NotifyValidationException` | 带 `code()`。发生在 Adapter 前则释放占坑、不打供应商 |
| 附件快照 SPI | `NotifyAttachmentSnapshotService` | 「由应用层存储模块实现」。2026-09-17 **无生产实现** |
| 业务同步入口 | `NotificationApplicationService` | `wta-api`。业务模块停在这里 |
| 通知楼工人 | `DispatchNotificationService.dispatch` | 本课格子的**调用方**，不是第二张矩阵 (b) |
| 站内信端口 | `InAppNotificationPort` | 工人的另一条河。不经过 `send` |

`NotifyAuditPolicy` 有 `FULL` / `REDACT_SENSITIVE`，Dispatcher **不读**它来改发送行为；口试不要把它说成脱敏开关已经接到 `send`。

### 机制/因果链

#### 1. 装配：谁把 Dispatcher 塞进容器

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 只有一行 `NotifyAutoConfiguration`。

缺省 Bean：

| Bean | 条件 | 缺省实现 |
| --- | --- | --- |
| `NotifyContextResolver` | `@ConditionalOnMissingBean` | `NotifyContext::empty` |
| `NotifyEventPublisher` | 同上 | `ApplicationEventPublisher::publishEvent` |
| `NotifyChannelRegistry` | 同上 | `ObjectProvider<NotifyChannelAdapter>.orderedStream()` |
| `NotifyIdempotencyStore` | **还要** `@ConditionalOnBean(RedissonClient.class)` | `RedisNotifyIdempotencyStore` |
| `NotifyIdempotencyCoordinator` | `@ConditionalOnMissingBean` | `store.getIfAvailable()` 可以为 null |
| `NotifyClient` | `@ConditionalOnMissingBean(NotifyClient.class)` | `new NotifyDispatcher(...)`，附件 SPI `getIfAvailable()` |

admin 的 `NotifyContextConfiguration` 用 `@ConditionalOnMissingBean(NotifyContextResolver.class)` 换成 `RequestNotifyContextResolver`：从 `LoginHelper.getLoginUser()` 取 `userId`/`clientPk`，从 MDC `traceId` 或 `trace_id` 取链路。Worker 线程若没有登录人，上下文字段就是 null，**不**阻止发送。

没有 Redisson 时：`NotifyClient` **仍然在**。带 `idempotencyKey` 的 `send` 会在 `begin` 阶段 `NotifyIdempotencyUnavailableException(ACQUIRE)`；不带键的 `send` 可以打供应商。单测 `shouldKeepNotifyClientAvailableWithoutRedissonAndFailClosedOnlyForKeyedRequests` 锁死这句。

邮件/短信 Adapter **不是** notify 模块注册的。`MailConfig` 注册 `MailNotifyChannelAdapter`；`SmsAutoConfiguration` 注册 `SmsNotifyChannelAdapter`。`NotifyAutoConfigurationUnitTest` 把三份 AutoConfiguration 一齐跑，才能 `registry.require(MAIL)` / `require(SMS)`。

#### 2. `send` 主路径（调用线程，无 `@Transactional`）

`NotifyDispatcher.send` 按这个顺序跑。口试要能把「哪一步还没碰到供应商」说清楚：

1. **`validateRequest`。** null 请求 / 空渠道 / 空目标 / 空内容 / 模板空白快照 / 非正整数 OSS ID。失败抛 `NotifyValidationException`，**尚无**幂等占坑。
2. **`normalizeAttachments`。** `LinkedHashSet` 去重并保序。`[10,10,20]` 变成 `[10,20]`。没变化则还是同一条 request。
3. **`registry.require(channel)`。** 没登记 → `UNKNOWN_CHANNEL`。
4. **`validateTargets`。** 目标 type/value 空白 → `INVALID_TARGET`；type 忽略大小写等于 `USER` → `LOGICAL_TARGET_NOT_SUPPORTED`；Adapter 声明了支持类型且当前 type 不在其中 → `UNSUPPORTED_TARGET_TYPE`。支持集为空则不再按类型过滤（测试用的 `test` 渠道就是这样）。
5. **`resolveContext`。** resolver 空或返回 null → `NotifyContext.empty()`。
6. **`beginIdempotency`。** 键空白 → `claim=null`，后面 complete/release 都是 no-op。否则 `coordinator.begin`。
7. **认领结果分流。**
   - `InProgress` → `NotifyInProgressException`
   - `Conflict` → `NotifyIdempotencyConflictException`
   - `Completed` → 发 `SKIPPED_DUPLICATE` 事件（空 deliveries，带 `originalRequestId`），然后 `requireAccepted(原结果)`
   - `Acquired` 或 `null` → 继续
8. **`createSnapshots`。** 没有附件 → 空批次。有附件但 SPI 没装配 → `ATTACHMENT_SNAPSHOT_NOT_CONFIGURED`，并 `release` 占坑。快照条数/OSS ID/文件名/size/materializer 不合法 → 先 cleanup 再抛。
9. **`adapter.send(new NotifyAdapterRequest(request, context, snapshots))`。**
   - `NotifyValidationException` / `NotifyAttachmentSnapshotException`：cleanup + release + 原样抛。物化失败走这条，供应商结果不算数。
   - 其他 `RuntimeException`（含 `validateAdapterResult` 的 `IllegalStateException`）：**改写成**全目标 `PROVIDER_ERROR`，打 warn，**不** release（后面要 complete）。
10. **`validateAdapterResult`。** providerKey 空白、条数对不上、目标对象不等于请求里同一条、status 空 → `IllegalStateException`，随上一步变成 PROVIDER_ERROR。
11. **`aggregate`。** 全部 ACCEPTED → `NotifyStatus.ACCEPTED`；一个都没有 → `FAILED`；其余 → `PARTIAL_FAILURE`。
12. **`complete`。** 只对 `Acquired` 写完成态。写失败：先 `publish` 真实供应商结果，再抛 `COMPLETE` 阶段不可用。调用方看不到成功返回值。
13. **`publish`。** 监听器爆炸只 warn，不改 `NotifyResult`。
14. **`requireAccepted`。** 不是 ACCEPTED 就 `NotifyDeliveryException`。

部分失败单测 `shouldAttemptEveryTargetBeforeThrowingPartialFailure`：三个电话，Adapter 第二个失败，调用方接到异常，但 `attemptedTargets.size()==3`。邮筒不会在第一个失败后收摊——**遍历是 Adapter 的事**；Dispatcher 要求结果列表与目标列表 1:1。

#### 3. 幂等四态（有键才存在）

Redis 桶里一份 JSON：`IN_PROGRESS` / `COMPLETED` + digest + requestId + 可选 result。键是哈希，单测断言键里**没有**业务密钥明文、**没有**手机号。

| 第二次 `send` 看见 | 供应商再打吗 | 调用方得到 |
| --- | --- | --- |
| 进行中、digest 相同 | 否 | `NotifyInProgressException` |
| 已完成、digest 相同、原结果 ACCEPTED | 否 | 原 `NotifyResult`；事件 `SKIPPED_DUPLICATE` |
| 已完成、digest 相同、原结果 FAILED | 否 | 再扔一次 `NotifyDeliveryException(原结果)`；事件仍 `SKIPPED_DUPLICATE` |
| digest 不同 | 否 | `NotifyIdempotencyConflictException` |
| Store 抛错 / store=null | 否（有键时） | `NotifyIdempotencyUnavailableException` |
| 无键 | 是 | 正常聚合 |

默认窗口 5 分钟。请求自带窗口也必须落在 30 秒…24 小时。校验在 coordinator，不在 Dispatcher 正文。

`releaseQuietly`：校验失败要释放占坑；释放失败只吞掉，占坑留到 TTL，避免立即重放。complete 失败**不**当成功返回。

#### 4. 业务模块的同步入口 ≠ 这支枪

规范原文：业务只能依赖 `NotificationApplicationService`，构造 `NotificationCommand`，带稳定 `idempotencyKey`。Controller / Workflow / 其它业务**不得**直接依赖渠道客户端。

磁盘对照：

| 调用方 | 喊的方法 | 算不算本课格子 |
| --- | --- | --- |
| `CaptchaController` 短信/邮件验证码 | `notificationService.submit` | **否。** 业务同步入口在 L-051。测试方法名带 NotifyClient 是历史误名 |
| 公告 Publisher、配置试发 | 同样 `submit`（试发可 `mode=SYNC`） | **否。** SYNC 仍是柜台 |
| `DispatchNotificationService` MAIL/SMS | `notifyClient.send` | **是。** 通知适配层 |
| 同工人 IN_APP | `InAppNotificationPort` | **否。** 不进 Dispatcher |
| 供应商回执 | `ProviderCallbackUseCase.apply` | **否。** L-050；不打 `send` |

工人填信封时的性状（口试用来证明「谁在喊 send」，不要把 planner 升成 claimed cell）：

- `requestId` / `idempotencyKey` 都是 `String.valueOf(deliveryId)`
- `channel` = `NotifyChannel.of(delivery.getChannel().toLowerCase())`，所以库里的 `MAIL` 对得上牌子 `mail`
- 邮件内容是场景绑定渲染后的 `NotifyRichContent`，**不是**调用方 `titleSnapshot`/`contentSnapshot`
- 短信内容是 `new NotifyTemplateContent("sms", smsTemplateCode, smsParams, "")`——第四个参数是空串
- 缺绑定、账号停用、配额用尽：工人 **never** `send`，Delivery 直接 FAILED

**边界（必须口述）：** Dispatcher 规定模板空白快照非法。工人给短信塞的快照就是 `""`。`DispatchNotificationServiceTest` **mock** 了 `NotifyClient`，所以那份测试不会爆 `CONTENT_SNAPSHOT_REQUIRED`。口试按 Dispatcher 契约说「真 `send` 会在供应商前拒绝空白模板快照」；不要把 mock 成功说成「短信模板可以没有快照」。邮件走 `NotifyRichContent`，快照等于渲染正文，这条河对得上。

#### 5. 两只生产 Adapter（只标位置，不拆厂商）

`MailNotifyChannelAdapter`：`channel()=MAIL`，支持类型只有 `EMAIL`。按 role 分成 TO/CC/BCC；没有 TO → `MAIL_TO_REQUIRED`。有 `MailAccountResolver` 时必须带 `providerKey`。SMTP 一次发送，所有目标同一次 ACCEPTED 或同一次 `PROVIDER_ERROR`。附件先物化到临时目录，`finally` 删除；物化失败是快照异常，Dispatcher 会 cleanup。

`SmsNotifyChannelAdapter`：`channel()=SMS`，支持类型只有 `PHONE`。**每个目标单独** `provider.sender().send`。一个失败不影响下一个被尝试。这就是「部分失败」在短信河上的来源。

自定义渠道（单测用 `test`、拒绝用例用 `feishu`）必须自己实现 Adapter 并成为 Spring Bean，否则 `UNKNOWN_CHANNEL`。不要把「record 允许自定义」说成「feishu 已经能发」。

#### 6. 邻居：事件、附件、Nacos（只标位置）

`NotifyDeliveryEvent` 是「Provider 同步调用后的不可变监控事件」。L-049 看板读的是通知楼表，不是这个事件本身。口试：事件发布失败不影响 `send` 的 ACCEPTED。

附件 SPI 在 `attachment/`。Dispatcher 会去重、校验、把快照交给 Adapter、在校验/物化失败时 cleanup；供应商失败则**保留**快照对象（单测 `providerFailureAndEventFailureMustRetainSnapshotObjects`）。生产容器 2026-09-17 没挂实现，Outbox 工人也没填 `attachmentOssIds`。不要把 SPI 说成已经接通 OSS。

`NotifyIdempotencyProperties` 实现 `NacosConfigParticipant`，id `notify-idempotency`。这是 pom 依赖 `wta-common-nacos` 的原因。Goal 把 Nacos 叠加标 deferred。本课只认：窗口数字来自这份 properties 的 snapshot，缺 Nacos accessor 就用本地 5 分钟/30 秒/24 小时。

## 图、表或文本图

**`send` 决策表（口试用）：**

| 输入 | 供应商 | 调用方 | 事件 |
| --- | --- | --- | --- |
| 合法、全 ACCEPTED、无键 | 打一次 | 返回 `NotifyResult(ACCEPTED)` | 同结果 |
| 合法、部分 FAILED | Adapter 仍跑完全部目标 | `NotifyDeliveryException(PARTIAL_FAILURE)` | 同结果 |
| Adapter 抛 RuntimeException | 视 Adapter；Dispatcher 记全 FAILED | `NotifyDeliveryException(FAILED)` | 同结果 |
| `USER` 目标 / 空白模板快照 / 未知渠道 | **0 次** | `NotifyValidationException(code)` | 无 |
| 有键、进行中 | 0 次 | `NotifyInProgressException` | 无 |
| 有键、完成且 digest 同 | 0 次 | 原结果（失败则仍是 DeliveryException） | `SKIPPED_DUPLICATE` 空 deliveries |
| 有键、digest 不同 | 0 次 | `NotifyIdempotencyConflictException` | 无 |
| 有键、无 Store | 0 次 | `NotifyIdempotencyUnavailableException(ACQUIRE)` | 无 |
| 无键、无 Store | 打 | 正常 | 正常 |
| 监听器抛错 | 已打完 | 不改变供应商结果 | warn |
| complete 写 Redis 失败 | 已打完 | `Unavailable(COMPLETE)`；事件里已是 ACCEPTED | 先 publish 再抛 |
| 带附件、无 SPI | 0 次 | `ATTACHMENT_SNAPSHOT_NOT_CONFIGURED` | 无 |

**三套通知名词对照：**

| 口头「通知」 | 包 | 方法 | 本课 |
| --- | --- | --- | --- |
| 业务交单 | `org.namewta.notify.api` | `submit/query/retry/cancel` | 对照；格子不是它 |
| 控制面 HTTP | `wta-notify` Controller | `/notify/*` | 对照 |
| 同步渠道 | `org.namewta.common.notify.core` | `send` | **格子** |
| 浏览器厨房 | `@namewta/domain-notify` | 22 枪，无 submit | L-053；厨房不打这支枪 |

**异常码（Dispatcher 正文，Adapter 前）：**

`REQUEST_REQUIRED` / `CHANNEL_REQUIRED` / `TARGET_REQUIRED` / `CONTENT_REQUIRED` / `CONTENT_SNAPSHOT_REQUIRED` / `INVALID_ATTACHMENT_OSS_ID` / `ATTACHMENT_SNAPSHOT_NOT_CONFIGURED` / `INVALID_TARGET` / `LOGICAL_TARGET_NOT_SUPPORTED` / `UNSUPPORTED_TARGET_TYPE` / `UNKNOWN_CHANNEL`。

Adapter 另有 `UNKNOWN_PROVIDER`、`MAIL_TO_REQUIRED`、`INVALID_TARGET_ROLE`、`SNAPSHOT_MATERIALIZE_FAILED`。Dispatcher 合成的供应商失败码是 `PROVIDER_ERROR`。

## 正例、反例与边界

**正例 1 — 指入口。** 打开 `NotifyClient.java`，圈唯一方法 `send`。打开 `NotifyDispatcher` 类声明 `implements NotifyClient`。打开 `NotifyAutoConfiguration.notifyClient`，圈返回类型 `NotifyClient`、`new NotifyDispatcher`。OBJ-82 这一句就成立。

**正例 2 — 指业务同步入口停在哪。** 打开 `CaptchaController` 构造器，字段类型是 `NotificationApplicationService`。打开 `CaptchaNotifyCallerUnitTest.emailCaptchaUsesNotifyClientAndCachesOnlyAfterAccepted`，断言是 `verify(notificationService).submit`。业务房间不碰 `send`。

**正例 3 — 指出盒才 send。** 打开 `DispatchNotificationService.dispatch`。`IN_APP` 分支没有 `notifyClient`。`else` 里 `planChannel` 失败则 never send；成功才 `notifyClient.send`。打开 `DispatchNotificationServiceTest.unboundMailFailsClosedWithoutProviderSend`，`verify(..., never()).send`。

**正例 4 — 物理目标。** 工人 MAIL → `NotifyTarget.email(targetValue)`；SMS → `phone(targetValue)`。Dispatcher 单测 `shouldRejectLogicalUserAndTemplateWithoutSnapshotBeforeProvider`：`NotifyTarget.user("100")` 码 `LOGICAL_TARGET_NOT_SUPPORTED`，`attemptedTargets` 仍空。

**正例 5 — 渠道小写。** `NotifyChannel.MAIL` 的 value 是 `"mail"`。工人 `NotifyChannel.of("MAIL".toLowerCase())` 才能命中注册表。直接 `of("MAIL")` 会先被 record 规范成 `"mail"`（构造器 `toLowerCase`），牌子仍对得上。非法字符或空串在 record 构造期就 `IllegalArgumentException`，进不了 Dispatcher 正文。

**正例 6 — 部分失败仍跑完。** 三个 PHONE，第二个 REJECTED。调用方 `NotifyDeliveryException`，`status=PARTIAL_FAILURE`，三次 attempt。`requireAccepted` 不会把 PARTIAL 当成成功。

**正例 7 — 事件失败不影响盖章。** `shouldNotChangeProviderResultWhenEventPublishingFails`：监听器扔 `IllegalStateException`，`send` 仍返回 ACCEPTED。

**正例 8 — 重复成功。** 同一幂等键、同一 digest 第二次 `send`：供应商计数仍 1；返回值 `equals` 第一次；第二份事件 `SKIPPED_DUPLICATE`、空 deliveries、带 `originalRequestId`。

**正例 9 — 重复失败。** 第一次 Adapter 全 FAILED → 第一次就 `NotifyDeliveryException`。第二次不再打供应商，异常里的 `result()` 与第一次相同。

**正例 10 — 无 Redisson 仍有 Client。** 上下文 runner 只有 `NotifyAutoConfiguration`：`getBean(NotifyClient.class)` 非空；带键 `coordinator.begin` 抛不可用。

**正例 11 — 邮件正文权威在绑定。** `boundMailUsesRenderedTemplateNotCallerSnapshots`：调用方快照是秘密句子，真正 `NotifyRequest.content()` 是「验证码 1234」/「有效 5 分钟，码 1234」，`providerKey=smtp-main`。

**正例 12 — 自定义未登记渠道。** `NotifyChannel.of("feishu")` 合法，空注册表 `require` → `UNKNOWN_CHANNEL`。openId 目标也救不了。

**反例 1 — 「业务要发短信就注入 `NotifyClient`。」** 规范禁止。磁盘上业务走 `submit`。

**反例 2 — 「`NotificationMode.SYNC` 就是 `NotifyDispatcher.send`。」** SYNC 仍写 Outbox。同步邮筒在 claim 之后。

**反例 3 — 「`NotifyDispatcher` 是 HTTP Controller。」** 工具间零扇 `@RequestMapping`。

**反例 4 — 「站内信也走 `send`。」** `IN_APP` 走 `InAppNotificationPort`。common-notify 没有 IN_APP 渠道常量。

**反例 5 — 「`NotifyTarget.user` 工厂存在所以能发。」** 工厂在，Dispatcher 拒。

**反例 6 — 「`send` 返回 `SKIPPED_DUPLICATE`。」** 返回路径 `requireAccepted` 只放行 ACCEPTED。重复态在事件。

**反例 7 — 「Adapter 抛异常等于调用方收到那个异常。」** 普通 RuntimeException 被折成 `PROVIDER_ERROR` 结果再 DeliveryException。只有校验/快照异常原样冒泡。

**反例 8 — 「厨房 `createNotificationService` 会打 `send`。」** 浏览器 22 枪没有这支。统一通知四扇厨房标签有、方法没有（L-053）。

**反例 9 — 「回调会再 `send` 一次。」** 回调升级已有 Delivery（L-050）。

**反例 10 — 「common-notify 自己测自己。」** 测试在 `wta-admin`。模块地图写 `tests through consumers/admin`。

**反例 11 — 「Skill 模块地图 pom 只有 core+redis。」** 磁盘还有 `wta-common-nacos`。以 pom 为准。

**反例 12 — 「附件快照已经接到 OSS。」** 只有 SPI + Dispatcher 调用点 + 单测 Recording 实现。

**反例 13 — 「`emailCaptchaUsesNotifyClient...` 证明验证码走 Dispatcher。」** 读断言，不要读方法名。

**反例 14 — 「把 `DispatchNotificationService.dispatch` 标成第二张 (b)。」** 矩阵 (b) 符号是 `NotifyClient.send / NotifyDispatcher.send`。dispatch 是调用方，领取性状属于 L-052。

**反例 15 — 「`wta-system` POM 写了 `wta-common-notify` 所以 system Service 可以 `send`。」** 依赖边不是调用授权。跨房间合同仍是 `wta-api`。

**边界 1 — `requestId` 空会 UUID。** 幂等键空才跳过占坑。两套 ID 不要混。工人把两者都写成 deliveryId。

**边界 2 — 支持类型空集 ≠ 支持 USER。** USER 是硬拒绝。空集只表示不再做 PHONE/EMAIL 白名单。

**边界 3 — `validateAdapterResult` 的 IllegalStateException 会被下一道 `catch (RuntimeException)` 吃掉。** 畸形 Adapter 结果在调用方看来也是 PROVIDER_ERROR，不是那句「未返回 Provider 标识」原文。

**边界 4 — 短信工人的空 `contentSnapshot`。** 真 Dispatcher 会 `CONTENT_SNAPSHOT_REQUIRED`。当前耦合靠 mock 遮住。口试分开说两层。

**边界 5 — 幂等 complete 失败：供应商已经成功。** 事件是 ACCEPTED，调用方却是 `COMPLETE` 不可用。工人若没接到 `NotifyResult`，会走自己的 `DISPATCH_ERROR` / UNKNOWN 分支——那是通知楼，不是 Dispatcher 再打一次。

**边界 6 — 租约续租失败则工人不写 Delivery。** 发生在 `send` 返回之后。本课不把租约当 Dispatcher 性状。

**边界 7 — `NotifyStatus` 与 Delivery 表状态不是一张表。** Dispatcher 的 ACCEPTED 被工人 `delivery.setStatus(result.status().name())` 写成 `"ACCEPTED"`。站内信成功写的是 `"DELIVERED"`。回调才可能再升级。不要说 `send` 已经 DELIVERED。

**边界 8 — 自定义渠道要小写合规。** `NotifyChannel` 构造器会 trim+lowerCase。中文、空格、大写开头的非法串进不了 Dispatcher。

**边界 9 — 默认审计策略 FULL。** 没有接到发送路径。不要教「REDACT_SENSITIVE 会在 send 里抹手机号」。

**边界 10 — 无 HTTP 质量门禁专属于本课方法。** 通知楼仍走 `./mvnw -pl wta-modules/wta-notify -am test`。common-notify 自身 `compile` 即可；契约测试挂在 admin。

## 变式与迁移

1. **和 L-002 对照。** 总览只让你在工具间门口停步。本课把门口拆成「Bean 口 `NotifyClient` + 实现 `NotifyDispatcher.send`」。三套通知名词不要重新揉回去。
2. **和 L-051 对照。** 柜台 `submit` 的同步入口是给**业务模块**的。本课同步入口是给**渠道 Adapter** 的。`mode=SYNC` 不是短路到 `send`。submit 注入 `DispatchNotificationService` 只为 `refreshAggregate`，不是本课格子。
3. **和 L-052 对照。** 领取成功才允许工人碰 Port。供应商 I/O、限额、backoff 不是领取 (b)。本课接过「碰 Port」之后那一枪。
4. **和 L-050 对照。** 门铃升级已有 Delivery 的 `providerMessageId`。ID 从哪来：本课 `NotifyTargetResult.providerMessageId`，工人写回 Delivery。回调不 `send`。
5. **和 L-047 对照。** 试发仍 `submit`。场景绑定、账号、热配 `${name}` 在工人 `NotifySendPlanner`，Dispatcher 只看见已经渲染的 `NotifyContent` 和 `providerKey`。
6. **和 L-053 对照。** 厨房没有 `send`，也没有 `/notify/notification`。不要把前端工厂说成渠道入口。
7. **和 Captcha 对照。** 验证码业务入口是 `submit` + `recipientType=PHONE/EMAIL`。真正 SMTP/短信发生在本课 `send`，中间隔着 Outbox。
8. **和 ThirdPartyGateway 对照。** L-064 是另一条出站 HTTP 河（第三方网关）。通知渠道不是 `ThirdPartyGateway.execute`。
9. **迁移：业务房间想「同步立刻发」。** 仍然 `NotificationCommand` + 稳定幂等键；需要当面回执用 `mode=SYNC` 等 Outbox 转一圈。不要在 profile/system 里 `@Autowired NotifyClient`。
10. **迁移：新渠道（飞书/企微）。** 新增 `NotifyChannelAdapter` Bean，`channel()` 返回合规小写牌子，`supportedTargetTypes` 写物理类型。登记表重复会让应用启动失败。不要改 `NotifyClient` 接口加方法。
11. **迁移：要带 OSS 附件。** 先在应用层实现 `NotifyAttachmentSnapshotService` + `NotifyLogIdGenerator` 并成为 Bean，再让请求填正整数 OSS ID。只填 ID 不装 SPI，`send` 会校验失败。
12. **迁移：想让短信模板快照对得上 Dispatcher。** 改的是工人 `toContent`，给 `NotifyTemplateContent` 第四参一个非空快照（或改 Dispatcher 契约并改单测 `shouldReject...TemplateWithoutSnapshot`）。不要只改 mock。
13. **迁移口诀：** 先数三套门口 → 再数 Bean 类型是 `NotifyClient` → 再数 `send` 同步、只认物理目标、只返回 ACCEPTED → 再数唯一生产调用方是通知楼 MAIL/SMS → 再数幂等四态与「有键才失败关闭」。跳步会出现「业务直接 send」「SYNC 等于 Dispatcher」「USER 能发」「站内信走邮筒」。

## 常见误区

1. **「OBJ-82 要业务模块调用 `NotifyDispatcher`。」** 业务模块的同步入口是 `NotificationApplicationService`。Dispatcher 是渠道同步入口。
2. **「两张 (b) 是两个方法。」** 同一支 `send`。接口名 + 类名都要会指。
3. **「注入实现类。」** 注入 `NotifyClient`。
4. **「有 `NotifyTarget.user` 就能发站内信。」** 逻辑目标被拒；站内信不走这支枪。
5. **「`IN_APP` 是 `NotifyChannel`。」** 那是 `wta-api` 的 `NotificationChannel`。
6. **「`send` 是异步。」** 调用线程阻塞到 Adapter 返回。
7. **「失败返回 `NotifyResult(FAILED)`。」** 失败扔 `NotifyDeliveryException`。
8. **「重复发送返回 SKIPPED。」** 事件 SKIPPED，方法仍 `requireAccepted(原结果)`。
9. **「验证码测试名等于走 Client。」** 读 `submit` 断言。
10. **「部分失败会短路。」** Dispatcher 要求 1:1 结果；短信 Adapter 逐目标尝试。
11. **「事件失败会回滚供应商。」** 不会。
12. **「没 Redis 就没有 `NotifyClient`。」** Client 在；有键才失败关闭。
13. **「附件已经能发。」** SPI 未落地。
14. **「厨房或回调会 `send`。」** 不会。
15. **「把 `dispatch` 标 covered 就算本格。」** 符号是 `NotifyClient.send` / `NotifyDispatcher.send`。
16. **「模板无快照也能发，因为工人就是这么写的。」** Dispatcher 单测锁死拒绝；工人那条河被 mock 遮住。
17. **「`wta-common-notify` 依赖 nacos 所以本课覆盖 Nacos。」** 推迟。只解释窗口 properties 为何在这个 jar。
18. **「`NotifyAuditPolicy.REDACT_SENSITIVE` 会在 send 里脱敏。」** 2026-09-17 `send` 不读它。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `NotifyClient.java` 与 `NotifyDispatcher.java`。圈函数式接口、`implements`、`send` 方法、`requireAccepted`、`LOGICAL_TARGET_NOT_SUPPORTED`、`CONTENT_SNAPSHOT_REQUIRED`、RuntimeException 折成 `providerFailure`。
2. 打开 `NotifyAutoConfiguration.java` 与 `AutoConfiguration.imports`。圈 Bean 类型是 `NotifyClient`、Store 需要 `RedissonClient`、附件 SPI `getIfAvailable`。打开 `pom.xml` 圈 nacos/core/redis。确认没有 `src/test`。
3. 打开 `DispatchNotificationService.java`。圈 IN_APP 不 send、MAIL/SMS 的 `NotifyRequest.builder`、`toLowerCase`、`NotifyTemplateContent(..., "")`、`catch (NotifyDeliveryException)`。打开 `DispatchNotificationServiceTest` 圈 `never().send` 与邮件渲染断言。
4. 打开 `NotifyDispatcherUnitTest` / `NotifyIdempotencyDispatcherUnitTest` / `NotifyAttachmentDispatcherUnitTest` / `NotifyIdempotencyAutoConfigurationUnitTest`。圈：USER 拒绝、部分失败、重复 SKIPPED 事件、无 Redisson、附件去重、供应商失败保留快照。
5. 打开 `MailNotifyChannelAdapter` 与 `SmsNotifyChannelAdapter`。圈 `supportedTargetTypes`、邮件一次发送 vs 短信逐目标。打开 `CaptchaNotifyCallerUnitTest` 圈方法名与真正的 `submit`。打开 `notification.md`「调用入口」段，圈业务只依赖 `NotificationApplicationService` 那一句。

## 总结、词汇表与下一步

- **宏观同步邮筒：** `NotifyClient.send` 是容器口，`NotifyDispatcher.send` 是同一支同步枪。它把已经写好的物理信封交给渠道 Adapter，全部 ACCEPTED 才还 `NotifyResult`。
- **(b) 方法性状：** 校验在供应商前；`USER` 与空白模板快照直接拒绝；有幂等键才占 Redis，无 Store 则有键失败关闭、无键仍可发；Adapter 普通异常折成 PROVIDER_ERROR；事件失败不改结果；非 ACCEPTED 走 `NotifyDeliveryException`。
- **与业务模块的同步入口：** 业务停在 `NotificationApplicationService.submit`。唯一生产 `send` 调用方是通知楼 `DispatchNotificationService` 的 MAIL/SMS 分支。站内信、回调、厨房都不碰这支枪。
- **不要把邻居并进格子。** 领取是 L-052，柜台是 L-051，门铃是 L-050，Nacos/MyBatis/会话/MySQL 是 L-083…L-085。

词汇表：`NotifyClient` / `NotifyDispatcher` / `send` / `NotifyRequest` / `NotifyResult` / `NotifyChannel` / `NotifyTarget` / `NotifyContent` / `NotifyChannelAdapter` / `NotifyChannelRegistry` / `NotifyDeliveryException` / `NotifyValidationException` / `NotifyInProgressException` / `NotifyIdempotencyConflictException` / `NotifyIdempotencyUnavailableException` / `NotifyStatus` / `NotifyDeliveryStatus` / `NotifyDeliveryEvent` / `NotifyIdempotencyCoordinator` / `RedisNotifyIdempotencyStore` / `LOGICAL_TARGET_NOT_SUPPORTED` / `CONTENT_SNAPSHOT_REQUIRED` / `UNKNOWN_CHANNEL` / `ATTACHMENT_SNAPSHOT_NOT_CONFIGURED` / `SKIPPED_DUPLICATE` / `NotificationApplicationService` / `DispatchNotificationService` / `InAppNotificationPort` / `MailNotifyChannelAdapter` / `SmsNotifyChannelAdapter`。

下一步：L-083 才是 `BaseEntity` / `BaseMapperPlus` / QueryBuilder 的所有权。L-084 才是 Sa-Token 会话写入 Redis 的失败路径。L-085 才是 MySQL 基座与 `@DS`。本课结束不发作业、不打分；要练习请之后主动激活 Homework。覆盖矩阵这一格仍等阶段 M 的探针，本文件不是掌握证明。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | `wta-common-notify` 公开入口是 Dispatcher / Client / Adapter SPI；测试在消费者/admin | 后端模块表 notify 行 | 2026-09-17 |
| S-015 | `backend/wta-api/.../notify/api` | 业务跨模块合同是 `NotificationApplicationService`，不是 `NotifyClient` | `wta-api` notify 包 | 2026-09-17 |
| S-L002-01 | 子课 L-002 | 三套通知门口；工具间不讲 Dispatcher 内部 | `children/.../L-002-backend-assembly.md` | 2026-09-17 |
| S-L082-01 | `NotifyClient.java`；`NotifyDispatcher.java` | 函数式 `send`；校验/幂等/快照/Adapter/聚合/`requireAccepted` | `org.namewta.common.notify.core` | 2026-09-17 |
| S-L082-02 | `NotifyAutoConfiguration.java`；`AutoConfiguration.imports`；`pom.xml` | Bean 口是 `NotifyClient`；Store 条件 Redisson；依赖 nacos+core+redis；无 src/test | `wta-common-notify` | 2026-09-17 |
| S-L082-03 | `model/*`；`registry/NotifyChannelRegistry.java`；`spi/NotifyChannelAdapter.java` | 渠道小写、物理目标、内容 sealed、重复 Adapter 启动失败、未知渠道码 | 同模块 model/registry/spi | 2026-09-17 |
| S-L082-04 | `wta-admin/.../test/notify/core|idempotency|attachment/*` | USER 拒绝、部分失败、重复 SKIPPED、无 Redis、附件去重与保留/cleanup | `@Tag("dev")` 契约测试 | 2026-09-17 |
| S-L082-05 | `DispatchNotificationService.java` 与 `DispatchNotificationServiceTest.java` | 唯一生产 `send`；IN_APP 不走；缺绑定 never send；邮件渲染；短信空快照 | `wta-notify/.../runtime` | 2026-09-17 |
| S-L082-06 | `MailNotifyChannelAdapter.java`；`SmsNotifyChannelAdapter.java`；`MailConfig`；`SmsAutoConfiguration` | 生产 Adapter 与支持类型；谁注册进 Registry | `wta-common-mail` / `wta-common-sms` | 2026-09-17 |
| S-L082-07 | `engineering-standards/references/notification.md`；`wta-module-guide` Notify 事实；`wta-common-modules-guide/SKILL.md` | 业务只 submit；Client 只给适配层。摘要冲突以磁盘+硬合同为准 | 统一通知规范；Skill 入口 | 2026-09-17 |
| S-L082-08 | `CaptchaNotifyCallerUnitTest.java`；`RequestNotifyContextResolver.java`；`NotifyContextConfiguration.java` | 验证码走 submit；admin 覆盖上下文；Worker 无登录人仍可 send | `wta-admin` web/config 与 test/notify/caller | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：`wta-common-notify` 是工具间里的同步邮筒，不是业务柜台，也不是 HTTP 窗。公开口是 `NotifyClient.send`，实现类是 `NotifyDispatcher`，Spring 只把接口暴露成 Bean。`send` 在调用线程里检查信封：必须有渠道、物理目标、内容；用户 ID 这种逻辑地址当场退回；模板如果没有内容快照也退回。有幂等键才去 Redis 占坑，键和手机号都不会以明文进 Redis 键名；没键就直接发。找到渠道 Adapter 后同步打供应商，按每个目标的 ACCEPTED 数量聚合成成功、部分失败或全失败，只有全部成功才把 `NotifyResult` 正常返回，否则扔带着同一张结果的 `NotifyDeliveryException`。监控事件失败不追回已经发出去的信。业务模块不准摸这只邮筒，它们把单交给 `NotificationApplicationService.submit`。通知楼工人领取 Outbox 以后，站内信走自己的端口；只有邮件和短信才调用 `notifyClient.send`。2026-09-17 还没有生产附件快照实现，也没有 IN_APP 渠道牌子。
