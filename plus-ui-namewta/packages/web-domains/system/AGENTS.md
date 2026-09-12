# 系统管理 Web 领域索引

## Scope
`@namewta/web-domain-system`。

## Purpose
提供系统、资源和监控 Controller 对应的 Vue 页面、局部状态和 manifest。

## Components
页面资源位于 `src/client/`、`src/user/`、`src/oss/`、`src/monitor/` 等目录；宿主 runtime 位于 `src/runtime.ts`。

## Entry Points
资源子路径、`./pages` 和根入口见 [package.json](package.json)。

## Dependencies
依赖 `domain-system`、platform、Vue 和 UI 组件；不拥有领域服务、App 路由或当前会话。

## Verification
`pnpm --filter @namewta/web-domain-system lint`、`typecheck`、`test`、`build`。

## Read Next
领域合同读取 [domain-system](../../domains/system/AGENTS.md)。
