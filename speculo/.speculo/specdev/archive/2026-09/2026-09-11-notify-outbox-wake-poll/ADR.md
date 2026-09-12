# ADR — 2026-09-11-notify-outbox-wake-poll

## ADR-001: 保留 Outbox；写入唤醒 + 慢速兜底；跨进程唤醒一等

**Status:** accepted
**Source:** LOG-015 / LOG-016 / LOG-017 / LOG-012 / LOG-019
**Supersedes:** none

### Context
现网 1s 空转 claim 且无写入唤醒。CTO via Lead 确认保留 Outbox，并要求多实例下跨进程唤醒支撑 <1s 首投。

### Decision
保留 Transactional Outbox；调度为「写入唤醒 + 慢速兜底」。默认 `notify.outbox.poll-delay-ms=60000`。唤醒必须跨进程可达。不扩配置页、不做 MQ Relay、不改 common-notify。

### Trade-off
相对同 JVM-only，增加 Redis（或等价）信号依赖以兑现多实例延迟目标。相对 Outbox→MQ Relay，仍复用 DB Outbox + claim/lease。

### Consequences
- 空队列默认不得再是 1s。
- 重试到期可见性依赖兜底间隔（见 ADR-003 / LOG-021）。

## ADR-002: （已撤回）同 JVM afterCommit 为唯一唤醒

**Status:** superseded
**Source:** LOG-013
**Supersedes:** none

### Decision
撤回。见 ADR-003。

## ADR-003: Redis 为主的跨进程唤醒；唤醒后复用 claim

**Status:** accepted
**Source:** LOG-018 / LOG-020 / LOG-021 / LOG-022 / LOG-023
**Supersedes:** ADR-002

### Context
D-004=A 要求跨进程一等；需选定介质并保持租约安全。

### Decision
以 Redis pub/sub 或 list 作为跨进程主唤醒；允许同 JVM afterCommit 辅助。信号必须在事务提交之后发出。Worker 被唤醒后只执行既有 `claim()` + `dispatch`（可连捞到空批），不绕过 lease。`next_attempt_at` 到期只靠慢速兜底再进入 claim。改造仅在 `ruoyi-notify`。

### Trade-off
相对纯进程内事件，多实例可达 <1s，但依赖 Redis 可用性；Redis 短暂故障时兜底扫描仍保证最终投递。相对 outboxId 直投，多一次 claim，避免双投。

### Consequences
- 验收按跨进程：空队列不再 1s claim；写入后 <1s 被某实例 claim；兜底捞漏；lease 防双投。
- pub/sub vs list 的具体实现细节可在 Spec/Ticket 细化，不得取消跨进程主路径。
