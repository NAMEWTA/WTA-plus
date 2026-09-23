# 跨模块 Utils / Helper 索引

条目不够明确时，按路径读取源码，不得凭空推断。路径相对工作区，前缀 `backend/`。职责取各类 JavaDoc 首句。

全 `wta-common` glob：`*Utils.java` 29 + `*Helper.java` 4 + `*Util.java` 3。本表覆盖 **15** 个跨模块 notable `*Utils` / `*Util` / `*Helper`（不含 [core-utils.md](core-utils.md) 的 21 个）。`ExcelBuilder` / `ExcelWriterWrapper` / `MailBuilder` / `OssFactory` / `OssClient` 不是 Utils 命名，但替代旧入口，附在对应模块。

依赖哪个 artifact 见 [module-map.md](module-map.md)。

新业务层级约束见后端[框架公共入口与分层用法](../../namewta-fullstack-development/references/backend/framework-usage.md)：优先调用本页列出的项目入口，不直接复制底层实现。`RedisUtils` 内部已经封装 `RedissonClient`；只有明确的 Store/Provider owner 需要原子、锁或队列特性时才允许直接使用 Redisson。`OssFactory`/`OssClient` 是基础设施入口，业务 Service 优先依赖 `wta-api` 的 OSS 合同；通知统一走 `NotifyClient`/`NotifyDispatcher`，不要直连短信或邮件渠道。

## 目录

1. [redis](#redis)
2. [json](#json)
3. [encrypt](#encrypt)
4. [social](#social)
5. [mybatis](#mybatis)
6. [satoken / LoginHelper](#satoken--loginhelper)
7. [push](#push)
8. [liteflow](#liteflow)
9. [oss](#oss)
10. [excel（无 ExcelUtil）](#excel无-excelutil)
11. [mail](#mail)

## redis

模块：`wta-common/wta-common-redis/`。包 `org.namewta.common.redis.utils`。

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.redis.utils.RedisUtils` | redis 工具类。公开方法含限流、发布/订阅、对象/List/Set/Map/Hash 缓存、原子值、扫描删除及`deleteObjectIfEquals`所有权比较删除。内部取 `RedissonClient` | `src/main/java/org/namewta/common/redis/utils/RedisUtils.java` |
| `org.namewta.common.redis.utils.CacheUtils` | 缓存操作工具类。按 `cacheNames` + key 走 Spring `CacheManager` | `.../redis/utils/CacheUtils.java` |
| `org.namewta.common.redis.utils.QueueUtils` | 分布式队列工具。JavaDoc：轻量级队列；重量级数据量请使用 MQ；要求 redis 5.X 以上 | `.../redis/utils/QueueUtils.java` |
| `org.namewta.common.redis.utils.SequenceUtils` | 发号器工具类。委托 Redisson `RIdGenerator` | `.../redis/utils/SequenceUtils.java` |

有绝对业务截止的值使用 `RedisUtils.setCacheObjectUntil(key, value, Instant)`：单键脚本在 Redis 时钟尚未到期时以 `PXAT` 原子写入，已过期返回 `false` 且不改旧值/TTL。该入口沿原 Bucket codec 与 NameMapper，可由既有 `getCacheObject` 读取；不要改成提交后完整相对 TTL，也不要用分离的 SET/EXPIRE 留下无期限窗口。`RedisUtils` 客户端为静态绑定，隔离集成测试须在独立 JVM 内持有类级稳定客户端。

配套注解（同模块，非 Utils）：

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.redis.annotation.RepeatSubmit` | 同步短时防重；随机owner租约、失败原子比较删除、成功保留TTL；不替代业务持久幂等 | `.../redis/annotation/RepeatSubmit.java` |
| `org.namewta.common.redis.annotation.RateLimiter` | 限流注解。`key` 支持 Spring EL | `.../redis/annotation/RateLimiter.java` |

## json

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.json.utils.JsonUtils` | JSON 工具类。内部用 Jackson `JsonMapper`（`tools.jackson`）；无 Spring 测试上下文时使用内部 fallback，容器 Bean 可用后优先使用 Bean | `wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/JsonUtils.java` |

## encrypt

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.encrypt.utils.EncryptUtils` | 安全相关工具类（RSA/SM2/Base64 等，以源码方法为准） | `wta-common/wta-common-encrypt/src/main/java/org/namewta/common/encrypt/utils/EncryptUtils.java` |

## social

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.social.utils.SocialUtils` | 认证授权工具类。import `me.zhyd.oauth.*`；回调走 `AuthRedisStateCache` | `wta-common/wta-common-social/src/main/java/org/namewta/common/social/utils/SocialUtils.java` |

## mybatis

模块：`wta-common/wta-common-mybatis/`。

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.mybatis.utils.IdGeneratorUtil` | ID 生成工具类。优先委托 MyBatis-Plus `IdentifierGenerator`，无 Spring 测试上下文时回退 `IdWorker`；业务代码不得直连底层实现 | `src/main/java/org/namewta/common/mybatis/utils/IdGeneratorUtil.java` |
| `org.namewta.common.mybatis.helper.DataPermissionHelper` | 数据权限助手。Sa-Token Storage 键 `data:permission` | `.../mybatis/helper/DataPermissionHelper.java` |
| `org.namewta.common.mybatis.helper.DataBaseHelper` | 数据库助手。动态数据源 / 库类型 | `.../mybatis/helper/DataBaseHelper.java` |
| `org.namewta.common.mybatis.core.query.AggregateSelectUtils` | 聚合查询字段 SQL 构造工具。 | `.../mybatis/core/query/AggregateSelectUtils.java` |

`@DataPermission`：`.../mybatis/annotation/DataPermission.java`（「数据权限组注解，用于标记数据权限配置数组」）。Mapper 基类 `BaseMapperPlus`：`.../mybatis/core/mapper/BaseMapperPlus.java`。

## satoken / LoginHelper

路径：`wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java`。

FQN：`org.namewta.common.satoken.utils.LoginHelper`。JavaDoc「登录鉴权助手」：`user_type` 为用户类型（同一用户表可有 pc/app 等）；`device` 为设备类型（web/ios 等）；可组成多用户体系。依赖 `org.namewta.system.api.model.LoginUser`。

公开静态方法（以源码为准，未展开实现）：

| 方法 | 源码位置附近 |
|---|---|
| `login(LoginUser, SaLoginParameter)` | 基于设备类型登录，写入 Token extra 与 session `loginUser` |
| `getLoginUser()` / `getLoginUser(String token)` | 取当前或指定 token 的 `LoginUser` |
| `getUserId()` / `getUserIdStr()` / `getUsername()` | 当前用户 |
| `getDeptId()` / `getDeptName()` / `getDeptCategory()` | 当前部门 |
| `getUserType()` | 当前登录域 userType |
| `isSuperAdmin(Long userId)` / `isSuperAdmin()` | 超管判断 |
| `isLogin()` | 是否已登录 |

常量含 `LOGIN_USER_KEY`、`USER_KEY`、`CLIENT_KEY`、`CLIENT_PK_KEY`、`USER_TYPE_KEY` 等，见类字段。

配套：`config/SaTokenConfig.java`、`core/service/SaPermissionImpl.java`、`core/dao/PlusSaTokenDao.java`（同模块 `src/main/java/org/namewta/common/satoken/`）。

## push

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.push.helper.PushHelper` | 统一消息推送工具。指定用户/广播文本或自定义消息体；开关读 `message.enabled` | `wta-common/wta-common-push/src/main/java/org/namewta/common/push/helper/PushHelper.java` |

配套：`dto/PushDTO.java`、`controller/SseController.java`（同模块 `.../common/push/`）。

## liteflow

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.liteflow.utils.LiteFlowUtils` | LiteFlow 执行工具。`execute(chainId, context)` 调 `FlowExecutor.execute2Resp` | `wta-common/wta-common-liteflow/src/main/java/org/namewta/common/liteflow/utils/LiteFlowUtils.java` |

## oss

模块：`wta-common/wta-common-oss/`。无 AutoConfiguration.imports。

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.oss.util.BucketUrlUtil` | 桶链接工具类。路径风格 / virtual-host 风格 URL | `src/main/java/org/namewta/common/oss/util/BucketUrlUtil.java` |
| `org.namewta.common.oss.factory.OssFactory` | S3存储客户端工厂 | `.../oss/factory/OssFactory.java` |
| `org.namewta.common.oss.client.OssClient` | S3 存储客户端接口。`bucketXxx(...)` 显式桶 + 无前缀默认桶两套 API | `.../oss/client/OssClient.java` |

## excel（无 ExcelUtil）

全 `wta-common` 无名为 `ExcelUtil` 的类。若上游文档仍写 `ExcelUtil`，以本仓库 `ExcelBuilder` 为准。

模块：`wta-common/wta-common-excel/`。无 AutoConfiguration.imports。

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.excel.utils.ExcelBuilder` | Excel 导出构造器。底层 `org.apache.fesod.sheet` | `src/main/java/org/namewta/common/excel/utils/ExcelBuilder.java` |
| `org.namewta.common.excel.utils.ExcelWriterWrapper` | ExcelWriterWrapper Excel写出包装器。与 ExcelWriter 一一对应，避免直接关闭 IO | `.../excel/utils/ExcelWriterWrapper.java` |
| `org.namewta.common.excel.core.ExcelResult` | excel返回对象 | `.../excel/core/ExcelResult.java` |

字典列转换会 `SpringUtils.getBean(DictService.class)`：`convert/ExcelDictConvert.java`、`core/ExcelDownHandler.java`。

## mail

| FQN | 职责 | 路径 |
|---|---|---|
| `org.namewta.common.mail.core.MailBuilder` | 邮件发送构建器。运行时 SMTP 账户由通知配置解析后传入，不从 YAML 发件人读取。 | `wta-common/wta-common-mail/src/main/java/org/namewta/common/mail/core/MailBuilder.java` |
