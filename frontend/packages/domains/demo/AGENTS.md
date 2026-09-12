# 示例业务领域索引

## Scope
`@namewta/domain-demo`，对应后端 `wta-demo`。

## Purpose
提供普通列表、树表示例的查询、命令、领域模型和传输映射。

## Components
资源位于 `src/test-demo/`、`src/test-tree/`，共享映射位于 `src/transport.ts`。

## Entry Points
公开 `./test-demo`、`./test-tree` 和根入口，见 [package.json](package.json)。

## Dependencies
依赖 API/platform 合同；不拥有 Vue 页面、分页组件、请求单例或布局。

## Verification
`pnpm --filter @namewta/domain-demo lint`、`typecheck`、`test`。

## Read Next
页面实现读取 [web-domain-demo](../../web-domains/demo/AGENTS.md)。
