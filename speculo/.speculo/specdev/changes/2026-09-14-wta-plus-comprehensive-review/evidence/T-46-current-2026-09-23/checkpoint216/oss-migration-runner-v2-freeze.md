# T46 owned real runner v2 freeze

Candidate source: clean `4f8c4b4aba1c91e273fa47ef4cad345d32a04062`; `OssStorageMigrationIntegrationTest.java` SHA256 `85e273553e87871623b79db5bd3c23757cd28478d3530e26a1e0c497f5173c7b`.

Frozen runner: `/tmp/wta-t46/run-oss-migration-integration-v2.py`, SHA256 `427a32985ebff608bc3359893a2d6e6a98f64f841c9a4a877d91f247289b11cf`. Original v1 `/tmp/wta-t46/run-oss-migration-integration.py` retained unchanged, SHA256 `0595fc1e9723837d3a83ad4838e78938a582c5acb94880131ee9f356347e3a77`.

V2 only narrows the method gate to the exact two committed test methods, in source order. It retains v1's owned MySQL 8.4.9 empty migration schema, pinned MinIO, private 0600 Surefire properties, clean source before/after, fresh XML positive/zero skip/failure/error, full-ID/volume/port/PGID and temporary file cleanup. It does not run Redis or reuse any external database, identity or bucket.

Offline checks: `python3 -B /tmp/wta-t46/test_run_oss_migration_offline_v2.py` exited 0, 7 tests; both Python files passed AST parse. No Docker, Maven or network service was launched. Test file SHA256 `dbe9db6d8cc8c09d9d2b7ea41f964f15841eba1a11d3fcff80d4ae27cee32ded`.

Lead-only real command, after confirming clean candidate and no concurrent Maven/service owner:

```text
python3 -B /tmp/wta-t46/run-oss-migration-integration-v2.py --execute --expected-head 4f8c4b4aba1c91e273fa47ef4cad345d32a04062 --expected-methods migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack,restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource
```

Result: `/tmp/wta-t46/oss-migration-runs/<run-id>/result.json` plus sanitized fresh XML in the same run directory. Static review is `/tmp/wta-t46/latest-integration-static-review.md`. This freeze is not an acceptance result; the two JUnit methods still require the owned real run.
