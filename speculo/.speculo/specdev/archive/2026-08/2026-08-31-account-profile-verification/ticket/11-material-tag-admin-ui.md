---
schema_version: 3
artifact: ticket
change: 2026-08-31-account-profile-verification
id: T-11
title: 交付材料分类与标签管理页面
status: done
planning_depth: standard
planning_depth_reason: 页面边界单一，树层级和系统必传标签约束已由后端及公共合同锁定。
ready: true
risk: medium
blocked_by: [T-10]
contract_ids: [AC-032, AC-033, AC-034, AC-035, AC-040]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/material-tag/**</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/material-tag/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/profile/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/**</Path>"
shared_paths: []
shared_path_owners: []
---

# Ticket T-11: 交付材料分类与标签管理页面

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/11-material-tag-admin-ui.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>`

## 1. 战略与来源

- **目标：** 在 admin-web 提供受限三层语义的材料分类与标签管理面。
- **可观察产出：** 管理员可用树表查看、创建、修改、排序和按允许规则停用节点，系统必传标签的不可变约束可见且不可误操作。
- **来源：** `US-016`、`US-017`、`AC-032` 至 `AC-035`、`AC-040`。
- **当前事实：** T-10 提供 service/runtime/componentKey，T-04 提供后端规则。
- **Planning Depth 原因：** 复用现有树表交互即可完成，风险集中在约束投影。

## 2. 决策状态

### 已锁定决策

- 一级固定个人/企业/通用；一级可挂标签或增加一层二级分类，分类不可作为材料标签选择值。
- 系统必传标签只允许改显示名和排序；被引用节点遵守后端拒绝，不提供物理删除幻觉。
- 按能力控制命令按钮，服务端仍是最终授权和结构校验者。

### 已采用的低影响假设

- 交互沿用 system 菜单/部门的树表、父节点选择和对话框模式。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 树表、编辑表单、状态/约束投影、组件测试 | T-10 runtime、system 树表范式 | 材料上传、个人/企业详情、App 组合 |

## 4. 要构建什么

实现材料标签注册页面和子组件，以 nodeType/parentId 构树；表单根据层级、父类型、systemRequired 与引用状态限制可用动作，并明确展示后端拒绝。

## 5. 实现契约

- **入口或接缝：** T-10 预注册的材料标签 componentKey。
- **输入与输出：** 树查询/节点命令 -> 树表刷新与可恢复反馈。
- **公共接口变化：** 无；仅消费 T-10 合同。
- **不变量：** 标签恰好一个父分类；不可产生非法深度；系统 code 不可编辑。
- **状态或数据流：** load tree -> permission/state projection -> command -> reload。
- **错误与失败行为：** 后端引用/规则冲突保留当前数据并显示业务错误。
- **兼容要求：** 不修改 profile package exports 或 App 注册。
- **安全与隐私要求：** 页面不接触身份明文或材料文件内容。

## 6. 执行路线

1. 建立树形 view model 与合法父节点选择器。
2. 实现列表、创建/编辑/排序/停用动作和权限投影。
3. 覆盖非法层级、必传标签、引用冲突和刷新状态测试。
4. 运行 web-domain 定向测试、lint、typecheck。

## 7. 路径访问契约

- **预计修改点/可写范围：** `material-tag` 子树。
- **只读上下文：** T-10 profile 合同与 system 树表实现。
- **共享路径：** 无。
- **保留或不动：** web-domain 根入口、个人/企业页面、App 与锁文件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常 | Vitest/component | 加载树、合法增改排停 | 树结构和刷新正确 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| 失败 | form/service mock | 非法父级、系统标签、引用冲突 | UI 禁用或显示后端拒绝 | 同上 |
| 回归 | typecheck/lint | web-domain filters | 不改公共合同和其他页面 | 同上 |

- **Workspace checks：** profile web-domain test、lint、typecheck。
- **E2E disposition：** not-required：单页组件行为在本票验证，动态菜单和真实后端由 T-14 E2E。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；仅执行组件合同检查，真实 E2E 归 T-14。
- **Integration evidence：** 组件测试、状态截图/记录和提交 SHA。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-02 种子与 T-04 API 先于页面开放。
- **兼容窗口：** 无菜单组合时页面不可达且无副作用。
- **监控信号：** 标签结构冲突与权限拒绝。
- **回滚或前向恢复：** 移除注册组合；数据不回滚。
- **不可逆操作与批准点：** 生产标签变更需正常权限，票据不授权。
- **收缩条件：** 不适用。

## 10. 验收标准

- [x] 合法树操作、系统必传约束和引用冲突都有自动化覆盖。
- [x] 无身份明文、物理删除或 package 共享路径修改。
- [x] Evidence 和集成结果完整。
