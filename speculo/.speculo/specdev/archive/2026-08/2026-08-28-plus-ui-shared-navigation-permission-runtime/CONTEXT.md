# Plus UI 权限与导航运行时

**必要共享能力**：语义稳定、可以脱离具体 App 单例、已有真实消费证据，并能避免未来 App 重复实现的权限或导航能力。
_Avoid_: 为了解耦而解耦、按目录位置判断是否共享

**应用导航装配**：具体 App 将共享身份、权限与服务端菜单流程连接到自己的 Router、Store、布局、白名单和导航容器的薄层。
_Avoid_: 公共 Router、公共 Admin Store

**权限宿主合同**：Web Domain 页面运行所依赖、由宿主 App 提供的统一权限可见性接口；它不替代后端鉴权。
_Avoid_: 后端安全边界、Admin 私有权限算法

**Web 权限指令适配器**：把跨终端权限求值器连接成 Vue 全局 `v-hasPermi` 与 `v-hasRoles` 的 Web Kit 能力；它不读取具体 App Store。
_Avoid_: Admin 权限指令、平台内核中的 Vue 指令

**导航状态 Store**：由具体 App 拥有，保存该 App 的侧栏、顶栏、默认路由和动态路由注册投影；它不计算后端授权。
_Avoid_: Permission Store、公共 Pinia Store

**Manifest-only 动态页面**：服务端菜单中的动态组件键只能解析为特殊宿主组件或当前 App 已选择的 WebDomainManifest 注册项。
_Avoid_: App 本地 views 动态兜底、任意包路径加载
