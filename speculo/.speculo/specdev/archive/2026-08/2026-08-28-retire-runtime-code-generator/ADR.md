# Change 架构决策

## ADR-001: 以静态标准模板替代运行时代码生成器

**Status:** accepted
**Source:** LOG-001 / LOG-003 / user decision
**Supersedes:** none

### Context
项目已将完整 CRUD 模板与开发注意事项迁移到父仓库 docs/fm。继续维护后端生成模块、前端管理页面、数据库元数据和权限面，会形成第二套模板解释与运行维护成本。

### Decision
完整退役产品运行时代码生成器，不保留兼容 API 或管理界面；保留 docs/fm 作为 AI 与开发者使用的静态 CRUD 标准参考资产。

### Trade-off
保留生成器可继续支持交互式配置和批量下载，但需要长期维护模板引擎、数据库元数据、跨端页面与权限。选择静态模板会失去在线低代码体验，但符合当前 AI 驱动开发方式，并消除重复能力与漂移源。

### Consequences
运行时代码、装配、HTTP/OpenAPI 合同、前端包、菜单权限和专属数据库对象必须作为一个完整纵向切片退役。CRUD 开发的质量取决于 docs/fm 与工程规范持续准确。

### Verification / Migration
通过残留引用扫描、双端质量门禁、OpenAPI 漂移检查和 namewta SQL 最终状态检查证明运行能力已消失且其他模块可正常构建。

## ADR-002: 生成器专属元数据随基座永久删除

**Status:** accepted
**Source:** LOG-006 / LOG-007 / user decision
**Supersedes:** none

### Context
gen_table 与 gen_table_column 只服务于运行时代码生成器。项目当前是未投产基座，不存在历史生成配置的生产保留、旧版本并行或升级兼容合同。

### Decision
物理删除 gen_table_column 与 gen_table 及其中全部数据；不创建备份表、归档、兼容视图或恢复路径。

### Trade-off
保留或导出数据可以为未来追溯旧生成配置提供素材，但会延续已退役领域的数据 owner 和维护义务。永久删除不可恢复，但能使基座 schema 与产品能力保持一致。

### Consequences
namewta DDL 必须按依赖顺序删除两张表。验收只证明最终基座无残留，不承担生产升级、回滚或数据恢复验证。

### Verification / Migration
静态检查 DDL 删除顺序，并在可丢弃的基座数据库中确认两张表均不存在。
