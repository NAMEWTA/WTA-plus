---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-13
title: 交付企业档案管理列表、详情与负责人操作
status: done
planning_depth: deep
planning_depth_reason: 企业详情同时承载法律主体明文、唯一负责人、转移历史、workflow 和管理员覆盖操作。
ready: true
risk: high
blocked_by: [T-10]
contract_ids: [AC-018, AC-019, AC-020, AC-021, AC-022, AC-023, AC-024, AC-025, AC-026, AC-027, AC-028, AC-029, AC-030, AC-031, AC-039, AC-040, AC-041]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/enterprise/**</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/enterprise/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/profile/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/workflow/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-13: 交付企业档案管理列表、详情与负责人操作

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/13-enterprise-profile-admin-ui.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-13.md</Path>`

## 1. 战略与来源

- **目标：** 交付企业档案管理列表、独立详情、审核上下文和负责人相关运营操作。
- **可观察产出：** 有权人员可查企业明文与完整历史，办理审核、直建/修订、绑定处置、负责人指定和注销；唯一负责人/目标资格冲突准确失败。
- **来源：** `US-009` 至 `US-015`、`US-021`、`US-022` 及所列 AC。
- **当前事实：** T-10 提供公共合同，T-08/T-09 提供转移与管理 API。
- **Planning Depth 原因：** 页面要区分自助转移证据与管理员直接指定，且所有终态需保持可审计。

## 2. 决策状态

### 已锁定决策

- 企业列表默认排除 revoked，不提供导出；详情展示企业版本、申请、材料、负责人绑定事件、流程与审计。
- 法定代表人是企业字段，不等于系统负责人；管理员指定目标仍必须满足个人 active 和企业绑定资格。
- 管理员直建/指定不触发短信；自助验证码挑战仅作为详情历史展示，不在管理 UI 代替用户发起。
- 高风险操作详情页确认且原因必填；revoked 全量只读。

### 已采用的低影响假设

- 与个人页面共享视觉/运行时范式，但各自在独立子树实现，不抽取跨票据可写组件。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 企业列表/详情/审核/直建/修订/处置/负责人指定 UI | T-10 runtime、workflow/OSS host | 申请人端转移 UI、个人/标签页、App 组合 |

## 4. 要构建什么

实现企业档案页面集合，以统一信用代码、企业名称、状态等必要字段检索；详情明确法律主体和认证负责人两个概念，展示完整负责人历史并对管理操作执行资格、权限和状态投影。

## 5. 实现契约

- **入口或接缝：** 企业列表、详情和 workflow formPath componentKey。
- **输入与输出：** 查询/profileId/review/override/manage 命令 -> 企业管理视图与结果。
- **公共接口变化：** 无；消费 T-10 合同。
- **不变量：** 唯一负责人；无 export/DELETE；历史不可改；revoked 只读。
- **状态或数据流：** list -> detail/context -> command eligibility -> confirm/reason -> refetch。
- **错误与失败行为：** 目标资格、绑定版本或 workflow 冲突不做局部 UI 提交，刷新权威状态。
- **兼容要求：** componentKey/权限 ID 与 T-10 一致。
- **安全与隐私要求：** 明文与材料仅授权可见；确认/错误/前端日志不复制敏感值。

## 6. 执行路线

1. 建立企业/负责人/申请状态与权限动作矩阵。
2. 实现列表、默认过滤和独立详情分区。
3. 实现审核、直建、修订、绑定处置、负责人指定和注销交互。
4. 覆盖法定代表人与负责人区分、目标不合格、冲突回刷和 revoked 只读。
5. 运行定向测试、lint、typecheck。

## 7. 路径访问契约

- **预计修改点/可写范围：** profile web-domain `enterprise` 子树。
- **只读上下文：** T-10 合同与 workflow 页面范式。
- **共享路径：** 无。
- **保留或不动：** 公共入口、个人/标签页面、App、lock。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | component/Vitest | 列表、详情、审核、直建、修订、负责人处置 | 字段/历史/命令正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-13.md</Path>` |
| 失败 | state/service mock | 目标不合格、唯一性冲突、终止失败、revoked 写入 | 不误报成功且回刷 | 同上 |
| 回归 | typecheck/lint | profile web-domain filters | formPath/动态注册稳定 | 同上 |

- **Workspace checks：** profile web-domain test、lint、typecheck。
- **E2E disposition：** not-required：页面组件在本票验证，真实权限/workflow/材料链路由 T-14 E2E。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；仅执行组件合同检查，真实 E2E 归 T-14。
- **Integration evidence：** 状态矩阵、组件测试和提交 SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** 企业后端与公共合同先发布，T-14 后组合。
- **兼容窗口：** 未组合时页面不可达。
- **监控信号：** 负责人冲突、资格变化、override/workflow 失败。
- **回滚或前向恢复：** 移除 manifest；已执行绑定事件只前向处置。
- **不可逆操作与批准点：** 生产负责人指定/revoke 需权限与确认，票据不授权。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] 企业主体/负责人概念、状态、权限、历史和高风险动作矩阵通过。
- [x] revoked 只读、无导出、无物理删除和失败回刷符合合同。
- [x] Evidence 和集成结果完整。
