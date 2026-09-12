---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-09
title: 交付管理查询、审核上下文与高风险 Profile 覆盖命令
status: done
planning_depth: deep
planning_depth_reason: 明文全量查看、ADMIN_CREATE/OVERRIDE、流程竞态和注销终态同时涉及授权、隐私与数据完整性。
ready: true
risk: critical
blocked_by: [T-01, T-04, T-05, T-07]
contract_ids: [AC-024, AC-025, AC-026, AC-027, AC-028, AC-029, AC-030, AC-031, AC-039, AC-041, AC-042, AC-043, AC-044]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/admin/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/admin/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/admin/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/admin/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/person/admin/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/test/java/org/dromara/profile/enterprise/admin/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/application/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-09: 交付管理查询、审核上下文与高风险 Profile 覆盖命令

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/09-admin-profile-operations.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>`

## 1. 战略与来源

- **目标：** 提供后台完整读取、workflow 审核上下文和有审计的直接运营处置。
- **可观察产出：** 有权管理员可查全量详情/历史、办理审核、直建/修订/状态处置；无权拒绝；注销只读；覆盖与迟到流程不会双决定。
- **来源：** `US-011` 至 `US-015`、`US-021`、`US-022`、`AC-024` 至 `AC-031`、`AC-039`、`AC-041` 至 `AC-044`、`ADR-007`、`ADR-012`、`ADR-017`、`ADR-022`、`ADR-024`。
- **当前事实：** T-01 提供终止接缝，T-05/T-07 提供状态机，尚无管理 API。
- **Planning Depth 原因：** override 可直接改变法律主体/绑定，且接口明文展示敏感数据。

## 2. 决策状态

### 已锁定决策

- query/material/review/manage/override 按完整能力授权，不叠加部门数据范围。
- review 自带所需快照/材料读取；override 自带终止/读取，不要求隐藏伴随权限。
- waiting 覆盖先锁申请/递增 decisionVersion/override-pending，再终止流程；失败整体失败，迟到事件只审计。
- ADMIN_CREATE 满足相同字段/材料并生成独立来源；ADMIN_OVERRIDE 新版本不改历史。
- manage 负责 suspend/resume/unbind；override 负责直批/拒、直建、核心修订、指定负责人和 revoke；全部填原因。
- revoked 默认列表排除、筛选可见且全量只读，不能恢复/绑定/编辑。

### 已采用的低影响假设

- 列表过滤采用现有 PageQuery，详情按聚合 DTO 返回分区历史，不提供导出。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 管理列表/详情、审核上下文、材料授权、create/override/manage/revoke、decision fence | T-01/04/05/07、workflow 待办 | 前端、批量导出、物理删除、数据范围 |

## 4. 要构建什么

管理者从档案或 workflow 上下文读取当前版本、快照、材料、绑定、流程和审计；所有高风险命令在事务前鉴权、带原因并追加来源/事件，注销记录以后只读。

## 5. 实现契约

- **入口或接缝：** `/profile/person/archive`、`/profile/enterprise/archive` 管理 GET/POST 与 review context。
- **输入与输出：** 筛选/ID/命令+原因 -> 全量 DTO、版本/绑定/决定结果。
- **公共接口变化：** 新增管理 HTTP；无 export；POST 均安全 `@Log`。
- **不变量：** 快照不改、版本/事件追加、终止失败不决定、revoked 终态。
- **状态或数据流：** auth -> lock -> optional workflow terminate -> source/version/binding/audit commit。
- **错误与失败行为：** 无权、目标不合格、冲突、流程终止失败、注销写入均在无部分状态下拒绝。
- **兼容要求：** workflow 历史保留，system/RBAC/Client 不变。
- **安全与隐私要求：** 后端授权材料下载；有权明文、无权零数据；日志不载荷。

## 6. 执行路线

1. 建立六能力正反矩阵、明文字段白名单和决定竞态测试。
2. 实现个人/企业管理聚合查询、审核上下文和材料 ACL。
3. 实现 ADMIN_CREATE 与目标资格/失败回滚。
4. 实现 decisionVersion 覆盖、修订、绑定 manage/指定和注销。
5. 运行并发 workflow、权限、日志、无 DELETE 和 system 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** 两叶子 admin 子树及测试。
- **只读上下文：** T-01/04/05/07 接缝。
- **共享路径：** 无。
- **保留或不动：** 申请/材料实现、SQL、workflow 内部和 system 数据。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | MVC/service/MySQL | 查询、审核、直建、修订、处置、注销 | 正确来源/版本/历史 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-09.md</Path>` |
| 失败 | auth/race/fault | 缺权限、目标冲突、终止失败、迟到事件 | 事务前/整体拒绝，无双决定 | 同上 |
| 回归 | log/DELETE/system scan | 明文/材料正反、日志、角色菜单检查 | 无泄漏/物理删除/RBAC 变化 | 同上 |

- **Workspace checks：** 定向测试、权限/日志/SQL 扫描和 Maven 编译。
- **E2E disposition：** required：HTTP 授权、workflow 终止、MySQL 事务和材料下载需联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** commit、parent/candidate/result SHA、决定/历史/日志证据。

## 9. 发布、迁移与恢复

- **迁移顺序：** 基础业务后部署管理 API；权限默认不授予。
- **兼容窗口：** ADMIN source type/decisionVersion 发布后稳定。
- **监控信号：** override、终止失败、冲突、明文/材料访问与注销量。
- **回滚或前向恢复：** 撤销菜单/权限入口；已提交决定不回写，前向修订。
- **不可逆操作与批准点：** 生产权限赋予、revoke 和 SQL 未授权。
- **收缩条件：** 不适用：无旧管理 API。

## 10. 验收标准

- [x] 所列 AC 的权限、决定、历史、隐私和回归矩阵通过。
- [x] required E2E、Evidence、提交和父分支结果完整。
- [x] 无导出、物理删除、数据范围或 system 授权副作用。
