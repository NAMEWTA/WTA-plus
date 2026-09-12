# Existing-DB username migrate (T-17)

Fresh MySQL init (`10-wta-base.sql`) seeds `admin` / `test`, not `ruoyi`.

For **already-deployed** databases that still have login `ruoyi`:

1. Backup `sys_user`.
2. Confirm `SELECT user_id, user_name FROM sys_user WHERE user_name IN ('ruoyi','wta');`
3. If `wta` already exists, stop (manual merge).
4. Run `65-wta-user-migrate.sql`.
5. Log in as `wta` with the **same password hash** as the old `ruoyi` row.

Rollback: `65-wta-user-rollback.sql` (only if `ruoyi` is free).

Password policy / hash algorithm is out of scope.
