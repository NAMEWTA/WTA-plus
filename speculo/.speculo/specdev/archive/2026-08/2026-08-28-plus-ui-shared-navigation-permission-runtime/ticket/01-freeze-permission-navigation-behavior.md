---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-01
title: 冻结权限与导航跨边界行为基线
status: done
planning_depth: deep
planning_depth_reason: 认证后导航恢复和失败关闭属于高事故半径安全路径，必须先建立浏览器级行为基线再迁移共享核心。
ready: true
risk: high
blocked_by: []
contract_ids: [AC-005, AC-006, AC-008, AC-009, AC-011, AC-012, AC-014]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/e2e/app-runtime-baseline.spec.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/e2e/app-runtime-baseline.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/apps/admin-web/src/**</Path>", "<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 冻结权限与导航跨边界行为基线

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/01-freeze-permission-navigation-behavior.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 在改动共享权限和菜单实现前，用真实 Admin 浏览器边界冻结服务端菜单、manifest、权限可见性、请求次数和失败关闭行为。
- **可观察产出：** 重构前后的同一组 Playwright 场景可证明登录只触发一次身份/菜单请求、所选 manifest 页面可达、受限操作不可见、未知组件显示稳定诊断且不会加载任意本地页面。
- **来源：** `US-004`、`US-005`、`US-006`、`AC-005`、`AC-006`、`AC-008`、`AC-009`、`AC-011`、`AC-012`、`AC-014`、`USER-DECISION:每阶段先保持绿色`。
- **当前事实：** `<Path>plus-ui-namewta/e2e/app-runtime-baseline.spec.ts</Path>` 已覆盖登录、`getInfo`、`getRouters` 与重定向，但菜单 fixture 仍使用会被本次删除的 App 本地页面键，且没有同一接缝下的未知组件诊断断言。
- **Planning Depth 原因：** 该 Ticket 不改变生产行为，但它定义后续认证、权限与动态路由迁移的安全判定基线。

## 2. 决策状态

### 已锁定决策

- 基线菜单必须使用当前 Admin 已选择的 Web Domain manifest 键，不得把即将删除的本地 `views` 兜底冻结成正确行为。
- 请求顺序以浏览器可观察网络事件冻结为 `getInfo -> getRouters`；`addRoute -> replace` 由稳定单元接缝补证。
- 未知组件键展示诊断组件，但不得加载未知实际页面；后端菜单仍是 Client 裁剪权威。

### 已采用的低影响假设

- 在现有基线文件中扩展 fixture 与场景，避免新建重复 API mock。验证方式是 Playwright discovery 和定向运行。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 选中 manifest 的成功场景、权限失败关闭场景、未知组件诊断场景、请求次数与顺序断言 | 现有 Playwright API 拦截、Admin 登录页、manifest registry 与诊断 UI | 不修改生产源码，不测试具体内部函数，不激活其他 App |

## 4. 要构建什么

浏览器以受控 Client、身份、权限和菜单响应登录 Admin。成功场景返回当前已选择 manifest 中的动态组件键，页面完成注册并恢复到原目标；权限不足时对应操作不可见但服务端菜单不被前端二次删除；未知组件键时只显示带 app、domain、key 的稳定诊断。所有场景核对请求次数、未知请求和目标 URL，形成后续 Ticket 不得破坏的外部基线。

## 5. 实现契约

- **入口或接缝：** Playwright 启动的 Admin 页面与 `**/prod-api/**` 拦截。
- **输入与输出：** 输入为 Client context、身份权限和 RouterVo fixture；输出为页面 URL、可见内容、诊断内容和网络事件序列。
- **公共接口变化：** 无。
- **不变量：** 不冻结本地 `views` 兜底；每次身份和菜单请求各一次；权限不足不改变服务端菜单选择。
- **状态或数据流：** 登录成功后 `getInfo`，随后 `getRouters`，动态注册完成后恢复目标。
- **错误与失败行为：** 未知组件不执行任意页面加载，不出现空白页，并显示稳定诊断；缺少权限不显示受限操作。
- **兼容要求：** 无；测试 fixture 直接迁移到目标 manifest 合同。
- **安全与隐私要求：** 不记录 Token 明文或真实凭据；权限与未知组件场景均失败关闭。

## 6. 执行路线

1. 复核现有基线 fixture，先将动态组件键改为当前已选择的 manifest 注册项并保持原成功场景绿色。
2. 扩展受控权限输入与页面断言，冻结“菜单保留、受限操作隐藏”的差异。
3. 增加未知组件键场景，断言稳定诊断上下文、目标不被任意本地页面替代。
4. 运行定向 Playwright 与前端 lint/typecheck，形成行为冻结 checkpoint。

## 7. 路径访问契约

- **预计修改点：** `<Path>plus-ui-namewta/e2e/app-runtime-baseline.spec.ts</Path>`。
- **可写范围：** 仅该 E2E 文件。
- **只读上下文：** Admin 源码、Platform 与 Web Domain manifest。
- **共享路径：** 无。
- **保留或不动：** 所有生产源码、后端和其他 E2E 场景。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Admin 浏览器 | 定向运行 `e2e/app-runtime-baseline.spec.ts` 成功场景 | manifest 页面可达，请求顺序和次数准确 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-01.md</Path>` |
| 失败路径 | Admin 浏览器 | 定向运行权限不足与未知组件场景 | 控件和未知页面失败关闭，诊断包含稳定上下文 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-01.md</Path>` |
| 回归 | 前端静态检查 | `pnpm lint`、`pnpm typecheck` | E2E fixture 与项目类型/规范一致 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-01.md</Path>` |

- **Workspace checks：** current 策略在 current-workspace 运行；required 策略在 source-worktree 运行 lint/typecheck，E2E 只由 Lead 在 parent-candidate 运行。
- **E2E disposition：** required：本 Ticket 的交付物就是认证、权限和动态路由浏览器基线。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；不得在 source-worktree 声明 E2E 通过。
- **Integration evidence：** 记录 implementation/source commit、parent before、适用 candidate/result SHA、父分支包含关系与 Lead 复跑结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** 必须先于所有生产实现 Ticket 完成并绿色。
- **兼容窗口：** 不适用；只迁移测试 fixture 到已接受的 manifest 合同。
- **监控信号：** Playwright 场景结果、请求次数、未知请求列表和诊断文本。
- **回滚或前向恢复：** 未通过时回退本 Ticket checkpoint，不允许带红进入共享能力阶段。
- **不可逆操作与批准点：** 无；implementation commit 和父分支推进仍需授权。
- **收缩条件：** 不适用；本 Ticket 不建立临时生产入口。

## 10. 验收标准

- [x] `AC-005`、`AC-006`、`AC-008`、`AC-009`、`AC-011`、`AC-012` 的浏览器基线可重复执行。
- [x] fixture 不再依赖 App 本地动态 `views` 兜底。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-01.md</Path>`。
- [x] 修改未超出 `writable_paths`，并形成非空 implementation/source commit。
- [x] Lead 完成 direct-parent 或 parent-candidate E2E，记录 result SHA 与父分支包含关系。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 状态一致。
