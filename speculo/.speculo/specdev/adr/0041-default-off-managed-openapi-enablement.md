# ADR-0041: 默认关闭并受管启用 OpenAPI

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-admin-runtime-capability-reconciliation/ADR.md</Path>` ADR-001
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-harden-namewta-full-stack-deployment-skill/evidence/T-04.md</Path>`

## Context

OpenAPI 引入机器身份、长期凭据、全局权限聚合和 KEK 管理。为修复单一受管环境而默认开启，会扩大所有安装的攻击面；仅设置 enable 而缺少有效密钥材料，又会产生表面可用、实际不安全的半启用状态。

## Decision

`openapi.enabled` 的代码默认值和公开发布样例保持 `false`。受管环境必须显式提供 enable、KEK version 和 KEK；OpenAPI secret 只进入权限受限的私密 release env，不进入 profile、state、报告或标准输出。启用但 KEK、版本、Redis 或唯一 system SPI 无效时，应用启动失败，不进入部分可用状态。

## Consequences

未配置环境不会装配 OpenAPI 管理 Controller 或调用入口。部署工具必须验证显式启用状态和安全材料存在性，同时只记录非敏感摘要；任何改变默认值或允许安全依赖降级的行为都需要新的安全决策。
