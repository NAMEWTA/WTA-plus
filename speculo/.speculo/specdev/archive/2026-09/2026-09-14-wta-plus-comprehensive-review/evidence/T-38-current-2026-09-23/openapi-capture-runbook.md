# T-38 full-JAR live OpenAPI capture runner

This is a prepared runner, not an executed acceptance. It is independent of the product writer's OpenAPI fetch/generate/check edits. The runner is `/tmp/wta-t38/capture-live-openapi.py`; it imports only the frozen owned-resource helper `/tmp/wta-t38/run-notify-manual-retry-integration.py` at SHA-256 `d068610f75a9c65cabf77d69d230884b33edd9c9f3d64c73c73296655066c31e`. It never uses `/srv/ops` credentials or existing services.

Run the **no-service** inspection any time:

```sh
python3 /tmp/wta-t38/capture-live-openapi.py --preflight
```

This reads Git, the committed active OpenAPI pointer, six SQL files, the JAR file hash if present, and the helper hash. It does not inspect/start Docker, run Maven/JVM, or make HTTP requests. `source_clean=false` or an existing JAR hash is **not** an acceptance claim.

The eventual Lead-owned capture requires a fixed clean implementation commit, independent tests, and a new full bundle package. The build command must be exactly `cd backend && ./mvnw -B -ntp clean package -DskipTests` (optionally `-Pbundle-full` at the end). Store a 0600 log with Maven `BUILD SUCCESS`; record actual start/end UTC around that command. Before constructing the proof, verify Git HEAD/tree and clean state, the full bundle check, JAR SHA-256/size, and that the JAR modification time falls within the recorded build interval. Proof is a 0600 JSON file outside the repo:

```json
{
  "command": ["./mvnw", "-B", "-ntp", "clean", "package", "-DskipTests"],
  "cwd": "backend",
  "exit_code": 0,
  "source_clean_at_build": true,
  "source_head": "<40-hex commit of the actual build>",
  "source_tree": "<40-hex Git tree of that commit>",
  "started_utc": "<ISO UTC build start>",
  "finished_utc": "<ISO UTC build finish>",
  "build_log": {"path": "/tmp/<private-build-log>", "sha256": "<64-hex log digest>"},
  "artifact": {
    "path": "backend/wta-admin/target/wta-admin.jar",
    "sha256": "<64-hex digest of this full JAR>",
    "size_bytes": 123456789
  }
}
```

After the clean source, full JAR and proof are fixed, the **future** execution command is:

```sh
python3 /tmp/wta-t38/capture-live-openapi.py --execute \
  --expected-head <fixed-40-hex-HEAD> \
  --expected-jar-sha256 <fresh-full-JAR-64-hex-SHA256> \
  --package-proof /tmp/<private-package-proof>.json
```

The script requires both digests and the build proof **before** creating services. Its Docker image references are local `mysql:8.4.9`, `redis:8.6.3`, and the fixed locally inspected `pgsty/minio@sha256:83885c27b3b5b673049e33ddf4029afe2c134fd51ce4309e65e4f39d3b9ca282`; all `docker run` calls use `--pull=never`. MySQL/Redis/MinIO ports are random loopback-only and containers have exact owner/run labels. The six canonical SQL files create fresh `wta-plus` plus an app-only account; startup is blocked unless all Outbox rows, external deliveries and enabled SMS/MAIL accounts are zero. The 0600 prod overlay overrides datasource, Redis, third-party endpoint addresses, multipart and log paths; the full JAR gets only the overlay *path* in argv. No vendor account is enabled or contacted.

The runner captures the direct 200 response bytes from `http://127.0.0.1:<owned-port>/v3/api-docs` to private `source.json`, verifies OpenAPI 3.0/3.1, all old paths/methods/schemas retained, retry/cancel POST and `RetryReceipt.queuedCount: integer`, and records body length/hash plus current active revision. Missing paths fail; no JSON assembly or fallback to an old source is performed. It closes the application process group, removes only dual-labelled full-ID containers with `rm -fv`, verifies every captured anonymous volume absent and mapped port closed, deletes private credential/config files, and verifies source/JAR/helper hashes afterward. Any failure keeps a private `result.json` with `acceptance=false` and cleanup errors. The product writer then uses exactly that `source.json` for `openapi:fetch`, `openapi:generate`, and `openapi:check` under its own write scope.

Remaining execution-time unknowns: the fixed T-38 candidate and its package proof do not yet exist; the full app might fail startup on newly introduced configuration; the OpenAPI generator may expose a schema name or path different from source expectations. Those failures require source inspection, not switching to shared services or patching an old snapshot. This preparation has not started Docker, Maven, a JVM, HTTP, or any vendor call.
