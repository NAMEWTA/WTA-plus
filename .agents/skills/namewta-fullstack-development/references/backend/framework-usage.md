# 后端框架公共入口与分层用法

本页只规定新业务模块如何选择现有框架入口和允许层级；具体 artifact、公开类型和方法以 [wta-common-modules-guide](../../../wta-common-modules-guide/SKILL.md) 及其源码为准。禁止为了绕过既有入口创建同义 `Utils`、`Helper`、`Manager` 或自定义框架包装。

## 能力矩阵

| 能力 | 首选项目入口 | 允许层级 | 关键约束 |
|---|---|---|---|
| MyBatis-Plus / SQL | `BaseMapperPlus`、`QueryBuilder`、MPJ、Mapper XML | layered: DAO/Mapper；classic: ServiceImpl/Mapper | layered 的 Wrapper、分页、条件更新和行锁只在 DAO；Mapper 只声明 SQL；复杂查询进入 XML |
| JSON | `org.namewta.common.json.utils.JsonUtils` | Service、纯 Codec、必要时 Controller transport 适配 | UseCase 只编排 Service，不直接处理序列化；不直接新建 Jackson/ObjectMapper；专用 Codec 才可封装序列化细节 |
| 登录态 / Sa-Token | `org.namewta.common.satoken.utils.LoginHelper`、现有 `@SaCheckPermission`/`@SaIgnore` | Controller 和安全适配器 | 当前用户、userType、client 信息在入口解析后作为明确参数传入 UseCase；不要在 DAO 读取会话 |
| Redis / 锁 / 限流 | `org.namewta.common.redis.utils.RedisUtils`、`CacheUtils`、`@RepeatSubmit`、`@RateLimiter` | Service 的外部 Store/Port、Controller 注解 | 普通缓存不直接持有 `RedissonClient`；复杂原子操作才在有 owner 的 Store adapter 中使用 Redisson；锁查询必须经 Service 在 UseCase 事务内调用 |
| OSS | `wta-api` 的 `OssService`；基础设施实现使用 `OssFactory`/`OssClient` | Service 的外部 Gateway/Port；system 基础设施 | 业务模块不得直接依赖 OSS 客户端实现；文件引用、删除和 URL 生成必须保留当前 API 合同 |
| 通知 | `org.namewta.common.notify.core.NotifyClient`/`NotifyDispatcher`、`NotifyChannelAdapter` | Service 的外部 Port 或提交后事件适配器 | 说明提交时机、幂等键、失败补偿和敏感字段脱敏；不要自行直连邮件、短信渠道 |
| 工作流 | `org.namewta.workflow.api.WorkflowService` 与公开事件合同 | `adapter/gateway` 封装为 Service 使用的外部 Port；UseCase 通过 Service 间接编排，事件由 listener 转发 | 业务模块只依赖公开 workflow API/事件；不得依赖 `wta-workflow` implementation POM、内部 `IFlw*` 或 Warm-Flow 实现 |
| 操作日志 | `org.namewta.common.log.annotation.Log` + `BusinessType` | Controller 写入口 | POST 业务接口必须准确设置 title、BusinessType 和敏感参数保存策略；不要在 Service 重复记录同一操作日志 |
| 脱敏 / 字典 / 权限 SPI | `SensitiveService`、`DictService`、`PermissionService` | Service 或公共转换辅助 | 注入公开 SPI；实现位于 system，业务模块不依赖 system implementation |
| Excel / 推送 | `ExcelBuilder`/`ExcelWriterWrapper`、`PushHelper` | Controller 适配器和 Service 外部 Port | 导出只使用 VO；推送说明提交后时机和幂等；不存在 `ExcelUtil` |

## 分层放置

- Controller 负责 HTTP 绑定、校验、认证/权限、`@Log`、响应包装；不构造 Wrapper，不调用工具执行业务事务。
- UseCase 负责用例编排、事务和幂等边界；不读取登录上下文，不调用 DAO、Mapper 或公共基础设施实现。
- Service 负责业务规则和外部 Port；可以调用 `JsonUtils`、SPI 和明确的 Gateway/Store/Provider，但不能调用另一个 Service 或 MyBatis 类型。
- DAO 负责持久化查询条件、分页、锁和 Mapper 结果转换；只能调用本 owner Mapper 及纯 domain 辅助。
- Mapper 负责 SQL 声明和 XML 映射；不能调用工具、缓存、通知或其他 Mapper。
- Gateway/Provider/Store 是端口或适配器，不是第六层。只有存在真实外部系统、缓存存储或可替换提供者时才建立，禁止以 `*DataSupport` 包装 Mapper。

## 常见错误

- 用 `ObjectMapper`、Hutool 同名工具或自建 JSON helper 代替 `JsonUtils`。
- `JsonUtils` 与 `IdGeneratorUtil` 已兼容无 Spring 测试上下文：仅在找不到容器 Bean 时使用内部 fallback，容器就绪后仍解析并缓存项目标准 Bean。业务代码仍必须调用这两个统一入口，不得自行 `new JsonMapper`、缓存静态 Bean 结果或直接调用 `IdWorker`。
- 在 DAO/Service 注入 `LoginHelper`、通过静态登录态决定数据库条件，导致业务难测和权限越层。
- 直接在业务 Service 使用 `RedissonClient`、`OssFactory` 或渠道 SDK，却没有可测试的 Store/Gateway owner。
- 在 UseCase 直接发通知、写 Redis、访问 OSS 或调用 `wta-system` implementation。
- 只在 Controller 隐藏按钮，未在入口权限和 Mapper 数据权限上验证。
- 将 `Row`/`Projection` 直接返回 Controller，或把它们当成独立 Service/DAO 层。

## 验证

根据模块模式运行 `validate-module-mode.mjs`；静态检查工具和框架 import 的允许层级，再运行权限、事务、缓存、OSS/Notify 失败补偿及 Maven 测试。若要新增公共入口，先更新 `wta-common-modules-guide` 的源码索引和 artifact 选择，再改业务 Skill。
