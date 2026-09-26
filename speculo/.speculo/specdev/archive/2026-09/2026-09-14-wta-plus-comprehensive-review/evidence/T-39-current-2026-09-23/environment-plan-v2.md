# T-39 owned integration driver v2

`run-notify-deadline-integration-v2.py` is a separate candidate from frozen v1; v1 and its evidence remain unchanged. This preparation has not launched Docker, Maven, or a JVM.

The v2 acceptance selector contains four exact classes: `NotifyDeadlineIntegrationTest`, `EnterpriseQueuedNotificationIntegrationTest`, `NotifyWakeIntegrationTest` (all `wta-admin` reports), and `RedisUtilsDeadlineIntegrationTest` (`wta-common-redis` report). Before creating credentials or containers the runner checks a clean exact HEAD and all four sources/opt-in flags. In particular, the new common source must exist at `backend/wta-common/wta-common-redis/src/test/java/org/namewta/common/redis/utils/RedisUtilsDeadlineIntegrationTest.java`, declare `notify.deadline.redis.integration=true`, use `notify.redis.integration.port`, and show `GenericApplicationContext`. As of preparation, that new source has not landed; the real runner must therefore reject execution at preflight until the writer commits it.

The eventual Lead-only command is:

```text
python3 /tmp/wta-t39/run-notify-deadline-integration-v2.py --execute --expected-head <40-hex-clean-candidate-SHA>
```

Maven runs `-Pdev -pl wta-admin -am test` with four class selectors, their four opt-in properties, and `-DforkCount=1 -DreuseForks=false`. The password is passed only through child process environment as identical `T39_MYSQL_PASSWORD` and `T36_MYSQL_PASSWORD`; URL, username, and owned Redis port are non-secret properties. The runner creates a fresh owned six-SQL/103-table MySQL database and loopback Redis, uses separate exact Surefire report paths, requires positive tests and zero failures/errors/skips in each class, and records per-class methods. Cleanup checks process group, owned full container IDs and labels, captured anonymous volumes, ports, and exact clean source after the run. No supplier network is part of the selected classes.

Synthetic validation: `/tmp/wta-t39/driver-synthetic-v2-freeze-20260923.json` and matching `.log`, 13/13 passing, exit 0. Real acceptance remains unrun and must be driven by the Lead after a fixed clean candidate and source preflight.
