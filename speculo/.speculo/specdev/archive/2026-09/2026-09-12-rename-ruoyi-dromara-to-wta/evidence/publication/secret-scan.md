# Secret scan

No `BEGIN PRIVATE KEY` / `AKIA` hits outside Vite env.

**Inherited (not introduced by rename), from plus-ui-namewta HEAD `d77b556`:**

- `apps/*/ .env.*` contain `VITE_APP_RSA_PUBLIC_KEY` / `VITE_APP_RSA_PRIVATE_KEY` demo client-encrypt keys (RuoYi-style frontend demo).
- `admin-web/.env.development` contains lab IPs `172.16.105.9`.

These are pre-existing demo/dev values. Publication execution (T-14) should still decide whether to strip RSA private keys before a **public** push. This rename did not add new credentials.

`application-local.yml` was excluded from the prep tree.
