# T-47 specification-axis review (fixed point)

- Result: **request-changes** (one P2 behavioral finding). This is the specification axis only; it has not read or used the standards-axis result.
- Fixed base: `97e1ee9e1ad40de75deffd025379a6a5488c4882`.
- Fixed head: `2d60e1af730036837576685a56657bf630a391cb`.
- Diff: `git diff 97e1ee9e1ad40de75deffd025379a6a5488c4882...2d60e1af730036837576685a56657bf630a391cb`; merge base equals fixed base; one commit, `2d60e1a fix(system): apply only the latest OSS page query`.
- Product delta: `frontend/packages/web-domains/system/src/oss/OssPage.vue` and new `frontend/packages/web-domains/system/src/oss/page-query.test.ts`. The same fixed diff also carries ticket/evidence bookkeeping, not treated as product behavior.
- Authority: `temp/WTA-plus-review-64b4ea7.md` O-04 (especially lines 382-391); current change `spec.md` AC-047 (line 110); `ticket/47-latest-oss-query-wins.md` sections 1, 4, 6, 8, 10; `verification.md` line 52. Review method: `speculo/workflows/specdev/common/skills/code-review/SKILL.md`, source-discovery, risk-review, reviewer-contracts.

## Finding 1 — P2: preview URL latency now blocks the already successful OSS list

**Location:** `frontend/packages/web-domains/system/src/oss/OssPage.vue:391-407,422-439`; newly added `page-query.test.ts:146-153` asserts this blocking behavior. Baseline `OssPage.vue:380-389` committed rows/total and ended table loading immediately after the list response, then fetched image previews separately.

**Reachable trigger and impact:** Enable previews and return a page containing an active image. Let the config and `/resource/oss/list` requests succeed, but keep one image `/download-url` request pending or slow. `resolvePreviewUrls` waits for `Promise.all` of every active image, and `getList` waits for it before writing `ossList`, `total`, `previewListResource`, or ending `queryLoading`. On initial load the already available rows remain absent; on a subsequent query the previous page remains displayed under the spinner. If that preview request never settles, the latest successful list never becomes visible and the loader never ends. The new controlled-Promise test explicitly expects an empty list and loading=true after the list has returned, so it does not catch the regression.

**Contract:** AC-047 and Ticket 47 require latest-owned rows/total/preview/loading and say the existing preview lifecycle must not regress. O-04's minimal remedy is to verify generation before committing page state; it does not require a preview endpoint to succeed before showing already returned list metadata. This introduced dependency is a practical regression for slow or unhealthy storage and also delays mutation-success refreshes that await `getList()`.

**Fix condition:** After a successful current-generation list response, commit its rows/total/config preview mode and reset the preview maps for that generation. Finish list loading independently of optional preview hydration. Apply later authorized preview URL/deleted markers only after another current-generation-and-alive check; ignore old responses and failures. Add a negative controlled-Promise case that leaves an image preview unresolved while asserting the latest list/total is visible and list loading settled, then resolves the old preview after a newer query and verifies no overwrite. Keep the existing no-list-URL, PENDING/deleted, and authorization failure assertions.

## Checked behavior without further finding

- Config resolution is held in a local value; a stale config cannot launch or commit a stale list (`OssPage.vue:419-423`, `page-query.test.ts:133-144`).
- Query including nested params and date range is snapshotted before the first await, preserving filter, page, and sort intent (`OssPage.vue:418`, `page-query.test.ts:115-131,257-274`).
- A list response, preview result, or exception checks generation and disposal before writing the page; stale `finally` cannot clear current query loading (`OssPage.vue:414,423,426,434-439`, tests 103-113, 146-189, 218-238).
- On current success, rows, total, preview maps and preview switch come from one generation; on current failure, a visible error is set and an empty successful retry clears it (`OssPage.vue:425-440`, tests 191-204). Mutation loading is kept distinct from query loading (`OssPage.vue:305-308`, test 240-255).
- List transport strips list URLs; current preview uses the validated `downloadUrl` service only for active images, skipping pending objects; ordinary URL failure stays blank and a deleted failure marks deleted (`resource-service.ts:157-168`, `OssPage.vue:387-407`, tests 146-169, 276-298). Permission directives and action predicates in the template are unchanged by this diff.
- `git diff --check` on the fixed frontend diff returned 0. No repo edits, services, builds, or tests were run by this reviewer. Parent reports 70 package tests with zero skips plus package typecheck/lint; those results were not independently executed here. Full frontend gates remain with the Lead. The test mounts real SFC setup and domain service but overrides rendering, so it does not itself exercise rendered permissions; T-30 owns the combined browser regression.
