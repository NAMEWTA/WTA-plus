# ADR-0069: OAuth Authorization Code + PKCE S256

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-wta-sso` ADR-001（LOG-004 / LOG-008）

## Context

需要跨 App 统一登录，同时保留每 Client 的 Sa-Token 隔离。Implicit 已废弃；把现有密码登录包装成 password grant 会混淆本地登录与 OAuth 客户端凭证；SAML 超出本期栈。

## Decision

采用 **OAuth 2.0 Authorization Code + PKCE（S256）**。禁止 Implicit、OAuth password grant、SAML。OIDC discovery / id_token / userinfo 延期 P1。产品定位：**FirstPartySsoProvider**——自建 SSO 是第三方登录目录的默认第一提供方（与 Mask/GitHub 等同槽、排最前）；身份真相源仍在本仓。禁止把用户目录外包给 Keycloak / Casdoor / Logto / Hydra。废止「NoExternalIdP / 仅第一方」产品口号。

## Consequences

P0 只实现 authorize / token（authorization_code+PKCE）/ revoke；客户端必须支持 PKCE；第一方 SPA=public；PKCE/code 负向矩阵为 P0 硬 AC；revoke≠SLO。
