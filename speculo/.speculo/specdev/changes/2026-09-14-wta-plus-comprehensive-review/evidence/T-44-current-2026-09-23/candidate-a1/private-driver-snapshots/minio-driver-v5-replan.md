# T44 owned MinIO driver recovery after three failed runs

Scope: fixture-only recovery in `/tmp`; no product or SpecDev candidate was executed or changed for these failures. Preserve all original sanitized `result.json` files and their non-acceptance verdicts.

| Run | Driver | Fixed source and JAR | Observed failure | Cleanup |
|---|---|---|---|---|
| `92641a8250fba5db` | v1 | HEAD `e11c1b6f3a4a4a8bdc1746044d65fe8445c0865c`, tree `92781356146df461ab9815a13aa2884effc0740e`, JAR SHA-256 `835b4de84b911ff77ba20ba003218f30af015559dccfadcdbc30fdf1d8d069a8` | `owned_minio` RuntimeError; later static diagnosis found two invalid empty `hexsql` expressions. | `[]` |
| `48734cc2c4defd2e` | v2 | Same fixed HEAD/tree/JAR | `owned_minio` RuntimeError; additional safe reason allowlist did not identify dynamic SQL failure. Same empty-hex fixture defect remained. | `[]` |
| `c4d6b1dd5e8d3a26` | v4 | Same fixed HEAD/tree/JAR | Reached `diagnostic`; SQL stage `diagnostic_audit`, MySQL errno 1267/SQLSTATE HY000. Startup remote count 0, real A/public/negative/authorization and scoped diagnostic checks preceding audit passed, but run `accepted=false`. | `[]` |

v3 was a static diagnostic derivative, not a live accepted run. v4 fixed the empty SQL literals and added sanitized SQL stage/errno reporting. v5 changes five audit comparison operands: two URL `LIKE` patterns and three canary terms inside `CONCAT('%', ..., '%')` use the already tracked `base.compared_hexsql` helper (`CONVERT(... USING utf8mb4) COLLATE utf8mb4_general_ci`). It leaves SQL inserts, credentials and production code unchanged. It also adds three real administrator POST negative-ID requests (`0`, `-1`, nonnumeric), requiring each to avoid HTTP 200 plus business code 200 and requiring the owned MinIO proxy total to remain unchanged; only safe HTTP/business codes are recorded. No other `h(...)` operands in v5 are used in a text comparison; the `m.perms LIKE 'system:oss%'` query uses a plain SQL literal, not a hex conversion. The helper was previously used by the T40 owned runner for real SQL comparisons; this is static support, not a claim that the new T44 query ran successfully.

Fixed v4 SHA-256: `9da4734a920245fe2824a8c98f281bdbe77ea84e18a0349decfb8ad9953f87aa`.
Fixed v5 SHA-256: `22e9397f5897886f834443637055b9b8182f48b1f1c32a6efec0358f3722d439`.
The six SQL source hashes are retained in each run's result; v4 result records 103 fresh tables. Do not infer later stages passed from v4's partial progress.

Recovery gate before Lead executes v5: independent review of the five-operand diff, three negative requests and SHA, `py_compile` exit 0, exact expected clean HEAD/tree/full JAR provenance, unchanged six SQL inputs, private owned service labels/IDs/ports, and no concurrent source/build mutation. Lead's v5 result may be accepted only if all required phases pass with the existing strict assertions, zero skips if any test reporter is involved, `accepted=true`, source/JAR unchanged after run, and `cleanup_errors=[]`. Preserve any v5 failure and investigate its bounded stage instead of relabeling v1/v2/v4 as successful.
