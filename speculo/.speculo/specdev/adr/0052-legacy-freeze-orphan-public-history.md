# ADR-0052: 原仓冻结 + 新 public 仓 orphan 历史

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-010（FINAL for history/freeze/sequencing）

## Context

旧三仓带上游与双品牌历史；在原 remote 上 filter-repo 会破坏协作者 clone 与溯源备份。

## Decision

1. **Freeze** 旧三仓 remote（`ruoyi-vue-plus-namewta`、`plus-ui-namewta`、`ruoyi-vue-plus-docs`）：禁止默认主线新功能/rename、force-push、history rewrite；mutation 仅在 `legacy_repo_mutation_authorized`。
2. **Orphan**：新仓以无旧三仓 Git 祖先的干净历史发布；不得拷入旧 `.git`/构建缓存。
3. **Sequencing**：inventory →（implementation_authorized）staged rename → verify → publication readiness →（public_repo_publication_authorized）orphan push →（legacy_repo_mutation_authorized）freeze transition。**禁止 publication gate 前公开 push。**

## Consequences

未授权不得创建/push 新仓；ORPH 验收见源 change ADR-010。
