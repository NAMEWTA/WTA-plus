# T-34 standard-axis review — 7c98383

- Base: `6fcbfeb50747b67bcaf4bf9af1d964cc7d760c04`
- Head: `7c983838c1de52aa086d8335b1d33684da1cb83e`
- Scope: `git diff base...head -- frontend`; reviewed fixed commit source, applicable AGENTS and engineering/fullstack standards, and code-review skill's source-discovery, Fowler, risk, and reviewer-contract references. No other review axis used.
- Verdict: **request changes**.

## Blocking finding

`frontend/e2e/run-inbox-real.py:161-165,240-243,319-325,360-365` — The driver captures full container IDs returned by `docker run -d`, but `owned_container_ids()` calls `docker ps -aq` without `--no-trunc`. Docker's default `ps -q` output is shortened, so every captured full ID is absent from `discovered`, and cleanup reports `captured_container_not_found_with_exact_labels` even after the correctly labelled containers were removed. `gate_exit_code()` then changes an otherwise successful browser run to exit 1. Add `--no-trunc` to discovery (or normalize IDs by inspect), and cover captured IDs and short-ID behavior in the no-Docker regression. Existing `test_recovers_uncaptured_container_from_both_exact_labels` supplies full IDs from its fake Docker and passes an empty captured list, so it does not catch this case. Severity: **blocking for acceptance gate**; resource deletion still verifies exact labels.

## Other review results

The earlier review's process-group, failure-independent cleanup, and Playwright source-selection concerns were addressed in this fixed head: exited leaders no longer hide live child processes; cleanup exceptions accumulate into a failing verdict while remaining cleanup continues; the dedicated file/config/title are selected and checked against the JSON reporter. The fixed test uses a private random run directory, exact run and owner Docker labels, loopback service ports, synthetic owned data, JAR hash and backend/SQL input equivalence checks. The doc's described cleanup gate is currently contradicted by the ID-length defect above. No further blocking standard-axis product finding was identified from this static inspection.

`git diff --check base...head -- frontend` exited 0. No services, Maven, build, browser, or Python tests were run by this reviewer. Lead reports 9 no-Docker tests passing; this reviewer did not independently run them. Real E2E and actual cleanup were unverified at the time of this fixed-point report. Raw historical log whitespace is outside product findings.
