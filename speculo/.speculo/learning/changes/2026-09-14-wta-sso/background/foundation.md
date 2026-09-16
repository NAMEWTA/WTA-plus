# 总体背景：wta-sso

## 为什么这个主题重要

管理端和门户可以共用“认人”，但不能共用同一张业务 Cookie。`wta-sso` 是第一方 Authorization Code + PKCE 房间：SSO 域自己有会话，业务应用拿自己的 Sa-Token。抄近路（业务直接读 SSO cookie）会把 Client 隔离拆掉。

## 宏观地图

```text
浏览器 sso-web
    |  GET /sso/oauth2/authorize
    |  POST /sso/login  （SSO 域会话）
    v
SsoOAuthController / SsoSessionController   （anonymous）
    v
SsoOAuthUseCaseImpl / SsoSessionUseCaseImpl
    v
SsoAuthorizationService / SsoSessionService
    |-- ports: SsoIdentityPort, SsoClientCatalogPort, SsoSessionPort, SsoBusinessTokenPort
    |-- adapters: WtaApiSsoIdentityAdapter, WtaApiSsoClientCatalogAdapter,
    |            RedisSsoSessionStore, SaTokenSsoBusinessTokenAdapter
    |-- DAO: SsoAuthorizationCodeDao -> SsoAuthorizationCodeMapper
    v
MySQL 授权码 + Redis SSO 会话 + 业务 Sa-Token
```

**类比失效处：** “认人厅”不等于“整栋楼的通行证”。SSO 会话过了，业务 App 仍要换自己的票。

## 核心概念与关系

| 中文 | English | 一句话 |
| --- | --- | --- |
| 授权码 | authorization code | 一次性、可绑定 PKCE 的短码 |
| PKCE S256 | PKCE | 用 code_verifier 证明换票者就是授权时的客户端 |
| SSO 域会话 | SSO session | Redis 里的认人状态，Cookie 由 `SsoSessionCookie` 读写 |
| 业务令牌 | business token | `SsoBusinessTokenPort` 发给业务 App 的 Sa-Token |

## 先决知识与缺口

需要知道 HTTP 重定向和 cookie。不要求已经会写 Sa-Token。基线见 `baseline.md`。

## 术语表

`SsoOAuthCommands` 是授权/换票命令对象。`SsoProperties` 是配置（cookie 名、重定向白名单等），不是业务规则。

## 来源与不确定性

权威是当前 `wta-sso` 源码。Skill 摘要若与源码冲突，以源码为准。
