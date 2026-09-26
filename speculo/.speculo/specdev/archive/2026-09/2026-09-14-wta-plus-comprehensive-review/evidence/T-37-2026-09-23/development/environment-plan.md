# T-37 isolated real-integration runner — preparation only

Status 2026-09-23: `/tmp/wta-t37/run-notify-retry-integration.py` and synthetic safety tests prepared; **no Docker container, Maven command, service, database, or supplier was started** by this task. The Lead alone may execute after the product writer fixes a nonempty clean implementation commit and reconciles any final test-selector changes.

## Confirmed current test entrypoints

- `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifySmsDispatchIntegrationTest.java`: exact FQCN `org.namewta.test.notify.NotifySmsDispatchIntegrationTest`; `@EnabledIfSystemProperty(notify.sms.integration=true)`, JDBC URL property `notify.mysql.integration.url`, nonsecret username property `notify.mysql.integration.username`, Redis port property `notify.redis.integration.port`; MySQL app password read solely from child-process environment `T35_MYSQL_PASSWORD`. This is the T-35 fixture reused by T-37; no product-code credential interface change is needed. Real Notify DAO/Outbox/Redis and production SMS adapter execute with only a synthetic supplier sender, never a vendor network call.
- `backend/wta-admin/src/test/java/org/namewta/test/notify/idempotency/RedisNotifyIdempotencyStoreIntegrationTest.java`: exact FQCN `org.namewta.test.notify.idempotency.RedisNotifyIdempotencyStoreIntegrationTest`; reads `notify.redis.integration.port`, skips if absent/invalid, connects to `redis://127.0.0.1:<owned-port>`, uses unique UUID keys and deletes its key. No MySQL connection or separate enable property. Driver requires its exact fresh Surefire XML with positive tests and zero skipped, so a silent assumption skip cannot pass.

## Execution contract for Lead (not executed in preparation)

Invoke only with an exact **40-hex clean HEAD**, after reviewing final test source and no concurrent product writer:

`python3 /tmp/wta-t37/run-notify-retry-integration.py --execute --expected-head <FULL_CLEAN_T37_SHA>`

The driver launches Maven from `/srv/WTA-plus/backend` with argv (nonsecret portions; randomized loopback ports/database/user are filled at runtime):

`./mvnw -Pdev -pl wta-admin -am test -Dtest=NotifySmsDispatchIntegrationTest,RedisNotifyIdempotencyStoreIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=1 -DreuseForks=false -Dnotify.sms.integration=true -Dnotify.mysql.integration.url=jdbc:mysql://127.0.0.1:<owned-mysql-port>/namewta_notify_test_<runid>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai -Dnotify.mysql.integration.username=t37_<runid> -Dnotify.redis.integration.port=<owned-redis-port>`

The password never appears in argv: the driver generates synthetic 40-character values in memory, writes owner-private `0600` temporary environment files, passes `T35_MYSQL_PASSWORD` only in the Maven child environment, streams app-user CREATE/GRANT SQL via stdin inside the **owned** MySQL container, redacts/removes raw output, and deletes the temporary files in `finally`. No secret values, hashes of secrets, full inspect Env, supplier body, or `/srv/ops` configuration are emitted. Result and cleaned log are private `0600` in `/tmp/wta-t37/runs/<64-bit-random-runid>/` (directory `0700`).

Isolation: direct local Docker socket, exact two labels `namewta.test.owner=T-37` and `namewta.test.run=<runid>`, captured **full** 64-character container IDs. `mysql:8.4.9` and `redis:8.6.3` bind only Docker-assigned random `127.0.0.1` ports. MySQL uses a new `namewta_notify_test_<runid>` database, six public SQL baselines in `10/20/30/40/50/60` order, expected 103 application tables, and app-user grants limited to that database. Its owned startup includes `--log-bin-trust-function-creators=1` so six-SQL trigger creation does not require global SUPER. Redis is an isolated loopback-only instance without auth or persistence because neither test accepts a Redis credential; no shared Redis FLUSH, production database, shared schema, vendor service, MinIO, or network supplier is used.

The acceptance verdict requires both exact selected Surefire class XML files to be fresher than Maven start and to report tests>0, failures=errors=skipped=0; methods and counts are recorded per class. Reactor no-match modules do not satisfy target evidence. Maven uses a fresh Surefire fork per test class (`forkCount=1`, `reuseForks=false`) to avoid sharing RedisUtils static Redisson state between classes. Before and after identity must be the same clean commit/tree, Maven exit 0, and owned cleanup complete. A one-hour timeout is failure. In all exits, the runner checks/kills the Maven **process group** even if its leader has exited, removes only discovered/captured containers after checking both labels, uses `docker rm -fv`, verifies captured anonymous volumes are absent, verifies loopback ports closed and both labels discover no containers. Any cleanup/identity/XML failure yields driver exit 1 and `acceptance=false`.

If the writer adds a new independent real class, the Lead must reconcile its exact FQCN, enable property, fixture credentials, selector and per-class XML requirement before use. This runner does not automatically satisfy a new test class.

## Synthetic verification and limits

- `python3 /tmp/wta-t37/test_driver_safety.py -v` ran via `/tmp/wta-check.py`: exit 0, seven synthetic cases passed. The cases cover owned MySQL flags, full-ID/dual-label removal, refusing foreign removal, anonymous-volume verdict, both exact fresh XML classes with skip/stale negative paths, process group with exited leader, and child-only credential environment. Record: `/tmp/wta-t37/verification/driver-synthetic-path-corrected-20260923.{json,log}`.
- An earlier unittest invocation used an unsupported absolute module-style argument, exit 1 before any test ran; preserved at `/tmp/wta-t37/verification/driver-synthetic-20260923.{json,log}`. The corrected path invocation above passed. This was a command error, not a runner finding.
- `python3 -m py_compile /tmp/wta-t37/run-notify-retry-integration.py` exit 0. No real service test has been run or claimed by this preparation.
