# ADR-0011: NAMEWTA MySQL-only 数据库合同

- **Status:** Accepted
- **Date:** 2026-08-24
- **Source:** `2026-08-24-upstream-fork-upgrade-remediation` DEC-001

## Context

上游框架具备多数据库和异构数据源能力，但 NAMEWTA 的 Client/RBAC、OSS 生命周期、统一通知及增量迁移只在 MySQL 8.4 上形成了自动化合同。继续宣称其他方言可用，会把未经验证的 SQL、MyBatis 映射和迁移兼容性暴露为虚假支持面。

## Decision

NAMEWTA 业务扩展只支持并自动化验收 MySQL 8.4。上游框架对其他数据库的支持不自动扩展到 NAMEWTA 产品能力。若未来需要新增方言，必须通过独立 change 明确 schema、SQL、MyBatis、迁移、外部服务和 CI 验收合同，不能以文档声明或上游能力推定兼容。

## Consequences

数据库 schema、增量 SQL、集成测试和支持文档可以围绕单一可信方言维护，减少未经验证的兼容分支。代价是放弃表面上的数据库可移植性；切换或新增数据库将成为需要显式设计、迁移和完整验证的产品变更。
