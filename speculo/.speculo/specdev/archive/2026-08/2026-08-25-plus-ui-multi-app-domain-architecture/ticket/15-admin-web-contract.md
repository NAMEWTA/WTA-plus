---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-15
title: 激活 admin-web 并收缩根应用兼容入口
status: done
planning_depth: deep
planning_depth_reason: 最终汇合全部领域并移动生产管理端入口、删除旧 src 兼容层，属于高事故半径的 expand-contract 收缩
ready: true
risk: critical
blocked_by: [T-11, T-12, T-13, T-14]
contract_ids: [AC-002, AC-021, AC-024]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/apps/admin-web/**</Path>", "<Path>plus-ui-namewta/src/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/playwright.config.ts</Path>", "<Path>plus-ui-namewta/vitest.config.ts</Path>", "<Path>plus-ui-namewta/tsconfig.json</Path>", "<Path>plus-ui-namewta/index.html</Path>", "<Path>plus-ui-namewta/public/**</Path>", "<Path>plus-ui-namewta/vite.config.ts</Path>", "<Path>plus-ui-namewta/vite/**</Path>", "<Path>plus-ui-namewta/uno.config.ts</Path>", "<Path>plus-ui-namewta/.env.development</Path>", "<Path>plus-ui-namewta/.env.production</Path>"]
writable_paths: ["<Path>plus-ui-namewta/apps/admin-web/**</Path>", "<Path>plus-ui-namewta/src/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/playwright.config.ts</Path>", "<Path>plus-ui-namewta/vitest.config.ts</Path>", "<Path>plus-ui-namewta/tsconfig.json</Path>", "<Path>plus-ui-namewta/index.html</Path>", "<Path>plus-ui-namewta/public/**</Path>", "<Path>plus-ui-namewta/vite.config.ts</Path>", "<Path>plus-ui-namewta/vite/**</Path>", "<Path>plus-ui-namewta/uno.config.ts</Path>", "<Path>plus-ui-namewta/.env.development</Path>", "<Path>plus-ui-namewta/.env.production</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/**</Path>", "<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>"]
shared_paths: ["<Path>plus-ui-namewta/src/**</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/src/**</Path> => T-15"]
---

# Ticket T-15: 激活 admin-web 并收缩根应用兼容入口

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/15-admin-web-contract.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-15.md</Path>`。

## 1. 战略与来源

- **目标/产出：** admin-web 以 composition manifest 组装七领域并独立构建运行；所有旧消费者清零后删除根 src 兼容入口。
- **来源：** `US-001` 至 `US-003`、`US-011`、`AC-002`、`AC-021`、`AC-024`、`ADR-001`、`ADR-004`、`ADR-006`。
- **当前事实：** 前序 Ticket 都保留旧 src facade；本 Ticket 是唯一 contract owner，必须汇合 system/AI/devtools/operations 后才开始。
- **Planning Depth 原因：** 删除生产入口难以局部回滚，必须用零调用点、双 App 构建和全 E2E 共同批准。

## 2. 决策状态

### 已锁定决策

- admin-web 显式选择 identity-access、system-admin、workflow、ai、demo、devtools、operations manifests。
- 先复制/组合并证明等价，再将旧 imports 降为零，最后删除兼容 src；任何 Gate 失败都不删除。
- 根配置仍归 T-03；若 admin 激活必须调整根 shared config，Goal Plan 先记录 ownership deviation 或由 T-03 owner 提供集成 commit。
- `DEV-T15-001`：T15 可把根 App 入口、Vite/Uno/env/public 资产复制到 admin-web，在 pre-contract 绿色后删除旧根副本；可激活 admin package、把根 package 收缩为 workspace 编排器、机械生成 admin lock importer、把 Playwright preview 指向 admin package，并仅移除根 tsconfig 的旧 `src` App include/alias。packages、workspace/catalog、外部版本、架构规则与 client-web 保持只读。
- `DEV-T15-002`：旧根 Vite 配置还被工作区 Vitest 隐式向上发现，删除它会令 web-domain 的 Vue SFC 测试失去编译插件；根开发依赖也承担现有 packages/E2E 的共享工具链。T15 可保留既有根 devDependencies、把 E2E 已直接导入的现有 catalog `crypto-js` 明确列为根 test-only devDependency、增加仅注册 Vue 插件的根 `vitest.config.ts`，并把 admin 内 `VxeTableInstance` 类型改从实际公开声明路径导入；不得恢复根 runtime dependencies 或 App 构建入口，不得修改 packages、外部版本和测试断言。
- `DEV-T15-003`：初轮 fixed-point review 判定 `dd14be9` 只是 expand checkpoint，`da8e21f` 同时迁移调用点与删除旧入口，不能充当 AC-024 删除批准点；同时发现 Playwright 未自主管理 client 4174、根 `build:dev` 静默走 production。T15 以 append-only 方式恢复 dormant 根副本，形成 root scripts/HTML/tsconfig/Playwright 均已切到 admin 且完整绿色的 `ad4e971`，随后由独立 commit 再删除旧副本；Playwright 必须等待双 App 服务，`build:dev` 必须显式构建 development admin 与其余 workspace。
- `DEV-T15-004`：第二轮标准轴发现 Oxfmt ignore 仍指向已删除的根生成声明，且 package 边界 README 仍把 root facade 描述为当前存在。T15 可仅把两个 ignore 路径迁到 `apps/admin-web/src/types/**`，并把直接受 contract 影响的 package README 更新为“T15 已收缩”的当前事实；不得修改 package 源码、manifest、测试或依赖。
- `DEV-T15-005`：第三轮标准轴证明首批 README 收口仍遗漏 devtools/app-runtime/ui-element/permission/architecture 等当前态，并指出 platform-auth 实际只由 admin-web 消费。T15 可仅修正这些剩余 README 与 admin/packages 索引的事实描述；不得声称 client-web 组合 platform-auth，不得修改运行代码、manifest、依赖或测试。

### 已采用的低影响假设

- 静态 public/assets 可按实际 Vite root 归入 admin-web，但 URL 与部署 base 行为保持。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| admin App/main/shell/composition、所有旧 consumer 迁移、src contract 删除、双 App E2E | 全部 domains/web-domains/platform/adapters、T-03 scripts | OpenAPI、上游同步、后端/部署改造、移动端 |

## 4. 要构建什么

管理端用户从正式 admin-web 完成登录、动态菜单和各领域关键路径；client-web 同时独立运行。扫描不存在旧 src imports/入口，删除后 lint、typecheck、unit、E2E、双生产构建全绿；否则保留上一可运行 commit。

## 5. 实现契约

- **入口/输入输出：** admin main、AppCompositionManifest、shell/router/store adapters；App config/manifests 输入，完整管理端 runtime 输出。
- **公共接口变化：** admin-web 成为正式入口；旧 root src exports 在批准点删除。
- **不变量/数据流：** ClientContext -> adapters -> identity restore -> selected manifests -> registry routes；未选贡献不注册。
- **失败行为：** key/config/Client 错误稳定失败；删除前任一 scan/build/E2E 失败立即停止 contract。
- **兼容/安全：** 登录、401、Client、权限、superadmin、页面 URL 与部署行为保持；服务端授权权威不变。

## 6. 执行路线

1. 在不删 src 下激活 admin-web composition、main、shell 和 static assets。
2. 运行所有领域 manifest、route key、auth 和关键页面集成测试。
3. 按调用点批量迁移剩余 root imports 到公开 exports，每批保持绿色。
4. 用 `rg`、architecture check 和 build 证明旧 consumers 为零。
5. 创建明确 pre-contract commit/回滚点，经 Lead 批准后删除 src facade。
6. 在 parent-candidate/current workspace 运行全门禁、双 App production builds 和完整 E2E，记录 Gate H result。

## 7. 路径访问契约

- **可写：** admin-web、整个旧 src 收缩面和 E2E；**只读：** packages 与 T-03 根配置。
- **共享路径：** `<Path>plus-ui-namewta/src/**</Path>` 唯一 contract owner `T-15`；前序 Ticket 合并后不得继续修改。
- **保留或不动：** package public contracts、根 lock/config、后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | dual build + full E2E | admin/client build；全用户路径 | 两 App 独立且 admin 七领域可达 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-15.md</Path>` |
| 失败路径 | config/key/auth E2E | Client/key/401/权限拒绝 | 稳定失败且不越权 | 同上 |
| 回归/收缩 | rg/graph/full gates | 零旧 import；lint/type/unit/build/E2E | src 删除后全绿且无 deep import | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 scan、architecture、lint、typecheck、unit、双 build。
- **E2E disposition：** required：正式入口、所有动态路由和跨 App 行为必须在集成父候选验证。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖 auth、菜单和每领域 smoke/关键失败路径。
- **Integration evidence：** source commit、pre-contract commit、parent before、candidate/result SHA、父分支包含关系和 Gate H 批准。

## 9. 发布、迁移与恢复

- **迁移顺序：** admin expand -> consumers migrate -> zero scan -> pre-contract checkpoint -> src contract -> full Gate H。
- **兼容窗口：** 从 T-03 延续至零调用点批准；删除后只保留 git 可恢复 checkpoint，不保留双入口。
- **监控信号：** old import count、missing keys、auth/route errors、双 build/E2E。
- **回滚或前向恢复：** Gate H 失败回退到 pre-contract commit；部署异常恢复上一 admin artifact。
- **不可逆操作与批准点：** 删除 src 前必须由 Lead 审查零调用点和所有前置 Evidence；不得先删再补证据。
- **收缩条件：** 所有现有消费者为零，七领域 Gate、双 App build、lint/type/unit/E2E 全部真实通过。

## 10. 验收标准

- [x] `AC-002`：admin-web 独立生产构建且兼容用户路径可访问。
- [x] `AC-024`：旧调用点为零后才删除 src，删除后双 App build/E2E 通过。
- [x] `AC-021`：Gate H 全量真实结果齐全，失败恢复 pre-contract。
- [x] shared owner、批准点、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-15.md</Path>`。
