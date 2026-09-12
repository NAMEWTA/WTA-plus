---
schema_version: 3
artifact: ticket
change: 2026-08-28-plus-ui-shared-navigation-permission-runtime
id: T-04
title: 迁移 Admin 权限宿主与导航装配
status: done
planning_depth: deep
planning_depth_reason: 同时迁移认证守卫、Vue 全局权限宿主、Pinia 导航状态和动态菜单消费者，直接影响 Admin 全部受保护页面。
ready: true
risk: critical
blocked_by: [T-02, T-03]
contract_ids: [AC-001, AC-002, AC-003, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010, AC-011, AC-012, AC-014]
owner: codex:leadership-epoch-1
expected_changes: ["<Path>plus-ui-namewta/apps/admin-web/src/directive/index.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/directive/permission/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/permission.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/components/Breadcrumb/index.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/components/TopNav/index.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/layout/components/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry*</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/manifestDiagnostic*</Path>", "<Path>plus-ui-namewta/apps/admin-web/README.md</Path>"]
writable_paths: ["<Path>plus-ui-namewta/apps/admin-web/src/directive/index.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/directive/permission/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/store/modules/navigation.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/permission.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/components/Breadcrumb/index.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/components/TopNav/index.vue</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/layout/components/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry*</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/manifestDiagnostic*</Path>", "<Path>plus-ui-namewta/apps/admin-web/README.md</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/web-kit/permission/**</Path>", "<Path>plus-ui-namewta/packages/platform/permission/**</Path>", "<Path>plus-ui-namewta/packages/platform/app-runtime/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>", "<Path>plus-ui-namewta/e2e/app-runtime-baseline.spec.ts</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-04: 迁移 Admin 权限宿主与导航装配

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/ticket/04-migrate-admin-navigation-composition.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-04.md</Path>`

## 1. 战略与来源

- **目标：** 让 Admin 显式组合新 Web 权限宿主和菜单纯运行时，并把 permission Store 收口为 App 自有 navigation Store。
- **可观察产出：** Admin 登录、导航、侧栏、顶栏、标签页、动态页面、权限按钮和失败诊断与 T-01 基线一致，同时生产源码不再包含 App 私有权限指令或本地动态页面解析算法。
- **来源：** `US-001`、`US-002`、`US-004` 至 `US-007`、`AC-001` 至 `AC-012`、`ADR-001` 至 `ADR-003`。
- **当前事实：** `<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>` 混合导航状态与纯菜单算法，8 个布局/导航消费者使用旧 Store；指令仍直接导入 Admin evaluator。
- **Planning Depth 原因：** 这是认证后纵向链路的生产迁移汇合点，任何遗漏都可能导致空白页、循环导航或权限可见性回归。

## 2. 决策状态

### 已锁定决策

- 新正式 Store 命名为 navigation Store，Pinia id、导出名、变量名和调用点统一表达 navigation，不保留旧 alias。
- navigation Store 只拥有 routes、sidebar、topbar、default 投影和 Router 注册；菜单转换调用 T-03 公开能力。
- Admin evaluator provider 由 `<Path>plus-ui-namewta/apps/admin-web/src/application/access.ts</Path>` 提供给 T-02 安装入口。
- 动态领域页面只使用特殊组件、所选 manifest 或 App-owned manifest；新 Store 不包含 `import.meta.glob`、`loadView` 或本地权限 dynamic route 过滤。

### 已采用的低影响假设

- 现有布局 getter/action 的外部效果保持，局部命名按 navigation 语义调整；由 TypeScript、Store 测试和 Playwright 证明。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 权限指令安装、navigation Store、守卫、布局消费者、manifest/诊断接线、旧私有实现删除 | T-02 Web Kit、T-03 App Runtime、Platform Permission、现有 domain 服务和静态路由 | 不改 Router 静态表中的空 `dynamicRoutes` 导出，不改后端，不重构其他 Store/布局样式 |

## 4. 要构建什么

Admin 启动时通过自己的会话状态安装 Web Kit 权限指令。受保护导航先加载身份，再由 navigation Store 获取服务端 Client 菜单，使用共享纯流程和 Admin 选择的 manifest 生成三类投影，注册内部动态路由并恢复目标。布局只读取 navigation Store；未知组件与重复名称通过 Admin 诊断边界呈现。旧 permission Store、App 私有权限指令和菜单算法副本在同一 Ticket 内删除，不保留转发。

## 5. 实现契约

- **入口或接缝：** Admin directive 安装入口、路由守卫、navigation Store 和 manifest registry。
- **输入与输出：** 输入为当前 session evaluator、服务端 RouterVo、特殊组件和所选 manifests；输出为 Vue 指令、导航投影、Router 注册和稳定诊断。
- **公共接口变化：** Admin 内部 Store 正式入口改名；消费 T-02/T-03 的 workspace public exports。无后端或外部 wire 变化。
- **不变量：** `getInfo -> getRouters -> addRoute -> replace`；后端 Client 菜单权威；静态页面显式路由不变；无公共 Store/Router。
- **状态或数据流：** session -> evaluator provider；identity service -> menu projection -> navigation Store -> addRoute -> replace；布局从 Store 读取投影。
- **错误与失败行为：** 任一加载/解析/注册失败不 replace；未知键展示诊断；权限缺失移除元素；重复名称由 App 呈现且不由共享层调用 UI。
- **兼容要求：** 零兼容；旧 Store 导出、旧 directive 实现和旧算法直接删除。
- **安全与隐私要求：** 不前端补偿跨 Client 权限，不缓存 Token/权限快照，不以 UI 隐藏替代后端鉴权。

## 6. 执行路线

1. 在 T-02/T-03 已集成状态下迁移或补齐 Admin 定向测试，使其针对新正式入口先失败。
2. 将 directive 注册改为注入 Admin evaluator 的 Web Kit 安装入口，删除 App 私有权限指令实现。
3. 创建 navigation Store，使用共享菜单投影与诊断能力，移除本地 glob、local dynamic filter 和重复名称算法副本。
4. 一次性迁移守卫及全部布局/导航消费者，删除旧 Store 文件与导出，不建立 alias。
5. 校准 manifest/诊断接线和 Admin README，运行 Store、Router、directive 相关定向测试与源码扫描。
6. 由 Lead 运行 T-01 浏览器基线和适用 Admin E2E，确认生产纵向链路绿色。

## 7. 路径访问契约

- **预计修改点：** frontmatter 列出的 Admin 指令、Store、守卫、导航消费者、manifest/诊断与 README。
- **可写范围：** 仅列出的 Admin 路径；越界必须停止。
- **只读上下文：** 三个共享包、Router 静态表和 T-01 E2E。
- **共享路径：** 无；本 Ticket 是 Admin 迁移路径唯一 writer。
- **保留或不动：** `<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>` 留给 T-05 删除空 legacy export；其他 Store、样式和领域页面不动。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | Admin Store/Router + 浏览器 | Admin 定向 Vitest；T-01 Playwright | 三类导航投影、manifest 页面、指令和恢复顺序一致 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-04.md</Path>` |
| 失败路径 | 指令/菜单/守卫测试 + 浏览器 | 非法权限、未知键、重复名称和加载失败场景 | 全部失败关闭，不 replace 未注册目标 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-04.md</Path>` |
| 回归 | App 与工作区静态门禁 | Admin test/typecheck/build，根 architecture/lint/typecheck/test | 全部消费者迁移，无旧 Store/import，静态页面不变 | `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-04.md</Path>` |

- **Workspace checks：** current 策略在 current-workspace 严格串行；required 策略在 source-worktree 跑非 E2E，Lead 在 parent-candidate 跑组合检查与 E2E。
- **E2E disposition：** required：生产认证、权限、Router 和布局跨边界全部发生变化。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate，至少运行 T-01 基线和受影响权限/动态领域场景；source-worktree 不运行 E2E。
- **Integration evidence：** 记录 implementation/source commit、T-02/T-03 父依赖、parent before、candidate/result SHA、父分支包含关系和 Lead E2E。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-02 与 T-03 均已绿色并集成后，按“测试 -> 指令 -> Store -> 消费者 -> 删除旧入口 -> E2E”推进。
- **兼容窗口：** 无；新旧 Store/指令不得同时作为正式入口存在。
- **监控信号：** 定向测试、旧 import 零匹配、路由诊断、Playwright URL/网络顺序和浏览器 console error。
- **回滚或前向恢复：** 任一步红则停在 T-04 checkpoint，通过 Git 整体回退该 Ticket；不重新启用双路径。
- **不可逆操作与批准点：** 删除旧源码可由 Git 恢复；commit、candidate/direct-parent 集成和父分支更新需授权。
- **收缩条件：** 旧 permission Store、App 私有 permission directive、本地 glob、filterDynamicRoutes 和算法副本调用点均为零。

## 10. 验收标准

- [x] `AC-001` 至 `AC-012` 中映射本 Ticket 的行为在 Admin 组合状态成立。
- [x] 所有旧 Store 消费者使用 navigation Store，无 alias 或转发门面。
- [x] Admin 不再拥有权限指令算法、本地动态页面 glob、local dynamic route 权限过滤或重复名称算法副本。
- [x] Lead 在 direct-parent 或 parent-candidate 完成 required E2E，并记录 result SHA。
- [x] Evidence 完整写入 `<Path>{roots.state}/specdev/changes/2026-08-28-plus-ui-shared-navigation-permission-runtime/evidence/T-04.md</Path>`，未发生未批准偏差。
