---
schema_version: 3
artifact: ticket
change: 2026-08-28-retire-runtime-code-generator
id: T-02
title: 删除前端 Admin 代码生成管理能力
status: done
planning_depth: deep
planning_depth_reason: 物理删除两个公共 workspace 包并同步 Admin 显式组合、全局架构映射与共享 lockfile
ready: true
risk: high
blocked_by: []
contract_ids: [AC-003, AC-004]
owner: codex:/root
expected_changes: ["<Path>plus-ui-namewta/packages/domains/gen/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/gen/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/host/download.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/views/index.vue</Path>", "<Path>plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/e2e/devtools-domain.spec.ts</Path>"]
writable_paths: ["<Path>plus-ui-namewta/packages/domains/gen/**</Path>", "<Path>plus-ui-namewta/packages/web-domains/gen/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/package.json</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/host/download.test.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/views/index.vue</Path>", "<Path>plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/e2e/devtools-domain.spec.ts</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/packages/api-contracts/**</Path>", "<Path>plus-ui-namewta/tooling/generators/**</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/host/download.ts</Path>", "<Path>docs/fm/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/apps/admin-web/package.json</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>", "<Path>plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/apps/admin-web/package.json</Path> => T-02", "<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path> => T-02", "<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path> => T-02", "<Path>plus-ui-namewta/tooling/architecture/test/domain-layout.test.mjs</Path> => T-02", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-02"]
---

# Ticket T-02: 删除前端 Admin 代码生成管理能力

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/02-remove-admin-generator-ui.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-02.md</Path>`

## 1. 战略与来源

- **目标：** 从前端 workspace 与 Admin 显式组合中完整删除代码生成 Domain/Web Domain 和管理界面。
- **可观察产出：** Admin 不注册生成器 service、manifest 或 component，不显示首页生成器宣传；通用下载仍正常。
- **来源：** `US-002`、`AC-003`、`AC-004`、`ADR-001`、`LOG-002`、当前 package/manifest/test 事实。
- **当前事实：** 两个 gen 包仍存在；Admin package、services 和 manifest 显式选择它们；架构映射、组合测试、下载 fixture、首页文案与 lockfile 仍引用生成器。
- **Planning Depth 原因：** 删除公共 workspace 包并修改全局 App 组合和 lockfile，遗漏会导致动态路由残留或整个 workspace 解析失败。

## 2. 决策状态

### 已锁定决策

- 物理删除 `<Path>plus-ui-namewta/packages/domains/gen/**</Path>` 与 `<Path>plus-ui-namewta/packages/web-domains/gen/**</Path>`，不保留空包、alias 或 stub registration。
- 从 Admin 显式组合和依赖中删除 `gen`；不修改 manifest-only 失败关闭合同。
- 通用下载宿主保留，仅把其测试中的 `/tool/gen` fixture 替换为中性下载资源。

### 已采用的低影响假设

- pnpm workspace 通过目录发现包；删除目录并刷新 lockfile 即可收缩包图，无需编辑不存在的显式 workspace 列表。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 两个 gen 包、Admin service/manifest/dependency、组合测试、首页文案、lockfile 与架构映射 | 动态菜单 manifest-only 诊断、通用下载、HTTP、权限宿主 | OpenAPI 当前合同、数据库菜单、后端模块、`tooling/generators` README-only 占位 |

## 4. 要构建什么

Admin 解析动态菜单和启动应用时，不再有 `gen` domain、service 或 component registration；目标菜单数据删除后导航无代码生成入口，首页也不再宣传该能力。若服务端仍意外返回旧 `tool/gen` component key，现有 manifest-only 机制继续失败关闭并诊断，不能通过兼容页面兜底。非生成器下载测试继续证明宿主能力。

## 5. 实现契约

- **入口或接缝：** pnpm workspace/package graph、Admin services、manifest registry、架构测试和首页。
- **输入与输出：** 输入为动态菜单和当前 Admin 组合；输出为无 gen package/registration 的可构建应用。
- **公共接口变化：** 删除 `@namewta/domain-gen`、`@namewta/web-domain-gen` 包及其导出；不新增替代包。
- **不变量：** App 继续显式组合真实 Domain/Web Domain；manifest-only 路由、权限和通用下载语义不变。
- **状态或数据流：** 菜单 component key 只解析到现存 registration；生成器 service 不再进入应用容器。
- **错误与失败行为：** 旧 gen component key 继续由现有未知 key 诊断路径拒绝；缺失依赖由 architecture/typecheck/build 暴露。
- **兼容要求：** 不适用：旧前端和旧菜单不在兼容范围。
- **安全与隐私要求：** 不以 UI 隐藏替代服务端/菜单删除；本 Ticket 不扩大其他权限。

## 6. 执行路线

1. 用现有组合测试和引用扫描冻结 gen package、service、manifest、文案与 fixture 的当前命中。
2. 从 Admin package/services/manifest 中移除 gen 选择，并删除对应正向组合断言。
3. 物理删除两个包，刷新架构映射和 pnpm lockfile，确保没有空 package 占位。
4. 替换通用下载测试中的生成器 URL fixture并删除首页宣传，不改变下载实现。
5. 运行 architecture、定向 Vitest、lint、typecheck、test 与 build，记录实现和父分支证据。

## 7. 路径访问契约

- **预计修改点：** frontmatter `expected_changes` 所列两个包和 Admin/architecture/lockfile 接缝。
- **可写范围：** 仅 `writable_paths`；OpenAPI 与说明文档分别由 T-04/T-05 处理。
- **只读上下文：** 当前 API contracts、通用下载实现、`tooling/generators` 与 `docs/fm`。
- **共享路径：** Admin package/services/manifest、architecture mapping 和 lockfile 由 T-02 唯一修改。
- **保留或不动：** `<Path>plus-ui-namewta/tooling/generators/**</Path>`、非 gen Domain/Web Domain 与通用下载实现。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | workspace/App 组合 | 在 `<Path>plus-ui-namewta/**</Path>` 运行 `pnpm architecture:check`、`pnpm architecture:test`、定向 Admin Vitest | 包图和组合测试通过且无 gen registration | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-02.md</Path>` |
| 失败路径 | 未知 component 与引用扫描 | 保留现有未知 key 断言；扫描 package、service、manifest、lockfile 和文案 | 未知 key 仍失败关闭，生成器活动引用为零 | 同上 |
| 回归 | 全前端门禁 | 运行 `pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build:prod` | 全部通过；通用下载定向测试通过 | 同上 |

- **Workspace checks：** 按 Goal Plan 在 current workspace 或 source worktree 运行上述 architecture、测试、lint、类型和构建门禁。
- **E2E disposition：** not-required：删除行为由 App 组合、静态文案、架构测试和构建证明；若实现新增可见导航行为则登记偏差并提升定向 Admin smoke。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；默认无独立 E2E。
- **Integration evidence：** 非空 implementation/source commit、parent before、适用 candidate/result SHA、验证命令与父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 同一实现提交内先收缩消费者和测试基线，再删除包并刷新 lockfile；不得发布中间空包。
- **兼容窗口：** 不适用：旧 App/package consumer 不保留。
- **监控信号：** 不适用：无生产窗口；workspace 与 manifest 门禁承担验收。
- **回滚或前向恢复：** 实施提交前可恢复本地删除；集成后修正遗漏消费者，不恢复 gen alias/stub。
- **不可逆操作与批准点：** 代码删除由 Git 可追溯；implementation commit/integration 需 Goal Plan/I 授权。
- **收缩条件：** workspace、lockfile、Admin 组合、活动测试和首页文案中的专属生成器引用为零。

## 10. 验收标准

- [x] `AC-003`：两个 gen 包、依赖、service、manifest、selected id 和 component registration 均不存在，前端门禁通过。
- [x] `AC-004`：Admin 不显示代码生成入口或宣传，其他组合与通用下载行为保持。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-02.md</Path>`。
- [x] 修改未超出 `writable_paths`，shared path 由 T-02 修改。
- [x] 已形成非空 implementation/source commit，direct-parent 或 candidate 验证与父分支 result 已记录。
- [x] E2E disposition 已执行；未发生未批准偏差；Ticket、Map 和 Evidence 状态一致。
