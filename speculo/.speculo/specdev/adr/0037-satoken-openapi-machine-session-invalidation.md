# ADR-0037: Sa-Token OpenAPI 机器会话与确认失效

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-08/2026-08-30-openapi-common-module/ADR.md</Path>` ADR-005、ADR-009、ADR-010、ADR-016

## Context

OpenAPI 需要复用现有 `LoginHelper`、权限注解、数据权限和敏感字段判断，但第三方不能持有浏览器式 Sa-Token。另建 Principal、权限注解或 Redis 权限缓存会形成第二条安全主链和第二个授权事实源。

## Decision

每次开放调用都先完成凭据与签名校验，再取得仅服务端可见的内部机器 Token，并只写入当前 Sa-Token request Storage。标准 `LoginUser` 保存在现有 TokenSession 中并通过现有 Redis DAO 复用；cache miss 时由 system 权威关系只读重建，绝不按当前请求补授权限。凭据、用户、角色、菜单、登录域或 Client 状态变化时，注销受影响用户的机器 Session；删除继续经过现有 `PlusSaTokenDao` 和 `ClusterCacheInvalidationCoordinator`，写操作在集群确认失败时显式失败。

## Consequences

内部 Token 不进入协议、响应、文档或日志，且缓存不能绕过逐请求验签。请求结束只清理 request Storage，仍有效的机器 Session 可继续复用。授权变更成功返回前旧 Session 必须完成跨节点确认失效，下一次有效请求只能在 miss 后重建当前授权。
