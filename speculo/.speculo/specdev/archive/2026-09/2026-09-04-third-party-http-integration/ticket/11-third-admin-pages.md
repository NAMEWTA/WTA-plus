---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-11
title: 交付三方接口管理四个 Admin 页面
status: done
planning_depth: standard
planning_depth_reason: 新增四个工作型 CRUD/查询页面、凭据编辑与权限状态，但完全消费已冻结 domain 合同且不拥有 App 组合。
ready: true
risk: medium
blocked_by: [T-10]
contract_ids: [AC-015, AC-016]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/third/**</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/third/**</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/third/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/profile/**</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/third/**</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/packages/web-domains/third/**</Path> => T-11"
---

# Ticket T-11: 交付三方接口管理四个 Admin 页面

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/11-third-admin-pages.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>`

## 1. 战略与来源

- **目标：** 提供供应商、Endpoint、脱敏调用记录和聚合统计四个可重复操作的管理界面。
- **可观察产出：** 管理员可高效筛选/维护启停与策略，在所属编辑流程替换凭据，并查看 requestId、结果和双维度统计；无权操作隐藏且后端拒绝仍正确呈现。
- **来源：** `US-002`、`US-003`、`US-004`、`US-007`、`US-008`、`AC-015`、`AC-016`、`ADR-012`。
- **当前事实：** T-10 提供 headless domain，无 third web-domain、manifest page exports 或 UI 状态。
- **Planning Depth 原因：** 多页面和表单状态较多，但均沿用既有 Admin CRUD 组件与稳定 transport。

## 2. 决策状态

### 已锁定决策

- 页面为 Provider、Endpoint、调用记录、调用统计；credential/policy 在 Provider/Endpoint 编辑流内，不提供凭据列表页。
- 组件键为 `third/provider/index`、`third/endpoint/index`、`third/invocation/index`、`third/statistics/index`。
- 页面工作密集、可扫描，使用项目表格/搜索/抽屉或对话框模式；不回显 secret、ciphertext 或完整 raw body。

### 已采用的低影响假设

- 统计使用现有图表依赖；若图表不足，先以可排序汇总表实现，不新增视觉依赖。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>、<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/implementation.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/crud-resource-slices.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path>。
- **目录与代码最低要求：** web-domain-third 只放 Vue 页面、局部状态、runtime port 和 manifest，目录与 domain 资源一一对应；页面组件使用既有命名/布局和显式 component key，公共逻辑提取到有真实 owner 的 composable，不创建 helpers/common 垃圾桶。测试与被测模块相邻，浏览器测试使用 e2e/**/*.spec.ts。
- **交互与安全要求：** 表格/表单/分页/加载/空态/错误态遵守现有 CRUD 资源切片；权限用 v-hasPermi 等既有机制，后端仍是权威。secret 输入提交即清空，敏感值不得进入 URL、store、DOM、快照或通知。
- **执行停止条件：** 页面直接请求后端、跨 domain deep import、catch-all manifest、把完整 raw body 做详情展示、隐藏后端 403、或以视觉通过替代类型/权限测试时立即停止。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| web-domain package、四页面、manifest exports、表单/表格/权限/状态测试 | domain-third、Element Plus、既有 CRUD hooks/components | App 注册、菜单 DML、独立 credential 页、完整报文查看 |

## 4. 要构建什么

管理员进入各资源页后可按编码、状态、时间和结果快速筛选；Provider/Endpoint 表单呈现继承和上限，危险输入显示后端/前端一致错误；凭据替换框提交后清空且只展示已配置掩码。调用记录只读且可用 requestId 关联日志，统计同时展示 Provider 与 Endpoint 维度及 quota 展示值。

## 5. 实现契约

- **入口或接缝：** web-domain page exports/manifest、domain-third services。
- **输入与输出：** 用户筛选/命令 → domain service → 可扫描状态、通知和刷新。
- **公共接口变化：** 新增 `@namewta/web-domain-third` 及四 component-key exports。
- **不变量：** 不深导入 App/system；secret 输入提交后清除；前端权限不是后端授权替代。
- **状态或数据流：** route page → query/form state → domain command/query → refresh；provider filter 驱动 endpoint。
- **错误与失败行为：** disabled/冲突/权限/CONFIG_UNAVAILABLE/RATE_LIMITED 以明确消息呈现，不泄露响应原文。
- **兼容要求：** 未由 App 注册前不影响现有路由。
- **安全与隐私要求：** DOM、store、URL、表格、通知和测试快照均不得出现 secret/ciphertext/raw body。

## 6. 执行路线

1. 建立 page manifest、权限、敏感 DOM 和关键状态组件测试。
2. 创建 web-domain package/runtime/pages exports。
3. 实现 Provider/Endpoint 管理及嵌套 credential/policy 编辑。
4. 实现 Invocation/Statistics 只读查询与空/加载/错误状态。
5. 运行 Vitest、vue-tsc、lint 和响应式布局检查。

## 7. 路径访问契约

- **预计修改点/可写范围：** 整个新 `packages/web-domains/third`，由 T-11 唯一写。
- **只读上下文：** domain-third 和邻近 system/profile web-domain。
- **共享路径：** 新 web-domain 包为 App 共享入口；T-12 只经 exports 消费。
- **保留或不动：** Admin App、pnpm lock、SQL、后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Vue component tests | 四页面查询、CRUD、凭据替换、刷新和统计 | 状态/请求/展示符合 domain 合同 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| 失败路径 | component/DOM tests | 无权限、禁用、冲突、网络失败、敏感 canary | 操作关闭或报错，DOM/store 无敏感值 | 同上 |
| 回归 | package gates | web-domain test/typecheck/lint | exports 稳定、无跨域深导入、文本不溢出 | 同上 |

- **Workspace checks：** source/current workspace 运行 web-domain test/typecheck/lint 和 architecture check。
- **E2E disposition：** not-required：页面组件在隔离 runtime 验证，真实菜单/权限/后端串联由 T-12。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 不适用：additive 且未组合页面包。
- **兼容窗口：** page exports 冻结后由 T-12 注册。
- **监控信号：** 不适用：未独立运行。
- **回滚或前向恢复：** App 组合前可整体回滚，组合后先撤菜单/manifest。
- **不可逆操作与批准点：** 无。
- **收缩条件：** 不适用：无旧页面。

## 10. 验收标准

- [x] `AC-015`：四页面和准确 component keys 可被 manifest 消费。
- [x] `AC-016`：页面/按钮权限和敏感信息防线成立。
- [x] 组件、typecheck、lint、架构与响应式验证记录完整。
- [x] E2E not-required 理由、提交和集成 Evidence 完整。
