# 平台基础合同索引

## Scope
`@namewta/platform-contracts`。

## Purpose
定义跨领域、跨终端可复用的最小端口与基础类型。

## Components
合同和测试位于 `src/index.ts`、`src/index.test.ts`。

## Entry Points
仅公开包根入口，见 [package.json](package.json)。

## Dependencies
无运行时业务依赖；不得承载领域模型、Vue、DOM、浏览器存储或具体请求库。

## Verification
`pnpm --filter @namewta/platform-contracts lint`、`typecheck`、`test`。

## Read Next
依赖方向读取工作区 [AGENTS.md](../../../AGENTS.md)。
