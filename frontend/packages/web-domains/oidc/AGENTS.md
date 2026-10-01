# OIDC Web 领域索引

## Purpose

提供第三方应用创建、字段授权和配置交付页面。页面不拥有 App 会话、全局路由或请求单例。

## Entry Points

[清单与公开入口](src/index.ts)、[宿主合同](src/runtime.ts)。

## Verification

`pnpm --filter @namewta/web-domain-oidc lint`、`typecheck`、`test`、`build`。

`OidcServicePage.vue` 对应 `oidc/service`，管理 Provider 服务设置、私钥写入与退出通知重试。与 System 的外部 RP 接入配置页面分别维护。
