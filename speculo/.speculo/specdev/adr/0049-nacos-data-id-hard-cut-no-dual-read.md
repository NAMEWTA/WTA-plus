# ADR-0049: Nacos data-id 一次性硬切（无双读）

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-006（LOG-018 / t89s3）

## Context

自有 Nacos data-id 含 `ruoyi` 段会造成品牌分裂；兼容期双读已被否决。

## Decision

自有 Nacos data-id/group 中 `ruoyi` 收敛为 `wta`。**无双读**：在约定发版窗口人工迁旧→新 data-id，再切应用只认新 id。

## Consequences

须有发版 checklist、对照表、迁移步骤与回滚；禁止只改代码不迁 Nacos 内容。
