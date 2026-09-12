---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-01
title: 固化迁移前行为与质量基线
status: done
planning_depth: standard
planning_depth_reason: 涉及认证、请求、动态路由和构建四个高回归面，但只增加特征测试与基线文档，不改变生产行为
ready: true
risk: high
blocked_by: []
contract_ids: [AC-021]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/e2e/multi-app-baseline.spec.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/permission.test.ts</Path>", "<Path>plus-ui-namewta/src/utils/request.test.ts</Path>", "<Path>plus-ui-namewta/docs/architecture-baseline.md</Path>"]
writable_paths: ["<Path>plus-ui-namewta/e2e/multi-app-baseline.spec.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/permission.test.ts</Path>", "<Path>plus-ui-namewta/src/utils/request.test.ts</Path>", "<Path>plus-ui-namewta/docs/architecture-baseline.md</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/src/permission.ts</Path>", "<Path>plus-ui-namewta/src/utils/request.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/user.ts</Path>", "<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-01: 固化迁移前行为与质量基线

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/01-baseline-behavior.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-01.md</Path>`

## 1. 战略与来源

- **目标：** 在移动任何生产代码前，把登录 Client、401、动态路由恢复和管理端构建的当前语义变成可重复门禁。
- **可观察产出：** 维护者能运行一组基线命令并得到真实通过/失败结果和已知存量偏差报告。
- **来源：** `US-002`、`US-007`、`US-011`、`AC-021`、`ADR-006`、`CODE`。
- **当前事实：** `<Path>plus-ui-namewta/src/permission.ts</Path>` 直接 glob views 并 `addRoute`；Client 认证已有局部 E2E，但 permission/request 缺少完整迁移特征测试。
- **Planning Depth 原因：** 测试跨认证、路由、request 与构建边界，错误基线会让后续重构产生假阳性。

## 2. 决策状态

### 已锁定决策

- 本 Ticket 不重构生产代码；发现真实缺陷只记录，不顺手改变行为。
- 基线覆盖正常登录、Client 上下文失败、401 回跳、动态菜单恢复顺序和生产构建。

### 已采用的低影响假设

- 新测试沿用现有 Vitest/Playwright 配置与 mock 接缝。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 特征测试、E2E 基线、命令和偏差文档 | 现有 auth E2E、测试配置、生产实现 | 修复存量行为、创建 workspace 包、目录迁移 |

## 4. 要构建什么

维护者从干净依赖状态运行基线：正常认证和动态路由可达；错误 Client 在请求前失败；401 只出现一次重新登录流程并保留 redirect；管理端生产构建成功。任何失败都以测试断言或偏差条目显式出现，而不是调整预期掩盖。

## 5. 实现契约

- **入口或接缝：** permission store/router、request 401 handler、现有 Playwright login fixtures、根 scripts。
- **输入与输出：** mock Client/menu/401 响应输入；测试结果和基线报告输出。
- **公共接口变化：** 无。
- **不变量：** 不改变运行时代码、后端合同和权限判断。
- **状态或数据流：** Token -> getInfo -> getRouters -> addRoute -> replace；401 -> 单例提示 -> logout -> redirect。
- **错误与失败行为：** 环境依赖导致的不可执行项必须记录命令、原因和替代证据，不能标为通过。
- **兼容要求：** 当前 admin 根应用和脚本保持原样。
- **安全与隐私要求：** 测试日志不得包含真实 token、密码或 Client secret。

## 6. 执行路线

1. 运行并记录现有 lint、typecheck、test、E2E 和 build 基线。
2. 为 permission 恢复顺序和 request 401 语义补充最小特征测试。
3. 扩展浏览器场景证明登录、错误 Client 和动态路由关键路径。
4. 写入基线命令、已知偏差和后续 Gate 使用方式。
5. 重跑定向测试和全量适用门禁，形成不含生产改动的 commit。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅 frontmatter 列出的三个测试文件与基线文档。
- **只读上下文：** permission、request、user store、既有 E2E 和 package scripts。
- **共享路径：** 无。
- **保留或不动：** 所有生产代码和依赖清单。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Vitest + Playwright | `pnpm test`；Lead 运行 `pnpm test:e2e` | 恢复顺序、登录和菜单通过 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-01.md</Path>` |
| 失败路径 | request/auth mock | 定向运行新增 request 与 Client 场景 | 错误 Client fail-close；401 单例处理 | 同上 |
| 回归 | lint/type/build | `pnpm lint && pnpm typecheck && pnpm build:prod` | 真实结果被记录且生产行为未改 | 同上 |

- **Workspace checks：** current-workspace 运行 Vitest、lint、typecheck、build；Goal Plan 若选择 required 模式则改为 source-worktree 非 E2E。
- **E2E disposition：** required：认证、浏览器路由与 redirect 是跨边界用户行为。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；覆盖正常登录、错误 Client、401 和动态路由。
- **Integration evidence：** 记录 implementation/source commit、parent before、适用 candidate/result SHA 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序/兼容窗口/收缩条件：** 不适用：本 Ticket 不迁移生产实现。
- **监控信号：** Gate 命令退出码、Playwright report、baseline 文档偏差数。
- **回滚或前向恢复：** 回退新增测试和文档 commit 即可，不触及生产状态。
- **不可逆操作与批准点：** 无。

## 10. 验收标准

- [x] `AC-021`：后续每波可复用的 lint、typecheck、unit、E2E、build 基线均有真实结果和已知偏差。
- [x] 生产代码和依赖未改变，敏感信息未进入 Evidence。
- [x] 验证矩阵全部执行并记录到 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-01.md</Path>`。
- [x] 修改未超出 `writable_paths`，形成非空 implementation/source commit 并记录父分支 result。
