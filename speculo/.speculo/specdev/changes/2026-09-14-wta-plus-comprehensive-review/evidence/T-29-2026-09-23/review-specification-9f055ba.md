# T-29 specification-axis review

- Fixed base: `7a1810288d3292ceeb4987f488b13353af1a1286`.
- Fixed head: `9f055ba15d9d5a828fb08cdfb0b24efa32642889` (tree `02f01778a803ad46e6cbdb1e411a8538c7869c5e`; Lead's `/tmp/wta-t29/acceptance-before.json` records clean workspace at this exact point).
- Diff: `git diff 7a1810288d3292ceeb4987f488b13353af1a1286...9f055ba15d9d5a828fb08cdfb0b24efa32642889 -- .agents/skills`; 12 product files, 47 insertions/28 deletions. Commit: `9f055ba fix(docs): align private release paths and current skill facts`.
- Axis: specification only. I did not use the standard-axis report, run a product gate, read private contents, or perform private migration. My earlier authorship of the `/tmp` migration helper limits independence for private-operation assessment; Lead owns its real validation.
- Sources: `speculo/.speculo/specdev/changes/2026-09-14-wta-plus-comprehensive-review/spec.md` AC-029, `ticket/29-converge-current-documentation.md` sections 8/10 and 148–158, `tickets-map.md` T-29/AC-029, `ADR.md` ADR-CR-006, `source.md`, historical `evidence/T-29-*.json`, `/tmp/wta-t29-current-audit-v2.json`, Lead-owned `/tmp/wta-t29/acceptance-*.json` and `/tmp/wta-t29/private-final-check.json`. No remote issue source applies.

## Result: pass, with verification ownership stated

No specification finding in the fixed 12-file product diff. This is a contract review of the current candidate, not a claim that I personally reran gates or inspected private records.

| AC-029 clause | Current evidence and conclusion |
|---|---|
| Every removed file has owner, destination, no lost hard constraint | Historical `T-29-originals.json` and current audit enumerate 37 removed handbook files; current audit verifies 37 original hashes and 37 owner/destination rows, 28 special rules, zero remaining references and zero errors. All rows route hard rules to `backend/AGENTS.md` and navigation to the module map, with the one effective-owner delta explicitly reviewed. **Covered**. The audit flags a changed protected root `AGENTS.md` but records it as reviewed blank-line-only; it is outside this fixed product diff. |
| POM/App/Java test-source inventory reflects final source | Current-head audit ran three `rg --files` inventories with exit 0: 49 backend POM files, 3 frontend App package files, 296 Java test source files. The project profile and module map now derive counts from current files instead of asserting the historical 50/3/247 baseline; they explicitly distinguish source files from JUnit case totals. **Covered at 9f055ba**. |
| Current references and cwd commands resolve | Current-head audit reports 147 checked links, zero broken links/errors, 48 module rows with no test-root mismatch, and zero references to removed handbook paths. The changed deploy/customization examples consistently use `temp/release`; their invoked script paths exist in the repo. `release-state.mjs` exists at `release-artifacts/scripts/release-state.mjs`, matching the corrected project profile. The fact checker now scans **all** Skill Markdown for obsolete `temp/relase`, and added negative fixtures for both deploy and customization Skills. The `temp/release` input files are generated/private operational inputs, not tracked source prerequisites. **Covered**. |
| No candidate CI/unrun service/unapproved design claimed complete | Project profile continues to label GitHub Actions as candidate until actual remote execution and required branch protection; App/build/deploy states remain distinct. The 12-file diff changes path facts, one stale script extension and fact-check scope, without adding claims of remote CI or deployment success. **Covered**. |

The current conflict that triggered this ticket is addressed on both sides: deployment and customization Skills now point to `temp/release`; `validate-skill-facts.mjs` preserves the ignored/zero-tracked/old-directory-absent checks and broadens stale-path scanning instead of relaxing it. Lead's private-operation summaries record second prepare and move exit 0, 33 entries, and post-move checks true for unchanged entry set, restricted permissions, backup/rehearsal, ignored/zero-tracked paths, and absent old path. I treat these only as Lead-owned evidence and did not inspect its private manifest or contents.

Lead's clean-head acceptance records exit 0 for current facts checker and unit fixtures, handbook gate and unit fixtures, FreeMarker validation, deploy Skill unit tests, release-artifacts verification, frontend architecture/OpenAPI checks, Maven validate and diff check. These are reported from `/tmp/wta-t29/acceptance-*.json`; this reviewer did not rerun them. Final ticket status/direct-parent/result registration remains Lead's integration responsibility.

## Independent `/tmp` synthetic helper record

Saved executable source: `/tmp/wta-t29/synthetic-private-move-test.py` (injects `fake_git`, creates only `tempfile.TemporaryDirectory` fixtures); command: `python3 /tmp/wta-t29/synthetic-private-move-test.py > /tmp/wta-t29/synthetic-private-move-test.log 2>&1`; exit `0`. Full stdout is preserved in `/tmp/wta-t29/synthetic-private-move-test.log`: AST pass and **9/9** cases (shared 1000:1000/0755 parent plus root/02700 source prepare→move; repo/temp writable and owner negatives; move owner drift; symlink; source mutation; existing target). This is helper evidence only, not independent proof of the real private move.

## Limits

I did not rerun any repository gate or inspect actual private file contents, report pointers, or server state. The private filesystem transaction is supported only by Lead's metadata-only records. The current-source audit's 49/3/296 counts are exact at the fixed head and need refresh if subsequent tickets change source. No required E2E is specified for AC-029.
