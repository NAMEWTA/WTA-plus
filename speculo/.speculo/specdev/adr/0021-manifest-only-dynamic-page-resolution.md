# ADR-0021: 服务端动态页面仅使用 Manifest 显式解析

- **Status:** Accepted
- **Date:** 2026-08-29
- **Source:** `2026-08-28-plus-ui-shared-navigation-permission-runtime` ADR-003、T-01、T-05、T-08

## Context

后端菜单中的 component 字符串需要在多 App 包边界下稳定解析。App 本地 `views` 全仓 glob、空 `dynamicRoutes` 和兼容双轨会绕过编译期 Web Domain 选择，使未知组件键被隐式加载，并重新引入单 App 目录耦合。显式注册增加维护步骤，但能够保持依赖、诊断和打包边界可追踪。

## Decision

服务端动态页面只能解析为 App 明确提供的特殊宿主组件，或当前 App 编译期选择的 `WebDomainManifest` 注册项。禁止 App 本地 views glob 兜底、空动态路由权限分支、旧入口别名、转发门面、功能开关或双轨实现。未知组件键和未选择领域必须产生包含稳定组件键上下文的诊断并失败关闭，不得注册或导航到猜测页面。

## Consequences

新增动态页面必须进入对应 Web Domain manifest 或明确的 App-owned manifest；不同 App 只打包各自选择的页面。Admin 登录、错误、首页和个人中心等 App 自有页面继续使用显式静态路由。迁移旧入口时必须一次性更新消费者，不能依赖运行时兼容层。
