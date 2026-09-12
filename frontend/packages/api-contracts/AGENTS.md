# API 合同索引

## Scope
`@namewta/api-contracts`。

## Purpose
保存由 OpenAPI 工具确定性生成的 TypeScript HTTP 传输合同。

## Components
生成入口为 `src/index.ts`；来源快照和生成工具位于 [tooling/openapi](../../tooling/openapi/AGENTS.md)。

当前 `openapi/current.json` 不含 `/profile/**`，因此 Profile domain 暂以自身 HTTP 合同过渡；快照纳入 Profile 后必须通过 `tooling/openapi` 重新生成，不能手改 generated 文件。

## Entry Points
只从包公开入口导入，导出见 [package.json](package.json)。

## Dependencies
不拥有领域模型、页面、认证策略或请求适配器；domain 必须在边界映射生成类型。

## Verification
`pnpm --filter @namewta/api-contracts typecheck`，并运行根 `pnpm openapi:check`（若由工作区脚本提供）。

## Read Next
领域映射读取对应 `packages/domains/*/AGENTS.md`。
