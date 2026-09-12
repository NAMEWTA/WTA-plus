---
schema_version: 6
artifact: goal-plan
change: 2026-08-28-user-password-policy-temporary-credentials
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

# Goal Plan: 统一密码策略、临时凭据与 Client 精确会话失效

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在保持存量弱密码登录兼容和永久密码重置既有会话行为的前提下，交付一套由 `sys_config` 驱动、服务端最终裁决的统一密码策略；让管理员可编辑服务端生成的重置候选，并能在独立权限下签发 60 秒、用户级、单次原子消费的临时登录密码；角色权限和用户角色变化后，只让受影响 Client 的旧授权会话在 Redis 与全部 JVM 本地缓存中立即失效。

### Success and False Completion

成功必须同时满足：8 个非 cancelled Ticket 都有非空 source commit、通过的 parent-candidate、对应子模块 `main` result SHA 和 Lead Evidence；`AC-001` 至 `AC-024` 全部闭合；真实 Redis/MySQL、双 JVM、多 Client、并发 CAS 和浏览器 required E2E 均未跳过；OpenAPI 生成来源指向已集成后端 result；NAMEWTA DML fresh/upgrade/补偿演练通过；父仓库最终 result 精确记录两个已验证 gitlink 和本 change 长期文档。

以下均属于伪完成：只加强前端正则；仍有密码写入口绕过服务端策略；临时密码可重复使用、修改永久密码或产生特殊会话；只清 Redis 或当前 JVM；权限变化扩大到无关 Client；source worktree 自报 E2E；mock 替代 Redis CAS、跨 JVM或 MySQL 迁移；candidate 未进入子模块 `main`；父仓库误吸收未批准 gitlink 或其他工作区改动；通过删除测试、放宽规则或把 skipped 写成 passed 制造绿色。

### Non-goals

- 不迁移、批量重置或登录时阻断存量弱 BCrypt 密码。
- 不改变永久密码重置或个人改密后的既有会话生命周期。
- 不建立受限、短时、带标记或强制改密的临时登录会话。
- 不改变 BCrypt 算法，不新增密码历史、周期过期或找回密码。
- 不推送、创建 PR、远程合并、部署、执行生产 DML 或授予生产角色权限。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | worktree 策略、产品取舍与执行批准 | 更新真正拥有决定的工件和授权状态 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/CONTEXT.md</Path>` | 当前认证、Client、缓存和密码策略语义 | 架构变化返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业的 Client、认证日志和前端架构边界 | 当前 change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>` | 外部行为、范围、错误和验收合同 | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/</Path>` | 单 Ticket 路线、路径 owner、迁移与验证 | Goal Plan 只编排 |
| 6 | 当前代码、测试和 Git | 基线、现有缓存层、接口与可行性 | 冲突时暂停并按偏差等级返回 owner |

## 2. Execution Graph

### DAG and Critical Path

```text
G-00 Authorization and repository baseline
  -> T-01 cluster invalidation prefactor
       -> G-10 shared invalidation
            ├─-> T-03 password policy contract -> G-20 policy contract
            │       ├─-> T-04 password writes --------┐
            │       └─-> T-05 temporary auth ---------┴-> G-30 backend feature
            └─-> T-02 Client authorization invalidation ──┘
                                                         -> T-06 public UI/OpenAPI
                                                              -> T-07 admin UI
                                                                   -> G-40 frontend verticals
                                                                        -> T-08 DML/contracts
                                                                             -> G-50 final release candidate
```

关键路径是 `G-00 -> T-01 -> T-03 -> max(T-04,T-05) -> T-06 -> T-07 -> T-08 -> G-50`。T-02 从 T-01 分支并在 G-30 汇合。required 模式允许 T-02/T-03、T-04/T-05 分别并行形成 source commit，但 Lead 始终串行 candidate 集成；固定集成顺序为 `T-01 -> T-03 -> T-02 -> T-04 -> T-05 -> T-06 -> T-07 -> T-08`。

T-03 先于 T-02 集成是为了尽早打开 T-04/T-05 source worktree；T-02 随后进入最新后端 `main`，T-04/T-05 candidate 必须在已包含 T-02 的最新父状态重建。该顺序不改变 Ticket `blocked_by`，只优化关键路径并保持最终汇合。

### Waves and Ownership

| Wave | Ticket | 前置条件 | Repository/项目写路径摘要 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| 0 | T-01 | G-00 | backend common Redis/Sa-Token 与聚合测试 | T-01：跨 JVM cache protocol | 1 / G-10 |
| 1 | T-03 | G-10 | backend policy、common error、Auth context | T-03：policy/error contract | 2 / G-20 |
| 1 | T-02 | G-10 | backend ClientSession 与 role/user service | 无；只消费 T-01 | 3 / G-30 |
| 2 | T-04 | G-20 | backend 五类密码写入与 reset candidate | 无；只消费 T-03 | 4 / G-30 |
| 2 | T-05 | G-20 | backend temporary store/API/password grant | 无；只消费 T-03 | 5 / G-30 |
| 3 | T-06 | G-30 | frontend OpenAPI、admin domain、register/profile | T-06：api-contracts | 6 / G-40A |
| 4 | T-07 | T-06 result | frontend system domain/web-domain/Admin 组合 | 无；只读 T-06 contract | 7 / G-40B |
| 5 | T-08 | G-40B；T-02 result | backend DML/tests；parent docs/gitlinks | T-08：DML/SQL README/customization map | 8 / G-50 |

required 模式下每个 Wave 最多两个 implementation owner；实际峰值为 2，不以配置上限 3 强行填满。父仓库的 Speculo 状态、Evidence、`docs/upstream/customization-map.md` 和最终 gitlink 组合始终由 Lead 持有。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 两个 JVM 的 Spring/Sa-Token 本地缓存立即失效 | — | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-01`（backend） | execution-time dynamic | required：Lead 在 backend parent-candidate 跑双 JVM + 真实 Redis | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-01.md</Path>` |
| T-02 | 角色/用户角色按 Client 精确强制重新登录 | T-01 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-02`（backend） | execution-time dynamic | required：Lead 跑多 Client、多 Token、双 JVM | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-02.md</Path>` |
| T-03 | 统一策略、CSPRNG、详细错误和安全公开投影 | T-01 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-03`（backend） | execution-time dynamic | required：Lead 跑公开 API、跨节点配置刷新和泄漏检查 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-03.md</Path>` |
| T-04 | 五类密码写入收敛与可编辑 reset candidate | T-03 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-04`（backend） | execution-time dynamic | required：Lead 跑真实 MySQL、直接 API、导入和会话兼容 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-04.md</Path>` |
| T-05 | 60 秒、覆盖、单次 CAS 的普通会话认证 | T-03 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-05`（backend） | execution-time dynamic | required：Lead 跑真实 Redis、并发、多 Client 和普通会话等价 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-05.md</Path>` |
| T-06 | OpenAPI 同步，注册/profile 动态策略反馈 | T-04, T-05 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-06`（frontend） | execution-time dynamic | required：Lead 跑公开注册与登录后 profile 浏览器路径 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-06.md</Path>` |
| T-07 | 用户管理重置候选与一次性临时密码交互 | T-04, T-05, T-06 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-07`（frontend） | execution-time dynamic | required：Lead 跑权限、一次展示、复制/清理和 reset E2E | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-07.md</Path>` |
| T-08 | DML fresh/upgrade/补偿与最终长期合同 | T-02, T-06, T-07 | `specdev-worktree/2026-08-28-user-password-policy-temporary-credentials/T-08`（backend）；parent docs 由 Lead current workspace | execution-time dynamic + Lead parent owner | required：Lead 跑真实 MySQL、Redis 和最终跨端流程 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-08.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- 每个非 cancelled Ticket 有 clean、非空 source commit，通过的 candidate、子模块 `main` result SHA 与 Lead Evidence；后续经单独授权，全部 source/candidate worktree 与对应本地分支均已清理并记录为 `removed`。
- backend result 包含 T-01 至 T-05 与 T-08；frontend result 包含 T-06/T-07；父仓库最终 result 只包含经批准的两个 gitlink、本 change Speculo 工件和 T-08 长期文档，不覆盖其他 dirty 内容。
- 24 条 AC 无 deferred；真实 Redis/MySQL、跨 JVM、并发 CAS、多 Client、公开 API、浏览器和迁移 Gate 都有 cwd、checkpoint、命令、退出码和未运行项。
- OpenAPI snapshot/current/generated 只由 T-06 从 G-30 已集成 backend result 生成，并记录 provenance；T-07 不改生成物。
- `sys.user.initPassword` 新代码读取、旧 5/6 位前端规则和 `123456` 默认路径零匹配；DML 历史前缀逐字不变。
- 无未批准偏差、未集成 source checkpoint、活动 candidate、敏感值 Evidence 或把生产动作误写为本地完成。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-00 Authorization + Baseline | Goal Plan 已发布 | implementation commit 与 local candidate integration/parent update 明确 authorized；重读 parent/frontend/backend branch、HEAD、dirty、active worktrees；父 gitlink 前序已单独确认或同步 | 全部实现 | Lead / 用户 | 保持 blocked，不创建 Ticket worktree、不提交 |
| G-10 Shared Invalidation | G-00 关闭，T-01 source checks 通过 | T-01 candidate 双 JVM + 真实 Redis 通过；消息无秘密；backend result/Evidence | T-02/T-03 | Lead | backend `main` 不动，返回 T-01 worktree 修正 |
| G-20 Policy Contract | G-10 关闭，T-03 source 就绪 | T-03 candidate config/error/API/跨节点刷新通过；backend result/Evidence | T-04/T-05 | Lead | 父分支不动；T-02 source 可继续，但消费者不开始 |
| G-30 Backend Feature | T-02/T-04/T-05 source 完成，T-03 result 已稳定 | 按 3→4→5 candidate 集成；真实 Redis/MySQL、多 Client、导入、会话兼容、临时 CAS、认证回归通过；固定 backend OpenAPI SHA | T-06 | Lead | 失败 Ticket 回原 worktree；已通过 result 不回退；stale candidate 重建 |
| G-40A Public Frontend（已关闭） | G-30 关闭，T-06 基于固定 backend contract | `pnpm --filter @namewta/tooling-openapi openapi:check`、架构/lint/type/test/双模式 build、App 私有 register/profile required E2E、frontend result `b06957c558a6864bc824f5ffa0db6d44a8418739` | T-07 | Lead | frontend `main` 不动，修正 T-06；不得手改生成物或迁移静态 App 页面 |
| G-40B Admin Credential UI（已关闭） | G-40A 关闭，T-07 基于 T-06 result | system domain/web-domain manifest/Admin runtime gates、权限负向、一次展示/清理、reset/temporary required E2E、无静态业务路由回流、frontend result `8aa184b353c5a37ee555feb8be808fe9ba885297` | T-08 | Lead | frontend `main` 不动，修正 T-07；backend result 保持 |
| G-50 Migration + Final（已关闭） | G-40B 关闭，T-08 source/parent docs 就绪 | MySQL fresh/upgrade/重复/冲突/补偿；全 backend test/full/core、frontend architecture/lint/type/test/build/48 E2E；两个 gitlink 和父文档 result；24 AC 汇总 | change complete | Lead / 用户授权边界 | 本地 result 已固化；生产动作仍未执行 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..004 候选、确认、详细错误与永久 reset 会话兼容 | T-03, T-04, T-07 | policy/API、MySQL、Token、浏览器 | T-03/T-04/T-07 | passed |
| AC-005..014 临时凭据权限、TTL、原子消费与普通会话 | T-05, T-07, T-08 | 权限/API、Redis CAS、多 Client、浏览器、DML | T-05/T-07/T-08 | passed |
| AC-015..019 公开投影、全写入口、存量兼容和配置迁移 | T-03, T-04, T-06, T-08 | Auth context、直接 API、OpenAPI/UI、MySQL | T-03/T-04/T-06/T-08 | passed |
| AC-020..022 Client 精确会话与跨 JVM 全层失效 | T-01, T-02 | 双 JVM Redis、角色/用户关系、多 Token | T-01/T-02 | passed |
| AC-023..024 审计脱敏与一次 grant 计数 | T-05, T-07, T-08 | 日志/审计/UI/DML review、认证计数 | T-05/T-07/T-08 | passed |
| NAMEWTA append-only SQL 与长期定制边界 | T-08 | DML prefix diff、isolated MySQL、文档交叉 review | T-08 | passed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:leadership-epoch-1` | change leadership；唯一 SpecDev、Evidence、E2E、candidate 与父仓库 owner |
| Implementation subagents | 上限 3，Lead 不计入；实际并行峰值 2 | config=3、平台能力、仅 Wave 1/2 有两个无写冲突 Ticket |
| Integration attempts | 每 Ticket 最多 7 | config 快照 `max_integration_attempts=7` |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写项目或状态 |
| Dispatch | execution-time dynamic | Lead 自行实现或派单、provider 和模型在 Ticket preflight 后决定 |

任何派单都必须使用 operation=dispatch 的不可变 Packet，绑定 Ticket、最新 `base_sha`、repository/source worktree、依赖 Evidence、writable/shared owner、允许动作、非 E2E checks 和停止条件。implementation owner 不写 Speculo、Evidence、父分支或 E2E 结论；Lead 以 operation=accept 重读 diff、commit、dirty 状态和命令结果。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend `main` at G-00 | backend `specdev-worktree/.../T-01` / `speculo/.../T-01` | 定向 common/admin Maven；非 E2E 故障测试 | clean non-empty backend source commit | Lead candidate：双 JVM + 真实 Redis、backend regression | backend result 1 |
| T-03 | T-01 backend result | backend `specdev-worktree/.../T-03` / `speculo/.../T-03` | common/system/admin policy tests | clean non-empty backend source commit | Lead candidate：public API、跨节点刷新、秘密零泄露 | backend result 2 |
| T-02 | T-01 backend result；candidate 基于 T-03 result | backend `specdev-worktree/.../T-02` / `speculo/.../T-02` | system/admin authorization tests | clean non-empty backend source commit | Lead candidate：多 Client/Token/JVM + Redis | backend result 3 |
| T-04 | T-03 result；candidate 基于最新 backend result | backend `specdev-worktree/.../T-04` / `speculo/.../T-04` | api/system/admin write tests | clean non-empty backend source commit | Lead candidate：真实 MySQL、五入口、session compatibility | backend result 4 |
| T-05 | T-03 result；candidate 基于 T-04 result | backend `specdev-worktree/.../T-05` / `speculo/.../T-05` | system/admin auth tests，真实 Redis用例不得 skip | clean non-empty backend source commit | Lead candidate：CAS concurrency、多 Client、ordinary session | backend result 5 / G-30 |
| T-06 | frontend `main` at G-30；backend result 5 provenance | frontend `specdev-worktree/.../T-06` / `speculo/.../T-06` | tooling-openapi check、domain-admin/App test、architecture/lint/type/双模式 build | clean non-empty frontend source commit | Lead candidate：App 私有 register/profile Playwright | frontend result 1 |
| T-07 | T-06 frontend result | frontend `specdev-worktree/.../T-07` / `speculo/.../T-07` | domain-system/web-domain manifest/Admin registry tests 与根非 E2E gates | clean non-empty frontend source commit | Lead candidate：system identity Playwright、manifest-only/权限/截图/网络断言 | frontend result 2 / G-40B |
| T-08 | backend result 5；frontend result 2；parent baseline at G-00 | backend `specdev-worktree/.../T-08` / `speculo/.../T-08`；parent docs current Lead-only | SQL prefix/static/migration tests；parent doc review | clean non-empty backend source commit；Lead parent docs commit | Lead backend candidate + isolated MySQL；最终跨端/E2E；父 gitlink/doc check | backend final + parent final result |

`ticket_workspace_policy: required`：每张 Ticket 在其主产品 repository 只有一个 source worktree；source worktree 不运行或声明 E2E。Lead 在最新子模块 `main` 的独立 parent-candidate checkout 组合 source checkpoint，验证通过且父 HEAD 未漂移后才推进对应 `main`。成功集成不自动删除 source branch/worktree。

执行硬约束：source worktree 不运行 E2E；所有 required E2E 仅由 Lead 在 parent-candidate 环境执行并记录。

T-08 跨 backend 与 workspace-parent：backend 源码/DML/test 在唯一 T-08 source worktree；`docs/upstream/customization-map.md`、Speculo 和最终 gitlink 由 Lead 在父仓库 current workspace 处理，不授予 implementation owner 第二个 repository 写身份。父仓库提交前必须重读现有 dirty paths，只暂存本 change 已授权路径；若前序 gitlink 尚未独立确认或发生重叠，暂停父结果，不覆盖或吸收未知改动。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | authorized | 仅 T-08 Lead-owned 父文档、Evidence、状态和 gitlink；由完成 Goal Plan 指令授权 |
| Ticket worktree local changes | authorized | 仅本 change 的 required source worktree；由完成 Goal Plan 指令授权 |
| Implementation commit | authorized | 八张 Ticket 的本地实现提交；由完成 Goal Plan 指令授权并已完成 |
| Local direct-parent verification and parent update | not-authorized | 不适用：当前计划固定 required/candidate-merge |
| Local candidate integration and parent update | authorized | Lead-only；由完成 Goal Plan 指令授权并已完成 |
| Push / PR / remote merge | not-authorized | 不从本地实施授权继承 |
| Branch/worktree cleanup | authorized / completed | 用户已明确授权全面清理；T-01 至 T-08 source/candidate worktree 与对应本地分支均已移除 |
| Deploy / production DML / role grant | not-authorized | T-08 只编写并在隔离环境演练；逐动作另行批准 |

### Evidence Return

implementation owner 只返回 Ticket ID、repository/workspace locator、source commit、dirty 状态、实际路径、非 E2E 命令/结果、失败/未运行项和恢复条件。Lead 独立核对后，执行 candidate integration、required E2E、双轴审查和父 HEAD 重读，唯一写入 `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-NN.md</Path>`、Tickets Map 与 change 状态。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 服务端是密码策略、生成、Client 准入、临时消费和授权失效的唯一权威；前端规则和权限指令只改善交互。
- `sys_config` v1 policy、稳定 violation 顺序、公开最小投影和 last-known-good 失败关闭合同不分叉。
- fixedValue、generator internals、提交密码、临时 hash、Token 和 LoginUser 正文不得进入普通日志、审计、缓存消息、测试输出或 Evidence；永久 ADR-0017 例外不得扩大。
- 临时密码 user-scoped、60 秒、重新签发覆盖、成功后单次 CAS；永久密码优先且不得消费临时值；成功会话无标记、无额外限制。
- OAuth `clientId` 字符串与 Long `clientPk` 分离；角色权限按所属 Client 全部注销，用户角色按 user+Client 注销，其他 Client 零影响。
- 永久 reset 不踢既有会话；存量弱密码登录不按新策略拒绝，下一次写入才收敛。
- OpenAPI 生成物、NAMEWTA append-only DML、common cache/error 和长期 customization map 均保持唯一 owner；业务页面与按钮权限只经 web-domain manifest 进入动态导航，T-06 保留的 register/profile App 静态页不扩展为业务路由清单。

### Verification Integrity

本次刷新实测仓库基线：workspace-parent `main@21eb032336592460321d6286f2cce71e216d5aca`，存在本 change、其他 Speculo/归档状态与 backend gitlink 等既有 dirty 内容；frontend `main@efb8e0d7fae86cfd09c1f55204e8b486a499a3cc` clean，父仓库 gitlink 已记录同一 SHA；backend `main@f88012eae14374bb6f59b0389e3e937345ccde7e` clean，但父仓库仍记录 `e8bbdf0d9bfd608828cfd8977d524d178e50c7d4`。这些 SHA 只是 2026-08-28 14:39 的规划观测，不是执行期 base。G-00 必须重新读取并冻结实际 predecessor，不能覆盖现有 worktree 或把 backend gitlink 漂移、其他 change 的 Speculo 变化默认为本 change 产出。

source-worktree 只运行 Ticket 非 E2E checks；parent-candidate 才是 Redis/MySQL、跨 JVM、浏览器和组合状态判卷接缝。真实服务用例必须记录 executed/skipped 数；skipped 不构成通过。不得删测试、降低断言、放宽 tsconfig/架构/安全规则、手改 OpenAPI 生成物、改写 DML 历史或把 package 等同 test passed。

### Migration or Release Sequence

本地实现顺序固定为 cache prefactor -> policy contract -> authorization invalidation/backend features -> OpenAPI/public UI -> Admin UI -> migration/final contracts。实际发布属于未授权外部动作，若未来批准，顺序固定为：冻结旧弱写入口 -> 应用 legacy 安全值与新 DML -> 部署全部 backend 节点 -> 验证策略/cache/listener 健康 -> 部署 frontend -> 独立授予 `system:user:temporaryPassword` -> 观察登录失败、签发/CAS 和 session invalidation 指标。混合集群期间不得开放临时签发或宣称跨 JVM 立即失效。

### Risks, Monitoring and Recovery

- **跨 JVM 失效不完整：** T-01 先做双实例红测；G-10 不允许 TTL 代替确认。失败时不启用 T-02/T-03 新合同。
- **临时凭据竞争或失败计数漂移：** T-05 用真实 Redis 双并发和永久/临时组合矩阵；失败保留 source，backend `main` 不动。
- **Client 范围过宽/过窄：** T-02 覆盖 role Client、user+Client、默认角色、其他 Client 零注销；任何标识混用阻塞 G-30。
- **敏感值泄漏：** 每 Gate 检查 HTTP cache header、`@Log` 排除、普通日志/审计/Redis payload/UI 状态；发现泄漏按 high-assurance blocker 处理。
- **OpenAPI/前后端漂移：** T-06 只从 G-30 backend result 生成；provenance 不匹配即拒绝 candidate。
- **DML 或滚动窗口错误：** T-08 在隔离 MySQL 演练 fresh/upgrade/重复/补偿；生产未批准且不在本计划执行。
- **父 HEAD 或并行 change 漂移：** candidate 标记 stale，从最新 `main` 重建并重跑；父仓库 gitlink 前序不清晰时暂停 G-50。
- **尝试上限：** 单 Ticket candidate 最多 7 次；继续修正无合理收益、需要新产品决定或超限时转 blocked，不推进父分支。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。路径越界、shared owner 冲突、公共合同变化、验证接缝失效、新安全/数据风险或发布顺序变化立即暂停受影响 Wave；local 偏差记 Evidence，ticket/spec/architecture/release 偏差返回对应权威工件与批准人。

## 6. Progress and Decisions

### Current Status

- Spec、8 张 Deep Ticket 与 Tickets Map 均已完成；`AC-001` 至 `AC-024` 全部由实际代码、自动测试或隔离环境演练闭合，无 deferred。
- 八张 Ticket 均在 required source worktree 形成非空提交，经 clean candidate 验证后进入对应子模块 `main`；backend result 为 `42e06c0f713e0d724813800505e5bb5b40ab563b`，frontend result 为 `8aa184b353c5a37ee555feb8be808fe9ba885297`。
- G-10 至 G-30 的真实 Redis/MySQL、双 JVM、多 Client、并发 CAS 和 HTTP 流程通过；G-40 的 OpenAPI、架构、单测、构建与浏览器场景通过；G-50 的 MySQL fresh/upgrade/重复/冲突/补偿及最终 48 项 Playwright 通过。
- NAMEWTA DML 历史 15,370 字节前缀 SHA-256 保持 `637d6ab8aa4a813536597d2e2e49ac98f0a932284325a6023a77d09f8b86d8e6`；长期定制地图已同步策略、临时凭据、Client 精确失效与发布/回滚边界。
- 父仓只组合本 change Speculo 工件、长期定制文档和两个已验证 gitlink；工作区既有其他暂存、未暂存和子仓 dirty 内容均保留。

### Pending Decisions and Blockers

- 无实现或本地集成 blocker。
- source branch/worktree cleanup 已单独授权并完成；push/PR、部署、生产 DML 和角色授权仍未授权。

### Resume Protocol

若后续执行发布或生产迁移，先读取 T-08 Evidence、NAMEWTA SQL README 与长期定制地图，重新冻结部署 SHA、数据库备份和角色授权范围；严格按 DML -> backend -> frontend -> 独立授权顺序执行。任何 preflight、缓存刷新或健康检查失败都停止发布并按已演练补偿恢复，绝不改写用户密码。

## Assumptions

- 用户“根据 Goal Plan 完成 change 的所有功能需求”指令构成本 change 的实现、implementation commit、本地 candidate integration 与父分支更新授权；后续“全面清理删除干净”指令独立授权并完成 source/candidate worktree 与本地分支清理，但不扩展到远程或生产动作。
- implementation owner/provider 在每张 Ticket preflight 时动态选择，不改变已锁定路径、Gate、E2E owner 或 Lead 责任。
- G-00 关闭时两个子模块 `main` 可定位到明确、clean 的 predecessor；若另一个 active change 仍在推进，Lead 等待其不可变 result 或把冲突升级为 ticket/deviation，不猜测基线。
