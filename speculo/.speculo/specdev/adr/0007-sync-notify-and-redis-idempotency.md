# ADR-0007: 同步通知与 Redis 幂等

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-007

## Context

业务需要同步得知 Provider 结果，内部 Service、Job 和 Workflow 又不能依赖 Controller 防重复注解。

## Decision

Dispatcher 同步尝试全部合法目标；任一失败时抛出携带完整结果的 typed exception。可选 `idempotencyKey` 的 Redis 作用域是 Channel 与业务 Key，默认窗口 5 分钟；Client 不进入 Key。同 Key 不同摘要拒绝，IN_PROGRESS 命中抛异常，完成态复用首次结果并记录 `SKIPPED_DUPLICATE`。Redis 不可用时，带 Key 请求 fail-closed 且不调用 Provider；无 Key 请求继续发送。

## Consequences

同步调用承担 Provider 时延。该能力不是 durable exactly-once，不自动故障切换或重试；需要最终必达时应另建 Outbox 或 durable queue。
