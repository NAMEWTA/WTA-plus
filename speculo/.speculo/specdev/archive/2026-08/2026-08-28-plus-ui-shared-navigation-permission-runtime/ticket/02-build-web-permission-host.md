---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-02
title: 建立 Vue Web 权限宿主合同
status: done
planning_depth: deep
planning_depth_reason: 新增由多个 Web Domain 消费的认证权限公共入口，并必须保持跨终端依赖纯度和失败关闭语义。
ready: true
risk: high
blocked_by: [T-01]
contract_ids: [AC-001, AC-002, AC-003, AC-004, AC-012, AC-014]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/packages/web-kit/permission/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/web-kit/permission/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/apps/admin-web/src/directive/permission/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/access.ts</Path>", "<Path>plus-ui-namewta/packages/platform/permission/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/apps/admin-web/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/apps/admin-web/package.json</Path> => T-02", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-02"]
---

# Ticket T-02: 建立 Vue Web 权限宿主合同

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/02-build-web-permission-host.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 建立可由任意 Vue Web App 安装、但不读取 Admin Store 的权限指令宿主。
- **可观察产出：** 一个最小测试宿主仅注入 `AccessEvaluator` provider，即可获得行为与当前 `v-hasPermi`、`v-hasRoles` 一致的全局指令。
- **来源：** `US-001`、`US-002`、`US-003`、`US-005`、`AC-001` 至 `AC-004`、`AC-012`、`ADR-001`。
- **当前事实：** 指令位于 `<Path>plus-ui-namewta/apps/admin-web/src/directive/permission/</Path>` 并直接导入 Admin 权限装配，而多个 `<Path>plus-ui-namewta/packages/web-domains/</Path>` 页面依赖同名全局指令。
- **Planning Depth 原因：** 该 Ticket 新增权限公共接口并处理非法输入、超级管理员和通配符等安全语义。

## 2. 决策状态

### 已锁定决策

- 新包目录为 `<Path>plus-ui-namewta/packages/web-kit/permission/</Path>`，包名使用 `@namewta/web-kit-permission`。
- 根公开入口提供显式安装函数；输入是每次调用可取得当前 `AccessEvaluator` 的 provider，避免捕获过期身份快照。
- 安装函数只注册 `hasPermi` 与 `hasRoles`，不迁移 copyText 或其他 Admin directive。
- 指令仅调用 Platform Permission 的 `hasAnyPermission` / `hasAnyRole`，不得复制角色或通配权限算法。

### 已采用的低影响假设

- 指令错误文案保持当前可识别意图，但具体英文标点可按测试冻结；验证非法空数组和非数组均显式失败。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 新 Web Kit 包、公开 exports、README、指令测试、依赖与锁文件登记 | `@namewta/platform-permission` 的 AccessEvaluator 及现有 Vue 指令语义 | 不迁移 Admin 消费者，不创建 Store/Router，不发布外部 npm |

## 4. 要构建什么

Vue Web App 调用公开安装入口并提供一个动态 evaluator provider。页面挂载带 `v-hasPermi` 或 `v-hasRoles` 的元素时，宿主读取最新 evaluator：任一条件匹配则保留，否则移除；非法绑定显式抛错。该包可被 Web Domain 宿主复用，但 Platform Permission 和非 Web 终端不反向依赖它。

## 5. 实现契约

- **入口或接缝：** `@namewta/web-kit-permission` 根 exports 的 Vue 安装入口。
- **输入与输出：** 输入为 Vue `App` 与 `() => AccessEvaluator`；输出为注册的全局 `hasPermi`、`hasRoles` 指令，无持久状态。
- **公共接口变化：** 新增私有 workspace 包和根公开入口；Admin 依赖提前登记，由 T-04 正式消费。
- **不变量：** provider 在指令执行时取值；指令名不变；Platform Permission 不依赖 Web Kit。
- **状态或数据流：** App session -> evaluator provider -> directive mounted -> hasAny 检查 -> 保留或移除 DOM。
- **错误与失败行为：** 非数组、空数组、空白项或 evaluator 缺失不得默认放行；不匹配直接移除元素。
- **兼容要求：** 无兼容门面；旧 App 私有实现由 T-04 迁移后删除。
- **安全与隐私要求：** 指令只控制呈现；不得缓存、打印或持久化角色、权限、Token。

## 6. 执行路线

1. 先以当前指令行为建立包级 Vue/Vitest 失败测试，覆盖允许、拒绝、超管、通配符、角色和非法绑定。
2. 创建最小 Web Kit 包、tsconfig、package manifest、README 和根 exports，实现 evaluator provider 安装合同。
3. 登记 Admin 对新包的 workspace 依赖，并由本 Ticket 唯一更新锁文件，暂不迁移生产调用点。
4. 运行包级 lint、typecheck、test、build 与架构检查，确认 Web Kit 依赖方向和 Platform 纯度。

## 7. 路径访问契约

- **预计修改点：** 新权限包、Admin package manifest、锁文件。
- **可写范围：** frontmatter 所列三项；不得修改 Admin `src`。
- **只读上下文：** 当前 Admin 指令、权限装配、Platform Permission 与 Web Domain 用法。
- **共享路径：** Admin package manifest 与锁文件；唯一 owner 为 T-02。
- **保留或不动：** Platform Permission 实现、Web Domain 模板和 App Store。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Web Kit Vue 单元 | 包级 test | 权限/角色匹配、超管与通配符保留元素 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-02.md</Path>` |
| 失败路径 | Web Kit Vue 单元 | 包级 test | 不匹配、缺少身份和非法绑定失败关闭 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-02.md</Path>` |
| 回归 | 包与架构门禁 | 包级 lint/typecheck/build、`pnpm architecture:check`、`pnpm architecture:test` | 无 App/Pinia/Router 深依赖，Platform 纯度不变 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-02.md</Path>` |

- **Workspace checks：** current 策略在 current-workspace；required 策略在 source-worktree 运行全部非 E2E 包级和架构检查。
- **E2E disposition：** not-required：本 Ticket 尚不迁移任何 App 生产消费者，公共合同由 Vue 单元和架构接缝完整验证。
- **E2E owner/environment：** Lead / direct-parent 或 parent-candidate 复核 not-required 理由；source-worktree 不运行 E2E。
- **Integration evidence：** 记录 source/implementation commit、parent before、适用 candidate/result SHA、锁文件唯一 owner 与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-01 绿色后创建共享能力；T-04 才迁移 Admin 并删除旧实现。
- **兼容窗口：** 无运行时兼容层；短暂未消费的新包只存在于阶段 checkpoint。
- **监控信号：** 包级测试、架构违规数、类型诊断和依赖图。
- **回滚或前向恢复：** 未通过时回退新包、manifest 和锁文件的同一 checkpoint，不触碰 Admin 源码。
- **不可逆操作与批准点：** 无外部发布；commit/集成需显式授权。
- **收缩条件：** T-04 证明 Admin 私有权限指令调用点为零后删除旧实现。

## 10. 验收标准

- [x] `AC-001` 至 `AC-004` 和 `AC-012` 的包级合同通过。
- [x] 新包不依赖 apps、Pinia、Router 或 UI 消息单例。
- [x] Admin 依赖和锁文件由 T-02 一次性登记，其他 Ticket 不修改这些 shared paths。
- [x] 验证与 commit/integration 证据写入 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-02.md</Path>`。
- [x] 未发生未批准偏差，Ticket、Map 与 Evidence 状态一致。
