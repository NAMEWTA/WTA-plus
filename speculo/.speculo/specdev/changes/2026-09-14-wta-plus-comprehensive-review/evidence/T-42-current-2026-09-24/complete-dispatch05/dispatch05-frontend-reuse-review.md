# Dispatch05 frontend input reuse and fresh backend build review

Conclusion: the 73c28 frontend gates can be **reused by exact input equivalence**, with their original source/run coordinates retained. They were not rerun at d95. The new d95 full backend build has its own clean-source and JAR proof. Real Mail/Notify services are currently executing under Lead, and a fresh d95 full-JAR HTTP/OpenAPI run is still needed before claiming public contract parity or T42 completion.

## Fixed inputs and scope

- Prior frontend source: `73c28e751f6276e45b0488ab5d8eacbf9f0c77fb`, clean tree `748f1b22d4d8617582cbca01fc9eb70e289b35eb` in `final04-frontend-source-after.json`.
- New candidate: `d95b464e46ec73d7a913809f4c84332a7f2eeffb`, tree `39f18706226fed11ed19b5cdc1f822d60492350c`.
- Independent `git rev-parse <commit>:<path>` and `git diff --quiet 73c28..d95 -- <path>` checks agree: `frontend` tree `2ff7b932a58a4a097ac2a1c14f77776eb916e474`, `.agents` tree `a592be629442722dd932e8ca673771897ed5e039`, `scripts` tree `58f0666836e2b3849f2f9a45bb0207e44e0a910b`, and `release-artifacts` tree `14c507cd668de5f2fd418dc5e1714f9e0c81bb09` are each identical at both commits. Across 658 changed paths, 11 are under `backend/`, 647 under `speculo/`, and none elsewhere. Thus frontend source, test/config/lock inputs and generated transport files in `frontend/`, plus the named shared quality scripts and release contract inputs, did not change. The new backend runtime behavior is not inferred from that tree equality.

The prior [frontend count record](/tmp/wta-t42/final04-frontend-counts.json) reports 663 Vitest plus 108 Node = **771** passing tests at 73c28. Its [artifact manifest](/tmp/wta-t42/final04-frontend-build-artifacts.json) records admin 311, home 14, SSO 4 = **329** build files; the previous frontend architecture/lint/typecheck/OpenAPI/build command records all have exit 0. These are original 73c28 results, reused for d95 only because their complete relevant inputs are byte-identical. Do not rewrite their source or time as d95 execution.

## Fresh d95 backend proof

`dispatch05-targeted-source-and-counts.json` reports 10/0 fail/0 error/0 skip. `dispatch05-backend-default-counts.json` reports 265 fresh suites, 1,184 tests, zero failures/errors, 244 environment skips, hence 940 executed passes. This default count is not a substitute for the opt-in real Mail/Notify gates currently run by Lead. `dispatch05-source-before.json` and `dispatch05-source-after.json` are identical clean d95/tree39f records.

`dispatch05-full-package.json` and `dispatch05-full-bundle.json` both record exit 0. The [new package proof](/tmp/wta-t42/dispatch05-full-package-proof.json) binds the actual `./mvnw -B -ntp clean package -DskipTests` command, clean d95 HEAD/tree, exit 0, full-package log SHA and JAR SHA/size. I independently hashed the private `/tmp/wta-t42/artifacts/dispatch05-full/wta-admin.jar` copy: its bytes and size match both that proof and `artifacts/dispatch05-full/manifest.json`; the log SHA matches the retained full-package log. The old c980 JAR/proof is not used here. `dispatch05target-jdt-exclusion.json` and `dispatch05default-jdt-exclusion.json` each record wrapper exit 0 and **zero** captured editor processes: they establish no paused process remained in those invocations, but do not themselves prove that a nonzero editor was stopped/resumed.

## Remaining contract boundary

The c980 HTTP result and 73c28 generated frontend source establish the **previous** live OpenAPI shape only. The layered backend refactor can change runtime assembly or responses despite unchanged frontend files. After current Mail 27 and Notify eight-class real gates finish, Lead must use the freshly packaged d95 JAR with its exact new package proof for the full-app HTTP/OpenAPI capture. Compare its real response against the prior generated contract; if formal fetch/generate changes any frontend source or generated type, this byte-equivalence reuse ceases and the affected frontend gates must run on the new input. Until then, no d95 live HTTP parity or whole-T42 pass is claimed.

This review is read-only and did not poll the active real-service session, run builds/tests, or modify the repository.
