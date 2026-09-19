# 后端工作区导航

本目录是 monorepo 的 Maven reactor。处理实现前读取仓根 [engineering-standards](../.agents/skills/engineering-standards/SKILL.md)；业务模式和完整模块清单分别以[模式登记表](../.agents/skills/engineering-standards/references/project/03-backend-module-modes.md)和[模块地图](../.agents/skills/engineering-standards/references/project/01-module-map.md)为准。

依赖关系以各模块 `pom.xml` 为准；跨模块能力必须使用项目公开 API 或 SPI。业务规则、接口与数据结构以当前源码和测试为准；先定位目标模块的源码、资源和测试，再按工程 Skill 路由加载规范。

- [pom.xml](pom.xml)：版本、reactor、测试与 bundle 配置；[mvnw](mvnw) 是统一 Maven 入口。
- [wta-admin](wta-admin/pom.xml)：主应用组装，入口 `wta-admin/src/main/java/org/namewta/NamewtaApplication.java`；集成测试位于 `wta-admin/src/test/java`。
- [wta-api](wta-api/pom.xml)：跨业务模块 Service/SPI/DTO；兼容演进读取 [java-api-compatibility](../.agents/skills/java-api-compatibility/SKILL.md)。
- [wta-common](wta-common/AGENTS.md)、[wta-modules](wta-modules/AGENTS.md)、[wta-extend](wta-extend/AGENTS.md)：基础设施、业务模块与独立应用导航。聚合 POM/BOM 不虚构 `src/main`。

## 验证

以下 cwd 均为 `backend/`。完整测试为 `./mvnw test`；定向示例为 `./mvnw -pl wta-modules/wta-demo -am test`，其他模块使用模块地图中相对 backend 的真实 POM 路径替换 `-pl`，BOM 只做构建验证、不声称运行测试。

后端交付还需 `./mvnw clean package -DskipTests` 与 `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`，各自在仓根运行 `bash scripts/ci/verify-admin-bundle.sh full` 或 `core` 核对对应产物。先保存完整测试证据，再跳过测试打包；外部服务用例按实际条件单独启用，skip 不算通过。
