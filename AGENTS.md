# AGENTS.md

<PROJECT_SKILLS>
## 项目开发 Skill

- 项目开发 Skill 的唯一根目录是 `./.agents/skills/`；本 monorepo 不维护 `.claude` 或 `.codex` Skill/Agent 副本。
- 处理代码、目录、测试、构建、依赖、API、数据库、权限或交付变化前，必须先读取 `./.agents/skills/engineering-standards/SKILL.md`。该入口保留项目架构、模块边界、MyBatis/Java/Vue 结构、质量门禁和交付裁决；入口内部按任务 scope 选择 references。
- 再按任务加载最小充分的前端、后端、system、workflow、common Skill。Skill 的描述只用于触发判断，详细资料按入口中的路由按需读取。
- 项目规范中的“必须/禁止”是硬约束；“建议/优先”是可调整的实现指导。硬约束不能为了省上下文而删除或改成可选。
- 用户指令优先于项目 Skill 中的普通指导；如果用户要求与硬约束冲突，先指出冲突和受影响的契约，再继续完成可以安全完成的部分。
- Skill 摘要与当前源码、配置、POM 或测试冲突时，以当前工作树证据为准，并同步修正父级 Skill；不要为了迎合摘要修改真实实现。

## 执行与完成

- 根据用户意图和任务范围推进工作。已经获得授权的读取、审查、本地测试和可逆修改直接执行，不要因为惯例反复请求确认。
- 任务包含实现、运行、检查和修复时，必须持续到这些目标完成；把完成标准写进任务计划或工作记录。
- 存在彼此独立且可并行的子任务时，若当前执行环境提供子代理能力，应按收益使用子代理；为每个子任务写清范围、输入、输出和验证边界，消息保持人类可读。
- 只读取与当前 scope 有关的文件和 references。小型文档或拼写修改不要求先读取整个项目地图；涉及架构、公共合同、数据库、权限或交付时必须读取相应硬约束。
- 只有部署、发布、远程写入、删除重要数据、提交/推送/合并等不可逆或外部副作用操作，才在准备好具体可审查结果后请求最终确认。
- 验证范围与变更风险匹配：运行受影响的质量门禁，修复由本次变更引起的失败，再报告命令、退出码、未验证项和残余风险；不要用删除测试或放宽规则获得通过。
- 长任务可以维护任务目录内的 `worklog.md`，记录 Goal、Current status、Decisions、Files changed、Remaining work 和 Verification，避免上下文压缩后丢失目标。

## Skill 路由

| 任务 | Skill |
|---|---|
| 全局架构、目录、Java/Vue/MyBatis 结构、测试与交付裁决 | [engineering-standards](.agents/skills/engineering-standards/SKILL.md) |
| 跨前后端业务垂直切片 | [namewta-fullstack-development](.agents/skills/namewta-fullstack-development/SKILL.md) |
| 新增模块、模块事实和跨模块接入 | [wta-module-guide](.agents/skills/wta-module-guide/SKILL.md) |
| wta-common 依赖、SPI 和工具入口 | [wta-common-modules-guide](.agents/skills/wta-common-modules-guide/SKILL.md) |
| Java 公共 API 兼容演进 | [java-api-compatibility](.agents/skills/java-api-compatibility/SKILL.md) |
| NAMEWTA 环境审计、部署、升级和回滚 | [deploy-namewta-environment](.agents/skills/deploy-namewta-environment/SKILL.md) |

</PROJECT_SKILLS>

<!-- SPECULO-PERSISTENT-KNOWLEDGE:START -->
## Speculo 永久知识

以下路径只在当前任务相关时按需读取，不会自动激活 workflow 或 Work：

- learning：<Path>{roots.state}/learning/context/INDEX.md</Path>
- learning：<Path>{roots.state}/learning/context/REVIEW.md</Path>
- ops：<Path>{roots.state}/ops/knowledge/</Path>
- ops：<Path>{roots.state}/ops/hosts/{host_id}/knowledge/</Path>
- ops：<Path>{roots.state}/ops/projects/{project_id}/knowledge/</Path>
- specdev：<Path>{roots.state}/specdev/adr/</Path>
- specdev：<Path>{roots.state}/specdev/context/</Path>
<!-- SPECULO-PERSISTENT-KNOWLEDGE:END -->
