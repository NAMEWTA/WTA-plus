# T-36 预算/结果路径草稿源审查（实施中，不是固定候选结论）

读取基准：当前工作树一次逐文件字节复制到 `/tmp/wta-t36/draft-budget-snapshot/`；下表 SHA-256 对应该快照，后续 writer 可能修改工作树。对照材料：`/tmp/wta-t36/budget-design-review.md` 与 T-36 revision155。只读检查；未运行 Maven、服务、SQL 或测试。

## 快照文件

| 相对路径 | SHA-256 |
|---|---|
| `backend/wta-api/src/main/java/org/namewta/notify/api/InAppNotificationPort.java` | `b522c8f7b026d9b3252c5e1faa6e24b33684c26e11ba2a96d3f27dfd882320ff` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java` | `19768823b03c93abceef5fa4b22d02841b3bfbf945131a75cf7f7eef950f084b` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/entity/NotifyOutbox.java` | `f1ec2c633f90539e8d3f1228254aa71ba0f534165cba366cf3dddff54aeda8d1` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/mapper/NotifyOutboxMapper.java` | `d1f9368081cec484d75cf9df4dba0e28c126a14be546417497d3443bac731ae3` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/NotifyDispatchResultPort.java` | `7c49e62a24a288f6a4773390e4a74a17fb791ee72f87fc5e247dddb4735d4a0c` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/port/InAppDeliveryCommittedEvent.java` | `7a4d23763fad1d29a906baf2f4ccaea76fcf1a2de2e015a8f493e3edc3d3457d` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java` | `30b852fcf3d3981929532fa7fa71ab29aa4cd35b0cce20ecfd606e5e9233317d` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/InAppNotificationService.java` | `82ad511415dcbdb870dc9ec241e18589cc28f7ba812a931c50fbbf6ba47fc8e5` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java` | `1d626e5834b92499a53996e78611d924aab2f8cdcaf06b1ee6fc9dd9917b211f` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyDispatchResultUseCase.java` | `4dc9835741cfea4e65f087f9b5ba4c784fae4b1588b3e617adba38597e1fce08` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/InAppCommittedPushUseCase.java` | `20f4873237c0061864bf2472398c42ecc88d8b8e026b8747c51597c157ce9410` |
| `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/event/InAppCommittedPushListener.java` | `257d75b91aa367c3c8f3e2b4c542d071e424b0ff54e0b3035110abce90c56ca3` |
| `backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml` | `acb0dad2c710051be26ab7d5664d469fa8b8f546ec3bd571215d40efc9f73a0d` |

## 需修的合同缺口

**[设计阻断，当前受控调度路径尚未触发] `complete()` 没有封死未预留的 IN_APP 成功结果。** `NotifyDispatchResultService.java:114-123` 在锁/fence 后直接调用 `writeResult`；其 `149-151` 仅在缺少 RESERVED 时给 IN_APP 再加 Outbox 计数，依然允许 `Result("DELIVERED", ...)` 把 Delivery/Attempt/Outbox/Intent 写成成功，而从未写消息和收件关系。现有 `DispatchNotificationService.dispatchInApp:202-209` 只在本地参数失败时以 `FAILED/LOCAL_DISPATCH_ERROR` 调用普通 `complete`，所以正常生产调度目前避开了此路径；但公开 port 的事务边界未兑现“IN_APP 成功必须先预留并同事务 persist+result”。建议在 `complete` 对 IN_APP 仅允许明确的预留前本地终态（状态、错误码双条件），其余拒绝/回滚；成功只允许 `completeInApp`。加入直接调用 port 的反例测试：有效 lease、无 RESERVED、传 DELIVERED 后 message/recipient/delivery/outbox/attempt 不发生成功写入；本地确定性 FAILED 仍正常终结。

## 已见满足方向及待验证处

- `beginInAppAttempt` 的锁序是 Intent→Outbox→Delivery，锁后查数据库 UTC/fence、channel/status；`RESERVED` 检查在 max 判断之前（`NotifyDispatchResultService:27-42,61-83`）。额度耗尽在该独立事务写 FAILED/DEAD_LETTER/聚合，零 persist；`reserveInApp` SQL 再以 owner/token、租约、`attempt_count<max_attempts` 和 marker 条件 CAS（`NotifyOutboxMapper.xml:28-35`）。`NotifyDispatchResultUseCase:25-33` 的两个入口各自 `@DSTransactional`，正常调用顺序是 begin 成功后才 completeInApp（`DispatchNotificationService:207-209`）。这支持“预算提交失败/ACK 未知则不会调用 persist”；测试仍须实证代理生效、ACK 丢失不返回 true。
- claim 更新同一 SQL 成功写新 token 时，只在对应 delivery 为 IN_APP 且错误码恰为 marker 时清标记（`NotifyOutboxMapper.xml:14-25`）；其他渠道错误码保留。ClaimService 返回候选对象可能仍带旧标记，但 dispatch 首先重读 outbox，begin 再锁定当前行；不能在后续重构中改用候选对象判断。两连接竞争、旧 owner 和同 token 重入须测真实 SQL。由于该 SQL 在 outbox UPDATE 内有读 delivery 的子查询，真实 MySQL 并发门禁还需覆盖锁等待/死锁，不凭静态推断通过。
- `completeInApp` 要求当前活 lease、PENDING IN_APP、匹配用户且当前标记 RESERVED；在同一被代理事务里调用 `port.persist`，随后再查租约并共用 `writeResult` 保存 Delivery/Attempt/Outbox/聚合（`NotifyDispatchResultService:94-106,125-164`）。已预留 IN_APP 不二次加 Outbox count；消息 SQL 或 finish 冲突抛异常会回滚此结果事务。需要真实动态事务/提交故障证明。允许预算已消耗而消息事务完全回滚，这是 revision155 的刻意语义。
- `InAppNotificationService.persist:54-59` 对已有共享消息校验 title/content/path/noticeType/channelsJson，不一致直接抛错；新关系才发布只含 messageId/userId 的事件（`61-72`）。`InAppCommittedPushListener:22-29` 使用 `DsTxEventListener(AFTER_COMMIT)` 且只记异常类别，UseCase 从已提交本人关系/消息重读后定向推送（`InAppNotificationService:80-102`）。须在真实动态事务下断言回滚零推送、提交仅新关系推送、推送故障不改持久结果。现有比对没有覆盖 `category/type/source/message` 字段；若历史消息可同 ID 且这些字段失配，恢复可接受错误旧行。此为存量异常修复审阅项，主路径当前已有五个核心快照字段校验；是否扩比较应按真实旧数据合同裁决，不凭理论碰撞扩写集。
- `DispatchNotificationService.dispatchInApp:189-205` 的端口/用户/参数失败在预算前走明确 `FAILED/LOCAL_DISPATCH_ERROR`，不会伪装供应商 UNKNOWN；begin/completeInApp 的异常未被 catch 成结果，所以 ACK 未知不会被当作确定失败。还需断言 snapshot 构造使用的 intent 在结果事务锁后没有被其他合法路径更改；当前运行时仅见 intent 快照在 submit 插入，更新仅状态/聚合，但集成门禁应覆盖快照冲突。
- `NotifyOutboxMapper.xml:57-66` 的人工 requeue 仍将 Outbox count 清零；自动重领上限可成立，全生命周期上限（含管理员人工重试）不能声称。T-36 恢复稿已禁用通知级 retry，若此人工接口在验收中被算作“无界自动重试”，需另行明确合同/写集。

需 writer 先修第一项并保留红灯/绿灯证据；本稿不宣称当前产品已通过 AC-036。
