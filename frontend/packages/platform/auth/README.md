# 认证平台合同

`@namewta/platform-auth` 定义终端无关的认证会话、ClientContext、令牌和认证流程端口。

本包不调用后端、不访问存储或 DOM、不决定页面和路由，也不拥有某个 App 的 ClientId。具体请求与存储由 adapter 实现，业务认证规则由 admin domain 编排，App 提供自身 Client 配置。

所有缺失或畸形 ClientContext 必须失败关闭；验证覆盖精确布尔值、会话隔离和显式 Client。

SSO由App注入HTTP、随机源、SHA-256、导航和临时状态存储。`startSsoLogin`保存一次性state/verifier和已校验的App内`returnTo`；`handleCallback`在换票前清除状态，拒绝重复参数、错误state和错Client的响应，失败后只能重新授权。`SsoCallbackError`只携带固定分类文案及安全路径，不转发网络请求、响应正文或凭据。

App使用`buildSsoCallbackUri(origin, contextPath)`构造包含部署前缀的回调；`safeSsoReturnTo`的结果交给当前App Router解析，不直接赋值给浏览器location。回调页应先清除地址栏中的code/state，再交换Token；重新授权从服务端重新读取当前Client配置并生成全新state/verifier。
