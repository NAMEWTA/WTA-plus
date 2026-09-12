---
schema_version: 3
artifact: spec
change: 2026-09-11-notify-outbox-wake-poll
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:Lead-CTO-lock-D-004=A-D-010=B-D-011=D60s-D-012=A-D-013=A-D-020=A-D-021=A
  - USER-DECISION:retain-outbox-wake-plus-slow-poll-no-config-page
  - ADR-001
  - ADR-003
  - PERM-ADR-0006
  - PERM-ADR-0007
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/NotifyOutboxWorker.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotifyOutboxClaimService.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotificationApplicationRuntimeService.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/usecase/NotificationApplicationUseCase.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/utils/RedisUtils.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-push/src/main/java/org/dromara/common/push/core/SseEmitterSessionManager.java
---

# Spec: 通知 Outbox 写入唤醒与慢速兜底轮询

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

现网 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/NotifyOutboxWorker.java</Path>` 以 `@Scheduled(fixedDelayString = "${notify.outbox.poll-delay-ms:1000}")` 约每秒执行一次 `claim()`。仓库未覆盖该配置的慢默认值，空队列仍持续打 claim SQL。业务在事务内写入 `notify_outbox`（见 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>`）后没有提交后唤醒，有任务时首投延迟也受轮询间隔下界约束。多实例下 claim/lease（`FOR UPDATE SKIP LOCKED` + 租约）已具备正确性基础，但调度策略浪费 DB 并与「有任务近实时」目标不匹配。

### 目标用户与场景

- **通知运维 / SRE**：希望空闲时 Outbox Worker 几乎不空转 claim，降低无意义 DB 负载与日志噪音。
- **异步通知调用路径**（业务经 `NotificationApplicationUseCase.submit` / 重试入队写出 Outbox 行）：希望提交成功后，集群内某 Worker 典型在 <1s 内 claim 并开始 dispatch。
- **多实例部署**：唤醒必须跨进程可达；正确性继续依赖现有 lease，不引入绕过租约的直投。
- **排障人员**：漏唤醒、租约过期、失败退避后的 `next_attempt_at` 到期仍能被慢速兜底扫描捞起，最终可投递。

### 成功标准

- 空队列时默认不再以约 1s 频率执行 claim SQL；默认兜底间隔为 60s，且 `notify.outbox.poll-delay-ms` 仍可配置。
- 业务事务成功提交并写出可 claim 的 Outbox 行后，跨进程唤醒使集群内某实例典型在 <1s 内完成一次 claim（并进入既有 dispatch）。
- 唤醒丢失或 Redis 短暂不可用时，慢速兜底仍能捞起到期任务；最终投递不依赖「永远成功的唤醒」。
- 唤醒后只走既有 `claim()` + `dispatch`；多实例不因唤醒而双投（复用 lease / `SKIP LOCKED`）。
- 改造仅落在 `ruoyi-notify` Outbox 写入/Worker 路径；不改 common-notify 同步 Dispatcher 语义；不扩通知配置页。

### 非目标

见 **OOS-001**～**OOS-006**。

## 2. 解决方案与外部行为

### 解决方案摘要

保留 Transactional Outbox 与现有 claim/lease。调度改为「跨进程写入唤醒为主 + 慢速兜底扫描」：事务**提交成功之后**，以 Redis pub/sub 或 list 向集群内 Worker 发唤醒信号；允许同 JVM afterCommit 事件/latch 作同实例辅助，但不能替代跨进程主路径。Worker 被唤醒后仅执行与现网相同的 `claim(owner)` + 对每条 `dispatch`（可连续 claim 直到本批空），不携带 outboxId 绕过 lease。`@Scheduled` 保留为兜底，默认 `notify.outbox.poll-delay-ms=60000`。失败退避后的 `next_attempt_at` 到期只靠兜底再进 claim，不为重试另建快轮询或延迟唤醒合同。

### 主要流程

**写入并唤醒**

1. 调用方经既有提交路径写入 intent/delivery/`notify_outbox`（同事务；提交阶段不做 Provider I/O）。
2. 事务提交成功后发出跨进程唤醒信号（Redis 为主）；同 JVM 辅助可选。
3. 集群内一个或多个 Worker 收到信号后触发 claim 循环；`SKIP LOCKED` + lease 保证一行至多被一个活跃租约持有者成功 claim。
4. 成功 claim 的行进入既有 `DispatchNotificationService.dispatch`（租约校验、投递、完成/失败退避语义保持）。

**慢速兜底**

1. Worker 仍按 `notify.outbox.poll-delay-ms`（默认 60000）周期性执行同一 claim+dispatch 入口。
2. 兜底覆盖：漏唤醒、租约过期可再领、以及 `next_attempt_at` / `available_at` 到期的重试行。

**重试可见性**

1. dispatch 失败写入 `next_attempt_at` 后，本期不注册「未来时刻」的专用唤醒。
2. 到期后由下一次兜底扫描（或恰好到来的无关唤醒所触发的 claim）领走；验收不要求重试首见延迟 <1s。

### 边界、失败与稳定错误行为

- **提交前禁止唤醒**：不得在事务提交前发跨进程或进程内唤醒，以免 claim 读到未提交行或竞态空转。
- **Redis 短暂故障**：唤醒失败不得回滚已提交的业务/Outbox 写入；最终投递依赖兜底扫描。不把 Redis 不可用升级为提交失败（除非实现时另有与本 Spec 无冲突的既有基础设施约束；本期不新增「提交依赖 Redis」合同）。
- **唤醒风暴 / 空批**：允许多次唤醒导致多次空 claim；正确性优先于最优空转次数。不做合同级 burst 调度。
- **带 outboxId 直投**：禁止作为默认路径；任何实现优化不得绕过 lease 校验造成双投。
- **配置缺失**：未显式配置时默认兜底为 60000ms，不得回落到 1000ms 作为产品默认。
- **既有 dispatch 失败码与重试上限**：沿用现网 Outbox/dispatch 行为；本期不重新定义错误码表或 `maxAttempts` 政策。

### 状态转换与不变量

- Outbox 行状态机与 claim SQL 谓词保持：`READY`/`PROCESSING` + `available_at`/`next_attempt_at`/`lease_until` 条件见 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>`。
- **不变量 I1**：任意时刻同一 `outbox_id` 至多一个有效 lease 持有者可推进 dispatch 完成写回。
- **不变量 I2**：唤醒只是调度提示，权威可投递集合仍由 DB claim 决定。
- **不变量 I3**：空闲默认 claim 周期 ≫ 1s（默认 60s）。
- **不变量 I4**：跨进程唤醒路径存在且为满足多实例 <1s 首投的主手段。

## 3. 用户故事

- **US-001**：作为通知运维，我希望空队列时 Worker 不再每秒 claim，以便降低无意义的数据库负载。
- **US-002**：作为异步通知调用方，我希望 Outbox 行在事务提交后典型 <1s 被集群内某实例 claim，以便接近实时开始投递。
- **US-003**：作为多实例部署运维，我希望唤醒跨进程可达且仍走 lease，以便多节点不双投。
- **US-004**：作为排障人员，我希望漏唤醒或 Redis 短暂故障时任务仍被慢速兜底捞起，以便最终必达不依赖单次信号。
- **US-005**：作为平台维护者，我希望失败退避后的到期重试只靠兜底进入 claim，以便调度模型简单、不另建重试总线。
- **US-006**：作为与 common-notify / 配置页并行的变更所有者，我希望本期只改 `ruoyi-notify` Outbox 唤醒/轮询，以便不冲击同步 Dispatcher 与通知配置 change。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 默认配置（未把 `poll-delay-ms` 设回 1000）；队列持续为空 | 观察 Worker 空闲期 claim 触发/对应 SQL 频率 | 不再以约 1s 固定频率 claim；默认间隔符合 60s 兜底（允许调度器 jitter，但数量级为分钟级慢轮询而非秒级空转） | 单元/组件：Worker 调度配置与唤醒入口；必要时集成观察 claim 调用计数 |
| AC-002 | ≥2 个 `ruoyi-notify` 进程共享同一 DB 与 Redis；跨进程唤醒已启用 | 在实例 A 提交并成功写入可 claim 的 Outbox 行 | 典型 <1s 内，集群中某实例完成对该行的 claim（或等价可观察：行进入 `PROCESSING` 且带 lease）；不得仅验证同 JVM 路径作为门禁 | 跨进程集成/双进程测试接缝（D-021=A） |
| AC-003 | 人为抑制或丢掉一次唤醒信号；存在已提交且到期可 claim 的行 | 等待不超过一个默认兜底周期（或测试中注入一次兜底 tick） | 该行仍被 claim；最终可进入既有 dispatch 路径 | 组件/集成：兜底 `@Scheduled` 与 claim 复用 |
| AC-004 | 两实例同时收到唤醒；同一 Outbox 行可 claim | 并发 claim | 至多一个实例获得有效 lease 并推进完成写回；另一实例 claim 失败或跳过；无双投写回 | 复用 claim SQL/`SKIP LOCKED`/lease；可补并发组件测试；回归既有 dispatch lease 行为 |
| AC-005 | 一行因失败写入未来 `next_attempt_at` | 在到期前仅依赖写入唤醒；到期后仅慢速兜底 | 到期前唤醒不保证领走未到期行；到期后由兜底（或其后的 claim）领走；不存在合同级「重试专用快轮询」 | claim 谓词 + Worker 调度；定向测试 `next_attempt_at` |
| AC-006 | 变更范围审查 | 对照 diff / 模块边界 | 无 common-notify 同步契约行为变更；无通知配置页/渠道账号管理扩 scope；未取消 Outbox、未上 Outbox→MQ Relay 平台 | 代码审查 + 路径所有权检查 |

## 5. 范围

### IN

- `ruoyi-notify` 内 Outbox **写入后**跨进程唤醒（Redis pub/sub 或 list）及 Worker 侧订阅/消费。
- 可选同 JVM afterCommit 辅助唤醒（不得替代跨进程主路径）。
- 将默认 `notify.outbox.poll-delay-ms` 改为 60000，保留可配置。
- Worker 在唤醒与兜底下统一走既有 `claim` + `dispatch`。
- 跨进程验收所需的测试/观测接缝（可新建测试，优先贴近现有 runtime 测试风格）。

### REUSE

- Transactional Outbox 表与实体 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/domain/entity/NotifyOutbox.java</Path>`。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotifyOutboxClaimService.java</Path>` 与 mapper claim/lease。
- `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/DispatchNotificationService.java</Path>` 投递与租约校验。
- `@DSTransactional` 提交边界：`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/usecase/NotificationApplicationUseCase.java</Path>`。
- 项目既有 Redis 发布订阅能力：`<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/utils/RedisUtils.java</Path>`（及 push 模块跨进程 pub/sub 先例，仅作模式参考，不改 push 合同）。

### OUT

- **OOS-001**：取消 Transactional Outbox 或改回提交线程内 Provider I/O。
- **OOS-002**：本期做 Outbox→MQ/Redis 队列 Relay 平台化。
- **OOS-003**：扩通知配置页、渠道账号管理（并行 change `2026-09-10-notify-channel-config`）。
- **OOS-004**：修改 `ruoyi-common-notify` 同步 Dispatcher / 幂等语义（永久 ADR-0006/0007）。
- **OOS-005**：为 `next_attempt_at` 建立合同级延迟唤醒或重试专用快轮询。
- **OOS-006**：以压测吞吐数字作为完成门禁；调 claim 批次/租约秒数仅当实现需要且不改变本 Spec 外部行为时可作低影响实现细节，不升格为本期平台合同。

## 6. 已锁定实现约束

- **DEC-001**：保留 Transactional Outbox；调度为写入唤醒 + 慢速兜底；不取消 Outbox；不扩通知配置页；不做 Outbox→MQ Relay。来源：`ADR-001` / D-001 / D-002。
- **DEC-002**：多实例跨进程唤醒为一等公民（D-004=A）；同 JVM-only 不得作为满足多实例 <1s 的唯一手段。来源：`ADR-001` / `ADR-003`。
- **DEC-003**：唤醒介质以 Redis pub/sub 或 list 为主；允许同 JVM afterCommit 辅助。pub/sub 与 list 的具体选型为实现细节，但不得取消跨进程主路径。来源：`ADR-003` / D-010=B。
- **DEC-004**：默认 `notify.outbox.poll-delay-ms=60000`，配置项保留。来源：`ADR-001` / D-011=D。
- **DEC-005**：唤醒后仅触发既有 `claim()` + `dispatch`（可连捞到空批）；不绕过 lease；不带 outboxId 默认直投。来源：`ADR-003` / D-012=A。
- **DEC-006**：`next_attempt_at` 到期只靠慢速兜底再进 claim；写入唤醒不负责未来时刻。来源：`ADR-003` / D-013=A。
- **DEC-007**：改造仅限 `ruoyi-notify` Outbox Worker/写入唤醒路径；不改变 ADR-0007 同步 Dispatcher；不把可靠重试塞进 common-notify。来源：`ADR-001` / D-020=A / 永久 ADR-0006/0007。
- **DEC-008**：验收按跨进程（D-021=A）：空队列不再 1s claim；写入后典型 <1s 被某实例 claim；漏唤醒仍被兜底捞起；多实例靠 lease 不双投。压测非门禁。来源：`ADR-003` / D-021=A。
- **DEC-009**：跨进程与进程内唤醒信号必须在事务提交之后发出。来源：`ADR-003` / CONTEXT。

## 7. 数据、接口与兼容

- **公共接口变化：** 无对外 REST/API 契约变更要求。内部可新增唤醒发布/订阅协作类型，但不暴露为业务 API。
- **数据模型与持久化：** 不要求变更 `notify_outbox` 表结构或迁移政策；claim 谓词保持。若实现选择 list 载荷字段，不得把 DB schema 变更当作本期前提。
- **兼容要求：** 单实例与多实例部署在「最终可 claim / 不双投」上兼容；配置项名 `notify.outbox.poll-delay-ms` 保留，仅默认值从 1000 改为 60000（显式配置 1000 仍应生效，但产品默认不得为 1000）。
- **迁移要求：** 无数据迁移。发布时需依赖环境已有 Redis（与现网会话/缓存等一致）；不引入新的强制外部系统类型。
- **发布或运维影响：** 默认空转频率显著下降；重试可见延迟上界与兜底间隔同量级（默认约 60s）。运维若曾依赖 1s 轮询「尽快看见重试」需按新默认预期调整，或显式配置更短兜底（不推荐回到 1s 作为全局默认）。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 唤醒载荷不得包含通知正文、收件人 PII 或 secret；足以提示「有工作」即可（实现可选无业务字段的 ping）。不适用额外鉴权面：沿用实例内 Redis 网络信任模型，不新建公网端点。
- **NFR-002 性能与容量：** 空闲 claim 频率默认降至约 1/60s 量级；写入后首投典型 <1s（跨进程）。不虚构吞吐/QPS 门禁数字。
- **NFR-003 可用性与可靠性：** Redis 短暂不可用时提交与 Outbox 持久化仍成功；最终投递由 DB + 兜底保证。lease 防双投不因唤醒而削弱。
- **NFR-004 可观测性与运营：** 至少能区分「唤醒触发的 claim」与「兜底 tick 的 claim」或提供等价日志/指标字段，便于确认空转消失与漏唤醒兜底；不强制新建独立监控产品页。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| Worker 默认 delay 与唤醒后调用 `claim`+`dispatch` | 单元/组件 | AC-001, AC-005 | 扩展或旁路新建测试于 `ruoyi-notify/src/test/...`；风格参考 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/service/runtime/DispatchNotificationServiceTest.java</Path>` | Ticket Evidence |
| Claim/lease 并发与完成写回 | 组件 | AC-004 | 复用 mapper 语义；现有 dispatch lease 测试思路 | Ticket Evidence |
| 双进程/跨进程：A 写 B 或任实例 claim <1s | 集成 | AC-002, AC-003 门禁语义 | 现成 Outbox 跨进程测试先例不足，需新建最小双进程或测试容器接缝；D-021=A 要求不可降级为仅同 JVM | Ticket Evidence |
| 模块边界与配置默认 | 静态审查 + 定向测试 | AC-006, AC-001 | `mvn -pl ruoyi-modules/ruoyi-notify -am test`（或项目惯用模块测试命令） | Ticket Evidence |

说明：现网**没有** `NotifyOutboxWorker` / claim 专用测试；优先新增贴近 runtime 的组件测试，跨进程合同用最小集成接缝覆盖，不机械要求 UI E2E。

## 10. 风险、假设与未决问题

### 风险

- Redis pub/sub 消息在无订阅者时丢失 → 依赖慢速兜底；list 可保留信号但需防无限堆积（实现细节，须满足「提交不依赖 Redis 成功」与防双投）。
- 多实例同时被唤醒造成短暂 claim 风暴 → 可接受；`SKIP LOCKED` 限制伤害。
- 重试可见性变慢（默认至约 60s）→ 已由 D-013 显式接受；文档/运维需知悉。
- 与并行 change `2026-09-10-notify-channel-config` 同模块改动可能冲突 → 路径上应限制在 Worker/写入唤醒，避免碰配置页与绑定逻辑。

### 已采用的低影响假设

- Redis 通道/list 键名、载荷是否含 outboxId（仅作 hint 仍必须 claim）、同 JVM 辅助是否落地：实现者按现有 `RedisUtils` 与模块惯例选择，验证方式为 AC-002/AC-004。
- 兜底 `@Scheduled` 可继续 fixedDelay；唤醒可用独立执行入口调用同一 poll/claim 循环，避免双套业务逻辑。
- claim 批次 50、lease 60s 等现网常量本期不强制调整。

### 未决问题

无。
