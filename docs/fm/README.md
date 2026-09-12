# NAMEWTA FreeMarker 模板

本目录是项目代码模板的唯一权威位置，保存当前有效的 Java、Vue、React、MyBatis XML 与菜单 SQL 模板。历史版本通过 Git 查询，不在目录内保留副本。

## 状态

- 本目录是独立静态模板资产，不属于任何运行时 classpath 资源。
- 运行时代码生成器已从基座物理删除；AI 与开发者直接参考本目录完成 CRUD 实现和审查。
- 使用模板时必须显式读取 [catalog.json](./catalog.json)，按 [context-contract.md](./context-contract.md) 提供上下文。
- 模板只生成资源切片，不覆盖已有模块的共享入口、包清单或 App 组合文件。

## Vue 输出

Vue 模板面向 `plus-ui-namewta` 当前多 App 架构：

```text
packages/domains/<module>/src/<resource>/
  index.ts
  service.ts
  transport.ts
  types.ts

packages/web-domains/<module>/src/<resource>/
  <BusinessName>Page.vue
  composables.ts
  index.ts
  registration.ts
  runtime.ts
```

domain 保持无界面并通过 `HttpClient` 端口访问 HTTP；web-domain 通过类型化 runtime 获得 service、确认、反馈、字典、下载以及可选宿主组件。资源 registration 仍需由所属 web-domain manifest 汇总，并由目标 App 显式选择。

具体的薄入口、共享类型、树语义、异步生命周期、package exports 与 App 人工接入清单见 [vue/README.md](./vue/README.md)。

## HTTP 合同

- 列表、详情、树和选项等只读操作使用 GET。
- 新增、修改、删除、状态和排序等业务变更使用 POST。
- Java POST 业务接口生成准确的 `@Log`；修改与删除使用 `/edit`、`/remove/{ids}` 避免与新增和详情路径冲突。

## 后端模板映射

后端模板分为两个明确生命周期：新建业务模块统一使用 `layered`，强制生成 `Controller -> UseCase -> Service -> DAO -> Mapper -> XML`；`classic` 仅用于已登记的存量模块维护。`wta-profile` 是当前五层参考实现，`wta-system`、`wta-workflow`、`wta-job`、`wta-demo`、`wta-ai` 保持 legacy，不因模板升级自动迁移。

classic 标准 CRUD 必须按职责使用对应模板，不能把多层职责压进自定义文件。下表仅适用于登记为 `classic` 的存量模块：

| 目标职责 | 模板 |
|---|---|
| Entity | `java/domain.java.ftl` |
| BO | `java/bo.java.ftl` |
| VO | `java/vo.java.ftl` |
| Mapper Read Model | `java/read.java.ftl` |
| Mapper | `java/mapper.java.ftl` |
| Service 接口 | `java/service.java.ftl` |
| Service 实现 | `java/serviceImpl.java.ftl` |
| Controller | `java/controller.java.ftl` |
| 自定义 Mapper XML | `xml/mapper.xml.ftl` |

`sql/mysql.sql.ftl` 只提供菜单 DML 片段。NAMEWTA 不生成 Oracle、PostgreSQL 或 SQL Server 脚本，也不生成模块私有 SQL 文件：表结构变更合并到 `release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql`，初始化数据、菜单和回填合并到 `release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql`。

layered Java 模板位于 `java/layered/`：

| 目标职责 | 模板 |
|---|---|
| UseCase 接口 | `java/layered/usecase.java.ftl` |
| UseCase 实现 | `java/layered/usecaseImpl.java.ftl` |
| Service | `java/layered/service.java.ftl` |
| DAO | `java/layered/dao.java.ftl` |
| Mapper | `java/layered/mapper.java.ftl` |
| Controller | `java/layered/controller.java.ftl` |

所有新模块生成的自有 Java 类型和显式方法都必须提供简明中文 Javadoc。模板使用者应在公共入口、事务/锁边界、外部调用和兼容桥处补齐实际的 `@param`、`@return`、`@throws` 与副作用说明；禁止生成“处理业务”“执行操作”等无信息注释。JSON、ID、Redis、OSS、Notify、Log、Sa-Token 和 MyBatis-Plus 必须复用项目现有统一入口，不得在业务模块生成同义工具或框架直连代码。

模板上下文必须显式传入 `moduleLifecycle` 和 `architectureMode`。合法组合只有 `new + layered`、`legacy + classic`；缺失或交叉组合必须在渲染前失败。layered 模板不生成或继承 MyBatis-Plus `IService`/`ServiceImpl`。

classic 的 Java 模板仍使用 `BaseMapperPlus<Entity, Vo>` 与 `selectVo*`，只用于保持存量模块兼容。layered 必须改用 `java/layered/mapper.java.ftl`：Mapper 的读类型为 `domain/model/read/<ClassName>Row`，Service 将 Row 转换为 HTTP VO；不得让 layered Mapper/XML 直接面向 `domain/vo`。新模块的 DAO 业务调用必须落到同名 Mapper 方法和 XML statement，不得在 DAO 直接调用继承的 CRUD 方法绕过 XML。

查询读模型使用 `java/read.java.ftl`，输出到 `${packagePath}/domain/model/read/`，命名为 `<ClassName>Row`。读模型不是 HTTP VO，不得由 Controller 或 UseCase 直接返回；DAO 只负责从 Mapper/MP Page 组装 `PageResult<Row>`，Service 负责 Row/Entity 到业务结果或 HTTP VO 的转换；稳定跨能力的只读模型可以在后续演进为 `<ClassName>Projection`，但不应与包迁移同时批量重命名。

layered 分页边界：Controller 可以接收现有 `org.namewta.common.mybatis.core.page.PageQuery`，但必须在调用 UseCase 前解包为页码、页大小和排序基础值。UseCase 与 Service 不得导入 `PageQuery` 或 MyBatis `Page`；DAO 在内部构造分页对象并将结果收敛为 `PageResult<Row>`。

## AGENTS 模板

`agents/backend-module.AGENTS.md.ftl` 与 `agents/frontend-package.AGENTS.md.ftl` 只生成模块导读，不复制工程规范。每份手册保持短小，说明作用、组成、入口、依赖、最小验证和下一步阅读；详细规则继续由 `.agents/skills` 提供。

## 校验

```bash
node docs/fm/scripts/validate.mjs
```

校验模板清单、Vue 架构边界、CRUD method 和 Java POST 日志的静态合同。完整验收还必须使用代表性普通表与树表上下文渲染，再在目标前后端工程执行类型检查、测试和构建。
