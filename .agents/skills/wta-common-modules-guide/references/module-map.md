# wta-common 子模块地图

条目不够明确时，按路径读取源码，不得凭空推断。路径相对工作区，前缀 `backend/`（磁盘目录亦可能显示为 `WTA-Plus-namewta`）。

## 目录

1. [父模块与 BOM](#父模块与-bom)
2. [如何选依赖](#如何选依赖)
3. [27 个子模块](#27-个子模块)
4. [common 内部依赖分层](#common-内部依赖分层)
5. [消费方显式依赖](#消费方显式依赖)
6. [SPI 接口 vs system 实现](#spi-接口-vs-system-实现)

## 父模块与 BOM

| 项 | 路径 / 事实 |
|---|---|
| 父聚合 | `wta-common/pom.xml`：`artifactId=wta-common`，`packaging=pom`，description「common 通用模块」，`groupId=org.namewta`。自身不是 jar。 |
| 子模块全集 | 同文件 `<modules>` **27** 项（顺序与 POM 一致）：bom、social、core、doc、excel、job、log、notify、mail、mybatis、oss、redis、satoken、security、sms、elasticsearch、web、translation、sensitive、json、encrypt、push、liteflow、mqtt、ai、mcp、openapi。 |
| BOM | `wta-common/wta-common-bom/pom.xml`：`packaging=pom`，description「wta-common-bom common依赖项」。`<dependencyManagement>` 纳入其余 **26** 个 jar（不含 BOM 自身），版本均为 `${revision}`（BOM 内写死 `revision=6.0.0`）。 |
| 根工程 | `pom.xml` `revision=6.0.0`，Java 21，Spring Boot 4.1.0。 |

加 BOM ≠ 自动获得全部 common 能力。业务模块仍须按需显式声明子 artifact。

24 个模块有 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。**excel / oss / bom 没有该文件**。elasticsearch 另有 `wta-common/wta-common-elasticsearch/src/main/resources/META-INF/spring.factories` 注册 `EnvironmentPostProcessor`。

## 如何选依赖

| 需求 | 显式依赖 | 入口 |
|---|---|---|
| 响应体 / 业务异常 / 字符串日期 / Spring bean | `wta-common-core` | `R`、`PageResult`、`ServiceException`、core utils |
| JSON | `wta-common-json` | `JsonUtils` |
| 缓存、分布式锁、限流、防重复提交 | `wta-common-redis` | `RedisUtils`、`CacheUtils`、`@RepeatSubmit`、`@RateLimiter` |
| 登录态（user_type × device） | `wta-common-satoken` | `LoginHelper` |
| Web CRUD（Controller + 异常 + 数据权限 SQL） | `mybatis` + `web` + `security` | `BaseMapperPlus`、`BaseController`、`SecurityConfig` |
| Excel 导入导出 | `wta-common-excel` | `ExcelBuilder`（无 `ExcelUtil`） |
| OSS 文件 | `wta-common-oss` | `OssFactory` / `OssClient` |
| 渠道无关通知、幂等、附件快照 | `wta-common-notify` | `NotifyDispatcher`、`NotifyClient`、`NotifyChannelAdapter` |
| 邮件 | `wta-common-mail` | `MailBuilder`、`MailAccountResolver`（发件账号由 wta-notify 通知配置解析） |
| 推送 | `wta-common-push` | `PushHelper` |
| 机器调用协议、显式开放注册表、签名网关与机器 Session | `wta-common-openapi` | `@OpenApi`、`OpenApiCanonicalizer`、OpenAPI SPI；admin 默认开启，可用 `OPENAPI_ENABLED=false` 显式关闭，业务凭据与授权实现位于 system |
| 字典标签 / 角色菜单权限 / 是否脱敏 | 注入 SPI，不要在 common 找实现 | 见 [SPI](#spi-接口-vs-system-实现) |
| 定时任务 / AI / ES | 对应薄包装模块 | 业务 API 在第三方 starter |

## 27 个子模块

下列「关键公开类型」是业务最常直接引用的入口，不是该模块全部 class。源码根已给出；条目不清时直接读该目录。

### 1. wta-common-bom

- POM：`wta-common/wta-common-bom/pom.xml`，description「wta-common-bom common依赖项」，`packaging=pom`。
- 无 Java。只做 `dependencyManagement`。
- 源码根：`wta-common/wta-common-bom/`

### 2. wta-common-social

- POM：`wta-common/wta-common-social/pom.xml`，description「wta-common-social 授权认证」。显式 common 依赖：`wta-common-redis`。
- 源码根：`wta-common/wta-common-social/src/main/java/org/namewta/common/social/`
- AutoConfiguration.imports：`SocialAutoConfiguration`
- 入口：`config/SocialAutoConfiguration.java`；`utils/SocialUtils.java`（JavaDoc「认证授权工具类」，import `me.zhyd.oauth.*`）；`utils/AuthRedisStateCache.java`；`config/properties/SocialProperties.java`

### 3. wta-common-core

- POM：`wta-common/wta-common-core/pom.xml`，description「wta-common-core 核心模块」。不依赖其他 `wta-common-*`。
- 源码根：`wta-common/wta-common-core/src/main/java/org/namewta/common/core/`
- AutoConfiguration.imports：`config/ApplicationConfig.java`、`config/ThreadPoolConfig.java`、`config/ValidatorConfig.java`、`utils/SpringUtils.java`
- 入口：`domain/R.java`（JavaDoc「响应信息主体」）；`domain/PageResult.java`（「表格分页数据对象」；本仓库无 `TableDataInfo`）；`exception/ServiceException.java`（「通用业务异常，支持使用占位符拼接错误信息。」）；SPI `service/DictService.java`、`service/PermissionService.java`。工具类全集见 [core-utils.md](core-utils.md)。

### 4. wta-common-doc

- POM：`wta-common/wta-common-doc/pom.xml`，description「wta-common-doc 系统接口」。显式 common：`wta-common-core`。
- 源码根：`wta-common/wta-common-doc/src/main/java/org/namewta/common/doc/`
- AutoConfiguration.imports：`config/SpringDocConfig.java`
- 入口：`config/SpringDocConfig.java`、`config/properties/SpringDocProperties.java`（SpringDoc / OpenAPI）

### 5. wta-common-excel

- POM：`wta-common/wta-common-excel/pom.xml`，description 仅为「wta-common-excel」。显式 common：`wta-common-json`。无 AutoConfiguration.imports（库式 API）。
- 源码根：`wta-common/wta-common-excel/src/main/java/org/namewta/common/excel/`
- 入口：`utils/ExcelBuilder.java`（JavaDoc「Excel 导出构造器。」，import `org.apache.fesod.sheet.*`）；`utils/ExcelWriterWrapper.java`；`core/ExcelResult.java`（「excel返回对象」）；`annotation/ExcelDictFormat.java`；`annotation/CellMerge.java`。全仓无 `ExcelUtil`。

### 6. wta-common-job

- POM：`wta-common/wta-common-job/pom.xml`，description「wta-common-job 定时任务」。显式 common：`wta-common-core`。第三方：`com.aizuda:snail-job-client-starter`、`snail-job-client-job-core`。
- 源码根：`wta-common/wta-common-job/src/main/java/org/namewta/common/job/`
- Java 仅 1 类：`config/SnailJobConfig.java`（JavaDoc「启动定时任务」；`@ConditionalOnProperty(prefix = "snail-job", name = "enabled", havingValue = "true")` + `@EnableSnailJob`）。AutoConfiguration.imports 只列该类。业务 API 在 SnailJob starter，本模块未展开。

### 7. wta-common-log

- POM：`wta-common/wta-common-log/pom.xml`，description「wta-common-log 日志记录」。显式 common：`wta-common-satoken`、`wta-common-json`。
- 源码根：`wta-common/wta-common-log/src/main/java/org/namewta/common/log/`
- AutoConfiguration.imports：`aspect/LogAspect.java`
- 入口：`annotation/Log.java`（「自定义操作日志记录注解」）；`aspect/LogAspect.java`；`event/OperLogEvent.java`；`event/LoginInfoEvent.java`；`enums/BusinessType.java`

### 8. wta-common-notify

- POM：`wta-common/wta-common-notify/pom.xml`，description「wta-common-notify 渠道无关通知契约」。显式 common：`wta-common-core`、`wta-common-redis`。
- 源码根：`wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/`
- AutoConfiguration.imports：`config/NotifyAutoConfiguration.java`
- 入口：`core/NotifyDispatcher.java`、`core/NotifyClient.java`、`spi/NotifyChannelAdapter.java`、`model/NotifyRequest.java`；幂等由 `NotifyIdempotencyCoordinator` / `RedisNotifyIdempotencyStore` 承担，附件快照位于 `attachment/**`。

### 9. wta-common-mail

- POM：`wta-common/wta-common-mail/pom.xml`，description「wta-common-mail 邮件模块」。显式 common：`wta-common-core`、`wta-common-notify`。
- 源码根：`wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/`
- AutoConfiguration.imports：`config/MailConfig.java`
- 入口：`config/MailConfig.java`；`core/MailBuilder.java`（「邮件发送构建器。」）；`notify/MailAccountResolver.java`（按 configKey 解析 SMTP，实现位于 wta-notify）。运行时发件账号不从 YAML `mail.from`/`user`/`pass` 读取。

### 10. wta-common-mybatis

- POM：`wta-common/wta-common-mybatis/pom.xml`，description「wta-common-mybatis 数据库服务」。显式 common：`wta-common-core`、`wta-common-satoken`。
- 源码根：`wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/`
- AutoConfiguration.imports：`config/MybatisPlusConfig.java`
- 入口：`core/mapper/BaseMapperPlus.java`（「自定义 Mapper 接口, 实现 自定义扩展」）；`core/domain/BaseEntity.java`；`core/page/PageQuery.java`；`annotation/DataPermission.java`（「数据权限组注解，用于标记数据权限配置数组」）；`helper/DataPermissionHelper.java`；`helper/DataBaseHelper.java`；`utils/IdGeneratorUtil.java`

### 11. wta-common-oss

- POM：`wta-common/wta-common-oss/pom.xml`，description「wta-common-oss oss服务」。显式 common：`wta-common-json`、`wta-common-redis`。无 AutoConfiguration.imports。
- 源码根：`wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/`
- 入口：`factory/OssFactory.java`（「S3存储客户端工厂」）；`client/OssClient.java`（「S3 存储客户端接口。」）；`properties/OssProperties.java`；`util/BucketUrlUtil.java`（「桶链接工具类」）。配置对象不是 Spring `@Configuration`。

### 12. wta-common-redis

- POM：`wta-common/wta-common-redis/pom.xml`，description「wta-common-redis 缓存服务」。显式 common：`wta-common-core`、`wta-common-json`。
- 源码根：`wta-common/wta-common-redis/src/main/java/org/namewta/common/redis/`
- AutoConfiguration.imports：`config/RedisConfig.java`、`config/CacheConfig.java`、`config/IdempotentConfig.java`、`config/RateLimiterConfig.java`、`config/Lock4jConfig.java`
- 入口：`utils/RedisUtils.java`（「redis 工具类」）；`utils/CacheUtils.java`；`utils/QueueUtils.java`；`utils/SequenceUtils.java`；`annotation/RepeatSubmit.java`（「自定义注解防止表单重复提交」）；`annotation/RateLimiter.java`（「限流注解」）

### 13. wta-common-satoken

- POM：`wta-common/wta-common-satoken/pom.xml`，description「wta-common-satoken 权限认证」。显式 common：`wta-common-core`、`wta-common-redis`。
- 源码根：`wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/`
- AutoConfiguration.imports：`config/SaTokenConfig.java`
- 入口：`utils/LoginHelper.java`（「登录鉴权助手」；`user_type` × `device` 多用户体系）；`config/SaTokenConfig.java`；`core/service/SaPermissionImpl.java`；`core/dao/PlusSaTokenDao.java`

### 14. wta-common-security

- POM：`wta-common/wta-common-security/pom.xml`，description「wta-common-security 安全模块」。显式 common：`wta-common-satoken`。
- 源码根：`wta-common/wta-common-security/src/main/java/org/namewta/common/security/`
- AutoConfiguration.imports：`handler/AllUrlHandler.java`、`config/SecurityConfig.java`
- 入口：`config/SecurityConfig.java`（Sa-Token 过滤器/拦截器：`SaServletFilter`、`SaInterceptor`）；`config/properties/SecurityProperties.java`；`handler/AllUrlHandler.java`

### 15. wta-common-sms

- POM：`wta-common/wta-common-sms/pom.xml`，description「wta-common-sms 短信模块」。显式 common：`wta-common-redis`、`wta-common-notify`。
- 源码根：`wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/`
- AutoConfiguration.imports：`config/SmsAutoConfiguration.java`（JavaDoc「短信配置类」，import `org.dromara.sms4j`）
- 入口：`config/SmsAutoConfiguration.java`；`core/dao/PlusSmsDao.java`；`handler/SmsExceptionHandler.java`；`notify/SmsNotifyChannelAdapter.java`。厂商账号由 wta-notify 通知配置注册 SMS4J blend，YAML 不再保存 `sms.blends` 与全局拦截。

### 16. wta-common-elasticsearch

- POM：`wta-common/wta-common-elasticsearch/pom.xml`，description「wta-common-elasticsearch ES搜索引擎服务」。**不依赖**任何其他 `wta-common-*`。第三方：`org.dromara.easy-es:easy-es-boot-starter`。
- 源码根：`wta-common/wta-common-elasticsearch/src/main/java/org/namewta/common/elasticsearch/`
- Java 仅 2 类：`config/EasyEsConfiguration.java`（「easy-es 配置」；`@ConditionalOnProperty(value = "easy-es.enable", havingValue = "true")` + `@EsMapperScan("org.namewta.**.esmapper")`）；`config/ActuatorEnvironmentPostProcessor.java`（「健康检查配置注入」，经 `src/main/resources/META-INF/spring.factories` 注册为 `EnvironmentPostProcessor`）。AutoConfiguration.imports 只列 `EasyEsConfiguration`。Easy-Es 与本工程数据源对齐未在这 2 个类中展开。

### 17. wta-common-web

- POM：`wta-common/wta-common-web/pom.xml`，description「wta-common-web web服务」。显式 common：`wta-common-json`。
- 源码根：`wta-common/wta-common-web/src/main/java/org/namewta/common/web/`
- AutoConfiguration.imports：`config/CaptchaConfig.java`、`config/FilterConfig.java`、`config/I18nConfig.java`、`config/ResourcesConfig.java`
- 入口：`core/BaseController.java`（「web层通用数据处理」）；`handler/GlobalExceptionHandler.java`；以及上列 4 个配置类。

### 18. wta-common-translation

- POM：`wta-common/wta-common-translation/pom.xml`，description「wta-common-translation 通用翻译功能」。显式 common：`wta-common-json`。
- 源码根：`wta-common/wta-common-translation/src/main/java/org/namewta/common/translation/`
- AutoConfiguration.imports：`config/TranslationConfig.java` 以及 `core/impl/` 下 `UserNameTranslationImpl`、`NicknameTranslationImpl`、`DeptNameTranslationImpl`、`DictTypeTranslationImpl`、`OssUrlTranslationImpl`
- 入口：`annotation/Translation.java`（「通用翻译注解」）；`core/TranslationInterface.java`；`config/TranslationConfig.java`

### 19. wta-common-sensitive

- POM：`wta-common/wta-common-sensitive/pom.xml`，description「wta-common-sensitive 脱敏模块」。显式 common：`wta-common-json`。
- 源码根：`wta-common/wta-common-sensitive/src/main/java/org/namewta/common/sensitive/`
- AutoConfiguration.imports：`config/SensitiveConfig.java`
- 入口：`annotation/Sensitive.java`（「数据脱敏注解」）；`core/SensitiveService.java`（「脱敏服务」SPI，实现不在本模块）；`core/SensitiveStrategy.java`；`config/SensitiveConfig.java`

### 20. wta-common-json

- POM：`wta-common/wta-common-json/pom.xml`，description「wta-common-json 序列化模块」。显式 common：`wta-common-core`。
- 源码根：`wta-common/wta-common-json/src/main/java/org/namewta/common/json/`
- AutoConfiguration.imports：`config/JacksonConfig.java`、`config/JsonEnhancementConfig.java`
- 入口：`utils/JsonUtils.java`（「JSON 工具类」）；上列配置；`validate/JsonPattern.java`

### 21. wta-common-encrypt

- POM：`wta-common/wta-common-encrypt/pom.xml`，description「wta-common-encrypt 数据加解密模块」。显式 common：`wta-common-core`。
- 源码根：`wta-common/wta-common-encrypt/src/main/java/org/namewta/common/encrypt/`
- AutoConfiguration.imports：`config/EncryptorAutoConfiguration.java`、`config/ApiDecryptAutoConfiguration.java`
- 入口：`utils/EncryptUtils.java`（「安全相关工具类」）；`annotation/EncryptField.java`；`annotation/ApiEncrypt.java`；`core/IEncryptor.java`

### 22. wta-common-push

- POM：`wta-common/wta-common-push/pom.xml`，description「wta-common-push 消息推送模块」。显式 common：`wta-common-core`、`wta-common-redis`、`wta-common-satoken`、`wta-common-json`。
- 源码根：`wta-common/wta-common-push/src/main/java/org/namewta/common/push/`
- AutoConfiguration.imports：`config/MessageAutoConfiguration.java`、`config/MessageSseConfiguration.java`、`config/MessageWebSocketConfiguration.java`
- 入口：`helper/PushHelper.java`（「统一消息推送工具。」）；`dto/PushDTO.java`；`controller/SseController.java`

### 23. wta-common-liteflow

- POM：`wta-common/wta-common-liteflow/pom.xml`，description「wta-common-liteflow LiteFlow规则编排模块」。显式 common：`wta-common-core`。
- 源码根：`wta-common/wta-common-liteflow/src/main/java/org/namewta/common/liteflow/`
- AutoConfiguration.imports：`config/LiteFlowAutoConfiguration.java`
- 入口：`utils/LiteFlowUtils.java`（「LiteFlow 执行工具。」）；`config/LiteFlowAutoConfiguration.java`；内置 `component/FailComponent.java`、`NoopComponent.java`、`AlwaysTrueComponent.java`、`AlwaysFalseComponent.java`、`ContextRequiredComponent.java`

### 24. wta-common-mqtt

- POM：`wta-common/wta-common-mqtt/pom.xml`，description「wta-common-mqtt mqtt模块」。显式 common：`wta-common-core`、`wta-common-json`。
- 源码根：`wta-common/wta-common-mqtt/src/main/java/org/namewta/common/mqtt/`
- AutoConfiguration.imports：`config/MqttAutoConfiguration.java`
- 入口：`config/MqttAutoConfiguration.java`；`listener/MqttClientConnectListener.java`；`listener/MqttClientGlobalMessageListener.java`

### 25. wta-common-ai

- POM：`wta-common/wta-common-ai/pom.xml`，description「wta-common-ai AI公共模块」。显式 common：`wta-common-core`。第三方：`com.aizuda:snail-ai-agent-chat-starter`、`snail-ai-agent-executor-starter`、`snail-ai-openapi-starter`。
- 源码根：`wta-common/wta-common-ai/src/main/java/org/namewta/common/ai/`
- Java 仅 1 类：`config/SnailAiConfig.java`（JavaDoc「Snail AI 自动配置」；`@ConditionalOnProperty(prefix = "snail-ai", name = "enabled", havingValue = "true")` + `@EnableSnailAiAgent` + `@EnableSnailAiOpenApi`）。AutoConfiguration.imports 只列该类。业务 API 在 SnailAi starter，本模块未展开。

### 26. wta-common-mcp

- POM：`wta-common/wta-common-mcp/pom.xml`，description「wta-common-mcp mcp模块」。显式 common：`wta-common-core`。
- 源码根：`wta-common/wta-common-mcp/src/main/java/org/namewta/common/mcp/`
- AutoConfiguration.imports：`config/McpAutoConfiguration.java`
- 入口：`config/McpAutoConfiguration.java`；`core/McpClientTemplate.java`（「MCP Client 通用操作模板。」）；`core/McpToolCallResult.java`；`core/McpResourceReadResult.java`

### 27. wta-common-openapi

- POM：`wta-common/wta-common-openapi/pom.xml`，description「wta-common-openapi machine invocation runtime」。显式 common：`wta-common-core`、`wta-common-redis`、`wta-common-satoken`、`wta-common-doc`。
- 源码根：`wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/`
- AutoConfiguration.imports：`config/OpenApiAutoConfiguration.java`；由 `openapi.enabled=true` 条件启用，配置或必要 SPI 无效时失败关闭。
- 入口：`annotation/OpenApi.java`；`protocol/OpenApiCanonicalizer.java`、`OpenApiSigner.java`；`registry/OpenApiOperationRegistry.java`；`session/OpenApiMachineSessionBridge.java`、`OpenApiMachineSessionInvalidator.java`；`spi/OpenApiAuthorizationResolver.java`、`OpenApiCredentialResolver.java`、`OpenApiCallEventPublisher.java`。

## common 内部依赖分层

来源：各子模块 `wta-common/*/pom.xml` 中显式 `<artifactId>wta-common-*</artifactId>`。第三方 starter 传递树未展开。

| 模块 | 显式 `wta-common-*` |
|---|---|
| core | 无 |
| elasticsearch | 无 |
| json / doc / encrypt / job / liteflow / ai / mcp | core |
| redis | core + json |
| notify | core + redis |
| mail | core + notify |
| satoken | core + redis |
| security | satoken |
| log | satoken + json |
| web / excel / translation / sensitive | json |
| mybatis | core + satoken |
| oss | json + redis |
| push | core + redis + satoken + json |
| mqtt | core + json |
| sms | redis + notify |
| social | redis |
| openapi | core + redis + satoken + doc |

新业务若只需工具类，依赖 `wta-common-core`（及需要的 json/redis）即可；Web CRUD 通常再加 mybatis + web + security。

## 消费方显式依赖

均为 `groupId=org.namewta`，无版本号，走 BOM。satoken / redis / json 等常通过 web/security/mybatis 传递进入，不一定出现在业务 POM。未跑 `mvn dependency:tree`。

### wta-system（15 个 common）

路径：`wta-modules/wta-system/pom.xml`

`wta-common-core`、`wta-common-doc`、`wta-common-mybatis`、`wta-common-translation`、`wta-common-oss`、`wta-common-notify`、`wta-common-log`、`wta-common-excel`、`wta-common-sms`、`wta-common-security`、`wta-common-web`、`wta-common-sensitive`、`wta-common-encrypt`、`wta-common-push`、`wta-common-openapi`。另有 `wta-api`（非 common）。

未显式依赖 satoken / redis / json / mail / social / job / ai / mcp / mqtt / elasticsearch / liteflow。

### wta-workflow（10 个 common）

路径：`wta-modules/wta-workflow/pom.xml`

`wta-common-push`、`wta-common-doc`、`wta-common-notify`、`wta-common-mybatis`、`wta-common-web`、`wta-common-log`、`wta-common-excel`、`wta-common-translation`、`wta-common-security`、`wta-common-liteflow`。另有 `wta-api` 与 Warm-Flow 引擎。

未显式依赖 `wta-common-core` / oss / mail / sms / sensitive / encrypt。相对 system 多了 liteflow，少了 core/oss/sms/sensitive/encrypt；邮件/短信能力通过统一通知 adapter 解耦，不再由 workflow 直接依赖。

### wta-admin（6 个直接 common + 业务模块并集）

路径：`wta-admin/pom.xml`

直接：`wta-common-doc`、`wta-common-social`、`wta-common-mail`、`wta-common-notify`、`wta-common-mcp`、`wta-common-openapi`。

业务模块由 profile 组装：默认 `bundle-full` 包含 `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`；显式 `bundle-core` 只保留 `wta-api`、`wta-system` 与直接 common 依赖。运行时代码生成器已从 reactor 和两个 bundle 删除。

因此 `bundle-full` admin 进程还会装入（POM 图推断）：system 的 15 个、workflow 的 10 个、job 的 `wta-common-json`+`wta-common-job`（`wta-modules/wta-job/pom.xml`）、ai 的 `wta-common-core`+`wta-common-ai`+`wta-common-satoken`+`wta-common-web`（`wta-modules/wta-ai/pom.xml`）、demo 额外的 `wta-common-redis`+`wta-common-elasticsearch`+`wta-common-mqtt`+`wta-common-mcp` 等（`wta-modules/wta-demo/pom.xml`）。openapi / social / mcp 由 admin 直接引入；elasticsearch / mqtt 主要由 demo 引入。需要 ES/MQTT 时看 demo POM，不要写进 system/workflow POM。

## SPI 接口 vs system 实现

下列跨边界 SPI 在 common 声明合同；业务实现位于 `wta-system`，可选扩展点可由组装应用提供：

| SPI | 接口路径 | system 实现 |
|---|---|---|
| `DictService` | `wta-common/wta-common-core/src/main/java/org/namewta/common/core/service/DictService.java`（「通用 字典服务」；`getDictLabel` / `getDictValue` / `getAllDictByDictType` / `getDictType` / `getDictData`） | `wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysDictTypeServiceImpl.java`（`implements ISysDictTypeService, DictService`） |
| `PermissionService` | `wta-common/wta-common-core/src/main/java/org/namewta/common/core/service/PermissionService.java`（「用户权限处理」；`getRolePermission` / `getMenuPermission`） | `.../service/impl/SysPermissionServiceImpl.java`（`implements ISysPermissionService, PermissionService`） |
| `SensitiveService` | `wta-common/wta-common-sensitive/src/main/java/org/namewta/common/sensitive/core/SensitiveService.java`（「脱敏服务」；`isSensitive(roleKey[], perms[])`） | `.../service/impl/SysSensitiveServiceImpl.java` |
| `OpenApiAuthorizationResolver` | `wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/spi/OpenApiAuthorizationResolver.java` | `.../openapi/authorization/SystemOpenApiAuthorizationResolver.java` |
| `OpenApiCredentialResolver` | `wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/spi/OpenApiCredentialResolver.java` | `.../openapi/credential/service/SystemOpenApiCredentialResolver.java` |
| `OpenApiCallEventPublisher` | `wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/spi/OpenApiCallEventPublisher.java` | 组装层可覆盖；common auto-configuration 提供 no-op 缺省实现 |

调用约定与方法语义见统一的 [wta-module-guide](../../wta-module-guide/SKILL.md) system reference，不要在本 Skill 复制。
