# 全栈验证矩阵

先运行受影响包/模块的快速反馈，再按风险扩展到根级门禁。每项命令记录工作目录、退出码、跳过原因和外部服务条件。

| 变化 | 最低验证 |
|---|---|
| domain transport/API | URL、method、请求参数、响应映射、错误和相邻 domain tests |
| Vue 页面/runtime/manifest | 页面状态、异步取消、权限失败关闭、registration 与 App 组合 tests |
| CRUD/树表/字典 | `docs/fm` 校验、选择/标签/详情回显、空值和未匹配值、缓存失效 |
| 后端 CRUD/Controller | 模块测试、权限/数据范围、GET/POST/`@Log`、校验和序列化 |
| layered 模块 | `validate-module-mode.mjs`、模块测试、事务与 DAO/Mapper 边界 |
| Mapper/XML/SQL | 参数/alias/分页/数据权限、MySQL 基座和真实数据库测试 |
| POM/package/bundle | `pnpm architecture:check`、前端 lint/typecheck/test/build；Maven test、full/core package |
| 权限、Client、匿名接口 | 未登录、无权限、跨 Client、签名/重放/幂等和审计脱敏 |
| 发布配置 | release contract、外部服务属性门控、bundle 内容和部署报告 |

Skill 文档或模板变化至少运行：

```bash
node .agents/skills/namewta-fullstack-development/scripts/validate-skill.mjs
node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs
node docs/fm/scripts/validate.mjs
```
