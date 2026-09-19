# NAMEWTA 前端

本目录是 WTA-plus monorepo 的前端（`frontend/`）。它把后端合同、领域规则、Web 页面和终端壳层拆分到明确的工作区包中，不再以单 App 的 `src/api + src/views + src/store` 作为复用边界。

## 核心能力

| 增强方向     | 当前实现                                                                                           | 带来的变化                                                            |
| ------------ | -------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| 多 App 交付  | `admin-web`、`home-web`、`sso-web` 均有真实入口并参与构建；发布面由 apps.json 显式登记 | 业务 App 共享领域，SSO 采用独立 Origin；Client、会话和菜单保持隔离 |
| 领域复用     | 无界面 API、类型和业务服务进入 `packages/domains/*`，Vue 页面进入 `packages/web-domains/*`         | 新 App 直接组合已有能力，不重新创建后端接口和数据模型                 |
| 前后端映射   | domain 名与后端 `admin/system/workflow/demo/profile/notify/third/ai` 模块一致，资源目录与 Controller base path 一致 | 可从 Java Controller 或 URL 快速定位前端 API、类型和页面              |
| 通用平台能力 | 认证、权限、HTTP 和运行时进入 `platform`，Axios、存储和 OSS 上传进入 `adapters`，Web 壳层进入 `web-kit` | 动态路由、权限、字典、OSS 等能力可复用，同时允许不同终端替换实现 |
| Client 安全  | App 使用显式 ClientId、独立会话命名空间和服务端 ClientContext；认证与路由失败关闭                  | 避免不同 App 之间的 Token、菜单和权限串用                             |
| 动态菜单     | App 只注册已选择 web-domain manifest 中的页面，未知或未选择的组件键拒绝解析                        | 后端菜单仍动态驱动界面，但不能越过 App 的编译期能力边界               |
| API 合同     | OpenAPI 传输合同确定性生成并检查漂移，domain 在边界处映射自己的领域模型                            | 自动生成类型可追溯，又不会把页面直接绑定到生成器内部结构              |
| 工程门禁     | 架构检查、Oxlint、TypeScript、Vitest、工作区 build 和 Playwright；Oxfmt 是写入式工具                            | 依赖方向、公开导出和关键登录/权限流程可以持续验证                     |

## 技术栈

- Vue 3、TypeScript 6、Vite 8、Pinia 4、Vue Router、Element Plus。
- pnpm 10 workspace 与 catalog 管理依赖。
- Oxlint、Oxfmt、Vitest、Playwright 和自有架构检查工具负责质量验证。
- Node.js `>=20.19.0`，pnpm `>=10.0.0`；仓库锁定版本见 `packageManager`。

## 当前架构

```text
apps/                    独立终端入口、Client、布局、品牌与组合
packages/domains/        无界面业务领域
packages/web-domains/    领域对应的 Vue Web 表现层
packages/platform/       跨领域端口与组合运行时
packages/adapters/       浏览器及未来终端适配器
packages/web-kit/        经过多消费者验证的 Web 共享机制
packages/api-contracts/  OpenAPI 生成的传输合同
tooling/                 架构、OpenAPI 与未来脚手架工具
```

当前构建包含 `admin-web` 管理端、`home-web` 用户端及独立 Origin 的 `sso-web` 认人页。Admin/Home 使用各自 Client 与会话，SSO Cookie 不与业务应用共享。三个 App 的发布配套由 [apps.json](../release-artifacts/apps.json) 和[发布说明](../release-artifacts/README.md)核对；可构建、可预览不代表已部署。移动 Web、小程序和 Taro 适配器保持有激活门槛的占位。

八个 headless domains 与后端模块一一对应：admin、system、workflow、demo、profile、notify、third、ai。每个 App 只显式组合需要的 domain/web-domain，可以独立定制布局、样式和 CSS；`gen` 已从基座物理删除，不属于运行时领域。

包内第二层按 Controller 的稳定 HTTP 资源命名。例如 `SysClientController` 的 `/system/client` 对应 `packages/domains/system/src/client/`，页面对应 `packages/web-domains/system/src/client/`；`SysUserOnlineController` 对应两侧的 `system/src/monitor/online/`。Java 的 `Sys`、`Flw` 等实现前缀不进入目录名，公开使用 package exports，禁止包间深层导入。

详细边界见 [架构基线](docs/architecture-baseline.md)、各目录 README，以及父聚合工作区 `../.agents/skills/namewta-fullstack-development/SKILL.md`。项目开发 Skill 只在父工作区集中维护，本仓库不保留 `.claude` 或 `.codex` 副本。

动态导航由 `packages/platform/app-runtime` 投影服务端菜单，`apps/admin-web` 与 `apps/home-web` 各自维护导航状态，并只解析本 App 编译期已选择的 Web manifest。Vue 权限指令由 `packages/web-kit/permission` 提供，Admin 在自己的 directive 入口注入当前会话 evaluator；Home 对用户中心认证入口执行同等权限判断，这些前端可见性机制不替代后端鉴权。

## 新 App 如何复用后端能力

新 App 不复制 Admin 的 API、Store 或页面目录。标准组合顺序是：

1. 为 App 配置独立 ClientId、会话命名空间、路由基路径和部署环境。
2. 从 `packages/domains/*` 的公开导出选择需要的 API、领域类型和服务。
3. Web App 按需选择对应 `packages/web-domains/*` manifest；非 Web 终端使用自己的表现层。
4. 通过 platform port 注入请求、存储、导航、下载和反馈等终端能力。
5. App 仅实现布局、品牌、主题、静态页面及启动/路由编排。

以 `/system/client` 为例，HTTP 与领域模型归 `packages/domains/system/src/client/`，Vue 管理页面归 `packages/web-domains/system/src/client/`，Admin 只负责选择它并将后端菜单组件键接入动态路由。其他 App 若只需要 Client 查询服务，可只组合 domain，不必引入系统管理页面。

## 开发命令

以下命令均在 `frontend/` 执行，使用 packageManager 锁定的 pnpm；命令详情以 [package.json](package.json) 为准。

各 App 的 Vite 公开参数在 `apps/<app>/.env.development` 与 `.env.production` 中跟踪，作为迁移和上线要改哪些键的清单。`VITE_*` 会进入浏览器产物，不要写入数据库、Redis 或 MinIO 密码。本机私有覆盖用未被跟踪的 `.env.*.local`。发布脚本会覆盖 `VITE_APP_CONTEXT_PATH` 与 `VITE_APP_BASE_API`。

管理端登录页默认账号为基座用户 `WTA` / `admin123`，不是上游的 `admin`。

```bash
# 安装锁定依赖
pnpm install --frozen-lockfile

# 启动 Admin Web
pnpm dev

# 架构检查与测试
pnpm architecture:check
pnpm architecture:test

# 工作区质量门禁
pnpm lint
pnpm typecheck
pnpm test

# 开发配置与生产配置构建
pnpm build:dev
pnpm build:prod
```

需要只验证单个包时使用 `pnpm --filter <package-name> <script>`。涉及登录、动态菜单或权限的变化还应运行对应 Playwright 流程，并覆盖当前 Admin/Home 的 Client 与会话隔离；SSO 和三 Origin 发布入口需使用专用验收，默认 `pnpm test:e2e` 不涵盖它们。运行入口及真实/夹具边界见[发布验证说明](../release-artifacts/README.md#nginx-请求链路)。

## 维护规则

- 产品变更进入本仓 `main`。
- 只从包的公开 `exports` 导入；禁止跨 App 导入、包深层导入和跨工作区相对导入。
- 产品源码只存在于已激活 App 和工作区包的所有权目录中。
- 前端可见性控制不是安全边界，后端始终负责最终认证和授权。

## 配套后端

后端在同仓 `backend/`，通过 HTTP 合同协作。

## 许可证

见 `LICENSE`。
