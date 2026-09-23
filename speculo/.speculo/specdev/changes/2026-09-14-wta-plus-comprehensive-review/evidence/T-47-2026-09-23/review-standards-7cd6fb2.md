# T-47 final standard-axis review — 7cd6fb2

- Fixed base: `97e1ee9e1ad40de75deffd025379a6a5488c4882`
- Fixed head: `7cd6fb22b28b7d464b9648ba77f2a2308e5b2016`, direct child of `2d60e1af730036837576685a56657bf630a391cb`.
- Three-dot diff: `git diff base...head -- frontend`; only `OssPage.vue` and adjacent `page-query.test.ts` in `frontend/packages/web-domains/system/src/oss/`. `git diff --check base...head -- frontend` exited 0.
- Verdict: **pass** on the independent standards axis; no actionable finding from this fixed diff. The earlier 2d60 report remains intact.

Applied the applicable root/frontend/web-domain AGENTS, engineering-standards and scoped TypeScript/Vue/browser/CRUD/error-security rules, the domain OSS URL contract, and the code-review Skill with its source-discovery/Fowler/risk/reviewer-contract references. No specification-axis findings were read or reused.

The incremental change moves optional previews after the owned list commit. At `OssPage.vue:407-436`, a current generation commits config, rows, total, and cleared preview maps before starting authorization requests; `queryLoading` is finalized without waiting for image URLs. `hydratePreviewUrls` at `:387-407` supervises each promise with a catch and checks the same generation/disposed owner before either authorized URL or deleted marker is published. This preserves the previous old-query and unmount guards while preventing a hung image authorization request from blocking a usable list or a mutation refresh. Pending rows still skip URL lookup through `ossFilePresentation`; domain list projection still clears untrusted stored URLs and validates each download URL. No new cross-package import or mutable global owner was added.

`page-query.test.ts` now covers list-first completion, individually resolving images, an indefinitely pending preview, mutation-refresh completion, stale preview rejection, and unmount after preview launch. Lead reports affected package tests 73/0 skip plus typecheck and lint passed; I did not run them. Full frontend gates and actual UI/browser evidence remain Lead-owned. The SFC test intentionally replaces the render function, so it validates reactive state but not the new `el-alert` markup visually; a screenshot or browser interaction remains delivery evidence. No services, build, E2E, or tests were run for this review.
