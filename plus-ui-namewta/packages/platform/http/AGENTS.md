# HTTP 平台合同索引

## Scope
`@namewta/platform-http`。

## Purpose
定义请求、响应、错误、取消和拦截的终端无关合同。

## Components
合同和测试位于 `src/index.ts`、`src/index.test.ts`。

## Entry Points
仅从包根公开入口导入，见 [package.json](package.json)。

## Dependencies
依赖 `platform-contracts`；不依赖 Axios、Taro、浏览器全局或业务 DTO。

## Verification
`pnpm --filter @namewta/platform-http lint`、`typecheck`、`test`。

## Read Next
浏览器实现读取 [adapter-axios-browser](../../adapters/axios-browser/AGENTS.md)。
