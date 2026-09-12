# ADR-0055: 旧库默认用户名一律迁移为 wta

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-013（覆盖 change ADR-007 旧库未定部分）

## Context

默认登录名与演示账号仍可能为 `ruoyi`，与产品品牌不一致。

## Decision

不仅新装种子：既有数据库中的默认/演示用户名 **`ruoyi` → `wta`**（品牌书面可写 WTA）。须有迁移 SQL/脚本与回滚说明；密码策略不在本 ADR。

## Consequences

登录文档、E2E 夹具与初始化脚本同步。
