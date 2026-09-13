# Evidence T-06 rework t290u

- Goal: WTA SSO as social-row circle+svg-icon; real Client SSO 接入 clicks; overwrite three-gate shots.
- Code: `login.vue` / `LoginPage.vue` first social control is `el-button circle` + svg (`icon-class="wta"` / inline W mark); `data-testid="sso-first-provider"` kept.
- Unit (twice, both exit 0):
  - `pnpm --filter @namewta/admin-web --filter @namewta/web-domain-admin test`
  - logs: `/tmp/grok-goal-f4299efa79f3/implementer/sso-rework-unit-1.log`, `sso-rework-unit-2.log`
- E2E (twice, both exit 0):
  - `bash scripts/sso-hard-e2e.sh` → Playwright `playwright.sso.config.ts` (`sso-admin-config.spec.ts` + `sso-three-gates.spec.ts`)
  - logs: `/tmp/grok-goal-f4299efa79f3/implementer/sso-e2e-rework-1.log`, `sso-e2e-rework-2.log`
- Screenshots (OBJECTIVE + copies in this evidence dir):
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-ac001-default-provider-path.png` — AC-001 first social slot is W circle icon (not text “WTA SSO”)
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-ac001-admin-logged-in.png`
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-ac002-reuse-no-password.png`
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-ac003-client-isolation.png`
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-admin-config-list.png` — Client list
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-admin-config-dialog.png` — SSO 接入: 启用 SSO / 登录模式 / 精确回调 / PKCE
  - `/workspace/vp-dev/WTA-plus/temp/team/lead/e2e/sso-admin-config-secret.png` — confidential + SSO 密钥 + 「已配置」
- Residual: home `access_path=/home/**` still 403s `/system/user/getInfo` after SSO (does not block isolation assertion). Notify archive untouched. Not pushed.
