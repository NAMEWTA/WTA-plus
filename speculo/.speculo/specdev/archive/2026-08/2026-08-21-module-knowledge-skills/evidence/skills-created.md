---
artifact: evidence
change: 2026-08-21-module-knowledge-skills
id: skills-created
lead: cursor-agent
updated_at: 2026-08-22T00:02:19+08:00
---

# Evidence: 模块知识 Skill 创建与校验

本 change 为知识/Skill 作者，不是生产实现。未授权 git commit，未运行 `package_skill.py`。

## 研究工件

| 主题 | Investigation Ticket |
|---|---|
| plus-ui 前端约定与动态权限路由 | `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-01-plus-ui-conventions.md</Path>` |
| ruoyi-system 对外能力与调用方式 | `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-02-ruoyi-system.md</Path>` |
| ruoyi-workflow 对外能力与业务接入 | `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-03-ruoyi-workflow.md</Path>` |
| ruoyi-common 子模块与工具类 | `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/INV-04-ruoyi-common.md</Path>` |

关闭评论：

- `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-01/01-solution.md</Path>`
- `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-02/01-solution.md</Path>`
- `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-03/01-solution.md</Path>`
- `<Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/investigation/comments/INV-04/01-solution.md</Path>`

## Skill 路径

| Skill | 目录 |
|---|---|
| namewta-fullstack-development | `<Path>.agents/skills/namewta-fullstack-development/</Path>` |
| ruoyi-system-module-guide | `<Path>.agents/skills/ruoyi-system-module-guide/</Path>` |
| ruoyi-workflow-module-guide | `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>` |
| ruoyi-common-modules-guide | `<Path>.agents/skills/ruoyi-common-modules-guide/</Path>` |

## quick_validate 结果

执行目录：项目根（包含 `speculo/`、`.agents/` 与两个子模块的父仓库目录）。

环境：`uv 0.7.6`、`Python 3.12.10`；成功复跑通过 `uv run --with pyyaml` 显式提供临时 `PyYAML` 依赖。未运行 `package_skill.py`。

直接使用当前 Python 环境的首次复核命令：

```bash
python "$HOME/.codex/skills/.system/skill-creator/scripts/quick_validate.py" .agents/skills/namewta-fullstack-development
```

结果：exit 1，`ModuleNotFoundError: No module named 'yaml'`；分类为验证环境依赖缺失，不是 Skill 内容失败。

成功复跑的完整命令：

```bash
for skill_dir in \
  .agents/skills/engineering-standards \
  .agents/skills/namewta-fullstack-development \
  .agents/skills/ruoyi-system-module-guide \
  .agents/skills/ruoyi-workflow-module-guide \
  .agents/skills/ruoyi-common-modules-guide
do
  uv run --with pyyaml python \
    "$HOME/.codex/skills/.system/skill-creator/scripts/quick_validate.py" \
    "$skill_dir" || exit 1
done
```

| Skill 目录 | 退出码 | 输出 |
|---|---|---|
| `<Path>.agents/skills/engineering-standards/</Path>` | 0 | Skill is valid! |
| `<Path>.agents/skills/namewta-fullstack-development/</Path>` | 0 | Skill is valid! |
| `<Path>.agents/skills/ruoyi-system-module-guide/</Path>` | 0 | Skill is valid! |
| `<Path>.agents/skills/ruoyi-workflow-module-guide/</Path>` | 0 | Skill is valid! |
| `<Path>.agents/skills/ruoyi-common-modules-guide/</Path>` | 0 | Skill is valid! |

## Wayfinder 校验

命令：`node <Path>{roots.workflows}/specdev/common/tools/validate-specdev.mjs</Path> --stage wayfinder <Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills</Path>`

结果：exit 0；`Summary: 0 error(s), 0 warning(s)`。

## CR-001 修复

- `<Path>.agents/skills/namewta-fullstack-development/SKILL.md</Path>` 已将仓库锚点改为“工具链脚本”，并明确 `pnpm fmt` 会写入工作树、不是门禁。
- 本 Evidence 已补齐验证 cwd、命令、解释器、依赖、退出码、首次失败分类和成功复跑结果。
- `<Path>.agents/skills/engineering-standards/SKILL.md</Path>` 已增加领域知识路由：规范裁决仍由 engineering-standards 负责，实际任务命中 plus-ui、system、workflow 或 common 领域时调用最小充分的原子 Skill。

## 偏差与残余

- 未把研究正文从 Ticket 移入 solution comment 全文；评论只保留一句话答案与资产指针，详细声明仍在 Ticket 正文。
- 四个原始领域 Skill 与本 Evidence 的初版已包含在父仓库 commit `477f09b032d514ea80fc71b49e92c681c3b00b74`。CR-001 修复、engineering-standards 路由、review 与状态收尾是当前未提交工作树变更；未授权且未执行 git commit。
- 未归档、未提升永久 `<Path>{roots.state}/specdev/research/</Path>`；需用户确认后由 A-archive 处理。
- 不修改 `<Path>plus-ui-namewta/</Path>` 与 `<Path>ruoyi-vue-plus-namewta/</Path>`。
