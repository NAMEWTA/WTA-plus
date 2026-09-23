# T-50 Source B engineering review

Verdict: **PASS for the fixed generated-contract delta; final T-50 acceptance remains pending running gates.** Fixed base `0b33ff361820249f947a2767674356dabab59774` (Source A3), head `50757f9b872b0b9b0131e4b2a4538bff7473f74d` (Source B), reviewed with `git diff base...head`. The intervening commit changes exactly four `frontend/packages/api-contracts` files. Both commits have the same backend subtree `507250fd94c75aa15a89506e2a4771aa3497469b`; no backend source is silently attributed to B.

## Provenance and generation

- The committed new `source.json` is byte-for-byte identical to the retained A3 HTTP 200 raw capture at `/tmp/wta-t50/openapi-live/00926d62b78a715a/source.json`: 508,752 bytes, SHA-256 `2c12f7d5eeea5afc906577250a928f6d2d0e5e7bbd45a6e345dd6320bf32230c`. That live capture had 436 paths, 445 schemas, no baseline loss, clean A3 source before/after, the same full-JAR hash before/after, and complete recorded owned-resource cleanup; see `source-a3-live-contract-review.md`.
- The committed `provenance.json` names backend commit A3, repository `backend`, `/v3/api-docs`, `openapi-typescript@7.13.0`, OpenAPI 3.1.0, 436 paths, 445 schemas and 77 tags. Its `rawSha256` matches the committed raw bytes. SHA-256 of `source.json` bytes followed by the exact `provenance.json` bytes is `6dcd90b63abedf4d897c710e0b73709c7d202b18bc993b2ccc24a353d70fa1ce`, matching the immutable directory and `current.json` pointer. This is the repository tool's `createRevision` algorithm.
- Formal `openapi:fetch`, `openapi:generate`, and `openapi:check` records are `/tmp/wta-t50/b1-openapi-{fetch,generate,check}.{json,log}`; all three exit 0. Generation and check both report 900,696 bytes and SHA-256 `734d43fc7b8704ad555df9a65a034f89f55c449eba12b5590107dfc731bcf0a5`, equal to the fixed B `generated/openapi.ts` bytes independently hashed from Git. Thus the generated TypeScript is reproducible from the active immutable snapshot.

## Contract delta and boundaries

- Recursive JSON comparison of the previous active raw snapshot and B's new snapshot finds exactly five semantic locations: descriptions of `NotificationCommand.strategy`, `.mode`, and `.priority`; addition of `NotificationCommand.required=["priority"]`; and `/servers/0/url` changing with the isolated localhost capture port. All `paths` and the `Skip` schema are JSON-value equal. The source file has additional raw ordering changes, but no other JSON contract change.
- The generated TypeScript makes `NotificationCommand.priority: number` required, with the explicit HTTP JSON `0` description. `strategy` remains the historical enum union with new-submit `ALL` described, and `mode` remains `ASYNC`; no unrelated TS type changed. The two `Skip` hunks only reorder optional properties to reflect source property order and do not change their names or types.
- `current.json` points only to the new immutable revision. No manual edit to a generated business model, HTTP operation, or backend contract appears in this four-file diff. The exact A3 live source and Source B provenance prevent the stale-snapshot problem that the former A2 capture exposed.

## Evidence limit

This is a static and retained-evidence review. At report time, the fixed-B eight-class owned integration rerun and frontend checks were still running; core/static gates had not yet been reported. They must be evaluated by their actual fixed-B records before marking the whole ticket accepted. I did not execute Maven, Docker, HTTP, or frontend builds.
