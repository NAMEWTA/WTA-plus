# 多 App 领域架构基线

本文描述 NAMEWTA 相较上游单 App 前端形成的当前架构，以及必须保持的行为边界。该分层的目标是让多个终端共享后端合同和领域能力，同时保留各 App 对布局、品牌、样式、路由选择与终端适配的控制权。

## 复用模型

```text
后端 Controller / HTTP
          ↓
domains：API、类型、领域服务、传输映射
          ↓
web-domains：Vue 页面、领域组件、hooks、manifest
          ↓
apps：Client、布局、品牌、路由与部署组合

platform ports ← adapters / web-kit 提供终端实现
```

- 共享后端 API 或数据模型时修改 domain，不在每个 App 各写一份。
- 共享 Vue 领域页面时修改 web-domain；App 定制布局、主题或静态页面时留在 App。
- 浏览器、Taro 等运行时差异通过 adapter 实现 platform port，不进入 headless domain。
- 目录按后端模块和 Controller HTTP 资源定位，不追随 Java 实现类前缀，也不使用包内部深层导入。

## 认证不变量

- 登录前必须先获取 `/auth/client/context`；`clientEnabled`、`registerEnabled` 只接受精确布尔值，缺失、畸形或请求失败均关闭后续认证动作。
- 每个已激活 App 必须使用显式 ClientId、独立环境变量与会话命名空间，不存在隐式默认 Client。
- 请求加密、Client 请求头、验证码、登录、注册和社交登录能力必须服从服务端 ClientContext。
- 登出和登录态失效必须清理当前 App 的会话，并防止跨 App 会话串用。

## 路由与权限不变量

- Admin 受保护导航按“恢复用户信息、获取菜单、解析所选 Web 领域、`addRoute`、replace 导航”的顺序执行。
- 后端菜单组件键必须解析到当前 App 已选择的 manifest；未知、重复、缺少依赖或未选择能力均失败关闭。
- `v-hasPermi`、`v-hasRoles` 与命令式权限检查使用统一访问评估语义。
- 前端菜单和按钮控制只影响可见性与交互，后端仍是最终授权边界。

## 架构不变量

- `apps/*` 只负责终端组合与交付；App 之间不得互相导入。
- `domains/*` 保持无界面、无 DOM、无浏览器实现；`web-domains/*` 不拥有 App 布局和全局单例。
- 所有工作区内部依赖必须显式声明并从公开 `exports` 导入。
- 产品源码必须位于明确的 App、domain、web-domain、platform、adapter、web-kit 或 tooling 所有权目录。
- Client Web、移动 Web、小程序与 Taro 适配器在独立规格激活前保持 README-only。

## 验证基线

根级 `architecture:check`、`architecture:test`、lint、typecheck、test、开发构建和生产构建必须通过。OpenAPI 快照变化还必须通过 `openapi:check`。涉及真实登录、菜单或权限时，增加对应 Playwright 浏览器验收；激活第二个 App 时恢复跨 App Client 与会话隔离验收。
