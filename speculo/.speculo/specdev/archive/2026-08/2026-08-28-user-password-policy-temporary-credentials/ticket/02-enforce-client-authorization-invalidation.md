---
schema_version: 3
artifact: ticket
change: 2026-08-28-user-password-policy-temporary-credentials
id: T-02
title: 按受影响 Client 立即失效授权会话
status: done
planning_depth: deep
planning_depth_reason: 角色和用户角色写入会改变登录授权快照，必须在多 Client、多 Token、多 JVM 下精确清理 Redis 与内存。
ready: true
risk: critical
blocked_by: [T-01]
contract_ids: [AC-020, AC-021, AC-022]
owner: codex:lead
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysRoleServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/authorization/session/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ClientSessionService.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysRoleServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/authorization/session/**</Path>"]
read_only_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysRoleController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysUserController.java</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-redis/src/main/java/org/dromara/common/redis/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/core/dao/PlusSaTokenDao.java</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 按受影响 Client 立即失效授权会话

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/ticket/02-enforce-client-authorization-invalidation.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 让角色菜单/数据权限和用户角色关系变化后的旧 `LoginUser` 快照在目标 Client 内立即失效，其他 Client 保持登录。
- **可观察产出：** 角色权限保存后该角色所属 Client 的全部 Token 失效；用户角色授予/撤销/覆盖后仅该用户在目标 Client 的全部 Token 失效；重新登录取得最新权限。
- **来源：** `US-008`、`AC-020`、`AC-021`、`AC-022`、`ADR-004`。
- **当前事实：** 现有 role/user 服务已经调用 `ClientSessionService` 做 Client 或 user+Client 注销，但服务只扫描并 logout Token，无法证明 Spring/Sa-Token 的全部 Redis 和 JVM 本地副本同步失效。
- **Planning Depth 原因：** 这是认证授权核心和跨 Client 隔离边界，清理过窄会保留权限，过宽会中断无关应用。

## 2. 决策状态

### 已锁定决策

- 角色菜单或数据权限变化按角色所属 `clientPk` 注销该 Client 全部会话，覆盖默认角色和非显式关系来源。
- 用户角色关系变化按 `userId + clientPk` 注销全部 Token，不影响该用户其他 Client。
- 复用 T-01 统一失效协议；system 不硬编码 Sa-Token Redis key。
- 成功响应不得先于会话与权限缓存失效完成；失效可幂等重试。

### 已采用的低影响假设

- 保留当前 Controller 到 service 的调用形状，优先强化 ClientSessionService 返回/失败语义。验证：现有调用点扫描和 API 回归。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 角色权限和用户角色关系写后的目标解析、Token/Session/权限缓存清理、幂等失败语义 | 现有角色 Client 归属、用户角色写入、T-01 广播、LoginHelper | 不改变永久密码重置，不注销其他 Client，不原地改 Token 权限集合 |

## 4. 要构建什么

权限管理员保存角色菜单或数据范围后，在响应成功前清理角色所属 Client 的所有在线快照。管理员从用户页或角色页单个/批量授予、撤销或覆盖角色后，在响应成功前只清该用户在目标 Client 的所有快照。旧 Token 随即未登录，其他 Client Token 继续可用，重新登录按数据库最新状态重建权限。

## 5. 实现契约

- **入口或接缝：** 现有角色 permission 写入、用户 `insertUserAuth`、角色 authUser 单个/批量写入。
- **输入与输出：** 输入为 roleId 或 userId+clientPk；输出为失效结果（Token 数、缓存层、节点确认）或可重试失败。
- **公共接口变化：** HTTP 路径和成功 envelope 不变；失败时不得返回成功。现有 PUT 属存量合同，本 Ticket 不扩展 CRUD method 迁移范围。
- **不变量：** OAuth `clientId` 字符串与 Long `clientPk` 不混用；角色按 Client 全部，用户关系按 user+Client；其他 Client 零注销。
- **状态或数据流：** DB write -> resolve target -> enumerate/logout -> Redis/cache invalidate -> remote local invalidate -> success。
- **错误与失败行为：** 任一必要失效层失败时记录目标与已完成层，返回失败并支持同目标幂等重试；不等待 TTL 冒充成功。
- **兼容要求：** 永久密码重置和个人改密会话行为不变；直接菜单 CRUD 当前既有 Client kick 继续复用增强后的 service。
- **安全与隐私要求：** 日志只记录 userId/clientPk/count/requestId，不记录 Token 值或 LoginUser 正文。

## 6. 执行路线

1. 用双 Client、多 Token、双 JVM fixture 先证明当前远端本地快照残留。
2. 强化 ClientSessionService 的目标解析、结果与错误传播，并接入 T-01 失效协议。
3. 复核角色 permission、用户覆盖角色、角色页单个/批量授予撤销的所有写后调用点。
4. 增加默认角色、缺失/错误 Client 标识、重复失效和中途失败测试。
5. 运行定向集成、system/admin reactor 和多 Client E2E。

## 7. 路径访问契约

- **预计修改点：** ClientSessionService、SysRoleServiceImpl、SysUserServiceImpl 与专用授权测试。
- **可写范围：** frontmatter 列出的 system service 与测试目录。
- **只读上下文：** controllers、T-01 common contract、Sa-Token DAO。
- **共享路径：** 无；common 失效实现由 T-01 独占。
- **保留或不动：** 密码、注册、前端、SQL、raw HTTP 日志。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 多 Client API + Redis | 修改角色权限和用户角色 | 目标 Token 全失效，重新登录为新快照，其他 Client 保持 | `<Path>{roots.state}/specdev/changes/2026-08-28-user-password-policy-temporary-credentials/evidence/T-02.md</Path>` |
| 失败路径 | 故障注入 | 让远端缓存确认失败 | API 不报告完整成功，重试后收敛，无秘密日志 | 同上 |
| 回归 | Maven + 认证矩阵 | `./mvnw -pl ruoyi-modules/ruoyi-system,ruoyi-admin -am test` | 角色/用户/菜单与永久重置既有行为保持 | 同上 |

- **Workspace checks：** 按 Goal Plan 在 current-workspace 或 source-worktree 运行非 E2E 检查。
- **E2E disposition：** required：跨 Client 授权撤销与双 JVM 立即失效必须从真实 Token 边界证明。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；至少覆盖 Client A/B、同用户多 Token、角色全 Client 和 user+Client。
- **Integration evidence：** backend commit、parent before、candidate/direct-parent、result SHA 与父仓库 gitlink。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 全节点协议先部署；本 Ticket 所有节点完成后才依赖立即失效合同。
- **兼容窗口：** 混合版本只允许旧行为，不得对外宣称立即失效；发布 Gate 要求全后端节点同版本。
- **监控信号：** target type、clientPk、Token 数、节点确认、耗时、失败和重试。
- **回滚或前向恢复：** 失败优先前向重试失效；代码回滚恢复旧 TTL 行为时必须停止权限管理写入或明确安全降级。
- **不可逆操作与批准点：** 会话注销不可恢复但属于用户已批准行为；部署和实际生产失效仍需授权。
- **收缩条件：** 不适用；无临时协议。

## 10. 验收标准

- [x] `AC-020`：角色权限变化只让所属 Client 全部会话失效。
- [x] `AC-021`：用户角色变化只让该用户在目标 Client 的全部会话失效。
- [x] `AC-022`：任一缓存层失败不假成功且可重试。
- [x] 真实 Redis + 双 JVM Evidence 完整，其他 Client 零注销。
- [x] 修改未越界并形成 backend commit；Lead 完成集成、E2E 和父状态记录。
