# T-38 live OpenAPI capture v2 — isolated Redis authentication

Version 1 `/tmp/wta-t38/capture-live-openapi.py` remains frozen unchanged at SHA-256 `06d93779dd9812d46105e2e7e7f3192b74a06f2453be50dee0b46fe7634b087c`. Its first real run, `/tmp/wta-t38/openapi-live/b6b7be1d9ef06cea/result.json`, failed before startup because the application sent Redis AUTH while the owned Redis instance had no password. The result retained identical clean source `7a6f75ac9238399daf7936797d07da141f0f5a03`, identical full-JAR SHA-256 `01678e64ede453a387d352b7961cb97ee382bd945ed2a2935851f46ee52c1ef4`, and complete process/container/volume/port cleanup. This is an environment-runner defect, not product evidence. The existing full clean package proof can be reused if its file/hash/source remain unchanged.

Use `/tmp/wta-t38/capture-live-openapi-v2.py` for the next run. Static inspection remains:

```sh
python3 /tmp/wta-t38/capture-live-openapi-v2.py --preflight
```

Real execution still requires the same clean source, full JAR digest and package-proof JSON:

```sh
python3 /tmp/wta-t38/capture-live-openapi-v2.py --execute \
  --expected-head 7a6f75ac9238399daf7936797d07da141f0f5a03 \
  --expected-jar-sha256 01678e64ede453a387d352b7961cb97ee382bd945ed2a2935851f46ee52c1ef4 \
  --package-proof /tmp/<same-private-package-proof>.json
```

The runner generates a fresh 40-character Redis password and writes a private 0600 `redis.conf` under the owned 0700 run directory. It changes that file's owner to UID/GID 65534 and launches only its owned Redis container as `--user 65534:65534`; the default Redis entrypoint therefore stays non-root and does not switch to another UID. The config is mounted read-only, and Redis runs from the mounted path. This keeps the 0600 file readable by the exact Redis process without exposing it on command lines. The published Redis port remains random `127.0.0.1` only, `--pull=never` remains, and the application overlay receives the same generated password. Health uses `REDISCLI_AUTH` populated inside the container from the mounted config, with no password literal in Docker or redis-cli argv. The config joins the private-file cleanup list and Redis password joins log redaction.

After direct HTTP 200 and bounded `/v3/api-docs` bytes, v2 now saves exact bytes to 0600 `source.json` and records its SHA-256 **before** strict schema comparison. A loss of any old path, operation or schema still fails the run; `result.json` retains the specific missing names, not the schema body, and the raw source stays available for review. The active baseline revision is `b36ef1c512d1af0e1196e3867dc836af692e5f15873617e168fbf166e140f0a8`, with 433 paths and 444 schemas; the baseline already has the retry path, while the T-38 DTO field must be supplied by the new full-JAR response. V2 never splices this baseline with the live document.

The original owner/run labels, full-ID cleanup, exact anonymous-volume checks, source/JAR/helper identity checks and strict package proof are unchanged. V2's Redis authentication and failure-evidence branches have only been exercised by offline synthetic tests. Lead alone may run owned services; no real run of v2 or product modification occurred in this preparation.
