---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 .agents/skills/*/SKILL.md：适用 engineering-standards、ruoyi-module-guide、ruoyi-common-modules-guide。不适用 namewta-fullstack-development（无前端/菜单）；不适用 java-api-compatibility（无对外 API 变更）；不适用 deploy-namewta-environment / upstream-fork-sync / project-customization-delivery。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"apply-notify-outbox-boundaries","inputs":["本Ticket路径契约与DEC锁定项","当前 NotifyOutboxWorker 与 claim/dispatch 源码"],"outputs":["统一 claim+dispatch 唤醒入口与默认 60s 兜底落点"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/notification.md</Path>","sha256":"9ada813e7afcd20713787f96c8fc85fef753b8858933b071e62459be11562624","when":"改 Outbox Worker 调度前"},{"path":"<Path>.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md</Path>","sha256":"4d13fcc7143eac940e507b4f4ad47c55391cd64c1a724fbbcda33d4537b5230d","when":"核对 Worker 不在业务事务内直投 Provider 前"}]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"verify","operation":"run-notify-module-quality-gates","inputs":["本Ticket验证矩阵","Worker 调度与 next_attempt_at 测试"],"outputs":["含命令与退出码的 Evidence","未验证项清单"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>","sha256":"913ab52d941f6533f93f22e0486c150c6c62163c61130a987ad0e082b88feac5","when":"设计或运行 Worker 定向测试前"}]}
  - {"id":"ruoyi-module-guide","path":"<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>","sha256":"529cd870b94a789d7504dae81c95629d417fbaa4f89f9cdd0c311387128fa0db","phase":"implement","operation":"navigate-notify-outbox-facts","inputs":["Worker/Port 入口","notify 模块事实"],"outputs":["adapter/worker 与 Port 复用约束摘要"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/ruoyi-module-guide/references/modules/notify/index.md</Path>","sha256":"8bc0a16291568f9b18119cf9abc20d74dddfd47ce1f1cf131f503920e2a20226","when":"改 NotifyOutboxWorker 前"}]}
  - {"id":"ruoyi-common-modules-guide","path":"<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>","sha256":"79fa186e015f38b9c91a348a08ec59c92aa5abc016c693b828577976ac33d362","phase":"implement","operation":"reuse-redisutils-pubsub-or-list","inputs":["T-01 唤醒信号合同","RedisUtils 订阅入口"],"outputs":["Worker 侧订阅/消费实现选择记录"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/ruoyi-common-modules-guide/references/other-utils.md</Path>","sha256":"8d1bfecc538d9b06210a6339cae10b34cce89178a679ddbe300354ac1cad8f89","when":"编写 Worker Redis 订阅前"}]}
resource_claims:
  - "notify.outbox.wake.signal-contract"
  - "notify.outbox.poll-delay-ms-default"
  - "notify.outbox.worker.claim-dispatch-loop"
artifact: ticket
change: 2026-09-11-notify-outbox-wake-poll
id: T-02
title: Worker 订阅唤醒、默认 60s 兜底与统一 claim/dispatch
status: done
planning_depth: standard
planning_depth_reason: 改造调度主路径与默认配置，并接入跨进程订阅，但不改 claim SQL/对外 API。
ready: true
risk: medium
blocked_by: [T-01]
contract_ids: [AC-001, AC-003, AC-005]
owner: rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/NotifyOutboxWorker.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/NotifyOutboxWakeSubscriber.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/adapter/worker/NotifyOutboxWorkerWakePollTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/adapter/worker/</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotifyOutboxWakePublisher.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/support/outbox/</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotifyOutboxClaimService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/utils/RedisUtils.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: Worker 订阅唤醒、默认 60s 兜底与统一 claim/dispatch

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/02-worker-wake-subscribe-and-slow-poll.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-02.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-02` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

## 1. 战略与来源

- **目标：** Worker 订阅 T-01 跨进程唤醒（及可选同 JVM 辅信号），被唤醒后只走既有 `claim`+`dispatch`；将默认 `notify.outbox.poll-delay-ms` 改为 60000 作为慢速兜底。
- **可观察产出：** 默认空闲不再约 1s claim；唤醒触发与兜底 tick 进入同一 claim/dispatch 循环；漏唤醒时兜底仍能捞起到期行；未到期 `next_attempt_at` 不被唤醒强行领走。
- **来源：** `US-001`、`US-004`、`US-005`、`AC-001`、`AC-003`、`AC-005`、`DEC-001`、`DEC-004`、`DEC-005`、`DEC-006`、`ADR-001`、`ADR-003`、`CODE:NotifyOutboxWorker`。
- **当前事实：** `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/NotifyOutboxWorker.java</Path>` 使用 `@Scheduled(fixedDelayString = "${notify.outbox.poll-delay-ms:1000}")`，无订阅入口。
- **Planning Depth 原因：** 调度行为与默认配置变更，需统一入口避免双套业务逻辑。

## 2. 决策状态

### 已锁定决策

- 调度=写入唤醒 + 慢速兜底；保留 Outbox（DEC-001）。
- 默认 `poll-delay-ms=60000`，配置项名保留（DEC-004）；产品默认不得回落 1000。
- 唤醒后仅 `claim()`+`dispatch`，可连捞到空批；不带 outboxId 默认直投、不绕过 lease（DEC-005）。
- `next_attempt_at` 到期只靠慢速兜底再进 claim；写入唤醒不负责未来时刻（DEC-006）。
- 至少能区分唤醒触发的 claim 与兜底 tick（NFR-004），不强制新建监控产品页。

### 已采用的低影响假设

- 抽取/复用单一 poll/claim 循环方法，供 `@Scheduled` 与唤醒回调调用。
- 订阅生命周期随 Worker 组件启动/销毁；通道合同只读自 T-01。
- claim 批次 50、lease 60s 等现网常量本期不强制调整。
- 显式配置 `poll-delay-ms=1000` 仍应生效，但不得作为仓库默认。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Worker 订阅/唤醒入口与默认 60s 兜底 | `NotifyOutboxClaimPort` / `NotifyDispatchPort`、claim SQL | 写入侧 afterCommit 发布（T-01） |
| 统一 claim+dispatch 循环与轻量触发来源日志/指标 | T-01 信号合同常量 | 双进程跨实例门禁与并发双投矩阵主体（T-03） |
| Worker 定向测试：默认 delay、唤醒触发、兜底捞漏、`next_attempt_at` | DispatchNotificationService 完成/失败语义 | 配置页、common-notify、重试专用快轮询/延迟唤醒合同 |

## 4. 要构建什么

运维看到空队列时 Worker 默认按约 60s 量级兜底，而非每秒 claim。异步写入经 T-01 发出唤醒后，本进程或集群内订阅者触发与兜底相同的 claim+dispatch。人为丢掉一次唤醒后，下一次兜底 tick 仍能领取到期行。失败退避写入未来 `next_attempt_at` 后，到期前的无关唤醒不保证领走未到期行。

## 5. 实现契约

- **入口或接缝：** `@Scheduled` 兜底；Redis 订阅回调；可选同 JVM 辅信号监听。
- **输入与输出：** 输入为唤醒信号或定时 tick；输出为对到期行的既有 claim+dispatch 副作用。
- **公共接口变化：** 无 REST；仅内部 Worker 行为与默认配置。
- **不变量：** I1（租约单持有者推进）；I2；I3（空闲默认 ≫ 1s）；DEC-005/006。
- **状态或数据流：** 信号/tick → 同一 poll 循环 → `claim(owner)` → 逐条 `dispatch` → 可再 claim 至空批。
- **错误与失败行为：** 单次 dispatch 失败按现网退避；订阅异常不得杀死整个应用无恢复；空批唤醒可接受。
- **兼容要求：** 配置项名不变；显式 1000 仍可用。
- **安全与隐私要求：** 订阅侧不记录信号中的敏感字段（信号本应无 PII）。

## 6. 执行路线

1. 先写失败测试：默认 delay 注解/解析不为 1000；唤醒回调与 scheduled 共用 claim 入口。
2. 将 Worker 默认改为 60000；抽取统一 poll 循环；接入 T-01 通道订阅（及可选本地辅）。
3. 增加触发来源区分（日志字段或计数）。
4. 覆盖漏唤醒兜底与 `next_attempt_at` 未到期不被领走的定向测试。
5. 运行模块定向测试并写 Evidence。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐；订阅类文件名可按实现微调但必须落在 worker 包可写范围。
- **可写范围：** `adapter/worker/` 与对应测试目录。
- **只读上下文：** T-01 发布合同、Claim/Dispatch、Mapper、RedisUtils、common-notify。
- **共享路径：** 无（`notify.outbox.wake.signal-contract` 由 DAG 串行消费，不并行改写）。
- **保留或不动：** Runtime 写入逻辑（T-01 owner）、配置页、claim XML 谓词语义。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常：默认慢轮询 + 唤醒触发 claim | Worker 组件测试 | `./mvnw -pl ruoyi-modules/ruoyi-notify -am -Dtest=NotifyOutboxWorkerWakePollTest test` | 默认 60000；唤醒走同一 claim+dispatch | `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-02.md</Path>` |
| 失败/补偿：抑制唤醒后兜底仍领取 | 注入/跳过信号 + 触发 scheduled | 同上 | AC-003：到期行仍被 claim | 同上 |
| 回归：`next_attempt_at` 未到期不因唤醒领走；无直投 | claim 谓词 + Worker 测试 | 同上 | AC-005；无 outboxId 绕过 lease | 同上 |
| 不适用：UI E2E | — | — | 不适用：原因：无 UI | 同上 |

- **Workspace checks：** current-workspace 或 source-worktree 运行定向测试。
- **E2E disposition：** not-required：原因：组件级可证；跨进程 <1s 门禁在 T-03。
- **E2E owner/environment：** Lead / parent-candidate（若 Goal 升格）。
- **Integration evidence：** commit/parent SHA 记入 Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 建议与 T-01 同波发布；若仅 T-02，则仅获得慢轮询收益而无写入唤醒。
- **兼容窗口：** 运维需知悉重试可见性上界约等于兜底间隔。
- **监控信号：** 唤醒 claim vs 兜底 claim 区分字段。
- **回滚或前向恢复：** 回滚 Worker diff 可恢复旧默认；注意回滚后会回到 1s 空转。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：原因：无双写协议。

## 10. 验收标准

- [ ] `AC-001`：默认空闲不再约 1s claim，默认间隔符合 60s 兜底量级。
- [ ] `AC-003`：漏唤醒时兜底仍能 claim 到期行。
- [ ] `AC-005`：未来 `next_attempt_at` 到期前唤醒不保证领走；到期后靠兜底（或其后 claim）。
- [ ] 实现开始前已完整读取 Tickets Map 与适用 Skill。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-02.md</Path>`。
- [ ] 未超出 `writable_paths`。
- [ ] 已形成非空 implementation/source commit 且父分支验证记录完整。
- [ ] E2E disposition（not-required）已声明执行。
- [ ] 无未批准偏差；Map/Evidence 状态一致。

## 11. SKILL 调用计划

- `engineering-standards`：约束 Outbox Worker 不得绕过租约/分层。
- `ruoyi-module-guide`：定位 adapter/worker 与 Port 复用。
- `ruoyi-common-modules-guide`：订阅必须走 `RedisUtils`。
- verify 阶段跑模块质量门禁并写 Evidence。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** 无额外数量合同。
- **必需 Skill 不可用：** 阻塞本票。
- **归属与资源冲突：** 与 `2026-09-10-notify-channel-config` 冲突时暂停；禁止改配置页写集。
- **检查点 / 完成出口：** 交回 Goal；不自启 P/I。
