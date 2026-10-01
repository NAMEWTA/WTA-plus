# 外部 OIDC 三端真实验收

运行 `EXTERNAL_AUTH_FIXTURE=/tmp/owned-fixture.json node e2e/run-external-auth-acceptance.mjs`。脚本使用真实 Admin/Home/SSO 页面与业务后端，不拦截或伪造协议响应。配置和凭据不得提交仓库；默认使用本机 Chrome。

Fixture JSON 字段：

```json
{
  "adminOrigin": "http://127.0.0.1:19441",
  "homeOrigin": "http://127.0.0.1:19442",
  "ssoOrigin": "http://127.0.0.1:19443",
  "providerName": "测试统一登录",
  "mode": "shared",
  "adminLocal": { "username": "fixture-admin", "password": "owned-only" },
  "sharedCentral": { "username": "fixture-shared", "password": "owned-only" },
  "missingPhoneCentral": { "username": "fixture-no-phone", "password": "owned-only", "phoneNumber": "13800138001" },
  "conflictCentral": { "username": "fixture-conflict", "password": "owned-only" },
  "unboundCentral": { "username": "fixture-unbound", "password": "owned-only" },
  "sharedAlreadyBound": false,
  "adminTarget": "/index",
  "homeTarget": "/profile",
  "evidenceDir": "/tmp/namewta-external-auth-evidence"
}
```

前置条件：

- 三端使用最终构建产物和自有 loopback HTTP/HTTPS 网关，业务 API 前缀正确转发；验证码关闭只限该隔离 fixture。
- 外部身份源及两业务客户端接入已配置；Admin 为 `BIND_ONLY`，Home 为 `AUTO_REGISTER`，回调分别为对应前端的 `/social-callback` 与 `/logout/callback`。
- `adminLocal` 已有本地账号，具有可用 Admin 菜单；其绑定的 `sharedCentral` 必须允许在两业务客户端登录。共享数据库场景需给该本地身份配置 Home 登录域准入和菜单，不能以绕过服务端权限代替。
- `missingPhoneCentral` 首次登录且不返回手机号；`conflictCentral` 手机号或邮箱与已有 Home 账号冲突；`unboundCentral` 未绑定任何业务账号。三者与主流程账号不同，每次完整验收前重置现场。
- 第二轮将 Home 网关上游切至另一业务后端（例如 18889），该后端使用独立 MySQL 数据库与 Redis 逻辑库，并独立建立 RP 配置；中央 Provider 仍为同一个标准 OIDC 服务。更新 fixture `mode=independent`，如 Admin 绑定已存在则设置 `sharedAlreadyBound=true`。重新准备三个边界账号的业务数据，再运行相同脚本。

证据包括桌面/移动截图与仅包含验证标签的结果 JSON。脚本不打印密码、业务 token、客户端密钥或回调参数。

旧第一方 SSO 浏览器脚本属于 `/sso/oauth2/*` 协议回归，通过 `first-party-sso-fixture.mjs` 显式建立 PKCE 事务并发起授权；两端保留 `/sso/callback` 处理已发起的协议请求。Admin/Home 登录页只展示配置的外部身份源，旧协议回归不能代替本脚本的真实三端验收。
