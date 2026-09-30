# OIDC 领域索引

## Scope

`@namewta/domain-oidc`，对应 `wta-oidc` 管理 API。

## Purpose

拥有应用、字段授权、一次性密钥交付和 Provider 状态的无界面合同。请求通过注入的 HttpClient，响应从 unknown 验证。

## Entry Points

[公开入口](src/index.ts)；[传输服务](src/service.ts)。

## Verification

`pnpm --filter @namewta/domain-oidc lint`、`typecheck`、`test`。