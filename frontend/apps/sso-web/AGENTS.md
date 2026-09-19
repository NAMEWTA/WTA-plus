# sso-web

独立 Origin 的第一方 SSO 登录页。只接受本仓用户名密码，SSO 会话由后端 HttpOnly Cookie 建立。

生产经 HTTPS 访问，保持 `VITE_SSO_API` 为空并由当前 Origin 的 `/sso` 反代访问后端；Cookie 不进入 JavaScript、localStorage 或共享父域。后端默认 Secure Cookie，本地 HTTP 调试需显式设置后端 `SSO_COOKIE_SECURE=false` 且仅激活 local/dev profile。跨 Origin 调用还需后端精确配置 `WEB_CORS_ALLOWED_ORIGINS`；不通过放宽 SameSite 或 CORS 通配符修复登录循环。

`test`、`lint`、`typecheck`、`build:prod` 是本 App 常规门禁。`playwright.security.config.ts` 使用 Chrome 串行验证构建产物；由后端 `SsoHttpsSessionIntegrationTest` 提供隔离 HTTPS、Redis 和 MySQL。测试的 `ignoreHTTPSErrors` 仅用于自建临时证书，不影响产品代码。
