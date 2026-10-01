# Home Web 索引

## Scope
应用用户门户与用户中心浏览器 App。

## Purpose
提供公开门户、用户登录注册和基于服务端菜单的档案认证中心。

## Components
`src/application/` 负责 Client、请求和会话；`src/router/` 负责 manifest 解析与动态路由；`src/layout/` 负责用户中心壳；`src/views/` 负责门户与认证入口。

## Entry Points
`package.json` 提供开发、构建、lint、typecheck 和 test 脚本；`vite.config.ts` 负责 Home Web 的环境、代理与端口。产品源码入口按 README 规划，实际文件以当前工作树为准。

## Dependencies
只从 `packages/**` 的公开入口组合 admin/profile/workflow domain、profile self/review 和 workflow task web-domain，不依赖 admin-web。登录布局复用 ui-element 框架，App 保留自己的导航、会话和菜单状态。待办/已办和审核页仍由服务端菜单授权；工作流表单通过 componentKey 映射当前客户端实际路由，已办必须传原始 taskId。

档案自助材料通过 App 注入 `uploadMaterial`，使用 OSS adapter 的私有 `general` 策略；预览、登记和移除引用经过 Profile owner 接口，不授予通用 OSS 管理查询、下载或删除权限。

## Verification
`pnpm --filter @namewta/home-web lint`、`typecheck`、`test`、`build`。

## Read Next
终端组合与领域边界读取 [前端 Skill](../../../.agents/skills/namewta-fullstack-development/SKILL.md)；Profile 合同读取 [domain-profile](../../packages/domains/profile/AGENTS.md) 和 [web-domain-profile](../../packages/web-domains/profile/AGENTS.md)。

Home 使用配置驱动的标准 OIDC 第三方登录，与 Admin 复用 `/social-callback` 页面和会话代次检查；首次登录需要手机号时补填，冲突引导登录原账号后到静态 `/account/bindings` 绑定。本地登录注册保持，`/logout/callback` 只显示退出结果，不自动登录。旧 `/sso/callback` 保留处理已有第一方协议回调，旧第一方授权发起不再作为产品入口。
