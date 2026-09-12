---
schema_version: 1
artifact: triage
change: 2026-08-21-module-knowledge-skills
mode: intake
source: <Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/source.md</Path>
classification: mixed
risk: low
route: specdev/wayfinder
ready_for_implementation: false
external_action: not-applicable
updated_at: 2026-08-21T09:50:00+08:00
---

# Triage: 模块研究与 Skill 创建

## 当前判定

- **影响：** 仅本地知识与 Agent Skill 作者；不修改生产前后端源码。错误知识可能误导后续编码，但本 change 本身无运行时、数据或对外接口事故半径。
- **紧急度：** scheduled
- **当前证据：** 用户在对话中要求新建独立 change，先按 `<Path>{roots.workflows}/specdev/common/skills/research/SKILL.md</Path>` 深度检索 4 个仓库主题，再用 skill-creator 在 `<Path>.agents/skills/</Path>` 下落地多个 Skill；并要求描述不清时按引用路径读源码。既有 change `2026-08-20-namewta-client-rbac-review` 仍为 blocked，用户指定本次另开 change。仓库已有 `<Path>.agents/skills/engineering-standards/</Path>`，新 Skill 定位为模块知识地图，不替代规范裁决。
- **相关代码/工件：** `<Path>plus-ui-namewta/</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/</Path>`、`<Path>ruoyi-vue-plus-namewta/ruoyi-common/</Path>`、`<Path>.agents/skills/</Path>`
- **规划深度：** lite（文档/Skill 变更、无生产路径写入、无公共 API/数据迁移）；后续若 Skill 结构或研究切片需要跨文件合同，可升为 standard。

## 未知项

- **可发现事实：** 4 个主题的现行代码事实、调用入口、工具类清单与 Skill 切片边界，须由后续研究从仓库查明。
- **需要用户决定：** 无。Skill 目录名、研究并行与「描述不清则读源码」已由用户指定；本 intake 不扩大范围。
- **低影响实现细节：** Skill 内 references 拆分粒度、frontmatter 触发措辞，由后续 Skill 作者按 skill-creator 与既有 `<Path>.agents/skills/engineering-standards/</Path>` 惯例决定。

## 路由

- **下一 Work：** `<Path>{roots.workflows}/specdev/W-wayfinder/W-wayfinder.md</Path>`
- **理由：** 外部行为尚未成为可实现合同；4 条研究线超出单次上下文，且到 Skill 落地的路径仍不可见。Wayfinder 研究 Ticket 调用 `<Path>{roots.workflows}/specdev/common/skills/research/SKILL.md</Path>` 写入 `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/</Path>`，关闭后再进行 Skill 创建。不是 bug、不需要 Grill 锁定产品行为，也未到 Spec/Tickets/实现。`ready_for_implementation` 为 false。

## 外部动作

- **远程目标：** 无
- **关闭能力：** not-applicable
- **当前状态：** not-applicable
- **授权记录：** 无。未授权 git 提交、推送、打包 `.skill`、或写入 `<Path>plus-ui-namewta/</Path>` / `<Path>ruoyi-vue-plus-namewta/</Path>`。
- **尝试与结果：** 无

外部动作只投影最终完成，不替代本地状态、Ticket、Map 或 Evidence。
