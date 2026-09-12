# ADR-0046: 自有产品表面 ruoyi → wta

- **Status:** Accepted
- **Date:** 2026-09-12
- **Source:** `2026-09-12-rename-ruoyi-dromara-to-wta` ADR-001

## Context

NAMEWTA 增强版前端/Docker 已多用 `namewta`，后端模块、artifactId、品牌与容器路径仍充斥 `ruoyi`，造成双品牌。

## Decision

凡**项目自有**的 `ruoyi`/`RuoYi`/`RUOYI` 产品命名（模块目录、artifactId、自有配置键产品段、品牌标题、容器内自有工作目录等）统一迁到 `wta`/`WTA`。前端 npm 范围继续 `@namewta`，不强制 `@wta`。

命名分裂：名型 `ruoyi`→`wta`（本 ADR）；自有 brand token `dromara`→`org.namewta`（ADR-0047）；第三方 `org.dromara.<product>` KEEP（ADR-0048）。

## Consequences

按 inventory/ownership 迁移；禁止全库盲 `s/ruoyi/wta/`；上游溯源叙述可保留「基于 RuoYi-Vue-Plus」，不作运行时坐标。
