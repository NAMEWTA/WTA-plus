---
schema_version: 3
artifact: ticket
change: 2026-08-30-openapi-common-module
id: T-11
title: 交付管理员动态页与个人中心 OpenAPI 双入口
status: done
planning_depth: deep
planning_depth_reason: 同一工作区组件必须支持 current-user/target-user 两种安全 scope，并正确落入动态菜单与静态个人中心两种组合机制。
ready: true
risk: high
blocked_by: [T-10]
contract_ids: [AC-023, AC-024, AC-025, AC-026, AC-027]
owner: codex:/root
expected_changes:
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/open-api/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/index.vue</Path>"
writable_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/open-api/**</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/runtime.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/README.md</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/index.vue</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/openApi.vue</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/openApi.test.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>"
read_only_paths:
  - "<Path>plus-ui-namewta/packages/domains/system/src/open-api/**</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/directives/permission/**</Path>"
shared_paths:
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/runtime.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/package.json</Path>"
  - "<Path>plus-ui-namewta/packages/web-domains/system/README.md</Path>"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/index.vue</Path>"
shared_path_owners:
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.ts</Path> => T-11"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/runtime.ts</Path> => T-11"
  - "<Path>plus-ui-namewta/packages/web-domains/system/src/index.test.ts</Path> => T-11"
  - "<Path>plus-ui-namewta/packages/web-domains/system/package.json</Path> => T-11"
  - "<Path>plus-ui-namewta/packages/web-domains/system/README.md</Path> => T-11"
  - "<Path>plus-ui-namewta/apps/admin-web/src/views/system/user/profile/index.vue</Path> => T-11"
---

# Ticket T-11: 交付管理员动态页与个人中心 OpenAPI 双入口

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/{change}/ticket/11-frontend-dual-openapi-ui.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/{change}/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/{change}/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>`

## 1. 战略与来源

- **目标：** 以一个共享 OpenAPI 工作区组件交付管理员目标用户入口与个人中心本人入口。
- **可观察产出：** 动态菜单 component key 可解析；个人中心出现静态 tab；两者按各自权限与 scope 安全执行生命周期操作。
- **来源：** `AC-023` 至 `AC-027`、前端显式组合与动态菜单路由规范。
- **当前事实：** system web-domain 通过 manifest 暴露动态页；admin profile tabs 是 App 自有静态组合点。
- **Planning Depth 原因：** 两种入口不能通过 UI 隐藏来代替后端 scope，且一次性 secret 的展示/复制/关闭状态不可出错。

## 2. 决策状态

### 已锁定决策

- 共享 `OpenApiWorkspace` 接受判别式 scope：`current-user` 或 `target-user + userId`。
- 管理员页面以 `system/openApi/index` component key 注册到 system manifest；个人中心由 App 显式新增 tab/wrapper。
- UI 仅按权限改善体验，后端仍是最终授权边界；admin 操作必须展示当前目标用户。
- secret 仅在 create/reset 成功 modal 中显示一次；关闭即丢弃，复制失败必须可见。

### 已采用的低影响假设

- 复用现有表格、对话框、结果态和图标库，不引入新全局 UI 状态库。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| shared workspace、system manifest、admin target page、profile tab、loading/empty/error/secret states | T-10 domain、web-domain runtime、App profile/permission conventions | landing page、凭据批量操作、Playwright 全链、移动端新 App |

## 4. 要构建什么

实现密集、工作导向的 OpenAPI 管理工作区，展示 catalog 与凭据摘要，提供 create/reset/enable/disable/delete。管理员动态页显式选择/接收目标用户，个人中心固定 current-user；两者共享行为但不会共享敏感瞬态状态。

## 5. 实现契约

- **入口或接缝：** system web-domain manifest key、App profile tab、T-10 domain functions。
- **输入与输出：** scope + optional userId -> catalog/credential view 与生命周期 commands。
- **公共接口变化：** system web-domain 新增 manifest component 与可供 App profile 包装的组件。
- **不变量：** current-user 不发送 userId；target-user 始终显示目标；secret 不进入 URL/store/cache；权限隐藏不替代请求授权。
- **状态或数据流：** entry scope -> shared workspace -> domain query/command -> explicit state -> discard one-time secret on close。
- **错误与失败行为：** loading、empty、disabled、expired、forbidden、conflict、copy failure 均有稳定可恢复状态。
- **兼容要求：** 既有 system manifest 与 profile tabs 不变形；动态菜单无法识别 key 时测试失败。
- **安全与隐私要求：** secret modal 禁止自动持久化/回显，delete/reset 有明确确认。

## 6. 执行路线

1. 先写 scope、状态机、一次性 secret 丢弃与权限测试。
2. 实现共享 workspace 与 admin target wrapper，注册 `system/openApi/index`。
3. 在 admin profile 显式组合 current-user tab/wrapper。
4. 补 manifest 解析/App 集成测试，跑 architecture/test/typecheck/lint/build。

## 7. 路径访问契约

- **预计修改点/可写范围：** system web-domain OpenAPI slice/root manifest，以及 admin profile wrapper/tab 和定向测试。
- **只读上下文：** T-10 domain、admin manifest registry、现有权限指令。
- **共享路径：** web-domain root/package 与 profile index 由 T-11 独占。
- **保留或不动：** domain 实现、router registry 生产代码、其他 App 与后端。
- **组合修订：** T-10 仅导出 `createOpenApiService(http)`，既有 `SystemService` 不包含该 slice。T-11 因此可在 App service composition 导出唯一实例，并只在 admin registry 的 `SystemWebRuntime` 对象注入该实例；registry 的 manifest 选择、resolver 和路由语义继续只读。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 双 scope 与状态机 | component tests | `pnpm --filter @namewta/web-domain-system test` | current/target 参数正确，所有可见状态稳定 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| 一次性 secret/复制 | component + App test | 同上并运行 admin profile 定向测试 | 只显示一次，关闭清除，复制失败可见 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| manifest/profile 组合 | App integration tests | `pnpm --filter @namewta/admin-web test -- adminManifestRegistry profile/openApi` | 动态 key 可解析且静态 tab 可达 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |
| 架构与构建 | workspace gates | `pnpm architecture:check && pnpm typecheck && pnpm lint && pnpm build:prod` | 无反向依赖、类型/构建回归 | `<Path>{roots.state}/specdev/changes/{change}/evidence/T-11.md</Path>` |

- **Workspace checks：** current-workspace 跑定向 Vitest；parent-candidate 跑全量 architecture/typecheck/lint/build。
- **E2E disposition：** not-required：用户已批准轻量门禁，以组件和 App 集成测试覆盖；完整 Playwright 明确保留为 residual risk。
- **E2E owner/environment：** Frontend owner / current-workspace 或 parent-candidate。
- **Integration evidence：** implementation/source、parent before、candidate/result SHA、父分支包含关系与测试输出。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-10 后实现，后端默认关闭时 UI 必须呈现 disabled 而非崩溃。
- **兼容窗口：** additive manifest/profile tab；无本地数据迁移。
- **监控信号：** API error category、runtime parse error、复制失败与前端异常。
- **回滚或前向恢复：** 移除菜单 DML/关闭后端功能并隐藏入口；不恢复已丢弃 secret。
- **不可逆操作与批准点：** 无；生产入口启用随 T-12 另行批准。
- **收缩条件：** 双入口、权限、secret 与构建门禁全部通过。

## 10. 验收标准

- [x] `AC-023` 至 `AC-027` 的管理员动态页和个人中心 tab 均可达。
- [x] component key 为 `system/openApi/index` 且 manifest 解析测试通过。
- [x] 双 scope、权限、全部空错态和一次性 secret 行为有测试。
- [x] architecture/test/typecheck/lint/build 与 Evidence/集成 SHA 完整。
