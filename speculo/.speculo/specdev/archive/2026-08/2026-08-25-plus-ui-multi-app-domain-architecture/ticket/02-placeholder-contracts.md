---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-02
title: 建立未来终端与共享层占位合同
status: done
planning_depth: lite
planning_depth_reason: 只创建目录 README 和文档扫描，不激活 package、不改变依赖或运行行为
ready: true
risk: low
blocked_by: [T-01]
contract_ids: [AC-004, AC-005, AC-009]
owner: native:/root/t01
expected_changes: ["<Path>plus-ui-namewta/apps/**/README.md</Path>", "<Path>plus-ui-namewta/packages/**/README.md</Path>", "<Path>plus-ui-namewta/tooling/**/README.md</Path>"]
writable_paths: ["<Path>plus-ui-namewta/apps/**/README.md</Path>", "<Path>plus-ui-namewta/packages/**/README.md</Path>", "<Path>plus-ui-namewta/tooling/**/README.md</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/package.json</Path>", "<Path>plus-ui-namewta/pnpm-lock.yaml</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-02: 建立未来终端与共享层占位合同

- **Ticket/Map/Spec/Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/02-placeholder-contracts.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；`<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-02.md</Path>`。

## 1. 战略与来源

- **目标：** 把完整目标结构和未来 mobile-web、miniapp-taro、Taro adapters 的边界写成可检查占位。
- **可观察产出：** 所有规划目录可浏览且 README 说明八项合同；workspace 与锁文件完全未激活这些目录。
- **来源：** `US-006`、`US-010`、`AC-004`、`AC-005`、`AC-009`、`ADR-002`、`ADR-003`。
- **当前事实：** 当前项目是单根 App，目标 apps/packages/tooling 结构尚不存在。
- **Planning Depth 原因：** 文档和目录创建可逆、无公共运行合同。

## 2. 决策状态

### 已锁定决策

- 每个空占位只含 README/文档资产，不含 `package.json`、依赖、构建声明或空源码伪实现。
- README 必含状态、职责、非职责、允许/禁止依赖、公开入口、后端来源、激活条件和验证。

### 已采用的低影响假设

- 相同层级使用统一 README 模板，但职责和 backendModules 必须具体化。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| apps/packages/tooling 目标目录和 README | Spec 目录蓝图和领域清单 | package.json、源码、移动端/小程序实现、依赖安装 |

## 4. 要构建什么

维护者浏览目标目录即可知道哪个模块未激活、未来负责什么、能依赖什么以及何时可激活；自动扫描证明占位没有被 pnpm 当作包，也没有通过空实现制造虚假完成度。

## 5. 实现契约

- **入口/输入输出：** Spec 蓝图输入；README 与文件清单输出。
- **公共接口变化：** 无；公开入口只是未来合同文字。
- **不变量：** 无 package manifest、无运行依赖、无构建声明。
- **错误行为：** 缺字段或出现 package.json 时文档检查失败。
- **兼容/安全：** 根 App 与锁文件不变；无敏感数据。

## 6. 执行路线

1. 按 Spec 蓝图创建 apps、platform、domains、web-domains、web-kit、adapters、api-contracts、tooling README。
2. 为七个领域写唯一职责和 backendModules 追踪。
3. 扫描占位文件、package.json 和 workspace 列表并记录结果。

## 7. 路径访问契约

- **预计修改点/可写范围：** 仅新目标目录中的 README。
- **只读上下文：** 根 manifest 与 lockfile。
- **共享路径：** 无。
- **保留或不动：** 所有现有源码、package manifest、依赖配置。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | README 扫描 | `rg` 检查八项合同 | 所有占位合同完整 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-02.md</Path>` |
| 失败路径 | package 扫描 | `find apps packages tooling -name package.json` | 未激活占位无结果 | 同上 |
| 回归 | Git diff/workspace | 检查 package/lock diff 与 workspace list | 依赖和运行行为无变化 | 同上 |

- **Workspace checks：** current-workspace 运行文件扫描和 Markdown review。
- **E2E disposition：** not-required：只创建不参与运行的 README 占位。
- **E2E owner/environment：** Lead / current-workspace；不执行浏览器场景。
- **Integration evidence：** 记录 implementation commit、direct-parent result SHA 和包含关系。

## 9. 发布、迁移与恢复

- **迁移/兼容/监控/收缩：** 不适用：占位不进入运行时。
- **回滚或前向恢复：** 回退纯文档 commit。
- **不可逆操作与批准点：** 无。

## 10. 验收标准

- [x] `AC-004`：mobile-web、miniapp-taro、Taro adapters 仅含边界文档且未激活。
- [x] `AC-005`：任一占位 README 均含八项合同。
- [x] `AC-009`：七个领域具有唯一职责和 backendModules 追踪。
- [x] 验证矩阵、路径边界、commit 与父分支 result 记录在 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-02.md</Path>`。
