# Change ADR: Admin 运行能力收敛

## ADR-001：OpenAPI 保持默认关闭，由受管环境显式启用

- **状态：** accepted
- **决定：** `openapi.enabled` 的代码与公开发布样例默认值继续为 `false`。发布 Compose 必须显式透传 `OPENAPI_ENABLED`、`OPENAPI_KEK_VERSION`、`OPENAPI_KEK`；目标开发环境通过被忽略且权限受限的配置提供合法值。
- **原因：** OpenAPI 包含机器身份、长期凭据和全局权限聚合，不能因修复开发环境而扩大所有安装的默认攻击面。
- **后果：** 未配置环境继续没有管理 Controller；显式启用但安全配置无效时应用启动失败。
- **来源：** `DIAG-001`、`USER-DECISION:2026-09-01-execute-confirmed-plan`。

## ADR-002：以新的幂等尾部块收敛 fresh 与 upgrade

- **状态：** accepted
- **决定：** 不修改已经提交或执行过的历史 SQL。只在 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 和 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 末尾追加新的能力收敛块，并让其同时处理全新初始化和当前混合状态升级。
- **原因：** PERSIST-006 要求双文件 append-only；当前环境证明仅依赖历史块执行顺序不足以保证 upgrade 最终态。
- **后果：** 新块必须有固定 ID 前置校验、可重复执行合同、隔离 MySQL fresh/upgrade/replay 验证和部署后最终态查询。
- **来源：** `DIAG-001`、工程规范 `PERSIST-006`。

## ADR-003：菜单名称与位置覆盖历史 change

- **状态：** accepted
- **决定：** OpenAPI 系统管理菜单显示为“OpenAPI管理”；Nacos 固定菜单显示为“Nacos配置中心”，父菜单改为“系统监控”，排序在现有 AI 控制台之后。component、path 和 permission 保持不变。
- **原因：** 用户最新明确决定高于历史 change 的“应用开放管理”和“系统管理 > 配置中心”合同。
- **后果：** 新 DML 在固定 ID 上前向更新，不创建别名或第二个菜单；历史 change 保留为旧决策证据。
- **来源：** `USER-DECISION:2026-09-01-menu-corrections`。

## ADR-004：生成器数据库面完成物理退役

- **状态：** accepted
- **决定：** 在确认两表当前为空后，删除固定九条生成器菜单及角色关系，物理删除 `gen_table_column` 和 `gen_table`。不保留兼容表、页面或恢复入口；备份要求由后续 `ADR-005` 覆盖。
- **原因：** 运行代码和前端已经物理退役，残留数据面继续制造不可用导航和虚假产品能力。
- **后果：** 数据库写入属于破坏性批准点；应用回滚不自动恢复表，恢复策略以 `ADR-005` 为准。
- **来源：** `USER-DECISION:2026-09-01-execute-confirmed-plan`、历史 generator retirement Spec。

## ADR-005：目标开发库本次不创建迁移前备份

- **状态：** accepted
- **决定：** 用户在 G1 完成后明确要求“无需备份”。本次仅对已确认的 NAMEWTA 开发库执行新 DDL/DML，不创建数据库备份；仍必须在写入前确认两张生成器表均为空、对象身份匹配、固定菜单无冲突，并禁止 `--force`。
- **原因：** 用户作为目标开发环境 owner 明确接受空生成器表物理删除后的无数据级回滚风险。
- **后果：** `gen_table`、`gen_table_column` 及迁移前菜单状态没有本 change 创建的恢复副本；失败时只能停止 rollout、修正冲突后前向重放。非空表、身份不匹配或 SQL 前置失败仍是硬停止条件。
- **边界：** 只覆盖本次目标开发库的备份 Gate，不扩展到生产/CDE，不授权 volume 删除，不降低 OpenAPI secret、双实例逐个滚动或最终态验证要求。
- **来源：** `USER-DECISION:2026-09-01-no-backup`。
