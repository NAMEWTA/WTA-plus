# T-36 草稿功能审查（仅供实施中反馈，非最终 PASS）

- 合同：当前 change 的 AC-036、`ticket/36-atomic-in-app-delivery.md`，固定产品基线 `5118051403de0648540646d98536d8c4f3f9f26d`。
- 主审输入：`/tmp/wta-t36/draft-functional-snapshot/manifest.json` 的 17 个逐文件 SHA256；复制时逐文件源哈希与副本相等。工作树仍由产品 writer 修改，故不把不同时间的文件混作同一候选。
- 方法：只读源码和合同；未运行 Maven、MySQL/Redis、服务或测试。此报告不能用作完成/验收证据。

## 快照中可确认的设计路径

`DispatchNotificationService.java:66-89,184-210` 把 IN_APP 从 SMS/MAIL 外部 I/O 路径分开；参数和端口预检后，先 `beginInAppAttempt`，再 `completeInApp`，后者异常不被改写为供应商 `UNKNOWN`。`NotifyDispatchResultUseCase.java:25-34` 两步均由动态数据源事务代理调用。预算先提交，消息、关系、Delivery、Attempt、Outbox、Intent 在第二个短事务中提交；失效租约或重入由 `NotifyDispatchResultService.java:26-42,61-105` 的 Intent→Outbox→Delivery 锁序、owner/token/数据库时间及预留标志阻止。`NotifyOutboxMapper.xml:14-35,47-55` 在新 claim 清旧标志，预留次数并于 finish 再查有效租约。实时事件仅携带 message/user ID，`InAppCommittedPushListener.java:22-28` 是 AFTER_COMMIT，失败只写安全类别日志；`InAppNotificationService.java:80-87` 重读已提交本人关系。

旧 MAIL 回执、结果推进和 SMS 幂等异常分支没有因 IN_APP 拆分而发生明显逻辑改写；外部渠道仍走原 `complete` 路径。MAIL `NotifyValidationException` 被映射为 `UNKNOWN/DISPATCH_ERROR`（`DispatchNotificationService.java:143-152`）是基线既有行为，不应误记为 T-36 新回归；其合同归属需另行判断。

## 快照发现与实施中修复

1. **快照时的确定写入数漏检，后来已见修复。** 快照 `InAppNotificationService.java:53,70-71` 忽略消息和关系 insert 返回值；若插入返回 0 而不抛异常，结果可进入 DELIVERED 且事件可能已登记。再次读取工作树时，两处已改为 `!=1` 抛错；当前该文件 SHA256 为 `9f5962d07d94e297eaab0658e59041fa6a160a04ae67b2785f672fe057326554`。固定提交审查须重核，不将旧快照缺陷当成当前未修复事实。
2. **快照时的 noticeType 规范漂移，后来已见修复。** 基线 `DispatchNotificationService` 用 `String.valueOf(params.getOrDefault("noticeType", ""))`；快照 `:199-201` 在字段缺失时写 `null`，而 `InAppNotificationService.java:54-59` 严格比较已有消息快照。同一意图先有旧版空串消息、后有新版收件人时会拒绝关系写入并消耗预算。再次读取工作树时缺省已恢复为空串；当前分发器 SHA256 为 `b771a6aac6319d0c2ba53a466f5aa65ba1c7a6d92dd190cdf9b8fbc29ae47e56`。正式审查重核同 intent 跨版本事实。
3. **快照时通用 `complete` 可绕开 IN_APP 关系持久化，后来已见修复。** 快照 `NotifyDispatchResultService.java:114-123` 没有渠道保护，内部调用者理论上可写 DELIVERED 而无消息关系。后来工作树加 IN_APP 保护，仅允许确定本地参数失败走通用 `complete`；当前 SHA256 为 `f9198d9525ef2eea27faa24e5e969ae08c20fa38b75e2b2658444c812fe8fd09`。需在固定提交核对旧 callback/record/monitor 消费者及测试已适配，不用旧快照判阻断。

## 固定候选仍需验证的边界

- 快照的 `NotifyAtomicResultIntegrationTest.java:307-320` 虽有双 delivery 并发结果，但调用的是通用 `results.complete`，未执行两个 IN_APP 收件箱持久化，也没有关系断言。后来工作树增加 `concurrentDifferentRecipientsShareOneMessageAndHaveDistinctInboxRelations`（当前 `:249-276`），用两个物理连接、两用户、一消息、两关系及重复任务断言；测试当前 SHA256 `02305cc3dff0e33e7b8159c62735cde529ed9602181f9e5ad54a7a03c1bad0c5`，尚无本轴运行证据。
- 后来工作树增加六个表阶段故障触发器及回滚断言（当前集成测试 `:216-235`），覆盖消息、关系和结果主要写入阶段。AC-036 明写提交故障；现可见 `commitDisconnectOrLostAcknowledgement...`（`:684-697`）是**外部回执**路径，未证明 IN_APP 消息、关系、结果在提交前断连和提交后确认丢失时的原子事实与可重试结果。固定候选要确认另有 IN_APP commit 注入，否则属于验收覆盖缺口。
- `InAppNotificationService.java:54-59` 仅在 message 已存在时校验消息级快照，recipient 通过唯一关系和 Intent 行锁防重。固定候选真实测试应证明两个收件人共同快照、重复任务不增加关系或事件；不能只以单收件人成功代替。
- `DispatchNotificationService.java:189-205` 对确定的 JSON/用户参数错误给本地 FAILED，不进入 WAITING_RECEIPT；`completeInApp` 的 SQL/事务异常原样传播供有界重领。固定候选应覆盖格式错误、port 缺失和异常回滚后的最大尝试次数，避免“失败了”但既非终结亦非有界重试。
- 同租约并发与过期 finish 由锁/fence 和预留标志防护；当前测试已有旧 worker、重领和过期案例，需确认真实 IN_APP 的 `attempt_count` 语义（预算计已开始、Delivery/Attempt 只计已提交）及最终 dead-letter 在失败跨事务后仍一致。
- `InAppCommittedPushListener.java:22-28` 的事件契约需真实运行证明回滚时零事件、提交后推送失败不回滚/重复落库。当前测试新增 `realtimeCalls` 断言，但尚未由本轴运行；`NotifyAtomicResultIntegrationTest.java:148-159` 使用真正的 `DsTxEventListenerFactory`，因此可在 owned MySQL 上验，不应以普通 Spring `@TransactionalEventListener` 替代。
- SMS/MAIL 在本次拆分后应跑原通知同域测试和真实选择器，尤其旧 callback 的状态单调、mail/sms 一次 I/O、租约过期与聚合；静态 diff 不能替代这些回归。

以上均是实施中快照审查。后续源码还在变化，正式 verdict 只针对 Lead 另给的固定 base/head、clean tree 和真实验收证据。
