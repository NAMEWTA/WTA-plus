# 通知 Outbox 唤醒

**Transactional Outbox（本模块）**：`ruoyi-notify` 在业务提交事务内写入 `notify_outbox`，由 Worker 异步 claim 后投递；提交阶段不执行 Provider I/O。
_Avoid_: 把可靠重试塞进 `common-notify`、取消 Outbox、本期做 Outbox→MQ Relay

**Claim / Lease**：短事务用 `FOR UPDATE SKIP LOCKED` 领取到期行，写入 `lease_owner`/`lease_token`/`lease_until`，再在事务外 dispatch；任何唤醒后仍走同一 claim 路径。
_Avoid_: 带 outboxId 绕过 claim 的默认直投

**跨进程写入唤醒**：事务提交成功后，以 Redis pub/sub 或 list 为主通知集群内 Worker 尽快 claim；允许同 JVM afterCommit 事件/latch 作同实例辅助。典型首投 <1s。
_Avoid_: 仅同 JVM 信号当作满足多实例 <1s 的唯一手段、提交前唤醒

**慢速兜底扫描**：`notify.outbox.poll-delay-ms` 可配置，默认 60000ms；补偿漏唤醒、租约过期与 `next_attempt_at` 到期（重试不另建快轮询或延迟唤醒合同）。
_Avoid_: 默认 1000ms 空转、把兜底当有任务主路径

**本 change 范围**：只改 `ruoyi-notify` Outbox 唤醒/轮询；不改 common-notify 同步契约；不扩通知配置页/渠道账号。
_Avoid_: 改 ADR-0007 Dispatcher、接管 `2026-09-10-notify-channel-config`
