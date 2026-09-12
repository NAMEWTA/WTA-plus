# Forbidden files

| Item | Result |
|---|---|
| nested `.git` from legacy | 0 |
| `.gitee` | 0 |
| committed `node_modules` | not present (symlink used only for local architecture:check; unlinked before orphan commit) |
| `target/` | gitignored; not for publication |
| `.gitmodules` | not copied |
