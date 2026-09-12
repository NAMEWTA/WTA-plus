# 身份与访问 Web 领域索引

## Scope
`@namewta/web-domain-admin`。

## Purpose
提供登录、注册和身份相关 Vue 页面及 manifest 注册。

## Components
页面位于 `src/auth/`；runtime、状态逻辑和 manifest 入口位于 `src/`。

## Entry Points
公开 `./auth` 和根入口，见 [package.json](package.json)。

## Dependencies
依赖 `domain-admin`、platform 和 Vue；宿主注入路由、提示、品牌与终端配置。

## Verification
`pnpm --filter @namewta/web-domain-admin lint`、`typecheck`、`test`、`build`。

## Read Next
领域合同读取 [domain-admin](../../domains/admin/AGENTS.md)。
