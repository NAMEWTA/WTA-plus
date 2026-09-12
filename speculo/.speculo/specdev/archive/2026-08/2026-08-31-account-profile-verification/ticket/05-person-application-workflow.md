---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-05
title: 交付个人申请、不可变快照与 Workflow 发布闭环
status: done
planning_depth: deep
planning_depth_reason: 个人明文身份、并发唯一性、不可变快照和流程终态原子发布直接影响认证完整性。
ready: true
risk: critical
blocked_by: [T-03, T-04]
contract_ids: [AC-001, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-014, AC-015, AC-038]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/persistence/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialRepository.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/MybatisProfileMaterialRepository.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/mapper/ProfileMaterialMapper.java</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/person/persistence/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/person/application/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialRepository.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/MybatisProfileMaterialRepository.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/mapper/ProfileMaterialMapper.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/shared/material/ProfileMaterialServiceTest.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/test/java/org/dromara/profile/shared/material/ProfileMaterialMySqlE2ETest.java</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialService.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialRepository.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/MybatisProfileMaterialRepository.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/mapper/ProfileMaterialMapper.java</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialService.java</Path> => T-05"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/ProfileMaterialRepository.java</Path> => T-05"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/MybatisProfileMaterialRepository.java</Path> => T-05"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/src/main/java/org/dromara/profile/shared/material/mapper/ProfileMaterialMapper.java</Path> => T-05"
---

# Ticket T-05: 交付个人申请、不可变快照与 Workflow 发布闭环

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/05-person-application-workflow.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 从当前账户草稿到个人流程终态形成首条完整认证闭环。
- **可观察产出：** 有 apply 权限的账户可保存/重提，审核看到精确快照；finish 原子发布档案/绑定，其他终态不污染旧认证。
- **来源：** `US-001`、`US-003` 至 `US-005`、`US-011`、对应 AC、`ADR-002`、`ADR-003`、`ADR-015`、`ADR-024`。
- **当前事实：** T-01 至 T-04 提供合同、schema、provider 和材料，尚无个人申请状态机。
- **Planning Depth 原因：** 身份唯一、流程竞态和发布事务错误可生成双档案或污染有效身份。

## 2. 决策状态

### 已锁定决策

- 当前用户且有 `profile:person:apply` 才能操作自己的工作副本；不接受请求指定 userId。
- 保存姓名、证件号、性别、出生日期、有效期；只支持已锁定大陆/港澳台证件目录。
- 每次提交冻结字段、材料、provider 和递增 snapshotVersion；申请 ID 始终为 businessId。
- draft/back/cancel 可编辑重提，waiting 只读，finish/invalid/termination 关闭；旧有效绑定待审时继续有效。
- unbound 未注销档案通过完整认证复用；已注销身份创建新 ID 和 previousProfileId。

### 已采用的低影响假设

- flowCode/config key 采用 profile person 稳定命名并由 T-02 配置项承载。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 工作副本、证件矩阵、提交快照、流程启动/事件、档案/绑定发布、person 投影 | T-01 API、T-03 provider、T-04 材料、WorkflowService | 他人 active 换绑、本人解绑、管理 UI |

## 4. 要构建什么

申请人完整填写并挂接材料后提交；服务端先冻结快照并可靠启动流程。只有精确 flowCode 的 finish 对当前 snapshot 发布版本/绑定；退回、撤销、作废、终止或迟到旧版本事件不改变有效档案。

## 5. 实现契约

- **入口或接缝：** `/profile/person/application` 当前用户 GET/POST、ProcessEvent listener、ProfileService person 投影。
- **输入与输出：** 工作副本/submit -> 申请状态、snapshotVersion、当前本人资格。
- **公共接口变化：** 新增个人申请 HTTP；变更 POST+安全 `@Log`。
- **不变量：** 一账户/身份一个进行中申请；快照不可变；finish 发布原子且幂等。
- **状态或数据流：** draft -> snapshot -> workflow waiting -> event -> version/profile/binding。
- **错误与失败行为：** 材料/证件/provider/唯一性/工作流不可用时失败且无伪 waiting。
- **兼容要求：** 不修改 system 用户/RBAC；core 查询/草稿可用。
- **安全与隐私要求：** 只返回本人数据，日志排除全部身份与材料字段。

## 6. 执行路线

1. 建立状态、唯一性、快照与 workflow 缺失失败测试。
2. 实现工作副本、证件/材料校验和提交快照事务。
3. 连接可选 WorkflowService，保证启动失败可恢复。
4. 实现精确 flowCode 事件、幂等发布、认领/注销新建与 person 投影。
5. 运行并发、回滚、模块与 full/core 回归。

## 7. 路径访问契约

- **预计修改点/可写范围：** person application/persistence 与测试子树。
- **只读上下文：** T-01 至 T-04 产物和 SQL。
- **共享路径：** T-05 临时拥有材料 service/repository/mapper 的最小接缝修正；不改公开端口。
- **保留或不动：** rebind、通知、企业与管理员命令。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | service/Event/MySQL | 首次、退回重提、unbound 认领、注销新建 | 正确快照、版本和绑定 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-05.md</Path>` |
| 失败 | fault/concurrency | workflow 缺失、重复提交、迟到事件、回滚 | 无双记录或伪 waiting | 同上 |
| 回归 | API/模块 | 权限、ProfileService、full/core | system/RBAC 不变，core 可启动 | 同上 |

- **Workspace checks：** 定向 Maven 测试、编译与敏感日志扫描。
- **E2E disposition：** required：HTTP、MySQL、workflow 事件和事务发布需联合验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate，使用测试流程定义。
- **Integration evidence：** commit、parent/candidate/result SHA 与 DB/事件记录。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01~04 -> person 代码 -> 流程定义 -> 默认授权关闭。
- **兼容窗口：** flowCode/snapshotVersion 发布后稳定。
- **监控信号：** 提交失败、事件忽略、发布冲突与 workflow 不可用。
- **回滚或前向恢复：** 停止提交并保留草稿/快照；前向重放幂等事件。
- **不可逆操作与批准点：** 生产流程发布/SQL/启用未授权。
- **收缩条件：** 不适用：无旧个人认证。

## 10. 验收标准

- [x] 所列 AC 正常、失败、并发和 core 回归通过。
- [x] required E2E、Evidence、提交和父分支结果完整。
- [x] 无越界或未批准偏差。

## 11. 执行偏差

- T-04 的共享材料实现只允许 `WORKING -> immutable`，无法按本 Ticket 已锁定合同把审批提交材料冻结为档案版本材料；同时工作材料写入未校验申请状态，`WAITING` 仍可能被修改。
- T-05 仅接管上述四个共享实现文件、一个聚焦测试及受新状态门禁影响的既有 MySQL E2E 夹具，增加 `WORKING` 可编辑状态检查与受关系约束的 `SUBMISSION -> VERSION` 复制；`ProfileMaterialPort`、schema、OSS 生命周期和企业 adapter 均不改变。
