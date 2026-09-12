---
artifact: wayfinder-solution-comment
ticket: INV-03
sequence: 1
resolution: answered
---

# Solution: ruoyi-workflow 对外能力与业务接入

- **Ticket：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-03-ruoyi-workflow.md</Path>`
- **答案：** 公开门面是 `ruoyi-api` 的 `WorkflowService`；业务用 `businessId` 绑定、`startWorkFlow` / `completeTask` 驱动，并用 `ProcessEvent` 回写状态；不要 Maven 依赖 `ruoyi-workflow`。
- **事实与来源：** 研究正文声明与仓库路径见 Ticket；Skill 落地 `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>`。
- **资产：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-03-ruoyi-workflow.md</Path>`；`<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>`
- **后续 Ticket 所依赖的事实：** Skill 目录名 `ruoyi-workflow-module-guide`；references 拆分为 capability-map、integration-guide、leave-sample。
- **新浮现的 Tickets：** 无
- **升级的战争迷雾：** 无
- **对现有 Tickets 的影响：** 无
