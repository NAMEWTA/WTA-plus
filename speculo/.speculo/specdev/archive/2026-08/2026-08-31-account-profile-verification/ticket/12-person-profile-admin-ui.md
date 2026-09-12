---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-12
title: 交付个人档案管理列表、详情与审核操作
status: done
planning_depth: deep
planning_depth_reason: 页面同时呈现明文身份、不可变历史、材料与 workflow，并承载覆盖决定和注销等高风险命令。
ready: true
risk: high
blocked_by: [T-10]
contract_ids: [AC-010, AC-011, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-024, AC-025, AC-026, AC-027, AC-028, AC-029, AC-030, AC-031, AC-039, AC-040, AC-041]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/person/**</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/person/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/profile/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/workflow/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-12: 交付个人档案管理列表、详情与审核操作

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/12-person-profile-admin-ui.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>`

## 1. 战略与来源

- **目标：** 交付个人档案管理列表、独立详情、workflow 审核上下文和有确认的管理命令。
- **可观察产出：** 有权人员可查看合同内全量明文、版本/申请/材料/绑定/流程/审计历史并完成审核、直建、修订、绑定处置与注销；无权动作不可用且后端拒绝。
- **来源：** `US-006` 至 `US-008`、`US-011` 至 `US-015`、`US-021`、`US-022` 及所列 AC。
- **当前事实：** T-10 提供公共合同，T-09 提供管理 API。
- **Planning Depth 原因：** 高风险命令、终态只读与 workflow 路由必须保持一致。

## 2. 决策状态

### 已锁定决策

- 列表默认排除 revoked，可显式筛选；不提供批量导出。
- 详情分区显示 current、来源版本、submission snapshot、材料、绑定历史、流程和审计。
- 有权用户看明文且不叠加部门数据权限；无权不能靠前端绕过。
- 高风险动作只在详情发起，必须确认并填写原因；revoked 详情全量只读。
- workflow formPath 直接解析个人审核上下文，不要求额外 query/material 权限。

### 已采用的低影响假设

- 表格、详情抽屉/页、确认对话框和材料预览复用现有 admin-web 宿主组件。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 个人列表/详情/审核/直建/覆盖/处置 UI 与测试 | T-10 runtime、workflow/OSS host | 申请人自助 UI、企业页面、App 组合 |

## 4. 要构建什么

实现个人档案页面集合，确保列表扫描、独立详情和审核路径一致使用不可变来源；每个命令按 profile 能力投影，并在注销、等待覆盖、换绑历史等状态下呈现准确动作集。

## 5. 实现契约

- **入口或接缝：** 个人列表、详情和 workflow formPath componentKey。
- **输入与输出：** 查询条件/profileId/review 或 override 命令 -> 列表、详情分区与确认结果。
- **公共接口变化：** 无；仅消费 T-10 service/runtime。
- **不变量：** 无 export/DELETE；历史不可编辑；revoked 终态只读；原因必填。
- **状态或数据流：** list -> detail/context -> permission/state decision -> command -> refetch。
- **错误与失败行为：** 冲突、流程终止失败、资格变化不做乐观成功，刷新服务端状态。
- **兼容要求：** componentKey 与 T-10 清单一致，不改公共 exports。
- **安全与隐私要求：** 明文只在授权页面；日志、错误和确认摘要避免复制证件号/手机号等敏感原文。

## 6. 执行路线

1. 建立权限/档案/申请状态到可见字段和动作的矩阵测试。
2. 实现列表筛选、分页、默认排除注销和独立详情。
3. 实现审核上下文、材料访问、直建/修订/状态/注销对话框。
4. 覆盖等待覆盖、冲突回刷、只读终态和无权行为。
5. 运行定向测试、lint、typecheck。

## 7. 路径访问契约

- **预计修改点/可写范围：** profile web-domain `person` 子树。
- **只读上下文：** T-10 合同与 workflow 页面范式。
- **共享路径：** 无。
- **保留或不动：** 公共入口、企业/标签页面、App、lock。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | component/Vitest | 列表、详情、审核、直建、修订、处置 | 分区/命令/回刷正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| 失败 | permission/state mock | 无权、终止失败、并发冲突、revoked 写入 | 不误报成功，不泄漏/写入 | 同上 |
| 回归 | typecheck/lint | profile web-domain filters | formPath 和动态注册类型稳定 | 同上 |

- **Workspace checks：** profile web-domain test、lint、typecheck。
- **E2E disposition：** not-required：组件契约在本票验证，真实权限/workflow/材料链路由 T-14 E2E。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；仅执行组件合同检查，真实 E2E 归 T-14。
- **Integration evidence：** 权限/状态矩阵、组件测试和提交 SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 后端管理 API 和公共合同先发布，T-14 最后组合菜单。
- **兼容窗口：** 未组合时页面不可达。
- **监控信号：** 明文/材料访问拒绝、override 冲突和 workflow 终止失败。
- **回滚或前向恢复：** 移除 App manifest；已执行决定仅前向修订。
- **不可逆操作与批准点：** 生产 override/revoke 需业务权限与确认，票据不授权。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] 个人管理状态/权限/明文/材料/高风险命令矩阵通过。
- [x] revoked 只读、无导出、无物理删除和失败回刷符合合同。
- [x] Evidence 和集成结果完整。
