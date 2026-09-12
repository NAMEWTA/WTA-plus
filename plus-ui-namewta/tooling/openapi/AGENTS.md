# OpenAPI 工具索引

## Scope
`@namewta/tooling-openapi` 私有工作区工具。

## Purpose
获取、校验、生成和检查 `api-contracts` 的确定性 OpenAPI 快照与 TypeScript 合同。

## Components
CLI 在 `src/cli.mjs`，核心实现和测试在 `src/index.mjs`、`test/`。

## Entry Points
命令 `namewta-openapi`，脚本和 exports 见 [package.json](package.json)。

## Dependencies
只负责合同生成，不进入产品运行时；生成结果由 `packages/api-contracts` 消费。

## Verification
`pnpm --filter @namewta/tooling-openapi lint`、`typecheck`、`test`、`openapi:check`。

## Read Next
合同包索引见 [api-contracts](../../packages/api-contracts/AGENTS.md)。
