# ADR-0044: 保留 Outbox；写入唤醒 + 慢速兜底；跨进程唤醒一等

- **Status:** Accepted
- **Date:** 2026-09-11
- **Source:** `2026-09-11-notify-outbox-wake-poll` ADR-001（LOG-015 / LOG-016 / LOG-017 / LOG-012 / LOG-019）

## Context

现网 Outbox Worker 曾以约 1s 空转 claim 且无写入唤醒。需要在保留 Transactional Outbox 与 claim/lease 的前提下，兑现多实例下跨进程首投延迟目标，并避免把可靠重试塞进 `common-notify` 或本期做 Outbox→MQ Relay。

## Decision

保留 Transactional Outbox；调度改为「写入唤醒 + 慢速兜底」。默认 `notify.outbox.poll-delay-ms=60000`。唤醒必须跨进程可达。不扩通知配置页、不做 Outbox→MQ Relay、不改 `ruoyi-common-notify` 同步 Dispatcher。

## Consequences

- 空队列默认不得再是 1s claim。
- 重试到期可见性依赖慢速兜底间隔。
- 与 ADR-0006/0007 互补：common-notify 仍薄同步契约；可靠异步投递调度落在 `ruoyi-notify` Outbox。
