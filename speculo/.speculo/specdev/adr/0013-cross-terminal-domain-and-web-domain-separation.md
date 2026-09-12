# ADR-0013: 分离跨终端 Domain 与 Web Domain

- **Status:** Accepted
- **Date:** 2026-08-28
- **Source:** `2026-08-25-plus-ui-multi-app-domain-architecture` ADR-002

## Context

业务请求、模型和认证权限语义需要在未来 Web、移动端和小程序之间复用，但 Vue Router、Element Plus、DOM、浏览器存储和页面组件只能服务 Web 终端。把这些内容放在同一个包会锁死终端选择。

## Decision

跨终端无头业务能力进入 `packages/domains`，Vue Web 表现能力进入 `packages/web-domains`。`platform` 定义运行时无关端口，`adapters` 提供浏览器及未来终端实现。Domain 不依赖 Vue、Router、DOM、Element Plus、浏览器存储或具体 HTTP 客户端；Web Domain 通过 Domain 公共入口消费业务能力。

## Consequences

包数量和装配成本增加，但 API、模型、权限语义与纯逻辑可以跨终端复用，Web 页面和 composable 不会污染无头层。依赖方向必须由架构检查持续约束。
