# ADR-0070: access_token 即 Sa-Token；extras 写目标业务 Client

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-wta-sso` ADR-002（LOG-004 / LOG-009）

## Context

业务安全模型已按 `clientid` / `clientPk` 隔离 RBAC 与会话。替换 Token 形态或签发 SSO 中心 Client 票会破坏隔离或迫使伪 SSO。

## Decision

`access_token` **就是**现有 WTA Sa-Token。换票时 extras **必须**写入**目标业务 Client**，禁止写入 `sso` 中心 Client。

## Consequences

硬验收三门并列：`P0-DEFAULT-PROVIDER-PATH` + `P0-SSO-REUSE` + `P0-CLIENT-ISOLATION`。admin 与 home 两 Token `clientid` 不同且互打被拒。SSO Cookie 不得被业务 App 当作 API Token。
