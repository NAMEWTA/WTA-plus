---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-09
title: Outbound invocation logs and provider and endpoint statistics
status: done
planning_depth: deep
planning_depth_reason: Outbound traffic contains credentials and business payloads. Every physical attempt must be captured automatically, sanitized by a server-side blacklist, bounded in size, and separated from logical provider/endpoint outcome statistics.
ready: true
risk: high
blocked_by: [T-02, T-07, T-08]
contract_ids: [AC-011, AC-013, AC-014]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/observability/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/log/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/observability/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/log/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/controller/admin/ThirdObservabilityController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/usecase/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/service/ThirdObservabilityService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/logging/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/config/SysLogConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/gateway/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-09: Outbound invocation logs and provider and endpoint statistics

- Ticket file: `<Path>{roots.state}/specdev/changes/{change}/ticket/09-observability-and-statistics.md</Path>`
- Map: `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- Parent spec: `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- Evidence: `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>`

## 1. 战略与来源

Capture outbound HTTP automatically from the ruoyi-third pipeline and send a bounded, sanitized summary into the existing system log sink. Persist invocation details and aggregate statistics without storing complete request/response bodies in business tables. Keep provider totals and endpoint dimensions, with `attempt` representing physical sends and logical result representing the final gateway outcome.

Sources: `US-009`, `AC-011`, `AC-013`, `AC-014`, `ADR-006`, and `ADR-009`.

## 2. 决策状态

### 已锁定决策

- Capture is mandatory on the gateway path; callers do not need an annotation.
- The server-side sensitive-key blacklist cannot be disabled. Endpoint metadata may add redaction rules only.
- Summaries are capped at 16 KiB and redact authorization, API key, secret, signature, cookie, encryption, and configured sensitive fields before persistence or log emission.
- A physical attempt increments invocation count once a request is actually sent. Rejected preflight and rate-limit calls do not become HTTP attempts, but are counted in the rejected category.
- Outcome categories include success, failure, timeout, rejected, and provider business failure. 2xx alone is not business success.
- The admin query is read-only and returns sanitized VO data; it never returns raw body, credential ciphertext, or stack traces.
- Logging failures do not turn a successful external call into a business failure, but metrics and health signals must expose sink failure.

## 2.1 必须加载的 Skill 与工程基线

The implementer must load:

- `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`
- `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`
- `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`
- `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`

Read the backend architecture, implementation, security/data, controller/API, and common-web logging references. Preserve the layered chain and keep SysLog integration inside this module's adapter/log package. Do not modify common-web for this first slice, add caller annotations, or persist full payloads.

Stop if a blacklist can be bypassed, if a full payload is written to a business table, or if the recorder is optional on any outbound path.

### 已采用的低影响假设

- The existing `SysLogEventSink` contract remains the integration point for this module's outbound log events.

### 未决问题

无。

## 3. 范围边界

| In scope | Reuse without contract changes | Out of scope |
|---|---|---|
| Invocation recorder, sanitizer, aggregate counters, read-only admin query, mandatory gateway hook | Existing `SysLogEventSink`, T-02 invocation/statistic tables, T-07 pipeline, T-08 outcome categories | Independent log product, tracing platform, automatic quota blocking, payload archival |

## 4. 要构建什么

The pipeline emits a start/finish record around every physical send. The recorder receives provider code, endpoint code, request id, attempt number, elapsed time, status category, sanitized request summary, and sanitized response summary. The statistic store upserts provider and endpoint totals in the same logical result vocabulary.

## 5. 实现契约

- `ThirdInvocationRecorderPort` is called by the gateway adapter for accepted sends and preflight rejections.
- `ThirdInvocationRecorderAdapter` separates detail persistence from aggregate updates and delegates log output to `ThirdLogSanitizerAdapter`/`SysLogEventSink`.
- `ThirdObservabilityUseCase` and its controller expose query-only VOs; controllers do not access mappers directly.
- Redaction is deterministic and tested with canary secrets, nested JSON, headers, and truncation.
- The recorder never logs credentials, full URLs with query secrets, raw exception stacks, or complete raw bodies.

## 6. 执行路线

1. Freeze category, attempt, and request-id semantics with T-07/T-08.
2. Implement immutable sanitizer and size cap.
3. Hook recorder into accepted, rejected, timeout, transport, and provider-error paths.
4. Implement provider and endpoint aggregate upsert plus read-only admin query.
5. Run canary scans and persistence tests with MySQL/log sink when available.

## 7. 路径访问契约

Writable paths are limited to observability/log adapters, the observability use case/service/controller, and tests. Common-web logging/config and gateway implementation are read-only dependencies. No shared path is owned by this ticket.

## 8. 验证矩阵

| Behavior | Check | Expected result | Evidence |
|---|---|---|---|
| Accepted send | gateway integration test | detail and provider/endpoint aggregates are written with request id and actual attempts | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| Rejection | preflight/rate-limit test | rejected category is visible; no HTTP attempt is recorded | same evidence |
| Failure classes | timeout, transport, provider business error | categories and counts remain distinct | same evidence |
| Sanitization | nested canary and oversize payload scan | blacklist always redacts and output is capped at 16 KiB | same evidence |
| Query boundary | admin controller test | only sanitized VOs are returned | same evidence |

Workspace tests are required. MySQL aggregate/upsert and real SysLog sink checks are release gates and must be marked pending if the services are unavailable.

E2E disposition: required for the release gate; execution environment is `current-workspace` with MySQL 8.4 and the existing SysLog sink. No production credentials or complete payloads are permitted.

## 9. 发布、迁移与恢复

Deploy recorder and schema before enabling outbound providers. Rollback disables the gateway path or affected providers while retaining historical invocation/statistics rows. Never delete audit records as part of rollback. A category or field rename requires an additive compatibility decision and evidence update.

## 10. 验收标准

- [x] AC-011 transport success and provider business outcome are distinct in response, detail, and aggregates.
- [x] AC-013 provider and endpoint statistics preserve physical attempt count and logical result counts.
- [x] AC-014 every outbound path is automatically logged through the immutable blacklist and size cap.
- [x] No complete request/response body or credential material is stored in the business tables.
- [x] External MySQL and SysLog gate status is recorded honestly in the evidence file.
