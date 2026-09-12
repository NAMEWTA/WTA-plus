---
schema_version: 6
artifact: goal-plan
change: 2026-08-24-oss-eli5
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

# Goal Plan: OSS 公共桶与私有桶增强交付

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/{change}/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/{change}/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在复用现有直传、UploadTicket、生命周期、引用和多 configKey 能力的前提下，交付物理隔离的 PUBLIC_READ 与 PRIVATE 双 Bucket 模型：公共对象可通过稳定受管域名匿名 GET/HEAD，私有对象只能在业务授权后获得服务端命名策略约束的短时签名 URL；上传路由、配置治理、Provider readiness、存量迁移、OpenAPI/UI 和发布门禁全部失败关闭。

### Success and False Completion

成功必须同时满足：8 张非 cancelled Ticket 各有 clean、非空 source checkpoint、Lead 核对的路径 diff、通过的 parent-candidate 与对应 repository result SHA；AC-001 至 AC-024 全部闭合；公共 Bucket 匿名写始终拒绝；PRIVATE 原始 URL 匿名读取拒绝且签名过期失效；`sys_oss.service` 是访问类型和存储位置的唯一对象来源；配置与迁移不会把任何存量对象隐式公开；T-02 至 T-06 的真实 MySQL/MinIO E2E、T-07 浏览器 E2E 和 T-08 完整双 Bucket release Gate 均由 Lead 在 candidate 状态执行；OpenAPI 与前端配对无 drift；Runbook 记录生产人工批准点和恢复路径。

以下均为伪完成：只增加 `PUBLIC_READ` 字段而未验证真实 Policy；在同一 Bucket 混合对象 ACL；用应用自动修改 Bucket Policy；客户端提交 configKey、Bucket、访问类型或 TTL；按旧 accessPolicy 自动公开；管理列表批量制造签名 URL；知道 ossId 即可匿名查 URL；source worktree 自报 E2E；用 mock 替代 Ticket 要求的真实 MySQL/MinIO/browser 结论；candidate 未进入对应 `main`；父仓候选吸收现有无关 dirty 内容；通过跳过测试、放宽权限或恢复 PUBLIC_READ_WRITE/custom 制造绿色。

### Non-goals

- 不重建 OSS 子系统，不恢复后端对象字节中转，不让业务表持久化 URL。
- 不创建门户 App 或通用匿名 ossId 查询端点，不把 `sys_oss_ref` 用作 ACL。
- 不自动配置 Provider Policy、DNS/CDN 或生产 domainUrl，不执行生产 DDL/DML、正式迁移、流量启用、rollback 或源对象 cleanup。
- 不推送、创建 PR、远程合并、部署或清理 source/candidate branch/worktree。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | worktree 策略、产品取舍与授权 | 更新真正拥有该决定的工件和授权状态 |
| 2 | `<Path>{roots.state}/specdev/changes/{change}/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/{change}/CONTEXT.md</Path>` | 当前 change 架构决定与 OSS 语义 | 返回 `<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>` 更新真正 owner |
| 3 | `<Path>{roots.state}/specdev/adr/</Path>` 与 `<Path>{roots.state}/specdev/context/</Path>` | 控制面/数据面、UploadTicket、引用、Business Owner 与前端分层 | 当前 change 替代时在 ADR/LOG 明示 |
| 4 | `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>` | 外部行为、范围、24 个 AC 与验收接缝 | 下游不得改写 |
| 5 | `<Path>{roots.state}/specdev/changes/{change}/ticket/</Path>` | 单 Ticket 路线、路径、迁移与验证 | Goal Plan 只编排 |
| 6 | 当前代码、配置、POM、package scripts、测试、Provider 能力与 Git | 可执行命令、基线和实现可行性 | 冲突时暂停并按 deviation 返回真正 owner |

## 2. Execution Graph

### DAG and Critical Path

```text
G-00 authorization + repository baseline
  └─→ T-01 access/config prefactor
        └─→ T-02 safe config/schema upgrade
              └─→ T-03 Provider diagnosis + readiness
                    ├─→ T-04 unified access URL ─→ T-06 audited migration ─→ T-07 OpenAPI + UI ─┐
                    └─→ T-05 uploadPolicy storage routing ─────────────────────────────────────┤
                                                                                               └─→ T-08 release gate
```

关键路径为 `G-00→T-01→T-02→T-03→T-04→T-06→T-07→T-08`。T-04 与 T-05 在 G-30 后可并行形成 source checkpoint；Lead candidate integration 全局串行，固定顺序为 `T-01→T-02→T-03→T-04→T-05→T-06→T-07→T-08`。下游 worktree 激活时必须从对应 repository 最新已通过 result 重新冻结 base，不以计划创建时的 seed SHA 代替 blocker result。

### Waves and Ownership

| Wave | Ticket | 前置条件 | Repository/项目写路径摘要 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | G-00 | backend common-oss、properties、application.yml | T-01：双类型与策略配置合同 | 1 / G-10 |
| W2 | T-02 | T-01 result；G-10 | backend OSS config domain/service/controller、NAMEWTA SQL | T-02：配置治理与 DDL/DML | 2 / G-20 |
| W3 | T-03 | T-02 result；G-20 | backend Provider capabilities/diagnostics、readiness | T-03：Provider 安全状态 | 3 / G-30 |
| W4 | T-04 | T-03 result；G-30 | backend ruoyi-api、lifecycle、object store、访问 Controller | T-04：统一 URL Java/HTTP 合同 | 4 / G-40 |
| W4 | T-05 | T-03 result；G-30 | backend upload object store/service | T-05：上传路由 | 5 / G-40 |
| W5 | T-06 | T-04 result；G-40 | backend migration、SysOssMapper、migration Controller | T-06：迁移状态与原子切换 | 6 / G-50 |
| W6 | T-07 | T-06 result；G-50 | frontend OpenAPI、system domain/web-domain、定向 E2E | T-07：生成合同与配置 UI | 7 / G-60 |
| W7 | T-08 | T-04/T-05/T-06/T-07 result；G-60 | aggregate parent + backend release tests + frontend E2E + Runbook | T-08：最终验收资产 | 8 / G-70 |

`implementation_agent_limit=3` 是 config 与平台能力共同上限，不要求填满。只有 W4 的 T-04/T-05 具备实现并行条件；其项目写路径不相交。所有 candidate 仍由 Lead 一次集成一个。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 双类型、命名 TTL 与 uploadPolicy 存储配置 | — | `specdev-worktree/{change}/T-01`（backend） | execution-time dynamic | not-required：default-off 配置 prefactor | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| T-02 | 配置保护、保守升级与迁移台账 schema | T-01 | `specdev-worktree/{change}/T-02`（backend） | execution-time dynamic | required：Lead 在 candidate 使用隔离 MySQL | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| T-03 | Provider 只读诊断与 readiness | T-01,T-02 | `specdev-worktree/{change}/T-03`（backend） | execution-time dynamic | required：Lead 在 candidate 使用双 Bucket MinIO | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| T-04 | 公共稳定 URL 与私有短时 URL 统一解析 | T-01,T-03 | `specdev-worktree/{change}/T-04`（backend） | execution-time dynamic | required：Lead 验证真实匿名/签名/过期矩阵 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-04.md</Path>` |
| T-05 | 服务端策略固定路由公共/私有上传 | T-01,T-03 | `specdev-worktree/{change}/T-05`（backend） | execution-time dynamic | required：Lead 验证真实双 Bucket 上传位置 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| T-06 | 可审计复制、校验、切换、重试与回滚 | T-02,T-03,T-04 | `specdev-worktree/{change}/T-06`（backend） | execution-time dynamic | required：Lead 使用真实 MySQL + MinIO | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-06.md</Path>` |
| T-07 | OpenAPI、transport 与 OSS 配置 UI 配对 | T-02,T-04,T-06 | `specdev-worktree/{change}/T-07`（frontend） | execution-time dynamic | required：Lead 运行浏览器 transport E2E | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| T-08 | 双 Bucket 完整发布 Gate 与 Runbook | T-04,T-05,T-06,T-07 | `specdev-worktree/{change}/T-08`（aggregate parent） | execution-time dynamic + Lead multi-repo owner | required：Lead 运行真实 MySQL/MinIO/backend/browser 全链 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- 每张非 cancelled Ticket 使用最新 blocker result 建立唯一 source worktree，形成非空 commit，实际修改不超出 writable/shared owner；成功集成后保留 source/candidate worktree，cleanup 仍未授权。
- T-01 至 T-06 进入 backend `main`，T-07 进入 frontend `main`；T-08 的 backend/frontend 子提交分别进入对应 `main`，aggregate parent result 精确记录两个 gitlink 与 Runbook，不吸收父仓其他 dirty 内容。
- AC-001 至 AC-024 无未经批准 deferred；公共只读、私有时限、服务端路由、readiness、配置保护、迁移恢复和兼容调用均有 checkpoint、命令、cwd、退出码与清洗后的 Evidence。
- T-08 在统一 parent-candidate 上运行 backend tests/package、frontend OpenAPI/architecture/test/typecheck/lint/build 和真实 MySQL/MinIO/browser release E2E；环境缺失或关键断言未执行时不得 Done。
- 生产 Policy/domain、SQL、流量、正式迁移、rollback、cleanup、部署与远程操作保持独立批准；本地完成不能推定生产已发布。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-00 Execution authorization | 用户选择 required | implementation commit 与 local candidate integration 均 authorized；三仓 branch/SHA/dirty 重读 | 全部 Ticket | Lead + 用户 | 已关闭；执行前漂移则刷新 base，未进入 I-implement 不创建实际 worktree |
| G-10 Access/config prefactor | G-00 | T-01 result；双类型、PRIVATE 默认、TTL 边界、storageConfigKey 与客户端字段白名单通过 | T-02 至 T-08 | Lead | 父分支不推进失败 candidate；在 T-01 source 修正 |
| G-20 Safe config/schema | G-10 | T-02 result；隔离 MySQL fresh/upgrade、旧值私有化、引用保护和 additive SQL 通过 | T-03 至 T-08 | Lead；生产 SQL 仍需用户 | 保留备份和 additive schema，修正 T-02，不执行生产迁移 |
| G-30 Provider readiness | G-20 | T-03 result；真实 public/private 匿名能力、unsupported/timeout 和零 Policy mutation 通过 | T-04/T-05 及下游 | Lead + 环境 owner | 配置保持 NOT_SERVING；修正外部 Policy 或 T-03 adapter 后重建 candidate |
| G-40 Access/upload verticals | G-30 | T-04/T-05 result；公共 GET/HEAD、匿名写拒绝、私有原始拒绝、签名过期与上传落点通过 | T-06/T-08 | Lead | 暂停消费者；分别返回唯一 owner Ticket，不跨票修补 |
| G-50 Audited migration | T-04 result；G-40 URL 侧已关闭 | T-06 result；dry-run、复制/校验、CAS service 切换、失败重试/回滚和未提前清源通过 | T-07/T-08 | Lead；正式迁移/cleanup 仍需用户 | 停止批次，保持源对象，按审计台账重试或恢复 service |
| G-60 Paired contract/UI | G-50 | T-07 result；OpenAPI provenance/no drift、POST transport、精确类型与浏览器 UI E2E 通过 | T-08 | Lead | 后端保持可用，前端不发布；在 T-07 source 前向修复 |
| G-70 Release candidate | G-40/G-50/G-60 | T-08 aggregate result；真实全链 E2E、全量适用门禁、禁止能力扫描、Runbook 与 SHA/Evidence 清单 | change completion | Lead；生产启用由用户另批 | no-release；Provider/config 置 NOT_SERVING，父分支不推进失败 candidate |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001 至 AC-003、AC-022/023：服务端上传路由 | T-01,T-05,T-08 | properties + upload service + real Bucket | T-01/T-05/T-08 | verified |
| AC-004、AC-006 至 AC-012、AC-015/021/024：URL 与授权边界 | T-03,T-04,T-07,T-08 | Java/API + anonymous HTTP + browser | T-03/T-04/T-07/T-08 | verified |
| AC-005、AC-014、AC-016：Provider readiness | T-03,T-07,T-08 | capability/readiness + real Provider matrix | T-03/T-07/T-08 | verified |
| AC-013、AC-017/018：安全配置与升级 | T-01,T-02,T-06,T-07 | config/service + MySQL fresh/upgrade | T-01/T-02/T-06/T-07 | verified |
| AC-019/020：迁移完整性与恢复 | T-06,T-08 | state machine + MySQL/MinIO fault injection | T-06/T-08 | verified |
| 永久 OSS ADR-0002/0003/0004/0005/0010 | T-01 至 T-08 | 控制/数据面、Ticket、策略、TEMP/ref、Business Owner 回归 | 各 Ticket + T-08 | verified |
| 前端 ADR-0013 | T-07,T-08 | App -> web-domain -> domain architecture check | T-07/T-08 | verified |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence、E2E、candidate 与父分支 owner |
| Implementation subagents | 最多 3，Lead 不计入 | config=3、平台可用上限=3；实际受 Wave/路径和测试环境限制 |
| Integration attempts | unlimited（`null`） | config 快照为 `null`；持续记录 attempts，停止由失败原因与偏差控制决定 |
| Read-only agents | 无 SpecDev 数字上限 | review/research/test-observation，不写项目或状态 |
| Dispatch | execution-time dynamic | provider、模型与是否派单按 Ticket 激活时事实选择，不预分配 |

每次 implementation Dispatch Packet 必须绑定 Ticket、依赖 Evidence、repository、不可变 base SHA、branch/worktree locator、writable/read-only/shared paths、允许动作、非 E2E 检查、停止条件和返回格式。Subagent 只写分配的 source worktree 并返回 commit；不写 Ticket/Map/Goal Plan/Evidence/status，不推进 `main`。Lead 独立复核实际 diff、commit、dirty 和检查后才进入 candidate。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend `main`; seed `c13a375f649cba176ba004d2f45ab2907c0f3574` | backend `speculo/{change}/T-01` | config/common-oss unit + reactor package | required | Lead affected reactor；E2E n/r | backend result |
| T-02 | backend 最新 T-01 result | backend `speculo/{change}/T-02` | config/service/migration unit | required | Lead 隔离 MySQL fresh/upgrade E2E | backend result |
| T-03 | backend 最新 T-02 result | backend `speculo/{change}/T-03` | capability/readiness tests | required | Lead 双 Bucket MinIO/canary E2E | backend result |
| T-04 | backend 最新 T-03 result | backend `speculo/{change}/T-04` | Java/API/lifecycle tests | required | Lead 匿名/签名/过期 Provider E2E | backend result |
| T-05 | backend 最新 T-03 result | backend `speculo/{change}/T-05` | upload route/ticket tests | required | Lead 双 Bucket 上传 E2E | backend result |
| T-06 | backend 最新 T-04/T-05 integration result | backend `speculo/{change}/T-06` | migration state/fault unit | required | Lead MySQL + MinIO migration E2E | backend result |
| T-07 | frontend `main`; seed `4b204f65a822bf080d71d9c90ed430e9467bcf16`，backend T-02/T-04/T-06 Evidence 与文档端点启动前置 result 为输入 | frontend `speculo/{change}/T-07`；backend 最小 prerequisite worktree | backend Spring 容器回归 + OpenAPI/domain/web-domain gates | required | Lead 固定 backend `/v3/api-docs` + Playwright + architecture/build | backend prerequisite + frontend result |
| T-08 | aggregate parent `main`; seed `41d2a30cb4b9ea24a00d870d319fd4ad59f2ebf3`，使用全部产品 result | aggregate `speculo/{change}/T-08`，嵌套 backend/frontend source commits | release tests、E2E assets、Runbook review | required composite parent checkpoint | Lead 分别验证并推进 backend/frontend candidate，再验证精确 gitlink 的 parent candidate 和全链 E2E | backend + frontend + parent result |

required 模式下 source worktree 不运行 E2E。每个 source commit 由 implementation owner 运行非 E2E 检查；Lead 在最新 parent 上创建 `speculo/integration/{change}/T-NN` candidate，运行 Ticket 要求的集成检查和 E2E，重读 parent HEAD 未漂移后才 fast-forward 或 merge。失败 candidate 不推进父分支；保留 source 继续修正。`Local candidate integration and parent update` 已获授权，但仅限本 change 的 Lead-owned 本地操作。

T-08 是单一 Ticket 的 aggregate checkpoint：backend/frontend 各自先形成子仓 source commit，aggregate parent source commit 只精确记录这两个 gitlink 与 `<Path>docs/oss-public-private-operations.md</Path>`。Lead 分别完成子仓 candidate 后，才建立指向同一 result SHA 的 parent candidate；三层 SHA 全部记录在 T-08 Evidence，change status 的 T-08 `source_checkpoint/result_sha` 以 aggregate parent commit 为主索引。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | not-authorized | 本 Plan 固定 required；当前 dirty workspace 只用于 Lead SpecDev 状态，不实现产品代码 |
| Ticket worktree local changes | allowed | 限 Ticket writable/shared owner 合同和唯一 source worktree |
| Implementation commit | authorized | 用户于 2026-08-31 明确授权；每 Ticket/子仓必须非空且可定位 |
| Local direct-parent verification and parent update | not-applicable | required 模式不使用 direct-parent |
| Local candidate integration and parent update | authorized | Lead-only；candidate 通过且父 HEAD 未漂移后推进对应本地 `main` |
| Push / PR / remote merge | not-authorized | 不从本地授权继承 |
| Branch/worktree cleanup | not-authorized | 成功集成后仍保留，另行批准 |
| Deploy / migration / production actions | not-authorized | Provider Policy/domain、生产 SQL、流量、迁移、rollback、cleanup 均逐项批准 |

### Evidence Return

Implementation subagent 只返回 Ticket、workspace、final commit、dirty、修改路径、非 E2E 命令/结果、未运行项与恢复条件。Lead 重读 Git 和文件，运行 candidate 集成/E2E，写入 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-NN.md</Path>`、Ticket/Map/Goal Plan 和 change status；任何 subagent 自报的 E2E 在 Lead 复核前均为未验证。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 单个 configKey/Bucket 只对应 PRIVATE 或 PUBLIC_READ；公共只允许匿名 GET/HEAD，禁止 PUBLIC_READ_WRITE/custom 和匿名写。
- 对象访问和迁移唯一依据 `sys_oss.service`；客户端不能提交 configKey、Bucket、访问类型或 TTL，UploadTicket 冻结实际目标。
- Provider Policy 只读诊断且失败关闭；应用不创建公共测试对象、不修改 Policy，无法验证的 Provider 为 UNVERIFIED/NOT_SERVING。
- 生产 PUBLIC_READ 必须有有效 domainUrl；开发 Provider URL 回退必须显式开启；PRIVATE 始终签名。
- 存量对象全部保持 PRIVATE；有对象的配置不得普通编辑 Bucket/类型；迁移必须先复制校验、再原子切换、后观察，源清理独立批准。
- `OssService` 统一解析且保留旧方法；业务 Owner 先授权，不新增匿名 ossId 元数据接口，管理列表 URL 保持 null。
- SQL 只追加 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 与 `DML.sql`，不修改 `ry_vue.sql`，不执行生产 SQL。
- OpenAPI 通过 tooling 生成且有 provenance；前端保持 App -> web-domain -> domain，不新建门户 App。

### Verification Integrity

每张票先证明目标测试会在旧行为上失败，再实现到通过。source-worktree 只接受 Ticket 非 E2E 检查；任何真实 MySQL、MinIO、HTTP expiry 或 browser 声明必须来自 Lead-owned parent-candidate。Evidence 记录精确 cwd、命令、exit code、checkpoint 与环境清单，并清洗 Secret、AccessKey 和完整签名查询串。禁止通过跳过测试、仅静态 mock、放宽权限/类型规则、复用生产 Bucket 或把 environment blocker 写成 pass 获得绿色。

### Migration or Release Sequence

1. T-01 expand 双类型和服务端策略合同，默认 PRIVATE 且不激活公共流量。
2. T-02 additive schema/config migrate，把旧值保守解释为 PRIVATE，并建立迁移台账。
3. T-03 observe：部署方准备 Policy/domain，应用只读诊断，readiness 未通过不得服务。
4. T-04/T-05 contract：公共/私有 URL 与新上传路由进入候选；现有对象仍 PRIVATE。
5. T-06 migrate：备份、dry-run、小批复制/校验、CAS 切换、观察；失败可重试/回滚，源对象不清理。
6. T-07 配对生成 OpenAPI 并更新 admin UI；前后端只按通过的 result 组合。
7. T-08 verify/release：完整候选回归和 Runbook 审核；生产 Policy、SQL、流量、正式迁移与 cleanup 分别再批准。

### Risks, Monitoring and Recovery

- Provider 对 Policy 查询、匿名 HEAD、path-style 和域名行为不同；能力不足时保持 NOT_SERVING，不猜测安全状态。
- 公共稳定 URL 一旦传播无法靠业务撤下立即失效；紧急恢复为迁回 PRIVATE、删除对象或由部署系统收紧 Policy/CDN。
- 签名 URL 是短期 bearer URL；监控只记录策略、类型、错误类和到期秒数，不记录完整 URL。
- 大对象迁移可能受限流、网络和校验能力影响；批量有界、幂等、保留源对象与逐项审计，父分支失败不推进。
- 父 HEAD 漂移使 candidate stale；Lead 从最新 result 重建并重跑，持续累计 attempts 但不按次数自动阻断；继续修正无合理收益或需要上游决定时标记 blocked，不改写历史。
- T-08 多仓组合失败时，各子仓 `main` 只保留已独立通过的 result；parent 不推进，Runbook 和 gitlink 在 aggregate source 修正。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。路径越界、共享 owner 冲突、Provider 不能安全诊断、外部合同变化、迁移恢复不成立或需要新产品决定时，停止受影响 Wave，记录 observed fact/impact/correction owner，并返回 Spec、Ticket 或 Grill 的真正权威层；不得在 Goal Plan 或实现中静默改写 AC。

## 6. Progress and Decisions

### Current Status

- Workspace：用户明确选择开启独立 Ticket worktree；固定 `required + candidate-merge`。
- Authorization：implementation commit 与 Lead-owned local candidate integration/parent updates 已授权；cleanup、remote、deploy 和 production actions 未授权。
- Limits：Lead `codex:/root`；implementation subagent 上限 3；integration attempt 不设次数上限但完整累计审计。
- Result：aggregate parent `main@01c815f8952ec5a5a3e92d75341ab3af3fd36895`；backend `main@7ea0de75e17483040411a136ca1199dbb76b6d8b` clean；frontend `main@338c6b08dc6918b33248180bdfe3eb826099a901` clean。
- Parent dirty：既有用户 archive/ADR/context/command、其他 change 和本 change SpecDev 工件仍保留；T-08 aggregate result 仅吸收两个精确 gitlink 与 Runbook，未吸收这些 dirty 内容。
- Gate：T-03 readiness 周期续期偏差已关闭；修正后的真实隔离 MySQL、双 Bucket MinIO、Redis、Chromium、全量门禁和聚合候选均已通过，G-70 关闭。

### Pending Decisions and Blockers

- 当前无外部阻塞；change implementation 已完成。
- 真实隔离 MySQL、双 Bucket MinIO 和 browser Gate 已执行，不以 mock 替代。
- 生产 Provider Policy/domain、SQL、流量、正式迁移、rollback、cleanup、远程和部署不是 Plan Ready 前置，但始终保持独立未授权。

### Resume Protocol

恢复或进入 I-implement 时读取本 Goal Plan、目标 Ticket、`<Path>{roots.state}/specdev/changes/{change}/.status.json</Path>` 与最新 Evidence，并重读三仓 branch/SHA/dirty、worktree list 和授权。激活 Ticket 时将 planned `base_sha` 更新为对应 repository 最新 blocker result，再创建唯一 source worktree；从最后通过的 result 或待修正 source checkpoint 继续，不重新决定 workspace、DAG、Gate、shared owner 或 E2E disposition。

## Assumptions

- 逻辑 locator `specdev-worktree/{change}/T-NN` 在执行期映射为对应 repository 的唯一 Git worktree；物理路径由 Lead 创建时记录。
- T-08 的 aggregate parent checkpoint 可用 gitlink 完整定位 backend/frontend 子提交；三仓 checkpoint 与验证详情同时写入 T-08 Evidence。
- 计划创建后父分支或命令若漂移，Lead只刷新基线与受影响投影；若改变外部行为、路径所有权或验收能力则触发 deviation。
