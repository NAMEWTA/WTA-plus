# ADR-0014: 前端 Domain 一级边界对齐后端模块

- **Status:** Accepted
- **Date:** 2026-08-28
- **Source:** `2026-08-27-plus-ui-backend-aligned-domains` Spec DEC-001..005

## Context

前端领域一级目录曾混合使用业务能力名、管理端用途名和后端模块名，导致开发者从 Maven 模块、Controller base path 或动态菜单 component key 无法确定定位 API、模型和页面。

## Decision

一个后端模块对应一个 Domain npm 包；去掉 `ruoyi-` 前缀后的 `admin`、`system`、`gen`、`workflow`、`demo`、`ai` 是当前 canonical 一级标识。一个 Controller 稳定 HTTP 资源对应包内一个 kebab-case 资源目录，Web Domain 使用同名模块和资源目录。包粒度停留在后端模块级，不创建一 Controller 一包；跨模块流程通过公开端口注入组合。

## Consequences

后端模块、HTTP 资源和前端实现可以确定性互相定位，代价是包边界不再完全按前端用例内聚。所有跨模块依赖必须通过显式 exports，禁止深层导入和旧语义包 facade；迁移前的 identity-access、system-admin、devtools、operations 一级包只保留在历史归档中。
