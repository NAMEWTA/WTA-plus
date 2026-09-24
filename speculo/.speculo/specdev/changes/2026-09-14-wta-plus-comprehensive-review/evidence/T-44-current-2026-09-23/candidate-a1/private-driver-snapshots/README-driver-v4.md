# T44 private full-app core and scheduled fallback driver

`run-full-app-core-fallback-v4.py` is a private, owned-environment acceptance
candidate. It has not been run against Docker, Maven, Java, HTTP or production.
The completed offline checks cover AST/helper/stage safety, private config
construction, allowlisted ACL log output and startup readiness transitions.
It imports the reviewed T40 browser runner solely for source, package, SQL,
Docker ownership and cleanup helpers. Its default `--preflight` checks file
presence without launching any service. Lead alone may execute it after a
clean source commit, full package and proof are fixed.

Execution needs an exact 40-digit clean HEAD, exact 64-digit full JAR SHA-256,
and the matching private package-proof JSON. Each invocation creates new
MySQL 8.4.9 and Redis 8.6.3 containers under `namewta.test.owner=T-44-FULL-APP`
plus a random run label. It imports the six SQL files into only its owned
database, grants only the owned app user, rewrites **every** OSS config row to
a synthetic credential and counted/closed loopback endpoint, and starts the
full JAR with a 0600 isolated dev overlay. No supplier credentials or vendor
network are involved. Source/JAR before and after, full container IDs,
anonymous volumes, process groups and loopback ports are acceptance gates.

For all five scenarios, `readiness` and `liveness` must be HTTP 200/UP, a real
Admin login and nonempty menu must work, and the OSS loopback probe must have
zero connections **before** an explicitly requested administrator diagnosis.
The `ossdiagnostics` group is checked against the HTTP/state values supplied
by Lead for that scenario. Health paths are fixed to:

```
/actuator/health/readiness
/actuator/health/liveness
/actuator/health/ossdiagnostics
```

Scenarios:

- `empty-config`: delete all owned OSS config rows, seed stale physical Redis
  default pointer and `sys_oss_config` map, then require startup to clear both.
- `duplicate-default`: make the owned `image` row a second valid PRIVATE
  default. The old pointer and stale map field must clear; valid config entries
  may repopulate the map, but the default pointer must remain absent.
- `bad-default`: make only the owned default `minio` row's access policy invalid;
  the valid nondefault `image` row can repopulate the map, but no default pointer
  may be selected.

  In all three unusable-default scenarios, a real `/resource/oss/uploads`
  request with valid owned metadata must return `STORAGE_NOT_SERVING` without
  contacting the OSS probe.
- `bad-nondefault`: corrupt only the owned nondefault `image` row's access
  policy; the full app and core remain available.
- `invalid-diagnostic`: pass exactly
  `--invalid-diagnostic-key oss.readiness.diagnostic-timeout` and
  `--invalid-diagnostic-value invalid-owned-duration`; the app must keep core
  paths available without an automatic remote probe. After these checks, a
  mandatory explicit `POST /resource/oss/config/diagnose/{config_id}` must
  return only `status`, `reason=DIAGNOSTIC_CONFIG_INVALID`, `checkedAt`; then
  `ossdiagnostics` must expose that reason, still with zero remote connections.
- `minio-offline`: reserve a closed loopback port instead of a MinIO service;
  no upload/download is attempted by this driver.
- `redis-publish-denied`: start Redis with normal permissions; prove subscribed
  wake channel, cache SET/GET, startup and menu first. Then owned Redis admin
  denies `PUBLISH` on the Notify wake channel while allowing the separate
  `namewta:cache:invalidation:v1` channel through a Redis ACL selector. The
  driver checks `ACL DRYRUN` on both channels, plus `SUBSCRIBE`, SET/GET and
  PING, then re-enters the real application login/menu
  path. It submits a real IN_APP Notice, observes the
  PUBLISH denial and same Outbox's failed wake log, sees READY before the next
  scheduled tick, then requires `POLL` to claim it and exactly one attempt,
  message and recipient relation. Later empty ticks must not duplicate delivery.
  Snail Job stays disabled and no real SMS/MAIL provider is called.

An optional exact `POST /resource/oss/config/diagnose/{config_id}` hook may be
requested for `bad-nondefault` or `minio-offline`, with explicit expected
HTTP/business codes. It is mandatory for `invalid-diagnostic`.
Its proxy connection count is recorded separately before and after the call.
The bounded call must finish within 20 seconds; this measures the whole HTTP
exchange and does not itself prove the internal per-step timeout.
An optional `/v3/api-docs` hook saves the raw HTTP bytes privately and reports
only SHA-256/path/schema counts; it does **not** validate the full OpenAPI
contract or generate public snapshots.

Example invocation shape, to fill only after source and full proof are fixed:

```
python3 /tmp/wta-t44/run-full-app-core-fallback-v4.py --execute \
  --scenario redis-publish-denied \
  --expected-head <fixed-clean-head> \
  --expected-jar-sha256 <full-jar-sha256> \
  --package-proof <private-full-package-proof.json> \
  --core-health-path /actuator/health/readiness \
  --liveness-health-path /actuator/health/liveness \
  --oss-health-path /actuator/health/ossdiagnostics \
  --oss-health-http <200-or-503> --oss-health-state <UP-or-DOWN-or-UNKNOWN>
```

All run artifacts are private under `/tmp/wta-t44/runs/<random>/` with a
0700 directory. The result contains allowlisted counts and states only;
raw backend/initializer logs remain 0600 for Lead's private diagnosis. Never
copy those logs, HTTP bodies, Redis ACL client info or the overlay into
committable evidence. A failure is not acceptance even when cleanup succeeds.
The failure summary records only a fixed phase/type, this runner's numeric
source line and, for owned HTTP/SQL controls, their existing sanitized
stage/kind/status or SQL error number. It omits exception messages and stacks.

The first v2 fallback attempt reached the second real login after an overly
broad global `-publish` and failed in cache invalidation before Notice publish.
That result is preserved separately; it is not fallback product evidence.
The v3 attempt reached real Notice publish and its same-Outbox failed wake,
but stopped at ACL LOG classification. Redis can report a denied Pub/Sub
channel as `reason=channel` with the channel in `object`; v4 accepts exactly
that default-user wake channel alongside the existing default-user
`reason=command/object=PUBLISH` form. Other ACL events cannot satisfy the
gate. Unexpected logs yield only bounded category counts, never client info.

Before execution, review the candidate T44 source against these interfaces:
the health group status expected for each scenario, diagnostic endpoint
business code, Notice API fields, scheduled Worker log signature, and cache
startup clearing. In particular, the stale `sys_oss_config` Redis hash is
intentionally a disposable **owned** preseed: if current Redisson cache
initialization rejects the synthetic entry before clearing it, replace this
fixture with a verified two-start valid-cache setup in a new private driver
version. Do not weaken the startup or cleanup gates to accommodate it.

Suggested offline-only checks before Lead's first execution: AST parse;
resolve every imported helper name and SQL stage; reject empty optional
secret in the argv guard; assert all scenario/health argument combinations;
simulate RESP2 parser and ACL-log sanitization in memory; exercise cleanup
decision logic with fake full container IDs, volumes, ports and process groups.
These suggestions are **not** recorded as completed tests.
