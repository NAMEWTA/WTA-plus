---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-01
title: 建立 Profile 模块、公共查询合同与 Workflow 终止接缝
status: done
planning_depth: deep
planning_depth_reason: 新增 Maven 模块图、跨模块 Java API 和工作流终止公共合同，影响 full/core 兼容与后续全部切片。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-025, AC-026, AC-038, AC-044]
owner: codex:/root
expected_changes:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/**</Path>"
writable_paths:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-bom/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-person/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-profile/ruoyi-profile-enterprise/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/impl/WorkflowServiceImpl.java</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/profile/contract/**</Path>"
read_only_paths:
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/**</Path>"
shared_paths:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/**</Path>"
shared_path_owners:
  - "<Path>ruoyi-vue-plus-namewta/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-modules/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/profile/api/**</Path> => T-01"
  - "<Path>ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/workflow/api/**</Path> => T-01"
---

# Ticket T-01: 建立 Profile 模块、公共查询合同与 Workflow 终止接缝

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/01-profile-contracts-and-workflow-seam.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 建立后续个人、企业、材料和管理切片共同依赖的模块与公开接缝。
- **可观察产出：** Maven reactor 同时识别聚合、BOM 和两个叶子；full/core 均可装配 profile；消费者可编译调用非敏感 ProfileService；workflow 可按 businessId 幂等终止活动实例。
- **来源：** `US-012`、`US-020`、`AC-025`、`AC-026`、`AC-038`、`AC-044`、`ADR-001`、`ADR-011`、`ADR-014`。
- **当前事实：** 当前不存在 profile artifact/ProfileService，WorkflowService 只有查询、启动和删除等合同。
- **Planning Depth 原因：** 公共 API、共享 POM 与工作流兼容错误会阻塞全部后续 Ticket。

## 2. 决策状态

### 已锁定决策

- 只创建已确认的聚合、BOM、person 和 enterprise artifact；不增加第三个业务 artifact。
- ProfileService 只返回有效状态和非敏感摘要；组合实现通过 ruoyi-api 窄端口汇聚两个叶子。
- person 叶子作为共享 profile 门面与材料目录实现 owner，enterprise 仅通过 ruoyi-api 调用；两叶子始终共同装配。
- Workflow 终止以 businessId、原因和幂等结果表达，不暴露 WarmFlow 类型或删除历史。

### 已采用的低影响假设

- 公共 DTO 使用不可变 record/普通 DTO 取决于当前 ruoyi-api 邻近风格，字段语义不变。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| POM/BOM、ProfileService/贡献端口、Workflow 终止合同与实现 | UserService、现有 WorkflowService/实例服务 | profile 业务表、申请逻辑、前端与生产发布 |

## 4. 要构建什么

后续业务模块可只依赖 ruoyi-api 查询个人/企业认证状态或请求终止活动流程；缺少 enterprise/person 贡献时返回未认证而非启动失败。终止无活动实例可幂等返回，终止失败必须明确失败并保留流程历史。

## 5. 实现契约

- **入口或接缝：** ProfileService 单个/批量查询；Profile 投影贡献端口；WorkflowService terminate-by-businessId。
- **输入与输出：** userId 集合或 businessId+reason -> 稳定 DTO/终止结果。
- **公共接口变化：** 新增 `org.dromara.profile.api`；兼容扩展 WorkflowService。
- **不变量：** DTO 不含证件号、材料、申请；业务模块不依赖 workflow/system 实现。
- **状态或数据流：** facade 汇聚 person/enterprise 投影；workflow 实现查活动实例后终止并保留轨迹。
- **错误与失败行为：** 非法 ID、重复终止和引擎失败均有可判定结果；不能伪造成功。
- **兼容要求：** 现有 WorkflowService 调用方源码/行为不变；core 无 workflow Bean 仍启动。
- **安全与隐私要求：** 公共摘要字段白名单测试，禁止敏感字段扩散。

## 6. 执行路线

1. 先写模块图、Profile DTO 白名单和 workflow 终止合同测试。
2. 注册聚合/BOM/叶子及 full/core 依赖，锁定所需 common 依赖全集。
3. 增加 ProfileService 与组合贡献端口，并实现空/批量语义。
4. 扩展 WorkflowService 与实现，覆盖无实例、活动、终态、重复和失败。
5. 运行 API/模块定向测试以及 full/core reactor 编译。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 所列 POM、ruoyi-api、WorkflowServiceImpl 和合同测试。
- **只读上下文：** workflow 现有实例服务与 system 公开 API。
- **共享路径：** 所列 POM 与 ruoyi-api 只由 T-01 修改，后续 Ticket 只读。
- **保留或不动：** system 内部 service/mapper、WarmFlow 类型边界和认证/RBAC 行为。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | API/模块合同测试 | `./mvnw -pl ruoyi-admin -am -Dtest='*Profile*ContractTest,*Workflow*ContractTest' -Dsurefire.failIfNoSpecifiedTests=false test` | 聚合查询与活动实例终止通过 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>` |
| 失败 | workflow fault/DTO scan | 无实例、终态、异常与字段白名单矩阵 | 幂等或明确失败，无敏感 DTO | 同上 |
| 回归 | Maven 双 bundle | full 测试后分别 package full/core | 两种组合可构建，core 无 workflow 仍启动 | 同上 |

- **Workspace checks：** 按 Goal Plan 在所选 workspace 运行定向测试和 Maven 编译。
- **E2E disposition：** not-required：本 Ticket 是共享合同 prefactor，跨边界业务由 T-05/T-07/T-09/T-14 验证。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 先 additive 合同和模块，再由消费者实现。
- **兼容窗口：** WorkflowService 只增加方法；消费者合入前不激活 profile 行为。
- **监控信号：** 终止失败类别与 businessId 安全审计。
- **回滚或前向恢复：** 消费者合入前可回滚；合入后保持 API 并前向修复。
- **不可逆操作与批准点：** 无生产动作；实现 commit/父分支推进需授权。
- **收缩条件：** 不适用：无旧接口迁移。

## 10. 验收标准

- [x] AC-025/026/038/044 的公共接缝与回归成立。
- [x] POM、ruoyi-api 与 workflow shared path 仅由 T-01 修改。
- [x] 验证矩阵和提交/集成结果记录到 `<Path>{roots.state}/specdev/changes/{change}/evidence/T-01.md</Path>`。
- [x] E2E disposition 已执行，无未批准偏差。
