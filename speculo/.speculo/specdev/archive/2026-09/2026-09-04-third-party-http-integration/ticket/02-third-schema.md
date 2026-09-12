---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-02
title: 建立 Provider、Endpoint、凭据与调用统计数据基座
status: done
planning_depth: deep
planning_depth_reason: 新增安全凭据和审计统计相关 MySQL schema、唯一约束、逻辑删除与跨后续切片共享的持久化模型。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-001, AC-002, AC-005, AC-013, AC-017]
owner: codex:/root
expected_changes:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/entity/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/**</Path>"
writable_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/domain/entity/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/java/org/dromara/third/mapper/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/main/resources/mapper/third/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-third/src/test/java/org/dromara/third/schema/**</Path>"
read_only_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/10-ruoyi-base.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/**</Path>"
shared_paths:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
shared_path_owners:
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path> => T-02"
---

# Ticket T-02: 建立 Provider、Endpoint、凭据与调用统计数据基座

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-third-schema.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 一次建立 Provider、Endpoint、分 scope 凭据、脱敏调用明细和时间桶统计的可信数据模型。
- **可观察产出：** MySQL 8.4 fresh-init 后表、唯一约束和索引可用，Mapper 可验证继承回源、管理筛选和聚合所需查询。
- **来源：** `US-002`、`US-003`、`US-007`、`AC-001`、`AC-002`、`AC-005`、`AC-013`、`AC-017`、`ADR-002`、`ADR-003`。
- **当前事实：** NAMEWTA DDL 中没有 third 表，外部参考只有单层配置/计数，不能复用其数据边界。
- **Planning Depth 原因：** schema 与密文/历史完整性不可事后无损修补，且被所有后端行为共享。

## 2. 决策状态

### 已锁定决策

- providerCode 全局唯一；endpointCode 在 provider 内唯一；credential 在 scope/type 内唯一。
- Provider/Endpoint 支持启停和逻辑删除；调用历史保留编码快照，不级联删除。
- 调用明细是一条逻辑调用并含 attemptCount；时间桶区分实际发送、成功、失败、超时、拒绝。
- 凭据只存版本化认证密文、nonce/tag 等 envelope 字段，不存主密钥。

### 已采用的低影响假设

- 表名前缀、ID、version、createDept、审计列和 delFlag 精确沿用当前 NAMEWTA 自有表基线。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>、<Path>.agents/skills/ruoyi-common-modules-guide/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/engineering-standards/references/project/02-decisions-and-exceptions.md</Path>、<Path>.agents/skills/engineering-standards/references/project/03-backend-module-modes.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/persistence-transactions-and-ddl.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/mapper-and-sql.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/framework-usage.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/verification.md</Path>。
- **目录与代码最低要求：** ruoyi-third 按 layered 持久化边界组织 domain/entity、domain/model/read、mapper、mapper XML；DAO 是唯一业务持久化入口，Service/UseCase 不导入 MyBatis/Mapper。Entity、Row/Projection、BO、VO 职责分离，禁止把数据库实体或密文模型放入 ruoyi-api。
- **SQL 最低要求：** 只支持 MySQL 8.4；项目自有表使用现有主键、version、审计列和 del_flag 基线；DDL 只能进入 <Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>，不得创建 script/、模块私有 SQL 或其他方言。Mapper 遵守 BaseMapperPlus → wrapper/query builder → MPJ → XML 阶梯，复杂 SQL 必须落 XML。
- **执行停止条件：** 未先验证 10→20→30→40→50→60→61 基座、把完整报文/明文凭据落库、绕过 DAO、修改基础表或把 DDL 写入非 owner 文件时立即停止并升级 T-02/Lead。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 五类表、实体、Mapper/XML、索引与 schema 测试 | MyBatis-Plus/BaseMapperPlus、自有表字段惯例 | CRUD 行为、加解密实现、菜单 DML、存量迁移 |

## 4. 要构建什么

fresh-init 环境能够持久化两级配置、独立凭据和不含完整报文的调用历史；重复编码、重复 scope/type、非法关联或有历史时的清理不能破坏唯一性与可追溯性，管理筛选和 provider/endpoint 聚合均有索引支撑。

## 5. 实现契约

- **入口或接缝：** Entity、Mapper/XML 与 MySQL DDL。
- **输入与输出：** 规范记录写入后可按 code、状态、时间、requestId 和 provider/endpoint 查询。
- **公共接口变化：** 无。
- **不变量：** 明细不含完整 request/response；凭据无明文列；历史不依赖当前对象仍存在。
- **状态或数据流：** 后续 UseCase 写事实表，缓存从 Provider/Endpoint/Credential 回源，统计按固定时间桶聚合。
- **错误与失败行为：** 唯一冲突由 DB 拒绝；不以软删除绕过有效唯一性；无级联物理删除历史。
- **兼容要求：** 仅追加 NAMEWTA DDL，不修改 RuoYi 基础表语义。
- **安全与隐私要求：** 密文 envelope 字段可校验版本；日志/统计字段长度限制阻止原始大 body 落库。

## 6. 执行路线

1. 先用 schema contract 声明列、唯一性、索引和敏感字段负向断言。
2. 在 50 DDL 中按依赖顺序增加五类表与约束。
3. 增加 Entity、Mapper/XML 和管理/回源/统计必要查询。
4. 使用 MySQL 8.4 执行 fresh-init、重复值、逻辑删除和历史保留验证。
5. 运行 Mapper 定向测试并记录 explain/索引证据。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 50 DDL、third 持久化模型、Mapper/XML 和 schema 测试。
- **只读上下文：** RuoYi 基础 DDL及 system 表/实体字段惯例。
- **共享路径：** 50 DDL 由 T-02 唯一写；其他 Ticket 只消费已集成 schema。
- **保留或不动：** 61-third-dml.sql、私有 backend SQL 目录、外部 cde 数据。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | MySQL/Mapper | 以 10→50 初始化后运行 third Mapper 集成测试 | 表、关联、筛选和聚合查询成立 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 失败路径 | DB 约束 | 重复 provider/endpoint/credential 与超长原始载荷尝试 | 唯一/长度约束拒绝，历史不被级联删除 | 同上 |
| 回归 | fresh-init | MySQL 8.4 执行完整 10→20→30→40→50→60→61 初始化 | 既有基座和 third menu DML 均不报错 | 同上 |

- **Workspace checks：** source/current workspace 运行 Maven Mapper 测试和 SQL 静态校验。
- **E2E disposition：** required：真实 MySQL 8.4 DDL、约束与事务边界无法由纯单元测试证明。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；执行 fresh-init、唯一冲突和历史保留场景。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA、数据库版本与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** Provider → Endpoint → Credential → Invocation → Statistics；随后才部署后端写入。
- **兼容窗口：** additive schema 可先发布，旧应用忽略新表。
- **监控信号：** migration 失败、唯一冲突、密文字段异常长度、统计桶写失败。
- **回滚或前向恢复：** 未写业务数据可回滚新表；产生历史后保留表并前向修复，不丢调用记录。
- **不可逆操作与批准点：** 生产 DDL 不在本 Ticket 执行，需独立发布批准与备份。
- **收缩条件：** 不适用：无旧 third schema/数据迁移。

## 10. 验收标准

- [x] `AC-001/002/005`：两级配置与分 scope 密文数据约束成立。
- [x] `AC-013/017`：统计语义和历史保留可由 MySQL 测试判定。
- [x] fresh-init、失败矩阵、回归和路径所有权证据完整。
- [x] required E2E 由 Lead 在集成状态执行且无未批准生产动作。
