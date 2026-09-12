# Third-party HTTP integration (change-local)

**Provider aggregate**: the managed external HTTP supplier. It owns a trusted base URL, shared authentication defaults, lifecycle state, static policy defaults, and provider-wide statistics. Do not call a single URI configuration a provider.

**Endpoint**: one managed external URI under a provider. It is independently switchable and addressable by the unique pair `provider_code + endpoint_code`. It inherits provider defaults and may declare constrained overrides.

**Unified gateway**: the public `ThirdPartyGateway` contract plus `ThirdPartyRequest` and `ThirdPartyResponse`. Business modules pass provider/endpoint codes and constrained values; they do not depend on DB entities, provider clients, or `RestClient` internals.

**Metadata transport**: whitelisted endpoint metadata produces standard method/path/query/header/body and JSON behavior. It cannot execute DB scripts, SpEL, arbitrary expressions, or arbitrary class-name reflection.

**Provider adapter SPI**: an explicit Spring Bean/Java SPI keyed by unique `providerCode`, used only for behavior that metadata cannot safely express, such as signing, non-standard encryption, pagination, business error-code mapping, or typed response adaptation.

**Ciphertext credential**: API keys, secrets, signing keys, and encryption keys stored in DB only as ciphertext. The symmetric master key is supplied by deployment yml/external configuration, never by DB or a management response. Unknown or undecryptable credentials fail closed.

**HTTP client boundary**: use `RestClient` for synchronous runtime-defined endpoints and as the execution base for typed `@HttpExchange` facades. Use `WebClient` only for reactive execution. A fixed Java contract with dynamic values remains `@HttpExchange`; a DB-defined method, URI template, parameter structure, or response type uses generic `RestClient`.

**Request-target boundary**: Provider owns the trusted base URL. A DB endpoint contributes only a validated relative path and whitelisted parameters. Reject absolute URLs, `//host`, path traversal, scripts/expressions, arbitrary headers, and cross-origin redirects before send.

**Pattern ownership**: Facade orchestrates; Registry/Strategy selects an adapter; Factory creates the client; Pipeline/Chain owns request lifecycle; Decorator/Interceptor owns cross-cutting logging, statistics, and limiting. No owner may bypass the common executor.

**Configuration snapshot**: DB is the fact source. Redis/Redisson is the second-level cache. Successful provider/endpoint changes clear the relevant key and publish invalidation; the next request loads a new snapshot. If state, configuration, credentials, DB, or Redis cannot be confirmed, return `CONFIG_UNAVAILABLE` and do not use an expired unknown snapshot.

**Statistics and retention**: count one request when an outbound HTTP attempt is actually sent. Keep provider aggregates and endpoint dimensions, split success/failure/timeout/rejected, and persist only sanitized details. Full request/response bodies are not business-table data; outbound structured logging is a separate decision owned by D-016. Quota is display-only in the first release. Server-side sensitive header/field blacklists cannot be disabled; an endpoint may only add redaction rules.

**Rate-limit semantics**: provider and endpoint limits are both supported; endpoint settings cannot exceed provider settings. Limiting or concurrency rejection happens before HTTP send and returns `RATE_LIMITED`. Disabled or unknown states return stable non-secret codes `PROVIDER_DISABLED`, `ENDPOINT_DISABLED`, or `CONFIG_UNAVAILABLE`.

**Initial scope**: synchronous, type-safe public contract; bounded retry only for explicitly idempotent endpoints; mandatory connect/read timeouts and failure classification; Redisson-based limiting; no async delivery, batch task, outbox, automatic unlimited fallback, or default circuit-breaker dependency.

**Repository ownership**: implement in the current product repository under `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third`. The external `cde-standard/cde-third` tree is reference material only and is not modified by this change.

**Credential scope**: provider and endpoint credentials are separate records. An endpoint credential overrides the same provider credential type. The encrypted envelope is versioned and rotation-ready; the first release has one active version and does not coordinate online dual-key rotation.

**Dynamic format scope**: first-release metadata supports JSON/query/form requests and JSON/text/bytes responses. JSON bodies are structured, versioned, and populated only from explicit variable allowlists. Multipart, streaming uploads, and complex templates are explicit-SPI work for a later release.

**Response typing**: dynamic endpoints return a restricted `JsonNode`/value representation. Source-defined typed facades use `Class<T>` or `ParameterizedTypeReference<T>`. HTTP 2xx is transport success only; provider business failures are mapped by explicit adapters to `PROVIDER`. Complete raw bodies stay out of business responses and statistics tables.

**Outbound logging**: every ruoyi-third request, including `@HttpExchange` facade calls, goes through the common executor/interceptor/decorator path. The module reuses public `SysLogEventWriter`/`SysLogEventSink` and the `org.dromara.system.http` logger, while enforcing outbound truncation and redaction locally. No opt-in annotation or independent logging scenario is in the first release. Full payload visibility never overrides the server-side blacklist or size limit.

**Administration navigation**: Admin Web shows a `三方接口管理` directory under the existing System Management menu. Navigation placement does not transfer source ownership to `system`; implementation belongs to independent `domains/third` and `web-domains/third` packages selected by Admin Web.

**Administration pages**: the first release exposes Provider management, Endpoint management, sanitized invocation records, and aggregate statistics. Credential and transport-policy editing are scoped actions inside Provider or Endpoint management, not a standalone page that lists decrypted secrets.
