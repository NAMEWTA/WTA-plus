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
只从 `packages/**` 的公开入口组合 admin/profile domain 和 profile self web-domain，不依赖 admin-web。

## Verification
`pnpm --filter @namewta/home-web lint`、`typecheck`、`test`、`build`。

## Read Next
终端组合与领域边界读取 [前端 Skill](../../../.agents/skills/namewta-fullstack-development/SKILL.md)；Profile 合同读取 [domain-profile](../../packages/domains/profile/AGENTS.md) 和 [web-domain-profile](../../packages/web-domains/profile/AGENTS.md)。
