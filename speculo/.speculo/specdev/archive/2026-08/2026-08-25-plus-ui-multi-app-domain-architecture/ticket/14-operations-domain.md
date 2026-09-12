---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-14
title: 迁移监控与运维领域
status: done
planning_depth: standard
planning_depth_reason: 监控页面含敏感运维信息、通知和外部监控入口，需要保持 URL/权限安全测试但无新跨域公共合同
ready: true
risk: high
blocked_by: [T-09]
contract_ids: [AC-009, AC-010, AC-019, AC-021]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/operations/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/operations/**</Path>", "<Path>plus-ui-namewta/src/api/monitor/**</Path>", "<Path>plus-ui-namewta/src/views/monitor/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/operations/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/operations/**</Path>", "<Path>plus-ui-namewta/src/api/monitor/**</Path>", "<Path>plus-ui-namewta/src/views/monitor/**</Path>", "<Path>plus-ui-namewta/e2e/operations-domain.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/packages/domains/identity-access/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-14: 迁移监控与运维领域

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/14-operations-domain.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-14.md</Path>`。

## 1. 战略与来源

- **目标/产出：** monitor APIs/views 成为 operations domain/web-domain，admin 选择后保持在线用户、缓存、任务、服务监控等行为和安全边界。
- **来源：** `US-004`、`US-005`、`US-007`、`AC-009`、`AC-010`、`AC-019`、`AC-021`、`ADR-003`、`ADR-004`。
- **当前事实：** monitor 位于根 src；通知和外部 monitor URL 已有定向安全/单元测试。
- **Planning Depth 原因：** 运维数据敏感且含外部导航，需要失败与权限回归。

## 2. 决策状态

### 已锁定决策

- operations 包含 monitor capability，不机械按后端 controller 继续碎分。
- 外部 URL、通知和危险操作继续使用现有安全校验与服务端权限；UI 在 web-domain。
- `DEV-T14-001`：只可添加 operations 两个实际 `workspace:*` 根依赖、匹配 root lock specifiers 与机械 package importers，并在唯一 admin registry 追加 operations domain/manifest、selected IDs 和最小 request/modal/download/permission/IFrame/三个公开 monitor URL host ports；URL 信任与导航 intent 仍由 operations 验证。registry test 只追加 selected/unselected 断言；既有 composition/resolver/router/request/App、platform/identity、scripts/catalog/外部版本/既有 resolutions 与无关 lock 节点继续只读。

### 已采用的低影响假设

- 现有 monitor component keys 和 endpoints 保持不变。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| monitor APIs/models/pages/manifest/facades/security tests | platform adapters、identity permission、registry | 后端监控、基础设施部署、client 选择 operations |

## 4. 要构建什么

授权运维人员在 admin 中访问选择的监控页面、通知和允许的外部入口；无权限或不安全 URL 明确失败。client 未选择时不含任何 operations routes 或数据请求。

## 5. 实现契约

- **入口/输入输出：** OperationsService、manifest；monitor query/action/URL 输入，数据、导航 intent 或错误输出。
- **公共接口变化：** operations exports；旧 monitor paths facade。
- **不变量/数据流：** web -> domain -> platform; external navigation 经安全 port；server authorization authoritative。
- **失败行为：** 无权限、不安全 URL、API 失败不导航/不泄露数据。
- **兼容/安全：** 现有 monitor notify/security tests 保持；旧 paths 到 T-15。

## 6. 执行路线

1. 盘点 monitor API/views/keys 与安全测试。
2. 提取 operations domain 和公开 exports。
3. 迁移 web 页面/manifest，外部导航经 platform port。
4. 建立旧 paths facade。
5. 跑 security/unit/graph/type/build 与 Lead E2E，记录 Gate G6。

## 7. 路径访问契约

- **可写：** operations 新包、旧 monitor paths、专用 E2E；**只读：** platform/identity/root config。
- **共享路径：** 无；不得写全局 router 或 request facade。
- **保留或不动：** 后端和其他领域。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | service/E2E | monitor query/navigation | 授权页面和安全入口可用 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-14.md</Path>` |
| 失败路径 | security tests | 无权限、不安全 URL、API 错 | 不导航、不泄露、错误稳定 | 同上 |
| 回归 | graph/dual build | architecture/type/build | client 无 operations，旧 admin 兼容 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit/security、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：权限、外部导航和监控页面是安全相关浏览器行为。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖授权与拒绝场景。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G6。

## 9. 发布、迁移与恢复

- **迁移/兼容：** domain/web migrate，facade 到 T-15。
- **监控/回滚：** URL rejection、403、route/API errors；回切旧 monitor views。
- **批准点/收缩：** Gate G6；旧 imports T-15 清零。

## 10. 验收标准

- [x] `AC-009/AC-010/AC-019`：operations 可追踪、选择性注册且表现可独立定制。
- [x] `AC-021`：安全测试、build 与 required E2E 真实通过。
- [x] commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-14.md</Path>`。
