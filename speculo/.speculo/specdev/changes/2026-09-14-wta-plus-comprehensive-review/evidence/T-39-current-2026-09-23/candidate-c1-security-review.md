# T-39 C1 固定候选：安全与事务轴审查

## 固定输入与结论

- base `bec94ae442966864a74ff52ffb3e4e305a37441d`，red `162d405642d88825f59b23bb9a61d4063ac2aaa4`，candidate `c2ab3d63225d2d1406aa9ecc9c072fecc83da297`，tree `5be3152f5fbf11e3bc12e3fd3620f2d97f96894c`。仅用 `git show`/`git diff base...candidate` 审固定源码，没有用变化中的工作树判断候选。`git diff --check base...candidate` 退出 0。
- **静态安全/事务轴：未发现要求修改 T-39 生产实现的阻断项。** C1 **不能验收通过**：Lead 的隔离真实八类运行 `runs/92c675af47abf448/result.json` 记录 Maven/driver exit 1，Enterprise 7 项中 1 failure、Atomic 66 项中 2 failures。该运行的源码前后均为本 candidate clean，owned MySQL/Redis/匿名卷/端口/进程组清理为零遗留。修正夹具后须固定新 SHA 并完整复验；本报告不是运行门禁结论。
- 审查范围限 T-39 安全与事务合同、相关消费者失败原因。未运行 Maven、Docker、服务、测试，也未写产品或 SpecDev。依据 Ticket 39 revision 167、dispatch、legacy disposition、工程规范及 code-review Skill/source-discovery/Fowler/risk/reviewer-contracts。

## 源码判定

1. **外部发送事实与崩溃恢复。** 新外部 Outbox 创建时才写 `DEADLINE_UNSENT_READY`，IN_APP 不写；见 `NotificationApplicationRuntimeService.java:137-149`。`NotifyOutboxClaimService.java:27-42` 在成功 claim 后把领取前 READY/PROCESSING 放入本次内存对象；`NotifyOutbox.java:37-40` 禁止该字段持久化/JSON 输出。Dispatch reload 后保留本次来源 `DispatchNotificationService.java:69-82`。结果端 `NotifyDispatchResultService.java:69-80` 先对外部重领 PROCESSING 记 UNKNOWN/WAITING_RECEIPT，覆盖未到期 WAIT 路径；旧无 marker READY/0 到期也不能被误当确定未发送。只有本次 READY、固定 marker、Outbox/Delivery attempts 均 0 且无 providerMessageId/acceptedAt/deliveredAt 才把到期外部 PENDING 结算 FAILED；见 `:93-135`。该门槛不会抹掉旧 Provider I/O 事实。部署仍需先停旧 Worker、后起新 Worker；代码测试不能证明已完成此发布动作。
2. **截止与事务 fence。** `DispatchNotificationService.java:80-124` 在路由前及 `notifyClient.send` 前经结果端再次检查，`deadlineGateInProgress` 防止检查异常被普通发送异常分类吞掉；Provider 已进入之后不以过期否定真实结果，`NotifyDeadlineIntegrationTest.java:248-271` 对 ACCEPTED 后跨截止有断言。结果端 `NotifyDispatchResultService.java:26-42,60-71,122-156,328-329` 使用 Intent→Outbox→Delivery 锁序、owner/token/有效租约与锁后 DB 时钟；`NotifyDispatchResultUseCase.java:17-45` 将 gate、站内预算/持久化、结果和 settle 放在短事务代理中，行数不符抛异常回滚。取消的 Intent 终态不由聚合重算复活 `NotifyDispatchResultService.java:81-91`；WAIT 再进同一 gate `:296-312`。提供者前仍有 gate 返回与 I/O 开始之间的微小时间窗，这是本票“发送前重检”方案的固有限制，并非声称分布式原子截止。
3. **IN_APP 持久事实与预算。** 已存在本人 recipient 关系及同 Intent message 才恢复 DELIVERED；孤儿关系转明确本地 FAILED；不存在事实的过期项转 `NOTIFICATION_EXPIRED`，不创建 Attempt/消息/关系/推送 `NotifyDispatchResultService.java:93-135`。`beginInAppAttempt` 在独立短事务中先 gate 再预留、保留同 lease marker 和上限 `:178-201`；`completeInApp` 再 gate 后于一个事务内 `persist` + result，失效 lease 抛异常回滚 `:212-225`。真实新测试覆盖三类到期关系与无预算消耗 `NotifyDeadlineIntegrationTest.java:135-174`。源码没有给已预留预算重新置零。
4. **提交/人工重试。** `NotificationApplicationRuntimeService.java:52-95,137-149,178-261,337-345,422-435` 将计划向上、截止向下归整到秒，校验 `dbNow >= expiresAt`，收件人/配置准备后再次拒绝过期；重复提交及 duplicate-key 分支检查原持久截止，不以新命令续期；retry 锁原 Intent 后先查其原截止，不重新排队过期任务。可观察限制是新 Intent 首次检查后到多收件人插入/commit 仍可跨截止，但 Worker 的独立 gate 会结算且不会开始已过期 Provider I/O；这不构成延长原截止。
5. **验证码与 Redis。** `CaptchaController.java:68-82,108-122` 每次一次生成整秒绝对截止，同时传入 command 和 `cacheCaptchaCode`；每个新 code 使用独立 idempotency key `:125-127`。`RedisUtils.java:189-204` 基于 Redis `TIME`，在一个 Lua 脚本中先判截止再 `SET ... PXAT`，逾期不覆盖旧 key；value 沿 bucket 实际 codec，key 沿 Redisson NameMapper 的 script API，截止 long 经精确整数界限验证后才作为十进制字面量进入脚本。`RedisUtilsDeadlineIntegrationTest.java:73-96` 针对生产 CompositeCodec/NameMapper、实际值和 PEXPIRETIME/过期不覆盖。MySQL 提交和 Redis 写入之间没有跨资源事务；Ticket 明确接受此边界，失败会明确报错，不能当作通知已撤回。

## C1 实测失败与建议

- `EnterpriseQueuedNotificationIntegrationTest.java:190-201` 的旧 fixture 在 `:198` 返回 `NotifyStatus.FAILED` 但 `deliveries=List.of()`，却在 `:200` 期待终态 FAILED。现有 `DispatchNotificationService.java:279-306` 对没有单目标、明确 `UNSENT_TERMINAL` 的外部结果保守置 UNKNOWN，故 challenge 仍 QUEUED。**不能为了绿灯放宽生产 UNKNOWN**；应在新 SHA 把测试 fixture 改为真实单目标明确终态未受理，并保留 challenge/重发断言。这是 C1 验收阻断、不是 T-39 生产安全缺陷。
- `NotifyAtomicResultIntegrationTest.java:503-544` 在第一笔成功使原 Intent 已 DELIVERED 后，直接插入第二笔 PENDING delivery/outbox (`:535-539`) 却未将 Intent 改为与该待发事实一致的非终态；T-39 结果 gate 按真实终态拒绝继续，导致 `:542` 的 AFTER_COMMIT push 断言两种 phase 均失败。应仅修合成前置为一致聚合状态，保留原“下一次正常提交有 push/关系”的断言，不移除 `NotifyDispatchResultService.java:88-91` 的终态保护。这也是 C1 验收阻断；Lead 已定位并授权 writer 处理。
- 已完成那次真实运行的其他六类计数：Deadline 15、Redis 2、Wake 1、ManualRetry 13、SMS 10、Idempotency 3，全部 0 failure/error/skip；Atomic 66 中 2 failure、Enterprise 7 中 1 failure。**总运行 exit 1，不是绿灯。** Source-before/after clean 与资源 cleanup 可从上述 `result.json` 独立核对。新 candidate 仍须重跑相同真实组合、确认八类 0 skip/0 failure/error；此外按 Lead 门禁矩阵完成适用构建/消费者检查。本轴未独立执行它们。

## 非阻断限制与交接

- 本地静态审查没有验证线上历史无截止 OTP 的数量或处置，也没有执行生产 SQL。`T-39-legacy-deadline-disposition.md:44-54` 仅是只读分类/待审批发布稿；T-30 发布前仍要隔离旧 Worker 与旧 OTP 队列、记录真实清单及裁决，不可把本票新代码当成历史数据已修复。
- Redis 与 MySQL 时钟若显著漂移，缓存写入和通知提交可能给出不同的即时成功/失败结果；代码以同一绝对 Instant 储存截止，Redis/DB 各自在其边界判断。运营需保持时钟同步。此为跨资源时钟条件，不是 C1 发现的静态逻辑阻断。
- IN_APP 检查发生于事务内 `port.persist` 前，提交本身仍可能跨墙钟截止；消息与结果同事务提交，不会形成单独半提交。若验收要求“提交时刻也必须早于截止”而非 Ticket 当前“持久化前检查”，需另立可执行合同与边界测试，不能从现有测试推断此更强保证。
