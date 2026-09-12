---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-06
title: 修复权限指令异常路径的失败关闭
status: done
planning_depth: deep
planning_depth_reason: 该修复位于共享 Vue 权限宿主和认证授权可见性边界，必须同时证明稳定报错与真实 mounted 生命周期中 DOM 不可见。
ready: true
risk: high
blocked_by: [T-05]
contract_ids: [AC-002, AC-003, AC-004, AC-012, AC-014]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/packages/web-kit/permission/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/web-kit/permission/src/index.test.ts</Path>", "<Path>plus-ui-namewta/packages/web-kit/permission/README.md</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/web-kit/permission/src/index.ts</Path>", "<Path>plus-ui-namewta/packages/web-kit/permission/src/index.test.ts</Path>", "<Path>plus-ui-namewta/packages/web-kit/permission/README.md</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/platform/permission/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/directive/index.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/main.ts</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-06: 修复权限指令异常路径的失败关闭

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/06-fail-close-permission-directive-errors.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>`

## 1. 战略与来源

- **目标：** 使非法 `v-hasPermi`/`v-hasRoles` 绑定和 evaluator 缺失在 Vue mounted 异常路径中也先移除受限元素，再抛出稳定错误。
- **可观察产出：** 指令配置错误不会让受限按钮残留在 DOM；调用者仍得到可诊断的显式错误；正常允许、拒绝、超管和通配符语义不变。
- **来源：** `AC-002`、`AC-003`、`AC-004`、`AC-012`、`AC-014`、`CR-001` 规范轴 finding。
- **当前事实：** 当前 mounted hook 在 `requireValues`/`requireEvaluator` 抛错后不会到达移除分支，现有测试只证明 `toThrow`，没有通过 Vue 生命周期证明 DOM 失败关闭。
- **Planning Depth 原因：** 这是共享公共宿主和权限可见性合同，错误时序错误会扩散至所有 Web Domain 消费者。

## 2. 决策状态

### 已锁定决策

- 异常路径必须遵循 `remove element -> throw stable error`，不能吞错、默认放行或仅在正常拒绝时移除。
- 测试必须经过真实 Vue mount/custom renderer/component 生命周期，不能只直接调用 hook 并检查 `toThrow`。
- 继续复用 Platform `AccessEvaluator`；不复制权限算法，不让 Platform 引入 Vue/DOM。

### 已采用的低影响假设

- 保留当前公开安装入口和错误消息，除非现有测试证明错误文本不是合同；本 Ticket 不新增配置项。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 权限指令异常清理时序、真实生命周期测试、README 失败行为 | Platform evaluator、Admin provider 装配 | 不迁移 Admin 指令入口，不改权限算法、Router、Store 或后端鉴权 |

## 4. 要构建什么

当 Vue 把受限元素插入宿主后执行指令 mounted hook，正常绑定继续按 evaluator 结果保留或移除；非法绑定、缺失 evaluator 或求值前置条件异常时，宿主必须可靠移除元素并抛出稳定错误。组件测试直接检查最终 DOM，而不是把“抛错”误认为“不可见”。

## 5. 实现契约

- **入口或接缝：** Web Kit permission 指令工厂/安装入口与 Vue mounted 生命周期。
- **输入与输出：** 输入为 directive binding 和注入 evaluator；输出为保留元素、移除元素，或移除后抛出稳定错误。
- **公共接口变化：** 无；只纠正异常路径行为并补充文档。
- **不变量：** 正常允许/拒绝、超级管理员、通配符、any-match 语义与 `AccessEvaluator` 单一算法保持不变。
- **状态或数据流：** 不新增持久状态；清理必须幂等处理已脱离父节点的元素。
- **错误与失败行为：** 所有配置/依赖异常都失败关闭；错误继续显式传播，不静默降级。
- **兼容要求：** 无兼容开关；这是现有 Spec 合同的缺陷修复。
- **安全与隐私要求：** 前端可见性不是后端鉴权，但不得因宿主错误展示受限操作。

## 6. 执行路线

1. 先增加真实 Vue 生命周期测试，复现非法绑定和 evaluator 缺失时元素残留。
2. 调整 mounted 异常路径，使 DOM 清理先于稳定错误传播，并覆盖正常 allow/deny 回归。
3. 同步 README 的失败关闭语义，不扩大公开 API。
4. 在 source worktree 运行包测试、typecheck、lint 和 architecture；由 Lead 在 candidate 验证 Admin 组合回归。

## 7. 路径访问契约

- **预计修改点：** frontmatter 列出的 Web Kit 实现、测试和 README。
- **可写范围：** 仅 Web Kit permission 三个路径；越界必须停止。
- **只读上下文：** Platform evaluator 与 Admin 安装接缝。
- **共享路径：** 无。
- **保留或不动：** Admin Store/Router、Web Domain 页面、后端与 OpenAPI 产物。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Web Kit Vue component test | 运行 permission 包定向测试 | allow 保留；deny、角色/权限不匹配移除 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>` |
| 失败路径 | 真实 mount/custom renderer | 非法绑定、provider 缺失、evaluator 缺失 | DOM 无受限元素且抛出稳定错误 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>` |
| 回归 | Workspace 静态与组合检查 | package test、typecheck、lint、architecture | 无 API/依赖方向或正常语义回归 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>` |

- **Workspace checks：** source worktree 运行非 E2E 包级/静态检查；Lead 在 parent-candidate 重跑受影响 Admin 单元与静态检查。
- **E2E disposition：** not-required：配置错误无法从稳定生产页面入口触发，真实 Vue 生命周期测试是直接判卷接缝；T-08 承担完整 Admin 浏览器回归。
- **E2E owner/environment：** Lead / parent-candidate；本 Ticket 不单独声明 E2E 通过。
- **Integration evidence：** 记录 implementation/source commit、parent before、candidate/result SHA 和前端 main 包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 从 T-05 最终结果创建独立 source worktree；通过 candidate 后再推进 T-08。
- **兼容窗口：** 无；行为修复直接替代错误时序。
- **监控信号：** 生命周期测试、包测试、静态检查和 candidate 组合结果。
- **回滚或前向恢复：** candidate 失败时父分支不动，在原 source worktree 前向修复；不得恢复默认展示。
- **不可逆操作与批准点：** 实现 commit、candidate integration 和 source cleanup 均需新的用户授权。
- **收缩条件：** 不适用：不引入临时兼容路径。

## 10. 验收标准

- [ ] `AC-002`/`AC-012`：非法绑定与 evaluator 缺失时 DOM 不存在受限元素，同时抛出稳定错误。
- [ ] 正常 allow/deny、超级管理员、通配符和 any-match 行为全部回归通过。
- [ ] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-06.md</Path>`。
- [ ] 实际修改未超出 `writable_paths`，source/candidate/result 与 Map 状态一致。
