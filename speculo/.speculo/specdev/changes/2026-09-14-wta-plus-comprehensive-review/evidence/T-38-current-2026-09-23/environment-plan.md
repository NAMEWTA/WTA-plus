# T-38 isolated manual-retry acceptance runner (prepared only)

Runner: `/tmp/wta-t38/run-notify-manual-retry-integration.py`.

Source checkpoint at preparation: `b47ff8b91cfef02a9f28de0e201edcd1575a950f`, clean; T-38 product source still equals the reviewed `617a3693ddb2897851ab46d4131690898df6a690` baseline. The expected new `org.namewta.test.notify.NotifyManualRetryIntegrationTest` does **not** exist yet; first real run must wait for its immutable implementation commit and inspect its final name, opt-in property, fresh-owned JDBC/user/Redis properties, and `T38_MYSQL_PASSWORD` environment use. Existing second selector is the actual `org.namewta.test.notify.NotifyWakeIntegrationTest`, gated by `notify.wake.integration=true`; its fixture reads `T36_MYSQL_PASSWORD`, so the private child environment supplies the same newly generated owned app password under both names. No supplier network is required or configured by this runner.

The eventual Lead-only invocation is:

```bash
python3 /tmp/wta-t38/run-notify-manual-retry-integration.py --execute --expected-head <40-hex-clean-implementation-SHA>
```

The runner refuses missing execute/SHA, dirty or different source before and after, absent exact test class, missing opt-in property, missing fresh Surefire XML, zero tests or any skip/failure/error in either class. It records method names and counts separately. Maven selects only `NotifyManualRetryIntegrationTest,NotifyWakeIntegrationTest`, enables both properties, uses one fresh fork per class, and passes nonsecret owned JDBC URL/user/Redis port via JVM properties. Passwords are generated in memory, written only to 0600 temporary env files and Maven child environment (`T38_MYSQL_PASSWORD`, `T36_MYSQL_PASSWORD`); no password enters Maven argv. Raw Maven output stays private, is redacted to private clean log, and is removed. Fresh exact-class XML is scanned/redacted if an owned password appears; such leakage fails the gate.

Resources: dual `namewta.test.owner=T-38` and random run label; Docker full IDs only; MySQL 8.4.9 and Redis 8.6.3 bind random 127.0.0.1 ports; six SQL baseline files load into a fresh random `namewta_notify_test_*` DB; 103-table count checked; app account receives grants only on that database. Redis is loopback-only and unauthenticated. The runner pre-captures anonymous volume names and later verifies each absent, removes only dual-label/full-ID containers, kills/verifies Maven process group even if its leader exited, verifies published ports closed, and requires clean source identity after cleanup. Result and logs live under private `/tmp/wta-t38/runs/<run-id>/`.

Preparation-only synthetic check: `python3 /tmp/wta-check.py /tmp/wta-t38 driver-synthetic-final /srv/WTA-plus python3 /tmp/wta-t38/test_driver_safety.py` (re-run after final source edit under a new evidence name). No Docker, Maven, provider call, or network service has been started in this preparation. `NotifyOutboxWakeTransactionTest` is not a real current class; `NotifyWakeIntegrationTest` is the exact second class.
