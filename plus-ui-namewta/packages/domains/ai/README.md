# AI 领域

`@namewta/domain-ai` 已激活，当前拥有 `SnailAiController` 用户接入所需的终端无关模型、命令服务和传输映射。

本领域不拥有 Vue 页面、富文本展示、流式界面、App 路由、浏览器副作用或服务端授权。运行时能力通过显式端口注入。

后端映射为 `wta-ai`，资源入口为 `snail-ai/`。Admin 选择本领域，Client 当前不选择。

验证覆盖请求与响应映射、配置边界、错误处理和不依赖 Web/DOM 的架构约束。