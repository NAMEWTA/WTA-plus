# T-41 owned environment runners — prepared, never launched

This private preparation is bound to `/tmp/wta-t41/version-freeze-v1.json`. The repository at preparation was clean source `d7d534cb03aa000d603a53c092c4bf1d48bb5cc3`; the new `NotifyInboxPagingIntegrationTest` class was not present. The runner will refuse to launch services until a fixed clean implementation commit contains the exact class, `notify.inbox.paging.integration=true` opt-in, and a reviewed owned MySQL/Redis seam. No Docker, Maven, Java service, HTTP or browser run was started here.

The single-class runner is `/tmp/wta-t41/run-notify-inbox-paging-integration-v1.py` (SHA-256 `81ecc9db1fc4ecbde4ae1332b90f753d499ca18e26ed3c361b3f9ac21eeef6b9`). It selects **only** `org.namewta.test.notify.NotifyInboxPagingIntegrationTest`, invokes an independent Surefire fork, and requires a fresh XML with positive tests and zero skip/failure/error for acceptance. It passes the same new random owned application password to the Maven child's private `T41_MYSQL_PASSWORD` and `T36_MYSQL_PASSWORD` environment variables, for either a direct seam or the existing Atomic fixture; the password never enters the Maven argv. Nonsecret JDBC URL, username and random loopback Redis port are properties. A natural red-light Maven failure retains its sanitized fresh XML in the private run directory and preserves methods/counts and the sanitized log, while `acceptance=false` remains mandatory. Only Lead may decide whether that failure is the intended 501st-row assertion; bootstrap failures do not qualify.

The driver uses `mysql:8.4.9`, `redis:8.6.3`, six fixed SQL files and exactly 103 fresh schema tables. The MySQL root password is in a 0600 per-run container env-file; the generated application user has grants only on the random owned database. Redis is an isolated loopback-only instance. Both containers carry `namewta.test.owner=T-41` plus a random `namewta.test.run` label. It captures full IDs and anonymous volume names before cleanup, then requires group termination even when the Maven leader exits, both full-ID containers removed, each captured volume absent, both ports closed, and exact clean source HEAD/tree before and after. Any missing XML, skip, failed assertion, source drift or cleanup error fails acceptance.

Only after the writer fixes the implementation commit and Lead verifies that the test source's final fixture matches these two environment aliases, the proposed invocation is:

```bash
python3 /tmp/wta-t41/run-notify-inbox-paging-integration-v1.py --execute --expected-head <fixed-clean-40-hex-commit>
```

Do not run this concurrently with a frontend Vite build or another repository writer, because the exact-clean gate covers tracked generated files. For a red-light development checkpoint, use the same opt-in and exact clean SHA, retain the returned `result.json`/XML/log, and require a concrete assertion failure before changing product code. No special mode converts a red result into acceptance.

The independent full-JAR OpenAPI capture clone is `/tmp/wta-t41/capture-live-openapi-v1.py` (SHA-256 `e5ec0e5ada965c1a9df2fe007af3258627a32c1676c9643a3eac412aeff15b54`). It has its own `T-41-OPENAPI` owner/run labels and run directory, pinned MinIO image digest, owned MySQL/Redis/MinIO secrets in 0600 env/config files, explicit full-JAR clean package proof, exact artifact SHA, live bounded HTTP raw preservation before strict path/method/schema validation, and process/container/volume/port/source/JAR cleanup proof. Its reference helper remains the frozen T-38 helper SHA in the manifest. This clone was **not** run or used to fetch/generate an API contract. When a new full JAR and proof exist, Lead can supply its fixed source/JAR values:

```bash
python3 /tmp/wta-t41/capture-live-openapi-v1.py --execute --expected-head <fixed-clean-40-hex-commit> --expected-jar-sha256 <full-jar-64-hex-sha256> --package-proof <private-full-package-proof.json>
```

The formal `fetch/generate/check` workflow must consume the resulting direct HTTP `source.json` only after its capture gate passes; no old snapshot may be patched into a new revision. A T-41 browser runner is a separate future product/owned-resource task and is not implemented by these private scripts.

Offline synthetic safety checks are `/tmp/wta-t41/test_driver_safety_v1.py`; final record `/tmp/wta-t41/offline-synthetic-v1-final.json` and log are 0600. **12/12 pass, exit 0**, covering exact one-class selector/flag/fixture, deny-by-default CLI, dual-label full-ID cleanup, volume absence, PGID after leader exit, secret-redaction/env boundaries, positive/failure/skip/stale XML, retained failure XML, and capture raw-before-loss behavior. The earlier `offline-synthetic-v1.json` is a draft-run record before automatic red XML retention was added; it is preserved but superseded by the final record. No actual integration count, runtime schema behavior, browser flow, JAR capture or supplier interaction is claimed.
