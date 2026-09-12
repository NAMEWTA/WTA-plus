# 系统消息、通知公告与 notify

先记住三个答案：

1. `sys_message` 可以算一个轻量的站内消息盒子，但还不是完整的站内信系统。
2. `sys_notice` 保存公告原文，`sys_message` 保存一次发布出来的消息快照；两者不是同一张表。
3. 自研 `notify` 主要负责把邮件、短信送到系统外部。目前公告发布没有经过 `notify`。

## 先看全图

```text
                         同一个后端应用

[管理员新增公告]
        |
        v
[公告管理入口]
        |
        +---------------------> [公告原文表 sys_notice]
        |                              保存标题、正文、状态
        |
        v
[系统消息服务]
        |
        +---------------------> [消息记录表 sys_message]
        |                              保存消息快照，供以后查询
        |
        v
[实时推送工具] -> [Redis 消息通道] -> [在线用户的网页]
                                             |
                                             v
                                      [右上角消息盒子]


[需要发邮件或短信的业务]
        |
        v
[notify 通知内核] -> [邮件或短信适配器] -> [外部邮箱或手机]
        |
        v
[通知监控日志 sys_notify_log / sys_notify_delivery_log]

当前没有自动箭头：

[公告管理入口] - - 不会自动调用 - -> [notify 通知内核]
[notify 通知内核] - - 不会自动写入 - -> [sys_message]
```

可以把它们想成三样东西：

- `sys_notice` 是公告栏里保存的“正式原稿”。
- `sys_message` 是投进用户站内消息盒子的“消息单”。
- `notify` 是把信送到外部邮箱或手机的“投递工具”。

## 一步一步看

### 第一步：`sys_notice` 和 `sys_message` 各自保存什么

```text
[sys_notice：公告原稿]
  +-- 公告编号
  +-- 标题
  +-- 类型：通知或公告
  +-- 完整正文
  +-- 状态：正常或关闭

[sys_message：消息快照]
  +-- 消息编号
  +-- 分组：系统、通知、工作流
  +-- 类型和来源
  +-- 标题、摘要、详细内容
  +-- 扩展数据
  +-- 点击后的跳转路径
  +-- 目标用户；0 表示所有用户
```

因此，`sys_notice` 负责“公告本身是什么”，`sys_message` 负责“消息盒子要向谁展示什么”。

`sys_message` 可以叫站内消息记录，也可以把它看成统一消息盒子的后端存储。不过它还不是完整的站内信系统：

```text
完整站内信常见能力       当前 sys_message

每位收件人一条投递记录   没有，多个用户编号放在一个字符串里
服务端保存已读状态       没有，已读编号保存在浏览器本机
长期分页历史             没有，只取最近 30 天、每个分组最多 100 条
回复、归档、删除          没有
系统、公告、工作流归类   有
离线后重新查询           有
在线实时到达             有
```

所以最准确的称呼是“轻量站内消息盒子”，而不是功能完整的“统一收件箱平台”。

### 第二步：公告新增后怎样进入消息盒子

当前代码没有单独的“发布公告”接口。管理员调用新增接口时，保存和消息发布连续发生：

```text
[网页提交新增公告]
        |
        v
POST /system/notice
        |
        v
[SysNoticeController.add]
        |
        +-- 1. noticeService.insertNotice
        |          |
        |          v
        |     INSERT sys_notice
        |
        +-- 2. 组装公告消息
        |          类型 = notice
        |          来源 = notice
        |          data 带公告编号、标题、正文和状态
        |          path 带公告详情跳转地址
        |
        +-- 3. messageService.publishAll
                   |
                   +-- 先 INSERT sys_message
                   |      category = notice
                   |      send_user_ids = 0
                   |
                   +-- 再交给实时推送工具
                              |
                              v
                         Redis 消息通道
                              |
                              v
                      所有在线用户的网页
```

关键点是 `publishAll` 不只是“推一下”。它先调用 `storeAll` 生成 `sys_message` 记录，再调用 `PushHelper` 做实时广播。消息记录得到编号后，这个编号也会放进实时消息体，供前端去重和标记已读。

### 第三步：用户打开页面时怎样取到消息

```text
[用户进入后台布局]
        |
        +-- 先请求 GET /resource/message/box
        |                 |
        |                 v
        |          读取当前登录用户编号
        |                 |
        |                 v
        |    查询全局消息或包含该用户的消息
        |                 |
        |                 v
        |    分成系统、通知、工作流三个列表
        |
        +-- 再建立 SSE 或 WebSocket 长连接
                          |
                          v
                    接收后来产生的新消息
```

这两条路互相补充：

- 历史查询让暂时离线的用户重新上线后还能看到近 30 天消息。
- 实时连接让在线用户不用刷新页面就能收到新消息。
- 后端只返回当前用户可见的消息；全局消息的目标用户字段是 `0`。
- 前端把三个列表合起来显示在右上角消息盒子，再按“系统、通知、工作”三个标签筛选。

### 第四步：已读状态在哪里

```text
[用户点击消息]
        |
        +-- 在当前浏览器保存消息编号 -> 显示“已读”
        |
        +-- 如果有跳转路径 -> 打开对应页面

不会发生：

[用户点击消息] - - 不会更新 - -> [sys_message]
```

`sys_message` 没有已读字段，也没有“用户与消息”的单独关系表。前端把最多 300 个已读消息编号按用户保存在浏览器本地缓存中。

这意味着同一个账号换一台电脑或清除浏览器缓存后，原来已读的消息可能再次显示为未读。它也说明当前实现更接近轻量消息盒子，而不是严格的企业统一收件箱。

### 第五步：公告原稿和消息快照会不会一直同步

不会。它们只在“新增公告”这一刻由 Controller 串起来，没有数据库外键，也没有后续同步代码：

```text
[新增公告] -> [写 sys_notice] -> [新建一条 sys_message 快照]

[修改公告] -> [只改 sys_notice]
                         [旧 sys_message 不变]

[删除公告] -> [只删 sys_notice]
                         [旧 sys_message 仍可能存在]
```

还有一个容易忽略的事实：新增接口没有先判断公告状态是否为“正常”。即使请求中的状态是“关闭”，只要新增成功，当前代码仍会建立消息记录并尝试广播。因此这里的“发布”实际上是“新增成功后立即发布”，不是独立、受状态控制的发布动作。

### 第六步：`notify` 与这套消息盒子是什么关系

两者都在“发送信息”，但解决的是不同问题：

```text
                    站内消息链路                         notify 链路

入口                MessageService                      NotifyClient
主要目标            后台网页中的登录用户                邮箱、手机号等外部目标
当前渠道            SSE 或 WebSocket                    mail、sms
用户查看位置        右上角消息盒子                      邮箱客户端、手机短信
主要记录表          sys_message                         sys_notify_log
                                                       sys_notify_delivery_log
记录的目的          给用户展示消息                      给管理员审计投递结果
已读状态            浏览器本地保存                      不负责
幂等和附件快照      不负责                              负责
```

`notify` 不只是一个简单的 `send()` 工具。它还统一了请求格式、渠道选择、目标校验、防重复发送、附件快照、发送结果和监控事件。但从产品视角看，它仍是“通知投递基础设施”，不是用户收件箱。

当前真实调用关系如下：

```text
[公告新增]
    |
    +-- 使用 MessageService -> sys_message + 站内实时推送
    |
    +-- 没有使用 NotifyClient -> 不会因此自动发邮件或短信

[验证码、工作流等业务]
    |
    +-- 可以使用 NotifyClient -> 邮件或短信
    |
    +-- 某些业务也可另外使用 MessageService -> 站内消息
```

因此，“同一件业务既发站内信，又发邮件或短信”需要业务代码明确调用两套入口；当前没有一个总编排器自动把两者绑在一起。

### 第七步：实时推送失败时，消息还在不在

```text
[messageService.publishAll]
        |
        +-- 先保存 sys_message
        |
        +-- 后执行实时推送
```

因为保存发生在实时推送之前，用户当时不在线，或者后端关闭实时推送时，已经保存的 `sys_message` 仍可在之后通过消息盒子接口查询。实时推送负责“马上提醒”，数据库记录负责“稍后还能找到”。

## 术语小词典

- 站内信：只在本系统内部登录后查看的消息，不依赖外部邮箱或手机号。
- 消息盒子：网页右上角集中展示消息的小窗口；它可以是站内信的一部分。
- 公告原稿（主数据）：公告当前正式内容的唯一保存位置，这里是 `sys_notice`。
- 消息快照：发布当时复制出来的一份消息内容；原稿后来变化，快照不一定跟着变化。
- 实时推送：网页不刷新也能马上收到后端消息，当前可使用 SSE 或 WebSocket。
- SSE：服务器沿一条长连接持续向网页发送事件，专业名是 Server-Sent Events。
- WebSocket：网页和服务器保持一条可双向传话的长连接。
- Redis 消息通道：多个后端实例之间转发实时消息的公共通道，专业名是发布订阅。
- 幂等：同一个业务请求重复到达时，尽量不要重复发送。
- 渠道：消息真正走出去的方式，例如站内推送、邮件或短信。
- 监控日志：记录通知交给哪个服务商、成功还是失败，供管理员排查；它不是用户收到的信。

## 你现在能复述什么

1. `sys_message` 是什么：它是轻量站内消息盒子的持久化记录，按系统、通知、工作流分组，同时支持离线查询和在线推送；但服务端没有已读、每人一条投递、回复或归档能力。
2. 它与 `sys_notice` 的关系：`sys_notice` 是公告原稿；新增公告成功后，Controller 显式调用 `MessageService`，从公告组装并保存一条 `sys_message` 快照，再实时广播。
3. 它与 `notify` 的关系：两者目前是并列能力。消息盒子面向站内用户，`notify` 面向邮件和短信等外部渠道；`sys_notify_log` 是投递审计，不是站内收件箱。
4. 公告怎样到达前端：`SysNoticeController` 调用 `MessageService.publishAll`，后者先写 `sys_message`，再经 `PushHelper`、Redis 和 SSE/WebSocket 到达在线网页；用户重新登录时也会请求 `/resource/message/box` 补取历史消息。
5. 当前实现的边界：只有新增公告会生成消息；修改和删除不会同步旧消息，新增时也没有按公告状态决定是否发布；已读状态只保存在当前浏览器。

## 源码导航

- 公告新增与消息发布入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysNoticeController.java</Path>`
- 公告原稿的保存逻辑：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysNoticeServiceImpl.java</Path>`
- 跨模块站内消息入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/MessageService.java</Path>`
- 消息存储、分类与查询：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMessageServiceImpl.java</Path>`
- 当前用户消息盒子接口：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysMessageController.java</Path>`
- 两张表的结构：`<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`
- 实时推送入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-push/src/main/java/org/dromara/common/push/helper/PushHelper.java</Path>`
- Redis 消息订阅与在线分发：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-push/src/main/java/org/dromara/common/push/listener/MessageTopicListener.java</Path>`
- 前端历史加载和实时接收：`<Path>plus-ui-namewta/apps/admin-web/src/utils/push.ts</Path>`
- 前端已读状态：`<Path>plus-ui-namewta/apps/admin-web/src/utils/message-read.ts</Path>`
- `notify` 统一入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/core/NotifyClient.java</Path>`
- `notify` 调度规则：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/src/main/java/org/dromara/common/notify/core/NotifyDispatcher.java</Path>`
- `notify` 监控落库：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/service/impl/SysNotifyMonitorServiceImpl.java</Path>`
