---
schema_version: 3
artifact: ticket
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
id: T-06
title: 打通发布回归与 MySQL 全新初始化门禁
status: done
planning_depth: deep
planning_depth_reason: 跨父仓库、后端子仓库和真实 MySQL 8.4 验证最终组合状态，并固化 CI 防回归入口
ready: true
risk: high
blocked_by: [T-04, T-05]
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-013, AC-014]
owner: codex:/root
expected_changes:
  - "<Path>scripts/ci/run-external-services.sh</Path>"
  - "<Path>.github/workflows/quality-gates.yml</Path>"
  - "<Path>release-artifacts/scripts/verify-release.sh</Path>"
  - "<Path>release-artifacts/scripts/init-mysql-container.sh</Path>"
  - "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>"
writable_paths:
  - "<Path>scripts/ci/run-external-services.sh</Path>"
  - "<Path>.github/workflows/quality-gates.yml</Path>"
  - "<Path>release-artifacts/scripts/verify-release.sh</Path>"
  - "<Path>release-artifacts/scripts/init-mysql-container.sh</Path>"
  - "<Path>release-artifacts/tests/release-integration-contract.test.mjs</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/**</Path>"
  - "<Path>release-artifacts/workflow/leave/**</Path>"
  - "<Path>release-artifacts/tests/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/**</Path>"
  - "<Path>.agents/skills/**</Path>"
shared_paths:
  - "<Path>scripts/ci/run-external-services.sh</Path>"
  - "<Path>.github/workflows/quality-gates.yml</Path>"
shared_path_owners:
  - "<Path>scripts/ci/run-external-services.sh</Path> => T-06"
  - "<Path>.github/workflows/quality-gates.yml</Path> => T-06"
---

# Ticket T-06：打通发布回归与 MySQL 全新初始化门禁

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/06-prove-release-and-mysql-gates.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 在最终组合状态证明六份唯一 SQL 可初始化隔离 MySQL 8.4，发布、后端、治理与路径收缩门禁全部可重复，并把关键检查接入现有 CI 入口。
- **可观察产出：** 一条聚合验证链能指出具体失败文件或阶段；fresh MySQL 顺序导入成功；所有 14 项合同均有最终覆盖证据。
- **来源：** `US-007`、`US-008`、`US-009`、`AC-001` 至 `AC-014`、`NFR-001` 至 `NFR-005`。
- **当前事实：** 已有发布验证和真实外部服务脚本，但当前门禁仍建立在后端 SQL 来源和旧 stage 语义上。
- **Planning Depth 原因：** 涉及真实数据库、CI 共享入口、跨 Git 仓库结果与数据完整性，事故半径覆盖整个开发基座。

## 2. 决策状态

### 已锁定决策

- E2E 仅对隔离 MySQL 8.4 执行，不连接开发、生产或用户数据库。
- 按 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 顺序初始化，校验关键表、数据和元数据。
- 已有数据库升级不在此 Ticket 自动执行；只验证部署 Skill 要求源/目标 Tag、备份、隔离演练和 `<Path>temp/relase/</Path>` 私密报告。
- CI 与本地命令同源；无法运行 Docker 时必须报告环境缺口，不得宣称 E2E 通过。

### 已采用的低影响假设

- 优先扩展现有 `run-external-services.sh`、`verify-release.sh` 和初始化脚本，不另造平行编排器。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 聚合回归、隔离 MySQL fresh 初始化、CI 接线、失败定位、合同覆盖收口 | 现有 Docker/Node/Maven 门禁、六份 SQL、Nacos 固定资产 | 真实开发/生产部署、已有库升级执行、推送、Tag、远程 required check 配置 |

## 4. 要构建什么

Lead 在最终父分支候选状态运行发布静态合同、后端测试与打包，并启动一次隔离 MySQL 8.4 顺序导入六份 SQL。门禁要验证 Git 跟踪、零写入 stage、旧目录和非 MySQL 零残留、Workflow JSON、SQL 分类、关键 Schema/数据以及当前规范一致性。任何失败必须指向具体阶段，且只清理本次隔离资源。

## 5. 实现契约

- **入口或接缝：** 现有父级 CI、发布验证脚本、MySQL 初始化脚本、Node/Maven 测试。
- **输入与输出：** 输入为最终仓库候选、隔离 MySQL 配置和占位安全环境；输出为逐阶段状态、退出码与 Evidence。
- **公共接口变化：** 仅质量门禁入口增强；不改变产品 API。
- **不变量：** 不改六份 SQL，不连接真实环境，不泄露环境变量，不把 Docker 不可用标成成功。
- **状态或数据流：** 父候选 -> 静态合同 -> 后端回归 -> 隔离 MySQL 导入 -> SQL/数据断言 -> 清理隔离资源。
- **错误与失败行为：** 任一阶段失败立即非零退出并保留必要的脱敏日志；清理仅限本次显式创建的容器/卷。
- **兼容要求：** 保持现有 CI job 和发布脚本入口，新增检查与本地命令同源。
- **安全与隐私要求：** 使用临时占位账密和隔离容器；不打印 secret；不写真实 `<Path>release-artifacts/.env</Path>`。

## 6. 执行路线

1. 在最新父分支候选重读 T-01 至 T-05 Evidence、Git 状态和子模块提交关系。
2. 建立聚合合同测试，覆盖六文件跟踪、忽略、stage 零写入、旧路径、非 MySQL、Workflow JSON 和当前规范。
3. 调整现有发布验证和 CI 入口，使其调用同一组合同与显式 SQL 根测试。
4. 运行 Node、Shell、Maven 测试和两个后端 bundle 打包门禁。
5. 启动隔离 MySQL 8.4，按固定顺序导入六份 SQL并查询关键结构、数据与元数据。
6. 执行受控失败检查，证明缺文件、错误 SQL 根或导入失败能使门禁变红；恢复后重跑。
7. 汇总 14 项合同证据、清理隔离资源并记录未运行项和残余风险。

## 7. 路径访问契约

- **预计修改点/可写范围：** 只允许现有 CI/发布门禁入口及独立集成合同测试。
- **只读上下文：** 所有产品代码、SQL、Workflow、Skill 和前置测试只读。
- **共享路径：** 父级 CI 入口由 T-06 唯一 owner；Lead 负责最终集成和 E2E。
- **保留或不动：** 六份 SQL 内容、真实数据库、真实 `.env`、远程分支保护和生产服务器。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 聚合发布门禁 | Node/Shell/Maven/Skill/SpecDev 全套检查 | 所有静态与构建门禁通过 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-06.md</Path>` |
| 失败路径 | 受控反向验证 | 隔离夹具缺文件、错误 SQL 根或失败 SQL | 对应门禁非零且指出失败对象 | 同上 |
| E2E | 隔离 MySQL 8.4 | 顺序导入六份 SQL并查询关键不变量 | fresh 初始化成功且结果可重复 | 同上 |
| 回归 | Git/子模块/构建 | 检查父子提交、双 bundle 和 stage 摘要 | 组合状态完整且 SQL 零改写 | 同上 |

- **Workspace checks：** Node、Shell、Maven、Skill、SpecDev、Git 与子模块检查。
- **E2E disposition：** `required`：六份基座能否在真实 MySQL 8.4 fresh 初始化是本 change 的核心跨边界合同。
- **E2E owner/environment：** Lead / current 模式使用 current-workspace，required 模式使用 parent-candidate；禁止在 source worktree 声明 E2E 通过。
- **Integration evidence：** 所有前置实现提交、父分支 before/result、候选提交、子模块 SHA、MySQL 日志摘要和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 静态合同 -> 后端回归 -> 候选集成 -> 隔离 MySQL -> 最终合同覆盖。
- **兼容窗口：** T-06 通过后，旧目录和旧 stage 语义不再兼容；已有库仍走独立 Tag 差异流程。
- **监控信号：** 每阶段退出码、失败文件、SQL 导入位置、关键表/数据查询、容器清理状态。
- **回滚或前向恢复：** 候选失败不推进父分支；修复对应 owner Ticket 后重建候选。隔离库失败只清理本次资源，不触达外部卷。
- **不可逆操作与批准点：** 运行隔离容器需实施阶段授权；真实数据库、部署、提交推送、Tag 均不在授权范围。
- **收缩条件：** 14 项合同全部 covered，required E2E 通过，父分支包含全部实现提交并有完整 Lead Evidence。

## 10. 验收标准

- [ ] `AC-001` 至 `AC-014` 均有最终可重复证据。
- [ ] 发布、Shell、Node、后端测试和打包门禁通过。
- [ ] 隔离 MySQL 8.4 按六文件顺序初始化成功，关键不变量通过。
- [ ] 失败门禁被受控证明有效，无法运行项未被误报为成功。
- [ ] 未连接或修改真实开发/生产数据库，未泄露 secret。
- [ ] Lead Evidence、实现提交、候选/父分支结果和清理记录完整。
