---
schema_version: 6
artifact: goal-plan
change: 2026-08-25-plus-ui-multi-app-domain-architecture
status: completed
modes: [migration, high-assurance, reference-conformance]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: 7
ticket_workspace_policy: required
integration_gate: candidate-merge
ready_for_execution: false
---

# Goal Plan: plus-ui 多 App 领域架构改造

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

把当前单根 Vue 管理端迁移为以 `apps/`、跨终端 `domains/`、Web 专用 `web-domains/`、`platform/`、`adapters/` 和 `web-kit/` 组成的 pnpm 编译期模块化单体。`admin-web` 与 `client-web` 使用独立 ClientContext 和壳层，通过 App Composition Manifest 显式选择能力；认证、权限、动态路由和业务数据模型复用且不发生跨 Client 泄漏；未来 mobile-web/miniapp-taro 只保留真实边界占位。

项目实现的主 Git 边界是干净的前端子仓库 `<Path>plus-ui-namewta/</Path>`，其初始父分支为 `main@0bf978670e7915490fc85208946e0d022dedf55e`。每张涉及前端代码的 Ticket 使用该子仓库独立 source worktree，Lead 在前端最新 `main` 上串行构建 candidate、运行集成检查/E2E 并推进前端父分支。docs 聚合父仓库不创建 worktree；Lead 只在当前 workspace 写 SpecDev 工件，并在 `T-17` 对根 `<Path>docs/upstream/**</Path>` 形成显式路径的独立 direct-parent commit。

### Success and False Completion

成功要求 17 张非 cancelled Ticket 均有真实 implementation/source commit、通过的 integration result、Lead Evidence 和 Gate 关闭证据；30 个 AC 全部通过或保持已批准 deferred；双 App 构建、认证/路由 E2E、架构 Ratchet、旧调用点清零、OpenAPI 漂移检查和上游能力映射闭合。

以下均不算完成：只移动目录；只让 TypeScript 编译；在 source worktree 声称 E2E 通过；通过 deep import、全局 ignore、默认 Client、删除测试或保留未记录双入口制造绿色；未合入前端 `main` 的 source commit；只更新 docs 父仓库 submodule 指针而无前端 result；把移动端占位当成终端实现。

### Non-goals

- 不实现真实 mobile-web、Taro 小程序或原生 App，不选择其最终 UI 技术。
- 不引入 Nx、Turbo、微前端、运行时远程模块发现或公共 npm 发布。
- 不修改后端 API、菜单 component key、数据库或部署拓扑。
- 不自动 push、建 PR、远程 merge、部署、发布、归档或清理 source worktree/branch。
- 不把上游目录结构重新设为本地架构权威。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | worktree、commit、集成授权与 docs/前端仓库边界 | 更新 Goal Plan 与 change 授权状态 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/CONTEXT.md</Path>` | 模块化单体、Domain/Web Domain、manifest、Client、渐进迁移 | 架构冲突返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业 Client/安全等永久决定 | 当前 change 替代时须在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>` | 外部行为、范围、30 个 AC 和迁移 Gate | 下游不得改写；行为偏差返回 Spec |
| 5 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/</Path>` | 单 Ticket IN/OUT、路径、契约和验证 | Goal Plan 只编排；路径变化走 ticket deviation |
| 6 | 当前代码、Git 与命令事实 | 可行性、基线、真实接口与工具链 | 冲突时暂停受影响 Wave 并登记 deviation |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 -> T-02 -> T-03 -> T-04 -> T-05 -> T-06 -> T-07 -> T-08 -> T-09
                                                                    |
                          +-----------------------------------------+------------------+
                          |                                         |                  |
                          v                                         v                  v
                        T-10 -> T-11 -> T-13                       T-12               T-14
                          |          |                               |                  |
                          +----------+-------------------------------+------------------+
                                                                    v
                                                                  T-15 -> T-16 -> T-17
```

关键路径是 `T-01 -> T-02 -> T-03 -> T-04 -> T-05 -> T-06 -> T-07 -> T-08 -> T-09 -> T-10 -> T-11 -> T-13 -> T-15 -> T-16 -> T-17`。`T-09` 是扇出点，`T-15` 是 contract 汇合点。`T-12` 与 `T-14` 可以与 system 链并行实现，但必须在 `T-15` 前集成。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W01 | T-01 | 前端 clean baseline | tests/docs baseline | 无 | Gate A / 01 |
| W02 | T-02 | Gate A | placeholder README | 无 | Gate B / 02 |
| W03 | T-03 | Gate B | root workspace/config/tooling | T-03：package/workspace/lock/TS/Vite/Oxlint | Gate C / 03 |
| W04 | T-04 | Gate C | platform/adapters/compat request + root workspace refs/generated lock entries | T-04：request/auth facade；DEV-T04-001：T-03 保留根依赖/lock 策略 owner，T-04 仅写 facade 所需 `workspace:*` 与匹配 lock entries | Gate D / 04 |
| W05 | T-05 | Gate D | demo domain/web-domain + platform app-runtime registry + scoped root workspace refs/generated lock entries | T-05：首个 manifest registry 合同；DEV-T05-001：app-runtime activation 与精确 root/lock 机械变化 | Gate E / 05 |
| W06 | T-06 | Gate E | client-web/identity login/web-kit + generated lock importers | `DEV-T06-001`：T-03 保留 lock 策略 owner，T-06 只写自身 manifests 机械 importer | Gate F / 06 |
| W07 | T-07 | Gate F | identity/access/router/stores | T-07：permission/router/user stores | Gate G1 / 07 |
| W08A | T-08 | Gate G1 | workflow definition | 无 | Gate G2 / 08 |
| W08B | T-09 | Gate G2 | workflow runtime/system user seam | T-09：system user public seam | Gate G3 / 09 |
| W09A | T-10, T-12, T-14 | Gate G3；分别独立 source worktree | system identity；AI；operations | 各 Ticket 自有路径 | T-10 优先集成为 10；T-12/14 为 11/12 |
| W09B | T-11 | T-10 result；可与尚未完成的 T-12/T-14 并行 source 实现 | system resources/public dict-menu | T-11：system resource public contracts | Gate G5 / 下一可用序号 |
| W09C | T-13 | T-11 result | devtools | 无 | Gate G6 / 下一可用序号 |
| W10 | T-15 | T-11/T-12/T-13/T-14 results 且 Gate G6 | admin-web 与旧 src contract | T-15：`src/**` 收缩 | Gate H / 汇合后 |
| W11 | T-16 | Gate H | api-contracts/openapi/domain mappers | T-16：api-contracts | Gate I |
| W12 | T-17 | Gate I | docs upstream map + frontend README | 无；Lead 双仓库协调 | Gate J |

W09A 允许最多 3 个 implementation source worktree 同时活跃。candidate integration 永远由 Lead 串行：优先合入 T-10 以解锁 T-11；其他候选每次基于最新前端 `main` 重建。Wave 是可并发性，不授权共享写入或强制填满 agent 上限。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 迁移前行为基线 | — | plus-ui source worktree | Lead / dynamic dispatch | required：认证路由基线 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-01.md</Path>` |
| T-02 | 未激活占位合同 | T-01 | plus-ui source worktree | Lead / dynamic dispatch | not-required：纯 README | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-02.md</Path>` |
| T-03 | workspace 与 Ratchet | T-02 | plus-ui source worktree | Lead / dynamic dispatch | required：根配置影响运行 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-03.md</Path>` |
| T-04 | platform/browser adapters | T-03 | plus-ui source worktree | Lead / dynamic dispatch | required：request/session 跨边界 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-04.md</Path>` |
| T-05 | demo manifest 曳光弹 | T-04 | plus-ui source worktree | Lead / dynamic dispatch | required：菜单到页面 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-05.md</Path>` |
| T-06 | client-web 第二 App | T-05 | plus-ui source worktree | Lead / dynamic dispatch | required：双 Client 登录 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-06.md</Path>` |
| T-07 | 认证授权核心 | T-06 | plus-ui source worktree | Lead / dynamic dispatch | required：安全全链路 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-07.md</Path>` |
| T-08 | workflow definition | T-07 | plus-ui source worktree | Lead / dynamic dispatch | required：动态页面/API | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-08.md</Path>` |
| T-09 | workflow runtime/user seam | T-08 | plus-ui source worktree | Lead / dynamic dispatch | required：审批与用户选择 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-09.md</Path>` |
| T-10 | system 身份治理 | T-09 | plus-ui source worktree | Lead / dynamic dispatch | required：权限/Client | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-10.md</Path>` |
| T-11 | system 资源服务 | T-10 | plus-ui source worktree | Lead / dynamic dispatch | required：上传下载/社交 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-11.md</Path>` |
| T-12 | AI 领域 | T-09 | plus-ui source worktree | Lead / dynamic dispatch | required：路由/流式 UI | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-12.md</Path>` |
| T-13 | devtools 领域 | T-11 | plus-ui source worktree | Lead / dynamic dispatch | required：预览/下载 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-13.md</Path>` |
| T-14 | operations 领域 | T-09 | plus-ui source worktree | Lead / dynamic dispatch | required：权限/外部导航 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-14.md</Path>` |
| T-15 | admin-web contract | T-11/12/13/14 | plus-ui source worktree | Lead / dynamic dispatch | required：全入口/全路由 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-15.md</Path>` |
| T-16 | OpenAPI contracts | T-15 | plus-ui source worktree | Lead / dynamic dispatch | not-required：生成/type/API/build | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-16.md</Path>` |
| T-17 | upstream capability map | T-16 | plus-ui source worktree + docs current workspace | Lead / dynamic dispatch | not-required：维护文档 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-17.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- `AC-001` 至 `AC-019`、`AC-021` 至 `AC-030` 有通过 Evidence；`AC-020` 保持用户批准的 future-terminal deferred，未被伪装完成。
- 17 张非 cancelled Ticket 均在 `<Path>plus-ui-namewta/</Path>` 有 clean source commit、通过 Lead candidate、前端 `main` result SHA；`T-17` 另有 docs 当前 workspace 的 scoped commit/result。
- 根 workspace、依赖方向、exports、Client/auth/permission、manifest、七领域、双 App、兼容收缩、生成合同和上游映射全部闭合。
- `pnpm lint`、`pnpm typecheck`、`pnpm test`、受影响构建和 required Playwright E2E 无未经批准退化；生成/声明文件不手改，Oxfmt 不作为只读门禁。
- Tickets、Map、Goal Plan、Evidence、change status、前端 Git 与 docs Git checkpoint 一致；不存在未集成 source、活动 candidate、未决 release/architecture 偏差。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| A 基线可信 | T-01 source ready | 当前行为命令、分类偏差、candidate E2E/build、result SHA | T-02+ | Lead | 修正测试；不改生产行为 |
| B 占位合同 | Gate A | README 八项合同、无虚假 package、result SHA | T-03+ | Lead | 回退纯文档 candidate |
| C workspace 可运行 | Gate B | workspace/lock review、Ratchet 反向夹具、admin E2E/build | T-04+ | Lead | 父分支不动；修正规则/lock，禁止扩大 ignore |
| D 平台等价 | Gate C | request/session unit、Client/401 E2E、无头 graph | T-05+ | Lead | compat facade 回切旧实现 |
| E demo 曳光弹 | Gate D | manifest success/conflict/missing、有效 registry 页面、显式诊断 harness 与 demo keep-alive E2E/build；任意未知后端 key 留给 T-07 | T-06/T-07+ | Lead | 保留旧 resolver/入口 |
| F 第二 App 成立 | Gate E | 双 App build、多 Client/session E2E、未选领域证明 | T-07/T-15 | Lead | 禁用 client entry，admin 不动 |
| G1 安全等价 | Gate F | auth/router/permission matrix、401/Client E2E、alias Evidence | T-08+ | Lead；Deep 人工批准由用户授权覆盖执行，偏差另批 | facade 回切，保持 fail-close |
| G2/G3 workflow 合同 | G1；T-08 后 T-09 | definition/runtime E2E、system user seam、无 deep/cycle | T-09；W09 扇出 | Lead | 回切 workflow manifests/facades |
| G4-G6 领域迁移 | Gate G3 | system/AI/devtools/operations 各自 result；安全/下载/双 build/E2E | T-15 | Lead | 单领域 candidate 不推进；其他 result 保持 |
| H 兼容收缩 | G6 且所有消费者 result | 零旧 import、pre-contract checkpoint、双 App 全门禁与全 E2E | T-16+ | Lead；删除前显式批准点以本 Goal Plan 授权为边界 | 回退 pre-contract commit/artifact |
| I OpenAPI 稳定 | Gate H | immutable source/provenance、clean generate/check、drift 反向测试、type/API/build | T-17 | Lead | 保留 last-known-good，回退 domain mapper |
| J 长期治理 | Gate I | customization map 完整/缺字段反向 review、双仓库 result | change completion | Lead | 回退/修订文档，不触发远程同步 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| Workspace/exports/依赖方向 | T-02-T-04 | workspace list、architecture Ratchet、type/build | T-02-T-04 | passed |
| Manifest/component key | T-05、T-07-T-15 | registry unit、route integration、Playwright；T-07 用无物理 facade 的 typo key 关闭 AC-012 | 对应 Ticket | passed |
| Client/auth/session/permission | T-01、T-04、T-06、T-07、T-10 | unit、auth matrix、多 Client E2E | 对应 Ticket | passed |
| 七 capability domains 与表现定制 | T-05、T-08-T-14 | public exports、import graph、双 App build | 对应 Ticket | passed |
| Expand-migrate-contract | T-03-T-15 | facade、调用点扫描、pre-contract、全回归 | T-15 汇总 | passed |
| OpenAPI reference conformance | T-16 | 固定 source digest、generate/check、mapper tests | T-16 | passed |
| 上游选择性能力映射 | T-17 | customization map schema/review | T-17 | passed |
| 未来终端真实实现 | 后续独立 change | architecture + terminal build | 后续 change | deferred（用户已批准） |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence、E2E、双仓库父分支 owner |
| Implementation subagents | 最多 `3`，Lead 不计入 | config=3，平台可用实现 slot=3；W09A 才可能用满 |
| Integration attempts | 每 Ticket 当前硬上限 `7` | config 快照；第 3 次失败必须先登记 deviation 与根因复盘，超过 7 次则 blocked，不循环试错；T11 的 4->5、5->6 与 T13 的 6->7 均有用户明确授权 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation 只读且不得争用可变环境 |
| Dispatch | `execution-time dynamic` | provider、模型、是否派单按 Ticket 开始时的上下文与能力选择 |

以 `operation=plan` 固定 Subagent Delivery 合同：允许 `implementation | review | research | test-observation`；implementation 只写其前端 source worktree 和 Ticket writable/shared owner 路径，形成 source commit并只跑非 E2E；subagent 不写 Ticket/Map/Goal Plan/Evidence/status，不推进任何父分支。Lead 独立核对 diff、commit、dirty 状态和命令事实，拥有全部 E2E 与 candidate/result。外部网页 provider 未获源码发送授权，当前不得使用；执行期如需启用必须另取明确授权并遵守 ZIP-only 合同。

### Ticket Workspace and Integration

前端 `ticket_workspace_policy: required`：每个 Ticket 在执行开始时从最新已通过的 `<Path>plus-ui-namewta/</Path>` `main` result 建立逻辑 locator `specdev-worktree/2026-08-25-plus-ui-multi-app-domain-architecture/T-NN` 和唯一 source branch。source worktree 不运行 E2E；implementation owner 完成定向 unit/component、architecture、lint、typecheck、适用 build 后提交 clean source commit。

Lead 冻结 `parent_before_sha`，在 Lead-owned candidate 状态组合 source，运行受影响回归和 required E2E，重读前端父 HEAD；通过且未漂移后才推进前端 `main` 并写 `result_sha`。candidate 失败或 stale 时父分支不动。成功集成不自动删除 source branch/worktree，因为 cleanup 未授权。

docs 聚合仓库采用用户指定例外：不为 SpecDev 工件或 `T-17` 建 worktree。`T-17` 先完成并集成前端 README candidate，再由 Lead 在当前 docs workspace 仅暂存 `<Path>docs/upstream/customization-map.md</Path>`、`<Path>docs/upstream/README.md</Path>` 和必要的 submodule pointer，核对不包含既有用户脏改动后形成 scoped direct-parent commit。docs commit 失败不回滚已通过的前端 result，但 Gate J 保持未关闭并记录恢复条件。

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01-T-09 | 每张取上一 result | 唯一 plus-ui source worktree/branch | Ticket 定向测试 + lint/type/build（适用） | 必需、非空、clean | Lead 串行 candidate；除 T-02 外按表 required E2E | plus-ui `main` result SHA |
| T-10/T-12/T-14 | T-09 result，可并行 source | 三个不相交 plus-ui worktree | 各领域定向/安全/graph/type/build | 各自必需 | Lead 优先 T-10；每个在最新 parent 重建并 E2E | 三个串行 result SHA |
| T-11 | T-10 result | 独立 plus-ui worktree | system resource/security checks | 必需 | candidate + OSS/消息 E2E | plus-ui result SHA |
| T-13 | T-11 result | 独立 plus-ui worktree | devtools graph/type/download tests | 必需 | candidate + preview/download E2E | plus-ui result SHA |
| T-15 | T-11/12/13/14 latest results | 独立 plus-ui worktree | zero scan + full non-E2E + dual build | 必需，含 pre-contract checkpoint | candidate + full dual-App E2E | plus-ui contract result SHA |
| T-16 | T-15 result | 独立 plus-ui worktree | generate/check/type/API/dual build | 必需 | candidate；E2E not-required，复用 T-15 smoke 仅作观察 | plus-ui result SHA |
| T-17 | T-16 result；docs 当前 HEAD | plus-ui worktree + Lead docs current | Markdown/link/schema review | plus-ui source commit + docs scoped commit | plus-ui candidate；docs direct-parent review，无 E2E | plus-ui result + docs result SHA |

每张 Ticket 开始时才在 change `.status.json.worktrees` 建立 `planned/active` 记录，写入实际 base、branch、workspace locator、implementation/integration owner；不能用当前初始 SHA 预填未来 Ticket。W09 并行候选的 source base 可能早于最新 parent，Lead 必须在 candidate 集成时重建而非直接推进旧 branch。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | allowed（仅 docs T-17） | docs 不建 worktree；Lead-only、显式路径、不得带入既有脏改动 |
| Ticket worktree local changes | allowed | plus-ui required 模式；仅 Ticket writable/shared owner |
| Implementation commit | allowed | 用户已授权；plus-ui 每 Ticket 必需，T-17 docs 另有 scoped commit |
| Local direct-parent verification and parent update | allowed（仅 docs T-17） | Lead-only；仅 scoped docs result，不替代前端 candidate Gate |
| Local candidate integration and parent update | allowed | 用户已授权；仅 Lead 对 plus-ui 最新 `main` 执行 |
| Push / PR / remote merge | not-authorized | 本计划不继承远程写权限 |
| Branch/worktree cleanup | not-authorized | 集成成功后保留来源，另行授权清理 |
| Deploy / migration / production actions | not-authorized | 本 change 只做本地代码/文档迁移与验证 |

### Evidence Return

implementation subagent 只返回 Ticket、workspace locator、base/source commit、dirty 状态、实际路径、非 E2E 命令/退出码、未运行项和恢复条件。Lead 重读 Git 和 diff、复核或重跑检查，在 candidate/current docs 环境运行集成/E2E，并写 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-NN.md</Path>`。自报截图、E2E 或外部结论在 Lead 验证前保持 `unverified`。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- Domain 不依赖 Vue Router、Element Plus、DOM、浏览器存储、App 或 concrete adapter；跨包只从 exports 导入且依赖图无环。
- 后端按 Client 裁剪菜单是权威；前端只解析 component key 并 `addRoute`，不得二次跨 Client 过滤。
- ClientContext 缺失、字段缺失或 Boolean 类型错误在认证请求前 fail-close；不得回退默认 Client。
- 动态恢复保持 `getInfo -> getRouters -> addRoute -> replace`；401 保持单例提示、logout 和 redirect；`superadmin`/`admin` alias 只能以运行矩阵决定。
- manifest duplicate/missing key 稳定失败且含 App/domain/key 诊断；未选领域不进入 runtime。
- workspace root、request/auth、router/store、system public seam、旧 src contract 和 api-contracts 只由指定 Ticket owner 修改。
- 不手改生成声明或 OpenAPI 生成物；不通过删除测试、降低断言、扩大 ignore、关闭 Oxlint/typecheck 或移动验证位置制造绿色。
- 前端子仓库独立形成 source/candidate/result；docs 父仓库不创建 worktree，不混入现有用户修改。

### Verification Integrity

初始基线由 T-01 在前端 `main@0bf9786` 实测，不能把计划阶段仅确认“命令存在”写成通过。source-worktree 运行非 E2E；Lead parent-candidate 运行受影响集成与 required Playwright。`pnpm lint` 是只读 Oxlint，`pnpm typecheck` 是 `vue-tsc --noEmit`，`pnpm test` 是 Vitest，`pnpm build:prod` 是生产构建；`pnpm fmt` 会写文件，不作为 Gate。T-03、T-05、T-15、T-16 对容易静默通过的 Ratchet/registry/zero-scan/drift Gate执行受控反向验证。

### Migration or Release Sequence

执行固定为 expand -> migrate -> observe -> contract -> verify：T-03/T-04 建立 workspace 与 compat facade；T-05-T-14 按领域迁移且每个 candidate 保持 admin 可运行；T-15 在零调用点和 pre-contract checkpoint 后删除旧 src；T-16 后置生成；T-17 固化维护。没有部署/生产 migration；所谓发布 Gate 只指本地父分支推进。移动/小程序继续占位，另立 change 才能激活。

### Risks, Monitoring and Recovery

| 风险 | 信号 | 阻断/恢复 |
|---|---|---|
| 根 workspace/lockfile 破坏构建 | workspace 包数、lock diff、admin build | Gate C 不推进；回退 T-03 candidate |
| Client/auth 权限漂移 | Client mismatch、401、auth matrix、跨 App session | Gate D/F/G1 不推进；facade 回切且 fail-close |
| component key 漂移 | duplicate/missing registry diagnostics、404/E2E | 阻断领域/contract；恢复旧 resolver |
| 跨域循环/deep import | architecture Ratchet 与 graph | 暂停消费者，修正 public contract owner |
| W09 并行基线陈旧 | parent HEAD 与 source base 不同 | candidate 重建；第 3 次失败先暂停复盘，当前硬上限 6 次后 blocked |
| src 提前删除 | old import count、双 build/E2E | 不执行 contract；回到 pre-contract result |
| OpenAPI 来源/输出漂移 | source digest、clean regenerate、diff | 保留 last-known-good；不伪造 schema |
| docs 脏工作树混入提交 | staged path/diff 与 status | 取消 docs commit，重新显式暂存；不触碰用户改动 |

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。局部实现细节写 Evidence；可写路径、Ticket 契约或验证变化暂停 Ticket 并修订；外部行为返回 Spec；ADR/依赖方向返回 Grill；收缩、workspace 策略、授权或恢复变化属于 release deviation，必须重新取得用户批准。任何 shared owner 变化先暂停其消费者和 W09 candidate 队列。

- `DEV-T04-001`（ticket，Lead 于 2026-08-26 批准）：T-04 激活真实 package 且根 compat facade 从 public exports 导入后，T-03 的依赖声明与 lock Gate 要求根 manifest 存在 `workspace:*`，并要求同步 lock specifier/importers。T-03 保留 workspace/catalog/root-dependency/lock 策略所有权；T-04 只可向根 manifest 添加 facade 实际消费的 T-04 包声明，并写匹配的根 lock specifier 与本 Ticket manifests 机械生成的 importers，禁止修改根 scripts、catalog、外部版本、无关依赖和既有 resolution。Lead 必须在 candidate 比对结构化 manifest/lock diff、运行 frozen install 与 architecture tests；不满足则 Gate D 不推进。
- `DEV-T05-001`（ticket，Lead 于 2026-08-26 批准）：`platform/app-runtime` 占位合同明确要求随 demo pilot 激活，且 demo domain/web-domain 与 root compatibility facades 激活后会新增内部依赖和 lock importers。T-05 获得 `<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>` 的实现所有权，并只可向根 manifest 添加 root compatibility facade 实际消费的新包 `workspace:*` 声明、匹配 root lock specifier 与本 Ticket manifests 机械生成的 importers。T-03 保留 workspace/catalog/root-dependency/lock 策略所有权；根 scripts、catalog、外部版本、无关依赖、workspace 配置、既有 resolution、全局 permission/router/store 继续只读。Lead candidate 必须执行 registry 反向夹具、structured manifest/lock diff、frozen install、architecture tests、admin build 与 required demo route E2E。
- `DEV-T05-002`（ticket compatibility，Lead 于 2026-08-26 批准）：工程 Target 要求 CRUD 变更使用 `POST`，但本 change 的外部范围明确不修改后端 API，且现有 demo update/delete 合同分别为 `PUT/DELETE`。T-05 只可原样迁移这些已存在的方法与路径，禁止借前端架构重构单边改变 transport 或新增非 POST 变更端点。Evidence 必须记录该兼容差异；例外在后端完成协调方法迁移并同步消费者后到期。
- `DEV-T05-003`（ticket ownership，Lead 于 2026-08-26 批准）：代码事实表明 T-05 的 demo 专用 facade 只能证明已知 key 经 registry 加载，而任意未知后端 key 会先被 T-07 独占的旧全局 views glob 返回 `undefined`。T-05 Gate E 因此只验 registry missing/error、有效 demo facade 的 `compose -> resolve -> load`、显式诊断 harness 的可见反馈与 keep-alive；其 E2E 不得声称预置 sentinel 等价于通用未知 key。AC-012 的最终外部行为不变，由 T-07 在改造全局 permission/router 时使用无对应物理 facade 的 typo key 完成 fail-visible E2E，并在 Gate G1 前关闭。全局 permission/router/store 在 T-05 继续只读。
- `DEV-T06-001`（ticket ownership，Lead 于 2026-08-26 批准）：pnpm workspace 已通过 `apps/*` 与 `packages/*/*` 覆盖 T-06 目录，但任何新激活 package manifest 都必须在 frozen lock 中有 importer。T-06 可写 `<Path>plus-ui-namewta/pnpm-lock.yaml</Path>` 中仅由其 apps/identity/web-kit manifests 机械生成的新增 importer/specifier；T-03 保留 root manifest、workspace、catalog、外部版本、既有 importer/resolution 与 lock 策略所有权。Lead candidate 必须执行 structured lock diff、frozen install、workspace list、architecture tests、双 App build 与 required E2E。
- `DEV-T07-001`（ticket ownership，Lead 于 2026-08-26 批准）：激活 `platform/permission` 并把根 auth/menu/store/view 改为公共包 facade 会要求根声明实际消费的内部包，并要求 lock 同步 root specifier、新 importer 与 T-07 manifests 的依赖变化。T-07 只可向根 manifest 添加实际 facade 消费的 identity-access、web-domain-identity-access、platform-permission `workspace:*` 声明，并写精确匹配的 root lock specifier、platform-permission importer 与 T-07 writable manifests 机械 importer 更新；T-03 保留 root dependency/workspace/catalog/lock 策略所有权，root scripts、外部版本、既有 resolution 与无关 lock 节点继续只读。Lead candidate 必须执行 structured manifest/lock diff、frozen install、workspace list、architecture tests、双 App build 与 required auth/router E2E。
- `DEV-T07-002`（ticket test ownership，Lead 于 2026-08-26 批准）：T-01 的 request characterization 固定了迁移前 `login()` 立即请求 `/auth/login` 的行为，但 T-07/AC-015 明确要求严格 ClientContext 前置，二者不能同时成立。T-07 获得 `<Path>plus-ui-namewta/src/utils/request.test.ts</Path>` 的精确测试写权限，只可把 login 基线更新为 `context -> code -> login` 并补齐兼容 facade 需要的 session mock；生产 request adapter 继续只读，既有 header、401 单例、下载与错误安全断言不得删除或放宽。
- `DEV-T08-001`（ticket composition/lock ownership，Lead 于 2026-08-26 批准）：T-08 要证明 admin 显式选择 workflow definition manifest，但唯一 admin composition 点由 T-07 所有，且两个 workflow package 激活需要 T-03 策略所有的 root manifest/lock 更新。T-08 只可在 `<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>` 追加 workflow domain/manifest inputs 与 selected IDs，并添加两个实际消费的 workflow `workspace:*` 根依赖、匹配 root lock specifiers 和新 package importers；resolver/route assembler、identity/demo 选择、root scripts、workspace/catalog、外部版本、既有 resolution 与无关 lock 节点继续只读。Lead candidate 必须审计 composition/manifest/lock structured diff，并跑 selected/unselected tests、frozen install、workspace/architecture、双 build 与 required definition E2E。
- `DEV-T08-002`（ticket compatibility，Lead 于 2026-08-26 批准）：工程 Target 要求 CRUD mutation 使用 POST，但本 change 不修改后端，且现有 workflow category/definition/SpEL controller 与前端共同使用 PUT/DELETE mutation。T-08 必须原样迁移这些路径和方法，不得发明平行端点或单边改变 transport；例外仅在后端协调迁移并同步全部消费者后到期。Domain transport tests 必须锁定所有迁移方法，candidate E2E 至少覆盖代表性 mutation，Evidence 记录到期条件。
- `DEV-T08-003`（ticket test ownership，Lead 于 2026-08-26 批准）：为证明 DEV-T08-001 的 admin composition 选择边界，T-08 可在既有 `<Path>plus-ui-namewta/src/router/adminManifestRegistry.test.ts</Path>` 仅追加 workflow definition keys 已选中及 T-09 runtime key 未选中的断言；既有 identity/demo 断言、测试基础设施、生产 resolver 语义和其他 router tests 保持只读。Lead 必须审查该 test diff 仅含授权断言并跑全 root unit。
- `DEV-T08-004`（ticket execution，Lead 于 2026-08-26 登记）：T-08 candidate integration 实际执行 4 次，超过默认上限 3。前三次依次暴露隐藏 native checkbox 定位、SpEL mutation 完成竞态和两个 live dict fixture 缺失，均未推进 parent；每次均回到 source-only 追加修复、双审并从未漂移 parent 重建。第 4 次 full Gate 与 Playwright 18/18 通过后才推进。该偏差不降低验收合同；后续 Ticket 达到第 3 次失败时必须先停止、登记偏差并完成根因复盘，不能直接运行第 4 次。
- `DEV-T09-001`（ticket package/lock ownership，Lead 于 2026-08-26 批准）：T-09 要求 workflow 只从 `system-admin/public/user` 消费最小公开合同，但 system-admin 仍是 README-only 占位，不能通过合法 package export 被跨 workspace 使用。T-09 可激活 `@namewta/domain-system-admin` 的 `README.md`、`package.json`、`tsconfig.json` 与 `public/user/**`，在 workflow package manifest 添加该包的 `workspace:*` 依赖，并机械更新 lock 中新增 system-admin importer 和 workflow dependency specifier。T-03 保留 lock/workspace/catalog 策略所有权，T-10 保留其余 system-admin 实现所有权；根 package/scripts、admin registry、外部版本、既有 resolution 与无关 lock 节点继续只读。Lead candidate 必须核对结构化 manifest/lock diff、frozen install、workspace/architecture、无 deep/cycle、双 build 与 required runtime E2E。
- `DEV-T09-002`（ticket composition/parity ownership，Lead 于 2026-08-26 批准）：双轴固定点审查确认 T-09 当前以附件 ID 文本框替代真实上传、缺少退回附件、实例变量更新、请假 timestamp range/自动天数、办理完成关闭页和撤销原因，并保留了平行的旧 Process 实现；候选人查询被错误序列化为数组，管理员干预又错误沿用了办理人 `buttonList`。T-09 可在 `<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>` 仅把现有根 FileUpload 与 close-current-page 能力注入 `WorkflowWebRuntime`，并公开同一 runtime 供 `<Path>plus-ui-namewta/src/components/Process/**</Path>` 兼容 facade 使用；根上传实现、resolver/route assembler、manifest 选择、root manifest/lock、外部版本和其他宿主能力保持只读。source 必须恢复上述行为，以逗号字符串提交候选人查询，并为 admin intervention 建立独立 action policy；E2E 必须含 forbidden-user/URL 和隐藏 participant button 的反向夹具，README 只能陈述实际覆盖。
- `DEV-T09-003`（ticket composition/facade ownership，Lead 于 2026-08-26 批准）：Phase A 收敛 `submitVerify/processMeddle` 后，其余 approval/history/chart Process 组件仍需访问宿主 Token、OSS 元数据与下载能力；直接从 web-domain deep import 根实现会违反 AC-006/007。T-09 可在同一 `<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>` 仅追加 `chartUrl`、`resolveAttachments` 与 `downloadAttachment` 端口，并把其余六个组件迁入 web-domain public exports，使全部 `<Path>plus-ui-namewta/src/components/Process/**</Path>` 成为兼容 facade。根 auth/OSS/download 实现、manifest/resolver 语义和其他 runtime 行为只读；architecture/no-deep、旧 props/events/exposes 与双 build 必须通过。
- `DEV-T09-004`（ticket execution，Lead 于 2026-08-26 登记）：T-09 已达到第 3 次 candidate 失败检查点，三次均未推进 `plus-ui-namewta main@262c06d38be0d123e4ec07d8905fc13f87a8fcab`，transient candidate 均已移除。第 1 次 full non-browser Gate 通过但 full E2E `18/29`，暴露 client-web `4174` 缺失与 workflow 定位问题；第 2 次在正确双 App 环境中 workflow `6/11`，暴露 locator/fixture 缺口；第 3 次 workflow `8/11`，最终定位到候选过滤的 Vue props 同 tick 竞态、错误消息重复定位和不变请求即时重试触发 repeat-submit。仅当 source 以 append-only commit 修复、双轴 fixed-point 复审通过后，才允许第 4 次 candidate；其必须依次通过 workflow `11/11`、full dual-App Playwright `29/29` 和完整 Gate 后才可推进。
- `DEV-T10-001`（ticket composition/manifest/lock ownership，Lead 于 2026-08-26 批准）：T-10 激活 system-admin 完整 domain/web-domain 后，frozen lock 需要两个 package importers，根兼容 facade 与唯一 admin composition 点需要匹配 `workspace:*` 依赖和 selected-only wiring。T-10 只可添加根实际消费的两个 system-admin workspace 依赖、对应 root lock specifiers 与两个 manifests 机械 importer；`src/router/adminManifestRegistry.ts` 只可追加 system domain/manifest、selected IDs 和七个切片所需的最小 typed host ports，registry test 只追加 selected/unselected 断言。既有 identity/demo/workflow composition、resolver、全局 permission、workspace/catalog、root scripts、外部版本、既有 resolution 与无关 lock 节点继续只读；Lead 必须核对 structured diff、T-09 `public/user/**` 字节不变、workflow 回归、双 build 与 required E2E。
- `DEV-T10-002`（ticket compatibility，Lead 于 2026-08-26 批准）：工程 CRUD Target 偏好 POST mutation，但本 change 明确不修改后端，七组现有 system controller/frontend 合同包含 PUT/DELETE。T-10 必须原样迁移全部既有方法与路径，由 domain transport tests 锁定，禁止发明平行 endpoint 或前端单边规范化；例外只在后端协调迁移并同步全部消费者后到期，Evidence 必须记录。
- `DEV-T11-001`（ticket host-port ownership，Lead 于 2026-08-26 批准）：T-11 的 manifest-loaded system resource 页面必须复用宿主现有 dict cache mutation、OSS download、HTML/content security 和 Editor/ImagePreview，不能 deep import 根实现或在包内复制安全代码。T-11 只可在 `src/router/adminManifestRegistry.ts` 为既有 system runtime 追加这些 typed ports，并在 registry test 将 T-10 的 system/oss 未选择夹具更新为资源页 selected 与无关 key unselected；manifest 选择、resolver、根安全/组件实现、全局 router/request/App、root manifest/lock、identity 和 devtools 继续只读。Lead 必须核对精确 diff，并跑既有 security/OSS/upload、完整 Gate 与 required E2E。
- `DEV-T11-002`（ticket execution，Lead 于 2026-08-26 登记）：attempt 1 建 candidate 时快进命令误在 parent checkout 执行；Lead 在任何验证前即把 parent detach、将 candidate 精确快进到 source，并用 expected-old-value guard 把 `main` 恢复为 `9ce426c20be49209d24b40083e14eede4ea31109`，随后重验 source/candidate/main clean，全部 commit 由 refs 保留。纠正后的 candidate 完整 non-browser Gate 通过，但 T11 targeted Playwright `2/4` 暴露无 Layout 的不真实菜单夹具与无 accessible title 的确认框 locator。attempt 1 不推进；T11 仅可在专用 E2E append-only 改为真实 Layout 父菜单/子路由，并以 dialog role + 可见解除文案定位确认框，不得放宽消息消费、上传下载、Client/social/permission/strict-unknown 断言。新 source 必须双轴 PASS，attempt 2 从未漂移 parent 重建并运行完整 Gate、T11 `4/4` 与 full dual-App E2E。
- `DEV-T11-003`（ticket execution，Lead 于 2026-08-26 登记）：T11 已达到第 3 次 candidate 失败检查点，三次均未推进 `main@9ce426c20be49209d24b40083e14eede4ea31109` 且 transient candidate 已移除。attempt 2 在完整 non-browser Gate 后 targeted `3/4`，暴露真实 OSS icon button 缺少 accessible name；生产最小修复双轴 PASS 后，attempt 3 同样 non-browser 全绿、targeted `3/4`，strict-unknown 最终揭示页面真实发出的 `/system/dict/data/list` 与上传后 `/resource/oss/listByIds/8` 缺少 fixture。仅允许在专用 E2E append 两个具体成功响应、Client request 记录与既有 strict-unknown 空终态，不得改生产、locator、权限、endpoint 或弱化断言。新 source 必须双轴 PASS；第 4 candidate 从未漂移 parent 重建，依次通过完整 Gate、T11 `4/4` 与 full dual-App Playwright 后方可推进。
- `DEV-T11-004`（ticket execution，用户于 2026-08-26 明确授权）：attempt 4 的完整 non-browser Gate 通过，但专用 E2E 在上传选择后用 page-wide exact text 同时匹配上传弹窗与背景列表中的同名链接，targeted 为 `3/4`，未推进 `main@9ce426c20be49209d24b40083e14eede4ea31109`。Speculo config 与本 Goal Plan 的 integration attempt 上限同步由 `4` 提高为 `5`。T11 只可把文件 input 与确认前文件名可见断言限定到真实“上传文件”dialog；确认后的列表、PUT transfer、complete、`listByIds/8`、Client/method/path 与 strict-unknown 强断言不得删除或放宽。新 source 必须双轴 PASS；第 5 candidate 必须从未漂移 parent 重建，并依次通过完整 non-browser Gate、T11 `4/4` 和 full dual-App Playwright 后才可推进。
- `DEV-T11-005`（ticket review，Lead 于 2026-08-26 批准）：`cfc09f4` 规范轴 PASS，但标准轴发现确认后 page-wide `.first()` 可在 Element Plus dialog 关闭动画期间命中旧上传链接，无法严格证明 `submitForm -> getList` 刷新。T11 可在同一专用 E2E 增强确认后证据：点击前记录 OSS list request 次数，点击后先等待 upload dialog hidden，再等待 list request 精确增加一次并在 OSS table row 内断言新文件。现有 transfer/complete/`listByIds/8`、Client/method/path、permission 与 strict-unknown 断言保持；生产代码零变化。新 fixed point 必须重新双轴 PASS 后才进入已授权的第 5 candidate。
- `DEV-T11-006`（ticket execution，用户于 2026-08-26 明确授权）：attempt 5 的完整 non-browser Gate 通过，但真实 upload dialog 内部有两个同名 link，确认前 dialog-scoped exact-text locator 仍触发 strict-mode，targeted 为 `3/4`；未进入 confirm，未推进 `main@9ce426c20be49209d24b40083e14eede4ea31109`，transient candidate 已移除。Speculo config 与本 Goal Plan 的集成上限同步由 `5` 提高为 `6`。T11 只可把确认前文件名断言收窄为 dialog 内有准确 accessible name 的 link，并显式选择单个元素；`DEV-T11-005` 的 dialog hidden、list request 精确 `+1`、OSS table row 以及 transfer/complete/`listByIds/8`、Client/method/path、permission、strict-unknown 强证据必须全部保留。新 source 必须双轴 PASS；第 6 candidate 从未漂移 parent 重建，并依次通过完整 non-browser Gate、T11 `4/4` 与 full dual-App Playwright 后才可推进。
- `DEV-T12-001`（ticket composition/manifest/lock ownership，Lead 于 2026-08-26 批准）：T-12 可添加两个 AI `workspace:*` 根依赖、匹配 root lock specifiers 与两个 package manifests 机械 importer；可在 admin registry 只追加 AI domain/manifest、selected IDs 和最小 request/trusted-credential/base-URL ports，registry test 只追加 selected/unselected 断言。既有 composition/resolver/router/request/App、platform/identity、scripts/catalog/外部版本/既有 resolutions 与无关 lock 节点只读；client-web 保持不选择。Lead 必须执行 structured diff、secret review、完整 Gate 与 required E2E。
- `DEV-T12-002`（ticket current-behavior interpretation，Lead 于 2026-08-26 批准）：固定基线只有 `POST /snail-ai/user/register` 与 `ai/chat/index` iframe，旧前端流式聊天已删除。T-12 不发明 Fetch/SSE/ReadableStream/prompt 协议，而把 planned stream lifecycle 落实为注册请求、显式 runtime credential/base URL、iframe load/error、credential URL 清理和重试；保留 endpoint/key/空 permission，任何测试或诊断不得暴露 token/credential。
- `DEV-T12-003`（ticket host-probe ownership，Lead 于 2026-08-26 批准）：双轴 fixed-point review 证明 iframe `error` 不能可靠表示导航失败。T-12 可为 `AiWebRuntime` 增加唯一 abortable same-origin GET `probeFrame` port，在 `src/router/adminManifestRegistry.ts` 只追加 `redirect: error`、成功状态与 HTML content-type 校验，并在 registry test 增加聚焦 adapter 断言；session 必须先验证 root-relative base path，probe 成功后才发布 credential-bearing iframe URL，timeout/dispose 清理，立即注册重试使用既有 `repeatSubmit: false`。该决定只替代 `DEV-T12-002` 的 iframe-error 信号，不授权新后端、postMessage、SSE、router/resolver/selection 或其他 registry 行为。Lead candidate 必须验证真实 probe failure、立即成功重试、终态 load、abort/timeout 清理与无 credential diagnostics。
- `DEV-T13-001`（ticket public-seam/composition/safe-download ownership，Lead 于 2026-08-26 批准）：T-13 只能在 T-11 result 后添加两个 devtools `workspace:*` 根依赖、匹配 root lock specifiers/two importers，并让 domain-devtools 通过 public entry 依赖 system-admin。因 codegen 真实需要 `/system/dict/type/optionselect`，可仅在 `packages/domains/system-admin/public/dict/**` 新增 projected dict-type catalog port，既有 `get(type)` 与 system DTO/实现保持不变；admin registry/test 只追加 devtools selected IDs、最小 typed request/modal/navigation/client/menu/dict/downloadZip ports 与 selected/unselected 断言。为满足失败不保存损坏 ZIP，可只硬化 `src/plugins/download.ts` 的 `zip` 与聚焦测试：GET ZIP 必须验证响应、拒绝 JSON/HTML/plain/truncated/network payload、finally 清理并仅呈现 sanitized error，禁止 raw console；通用 request/axios adapter、OSS download、resolver/router/App、client-web 与无关 lock/config 继续只读。Lead 必须核对 structured diff、no-deep/no-cycle、ZIP 反向 unit、完整 Gate 与 preview/valid-download/failure/client-exclusion E2E。
- `DEV-T13-002`（ticket execution，Lead 于 2026-08-27 登记）：T13 已达到第 3 次 candidate 失败检查点，三次均未形成父结果，`main` 保持 `8144f98bd8a005c1386e7de8462fd354776b5d34`。attempt 1 完整 non-browser Gate 通过但 targeted 为 `2/5`，暴露 preview close、tabpanel 与不存在的 display-value locator；其创建阶段还曾因 shell 工作目录错误短暂快进 clean parent，Lead 在验证前即以 expected-old guard 恢复 ref 和已核验 base 的 index/worktree，再于正确 candidate 重建。attempt 2 non-browser 全绿、targeted `4/5`，暴露 Element Plus 留存 hidden option；attempt 3 non-browser 全绿、targeted `4/5`，证明 `:visible` 仍会在 popper 过渡期绑定即将隐藏的旧节点。三个 transient candidate 均已移除。用户已授权全局集成上限 6；第 4 次只允许在专用 E2E 通过被点击 combobox 的 `aria-controls` 精确解析所属 listbox，禁止 page-wide/hidden/index/transition locator，生产代码及完整 root/params/columns payload、metadata Client IDs、ZIP zero-save、retry/race、client exclusion、strict-unknown 证据不得改变。新 fixed point 必须双轴 PASS；attempt 4 从未漂移 parent 重建并依次通过完整 non-browser Gate、targeted `5/5` 与 full dual-App Playwright 后才可推进。
- `DEV-T13-003`（ticket execution，Lead 于 2026-08-27 登记）：attempt 4 完整 non-browser Gate 通过但 targeted 仍为 `4/5`，唯一失败是 Element Plus placeholder 拦截内部 combobox input 的 click，尚未进入已审查的 `aria-controls` listbox 解析；candidate 已清理且 `main@8144f98bd8a005c1386e7de8462fd354776b5d34` 未推进。在已授权上限 6 内，仅允许把 E2E helper 的打开动作改为点击所属 test-id select 容器，同时继续从其子 combobox 读取 `aria-controls` 并保留精确 listbox、role/name option、Escape/hidden 与全部终态断言。新 fixed point 双轴 PASS 后，第 5 candidate 必须从未漂移 parent 重建并通过完整 non-browser Gate、targeted `5/5` 与 full dual-App Playwright 后才可推进。
- `DEV-T13-004`（ticket execution，Lead 于 2026-08-27 登记）：attempt 5 完整 non-browser Gate 与 `aria-controls` listbox 流程通过，但 targeted 仍为 `4/5`，随后真实树表 radio input 被 Element Plus 可视 wrapper 拦截；candidate 已清理，`main@8144f98bd8a005c1386e7de8462fd354776b5d34` 未推进。已授权上限 6 的最后一次只允许 E2E 点击包含具名 tree radio 的 `label.el-radio` 并追加 checked 终态；生产、listbox、完整 payload、metadata Client IDs、ZIP zero-save、retry/race、client exclusion 与 strict-unknown 证据不得变化。新 fixed point 双轴 PASS 后，第 6 candidate 必须从未漂移 parent 重建并通过完整 non-browser Gate、targeted `5/5` 与 full dual-App Playwright；任何失败都必须在上限处停止。
- `DEV-T13-005`（ticket execution，Lead 于 2026-08-27 登记）：attempt 6 在 source `aca2047310ec8a9b5205c72e4bd8585b7b8a5ac2` 双轴 PASS 后从未漂移 parent 重建，frozen install、27-workspace architecture `0 + 92/92`、lint/typecheck、root `48/232` 与全部 workspace unit、双 App production build 均通过；targeted Playwright 仍为 `4/5`。树表 radio wrapper 点击和 checked 终态已成功，随后 E2E 等待未暴露到 DOM 的 `data-testid="tree-root-value"` 超时，而失败快照明确显示 `textbox "根节点值"` 已渲染且值为 `0`，故根因是测试定位假设。达到当前上限 6 后已停止，未运行 full E2E、未推进 `main@8144f98bd8a005c1386e7de8462fd354776b5d34`，transient candidate 已移除，source 保留。恢复需用户明确提高上限；下一 source 只可改为 unique accessible textbox `根节点值` locator，并保留所有既有强断言、重新双轴审查及完整 candidate Gate。
- `DEV-T13-006`（ticket execution，用户于 2026-08-27 明确授权）：用户在收到“提高到 7 才能恢复”的阻塞说明后回复“完全授权给你”，因此 Speculo config 与本 Goal Plan 的集成上限同步由 `6` 提高为 `7`，`BLK-T13-001` 解除。第 7 次只允许把专用 E2E 的不存在 `tree-root-value` test-id lookup 改为唯一 accessible textbox `根节点值` locator；tree radio checked、`aria-controls` listbox、完整 payload、metadata Client IDs、ZIP zero-save、retry/race、client exclusion 与 strict-unknown 证据保持。新 source fixed point 必须双轴 PASS，第 7 candidate 从未漂移 parent 重建并通过完整 non-browser Gate、targeted `5/5` 与 full dual-App Playwright 后才可推进。
- `DEV-T14-001`（ticket composition/manifest/lock ownership，Lead 于 2026-08-26 批准）：T-14 可添加两个 operations `workspace:*` 根依赖、匹配 root lock specifiers 与两个 package manifests 机械 importer；可在 admin registry 只追加 operations domain/manifest、selected IDs 和最小 request/modal/download/permission/IFrame/三个公开 monitor URL host ports，registry test 只追加 selected/unselected 断言。URL 校验/导航 intent 仍属 operations；既有 composition/resolver/router/request/App、platform/identity、scripts/catalog/外部版本/既有 resolutions 与无关 lock 节点只读。Lead 必须执行 structured diff、安全反向测试、完整 Gate 与 required E2E。
- `DEV-T15-001`（ticket root-App contract ownership，Lead 于 2026-08-27 批准）：admin-web 激活需要独立 package/lock importer，根 `src` 删除后原 root scripts、Playwright preview、index/Vite/Uno/env/public 与 tsconfig alias/include 不能继续指向已退役 App。T15 可先复制现有根宿主入口到 admin-web 并形成通过独立 admin build/type/unit/architecture 的 pre-contract commit；之后才可删除根副本、把既有 App runtime dependencies/tooling 转移到 admin manifest、把 root package 收缩为 workspace 编排器、机械更新 lock importer、更新 Playwright preview，并只移除 shared tsconfig 的旧 root-src alias/include。packages、workspace/catalog、外部版本、architecture rules、client-web、后端合同和 E2E 强断言只读。
- `DEV-T15-002`（ticket shared-test-tooling ownership，Lead 于 2026-08-27 批准）：删除根 App `vite.config.ts` 后，现有 web-domain Vue SFC 单测暴露其长期依赖 Vitest 的隐式向上配置发现；packages/E2E 也依赖根 devDependencies 提供共享编译/测试工具，fresh frozen install 进一步证明 E2E 直接导入的 `crypto-js` 不能依赖旧 root runtime hoist。T15 可保留既有根 devDependencies、把现有 catalog `crypto-js` 明确列为 test-only devDependency、增加只注册 Vue 插件的最小根 `vitest.config.ts`，并把复制到 admin 的 `RoleSelect` 中 `VxeTableInstance` 类型改从已安装组件库真实声明入口导入。根 runtime dependencies、App build/preview 仍必须退役，packages、workspace/catalog、外部版本、architecture rules、client-web 和测试断言保持只读；完整 Playwright discovery、workspace typecheck/unit/build 验证该兼容修复。
- `DEV-T15-003`（ticket review remediation，Lead 于 2026-08-27 登记）：初轮规格轴判定 `dd14be9` 只完成 expand，而 `da8e21f` 同时迁移真实调用点和删除旧入口，未形成 AC-024 可审计批准点；标准轴同时发现 Playwright 未启动/等待 client 4174，且 root `build:dev` 静默走 production。`da8e21f` 不进入 candidate。T15 以 append-only commit 恢复 dormant 根文件/声明，在 root scripts、HTML、tsconfig、Playwright 均已切到 admin 后形成 `ad4e971` 并通过零活动消费者、frozen、architecture、lint/type/unit、dev/prod builds；下一独立 commit 才可删除 dormant 副本。Playwright 配置必须自主管理双 App readiness，dev artifact 必须证明 `/dev-api` 且排除 `/prod-api`，prod 反向证明。
- `DEV-T15-004`（ticket review remediation，Lead 于 2026-08-27 登记）：第二轮标准轴在 `3fc3a68` 发现迁移后的 `apps/admin-web/src/types/{auto-imports,components}.d.ts` 未被 Oxfmt ignore 覆盖，以及 12 份直接受 contract 影响的 package README 仍把 root facade 描述为当前存在；规格轴对同固定点 PASS。T15 只迁移两个 ignore 路径并同步边界文档，不修改 package 源码/manifest/测试/依赖。新 source `27c3c5262bfa67c9c79701a7f570ee31bd1bfc79` 必须重新双轴 PASS 后才可建立 candidate。
- `DEV-T15-005`（ticket review remediation，Lead 于 2026-08-27 登记）：第三轮标准轴在 `27c3c52` 发现 platform-auth 被误述为双 App 消费，且 devtools、app-runtime、ui-element、permission、architecture 等 README 仍保留根 App 当前态；规格轴对同固定点 PASS。T15 只修正 11 份 README，明确 client-web 自持 unauthorized 流程、root 为 workspace orchestrator、现有 Element 组件 App-owned，并同步剩余 facade 退役事实。新 source `50ff2207791c87d10ff8f618090a6d82f160b645` 必须重新双轴 PASS 后才可建立 candidate。
- `DEV-T16-001`（ticket package/lock ownership，Lead 于 2026-08-27 批准）：T16 可激活 private `@namewta/api-contracts` 与 `@namewta/tooling-openapi` manifests，在七个 domain manifests 中仅增加实际消费的 `@namewta/api-contracts: workspace:*`，固定唯一外部生成器 `openapi-typescript@7.13.0`，并机械更新对应 lock importers/nodes。根 package/scripts、pnpm workspace/catalog、apps、web-domains、其他依赖与既有 resolutions 只读；generate/check/fetch 从 package-local scripts 暴露。Lead 必须核对 structured manifest/lock diff、离线确定生成、失败保留 last-known-good、intentional drift/manual edit 反向测试、完整 Gate I 与双 App build。
- `DEV-T16-002`（ticket review remediation，Lead 于 2026-08-27 登记）：初轮双轴审查在 `40627f50bd58fc495b046434da51c9248b83ca6c` 发现 provenance 未进入 fetch/generate/check 合同、根 Oxfmt 会改写 raw snapshot/generated output、七领域 mapper 仅被测试引用、生成器在不支持的 TypeScript 6 peer 下运行，另有 source URL 泄漏和测试临时目录未清理风险。T16 可只为两个受保护文件增加精确 Oxfmt ignore，在 tooling package 本地固定 `typescript@5.9.3`，并整改 provenance 原子更新/严格校验、URL 脱敏、临时目录清理、真实 HTTP response -> generated transport -> mapper -> existing business model 链路及直接受影响 README。App、根 package/scripts、workspace/catalog、web-domains、其他根配置、公共业务模型和 endpoint 行为只读；新 source 必须通过 provenance/drift/formatter 反向测试、完整 Gate I 与同一固定点双轴 PASS。
- `DEV-T16-003`（architecture ratchet，Lead 于 2026-08-27 登记）：source Gate 在整改后首次运行 architecture check，发现 generator-local `typescript@5.9.3` 与共享 catalog `typescript@6.0.3` 的统一引用规则冲突。T16 可在 architecture tooling 中增加且仅增加 `@namewta/tooling-openapi` / `devDependencies` / `typescript` / `5.9.3` 四元组例外，并用正反 fixture 证明其他 package、dependency field、version 仍触发 `catalog-reference`。不得修改 catalog、根 scripts/package 或把例外扩展为路径/规则级 ignore。
- `DEV-T16-004`（second standards review remediation，Lead 于 2026-08-27 登记）：规格轴对 `a33d5b75a54e6c5a43967616926575271b4f5891` PASS，标准轴不通过并发现 workflow/operations mapper 仍以双断言展开 transport、大小写 HTTP scheme 泄漏 source secret，以及 snapshot/provenance 双 rename 在进程终止窗口不具备原子性。T16 只在既有授权路径内改为字段级 mapper（含 Date/Map/identifier/nested list 转换并拒绝携带 generated-only 字段）、URL parser 规范化 protocol 分流与脱敏、immutable digest revision + 单一原子 `current.json` activation pointer；补齐全字段/额外字段排除、大小写 scheme 与 revision activation/last-known-good tests。新 append-only source 必须重新跑完整 Gate I，并让两轴对同一 SHA 重新 PASS。
- `DEV-T16-005`（third fixed-point remediation，Lead 于 2026-08-27 登记）：`b7d2a283768bfa5bc6266ec5afd85b514eb5f632` 的规格轴与标准轴复审发现 workflow/operations 在 page `data/rows` 缺失时仍以双断言绕过 projection，raw-only revision 不能绑定 provenance 身份且同 raw/新 backend commit 会碰撞，`HTTPS:/` 单斜杠凭据 URL 又会落入本地路径错误。T16 只在既有授权路径内移除剩余断言并把缺失 page data/rows/total 显式归一化，改用 raw bytes + canonical provenance bundle digest 标识 revision，并让任意大小写 `http(s):` 输入先经 URL parser 与脱敏 label。反向测试必须覆盖三项合法 provenance 字段篡改、同 raw 不同 backend commit、缺失分页字段和标准/单斜杠 URL secret/query；新 source 必须完整重跑 Gate I 并取得同一 SHA 双轴 PASS。
- `DEV-T16-006`（fourth spec review remediation，Lead 于 2026-08-27 登记）：规格轴在 `6d06f89d911f24a0deed64cc71f836d9b3626c75` 发现带前导空格/Tab 的 `HTTPS:/` 会被 WHATWG URL parser 规范化，但 scheme-first 正则仍把原串当本地路径回显。T16 仅可让 `classifySource` 先以 `new URL` 分类可解析 HTTP(S)，对解析失败但 `trimStart()` 后声明 HTTP(S) scheme 的输入返回固定错误，并补标准、单斜杠、前导空格与前导 Tab 的 credential/query 反向测试。控制字符范围正则因 `no-control-regex` 被完整 Gate 拒绝，不得提交；当前实现必须重新通过完整 Gate I 与同一 SHA 双轴 PASS。
- `DEV-T16-007`（fifth spec review remediation，Lead 于 2026-08-27 登记）：规格轴在 `e48375c11ff16cd9fe2658e22b50ee5c2d5cb380` 发现 URL/fetch 的原始异常仍由 `Error.cause` 在常规日志中泄漏 credential/query，既有测试只检查顶层 message。T16 仅可让 HTTP URL parse/fetch 安全错误不携带不可信 cause，并在分类阶段拒绝含 userinfo 的 HTTP(S) URL；测试用完整错误对象展开验证四类规范化输入均不含 secret/token/query。其他本地文件/JSON/provenance 错误的诊断 cause 保留；新 source 必须完整重跑 Gate I 与同一 SHA 双轴 PASS。
- `DEV-T17-001`（docs target ownership，Lead 于 2026-08-27 登记）：`docs/upstream/customization-map.md` 在 T17 开始前已有用户把错误的 `DSL.sql` 修正为 `DML.sql`；旧表仍按上游路径热点组织并描述已退役的根 `src`。T17 可整体替换该目标文件，建立 capability-first 十字段 schema 和 T15/T16 后的真实 owner boundaries，但必须保留用户的 DDL/DML append-only 修正以及 Client/auth/permission 安全不变量。docs commit 只暂存 Ticket 明确路径，不吸收其他既有脏改动。
- `DEV-T17-002`（first standards review remediation，Lead 于 2026-08-27 登记）：标准轴对 source `60540c4` / tree `7b7c9ba` 与 docs hashes `43435bca...` / `74dcb440...` 判定 FAIL：completed baseline 残留不存在的概念路径，decision/status 流无法无歧义表达有效 defer，终端索引遗漏 README-only taro-request。T17 只把 owner 改为真实 `packages/**`，增加 `deferred` 终态及 re-triage 流，区分 blocked 与 defer，并补齐 taro-request；source commit 不变，整改后的 docs hashes 必须重新取得同一 fixed-point 双轴 PASS。

## 6. Progress and Decisions

### Current Status

- Goal Plan 状态：`completed`；Lead：`codex:/root`；T-01 至 T-17、Gate A 至 Gate J 全部关闭。
- 前端最终结果：`plus-ui-namewta main@60540c412fd00ebb5b948ff26dcab6ae9763d7ef`，tree `7b7c9baa447f0d241fdd847d4a083acc1fbcb515`。T17 source/candidate/result 完全一致，第二轮双轴 PASS，Attempt 1 scoped Gate 通过后 fast-forward 推进；transient candidate 已删除、source 保留。
- docs product result：`main@0e31753ab1c8929951ee9f5aafbe913b5dc11440`，tree `e67fe1efd2587f931c3562070b7d86ee0f5fd842`；只提交两份 upstream 文档和必要 submodule pointer，未吸收父仓库其他既有脏改动。
- `T-09 / Gate G3` 已关闭：最终 source/candidate/result 为 `39f7eaef8bff2002ba6824a2616e2050991e6959`，tree `6d62a305098c787bc586adc360f01c14e630fbd9`；双轴 PASS，frozen install、20-workspace architecture `0 + 92/92`、lint/typecheck、root `34/145` 与全部 workspace unit、双 App build、workflow Playwright `11/11` 和 full dual-App `29/29` 全部通过。前三次 candidate 未推进 parent，第 4 次全绿后 Lead 才推进前端 `main`；transient candidate 已删除，source 保留。
- `T-10 / Gate G4` 已关闭：source/candidate/result 为 `9ce426c20be49209d24b40083e14eede4ea31109`，tree `1672fcb46b94acbb1f5f71cefaa1a8aa77415987`；双轴 PASS，frozen install、21-workspace architecture `0 + 92/92`、lint/typecheck、root `36/153` 与全部 workspace unit、双 App build、T10 Playwright `3/3` 和最终 full dual-App `32/32` 全部通过。full 首轮 `31/32` 的既有 workflow locator 时序歧义在单测 `1/1` 与第 2 次 full 全绿后关闭，父分支仅在完整通过后推进；transient candidate 已删除，source 保留。
- `T-13 / Gate G6` 已关闭：attempt 7 source/candidate/result 为 `a73007e54dcb517fccf4e5470e679dfee7ac0c00`，tree `37895e4c70901b47dfe40f17c9fe47b5c907687c`；双轴 PASS，27-workspace architecture `0 + 92/92`、lint/typecheck、root `48 files / 232 tests` 与全部 workspace unit、双 App build、targeted devtools `5/5` 和 full dual-App Playwright `47/47` 全部通过。transient candidate 已删除，source 保留；下一执行点为 T15。
- T15 的 `da8e21f`、`3fc3a68` 与 `27c3c52` 均未进入 candidate；append-only 整改保留真正的 pre-contract `ad4e971`。最终 source/candidate/result 均为 `50ff2207791c87d10ff8f618090a6d82f160b645`（tree `a3b748e2d48ddbb96f9eba5e1bdc3bb868780e71`），双轴复审 PASS。Attempt 1 通过 frozen install、architecture `28/0 + 92/92`、完整 lint/typecheck/unit、development/production 双 App build、API 模式反向产物检查与 Playwright `47/47`，Lead 以 fast-forward 推进 `main` 并清理 transient candidate；Gate H 已关闭，源 worktree/branch 保留。

### Pending Decisions and Blockers

无执行 blocker。用户已授权 `integration_attempt_limit: 7`、计划内实现 commit、本地 plus-ui candidate integration/父分支推进和 T-17 docs scoped commit；push、部署、发布、归档和 source cleanup 仍不在本 change 的授权范围。

### Resume Protocol

恢复时依次读取本 Goal Plan、当前 Ticket、`.status.json` 的实际 worktree 记录、最新 Ticket Evidence、plus-ui `main` HEAD/status/worktree list 和 docs status。若存在 source checkpoint，从该 worktree继续；若 candidate failed/stale，父分支保持最后 result 并按最新 HEAD 重建；若无活动记录，从所有依赖 Evidence 已关闭的最小 Ticket 开始。不得根据 Ticket 数量或口头进度猜测 checkpoint。

## Assumptions

- 前端 `main` 是本地集成父分支；执行前若分支或 HEAD 由用户改变，Lead 重做 preflight 并将 planned base 更新为实际值。
- worktree 的实际物理目录由 I-implement 的 dev-worktree 生命周期选择；状态中只使用 portable locator `specdev-worktree/2026-08-25-plus-ui-multi-app-domain-architecture/T-NN`。
- T-17 的 docs 修改可通过显式 staged paths 与现有脏改动隔离；若同一目标文件已有用户修改，Ticket 停止并升级 ownership deviation。
- OpenAPI 权威来源是执行期可从后端配置/文档发现的事实；不存在或不稳定时 T-16 记录 deviation 并停止，不降低生成合同。
