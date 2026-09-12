---
schema_version: 3
artifact: ticket
change: 2026-08-28-retire-runtime-code-generator
id: T-03
title: 永久清除生成器数据库表与菜单权限
status: done
planning_depth: deep
planning_depth_reason: 在 append-only SQL 中执行不可逆表数据删除和授权菜单清理，并要求可丢弃 MySQL 8.4 fresh 初始化验证
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-005, AC-006, AC-007]
owner: codex:/root
expected_changes: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-gen/**</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path> => T-03", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> => T-03"]
---

# Ticket T-03: 永久清除生成器数据库表与菜单权限

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/03-hard-retire-generator-database.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 通过 NAMEWTA append-only SQL 把生成器元数据表、九个菜单及全部角色关系从最终 MySQL 8.4 基座永久移除。
- **可观察产出：** fresh 初始化成功；`gen_table_column`、`gen_table`、九个 menu_id、关联 `sys_role_menu`、`tool:gen:*` 和 `tool/gen` 当前数据均不存在。
- **来源：** `US-002`、`US-004`、`AC-005` 至 `AC-007`、`ADR-002`、`DEC-004`、`DEC-005`、用户永久删除确认。
- **当前事实：** 冻结 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 创建两张表、系统工具目录、两个页面菜单和六个功能权限；Docker 依次执行冻结 SQL、NAMEWTA DDL、NAMEWTA DML。
- **Planning Depth 原因：** 操作会不可恢复地删除数据/schema 和授权投影，且必须证明 fresh 初始化最终状态。

## 2. 决策状态

### 已锁定决策

- 只在两个现有 NAMEWTA 文件末尾追加；冻结上游 SQL 不修改，历史前缀不改写。
- DDL 先删除 `gen_table_column`，再删除 `gen_table`；不备份、不归档、不创建兼容视图。
- DML 先删除九个固定 menu_id 的全部 `sys_role_menu`，再删除菜单；系统工具目录仅在确认无非目标子菜单时删除。
- 固定 ID：`1761400000000000003`、`1761400000000000115`、`1761400000000000116`、`1761400000000001055` 至 `1761400000000001060`。

### 已采用的低影响假设

- 实施前静态重验冻结基线中固定 ID 的父子、类型、component 和 perms；若已出现非生成器子菜单，停止并登记偏差。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| NAMEWTA DDL/DML 末尾硬退役块与最终状态查询 | MySQL 8.4 Docker fresh 初始化顺序、固定基线 ID | 冻结 SQL改写、生产 upgrade、备份、恢复、其他数据库方言、代码删除 |

## 4. 要构建什么

维护者用空数据目录启动既有 MySQL 8.4 Compose 时，上游基线先创建旧对象，随后 NAMEWTA 追加块将其收缩到目标最终状态。查询者不能再找到两张生成器表、九个菜单或角色关系。DML 重复执行保持最终状态；DDL 按基座初始化合同执行一次，不承诺重复执行。若系统工具目录被新能力复用，清理必须停止而非宽泛删除。

## 5. 实现契约

- **入口或接缝：** `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>`、`DML.sql` 与 Docker MySQL 8.4 initdb 顺序。
- **输入与输出：** 输入为冻结基线创建的表和菜单；输出为无生成器 schema/权限的最终数据库。
- **公共接口变化：** 数据库 schema 删除 `gen_table_column` 和 `gen_table`；授权投影删除 `tool:gen:*` 与 `tool/gen` 菜单。
- **不变量：** DDL/DML 分类正确且只追加；角色关系先删；子表先删；其他 client、角色、菜单、权限和表不变。
- **状态或数据流：** `10-ry-vue.sql` 创建旧基线，`50-namewta-ddl.sql` 删除表，`60-namewta-dml.sql` 删除关系和菜单。
- **错误与失败行为：** 前置事实不符或 SQL 失败时丢弃数据目录、修正脚本并 fresh 重建；禁止扩大 ID/pattern 删除范围。
- **兼容要求：** 不适用：基座无旧版本、生产数据或 rollback consumer。
- **安全与隐私要求：** 删除全部角色关系且只能命中九个固定 ID；以查询证明其他授权未被误删。

## 6. 执行路线

1. 重验冻结基线的两张表、九个菜单、父子关系和系统工具无其他子菜单，并保存 append 前缀哈希/diff 基线。
2. 在 DDL.sql 末尾追加带唯一变更标识的删除块，按子表后主表顺序物理删除。
3. 在 DML.sql 末尾追加带唯一变更标识的删除块，先删固定 ID 角色关系，再删功能、页面和空目录菜单。
4. 静态 review 只追加、分类、顺序、重复执行和无备份/恢复语义。
5. 由 Lead 在 current workspace 或 parent-candidate 使用空数据目录执行 MySQL 8.4 fresh 初始化和最终查询，再记录实现与父分支证据。

## 7. 路径访问契约

- **预计修改点：** 两个 NAMEWTA SQL 文件末尾。
- **可写范围：** 仅 frontmatter 两个 SQL；任何其他迁移文件需要偏差审批。
- **只读上下文：** 冻结基线、Docker Compose 和后端生成器源码。
- **共享路径：** DDL.sql 与 DML.sql 均由 T-03 唯一修改。
- **保留或不动：** `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 及 PostgreSQL、Oracle、SQL Server 变体。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | MySQL 8.4 fresh init | 以临时 `NAMEWTA_DATA_ROOT` 和测试凭据启动 Compose mysql，等待 healthcheck，查询 `information_schema`、`sys_menu`、`sys_role_menu` | 初始化成功；两表、九菜单和关系均为零 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-03.md</Path>` |
| 失败路径 | 前置/范围与日志 | review 固定 ID 映射、容器 init 日志和退出状态；模拟依据仅限可丢弃环境 | 非目标子菜单或 SQL 失败会阻塞，不产生宽泛删除 | 同上 |
| 回归 | append-only 与旁观数据 | diff 历史前缀；比较非目标菜单/角色计数和代表性 ID | 只在文件末尾追加，冻结基线和非目标授权不变 | 同上 |

- **Workspace checks：** source/current workspace 先做 SQL 静态顺序、固定 ID、append-only 和 Compose 配置检查。
- **E2E disposition：** required：必须跨 Docker init、MySQL schema 与授权数据证明不可逆最终状态。
- **E2E owner/environment：** Lead / current-workspace（current 模式）或 parent-candidate（required 模式）；空数据目录、MySQL 8.4.x、既有 initdb 顺序和最终 SQL 查询。
- **Integration evidence：** 非空 implementation/source commit、parent before、适用 candidate/result SHA、fresh 环境定位（不含秘密）、命令退出码和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** DDL 子表后主表；DML 角色关系后六个功能权限、两个页面菜单、系统工具目录；Docker 保持 DDL 后 DML。
- **兼容窗口：** 不适用：用户明确排除兼容和生产。
- **监控信号：** 不适用：无生产运行期；fresh init healthcheck、日志和最终查询是唯一信号。
- **回滚或前向恢复：** 无数据回滚；失败时删除可丢弃数据目录、修正脚本并完整重建。不得从备份恢复生成器数据。
- **不可逆操作与批准点：** 两张表及历史数据永久丢失；用户已明确两次确认物理/永久删除，实际执行只允许在可丢弃环境，implementation commit/integration 仍需 I/Goal Plan 授权。
- **收缩条件：** 两表、九个固定菜单、关系、`tool:gen:*` 和 `tool/gen` 查询均为零，其他授权保持。

## 10. 验收标准

- [x] `AC-005`：九个固定菜单、全部角色关系、生成器 perms/component 和空系统工具目录不存在。
- [x] `AC-006`：`gen_table_column` 与 `gen_table` 均不存在，删除顺序正确且无备份/恢复面。
- [x] `AC-007`：可丢弃 MySQL 8.4 fresh 初始化成功并满足 AC-005/006。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-03.md</Path>`。
- [x] 修改未超出 `writable_paths`，shared path 由 T-03 修改。
- [x] 已形成非空 implementation/source commit，父分支验证/result、required E2E 与包含关系已记录。
- [x] 未发生未批准偏差；Ticket、Map 和 Evidence 状态一致。
