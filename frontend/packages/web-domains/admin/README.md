# 身份与访问 Web 领域

`@namewta/web-domain-admin` 已激活，为 Admin 与 Client 提供登录、注册及身份相关 Vue 表现，并通过 manifest 公开页面注册。

本包依赖 admin domain 的公开入口，通过类型化宿主运行时获取路由、提示、品牌和终端配置。它不拥有 ClientId、会话存储实现、App 布局、全局路由器或后端授权。

App 必须同时选择 admin domain 与本 Web domain。验证覆盖 ClientContext 失败关闭、登录/注册状态、manifest 注册和浏览器认证流程；第二个 App 激活后再补充真实品牌与会话隔离验收。
