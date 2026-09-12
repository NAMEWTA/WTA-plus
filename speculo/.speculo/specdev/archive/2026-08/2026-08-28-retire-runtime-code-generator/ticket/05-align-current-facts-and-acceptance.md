---
schema_version: 3
artifact: ticket
change: 2026-08-28-retire-runtime-code-generator
id: T-05
title: 对齐当前事实并完成集成验收
status: done
planning_depth: standard
planning_depth_reason: 在四个实现切片汇合后同步跨仓库工程事实并运行完整合同覆盖门禁，但不再改变公共接口或数据
ready: true
risk: medium
blocked_by: [T-01, T-02, T-03, T-04]
contract_ids: [AC-009, AC-010, AC-011, AC-012]
owner: codex:/root
expected_changes: ["<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/review-checklist.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/architecture.md</Path>", "<Path>.agents/skills/ruoyi-common-modules-guide/references/module-map.md</Path>", "<Path>.agents/skills/ruoyi-workflow-module-guide/references/capability-map.md</Path>", "<Path>.agents/skills/ruoyi-workflow-module-guide/references/integration-guide.md</Path>", "<Path>docs/fm/README.md</Path>", "<Path>docs/fm/react/api.ts.ftl</Path>", "<Path>docs/fm/react/types.ts.ftl</Path>", "<Path>docs/fm/react/index.tsx.ftl</Path>", "<Path>docs/fm/react/index-tree.tsx.ftl</Path>", "<Path>docs/fm/sql/oracle.sql.ftl</Path>", "<Path>docs/fm/sql/postgres.sql.ftl</Path>", "<Path>docs/fm/sql/sqlserver.sql.ftl</Path>", "<Path>plus-ui-namewta/apps/admin-web/README.md</Path>"]
writable_paths: ["<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/review-checklist.md</Path>", "<Path>.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md</Path>", "<Path>.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/architecture.md</Path>", "<Path>.agents/skills/ruoyi-common-modules-guide/references/module-map.md</Path>", "<Path>.agents/skills/ruoyi-workflow-module-guide/references/capability-map.md</Path>", "<Path>.agents/skills/ruoyi-workflow-module-guide/references/integration-guide.md</Path>", "<Path>docs/fm/README.md</Path>", "<Path>docs/fm/react/api.ts.ftl</Path>", "<Path>docs/fm/react/types.ts.ftl</Path>", "<Path>docs/fm/react/index.tsx.ftl</Path>", "<Path>docs/fm/react/index-tree.tsx.ftl</Path>", "<Path>docs/fm/sql/oracle.sql.ftl</Path>", "<Path>docs/fm/sql/postgres.sql.ftl</Path>", "<Path>docs/fm/sql/sqlserver.sql.ftl</Path>", "<Path>plus-ui-namewta/apps/admin-web/README.md</Path>"]
read_only_paths: ["<Path>docs/fm/catalog.json</Path>", "<Path>docs/fm/context-contract.md</Path>", "<Path>docs/fm/scripts/validate.mjs</Path>", "<Path>docs/fm/java/**</Path>", "<Path>docs/fm/vue/**</Path>", "<Path>docs/fm/xml/**</Path>", "<Path>docs/fm/sql/mysql.sql.ftl</Path>", "<Path>ruoyi-vue-plus-namewta/**</Path>", "<Path>plus-ui-namewta/packages/api-contracts/**</Path>"]
shared_paths: ["<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path>", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>", "<Path>.agents/skills/namewta-fullstack-development/references/backend/architecture.md</Path>", "<Path>docs/fm/README.md</Path>", "<Path>plus-ui-namewta/apps/admin-web/README.md</Path>"]
shared_path_owners: ["<Path>.agents/skills/engineering-standards/references/project/00-project-profile.md</Path> => T-05", "<Path>.agents/skills/engineering-standards/references/project/01-module-map.md</Path> => T-05", "<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path> => T-05", "<Path>.agents/skills/namewta-fullstack-development/references/backend/architecture.md</Path> => T-05", "<Path>docs/fm/README.md</Path> => T-05", "<Path>plus-ui-namewta/apps/admin-web/README.md</Path> => T-05"]
---

# Ticket T-05: 对齐当前事实并完成集成验收

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/ticket/05-align-current-facts-and-acceptance.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 在后端、前端、数据库和当前合同均收缩后，使当前工程导航只描述真实基座，并以完整门禁证明纵向退役无意外残留。
- **可观察产出：** Skills/README 不再把运行生成器描述为当前或待退役能力；`docs/fm` 仍是完整可校验的静态 CRUD 标准资产；全套适用门禁通过。
- **来源：** `US-001` 至 `US-005`、`AC-009` 至 `AC-012`、`ADR-001`、`DEC-003`、`NFR-005`。
- **初始事实：** 多个父级 Skill、模块地图、workflow/common 能力地图、`docs/fm/README.md` 和 Admin README 曾提到现存/待退役 `ruoyi-gen`；现已在保留用户在途内容的前提下完成最小修正。
- **Planning Depth 原因：** 本 Ticket 是四个切片的集成汇合和事实校准，修改范围是文档但验证横跨后端、前端、OpenAPI、SQL 和模板。

## 2. 决策状态

### 已锁定决策

- 当前事实应写成“运行时代码生成器已删除；`docs/fm` 是静态模板资产”，不得保留“待退役”措辞。
- 不删除或重写已存在的 `docs/fm` 模板、catalog、上下文合同和校验脚本；允许恢复 catalog 已声明但迁移遗漏的模板，并仅按现有 validator/Java HTTP 合同修正恢复模板。
- T-05 不修补 T-01 至 T-04 的产品路径；最终门禁发现问题时退回唯一 owner 或登记偏差。
- 永久 SpecDev ADR/context 只读；知识毕业由后续 Archive Work 决定。

### 已采用的低影响假设

- 当前 `rg` 命中的事实文档集合是最小充分写范围；实施时可读扫描新命中，但新增可写文件前必须按偏差控制扩展 Ticket。
- 实施期运行 validator 发现 React 与三种 SQL 方言模板迁移不完整；缺失文件从 T-01 删除前提交逐字恢复，React 仅修正 POST/path 与资源字段谓词，不设计新模板能力。

### 未决问题

无。

## 3. 范围边界

| IN（本 Ticket 构建） | REUSE（复用且不改变契约） | OUT（明确不做） |
|---|---|---|
| 当前 Skills/模块地图/README 生命周期措辞、`docs/fm` 校验、全局残留与质量门禁 | T-01 至 T-04 已集成结果、现有 validator/Maven/pnpm/OpenAPI/MySQL Evidence | 修改产品代码/SQL/合同、模板行为、历史 revisions、永久 ADR/context、归档 |

## 4. 要构建什么

新维护者阅读工程 Skill、模块地图和 Admin/模板说明时，只看到一个一致事实：产品运行时没有代码生成器，CRUD 开发使用父仓库静态模板与工程规范。随后同一已集成父分支运行残留扫描和全套质量门禁，证明后端、前端、当前合同、数据库 Evidence 与模板资产一致。任何产品残留都必须回到对应 Ticket owner 处理，不能由文档票越界掩盖。

## 5. 实现契约

- **入口或接缝：** 当前工程 Skills、模块/能力地图、Admin README、`docs/fm` README/validator 和四个 Ticket Evidence。
- **输入与输出：** 输入为 T-01 至 T-04 已验证父分支结果；输出为准确当前事实与最终验收 Evidence。
- **公共接口变化：** 无；只描述 T-01/T-04 已完成的接口删除。
- **不变量：** 保留用户在途内容；不改模板行为、不改冻结历史、不在文档中虚构未运行结果。
- **状态或数据流：** 四个实现 Evidence -> 当前事实修正 -> scoped residual allowlist -> 全门禁 -> T-05 Evidence。
- **错误与失败行为：** 门禁失败即阻塞；按路径 owner 返回 T-01/T-02/T-03/T-04 或登记正式偏差，不把未运行项标为通过。
- **兼容要求：** 不适用：事实文档描述目标基座，不维护旧运行时说明。
- **安全与隐私要求：** Evidence 不写凭据；确认服务端 endpoint、菜单权限和角色关系均已删除。

## 6. 执行路线

1. 核对 T-01 至 T-04 的父分支结果、Evidence 和所有权，没有全部完成则不开始。
2. 在列出的当前事实文档中最小修正现存/待退役生成器描述，同时保留 `docs/fm` 静态资产和无关通用 generate 语义。
3. 运行 `docs/fm` validator、当前事实扫描和有范围全仓残留扫描，对每个允许命中记录 OOS 理由。
4. 在同一集成结果上运行后端 Maven、前端 pnpm 和 OpenAPI 完整门禁；引用 T-03 已完成的 MySQL E2E Evidence。
5. 记录文档 implementation/source commit、父分支 result、全套结果和残余风险；失败项返回唯一 owner。

## 7. 路径访问契约

- **预计修改点：** frontmatter 列出的当前事实文档与 catalog 已声明但迁移遗漏的模板。
- **可写范围：** 仅 `writable_paths`；产品实现路径全部只读。
- **只读上下文：** `docs/fm` 模板合同、完整后端、API contracts 和四个 Ticket Evidence。
- **共享路径：** 关键工程事实、模板 README 和 Admin README 由 T-05 唯一修改。
- **保留或不动：** 现有用户改动、`docs/fm` 模板本体、历史 OpenAPI revisions、冻结 SQL、永久 SpecDev namespaces。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 模板与当前事实 | 运行 `node docs/fm/scripts/validate.mjs`；扫描列出的 Skills/README | validator 通过；无“现存/待退役运行生成器”陈述 | `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-05.md</Path>` |
| 失败路径 | scoped residual allowlist | 扫描活动 POM/源码/包/manifest/current OpenAPI/NAMEWTA SQL/当前文档，并逐项对照 Spec OOS | 只命中冻结 SQL、历史 revision、`docs/fm` 说明和无关 generate 语义；意外命中阻塞 | 同上 |
| 回归 | 完整集成门禁 | 后端运行 `./mvnw test`、默认/full 与 core package；前端运行 architecture、lint、typecheck、test、build；运行 OpenAPI check | 全部适用命令成功，未执行项明确记录 | 同上 |

- **Workspace checks：** 按 Goal Plan 只在已汇合的 current workspace 或 parent-candidate 运行文档扫描、模板校验与全套非 E2E 门禁。
- **E2E disposition：** not-required：T-05 不新增跨边界行为；数据库跨边界 E2E 由 T-03 Lead Evidence 提供并在此复核。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate；引用 T-03 的 MySQL 8.4 E2E，不重复冒充执行。
- **Integration evidence：** T-01 至 T-04 result SHA、T-05 非空 implementation/source commit、parent before、适用 candidate/result SHA、全门禁结果和父分支包含关系。

## 9. 发布、迁移与恢复

- **迁移顺序：** 只在四张前置票完成后更新事实并运行最终 Gate。
- **兼容窗口：** 不适用：文档只描述当前基座。
- **监控信号：** 不适用：无生产发布；validator、扫描和构建是验收信号。
- **回滚或前向恢复：** 文档错误在同一 owner 范围修正；产品失败返回原 Ticket owner，禁止 T-05 越界修复。
- **不可逆操作与批准点：** 无新的不可逆操作；T-03 的永久删除批准与 Evidence 必须存在。
- **收缩条件：** 全部 12 条 AC 有完成 Evidence，活动残留为零或逐项命中 Spec OOS allowlist。

## 10. 验收标准

- [x] `AC-009`：有范围残留扫描无意外命中，允许项均有 OOS 理由。
- [x] `AC-010`：`node docs/fm/scripts/validate.mjs` 通过；恢复模板的来源、最小合同修正和哈希已记录。
- [x] `AC-011`：当前 Skills、模块地图和 README 准确描述硬退役后的基座。
- [x] `AC-012`：后端、前端、OpenAPI 全部适用门禁通过，T-03 MySQL E2E Evidence 已复核。
- [x] 验证矩阵全部记录到 `<Path>{roots.state}/specdev/changes/2026-08-28-retire-runtime-code-generator/evidence/T-05.md</Path>`。
- [x] T-05 修改位于 `writable_paths`；Final Gate 漏项已返回 T-01/T-02 并扩展原 owner 路径合同，现有用户改动已保留。
- [x] 已形成非空 implementation/source commit，父分支验证/result 和包含关系已记录。
- [x] E2E disposition 已执行；所有偏差已记录并收敛；Ticket、Map 和 Evidence 状态一致。
