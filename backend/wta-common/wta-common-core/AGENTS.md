# wta-common-core 模块索引

## Scope

本索引覆盖 wta-common/wta-common-core，以本目录的 pom.xml 和源码为准。

## Purpose

说明 wta-common-core 的职责边界、组成和最小验证入口。

## Components

- Java/Spring Boot 代码、资源文件及本模块 Maven 依赖。
- 具体业务规则、接口与数据结构以源码为准。

## Entry Points

- `pom.xml`：模块构建和依赖入口。
- `src/main`：生产代码与资源入口。

## Dependencies

依赖关系以 `pom.xml` 为准；跨模块能力必须使用项目公开 API 或 SPI。

## Verification

在后端仓库根目录执行 `mvn -pl wta-common/wta-common-core -am -DskipTests compile`，再按模块测试配置执行验证。

## Read Next

先阅读本目录的源码包结构、测试和资源，再按任务读取 `.agents/skills` 中对应规范。
