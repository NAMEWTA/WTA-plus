# Plus UI 多应用领域化架构设计日志

## LOG-001 — 2026-08-25T21:11:46+0800 — 多应用前端目标
- **设计树节点：** D-001
- **轮次与依赖：** round 1 / 无
- **状态：** confirmed
- **问题：** 同一后台如何同时支撑多个独立 Web Client，并为未来移动端和小程序保留复用空间。
- **事实与来源：** 用户确认多个 App 共享后台、API、数据模型、认证授权和基础组件，但布局、样式与产品组合可以不同；`USER-DECISION:2026-08-25`。
- **选项：** 继续复制单体前端；按 App 分叉并手工同步；建立共享平台与领域包供 App 组合。
- **推荐：** 建立共享平台、无头领域包、Web 领域包与独立 App 的组合架构，减少复制并保持终端定制自由。
- **结论：** 目标架构以多个独立 App 为交付单位，以共享能力包为复用单位；App 不等同于 Client，但每个部署可配置自己的 Client 上下文。
- **原因：** 后台合同和大部分业务语义稳定复用，真正变化主要集中在终端表现和 App 组合。
- **影响工件：** CONTEXT / ADR / Spec / Ticket / Goal Plan
- **约束或不变量：** 各 App 必须能够独立构建、部署和选择领域；不得通过复制形成长期分叉。
- **后续：** 下一阶段由 Spec 固化可观察行为与迁移验收合同。
- **替代/被替代：** 无

## LOG-002 — 2026-08-25T21:11:46+0800 — 编译期模块化 Monorepo
- **设计树节点：** D-002
- **轮次与依赖：** round 1 / D-001
- **状态：** confirmed
- **问题：** 多 App 共享采用编译期包组合还是运行时微前端。
- **事实与来源：** 当前是单一 Vue/Vite 应用，目标 App 共享同一团队、同一后台合同和大量代码；pnpm workspace 支持本地包链接，成熟开源实践展示 apps/packages 分层。`RESEARCH:<Url>https://pnpm.io/workspaces</Url>`；`RESEARCH:<Url>https://github.com/vbenjs/vue-vben-admin</Url>`。
- **选项：** 运行时微前端；独立仓库复制；pnpm Monorepo 内编译期组合。
- **推荐：** 采用编译期模块化单体 Monorepo，避免当前没有必要的运行时边界、版本编排和部署复杂度。
- **结论：** 每个 App 在构建时显式依赖所需私有包并独立产出；首期不引入微前端或远程模块。
- **原因：** 当前差异是产品组合和表现层差异，不是独立团队、独立发布协议或运行时隔离问题。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **约束或不变量：** 共享包不得依赖具体 App；运行时不得动态下载未受同一构建验证的领域模块。
- **后续：** 规模和组织边界发生实质变化时，另立架构评估。
- **替代/被替代：** 无

## LOG-003 — 2026-08-25T21:11:46+0800 — Domains 与 Web Domains 分层
- **设计树节点：** D-003
- **轮次与依赖：** round 2 / D-002
- **状态：** confirmed
- **问题：** 业务 API、模型和规则是否与 Vue/Element Plus 页面、路由和浏览器能力放在同一领域包。
- **事实与来源：** 用户要求未来可支持移动端和小程序；当前请求层同时依赖 Axios、Element Plus、Router、Pinia、浏览器 Token 和语言设施，直接复用会把非 Web 端绑定到浏览器实现；`CODE:<Path>plus-ui-namewta/src/utils/request.ts</Path>`。
- **选项：** 每个领域只有一个 Vue 包；每个终端完整复制领域代码；拆分无头 domains 与 Web 专用 web-domains。
- **推荐：** 拆分无头业务层和 Web 表现层，以端口/适配器隔离运行时依赖。
- **结论：** `domains` 保存 API 合同、模型、应用服务、权限语义、领域 i18n 数据和纯函数；`web-domains` 保存 Vue 页面、组件、composables 与 Web 路由清单。
- **原因：** 移除 Vue Router、Element Plus 和浏览器后仍有业务价值的代码才是真正跨端复用资产。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** domains 禁止依赖 Vue Router、Element Plus、DOM、浏览器存储和 Axios 具体实现；web-domains 可以依赖对应 domain。
- **后续：** Spec 定义依赖检查和包公开入口验收。
- **替代/被替代：** 无

## LOG-004 — 2026-08-25T21:11:46+0800 — 前端能力领域而非 Maven 镜像
- **设计树节点：** D-004
- **轮次与依赖：** round 2 / D-003
- **状态：** confirmed
- **问题：** 前端领域是否严格使用 ruoyi-admin、ruoyi-system、ruoyi-workflow、ruoyi-demo、ruoyi-ai 等后端模块名一一划分。
- **事实与来源：** 登录流程横跨后端 `/auth/**` 与 `/system/user/getInfo`；现有 workflow 页面合法复用 system 用户能力，tool 与 operations 也存在跨后端模块消费。机械镜像会把一个用户流程切碎或制造错误依赖。
- **选项：** 完整复制 Maven 模块；继续使用 api/views 技术目录；按前端稳定业务能力划分并记录后端来源。
- **推荐：** 使用 identity-access、system-admin、workflow、ai、demo、devtools、operations，并通过 `backendModules` 元数据保持可追踪性。
- **结论：** 前端 bounded context 由用户能力和变更内聚性决定，不由后端部署/构建模块直接决定。
- **原因：** 前后端模块服务于不同的内聚目标；一一镜像会把跨模块业务流程误建模为依赖异常。
- **影响工件：** CONTEXT / ADR / Spec / Ticket / Goal Plan
- **约束或不变量：** 领域之间只能通过公开入口依赖；合法跨域关系必须可见并受依赖规则约束。
- **后续：** 迁移前为每个首批领域建立职责、非职责和 backendModules 清单。
- **替代/被替代：** 无

## LOG-005 — 2026-08-25T21:11:46+0800 — App 组合与未来终端占位
- **设计树节点：** D-005
- **轮次与依赖：** round 3 / D-002, D-003
- **状态：** confirmed
- **问题：** 如何在不提前实现未来终端的前提下，使目录和依赖边界容纳移动端与小程序。
- **事实与来源：** 用户明确要求现在创建未来移动端/小程序占位目录，目录中可仅含解释性 README；Taro 提供多端框架但平台能力与组件并非 Web 等价，Capacitor 更适合把 Web 应用封装为原生容器。`RESEARCH:<Url>https://docs.taro.zone/en/docs/</Url>`；`RESEARCH:<Url>https://capacitorjs.com/docs</Url>`。
- **选项：** 立即初始化 Taro/Capacitor；完全不留未来边界；只建立 apps 和 adapters 的文档占位。
- **推荐：** 首期只建立 `admin-web`、`client-web`、`mobile-web`、`miniapp-taro` 及未来适配器 README，占位不伪装成可运行包。
- **结论：** mobile-web 与 miniapp-taro 在激活前只有边界文档，不创建 package.json、不安装运行时依赖、不进入构建门禁。
- **原因：** 提前固定未验证的移动技术选型会产生无效维护和错误合同，但提前命名边界能约束 Web 耦合扩散。
- **影响工件：** CONTEXT / Spec / Ticket / Goal Plan
- **约束或不变量：** README 必须说明状态、职责、非职责、允许/禁止依赖、激活条件和未来公开入口。
- **后续：** 占位创建作为最早期独立文档 Ticket。
- **替代/被替代：** 无

## LOG-006 — 2026-08-25T21:11:46+0800 — 平台端口与运行时适配器
- **设计树节点：** D-006
- **轮次与依赖：** round 3 / D-003, D-005
- **状态：** confirmed
- **问题：** HTTP、Token、Client、导航、错误呈现、存储和加密如何在不同终端复用。
- **事实与来源：** 当前 `request.ts` 聚合传输、UI、Router、Store、Token、语言与加密职责；未来 Taro 请求/存储与浏览器 API 不同。Headless 框架的 provider 模式证明核心规则可以依赖能力合同而不是 UI/运行时实现。`RESEARCH:<Url>https://refine.dev/docs/core/providers/</Url>`。
- **选项：** domains 直接使用 Axios/浏览器 API；每个 App 重写整套请求层；platform 定义端口并由 adapters 实现。
- **推荐：** platform 只定义稳定合同和编排，adapters 按运行时实现。
- **结论：** 首批合同包括 HttpClient、TokenStorage、ClientContext、SessionService、AccessEvaluator、ErrorPresenter、NavigationPort、DomainModule；浏览器实现进入 axios-browser、storage-browser、crypto-browser，Taro 实现延后。
- **原因：** 依赖倒置既保留业务复用，也允许每个终端采用适合自身的传输、导航和交互。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** platform 不反向依赖 domains；adapters 实现 platform 合同但不拥有业务规则。
- **后续：** 先以现有行为建立 characterization tests，再抽取接口，避免顺手改变 401、Token 和错误语义。
- **替代/被替代：** 无

## LOG-007 — 2026-08-25T21:11:46+0800 — WebDomainManifest 与动态路由注册
- **设计树节点：** D-007
- **轮次与依赖：** round 3 / D-003, D-004, D-005
- **状态：** confirmed
- **问题：** 拆包后如何继续解析后端返回的 component 字符串，并让不同 App 只注册自己拥有的页面。
- **事实与来源：** 当前权限 Store 通过 `import.meta.glob('./../../views/**/*.vue')` 绑定单一 `src/views` 目录；后端菜单已经按 Client 裁剪，前端守卫负责 `addRoute`。`CODE:<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>`；`CODE:<Path>plus-ui-namewta/src/permission.ts</Path>`。
- **选项：** 保留单一根 views；让后端返回包 import 路径；每个 web-domain 导出清单，由 App 建立组件注册表。
- **推荐：** 使用显式 WebDomainManifest，保留后端稳定 component key，并在 App 组合阶段汇总 view loaders、消息和权限贡献。
- **结论：** App 只注册选择的 web-domain；组件注册表必须检测重复 key 和缺失 key，服务端 component 字符串不直接等同于 npm 包路径。
- **原因：** 显式清单建立稳定公共入口，也避免 Vite glob 越过包边界和隐式打包全部页面。
- **影响工件：** CONTEXT / ADR / Spec / Ticket
- **约束或不变量：** 后端继续按 Client 裁剪菜单；前端不得再次进行跨 Client 菜单过滤；缺失组件必须产生可诊断失败。
- **后续：** demo 试点验证 manifest、懒加载、keep-alive name、404 和重复键行为。
- **替代/被替代：** 无

## LOG-008 — 2026-08-25T21:11:46+0800 — 共享认证授权与 Client 上下文
- **设计树节点：** D-008
- **轮次与依赖：** round 3 / D-005, D-006, D-007
- **状态：** confirmed
- **问题：** 多 App 如何复用登录注册、鉴权、动态路由和按钮权限，同时保持 NAMEWTA Client 安全合同。
- **事实与来源：** `VITE_APP_CLIENT_ID` 是部署级 OAuth Client 标识；后端 `getRouters` 已按 Client 裁剪；永久 ADR 已确认 Client 是认证授权上下文而非 tenant。`CODE:<Path>docs/upstream/customization-map.md</Path>`；`CODE:<Path>plus-ui-namewta/src/utils/request.ts</Path>`；`CODE:<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>`；`ADR-0001`。
- **选项：** 每个 App 复制登录流程；把 Client 当业务租户；以 identity-access 和 platform auth 共享流程并由 App 注入 ClientContext。
- **推荐：** 共享认证状态机和权限求值，App 仅提供 Client、导航和 UI 适配。
- **结论：** 登录、注册、社交回调、Token、401、getInfo、getRouters、路由注册和按钮权限形成公共能力；保持 `getInfo -> getRouters -> addRoute` 顺序和服务端授权权威。
- **原因：** 认证授权语义必须在所有 App 一致，但登录页面、壳层和错误展示允许终端定制。
- **影响工件：** CONTEXT / ADR / Spec / Ticket / Goal Plan
- **约束或不变量：** Client 缺失或非法时 fail-close；前端 UI 权限不替代后端鉴权；超管规范键以 `superadmin` 为目标，存量 `admin` 仅在验证前作为显式兼容候选。
- **后续：** Spec 必须覆盖登录、注册、社交回调、401、Client 真值边界、动态菜单和按钮权限回归。
- **替代/被替代：** 无

## LOG-009 — 2026-08-25T21:11:46+0800 — pnpm Workspace 与包边界治理
- **设计树节点：** D-009
- **轮次与依赖：** round 3 / D-002, D-004, D-005
- **状态：** confirmed
- **问题：** Monorepo 首期采用什么工具和包发布形态，并如何约束依赖方向。
- **事实与来源：** 当前项目已经使用 pnpm 10，但 `pnpm-workspace.yaml` 只包含根包；pnpm 原生提供 workspace 协议和 catalog。Nx 与 dependency-cruiser 可执行模块边界检查，Feature-Sliced Design 强调公开 API。`RESEARCH:<Url>https://pnpm.io/workspaces</Url>`；`RESEARCH:<Url>https://nx.dev/features/enforce-module-boundaries</Url>`；`RESEARCH:<Url>https://feature-sliced.design/docs/reference/public-api</Url>`。
- **选项：** 首期引入 Nx/Turbo 全套任务图；维持单包别名；pnpm 原生 workspace 加轻量边界检查。
- **推荐：** 沿用现有 pnpm/Vite 工具链，采用 source-first 私有包、`workspace:*`、catalog、显式 exports 和依赖检查，复杂编排工具延后。
- **结论：** workspace 覆盖根、apps、packages、tooling；包名使用 `@namewta/*`；禁止深导入、循环依赖和 App 反向泄漏到共享包。
- **原因：** 原生工具已能满足首期组合与版本一致性，新增大型构建系统应由规模瓶颈驱动。
- **影响工件：** CONTEXT / Spec / Ticket / Goal Plan
- **约束或不变量：** source-first 包只暴露 index/exports 公共面；未激活占位目录不得创建虚假 package.json。
- **后续：** 先测量现有 lint/typecheck/build 基线，再以 Ratchet 方式增加架构检查。
- **替代/被替代：** 无

## LOG-010 — 2026-08-25T21:11:46+0800 — 渐进式迁移与试点
- **设计树节点：** D-010
- **轮次与依赖：** round 3 / D-003, D-004, D-006, D-007, D-009
- **状态：** confirmed
- **问题：** 如何在大改目标下控制现有单体前端迁移风险。
- **事实与来源：** 当前 API、views、hooks、components、store、request 和 router 彼此存在真实耦合；动态路由、Client 和登录链路是高风险共享行为。用户允许前端大改，但未授权一次性替换运行系统。
- **选项：** 一次性移动全部目录；长期双写；以兼容入口和垂直试点逐波迁移。
- **推荐：** 先冻结行为，再创建占位和平台合同，以 demo 验证完整纵向路径，再用第二 App 证明组合能力，最后迁移高风险共享能力。
- **结论：** 迁移波次为：基线特征测试；README 占位；workspace 骨架；platform/adapters；demo 试点；client-web 证明；identity/shell；workflow；其余领域；admin-web 入口迁移；OpenAPI 生成；按需激活移动端。
- **原因：** 每个波次都形成可运行落点，能够发现错误边界而不让目录移动掩盖行为回归。
- **影响工件：** ADR / Spec / Ticket / Goal Plan
- **约束或不变量：** 现有 `src` 在兼容期继续可构建；一个波次未验证前不删除旧入口；跨域引用先显式化再收紧。
- **后续：** 进入 Spec 后把波次转为验收合同，再由 Tickets 拆成垂直切片。
- **替代/被替代：** 无

## LOG-011 — 2026-08-25T21:11:46+0800 — 上游只作为能力来源
- **设计树节点：** D-011
- **轮次与依赖：** round 3 / D-002, D-010
- **状态：** confirmed
- **问题：** 架构大幅演进后是否继续以低差异整包同步上游前端为主要目标。
- **事实与来源：** 用户明确表示未来只查看上游增加或优化的功能能力，再在本地对应代码区域吸收；前端将进行较大自主改造。`USER-DECISION:2026-08-25`。
- **选项：** 始终保持可整包合并；完全忽略上游；保留镜像和能力评估，但选择性移植到本地边界。
- **推荐：** 保留上游镜像作为变化来源和比较基线，产品 main 按本地架构演进，逐项吸收有价值能力。
- **结论：** 不再以目录同构或最小 diff 作为架构约束；上游变化需先映射到 platform、domain、web-domain、web-kit 或 app 后再实施。
- **原因：** 本地多 App 和跨端目标高于继续维持上游单 App 目录形态的便利性。
- **影响工件：** ADR / Spec / Goal Plan
- **约束或不变量：** 后端认证、Client、权限和菜单定制合同仍须在任何上游吸收中保持；上游镜像分支治理不因前端架构变化而自动废止。
- **后续：** 后续更新 `docs/upstream/customization-map.md` 时记录新的本地映射，而不是假定文件一一对应。
- **替代/被替代：** 无

## LOG-012 — 2026-08-25T21:11:46+0800 — 质量门禁、非目标与最终共识
- **设计树节点：** D-012
- **轮次与依赖：** round 4 / D-005, D-006, D-007, D-008, D-009, D-010, D-011
- **状态：** confirmed
- **问题：** 计划是否已覆盖验证边界和首期非目标，并可作为下游设计合同。
- **事实与来源：** 当前前端已有 lint、typecheck、Vitest、Playwright 和生产构建门禁；用户明确表示认可解释和方案，并确认整体计划已经完整。`CODE:<Path>plus-ui-namewta/package.json</Path>`；`USER-DECISION:2026-08-25`。
- **选项：** 继续扩展开放问题；直接开始代码改造；结束 Grill 并进入 Spec。
- **推荐：** 关闭设计树，先进入 Spec 固化外部行为、迁移验收与非功能约束，不直接实施。
- **结论：** 设计树达成 consensus。首期非目标包括微前端、运行时远程模块、公共 npm 发布、一次性 UI 重写、Taro/Capacitor 实装和全量自动 OpenAPI 生成；OpenAPI 类型生成作为后期波次评估。
- **原因：** 所有会改变架构、范围、兼容、迁移或验收的已知问题都有结论，剩余文件组织细节可由 Spec/Ticket 在既定边界内完成。
- **影响工件：** CONTEXT / ADR / Spec / Ticket / Goal Plan
- **约束或不变量：** 每个迁移波次至少通过适用的 lint、typecheck、unit、E2E、build；新架构边界检查按基线 Ratchet 启用。
- **后续：** 下一 Work 为 `<Path>{roots.workflows}/specdev/S-spec/S-spec.md</Path>`，不得自动实施产品代码。
- **替代/被替代：** 无

## LOG-013 — 2026-08-27T16:35:54+0800 — Admin 零兼容门面收口
- **设计树节点：** D-013
- **轮次与依赖：** contract follow-up / D-006, D-007, D-008, D-010
- **状态：** implemented
- **问题：** 基座完成 domain / web-domain 迁移后，Admin App 是否继续保留 `src/api`、领域页面 wrapper、全局 `$auth` 插件和旧工具兼容入口。
- **事实与来源：** 用户明确确认当前项目是新基座，无需为旧调用方保留兼容；实施前 Admin 仍有 70 余个 API 转发文件和大量领域页面 wrapper。`USER-DECISION:2026-08-27`；`CODE:<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>`。
- **选项：** 长期保留兼容门面；按 App 继续复制 API；迁移消费者后一次性删除旧入口并由架构规则禁止回流。
- **推荐：** 采用零兼容收口，App 只保留服务组合、终端 HTTP/会话/权限装配和宿主行为，正式 API、模型与领域页面分别由 domain 和 web-domain 提供。
- **结论：** 已删除 Admin `src/api`、`src/plugins`、领域页面 wrapper、旧流程组件及无调用工具；新增 `application/services.ts`、`application/http.ts`、`application/session.ts`、`application/access.ts` 和 `application/host/*`；新增 `app-api-facade` 架构规则，阻止已激活 App 重建 `src/api`。
- **原因：** 基座没有需要保护的旧消费者，继续保留转发层只会形成双入口、重复模型和错误的新 App 模板。
- **影响工件：** frontend main `735a8a9` / Skill / ELI5 / LOG
- **约束或不变量：** 新 App 直接在 `src/application` 显式组合所需 domain service；不得复制 Admin API、模型或领域页面；401、Client 上下文、动态菜单与权限仍须保持既有安全合同。
- **验证：** architecture check 30 包零违规；architecture test 95/95；全仓 lint、typecheck、unit、production build 通过；Playwright E2E 47/47。
- **后续：** 新 App 以正式 domain / web-domain 公共入口组合能力；上游变化按本地边界选择性吸收。
- **替代/被替代：** 取代 LOG-010 中“现有 `src` 在兼容期继续可构建”和“一个波次未验证前不删除旧入口”的临时迁移约束；LOG-010 其余分波验证原则继续有效。
