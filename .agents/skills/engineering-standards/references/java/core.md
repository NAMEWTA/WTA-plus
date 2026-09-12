# Java / Maven 工程规范

适用 `language:java`, `module:wta-vue-plus-namewta`。版本事实：Java 21、Maven Wrapper、多模块 parent/BOM/pluginManagement、JUnit Platform/Surefire；未启用 JPMS、统一 formatter 或静态分析插件。

### JAVA-001 Maven 模块与依赖

Scope: `language:java`, `module:wta-vue-plus-namewta`

Level: MUST

Source: `repository-fact` (root and child POMs)

Rule: 使用 `./mvnw`；版本集中于 root properties、dependencyManagement/BOM 和 pluginManagement，子模块不漂移重复版本。新增依赖使用准确 scope，业务模块不反向污染 common/api，module graph 不形成循环。

Verification: `./mvnw clean package`; review effective dependency direction、POM scope 和版本来源；检查 Wrapper/BOM 无意外变化。

### JAVA-002 Package、可见性与公开 API

Scope: `language:java`

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: package 与 Maven source path 一致并沿用 `org.namewta.<feature>`；使用最小可见性。跨模块 public API 使用明确 DTO/VO/领域类型，不泄露 mapper、持久化 wrapper 或内部可变集合；不要为测试便利扩大可见性。

Verification: compiler/package review；跨模块 import review；API/序列化契约测试。

### JAVA-003 BO/VO/entity 映射

Scope: `path:wta-vue-plus-namewta/wta-modules/**`, `public-api:wta-api`

Level: MUST

Source: `repository-fact` (`SysClientBo`, `SysClientVo`, MapStruct Plus patterns)

Rule: transport BO、response VO、持久化 entity 和跨模块 DTO 按生命周期分离；映射显式可追踪且不能静默丢失安全/Client 字段。Bean Validation 负责 transport 边界，跨字段/领域不变量在 service/validator 处理。

Verification: BO/VO/entity/mapper 对照；mapping 与 validation tests；Maven build。

### JAVA-004 异常、空值与资源

Scope: `language:java`

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: 只在恢复、协议映射或增加边界上下文时 catch，保留 cause；不 catch `Throwable`、不吞 interrupt。外部/ORM 边界明确 null 语义；AutoCloseable 使用 try-with-resources；executor、scheduler、stream、HTTP response 和事务有 owner/关闭路径。

Verification: exception/null/resource path tests；review catch、Optional/nullable、try-with-resources、Bean lifecycle 与 interrupt handling。

### JAVA-005 测试与默认 skip 透明

Scope: `language:java`

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: 纯服务/领域逻辑不默认启动完整 Spring；Web/Data/Security 与真实依赖按风险选择 slice/integration。高风险变更和交付候选运行默认 `./mvnw test`；package 阶段显式 skip 只允许复用同一候选已经取得的独立测试证据。

Verification: 默认 test command；测试资源清理 review；分别报告 test 与 package，不将 package 等同 test passed。core bundle 另用产物清单断言可选模块确实缺席。
