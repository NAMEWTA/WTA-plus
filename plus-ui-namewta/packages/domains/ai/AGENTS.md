# AI 领域索引

## Scope
`@namewta/domain-ai`，对应后端 `wta-ai`。

## Purpose
提供 `SnailAiController` 所需的终端无关模型、命令服务和传输映射。

## Components
`src/snail-ai/` 资源、`src/transport.ts` 映射和根 module 入口。

## Entry Points
公开 `./snail-ai` 与根入口，见 [package.json](package.json)。

## Dependencies
依赖 API/platform 合同；不拥有 Vue、流式页面、路由或浏览器副作用。

## Verification
`pnpm --filter @namewta/domain-ai lint`、`typecheck`、`test`。

## Read Next
Web 表现读取 [web-domain-ai](../../web-domains/ai/AGENTS.md)。
