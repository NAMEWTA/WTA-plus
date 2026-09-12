---
schema_version: 3
artifact: ticket
change: 2026-08-25-plus-ui-multi-app-domain-architecture
id: T-17
title: 固化上游能力映射与本地架构维护合同
status: done
planning_depth: standard
planning_depth_reason: 建立跨七领域和平台层的长期上游吸收决策记录及验证模板，但不执行具体上游合并
ready: true
risk: medium
blocked_by: [T-16]
contract_ids: [AC-029]
owner: codex:/root
expected_changes: ["<Path>docs/upstream/customization-map.md</Path>", "<Path>docs/upstream/README.md</Path>", "<Path>plus-ui-namewta/README.md</Path>"]
writable_paths: ["<Path>docs/upstream/customization-map.md</Path>", "<Path>docs/upstream/README.md</Path>", "<Path>plus-ui-namewta/README.md</Path>"]
read_only_paths: ["<Path>plus-ui-namewta/apps/**</Path>", "<Path>plus-ui-namewta/packages/**</Path>", "<Path>speculo/.speculo/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/**</Path>"]
shared_paths: []
shared_path_owners: []
---

# Ticket T-17: 固化上游能力映射与本地架构维护合同

- **工件：** Ticket `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/ticket/17-upstream-capability-map.md</Path>`；Map `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/tickets-map.md</Path>`；Spec `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/spec.md</Path>`；Evidence `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-17.md</Path>`。

## 1. 战略与来源

- **目标/产出：** 建立可重复 customization map，使每次上游评估记录能力、目标本地边界、保留不变量、选择结果和真实验证。
- **来源：** `US-009`、`AC-029`、`ADR-007`、`USER-DECISION`。
- **当前事实：** 用户已决定产品 main 以本地架构为权威，上游只用于发现新增/优化能力，不再追求文件路径同构。
- **Planning Depth 原因：** 文档影响长期维护与安全补丁路由，需覆盖所有边界和决策状态但不改运行代码。

## 2. 决策状态

### 已锁定决策

- 上游项按 capability 映射到 app/domain/web-domain/platform/adapter/tooling，而非按旧 src 路径搬运。
- 每项记录 adopt/adapt/reject/defer、原因、保留不变量、目标 owner、验证和关联 issue/change。
- 安全修复优先评估但仍必须通过本地 Client/auth/architecture contracts。
- `DEV-T17-001`：目标 customization map 在执行前已有用户将数据库同步约束从错误的 `DSL.sql` 修正为 `DML.sql`，且旧表按上游路径热点组织，与 T-15/T-16 后的本地架构不一致。T17 可在保留 DDL/DML append-only、Client、认证和权限不变量的前提下，将该文件整体改写为 capability-first 十字段 schema；不得恢复错误术语或把路径同构重新作为完成条件。
- `DEV-T17-002`：首轮标准轴在固定 docs hashes 发现 completed baseline 使用概念路径、decision 流把 reject/defer 错导向 completed、defer 缺少唯一合法终态，且终端索引遗漏 README-only 的 taro-request。T17 只整改治理文档：owner 使用真实 `packages/**`，明确 adopt/adapt -> completed、reject -> rejected、defer -> deferred -> re-triage，并补齐 taro-request 占位；前端 source commit 保持不变，docs 新 hashes 必须重新双轴复审。

### 已采用的低影响假设

- 初始 map 使用当前架构和已知定制点建立 baseline，不需要伪造尚未发生的上游差异。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| customization map schema、初始边界映射、维护流程、README 导航 | ADR、Spec、最终 package exports、现有 upstream sync 能力 | 拉取/合并上游、修改产品代码、恢复路径同构 |

## 4. 要构建什么

维护者评估任一上游能力时，能从单一 map 找到其本地归属和必须保持的认证、Client、manifest、依赖方向与 UI 定制合同；只有记录实际验证后才能标记已吸收，不以文件复制完成为准。

## 5. 实现契约

- **入口/输入输出：** customization map 表格/记录模板；上游 commit/capability 输入，decision/target/verification 输出。
- **公共接口变化：** 无运行接口；新增维护文档合同。
- **不变量/数据流：** upstream observation -> capability classify -> local boundary -> decision -> implementation change -> validation/evidence。
- **失败行为：** 缺来源、目标 owner、不变量或验证的条目不能标 completed。
- **兼容/安全：** 安全修复不得因架构不同被静默遗漏；不记录私有凭据或远端 token。

## 6. 执行路线

1. 从最终 apps/packages/exports 和 ADR 建立本地边界索引。
2. 定义上游记录必填字段、decision 状态和安全优先级。
3. 填写当前定制 baseline，覆盖认证、路由、Client、domains、UI、tooling。
4. 在 workspace/frontend README 链接维护入口和每次更新流程。
5. 用完整/缺字段示例审查可判定性，记录 Gate J。

## 7. 路径访问契约

- **可写：** docs/upstream 两文档和前端 README；**只读：** 最终 apps/packages 与本 change 工件。
- **共享路径：** 无。
- **保留或不动：** 上游 refs、产品代码、package exports 和 SpecDev 历史。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 文档 review | 用一个能力样例走完整映射 | 来源/边界/决定/验证齐全 | `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-17.md</Path>` |
| 失败路径 | schema checklist | 删除 owner/invariant/evidence 字段 | 条目不能标 completed | 同上 |
| 回归 | link/boundary review | 检查 README 链接与七领域归属 | 无路径同构要求，边界全覆盖 | 同上 |

- **Workspace checks：** current-workspace 运行 Markdown/link review 和边界清单审查。
- **E2E disposition：** not-required：只新增维护文档，不改变应用运行行为。
- **E2E owner/environment：** Lead / current-workspace；不执行浏览器场景。
- **Integration evidence：** implementation commit、direct-parent result SHA、Gate J 和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移/兼容/收缩：** 不适用：这是长期维护合同，不迁移运行入口。
- **监控信号：** 未决上游条目数、安全项 age、缺 Evidence 条目数。
- **回滚或前向恢复：** 文档 commit 可回退；字段缺陷前向修订并保留历史。
- **不可逆操作与批准点：** 无。

## 10. 验收标准

- [x] `AC-029`：任一上游能力均记录来源、目标本地边界、保留不变量、决定和实际验证，不以路径同构完成。
- [x] 安全项不会静默 defer，README 可发现维护入口。
- [x] review、commit/direct-parent result SHA 写入 `<Path>{roots.state}/specdev/changes/2026-08-25-plus-ui-multi-app-domain-architecture/evidence/T-17.md</Path>`。
