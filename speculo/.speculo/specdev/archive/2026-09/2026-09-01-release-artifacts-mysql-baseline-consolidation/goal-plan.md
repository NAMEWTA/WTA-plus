---
schema_version: 6
artifact: goal-plan
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
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

# Goal Plan：发布资产与 MySQL 基座收敛

- **Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/goal-plan.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **Tickets Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/</Path>`

## 1. Outcome and Authority

### Outcome

父仓库 `<Path>release-artifacts/docker/infrastructure/mysql/init/</Path>` 成为唯一 MySQL 8.4 基座，六份 SQL 和六份 Workflow JSON 均被跟踪；后端 `<Path>ruoyi-vue-plus-namewta/script/</Path>` 完整退出；测试、发布脚本、当前规范和活动实施工件只读取新位置，并以隔离 MySQL fresh 初始化闭合全部验收合同。

### Success and False Completion

成功要求六份 SQL 可直接维护、`stage-mysql` 零写入、全部活动消费者迁移、旧目录和非 MySQL 方言归零、当前治理一致、隔离 MySQL 8.4 初始化与回归全部通过。只复制资产但继续消费旧目录、删除旧目录却静默跳过测试、或未实际运行隔离 MySQL 都属于伪完成。

### Non-goals

不部署开发或生产环境，不连接或修改现场数据库，不生成或执行已有库升级 SQL，不改写 completed/archived/Evidence 历史，不提交真实 `.env`、证书、密钥、日志、构建物或运行数据。

### Authoritative Inputs

| 优先级 | 来源 | 负责内容 | 冲突处理 |
|---|---|---|---|
| 1 | 用户最新明确决定 | MySQL-only、六文件直接修改、删除旧目录、提交推送和标签授权 | 更新真正拥有该决定的工件 |
| 2 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>` | 范围、行为、验收合同 | 下游不得改写 |
| 3 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/</Path>` | 单 Ticket 实现合同 | 本计划只编排 |
| 4 | 当前 Git、脚本、测试与隔离运行事实 | 可行性和验证结果 | 冲突时按偏差控制返回契约 owner |

## 2. Execution Graph

### DAG and Critical Path

```text
T-01 -> T-03 --+
                +-> T-04 -> T-05 -> T-06
T-02 -----------+
```

关键汇合点为 T-04，最终汇合点为 T-06。当前工作区策略强制按 `T-01 -> T-02 -> T-03 -> T-04 -> T-05 -> T-06` 串行执行。

### Waves and Ownership

| Wave | Ticket | 前置条件 | 项目写路径 | Shared owner | Gate/集成序号 |
|---|---|---|---|---|---|
| 1 | T-01 | Ready、摘要一致 | `release-artifacts` SQL/发布脚本/测试 | T-01 | G-10 / 1 |
| 1 | T-02 | Ready | `release-artifacts/workflow/leave` 与合同测试 | T-02 | G-10 / 2 |
| 2 | T-03 | T-01 Evidence | 父级 SQL 合同与后端测试消费者 | T-03 | G-20 / 3 |
| 3 | T-04 | T-01/T-02/T-03 Evidence | 后端 `script/` 删除 | T-04 | G-30 / 4 |
| 4 | T-05 | T-04 Evidence | 当前文档、Skill、活动 change | T-05/Lead | G-40 / 5 |
| 5 | T-06 | T-04/T-05 Evidence | CI/发布验证入口 | T-06/Lead | G-50 / 6 |

### Ticket Quick Reference

| ID | 可观察产出 | Dependencies | Workspace | Implementation owner | E2E disposition | Evidence |
|---|---|---|---|---|---|---|
| T-01 | 唯一 SQL 基座和只读 stage | - | `current` | Lead | not-required：T-06 统一导入 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-01.md</Path>` |
| T-02 | 六份 Workflow JSON 版本化 | - | `current` | Lead | not-required：静态资产 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-02.md</Path>` |
| T-03 | 所有测试读取唯一 SQL 根 | T-01 | `current` | Lead | not-required：定向集成由现有测试覆盖 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-03.md</Path>` |
| T-04 | 后端旧 script 完整退出 | T-01、T-02、T-03 | `current` | Lead | not-required：收缩检查 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-04.md</Path>` |
| T-05 | 当前治理和活动工件一致 | T-04 | `current` | Lead | not-required：治理变更 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-05.md</Path>` |
| T-06 | 聚合门禁与隔离 MySQL 8.4 初始化 | T-04、T-05 | `current` | Lead | required：核心跨边界合同 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-06.md</Path>` |

## 3. Gates and Completion Evidence

### Overall Definition of Done

AC-001 至 AC-014 均有可重复 Evidence；六个非 cancelled Ticket 都有非空实现提交或可审计的跨仓库结果提交、通过的 direct-parent 验证和父分支结果；后端、父仓库和子模块指针一致；隔离 MySQL 8.4 E2E 实际通过；工作树只剩用户明确保留的无关改动。

### Gates

| Gate | 开启条件 | 关闭证据 | 阻塞范围 | Lead/批准人 | 失败恢复 |
|---|---|---|---|---|---|
| G-10 资产接管 | T-01/T-02 Ready | SQL 跟踪、stage 零写入、Workflow 摘要与解析 | T-03/T-04 | Lead | 保留旧副本并修正新资产 |
| G-20 消费者迁移 | G-10、T-03 Ready | 活动测试旧 SQL 路径归零且缺根明确失败 | T-04 | Lead | 恢复当前 Ticket 修正接缝 |
| G-30 旧目录收缩 | G-20、Workflow 新位置有效 | `script/` 不存在，新资产摘要不变 | T-05/T-06 | Lead/用户已授权删除 | 由 Git 提交恢复后前向修正 |
| G-40 治理一致 | G-30 | 当前规范和活动路径只声明新 owner | T-06 | Lead | 修正规范 owner，不改历史证据 |
| G-50 发布就绪 | G-40 | 聚合回归、隔离 MySQL、SpecDev complete validator | Change 完成 | Lead | 清理本次隔离资源，回到失败 Ticket |

### Contract and Reference Coverage

| 合同或参考要求 | 覆盖 Ticket | 验证接缝 | Evidence | 状态 |
|---|---|---|---|---|
| AC-001 至 AC-003、AC-007、AC-009 | T-01、T-06 | Git/Node/Shell | T-01、T-06 | passed |
| AC-004 至 AC-006 | T-02、T-04、T-05、T-06 | 文件、摘要、当前引用扫描 | T-02、T-04、T-05、T-06 | passed |
| AC-008、AC-012 | T-06 | 隔离 MySQL 8.4 | T-06 | passed |
| AC-010 | T-03、T-06 | Node/JUnit/Maven | T-03、T-06 | passed |
| AC-011、AC-013、AC-014 | T-05、T-06 | 文档、Skill、活动 Change、升级合同 | T-05、T-06 | passed |

## 4. Execution and Integration Protocol

### Lead Orchestration

| 项目 | 决定 | 事实依据 |
|---|---|---|
| Lead | `codex:/root` | 唯一 SpecDev 状态、Evidence 与父分支 owner |
| Implementation subagents | 3，Lead 不计入；本计划不派遣 | config 快照；current 模式仍只允许单 writer |
| Integration attempts | `null`，不限次数 | config 快照；仍受停止条件和偏差控制 |
| Read-only agents | 无 SpecDev 数字上限；本计划不派遣 | 不写项目和状态 |
| Dispatch | execution-time dynamic | 当前由 Lead 串行自行实现 |

### Ticket Workspace and Integration

| Ticket | Parent/base | Workspace/branch | Source checks | Implementation commit | Integration checks/E2E | Parent result |
|---|---|---|---|---|---|---|
| T-01 | 根仓 `main` 开始 SHA | current/main | Node、Shell、Git、摘要 | 必须，引用 T-01 | direct-parent，无 E2E | Evidence 记录 SHA |
| T-02 | T-01 result | current/main | Node、JSON、摘要 | 必须，引用 T-02 | direct-parent，无 E2E | Evidence 记录 SHA |
| T-03 | 根仓和后端最新 main | current/main | Node、JUnit/Maven | 后端与父仓分别提交，引用 T-03 | direct-parent，定向集成 | Evidence 记录双方 SHA |
| T-04 | T-03 result | current/main | 删除扫描、后端回归 | 后端提交，引用 T-04 | direct-parent，无独立 E2E | Evidence 记录 SHA |
| T-05 | T-04 result | current/main | 文档/Skill/SpecDev 校验 | 父仓提交，引用 T-05 | direct-parent，无 E2E | Evidence 记录 SHA |
| T-06 | T-05 result | current/main | 全套门禁 | 父仓最终提交，引用 T-06 | direct-parent + required MySQL E2E | Evidence 记录 SHA |

所有 Ticket 严格串行。每次只有 Lead 写 current workspace；非 E2E 检查与提交完成后由 Lead 在同一父分支验证，成功才开始下一 Ticket。父仓最终提交更新后端子模块指针；前端无业务改动，仅验证并随已有提交推送。

### Authorization Matrix

| 动作 | 状态 | 依据 |
|---|---|---|
| Implementation commits | authorized | 用户明确要求完成全部迁移并提交 |
| Local direct-parent verification and parent update | authorized | 用户明确要求提交、推送最终结果 |
| 后端 `script/` 删除 | authorized | 用户明确要求迁移后删除整个目录 |
| 三仓库 push 与 `namewta-v0.0.3` 标签 | authorized | 用户本次明确授权 |
| 真实数据库写入或部署 | not-authorized | 本次只运行隔离 MySQL 验证 |
| 分支/worktree cleanup 与 Change archive | not-authorized | 不从完成授权继承 |

### Evidence Return

Lead 记录每个 Ticket 的父分支前后 SHA、修改路径、验证命令与退出码、SQL/Workflow 摘要、隔离 MySQL 结果和未运行项；不记录凭据、现场地址、数据正文或真实 `.env` 内容。

## 5. Constraints, Risk and Recovery

### Non-negotiable Constraints

- 用户本次明确要求完成所有迁移、提交、推送并发布 `namewta-v0.0.3`，视为 implementation commit、direct-parent integration、后端 `script/` 删除、三个仓库 push 和标签发布授权。
- 不授权真实开发/生产数据库写入、部署、归档 Change、删除分支/worktree 或提交敏感文件。
- 隔离 MySQL 资源必须使用唯一名称并只清理本次创建的容器/网络/卷。

### Verification Integrity

静态文本检查不代替 JUnit/Maven，JUnit 不代替真实 MySQL 8.4；Docker 不可用或隔离初始化失败时不得宣称 E2E 通过。不得通过跳过测试、放松断言、恢复旧 SQL 副本或让命令固定退出零制造绿色。

### Migration or Release Sequence

迁移采用 expand-contract：先跟踪唯一 SQL 和 Workflow 新位置，再迁移测试消费者，随后删除后端旧目录，最后收敛治理和 CI。fresh 环境直接按六文件顺序导入；已有库必须指定源/目标 Git Tag、先备份、在隔离库根据差异形成步骤，并把包含现场信息的报告写到被忽略的 `<Path>temp/relase/</Path>`，不得重放完整基座。

### Risks, Monitoring and Recovery

任一 direct-parent 检查失败时不将 Ticket 标为 done，保留当前改动继续前向修正。SQL 摘要在接管前不一致、活动 Change 无法取得唯一 owner、隔离 MySQL 失败或 secret 忽略失效时停止受影响 Gate。推送前重新 fetch 并检查远端非快进；标签只在三个 main 均推送成功且工作树满足交付条件后创建。

### Deviation Control

遵循 `<Path>{roots.workflows}/specdev/common/rules/deviation-control.md</Path>`。路径和局部实现偏差返回 Ticket；唯一基座、数据库支持范围和升级策略变化返回 Spec/用户。2026-09-01 执行预检发现 `50/60` 落后于随后完成的管理端运行能力 SQL，Lead 必须先等价接管最新后端内容并记录摘要，再删除旧目录。

## 6. Progress and Decisions

### Current Status

- G-10 至 G-50 已全部关闭，T-01 至 T-06 均完成并形成 Evidence。
- 父仓实现结果为 `9dc99e715d8ba542dcee547205504cf9d74c32a4`，后端实现结果为 `6927ee9786ba87f432127f4feda61c0f43c01849`。
- 发布合同 31/31、后端 42 模块测试、full/core 打包及 JAR 校验全部通过。
- 用户指定开发服务器上的隔离 MySQL 8.4.9 fresh 初始化通过；最终 87 张表，回放保护生效，容器与 `/data/namewta-mysql-baseline-validation-v003` 已清理。
- T-01 的 `50/60` 计划后漂移已通过接管后端最新内容解决；无活动 deviation、blocker 或未运行的 required 检查。

### Pending Decisions and Blockers

无产品决策 blocker。真实环境数据库、部署、归档和来源清理不在本次授权范围；MySQL 8.4 整数显示宽度与 AI 重复索引告警记录为后续清理候选，不阻塞本 Change。

### Resume Protocol

恢复时读取本 Goal Plan、Ticket frontmatter、change `.status.json`、最新 Evidence 和三个仓库 HEAD。最后一个 `done` Ticket 的 `result_sha` 是恢复 checkpoint；任何未记录验证的工作都按未通过处理。G-50 关闭后由 Lead 按 change completion 合同把 Change 标记 completed；远程 reconcile 与 archive 不属于本次范围。

## Assumptions

- 用户要求直接完整执行且未要求 worktree，因此采用工作流推荐默认 `current`，只作用于本 Goal Plan。
- 父仓和后端已有未推送提交属于用户已完成工作，本 Change 只在其后追加提交，不回退历史。
- 标签将发布到三个仓库同名 `namewta-v0.0.3`，与既有 `namewta-v0.0.1`、`namewta-v0.0.2` 发布方式一致。
