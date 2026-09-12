---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-07
title: 补齐用户、Client 与登录域权限变更的机器 Session 失效
status: done
planning_depth: deep
planning_depth_reason: 多条现有事务写路径必须在成功返回前完成集群级机器 Session 注销，遗漏任一入口都会形成旧权限窗口。
ready: true
risk: critical
blocked_by: [T-05]
contract_ids: [AC-018, AC-030]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysClientServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserTypeServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserTypeRelServiceImpl.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysClientServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserTypeServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserTypeRelServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/invalidation/IdentityOpenApiSessionInvalidationTest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-07: 补齐用户、Client 与登录域权限变更的机器 Session 失效

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/07-identity-client-session-invalidation.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>`

## 1. 战略与来源

- **目标：** 身份、Client、登录域或显式用户角色发生权限相关变化后，在写操作成功返回前注销所有受影响机器 Session。
- **可观察产出：** 用户状态/删除/角色、Client 状态/登录域/默认角色和用户类型关系变更后，旧机器 Token 立即失效。
- **来源：** `AC-018`、`AC-030`、全局登录态即时失效约束。
- **当前事实：** `ClientSessionService` 已集中处理活动 Token 搜索与 logout，`PlusSaTokenDao` 已提供集群失效传播。
- **Planning Depth 原因：** 失效调用点横跨多种聚合根，且必须与业务事务成功语义一致。

## 2. 决策状态

### 已锁定决策

- 失效逻辑进入 service 写路径，不依赖前端调用顺序或仅在 Controller 补偿。
- 写入成功但机器 Session 注销失败时不得静默返回成功；错误进入统一操作错误与审计分类。
- 被修改方法若仍使用 Spring `@Transactional`，按工程增量规则迁移为 `@DSTransactional`。
- 浏览器用户 Session 的既有策略保持不变，本票只新增/校正 OpenAPI 机器 Session 失效。

### 已采用的低影响假设

- 受影响用户集合由现有关系表在事务内精确求得；不做全租户或全系统 Token 扫描。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 用户状态/删除/显式角色、Client 状态/登录域/默认角色、用户类型关系写路径 | T-05 invalidator、现有 service/mapper、`@DSTransactional` | role/menu/data-scope 写路径（T-08）、凭据生命周期（T-06）、新消息中间件 |

## 4. 要构建什么

在所有身份和 Client 权限写路径中精确收集受影响 userId，并调用 T-05 的集群级机器 Session invalidator。调用必须覆盖批量、关系替换和删除分支，且发生在业务结果对外确认之前。

## 5. 实现契约

- **入口或接缝：** `SysUserServiceImpl`、`SysClientServiceImpl`、`SysUserTypeServiceImpl`、`SysUserTypeRelServiceImpl` 的现有命令方法。
- **输入与输出：** 原业务命令 -> 原返回合同；仅增加 Session 失效副作用和明确失败。
- **公共接口变化：** 无新增 HTTP 路径或公共 DTO。
- **不变量：** 不误注销无关用户；无实际权限变化时不制造无界扫描；普通浏览器 Token 行为不变。
- **状态或数据流：** command -> relation diff/affected users -> database write -> T-05 invalidator -> success response。
- **错误与失败行为：** 无法确定影响集合或注销失败时 fail closed；删除后仍需使用删除前捕获的 userId 集合。
- **兼容要求：** 保留现有权限、响应与审计注解。
- **安全与隐私要求：** 日志只记录受影响数量/业务 ID，不记录 Token 或凭据。

## 6. 执行路线

1. 用调用点测试枚举用户、Client、登录域和用户角色全部写分支。
2. 在每条写路径计算最小受影响 userId 集合并接入 T-05 invalidator。
3. 迁移本票触及的遗留事务注解，验证异常能阻止成功返回。
4. 运行 system/admin 定向测试，并以源码搜索复核无漏网写入口。

## 7. 路径访问契约

- **预计修改点/可写范围：** 四个既有 service 实现与一组定向失效测试。
- **只读上下文：** `ClientSessionService`、Controller 路由和 common-openapi Session SPI。
- **共享路径：** 无。
- **保留或不动：** role/menu service、credential 包、Sa-Token DAO 与前端源码。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 各身份写路径立即失效 | service interaction test | `./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test -Dtest=IdentityOpenApiSessionInvalidationTest` | 每个成功写分支调用精确 userId 集合 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| 注销失败不返回成功 | failure-path test | 同上 | 事务/响应按约定失败且错误可分类 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| 入口完整性 | `rg` + review | 搜索目标 service 的状态、删除、关系与授权命令 | 所有权限影响入口均有测试映射 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |

- **Workspace checks：** current-workspace 跑定向测试；parent-candidate 跑 system reactor tests。
- **E2E disposition：** not-required：本票是 service 副作用接缝，使用交互测试验证；真实多节点传播保留为 residual risk。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-05 先交付，再合入本票；功能仍由 T-12 总开关控制。
- **兼容窗口：** 无 schema/API 变化。
- **监控信号：** invalidation 失败类别、受影响 Session 数量与写操作错误率。
- **回滚或前向恢复：** 关闭 OpenAPI 停止新机器调用；修复失效调用后前向恢复。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 所有列举写入口均有用例且未引入全量 Token 扫描。

## 10. 验收标准

- [ ] `AC-018`、`AC-030` 在身份/Client/login-domain/user-role 写路径成立。
- [ ] 写成功返回前完成集群注销，失败不会被吞掉。
- [ ] 普通浏览器登录行为、原权限与 HTTP 合同无回归。
- [ ] Evidence 明确多节点 Redis 未实测风险及实现/集成 SHA。
