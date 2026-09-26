# T42 private MAIL v5 handoff — offline only

Fixed review input: product `0595e2ccbd03474bced92809b2ac9056c9a15751`, full-context test source SHA-256 `a4ad9bf43c3f2eabdc725ba65b57e818c37fff1134f1c670135d6cd38a8e836b`. This is an exact-source review input, not a completed real acceptance. The first Lead targeted Maven failed at common-OSS test compilation before any T42 test ran; that separate source fix requires a later clean candidate and ordinary gate rerun.

V4 remains unchanged and its failed static interface finding is preserved in `/tmp/wta-t42/review-product-a1-runtime.md`. V5 changes only the private proxy's `disarm_drop_put`: in NORMAL, `/disarm` returns 200 only if `active_holds == 0`; in NORMAL with an active hold it remains 409. DROP_PUT_RESPONSE continues to drop every successful PUT response until disarm; HOLD_CANCELLED still rejects retries and can be disarmed only when waiters drain. `/release-hold` remains the only action that allows a held PUT to forward. The v5 runner differs from v4 solely in private proxy path, pin hash and version label. No product file, frozen v4 file, Docker container, Maven process or network service was modified or started by this preparation.

Frozen files:

- `/tmp/wta-t42/safe_s3_count_proxy_v5.py` SHA-256 `0b465c2a3a518ad503daeeba787df14a9cc4be06f48d42de517ff9fc14020fca`
- `/tmp/wta-t42/run-notify-mail-attachment-integration-v5.py` SHA-256 `25ee81558bffd6e0a28b4130b06641bce0c450b9bb2cf7717e1d733179bc7faa`
- `/tmp/wta-t42/test_s3_fault_proxy_v5.py` SHA-256 `cc5214eb921f9117897b9825683ae2c029370407488f03078b01c99169b815cd`

Offline verification from `/tmp/wta-t42`: `python3 -m py_compile safe_s3_count_proxy_v5.py run-notify-mail-attachment-integration-v5.py test_s3_fault_proxy_v5.py` exit 0; `python3 -m unittest -v test_s3_fault_proxy_v5.py` exit 0, 14 tests. New negative control checks NORMAL with an active hold is 409 and idempotent NORMAL after waiter drain is 200; inherited tests check no upstream PUT after timeout/cancel, persistent response-drop, phase transitions, count endpoint and control isolation. These are in-memory tests and do not prove product behavior.

Lead-only real command after a new clean source candidate and exact method-list review:

```text
python3 /tmp/wta-t42/run-notify-mail-attachment-integration-v5.py --execute --expected-head <CLEAN_40HEX_SHA> --expected-methods <15_EXACT_METHOD_NAMES_COMMA_SEPARATED>
```

The runner requires all 15 methods in one fresh `NotifyMailAttachmentIntegrationTest` XML, zero failure/error/skip, private Surefire property marker, exact source identity before/after, and owned MySQL/Redis/MinIO/proxy/process/volume/port cleanup. Keep actual provider bytes and COPY_UNKNOWN assertions in the Java tests. Do not count the first compile failure or these 14 offline checks as real acceptance.
