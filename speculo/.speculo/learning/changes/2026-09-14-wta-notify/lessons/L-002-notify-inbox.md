---
lesson_id: L-002
objective_ids: [OBJ-02]
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
source_ids: [S-N-02, S-N-09, S-N-10]
---

# Lesson 002：收件箱只读自己的站内信

## 学完你能做什么

能口述 `/notify/inbox` 四条路径如何从 `NotifyInboxController` 进 `NotifyInboxUseCase`，再进它唯一字段 `NotifyInboxService inboxService`。能分清 **已见 seen** 和 **已读 read**：`seen` 调用 `inboxService.mark(id, userId, false)`，`read` 调用 `mark(..., true)`，`read-all` 调用 `markAll`。能说明列表读的是 `NotifyMessage` + `NotifyMessageRecipient`，不是公告表 `notify_notice`。

## 先把宏观地图放在桌上

把收件箱当成自己书包里的纸条。公告发布、业务 submit 之后，站内信投递员把纸条塞进书包（那是 L-007/L-008 的 `InAppNotificationService.persist`）。本课只讲：你打开书包看哪些纸条、把哪一张标成「看过了」或「读完了」。

```text
已登录用户
  GET  /notify/inbox                  -> list()
  POST /notify/inbox/{messageId}/seen -> seen()
  POST /notify/inbox/{messageId}/read -> read()
  POST /notify/inbox/read-all         -> readAll()
           |
           v
NotifyInboxController   @SaCheckLogin
  取 LoginHelper.getUserId()
  注入 NotifyInboxUseCase inboxUseCase
           |
           v
NotifyInboxUseCase
  唯一字段：NotifyInboxService inboxService
           |
           v
NotifyInboxService
  唯一字段：NotifyNotificationDao dao
           |
           +-- list:     dao.messageRecipients(userId, 500) + dao.messages(...)
           +-- mark:     dao.messageRecipient + dao.update
           +-- markAll:  dao.markAllMessages
```

**类比失效处：** 书包比喻不包含推送铃铛。`InAppNotificationService.pushRealtime` 会发实时提示，但收件箱 HTTP 不负责推送，失败也不回滚已落库的纸条。

## 核心概念与机制

### 直觉讲解

老师把通知塞进每个学生的抽屉。学生做三件事：打开抽屉看目录（list）、扫一眼封面（seen）、把全文读完（read）。全部标已读是一次性把抽屉里没标完的都补上时间戳。

`NotifyInboxController` 类上有 `@SaCheckLogin`，所以四条路都要登录。它**不**接收前端传来的 userId，只用 `LoginHelper.getUserId()`。UseCase 四个方法几乎是一行转发，真正规则在 `NotifyInboxService`。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 收件箱消息 | inbox message | `NotifyInboxMessageVo`：来自 `NotifyMessage` 的标题正文，加上本用户行上的 `seenTime`/`readTime` |
| 收件关系 | message recipient | `NotifyMessageRecipient`：`(messageId, userId)` 一行，已见已读时间戳活在这里 |
| 已见 | seen | `seenTime` 从 null 变成现在；不要求 `readTime` |
| 已读 | read | 若 `seenTime` 仍空会一并补上；仅当 `readTime` 为空才写入 |
| 幂等标记 | idempotent mark | `mark` 重复调用：已有时间戳则不覆盖；`markAll` 只更新「seen 或 read 仍为空」的行 |

规范说站内信、收件箱、消息盒子读同一 `/notify/inbox` 数据源。Java 侧本切片只暴露这一组 HTTP；它读 `NotifyNotificationDao.messageRecipients` / `messages`，与公告表无关。

### 机制/因果链

1. **列表 GET `/notify/inbox`。** 无额外 `@SaCheckPermission`（只要登录）。`UseCase.list(userId)` → `Service.list`：`dao.messageRecipients(userId, 500)` 按创建时间、messageId 倒序，`Math.clamp` 把 limit 卡在 1..500。没有收件行则空列表。再用收件行上的 `messageId` 批量 `dao.messages`。只保留两边都能对上的行，组装 Vo：`category`/`noticeType`/`type`/`source`/`title`/`message`/`content`/`path`/`createTime` 来自消息；`seenTime`/`readTime` 来自收件行。`channelsJson` 空则渠道列表为 `["IN_APP"]`。
2. **已见 POST `/{messageId}/seen`。** 权限 `notify:inbox:seen`。`UseCase.seen` → `inboxService.mark(messageId, userId, false)`。找不到收件行返回 `false`；Controller **忽略**返回值，一律 `R.ok()`。
3. **已读 POST `/{messageId}/read`。** 权限 `notify:inbox:read`。`mark(..., true)`：先保证 `seenTime`，再在 `read=true` 且 `readTime==null` 时写 `readTime`，然后 `dao.update`。
4. **全部已读 POST `/read-all`。** 同一权限 `notify:inbox:read`。`UseCase.readAll` → `markAll` → `dao.markAllMessages(userId, now)`：把该用户 `seenTime` 或 `readTime` 为空的行一次写成同一个 `now`。
5. **谁写入这些行？** 本切片不写消息正文。`InAppNotificationService.persist` 在 Outbox 投递 IN_APP 时插入 `NotifyMessage`（`messageId` 用 intentId）和每个用户的 `NotifyMessageRecipient`。没有投递成功，收件箱就是空的。

### 图、表或文本图

**图题 / caption：** 一条站内信如何同时出现在两张表，以及已见/已读改哪一张。

```text
NotifyMessage                         NotifyMessageRecipient
 messageId = intentId                   messageRecipientId
 title / content / path                 messageId  --------+
 channelsJson                           userId            |
 createTime                             seenTime          |
                                        readTime          |
                                              ^           |
                                              |           |
GET /inbox  把两表拼成 Vo <---------------+-----------+
POST /seen  只改 recipient.seenTime（若空）
POST /read  改 seenTime（若空）和 readTime（若空）
POST /read-all  对该 userId 批量补两个时间
```

**文字等价物：** 消息正文只存一份。每个用户是否看过，记在收件关系表。列表接口先按当前用户取出最多 500 条收件行，再补正文。已见已读从不改 `NotifyMessage` 标题。找不到属于你的收件行时，标记函数返回 false，HTTP 仍成功。

**图的边界：** 不画短信、邮件、监控 snapshot。也不画 `PushHelper.publishMessage`：那是投递时的铃铛，不是本 Controller。

### 正例、反例与边界

**正例 1：** 登录用户拉列表。`LoginHelper.getUserId()` 为 42 → 最多 500 条自己的收件行 → Vo 里能看到自己的 `readTime`。不能传入别人的 userId。

**正例 2：** 对已有 `seenTime` 再调 `seen`。`item.getSeenTime() == null` 为假，时间戳保持原值，仍然 `dao.update`。这是「再标一次不把时间改晚」的幂等。

**正例 3：** `read-all` 把未读和未见到的行都写成同一时刻的 seen+read。已经有两个时间戳的行不在 SQL 的 `isNull(seen) or isNull(read)` 条件里。

**反例 1：** 把 `GET /notify/notice/list` 当成用户收件箱。那是运营公告目录，权限是 `notify:notice:list`，读的是 `NotifyNotice`。

**反例 2：** 前端自己传 `userId` 想看别人的盒子。本 Controller 没有这个参数。

**反例 3：** 以为 `list` 也要 `notify:inbox:read`。源码里 list 只有类上的 `@SaCheckLogin`；没有读权限仍可能列出，但不能调用 read/seen（若权限指令生效）。

**边界：** 超过 500 条的更早消息本接口不返回。消息存在但收件行不属于你：`mark` 为 false，看起来像成功。`channels` 缺省只有站内信标签，不代表短信已经发出。

## 变式与迁移

- **变式 A：只扫一眼。** 产品若把打开列表当「已见」，应对每条调 `seen`，不要误调 `read`；`read` 会写 `readTime`。
- **变式 B：工作流路径。** `InAppNotificationService.resolveCategory`：path 以 `/workflow` 开头则 `category=workflow`，以 `/notify/notice` 开头则 `notice`。收件箱 Vo 会带上这个 category，但本切片不按 category 过滤。
- **迁移到公告：** 公告 `pathSnapshot` 是 `/notify/notice?noticeId=` + id。用户点开的是公告详情权限，不是 inbox 的 mark。
- **迁移到监控：** 监控 `NotificationDeliveryView` 有 `readAt` 字段，来自投递表，不是 inbox 的 `readTime`。两套时间不要混着汇报。

## 常见误区

1. **「UseCase 自己查表。」** `NotifyInboxUseCase` 只注入 `NotifyInboxService`。DAO 在 Service 里。
2. **「seen 和 read 是同一个布尔。」** `mark` 的第三个参数 `read`：false 只保证 seen；true 才尝试写 read。
3. **「HTTP 成功表示一定改到了行。」** Controller 丢弃 `boolean` / `int` 返回值。
4. **「收件箱就是公告列表的个人过滤。」** 数据源是消息表，由 IN_APP persist 写入，intentId 当 messageId。
5. **「规范里的 SSE 由这个 Controller 推。」** 本类四个方法都是请求-响应，没有 SSE 映射。

## 非评分暂停

想一想：若权限系统只给了登录、没给 `notify:inbox:read`，用户还能不能看见标题？能不能把 `readTime` 写上？

再想：`markAll` 的 SQL 条件是「seen 空 **或** read 空」。一条已经 seen 但未 read 的行，会被怎样处理？

## 总结、词汇表与下一步

- Controller → UseCase → `NotifyInboxService` → `NotifyNotificationDao`。
- 方法：`list` / `seen`→`mark(false)` / `read`→`mark(true)` / `readAll`→`markAll`。
- userId 只来自登录上下文；列表上限 500。
- 词汇：inbox、seen、read、message recipient、idempotent mark。

下一步：L-003 配置发件账号和场景绑定，否则 MAIL/SMS 在投递时会 `UNBOUND_CHANNEL`。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-02 | `NotifyInboxController.java`、`NotifyInboxUseCase.java` | 路径、权限、UseCase 对 Service 的四次转发 | `controller/admin`、`usecase` | 2026-09-14 |
| S-N-09 | `NotifyInboxService.java`、`NotifyNotificationDao.java`、`InAppNotificationService.java`、`NotifyInboxMessageVo.java` | 500 条、mark 幂等、persist 写入 | `service/runtime`、`dao`、`domain/vo` | 2026-09-14 |
| S-N-10 | `notification.md` | 收件箱与消息盒子同一数据源；SSE 不承担持久化 | 「目标与渠道」 | 2026-09-14 |
