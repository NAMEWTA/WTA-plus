---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-08
title: 补齐角色、菜单与数据范围变更的机器 Session 失效
status: done
planning_depth: deep
planning_depth_reason: RBAC 写入口包含基础信息、状态、授权用户、菜单权限和数据范围，必须统一迁移到成功前失效语义。
ready: true
risk: critical
blocked_by: [T-05]
contract_ids: [AC-018, AC-030]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysRoleServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysRoleServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysMenuServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/invalidation/RbacOpenApiSessionInvalidationTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/system/AuthorizationInvalidationCallSiteUnitTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-08: 补齐角色、菜单与数据范围变更的机器 Session 失效

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/08-rbac-menu-session-invalidation.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 角色、菜单和数据范围变化后立即撤销持有旧授权快照的机器 Session。
- **可观察产出：** 角色状态/删除/权限/授权用户/数据范围以及菜单状态/权限标识/Client 关联变化后，相关机器 Token 不再可用。
- **来源：** `AC-018`、`AC-030`、权限快照不可跨变更继续使用的不变量。
- **当前事实：** 部分普通登录失效调用当前位于 Controller；T-05 提供统一机器 Session invalidator。
- **Planning Depth 原因：** 角色与菜单关系存在多条批量写路径，影响用户集合必须在删除关系前捕获。

## 2. 决策状态

### 已锁定决策

- 机器 Session 失效以 service 层命令为最终保障；清理重复 Controller 调用但保持普通登录既有行为。
- 角色基本信息只有影响授权语义的变化才触发；状态、权限、数据范围、授权用户和删除必须覆盖。
- 菜单状态、permission key、Client 关联及删除必须覆盖其关联角色下所有用户。
- 本票触及的遗留 Spring `@Transactional` 迁移为 `@DSTransactional`。

### 已采用的低影响假设

- 通过既有 role-user/menu-role/client-menu 关系查询受影响集合，不引入缓存广播新协议。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| role/menu/data-scope/authorized-user 权限写路径与必要 Controller 调整 | T-05 invalidator、既有普通登录失效测试 | user/client/login-domain（T-07）、权限快照计算（T-03）、前端 RBAC 编辑页 |

## 4. 要构建什么

为 RBAC 命令建立完整调用点矩阵，精确查询受影响用户，并在命令成功返回前注销机器 Session。删除关系必须先捕获影响集合，批量授权必须覆盖新增和移除双方。

## 5. 实现契约

- **入口或接缝：** `SysRoleServiceImpl`、`SysMenuServiceImpl` 以及当前承载失效逻辑的 `SysRoleController`。
- **输入与输出：** 原角色/菜单命令 -> 原业务响应；增加机器 Session 失效副作用。
- **公共接口变化：** 无。
- **不变量：** 超级管理员与普通角色都不能绕过机器 Token 失效；无关用户不受影响。
- **状态或数据流：** RBAC command -> capture affected users -> persist relations -> invalidate machine sessions -> return。
- **错误与失败行为：** 影响集合查询或注销失败即明确失败；不得只写日志继续成功。
- **兼容要求：** 保留普通用户登录注销逻辑和现有权限控制。
- **安全与隐私要求：** 不记录 Token、权限全集或敏感用户详情。

## 6. 执行路线

1. 将所有 role/menu/data-scope 写入口列入测试参数矩阵。
2. 把机器失效保障落到 service，并协调 Controller 中的既有普通 Session 逻辑。
3. 为删除前捕获、批量授权、状态变化和失败路径增加测试。
4. 运行定向测试与源码入口审查。

## 7. 路径访问契约

- **预计修改点/可写范围：** role/menu service、必要的 role Controller 调整与独立测试。
- **只读上下文：** T-05 invalidator 和既有授权失效单测。
- **共享路径：** 无。
- **保留或不动：** 用户/Client service、credential、common-openapi 网关和前端源码。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| RBAC 全写入口失效 | parameterized interaction test | `./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test -Dtest=RbacOpenApiSessionInvalidationTest` | 每条权限写路径命中正确用户集合 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 删除前捕获/失败关闭 | service failure test | 同上 | 关系删除后仍能注销，失败不返回成功 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |
| 普通登录无回归 | existing + new test | `./mvnw -pl ruoyi-admin -am test -Dtest='AuthorizationInvalidationCallSiteUnitTest,RbacOpenApiSessionInvalidationTest'` | 原普通 Session 行为保留且无重复副作用 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-08.md</Path>` |

- **Workspace checks：** current-workspace 跑定向用例；parent-candidate 跑 system/admin reactor tests。
- **E2E disposition：** not-required：service 接缝由交互测试覆盖；真实 Redis 多节点传播保留为 residual risk。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-05 后合入，与 T-07 可并行；T-12 启用前完成。
- **兼容窗口：** 无 schema/API 变化。
- **监控信号：** RBAC 写失败、失效用户数和 invalidator 错误类别。
- **回滚或前向恢复：** 关闭 OpenAPI；修复调用点后前向恢复，避免恢复旧权限机器 Token。
- **不可逆操作与批准点：** 无。
- **收缩条件：** role/menu 全入口矩阵完整且普通登录测试通过。

## 10. 验收标准

- [x] `AC-018`、`AC-030` 覆盖角色、菜单、数据范围和授权用户全部写入口。
- [x] 受影响机器 Session 在成功返回前注销，失败不静默。
- [x] 普通登录失效策略无回归且不产生无界扫描。
- [x] Evidence 记录真实多节点未验证风险及实现/集成 SHA。
