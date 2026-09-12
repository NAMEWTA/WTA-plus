# ADR-0051: 自有种子与示例字面量 ruoyi → wta

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-009

## Context

SQL seed、YAML 示例与文档中存在自有品牌/租户/bucket/示例账号字面量 `ruoyi`。

## Decision

凡属**自有**品牌/租户/bucket/示例账号的 `ruoyi` 字面量一并改为 `wta`（或统一 `wta-` 前缀规则）。上游产品名与第三方坐标 KEEP。

## Consequences

改前应用 ownership inventory；禁止盲替换。
