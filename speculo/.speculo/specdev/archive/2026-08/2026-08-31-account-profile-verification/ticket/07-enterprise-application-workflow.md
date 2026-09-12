---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-07
title: 交付企业申请、不可变快照与唯一负责人发布
status: done
planning_depth: deep
planning_depth_reason: 企业身份唯一、授权材料、流程发布和唯一负责人绑定属于高完整性公共业务合同。
ready: true
risk: critical
blocked_by: [T-03, T-04]
contract_ids: [AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-018, AC-023, AC-038]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/application/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/persistence/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/test/java/org/dromara/profile/enterprise/application/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/50-namewta-ddl.sql</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/60-namewta-dml.sql</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/src/main/java/org/dromara/profile/enterprise/material/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-07: 交付企业申请、不可变快照与唯一负责人发布

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/07-enterprise-application-workflow.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>`

## 1. 战略与来源

- **目标：** 从当前账户企业草稿到唯一负责人发布形成完整认证闭环。
- **可观察产出：** 申请人自行填写全部企业资料/新材料；workflow finish 发布企业版本和负责人，已绑定企业不能被普通申请顶替。
- **来源：** `US-002`、`US-003`、`US-004`、`US-011`、`AC-002` 至 `AC-008`、`AC-018`、`AC-023`、`ADR-002`、`ADR-003`、`ADR-015`。
- **当前事实：** T-01 至 T-04 提供接缝，尚无企业状态机和投影。
- **Planning Depth 原因：** 企业活动唯一、负责人绑定与授权书规则错误会产生主体控制权冲突。

## 2. 决策状态

### 已锁定决策

- 普通信用代码探测只返回状态，不预填旧企业资料/材料。
- 核心字段、营业执照、法人证件和非法人办理授权书按 Spec 校验。
- 申请账户通过后成为唯一负责人；法定代表人字段不自动绑定 sys_user。
- 已有 active/suspended 负责人时普通认证拒绝；unbound 后须重新认证。
- 快照、workflow 可选装配、事件幂等和注销新建语义与个人一致。

### 已采用的低影响假设

- 企业性质、行业等显示值沿用字典能力，核心持久值使用稳定 code。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 企业工作副本、材料矩阵、快照、workflow、版本/负责人发布、enterprise 投影 | T-01~04 | 转移验证码、管理员 override、前端 |

## 4. 要构建什么

申请人完整提交独立证据，流程审核精确 snapshot；finish 原子发布企业档案和唯一负责人。任何非 finish 终态、启动失败或重复事件都不改变当前企业控制权。

## 5. 实现契约

- **入口或接缝：** `/profile/enterprise/application` GET/POST、ProcessEvent、enterprise Profile 投影。
- **输入与输出：** 企业工作副本/submit -> 状态、snapshotVersion、本人负责人资格。
- **公共接口变化：** 新增企业申请 HTTP；变更 POST+安全 `@Log`。
- **不变量：** 企业活动身份/进行中申请/负责人唯一，快照不可变，发布原子。
- **状态或数据流：** draft -> snapshot -> workflow -> version/profile/responsible binding。
- **错误与失败行为：** 已绑定、缺授权/材料、工作流不可用、并发冲突均无部分发布。
- **兼容要求：** 不自动匹配法人用户，不改变 system/RBAC。
- **安全与隐私要求：** 探测不披露，日志不含企业/法人敏感载荷。

## 6. 执行路线

1. 建立字段/材料、探测、唯一负责人和 workflow 失败矩阵。
2. 实现工作副本、提交快照与办理身份规则。
3. 接入可选 WorkflowService 和精确企业 flowCode。
4. 实现幂等发布、注销重建及 enterprise 投影。
5. 运行并发、事务、权限和 full/core 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** enterprise application/persistence 与测试。
- **只读上下文：** T-01~04 和 SQL。
- **共享路径：** 无。
- **保留或不动：** transfer、admin、person、system/RBAC。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | service/Event/MySQL | 首次、重提、unbound 重认证、注销新建 | 正确版本/唯一负责人 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-07.md</Path>` |
| 失败 | matrix/concurrency | 已绑定、缺材料、workflow 缺失、重复事件 | 无顶替或部分状态 | 同上 |
| 回归 | API/ProfileService | full/core 与 system 检查 | enterprise 投影正确，RBAC 不变 | 同上 |

- **Workspace checks：** 定向 Maven 测试、编译与日志扫描。
- **E2E disposition：** required：HTTP、MySQL、workflow 与负责人发布需联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** commit、parent/candidate/result SHA 与事件/DB 结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01~04 -> enterprise 代码 -> 独立流程定义。
- **兼容窗口：** 企业 flowCode 和 snapshot 合同稳定。
- **监控信号：** 已绑定拒绝、发布冲突、流程不可用与迟到事件。
- **回滚或前向恢复：** 停止提交、保留证据、幂等前向恢复。
- **不可逆操作与批准点：** 生产流程/启用未授权。
- **收缩条件：** 不适用：无旧企业认证。

## 10. 验收标准

- [x] 所列 AC 与企业字段/材料/唯一负责人矩阵通过。
- [x] required E2E、Evidence、提交和父分支结果完整。
- [x] 无普通申请顶替或资料预填偏差。

## 11. 执行偏差

- Spec 已锁定企业邮箱、注册资本、行业和网站为可选档案字段，但 T-02 schema 未在企业工作副本、提交快照、主体、版本和管理员来源表中为其建列；仅写 `field_snapshot_json` 会在发布后丢失这四项。
- T-07 接管聚合仓库 `50-namewta-ddl.sql` 的最小 DDL 修正，在上述五张表增加 nullable 字段；不修改已有列、索引、DML、业务数据库或生产环境。源码在申请、快照、主体和版本全链持久化，管理员来源由 T-09 继续消费同一 schema。
