# ADR-0050: 子模块目录换前缀 ruoyi- → wta-

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-008（LOG-011 / t81u）

## Context

自有子模块物理目录与 artifactId 曾为 `ruoyi-X`；去前缀裸名会导致目录与坐标分裂。

## Decision

自有子模块目录与 artifactId：`ruoyi-X` → **`wta-X`**（小写）。不是删成无前缀的 `X`。整体仓形态见 ADR-0053/0054。

## Consequences

对照表驱动搬迁；CI/include/Docker context 全量更新；在新 monorepo 准备树完成，旧仓不动。
