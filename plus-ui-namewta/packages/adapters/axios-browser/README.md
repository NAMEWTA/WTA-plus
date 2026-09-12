# Axios 浏览器请求适配器

`@namewta/adapter-axios-browser` 已激活，在浏览器中实现平台 HTTP 合同，并接入 App 提供的基础地址、Client、令牌、加密、错误与失效处理。

它不拥有领域模型、业务错误语义、页面、路由或默认 Client。禁止依赖 App 内部和 web-domain；所有终端差异通过构造参数或平台端口注入。

验证覆盖请求头、会话注入、加密边界、错误映射、取消和多 App 配置隔离。
