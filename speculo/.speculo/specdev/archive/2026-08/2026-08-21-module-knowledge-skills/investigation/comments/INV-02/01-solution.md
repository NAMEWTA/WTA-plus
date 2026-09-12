---
artifact: wayfinder-solution-comment
ticket: INV-02
sequence: 1
resolution: answered
---

# Solution: ruoyi-system 对外能力与调用方式

- **Ticket：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-02-ruoyi-system.md</Path>`
- **答案：** 跨模块稳定面在 `ruoyi-api` 的 `org.dromara.system.api.*`，同 JVM Spring 注入；HTTP `/system`、`/resource`、`/monitor` 仅供前端；其他业务模块 POM 不依赖 `ruoyi-system`。
- **事实与来源：** 研究正文声明与仓库路径见 Ticket；Skill 落地 `<Path>.agents/skills/ruoyi-system-module-guide/</Path>`。
- **资产：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-02-ruoyi-system.md</Path>`；`<Path>.agents/skills/ruoyi-system-module-guide/</Path>`
- **后续 Ticket 所依赖的事实：** Skill 目录名 `ruoyi-system-module-guide`；references 拆分为 capability-map、domains、how-other-modules-call。
- **新浮现的 Tickets：** 无
- **升级的战争迷雾：** 无
- **对现有 Tickets 的影响：** 无
