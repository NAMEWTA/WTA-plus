# ADR-0040: OpenAPI 双入口共享 system 前端域

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-013、ADR-014、ADR-020
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-admin-runtime-capability-reconciliation/ADR.md</Path>` ADR-003

## Context

管理员需要按用户管理开放凭据，普通用户只需要管理本人凭据和阅读本人可调用接口。为两个入口复制 transport、状态和页面逻辑会造成 owner 边界与授权结果漂移，也不符合 admin App 显式组合 system domain/web-domain 的当前架构。

## Decision

管理员入口当前命名为“系统管理 > OpenAPI管理”，使用 target-user scope；个人入口是个人信息现有 Tabs 中的同级“开放应用”，使用 current-user scope。OpenAPI transport、领域模型和 owner-scope 服务归 `@namewta/domain-system`，共享组件和动态管理页归 `@namewta/web-domain-system`，admin App 只进行显式组合。两个入口共享凭据生命周期、接口目录、详情、文档和权限结果，后端仍对每项操作独立鉴权。

## Consequences

个人信息页继续是静态路由，不因 Tab 变成动态菜单；个人入口不能选择其他 owner，管理入口不能用管理员自身目录代替目标用户目录。不得恢复 `gen`，也不得在 App 内建立 system transport 或状态副本。
