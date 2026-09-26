# T-39 eight-class driver v4 freeze

Driver `/tmp/wta-t39/run-notify-deadline-integration-v4.py` is a copy of frozen v3 with only the common Redis source preflight corrected. v1–v3 and their evidence are unchanged. Do not run a real gate until the Lead fixes a clean implementation commit and authorizes owned resources.

The new common test on the current uncommitted T-39 source uses `AnnotationConfigApplicationContext`, a subclass of `GenericApplicationContext`. v4 accepts either declared-and-instantiated context class and checks `@TestInstance(PER_CLASS)`, `@BeforeAll`, `@AfterAll`, the class-level Redisson field, context/client close, and `notify.redis.integration.port` resolving to `127.0.0.1`. The v3 literal `GenericApplicationContext` test would have rejected the actual source before launch. A synthetic copy using `GenericApplicationContext` passes, and missing lifecycle/loopback cases fail in the pure preflight test.

`NotifyDeadlineIntegrationTest` currently has `notify.deadline.integration=true`, reads `T39_MYSQL_PASSWORD`, validates a fresh `jdbc:mysql://127.0.0.1:<port>/namewta_notify_test_<id>` URL, and opens/closes the T36 owned Atomic fixture. `RedisUtilsDeadlineIntegrationTest` currently has `notify.deadline.redis.integration=true`, reads `notify.redis.integration.port`, creates a single class-level Redisson client using the production RedisConfig codec/NameMapper, and closes it in `@AfterAll`. Both source files are still uncommitted as of this review; these observations are not acceptance. v4 retains the eight exact classes, four password env aliases, fresh Surefire XML/positive zero-skip gate, and owned resource cleanup from v3.

Offline validation: `/tmp/wta-t39/driver-synthetic-v4-freeze-20260923.json` and matching `.log`, exit 0, 14 tests. Real Docker/Maven/JVM gate not run.

Future Lead-only command:

```text
python3 /tmp/wta-t39/run-notify-deadline-integration-v4.py --execute --expected-head <40-hex-clean-candidate-SHA>
```
