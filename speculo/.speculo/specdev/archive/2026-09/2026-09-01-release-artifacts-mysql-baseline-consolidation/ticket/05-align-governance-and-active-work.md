---
schema_version: 3
artifact: ticket
change: 2026-09-01-release-artifacts-mysql-baseline-consolidation
id: T-05
title: 同步 MySQL-only 规范与活动实施工件
status: done
planning_depth: deep
planning_depth_reason: 修改跨仓库长期规范、部署 Skill 和仍在实施的 SpecDev 路径所有权，需要协调其他活动 Work
ready: true
risk: high
blocked_by: [T-04]
contract_ids: [AC-005, AC-006, AC-011, AC-013, AC-014]
owner: codex:/root
expected_changes:
  - "<Path>README.md</Path>"
  - "<Path>docs/**</Path>"
  - "<Path>.agents/skills/engineering-standards/**</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/**</Path>"
  - "<Path>.agents/skills/deploy-namewta-environment/**</Path>"
  - "<Path>.agents/skills/ruoyi-workflow-module-guide/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/README.md</Path>"
  - "<Path>ruoyi-vue-plus-namewta/docs/upstream/README.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-08-31-account-profile-verification/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/**</Path>"
writable_paths:
  - "<Path>README.md</Path>"
  - "<Path>docs/**</Path>"
  - "<Path>.agents/skills/engineering-standards/**</Path>"
  - "<Path>.agents/skills/namewta-fullstack-development/**</Path>"
  - "<Path>.agents/skills/deploy-namewta-environment/**</Path>"
  - "<Path>.agents/skills/ruoyi-workflow-module-guide/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/README.md</Path>"
  - "<Path>ruoyi-vue-plus-namewta/docs/upstream/README.md</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-08-31-account-profile-verification/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/**</Path>"
read_only_paths:
  - "<Path>release-artifacts/**</Path>"
  - "<Path>ruoyi-vue-plus-namewta/script/**</Path>"
  - "<Path>{roots.state}/specdev/archive/**</Path>"
shared_paths:
  - "<Path>.agents/skills/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-08-31-account-profile-verification/**</Path>"
  - "<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/**</Path>"
shared_path_owners:
  - "<Path>.agents/skills/**</Path> => T-05"
  - "<Path>{roots.state}/specdev/changes/2026-08-31-account-profile-verification/**</Path> => T-05"
  - "<Path>{roots.state}/specdev/changes/2026-09-01-admin-runtime-capability-reconciliation/**</Path> => T-05"
---

# Ticket T-05：同步 MySQL-only 规范与活动实施工件

- **Ticket 文件：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/ticket/05-align-governance-and-active-work.md</Path>`
- **总体 Map：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/tickets-map.md</Path>`
- **上游 Spec：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **完成 Evidence：** `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-05.md</Path>`

## 1. 战略与来源

- **目标：** 让所有当前权威统一声明父仓库唯一 SQL owner、MySQL-only、六文件直接修改和 fresh/upgrade 分流，并修订仍在实施的旧路径授权。
- **可观察产出：** 开发者搜索当前文档、Skill 和活动实施工件时只得到新路径与新规则；历史归档保持原文。
- **来源：** `US-006`、`US-009`、`AC-005`、`AC-006`、`AC-011`、`AC-013`、`AC-014`。
- **当前事实：** 项目画像、数据规则、部署 Skill、README 和至少两个仍在实施的 change 继续声明后端 `script/sql`、append-only 或旧 writable owner。
- **Planning Depth 原因：** 规范是后续 Agent 的行为约束，且活动 change 由其他 Work 占用，错误更新会造成并发路径冲突或历史证据污染。

## 2. 决策状态

### 已锁定决策

- 当前权威只声明 MySQL 8.4；Oracle、PostgreSQL、SQL Server 不支持。
- 六份 SQL 直接修改；`50` 只放 DDL，`60` 只放 DML，不再要求追加历史块。
- fresh 环境顺序导入完整基座；已有库从源/目标 Git Tag 差异生成现场升级步骤，输出到被忽略的 `<Path>temp/relase/</Path>`。
- completed、archived 和纯历史 ELI5/Evidence 不批量改写。
- 其他活动 change 的 SpecDev 状态只能由 Lead 协调修改。

### 已采用的低影响假设

- 活动扫描以 `change_status=active`、`current_work` 和实施路径授权为依据，不把历史说明文档误判为可执行 owner。

### 未决问题

无。

## 3. 范围边界

| IN | REUSE | OUT |
|---|---|---|
| 当前 README/docs、四个项目 Skill、后端当前文档、活动 change 的路径与 SQL 合同修订 | 已实现的新目录、新发布合同、Speculo 偏差控制 | 改写历史归档、执行已有库升级、提交账密、改业务功能 |

## 4. 要构建什么

开发者查阅项目规范后，可以直接知道在哪六份文件修改 SQL、DDL/DML 如何分类、新库和已有库分别怎样处理，以及非 MySQL 方言不受支持。任何仍在实施的 Ticket 不得继续把后端旧 SQL 设为 writable；若其行为合同依赖 append-only，Lead 必须按 SpecDev 偏差规则先修订上游权威再恢复实施。

## 5. 实现契约

- **入口或接缝：** 根 README、项目文档、工程规范、后端/部署/Workflow Skill、活动 SpecDev Ticket/Goal Plan。
- **输入与输出：** 输入为已集成的最终目录与 Spec 锁定决策；输出为一致的当前导航和路径所有权。
- **公共接口变化：** 无运行 API；改变开发与部署治理合同。
- **不变量：** 不写 secret；不改 archived/completed 历史；不覆盖其他 current Work 的未合并用户改动。
- **状态或数据流：** 已验证代码事实 -> 当前文档/Skill -> 活动 Ticket/Goal Plan -> 后续实现者。
- **错误与失败行为：** 发现活动 change 正在写旧 SQL 时停止并协调，不做静默路径替换；规则冲突时更新真正 owner 工件。
- **兼容要求：** 对历史保留原文，通过“当前/历史”分类避免全仓零命中式错误门禁。
- **安全与隐私要求：** 升级报告只写模板字段，不在受跟踪文档记录真实地址、账号、密码或备份位置。

## 6. 执行路线

1. 以最终工作树重新盘点当前文档、Skill、活动 change 与历史工件引用并分类。
2. 先更新工程规范的项目画像、决策例外和数据规则，再同步导航型 Skill。
3. 更新根文档、发布/增强/OSS/上游文档和后端 README，统一 MySQL-only 与六文件直接编辑。
4. Lead 协调并修订仍在实施 change 的 SQL writable/read-only/shared owner、执行路线和验证命令。
5. 保留归档和完成工件原文，对活动范围运行旧路径、append-only 和多方言声明扫描。
6. 运行 Skill、SpecDev、链接与 diff 校验。

## 7. 路径访问契约

- **预计修改点/可写范围：** 与 frontmatter 一致；SpecDev 活动工件必须由 Lead 修改。
- **只读上下文：** 最终发布资产、已删除旧目录状态和历史归档仅用于事实核对。
- **共享路径：** `.agents/skills` 和两个活动 change 由 T-05 唯一 owner；实施前须解除其他 Work 冲突。
- **保留或不动：** `<Path>{roots.state}/specdev/archive/</Path>`、completed 工件、真实 `.env` 与 `<Path>temp/relase/</Path>` 私密报告。

## 8. 验证矩阵

| 行为或风险 | 验证接缝 | 命令或步骤 | 预期结果 | Evidence |
|---|---|---|---|---|
| 正常路径 | 当前权威扫描 | 搜索新 owner、MySQL-only、fresh/upgrade 规则 | 所有要求均可从单次导航找到 | `<Path>{roots.state}/specdev/changes/2026-09-01-release-artifacts-mysql-baseline-consolidation/evidence/T-05.md</Path>` |
| 失败路径 | 冲突检测 | 检查其他 active `current_work` 和 writable paths | 冲突时停止，不覆盖并发工件 | 同上 |
| 回归 | Skill/SpecDev/链接扫描 | 运行 validator 与分类 `rg` | 当前权威无旧规则，历史原文未批量改写 | 同上 |

- **Workspace checks：** Skill 校验、SpecDev 校验、路径链接检查、当前/历史分类扫描。
- **E2E disposition：** `not-required`：治理文档无运行时行为；T-06 负责实际发布与 MySQL E2E。
- **E2E owner/environment：** Lead / current-workspace 或 parent-candidate。
- **Integration evidence：** 父仓库与后端文档提交、活动 change 状态重读、父分支结果。

## 9. 发布、迁移与恢复

- **迁移顺序：** 代码事实完成 -> 规范裁决层 -> 导航 Skill -> 文档 -> 活动计划 -> 校验。
- **兼容窗口：** T-05 完成后当前权威只保留新规则；历史工件长期保留原文。
- **监控信号：** 活动旧路径数量、Skill validator、SpecDev validator、链接扫描和并发状态。
- **回滚或前向恢复：** 若规范与代码冲突，优先前向修正文档；若活动 Work 冲突则停止并恢复其原 owner，不覆盖。
- **不可逆操作与批准点：** 修改其他活动 change 前必须由 Lead 确认其 owner 与状态；不授权数据库、部署、推送或归档。
- **收缩条件：** 所有当前权威和活动 implementation 路径不再表达旧 owner、append-only 或多方言支持。

## 10. 验收标准

- [ ] `AC-005`、`AC-006`、`AC-011`、`AC-013`、`AC-014` 的治理部分有证据。
- [ ] 当前权威统一为父仓库六文件、MySQL-only、直接修改、fresh/upgrade 分流。
- [ ] 活动 change 不再授权写旧 SQL，历史归档未被批量改写。
- [ ] Skill 与 SpecDev 校验无错误，未泄露现场 secret。
- [ ] Evidence、实现提交和父分支结果完整。
