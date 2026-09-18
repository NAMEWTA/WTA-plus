# 学习者原始基线

本文件只保存评估时的原始陈述和推断边界。不把“在仓库里工作过”“已经写过四门课”当成能力。

## 采集记录

| 项 | 原始内容 | 时间 |
| --- | --- | --- |
| 激活语句 | 用户消息：激活 `speculo/workflows/learning/G-goal/G-goal.md`；要对当前项目前后端各个模块进行全面的深度学习 | 2026-09-16 |
| 已有材料 | 用户指出 `<Path>{roots.state}/learning/changes/*</Path>` 可优化升级，然后归档为同一个 Change，内含不同子 Change（按业务域 / 切片 / 模块 / 总览） | 2026-09-16 |
| 项目路径 | 当前工作区 `/srv/WTA-plus`（workspace.json `path_base=project-root`） | 2026-09-16 |
| 覆盖深度 | 「全面的深度的学习」→ `coverage_depth=deep` | 2026-09-16 |
| 表达基线 | 未新答；沿用 `learner-profile.md` 与已有四门课的 `expression_level=eli5` | 2026-09-16 |
| 已有 Change | `2026-09-14-namewta-architecture`（standard 总览 6 课）、`2026-09-14-wta-sso`（deep 后端 3 课）、`2026-09-14-wta-notify`（deep 后端 8 课）、`2026-09-14-wta-third`（deep 后端 5 课） | 2026-09-16 |

## 能力基线（未验证）

| 声称或观察 | 是否当作能力 | 说明 |
| --- | --- | --- |
| 仓库里已有 architecture / sso / notify / third Lesson 文件 | 否 | 文件存在只证明课已写出，不是掌握，也不是 deep+前端 已闭合 |
| architecture 课 `coverage_depth=standard` | 缺口 | 本 Goal 升为 deep，必须升级，不把 standard 地图课当成函数级覆盖 |
| sso / notify / third 课明确把前端页面树划出范围 | 缺口 | 本 Goal 要求前后端；这些课要补 domain / web-domain / App 组合 |
| 未采集到“已经会 / 不会”的自评 | 缺口 | 后续自述追加到本文件，不改写本段 |

## 缺口（会改变教学，但不阻止开课）

- 现有四门课仍是独立根 Change；物理子树要等用户确认 `C-consolidate`。
- 不知道学习者是否已能独立改 layered UseCase 或 classic ServiceImpl。
- 不知道是否更关心管理端 `admin-web`，还是三条终端都要同样深。

本课按「三条终端都要能指到组合点，业务模块按前后端切片走完公开函数」设计。若后续只要 admin-web，用 Goal `replan` 缩 scope，不改写本基线原文。

## 追加记录（不改写上文）

| 项 | 原始内容 | 时间 |
| --- | --- | --- |
| C-consolidate | 四门课已嵌入 `children/`（relocation-20260916075345） | 2026-09-16T07:53:45.456Z |
| G-goal replan | 用户要求按最新 G-goal work 完整对齐；覆盖波 2 + mine unit ≤15 + teach-then-mine | 2026-09-16T08:47:16.000Z |

## 推断规则

- 「全面的深度」可以推断 **coverage_depth 与 in-scope 模块集合**，不能推断 **已掌握**。
- 现有 Lesson 是可升级原料，不是覆盖矩阵的 `covered`。
