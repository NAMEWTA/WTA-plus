# Dispatch04 private evidence stage

Source: clean `c980698b794fba9b02d0041207f57c7b5aa77bb7`.

The three-contract targeted gate passed 8 tests. The first default gate then reported 1,184 tests, 9 failures, 0 errors and 244 environment skips. Its failed XML and logs remain here as a separate checkpoint. During that time window a concurrent editor builder was active. After temporarily pausing the shared editor JDT work and running a clean build, the second default gate reported 265 fresh suites, 1,184 tests, 940 executed passes, 244 environment skips, and no failures or errors; the full package and bundle commands also exited successfully. The JDT exclusion record says it was resumed. This sequence supports an interference diagnosis, but no per-write trace establishes a unique cause for each failure.

The original failed attempt is retained, not retroactively counted as a pass. The full JAR is intentionally omitted: only its build proof and artifact manifest are included. HTTP, core, frontend, and later gates are outside this stage and may be added separately by Lead. Review files retain their original review-time scope.

`manifest.json` records each source and retained SHA-256, the count of JWT-shaped replacements in logs/XML, and count-only secret-indicator scans. No raw environment files, credentials, properties, binaries, or JDT project lists were copied. The indicator scan is a screening aid, not proof that arbitrary text has no secrets.
