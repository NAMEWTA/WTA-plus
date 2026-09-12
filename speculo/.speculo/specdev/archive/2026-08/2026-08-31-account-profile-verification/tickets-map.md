---
schema_version: 3
artifact: tickets-map
change: 2026-08-31-account-profile-verification
status: completed
---

# Tickets Map: 账户个人与企业档案及实名认证闭环

- **Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`
- **推荐 Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`

## 1. 目标与拆分策略

14 个 Ticket 共同交付 `US-001` 至 `US-022`：先建立模块/API/workflow 接缝与全量数据模型，再并行交付供应商、材料、个人和企业闭环，随后建立管理 API、前端公共合同及三个管理面，最后由 App 组合与真实 E2E Gate 汇合。

切片遵守以下边界：

- 数据 schema、Maven/API、前端 package/lock 和 App 组合各有唯一 shared owner；业务 Ticket 不改共享根文件。
- 个人、企业、材料、供应商和管理员按领域子树分隔；不可变来源和追加历史从首版 schema 起生效，不安排事后清洗式 prefactor。
- 后端先扩展公共合同和数据，业务能力随后展开；动态菜单和 App 入口在全部页面完成后才 contract/开放。
- T-14 是集成与发布 Gate，不承载新业务能力。由于 Ticket 数量、Deep 风险和共享路径均较高，进入实现前推荐执行 Goal Plan。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/{change}/ticket/01-profile-contracts-and-workflow-seam.md</Path>` | Profile 模块图、公共查询与 workflow 终止接缝 | — | deep | high | yes | codex:/root | AC-025,026,038,044 | W1 / foundation | done |
| T-02 | `<Path>{roots.state}/specdev/changes/{change}/ticket/02-profile-schema-reference-data.md</Path>` | 全量 schema、活动唯一约束、菜单权限与参考数据 | — | deep | critical | yes | codex:/root | AC-008,015,027,028,031-034,039,042 | W1 / data | done |
| T-03 | `<Path>{roots.state}/specdev/changes/{change}/ticket/03-verification-provider-framework.md</Path>` | 两类 provider SPI、manual 策略和回调证据 | T-01,T-02 | deep | high | yes | codex:/root | AC-036,037 | W2 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/{change}/ticket/04-material-catalog-and-lifecycle.md</Path>` | 材料树、必传校验和 OSS 引用生命周期 | T-01,T-02 | deep | high | yes | codex:/root | AC-032-035,042 | W2 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/{change}/ticket/05-person-application-workflow.md</Path>` | 个人申请、快照与 workflow 发布闭环 | T-03,T-04 | deep | critical | yes | codex:/root | AC-001,003-008,014,015,038 | W3 | done |
| T-06 | `<Path>{roots.state}/specdev/changes/{change}/ticket/06-person-rebind-unbind-notify.md</Path>` | 个人隐私探测、换绑、解绑和安全通知 | T-05 | deep | critical | yes | codex:/root | AC-009-014,016,017 | W4 | done |
| T-07 | `<Path>{roots.state}/specdev/changes/{change}/ticket/07-enterprise-application-workflow.md</Path>` | 企业申请、快照和唯一负责人发布 | T-03,T-04 | deep | critical | yes | codex:/root | AC-002-008,018,023,038 | W3 | done |
| T-08 | `<Path>{roots.state}/specdev/changes/{change}/ticket/08-enterprise-transfer-challenge.md</Path>` | 企业解绑与短信挑战负责人转移 | T-05,T-07 | deep | critical | yes | codex:/root | AC-019-023 | W4 | done |
| T-09 | `<Path>{roots.state}/specdev/changes/{change}/ticket/09-admin-profile-operations.md</Path>` | 管理查询、审核上下文和高风险覆盖命令 | T-01,T-04,T-05,T-07 | deep | critical | yes | codex:/root | AC-024-031,039,041-044 | W4 | done |
| T-10 | `<Path>{roots.state}/specdev/changes/{change}/ticket/10-profile-frontend-contracts.md</Path>` | Profile domain、transport、runtime 与 package 合同 | T-04,T-05,T-06,T-07,T-08,T-09 | deep | high | yes | codex:/root | AC-009,010,024,031,032,034,038-041 | W5 / frontend foundation | done |
| T-11 | `<Path>{roots.state}/specdev/changes/{change}/ticket/11-material-tag-admin-ui.md</Path>` | 材料分类与标签树管理页 | T-10 | standard | medium | yes | codex:/root | AC-032-035,040 | W6 | done |
| T-12 | `<Path>{roots.state}/specdev/changes/{change}/ticket/12-person-profile-admin-ui.md</Path>` | 个人档案列表、详情、审核和管理操作 | T-10 | deep | high | yes | codex:/root | AC-010-017,024-031,039-041 | W6 | done |
| T-13 | `<Path>{roots.state}/specdev/changes/{change}/ticket/13-enterprise-profile-admin-ui.md</Path>` | 企业档案列表、详情、审核和负责人操作 | T-10 | deep | high | yes | codex:/root | AC-018-031,039-041 | W6 | done |
| T-14 | `<Path>{roots.state}/specdev/changes/{change}/ticket/14-admin-composition-and-release-gate.md</Path>` | admin-web 显式组合、assembly 与真实发布 Gate | T-02,T-11,T-12,T-13 | deep | critical | yes | codex:/root | AC-001,002,005,007,024,025,031,034,038-044 | W7 / release Gate | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影，不得独立修改出另一套真相。

## 3. 依赖 DAG

```text
W1  T-01 [module/API/workflow seam]       T-02 [schema/reference data]
       └──────────────┬────────────────────┘
W2                    ├─→ T-03 [provider prefactor]
                      └─→ T-04 [material lifecycle]
                              │
W3                    ┌────────┴────────┐
                      T-05 [person]     T-07 [enterprise]
                         │      ╲       ╱   │
W4                       T-06    T-08      T-09 [admin API]
                           ╲       │       ╱
W5                          └────→ T-10 [frontend contracts]
                                      │
W6                         ┌────────────┼────────────┐
                            T-11         T-12         T-13
                              ╲            │            ╱
W7                             └────────→ T-14 [RELEASE GATE]
```

补充真实开始条件：T-08 同时等待 T-05 与 T-07；T-09 同时等待 T-01、T-04、T-05、T-07；T-10 等待全部后端用户/管理 transport 稳定；T-14 还直接等待 T-02，以数据库种子与真实环境作为 Gate 前提。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-05,T-14 | person service/MVC/E2E | covered | 草稿、提交、不可变快照、流程启动 |
| AC-002 | T-07,T-14 | enterprise service/MVC/E2E | covered | 企业提交与候选负责人 |
| AC-003 | T-05,T-07 | persistence | covered | 退回/撤销后新快照 |
| AC-004 | T-05,T-07 | publication transaction | covered | 待审不污染 current |
| AC-005 | T-05,T-07,T-14 | ProcessEvent/E2E | covered | 完成发布与幂等 |
| AC-006 | T-05,T-07 | ProcessEvent matrix | covered | 非完成终态不改绑定 |
| AC-007 | T-05,T-07,T-14 | conditional context/fault | covered | workflow 缺失失败关闭 |
| AC-008 | T-02,T-05,T-07 | DB concurrency | covered | 活动唯一性 |
| AC-009 | T-06,T-10 | transport/privacy | covered | 普通探测最小响应 |
| AC-010 | T-06,T-10,T-12 | rebind privacy/UI | covered | 精确匹配后才给掩码 |
| AC-011 | T-06,T-12 | snapshot contract | covered | 换绑意图与版本冻结 |
| AC-012 | T-06,T-12 | lock/transaction | covered | 原子换绑 |
| AC-013 | T-06,T-12 | race/fault | covered | 拒绝与竞态不切换 |
| AC-014 | T-05,T-06,T-12 | reclaim integration | covered | 未注销档案重新认领 |
| AC-015 | T-02,T-05,T-12 | succession | covered | 注销后新档案链 |
| AC-016 | T-06,T-12 | after-commit notify | covered | 通知失败不回滚 |
| AC-017 | T-06,T-12 | unbind service/UI | covered | 个人即时解绑 |
| AC-018 | T-07,T-13 | ownership matrix | covered | 企业负责人不可顶替 |
| AC-019 | T-08,T-13 | challenge service/UI history | covered | 三要素与目标短信 |
| AC-020 | T-08,T-13 | Redis clock/UI state | covered | 验证码协议 |
| AC-021 | T-08,T-13 | transfer transaction | covered | 原子负责人切换 |
| AC-022 | T-08,T-13 | transfer concurrency | covered | 确认时重验资格 |
| AC-023 | T-07,T-08,T-13 | unbind/rebind | covered | 企业解绑后无恢复 |
| AC-024 | T-09,T-10,T-12,T-13,T-14 | authorization/E2E | covered | review 能力闭合 |
| AC-025 | T-01,T-09,T-12,T-13,T-14 | workflow termination | covered | 管理员唯一最终决定 |
| AC-026 | T-01,T-09,T-12,T-13 | decision fence | covered | 终止失败与迟到事件 |
| AC-027 | T-02,T-09,T-12,T-13 | admin create | covered | 独立来源与可选绑定 |
| AC-028 | T-02,T-09,T-12,T-13 | rollback | covered | 直建全事务回滚 |
| AC-029 | T-09,T-12,T-13 | version history | covered | 覆盖版本不改历史 |
| AC-030 | T-09,T-12,T-13 | binding state | covered | 暂停/恢复/解绑事件 |
| AC-031 | T-02,T-09,T-10,T-12,T-13,T-14 | MVC/page/E2E | covered | 注销默认排除及只读 |
| AC-032 | T-02,T-04,T-10,T-11 | material tree | covered | 受限树结构 |
| AC-033 | T-02,T-04,T-11 | tag invariant | covered | 生命周期与系统 code |
| AC-034 | T-02,T-04,T-10,T-11,T-14 | OSS/material/E2E | covered | 数量、大小、类型、必传 |
| AC-035 | T-04,T-11 | OSS lifecycle | covered | detached 审计与不删除 |
| AC-036 | T-03 | provider registry | covered | 固定启用策略与重试 |
| AC-037 | T-03 | callback contract | covered | 鉴权、幂等、迟到证据 |
| AC-038 | T-01,T-05,T-07,T-10,T-14 | ruoyi-api compatibility | covered | 单个/批量非敏感摘要 |
| AC-039 | T-02,T-09,T-10,T-12,T-13,T-14 | auth matrix/E2E | covered | 完整能力闭合 |
| AC-040 | T-10,T-11,T-12,T-13,T-14 | Vitest/Playwright | covered | 三菜单与动态路由 |
| AC-041 | T-09,T-10,T-12,T-13,T-14 | MVC/download/E2E | covered | 明文和材料权限 |
| AC-042 | T-02,T-04,T-09,T-14 | SQL/OSS invariant | covered | 无物理删除 |
| AC-043 | T-09,T-14 | log capture | covered | 审计存在、敏感原文不入日志 |
| AC-044 | T-01,T-09,T-14 | system regression | covered | 认证只建立绑定 |

不存在 `uncovered` 或 `deferred` 合同。重复覆盖表示单元/集成/页面/发布 Gate 的不同验证接缝，不表示重复实现所有权。

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`，Goal Plan 可进一步降低且不含 Lead。
- review/research/test-observation agent 保持只读；Lead 持有 SpecDev 状态与父分支集成。
- Ticket frontmatter 是路径权威；shared owner 只允许所属 Ticket 写共享文件，其他 Ticket 只读。
- 正式实现 workspace 模式与派单由 Goal Plan 决定；当前 Map 不授权 implementation、worktree、提交、合并或生产操作。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | W1 可并行；分别持有 Maven/API 与 SQL |
| T-03 | T-04 | 无 | 否 | W2 可并行；供应商与材料子树隔离 |
| T-05 | T-07 | 无 | 否 | W3 可并行；个人与企业叶子隔离 |
| T-06 | T-08 | 无 | 否 | W4 可并行；T-08 只读个人资格 API |
| T-08 | T-09 | 无 | 否 | W4 可并行；transfer/admin 子树隔离 |
| T-11 | T-12 | 无 | 否 | W6 可并行；material-tag/person 子树隔离 |
| T-12 | T-13 | 无 | 否 | W6 可并行；person/enterprise 子树隔离 |
| T-10 | T-11/T-12/T-13 | 公共包根由 T-10 唯一写 | 是 | T-10 先冻结 exports；后续仅写子树 |
| T-14 | 所有前序 | App 组合由 T-14 唯一写 | 是 | 最终串行汇合并执行发布 Gate |

共享文件唯一所有者：

| Shared owner | 共享路径 |
|---|---|
| T-01 | 根/模块/admin POM、Profile/Workflow `ruoyi-api` 公共合同 |
| T-02 | 父仓库 NAMEWTA `50-namewta-ddl.sql`、`60-namewta-dml.sql` 完整基座 |
| T-10 | profile 前端包入口、admin package、`pnpm-lock.yaml` |
| T-14 | admin `services.ts`、`adminManifestRegistry.ts` |

## 6. Gate、Wave 与集成点

| Wave | 候选 Ticket | 行为里程碑 | Gate 退出条件 |
|---|---|---|---|
| W1 | T-01,T-02 | 模块/API/数据地基 | reactor 合同与 schema/种子校验通过 |
| W2 | T-03,T-04 | 外部验证和材料 prefactor | provider 安全矩阵与 OSS 生命周期通过 |
| W3 | T-05,T-07 | 两类申请/流程闭环 | 快照、流程状态、原子发布和并发测试通过 |
| W4 | T-06,T-08,T-09 | 换绑/转移/管理覆盖 | 隐私、短信、decision fence、权限矩阵通过 |
| W5 | T-10 | 前端公共合同 | transport/manifest/typecheck 稳定 |
| W6 | T-11,T-12,T-13 | 三个管理面 | 组件行为、权限/状态投影通过 |
| W7 | T-14 | App 组合与发布 Gate | required E2E、core/full、system/log/SQL 回归全绿 |

Goal Plan 已选择 `lead-directed + required + candidate-merge`：Lead 为 `codex:/root`，implementation agent 上限 3，集成尝试上限为 `null`。Wave 内 source worktree 可以按依赖和路径并行，Lead 按 T-01 至 T-14 的集成序号串行构建 parent-candidate；每个 required E2E 只在 candidate 状态有效。

正式 Gate 为 G0 执行授权、G1 合同与数据地基、G2 验证证据地基、G3 两类认证垂直路径、G4 所有权与运营闭环、G5 前端公共合同、G6 三管理面、G7 发布就绪。implementation commit 与 Lead-owned local candidate integration 均已授权，G0 已关闭；计划已可由 I-implement 从 T-01/T-02 开始执行。完整编排权威见 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`。

## 7. 横切契约与风险

- **数据：** 主体、工作副本、不可变提交/管理员来源、档案版本、绑定事件、材料引用、provider attempt 和审计分离；无业务物理删除。
- **并发：** 活动身份/账户唯一性由 DB+锁+幂等保证；个人换绑、企业转移和管理员覆盖都在提交时重验版本。
- **安全：** 后端权限是权威；完整能力闭合；明文/材料只对有权管理者开放；普通探测不回显，只有个人精确匹配才返回手机号掩码。
- **日志：** 用户已明确无需展示脱敏，但安全合同仍禁止姓名、证件号、手机号、后四位、验证码、材料和供应商敏感原文进入持久日志。
- **兼容：** 不修改 system 登录、注册、角色、菜单授予、Client 或登录域；ProfileService 消费者不读内部表。
- **workflow：** 两套独立 flowCode；core 无 workflow 时查询/草稿可用、提交失败关闭；管理员覆盖终止失败整体失败。
- **迁移：** DDL -> DML -> 后端 -> 前端组合 -> 人工赋权；生产 migration、授权和高风险命令均需外部批准。
- **恢复：** 撤入口不删除历史；已发布档案/版本/绑定只能前向修订或状态处置。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；
- Ticket ID、路径、依赖或 frontmatter 不一致时，以 Ticket 文件为权威并修复本 Map；
- Goal Plan 存在时，Wave、Gate 和 owner 以 `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>` 为编排权威；
- 依赖、合同覆盖或路径所有权变化后运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`；
- 内部工件不得使用相对 Markdown 链接；
- 本 Map 和 Ticket 均不构成实现、提交、合并、生产迁移或权限赋予授权。
