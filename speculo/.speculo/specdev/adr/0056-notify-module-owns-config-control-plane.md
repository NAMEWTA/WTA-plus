# ADR-0056: notify 模块拥有邮件/短信配置控制面

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-002（LOG-005）

## Context

OSS 配置在 system 的 classic CRUD 中。通知中心已是 notify 域。永久 ADR-0006 禁止把模板中心放进 common-notify。

## Decision

`wta-notify`（layered；change 原文 `ruoyi-notify`）拥有配置表、管理 API、权限菜单和前端页面。`wta-common-mail` / `wta-common-sms` 只增加运行时读取与刷新 SPI。`wta-common-notify` 不建模板中心，不拥有账号表。

## Consequences

前端落在 `packages/domains/notify` 与 `packages/web-domains/notify`。system 不新增邮件/短信配置控制器。Dispatcher 仍通过数据面 Adapter 发送，由控制面在调用前解析账号与内容。与 ADR-0006 互补：common-notify 仍是薄渠道契约。
