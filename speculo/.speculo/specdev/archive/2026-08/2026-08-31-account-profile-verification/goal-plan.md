---
schema_version: 6
artifact: goal-plan
change: 2026-08-31-account-profile-verification
status: completed
modes: [migration, high-assurance, release-coordination]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: null
ticket_workspace_policy: required
integration_gate: candidate-merge
ready_for_execution: false
---

# Goal Plan: 账户个人与企业档案及实名认证闭环

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在后端交付独立 `ruoyi-profile` 聚合/BOM/个人/企业模块、不可变认证来源与绑定历史、两套 workflow、材料目录、供应商 SPI、管理员覆盖及跨模块只读服务；在 `admin-web` 交付个人档案、企业档案和材料标签三个管理面。最终系统在 `bundle-core` 与 `bundle-full` 均可装配，44 条验收合同都有可复查 Evidence，且认证只建立档案绑定，不改变 system RBAC、Client 或登录域。

### Success and False Completion

成功要求 14 个非取消 Ticket 都有 source commit、通过的 parent-candidate 验证、父分支 result SHA 和 Lead 写入的 Evidence；数据库、权限、workflow、OSS、Redis、通知、前端动态路由和真实 E2E Gate 全部闭合。

以下均属于伪完成：只生成表/实体或页面壳；只跑 mock/单元测试却声称跨边界通过；在 source worktree 声称 E2E；待审直接污染 current；以物理删除清理历史；有页面权限但后端无授权；产生 source commit 但未通过 candidate-merge；只推进一个子仓库而未记录另一个仓库的验证基线。

### Non-goals

- 不交付申请人 Web/移动端页面，不改变注册、登录、角色、菜单授予、Client 和登录域。
- 不接入实际支付宝/微信/微警生产供应商，不支持中国大陆及港澳台之外签发的证件。
- 不提供批量导出、物理删除、已注销档案恢复、企业多负责人或成员权限。
- 不执行 push、PR、远程合并、生产 DDL/DML、部署、生产权限赋予或来源 worktree 清理。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | 产品取舍、worktree 与授权 | 更新真正拥有该决策的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | 27 项架构决定与领域语义 | 返回 Grill 更新真正 owner |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 永久工程决定 | 当前 change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为、范围、AC-001 至 AC-044 | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 局部合同与路径 | Goal Plan 只编排 |
| 6 | 当前源码、POM、配置、测试与 Git 实测 | 可行性和基线 | 冲突触发 deviation 并返回真正 owner |
| 7 | CDE person/enterprise 参考实现 | 领域概念参考 | 不覆盖当前模块、API、workflow、OSS 与权限边界 |

基线快照：聚合仓库 `main@41d2a30cb4b9ea24a00d870d319fd4ad59f2ebf3`；后端 `main@c13a375f649cba176ba004d2f45ab2907c0f3574`；前端 `main@4b204f65a822bf080d71d9c90ed430e9467bcf16`。两个产品子仓库在规划时 clean；聚合仓库存在用户其他改动，Lead 不覆盖。

## 2. Execution Graph

### DAG and Critical Path

```text
G0 authorization/workspace
  │
W1  T-01 ─┐
    T-02 ─┴─ G1 foundation
             ├─ W2 T-03 ─┐
             └─ W2 T-04 ─┴─ G2 evidence foundation
                              ├─ W3 T-05 ─┐
                              └─ W3 T-07 ─┴─ G3 certification verticals
                                           ├─ W4 T-06 ─┐
                                           ├─ W4 T-08 ─┼─ G4 ownership/admin
                                           └─ W4 T-09 ─┘
                                                        │
                                                W5 T-10 ─ G5 frontend contracts
                                                        │
                                           ┌─ W6 T-11 ─┤
                                           ├─ W6 T-12 ─┼─ G6 admin surfaces
                                           └─ W6 T-13 ─┘
                                                        │
                                                W7 T-14 ─ G7 release readiness
```

关键路径为 `T-01/T-02 -> T-03/T-04 -> T-05/T-07 -> T-06/T-08/T-09 -> T-10 -> T-11/T-12/T-13 -> T-14`。W1、W2、W3、W4、W6 的 source 实现可按依赖并行；candidate integration 始终由 Lead 按表中序号串行。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | G0；后端基线固定 | backend POM、profile/workflow API、profile contract test | T-01 | G1 / 1 |
| W1 | T-02 | G0；后端与父仓库基线固定 | 父仓库 50/60 完整基座、backend schema test | T-02 | G1 / 2 |
| W2 | T-03 | G1 | person/enterprise verification 子树 | — | G2 / 3 |
| W2 | T-04 | G1 | shared material 与 enterprise material 子树 | — | G2 / 4 |
| W3 | T-05 | G2 | person application/persistence 子树 | — | G3 / 5 |
| W3 | T-07 | G2 | enterprise application/persistence 子树 | — | G3 / 6 |
| W4 | T-06 | T-05 result | person rebind/notification 子树 | — | G4 / 7 |
| W4 | T-08 | T-05、T-07 result | enterprise transfer 子树 | — | G4 / 8 |
| W4 | T-09 | T-01、T-04、T-05、T-07 result | person/enterprise admin 子树 | — | G4 / 9 |
| W5 | T-10 | G4 | profile domain、web-domain 根、admin package、lock | T-10 | G5 / 10 |
| W6 | T-11 | G5 | material-tag 页面子树 | — | G6 / 11 |
| W6 | T-12 | G5 | person 页面子树 | — | G6 / 12 |
| W6 | T-13 | G5 | enterprise 页面子树 | — | G6 / 13 |
| W7 | T-14 | T-02 与 G6 | admin services/manifest、profile E2E | T-14 | G7 / 14 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 模块/API/workflow 接缝 | — | backend `specdev-worktree/{change}/T-01` | Lead / dynamic dispatch | not-required: 下游验证跨边界 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-02 | schema/约束/种子 | — | backend source + 父仓库 SQL owner | Lead / dynamic dispatch | required: fresh MySQL 六文件基座 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| T-03 | provider SPI/回调 | T-01,T-02 | backend `specdev-worktree/{change}/T-03` | Lead / dynamic dispatch | required: HTTP+DB+鉴权 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| T-04 | 材料树/OSS 生命周期 | T-01,T-02 | backend `specdev-worktree/{change}/T-04` | Lead / dynamic dispatch | required: MySQL+OSS+ACL | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| T-05 | 个人申请/流程发布 | T-03,T-04 | backend `specdev-worktree/{change}/T-05` | Lead / dynamic dispatch | required: HTTP+DB+workflow | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| T-06 | 个人换绑/解绑/通知 | T-05 | backend `specdev-worktree/{change}/T-06` | Lead / dynamic dispatch | required: workflow+DB+notify | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| T-07 | 企业申请/负责人发布 | T-03,T-04 | backend `specdev-worktree/{change}/T-07` | Lead / dynamic dispatch | required: HTTP+DB+workflow | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| T-08 | 企业转移挑战 | T-05,T-07 | backend `specdev-worktree/{change}/T-08` | Lead / dynamic dispatch | required: Redis+notify+DB | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| T-09 | 管理查询/覆盖命令 | T-01,T-04,T-05,T-07 | backend `specdev-worktree/{change}/T-09` | Lead / dynamic dispatch | required: auth+workflow+DB+OSS | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| T-10 | 前端公共合同 | T-04,T-05,T-06,T-07,T-08,T-09 | frontend `specdev-worktree/{change}/T-10` | Lead / dynamic dispatch | not-required: T-14 统一 E2E | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-10.md</Path>` |
| T-11 | 材料标签管理页 | T-10 | frontend `specdev-worktree/{change}/T-11` | Lead / dynamic dispatch | not-required: 组件测试，T-14 E2E | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| T-12 | 个人档案管理页 | T-10 | frontend `specdev-worktree/{change}/T-12` | Lead / dynamic dispatch | not-required: 组件测试，T-14 E2E | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| T-13 | 企业档案管理页 | T-10 | frontend `specdev-worktree/{change}/T-13` | Lead / dynamic dispatch | not-required: 组件测试，T-14 E2E | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-13.md</Path>` |
| T-14 | App 组合与发布 Gate | T-02,T-11,T-12,T-13 | frontend `specdev-worktree/{change}/T-14` | Lead / dynamic dispatch | required: 全栈管理闭环 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-14.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- AC-001 至 AC-044 全部为 passed Evidence，无未经批准的 deferred。
- 每个非取消 Ticket 的 source worktree clean，source checkpoint 可达；candidate/result SHA 与产品子仓库父分支实际状态一致。
- shared owner、公共 API、父仓库 50/60 基座、domain exports、App manifest 和 lock 无竞争写入；所有消费者基于已集成 checkpoint。
- 后端定向测试、受影响 reactor/build、前端 lint/typecheck/test/build 和 required E2E 无回归。
- 无业务 DELETE/OSS 删除、敏感日志、未授权数据读取、伪 workflow 成功、双决定或 system RBAC 副作用。
- SpecDev Ticket/Map/Plan/Evidence/status 与 Git 一致，无活动 candidate、未集成 source 或高影响未决偏差。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 执行授权 | 上游 ready、workspace=required、基线 clean | implementation commit 与 local candidate integration 均 authorized | 全部 Ticket | Lead / 用户 | 保持 blocked，不创建 worktree |
| G1 合同与数据地基 | G0 | T-01/T-02 result、reactor/API、fresh 六文件基座/唯一约束 Evidence | W2+ | Lead | 保留失败 source/candidate，父分支不动 |
| G2 验证证据地基 | G1 | provider 安全矩阵、材料树/OSS ACL/lifecycle E2E | W3+ | Lead | 修正 T-03/T-04，不扩大 SPI/材料合同 |
| G3 两类认证垂直路径 | G2 | 个人/企业提交、快照、流程终态、并发、发布 E2E | W4+ | Lead | 回到失败叶子，旧 current/绑定保持不变 |
| G4 所有权与运营闭环 | G3 | 换绑/通知、转移挑战、override/decision fence、权限矩阵 E2E | W5+ | Lead | 暂停消费者，保留权威绑定/决定版本 |
| G5 前端公共合同 | G4 | profile transport/manifest/exports、typecheck、package graph | W6+ | Lead | 只修 T-10 shared owner，再刷新下游基线 |
| G6 三管理面 | G5 | 标签/个人/企业组件和状态/权限矩阵通过 | W7 | Lead | 单页回退对应 Ticket，不改公共入口 |
| G7 发布就绪 | G6 且 T-02 result | core/full、全栈 Playwright、auth/OSS/workflow/system/log/SQL 回归及迁移演练 | change 完成/发布 | Lead；生产另需人工批准 | 不推进前端父分支；已集成后端只前向修复，生产不动作 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001 至 AC-008 | T-02,T-05,T-07,T-14 | application/persistence/workflow/MySQL | T-02,T-05,T-07,T-14 | passed |
| AC-009 至 AC-017 | T-05,T-06,T-10,T-12 | privacy/rebind/transaction/notify/UI | T-05,T-06,T-10,T-12 | passed |
| AC-018 至 AC-023 | T-07,T-08,T-13 | ownership/Redis/transfer/UI | T-07,T-08,T-13 | passed |
| AC-024 至 AC-031 | T-01,T-02,T-09,T-10,T-12,T-13,T-14 | auth/workflow/admin/version/UI/E2E | 对应 Ticket | passed |
| AC-032 至 AC-035 | T-02,T-04,T-10,T-11,T-14 | material tree/OSS/UI/E2E | 对应 Ticket | passed |
| AC-036 至 AC-038 | T-01,T-03,T-05,T-07,T-10,T-14 | provider/ProfileService/API | 对应 Ticket | passed |
| AC-039 至 AC-044 | T-01,T-02,T-04,T-09,T-10-T-14 | auth/UI/history/log/system | 对应 Ticket | passed |
| ADR-001 至 ADR-027（排除 superseded ADR-010/021） | T-01 至 T-14 | Ticket 验证矩阵与 Gate | 全部 Evidence | passed |
| CDE 参考 | T-01,T-02,T-05,T-07 | 概念映射，不做逐文件一致性 | T-01/T-02/T-05/T-07 | reference-only |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence、E2E 与父分支 owner |
| Implementation subagents | 最多 3，Lead 不计入 | config=3，平台可用 implementation 槽=3 |
| Integration attempts | `null`（unlimited） | config 快照；仍受停止条件与偏差控制 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，只读且不争用可变环境 |
| Dispatch | execution-time dynamic | 当前协作约束下未获用户明确委派时由 Lead 实现；不预分配 provider/模型 |

`subagent-delivery operation=plan` 合同：允许 `implementation`、`review`、`research`、`test-observation`；implementation 仅写 Ticket source worktree 的 writable/shared-owner 路径并创建已授权 source commit；所有 subagent 禁止写 SpecDev 工件、Evidence、父分支和 E2E 结论；Lead 独立核对 checkpoint、diff、检查与 dirty 状态。每次真实派单必须另建不可变 Dispatch Packet；本计划不构成外部 provider 或数据发送授权。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend `main@6927ee978...` | `{change}/T-01` | Maven/API contract passed | `b62d64b5511847f265c63b21d02117ee21630453` | candidate/reactor passed，E2E not-required | `672268b2fa18b4cdd0d6ee19be1cb1615252e040` |
| T-02 | backend `672268b2f` + root `b9685ae` | backend `{change}/T-02-v2` + root `{change}/T-02-root-v2` | schema static + MySQL passed | backend `b57e25d14` / root `bc812e2` | fresh MySQL 8.4.9 required/passed | backend `b57e25d14` / root `bc812e2` |
| T-03 | backend `b57e25d14` | `{change}/T-03` | 19 provider unit/security tests passed | `8b8353491` | callback+DB required/passed | `4bc543c01` |
| T-04 | backend `4bc543c01` | `{change}/T-04` | 12 material/OSS contract tests passed | `c9691c9a8` | MySQL+OSS+ACL required/passed | `7eb550b90` |
| T-05 | backend `7eb550b90` | `{change}/T-05` | 43 person/material tests passed | `44f83197d` | HTTP+MySQL+ProcessEvent required/passed | `8393756b0` |
| T-07 | backend `8393756b0` | `{change}/T-07` | 31 enterprise tests passed | `1ad938d67` | HTTP+DB+workflow required/passed | `f41c792bc` |
| T-06 | backend `f41c792bc` | `{change}/T-06` | 59 rebind/notify tests passed | `10edd3102` | workflow+DB+notify required/passed | `d07a94635` |
| T-08 | backend `d07a94635` | `{change}/T-08` | 43 enterprise/transfer tests passed | `01ba4b479` | Redis+notify+DB required/passed | `7635e962b` |
| T-09 | backend `7635e962b` | `{change}/T-09` | person 62 + enterprise 46 tests passed | `a850f288d` | workflow+DB+OSS required/passed | `12192f90a` |
| T-10 | frontend `a2d79cf7b` | `{change}/T-10` | 13 contract + 100 architecture tests passed | `f6c4d3329` | package/admin integration passed，E2E not-required | `1162bf650` |
| T-11 | frontend `1162bf650` | `{change}/T-11` | 7 web-domain tests + lint/typecheck passed | `333d9e0c5` | web-domain integration passed，E2E not-required | `f19ad1c48` |
| T-12 | frontend `f19ad1c48` | `{change}/T-12` | 15 component/logic tests + lint/typecheck passed | `f501b33d2` | candidate/admin integration passed，E2E not-required | `ee35471d1` |
| T-13 | frontend `1162bf650` | `{change}/T-13` | 11 focused tests + lint/typecheck passed | `966b2c558` | 23 combined tests + admin integration passed，E2E not-required | `ae4e2b404` |
| T-14 | backend `12192f90a` + frontend `ae4e2b404` | backend `{change}/T-14-backend` + frontend `{change}/T-14` | backend 351 tests；frontend lint/test/typecheck | backend `dc3ec6ae9` / frontend `987395479` | MySQL/Redis/schema/core/full/Playwright required/passed | backend `e2a73d5c1` / frontend `e47c558af` |

表内 workspace 均展开为 `specdev-worktree/2026-08-31-account-profile-verification/T-XX`；branch 使用同名逻辑标识。创建时必须从对应产品子仓库最新已通过父结果固定精确 `base_sha`，不得沿用本表缩写。

source worktree 不运行 E2E，也不得声明 E2E 通过。Lead 在 `specdev-worktree/.integration/2026-08-31-account-profile-verification/T-XX` 组合最新父分支与 source checkpoint，按集成序号运行检查及适用 E2E；父 HEAD 漂移则 candidate=`stale` 并重建。通过且父 HEAD 未变后才能 fast-forward/merge-commit，写入 `result_sha` 并开放下游。成功不自动清理 source branch/worktree。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | not-authorized | 本计划为 required，不允许在当前产品 workspace 实现 |
| Ticket worktree local changes | allowed | 用户于 2026-08-31 明确“开启，可 implement”；仅 Ticket writable/shared owner |
| Implementation commit | allowed | 用户明确“可 commit”；每 Ticket source commit 必需 |
| Local direct-parent verification/update | not-authorized | required 模式不使用 |
| Local candidate integration and parent update | allowed | 用户于 2026-09-01 明确确认；限 Lead-owned candidate 验证及通过后推进本地父分支 |
| Push / PR / remote merge | not-authorized | 不从本地实现授权继承 |
| Branch/worktree cleanup | not-authorized | 成功集成也不自动清理 |
| Deploy / migration / production actions | not-authorized | DDL/DML 仅测试环境演练；生产逐动作批准 |

### Evidence Return

implementation 返回 Ticket、仓库/workspace、base/source commit、clean 状态、实际路径、非 E2E 命令结果、未运行项和阻塞。Lead 重读并核对后才写 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-XX.md</Path>`；candidate 的 parent-before/source/candidate/tree/result SHA、方法、冲突、集成检查与 E2E 均由 Lead 记录。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 每类账户最多一个 active/suspended 绑定；个人主体和企业主体各自最多一个有效账户/负责人，个人企业可同时存在。
- 工作副本、不可变提交/管理员来源、档案版本、绑定事件、材料引用、provider attempt 和审计分离；无物理删除。
- 两套精确 flowCode；缺 workflow 时查询/草稿可用、提交失败关闭；ADMIN_OVERRIDE 以 decisionVersion 围栏终止竞态。
- 普通探测不回显主体；个人完整匹配才可返回旧 system 手机掩码；企业普通申请不能顶替负责人。
- 明文存储与有权明文查看不等于日志授权；敏感请求、错误、通知和供应商原文不得进入持久日志。
- 后端授权权威、能力闭合且无部门数据范围；前端只是投影。
- profile 只使用 ruoyi-api/common 公开能力，不依赖 system/workflow 内部 mapper/entity/service。

### Verification Integrity

source 测试只证明局部实现；required E2E 只在 Lead-owned parent-candidate 有效。禁止以 mock 替代 MySQL 唯一约束、workflow 终止/事件、Redis 时钟、OSS ACL、通知故障或动态菜单真实解析；禁止删除/降低断言、跳过失败模块、只测 happy path 或用截图代替数据不变量。T-14 必须读取后端与前端精确 result SHA，避免跨仓库漂移。

### Migration or Release Sequence

1. 测试环境按 `10 -> 20 -> 30 -> 40 -> 50 -> 60` fresh 执行完整基座，验证表、约束和索引。
2. 验证 60 基座中的字典、流程/配置、菜单权限和 systemRequired 材料标签稳定 code；已有环境另按源/目标 Git Tag 差异演练。
3. 部署 backend `bundle-core`，验证无 workflow 的启动、查询/草稿与提交失败关闭。
4. 部署 backend `bundle-full`，发布并验证个人/企业两套流程、OSS/Redis/通知。
5. 部署前端 profile 包与 admin App 组合；菜单默认不赋权。
6. 测试角色按完整能力授权，运行 G7 全量验证和观察。
7. 生产迁移、权限赋予、发布与观察需另行批准；无批准不得执行。

### Risks, Monitoring and Recovery

- **明文身份风险：** 监控 profile 明文/材料访问、403、异常下载与日志扫描；入口可撤权，已落库数据不能靠回滚清除。
- **并发与双决定：** 监控唯一约束冲突、decisionVersion、迟到 workflow、换绑/转移版本冲突；失败保持旧 current/绑定并前向修正。
- **外部依赖：** 监控 workflow/provider/OSS/Redis/notify 失败；供应商证据不自动发布，通知失败不回滚已提交绑定。
- **迁移与前端注册：** 监控 DDL/DML 校验、未知 componentKey、路由解析与 5xx；先撤菜单/组合，再回退服务，历史数据不删除。
- **candidate 失败：** 父分支不动，保留 source worktree/candidate 事实；修正同一 Ticket 后生成新 source checkpoint。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。局部实现偏差记 Evidence；路径/局部合同返回 Ticket；外部行为返回 Spec；架构返回 Grill/ADR；迁移、Gate 或生产动作变化为 release 偏差并暂停受影响 Wave。任何执行者不得先扩大路径再补批准。

## 6. Progress and Decisions

### Current Status

- T-tickets 已完成：14/14 ready，AC-001 至 AC-044 covered，validator `0 error(s), 0 warning(s)`。
- Goal Plan 输入、DAG、路径、Git 基线和工作区策略已固定；G0 已关闭。
- 用户已选择 required worktree，并授权 Ticket implementation changes 与 implementation commits。
- 用户已授权 Lead-owned local candidate integration 与验证通过后的本地父分支推进。
- T-01 已完成并集成：source `b62d64b5511847f265c63b21d02117ee21630453`，candidate/result `672268b2fa18b4cdd0d6ee19be1cb1615252e040`；合同测试、full/core 干净构建与 JAR 装配核验通过。
- T-02 已完成并集成：旧 checkpoint 因 SQL owner 迁移仅保留审计；backend result `b57e25d14273ab33cd09052544d50d936866d5ce`，root SQL result `bc812e21fd268874436d2726887671768b4479d7`。授权开发服务器 MySQL 8.4.9 六文件 fresh 初始化得到 111 表，Java 约束测试 6/6 通过，临时库/用户已清理且业务库保持 87 表。
- G1 已关闭；T-03/T-04 可从 backend `b57e25d14273ab33cd09052544d50d936866d5ce` 开始。
- T-03 已完成并集成：source `8b83534917e8a235f00879f05687bfc674e303c6`，candidate/result `4bc543c01e190f92468e05e8b99f8d8d55d6cade`；19 个 provider 单元/安全合同测试和 person/enterprise 真实 MySQL callback E2E 通过，隔离库/用户清理后业务库仍为 87 表。
- T-03 执行发现叶子 POM 缺少测试依赖；已记录局部路径偏差，两个叶子 POM 仅增加 test-scope 测试与 MySQL 驱动依赖，运行时依赖方向未变化。
- T-04 已完成并集成：source tip `c9691c9a82cfdda845046fcaaace7f32c112b064`，candidate/result `7eb550b9041603aa8317913574c70ec8140bb244`；12 个材料/OSS 合同测试和 1 个真实 MySQL 材料生命周期 E2E 通过，隔离库/用户清理后业务库仍为 87 表。
- T-04 通过兼容 default method 扩展 `OssService` 的只读元数据合同；旧方法、OSS URL/删除合同、schema 和运行时依赖均未改变。
- G2 已关闭；T-05/T-07 可从 backend `7eb550b9041603aa8317913574c70ec8140bb244` 开始。
- T-05 已完成并集成：source tip `44f83197d519ce1d86ea1096f163e3b106dd298b`，candidate/result `8393756b0973ccecc4bf73a6669c0c1d6c0b0b1e`；43 个 person/material 测试和 5 个真实 MySQL HTTP/ProcessEvent/事务发布 E2E 通过，full/core bundle 与 JAR 装配核验通过。
- T-05 发现材料发布接缝缺口并按偏差控制接管最小共享实现：补齐工作副本状态写保护与 `SUBMISSION -> VERSION` 关系约束复制，不改变公开端口、schema 或 OSS 生命周期。
- T-05 的完整 Warm-Flow 引擎/流程定义启动链保留给 T-14 全栈 Gate；本 Ticket 已覆盖生产 WorkflowService gateway 合同、持久化 instanceVariable fence，以及生产 ProcessEvent 监听与真实 MySQL 原子发布。
- T-07 已完成并集成：source `1ad938d67d634b6ea880c40d85621333c8209fc0`，candidate/result `f41c792bcb3782ece300d256317825fd1a7a8c87`；31 个 enterprise 测试和 3 个真实 MySQL HTTP/ProcessEvent/事务/唯一竞态 E2E 通过，full/core bundle 与 JAR 装配核验通过。
- T-07 发现 Spec 已锁定的企业邮箱、注册资本、行业、网站未进入 T-02 schema；已按偏差控制把 `50-namewta-ddl.sql` 纳入最小可写范围，为五张企业事实表增加 nullable 列并完成申请到版本全链持久化，DML 和已有约束不变。
- G3 已关闭；T-06、T-08、T-09 可进入 W4，后端父结果为 `f41c792bcb3782ece300d256317825fd1a7a8c87`。
- T-06 已完成并集成：source `10edd3102416722a3a7c2c8e08b4eec88ae1a005`，candidate/result `d07a946353477f8936f343c1ef71b321b598d285`；59 个 rebind/notify 测试和 2 个真实 MySQL HTTP/ProcessEvent/原子换绑/通知失败 E2E 通过，core bundle 与 JAR 装配核验通过。
- T-06 复核补齐安全 POST 审计、动态数据源事务提交后通知和事务内持久 `PENDING` 通知待办；通知失败不回滚绑定且保留可重试记录，schema、system 用户合同和业务历史均未改变。
- T-08 source worktree 已从 backend `d07a946353477f8936f343c1ef71b321b598d285` 创建，状态为 active；实现保持在 enterprise transfer 独立子树。
- T-08 已完成并集成：source `01ba4b479324420b7f0f9dfb635a9c5f45b2ad81`，candidate/result `7635e962bfb12e3718c52741ea05433ca3d98b06`；43 个 enterprise/transfer 测试和 4 个真实 MySQL/Redis/notify/current-user E2E 通过，core bundle 与 JAR 装配核验通过，临时资源残留为零。
- T-08 集成顺序偏差已在 Evidence 如实记录：父 merge commit 因当前目录错误先创建，候选随后快进到完全相同 SHA/tree 并在候选树完成全部 Gate；未 reset/revert，最终 result tree 无额外差异。
- T-09 source worktree 已从 backend `7635e962bfb12e3718c52741ea05433ca3d98b06` 创建，状态为 active；实现限定在 person/enterprise admin 独立子树。
- T-09 已完成并集成：backend source/result `a850f288d` / `12192f90a`；person 62 + enterprise 46 测试及 workflow/MySQL/OSS 管理闭环通过。
- T-10 已完成并集成：frontend source/result `f6c4d3329` / `1162bf650`；13 项 profile 合同与 100 项架构测试通过。
- T-11 已完成并集成：frontend source/result `333d9e0c5` / `f19ad1c48`；材料标签树 7 项测试与 admin 类型检查通过。
- T-12 已完成并集成：frontend source/result `f501b33d2` / `ee35471d1`；个人档案 15 项逻辑/组件合同、profile lint/typecheck、架构与 admin 类型检查通过。
- T-13 已完成并集成：frontend source/result `966b2c558` / `ae4e2b404`；企业主体/负责人分离、不可变来源和管理操作 11 项聚焦测试，三管理面组合 23 项测试与 admin 类型检查通过。
- T-14 已完成并集成：backend source/result `dc3ec6ae9` / `e2a73d5c1`，frontend source/result `987395479` / `e47c558af`；真实 MySQL/Redis E2E、fresh 六文件 schema、351 项后端开发组测试、core/full JAR 装配、前端全量 lint/test/typecheck 与候选 Playwright 均通过。
- G7 已关闭；14/14 Ticket、AC-001 至 AC-044 和全部 required Gate 均有 Evidence。代码已达到本地发布候选状态，但 push、部署、生产迁移、权限赋予和 worktree 清理仍未授权。

### Pending Decisions and Blockers

- 当前无执行 blocker，Change 已完成。来源 worktree 清理、push/PR/远程合并、部署和生产迁移保持未授权。

### Resume Protocol

恢复时依次读取本 Goal Plan、对应 Ticket、`.status.json`、Tickets Map 和最新 Evidence；重读聚合/后端/前端仓库 branch、HEAD、worktree list 与 dirty 状态。从最小未完成集成序号创建/恢复 source worktree；父结果不匹配时标记 stale，不复用漂移 candidate。

## Assumptions

- 后端与前端 `main` 是本 change 的本地父分支；执行时每个 worktree 创建前重新实测，不假定规划 SHA 永久不变。
- 测试环境可提供隔离 MySQL、Redis、OSS、workflow 与通知替身/沙箱；若不可提供则相应 required E2E Gate 阻塞，不以 mock 降级。
- T-14 只写前端 App 组合/E2E，后端 assembly 由 T-01 所有并在 T-14 只读重跑；该安排消除了跨 Git 仓库单 Ticket 双 commit 歧义，不改变外部行为。
