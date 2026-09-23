# T-47 standard-axis review

- Fixed base: `97e1ee9e1ad40de75deffd025379a6a5488c4882`
- Fixed head: `2d60e1af730036837576685a56657bf630a391cb` (direct child; `fix(system): apply only the latest OSS page query`)
- Diff: `git diff base...head -- frontend`; only `frontend/packages/web-domains/system/src/oss/OssPage.vue` and `page-query.test.ts`. `git diff --check base...head -- frontend` exited 0.
- Verdict: **pass** on the independent standard axis; no actionable code finding in the fixed diff. This is a static review, not a claim that Lead's full frontend gates or browser evidence have finished.

## Sources and checks

Applied the repository root, `frontend/`, and `frontend/packages/web-domains/system/` AGENTS; engineering-standards entry and scoped TypeScript/Vue/browser/CRUD/error-security references; namewta-fullstack-development frontend boundary; and `speculo/workflows/specdev/common/skills/code-review/SKILL.md` with source-discovery, Fowler, risk-review, and reviewer-contracts references. Checked package test/lint/typecheck scripts, page registration, domain OSS service, and existing OSS presentation/URL validation. Specification-axis results were neither read nor used.

`OssPage.vue:410-440` snapshots nested query values and date range before the first await; config, list, and authorized preview results are staged locally and committed only if the same generation still owns the page. Old responses and failures cannot clear the new request's loading/error state; `onScopeDispose` at `:356-363` invalidates pending work. `queryLoading` and `mutationLoading` remain separate, so a list completing during a mutation cannot turn the table spinner off. Current query failure is visible and a successful empty retry clears it. Mutation call sites at `:540-605` only report a completed refresh when `getList()` applied; search, sort, pagination, reset, and upload refresh all call the same generation-guarded query.

Authorized URL handling remains within the existing domain boundary: `resource-service.ts:165-168` blanks stored list URLs, `:157-162` validates a separately requested download URL, and the page's `resolvePreviewUrls` at `:386-408` only requests active image rows. Pending rows do not request URLs. Old preview results are not committed to new rows, and the supplied tests at `page-query.test.ts:126-299` cover the list/config/preview races, nested snapshot, current/stale failures, unmount at each stage, mutation loading, caller query values, and URL sanitization.

## Verification limits

Lead reports 70 affected tests with zero skips plus typecheck/lint passed; this reviewer did not run those commands, services, build, or E2E. The new tests mount the SFC setup with a no-op renderer (`page-query.test.ts:84`), so they prove state behavior but do not render the new `el-alert` or verify visual/focus behavior. A browser interaction or screenshot remains the appropriate delivery evidence for that small UI addition. Existing bounded HTTP timeout and URL authorization are delegated to the unchanged domain/browser adapters. No unrelated SFC cleanup or style-only finding was raised.
