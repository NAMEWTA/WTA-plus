# T-36 安全／规格轴固定候选审查

**结论：REQUEST CHANGES／固定 C1 拒绝。** 静态预算、fence 和普通 complete 收口未见新的安全绕过，但真实 Atomic61 有 3 项失败，提交后推送与取消竞争的 AC-036 行为尚未成立。固定 base `5118051403de0648540646d98536d8c4f3f9f26d`，source `54cf715c2829ff95cf994401ff52f02668c1e171`，tree `fb42e3016b15f5f3b9e741d90c4f9803925d651d`；按三点差异审 21 个产品路径（20 个 backend + 1 个六 SQL 基座注释），`/tmp/wta-t36-c1/path-audit.json` 显示写集违规 0。未改 repo、未运行 Maven/Docker/SQL/服务；实际运行数字只引用 Lead 的 `/tmp/wta-t36-c1/real-atomic-wake-counts.json` 与 `candidate-verdict.json`，没有把它们冒称为本人执行。

## 静态已核对的边界（不能覆盖下述真实失败）

1. **有界预算与重复领取。** `NotifyDispatchResultService.java:61-83` 在独立代理短事务中按 Intent→Outbox→Delivery 锁定当前行，核对租约 owner/token、数据库 UTC、IN_APP/PENDING，然后**先**识别本租约 `IN_APP_ATTEMPT_RESERVED`，再检查 `attempt_count/max_attempts`。`NotifyOutboxMapper.xml:26-34` 再以当前 token/租约/预算/标记做条件更新，只有恰好一行才返回 true；耗尽分支在锁内写 FAILED/DEAD_LETTER/聚合，零消息写。Outbox count 已持久消耗而消息结果事务回滚是 revision155 允许的状态。新 token 的 claim 在同一 UPDATE 清仅 IN_APP 固定标记，渠道从批量 Delivery 查询取得，外部渠道同字面错误码不清（`NotifyOutboxClaimService.java:27-42`、Mapper XML `14-24`）。claim 返回的旧候选对象不被用于判断：dispatch 重读，begin 再锁当前行。
2. **消息／关系／结果原子与 fence。** `NotifyDispatchResultUseCase.java:25-34` 把 begin 和 completeInApp 分为两个 `@DSTransactional` 代理入口；仅 begin 成功才调用后者（`DispatchNotificationService.java:184-209`）。`NotifyDispatchResultService.java:94-105` 在当前活租约、PENDING IN_APP、用户匹配和预留标记成立后才调用本地 persist，之后再次验证租约，并在同一结果事务写 Delivery/Attempt/Outbox/聚合；任意 insert/update/finish 零行或异常抛出并回滚消息结果。Outbox finish SQL 自身仍以 owner/token、未过期 lease 做条件写（Mapper XML `46-54`）。同 intent 两用户由 Intent 锁串行化共享消息创建，关系有 DDL 唯一键，已有快照不一致抛错（`InAppNotificationService.java:37-71`）。摘要列按 Unicode 码点截取 1000，完整正文留 content（`122-126`）。
3. **草稿绕过已修。** 普通 `complete` 对 IN_APP 只允许**未预留**的 `FAILED/LOCAL_DISPATCH_ERROR`，成功结果或已预留后绕过原子入口均抛出（`NotifyDispatchResultService.java:114-127`）。候选包含代理层反例和本地失败正例（`NotifyAtomicResultIntegrationTest.java:216-252`）。此前草稿提出的“无预留也能 DELIVERED”不再成立。
4. **提交后实时与隐私的源码路径。** 新关系才发布只有 messageId/userId 的事件；`DsTxEventListener(AFTER_COMMIT)` 再从已提交的本人关系及消息读取并定向推送（`InAppNotificationService.java:61-102`、`InAppCommittedPushListener.java:18-26`）。监听器捕获推送异常仅记 ID/异常类，不记录正文或异常 message，不回写 Delivery；没有新增外部 Provider I/O 进入结果事务。复投已有关系不再发布事件。不过 C1 真实用例中该监听器调用次数为 0，故此项不能判通过。
5. **外部渠道保守分类保持。** `DispatchNotificationService.java:90-181` 的 SMS/MAIL plan、`NotifyClient.send`、ACQUIRE 前置失败、COMPLETE/未知调用后保守 UNKNOWN 分支相对 base 只是从原 IN_APP if/else 提出，无分类改变；普通结果事务的外部渠道仍自增原结果计数（`NotifyDispatchResultService.java:151-168`），claim 仅在 `inApp` 为真时清内部标记。人工管理 retry 仍重置 Outbox 次数；本票的“有界”仅指**自动调度周期**，T38 另定人工重试合同，不将它误称全生命周期物理上限。

## 实际失败与处置

- **阻断 AC-036 提交后实时行为：** `afterCommitPushFailureKeepsCommittedFactAndDoesNotRetriggerOnDuplicate`（测试行 288-298）预期 AFTER_COMMIT 监听调用 1，实得 0；`successAndReclaimAfterRollbackKeepOneInboxFactAndOneCommittedAttempt`（373-390）预期重领成功后的实时调用 1，实得 0。两项同指提交后事件从注册到执行的真实链路没有达到测试断言；静态注解存在不等于真实监听生效。根因可在新候选与隔离环境进一步查明，不能把“只是测试夹具”当作已证事实。
- **阻断取消竞争清理：** `cancelledBeforeInAppTransactionCannotWriteInboxFact`（631-650）在取消和在途 dispatch 竞争后要求 Outbox DONE，实得 PROCESSING。候选 `dispatchInApp` 在 begin 返回 false 时直接返回（207），若取消已把 Delivery 置 CANCELLED，Outbox 仍可能保持该状态直至后续租约处理。修复需避免把同 lease 已预留且正在执行的重复 begin 当作取消并过早关闭；必须以新候选真实测试验证。
- Lead 记录固定 C1 Atomic **61 测试／3 failures／0 errors／0 skipped**，Wake **1／0／0／0**；Unit175 和 SMS8 由 Lead 的 `candidate-verdict.json` 记为通过。`source-after.json` 确认记录时 HEAD/tree 为上述固定值且状态 clean；`candidate-verdict.json` 已标记 attempt1 rejected。保留这一失败历史，后续 C2 不能覆盖 C1 证据。

## 仍需验收的范围

源码中有真实 `DSTransactional` 代理及 Mapper/双连接夹具；集成测试仅在 `-Dnotify.atomic.integration=true` 时启用，启用后 URL 限定 owned loopback 测试库且环境密码必须存在（`NotifyAtomicResultIntegrationTest.java:61-95`）。源码包含六处写入失败、同 intent 双用户、同 token 末次预算、ACK 丢失、双 claimer、旧 owner/取消竞争等断言；其中真实失败如上，故不能勾 AC-036。本人未独立运行门禁；新候选仍须完整真实 MySQL/Redis、零 skip、故障与清理证据。静态审查不能证明 `DsTxEventListener` 装配、MySQL 锁时序或 ACK 注入结果。

`/tmp/wta-t36/recovery-plan-review.md` 与 `/tmp/wta-t36/recovery-by-id-sql-draft.md` 只是**未经授权、未执行**的历史 IN_APP 只读清单／指定 ID 操作稿，绝非生产修复证据。后者的五字段 SQL 归一化判定仍未与 Java 的 JSON 解析后列表语义作隔离对照；C1 的 JSON 比较/长度问题已由 Lead 标为待修，当前草稿必须暂停演练，待 C2 的最终语义和等价查询明确后再复核。它只覆盖 UNKNOWN+缺关系+单 WAITING_RECEIPT 的窄分支；`DELIVERED` 缺关系明确留给另批批准，不得宣称全部历史数据已修。T-36 合同要求清单与操作稿，并不授权实际修复，因此排除该分支不是 C1 三项失败的原因。

低风险观察：persist 对已有消息比较 title/content/path/noticeType/channelsJson，未比较 category/type/source/summary；若历史同主键行这些非快照字段受损，可能沿用旧行。当前无可触达样例；放入存量清单人工判定，不以理论碰撞扩大本候选阻断。
