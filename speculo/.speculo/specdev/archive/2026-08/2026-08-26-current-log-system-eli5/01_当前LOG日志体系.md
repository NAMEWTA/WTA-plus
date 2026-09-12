# 当前系统的 LOG 日志体系

## 先看全图

当前系统不是只有一种“日志”。最重要的是三条管道：

```text
                         [一次用户请求]
                                |
             +------------------+------------------+
             |                                     |
             v                                     v
   [方法上写了 @Log]                     [代码调用 log.info 等]
             |                                     |
             v                                     v
   [操作日志切面收集信息]                  [日志门面 SLF4J]
             |                                     |
             v                                     v
   [发布操作日志事件]                       [日志实现 Logback]
             |                                     |
             v                                     +---> [终端]
   [系统模块异步接收]                       |
             |                             +---> [logs/*.log]
             v
   [数据库 sys_oper_log]
             |
             v
   [后台“操作日志”页面]


   [登录、登出、注册]
             |
             v
   [发布登录日志事件]
             |
             v
   [系统模块异步接收]
             |
             +---> [数据库 sys_login_info]
             |
             +---> [log.info] ---> [Logback] ---> [logs/*.log]
```

一句话区分：

- `@Log` 是“谁在什么时候做了什么业务操作”的结构化审计记录，主要目的地是数据库。
- `log.info`、`log.warn`、`log.error` 是“程序运行时发生了什么”的诊断记录，主要目的地是终端和文本文件。
- 登录日志是一个交叉点：同一次登录事件既写数据库，也调用 `log.info` 写运行日志。

可以把它们想成两本不同的本子：

```text
[业务审计台账]                          [机器运行值班记录]
谁操作了哪个功能？                      程序启动了吗？
请求是什么？成功还是失败？              哪个组件报错了？
花了多久？                              异常堆栈是什么？
        |                                        |
        v                                        v
sys_oper_log / sys_login_info              logs/*.log
```

## 一步一步看

### 第一步：先分清 `ruoyi-common-log` 和 `logs/`

它们名字里都有 log，但职责完全不同：

| 对比项 | `ruoyi-common-log` 注解日志 | `logs/` 文件日志 |
| --- | --- | --- |
| 它是什么 | Java 公共模块 | Java 进程运行后产生的目录 |
| 入口 | 方法上的 `@Log(...)` | `log.info/warn/error(...)` 和框架自身日志 |
| 主要实现 | Spring AOP 切面、Spring 事件 | SLF4J + Logback |
| 数据形状 | 固定字段的结构化记录 | 一行行文本和异常堆栈 |
| 主要去向 | 数据库 `sys_oper_log` | `sys-console.log`、`sys-info.log`、`sys-error.log` |
| 查看方式 | 后台“监控 -> 操作日志” | 直接读文件，或读取 Actuator logfile |
| 保存周期 | 由数据库数据管理决定 | Logback 按天滚动和自动保留 |
| 是否自动覆盖所有代码 | 否，只有被切面命中的方法 | 是，只要日志级别允许且走 SLF4J/Logback |

最容易记住的区别是：

```text
@Log = 面向管理员追责和查询的业务记录

log.info/error = 面向开发、运维排错的程序记录
```

### 第二步：`@Log` 到底怎样工作

例如一个 Controller 方法写了：

```java
@Log(title = "用户管理", businessType = BusinessType.UPDATE)
```

调用流如下：

```text
[HTTP 请求到达带 @Log 的方法]
                |
                v
[LogAspect.doAround 开始计时]
                |
                v
[真正的业务方法执行]
         |成功             |抛出 Exception
         v                 v
   [拿到返回值]       [拿到错误消息]
         |                 |
         +--------+--------+
                  v
       [组装 OperLogEvent]
         - 模块标题、业务类型
         - 操作人、用户、部门、客户端
         - IP、URL、HTTP 方法、Java 方法
         - 请求参数、返回结果或错误消息
         - 成功/失败、耗时
                  |
                  v
       [Spring 发布应用内事件]
                  |
                  v
       [SysOperLogServiceImpl 异步接收]
                  |
                  v
       [补充 IP 所在地并用 MyBatis 插入]
                  |
                  v
            [sys_oper_log]
```

实现分成两边：

1. 公共模块负责“看见注解、收集信息、发事件”。

   - 注解定义：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/java/org/dromara/common/log/annotation/Log.java</Path>`
   - 切面实现：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/java/org/dromara/common/log/aspect/LogAspect.java</Path>`
   - 事件数据：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/java/org/dromara/common/log/event/OperLogEvent.java</Path>`
   - 自动装配登记：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>`

2. 系统业务模块负责“接事件、真正写数据库、提供查询接口”。

   - 异步监听与写入：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOperLogServiceImpl.java</Path>`
   - 数据库实体：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysOperLog.java</Path>`
   - MyBatis Mapper：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOperLogMapper.java</Path>`
   - 后台查询接口：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/monitor/SysOperlogController.java</Path>`
   - 建表 SQL：`<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 中的 `sys_oper_log`

这解释了为什么模块要拆开：

```text
[ruoyi-common-log]
只定义可复用的“采集和发信”能力
              |
              | OperLogEvent
              v
[ruoyi-system]
拥有系统日志表和后台管理功能
```

如果某个应用只引入 `ruoyi-common-log`，却没有装配 `ruoyi-system` 中的事件监听器，那么公共模块仍能发布事件，但不会凭空获得 `sys_oper_log` 的落库实现。

当前仓库中 `ruoyi-system`、`ruoyi-demo`、`ruoyi-gen` 和 `ruoyi-workflow` 都显式依赖了 `ruoyi-common-log`。本次静态搜索在后端源码和生成模板中找到了 131 处 `@Log(`，说明它是按重要业务动作选择性使用，不是每个请求的全量访问日志。

### 第三步：`@Log` 记录成功、失败和参数的规则

切面用“包住方法”的方式工作：

```text
[开始计时] -> [执行方法] -> [记录成功和返回值] -> [返回给调用者]
                   |
                   | Exception
                   v
              [记录失败和消息] -> [异常继续向外抛]
```

它不会吞掉业务异常。它先尝试形成失败审计记录，再把原异常继续交给系统异常处理器。

当前字段长度边界是：

- URL 最多 255 个字符。
- 客户端标识最多 32 个字符。
- 请求参数、响应结果和错误消息各最多 3800 个字符。
- 上传文件、Servlet 请求/响应对象、参数校验结果不会被序列化。
- 默认只按字段名排除 `password`、`oldPassword`、`newPassword`、`confirmPassword`。
- 单个接口可以用 `excludeParamNames` 再排除自己的敏感字段，或关闭请求/响应保存。

因此 `@Log` 有基础过滤，但不能理解所有业务秘密：

```text
[请求对象]
    |
    +-- password -----------------> [默认排除]
    +-- newPassword --------------> [默认排除]
    +-- idCard / token / secret --> [不会自动识别]
                                      |
                                      v
                         [接口必须显式排除或关闭保存]
```

还有三个实现边界：

- 数据库监听器带 `@Async`，所以业务响应和审计落库不是同一个同步步骤；审计记录可能稍晚出现，也不与业务事务构成原子提交。
- 切面捕获的是 `Exception`。正常 Java 业务异常会记录；`Error` 这类更严重的 JVM 错误不在这条失败记录分支内。
- 注解虽然声明也可放在参数上，但切点使用的是方法注解匹配；当前真正有效、仓库实际采用的是“放在方法上”。

如果采集或发布本身失败，`LogAspect` 会调用 `log.error("记录操作日志异常", exp)`。这时失败信息走的是另一条 Logback 文件日志管道，而不是再次写 `sys_oper_log`，从而避免无限递归。

### 第四步：`logs/` 是怎样产生的

运行日志的主链路是：

```text
[业务代码 @Slf4j]
        |
        | log.info / log.warn / log.error
        v
[SLF4J：统一的日志调用门面]
        |
        v
[Logback：决定级别、格式和去向]
        |
        +---> [终端 console]
        +---> [异步 INFO 文件]
        +---> [异步 ERROR 文件]
        +---> [INFO 及以上汇总文件]
```

主应用通过以下配置接通 Logback：

- Spring 日志入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>` 中的 `logging.config: classpath:logback-plus.xml`。
- 具体文件规则：`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>`。
- `org.dromara` 在 local/dev Maven profile 下是 INFO，prod profile 下是 WARN；Spring 自身是 WARN，MyBatis mapper 是 ERROR。
- 根记录器是 INFO，因此未单独调低级别的 DEBUG/TRACE 不会进入这些文件。

当前三个文件的分流不是互斥的：

```text
一条 INFO 事件
    +---> sys-console.log
    +---> sys-info.log

一条 WARN 事件
    +---> sys-console.log

一条 ERROR 事件
    +---> sys-console.log
    +---> sys-error.log
```

| 文件 | 实际接收范围 | 滚动与保留 |
| --- | --- | --- |
| `sys-console.log` | INFO、WARN、ERROR，即 INFO 及以上 | 每天滚动，历史保留 1 天 |
| `sys-info.log` | 只接收 INFO | 每天滚动压缩，历史保留 60 天 |
| `sys-error.log` | 只接收 ERROR | 每天滚动压缩，历史保留 60 天 |

`sys-console.log` 这个名字容易误导。它是一个文件 appender，并不是“只有屏幕上才有”的日志；同时 Logback 另有真正输出到终端的 `console` appender。

INFO 和 ERROR 文件前面各有一个异步队列，队列大小 512，`discardingThreshold=0`。这让业务线程通常不必等待磁盘逐行写完。`sys-console.log` 则直接由滚动文件 appender 接收。

### 第五步：为什么文件落在聚合仓库根的 `logs/`

Logback 写的是相对路径：

```text
log.path = ./logs
               |
               v
[Java 进程的当前工作目录] + /logs
```

它没有硬编码为某台机器上的绝对目录。现有 `sys-console.log` 的启动行明确显示：本次 `DromaraApplication` 是从聚合仓库根启动的，所以 `./logs` 被解析为用户指出的 `<Path>logs/</Path>`。

换一个启动目录，落盘位置也会跟着变：

```text
从聚合仓库根启动
    -> <Path>logs/</Path>

从后端子模块根启动
    -> <Path>ruoyi-vue-plus-namewta/logs/</Path>
```

这也是“我配置明明没变，日志怎么换地方了”的常见原因。

主应用还把 Actuator 的 logfile 指向 `./logs/sys-console.log`，配置也在 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application.yml</Path>`。这只是让监控端点知道要读哪个文件，不是文件的写入实现。

部署脚本 `<Path>ruoyi-vue-plus-namewta/script/bin/ry.sh</Path>` 虽然声明了一个 `LOG_PATH` 变量，但当前启动命令把标准输出和错误输出重定向到了 `/dev/null`，并没有使用该变量。用户指出的三个 `sys-*.log` 是 Logback 创建的，不是这个 Shell 变量创建的。

扩展应用有各自的 `logback-plus.xml`：Monitor Admin、SnailJob Server、SnailAI Server 会使用自己的文件名或子目录。本图重点解释当前 `<Path>logs/</Path>` 中由 `ruoyi-admin` 产生的 `sys-*.log`。

### 第六步：登录日志为什么会同时出现于两边

登录日志不是由 `@Log` 自动生成。登录、登出或注册代码主动发布 `LoginInfoEvent`：

```text
[登录 / 登出 / 注册]
          |
          v
[SysLoginService 或 SysRegisterService 发布 LoginInfoEvent]
          |
          v
[SysLoginInfoServiceImpl 异步接收]
          |
          +---> log.info(...) ---> Logback 文件
          |
          +---> MyBatis insert ---> sys_login_info
```

对应实现是：

- 登录事件类型：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/java/org/dromara/common/log/event/LoginInfoEvent.java</Path>`
- 登录与登出发布入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>`
- 注册发布入口：`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysRegisterService.java</Path>`
- 异步监听、打印和落库：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysLoginInfoServiceImpl.java</Path>`
- 数据表：`<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 中的 `sys_login_info`

所以“数据库日志”和“文件日志”不是永远互斥。它们是两条独立管道，同一业务事件可以主动同时送往两边。

### 第七步：两种日志各自适合回答什么问题

```text
问题：“张三昨天下午删除了什么？”
                    |
                    v
          [查 sys_oper_log]

问题：“为什么删除接口返回 500？”
                    |
                    v
          [查 sys-error.log 和调用上下文]

问题：“某次请求的完整异常堆栈在哪里？”
                    |
                    v
          [查文件日志]

问题：“这次操作属于新增、修改还是删除？”
                    |
                    v
          [查 @Log 写入的 business_type]
```

审计日志便于按用户、部门、客户端、业务类型、状态和时间筛选；文件日志保留线程、记录器名称、文字消息和异常堆栈，更适合排障。数据库审计不会自动包含所有底层框架消息，文件日志也不会自动变成可可靠查询的业务台账。

### 第八步：当前必须注意的安全边界

两条管道都不能被当成“天然安全的保险箱”：

```text
[敏感数据进入消息或对象]
            |
            +---> @Log 只排除已知字段名
            |
            +---> log.error 原样格式化消息
                         |
                         v
                [数据库或日志文件泄露风险]
```

本次只读检查已经在现有 `sys-error.log` 中看到认证异常输出过完整令牌。根因路径是认证异常处理器把 `NotLoginException.getMessage()` 直接以 ERROR 级别写入文件：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/handler/SaTokenExceptionHandler.java</Path>`。本图不复制该令牌。

这说明：

- Logback 负责分流、格式和保存，不负责理解并自动遮住令牌、证件号或业务秘密。
- `@Log` 的默认密码字段排除也不是通用脱敏器。
- 新接口必须主动决定哪些请求/响应字段允许进入审计库；普通日志调用也必须避免直接记录 Token、密码和完整敏感对象。
- `logs/*.log` 已被仓库的 `*.log` 规则忽略，不会正常进入 Git，但这不等于本机文件没有泄露风险。

## 术语小词典

- 业务审计台账（操作日志）：记录“谁做了什么、结果怎样”的结构化数据，专业名字是 audit log。
- 程序值班记录（运行日志）：记录程序启动、警告和异常的文本，专业名字是 application log。
- 方法上的标签（注解）：贴在 Java 方法上、让框架知道要额外做什么的标记，本例是 `@Log`。
- 包住方法的检查员（切面）：在原方法执行前后插入统一逻辑，专业名字是 AOP Aspect。
- 程序内通知单（事件）：发布者只发一份消息，由监听者决定怎样处理，本例是 `OperLogEvent` 和 `LoginInfoEvent`。
- 后台慢一步处理（异步）：把写日志工作交给另一个线程，调用者不必同步等它完成，专业名字是 async execution。
- 日志统一插座（SLF4J）：业务代码面向它调用日志，不直接依赖某个具体写入器，专业名字是 logging facade。
- 真正的日志写入器（Logback）：按级别把日志送到终端或文件，专业名字是 logging implementation。
- 文件分流出口（Appender）：Logback 中决定一条日志写去哪里的部件。
- 每天换一本文件（滚动）：按日期创建历史文件并按天数清理，专业名字是 rolling policy。
- 数据库对象搬运员（MyBatis Mapper）：把 Java 对象插入或查询数据库表的数据访问组件。
- 启动所在文件夹（工作目录）：Java 进程解析 `./logs` 时使用的基准目录，专业名字是 current working directory。

## 你现在能复述什么

读完后，应当能说清三件事：

1. `ruoyi-common-log` 的 `@Log` 由 AOP 切面收集结构化操作信息，发布事件，再由 `ruoyi-system` 异步写入 `sys_oper_log`；它不是 Logback 文件配置。
2. `<Path>logs/</Path>` 下的 `sys-console.log`、`sys-info.log`、`sys-error.log` 由 `ruoyi-admin` 的 `logback-plus.xml` 创建，接收业务和框架通过 SLF4J 发出的运行日志。
3. 两条管道用途、格式和保存方式不同，但可以相交；登录事件会同时写 `sys_login_info` 和文件，而 `@Log` 自身失败也会通过 `log.error` 进入文件。

最后用一张最短的判断图收尾：

```text
想知道“谁做了什么”吗？
          |
          +-- 是 --> [查操作/登录数据库表]
          |
          +-- 否
              |
              v
想知道“程序为什么这样运行或报错”吗？
          |
          +-- 是 --> [查 logs/*.log]
```
