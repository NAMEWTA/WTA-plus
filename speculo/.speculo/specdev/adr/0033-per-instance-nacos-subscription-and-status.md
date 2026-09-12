# ADR-0033: Nacos 按实例订阅与状态报告

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-010

## Context

双 ruoyi-admin 实例部署中，Nacos 发布成功不能证明每个实例都解析并应用了同一版本。引入 Spring Cloud Bus 只转发事件，不能消除实例级解析和应用失败，反而增加消息基础设施和传播路径。

## Decision

每个 ruoyi-admin 实例直接订阅其 active profile 对应 namespace、`DEFAULT_GROUP` 和 `ruoyi-namewta.yml`，不引入 Spring Cloud Bus。每个实例独立记录远程版本摘要、校验与应用结果、最后成功时间，以及即时生效或等待重启分类。

## Consequences

运维必须按实例判断最终收敛，并能单独定位拒绝远程版本的实例。健康或管理状态只暴露安全摘要和分类，不返回配置正文、secret 或 Nacos 凭据。
