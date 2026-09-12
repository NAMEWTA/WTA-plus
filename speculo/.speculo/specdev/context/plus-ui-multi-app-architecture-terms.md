# Plus UI 多 App 架构术语

- **Sources:** `2026-08-25-plus-ui-multi-app-domain-architecture`、`2026-08-27-plus-ui-backend-aligned-domains`、`2026-08-28-plus-ui-shared-navigation-permission-runtime`
- **Graduated:** 2026-08-28

**应用（App）**：可独立构建、部署并显式选择一组 Domain、Web Domain、平台适配器和壳层能力的前端交付单元；每个 App 拥有自己的入口、环境配置和组合层。
_Avoid_: OAuth Client、tenant、单个页面包

**Client 上下文（Client Context）**：由 App 注入并参与登录、会话命名空间和服务端认证授权裁剪的 OAuth Client 身份；不构成业务数据租户。
_Avoid_: tenant、App ID、`client_pk`、OAuth `clientId` 混用

**平台内核（Platform Kernel）**：不属于具体业务模块、为所有终端提供稳定端口和公共编排的最小基础层。
_Avoid_: 通用 Vue 组件库、杂项 utils、业务 Domain

**运行时适配器（Runtime Adapter）**：平台端口的终端相关实现，例如浏览器 HTTP、存储和会话适配器；不拥有业务规则。
_Avoid_: Domain service、业务 API 合同

**领域包（Domain Package）**：与一个后端模块一级对齐的跨终端无头 npm 包，内部按 Controller 稳定 HTTP 资源组织 API、模型、mapper、service 和公开入口。
_Avoid_: Vue 页面包、一 Controller 一 npm 包、跨多个后端模块的旧语义一级包

**Controller 资源（Controller Resource）**：从 Controller 稳定 base path 提取的 kebab-case 包内目录，是后端 HTTP 资源到前端 API、类型和页面的确定性定位单元。
_Avoid_: Java 类前缀、临时方法名、npm 包粒度

**Web 领域包（Web Domain Package）**：与 Domain 同名的 Vue Web 表现层包，按相同 Controller 资源组织页面、组件和 composable，并通过 Domain 公开入口消费业务能力。
_Avoid_: 无头 Domain、独立 App

**Web 壳层（Web Shell）**：处理布局、导航容器、主题、全局反馈和 Web 启动装配的 App 基础设施，不承载具体业务模块实现。
_Avoid_: 所有共享代码、认证 Domain

**应用组合清单（App Composition Manifest）**：App 在编译期显式声明启用哪些 Domain、Web Domain、平台适配器和壳层能力的合同。
_Avoid_: 运行时插件市场、服务端菜单树

**应用组合层（Application Composition Layer）**：位于 App 内部，负责实例化该 App 选择的 Domain service、HTTP/会话/权限适配器和宿主行为；不重新声明 API 或业务模型。
_Avoid_: 转发 facade、共享 Domain 实现、另一个 App 的运行时单例

**WebDomainManifest**：Web Domain 的公开注册合同，向 App 提供 component key 到懒加载视图的映射和路由贡献。
_Avoid_: Vite 全仓隐式 glob、后端 `RouterVo`、包物理路径

**公开入口（Public Entry）**：共享包通过 `exports` 允许跨包导入的稳定表面；包内目录不属于跨包合同。
_Avoid_: deep import、通过 App 别名越过包边界

**Source-first 包**：在同一 workspace 中直接由源码参与 App 类型检查和构建的私有包，不要求独立发布或预构建。
_Avoid_: 未发布公共 npm 包、运行时远程模块

**占位目录（Placeholder Directory）**：仅用 README 固化未来终端边界、依赖规则和激活条件，尚未声明为可构建包的目录。
_Avoid_: 空 package、已实现终端

**选择性上游吸收（Selective Upstream Adoption）**：评估上游能力、修复和优化后，将其映射到本地架构边界增量实现，不要求目录同构或整包合并。
_Avoid_: 放弃上游跟踪、无审查复制、以同步为由恢复单 App 边界

**应用导航装配**：具体 App 将共享身份、权限与服务端菜单流程连接到自己的 Router、Store、布局、白名单和导航容器的薄层。
_Avoid_: 公共 Router、公共 Admin Store

**权限宿主合同**：Web Domain 页面运行所依赖、由宿主 App 提供的统一权限可见性接口；它不替代后端鉴权。
_Avoid_: 后端安全边界、Admin 私有权限算法

**Web 权限指令适配器**：把跨终端权限求值器连接成 Vue 全局 `v-hasPermi` 与 `v-hasRoles` 的 Web Kit 能力；它不读取具体 App Store。
_Avoid_: Admin 权限指令、Platform 中的 Vue 指令

**导航状态 Store**：由具体 App 拥有，保存该 App 的侧栏、顶栏、默认路由和动态路由注册投影；它不计算后端授权。
_Avoid_: Permission Store、公共 Pinia Store

**Manifest-only 动态页面**：服务端菜单中的动态组件键只能解析为特殊宿主组件或当前 App 已选择的 `WebDomainManifest` 注册项。
_Avoid_: App 本地 views 动态兜底、任意包路径加载
