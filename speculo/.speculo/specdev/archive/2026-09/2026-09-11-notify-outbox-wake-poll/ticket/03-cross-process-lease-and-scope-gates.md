---
schema_version: 3
plan_contract_version: 1
skill_scan: "已扫描 AGENTS.md 与 .agents/skills/*/SKILL.md：适用 engineering-standards、ruoyi-module-guide、ruoyi-common-modules-guide（跨进程 Redis 观测）。不适用 namewta-fullstack-development（无前端）；不适用 java-api-compatibility；不适用 deploy-namewta-environment / upstream-fork-sync / project-customization-delivery。"
skill_bindings:
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"implement","operation":"apply-notify-outbox-boundaries","inputs":["本Ticket验收门禁","claim/lease 与模块边界审查清单"],"outputs":["跨进程/并发/范围门禁测试接缝与路径审查结论"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/notification.md</Path>","sha256":"9ada813e7afcd20713787f96c8fc85fef753b8858933b071e62459be11562624","when":"设计 Outbox 租约/双投门禁前"},{"path":"<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>","sha256":"913ab52d941f6533f93f22e0486c150c6c62163c61130a987ad0e082b88feac5","when":"设计跨进程或并发测试接缝前"}]}
  - {"id":"engineering-standards","path":"<Path>.agents/skills/engineering-standards/SKILL.md</Path>","sha256":"acac7d1d4341d3361c298ebc5a61841d794cafbe9f235171b56fddd7ecb0037d","phase":"verify","operation":"run-notify-module-quality-gates","inputs":["本Ticket验证矩阵","双进程/并发/范围审查证据"],"outputs":["含命令与退出码的 Evidence","AC-002/004/006 映射"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/engineering-standards/references/rules/testing.md</Path>","sha256":"913ab52d941f6533f93f22e0486c150c6c62163c61130a987ad0e082b88feac5","when":"执行门禁测试前"}]}
  - {"id":"ruoyi-module-guide","path":"<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>","sha256":"529cd870b94a789d7504dae81c95629d417fbaa4f89f9cdd0c311387128fa0db","phase":"implement","operation":"navigate-notify-outbox-facts","inputs":["Outbox claim/lease 事实","模块边界 OUT 清单"],"outputs":["测试接缝落点与禁止改动面核对"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/ruoyi-module-guide/references/modules/notify/index.md</Path>","sha256":"8bc0a16291568f9b18119cf9abc20d74dddfd47ce1f1cf131f503920e2a20226","when":"设计 notify 测试接缝前"}]}
  - {"id":"ruoyi-common-modules-guide","path":"<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>","sha256":"79fa186e015f38b9c91a348a08ec59c92aa5abc016c693b828577976ac33d362","phase":"implement","operation":"reuse-redisutils-pubsub-or-list","inputs":["跨进程唤醒观测需求","RedisUtils 能力边界"],"outputs":["双进程测试中 Redis 信号观测方式说明"],"required":true,"on_failure":"block-ticket","references":[{"path":"<Path>.agents/skills/ruoyi-common-modules-guide/references/other-utils.md</Path>","sha256":"8d1bfecc538d9b06210a6339cae10b34cce89178a679ddbe300354ac1cad8f89","when":"搭建跨进程 Redis 观测/夹具前"}]}
resource_claims:
  - "notify.outbox.wake.cross-process-gate"
  - "notify.outbox.claim-lease-concurrency-gate"
  - "notify.outbox.scope-boundary-gate"
artifact: ticket
change: 2026-09-11-notify-outbox-wake-poll
id: T-03
title: 跨进程首投、并发租约不双投与范围门禁接缝
status: done
planning_depth: standard
planning_depth_reason: 需要新建跨进程/并发验收接缝以锁定 D-021=A，并做模块边界门禁，但不改产品行为合同本身。
ready: true
risk: high
blocked_by: [T-01, T-02]
contract_ids: [AC-002, AC-004, AC-006]
owner: rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/adapter/worker/NotifyOutboxWakeCrossProcessIT.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/service/runtime/NotifyOutboxClaimConcurrencyTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/NotifyOutboxWakeScopeGateTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/adapter/worker/NotifyOutboxWakeCrossProcessIT.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/service/runtime/NotifyOutboxClaimConcurrencyTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/NotifyOutboxWakeScopeGateTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/support/outbox/</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/adapter/worker/</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/service/runtime/</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/utils/RedisUtils.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-notify/</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/main/java/org/dromara/notify/controller/</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 跨进程首投、并发租约不双投与范围门禁接缝

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/03-cross-process-lease-and-scope-gates.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-03.md</Path>`

实现本 Ticket 时，Lead 与 implementation subagent 必须按顺序完整读取总体 Map，读取项目 Skill 的 frontmatter 与入口并只展开适用于 `ALL`/`T-03` 的匹配项，再读取本 Ticket 与相关上游工件。Map 中的 Skill 是最低必读集合；新的匹配项先由 Lead 同步到 Map 并重新校验。

## 1. 战略与来源

- **目标：** 用最小可维护接缝证明：实例 A 写入后集群内某实例典型 <1s claim（不得仅同 JVM）；并发唤醒下 lease/`SKIP LOCKED` 不双投；变更范围不触碰配置页与 common-notify 同步契约。
- **可观察产出：** 自动化或半自动双进程/测试容器证据显示跨进程 claim；并发 claim 至多一个有效 lease 完成写回；范围审查/门禁测试通过。
- **来源：** `US-002`、`US-003`、`US-006`、`AC-002`、`AC-004`、`AC-006`、`DEC-002`、`DEC-005`、`DEC-008`、`ADR-003`、`D-021=A`。
- **当前事实：** 仓库缺少 `NotifyOutboxWorker`/跨进程唤醒专用测试；dispatch lease 有可参考单测风格。
- **Planning Depth 原因：** 高事故半径在于错误降级为同 JVM 门禁；需明确跨进程证据，但仍属测试接缝而非平台迁移。

## 2. 决策状态

### 已锁定决策

- 验收按跨进程，不可降级为仅同 JVM（DEC-008 / D-021=A）。
- 唤醒后仍走 claim；多实例靠 lease 不双投（DEC-005 / AC-004）。
- 范围仅 `ruoyi-notify` Outbox 唤醒/轮询；OUT 含配置页与 common-notify（AC-006）。
- 压测吞吐不是完成门禁（OOS-006）。

### 已采用的低影响假设

- 优先 Testcontainers/双进程最小夹具；若环境缺容器，可用两套 Spring 上下文 + 共享 Redis/DB 的组件级双监听者接缝，但证据必须证明跨进程或跨 JVM 信号路径，而不能只 assert 本地方法调用。
- 并发测试可在单 JVM 内多 owner 并发 claim 证明 lease 互斥，作为 AC-004 主证据；AC-002 仍需跨进程信号路径证据。
- 范围门禁可用静态路径断言或架构测试，对照禁止目录。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 跨进程 <1s claim 门禁接缝 | T-01/T-02 已落地的发布/订阅与 Worker 循环 | 新功能行为变更（应已在 T-01/T-02） |
| 并发 claim/lease 不双投测试 | 现有 `DispatchNotificationServiceTest` 风格、claim SQL | 通知配置页 E2E、MQ Relay |
| 模块边界/路径所有权门禁 | RedisUtils、Mapper lease 语义 | 调 claim 批次/租约秒数作为平台合同 |

## 4. 要构建什么

在共享 DB+Redis 的条件下，实例 A 提交 Outbox 后，集群中某 Worker 在典型 <1s 内 claim 该行（进入 PROCESSING/持有 lease 或等价可观察点）。两实例同时被唤醒时，同一行至多一个有效 lease 持有者完成写回。审查本 change 最终 diff：无 common-notify 同步契约行为变更，无通知配置页/渠道账号扩 scope。

## 5. 实现契约

- **入口或接缝：** 跨进程 IT/双上下文测试；并发 claim 测试；范围门禁测试。
- **输入与输出：** 输入为 T-01/T-02 合并后的行为；输出为可重复的失败即红的自动化证据。
- **公共接口变化：** 无。
- **不变量：** I1、I4；AC-002 不得用同 JVM-only 顶替。
- **状态或数据流：** A 提交 → Redis 信号 → B（或 A）claim → lease 互斥。
- **错误与失败行为：** 测试环境缺 Redis/DB 时失败并报告，不静默 skip 充当通过。
- **兼容要求：** 不修改生产默认之外的行为；若需测试专用配置，隔离在测试资源。
- **安全与隐私要求：** 测试数据不含真实 PII。

## 6. 执行路线

1. 确认 T-01/T-02 行为已在父分支/当前工作树可调用。
2. 建立跨进程（或双 JVM 上下文）接缝，先让“仅同 JVM”对照失败于门禁定义。
3. 添加并发 claim/lease 测试与范围门禁断言。
4. 运行定向测试；记录耗时样本证明典型 <1s（允许抖动，但数量级为亚秒而非兜底 60s）。
5. 写 Evidence，映射 AC-002/004/006。

## 7. 路径访问契约

- **预计修改点：** 与 `expected_changes` 对齐；文件名可微调但须落在所列测试可写文件/support 测试工具目录。
- **可写范围：** 仅测试源与测试 support；**禁止**借机改 main 生产代码——若发现 T-01/T-02 缺口，停止并回退对应票或提 deviation。
- **只读上下文：** worker/runtime main、mapper、RedisUtils、common-notify、controller。
- **共享路径：** 无。
- **保留或不动：** 生产主代码路径（回归缺陷除外并升级 Lead）。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常：跨进程唤醒后 <1s claim | CrossProcess IT | `./mvnw -pl ruoyi-modules/ruoyi-notify -am -Dtest=NotifyOutboxWakeCrossProcessIT,NotifyOutboxClaimConcurrencyTest,NotifyOutboxWakeScopeGateTest test` | AC-002 跨进程证据成立 | `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-03.md</Path>` |
| 失败：双持有者并发 claim | Concurrency 测试 | 同上 | 至多一个完成写回；无双投 | 同上 |
| 回归：范围门禁 | ScopeGate 测试/路径审查 | 同上 + diff 审查 | 无配置页/common-notify 同步契约改动 | 同上 |
| 不适用：UI E2E | — | — | 不适用：原因：无 UI；跨进程 IT 已覆盖关键边界 | 同上 |

- **Workspace checks：** current-workspace 或 source-worktree 运行上述测试；缺基础设施时记阻塞而非假绿。
- **E2E disposition：** not-required：原因：以模块跨进程/并发 IT 作为跨边界证据，不要求 UI E2E；若 Goal 判定环境只能在 parent-candidate 跑容器，则由 Lead 在 parent-candidate 执行本票 IT。
- **E2E owner/environment：** Lead / parent-candidate（仅当 current-workspace 无法提供 Redis/DB 双进程夹具）。
- **Integration evidence：** commit 与父分支 SHA 记入 Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** 本票随 T-01/T-02 之后验证；不单独改变运行时默认。
- **兼容窗口：** 不适用：原因：测试票。
- **监控信号：** 复用 T-02 触发来源字段核对跨进程路径。
- **回滚或前向恢复：** 删除/回滚测试不影响生产；行为回滚走 T-01/T-02。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用。

## 10. 验收标准

- [ ] `AC-002`：存在跨进程（非仅同 JVM）证据表明写入后典型 <1s 被某实例 claim。
- [ ] `AC-004`：并发 claim 下至多一个有效 lease 完成写回。
- [ ] `AC-006`：范围门禁证明未改 common-notify 同步契约、未扩配置页/渠道账号。
- [ ] 已读取 Map 与适用 Skill。
- [ ] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/T-03.md</Path>`。
- [ ] 未超出 `writable_paths`；发现主代码缺陷时停止并升级。
- [ ] implementation/source commit 与父分支验证记录完整。
- [ ] E2E disposition 已按环境执行或明确 not-required。
- [ ] Map/Evidence 状态一致；无未批准偏差。

## 11. SKILL 调用计划

- `engineering-standards`：按通知与测试规则设计门禁，禁止假绿 skip。
- `ruoyi-module-guide`：核对测试触及的模块边界。
- `ruoyi-common-modules-guide`：跨进程观测使用 `RedisUtils` 能力边界。
- verify：执行命令并写 Evidence。

## 12. 停止、检查点与交付

- **用户交付要求与数量：** 无额外数量合同。
- **夹具不可用：** 阻塞本票并报告环境缺口，不降级为仅同 JVM 宣称 AC-002 通过。
- **与并行 change 冲突：** 只读主代码；配置页冲突升级 Lead。
- **完成出口：** 交回 Goal；不自启 P/I。
