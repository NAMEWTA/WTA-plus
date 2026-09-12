---
artifact: wayfinder-solution-comment
ticket: INV-04
sequence: 1
resolution: answered
---

# Solution: ruoyi-common 子模块与工具类

- **Ticket：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-04-ruoyi-common.md</Path>`
- **答案：** 父 POM 共 25 个子模块（BOM + 24 jar）；业务按需声明子 artifact，不能依赖父 `ruoyi-common` pom；常用入口含 `RedisUtils`、`LoginHelper`、`JsonUtils`、`ExcelBuilder`、`OssFactory`。
- **事实与来源：** 研究正文声明与仓库路径见 Ticket；Skill 落地 `<Path>.agents/skills/ruoyi-common-modules-guide/</Path>`。
- **资产：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-04-ruoyi-common.md</Path>`；`<Path>.agents/skills/ruoyi-common-modules-guide/</Path>`
- **后续 Ticket 所依赖的事实：** Skill 目录名 `ruoyi-common-modules-guide`；references 拆分为 module-map、core-utils、other-utils。
- **新浮现的 Tickets：** 无
- **升级的战争迷雾：** 无
- **对现有 Tickets 的影响：** 无
