# 独立应用导航

[pom.xml](pom.xml) 聚合三个独立 Spring Boot 应用，具体版本、依赖、启动类和测试入口以各模块 POM/源码及[模块地图](../../.agents/skills/engineering-standards/references/project/01-module-map.md)为准：

- [wta-monitor-admin](wta-monitor-admin/pom.xml)：Monitor Admin。
- [wta-snailai-server](wta-snailai-server/pom.xml)：SnailAI 服务。
- [wta-snailjob-server](wta-snailjob-server/pom.xml)：SnailJob 服务。

它们通过发布资产组装，供应商坐标、协议和 schema 不能按自有命名规则批量改写。工程规则与测试/双 bundle 命令见[后端入口](../AGENTS.md)；仅做定向编译时，在 `backend/` 使用 `./mvnw -pl wta-extend/wta-monitor-admin -am -DskipTests compile` 并明确这不是运行验收。
