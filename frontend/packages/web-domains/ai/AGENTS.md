# AI Web 领域索引

## Scope
`@namewta/web-domain-ai`。

## Purpose
在 `snail-ai/` 提供嵌入式 AI 会话页面和 manifest。

## Components
页面、流式状态、runtime 和 registration 位于 `src/snail-ai/` 与 `src/`。

## Entry Points
公开 `./snail-ai`、`./pages` 和根入口，见 [package.json](package.json)。

## Dependencies
依赖 `domain-ai`、platform 和 Vue；不拥有 App 布局、全局请求/路由单例或授权。

## Verification
`pnpm --filter @namewta/web-domain-ai lint`、`typecheck`、`test`、`build`。

## Read Next
领域合同读取 [domain-ai](../../domains/ai/AGENTS.md)。
