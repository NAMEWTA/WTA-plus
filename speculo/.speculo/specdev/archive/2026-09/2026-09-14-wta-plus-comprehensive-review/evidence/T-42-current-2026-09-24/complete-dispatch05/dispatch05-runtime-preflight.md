# Dispatch05 fresh runtime preflight (private, not execution evidence)

Status: **prepared only**. Dispatch05 writer has not frozen a new clean candidate for this review. Do not reuse Mail 27, regression 135, c980 HTTP, or old package proof as a new-source pass. No build, service, or repository write was performed here.

## Fixed tools and known prior inputs

- MAIL full Spring driver: `/tmp/wta-t42/run-notify-mail-attachment-integration-v5.py`, SHA-256 `25ee81558bffd6e0a28b4130b06641bce0c450b9bb2cf7717e1d733179bc7faa`. It requires `--execute --expected-head <40-hex clean SHA> --expected-methods <exact comma-separated names>`; one fresh `NotifyMailAttachmentIntegrationTest` XML must contain exactly those methods, positive tests, zero failure/error/skip. The prior green `a36d6d66aa5e06bd` had 27 methods at old head 43e5, only as an inventory reference.
- Eight-class Notify regression driver: `/tmp/wta-t42/run-notify-regressions-v1.py`, SHA-256 `8966d1ef0e403a3830f2fd7cc0137066ae3247baa029503dcd1c437151430867`. It requires `--execute --expected-head <clean SHA> --expected-suite-manifest <new private JSON>`. The old `dispatch02b-a3-regression-manifest.json` is explicitly bound to cd78 and its eight source hashes; it must fail the new candidate preflight. Prior green 135 at cd78 is not a Dispatch05 test result.
- Full JAR live API/HTTP driver: `/tmp/wta-t42/capture-live-openapi-v5.py`, SHA-256 `5b4da905037f4ff82bf2d0a24f5da8269561fafbd93ef5300aaab857e40a7be7`. It requires `--execute --expected-head <clean SHA> --expected-jar-sha256 <64-hex> --package-proof <new source-bound JSON>`. It validates JAR bytes, package source/tree, raw live OpenAPI, real login/upload/notification and source/JAR unchanged after cleanup. The c980 `d67d…` JAR and its proof cannot satisfy a new head.
- JDT wrapper: `/tmp/wta-t42/run-with-jdt-paused.py`, SHA-256 `e063af67f85d512ebc30c43474d1d4d32f91fabd9809b14883d7a73c25d2f061`. Invocation is `python3 …/run-with-jdt-paused.py <clean SHA> <alphanumeric unique label> <command> [args…]`. It requires clean HEAD, recognizes at most one Java language server with a current-repository project, records PID/start-ticks/count only, sends STOP, runs the command, and sends CONT in `finally` if the same PID/start-ticks still exist. Inspect `label-jdt-exclusion.json` for `paused`/`resumed` and command exit; a zero-process record means no editor was excluded. Hard termination of the wrapper (especially SIGKILL) is outside Python `finally`; Lead must verify/resume that exact process before continuing. This wrapper checks source before only: the wrapped driver/build proof must verify source after. Use distinct labels; existing records cannot be overwritten.

## Refresh inventory after the fixed candidate exists

1. Lead fixes a full candidate SHA and verifies `git status --porcelain --untracked-files=all` empty, records HEAD/tree, path scope, and no concurrent writer. Run the two **inventory-only** private tools on that exact SHA:

   ```text
   python3 /tmp/wta-t42/freeze-mail-methods.py <SHA> /tmp/wta-t42/dispatch05-mail-methods.json
   python3 /tmp/wta-t42/freeze-regression-manifest.py <SHA> /tmp/wta-t42/dispatch05-regression-manifest.json
   ```

   Both refuse dirty/mismatched source and existing destination. Verify MAIL still has exactly 27 plain `@Test` methods only if actual source proves it; record its fresh test-source SHA. Verify all eight regression classes and current method/parameter counts from the new source; do not assume 135. The regression freezer fails for unsupported parameter sources or renamed/missing classes, requiring deliberate selector/inventory review rather than loosening the gate. Both outputs are inventory, never pass evidence.

2. The MAIL driver source guard expects the fixed `NotifyMailAttachmentIntegrationTest` class plus `@SpringBootTest`, `NamewtaApplication`, `MailNotificationSender`, owned properties and the expected `void` methods. It does **not** pin the production class names currently being moved for the layered fix. Extra tests cannot silently pass because the fresh XML count/method set must equal the supplied inventory. The regression driver pins the eight current class paths, per-class source SHA, method counts, and the SupportedMode source seam. If Dispatch05 renames a selected test class or changes the accepted annotations, stop and review a new private runner/version before real launch. Do not edit a frozen runner or map an old class name to a new one by assertion only.

3. Lead can execute the fresh MAIL and eight-class gates serially using the candidate inventories, each under a distinct JDT wrapper label if editor interference is still present. Concrete argv shape:

   ```text
   python3 /tmp/wta-t42/run-with-jdt-paused.py <SHA> dispatch05mail python3 /tmp/wta-t42/run-notify-mail-attachment-integration-v5.py --execute --expected-head <SHA> --expected-methods <exact value from dispatch05-mail-methods.json>
   python3 /tmp/wta-t42/run-with-jdt-paused.py <SHA> dispatch05regressions python3 /tmp/wta-t42/run-notify-regressions-v1.py --execute --expected-head <SHA> --expected-suite-manifest /tmp/wta-t42/dispatch05-regression-manifest.json
   ```

   Never place a password in these argv values. Each runner generates its own owned MySQL/Redis/MinIO (MAIL) or MySQL/Redis (regression), fresh six-SQL database, private credentials, fresh Surefire XML, zero-skip exact selectors, and checks source before/after. Inspect each result's `acceptance`, exact class/method counts, own label/full container IDs, anonymous volumes, loopback ports, Maven process group, proxy/owned threads, cleanup errors and retained XML hashes. Keep failed attempts separate; a wrapper exit 0 alone is not test evidence.

4. For the live full-app gate, build a **new** full JAR for the candidate (historical command shape `./mvnw -B -ntp clean package -DskipTests`, followed by `bash scripts/ci/verify-admin-bundle.sh full`) while excluding the current-repository JDT builder and prohibiting concurrent Maven. Preserve the old c980 full JAR/proof under its existing uniquely named private artifact path; do not replace that retained copy. Packaging necessarily writes the normal `backend/wta-admin/target/wta-admin.jar`, so compute the new JAR SHA/size and a new private proof containing exact source HEAD/tree, clean build identity, full package command/exit and log SHA. Do not copy the old JAR back or reuse its proof. Then run:

   ```text
   python3 /tmp/wta-t42/capture-live-openapi-v5.py --execute --expected-head <SHA> --expected-jar-sha256 <NEW_JAR_SHA256> --package-proof /tmp/wta-t42/dispatch05-full-package-proof.json
   ```

   Its hardcoded target path is the newly packaged JAR. The runner itself never rebuilds or overwrites it and rejects any before/after hash or source mismatch. A new formal fetch/generate/check decision must be based on the captured candidate bytes; do not attribute old c980 OpenAPI/source proof to Dispatch05.

This preflight does not execute a candidate. The only complete historical cleanups belong to their original run IDs and source SHAs; each new run must prove its own owned container/volume/port/process cleanup even if its test assertions pass.
