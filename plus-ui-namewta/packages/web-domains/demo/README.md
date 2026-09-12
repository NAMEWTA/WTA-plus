# 示例业务 Web 领域

`@namewta/web-domain-demo` 已激活，为 Admin 与 Client 提供普通列表、树表等示例页面和 manifest 注册。

本包依赖 demo domain 的公开入口，并从宿主取得权限评估、反馈、工具栏与分页等能力。它不拥有 App 布局、全局路由/Store、请求单例或后端授权。

任何 App 使用本能力时都必须显式选择 demo domain 与本 Web domain。验证覆盖组件键、查询与编辑行为、树表交互和权限失败关闭。
