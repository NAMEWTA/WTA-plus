# notify 架构模式与模块合并

先给结论：

- 当前通知子系统最准确的描述是“模块化单体中的端口与适配器架构”。
- 它同时使用了依赖倒置、插件架构、Spring 自动装配和事件驱动旁路。
- 这些设计模式并不强制要求三个 Maven JAR。技术上完全可以把 Core、Mail、SMS 放进一个 `ruoyi-common-notify` JAR 的三个 Java 子包。
- 但是“可以”不等于“当前仓库值得这样改”。本项目需要持续跟随 upstream，Mail/SMS 又各自拥有独立基础能力，所以保留 artifact 边界更稳妥。
- 如果只是希望物理目录更整齐，推荐“一个通知父目录 + 多个子 artifact”，不要为了目录观感把它们强行编译成一个 JAR。

## 先看全图

### 当前架构由哪些模式组成

```text
[一个可部署的 ruoyi-admin 应用]
                |
                v
          [模块化单体]
                |
                +-- [NotifyClient 端口]
                |          ^
                |          |
                |   [Mail/SMS 适配器]
                |          |
                |          +-- 端口与适配器
                |
                +-- [依赖指向抽象接口]
                |          +-- 依赖倒置
                |
                +-- [Spring 收集渠道 Bean]
                |          +-- 插件架构
                |
                +-- [投递后发布监控事件]
                           +-- 事件驱动旁路
```

这不是完整系统都严格采用的六边形架构，也不是多个微服务。更准确地说，是通知子系统局部采用了端口与适配器思想，而整个后端仍是一个模块化单体。

### 三种“合并”不是一回事

```text
方案 A：只合并物理目录

[notify 父目录]
  +-- [notify-core JAR]
  +-- [notify-mail JAR]
  +-- [notify-sms JAR]

方案 B：合成一个 JAR

[common-notify JAR]
  +-- [core Java package]
  +-- [mail Java package]
  +-- [sms Java package]

方案 C：保持现在

[ruoyi-common-notify JAR]
[ruoyi-common-mail JAR]
[ruoyi-common-sms JAR]
```

方案 A 改的是文件摆放位置，方案 B 改的是编译、依赖和发布边界。两者影响完全不同。

## 一步一步看

### 第一步：主架构是模块化单体

当前后端最终打成一个 `ruoyi-admin` 可执行应用，所有模块运行在同一个 JVM 和 Spring 容器里：

```text
[ruoyi-admin]
      |
      +-- [ruoyi-system]
      +-- [ruoyi-workflow]
      +-- [ruoyi-common-notify]
      +-- [ruoyi-common-mail]
      +-- [ruoyi-common-sms]
```

它不是微服务，因为这些模块没有各自独立的进程、网络接口和部署生命周期。

它又不只是一个普通大单体，因为 Maven artifact 明确限制了依赖方向，模块有自己的职责和公开入口。这就是模块化单体。

#### 好处

- 运行简单：一次启动，不需要服务注册、远程调用或分布式事务。
- 边界比单个大工程清楚：Maven 能阻止部分错误依赖。
- 可以按部署组合选择模块。
- 将来某个领域真需要独立部署时，已有接口比从大类里拆分容易。

### 第二步：通知内核使用端口与适配器

端口与适配器也常被称为六边形架构。这里不必想象真的有六条边，核心思想只有一句：业务规则面向接口，外部技术实现接口。

```text
                   [NotifyClient]
                         |
                         v
                 [NotifyDispatcher]
                    核心发送规则
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
[Mail Adapter]   [SMS Adapter]   [附件快照 SPI]
     |                |                |
     v                v                v
   SMTP             SMS4J         system + OSS
```

`NotifyClient` 是调用入口端口，`NotifyChannelAdapter`、附件快照和上下文解析是输出端口。Mail、SMS、system/notify 是适配器。

#### 好处

- Dispatcher 不需要 import SMTP、SMS4J、SysOss 或 Mapper。
- 单元测试可以给 Dispatcher 传入假的 Adapter，不启动真实 Provider。
- 增加新渠道主要是新增实现，而不是继续扩展中央 `if/else`。
- Provider SDK 更新只影响对应适配器。
- 核心规则可以独立检查目标、幂等和结果聚合。

### 第三步：依赖倒置保证箭头方向

传统直接写法通常是：

```text
[NotifyService]
      |
      +-- new MailSender
      +-- new SmsSender
      +-- new SysNotifyMapper
```

此时高层发送流程直接依赖三个低层实现。

当前写法是：

```text
[Dispatcher] -> [NotifyChannelAdapter 接口]
                         ^
                         |
               [Mail/SMS 实现接口]
```

高层和低层都围绕抽象接口协作。这就是依赖倒置原则。

#### 好处

- 高层规则不会因某个短信厂商 SDK 改动而变化。
- Adapter 可以被替换、增加或在测试中模拟。
- common 不需要反向依赖 system。
- Maven 依赖图保持无环。

### 第四步：渠道发现属于插件架构

Spring 启动时寻找所有 `NotifyChannelAdapter` Bean，`NotifyChannelRegistry` 再按 channel 登记：

```text
[classpath 中存在 Mail 模块] -> [注册 mail]
[classpath 中存在 SMS 模块 ] -> [注册 sms]
[未来存在 Feishu 模块      ] -> [注册 feishu]
```

Dispatcher 只按 `request.channel()` 查插件，不硬编码插件数量。

#### 好处

- 渠道可以按 classpath 和配置启用。
- 内核无需知道所有插件。
- 重复 channel 会在启动时失败，而不是运行时随机选择。
- 不同部署理论上可以选择不同渠道组合。

### 第五步：监控使用事件驱动旁路

```text
[同步发送主路] -> [立即返回 Provider 结果]
       |
       +-- 发布 Event -> [异步监控落库]
```

它不是可靠消息队列，而是 Spring 内部事件。这样做是为了不让监控表故障篡改真实发送结果。

#### 好处

- 发送和监控的失败语义分离。
- system 可以监听 common 事件，common 不需要依赖 system。
- 请求线程先拿到 Provider 结果。

#### 代价

- 应用宕机窗口可能丢监控记录。
- 它不提供最终必达、自动重试或 Outbox 能力。

### 第六步：能否合成一个 common-notify JAR

能。Java package 也可以表达端口和适配器：

```text
ruoyi-common-notify/
  pom.xml
  src/main/java/org/dromara/common/notify/
    core/
    model/
    spi/
    channel/mail/
    channel/sms/
```

因为这些代码在同一个 Maven 编译单元中，`channel.mail` 和 `channel.sms` 可以直接实现 `spi.NotifyChannelAdapter`，不会出现 Maven 循环。

架构模式仍然可以保留：

- Dispatcher 仍只面向 Adapter 接口。
- Mail/SMS 仍位于适配器 package。
- Spring 仍可按 Bean 收集插件。
- system 仍通过 Event 和 SPI 接入。

所以，端口与适配器的本质是依赖方向，不是 JAR 数量。

### 第七步：合成一个 JAR 的好处

| 好处 | 实际影响 |
| --- | --- |
| POM 更少 | 删除两个子模块及 BOM 条目，依赖图更短 |
| 目录更集中 | 通知相关源码从一个根目录开始查找 |
| 组装更简单 | 引入一个 artifact 即获得 Core、Mail、SMS |
| 版本天然一致 | 三部分永远以同一个版本发布 |
| 适合固定产品 | 如果每个部署永远都要 Mail 和 SMS，可选模块价值较低 |

如果这是一个只服务单一应用、永远同时启用 Mail/SMS、也不需要把 MailBuilder 独立给其他模块使用的小型项目，一个 JAR 是合理方案。

### 第八步：合成一个 JAR 会失去什么

#### 1. 所有渠道 SDK 变成强制依赖

```text
[只想使用 NotifyRequest]
          |
          v
[同时得到 Jakarta Mail + SMTP 实现 + SMS4J + Redis]
```

即使某个部署完全不用短信，SMS4J 仍在 classpath；不用邮件也会携带邮件实现。

#### 2. package 只是约定，Maven module 是强边界

不同 Maven module 之间，错误依赖会直接编译失败。放进一个 JAR 后，Dispatcher 很容易在未来直接 import `MailBuilder` 或 SMS4J，架构只能依赖代码审查维持。

本仓库当前没有 ArchUnit 之类的自动包依赖门禁，因此合并后的边界会更软。

#### 3. Mail/SMS 不只是 Adapter

当前 Mail 模块还有 `MailBuilder`、账户配置和附件能力；SMS 模块还有 SMS4J Provider、缓存 DAO 和异常处理。它们是原子基础设施模块，不只是通知目录里的两个小类。

把它们全部迁进 notify，等于做出新的领域决定：以后 Mail/SMS 只作为统一通知的内部实现，不再是独立公共能力。

#### 4. 变化事故半径扩大

- 升级 SMS4J 需要重新发布整个 notify artifact。
- 邮件安全补丁和通知内核变化绑在同一发布单元。
- 某个渠道的自动配置错误可能影响所有只需要内核的消费者。

#### 5. 上游同步差异明显扩大

`common-mail` 和 `common-sms` 是 upstream 已有模块。当前 fork 主要是在它们内部增加 Adapter，并新增 `common-notify`。

如果移动或删除两个上游模块：

```text
[upstream 更新 common-mail/common-sms]
              |
              v
[本地路径已被删除或整体搬迁]
              |
              v
[每次同步需要人工映射和冲突处理]
```

对于持续跟随 upstream 的 fork，这是比少两个 POM 更重要的长期成本。

### 第九步：只合并物理父目录是否更合适

可以采用一个聚合父目录，但子模块仍是独立 artifact：

```text
ruoyi-common-notification/
  pom.xml                         packaging=pom
  ruoyi-common-notify-core/
  ruoyi-common-notify-mail/
  ruoyi-common-notify-sms/
  ruoyi-common-notify-starter/    可选：一次引入全部渠道
```

这样同时得到：

- 从一个目录查找通知代码。
- Core、Mail、SMS 仍有独立编译边界。
- 使用者可以只引入需要的渠道。
- starter 可以为“全都要”的应用提供一个依赖入口。

不过对当前 fork，这仍会移动 upstream 原路径。除非目录混乱已经造成持续维护问题，否则迁移收益可能不足以覆盖同步成本。

### 第十步：还可以只拆 Adapter，不搬原子模块

更纯粹、但模块更多的结构是：

```text
[common-mail]  原有 MailBuilder 和 SMTP 能力
       ^
       |
[notify-channel-mail] -> [notify-core]

[common-sms]   原有 SMS4J 能力
       ^
       |
[notify-channel-sms]  -> [notify-core]
```

这让 Mail/SMS 完全不知道 notify，由 Adapter 模块同时依赖两边。它是最标准的端口适配器分离，却会从 3 个 artifact 增加到至少 5 个。

这适合大型平台或多套产品组合，不适合仅仅为了“目录看起来统一”而采用。

### 第十一步：三种方案怎样选

| 判断条件 | 单一 JAR | 同父目录、多 artifact | 保持当前 |
| --- | ---: | ---: | ---: |
| 每个部署永远同时需要 Mail/SMS | 适合 | 适合 | 也可 |
| 需要渠道可选 | 不适合 | 适合 | 适合 |
| 重视 Maven 强边界 | 不适合 | 适合 | 适合 |
| 只想减少 POM 数量 | 最适合 | 不适合 | 不适合 |
| 只想提高目录可发现性 | 可以但过度 | 最适合 | 可通过文档解决 |
| 需要持续轻松合并 upstream | 风险最高 | 有路径迁移风险 | 最适合 |
| MailBuilder/SMS 能力要独立复用 | 不适合 | 视子模块设计 | 最适合 |

### 第十二步：针对当前仓库的推荐

当前不建议直接合成一个 JAR，原因按重要性排序：

1. 这是 upstream fork，保留上游 `common-mail`、`common-sms` 路径能显著降低以后同步成本。
2. Mail/SMS 不只是 Adapter，还有独立基础设施 API 和配置。
3. Maven artifact 正在替你强制执行依赖方向。
4. 当前只有 3 个相关 artifact，模块数量尚未形成明显维护负担。
5. 已归档 ADR 明确决定 Mail/SMS 保持原子模块；改变它需要新的架构决策和迁移验证。

如果未来出现以下事实，可以重新考虑单 JAR：

- 所有直接 MailBuilder/SMS 调用已经永久禁止并完成迁移。
- 所有部署都固定携带 Mail 和 SMS。
- upstream 跟随策略改变，或愿意承担路径级长期冲突。
- 团队认为 POM、BOM、自动装配维护成本已经高于模块隔离收益。
- 有 ArchUnit 或类似检查保护单 JAR 内的 package 依赖方向。

如果你现在只是想让开发者更容易找到代码，低成本方案是保留现状，并增加一份模块地图；本 change 的第 01 篇图解已经承担这个作用。

## 术语小词典

- 模块化单体：一个应用进程，但代码按有依赖边界的模块组织。
- 端口：核心向外公开或向外请求能力的接口，例如 `NotifyClient`、`NotifyChannelAdapter`。
- 适配器：把 SMTP、SMS4J、OSS 等具体技术转换为端口接口的实现。
- 六边形架构：端口与适配器架构的常见名称，不要求真的有六个模块。
- 依赖倒置：高层规则不直接依赖低层工具，两边围绕抽象接口协作。
- 插件架构：主程序在启动时发现并登记可选实现。
- 事件驱动旁路：主流程完成后发布事件，让监控等次要流程独立处理。
- Maven artifact：可以独立编译、依赖和发布的 JAR 或 POM 单元。
- Java package：一个 JAR 内的代码命名和可见性目录，边界通常比 Maven module 弱。
- 聚合父模块：`packaging=pom`、只负责列出子模块和统一版本的 Maven 工程。
- starter：为使用者一次性引入和自动配置一组能力的组合 artifact。
- classpath：应用启动时实际携带并可加载的全部类和第三方库。
- 事故半径：一个变化失败时可能影响的模块和功能范围。

## 你现在能复述什么

1. 当前是什么架构：模块化单体中的端口与适配器，配合依赖倒置、插件装配和事件监控。
2. 为什么这样设计：隔离 Provider SDK、避免循环依赖、让渠道可替换，并把发送结果与监控故障分开。
3. 能否合成一个 JAR：能，架构模式仍可保留，但 Maven 强边界和渠道可选性会减弱。
4. 同目录是否等于同模块：不等于；一个父目录可以包含多个独立 Maven artifact。
5. 当前推荐是什么：保留现状；如果只为目录整齐，可考虑聚合父目录，但 upstream 路径迁移成本仍需先证明值得。
6. 什么时候适合单 JAR：所有部署固定使用全部渠道、没有独立 Mail/SMS 消费者、接受统一发布和 SDK 常驻，并有包依赖门禁时。

事实依据：当前三个 common POM、MailBuilder 与 SMS4J 源码、NotifyClient/Adapter/AutoConfiguration、调用点扫描、工程依赖规则，以及已归档 ADR-006。
