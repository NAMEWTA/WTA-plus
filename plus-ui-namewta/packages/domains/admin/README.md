# 身份与访问领域

## 当前状态

`@namewta/domain-admin` 已激活，当前由 Admin 使用，并作为未来终端可组合的认证与入口领域。

## 职责

拥有 ClientContext、验证码、登录、注册、社交登录、社交账号绑定、会话和登出所需的终端无关模型、映射器与服务。当前用户与菜单由 App 注入 system 端口后完成认证流程编排。

## 边界

本领域不依赖 Vue、DOM、路由器、具体 Axios/Storage 实现或 App ClientId，不拥有登录页面和 App 布局。后端仍是认证与授权权威。

## 后端映射

只对应 `wta-admin`。`auth/` 对应 `AuthController`，`captcha/` 对应 `CaptchaController`；用户信息和菜单 HTTP 调用归 `domain-system`。

## 验证

覆盖精确 ClientContext、显式 ClientId、加密边界、会话命名空间、失败关闭、登录恢复和权限评估。
