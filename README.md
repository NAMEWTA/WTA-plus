# NAMEWTA WTA-Plus

本项目是 NAMEWTA 增强版后台与多 App 前端的单一 monorepo（公开仓名 **WTA-plus**）。它不是简单改名或上游源码副本，而是在保留上游主要业务能力的基础上，重点增强了多 App 前端架构、Client 级身份与权限隔离、OSS 直传与生命周期、统一通知、HTTP 可观测性及工程治理。

溯源：基于 RuoYi-Vue-Plus / Plus-UI；交付不以 git submodule 或上游 URL 为依赖。

| 子树 | 职责 | 详细说明 |
|---|---|---|
| `WTA-plus`（本仓） | 聚合文档、工程规范、发布资产与前后端同源 | 当前 README 与 [文档导航](docs/README.md) |
| `frontend` | Vue 3 多 App 领域化前端 | [前端 README](frontend/README.md) |
| `backend` | Spring Boot 模块化后端（`org.namewta` / `wta-*`） | [后端 README](backend/README.md) |

## 相较上游的核心增强

| 方向 | NAMEWTA 增强 | 直接收益 |
|---|---|---|
| 多 App 前端 | 将单体前端重构为 `apps + domains + web-domains + platform + adapters + web-kit` 的 pnpm monorepo | Admin 当前独立交付，未来 Client/移动端/小程序可按需组合能力而不重复实现 API 与数据模型 |
| 前后端定位 | 前端 domain 按 `admin/system/workflow/demo/profile/notify/third/ai` 后端模块组织，第二层按 Controller HTTP 资源命名 | 从 Controller、URL 到前端 API、类型、页面都有稳定查找路径 |
| Client 身份域 | Client 可独立配置登录域、注册开关、默认角色和用户归属；密码、短信、邮件、社交、小程序登录统一执行准入 | 一套后端可安全服务多个产品入口和用户群体 |
| RBAC 与会话 | 角色、菜单、按钮权限、动态路由、默认角色和会话按 `userId + Client 主键` 计算 | 防止跨 App 菜单、权限、Token 和会话串用 |
| OSS | 浏览器直传对象存储，支持单文件、分片、断点续传、失败恢复、对象引用、临时文件清理、可恢复删除和授权下载 | 大文件不再经过应用服务器转发，并补齐对象全生命周期治理 |
| 通知 | 渠道无关的通知分发，提供邮件/短信适配、Redis 幂等、OSS 附件快照、调用上下文审计、脱敏和全局投递监控 | 业务模块通过统一合同发送和追踪通知 |
| HTTP 可观测性 | 在 Servlet 边界输出可关联的请求/响应结构化事件，覆盖同步、异步、异常、正文截断和媒体类型策略 | 无需为每个接口重复编写基础访问日志，可按 requestId 串联一次调用 |
| 数据变更 | 父仓库统一维护六份 MySQL 8.4 完整初始化基座（三方菜单 DML 已并入 60），业务迭代直接修改对应基座文件 | 全新环境可确定性初始化，已有环境按源/目标 Git Tag 评审差异后升级 |
| 工程治理 | 前后端产品分支与上游镜像分离，固定基线，维护定制边界、架构检查、OpenAPI 漂移检查和分层测试 | 可吸收上游能力，同时避免覆盖 NAMEWTA 的核心改造 |

完整能力、实现位置和边界见 [NAMEWTA 增强说明](docs/namewta-enhancements.md)。

## 整体架构

```text
WTA-plus/
├── .agents/skills/                    # 唯一项目开发 Skill 根目录
├── frontend/                 # 前端（合入，非 submodule）
│   ├── apps/                        # 可独立构建、部署的终端 App
│   └── packages/                    # 领域、平台、适配器和 Web 共享包
├── backend/          # 后端（合入，非 submodule）
│   ├── wta-admin/                 # 服务启动与模块组装
│   ├── wta-api/                   # 跨模块公开合同
│   ├── wta-common/                # 通用基础能力
│   └── wta-modules/               # system/workflow/demo/profile/notify/ai/job/third
├── docs/                            # 当前架构与上游治理文档
├── release-artifacts/               # 发布资产及六份 MySQL 8.4 初始化基座
├── scripts/                         # 聚合 CI / 开发脚本
└── speculo/                         # 规格驱动研发状态
```

前后端通过 HTTP/JSON 合同协作并独立构建、测试和发布。前端 App 只负责 Client、布局、品牌和终端组合；可复用 API、类型、业务规则与 Web 页面分别归 domain 和 web-domain。后端 `wta-admin` 只负责组装，跨业务模块通过 `wta-api` 或明确的 common SPI 协作。

项目开发 Skill 统一位于 [`.agents/skills`](.agents/skills)，由 [工程规范](.agents/skills/engineering-standards/SKILL.md)负责规则与质量门禁裁决，再按任务路由到前端、后端和具体模块 Skill。本 monorepo 不维护工具专属的 `.claude` 或 `.codex` Skill 副本。

## 当前终端

| 终端 | 状态 | 说明 |
|---|---|---|
| `admin-web` | 已激活 | 完整后台管理端，组合八个业务领域及动态菜单权限 |
| `home-web` | 已激活 | 用户门户与用户中心，组合登录、注册和档案认证流程 |

新增 App 时不复制 `admin-web/src/api`、业务类型或领域页面。App 从公开包入口选择所需 domain/web-domain，再提供本终端的请求、存储、加密、布局和路由适配。

前端动态导航由 `packages/platform/app-runtime` 投影当前 Client 的服务端菜单，Admin 的 `navigation` Store 只维护 App 自有导航状态，页面解析严格使用已选择的 web-domain manifest。Vue 权限指令由 `packages/web-kit/permission` 提供，Admin 入口注入当前会话 evaluator；菜单和按钮可见性始终不替代后端鉴权。

## 获取项目

本仓是单一 monorepo（前后端与文档合入，**不是** git submodule 交付）：

```bash
git clone https://github.com/NAMEWTA/WTA-plus.git
cd WTA-plus
```

前端在 `frontend/`，后端在 `backend/`。前端要求 Node.js `>=20.19.0`、pnpm `>=10.0.0`；后端要求 Java 21，并通过仓库内 Maven Wrapper 构建。具体启动、构建和验证命令分别见前后端 README。

## 分支与上游关系

- 本仓 `main` 是 NAMEWTA 产品分支；默认交付不再使用 git submodule。
- 上游 RuoYi-Vue-Plus / Plus-UI 仅作能力发现；不要求本地目录与上游路径同构，也不把上游 URL 当作运行依赖。
- 旧三仓 `ruoyi-vue-plus-namewta` / `frontend` / `ruoyi-vue-plus-docs` 按 freeze 语义只读备份，不在本仓内 `gitlink` 推进。

长期约束见 [上游能力治理](docs/upstream/README.md)与 [定制边界](docs/upstream/customization-map.md)。历史变化直接通过本仓 Git 日志查看，文档只维护当前有效状态。

## 许可证与上游

NAMEWTA 保留上游项目许可证和署名。使用、分发与二次开发时，请同时遵守前后端仓库中的许可证及其依赖许可证。
