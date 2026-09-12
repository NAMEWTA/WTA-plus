# ADR-0012: Plus UI 使用编译期模块化 Monorepo

- **Status:** Accepted
- **Date:** 2026-08-28
- **Source:** `2026-08-25-plus-ui-multi-app-domain-architecture` ADR-001

## Context

多个前端 App 共享后台合同、团队和大部分领域能力，但需要独立构建、部署、Client 配置和表现层定制。运行时微前端会增加版本协议、远程加载、故障隔离和部署编排成本；多仓复制会造成类型、依赖和安全语义漂移。

## Decision

`plus-ui-namewta` 使用 pnpm workspace 管理的编译期模块化单体。App 通过 workspace 私有包显式组合能力并独立产出；首期不采用微前端、Module Federation、运行时远程插件或多仓复制。

## Consequences

所有 App 和共享包保留在同一前端子仓库，统一执行类型检查、依赖升级和架构门禁。代价是领域包不能任意独立发布；只有组织或发布独立性成为真实瓶颈时才重新评估微前端。
