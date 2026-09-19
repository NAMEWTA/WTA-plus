# common 基础设施导航

本目录由 [pom.xml](pom.xml) 聚合基础能力，[wta-common-bom/pom.xml](wta-common-bom/pom.xml) 管理版本；业务调用者按需依赖子模块。完整清单与实际测试入口见[模块地图](../../.agents/skills/engineering-standards/references/project/01-module-map.md)，包含富文本清洗与 OSS 引用桥接的 `wta-common-richtext`。

复用、SPI、工具和依赖入口读取 [wta-common-modules-guide](../../.agents/skills/wta-common-modules-guide/SKILL.md)；硬约束由 [engineering-standards](../../.agents/skills/engineering-standards/SKILL.md) 裁决。common 不反向依赖业务实现，也不强套业务五层目录。

定向验证沿用[后端统一命令](../AGENTS.md#验证)。例如在 `backend/` 执行 `./mvnw -pl wta-common/wta-common-richtext -am test`。模块没有独立测试时，选择真实消费者测试，不能把零测试报告称为验证通过。
