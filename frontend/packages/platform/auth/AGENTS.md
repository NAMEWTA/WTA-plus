# 认证平台索引

## Scope
`@namewta/platform-auth`。

## Purpose
承载终端无关的认证端口和身份合同，供领域与 App 显式组合。

## Components
合同与实现位于 `src/index.ts`。

## Entry Points
仅从包根公开入口导入，见 [package.json](package.json)。

## Dependencies
只依赖 `platform-contracts`；不读取 App Store、浏览器全局或后端实现。

## Verification
`pnpm --filter @namewta/platform-auth lint`、`typecheck`、`test`。

## Read Next
认证领域读取 [domain-admin](../../domains/admin/AGENTS.md)。

`social-transaction.ts` 提供按 namespace/client/state 隔离的一次性事务与退出状态校验；`sha256.ts` 为认证和 OSS 指纹提供无浏览器依赖的哈希实现，兼容内网 HTTP。
