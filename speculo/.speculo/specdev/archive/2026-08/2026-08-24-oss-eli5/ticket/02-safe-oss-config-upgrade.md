---
schema_version: 3
artifact: ticket
change: 2026-08-24-oss-eli5
id: T-02
title: 建立安全配置治理与升级基线
status: done
planning_depth: deep
planning_depth_reason: 修改配置管理安全边界、HTTP CRUD 合同、NAMEWTA schema 和存量数据，涉及迁移与误公开风险。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-013, AC-017, AC-018]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysOssConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/bo/SysOssConfigBo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysOssConfigVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssConfigMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISysOssConfigService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssConfigServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssConfigController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/OssConfigChangeListener.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/ossaccess/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysOssConfig.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/bo/SysOssConfigBo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/vo/SysOssConfigVo.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssConfigMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISysOssConfigService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssConfigServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssConfigController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/OssConfigChangeListener.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/oss/config/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/migration/ossaccess/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/resources/oss/business-oss-owners.json</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOssMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/event/OssConfigChangeEvent.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssConfigServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssConfigController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssConfigServiceImpl.java</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssConfigController.java</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/listener/OssConfigChangeListener.java</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path> => T-02"
  - "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> => T-02"
---

# Ticket T-02: 建立安全配置治理与升级基线

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/02-safe-oss-config-upgrade.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 让数据库、管理 API 和缓存刷新共同执行 PRIVATE/PUBLIC_READ 安全边界，并为显式对象迁移预建可审计 schema。
- **可观察产出：** 管理员只能保存两种访问类型；唯一默认配置必须 PRIVATE；有对象的 configKey 无法通过普通编辑改变 Bucket/访问类型；fresh/upgrade 数据库中的历史配置均不会自动公开。
- **来源：** `US-005`、`US-006`、`AC-013`、`AC-017`、`AC-018`、`ADR-005`、`ADR-006`、`ADR-012`、`ADR-013`。
- **当前事实：** 上游表注释仍为 private/public/custom，初始值 `1`；配置更新不检查 `sys_oss.service` 引用；修改/删除/切换默认仍使用 PUT/DELETE。
- **Planning Depth 原因：** schema、存量回填、缓存一致性和配置安全边界必须按 expand-migrate 顺序完成。

## 2. 决策状态

### 已锁定决策

- 物理编码固定为 `0=PRIVATE`、`2=PUBLIC_READ`；旧 `0/1/2` 和含义不明值均按 PRIVATE 安全回填，不从旧标签推断公开。
- `status` 只表示唯一默认配置，默认必须 PRIVATE；不得复用为启用开关。
- configKey 已被 `sys_oss.service` 引用时，普通编辑 Bucket 或访问类型失败；凭据、endpoint、domainUrl 等非边界字段仍可轮换。
- 配置变更失败时数据库和缓存均保持旧值；删除被对象引用的配置同样拒绝。
- 迁移审计采用 `sys_oss_migration_batch` 与 `sys_oss_migration_item` 两张项目自有表，完整包含七基础字段、中文注释和幂等索引。
- 配置新增、编辑、删除、默认切换全部遵循查询 GET、变更 POST、每个 POST 使用准确 `@Log`。

### 已采用的低影响假设

- 迁移表只在本 Ticket 建立 additive schema；T-06 才映射 entity、状态机和管理 API。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 配置 CRUD 不变量、对象引用检查、POST 合同、append-only DDL/DML、两张迁移审计表、fresh/upgrade 测试 | 现有配置事件与缓存、`BaseEntity`、NAMEWTA 时间戳/雪花脚本 | Provider readiness、对象复制、迁移执行、前端改造、生产 SQL 执行 |

## 4. 要构建什么

管理员保存 OSS 配置时，Service 先验证双类型和默认不变量，再检查该 configKey 是否承载对象。安全轮换成功后发布原有缓存事件；边界变更或删除引用配置在写入前失败。升级 SQL 先执行 preflight，再保守回填历史访问类型并创建迁移审计表，不改变 ossId 或 `sys_oss.service`。

## 5. 实现契约

- **入口或接缝：** `/resource/oss/config` 管理 API、`ISysOssConfigService`、`SysOssConfigMapper`、NAMEWTA DDL/DML。
- **输入与输出：** 配置 BO -> 已验证的配置/稳定业务失败；旧数据库 -> PRIVATE 基线与空迁移审计表。
- **公共接口变化：** 配置变更路由从 PUT/DELETE 收敛为无冲突 POST 路径；查询路由不变。
- **不变量：** configKey 唯一；唯一默认且为 PRIVATE；引用对象时 Bucket/类型/configKey 归属不被普通 CRUD 改变。
- **状态或数据流：** validate -> 引用计数/旧值比较 -> 事务写入 -> AFTER_COMMIT 缓存事件；SQL preflight -> DDL -> DML 回填。
- **错误与失败行为：** 旧值不存在、非法类型、默认公共、引用边界变化和删除引用配置均在副作用前失败。
- **兼容要求：** `sys_oss` 不加 accessType；上游 `ry_vue.sql` 不修改；fresh 固定 DDL 后 DML，upgrade 只执行新增块。
- **安全与隐私要求：** 配置响应和日志继续遵循现有密钥处理，不输出 Secret 或完整连接错误。

### 实现偏差记录

- **批准依据：** 用户已授权采用推荐实现；工程规范 `PERSIST-001`/`PERSIST-002` 与本 Ticket 的 AFTER_COMMIT 不变量为强制约束。
- **偏差：** `OssConfigChangeListener` 从只读上下文提升为 T-02 可写路径，将 Spring `@TransactionalEventListener(fallbackExecution = true)` 替换为 dynamic-datasource 的 `@DsTxEventListener`。
- **原因与影响：** 配置写边界迁移到 `@DSTransactional` 后，原监听器会在提交前立即刷新缓存；最小修改监听器才能保证回滚不产生缓存副作用。事件模型和缓存行为不扩展，后续 Ticket 继续只读消费。
- **偏差：** `business-oss-owners.json` 纳入 T-02 可写路径，为迁移治理表中的 OSS 命名主键、批次外键和迁移对象键登记显式 `carrierAllowlist`。
- **原因与影响：** 现有架构棘轮会把所有 SQL 中以 `oss_id` 结尾的字段视作候选业务引用载体；迁移表字段属于 T-06 状态机基础设施而非业务 Owner。显式登记理由可避免误报，同时保留未知业务载体 fail-closed 行为。

## 6. 执行路线

1. 使用 timestamp 生成器确定两个 append-only 变更块，并建立历史 SQL 前缀不变测试。
2. 先追加迁移表和访问类型 upgrade SQL，覆盖 fresh、旧值、未知值和重复执行前置。
3. 在配置 Service 中加入双类型、默认 PRIVATE、引用边界和删除保护，并保持成功后缓存事件。
4. 将受影响 CRUD 变更路由迁为 POST，逐方法补齐安全 `@Log` 和合同测试。
5. 运行配置单元/集成、MySQL fresh/upgrade 和后端 reactor 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** frontmatter 中配置 domain/service/controller、SQL 与定向测试。
- **只读上下文：** `SysOssMapper`、配置事件/监听器和冻结上游 SQL。
- **共享路径：** 配置 Service/Controller 和 NAMEWTA DDL/DML 只由 T-02 修改；后续 Ticket 只读消费。
- **保留或不动：** `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`、对象表 accessType 字段和现有 ossId。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 合法配置与安全轮换 | Service/DB integration | 运行 `OssConfig*Test` | 两种类型可保存，非边界字段轮换并刷新缓存 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| 边界变更/默认公共/删除引用配置 | negative integration | 同套件注入引用对象与缓存 spy | 写入失败，数据库和缓存保持旧值 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| fresh/upgrade 安全 | MySQL migration | 运行 `OssAccessMigrationMySqlIntegrationTest`，分别执行 fresh 与旧值 fixture | 历史对象 PRIVATE、表/索引/注释正确、ossId/service 不变 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |
| HTTP 回归 | controller contract | 扫描受影响 mapping 并运行 MockMvc | 查询 GET；变更 POST+`@Log`；无 PUT/DELETE | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>` |

- **Workspace checks：** 按 Goal Plan 在 current workspace 或 source worktree 运行定向非 E2E 测试和 `./mvnw -pl ruoyi-admin -am -DskipTests package`。
- **E2E disposition：** required：必须由 Lead 在 current workspace 或 parent-candidate 使用真实 MySQL 证明 fresh/upgrade、引用保护和数据不变量。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；隔离 MySQL，不执行生产 SQL。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA、SQL 前缀哈希和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 备份/匿名访问盘点 -> 新增 DDL -> 安全 DML 回填 -> 部署默认关闭代码；应用实现不得先于 schema 依赖生效。
- **兼容窗口：** 旧 HTTP 方法与旧 accessPolicy 值不长期双写；前后端必须配对发布，T-07 负责消费者迁移。
- **监控信号：** 非法配置数量、引用边界拒绝、默认配置不变量和 SQL preflight 结果。
- **回滚或前向恢复：** 保留 additive 迁移表；配置语义只前向修复为 PRIVATE，不能回滚到公开读写解释。
- **不可逆操作与批准点：** 生产 SQL 和数据回填未授权；执行前必须另行批准并确认备份/Provider Policy。
- **收缩条件：** 数据库只存在 `0/2`，默认行为为 `0`，前后端无旧 PUT/DELETE/config 标签调用。

## 10. 验收标准

- [x] `AC-013`、`AC-017`、`AC-018` 正常、失败和 upgrade 场景通过。
- [x] 两张迁移表符合基础字段、前缀、主键和中文注释规范。
- [x] `DDL.sql`/`DML.sql` 只在末尾追加，`ry_vue.sql` 未修改。
- [x] 生产迁移未执行，真实 MySQL 证据仅来自隔离环境。
- [x] 验证矩阵记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-02.md</Path>`。
- [x] 实际修改未超出 writable_paths，共享路径只由 T-02 修改。
- [x] 形成非空 implementation/source commit，经 direct-parent 或 candidate 验证并记录 result SHA。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 一致。
