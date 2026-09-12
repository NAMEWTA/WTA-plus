# ADR-0045: Redis 为主的跨进程唤醒；唤醒后复用 claim

- **Status:** Accepted
- **Date:** 2026-09-11
- **Source:** `2026-09-11-notify-outbox-wake-poll` ADR-003（LOG-018 / LOG-020 / LOG-021 / LOG-022 / LOG-023）

## Context

跨进程唤醒需选定介质并保持租约安全；同 JVM afterCommit 不能单独满足多实例 <1s。

## Decision

以 Redis pub/sub（或 list）作为跨进程主唤醒；允许同 JVM afterCommit 事件作同实例辅助。信号必须在事务提交之后发出。Worker 被唤醒后只执行既有 `claim()` + `dispatch`（可连捞到空批），不绕过 lease，不以 outboxId 默认直投。`next_attempt_at` 到期只靠慢速兜底再进入 claim。改造仅在 `ruoyi-notify`。

## Consequences

- 验收按跨进程：写入后典型 <1s 被某实例 claim；漏唤醒靠兜底；lease 防双投。
- Redis 短暂故障时提交与 Outbox 持久化仍成功，最终投递靠兜底扫描。
- 具体 pub/sub vs list 实现细节可变，但不得取消跨进程主路径。
