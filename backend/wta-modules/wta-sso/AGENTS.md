# wta-sso

第一方 SSO 模块，提供 Authorization Code + PKCE S256 的 authorize / token / revoke，以及 SSO 域会话。

## Structure

`controller/anonymous -> usecase/impl -> service -> dao -> mapper -> XML`。用户、Client 与登录准入只经 `wta-api` 的 `SsoClientCatalog` / `SsoIdentityService`。业务 Token extras 必须是目标业务 Client，禁止 `sso`。

Cookie 渲染归 `adapter/http`，Sa-Token extras 归 `adapter/gateway`。授权码和会话标识使用 `SsoBearerTokens` 的32字节 SecureRandom；数据库主键保持原生成方式。会话只读取 `sso:session:v2:`，旧会话需要重新登录，旧 Redis key 自然到期。

## 配置

生产保持 `SSO_COOKIE_SECURE=true`（默认），通过 HTTPS 的独立 SSO Origin 及其同源 `/sso` 反代访问后端。Cookie 为 host-only、HttpOnly、Secure、Path=/、SameSite=Lax，创建和删除属性一致。开发 HTTP 必须显式设置 `SSO_COOKIE_SECURE=false`，且实际 active profiles 只能为 `local`/`dev`；未声明、prod 或混合其他 profile 会拒绝启动。

`WEB_CORS_ALLOWED_ORIGINS` 是逗号分隔的精确 HTTP(S) Origin 白名单。未设置时默认 `*`，允许任意 Origin；需要收紧时再设成不含路径的精确 Origin。单独的 `*` 不能和其他来源混写。同源访问不需要 CORS 许可。跨 Origin 不自动使用 SameSite=None 或共享父域 Cookie。

## 验证

在 `backend` 目录运行 `./mvnw -pl wta-modules/wta-sso -am test`；在仓库根目录运行 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered`。

真实浏览器验收在 `wta-admin` 的 `SsoHttpsSessionIntegrationTest`：先构建 sso-web，再显式传入自建隔离 MySQL/Redis 参数。测试启动本地 HTTPS/HTTP，运行 App 内的 `playwright.security.config.ts`；需要 Chrome。身份密码校验与业务 Token 签发使用测试替身，SSO Controller/UseCase/Service、Redis 会话、MySQL DAO/Mapper 和浏览器 Cookie 为真实执行。
