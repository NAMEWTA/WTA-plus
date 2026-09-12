---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-03
title: 激活 pnpm workspace 与架构 Ratchet
status: done
planning_depth: deep
planning_depth_reason: 修改根依赖解析、锁文件和全局质量配置，并建立所有后续包共享的依赖方向公共 Gate
ready: true
risk: high
blocked_by: [T-02]
contract_ids: [AC-001, AC-007, AC-008, AC-025, AC-026, AC-030]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/tsconfig.json</Path>", "<Path>plus-ui-namewta/vite.config.ts</Path>", "<Path>plus-ui-namewta/.oxlintrc.json</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>"]
writable_paths: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/tsconfig.json</Path>", "<Path>plus-ui-namewta/vite.config.ts</Path>", "<Path>plus-ui-namewta/.oxlintrc.json</Path>", "<Path>plus-ui-namewta/tooling/architecture/**</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/src/**</Path>", "<Path>plus-ui-namewta/e2e/**</Path>"]
shared_paths: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-workspace.yaml</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>", "<Path>plus-ui-namewta/tsconfig.json</Path>", "<Path>plus-ui-namewta/vite.config.ts</Path>", "<Path>plus-ui-namewta/.oxlintrc.json</Path>"]
shared_path_owners: ["<Path>plus-ui-namewta/package.json</Path> => T-03", "<Path>plus-ui-namewta/pnpm-workspace.yaml</Path> => T-03", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path> => T-03", "<Path>plus-ui-namewta/tsconfig.json</Path> => T-03", "<Path>plus-ui-namewta/vite.config.ts</Path> => T-03", "<Path>plus-ui-namewta/.oxlintrc.json</Path> => T-03"]
---

# Ticket T-03: 激活 pnpm workspace 与架构 Ratchet

- **Ticket/Map/Spec/Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/03-workspace-and-architecture-gate.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-03.md</Path>`。

## 1. 战略与来源

- **目标：** 在保持现有 admin 根入口工作的前提下，激活轻量 pnpm workspace、catalog/workspace 协议和可增量收紧的依赖边界检查。
- **可观察产出：** pnpm 能识别根与真实激活包，锁文件稳定；新包立即受公开 exports、方向和循环检查约束，存量偏差有基线而非全局忽略。
- **来源：** `US-001`、`US-011`、`US-012`、`AC-001`、`AC-007`、`AC-008`、`AC-025`、`AC-026`、`AC-030`、`ADR-001`。
- **当前事实：** workspace 当前仅根 `.`，根配置和 lockfile 是所有后续迁移共享接缝。
- **Planning Depth 原因：** 根依赖与质量 Gate 影响全部 App/包，错误会阻断安装或放任架构退化。

## 2. 决策状态

### 已锁定决策

- source-first private packages 使用 `workspace:*`；共享版本族使用 pnpm catalog。
- 首期不引入 Nx、Turbo、微前端或公共发布配置。
- 先测量存量偏差；新包和变更边界严格，存量按显式 Ratchet 收敛。

### 已采用的低影响假设

- 架构检查选用与现有 Node/pnpm 版本兼容的轻量工具或仓内脚本，具体实现由执行者基于 lockfile 选择。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| workspace globs、catalog、root scripts、架构规则、存量基线 | 当前 Vite/Vue/TS/Oxlint 配置和根 App | 迁移业务代码、激活未来终端、任务编排平台 |

## 4. 要构建什么

开发者安装和运行根脚本时，当前管理端继续工作；真实包可被 filter；占位不被识别。提交违反依赖方向、deep import 或循环的新边界时 architecture check 失败并给出路径，现有债务只按记录的 baseline 允许且不能增长。

## 5. 实现契约

- **入口或接缝：** 根 `package.json` scripts、pnpm-workspace、tsconfig/Vite/Oxlint、tooling architecture command。
- **输入与输出：** workspace manifests/import graph 输入；包列表、违规诊断和 baseline report 输出。
- **公共接口变化：** 定义 workspace 包发现和 architecture-check 命令合同。
- **不变量：** 根 App 可构建；占位无 package；内部依赖 `workspace:*`；禁止 deep import、反向依赖和循环。
- **状态或数据流：** pnpm discovery -> catalog/lock resolve -> static graph -> baseline/new violations comparison。
- **错误与失败行为：** 新违规、baseline 增长、锁文件非预期漂移均返回非零。
- **兼容要求：** 保留现有根 scripts 名称或提供等价兼容别名，后续 Ticket 不直接写这些 shared paths。
- **安全与隐私要求：** 工具不读取或输出 `.env` secrets。

## 6. 执行路线

1. 从当前安装与质量命令生成可复现 baseline。
2. 激活仅覆盖真实 packages/apps/tooling 的 workspace globs 和 catalog。
3. 将内部引用和共享版本族迁移到 workspace/catalog，并审查 lockfile。
4. 实现 exports、依赖方向、循环与占位误激活检查，记录存量 Ratchet。
5. 接入根 scripts，运行 install/list/lint/typecheck/test/build 和架构失败夹具。
6. 保留兼容入口并形成共享配置 owner commit。

## 7. 路径访问契约

- **预计修改点/可写范围：** frontmatter 所列根配置与 `<Path>plus-ui-namewta/tooling/architecture/**</Path>`。
- **只读上下文：** 当前 src 与 E2E，只用于证明兼容。
- **共享路径：** 六个根配置均由 `T-03` 唯一拥有；后续越界必须在 Goal Plan 记录 deviation。
- **保留或不动：** 业务源码、后端与占位 README 语义。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | pnpm + graph | workspace list、architecture check | 只发现激活包，依赖图合法 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-03.md</Path>` |
| 失败路径 | 规则夹具 | deep import、反向边、循环、baseline 增长夹具 | 各自非零且定位违规 | 同上 |
| 回归 | root gates | `pnpm lint && pnpm typecheck && pnpm test && pnpm build:prod` | 当前 admin 保持绿色，lockfile 仅预期变化 | 同上 |

- **Workspace checks：** source-worktree 或 current-workspace 运行 workspace、architecture、lint、typecheck、unit、build。
- **E2E disposition：** required：根依赖/构建配置变化可能破坏现有管理端加载。
- **E2E owner/environment：** Lead / parent-candidate 或 current-workspace；运行 T-01 管理端基线。
- **Integration evidence：** 记录 source commit、parent before、candidate/result SHA、lockfile diff 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** baseline -> workspace discovery -> catalog/lock -> architecture Ratchet -> 全门禁。
- **兼容窗口：** 根 App 和旧 scripts 持续到 T-15；占位持续不激活。
- **监控信号：** workspace 包数、baseline 违规数、新违规数、lockfile diff、构建结果。
- **回滚或前向恢复：** 回退 workspace/config commit 可恢复单根模式；若仅规则误报，前向修正规则而非扩大 ignore。
- **不可逆操作与批准点：** 无；合并前 Lead 必须审查 lockfile 和 baseline。
- **收缩条件：** T-15 后才可移除根 App 兼容脚本，且双 App build/E2E 通过。

## 10. 验收标准

- [x] `AC-001/AC-025`：真实 workspace、`workspace:*`、catalog 与 lockfile 解析符合合同。
- [x] `AC-007/AC-008`：公开 exports、依赖方向和无环可重复检查。
- [x] `AC-026`：未引入 Nx/Turbo、微前端或公共发布配置。
- [x] `AC-030`：存量 baseline 显式，新包/变更边界立即严格且 baseline 不可增长。
- [x] 验证、shared owner、commit/candidate/result SHA 记录到 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-03.md</Path>`。
