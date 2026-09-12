# MySQL 发布基座术语

- **Source:** `<Path>{roots.state}/specdev/archive/2026-09/2026-09-01-release-artifacts-mysql-baseline-consolidation/spec.md</Path>`
- **Graduated:** 2026-09-02

**MySQL 发布基座**：由 `release-artifacts/docker/infrastructure/mysql/init/` 唯一拥有并被 Git 跟踪的六份当前完整 SQL；开发者直接迭代，发布脚本只消费和验证。
_Avoid_: 后端 script 副本、构建生成 SQL、append-only 迁移账本

**全新初始化**：在空白 MySQL 8.4 目标库中按 `10 -> 20 -> 30 -> 40 -> 50 -> 60` 顺序导入六份完整基座并验证运行不变量的流程。
_Avoid_: 对已有数据库重放完整基座、任意排序执行

**已有数据库升级**：比较现场 Git Tag 与目标 Git Tag 的六份基座差异，经备份、评审和隔离演练后只执行目标环境所需 DDL/DML，并在私密部署报告中留证。
_Avoid_: 仓库内第二套 migration 事实源、没有 Tag 或备份就试错

**NAMEWTA DDL/DML 分层**：`50-namewta-ddl.sql` 只承载 NAMEWTA 结构，`60-namewta-dml.sql` 只承载初始化、菜单、字典、配置和回填数据。
_Avoid_: 在 50 写业务数据、在 60 建表改表、恢复非 MySQL 方言
