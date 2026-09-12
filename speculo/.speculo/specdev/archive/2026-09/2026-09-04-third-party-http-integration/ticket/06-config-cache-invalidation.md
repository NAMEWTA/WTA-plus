---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-06
title: Provider and endpoint configuration snapshots with Redis invalidation
status: done
planning_depth: deep
planning_depth_reason: Configuration, enabled state, and credential snapshots control whether an outbound request is allowed. Unknown cache state must fail closed and never reuse an unverified stale snapshot.
ready: true
risk: high
blocked_by: [T-03, T-04, T-05]
contract_ids: [AC-003, AC-004, AC-005, AC-010]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/port/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/store/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/support/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/port/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/adapter/store/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/support/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/dao/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-06: Provider and endpoint configuration snapshots with Redis invalidation

- Ticket file: `<Path>{roots.state}/specdev/changes/{change}/ticket/06-config-cache-invalidation.md</Path>`
- Map: `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- Parent spec: `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- Evidence: `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>`

## 1. 战略与来源

DB is the fact source. The module builds a provider/endpoint/credential snapshot, stores a versioned copy in the `third:config:` Redis namespace, and invalidates affected keys after a successful management write. A request reads a fresh snapshot; uncertain configuration, cache, or credential state returns `CONFIG_UNAVAILABLE` and sends zero HTTP requests.

Sources: `US-002`, `US-003`, `US-004`, `AC-003`, `AC-004`, `AC-005`, `AC-010`, `ADR-002`, and `ADR-003`.

## 2. 决策状态

### 已锁定决策

- Provider enabled state dominates every endpoint enabled state.
- Provider and endpoint values merge through explicit inheritance rules. Endpoint values may override only declared fields and cannot widen provider security limits.
- Successful provider, endpoint, or credential writes evict the affected provider and endpoint keys and publish an invalidation event. The next request loads DB data.
- A missing, unreadable, or version-mismatched Redis value is not an invitation to use an old value. The result is `CONFIG_UNAVAILABLE`.
- Cache keys contain stable provider/endpoint codes only; they never contain credentials, request values, or raw URLs.
- The cache adapter owns Redis mechanics. The pipeline consumes `ThirdConfigSnapshotPort` and does not import a DAO or `RedisUtils` directly.

## 2.1 必须加载的 Skill 与工程基线

The implementer must load and apply all of these before changing files:

- `<Path>.agents/skills/engineering-standards/SKILL.md</Path>`
- `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>`
- `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>`
- `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>`

Required references include the project profile/module map/backend module modes, backend architecture and implementation references, and the common Redis utility index. The new module remains `layered`: `controller -> usecase -> service -> dao -> mapper -> XML`; runtime cache access is isolated in `port`, `adapter`, and pure `support`. Do not add a second cache abstraction, database script, SpEL, arbitrary reflection, or direct controller-to-DAO calls.

Stop this ticket if a Skill has not been read, an unknown snapshot is treated as usable, or a cache implementation leaks credentials into keys/logs.

### 已采用的低影响假设

- The existing Redis/Redisson primitives and MySQL transaction convention remain available in both supported application bundles.

### 未决问题

无。

## 3. 范围边界

| In scope | Reuse without contract changes | Out of scope |
|---|---|---|
| Snapshot port, Redis cache adapter, version/fingerprint checks, invalidation event, fail-closed result | T-02/T-03/T-04/T-05 stores and transactions; project Redis/Redisson primitives | Distributed config service, stale-while-revalidate, automatic recovery, quota blocking |

## 4. 要构建什么

The read sequence is `DB fact source -> validated snapshot -> Redis read/write -> immutable snapshot returned to gateway`. A write sequence is `transactional DB update -> immediate targeted eviction -> invalidation publication`. Provider changes invalidate provider and all endpoint/credential descendants; an endpoint change invalidates only that endpoint and its effective provider relationship.

## 5. 实现契约

- Port: `ThirdConfigSnapshotPort` and supporting store ports define immutable data consumed by the gateway.
- Adapter: `ThirdConfigCacheAdapter` owns Redis serialization, namespace, TTL, invalidation, and fail-closed mapping.
- Support: `ThirdEndpointSecurity` performs the second validation at request time; it does not read from Redis.
- No entity, mapper, Redis client, or ciphertext is exposed through `ruoyi-api`.
- Management transactions use the existing project transaction convention; invalidation occurs only after a committed write.

## 6. 执行路线

1. Define snapshot shape, version, and cache key rules.
2. Implement the cache adapter and invalidation publisher behind ports.
3. Connect provider/endpoint/credential management writes to targeted eviction.
4. Add tests for hit, miss, version mismatch, Redis failure, provider precedence, and post-save refresh.
5. Run module-mode validation and Maven tests before handing off to T-07.

## 7. 路径访问契约

- Writable paths are restricted to the frontmatter port/adapter/store/support and test paths.
- DAO, Mapper/XML, API contracts, and common Redis code are read-only dependencies.
- No shared path is owned by this ticket; changes to schema or management services belong to T-02 through T-05.

## 8. 验证矩阵

| Behavior | Check | Expected result | Evidence |
|---|---|---|---|
| Normal cache path | two-node Redis test or local adapter test | fresh provider/endpoint snapshot is returned | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| Post-save invalidation | provider, endpoint, and credential writes | targeted keys are evicted and next read observes new data | same evidence |
| Unknown state | Redis unavailable, corrupt value, version mismatch | `CONFIG_UNAVAILABLE`; zero HTTP requests | same evidence |
| Security | key/log/canary scan | no credential or full URL in cache keys or logs | same evidence |

Workspace checks are required locally. Redis multi-instance and MySQL transaction checks remain release gates when those services are available; they must be recorded as pending when unavailable, never simulated.

E2E disposition: required for the release gate; execution environment is `current-workspace` with MySQL 8.4 and multi-instance Redis. If services are unavailable, record pending rather than simulating.

## 9. 发布、迁移与恢复

Deploy code and namespace support before enabling a provider. Rollback disables affected providers/endpoints and keeps historical DB records; it does not delete audit/statistics data. Cache entries may be safely discarded because DB is authoritative. A change to key format requires a version bump and an explicit compatibility decision.

## 10. 验收标准

- [x] AC-003 inheritance and provider-first enablement are enforced from a single immutable snapshot.
- [x] AC-004 successful writes immediately invalidate affected Redis keys and publish the event.
- [x] AC-005 credentials are represented only by the encrypted credential contract and never by cache plaintext.
- [x] AC-010 unknown Redis/configuration state fails closed before the HTTP client is reached.
- [x] Tests and evidence identify any unavailable external Redis/MySQL gate.
