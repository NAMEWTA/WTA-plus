# 示例业务 Web 领域索引

## Scope
`@namewta/web-domain-demo`。

## Purpose
提供普通列表、树表示例页面、局部状态和 manifest 注册。

## Components
页面位于 `src/test-demo/`、`src/test-tree/`，组合逻辑和 runtime 位于 `src/`。

## Entry Points
公开 `./test-demo`、`./test-tree` 和根入口，见 [package.json](package.json)。

## Dependencies
依赖 `domain-demo`、platform、Vue 和 Element Plus；宿主提供权限、反馈和分页能力。

## Verification
`pnpm --filter @namewta/web-domain-demo lint`、`typecheck`、`test`、`build`。

## Read Next
领域合同读取 [domain-demo](../../domains/demo/AGENTS.md)。
