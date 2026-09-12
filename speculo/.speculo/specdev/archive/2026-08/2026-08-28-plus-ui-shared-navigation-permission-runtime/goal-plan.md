---
schema_version: 6
artifact: goal-plan
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
status: completed
modes: [migration, high-assurance, release-coordination]
orchestration: lead-directed
lead: codex:leadership-epoch-1
implementation_agent_limit: 3
integration_attempt_limit: 7
ticket_workspace_policy: required
integration_gate: candidate-merge
ready_for_execution: false
---

# Goal Plan: Plus UI 共享导航与权限运行时收口

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在不改变后端 Client 裁剪、HTTP/JSON 合同和 Admin 用户行为的前提下，把 Vue 权限指令收口到独立 Web Kit，把服务端菜单确定性流程收口到 Platform App Runtime，把 Admin permission Store 归位为 App 自有 navigation Store，并删除本地动态页面兜底与旧入口。最终只保留 `getInfo -> getRouters -> addRoute -> replace` 和 manifest-only 的正式路径。

### Success and False Completion

成功必须同时满足：T-01 至 T-05 的历史 Evidence 保持有效；T-06 至 T-08 都有通过的 source commit、parent-candidate、前端父分支 result 与 Lead Evidence；`AC-001` 至 `AC-015` 全部闭合；权限异常 DOM 失败关闭、递归菜单边界、显式 Router adapter 和并发 E2E 稳定性均通过；独立 `CR-002` 状态为 `approved`；旧 Store、私有权限指令、`dynamicRoutes`、`filterDynamicRoutes` 和本地动态 `views` glob 继续零匹配。

以下均属于伪完成：忽略 `CR-001=request-changes` 而沿用旧 completed 状态；把“抛错”等同于 DOM 失败关闭；以双重断言代替 runtime narrowing；以失败后的单次重跑替代并发稳定证据；source worktree 自报 E2E；candidate 未进入 `plus-ui-namewta/main`；通过 alias、功能开关、提高 retry、降低 workers、删除测试或放宽架构规则制造绿色。

### Non-goals

- 不修改后端、数据库、Client 策略、领域 API 或 Web Domain 页面业务功能。
- 不激活 client-web、mobile-web 或 miniapp-taro。
- 不建立公共 Pinia Store、公共 Router、微前端或兼容门面。
- 不推送、创建 PR、远程合并、部署或发布。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | worktree 偏好、零兼容、实现与集成批准 | 更新真正拥有决定的工件和授权状态 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/CONTEXT.md</Path>` | 当前架构与领域语义 | 架构变化返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业永久决定 | 当前 change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>` | 外部行为、范围与验收 | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/</Path>` | 单 Ticket 契约和路径 owner | Goal Plan 只编排 |
| 6 | 当前代码、测试和 Git | 基线与可行性 | 冲突时暂停并按偏差等级返回 owner |
| 7 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/reviews/CR-001.md</Path>` | 固定实现评审 finding 与 satisfied-when | `request-changes` 必须通过新 Ticket 和独立复审关闭 |

## 2. Execution Graph

### DAG and Critical Path

```text
G-00 Authorization and clean baseline
  -> T-01 behavior freeze
       -> T-02 Web permission host ----┐
       -> T-03 server-menu runtime ----┴-> G-20 shared contracts
                                             -> T-04 Admin migration
                                                  -> G-30 vertical behavior
                                                       -> T-05 contract + final validation
                                                            -> G-40 final parent result
                                                                 -> CR-001 request-changes
                                                                      -> G-50 fresh repair authorization
                                                                           -> T-06 permission fail-close -----┐
                                                                           -> T-07 recursive menu boundary ---┴-> G-55 repair convergence
                                                                                                                  -> T-08 stable E2E + final gates
                                                                                                                       -> G-60 CR-002 approved
                                                                                                                            -> archive readiness
```

历史关键路径与整改关键路径均已完成：`G-00 -> T-01 -> max(T-02,T-03) -> T-04 -> T-05 -> G-40 -> G-50 -> max(T-06,T-07) -> G-55 -> T-08 -> G-60`。最终前端 result 为 `07962c7cad9ca4db168b3c423b9e3675f312a874`，`CR-002` 为 `approved`。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| 0 | T-01 | G-00；前端 clean main 与基线命令通过 | Playwright 基线文件 | 无 | 1 / G-10 |
| 1 | T-02 | G-10 | Web Kit permission、Admin manifest、lockfile | T-02：package manifest/lockfile | 2 / G-20 |
| 1 | T-03 | G-10 | Platform App Runtime | T-03：App Runtime 公共合同 | 3 / G-20 |
| 2 | T-04 | G-20 | Admin 指令、navigation Store、守卫与消费者 | T-04：Admin migration paths | 4 / G-30 |
| 3 | T-05 | G-30 | Router 收缩、架构工具、长期文档与 Skill | T-05：架构/长期文档 | 5 / G-40 |
| 4 | T-06 | G-50；T-05 历史 result | Web Kit permission 实现/测试/README | T-06：Web Kit permission | 6 / G-55 |
| 4 | T-07 | G-50；T-05 历史 result | Admin Domain、App Runtime、navigation Store、Router adapter | T-07：App Runtime 共享合同 | 7 / G-55 |
| 5 | T-08 | G-55 | 两条 Admin Playwright spec | 无 | 8 / G-60 |

required 模式下 Wave 4 最多同时存在两个 implementation owner；其他当前 Wave 只有一个。父仓库的 Speculo、Evidence、review 和 submodule pointer 始终由 Lead 在当前聚合工作区处理，不创建 docs 产品 worktree。本次用户已授权 T-06 至 T-08 implementation commit 与 local candidate integration，G-50 已关闭；后续 source cleanup 也已单独授权并完成。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 浏览器基线脱离 legacy fallback | — | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-01` | execution-time dynamic | required：Lead 在 parent-candidate 跑定向 Playwright | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-01.md</Path>` |
| T-02 | Vue Web 权限宿主公共合同 | T-01 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-02` | execution-time dynamic | not-required：无 App 生产消费者变化 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-02.md</Path>` |
| T-03 | 服务端菜单纯运行时与诊断 | T-01 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-03` | execution-time dynamic | not-required：纯函数公共能力 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-03.md</Path>` |
| T-04 | Admin 使用新宿主和 navigation Store | T-02, T-03 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-04` | execution-time dynamic | required：Lead 跑权限/路由组合 E2E | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-04.md</Path>` |
| T-05 | legacy 零匹配、架构 Ratchet、全量验收 | T-04 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-05`；父 docs 由 Lead current workspace | execution-time dynamic + Lead parent-doc owner | required：Lead 跑完整 Playwright | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-05.md</Path>` |
| T-06 | 权限异常先移除 DOM 再抛错 | T-05 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-06` | execution-time dynamic | not-required：真实 Vue 生命周期直接判卷；T-08 完整回归 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>` |
| T-07 | 递归菜单模型与显式 Router adapter | T-05 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-07` | execution-time dynamic | required：Lead 跑 app-runtime baseline | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>` |
| T-08 | 稳定 locator 并重新执行最终 Gate | T-06, T-07 | `specdev-worktree/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-08` | execution-time dynamic | required：5 workers/0 retries 重复完整套件 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-08.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- 每个非 cancelled Ticket 有非空 source commit、通过的 candidate、`plus-ui-namewta/main` result SHA 和 Lead Evidence。
- T-05 的前端 result 已由父仓库以 submodule pointer 记录；父仓库文档 commit 不夹带现有无关归档或后端指针改动。
- Spec 合同全部 covered，无 deferred、未批准偏差、未集成 source checkpoint 或活动 candidate。
- 架构、lint、typecheck、workspace test、开发/生产构建与完整 Playwright 通过；命令、cwd、退出码和未运行项均记录。
- source worktree/branch 的清理仅在单独授权后执行；本 change 的 T-01 至 T-08 均已获得清理授权并记录为 `removed`。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-00 Authorization | Goal Plan 已发布；前端 main/HEAD 重读 | implementation commit 与 local candidate integration 均为 authorized；基线 architecture/test 绿色 | 全部实现 | Lead / 用户 | 保持 blocked，不创建 worktree |
| G-10 Behavior Freeze | G-00 关闭，T-01 source 就绪 | T-01 candidate 定向 E2E、静态检查、result SHA 和 Evidence | Wave 1 | Lead | 父分支不动，返回 T-01 worktree 修正 |
| G-20 Shared Contracts | T-02、T-03 source checks 通过 | 按 2→3 集成的两个 candidate/result；公共 exports、依赖方向与包测试通过 | T-04 | Lead | 失败 Ticket 返回原 worktree；另一 Ticket 的已通过 result 不回退 |
| G-30 Admin Vertical | G-20 关闭，T-04 基于最新 result | Admin 定向测试、架构/类型检查、required E2E、legacy consumer 扫描、result SHA | T-05 | Lead | 父分支不动，修正 T-04；不得恢复双路径 |
| G-40 Final Contract | G-30 关闭，T-05 source 就绪 | legacy 零匹配、架构反向验证、全部根 Gate、完整 Playwright、前端 result、父文档/pointer commit | change completion | Lead / 用户授权边界 | 不推进对应父分支，保留最后可信 result 与失败 Evidence |
| G-50 Repair Authorization | CR-001 已接受并形成 T-06 至 T-08 | 新的 implementation commit 与 local candidate integration 授权；最新 main/dirty/base 预检 | T-06/T-07 | Lead / 用户 | 保持 blocked，不创建实现 worktree |
| G-55 Repair Convergence | T-06/T-07 source checks 通过 | 两个 candidate/result、生命周期失败关闭、递归 parser、显式 adapter、定向 E2E | T-08 | Lead | 父分支不推进失败 candidate，返回对应 source |
| G-60 Review Closure | T-08 result 和完整 Evidence 就绪 | 连续并发 E2E、全量根 Gate、`CR-002=approved`、状态/Evidence/Map 一致 | completion/archive | Lead / 独立 review | 任一 finding 未闭合则新 Ticket 或 blocked，不归档 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..004 Web 权限宿主与跨端纯度 | T-02, T-04, T-06 | Vue lifecycle、architecture、Admin composition | T-02/T-04/T-06 | passed |
| AC-005..012 导航、manifest 与失败关闭 | T-01, T-03, T-04, T-06, T-07, T-08 | lifecycle、递归 parser、Store/Router、定向 Playwright | T-01/T-03/T-04/T-06/T-07/T-08 | passed |
| AC-013 零兼容收缩 | T-05 | 精确扫描、架构反向验证 | T-05 | passed |
| AC-014 分阶段绿色 | T-01..T-08 | Gate Evidence 和 result SHA | 全部 | passed |
| AC-015 全量质量门禁 | T-05, T-08 | 根命令、连续并发与标准完整 Playwright | T-05/T-08 | passed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:leadership-epoch-1` | `.status.json` leadership；唯一 SpecDev、Evidence、E2E、candidate 与父分支 owner |
| Implementation subagents | 上限 3，Lead 不计入；当前计划并行峰值 2 | config=3、平台剩余并发能力、Wave 4 仅两个无写冲突 Ticket |
| Integration attempts | 每 Ticket 最多 7 | config 快照 `max_integration_attempts=7` |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写项目或状态 |
| Dispatch | execution-time dynamic | provider、模型、Lead 自行实现或派单在 Ticket preflight 后决定 |

任何派单都必须使用 operation=dispatch 的不可变 Packet，包含 Ticket、最新 `base_sha`、source worktree、路径 owner、允许动作、非 E2E 检查和停止条件。subagent 不写 Speculo、Evidence、父分支或 E2E 结论；Lead 按 operation=accept 独立核对。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | `plus-ui-namewta/main` 最新 SHA | `specdev-worktree/.../T-01` / `specdev/.../T-01` | lint/typecheck 与 E2E 文件静态检查 | source clean non-empty commit | Lead candidate：定向 Playwright | result 进入前端 main |
| T-02 | T-01 result | `specdev-worktree/.../T-02` / `specdev/.../T-02` | 包 lint/typecheck/test/build、architecture | source clean non-empty commit | Lead candidate：包/架构；E2E not-required | integration 序号 2 |
| T-03 | T-01 result | `specdev-worktree/.../T-03` / `specdev/.../T-03` | App Runtime lint/typecheck/test/build、architecture | source clean non-empty commit | Lead 在 T-02 后的最新 main 重建 candidate；E2E not-required | integration 序号 3 |
| T-04 | T-03 result（已含 T-02） | `specdev-worktree/.../T-04` / `specdev/.../T-04` | Admin 定向 test/typecheck/build、root architecture/lint/test | source clean non-empty commit | Lead candidate：T-01 + 受影响权限/领域 E2E | integration 序号 4 |
| T-05 | T-04 result | `specdev-worktree/.../T-05` / `specdev/.../T-05` | legacy scan、architecture 反向验证、全部非 E2E 根 Gate | 前端 source commit；父 docs 由 Lead 单独 commit | Lead candidate：build:dev/prod + 完整 Playwright；随后父 pointer 验证 | integration 序号 5 + parent result |
| T-06 | 获得 G-50 后的最新 main（规划 checkpoint 为 T-05 result） | `specdev-worktree/.../T-06` / `specdev/.../T-06` | Web Kit Vue lifecycle、package test/typecheck/lint、architecture | source clean non-empty commit | Lead candidate：受影响包与 Admin 组合；E2E not-required | integration 序号 6 |
| T-07 | 获得 G-50 后的最新 main（规划 checkpoint 为 T-05 result） | `specdev-worktree/.../T-07` / `specdev/.../T-07` | Domain/App Runtime/Admin tests、typecheck/lint、architecture | source clean non-empty commit | Lead 在 T-06 后最新 main 重建 candidate；required app-runtime E2E | integration 序号 7 |
| T-08 | T-07 result（已含 T-06） | `specdev-worktree/.../T-08` / `specdev/.../T-08` | 两个 E2E spec 静态/lint 检查 | source clean non-empty commit | Lead candidate：重复并发 Playwright、完整根 Gate | integration 序号 8 + CR-002 input |

`ticket_workspace_policy: required`：每个 Ticket 使用独立 source worktree；source worktree 不运行 E2E。Lead 在最新前端父分支创建 `specdev-worktree/.integration/2026-08-28-plus-ui-shared-navigation-permission-runtime/T-NN` candidate，验证通过且父 HEAD 未漂移后才推进 `plus-ui-namewta/main`。成功集成不自动删除 source branch/worktree。

T-05 的父仓库文档与 submodule pointer 由 Lead 在当前聚合 workspace 处理：先保存/重读现有无关 dirty paths，只暂存本 change 授权文档与 pointer；任何重叠或无法隔离的并行修改都会暂停父仓库 commit，不回退用户内容。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | not-authorized | 前端实现必须使用 required worktree |
| Ticket worktree local changes | authorized | 本次用户明确要求执行完 T-06 至 T-08 |
| Implementation commit | authorized | 覆盖 T-06 至 T-08 的非空 source commit |
| Local direct-parent verification and parent update | not-authorized | 不适用：当前计划选择 required/candidate-merge |
| Local candidate integration and parent update | authorized | 覆盖 T-06 至 T-08 Lead-owned candidate 与前端 main 更新 |
| Push / PR / remote merge | not-authorized | 本计划不请求远程写入 |
| Branch/worktree cleanup | authorized / completed | 用户已明确授权全面清理；T-01 至 T-08 source/candidate worktree 与对应本地分支均已移除 |
| Deploy / migration / production actions | not-authorized | 本变更不需要生产动作 |

### Evidence Return

implementation owner 只返回 source commit、dirty 状态、实际路径、非 E2E 命令/结果、未运行项和阻塞。Lead 重读 diff/commit，执行候选集成、required E2E、双轴审查和父分支核对后，唯一写入 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-NN.md</Path>` 与状态。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 后端 Client 裁剪与权限权威不变；前端不补偿或扩大菜单。
- `getInfo -> getRouters -> addRoute -> replace` 顺序不变，失败关闭。
- Vue 权限宿主在 Web Kit；Platform Permission 与 App Runtime 不引入 Vue/DOM/App 单例。
- navigation Store 归 Admin；动态页面严格 manifest-only。
- 零兼容：无 alias、转发、功能开关、dual path 或旧 Store facade。
- 每阶段 Gate 绿色后才开始下一阶段；T-02/T-03 的并行不绕过 G-20 汇合。

### Verification Integrity

规划基线已在 `plus-ui-namewta/main@d2961dbb444b9f036ad84c26ee8bcd69d973955c` 实测：architecture check 通过、architecture tests 98/98、App Runtime 18/18、Permission 13/13。实现不得删测试、降低断言、放宽 tsconfig/架构规则或把 skipped/not-run 写成 passed。

source-worktree 只运行非 E2E；parent-candidate 才是组合与 E2E 判卷接缝。T-05 架构规则必须做受控反向验证并恢复后重跑。每条命令记录 cwd、checkpoint、退出码和测试数。

### Migration or Release Sequence

顺序固定为行为冻结 -> 两个共享合同 -> Admin 消费者迁移 -> legacy contract -> 全量验证 -> 前端 main -> 父仓库文档与 pointer。没有兼容窗口、阶段部署或运行时观察期；Git checkpoint 是唯一恢复机制。

### Risks, Monitoring and Recovery

- **Manifest 覆盖缺口：** T-01 先把 fixture 切到正式 manifest；T-04/T-05 用未知键测试和零匹配扫描监控。
- **指令语义漂移：** T-02 特征测试覆盖非法输入、超管和通配符；T-04 E2E 复核真实页面。
- **Store 改名遗漏：** T-04 typecheck、调用点扫描和布局 E2E；命中旧调用即不关闭 G-30。
- **并行基线漂移：** T-03 candidate 必须基于 T-02 后的最新 main 重建；父 HEAD 变化标记 stale 并重跑。
- **父仓库 dirty 冲突：** Lead 只暂存授权路径；重叠即暂停，不清理或覆盖其他变更。
- **失败恢复：** candidate 失败时父分支不动，保留 source worktree 和 Evidence；在最多 7 次内修正，超限或需新决定则 blocked。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。路径越界、公共合同变化、验证接缝失效或新增安全风险立即暂停受影响 Wave；local 偏差记 Evidence，ticket/spec/architecture/release 偏差返回对应权威工件和批准人。

## 6. Progress and Decisions

### Current Status

- 上游 Spec 保持 ready；Tickets Map 的 8 个 Ticket 全部 done，T-06 至 T-08 的整改 Evidence 与 result 已固定。
- workspace 策略已固定为 `required + candidate-merge`，前端 parent 为 `main`。
- 最终前端父结果为 `efb8e0d7fae86cfd09c1f55204e8b486a499a3cc`，父仓库 pointer/docs result 为 `010b6c5a7044fa6545742a619d2cba6079b341c8`。
- G-00、G-10、G-20、G-30 与 G-40 是历史关闭记录；`CR-001=request-changes` 已重新打开 change。G-50 已由本次用户授权关闭，T-06/T-07 从最新前端 `main@8aa184b353c5a37ee555feb8be808fe9ba885297` 开始执行。

### Pending Decisions and Blockers

- 用户已先授权并完成 T-06 至 T-08 实现与本地 candidate integration，随后明确授权并完成全部已完成 worktree/branch cleanup；push、远端与生产动作仍未授权。
- `CR-001` 的三个整改边界已完成并由 `CR-002=approved` 关闭，change 已恢复 completed；归档仍需独立 archive 流程与明确归档确认。

### Resume Protocol

恢复时先读取 Goal Plan、Tickets Map、CR-001、CR-002、T-06 至 T-08 Evidence、`.status.json` 与最新 Git 状态。实现、candidate integration 与 source cleanup 已完成；不要重复执行。push、远端、生产与归档动作仍需各自授权。

## Assumptions

- 用户此前“前端子仓库使用 worktree、父 docs 无需 worktree”的偏好适用于本 Goal Plan；通过 `required` 前端 Ticket worktree与 Lead-owned parent current workspace 实现。
- implementation owner/provider 在每个 Ticket preflight 时动态选择，不影响已锁定路径、Gate 和 E2E owner。
- 父仓库当前存在其他归档与后端 pointer 修改；这些路径不是本 change 产出，Lead 在 T-05 只隔离授权路径并在提交前重读。
