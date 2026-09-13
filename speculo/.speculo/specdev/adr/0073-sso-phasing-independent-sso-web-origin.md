# ADR-0073: SSO 分期与独立 sso-web Origin

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-wta-sso` ADR-005（LOG-006 / LOG-007 / LOG-011）

## Context

协议核心可与 OIDC/SLO/独立进程解耦。域名与是否独立部署影响 cookie 域与回调配置。

## Decision

- **P0：** authorize + token + PKCE + sso-web + Client 扩展 + 默认第一提供方路径 + 本地并存 + 外部管理面登记
- **P1：** OIDC / refresh / SLO / 同意页
- **P2：** 独立进程 / MFA 收敛
- 对外域名/callback：环境级矩阵（可占位）。生产：**同进程** + **`sso-web` 独立 Web Origin**。独立后端进程维持 P2。
- SSO HttpOnly Cookie 由后端 Set-Cookie。

## Consequences

实现与发布票必须引用环境矩阵；占位行须在上线前替换真实 Origin/callback。反代须维持独立 Web Origin。
