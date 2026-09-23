# 清理与保留计划（当前候选）

旧37份重复手册等清理已发生，原逐文件记录保留在<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/replan-2026-09-23-before/cleanup-plan.md</Path>及历史Evidence，不能作为再次删除清单。本轮不删除产品文件。

| 对象 | 分类 | 责任 | 收缩/保留条件 |
|---|---|---|---|
| 受跟踪真实local配置 | REMOVE tracked / KEEP local | T-32 | 安全示例与ignore、取消跟踪、保留本地配置；真实轮换独立核实 |
| 公共CORS通配 | REWRITE | T-33 | 精确来源/配置负向/SSO回归 |
| 消息盒子总开关耦合、重复正文 | REWRITE | T-34 | REST独立及身份隔离 |
| 虚假支持的高级通知参数 | CONTRACT | T-50 | 全部仓内调用迁移、拒绝未支持值、存量清单 |
| OSS同步全配置巡检/业务canary前置 | CONTRACT | T-44 | D-008确认，权限/对象/PRIVATE校验先证明保留，规范/测试同改 |
| 403折叠空策略的确定结论 | REWRITE | T-45 | 已观察允许/拒绝/未知，报告范围明确 |
| 每次启动深度JAR修复与手写端口解析 | MOVE/REWRITE | T-48 | 显式doctor/repair仍受构建锁/安全路径保护；正常路径回归 |
| SINGLE无效参数与configKey重复事实 | REMOVE after evidence | T-49 | 逐键消费者/默认值审查，无安全规则流失 |
| 快照SPI和生产适配缺口 | IMPLEMENT | T-42 | 不删除附件功能规避缺陷；真实owner/授权/清理闭环 |
| 日志、原review、原执行Evidence | KEEP | Lead | 原字节不改；新结论写新文件 |
| 永久ADR/context、相邻OIDC change | READ ONLY | A/其他change | 当前只提候选替代及来源，不能直接提升/接管 |
| PKCE/HMAC/fence/唯一键/OSS引用/第三方schema | KEEP | 原能力owner | 不以配置简化或代码量理由删除 |

所有新增清理需真实引用扫描、owner和恢复点。生成物通过正式流程更新；禁止手改生成声明、删测试或放宽规则取绿。真实对象删除、Git历史重写与运行数据处置不在普通清理授权中。

## 活动文档归属

README/handoff/coverage/verification/refactoring/cleanup/architecture为当前投影；Spec决定行为；Ticket frontmatter决定状态/写集/依赖；Map/plan-data只投影；Goal决定Gate。LOG追加、ADR保留替代链、旧Evidence不可覆写。before快照仅用于历史恢复，不是另一套活动权威。
