---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-08
title: 稳定并发 E2E 并完成最终验证
status: done
planning_depth: deep
planning_depth_reason: 该 Ticket 是 CR-001 整改后的最终认证、权限与动态导航浏览器 Gate，必须修复并发定位器并重新执行完整质量矩阵。
ready: true
risk: high
blocked_by: [T-06, T-07]
contract_ids: [AC-002, AC-005, AC-006, AC-008, AC-009, AC-011, AC-012, AC-014, AC-015]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>", "<Path>plus-ui-namewta/e2e/workflow-runtime.spec.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/e2e/system-resources.spec.ts</Path>", "<Path>plus-ui-namewta/e2e/workflow-runtime.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/apps/admin-web/**</Path>", "<Path>plus-ui-namewta/packages/domains/admin/**</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>", "<Path>plus-ui-namewta/packages/web-kit/permission/**</Path>", "<Path>plus-ui-namewta/tooling/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-08: 稳定并发 E2E 并完成最终验证

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/08-stabilize-e2e-and-final-validation.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-08.md</Path>`

## 1. 战略与来源

- **目标：** 将 CR-001 中两个依赖全局同名 overlay 的 locator 收窄到业务 owner，并以并发重复运行和完整质量矩阵证明整改可归档。
- **可观察产出：** social failure 与 workflow participant action 场景在 5 workers、0 retries 下连续稳定通过；T-06/T-07 集成结果通过完整 Admin Playwright 与根门禁。
- **来源：** `AC-002`、`AC-005`、`AC-006`、`AC-008`、`AC-009`、`AC-011`、`AC-012`、`AC-014`、`AC-015`、`CR-001` low finding。
- **当前事实：** CR-001 第一次完整 Playwright 为 37/39，两条定向单 worker 重跑和第二次完整运行通过；失败源是同名 Element Plus message/button 的 strict locator 并发时序。
- **Planning Depth 原因：** 此 Ticket 判定整个 change 的最终质量和复审输入，不能以单次重跑替代稳定证据。

## 2. 决策状态

### 已锁定决策

- locator 必须限定到具体 message/dialog/form owner 或唯一业务标识，不使用全局 `.first()`/`.last()` 掩盖歧义。
- 必要时显式等待上一个 overlay/transition 关闭；不得增加 retries、串行化整套测试或放宽断言制造绿色。
- 产品源码默认只读；若稳定定位需要新增产品 test id，必须停止并修订 Ticket 路径合同。
- 最终复审由 Lead 在 T-08 完成后另行形成 `CR-002`，source owner 不自批。

### 已采用的低影响假设

- 先复用 Element Plus dialog/message 的可访问 role、可见容器和现有业务文本；不为了测试改产品行为。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 两条 locator/transition 修复、并发重复运行、完整静态/单元/构建/E2E | T-06/T-07 已集成产品修复、现有 Playwright 配置和业务 fixture | 不改产品行为，不加 retries，不串行化全套，不在 Ticket 内自写 approve review |

## 4. 要构建什么

social failure 断言只观察对应业务动作产生的消息；workflow 确认操作只点击当前目标对话框的确认按钮，并在需要时等待前一层 overlay 完全关闭。两条用例先定向并发重复运行，再执行多次完整 5-worker/0-retry suite，最后在相同 candidate 上运行全部根质量门禁，为独立 CR-002 提供固定输入。

## 5. 实现契约

- **入口或接缝：** 两个 Playwright spec 的 overlay/message/dialog locator 与最终根验证命令。
- **输入与输出：** 业务页面与并发 overlay 状态 -> 唯一目标 locator -> 稳定行为断言。
- **公共接口变化：** 无。
- **不变量：** 用户流程、权限/菜单行为、Playwright workers/retries 基准和断言强度不变。
- **状态或数据流：** 测试必须等待可观察 UI 状态，不依赖隐式时间或其他测试产生的 overlay。
- **错误与失败行为：** 任一重复运行、完整 suite 或根门禁失败都阻止 T-08 和 CR-002。
- **兼容要求：** 不适用：只修测试判卷接缝。
- **安全与隐私要求：** fixture 和 Evidence 不记录真实凭据；认证/权限失败关闭场景必须保留。

## 6. 执行路线

1. 在未修改状态复现/审查两个歧义 locator，确认其具体 dialog/message owner 和 transition。
2. 仅在两个授权 E2E 文件内收窄 locator 与等待条件，定向运行相关场景。
3. 使用 `--workers=5 --retries=0` 连续运行完整 Playwright 至少 3 次，任何失败都先诊断而非重跑掩盖。
4. 在同一 parent-candidate 运行 architecture、lint、typecheck、workspace test、build:dev、build:prod 和最终完整 Playwright。
5. 记录固定 SHA、命令、退出码和测试数，交由 Lead 创建 CR-002 独立复审。

## 7. 路径访问契约

- **预计修改点：** 仅两个 E2E spec。
- **可写范围：** 与 frontmatter 完全一致；产品 test id 需求属于路径偏差，必须暂停。
- **只读上下文：** T-06/T-07 产品实现、架构工具和其余 E2E。
- **共享路径：** 无。
- **保留或不动：** Playwright 全局 worker/retry 配置、产品源码、已有业务断言和 fixture 隔离策略。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 两条定向 E2E | 定向运行 social failure/workflow participant action | 每个 locator 唯一且业务结果通过 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-08.md</Path>` |
| 失败路径 | 并发稳定性 | 完整 suite `--workers=5 --retries=0` 连续至少 3 次 | 无 strict locator、overlay 时序或重试依赖 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-08.md</Path>` |
| 回归 | 根级最终 Gate | architecture、lint、typecheck、test、build:dev/prod、`pnpm test:e2e` | 全部退出 0，测试数与未运行项完整记录 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-08.md</Path>` |

- **Workspace checks：** source worktree 仅跑 E2E 文件静态/lint 检查；所有 required E2E 和完整 Gate 由 Lead 在 parent-candidate 执行。
- **E2E disposition：** required：这是认证、权限、动态菜单和并发 locator 的最终浏览器 Gate。
- **E2E owner/environment：** Lead / parent-candidate；5 workers、0 retries，完整 suite 连续至少 3 次并最终运行标准 `pnpm test:e2e`。
- **Integration evidence：** 记录 source/candidate/result SHA、每次完整 suite 的退出码/测试数和前端 main 包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-06/T-07 均集成后创建 T-08 source；T-08 candidate 通过后才能创建 CR-002。
- **兼容窗口：** 不适用：测试稳定性修复不改变生产接口。
- **监控信号：** strict locator 数量、每次 Playwright 退出码/测试数、根门禁与 CR-002 verdict。
- **回滚或前向恢复：** 失败时保留 source worktree与结果，修复 locator；不得提高 retry、降低 workers 或删除场景。
- **不可逆操作与批准点：** implementation commit、candidate integration 和 cleanup 均需新的用户授权；不推送或发布。
- **收缩条件：** 两个全局歧义 locator 已消除，连续并发 suite 和最终标准 suite 有 Evidence。

## 10. 验收标准

- [x] 两条 locator 作用域绑定具体业务 owner，无全局 `.first()`/`.last()` 掩盖歧义。
- [x] 完整 Playwright 在 5 workers、0 retries 下连续至少 3 次通过，随后标准 `pnpm test:e2e` 通过。
- [x] 根级 architecture、lint、typecheck、test、build:dev 和 build:prod 全部通过。
- [x] T-06/T-07/T-08 Evidence、source/candidate/result 完整，可作为 CR-002 固定输入。
- [x] 实际修改未超出 `writable_paths`，未通过 retries、串行化或放宽断言制造绿色。
