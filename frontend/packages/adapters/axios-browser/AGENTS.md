# Axios 浏览器适配器索引

## Scope
`@namewta/adapter-axios-browser`。

## Purpose
在浏览器中实现平台 HTTP 合同，处理地址、Client、令牌、加密、错误和取消。

## Components
入口与请求链位于 `src/index.ts`、`src/axios-chain.test.ts`。

## Entry Points
仅从 `@namewta/adapter-axios-browser` 导入，公开入口见 [package.json](package.json) 的 `exports`。

## Dependencies
只依赖 `platform-contracts`、`platform-http` 和 Axios；不拥有领域模型或页面。

## Verification
`pnpm --filter @namewta/adapter-axios-browser lint`、`typecheck`、`test`。

## Read Next
平台合同读取 [platform/http](../../platform/http/AGENTS.md)。
