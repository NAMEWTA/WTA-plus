# 系统管理 Web 领域

`@namewta/web-domain-system` 已激活，按 `client/`、`user/`、`role/`、`open-api/`、`monitor/*` 等 Controller 资源目录提供 Vue 页面、局部组件、组合式函数和 manifest。

编辑器、图片预览、字典缓存、OSS 下载和 HTML 净化通过类型化宿主端口取得。本包不拥有领域服务、App 布局、全局路由、当前会话认证或后端授权。

Admin 同时选择 system domain 与本 Web domain；Client 不选择。验证覆盖组件键和权限清单、资源加载终态、安全下载、错误可见性及真实管理流程。

OpenAPI 工作区以 `current-user` / `target-user` 判别 scope 复用同一状态机。动态页只接受显式目标用户，个人中心固定本人；一次性 AppSecret 只保留在创建/重置结果对话框，关闭即清空，不进入 URL、store 或缓存。