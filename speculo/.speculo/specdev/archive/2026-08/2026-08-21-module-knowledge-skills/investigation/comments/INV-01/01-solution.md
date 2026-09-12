---
artifact: wayfinder-solution-comment
ticket: INV-01
sequence: 1
resolution: answered
---

# Solution: plus-ui 前端约定与动态权限路由

- **Ticket：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-01-plus-ui-conventions.md</Path>`
- **答案：** 生效工具链为 Oxlint/Oxfmt 而非 ESLint/Prettier；动态路由由 `getRouters` + `filterAsyncRouter` + `addRoute` 构成；按钮权限走 `v-hasPermi` / `checkPermi`。
- **事实与来源：** 研究正文声明与仓库路径见 Ticket；Skill 落地 `<Path>.agents/skills/namewta-fullstack-development/</Path>`。
- **资产：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-01-plus-ui-conventions.md</Path>`；`<Path>.agents/skills/namewta-fullstack-development/</Path>`
- **后续 Ticket 所依赖的事实：** Skill 目录名 `namewta-fullstack-development`；references 拆分为 coding-style、comments、permission-routing。
- **新浮现的 Tickets：** 无
- **升级的战争迷雾：** 无
- **对现有 Tickets 的影响：** 无
