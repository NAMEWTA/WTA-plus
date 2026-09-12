# 浏览器存储适配器索引

## Scope
`@namewta/adapter-storage-browser`。

## Purpose
实现带 App 命名空间隔离的浏览器存储平台合同。

## Components
实现和隔离测试位于 `src/index.ts`、`src/index.test.ts`。

## Entry Points
只从包根公开入口导入，见 [package.json](package.json) 的 `exports`。

## Dependencies
仅依赖 `platform-contracts`；不定义令牌语义、默认会话键或业务模型。

## Verification
`pnpm --filter @namewta/adapter-storage-browser lint`、`typecheck`、`test`。

## Read Next
会话组合读取 [apps/admin-web](../../../apps/admin-web/AGENTS.md)。
