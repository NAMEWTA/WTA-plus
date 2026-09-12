# wta-common-core 工具类与基元

条目不够明确时，按路径读取源码，不得凭空推断。路径相对工作区，前缀 `wta-vue-plus-namewta/`。职责取各类 JavaDoc 首句。未逐方法展开 API；方法语义以源码为准。

模块根：`wta-common/wta-common-core/src/main/java/org/namewta/common/core/`。Maven：`wta-common/wta-common-core/pom.xml`。

## 目录

1. [领域基元与异常](#领域基元与异常)
2. [跨模块 SPI（实现不在 core）](#跨模块-spi实现不在-core)
3. [org.namewta.common.core.utils（13）](#orgdromaracommoncoreutils13)
4. [子包 utils（8，含 SqlUtil）](#子包-utils8含-sqlutil)
5. [同层工厂（非 Utils）](#同层工厂非-utils)
6. [未列入本表](#未列入本表)

权威清单：**20 个 `*Utils` + 1 个 `SqlUtil` = 21**。多数继承 Hutool / Commons Lang；项目约定优先用这些 FQN。

## 领域基元与异常

| FQN | 职责（JavaDoc 首句） | 路径 |
|---|---|---|
| `org.namewta.common.core.domain.R` | 响应信息主体 | `.../core/domain/R.java` |
| `org.namewta.common.core.domain.PageResult` | 表格分页数据对象 | `.../core/domain/PageResult.java`。本仓库无旧名 `TableDataInfo`。 |
| `org.namewta.common.core.exception.ServiceException` | 通用业务异常，支持使用占位符拼接错误信息。 | `.../core/exception/ServiceException.java` |

`...` = `wta-common/wta-common-core/src/main/java/org/namewta/common`。

## 跨模块 SPI（实现不在 core）

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.core.service.DictService` | 通用 字典服务。方法：`getDictLabel` / `getDictValue`（含分隔符重载）、`getAllDictByDictType`、`getDictType`、`getDictData` | `.../core/service/DictService.java` |
| `org.namewta.common.core.service.PermissionService` | 用户权限处理。方法：`getRolePermission(userId, clientId)`、`getMenuPermission(userId, clientId)` | `.../core/service/PermissionService.java` |

实现见 [module-map.md](module-map.md) SPI 节。字典校验还会经 `.../core/validate/dicts/DictPatternValidator.java` 调 `SpringUtils.getBean(DictService.class)`。

## org.namewta.common.core.utils（13）

目录：`wta-common/wta-common-core/src/main/java/org/namewta/common/core/utils/`。

| FQN | 职责 | 继承 / 备注 | 路径 |
|---|---|---|---|
| `DateUtils` | 时间工具类 | extends Hutool `DateUtil` | `utils/DateUtils.java` |
| `SpringUtils` | spring工具类 | extends Hutool `SpringUtil`；`@Component`；已列入 AutoConfiguration.imports | `utils/SpringUtils.java` |
| `TreeBuildUtils` | 扩展 hutool TreeUtil 封装系统树构建 | extends Hutool `TreeUtil` | `utils/TreeBuildUtils.java` |
| `StringUtils` | 字符串工具类 | extends Apache Commons `StringUtils` | `utils/StringUtils.java` |
| `MapstructUtils` | Mapstruct 工具类（JavaDoc 另给 mapstruct-plus 文档链接） | 内部 `SpringUtils.getBean(Converter.class)` | `utils/MapstructUtils.java` |
| `ServletUtils` | 客户端工具类，提供获取请求参数、响应处理、头部信息等常用操作 | extends Hutool `JakartaServletUtil` | `utils/ServletUtils.java` |
| `StreamUtils` | stream 流工具类 | 无父 Util | `utils/StreamUtils.java` |
| `ValidatorUtils` | Validator 校验框架工具 | Jakarta Validator | `utils/ValidatorUtils.java` |
| `ThreadUtils` | 线程工具 | 无父 Util | `utils/ThreadUtils.java` |
| `DesensitizedUtils` | 脱敏工具类 | extends Hutool `DesensitizedUtil` | `utils/DesensitizedUtils.java` |
| `MessageUtils` | 获取i18n资源文件 | 委托 Spring `messageSource` | `utils/MessageUtils.java` |
| `ObjectUtils` | 对象工具类 | extends Hutool `ObjectUtil` | `utils/ObjectUtils.java` |
| `NetUtils` | 增强网络相关工具类 | extends Hutool `NetUtil` | `utils/NetUtils.java` |

上表 FQN 均在包 `org.namewta.common.core.utils`。路径均相对 `.../core/`。

## 子包 utils（8，含 SqlUtil）

| FQN | 职责 | 继承 / 备注 | 路径 |
|---|---|---|---|
| `org.namewta.common.core.utils.reflect.ReflectUtils` | 反射工具类. 提供调用getter/setter方法, 访问私有变量, 调用私有方法, 获取泛型类型Class, 被AOP过的真实类等工具函数. | extends Hutool `ReflectUtil` | `utils/reflect/ReflectUtils.java` |
| `org.namewta.common.core.utils.reflect.AnnotationUtils` | 注解工具类 | extends Hutool `AnnotationUtil` | `utils/reflect/AnnotationUtils.java` |
| `org.namewta.common.core.utils.ip.AddressUtils` | 获取地址类 | | `utils/ip/AddressUtils.java` |
| `org.namewta.common.core.utils.ip.RegionUtils` | IP地址行政区域工具类（ip2region xdb） | | `utils/ip/RegionUtils.java` |
| `org.namewta.common.core.utils.regex.RegexUtils` | 正则相关工具类 | extends Hutool `ReUtil` | `utils/regex/RegexUtils.java` |
| `org.namewta.common.core.utils.file.FileUtils` | 文件处理工具类 | extends Hutool `FileUtil` | `utils/file/FileUtils.java` |
| `org.namewta.common.core.utils.file.MimeTypeUtils` | 媒体类型工具类 | | `utils/file/MimeTypeUtils.java` |
| `org.namewta.common.core.utils.sql.SqlUtil` | sql操作工具类 | 关键字过滤 / `order by` 校验 | `utils/sql/SqlUtil.java` |

## 同层工厂（非 Utils）

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.core.factory.YmlPropertySourceFactory` | yml 配置源工厂 | `factory/YmlPropertySourceFactory.java` |
| `org.namewta.common.core.factory.RegexPatternPoolFactory` | 正则表达式模式池工厂 | `factory/RegexPatternPoolFactory.java` |

## 未列入本表

同目录 `utils/regex/RegexValidator.java` JavaDoc「正则字段校验器」——不是 `*Utils`，未计入 21。需要时直接读该类，不要与 `RegexUtils` 混用。
