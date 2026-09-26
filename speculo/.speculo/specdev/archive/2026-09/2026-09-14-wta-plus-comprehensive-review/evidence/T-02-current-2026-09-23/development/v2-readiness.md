# T-02 isolated HTTP/MySQL runner v2 frozen for red candidate

Original `/tmp/wta-t02/run-log-redaction-integration.py` remains byte-identical at SHA256 `0f303c5e9b2fd5207187754670d458e1ccaec1fc2cd358f0db1b1cfb8043d1df`.

Frozen v2 `/tmp/wta-t02/run-log-redaction-integration-v2.py` SHA256 `cff4e66c3f108c71a212623fdab88f88783415f977d33803ed2bf2180024f452` targets fixed clean test-only source `8a88965363ca295122efadabf0bc97e99c6704c4`. It selects seven exact test classes, requiring the new `LogRedactionHttpMySqlIntegrationTest#onlineDeviceTokenFromRealIssuerNeverReachesHttpOrErrorAudit` method to execute. The fixed source has the method and both MySQL fixtures read only `T02_MYSQL_PASSWORD` directly from the child process environment; no `log.mysql.integration.password` property remains. The seven class source paths exist.

V2 uses a random password-bearing app account restricted to its owned random `namewta_log_test_<runid>` database with `CREATE,DROP,INDEX,SELECT,INSERT`. Root/app secrets never enter argv, public result, XML-property arguments or returned diagnostics. Both are redacted from logs and failure text. The owned Docker container uses dual owner/run labels, full IDs and a random loopback port. V2 records captured anonymous volume names before cleanup, accepts Docker's case-insensitive `no such volume` only when stderr names that exact volume, and verifies absent volumes, no owned containers, no Maven process-group members and closed ports. It retains clean exact-source before/after checks and fresh XML counts for all seven classes. On Maven test failure, the result records each test method's failure/error/skipped kind and XML exception `type` only; message/stack text is excluded. Missing/stale XML, setup failures, or non-canary failures must not be interpreted as a product behavior red just because exit is nonzero.

No-service synthetic safety check: `python3 /tmp/wta-check.py /tmp/wta-t02/verification driver-synthetic-v2-final-fixed-selector-20260923 /srv/WTA-plus python3 /tmp/wta-t02/test_driver_safety_v2.py` → exit 0, 10 tests, 0 failures/errors, result `/tmp/wta-t02/verification/driver-synthetic-v2-final-fixed-selector-20260923.json`. No Docker, Maven, HTTP or database service was run by this preparer.

Lead-only real red command:

```bash
python3 /tmp/wta-t02/run-log-redaction-integration-v2.py --execute --expected-head 8a88965363ca295122efadabf0bc97e99c6704c4 --required-canary-method onlineDeviceTokenFromRealIssuerNeverReachesHttpOrErrorAudit
```

The driver writes `/tmp/wta-t02/runs/<runid>/result.json` and sanitized `maven.log`, both private. Exit 1 is attributable only after checking exact new-method XML issue type, count/skip, source identity, and cleanup. A later implementation candidate requires its own exact SHA; the test-only red source is not acceptance.
