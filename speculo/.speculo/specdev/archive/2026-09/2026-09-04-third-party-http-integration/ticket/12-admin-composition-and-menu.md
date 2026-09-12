---
schema_version: 3
artifact: ticket
change: 2026-09-04-third-party-http-integration
id: T-12
title: 组合 Admin Web 与系统管理下三方接口菜单
status: done
planning_depth: deep
planning_depth_reason: 修改 App 全局组合、动态 manifest、workspace lock 和菜单权限 DML，必须保证 SQL、后端、domain 与路由键原子一致。
ready: true
risk: high
blocked_by: [T-11]
contract_ids: [AC-015, AC-016]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/61-third-dml.sql</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/third/**</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/tests/e2e/third/**</Path>"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/61-third-dml.sql</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/third/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/third/**</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/10-ruoyi-base.sql</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/61-third-dml.sql</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/apps/admin-web/package.json</Path> => T-12"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path> => T-12"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => T-12"
  - "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-12"
  - "<Path>release-artifacts/docker/infrastructure/mysql/init/61-third-dml.sql</Path> => T-12"
---

# Ticket T-12: 组合 Admin Web 与系统管理下三方接口菜单

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/12-admin-composition-and-menu.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>`

## 1. 战略与来源

- **目标：** 将独立 third domain/web-domain 显式组合到 Admin App，并以数据库菜单投影到“系统管理 → 三方接口管理”。
- **可观察产出：** 有权限管理员登录后看到目录和四子页，动态 component key 全部解析；无页面/按钮权限时前后端均拒绝。
- **来源：** `US-008`、`AC-015`、`AC-016`、`ADR-012`、`USER-DECISION:system-management-third-party-admin-menu`。
- **当前事实：** T-10/T-11 提供包与页面，Admin package/services/manifest、lock 和 61-third-dml.sql 尚未引用。
- **Planning Depth 原因：** 全局组合与菜单权限是共享入口，任一字符串漂移会导致运行时空白路由或授权缺口。

## 2. 决策状态

### 已锁定决策

- 菜单放在现有“系统管理”父菜单下，目录名“三方接口管理”，四子页顺序为供应商、Endpoint、调用记录、调用统计。
- frontend 源码归 third 包，不复制进 system；Admin 只注入 runtime 与注册 manifest。
- SQL、Controller、domain permission、按钮和 manifest component key 必须精确一致。
- 菜单种子只创建配置管理权限，不预置真实 Provider URL/凭据。

### 已采用的低影响假设

- 菜单 ID 使用当前 DML 约定的保留区间并通过唯一扫描证明无冲突。

### 未决问题

无。

## 2.1 必须加载的 Skill 与工程基线

- **必须加载：** <Path>.agents/skills/engineering-standards/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>、<Path>.agents/skills/ruoyi-module-guide/SKILL.md</Path>。
- **必须先读的参考：** <Path>.agents/skills/namewta-fullstack-development/references/frontend/architecture.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/naming-and-layout.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/implementation.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/frontend/permission-routing.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/persistence-transactions-and-ddl.md</Path>、<Path>.agents/skills/namewta-fullstack-development/references/backend/verification.md</Path>、<Path>.agents/skills/ruoyi-module-guide/references/modules/system/index.md</Path>。
- **目录与组合最低要求：** Admin 仅在 package.json、application/services.ts、adminManifestRegistry.ts 三个显式入口组合 third；依赖、service、manifest 集合必须一致。组件 key、后端 path、permission code 和 SQL menu/code 一一对应，禁止顶层副作用自动注册。三方菜单 SQL 独立写入 61-third-dml.sql，权限字符串使用 third:<resource>:<action>。
- **工程验证要求：** lock 与 package 声明同步，运行 architecture/test/typecheck/lint/build；菜单 fresh-init 在 System Management 父 ID 下验证唯一性。后端菜单/权限变更遵守 GET/POST、@Log 和既有自有表/DDL 基线。
- **执行停止条件：** 把 third 页面复制进 system、只更新一处组合入口、直接修改基础菜单、自动给角色赋权、提交真实凭据、或 manifest/key/permission/SQL 任一处漂移时立即停止。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Admin dependencies/runtime/manifest、lock、菜单/权限 DML、路由与浏览器 E2E | domain/web-domain exports、server menu adapter、现有 System Management ID | 页面实现、后端 CRUD、默认真实 Provider、自动角色赋权 |

## 4. 要构建什么

fresh-init 数据库提供菜单/按钮权限，Admin App 加载服务器菜单后将四个 component key 解析到 third 页面。用户只能看到已授予菜单，按钮同时由 `v-hasPermi` 与后端权限约束；直接输入 URL 或伪造 POST 均不能越权。无 Provider 数据时页面以空态正常工作。

## 5. 实现契约

- **入口或接缝：** Admin package composition、services runtime、admin manifest registry、sys_menu DML。
- **输入与输出：** server menu component/permission → lazy page component 与可执行 domain service。
- **公共接口变化：** Admin 显式依赖两个 third package；不扩大 package 深导入。
- **不变量：** component keys 四处一致；菜单父 ID 正确；按钮权限不替代后端授权；lock 与 package 声明同步。
- **状态或数据流：** DB menu → backend menu DTO → serverMenuAdapter → admin manifest → web-domain page。
- **错误与失败行为：** 未知 key 进入现有诊断；缺权限路由不可达/按钮隐藏，伪造请求由后端拒绝。
- **兼容要求：** 现有 system 菜单与 manifest 不重命名；新菜单 additive。
- **安全与隐私要求：** seed 无 secret/base URL；E2E 不将测试凭据截图/持久化。

## 6. 执行路线

1. 先建立菜单 ID/权限/component key 同步测试与 manifest 失败接缝。
2. 更新 Admin package、runtime service 注入和 manifest 注册。
3. 更新 pnpm lock 并运行 architecture/type gates。
4. 在 61-third-dml.sql 追加目录、四菜单和按钮权限，验证 ID/code 唯一。
5. 使用 MySQL fresh-init + backend + Admin 浏览器执行授权/越权/空态 E2E。

## 7. 路径访问契约

- **预计修改点/可写范围：** frontmatter 所列 Admin 共享文件、测试、lock 和 61-third-dml.sql。
- **只读上下文：** third packages 和 RuoYi 基础菜单。
- **共享路径：** App package/services/manifest、lock、61-third-dml.sql 由 T-12 唯一修改。
- **保留或不动：** domain/web-domain 源码、后端 Controller、角色自动赋权和真实供应商数据。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | MySQL/Admin browser | fresh-init 后授权用户展开菜单并打开四页 | key 全解析，页面空态/数据态正常 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-12.md</Path>` |
| 失败路径 | permission/E2E | 缺菜单/按钮权限、直接 URL、伪造 POST、未知 key | 前端关闭且后端拒绝，无空白页/越权 | 同上 |
| 回归 | monorepo/SQL gates | architecture、test、typecheck、lint、build 与 SQL unique/fresh-init | 现有 Admin 菜单/路由不回归 | 同上 |

- **Workspace checks：** source/current workspace 运行 Admin manifest tests、architecture、typecheck、lint、build 和 SQL 静态扫描。
- **E2E disposition：** required：服务器动态菜单、真实权限、后端和浏览器跨越多个运行时边界。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；MySQL/Redis/backend/Admin Web，桌面和移动视口覆盖无重叠。
- **Integration evidence：** implementation/source commit、parent before、candidate/result SHA、SQL checkpoint 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 50 DDL → 后端 → third packages/App → 61-third-dml.sql → 人工角色赋权。
- **兼容窗口：** 后端可先部署；菜单 DML 最后开放入口。
- **监控信号：** unknown component、403、菜单加载失败、前端请求错误和页面异常。
- **回滚或前向恢复：** 先撤菜单/角色权限，再撤 App 注册；保留后端和历史数据。
- **不可逆操作与批准点：** 生产 DML、角色赋权和入口开放需发布批准，本 Ticket 不执行。
- **收缩条件：** 不适用：无旧菜单待删除。

## 10. 验收标准

- [x] `AC-015`：系统管理下目录、四页面和动态 key 全部可达。
- [x] `AC-016`：菜单/按钮/直接请求的权限闭环成立。
- [x] lock、DML、manifest、权限字符串和 package composition 一致。
- [x] required 浏览器 E2E、提交与集成 Evidence 完整。
