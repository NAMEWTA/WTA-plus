# ADR-0072: Client authMode = local | sso | both

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-wta-sso` ADR-004（LOG-005）

## Context

迁移期不能切断现有 `POST /auth/login`。各入口是否强制 SSO 需产品决定。

## Decision

保留本地登录；按 Client 配置 `authMode`：`local` / `sso` / `both`。默认 admin/home 为 `both`；允许部分入口直接 `sso`。

## Consequences

`GET /auth/client/context` 扩展 `ssoEnabled` / `ssoAuthorizeUrl` / `authMode`。保留 `POST /auth/login`。生产默认按产品合同（both）。
