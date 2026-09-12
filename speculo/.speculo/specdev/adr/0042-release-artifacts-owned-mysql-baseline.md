# ADR-0042: release-artifacts 拥有 MySQL 完整基座

- **Status:** Accepted
- **Date:** 2026-09-02
- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>` DEC-001 至 DEC-008

## Context

父仓发布资产和后端 `script/` 曾同时保存数据库与流程资源，发布脚本还会复制并覆盖 SQL，导致事实源不清和双源漂移。项目已经由 ADR-0011 锁定为 MySQL-only，但仍需要明确当前完整基座的 owner、可变性和 fresh/upgrade 使用方式。

## Decision

`release-artifacts/` 是发布资产和 MySQL 基座的唯一 owner。`docker/infrastructure/mysql/init/` 下被 Git 跟踪的 `10/20/30/40/50/60` 六份 SQL 是可直接修改的当前完整基座；发布脚本只消费和验证，不复制、生成或覆盖。全新 MySQL 8.4 环境按 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 导入。已有数据库升级必须比较现场 Git Tag 与目标 Tag 的六份基座差异，经过备份、评审和隔离演练后只执行所需差异，不重放完整基座。`50` 保持 NAMEWTA DDL，`60` 保持 NAMEWTA DML；非 MySQL 方言不保留。

## Consequences

后端 `script/` 不再拥有发布资源，活动代码、测试、Skill 和文档不得重新依赖旧路径。完整基座可以直接迭代，不是 append-only 迁移账本；现场升级证据保存在被忽略的部署报告中，不能成为第二套仓库事实源。本决策补充 ADR-0011 的支持面，不取代它。
