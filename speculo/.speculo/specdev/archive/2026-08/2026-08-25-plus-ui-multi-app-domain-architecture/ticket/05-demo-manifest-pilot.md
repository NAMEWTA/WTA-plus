---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-05
title: 以 demo 建立 Domain 与 WebDomainManifest 曳光弹
status: done
planning_depth: deep
planning_depth_reason: 首次定义领域公开入口、App 组合 manifest、component key registry、兼容入口和失败诊断合同
ready: true
risk: high
blocked_by: [T-04]
contract_ids: [AC-010, AC-011, AC-012, AC-022, AC-027]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>", "<Path>plus-ui-namewta/packages/domains/demo/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/demo/**</Path>", "<Path>plus-ui-namewta/src/api/demo/**</Path>", "<Path>plus-ui-namewta/src/views/demo/**</Path>", "<Path>plus-ui-namewta/e2e/demo-manifest.spec.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>", "<Path>plus-ui-namewta/packages/domains/demo/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/demo/**</Path>", "<Path>plus-ui-namewta/src/api/demo/**</Path>", "<Path>plus-ui-namewta/src/views/demo/**</Path>", "<Path>plus-ui-namewta/e2e/demo-manifest.spec.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/platform/contracts/**</Path>", "<Path>plus-ui-namewta/packages/platform/http/**</Path>", "<Path>plus-ui-namewta/packages/platform/auth/**</Path>", "<Path>plus-ui-namewta/packages/platform/permission/**</Path>", "<Path>plus-ui-namewta/src/permission.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/src/router/**</Path>", "<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>"]
shared_paths: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/package.json</Path> => T-03 policy owner; T-05 scoped writer for workspace:* declarations actually consumed by root compatibility facades only (DEV-T05-001)", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03 policy owner; T-05 scoped writer for matching root specifiers and importers generated from T-05 manifests only (DEV-T05-001)"]
---

# Ticket T-05: 以 demo 建立 Domain 与 WebDomainManifest 曳光弹

- **Ticket/Map/Spec/Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/05-demo-manifest-pilot.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-05.md</Path>`。

## 1. 战略与来源

- **目标/产出：** 迁移低风险 demo 的 API、模型、应用服务和 Web 页面，形成首个可被 App manifest 选择、可构建、可路由的完整领域。
- **来源：** `US-003`、`US-004`、`US-005`、`US-008`、`AC-010` 至 `AC-012`、`AC-022`、`AC-027`、`ADR-003`、`ADR-004`、`ADR-006`。
- **当前事实：** demo API/views 位于根 src，动态路由以全局 views glob 解析，尚无显式 registry。
- **Planning Depth 原因：** 这是所有后续 web-domain 复用的公共 manifest 和失败语义原型。

## 2. 决策状态

### 已锁定决策

- Domain 含 transport 调用、类型和无头逻辑；web-domain 含 Vue/Element 页面、hooks、lang 和显式 `WebDomainManifest`。
- 后端 component key 保持稳定；冲突 fail-fast，缺失 key 返回含 App/domain/key 的稳定诊断，不静默空白。
- OpenAPI 未开始，手工 transport types 继续工作。
- `DEV-T05-002`：本架构迁移原样保持现有 demo HTTP transport；当前后端更新/删除端点仍使用 `PUT/DELETE`，不得在不修改后端的情况下单边改为 `POST`。该兼容例外只覆盖已存在的 demo 路径/方法，不授权新建非 POST 变更端点，并在后端完成协调迁移时到期。
- `DEV-T05-003`：T-05 只验证 registry 自身的 missing-key 结构化错误、有效 demo facade 的 `compose -> resolve -> load` 链路，以及显式诊断 harness 的可见反馈。任意后端未知 key 在旧全局 views glob 中被提前返回 `undefined`，其通用 fail-visible 集成必须由拥有 `<Path>src/store/modules/permission.ts</Path>` 与 router shared paths 的 T-07 完成并以真实 typo key E2E 验收；不得把预置物理 facade 伪装成通用未知 key 证明。AC-012 最终行为不变，改为 T-05/T-07 分段关闭。

### 已采用的低影响假设

- 首期 registry API 以纯 TypeScript 实现并由 App runtime 调用。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| demo domain/web-domain、manifest registry 合同、兼容 re-export、测试 | platform ports、现有 demo 行为和后端 keys | 认证迁移、第二 App、其他领域、OpenAPI 生成 |

## 4. 要构建什么

App 选择 demo manifest 后，只注册其页面和贡献；后端返回合法 key 时页面经 registry 可达。重复 key 在组合时报告双方 manifest；registry missing key 输出稳定诊断，显式诊断 harness 将其呈现为可见反馈。任意未知后端 key 穿过全局动态路由的能力按 `DEV-T05-003` 留给 T-07。旧 demo 导入在兼容期仍工作。

## 5. 实现契约

- **入口/输入输出：** demo public exports、`WebDomainManifest`、registry compose/resolve；manifest 与 component key 输入，唯一组件或结构化错误输出。
- **公共接口变化：** 新增 demo/domain、demo/web-domain exports 和通用 manifest 类型；旧 API/view 路径保留 facade。
- **不变量/数据流：** App 选择 -> manifests compose -> 后端菜单 key -> registry resolve -> route component；未选择领域不进入 runtime。
- **失败行为：** duplicate/missing key 在 registry/adapter 接缝稳定失败，不后者覆盖前者；任意后端未知 key 的全局路由反馈由 T-07 完成。
- **兼容/安全：** admin demo 行为不变；服务端权限仍是权威，registry 不扩大菜单权限；按 `DEV-T05-002` 保持既有 demo `PUT/DELETE` transport，避免前端单边破坏后端合同。

## 6. 执行路线

1. 为 demo API/page 和当前 component keys 建立特征测试。
2. 提取 demo domain public exports，确保无 Web 依赖。
3. 建立 web-domain manifest 与纯 registry 的成功、冲突、缺失测试。
4. 让现有 admin 通过兼容入口消费新包并保持页面可达。
5. 运行 architecture/type/unit/build 与 Lead E2E；Gate E 失败则停止 T-06/T-07。

## 7. 路径访问契约

- **可写：** demo 新包、旧 demo 兼容路径和专用 E2E；**只读：** platform、全局 permission、根配置。
- **共享路径：** `DEV-T05-001` 将通用 registry 的 `<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>` 激活权转交 T-05，并允许只写 root compatibility facade 实际消费的新包 `workspace:*` 声明、匹配 root lock specifier 与本 Ticket manifests 机械生成的 importer。T-03 继续拥有 workspace/catalog/root dependency/lock 策略；禁止修改根 scripts、catalog、外部版本、无关依赖、workspace 配置或既有 resolution。
- **保留或不动：** 全局认证/路由语义、其他业务域、后端 component key。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | domain/manifest unit + page E2E | test；Lead demo 导航 | 仅选择的 demo 注册且可达 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-05.md</Path>` |
| 失败路径 | registry unit + 显式诊断 harness E2E | 重复 key、adapter missing key | fail-fast；显式诊断含 App/domain/key；不声称覆盖任意未知后端 key | 同上 |
| 回归 | graph/type/build | architecture、typecheck、admin build | 无头边界合法，旧 demo 可用 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、build。
- **E2E disposition：** required：动态菜单到 Vue 页面解析是跨路由/UI 边界行为。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；验证合法 registry 页面、keep-alive 与显式诊断 harness。重复冲突由 registry unit 验证；任意未知后端 key 由 T-07 E2E 验证。
- **Integration evidence：** source commit、parent before、candidate/result SHA 和 Gate E 结论。

## 9. 发布、迁移与恢复

- **迁移/兼容：** domain expand -> web manifest -> admin migrate；旧 demo facade 保留到 T-15。
- **监控/回滚：** registry 错误数、demo E2E/build；失败回切旧 resolver/facade。
- **批准点：** Gate E 通过后才可开始 T-06；**收缩条件：** T-15 证明旧 demo imports 为零。

## 10. 验收标准

- [x] `AC-010/AC-011` 与 `AC-012` 前半段：选择性注册、冲突、registry missing 诊断及显式可见 adapter 均可判定；AC-012 任意未知后端 key 的全局 fail-visible 证据由 T-07 补齐。
- [x] `AC-022`：Gate E 失败会阻断后续高风险迁移且保留兼容入口。
- [x] `AC-027`：手工 transport types 在无生成器时通过 type/API tests。
- [x] 验证、路径、commit/candidate/result SHA 记录到 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-05.md</Path>`。
