# 后端 CRUD、查询与公共设施实现规范

适用 `module:wta-vue-plus-namewta` 中业务 CRUD、MyBatis 查询、树结构、翻译、缓存和导入导出。父仓库 `docs/fm` 定义标准骨架，demo 展示完整链路，system/workflow 展示复杂领域约束。

### BE-CRUD-001 标准分层与实现基线

Scope: `path:wta-vue-plus-namewta/wta-modules/**`

Level: MUST

Source: `repository-fact` (`docs/fm/java/{domain,bo,vo,mapper,service,serviceImpl,controller}.java.ftl`, `docs/fm/xml/mapper.xml.ftl`, `wta-system/**`, `wta-demo/**/TestDemo*`)

Rule: CRUD 目录和调用链必须先读取模块模式登记表。classic 存量模块保持 entity、BO、VO、mapper、service/interface、service/impl、controller 的兼容职责；layered 新模块强制 `Controller/Listener/API Adapter -> UseCase -> Service -> DAO -> Mapper -> XML`，并使用对应 layered 模板。逐项引用精确模板：Entity 用 `docs/fm/java/domain.java.ftl`，BO 用 `bo.java.ftl`，VO 用 `vo.java.ftl`，Read Model 用 `read.java.ftl`，Mapper 按模式选择 `mapper.java.ftl` 或 `layered/mapper.java.ftl`，classic Service 用 `service.java.ftl`/`serviceImpl.java.ftl`，layered 使用 `layered/service.java.ftl`、`layered/dao.java.ftl` 和 UseCase 模板，Controller 按模式选择对应模板，自定义 XML 用 `docs/fm/xml/mapper.xml.ftl`。模板只建立骨架；修改 system/workflow 等成熟模块时，保留同模块已有的关系维护、权限、缓存、事务、导入导出和条件装配，不把复杂用例退化成模板 CRUD。已知不规范、处于待重构状态的模块不得作为新代码样例。

Verification: 全链路文件与同模块最近实现对照；controller/service/mapper 职责 review；受影响 Maven reactor build/test。

### BE-CRUD-002 Entity、BO、VO 与映射

Scope: `path:wta-vue-plus-namewta/wta-modules/**/domain/**`

Level: MUST

Source: `repository-fact` (`TestDemo`, `TestDemoBo`, `TestDemoVo`, `docs/fm/java/**`)

Rule: 项目自有业务 entity 映射表并继承 `BaseEntity` 取得 `createDept/createBy/createTime/updateBy/updateTime` 自动填充字段；表中 `version`、`del_flag` 分别映射为显式 `@Version`、`@TableLogic` 字段，完整 schema 基线遵循[数据源事务与建表](persistence-transactions-and-ddl.md)。BO 使用 `@AutoMapper(target=..., reverseConvertGenerate=false)`、Serializable 和 Add/Edit validation groups；VO 使用面向响应的 `@AutoMapper`，只在确有导出或翻译需求时添加 Excel/Translation 注解。跨层转换沿用 `MapstructUtils`，不得把持久化 entity 直接作为外部合同。

Verification: 表 schema/entity/BO/VO 字段对照；mapping/validation 测试；编译并 review 安全字段和 nullable 语义。

### BE-CRUD-003 Mapper 与数据权限

Scope: MyBatis mapper interfaces and XML under `wta-vue-plus-namewta`

Level: MUST

Source: `repository-fact` (`BaseMapperPlus`, `TestDemoMapper`, `SysUserMapper`, `SysRoleMapper`)

Rule: 标准 mapper 继承 `BaseMapperPlus<Entity, Vo>`。查询按 `BaseMapperPlus` 内建能力 -> fresh lambda wrapper/`QueryBuilder` -> MyBatis-Plus-Join 类型化查询 -> Mapper XML 的阶梯选择；只有前一层不能清晰、类型安全、可维护地表达查询时才进入下一层。短小、静态、无需动态拼接的 SQL 注解仅作为经同模块证据确认的窄例外；禁止把长 SQL、动态条件、复杂 join、子查询或大段更新直接堆进 Mapper 注解。XML 位于 `src/main/resources/mapper/<module>/`，namespace、参数名和 `${ew.customSqlSegment}` 等合同必须准确。数据权限覆盖真实执行查询/更新/删除的方法；`@DataColumn.value` 与 SQL 中实际列或 alias 完全一致，join SQL 使用带 alias 的字段。

Verification: mapper 方法到最终 SQL 追踪；不同权限范围负向测试；SQL/alias review；相关集成测试。

### BE-CRUD-004 查询条件与 fresh wrapper

Scope: service query builders and `wta-common-mybatis`

Level: MUST

Source: `repository-fact` (`QueryBuilder`, `BaseMapperPlus.lambda()`, `LambdaCrudChainWrapper`, `docs/fm/java/serviceImpl.java.ftl`, `docs/fm/java/layered/dao.java.ftl`)

Rule: 动态查询使用 `QueryBuilder.lambda`、`Wrappers.lambdaQuery` 或 mapper 的 fresh `lambda()`，根据类型选择 `eqIfPresent`、`eqIfText`、`likeIfText`、`betweenParams` 等条件，并给出确定排序。classic 模式可由 Service 构建简单 Wrapper；layered 模式的查询条件、分页、条件更新和行锁必须统一封装在 DAO，Service/UseCase 不得出现 MyBatis-Plus Wrapper。链式 wrapper 有可变查询/更新状态，不得跨请求、线程或独立操作缓存复用；每个操作取得新实例，复用同一实例时必须先明确 `clear()` 能否完整清除所用状态。

Verification: 空字符串/null/日期范围/排序测试；review wrapper 创建位置和 Bean 字段；并发与重复调用测试。

### BE-CRUD-005 Service 不变量、唯一性与删除校验

Scope: service implementations under `wta-vue-plus-namewta/wta-modules/**`

Level: MUST

Source: `repository-fact` (`TestDemoServiceImpl`, `docs/fm/java/serviceImpl.java.ftl`, system service implementations)

Rule: classic service 负责 `buildQueryWrapper`、映射、领域不变量、唯一性和删除前校验；layered DAO 负责查询条件、分页、条件更新、行锁和持久化映射，layered Service 负责业务规则、唯一性、状态/删除校验和外部端口，UseCase 负责跨 Service 编排与事务边界。insert 成功后按合同回填生成主键；update/status/sort 只更新允许字段；唯一性检查在编辑时排除当前主键；无权限或违反不变量使用现有 `ServiceException` 合同失败，不以 mapper 返回 0 静默掩盖关键业务失败。

Verification: insert/update/duplicate/delete forbidden 测试；review `validEntityBeforeSave` 和 checkUnique；事务集成测试。

### BE-CRUD-006 Controller transport 与安全注解

Scope: Spring MVC controllers under `wta-vue-plus-namewta/wta-modules/**`

Level: MUST

Source: `repository-fact` (`TestDemoController`, Java controller template, system controllers) + `user-decision` (`API-005`)

Rule: controller 继承/复用现有 Web 响应合同，使用 `R`/`PageResult`、Bean Validation groups 和准确的 path/query/body 参数。受保护管理端放 `controller/admin`，`@SaIgnore` 匿名接口放 `controller/anonymous`；只有真实客户端存在时建立对应目录，已登录自服务接口先由业务规格裁决访问面。`@SaIgnore` 只跳过登录鉴权，不免除签名、时间戳、重放防护、幂等、审计与日志脱敏。CRUD 全部遵循 [API-005](../rules/api-errors-resources.md)：list/detail/tree/options 等只读查询使用 `@GetMapping`，不接收 request body；add/edit/remove、批量删除、状态变化和排序更新使用 `@PostMapping`，不得使用 `@PutMapping`、`@PatchMapping`、`@DeleteMapping` 或对应 `RequestMethod`。每个 `@PostMapping` 业务方法必须配置 `@Log(title = "...", businessType = BusinessType.XXX)`，新增使用 `INSERT`，修改/状态/排序使用 `UPDATE`，删除使用 `DELETE`；按请求和响应敏感性设置 `excludeParamNames`、`isSaveRequestData`、`isSaveResponseData`。各操作 URL 必须无映射冲突并与前端一致。写操作同时按风险使用 `@SaCheckPermission`、`@RepeatSubmit`，controller 不承载核心事务或多表编排；前端隐藏按钮不能替代这里及数据层的授权。

Verification: MockMvc/集成测试 status/body/validation/permission；前端 API 对照；对受影响 controller 搜索 mapping annotation 和 `RequestMethod`，确认查询使用 `@GetMapping`、变更使用 `@PostMapping` 且 CRUD 不含 PUT/PATCH/DELETE；逐个核对 POST 方法的 `@Log`、`BusinessType` 和敏感字段保存配置；annotation 与 permission string review。

### BE-CRUD-007 多表写入、缓存与副作用

Scope: service methods that modify relations, cached data or external state

Level: MUST

Source: `repository-fact` (`SysUserServiceImpl`, system cache-bearing services, workflow services)

Rule: 用户-角色、用户-岗位、角色-菜单等关联更新和主体写入位于覆盖完整用例的 `@DSTransactional` 中，并先校验引用、Client/用户类型等不变量。`@DSTransactional` 默认对 `Exception` 回滚，不为模仿 Spring 写法重复参数；改变 rollback/noRollback 或 propagation 时必须有用例证据。写后失效所有受影响缓存；提交阶段事件使用 `@DsTxEventListener`，外部会话踢出、通知或其他副作用说明发生在提交前后何处以及失败补偿，不能留下数据库、缓存和会话互相矛盾的状态。

Verification: 中途失败回滚、关联清空/替换、跨 Client 拒绝和缓存失效测试；review `@DSTransactional` 代理边界、`@DsTxEventListener` phase 与副作用顺序。

### BE-CRUD-008 树结构不变量

Scope: tree entities and services under `wta-vue-plus-namewta/wta-modules/**`

Level: MUST

Source: `repository-fact` (`docs/fm/java/serviceImpl.java.ftl` tree branch and demo tree implementation)

Rule: 树节点保存时统一处理 root value、parent existence、self-parent、ancestor path 和排序。移动节点必须拒绝选择自身或后代为父节点，并在同一一致性边界更新后代 ancestors；删除前检查或明确处理子节点。前后端对 root、id、parentId、children 的表示必须一致。

Verification: root/child insert、move、cycle rejection、descendant ancestors、delete-with-children 测试；事务回滚；前后端树合同 review。

### BE-CRUD-009 翻译使用批量合同

Scope: `wta-common-translation` implementations and VO `@Translation` usage

Level: MUST

Source: `repository-fact` (`TranslationInterface`, `TranslationJsonFieldProcessor`, `UserNameTranslationImpl`)

Rule: 新翻译实现使用 `@TranslationType` 注册，并为列表型响应实现 `translationBatch`，保持输入 key 到结果的映射以及逗号 ID 的原顺序语义。JSON 增强按 collect、prepare、process 三阶段工作；实现不得在逐字段 process 中制造可避免的 N+1。批量失败或缺失翻译按现有回退合同处理，不改变原字段所有权。

Verification: 单 key、批量、重复 key、逗号 ID、缺失值与失败回退测试；查询次数断言或日志/SQL 观察。

### BE-CRUD-010 导入导出与条件装配

Scope: Excel import/export and optional workflow/module beans

Level: MUST

Source: `repository-fact` (`TestDemoController`, Excel infrastructure, workflow `@ConditionalOnEnable` usage)

Rule: 导入先解析并执行现有校验，再转换和持久化，响应返回可理解的分析结果；批量写入的原子性与部分失败语义必须明确。导出使用 VO 与现有 Excel builder，不泄露未声明字段。可选 workflow 能力的 controller、service、listener、rule/config 等入口保持同一条件装配合同，禁用时不得残留依赖缺失的孤立 Bean。

Verification: 合法/非法/部分失败导入、导出字段与权限测试；启用/禁用两种 application context 测试；Maven build/test。

### BE-CRUD-011 生成模板演进

Scope: `path:docs/fm/**`

Level: SHOULD

Source: `repository-fact` (`docs/fm` Java, Vue, React, XML and SQL templates) + `user-decision` (`API-005`)

Rule: 会重复污染新模块的缺陷在 `docs/fm` 修复，一次性领域行为留在业务模块。模板变化同时检查 Java/Vue/React/API/types/XML/SQL 关联输出，以及 CRUD/tree、unique、status、sort、between、dict、permission 分支；CRUD controller 与前端 service/API 模板必须同步执行 [API-005](../rules/api-errors-resources.md)，查询只生成 GET，变更只生成 POST，且每个 POST controller 方法生成准确、安全的 `@Log`。Vue 模板生成 domain/web-domain 资源切片，不自动覆盖共享 package 入口、manifest 或 App 组合文件。SQL 只支持 `docs/fm/sql/mysql.sql.ftl`，该模板是合并到 `release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql` 的菜单 DML 片段，不生成模块私有脚本；NAMEWTA 表结构合并到同目录 `50-namewta-ddl.sql`，初始化数据、菜单和回填合并到 `60-namewta-dml.sql`。两份基座保持完整、可重建，不按时间无限追加迁移文件。生成结果必须能编译并保持前后端合同一致。

Verification: 先运行 `node docs/fm/scripts/validate.mjs`；再用覆盖受影响条件的生成元数据产出样例并 diff；搜索生成的 controller 和 API，确认查询为 `@GetMapping`/`method: 'get'`，变更为 `@PostMapping`/`method: 'post'`，不存在 PUT/PATCH/DELETE，且每个 POST 方法均有准确的 `@Log`；前端 lint/type diagnostic/build；后端 compile/test；检查未选择分支无回归。
