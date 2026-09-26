# T-50 只读安全与事务设计审查

## 输入与判断

固定产品提交 `6e1d7f8e9f3d67495a361634779b7f7f050e68ab`（T-39 C2）；当前 `d544f02e1627d76883e9eeaf73a8f2b05002d079` 仅治理。读取 Ticket 50、ADR-CR-015（仅 ALL/ASYNC/0）、`/tmp/wta-t50-current-audit.md` 和 `/tmp/wta-t50-implementation-outline.md`，并用固定 `git show`/`git grep` 复核通知入口、结果事务、claim 与所有生产构造。此为**实施前设计审查**，不是候选源码/门禁验收；未编辑仓库、未运行 Maven/Docker/服务/测试、未查询真实数据。

**结论：提纲的“入口前置拒绝 + 12 构造归零 + 在现有 `deadlineGateLocked` 原子处置旧任务”可在当前边界内实现，无需新表、Mapper、DAO、Port 或全局状态平台。** 下述顺序与反例是实施/正式候选的阻断判据。无当前设计不可解的安全 blocker；真实代码若偏离其中任一不变量，应请求修改。

## 必守顺序与事实

1. **新提交先拒绝再查重复。** `NotificationApplicationRuntimeService.java:52-60` 当前先 `validate` 后 `findDuplicate`，可在现有 `validate` 中仅允许 `strategy==ALL && mode==ASYNC && priority==0`，使不支持的 command（含既存相同 app/key）在任何 Intent/Delivery/Outbox 写入或旧回执返回前失败。`NotificationCommand.java:27-39` 构造器把 null strategy/mode 归一为 ALL/ASYNC，`priority` primitive 缺省 0；保留枚举旧值供已存事实读取，不凭空加入 SYNC Java 枚举。HTTP 字符串 `SYNC` 应在绑定层失败且零写。`retry()` 当前 `:178-261` 对旧 Intent 可重排局部安全 FAILED；必须在取得原 Intent、核对目标归属之后、进入 T-38 candidate/requeue 之前拒绝旧 `strategy/mode/priority` 非支持值，避免把未支持历史任务重新唤醒。拒绝应为明确业务错误，**不是返回成功的 queuedCount=0 回执**；断言 requeue/wake/预算均 0。`query/cancel` 历史控制面仍可使用。
2. **Worker 先处理不确定来源/原有终态，再处理 unsupported。** `DispatchNotificationService.java:69-93` 在 routeDecision/IN_APP persist/Provider 前调用现有 `resultPort.deadlineGate`；外部 Provider 前又调用同 gate `:108-124`，因此在 `NotifyDispatchResultService.deadlineGateLocked` 内实施可覆盖 WAIT 与最后入口。其锁序与 owner/token/lease、锁后 DB 时钟已有 `NotifyDispatchResultService.java:26-42,60-71`，代理短事务在 `NotifyDispatchResultUseCase.java:17-45`。新分支应在现有非 PENDING/重领 PROCESSING、CANCELLED/DELIVERED/EXPIRED Intent、到期分支之后，在 `scheduledAt`/route WAIT 之前：不要把已受理/未知 Delivery、旧 owner 或已进外部 Provider 的任务说成“未发送”；已过期仍由 T-39 明确 `NOTIFICATION_EXPIRED` 或 `DEADLINE_OUTCOME_UNKNOWN`。`finishOutbox` 必须继续检查影响行数，Delivery/Outbox/聚合同事务写入，失败回滚且不插假 Provider Attempt。
3. **外部未发证明是合取，不是 READY/0 单项。** 对还 PENDING 的旧不支持外部任务，只有本次 `claimedFromReady==true`、持久 `DEADLINE_UNSENT_READY`、Outbox/Delivery attempt 都 0、providerMessageId/acceptedAt/deliveredAt 均空，且 owner/token/lease 仍有效，才能以固定诊断码 FAILED/DONE；参照 T-39 `NotifyDispatchResultService.java:115-135`。缺一项则 UNKNOWN/WAITING_RECEIPT，无 Provider 调用、无 READY+30 秒、无自动重试。尤其旧 markerless READY/0 可能经历过旧 PROCESSING 外呼崩溃再 WAIT 回流；**未到期的正常 READY 仍保持 T-37/T-38 既有重试合同**，不可把 T-50 的 unsupported 处置误扩为全局历史 READY 轨迹证明。新错误码不可进入 T-38 `safeLocalFailure` 白名单，也不得清 Redis 幂等键。
4. **IN_APP 本地事实优先。** 对不支持任务，既有本人 `notify_message_recipient` 与同 Intent `notify_message` 都存在时恢复 DELIVERED/DONE，不新推送；仅有关系的孤儿形状转本地不一致 FAILED/DONE；无关系才确认未投递并 FAILED/DONE，不 reserve 预算、不 persist/建关系/推送。已预留 `IN_APP_ATTEMPT_RESERVED` 的 attempt_count 不得重置。当前 T-39 到期分支只在到期时查这些事实 `NotifyDispatchResultService.java:93-135`；新 unsupported 分支在未到期时也必须使用同等优先级，否则 ACK-loss 后已提交的站内收件箱会被误报未发。不能走外部 UNKNOWN/WAITING_RECEIPT 代替本地核查。
5. **聚合与取消不倒退。** `NotificationAggregatePolicy.java:20-34` 在已有 ACCEPTED/DELIVERED + 另一个 PENDING 的混合 Intent 上可能形成 PROCESSING/PARTIAL_FAILURE；只结算本 Delivery，再在 Intent 锁内重算，不能给整个 Intent 强写 FAILED，也不能改已有外部结果。`NotifyDispatchResultService.java:72-91` 的非 PENDING 和 CANCELLED/terminal 先行规则保留；`NotificationApplicationRuntimeService.cancel():282-296` 的控制面不因禁止 submit/retry 而被挡。真实历史不支持任务只读清单不能从数据库静态行推出本次 `claimedFromReady`（它是 `NotifyOutbox.java:37-40` 的内存字段），所以清单只能分类待运行时核实，不能批量 UPDATE 成“未发”。发布先停旧 Worker，存量 UNKNOWN/ACCEPTED/DELIVERED 与无 Outbox 行分别受控核对。

## 必需红灯与真实验收

- **入口/API**：公共 Service 与真实授权 HTTP POST 覆盖 ALL/ASYNC/0 成功、null 默认成功、ORDERED_FALLBACK/ESCALATION、priority `+1/-1`、相同幂等键的非法重复、HTTP `SYNC` 字符串绑定失败、无权限拒绝；逐例比对 Intent/Recipient/Delivery/Outbox/Attempt 零新增。旧 unsupported `FAILED`+T-38 可重试本地码的 retry 必须明确失败，零 requeue/wake；foreign delivery ID 仍拒绝归属，query/cancel 可用。不能以未装安全拦截器的 MockMvc 证明权限。
- **12 生产构造**：固定源码 `git grep` 仍为 10 文件/12 次调用。每处改 0，并在捕获命令或真实提交中核 `priority=0`、ALL/ASYNC 及原幂等键/目标/模板/权限/时效不变；尤其 Captcha SMS/MAIL 的 T-39 共同绝对截止、Enterprise challenge 截止与 PersonRebind 非 OTP 时效。T-39 `NotificationDeadlineRuntimeTest` 与 `NotifyDeadlineIntegrationTest` 的旧正向 80 夹具改为 0 后仍保留全部截止断言，并另有 80 负例。
- **六 SQL 隔离 MySQL/Redis**：分别构造旧 ORDERED_FALLBACK/ESCALATION 的实际 prior WAIT、旧 mode 字符串与 priority-only 非零；本次 READY+marker+0+无相反事实终结 FAILED/DONE/0 Provider/0 Attempt，markerless READY 与 PROCESSING 重领转 UNKNOWN/WAITING_RECEIPT/0 Provider，重复 wake 不再 READY+30 秒。混合已受理/已送达/未知 Delivery 不倒退，已有 IN_APP 本人关系恢复且不重推、孤儿本地失败、无关系零预算/消息，旧 token/过期 lease 无写，数据库末尾故障整笔回滚。T-39 到期优先、未来 schedule 不早发、Provider 进入后跨截止仍保真实 ACCEPTED。真实测试必须 opt-in 零 skip，使用 owned 隔离服务与来源 SHA。
- **存量与发布**：提供不含正文/地址/供应商 ID 值的只读分组/精确 ID 清单，区分可领取、活租约、WAITING_RECEIPT、已受理/终态、无 Outbox；静态清单不作未发证明。生产存量数量、逐 ID 审批、旧 Worker 停止与新版本接管仍是后续发布证据，不能以本地测试或 SQL 草案冒充已执行。

## 实施时易成 blocker 的偏差

- 只迁移提交校验而漏任一真实构造点，会让对应业务提交失败；仅改调用而不在 duplicate 前校验，会让非法请求复用旧回执。仅在 `routeDecision` 外部加判断、未在短事务 gate 中核锁后事实，会有取消/重领/过期与 Provider I/O 的竞争。
- 把 READY/0、`PENDING` 或 marker 任一单项当“未发”，或把旧 PROCESSING 重领重排 READY，可能盲重发历史外部投递；把 unsupported 错误码纳入人工安全重试同样危险。
- 先把 IN_APP 统一 FAILED 再查消息/本人关系，会把已提交收件箱事实误报失败；对纯未发 IN_APP 使用通用 `complete()` 可能插伪 Attempt 或绕开预算/本地事务语义。
- 未支持旧值的已受理/已送达 Delivery 必须保留；只关闭无效 Outbox 而不刷新应刷的混合聚合、或把整个 Intent 强写 FAILED，都会改变既有外部事实。
