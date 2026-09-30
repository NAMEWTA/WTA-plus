# 共享 Element Web UI 索引

## Scope

`@namewta/web-kit-ui-element`。

## Purpose

为 Admin、Home、SSO 提供同源设计变量、显式主题初始化、身份页面展示组件和登录后布局框架。

## Components

`src/theme.css` 为主题来源，`src/theme.ts` 处理宿主显式调用，AuthPanel 与 StatusPanel 只承载展示和插槽；AppShell、TopbarFrame、SidebarFrame 与 shell.css 共享布局尺寸，SvgIcon 共享离线 Tabler 图标及本地 sprite 优先规则。

## Entry Points

公开子路径见 [package.json](package.json)，消费与样式顺序见 [README.md](README.md)。

## Dependencies

依赖 Vue 与 Iconify/Tabler；不拥有领域服务、路由、App Store、品牌、密码或 Cookie。不得反向导入 App。

## Verification

运行本包 lint、typecheck、test 与三个消费 App 构建；公共登录页面另运行浏览器可访问性验收。

## Read Next

主题消费入口位于三个 App 的 main.ts；遵循 [前端工作区索引](../../../AGENTS.md)。
