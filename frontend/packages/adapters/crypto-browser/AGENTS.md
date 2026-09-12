# 浏览器加密适配器索引

## Scope
`@namewta/adapter-crypto-browser`。

## Purpose
实现浏览器端加解密平台能力，配置由 App 显式提供。

## Components
实现和往返测试位于 `src/index.ts`、`src/index.test.ts`。

## Entry Points
只使用包根公开入口；具体导出见 [package.json](package.json)。

## Dependencies
依赖 `platform-contracts` 和加密库；不选择密钥、Client 或业务策略。

## Verification
`pnpm --filter @namewta/adapter-crypto-browser lint`、`typecheck`、`test`。

## Read Next
组合方读取 [apps/admin-web](../../../apps/admin-web/AGENTS.md)。
