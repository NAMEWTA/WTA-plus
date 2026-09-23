# T-47 specification-axis review: final fixed head

- Result: **pass** for the specification axis at this fixed point. The P2 finding in `/tmp/wta-t47/review-specification-2d60e1a.md` is closed by the new delta; that historical request-changes record remains unchanged. This review did not read or use the standards-axis result.
- Base: `97e1ee9e1ad40de75deffd025379a6a5488c4882`; head: `7cd6fb22b28b7d464b9648ba77f2a2308e5b2016`. Merge base equals base. Diff command: `git diff 97e1ee9e1ad40de75deffd025379a6a5488c4882...7cd6fb22b28b7d464b9648ba77f2a2308e5b2016`. Commits: `2d60e1a fix(system): apply only the latest OSS page query`; `7cd6fb2 fix(system): keep OSS lists usable while previews load`.
- Product diff is limited to `frontend/packages/web-domains/system/src/oss/OssPage.vue` and `page-query.test.ts`. Authority: original review O-04, current Spec AC-047, Ticket 47 sections 4/6/8/10, verification.md T47; method: code-review skill, source-discovery, risk-review, reviewer-contracts.

## Closure of prior finding

`OssPage.vue:417-427` now checks generation/alive immediately after the list response and commits current rows, total, preview mode, empty preview maps and table visibility before optional URL requests. `getList` returns and `queryLoading` clears in `finally` without waiting for any image (`:427,432-434`). `hydratePreviewUrls` requests eligible active images independently; every success/deleted-error state write first checks the same current-generation/alive closure (`:387-403`). Thus a permanently pending image cannot withhold a successful current list; a late A URL or error cannot modify B or a disposed component. Tests now explicitly hold a preview unresolved while asserting list/total and `getList` completion (`page-query.test.ts:327-341`), and exercise two images resolving out of order (`:311-325`). The A→B test changed its old blocking assertion to require A rows visible before preview and B ownership after A preview arrives late (`:147-170`). This meets the fix condition recorded at 2d60e1a.

## Other AC-047 paths checked

- The filter/page/sort and nested date-range query are snapshotted before config I/O (`OssPage.vue:413`, tests 116-132, 263-280). Old config cannot issue a list after replacement (`:414-418`, tests 134-145).
- A stale list/failure has no page write; current failures expose error, the next query clears it, and only the current `finally` ends query loading (`:409-434`, tests 103-114, 173-217). Mutation loading remains an OR of independent query/mutation flags (`:305-308`, test 246-261).
- Unmount invalidates generation (`:358-363`) and the test covers pending config, list and now independently pending preview (`page-query.test.ts:220-244`). It checks prior rows and total are unchanged as well as no later preview/error backfill.
- The unchanged domain OSS list projection clears untrusted list URLs (`resource-service.ts:165-168`); only `downloadUrl` supplies validated current image previews. PENDING rows do not request a URL (`OssPage.vue:390`), ordinary preview failure leaves the current list intact and blank, deleted failure marks deleted (`page-query.test.ts:147-170,282-309`). Existing `presentation.test.ts` covers PENDING/deleted presentation. Template permission directives and paging/sorting controls have no product diff.
- The test mounts real SFC setup and domain service but replaces template rendering; rendered permission behavior is therefore a static non-regression check here and remains a T-30 page-composition acceptance obligation.

## Evidence limit

This is a read-only fixed-commit code/contract review. I did not run tests, build, services or edit the repository. The Lead reports 73 package tests with zero skips and applicable typecheck/lint; those results and full-frontend gates are Lead-owned execution evidence, not independently reproduced in this review. No further specification finding was identified at the fixed head.
