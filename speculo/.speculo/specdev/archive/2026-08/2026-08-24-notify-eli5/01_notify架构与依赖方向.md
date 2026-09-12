# notify 代码构造与依赖方向

先给直接答案：

1. `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>` 是渠道无关的发送内核，负责统一请求、校验、幂等、调度、结果和事件。
2. `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/</Path>` 和 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/</Path>` 是具体渠道插件。它们依赖内核并实现内核定义的接口，而不是由内核反向依赖它们。
3. `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/notify/</Path>` 不是另一个发送引擎。它负责只有 system 才知道的数据库监控、脱敏、通知附件快照和 OSS 引用生命周期。
4. 通用发送没有做成一个万能 Controller。每个业务入口先完成自己的权限和收件人判断，再在 JVM 内调用 `NotifyClient`；system 只提供监控管理 Controller。

## 先看全图

### 模块依赖图

```text
                         [业务调用方]
                  Captcha / Workflow / Demo
                              |
                              v
                    [common-notify 内核]
                 请求、校验、幂等、调度、事件
                       ^              ^
                       |              |
             实现渠道插座              实现附件/日志插座
                       |              |
              +--------+------+       +--------+
              |               |                |
              v               v                v
        [common-mail]    [common-sms]    [system/notify]
         SMTP 插件         SMS4J 插件      数据库、OSS、监控
```

箭头最容易看错的地方是：

```text
[common-mail] ----依赖----> [common-notify]
[common-sms ] ----依赖----> [common-notify]

不是：

[common-notify] ----依赖----> [mail + sms]
```

`common-notify` 像插座标准，Mail 和 SMS 像两种插头。插座只规定形状，不需要知道今天插的是邮件、短信，还是未来的微信、飞书。

### 运行时组装图

```text
[ruoyi-admin 可执行应用]
          |
          +-- 直接带入 common-mail
          |
          +-- 带入 ruoyi-system
                         |
                         +-- 带入 common-sms
                         +-- 带入 common-notify
                         +-- 提供 system/notify 实现
          |
          v
[Spring 容器发现全部 Bean]
          |
          +-- MailNotifyChannelAdapter
          +-- SmsNotifyChannelAdapter
          +-- SystemNotifyAttachmentSnapshotService
          +-- SystemNotifyLogIdGenerator
          +-- NotifyDeliveryEventListener
          |
          v
[NotifyAutoConfiguration 收集并装配 NotifyClient]
```

所以，编译时模块互不反向引用，运行时又能在同一个 Spring 容器里协作。

## 一步一步看

### 第一步：先拆开三个容易混淆的“通知”概念

| 名称 | 实际职责 | 不负责什么 |
| --- | --- | --- |
| `SysNotice` | 管理通知公告，保存公告内容并向在线用户广播摘要 | 不负责统一邮件、短信 Provider 调度 |
| `SysMessage` | 站内消息落库和 WebSocket/SSE 推送 | 本次没有并入 common-notify |
| `common-notify` | 调用邮件、短信等外部渠道并返回统一结果 | 不管理用户、公告、站内消息或 system 数据表 |
| `system/notify` | 保存外部通知投递监控和附件快照 | 不直接调用 SMTP 或 SMS4J |

如果把这四件事塞进同一个 `SysMessageServiceImpl`，名字看起来集中，实际会把公告、站内推送、邮件、短信、Redis、OSS 和监控数据库绑成一个大类。

### 第二步：common-notify 内部是怎样构造的

当前 `common-notify` 有 39 个 Java 文件，按职责拆成以下部分：

```text
[common-notify]
  |
  +-- [model]       请求、目标、正文、结果、状态
  +-- [core]        NotifyClient 与 NotifyDispatcher
  +-- [spi]         渠道、上下文扩展接口
  +-- [registry]    按 channel 查找 Adapter
  +-- [idempotency] Redis 防重复状态机
  +-- [attachment]  附件快照抽象接口
  +-- [event]       投递完成事件
  +-- [exception]   可区分的失败类型
  +-- [config]      Spring 自动装配
```

#### 1. model：统一语言

`NotifyRequest` 把一次通知固定为：

```text
[业务编号]
    + [一个渠道]
    + [多个物理目标]
    + [文本、富文本或模板内容]
    + [可选附件 ossId]
    + [审计策略]
    + [可选防重复 Key]
```

这里故意只接受手机号、邮箱等物理目标，不接受 `USER`。原因是 common 层不知道 `SysUser`，也不知道一个用户应该使用哪个手机号、邮箱或开放平台账号。这个决定必须由业务层完成。

#### 2. spi：只定义插座

`NotifyChannelAdapter` 只有三个核心问题：

```text
[你负责哪个 channel？]
[你支持哪些目标类型？]
[给你标准请求后，你返回什么标准结果？]
```

Mail Adapter 回答 `mail + EMAIL`，SMS Adapter 回答 `sms + PHONE`。以后增加飞书，只需新增实现 `NotifyChannelAdapter` 的模块，不必修改 `NotifyDispatcher` 的邮件或短信分支。

附件也使用同样的方法：common 只认识 `NotifyAttachmentSnapshotService`、`NotifyLogIdGenerator` 和可以物化到临时路径的资源，不认识 `SysOss`、Mapper 或 system 实体。

#### 3. registry：启动时登记插件

`NotifyChannelRegistry` 把所有 Adapter 按小写 channel 放入只读 Map：

```text
[Spring 找到的 Adapter 列表]
          |
          v
[检查 channel 非空且不重复]
          |
          v
mail -> MailNotifyChannelAdapter
sms  -> SmsNotifyChannelAdapter
```

两个插件声明同一个 channel 时直接启动失败，避免请求到来后才随机选择一个实现。

#### 4. dispatcher：统一发送流水线

`NotifyDispatcher.send` 的主流程是：

```text
[收到 NotifyRequest]
          |
          v
[校验 channel、目标、内容、附件编号]
          |
          v
[Registry 选择 Adapter]
          |
          v
[快照 userId、clientPk、traceId]
          |
          v
[可选 Redis 幂等占位]
          |
          v
[可选：发送前复制附件快照]
          |
          v
[同步调用 Mail 或 SMS Adapter]
          |
          v
[聚合 ACCEPTED / FAILED / PARTIAL_FAILURE]
          |
          +-- 成功时完成幂等状态
          |
          v
[发布 NotifyDeliveryEvent]
          |
          v
[全部目标未接受时抛出带完整结果的异常]
```

统一流水线的价值是，校验、防重复、附件快照、结果聚合和监控事件只实现一次。Mail 和 SMS Adapter 只处理各自 Provider 的细节。

#### 5. idempotency：为什么内核直接需要 Redis

防重复不是邮件独有，也不是短信独有。验证码、工作流通知或业务提醒都可能因为重试而重复发送，所以它属于统一调度流程。

```text
[同一 channel + 同一业务 Key]
          |
          +-- 第一次 -> Redis 写 IN_PROGRESS -> 调 Provider
          |
          +-- 正在执行 -> 报“发送中”
          |
          +-- 已完成且内容相同 -> 返回第一次结果
          |
          +-- Key 相同但内容不同 -> 报冲突
```

因此 `common-notify` 显式依赖 `common-redis`。有防重复 Key 时 Redis 不可用会失败关闭，避免“以为防重，实际重复发送”；没有 Key 的普通通知仍可以发送。

### 第三步：Mail 和 SMS 是怎样接入内核的

#### Mail

`common-mail` 的 POM 依赖 `common-notify`，然后提供：

```text
[MailConfig]
    |
    +-- mail.enabled=true -> MailAccount
    |
    +-- MailAccount 存在 -> MailNotificationSender
    |
    +-- Sender 存在 -> MailNotifyChannelAdapter
```

Mail Adapter 负责：

- 只接受 EMAIL 目标。
- 把 TO、CC、BCC 转换为邮件收件人。
- 把通知附件快照物化到独立临时目录。
- 调用原有 `MailBuilder` 和 SMTP。
- 把 Provider Message ID、耗时和失败转换为标准 `NotifyTargetResult`。
- 发送后清理本地临时文件。

#### SMS

`common-sms` 的 POM 同样依赖 `common-notify`，然后由 `SmsAutoConfiguration` 创建：

```text
[SMS4J 配置]
      |
      v
[SmsNotificationProviderResolver]
      |
      v
[SmsNotifyChannelAdapter]
```

SMS Adapter 负责：

- 只接受 PHONE 目标。
- 根据可选 `providerKey` 解析短信 Provider。
- 逐个手机号调用 Provider。
- 把每个目标的成功、失败、消息编号和耗时转换为统一结果。

这两个 Adapter 都不实现幂等、全局事件和监控落库，因为这些已经由 Dispatcher 统一处理。

### 第四步：system/notify 的 16 个类怎样分工

`system/notify` 使用“按功能聚在一起”的小型垂直目录，而不是把 16 个类分散到 system 顶层的通用 domain、mapper、service 和 listener 目录。

```text
[system/notify]
  |
  +-- [domain]
  |     +-- SysNotifyLog
  |     +-- SysNotifyDeliveryLog
  |
  +-- [mapper]
  |     +-- 两张表的 Mapper
  |
  +-- [bo]
  |     +-- SysNotifyQuery
  |
  +-- [vo]
  |     +-- 安全列表投影
  |     +-- 完整详情投影
  |
  +-- [service]
  |     +-- 记录、分页、详情、下载、删除、清空
  |
  +-- [listener]
  |     +-- 异步接收投递事件
  |
  +-- [attachment]
  |     +-- 复制 OSS 对象
  |     +-- 生成快照元数据
  |     +-- 预分配通知日志主键
  |
  +-- [support]
        +-- 错误文本清洗
        +-- 手机和邮箱脱敏
```

Controller 没有消失。它位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/monitor/SysNotifyController.java</Path>`，继续遵守项目的 HTTP Controller 目录习惯，并暴露 `/monitor/notify` 的列表、详情、附件下载、删除和清空。

MyBatis XML 也继续位于 system 的标准 resources mapper 目录。也就是说，功能内部聚合与项目框架约定同时保留了。

### 第五步：system/notify 在发送链路中做什么

#### 发送前：附件快照

```text
[Dispatcher 收到 attachmentOssIds]
          |
          v
[SystemNotifyLogIdGenerator 预分配日志主键]
          |
          v
[SystemNotifyAttachmentSnapshotService 查询源 SysOss]
          |
          v
[OSS 服务端复制为 notify/{日志主键}/...]
          |
          v
[登记新的 TEMP SysOss]
          |
          v
[Adapter 使用复制后的快照发送]
```

为什么要先生成日志主键？因为附件引用需要绑定真实的 `sys_notify_log` 主键。先复制再发送，才能保证 Provider 实际发送的附件和以后监控页面看到的归档附件是同一份。

如果复制失败，整次通知停止，不会悄悄变成“无附件发送”。如果后面的异步日志落库失败，快照仍是 TEMP，最终由生命周期清理。

#### 发送后：异步监控

```text
[Dispatcher 已拿到 Provider 同步结果]
          |
          v
[发布不可变 NotifyDeliveryEvent]
          |
          v
[@Async EventListener]
          |
          v
[SysNotifyMonitorServiceImpl]
          |
          +-- 写 sys_notify_log：一次逻辑通知
          +-- 写 sys_notify_delivery_log：每个物理目标结果
          +-- 建立通知日志到附件快照的 OSS 引用
```

事件中已经复制了 `userId`、`clientPk` 和 `traceId`，异步线程不再读取原请求线程的登录上下文。

监控是 best-effort：Provider 已经接受通知后，即使监控数据库暂时失败，也不能把“已经发送”伪装成“发送失败”。代价是应用在特殊故障窗口可能缺一条监控记录。

### 第六步：为什么不全部融入 system 的普通 Service 和 Controller

#### 原因一：发送不是只有 system 页面会调用

当前调用方至少包括 Captcha、Workflow 和 Demo。它们需要在 Controller、Service、后台流程中直接调用，而不是通过 system 的 HTTP Controller 再绕一圈。

```text
[Workflow Service] -> [NotifyClient]
[Captcha Controller] -> [NotifyClient]
[Demo Controller] -> [NotifyClient]
```

如果发送引擎属于 `ruoyi-system` 的内部 Service，其他业务模块就会被迫依赖 `ruoyi-system` 的实现类、Mapper 或领域对象，破坏现有“业务模块依赖 common 或 ruoyi-api，不依赖 system 实现”的边界。

#### 原因二：不存在安全清晰的万能发送 Controller

一个通用 `/notify/send` Controller 必须回答：谁能发、能发给谁、可以使用哪个模板、附件是否有权、失败后业务怎么处理。这些答案属于具体业务，不属于通知基础设施。

当前做法是：

```text
[业务入口先做权限和领域校验]
          |
          v
[业务把用户解析成真实邮箱或手机号]
          |
          v
[内部调用 NotifyClient]
```

这样避免出现一个可以向任意手机号、邮箱发送任意内容的高风险万能接口。

#### 原因三：发送和监控的失败语义不同

- Provider 发送是同步主流程，调用方必须立即知道成功、部分失败或失败。
- 监控落库是异步旁路，失败不能改变已经发生的 Provider 结果。

把两者写进一个带数据库事务的 `SysNotifyService.sendAndSave`，很容易让开发者误以为数据库回滚能撤回已经发出的短信或邮件。

#### 原因四：避免 system 反向污染 common

common 层不能引用 `SysUser`、`SysOss`、system Mapper 或通知日志表。于是 common 定义 SPI，system 实现 SPI：

```text
[common 定义能力接口] <- [system 提供业务实现]
```

依赖方向保持为业务模块指向公共契约，而不是公共模块反过来认识业务模块。

#### 原因五：feature package 提高局部可读性

从代码结构推断，`system/notify` 采用 feature package 的直接收益是：通知专属的 Entity、Mapper、Service、Listener、脱敏和附件代码可以一起阅读，不会散落在 system 已有的 44 个顶层 Service 文件及更多 domain/mapper 文件中。

它仍然属于 `ruoyi-system` Maven 模块，并不是新的微服务或部署单元。Spring 扫描、事务、MyBatis 和权限体系都没有被绕开。

### 第七步：为什么 common-notify 只依赖 core 和 redis

#### Redis 是真实的功能依赖

`RedisNotifyIdempotencyStore` 直接使用 Redisson 的 Bucket 和原子 compare-and-set，保存 `IN_PROGRESS` 与 `COMPLETED`。因此 `common-redis` 是明确的实现依赖。

#### Core 是基础依赖，但当前有一点冗余

POM 显式声明 `common-core`，表达它是 common 基础模块，并从该依赖图获得 Spring 基础能力。可是当前 `common-notify` Java 源码没有直接 import `org.dromara.common.core.*`，而 `common-redis` 自己也会传递带入 core。

所以准确说法是：

- Redis 是当前代码直接使用的依赖。
- Core 是显式基础依赖，但在当前 Maven 图中部分冗余。
- 这不影响依赖方向正确性；如果做极致依赖收敛，可以单独评估是否保留显式 core。

#### Mail 和 SMS 必须反向依赖 notify

如果 `common-notify` 依赖 Mail 和 SMS，同时 Mail/SMS 又要实现 `NotifyChannelAdapter` 并引用 `NotifyRequest`，依赖会变成：

```text
[notify] -> [mail] -> [notify]
```

这是 Maven 无法接受的循环依赖。

即使把 Adapter 搬回 notify 避免循环，也会带来三个问题：

- 任何使用通知内核的应用都会被迫携带 Jakarta Mail、SMTP 实现和 SMS4J。
- 增加微信、飞书等新渠道时必须修改通知内核。
- Mail/SMS 原有 Builder 和 Provider 配置边界被打散。

现在的方向让每个渠道自行选择加入：

```text
[只需要通知模型和 Dispatcher] -> 引入 common-notify
[需要邮件]                     -> 再引入 common-mail
[需要短信]                     -> 再引入 common-sms
```

### 第八步：POM 为什么看起来不完全对称

当前组装事实是：

| 模块 | 直接依赖 |
| --- | --- |
| `common-notify` | `common-core`、`common-redis` |
| `common-mail` | `common-core`、`common-notify`、邮件 SDK |
| `common-sms` | `common-notify`、`common-redis`、SMS4J |
| `ruoyi-system` | `common-notify`、`common-sms`、OSS、MyBatis 等 |
| `ruoyi-admin` | `common-mail`、`common-notify`、`ruoyi-system` 等 |

`ruoyi-system` 对 `common-sms` 的依赖在统一通知实现之前就已经存在；当前 system 源码没有直接 import common-sms 类型，它主要把 SMS 自动配置带入运行时。Mail 则由最终可执行应用 `ruoyi-admin` 直接带入。

这能工作，但装配位置确实不完全对称。更清晰的未来选择可以是：

```text
[ruoyi-admin 组装层]
    +-- common-notify
    +-- common-mail
    +-- common-sms
```

或者新增一个只负责组合的通知 starter。这样 system 只保留监控与附件 SPI 实现。不过这是进一步优化建议，不是当前代码运行所必需的修复。

### 第九步：当前方案与“全部放 system”对比

| 比较项 | 当前分层设计 | 全部放进 system |
| --- | --- | --- |
| 业务模块调用 | 依赖轻量 `NotifyClient` | 容易依赖 system 实现或再造 API |
| Mail/SMS 可选性 | Adapter 按模块加入 | system 常驻全部 Provider SDK |
| 新增渠道 | 新模块实现 SPI | 修改 system 中央发送逻辑 |
| 数据库监控 | system 负责，符合数据归属 | 可以实现，但会和发送事务混在一起 |
| 非 HTTP 调用 | Service、Job、Workflow 都能直接调用 | 若以 Controller 为中心会绕路 |
| 代码数量 | 模块和接口较多 | 初期文件少，但中央 Service 会持续膨胀 |
| 代价 | 理解依赖注入和事件需要学习成本 | 简单起步，长期耦合和测试成本更高 |

当前方案不是唯一正确答案，但它更符合这个仓库已有的多模块依赖规则，以及“common 不反向依赖 system”的明确架构决定。

### 第十步：这套设计仍有哪些边界

- Provider 是同步调用，业务线程会承担邮件或短信服务的延迟。
- Event 监控不是可靠队列，宕机窗口可能丢监控日志。
- Redis 幂等不是永久的 exactly-once，也没有自动重试或 Outbox。
- 一个请求只发一个 Channel，没有多渠道编排、用户偏好或故障切换。
- `common-notify` 自带 Redis 实现，若未来需要完全无 Redis 的极轻内核，可拆成 `notify-core` 与 `notify-redis`。
- Mail/SMS 的最终装配分散在 admin 和 system，未来可以收敛到专门的组合层。

这些是已接受的取舍，不应把它们误写成已经具备的可靠消息中心能力。

## 术语小词典

- 公共规则内核（core）：只保存所有渠道都要遵守的请求、校验和调度规则。
- 可替换插头（Adapter）：把某个具体 Provider 的接口转换成系统统一接口的代码。
- 扩展插座（SPI）：由内核定义、由外部模块实现的一组接口。
- 装配箱（Spring 容器）：启动时收集各模块对象并自动把它们连接起来的运行环境。
- 依赖箭头（Maven dependency）：一个模块编译和运行时需要另一个模块提供类型或能力。
- 循环依赖：A 需要 B，同时 B 又需要 A；Maven 模块无法按顺序构建。
- 物理目标：真正可以投递的邮箱、手机号或开放平台账号，不是抽象用户编号。
- 防重复状态（幂等）：同一业务请求重复到达时，不重复调用 Provider。
- 发送服务商（Provider）：真正完成 SMTP 邮件或短信发送的外部实现。
- 不可变通知单（Event）：发送完成后发布的一份结果快照，供旁路监控使用。
- 尽力记录（best-effort）：尽量保存监控，但监控失败不篡改已经发生的发送结果。
- 垂直功能目录（feature package）：把同一个功能的 Entity、Mapper、Service 和辅助代码放在一个上级目录内。
- 最终组装层：负责把多个可选模块放入同一个可执行应用的模块，本项目中主要是 `ruoyi-admin`。

## 你现在能复述什么

1. `common-notify` 是什么：它是统一通知的渠道无关内核，不是邮件、短信或 system 数据库实现。
2. `system/notify` 是什么：它是 system 内部的通知监控和附件快照子系统，通过 Event 与 SPI 接入内核。
3. 为什么不做万能 Controller：发送权限、收件人解析和业务失败策略属于具体业务；基础设施只接受业务已经确认的物理目标。
4. 为什么 notify 不依赖 mail/sms：内核定义 Adapter 接口，Mail/SMS 反向实现它，才能避免循环依赖并保持渠道可选。
5. 运行时怎样连起来：admin 带入 Mail，system 带入 SMS 和监控实现，Spring 收集全部 Adapter，再由 NotifyAutoConfiguration 创建统一 `NotifyClient`。
6. 哪些地方还可优化：Core 依赖部分冗余、Redis 可进一步拆插件、Mail/SMS 装配可以集中到专门的组装层。

事实依据：两个产品 POM、`NotifyAutoConfiguration`、`NotifyDispatcher`、Mail/SMS Adapter、`system/notify` 全部 16 个 Java 文件、监控 Controller、请求上下文装配，以及已归档的 ADR-006 至 ADR-009、Spec 和实现 Evidence。
