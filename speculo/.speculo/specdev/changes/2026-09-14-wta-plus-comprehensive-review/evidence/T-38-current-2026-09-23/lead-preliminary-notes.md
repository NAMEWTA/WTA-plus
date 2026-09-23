# T38 Lead pre-candidate observations

2026-09-23; incomplete working source after test-only9f8fa2b, not a formal review/pass. Writer retains the sole product lock. No formal acceptance candidate yet.

- Accepted N05/Ticket allows safe local IN_APP UNKNOWN restoration through T36 same Intent/message/recipient transaction and idempotency. Do not generalize external UNKNOWN refusal to all local UNKNOWN. Packet/Ticket revision164 clarified before implementation; synthetic existing-message/no-double-push and missing-relation recovery must be real DB tests.
- Preserve terminal CANCELLED Intent even when it contains local FAILED/DONE deliveries; manual retry must not silently resurrect canceled work. Writer notified to return0 or refuse and test.
- DAO lockOutboxes should scope both Intent and Delivery IDs rather than locking foreign malformed relationship rows under the current Intent lock. Recheck selected delivery and null current-read failure explicitly; avoid List.of(null) NPE. Writer notified.
- UI cancel is notification/intent-wide whereas retry is per delivery. Wording and confirmation must reflect scope. Delivery PENDING exists independently from old frontend NotificationStatus union.
- Correct existing admin authorization is per-action permissions plus shared login/Client-token/access_path checks. intent.appId is a business namespace, not the authenticated Client or owner; no new owner model.
- Red is real: fixed clean9f8fa2b,1test1failure0error0skip at verifyNoInteractions, productionController.retry:48 called Service for path101/body202. Later expected-ServiceException assertion aborted; this is standalone MVC, no full auth/DB claim.
- Current initial build request deferred until new real DB test is complete/frozen, avoiding Maven compiling a changing source tree. Lead controls build/service windows.
- Test-only commit command unnecessarily set core.hooksPath=/dev/null. Follow-up audit found no configured path and no executable non-sample hooks; no active gate was skipped. Do not repeat override. Audit saved test-checkpoint-hook-audit.json.
