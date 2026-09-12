---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-03
title: 提取服务端菜单纯运行时
status: done
planning_depth: deep
planning_depth_reason: 扩展共享 App Runtime 公共接口并承载动态路由失败诊断，属于共享核心与认证后导航路径。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-006, AC-007, AC-010, AC-012, AC-014]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/manifestDiagnostic.ts</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-03: 提取服务端菜单纯运行时

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/03-extract-server-menu-runtime.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-03.md</Path>`

## 1. 战略与来源

- **目标：** 将 ParentView 展平、空 children 清理和重复 route name 识别收口为无 App 单例的确定性公共能力。
- **可观察产出：** 任意 App 可输入服务端菜单、特殊组件、manifest resolver 与诊断 factory，得到确定路由投影及结构化重复名称诊断，无需复制 Admin Store 算法。
- **来源：** `US-002`、`US-005`、`US-006`、`AC-006`、`AC-007`、`AC-010`、`AC-012`、`ADR-002`。
- **当前事实：** `<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path>` 已负责组件装配和缺失键诊断，ParentView 展平、空 children 清理与重复名称通知仍混在 Admin permission Store。
- **Planning Depth 原因：** 公共类型和菜单状态转换被未来 App 复用，错误会影响认证后全部动态导航。

## 2. 决策状态

### 已锁定决策

- 继续扩展现有 `@namewta/platform-app-runtime` 根公开入口，不创建新的 common/menu 包。
- 公共能力处理不可变输入并返回新树；不得原地污染后端响应或读取 Pinia、Router、Admin Layout、Element Plus。
- 投影选项必须能表达“是否展平 ParentView”差异，以分别支持 rewrite、sidebar 和 default 投影。
- 重复名称能力返回稳定、去重的诊断数据，由 App 决定如何呈现。

### 已采用的低影响假设

- 局部函数名沿用现有 routeAssembler 词汇；公开类型以结构能力命名，不承诺 Vue Router 专属类型。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 服务端菜单投影、ParentView 展平、空 children 清理、重复名称诊断、README 与表驱动测试 | 现有 `assembleServerRoutes`、manifest resolver、特殊组件与缺失组件诊断 | 不创建 Store/Router，不导入 Vue/DOM/Admin，不迁移消费者 |

## 4. 要构建什么

调用者为同一后端菜单创建 sidebar、rewrite 和 default 投影时，公共运行时根据显式选项处理 ParentView 路径、组件解析和空 children，并保留服务端权限元数据。调用者还可以组合一组路由投影，获得重复非空 route name 的稳定诊断；共享层不显示通知，也不本地过滤服务端菜单。

## 5. 实现契约

- **入口或接缝：** `@namewta/platform-app-runtime` 根 exports。
- **输入与输出：** 输入为 readonly 服务端路由树、投影选项、特殊组件、registration resolver、missing-component factory；输出为新路由树和可选的重复名称诊断集合。
- **公共接口变化：** 扩展 App Runtime 根公开类型/函数；保留已有 `assembleServerRoutes` 与 `restoreProtectedNavigation` 正式入口。
- **不变量：** 不过滤 Client 已裁剪菜单；不改变权限 metadata；外链与特殊组件语义保持；无 UI 或 App 单例依赖。
- **状态或数据流：** RouterVo clone -> 可选 ParentView 展平 -> 组件装配 -> 空 children/redirect 规范化 -> 重复名称分析。
- **错误与失败行为：** 未知组件继续走注入诊断 factory；畸形/空 children 不生成隐式可访问页面；重复名称只返回诊断，不吞掉路由。
- **兼容要求：** 对现有公开调用保持源兼容，新增能力由 T-04 采用；最终不保留 Admin 私有算法副本。
- **安全与隐私要求：** 不接触 Token 或用户对象；任何缺失组件路径失败关闭。

## 6. 执行路线

1. 将 Admin 当前 ParentView、空 children 和重复名称行为转成 App Runtime 表驱动失败测试。
2. 设计最小 readonly 输入/新输出合同，并扩展现有 route assembler 公开入口。
3. 实现可选投影与结构化诊断，保持现有组件装配测试绿色。
4. 更新包 README，运行包级 lint/typecheck/test/build 和架构边界测试。

## 7. 路径访问契约

- **预计修改点：** `<Path>plus-ui-namewta/packages/platform/app-runtime/</Path>` 内源码、测试、exports 和 README。
- **可写范围：** 仅该包。
- **只读上下文：** Admin 当前 Store、manifest registry 和诊断组件。
- **共享路径：** 无；T-03 是 App Runtime 唯一 writer。
- **保留或不动：** Admin 源码、Platform Permission、package lock 与后端合同。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | App Runtime 单元 | 包级 test，覆盖三类投影、特殊组件、manifest 与多级 ParentView | 输出确定且与冻结行为一致 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-03.md</Path>` |
| 失败路径 | App Runtime 单元 | 畸形 children、未知键、重复名称测试 | 失败关闭并返回稳定诊断，无 UI 副作用 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-03.md</Path>` |
| 回归 | 包与架构门禁 | 包级 lint/typecheck/test/build、`pnpm architecture:check` | 原公开入口继续工作，无 App/Vue/Pinia/Router 依赖 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-03.md</Path>` |

- **Workspace checks：** current 策略在 current-workspace；required 策略在 source-worktree 运行全部非 E2E 检查。
- **E2E disposition：** not-required：本 Ticket 只形成无生产消费者变化的纯公共能力，表驱动单元与架构测试是最近稳定接缝。
- **E2E owner/environment：** Lead / direct-parent 或 parent-candidate 复核 not-required；source-worktree 不运行 E2E。
- **Integration evidence：** 记录 implementation/source commit、parent before、适用 candidate/result SHA、公共 exports diff 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 后可与 T-02 并行；必须先于 T-04 集成。
- **兼容窗口：** 现有正式函数保持可用，不新增旧 Admin 算法兼容门面。
- **监控信号：** 包级测试、重复诊断快照、架构违规和类型错误。
- **回滚或前向恢复：** 未通过时回退本包 checkpoint；Admin 尚未消费，不产生半迁移状态。
- **不可逆操作与批准点：** 无外部发布；commit/集成需授权。
- **收缩条件：** T-04 迁移后扫描确认 Admin 内 ParentView/children/duplicate 算法副本为零。

## 10. 验收标准

- [x] `AC-006`、`AC-007`、`AC-010`、`AC-012` 的纯运行时合同有正常与失败测试。
- [x] 包不导入 apps、Vue、DOM、Pinia、Vue Router 或 UI 消息实现。
- [x] 现有 App Runtime 测试与根公开入口保持绿色。
- [x] Evidence、commit、integration 和父分支结果写入 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-03.md</Path>`。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 状态一致。
