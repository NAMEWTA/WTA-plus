---
name: engineering-standards
description: 为本仓库提供架构、模块边界、Java/Vue/MyBatis 固定结构和按 scope 路由的工程质量规范；代码或交付任务使用。
---

# wta-vue-plus-docs 工程规范

本 Skill 是项目架构、模块边界、固定代码结构、质量门禁和交付裁决层。先确定变更 scope，再加载最小充分 references；不要把一个子模块的语言或框架规则扩散到另一个子模块。

文中的“必须/禁止/MUST”是项目硬约束，涉及 MyBatis 基类、分层、HTTP 合同、权限、数据库、依赖方向和安全时不得自行替换；“建议/优先”允许根据任务范围调整。当前源码、配置、POM、测试与本 Skill 冲突时，以工作树证据为准，并记录需要修订的事实。

## 任务路由与执行

1. 代码、架构、公共合同、数据库、权限或交付任务读取[项目画像](references/project/00-project-profile.md)和[模块地图](references/project/01-module-map.md)；仅文档排版或拼写修正可跳过它们。
2. 将任务映射到 `repository`、`module:*`、`language:*`、`framework:*`、`runtime:*` 或具体 `path:*` scope。
3. 修改后端业务模块时，必须读取[后端模块模式登记表](references/project/03-backend-module-modes.md)，由登记表裁决 `layered` 与 `classic`；未登记的新业务模块一律按 `layered` 处理。命中下列领域时，再根据实际任务调用最小充分的开发导航或原子 Skill；跨领域任务可以组合调用，不相关领域不加载。
4. 按风险读取相关通用规则：[架构与边界](references/rules/architecture-and-boundaries.md)、[文件/目录/命名](references/rules/files-and-naming.md)、[文档与注释](references/rules/documentation-and-comments.md)、[API/错误/资源](references/rules/api-errors-resources.md)、[测试](references/rules/testing.md)、[安全与数据](references/rules/security-and-data.md)、[质量门禁](references/rules/quality-gates.md)、[评审与交付](references/rules/review-and-delivery.md)。新增/移动文件必须读取命名规则；修改 public contract 或非直观实现时读取注释规则；涉及 HTTP API 或 CRUD 时必须读取 API 规则，并执行其中“查询 GET、变更 POST、POST 使用 `@Log`”的传输与追踪约束。
5. 修改 `plus-ui-namewta/**` 时读取 [TypeScript 核心](references/typescript/core.md)、[代码组织与注释](references/typescript/code-organization-and-comments.md)、[Vue](references/typescript/frameworks/vue.md)和[Browser](references/typescript/runtimes/browser.md)；涉及 `packages/domains/**`、`packages/web-domains/**`、CRUD 页面、树表、表单或 OpenAPI 传输合同映射时再读取[前端 CRUD/API 实现规范](references/typescript/crud-api-and-pages.md)。
6. 修改 `wta-vue-plus-namewta/**` 时读取 [Java 核心](references/java/core.md)；涉及 Spring 应用、Web、配置或 Bean 生命周期时再读取 [Spring Boot](references/java/frameworks/spring-boot.md)；涉及事务、`@DS`、数据源切换、事务事件、DDL、建表或 schema 迁移时必须读取[数据源事务与建表](references/java/persistence-transactions-and-ddl.md)；涉及 entity/BO/VO、mapper、查询封装、CRUD、树结构、翻译、缓存或导入导出时再读取[后端 CRUD/查询实现规范](references/java/crud-query-and-common.md)。
7. 涉及架构迁移、存量偏差、例外、上游同步或 Ratchet 时读取[决策、迁移与例外](references/project/02-decisions-and-exceptions.md)，确认 current、target、Ratchet、`pending-decision` 和未到期例外；普通局部修复不因惯例加载整份决策记录。
8. 实现前检查 public API、依赖方向、安全、错误/取消/资源、数据库兼容和测试影响。涉及动态菜单/路由或菜单图标时，同时读取 `namewta-fullstack-development` 的 `permission-routing.md`、`contract-mapping.md` 和 `backend/mapper-and-sql.md`，按动态路由与图标协议合同验收。
9. 实现后运行项目画像中该 scope 的真实质量门禁，并按[评审清单](references/project/review-checklist.md)报告命令、退出码、未验证项和残余风险。测试范围应覆盖受影响合同；小型可逆修改不添加只重复实现的测试。

涉及统一通知、站内信、对外渠道、通知收件箱、消息盒子、Outbox 或供应商回调时，必须读取[统一通知规范](references/notification.md)；该文件规定唯一 public API、目标/渠道合同、分层边界、幂等、回调和验证要求。

## 固定实现合同

- 后端业务必须先按登记表选择 `layered` 或 `classic`，不能把两种模式混用。新 `layered` 模块固定为 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> Mapper XML`；存量 `classic` 模块保持其既有结构。
- 标准业务 Entity 继承项目 `BaseEntity`；Mapper 按现有模式继承 `BaseMapperPlus`，并按 `BaseMapperPlus -> wrapper/QueryBuilder -> MPJ -> XML` 选择查询方式。不得自行引入平行基类、ORM 或同义持久化层。
- `layered` 中 DAO 是唯一持有 Mapper 和 MyBatis 持久化类型的业务层；UseCase/Service 不得导入 Mapper、`IService` 或 `ServiceImpl`。具体字段、泛型和 XML 合同以 [后端 CRUD/查询实现规范](references/java/crud-query-and-common.md) 及当前源码为准。
- 修改公共合同、权限、数据库、事务、依赖方向或安全规则时，即使任务很小，也必须执行对应硬约束和验证；按需加载只减少无关上下文，不减少命中规则。

## 领域知识路由

`engineering-standards` 是 MUST、质量门禁、Ratchet 和交付裁决层；下列 Skill 是当前仓库实现导航、事实、能力与调用面的知识地图，不能替代本 Skill 的规范裁决。描述不够明确或与当前工作树冲突时，以其引用的源码、配置和 POM 为证据，不把摘要当成永久事实。

| 实际任务领域 | Skill |
|---|---|
| `plus-ui-namewta` 与 `wta-vue-plus-namewta` 的业务垂直切片、CRUD/API、领域资源、权限菜单、事务、SQL、前端 App 组合或全栈交付 | [namewta-fullstack-development](../namewta-fullstack-development/SKILL.md) |
| 新增模块、Profile/System/Workflow/Notify 事实地图、跨模块 API 或业务接入 | [wta-module-guide](../wta-module-guide/SKILL.md) |
| `wta-common` 子模块选择、BOM、Maven 依赖、Utils/Helper、Redis/Login/JSON/Excel/OSS 等公共入口 | [wta-common-modules-guide](../wta-common-modules-guide/SKILL.md) |

## 约束

- MUST 规则只有在 `Verification` 可执行或给出精确 review 证据时成立。
- CRUD 只读查询使用 HTTP `GET`，产生业务状态变化的操作使用 HTTP `POST`，每个 POST 业务接口使用 `@Log` 记录调用追踪；具体范围、迁移与验证以 `API-005` 为准。
- 不适用规则不加载；依赖、构建输出、生成声明和冻结 scope 不手工修改。
- 不通过删除测试、关闭核心规则、放宽编译配置或扩大例外获得通过。
- 新代码遵循 Target；存量偏差按 Migration/Ratchet 处理，不发动无关全仓重写。
- 前后端是独立 Git Submodule；在各自仓库完成变更与验证后，再单独更新父仓库指针。
- 项目事实变化时先更新 Project Profile/Module Map，再更新规则，不静默猜测。

Skills 与模板发布前运行：

```bash
node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs
```
