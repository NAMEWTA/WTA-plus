# ADR-0006: common-notify 是薄渠道契约层

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-006

## Context

Mail、SMS 已是原子基础设施，站内消息由现有 system/push 能力承担；建设完整通知中心会重复领域并扩大依赖。

## Decision

`ruoyi-common-notify` 只拥有 NotifyClient、Dispatcher、Channel SPI、请求/结果、上下文 SPI 和 Delivery Event。Mail/SMS 保留原子模块并通过 Adapter 接入；每个请求只选择一个 Channel，但可包含多个物理目标。调用方解析 PHONE/EMAIL/OPEN_ID，common 不读取 SysUser 或 system Mapper。Provider 默认全局配置，可用 `providerKey` 覆盖，不按 Client 路由。

## Consequences

common 不反向依赖 system。模板中心、偏好、多渠道编排、可靠重试、回执与站内信统一不属于该契约；原子 Builder/SDK 只允许在 Adapter 内使用。
