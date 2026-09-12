---
schema_version: 3
artifact: spec
change: 2026-08-25-plus-ui-multi-app-domain-architecture
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-08-25-multi-app-domain-architecture
  - USER-DECISION:2026-08-25-final-plan-approved
  - ADR-001-through-ADR-007
  - "CODE:<Path>plus-ui-namewta/</Path>"
  - "CODE:<Path>docs/upstream/customization-map.md</Path>"
---

# Spec: Plus UI 多应用领域化 Monorepo 架构

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/CONTEXT.md</Path>`
- **规划深度：** Deep；原因是本 change 涉及认证授权、动态路由、共享核心路径、公共包合同、多 App 组合与渐进迁移。

## 1. 问题与目标

### 问题陈述

当前 `<Path>plus-ui-namewta/src/</Path>` 是单一 Vue Web 应用。API、类型、页面、hooks、组件、Store、请求适配、路由、认证和浏览器 UI 反馈共同依赖一个应用入口。虽然 `<Path>plus-ui-namewta/src/api/</Path>` 与 `<Path>plus-ui-namewta/src/views/</Path>` 已按 ai、demo、monitor、system、tool、workflow 分组，但这些分组尚未形成可由多个 App 独立选择、可跨终端复用、可验证依赖方向的公共模块。

未来需要多个独立前端 App 共用同一后台，并可能增加移动 Web 和小程序。若继续复制当前单体，每个 App 会重复维护 API、DTO、认证、权限和动态路由；若直接把现有 Vue 页面包当作领域包，未来非 Web 终端又会被 Vue Router、Element Plus、DOM、浏览器存储和 Axios 具体实现锁定。

本 change 要把前端演进为一个可渐进迁移的 pnpm Monorepo：跨终端业务能力进入 domains，Vue Web 表现进入 web-domains，平台合同和运行时适配器解耦终端，多个 App 在编译期显式组装所需能力。

### 目标用户与场景

- 前端平台维护者：维护认证、权限、请求、路由注册和 Monorepo 依赖规则，不再在每个 App 复制实现。
- 领域开发者：在一个能力领域内维护 API、模型、应用服务和权限语义，并通过公开入口被多个 App 使用。
- Web 产品开发者：复用领域数据和基础 Web 能力，同时为不同 App 定制布局、主题、导航和页面表现。
- App 交付维护者：为不同 OAuth Client 选择领域组合，独立构建和部署 App。
- 未来移动端/小程序开发者：复用无头领域能力，通过自己的运行时适配器和表现层实现终端，而不依赖 Element Plus Web 页面。
- 上游维护者：评估 RuoYi 上游新增能力后，将其映射到本地 platform、domain、web-domain、web-kit 或 app，而不依赖目录一一对应。

### 成功标准

1. 前端 workspace 能表达多个独立 App 和多个私有共享包，且现有管理端在兼容迁移期间持续可构建、可运行。
2. domains 与 web-domains 的依赖边界可重复验证；domains 不包含 Web 专属依赖。
3. admin-web 能组合完整领域集合；最小 client-web 能以不同 App 配置和表现层组合 identity-access 与 demo，证明共享能力不是管理端私有实现。
4. 每个 App 通过自己的 ClientContext 使用同一后台；登录、注册、社交回调、Token、401、用户信息、权限和动态菜单行为不弱化。
5. 后端菜单 component key 能经 WebDomainManifest 映射到被当前 App 选择的页面；重复或缺失映射产生明确、可诊断失败。
6. mobile-web、miniapp-taro 和未来 Taro adapter 以 README 占位表达边界，但在激活前不成为虚假可构建包、不引入依赖。
7. 迁移按可验证波次推进；旧 `<Path>plus-ui-namewta/src/</Path>` 仅在替代能力通过对应 Gate 后删除。
8. 上游变化可以按能力选择性吸收，同时继续保持 NAMEWTA Client、权限、菜单和认证不变量。

### 非目标

- 首期不采用微前端、Module Federation、运行时远程插件或多仓库独立版本协议。
- 首期不实现 Taro 小程序、原生移动 App 或 Capacitor 打包。
- 首期不发布公共 npm 包；共享包均为 workspace 私有 source-first 包。
- 不一次性重写全部页面、布局、样式或 CSS，也不要求多个 App 使用相同视觉设计。
- 不在首期自动生成全部 API Client；OpenAPI 类型生成在手工边界稳定后进入独立波次。
- 不因前端目录改造修改后端业务 API、数据库 schema 或服务端 Client 菜单裁剪语义。

## 2. 解决方案与外部行为

### 解决方案摘要

总体组合公式：

```text
App = Platform Kernel
    + Runtime Adapters
    + Web Shell（Web App 适用）
    + selected Domain Packages
    + selected Web Domain Packages（Web App 适用）
    + App-specific configuration / layout / theme / styles
```

采用编译期模块化单体 Monorepo。App 是独立交付单元，Domain Package 是跨终端业务复用单元，Web Domain Package 是 Vue Web 表现单元，Platform Kernel 定义跨终端公共端口，Runtime Adapter 实现浏览器或未来终端能力。

### 技术栈基线与目标

| 维度 | 选择 | 本 change 合同 |
|---|---|---|
| Web 框架 | Vue 3.5.40 + TypeScript 6 | 保留当前技术栈，Web App 与 web-domains 使用 Composition API 和 SFC |
| 构建 | Vite 8 | 每个激活 Web App 独立构建；共享包首期 source-first |
| 状态与路由 | Pinia 4 + Vue Router 5 | Web 壳层负责实例装配；领域规则不直接依赖具体 Router/Store 单例 |
| Web UI | Element Plus 2.14.3 | 限定在 web-domains、web-kit 和 Web App，不进入 domains/platform contracts |
| 国际化 | vue-i18n 11 | Web App 聚合消息；领域包可提供运行时无关消息键或资源数据 |
| HTTP | Axios 1 浏览器适配器 | Axios 只作为 browser adapter；domains 依赖 HttpClient 合同 |
| Monorepo | pnpm 10 workspace | 使用 `workspace:*`、catalog 和私有包；首期不增加 Nx/Turbo |
| 静态质量 | Oxlint、Oxfmt、vue-tsc | 沿用现有门禁；`pnpm fmt` 仍是写入工具，不伪装成检查门禁 |
| 测试 | Vitest + Playwright | 纯逻辑/manifest 使用单元测试；认证、Client 与动态路由使用 E2E/集成接缝 |
| 架构检查 | dependency-cruiser 或满足同一合同的轻量工具 | 在测量基线后 Ratchet 启用，禁止非法依赖、循环和 deep import |
| API 合同生成 | Springdoc/OpenAPI + openapi-typescript 候选 | 后期波次评估并引入；首期继续复用当前手工 transport types |
| 小程序/移动端 | Taro、Capacitor 仅作为候选 | 首期不安装、不固定版本、不进入构建；激活时另立规格 |

### 目标目录蓝图

以下是完整目标布局。标有 README 的未来目录在首期允许只有文档；只有被迁移波次激活的目录才获得 `package.json` 和源码。

```text
<Path>plus-ui-namewta/</Path>
├── <Path>plus-ui-namewta/apps/README.md</Path>
├── <Path>plus-ui-namewta/apps/admin-web/README.md</Path>
├── <Path>plus-ui-namewta/apps/client-web/README.md</Path>
├── <Path>plus-ui-namewta/apps/mobile-web/README.md</Path>
├── <Path>plus-ui-namewta/apps/miniapp-taro/README.md</Path>
├── <Path>plus-ui-namewta/packages/README.md</Path>
├── <Path>plus-ui-namewta/packages/platform/contracts/README.md</Path>
├── <Path>plus-ui-namewta/packages/platform/http/README.md</Path>
├── <Path>plus-ui-namewta/packages/platform/auth/README.md</Path>
├── <Path>plus-ui-namewta/packages/platform/permission/README.md</Path>
├── <Path>plus-ui-namewta/packages/platform/app-runtime/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/identity-access/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/system-admin/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/workflow/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/ai/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/demo/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/devtools/README.md</Path>
├── <Path>plus-ui-namewta/packages/domains/operations/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/identity-access/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/system-admin/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/workflow/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/ai/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/demo/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/devtools/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-domains/operations/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-kit/shell-element/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-kit/ui-element/README.md</Path>
├── <Path>plus-ui-namewta/packages/web-kit/design-tokens/README.md</Path>
├── <Path>plus-ui-namewta/packages/adapters/axios-browser/README.md</Path>
├── <Path>plus-ui-namewta/packages/adapters/storage-browser/README.md</Path>
├── <Path>plus-ui-namewta/packages/adapters/crypto-browser/README.md</Path>
├── <Path>plus-ui-namewta/packages/adapters/taro-request/README.md</Path>
├── <Path>plus-ui-namewta/packages/adapters/taro-storage/README.md</Path>
├── <Path>plus-ui-namewta/packages/api-contracts/README.md</Path>
├── <Path>plus-ui-namewta/tooling/architecture/README.md</Path>
├── <Path>plus-ui-namewta/tooling/generators/README.md</Path>
├── <Path>plus-ui-namewta/tooling/openapi/README.md</Path>
├── <Path>plus-ui-namewta/src/</Path>  # 兼容迁移入口，Wave 10 前保留
├── <Path>plus-ui-namewta/package.json</Path>
└── <Path>plus-ui-namewta/pnpm-workspace.yaml</Path>
```

每个占位 README 必须说明：当前状态、职责、明确非职责、允许依赖、禁止依赖、未来公开入口、后端能力来源、激活条件和适用验证。占位目录在激活前不得创建空 `package.json`、伪造 exports 或被根构建当成有效 App/包。

### 模块职责

| 层 | 负责 | 不负责 |
|---|---|---|
| apps | 入口、环境、ClientContext、领域选择、壳层选择、布局、主题、部署 | 可复用领域规则、共享 HTTP 实现 |
| platform | 稳定端口、认证/权限编排合同、App runtime 合同 | 具体业务领域、Element Plus 页面、Axios 实例 |
| domains | API 合同、模型、应用服务、权限语义、领域消息、纯逻辑 | Vue 页面、DOM、Router 单例、UI 消息框、浏览器存储 |
| web-domains | Vue 页面、领域组件、composables、WebDomainManifest | App 全局布局、跨终端业务规则、服务端鉴权 |
| web-kit | Web 壳层、Element Plus 基础组件、设计 tokens | 具体领域业务与后端 API |
| adapters | platform 端口的浏览器或未来终端实现 | 业务流程和领域状态 |
| api-contracts | 后期生成的 transport schema/types | 领域应用服务、页面状态、手写业务规则 |
| tooling | 架构检查、生成器和 OpenAPI 管线 | 产品运行时代码 |

一个激活后的 Domain Package 默认可包含 `api/`、`model/`、`application/`、`permissions/`、`i18n/`、`lib/` 与公开 `index.ts`。这些目录按实际职责创建，不为追求对称预建空 `hooks/`、`utils/` 或 `components/`；组件只属于 web-domain 或 web-kit。

### 依赖方向

```text
apps ───────────────> web-domains ─────> domains ─────> platform
  │                         │                │              ▲
  ├────────────────────────> web-kit ────────┘              │
  ├────────────────────────> domains                         │
  └────────────────────────> adapters ───────────────────────┘

domains ────────────> api-contracts（Wave 11 激活后，按需）
platform -X-> domains
domains -X-> web-domains / web-kit / apps / concrete adapters
web-kit -X-> domains
miniapp-taro -X-> web-domains / Element Plus web-kit
```

同层跨领域依赖不是默认权利。确需跨领域时只能导入目标领域公开入口，必须保持无环，并在包 README/架构检查中声明。web-domain 可以依赖自己的 domain；跨 web-domain 复用应下沉到 domain、web-kit 或显式公共组件边界，不通过 deep import 完成。

### 公共合同

| 合同 | 稳定职责 |
|---|---|
| HttpClient | 发送有类型请求并返回领域可消费结果，不暴露 Element Plus 或 Router |
| TokenStorage | 读写、清理认证 Token，不规定浏览器或 Taro 存储机制 |
| ClientContext | 提供当前 App 的 OAuth clientId 及经验证的 Client 公共上下文 |
| SessionService | 编排登录、登出、Token 失效和用户会话恢复 |
| AccessEvaluator | 统一角色和权限字符求值，支持 `*:*:*` 与规范超管键 |
| ErrorPresenter | 把已分类错误交给终端展示，不把 UI 框架带入请求核心 |
| NavigationPort | 表达登录跳转、回跳和受保护路由恢复，不绑定 Router 单例 |
| DomainModule | 声明领域标识、公开能力和 `backendModules` 来源 |
| WebDomainManifest | 声明 component key、懒加载视图、领域消息和 Web 注册贡献 |
| App Composition Manifest | 显式选择 domains、web-domains、adapters、shell 和 App 覆盖项 |

公共合同只通过包 exports 暴露。App 和包不得使用 `@/` 或物理相对路径越过其他包公开入口，也不得依赖另一个包的内部 `src`。

### 主要流程

#### App 编译期组装

1. App 声明 ClientContext、Runtime Adapters、Web Shell、Domain Packages 与 Web Domain Packages。
2. 构建阶段解析所有 workspace 包的公开入口。
3. App runtime 汇总 WebDomainManifest，检查领域 ID、component key、消息 namespace 和路由贡献冲突。
4. 只有当前 App 选择的 web-domain 页面进入其可注册组件表和构建图。
5. App 独立执行 typecheck、测试和生产构建并产生部署产物。

#### 登录、会话与动态路由

1. 登录和注册页读取 App 注入的 OAuth clientId，并调用 `/auth/client/context`。
2. Client 上下文只有在关键开关为精确 JSON Boolean 时可用；缺失、失败或畸形时禁止提交。
3. 登录成功后保存 Token；受保护导航先执行 `getInfo`，再执行 `getRouters`。
4. 服务端按当前 Client 裁剪菜单；前端不做二次跨 Client 菜单过滤。
5. App runtime 使用当前已选择 WebDomainManifest 的 component registry 解析后端 component key，并对非 HTTP 路由执行 `addRoute`。
6. 缺失映射、重复映射或服务端返回未启用领域页面时，产生包含 App、domain 和 component key 的诊断，不静默渲染空白页。
7. Token 失效继续执行单例重登录提示、登出和携带 redirect 的登录跳转；具体 UI 由 ErrorPresenter/Web Shell 适配。

#### 未来终端激活

1. Placeholder Directory 保持只读边界说明，不参与构建。
2. 真实业务需求出现后另立规格，确认 Taro、Capacitor、原生或其他终端技术。
3. 新终端先实现 HttpClient、TokenStorage、NavigationPort 等适配器，再组合已验证的 domains。
4. 新终端实现自己的表现层，不依赖 web-domains 或 Element Plus web-kit。

#### 选择性上游吸收

1. 查看上游镜像新增功能、修复与优化。
2. 判断变化属于 platform、domain、web-domain、web-kit、adapter 或具体 App。
3. 对照 NAMEWTA Client、认证、权限和动态菜单不变量评估冲突。
4. 在本地目标边界增量实现和验证，不以保持文件路径同构为验收目标。

### 边界、失败与稳定错误行为

- App 缺少 ClientContext 或 Client 公共上下文无效：fail-close，登录和注册不可提交，不退回全局 Client。
- 后端缺失 `clientPk`：保留服务端拒绝语义，前端不得伪造或自动选择其他 Client。
- WebDomainManifest component key 重复：App 启动或构建失败，并指出冲突 manifest；不得使用“后注册覆盖前注册”。
- 后端 component key 在当前 App registry 中不存在：导航失败必须可诊断；不得返回 `undefined` 后静默出现空白页。
- App 选择 web-domain 却未选择对应 domain：组合校验失败。
- Domain Package 引入 Vue Router、Element Plus、DOM、浏览器存储或 concrete adapter：架构检查失败。
- 出现循环依赖或 deep import：架构检查失败。
- 某迁移波次回归：保留兼容入口并停止后续波次，不通过删除旧能力或放宽门禁继续。
- 上游功能无法保持 NAMEWTA 安全合同：不吸收或另立设计决策，不静默降级。

### 状态转换与不变量

```text
placeholder
  -> activated package/app
  -> behavior-parity proven
  -> consumer migration
  -> legacy compatibility removal

任一步验证失败 -> 保持上一可运行状态并记录偏差
```

长期不变量：

- App、OAuth clientId、`clientPk` 与 tenant 是不同概念。
- 服务端授权和按 Client 菜单裁剪始终是安全权威，前端权限只控制呈现和导航装配。
- `getInfo -> getRouters -> addRoute` 顺序保持；`roles.length === 0` 的当前会话恢复语义在专门迁移前保持。
- 后端 component key 是路由映射合同，不等于包物理路径。
- `superadmin` 是规范超管角色键；兼容期允许保留存量 `admin` alias，但不得以 alias 取代规范键，也不得在无运行证据时删除兼容。
- Shared package 不依赖具体 App；Platform 不依赖 Domain；Domain 不依赖 Web/DOM/concrete adapter。
- 每个 App 可独立构建，未选择的 web-domain 不被注册为可导航页面。
- 兼容入口有删除条件，不成为永久双写架构。

## 3. 用户故事

- **US-001**：作为前端平台维护者，我希望在一个 pnpm Monorepo 中管理多个 App 和共享包，以便统一依赖、类型和质量门禁。
- **US-002**：作为管理端用户，我希望架构迁移期间现有登录、权限、动态菜单和业务页面持续可用，以便重构不影响日常工作。
- **US-003**：作为 Client App 维护者，我希望只选择本 App 需要的领域与 Web 页面，并配置独立 Client 和视觉壳层，以便复用后台而不复制管理端。
- **US-004**：作为领域开发者，我希望 API、模型和业务规则位于跨终端 Domain Package，并通过公开入口被多个 App 使用，以便消除重复实现。
- **US-005**：作为 Web 产品开发者，我希望复用 Domain Package，同时在 web-domain 或 App 中定制组件、布局和样式，以便不同产品保持独立体验。
- **US-006**：作为未来移动端或小程序开发者，我希望复用无头领域能力并实现终端适配器，以便不被 Element Plus 和浏览器 API 锁定。
- **US-007**：作为安全维护者，我希望所有 App 共用一致的 Client、认证、会话、权限和动态路由语义，以便避免跨 Client 泄漏和授权漂移。
- **US-008**：作为菜单配置维护者，我希望后端 component key 能稳定解析到当前 App 拥有的页面，并在配置不一致时获得清晰诊断。
- **US-009**：作为上游维护者，我希望按能力而非文件同构吸收上游改进，以便本地架构自主演进且不丢失安全修复。
- **US-010**：作为架构维护者，我希望空的未来目录也有明确 README 合同，以便占位表达意图但不制造虚假模块。
- **US-011**：作为迁移执行者，我希望按小步、可验证、有退出 Gate 的波次迁移，以便任何回归都能停在上一可运行状态。
- **US-012**：作为代码评审者，我希望依赖方向、公开入口和循环依赖有可重复检查，以便 Monorepo 不退化为新的耦合单体。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | Workspace 骨架激活 | 执行 pnpm workspace 依赖解析 | 根、激活 App、激活 packages、tooling 被识别；占位目录不产生虚假包 | pnpm workspace 列表与锁文件 review |
| AC-002 | admin-web 已激活 | 独立执行管理端生产构建 | 管理端产物成功生成，现有兼容功能可访问 | `pnpm build:prod` 对应 App filter + Playwright |
| AC-003 | client-web 证明阶段 | 组合 identity-access、demo、独立 ClientContext 和不同壳层 | client-web 可独立构建登录，只注册选择的领域，布局/主题无需修改共享 domain | client-web build + E2E |
| AC-004 | mobile-web、miniapp-taro 和 Taro adapters 未激活 | 扫描占位目录 | 每个目录只有边界 README 或明确文档资产，无 package.json、运行依赖和构建声明 | 文件清单与 workspace 列表 |
| AC-005 | 任一占位 README | 进行文档审查 | 包含状态、职责、非职责、允许/禁止依赖、公开入口、后端来源、激活条件和验证 | Markdown review |
| AC-006 | Domain Package 激活 | 扫描 import graph | 不依赖 Vue Router、Element Plus、DOM、浏览器存储、App 或 concrete adapter | architecture check |
| AC-007 | 任一跨包消费 | 导入领域或平台能力 | 只能从 package exports 公开入口导入，deep import 被检查拒绝 | architecture check + typecheck |
| AC-008 | 全部激活包 | 计算依赖图 | 依赖方向符合本 Spec 且无循环 | architecture check |
| AC-009 | 首批领域建立 | 检查领域清单 | identity-access、system-admin、workflow、ai、demo、devtools、operations 均有唯一职责与 backendModules 追踪 | README/manifest contract test |
| AC-010 | Web App 选择 web-domain | 汇总 WebDomainManifest | 只有已选择 manifest 的视图、消息和注册贡献进入 App runtime | manifest unit/integration test |
| AC-011 | 两个 manifest 声明同一 component key | 启动组合注册 | 组合失败并报告两个 manifest 与冲突 key，不发生覆盖 | manifest unit test |
| AC-012 | 后端返回当前 registry 不存在的 component key | 生成并添加动态路由 | 用户获得稳定失败页或错误反馈，诊断包含 App、domain 推断和 component key，不静默空白 | route integration + Playwright |
| AC-013 | 有效 Token 且尚未恢复用户信息 | 导航受保护页面 | 保持 `getInfo -> getRouters -> addRoute -> replace 当前目标` 顺序 | router/store unit + Playwright |
| AC-014 | 后端返回按 Client 裁剪的菜单 | 前端生成路由 | 前端只映射和 `addRoute`，不进行第二次跨 Client 菜单过滤 | permission store test + code review |
| AC-015 | Client 上下文请求失败、字段缺失或 Boolean 类型错误 | 用户尝试登录/注册 | 登录和注册 fail-close，不发送认证请求 | 现有 Client auth E2E 扩展 |
| AC-016 | Token 过期返回 401 | 用户选择重新登录 | 单例提示、登出、redirect 回跳语义保持；终端展示通过 ErrorPresenter 适配 | request/session unit + Playwright |
| AC-017 | 后端返回 `superadmin` 或权限 `*:*:*` | 执行按钮/角色权限求值 | 规范超管可访问授权能力；兼容期 `admin` alias 的保留/删除有显式证据 | AccessEvaluator unit + auth matrix |
| AC-018 | App 使用不同 OAuth clientId | 登录并获取菜单 | 请求携带对应 Client，服务端返回各自权限菜单，App 不共享错误会话上下文 | 多 Client Playwright matrix |
| AC-019 | Web 产品定制布局、主题或 CSS | 修改 App/web-domain 表现层 | domain API、模型和应用服务无需分叉或复制 | import graph review + two-App build |
| AC-020 | 新终端规格激活 | 组合 Domain Package 与新 adapters | 新终端不依赖 web-domain、Element Plus web-kit 或 DOM | architecture check + terminal build |
| AC-021 | 任一迁移波次完成 | 运行该波次 Gate | 适用 lint、typecheck、unit、E2E、build 均有真实结果；失败则不删除旧入口 | Gate Evidence |
| AC-022 | demo 试点未通过 manifest、API、页面和构建验证 | 准备迁移 identity-access | 高风险迁移被阻止，兼容入口保持 | Goal Plan Gate |
| AC-023 | client-web 尚未证明第二 App 组合 | 准备移动 admin-web 根入口 | 入口迁移被阻止 | Goal Plan Gate |
| AC-024 | 所有现有消费者已迁移且回归通过 | 删除兼容入口 | 删除后 admin-web/client-web 独立构建通过，不存在旧入口 import | `rg` 调用点扫描 + builds + E2E |
| AC-025 | 根依赖与版本更新 | 安装或构建 workspace | 内部依赖使用 `workspace:*`，共享版本族使用 catalog，pnpm lockfile 只有预期变化 | manifest/lockfile review |
| AC-026 | 首期工具链实施 | 检查仓库配置 | 未引入 Nx/Turbo、微前端或公共发布配置；架构检查以轻量 Ratchet 接入 | config diff review |
| AC-027 | OpenAPI 波次尚未开始 | 迁移 domain API | 手工 transport types 继续工作，不因等待代码生成阻塞前序迁移 | typecheck + API tests |
| AC-028 | OpenAPI 波次开始 | 生成 api-contracts | 生成物来源、命令和漂移检查明确；domain 业务模型不被 transport schema 反向替代 | generation check + typecheck |
| AC-029 | 评估上游新增能力 | 形成吸收变更 | 记录上游能力、目标本地边界、保留的不变量和实际验证，不以路径同构为完成条件 | customization map review |
| AC-030 | 架构检查首次接入 | 测量存量依赖偏差 | 新包/变更边界立即受约束，存量偏差有显式 Ratchet，不通过全局忽略制造绿色 | baseline report + architecture check |

## 5. 范围

### IN

- 将 `<Path>plus-ui-namewta/</Path>` 规划并渐进改造成 pnpm 多 App、多包 Monorepo。
- 创建目标 apps、platform、domains、web-domains、web-kit、adapters、api-contracts 和 tooling 的占位目录与 README 合同。
- 保留并逐步迁移现有 `<Path>plus-ui-namewta/src/</Path>`，直到新 admin-web 入口完成行为等价。
- 建立 platform 公共端口、browser adapters、DomainModule、WebDomainManifest 和 App Composition Manifest。
- 以 capability domain 而不是 Maven 模块镜像划分 identity-access、system-admin、workflow、ai、demo、devtools、operations。
- 建立 admin-web 和最小 client-web，验证多 Client、多 App、不同 UI 组合。
- 迁移登录、注册、社交回调、Token、401、权限、动态路由和 Web 壳层，同时保持安全不变量。
- 以 demo 为第一个端到端试点，以 workflow 作为首个复杂跨域迁移。
- 按需迁移现有 Web 基础组件与设计 tokens，使不同 App 能覆盖布局和样式。
- 建立 workspace、exports、catalog、边界检查、测试和构建治理。
- 后期建立 OpenAPI transport types 生成管线，并为移动/小程序激活保留明确入口。
- 更新上游 customization map，使选择性吸收能映射到新包边界。

### REUSE

- 当前 Vue、TypeScript、Vite、Pinia、Vue Router、Element Plus、vue-i18n 和 Axios 版本基线。
- 当前登录、注册、社交回调、Token、Client context、401、getInfo、getRouters、按钮权限和路由守卫行为。
- 当前 `<Path>plus-ui-namewta/src/api/</Path>` 的 HTTP/transport types 作为领域迁移来源。
- 当前 `<Path>plus-ui-namewta/src/views/</Path>`、`<Path>plus-ui-namewta/src/components/</Path>` 与 `<Path>plus-ui-namewta/src/hooks/</Path>` 的成熟页面、组件和 composable。
- 当前 Oxlint、Oxfmt、vue-tsc、Vitest、Playwright 与 Vite 门禁。
- `<Path>docs/upstream/customization-map.md</Path>` 的 NAMEWTA Client、认证、权限和动态路由合同。
- 后端现有 HTTP/JSON 合同和服务端按 Client 菜单裁剪。

### OUT

- **OOS-001**：微前端、Module Federation 与运行时远程模块；当前没有独立团队/发布协议需求支撑其复杂度。
- **OOS-002**：Taro、Capacitor 或原生移动端生产实现；本 change 只保留占位与跨端边界。
- **OOS-003**：公共 npm 发布和独立语义版本；首期使用私有 workspace source-first 包。
- **OOS-004**：一次性搬迁所有源码或全量 UI 重写；迁移必须按 Gate 渐进推进。
- **OOS-005**：统一所有 App 的布局、主题、CSS 或品牌；这些属于 App/Web 表现层定制。
- **OOS-006**：修改后端接口、数据库 schema、Client RBAC 或菜单裁剪算法；除非后续发现真实合同缺口并另立 change。
- **OOS-007**：首期自动生成全部 API Client；先稳定领域边界，再引入 api-contracts 生成。
- **OOS-008**：在没有真实职责时为每个 domain 强制创建 hooks、utils、components 等空目录。
- **OOS-009**：自动吸收所有上游提交；上游只作为选择性能力来源。

## 6. 已锁定实现约束

- **DEC-001**：采用 pnpm workspace 的编译期模块化单体，不引入运行时微前端。来源：`ADR-001`。
- **DEC-002**：跨终端 domains 与 Vue Web 专用 web-domains 分离。来源：`ADR-002`。
- **DEC-003**：platform 定义端口，adapters 实现运行时能力；依赖不得反转。来源：`ADR-002`。
- **DEC-004**：前端按 capability domain 划分，不机械镜像后端 Maven 模块。来源：`ADR-003`。
- **DEC-005**：首批领域固定为 identity-access、system-admin、workflow、ai、demo、devtools、operations。来源：`ADR-003`。
- **DEC-006**：每个 web-domain 通过 WebDomainManifest 提供显式组件注册表；后端 component key 不改成包物理路径。来源：`ADR-004`。
- **DEC-007**：App 通过 App Composition Manifest 显式选择领域，不进行运行时远程发现。来源：`ADR-001`、`ADR-004`。
- **DEC-008**：共享认证授权核心由 App 注入 ClientContext、NavigationPort 和 UI 适配；服务端授权继续是权威。来源：`ADR-005`。
- **DEC-009**：保持服务端按 Client 裁剪菜单，前端只映射并 `addRoute`。来源：`ADR-004`、`ADR-005`。
- **DEC-010**：`superadmin` 为规范超管键，`admin` 仅作为显式兼容 alias，删除必须有运行证据。来源：`ADR-005`。
- **DEC-011**：共享包首期为 private source-first，通过 `workspace:*` 和公开 exports 使用；禁止 deep import。来源：`ADR-001`。
- **DEC-012**：首期沿用 pnpm/Vite 工具链，不增加 Nx/Turbo；架构检查按基线 Ratchet 启用。来源：`LOG-009`。
- **DEC-013**：未激活的未来目录只有 README，不创建虚假包或运行依赖。来源：`LOG-005`。
- **DEC-014**：迁移保留现有兼容入口，按 12 个波次推进，替代路径通过 Gate 后才删除旧入口。来源：`ADR-006`。
- **DEC-015**：产品 main 以本地架构为权威，上游按能力选择性吸收。来源：`ADR-007`。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增前端 workspace 包公开合同，包括 platform ports、DomainModule、WebDomainManifest 和 App Composition Manifest。首期不改变后端 HTTP/JSON；后端 component 字符串继续作为稳定映射键。
- **数据模型与持久化：** 不涉及数据库和业务数据迁移。Token 与 Client 持久化的现有行为在 adapter 抽取前通过特征测试冻结；具体浏览器 storage 引擎须以源码/运行事实确认，不在 Spec 中发明。
- **兼容要求：** 现有 admin 行为、路由字段、keep-alive name、外链、redirect、404/401、登录白名单、按钮权限和 Client 安全合同在迁移期间保持。新 App 的服务端菜单必须只返回其已组合 component key；配置不一致时前端明确失败。
- **迁移要求：** 采用 expand-and-contract。先增加新包和兼容 adapter，再迁移消费者，最后以调用点扫描和全套验证删除旧入口；不得同时维护两个业务真相源。
- **发布或运维影响：** 每个激活 App 有独立构建产物、环境和 `VITE_APP_CLIENT_ID`。部署系统需要选择 App 构建目标，但仍可以指向同一后台。移动/小程序占位不产生部署产物。

### Monorepo 管理合同

`<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>` 激活后应覆盖根包、App、一级共享包、二级能力包和 tooling；允许采用等价 glob，但必须表达以下集合：

| Workspace 成员 | 项目根相对模式 |
|---|---|
| 根兼容包 | `<Path>plus-ui-namewta/</Path>` |
| App | `<Path>plus-ui-namewta/apps/*/</Path>` |
| 一级共享包 | `<Path>plus-ui-namewta/packages/*/</Path>` |
| 二级能力包 | `<Path>plus-ui-namewta/packages/*/*/</Path>` |
| Tooling | `<Path>plus-ui-namewta/tooling/*/</Path>` |

- 内部包统一以 `@namewta/` 为 scope，例如 `@namewta/app-admin-web`、`@namewta/domain-workflow`、`@namewta/web-domain-workflow`、`@namewta/platform-http`、`@namewta/adapter-axios-browser`、`@namewta/web-shell-element`。
- 内部依赖使用 `workspace:*`，共享第三方版本族使用 pnpm catalog；禁止在不同 App 重复漂移 Vue、Router、Pinia 和 UI 框架版本。
- 所有包默认 `private: true`。只有出现独立发布需求并另立决策后，才允许引入版本发布工具。
- 首期共享包直接由源码参与 App build/typecheck；只有构建性能、外部消费或发布边界形成真实需求后才增加独立产物。
- 每个包通过 `exports` 暴露公开入口，不允许消费者导入内部源码路径。
- 根脚本必须能够执行全 workspace 或按 App/package filter 执行 lint、typecheck、test、build；未激活占位不加入命令矩阵。
- 依赖和锁文件变化必须聚焦、可审查；非依赖波次不得修改 lockfile。

## 8. 非功能要求

- **NFR-001 安全与隐私：** Client 缺失或畸形时 fail-close；前端不提供跨 Client 菜单兜底；UI 权限不替代后端鉴权；Token、clientId、clientPk 和错误消息不得混用或泄露 secret。
- **NFR-002 性能与容量：** 每个 App 只注册所选 web-domain；迁移前记录当前构建产物和关键首屏基线，再采用 Ratchet 防止明显退化。本 Spec 不虚构固定 KB 或毫秒阈值。
- **NFR-003 可用性与可靠性：** manifest 重复、缺失和依赖错误必须显式失败；迁移期间保留上一可运行入口；认证和动态路由错误不得静默形成空白页。
- **NFR-004 可观测性与运营：** 组合诊断至少标识 App、domain/manifest、component key 或冲突包；构建与 Gate Evidence 记录命令、工作目录、退出码和未运行项。
- **NFR-005 可维护性：** 公开入口、依赖方向和循环依赖可重复检查；新增抽象必须对应真实复用或边界，不创建杂项 common/utils 容器。
- **NFR-006 可移植性：** domains 不依赖 Web 专属运行时；跨终端能力通过 platform ports 注入；未来终端激活前以 README 约束而不提前锁定框架版本。
- **NFR-007 兼容性：** Node 保持 `>=20.19.0`、pnpm 保持 10 系列；现有浏览器支持基线不因目录重构主动收窄。
- **NFR-008 可回滚性：** Wave 10 删除兼容入口前，每个波次可以通过保留旧消费者回退；删除后以版本控制回退整个已验证切片，不维护长期双写开关。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| Workspace/manifest 静态检查 | 架构 | AC-001、AC-004、AC-005、AC-025、AC-026 | pnpm workspace list、manifest/lockfile review | 命令输出与 diff review |
| Import graph / public exports | 架构 | AC-006、AC-007、AC-008、AC-020、AC-030 | dependency-cruiser 或等价工具；接入前先记录基线 | 架构报告 |
| Domain pure logic | 单元 | AC-006、AC-017、AC-019 | `pnpm test`，测试与包相邻 | Vitest 结果 |
| Manifest registry | 单元/集成 | AC-009、AC-010、AC-011、AC-012 | 新增稳定 registry 测试 seam | Vitest 结果 |
| Session/request/router | 单元/集成 | AC-013、AC-014、AC-016、AC-017 | 当前 permission/request/store 行为特征测试 | Vitest 结果 |
| Client 认证矩阵 | 浏览器 E2E | AC-015、AC-016、AC-018 | `<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>`；`pnpm test:e2e` | Playwright trace/report |
| admin-web 用户路径 | 浏览器 E2E | AC-002、AC-013、AC-014、AC-021、AC-024 | `pnpm test:e2e`，覆盖登录和动态路由 | Playwright report |
| client-web 组合证明 | 浏览器 E2E | AC-003、AC-018、AC-019、AC-023 | 新建第二 App 场景 | Playwright report |
| TypeScript/Vue 合同 | 静态 | AC-002、AC-003、AC-007、AC-021、AC-028 | `pnpm typecheck` | exit code + diagnostics |
| Oxlint | 静态 | AC-021、AC-024 | `pnpm lint` | exit code + diagnostics |
| App 生产构建 | 集成 | AC-002、AC-003、AC-020、AC-021、AC-024 | `pnpm build:prod` 或对应 workspace filter | Vite build output |
| OpenAPI generation | 生成合同 | AC-027、AC-028 | Wave 11 定义 generate/check 命令 | 生成漂移 diff + typecheck |
| 上游吸收映射 | 文档/评审 | AC-029 | `<Path>docs/upstream/customization-map.md</Path>` review | 文档 diff + 验证记录 |

文档、占位和 workspace-only 波次按实际风险运行最小充分验证；一旦修改前端代码或公共类型，至少运行 `pnpm lint`、`pnpm typecheck`、`pnpm test` 和 `pnpm build:prod`。触及认证、Client、权限、Router 或实际页面组合时，`pnpm test:e2e` 为 required，不得用低层单元测试替代。

## 10. 风险、假设与未决问题

### 风险

- 动态路由当前依赖单一 views glob，拆包后可能出现后端 component key 与 manifest key 不一致；通过 demo 试点和显式缺失诊断控制。
- 当前请求层聚合 Axios、加密、下载、UI、Router 和 Store，抽取时容易改变 401、重复提交、文件下载或加密行为；先建立特征测试并分 adapter 迁移。
- `superadmin` 与存量 `admin` alias 存在冲突；以服务端 `superadmin` 为规范，兼容删除必须基于角色返回值、调用点和 E2E 证据。
- workflow 合法依赖 system 用户能力，机械禁止所有跨领域依赖会破坏业务；通过公开入口和显式依赖声明治理，而不是禁止真实关系。
- source-first 包增加 Vite/TypeScript 配置复杂度；在引入独立包构建前先用两个 App 验证开发与生产构建。
- 短期双结构可能诱发新旧入口双写；每个迁移 Ticket 必须声明唯一业务真相源和兼容层删除条件。
- 上游目录与本地目录分化后，直接 cherry-pick 成功率可能下降；通过能力映射和安全热点清单控制遗漏。
- client-web 选择的领域与服务端 Client 菜单配置可能不一致；manifest 缺失必须显式失败，同时在部署验收中核对菜单键集合。

### 已采用的低影响假设

- 首个 client-web 架构证明至少组合 identity-access 与 demo；实际产品领域可在后续业务 Spec 中替换。验证方式：第二 App 独立 build/E2E 能证明组合和样式覆盖。
- 首期架构检查工具可使用 dependency-cruiser 或满足相同可执行合同的轻量替代；工具选择不改变依赖规则。验证方式：用已知非法 fixture 证明规则会失败。
- 激活领域内部只按真实职责创建目录，不强求固定空骨架。验证方式：README 与 exports review。
- OpenAPI 生成只负责 transport contract，不自动替换领域模型。验证方式：生成 diff 与 domain import review。

### 未决问题

无。

## 11. 完整迁移 Plan 与 Gate

以下计划是本 Spec 的迁移合同。Tickets 可以进一步拆分垂直切片和路径所有权，但不得改变波次依赖、退出 Gate 或安全不变量；需要改变时必须回到 Spec/ADR 处理偏差。

| Wave | 目标 | 主要产出 | 退出 Gate |
|---|---|---|---|
| 1. 行为基线 | 冻结当前认证、Client、请求、动态路由和构建行为 | 特征测试、E2E 矩阵、现有依赖/构建基线 | 当前 admin lint、typecheck、unit、E2E、build 结果已记录；已知失败分类清楚 |
| 2. 占位边界 | 创建完整目标占位目录和 README | apps、platform、domains、web-domains、web-kit、adapters、api-contracts、tooling 文档 | AC-004、AC-005 通过；无虚假 package.json 或依赖变化 |
| 3. Workspace 骨架 | 把单包配置扩展为 Monorepo，同时保留根 `src` | workspace globs、根 scripts、catalog 策略、私有包命名约定 | 现有 admin 仍按兼容入口完整通过；workspace/lockfile 审查通过 |
| 4. Platform 与 Browser Adapters | 抽取稳定端口并适配当前运行时 | contracts、http/auth/permission/app-runtime、Axios/storage/crypto browser adapters | 请求、Token、Client、401、下载/加密适用行为等价；domains 尚不依赖 UI |
| 5. Demo 垂直试点 | 用低风险完整领域证明 domain + web-domain + manifest | demo API/model/application、Vue 页面、WebDomainManifest、公开 exports | demo CRUD/页面、懒加载、keep-alive、重复/缺失 key、build 全部通过 |
| 6. 第二 App 证明 | 建立最小 client-web，证明组合而非复制 | identity-access 接线、demo、独立 Client、不同 shell/theme | admin-web 与 client-web 独立 build/E2E；共享 domain 无 App 反向依赖 |
| 7. Identity 与 Web Shell | 迁移最高复用且高风险的认证授权与壳层 | identity-access domain/web-domain、platform auth、shell-element、权限适配 | 多 Client、登录、注册、社交回调、401、getInfo/getRouters、按钮权限矩阵通过 |
| 8. Workflow 复杂领域 | 验证合法跨领域依赖和复杂页面迁移 | workflow domain/web-domain，对 system 用户公共能力的显式依赖 | workflow 关键路径、路由、用户选择与构建通过；依赖图无环 |
| 9. 其余领域 | 迁移 system-admin、ai、devtools、operations | 对应 domains/web-domains 和必要 web-kit 抽取 | 每个领域垂直行为通过；未迁移消费者继续走兼容入口且无双写 |
| 10. Admin 入口迁移与收缩 | 将根应用正式移动为 admin-web 并删除兼容入口 | admin-web 完整组合、旧入口调用点清零、兼容层删除 | AC-024 通过；admin/client 全门禁通过；回滚点和 Evidence 完整 |
| 11. OpenAPI 合同生成 | 在领域边界稳定后减少 transport types 手工漂移 | api-contracts、生成脚本、漂移检查和 owner | 可重复生成、无未解释 diff、domain model 不被 transport schema 污染 |
| 12. 移动/小程序激活 | 仅在真实需求出现时选择技术并实现终端 | 独立新 Spec、Taro/Capacitor/原生决策、终端 adapters 与 UI | 新终端不依赖 web-domains/Element Plus；独立 build/test/E2E 合同通过 |

### 跨 Wave Gates

- **Gate A：基线可信。** Wave 1 未冻结认证、Client、Router 和请求行为前，不允许抽取共享核心。
- **Gate B：架构可运行。** Wave 5 demo 未证明公开入口、manifest 和构建前，不允许迁移 identity-access。
- **Gate C：多 App 真实成立。** Wave 6 client-web 未独立通过前，不允许把现有根入口直接重命名为 admin-web 并宣告完成。
- **Gate D：安全等价。** Wave 7 全部认证与权限矩阵未通过前，不允许迁移后续 App 的共享登录核心或删除旧守卫。
- **Gate E：复杂依赖成立。** Wave 8 workflow 未证明合法公开跨域依赖前，不批量迁移其余复杂领域。
- **Gate F：兼容收缩。** 所有消费者、动态菜单键和 App 构建未核对前，不执行 Wave 10 旧入口删除。
- **Gate G：生成后置。** 手工领域边界未稳定前，不允许 OpenAPI 生成物决定 package 边界。
- **Gate H：未来终端按需。** 没有真实产品需求和独立规格时，Wave 12 保持占位状态。

### Ticket 拆分要求

- 每个 Ticket 必须形成一个可运行的垂直切片，不能只搬目录而不证明外部行为。
- shared path 必须有唯一 owner；当前 workspace 策略下实现严格串行，若 Goal Plan 选择 required worktree，则按其 parent-candidate Gate 执行。
- 修改 workspace 根配置、公共 platform 合同、App composition registry、认证核心和兼容入口删除属于高冲突共享路径，不并行写入。
- 每个 Ticket 明确 IN、REUSE、OUT、可写路径、只读路径、依赖、回滚点、验收合同映射和实际验证命令。
- 实现发现代码事实与本 Spec 冲突时停止并登记 deviation，不在 Ticket 或代码中静默重写 ADR/Spec。
