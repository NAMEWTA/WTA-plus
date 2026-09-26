# Dispatch Packet T-33-20260923-01

operation=dispatch；task_kind=implementation；delivery_channel=native；Lead=/root (single-agent)；provider=gpt-6-sol / reasoning=xhigh；implementation owner=/root/cors_audit。

- Fixed base: `dd8179e1cd394b092bfb36b8b91c0e04386563ce`；repository=/srv/WTA-plus；branch=main；workspace_ref=current；parent=main。
- Strategy current/direct-parent；serialization lock T-33 product writer only；no new worktree。
- Read order: tickets-map.md → .agents/skills/engineering-standards/SKILL.md + .agents/skills/wta-common-modules-guide/SKILL.md (scope refs/AGENTS) → ticket/33-safe-cors-defaults.md → goal-plan.md/spec AC-033/ADR。
- Inputs live under speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review; Map revision140，Goal run active。无blocked_by，T-32证据可回读但不是依赖。
- IN: exact explicit origins, default empty, no credential wildcard reflection, preserve preflight/same-origin/proxy behavior, profile binding, SSO cookie regression and docs.
- OUT: OIDC change, bucket/security policy changes, public API protocol migrations, external services/config, unrelated refactor。
- Writable: Ticket T-33 expected_changes/writable_paths list; sole product writer in those paths。No POM/dependencies/frontend product scope without Lead preapproval。
- Read-only: all SpecDev documents including status/evidence; adjacent OIDC; all other product files。Lead may update governance concurrently, not product; this expected governance difference is not conflicting baseline drift。
- Allowed: local product/test/doc edits and non-E2E targeted compile/unit/static tests; no commits, push, deploy, credential read/write, services mutation. Lead creates product commit and owns required current-workspace E2E after return。
- Plan: first red reproduces hostile Origin or unsafe startup, then minimal property/filter/YAML implementation. Reject wildcards under exact-only contract, blank list is no cross-origin; preserve explicit dev proxy origins based on actual5177/5175/4176. Verify origin-null/crafted URLs, explicit/no credentials, trusted/untrusted preflight, same-origin, real config profile. No new deps if existing embedded servlet test scaffolding available。
- Non-E2E: targeted common CorsPolicyTest + new profile test compile/run + relevant existing SSO unit tests, no skip; logs to /tmp/wta-t33 only. Lead runs real HTTP/SSO E2E and release regressions; return exact selectors.
- Stop: new hard contract/Skill/scope need, product baseline drift/other writer, dependency/policy conflict; report then wait. Do not ask user directly。
- Return: Ticket ID,workspace,base/current HEAD,commit=null pendingLead,git status/paths,red-green commands/cwd/exit/count/skip,evidence logpaths,skill execution trace,unverified E2E and limitations/recovery. Do not mark done or write SpecDev。
