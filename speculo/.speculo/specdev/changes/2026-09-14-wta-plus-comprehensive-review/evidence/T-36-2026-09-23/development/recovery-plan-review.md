# T-36 存量 IN_APP 恢复边界只读预审

固定源码：`5118051403de0648540646d98536d8c4f3f9f26d`；合同：T-36 执行路线第 5 步、发布/恢复段及 AC-036。只读静态核查；未连接数据库、未执行下列 SQL、未读取真实数据或凭据。以下是**盘点与待审批操作稿**，不是生产修复授权或现成可运行的恢复命令。

## 结论与现有能力

**可交付只读清单；现有公共重试入口不能安全完成“指定 ID 的 IN_APP 恢复”。** `NotificationRetryCommand.deliveryId` 已定义，但 `NotificationApplicationRuntimeService.retry()` 从未读取它，而是选中同一 intent 下所有 `FAILED/UNKNOWN` delivery 并重排，包括 SMS/MAIL（`NotificationApplicationRuntimeService.java:163-198`）。管理 POST `/notify/notification/{notificationId}/retry` 直接委托该方法（`NotificationController.java:40-49`），不能用于本操作。即使传 deliveryId、reason、idempotencyKey，也没有对应的过滤、审计幂等或外部渠道隔离。禁止为恢复调用此 API。

原实现把 `message_id=intent_id`，先查后插消息及 `(message_id,user_id)` 关系（`InAppNotificationService.java:33-59`），随后才单独写结果；`dispatch()` 仅接受 `PENDING` delivery（`DispatchNotificationService.java:66-76,92-110`）。因此 `DELIVERED` 但缺关系的记录不会因重新唤醒 Outbox 自动补齐。DDL 已有 `uk_notify_message_recipient(message_id,user_id)`，但唯一键只防重复，不能建立缺失关系。

可复用的底层构件有：`lockIntent→lockOutbox→lockDelivery`、owner/token/数据库时钟 fence、`saveDeliveryResult`、Attempt/Outbox/聚合写入（`NotifyDispatchResultService.java:24-100`）；`NotifyOutboxMapper.xml:4-22,34-53` 中有 claim/finish/requeue 条件；`NotificationApplicationUseCase.retry()` 是 `@DSTransactional`。这些构件并**未**组成单 delivery、仅 IN_APP、含预检查和幂等恢复的现成入口。特别是 DAO 的 `requeueOutbox` 只按 deliveryId 从 WAITING_RECEIPT/DEAD_LETTER 挑最后一行，不限制 intent/channel，也不验证关系或生成完整恢复决策。T-36 当前写集允许 runtime/DAO/mapper/usecase，但本票只要求操作稿；若要把操作稿变成生产能力，须由 Lead 先另行登记精确入口、权限/审计及测试，不能口头视为已经具备。

## 只读盘点 SQL（审核后按有界 ID 窗口在批准的数据源执行）

对每个预先确定的 `delivery_id` 主键区间绑定 `lower_id`、`upper_id`，每页最多 500。下一页把 `lower_id` 设为上一页最后返回的 ID；区间末端继续下一区间。初筛可用只读副本，任何恢复裁决必须在主库重新读取；只导出下列标识、状态和布尔值，不导出 title/content/target_value、联系方式或错误原文。MySQL `?` 是预编译绑定位置，不能字符串拼接。

```sql
SELECT d.delivery_id, d.intent_id, d.user_id, d.status AS delivery_status,
       i.status AS intent_status, d.attempt_count,
       m.message_id, mr.message_recipient_id,
       CASE WHEN m.message_id IS NULL THEN 1 ELSE 0 END AS message_missing,
       CASE WHEN mr.message_recipient_id IS NULL THEN 1 ELSE 0 END AS relation_missing,
       CASE WHEN m.message_id IS NOT NULL
                 AND (NOT (m.title <=> i.title_snapshot)
                   OR NOT (m.content <=> i.content_snapshot)
                   OR NOT (m.path <=> i.path_snapshot))
            THEN 1 ELSE 0 END AS snapshot_mismatch,
       (SELECT COUNT(*) FROM notify_outbox ob
         WHERE ob.delivery_id = d.delivery_id) AS outbox_rows,
       (SELECT COUNT(*) FROM notify_outbox ob
         WHERE ob.delivery_id = d.delivery_id
           AND ob.status = 'PROCESSING'
           AND ob.lease_until > UTC_TIMESTAMP(6)) AS live_leases
FROM notify_delivery d
JOIN notify_intent i ON i.intent_id = d.intent_id
LEFT JOIN notify_message m ON m.message_id = d.intent_id
LEFT JOIN notify_message_recipient mr
       ON mr.message_id = d.intent_id AND mr.user_id = d.user_id
WHERE d.delivery_id > ? AND d.delivery_id <= ?
  AND d.channel = 'IN_APP'
  AND (
       d.status = 'UNKNOWN'
       OR (d.status = 'DELIVERED'
           AND (m.message_id IS NULL OR mr.message_recipient_id IS NULL))
       OR (d.status IN ('PENDING', 'FAILED')
           AND mr.message_recipient_id IS NOT NULL)
       OR (m.message_id IS NOT NULL
           AND (NOT (m.title <=> i.title_snapshot)
             OR NOT (m.content <=> i.content_snapshot)
             OR NOT (m.path <=> i.path_snapshot)))
      )
ORDER BY d.delivery_id
LIMIT 500;
```

`UNKNOWN` 是待判定清单，不自动等于可重做；`PENDING` 且没有消息/关系可能是正常未处理，故未作为异常。相同 intent 的其他用户已经生成共享 `notify_message` 时，某个 PENDING 用户没有关系也属正常。任何 `snapshot_mismatch=1`、`user_id IS NULL`、孤立 intent/outbox、活租约、多 outbox 或跨渠道混合，都转人工调查；不能仅凭状态推断以前提交失败。上述查询的 `JOIN notify_intent` 会排除无 intent 的孤儿 delivery，若需完整数据质量盘点需另立范围。

指定 ID 预检查使用参数化只读查询，记录状态和计数，不记录正文或 provider 错误原文：

```sql
SELECT d.delivery_id, d.intent_id, d.user_id, d.channel, d.status,
       d.attempt_count, i.status AS intent_status,
       m.message_id, mr.message_recipient_id,
       CASE WHEN m.message_id IS NOT NULL
                 AND (NOT (m.title <=> i.title_snapshot)
                   OR NOT (m.content <=> i.content_snapshot)
                   OR NOT (m.path <=> i.path_snapshot))
            THEN 1 ELSE 0 END AS snapshot_mismatch
FROM notify_delivery d
JOIN notify_intent i ON i.intent_id = d.intent_id
LEFT JOIN notify_message m ON m.message_id = d.intent_id
LEFT JOIN notify_message_recipient mr
       ON mr.message_id = d.intent_id AND mr.user_id = d.user_id
WHERE d.delivery_id = ? AND d.channel = 'IN_APP';

SELECT outbox_id, intent_id, delivery_id, status, attempt_count, max_attempts,
       lease_until, next_attempt_at
FROM notify_outbox WHERE delivery_id = ? ORDER BY outbox_id;

SELECT attempt_no, status, error_code
FROM notify_attempt WHERE delivery_id = ?
ORDER BY attempt_no DESC LIMIT 10;
```

## 待审批的**定向**操作稿；当前不可直接生产执行

输入必须是逐个审阅、批准的 `delivery_id` 正整数清单及每个 ID 的预期 `intent_id/user_id/status/outbox_id`、审批编号、前后快照。导出只含非敏感状态/计数的备份证据；业务内容仍留在受控数据库。每个 ID 独立短事务，先按现有结果写序锁 Intent→明确 Outbox→Delivery，在锁后以数据库 UTC 时间重读所有预期值和关系。若任何值变化、outbox 不唯一或仍有 live PROCESSING 租约，**停止该 ID**，不得清租约或越过 fence。检查该 intent 的 SMS/MAIL 行只用于证明不会变更，不把它们加入更新集。

仅对确认为 `IN_APP + UNKNOWN + 本用户关系缺失 + 对应 WAITING_RECEIPT outbox + 无活租约 + 消息不存在或快照完全一致` 的 ID，未来受控恢复工具可在同一事务内使用**带 delivery_id、旧状态、outbox_id、旧状态的条件更新**把这一条 delivery 设为 PENDING、这一条 WAITING_RECEIPT outbox 设为 READY/到期可领取，清理该 outbox 的过期 owner/token/lease，并按 `NotificationAggregatePolicy` 在同一事务重算 intent；每一步必须恰好影响 1 行，否则整体回滚。工具需登记操作幂等键/审计，保持历史 Attempt 和旧状态取证；提交后仅让新 T-36 原子 IN_APP dispatch 走正常 claim/fence 写消息、关系、结果。这里是差异设计，不是已实现 SQL：现有公共 retry 会触发外部渠道，现有 DAO requeue 缺上述条件，直接运行其方法不符合合同。若指定 outbox 是 DEAD_LETTER、DONE、READY 或缺失，不能套用这一分支，需逐案设计并隔离演练；特别是生成新 outbox ID/聚合状态不能靠一条临时 SQL 猜测。

`DELIVERED` 缺本用户关系须**单列**：原 dispatcher 会直接 CLOSE，不能通过 UNKNOWN→PENDING 重排。先核实共享消息快照与 intent 一致、用户归属、其他收件人及已有已读事实；若消息缺失或快照冲突，冻结并调查。安全修复需另行批准受控事务在 Intent/对应 Outbox/Delivery 锁下按唯一键插入缺失消息/关系（或识别已存在），保持 DELIVERED 结果和历史 Attempt，不重发 SMS/MAIL，不把该行改 PENDING；重复执行应为无变化。当前没有这样的按 ID 生产恢复入口，不能把这段作为可执行修复步骤。

提交结果 ACK 丢失或脚本超时属于**未知提交**：不要重跑 UPDATE。先从主库重新查询该 ID、outbox、消息/关系、Attempt 及聚合；若仍 UNKNOWN+WAITING_RECEIPT 且快照未变，可重新经完整预检；若 PENDING+READY、已被 claim，或 DELIVERED+关系已齐，按已前进处理；其他组合隔离人工判定。任何阶段都不得直接触发 NotifyClient 或让 SMS/MAIL UNKNOWN 重发。

## 隔离演练与证据要求

在 T-36 新原子实现固定后，用真实隔离 MySQL/DSTransactional 代理先演练：同 intent 含 UNKNOWN IN_APP 与 UNKNOWN SMS/MAIL，只改变指定 IN_APP ID；两个 IN_APP 用户共享 message，另一用户关系不受损；两次相同恢复/提交 ACK 丢失后复查不增加 message/recipient/Attempt；活租约、过期 owner、状态并发变化、快照不符、零行和多 outbox 均 fail closed；UNKNOWN 缺关系经新 dispatch 收敛；DELIVERED 缺关系走独立分支或明确保留待审批，不能虚报完成。记录前后 ID/状态/行数、唯一键和回滚/提交证据，隐藏正文/目标/凭据。真实生产修复本票不执行。

静态审查命令仅为 `git show 5118051:<path>`、`git grep 5118051`；本报告生成过程中没有运行 SQL、服务、Maven 或测试。
