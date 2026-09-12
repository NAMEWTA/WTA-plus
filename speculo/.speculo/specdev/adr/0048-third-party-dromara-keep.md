# ADR-0048: 第三方 dromara 生态 KEEP

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-003 / evidence/THIRD-PARTY-KEEP.md

## Context

仓内引入 sms4j、warm-flow、easy-es、mica-mqtt，均以 `org.dromara.<product>` 发布。

## Decision

硬 KEEP：`org.dromara.sms4j`、`org.dromara.warm`、`org.dromara.easy-es`（运行时 `easyes`）、`org.dromara.mica-mqtt`（运行时 `mica.mqtt`）。自有包装模块可 RENAME，但其 import 的 SDK 包 KEEP。

## Consequences

验收含 KEEP 回归；**全局 `ruoyi`/`org.dromara` zero-match 不是验收标准**。
