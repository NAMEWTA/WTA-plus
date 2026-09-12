---
schema_version: 6
artifact: goal-plan
change: 2026-08-21-oss-direct-unified-notification
status: completed
modes: [migration, high-assurance, release-coordination]
orchestration: lead-directed
lead: codex
implementation_agent_limit: 1
integration_attempt_limit: 3
ticket_workspace_policy: current
integration_gate: direct-parent
ready_for_execution: false
---

# Goal Plan: OSS 浏览器直传与统一对外通知

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

在后端和前端 `main` 上形成可追溯的本地聚焦 commits：浏览器文件字节只与 OSS 交换，后端承担安全控制面、TEMP/引用生命周期和授权；Mail/SMS 通过薄 `common-notify` 统一发送，并以 best-effort Event 记录两层全局监控。最终父仓库只指向已经通过 direct-parent Gate 的两个子模块结果。

### Success and False Completion

成功要求 `AC-001` 至 `AC-032` 都有 Evidence，T-01 至 T-18 的既有交付保持通过，新增 T-19 至 T-22 均有真实 implementation checkpoint、current-workspace 检查和 Lead direct-parent result SHA；Business OSS Owner、同事务 fail-closed、fresh baseline 与 future ratchet Gate 全部关闭。

以下均属于伪完成：只完成 SDK 或表结构；后端仍代理文件字节；旧 OSS URL 仍有调用；业务仍直接调用 MailBuilder/SMS SDK；Event 落库缺少部分失败/duplicate/附件引用；把 Client 当租户过滤；只运行默认跳过测试的 package；把既有 `vue-tsc` 失败隐藏为通过；把人工浏览器验收写成 E2E passed。

用户于 2026-08-22 明确要求不做 E2E 测试。全部 Ticket 的 E2E disposition 为 `not-required`；不新增或运行自动化 E2E harness。真实 Bucket/CORS/Provider 浏览器矩阵仅作为发布前人工条件保留，本次未执行且不声称通过。

### Non-goals

- 不 push、创建 PR、远程合并、部署或执行生产数据库/OSS 迁移。
- 不创建 Ticket worktree、candidate branch 或并行项目 writer。
- 不建设通知中心、可靠队列、自动重试、回执、Provider failover、站内信统一或数据库模板中心。
- 不处理与本 change 无关的 `loginInfo/logininfo` 大小写基线问题，也不覆盖 `ruoyi-admin/pom.xml` 的既有用户修改。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户截至 2026-08-22 的明确决定与授权 | workspace、commit/direct-parent、测试边界、Client/ref 语义 | 更新真正拥有该决定的 Spec/Ticket/Plan，不在下游保留旧说法 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ADR.md</Path>` 与 `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/CONTEXT.md</Path>` | change 架构与领域语义 | 返回 Grill owner 修订，不由实现重议 |
| 3 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>` | 外部行为、范围、AC 与验证接缝 | 下游不得改写；新产品决定先更新 Spec |
| 4 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/</Path>` | 单 Ticket 范围、路径和完成合同 | Goal Plan 只编排；偏差返回 Ticket owner |
| 5 | `<Path>docs/upstream/customization-map.md</Path>` 与 engineering standards | Client/RBAC、SQL、子模块、测试和交付硬边界 | 冲突时暂停并按用户决定/代码证据修订上游工件 |
| 6 | 当前代码、Git 与命令基线 | 可行性和存量偏差 | 不覆盖用户 dirty 修改；新冲突进入 deviation |

## 2. Execution Graph

### DAG and Critical Path

```text
W1: T-01 -> T-02 -> T-06
             |       |
W2:          T-03 -> T-07
               \     /
W3:             T-04 -> T-08
                  |      |
W4:              T-05 -> T-09
                           |
W5:                     T-10 -> T-11
```

上图按 current workspace 的固定执行序列绘制；真实 `blocked_by` 仍以 Ticket frontmatter 为权威。关键依赖链是 `T-01/T-02 -> T-03 -> T-04 -> T-05` 与 `T-06 -> T-07 + T-03 -> T-08 -> T-09 -> T-10/T-11`。为保持单写者和逐 Gate 可恢复，固定执行顺序为：`T-01, T-02, T-06, T-03, T-07, T-04, T-08, T-05, T-09, T-10, T-11`。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径摘要 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| W1 | T-01 | backend 基线锁定 | common-oss + admin OSS client tests | 无 | G1/1 |
| W1 | T-02 | T-01 result | NAMEWTA DDL/DSL + admin migration tests | T-02 独占 DDL/DSL | G1/2 |
| W1 | T-06 | T-02 result | common-notify/mail/sms + common POM/BOM + admin tests | T-06 独占 POM/BOM | G1/3 |
| W2 | T-03 | G1 | ruoyi-api/system OSS lifecycle + admin tests | 无 | G2/4 |
| W2 | T-07 | T-03 result、T-06 Evidence | common-notify idempotency + admin tests | 无 | G2/5 |
| W3 | T-04 | G2 | system upload control/config + admin tests | 无 | G3/6 |
| W3 | T-08 | T-04 result、T-03/T-07 Evidence | common-notify、Mail Adapter、system direct notify dependency、attachment snapshot + admin tests | 无 | G3/7 |
| W4 | T-05 | G3 | backend contract + frontend OSS API/components | 无 | G4/8（双子仓库 commits） |
| W4 | T-09 | G4、T-08 Evidence | system notify monitor/API + admin tests | 无 | G5/9 |
| W5 | T-10 | G5 | admin/workflow/demo callers + admin tests | 无 | G6/10 |
| W5 | T-11 | T-10 result、G5 | frontend notify API/view | 无 | G6/11 |

Wave 是里程碑投影，不是并发授权。current 模式任何时刻只有一个 implementation writer。

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | S3-compatible Multipart/HEAD/Copy | — | current/backend main | Lead；仅用户另行明确要求时 dynamic dispatch | not-required；admin 单元/Provider 集成 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-01.md</Path>` |
| T-02 | schema、回填、权限菜单 | —；执行序列在 T-01 后 | current/backend main | Lead | not-required；admin DB 集成+迁移演练 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-02.md</Path>` |
| T-03 | TEMP/ref/授权下载 | T-01,T-02 | current/backend main | Lead | not-required；admin 集成+人工 API | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-03.md</Path>` |
| T-04 | 上传控制面 | T-03 | current/backend main | Lead | not-required；admin 集成+人工 API | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-04.md</Path>` |
| T-05 | OSS 协议切换 | T-04 | current/backend main + frontend main | Lead | not-required；门禁+人工浏览器网络验收 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-05.md</Path>` |
| T-06 | Notify core + Mail/SMS Adapter | —；执行序列在 T-02 后 | current/backend main | Lead | not-required；admin 单元/Adapter 集成 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-06.md</Path>` |
| T-07 | Redis 幂等 | T-06 | current/backend main | Lead | not-required；admin Redis 并发集成 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-07.md</Path>` |
| T-08 | 发送前附件快照 | T-03,T-07 | current/backend main | Lead | not-required；admin OSS/Provider 集成 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-08.md</Path>` |
| T-09 | 两层监控/API | T-02,T-03,T-08 | current/backend main | Lead | not-required；admin Event/DB/HTTP 集成 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-09.md</Path>` |
| T-10 | 现有调用收口 | T-09 | current/backend main | Lead | not-required；admin 聚合集成 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-10.md</Path>` |
| T-11 | 监控前端 | T-09；执行序列在 T-10 后 | current/frontend main | Lead | not-required；前端门禁+人工安全验收 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-11.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

- 22 个 Ticket 均为 done 或经用户批准 cancelled；每个 done Ticket 在所属子仓库有聚焦 commit 或明确复用的共享文件 checkpoint，并记录 direct-parent result SHA。
- `AC-001..032` 都映射到实际命令、数据库/Provider/API transcript、静态扫描或人工浏览器 Evidence；E2E 始终记录为 not-required。
- 后端新增单元/集成测试全部集中在 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/**</Path>`，并从 ruoyi-admin 聚合入口运行。
- 后端 opt-in tests 和 package 通过；前端 lint/build 通过；`vue-tsc` 不新增超出基线两个 TS1149 的诊断。
- 旧 OSS 字节协议和 Adapter 外已知 Mail/SMS 直接调用为零；TEMP/ref/notify schema、清理、权限、失败恢复与手工运维合同闭合。
- Client/userType 没有成为 OSS/通知租户键；`client_pk` 只记录来源；ref_type 是真实物理表名且不用于 ACL。
- 父仓库最终只更新两个已验证子模块指针和本 change 工件，不混入其他 dirty 内容。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G0 基线与授权 | Plan 开始 | current/direct-parent 选择、两项本地授权、三仓库 SHA/dirty 快照、基线命令 | 全部实现 | cursor-agent / 用户 | 授权撤销则 Plan blocked；dirty 漂移则重算基线 |
| G1 公共基础稳定 | G0 | T-01/T-02/T-06 commits；common/API/schema review；admin tests/package；SQL fresh/upgrade Evidence | T-03 以后 | cursor-agent；Deep 迁移批准点由用户 | 修正当前 Ticket；父 result 不通过不启动消费者 |
| G2 生命周期与幂等 | G1 | T-03/T-07 commits；引用并发、下载授权、Redis 幂等/故障测试 | T-04/T-08 以后 | cursor-agent | 保留最后 result，修正失败 Ticket，最多 3 次 direct-parent 尝试 |
| G3 控制面与附件 | G2 | T-04/T-08 commits；上传状态机、校验补偿、Copy/Provider 顺序与 TEMP 回收测试 | T-05/T-09 | cursor-agent | 禁用新策略/附件调用，清理测试对象，父分支停在上一 result |
| G4 OSS Contract | G3 | T-05 后端+前端 commits；旧 URL 扫描为零；admin tests；lint/build；人工网络/cancel/resume Evidence | OSS 对外完成、T-09 | cursor-agent / 用户发布批准另行取得 | 前后端配对前向修正；不得单侧推进父仓库 gitlink |
| G5 通知 Observe | G4 | T-09 commit；Event/DB/API/权限/删除/附件引用 tests；明文/脱敏/Client 矩阵 | T-10/T-11 | cursor-agent；敏感权限授予不在本地计划执行 | 停用 listener/API，保留 schema，修正后重跑 |
| G6 消费者与交付 | G5 | T-10/T-11 commits；直接调用扫描为零；admin tests/package、frontend lint/build、人工 HTML/权限验收；父 gitlinks/result | change 完成 | cursor-agent / 用户 | 不更新父指针或回退未通过子指针；不触碰远端/生产 |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001..014 OSS 直传/生命周期/授权 | T-01..05 | admin 单元/集成、SQL/Provider transcript、前端门禁、静态审查 | T-01..05 | completed；E2E not-required |
| AC-015..024 Notify core/幂等/附件 | T-06..10 | admin Dispatcher/Redis/OSS/Adapter/caller tests | T-06..10 | completed；E2E not-required |
| AC-025..031 Monitor/Client/Job | T-03,T-06,T-08..11 | admin Event/DB/HTTP/权限 tests、前端安全合同测试和门禁 | T-03,T-06,T-08..11 | completed；E2E not-required |
| AC-032 配置安全 | T-01,T-02,T-04 | ConfigurationProperties context test、运维诊断 review | T-01,T-02,T-04 | completed |
| Client/RBAC customization | T-03..05,T-09..11 | 多 Client 权限矩阵；检查无 client_pk 行过滤 | 对应 Ticket | completed |
| NAMEWTA SQL 只追加/七字段 | T-02 | diff、时间戳脚本、information_schema、fresh/upgrade | T-02 | completed |
| ADR-010 Business OSS Owner | T-19..T-22 | 集合协调、system/workflow Owner 合同、manifest/架构扫描 | T-19..T-22 | completed；E2E not-required |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `cursor-agent` | 唯一 SpecDev 状态、Evidence、人工验收与父分支 owner |
| Implementation subagents | 1，Lead 不计入；current 模式仍只允许一个 writer | config 上限 3，计划主动降为 1；用户未授权代理派单 |
| Integration attempts | 3 | config `max_integration_attempts=3` 快照 |
| Read-only agents | 无 SpecDev 数字上限，但本计划不预分配 | review/research/test-observation 只读且不写状态 |
| Dispatch | execution-time dynamic；只有用户后续明确要求委派时才创建 Packet | subagent-delivery `operation=plan` 合同；provider/模型不静态分配 |

Lead 保留全部 SpecDev、Git staging、commit 验收、direct-parent、人工验收和最终回复。即使后续获得明确委派授权，implementation 候选也只能在 current workspace 获取唯一 writer 锁；subagent 不写 SpecDev 工件或父分支状态。

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | backend main `1914c4917` + preserved dirty patch | current/backend main | admin focused tests | backend focused commit | admin opt-in tests/package；E2E not-required | backend commit SHA |
| T-02 | T-01 backend result | current/backend main | admin migration tests + SQL review | backend focused commit | fresh/upgrade transcript + admin tests；E2E not-required | backend commit SHA |
| T-06 | T-02 backend result | current/backend main | admin notify core tests | backend focused commit | dependency/BOM review + admin tests/package；E2E not-required | backend commit SHA |
| T-03 | T-06 backend result | current/backend main | admin lifecycle tests | backend focused commit | DB/Redis/OSS/auth integration；E2E not-required | backend commit SHA |
| T-07 | T-03 backend result | current/backend main | admin Redis tests | backend focused commit | concurrency/failure integration；E2E not-required | backend commit SHA |
| T-04 | T-07 backend result | current/backend main | admin upload tests | backend focused commit | HTTP/Redis/Provider/DB integration；E2E not-required | backend commit SHA |
| T-08 | T-04 backend result | current/backend main | admin attachment tests | backend focused commit | Copy/Provider/lifecycle integration；E2E not-required | backend commit SHA |
| T-05 | T-08 backend result + frontend main `adf5a0c5d` | current/backend and current/frontend main，依次单写 | admin cutover tests；frontend lint/build/type diagnostic | 一个 backend commit + 一个 frontend commit | 双端合同/扫描/人工浏览器；E2E not-required | backend/frontend SHA pair |
| T-09 | T-05 backend result | current/backend main | admin monitor tests | backend focused commit | Event/DB/HTTP/权限/删除 integration；E2E not-required | backend commit SHA |
| T-10 | T-09 backend result | current/backend main | admin caller tests | backend focused commit | admin aggregate tests/package + direct-call scan；E2E not-required | backend commit SHA |
| T-11 | T-05 frontend result + T-10 completed | current/frontend main | security test/lint/build/type diagnostic | `783da5759bbc0b978bec90a410ac940957ed9cc7` | security contract/static audit；E2E not-required | `783da5759bbc0b978bec90a410ac940957ed9cc7` |

`ticket_workspace_policy: current` 固定以下协议：不创建 source/candidate worktree；每次只允许一个 implementation owner；先核对 HEAD 与 dirty 基线，再修改、运行非 E2E 检查、形成 commit；Lead 重读 diff/commit/HEAD，在同一 current workspace 执行集成检查和人工验收，成功后记录 `method=direct-parent`、`source_checkpoint=result_sha`，再开始下一 Ticket。

T-05 是双仓库 Ticket，backend 与 frontend commit 均为完成条件，任一失败都不更新父仓库 gitlink。所有 Ticket 完成后，Lead 在父仓库 `main` 基于 `477f09b03` 选择性暂存两个子模块指针和本 change 工件，排除所有无关 dirty 文件，形成最终聚合 commit。

### Authorization Matrix

| 动作 | 状态 | 目标与条件 |
|---|---|---|
| Current workspace Ticket changes | allowed | 用户 2026-08-22 明确授权；严格串行、仅 Ticket writable paths |
| Ticket worktree local changes | not-authorized | 用户选择不开启 worktree |
| Implementation commit | allowed | 每 Ticket 在所属子仓库形成聚焦本地 commit；不含既有用户改动 |
| Local direct-parent verification and parent update | allowed | 用户明确授权；Lead 验证通过后推进本地 main/result，最终选择性更新父 gitlinks |
| Local candidate integration and parent update | not-authorized | current 模式不创建 candidate |
| Push / PR / remote merge | not-authorized | 需要独立明确授权 |
| Branch/worktree cleanup | not-authorized | 本计划不创建 Ticket worktree；其他清理不继承授权 |
| Deploy / migration / production actions | not-authorized | 只允许 disposable/local 验证；生产 SQL、Bucket policy/CORS/Lifecycle 另行批准 |

### Evidence Return

每个 Ticket 的 Evidence 记录：开始/结束 HEAD、dirty 快照、修改路径、admin/前端命令与退出码、人工 transcript、implementation commit、direct-parent result SHA、E2E not-required 理由、双轴 review、偏差和恢复条件。Lead 独立核对后写入；实现候选不能自报 Done。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- Client/userType 只用于认证授权和动态路由；`client_pk` 只记录来源，不是租户/所有权/Provider/幂等键。
- `sys_oss_ref.ref_type` 必须是实际物理表名，`ref_id` 是真实主键；仅反向定位和生命周期保护，不做 ACL 或动态查表。
- common 不反向依赖 system；跨业务模块通过 ruoyi-api 或 common SPI；业务 POM 只依赖最小 artifact。
- 后端不搬运浏览器文件字节；签名 URL、Secret、Authorization 不入库/日志。
- 新项目自有表具备七个基础字段、模块前缀、非裸 id 与中文注释；只追加 DDL.sql/DML.sql。
- Provider 同步发送；Event 只做 best-effort 监控；ACCEPTED 不等于 DELIVERED。
- 通知正文/目标永久明文是已批准风险，但列表脱敏、HTML 不执行、Provider Secret 不入库。
- 后端测试集中 ruoyi-admin；不在 common/system/workflow/demo 新建 `src/test`。

### Verification Integrity

- 后端实际测试命令：在 `<Path>ruoyi-vue-plus-namewta</Path>` 运行 `sh mvnw -pl ruoyi-admin -am -Dmaven.test.skip=false test`；package 不能替代 tests。
- 后端 build：`sh mvnw -pl ruoyi-admin -am package -DskipTests`，关键 Gate 可运行 `sh mvnw clean package`。
- 前端 active gates：在 `<Path>plus-ui-namewta</Path>` 运行 `pnpm lint` 与 `pnpm build:prod`。
- `pnpm exec vue-tsc --noEmit` 仅为补充诊断。基线因 `src/api/monitor/loginInfo` 与 `logininfo` 大小写冲突产生两个 TS1149、退出 2；本 change 不新增诊断即可，不能报告 typecheck passed。
- 全部 E2E status 为 not-required。人工浏览器/API/SQL/Provider 验收只按 manual/integration Evidence 报告。
- 禁止通过删除测试、放宽 compiler/lint、修改全局 skip、吞失败或提交生成物获得绿色。

### Migration or Release Sequence

1. Expand common-oss、schema 和 common-notify 公共合同。
2. 建立 OSS lifecycle/ref 与通知 Redis 状态能力。
3. 建立上传控制面和附件快照，再观察补偿/清理。
4. 同一发布单元切换前后端 OSS 协议并收缩旧字节入口。
5. 启用通知 Event 监控后再迁移业务调用和开放前端菜单。
6. 生产启用前另行批准并执行 MySQL migration、明确 Origin CORS、Expose ETag 与 incomplete multipart Lifecycle；本计划只产出可执行说明和本地/隔离验证。

### Risks, Monitoring and Recovery

- **既有 dirty POM：** backend `ruoyi-admin/pom.xml` 的用户 patch 当前 SHA-256 为 `1ea9de0c55c2718576868a792a7f8638a2c0d8fb9126eea365563d3d9c56d958`。T-10 若需同文件，只能 selective staging 本 change hunk；patch 漂移先停止核对。
- **无自动 E2E：** 浏览器/CORS/真实 Provider 的组合回归风险由用户接受；以人工网络 Evidence、集成测试和发布前独立批准补偿。
- **S3 兼容差异：** capability 测试与人工目标 Provider 验收；失败时保持旧发布版本，不静默降级安全校验。
- **明文永久日志：** 表容量和权限误授风险高；删除有 OperLog，但生产权限授予不在本地计划。
- **best-effort Event：** 允许宕机窗口丢监控；不能以日志缺失重试 Provider。
- **失败恢复：** 任一 direct-parent 检查失败保持当前 Ticket in_progress，后续 Ticket不启动；最多 3 次集成尝试，之后 blocked 并保留最后可信 commit/Evidence。
- **父 HEAD 漂移：** 停止、重读三个仓库 HEAD/dirty、重算 base；不在旧基线继续提交。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。出现公共 contract/schema、安全语义、E2E 决定、发布顺序、路径越界或不可逆动作变化时立即停止，更新其真正 owner 工件并获得用户批准；不在实现 commit 中静默决定。

- `DEV-T10-001`：T-10 的 CaptchaController 路径随仓库升级已迁移，精确纠正 expected/writable path，不改变行为。
- `DEV-T10-002`：Demo 邮件附件的本地占位路径改为 ossId/ossIds；SMS 黑名单保留为 Provider 管理原子操作，不计入通知发送直调扫描。
- `DEV-T10-003`：最终审计发现应用未装配 NotifyContextResolver；只扩展 T-10 的 ruoyi-admin config/context-test 路径，履行既有 AC-031，不改变 Client 语义。
- `DEV-T20-001`：迁移两个旧通知测试中的 raw bind/unbind 断言为集合协调断言，仅维护既有回归合同。
- `DEV-T22-001`：T-22 公共 API 收缩使旧生命周期合同测试必然过期；将该精确测试路径纳入 writable scope 并改为断言 `reconcileReferences` 存在、raw bind/unbind 不存在。

## 6. Progress and Decisions

### Current Status

- 2026-08-23 AR-001/ADR-010 新增 T-19 至 T-22 已获用户确认；change 重新打开为 active，本计划按 current/direct-parent 刷新并进入 I-implement。
- 2026-08-23 T-19 至 T-22 已按 current/direct-parent 串行完成；最终 backend result 为 `704d87a6ba09cf106760ff6619c4d4e4437c4087`，G7/G8/G9 全部关闭，change completion 门禁通过。
- 新批次 backend 基线为 `d836894f969d8f4134d72af129f3c452f8265c46`；仅保留用户 `ruoyi-admin/pom.xml` dirty patch，frontend `f7d116f6e2b6b61239afc86cbcb860a07530abad` 无需修改。
- CR-001 remediation 已完成；T-12 至 T-18 在 current workspace 严格串行落为 backend `d836894f969d8f4134d72af129f3c452f8265c46` 与 frontend `f7d116f6e2b6b61239afc86cbcb860a07530abad`，未创建 worktree、未触碰远端。
- 修复覆盖附件入口授权、REDACT_SENSITIVE、重复日志索引、分批物理清理、OSS PENDING 删除、续传/策略、业务引用/下载以及管理面诊断；102 个后端测试、36 模块 package、前端 Vitest/lint/build 已通过，vue-tsc 仅保留既有两条 TS1149。

- Goal Plan 已选择 current/direct-parent，implementation writer 上限 1，integration attempt 上限 3。
- 用户已授权 current workspace 修改、每 Ticket implementation commit 和 Lead local direct-parent/父分支推进；远端、部署、生产迁移和清理未授权。
- 基线：parent `main@477f09b03`；backend `main@1914c4917`，仅 `ruoyi-admin/pom.xml` 有上述用户 dirty patch；frontend `main@adf5a0c5d` 干净。
- 基线验证：backend ruoyi-admin opt-in test reactor 35/35 SUCCESS、现有 tests 1/1；frontend lint SUCCESS、build SUCCESS；vue-tsc 为既有两个 TS1149，退出 2。
- I-implement 已激活；T-01 已完成 direct-parent 集成，backend `main` 从 `1914c4917` 推进到 `eadbe8b60`。
- T-01 Evidence 已记录 7 个 OSS 聚焦测试、8 个 admin 全量 opt-in 测试、35 模块 package 与双轴审查；E2E disposition 为 not-required。
- T-02 已完成 direct-parent 集成，backend `main` 推进到 `20600f1d9`；fresh/upgrade MySQL 8.4、10 个 admin 测试和 package 均通过。
- T-06 已完成 direct-parent 集成，backend `main` 推进到 `7a37357b6`；8 个 notify 聚焦测试、18 个 admin 全量测试和 36 模块 package 均通过。
- T-03 已完成 direct-parent 集成，backend `main` 推进到 `2a94b4dfa`；14 个生命周期聚焦测试、32 个 admin 全量测试、36 模块 package 和一次性 MySQL 行锁竞争验证均通过。
- T-07 已完成 direct-parent 集成，backend `main` 推进到 `3e35a9c36`；真实 Redis 20 路并发、17 个通知聚焦测试、42 个 admin 全量测试和 41 模块 package 均通过。
- T-04 已完成 direct-parent 集成，backend `main` 推进到 `69f8b07e4`；17 个上传控制面聚焦测试、真实 Redis 12 路锁竞争、60 个 admin 全量测试和 36 模块 package 均通过。
- T-08 已完成 direct-parent 集成，backend `main` 推进到 `bb2a8114f`；10 个附件快照聚焦测试、70 个 admin 全量测试和 36 模块 package 均通过。
- T-05 已完成双仓库 direct-parent 集成：backend `68473cbbb`、frontend `1dba1ea71`；旧字节协议扫描为零，72 个 admin 测试、36 模块 package、frontend lint/build 通过，vue-tsc 仅保留基线两个 TS1149。
- T-09 已完成 direct-parent 集成，backend `main` 推进到 `63a05b8ee`；8 个聚焦单测、一次性 MySQL 8.4 真实 Mapper/Service 测试、81 个 admin 全量测试和 41 模块 package 均通过。
- T-10 已完成 direct-parent 集成并经最终审计推进到 backend `a56a6f907dc1fad2bde4daa92adfeafbcea3613f`；应用 Resolver 已真实快照请求来源，最终 93 个 admin 测试和 36 模块 package 通过，Adapter 外发送直调只剩已批准的 Demo SMS 黑名单管理例外。
- T-11 已完成 direct-parent 集成，frontend `main` 推进到 `783da5759bbc0b978bec90a410ac940957ed9cc7`；安全合同测试、lint、format 和 production build 通过，vue-tsc 仅保留基线两个 TS1149。
- T-05 的最终前端审计 checkpoint 为 `866e5ba1a75c9d308ce752f32fa6b4158763feed`；新增 2 个 Multipart retry/resume Vitest 后，前端全部 4 tests、lint 和 production build 通过。
- 当前恢复点是 backend `704d87a6ba09cf106760ff6619c4d4e4437c4087` / frontend `f7d116f6e2b6b61239afc86cbcb860a07530abad`；22 个 Ticket 全部完成，本地 change 已关闭。

### Pending Decisions and Blockers

无 blocker 或未决设计。生产迁移、权限授予、Bucket CORS/Lifecycle、部署、Push/PR 均未获授权且未执行，不影响本地实现完成态。

### Resume Protocol

如进入后续发布阶段，依次读取 Goal Plan、`.status.json.worktrees`、T-01..T-22 Evidence 和三个仓库 HEAD/dirty，并以 backend `704d87a6b`、frontend `f7d116f6e` 为本地完成基线。生产动作必须重新取得明确授权。

## 7. ADR-010 Owner Extension Execution

### DAG, Waves and Gates

```text
W-A: T-19 [reconcile expand]
             ├─> W-B1: T-20 [system owners]
             └─> W-B2: T-21 [workflow owner]
                         \ /
W-C:                    T-22 [manifest + API contract]
```

current workspace 固定串行执行 `T-19 -> T-20 -> T-21 -> T-22`。T-20/T-21 的 DAG 可并行性只保留为架构事实，不构成第二 writer 授权。

| Gate | 开启条件 | 关闭证据 | Shared owner | 失败恢复 |
|---|---|---|---|---|
| G7 引用协调稳定 | T-19 Ready | 集合差分、非法输入、幂等与失败传播测试；backend commit/result | T-19 expand OssService/SysOssServiceImpl | 保持后续 Owner 未迁移，修正 T-19 |
| G8 Owner 迁移完成 | G7；T-20/T-21 Ready | system 与 workflow insert/update/delete/rollback 测试；公共 bind/unbind 业务调用为零 | system 路径 T-20；workflow 路径 T-21 | 停在最后通过的 result，修正当前 Owner |
| G9 Ratchet Contract | G8 | manifest 双向扫描、fresh baseline、cleanup disabled/dry-run、旧公共 API 删除、全量 test/package | T-22 contract OssService/SysOssServiceImpl | 恢复编译并前向登记/迁移遗漏 Owner，不启用兼容层 |

### Ticket Execution Contract

| Ticket | Parent/base | Workspace | Implementation owner | Checks/E2E | Result |
|---|---|---|---|---|---|
| T-19 | backend `d836894f969d8f4134d72af129f3c452f8265c46` + preserved dirty POM | current/backend main | Lead | 24 focused lifecycle tests + 36-module package；E2E not-required | `c658669af9f6f44ddc92899b4fa3d52b3711179d` |
| T-20 | `c658669af9f6f44ddc92899b4fa3d52b3711179d` | current/backend main | Lead | 14 system/notify tests + 36-module package；E2E not-required | `3cd24fa2e63362e2c970a93773187c24e057a0ed` |
| T-21 | `3cd24fa2e63362e2c970a93773187c24e057a0ed` | current/backend main | Lead | 7 workflow Owner tests + 36-module package；E2E not-required | `44049525e24b688366b889f0771d096ef9aa3f30` |
| T-22 | `44049525e24b688366b889f0771d096ef9aa3f30` | current/backend main | Lead | 9 architecture/fresh baseline tests + 129 full opt-in tests + 41-module package；E2E not-required | `704d87a6ba09cf106760ff6619c4d4e4437c4087` |

### Extension Constraints and Recovery

- `OssService.reconcileReferences` 只承载机械集合差分；业务授权、表结构、恢复和删除语义留在 Owner module。
- 业务行与引用转换使用同一 `@DSTransactional` 并 fail-closed；不引入 best-effort 修复队列。
- workflow 只依赖 ruoyi-api；`flow_his_task.id` 是 refId，`task_id` 只用于定位新历史记录。
- manifest 是测试资源，不是运行时注册中心；禁止注解扫描、动态回调和动态查表。
- 当前无历史负担，不回填；`cleanup-enabled:false`、`cleanup-dry-run:true` 保持不变，启用仍需独立批准。
- backend 预存 `ruoyi-admin/pom.xml` patch 的 SHA-256 为 `53683d824e306dcb8584e22ad0cc39acdb7526d11e887dca6674e27f2310df97`，所有 commits 选择性暂存且不得包含该文件。

## Assumptions

- 用户所说“不做 E2E 测试”解释为 E2E disposition 全部 not-required，不建设/运行自动化 E2E；允许 Lead 进行不计为 E2E Gate 的人工浏览器、API、SQL 与 Provider 验收。
- 用户授权本地 commit/direct-parent 包含两个子模块的聚焦 commits及最终父仓库 gitlink commit，不包含 push、远程合并、部署、生产数据或 Bucket policy 修改。
- 当前模式不使用 implementation subagent；未来只有用户明确要求委派时，才按 execution-time dynamic Dispatch Packet 启用，且仍保持 current workspace 单 writer。
