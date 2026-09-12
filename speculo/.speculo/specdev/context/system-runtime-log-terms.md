# 系统运行日志术语

- **Source:** `2026-08-26-current-log-system-eli5`
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-29-login-password-policy-runtime-and-http-log-redaction</Path>`
- **Graduated:** 2026-08-28

**系统运行日志（System Runtime Log）**：由 SLF4J/Logback 写入终端和文件、用于还原程序运行及 HTTP 调用过程的日志；ruoyi-admin 的唯一当前运行日志文件是 `sys-console.log`。
_Avoid_: 操作审计日志、`sys_oper_log`、`@Log`

**完整请求生命周期日志（Complete HTTP Exchange Log）**：共同记录请求进入时的方法、路径和输入，以及处理结束后的状态、响应正文与耗时，并能识别为同一次调用。
_Avoid_: 只有请求参数的访问日志、只有异常堆栈的错误日志

**HTTP 请求双记录（HTTP Exchange Pair）**：请求进入和响应完成分别输出一条单行 JSON 事件，二者共享服务器生成的 UUID `requestId`。
_Avoid_: 一个混合事件、依赖客户端 ID 作为主关联键

**Filter 所见正文（Filter-observed Payload）**：日志 Filter 在调用链中实际观察到的正文；当前顺序下请求是解密后正文，响应是外层响应加密前正文。
_Avoid_: Controller 返回值重建、客户端线路密文字节

**受控业务正文日志副本（Sanitized Business Payload Copy）**：普通 JSON/文本的日志副本保留非敏感字段；认证、会话、密码、token、secret、captcha 等凭据字段必须脱敏，非法或截断 JSON 整段隐藏；真实 request/response 正文不被改写。
_Avoid_: 把日志副本等同于业务正文、持久化可重放凭据、脱敏失败后回退输出原文

**单文件运行日志（Single Runtime Log File）**：INFO 及以上运行事件统一同步写入 `sys-console.log`，按天 gzip，最多保留 60 天且归档总量最多 40GB，任一上限先达到即清理更旧归档。
_Avoid_: 同时维护 sys-console、sys-info、sys-error；把 60 天理解为最低保证

**同步文件写入（Synchronous File Write）**：`sys-console.log` 由 `RollingFileAppender` 直接写入，不使用 `AsyncAppender` 或日志丢弃队列。
_Avoid_: 把旧 info/error 异步 appender 迁入唯一文件

**可记录正文（Loggable Payload）**：普通 JSON/文本请求与响应正文的日志副本每方向最多 1 MiB；超限只截断日志副本并保留原始长度，文件、multipart、二进制和流式正文只记录元数据。
_Avoid_: 缓存文件、SSE 或无限流后写入日志

**系统日志默认配置（System Log Defaults）**：HTTP 采集稳定默认值由 common-web Java 配置拥有，默认开启并可外部关闭；文件持久化由 Logback XML 拥有。
_Avoid_: 用普通 Spring Bean 接管 Logback 启动、在 YAML 重复低变化默认值
