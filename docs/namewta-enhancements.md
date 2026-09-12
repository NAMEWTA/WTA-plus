# NAMEWTA 增强说明

NAMEWTA 是 WTA-plus 的产品发行线。目录所有权和安全不变量以本工作区当前实现为准。

## 前端增强

### 多 App 领域化 monorepo

前端从单 App 源码树调整为以下所有权分层：

| 层级 | 当前职责 |
|---|---|
| `apps/*` | 独立 Client、入口、布局、品牌、主题、路由组合、会话命名空间和部署配置 |
| `packages/domains/*` | 无 Vue/DOM 的 API、领域类型、查询/命令服务和传输映射 |
| `packages/web-domains/*` | Vue 页面、领域组件、Web hooks、语言资源和动态路由 manifest |
| `packages/platform/*` | 认证、权限、HTTP、运行时等跨领域端口和合同 |
| `packages/adapters/*` | Axios、浏览器存储、浏览器加密及未来终端适配实现 |
| `packages/web-kit/*` | 多个 Web 消费者共同使用的壳层、基础组件和设计 token |
| `packages/api-contracts` | 可追溯、确定性生成的 OpenAPI 传输合同 |

当前发布面包含 `admin-web` 管理端和 `home-web` 用户门户；未激活终端不参与工作区构建、路由和权限注册。

domain 与后端模块一一对应为 `admin`、`system`、`workflow`、`demo`、`profile`、`notify`、`third`、`ai`；包内第二层按 Controller 的稳定 HTTP 资源命名。由此可以从 `/system/client` 直接定位到 `domains/system/src/client`，再定位到 `web-domains/system/src/client` 的 Web 表现层。

App 不重新实现后端接口和数据模型。新增 App 时只选择需要的公开 domain/web-domain，并实现该终端自己的适配和界面组合。未来移动端、小程序可以复用 headless domain 与 platform 合同，同时替换终端特有 UI、请求、存储和生命周期。

### Client 级认证、动态路由与权限

每个 App 使用显式 ClientId 和独立会话命名空间。登录前先获取服务端 ClientContext；注册开关、登录域、请求加密和认证方式均由服务端上下文约束，失败或畸形数据按关闭处理。

Admin 登录后恢复用户信息，按当前 Client 获取后端菜单，由 `packages/platform/app-runtime` 生成确定导航投影，再使用 App 已选择的 web-domain manifest 解析组件键并通过 Vue Router `addRoute` 注册。Admin 自有 `navigation` Store 维护 sidebar、topbar、default 和 Router 投影；`packages/web-kit/permission` 安装的 `v-hasPermi`、`v-hasRoles` 与命令式检查消费同一实时 evaluator。后端仍负责最终授权。

### 工程质量

前端使用 pnpm workspace/catalog 管理依赖，并以架构检查、OpenAPI 漂移检查、Oxlint、Oxfmt、TypeScript、Vitest、工作区构建和 Playwright 验证包边界及关键流程。跨 App 导入、包深层导入、跨工作区相对导入和未声明依赖由门禁阻止；第二个 App 激活时必须恢复真实跨 App Client 与会话隔离验收。

## 后端增强

### Client 登录域、RBAC 与会话隔离

用户类型由可维护的登录域及用户关联表达，不再依赖用户表上的单值类型。Client 可配置允许的登录域、注册开关和默认角色；密码、短信、邮件、社交和小程序登录都必须通过当前 Client 准入。

Token 明确区分 OAuth `clientId`、数据库 Client 主键和登录域。角色、菜单、按钮权限、动态路由、默认角色和超级管理员查询按 `userId + Client 主键` 计算；缺失 Client 上下文时拒绝跨域兜底。用户、Client、登录域或角色状态变化时，相关 Client 会话会被定向清理。

### OSS 直传与对象生命周期

后端提供浏览器直传控制面，支持单文件和 Multipart 上传、断点续传、服务端分片校验、过期会话清理及 Client/用户归属校验。对象完成上传后继续通过业务引用、临时状态、授权下载、可恢复删除和物理清理管理生命周期。

### 统一通知

通知基础设施通过渠道标识和适配器解耦业务与邮件/短信实现，补充 Redis 幂等状态机、请求上下文、OSS 附件快照、逻辑通知与投递记录、敏感字段脱敏、清理策略及全局监控。业务模块不再直接绑定具体发送工具。

通知公告先保存草稿，再单独发布。发送范围支持全部正常用户、指定用户和指定用户类型（登录域），接收人通过 `wta-api` 的公开 `UserService` 解析；渠道可选择站内信、短信和邮件，默认站内信。发布后保留内容、通知类型与渠道快照，投递记录用于区分已提交、供应商已接受和实际送达。

### 完整 HTTP 系统日志

`wta-common-web` 在 Servlet Filter 边界生成一对可由 `requestId` 关联的请求与响应结构化事件，覆盖同步、异步和异常完成路径。正文采集具有大小上限和媒体类型策略，不可记录或无法采集时输出明确原因；日志输出失败不会改变业务响应。

### 模块和数据治理

`wta-admin` 保持组合入口，业务能力位于 `wta-modules`，跨模块服务与 DTO 通过 `wta-api` 或 common SPI 暴露。默认 full bundle 和 `bundle-core` 都有明确的 Maven profile 与验证方式。

NAMEWTA 当前只支持并验收 MySQL 8.4。数据库初始化资产由父仓库 `release-artifacts/docker/infrastructure/mysql/init/` 统一拥有，六份 SQL 是可直接修改的当前完整基座：`10` 至 `40` 分别承载 WTA、Job、Workflow、AI，`50-namewta-ddl.sql` 承载 NAMEWTA 结构，`60-namewta-dml.sql` 承载 NAMEWTA 数据（包括三方接口管理菜单与权限）。Nacos schema 保持在 `init/nacos/` 独立初始化。后端仓库不再保存 `script/` 或 SQL 副本，也不维护 PostgreSQL、Oracle、SQL Server 方言。

全新环境按文件名前缀顺序执行全部六份基座。已有环境不得重放基座；升级时必须指定源 Git Tag 与目标 Git Tag，备份现场，在隔离库中生成、评审并演练差异 SQL，再执行获批的升级步骤。
