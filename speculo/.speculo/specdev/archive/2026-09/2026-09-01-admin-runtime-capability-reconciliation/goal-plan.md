---
schema_version: 6
artifact: goal-plan
change: 2026-09-01-admin-runtime-capability-reconciliation
status: completed
modes: [migration, high-assurance, release-coordination]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: null
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: Admin 运行能力与开发环境收敛

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

将仓库声明、发布配置、开发数据库与两个 admin 实例收敛到同一最终态：本人 OpenAPI 与系统管理入口可用，Nacos 入口显示在系统监控且名称正确，生成器数据库/菜单面彻底消失；默认安装仍保持 OpenAPI 关闭且无 secret 泄露。

### Success and False Completion

成功要求 AC-001..AC-016 全部有 Lead 可复查 Evidence；T-01/T-02 均有非空 implementation commit 和 direct-parent result；SQL fresh/upgrade/replay/conflict、release/Spring/frontend gates、用户明确无备份的开发库迁移、双实例滚动和登录浏览器验收全部通过。

以下均是伪完成：只改源码不修目标数据库；只改菜单不启用 Controller；把 OpenAPI 默认改为 true；只验证一个实例；只看截图不验证 HTTP/DB；删除菜单但保留生成器表；将真实 KEK 写入 Git/Evidence；用静态测试替代真实目标环境。

### Non-goals

不修改 OpenAPI 协议/API/凭据模型，不建立 Nacos SSO 或 RuoYi CRUD，不恢复生成器，不自动给普通角色授权，不操作同机 CDE/生产，不 push/PR/remote merge，不执行广泛 cleanup。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | `USER-DECISION:2026-09-01-menu-corrections`、`USER-DECISION:2026-09-01-execute-confirmed-plan` 与 `USER-DECISION:2026-09-01-no-backup` | 最终菜单语义、实现/开发环境执行授权、本次目标库无备份决定 | 更新真正拥有该决定的工件；不从开发授权推断生产权限 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | default-off、append-only、物理收缩与领域语义 | 返回上游 ADR/CONTEXT 处理，不由 Ticket 改写 |
| 3 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为、范围、AC 与安全约束 | 下游不得降级或扩展 |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 实现、路径、验证和恢复合同 | Goal Plan 只编排跨 Ticket 顺序 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/diagnosis.md</Path>` 与当前代码/DB/运行事实 | 已验证根因与基线 | 新事实冲突时暂停 Gate 并更新真正 owner |
| 6 | `<Path>{roots.state}/specdev/changes/2026-08-30-openapi-common-module/</Path>`、`<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/</Path>`、`<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/</Path>` | 历史实现合同 | 最新用户决定只覆盖菜单位置/名称，不回退其安全与退役合同 |

## 2. Execution Graph

### DAG and Critical Path

```text
G0 ready spec + authorization + clean product baseline
  |
  v
T-01 append-only SQL + migration tests
  -> G1 isolated fresh/upgrade/replay/conflict + direct-parent result
  -> G2 explicit no-backup approval + development DB final state
       |
       v
T-02 release config + static/runtime regression
  -> G3 default-off/fail-closed/release/frontend + direct-parent result
  -> G4 server1 -> server2 -> Admin browser final E2E
```

关键路径是 `T-01 -> T-02`。current 模式固定串行，任何 Gate 失败都不启动后续 Ticket 或实例。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | G0；backend/parent 基线固定 | backend NAMEWTA DDL/DML + adminruntime tests | T-01 为两个 SQL 文件唯一 writer | G1/1，G2/2 |
| W2 | T-02 | G2；重读 parent/backend/frontend 基线 | parent release backend Compose/env/test | T-02 为三个 release 文件唯一 writer | G3/3，G4/4 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | OpenAPI/Nacos/生成器数据库最终态 | — | `current` / parent+backend `main` | Lead `codex:/root` | required：隔离 MySQL 与用户明确无备份的开发库升级 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-02 | 双实例显式 OpenAPI 与 Admin 最终体验 | T-01 | `current` / parent `main` | Lead `codex:/root` | required：目标双实例 HTTP/log 与登录浏览器 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- AC-001..AC-016 全部 passed，无未批准 deferred。
- T-01 backend child/aggregate 与 T-02 aggregate 都有非空 commit，Lead 核对 diff、路径、dirty、ancestry 和 result SHA。
- 历史 SQL 前缀不变；新块 fresh/upgrade/replay 收敛、冲突写前失败；用户无备份批准已记录且目标库最终诊断全真。
- release/Spring/frontend 回归通过；公开配置 default-off 且无 secret；目标两实例同 KEK/version 并逐个通过健康、路由和日志门。
- 登录 Admin 可观察到个人 OpenAPI、系统管理 OpenAPI管理、系统监控 Nacos配置中心，无系统工具/代码生成。
- change/Ticket/Map/Plan/Evidence 与实际 Git/运行状态一致，无未归属项目修改或未验证 secret 暴露。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 执行与基线 | Spec/Tickets/Plan ready | 用户执行决定；current/direct-parent 授权；parent `82cd80bf990a6182e4fd16a6884f402511098998`、backend `7864237127a1ab7644ec03706f930c6856987f0e`、frontend `a2d79cf7bc3e95f69b4aa2fc814e4ba0156687e2`；product trees clean | 全部 | Lead / 用户 | 不开始项目写入；重新归属漂移 |
| G1 SQL candidate | G0 | T-01 static + isolated MySQL matrix、historical prefix、backend/aggregate result SHA | 开发库写入、T-02 | Lead；Deep 已由用户批准 | 修正 T-01，不执行目标迁移 |
| G2 开发库最终态 | G1 | 当前目标定位；用户无备份批准；0-row/对象身份复核；新块成功；final diagnostic 与 role query | T-02 | Lead / 用户明确批准无备份执行 | 停止 rollout；保持 OpenAPI disabled，修正后前向重放 |
| G3 发布候选 | G2 | T-02 Node/Compose、Spring assembly、Vitest/typecheck/build、secret scan 与 aggregate result SHA | 远端配置/滚动 | Lead | 修正 T-02；目标实例不变 |
| G4 运行最终态 | G3 | server1/server2 逐个 health/HTTP/log；配置一致；登录浏览器四项断言；DB final query | change 完成 | Lead；目标开发环境已授权 | 在失败实例关闭 OpenAPI/恢复上一配置；不推进下一实例 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-005..AC-011、AC-013 | T-01 | static SQL、isolated MySQL、target DB | T-01 | passed |
| AC-001..AC-004、AC-012 | T-02 | Node/Compose、Spring assembly、real HTTP/log | T-02 | passed |
| AC-014、AC-016 | T-02 | Vitest/build + Admin browser | T-02 | passed |
| AC-015 | T-02 | takeover rollout | T-02 | passed |
| ADR-001..ADR-005 | T-01,T-02 | Ticket constraints + Gates | T-01,T-02 | passed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev、Evidence、E2E、direct-parent、开发环境和最终回复 owner |
| Implementation subagents | config 快照上限 3，Lead 不计入；current 同时 writer=1 | `<Path>{roots.state}/specdev/config.json</Path>`；当前协作约束下用户未要求 subagent，因此本计划不派单 |
| Integration attempts | `null`（unlimited） | config 快照；仍受停止条件和 deviation control 约束 |
| Read-only agents | 无 SpecDev 数字上限 | 本计划未授权/安排 subagent；任何后续只读派单仍不得写状态或竞争环境 |
| Dispatch | execution-time dynamic | `subagent-delivery operation=plan` 合同已读取；本次由 Lead 直接实现，无 Dispatch Packet |

若用户后续明确要求派单，implementation/review/research/test-observation 都必须通过不可变 Packet；current implementation 绑定当前 parent result、`workspace_ref=current`、唯一 writer lock、路径、非 E2E checks 和 commit 授权。subagent 不写 SpecDev/Evidence、不运行或拥有 E2E、不推进父分支；Lead 独立验收。外部 provider/源码发送仍需独立授权。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | G0 parent/backend refs | `current` / parent+backend `main` | SQL contract、isolated MySQL、admin reactor | backend 非空 commit；aggregate 精确 gitlink commit | Lead 核对历史前缀、无备份批准、DB upgrade/final state | `a21d0fdb7fa8782cfd83afa9237c74a5311cfa45` |
| T-02 | T-01 aggregate result | `current` / parent `main` | Node/Compose、Spring、Vitest/typecheck/build | aggregate 非空 commit | Lead 逐实例 rollout/HTTP/log + browser | `ae3689ec419fb97e0a216e4ab44b85a3517d3bd9` |

多仓库 T-01 先形成 clean backend commit，再由 aggregate commit 精确记录 backend gitlink；T-02 只修改 parent-owned release 文件。每个 Ticket 的 result 等于通过 direct-parent 与适用 E2E 后的 aggregate implementation commit。失败时不开始下一 Ticket，不把其他范围混入修正 commit；父/子 HEAD 漂移则 checkpoint stale，重读归属和验证。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | authorized | 用户确认执行计划；严格串行、Lead 唯一 writer、限 writable/shared paths |
| Ticket worktree local changes | not-authorized | 本计划选择 current，不创建 worktree |
| Implementation commit | authorized | 每 Ticket/每被修改仓库必需；已记录于 `.status.json` |
| Local direct-parent verification and parent update | authorized | current 模式必需；已记录于 `.status.json` |
| Local candidate integration | not-authorized | 本计划不使用 candidate-merge |
| Development DB migration without backup | authorized-with-conditions | 仅目标 NAMEWTA 开发库；用户明确批准无备份，仍要求 G1、0-row、对象身份与 SQL preflight 全部通过 |
| Development private config/rollout | authorized-with-conditions | 仅目标两个 admin 实例；G2/G3 关闭，逐实例验证，不操作 CDE |
| Push / PR / remote merge | not-authorized | 不从本地实现或部署授权继承 |
| Production/CDE/destructive volume actions | not-authorized | 不使用 `down -v`，不碰 CDE/生产/广泛目录 |
| Branch/worktree/source cleanup | not-authorized | 不随完成自动继承 |

### Evidence Return

Lead 记录每个仓库的 parent-before/implementation/result SHA、实际路径、clean/dirty、命令结果、E2E 环境和未运行项。数据库 Evidence 记录用户无备份批准、目标定位、写前计数/身份与最终态，不记录凭据或数据正文；HTTP/log/browser 输出先脱敏，任何 KEK、Token、AppSecret、数据库/Redis 凭据都不写入 Evidence。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 只追加 NAMEWTA SQL 新块，历史前缀和冻结上游 SQL不变。
- OpenAPI default-off；目标环境显式 enable；错误配置 fail closed。
- 固定菜单 ID 与 component/path/permissions 不变；不自动授予普通角色。
- 生成器只删除精确冻结目标；表非空、对象身份不符或冲突状态立即停止；本次不创建迁移前备份。
- 两实例 KEK/version 同源；真实 secret 只在忽略且 `0600` 的远端私密配置。
- 数据库先于 rollout；server1 通过后才推进 server2；同机 CDE、生产和远端 Git 始终排除。

### Verification Integrity

JUnit 静态解析不代替真实 MySQL；隔离 MySQL 不代替当前混合目标库；Spring/Vitest 不代替目标双实例和登录浏览器。禁止改弱断言、修改历史 SQL 规避 upgrade、用 `--force` 跳过 SQL、用 HTTP 200 代替业务 code 判断、只看单实例/LB、输出 secret 辅助诊断或用截图代替 DB/HTTP 断言。

### Migration or Release Sequence

1. T-01 red tests -> append-only DDL/DML -> static/isolated MySQL -> backend/aggregate commits。
2. 只读确认目标环境与用户无备份批准，复核生成器表 0 行、主键身份和菜单冲突前置。
3. 仅执行新 DDL/DML，验证菜单/schema/role 最终态并按现有机制刷新菜单缓存。
4. T-02 red Node test -> Compose/env change -> release/Spring/frontend checks -> aggregate commit。
5. 定位私密 `.env`，安全生成/注入同一 KEK/version，确认权限和 Compose rendering 不泄密。
6. server1 recreate/health/route/log -> server2 recreate/health/route/log；任一失败停止。
7. 重新登录 Admin，验证个人 OpenAPI、两个菜单和生成器消失；final DB/HTTP/secret/Git reconcile。

### Risks, Monitoring and Recovery

- **不可逆表删除：** 用户已批准无备份；以 0-row/对象身份/preflight 为硬门，失败只停止 rollout 并前向修复，不制造空兼容表。
- **权限投影错误：** 监控固定菜单字段、重复 component/perms 和 role-menu 增量；冲突写前停止。
- **KEK 不一致/泄露：** Compose rendering 只检查 key 存在和变量引用；运行检查脱敏。泄露即停止、轮换 KEK 并审计凭据，不继续 rollout。
- **双实例分歧：** 逐实例健康、路由和日志；首实例失败恢复 disabled/上一配置，第二实例不动。
- **缓存/会话陈旧：** 数据库最终态后使用现有缓存刷新或重新登录，不宽泛清 Redis。
- **direct-parent 失败：** 保留当前 Ticket commit/checkpoint，修正同一范围并重跑；不开始下一 Ticket。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。路径/局部实现偏差返回 Ticket；菜单/API/范围返回 Spec；安全/迁移/恢复取舍返回 ADR/用户。任何执行者不得先扩大目录、secret、数据、CDE、生产或远程 Git 权限再补批准。

## 6. Progress and Decisions

### Current Status

- G0 已关闭：Spec、Tickets、Goal Plan ready；用户已授权执行、implementation commits 和 local direct-parent；product 子仓 clean。
- 工作区策略采用推荐默认 `current/direct-parent`；两个 Ticket 严格串行，由 Lead 直接实施。
- T-01 已完成：G1 由 backend `9f5d382ada11e4bbc01bb7b49ca8ed4c6770f6ef` 与 aggregate `1bae24677e601b6313820a3198fc0e8690ab8248` 关闭；无备份 follow-up 为 backend `c7e926c8ac663b1fc83e1df2abf39ffcebbecef3`、aggregate result `a21d0fdb7fa8782cfd83afa9237c74a5311cfa45`。
- G2 已关闭：目标库写前 0-row/身份/冲突检查通过，DDL/DML 最终态四项全真，重放前后指纹一致且普通角色授权增量为 0。
- T-02 已完成：release Node `18/18`、Spring assembly `8/8`、凭据合同 `4/4`、最终 Admin reactor `330` 项、前端测试/typecheck/build 和目标 Compose parse 全部通过；aggregate result 为 `ae3689ec419fb97e0a216e4ab44b85a3517d3bd9`。
- G3/G4 已关闭：两实例最终镜像一致、healthy/0 重启、OpenAPI 配置一致且日志扫描为 0；登录浏览器确认个人 OpenAPI 空态、OpenAPI管理、Nacos配置中心和生成器入口退役全部符合合同。
- 用户明确决定本次目标开发库“无需备份”，已由 `ADR-005` 接管恢复边界；该决定不降低其他 Gate。

### Pending Decisions and Blockers

- 无产品、实现或运行 blocker；change 已完成。
- 用户选择可见终端后又明确要求直接 SSH 连接，最终接管与滚动均在可见会话中完成。
- 远程 push、生产/CDE、角色授权、cleanup 持续未授权且不阻止本 change。

### Resume Protocol

恢复时依次读取 Goal Plan、当前 Ticket、最新 Evidence、parent/backend/frontend HEAD/dirty 和目标环境最近 Gate。只从最后 passed aggregate result 或当前 Ticket implementation checkpoint 继续；若目标环境/instance 状态漂移，先重新打开对应 Gate，不盲目重放破坏性步骤。

## Assumptions

- 用户确认“按计划执行”且未要求 worktree，因此采用工作流推荐默认 `current`，只作用于本 Goal Plan。
- G0 记录的三个 HEAD 是规划时实测基线；开始每 Ticket 前重读，漂移则更新 Evidence/checkpoint 而非回退用户变化。
- 目标是现有 NAMEWTA 开发环境；任何路径、实例或数据库标识在写入前都通过只读发现确认，不能依赖名称猜测。
