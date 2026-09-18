---
lesson_id: L-008
objective_ids: [OBJ-08]
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
source_ids: [S-N-08, S-N-09, S-N-10]
---

# Lesson 008：Outbox 领取：短事务租约，再投递

## 学完你能做什么

能口述这条**没有 HTTP Controller** 的链：`NotifyOutboxClaimUseCase` implements `NotifyOutboxClaimPort`，注入 `NotifyOutboxClaimService`；`claim(owner)` 带 `@DSTransactional`。能说明 Worker `NotifyOutboxWorker` 如何 `claimService.claim(owner)` 再 `dispatchService.dispatch`；唤醒如何从 `NotifyOutboxWakePublisher`（AFTER_COMMIT）经 Redis/`NotifyOutboxWakeSubscriber` 或本地 `NotifyOutboxWakeSignal` 进入 `onWake`。能强调：载荷里的 `outboxId` 只是 hint，**禁止按 ID 直投**。

## 先把宏观地图放在桌上

把 Outbox 当成「待寄篮」。submit 往篮子里放信封（L-007）。本课是：哪个投递员伸手进去拿、如何在篮子上贴「我正在处理、60 秒内别人别抢」，以及谁在篮子一响时叫醒投递员。

本切片**没有** `/notify/outbox` 之类 HTTP。领取只给 Worker 用。

```text
写路径（上一课已经发生）
  Runtime.requestOutboxWake(hint)
    --> ApplicationEventPublisher
          NotifyOutboxWakeRequestedEvent(outboxId?)
                |
                | 事务提交之后
                v
        NotifyOutboxWakePublisher.publishAfterCommit
          RedisUtils.publish("notify:outbox:wake", WakeSignal)
          以及本地 publishEvent(WakeSignal)   // 辅信号，不能替代 Redis

读/领路径（本课）
  NotifyOutboxWakeSubscriber  --subscribe--> 同一 REDIS_CHANNEL
        --> worker.onWake(signal)           // signal 可为 null，drain 不读 id
  NotifyOutboxWorker.onLocalWake            // @EventListener 同 JVM
  NotifyOutboxWorker.poll                   // @Scheduled 默认 60000ms
        |
        v
     drain(trigger):
        loop:
          batch = claimService.claim(owner)     // Port -> UseCase -> ClaimService
          for each: dispatchService.dispatch(outbox)
        until batch empty
```

**类比失效处：** 篮子比喻不包含短信 HTTP。`dispatch` 在领取事务**之外**；本课只把 dispatch 当作 claim 成功后的下一步方法名，不编造供应商内部。

## 核心概念与机制

### 直觉讲解

为什么领取要单独 UseCase？注释写得很白：租约必须发生在**独立短事务**里。如果把「占坑」和「访问短信网关」放在同一长事务，数据库连接会被供应商超时拖死，别的 Worker 也抢不到过期任务。

`NotifyOutboxWorker` 构造时 `owner = UUID.randomUUID()`，每个进程一个身份。它注入的是端口 `NotifyOutboxClaimPort` 和 `NotifyDispatchPort`，不直接 new ClaimService。

叫醒有三条入口，**共用** `drain`：

- `poll()`：`@Scheduled(fixedDelayString = "${notify.outbox.poll-delay-ms:60000}")`，`Trigger.POLL`。
- `onWake(NotifyOutboxWakeSignal)`：Redis 跨进程，`Trigger.WAKE`。方法参数存在，但 `drain` 不按 `signal.outboxId` 过滤。
- `onLocalWake`：`@EventListener`，转给 `onWake`。注释：不能替代 Redis 主路径。

订阅失败不得阻止启动：`NotifyOutboxWakeSubscriber.afterPropertiesSet` catch 后 warn，「慢速兜底仍在」。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，仅源码已写明者） |
| --- | --- | --- |
| 领取端口 | claim port | `NotifyOutboxClaimPort.claim(String owner)` |
| 领取用例 | claim use case | `NotifyOutboxClaimUseCase`：短事务 + 委托 `NotifyOutboxClaimService` |
| 租约 | lease | `leaseOwner` + `leaseToken` + `leaseUntil`；token 每次领取 `UUID` |
| 可领取行 | claimable | XML：`status in ('READY','PROCESSING')` 且 `available_at<=now` 且 next_attempt 到期且租约空或已过期；`for update skip locked` |
| 唤醒通道 | wake channel | `NotifyOutboxWakeChannels.REDIS_CHANNEL = "notify:outbox:wake"` |
| 唤醒信号 | wake signal | `NotifyOutboxWakeSignal(type=WAKE, outboxId?)`；无 PII |
| 触发来源 | trigger | Worker 内部枚举 `WAKE` / `POLL`，只用于日志 |

规范「Worker 使用租约 owner/token 更新，续租失败时禁止继续写入投递结果」发生在 `DispatchNotificationService.renewLease`（dispatch 路径）。领取课只保证：claim 时写入 owner/token/until；dispatch 开头 `leaseActive` 对不上就 return。不把未在源码出现的「神秘队列中间件」说成事实。

### 机制/因果链

**ClaimService.claim(owner)**

1. `now = Instant.now()`，UTC 的 `nowUtc`，`leaseUntil = now+60s`。
2. `dao.claimCandidates(nowUtc, 50)` → `NotifyOutboxMapper.selectClaimable`：最多 50 行，`order by outbox_id`，`for update skip locked`。
3. 对每个候选：新 `token`，`dao.claimOutbox(id, owner, token, leaseUntil, nowUtc)` 必须恰好 1 行。失败（别人抢先）则丢掉该候选。
4. 成功则内存对象设 `status=PROCESSING`、`leaseOwner`、`leaseToken`、`leaseUntil`，进入返回列表。
5. UseCase 的事务在这里结束。之后 Worker 才 `dispatch`。

**XML claim 条件（与 select 对齐）**

`update ... set PROCESSING, owner, token, until where id=? and status in (READY, PROCESSING) and available/next/lease 到期`。过期的 PROCESSING 允许被新 Worker 抢走——这就是租约超时回收，不是「永久锁死」。

**Wake 写路径（接到 submit）**

6. Runtime `publishEvent(NotifyOutboxWakeRequestedEvent)`。
7. `NotifyOutboxWakePublisher.publishAfterCommit`：`@DsTxEventListener(phase = AFTER_COMMIT)`。event null 则忽略。构造 `NotifyOutboxWakeSignal.wake(outboxId)`。`transport.publish(REDIS_CHANNEL, signal)` 失败只 warn，**不回滚已提交事务**。然后本地 `localEvents.publishEvent(signal)`，失败同样只 warn。

**Wake 读路径**

8. Subscriber 启动时 `RedisUtils.subscribeAndGetListenerId(channel, NotifyOutboxWakeSignal.class, this::onMessage)`。`onMessage` 调 `worker.onWake`，异常 warn。destroy 时 unsubscribe。
9. Worker `drain`：反复 claim，直到某批空。每批内逐条 `dispatchService.dispatch(outbox)`。日志：`trigger`、claimed 总数、batch 次数。

**dispatch 与领取的衔接（只引用已读方法，不发明）**

10. `DispatchNotificationService.dispatch` 先 `dao.outbox` 再 `leaseActive`（必须仍是 PROCESSING、owner/token 一致、until 未过）。`renewLease` 必须 1 行，否则 return。Provider I/O 之后再 renew 一次；失败则「过期 Worker 不得覆盖新 Worker」。这些是领取之后的篱笆，不是 ClaimService 内部循环。

### 图、表或文本图

**图题 / caption：** 叫醒、领取、投递的事务边界。

```text
[已提交事务] Intent + Outbox(READY)
        |
        v
 AFTER_COMMIT: WakePublisher
        |-- Redis  notify:outbox:wake
        |-- 本地   NotifyOutboxWakeSignal
        v
 Worker.drain
        |
        +-- 短事务: ClaimUseCase.claim(owner)
        |     selectClaimable 50 skip locked
        |     claim 成功 -> PROCESSING + token + 60s
        |     事务提交
        |
        +-- 事务外: DispatchPort.dispatch(outbox)
              leaseActive? renew?  --> Provider I/O --> renew? finish
```

**文字等价物：** 提交事务只保证篮子里出现 READY 行，并在提交后发出不含隐私的叫醒。投递员被叫醒后，用短事务一次最多试图占用 50 封信，占用成功才带上自己的 owner 和一次性 token。短事务结束，数据库连接释放，然后才去访问供应商。叫醒信号里即使带了 outboxId，领取 SQL 也不按这个 ID 点名，避免跳过租约。

**图的边界：** 不展开 `NotifySendPlanner` 限额、IN_APP persist、backoff 公式以外的 dispatch 细节。`backoff` 存在于 dispatch 源码，但不是 claim 的一部分。不把 Redis 当成 Outbox 存储——Outbox 在表 `notify_outbox`。

### 正例、反例与边界

**正例 1：** submit 插入一条 READY。提交后 Publisher 发出 WAKE。同机 Subscriber 或本地 listener 进入 `drain(WAKE)`，claim 得到该行，dispatch 开始。

**正例 2：** Redis 订阅失败。进程仍启动。最多约 60 秒后 `poll` 用同一 `drain(POLL)` 领走任务。

**正例 3：** 两个进程同时 claim。`skip locked` 让它们看到不同候选；`claim` 更新行数为 0 的候选被 filter 掉。不会两个 owner 都带着同一 token 去 dispatch。

**反例 1：** 收到 wake 后 `dispatchService.dispatch(dao.outbox(signal.outboxId()))` 且不 claim。Worker 注释明确禁止「按 outboxId 直投」。那会绕过租约，双投。

**反例 2：** 把 claim 和 HTTP 发短信放进同一个 `@DSTransactional`。与「事务结束后才能执行供应商 I/O」相反。

**反例 3：** 把 `NotifyOutboxWakePublisher` 画进 ClaimUseCase 字段。UseCase 只注入 `NotifyOutboxClaimService`。Publisher 在写路径；Subscriber/Worker 在读路径。

**边界：** 候选含 PROCESSING 且租约过期的行，这是回收，不是正常 READY。`maxAttempts`、DEAD_LETTER、WAITING_RECEIPT 的状态迁移在 dispatch/retry XML `requeue` 里，claim 的 select 并不领取 DEAD_LETTER。hint 允许 null。信号 `type` 固定 `WAKE`。

不确定（不编造）：多实例下 Redis pub/sub 是否至少一次、是否丢失，源码只保证丢失时 poll 兜底；没有在 Java 里实现 Outbox 的持久消息总线。

## 变式与迁移

- **变式 A：空篮子上的 wake。** `claim` 返回空列表，`drain` 的 do-while 立刻停，日志 claimed=0。无害。
- **变式 B：手动 retry。** L-007 `requeueOutbox` 把 WAITING_RECEIPT/DEAD_LETTER 变 READY 并再 wake。领取条件再次满足。
- **变式 C：测试缝合。** Publisher 可用 `NotifyOutboxWakeTransport` 替换 Redis；Subscriber 可用 `NotifyOutboxWakeBus`。生产构造走 `RedisUtils`。
- **迁移到回调：** 供应商回执不经过 claim。它改 Delivery 并 `refreshAggregate`（L-006）。
- **迁移到规范：** 「同一业务事务写 Outbox」是 submit 事务；「租约短事务」是 claim 事务。两段不要合成一句「全程一个事务」。

## 常见误区

1. **「Outbox 有管理端 Controller。」** 本切片无 HTTP Controller。
2. **「wake 的 outboxId 就是任务主键直达。」** 只是 hint；`onWake` 忽略它，始终 `claim(owner)`。
3. **「本地 EventListener 就够了。」** 注释写明不能替代 Redis 跨进程主路径。单实例开发可以「看起来能工作」，多实例会只叫醒提交那一进程。
4. **「claim 返回后仍持有行锁直到短信结束。」** UseCase 事务在 claim 方法结束时提交；skip locked 锁随之释放，保护改成 lease 字段。
5. **「续租失败还能把结果写回。」** dispatch 在 I/O 前后 `renewLease != 1` 就 return；这是规范与 Java 对齐的一点。

## 非评分暂停

想一想：若 wake 比事务提交更早发出，别的进程可能 claim 到未提交的行吗？指出 `@DsTxEventListener(AFTER_COMMIT)` 在防哪一类竞态。

再想：`selectClaimable` 为什么包含 PROCESSING？结合 `lease_until <= now` 用一句话说出「超时回收」。

## 总结、词汇表与下一步

- `NotifyOutboxClaimUseCase` → `NotifyOutboxClaimService` → `NotifyNotificationDao.claimCandidates` / `claimOutbox`（50 条、60 秒、token）。
- Worker：`poll` / `onWake` / `onLocalWake` 都 `drain` → claim → `NotifyDispatchPort.dispatch`。
- Publisher AFTER_COMMIT 发 Redis + 本地信号；Subscriber 订阅失败不挡启动。
- 词汇：claim、lease、skip locked、wake signal、AFTER_COMMIT、hint、fencing token。

课程地图到此结束。八条切片应能从公告草稿走到领取，而不把它们说成「有个消息队列」。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-08 | `NotifyOutboxClaimUseCase.java`、`NotifyOutboxClaimService.java`、`NotifyOutboxWorker.java`、`NotifyOutboxWakeSubscriber.java`、`NotifyOutboxWakePublisher.java` | 领取事务、60s/50 条、drain、订阅、AFTER_COMMIT | `usecase/`、`service/runtime/`、`adapter/worker/` | 2026-09-14 |
| S-N-09 | `NotifyOutboxClaimPort.java`、`NotifyOutboxMapper.xml`、`NotifyOutboxWakeChannels.java`、`NotifyOutboxWakeSignal.java`、`NotifyOutboxWakeRequestedEvent.java`、`DispatchNotificationService` 的 leaseActive/renewLease | skip locked、通道名、hint、续租篱笆 | `port/`、`mapper/notify/`、`support/outbox/`、`service/runtime/` | 2026-09-14 |
| S-N-10 | `notification.md` | 发布写 Outbox；Worker 租约；续租失败禁止写结果 | 「后端分层与状态」 | 2026-09-14 |
