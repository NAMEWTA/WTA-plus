---
schema_version: 3
artifact: ticket
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
id: T-03
title: 将 SQL 合同测试迁移到唯一基座
status: done
planning_depth: deep
planning_depth_reason: 跨父仓库与 Java 测试套件改变 SQL 输入接缝，必须避免静默跳过和第三份测试副本
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-010]
owner: codex:/root
expected_changes:
  - "<Path>release-artifacts/tests/mysql-sql-contract.test.mjs</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-prod.yml</Path>"
writable_paths:
  - "<Path>release-artifacts/tests/mysql-sql-contract.test.mjs</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-prod.yml</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/*.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/**</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/**</Path> => T-03"
---

# Ticket T-03：将 SQL 合同测试迁移到唯一基座

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/03-migrate-sql-test-consumers.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 让所有 SQL 内容合同和 Java/MySQL 集成测试读取父仓库六份唯一 SQL，同时保持后端普通构建可独立运行。
- **可观察产出：** 活动 Java 测试和资源不再解析 `script/sql`；父级纯 SQL 合同直接读取发布基座；需要 SQL 的 Java 测试接受显式 SQL 根，缺失时明确失败。
- **来源：** `US-008`、`AC-010`、`CODE:ruoyi-vue-plus-namewta/ruoyi-admin/src/test/**`。
- **当前事实：** 多个迁移、OSS、Nacos、密码、OpenAPI、通知测试硬编码后端 `script/sql`；部分测试同时承担纯文本合同和真实 MySQL 行为。
- **Planning Depth 原因：** 这是跨仓库测试接缝迁移，错误处理会导致 CI 静默少测或制造新的 SQL 副本。

## 2. 决策状态

### 已锁定决策

- 纯 SQL 内容、分类、菜单、权限和标记合同尽可能上移到父仓库 Node 测试。
- 仍需 Java Service、Mapper 或真实 MySQL 的测试通过统一解析器读取显式 SQL 根。
- 显式 JVM 属性优先，其次显式环境变量；聚合工作区路径只能作为本地便利，不得静默跳过。
- 禁止把 SQL 复制到 `src/test/resources`。

### 已采用的低影响假设

- 统一测试辅助类放在后端测试源码中，由现有相关测试复用；具体类名沿用测试包命名惯例。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| SQL 测试分类、父级合同、统一 SQL 根解析、Java 消费者迁移、配置注释和资源路径 | 现有 JUnit/Testcontainers/Node 测试模式、六份唯一 SQL | 改业务 SQL 内容、删除旧 script、修改 Maven 公共模块、访问生产数据库 |

## 4. 要构建什么

在聚合工作区运行测试时，纯 SQL 规则直接验证六份发布基座；需要 Java 或 MySQL 的测试通过一个明确输入定位相同文件。输入不存在、不是预期六文件目录或文件缺失时，测试明确失败并打印可操作的路径说明。后端不依赖 SQL 的普通编译和测试继续独立运行。

## 5. 实现契约

- **入口或接缝：** 父仓库 Node SQL 合同、后端测试 SQL 根解析器、现有 JUnit 和 MySQL 集成测试。
- **输入与输出：** 输入为包含六份固定文件的 SQL 根；输出为合同断言和测试退出状态。
- **公共接口变化：** 无生产 Java API；新增的仅为测试输入合同。
- **不变量：** 所有测试读取同一六文件，不生成副本；required SQL 缺失时不跳过；普通非 SQL 测试不被父仓库路径绑死。
- **状态或数据流：** 父仓库 SQL -> Node 静态合同 / 显式测试输入 -> Java 或 MySQL 集成断言。
- **错误与失败行为：** 路径缺失、文件清单不完整、SQL 标记不存在时明确失败；外部服务不可用按既有属性门控分类，不吞错。
- **兼容要求：** 保持 Maven 普通 compile/package 与非 SQL 单测独立；父仓库 CI 负责传入 required SQL 根。
- **安全与隐私要求：** 输入只允许仓库 SQL 目录；不得把数据库密码或现场路径写入测试资源。

## 6. 执行路线

1. 盘点并分类所有活动 `script/sql` 读取点，形成纯文本、Java 行为、真实 MySQL 三类清单。
2. 先建立父仓库 SQL 内容合同和统一 Java 测试输入解析的失败用例。
3. 上移纯 SQL 断言，避免同一语义在 Java 和 Node 重复维护。
4. 分批迁移剩余 Java/MySQL 测试、OSS owner 资源和 SnailJob SQL 导航注释。
5. 对缺失属性、错误目录和完整目录分别验证；确认没有测试被静默禁用。
6. 运行父级 Node 测试、后端定向测试和非 SQL 回归，扫描旧引用为零。

## 7. 路径访问契约

- **预计修改点/可写范围：** 与 frontmatter 一致；T-03 独占后端相关测试树写入。
- **只读上下文：** 六份发布 SQL 和旧 SQL 仅用于迁移比对，不修改内容。
- **共享路径：** 后端测试树由 T-03 唯一 owner；T-04 只删除 `script/`，不得改测试。
- **保留或不动：** 生产 Java、Maven 依赖图、业务表与现场数据库。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Node + JUnit | 传入正确 SQL 根运行父级和后端定向测试 | 所有合同读取六份发布 SQL 并通过 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-03.md</Path>` |
| 失败路径 | SQL 根解析 | 不传或传错误目录运行 required 测试 | 明确失败并指出配置方式，不跳过 | 同上 |
| 回归 | Maven + 路径扫描 | 运行非 SQL 单测、package 与 `rg` | 普通后端门禁可运行，活动测试无旧路径 | 同上 |

- **Workspace checks：** Node 定向测试、后端相关 JUnit、`./mvnw test` 的适用范围和 `./mvnw clean package -DskipTests`。
- **E2E disposition：** `not-required`：本 Ticket 迁移测试输入；真实 MySQL 顺序导入和整体验收由 T-06 执行。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** 后端实现提交、父仓库集成提交、输入参数、父分支结果和旧引用扫描。

## 9. 发布、迁移与恢复

- **迁移顺序：** 建立新接缝 -> 上移纯合同 -> 迁移 Java 消费者 -> 扫描旧引用 -> 允许 T-04 删除。
- **兼容窗口：** 本 Ticket 执行期间旧 SQL 仍存在但只读；完成后活动测试只能读新基座。
- **监控信号：** 旧引用数量、required 输入失败、测试执行数量和跳过原因。
- **回滚或前向恢复：** 批次失败时恢复该批测试消费方式并保留已通过批次；不得复制 SQL 规避路径错误。
- **不可逆操作与批准点：** 不删除旧 SQL、不写真实业务库；跨子仓库实现提交需授权。
- **收缩条件：** 活动测试、测试资源和配置注释中的 `script/sql` 引用为零。

## 10. 验收标准

- [ ] `AC-010` 有正常、失败和回归证据。
- [ ] 纯 SQL 合同由父仓库唯一基座驱动；Java required SQL 输入明确且不可静默跳过。
- [ ] 后端普通非 SQL 测试与打包能力保持。
- [ ] 活动测试、测试资源和配置注释无旧 SQL 路径。
- [ ] Evidence、实现提交和父分支结果完整。
