# ADR-0054: 公开仓 WTA-plus + 布局镜像 + 清理旧平台残留

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-012（LOG-017）

## Context

需锁定公开仓名与交付布局，并避免把旧托管钩子带进 public。

## Decision

1. 公开 monorepo 名为 **`WTA-plus`**（远程 `NAMEWTA/WTA-plus`）。
2. 目录布局对齐原聚合仓（前后端子树 + docs/speculo/release-artifacts/scripts 等）；去 submodule 默认交付。
3. 必须清理 `.gitee` 及同类旧托管/CI 残留。
4. 交付不以 upstream URL/remote 为依赖。

## Consequences

Publication checklist 含 `.gitee` 扫描与布局对照。
