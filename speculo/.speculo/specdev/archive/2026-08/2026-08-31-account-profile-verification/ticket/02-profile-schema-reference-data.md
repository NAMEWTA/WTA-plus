---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-02
title: 建立 Profile 全量 Schema、活动唯一约束与参考数据
status: done
planning_depth: deep
planning_depth_reason: 新增明文身份数据 schema、并发唯一约束、权限菜单和不可删除历史，属于高事故半径数据迁移。
ready: true
risk: critical
blocked_by: []
contract_ids: [AC-008, AC-015, AC-027, AC-028, AC-031, AC-032, AC-033, AC-034, AC-039, AC-042]
owner: codex:/root
expected_changes:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
writable_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/profile/schema/**</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/10-ruoyi-base.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/**</Path>"
shared_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
shared_path_owners:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path> => T-02"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path> => T-02"
---

# Ticket T-02: 建立 Profile 全量 Schema、活动唯一约束与参考数据

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-profile-schema-reference-data.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 一次性建立所有垂直切片依赖的数据结构、活动唯一性和稳定参考数据。
- **可观察产出：** NAMEWTA 50/60 完整基座可随六文件顺序在 MySQL 8.4 fresh 执行，重复身份/进行中申请/有效绑定并发写被数据库约束阻止，历史注销记录可共存。
- **来源：** `AC-008`、`AC-015`、`AC-027`、`AC-031` 至 `AC-034`、`AC-039`、`AC-042`、`ADR-023` 至 `ADR-025`。
- **当前事实：** 当前 SQL 无 profile 表、菜单、权限、证件目录或 systemRequired 标签。
- **Planning Depth 原因：** schema、明文敏感数据与不可删除历史一旦进入已有环境只能通过评审后的 Tag 差异前向修复。

## 2. 决策状态

### 已锁定决策

- 表覆盖主体、版本、工作副本、提交/来源快照、绑定事件、材料目录/关系、验证尝试、决定/通知审计。
- 每张项目表具备七基础字段；历史使用显式业务状态，不以物理删除复用身份。
- 使用数据库可判定的活动键/守卫约束保证未注销身份、进行中申请和 active/suspended 绑定唯一。
- 权限固定为 person/enterprise 六类能力及共享 material-tag query/manage；必传标签 code 稳定且 systemRequired。

### 已采用的低影响假设

- 采用 MySQL 当前版本可执行的生成列、规范化活动键或守卫表方案，由 schema 测试证明语义，不绑定某一种技巧。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 50/60 完整基座、菜单权限、字典/配置、必传标签与 schema 测试 | 六文件 MySQL 基座合同、BaseEntity 基线 | CDE 数据导入、生产执行、后端 SQL 副本、其他方言 |

## 4. 要构建什么

fresh install 后可持久化完整个人/企业档案历史，并允许同一身份在旧档案注销后创建带前后继的新档案；任何时间都不能同时存在冲突活动主体、进行中申请或同类有效绑定。

## 5. 实现契约

- **入口或接缝：** MySQL 六文件 fresh fixture 与并发写 SQL。
- **输入与输出：** 空库 -> profile schema、reference rows、菜单权限及可判定约束。
- **公共接口变化：** 数据库 schema 与权限字符成为持久合同。
- **不变量：** 无业务物理 DELETE；快照/事件只追加；材料引用保存标签名称快照。
- **状态或数据流：** revoked 主体释放活动身份键，新主体记录 previousProfileId；绑定事件不覆盖旧行。
- **错误与失败行为：** 重复 code、活动身份/绑定/申请冲突由约束拒绝；完整基座只用于全新库，不承诺在已有库重复执行。
- **兼容要求：** 直接修改父仓库 50/60 当前完整基座；已有库按源/目标 Git Tag 差异升级，不恢复后端 SQL。
- **安全与隐私要求：** 字段长度支持既定证件但不在索引/注释中写真实敏感样例。

## 6. 执行路线

1. 建立 fresh schema 与约束失败 fixture。
2. 直接更新 50 完整基座中的表、索引、外键/逻辑约束和中文注释。
3. 直接更新 60 完整基座中的证件 code、材料根/必传标签、权限菜单、字典与流程配置占位。
4. 验证注销重建、并发冲突、历史只读与 fresh 数据结果。
5. 静态确认后端 `script/` 不存在且后续 Ticket 不再写共享 SQL。

## 7. 路径访问契约

- **预计修改点/可写范围：** 两个 NAMEWTA SQL 文件与 schema 测试。
- **只读上下文：** 上游 schema 与 MyBatis 基础字段约定。
- **共享路径：** DDL/DML 唯一 owner 为 T-02。
- **保留或不动：** 生产数据库；本 Ticket 不恢复后端 `script/`，不新增其他 SQL 文件或方言。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | MySQL fresh fixture | 依序执行六份基座并查询表/种子 | 结构、菜单、权限和必传 code 完整 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 失败 | 并发/重复 SQL | 插入冲突活动身份、申请、绑定与非法树 | 数据库拒绝且无部分行 | 同上 |
| 回归 | schema diff/DDL test | Maven schema 测试与 Git diff | 七字段齐全，后端无 SQL 副本 | 同上 |

- **Workspace checks：** source/current workspace 执行静态与 MySQL fixture。
- **E2E disposition：** required：真实 MySQL 约束和 fresh 六文件基座是本 Ticket 的核心跨边界行为。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；使用隔离测试库，不执行生产迁移。
- **Integration evidence：** implementation/source commit、parent/candidate/result SHA、MySQL 查询记录。

## 9. 发布、迁移与恢复

- **迁移顺序：** 全新库隔离验证六文件；已有库备份 -> 源/目标 Tag 差异 -> 隔离演练 -> 获批执行 -> 默认未开放代码。
- **兼容窗口：** 当前无 profile 数据；基座可直接修改，已有库只执行评审后的差异升级。
- **监控信号：** 唯一约束冲突、DDL/DML 失败和历史增长。
- **回滚或前向恢复：** 未部署可回滚 SQL diff；部署后保留表并前向修复，不删除历史。
- **不可逆操作与批准点：** 生产 SQL 执行未授权，必须单独批准。
- **收缩条件：** 不适用：无旧 schema。

## 10. 验收标准

- [x] 所列 AC 的 schema/权限/历史不变量通过 MySQL fixture。
- [x] DDL/DML 只有 T-02 写入，后端 `script/` 未恢复。
- [x] required E2E、Evidence、实现提交和父分支结果完整。
- [x] 无生产执行或未批准偏差。
