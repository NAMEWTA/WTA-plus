---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-08
title: 迁移工作流定义管理纵向切片
status: done
planning_depth: standard
planning_depth_reason: 迁移流程分类、定义和表达式的 API 到页面完整行为，但不处理运行时任务和跨领域用户选择
ready: true
risk: high
blocked_by: [T-07]
contract_ids: [AC-009, AC-010, AC-021]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/packages/domains/workflow/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/workflow/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/category/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/definition/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/spel/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/category/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/processDefinition/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/spel/**</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/workflow/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/workflow/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/category/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/definition/**</Path>", "<Path>plus-ui-namewta/src/api/workflow/spel/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/category/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/processDefinition/**</Path>", "<Path>plus-ui-namewta/src/views/workflow/spel/**</Path>", "<Path>plus-ui-namewta/e2e/workflow-definition.spec.ts</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/src/components/Process/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/src/router/adminManifestRegistry.ts</Path> => T-07 composition owner; T-08 scoped writer for workflow domain/manifest inputs and selected IDs only (DEV-T08-001)", "<Path>plus-ui-namewta/src/router/adminManifestRegistry.test.ts</Path> => T-07 test owner; T-08 scoped writer for workflow selected and runtime-key-unselected assertions only (DEV-T08-003)", "<Path>plus-ui-namewta/package.json</Path> => T-03 policy owner; T-08 scoped writer for two actually consumed workflow workspace:* dependencies only (DEV-T08-001)", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03 policy owner; T-08 scoped writer for matching root specifiers and workflow package importers only (DEV-T08-001)"]
---

# Ticket T-08: 迁移工作流定义管理纵向切片

- **工件：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/08-workflow-definition-slice.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-08.md</Path>`。

## 1. 战略与来源

- **目标/产出：** 流程分类、流程定义与 SpEL 管理通过 workflow domain/web-domain manifest 在 admin 中完整可用。
- **来源：** `US-004`、`US-005`、`AC-009`、`AC-010`、`AC-021`、`ADR-003`、`ADR-004`、`ADR-006`。
- **当前事实：** 三组 API/views 在根 src，适合先于任务运行时独立迁移。
- **Planning Depth 原因：** 多页面/API 的纵切，沿用已验证 manifest 模式，无新全局公共合同。

## 2. 决策状态

### 已锁定决策

- workflow 是一个 capability domain；本 Ticket 只激活 definition-admin 子入口并为 T-09 保留可扩展 exports。
- 原后端 endpoints、component keys 和权限标识不变，旧路径作为 facade。
- `DEV-T08-001`：T-08 可在 admin composition 中仅追加 workflow domain/manifest inputs 与 selected IDs，并添加实际消费的两个 workflow `workspace:*` 根依赖、匹配 lock specifiers 和两个新 package importers；不得改变 resolver/route assembler、identity/demo 选择、workspace/catalog、root scripts、外部版本或既有 resolution。
- `DEV-T08-002`：后端不在本 change 范围内，category/definition/SpEL 的存量 `PUT/DELETE` mutation 必须按原路径和方法迁移，不得单边改成 POST；例外在后端完成协调迁移并同步所有消费者后到期。
- `DEV-T08-003`：T-08 可在既有 admin registry unit 中仅追加 workflow definition keys 已选中、T-09 runtime key 未选中的断言；不得改写既有 identity/demo 断言或其他 router tests。
- `DEV-T08-004`：实际 candidate integration 使用 4 次，超过 Goal Plan 默认上限 3；前三次均在 parent 未推进时暴露专用 E2E fixture/确定性缺口，并经过 source-only 追加修复、双审和干净 candidate 重建。后续 Ticket 第 3 次失败时必须先停止并登记偏差，再决定是否运行下一候选。

### 已采用的低影响假设

- 包内部可按 `definition` 子模块组织，但消费者只从根 exports 导入。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| category/definition/spel API、模型、页面、manifest、兼容入口 | identity/runtime、platform、registry | task/instance/leave、Process/UserSelect、system user |

## 4. 要构建什么

admin 选择 workflow manifest 后可查询和管理分类、定义和表达式；权限、请求、错误和动态路由与迁移前一致。未选择 workflow 的 App 不包含这些页面。

## 5. 实现契约

- **入口/输入输出：** workflow root exports 与 manifest contributions；现有 DTO/VO 和页面交互保持。
- **公共接口变化：** 新增 workflow 公开 definition API；旧 src re-export。
- **不变量/数据流：** view -> domain use/API -> HttpClient；manifest key -> route component；只走 exports。
- **失败/兼容/安全：** 请求错误继续由 presenter 处理；服务端权限不变；旧路径保留到 T-15。

## 6. 执行路线

1. 盘点 endpoints、types、keys 与页面行为并补定向测试。
2. 提取无头 definition domain 与 root exports。
3. 迁移 web 页面、lang/hooks/components 和 manifest。
4. 旧路径改 facade，admin composition 选择 workflow。
5. 运行 graph/type/unit/build 与 Lead 页面 E2E，记录 Gate G2。

## 7. 路径访问契约

- **可写：** workflow 新包中 definition 范围、对应旧 API/views 和专用 E2E；**只读：** identity/platform/Process。
- **共享路径：** 无；不得写全局 router/store 或 T-09 运行时旧路径。
- **保留或不动：** task/instance/leave 和用户组件。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | unit/page E2E | API tests；Lead 三类页面 | CRUD/查询与路由可用 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-08.md</Path>` |
| 失败路径 | API/permission | 错误响应、无权限、未知 key | 稳定提示且不扩大权限 | 同上 |
| 回归 | graph/type/build | architecture/type/admin+client build | 旧入口兼容，client 未选不注册 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：动态路由页面和表单/API 行为跨边界。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；验证 category/definition/spel 关键路径。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G2。

## 9. 发布、迁移与恢复

- **迁移/兼容：** definition expand/migrate；旧路径到 T-15。
- **监控/回滚：** 页面 route/API 失败；manifest 可回切旧 views。
- **批准点/收缩：** Gate G2 后开始 T-09；删除由 T-15 零引用扫描决定。

## 10. 验收标准

- [x] `AC-009/AC-010`：workflow 职责可追踪且只有选中 manifest 才注册定义页面。
- [x] `AC-021`：适用门禁与 required E2E 有真实结果，失败不删旧入口。
- [x] 路径、commit/candidate/result SHA 记录到 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-08.md</Path>`。
