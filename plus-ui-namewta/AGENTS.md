# 前端工作区索引

## Scope
`plus-ui-namewta` pnpm 多 App 工作区。

## Purpose
组合可复用 domain、Web domain、平台合同和终端适配器，当前产品入口是 Admin Web 与 Home Web。

## Components
`apps/` 终端；`packages/domains/` 无界面领域；`packages/web-domains/` Vue 表现；`packages/platform/` 合同与运行时；`packages/adapters/` 具体适配；`packages/web-kit/` Web 机制；`tooling/` 工具。

## Entry Points
工作区脚本见 [package.json](package.json)；Admin 入口见 [apps/admin-web](apps/admin-web/AGENTS.md)，Home 入口见 [apps/home-web](apps/home-web/AGENTS.md)。

## Dependencies
组合方向为 `apps -> web-domains -> domains -> platform`，适配器和 web-kit 通过公开合同接入；禁止跨包深层导入。

## Verification
`pnpm architecture:check`、`pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod`。

## Read Next
通用边界与命名读取父工作区 [namewta-fullstack-development](../.agents/skills/namewta-fullstack-development/SKILL.md)；具体包读取其同目录索引。
