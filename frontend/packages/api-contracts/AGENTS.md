# API 合同索引

## Scope
`@namewta/api-contracts`。

## Purpose
保存由 OpenAPI 工具确定性生成的 TypeScript HTTP 传输合同。

## Components
生成入口为 `src/index.ts`；来源快照和生成工具位于 [tooling/openapi](../../tooling/openapi/AGENTS.md)。

`openapi/current.json` 仅保存不可变快照版本指针；对应 source 已包含 `/profile/**`。消费者在 domain 边界映射生成 transport；修改后运行正式 fetch/generate/check，不手改 generated 文件。

## Entry Points
只从包公开入口导入，导出见 [package.json](package.json)。

## Dependencies
不拥有领域模型、页面、认证策略或请求适配器；domain 必须在边界映射生成类型。

## Verification
`pnpm --filter @namewta/api-contracts typecheck`，并运行根 `pnpm openapi:check`（若由工作区脚本提供）。

## Read Next
领域映射读取对应 `packages/domains/*/AGENTS.md`。
