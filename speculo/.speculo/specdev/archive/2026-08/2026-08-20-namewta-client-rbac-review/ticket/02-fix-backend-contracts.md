---
schema_version: 3
artifact: ticket
change: 2026-08-20-namewta-client-rbac-review
id: T-02
title: 修复后端 Client RBAC 与认证合同
status: done
planning_depth: deep
planning_depth_reason: 修改权限边界、公共 HTTP/JSON 合同、事务和会话失效，事故半径高。
ready: true
risk: critical
blocked_by: [T-01]
contract_ids: [AC-001, AC-003, AC-004, AC-005]
owner: codex-root
expected_changes: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/**</Path>"]
writable_paths: ["<Path>ruoyi-vue-plus-namewta/ruoyi-api/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/**</Path>", "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/**</Path>"]
read_only_paths: ["<Path>plan/update.md</Path>", "<Path>docs/upstream/customization-map.md</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 修复后端 Client RBAC 与认证合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/ticket/02-fix-backend-contracts.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 关闭所有后端 fail-open、全局角色 fallback、认证/注册和会话接口 finding。
- **可观察产出：** 无效 Client/登录域/角色明确拒绝；用户详情按 Client 返回角色；注册可选身份唯一；用户全域 Token 清理。
- **来源：** `AC-001`、`AC-003`、`AC-004`、`AC-005`、`CR-001`。
- **当前事实：** 两条授权路径跳过无效关系，用户角色查询可无 Client，LoginHelper 有两级 fallback。
- **Planning Depth 原因：** 核心权限与公共合同变更。

## 2. 决策状态

### 已锁定决策

- 失败关闭；用户基础详情无 Client 时不返回角色；authRole 和角色管理入口要求 Long Client PK。
- RegisterBody 后端可选手机/邮箱，当前前端不采集。
- ClientSessionService 仅保留三个规定方法。

### 已采用的低影响假设

- 不新增 schema 或测试源码；复用现有唯一性服务。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| role/user/menu/auth/register/session 修复 | 现有 BO/VO/mapper/事务/Sa-Token | 新依赖、SQL schema、前端 |

## 4. 要构建什么

管理员只能在合法 Client 和登录域下读取、分配角色；终端认证上下文不能被 legacy 信息重建；注册可选身份重复时拒绝；用户全局停用/删除逐登录域清理 Token。

## 5. 实现契约

- **入口或接缝：** SysRole/User/Menu service、SysUserController、LoginHelper、SysRegisterService、ClientSessionService。
- **输入与输出：** Long Client PK 显式输入；角色响应仅当前 Client；RegisterBody 可选 phone/email。
- **公共接口变化：** authRole/list/export Client 必填；用户详情角色条件化；注册增加可选字段。
- **不变量：** 所有记录存在启用且同 Client；默认角色不显式写入；无 Token 不回退。
- **状态或数据流：** 先更新登录域关系，再验证并替换显式角色；删除关系前捕获会话域。
- **错误与失败行为：** 所有无效状态抛 ServiceException；不吞异常。
- **兼容要求：** 前端同步交付；不保留旧 fallback。
- **安全与隐私要求：** 不记录 Token、手机、邮箱或 secret。

## 6. 执行路线

1. 收紧角色查询和默认角色运行时校验，删除无 Client mapper。
2. 收紧两条授权路径与用户详情/authRole 合同。
3. 删除 LoginHelper fallback，增加可选注册身份校验。
4. 收缩会话公开接口并改造全局用户失效调用点。
5. 分段 package，最终运行 opt-in tests 和全量 package。

## 7. 路径访问契约

- **预计修改点：** frontmatter 中四个后端模块。
- **可写范围：** 仅列出的后端路径。
- **只读上下文：** plan 与 customization map。
- **共享路径：** 无。
- **保留或不动：** POM、Wrapper、SQL、生成物。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 Client 角色 | service/controller | scoped request review | 只返回/写入当前 Client | `<Path>{roots.state}/specdev/changes/2026-08-20-namewta-client-rbac-review/evidence/T-02.md</Path>` |
| 无效关系 | service negative matrix | 缺失/停用/跨 Client 输入 | 全部拒绝 | 同上 |
| 注册与会话 | register/session paths | 重复身份与多域用户场景 | 重复拒绝、全部 Token 失效 | 同上 |
| 回归 | Maven | opt-in test + clean package | exit 0 | 同上 |

- **Workspace checks：** current workspace，定向编译、opt-in tests、clean package、diff-check。
- **E2E disposition：** required：认证、事务和 Token 跨边界高风险；整体环境在 T-04 执行。
- **E2E owner/environment：** Lead/current-workspace；MySQL/Redis/HTTP。
- **Integration evidence：** implementation commits、parent before/result SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端合同先完成，前端随后适配，协调发布。
- **兼容窗口：** 新后端不接受旧无 Client authRole 请求；不得单独长期发布。
- **监控信号：** 业务拒绝响应、登录失败和 Token 状态。
- **回滚或前向恢复：** revert 聚焦提交；无 schema 回滚。
- **不可逆操作与批准点：** 本地 commit 已授权；push/deploy 未授权。
- **收缩条件：** 删除所有无 Client 调用和 legacy fallback 后由 rg/diff 证明为零。

## 10. 验收标准

- [x] `AC-001`、`AC-003`、`AC-004`、`AC-005` 的代码合同全部满足。
- [x] 验证矩阵写入 Evidence。
- [x] 修改不越界且无锁文件/POM/schema 漂移。
- [x] 形成聚焦 implementation commits，direct-parent 验证通过。
- [x] E2E 明确委托 T-04 完成并回填。
- [x] 无未批准偏差。
- [x] Ticket、Map、Evidence 一致。
