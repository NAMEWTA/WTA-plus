---
schema_version: 3
artifact: spec
change: 2026-08-26-current-log-system-eli5
status: ready
ready_for_tickets: true
sources:
  - "USER-DECISION:2026-08-26-system-runtime-log-consensus"
  - "CHANGE-ADR:<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ADR.md</Path>"
  - "CHANGE-CONTEXT:<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/CONTEXT.md</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/interceptor/PlusWebInvokeTimeInterceptor.java</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>"
---

# Spec: 单文件完整 HTTP 系统运行日志

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

当前 `ruoyi-admin` 的服务器文件日志不能从一个稳定接缝还原完整 HTTP 调用：现有 `PlusWebInvokeTimeInterceptor` 只输出请求参数和耗时，JSON 参数会脱敏且最多保留 4000 个字符，不记录响应正文，也没有统一 requestId。当前 Logback 同时生成 `sys-console.log`、`sys-info.log` 和 `sys-error.log`，内容重复；`sys-console` 只保留 1 天且不压缩。生产 Maven profile 又将 `org.dromara` 设为 WARN，使 INFO 级请求日志无法稳定出现。

运维与开发人员需要仅凭服务器 `sys-console` 日志，用同一个 requestId 找到请求进入和响应完成两条事件，查看 Filter 边界实际观察到的输入输出，并依靠确定的滚动策略控制磁盘占用。

### 目标用户与场景

- 服务器运维人员：按前端提供的 requestId 检索一次请求的入口、结果、状态和耗时。
- 后端开发人员：排查参数传递、解密后正文、响应增强后正文、异常响应和异步完成问题。
- 前端支持人员：从 `X-Request-Id` 响应头取得服务端关联标识并用于报障，不需要改变现有业务响应结构。

### 成功标准

- 普通 JSON/文本 HTTP 调用产生恰好一条“请求进入”和一条“响应返回”单行 JSON 事件，共用服务端生成的 UUID requestId；系统自定义字段名、事件名和正文省略原因使用中文。
- 请求事件包含 Filter 所见的方法、路径、查询、参数、请求头和正文；响应事件包含状态、响应头、正文和耗时；正文值不做脱敏或语义改写。
- 加密接口记录解密后请求正文与响应加密前正文，客户端实际收到的响应内容不因日志采集改变。
- 文件、multipart、二进制和 SSE/流式方向不缓存或记录原始正文，只记录已确认的元数据。
- 所有环境默认启用；外部配置可显式关闭；普通应用 YAML 不需要新增默认项。
- `ruoyi-admin` 只继续写入 `sys-console.log`：同步写入、每日 gzip、最多 60 天、归档最多 40GB。
- 日志采集失败不改变业务响应，非法配置在启动阶段被拒绝。

### 非目标

- 不修改 `@Log`、`LogAspect`、`OperLogEvent`、登录日志事件或 `sys_oper_log` 数据库审计链路。
- 不修改任何业务 Controller、HTTP method、业务响应 DTO、数据库 schema 或前端请求协议。
- 不记录文件、multipart、二进制下载或 SSE/AI 流的原始字节。
- 不为其他可部署应用统一改造其 `logback-plus.xml`；本次文件持久化范围仅为 `ruoyi-admin`。
- 不删除部署机器上已经存在的历史 `sys-info*` 或 `sys-error*` 文件。

## 2. 解决方案与外部行为

### 解决方案摘要

在 `ruoyi-common-web` 的 Servlet Filter 边界提供默认开启的系统 HTTP 日志能力，以类型安全 Java 配置拥有启用开关和正文上限。Filter 位于现有 `CryptoFilter` 内侧，以经过自身时观察到的请求与响应为准，并替代现有 `[PLUS]` 请求/耗时日志，避免一次调用重复记录两套访问日志。

HTTP 采集通过专用 logger 以 INFO 输出；即使生产环境的通用 `org.dromara` logger 为 WARN，该 logger 在能力启用时仍必须有效。`ruoyi-admin` 的 Logback 配置只保留终端 appender 和同步 `sys-console` 文件 appender，并由 Logback 自身拥有路径、压缩、历史与总容量策略。

### HTTP 事件合同

每个 HTTP 事件占一条完整物理日志行，并且该行本身是合法 JSON；Logback 不得在 JSON 前后拼接时间、线程、级别或 logger 文本。正文放在 JSON 字符串字段中，只进行 JSON 必需的引号、换行和控制字符转义，解析该字符串后必须得到 Filter 捕获的原值。非 HTTP 普通系统日志继续沿用现有文本格式并写入同一个 `sys-console.log`。

两类事件在最终日志文件中使用以下稳定中文字段；Filter 内部标识不属于运维日志合同：

| 事件 | 必需字段 | 条件字段 |
|---|---|---|
| 请求进入 | `记录时间`、`事件类型`、`请求标识`、`请求方法`、`请求路径`、`查询字符串`、`请求参数`、`请求头`、`内容类型`、`内容长度`、`正文已记录`、`正文长度`、`正文已截断` | 客户端提供 `X-Request-Id` 时增加`上游请求标识`；可记录正文时增加字符串`正文`；排除正文时增加`正文省略原因` |
| 响应返回 | `记录时间`、`事件类型`、`请求标识`、`请求方法`、`请求路径`、`响应状态`、`响应头`、`内容类型`、`内容长度`、`耗时毫秒`、`处理完成`、`正文已记录`、`正文长度`、`正文已截断` | 可记录正文时增加字符串`正文`；排除或未能取得正文时增加`正文省略原因` |

字段语义：

- `事件类型` 只能是`请求进入`或`响应返回`。
- `请求标识` 是当前服务器使用 `UUID.randomUUID()` 为每次新请求生成的 UUID 字符串；客户端值不能覆盖它。
- `记录时间` 是事件实际输出时的服务器时间；具体标准时间格式由实现沿用项目 JSON 时间约定，但必须稳定且可解析。
- `请求参数`、`请求头`和`响应头`保留多值，不得把同名多值静默合并成单值；其中来自 HTTP 的参数名和 header 名保持原值，不做翻译。
- 普通 JSON/文本请求和响应的 headers、参数与 body 按 Filter 边界原值记录，不做字段脱敏、排除或值改写；这包括 Authorization、Cookie、Set-Cookie、密码、验证码、token 和 client secret。
- `正文长度` 是该方向实际观察到的原始正文 UTF-8 字节数；无正文时为 0。`内容长度` 保留 Servlet 边界报告的长度，未知时允许为 -1，不能用它替代实际`正文长度`。
- `正文已截断=true` 表示日志中的`正文`因正文上限被截断；截断只作用于日志副本，必须保持合法 UTF-8，不能截断业务请求或客户端响应。
- `处理完成=false` 表示 Filter 链、异步请求或响应写出未正常完成；日志随后必须保持原异常传播和响应语义，不能把失败伪装成成功。布尔值继续使用 JSON 原生 `true/false`，不转换成中文字符串。

### 主要流程

1. 新 HTTP 请求进入系统日志 Filter 时，服务器生成新的 UUID requestId，把它写入 MDC，并立即设置 `X-Request-Id` 响应头。若请求携带同名头，只将其作为可选“上游请求标识”记录。
2. Filter 在现有 `CryptoFilter` 之后观察请求，因此加密请求记录解密后的正文。它输出一次“请求进入”，并让下游继续读取语义完全相同的请求。
3. 普通 JSON/文本响应在外层 `CryptoFilter` 加密之前被观察并缓存日志副本。同步请求完成后输出一次“响应返回”，把未改写的响应继续交给外层链路或客户端。
4. Servlet 异步请求在真正完成、出错或超时时输出一次“响应返回”；初始 dispatch 返回和后续重复 dispatch 不得产生重复响应事件。完成回调只在输出事件期间恢复该 requestId 的 MDC，随后清理。
5. 两条事件和中间同步业务日志可通过 MDC requestId 关联。每个同步、异常、异步完成和超时路径均清理当前线程 MDC，不能污染下一次线程复用。
6. `ruoyi-admin` 将 INFO 及以上系统日志同步写入当前 `sys-console.log`；到日切点后归档为 `.log.gz`，按 60 天和 40GB 双上限清理。

### 正文覆盖范围

- 可记录：普通 `application/json`、`application/*+json`、`text/*` 及其他被应用作为普通文本处理的非流式请求/响应。
- 请求排除：`multipart/*` 和文件上传的原始内容；仍记录路径、查询/表单参数、Content-Type 和 Content-Length。
- 响应排除：附件、Excel、ZIP、PDF、图片、音视频、通用二进制、SSE、流式与持续推送内容；仍记录路径、Content-Type、Content-Length、状态和耗时。
- 排除只作用于对应方向：例如 multipart 上传后的普通 JSON 响应仍可记录响应正文。
- 媒体类型缺失或无法可靠判定为普通文本时，默认不记录正文，并通过“正文省略原因”表明原因。

### 配置行为

`ruoyi-common-web` 提供前缀为 `sys.log` 的类型安全配置，稳定外部属性只有：

| 属性 | Java 默认值 | 行为 |
|---|---:|---|
| `sys.log.enabled` | `true` | `false` 时不装配系统 HTTP 日志 Filter，不输出 HTTP 事件，也不增加 `X-Request-Id` 响应头 |
| `sys.log.max-body-size` | `1MiB` | 分别限制请求和响应日志副本；必须是大于 0 的有限 DataSize |

默认值只存在于 Java 配置，不向 `application.yml`、`application-dev.yml` 或 `application-prod.yml` 增加常规配置项。部署方仍可通过 Spring Boot 支持的 YAML、环境变量、系统属性或命令行参数覆盖。无法绑定的值、非正数或非有限正文上限必须使应用启动失败。

### 边界、失败与稳定错误行为

- 日志读取、缓存、字段提取、JSON 编码或输出调用发生异常时，记录终端故障信号并继续原业务流程；不能改变原状态码、响应正文、异常类型或异常 cause，也不能吞掉线程中断。
- 若下游抛出未处理异常，Filter 在可行范围内输出“处理完成=false”的“响应返回”，保留当时状态与已写正文，然后重新抛出同一个异常。
- 经现有 `GlobalExceptionHandler` 转换为正常 HTTP 错误响应的业务异常，按普通响应记录最终状态和响应正文。
- 日志失败后无法获得某方向正文时，允许输出元数据事件并以“正文已记录=false”和“正文省略原因”说明，不允许伪造空正文为完整捕获。
- 进程在请求处理中被强制终止时允许只有“请求进入”；不得为了等待合并事件而延迟入口日志。
- 磁盘写入失败时 Logback 的终端/内部状态用于报告故障；系统日志能力不能自行重试业务请求。

### 状态转换与不变量

```text
NEW
  -> REQUEST_LOGGED
      -> SYNC_COMPLETED  -> RESPONSE_LOGGED
      -> ASYNC_STARTED   -> ASYNC_COMPLETED/ERROR/TIMEOUT -> RESPONSE_LOGGED
      -> CHAIN_ERROR     -> RESPONSE_LOGGED(completed=false) -> 原异常继续传播
```

- 每个新请求最多一个服务端 requestId、一个“请求进入”和一个“响应返回”；只有进程非正常终止等无法执行完成逻辑的情况允许缺少响应事件。
- 客户端 requestId 永远不是内部权威关联标识。
- 日志包装器不得改变请求可重复读取、响应字节、响应加密、状态码、headers、异步或流式语义。
- 正文上限仅限制日志副本，不成为 HTTP 上传或下载大小限制。
- 采集关闭时不得保留旧 `[PLUS]` 拦截器产生另一套请求日志。
- 60 天和 40GB 是归档双上限；任一条件先达到即可清理更旧日志，因此 60 天不是最低保留保证。

## 3. 用户故事

- **US-001**：作为服务器运维人员，我希望按 requestId 找到一次普通 HTTP 调用的完整请求与响应，以便无需复现即可排查参数和结果。
- **US-002**：作为后端开发人员，我希望日志反映 Filter 实际看到的解密后请求和加密前响应，以便定位应用内部处理问题而不是线路密文问题。
- **US-003**：作为前端支持人员，我希望响应头返回服务端 requestId，以便把可检索标识提供给后端排障。
- **US-004**：作为服务器运维人员，我希望文件、multipart 和流式调用不写入原始字节，以便这些请求仍可观测而不会破坏流式行为或迅速放大日志。
- **US-005**：作为系统维护人员，我希望日志能力默认开启但可显式关闭，并在采集失败时保持业务可用，以便处理容量或采集故障。
- **US-006**：作为服务器管理员，我希望只维护一个按日压缩且受时间与容量双限制的文件日志，以便控制重复日志和磁盘风险。
- **US-007**：作为访问后端根路径的维护人员，我希望同时看到固定的应用启动时间和本次访问时间，以便快速判断实例何时启动以及接口当前是否可达。
- **US-008**：作为启动服务的维护人员，我希望控制台和 sys-console 摘要直接展示访问地址、启动时间、运行系统、启动耗时和运行内存，以便启动后立即获得最常用的运行信息并保留排障记录。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | `sys.log.enabled` 未配置 | 发送带 JSON 请求体的同步成功请求 | 输出一条`事件类型=请求进入`和一条`事件类型=响应返回`的合法单行 JSON；`请求标识`相同且响应头等于该值；系统标签为中文，参数、headers、请求 body、状态、响应 headers/body 和耗时可还原 | Servlet Filter 合同测试 + 捕获专用 logger 事件 |
| AC-002 | 请求携带自定义 `X-Request-Id` | 完成普通请求 | 服务端生成不同的 UUID 作为`请求标识`和响应头；客户端值只出现在`上游请求标识` | Servlet Filter 合同测试 |
| AC-003 | API 请求/响应启用现有加密链路 | 完成调用 | 请求事件包含解密后正文，响应事件包含加密前正文；客户端仍收到现有加密响应 | Filter 顺序集成测试，组合 `CryptoFilter` 与系统日志 Filter |
| AC-004 | 请求或响应正文超过 1MiB，且包含跨截断边界的 UTF-8 多字节字符 | 完成调用 | 日志 body 不超过上限且是合法 UTF-8，`truncated=true`、`bodyLength` 为完整原始字节数；业务请求和响应未截断 | 正文缓存/截断单元测试 + Servlet Filter 合同测试 |
| AC-005 | 请求含 Authorization、Cookie、密码、验证码、token 或 client secret，响应含相关原值 | 完成普通 JSON/文本调用 | 对应 headers 和 body 字符串中的值与 Filter 所见原值一致，不脱敏、不删除 | 安全例外精确合同测试 |
| AC-006 | multipart 上传、文件/二进制下载或 SSE/流式响应 | 完成、超时或关闭调用 | 对应方向“正文已记录=false”，无原始正文；保留路径、参数、Content-Type、Content-Length、状态和耗时；业务流不被缓存或改变 | 媒体分类单元测试 + multipart/下载/SSE Servlet 合同测试 |
| AC-007 | Controller 抛出被 `GlobalExceptionHandler` 映射的业务异常 | 请求结束 | 仍输出同 requestId 的响应事件，记录最终 HTTP 状态和错误响应 body | Web 异常集成测试 |
| AC-008 | Filter 链抛出未处理异常 | 请求失败 | 尽可能输出“处理完成=false”响应事件，然后原异常及 cause 不变地继续传播；MDC 已清理 | Filter 失败路径单元测试 |
| AC-009 | Servlet 请求进入异步模式并经历完成、错误或超时之一 | 异步生命周期结束 | 请求事件一次、响应事件一次；初始/重复 dispatch 不重复；完成回调使用同 requestId 且执行后无 MDC 遗留 | 可控 AsyncContext/AsyncListener 合同测试 |
| AC-010 | 日志正文读取或 JSON 编码被注入失败 | 完成业务请求 | 业务状态、headers 和正文不变；终端可见采集故障；MDC 清理；允许正文降级为未记录元数据 | 故障注入单元测试 |
| AC-011 | `sys.log.enabled=false` | 发送请求 | 不装配或不执行系统日志 Filter；无“请求进入”/“响应返回”、无新增 `X-Request-Id`；不存在旧 `[PLUS]` 访问日志替代它 | ApplicationContext 条件装配测试 + 日志捕获 |
| AC-012 | `sys.log.max-body-size` 无法解析、为 0 或负数 | 启动 Spring context | 配置绑定/校验失败，应用不进入运行态 | ApplicationContextRunner 配置测试 |
| AC-013 | local/dev/prod 有效配置 | 发送请求 | 能力启用时专用 HTTP logger 均以 INFO 输出，不被 prod 的 `org.dromara=WARN` 抑制，且每个事件只写一次 | profile/logger 配置合同测试 |
| AC-014 | 检查 `ruoyi-admin` Logback 配置 | 初始化日志系统并触发日滚动合同 | 只有终端和同步 `sys-console` 文件路径；历史模式以 `.log.gz` 结尾，`maxHistory=60`、`totalSizeCap=40GB`；不再装配或引用 info/error/async appender | Logback 配置解析测试 + 静态 XML 合同测试 |
| AC-015 | 部署前已有 `sys-info*`/`sys-error*` 历史文件 | 部署新版本 | 应用停止写入这些文件，但不主动删除它们；当前 `sys-console.log` 路径保持兼容 | 发布前后人工文件检查 |
| AC-016 | 任意 HTTP JSON 事件含换行、引号或控制字符 | 读取对应物理日志行 | 整行可由 JSON parser 解析，解析后的 body 与原值一致，且没有 Logback 文本前后缀 | 文件 appender 集成测试 |
| AC-017 | 应用已完成 Spring 启动 | 多次访问根路径 `/` | 每次响应均包含同一个基于 `ApplicationContext#getStartupDate()` 的启动时间，并包含按本次访问重新计算的当前时间；两者使用 `yyyy-MM-dd HH:mm:ss` | Controller 单元测试 |
| AC-018 | `DromaraApplication` 完成启动 | 查看启动后的控制台和 `sys-console.log` | 同一条 INFO 摘要在两个位置均可见；分隔线之间依次显示访问地址、固定启动时间、运行的系统、启动耗时、已用/最大堆内存；不再输出额外“启动成功”提示语 | 启动类日志捕获测试 + 真实启动落盘验证 |
| AC-019 | 服务使用默认或覆盖后的端口、地址、SSL 与 context path | 生成启动摘要 | 访问地址使用实际 WebServer 绑定端口，按 SSL 选择协议并规范化 context path；通配监听地址以 `localhost` 展示，IPv6 host 使用方括号 | 启动类单元测试 + 运行时冒烟检查 |

## 5. 范围

### IN

- `ruoyi-common-web` 中可复用的系统 HTTP Filter、类型安全默认配置、事件建模和 Servlet 同步/异步生命周期处理。
- 现有 `PlusWebInvokeTimeInterceptor` 请求/耗时日志的替代，防止重复日志和旧脱敏/4000 字符合同继续生效。
- common-web 自动配置入口及所需模块内测试接缝。
- `ruoyi-admin` 的 `logback-plus.xml` 单文件、同步、gzip、60 天、40GB 和专用 HTTP logger 配置。
- `ruoyi-admin` 根路径的固定启动时间/动态访问时间响应，以及启动后的访问地址、系统、耗时和内存控制台摘要。
- 支撑上述合同的后端测试及构建验证。

### REUSE

- 复用 `ruoyi-common-web` 的 Filter 自动配置边界和现有 Servlet/MVC 错误处理。
- 复用 `ruoyi-common-json` 的项目 `JsonMapper`/`JsonUtils` 生成合法 JSON，不新增另一套 JSON 依赖。
- 复用现有 `CryptoFilter` 注册顺序与请求解密、响应加密能力，不复制加密逻辑。
- 复用 Logback `RollingFileAppender` 和 `TimeBasedRollingPolicy`，不自建文件轮转线程。
- 复用 Maven Wrapper 和现有 JUnit/Spring Boot 测试基础设施。

### OUT

- **OOS-001**：`ruoyi-common-log` 的注解审计、登录事件和数据库落库链路保持不变，因为它承担业务审计而不是服务器运行排障。
- **OOS-002**：不修改业务接口、前端代码或数据库，因为 `X-Request-Id` 是向后兼容的附加响应头，日志采集不需要业务协议迁移。
- **OOS-003**：不把文件、multipart、二进制或流式正文纳入日志，因为会扩大内存/磁盘并改变流式语义。
- **OOS-004**：不为 `ruoyi-monitor-admin`、`ruoyi-snailai-server` 或 `ruoyi-snailjob-server` 修改各自 Logback 持久化策略；它们若消费 common-web，HTTP 事件仍遵循自身 appender 配置。
- **OOS-005**：不建立跨进程分布式 traceId 或接受客户端 requestId 作为内部主键；本合同只覆盖当前 Servlet 应用的一次 HTTP 生命周期。
- **OOS-006**：不自动清除部署前遗留的 info/error 历史文件，避免未经授权的破坏性文件操作。

## 6. 已锁定实现约束

- **DEC-001**：HTTP 日志采集位于现有 `CryptoFilter` 内侧，以 Filter 所见的解密后请求和加密前响应为合同，不重建 Controller 语义对象或线路密文。来源：`ADR-001`、`LOG-011`。
- **DEC-002**：普通 JSON/文本 headers、参数和正文原值记录，包括认证凭证；这是当前 change 对 `SEC-003`/`SEC-005` 的局部安全例外，不得扩展到 `@Log` 或其他日志体系。来源：`ADR-001`、`LOG-013`。
- **DEC-003**：请求和响应分别输出完整物理行 JSON，通过服务端 UUID requestId 和 MDC 关联；最终日志的系统字段、事件名和正文省略原因使用中文，HTTP 方法、header、参数与正文等来源数据保持原值；客户端同名 header 只作为`上游请求标识`。来源：`LOG-009`、`LOG-012`、`LOG-014`、`USER-DECISION:2026-08-26-Chinese-system-log-output`。
- **DEC-004**：正文默认上限为每方向 1MiB，只截断日志副本；文件、multipart 和流式方向只记录元数据。来源：`LOG-007`、`LOG-008`。
- **DEC-005**：HTTP 稳定默认值由 common-web Java 配置拥有，默认开启且可外部覆盖；文件持久化由 `ruoyi-admin` Logback XML 拥有，不向应用 YAML 写入默认项。来源：`LOG-005`、`LOG-010`。
- **DEC-006**：`sys-console` 保持同步写入；归档每日 gzip、最多 60 天且最多 40GB，不创建新的 info/error 文件。来源：`LOG-004`、`LOG-015`、`LOG-016`。
- **DEC-007**：采集运行时失败不改变业务，非法配置启动失败；同步、异常和 Servlet 异步路径均有一次性完成与 MDC 清理合同。来源：`LOG-017`。
- **DEC-008**：common-web 继续只依赖所需 common 子 artifact；JSON 复用其现有显式 `ruoyi-common-json` 依赖，不依赖聚合 POM 或反向依赖业务模块。来源：`PROJECT-RULE:ARCH-002`、`PROJECT-FACT:ruoyi-common-module-map`。

## 7. 数据、接口与兼容

- **公共接口变化：** 所有启用该能力的 Servlet HTTP 响应新增 `X-Request-Id` header；这是附加、向后兼容的响应元数据。现有请求路径、method、body 与业务响应 schema 不变。外部 `sys.log.enabled`、`sys.log.max-body-size` 和 HTTP JSON 字段构成运维合同。
- **数据模型与持久化：** 不新增或修改数据库、缓存、消息或对象存储数据；只改变服务器日志文件内容和归档。
- **兼容要求：** 现有 `${log.path}/sys-console.log` 当前文件路径保持；普通非 HTTP 系统日志文本格式保持；客户端可忽略新增 header。旧 `[PLUS]` 日志文本不再作为兼容合同。
- **迁移要求：** 无数据迁移。部署后停止创建/写入新的 `sys-info.log`、`sys-error.log` 及其归档；已有文件保留，由运维按既有流程处理。当前 sys-console 在下一次滚动后使用 gzip 历史命名。
- **发布或运维影响：** 最坏情况下每个普通请求最多新增约 2MiB 正文日志加结构字段，并同步参与请求延迟；服务器需为当前文件和最多 40GB 归档预留空间。回滚可恢复旧 Filter/拦截器和 Logback 配置，不涉及数据回滚。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 用户已明确接受原样记录 Authorization、Cookie、密码、验证码、token、client secret 等凭证的风险；不得在实现中自行脱敏，也不得把例外扩展到其他日志体系。日志文件访问控制、主机权限和备份治理由部署环境负责，超出本 change 实现范围。
- **NFR-002 性能与容量：** 单方向正文日志副本默认最多 1MiB；二进制和流式不缓存正文；文件同步写入且不设置未确认的延迟或吞吐数字；归档按 60 天/40GB 双上限控制。
- **NFR-003 可用性与可靠性：** 采集失败 fail-open，配置错误 fail-fast；不吞中断，不改变业务响应；异步完成只记录一次并清理资源/MDC。
- **NFR-004 可观测性与运营：** 所有环境默认可观察，专用 logger 不受 prod 通用 WARN 阈值抑制；HTTP 行可被 JSON parser 独立解析；requestId 可从响应头、请求事件、响应事件和同步中间日志交叉检索。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 正文分类、UTF-8 截断与事件序列化 | 纯单元 | AC-004、AC-005、AC-006、AC-016 | JUnit；项目默认 `./mvnw test` | 测试报告与解析断言 |
| Servlet Filter 同步、异常、故障注入和 MDC | Web/Servlet 合同 | AC-001、AC-002、AC-007、AC-008、AC-010 | Spring mock servlet/Mockito；现有后端 JUnit 基础设施 | 定向测试输出 |
| Servlet 异步生命周期 | Web/Servlet 合同 | AC-009 | 可控 AsyncContext/AsyncListener 测试，重复运行验证一次性 | 定向测试输出与资源清理断言 |
| CryptoFilter 组合顺序 | 模块集成 | AC-003 | 组合 common-encrypt 与 common-web 的 Filter 链测试 | 明文/密文边界断言 |
| ConfigurationProperties 与条件装配 | Spring context | AC-011、AC-012 | ApplicationContextRunner/配置绑定测试 | context 成功/失败证据 |
| logger level 与 Logback XML | 配置合同/日志集成 | AC-013、AC-014、AC-016 | 解析 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>` 并初始化隔离 Logback context | appender、policy、单行输出断言 |
| 历史文件非破坏性迁移 | 人工运维 | AC-015 | 临时日志目录启动/停止检查 | 文件清单 |
| 根路径时间响应 | Controller 单元 | AC-017 | 直接构造 Controller 并跨两次访问比较启动时间与当前时间 | 定向测试输出 |
| 启动运行摘要与访问地址 | 启动类单元/运行时 | AC-018、AC-019 | 捕获 INFO 事件验证摘要字段和地址构造边界，并启动应用同时检查终端与 `sys-console.log`；至少执行 admin reactor package | 定向测试、启动输出、物理文件与打包摘要 |
| 后端回归与组装 | Maven reactor | 全部回归 | cwd `<Path>ruoyi-vue-plus-namewta</Path>`：`./mvnw test`；`./mvnw clean package -DskipTests`；`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | 命令、退出码、测试/打包摘要 |

## 10. 风险、假设与未决问题

### 风险

- 原值凭证日志在凭证有效期内可被重放，并会随归档最多保存 60 天；这是用户接受且已进入 ADR 的风险。
- 同步写入与每请求最多约 2MiB 正文日志会增加磁盘 I/O 和接口尾延迟；发生容量或性能事件时可用 `sys.log.enabled=false` 显式关闭。
- Filter 与加密、XSS、异常和 Servlet async 的包装顺序错误可能改变请求/响应或重复日志；必须由组合与异步合同测试约束。
- 40GB 上限可能使高流量环境不足 60 天，不能将 60 天解释为最低保存承诺。
- 同一文件混合普通文本系统日志与无前缀 JSON HTTP 行需要精确的 Logback 格式路由；AC-016 要求以真实文件 appender 验证，不能只测事件对象。
- 启动摘要的访问地址属于本机可访问提示，不是反向代理后的公网权威 URL；AC-019 仍需真实启动冒烟确认最终显示值。

### 已采用的低影响假设

- 已有 info/error 历史文件不主动删除；验证部署后只停止新写入。若运维需要清理，使用独立、明确授权的服务器操作。
- `timestamp` 沿用项目 Jackson/运行环境的可解析时间约定，不在 Spec 新增跨系统时间格式标准；序列化测试固定其实际输出合同。
- Logback 清理在其滚动策略正常触发时生效，本 change 不自建启动扫描或后台清理器。

### 未决问题

无。
