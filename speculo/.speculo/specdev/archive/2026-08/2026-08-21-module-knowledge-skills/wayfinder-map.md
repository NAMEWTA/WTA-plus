---
artifact: wayfinder-map
change: 2026-08-21-module-knowledge-skills
status: active
---

# Wayfinder Map: 模块知识 Skill 寻路

## 目的地

在 `<Path>.agents/skills/</Path>` 下落地 4 个模块知识型 Skill，覆盖前端约定、ruoyi-system、ruoyi-workflow 与 ruoyi-common；每条能力描述附仓库路径，描述不清时按路径读取源码。Intake 只冻结目的地，不绘制或关闭调查 Ticket。

## 说明

- 领域：仓库模块知识与 Agent Skill 作者，不是生产代码变更。
- 研究调用：`<Path>{roots.workflows}/specdev/common/skills/research/SKILL.md</Path>`；结果写入 `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/</Path>`。
- 规划深度：lite；若切片合同需要再升为 standard。
- 本地图不把生产实现带入执行。四条研究 Ticket 已关闭，对应 Skill 已写入 `<Path>.agents/skills/</Path>`。
- 与 `<Path>.agents/skills/engineering-standards/</Path>` 分工：规范裁决归既有 Skill，本批只做模块知识地图。

## 已做出的决策

- **plus-ui 前端约定与动态权限路由：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-01/01-solution.md</Path>` —— 研究见 `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-01-plus-ui-conventions.md</Path>`；Skill 落地 `<Path>.agents/skills/namewta-fullstack-development/</Path>`。
- **ruoyi-system 对外能力与调用方式：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-02/01-solution.md</Path>` —— 研究见 `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-02-ruoyi-system.md</Path>`；Skill 落地 `<Path>.agents/skills/ruoyi-system-module-guide/</Path>`。
- **ruoyi-workflow 对外能力与业务接入：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-03/01-solution.md</Path>` —— 研究见 `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-03-ruoyi-workflow.md</Path>`；Skill 落地 `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>`。
- **ruoyi-common 子模块与工具类：** `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-04/01-solution.md</Path>` —— 研究见 `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-04-ruoyi-common.md</Path>`；Skill 落地 `<Path>.agents/skills/ruoyi-common-modules-guide/</Path>`。

## 尚未明确

无。

## 超出范围

- 不修改 `<Path>plus-ui-namewta/</Path>` 或 `<Path>ruoyi-vue-plus-namewta/</Path>` 生产源码
- 不打包 `.skill` 文件
- 不提交 git
- 不改写既有 blocked change `2026-08-20-namewta-client-rbac-review`
