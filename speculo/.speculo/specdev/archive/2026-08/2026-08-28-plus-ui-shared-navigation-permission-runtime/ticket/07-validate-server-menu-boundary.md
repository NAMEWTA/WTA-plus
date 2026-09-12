---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-07
title: 建立递归菜单领域边界与显式 Router 适配
status: done
planning_depth: deep
planning_depth_reason: 该修复跨 HTTP unknown、Admin Domain、Platform 纯投影和 Vue Router App 适配四个边界，需消除宽泛 Record 与双重断言且保持 Platform 终端无关。
ready: true
risk: high
blocked_by: [T-05]
contract_ids: [AC-006, AC-007, AC-009, AC-010, AC-012, AC-014]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/packages/domains/admin/src/**</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/serverMenuAdapter.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/serverMenuAdapter.test.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/admin/src/**</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/serverMenuAdapter.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/serverMenuAdapter.test.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/api-contracts/src/generated/**</Path>", "<Path>ruoyi-vue-plus-namewta/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/manifestDiagnostic.ts</Path>"]
shared_paths: ["<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path> => T-07"]
---

# Ticket T-07: 建立递归菜单领域边界与显式 Router 适配

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/07-validate-server-menu-boundary.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>`

## 1. 战略与来源

- **目标：** 在 Admin Domain 入口递归验证服务端菜单，输出终端无关的已缩窄模型，再由 Admin 显式适配为 Vue Router 类型。
- **可观察产出：** 合法菜单与现有路由行为一致；嵌套畸形菜单在进入 Platform/Router 前稳定失败关闭；生产链路不再以 `as unknown as` 穿透领域、平台和 Router 边界。
- **来源：** `AC-006`、`AC-007`、`AC-009`、`AC-010`、`AC-012`、`AC-014`、`CR-001` 标准轴 finding、`TS-001`、`TS-002`、`TS-005`、`DEC-002`。
- **当前事实：** `IdentityMenu` 是 `Readonly<Record<string, unknown>>`，`parseMenus` 只验证顶层对象；navigation Store 随后在 IdentityMenu、ServerRouteNode 与 RouteRecordRaw 之间使用多次双重断言。
- **Planning Depth 原因：** 修复必须同时尊重 OpenAPI 传输事实、Domain 所有权、Platform 纯度和 App Router 所有权，错误抽象会形成新的共享耦合。

## 2. 决策状态

### 已锁定决策

- 权威数据流固定为 `HTTP/JSON unknown -> domain-admin 递归校验 ServerMenuNode -> platform-app-runtime 纯投影 -> Admin 显式 RouteRecordRaw adapter`。
- 递归模型由 Admin Domain 拥有且不导入 Vue Router；Platform 不接管 Router 类型、Pinia、Layout 或诊断 UI。
- 传输层可引用生成的 `RouterVo` schema 作为编译期事实，但不得手改生成 OpenAPI；运行时仍以 unknown 输入进行递归验证。
- 畸形节点必须在状态写入和 `addRoute` 之前失败关闭，并返回稳定、可定位的错误。

### 已采用的低影响假设

- 只验证投影实际依赖的已知字段和递归 children；未知扩展字段可保留或忽略，但不能借此跳过已知字段类型检查。
- Adapter 文件名可按现有 Router 目录命名调整，只要 owner 和公开边界不变。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| Domain 递归菜单模型/校验、Platform 输入收窄、Admin Router adapter、失败测试 | 生成 RouterVo、现有纯投影、manifest resolver、diagnostic presenter | 不建立公共 Router/Store，不修改后端协议，不迁移 navigation Store、manifest registry 或诊断 UI |

## 4. 要构建什么

Admin 获取菜单响应后，Domain transport 把未知 JSON 递归校验为明确的 ServerMenuNode。Platform 只消费该终端无关模型完成特殊组件、children 和诊断投影；Admin adapter 最后把投影显式转换为 RouteRecordRaw。任意深度的错误字段、无效 children 或不满足投影约束的节点都在写入导航状态前返回稳定错误，不能依赖双重类型断言继续执行。

## 5. 实现契约

- **入口或接缝：** Admin Domain 菜单 transport/parser、Platform `assembleServerRoutes`、Admin navigation Store 和 App-owned Router adapter。
- **输入与输出：** unknown HTTP body -> validated recursive domain nodes -> platform route projection -> explicit RouteRecordRaw。
- **公共接口变化：** Domain 新增/收窄菜单模型；Platform 输入类型可收窄为结构化节点，但不得暴露 Vue Router 类型。
- **不变量：** 后端 Client 裁剪菜单权威、manifest-only、ParentView/InnerLink 语义、重复 name 诊断和恢复顺序保持。
- **状态或数据流：** 校验完成前不得写 Pinia；适配只发生在 Admin App 边界。
- **错误与失败行为：** 任意嵌套畸形输入稳定失败，既不生成部分路由也不保留上一次失败请求的半成品状态。
- **兼容要求：** 不引入宽泛 fallback、双模型或 `as unknown as` 兼容门面。
- **安全与隐私要求：** 菜单校验不替代后端鉴权；失败时不得恢复本地动态页面或扩大可见导航。

## 6. 执行路线

1. 以合法、深层合法和多类嵌套畸形 RouterVo fixture 建立 Domain parser 失败测试。
2. 定义终端无关递归模型并完成 runtime narrowing；生成 OpenAPI 保持只读。
3. 收窄 Platform 投影合同，保留纯函数与可注入 resolver/diagnostic 接缝。
4. 在 Admin Router 目录建立显式 adapter，移除 production 链路中的双重断言并更新 Store 测试。
5. 运行 Domain、App Runtime、Admin 定向测试及 architecture/typecheck/lint；Lead 在 candidate 跑 app-runtime browser baseline。

## 7. 路径访问契约

- **预计修改点：** frontmatter 列出的 Domain、App Runtime、navigation Store 和 Admin Router adapter/test。
- **可写范围：** 仅 frontmatter `writable_paths`；若必须手改生成 schema 或后端，停止并升级偏差。
- **只读上下文：** 生成 API 合同、后端 RouterVo、manifest registry 和诊断呈现。
- **共享路径：** App Runtime route assembler 由 T-07 唯一拥有；T-06 与 T-08 不写该路径。
- **保留或不动：** Admin 守卫、Layout、白名单、权限指令与 Web Domain 页面。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Domain + Platform + Admin tests | 合法多级菜单、ParentView、InnerLink、manifest route | 输出与现有行为一致，无双重断言 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>` |
| 失败路径 | 递归 parser 与 Store test | 非对象节点、错误字段、畸形 children、未知组件 | 写状态/addRoute 前稳定失败且无部分投影 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>` |
| 回归 | 静态、架构和浏览器基线 | architecture、typecheck、lint、packages/Admin tests、app-runtime baseline | Platform 无 Vue 依赖，Admin 动态导航行为不变 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>` |

- **Workspace checks：** source worktree 跑 Domain/App Runtime/Admin 非 E2E；Lead 在 parent-candidate 跑组合检查。
- **E2E disposition：** required：菜单解析、Platform 投影和真实 Admin Router 注册跨越生产边界。
- **E2E owner/environment：** Lead / parent-candidate；至少运行 app-runtime baseline 的登录、菜单恢复、manifest 页面和未知组件场景。
- **Integration evidence：** 记录 source commit、parent before、candidate/result SHA、E2E 结果和前端 main 包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 可与 T-06 从 T-05 result 独立开发；candidate 必须按 Goal Plan 串行并基于最新 main 重建。
- **兼容窗口：** 无；校验模型和显式 adapter 直接替换宽泛断言路径。
- **监控信号：** parser 失败用例、双重断言扫描、包测试、architecture/typecheck 和定向 E2E。
- **回滚或前向恢复：** candidate 失败时父分支不动；在 source worktree 修复，不回退到宽泛 Record 或本地 fallback。
- **不可逆操作与批准点：** implementation commit、candidate integration 和 cleanup 均需新的用户授权。
- **收缩条件：** production 菜单链路中的 `as unknown as` 和 `Readonly<Record<string, unknown>>` 边界零匹配，并有 Evidence。

## 10. 验收标准

- [ ] Domain 拥有递归、终端无关的 ServerMenuNode 并校验投影依赖字段与 children。
- [ ] Admin 通过显式 adapter 进入 RouteRecordRaw，production 链路无双重断言。
- [ ] 畸形嵌套菜单在状态写入/addRoute 前稳定失败关闭，无部分导航投影。
- [ ] Platform 继续不依赖 Vue、Vue Router、Pinia、DOM 或 Element Plus。
- [ ] 验证、E2E 和 source/candidate/result 记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-07.md</Path>`。
