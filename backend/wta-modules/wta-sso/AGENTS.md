# wta-sso

第一方 SSO 模块，提供 Authorization Code + PKCE S256 的 authorize / token / revoke，以及 SSO 域会话。

## Structure

`controller/anonymous -> usecase/impl -> service -> dao -> mapper -> XML`。用户、Client 与登录准入只经 `wta-api` 的 `SsoClientCatalog` / `SsoIdentityService`。业务 Token extras 必须是目标业务 Client，禁止 `sso`。

Cookie 渲染归 `adapter/http`，Sa-Token extras 归 `adapter/gateway`。授权码和会话标识使用 `SsoBearerTokens` 的32字节 SecureRandom；数据库主键保持原生成方式。中央会话的权威状态在 `sso_session`，原始随机 Cookie 只用于摘要定位；Redis 的 `sso:session:v2:` 不得覆盖持久撤销事实。第一方授权码保存 `session_hash`，换票登记 `sso_business_session`，全退后由持久任务注销业务令牌。升级前的 Redis-only 会话需要重新登录，旧 key 自然到期。

## 配置

服务配置由 MySQL `sso_service_config` 管理，Redis 按数据库版本缓存；生产不再从 `namewta.sso` / `SSO_*` 环境变量绑定业务参数。OIDC 管理聚合页通过 `wta-api` 的 `SsoRuntimeConfiguration` 更新配置，不能直接读取 SSO 表。首次配置保持停用；原普通本地登录不受影响。

Origin、页面 base、Cookie 名与 Secure 属性是启动冻结结构。管理页先停用、执行维护退出，确认退出投递完成后保存目标，维护重启生效；旧进程未加载新结构时保持停用。启停与新凭据期限按请求冻结快照热读取，既有会话截止不被延长。Cookie 为 host-only、HttpOnly、Path=/、SameSite=Lax，创建和删除属性一致。非 Secure Cookie 只允许明确的 local/dev 环境。

`SsoSessionLifecycle` 让内部签发事务锁定中央会话行，第一方和 OIDC 的关联登记与全退遵循同一锁序。`SsoSessionRevocationParticipant` 仅在同一主库事务内撤销协议授权及预约退出任务，不在回调中执行 HTTP。`/sso/logout` 与标准 Provider logout 统一终结当前中央会话及其关联 App，不扩大到该用户其他设备。停止新认证后，过期撤销和待办业务票注销任务仍继续运行。

`WEB_CORS_ALLOWED_ORIGINS` 支持逗号分隔的精确 HTTP(S) Origin 列表，或独占列表的 `*`。公共 YAML 默认单独 `*`，回显 HTTP(S) 请求来源并允许凭证；空值不许可跨来源。混合通配符、路径、通配子域和非 HTTP(S) 来源不受支持。同源访问不需要 CORS 许可。跨 Origin 不自动使用 SameSite=None 或共享父域 Cookie。

## 验证

在 `backend` 目录运行 `./mvnw -pl wta-modules/wta-sso -am test`；在仓库根目录运行 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-sso --mode layered`。

真实浏览器验收在 `wta-admin` 的 `SsoHttpsSessionIntegrationTest`：先构建 sso-web，再显式传入自建隔离 MySQL/Redis 参数。测试启动本地 HTTPS/HTTP，运行 App 内的 `playwright.security.config.ts`；需要 Chrome。身份密码校验与业务 Token 签发使用测试替身，SSO Controller/UseCase/Service、Redis Cookie载荷、MySQL 中央会话事实/DAO/Mapper 和浏览器 Cookie 为真实执行。

`sso.fixture.dist` 可指向独占的已构建 SSO 副本；MySQL 用户和密码可由 `sso.mysql.integration.username/password` 提供。发布模板验收使用仓根 `release-artifacts/tests/fixtures/sso-release-origin.mjs --evidence <文件>`，三 App 产物均输出到任务临时目录，不覆盖共享 App dist；已有依赖产物时可加 `--reuse-dependencies`。

`wta-admin` 的 `SsoOidcLogoutMySqlIntegrationTest` 使用本任务独占数据库验证 dynamic-datasource 事务回滚、退出幂等、并发签发与全退的行锁顺序、投递租约。仅显式 `oidc.logout.mysql.integration=true` 和隔离连接参数时运行，不允许指向现存环境。
