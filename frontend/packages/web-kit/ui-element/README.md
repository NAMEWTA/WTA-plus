# 共享 Element Web UI

Admin、Home、SSO 的主题来源。App 显式按 Element Plus 基础 CSS、暗色 CSS、`@namewta/web-kit-ui-element/theme.css`、App 布局样式的顺序导入，并在挂载前调用 `initializeTheme()`。模块导入不自动读写 DOM 或存储。

`./auth-panel` 提供标题、描述和表单插槽；`./status-panel` 提供进度/错误标题、live region 和操作插槽。Admin/Home 登录注册、SSO 登录和两个 App 的回调页是真实消费者。品牌、登录状态、网络请求、导航、密码和会话始终由宿主拥有。

SSO 构建把同一份 `theme.css` 发布为稳定的 `/oidc-theme.css`。服务端 OIDC 退出确认页用 `.oidc-card` 消费卡片、标题和按钮样式，无需 Vue 或 Element 运行时；确认事务与 HTML 归协议端所有。

默认采用 Admin 的蓝白配色、14px 字号、14/10/6px 圆角和 12px 间距。品牌蓝为 `#409eff`，白字主操作使用 `#2b6bd3` 保证文字对比度。`html.dark` 使用同源暗色变量；初始化兼容 Admin 的 `layout-setting` 和 `useDarkKey`，新访问默认浅色，不跨 Origin 同步偏好。

`./app-shell`、`./topbar-frame`、`./sidebar-frame` 和 `./shell.css` 统一 Admin 与登录后 Home 的顶部栏、侧栏与内容布局。路由、菜单、设备状态、侧栏展开状态、页签和账户操作由 App 管理；Admin 保留原有页签、设置与多级菜单，Home 通过插槽只组合个人中心与授权任务入口。宿主内容容器使用 AppShell 默认插槽提供的 `contentClass`，避免重复 `.app-main`。

`./icon` 与 `./icons` 提供相同的离线 Tabler 图标、别名和缺失回退；宿主显式调用 `initializeIcons()`，传入本地 sprite 名称时本地图标优先。CRUD 与业务组件由对应 web-domain 拥有，本包不提供全局注册插件或通用 catch-all barrel。

执行本包 lint/typecheck/test，工作区 architecture:check，并构建三个消费 App。浏览器验证使用 `playwright.accessibility.config.ts` 覆盖重排、明暗颜色、可见焦点与键盘提交。
在 `frontend` 完成三 App 生产构建后运行 `node e2e/run-public-accessibility.mjs`：脚本创建自有临时 HTTPS 三站点、运行专用 Playwright 配置并清理证书/监听端口，截图与对比度证据保存在 `tests/e2e/reports/accessibility-evidence`。测试拦截后端接口，不宣称真实身份服务验收。

登录后布局与档案状态浏览器验收：`UI_PLAYWRIGHT_CONFIG=playwright.profile-ui.config.ts node e2e/run-public-accessibility.mjs`。该验收同样使用拦截接口，覆盖桌面/移动壳、认证通过刷新、退回重提及待办/已办任务上下文。
