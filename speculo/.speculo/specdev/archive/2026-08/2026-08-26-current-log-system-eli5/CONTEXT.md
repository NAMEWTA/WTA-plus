# 系统运行日志

**系统运行日志（System Runtime Log）**：由 SLF4J/Logback 写入服务器终端和文件、用于还原程序运行及 HTTP 调用过程的日志；本 change 的唯一运行日志文件是 `sys-console.log`。
_Avoid_: 操作审计日志、`sys_oper_log`、`@Log` 日志

**完整请求生命周期日志（Complete HTTP Exchange Log）**：共同记录请求进入时的方法、路径和输入，以及处理结束后的响应状态、响应 body 与耗时，并能识别为同一次 HTTP 调用。
_Avoid_: 仅有请求参数的访问日志、仅有异常堆栈的错误日志

**业务正文原值（Original Business Payload）**：普通业务请求参数和响应 JSON 中的值不进行字段级脱敏、替换或语义改写；正文中的密码、验证码、token 和 client secret 等认证凭证也按原值记录，这是当前 change 的明确安全例外。
_Avoid_: 在本 change 的实现中自行增加字段遮盖或凭证排除；把该例外扩展到其他日志体系

**单文件运行日志（Single Runtime Log File）**：INFO 及以上的系统运行事件统一同步写入当前 `sys-console.log`，历史文件按天 gzip 压缩，最多保留 60 天且归档总量最多 40GB，任一上限先达到即清理更旧归档。
_Avoid_: 同时维护 sys-console、sys-info、sys-error 三份重复文件；把 60 天误解为最低保留保证

**同步文件写入（Synchronous File Write）**：`sys-console.log` 由 `RollingFileAppender` 直接写入，不增加 `AsyncAppender` 或日志丢弃队列。
_Avoid_: 把旧 info/error 的异步 appender 迁移到唯一 sys-console 文件

**系统日志默认配置（System Log Defaults）**：HTTP 日志采集的稳定默认值由 `ruoyi-common-web` Java 配置拥有，所有环境默认开启且可通过外部属性显式关闭；文件持久化由 Logback XML 拥有，不要求在应用 YAML 声明常规默认项。
_Avoid_: 用普通 Spring Bean 接管 Logback 启动、在 application.yml 重复低变化默认值

**可记录正文（Loggable Payload）**：普通 JSON/文本请求与响应正文最多记录 1 MiB，超限时只截断日志副本并标明原始长度；文件、multipart 与流式响应只记录元数据。
_Avoid_: 把二进制文件或 SSE 流缓存后写入 sys-console

**Filter 所见正文（Filter-observed Payload）**：系统日志 Filter 位于现有 `CryptoFilter` 之后，以经过自身时实际可见的内容为准，因此请求是解密后正文，响应是外层响应加密前正文。
_Avoid_: Controller 返回值的语义重建、客户端线路密文字节还原

**HTTP 请求双记录（HTTP Exchange Pair）**：请求进入和响应完成分别输出一条单行 JSON 事件，二者共享同一个由服务器生成的 UUID requestId。
_Avoid_: 无关联的请求/响应文本、只在请求完成后才留下入口证据

**服务端 requestId（Server Request ID）**：服务器为每次请求生成新的权威 UUID，写入 MDC，并在 `X-Request-Id` 响应头回传；客户端同名请求头不得覆盖它，只能另记为 upstreamRequestId。
_Avoid_: 信任客户端提供的内部关联标识、请求结束后遗留 MDC

**单行结构化 HTTP 事件（Single-line Structured HTTP Event）**：每个 `HTTP_REQUEST` 和 `HTTP_RESPONSE` 都是可独立解析的一行 JSON；正文不改值，只做 JSON 所需的转义。
_Avoid_: 把正文换行直接写成新的物理日志行、在 JSON 前拼接不可解析文本

**业务优先日志降级（Business-preserving Log Degradation）**：运行时日志采集、编码或缓存失败不得改变原接口响应或异常语义，并应通过终端报告日志故障；非法配置则在应用启动阶段直接失败。
_Avoid_: 因日志失败改变状态码或正文、静默接受非法配置

**异步请求闭合（Async Exchange Completion）**：Servlet 异步请求只在真实完成时输出一次 `HTTP_RESPONSE`；普通 JSON/文本按相同正文规则记录，SSE/文件只记录元数据，并在所有完成和异常路径清理 MDC。
_Avoid_: 在初始 dispatch 返回时提前记录响应、重复 dispatch 产生多条响应事件、线程复用导致 requestId 串号

**首页运行时间响应（Index Runtime Time Response）**：根路径响应同时包含从 Spring `ApplicationContext` 启动时间固定下来的“启动时间”，以及每次请求即时计算的“当前时间”。
_Avoid_: 每次访问都重算启动时间、把类加载时间误当成应用启动时间

**启动运行摘要（Startup Runtime Summary）**：应用启动完成后通过 INFO logger 在控制台和 `sys-console.log` 的分隔线内输出访问地址、固定启动时间、运行系统、启动耗时和已用/最大堆内存，不输出独立的成功口号。
_Avoid_: 用 `System.out` 绕过文件 appender、在 Spring 启动完成前读取实际端口、重复输出装饰性提示语

**访问地址（Access Address）**：启动摘要根据 SSL、监听地址、实际 WebServer 端口和 servlet context path 组合的本机访问 URL；通配监听地址对操作者显示为 `localhost`。
_Avoid_: 把配置端口当成一定等于实际随机端口、把反向代理公网地址当成本地可推导信息
