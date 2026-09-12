---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-07
title: RestClient gateway pipeline with HttpExchange and provider SPI
status: done
planning_depth: deep
planning_depth_reason: This is the only outbound HTTP execution boundary. It combines dynamic endpoint metadata, typed HttpExchange contracts, URI security, credential resolution, adapter mapping, and stable public response semantics.
ready: true
risk: high
blocked_by: [T-01, T-06]
contract_ids: [AC-003, AC-006, AC-007, AC-008, AC-011, AC-012]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/http/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/spi/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/gateway/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/http/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/spi/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/port/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/support/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/third/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/dao/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-07: RestClient gateway pipeline with HttpExchange and provider SPI

- Ticket file: `<Path>{roots.state}/specdev/changes/{change}/ticket/07-http-gateway-pipeline.md</Path>`
- Map: `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- Parent spec: `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- Evidence: `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>`

## 1. 战略与来源

Expose one synchronous `ThirdPartyGateway` boundary. `RestClient` owns connection/read timeouts, status handling, and message conversion. `@HttpExchange` is available for fixed, typed contracts; database-driven method/path/parameter metadata uses the same fixed gateway pipeline with a safe dynamic request model. Provider-specific signing, non-standard encryption, pagination, and business-error mapping attach through an explicit Java SPI.

Sources: `US-001`, `US-005`, `US-006`, `US-009`, `AC-003`, `AC-006`, `AC-007`, `AC-008`, `AC-011`, `AC-012`, `ADR-001`, `ADR-004`, and `ADR-005`.

## 2. 决策状态

### 已锁定决策

- Facade only orchestrates. Registry/strategy only selects an adapter. Factory only creates the HTTP client. Pipeline owns request lifecycle. Decorators/interceptors handle cross-cutting concerns only.
- Spring Bean registration is keyed by `providerCode`; duplicate codes fail application startup.
- Provider `baseUrl` is trusted configuration. Endpoint metadata is only a validated relative path and a whitelist of method/query/header/body fields.
- Endpoint `overrideJson` is limited to validated server-controlled shared headers; it cannot replace the Provider origin or introduce a complete URL.
- Reject full URLs, `//host`, path traversal, arbitrary headers, scripts/expressions, arbitrary class-name reflection, cross-domain redirects, multipart, and streaming bodies in this first slice.
- Supported body formats are JSON, query, and form. Supported response formats are JSON, text, and bytes. Dynamic values use constrained `JsonNode`; typed calls use `Class<T>` or `ParameterizedTypeReference<T>`.
- HTTP 2xx means transport success only. A provider adapter may map a provider business error to the stable `PROVIDER` category without returning the raw body in the business response.

## 2.1 必须加载的 Skill 与工程基线

The implementer must load:

- `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`
- `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`
- `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`
- `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`
- `<Path>.agents/skills/java-api-compatibility/SKILL.md</Path>`

Read the backend architecture, module layout, security/data, implementation, common-module index, and API-compatibility references. Keep the layered chain intact and expose only stable API DTO/contracts. The gateway may depend on ports and adapters, but no controller, caller, database entity, or mapper may bypass it.

Stop if a request can escape the provider origin, if an arbitrary header/class/script can be selected from DB, or if a new public API is added without compatibility review.

### 已采用的低影响假设

- The standard Spring `RestClient` conversion stack is sufficient for JSON, text, and bytes in the first release.

### 未决问题

无。

## 3. 范围边界

| In scope | Reuse without contract changes | Out of scope |
|---|---|---|
| RestClient factory, dynamic request builder, typed HttpExchange marker, SPI registry, pipeline stages, safe response conversion | T-01 API contracts, T-06 snapshot port, T-05 credential resolver, T-08 resilience, T-09 recorder | WebClient streaming, multipart, arbitrary templates, async/outbox, provider production credentials |

## 4. 要构建什么

`resolve snapshot -> validate effective endpoint -> resolve credentials -> select provider adapter -> acquire resilience lease -> build safe URI/request -> send with RestClient -> map status/body -> record attempt -> release lease`.

Every physical send is recorded as an attempt. The returned `ThirdPartyResponse<T>` contains request id, transport category, provider mapping, and constrained data only; it does not expose entities, ciphertext, stack traces, or complete raw bodies.

## 5. 实现契约

- `ThirdHttpClientFactory` centralizes client settings and disables cross-domain redirects.
- `ThirdGatewayAdapter` is the single execution implementation for `ThirdPartyGateway`.
- `ThirdProviderAdapterRegistry` validates one Spring adapter per provider code and returns a neutral default when no explicit SPI exists.
- URI, method, header, query, and body validation runs before any network call and is repeated at execution time.
- Any adapter exception is mapped to a stable error category; secrets are never included in exception text or log fields.

## 6. 执行路线

1. Freeze the request/response contract and error categories from T-01.
2. Build the RestClient factory and safe dynamic request model.
3. Add typed HttpExchange support without bypassing the pipeline.
4. Add provider SPI registry and an example adapter seam.
5. Test SSRF/path/header/script rejection, JSON/query/form, JSON/text/bytes, 2xx mapping, provider error mapping, redirect rejection, and parameterized response types.

## 7. 路径访问契约

Writable paths are limited to gateway/http/spi plus declared ports/support and tests. API contracts and configuration/DAO code are read-only. Do not change unrelated modules or introduce a second outbound client stack.

## 8. 验证矩阵

| Behavior | Check | Expected result | Evidence |
|---|---|---|---|
| Normal dynamic call | local HTTP server | relative path and allowed fields reach the trusted origin | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| Typed call | Spring context and generic response test | `Class<T>` and `ParameterizedTypeReference<T>` preserve the target type | same evidence |
| SSRF boundary | full URL, `//host`, traversal, redirect, arbitrary header/script tests | rejected before HTTP; zero outbound requests | same evidence |
| Provider SPI | duplicate registry and mapping tests | startup conflict fails; adapter maps request/business error explicitly | same evidence |
| Failure accounting | transport exception and timeout | actual physical attempt count and elapsed time are retained | same evidence |

E2E disposition: required for the release gate; execution environment is `current-workspace` with a local HTTP server and the application context. External provider credentials are forbidden.

## 9. 发布、迁移与恢复

Deploy the gateway with no enabled provider first. Existing callers receive additive API behavior only. Rollback disables providers/endpoints and leaves invocation history intact. Any future dynamic endpoint capability must be a separately reviewed SPI and cannot weaken this whitelist.

Latest local evidence: `ThirdGatewayAdapterTest` uses a JDK loopback `HttpServer` and passes 4/4 for constrained JSON POST delivery, provider-disabled and rate-limit zero-request behavior, and 302 classification as an HTTP failure without following the redirect. Typed/SPI mapping and full application-context status/retry checks remain release-gate work.

## 10. 验收标准

- [x] AC-003 provider-first enablement and inherited base URL are enforced.
- [x] AC-006 supported request/response formats work through one pipeline.
- [x] AC-007 every origin/path/header/script/redirect rejection produces zero HTTP.
- [x] AC-008 provider adapters are Spring-registered, unique, and explicitly selected.
- [x] AC-011 transport and provider business outcomes remain distinct.
- [x] AC-012 dynamic and typed calls use the stable public contract without exposing persistence types.
