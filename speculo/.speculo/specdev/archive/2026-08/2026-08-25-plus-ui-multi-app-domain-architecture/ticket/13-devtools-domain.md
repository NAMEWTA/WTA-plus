---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-13
title: 迁移代码生成与开发工具领域
status: done
planning_depth: standard
planning_depth_reason: 代码生成跨 API、配置和复杂页面，并真实依赖 system 字典/菜单合同，但依赖方向已由 T-11 锁定
ready: true
risk: high
blocked_by: [T-11]
contract_ids: [AC-009, AC-010, AC-019, AC-021]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/devtools/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/devtools/**</Path>", "<Path>plus-ui-namewta/src/api/tool/**</Path>", "<Path>plus-ui-namewta/src/views/tool/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/devtools/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/devtools/**</Path>", "<Path>plus-ui-namewta/src/api/tool/**</Path>", "<Path>plus-ui-namewta/src/views/tool/**</Path>", "<Path>plus-ui-namewta/e2e/devtools-domain.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/domains/system-admin/**</Path>", "<Path>plus-ui-namewta/packages/platform/**</Path>", "<Path>plus-ui-namewta/package.json</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-13: 迁移代码生成与开发工具领域

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/13-devtools-domain.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-13.md</Path>`。

## 1. 战略与来源

- **目标/产出：** tool/gen API、模型和页面成为 devtools domain/web-domain，并只经 system public contracts 获取字典/菜单。
- **来源：** `US-004`、`US-005`、`US-012`、`AC-009`、`AC-010`、`AC-019`、`AC-021`、`ADR-003`。
- **当前事实：** codegen 页面直接消费 system dict/menu，是潜在 deep import/cycle 风险。
- **Planning Depth 原因：** 跨包依赖和代码生成下载行为需完整回归，但公共 seam 已由 T-11 提供。

## 2. 决策状态

### 已锁定决策

- devtools 是独立 capability，不并入 system-admin；只依赖其显式 public dict/menu contracts。
- 生成预览/下载和配置 UI 在 web-domain；生成 transport/models 在 domain。
- `DEV-T13-001`：T-11 result 后只可激活两个 devtools workspace 包及其 root/lock mechanical wiring；devtools domain 只从 system-admin public entry 消费 menu 与新增的 projected dict-type catalog，禁止 deep import system DTO/实现。唯一 admin registry 只追加 selected IDs 与最小 typed host ports；legacy `download.zip` 只可在原文件内硬化 GET ZIP 校验、finally cleanup 与 sanitized error，并补聚焦测试，禁止改通用 request/axios adapter、OSS download、resolver/router/App 或 client-web。
- `DEV-T13-006`：用户已授权把集成上限从 6 提高到 7 并继续完整 Goal Plan。第 7 次只可把专用 E2E 中未暴露的 `tree-root-value` test-id lookup 改为唯一可访问名称“根节点值”的 textbox locator；所有既有强断言保持。

### 已采用的低影响假设

- 生成模板与后端结果合同保持现状，不在本 Ticket 改生成策略。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| tool/gen API、配置、预览、下载、manifest、facades | system public dict/menu、platform download | OpenAPI 生成、后端 generator、system 实现 |

## 4. 要构建什么

admin 选择 devtools 后可配置、预览并下载生成结果；字典和菜单选择通过 system public contract，未选择的 App 不注册工具页面。失败下载不产生损坏文件。

## 5. 实现契约

- **入口/输入输出：** DevtoolsService、system public ports、manifest；生成配置输入，preview/file 或稳定错误输出。
- **公共接口变化：** devtools exports；旧 tool paths facade。
- **不变量/数据流：** web -> devtools domain -> transport；metadata -> system public ports；不得 deep import。
- **失败行为：** preview/download/metadata 错误可重试且不保存无效文件。
- **兼容/安全：** 权限/endpoints/keys 保持，不把数据库敏感元数据写日志。

## 6. 执行路线

1. 盘点 tool/gen 行为和 system imports，补失败测试。
2. 提取 devtools domain，替换为 system public contracts。
3. 迁移 web 页面/manifest 和下载适配。
4. 建立旧 paths facade。
5. 运行 graph/type/unit/build 和 Lead preview/download E2E，记录 Gate G6。

## 7. 路径访问契约

- **可写：** devtools 新包、旧 tool API/views、专用 E2E；**只读：** system public、platform、root config。
- **共享路径：** 无；不得修改 system-admin 以迁就 deep import。
- **保留或不动：** 后端生成策略和全局 router。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | service/E2E | metadata/preview/download | 完整生成路径可用 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-13.md</Path>` |
| 失败路径 | download tests | metadata/API/file 错误 | 明确错误且无损坏下载 | 同上 |
| 回归 | graph/dual build | no deep/cycle；build | 选择性注册，旧入口兼容 | 同上 |

- **Workspace checks：** source-worktree/current-workspace 运行 unit、architecture、lint、typecheck、双 build。
- **E2E disposition：** required：生成预览和浏览器下载跨网络/UI/file 边界。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；覆盖 preview/download 与失败。
- **Integration evidence：** source commit、parent before、candidate/result SHA、Gate G6。

## 9. 发布、迁移与恢复

- **迁移/兼容：** domain/web migrate，facade 到 T-15。
- **监控/回滚：** preview/download/system port failures；回切旧 tool views。
- **批准点/收缩：** Gate G6；旧 imports T-15 清零。

## 10. 验收标准

- [x] `AC-009/AC-010/AC-019`：devtools 可追踪、选择性注册且不复制 system/domain。
- [x] `AC-021`：graph、build 和 E2E 真实通过。
- [x] commit/candidate/result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-13.md</Path>`。

## 11. 完成证据

- source 保留且 clean：`a73007e54dcb517fccf4e5470e679dfee7ac0c00`，tree `37895e4c70901b47dfe40f17c9fe47b5c907687c`。
- attempt 7 完整 non-browser Gate 通过：27-workspace architecture `0 + 92/92`、lint/typecheck、root `48 files / 232 tests`、全部 workspace unit 与双 App production build 均通过。
- Lead candidate targeted Playwright `5/5`、full dual-App Playwright `47/47`；预览进程停止且 `4173/4174` 已释放。
- `plus-ui-namewta main` 仅在完整门禁通过后以 `--ff-only` 推进到 `a73007e54dcb517fccf4e5470e679dfee7ac0c00`；transient candidate worktree/branch 已删除，source worktree/branch 保留。
