# Web 上传组件索引

## Scope
`@namewta/web-kit-file-upload`。

## Purpose
提供经真实消费者验证的 Vue 文件、图片上传组件和请求归一化机制。

## Components
组件位于 `src/FileUpload.vue`、`src/ImageUpload.vue`；规范化和类型位于 `src/normalize.ts`、`src/types.ts`。

## Entry Points
公开根入口及 `./file`、`./image`、`./normalize`，见 [package.json](package.json)。

## Dependencies
依赖 platform contracts、Vue 和 Element Plus；不拥有领域流程、App Store 或 Router。

## Verification
`pnpm --filter @namewta/web-kit-file-upload lint`、`typecheck`、`test`、`build`。

## Read Next
具体 OSS 上传实现读取 [adapter-oss-upload-browser](../../adapters/oss-upload-browser/AGENTS.md)。
