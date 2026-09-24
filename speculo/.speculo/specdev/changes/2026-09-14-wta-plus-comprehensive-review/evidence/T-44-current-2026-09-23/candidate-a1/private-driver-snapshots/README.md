# T44 owned MinIO gate — prepared, not executed

`run-minio-real.py` is an inert, exact-source driver. Only Lead runs it after the
T44 candidate and full Admin JAR are frozen. Preparation touched `/tmp/wta-t44`
only. No Docker container, JVM, Maven build or HTTP service was started while
preparing this file.

## Inputs and command

The driver pins tracked `frontend/e2e/run-notice-retraction-real.py` at SHA256
`7afb43db7d0086aeda0778389ad2fba95252dddf221eec406024101662c3bf46`.
It reuses that runner's exact-clean source/JAR proof, pinned images, six-SQL
initializer, owned labels, resource cleanup, login and bundle validation.
The copied local `mc` binary is `/tmp/wta-t44/mc`, mode 0700, SHA256
`01f866e9c5f9b87c2b09116fa5d7c06695b106242d829a8bb32990c00312e891`;
its original was `/srv/ops/minio-main/scripts/mc`. No production mc alias or
configuration is read. `mc alias import` uses private, run-specific 0700
configuration directories and 0600 JSON credentials. No credential appears in
argv or the public result.

Static preflight, with no services:

```sh
python3 /tmp/wta-t44/run-minio-real.py --preflight
```

Only after Lead has created a clean full-JAR package proof matching the frozen
commit, run serially with no concurrent repo writer/build:

```sh
python3 /tmp/wta-t44/run-minio-real.py --execute \
  --expected-head '<frozen-full-commit-sha>' \
  --expected-jar-sha256 '<fresh-full-jar-sha256>' \
  --package-proof '<private-full-package-proof-json>'
```

The proof uses the T40 full clean package schema. `result.json` and a private
`openapi.raw.json` remain under a new 0700 `/tmp/wta-t44/runs/<id>/` directory.
All env files, alias files/config, credential policy, overlay, raw JVM log and
temporary payloads are removed in `finally`; only the sanitized result and
OpenAPI raw capture remain. Root must retain/compare the latter privately and
run formal API generation/check separately.

## Real acceptance scope

The driver creates dual-labelled, owned loopback MySQL 8.4.9, Redis 8.6.3 and
pinned MinIO containers, applies exactly six SQL files and verifies 103 tables.
It creates three fresh synthetic buckets. The app uses a newly generated MinIO
access key limited to object read/write and minimal bucket reads in those
buckets; bucket policy and object ACL writes are denied on owned targets. A
separate root alias only bootstraps the synthetic buckets and public policy.

The app's MinIO endpoint is an in-process loopback counting proxy. It preserves
the original Host and counts every inbound request, including failures, before
forwarding. The root setup uses the direct MinIO port. The driver first waits
for production readiness to report `UP` with DB, Redis and readinessState UP,
then requires the app startup count to be zero. All example provider endpoints
in the owned DB also point at this proxy, so their unexpected startup calls
would be counted. The proxy only reports method/status and known bucket labels; it
never writes URLs, signatures, paths or headers to its result.

The gate checks real Admin login and an isolated ordinary user with a narrow
owned inbox-only role; the latter cannot call OSS download or the diagnostic
controller. It tests A PRIVATE upload init→signed PUT→complete→app-credential
HEAD→download URL→GET; an image/P PUBLIC_READ object with no diagnostic canary
and anonymous GET; non-ACTIVE, invalid-policy and missing-config denials;
single-config A diagnostic against an actual owned canary with bucket-specific
proxy delta; safe three-field VO and empty request/response audit; stale OSS
health after the 1-second snapshot window while core readiness/liveness remain
UP and private GET still succeeds; managed default switch to B followed by new
B upload/HEAD/GET and old A GET. It checks DB service binding, private SigV4
signature/expiry, public no-signature/no-expiry, and captures the live OpenAPI
path. Other demo provider endpoints are rewritten to the owned loopback proxy in the
owned database before app start.

The diagnostic elapsed time is recorded. A normal fast MinIO response **does
not** prove the worst-case 3 seconds per network step / five-step bound; that
requires a separate fault-injection test. T45's future policy-state semantics
are excluded: `UNVERIFIED` may be a legitimate diagnostic result here.

The current script is a prepared acceptance candidate, not a passing result.
`mc admin accesskey create --json` response shape, MinIO ACL denial behavior,
OpenAPI/Actuator response shape and the fresh T44 product candidate still need
the Lead-owned real run. A failure retains only phase/type and safe counts; it
does not get silently reclassified as a pass.
