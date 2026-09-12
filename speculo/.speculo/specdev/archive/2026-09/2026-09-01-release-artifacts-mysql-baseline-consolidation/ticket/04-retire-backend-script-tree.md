---
schema_version: 3
artifact: ticket
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
id: T-04
title: 删除后端 script 并收缩为 MySQL-only
status: done
planning_depth: deep
planning_depth_reason: 整目录删除包含 SQL、工作流资产和旧部署脚本，并永久退出三种非 MySQL 方言
ready: true
risk: high
blocked_by: [T-01, T-02, T-03]
contract_ids: [AC-004, AC-005, AC-006]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/script/**</Path>"
  - "<Path>release-artifacts/tests/backend-script-retirement-contract.test.mjs</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/**</Path>"
  - "<Path>release-artifacts/tests/backend-script-retirement-contract.test.mjs</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/**</Path>"
  - "<Path>release-artifacts/workflow/leave/**</Path>"
  - "<Path>release-artifacts/tests/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/**</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/script/**</Path> => T-04"
---

# Ticket T-04：删除后端 script 并收缩为 MySQL-only

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/04-retire-backend-script-tree.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 在新 owner、测试接缝和工作流资产全部可用后，删除后端整个 `script/`，明确只支持 MySQL。
- **可观察产出：** `<Path>ruoyi-vue-plus-namewta/script/</Path>` 不存在；Oracle、PostgreSQL、SQL Server 文件没有迁移副本；旧 Docker/bin/生成器也一并退出。
- **来源：** `US-004`、`US-005`、`US-006`、`AC-004`、`AC-005`、`AC-006`、`USER-DECISION:非 MySQL 直接删除无需迁移`。
- **当前事实：** 旧目录仍包含六份 MySQL 来源、三种非 MySQL 方言、Workflow JSON、Docker/bin 和追加式 SQL 辅助脚本。
- **Planning Depth 原因：** 这是不可通过运行时兼容层弥补的目录级收缩，必须先证明所有保留资产已接管。

## 2. 决策状态

### 已锁定决策

- 删除整个 `<Path>ruoyi-vue-plus-namewta/script/</Path>`，不保留 README、占位目录或兼容链接。
- Oracle、PostgreSQL、SQL Server 直接删除，不迁移、不归档、不生成替代文件。
- 六份 MySQL SQL 仅在 T-01 摘要相等并被跟踪后删除旧副本。
- 六个 Workflow JSON 仅在 T-02 等价验证后删除旧副本。

### 已采用的低影响假设

- 新增父级收缩合同测试，持续断言旧目录与三种方言路径不存在。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 整个后端 script 删除、收缩合同测试、非 MySQL 零残留断言 | T-01/T-02/T-03 已集成的新资产和新测试接缝 | 迁移非 MySQL、修改六份新 SQL、删除历史 SpecDev 工件、部署或数据库写入 |

## 4. 要构建什么

当上游三个 Ticket 的证据全部成立后，维护者可以删除整个后端 `script/`。门禁会证明所有需要保留的 MySQL SQL 和 Workflow JSON 已由父仓库接管，活动测试不再消费旧路径，并且没有在其他目录偷偷保留非 MySQL 方言。

## 5. 实现契约

- **入口或接缝：** 文件系统目录、Git 删除集和父级 Node 收缩合同。
- **输入与输出：** 输入为已通过的 T-01/T-02/T-03 Evidence；输出为后端 `script/` 零存在。
- **公共接口变化：** 旧脚本路径整体退出；无 HTTP/Java API 变化。
- **不变量：** 父仓库六份 SQL 和六个 Workflow JSON 内容不变；不删除后端 `script/` 外文件。
- **状态或数据流：** 先接管、再迁移消费者、最后删除旧 owner。
- **错误与失败行为：** 任一上游摘要、跟踪、测试或消费者扫描不通过时停止删除。
- **兼容要求：** 不提供旧路径符号链接或复制脚本；所有活动消费者必须在收缩前迁移。
- **安全与隐私要求：** 删除不触达本地运行数据、数据库卷或服务器路径。

## 6. 执行路线

1. 复核 T-01/T-02/T-03 Evidence、提交包含关系和当前工作树，确认新资产完整。
2. 建立会在旧目录或非 MySQL 副本存在时失败的收缩合同。
3. 删除后端 `script/` 全目录；三种非 MySQL 方言不做任何迁移。
4. 检查 Git 删除清单没有越出授权目录，并重算保留资产摘要。
5. 运行后端测试、打包、父级合同和活动路径扫描。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅后端旧目录删除和独立父级合同测试。
- **只读上下文：** 新 SQL、新 Workflow JSON、迁移后的测试只用于收缩前后验证。
- **共享路径：** 整个旧目录由 T-04 唯一 owner；其他 Ticket 不得同时写入。
- **保留或不动：** 两个产品源码、发布 Docker 运行资产和所有历史归档工件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 文件/Git 合同 | 断言旧目录不存在并检查删除清单 | 整个 `script/` 删除且未越界 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-04.md</Path>` |
| 失败路径 | 收缩前置门 | 模拟新资产缺失或旧消费者存在 | 删除步骤拒绝继续并指出前置条件 | 同上 |
| 回归 | Maven + Node + 摘要 | 后端测试/打包、父级合同、保留资产摘要 | 构建通过，新 owner 内容不变 | 同上 |

- **Workspace checks：** 路径与 Git 删除检查、Node 合同、后端 Maven 定向和打包回归。
- **E2E disposition：** `not-required`：本 Ticket 只收缩仓库目录；真实 MySQL 与完整发布链由 T-06 验收。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** 后端删除提交、父仓库合同提交、父分支结果和新资产摘要。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 SQL 接管 -> T-02 Workflow 接管 -> T-03 消费者迁移 -> T-04 删除。
- **兼容窗口：** 删除提交集成后立即结束双源窗口，不保留旧路径兼容。
- **监控信号：** 旧目录存在性、非 MySQL 文件扫描、Git 删除范围、保留资产摘要。
- **回滚或前向恢复：** 若删除后发现遗漏，先停止后续 Ticket；在未发布范围内成对回滚后端和父级提交，或修正新 owner，禁止只恢复部分旧目录形成双源。
- **不可逆操作与批准点：** 删除整个目录前必须取得用户实现批准；不包含远端推送、部署或数据库操作。
- **收缩条件：** T-01/T-02/T-03 均完成且父分支包含其提交；所有活动消费者旧路径为零。

## 10. 验收标准

- [ ] `AC-004`、`AC-005`、`AC-006` 全部有证据。
- [ ] 后端 `script/` 不存在，三种非 MySQL 方言无迁移副本。
- [ ] 新 SQL、Nacos 和 Workflow 资产摘要未因删除改变。
- [ ] Git 删除未越出授权目录，后端与父级回归通过。
- [ ] Evidence、实现提交和父分支结果完整。
