---
schema_version: 3
artifact: tickets-map
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
status: completed
---

# Tickets Map：发布资产与 MySQL 基座收敛

- **Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **Ticket 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/</Path>`
- **Evidence 目录：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/</Path>`
- **后续 Goal Plan：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/goal-plan.md</Path>`

## 1. 目标与拆分策略

六个 Ticket 共同把父仓库发布目录确立为 SQL 和 Workflow 资产唯一 owner，删除后端旧 `script/`，将项目收缩为 MySQL-only，并以隔离 MySQL 8.4 证明全新初始化和发布门禁成立。

拆分遵循以下原则：

- T-01 先建立唯一 SQL owner 和只读发布接缝，是后续测试迁移与收缩的基础。
- T-02 以 expand 方式先接管六个 Workflow JSON，可与 T-01 并行准备。
- T-03 迁移 SQL 测试消费者，防止删除旧目录后出现静默少测。
- T-04 是 contract 收缩点，只有三个前置 Ticket 都形成父分支证据后才能删除整个旧目录。
- T-05 同步当前治理和活动计划，由 Lead 独占 SpecDev 状态路径，避免和其他运行中 Work 冲突。
- T-06 是最终集成与 required E2E Gate，不代替前置 Ticket 的单独实现和证据。

当前拆分包含多个 Deep Ticket、共享路径、跨 Git 仓库集成、目录删除和真实 MySQL E2E，因此用户确认后必须进入 `<Path>{roots.workflows}/specdev/P-goal-plan/P-goal-plan.md</Path>`，不直接进入实现。

## 2. 执行清单

| ID | Ticket | 可观察产出 | Blocked By | Depth | Risk | Ready | Owner | Contract IDs | 候选 Wave/Gate | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| T-01 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/01-establish-canonical-mysql-baseline.md</Path>` | 六份 SQL 被跟踪，`stage-mysql` 只验证且零写入 | — | deep | high | yes | codex:/root | AC-001、AC-002、AC-003、AC-007、AC-009 | Wave 1 / 基座 Gate | done |
| T-02 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/02-version-workflow-json-assets.md</Path>` | 六个 Workflow JSON 在发布目录等价、可解析、被跟踪 | — | standard | medium | yes | codex:/root | AC-005 | Wave 1 / 资产 Gate | done |
| T-03 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/03-migrate-sql-test-consumers.md</Path>` | 父级和 Java 测试只读取唯一 SQL 基座且不静默跳过 | T-01 | deep | high | yes | codex:/root | AC-010 | Wave 2 / 消费者 Gate | done |
| T-04 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/04-retire-backend-script-tree.md</Path>` | 后端 `script/` 整体删除，三种非 MySQL 方言零迁移 | T-01、T-02、T-03 | deep | high | yes | codex:/root | AC-004、AC-005、AC-006 | Wave 3 / 收缩 Gate | done |
| T-05 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/05-align-governance-and-active-work.md</Path>` | 当前规范、Skill 和活动实施工件统一新 owner 与 MySQL-only | T-04 | deep | high | yes | codex:/root | AC-005、AC-006、AC-011、AC-013、AC-014 | Wave 4 / 治理 Gate | done |
| T-06 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/06-prove-release-and-mysql-gates.md</Path>` | 聚合回归与隔离 MySQL 8.4 fresh 初始化通过 | T-04、T-05 | deep | high | yes | codex:/root | AC-001 至 AC-014 | Wave 5 / 最终 E2E Gate | done |

Ticket frontmatter 是状态、依赖、深度和路径访问契约的权威；本表只是同步投影。

## 3. 依赖 DAG

```text
T-01 [基座 EXPAND]
  ├─→ T-03 [测试消费者迁移]
  └───────────────┐
                  │
T-02 [Workflow EXPAND]
  └───────────────┼─→ T-04 [旧 script CONTRACT]
T-03 ─────────────┘       │
                           └─→ T-05 [治理与活动计划]
                                  │
                                  └─→ T-06 [最终发布与 MySQL E2E]
```

- **根 Ticket：** T-01、T-02。
- **关键汇合点：** T-04；它要求 SQL owner、Workflow owner 和测试消费者三条路径全部完成。
- **最终集成点：** T-06；它只在收缩和治理都完成后运行。
- **真实阻塞说明：** T-03 必须先有可跟踪的新 SQL；T-04 必须先证明保留资产和消费者已迁移；T-05 必须基于最终目录事实；T-06 必须验证最终组合状态。

## 4. 合同覆盖矩阵

| Contract ID | 覆盖 Ticket | 验证接缝 | 状态 | 说明 |
|---|---|---|---|---|
| AC-001 | T-01、T-06 | Git 跟踪与聚合门禁 | covered | 六份 SQL 全部被跟踪 |
| AC-002 | T-01、T-06 | 忽略正反矩阵 | covered | SQL 不忽略，secret/生成物仍忽略 |
| AC-003 | T-01、T-06 | `stage-mysql` 前后摘要 | covered | CLI 只验证且零写入 |
| AC-004 | T-04、T-06 | 目录存在性和活动扫描 | covered | 后端 `script/` 不存在 |
| AC-005 | T-02、T-04、T-05、T-06 | Workflow 资产、删除和当前引用 | covered | 保留资产迁移后旧目录删除 |
| AC-006 | T-04、T-05、T-06 | 非 MySQL 路径和当前支持声明 | covered | 三种方言直接退出 |
| AC-007 | T-01、T-06 | 六文件清单和摘要 | covered | 唯一 SQL 基座完整 |
| AC-008 | T-06 | 隔离 MySQL 8.4 E2E | covered | 固定顺序 fresh 初始化 |
| AC-009 | T-01、T-06 | Shell/Node 失败合同 | covered | 缺文件明确失败且不自修复 |
| AC-010 | T-03、T-06 | Node/JUnit/Maven | covered | SQL 测试消费唯一基座 |
| AC-011 | T-05、T-06 | 当前/历史分类扫描 | covered | 当前权威无旧 owner，历史保留 |
| AC-012 | T-06 | fresh 初始化结果查询 | covered | 关键结构、数据与元数据成立 |
| AC-013 | T-05、T-06 | 部署治理与升级门禁检查 | covered | 已有库不重放基座，Tag 差异受控 |
| AC-014 | T-05、T-06 | Skill/文档/活动计划校验 | covered | 长期规范与实现事实一致 |

无 `uncovered` 或 `deferred` 合同。

## 5. 并行与路径所有权

- implementation subagent 上限取 `<Path>{roots.state}/specdev/config.json</Path>`，当前为 3；是否实际并行由 Goal Plan 决定。
- Lead 固定拥有 SpecDev 状态、Evidence、父分支集成和 T-06 E2E。
- T-01 与 T-02 项目可写路径无交集，可作为候选并行组；current workspace 策略下仍必须串行。
- T-03、T-04、T-05、T-06 按 DAG 串行，不以人工交接替代依赖证据。
- T-05 触达其他活动 change，实施前必须重读其 `current_work` 并协调唯一 owner；不得覆盖并发修改。

| Ticket A | Ticket B | Writable 交集 | 真实依赖 | 处理 |
|---|---|---|---|---|
| T-01 | T-02 | 无 | 否 | required 策略下可并行；current 策略串行 |
| T-01 | T-03 | 无，但 T-03 消费 T-01 SQL | 是 | T-03 等待 T-01 集成 |
| T-02 | T-03 | 无 | 否 | 可并行，但 T-04 等待两者 |
| T-03 | T-04 | 无；T-04 删除 T-03 的旧输入 | 是 | 完成旧引用扫描后才能收缩 |
| T-04 | T-05 | 无；文档必须反映最终目录 | 是 | T-05 以后置事实更新当前权威 |
| T-05 | T-06 | 无；T-06 验证最终治理状态 | 是 | T-06 最终汇合 |

共享 owner：

- 六份 MySQL SQL、发布脚本：T-01。
- 六个 Workflow JSON：T-02。
- 后端 SQL 相关测试树：T-03。
- 后端旧 `script/`：T-04。
- 项目 Skill 和两个活动 change：T-05，仅 Lead。
- 父级 CI 与外部服务入口：T-06，仅 Lead 集成。

## 6. Gate、Wave 与集成点

| 候选 Wave | Ticket | Gate | 通过条件 |
|---|---|---|---|
| Wave 1 | T-01、T-02 | 资产接管 | SQL 跟踪且 stage 零写入；Workflow 六文件等价 |
| Wave 2 | T-03 | 消费者迁移 | 活动测试无旧 SQL 路径，required 输入不静默跳过 |
| Wave 3 | T-04 | 收缩 | 三个前置提交已集成；旧 `script/` 删除且新资产不变 |
| Wave 4 | T-05 | 治理 | 当前规范和活动计划只使用新 owner；历史原文保留 |
| Wave 5 | T-06 | 最终 E2E | 全部回归和隔离 MySQL 8.4 fresh 初始化通过 |

正式 workspace 策略、owner、候选合并顺序、用户批准点和恢复流程由 Goal Plan 锁定。由于存在 Deep Ticket 与跨仓库收缩，本 Map 不授权直接实施。

## 7. 横切契约与风险

- **数据完整性：** 六组旧/新 SQL 在 T-01 开始前必须逐文件摘要一致；不一致属于 Spec 级偏差。
- **安全：** 解除忽略必须是白名单式；真实 `.env`、证书、私钥、日志、运行数据和构建物不得进入 Git。
- **兼容：** `stage-mysql` 名称保留，复制语义退出；旧 `script/` 删除后不提供符号链接或副本。
- **数据库：** 只支持 MySQL 8.4；fresh 使用完整基座，已有库使用 Git Tag 差异、备份与隔离演练。
- **历史：** 当前权威和活动实施工件更新；completed、archived 和历史解释保留原文。
- **真实服务：** E2E 只允许本次创建的隔离 MySQL，失败清理不得触达开发或生产资源。
- **跨 Work 冲突：** T-05 触达的其他 active change 在实施前必须由 Lead 协调，无法取得唯一 owner 时阻塞，不强行修改。

## 8. Ready 审核与同步规则

用户已确认拆分、Deep 风险和删除边界。六个 Ticket 均为 `done`，对应 Evidence 已落盘；实现、目录删除和隔离验证均在授权边界内完成。

Ticket 状态、路径、依赖发生变化时，以 Ticket frontmatter 为权威同步本 Map；Goal Plan 创建后，Wave、Gate、workspace 策略和 owner 以 Goal Plan 为编排权威。
