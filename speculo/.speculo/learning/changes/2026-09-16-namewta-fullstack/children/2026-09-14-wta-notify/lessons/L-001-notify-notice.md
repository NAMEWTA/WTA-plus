---
lesson_id: L-001
objective_ids: [OBJ-01]
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
source_ids: [S-N-01, S-N-09, S-N-10, S-N-11]
---

# Lesson 001：公告草稿怎么变成统一通知

## 学完你能做什么

能顺着 `/notify/notice` 把六件事说成真实函数名：列表、详情、保存、发布、撤回、删除。能指出 `NotifyNoticeController` 只注入 `NotifyNoticeUseCase`；UseCase 字段只有 `NotifyNoticeService noticeService` 和 `NotifyNoticePublisherService publisher`。能分清：改草稿不等于发信；发信发生在 `publisher.publish` 调用 `NotificationApplicationService.submit` 之后。

## 先把宏观地图放在桌上

把公告当成一张「还没寄出的信」。运营在通知中心改标题、对象、渠道，信还在抽屉里。只有按下发布，抽屉才会把这封信复印一份快照，再交给统一通知柜台去排队。

```text
浏览器
  GET  /notify/notice/list
  GET  /notify/notice/{noticeId}
  POST /notify/notice/save
  POST /notify/notice/{noticeId}/publish
  POST /notify/notice/{noticeId}/retract
  POST /notify/notice/remove
           |
           v
NotifyNoticeController  --inject-->  NotifyNoticeUseCase
                                         |  noticeService: NotifyNoticeService
                                         |  publisher:     NotifyNoticePublisherService
                                         |
           +-----------------------------+-----------------------------+
           | list/get/save/retract/remove | publish 成功且 lifecycle 变更 |
           v                              v
 NotifyNoticeService                 publisher.publish(notice)
   dao: NotifyPersistenceDao           NoticeAudiencePolicy.normalize
   userService: UserService            UserService 解析 USER / USER_TYPE
                                       dao.insertSnapshot
                                       notificationService.submit(...)
```

**类比失效处：** 抽屉比喻只帮你记住「草稿和投递是两段」。它不解释锁、幂等键、Outbox。那些是后面的课。本课先把 HTTP 六条路径钉在 Java 方法上。

## 核心概念与机制

### 直觉讲解

小孩子画画：先在草稿本上改，满意了再复印一张贴到墙上。墙上那张不能再涂，要改就先撕下来（撤回），再回到草稿本。

`NotifyNoticeController` 是窗口柜员：检查登录权限，把 JSON 交给 `NotifyNoticeUseCase`。UseCase 是带印章的办事员：写操作加 `@DSTransactional`，读操作直接转发。真正的规矩在两个下游：

- `NotifyNoticeService` 管抽屉：分页、详情、新增、修改、发布时改 lifecycle、撤回、删除。持久化只走 `NotifyPersistenceDao`。
- `NotifyNoticePublisherService` 管复印和交柜台：冻结目录、写 `NotifyNoticeSnapshot`、构造 `NotificationCommand` 调用 `notificationService.submit`。

窗口柜员**不**直接找 DAO，也**不**自己发短信。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以 Java 为准） |
| --- | --- | --- |
| 公告草稿 | notice draft | 表 `notify_notice` 上一行；`lifecycle` 为 `DRAFT` 或 `RETRACTED` 才允许编辑/发布/删除 |
| 生命周期 | lifecycle | `NotifyNoticeService` 写入的 `DRAFT` / `PUBLISHED` / `RETRACTED`，与 `status` 字段不是同一件事 |
| 发送范围 | audience | `NoticeAudiencePolicy.Audience`：`recipientType` + 用户 ID 列表 + 用户类型 ID 列表 + 渠道列表 |
| 内容快照 | notice snapshot | `NotifyNoticeSnapshot`：发布时冻结标题、正文、类型、路径；版本号递增 |
| 统一提交 | submit | `NotificationApplicationService.submit(NotificationCommand)`；公告场景码是 `notice-published` |
| 分层模块 | layered module | `wta-notify` 固定 `controller -> usecase -> service -> dao -> mapper`（S-N-11） |

`status` 在 `publish` 里被设成 `"0"`，`add` 时草稿是 `"1"`。不要把 `"0"` 读成「没有通知」。它是公告表自己的状态码，不是统一通知的 `NotificationStatus`。

### 机制/因果链

1. **列表。** `GET /list` + 权限 `notify:notice:list` → `Controller.list` → `UseCase.list` → `NotifyNoticeService.page` → `NotifyPersistenceDao.page`（标题模糊、类型/status 精确、按创建时间与 `noticeId` 倒序）。
2. **详情。** `GET /{noticeId}` + `notify:notice:query` → `UseCase.get` → `Service.get`。找不到抛 `ServiceException("通知不存在")`。`toVo` 把 JSON 列还原成 `recipientIds` / `userTypeIds` / `channels`；渠道缺省 `IN_APP`。
3. **保存。** `POST /save`。`noticeId == null` 用权限 `notify:notice:add` 并走 `Service.add`（lifecycle=`DRAFT`，status=`1`）；否则 `notify:notice:edit` 并走 `Service.update`。`toEntity` 先 `NoticeAudiencePolicy.normalize`：`USER` 必须有正数用户 ID，`USER_TYPE` 必须有类型 ID，`ALL` 必须两边都空。草稿阶段就调用 `UserService.selectNotificationUsers` / `selectUsersByUserTypeIds`，不能靠手填 ID 绕过目录。
4. **发布。** `POST /{noticeId}/publish` + `notify:notice:publish`。`UseCase.publish` 整段 `@DSTransactional`：`noticeService.publish(id)` 先 `findForUpdate` 加锁；若已是 `PUBLISHED` 返回 `null`，UseCase **不再**调用 publisher（幂等：重复发布不二次投递）。否则要求当前可编辑（仅 `DRAFT`/`RETRACTED`），写成 `PUBLISHED`、`status="0"`、填 `publishedAt`、清空 `retractedAt`。非 null 时 `publisher.publish(notice)`。
5. **Publisher 内部。** 再 normalize 一次 audience。`ALL` → 提交时 `recipientIds` 为空且类型 `ALL`（全体解析留给应用提交，见 L-007）。`USER` → `userService.selectNotificationUsers`。`USER_TYPE` → `selectUsersByUserTypeIds`，提交时改成 `recipientType=USER`。空用户抛「所选范围没有可接收通知的正常用户」。然后 `latestSnapshot` 取版本，`insertSnapshot` 写入冻结内容，`idempotencyKey` 为 `notice-published:{noticeId}:{version}`，`NotificationMode.ASYNC`，`NotificationStrategy.ALL`，场景 `notice-published`。
6. **撤回。** `POST /{noticeId}/retract`。已是 `RETRACTED` 则原样返回（UseCase 计 1 行）。不是 `PUBLISHED` 则拒绝。撤回**只改公告 lifecycle**，源码里没有取消已经 submit 出去的 Intent。
7. **删除。** `POST /remove` 接收 `Long[]`。ID 必须为正；`distinct().sorted()` 后逐个 `requireLocked` + `requireEditable`，避免交叉锁。已发布未撤回的不能删。

规范 `notification.md` 写「UseCase 负责目标解析」。**冲突（Java 优先）：** 目标规范化在 `NoticeAudiencePolicy` 与两个 Service；UseCase 只做事务边界和「publish 返回非 null 才 publisher.publish」。规范写「同一事务写意图、接收者快照、Outbox」：公告内容快照是 `NotifyNoticeSnapshot`；接收者行和 Outbox 在 `submit` 路径（L-007 / L-008），由本次 `@DSTransactional` 包住 `submit`。

### 图、表或文本图

**图题 / caption：** 发布成功时，lifecycle 更新与统一提交的顺序。

```text
UseCase.publish(id)                @DSTransactional
        |
        v
Service.publish(id)
  findForUpdate -> 已 PUBLISHED? --yes--> return null --> 结束（不 submit）
        | no
        v
  requireEditable (DRAFT/RETRACTED)
  lifecycle=PUBLISHED, status=0, publishedAt=now
  dao.update
        |
        v
Publisher.publish(notice)
  resolve audience (ALL / USER / USER_TYPE)
  insert NotifyNoticeSnapshot (version+1)
  NotificationApplicationService.submit
        |
        v
  Intent + Recipient + Delivery + Outbox   （下一课之后才投递）
```

**文字等价物：** 发布先在公告表上加锁。若这一版已经是发布态，函数立刻停，不会再生成快照或通知意图。若允许发布，先把 lifecycle 改成 `PUBLISHED` 并落库，再由 Publisher 解析收件人、写入内容快照、调用统一提交。统一提交写出的是意图和 Outbox，不是供应商回执。图里没有短信网关。

**图的边界：** 不画出 Worker、回调、收件箱。撤回不出现在这张图里，因为它不调用 Publisher。

### 正例、反例与边界

**正例 1：** 新建公告。`noticeId` 为空 → Controller 检查 `notify:notice:add` → `UseCase.save` → `noticeService.add` → `dao.insert`。此时没有 Snapshot，也没有 Intent。

**正例 2：** 草稿对象为 `USER` 且目录能解析出用户。`publish` 返回实体 → `publisher.publish` → `selectNotificationUsers` → `submit`，幂等键带上 snapshot 版本。

**正例 3：** 对同一 `noticeId` 连续点两次发布。第一次变成 `PUBLISHED` 并 submit；第二次 `Service.publish` 返回 `null`，Publisher 不跑。Controller 两次都 `R.ok()`。

**反例 1：** 把 `POST /save` 当成「发送」。save 只写 `notify_notice`。没有 `publisher.publish` 就没有 `submit`。

**反例 2：** `recipientType=USER` 但 `recipientIds` 为空。`NoticeAudiencePolicy.normalize` 抛「发送对象类型与所选目标不匹配」。空指定**不会**退化成全体用户。

**反例 3：** 已发布未撤回就 `remove`。`requireEditable` 拒绝：「仅草稿或已撤回通知允许编辑、发布或删除」。

**边界：** `USER_TYPE` 只存在于公告草稿。提交统一通知时被压成 `USER`。应用 API 的 `validate` 并不接受 `USER_TYPE`（见 L-007）。渠道只允许 `IN_APP` / `SMS` / `MAIL`。

## 变式与迁移

- **变式 A：撤回后再发布。** `retract` 把 lifecycle 改回可编辑；再次 `publish` 会新快照版本，幂等键因此不同，会再 submit 一次。这不是「同一键覆盖」。
- **变式 B：全体用户。** 公告侧 `ALL` 且 ID 列表必须空。Publisher 把空列表和 `recipientType=ALL` 交给 submit，由运行时分页拉活跃用户，而不是把全量 ID 塞进公告 JSON。
- **变式 C：只改标题不发布。** 走 `update`：锁行、要求可编辑、保留原 lifecycle / publishedAt / retractedAt。
- **迁移到收件箱：** 用户在 `/notify/inbox` 看到的不是 `notify_notice` 行，而是后续 IN_APP 投递写入的 `NotifyMessage`（L-002）。
- **迁移到配置：** 若渠道含 MAIL/SMS，正文权威在场景绑定 `notice-published`（L-003），不是公告表自己去连 SMTP。

## 常见误区

1. **「Controller 里已经有 Service 了。」** 没有。`NotifyNoticeController` 唯一字段是 `NotifyNoticeUseCase noticeUseCase`。
2. **「UseCase.publish 自己解析用户。」** 没有。解析在 `NotifyNoticePublisherService.publish`。UseCase 只判断 `notice != null`。
3. **「撤回等于取消短信。」** 源码撤回只更新公告行。已进 Outbox 的任务要靠应用取消/监控（L-007 / L-005），本切片没有调用 `cancel`。
4. **「规范怎么写 UseCase 就怎么分工。」** 规范是意图；本模块 Java 把规则放进 Service / Policy。冲突以工作树为准。
5. **「status 和 lifecycle 是同义词。」** `add` 时 status=`1` 且 lifecycle=`DRAFT`；`publish` 时 status=`0` 且 lifecycle=`PUBLISHED`。列表筛选用的是 status 列。

## 非评分暂停

想一想（不必写下来，本课不给标准答卷）：若产品说「发布按钮连点两次不能发两封」，你指出哪一个 `return null` 在保护它？若产品说「撤回后标题改了再发，用户应看到新标题」，你指出哪个字段让第二次 submit 不撞上第一次的幂等键？

再想：为什么 `save` 在草稿阶段就要调用 `UserService`，而不是等到 `publish`？

## 总结、词汇表与下一步

- 六条 HTTP 全部进入 `NotifyNoticeUseCase` 的同名方法：`list` / `get` / `save` / `publish` / `retract` / `remove`。
- 下游两个字段：`NotifyNoticeService`（抽屉与锁）、`NotifyNoticePublisherService`（快照 + `submit`）。
- 可编辑集合只有 `DRAFT` 与 `RETRACTED`；已发布返回 `null` 从而跳过 Publisher。
- `NoticeAudiencePolicy`：空目标不能变成全体。
- 词汇：lifecycle、audience、snapshot、submit、layered。

下一步：L-002 看用户怎么读已经投递进站内信表的消息。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-01 | `NotifyNoticeController.java`、`NotifyNoticeUseCase.java` | HTTP 路径、权限、UseCase 字段与 `@DSTransactional` | `controller/admin`、`usecase` | 2026-09-14 |
| S-N-09 | `NotifyNoticeService.java`、`NotifyNoticePublisherService.java`、`NoticeAudiencePolicy.java`、`NotifyPersistenceDao.java` | 生命周期、加锁、快照、submit 参数 | `service/`、`domain/policy/`、`dao/` | 2026-09-14 |
| S-N-10 | `.agents/skills/engineering-standards/references/notification.md` | 分层与「UseCase 负责解析」的规范句；与 Java 冲突已标出 | 「后端分层与状态」 | 2026-09-14 |
| S-N-11 | 登记表：wta-notify = layered | Controller 不得直连 DAO | 模块事实 | 2026-09-14 |
