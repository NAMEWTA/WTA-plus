# Spring Boot 4 工程规范

适用明确使用 Spring Boot 的后端模块。当前是 Java 21、Spring Boot 4.1.0、Servlet/MVC 主栈，使用 MyBatis Plus、Sa-Token、Actuator 和多种外部集成；Monitor 模块局部出现 Reactor 类型不代表全仓 WebFlux。

### SPRING-001 Web 与 service 边界

Scope: `framework:spring-boot`, Spring MVC controllers and services

Level: MUST

Source: `repository-fact` + `official-guidance`

Rule: controller 负责 transport、validation、认证上下文和响应映射，不承载核心流程；service 负责用例/事务和领域不变量；mapper/repository 只暴露所需查询，不让上层依赖 ORM 实现细节。使用构造器注入，singleton Bean 不保存请求可变状态。

Verification: controller/service/mapper diff review；slice/unit/integration tests；Maven build。

### SPRING-002 配置与生命周期

Scope: `framework:spring-boot`, configuration and integration modules

Level: MUST

Source: `repository-fact` + `official-guidance`

Rule: 相关配置聚合为类型安全 `@ConfigurationProperties` 并在启动边界验证；profile 表达环境差异但不复制业务逻辑；secret 不进入默认配置。scheduler、listener、client、pool 和 conditional Bean 写明触发、owner、超时、关闭与观测。

Verification: configuration binding/context test；profile diff review；检查 actuator endpoint 暴露和 sensitive config；Maven build。

### SPRING-003 事务与跨系统副作用

Scope: `framework:spring-boot`, persistence and messaging paths

Level: MUST

Source: `repository-fact` + `official-guidance`

Rule: 事务边界、数据源切换和事务事件遵循[数据源事务与建表](../persistence-transactions-and-ddl.md)：新建或实质修改的业务事务使用 dynamic-datasource 的 `@DSTransactional`，关联表、用户、角色、菜单等一致性修改处于覆盖完整用例的事务中。外部 I/O 不在长事务中无界等待；提交后副作用使用与事务实现匹配的 event/outbox/idempotency 语义，不能把 application event 当隐藏的跨进程保证。

Verification: transaction rollback/integration tests；review `@DSTransactional`/`@DS` 的代理边界、propagation、event listener 类型与 phase、外部调用顺序和失败补偿。

### SPRING-004 认证与权限上下文

Scope: Sa-Token, authentication, authorization, menu, role and session services

Level: MUST

Source: `repository-fact` (`docs/upstream/customization-map.md`)

Rule: controller annotation 只是入口检查；service/mapper 的数据访问仍必须携带当前 Client 上下文。所有五种认证策略统一执行登录域准入并构建含 `clientPk` 的 LoginUser；缺上下文失败关闭，不能 userId-only fallback。

Verification: 五种策略与 `IAuthStrategy` review；跨 Client/缺 `clientPk` 负向测试；权限、菜单与会话验收矩阵。

### SPRING-005 错误与可观测性

Scope: `framework:spring-boot`, Web and integration boundaries

Level: SHOULD

Source: `repository-fact` (`GlobalExceptionHandler`, Actuator dependency) + `official-guidance`

Rule: 使用现有全局异常映射形成稳定 HTTP 错误合同，不泄露 stack/SQL/secret；日志、metrics 和 trace 使用可关联字段且不重复记录。health/readiness 反映真实依赖但最小化敏感 endpoint 暴露。

Verification: error mapping test；Actuator/security config review；失败日志抽查；相关 Maven tests/build。
