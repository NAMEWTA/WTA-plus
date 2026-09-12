# 评审、上游同步与交付

### DELIVERY-001 聚焦变更

Scope: `repository`

Level: MUST

Source: `repository-fact` (`plan/update.md`, Git history)

Rule: 一个提交聚焦一个逻辑主题；不混入全仓格式化、依赖升级、lockfile 漂移、生成输出或无关重构。后端合同提交先于对应前端消费者，父仓库指针更新单独可审查。

Verification: `git diff --stat` 与逐文件 review；检查提交边界和两端顺序；运行相应门禁。

### DELIVERY-002 上游同步保护

Scope: both submodules and `docs/upstream/customization-map.md`

Level: MUST

Source: `repository-fact`

Rule: 同步上游前检查 `main..upstream/<mirror>` 日志和 merge-base diff；涉及认证、权限、菜单或 Client 的重叠优先审查。镜像分支只 fast-forward，基线标签不移动，合并后逐项保留 customization map 不变量。

Verification: 执行 README/customization map 中的只读 Git 命令；核对 branch/tag；完成热点矩阵与两端构建。

### DELIVERY-003 交付证据

Scope: `repository`

Level: MUST

Source: `builder-baseline`

Rule: 交付说明包含问题/方案、影响 scope、public contract、数据/配置迁移、风险/回滚、实际验证命令和结果。UI 变化提供截图或交互证据；未执行门禁和残余风险显式列出。

Verification: 使用 project review checklist；核对每个结果有 command/cwd/exit code；不得把 planned/not-run 写成 passed。

### DELIVERY-004 文档同步

Scope: architecture, public API, authentication, authorization, database, deployment and tooling changes

Level: MUST

Source: `repository-fact` + `builder-baseline`

Rule: 改变长期不变量时同步对应 README、customization map、SQL README 或本 Skill 的 project facts/decisions；注释解释 WHY、兼容和删除条件，不逐行翻译实现。

Verification: 文档 diff 与代码合同交叉 review；检查旧说明没有继续宣称已失效事实。

### DELIVERY-005 发布与回滚

Scope: release, deployment, schema and configuration changes

Level: SHOULD

Source: `builder-baseline`

Rule: 发布产物来自可复现构建；迁移有顺序、兼容窗口、回滚/补偿和发布后观测。feature flag/临时兼容必须有 owner 与清理条件。

Verification: 发布清单；构建产物来源；迁移演练；监控与回滚步骤 review。
