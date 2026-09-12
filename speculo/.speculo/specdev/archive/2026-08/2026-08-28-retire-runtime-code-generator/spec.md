---
schema_version: 3
artifact: spec
change: 2026-08-28-retire-runtime-code-generator
status: ready
ready_for_tickets: true
sources:
  - "USER-DECISION:2026-08-28-retire-runtime-code-generator"
  - "<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/design-tree.json</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ADR.md</Path>"
  - "CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-gen/**</Path>"
  - "CODE:<Path>plus-ui-namewta/packages/domains/gen/**</Path>"
  - "CODE:<Path>plus-ui-namewta/packages/web-domains/gen/**</Path>"
---

# Spec: 完整退役运行时代码生成器

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/CONTEXT.md</Path>`
- **Planning Depth：** Deep。删除范围包含公共 HTTP/OpenAPI 合同、权限菜单和不可逆数据库结构；生产兼容、备份与回滚经用户确认不适用。

## 1. 问题与目标

### 问题陈述

项目已经把当前 CRUD 标准模板和注意事项集中到 `<Path>docs/fm/**</Path>`，AI 与开发者可以据此完成完整资源纵切片。产品运行时仍保留另一套低代码生成能力：后端 `ruoyi-gen` 模块、`/tool/gen` HTTP 接口、前端 gen Domain/Web Domain、Admin 页面组合、OpenAPI 当前合同、权限菜单以及 `gen_table*` 元数据表。

继续保留这套能力会形成两个 CRUD 事实来源，并使构建图、授权面、数据库 schema 和前端运行时持续承担已经不再需要的维护成本。仅隐藏菜单不能解决后端接口、前端包、合同和数据结构仍然存在的问题。

### 目标用户与场景

- 基座维护者需要在不携带运行时代码生成器的前提下构建、测试和部署前后端。
- Admin 用户不应再看到或访问代码生成、导入表、预览代码、生成配置和下载代码等管理能力。
- AI 与业务开发者继续使用 `<Path>docs/fm/**</Path>` 作为 CRUD 标准参考资产。
- API、菜单和数据库基座的消费者需要看到一致的最终状态，不再发现当前生成器合同或专属数据对象。

### 成功标准

- 后端 Maven reactor、默认 Admin 组合、Springdoc 扫描和可部署产物均不再包含 `ruoyi-gen`。
- 当前运行应用不再映射 `/tool/gen` 接口，当前 OpenAPI 合同不再公开生成器路径或模型。
- 前端工作区与 Admin 组合不再包含 gen Domain、Web Domain、页面 registration、服务实例或依赖。
- 菜单、功能权限、角色菜单关系及仅承载生成器的空目录被物理删除。
- `gen_table_column` 与 `gen_table` 及其中全部数据被永久删除。
- `<Path>docs/fm/**</Path>` 保持为可校验的唯一 CRUD 标准模板资产。
- 受影响前后端质量门禁通过，残留扫描只命中本 Spec 明确允许的历史或无关语义。

### 非目标

- 不用新生成器、CLI、Agent、插件或低代码平台替代被删除能力。
- 不改写 `<Path>docs/fm/**</Path>` 的模板行为，也不在本 change 生成新的业务 CRUD 代码。
- 不建立旧 `/tool/gen` API、前端路由、菜单权限或数据库表的兼容门面。
- 不设计生产迁移、数据备份、滚动发布、upgrade 兼容或回滚恢复。

## 2. 解决方案与外部行为

### 解决方案摘要

将运行时代码生成器作为一个跨后端、前端、合同、授权和数据的完整纵向能力退役：删除后端模块及组装，删除前端领域包及 Admin 组合，重建当前 OpenAPI 合同，在 NAMEWTA append-only SQL 末尾追加物理清理块，并同步所有仍描述生成器为当前能力的项目事实文档。

`<Path>docs/fm/**</Path>` 保留且继续通过自身校验脚本证明完整性。冻结上游初始化 SQL 和不可变 OpenAPI 历史 revisions 只作为历史来源保留，不代表当前产品仍支持生成器。

### 主要流程

1. 基座维护者构建后端时，Maven 不再解析、编译或打包 `ruoyi-gen`；Admin 默认和 core 两种组合均正常产出。
2. Admin 启动时只组合仍存在的 Domain 与 Web Domain；服务端菜单中不存在生成器条目，前端也不注册生成器 component key。
3. 调用者读取当前 OpenAPI 合同时看不到 `/tool/gen*`、`GenTable`、`GenTableColumn` 或生成器专属 operation。
4. 全新可丢弃 MySQL 8.4 基座按既有顺序执行冻结上游 SQL、NAMEWTA DDL 和 DML 后，最终 schema 与菜单数据中不存在运行代码生成器。
5. AI 与开发者创建 CRUD 能力时读取 `<Path>docs/fm/catalog.json</Path>`、上下文合同和模板，不依赖产品运行时。

### 边界、失败与稳定错误行为

- `/tool/gen` 及其子路径不保留专用兼容响应；请求按应用现有“无匹配路由”行为处理，不新增或虚构错误码。
- 若删除后仍有 Maven、TypeScript、manifest 或 OpenAPI 消费者引用生成器，编译、架构检查或合同漂移检查必须失败，不能用空包、stub、alias 或跳过配置兜底。
- 若动态菜单数据仍出现生成器 component key，Admin 保持现有 manifest-only 失败关闭与诊断行为；本 change 不增加兼容页面。
- NAMEWTA DDL 的生成器表删除块按基座初始化合同执行，不承诺重复执行；DML 删除使用最终状态语义，重复执行不得重新创建菜单或授权。
- SQL 执行失败时修正基座或脚本后重建可丢弃环境；不建立生产数据补偿或恢复路径。

### 状态转换与不变量

```text
当前基座
  ├─ backend: ruoyi-gen 已装配，/tool/gen 可见
  ├─ frontend: gen 包与 Admin manifest 已选择
  ├─ contract: 当前 OpenAPI 含生成器路径/模型
  └─ data: 菜单/权限与 gen_table* 存在
          |
          v  基座硬退役
目标基座
  ├─ backend/frontend 当前构建图无生成器
  ├─ 当前 HTTP/OpenAPI/菜单表面无生成器
  ├─ gen_table* 永久不存在
  └─ docs/fm 是保留的 CRUD 标准模板资产
```

- `<Path>docs/fm/**</Path>` 与运行时代码生成器是不同 bounded context；删除后者不得删除前者。
- 前端 App 只能显式组合真实存在的 Domain/Web Domain；不得保留 id 为 `gen` 的空组合项。
- 后端与前端当前合同同步删除，不提供兼容窗口。
- `sys_role_menu` 关系必须先于对应菜单删除；`gen_table_column` 必须先于 `gen_table` 删除。
- 冻结的 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 不修改；NAMEWTA 变化只追加到两个现有 SQL 文件末尾。
- 当前 OpenAPI 指针与生成 TypeScript 必须无生成器；不可变历史 revisions 不重写。

## 3. 用户故事

- **US-001**：作为基座维护者，我希望后端构建图和部署产物彻底移除 `ruoyi-gen`，以便不再维护或暴露已退役的低代码运行能力。
- **US-002**：作为 Admin 用户，我希望导航、页面和按钮权限中不再出现代码生成器，以便管理端只呈现真实受支持的能力。
- **US-003**：作为 API 合同消费者，我希望当前 OpenAPI 不再声明生成器接口和模型，以免新代码继续依赖已删除能力。
- **US-004**：作为基座数据库维护者，我希望生成器菜单数据和专属元数据表被永久删除，以便最终 schema 与产品能力一致。
- **US-005**：作为 AI 或业务开发者，我希望 `<Path>docs/fm/**</Path>` 保持完整可校验，以便继续按当前工程标准实现 CRUD。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 应用源码处于目标基座 | 解析后端 Maven reactor 与 Admin profiles | 不存在 `ruoyi-gen` module、dependencyManagement 或 bundle 依赖，其他业务模块组合保持不变 | POM 静态扫描；后端双 bundle clean package |
| AC-002 | 后端按目标代码启动或生成 OpenAPI | 枚举请求映射与 Springdoc 合同 | 不存在 `/tool/gen` 及子路径、生成器 operation 或 `GenTable*` schema | 后端源码扫描；当前 OpenAPI 快照检查 |
| AC-003 | 前端依赖已安装 | 执行 workspace 解析、架构检查和 Admin 组合测试 | gen Domain/Web Domain 包、依赖、service、manifest、selected id 和 component registration 均不存在 | pnpm lock/workspace 扫描；architecture 与 Vitest |
| AC-004 | Admin 使用目标静态资源和目标菜单数据 | 打开导航及首页 | 不显示代码生成、生成配置或“代码生成器”能力宣传；其他导航与首页内容正常 | Admin 组合测试；静态文案扫描；需要时人工 smoke |
| AC-005 | NAMEWTA DML 在具有上游基线菜单的可丢弃数据库执行 | 查询生成器菜单和角色关系 | 九个固定菜单 ID 及其全部 `sys_role_menu` 关系均不存在，且仅承载生成器的系统工具目录不存在 | SQL 顺序 review；MySQL 8.4 最终状态查询 |
| AC-006 | NAMEWTA DDL 在具有上游基线表的可丢弃数据库执行 | 查询 `information_schema` | `gen_table_column` 与 `gen_table` 均不存在，原有数据不可恢复 | SQL 顺序 review；MySQL 8.4 最终状态查询 |
| AC-007 | 可丢弃 MySQL 8.4 数据目录为空 | 按 Docker 既有初始化顺序启动 MySQL | 初始化成功，最终数据库同时满足 AC-005 与 AC-006 | `<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>` fresh baseline 接缝 |
| AC-008 | 后端当前合同已移除生成器 | 获取、激活并生成新的 OpenAPI revision | current pointer 与生成 TypeScript 不含生成器路径、operation 和模型，`openapi:check` 无漂移 | `@namewta/tooling-openapi` fetch/generate/check |
| AC-009 | 运行时代码生成器已删除 | 执行有范围的全仓残留扫描 | 活动构建、运行代码、当前合同、当前 SQL 增量和当前事实文档中无生成器残留；只允许冻结上游 SQL、不可变 OpenAPI 历史 revisions、`docs/fm` 说明及明确无关的通用 generate 术语 | `rg` 允许清单扫描与人工 review |
| AC-010 | `<Path>docs/fm/**</Path>` 保留 | 执行模板资产校验 | catalog、上下文合同及 Java/Vue/XML/SQL 模板仍完整并通过静态合同校验 | `node docs/fm/scripts/validate.mjs` |
| AC-011 | 代码和合同达到目标状态 | 阅读项目画像、模块地图、前后端导航与 README | 文档不再把 `ruoyi-gen`、gen Domain/Web Domain 或 Admin 生成器描述为当前/待退役运行能力，并继续区分静态模板与生成物 | 当前事实文档扫描与 diff review |
| AC-012 | 全部删除和文档同步完成 | 执行前后端完整适用门禁 | 后端测试、默认/full 与 core 构建，以及前端 architecture、lint、typecheck、test、build 均通过；未运行项不冒充通过 | Maven 与 pnpm 根级质量门禁 |

## 5. 范围

### IN

- 后端 `ruoyi-gen` 运行模块及其 Maven reactor、dependencyManagement、Admin profile 和 Springdoc 组装面。
- 前端 gen Domain、Web Domain、页面/运行时/manifest、Admin service 与组合、workspace 依赖及相关架构/组合测试。
- 仅把 `/tool/gen` 当通用下载测试样例的 fixture 文本替换为中性非生成器资源；通用下载能力本身保留。
- 当前 OpenAPI revision、pointer 与生成 TypeScript 合同的受控重建。
- NAMEWTA DDL/DML 末尾追加的表、菜单、功能权限、角色菜单关系和空系统工具目录物理删除。
- Admin 首页和当前项目事实文档中已失效的生成器描述。
- 与当前源码事实冲突的父级工程 Skill、前端/后端导航和相邻模块能力地图同步。

### REUSE

- `<Path>docs/fm/**</Path>` 的 CRUD 标准模板、catalog、上下文合同和校验脚本。
- `<Path>plus-ui-namewta/tooling/openapi/**</Path>` 的不可变 revision、current pointer 和确定性生成机制。
- `<Path>plus-ui-namewta/tooling/architecture/**</Path>` 的 workspace、包边界和 App 组合检查。
- Admin 现有 manifest-only 路由诊断、通用下载、HTTP、权限和反馈宿主能力。
- `<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>` 的 MySQL 8.4 fresh 初始化顺序。

### OUT

- **OOS-001**：不修改 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 或其 PostgreSQL、Oracle、SQL Server 上游变体；它们属于冻结上游基线。
- **OOS-002**：不删除不可变 OpenAPI 历史 revisions；只更新 current 指针和当前生成合同。
- **OOS-003**：不删除或重写过去由生成器产出的业务代码；真实业务模块继续由各自 owner 维护。
- **OOS-004**：不删除路由 `generateRoutes`、密码生成、OpenAPI 生成等与低代码 CRUD 运行能力无关的通用语义。
- **OOS-005**：不激活、扩展或删除 `<Path>plus-ui-namewta/tooling/generators/**</Path>` 的工作区脚手架占位；它不是当前运行时代码生成器。
- **OOS-006**：不改变认证、Client 隔离、菜单裁剪或其他模块的权限语义。
- **OOS-007**：不承担生产数据迁移、备份、兼容、滚动发布、upgrade 或回滚恢复。

## 6. 已锁定实现约束

- **DEC-001**：以 `<Path>docs/fm/**</Path>` 静态标准模板替代运行时代码生成器，不保留兼容 API、UI 或数据面。来源：change `ADR-001`。
- **DEC-002**：永久删除 `gen_table_column`、`gen_table` 及其中数据，不创建备份、归档、兼容视图或恢复路径。来源：change `ADR-002`。
- **DEC-003**：前端删除后必须继续符合编译期模块化 Monorepo、Domain/Web Domain 分离、App 显式组合和 manifest-only 路由合同。来源：永久 `ADR-0012` 至 `ADR-0015`。
- **DEC-004**：NAMEWTA 数据库只维护 MySQL 8.4；冻结上游 SQL 不承载项目退役变化。来源：永久 `ADR-0011`。
- **DEC-005**：NAMEWTA SQL 仍遵守双文件和 append-only 合同；DDL 与 DML 分类、变更标识和执行顺序不得破坏。来源：工程规范 `PERSIST-006`。
- **DEC-006**：当前 OpenAPI 合同必须通过快照来源和生成工具更新，不直接手改生成 TypeScript；历史 revisions 保持不可变。来源：工程规范 `ARCH-006` 与当前 OpenAPI 工具合同。
- **DEC-007**：基座阶段无兼容与生产迁移义务；项目通用规范中的生产回滚要求对本 change 明确不适用。来源：`USER-DECISION:base-hard-retirement`。

## 7. 数据、接口与兼容

- **公共接口变化：** 删除全部 `/tool/gen*` HTTP 路径、operation 和请求/响应模型；无替代接口、redirect、deprecated stub 或兼容错误码。
- **数据模型与持久化：** 物理删除 `gen_table_column` 后物理删除 `gen_table`；物理删除生成器功能权限、页面菜单、角色菜单关系和成为空目录的系统工具菜单。数据库内历史配置永久丢失。
- **菜单数据范围：** 固定删除 `1761400000000000003`、`1761400000000000115`、`1761400000000000116`、`1761400000000001055` 至 `1761400000000001060` 九个 `menu_id` 及其全部角色关系；同时验证活动菜单中不存在 `tool:gen:*` 权限或 `tool/gen` component。
- **兼容要求：** 不适用。当前项目是基座，用户明确不保留旧前端、旧 API consumer、旧菜单或旧数据库兼容性。
- **迁移要求：** 只在现有 NAMEWTA DDL.sql 与 DML.sql 末尾追加硬退役块；全新可丢弃 MySQL 8.4 环境执行完整初始化后必须得到目标最终状态。不存在生产 upgrade 或数据迁移合同。
- **发布或运维影响：** 不适用。没有生产部署或滚动窗口；实现验收仅允许在可丢弃的基座环境执行物理删除。
- **安全影响：** 删除授权面和服务端接口，不以 UI 隐藏替代清理；其他 Client、角色、菜单和权限数据不得受影响。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 生成器 endpoint、菜单和权限必须同时消失，不留下可绕过 UI 调用的服务端入口；删除范围不得波及其他 Client 或角色授权。
- **NFR-002 性能与容量：** 不设置新的数值阈值。目标构建不得重新引入生成器依赖，运行产物不携带生成器代码与专属模板资源。
- **NFR-003 可用性与可靠性：** 非生成器模块的默认/full、core 后端组合和 Admin 前端构建保持可用；未知动态 component key 继续失败关闭。
- **NFR-004 可观测性与运营：** 不适用。能力被物理删除且不存在生产运行期；不新增监控、告警或迁移仪表盘。
- **NFR-005 可维护性：** 当前模块地图、工程 Skill、README 和架构测试必须反映“运行生成器已退役、docs/fm 为静态标准模板”的真实状态。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 后端 Maven reactor 与部署组合 | 构建/集成 | AC-001, AC-002, AC-012 | 在 `<Path>ruoyi-vue-plus-namewta/**</Path>` 运行 `./mvnw test`、`./mvnw clean package -DskipTests`、`./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | Ticket Evidence 中的命令、退出码与跳过项 |
| 前端包图、App 组合与单元测试 | 架构/类型/测试/构建 | AC-003, AC-004, AC-012 | 在 `<Path>plus-ui-namewta/**</Path>` 运行 `pnpm architecture:check`、`pnpm architecture:test`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod` | Ticket Evidence 中的命令、退出码与诊断 |
| 当前 OpenAPI 快照与生成合同 | 合同 | AC-002, AC-008 | 使用 `pnpm --filter @namewta/tooling-openapi openapi:fetch`、`pnpm --filter @namewta/tooling-openapi openapi:generate`、`pnpm --filter @namewta/tooling-openapi openapi:check` 的既有流程，并扫描 current snapshot 与生成 TypeScript | revision provenance、diff 与 check 结果 |
| NAMEWTA MySQL 最终状态 | 数据库集成 | AC-005, AC-006, AC-007 | 在可丢弃 MySQL 8.4 数据目录按 `<Path>ruoyi-vue-plus-namewta/script/docker/docker-compose.yml</Path>` 的既有初始化顺序执行，并查询 `sys_menu`、`sys_role_menu` 与 `information_schema` | SQL 执行结果和最终状态查询 |
| CRUD 标准模板资产 | 静态合同 | AC-010 | 在父仓库运行 `node docs/fm/scripts/validate.mjs` | validator 退出码与输出摘要 |
| 有范围的残留扫描与文档 review | 静态/人工 | AC-009, AC-011 | 对活动源码、POM、当前 OpenAPI、NAMEWTA SQL 和当前事实文档执行 `rg`；对照 OOS 允许清单 review | 匹配清单、允许理由与零意外残留结论 |

不要求浏览器 E2E：本 change 删除的是整项管理能力，关键外部证据由服务端合同消失、前端组合/构建消失和数据库菜单最终状态共同提供。若 Ticket 实现引入新的可见导航行为或现有测试无法证明 AC-004，再把定向 Admin smoke 或 Playwright 提升为 required。

## 10. 风险、假设与未决问题

### 风险

- Maven 或 Springdoc 遗漏引用会导致模块目录删除后构建失败，或当前 OpenAPI 继续暴露已删除合同。
- 前端 package、lockfile、Admin service、manifest 与架构基线若未同步删除，会造成 workspace 解析失败或动态组件残留。
- 直接搜索 `gen` 容易误删 OpenAPI 生成、路由生成、密码生成和历史不可变工件；必须使用 OOS 允许清单裁决。
- NAMEWTA SQL 是 append-only，错误修改历史前缀会破坏其他在途变更；实现必须证明只在末尾追加。
- 系统工具目录当前只承载生成器页面；若实施前源码事实变化并新增真实子菜单，删除该目录属于偏差，必须退回 Spec 修订。
- 当前聚合仓库和两个子模块均有既存未提交改动；实施需要保留用户改动并逐仓验证边界。

### 已采用的低影响假设

- `sys_menu` 固定 ID 和父子关系仍与当前 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>` 一致；实施前以静态查询重验，不一致时停止而非扩大删除条件。
- `gen_table*` 没有生成器之外的消费者；实施前以 Java、SQL 和配置扫描重验。
- 当前 OpenAPI 工具仍以不可变 revision、current pointer 和确定性 TypeScript 生成工作；以 `openapi:check` 和工具测试验证。
- `<Path>docs/fm/**</Path>` 当前未完成的用户改动属于本 change 的保留资产；只运行校验和同步退役状态说明，不回退其内容。

### 未决问题

无。
