# wta-profile-person 模块索引

## Scope

本索引覆盖 wta-modules/wta-profile/wta-profile-person，以本目录的 pom.xml 和源码为准。

## Purpose

个人申请、认证、材料、档案投影、重新绑定和通知；本索引只说明入口和边界，业务细节以源码为准。

## Components

- `controller/admin|self|anonymous`：HTTP 入口。
- `usecase`、`service`、`dao`、`mapper`、`domain`：五层主链路及领域模型。
- `listener`、`event`、`port`、`support`：事件、外部合同和纯辅助；外部实现位于 `adapter/api|codec|gateway|provider|security|time`。

## Entry Points

- `pom.xml`：模块构建和依赖入口。
- `src/main/java/org/dromara/profile/person`：生产代码入口。
- `src/main/resources/mapper/person`：Mapper XML 入口。

## Dependencies

依赖关系以 `pom.xml` 为准；person 不读取 enterprise 实现、Mapper、Entity 或表。入口统一走 `Controller|Listener|API Adapter -> UseCase -> Service -> DAO -> Mapper -> XML`；DAO 只依赖 Mapper。

## Verification

在后端仓库根目录执行 `mvn -pl wta-modules/wta-profile/wta-profile-person -am -DskipTests compile`，并运行 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs wta-modules/wta-profile/wta-profile-person --mode=layered`，再按模块测试配置执行验证。

## Read Next

先阅读本目录源码包结构、测试和资源，再按任务读取 [Profile 分层边界](../../../../.agents/skills/wta-module-guide/references/modules/profile/layered-boundaries.md)。
