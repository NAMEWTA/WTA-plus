# OSS 上传适配器索引

## Scope
`@namewta/adapter-oss-upload-browser`。

## Purpose
实现浏览器 OSS 上传、分片续传和上传状态存储。

## Components
客户端、传输、指纹和续传存储位于 `src/client.ts`、`src/transport.ts`、`src/fingerprint.ts`、`src/resume-store.ts`。

## Entry Points
公开入口为 `@namewta/adapter-oss-upload-browser`，见 [package.json](package.json)。

## Dependencies
只依赖 `platform-contracts`；地址、令牌和网关由 App 注入，不拥有领域权限。

## Verification
`pnpm --filter @namewta/adapter-oss-upload-browser lint`、`typecheck`、`test`。

## Read Next
OSS 领域合同读取 [domain-system](../../domains/system/AGENTS.md)。
