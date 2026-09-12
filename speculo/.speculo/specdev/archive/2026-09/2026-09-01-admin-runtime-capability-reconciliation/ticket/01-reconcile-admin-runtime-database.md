---
schema_version: 3
artifact: ticket
change: 2026-09-01-admin-runtime-capability-reconciliation
id: T-01
title: 收敛 OpenAPI、Nacos 与生成器数据库最终态
status: done
planning_depth: deep
planning_depth_reason: 修改共享初始化 SQL、执行可重放升级迁移并物理删除已退役表，涉及固定菜单 ID、数据完整性、冲突前置失败和不可逆恢复边界。
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-013]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/adminruntime/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/password/PasswordMigrationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/OpenApiCredentialSqlContractTest.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/adminruntime/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/password/PasswordMigrationUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/credential/OpenApiCredentialSqlContractTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/nacos/menu/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> => T-01"
---

# Ticket T-01: 收敛 OpenAPI、Nacos 与生成器数据库最终态

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-reconcile-admin-runtime-database.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 用新的 append-only 尾部 DDL/DML 块把 fresh、当前混合 upgrade 和 replay 收敛到同一数据库最终态。
- **可观察产出：** 六条 OpenAPI 菜单完整存在；主菜单显示为“OpenAPI管理”并位于系统管理；固定 Nacos 入口显示为“系统监控 > Nacos配置中心”；九条生成器菜单、对应角色关系和两张生成器表均不存在。
- **来源：** `US-002` 至 `US-005`、`AC-005` 至 `AC-011`、`AC-013`、`ADR-002` 至 `ADR-004`、`DIAG-001`、`USER-DECISION:2026-09-01-menu-corrections`。
- **当前事实：** 历史 SQL 分别定义了生成器删除、OpenAPI 菜单和 Nacos 菜单，但目标库只应用了其中一部分；现有 OpenAPI/Nacos 历史块又保留了旧名称与旧父菜单，不能修改历史内容来伪装已升级环境。
- **当前事实补充：** 实施期反向验证确认当前 backend HEAD 已因后续 OSS SQL 演进超过旧 OpenAPI 前缀常量，旧测试在本 change 前即失败；T-01 将保护边界移动到实施前不可变 HEAD 的完整 SQL 字节，不修改或放宽哈希算法。
- **全量门禁补充：** `PasswordMigrationUnitTest` 固定的 15,370 字节 DML 前缀也已在实施前 backend HEAD 上漂移：旧期望为 `637d6ab...`，HEAD 与工作树均为 `698675a...`；T-01 只同步该固定长度的真实 HEAD 哈希，不修改密码迁移 SQL、长度或算法。
- **Planning Depth 原因：** 本 Ticket 触及 schema 和权限投影，并含无备份表删除；必须证明 preflight、重放、冲突状态与前向恢复边界。

## 2. 决策状态

### 已锁定决策

- 只在 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 与 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 尾部追加新逻辑块，不改历史块或冻结上游 SQL。
- OpenAPI 固定六个 menu ID 为 `2094360621561675776..2094360621561675781`；主菜单最终名为“OpenAPI管理”，父菜单为系统管理 `1761400000000000001`，component/path/permissions 保持既有合同。
- Nacos 固定 menu ID `2094360621561675790`；最终名为“Nacos配置中心”，父菜单为系统监控 `1761400000000000002`，`order_num=8`，path/component/permission 保持不变。
- 生成器只精确删除九个冻结 ID 及其角色关系；不使用名称、路径或前缀的宽泛删除。
- 任一固定 ID、component、permission、父菜单或生成器子树处于非允许冲突状态时，DML 在目标写入前失败。
- 不向普通角色插入 OpenAPI 或 Nacos 的 `sys_role_menu` 关系。

### 已采用的低影响假设

- 当前两张生成器表仍为空；目标迁移前再次查询，非空则停止并升级为数据处置决定。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 新 DDL/DML 收敛块、静态合同测试、隔离 MySQL fresh/upgrade/replay/conflict 测试、用户明确无备份的开发库升级 | 历史固定 ID、`sys_menu`/`sys_role_menu` schema、现有 OpenAPI/Nacos frontend component | 修改历史 SQL、恢复生成器、自动角色授权、操作 CDE/生产库、删除非固定菜单 |

## 4. 要构建什么

维护者无论对 fresh 初始化后的历史状态、当前缺 OpenAPI 且残留生成器的混合状态，还是已完成状态执行新块，都得到相同菜单/schema 结果。若固定 ID 已被其他能力占用或目标 component/permission 存在重复，迁移先失败且不删除目标数据。开发库按用户明确的无备份决定执行，但仍以 0 行、对象身份和冲突前置为硬门，完成后用最终态查询证明所有目标计数和属性。

## 5. 实现契约

- **入口或接缝：** NAMEWTA DDL/DML 新逻辑标识块；Java SQL contract/MySQL integration test；目标开发库升级会话。
- **输入与输出：** fresh、历史完整、当前混合、已收敛或冲突的 `sys_menu`/`sys_role_menu`/`gen_table*` 状态 -> 目标最终态或写入前明确失败。
- **公共接口变化：** 无；只修正数据库 schema 与动态菜单投影。
- **不变量：** 稳定 menu ID、OpenAPI component/path/permissions、Nacos path/component/permission 不变；普通角色授权不扩大；非目标菜单数据不变。
- **状态或数据流：** 冲突 preflight -> 生成器角色/菜单精确删除 -> OpenAPI 缺失行补齐/主菜单收敛 -> Nacos 缺失行补齐或历史行收敛 -> 最终态查询。
- **错误与失败行为：** 冲突通过约束哨兵在首个目标 DML 前失败；生成器表非空、身份不匹配或任一 SQL 前置失败时外部迁移停止；不得用 `--force` 跳过错误。
- **兼容要求：** fresh 可继续执行完整历史 SQL；upgrade 只执行新块；新块重放成功且结果稳定。
- **安全与隐私要求：** 不读取或输出用户凭据；Evidence 只记录脱敏计数、目标定位和用户无备份决定，不记录连接凭据或数据正文。

## 6. 执行路线

1. 先新增静态合同与隔离 MySQL 场景，使新逻辑标识、最终属性、重放和冲突前失败按预期红灯。
2. 追加幂等 DDL 块：检查生成器行数安全条件，再按依赖顺序 `DROP TABLE IF EXISTS`。
3. 追加单一 DML 收敛块：完整 preflight 后精确删除生成器面、补齐 OpenAPI、移动并重命名 Nacos，不创建普通角色授权。
4. 运行静态测试和隔离 MySQL 的 fresh/upgrade/replay/conflict 矩阵，核对历史前缀未变化。
5. 形成 backend implementation commit 和聚合 direct-parent checkpoint。
6. 登记当前发布定位与无备份前向恢复边界；再次确认生成器表为 0 行、对象身份匹配后只执行新块。
7. 运行开发库最终态诊断，记录脱敏 Evidence 后开放 T-02。

## 7. 路径访问契约

- **预计修改点：** 两个 NAMEWTA SQL 文件、新的 adminruntime 迁移测试包、把既有 append-only 哈希门移动到实施前完整 SQL HEAD 的 OpenAPI contract test，以及同步已在实施前 HEAD 失真的密码迁移固定长度前缀哈希。
- **可写范围：** 仅 frontmatter `writable_paths`；任何现有 OpenAPI/Nacos 业务代码或前端修改都必须停止。
- **只读上下文：** 冻结基线、相邻 SQL contract tests、前端 component 注册。
- **共享路径：** 两个 append-only SQL 文件唯一 owner 均为 T-01。
- **保留或不动：** `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`、OpenAPI 凭据数据、Nacos 服务端配置和普通角色授权。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 静态最终合同 | JUnit SQL contract | `./mvnw -pl ruoyi-admin -am test -Dtest='*AdminRuntimeCapability*' -Dsurefire.failIfNoSpecifiedTests=false` | 新标识、固定 ID/属性、append-only、无普通角色 grant 全部通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| fresh/upgrade/replay | 隔离 MySQL | 同一测试类带一次性 JDBC 参数运行 | 三种状态收敛，重复执行结果不变 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 冲突失败 | 隔离 MySQL | 注入固定 ID/component 冲突后执行 DML | 首个目标写入前失败，目标和非目标数据未改变 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 开发库升级 | DBX/MySQL + final diagnostic | 记录无备份批准、复核 0 行/身份、执行新块、运行 `<Path>{roots.state}/specdev/changes/{change}/diagnostics/runtime-capability-state.sql</Path>` | 四项最终态为真，OpenAPI/Nacos 无普通角色新增 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 回归 | 相邻 SQL tests + Maven reactor | OpenAPI/Nacos/retirement 定向测试和 `./mvnw -pl ruoyi-admin -am test` | 历史合同与 admin 模块保持绿色 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |

- **Workspace checks：** Lead 在 current workspace 运行定向 JUnit、隔离 MySQL、admin reactor 和 diff/secret 检查。
- **E2E disposition：** required：目标行为是数据库真实升级与动态菜单投影，静态解析不能替代真实 MySQL fresh/upgrade/replay 和当前开发库最终态。
- **E2E owner/environment：** Lead / current-workspace；隔离 MySQL 用于迁移矩阵，目标开发 MySQL 仅在无备份批准与写入前硬检查后用于 upgrade 验收。
- **Integration evidence：** backend child implementation SHA、aggregate parent-before/result SHA、gitlink 包含关系、用户无备份批准和脱敏查询结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** 静态/隔离测试 -> implementation commit -> 当前目标定位 -> 无备份批准登记 -> 表行数/身份复核 -> 新 DDL -> 新 DML -> 最终态与权限查询。
- **兼容窗口：** 代码和前端已兼容最终菜单；数据库升级完成前 OpenAPI 菜单仍缺失、Nacos 仍在旧位置，窗口只允许在受控维护期存在。
- **监控信号：** SQL exit status、目标表/菜单计数、固定字段、role-menu 增量、应用菜单缓存刷新结果。
- **回滚或前向恢复：** 本次不创建数据库备份，无法恢复迁移前 `sys_menu`/`sys_role_menu` 或生成器表；失败时保持 OpenAPI disabled，修正冲突后前向重放。
- **不可逆操作与批准点：** 用户已明确批准无备份执行；删除生成器表和目标库写入仍只在目标身份、0 行复核和 SQL preflight 同时通过后执行，任一失败即停止。
- **收缩条件：** 九个生成器菜单/角色关系计数为 0，`gen_table*` 不存在，代码/前端扫描仍无生成器运行面。

## 10. 验收标准

- [ ] `AC-005` 至 `AC-011`、`AC-013` 全部有静态与真实 MySQL Evidence。
- [ ] fresh、当前混合 upgrade 和 replay 收敛到同一固定最终态；冲突在任何目标写入前失败。
- [ ] 已记录用户无备份批准与前向恢复边界，生成器表删除前仍为 0 行且身份匹配。
- [ ] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`。
- [ ] 实际项目修改未超出 `writable_paths`，两个 shared SQL path 仅由 T-01 修改。
- [ ] backend 非空 implementation commit 与 aggregate direct-parent result 已记录并核对包含关系。
- [ ] required E2E 由 Lead 在 current workspace/目标开发环境完成。
- [ ] 未发生未批准的范围、契约、数据或发布偏差。
