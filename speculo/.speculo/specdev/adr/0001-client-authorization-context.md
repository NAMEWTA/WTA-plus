# ADR-0001: Client 是认证授权上下文

- **Status:** Accepted
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` ADR-001

## Context

多个 APP 前端共用后端、用户和业务数据，但通过不同 Client 获得登录域、角色、菜单、权限字符、数据权限和动态路由。把 Client 建模为 tenant 会错误引入数据分区和跨 Client 所有权。

## Decision

Client 与 userType 只属于认证授权上下文，不构成 tenant 或通用业务数据所有权。`sys_client.id` Long 主键可作为 `client_pk` 记录调用来源，但不得参与 OSS/通知隔离、Provider 路由或幂等作用域。OAuth `clientId` 字符串与 `client_pk` 不可混用；后台任务不伪造 SYSTEM scope。

## Consequences

共享资源的访问必须由当前 Client 下的权限字符、数据权限和业务对象授权决定，不能依赖 Client 行级过滤兜底。新增 `client_pk` 数据隔离需要独立领域决定。

## Multi-App Assembly

在 plus-ui 多 App 架构中，共享认证授权核心由每个 App 显式注入 `ClientContext`、导航端口和终端 UI 适配器。认证流程保持 `getInfo -> getRouters -> addRoute`，Client 缺失或非法时 fail-close；App 可以定制页面与反馈，但不能绕过共享 Client、Token、权限和失败策略。

- **Append Source:** `2026-08-25-plus-ui-multi-app-domain-architecture` ADR-005（2026-08-28 归档提升）

## Client-scoped Authorization Snapshot Invalidation

角色菜单或数据权限变化后，注销该角色所属 Client 的全部会话；用户角色关系变化后，只注销该用户在目标 Client 的全部会话。其他 Client 的授权快照未变化，必须保留其会话。目标范围的失效同时覆盖 Sa-Token Redis Token/Session、权限数据缓存和全部运行实例的 JVM 本地 Caffeine 副本，使重新登录生成新的角色、菜单和数据权限快照。

权限变更成功响应不能早于上述分层失效完成，也不能通过原地修改旧 Token 的部分集合代替重新登录。业务代码通过统一会话与缓存抽象执行失效，不散落硬编码 Redis key。

- **Append Source:** `2026-08-28-user-password-policy-temporary-credentials` ADR-004（2026-08-29 归档提升）
