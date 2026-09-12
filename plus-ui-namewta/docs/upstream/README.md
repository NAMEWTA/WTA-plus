# 上游跟踪

本目录只保存前端上游能力评估与映射材料。产品架构以 `main` 的本地 monorepo 为权威，上游 `6.X-Vue` 是只读能力来源，不要求目录同构。

NAMEWTA 已将上游单 App 源码树重构为多 App 领域架构，因此评估不能以“同路径覆盖”为目标。上游 `src/api`、`src/views`、Store、插件或工具变化，应先判断其真实所有者：领域合同进入 domain，Vue 领域界面进入 web-domain，跨领域合同进入 platform，浏览器实现进入 adapter/web-kit，布局和启动编排才进入 App。

评估上游变化时：

1. 识别新增能力、缺陷修复、安全变化和行为不变量。
2. 映射到本地 `apps`、`domains`、`web-domains`、`platform`、`adapters`、`web-kit` 或 `tooling` 的真实 owner。
3. 明确记录 `adopt`、`adapt`、`reject` 或 `defer`，并给出理由和验证证据。
4. 安全变化不得静默延后；实现必须进入当前本地 owner 并通过对应门禁。

已经删除的根级 `src` 和 App 内领域 API 兼容门面不再恢复。新增 App 必须消费公开包入口，不能通过复制 Admin 源码吸收上游变化。
