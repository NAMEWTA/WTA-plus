# T-34 specification review — request-changes (isolated acceptance driver)

Fixed base `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`; fixed head `e78d886119d773f47ac985762a5459d6a1832609`; merge base equals base. Reviewed `git diff base...head -- frontend` and its four fixed commits. Relative to the previously reviewed `24909df0` product and browser-test source, this commit adds only `frontend/e2e/run-inbox-real.py` and `frontend/e2e/inbox-real.md`. No standards-axis findings were read. I ran no services, tests or builds and changed no repository files.

Normative sources: this change's `spec.md:97` (AC-034); `ticket/34-inbox-without-realtime.md:34-35,68-85,97-110,123-127,139-147`; `ADR.md:1-24` (T-41 retains full pagination); and `tickets-map.md:77,152,217-223` (scope/ownership/recovery). The prior S34-1 product finding remains closed: Navbar open uses `initMessageBox(true)`, and the controlled UI test requires the post-open result. The new driver's own MySQL/Redis containers, loopback backend/Vite, isolated baseline seed, real login, exact inbox title/body, and zero browser push-request assertion are directionally aligned with AC-034 (`run-inbox-real.py:176-265`; `inbox-real.e2e.ts:9-47`). Runtime success is still pending independent execution evidence.

## Finding S34-2 — medium — reported test hash is not bound to the test actually executed

**Location:** `frontend/e2e/run-inbox-real.py:115-118,154-169,266-288` and `frontend/e2e/playwright.inbox-real.config.ts:4-11`.

The driver accepts `--playwright-test`, resolves and hashes that file into `result.artifact.playwright_test_sha256`, but never passes the resolved `test` to Playwright or checks the JSON reporter's selected file/title. The command selects only a caller-supplied config and grep, while `--expected-tests` is also caller-supplied. With the documented defaults the dedicated config selects the intended real test, but a mistyped/overridden test path can be reported as the attested source while another one-test suite runs; an overridden config/grep can satisfy the count gate without exercising the required real Admin login and inbox assertions. The result JSON records counts and hashes, not the identity of the executed test. This permits a false AC-034 required-E2E claim despite exit 0.

**Fix condition:** Freeze the required config/test/title/count for this T-34 gate, or pass the resolved test path to Playwright and independently validate the reporter's executed file and exact title against it and `TEST_NAME`; record that identity in `result.json`. Do not allow count or grep overrides to weaken the required test. Keep the default no-mock real test and zero-skip assertion.

## Finding S34-3 — medium — early process-group leader exit can leave owned children behind

**Location:** `frontend/e2e/run-inbox-real.py:95-103,242-275,292-320`.

Backend, Vite and Playwright are started as separate process groups; Chrome and Vite may create children. `stop(proc)` immediately returns when the recorded group leader has already exited (`proc.poll() is not None`), so a surviving owned child receives no termination. Cleanup checks only backend/Vite and Docker mapped TCP ports, not the Playwright/Chrome process group; `result.exit_code` can remain 0 while an owned browser child survives. A Docker `run` that created a labelled container but timed out before its ID was captured is likewise only detected by the final label query, not removed. This conflicts with the driver/documentation promise to recover its own resources after failure.

**Fix condition:** Terminate or verify all owned process groups even if their leaders exited, using a bounded, ownership-safe check; include residual group/process state in the result and fail if any owned child remains. Sweep only this run ID's Docker labels to recover a container whose ID was not captured, then confirm no matching IDs remain. Ensure cleanup continues through individual stop/log errors and records incomplete cleanup as failure. No existing or deployment resources should be targeted.

The script sets backend `MESSAGE_ENABLED=false` and frontend `VITE_APP_MESSAGE_ENABLED=false` (`:227-265`); checked backend condition sources use `message.enabled` (`MessageTransportCondition.java:31-34`, `MessageAutoConfiguration.java:11-14`). That supports, but does not itself replace, a real run showing the backend disabled and the browser's zero push requests. This report does not claim the pending run passed or failed.
