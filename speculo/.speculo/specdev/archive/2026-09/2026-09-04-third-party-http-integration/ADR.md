## ADR-001: RestClient as the synchronous HTTP execution baseline
**Status:** accepted
**Source:** LOG-004 / user decision
**Supersedes:** none

### Context
The backend runs Spring Boot 4.1.0 and Java 21. It must support both database-defined dynamic endpoints and strongly typed provider clients. `@HttpExchange` is a source-level Java contract and is not a runtime endpoint generator.

### Decision
Use Spring `RestClient` as the unified synchronous execution layer. Source-defined provider facades may use `@HttpExchange` through `HttpServiceProxyFactory`; database-defined endpoints use generic `RestClient` request construction. Use `WebClient` only for a reactive execution path. Do not add OpenFeign in the first release.

### Trade-off
The mixed design maintains one dynamic request pipeline and one typed facade path, but both paths must share timeout, policy, logging, and statistics infrastructure.

### Consequences
Callers do not create or configure low-level HTTP clients. The module owns connection, timeout, status handling, message conversion, and execution policy.

### Verification / Migration
Verify Spring Boot 4.1 APIs in the `ruoyi-third` POM and add focused tests for dynamic requests and typed provider facades.

## ADR-002: Provider/Endpoint aggregate with explicit Java SPI
**Status:** accepted
**Source:** LOG-005 / LOG-006 / user decision
**Supersedes:** none

### Context
The reference `cde-third` only has one API configuration and an asynchronous counter; it does not model shared provider settings, endpoint lifecycle, or a unified HTTP pipeline. Future providers may need signatures, non-standard encryption, pagination, and business error conversion.

### Decision
Use a two-level Provider -> Endpoint aggregate. Provider shutdown overrides endpoint state. An endpoint is unique by `provider_code + endpoint_code`; base URL and shared authentication are inherited from Provider unless explicitly overridden. Standard method/path/query/header/body/JSON behavior is metadata-driven under a security whitelist. Special behavior is an explicit Java SPI/Spring Bean. Compose Facade, Registry/Strategy, Factory, Pipeline/Chain, and Decorator/Interceptor with one owner per concern. Forbid database scripts, SpEL, and arbitrary class-name reflection.

The safe override boundary is explicit: Endpoint metadata may replace only server-controlled shared header values through a validated `{\"headers\":{...}}` object. The Provider base URL is always the trusted origin and cannot be overridden by database metadata.

### Trade-off
This is more structured than a single API table, but keeps shared configuration, endpoint lifecycle, and provider-specific behavior replaceable and testable.

### Consequences
The module needs provider, endpoint, credential, statistics, and startup registration boundaries. Missing or conflicting provider adapters fail before an outbound request.

### Verification / Migration
Build `ruoyi-third` in the current product repository. Migrate the reference configuration only as compatibility input; validate Qichacha with multiple endpoints, shared headers/base URL, provider and endpoint statistics, and explicit SPI behavior.

## ADR-003: Ciphertext credentials with deployment-managed master key
**Status:** accepted
**Source:** LOG-009 / user decision
**Supersedes:** none

### Context
Initial operations require API keys, secrets, signing keys, or encryption keys to be maintained in the management database, but plaintext must not be persisted. The project already exposes `ruoyi-common-encrypt`, while the symmetric master key can be supplied by yml or an external deployment secret.

### Decision
Credentials may be stored in DB only as versioned ciphertext. The symmetric master key is supplied by deployment configuration and is never stored in DB or returned by management APIs. Decryption or key availability failure is fail-closed and prevents outbound traffic.

### Trade-off
Database management is convenient for the first release, but deployment owns master-key protection and rotation.

### Consequences
The implementation still needs an encrypted payload envelope, algorithm/nonce/version rules, rotation behavior, and redaction rules. Management APIs expose only non-sensitive status.

### Verification / Migration
Use the supported `ruoyi-common-encrypt` entry points and test that master keys never enter Git, responses, or logs.

## ADR-004: Relative dynamic paths and a request-target security boundary
**Status:** accepted
**Source:** LOG-010 / user decision
**Supersedes:** none

### Context
Provider base URL is connection configuration, not a security boundary. A database endpoint that accepts an absolute URL, arbitrary header, or unsafe redirect could bypass the provider target and create SSRF or cross-origin behavior.

### Decision
When a fixed Java contract only receives dynamic id, query, header, or body values, use typed `@HttpExchange`. When DB determines HTTP method, URI template, parameter structure, or response type, use generic `RestClient`; use `WebClient` for reactive execution. Provider supplies a trusted base URL. A DB endpoint may supply only a validated relative path and whitelisted parameters. Reject absolute URLs, `//host`, path traversal, scripts/expressions, arbitrary headers, and cross-origin redirects before send.

### Trade-off
Dynamic endpoints cannot express arbitrary protocols, but the target is auditable and provider base URL cannot be bypassed.

### Consequences
Normalize and validate the target before HTTP send; declare allowed parameter locations and headers; enforce same-origin redirect policy.

### Verification / Migration
Test absolute/protocol-relative URLs, `..` traversal, expression payloads, unknown headers, and cross-origin redirects. Verify typed and dynamic paths share the same executor policy.

## ADR-005: Restricted ownership for the extension pattern composition
**Status:** accepted
**Source:** LOG-011 / user decision
**Supersedes:** none

### Context
Provider count and endpoint count will grow. A single gateway with provider-specific branches would mix orchestration, client creation, lifecycle processing, and cross-cutting behavior.

### Decision
Facade only orchestrates the public call. Registry/Strategy only selects an adapter by `providerCode`. Factory only creates a configured HTTP client. Pipeline/Chain only handles request lifecycle. Decorator/Interceptor only handles cross-cutting concerns such as logging, statistics, and rate limits. Provider adapters are explicit Spring Beans/SPI registered by unique `providerCode`; startup validates conflicts and missing required registrations.

### Trade-off
Adding a provider requires a small explicit adapter contract, but prevents reflection-driven behavior and keeps each concern testable.

### Consequences
No provider may silently create its own client or bypass the common pipeline. Registration failure is a startup/configuration failure.

### Verification / Migration
Test owner boundaries, registry conflicts, pipeline ordering, and adapter behavior independently.

## ADR-006: DB fact source with Redis/Redisson runtime snapshots
**Status:** accepted
**Source:** LOG-012 / user decision
**Supersedes:** none

### Context
Provider and endpoint switches, inherited settings, and credentials are changed by management operations. Cluster instances need prompt invalidation without making every call a DB query.

### Decision
DB is the fact source. Redis/Redisson is the second-level runtime snapshot cache. A successful provider or endpoint save immediately clears the corresponding Redis key and publishes an invalidation notification; the next request loads a new snapshot. If switch state, configuration, or credentials cannot be confirmed, fail-closed and never send using an expired unknown snapshot.

### Trade-off
Changes cause a cache miss and notification traffic, and temporary DB/Redis uncertainty can reject calls, but shutdown and credential changes cannot be masked by stale state.

### Consequences
Define key names, snapshot versions, invalidation messages, and map read failures to `CONFIG_UNAVAILABLE`.

### Verification / Migration
Test save-and-invalidate, multi-instance notification, read failures, and concurrent version changes.

## ADR-007: Provider and endpoint rate limits reject before send
**Status:** accepted
**Source:** LOG-014 / user decision
**Supersedes:** none

### Context
Provider totals must cover all child endpoints, while an endpoint may have a stricter rate or concurrency limit. Continuing after a limiter failure would bypass operations controls; allowing traffic during Redis failure is unsafe.

### Decision
Configure provider and endpoint limits, with endpoint limits bounded by the provider limit. Rate-limit, concurrency, disabled-state, and unknown-configuration decisions happen before HTTP send. Return stable non-secret codes: `PROVIDER_DISABLED`, `ENDPOINT_DISABLED`, `RATE_LIMITED`, and `CONFIG_UNAVAILABLE`. Quota is display-only in the first release and does not auto-block.

### Trade-off
Redis or configuration uncertainty sacrifices availability by rejecting calls, preserving fail-closed behavior and provider-wide bounds.

### Consequences
The limiter is a pre-send pipeline owner. Statistics distinguish rejected calls from attempted HTTP calls.

### Verification / Migration
Test provider and endpoint keys, provider-bound constraints, Redis failure with zero outbound calls, and stable error mapping.

## ADR-008: Scoped ciphertext credentials with rotation-ready envelope
**Status:** accepted
**Source:** LOG-015 / user decision
**Supersedes:** none

### Context
Provider credentials are shared by default, while an endpoint may need a credential override. The first release needs ciphertext at rest without introducing online key-rotation complexity.

### Decision
Model provider and endpoint credentials separately. An endpoint credential overrides the same credential type at provider scope. Store a versioned authenticated-encryption envelope containing algorithm, nonce, key version, and ciphertext; use AES-GCM or an equivalent authenticated cipher. Read the master key only from deployment yml/external secret configuration. Keep rotation fields, but implement only one active version in the first release.

### Trade-off
Scoped records limit blast radius and leave a rotation path, while postponing dual-key runtime coordination.

### Consequences
Unsupported version, missing master key, or decryption failure returns `CONFIG_UNAVAILABLE` and cannot reach the HTTP executor.

### Verification / Migration
Test provider fallback, endpoint override, authenticated decryption, version validation, and no-secret logging.

## ADR-009: Constrained metadata request and response formats
**Status:** accepted
**Source:** LOG-016 / user decision
**Supersedes:** none

### Context
Database-defined endpoints need useful flexibility without becoming a template or scripting engine.

### Decision
The first release supports JSON, query, and form requests and JSON, text, and bytes responses. Endpoint metadata declares parameter name, location, type, requiredness, and allowed headers. JSON bodies are structured and versioned with explicit variable allowlists. Multipart, streaming upload, and complex templates require a later explicit Java SPI.

### Trade-off
Some supplier protocols need a later adapter, but standard endpoints remain schema-validated and safe to review.

### Consequences
No arbitrary URL/header, JSONPath, SpEL, or DB script is accepted. `@HttpExchange` typed facades remain available for source-defined contracts.

### Verification / Migration
Test schema validation, type conversion, body variable allowlists, content negotiation, and rejection of unsupported content types.

## ADR-010: Restricted dynamic values and explicit provider error mapping
**Status:** accepted
**Source:** LOG-017 / user decision
**Supersedes:** none

### Context
Dynamic endpoint responses have no compile-time Java type, while typed provider facades should retain type safety. HTTP 2xx does not necessarily mean provider business success.

### Decision
Dynamic calls return `JsonNode` or another restricted value representation. Typed facades may request `Class<T>` or `ParameterizedTypeReference<T>`. `ThirdPartyResponse<T>` carries requestId, provider/endpoint, HTTP status, sanitized headers, provider business code, typed/restricted data or summary, and duration. A 2xx status means transport success only; explicit adapters map provider business failures to `PROVIDER`. Complete raw bodies never enter business responses or statistics tables.

### Trade-off
Callers must use a typed facade or parse a constrained value for dynamic endpoints, but the public contract remains stable and avoids leaking supplier-specific raw payloads.

### Consequences
Transport, timeout, HTTP, provider, decode, configuration, and pre-send rejection remain distinguishable while preserving a controlled cause correlation in logs.

### Verification / Migration
Test typed conversion, dynamic JSON/text/bytes handling, non-2xx classification, 2xx business failure mapping, and raw-body exclusion.

## ADR-011: Mandatory outbound logging through ruoyi-third executor
**Status:** accepted
**Source:** LOG-018 / user decision
**Supersedes:** none

### Context
`ruoyi-common-web` currently captures inbound Servlet events. Outbound `RestClient` calls are not automatically captured, and typed facades must not bypass sensitive-data controls.

### Decision
All ruoyi-third outbound HTTP calls are automatically captured by the shared executor/interceptor/decorator path. Reuse public `SysLogEventWriter`, `SysLogEventSink`, and the `org.dromara.system.http` logger contract. Keep outbound capture, body truncation, and redaction composition inside ruoyi-third. Do not add a business-side annotation or a separate log scenario in the first release.

### Trade-off
The module owns a small outbound logging adapter, but every call receives consistent correlation, size, and sensitive-field controls.

### Consequences
"Full logging" means records go to the system-log pipeline instead of business tables; it never disables the server-side sensitive header/field blacklist, truncation, or endpoint-added redaction rules.

### Verification / Migration
Test dynamic and typed paths, success/failure/timeout/rejected events, correlation IDs, truncation, blacklist enforcement, and writer/sink reuse.

## ADR-012: Independent third frontend domain under System Management navigation
**Status:** accepted
**Source:** LOG-019 / user decision and repository conventions
**Supersedes:** none

### Context
The administrator expects a `三方接口管理` menu below System Management. Source ownership is determined by the backend business owner, not by where a menu is displayed.

### Decision
Create independent frontend packages `domains/third` and `web-domains/third`, and explicitly compose both in Admin Web. Add the `三方接口管理` directory below the existing System Management menu. The first-release child pages are provider management, endpoint management, sanitized invocation records, and aggregate statistics. Credential and policy editing remain within provider/endpoint workflows rather than exposing a standalone credential list.

### Trade-off
Admin Web gains another explicitly selected domain and manifest, but `system` packages remain free of third-party integration behavior.

### Consequences
Backend permission strings, MySQL menu DML, domain metadata, web-domain component keys, package exports, Admin service composition, and manifest registration must remain synchronized.

### Verification / Migration
Validate menu hierarchy, permission-denied behavior, unknown component failure closure, all four page registrations, package architecture tests, and Admin E2E navigation.
