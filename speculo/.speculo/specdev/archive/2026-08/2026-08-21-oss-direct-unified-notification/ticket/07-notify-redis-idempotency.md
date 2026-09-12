---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-07
title: Redis 通知幂等
status: done
planning_depth: standard
planning_depth_reason: 在既有 Dispatcher 接缝增加有界 Redis 状态机和并发失败语义，不改变数据库 schema。
ready: true
risk: high
blocked_by: [T-06]
contract_ids: [AC-018, AC-019, AC-020, AC-021, AC-022]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/idempotency/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-07: Redis 通知幂等

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/07-notify-redis-idempotency.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-07.md</Path>`

## 1. 战略与来源

- **目标：** 给 NotifyClient 提供可选、默认五分钟的业务幂等，而不把它误建成可靠队列。
- **可观察产出：** 首次调用一次 Provider；执行中、已完成重复、摘要冲突和 Redis 故障都有稳定结果。
- **来源：** `ADR-007`、`AC-018..022`。
- **当前事实：** Controller `@RepeatSubmit` 无法覆盖 Service、Job、Workflow；common-notify 当前无内部幂等。
- **Planning Depth 原因：** 并发与 fail-closed 行为高风险，但沿 T-06 Dispatcher 扩展且无 schema 迁移。

## 2. 决策状态

### 已锁定决策

- 幂等可选；作用域为 `Channel + 业务 Key`，不含 tenant/client/user。
- 默认 5 分钟，可在配置上下限覆盖。
- IN_PROGRESS 立即抛异常；COMPLETED 同摘要复用首次结果并发布 SKIPPED_DUPLICATE，不创建 Delivery。
- 同 Key 不同摘要拒绝；Redis 不可用时带 Key fail-closed，无 Key 正常发送。

### 已采用的低影响假设

- 请求摘要采用规范化稳定字段，排除 requestId、审计上下文和其他易变非业务值。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Redis 原子状态机、摘要、结果复用、异常与事件语义 | T-06 Dispatcher、RedisUtils/Redisson | Provider 重试、Outbox、跨窗口 exactly-once、client 分区 |

## 4. 要构建什么

没有 idempotencyKey 的请求直接发送。有 Key 时先原子占位：首次执行 Provider 并存结果；并发同摘要立即报发送中；已完成同摘要复用原结果并发布关联 originalRequestId 的跳过事件；不同摘要拒绝。Redis 故障只阻断明确要求幂等的请求。

## 5. 实现契约

- **入口或接缝：** Dispatcher idempotency decorator/service、Redis 原子脚本或锁定原语。
- **输入与输出：** Channel/key/digest/window -> acquired/in-progress/completed/conflict；completed 保存可还原 NotifyResult。
- **公共接口变化：** NotifyRequest 增加可选 key/window（若 T-06 未预留）；新增 typed exceptions/status。
- **不变量：** Provider 最多一次进入首次占位路径；duplicate 不创建 Delivery；client_pk 不入作用域。
- **状态或数据流：** ABSENT -> IN_PROGRESS -> COMPLETED -> expire。
- **错误与失败行为：** Redis timeout/unavailable 可区别 Provider FAILED，且不调用 Provider。
- **兼容要求：** 无 Key 路径与 T-06 行为一致。
- **安全与隐私要求：** Redis key 不含完整目标/正文；摘要不可逆且日志不输出 Key 敏感值。

## 6. 执行路线

1. 建立并发、摘要冲突、Redis 故障和无 Key 回归测试。
2. 实现规范化摘要与窗口上下限。
3. 实现原子占位、结果存储/复用和过期。
4. 接入 Dispatcher 异常和 SKIPPED_DUPLICATE Event。
5. 运行 Redis 并发集成和 common-notify 回归。

## 7. 路径访问契约

- **预计修改点：** `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/**</Path>`。
- **可写范围：** 同上；common Redis 只读。
- **只读上下文：** RedisUtils/Redisson 既有原语。
- **共享路径：** 无；T-06 完成后顺序接管，T-08 之后接管。
- **保留或不动：** SQL、system、Provider Adapter。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Redis/Dispatcher 集成 | 首次与完成后重复 | Provider 一次、结果复用 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-07.md</Path>` |
| 失败路径 | 并发/故障注入 | IN_PROGRESS、不同摘要、Redis down | typed fail-closed，Provider 零调用 | 同上 |
| 回归 | 单元测试 | 无 Key 且 Redis down | 正常 Provider 发送 | 同上 |

- **Workspace checks：** current 为 `current-workspace`；required 策略下在 `source-worktree` 做定向非 E2E 检查。
- **E2E disposition：** not-required：确定性 Redis 并发集成可完整验证此内部状态机。
- **E2E owner/environment：** Lead / current-workspace；T-09 验证 duplicate 日志落库。
- **Integration evidence：** source commit、direct-parent/candidate parent/result SHA 和 Lead Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-06 后 additive 启用；调用方只有显式 Key 才进入新路径。
- **兼容窗口：** 无 Key 请求完全兼容。
- **监控信号：** acquired/in-progress/duplicate/conflict/redis-unavailable 计数。
- **回滚或前向恢复：** 移除调用方 Key 可绕开；不要在故障时自动 fail-open。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：无旧协议删除。

## 10. 验收标准

- [x] `AC-018..022` 并发与故障矩阵通过。
- [x] 默认窗口是 5 分钟且 client/user/tenant 不进入作用域。
- [x] duplicate 不调用 Provider、不创建 Delivery。
- [x] 验证、提交 SHA 与 Evidence 完整。
