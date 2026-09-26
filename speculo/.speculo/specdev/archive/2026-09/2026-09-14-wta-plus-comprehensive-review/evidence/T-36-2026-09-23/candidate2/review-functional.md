# T-36 C2 功能/工程轴只读审查：静态无新增阻断，当前候选验收未通过

固定输入：base `5118051403de0648540646d98536d8c4f3f9f26d`，C1 `54cf715c2829ff95cf994401ff52f02668c1e171`，C2 `a2749a7dcc0bbc5c0643e07eba938446611c995a`，C2 tree `ff190a2f846c3b1545832bc05fbab744913d3bf2`。审查 `git diff C1...C2` 的四个产品路径（清单 `/tmp/wta-t36-c2/product-delta.json`）；其余 base...C1 产品路径与 `/tmp/wta-t36-c1/review-functional.md` 的已核对结论等价。权威合同为当前 Spec AC-036、Ticket 36 revision154/155、适用 AGENTS/Skill 与本票测试矩阵。审查时 `HEAD=C2`、工作树 clean、`git diff --check base...C2` 退出 0。本轴未改 repo，也未运行 Maven、Docker、服务；不读取另一审查轴结论。

## C1 阻断修复复核

1. **C1/P1 多渠道 JSON 文本误判：已在静态实现中关闭。** `InAppNotificationService.java:54-60` 将既存 JSON 解析为 `List<String>` 再与新快照的有序列表比较，保留标题、正文、路径、noticeType 的严格一致性检查。MySQL JSON 的文本空格规范化不再影响同一 intent 的第二个收件人，真实不同渠道及顺序仍被拒绝。`NotifyAtomicResultIntegrationTest.java:333-357` 令同 intent 两收件人的 channels 为 `IN_APP,SMS`，断言一条消息、两条关系、两次实时提示和解析后的渠道顺序。当前 C2 隔离运行中此方法已执行且未失败；整套运行另有一处错误，不能据此宣布整票通过。

2. **C1/P2 站内持久化列长度：已在静态实现中关闭。** `DispatchNotificationService.java:195-210,246-248` 在 `beginInAppAttempt` 的独立预算事务之前，以 Unicode 码点数检查 title≤255、noticeType≤10、path≤500；越界走既有 `LOCAL_DISPATCH_ERROR` 终态，由 `NotifyDispatchResultService.java:122-127,219-225` 容许未预留的本地失败直接 DONE。正文保持完整，1000 码点摘要逻辑不变。真实 MySQL 参数化用例 `NotifyAtomicResultIntegrationTest.java:253-283` 覆盖 255/256 标题、10/11 类型、500 路径及无消息/关系；单元用例 `DispatchNotificationServiceTest.java:299-312` 检查越界 path 不调用预算/持久化。当前 C2 隔离运行中这五种边界用例未失败。合法上限、存量越界、本地终态与原预算语义匹配。

## C2 其余变更及边界

- `DispatchNotificationService.java:212-244` 仅在**代理调用前无 XID 且无同步集合**，并且 `beginInAppAttempt` 或 `completeInApp` 异常后无活 XID 时，移除本次失败留下的同步；原 `RuntimeException|Error` 原样外溢，不把提交不确定性改写成外部 UNKNOWN 或本地 FAILED。已核 dynamic-datasource 4.5.0 源码/字节码的提交异常控制流，见 `/tmp/wta-t36-c1/ds-event-cleanup-review.md`。真实故障用例 `NotifyAtomicResultIntegrationTest.java:503-541` 检查同步清空后同线程下一笔正常提交触发一次新的 AFTER_COMMIT；单元用例 `DispatchNotificationServiceTest.java:314-347` 检查活外层 XID 及合法 AFTER_COMMIT 阶段的既存同步不会被清掉。C2 的局部保护**不修复**其他 UseCase 提交失败遗留同步，也不应在 dispatch 入口凭 XID=null 广域清理；须在后续 T-22/T-30 或框架范围复验登记该残余。
- `DispatchNotificationService.java:220-225` 在预算入口返回 false 后只读复查 Delivery；若已取消/缺失才交由原 `settle(CLOSE)` 的 Intent→Outbox→Delivery 锁序、数据库时钟和 owner/token fence 关闭残留 PROCESSING Outbox。Delivery 仍为 PENDING 的预算重入/失效租约不被擅自 CLOSE；已 DEAD_LETTER 或换 owner 则 `settle` 无权写入。真实 `cancelledBeforeInAppTransactionCannotWriteInboxFact` (`NotifyAtomicResultIntegrationTest.java:678-699`) 断言 CANCELLED、DONE、零 Attempt/Message，当前 C2 隔离运行未失败。
- `NotifyAtomicResultIntegrationTest.java:195-201` 的 teardown 仅在无活 XID 时清测试线程遗留同步，不是产品路径；它不会替代同一方法内的故障→正常提交验证。C2 对 SMS/MAIL 分支、DAO 锁/SQL、Outbox lease fence、结果端口签名、提交后事件消费者与六 SQL 基座无增量改动；C1 对这些路径的逐项核查继续适用。

## 当前门禁事实与处置

Lead 的固定 C2 隔离结果 `/tmp/wta-t36/runs/909ae637d59b2ebe/result.json`：source before/after 均为 C2 clean/tree `ff190a2...`，Atomic 66 tests、0 failure、1 error、0 skipped；Wake 1/0 error/0 skipped；Maven exit 1、driver exit 1，owned 容器/端口/进程组均清理。错误为 `uncertainResultCommitUsesDurableFactsToPreventDuplicateInbox(AFTER)` 的新“同线程下一正常 Delivery”步骤；源码 `NotifyAtomicResultIntegrationTest.java:505-507,521-538` 仅在 BEFORE 分支将 `duringProvider` 恢复为空操作，AFTER 分支在创建第二 Delivery 前仍保留原 commit-fault 注入器，使第二笔继续人为抛异常。此为夹具清场遗漏，不能据此断言生产局部清理失败；C3 应在两分支汇合后、第二笔之前清掉注入器，保留同步为空及下一笔实时事件的原断言，并用固定新提交重新跑完整真实用例。不得把 C2 记录标为通过。SMS8及其余质量门禁仍以 Lead 的独占当前候选记录为准，本轴未执行。

**结论：C2 静态功能/工程审查无新增产品阻断；整票仍 request-changes（当前固定候选真实验收 exit 1）。** 下一候选只需补审相对 C2 的夹具差异，并复验同线程 BEFORE/AFTER 故障、Atomic/Wake 和已规划消费者/质量门禁；若出现新失败另行分析。此判断只绑定 C2 SHA，不预授 C3 PASS。
