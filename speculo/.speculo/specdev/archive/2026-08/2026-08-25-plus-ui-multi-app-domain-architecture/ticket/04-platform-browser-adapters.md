---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-04
title: 提取平台端口与浏览器适配器
status: done
planning_depth: deep
planning_depth_reason: 抽取共享 request、token、storage、crypto 和错误展示公共边界，同时必须保持现有认证与 401 安全语义
ready: true
risk: high
blocked_by: [T-03]
contract_ids: [AC-006, AC-007, AC-008, AC-021]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/adapters/axios-browser/**</Path>", "<Path>plus-ui-namewta/packages/adapters/storage-browser/**</Path>", "<Path>plus-ui-namewta/packages/adapters/crypto-browser/**</Path>", "<Path>plus-ui-namewta/src/utils/request.ts</Path>", "<Path>plus-ui-namewta/src/utils/auth.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/adapters/axios-browser/**</Path>", "<Path>plus-ui-namewta/packages/adapters/storage-browser/**</Path>", "<Path>plus-ui-namewta/packages/adapters/crypto-browser/**</Path>", "<Path>plus-ui-namewta/src/utils/request.ts</Path>", "<Path>plus-ui-namewta/src/utils/auth.ts</Path>", "<Path>plus-ui-namewta/src/utils/request.test.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>", "<Path>plus-ui-namewta/src/store/modules/user.ts</Path>", "<Path>plus-ui-namewta/src/permission.ts</Path>", "<Path>plus-ui-namewta/e2e/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/src/utils/request.ts</Path>", "<Path>plus-ui-namewta/src/utils/auth.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/src/utils/request.ts</Path> => T-04", "<Path>plus-ui-namewta/src/utils/auth.ts</Path> => T-04", "<Path>plus-ui-namewta/package.json</Path> => T-03 policy owner; T-04 scoped writer for workspace:* declarations actually consumed by its root facades only (DEV-T04-001)", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03 policy owner; T-04 scoped writer for matching root specifiers and importers generated from T-04 manifests only (DEV-T04-001)"]
---

# Ticket T-04: 提取平台端口与浏览器适配器

- **Ticket/Map/Spec/Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/04-platform-browser-adapters.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-04.md</Path>`。

## 1. 战略与来源

- **目标：** 让 domain 依赖抽象 Http/Session/Storage/Crypto/Error ports，由 Web App 组合 axios-browser 等 concrete adapters。
- **可观察产出：** 当前管理端通过兼容入口使用新适配器，登录 token、错误提示、下载和 401 回跳行为不变；platform 包不含浏览器/UI 依赖。
- **来源：** `US-004`、`US-006`、`US-007`、`AC-006`、`AC-007`、`AC-008`、`AC-021`、`ADR-002`、`ADR-005`、`ADR-006`。
- **当前事实：** request/auth 直接耦合 axios、Element Plus、浏览器存储与路由副作用，是所有 domain 迁移的前置阻碍。
- **Planning Depth 原因：** 公共端口和认证失败语义影响全部后续包，且跨兼容入口完成 expand。

## 2. 决策状态

### 已锁定决策

- platform 只定义端口、值对象和纯策略；adapters 实现浏览器能力，App 负责组合。
- 现有 request/auth 路径暂作兼容 facade；T-15 前不得删除。
- 401 的单例提示、登出和 redirect 语义必须保持，展示通过 ErrorPresenter 注入。

### 已采用的低影响假设

- 首期保持 axios 作为浏览器 HTTP 实现，不更换传输库。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 平台端口、浏览器 adapters、request/auth 兼容 facade、测试 | 当前 axios 拦截器语义、token key、Client header、T-01 基线 | 业务 domain、登录 UI、动态路由、移动端 adapter 实现 |

## 4. 要构建什么

App 组装浏览器 adapters 后，调用者通过 platform/domain 端口发出带 Token 和 ClientContext 的请求；成功响应、业务错误、网络错误和 401 得到与当前管理端等价结果。无头包可在不加载 DOM、Vue Router 或 Element Plus 的测试环境导入。

## 5. 实现契约

- **入口或接缝：** HttpClient、SessionStore、StoragePort、CryptoPort、ErrorPresenter；axios-browser factory；旧 request/auth facade。
- **输入与输出：** typed request/ClientContext/session 输入；typed response 或稳定错误分类输出。
- **公共接口变化：** 新增 package exports；旧默认 request/auth 导入保持兼容。
- **不变量：** domain 不见 concrete adapter；Client/Token header 规则不漂移；401 只触发一次恢复流程。
- **状态或数据流：** App config -> adapter factory -> interceptors -> platform result/error -> caller；session side effect 由注入端口执行。
- **错误与失败行为：** Client 缺失/非法时 fail-close；业务/网络/401 分类稳定；ErrorPresenter 不可用时仍保留结构化错误。
- **兼容要求：** 根 App 调用点无需一次性迁移；compat facade 和新端口同时存在到 T-15。
- **安全与隐私要求：** token 不写日志；不同 App storage namespace 可隔离；不得默认回退 Client ID。

## 6. 执行路线

1. 用 T-01 request/session 特征测试锁定成功、业务错误、网络错误、401 和 Client header。
2. 建立 platform 端口和值对象，证明无 DOM/Vue/Element/concrete adapter 依赖。
3. 实现 axios/storage/crypto browser adapters 和注入式 ErrorPresenter 接缝。
4. 将旧 request/auth 改为兼容 facade，逐项对照现有调用行为。
5. 运行 import graph、unit、typecheck、管理端 build 与 Lead E2E。
6. 记录兼容窗口、shared owner 与恢复点后形成 commit。

## 7. 路径访问契约

- **预计修改点/可写范围：** 新 platform/adapters 包、request/auth facade 及其定向测试。
- **只读上下文：** 根 manifest 策略由 T-03 拥有；user store、permission 和 E2E 只用于兼容判断。
- **共享路径：** request/auth facade 由 `T-04` 唯一修改；`T-07` 只能消费其 exports。`DEV-T04-001` 允许 T-04 仅向根 manifest 增加 facade 实际导入的新包 `workspace:*` 声明，并写匹配的根 lock specifier 与本 Ticket manifests 机械生成的 importers；T-03 仍拥有 workspace/catalog/lock 策略，禁止改根 scripts、catalog、外部版本、无关依赖或既有 resolution。
- **保留或不动：** 除 `DEV-T04-001` 精确声明外的根依赖配置、业务 API/views、后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | adapter/compat unit | `pnpm test` 定向 platform/request | header、response、storage 与旧行为一致 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-04.md</Path>` |
| 失败路径 | session/error unit | 缺 Client、业务错、网络错、并发 401 | fail-close；结构化错误；401 单例 | 同上 |
| 回归 | graph/type/build/E2E | architecture check、typecheck、build；Lead 基线 E2E | domain 边界合法且 admin 路径通过 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、build。
- **E2E disposition：** required：request/session 适配跨网络、存储、展示与浏览器导航。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖登录、Client header、401 redirect。
- **Integration evidence：** 记录 source commit、parent before、candidate/result SHA、测试报告和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 端口 expand -> adapters -> compat facade -> 调用方逐 Ticket migrate -> T-15 contract。
- **兼容窗口：** `<Path>plus-ui-namewta/src/utils/request.ts</Path>` 与 auth facade 保留至 T-15。
- **监控信号：** 401 次数、Client 缺失错误、request 测试、import graph、admin E2E。
- **回滚或前向恢复：** 在兼容 facade 内切回旧实现；不得通过默认 Client 绕过失败。
- **不可逆操作与批准点：** 无；compat 删除需 T-15 Lead 批准。
- **收缩条件：** 旧 request/auth import 为零且双 App 的 auth/E2E/build 全绿。

## 10. 验收标准

- [x] `AC-006/AC-007/AC-008`：platform/domain 边界无 Web 依赖、只走 exports、依赖图无环。
- [x] `AC-021`：unit、architecture、typecheck、build 和 required E2E 有真实结果，失败不删除 facade。
- [x] Client/Token/401/error 语义保持且敏感数据不泄漏。
- [x] shared owner、迁移窗口、commit/candidate/result SHA 记录到 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-04.md</Path>`。
