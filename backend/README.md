# NAMEWTA 后端

本目录是 WTA-plus monorepo 的后端（`backend/`）。它增强 Client 登录域与 RBAC 隔离、OSS 直传和对象生命周期、统一通知、HTTP 系统日志、六文件 SQL 基座及组合构建治理。

## 核心能力

| 增强方向 | 当前实现 | 主要位置 |
|---|---|---|
| Client 登录域 | Client 可配置允许的登录域、注册开关和默认角色；用户可属于多个登录域；全部认证策略统一执行当前 Client 准入 | `wta-admin` 认证策略、`wta-modules/wta-system` Client 与用户域服务 |
| RBAC 隔离 | 角色、菜单、按钮权限、动态路由、默认角色和超级管理员查询按 `userId + Client 主键` 计算 | `wta-system` permission、role、menu、user、client |
| Token 与会话 | Token 明确携带 Client 主键和登录域，区分 OAuth `clientId`；配置或身份状态变化时定向清理会话 | `wta-api` LoginUser、`wta-admin` 登录、`wta-system` 会话服务 |
| OSS 直传 | 浏览器直接向对象存储上传，支持单文件、Multipart、断点续传、分片校验、上传会话隔离和失败恢复 | `wta-common-oss`、`wta-system` oss/upload |
| 对象生命周期 | 通过临时状态、业务引用、授权下载、可恢复删除、过期与物理清理管理文件 | `wta-api` OssService、`wta-system` OSS 服务与任务 |
| 统一通知 | 邮件/短信使用渠道适配器统一分发，支持 Redis 幂等、OSS 附件快照、请求上下文、脱敏、投递记录和监控 | `wta-api` NotificationService、`wta-common-notify`、`wta-modules/wta-notify` |
| HTTP 系统日志 | Servlet Filter 为每次请求输出可由 requestId 关联的结构化请求/响应事件，覆盖异步、异常、正文截断和媒体类型策略 | `wta-common-web/.../logging` |
| 模块组合 | 明确 `wta-admin` 组装、`wta-api` 跨模块合同和 common SPI 边界，同时验证 full/core bundle | 根 POM、`wta-admin`、`wta-api`、`wta-common` |
| MySQL 合同 | 后端测试消费父聚合仓库维护的六份 MySQL 8.4 完整基座，本仓库不保存 SQL 副本 | `../release-artifacts/docker/infrastructure/mysql/init` |

更完整的跨端行为和安全不变量见仓库根目录 `docs/namewta-enhancements.md`。

## 技术栈

- Java 21、Spring Boot 4.1、Maven Wrapper。
- Sa-Token、MyBatis-Plus、dynamic-datasource、Redisson。
- MySQL 8.4 是 NAMEWTA 业务扩展当前唯一支持并自动化验收的数据库。
- Redis、MinIO 等外部服务用于会话、缓存、OSS 与集成测试。

## OSS 运行与诊断

OSS 配置由数据库在启动时重建专用缓存；只有唯一合法的 PRIVATE 默认配置才可用于新上传。可选存储或诊断配置异常不会阻止核心启动。管理员可凭 `system:ossConfig:list` 使用 `POST /resource/oss/config/diagnose/{ossConfigId}` 诊断单个配置，公开结果只含固定状态、原因和检查时间。诊断不作为上传、下载或迁移的门禁，也不修改远端 Bucket/Policy。`/actuator/health/readiness` 检查核心 DB/Redis，`/actuator/health/liveness` 报告进程存活状态，`/actuator/health/ossdiagnostics` 仅显示已检查配置的观察快照；这些 Actuator 路径仍受现有 Basic Auth 保护，根 `/actuator/health` 不是核心专用组。诊断的每个网络步骤超时为 100ms–3s，最多五个顺序步骤，网络等待最多 15s，并非整个 HTTP 请求三秒上限。

## 模块结构

```text
wta-admin/       主应用与模块组装
wta-api/         跨业务模块公开服务和 DTO
wta-common/      可按需依赖的通用基础能力
wta-modules/     system、workflow、demo、profile、notify、ai、job、third、sso 业务模块
wta-extend/      monitor、SnailJob 独立应用
```

`wta-admin` 只负责组装。跨模块调用应通过 `wta-api` 或明确的 common SPI，禁止依赖其他业务模块的 mapper、entity 或内部实现。

## 与前端的协作边界

后端向所有 App 提供一致的 HTTP/JSON 合同，并负责最终认证、授权、数据范围和 Client 隔离。前端可以按 App 选择页面与菜单表现，但不能放宽服务端权限。

前端只消费 HTTP/JSON 合同。Vue 页面、动态路由和 App 组合归同一 monorepo 的 `frontend/` 工作区所有，后端目录不维护前端源码副本。

新增前端 App 不需要在后端复制 Controller。它应申请或配置独立 ClientId，并复用相同的领域接口；需要不同准入、默认角色、菜单和权限时，通过 Client 级数据配置实现。

## 本地配置与凭据

`wta-admin/src/main/resources/application-local.yml` 是被 Git 忽略的本机文件。
首次开发时，从 [公开模板](wta-admin/src/main/resources/application-local.example.yml) 复制一份，
通过进程环境提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `REDIS_PASSWORD`；
Redis 地址、端口和逻辑库可由 `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE` 覆盖。
模板没有密码默认值，缺少必需变量会明确报出缺失的配置键。不要把真实值写回模板、提交或粘贴到日志。

仓根 `scripts/start-dev.sh` 显式加载这份外部文件；已有 `SPRING_CONFIG_ADDITIONAL_LOCATION`
保持优先。直接通过 Maven 启动时，从 `backend/` 为该变量指定
`optional:file:$PWD/wta-admin/src/main/resources/application-local.yml`。生产配置由部署环境注入。
HTTP 本地 SSO 如需关闭 Secure Cookie，必须仅在 dev/local 显式设置 `SSO_COOKIE_SECURE=false`；
模板不关闭验证码或生产安全默认值。

应用 CORS 默认不许可跨来源浏览器请求，同源请求无需配置。生产发布从固定版本的
App Origin 清单注入 `WEB_CORS_ALLOWED_ORIGINS`，值为逗号分隔的精确
`http(s)://host[:port]`，不能使用 `*`、路径或通配子域；无效配置会使应用启动失败。
本地模板仅列出当前 Admin、Home、SSO Vite 代理使用的 `127.0.0.1`
入口（5177、5175、4176）。浏览器继续请求各 App 的同源代理路径；若开发入口的主机名或
端口不同，必须显式设置对应的精确 Origin。对象存储桶的浏览器直传 CORS 由桶配置单独管理，
不会因应用 CORS 白名单改变而扩大。CORS 只约束浏览器读取跨来源响应，接口仍按自身认证与权限规则鉴权。

本地配置和 `.example.yml/.yaml` 同时从 Maven 资源复制和 JAR 打包中排除，
旧 `target/classes` 残留也不能进入新 JAR。升级时先在仓库外安全备份原本机配置，更新后恢复到忽略路径；
不从旧 Git 历史恢复已披露密码。取消跟踪不会清除历史、已发布产物或旧日志。

已披露凭据的处置必须由对应环境负责人批准并逐项留存证据：

| 对象 | 执行前确认 | 执行与核验 | 当前状态 |
|---|---|---|---|
| MySQL 应用账号 | 环境、账号、依赖应用、最小权限与维护窗口 | 轮换后更新所有消费者，验证连接及必要操作；确认旧密码失效 | 待环境负责人执行 |
| Redis 应用账号 | 环境、ACL 用户、所有连接池/任务与维护窗口 | 轮换后更新消费者，验证授权命令及重连；确认旧凭据失效 | 待环境负责人执行 |
| 历史产物与日志 | 发布物/镜像/缓存/日志的保管人和披露范围 | 依据批准清单限制访问、替换或清理；只记录定位与摘要，不复制凭据 | 待范围核实与批准 |

记录应包含执行人、时间、受控证据位置和失败恢复步骤；失败时修正新配置，不恢复已披露值。
重写 Git 历史需单独批准，不能代替轮换。上述外部动作未执行前，不宣称泄漏风险已关闭。

## 构建与验证

以下命令 cwd 为 `backend/`；工作区入口见 [AGENTS.md](AGENTS.md)，模块与测试事实见[模块地图](../.agents/skills/engineering-standards/references/project/01-module-map.md)。

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

### 请求正文预算

普通 JSON（含 `+json`）和 OpenAPI 机器调用默认各允许 **2 MiB（2,097,152 字节）**。这是尚无业务样本和目标内存/并发预算时的初始配置，部署后应按合法业务请求与容量测量调整。

| 配置 | 环境变量 | 默认值 | 范围 |
|---|---|---|---|
| `namewta.web.request-body.max-size` | `REQUEST_BODY_MAX_SIZE` | `2MB` | 普通 JSON，以及 HTTP 日志需捕获的文本正文 |
| `openapi.max-body-size` | `OPENAPI_MAX_BODY_SIZE` | `2MB` | 机器签名调用的原始正文，独立于普通请求预算 |
| `sys.log.max-body-size` | — | `1MB` | 日志正文前缀；不代替入口预算 |

入口预算按收到的字节计数，固定长度声明和实际读取都检查；chunked 请求同样受限。超限直接返回 HTTP `413` 和 `REQUEST_BODY_TOO_LARGE`，不进入业务逻辑。零、负数、非法单位及超出 JVM 字节数组范围的配置会阻止启动。

机器验签使用未改写的原始字节；日志和重复读取共享同一份只读缓存，XSS 使用独立视图。验签合同保留必要的防御性副本。普通 multipart 上传、二进制流和 SSE 不进入此正文缓存，继续遵循各自的上传/流式处理约束；上述 2 MiB 不是所有 HTTP 上传的统一上限。

## SQL

数据库初始化与发布资产由仓根 `release-artifacts/docker/infrastructure/mysql/init/` 统一维护，本仓库不再保留 `script/` 或 SQL 副本。后端 SQL 合同测试默认从 monorepo 定位该目录；为隔离验证显式提供其他基座副本时，通过 `-Dnamewta.sql.root=/绝对路径/release-artifacts/docker/infrastructure/mysql/init` 显式指定六文件基座目录。

项目只支持 MySQL 8.4。全新库按六份文件的数字前缀初始化；已有库不得重放基座，必须依据源/目标 Git Tag 的基座差异形成单独的评审与执行方案。

## 开发导航

- 工程规范：`../.agents/skills/engineering-standards/SKILL.md`
- 后端导航：[namewta-fullstack-development](../.agents/skills/namewta-fullstack-development/SKILL.md)
- 通知模块：[wta-notify](wta-modules/wta-notify/AGENTS.md)
- SSO 模块：[wta-sso](wta-modules/wta-sso/AGENTS.md)

项目开发 Skill 只在仓库根 `.agents/skills/` 集中维护。

## 许可证

许可证见 `LICENSE`。
