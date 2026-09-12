# Plus UI 多应用领域化架构

**应用（App）**：可独立构建、部署并选择一组领域能力的前端交付单元；每个 App 自己拥有入口、环境配置、布局、主题和组合清单。
_Avoid_: 把 App 与 OAuth Client、租户或单个页面包视为同义词

**Client 上下文（Client Context）**：由 App 注入、参与登录和服务端认证授权裁剪的 OAuth Client 身份；它不构成业务数据租户。
_Avoid_: tenant、App ID、client_pk 与 OAuth clientId 混用

**平台内核（Platform Kernel）**：不属于具体业务领域、为所有终端提供稳定合同和公共编排的最小基础层。
_Avoid_: 把通用 Vue 组件库或杂项 utils 统称为平台内核

**运行时适配器（Runtime Adapter）**：对平台端口的终端相关实现，例如浏览器 Axios、浏览器存储或未来 Taro 请求；适配器不拥有业务规则。
_Avoid_: domain service、业务 API 包装

**领域包（Domain Package）**：围绕一个前端业务能力组织的跨终端无头包，包含 API 合同、模型、应用服务、权限语义、领域消息和纯逻辑。
_Avoid_: 后端 Maven 模块镜像、Vue 页面包

**Web 领域包（Web Domain Package）**：对应领域的 Vue Web 表现层，提供页面、组件、composables 和 WebDomainManifest，并依赖领域包获得业务能力。
_Avoid_: domain、独立 App

**能力领域（Capability Domain）**：按用户能力、变化内聚性和前端消费关系划分的 bounded context；它可对应一个或多个后端模块。
_Avoid_: 强制与 ruoyi-admin、ruoyi-system 等 Maven 模块一一对应

**Web 壳层（Web Shell）**：处理布局、导航容器、主题、全局反馈和 Web 启动装配的 App 基础设施，不承载具体领域业务实现。
_Avoid_: 所有共享代码、identity-access 领域

**应用组合清单（App Composition Manifest）**：由 App 显式声明启用哪些领域、Web 领域、平台适配器和壳层能力的编译期组合合同。
_Avoid_: 运行时插件市场、服务端菜单树

**应用组合层（Application Composition Layer）**：位于 `apps/<app>/src/application`，负责实例化该 App 选择的 domain service、终端 HTTP/会话/权限适配和宿主行为；不重新声明 API 或业务模型。
_Avoid_: `src/api` 转发门面、共享 domain 实现、另一个 App 的运行时单例

**WebDomainManifest**：Web 领域包的公开注册合同，向 App 提供 component key 到懒加载视图的映射，以及该领域的路由、消息和权限相关贡献。
_Avoid_: Vite 全仓隐式 glob、后端 RouterVo

**公开入口（Public Entry）**：共享包允许跨包导入的稳定 exports 表面；包内目录结构不属于跨包合同。
_Avoid_: deep import、通过 @/ 别名越过包边界

**Source-first 包**：在同一 workspace 中直接由源码参与 App 类型检查与构建的私有包，首期不要求独立发布或预构建产物。
_Avoid_: 未发布 npm 包、运行时远程模块

**占位目录（Placeholder Directory）**：只用 README 固化未来边界、依赖规则和激活条件，但尚不声明为可构建包的目录。
_Avoid_: 空 package、已实现终端

**兼容入口（Compatibility Entry，历史术语）**：迁移期间曾用于承载旧单体调用方的临时入口；2026-08-27 已在 Admin 零兼容收口中删除，不再是允许新增的架构元素。
_Avoid_: 恢复 `src/api`、永久双写层、新架构公共 API

**选择性上游吸收（Selective Upstream Adoption）**：评估上游新增能力、修复和优化后，将其映射到本地架构边界进行增量实现，而不要求保持目录同构或整包合并。
_Avoid_: 放弃上游跟踪、无审查复制
