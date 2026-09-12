# 认证平台合同

`@namewta/platform-auth` 定义终端无关的认证会话、ClientContext、令牌和认证流程端口。

本包不调用后端、不访问存储或 DOM、不决定页面和路由，也不拥有某个 App 的 ClientId。具体请求与存储由 adapter 实现，业务认证规则由 admin domain 编排，App 提供自身 Client 配置。

所有缺失或畸形 ClientContext 必须失败关闭；验证覆盖精确布尔值、会话隔离和显式 Client。