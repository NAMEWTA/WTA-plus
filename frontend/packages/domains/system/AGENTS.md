# 系统管理领域索引

## Scope
`@namewta/domain-system`，对应后端 `wta-system` 的 system/resource/monitor 接口。

## Purpose
提供用户、部门、角色、菜单、字典、配置、通知、OSS、OpenAPI 和监控领域合同。

## Components
Controller 资源位于 `src/client/`、`src/user/`、`src/oss/`、`src/monitor/` 等目录；聚合服务位于 `src/service.ts`。

## Entry Points
资源子路径和根兼容入口见 [package.json](package.json)。

## Dependencies
依赖 API/platform 合同；不拥有 Vue、请求适配器、页面或后端授权。

## Verification
`pnpm --filter @namewta/domain-system lint`、`typecheck`、`test`。

## Read Next
页面实现读取 [web-domain-system](../../web-domains/system/AGENTS.md)。
