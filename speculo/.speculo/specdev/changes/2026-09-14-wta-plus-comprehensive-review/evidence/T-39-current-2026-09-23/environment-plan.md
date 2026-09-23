# T-39 isolated deadline/Profile/Wake real-test runner preparation

Prepared on repository HEAD `5ec74a2764e11a09f4303e546d5132ddcf36aa75` without modifying the repository, building, or launching a service. The new T-39 test source does not yet exist. Runner: `/tmp/wta-t39/run-notify-deadline-integration.py`, adapted as a separate file from the frozen T-38 resource hygiene runner `/tmp/wta-t38/run-notify-manual-retry-integration.py` (SHA-256 `d068610f75a9c65cabf77d69d230884b33edd9c9f3d64c73c73296655066c31e`). It is opt-in and refuses to launch without `--execute --expected-head <exact-clean-40hex>`.

## Exact three-class contract

The future new class is `org.namewta.test.notify.NotifyDeadlineIntegrationTest`, opt-in `notify.deadline.integration=true`. The runner requires its file, exact class name, the property and `T39_MYSQL_PASSWORD` environment read **before resource launch**. When the writer lands its code, inspect this test source before first run and adjust only a new runner version if its declared properties change. Its method count is unknown; a positive fresh XML and zero failure/error/skip are mandatory, so missing source or skipped class cannot pass.

The current existing `org.namewta.test.notify.EnterpriseQueuedNotificationIntegrationTest` has seven `@Test` cases and `@EnabledIfSystemProperty(profile.notify.integration=true)`. It calls `new NotifyAtomicResultIntegrationTest().open()/close()` as a fixture; that fixture reads `T36_MYSQL_PASSWORD`, an owned loopback MySQL URL, username and Redis port. The runner provides **the same random app password** to `T39_MYSQL_PASSWORD` and `T36_MYSQL_PASSWORD` in the Maven child environment, never in Maven argv. The Enterprise fixture uses real MyBatis/Profile and Redis; its `NotifyClient` is a controlled substitute, so it neither needs nor calls a real SMS supplier. The whole Atomic test class (about 66 cases) is **not selected**.

The current `org.namewta.test.notify.NotifyWakeIntegrationTest` has one `@Test` and `notify.wake.integration=true`; it reuses the same Atomic fixture and sends a raw `CLIENT PAUSE` to its owned Redis. Therefore this runner's loopback Redis instance remains **unauthed**, as in the previously accepted T-38 test runner. That is isolated to a randomly assigned 127.0.0.1 host port and is different from the full-JAR OpenAPI runner's authenticated Redis config. `NotifyWake` and Enterprise require `T36_MYSQL_PASSWORD`; no other external service or vendor account is needed.

The future Maven selector is exactly:

```text
./mvnw -Pdev -pl wta-admin -am test
-Dtest=NotifyDeadlineIntegrationTest,EnterpriseQueuedNotificationIntegrationTest,NotifyWakeIntegrationTest
-Dsurefire.failIfNoSpecifiedTests=false -DforkCount=1 -DreuseForks=false
-Dnotify.deadline.integration=true -Dprofile.notify.integration=true -Dnotify.wake.integration=true
-Dnotify.mysql.integration.url=jdbc:mysql://127.0.0.1:<random>/namewta_notify_test_<runid>?...
-Dnotify.mysql.integration.username=<owned_app_user> -Dnotify.redis.integration.port=<random>
```

Only nonsecret URL/user/port appear in argv and the result. MySQL root and app passwords live in 0600 temporary env files and the child process environment; SQL/Redis do not contact shared instances. A new owner/run pair of Docker labels `T-39` and random 64-bit run ID, full container IDs, random 127.0.0.1 ports, local MySQL 8.4.9/Redis 8.6.3 with `--pull=never`, six exact SQL imports and 103 business tables are required. The generated app account has grants only on its fresh named database. All Docker captures are by exact full ID and both labels. The runner records pre-captured anonymous-volume names before cleanup, verifies each absent after `rm -fv`, confirms all owned ports closed and Maven process group empty even if its leader exited. It sanitizes fresh Surefire XML/logs and deletes private env files on every path.

The runner records each of the three exact fresh Surefire XML suites separately with method names and counts. `tests>0`, `failures=errors=skipped=0` and Maven exit 0 are required for **each** class; a single stale/missing XML fails the verdict. The class-level `reuseForks=false` avoids the static RedisUtils client surviving between fixture contexts. The result is accepted only if clean expected HEAD/tree are identical before/after and all owned-resource cleanup checks pass. Any future T39 new class count/property change needs a new frozen runner revision and fresh synthetic record before Lead's first real run.

Synthetic-only preparation: `python3 /tmp/wta-check.py /tmp/wta-t39 driver-synthetic-freeze-20260923 /srv/WTA-plus python3 /tmp/wta-t39/test_driver_safety.py` exited 0 with 12/12 tests. The synthetic test mocks Docker/Maven boundaries and covers exact labels/full IDs, volume absence error forms, XML freshness/skip rejection, child-secret placement and process-group cleanup. The initial synthetic run is also retained; the freeze run binds the final driver. No real Docker/Maven/JVM/database/network test was executed. The later real command, only after the new test exists and Lead freezes an implementation commit, is:

```sh
python3 /tmp/wta-t39/run-notify-deadline-integration.py --execute --expected-head <fixed-clean-T39-commit>
```

Lead must inspect the new class's property and secret contract and exclusively authorize/execute that command. This note and the driver are preparation, not T-39 Evidence or a passed integration run.
