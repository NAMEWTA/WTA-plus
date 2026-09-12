# AI Web 领域

`@namewta/web-domain-ai` 已激活，当前在 `snail-ai/` 提供嵌入式 AI 会话页面和 manifest。

本包只依赖 AI domain 与必要的公开 Web/平台合同；流式交互、反馈、下载和导航通过宿主端口取得。它不拥有 App 布局、全局请求/路由单例或后端授权。

Admin 同时选择 AI domain 与本 Web domain；Client 不选择。验证覆盖组件键、权限、加载/错误终态、交互状态和生产构建。