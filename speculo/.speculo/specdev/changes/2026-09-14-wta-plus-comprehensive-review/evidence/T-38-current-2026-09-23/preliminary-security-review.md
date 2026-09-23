# T-38 冻结副本安全预审（非正式候选）

输入：base `b47ff8b91cfef02a9f28de0e201edcd1575a950f`；Lead 2026-09-23T12:31:23Z 捕获的 `/tmp/wta-t38/preliminary-source/`。`manifest.json` 的 21 个文件 SHA-256 重新核对为 **21/21 一致、0 mismatch**；未把正在开发的工作树当固定输入。未运行 Maven、服务或测试，未写仓库。其余未冻结调用链按 `git show 9f8fa2b1e36c851be8cee6763a84ebf6e2072928` 查看。此报告不能替代最终提交的正式审查或 Lead 验收。

## 源码安全结论

本冻结副本**未发现已证的新增生产安全阻断**；以下是应在最终候选重核的事实，不等于测试通过。

- URL/body 与权限：冻结 `NotificationController.java:42–70` 保留 retry/cancel 独立 `@SaCheckPermission` 和业务 `@Log`，body 含不同 notificationId 时在调用服务前拒绝；一致或空 body 都以路径 ID 重建命令，取消回执由该命令生成。固定全局 `SecurityConfig.java:84–112,161–178` 对普通会话检查登录、ClientID 与 token 一致及 access_path。`NotificationRetryControllerContractTest.java:23–58` 覆盖两种冲突和空 body；它使用 standalone MockMvc，**没有装 SaInterceptor**，不能充当越权 HTTP 实测。权限代码本轮未改；最终需引用既有真实鉴权证据或补带拦截器的负向用例。
- 归属/锁序：冻结 `NotificationApplicationRuntimeService.java:177–205` 先锁 Intent；指定 delivery 的普通读只用于归属预检，随后先锁该 Intent 的所有目标 Outbox、再锁 Delivery 并复验 `intentId`。`NotifyNotificationDao.java:47–70` 的锁定查询按主键排序。`NotificationApplicationUseCase.java:34–38` 在动态事务代理内执行 retry。业务 `appId` 仍非登录 Client owner，不应新增猜测的 appId=ClientID 规则。
- 批量 UNKNOWN 与取消：`RuntimeService.java:189–233` 对 CANCELLED/EXPIRED/DELIVERED 聚合返回原状态和 0；对目标范围内外部 UNKNOWN 在构造任何可写候选之前抛错。安全预检结束后才在 237–252 行写入，因此通知级混合“本地可重试 FAILED + 外部 UNKNOWN”按源码应整批拒绝且零写，但当前新真实测试没有该组合。指定 foreign delivery 在 183–187、200–205 行拒绝；无任务返回持久 Intent 原状态和 `queuedCount=0`。
- FAILED 仅证实未外呼者：`RuntimeService.java:40–44,255–261` 使用固定 exact-code allowlist，不接受泛 FAILED/旧 PROVIDER_ERROR/UNKNOWN。`UNBOUND_CHANNEL`、账号/模板/收件人 QUOTA、`MISSING_VARIABLE`、短信模板参数缺失等在固定 `NotifySendPlanner.java:72–150` 的 `plan` 阶段返回，`DispatchNotificationService.java:99–118` 只在 `plan.ok()` 后进入 `NotifyClient.send`；IN_APP `LOCAL_DISPATCH_ERROR` 在同文件 174–194 行的本地快照/端口预检失败时、预算预留与持久化前生成。冻结代码还要求唯一 Outbox、无 providerMessageId、Delivery/Outbox 错误码一致、无 lease、预算未耗尽（`RuntimeService.java:216–231`）；不允许按 DEAD_LETTER 或提供商文案重发。此 allowlist 没有凭自身状态对“发送后未知”授予权限。
- Outbox/CAS：`RuntimeService.java:237–250` 先 CAS 原 Outbox 再 CAS Delivery，任一影响行数不是 1 即抛异常回滚事务；`NotifyOutboxMapper.xml:56–65` 按 outbox/intent/delivery ID、旧状态/错误码、无租约、剩余预算更新，只允许 DONE 或 WAITING_RECEIPT，且**不重置 attempt_count**；`NotifyNotificationDao.java:152–161,197–200` Delivery CAS 含 intent、状态、错误码、无 providerMessageId。重复请求首轮使 Delivery=PENDING/Outbox=READY，下一次不再合格；活 PROCESSING 租约不被抢占。没有新建第二个 Outbox 的分支。
- IN_APP UNKNOWN：`RuntimeService.java:229–232,264–269` 仅固定 `DISPATCH_ERROR` 且 WAITING_RECEIPT 时允许，并检查原 message/本人 recipient；若 recipient 存在但 message 不存在即拒绝。固定 T-36 `InAppNotificationService.java:33–73` 按 intent ID 复用 message、按 `(messageId,userId)` 跳过已有关系，只为新关系发布 AFTER_COMMIT 提示事件；`NotifyDispatchResultService.java:94–105` 在结果短事务内持久化关系和投递结果。冻结 `NotifyManualRetryIntegrationTest.java:200–218` 覆盖已有 message+recipient 的重放，确认一份关系且 `realtimeCalls=0`；未覆盖关系缺失时新建一次及重复重试无重复 push，也未覆盖 orphan recipient 被拒。

## 需补的验证（测试缺口，预审 P2）

1. **通知级外部 UNKNOWN 全批拒绝：** `NotifyManualRetryIntegrationTest.java:127–144` 只有指定 UNKNOWN 的单目标负例；增加同 Intent 下一个安全 FAILED 和一个外部 UNKNOWN，调用 `deliveryId=null`，断言异常、两行/Outbox/聚合均未变、零 wake。该路径的源码顺序是对的，但这是 AC-038 明确的高风险组合。
2. **IN_APP 缺关系与提示幂等：** 现 `NotifyManualRetryIntegrationTest.java:200–218` 仅已有关系且零 push。补 message 存在但本人关系缺失、message/关系均缺失、orphan 关系三种有界场景；前两者首次产生同主键的一份 message/关系且恰好一次提交后提示，重复操作零新增，orphan 拒绝。对含真实端口和事件的事务回滚后，关系与提示都不外泄。
3. **并发确实重叠：** `NotifyManualRetryIntegrationTest.java:180–198` 是真实隔离 MySQL + 两线程同起点，合计 queuedCount=1/一份 Outbox；它并未保证第二线程在第一线程持有 Intent 行锁期间开始等待，因此单纯顺序运行也会通过。可用有界测试锁闩或第二数据库连接持锁验证阻塞/释放后的唯一结果。当前测试不是“空正例”，但竞争强度尚未严格证明。
4. **真实 rollback 已覆盖一处：** 同文件 165–178 行用数据库 BEFORE UPDATE trigger 让第二个 CAS 失败，断言先执行的 Outbox requeue 回滚、Intent 不变且无 wake；这是实际 DB 事务负例，不应误报成 mock。它发生在事件发布前，故 `wakes=0` 不单独证明 AFTER_COMMIT 事件在发布后回滚的语义；若该语义由旧测试证明，最终证据可引用旧测试。Controller 契约测试不证明 HTTP permission，有独立来源才算闭合。

次要 UX 限制：冻结 `NotificationPage.vue:125–140` 对任意 FAILED 显示“重试此投递”，后端对不在安全 allowlist 的行返回 0 和现状；用户可见按钮会略宽于可执行集合，但服务端仍 fail closed，不构成安全阻断。若要隐藏，应以后端安全资格投影为准，不能把前端错误码推断当授权。

正式候选核查：重新固定 final SHA/tree 和三点 diff；复核上述锁序、CAS 影响行数/动态事务、真实 MySQL 测试实际 enabled 且零 skip、前端消费者/生成合同，以及是否出现新的错误码/渠道未发送来源。本报告未声称任何门禁通过。
