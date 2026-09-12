# App 运行时索引

## Scope
`@namewta/platform-app-runtime`。

## Purpose
提供菜单投影、manifest 组合、页面注册和结构化失败诊断的终端无关运行时。

## Components
实现与测试位于 `src/index.ts`、`src/index.test.ts`。

## Entry Points
仅公开包根入口，见 [package.json](package.json)。

## Dependencies
只依赖架构工具；不得依赖 Vue、DOM、Router、Store、App 或业务领域。

## Verification
`pnpm --filter @namewta/platform-app-runtime lint`、`typecheck`、`test`。

## Read Next
组合入口读取 [apps/admin-web](../../../apps/admin-web/AGENTS.md)。
