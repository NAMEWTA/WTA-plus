# 外部 OIDC 三端真实验收

运行 `EXTERNAL_AUTH_FIXTURE=/tmp/owned-fixture.json node e2e/run-external-auth-acceptance.mjs`。脚本使用真实 Admin/Home/SSO 页面与业务后端，不拦截或伪造协议响应。配置和凭据不得提交仓库；默认使用本机 Chrome。

`adminOrigin`、`homeOrigin`、`ssoOrigin` 可包含各自静态部署前缀；脚本同时比较 Origin 与 App 路径，支持同域不同前缀。构建 base、回调和网关挂载须与 fixture 完全一致。

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
- 第二轮将 Home 网关上游切至另一业务后端进程（例如 18889）。它与原节点共享同一 MySQL 业务库、Redis 实例/逻辑 DB/key prefix、`AUTH_CONFIG_ROOT_KEY`，并保持 Sa-Token 登录类型、Token 名称、JWT 密钥及会话设置兼容；Provider 仍为同一个标准 OIDC 服务，RP 配置按业务 Client 区分且在共享库中只配置一次。fixture 的 `mode=independent` 表示独立进程/网关，不表示独立数据库或 Redis 命名空间。如 Admin 绑定已存在，设置 `sharedAlreadyBound=true`；重新准备三个边界账号的业务数据，再运行相同脚本。

此脚本验收包含自建 Provider 的完整三端拓扑，因此要求三端启用。产品部署可以只启用 Admin 或 Admin/Home，并接入外部 OIDC，无需运行本项目 SSO Web/Provider；这类部署使用发布环境 `NAMEWTA_ENABLED_APPS` 选择运行面，不能把三端夹具的前置条件当成产品必选项。

证据包括桌面/移动截图与仅包含验证标签的结果 JSON。脚本不打印密码、业务 token、客户端密钥或回调参数。

旧第一方 SSO 浏览器脚本属于 `/sso/oauth2/*` 协议回归，通过 `first-party-sso-fixture.mjs` 显式建立 PKCE 事务并发起授权；两端保留 `/sso/callback` 处理已发起的协议请求。Admin/Home 登录页只展示配置的外部身份源，旧协议回归不能代替本脚本的真实三端验收。

## 管理配置与第三方互操作

`OIDC_MANAGEMENT_FIXTURE=/tmp/management-fixture.json node e2e/run-oidc-management-acceptance.mjs` 从真实 Admin 页面创建身份源和 App 接入，检测 Discovery、复制交付地址，验证密钥留空保留及客户端列表跳转。Fixture 包含 `adminOrigin`、`adminLocal`、`provider:{providerKey,name,issuer}`、`registrations`、`evidenceDir`；每条接入包含业务 `businessClientId/clientKey`、对方 `externalClientId/clientSecret`、`appPublicUrl/apiPublicBase`、`firstLoginPolicy` 和 `authenticationMethod`。默认菜单路径 `/system/externalAuthProvider`、`/system/externalAuthRegistration`、`/system/client` 可用 `paths` 覆盖。

仅恢复同一任务已创建的记录时使用可选 `reuseProvider: boolean` 与 `reuseRegistrations: string[]`。前者按 fixture 的 `providerKey` 打开已有身份源并核对 Issuer；后者列出已创建接入的 `externalClientId`，例如 `["wta-owned-admin"]`，脚本同时匹配身份源名称、业务 Client ID 与外部 Client ID 后编辑原记录。其余接入仍通过页面新建。这些选项仅用于恢复任务自有夹具，不删除记录，不自动发现或复用其他配置；创建成功后中断的首次运行与恢复运行应一并保留证据。

脚本输出非敏感注册 ID 和后台退出地址，随后须在提供方登记地址并启用 session logout。输入凭据及截图只留在任务拥有的 `/tmp` 目录。配置表单和实际登录分别验收，保存成功不代表对方登录已通过。

登录脚本的 `centralLoginButton` / `centralLogoutButton` 可指定外部页面按钮，例如 Keycloak 的 `Sign In` / `Logout`；账户输入仍使用 `name=username/password`。这仅是验收选择器，产品接入不依赖对方 DOM。其他供应商需对应浏览器夹具，单一供应商通过不能代表所有 OIDC 服务已验收。
