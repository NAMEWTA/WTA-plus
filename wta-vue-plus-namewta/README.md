# NAMEWTA 后端

本仓库是基于 WTA-Plus 持续演进的 NAMEWTA 后端增强版。它保留上游模块化业务基础，并重点增强 Client 登录域与 RBAC 隔离、OSS 直传和对象生命周期、统一通知、完整 HTTP 系统日志、增量 SQL 及组合构建治理。

`main` 是 NAMEWTA 产品实现权威，`6.X` 只跟踪上游能力，不承载本地业务提交。

## 相较上游的核心增强

| 增强方向 | 当前实现 | 主要位置 |
|---|---|---|
| Client 登录域 | Client 可配置允许的登录域、注册开关和默认角色；用户可属于多个登录域；全部认证策略统一执行当前 Client 准入 | `wta-admin` 认证策略、`wta-modules/wta-system` Client 与用户域服务 |
| RBAC 隔离 | 角色、菜单、按钮权限、动态路由、默认角色和超级管理员查询按 `userId + Client 主键` 计算 | `wta-system` permission、role、menu、user、client |
| Token 与会话 | Token 明确携带 Client 主键和登录域，区分 OAuth `clientId`；配置或身份状态变化时定向清理会话 | `wta-api` LoginUser、`wta-admin` 登录、`wta-system` 会话服务 |
| OSS 直传 | 浏览器直接向对象存储上传，支持单文件、Multipart、断点续传、分片校验、上传会话隔离和失败恢复 | `wta-common-oss`、`wta-system` oss/upload |
| 对象生命周期 | 通过临时状态、业务引用、授权下载、可恢复删除、过期与物理清理管理文件 | `wta-api` OssService、`wta-system` OSS 服务与任务 |
| 统一通知 | 邮件/短信使用渠道适配器统一分发，支持 Redis 幂等、OSS 附件快照、请求上下文、脱敏、投递记录和监控 | `wta-common-notify`、`wta-system` notify |
| HTTP 系统日志 | Servlet Filter 为每次请求输出可由 requestId 关联的结构化请求/响应事件，覆盖异步、异常、正文截断和媒体类型策略 | `wta-common-web/.../logging` |
| 模块组合 | 明确 `wta-admin` 组装、`wta-api` 跨模块合同和 common SPI 边界，同时验证 full/core bundle | 根 POM、`wta-admin`、`wta-api`、`wta-common` |
| MySQL 合同 | 后端测试消费父聚合仓库维护的六份 MySQL 8.4 完整基座，本仓库不保存 SQL 副本 | `../release-artifacts/docker/infrastructure/mysql/init` |

在父聚合工作区中，更完整的跨端行为和安全不变量位于 `docs/namewta-enhancements.md` 与 `docs/upstream/customization-map.md`。独立克隆本仓库时，可直接以本 README、[上游边界](docs/upstream/README.md)和当前测试作为入口。

## 技术栈

- Java 21、Spring Boot 4.1、Maven Wrapper。
- Sa-Token、MyBatis-Plus、dynamic-datasource、Redisson。
- MySQL 8.4 是 NAMEWTA 业务扩展当前唯一支持并自动化验收的数据库。
- Redis、MinIO 等外部服务用于会话、缓存、OSS 与集成测试。

## 模块结构

```text
wta-admin/       主应用与模块组装
wta-api/         跨业务模块公开服务和 DTO
wta-common/      可按需依赖的通用基础能力
wta-modules/     system、workflow、demo、profile、notify、ai、job、third 业务模块
wta-extend/      monitor、SnailJob、SnailAI 等独立应用
```

`wta-admin` 只负责组装。跨模块调用应通过 `wta-api` 或明确的 common SPI，禁止依赖其他业务模块的 mapper、entity 或内部实现。

## 与前端的协作边界

后端向所有 App 提供一致的 HTTP/JSON 合同，并负责最终认证、授权、数据范围和 Client 隔离。前端可以按 App 选择页面与菜单表现，但不能放宽服务端权限。

前端只消费 HTTP/JSON 合同。Vue 页面、动态路由和 App 组合归独立仓库 `plus-ui-namewta` 所有，后端仓库不维护前端源码副本。

新增前端 App 不需要在后端复制 Controller。它应申请或配置独立 ClientId，并复用相同的领域接口；需要不同准入、默认角色、菜单和权限时，通过 Client 级数据配置实现。

## 构建与验证

```bash
# 完整测试
./mvnw test

# 默认全量组合
./mvnw clean package -DskipTests

# 核心平台组合；必须在完整测试通过后执行
./mvnw clean package -Pbundle-core -Dmaven.test.skip=true
```

每次 bundle 构建都必须先 `clean`，避免复用另一 profile 的 fat jar。Maven 默认执行测试，只有已有独立测试证据的打包阶段才允许跳过。

## HTTP 与数据规则

- CRUD 只读查询使用 `GET`。
- 新增、修改、删除、状态和排序等业务变更使用 `POST`。
- 每个 POST 业务接口使用准确、安全的 `@Log` 记录调用追踪。
- 新建或实质修改的业务事务使用 `@DSTransactional`；事务事件使用匹配的 `@DsTxEventListener`。
- 权限、数据范围、缓存失效、关联维护和删除前校验以同模块成熟实现为准。

`@Log` 记录具体业务操作，完整 HTTP 系统日志记录通用请求/响应交换，两者用途不同且可以同时存在。系统日志正文具有字节上限和媒体类型策略，不应把凭据、密钥、Token 或不受控大正文写入日志。

## SQL

数据库初始化与发布资产由父聚合仓库 `release-artifacts/docker/infrastructure/mysql/init/` 统一维护，本仓库不再保留 `script/` 或 SQL 副本。后端 SQL 合同测试默认自动定位父仓库；独立检出后运行相关测试时，通过 `-Dnamewta.sql.root=/绝对路径/release-artifacts/docker/infrastructure/mysql/init` 显式指定六文件基座目录。

项目只支持 MySQL 8.4。全新库按六份文件的数字前缀初始化；已有库不得重放基座，必须依据源/目标 Git Tag 的基座差异形成单独的评审与执行方案。

## 开发导航

- 父工作区工程规范：`../.agents/skills/engineering-standards/SKILL.md`
- 父工作区后端导航：`../.agents/skills/namewta-fullstack-development/SKILL.md`
- 上游边界：[docs/upstream/README.md](docs/upstream/README.md)

项目开发 Skill 只在 `wta-vue-plus-docs/.agents/skills/` 集中维护，本仓库不保留 `.claude` 或 `.codex` 副本。独立克隆本仓库时以本 README、模块源码、POM、测试和上游边界文档为证据。

## 许可证

本仓库保留上游项目许可证和署名，详见 `LICENSE`。上游能力进入 NAMEWTA 时按当前模块和公开合同适配，不以覆盖本地 Client 隔离、OSS、通知或日志增强为代价。
