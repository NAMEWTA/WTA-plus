# Admin Web 应用

## 当前状态

`@namewta/admin-web` 是已激活的后台管理浏览器应用，可独立开发、测试、构建和部署。

## 职责

- 拥有 Admin 的 ClientContext、启动流程、布局、品牌、主题、路由与 Store 适配、浏览器插件和部署配置。
- 显式组合 admin、system、workflow、demo、ai；监控能力并入 system。运行时代码生成器及其 `gen` domain/web-domain 不属于当前应用。
- 将后端菜单组件键解析为所选 Web 领域的页面，并保留少量 App 自有静态页面。

## 边界

可复用领域服务、终端无关模型、浏览器适配器合同和后端授权不属于本 App。禁止导入其他 App、包内部路径或 Taro 适配器。

## 公开入口

- `src/main.ts`：浏览器入口。
- `src/application/services.ts`：Admin 所选领域服务的唯一装配入口。
- `src/application/http.ts`：Admin 浏览器 HTTP 适配器与 401 恢复编排。
- `src/router/adminManifestRegistry.ts`：编译期领域与页面清单组合入口。
- `src/permission.ts`：受保护导航与登录态恢复。
- `src/store/modules/navigation.ts`：通过共享菜单运行时投影后端权威菜单，并维护 Admin 导航状态。

## 后端映射

通过领域合同访问 `wta-admin`、`wta-system`、`wta-workflow`、`wta-ai`、`wta-demo`、`wta-job`；后端运行时代码生成器已删除。

页面、Store 与宿主组件按需使用 `src/application/services.ts` 中的正式服务；共享请求、数据模型和业务能力归 domain，共享 Web 页面归 web-domain，布局、导航和会话恢复等应用编排仍归本 App。

## 验证

运行本包的 lint、typecheck、test、build，并通过根级架构检查、工作区构建和涉及 Admin 流程的 Playwright 验收。
