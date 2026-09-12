---
schema_version: 6
artifact: goal-plan
change: 2026-08-31-optional-nacos-dynamic-config
status: completed
modes: [migration, high-assurance, reference-conformance, release-coordination]
orchestration: lead-directed
lead: codex:/root
implementation_agent_limit: 3
integration_attempt_limit: null
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: 可选 Nacos 动态配置

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-31-optional-nacos-dynamic-config/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在 NAMEWTA full/core 两种 ruoyi-admin bundle 中交付默认关闭、缺失可运行的 Nacos Config 稀疏覆盖层；只有 Captcha、通知幂等与 OSS 下载 TTL 三组配置即时生效，其余合法键等待重启。以固定官方 Nacos 2.5.4、独立 MySQL 权限和鉴权提供可选基础设施，并在系统管理中通过 `/nacos/` 同源 iframe 打开需要独立登录的官方控制台。最终以真实双实例、故障注入、持久化和浏览器 Gate 证明全部 24 个验收合同。

### Success and False Completion

成功要求六个非取消 Ticket 各有非空 implementation checkpoint、通过的 direct-parent 验证、聚合父分支 result SHA 和 Lead Evidence；后端/前端子仓库 checkpoint 与聚合 gitlink 一致；Maven full/core、前端 lint/typecheck/test/build、SQL/Compose、真实 MySQL/Nacos、双实例、Nginx/Playwright 和 secret scan 全部闭合。

以下属于伪完成：仅能从 Nacos 拉取文本却没有本地兜底；把所有 YAML 报告为已热更新；使用客户端磁盘 snapshot 伪装离线可用；只验证一个应用实例；控制台自动注入密码或绕过 Nacos 登录；使用浮动镜像；只跑 mock/static tests 却宣称鉴权、持久化或 iframe 通过；子仓库有 commit 但聚合 gitlink/父分支没有对应 result；把当前无关 dirty 变更混入 Ticket commit。

### Non-goals

- 不交付服务发现、Spring Cloud Bus/Alibaba、Nacos HA、自建 CRUD、SSO 或 AES 插件。
- 不承诺清单外配置即时生效，不删除或残缺化现有本地 YAML。
- 不执行 push、PR、远程合并、生产 DDL/DML、部署、角色授权、首次生产密码设置、卷删除或清理现有用户改动。
- 本 Goal Plan 不授权实现；只固化执行合同与解除 blocker 的条件。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | 产品取舍、workspace 与执行批准 | 更新真正拥有该决策的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | 12 项架构决定与领域词汇 | 返回 Grill 更新 ADR/LOG，不在计划重写 |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 已毕业的工程决定 | 当前 change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为、范围、AC-001 至 AC-024 | 下游不得降低或改写 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 路径、局部实现和验证 | Goal Plan 只做跨 Ticket 编排 |
| 6 | 当前源码、POM、配置、测试与 Git 实测 | 可行性、命令和执行基线 | 冲突触发 deviation 并返回真正 owner |
| 7 | CDE Nacos 参考实现与 Nacos 2.5 官方文档 | 客户端生命周期参考、镜像/auth/schema 合同 | 不复制 CDE 默认开启、addFirst、Spring Cloud rebinder 或 snapshot 行为 |

规划时实测：聚合仓库 `main@41d2a30cb4b9ea24a00d870d319fd4ad59f2ebf3`，其记录的 backend/frontend gitlink 分别为 `412c2bf1e394042aa841f719a0348b645a26680d` 与 `ea32aa1b1c9911e430f406631199e30589ba007b`；当前后端 clean `main@f6f3ef5e4f682c39aa8c372afd114303955693cd`，前端 clean `main@4b204f65a822bf080d71d9c90ed430e9467bcf16`。聚合仓库包含这两处 gitlink 漂移及大量与本 change 无关的用户修改，不能直接作为首个 Ticket 的干净提交边界。

## 2. Execution Graph

### DAG and Critical Path

```text
G0 execution authorization + clean current baseline
  │
W1  T-01 runtime contract ───────┐
    T-04 menu/console contract ──┴─ G1 contracts stable
                                   │
W2  T-02 safe live refresh ────────┤
    T-03 Docker infrastructure ─────┴─ G2 behavior + infrastructure
                                      │
W3  T-05 same-origin proxy ─────────── G3 browser path
                                      │
W4  T-06 release convergence ───────── G4 change complete
```

Ticket frontmatter 的真实依赖保持：T-02/T-03 只依赖 T-01，T-05 依赖 T-03/T-04，T-06 依赖 T-02/T-03/T-05。`current` workspace 不允许 Wave 内并发；固定串行顺序为 `T-01 -> T-04 -> T-02 -> T-03 -> T-05 -> T-06`。关键行为路径为 `T-01 -> T-03 -> T-05 -> T-06`，即时刷新路径 `T-01 -> T-02 -> T-06` 在最终 Gate 汇合。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | G0；backend/parent 基线固定 | backend common/POM/admin config/runtime tests | T-01 为 common runtime/POM 唯一 writer | G1 / 1 |
| W1 | T-04 | T-01 parent result；frontend/backend 基线重读 | frontend monitor/manifest/env + backend/release DML | T-04 为菜单/target/DML 唯一 writer | G1 / 2 |
| W2 | T-02 | G1；T-01 runtime contract 已在父结果 | backend 三组配置消费域 | — | G2 / 3 |
| W2 | T-03 | T-02 parent result；T-01 属性合同稳定 | release Compose/Nacos DB/init/env/static test | T-03 为 Nacos infrastructure 唯一 writer | G2 / 4 |
| W3 | T-05 | G2；T-03/T-04 Evidence | release LB/proxy tests + frontend Playwright | T-05 为 `/nacos/` LB 唯一 writer | G3 / 5 |
| W4 | T-06 | G3；T-02/T-03/T-05 results | release E2E harness/README | T-06 为 final gate/docs 唯一 writer | G4 / 6 |

表中 W1/W2 表示依赖层级，不是并发授权。每次只有表中最小未完成集成序号持有 current workspace implementation writer lock。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 可选稀疏覆盖、原子状态与逐实例观测 | — | `current` / parent `main` | Lead；后续明确允许时可动态派单 | not-required: 可控 SDK 接缝；真实服务在 T-03/T-06 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-04 | 系统菜单、权限与安全 external target | — | `current` / parent `main` | Lead；后续明确允许时可动态派单 | not-required: T-05 统一浏览器验证 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| T-02 | 三组配置在下一业务调用即时生效 | T-01 | `current` / parent `main` | Lead；后续明确允许时可动态派单 | not-required: Spring/模块行为测试，真实发布在 T-06 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| T-03 | 官方镜像、鉴权、独立 DB、持久化与健康门 | T-01 | `current` / parent `main` | Lead；后续明确允许时可动态派单 | required: MySQL/Nacos/auth/persistence | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| T-05 | `/nacos/` 同源 iframe 与官方独立登录 | T-03,T-04 | `current` / parent `main` | Lead；后续明确允许时可动态派单 | required: Nginx/Nacos/Playwright | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| T-06 | 双实例故障与全发布收敛门禁 | T-02,T-03,T-05 | `current` / parent `main` | Lead；后续明确允许时可动态派单 | required: 全栈最终 Gate | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- AC-001 至 AC-024 全部有 Lead 可复查的 passed Evidence，无未经批准的 deferred。
- 每个非取消 Ticket 在实际修改的每个 Git 仓库均有非空 implementation commit；聚合父分支 direct-parent result 包含正确子仓 gitlink 与 parent-owned 文件。
- T-01/T-02 的 Maven 定向测试与 full/core，T-04/T-05 的 frontend lint/typecheck/test/build，T-03/T-06 的 release/Compose/real E2E 全部通过。
- 配置优先级、两阶段原子接受、离线不读 snapshot、三组即时生效、清单外重启、双实例 digest 与所有失败语义成立。
- Nacos auth/DB/端口/明文风险、RuoYi/Nacos 双权限边界、同源 iframe 和输出不泄密均有正反证据。
- Ticket、Map、Plan、Evidence、change status、实际 branch/HEAD/dirty 状态一致；无未归属修改、未集成 child commit、未决 direct-parent 或测试容器残留。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 执行授权与基线 | 上游 ready；current/direct-parent 已选 | implementation commit 与 local direct-parent 均 authorized；聚合/子仓基线和现有用户改动归属明确；唯一 writer 可保证 | 全部 Ticket | Lead / 用户 | 保持 blocked，不改产品、不提交、不处理用户改动 |
| G1 运行时与入口合同 | G0 | T-01/T-04 result SHA；full/core 默认关闭；target/permission/menu/DML/frontend build Evidence | T-02+ | Lead；Deep 执行需用户批准 | 修正当前 Ticket，其他 Ticket 不开始 |
| G2 即时行为与基础设施 | G1 | T-02 三域原子行为；T-03 real MySQL/Nacos auth/persistence/health、base/override Compose Evidence | T-05+ | Lead | 停止在失败 owner；父结果不包含失败 commit |
| G3 同源浏览器路径 | G2 | T-05 HTTP/TLS config、same-origin iframe、官方登录、无资源/console 错误 Evidence | T-06 | Lead | 回到 T-05；Nacos 客户端可保持 enabled=false |
| G4 发布收敛 | G3 | T-06 全 24 AC、双实例/fault/restart/persistence、secret scan、full/core/frontend/release regression 与 final result SHA | change 完成/任何发布 | Lead；生产另需逐动作批准 | 不部署；按失败归属返回 T-01..T-05 前向修正并重跑 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..AC-009, AC-011..AC-014, AC-023 | T-01,T-02,T-06 | Boot/config state、业务行为、full/core、双实例 | T-01,T-02,T-06 | passed |
| AC-010..AC-013 | T-02,T-06 | Captcha/Notify/Oss 两阶段行为与真实发布 | T-02,T-06 | passed |
| AC-015..AC-017 | T-04,T-05,T-06 | menu/domain/manifest、Nginx、Playwright | T-04,T-05,T-06 | passed |
| AC-018..AC-021, AC-024 | T-03,T-04,T-06 | Compose、MySQL、auth、persistence、DML | T-03,T-04,T-06 | passed |
| AC-022 | T-01..T-06 | log/info/DOM/build/test secret scan | 全部 Evidence | passed |
| ADR-001..ADR-012（ADR-003 superseded） | T-01..T-06 | Ticket contract 与 Gate | 全部 Evidence | passed |
| CDE Nacos 参考 | T-01,T-02 | 生命周期/YAML 复用；显式反证不兼容默认 | T-01,T-02 | reference-only |
| Nacos 2.5.4 官方 auth/Docker/schema | T-03,T-05,T-06 | fixed image、官方配置、真实容器 | T-03,T-05,T-06 | passed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence、E2E、direct-parent 与最终回复 owner |
| Implementation subagents | 配置快照最多 3，Lead 不计入；current 模式实际同时 writer=1 | config=3；current 单 writer；当前协作约束下未经用户明确要求不派 subagent |
| Integration attempts | `null`（unlimited） | config 快照；仍受停止条件、收益和 deviation control 约束 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation 只读，不竞争可变环境 |
| Dispatch | execution-time dynamic | 不预分配 provider/模型；本计划不构成外部 provider 或数据发送授权 |

`subagent-delivery operation=plan` 合同允许 implementation/review/research/test-observation 四类任务。若用户后续明确要求派单，每个 Packet 必须绑定 Ticket、当前父 result `base_sha`、`workspace_ref=current`、唯一 writer lock、writable/read-only paths、非 E2E 检查、commit 授权、停止条件和返回字段。subagent 不写 SpecDev 工件/Evidence、不拥有 E2E 或父分支决策；Lead 重读 commit/diff/dirty/test 后才写 Evidence。外部网页通道还需要独立 provider/发送范围授权，本计划不授予。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | G0 固定 backend + aggregate result | `current` / 各自 `main` | Nacos JUnit、Maven reactor、full/core | backend `344c1bed`；aggregate `62d1aca` | default-off 与受影响回归通过；E2E not-required | passed |
| T-04 | T-01 aggregate result；重读 backend/frontend | `current` / 各自 `main` | domain/manifest/Vitest、SQL contract、typecheck/build | backend `3161aa`；frontend `da5318e`；aggregate `f8470f7` | 菜单/权限/镜像一致通过；E2E not-required | passed |
| T-02 | T-04 aggregate result；T-01 backend contract | `current` / 各自 `main` | Captcha/Notify/Oss JUnit 与 bundles | backend `c68cb55`；aggregate `a8f993e` | 三域原子/回归通过；E2E not-required | passed |
| T-03 | T-02 aggregate result | `current` / parent `main` | release static/shell/Compose config | aggregate `2cfc2cb` | real MySQL/Nacos required E2E 通过 | passed |
| T-05 | T-03 aggregate result；T-04 frontend result | `current` / 各自 `main` | Nginx static、frontend lint/typecheck/build | frontend `7462ffe`；aggregate `e43671c` | real Nginx/Nacos/Playwright required E2E 通过 | passed |
| T-06 | T-05 aggregate result | `current` / parent `main` | harness static、全项目非 E2E regression | aggregate `e37e542` | full real-stack required E2E 与 final regression 通过 | passed |

多仓库 Ticket 的 implementation checkpoint 是不可分割集合：所有被修改子仓 commit 先形成并保持 clean，随后聚合 parent commit 只包含该 Ticket 的 parent-owned 文件与精确 gitlink。Lead 在同一 current workspace 核对每层 ancestry、diff 和 dirty 状态；parent result 是该 Ticket 聚合 commit SHA，Evidence 同时记录 child SHA。缺少任一层 commit 不得关闭 Ticket。

`current` 模式不创建 source/candidate worktree。一个 Ticket 完成非 E2E 检查并形成 commits 后，Lead 在同一 current parent 上运行 integration checks 和适用 E2E；通过后 `result_sha` 等于聚合 implementation commit，再开放下一个集成序号。失败时不得开始后续 Ticket或用新 commit 混入其他范围。父 HEAD/child HEAD 漂移时先标记当前 checkpoint stale，重读归属并重新验证。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | authorized | 用户已明确要求执行 Goal Plan；保持 current workspace 唯一 writer 与精确路径边界 |
| Ticket worktree local changes | not-authorized | 本计划选择 current，不创建 Ticket worktree |
| Implementation commit | authorized | 每 Ticket/每被修改仓库必需；授权已记录于 `.status.json` |
| Local direct-parent verification and parent update | authorized | current 模式必需；授权已记录于 `.status.json` |
| Local candidate integration and parent update | not-authorized | 本计划不使用 candidate-merge |
| Push / PR / remote merge | not-authorized | 不从本地实现授权继承 |
| Branch/worktree cleanup | not-authorized | 不处理当前用户分支、gitlink 或工作树改动 |
| Deploy / migration / production actions | not-authorized | 本地隔离 E2E 之外逐动作、目标和条件批准 |

### Evidence Return

每个 Ticket 的实施返回必须包含仓库列表、branch/current locator、精确 parent-before、child/parent commits、clean/dirty 状态、实际路径、非 E2E 命令结果、未运行项和阻塞。Lead 只在独立核对后写 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-XX.md</Path>`，并记录 direct-parent verification、required E2E、result SHA 与父/子包含关系。测试日志、截图和状态输出先脱敏再引用。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- Nacos 默认关闭且不是启动必要条件；本地 YAML 保持完整。
- 命令行/system properties/环境变量高于 Nacos；远程保护 `nacos.config.*` 与 `spring.profiles.*`。
- 候选版本整份原子接受/拒绝；运行断连保留上一有效，离线重启不读客户端磁盘 snapshot。
- 即时清单只含 `captcha.*`、`notify.idempotency.*`、`oss.lifecycle.download-ttl`；其他合法键等待重启。
- 每实例直连、独立 digest/状态；发布成功不等于所有实例即时生效。
- 控制台是 official iframe、Nacos 独立登录；无 RuoYi CRUD/SSO/凭据注入。
- 固定 `nacos/nacos-server:v2.5.4` standalone、独立 DB/user、必填 secret、默认本机端口、可信网络；明文静态风险必须保留在文档。
- 日志、Actuator、DOM、构建产物与 Evidence 不出现正文、键值或 secret。

### Verification Integrity

T-01/T-02 的 fake SDK/Spring tests 只能证明解析、状态机和业务读取，不代替真实服务；T-03 必须真实证明官方镜像、MySQL schema、鉴权、持久化和健康门；T-05 必须用浏览器证明 iframe/资源/登录；T-06 必须在最终 parent result 上证明双实例与故障矩阵。禁止降低断言、跳过失败模块、把清单外键标成即时成功、用 sleep 代替状态轮询、在日志打印配置辅助测试，或用截图代替 digest/行为/数据库证据。

### Migration or Release Sequence

1. 在隔离 MySQL 验证 fresh 与 existing-volume 的 Nacos DB/user/schema 幂等初始化；不碰生产数据。
2. 以固定镜像启动 Nacos，开启鉴权，按官方流程设置强测试管理员密码并创建 local/dev/prod 测试 namespace。
3. 先运行 base backend，确认默认关闭与本地基线；再使用 optional override 显式开启两实例。
4. 发布测试 dataId，验证合法/混合/删除/非法/断连/重启/恢复与双实例状态。
5. 部署 LB `/nacos/` 和 admin-web target，保持菜单不自动授予普通角色；测试授权用户与 Nacos 独立登录。
6. 重启 Nacos 验证 MySQL 持久化，运行 full/core/frontend/release regression 与 secret scan。
7. 真实环境上线顺序为 DB/schema -> Nacos/auth/password -> namespace/config -> backend enable -> proxy/frontend -> 角色授权；每一步生产动作另行批准。

### Risks, Monitoring and Recovery

- **脏基线/多仓库：** 聚合 gitlink 已漂移且有其他用户修改。G0 关闭前必须形成用户认可的 clean parent checkpoint；Lead 不回退、不暂存、不提交这些现有变更。
- **明文 secret：** 监控 Nacos 登录、DB/backup 访问和输出扫描；泄露时轮换相关 secret、撤权并前向清理，不声称存储加密。
- **实例分歧：** 监控 connection、digest、last result/time 和分类；故障时停止继续发布，保留实例内上一有效并修复/回滚 Nacos 版本。
- **单节点故障：** 已运行实例保留内存，重启实例用本地基线；可关闭客户端并重启统一回本地，不自动删除 Nacos 数据。
- **代理/授权：** iframe 或 route 故障先撤菜单授权/隐藏入口，应用客户端可独立运行；RuoYi 权限不能替代 Nacos 账号撤权。
- **direct-parent 失败：** 保留当前 Ticket commits/工作树事实，不启动下一 Ticket；修正同一范围并重跑。无 candidate 次数上限不等于无限试错，触发停止条件即 deviation。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。局部实现偏差写 Ticket/Evidence；路径或局部合同返回 Ticket owner；外部行为返回 Spec；架构/安全取舍返回 Grill/ADR；迁移、固定版本、即时清单、权限或发布顺序变化暂停受影响 Gate。任何执行者不得先扩大路径、secret、网络或生产权限再补批准。

## 6. Progress and Decisions

### Current Status

- T-01、T-04、T-02 已按固定顺序完成并集成，分别有 implementation/result checkpoint 与 Lead Evidence。
- T-03 已以 `2cfc2cbfe80bed5e18556694c22ce2158ffead05` 完成并集成；15 项 release tests、Compose 5.5.0 正反向 config、Shell/YAML/schema 与双轴审查通过。
- 用户提供并授权远程中间件服务器后，Lead 在隔离目录完成 fresh/existing MySQL、官方 Nacos 2.5.4、鉴权、最小权限、持久化、重启与 secret scan required E2E；G2 已关闭。
- T-05 已以 frontend `7462ffece02a48f64288847c907a644c646e1944`、aggregate `e43671ce1b67873ad363512a3be69d5074c4a1a5` 完成；HTTP/TLS 真实 LB、21 个资源、权限、同源 iframe、官方登录、故障恢复与 secret scan 均通过。
- T-06 以 aggregate `e37e542f1b9f8990bdd84ae4fb22fa2ca2c77d0e` 完成；真实双实例、原子拒绝、断连、离线本地基线、恢复、MySQL 持久化、secret scan 与清理全部通过。
- full/core、admin-web build/lint/typecheck/test、release static gate 和 T-05 Chromium 同源登录 Evidence 全部闭合；24 个 AC 无 uncovered。

### Pending Decisions and Blockers

- 用户于 2026-09-01 明确要求执行本 Goal Plan 并以实际代码完成全部要求，因此 implementation commits、Deep Ticket 实施与 local direct-parent 推进已授权，G0 的授权门关闭。
- 聚合仓库既有 dirty 与 gitlink 漂移按用户指定的“当前工作树为权威”作为执行输入；所有提交使用精确 path staging，Evidence 区分 pre-existing dirty 与本 change 修改，不回退或夹带无关文件。
- Push/PR、远程合并、生产迁移/部署、角色授权、真实 secret、卷删除和 cleanup 继续未授权；这些不阻止隔离本地 E2E，但不会随实施授权自动发生。
- 当前无 blocker。所有 `namewta-nacos-t03-*`、`namewta-nacos-t05-*`、`namewta-nacos-e2e-*` 与专用 specdev 测试目录均已精确清理；既有 CDE Nacos 和 NAMEWTA MySQL/Redis/MinIO 保持运行。

### Resume Protocol

本 Goal 已完成。后续生产发布另行执行 DB/schema -> Nacos/auth/password -> namespace/config -> backend enable -> proxy/frontend -> 角色授权；发布前从 final aggregate result 重跑 `verify-release.sh` 与隔离 `verify-nacos.sh`，不得复用本次已清理的临时环境。

## Assumptions

- 用户未回复此前的 worktree 选择提示，因此按工作流推荐默认值采用 `current`；这是本 Goal Plan 局部选择，不修改全局配置。若用户改选 worktree，必须把 policy 改为 `required`、gate 改为 `candidate-merge` 并重新校验 Ticket workspace 合同。
- 后端/前端 `main` 与聚合 `main` 是预期本地父分支，但规划时的 SHA 只用于识别当前漂移；G0 关闭时必须重新固定实际 base。
- 用户提供的远程 Docker 主机与隔离 data root 是当前 T-03/T-05/T-06 required E2E 环境；任何真实环境不可用时保持对应 Ticket blocked，不以 mock 降级。
