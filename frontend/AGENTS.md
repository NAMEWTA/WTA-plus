# 前端工作区索引

## Scope
`frontend` pnpm 多 App 工作区。

## Purpose
组合可复用 domain、Web domain、平台合同和终端适配器，当前产品入口是 Admin Web、Home Web 与独立 Origin 的 SSO Web。

## Components
`apps/` 终端；`packages/domains/` 无界面领域；`packages/web-domains/` Vue 表现；`packages/platform/` 合同与运行时；`packages/adapters/` 具体适配；`packages/web-kit/` Web 机制；`tooling/` 工具。

## Entry Points
工作区脚本见 [package.json](package.json)；Admin 入口见 [apps/admin-web](apps/admin-web/AGENTS.md)，Home 入口见 [apps/home-web](apps/home-web/AGENTS.md)，SSO 入口见 [apps/sso-web](apps/sso-web/AGENTS.md)。完整事实见[项目画像](../.agents/skills/engineering-standards/references/project/00-project-profile.md)。

## Dependencies
组合方向为 `apps -> web-domains -> domains -> platform`，适配器和 web-kit 通过公开合同接入；禁止跨包深层导入。

## Verification
在 `frontend/` 执行 `pnpm architecture:check`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod`。

## Read Next
通用边界与命名读取父工作区 [namewta-fullstack-development](../.agents/skills/namewta-fullstack-development/SKILL.md)；具体包读取最近适用的索引，没有包级手册时继承父目录导航。
