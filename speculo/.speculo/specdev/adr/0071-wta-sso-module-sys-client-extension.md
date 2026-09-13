# ADR-0071: wta-sso 模块与 sys_client 扩展边界

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-wta-sso` ADR-003 耐久部分（LOG-004 / LOG-010 / LOG-012）；UI 主路径见 ADR-0074

## Context

需要清晰的第一方 SSO 表面，同时避免平行「OAuth 应用表」与现有客户端管理分叉。

## Decision

- 后端新模块 `backend/wta-modules/wta-sso`
- 前端新应用 `frontend/apps/sso-web`（ClientId=`sso`，会话键=`Sso-Token`）
- P0 **扩展** `sys_client`；新表仅 code / refresh / consent 等短寿命对象
- 现有 `client_secret` **不是** OAuth 密钥；新增 `sso_secret_hash`
- 回调白名单精确匹配（禁 `*`）
- `wta-admin` 以 `namewta.sso.enabled` 组装开关；`packages/platform/auth` 提供 `startSsoLogin()` / `handleCallback()`

## Consequences

模块与数据边界固定在 `wta-sso` + `sys_client` 扩展。创建应用/拿配置的 UI 主路径不在本 ADR（见 ADR-0074）。DDL/DML 基座变更进入实现票。
