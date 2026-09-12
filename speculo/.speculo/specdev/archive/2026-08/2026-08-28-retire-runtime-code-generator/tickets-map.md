---
schema_version: 3
artifact: tickets-map
change: 2026-08-28-retire-runtime-code-generator
status: completed
---

# Tickets Map: 完整退役运行时代码生成器

- **Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/</Path>`
- **建议 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/goal-plan.md</Path>`

## 1. 目标与拆分策略

五张 Ticket 共同交付 `US-001` 至 `US-005` 与 `AC-001` 至 `AC-012`：后端、前端和数据库分别形成独立可验证的硬退役切片；当前 OpenAPI 真实依赖后端 endpoint 删除；当前事实与最终门禁在所有实现汇合后执行。没有必要 prefactor，也不使用 expand-contract，因为用户明确选择无兼容窗口的基座硬删除。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | Candidate Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/01-remove-backend-runtime-generator.md</Path>` | 后端构建图和运行源码无 `ruoyi-gen`/`/tool/gen` | — | deep | high | yes | codex:/root | AC-001, AC-002 | Wave 1 / G1 / seq 1 | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/02-remove-admin-generator-ui.md</Path>` | 前端包图和 Admin 组合无生成器 | — | deep | high | yes | codex:/root | AC-003, AC-004 | Wave 1 / G2 / seq 2 | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/03-hard-retire-generator-database.md</Path>` | MySQL 最终状态无生成器表、菜单和授权 | — | deep | critical | yes | codex:/root | AC-005, AC-006, AC-007 | Wave 1 / G3 / seq 3 | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/04-refresh-current-openapi-contract.md</Path>` | 当前 OpenAPI/TS 合同无生成器 | T-01 | deep | high | yes | codex:/root | AC-002, AC-008 | Wave 2 / G4 / seq 4 | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/05-align-current-facts-and-acceptance.md</Path>` | 当前事实一致且完整集成门禁通过 | T-01, T-02, T-03, T-04 | standard | medium | yes | codex:/root | AC-009, AC-010, AC-011, AC-012 | Wave 3 / G5 / seq 5 | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表是同步投影。

## 3. 依赖 DAG

```text
T-01 [DONE] ───→ T-04 [DONE] ──┐
  └───────────────────────────────┤
T-02 [DONE] ──────────────────────┼──→ T-05 [DONE / G5 CLOSED]
T-03 [DONE / DB E2E] ─────────────┘
```

- 根 Ticket：T-01、T-02、T-03。
- 真实前置：T-04 必须引用 T-01 已删除 endpoint 的后端 commit。
- 汇合点：T-05 只在四张前置票的父分支结果和 Evidence 完成后开始。
- DAG 无 expand、migrate batches 或 contract 兼容阶段；本 change 本身就是获批 hard contract。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01 | POM 扫描、Maven test 与双 bundle build | covered | 后端构建图完整收缩 |
| AC-002 | T-01, T-04 | Java/配置扫描、当前 OpenAPI | covered | 源码和 wire contract 双重证明 |
| AC-003 | T-02 | workspace、architecture、Admin 组合测试 | covered | 包、service、manifest、lockfile 同步删除 |
| AC-004 | T-02 | 组合测试、静态文案和回归构建 | covered | 不新增 UI 行为时不要求浏览器 E2E |
| AC-005 | T-03 | SQL 顺序 review、MySQL 最终查询 | covered | 九菜单、角色关系和空目录删除 |
| AC-006 | T-03 | DDL 顺序 review、information_schema | covered | 两表永久删除 |
| AC-007 | T-03 | Docker MySQL 8.4 fresh init | covered | Lead E2E required |
| AC-008 | T-04 | fetch/generate/check 与残留扫描 | covered | 新 revision、current 和 TS 同步 |
| AC-009 | T-05 | scoped residual allowlist | covered | OOS 命中逐项说明 |
| AC-010 | T-05 | `docs/fm` validator | covered | 静态模板本体保留 |
| AC-011 | T-05 | 当前事实扫描和 diff review | covered | Skills/README 与源码一致 |
| AC-012 | T-05 | Maven/pnpm/OpenAPI 全门禁 + T-03 Evidence | covered | 未运行项不得冒充通过 |

## 5. 并行与路径所有权

- implementation subagent 上限来自 `<Path>{roots.state}/specdev/config.json</Path>`，当前为 3；是否使用 worktree/并行由后续 Goal Plan 与用户决定。
- current 模式必须在当前 workspace 严格串行；required 模式才可把候选 Wave 1 投影为独立 Ticket worktrees。
- Lead 始终拥有 SpecDev 状态、Evidence 和 direct-parent/parent-candidate integration。
- T-01 唯一拥有后端全局 POM、Springdoc 与 bundle 校验脚本/说明；T-02 唯一拥有 Admin package/组合、architecture mapping、pnpm lockfile 与生成器专用 E2E；T-03 唯一拥有 NAMEWTA DDL/DML；T-04 唯一拥有当前 OpenAPI；T-05 唯一拥有列出的当前事实文档与恢复模板。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | required 模式可并行；current 模式串行 |
| T-01 | T-03 | 无 | 否 | required 模式可并行；同属后端仓库但路径独占 |
| T-02 | T-03 | 无 | 否 | required 模式可并行 |
| T-01 | T-04 | 无 | 是 | T-04 等待 T-01 backend commit，不因路径而阻塞 |
| T-01..T-04 | T-05 | 无 | 是 | T-05 是最终事实和验收 Gate，不越界修补前置路径 |

## 6. Gate、Wave 与集成点

- **Wave 1：** T-01、T-02、T-03 是 DAG 并行候选，但 Goal Plan 已选择 current 策略，固定按 T-01 → T-02 → T-03 严格串行。
- **Wave 2：** T-04 在 T-01 后端结果上获取合同，并排在 T-02 后以先删除消费者、后收缩生成类型。
- **Wave 3：** T-05 在全部前置 result 与 Evidence 完成后执行。
- **DB E2E：** T-03 由 Lead 在 current workspace 或 parent-candidate 的可丢弃 MySQL 8.4 环境执行。
- **Final Gate：** T-05 汇合全部父分支结果，运行当前事实、模板、残留、后端、前端和 OpenAPI 门禁。
- Goal Plan 已固定 `current workspace + direct-parent`、唯一 Lead `codex:/root`、单 implementation writer 和 G0 至 G5 Gate；权威编排位于 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/goal-plan.md</Path>`。

## 7. 横切契约与风险

- `docs/fm` 与运行时代码生成器是不同 bounded context；所有 Ticket 都不得删除模板本体。
- 冻结 SQL和不可变 OpenAPI 历史 revisions 只读；当前合同和 NAMEWTA append-only 增量分别由 T-04/T-03 收缩。
- 不兼容、不备份、不生产迁移、不滚动发布、不恢复两张生成器表；实际破坏性 SQL 只在可丢弃环境验证。
- 活动 endpoint、UI、菜单、权限、当前合同和 schema 必须共同消失；任一层残留都阻塞 Final Gate。
- 现有未提交改动属于用户：T-01 保留模块模板删除，T-05 在当前 Skills/`docs/fm` 改动上最小修正，其他 Ticket 不回退无关内容。

## 8. 同步规则

- Ticket 状态变化后同步执行清单；frontmatter 是单 Ticket 权威。
- Goal Plan 存在后，Wave、Gate、workspace 策略与 owner 以其为编排权威并投影回本 Map。
- 依赖、合同覆盖或路径所有权变化后重新运行 `<Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path>`。
- 发现路径、契约、迁移或验收事实变化时按偏差控制停止，不静默扩写 Ticket。
- 所有 Evidence 写入 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/</Path>`；不得在本 Map 声称尚未执行的验证通过。
