# ADR-0053: 前端+后端+副仓合并为单一 monorepo

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-011（FINAL for target topology；slug 见 ADR-0054）

## Context

聚合仓 + 前后端 submodule 分裂增加坐标迁移与发布成本。

## Decision

将前端、后端、副/聚合仓合并为**同一个仓库**（monorepo），不再以 git submodule 为默认交付形态。精确公开 slug 由 ADR-0054 锁定为 `NAMEWTA/WTA-plus`。

## Consequences

新仓 `.gitmodules` 策略作废；不得把「本地 checkout 仍叫 ruoyi-vue-plus-docs」解释成目标拓扑 KEEP。
