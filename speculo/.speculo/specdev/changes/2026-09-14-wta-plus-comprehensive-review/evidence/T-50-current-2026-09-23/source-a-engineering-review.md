# T-50 Source A 功能／工程轴审查

固定输入：base `d544f02e1627d76883e9eeaf73a8f2b05002d079` → candidate `9bb2e2888b5d2b0563beb7164c8c0a2165547e2c`，三点 diff。审查仅用该固定 Git 对象，未把变化中的工作树混入。本报告只读；未运行 Maven、Docker、HTTP、前端构建，也未修改仓库。范围是 30 个 backend 路径及 `.agents/skills/engineering-standards/references/notification.md`；SpecDev 进度文件由 Lead 治理，live OpenAPI/生成物尚不在 Source A。

**结论：A1 request changes，AC-050 未通过。** 静态差异未见其他确定性产品缺陷，但 Lead 的 fixed Source A 八类隔离真实验收为 135 项、1 failure、0 error、0 skip；唯一失败是 `NotifySupportedModeIntegrationTest.loopbackHttpEnforcesSubmitPermissionAndSupportedValuesBeforeAnyWrite` 第 309 行，省略 strategy/mode/priority 的默认合法 HTTP 请求预期业务码 200，实际 400。显式 `ALL/ASYNC/0` 请求已通过。失败记录 `/tmp/wta-t50/runs/35e14468040352c5/result.json` 和同目录 fresh XML，source 前后均 clean `9bb2e28`、资源清理无 errors。实际原因待产品 writer 定位，**不能仅凭 record 构造器默认值断言 JSON/HTTP 绑定也正常**；修复条件是保留该真实默认请求断言并重新跑八类零失败。默认全量测试、full package/JAR、live OpenAPI 与前端生成校验尚无本次结果。`git diff --check base...candidate -- backend .agents/skills/engineering-standards/references/notification.md` exit 0。

## 合同核对

| 关注点 | 固定源码证据 | 结论 |
|---|---|---|
| 公共提交 fail closed | `NotificationApplicationRuntimeService.java:52-60,309-359` | `validate` 在 `findDuplicate`、收件人解析和任何写入之前检查 `ALL/ASYNC/0`，非支持策略/priority 抛 `ServiceException`，不能以同幂等键取回旧回执。`NotificationCommand.java:27-40` 原有 null strategy/mode 归一为 ALL/ASYNC，primitive priority 缺省为 0；非零拒绝。JSON `SYNC` 由现有仅 ASYNC 的枚举绑定拒绝。**HTTP 省略字段的真实正例当前失败，不能宣称默认输入在传输层可用。** |
| 生产调用迁移 | 固定提交全 backend `src/main` 的 `new NotificationCommand` 枚举；各路径见 diff | Auth 登录、Captcha SMS/MAIL、Demo Mail/SMS/WebSocket、公告发布、配置 test-send、企业转移、个人重绑两路径、Workflow 均显式 `ALL/ASYNC/0`；这些调用的目标、模板参数、`expiresAt`、`scheduledAt`、幂等键保持原表达式。未发现仍传非零 priority 的生产构造点；新增/改动 caller tests 捕获各类命令，验证码与企业转移保留绝对截止，个人重绑继续无截止。 |
| 历史策略／API 兼容 | `NotificationStrategy.java`、`NotificationMode.java`、`NotificationApplicationService.java`、`NotificationCommand.java` | Service 方法、record 字段及枚举常量未删除；`ORDERED_FALLBACK`/`ESCALATION` 保留供旧记录读取，`NotificationMode` 原本只有 ASYNC。变更是已确认的运行时语义收缩，会拒绝外部仍提交旧策略或非零优先级的客户端；公共 Javadoc 与通知规范已同步，live OpenAPI/生成物仍待 Source B。 |
| 人工 retry、查询、取消 | `NotificationApplicationRuntimeService.java:161-298` | `retry` 查明 Intent 与可选 Delivery 归属之后、候选扫描／requeue 之前校验旧 Intent 策略/模式/优先级；不把历史 unsupported 的本地失败重新排队。`query`/`cancel` 没被本次新检查拦住，保留旧通知控制面。原绝对过期和当前预算/lease检查未削弱。 |
| Worker 历史 WAIT 收敛 | `NotifyDispatchResultService.java:61-215`；`DispatchNotificationService.java:69-95` | 先在 Intent→Outbox→Delivery 锁及活 lease 下核 T-39 截止、旧 PROCESSING、取消/终态；unsupported 检查在路由/Provider 前。外部只在本次 READY 领取、固定新 `DEADLINE_UNSENT_READY` 标记、双方零 attempt、无 provider/accepted/delivered 事实合取下，写 `UNSUPPORTED_MODE_UNSENT` 和 DONE；旧无标记/重领取走 UNKNOWN/WAITING_RECEIPT，不自动发送。IN_APP 已存在消息及本人关系恢复 DELIVERED 而不再 push，孤儿关系按本地不一致失败，无事实时终结未发。各结果经原事务的严格 row-count、finish 和 aggregate；失效 fence 零写。 |
| 数据处置边界 | `evidence/T-50-legacy-mode-disposition.md` | 只读分类 SQL 排除正文、目标值、key 与凭据；新未发标记单独不足以授权数据更改，须由 Worker 锁与 READY 来源验证。处置稿不宣称生产旧数据已盘点/清理；上线前后操作与人工裁决仍属 T-30。 |

## 新测试的真实性与覆盖边界

`NotificationSupportedModeRuntimeTest.java` 直接断言旧策略/非零 priority **在 DAO 交互前**拒绝，以及支持值重复提交仍返回原回执；其他调用方测试核各生产入口优先级归零。`NotifySupportedModeIntegrationTest.java:82-585` 有 opt-in `notify.supported.mode.integration=true`，要求 `T50_MYSQL_PASSWORD` 和 T36 同值私有环境、owned loopback JDBC、Redis；复用 fresh 六 SQL 的 Atomic fixture，真实 DAO/Claim/DispatchResult 事务，供应商 `NotifyClient` 为唯一发送替身。它覆盖 ORDERED_FALLBACK/ESCALATION 的历史 WAIT、旧 mode/priority、markerless READY、重领 PROCESSING、站内已落关系/孤儿/空任务、旧重试拒绝、幂等提交拒绝、正向默认值与截止、JSON SYNC 绑定、loopback Jetty HTTP+SaToken 的授权/拒绝/无写、失效 lease、混合 ACCEPTED/UNKNOWN 同 Intent、截止优先及聚合更新失败回滚。每例清理自有 Intent/trigger；不能将测试替身称为真实短信／邮件供应商。

静态测试覆盖没有发现“只把原断言改成 0”而缺失历史失败路径的问题。Lead 已运行并保留 8 份 fresh XML，实际为 **135 项、1 failure、0 error、0 skip**；该失败命中真实默认 HTTP 正例，不能移除/放宽该断言以求通过。HTTP 测试的权限样例为隔离合成会话，不代表生产数据授权验收。`NotifyDeadlineIntegrationTest` 中将原 80 改 0 是新强制合同后的夹具调整，原截止断言仍在。

## 交付关口与限制

1. 产品 writer 修复默认字段省略请求的实际 HTTP 400：先查清 JSON record 绑定、入站转换或后续应用校验的精确根因，保留显式/省略两条正例及不支持值的负例；Lead 固定新 clean candidate 后重新运行八类，要求逐类正数且零 fail/error/skip，并复核 owned 清理。Source A 的失败记录不得覆盖或称作通过。
2. 同一完整来源执行默认 Maven 测试及 full package/JAR 证明；不能把 opt-in skip 当执行通过，也不能把先前 T-39 候选门禁称为 Source A 结果。
3. 用该候选完整后端 JAR 启动**owned**最小服务，直接捕获 `/v3/api-docs` 原字节、路径/schema 全量比较，再生成/校验 `frontend/packages/api-contracts`。Source A 没有该生成物，故仅判定 Java/文档兼容方向，未对生成合同签收。随后前端 typecheck/test 等适用门禁由 Lead 完成。
4. 历史不支持任务可能不在可领取 READY 范围，Worker 不会自动清空生产库存；只读盘点和人工处置／风险决定仍是 T-30 发布前条件。本次静态审查不把本地合成旧任务当生产盘点。

适用规则按 `engineering-standards` 的通知规范、Java 公共 API 兼容与后端分层核对。唯一 public `NotificationApplicationService` 入口仍由 UseCase→runtime 服务实现，业务模块没有新增 DAO/Mapper 直连。源码和测试未引入新数据库 schema 或新的外部服务。当前结果是 **static scope otherwise sound / Source A real acceptance failed / request changes**。
