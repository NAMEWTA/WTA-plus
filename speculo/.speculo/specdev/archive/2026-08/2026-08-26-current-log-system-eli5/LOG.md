# 系统运行日志优化设计日志

## LOG-001 — 2026-08-26T11:13:37+0800 — 操作审计日志不在范围内
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 本次是否修改 `@Log` 注解及其数据库操作审计链路。
- **事实与来源：** 用户明确要求当前先不修改 `@Log`；`USER-DECISION:2026-08-26`。
- **选项：** 同时改操作审计；仅改系统运行日志。
- **推荐：** 仅改系统运行日志，避免把数据库审计和文件排障日志混成一个合同。
- **结论：** `@Log`、`LogAspect`、`OperLogEvent` 与 `sys_oper_log` 全部 OUT。
- **原因：** 当前目标是优化服务器文件日志的持久化和 HTTP 调用内容。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 不改变现有操作日志的采集、异步事件和落库行为。
- **后续：** 下游 Spec 明确列入非目标。
- **替代/被替代：** 无

## LOG-002 — 2026-08-26T11:13:37+0800 — 完整 HTTP 请求生命周期
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 系统运行日志需要覆盖一次 HTTP 调用的哪些阶段。
- **事实与来源：** 用户要求同时看到请求进入时的参数和处理完成后返回前端的响应 body；现有拦截器只记录请求参数与耗时，不记录响应；`USER-DECISION:2026-08-26`，`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/interceptor/PlusWebInvokeTimeInterceptor.java</Path>`。
- **选项：** 只记录请求；只记录异常；记录请求进入与响应完成的全链路。
- **推荐：** 请求与响应都记录，并使用同一关联标识。
- **结论：** 系统运行日志必须覆盖方法、路径、输入参数/正文、响应状态、响应正文和耗时。
- **原因：** 只有输入与输出可以关联时，文件日志才足以还原一次普通 API 调用。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 日志采集不得改变发送给客户端的响应内容。
- **后续：** 继续决定捕获范围、准确性和关联形态。
- **替代/被替代：** 无

## LOG-003 — 2026-08-26T11:13:37+0800 — 业务正文不做字段级改写
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 普通业务输入输出是否进行字段级脱敏。
- **事实与来源：** 用户明确认为服务器系统日志用于排查，要求业务请求输入与响应输出原样记录；`USER-DECISION:2026-08-26`。
- **选项：** 全量脱敏；按业务字段白名单；普通业务正文原值记录并另行决定访问凭证例外。
- **推荐：** 固化业务正文原值目标，但把能直接取得访问权的凭证作为独立安全节点裁决。
- **结论：** 普通业务 JSON 字段不做脱敏、替换或值改写。
- **原因：** 用户希望排障时看到真实输入和输出语义。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 该结论尚不回答 Authorization、Cookie、密码、验证码和 Token 等认证凭证能否进入日志。
- **后续：** D-006 必须关闭后才能形成 Ready Spec。
- **替代/被替代：** 无

## LOG-004 — 2026-08-26T11:13:37+0800 — 单一 sys-console 持久化
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 文件日志数量、滚动压缩和保留时间。
- **事实与来源：** 用户要求基于前序讨论继续设计；前序已明确只保留 `sys-console.log`、每天 gzip 滚动、保留 60 天；`USER-DECISION:2026-08-26`。
- **选项：** 保留 console/info/error 三份；只保留 sys-console；按业务拆更多文件。
- **推荐：** 只保留 sys-console，避免 INFO 和 ERROR 重复占盘。
- **结论：** 当前写入文件为 `sys-console.log`；历史按天 gzip 压缩并保留 60 天，不再生成新的 `sys-info.log` 和 `sys-error.log`。
- **原因：** `sys-console` 已覆盖 INFO 及以上，其他两个文件是重复子集。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 文件日志仍可保留终端 appender；历史命名和容量上限待下游决定。
- **后续：** 持久化由 Logback 启动配置实现。
- **替代/被替代：** 无

## LOG-005 — 2026-08-26T11:22:40+0800 — Java 默认配置与 Logback 持久化分层
- **设计树节点：** D-005
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 系统日志配置是否需要写入 `application.yml`，以及 Java 配置和 Logback 各自拥有什么。
- **事实与来源：** Logback 在 Spring ApplicationContext 前初始化，普通 `@Configuration` 不能可靠拥有文件 appender 的启动配置；用户要求这种低变化系统配置尽量不写入 `application.yml`；`USER-DECISION:2026-08-26`。
- **选项：** 全写 YAML；全由 Spring Java 配置接管；Java 写 HTTP 采集默认值、Logback XML 写持久化，外部属性只用于显式覆盖。
- **推荐：** 按启动生命周期分层，默认值写代码且不污染应用 YAML。
- **结论：** HTTP 日志默认行为由 `ruoyi-common-web` Java 配置拥有；文件路径、滚动、gzip 和 60 天保留由 `logback-plus.xml` 拥有；默认开启可通过外部 `sys.log.enabled=false` 显式关闭，无需在 `application.yml` 声明。
- **原因：** 既符合初始化时序，也满足低变化配置集中在代码/XML 的要求。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 不创建一套与 Logback 重复且启动时不生效的 Spring 文件持久化配置。
- **后续：** Ticket 决定具体配置类命名和条件装配测试。
- **替代/被替代：** 无

## LOG-006 — 2026-08-26T11:22:40+0800 — 原样记录认证凭证的当前意图
- **设计树节点：** D-006
- **轮次与依赖：** round 1 / D-003
- **状态：** confirmed
- **问题：** 正文中的密码、验证码、token 和 client secret 是否也按原值记录。
- **事实与来源：** 用户基于服务器可信、密码可能已加密、动态凭证会刷新、部分前端 Client 信息公开的假设，当前要求所有正常请求输入输出保持原样；现有日志已出现过完整认证 token；`USER-DECISION:2026-08-26`，`CODE:<Path>logs/sys-error.log</Path>`。
- **选项：** 所有凭证原样；凭证不采集；凭证部分遮盖。
- **推荐：** 在进入下游合同前，用实际重放和 60 天留存事实再次确认是否建立安全规范例外。
- **结论：** 当前用户意图是凭证也原样记录，但尚未形成可执行安全例外。
- **原因：** 用户的回答包含“应该不是问题吧”的事实询问，而凭证刷新和服务器访问控制并不能消除日志副本风险。
- **影响工件：** LOG / ADR / Spec
- **约束或不变量：** D-014 未回答前阻止 Spec Ready。
- **后续：** 明确告知风险事实并取得最终选择。
- **替代/被替代：** 无

## LOG-007 — 2026-08-26T11:22:40+0800 — 文件与流式正文排除
- **设计树节点：** D-007
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 完整响应是否包含文件和流式输出的原始字节。
- **事实与来源：** 仓库存在 Excel、ZIP、代码生成、OSS 和直接写 `HttpServletResponse` 的接口；用户接受这些路径只记元数据；`USER-DECISION:2026-08-26`。
- **选项：** 所有字节全局缓存；仅普通 JSON/文本正文；所有响应都不记正文。
- **推荐：** JSON/文本记录正文，文件、multipart 和流式输出只记元数据。
- **结论：** 非普通 JSON/文本响应只记录路径、参数、Content-Type、Content-Length、状态和耗时，不记录原始字节。
- **原因：** 避免二进制日志膨胀、响应缓存占用和破坏 SSE/流式语义。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 下载、上传和流式功能不得因日志采集改变行为。
- **后续：** Spec 明确媒体类型与排除验收矩阵。
- **替代/被替代：** 无

## LOG-008 — 2026-08-26T11:22:40+0800 — 1 MiB 正文上限
- **设计树节点：** D-008
- **轮次与依赖：** round 1 / D-002, D-003
- **状态：** confirmed
- **问题：** 单次请求或响应正文的容量边界。
- **事实与来源：** 用户选择默认 1 MiB；`USER-DECISION:2026-08-26`。
- **选项：** 无限；固定 1 MiB；通过应用 YAML 频繁配置。
- **推荐：** Java 默认固定 1 MiB，超限显式截断并记录原始长度。
- **结论：** 单方向正文最多记录 1 MiB；超限日志携带 `truncated=true` 与原始字节长度。
- **原因：** 在排障完整性与 JVM、磁盘和留存事故半径之间建立确定边界。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 截断不得造成请求或响应本身被截断。
- **后续：** 测试 UTF-8 多字节边界和超限行为。
- **替代/被替代：** 无

## LOG-009 — 2026-08-26T11:22:40+0800 — 请求与响应双记录
- **设计树节点：** D-009
- **轮次与依赖：** round 1 / D-002
- **状态：** confirmed
- **问题：** 一次 HTTP 调用采用一条合并日志还是请求/响应两条日志。
- **事实与来源：** 用户选择两条记录和同一个 requestId；`USER-DECISION:2026-08-26`。
- **选项：** 请求结束后单条合并；请求与响应两条关联记录；无关联的多条文本。
- **推荐：** 两条结构化记录，共用服务端 requestId。
- **结论：** 请求进入输出 `HTTP_REQUEST`，响应完成输出 `HTTP_RESPONSE`，二者使用同一个 requestId。
- **原因：** 即使请求未完成也保留入口证据，中间日志可参与同一链路检索。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** requestId 在请求完成后必须从 MDC 清理。
- **后续：** D-013 决定生成、信任和响应回传语义。
- **替代/被替代：** 无

## LOG-010 — 2026-08-26T11:22:40+0800 — 所有环境默认启用
- **设计树节点：** D-010
- **轮次与依赖：** round 1 / D-002, D-004
- **状态：** confirmed
- **问题：** 完整 HTTP 日志在哪些环境默认启用。
- **事实与来源：** 现有拦截器无启用开关且始终注册，但 local/dev 的 `org.dromara=INFO` 会输出，prod 的 `org.dromara=WARN` 会抑制其 INFO 日志；用户要求目标状态所有环境默认开启并可显式关闭；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/config/ResourcesConfig.java</Path>`，`CODE:<Path>ruoyi-vue-plus-namewta/pom.xml</Path>`，`USER-DECISION:2026-08-26`。
- **选项：** 仅开发启用；所有环境启用；默认关闭按需开启。
- **推荐：** Java 条件装配 `matchIfMissing=true`，所有环境默认启用，外部属性显式关闭。
- **结论：** 目标状态所有环境默认输出 HTTP 请求/响应 INFO 日志，并提供不依赖 `application.yml` 默认项的显式关闭能力。
- **原因：** 符合系统级排障日志的常开预期，同时保留事故处置开关。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** prod 必须为 HTTP 采集 logger 保持 INFO，不能被 `org.dromara=WARN` 静默关闭。
- **后续：** 下游验证 local/dev/prod 的有效 logger level。
- **替代/被替代：** 无

## LOG-011 — 2026-08-26T11:35:20+0800 — 以系统日志 Filter 所见内容为捕获边界
- **设计树节点：** D-011
- **轮次与依赖：** round 2 / D-007, D-008
- **状态：** confirmed
- **问题：** 日志应记录语义对象、Filter 所见正文，还是客户端线路上的加密字节。
- **事实与来源：** 现有 `CryptoFilter` 以最高优先级注册，在下游 Filter 前解密请求，并在下游链完成后加密响应；用户要求“这个 Filter 经过的时候捕捉到是什么就是什么”；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-encrypt/src/main/java/org/dromara/common/encrypt/filter/CryptoFilter.java</Path>`，`USER-DECISION:2026-08-26`。
- **选项：** 记录 Controller 语义对象；记录日志 Filter 实际观察到的正文；还原客户端线路上的密文字节。
- **推荐：** 使用位于 `CryptoFilter` 之后的统一日志 Filter，以它当时可见的内容为合同。
- **结论：** 请求日志记录解密后交给应用的正文；响应日志记录应用写出且尚未被外层 `CryptoFilter` 加密的正文。除文件、multipart、流式路径外，不再进行语义重建或线路密文还原。
- **原因：** 这是用户定义的“原本是什么样子”边界，也能用一个 Filter 覆盖请求和响应生命周期。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 日志包装器不得改变下游读取的请求或最终发送的响应；排除范围仍遵守 D-007。
- **后续：** 验收加密请求、加密响应与普通 JSON 请求的捕获阶段。
- **替代/被替代：** D-011 原先的 ResponseBodyAdvice 推荐被本结论替代。

## LOG-012 — 2026-08-26T11:35:20+0800 — 服务端生成并回传 requestId
- **设计树节点：** D-013
- **轮次与依赖：** round 2 / D-009
- **状态：** confirmed
- **问题：** requestId 的来源、信任边界、传播和清理规则。
- **事实与来源：** 用户完整接受推荐方案；`USER-DECISION:2026-08-26`。
- **选项：** 信任客户端值；缺失时才生成；服务器始终生成权威 UUID。
- **推荐：** 服务器始终生成 UUID，MDC 传播并通过响应头回传。
- **结论：** 每个请求由服务器生成新的 UUID requestId，写入 MDC，使请求、响应和中间业务日志可关联，并通过 `X-Request-Id` 响应头返回。客户端传入的 `X-Request-Id` 不覆盖服务端值，若记录则使用独立 `upstreamRequestId` 字段。
- **原因：** 防止客户端伪造或碰撞内部关联标识，同时让前端可以携带服务端标识报障。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 在同步、异常和异步完成路径上都必须清理 MDC，避免线程复用串号。
- **后续：** D-012 纳入传播和清理测试。
- **替代/被替代：** 无

## LOG-013 — 2026-08-26T11:35:20+0800 — 凭证原值记录作为变更局部安全例外
- **设计树节点：** D-014
- **轮次与依赖：** round 2 / D-006
- **状态：** confirmed
- **问题：** 已知解密阶段、重放窗口和 60 天日志副本风险后，认证凭证是否仍原样进入文件日志。
- **事实与来源：** 用户明确要求 Filter 捕获到什么就记录什么，完整接受凭证原值记录，当前不处理相关风险；`USER-DECISION:2026-08-26`。
- **选项：** 排除凭证；遮盖凭证；凭证原值记录并建立明确安全例外。
- **推荐：** 排除认证凭证；若坚持原值，则必须将偏离安全规范写成 change-local ADR，而不能默默实现。
- **结论：** 密码、验证码、access/refresh token、client secret 及正文中其他认证凭证按日志 Filter 捕获到的原值写入 `sys-console.log`，不脱敏、不排除。本结论是当前 change 对 `SEC-003`/`SEC-005` 的显式局部例外。
- **原因：** 用户把服务器安全作为前提，优先选择完整排障内容，并明确接受已说明的风险。
- **影响工件：** ADR / CONTEXT / Spec / Ticket
- **约束或不变量：** 例外不扩展到其他日志体系或其他 change；1 MiB 上限和文件/流式排除仍然适用。
- **后续：** 下游规格必须直述该例外，不得由实现者自行恢复脱敏。
- **替代/被替代：** LOG-006 中“尚未形成可执行安全例外”的状态被本结论关闭。

## LOG-014 — 2026-08-26T11:35:20+0800 — 单行结构化 JSON 事件
- **设计树节点：** D-015
- **轮次与依赖：** round 2 / D-003, D-009
- **状态：** confirmed
- **问题：** 请求与响应日志使用结构化单行还是原始多行拼接。
- **事实与来源：** 用户选择单行结构化 JSON；`USER-DECISION:2026-08-26`。
- **选项：** 多行原文；单行键值文本；单行结构化 JSON。
- **推荐：** 单行结构化 JSON，仅做 JSON 格式必需的转义。
- **结论：** `HTTP_REQUEST` 与 `HTTP_RESPONSE` 各输出一条合法的单行 JSON 事件；正文的业务值不改写，换行、引号与控制字符仅按 JSON 规则转义。
- **原因：** 保持一条物理日志对应一个事件，可被日志采集器稳定解析，且不改变还原后的正文值。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** Logback pattern 不得在 JSON 事件正文中再插入不可解析前缀；异常堆栈等普通系统日志仍可使用既有格式，具体承载方式由下游规格明确。
- **后续：** 验证包含换行、引号和 Unicode 的正文可无损解析。
- **替代/被替代：** 无

## LOG-015 — 2026-08-26T11:43:12+0800 — sys-console 延续同步写入
- **设计树节点：** D-016
- **轮次与依赖：** round 3 / D-004, D-008, D-010, D-015
- **状态：** confirmed
- **问题：** 唯一文件 appender 使用同步写入还是异步队列。
- **事实与来源：** 当前 `file_console` 是直接挂载的 `RollingFileAppender`，旧 `async_info` 和 `async_error` 只分别包装重复的 info/error 文件；用户选择延续当前同步设计；`CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>`，`USER-DECISION:2026-08-26`。
- **选项：** 直接同步 RollingFileAppender；增加 AsyncAppender。
- **推荐：** 同步写入，优先减少排障证据在队列溢出或进程异常退出时丢失的可能。
- **结论：** `sys-console.log` 继续由同步 `RollingFileAppender` 直接写入，不新增异步包装器；删除旧 info/error 文件时，其异步 appender 也不迁移到 sys-console。
- **原因：** 保持现有可靠性语义和最小配置变化，符合用户对完整请求记录的优先级。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 同步磁盘写入耗时属于请求总耗时的一部分；不得用丢弃策略换取吞吐。
- **后续：** D-012 验证 appender 拓扑与业务响应降级行为。
- **替代/被替代：** 无

## LOG-016 — 2026-08-26T11:43:12+0800 — 60 天与 40GB 双上限
- **设计树节点：** D-017
- **轮次与依赖：** round 3 / D-004, D-008, D-010
- **状态：** confirmed
- **问题：** 时间保留目标与服务器磁盘保护如何共同生效。
- **事实与来源：** 用户选择最多保留 60 天并限制 40GB；`USER-DECISION:2026-08-26`。
- **选项：** 严格 60 天且无容量限制；60 天与总容量共同限制。
- **推荐：** 使用时间与容量双上限，避免全量 HTTP 正文占满服务器磁盘。
- **结论：** `sys-console` 日归档使用 gzip，`maxHistory=60`、`totalSizeCap=40GB`；任一限制先达到时，由 Logback 清理更旧归档，因此 60 天是最长保留期而不是最低保证。
- **原因：** 40GB 是用户确认的磁盘事故边界，同时保留正常流量下最多 60 天的排障窗口。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 当前活动文件不计入历史归档天数语义；不得对用户承诺高流量下一定存在完整 60 天历史。
- **后续：** D-012 验证 gzip、maxHistory 和 totalSizeCap 配置。
- **替代/被替代：** D-017 中 20GB 推荐值被用户选择的 40GB 替代。

## LOG-017 — 2026-08-26T11:49:07+0800 — 失败降级与完整验收合同
- **设计树节点：** D-012
- **轮次与依赖：** round 4 / D-005, D-006, D-007, D-008, D-009, D-010, D-011, D-013, D-014, D-015, D-016, D-017
- **状态：** confirmed
- **问题：** 日志采集失败、非法配置、异常响应、Servlet 异步请求和文件轮转如何处理与验收。
- **事实与来源：** 用户完整接受所列最终合同；工程规范要求类型安全配置在启动边界验证、异常保留原业务语义、异步资源具有明确完成与清理路径；`USER-DECISION:2026-08-26`，`PROJECT-RULE:SPRING-002`，`PROJECT-RULE:JAVA-004`，`PROJECT-RULE:JAVA-005`。
- **选项：** 日志严格失败并中断业务；静默吞掉采集失败；业务优先降级并保留终端故障信号和验收覆盖。
- **推荐：** 业务优先降级，非法配置启动失败，所有完成路径可验证。
- **结论：** 非法系统日志配置在启动阶段失败；运行时采集、JSON 编码或缓存失败不得改变接口响应，并向终端报告日志故障。正常、业务异常及未处理异常尽可能输出同 requestId 的 `HTTP_REQUEST`/`HTTP_RESPONSE`；Servlet 异步请求只在真实完成时输出一次响应；JSON/文本记录正文，SSE/文件只记录元数据；所有完成与异常路径清理 MDC。
- **原因：** 日志是排障旁路，不能成为业务可用性的单点故障，但配置错误和资源泄漏也不能静默存在。
- **影响工件：** CONTEXT / Spec / Ticket
- **约束或不变量：** 采集失败不改变原异常类型、状态码或响应正文；不得因为异步 dispatch 重复写入事件或让 requestId 串到其他请求。
- **后续：** 下游验证正常/异常/未处理异常、1 MiB 截断、凭证原值、加解密边界、文件/SSE 排除、异步完成、requestId 回传和 MDC 清理，并静态验证 sys-console 同步 appender、每日 gzip、60 天、40GB、不再生成 info/error 文件。
- **替代/被替代：** 无

## LOG-018 — 2026-08-26T15:27:06+0800 — 按非 SpecDev 完成模式直接实现
- **设计树节点：** post-plan reconciliation
- **轮次与依赖：** implementation follow-up / Goal Plan blocked
- **状态：** confirmed
- **问题：** 用户要求不创建 worktree、不提交，同时直接修改和测试，如何记录实际实现而不虚构 Ticket 完成。
- **事实与来源：** 用户明确选择“非 SpecDev 完成模式”；当前 backend `main` 基线为 `336134c5ff6e80bb8775617b64c4e04bf6c9d222`，实现存在于未提交工作区。
- **结论：** 允许在当前工作区完成代码和定向验证，但 Ticket 保持 ready、change 保持 blocked；测试结果只记录为观察证据，不能替代 implementation commit、direct-parent result SHA 或完成 Evidence。
- **影响工件：** Spec / Ticket / Map / Goal Plan / Evidence / status
- **约束或不变量：** 不 commit、不创建 worktree、不把 change 标记为 completed。

## LOG-019 — 2026-08-26T15:27:06+0800 — 根路径展示固定启动时间与动态当前时间
- **设计树节点：** post-plan reconciliation
- **轮次与依赖：** user follow-up / LOG-018
- **状态：** confirmed
- **问题：** 简单根路径如何区分实例启动时刻与本次访问时刻。
- **事实与来源：** 用户要求启动时间在应用启动后固定，当前时间在每次访问时刷新，相关方法直接放在 `IndexController`。
- **结论：** Controller 构造时从 `ApplicationContext#getStartupDate()` 固化格式化启动时间；处理 `/` 时重新取得当前时间，并用中文句子返回两者。
- **影响工件：** Spec / T-03 / Evidence
- **约束或不变量：** 时间格式统一为 `yyyy-MM-dd HH:mm:ss`；不引入额外服务类。

## LOG-020 — 2026-08-26T15:54:34+0800 — 启动摘要增加访问地址和启动时间并写入日志
- **设计树节点：** post-plan reconciliation
- **轮次与依赖：** user follow-up / LOG-018
- **状态：** confirmed
- **问题：** 应用启动后需要输出哪些最小运行信息，以及访问 URL 如何取得实际端口。
- **事实与来源：** 用户要求依次展示访问地址、启动时间、运行系统、启动耗时、运行内存，且无需“启动成功”提示语；诊断确认 `System.out` 不会写入 Logback 文件；实现方法全部位于 `DromaraApplication`。
- **结论：** `application.run` 返回后通过类自身 INFO logger 输出摘要，使终端和 sys-console 同时记录；启动时间取 `ApplicationContext#getStartupDate()` 并固定格式化；访问地址结合 SSL、server.address、实际 WebServer 端口和 context path，通配地址展示为 localhost；内存展示 JVM 已用和最大堆。
- **影响工件：** Spec / T-03 / Evidence
- **约束或不变量：** 不新增配置类或 YAML；不声称该 URL 是反向代理后的公网地址。

## LOG-022 — 2026-08-26T15:54:34+0800 — 启动摘要实现与运行时验证
- **设计树节点：** post-plan verification
- **轮次与依赖：** implementation follow-up / LOG-020
- **状态：** observed-not-complete
- **问题：** 未提交实现是否确实同时输出到终端和 sys-console。
- **事实与来源：** `DromaraApplicationUnitTest`、`SysConsoleLoggingUnitTest`、`IndexControllerUnitTest` 共 3 项通过；受影响 36 模块 `package -DskipTests` 通过；`spring-boot:run` 临时实例在 18081 启动，并在临时 sys-console 第 89 至 95 行写入完整摘要。
- **结论：** 当前工作区实现行为已观察通过；启动摘要包含 `访问地址：http://localhost:18081/`、固定启动时间、系统、8.51 秒耗时和内存，临时实例已正常关闭。
- **偏差与残余风险：** 未 clean 的 fat jar 因存量 `ruoyi-workflow` MapStruct 生成类重复方法而启动失败；为保护用户当前依赖 target/classes 的 8080 进程，未执行 `clean package`。这不影响 spring-boot:run 验证，但完整 clean fat jar 仍未验证。
- **影响工件：** T-03 / Evidence / Goal Plan
- **约束或不变量：** 观察证据不替代 implementation commit/result SHA；Ticket/change 保持未完成。

## LOG-023 — 2026-08-26T16:17:17+0800 — 后端合并范围提交并推送
- **设计树节点：** post-plan delivery
- **轮次与依赖：** user-authorized commit/push / LOG-018, LOG-022
- **状态：** confirmed
- **问题：** 后续 commit/push 授权如何投影到先前禁止 commit 的 Goal Plan 与 Evidence。
- **事实与来源：** 用户明确要求提交并推送 `ruoyi-vue-plus-namewta` 全部当前改动；本地与 origin/main 推送前均为 `336134c5f`，推送后均为 `a98d6edcc591550221dd983e293d43e3aac36d23`。
- **结论：** 25 个后端文件以 `feat(logging): add complete HTTP system logs` 单一提交推送到 `origin/main`；后端子仓库工作区 clean，远端 ahead/behind 为 0/0。
- **偏差与完成影响：** 该提交混合 T-01、T-02、T-03，无法补造三个逐 Ticket implementation/direct-parent checkpoint；完整 test 与 clean full/core package 未闭合，因此 Ticket/change 不标记完成，归档门继续阻塞。
- **影响工件：** Evidence / Goal Plan / change status
- **约束或不变量：** 不改写历史拆分提交，不把 push 成功等同 SpecDev change completion。

## LOG-021 — 2026-08-26T15:27:06+0800 — 归档请求受完成门阻塞
- **设计树节点：** archive precheck
- **轮次与依赖：** archive-and-consolidate / LOG-018
- **状态：** blocked
- **问题：** 当前 change 能否按 A-archive-and-consolidate 归档。
- **事实与来源：** change 状态为 blocked，Ticket 均未完成，implementation commit/result SHA 不存在；归档工作流只接受 completed change，并要求 dry-run 后单独确认。
- **结论：** 先回写遗漏范围和观察证据；归档只执行预检，不移动 change、不合并知识、不清理源文件。
- **影响工件：** Goal Plan / status / final handoff
- **约束或不变量：** 初始归档请求不视为第二阶段确认；不得绕过 completed gate。
