---
lesson_id: L-006
objective_ids: [OBJ-06]
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
source_ids: [S-N-06, S-N-09, S-N-10]
---

# Lesson 006：供应商回调：先验签，再单向升级投递

## 学完你能做什么

能口述 `POST /notify/callback/{channel}`：`ProviderCallbackController`（`@SaIgnore`）注入 `ProviderCallbackUseCase`，并用 `@Value("${notify.callback-secret:}")` 读取共享密钥。UseCase 唯一端口字段是 `ProviderCallbackPort callbackService`。能按源码顺序说出：HMAC-SHA256 验签 → 解析 `eventId`/`timestamp`/`providerKey` → 300 秒时间窗 → 再要 `providerMessageId`/`status` → `callbackService.apply`。能说明实现类 `ProviderCallbackService` 如何拒绝乱序降级和重复刷新。

## 先把宏观地图放在桌上

把回调当成「快递公司回传签收短信」。任何人都能往这个匿名邮筒塞纸，所以第一件事不是改数据库，而是核对信封上的蜡封（HMAC）。蜡封不对，里面的「已签收」三个字一律不信。

```text
供应商 HTTP
  POST /notify/callback/{channel}
  Header: X-Notify-Signature
  Body:   原始 JSON 字符串（不要先 parse 再签名）
           |
           v
ProviderCallbackController   @SaIgnore
  secret = notify.callback-secret
  callbackUseCase.apply(channel, signature, payload, secret)
           |
           v
ProviderCallbackUseCase        @DSTransactional
  1 verify HMAC-SHA256(payload, secret) vs signature hex
  2 parseMap: eventId, timestamp, providerKey 非空
  3 |now - Instant.parse(timestamp)| <= 300s
  4 providerMessageId, status 非空
  5 callbackService.apply(...)     类型：ProviderCallbackPort
           |
           v
ProviderCallbackService implements ProviderCallbackPort
  规范化 status、查 deliveryByProvider、单向升级、CAS 更新、refreshAggregate
```

**类比失效处：** 蜡封比喻只有一把共享 secret（配置项），不是每个厂商一把。时间窗是 300 秒绝对值，不是「业务过期日」。内存里的 `seenEvents` 也不是跨进程的事件表。

## 核心概念与机制

### 直觉讲解

`ProviderCallbackController` 放在 `controller.anonymous`，类上 `@SaIgnore`：供应商没有登录令牌。因此安全全靠签名和 UseCase 校验。失败捕获 `ServiceException | IllegalArgumentException` 后 `R.fail(message)`，成功 `R.ok()`。

UseCase 注释写「验签、解析和幂等更新」。真正的投递行更新在端口实现里。字段名叫 `callbackService`，类型却是 `ProviderCallbackPort`——和 L-004 一样，依赖端口而不是实现类名。

**源码编码事实：** `eventId`/`timestamp`/`providerKey` 为空、时间窗失败、时间戳解析失败这三处 `IllegalArgumentException` 文案在工作树里是乱码字节，不是干净中文。不要把乱码当成状态机的一部分；能确定的是抛错且 Controller 会把 `getMessage()` 原样放进 `R.fail`。后面「回调参数不完整」「回调验签失败」仍是可读中文。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 匿名回调入口 | anonymous callback | `POST /notify/callback/{channel}`，无 SaToken 登录，靠 HMAC |
| 原文字符串 | raw payload | `@RequestBody String payload`；签名对这串 UTF-8 字节计算，不是对 parse 后的 Map |
| HMAC 验签 | HMAC-SHA256 verify | `Mac.getInstance("HmacSHA256")`，`MessageDigest.isEqual` 比较，避免短电路计时 |
| 时间窗 | timestamp window | `Duration.between(Instant.parse(timestamp), Instant.now())` 绝对值大于 300 秒则拒绝 |
| 单向升级 | monotonic status | `rank`：DELIVERED=4，UNDELIVERABLE/FAILED=3，ACCEPTED=2，其它=1；只允许 rank 升高 |
| 事件去重 | event de-dup | 进程内 `ConcurrentHashMap seenEvents`，10 分钟过期；命中则直接 return |

规范句「必须验证 HMAC 原文、providerKey、eventId 和时间窗，状态只能单向升级，重复事件不重复刷新聚合状态」与 Java 总体同向。规范未写 secret 是单一配置项、也未写 seenEvents 仅内存；这些以 Java 为准。

### 机制/因果链

1. **验签。** `secret` 或 `signature` 空 → verify false → `回调验签失败`。HMAC 输出与 `HexFormat.parseHex(signature)` 常量时间比较。异常（含非法 hex）视为失败。
2. **元数据。** JSON Map 取 `eventId`、`timestamp`、`providerKey`，`Objects.toString(..., "")` 后若 blank 抛错。
3. **时间窗。** `Instant.parse` 失败进入 `DateTimeException` 分支。成功则与现在相差超过 300 秒抛错。
4. **投递键。** `providerMessageId`、`status` blank → `回调参数不完整`。
5. **Port.apply。** 渠道/消息号/状态/providerKey/eventId 再校验一遍。status 大写后必须属于 `ACCEPTED, DELIVERED, UNDELIVERABLE, FAILED, UNKNOWN`。清掉 10 分钟前的 seenEvents；eventId 已见则 return。`dao.deliveryByProvider(channel.upper, providerKey, providerMessageId)` 找不到则「未找到对应投递记录」。
6. **迁移规则。** 当前已是 `DELIVERED`/`CANCELLED`/`FAILED`/`UNDELIVERABLE` → 不允许。否则需要 `rank(next) > rank(current)`。不允许时仍 `seenEvents.putIfAbsent` 后 return（不 refresh）。
7. **写入。** 记下 previousStatus，写入新 status；ACCEPTED 填 `acceptedAt`；DELIVERED 补 acceptedAt（若空）和 `deliveredAt`。`dao.updateDeliveryStatus(id, previousStatus, delivery)` 行数为 0 则 return（**不**记 seenEvents）。成功才记 eventId，并 `dispatchService.refreshAggregate(intentId)`。

### 图、表或文本图

**图题 / caption：** 回调从蜡封到聚合刷新的闸门。

```text
payload + signature + secret
        |
        v
   HMAC match? --no--> fail
        | yes
        v
 eventId/timestamp/providerKey 齐全?
        | yes
        v
 |t_now - t_event| <= 300s ?
        | yes
        v
 providerMessageId + status 齐全?
        | yes
        v
 seenEvents 已有 eventId? --yes--> 静默成功（不 refresh）
        | no
        v
 找到 Delivery? --no--> 业务失败
        |
        v
 允许单向升级? --no--> 记 eventId，不 refresh
        | yes
        v
 CAS 按 previousStatus 更新 --0 行--> 不 refresh
        | 1 行
        v
 记 eventId，refreshAggregate(intentId)
```

**文字等价物：** 每一道闸门失败都不会改投递行。验签和时间窗在 UseCase；是否找得到行、状态能不能升、CAS 是否抢到，在 `ProviderCallbackService`。只有 CAS 成功才刷新意图聚合状态。内存去重在单进程有效；多实例并发靠 CAS 的 previousStatus 条件，失败者行数为 0 且不刷新。

**图的边界：** 不画出供应商如何生成 signature。不画出 Outbox 状态机。回调不领取新任务。

### 正例、反例与边界

**正例 1：** PENDING 投递已有 `providerKey`+`providerMessageId`。回调 status=`ACCEPTED`，签名正确、时间在 300 秒内。行更新，聚合刷新。

**正例 2：** 随后同一 eventId 再 POST。`seenEvents.containsKey` 直接 return。UseCase 仍在事务里，但 Port 立刻结束。

**正例 3：** 先 ACCEPTED 再另一 eventId 的 DELIVERED。rank 4>2，允许；补 `deliveredAt`。

**反例 1：** 先把 JSON parse 成对象再序列化去算签名。Controller 签名的是原始 body 字符串，字节不一致会验签失败。

**反例 2：** DELIVERED 之后再来 FAILED。终态集合禁止迁移，即使 rank 接近。

**反例 3：** 登录权限保护这个 URL。类上 `@SaIgnore`，靠 secret，不靠用户 token。

**边界：** `UNKNOWN` rank=1，不能用来覆盖 ACCEPTED。时间戳必须是 `Instant.parse` 能吃的 ISO-8601。channel 路径变量会再 `toUpperCase` 后查库。secret 配置缺省是空串，verify 直接 false——未配置等于拒绝所有回调。

## 变式与迁移

- **变式 A：乱序。** 先到 DELIVERED 再到 ACCEPTED：终态已 DELIVERED，拒绝。看板保持已送达。
- **变式 B：进程重启。** seenEvents 清空。同一 eventId 再来：若状态已升且不允许迁移，仍不 refresh；若第一次根本没成功，可以再试。
- **迁移到监控：** `refreshAggregate` 之后 snapshot 显示新的 Intent 状态（L-005）。
- **迁移到 Outbox：** 回调不改 lease。Worker 在 WAITING_RECEIPT 上的后续，不在本切片 HTTP 里。

## 常见误区

1. **「Controller 注入 ProviderCallbackService。」** 注入的是 `ProviderCallbackUseCase`。UseCase 注入 `ProviderCallbackPort`。
2. **「验签在 Service。」** HMAC 与时间窗在 UseCase；Service 假定这些已经通过。
3. **「重复回调会把聚合再算一遍。」** 去重或非法迁移路径不调用 `refreshAggregate`；CAS 0 行也不调用。
4. **「status 可以任意字符串。」** 集合外抛「回调状态不支持」。
5. **「规范写了就有按厂商区分的密钥。」** 工作树是单一 `notify.callback-secret`。

## 非评分暂停

想一想：为什么 `updateDeliveryStatus` 要把「旧状态」写进 WHERE？若两个实例同时处理两条不同 eventId、都想从 PENDING 升到 ACCEPTED，会发生什么？

再想：把 `@RequestBody` 改成 `Map` 再 `JsonUtils.toJsonString` 去验签，会踩中哪一条闸门？

## 总结、词汇表与下一步

- `POST /notify/callback/{channel}` → `ProviderCallbackUseCase.apply` → `ProviderCallbackPort.apply`（`ProviderCallbackService`）。
- 先 HMAC 原文与 300 秒窗，再单向升级 + CAS + 条件刷新聚合。
- 词汇：HMAC-SHA256、raw payload、timestamp window、monotonic status、event de-dup。

下一步：L-007 业务如何 submit/query/retry/cancel，从而产生回调要对上的 Delivery。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-06 | `ProviderCallbackController.java`、`ProviderCallbackUseCase.java` | 路径、SaIgnore、secret、验签与时间窗、端口字段 | `controller/anonymous`、`usecase` | 2026-09-14 |
| S-N-09 | `ProviderCallbackPort.java`、`ProviderCallbackService.java`、`NotifyNotificationDao.deliveryByProvider` | 状态集合、rank、seenEvents、CAS、refreshAggregate | `port/`、`service/runtime/`、`dao/` | 2026-09-14 |
| S-N-10 | `notification.md` | HMAC 原文、providerKey、eventId、时间窗、单向升级 | 「后端分层与状态」 | 2026-09-14 |
