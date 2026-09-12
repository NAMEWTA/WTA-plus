---
schema_version: 6
artifact: goal-plan
change: 2026-08-30-openapi-common-module
status: completed
modes: [migration, high-assurance, reference-conformance, release-coordination]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: 7
ticket_workspace_policy: required
integration_gate: candidate-merge
ready_for_execution: false
---

# Goal Plan: OpenAPI Common Module 完整平台交付

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在不改变普通浏览器 Token、Client 校验、动态路由和既有权限链的前提下，交付默认关闭的 NAMEWTA v1 OpenAPI 平台：仅方法级显式开放，按每用户唯一加密凭据验签，使用 Redis 原子 nonce/双限流，复用标准 `LoginUser` 与 Sa-Token TokenSession，按所有合法 Client 的当前权威授权构建快照，并提供管理员目标用户页与个人中心本人 Tab。

### Success and False Completion

成功必须同时满足：12 张非 cancelled Ticket 各有 clean、非空 source commit、通过的 parent-candidate、对应 repository `main` result SHA 与 Lead Evidence；30 个 AC 全部闭合；T-01 固定向量成为 NAMEWTA v1 可执行参考；T-06 SQL 只追加且未执行生产迁移；T-07/T-08 的权威写入口失效矩阵无遗漏；T-09/T-12 的应用边界 E2E 在 candidate 状态由 Lead 运行；T-12 证明 default-off、enabled fail-closed、backend `bundle-full`/`bundle-core` 和前端 architecture/test/typecheck/lint/build 门禁。

以下均为伪完成：只生成接口目录而无真实 HandlerMapping；只校验 AppKey 不校验完整请求；用当前请求补写权限；返回内部机器 Token；只删 Redis key；遗漏任一授权写入口；UI 隐藏替代后端 scope；secret 进入日志或持久化前端状态；把 test double 宣称为真实 MySQL/Redis/多节点；source worktree 自报 E2E；candidate 未进入对应 `main`；父仓库吸收无关 dirty 内容；通过跳过测试或放宽安全/类型规则制造绿色。

### Non-goals

- 不实现旧 MD5、OAuth2、多凭据、双 secret、IP 白名单、计费、框架级幂等或响应重放。
- 不开放依赖单一 `clientPk/clientKey` 的方法，不修改普通 SecurityConfig 以全局绕过 Client。
- 不交付 client-web、mobile-web、miniapp-taro 或恢复 `gen`。
- 不把真实 MySQL、真实 Redis、多进程、多节点或完整 Playwright 提升为本 change 强制 Gate；它们必须作为残余风险如实报告。
- 不推送、创建 PR、远程合并、部署、执行生产 DDL/DML、下发 KEK 或启用生产 `openapi.enabled=true`。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | worktree 策略、产品取舍与批准 | 更新真正拥有该决定的工件和授权状态 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | 当前 OpenAPI 架构、安全与领域语义 | 返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` 更新真正 owner |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | Client、日志、数据库与前端长期边界 | 当前 change 替代时必须在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为、范围、30 个 AC 与验收接缝 | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 路线、路径、迁移与验证 | Goal Plan 只编排 |
| 6 | 当前代码、配置、POM、package scripts、测试与 Git | 可执行命令、基线和实现可行性 | 冲突时暂停并按 deviation 返回真正 owner |

## 2. Execution Graph

### DAG and Critical Path

```text
G-00 authorization + repository baseline
  ├─→ T-01 protocol/module ─→ T-03 authorization snapshot
  │                           ├─→ T-05 session bridge ─→ T-06 credential/schema ─→ T-09 gateway ─┐
  │                           │                         ├─→ T-07 identity invalidation ───────────┤
  │                           │                         └─→ T-08 RBAC invalidation ───────────────┤
  │                           └─→ T-04 registry/catalog ────────────────┐                         │
  └─→ T-02 logging ────────────────────────────────────────────────→ T-09                         │
                                                                      │                         │
                              T-04 + T-06 ─→ T-10 frontend domain ─→ T-11 dual UI ──────────────┤
                                                                                                └─→ T-12 assembly/release
```

存在两条同长关键路径：`G-00→T-01→T-03→T-05→T-06→T-09→T-12` 和 `G-00→T-01→T-03→T-04→T-10→T-11→T-12`。T-02 在 T-09 汇合；T-07/T-08 在 T-12 汇合。source worktree 可按 Wave 并行，Lead candidate integration 全局串行，固定序号为 `T-01→T-02→T-03→T-05→T-04→T-06→T-10→T-07→T-08→T-09→T-11→T-12`。下游 worktree 激活时必须从其 repository 最新已通过 result 重新冻结 base，不以计划创建时 seed SHA 代替。

### Waves and Ownership

| Wave | Ticket | 前置条件 | Repository/项目写路径摘要 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | G-00 | backend common module/protocol/POM | T-01：四个共享 POM | 1 / G-10 |
| W1 | T-02 | G-00 | backend common-web 日志过滤 | T-02：统一日志策略 | 2 / G-10 |
| W2 | T-03 | T-01 result；G-10 | backend system authorization/mapper | 无 | 3 / G-20 |
| W3 | T-05 | T-01/T-03 result | backend common session bridge | 无 | 4 / G-30 |
| W3 | T-04 | T-01/T-03 result | backend registry/catalog/controllers | 无 | 5 / G-30 |
| W4 | T-06 | T-01/T-05 result | backend credential/API/SQL | T-06：DDL/DML | 6 / G-40 |
| W5 | T-10 | T-04/T-06 result | frontend system domain | T-10：domain root/package/README | 7 / G-50 |
| W4 | T-07 | T-05 result | backend user/Client/login-domain writes | 无 | 8 / G-40 |
| W4 | T-08 | T-05 result | backend role/menu/data-scope writes | 无 | 9 / G-40 |
| W5 | T-09 | T-02/T-04/T-05/T-06 result | backend gateway/nonce/rate/event | 无 | 10 / G-50 |
| W6 | T-11 | T-10 result | frontend web-domain/Admin profile | T-11：web-domain root/profile | 11 / G-60 |
| W7 | T-12 | T-07/T-08/T-09/T-11 result | backend assembly/config；parent customization map | T-12：imports/config/map | 12 / G-70 |

`implementation_agent_limit=3` 是 config 与平台能力快照，不要求填满。W1 峰值 2；W3 峰值 2；W4 可达 3；W5 峰值 2。T-10 result 一旦成立可启动 T-11 source，不必等待 T-09 candidate；所有 candidate 仍由 Lead 一次处理一个。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | module、SPI 与固定 v1 协议向量 | — | `specdev-worktree/{change}/T-01`（backend） | execution-time dynamic | not-required：纯协议 prefactor | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-02 | OpenAPI 凭据与正文日志脱敏 | — | `specdev-worktree/{change}/T-02`（backend） | execution-time dynamic | not-required：Servlet filter module test | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| T-03 | 全局只读授权快照 | T-01 | `specdev-worktree/{change}/T-03`（backend） | execution-time dynamic | not-required：无 HTTP resolver | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| T-04 | Handler registry 与目标用户目录 | T-01,T-03 | `specdev-worktree/{change}/T-04`（backend） | execution-time dynamic | not-required：Spring context/MockMvc | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| T-05 | 机器 TokenSession 与 scoped bridge | T-01,T-03 | `specdev-worktree/{change}/T-05`（backend） | execution-time dynamic | not-required：Sa-Token module test | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| T-06 | 唯一凭据、self/admin API 与 SQL | T-01,T-05 | `specdev-worktree/{change}/T-06`（backend） | execution-time dynamic | not-required：真实 MySQL 竞争是残余风险 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| T-07 | identity/Client 授权写后失效 | T-05 | `specdev-worktree/{change}/T-07`（backend） | execution-time dynamic | not-required：service interaction | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| T-08 | RBAC/menu/data-scope 写后失效 | T-05 | `specdev-worktree/{change}/T-08`（backend） | execution-time dynamic | not-required：service interaction | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| T-09 | 完整签名调用网关 | T-02,T-04,T-05,T-06 | `specdev-worktree/{change}/T-09`（backend） | execution-time dynamic | required：Lead 在 parent-candidate 跑 MockMvc 应用边界 E2E | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| T-10 | system domain 合同 | T-04,T-06 | `specdev-worktree/{change}/T-10`（frontend） | execution-time dynamic | not-required：domain tests | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |
| T-11 | admin 动态页与个人 Tab | T-10 | `specdev-worktree/{change}/T-11`（frontend） | execution-time dynamic | not-required：组件/App 集成；Playwright 非强制 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| T-12 | default-off 装配与最终发布候选 | T-07,T-08,T-09,T-11 | `specdev-worktree/{change}/T-12`（backend）；parent map 由 Lead | execution-time dynamic + Lead parent owner | required：Lead 跑 Spring/MockMvc 与前端集成发布边界 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- 每张非 cancelled Ticket 有最新依赖 base、clean source commit、Lead 核对的路径 diff、通过的 candidate/result SHA 和独立 Evidence；成功集成不自动清理 source/candidate worktree。
- backend `main` 包含 T-01 至 T-09、T-12；frontend `main` 包含 T-10/T-11；父仓库最终组合只包含经批准 gitlink、T-12 customization map 和本 change Speculo 工件，不吸收现有 archive/ADR/context 等无关 dirty 内容。
- AC-001 至 AC-030 无未经批准 deferred；固定向量、授权负向矩阵、日志脱敏、owner scope、Session 清理、default-off 与 fail-closed 都有 checkpoint/command/cwd/exit code。
- T-12 完整运行 backend tests、`bundle-full`、`bundle-core` 与 frontend architecture/test/typecheck/lint/build:prod；未运行真实基础设施和 Playwright 必须标记 `not-run/residual-risk`。
- 生产 DDL/DML、KEK、启用、远程操作与 cleanup 保持独立批准；本地代码完成不能推定生产已发布。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-00 Execution authorization | 用户选择 required；基线已冻结 | implementation commit 与 local candidate integration 均为 authorized；三仓 `main`/SHA/dirty 快照重读 | 全部 Ticket | Lead + 用户 | 已关闭；执行前若基线漂移则刷新快照，未进入 I-implement 不创建 source/candidate worktree |
| G-10 Protocol/log prefactor | G-00 | T-01/T-02 result；fixed vectors、artifact graph、敏感日志矩阵通过 | T-03 及所有消费者 | Lead | 父分支不推进失败 candidate；修正原 source worktree |
| G-20 Authorization contract | T-01 result | T-03 result；合法 Client/状态/默认显式角色/dataScope/超管负向矩阵 | T-04/T-05 | Lead | 返回 T-03；禁止消费者复制临时算法 |
| G-30 Registry/session contract | G-20 | T-04/T-05 result；registry/schema/matcher 与 Session miss/hit/cleanup 证据 | T-06/T-07/T-08/T-09/T-10 | Lead | 暂停消费者，修订唯一 owner Ticket 后重建 base |
| G-40 Credential + invalidation migration | G-30 | T-06/T-07/T-08 result；SQL append-only；credential lifecycle；全部权威写入口映射归零 | T-09/T-12 | Lead；生产迁移仍需用户 | 关闭 OpenAPI，保留 additive SQL，回到失败 Ticket 前向修复 |
| G-50 Vertical contracts | T-02/T-04/T-05/T-06 result | T-09 required E2E result；T-10 domain result；调用与 transport 合同一致 | T-11/T-12 | Lead | candidate 不推进；gateway/domain 各回原 worktree |
| G-60 Dual UI | T-10 result | T-11 result；manifest/profile、scope、permission、one-time secret 组件/App 测试 | T-12 | Lead | 保持后端 default-off；修正 T-11，不复制 App API |
| G-70 Release candidate | G-40/G-50/G-60 | T-12 result；default-off/fail-closed、full/core、frontend full gates、最终 SHA/Evidence/风险清单 | change completion | Lead；生产动作由用户另批 | 开关保持 false，父分支不推进失败 candidate，保留 schema 前向修复 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001 至 AC-007：开放/协议/nonce/限流 | T-01,T-04,T-09,T-12 | fixed vectors + registry + MockMvc | T-01/T-04/T-09/T-12 | verified |
| AC-008 至 AC-011：唯一凭据生命周期 | T-06,T-10 | service/controller/domain | T-06/T-10 | verified |
| AC-012 至 AC-019：授权快照与 Session | T-03,T-05,T-07,T-08,T-09,T-12 | authorization/session/invalidation matrices | T-03/T-05/T-07/T-08/T-09/T-12 | verified |
| AC-020 至 AC-022：真实目录 | T-04,T-10 | HandlerMapping/SpringDoc/domain | T-04/T-10 | verified |
| AC-023 至 AC-027：双 UI 与 owner scope | T-06,T-10,T-11,T-12 | controller/component/App integration | T-06/T-10/T-11/T-12 | verified |
| AC-028 至 AC-029：日志安全 | T-02,T-12 | captured HTTP/operation logs | T-02/T-12 | verified |
| AC-030：普通请求兼容 | T-05,T-07,T-08,T-09,T-12 | backend/frontend security regression | T-05/T-07/T-08/T-09/T-12 | verified |
| NAMEWTA v1 reference conformance | T-01,T-04,T-09 | immutable vectors + generated cURL/Java + real invocation | T-01/T-04/T-09 | verified |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence、E2E、candidate 与父分支 owner |
| Implementation subagents | 最多 3，Lead 不计入 | config=3、平台可用上限=3；实际仍受 Wave/路径限制 |
| Integration attempts | 每 Ticket 最多 7 | config 快照=7 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写项目或状态 |
| Dispatch | execution-time dynamic | provider/模型/派单按 Ticket 事实选择，不预分配 |

每次 implementation Dispatch Packet 必须绑定 Ticket、依赖 Evidence、repository、不可变 base SHA、branch/worktree locator、writable/read-only/shared paths、允许动作、非 E2E 检查、停止条件和返回格式。Subagent 只写分配的 source worktree 并返回 commit；不写 Ticket/Map/Goal Plan/Evidence/status，不运行或声明 E2E，不推进 `main`。Lead 复核实际 diff、commit、dirty 与检查后才进入 candidate。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01,T-02 | backend `main`; initial `e5cef5a616cea273d52fd57d510983b37f29144c` | 各自 backend source branch/worktree | Ticket module tests/package | required | Lead reactor；E2E n/r | backend result |
| T-03,T-04,T-05 | backend 最新 blocker result | 各自 backend source branch/worktree | Ticket authorization/registry/session tests | required | Lead affected reactor；E2E n/r | backend result |
| T-06,T-07,T-08 | backend 最新 blocker result | 各自 backend source branch/worktree | credential/SQL/invalidation tests | required | Lead system/admin regression；E2E n/r | backend result |
| T-09 | backend 最新 T-02/T-04/T-05/T-06 result | backend source branch/worktree | common gateway tests | required | Lead MockMvc required E2E | backend result |
| T-10 | frontend `main`; initial `381918e7c2c3e023c043adcdaf94b0476c501a2d`，并以 backend contract Evidence 为输入 | frontend source branch/worktree | domain test/typecheck/lint | required | Lead architecture + affected gates；E2E n/r | frontend result |
| T-11 | frontend 最新 T-10 result | frontend source branch/worktree | web-domain/App tests | required | Lead architecture/typecheck/lint/build；E2E n/r | frontend result |
| T-12 | backend 最新汇合 result；frontend T-11 result 只读 | backend source branch/worktree；parent map 由 Lead 精确暂存 | assembly tests/docs review | required | Lead backend full/core + frontend full gates + required release E2E | backend result + parent composition result |

required 模式下 source worktree 不运行 E2E。Lead 在对应 repository 最新 `main` 的独立 `specdev-worktree/.integration/{change}/T-NN` candidate 中组合 source checkpoint；验证通过且父 HEAD 未漂移后才 fast-forward 或 merge-commit 推进 `main`。candidate 失败保持父分支不变；父 HEAD 漂移标记 stale 并从最新 result 重建。T-12 的 parent customization map 与最终 gitlink/Speculo 组合由 Lead 在独立父仓 candidate 中精确暂存，禁止吸收当前父仓的无关 dirty archive/ADR/context/command 变化。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | not-authorized | required 模式不在 current workspace 实现项目代码 |
| Ticket worktree local changes | allowed | 仅在进入 I-implement 后，于目标 Ticket 唯一 source worktree 和 writable paths 内实施 |
| Implementation commit | allowed | 用户已明确授权；每 Ticket 必须形成 clean、非空 source commit |
| Local direct-parent verification and parent update | not-authorized | 本计划不使用 current/direct-parent |
| Local candidate integration and parent update | allowed | 用户已明确授权；required 模式 Lead-only，按固定顺序在隔离 candidate 验证后推进本地 parent |
| Push / PR / remote merge | not-authorized | 不从任何本地授权继承 |
| Branch/worktree cleanup | not-authorized | 成功集成不自动清理 |
| Deploy / production DDL/DML / KEK / enable | not-authorized | 每项需单独目标、窗口与批准 |

### Evidence Return

Implementation 返回至少包含 Ticket ID、repository/workspace locator、base/final commit、dirty 状态、修改路径、非 E2E 命令/退出码、未运行项、冲突和恢复条件。Lead 独立核对后写 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-NN.md</Path>`、worktree 状态、Ticket/Map 投影与 Gate 关闭证据。外部 provider 只有取得独立数据发送授权并满足 ZIP-only 合同后才可使用。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- common 不反向依赖 system；system 是 credential/authorization 事实 owner；admin 只装配。
- 只允许方法级 `@OpenApi`；NAMEWTA v1 canonicalization 与固定向量发布后不可静默变化。
- credential 只绑定 userId；AppSecret AES-256-GCM + versioned KEK 且只显示一次。
- Redis miss 只读装载已有授权；不创建/补写权限；`clientPk/clientKey` 为空且无 fallback。
- dual-auth、Redis/KEK/SPI/Session/失效错误全部 fail closed；普通 Token 路径不进入机器 Session。
- SQL 仅追加 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>`/`DML.sql`，不改 `ry_vue.sql`；新表满足七基础字段和中文注释。
- 前端保持 App -> web-domain -> domain；self 无 userId，admin 显式 target；secret 不进入 store/cache/URL。
- 所有 POST 业务接口使用准确且安全的 `@Log`；secret/signature/token/canonical/raw sensitive body 不进入日志。

### Verification Integrity

Source checks 只证明 Ticket 局部实现；required E2E 只能由 Lead 在 parent-candidate 运行。判卷必须记录 cwd、checkpoint、完整命令、exit code、执行/跳过数量和基础设施类型。禁止把测试文件存在、mock、skipped 属性测试、`-DskipTests` package、写入式 formatter 或缩小 typecheck 当作完整门禁。T-12 使用真实 active 命令：backend `./mvnw test`、`-Pbundle-full -DskipTests package`、`-Pbundle-core -Dmaven.test.skip=true package`；frontend `pnpm architecture:check`、`pnpm test`、`pnpm typecheck`、`pnpm lint`、`pnpm build:prod`。

### Migration or Release Sequence

1. Expand：T-01 至 T-06 引入默认不可达的 module/protocol/authorization/registry/session/credential 与 additive SQL。
2. Migrate：T-07/T-08 覆盖所有影响全局快照的 system 写入口，直到调用点矩阵归零。
3. Observe/verify：T-09 签名调用 E2E、T-10/T-11 双 UI 合同和普通认证回归。
4. Contract：T-12 唯一自动配置与 default-off/fail-closed 门禁，完成 full/core/前端全量验证。
5. Release：生产先备份并执行经批准 DDL/DML，再部署默认关闭代码，配置 Redis/KEK，最后单独批准启用；本 Plan 不执行这些动作。

### Risks, Monitoring and Recovery

- 授权并集或失效入口错误可能越权：以状态/默认角色/dataScope/写入口负向矩阵判卷；失败立即关闭 OpenAPI 并前向修复。
- canonicalization 跨语言漂移：T-01 固定向量是唯一可执行参考，示例和网关必须消费同一实现。
- test double 不能证明 MySQL 唯一竞争、Redis 原子性或多节点传播：最终报告保持 residual risk，不升级为 passed。
- 单 secret reset 会立即中断调用方：这是已批准产品取舍；恢复只能发新 secret，不能找回旧值。
- HTTP 不提供机密性：文档明确生产推荐 HTTPS；服务端不擅自强制 TLS。
- 父仓当前存在其他 archive/ADR/context/command dirty 变化：所有 parent candidate 精确按路径组合，任何意外 diff 停止并返回 Lead。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。public protocol、权限语义、schema、shared owner、workspace、E2E、生产顺序或授权变化属于高影响偏差，暂停受影响 Wave 并返回 Spec/ADR/Ticket/用户 owner；局部类名和私有拆分可在 Ticket writable path 内由实现者决定，但必须保持测试与公开合同。

## 6. Progress and Decisions

### Current Status

- Workspace 决定：用户于本次 Goal Plan 明确选择开启 Ticket 独立 worktree；固定 `required + candidate-merge`。
- Execution authorization：用户于 2026-08-31 明确授权 Ticket implementation commits 与 Lead-owned local candidate integration/parent updates。
- Baseline：parent `main@fd113e4c80b798063a21fde6986b884f8ff93557`；frontend `main@381918e7c2c3e023c043adcdaf94b0476c501a2d` clean；backend `main@e5cef5a616cea273d52fd57d510983b37f29144c` clean。
- Parent dirty：存在用户的 archive/ADR/context/command 变化及本 change 规划工件；不属于 implementation source worktree 输入，不得被候选隐式吸收。
- Gate：G-00 closed；Plan Ready。W1 的 T-01/T-02 可在进入 I-implement 并重读基线后激活；当前仍无 source/candidate/result SHA 或实现验证。

### Pending Decisions and Blockers

- 当前无执行阻塞项。
- production migration、KEK、启用、远程操作和 cleanup 不是 Plan Ready 前置，但始终保持独立未授权。

### Resume Protocol

恢复或进入 I-implement 时读取本 Goal Plan、目标 Ticket、`<Path>{roots.state}/specdev/changes/{change}/.status.json</Path>` 和最新 Evidence，并重读三仓 branch/SHA/dirty 与授权。Plan 已 Ready、G-00 已关闭；激活 Ticket 时把其 planned `base_sha` 更新为对应 repository 最新 blocker result，再创建唯一 source worktree；从最后通过的 result 或待修正 source checkpoint 继续，不重新决定 workspace、DAG、Gate 或 owner。

## Assumptions

- 逻辑 workspace locator `specdev-worktree/{change}/T-NN` 在执行期映射到对应 backend/frontend repository 的唯一 Git worktree；物理路径由 Lead 创建时记录。
- T-12 以 backend source worktree 为主要实现 workspace；父仓 customization map 与最终 gitlink/Speculo 组合由 Lead 在隔离 parent candidate 精确提交。
- 当前三个 `main` 和已确认的 Maven/pnpm 命令在授权前若漂移，Lead 只刷新基线与受影响计划投影；若改变合同则触发 deviation。
