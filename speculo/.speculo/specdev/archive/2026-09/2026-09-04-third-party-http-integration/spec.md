---
schema_version: 3
artifact: spec
change: 2026-09-04-third-party-http-integration
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-09-04-design-tree-consensus
  - USER-DECISION:system-management-third-party-admin-menu
  - ADR-001-through-ADR-012
  - CODE:ruoyi-vue-plus-namewta/pom.xml
  - CODE:plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts
  - CODE:release-artifacts/docker/infrastructure/mysql/init/10-ruoyi-base.sql
  - RESEARCH:https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
---

# Spec: 统一第三方 HTTP 供应商与 Endpoint 管理

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-04-third-party-http-integration/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-09-04-third-party-http-integration/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-09-04-third-party-http-integration/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

系统未来会持续接入企查查及其他外部 HTTP 供应商。每个供应商可能共享 base URL、认证请求头和签名规则，同时在其下提供多个 method、相对 path、参数结构和响应语义不同的 Endpoint。当前工作区没有统一的出站调用入口、供应商/Endpoint 生命周期、凭据保护、缓存刷新、限流、调用统计或出站日志合同；参考目录 `cde-third` 只提供单层 API 配置和计数，不能承担本产品目标。

若各业务模块分别创建 HTTP client，会重复认证、加密、超时、重试、日志和错误转换，并使供应商关闭、密钥变更和安全修复无法统一生效。若把数据库 endpoint 当作任意 URL/脚本执行器，又会形成 SSRF、凭据泄露和不可审计的执行入口。

### 目标用户与场景

- 平台管理员在 Admin Web 的“系统管理 → 三方接口管理”中维护供应商、Endpoint、凭据和固定策略，并控制启停。
- 运营和排障人员查看脱敏调用记录、供应商/Endpoint 聚合统计、quota 展示值，并通过 requestId 关联系统 HTTP 日志。
- 其他后端业务模块通过稳定的 `ThirdPartyGateway` 调用指定 `providerCode + endpointCode`，不接触数据库实体、凭据或底层 HTTP client。
- Provider 开发者以显式 Java SPI 扩展签名、非标准加密、分页或业务错误映射，而不复制整套 HTTP 基础设施。

### 成功标准

- 管理员可完成 Provider 和 Endpoint 的查询、新增、编辑、启停、受控删除以及凭据替换；任何响应都不回显凭据明文。
- 标准 JSON/query/form Endpoint 无需新增 Java 类即可由元数据执行；特殊供应商行为只能通过已注册 Java SPI 执行。
- 动态 Endpoint 不能通过绝对 URL、协议相对 URL、路径穿越、任意 header、表达式或跨域重定向绕过 Provider 目标。
- Provider 关闭优先于 Endpoint 状态；配置、凭据、Redis 或限流状态无法确认时，不发送 HTTP。
- 每个实际发出的 HTTP attempt 可计量，每个逻辑调用有最终成功、失败、超时或拒绝结果，并有脱敏日志关联。
- Admin 菜单、权限、前端组件清单、后端接口和 MySQL 基座保持一致，缺少选择或权限时失败关闭。

### 非目标

- 首期不提供异步投递、批量任务、消息队列、outbox 或自动补偿任务。
- 首期不支持 multipart、流式上传、复杂模板语言、JSONPath、SpEL、数据库脚本或任意类名反射。
- 首期不引入 OpenFeign、默认 WebFlux、通用熔断/自动降级平台或无限动态策略引擎。
- 首期不实现在线双密钥轮换；只保留密文版本和轮换兼容字段。
- 首期不把 quota 作为自动阻断依据，不在业务表保存完整请求/响应，不提供独立的日志注解场景。
- 不修改外部参考目录 `D:/Document/code/cde-standard/cde-base/cde-modules/cde-third`，不预置未经确认的生产供应商 URL、凭据或真实业务 Endpoint。

## 2. 解决方案与外部行为

### 解决方案摘要

在后端新增独立业务模块 `ruoyi-third`，并将跨模块 Java 合同放在 `ruoyi-api` 的 `org.dromara.third.api` 边界。模块采用新业务模块的 layered 模式；管理端入口只依赖 UseCase，业务调用 API Adapter 也只进入 UseCase/统一 Gateway 编排，持久化严格经过 Service、DAO、Mapper/XML。

同步执行核心使用 Spring `RestClient`。数据库定义 method、URI 模板、参数结构或响应类型的 Endpoint 走通用 RestClient executor；源码固定且类型化的供应商 facade 可通过 `@HttpExchange`/`HttpServiceProxyFactory` 构造，但必须使用同一 RestClient builder、Pipeline 和横切组件。只有未来明确的响应式场景才使用 `WebClient`。

Provider 是共享 base URL、默认认证、策略、总开关和汇总统计的聚合根；Endpoint 在 Provider 内以 `provider_code + endpoint_code` 唯一，继承 Provider 配置并只能声明受约束覆盖。标准传输由安全元数据驱动，特殊行为由按唯一 `providerCode` 注册并在启动时校验的显式 SPI 承担。

首期覆盖项仅限 Endpoint 的服务端共享请求头，格式为 `{\"headers\":{...}}`；Provider 的 base URL 始终是可信 origin，数据库 Endpoint 元数据不得改写它。

Admin Web 新增独立 `@namewta/domain-third` 与 `@namewta/web-domain-third`，由 Admin App 显式注入 service、runtime、domain module 和 manifest。菜单显示在现有系统管理目录下，但源码不并入 system 包。

### 主要流程

1. 业务调用方提交 Provider/Endpoint 编码、Endpoint schema 允许的参数，以及可选目标响应类型；调用方不能提交 method、完整 URL、认证凭据或任意 header。
2. Gateway 读取 DB 事实源对应的 Redis/Redisson 快照，合并 Provider 与 Endpoint 配置；缓存 miss 时回源 DB。无法确认状态、配置或凭据时返回失败结果且不发送 HTTP。
3. Pipeline 依次执行参数/schema 校验、Provider/Endpoint 状态检查、Provider/Endpoint 双层限流和并发检查、adapter 选择、凭据解密、认证/签名/加密、目标 URI 规范化与同源验证。
4. Factory 按已确认快照创建或取得受控 RestClient。Executor 仅将 Provider 的受信任 base URL 与校验后的相对 path、query、允许 header 和 body 组合后发送。
5. 只有标记为幂等的 Endpoint 才允许在服务端固定上限内重试。每次真实发送都记录同一 requestId 下的 attempt 序号并增加实际发送计数。
6. 响应先按 HTTP 状态分类，再由 Provider adapter 判断业务成功、业务错误和特殊解码。动态调用返回受限 `JsonNode`/value；类型化 facade 返回声明的 `Class<T>` 或 `ParameterizedTypeReference<T>`。
7. 完成后保存一条脱敏逻辑调用明细并更新 Provider/Endpoint 聚合。实际发送数按 HTTP attempt 计算；成功/失败/超时为逻辑调用终态，拒绝为发送前终态。
8. 统一横切组件为每次出站 request/response attempt 写结构化系统日志，复用 `SysLogEventSink`/`SysLogEventWriter`，并携带 requestId、attempt、Provider/Endpoint。日志正文仍受不可关闭的敏感黑名单和大小上限约束。

### 管理端行为

- “供应商管理”显示编码、名称、状态、base URL 摘要、策略摘要、quota 展示和汇总计数；支持新增、编辑、启停、凭据替换和受控逻辑删除。
- “Endpoint 管理”按供应商筛选，显示编码、method、相对 path、状态、内容类型、幂等/重试和策略覆盖；支持 schema/body 配置、启停、凭据覆盖和受控逻辑删除。
- “调用记录”只读展示 requestId、Provider/Endpoint、逻辑结果、HTTP 状态、供应商业务码、attempt 数、耗时、脱敏摘要和时间；不展示完整原始正文或凭据。
- “调用统计”按 Provider、Endpoint 和时间范围展示实际发送、逻辑成功、失败、超时、拒绝以及 quota 值；quota 只展示，不自动拦截。
- Provider/Endpoint 的凭据编辑为 write-only：管理员可替换或清除，详情只能返回类型、作用域、是否已配置、版本和更新时间。

### 边界、失败与稳定错误行为

- `PROVIDER_DISABLED`：Provider 已关闭；优先于 Endpoint 开关判断，不发送 HTTP。
- `ENDPOINT_DISABLED`：Provider 可用但 Endpoint 已关闭，不发送 HTTP。
- `RATE_LIMITED`：Provider 或 Endpoint 限流/并发约束拒绝，不发送 HTTP。
- `CONFIG_UNAVAILABLE`：DB/Redis 快照、必需配置、凭据、密钥版本或解密结果无法确认，不发送 HTTP。
- 其他失败使用已确认分类 `REJECTED`、`TRANSPORT`、`TIMEOUT`、`HTTP`、`PROVIDER`、`DECODE`；不得把凭据、签名原文或完整 raw body 放进调用方错误消息。
- HTTP 2xx 只代表传输成功。Provider adapter 判定的业务失败返回 `PROVIDER` 分类；HTTP 非 2xx 返回 `HTTP` 分类；转换失败返回 `DECODE` 分类。
- 完整 URL、`//host`、非法编码、规范化后越界的 `..`、未声明 header/参数、脚本或表达式在发送前拒绝。重定向只能保持相同 scheme、host 和有效端口；跨域重定向拒绝。
- Provider adapter 重复注册、必需 adapter 缺失或启动期静态配置冲突在应用启动/配置校验阶段失败，不静默选择实现。
- 出站日志写入失败不得泄露原始敏感数据；业务调用结果和日志故障必须可区分，日志故障进入模块内部告警日志。

### 状态转换与不变量

- Provider：`ENABLED ↔ DISABLED → DELETED`。删除前必须处于 DISABLED 且不存在未删除 Endpoint；仅逻辑删除。
- Endpoint：`ENABLED ↔ DISABLED → DELETED`。删除前必须处于 DISABLED；历史调用记录按编码保留；仅逻辑删除。
- Provider 为 DISABLED/DELETED 时，其所有 Endpoint 均不可调用；Endpoint 状态不能覆盖 Provider 状态。
- Endpoint 相同 Provider 内编码唯一；Provider 编码全局唯一；数据库唯一索引是并发最终保障。
- Endpoint 超时、重试、速率或并发覆盖不得超过 Provider 与服务端固定安全上限；未明确幂等时重试次数强制为零。
- 凭据按 scope + credential type 唯一；Endpoint 同类型有效凭据覆盖 Provider 凭据。任何读取/API/日志均不返回明文。
- DB 是配置事实源。保存事务成功后立即删除相应 Redis 快照并通过现有集群失效协调器通知各实例；失效确认失败时写操作返回失败，不能宣称新配置已全局生效。
- 服务端内置敏感 header/字段黑名单不可由 Provider/Endpoint 关闭；Endpoint 只能增加脱敏项。

## 3. 用户故事

- **US-001**：作为平台管理员，我希望统一维护 Provider 的 base URL、共享配置、凭据和总开关，以便集中控制供应商接入。
- **US-002**：作为平台管理员，我希望在 Provider 下维护多个 Endpoint 及其受约束参数/schema、独立开关和覆盖策略，以便无需复制 HTTP client。
- **US-003**：作为平台管理员，我希望凭据只能替换而不能读取明文，以便数据库和管理接口不会暴露秘密。
- **US-004**：作为后端业务开发者，我希望通过 `ThirdPartyGateway` 和稳定 DTO 调用 Provider/Endpoint，以便不依赖 ruoyi-third 实现或数据库实体。
- **US-005**：作为 Provider 开发者，我希望只为特殊签名、加密或业务错误编写显式 adapter，以便标准 HTTP 生命周期保持复用。
- **US-006**：作为运营人员，我希望查看 Provider/Endpoint 统计和脱敏调用记录，以便核对使用量、失败和超时。
- **US-007**：作为安全管理员，我希望非法目标、未知配置、关闭状态和限流故障在发送前拒绝，以便外部流量和凭据使用保持可控。
- **US-008**：作为排障人员，我希望通过 requestId 将业务调用记录与结构化出站 HTTP 日志关联，以便定位调用过程且不泄露敏感信息。
- **US-009**：作为 Admin Web 用户，我希望从“系统管理 → 三方接口管理”进入四类管理页面，并只看到被授权的操作。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 管理员具有 Provider 查询/写权限 | 新增并查询 Provider | Provider 编码唯一；详情不含凭据明文；写操作使用 POST 并产生安全操作日志 | 管理 HTTP 合同测试 + MySQL 集成测试 |
| AC-002 | Provider 已存在 | 新增两个不同 Endpoint，或重复同一 Endpoint 编码 | 不同编码成功；`provider_code + endpoint_code` 重复被唯一约束拒绝 | Endpoint Service/DAO + MySQL 集成测试 |
| AC-003 | Provider 或 Endpoint 已启用 | 管理员关闭 Provider 或 Endpoint 后调用 | 分别返回 `PROVIDER_DISABLED`/`ENDPOINT_DISABLED`，HTTP mock 未收到请求 | Gateway 集成测试 |
| AC-004 | Provider/Endpoint 配置保存成功 | 其他实例继续读取配置 | 对应 Redis 快照被失效并收到集群确认，新调用使用新版本；失效不可确认时保存不报告成功 | 缓存协调单元测试 + Redis 属性门控测试 |
| AC-005 | Provider/Endpoint 有凭据 | 保存、查询、覆盖、解密或使用凭据 | DB 只有 AES-GCM 类密文信封；Endpoint 同类型覆盖 Provider；API/日志无明文；缺 key/错误版本返回 `CONFIG_UNAVAILABLE` | 凭据 codec/service 安全测试 |
| AC-006 | 动态 Endpoint 声明 schema | 调用方传 JSON/query/form 参数 | 只接收声明名称、位置和类型；结构化 body 只替换白名单变量；返回 JSON/text/bytes 对应受限值 | 参数/codec 单元测试 + mock HTTP |
| AC-007 | 调用方尝试提供绝对 URL、`//host`、穿越、未知 header 或跨域重定向 | 调用动态 Endpoint | 调用在发送前拒绝或重定向被阻断，Provider 目标之外的 mock 服务零请求 | URI 安全负向集成测试 |
| AC-008 | 同一 `providerCode` 有两个 adapter，或必需 adapter 缺失 | Spring Context 启动 | 注册冲突/缺失校验失败，不选择任意实现 | ApplicationContext/registry 测试 |
| AC-009 | Endpoint 未标记幂等或已标记幂等 | 遇到可重试传输失败 | 非幂等只发送一次；幂等在固定上限内重试并沿用 requestId、递增 attempt | executor 重试测试 |
| AC-010 | Provider/Endpoint 达到限制，或 Redis 状态未知 | 发起调用 | 返回 `RATE_LIMITED` 或 `CONFIG_UNAVAILABLE`，HTTP mock 零请求，rejected 计数增加 | Redisson limiter 测试 + Gateway 测试 |
| AC-011 | HTTP 返回 2xx 且 Provider 业务码失败 | adapter 解析响应 | 结果分类为 `PROVIDER`；2xx 不被当作业务成功；完整 raw body 不进入业务响应/统计表 | adapter/response contract 测试 |
| AC-012 | 动态与类型化调用分别成功 | 读取结果 | 动态结果为受限 `JsonNode`/value；类型化 facade 按 `Class<T>`/`ParameterizedTypeReference<T>` 返回 DTO；两者不暴露底层 client | `ruoyi-api` 契约 + executor 测试 |
| AC-013 | 调用发生发送、重试、超时或发送前拒绝 | 查询记录和统计 | actual-send 按每个 HTTP attempt 计数；逻辑成功/失败/超时互斥；rejected 单独计数；Provider 和 Endpoint 维度一致 | 统计 Service + MySQL 集成测试 |
| AC-014 | 出站 request/response 含敏感头、敏感 JSON 和超长正文 | 调用完成后读取日志事件 | 每个 attempt 有可关联事件；内置黑名单已脱敏，正文按 `sys.log.max-body-size` 截断，Endpoint 规则只能增加脱敏 | SysLog sink 捕获测试 |
| AC-015 | Admin Web 加载动态菜单 | 用户展开系统管理 | 出现“三方接口管理”及供应商、Endpoint、调用记录、调用统计页面；组件键可解析到 third manifest | manifest 测试 + Admin E2E |
| AC-016 | 用户缺少页面或按钮权限 | 打开路由或执行新增/编辑/启停/凭据操作 | 前端失败关闭/隐藏操作，后端权限校验拒绝直接请求 | 权限负向 HTTP 测试 + Web 测试 |
| AC-017 | Provider/Endpoint 处于可删除或不可删除状态 | 执行删除 | 仅 disabled 对象可逻辑删除；Provider 有未删除 Endpoint 时拒绝；历史调用记录仍可查询 | UseCase/Service + MySQL 测试 |
| AC-018 | full 或 core bundle 构建 | 启动应用并解析 Gateway/Admin Controller | `ruoyi-third` 在两种 bundle 中均可装配；无 Provider 数据时不要求生产凭据即可启动 | Maven 双 bundle + context 测试 |

## 5. 范围

### IN

- `ruoyi-api` 中的 `ThirdPartyGateway`、`ThirdPartyRequest`、`ThirdPartyResponse<T>`、响应类型和稳定失败分类公共合同。
- 新的 layered `ruoyi-third` 模块、Provider/Endpoint/Credential/Invocation/Statistics 持久化和受保护 Admin Controller。
- 通用 RestClient executor、类型化 HTTP Service Client 基座、Provider SPI registry、client factory、请求 Pipeline 和横切 decorator/interceptor。
- URI/参数安全、AES-GCM 类凭据密文、Provider/Endpoint 配置继承、固定策略字段、有限幂等重试、Redisson 限流/并发控制。
- DB 事实源、Redis 二级快照、集群失效通知、脱敏调用明细、Provider/Endpoint 聚合统计和 quota 展示。
- ruoyi-third 出站结构化日志、系统日志 writer/sink 复用、模块本地不可绕过脱敏和截断。
- Admin Web 的独立 third domain/web-domain、四个页面、权限投影、manifest 和 App 显式组合。
- MySQL 8.4 的项目自有表 DDL，以及“系统管理 → 三方接口管理”目录、页面和按钮权限 DML。

### REUSE

- Spring Boot 4.1 / Spring Framework 7 的 `RestClient`、`@HttpExchange`、`HttpServiceProxyFactory` 和消息转换。
- `ruoyi-common-core/json/mybatis/web/security/log/redis/encrypt` 的现有公共入口；不依赖聚合 POM。
- `RedisUtils`/Redisson 限流能力和 `ClusterCacheInvalidationCoordinator` 集群失效合同。
- `SysLogEventSink`、`SysLogEventWriter`、`SysLogProperties` 和 `org.dromara.system.http` logger 输出合同。
- 现有 `R`、`PageResult`、Bean Validation、`@SaCheckPermission`、`@RepeatSubmit`、`@Log`、`@DSTransactional`、`BaseMapperPlus` 与 MapStruct 映射约定。
- plus-ui 的平台 HTTP 合同、domain/web-domain/App 组合、动态 manifest 和权限 runtime。

### OUT

- **OOS-001**：生产企查查或其他供应商的真实 URL、密钥和未提供的私有协议预置；通用能力以本地确定性 provider/mock 验收，真实供应商契约作为后续配置或 adapter 工作。
- **OOS-002**：异步、批处理、outbox、队列、回调编排和自动补偿。
- **OOS-003**：multipart、流式 body、复杂模板/脚本、任意 header 和任意响应类反射。
- **OOS-004**：在线双密钥轮换、外部 Secret Manager 集成和管理端主密钥管理。
- **OOS-005**：默认熔断、自动降级、quota 阻断和无限动态容错策略。
- **OOS-006**：前端查看未脱敏完整 body、独立日志注解、业务表存储完整 request/response。
- **OOS-007**：修改外部 `cde-standard/cde-third` 或恢复后端私有 SQL/script 目录。

## 6. 已锁定实现约束

- **DEC-001**：RestClient 是同步执行基座；固定类型化 facade 使用 `@HttpExchange`，动态 Endpoint 使用通用 RestClient，响应式场景才使用 WebClient。来源：`ADR-001`、`ADR-004`。
- **DEC-002**：Provider → Endpoint 两级聚合，Provider 关闭优先，Endpoint 以 Provider + code 唯一，配置继承后才允许受约束覆盖。来源：`ADR-002`。
- **DEC-003**：Facade、Registry/Strategy、Factory、Pipeline/Chain、Decorator/Interceptor 各有唯一职责；Provider SPI 按 providerCode 唯一注册并启动校验。来源：`ADR-005`。
- **DEC-004**：DB Endpoint 只能提供相对 path 和白名单参数；拒绝绝对 URL、`//host`、穿越、表达式、任意 header 和跨域重定向。来源：`ADR-004`。
- **DEC-005**：DB 是事实源，Redis/Redisson 是二级快照；保存后立即失效并确认集群通知，未知状态不使用陈旧快照。来源：`ADR-006`。
- **DEC-006**：凭据按 Provider/Endpoint scope 分离并使用认证密文；Endpoint 同类型覆盖 Provider，主密钥仅来自 yml/外部配置。来源：`ADR-003`、`ADR-008`。
- **DEC-007**：动态内容范围固定为 JSON/query/form 请求和 JSON/text/bytes 响应，body 使用结构化数据与显式变量白名单。来源：`ADR-009`。
- **DEC-008**：动态结果使用受限值，类型化 facade 使用显式 TypeRef；2xx 不是业务成功，Provider adapter 拥有业务错误映射。来源：`ADR-010`。
- **DEC-009**：Provider/Endpoint 双层限流与并发拒绝发生在发送前；quota 仅展示。来源：`ADR-007`。
- **DEC-010**：所有 ruoyi-third 出站流量自动进入统一系统日志管道，不能通过注解或类型化 facade 绕过脱敏。来源：`ADR-011`。
- **DEC-011**：前端 third domain/web-domain 独立于 system；菜单仅投影在系统管理目录下。来源：`ADR-012`。
- **DEC-012**：新后端业务模块采用 layered 五层；管理查询使用 GET，变更使用 POST，POST 使用安全 `@Log`，事务命令只在 UseCase 使用 `@DSTransactional`。来源：项目 `engineering-standards`。

## 7. 数据、接口与兼容

- **公共接口变化：** 在 `ruoyi-api` 新增 `org.dromara.third.api` 公共合同。Gateway 对常规可预期失败返回包含成功状态、requestId、错误分类/稳定错误码和脱敏元数据的 `ThirdPartyResponse<T>`；不以数据库 Entity、HTTP client 或 provider 实现类作为公开签名。新合同无存量兼容桥。
- **管理 HTTP：** 使用 `/third/provider`、`/third/endpoint`、`/third/invocation`、`/third/statistics` 资源前缀；只读 GET、变更 POST。权限统一使用 `third:<resource>:<action>` 命名空间，SQL、Controller、domain permission 和 web manifest 必须精确一致。
- **数据模型与持久化：** 项目自有表至少覆盖 Provider、Endpoint、Credential、脱敏 Invocation 和时间桶 Statistics；全部遵循主键、version、create_dept、create/update 审计和 del_flag 基线。Provider code 全局唯一，Endpoint 在 Provider 内唯一，Credential 在 scope/type 内唯一；索引支持管理筛选、缓存回源和统计聚合。
- **兼容要求：** 不改变现有 system/monitor/OpenAPI 行为；不把 third 页面加入 system 包。`ruoyi-third` 作为平台基础能力在 full/core bundle 均装配。新增 public API 后续演进遵守 Java API compatibility Skill，不删除或偷偷改变已发布签名。
- **迁移要求：** 无存量业务表或 `cde-third` 数据迁移。DDL 合并到 `<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>`；三方菜单/权限 DML 独立写入 `<Path>release-artifacts/docker/infrastructure/mysql/init/61-third-dml.sql</Path>`。已有环境根据发布 Tag 差异生成部署期 SQL，不重放完整基座。
- **发布或运维影响：** 部署需要配置凭据主密钥和静态超时/重试/策略安全上限。没有 Provider/凭据数据时应用可启动；发生 providerCode 注册冲突或非法静态策略时启动失败。调用所需密钥缺失时仅调用 fail-closed。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 凭据只以认证密文落库，写入 API 不记录敏感 request data；管理响应不回显明文。URI、redirect、header 和 schema 在发送前验证。内置敏感 header/字段黑名单不可关闭，Endpoint 只能增加规则。
- **NFR-002 性能与容量：** 调用读路径优先使用 Redis 快照，避免每次查询 DB；HTTP client 可按安全配置复用而不能为每个请求无界创建。统计写入不得改变同步调用结果，但失败必须可观测；不在未有容量证据时虚构吞吐阈值。
- **NFR-003 可用性与可靠性：** connect/read timeout 强制有界；只有显式幂等 Endpoint 可有限重试。Provider/Endpoint/Redis/配置/凭据状态未知时 fail-closed；不使用过期未知快照。缓存失效需要集群确认。
- **NFR-004 可观测性与运营：** requestId 贯穿 Gateway 响应、调用明细、attempt 统计和系统日志。结果区分 success/failure/timeout/rejected；日志捕获故障独立告警且不能输出未脱敏副本。
- **NFR-005 可扩展性：** 新 Provider 的标准 Endpoint 只需元数据；特殊行为只增加显式 SPI Bean。Registry 冲突在启动时发现，Factory/Pipeline/Decorator owner 不得被 Provider adapter 复制或替代。
- **NFR-006 可维护性：** 后端、前端、API、SQL 和测试分别遵守项目 Skill、最近 AGENTS 和静态模板；不创建同义 Utils/Manager/Repository 或包深层导入。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| `ruoyi-api` Gateway/DTO 合同 | 公共 Java API | AC-012, AC-018 | `./mvnw -pl :ruoyi-api -am test` | 契约测试、编译 |
| ruoyi-third 架构与 Spring Context | 模块架构/集成 | AC-008, AC-018 | Profile 架构测试先例；`validate-module-mode.mjs` | 架构扫描、Context 测试 |
| 本地确定性 HTTP provider/mock | HTTP 集成 | AC-003, AC-006-AC-012, AC-014 | Spring Test RestClient 接缝或本地 HTTP server | 行为测试、零请求断言 |
| Credential codec 与日志 sink | 安全单元/集成 | AC-005, AC-014 | `SysLogEventSink` 可注入接缝 | 密文、脱敏、截断断言 |
| DAO/Mapper + MySQL 8.4 | 持久化 E2E | AC-001, AC-002, AC-013, AC-017 | 项目 MySQL 属性门控测试 | SQL/schema/事务证据 |
| Cluster invalidation + Redis/Redisson | 多实例集成 | AC-004, AC-010 | `ClusterCacheInvalidationRedisIntegrationTest` | Redis 属性门控证据 |
| third domain/web-domain | 前端单元/组件 | AC-015, AC-016 | `pnpm --filter ... test/typecheck` | model/mapper/manifest/page 测试 |
| Admin App 动态路由 | App 集成/E2E | AC-015, AC-016 | `pnpm architecture:check`; `pnpm test:e2e` | 组件键、权限、浏览器证据 |
| MySQL 50/61 基座 | 发布数据 | AC-001, AC-002, AC-015, AC-017 | MySQL 8.4 10→50→61 初始化流程 | fresh-init Evidence |
| full/core bundle | 组装回归 | AC-018 | `./mvnw clean package ...`; bundle verifier | jar 内容、启动装配 |

## 10. 风险、假设与未决问题

### 风险

- 错误的 URI 规范化、redirect 策略或 header 合并可能形成 SSRF/凭据外发；必须以发送目标零请求负向测试作为验收，而不只做字符串校验测试。
- 凭据日志、Controller `@Log` 或异常消息可能绕过出站 sanitizer；管理写入、业务调用和日志失败路径都需要敏感值 canary 扫描。
- 统计、缓存失效和 HTTP 副作用跨越不同一致性边界；必须明确 DB 提交、缓存失效、实际发送和最终记录顺序，并暴露局部失败信号。
- 新增公共 API、后端模块、前端包、Admin 组合和根级 SQL 是多个共享路径；Ticket 必须指定唯一 owner 和集成顺序。

### 已采用的低影响假设

- 管理端采用四个子页面：供应商、Endpoint、调用记录、调用统计；凭据/策略使用所属对象的编辑流程。该分组可在不改变后端公共合同的情况下调整，以 manifest/page 测试验证。
- `ruoyi-third` 首期为单个业务 jar；只有出现两个以上真实独立发布子能力时才升级为聚合/BOM，不预建空子模块。
- 调用明细保存一条逻辑调用记录，并记录 attemptCount；实际发送聚合按每个 attempt 增加，最终结果聚合按逻辑调用增加。通过重试与拒绝测试验证计数关系。
- 无真实供应商协议输入时，通用 pipeline 以确定性本地 provider/mock 验收；真实企查查配置和 adapter 不以猜测写入。

### 未决问题

无。
