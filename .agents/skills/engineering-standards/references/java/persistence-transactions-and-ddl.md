# 数据源事务与建表规范

适用后端事务、`@DS` 数据源切换、事务事件、项目自有 DDL 和 schema 迁移。当前项目通过 `wta-common-mybatis` 使用 dynamic-datasource 4.5.0；上游初始化脚本、第三方组件 schema 与 NAMEWTA SQL 的所有权不同，不能混为一次整治。

## 规则索引

- [PERSIST-001 统一使用动态数据源事务](#persist-001-统一使用动态数据源事务)
- [PERSIST-002 事务事件与跨库一致性边界](#persist-002-事务事件与跨库一致性边界)
- [PERSIST-003 每个新建项目自有表的基础字段](#persist-003-每个新建项目自有表的基础字段)
- [PERSIST-004 模块表前缀与主键命名](#persist-004-模块表前缀与主键命名)
- [PERSIST-005 表与字段中文注释](#persist-005-表与字段中文注释)
- [PERSIST-006 NAMEWTA 六文件可编辑 MySQL 基座合同](#persist-006-namewta-六文件可编辑-mysql-基座合同)
- [PERSIST-007 DDL 所有权、方言与迁移](#persist-007-ddl-所有权方言与迁移)

### PERSIST-001 统一使用动态数据源事务

Scope: `module:backend`, business service transaction boundaries

Level: MUST

Source: `user-decision` + `repository-fact` (`dynamic-datasource-spring-boot4-starter`, `SysNotifyMonitorServiceImpl`)

Rule: 新建或实质修改的业务事务边界使用 `com.baomidou.dynamic.datasource.annotation.DSTransactional`，不再新增 Spring `org.springframework.transaction.annotation.Transactional`。注解放在可由 Spring AOP 代理调用的 public service 用例边界；需要切换数据源时，由该事务内被代理调用的 service/mapper 使用 `@DS`。同一调用链不得叠加或静默混用两套事务注解，也不得依赖 private/self-invocation 触发事务。

`@DSTransactional` 当前默认 `rollbackFor = Exception.class`、`propagation = REQUIRED`，常规用例直接写 `@DSTransactional`；只有领域语义确实不同且已有回滚/传播测试时才配置 `rollbackFor`、`noRollbackFor` 或 `propagation`。存量 Spring 事务按 `MIG-BE-DS-TX` Ratchet 迁移，不以本规则授权无关全仓替换。

Verification: `rg -n '@(DS)?Transactional'` 检查变更调用链和 import；通过真实 Spring proxy 执行单数据源及切换数据源的成功、checked/runtime exception 回滚和 propagation 集成测试；review 不存在 controller/private/self-invocation 边界。

### PERSIST-002 事务事件与跨库一致性边界

Scope: dynamic-datasource transactions, application events and multi-datasource writes

Level: MUST

Source: `repository-fact` (dynamic-datasource 4.5.0 `TransactionalTemplate`, `DsTxEventListener`)

Rule: `@DSTransactional` 下依赖提交阶段的事件监听使用 `com.baomidou.dynamic.datasource.annotation.DsTxEventListener` 并显式核对 `TransactionPhase`；不得继续依赖 Spring `@TransactionalEventListener(fallbackExecution = true)` 获得 AFTER_COMMIT 语义，因为没有 Spring transaction 时它会按 fallback 立即执行。外部通知、缓存、会话和消息在提交前后的时机、幂等与补偿必须明确。

dynamic-datasource 本地事务协调多个 JDBC connection，但不是天然的 XA/Seata 全局原子提交保证。跨库写入若不能容忍部分提交，必须先决定 Seata/XA、outbox/saga 或其他一致性方案并验证失败恢复，不能仅凭 `@DSTransactional` 宣称分布式强一致。

Verification: commit/rollback 时 listener 是否触发及触发次数的集成测试；故障注入覆盖第二数据源写入失败、提交失败和副作用重试；review 当前配置是否实际启用 Seata/XA，不以依赖存在推断已启用。

### PERSIST-003 每个新建项目自有表的基础字段

Scope: every new project-owned `CREATE TABLE` under `path:release-artifacts/docker/infrastructure/mysql/init/**`

Level: MUST

Source: `user-decision` + `repository-fact` (`10-wta-base.sql` 的 `test_demo`, `BaseEntity`)

Rule: 每个新建的项目自有表，包括业务表、关系表、历史/日志表和配置表，均以 `test_demo` 的以下字段集为建表基线；缺少任一字段都必须在实现前取得明确 schema 例外，不能因“只是关系表”自行省略：

| Column | Contract |
|---|---|
| `version` | 乐观锁版本，初始默认值为 `0`；对应 entity 显式字段和 `@Version` |
| `create_dept` | 创建部门；由 `BaseEntity.createDept` 映射/填充 |
| `create_time` | 创建时间；由 `BaseEntity.createTime` 映射/填充 |
| `create_by` | 创建人；由 `BaseEntity.createBy` 映射/填充 |
| `update_time` | 更新时间；由 `BaseEntity.updateTime` 映射/填充 |
| `update_by` | 更新人；由 `BaseEntity.updateBy` 映射/填充 |
| `del_flag` | 逻辑删除标志，未删除默认值为 `0`；对应 entity 显式字段和 `@TableLogic` |

DDL 中优先保持上述顺序，便于与 `10-wta-base.sql` 的 `test_demo` 逐项核对；字段类型、时间类型和注释使用 MySQL 8.4 语法，并与 Java 类型、MyBatis Plus 配置及同模块成熟表一致，不能机械复制 MySQL display width。项目自有 entity 应继承 `BaseEntity`，只显式声明 `version` 和 `delFlag`；没有 entity 的表仍不得省略 DDL 基础字段。

Verification: 对每个新增 `CREATE TABLE` 提供七字段逐项 review 证据；对照 schema/entity/`BaseEntity`，验证 insert 自动填充、update 自动填充、乐观锁冲突和逻辑删除查询；在目标数据库执行 fresh install。存量缺失字段按 `MIG-BE-DDL-BASE` 处理，不把未迁移的旧表当作新表范例。

### PERSIST-004 模块表前缀与主键命名

Scope: every new or project-taken-over `CREATE TABLE` under `path:release-artifacts/docker/infrastructure/mysql/init/**`

Level: MUST

Source: `user-decision`

Rule: 每张表必须以所有者子模块的稳定缩写和下划线开头，格式为 `<module_prefix>_<business_name>`，例如 `sys_user`、`sj_job`、`flow_category`。实现前先从同一子模块的成熟表确认前缀；无法确认时先形成模块前缀决策，不得创建无前缀表或借用其他模块前缀。这里约束的是数据库物理表名，`.sql` 只是文件扩展名，不属于表名。

主键列不得命名为裸 `id`。移除一次 `<module_prefix>_` 后，以完整业务表名加 `_id` 作为主键名：`flow_category` 使用 `category_id`，`sj_xxx` 使用 `xxx_id`，`sys_user_type` 使用 `user_type_id`。Java entity、mapper、关联表外键、索引和查询必须使用同一列名；自然键或联合主键只有取得明确 schema 例外后才可偏离。

Verification: review 每个 `CREATE TABLE` 的所有者、表名前缀和主键；检查不存在裸 `PRIMARY KEY (id)`，并对照 entity、mapper、外键与索引。存量或第三方表不因本规则批量改名，项目接管后新增的表立即遵循本规则。

### PERSIST-005 表与字段中文注释

Scope: project-owned table and column DDL under `path:release-artifacts/docker/infrastructure/mysql/init/**`

Level: MUST

Source: `user-decision`

Rule: 每张项目自有表都必须提供简明、准确的中文表注释；每个通过 `CREATE TABLE` 或 `ALTER TABLE` 新增的字段都必须提供简明中文字段注释。注释描述业务含义，不重复字段名，不使用“字段”“信息”等无区分度占位文本；状态、类型等编码字段应在不造成冗长的前提下说明关键取值语义。

Verification: 逐表检查 table comment，逐字段检查 column comment；在目标数据库查询 `information_schema.TABLES.TABLE_COMMENT` 与 `information_schema.COLUMNS.COLUMN_COMMENT`，两者不得为空且不得是占位文本。

### PERSIST-006 NAMEWTA 六文件可编辑 MySQL 基座合同

Scope: `path:release-artifacts/docker/infrastructure/mysql/init/**`

Level: MUST

Source: `user-decision`

Rule: `release-artifacts/docker/infrastructure/mysql/init/` 是数据库初始化资产的唯一事实源，只允许六份受管 SQL：`10-wta-base.sql`、`20-ry-job.sql`、`30-ry-workflow.sql`、`40-ry-ai.sql`、`50-namewta-ddl.sql`、`60-namewta-dml.sql`。六份文件都是当前完整基座，可以在迭代中直接修改、删除、替换或重排其内部内容；不得再使用 append-only 约束，也不得为单次变化新增版本、功能、临时或备份 SQL。

`10` 至 `40` 分别承载 WTA、Job、Workflow、AI 基座；`50-namewta-ddl.sql` 只保存 NAMEWTA 建表、改表、索引和约束等结构语句；`60-namewta-dml.sql` 只保存 NAMEWTA 初始化、回填及其他 `INSERT`、`UPDATE`、`DELETE` 数据语句。DDL 与 DML 不得混入对方文件。所有六份文件必须被 Git 跟踪，不得被 `.gitignore` 排除。

全新环境固定按文件名前缀 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 执行。已有环境不得重放任一完整基座；升级必须指定源 Git Tag 与目标 Git Tag，备份数据库，基于两 Tag 的六文件差异形成可审计升级 SQL，在隔离副本评审和演练后才能执行。差异 SQL 是部署报告中的临时交付物，不进入基座目录。

Verification: 检查目录中六份 SQL 的精确名称、顺序、非空和 Git 跟踪状态；检查 `50`/`60` 分类及后端 `script/` 不存在；在全新 MySQL 8.4 隔离库执行全部六份文件并验证关键表、菜单和 OSS 配置；存量升级验证源/目标 Tag、备份、差异、演练和回滚证据。

### PERSIST-007 DDL 所有权、方言与迁移

Scope: `path:release-artifacts/docker/infrastructure/mysql/init/**`, schema changes

Level: MUST

Source: `user-decision` + `repository-fact` (project profile, upstream and third-party SQL ownership)

Rule: 六份完整基座由父仓库发布资产统一拥有，后端仓库不得恢复 `script/` 或任何 SQL 副本。当前 NAMEWTA 只支持 MySQL 8.4，不维护或宣称 PostgreSQL、Oracle、SQL Server 兼容；上游出现其他方言文件时也不迁移到产品基座。

上游、SnailJob、WarmFlow、AI 或其他第三方拥有的 schema 不因本规则被批量重写；项目一旦新建或明确接管某张表，该表从接管点起应用 PERSIST-003 至 PERSIST-005。任何例外进入 decisions/exceptions，并包含 owner、理由、风险、补偿、验证和移除条件。

Verification: SQL diff 与文件所有权 review；隔离 MySQL 8.4 验证全新初始化；存量环境按源/目标 Git Tag 验证差异升级和回滚/补偿；扫描后端不存在 `script/` 和非 MySQL SQL。
