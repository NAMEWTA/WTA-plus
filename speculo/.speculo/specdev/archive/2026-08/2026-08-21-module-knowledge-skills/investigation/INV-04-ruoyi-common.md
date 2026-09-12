---
artifact: wayfinder-ticket
id: INV-04
name: ruoyi-common 子模块与工具类
parent_map: <Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/wayfinder-map.md</Path>
label: wayfinder:research
status: closed
blocked_by: []
resolution: answered
---

# ruoyi-common 子模块与工具类

## 问题

ruoyi-common 各子模块职责是什么，有哪些工具类？

## Research: ruoyi-common 子模块与工具类地图

- Decision / target: 产出 `ruoyi-vue-plus-namewta/ruoyi-common` 的证据化地图：全部子模块、职责、关键公开入口类型（尤其 `ruoyi-common-core` 工具类），供后续 Skill 使用。本文件为唯一 owning artifact。本次不创建 Skill、不改 plan、不改 status。
- Scope / version: 后端子聚合 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/</Path>`（磁盘目录亦可能显示为 `RuoYi-Vue-Plus-namewta`，大小写不敏感文件系统下为同一路径）。父 POM `artifactId=ruoyi-common`，`packaging=pom`，description「common 通用模块」。根工程 `<Path>ruoyi-vue-plus-namewta/pom.xml</Path>` `revision=6.0.0`，Java 21，Spring Boot 4.1.0。源码核对日期 2026-08-21。不含业务模块实现、不含创建 Skill、未执行 `mvn dependency:tree`。
- Stop condition: 父 POM 全部 `<module>` 均有 artifactId / 一句话职责 / 关键入口类型 / 源码路径；`org.dromara.common.core.utils` 与其他模块 notable `*Utils`/`*Helper`/`*Util` 按包列出；system / workflow / admin 的典型 Maven 依赖已记录；未知项单列。

### R-001

- Claim: 父模块 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>` 声明 **25** 个子模块（顺序与 POM `<modules>` 一致）：`ruoyi-common-bom`、`ruoyi-common-social`、`ruoyi-common-core`、`ruoyi-common-doc`、`ruoyi-common-excel`、`ruoyi-common-job`、`ruoyi-common-log`、`ruoyi-common-mail`、`ruoyi-common-mybatis`、`ruoyi-common-oss`、`ruoyi-common-redis`、`ruoyi-common-satoken`、`ruoyi-common-security`、`ruoyi-common-sms`、`ruoyi-common-elasticsearch`、`ruoyi-common-web`、`ruoyi-common-translation`、`ruoyi-common-sensitive`、`ruoyi-common-json`、`ruoyi-common-encrypt`、`ruoyi-common-push`、`ruoyi-common-liteflow`、`ruoyi-common-mqtt`、`ruoyi-common-ai`、`ruoyi-common-mcp`。父模块自身 `packaging=pom`，description 为「common 通用模块」。groupId 均为 `org.dromara`。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-common/pom.xml</Path>` `<modules>` 与 `<description>`
- Confidence: high
- Limits: 子模块职责与入口类型见 R-003；BOM 覆盖范围见 R-002。
- Artifact impact: Skill 模块表必须以这 25 个 artifactId 为全集，不得遗漏 bom / ai / mcp / mqtt / liteflow。

### R-002

- Claim: `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>` `packaging=pom`，description「ruoyi-common-bom common依赖项」。其 `<dependencyManagement>` 纳入其余 **24** 个 jar 子模块（不含 BOM 自身）：core、doc、excel、job、log、mail、mybatis、oss、redis、satoken、security、sms、elasticsearch、social、web、translation、sensitive、json、encrypt、push、liteflow、mqtt、ai、mcp。版本均为 `${revision}`（BOM 内写死 `revision=6.0.0`）。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/pom.xml</Path>`
- Confidence: high
- Limits: BOM 只锁版本，不把 24 个模块传递进业务 classpath；业务模块仍须按需显式依赖。
- Artifact impact: Skill 应说明「加 BOM ≠ 自动获得全部 common 能力」；业务 POM 按需声明子 artifact。

### R-003

- Claim: 25 个子模块的职责、关键公开类型与源码根如下（职责优先取各子 POM `<description>`，公开类型取该模块 Java 源与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）。
- Type: code fact
- Source: 各子模块 `pom.xml`、`src/main/java/org/dromara/common/**`、以及 22 个模块的 AutoConfiguration.imports（excel / oss / bom 无该文件）
- Confidence: high
- Limits: 「关键公开类型」是业务最常直接引用的入口，不是该模块全部 class 枚举。excel / oss 无 Spring Boot 自动配置清单，属库式 API。
- Artifact impact: Skill 按 artifactId 路由；调用方依赖对应子模块后使用下表入口，不要依赖父 `ruoyi-common`（它是 pom 聚合，不是 jar）。

| # | artifactId | POM 职责 | 关键公开类型 | 源码根 |
|---|---|---|---|---|
| 1 | ruoyi-common-bom | common依赖项 | 无 Java；仅 `dependencyManagement` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-bom/</Path>` |
| 2 | ruoyi-common-social | 授权认证 | `SocialAutoConfiguration`、`SocialUtils`、`AuthRedisStateCache`、`SocialProperties` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-social/src/main/java/org/dromara/common/social/</Path>` |
| 3 | ruoyi-common-core | 核心模块 | `R`、`PageResult`、`ServiceException`、`DictService`、`PermissionService`、`SpringUtils` 及 R-004 全部 utils；自动配置 `ApplicationConfig`、`ThreadPoolConfig`、`ValidatorConfig`、`SpringUtils` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/</Path>` |
| 4 | ruoyi-common-doc | 系统接口 | `SpringDocConfig`、`SpringDocProperties`（SpringDoc / OpenAPI） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-doc/src/main/java/org/dromara/common/doc/</Path>` |
| 5 | ruoyi-common-excel | ruoyi-common-excel | `ExcelBuilder`、`ExcelWriterWrapper`、`ExcelResult`、`@ExcelDictFormat`、`@CellMerge`（无 AutoConfiguration.imports） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-excel/src/main/java/org/dromara/common/excel/</Path>` |
| 6 | ruoyi-common-job | 定时任务 | `SnailJobConfig`（SnailJob 自动配置，模块仅 1 个 Java 类） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-job/src/main/java/org/dromara/common/job/</Path>` |
| 7 | ruoyi-common-log | 日志记录 | `@Log`、`LogAspect`、`OperLogEvent`、`LoginInfoEvent`、`BusinessType` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-log/src/main/java/org/dromara/common/log/</Path>` |
| 8 | ruoyi-common-mail | 邮件模块 | `MailConfig`、`MailBuilder`、`MailProperties` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail/src/main/java/org/dromara/common/mail/</Path>` |
| 9 | ruoyi-common-mybatis | 数据库服务 | `BaseMapperPlus`、`BaseEntity`、`PageQuery`、`@DataPermission`、`DataPermissionHelper`、`DataBaseHelper`、`IdGeneratorUtil`、`MybatisPlusConfig` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mybatis/src/main/java/org/dromara/common/mybatis/</Path>` |
| 10 | ruoyi-common-oss | oss服务 | `OssFactory`、`OssClient`、`OssProperties`、`BucketUrlUtil`（无 AutoConfiguration.imports；配置对象非 Spring `@Configuration`） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/</Path>` |
| 11 | ruoyi-common-redis | 缓存服务 | `RedisUtils`、`CacheUtils`、`QueueUtils`、`SequenceUtils`、`@RepeatSubmit`、`@RateLimiter`；自动配置 `RedisConfig`、`CacheConfig`、`IdempotentConfig`、`RateLimiterConfig`、`Lock4jConfig` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/</Path>` |
| 12 | ruoyi-common-satoken | 权限认证 | `LoginHelper`、`SaTokenConfig`、`SaPermissionImpl`、`PlusSaTokenDao` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/</Path>` |
| 13 | ruoyi-common-security | 安全模块 | `SecurityConfig`（Sa-Token 过滤器/拦截器）、`SecurityProperties`、`AllUrlHandler` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/</Path>` |
| 14 | ruoyi-common-sms | 短信模块 | `SmsAutoConfiguration`、`PlusSmsDao`、`SmsExceptionHandler`（封装 sms4j） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms/src/main/java/org/dromara/common/sms/</Path>` |
| 15 | ruoyi-common-elasticsearch | ES搜索引擎服务 | `EasyEsConfiguration`、`ActuatorEnvironmentPostProcessor`（本模块 POM 不依赖其他 `ruoyi-common-*`） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-elasticsearch/src/main/java/org/dromara/common/elasticsearch/</Path>` |
| 16 | ruoyi-common-web | web服务 | `BaseController`、`GlobalExceptionHandler`、`CaptchaConfig`、`FilterConfig`、`I18nConfig`、`ResourcesConfig` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-web/src/main/java/org/dromara/common/web/</Path>` |
| 17 | ruoyi-common-translation | 通用翻译功能 | `@Translation`、`TranslationInterface`、`TranslationConfig`；内置 `UserName`/`Nickname`/`DeptName`/`DictType`/`OssUrl` TranslationImpl | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-translation/src/main/java/org/dromara/common/translation/</Path>` |
| 18 | ruoyi-common-sensitive | 脱敏模块 | `@Sensitive`、`SensitiveService`、`SensitiveStrategy`、`SensitiveConfig` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sensitive/src/main/java/org/dromara/common/sensitive/</Path>` |
| 19 | ruoyi-common-json | 序列化模块 | `JsonUtils`、`JacksonConfig`、`JsonEnhancementConfig`、`@JsonPattern` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-json/src/main/java/org/dromara/common/json/</Path>` |
| 20 | ruoyi-common-encrypt | 数据加解密模块 | `EncryptUtils`、`@EncryptField`、`@ApiEncrypt`、`EncryptorAutoConfiguration`、`ApiDecryptAutoConfiguration`、`IEncryptor` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-encrypt/src/main/java/org/dromara/common/encrypt/</Path>` |
| 21 | ruoyi-common-push | 消息推送模块 | `PushHelper`、`PushDTO`、`MessageAutoConfiguration`、`MessageSseConfiguration`、`MessageWebSocketConfiguration`、`SseController` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-push/src/main/java/org/dromara/common/push/</Path>` |
| 22 | ruoyi-common-liteflow | LiteFlow规则编排模块 | `LiteFlowUtils`、`LiteFlowAutoConfiguration`、内置 `Fail`/`Noop`/`AlwaysTrue`/`AlwaysFalse`/`ContextRequired` Component | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-liteflow/src/main/java/org/dromara/common/liteflow/</Path>` |
| 23 | ruoyi-common-mqtt | mqtt模块 | `MqttAutoConfiguration`、`MqttClientConnectListener`、`MqttClientGlobalMessageListener` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mqtt/src/main/java/org/dromara/common/mqtt/</Path>` |
| 24 | ruoyi-common-ai | AI公共模块 | `SnailAiConfig`（模块仅 1 个 Java 类） | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-ai/src/main/java/org/dromara/common/ai/</Path>` |
| 25 | ruoyi-common-mcp | mcp模块 | `McpAutoConfiguration`、`McpClientTemplate`、`McpToolCallResult`、`McpResourceReadResult` | `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mcp/src/main/java/org/dromara/common/mcp/</Path>` |

core 另含跨模块 SPI 与领域基元（同属本 Claim）：

- `org.dromara.common.core.domain.R`：HTTP/服务响应主体（code/msg/data）
- `org.dromara.common.core.domain.PageResult`：表格分页数据对象（旧名 TableDataInfo 在本仓库不存在）
- `org.dromara.common.core.exception.ServiceException`：通用业务异常
- `org.dromara.common.core.service.DictService`：通用字典服务接口（实现不在 common）
- `org.dromara.common.core.service.PermissionService`：用户角色/菜单权限接口（实现不在 common）
- `org.dromara.common.sensitive.core.SensitiveService`：是否脱敏 SPI（实现不在 common）

### R-004

- Claim: `org.dromara.common.core.utils` 及其子包的工具类全集如下（20 个 `*Utils` + 1 个 `SqlUtil`）。多数继承 Hutool / Commons Lang，项目约定优先用这些 FQN 而非直接调 Hutool。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/utils/</Path>` 各类 JavaDoc 首句
- Confidence: high
- Limits: 未逐方法展开 API；`RegexValidator` 不是 Utils，未列入。
- Artifact impact: Skill 的 core 工具表以此清单为权威。

**`org.dromara.common.core.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.core.utils.DateUtils` | 时间工具类（extends Hutool `DateUtil`） |
| `org.dromara.common.core.utils.SpringUtils` | spring 工具类（extends Hutool `SpringUtil`；已注册为自动配置） |
| `org.dromara.common.core.utils.TreeBuildUtils` | 扩展 Hutool `TreeUtil`，封装系统树构建 |
| `org.dromara.common.core.utils.StringUtils` | 字符串工具类（extends Apache Commons `StringUtils`） |
| `org.dromara.common.core.utils.MapstructUtils` | Mapstruct-Plus 对象转换 |
| `org.dromara.common.core.utils.ServletUtils` | 客户端/请求响应工具（extends Hutool `JakartaServletUtil`） |
| `org.dromara.common.core.utils.StreamUtils` | stream 流工具类 |
| `org.dromara.common.core.utils.ValidatorUtils` | Jakarta Validator 校验框架工具 |
| `org.dromara.common.core.utils.ThreadUtils` | 线程工具 |
| `org.dromara.common.core.utils.DesensitizedUtils` | 脱敏工具类（extends Hutool `DesensitizedUtil`） |
| `org.dromara.common.core.utils.MessageUtils` | 获取 i18n 资源文件 |
| `org.dromara.common.core.utils.ObjectUtils` | 对象工具类（extends Hutool `ObjectUtil`） |
| `org.dromara.common.core.utils.NetUtils` | 增强网络相关工具类（extends Hutool `NetUtil`） |

**子包**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.core.utils.reflect.ReflectUtils` | 反射：getter/setter、私有访问、泛型 Class、AOP 真实类 |
| `org.dromara.common.core.utils.reflect.AnnotationUtils` | 注解工具类（extends Hutool `AnnotationUtil`） |
| `org.dromara.common.core.utils.ip.AddressUtils` | 获取地址类 |
| `org.dromara.common.core.utils.ip.RegionUtils` | IP 地址行政区域（ip2region xdb） |
| `org.dromara.common.core.utils.regex.RegexUtils` | 正则相关工具类（extends Hutool `ReUtil`） |
| `org.dromara.common.core.utils.file.FileUtils` | 文件处理工具类（extends Hutool `FileUtil`） |
| `org.dromara.common.core.utils.file.MimeTypeUtils` | 媒体类型工具类 |
| `org.dromara.common.core.utils.sql.SqlUtil` | sql 操作/关键字过滤工具类 |

core 同层相关工厂（非 Utils，但是公开入口）：`org.dromara.common.core.factory.YmlPropertySourceFactory`、`org.dromara.common.core.factory.RegexPatternPoolFactory`。

### R-005

- Claim: 其他 common 子模块 notable `*Utils` / `*Util` / `*Helper` 共 15 个，按包分组如下。excel 导出入口是 `ExcelBuilder`/`ExcelWriterWrapper`，**没有**名为 `ExcelUtil` 的类。
- Type: code fact
- Source: 全 `ruoyi-common` glob `*Utils.java`（29）+ `*Helper.java`（4）+ `*Util.java`（3）；各类 JavaDoc 首句
- Confidence: high
- Limits: `ExcelBuilder`/`ExcelWriterWrapper` 不是 Utils/Helper 命名，但因替代旧 `ExcelUtil` 而列入本 Claim。未把每个模块的全部非工具类再列一遍（见 R-003）。
- Artifact impact: Skill 应按模块引用这些 FQN；不要教调用方去找不存在的 `ExcelUtil`。

**`org.dromara.common.redis.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.redis.utils.RedisUtils` | redis 工具类 |
| `org.dromara.common.redis.utils.CacheUtils` | 缓存操作工具类 |
| `org.dromara.common.redis.utils.QueueUtils` | 分布式队列工具（轻量；重量级请用 MQ；要求 Redis 5.x+） |
| `org.dromara.common.redis.utils.SequenceUtils` | 发号器工具类 |

**`org.dromara.common.json.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.json.utils.JsonUtils` | JSON 工具类 |

**`org.dromara.common.encrypt.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.encrypt.utils.EncryptUtils` | 安全相关加解密工具类 |

**`org.dromara.common.social.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.social.utils.SocialUtils` | 认证授权（JustAuth）工具类 |

**`org.dromara.common.mybatis.utils` / `...helper` / `...query`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.mybatis.utils.IdGeneratorUtil` | ID 生成工具类（委托 MyBatis-Plus `IdentifierGenerator`/`IdWorker`） |
| `org.dromara.common.mybatis.helper.DataPermissionHelper` | 数据权限助手 |
| `org.dromara.common.mybatis.helper.DataBaseHelper` | 数据库助手 |
| `org.dromara.common.mybatis.core.query.AggregateSelectUtils` | 聚合查询字段 SQL 构造工具 |

**`org.dromara.common.satoken.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.satoken.utils.LoginHelper` | 登录鉴权助手（user_type × device 多用户体系） |

**`org.dromara.common.push.helper`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.push.helper.PushHelper` | 统一消息推送工具 |

**`org.dromara.common.liteflow.utils`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.liteflow.utils.LiteFlowUtils` | LiteFlow 执行工具 |

**`org.dromara.common.oss.util`**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.oss.util.BucketUrlUtil` | 桶链接（path/virtual-host 风格 URL）工具类 |

**excel 等价入口（非 Utils 命名）**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.excel.utils.ExcelBuilder` | Excel 导出构造器（Fesod） |
| `org.dromara.common.excel.utils.ExcelWriterWrapper` | ExcelWriter 写出包装器，避免直接关闭 IO |

**mail 等价入口**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.mail.core.MailBuilder` | 邮件发送构建器 |

**oss 等价入口**

| FQN | 一句话职责 |
|---|---|
| `org.dromara.common.oss.factory.OssFactory` | S3 存储客户端工厂 |
| `org.dromara.common.oss.client.OssClient` | OSS 客户端接口 |

### R-006

- Claim: 三个业务消费方对 common 的**显式** Maven 依赖如下（均为 `groupId=org.dromara`，无版本号，走 BOM）。satoken / redis / json 等常通过 web/security/mybatis 传递进入，不一定出现在业务 POM。
- Type: code fact
- Source: `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/pom.xml</Path>`；`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>`
- Confidence: high
- Limits: 未跑 `mvn dependency:tree`，传递闭包以 POM 图推断。admin 还会通过 `ruoyi-system` / `ruoyi-workflow` / `ruoyi-job` / `ruoyi-ai` / `ruoyi-demo`（及默认激活的 `ruoyi-gen`）把更多 common 装进同一 JVM。
- Artifact impact: Skill 写「某业务模块可用哪些 common」时，先看该模块 POM 显式依赖，再说明传递；不要假设 workflow POM 里有 `ruoyi-common-core`（它没有，但 mybatis/web/security 会带进来）。

**ruoyi-system（13 个 common）**

`ruoyi-common-core`、`ruoyi-common-doc`、`ruoyi-common-mybatis`、`ruoyi-common-translation`、`ruoyi-common-oss`、`ruoyi-common-log`、`ruoyi-common-excel`、`ruoyi-common-sms`、`ruoyi-common-security`、`ruoyi-common-web`、`ruoyi-common-sensitive`、`ruoyi-common-encrypt`、`ruoyi-common-push`。

另有 `ruoyi-api`（非 common）。未显式依赖 satoken / redis / json / mail / social / job / ai / mcp / mqtt / elasticsearch / liteflow。

**ruoyi-workflow（11 个 common）**

`ruoyi-common-push`、`ruoyi-common-doc`、`ruoyi-common-mail`、`ruoyi-common-sms`、`ruoyi-common-mybatis`、`ruoyi-common-web`、`ruoyi-common-log`、`ruoyi-common-excel`、`ruoyi-common-translation`、`ruoyi-common-security`、`ruoyi-common-liteflow`。

另有 `ruoyi-api` 与 Warm-Flow 引擎。未显式依赖 `ruoyi-common-core` / oss / sensitive / encrypt。相对 system 多了 mail 与 liteflow，少了 core/oss/sensitive/encrypt。

**ruoyi-admin（4 个直接 common + 业务模块并集）**

直接：`ruoyi-common-doc`、`ruoyi-common-social`、`ruoyi-common-mail`、`ruoyi-common-mcp`。

业务模块：`ruoyi-api`、`ruoyi-system`、`ruoyi-job`、`ruoyi-ai`、`ruoyi-demo`、`ruoyi-workflow`；profile `gen` 默认激活再加 `ruoyi-gen`。

因此 admin 进程还会装入：system 的 13 个、workflow 的 11 个、job 的 `ruoyi-common-json`+`ruoyi-common-job`、ai 的 `ruoyi-common-core`+`ruoyi-common-ai`+`ruoyi-common-satoken`+`ruoyi-common-web`、demo 额外的 `ruoyi-common-redis`+`ruoyi-common-elasticsearch`+`ruoyi-common-mqtt`+`ruoyi-common-mcp`。social / mcp 主要由 admin 直接引入；elasticsearch / mqtt 主要由 demo 引入。

### R-007

- Claim: common 内部依赖分层（子 POM 显式 `ruoyi-common-*`）：`core` 不依赖其他 common；`json` 依赖 core；`redis` 依赖 core+json；`satoken` 依赖 core+redis；`security`/`log` 依赖 satoken（log 另加 json）；`web`/`excel`/`translation`/`sensitive` 依赖 json；`mybatis` 依赖 core+satoken；`oss` 依赖 json+redis；`push` 依赖 core+redis+satoken+json；`mail`/`doc`/`encrypt`/`job`/`liteflow`/`ai`/`mcp` 依赖 core；`mqtt` 依赖 core+json；`sms`/`social` 依赖 redis；`elasticsearch` 不依赖任何其他 `ruoyi-common-*`。
- Type: code fact
- Source: 各子模块 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/*/pom.xml</Path>` 中 `<artifactId>ruoyi-common-*</artifactId>`
- Confidence: high
- Limits: 第三方 starter（Easy-Es、sms4j、SnailJob、Spring AI MCP）的传递树未展开。
- Artifact impact: 新业务若只需工具类，依赖 `ruoyi-common-core`（及需要的 json/redis）即可；Web CRUD 通常再加 mybatis+web+security。

### Conflicts and Unknowns

- 未执行 `mvn dependency:tree`：admin 运行时 common 并集是 POM 图推断，个别 optional/profile 边界未用 Maven 解析交叉验证。
- `ruoyi-common-excel` POM description 仅为 artifactId 自身（「ruoyi-common-excel」），没有业务说明；职责由 `ExcelBuilder` JavaDoc 与 fesod 依赖推断。
- 全仓 `ruoyi-common` 无 `ExcelUtil` 类。若上游文档仍写 `ExcelUtil`，与本仓库 6.0.0 源码冲突，应以 `ExcelBuilder` 为准。
- `DictService` / `PermissionService` / `SensitiveService` 的实现类不在 common（实现落在 `ruoyi-system`，见 sibling research-02）；本文件只确认接口定义位置。
- `ruoyi-common-elasticsearch` 不依赖其他 common，Easy-Es 如何与本工程 Spring 容器/数据源对齐未在本模块 2 个 Java 类中展开。
- `ruoyi-common-job` / `ruoyi-common-ai` 各仅 1 个 `@Configuration` 包装类，业务 API 在对应第三方 starter（SnailJob / SnailAi），本调研未展开其外部方法表。
- oss / excel 无 `AutoConfiguration.imports`：Oss 使用静态 `OssFactory`；Excel 为纯工具。是否还有被 `org.dromara` 包扫描拾取的 `@Component` 未逐文件确认（Oss 配置类本身是 Builder/record，不是 Spring 配置）。

### Recommendation

- 后续 `ruoyi-common` Skill 应以本文件 25 模块表 + core 21 个工具类 + 15 个跨模块 Utils/Helper 为权威目录，按 artifactId 路由，不要把父 POM 当成可依赖 jar。
- 指导调用方：CRUD/Web 走 mybatis+web+security；缓存/锁/限流走 redis 工具与注解；登录态走 `LoginHelper`；文件走 `OssFactory`/`OssClient`；导入导出走 `ExcelBuilder`；邮件走 `MailBuilder`；推送走 `PushHelper`；字典/权限/脱敏只注入 SPI，不在 common 找实现。
- 三个消费方模板：system 显式 13 个 common（偏 OSS/脱敏/加密/推送）；workflow 显式 11 个（含 liteflow/mail）；admin 只直接补 social/mail/doc/mcp，其余靠业务模块并集。需要 ES/MQTT 时看 demo，不要写进 system/workflow POM。
- 本调研已满足停止条件；创建 Skill 不在本次范围。
