---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-12
title: 迁移 AI 领域与 Web 表现层
status: done
planning_depth: standard
planning_depth_reason: AI API、模型与页面数量较多并含流式交互，但沿用既定 domain、adapter 和 manifest 合同
ready: true
risk: medium
blocked_by: [T-09]
contract_ids: [AC-009, AC-010, AC-019, AC-021]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/ai/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/ai/**</Path>", "<Path>plus-ui-namewta/src/api/ai/**</Path>", "<Path>plus-ui-namewta/src/views/ai/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/ai/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/ai/**</Path>", "<Path>plus-ui-namewta/src/api/ai/**</Path>", "<Path>plus-ui-namewta/src/views/ai/**</Path>", "<Path>plus-ui-namewta/e2e/ai-domain.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-12: 迁移 AI 领域与 Web 表现层

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/12-ai-domain.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-12.md</Path>`。

## 1. 战略与来源

- **目标/产出：** AI transport、模型、应用服务与 Vue 页面分层，可由 admin 选择而 client 不选择，表现定制不复制 domain。
- **来源：** `US-003` 至 `US-005`、`AC-009`、`AC-010`、`AC-019`、`AC-021`、`ADR-002` 至 `ADR-004`。
- **当前事实：** AI API/views 位于根 src，具有独立后端 `ruoyi-ai` 来源。
- **Planning Depth 原因：** 多文件和流式/长请求行为需回归，但无新跨域核心合同。

## 2. 决策状态

### 已锁定决策

- AI domain 无 Vue/DOM/Element 依赖；Web 流式呈现、页面、样式和消息贡献在 web-domain。
- 后端 endpoints/component keys/权限不变，旧路径作 facade。
- `DEV-T12-001`：只可添加 AI 两个实际 `workspace:*` 根依赖、匹配 root lock specifiers 与机械 package importers，并在唯一 admin registry 追加 AI domain/manifest、selected IDs 和最小 request/trusted-credential/base-URL typed ports；registry test 只追加 selected/unselected 断言。既有 composition/resolver/router/request/App、platform/identity、scripts/catalog/外部版本/既有 resolutions 与无关 lock 节点继续只读，client-web 不修改且不选择 AI。
- `DEV-T12-002`：固定基线只有 `POST /snail-ai/user/register` 与 `ai/chat/index` 的 Snail AI iframe，没有 Fetch/SSE/ReadableStream/prompt/stream retry。T-12 将 planned stream lifecycle 忠实解释为注册请求、显式 runtime credential/base URL、iframe load/error、credential URL 清理与用户重试；禁止发明或复活已删除的流式 transport，禁止记录 prompt/token/credential。
- `DEV-T12-003`：双轴审查确认 iframe `error` 不是可靠网络失败信号。T-12 只可为 `AiWebRuntime` 增加 abortable same-origin GET `probeFrame` port，在唯一 admin registry 用 `redirect: error` 与成功 HTML content-type 实现，并在同一 registry test 增加聚焦 adapter 测试；session 必须先验证 root-relative base path，probe 成功后才发布 credential-bearing iframe URL，并在 timeout/dispose 清理。注册立即重试复用既有 `repeatSubmit: false`。该决定只替代 `DEV-T12-002` 的 iframe-error 失败信号，不授权新后端/postMessage/SSE/router/resolver/selection 或 credential 日志。

### 已采用的低影响假设

- 当前 iframe 生命周期所需浏览器能力位于 web-domain，并通过显式 runtime 值与无头 domain transport 分离；不在 domain 直接访问 global API。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| AI APIs/models/services/pages/manifest/facades | platform/identity/registry | 后端模型服务、client 选择 AI、全局 shell |

## 4. 要构建什么

admin 组合 AI manifest 后，现有 AI 页面和请求可用；client 未选择时无 AI routes。App 能调整 AI 页面布局和样式而不分叉 API、模型和业务服务。

## 5. 实现契约

- **入口/输入输出：** AI public exports/manifest；现有 request/response/stream 语义。
- **公共接口变化：** 新 package exports；旧 src facade。
- **不变量/数据流：** web -> domain -> platform transport；选择 manifest 才注册。
- **失败行为：** 网络/stream 中断明确结束 loading 并可重试；不泄露 prompt/token。
- **兼容：** admin 行为、keys、权限保持到 T-15。

## 6. 执行路线

1. 盘点 AI APIs/pages/keys 与流式失败行为。
2. 提取无头 domain 和公开 exports。
3. 迁移 web-domain 页面、hooks/lang/styles 与 manifest。
4. 建立 facades 和选择性组合测试。
5. 运行 graph/type/unit/build 与 Lead AI 路由/E2E，记录 Gate G5。

## 7. 路径访问契约

- **可写：** AI 新包、旧 ai API/views、专用 E2E；**只读：** platform/identity/root config。
- **共享路径：** 无；不得写全局 router 或 App root。
- **保留或不动：** 其他领域和后端。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | unit/E2E | AI request + admin route | 页面/请求/stream 可用 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-12.md</Path>` |
| 失败路径 | transport tests | 网络/stream 中断、无权限 | loading 收敛、稳定错误、不泄漏 | 同上 |
| 回归 | graph/dual build | architecture/type/build | client 无 AI，domain 无 Web 依赖 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：动态路由和流式 UI 需集成验证。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；验证 AI 页面和未选 App。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G5。

## 9. 发布、迁移与恢复

- **迁移/兼容：** domain/web expand -> admin migrate -> facade 到 T-15。
- **监控/回滚：** stream errors、route errors、build；回切旧 views。
- **批准点/收缩：** Gate G5；旧 imports 由 T-15 清零。

## 10. 验收标准

- [x] `AC-009/AC-010/AC-019`：AI 可追踪、选择性注册、表现定制不复制 domain。
- [x] `AC-021`：门禁和 required E2E 真实通过。
- [x] commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-12.md</Path>`。
