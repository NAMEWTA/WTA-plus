---
schema_version: 3
artifact: ticket
change: 2026-08-21-oss-direct-unified-notification
id: T-11
title: 通知监控前端
status: done
planning_depth: deep
planning_depth_reason: 展示永久明文敏感内容与完整目标，必须落实动态权限、服务端脱敏和 HTML 非执行安全边界。
ready: true
risk: high
blocked_by: [T-09]
contract_ids: [AC-027, AC-028, AC-029, AC-030]
owner: cursor-agent
expected_changes: ["<Path>plus-ui-namewta/src/api/monitor/notify/**</Path>", "<Path>plus-ui-namewta/src/views/monitor/notify/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/src/api/monitor/notify/**</Path>", "<Path>plus-ui-namewta/src/views/monitor/notify/**</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/src/views/monitor/operlog/**</Path>", "<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-11: 通知监控前端

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ticket/11-notify-monitoring-frontend.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-11.md</Path>`

## 1. 战略与来源

- **目标：** 提供安静、可扫描的通知监控列表与详情，并安全执行人工删除维护。
- **可观察产出：** 用户按权限查看脱敏列表、完整详情/Delivery/附件，执行单删、批删和清空。
- **来源：** `ADR-001/008/009`、`AC-027/028/029/030`。
- **当前事实：** 后端动态菜单模式和 monitor CRUD 先例存在，但没有 notify API 类型或页面。
- **Planning Depth 原因：** UI 直接暴露 OTP/Token/完整目标，HTML 渲染与权限误授风险高。

## 2. 决策状态

### 已锁定决策

- 路由由 T-02 DSL 动态下发，不添加静态路由。
- 列表只显示后端脱敏目标；详情在 `system:notify:query` 下显示完整正文/目标，无独立 reveal 权限。
- HTML 正文不得通过 v-html 在主页面执行；使用纯文本或隔离预览。
- 删除/清空只在 `system:notify:remove` 可见并二次确认；client_pk 只是来源列/筛选，不是数据域。

### 已采用的低影响假设

- 首版详情使用抽屉或对话框，布局遵循现有 monitor 页面，不新增卡片嵌套。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| API/types、分页筛选、详情、Delivery、附件入口、删除/清空、权限与安全显示 | T-09 HTTP、现有 monitor/operlog 页面和动态路由 | SQL、静态路由、编辑/重发、Excel 导出、独立 reveal 权限 |

## 4. 要构建什么

获权运维人员进入动态菜单后，按业务、Channel、Provider、状态、时间等筛选全局通知。列表只显示脱敏目标；详情展示完整敏感正文、模板参数、完整目标、Delivery、错误和附件。HTML 默认作为文本或隔离内容，不成为主应用 DOM。具有 remove 权限的用户可单删、批删或清空，并看到明确确认与后端结果。

## 5. 实现契约

- **入口或接缝：** `/monitor/notify` API 类型、动态组件 `monitor/notify/index`、权限指令。
- **输入与输出：** query -> paged masked rows；id -> full detail；ids/clean -> mutation feedback。
- **公共接口变化：** 新前端 API/types/view；无静态 route。
- **不变量：** 列表不拼接客户端脱敏；HTML 不直接执行；没有 remove 权限不呈现操作。
- **状态或数据流：** route permission -> list -> detail -> optional delete/refresh。
- **错误与失败行为：** 403、已删除记录、删除部分失败和附件 URL 过期有明确反馈。
- **兼容要求：** 动态组件路径与 T-02 DSL 完全一致。
- **安全与隐私要求：** 不缓存/持久化详情敏感数据；关闭详情清理状态；附件下载仍经授权 URL。

## 6. 执行路线

1. 固化 T-09 API 类型、权限键和 DSL 组件路径。
2. 实现密集可扫描的列表、筛选、分页和服务端脱敏展示。
3. 实现完整详情与 Delivery/附件，同时保证 HTML 非执行。
4. 实现单删、批删、清空的权限与确认交互。
5. 运行 lint/type/build，并做权限账户矩阵和恶意 HTML 浏览器人工验收；不建设 E2E 测试套件。

## 7. 路径访问契约

- **预计修改点：** 新 API 和 `views/monitor/notify/**`。
- **可写范围：** frontmatter 两个新目录。
- **只读上下文：** monitor 先例和 T-02 DSL。
- **共享路径：** 无；SQL 由 T-02 owner 修改。
- **保留或不动：** router、全局权限守卫、后端和其他 monitor 页面。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | UI/API | 列表、筛选、详情、Delivery、附件 | 数据完整且列表脱敏 | `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/evidence/T-11.md</Path>` |
| 失败路径 | 权限/安全人工验收 | 无权限账户、恶意 HTML、过期附件、删除确认 | 不越权、不执行 HTML、反馈清晰 | 同上 |
| 回归 | frontend gates | `pnpm lint`; `pnpm exec vue-tsc --noEmit`; `pnpm build:prod` | lint/type/build 通过 | 同上 |

- **Workspace checks：** current 在 `current-workspace`；required 在 source-worktree 跑非 E2E，Lead 在 parent-candidate 浏览器验证。
- **E2E disposition：** not-required：用户明确不执行 E2E；动态路由、权限和恶意 HTML 由前端门禁与 Lead 浏览器人工验收覆盖。
- **E2E owner/environment：** 不适用；Lead 在 current-workspace 记录 list/query/remove 权限账户和安全 HTML 人工 Evidence。
- **Integration evidence：** frontend source commit、parent before/result SHA、DSL+API 候选 SHA、截图/网络 Evidence。

## 9. 发布、迁移与恢复

- **迁移顺序：** T-02 DSL 与 T-09 API 先进入候选，随后页面同批发布和授权。
- **兼容窗口：** 菜单在页面/API 均可用后才分配角色；旧前端遇新菜单前不应启用该 DSL。
- **监控信号：** API 403/5xx、详情错误、删除操作和 CSP/HTML 安全告警。
- **回滚或前向恢复：** 先撤销菜单角色授权/DSL 启用，再回滚页面；后端日志继续保留。
- **不可逆操作与批准点：** 首次授予 query/remove 权限前审查角色范围；清空必须二次确认。
- **收缩条件：** 不适用：新增页面，无旧页面收缩。

## 10. 验收标准

- [x] `AC-027/028/029/030` 的 UI、权限和安全显示通过。
- [x] 动态组件路径与 DSL 一致，无静态路由改动。
- [x] 恶意 HTML 不执行，列表只显示服务端脱敏目标。
- [x] 前端门禁、安全合同测试、提交 SHA 与 Evidence 完整；按用户要求未执行 E2E。
