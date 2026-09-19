# 业务模块导航

[pom.xml](pom.xml) 聚合业务模块。架构选择以[模式登记表](../../.agents/skills/engineering-standards/references/project/03-backend-module-modes.md)为唯一入口；完整模块、公开合同和测试根见[模块地图](../../.agents/skills/engineering-standards/references/project/01-module-map.md)。规则由 [engineering-standards](../../.agents/skills/engineering-standards/SKILL.md) 裁决，实现导航按需读取 [wta-module-guide](../../.agents/skills/wta-module-guide/SKILL.md)。

保留具有独立合同的模块手册：[System](wta-system/AGENTS.md)、[Demo](wta-demo/AGENTS.md)、[Profile](wta-profile/AGENTS.md)、[Notify](wta-notify/AGENTS.md)、[SSO](wta-sso/AGENTS.md)、[Third](wta-third/AGENTS.md)。Profile 的 person/enterprise 手册继续约束各自业务边界；profile-bom 仅管理版本。

Workflow、Job、AI 的入口分别为 [wta-workflow/pom.xml](wta-workflow/pom.xml)、[wta-job/pom.xml](wta-job/pom.xml)、[wta-ai/pom.xml](wta-ai/pom.xml)；其源码、资源、测试和 common/API 依赖以对应 POM 和模块地图为准。没有独立合同的重复手册由本导航与上级规则承接。

验证命令沿用[后端入口](../AGENTS.md#验证)，`-pl` 使用相对 `backend/` 的具体模块路径。layered 模块另在仓根运行 `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-notify --mode layered`，按模式登记替换具体模块；禁止把聚合 POM 的零测试误记为业务测试通过。
