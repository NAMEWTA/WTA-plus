---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-03
title: 构建跨合法 Client 的全局 OpenAPI 授权快照
status: done
planning_depth: deep
planning_depth_reason: 新增跨 Client 权限并集、默认角色和数据权限语义，属于认证授权核心且越权事故半径高。
ready: true
risk: critical
blocked_by: [T-01]
contract_ids: [AC-013, AC-014, AC-015, AC-016, AC-017, AC-019]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/authorization/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/authorization/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/authorization/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysOpenApiAuthorizationMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOpenApiAuthorizationMapper.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/openapi/authorization/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/model/LoginUser.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysRoleMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysMenuMapper.java</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 构建跨合法 Client 的全局 OpenAPI 授权快照

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/03-global-authorization-snapshot.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 由 system 权威关系只读构建标准 `LoginUser`，为目录和真实调用提供唯一授权事实。
- **可观察产出：** 给定 userId，可重复得到只含合法 Client 当前授权的 roles、permissions 和 `dataScopeRoleMap`，无目标权限时不补写。
- **来源：** `US-002`、`US-004`、`AC-013` 至 `AC-017`、`AC-019`、`ADR-018`、`ADR-019`。
- **当前事实：** 现有权限服务只处理单 Client；角色服务已有合法 Client、默认角色和用户登录域校验逻辑，但没有全局聚合入口。
- **Planning Depth 原因：** 错误聚合会放大机器身份权限并影响数据权限 SQL。

## 2. 决策状态

### 已锁定决策

- 合法 Client 必须正常、关联正常登录域且用户持有该登录域关系；在线 Session 不是事实源。
- 合并每个合法 Client 的正常默认角色与正常显式角色，只纳入正常菜单。
- permissions/role keys 去重；`dataScopeRoleMap` 按权限汇总实际角色 ID，沿用现有联合计算。
- 超管沿用 `superadmin` 与 `*:*:*`；`clientPk/clientKey` 始终为空。
- resolver 只返回现有授权，不根据目录或请求路径新增任何权限。

### 已采用的低影响假设

- 专用 mapper 可用一次或有界批量查询形成快照；不机械循环调用单 Client service 造成 N+1。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 全局授权查询、标准 LoginUser 组装、快照 SPI 实现、负向矩阵 | system 用户/Client/登录域/角色/菜单关系、现有 RoleDTO/dataScope 语义 | 动态路由、凭据、Session、授权写入、单 Client fallback |

## 4. 要构建什么

目录或机器会话请求某用户快照时，system 只从当前持久关系重建其真实全局能力。任何停用 Client、登录域、关系、角色或菜单立即从新快照消失；没有合法 Client 或用户无效时失败关闭。

## 5. 实现契约

- **入口或接缝：** T-01 授权快照 SPI 的 system 唯一 Bean。
- **输入与输出：** owner userId -> 标准 `LoginUser` 或不可构建失败。
- **公共接口变化：** 仅实现 common SPI，不新增跨模块 system 内部类型。
- **不变量：** `clientPk/clientKey=null`；不加载 routes/posts 的 Client 特例；角色/权限集合不可变或防御复制。
- **状态或数据流：** user -> 合法 userType/client -> 默认+显式角色 -> 正常菜单 -> RoleDTO/permission/dataScope map -> LoginUser。
- **错误与失败行为：** 用户无效、关系不完整、状态冲突或查询失败均不返回部分授权。
- **兼容要求：** 单 Client 登录与现有 `SysPermissionServiceImpl` 行为完全不变。
- **安全与隐私要求：** 查询不绕过状态过滤，不把敏感用户字段加入快照。

## 6. 执行路线

1. 建立完整授权矩阵测试，先证明单 Client helper 不能满足全局合同。
2. 设计批量查询/投影，明确每个状态与默认角色来源。
3. 实现 SPI 和标准 LoginUser 组装，不修改 LoginUser 或普通登录器。
4. 覆盖超管、重复权限、数据范围、无合法 Client 和空 Client 下游负向路径。
5. 运行定向测试及普通权限回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** 新 authorization owner 包、专用 mapper/XML 与定向测试。
- **只读上下文：** 现有单 Client 权限、角色、菜单和 LoginUser 实现。
- **共享路径：** 无；POM 由 T-01、现有认证类保持只读。
- **保留或不动：** `SysPermissionServiceImpl`、`LoginUser`、普通 Mapper 合同和动态路由。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 合法权限并集 | resolver unit/module | `./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test -Dtest='*OpenApiAuthorization*'` | 默认/显式角色、权限、dataScope 正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 状态负向矩阵 | mapper/service fixtures | 同上 | 任一停用或缺关系均不授权 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |
| 普通登录回归 | authorization tests | `./mvnw -pl ruoyi-admin -am test -Dtest='*Authorization*'` | 单 Client 登录与权限不变 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-03.md</Path>` |

- **Workspace checks：** current-workspace 或 source-worktree 跑定向测试；集成态跑普通授权回归。
- **E2E disposition：** not-required：本 Ticket 是无 HTTP 的安全解析接缝，目录与调用边界分别由 T-04/T-09 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA 与包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** additive resolver 先于目录、Session 和网关消费者集成。
- **兼容窗口：** 普通单 Client 解析继续存在；OpenAPI 仅调用新端口。
- **监控信号：** 快照构建失败类别与耗时，不记录权限外敏感值。
- **回滚或前向恢复：** 消费者启用前可移除；启用后以关闭 OpenAPI flag 隔离并前向修复。
- **不可逆操作与批准点：** 无数据写；实现提交/集成需授权。
- **收缩条件：** 不适用：无旧 OpenAPI resolver。

## 10. 验收标准

- [x] `AC-013` 至 `AC-017`、`AC-019` 的授权矩阵可判定通过。
- [x] `clientPk/clientKey` 为空，无在线 Session 推断和权限补写。
- [x] 普通单 Client 权限链、LoginUser 类型与 Mapper 公共行为不变。
- [x] Evidence、commit、集成 SHA、未验证真实 DB 风险与 Map 状态完整。
