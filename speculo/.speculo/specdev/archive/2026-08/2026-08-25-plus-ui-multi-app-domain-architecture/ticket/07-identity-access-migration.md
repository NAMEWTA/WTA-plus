---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-07
title: 迁移共享认证授权与动态路由核心
status: done
planning_depth: deep
planning_depth_reason: 迁移所有 App 共享的认证、会话、权限和动态路由安全核心，涉及 fail-close、401、超管兼容与全局路由共享路径
ready: true
risk: critical
blocked_by: [T-06]
contract_ids: [AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-021]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/platform/auth/**</Path>", "<Path>plus-ui-namewta/packages/platform/permission/**</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>", "<Path>plus-ui-namewta/src/permission.ts</Path>", "<Path>plus-ui-namewta/src/router/**</Path>", "<Path>plus-ui-namewta/src/store/modules/user.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/src/directive/permission/**</Path>", "<Path>plus-ui-namewta/src/api/login.ts</Path>", "<Path>plus-ui-namewta/src/api/menu.ts</Path>", "<Path>plus-ui-namewta/src/views/login.vue</Path>", "<Path>plus-ui-namewta/src/views/register.vue</Path>", "<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>", "<Path>plus-ui-namewta/src/utils/request.test.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/platform/auth/**</Path>", "<Path>plus-ui-namewta/packages/platform/permission/**</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>", "<Path>plus-ui-namewta/src/permission.ts</Path>", "<Path>plus-ui-namewta/src/router/**</Path>", "<Path>plus-ui-namewta/src/store/modules/user.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/src/directive/permission/**</Path>", "<Path>plus-ui-namewta/src/plugins/auth.ts</Path>", "<Path>plus-ui-namewta/src/utils/permission.ts</Path>", "<Path>plus-ui-namewta/src/api/login.ts</Path>", "<Path>plus-ui-namewta/src/api/menu.ts</Path>", "<Path>plus-ui-namewta/src/api/types.ts</Path>", "<Path>plus-ui-namewta/src/views/login.vue</Path>", "<Path>plus-ui-namewta/src/views/register.vue</Path>", "<Path>plus-ui-namewta/src/views/socialCallback.vue</Path>", "<Path>plus-ui-namewta/e2e/client-auth-context.spec.ts</Path>", "<Path>plus-ui-namewta/src/utils/request.test.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/adapters/**</Path>", "<Path>plus-ui-namewta/apps/client-web/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: ["<Path>plus-ui-namewta/src/permission.ts</Path>", "<Path>plus-ui-namewta/src/router/**</Path>", "<Path>plus-ui-namewta/src/store/modules/user.ts</Path>", "<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/src/permission.ts</Path> => T-07", "<Path>plus-ui-namewta/src/router/**</Path> => T-07", "<Path>plus-ui-namewta/src/store/modules/user.ts</Path> => T-07", "<Path>plus-ui-namewta/src/store/modules/permission.ts</Path> => T-07", "<Path>plus-ui-namewta/package.json</Path> => T-03 policy owner; T-07 scoped writer for root facades' actual internal workspace:* consumers only (DEV-T07-001)", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03 policy owner; T-07 scoped writer for matching root specifiers and T-07 manifest importers only (DEV-T07-001)"]
---

# Ticket T-07: 迁移共享认证授权与动态路由核心

- **Ticket/Map/Spec/Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/07-identity-access-migration.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-07.md</Path>`。

## 1. 战略与来源

- **目标/产出：** 双 App 共用无头 identity/access 核心和可注入 Navigation/Error UI，保持现有登录、注册、OAuth、会话、按钮权限与动态菜单行为。
- **来源：** `US-002`、`US-007`、`US-008`、`AC-013` 至 `AC-018`、`AC-021`、`ADR-004` 至 `ADR-006`。
- **当前事实：** auth 分散在 login/menu APIs、stores、guard、directives/plugins/utils；permission guard 直接 glob views 并 addRoute。
- **Planning Depth 原因：** 授权安全和全局导航事故半径最高，且需要 expand-migrate 兼容。

## 2. 决策状态

### 已锁定决策

- 服务端按 Client 裁剪菜单仍是权威；前端只映射 registry 并 `addRoute`，不二次跨 Client 过滤。
- 恢复顺序固定 `getInfo -> getRouters -> addRoute -> replace`；Client 无效在 auth 请求前 fail-close。
- `superadmin` 是规范角色，`admin` alias 只在有运行证据的兼容窗口保留；`*:*:*` 保持全权限语义。
- `DEV-T05-003` 将 AC-012 的全局动态路由收口明确交给本 Ticket：任意未命中物理 views glob/manifest registry 的后端 component key 必须进入稳定可见失败反馈，诊断含 App、domain 推断与原始 key；E2E 必须使用不存在对应 facade 的 typo key，不能复用预置 sentinel。
- `DEV-T07-001`：T-07 可向根 manifest 仅添加其兼容 facade 实际消费的 identity-access/web-domain/platform-permission 内部 `workspace:*` 声明，并写匹配 root lock specifier、platform-permission importer 与 T-07 writable manifests 的机械 importer 更新；不得修改 root scripts、workspace/catalog、外部版本、既有 resolution 或无关 lock 节点。
- `DEV-T07-002`：T-01 的直接 `/auth/login` characterization 是迁移前基线，已被本 Ticket 的 `AC-015` 严格 Client 前置合同取代。T-07 可更新 `<Path>plus-ui-namewta/src/utils/request.test.ts</Path>` 中该测试及所需 session mock，使其验证 `context -> code -> login`；生产 `<Path>plus-ui-namewta/src/utils/request.ts</Path>` 继续只读，401/request adapter 断言不得放宽。

### 已采用的低影响假设

- Pinia/Vue Router facade 保留到 T-15，纯 evaluator/use cases 可脱离 Vue 测试。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| identity domain/web-domain、auth/permission/runtime、兼容 stores/guard/UI、双 Client 测试 | T-04 adapters、T-05 registry、后端菜单裁剪 | system 用户管理页面、业务域 routes、旧入口删除 |

## 4. 要构建什么

任一 App 注入 ClientContext 和 manifests 后，用户登录/注册/OAuth、恢复用户与菜单、导航受保护页面、求值按钮权限和处理 401 均遵循统一语义；Client 错误不发请求，未知 key 明确失败，admin/client 会话不串用。

## 5. 实现契约

- **入口/输入输出：** AuthService、SessionService、AccessEvaluator、RouteAssembler、NavigationPort；Client/credential/menu/permission 输入，session/routes/decision 或稳定错误输出。
- **公共接口变化：** identity/platform exports；旧 APIs/stores/guard/directives 变 facade。
- **不变量/数据流：** validated Client -> auth -> token -> info -> Client-filtered menus -> registry -> addRoute -> replace；server authorization authoritative。
- **失败行为：** invalid Client fail-close；401 单例 logout/redirect；duplicate/missing key 使用稳定诊断。
- **兼容/安全：** token 隔离、不记录 secret；alias 是否保留由 auth matrix Evidence 判定，不猜测删除。

## 6. 执行路线

1. 扩充 T-01/T-06 的 auth、router、permission 与双 Client 失败特征测试。
2. 提取纯 auth/session/access/route contracts 和 identity domain APIs/models。
3. 迁移登录/注册/OAuth web-domain 与 admin/client composition。
4. 将旧 stores/guard/directives/plugins/APIs 改为兼容 facade，接入 manifest registry。
5. 执行 alias 使用扫描与 auth matrix，决定显式保留；不得无证删除。
6. 跑全门禁和 Lead E2E，Gate G1 失败时回切 facade。

## 7. 路径访问契约

- **可写：** identity/platform auth/runtime、新旧认证路由路径和 auth E2E；**只读：** adapters/client App/root config。
- **共享路径：** permission、router、user/permission stores 唯一 owner `T-07`；业务域不得写全局 registry/guard。`DEV-T07-001` 仅委托根内部依赖声明及匹配 lock 机械变化，T-03 保留策略所有权。
- **保留或不动：** T-04 request/auth facade、后端权限合同、业务域实现。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | unit + Playwright | router/store/evaluator tests；双 App 登录 | 恢复顺序、addRoute、权限和 Client 菜单正确 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-07.md</Path>` |
| 失败路径 | auth/request/E2E | Client 缺失/类型错、401、无物理 facade 的真实未知 key | 请求前阻断；单例恢复；诊断含 App/domain/key 且可见 | 同上 |
| 回归 | matrix/build | superadmin/admin/*:*:*；双 App build/E2E | 授权不扩大且兼容决定有证据 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：安全、网络、session、router、UI 全链路必须在集成环境验证。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；运行多 Client、401、动态路由、权限矩阵。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G1 和 alias 结论。

## 9. 发布、迁移与恢复

- **迁移/兼容：** core expand -> 双 App migrate -> facades；所有旧入口保留至 T-15。
- **监控：** auth failures、Client mismatch、401 concurrency、missing keys、E2E matrix。
- **回滚：** facade 切回旧 stores/guard；安全失败保持 fail-close，禁止默认 Client。
- **批准点/收缩：** Lead 审批 Gate G1；alias 删除需零使用证据；旧 auth 路径删除只在 T-15。

## 10. 验收标准

- [x] `AC-012`：无物理 facade 的任意未知后端 component key 进入稳定可见诊断，不返回 `undefined` 或静默空白。
- [x] `AC-013/AC-014`：恢复顺序保持，菜单只映射/addRoute。
- [x] `AC-015/AC-016`：Client 错误 fail-close；401 单例 logout/redirect。
- [x] `AC-017/AC-018`：超管/权限矩阵和多 Client 会话隔离通过。
- [x] `AC-021` 与 required E2E 有真实 Gate 结果；失败保留 facade。
- [x] shared owner、commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-07.md</Path>`。
