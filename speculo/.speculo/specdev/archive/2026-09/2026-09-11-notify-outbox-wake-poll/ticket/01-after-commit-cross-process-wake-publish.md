---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 .agents/skills/*/SKILL.md：适用 engineering-standards（通知/Java/测试硬约束）、ruoyi-module-guide（ruoyi-notify Outbox 事实）、ruoyi-common-modules-guide（RedisUtils 跨进程信号）。不适用 namewta-fullstack-development（无前后端页面/菜单/CRUD 垂直切片）；不适用 java-api-compatibility（无对外 REST/公共 API 契约变更）；不适用 deploy-namewta-environment / upstream-fork-sync / project-customization-delivery。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"apply-notify-outbox-boundaries","inputs":["本Ticket路径契约与DEC锁定项","当前 Notify Outbox/事务边界源码"],"outputs":["符合 layered/通知硬约束的唤醒实现落点","未越权改 common-notify/配置页的差异说明"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/notification.md</Path>","sha256":"9ada813e7afcd20713787f96c8fc85fef753b8858933b071e62459be11562624","when":"改 Outbox/Worker/通知运行时边界前"},{"path":"<Path>.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md</Path>","sha256":"4d13fcc7143eac940e507b4f4ad47c55391cd64c1a724fbbcda33d4537b5230d","when":"注册提交后唤醒或 DsTxEventListener 前"}]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"verify","operation":"run-notify-module-quality-gates","inputs":["本Ticket验证矩阵","ruoyi-notify 定向测试范围"],"outputs":["含命令与退出码的 Evidence","未验证项与残余风险清单"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>","sha256":"913ab52d941f6533f93f22e0486c150c6c62163c61130a987ad0e082b88feac5","when":"设计或运行定向测试前"}]}
  - {"id":"ruoyi-module-guide","path":"<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>","sha256":"529cd870b94a789d7504dae81c95629d417fbaa4f89f9cdd0c311387128fa0db","phase":"implement","operation":"navigate-notify-outbox-facts","inputs":["本Ticket目标入口","notify 模块事实地图"],"outputs":["确认的 Worker/Runtime/Port 落点与分层约束摘要"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/ruoyi-module-guide/references/modules/notify/index.md</Path>","sha256":"8bc0a16291568f9b18119cf9abc20d74dddfd47ce1f1cf131f503920e2a20226","when":"进入 ruoyi-notify Outbox 改动前"}]}
  - {"id":"ruoyi-common-modules-guide","path":"<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>","sha256":"79fa186e015f38b9c91a348a08ec59c92aa5abc016c693b828577976ac33d362","phase":"implement","operation":"reuse-redisutils-pubsub-or-list","inputs":["跨进程唤醒主路径需求","RedisUtils 发布订阅入口"],"outputs":["选用 RedisUtils.publish/subscribe 或 list 的实现选择记录","禁止直连 Redisson 业务绕道的核对"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/ruoyi-common-modules-guide/references/other-utils.md</Path>","sha256":"8d1bfecc538d9b06210a6339cae10b34cce89178a679ddbe300354ac1cad8f89","when":"编写或改动 Redis 唤醒发布/订阅代码前"}]}
resource_claims:
  - "notify.outbox.wake.signal-contract"
  - "notify.outbox.wake.after-commit-publish"
artifact: ticket
change: 2026-09-11-notify-outbox-wake-poll
id: T-01
title: 提交后跨进程 Outbox 唤醒发布与同 JVM 辅信号
status: done
planning_depth: standard
planning_depth_reason: 多文件写入路径改动，绑定 @DSTransactional 提交后时机与 Redis 信号合同，但不改表结构/对外 API。
ready: true
risk: medium
blocked_by: []
contract_ids: [AC-002, AC-006]
owner: rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotifyOutboxWakePublisher.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/service/runtime/NotifyOutboxWakePublisherTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotifyOutboxWakePublisher.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/support/outbox/</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/service/runtime/NotifyOutboxWakePublisherTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/support/outbox/</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/usecase/NotificationApplicationUseCase.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/NotifyOutboxWorker.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/utils/RedisUtils.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-push/src/main/java/org/dromara/common/push/core/SseEmitterSessionManager.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 提交后跨进程 Outbox 唤醒发布与同 JVM 辅信号

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/01-after-commit-cross-process-wake-publish.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-01.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-01` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

## 1. 战略与来源

- **目标：** 在业务事务成功提交并写出可 claim 的 `notify_outbox` 行之后，向集群发布跨进程唤醒信号；可选同 JVM afterCommit 辅信号，但不能替代跨进程主路径。
- **可观察产出：** submit/retry 写出 Outbox 且事务提交成功后，Redis 通道/list 上出现无 PII 的唤醒信号；提交前不发信号；Redis 发布失败不回滚已提交写入。
- **来源：** `US-002`、`US-003`、`US-006`、`AC-002`、`AC-006`、`DEC-002`、`DEC-003`、`DEC-007`、`DEC-009`、`ADR-001`、`ADR-003`、`CODE:NotificationApplicationRuntimeService`、`CODE:RedisUtils`。
- **当前事实：** `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>` 在 `@DSTransactional` 用例内插入 Outbox 后无提交后唤醒；Worker 仅靠约 1s `@Scheduled` 空转 claim。
- **Planning Depth 原因：** 标准跨层运行时切片；需严格 afterCommit 时机，但无 schema/对外 API 迁移。

## 2. 决策状态

### 已锁定决策

- 跨进程唤醒为一等公民；同 JVM-only 不得作为满足多实例 <1s 的唯一手段（DEC-002）。
- 唤醒介质以 Redis pub/sub 或 list 为主，允许同 JVM 辅（DEC-003）；具体选型为实现细节。
- 信号必须在事务提交之后发出（DEC-009）；提交前禁止唤醒。
- 改造仅限 `ruoyi-notify` Outbox 写入唤醒路径；不改 common-notify 同步 Dispatcher、不扩通知配置页（DEC-007 / AC-006）。
- 唤醒载荷不得含通知正文、收件人 PII 或 secret（NFR-001）。

### 已采用的低影响假设

- 优先复用 `RedisUtils.publish`/`subscribe` 模式（参考 push 模块先例，只读不改 push 合同）；若选 list，须防无限堆积且提交成功不依赖 Redis 成功。
- 在 Runtime 写入 Outbox 后登记提交后回调：优先 `DsTxEventListener` + 明确 `TransactionPhase`，或等价经 dynamic-datasource 认可的 afterCommit 接缝；禁止用会在无 Spring TX 时立即执行的 fallback 伪 afterCommit。
- 通道/键名由本票固化为模块内常量或小协作类型，供 T-02 订阅；载荷可为无业务字段 ping，可选 outboxId 仅作 hint。
- submit 与 retry 两条写出 Outbox 的路径均需唤醒；cancel 不要求唤醒。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 提交后跨进程唤醒发布组件/方法 | `RedisUtils` 发布能力；现有 Outbox 插入与 `@DSTransactional` 边界 | Worker 订阅与 poll 默认值改造（T-02） |
| 可选同 JVM afterCommit 辅信号 | push 模块 pub/sub 仅作模式参考 | 跨进程双实例门禁测试主体（T-03） |
| 写入路径定向测试：提交前后时机、Redis 失败不回滚、无 PII 载荷 | claim/lease/dispatch 语义 | 通知配置页、渠道账号、common-notify 同步契约、Outbox→MQ Relay |

## 4. 要构建什么

调用方经既有 `NotificationApplicationUseCase.submit`/`retry` 写出可 claim Outbox 行。事务**提交成功之后**，系统向约定 Redis 通道或 list 发出跨进程唤醒；同 JVM 可再发本地辅信号。调用者观察到：业务回执仍成功；若 Redis 短暂失败，写入已持久化且不因唤醒失败回滚。排障可确认信号不含业务敏感字段。

## 5. 实现契约

- **入口或接缝：** Outbox 持久化完成且处于活跃事务内的登记点 → 提交后发布器；只读 UseCase 事务边界。
- **输入与输出：** 输入为“本事务已插入至少一条可 claim Outbox”的事实；输出为跨进程唤醒信号（及可选本地辅信号）。无新 REST。
- **公共接口变化：** 无对外 API；可新增模块内发布协作类型。
- **不变量：** I2（唤醒只是调度提示）；I4（跨进程主路径存在）；提交前不发信号；Redis 失败不回滚已提交业务/Outbox。
- **状态或数据流：** DB Outbox 行已提交 → afterCommit → Redis publish/list push（+ 可选本地事件）。
- **错误与失败行为：** 唤醒发布异常记录日志/指标后吞掉或隔离，不得传播为提交失败；禁止为“保证唤醒”而把 Redis 升为提交依赖。
- **兼容要求：** 单实例与多实例均可发布；通道合同对 T-02 稳定。
- **安全与隐私要求：** 载荷禁止正文/PII/secret。

## 6. 执行路线

1. 建立验证接缝：模拟/桩 Redis，断言提交前无发布、提交后有发布；Redis 抛错时业务提交仍成功。
2. 引入唤醒发布协作类型与通道常量；经 `RedisUtils` 实现跨进程主路径。
3. 在 Runtime Outbox 写入路径登记 afterCommit（`DsTxEventListener` 或等价），覆盖 submit/retry；可选同 JVM 辅信号。
4. 形成可编译安全落点；运行定向单测与适用模块回归。
5. 记录 Evidence：命令、退出码、路径 diff 边界（未触配置页/common-notify）。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐。
- **可写范围：** 与 `writable_paths` 对齐；越界前必须停止。
- **只读上下文：** UseCase、现网 Worker、`RedisUtils`、push 先例、common-notify。
- **共享路径：** 无。
- **保留或不动：** 通知配置 Controller/UseCase、渠道账号、claim SQL 谓词、dispatch 完成写回语义。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常：提交后发布跨进程信号 | Publisher/Runtime 组件测试 | `./mvnw -pl ruoyi-modules/ruoyi-notify -am -Dtest=NotifyOutboxWakePublisherTest test`（或实现时等价定向命令） | 提交后恰好发布；载荷无 PII | `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-01.md</Path>` |
| 失败：Redis 发布失败 | 同上，注入 publish 异常 | 同上 | Outbox 已提交语义保留；异常不升级为业务提交失败 | 同上 |
| 回归：未提交前不唤醒；配置页/common-notify 未改 | 负向断言 + 路径审查 | 定向测试 + `git diff --stat` 范围核对 | 无提交前信号；diff 不落配置页/common-notify | 同上 |
| 不适用：E2E UI | — | — | 不适用：原因：本票无 UI/菜单 | 同上 |

- **Workspace checks：** current-workspace / source-worktree 运行上述模块定向测试与静态范围审查。
- **E2E disposition：** not-required：原因：无跨 UI/HTTP 新边界；跨进程行为门禁由 T-03 承担。
- **E2E owner/environment：** Lead / parent-candidate（若后续 Goal 升格）；本票不要求。
- **Integration evidence：** implementation/source commit、parent before、适用 candidate/result SHA 与父分支包含关系记入 Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：原因：无 DB/API 迁移。
- **兼容窗口：** 发布后无订阅者时信号可丢失，最终投递依赖 T-02 慢速兜底；可先于或与 T-02 同发布波次，但单独上线时不得声称 <1s 已兑现。
- **监控信号：** 唤醒发布成功/失败计数或日志字段（轻量）。
- **回滚或前向恢复：** 回滚本票 diff 即停止发布；Outbox 仍可由旧/新 Worker 轮询领取。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：原因：无旧协议双写收缩。

## 10. 验收标准

- [ ] `AC-002`（写路径部分）：事务提交后发出跨进程唤醒主信号；同 JVM 辅信号若存在不得替代主路径。
- [ ] `AC-006`：diff 不改 common-notify 同步契约行为，不扩通知配置页/渠道账号。
- [ ] 实现开始前已完整读取 Tickets Map，已读取项目 Skill 入口并完整展开适用于 `ALL`/`T-01` 的匹配项。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-01.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`。
- [ ] Ticket 已按 Goal Plan 策略形成非空 implementation/source commit，direct-parent 或 candidate 验证通过且父分支 result 已记录。
- [ ] E2E disposition 已执行（本票 not-required）。
- [ ] 未发生未批准的范围、契约或发布偏差。
- [ ] Ticket、Tickets Map 和 Evidence 状态一致。

## 11. SKILL 调用计划

- `engineering-standards` / implement：落笔前读取通知与事务 references，约束 afterCommit 与模块边界。
- `engineering-standards` / verify：按测试规则跑定向门禁并写入 Evidence。
- `ruoyi-module-guide` / implement：确认 Runtime/Outbox 分层落点，避免 UseCase 直连 Redis 实现细节失控。
- `ruoyi-common-modules-guide` / implement：强制经 `RedisUtils` 发布，不直连 Redisson/自造封装。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** 与 Map `requested_deliverables=[]` 对齐；本 change 无额外数量合同。
- **必需 Skill/引用/测试不可用：** 阻塞本票，报告缺口。
- **归属与资源冲突：** 与并行 change `2026-09-10-notify-channel-config` 争用 `ruoyi-notify` 时暂停并升级 Lead；不接管其配置页写集。
- **检查点：** 记录源版本、绑定摘要、已完成步骤、Evidence。
- **完成出口：** 验收通过后交回 Goal；不自启 P-goal-plan / I-implement。
