# wta-profile 模块索引

## Scope

本索引覆盖 wta-modules/wta-profile，以本目录的 pom.xml 和源码为准。

## Purpose

Profile 新增模块聚合入口，承载 person、enterprise 两个隔离子域；本索引只定位组成和合同，具体规则以源码及统一模块 Skill 为准。

## Components

- `wta-profile-person`：个人资料能力。
- `wta-profile-enterprise`：企业资料能力。
- `wta-profile-bom`：版本管理；聚合 POM 不承载业务实现。

## Entry Points

- `pom.xml`：模块构建和依赖入口。
- 各子模块的 `pom.xml`：构建和依赖入口。
- 公开跨模块合同位于 `wta-api`；实现模块不互相依赖实现类、Mapper、Entity 或表。

## Dependencies

依赖关系以 `pom.xml` 为准；Profile 只通过 `wta-api`/common SPI 接入 System，通过 Workflow API 合同接入流程；新增业务入口遵循 `Controller|Listener|API Adapter -> UseCase -> Service -> DAO -> Mapper -> XML`。

## Verification

在 `backend/` 目录执行 `./mvnw -pl wta-modules/wta-profile -am -DskipTests compile`，再按 person/enterprise 模块测试配置执行验证。

## Read Next

先阅读person/enterprise 各自的 `AGENTS.md`、源码包结构、测试和资源，再按任务读取 [统一模块导航](../../../.agents/skills/wta-module-guide/SKILL.md) 及适用工程 Skill。
