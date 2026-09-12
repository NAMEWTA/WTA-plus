# WTA SSO — 领域上下文（草案）

> **Status:** proposed / draft。G-grill 前**未**标 locked。  
> **Authority:** CTO 书面决定 > 本 CONTEXT（经 Grill 确认后）> Spec > Goal Plan。

## Glossary

| Term | Meaning |
|---|---|
| **SSO（本期）** | 第一方统一登录：人在 `sso-web` 认出，经 Authorization Code + PKCE 换得**目标业务 Client** 的现有 Sa-Token |
| **Sa-Token（access_token）** | 现有 JWT Simple + Redis 会话；`loginId=userType:userId`；extra 含 `clientid` 与 `clientPk` |
| **业务 Client** | `sys_client` 行（如 admin / home）；Token extras 必须指向它，而非 SSO 中心 Client |
| **SSO Client** | ClientId=`sso`；会话键=`Sso-Token`；用于 SSO 域会话，**不**作为业务 API Token extras |
| **authMode** | 每 Client：`local` / `sso` / `both`；保留 `POST /auth/login` |
| **PKCE S256** | Authorization Code 流强制；禁 Implicit / password grant |
| **sso_secret_hash** | 新增 OAuth 客户端密钥哈希；**不等于**现有 `client_secret`（后者仅用于生成 clientId 原料） |
| **两层会话** | SSO 域可 HttpOnly Cookie；业务 App 仅 Header Bearer + 自有存储键（Admin-Token / Home-Token） |

## 概念卡

**WhyNotPseudoSso：** 跨域种根 Cookie 或复用 Admin-Token 会因 `SecurityConfig` 要求请求 `clientid` == Token extra 而失败。真正 SSO 是「统一认人 + 按目标 Client 换票」。

**TokenInvariant：** `access_token` **就是**现有 Sa-Token；换票不得签发「SSO 中心 Client」的业务票。

**SingleAppDirectory：** P0 扩展 `sys_client`（回调白名单、public/confidential、SSO 开关、PKCE、自动同意、scope、密钥轮换）；一次性 code / refresh / consent 才可新表。

**NoExternalIdP：** 只借鉴 Keycloak / Casdoor / Logto / Hydra / Spring Authorization Server 的**协议**；用户与 RBAC 真相源仍在本仓。

**Phasing：** P0=协议核心+sso-web+并存登录；P1=OIDC/refresh/SLO/同意；P2=外部第三方/独立进程/MFA 收敛。

## 现状锚点（intake）

- 已激活终端：`admin-web` / `home-web`
- Cookie 默认关闭；业务走 Bearer
- `/auth/login` **不**校验 `client_secret` 作为 OAuth secret
- Baseline：CTO `b06d161`；规划冻结 HEAD `d1ce372`（祖先含基线）

## 开放语义（待 Grill / CTO）

见 Goal Plan CTO-Q1…Q4（P0 范围确认、域名与 callback、authMode 默认、生产同进程 vs 独立域）。
