---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-06
title: 激活 client-web 第二 App 组合证明
status: done
planning_depth: deep
planning_depth_reason: 新增独立可构建 App、ClientContext、登录纵切和不同视觉壳层，并验证跨 App 会话隔离
ready: true
risk: high
blocked_by: [T-05]
contract_ids: [AC-003, AC-018, AC-019, AC-023]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/apps/client-web/**</Path>", "<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/web-kit/**</Path>", "<Path>plus-ui-namewta/e2e/client-web-proof.spec.ts</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/apps/client-web/**</Path>", "<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/web-kit/**</Path>", "<Path>plus-ui-namewta/e2e/client-web-proof.spec.ts</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/domains/demo/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/demo/**</Path>", "<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/src/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: ["<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03 policy owner; T-06 scoped writer for importers mechanically generated from activated T-06 package manifests only (DEV-T06-001)"]
---

# Ticket T-06: 激活 client-web 第二 App 组合证明

- **Ticket/Map/Spec/Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/06-client-web-proof.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-06.md</Path>`。

## 1. 战略与来源

- **目标/产出：** client-web 以独立 ClientContext、composition manifest 和不同壳层复用 identity 登录纵切及 demo，不复制 domain。
- **来源：** `US-003`、`US-005`、`US-007`、`AC-003`、`AC-018`、`AC-019`、`AC-023`、`ADR-001`、`ADR-005`。
- **当前事实：** 当前只有根 admin App；`VITE_APP_CLIENT_ID` 分散在请求、登录、push/workflow 等路径。
- **Planning Depth 原因：** 第二 App 跨构建、Client 安全、认证网络、manifest 和 UI 壳层，是根入口迁移的强 Gate。

## 2. 决策状态

### 已锁定决策

- client-web 只选择 identity-access 登录纵切和 demo；未选 admin/workflow/AI/operations 不注册。
- ClientContext 由 App 注入且不共享错误 session namespace；布局/主题在 App/web-kit 层定制。
- 本 Ticket 只做最小登录纵切，完整 auth/permission 迁移留给 T-07。
- `DEV-T06-001`：激活 client-web、identity 与实际需要的 web-kit package 会机械新增 lockfile importers。T-06 只可写由其目录内 package manifests 生成的新增 importer/specifier；不得修改 root package、scripts、catalog、workspace 配置、外部版本、既有 importer/resolution 或无关 lock 节点。

### 已采用的低影响假设

- client-web 使用现有 Vue 3/Vite/Pinia/Router 技术栈和 Element Plus web-kit，不引入新 UI 框架。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| client App、最小 identity 登录、demo 组合、独立 shell/theme、E2E | platform/adapters、demo manifests、后端 auth APIs | 完整权限/注册/OAuth、admin 迁移、移动端 |

## 4. 要构建什么

用户打开 client-web，看到与 admin 不同的壳层，使用该 App 的 Client 完成登录并访问 demo；runtime 不含未选择领域。错误或缺失 Client 在认证请求前阻断；admin 与 client 并行会话不串用上下文。

## 5. 实现契约

- **入口/输入输出：** client main、AppCompositionManifest、ClientContext provider、identity login use case；配置/凭据输入，session 和选择性 routes 输出。
- **公共接口变化：** 激活 client-web 和 identity 登录 public export；不改变后端 API。
- **不变量/数据流：** App config -> validated ClientContext -> login use case -> adapter -> isolated session -> selected manifests。
- **失败行为：** Client 缺失/类型错误 fail-close；未选 component key 使用 T-05 稳定诊断。
- **兼容/安全：** admin 保持运行；Client ID 不是授权替代品；token 隔离且不记录。

## 6. 执行路线

1. 建立 client-web build/main、独立 shell 与显式 composition manifest。
2. 提取最小 identity 登录 use case/web view，复用 platform/adapters。
3. 注入并验证 ClientContext、storage namespace 和 demo manifest。
4. 增加双 App Client/header/session、未选领域和主题差异测试。
5. 运行双 App build、architecture/type/unit；Lead 完成 Gate F E2E。

## 7. 路径访问契约

- **可写：** client App、identity 最小切片、web-kit 和专用 E2E；**只读：** demo/platform、根 src 和 T-03 根配置。
- **共享路径：** `DEV-T06-001` 仅委托 `<Path>pnpm-lock.yaml</Path>` 中由 T-06 新激活 manifests 机械生成的 importer/specifier；T-03 保留 lock/workspace/catalog 策略 owner。需要构建时使用 package 自有 scripts 与既有 pnpm filter，不改根 manifest/scripts。
- **保留或不动：** admin auth/router、其他领域、后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | client build + E2E | filter build；Lead 登录/demo | 独立 Client、选择领域和壳层生效 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-06.md</Path>` |
| 失败路径 | Client/registry tests | 缺失/非法 Client、未选 key | 请求前 fail-close；稳定诊断 | 同上 |
| 回归 | graph + dual build/E2E | architecture/type；admin+client | domain 未复制，双 App 绿色且会话隔离 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 architecture、unit、typecheck、双 App build。
- **E2E disposition：** required：第二 App 登录、网络 Client header、会话和路由跨多个运行边界。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；执行双 Client 矩阵和未选领域场景。
- **Integration evidence：** source commit、parent before、candidate/result SHA 和 Gate F 结论。

## 9. 发布、迁移与恢复

- **迁移/兼容：** client 独立激活，不替换 admin；identity 完整迁移由 T-07 扩展。
- **监控/回滚：** 双 App build、Client mismatch、session namespace；失败禁用 client entry，不改变 admin。
- **批准点：** Gate F 通过后才允许 T-07 和最终根入口迁移；**收缩条件：** 不适用，本 Ticket 不删旧入口。

## 10. 验收标准

- [x] `AC-003`：client-web 独立构建、登录，只注册 identity/demo 且壳层不同。
- [x] `AC-018/AC-019`：不同 Client 会话隔离，表现层定制不分叉 domain。
- [x] `AC-023`：Gate F 失败阻止 T-07/T-15 并保持 admin。
- [x] 验证、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-06.md</Path>`。
