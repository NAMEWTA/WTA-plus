# Notify Domain

- 入口：`src/index.ts`；HTTP 只经 `transport.ts`，领域类型不依赖 Vue、DOM 或浏览器存储。
- 依赖方向：domain -> platform-contracts/api-contracts。
- 测试：`pnpm --filter @namewta/domain-notify test`。
