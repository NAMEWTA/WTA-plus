# ADR-0029: 受保护的 Nacos 单机发布基线

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-31-optional-nacos-dynamic-config/ADR.md</Path>` ADR-005、ADR-011

## Context

当前发布需要可复现、可持久化的 Nacos 基础设施，但尚未要求生产多节点高可用。直接公网暴露端口、复用业务数据库高权限账号或提交固定鉴权 secret 会放大配置和备份的暴露面。

## Decision

发布基线固定使用官方 `nacos/nacos-server:v2.5.4`，以 standalone 运行，在现有 MySQL 服务中使用独立数据库和最小权限账号。8848/9848 默认只绑定 `NAMEWTA_BIND_HOST=127.0.0.1`；鉴权 token、identity 和密码由必填环境变量注入。发布资产提供 schema 初始化、持久化和健康门，Web 控制台统一经 `/nacos/` 同源反代访问。

## Consequences

该基线提供单机可复现性，不代表生产高可用；集群化必须另立 change。仓库只能保存示例变量，不能保存真实凭据。应用仍须遵守本地配置兜底，不能因 Nacos 单节点不可用而失去启动能力。
