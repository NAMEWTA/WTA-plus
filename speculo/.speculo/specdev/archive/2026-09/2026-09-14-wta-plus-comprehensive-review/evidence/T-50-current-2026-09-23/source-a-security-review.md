# T-50 Source A 固定源码安全/事务审查

固定 base `d544f02e1627d76883e9eeaf73a8f2b05002d079`，candidate `9bb2e2888b5d2b0563beb7164c8c0a2165547e2c`；审查 `git diff base...candidate` 与 `git show candidate:path`。此为 Source A 后端候选，OpenAPI 捕获/生成物属于后续 B。本代理只读，未运行 Maven、Docker、HTTP 或测试。初审时 Lead 真实验收未返回；下方保留原静态判断并追加 A1 实测更正。

## 结论

**静态安全/事务轴原判断：PASS；A1 真实验收：FAIL，整体验收不得接受。** 静态检查未发现新代码把不支持策略/模式/优先级持久化、让历史外部未知任务自动重发、把已发送事实伪造成未发送，或绕开结果短事务/活租约的确定缺陷。但真实 HTTP 默认字段请求失败，详见末节；修复并重验前 AC-050 不闭合。生产存量只读清单/处置仅有稿件，未执行目标环境盘点或变更。

## 固定源码核查

1. **新提交和人工重试拒绝**：`NotificationApplicationRuntimeService.java:52-60,309-359` 在 `findDuplicate`、目标解析、插入之前调用 `validate`，只接受 `ALL/ASYNC/0`；非法相同幂等键不会返回旧回执。`NotificationCommand.java:27-40` 的 null 策略/模式仍规范为 ALL/ASYNC，primitive priority 默认 0；Java `NotificationMode` 没有为测试虚增 SYNC 值，HTTP 字符串 SYNC 应由绑定层拒绝。`retry()` 在 Intent 锁与指定 Delivery 归属检查后、扫描候选/重排之前 `requireSupportedMode`（`:178-209`）；旧不支持 Intent 不会借既有 `FAILED` 本地码重新排队。`query/cancel` 未新增该拒绝，旧控制面可读/可取消。错误是 `ServiceException`，不是成功的零排队回执。
2. **Worker 入口与事实顺序**：`DispatchNotificationService.java:69-94,108-124` 在 route WAIT、IN_APP persist、外部 Provider 前走 `resultPort.deadlineGate`，外部发送前再走一次。`NotifyDispatchResultService.java:27-42,61-125` 在 `@DSTransactional` 代理 `NotifyDispatchResultUseCase.java:21-23` 下按 Intent→Outbox→Delivery 锁序核 owner/token/租约、锁后 DB 时钟；非 PENDING 结果、重领 PROCESSING 外部未知、CANCELLED/终态 Intent、到期事实均先于新 unsupported 分支。支持性判断处于 schedule WAIT 之前，因此旧未支持未来任务不再每 30 秒回 READY。重领外部走 `UNKNOWN/WAITING_RECEIPT`，不调用 Provider。
3. **只有合取才证明外部未发**：`NotifyDispatchResultService.java:127-155` 要求本次 `claimedFromReady=true`、持久 `DEADLINE_UNSENT_READY`、两处尝试数均 0、providerMessageId/acceptedAt/deliveredAt 均空。缺证明则固定 `UNSUPPORTED_MODE_OUTCOME_UNKNOWN` 并 `UNKNOWN/WAITING_RECEIPT`，未把 READY/零次数单项当来源证明。此码不在 `NotificationApplicationRuntimeService.safeLocalFailure` 的重试白名单（`:265-272`）。原 T-39 截止分支仍优先，明确过期且未发为 `NOTIFICATION_EXPIRED`；进入 Provider 后的受理/未知不能被该分支覆盖。
4. **IN_APP 与混合事实**：`NotifyDispatchResultService.java:143-215` 对旧不支持 IN_APP 先用同 Intent+本人关系核消息：两者存在恢复 `DELIVERED/DONE`、关系孤儿标本地不一致、无关系才 `UNSUPPORTED_MODE_UNSENT`。该分支不调用 `beginInAppAttempt`、`port.persist` 或 Provider，不制造 Attempt、不重置已预留预算；已有 ACCEPTED/DELIVERED/UNKNOWN Delivery 先由非 PENDING guard 保留。`refreshAggregate` 只按所有 Delivery 重算，不给混合 Intent 强设 FAILED。`finish()` 要求影响恰一行；Delivery、Outbox、聚合任一步失败依既有动态短事务回滚，未新增外部副作用于事务内。
5. **仓内消费者**：固定 diff 中 10 个生产文件、12 处 `new NotificationCommand` 仅将 priority 改 0，策略/模式仍 ALL/ASYNC，截止字段、幂等键、收件人/模板参数未在这些调用中改变；Captcha 与 Enterprise 的原绝对期限保留，PersonRebind 仍按既有非验证码业务期限制定。公共 API Javadoc/通知规范准确收窄承诺，旧策略枚举保留读取用途。

## 已写测试的覆盖与运行待证

- 新 `NotifySupportedModeIntegrationTest.java` 为 opt-in（`notify.supported.mode.integration=true`），源码覆盖旧 ORDERED_FALLBACK/ESCALATION WAIT、SYNC/非零 priority、markerless READY、重领 PROCESSING、IN_APP 本人关系/孤儿/无关系、旧 retry、提交查重前拒绝、带权限的 loopback HTTP、旧 owner/token/过期租约、混合 ACCEPTED/UNKNOWN、截止优先、末尾聚合 SQL trigger 失败整事务回滚；它复用 `NotifyAtomicResultIntegrationTest` 的真实表/claim/DAO 接缝而非仅 mock 状态。HTTP 正负场景检查 Intent/Recipient/Delivery/Outbox/Attempt 五表数量，独立 400 绑定测试检查 SYNC 字符串。`NotificationSupportedModeRuntimeTest` 补纯运行时入口测试；受影响调用方及原截止测试均有修订。是否真实运行 0 failure/error/skip、权限拦截是否按预期加载、owned 清理是否完成，**待 Lead 结果**。
- 待验关键反例是数据库聚合尾部失败后 Delivery=PENDING、Outbox=PROCESSING 且原 lease token 保留、Attempt=0；若这条测试未实际执行或失败，本静态 PASS 不代表事务合同已验。T-39 原有 Atomic/Wake/截止与 T-38 手工重试回归也应由 Lead 的八类精确选集确认，不能只看新类绿灯。
- `evidence/T-50-legacy-mode-disposition.md` 提供只读汇总/逐 ID 查询模板，并明确静态行无法重构 `claimedFromReady`、不批量 UPDATE、不重放六 SQL、先停旧 Worker、按精确 ID 另行批准。它不是生产存量数量或修复完成证据；无 Outbox/不可领取/WAITING_RECEIPT 仍须人工核对，不能宣称上线即清空全部旧数据。

## 限制及后续验收

本轮没有执行任何构建/服务/测试，当前仅可判静态源无阻断；八类真实运行、后端/跨模块生产消费者、模块分层/Skill facts、Source B 的 OpenAPI fetch/generate/check、全量门禁、clean exact HEAD 和目标环境旧数据盘点均由 Lead 后续证据决定。真实运行若暴露失败，应保留 Source A 与失败日志后定点修复并另做固定 SHA 复审，不回写本静态结论。

## A1 真实运行补记（Lead 提供，原静态结论保留）

`/tmp/wta-t50/runs/35e14468040352c5/result.json` 与 fresh XML：135 项、**1 failure / 0 error / 0 skip，命令 exit 1**；134 项通过。唯一失败 `NotifySupportedModeIntegrationTest.loopbackHttpEnforcesSubmitPermissionAndSupportedValuesBeforeAnyWrite` 的 `:308-309`：第二个 HTTP 请求省略 strategy/mode/priority，预期业务 code 200，实际 400。source before/after 均 clean `9bb2e2888b5d2b0563beb7164c8c0a2165547e2c`、tree `071f854346faa03d2b0b1ceab5ba13f218fe59c7`；owned Maven 进程组无成员、容器/匿名卷/loopback 端口归零，cleanup errors 为空。故 **A1 整体验收失败，不可将上文静态 PASS 当作 candidate acceptance**。八类中其余项通过的计数由 Lead 的结果提供，本代理未执行服务。

固定源码追因：`NotificationController.java:29` 用 `@Valid @RequestBody NotificationCommand`；`NotificationCommand.java:27-40` 无 Bean Validation 字段约束，compact constructor 对 null strategy/mode 归一 ALL/ASYNC，`priority` 为 primitive int，JSON 缺省时应为 0。测试 `commandJson(null,null,null)` 在 `:401-409` 只省略这三个字段，首个显式 ALL/ASYNC/0 请求已 200。运行日志记录 `GlobalExceptionHandler` 收到 `HttpMessageNotReadableException` 并返回 400（该 handler `:265-269`），因此失败发生在 HTTP JSON 解析/record 构造边界，尚未到 `NotificationApplicationRuntimeService.validate`；`@Valid` 本身不是已证原因。日志的安全摘要没有打印 Jackson 原始 cause，具体哪一个缺失 creator 属性触发解析失败尚未证明。建议 writer 在 owned 合成 fixture 中捕获并脱敏根因，针对当前 Jackson/record 请求绑定明确缺省合同，保留缺省 ALL/ASYNC/0 的 HTTP 正例及五表零异常写断言；不能通过把预期改成 400 来“修绿”，也不应削弱非法旧策略/模式/非零优先级拒绝。修后新固定 SHA 重跑这条及八类相关验收。

### A1 根因复核更正（保留上段初始判断作为历史）

上述“JSON 缺省 primitive 应为 0、须扩 HTTP 省略 priority 正例”的推断**已被本地 Jackson 3.1.4 源码推翻**，不再作为产品阻断。`/root/.m2/repository/tools/jackson/core/jackson-databind/3.1.4/jackson-databind-3.1.4-sources.jar` 中 `DeserializationFeature.java:154-161` 明确 `FAIL_ON_NULL_FOR_PRIMITIVES(true)`（3.0 起默认）；`PropertyValueBuffer.java:295-305` 对缺失 creator 属性求 `getAbsentValue`，`ValueDeserializer.java:376-385` 默认把它交给 `getNullValue`，`NumberDeserializers.java:163-172` 遇 primitive 且该特性开启即报错。因此 HTTP 省略 `int priority` 在进入 record compact constructor 前解析失败，与 `@Valid` 或 runtime supported-mode 校验无关；strategy/mode 的 null 归一仍可在显式 `priority=0` 时运行。

Ticket revision169 只要求“构造器既有 null 归一保留”、支持唯一 `ALL/ASYNC/priority0`，未承诺 HTTP 可省略 primitive priority。当前未提交测试 diff 仅改 `NotifySupportedModeIntegrationTest.java:308`：省略 enum、**显式 priority 0** 期望 200；再于 `:330-333` 对省略 priority 期望 HTTP 400 且五表行数不增。未改生产合同或 Jackson 全局配置，独立静态复核**无反证、接受该测试修正方向**。A1 的 135 项/1 fail 真实历史仍然有效，新测试能否全绿必须由 Lead 在修订固定 SHA 的同样八类真实验收确认；不能用这份静态判断覆写 A1 原始结果。
