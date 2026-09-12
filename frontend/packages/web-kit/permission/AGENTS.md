# Web 权限宿主索引

## Scope
`@namewta/web-kit-permission`。

## Purpose
注册 Vue `v-hasPermi`、`v-hasRoles` 指令并委托宿主 evaluator。

## Components
指令注册和失败关闭逻辑位于 `src/index.ts`，测试位于 `src/index.test.ts`。

## Entry Points
仅公开包根入口，见 [package.json](package.json)。

## Dependencies
依赖 `platform-permission` 和 Vue；不读取 Store、Router、会话持久化或后端实现。

## Verification
`pnpm --filter @namewta/web-kit-permission lint`、`typecheck`、`test`、`build`。

## Read Next
权限语义读取 [platform-permission](../../platform/permission/AGENTS.md)。
