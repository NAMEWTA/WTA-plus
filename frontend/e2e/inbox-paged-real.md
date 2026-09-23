# T-41 真实 Admin 收件箱浏览器验收

The standalone `run-inbox-paged-real.py` is the T-41 browser gate. Its offline tests never start Docker, MySQL, Redis, MinIO, Java, Vite or Playwright. Lead runs the real gate only after the final product commit and full package are fixed. The runner derives a numeric-only source location for a failed assertion before rejecting the run and deletes the raw Playwright reporter and browser artifacts.

The dedicated Playwright test must be exactly `T-41 real Admin inbox page 26 and global read-all`, in project `chromium`, with one worker and zero retries. Child environment names are `T41_ADMIN_ORIGIN`, `T41_BACKEND_ORIGIN`, `T41_A_USERNAME`, `T41_A_PASSWORD`, `T41_B_USERNAME`, `T41_B_PASSWORD`, `T41_SEED_MANIFEST`, `T41_RUN_ID`. The runner generates both passwords, writes BCrypt `$2a$` hashes only to the freshly owned DB users, and passes the plaintext only in the private browser child environment. It needs the local Python `bcrypt` module; no parent or production password is supplied. The test reads the 0600 JSON seed manifest: `schema_version=1`, `run_id`, `a/b` each with string `userId` and numeric `total/unread`, string `messageId/title` pairs for `aOldest/shared/bOnly`, and `pageNum=26`, `pageSize=20`.

The runner requires the final **clean exact HEAD**, full admin JAR SHA-256 and a full clean package proof. It starts no service without all three and an explicit `--execute` flag. Example with placeholders only:

```bash
python3 frontend/e2e/run-inbox-paged-real.py \
  --execute \
  --expected-head <final-40-hex-commit> \
  --expected-jar-sha256 <full-jar-64-hex-sha256> \
  --package-proof /tmp/wta-t41/<private-full-package-proof>.json
```

Run only after all build/generation jobs have finished. Its isolated resources use `namewta.test.owner=T-41-BROWSER` and a random run label, local Docker socket, pinned images, an owned six-SQL schema, DB-scoped app user, Redis config and MinIO credentials in 0600 private files, a loopback `dev` full-JAR backend, Vite and Chrome. The Docker initializer is constrained to the owned MySQL full ID. It captures anonymous volume names before removal, verifies full process groups/ports/volumes and checks source/JAR bytes after cleanup. Reports land at `/tmp/wta-t41/browser-runs/<run-id>/result.json` and `playwright.json` (0700 parent, 0600 files). The latter contains only exact reporter identity/counts and a raw SHA, never the raw reporter. The runner refuses configs without `trace/video/screenshot: 'off'` and source that persists storage state or HAR; raw reporter, process logs and browser artifacts are deleted even on failure. No token, login body or raw browser request belongs in a committed Evidence file.

Seed: A has 501 inbox relationships, 481 unread; B has 2 unread. One message is shared and one is B-only; 502 synthetic messages yield 503 recipient relations. The final six-SQL DML deletes B's legacy role, so the runner creates a **new owned Admin-client role** with only the inbox menu/seen/read permissions and binds B to it. All messages have the same `create_time`; a B-only ID is interleaved near A's oldest rows so an implementation that paginates globally before filtering by owner produces two A rows on page 26 instead of one. The top 20 A rows start read, and the oldest A row starts seen **one day before** creation, making overwrites detectable even within the same execution second. The runner compares those precise timestamps after global read-all. It also snapshots both B rows' exact seen/read values, rather than accepting only unchanged B unread count. The browser must assert exact HTTP/page identity, A/B permission isolation, one A oldest row on page 26, global read-all of all A rows including off-page items, B unchanged, no push requests. The runner independently checks SQL before and after browser actions.

离线安全检查（不启动服务）：

```bash
python3 frontend/e2e/test_run_inbox_paged_real.py
python3 -m py_compile frontend/e2e/run-inbox-paged-real.py
```

`--preflight` is static only and is **not** a real acceptance result. The sole real executor is Lead after the final source/proof/spec have been fixed and reviewed.
