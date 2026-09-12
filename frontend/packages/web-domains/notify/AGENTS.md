# Notify Web Domain

- 入口：`src/index.ts`，由 admin App 显式提供 domain service。
- 页面：`notify/notice/index`、`notify/monitor/index`、`notify/inbox/index`；权限统一使用 `notify:*`。
- 测试：`pnpm --filter @namewta/web-domain-notify typecheck`。
