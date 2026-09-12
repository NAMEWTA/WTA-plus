---
schema_version: 3
artifact: ticket
change: 2026-08-26-current-log-system-eli5
id: T-01
title: 完整 HTTP 系统日志链路
status: done
planning_depth: deep
planning_depth_reason: 新增公共响应头和运维 wire format，并同时触达凭证原值记录、Servlet 异步生命周期、加密 Filter 顺序、MDC 线程安全和 common-web 共享核心路径。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/filter/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/interceptor/PlusWebInvokeTimeInterceptor.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/test/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/filter/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/interceptor/PlusWebInvokeTimeInterceptor.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/test/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-encrypt/src/main/java/org/dromara/common/encrypt/filter/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-encrypt/src/main/java/org/dromara/common/encrypt/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-json/src/main/java/org/dromara/common/json/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/logback-plus.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 完整 HTTP 系统日志链路

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/ticket/01-complete-http-system-log.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 在 common-web 的 Servlet Filter 边界交付可复用、默认开启且可关闭的完整 HTTP 系统日志能力，使一次请求的入口、结果和中间同步日志可以用服务端 requestId 关联。
- **可观察产出：** 普通请求产生恰好一条“请求进入”和一条“响应返回”中文字段单行 JSON；响应头返回服务端 UUID；日志记录 Filter 所见的原始请求和响应，覆盖同步、异常和 Servlet 异步完成路径而不改变业务字节。
- **来源：** `US-001`、`US-002`、`US-003`、`US-004`、`US-005`、`AC-001` 至 `AC-012`、`ADR-001`、`DEC-001` 至 `DEC-005`、`DEC-007`、`DEC-008`、`USER-DECISION`、`CODE`。
- **当前事实：** 现有 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/interceptor/PlusWebInvokeTimeInterceptor.java</Path>` 只记录请求参数和耗时，存在脱敏和 4000 字符截断，没有响应正文、requestId 或异步闭合；现有 Filter 自动配置和 common-json 依赖可复用。
- **Planning Depth 原因：** 本 Ticket 改变所有 Servlet 响应的附加 header 和运维 JSON 合同，并涉及凭证原值、安全例外、加密边界、异步回调与 MDC 清理，事故半径高。

## 2. 决策状态

### 已锁定决策

- `SysLogProperties` 和 `SysLogConfig` 位于 `org.dromara.common.web.config`，属性前缀固定为 `sys.log`；Java 默认 `enabled=true`、`maxBodySize=1MiB`，不向任何应用 YAML 添加默认项。
- `sys.log.max-body-size` 必须能绑定为大于 0 的有限 `DataSize`；不可解析、零或负数在 Spring 启动绑定阶段失败。
- 系统日志 Filter 位于现有 CryptoFilter 内侧，记录解密后请求和加密前响应；不得复制或修改加密算法。
- 每次新请求均由服务器调用 `UUID.randomUUID()` 生成权威 requestId；客户端 `X-Request-Id` 只映射为“上游请求标识”，不能覆盖服务端值。
- requestId 写入 MDC，并立即写入响应 `X-Request-Id`；同步、异常、异步回调和重复 dispatch 均遵守最多一条请求事件、一条响应事件和线程 MDC 清理不变量。
- 专用 logger 名固定为 `org.dromara.system.http`，事件级别为 INFO；事件消息本身是完整单行 JSON，不含可由 Logback pattern 再解释的占位符。
- 最终日志中的系统字段名、事件名、正文省略原因和采集失败提示使用中文；HTTP 方法、Content-Type、header 名、参数名、正文和 UUID 等来源或协议数据保持原值，JSON 布尔值保持原生类型。
- HTTP JSON 字段、正文分类、1MiB 单方向 UTF-8 安全截断、`bodyLength`、`truncated`、`completed` 与 omission reason 严格遵守上游 Spec。
- 普通 JSON/文本的 headers、参数和正文按 Filter 所见原值记录，包括 Authorization、Cookie、Set-Cookie、密码、验证码、token 和 client secret；不得自行恢复脱敏。
- multipart、文件、二进制、SSE 和流式方向不得缓存或输出原始正文；请求与响应分别判定，另一方向仍可正常记录文本正文。
- 运行时捕获、字段读取、JSON 序列化或日志调用失败时 fail-open，报告普通终端故障信号并保留原业务状态、headers、正文、异常类型、cause 和线程中断语义。
- 删除旧 `[PLUS]` 访问日志的注册和实现，关闭新能力时不得存在替代访问日志。

### 已采用的低影响假设

- 新增模块测试所需依赖只以 test scope 写入 common-web POM；CryptoFilter 只作为测试组合依赖，common-web 不新增对 common-encrypt 的运行时反向依赖。
- HTTP 文件行的无前缀格式由 T-02 配置；本 Ticket 提供稳定专用 logger、完整 JSON 消息以及 T-02 可复用的必要 Logback 格式接缝。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 类型安全配置、条件装配、请求/响应包装、事件模型、JSON 输出、requestId/MDC、同步/异常/异步生命周期、正文分类和截断、故障降级、旧拦截器移除及模块测试 | common-web 自动配置、common-json、现有 CryptoFilter、GlobalExceptionHandler、Servlet API 和 Spring Boot 配置绑定 | `@Log`/数据库审计、业务 Controller/DTO、前端协议、数据库、admin Logback 持久化、其他部署应用配置、分布式 traceId |

## 4. 要构建什么

请求进入 SysLogFilter 后立即得到新的服务端 UUID，响应 header 和 MDC 使用该值，并输出请求事件。Filter 以当时可见的 Servlet 数据构造多值 headers、parameters 和正文元数据；可记录的普通文本正文保留原值，只对 JSON 进行必要转义，日志副本在 UTF-8 字节上限处安全截断。

同步链结束时输出最终响应状态、headers、可记录正文与耗时；被 GlobalExceptionHandler 转换的异常按最终普通响应记录。未处理异常输出“处理完成=false”后重新抛出同一异常。进入 Servlet async 后不在初始 dispatch 提前完成，而由 AsyncListener 在 complete/error/timeout 中以同一 requestId 恢复 MDC、最多输出一次响应事件并立即清理。文件和持续流只输出元数据，不缓存原始字节。

## 5. 实现契约

- **入口或接缝：** common-web 自动配置注册 SysLogFilter；稳定 logger 为 `org.dromara.system.http`；配置入口为 `sys.log.enabled` 和 `sys.log.max-body-size`。
- **输入与输出：** 输入是 Filter 所见 HttpServletRequest/HttpServletResponse 和外部 Spring 属性；输出是响应 `X-Request-Id`、MDC requestId 以及 Spec 字段完整的中文“请求进入”/“响应返回”JSON 消息。
- **公共接口变化：** 所有启用能力的 Servlet 响应新增 `X-Request-Id`；新增两个稳定外部属性和 HTTP JSON 字段合同。业务 body schema、路径和 method 不变。
- **不变量：** 请求最多一个权威 ID 和一对事件；客户端 ID 不可信；业务请求/响应字节、状态、headers、加密、异步和流式语义不变；正文上限只限制日志副本；所有线程退出点清理 MDC。
- **状态或数据流：** `NEW -> REQUEST_LOGGED -> SYNC_COMPLETED | ASYNC_STARTED | CHAIN_ERROR -> RESPONSE_LOGGED`；异步 complete/error/timeout 共享原子一次性完成门闩，重复 dispatch 复用请求属性中的状态而不重复记录。
- **错误与失败行为：** 配置非法 fail-fast；捕获、编码和日志输出 fail-open；无法取得正文时降级为“正文已记录=false”和中文原因；未处理异常及 cause 原样传播；不吞线程中断。
- **兼容要求：** 附加响应 header 向后兼容；现有请求可重复读取能力必须保持；旧 `[PLUS]` 文本明确退出兼容合同；其他应用消费 common-web 时仅获得 HTTP 事件，文件策略仍由各自 Logback 决定。
- **安全与隐私要求：** 按 `ADR-001` 执行当前 change 的局部安全例外，凭证原值进入 HTTP 日志；例外不得扩展到 `@Log`、其他 logger 或其他 change。

## 6. 执行路线

1. 在 common-web 建立配置绑定、条件装配、事件序列化、正文媒体分类和 UTF-8 截断的失败优先测试接缝。
2. 实现专用 logger、单行 JSON 事件与请求/响应包装，完成同步成功、业务异常和未处理异常的数据流及 MDC 清理。
3. 按现有 CryptoFilter 注册顺序把 SysLogFilter 固定在其内侧，并用组合测试证明解密后请求、加密前响应和客户端密文均保持原合同。
4. 增加 Servlet async 一次性完成状态和 AsyncListener，覆盖 complete、error、timeout、重复 dispatch、正文排除与回调 MDC 清理。
5. 完成原值凭证、1MiB 多字节边界、multipart/文件/SSE、故障注入以及禁用/非法配置测试，并移除旧 PlusWebInvokeTimeInterceptor 注册和实现。
6. 运行模块定向测试与后端回归，核对实际修改范围，形成非空 implementation commit 和 Lead Evidence。

## 7. 路径访问契约

- **预计修改点：** frontmatter `expected_changes` 所列 common-web POM、配置、Filter、logging、旧 interceptor、自动配置 imports 和模块测试。
- **可写范围：** 仅 frontmatter `writable_paths`；若真实实现必须触达 admin、common-encrypt 生产源码、根 POM 或其他 common 模块，必须停止并走 deviation control。
- **只读上下文：** 现有 CryptoFilter/注册配置、common-json、admin Logback 和根 Maven profile。
- **共享路径：** 无；T-02 不写 common-web，T-01 是专用 logger 和事件消息合同的唯一 owner。
- **保留或不动：** `ruoyi-common-log`、业务 Controller、应用 YAML、数据库与其他可部署应用 Logback 配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常、原值与关联 | Servlet Filter 合同测试 + logger 捕获 | cwd `<Path>ruoyi-vue-plus-namewta</Path>`：`./mvnw -pl ruoyi-common/ruoyi-common-web -am test` | 正常 JSON 请求恰好两条合法事件，同一服务端 UUID 和响应 header；多值字段与凭证原值可还原 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` |
| 加密、截断与媒体边界 | CryptoFilter 组合、正文单元和 Servlet 合同测试 | 同一定向测试命令，检查 AC-003/004/006 用例 | 记录解密后/加密前正文；UTF-8 截断和原长度正确；文件、multipart、二进制、SSE 不缓存正文且业务输出不变 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` |
| 异常、异步与故障降级 | GlobalExceptionHandler、未处理异常、可控 AsyncContext、故障注入测试 | 重复执行异步与失败路径用例 | complete/error/timeout 各最多一条响应事件；异常语义和业务响应不变；MDC 无遗留；采集故障有终端信号 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` |
| 配置与旧日志回归 | ApplicationContextRunner + 源码/日志捕获断言 | 验证缺省、false、非法 DataSize，并扫描 `[PLUS]` 注册 | 默认装配；显式关闭无 Filter/header/事件；非法值启动失败；旧访问日志不再存在 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` |
| 后端回归 | Maven reactor | cwd `<Path>ruoyi-vue-plus-namewta</Path>`：`./mvnw test` | reactor 测试通过，无跨模块回归 | `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>` |

- **Workspace checks：** 默认 Goal Plan 策略为 current；implementation owner 在 current workspace 严格串行运行上述非 E2E 检查，形成 clean、非空 implementation commit，Lead 在父 HEAD 未漂移时复核 direct-parent。
- **E2E disposition：** not-required：公共 header、Filter 顺序、请求/响应字节和异步生命周期可由真实 Servlet/Spring Context/CryptoFilter 组合合同在进程内完整覆盖，不依赖浏览器、远程服务或部署环境。
- **E2E owner/environment：** Lead / current-workspace；本 Ticket 无 required E2E 场景。
- **Integration evidence：** Evidence 记录 base SHA、implementation commit、direct-parent 验证结果、父分支 result SHA、实际命令和退出状态；Goal Plan 若改为 required，则改用 source commit 与 parent-candidate 证据。

## 9. 发布、迁移与恢复

- **迁移顺序：** 配置、Filter、事件输出和旧 interceptor 移除在同一 Ticket 原子交付；无数据库或文件数据迁移。
- **兼容窗口：** 新 `X-Request-Id` 是附加响应元数据；不设置旧/new 访问日志双写窗口，部署后旧 `[PLUS]` 日志立即退出。
- **监控信号：** 启动配置绑定失败、普通终端采集故障信号、“请求进入”/“响应返回”配对、`处理完成=false`、MDC 清理测试和运行时 requestId 检索。
- **回滚或前向恢复：** 回滚 implementation commit 可恢复旧 interceptor 和原 Filter 注册；日志文件和业务数据无需回滚。采集容量或性能异常时可外部设置 `sys.log.enabled=false` 前向止损。
- **不可逆操作与批准点：** 无不可逆操作；凭证原值风险已由用户确认并由 `ADR-001` 批准。实际实施与 commit 仍需 I-implement/Goal Plan 的执行授权。
- **收缩条件：** 不适用 expand-contract；完成前必须通过源码扫描和日志捕获证明旧 `[PLUS]` 注册与输出为零。

## 10. 验收标准

- [ ] `AC-001`：默认同步 JSON 请求输出完整且可解析的一对事件，同一 requestId 与响应 header 可关联。
- [ ] `AC-002`：客户端 requestId 不能覆盖服务端 UUID，只作为“上游请求标识”。
- [ ] `AC-003`：加密组合记录解密后请求、加密前响应，客户端密文保持不变。
- [ ] `AC-004`：单方向 1MiB UTF-8 安全截断，完整 bodyLength 与业务字节不受影响。
- [ ] `AC-005`：headers 和正文中的凭证原值不脱敏、不删除。
- [ ] `AC-006`：multipart、文件、二进制、SSE/流式方向只记录元数据且不改变流。
- [ ] `AC-007`：GlobalExceptionHandler 最终错误状态和 body 被记录。
- [ ] `AC-008`：未处理异常产生“处理完成=false”后以相同异常/cause 传播，MDC 已清理。
- [ ] `AC-009`：异步 complete/error/timeout 和重复 dispatch 均最多输出一次响应事件且无 MDC 遗留。
- [ ] `AC-010`：捕获或 JSON 失败不改变业务响应，并留下终端故障信号。
- [ ] `AC-011`：显式关闭后无 Filter、事件、响应 header 或旧 `[PLUS]` 替代日志。
- [ ] `AC-012`：不可解析、零和负数 max-body-size 均使 context 启动失败。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-08-26-current-log-system-eli5/evidence/T-01.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`，并形成非空 implementation commit、direct-parent 或适用 candidate 验证及父分支 result SHA。
- [ ] 未发生未批准的范围、安全例外扩展、日志协议或发布偏差。
