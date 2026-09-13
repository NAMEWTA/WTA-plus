# WTA SSO 术语

- **Status:** Current
- **Date:** 2026-09-13
- **Source:** `2026-09-12-wta-sso` CONTEXT Glossary / 概念卡

SSO / 第三方登录体系术语。与 `plus-ui-multi-app-architecture-terms.md`、notify 术语互补，不覆盖。

**第三方登录体系**：登录页上可插拔的登录提供方目录。已有 Mask、GitHub 等 social；本期把自建 SSO 放进同一目录，且排第一。
_Avoid_: 取消第三方登录、把自建 SSO 排除在目录外

**FirstPartySsoProvider**：自建、自己实现的登录提供方：默认已接通、排序最前、默认路径可走通。对接入方等同「第三方登录」；身份真相源仍在本仓。
_Avoid_: `NoExternalIdP`、「仅第一方」、把用户目录外包给 Keycloak / Casdoor / Logto / Hydra

**登录归一化**：同一套本仓账号密码，多端（自有前端各 App + 外部系统 App）登录。账号归一 ≠ Token 共用。
_Avoid_: Token 跨 Client 共用、伪 SSO

**SSO（本期）**：自建 SSO 服务：人在 `sso-web` 用本仓账号认出，经 Authorization Code + PKCE 换得目标业务 Client 的现有 Sa-Token。
_Avoid_: Implicit、password grant、把 SSO Cookie 当业务 API Token

**接入方 App**：自有前端各 App（admin-web / home-web / 后续自有 App）以及外部系统 App；均可按同一套流程接入。
_Avoid_: 仅自有 App、把外部系统当外置身份源

**Sa-Token（access_token）**：现有 JWT Simple + Redis 会话；`loginId=userType:userId`；extra 含 `clientid` 与 `clientPk`。本期 `access_token` 就是它。
_Avoid_: 另发 SSO 中心 Client 业务票

**业务 Client**：`sys_client` 行（admin / home / 外部登记应用）；Token extras 必须指向它，而非 SSO 中心 Client。
_Avoid_: extras 写 `sso` 中心 Client

**SSO Client**：ClientId=`sso`；会话键=`Sso-Token`；仅 SSO 域会话，不作为业务 API Token extras。
_Avoid_: 用 Sso-Token 调业务 API

**authMode**：每 Client：`local` / `sso` / `both`；默认 admin/home=`both`；保留 `POST /auth/login`；context 扩展 `ssoEnabled` / `ssoAuthorizeUrl` / `authMode`。
_Avoid_: 切断本地登录、无 Client 级开关

**PKCE S256**：Authorization Code 流强制；禁 Implicit / password grant。OIDC → 后期。
_Avoid_: Implicit、password grant、无 PKCE

**sso_secret_hash**：OAuth 客户端密钥哈希；不等于现有 `client_secret`。
_Avoid_: 把 `client_secret` 当 OAuth 密钥、回调白名单 `*`

**两层会话**：SSO 域可 HttpOnly Cookie（后端 Set-Cookie）；业务 App 仅 Header Bearer + 自有存储键。
_Avoid_: 跨域种根 Cookie、复用 Admin-Token 当 SSO

**SsoAdminDualSurface**：系统管理下独立「SSO 管理」负责创建应用与拿配置（自有+外部注册）；「客户端管理」仅配置自有 App 的 SSO 接入；数据仍扩展 `sys_client`。
_Avoid_: 「创建应用=客户端管理」、把应用注册塞进客户端管理同页

**TokenInvariant**：`access_token` 就是现有 Sa-Token；换票不得签发「SSO 中心 Client」的业务票。
_Avoid_: 伪 SSO、中心 Client 业务票

**ThirdPartySlotFirst**：自建 SSO 是第三方登录目录的第一槽位：默认已接通、排序最前、默认路径可走通。
_Avoid_: 取消第三方登录、再引入外置用户目录

**OpsFlow**：(a) 创建应用 (b) 配置回调/返回地址 (c) 把 client/密钥交给应用；自有 App 也可直接读配置。
_Avoid_: 无交付流程、仅口头配置

**WhyNotPseudoSso**：跨域种根 Cookie 或复用 Admin-Token 会因 `SecurityConfig` 要求请求 `clientid` == Token extra 而失败。真正 SSO 是「统一认人 + 按目标 Client 换票」。
_Avoid_: 伪 SSO、共享业务 Token

**Phasing**：P0 = Authorization Code + sso-web + 应用接入 + 默认提供方槽位 + 本地并存；P1 = OIDC / refresh / SLO / 同意；P2 = 独立进程 / MFA 收敛。生产同进程 + `sso-web` 独立 Web Origin。
_Avoid_: 把外部第三方单列禁区、P0 做独立进程
