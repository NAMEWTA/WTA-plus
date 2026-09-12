---
schema_version: 3
plan_contract_version: 1
plan_revision: 6
requested_deliverables: []
deliverable_policy: "用户未指定额外交付物名称或正整数数量；本 change 仅要求可验收的计划型 Tickets 与 Map，票数量由垂直切片决定（3），不以票数充当 requested_deliverables。"
artifact: tickets-map
change: 2026-09-11-notify-outbox-wake-poll
status: ready
---

# Tickets Map: 通知 Outbox 写入唤醒与慢速兜底轮询

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/evidence/</Path>`
- **可选 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/goal-plan.md</Path>`

## 1. 目标与拆分策略

三张票共同交付 `US-001`–`US-006` 与 `AC-001`–`AC-006`：先固定提交后跨进程唤醒发布合同，再让 Worker 订阅并改为默认 60s 兜底且统一 claim/dispatch，最后用跨进程/并发/范围门禁锁定验收，防止降级为同 JVM-only。

切片原则：

- 垂直按“写后唤醒 → Worker 调度 → 门禁证明”切开，不按 DB/后端/测试水平拆空票。
- 无 Prefactor：现有 ClaimService/Mapper/RedisUtils 可直接复用。
- 真实 DAG：T-02 消费 T-01 信号合同；T-03 验证 T-01+T-02 合成行为。
- 与并行 change `2026-09-10-notify-channel-config`：本 Map 写集收紧到 Outbox Worker/写入唤醒/相关测试，避开通知配置页与渠道账号。

### 总体实施背景

- 保留 Transactional Outbox 与 claim/lease；唤醒只是调度提示（I2）。
- 跨进程 Redis 唤醒为主，同 JVM 辅可选且不能顶替多实例 <1s（DEC-002/003/009）。
- 默认 `notify.outbox.poll-delay-ms=60000`；配置项名保留。
- 唤醒后只走既有 `claim`+`dispatch`；禁止 outboxId 默认直投。
- `next_attempt_at` 到期只靠慢速兜底；不建重试专用快轮询。
- 不改 `ruoyi-common-notify` 同步 Dispatcher；不扩通知配置页；不做 Outbox→MQ Relay。
- `@DSTransactional` 提交后回调优先 `DsTxEventListener` + 明确 `TransactionPhase`。
- Ticket workspace 已由 Goal Plan 固定为 current（串行）+ direct-parent；Wave 仅依赖投影，同时仅一个 implementation writer。Wave/Gate 权威见 Goal Plan。

### 项目 Skill 读取矩阵

| Applies To | Project Skill | Trigger / Scope | Read Timing | Purpose |
|---|---|---|---|---|
| ALL | `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` | 通知 Outbox/租约、Java 事务 afterCommit、测试门禁 | Map 后、Ticket 前 | 硬约束与质量门禁 |
| ALL | `<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>` | `ruoyi-notify` Outbox Worker/Runtime 事实 | Map 后、Ticket 前 | 模块入口与分层落点 |
| ALL | `<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>` | RedisUtils 发布订阅/list 跨进程信号 | Map 后、进入含 Redis 唤醒的 Ticket 前 | 复用 Redis 公共入口，禁止业务直连 Redisson |

扫描证据：已枚举 `.agents/skills/*/SKILL.md`。不适用 `namewta-fullstack-development`（无前后端页面/菜单/CRUD 切片）；不适用 `java-api-compatibility`（无对外 API 契约变更）；不适用 `deploy-namewta-environment` / `upstream-fork-sync` / `project-customization-delivery`。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/01-after-commit-cross-process-wake-publish.md</Path>` | 提交后跨进程唤醒发布；Redis 失败不回滚 | — | standard | medium | yes | rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4 | AC-002, AC-006 | W1/G1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/02-worker-wake-subscribe-and-slow-poll.md</Path>` | Worker 订阅唤醒；默认 60s 兜底；统一 claim/dispatch | T-01 | standard | medium | yes | rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4 | AC-001, AC-003, AC-005 | W2/G2 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/ticket/03-cross-process-lease-and-scope-gates.md</Path>` | 跨进程 <1s、并发不双投、范围门禁证据 | T-01, T-02 | standard | high | yes | rvp-implement:4c753e13-b41b-408e-8cc8-275810f995e4 | AC-002, AC-004, AC-006 | W3/G3 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立修改出另一套真相。

## 3. 依赖 DAG

```text
T-01 [READY]
  └─→ T-02 [READY]
        └─→ T-03 [READY]
```

边含义：T-02 需要 T-01 固化的跨进程信号合同与发布行为；T-03 需要写路径+Worker 合成后才能做跨进程/并发/范围门禁。无并行 Ready 写冲突边（串行 DAG）。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-02 | Worker 默认 delay 与空闲 claim 频率 | covered | 默认 60s 量级 |
| AC-002 | T-01, T-03 | T-01 发布路径；T-03 跨进程 <1s 门禁 | covered | 写路径+跨进程证据；禁止仅同 JVM |
| AC-003 | T-02 | 抑制唤醒后兜底 tick | covered | 漏唤醒仍可 claim |
| AC-004 | T-03 | 并发 claim/lease | covered | 至多一个完成写回 |
| AC-005 | T-02 | `next_attempt_at` 定向测试 | covered | 无重试快轮询合同 |
| AC-006 | T-01, T-03 | 路径审查 + ScopeGate | covered | 避开配置页/common-notify |

## 5. 并行与路径所有权

- implementation subagent 上限来自 Goal Plan 快照（config `max_implementation_agents=3`）；current 模式强制串行单 writer。
- 本 change 三票由 DAG 串行；writable 仍按票收紧。
- 跨 change：`2026-09-10-notify-channel-config` 宽写 `ruoyi-notify/**`（含配置页）。本 change 只写 Outbox Runtime 唤醒、`adapter/worker` 与相关测试。Lead 集成时禁止两 change 同时改同一文件；冲突面写清为配置页/绑定 vs Worker/唤醒。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无生产路径交集（信号合同串行） | 是 | DAG 串行 |
| T-02 | T-03 | 无（T-03 仅测试；worker 测试目录与 T-02 测试目录可能相邻但不要求并行） | 是 | DAG 串行 |
| T-01 | T-03 | 测试 support/outbox 可能相邻 | 是 | DAG 串行；T-03 不改 main |

## 6. Gate、Wave 与集成点

Goal Plan 权威：`<Path>{roots.state}/specdev/changes/2026-09-11-notify-outbox-wake-poll/goal-plan.md</Path>`。

- W1/G1：T-01 提交后跨进程信号合同稳定（afterCommit + Redis；失败不回滚）。
- W2/G2：T-02 Worker 订阅 + 默认 60s 兜底 + 统一 claim/dispatch。
- W3/G3：T-03 跨进程 <1s（Redis+DB）、并发 lease、范围门禁；不得降级仅同 JVM。

workspace：`current` + `direct-parent`；Lead=`rvp-lead:1c44f1c3-a61e-4d39-a7a7-f2d21ce5fb00`；implementation_agent_limit=3（Lead 不计；current 单 writer）。Goal Plan 当前因实现授权 not-authorized 而 `ready_for_execution: false`；获授权前不进入 I-implement。

## 7. 横切契约与风险

- Redis 短暂不可用：提交与 Outbox 持久化仍成功；最终投递靠兜底。
- 唤醒风暴/空批可接受；正确性优先。
- 重试可见性上界与默认 60s 同量级（已锁定接受）。
- 并行 channel-config change：避开 `controller/admin/NotifyConfig*`、场景绑定与账号表。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；
- Ticket ID、路径、依赖或 frontmatter 不一致时，以 Ticket 文件为权威并修复本 Map；
- 项目 Skill 新增或触发范围变化时，先同步读取矩阵并重新校验；
- Goal Plan 存在时，Wave/Gate/owner 以 Goal Plan 为编排权威；
- 依赖、合同覆盖或路径所有权变化后运行 validate-specdev；
- 内部工件不得使用相对 Markdown 链接。

## 9. 总控与恢复

从本 Map 进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>` 的 plan/run/resume/replan/verify。先运行 `<Path>{roots.workflows}/specdev/common/tools/ticket-control.mjs</Path>` 的 `--map` 只读检查，再按 `<Path>{roots.workflows}/specdev/P-goal-plan/references/map-control.md</Path>` 调用 I 和真实 Skill、验收、更新状态直到完成或明确阻塞；此工具本身不执行代码。

- frontmatter 中 `requested_deliverables` 为空数组：用户未要求额外命名交付数量。
- `deliverable_policy` 记录依据见上。
- 变更范围或验收后递增 `plan_revision` 并重算受影响闭包。
- 共享语义资源在 Ticket `resource_claims` 声明；信号合同由 T-01 产出、T-02 消费。
- 未闭合事务先查原网关；全部票 done 后仍需整体 Gate 与集成验收（由 P 编排）。
