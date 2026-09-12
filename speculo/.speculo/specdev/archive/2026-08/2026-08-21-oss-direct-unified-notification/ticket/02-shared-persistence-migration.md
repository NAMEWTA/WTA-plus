---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-02
title: OSS 与通知共享持久化迁移
status: done
planning_depth: deep
planning_depth_reason: 新增并回填项目自有表、索引、权限与动态菜单，是共享且有数据安全影响的数据库迁移。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-005, AC-008, AC-009, AC-010, AC-011, AC-025, AC-029, AC-032]
owner: cursor-agent
expected_changes: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/ossnotify/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/ossnotify/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>", "<Path>plus-ui-namewta/src/views/monitor/**</Path>"]
shared_paths: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"]
shared_path_owners: ["<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path> => T-02", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> => T-02"]
---

# Ticket T-02: OSS 与通知共享持久化迁移

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/02-shared-persistence-migration.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 为 TEMP/引用和两层通知监控提供唯一增量 schema 与权限菜单基线。
- **可观察产出：** 新旧数据库均可获得可索引生命周期字段、`sys_oss_ref`、两张 notify 表及 monitor 菜单权限；历史 OSS 不会被误清理。
- **来源：** `ADR-005`、`ADR-008`、工程 SQL 规范、`AC-005/008/009/010/011/025/029/032`。
- **当前事实：** NAMEWTA 数据库改动只能追加到 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 与 `DML.sql`。
- **Planning Depth 原因：** schema、历史数据回填和权限菜单是共享迁移，错误可能不可逆删除业务文件或暴露监控功能。

## 2. 决策状态

### 已锁定决策

- `sys_oss` 增加独立 TEMP/expiry 可索引字段；历史行保守回填为非 TEMP。
- `sys_oss_ref.ref_type` 保存实际物理表名，`ref_id` 保存真实主键；唯一粒度 `(oss_id, ref_type, ref_id)`。
- notify 两表永久明文保存，`client_pk` 可空且仅为来源审计，不做过滤键。
- 所有项目自有表含 version/create_dept/create_time/create_by/update_time/update_by/del_flag；不编辑 `ry_vue.sql`。

### 已采用的低影响假设

- 索引围绕 expiry/ref/request/biz/channel/status/providerMessageId/create_time，最终名称遵循仓库既有风格。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| DDL、回填、索引、表注释、notify 菜单和权限 DSL | 现有 sys_oss 与 monitor 菜单惯例 | Java Mapper、前端页面、外键到多态业务表、新 SQL 文件 |

## 4. 要构建什么

升级数据库时先扩展字段和表，再对历史 OSS 作非 TEMP 保守回填，最后建立约束与索引；全新数据库运行相同追加脚本得到一致结构。DSL 增加通知监控菜单以及 list/query/remove 权限，组件路径与 T-11 约定一致，不按 Client 生成数据过滤。

## 5. 实现契约

- **入口或接缝：** NAMEWTA `DDL.sql`、`DML.sql` 与 MySQL 元数据查询。
- **输入与输出：** 现有库或新库 -> 完整 schema、索引、注释、回填和菜单权限。
- **公共接口变化：** 数据库 schema 与动态路由/权限键新增。
- **不变量：** 历史 sys_oss 不成为到期 TEMP；ref_type 是表名而非 `contract` 等领域别名；无多态外键。
- **状态或数据流：** expand 字段/表 -> 回填 -> 约束/索引 -> DSL。
- **错误与失败行为：** 不兼容数据必须在约束前显式暴露，不得用 destructive 重建掩盖。
- **兼容要求：** 只追加 change 块，可用于既有库升级；不改上游基线 SQL。
- **安全与隐私要求：** 明文正文与目标是已接受风险；菜单只授予明确权限，不默认扩大角色授权。

## 6. 执行路线

1. 对照实体约定设计字段、主键、唯一键、索引与中文注释。
2. 追加 sys_oss expand/历史回填和 `sys_oss_ref` DDL。
3. 追加 notify 逻辑/Delivery 表 DDL，保证父子删除实现可控。
4. 追加 monitor 菜单及 list/query/remove 权限 DSL。
5. 分别验证 fresh 与 upgrade 路径、字段注释、索引和历史行状态。

## 7. 路径访问契约

- **预计修改点：** 两个 NAMEWTA SQL 文件与 ruoyi-admin 迁移测试包。
- **可写范围：** frontmatter 两个精确文件。
- **只读上下文：** 上游 schema 与现有 monitor 页面路径。
- **共享路径：** 两个 SQL 文件唯一 owner 均为 T-02；其他 Ticket 不写。
- **保留或不动：** `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 和其他数据库方言文件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | MySQL fresh migration | 执行 DDL/DSL 并查询 information_schema | 表、字段、索引、菜单完整 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-02.md</Path>` |
| 失败路径 | 既有库 upgrade | 准备历史 sys_oss 后执行 change 块 | 历史对象保持非 TEMP，无数据丢失 | 同上 |
| 回归 | SQL 静态核对 | 检查只追加、主键/七字段/注释/组件路径 | 符合工程规范 | 同上 |

- **Workspace checks：** current 模式在 `current-workspace`；required 模式先在 `source-worktree` 静态检查，再由 Lead 在 parent-candidate 验证数据库。
- **E2E disposition：** not-required：用户明确不建设或执行 E2E 测试；用 ruoyi-admin 数据库集成测试和隔离 MySQL 迁移演练作为判卷接缝。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 核对迁移 transcript。
- **Integration evidence：** 记录 source commit、parent before、candidate/result SHA、SQL transcript 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** DDL expand/回填先于 Java 发布；DSL 与 T-11 页面同批启用。
- **兼容窗口：** 新字段需允许旧应用短时共存；旧应用不得把新增表误处理。
- **监控信号：** TEMP 历史行数量、孤立 ref、notify 表容量和重复索引错误。
- **回滚或前向恢复：** 发布前备份；应用回滚时保留 additive schema，采用前向修复，禁止自动 drop。
- **不可逆操作与批准点：** 任何历史数据 UPDATE 先输出影响行数并由 Lead 批准；不包含 DELETE。
- **收缩条件：** 不适用：本 Ticket 只 expand，不做 schema contract。

## 10. 验收标准

- [x] `AC-005/008/009/010/011/025/029/032` 的持久化前提满足。
- [x] fresh 与 upgrade Evidence 证明历史 OSS 不会成为 TEMP。
- [x] ref_type 合同明确为真实表名，client_pk 不构成隔离键。
- [x] SQL 修改仅由 T-02 完成，数据库集成验证与 SHA 已记录。
