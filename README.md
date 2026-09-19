# NAMEWTA WTA-Plus

NAMEWTA 增强版后台与多 App 前端的单一 monorepo（公开仓名 **WTA-plus**）。它不是简单改名，也不是上游源码副本：在保留 RuoYi-Vue-Plus / Plus-UI 主要业务能力的基础上，重点做了多 App 前端工程治理、Client 级身份与 RBAC 隔离、OSS 浏览器直传与对象生命周期、统一通知、HTTP 可观测性，以及可确定性初始化的数据基座。

交付不以 git submodule 或上游 URL 为依赖。完整能力、实现位置和边界见 [NAMEWTA 增强说明](docs/namewta-enhancements.md)。

| 子树 | 职责 | 详细说明 |
|---|---|---|
| `WTA-plus`（本仓） | 聚合文档、工程规范、发布资产与前后端同源 | 当前 README 与 [文档导航](docs/README.md) |
| `frontend` | Vue 3 多 App 领域化前端 | [前端 README](frontend/README.md) |
| `backend` | Spring Boot 模块化后端（`org.namewta` / `wta-*`） | [后端 README](backend/README.md) |

## 为什么选 / 突出点

一套后端同时服务管理端、用户门户和第一方 SSO，而菜单、Token、角色不会跨 App 串用。新终端按需组合已有 domain，而不是再复制一份 `src/api`。

| 方向 | NAMEWTA 增强 | 直接收益 |
|---|---|---|
| 多 App 前端治理 | 单体前端拆成 `apps + domains + web-domains + platform + adapters + web-kit` 的 pnpm monorepo | Admin / Home / SSO 独立交付；未来移动端或小程序可复用 headless domain，不必重写 API 与数据模型 |
| 前后端定位 | domain 对齐后端 `admin/system/workflow/demo/profile/notify/third/ai`，第二层按 Controller HTTP 资源命名 | 从 `/system/client` 可直接定位到 domain 与 web-domain |
| Client 身份域 | Client 独立配置登录域、注册开关、默认角色和用户归属；密码、短信、邮件、社交、小程序登录统一准入 | 一套后端安全服务多个产品入口和用户群体 |
| RBAC 与会话 | 角色、菜单、按钮、动态路由、默认角色和会话按 `userId + Client 主键` 计算 | 防止跨 App 菜单、权限、Token 和会话串用 |
| 第一方 SSO | 独立 `sso-web` 完成认人；业务应用各自持有会话，不共用 SSO cookie | 管理端、门户可走统一认人，同时保持 Client 隔离 |
| OSS 直传与生命周期 | 浏览器直传对象存储：单文件、分片、断点续传、失败恢复、对象引用、临时清理、可恢复删除、授权下载 | 大文件不再经应用服务器转发，并补齐对象全生命周期 |
| 统一通知 | 渠道无关分发：邮件/短信适配、Redis 幂等、OSS 附件快照、调用上下文审计、脱敏、投递监控 | 业务模块通过统一合同发送和追踪；公告先草稿再发布 |
| HTTP 可观测 | Servlet 边界输出可按 `requestId` 关联的请求/响应结构化事件 | 覆盖同步、异步、异常、正文截断和媒体类型策略，不必为每个接口重写访问日志 |
| 数据基座 | 父仓统一维护六份 MySQL 8.4 完整初始化基座（产品结构/数据分别由 10/50 文件拥有） | 全新环境确定性初始化；已有环境按源/目标 Git Tag 评审差异后升级 |
| 工程治理 | 单一 monorepo 固定基线：架构检查、OpenAPI 漂移检查、分层测试 | 产品能力在本仓演进，不以 submodule 或上游 URL 为交付依赖 |

## 架构一览

![NAMEWTA WTA-Plus 架构一览：终端 App、前端复用层、后端合同与数据基座](docs/image/architecture-overview.png)

```text
WTA-plus/
├── .agents/skills/           # 唯一项目开发 Skill 根目录
├── frontend/                 # 前端（合入，非 submodule）
│   ├── apps/                 # admin-web / home-web / sso-web
│   └── packages/             # domains、web-domains、platform、adapters、web-kit
├── backend/                  # 后端（合入，非 submodule）
│   ├── wta-admin/            # 服务启动与模块组装
│   ├── wta-api/              # 跨模块公开合同
│   ├── wta-common/           # OSS / 通知 / HTTP 日志等基础能力
│   └── wta-modules/          # system / workflow / demo / profile / notify / ai / job / third / sso
├── docs/                     # 当前架构与产品说明
├── release-artifacts/        # 发布资产及六份 MySQL 8.4 初始化基座
├── scripts/                  # 聚合 CI / 开发脚本
└── speculo/                  # 规格驱动研发状态
```

前后端通过 HTTP/JSON 合同协作并独立构建、测试和发布。前端 App 只负责 Client、布局、品牌和终端组合；可复用 API、类型、业务规则与 Web 页面分别归 domain 和 web-domain。后端 `wta-admin` 只负责组装，跨业务模块通过 `wta-api` 或明确的 common SPI 协作。

项目开发 Skill 统一位于 [`.agents/skills`](.agents/skills)，由 [工程规范](.agents/skills/engineering-standards/SKILL.md) 负责规则与质量门禁，再按任务路由到前端、后端和具体模块 Skill。本 monorepo 不维护工具专属的 `.claude` 或 `.codex` Skill 副本。

## 终端

| 终端 | 状态 | 说明 |
|---|---|---|
| `admin-web` | 已激活 | 完整后台管理端，组合八个业务领域及动态菜单权限 |
| `home-web` | 已激活 | 用户门户与用户中心，组合登录、注册和档案认证流程 |
| `sso-web` | 已激活 | 第一方 SSO 认人页；业务应用不共用这张会话 |

新增 App 时不复制 `admin-web/src/api`、业务类型或领域页面。App 从公开包入口选择所需 domain/web-domain，再提供本终端的请求、存储、布局和路由适配。

前端动态导航由 `packages/platform/app-runtime` 投影当前 Client 的服务端菜单，Admin 的 `navigation` Store 只维护 App 自有导航状态，页面解析严格使用已选择的 web-domain manifest。Vue 权限指令由 `packages/web-kit/permission` 提供，Admin 入口注入当前会话 evaluator；菜单和按钮可见性始终不替代后端鉴权。

## 功能速览

以下是历史本地预览截图，保留为界面参考，不作为当前工作树的验收或部署证据。截图时使用本仓本地预览（Admin `4174` / Home `4175` / SSO `4176`），侧栏与面包屑可见。弹窗一次性密钥未入库；列表页如含密钥列，仅为演示环境数据。未能打开的外部控制台见文末说明。

### Admin 登录与首页

登录页保留密码登录，并露出第一方 SSO 的 W 图标与其他第三方入口。登录后进入工作台，侧栏按当前 Client 动态投影。

![Admin 登录页，含第三方与 W 图标](docs/image/admin-login.png)

![Admin 工作台首页](docs/image/admin-home.png)

### 系统管理

用户、角色、菜单均带 Client 过滤：同一用户在不同 Client 下看到的菜单和按钮可以不同。

![用户管理](docs/image/admin-system-user.png)

![角色管理（按 Client 过滤）](docs/image/admin-system-role.png)

![菜单管理（按 Client 加载树）](docs/image/admin-system-menu.png)

![部门管理](docs/image/admin-system-dept.png)

![岗位管理](docs/image/admin-system-post.png)

![字典管理](docs/image/admin-system-dict.png)

![参数设置](docs/image/admin-system-config.png)

![操作日志](docs/image/admin-system-log-operlog.png)

![OpenAPI 管理](docs/image/admin-system-openapi.png)

### SSO 与多 Client

Client 是身份边界：登录域、注册开关、默认角色、SSO 接入状态都挂在 Client 上。SSO 管理只负责创建应用并交付配置，不走客户端管理的创建路径。

![登录域管理：系统用户 / 应用用户](docs/image/admin-system-login-domain.png)

![客户端管理：SSO 接入状态与登录域](docs/image/admin-system-client.png)

![SSO 管理列表（不含一次性 secret 弹窗）](docs/image/admin-system-sso.png)

### 通知中心

通知与渠道解耦。公告先保存草稿再发布；范围可以是全部正常用户、指定用户或指定登录域；渠道可选站内信、短信、邮件。投递记录区分已提交、供应商已接受和实际送达。

![通知管理 / 公告](docs/image/admin-notify-notice.png)

![通知配置 · 邮件渠道与场景绑定](docs/image/admin-notify-config-mail.png)

![通知配置 · 短信渠道](docs/image/admin-notify-config-sms.png)

![通知收件箱](docs/image/admin-notify-inbox.png)

![通知投递监控](docs/image/admin-notify-monitor.png)

### OSS / 文件

文件列表走对象生命周期字段（上传人、服务商、过期时间）。配置管理维护 MinIO / 云厂商访问点，页面不展示密钥明文。

![文件对象列表](docs/image/admin-oss-objects.png)

![OSS 配置（访问点与桶，无 secret 明文）](docs/image/admin-oss-config.png)

### 系统监控

在线用户按 Client 与会话展示；缓存监控读取 Redis 运行时指标。Spring Boot Admin、SnailJob、SnailAI、Nacos 控制台在本次预览环境不可达，见文末。

![在线用户](docs/image/admin-monitor-online.png)

![缓存监控](docs/image/admin-monitor-cache.png)

### 工作流与我的任务

流程分类、表达式、定义与监控沿用 Warm-Flow；待办/已办/抄送按当前用户投影。请假申请等演示单在测试菜单中。

![流程分类](docs/image/admin-workflow-category.png)

![流程定义](docs/image/admin-workflow-definition.png)

![流程表达式](docs/image/admin-workflow-spel.png)

![流程实例监控](docs/image/admin-workflow-processmonitor-processinstance.png)

![待办任务（流程监控）](docs/image/admin-workflow-processmonitor-alltaskwaiting.png)

![我发起的](docs/image/admin-task-mydocument.png)

![我的待办](docs/image/admin-task-taskwaiting.png)

![我的已办](docs/image/admin-task-taskfinish.png)

![我的抄送](docs/image/admin-task-taskcopylist.png)

### 档案、三方与测试菜单

档案中心覆盖个人、企业与材料标签。三方接口管理供应商、端点、调用明细和统计。测试菜单保留单表、树表、富文本和请假申请。

![个人档案](docs/image/admin-profile-person.png)

![企业档案](docs/image/admin-profile-enterprise.png)

![材料标签](docs/image/admin-profile-material-tag.png)

![三方供应商](docs/image/admin-system-third-provider.png)

![三方接口](docs/image/admin-system-third-endpoint.png)

![三方调用明细](docs/image/admin-system-third-invocation.png)

![三方调用统计](docs/image/admin-system-third-statistics.png)

![测试单表](docs/image/admin-demo.png)

![测试树表](docs/image/admin-demo-tree.png)

![富文本演示](docs/image/admin-demo-rich-text.png)

![请假申请](docs/image/admin-demo-leave.png)

### 用户门户与 SSO Web

Home 是独立 Client：门户介绍认证价值，登录页同样露出第一方 SSO。SSO Web 只负责认人，业务应用不会共用这张会话。

![Home 门户](docs/image/home-portal.png)

![Home 用户登录](docs/image/home-login.png)

![第一方 SSO 登录页](docs/image/sso-login.png)

未能截到的页面（环境暂不可达，未编造）：

- **登录日志**（`/system/log/logininfo`）：打开后会话被踢回登录页，未能稳定停留。
- **Admin 监控 / 任务调度中心 / AI 控制台 / Nacos 配置中心**：内嵌控制台持续 loading。本地 `application-local` 中 SnailJob / SnailAI 默认关闭，Nacos 控制台未作为本次预览依赖。
- **AI 会话**：页面报「加载 AI 聊天失败」。
- **旧路径** `/system/notice`：404，公告已归通知中心 `/notify/notice`。

## 获取与构建

本仓是单一 monorepo（前后端与文档合入，**不是** git submodule 交付）：

```bash
git clone https://github.com/NAMEWTA/WTA-plus.git
cd WTA-plus
```

前端在 `frontend/`，后端在 `backend/`。

- 前端：Node.js `>=20.19.0`、pnpm `>=10.0.0`；Vue 3、TypeScript 6、Vite 8、Pinia 4、Element Plus。
- 后端：Java 21、Spring Boot 4.1，仓库内 Maven Wrapper；Sa-Token、MyBatis-Plus、Redisson。
- 数据：MySQL 8.4 是 NAMEWTA 当前唯一支持并验收的数据库。全新库按 `release-artifacts/docker/infrastructure/mysql/init/` 六份基座的数字前缀执行；已有库不得重放基座，必须按源/目标 Git Tag 评审差异。

```bash
# 前端
cd frontend
pnpm install --frozen-lockfile
pnpm dev                 # Admin Web
pnpm architecture:check
pnpm test
pnpm build:prod

# 后端（从 frontend 返回同仓 backend）
cd ../backend
./mvnw test
./mvnw clean package -DskipTests
# 核心平台组合（须在完整测试通过后）
./mvnw clean package -Pbundle-core -Dmaven.test.skip=true
```

构建命令不会自动发布。三个 App 均已进入工作区构建和发布清单，实际发布仍需干净源码版本、配套 manifest、真实服务验收和目标环境批准。具体启动、构建和验证命令分别见前后端 README。每次 bundle 构建都必须先 `clean`，避免复用另一 profile 的 fat jar。

## 文档导航

- [NAMEWTA 增强说明](docs/namewta-enhancements.md)：当前能力、实现位置和前后端协作方式
- [文档导航](docs/README.md)
- [OSS 登录与浏览器直传排障](docs/error/oss-login-and-direct-upload-troubleshooting.md)
- [前端 README](frontend/README.md) / [前端架构基线](frontend/docs/architecture-baseline.md)
- [后端 README](backend/README.md)
- [发布资产](release-artifacts/README.md)：MySQL 8.4 六文件基座、初始化与部署规则

各 App、domain、web-domain、platform、adapter 和 web-kit 的局部职责由其目录 README 说明，不在本文件重复维护文件清单。

## 许可证

使用、分发与二次开发时，请遵守本仓前后端 `LICENSE` 及其依赖许可证。
