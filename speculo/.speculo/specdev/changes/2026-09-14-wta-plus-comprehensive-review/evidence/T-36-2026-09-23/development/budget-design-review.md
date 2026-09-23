# T-36 revision155 IN_APP 预算设计风险预审

基准：base `5118051403de0648540646d98536d8c4f3f9f26d` 的 Outbox/DAO/结果事务源码，当前 T-36 ticket/map revision155（仅设计，非最终产品 source 审查）。只读；未运行测试、服务或数据库。目标是“自动调度至多 `max_attempts` 次进入 IN_APP persist”；已授权的人工重试另见下文边界。

## 结论

独立短 `@DSTransactional beginInAppAttempt` 先持久消耗预算、后进入原子消息/关系/结果事务，能覆盖后者回滚或进程崩溃造成的无界重领。复用 `notify_outbox.attempt_count` 与固定 `last_error_code=IN_APP_ATTEMPT_RESERVED` 是无新增表/列的最小方案，**以下约束落实前不能宣称有界**。现有 `NotifyOutboxClaimService.claim` 不增次数，原 `NotifyDispatchResultService.complete` 在结果事务内自增（`NotifyDispatchResultService.java:88-95`），事务失败可回滚计数；修订方向正确。

## 必须同时成立的状态转换

1. 只有 IN_APP、确定性参数预检已通过且拥有当前租约，才允许走 begin。begin 必须经 Spring 动态事务代理独立提交，锁序 Intent→Outbox→Delivery；锁后复核 intent/delivery/outbox 三 ID、`delivery.channel='IN_APP'`、`delivery.status='PENDING'`、`outbox.status='PROCESSING'`、owner/token 完全匹配、`lease_until>数据库 UTC`。只信锁后当前行，不信 claim 返回的旧对象或预检快照。任何 DB/提交异常或 ACK 未知均不返回“获预算”，且不得调用 persist。
2. **先查同租约 RESERVED，再查上限。** 若当前有效 token 已有 RESERVED，返回“本 token 已开始/跳过”，不再次 persist、不再次加计数、更不能因 `attempt_count==max_attempts` 抢先把首次在途调用 DEAD_LETTER。若未 RESERVED 且 `attempt_count<max_attempts`，同一事务原子 `attempt_count+1`、设置固定标记并断言恰好一行；成功提交才把“可以进入 persist”返回。计数采用数据库当前值/条件 CAS，防止 Java 旧快照覆盖。
3. 若**新 token** 未 RESERVED 且计数已到上限，在相同锁/fence 事务内将该 IN_APP delivery FAILED、该 outbox DEAD_LETTER、重算 intent 聚合，绝不持久化消息/关系；每个必要 DML 行数必须核对。已预留当前 token 即使等于上限也不能在第二次调用时走耗尽分支。
4. 消息/关系/结果事务不再给 IN_APP outbox 加一次计数，完成失败后的 READY/DEAD_LETTER 判断使用 begin 后锁定的持久计数；Delivery.attempt_count/NotifyAttempt 仍仅记录已提交结果，与 Outbox“已经开始的物理尝试”刻意不相等。SMS/MAIL 路径继续现有结果事务计数语义。若预算事务已提交、消息事务回滚/崩溃，Outbox 保留 PROCESSING+RESERVED+已消耗次数；本 token 不能再次进入 persist，租约过期后新 token 可继续（若仍有额度）。
5. 新 token 的 claim 成功必须在**同一条 claim UPDATE/事务**清除仅这个固定 marker，保留所有其他错误码；`NotifyOutboxClaimService.claim` 返回的 `NotifyOutbox` 候选对象原先是清 marker 前的查询结果，后续 begin 必须重读锁定行。若清理发生在 claim 后另一事务，中间可能把新 token 错判为同租约已预留。
6. 结果事务必须在消息/关系写入之前取得同序锁并复核当前 fence；失效 owner/token/过期 lease 零写。一个持有旧 token 的消息事务与新 claim 竞争时，共用 outbox 行锁；旧事务先完成可提交，新 claim 先胜则旧事务看到不匹配而不得写消息/关系/结果。实时事件只能随已提交的结果事务 AFTER_COMMIT 发布。

## 故障及观测边界

- begin 的 DB 写失败/事务提交 ACK 丢失：调用者中止，不尝试由异常推算“没扣预算”。若提交实际成功，标记+计数留存；租约过期后新 claim 清标记但不减计数。完全不可写期间无法保证立即写入终态，只能保证未取得持久预算就不进入 persist。
- 消息事务提交 ACK 丢失：若事实已经提交，Outbox DONE/Delivery DELIVERED/唯一关系是主库后验；不可读取时等待 lease 安全到期，不能因为异常按旧 token 重新 persist。即使重领，新原子结果路径需检查已完成状态并无操作。
- 同租约重复调用：先看到 RESERVED 者只能跳过；若第一个仍在消息事务且已占尽额度，第二个也不能抢先终结。必须测屏障并发与“预留后、persist 前崩溃”。
- `last_error_code` 被临时复用为“预留状态”，在 PROCESSING 期间不再总是错误码。仓内查到此字段当前仅 Outbox 实体/DAO/结果写回读写，未发现管理 VO 直接暴露，但数据库运维读者仍需注明语义。begin 设 marker 时宜把 last_error_message 置空，避免一组不匹配的旧错误文本。若严格承诺“其他渠道原错误码一概保留”，claim 的清码条件还需限定 IN_APP delivery；否则外部 provider 恰返回相同字面码时也会被清掉。最小可行做法是 claim UPDATE 中对 delivery.channel 加条件，或明确将这个内部码保留为全局不可由 provider 输入覆盖的命名空间。
- 旧数据的 `attempt_count` 只含已提交结果，不代表以往所有物理 persist；新方案无法追溯旧版本未计入的调用。切换前需停旧 Worker/避免新旧二进制混跑；从新版本开始才可主张上限。
- 现有管理 `NotificationApplicationRuntimeService.retry():163-198` 对整个 intent 的 FAILED/UNKNOWN 进行手工重试，`NotifyOutboxMapper.xml:44-52` 的 requeue 把 `attempt_count` 清零，找不到旧 WAITING/DEAD_LETTER 时还会新建零计数 Outbox。故“`max_attempts` 覆盖全生命周期/含人工重试”在当前代码中不成立。可把人工、授权、逐次审阅的重试明确定义为**新预算周期**；若 AC 要求绝对物理上限，必须同步收紧现有 retry/reset 路径（且其按 intent 全渠道重排风险已在恢复报告列出），不能仅改 begin。
- 现有 Outbox DDL `attempt_count int NOT NULL DEFAULT 0`、`max_attempts int NOT NULL DEFAULT 5`。begin 应拒绝负数/超大等异常值，`attempt_count>=max_attempts` 时终结，避免整数溢出。耗尽终态不是一次 NotifyAttempt，因为没有进入 persist；必须在证据中解释 Delivery/Attempt 与 Outbox 次数的差异。

## 最小验证断言

隔离真实 MySQL、双连接与受控屏障：同 token 双 begin（含恰好最后一格）只有一个 STARTED/一次 persist；新 token 的 claim 同行清 marker、保留 count；旧 token 在过期/新 claim 后 begin 或原子结果零消息写；预算事务 ACK 丢失时本次不进 persist、后验 count 至多加一；结果事务六类故障/ACK 丢失后重领总物理进入次数不超过 max；达到上限只终结，不留新消息/关系；结果提交成功则不再重复；SMS/MAIL errorCode/count 和管理手动 retry 语义单独断言。包括 claim 返回对象仍带旧 marker 的反例、外部渠道同字面错误码保留、消息事务 rollback 留预算。

可接受此设计作为实施方向，前提是实现/测试覆盖上述不变量；目前没有看最终 diff，不能给产品通过结论。
