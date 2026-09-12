# ADR-0039: 分离 OpenAPI 管理与调用授权

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-004、ADR-011、ADR-012

## Context

凭据管理发生在已登录的前端 Client 中，开放调用发生在第三方取得凭据之后。把菜单权限当作接口权限会让调用授权失真；目录预览若借用查看者会话或单独编写过滤 SQL，也会与目标用户真实调用不一致。

## Decision

管理 API 使用当前前端 Client 的既有菜单/按钮权限，并在 service 层校验 current-user 或 target-user owner 范围。开放调用不继承管理者权限，而是在验签后恢复凭据 owner 的当前全局授权，再由目标方法原有权限注解决定结果。真实调用 cache miss、用户自助目录和管理员目标用户预览共享同一类型化授权解析器、开放接口注册表和权限匹配函数；预览是只读计算，不切换登录态、不创建机器 Session。

## Consequences

拥有管理权限不自动获得目标接口调用权，拥有接口权限也不自动获得凭据管理权。超级管理员预览必须展示目标用户而非管理员自身的可调用集合，新增 `@OpenApi` 方法无需维护第二份目录授权规则。
