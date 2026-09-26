# T-39 eight-class owned regression driver

Frozen v1 and v2 artifacts remain unchanged. The v3 candidate is `/tmp/wta-t39/run-notify-deadline-integration-v3.py`. It has not launched Docker, Maven, a JVM, or any network service. Its preflight currently rejects a real launch because the two new T-39 source classes have not yet landed.

Run only after the writer fixes a clean full candidate SHA:

```text
python3 /tmp/wta-t39/run-notify-deadline-integration-v3.py --execute --expected-head <40-hex-clean-candidate-SHA>
```

It selects exactly eight classes in one owned MySQL/Redis run, with separate fresh Surefire XML reports, positive observed test counts, zero failures/errors/skips, and method lists:

| Class | Opt-in property | Report module | Prior observed count (informational only) |
| --- | --- | --- | ---: |
| NotifyDeadlineIntegrationTest | notify.deadline.integration | wta-admin | new; unknown |
| EnterpriseQueuedNotificationIntegrationTest | profile.notify.integration | wta-admin | 7 |
| NotifyWakeIntegrationTest | notify.wake.integration | wta-admin | 1 |
| RedisUtilsDeadlineIntegrationTest | notify.deadline.redis.integration | wta-common-redis | new; unknown |
| NotifyAtomicResultIntegrationTest | notify.atomic.integration | wta-admin | 66 |
| NotifyManualRetryIntegrationTest | notify.manual.retry.integration | wta-admin | 13 |
| NotifySmsDispatchIntegrationTest | notify.sms.integration | wta-admin | 10 |
| RedisNotifyIdempotencyStoreIntegrationTest | no separate class opt-in; owned Redis port | wta-admin | 3 |

There are **four required MySQL password variable names**, not three: T39, T36, T38, and T35. Source inspection shows the SMS class still reads `T35_MYSQL_PASSWORD`; omitting it would skip/fail a required regression. All four names receive the *same newly generated owned application password* in the child process environment only; no password enters Maven argv, public XML, or the summary. The driver uses one fresh six-SQL 103-table database and localhost Redis, fresh Surefire forks (`-DforkCount=1 -DreuseForks=false`), full-ID/dual-label container ownership, captured anonymous volumes, process-group and port cleanup, and exact clean source proof before/after. Existing class counts above are only planning expectations; acceptance comes from new XML for this source and this run.

Offline safety validation: `/tmp/wta-t39/driver-synthetic-v3-freeze-20260923.json` and matching `.log`, 13/13 passing, exit 0. Real integration is unrun and reserved for the Lead.
