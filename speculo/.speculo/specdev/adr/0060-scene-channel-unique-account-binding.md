# ADR-0060: 场景在渠道上绑定唯一账号，调用方不传供应商

- **Status:** Accepted
- **Date:** 2026-09-10
- **Source:** `2026-09-10-notify-channel-config` ADR-006（LOG-012 / LOG-018）

## Context

账号可同时启用多个，发送又不能隐式默认。调用方传 providerKey 会把供应商硬编码退回业务。ADR-0006 数据面仍接受控制面解析后的 providerKey。

## Decision

逻辑场景在每个渠道上绑定唯一渠道账号；短信再绑定该账号下的供应商模板码。普通 `NotificationCommand` 不传 `providerKey`。绑定缺失或账号停用时，该渠道失败关闭，不改选其他启用账号；同一通知的其他渠道仍按各自绑定发送。

## Consequences

控制面按 scene + channel 解析账号后再调用数据面。测试发送的模板级路径必须走同一绑定。不改写 ADR-0006 的薄契约。
