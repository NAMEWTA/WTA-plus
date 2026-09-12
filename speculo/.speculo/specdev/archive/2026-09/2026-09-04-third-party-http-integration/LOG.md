## LOG-001 — 2026-09-04T00:00:00+08:00 — 启动第三方 HTTP 统一接入设计
- **设计树节点：** 不适用
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 用户希望把多个外部 HTTP 供应商及其多个 URI 统一纳入一个可管理、可统计、可扩展的子模块，并指出 `HTTPExchange` 是候选技术。
- **事实与来源：** 用户对话；`D:/Document/code/cde-standard/cde-base/cde-modules/cde-third` 本地源码；`<Path>ruoyi-vue-plus-namewta/pom.xml</Path>`。
- **选项：** 直接扩展现有单 API 配置；建立供应商-接口层级的通用模块；引入现成网关/Feign 体系。
- **推荐：** 先完成供应商-接口公共合同和 HTTP 执行层设计，再决定是否分阶段迁移现有配置。
- **结论：** 进入 `specdev/grill-with-docs`，不执行产品实现。
- **原因：** 该能力同时改变公共 API、凭据安全、数据库模型、外部副作用与运维合同，不能从单一 HTTP 库选择直接推导完整实现。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 永久 `context/` 与 `adr/` 只读；实现授权保持未授予。
- **后续：** 逐轮关闭设计树 frontier。
- **替代/被替代：** 无

## LOG-002 — 2026-09-04T00:00:00+08:00 — 外部事实基线
- **设计树节点：** D-002 / D-003 / D-004 / D-006
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** Spring Boot 4.x 下应如何选择 HTTP 客户端和声明式接口层。
- **事实与来源：** Spring Boot 4.0 REST client reference（页面列出 Boot 4.1.1 stable，并在非 WebFlux 场景推荐 `RestClient`）；Spring Framework REST Clients reference（`@HttpExchange` 通过 `HttpServiceProxyFactory` 可由 `RestClient`、`WebClient` 或 `RestTemplate` 执行）；Spring Cloud OpenFeign reference（5.0.3，项目已 feature-complete，建议迁移到 Spring HTTP Service Clients）；Spring Cloud compatibility page（2025.1.x 对应 Boot 4.0.x/4.1.x）。
- **选项：** OpenFeign；WebClient；RestClient；`@HttpExchange` + RestClient；底层自建 JDK HttpClient。
- **推荐：** 非响应式后端使用 `RestClient` 作为统一执行器，具体静态供应商适配器可使用 `@HttpExchange`；数据库驱动的动态 URI 仍通过 `RestClient` 的通用 exchange API；不新增 OpenFeign。
- **结论：** 待用户在 D-002、D-004 中确认适用范围。
- **原因：** `@HttpExchange` 是声明式接口合同而非动态配置中心；动态 URI、请求体、请求头、加密和响应类型需要可编程请求构造与拦截管线。
- **影响工件：** CONTEXT / ADR / Spec
- **约束或不变量：** 不能把 `@HttpExchange` 误写成可由数据库任意生成的运行时 Java 接口。
- **后续：** 询问本轮根决策。
- **替代/被替代：** 无

## LOG-003 — 2026-09-04T00:10:00+08:00 — 实现仓库与模块 owner
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 统一第三方 HTTP 能力由哪个代码仓库和 Maven 模块负责。
- **事实与来源：** 用户确认；当前工作区是 `ruoyi-vue-plus-docs`，后端产品子仓库为 `ruoyi-vue-plus-namewta`；外部 `cde-third` 仅作为参考目录。
- **选项：** 修改外部 `cde-standard/cde-third`；修改当前产品仓库；两边同步。
- **推荐：** 当前产品仓库单一 owner，避免父工作区和外部标准仓库双写漂移。
- **结论：** 在 `ruoyi-vue-plus-namewta/ruoyi-modules` 下建立独立、完整的 `ruoyi-third`；`cde-third` 只作参考。
- **原因：** 产品实现、Maven reactor、admin bundle 和数据库交付均由当前后端子仓库控制。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 不自动修改 `D:/Document/code/cde-standard/cde-base/cde-modules/cde-third`。
- **后续：** 在模块地图、根 POM、admin bundle 和 MySQL 基座中建立受影响映射。
- **替代/被替代：** 无

## LOG-004 — 2026-09-04T00:11:00+08:00 — HTTP 客户端混合基座
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** Spring Boot 4.x 下统一 HTTP 客户端与声明式接口层如何选型。
- **事实与来源：** 用户确认；Spring Boot/Framework 官方资料见 `https://docs.spring.io/spring-boot/4.0/reference/io/rest-client.html` 与 `https://docs.spring.io/spring-framework/reference/integration/rest-clients.html`；当前工程为 Spring Boot 4.1.0、Java 21。
- **选项：** OpenFeign；WebClient；RestClient；RestClient + `@HttpExchange`。
- **推荐：** `RestClient` 统一连接/超时/状态/消息转换，`@HttpExchange` 只承载代码拥有的强类型 facade；不引入 OpenFeign，非响应式 MVC 不引入 WebFlux/WebClient。
- **结论：** 用户确认混合方案；运行时 URI 的精确定义与执行边界由 D-008 继续收敛。
- **原因：** `@HttpExchange` 是声明式 Java 合同，数据库动态 endpoint 仍需可编程请求构造。
- **影响工件：** CONTEXT / ADR / Spec
- **约束或不变量：** 不把 `@HttpExchange` 误写成可由数据库任意生成的运行时接口。
- **后续：** D-008 关闭动态 URI 与静态 facade 的边界。
- **替代/被替代：** 无

## LOG-005 — 2026-09-04T00:12:00+08:00 — 供应商与 endpoint 聚合
- **设计树节点：** D-003
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 外部供应商、多个 URI 和开关/统计的领域层级。
- **事实与来源：** 用户确认；`cde-third` 当前只有 `third_api_config` 单层 API 配置，不能表达供应商共享 base URL/headers 与子 URI。
- **选项：** 单表 API 配置；供应商与 endpoint 两级；供应商、产品、版本、endpoint 多级。
- **推荐：** 两级聚合，供应商关闭优先，endpoint 编码在供应商内唯一，继承后允许覆盖。
- **结论：** 采用 Provider -> Endpoint 两级聚合；统计保留供应商汇总和 endpoint 维度。
- **原因：** 与企查查“共享头/base URL、多个 URI”形态一致，同时为未来供应商扩展保留稳定边界。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** provider disabled 时任何子 endpoint 均不得发出外部请求。
- **后续：** D-009 收敛适配器注册与设计模式；D-010 收敛缓存刷新。
- **替代/被替代：** 无

## LOG-006 — 2026-09-04T00:13:00+08:00 — 元数据与 SPI 混合执行
- **设计树节点：** D-004
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 动态 endpoint 的通用请求定义与供应商特殊逻辑如何共存。
- **事实与来源：** 用户确认；企查查示例显示同一供应商可共享 headers/base URL，但每个 URI 的 body 和响应语义不同。
- **选项：** 全部 Java DTO；全部数据库模板；标准元数据 + 显式 SPI。
- **推荐：** 标准 method/path/query/header/body/JSON 由安全白名单元数据驱动，签名/加密/分页/错误码转换由显式 Java SPI 承担。
- **结论：** 采用混合模式；禁止数据库脚本、SpEL 和任意类名反射执行。
- **原因：** 保留运营配置灵活性，同时避免运行时执行不受控代码。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 任何特殊行为必须是可测试、可注册、可审计的显式 SPI。
- **后续：** D-009、D-012、D-013 继续细化 SPI、参数和响应合同。
- **替代/被替代：** 无

## LOG-007 — 2026-09-04T00:14:00+08:00 — 统一同步调用端口
- **设计树节点：** D-005
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 其他业务模块应通过什么公共 API 发起外部请求。
- **事实与来源：** 用户确认；工程规则要求跨业务模块只使用公开 API/common SPI，不依赖实现模块内部实体。
- **选项：** 各业务模块自建 HTTP client；暴露 provider-specific client；统一 `ThirdPartyGateway`。
- **推荐：** 首期统一同步 `ThirdPartyGateway`/`ThirdPartyRequest`/`ThirdPartyResponse`，异步投递后置。
- **结论：** 调用方只传 provider/endpoint 编码和受约束参数，不接触数据库实体；异步、批量和 outbox 后续再做。
- **原因：** 将供应商配置、认证、容错、统计集中在唯一入口，降低跨模块耦合。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 公共合同不泄露凭据、数据库实体或底层 `RestClient` 生命周期。
- **后续：** D-013 细化强类型/动态响应结果。
- **替代/被替代：** 无

## LOG-008 — 2026-09-04T00:15:00+08:00 — 首期容错边界
- **设计树节点：** D-006
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 首期容错能力和策略配置的复杂度。
- **事实与来源：** 用户确认；工程已有 Redisson 能力；当前工程没有统一外部 HTTP 容错模块。
- **选项：** 引入 Spring Cloud/Resilience4j；自建无限动态策略；仅做超时和失败分类。
- **推荐：** 强制超时/失败分类，幂等接口才允许有限重试；限流复用 Redisson；策略先静态化，不默认自动降级。
- **结论：** 采用用户确认的简单首期边界；熔断、并发隔离等仅在不扩大依赖的前提下逐步纳入。
- **原因：** 当前目标是统一对外请求，不让容错框架反向主导领域模型。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 未明确幂等的 endpoint 不得自动重试；超时必须有上限。
- **后续：** D-015 明确 Redisson key、失败关闭和静态策略格式。
- **替代/被替代：** 无

## LOG-009 — 2026-09-04T00:16:00+08:00 — DB 密文凭据
- **设计树节点：** D-007
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** API key/secret、私钥和加密密钥的首期托管方式。
- **事实与来源：** 用户确认；项目存在 `ruoyi-common-encrypt`；yml 配置可提供对称加密主密钥。
- **选项：** 外部 Secret-only；普通表明文；DB 密文 + yml 主密钥。
- **推荐：** 采用 DB 密文 + yml 主密钥，但将主密钥视为部署秘密，不写入版本库；请求/响应日志默认脱敏且不保存完整正文。
- **结论：** 前期凭据允许 DB 托管但必须密文存储；对称加密密钥由 yml 配置；敏感日志与留存由 D-014 继续明确。
- **原因：** 满足前期管理端可配置性，同时避免密钥明文泄漏。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 主密钥不得由管理端接口返回；密文解密失败不得发出请求。
- **后续：** D-011 收敛密文 payload、版本和轮换；D-014 收敛日志留存。
- **替代/被替代：** 无

## LOG-010 — 2026-09-04T00:30:00+08:00 — 动态参数与动态 endpoint 的执行边界
- **设计树节点：** D-008
- **轮次与依赖：** round 2 / D-002
- **状态：** confirmed
- **问题：** 固定接口的动态值与数据库动态 endpoint 是否使用同一 HTTP 执行路径。
- **事实与来源：** 用户确认；Spring HTTP Service Clients 的 `@HttpExchange` 是源码固定的声明式 Java 合同；数据库可运行时改变 method/URI/参数结构/响应类型时必须使用通用请求构造；用户补充 SSRF 风险边界。
- **选项：** 所有请求都使用 `@HttpExchange`；所有请求都使用动态 `RestClient`；固定契约与动态 endpoint 分流。
- **推荐：** 固定接口只动态传值时使用类型化 `@HttpExchange`；数据库定义 endpoint 走 `RestClient`；响应式场景单独走 `WebClient`。
- **结论：** Provider 提供受信任 base URL，数据库只提供校验后的相对 path/白名单参数；拒绝完整 URL、`//host`、路径穿越、脚本/表达式、任意 header 和跨域重定向。
- **原因：** base URL 不是安全边界，绝对 URL 可绕过它；运行时 URI 必须在发送前完成规范化和目标约束校验。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** `provider base URL + 数据库相对 path/参数 -> RestClient`；源码固定契约 -> `@HttpExchange`。
- **后续：** D-012 细化参数 schema 和模板；实现阶段增加 URL/SSRF/重定向负向测试。
- **替代/被替代：** 无

## LOG-011 — 2026-09-04T00:31:00+08:00 — 供应商扩展设计模式
- **设计树节点：** D-009
- **轮次与依赖：** round 2 / D-003, D-004, D-005
- **状态：** confirmed
- **问题：** 如何让供应商和 URI 持续扩展而不复制 HTTP client 或越层。
- **事实与来源：** 用户确认；`cde-third` 当前仅提供单 API 配置与 AOP 计数，未提供供应商适配器注册；工程 layered 模式要求入口通过 UseCase/Service 和公开端口。
- **选项：** 每个供应商复制 client；单一巨型 if/else；Facade + Registry/Strategy + Factory + Pipeline/Chain + Decorator/Interceptor。
- **推荐：** 采用组合模式，并明确每个模式的唯一 owner。
- **结论：** Facade 只编排，Registry/Strategy 只选适配器，Factory 只建 client，Pipeline/Chain 只处理请求生命周期，Decorator/Interceptor 只处理横切；通过 providerCode 唯一 Spring Bean/SPI 注册，启动校验冲突。
- **原因：** 新供应商只实现特殊行为窄 SPI，标准传输复用通用链，避免运行时反射和供应商代码散落。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** providerCode 冲突或所需 SPI 缺失时启动/配置校验失败，不发出请求。
- **后续：** D-011 细化 credential adapter；D-013 细化响应 adapter。
- **替代/被替代：** 无

## LOG-012 — 2026-09-04T00:32:00+08:00 — DB 配置与 Redis 二级缓存
- **设计树节点：** D-010
- **轮次与依赖：** round 2 / D-003, D-006
- **状态：** confirmed
- **问题：** 供应商/endpoint 配置如何在多实例中刷新并保持关闭状态一致。
- **事实与来源：** 用户确认；项目已有 Redis/Redisson，`RedisUtils` 提供缓存和发布能力；工程配置/状态必须由明确 owner 维护。
- **选项：** 每次直接查 DB；只用本地缓存；DB 事实源 + Redis/Redisson 二级缓存和失效通知。
- **推荐：** DB 为事实源，二级缓存用于读路径，变更后按 provider/endpoint 精确失效并通知其他实例。
- **结论：** 保存成功后立即清空对应 Redis 缓存并发送失效通知；新请求读取新快照；无法确认启用状态、配置或凭据时 fail-closed，不使用过期未知快照。
- **原因：** 既控制数据库读取压力，又避免管理端关闭/修改在集群中延迟生效。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 缓存失效失败不能被当成配置已刷新；读取失败不得发出外部请求。
- **后续：** 实现阶段定义 key、topic、序列化版本和并发更新测试。
- **替代/被替代：** 无

## LOG-013 — 2026-09-04T00:33:00+08:00 — 调用统计与系统日志边界
- **设计树节点：** D-014
- **轮次与依赖：** round 2 / D-003, D-005, D-007
- **状态：** confirmed
- **问题：** 调用次数、明细和完整请求/响应日志如何分工。
- **事实与来源：** 用户确认；`ruoyi-common-web` 的 `SysLogFilter`/`SysLogEventWriter` 通过 `org.dromara.system.http` logger 记录入站 Servlet HTTP 事件，并由 `SysLogBodySanitizer` 递归脱敏、按 `sys.log.max-body-size` 截断；该 Filter 不会自动捕获出站 RestClient。
- **选项：** 仅计数；完整正文写业务明细表；聚合/脱敏明细入库，完整正文交给结构化系统日志。
- **推荐：** 已发出请求计一次并拆分结果，业务表只保留脱敏摘要/聚合；完整正文由统一出站日志组件按现有日志契约、大小上限和敏感黑名单输出。
- **结论：** 成功、失败、超时、拒绝分别统计；quota 首期仅展示；敏感黑名单由服务端固定，endpoint 只能增加规则不能关闭；出站日志实现由 D-016 负责。
- **原因：** 计量与审计可查询，同时避免把完整凭据或个人信息长期写入业务表；现有入站日志组件不能直接覆盖出站调用。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 不保存完整请求/响应到业务表；Authorization、签名、密钥及敏感字段必须脱敏。
- **后续：** D-016 明确自动捕获、公共 writer 复用和可选注解边界。
- **替代/被替代：** 无

## LOG-014 — 2026-09-04T00:34:00+08:00 — Redisson 限流与 fail-closed
- **设计树节点：** D-015
- **轮次与依赖：** round 2 / D-006, D-003
- **状态：** confirmed
- **问题：** 供应商/endpoint 限流和配置异常时的阻断语义。
- **事实与来源：** 用户确认；`ruoyi-common-redis` 的 `RedisUtils.rateLimiter` 基于 Redisson `RRateLimiter`，现有 `@RateLimiter` 也在令牌不足时抛出业务异常。
- **选项：** 仅供应商限流；仅 endpoint 限流；两级限流并由 Redis 状态决定是否 fail-closed。
- **推荐：** 供应商级和 endpoint 级均可配置，endpoint 受供应商上限约束；拒绝发生在 HTTP 发送前。
- **结论：** 限流或并发拒绝不发出 HTTP；Redis/配置状态无法确认时 fail-closed；稳定错误码区分 `PROVIDER_DISABLED`、`ENDPOINT_DISABLED`、`RATE_LIMITED`、`CONFIG_UNAVAILABLE`，不泄露凭据。
- **原因：** 防止供应商总额度被子接口绕过，并避免 Redis 故障时无限放大外部流量。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** Redis 故障、供应商关闭、endpoint 关闭和缺配置均在发送前终止调用。
- **后续：** 实现阶段验证 key 维度、令牌配置、并发释放和多实例行为。
- **替代/被替代：** 无

## LOG-015 - 2026-09-04T00:45:00+08:00 - Ciphertext credential model
- **Design nodes:** D-011
- **Round and dependencies:** round 3 / D-007, D-009
- **Status:** confirmed
- **Question:** How should provider and endpoint credentials be stored and prepared for later rotation?
- **Facts and sources:** User decision; project `ruoyi-common-encrypt`; deployment yml/external secret configuration.
- **Options:** Plaintext DB values; one shared provider credential; separate provider/endpoint ciphertext records.
- **Recommendation:** Separate provider and endpoint credentials, with endpoint credentials overriding the same provider credential type; use authenticated encryption such as AES-GCM and keep master key material outside DB.
- **Conclusion:** Separate scope models are accepted. Store versioned ciphertext with a rotation field; do not implement complex online key rotation in the first release.
- **Reason:** Limits credential blast radius and keeps future rotation compatible without expanding the first release.
- **Affected artifacts:** CONTEXT / ADR / Spec / Ticket
- **Constraints or invariants:** Missing master key, unsupported version, or decryption failure is fail-closed and must not send HTTP.
- **Next:** Define the encrypted envelope and persistence constraints in the implementation specification.
- **Supersedes:** none

## LOG-016 - 2026-09-04T00:46:00+08:00 - Dynamic endpoint content types
- **Design nodes:** D-012
- **Round and dependencies:** round 3 / D-003, D-004, D-008
- **Status:** confirmed
- **Question:** What request and response formats are supported by database-defined endpoints?
- **Facts and sources:** User decision; Spring `RestClient` message conversion boundary.
- **Options:** Arbitrary content and templates; typed Java DTO for every endpoint; constrained JSON/query/form and JSON/text/bytes.
- **Recommendation:** Use explicit parameter schema and versioned structured JSON with variable allowlists.
- **Conclusion:** First release supports JSON/query/form requests and JSON/text/bytes responses. Multipart, streaming upload, and complex templates require a later explicit SPI.
- **Reason:** Covers the expected supplier APIs while keeping request construction declarative and bounded.
- **Affected artifacts:** CONTEXT / ADR / Spec / Ticket
- **Constraints or invariants:** No arbitrary URL, header, JSONPath, SpEL, or database template script.
- **Next:** Define schema validation and body variable mapping in the implementation specification.
- **Supersedes:** none

## LOG-017 - 2026-09-04T00:47:00+08:00 - Response and business-error contract
- **Design nodes:** D-013
- **Round and dependencies:** round 3 / D-004, D-005, D-009
- **Status:** confirmed
- **Question:** How do dynamic values and typed facades map into one response contract?
- **Facts and sources:** User decision; `ThirdPartyResponse<T>` contract boundary.
- **Options:** Raw body everywhere; provider-specific return types everywhere; constrained JSON values for dynamic calls and typed Java values for source-defined facades.
- **Recommendation:** Use `JsonNode`/restricted values for dynamic endpoints and `Class<T>` or `ParameterizedTypeReference<T>` for typed facades.
- **Conclusion:** 2xx means transport success only. Explicit provider adapters map business errors to `PROVIDER`; complete raw bodies stay out of business responses and statistics tables.
- **Reason:** Preserves a stable caller contract without pretending dynamic endpoints have compile-time types.
- **Affected artifacts:** CONTEXT / ADR / Spec / Ticket
- **Constraints or invariants:** Keep requestId, status, sanitized headers, business code, data/summary, and duration; preserve cause only for controlled log correlation.
- **Next:** Define error taxonomy and response DTO fields in the implementation specification.
- **Supersedes:** none

## LOG-018 - 2026-09-04T00:48:00+08:00 - Mandatory outbound HTTP logging
- **Design nodes:** D-016
- **Round and dependencies:** round 3 / D-014, D-002, D-005
- **Status:** confirmed
- **Question:** Should ruoyi-third capture outbound request and response events automatically?
- **Facts and sources:** User decision; `SysLogEventWriter`, `SysLogEventSink`, and `org.dromara.system.http` in `ruoyi-common-web`; existing `SysLogFilter` captures inbound Servlet traffic only.
- **Options:** Business-side opt-in annotation; business-table detail records; mandatory unified outbound logging through the module executor.
- **Recommendation:** Enforce one outbound path and reuse the public system-log writer/sink contract, while keeping redaction and truncation in `ruoyi-third`.
- **Conclusion:** All ruoyi-third outbound HTTP calls are automatically captured by the common pipeline. No independent log scenario or business annotation is needed in the first release. Server-side blacklist, size limit, and redaction remain mandatory.
- **Reason:** Prevents typed facades or adapters from bypassing audit and sensitive-data controls.
- **Affected artifacts:** CONTEXT / ADR / Spec / Ticket
- **Constraints or invariants:** Complete payloads never enter business tables; log output still cannot expose blacklisted headers or fields.
- **Next:** Specify outbound event fields, correlation, truncation, and sanitizer composition.
- **Supersedes:** none
## LOG-019 - 2026-09-04T01:00:00+08:00 - Admin menu and frontend ownership
- **Design nodes:** D-017
- **Round and dependencies:** round 4 / D-001, D-003, D-014
- **Status:** confirmed
- **Question:** Where should the third-party administration UI live in navigation and source ownership?
- **Facts and sources:** User decision; `sys_menu` places System Management at menu ID `1761400000000000001`; frontend conventions map primary domain ownership to the backend module rather than menu placement.
- **Options:** Add pages to system packages; create an unrelated top-level menu; create independent third packages projected under System Management.
- **Recommendation:** Create independent `domains/third` and `web-domains/third`, compose them in Admin Web, and place a `三方接口管理` directory under System Management.
- **Conclusion:** The management menu is under System Management. The initial child pages are provider management, endpoint management, sanitized invocation records, and aggregate statistics; credential and policy editing stays in provider/endpoint workflows.
- **Reason:** Keeps navigation familiar while preserving module ownership and avoiding credential-list exposure.
- **Affected artifacts:** CONTEXT / ADR / Spec / Ticket
- **Constraints or invariants:** Dynamic route component keys and permission codes must match SQL and manifest registrations; frontend visibility never replaces backend authorization.
- **Next:** Capture pages, permissions, and menu verification in the Spec and Tickets.
- **Supersedes:** none

## LOG-020 - 2026-09-04T01:01:00+08:00 - Design-tree consensus
- **Design nodes:** D-001 through D-017
- **Round and dependencies:** round 4 / all prior decisions
- **Status:** confirmed
- **Question:** Does the user confirm the complete third-party HTTP integration design?
- **Facts and sources:** Explicit user confirmation after the complete design summary; the user additionally supplied D-017.
- **Options:** Reopen design; defer unresolved branches; declare consensus and proceed to Spec.
- **Recommendation:** Declare consensus because the frontier is empty and the new menu requirement is decision-complete.
- **Conclusion:** Design-tree status is `consensus`; proceed to `specdev/S-spec`, then `specdev/T-tickets` as requested by the prior routing confirmation.
- **Reason:** All external behavior, security boundaries, public contract direction, persistence/cache policy, logging, and management navigation have confirmed owners.
- **Affected artifacts:** Spec / Ticket / status
- **Constraints or invariants:** Planning does not authorize product-code implementation.
- **Next:** Write and validate `spec.md`.
- **Supersedes:** none

## LOG-021 - 2026-09-04T15:23:34+08:00 - Ticket split approved and published
- **Design nodes:** D-001 through D-017
- **Round and dependencies:** post-consensus / validated Spec
- **Status:** confirmed
- **Question:** Does the user approve the proposed 13-Ticket decomposition and its dependency/path ownership model?
- **Facts and sources:** Explicit user response “批准，按这个拆分”; `spec.md` is ready for Tickets with AC-001 through AC-018.
- **Options:** Revise the split; publish the approved Tickets; start product implementation immediately.
- **Recommendation:** Publish the approved Tickets and route to Goal Plan before implementation because the change has 13 Tickets, Deep security/schema work, shared paths, multiple repositories and required E2E Gates.
- **Conclusion:** T-01 through T-13 and the Tickets Map are published as Ready. T-01/T-02 are the only initial parallel candidates; all acceptance contracts are covered.
- **Reason:** The approved split is decision-complete and each Ticket has an independent behavior, writable boundary, verification matrix and integration evidence contract.
- **Affected artifacts:** Ticket / Tickets Map / status
- **Constraints or invariants:** Ticket approval does not authorize implementation commits, candidate integration, production migration, credentials, permissions or real provider calls.
- **Next:** Run specdev/P-goal-plan and obtain its execution authorization before specdev/I-implement.
- **Supersedes:** none

## LOG-022 - 2026-09-04T15:31:02+08:00 - Goal Plan created with current workspace policy
- **Design nodes:** D-001 through D-017
- **Round and dependencies:** post-Tickets / T-01 through T-13
- **Status:** blocked
- **Question:** Which cross-Ticket workspace and integration strategy applies after the user approves the Ticket split?
- **Facts and sources:** User selected “不开启”; backend/frontend/root baselines were read as backend main@a63c83ac3e8f999013a40fc350098023ebcbdf11, frontend main@587a629a7b3566213a2998fb7e4002d07aa2c168, root main@7e25128213417a00aad85079032fc902b6b3ffff; backend/frontend worktrees are clean.
- **Options:** Required worktrees with candidate-merge; current workspace with direct-parent; defer planning.
- **Recommendation:** Fix current workspace/direct-parent with one implementation writer and Lead-only integration, because the user explicitly declined worktrees and the repository has multiple shared paths.
- **Conclusion:** Goal Plan schema v6 is published with modes high-assurance and release-coordination, Lead codex:/root, implementation agent limit 3, unlimited integration attempts from config, and current/direct-parent topology. It remains blocked until implementation commit and direct-parent update authorization are explicit.
- **Reason:** The plan is decision-complete for sequencing, Gate, ownership, E2E, migration and recovery, but execution authorization is a separate irreversible boundary.
- **Affected artifacts:** Goal Plan / Tickets Map / status
- **Constraints or invariants:** No product code, worktree, commit, parent update, production migration, credentials, permissions or real provider call has been authorized or performed.
- **Next:** Obtain explicit implementation commit and Lead-owned direct-parent authorization, then enter specdev/I-implement.
- **Supersedes:** none

## LOG-023 - 2026-09-04T15:46:18+08:00 - Skill Gate and new-module baseline added to all planning artifacts
- **Design nodes:** D-001 through D-017
- **Round and dependencies:** post-Goal Plan / T-01 through T-13
- **Status:** confirmed
- **Question:** How should every Ticket enforce the project Skills and the minimum directory/code conventions for the new ruoyi-third module?
- **Facts and sources:** User requested explicit Skill usage per Ticket; engineering-standards requires project profile/module map/mode registration before backend changes, new business modules default to layered, and plus-ui requires domain/web-domain/App explicit composition. The current Ticket set has separate backend, SQL, frontend and integration owners.
- **Options:** Keep only high-level references in Goal Plan; add a shared Map summary; add a per-Ticket Skill Gate plus shared Map/Goal Plan baseline.
- **Recommendation:** Use the per-Ticket Skill Gate plus shared baseline, with exact project-relative Path tags and stop conditions.
- **Conclusion:** All 13 Tickets now include a “必须加载的 Skill 与工程基线” section. The Tickets Map and Goal Plan also define mandatory Skill groups, layered backend boundaries, POM/SQL/frontend package rules, tests and forbidden shortcuts.
- **Reason:** This makes directory structure, code style, dependency direction, API/SQL conventions and verification executable for each implementation context instead of relying on memory or a generic reference.
- **Affected artifacts:** Goal Plan / Tickets Map / Ticket / status
- **Constraints or invariants:** This change only updates SpecDev planning documents; no product code, Skill source, POM, SQL, frontend package, commit or authorization state was changed.
- **Next:** At I-implement dispatch, Lead verifies the Ticket Skill Gate before any writer receives a current-workspace lock.
- **Supersedes:** none
