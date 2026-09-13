# wta-sso

第一方 SSO 模块，提供 Authorization Code + PKCE S256 的 authorize / token / revoke，以及 SSO 域会话。

## Structure

`controller/anonymous -> usecase/impl -> service -> dao -> mapper -> XML`。用户、Client 与登录准入只经 `wta-api` 的 `SsoClientCatalog` / `SsoIdentityService`。业务 Token extras 必须是目标业务 Client，禁止 `sso`。

## 验证

在后端仓库根目录运行 `./mvnw -pl wta-modules/wta-sso -am test`，再运行 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered`。
