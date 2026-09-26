# T-36 指定 ID 的 IN_APP UNKNOWN 恢复：逐行差异 SQL 操作稿

**状态：C1 后冻结待 C2 复核；当前不能演练写入，更非生产修复证据。本轮不执行任何 SQL，也不授权生产修复。** 本稿只适用于 T-36 新原子 IN_APP dispatch 已部署、所有旧 Worker 已退出、且经审批的**一个** `delivery_id`。它不调用通知级 retry API、不触碰 SMS/MAIL、不清 Attempt、不重置 Outbox 预算，也不处理 DELIVERED 缺关系。真实执行前由 Lead 确认目标环境、维护窗口、备份、权限和回滚证据。表/字段与策略基于固定 base `5118051403de0648540646d98536d8c4f3f9f26d` 的六 SQL 基座与 `NotificationAggregatePolicy.aggregate`；必须与最终 T-36 head/部署版本再核对。

以下 `@t36_*` 是**同一 MySQL 连接上的绑定会话参数**，四个正整数只能来自已审阅的只读盘点，绝不拼接任意字符串。使用驱动时把值绑定到设置参数的预编译语句；在受控 SQL 客户端手工执行时，只把四处尖括号替换成已批准的十进制数字。每个 ID 独立连接/事务，不能复用上个 ID 的会话变量。

```sql
SET @t36_delivery_id := <APPROVED_DELIVERY_ID>;
SET @t36_intent_id   := <APPROVED_INTENT_ID>;
SET @t36_outbox_id   := <APPROVED_OUTBOX_ID>;
SET @t36_user_id     := <APPROVED_USER_ID>;
```

先在主库**只读**重查目标，保存不含正文/联系方式的状态、Outbox 全部行数和 Attempt 最大号；要求 delivery 单行 `IN_APP/UNKNOWN`、正整数 user、intent/outbox/recipient 外键一致、该用户 `notify_message_recipient` 缺失、`notify_message` 不存在或 title/content/path/归一化 noticeType/channelsJson 五项与新 persist 的快照判定完全一致、该 delivery **仅有一个** WAITING_RECEIPT outbox、owner/token/lease_until 全 NULL、`attempt_count<max_attempts`、intent 未取消且未过期。凡是不满足者移出本分支；尤其已有关系但 UNKNOWN、DEAD_LETTER、DONE、READY、PROCESSING 或多 Outbox 不通过。另确认此 intent 下 SMS/MAIL 的 ID/状态并保存为“不可变对照”，不读取目标值或正文。

```sql
SELECT d.delivery_id, d.intent_id, d.recipient_id, d.user_id,
       d.channel, d.status, d.attempt_count, d.delivered_at,
       i.status AS intent_status, i.expires_at,
       m.message_id, mr.message_recipient_id,
       CASE WHEN m.message_id IS NULL THEN 1
            WHEN (m.title <=> i.title_snapshot)
             AND (m.content <=> i.content_snapshot)
             AND (m.path <=> i.path_snapshot) THEN 1
            ELSE 0 END AS core_snapshot_safe
FROM notify_delivery d
JOIN notify_intent i ON i.intent_id=d.intent_id
LEFT JOIN notify_message m ON m.message_id=d.intent_id
LEFT JOIN notify_message_recipient mr
  ON mr.message_id=d.intent_id AND mr.user_id=d.user_id
WHERE d.delivery_id=@t36_delivery_id AND d.intent_id=@t36_intent_id
  AND d.user_id=@t36_user_id AND d.channel='IN_APP';

SELECT outbox_id, intent_id, delivery_id, status, attempt_count,
       max_attempts, lease_owner, lease_token, lease_until
FROM notify_outbox
WHERE delivery_id=@t36_delivery_id ORDER BY outbox_id;

SELECT MAX(attempt_no) AS latest_attempt_no, COUNT(*) AS attempt_rows
FROM notify_attempt WHERE delivery_id=@t36_delivery_id;

SELECT delivery_id, channel, status, attempt_count
FROM notify_delivery WHERE intent_id=@t36_intent_id
ORDER BY delivery_id;
```

初筛中的 `core_snapshot_safe` **只覆盖三个正文/路径字段，不能作为执行资格**；新 persist 还严格比较 noticeType/channelsJson。下方锁后 `@t36_exact_snapshot_safe=1` 只是待校验的五项候选谓词；当前 JSON 比较尚未证明与 Java 对 channels 的**解析后列表语义相等**，C1 已暴露文本/长度对照问题。不得把此布尔值当作可执行授权；须待 C2 固定语义、改为解析后等价判定，并在隔离 MySQL 与 Java 逐案对照后才可演练写入。

若主库初筛满足，开始**一个短事务**。下面三个锁查询必须按所列顺序执行，不因行在初筛中存在就跳过。锁后再次核对上述所有条件；`notify_outbox` 查询须锁住该 delivery 的**全部** outbox 行且结果恰为批准的一个 ID，不能只锁所选行而漏掉并行插入。保持事务短，不在锁内做人工长时审阅或外部 I/O。

```sql
START TRANSACTION;
SELECT intent_id, status, expires_at FROM notify_intent
WHERE intent_id=@t36_intent_id FOR UPDATE;

SELECT outbox_id, intent_id, delivery_id, status, attempt_count,
       max_attempts, lease_owner, lease_token, lease_until
FROM notify_outbox WHERE delivery_id=@t36_delivery_id
ORDER BY outbox_id FOR UPDATE;

SELECT delivery_id, intent_id, recipient_id, user_id, channel, status,
       attempt_count, delivered_at
FROM notify_delivery WHERE delivery_id=@t36_delivery_id FOR UPDATE;

SELECT m.message_id, mr.message_recipient_id,
       CASE WHEN m.message_id IS NULL THEN 1
            WHEN (m.title <=> i.title_snapshot)
             AND (m.content <=> i.content_snapshot)
             AND (m.path <=> i.path_snapshot) THEN 1
            ELSE 0 END AS core_snapshot_safe
FROM notify_intent i
LEFT JOIN notify_message m ON m.message_id=i.intent_id
LEFT JOIN notify_message_recipient mr
  ON mr.message_id=i.intent_id AND mr.user_id=@t36_user_id
WHERE i.intent_id=@t36_intent_id;

-- 这一布尔值才是写入资格；先重置，避免连接复用时遗留上一 ID 的值。
SET @t36_exact_snapshot_safe := NULL;
SELECT CASE
  WHEN i.template_params_json IS NOT NULL
   AND JSON_TYPE(i.template_params_json) NOT IN ('OBJECT','NULL') THEN 0
  WHEN JSON_CONTAINS_PATH(i.template_params_json,'one','$.noticeType')=1
   AND JSON_TYPE(JSON_EXTRACT(i.template_params_json,'$.noticeType'))
       NOT IN ('NULL','STRING','INTEGER','DOUBLE','DECIMAL','BOOLEAN') THEN 0
  WHEN JSON_CONTAINS_PATH(i.template_params_json,'one','$.channels')=1
   AND JSON_TYPE(JSON_EXTRACT(i.template_params_json,'$.channels'))
       NOT IN ('NULL','ARRAY') THEN 0
  WHEN m.message_id IS NULL THEN 1
  WHEN (m.title <=> i.title_snapshot)
   AND (m.content <=> i.content_snapshot)
   AND (m.path <=> i.path_snapshot)
   AND (m.notice_type <=> CASE
         WHEN i.template_params_json IS NULL OR JSON_TYPE(i.template_params_json)='NULL'
           THEN NULL
         WHEN JSON_CONTAINS_PATH(i.template_params_json,'one','$.noticeType')=0
           THEN ''
         WHEN JSON_TYPE(JSON_EXTRACT(i.template_params_json,'$.noticeType'))='NULL'
           THEN 'null'
         ELSE JSON_UNQUOTE(JSON_EXTRACT(i.template_params_json,'$.noticeType'))
       END)
   AND m.channels_json = CASE
         WHEN i.template_params_json IS NULL OR JSON_TYPE(i.template_params_json)='NULL'
           OR JSON_CONTAINS_PATH(i.template_params_json,'one','$.channels')=0
           OR JSON_TYPE(JSON_EXTRACT(i.template_params_json,'$.channels'))='NULL'
           OR JSON_LENGTH(JSON_EXTRACT(i.template_params_json,'$.channels'))=0
           THEN JSON_ARRAY('IN_APP')
         ELSE JSON_EXTRACT(i.template_params_json,'$.channels')
       END
    THEN 1 ELSE 0 END INTO @t36_exact_snapshot_safe
FROM notify_intent i
LEFT JOIN notify_message m ON m.message_id=i.intent_id
WHERE i.intent_id=@t36_intent_id;
SELECT @t36_exact_snapshot_safe AS exact_snapshot_safe; -- 必须是 1
```

如锁后数据与批准快照不同，或目标 intent 仍有旧版 Worker/未决持有者，立即 `ROLLBACK`。通过后仅执行两条限定 ID/旧状态的 CAS；第一条清除过时的 delivery 错误字段但保留 `attempt_count`、历史 Attempt 和时间，第二条保持 Outbox 已消耗预算计数不变，并保持历史错误字段供调查。两条各须 `ROW_COUNT()=1`；任一不是 1 或出现 SQL 异常立即 `ROLLBACK`，不可提交第一条的单边结果。为了可核对，执行每条 UPDATE 后**紧接着**保存 `ROW_COUNT()`，不要让其他语句覆盖它。

```sql
UPDATE notify_delivery d
SET d.status='PENDING', d.error_code=NULL, d.error_message=NULL
WHERE d.delivery_id=@t36_delivery_id
  AND d.intent_id=@t36_intent_id
  AND d.user_id=@t36_user_id
  AND d.channel='IN_APP'
  AND d.status='UNKNOWN'
  AND d.delivered_at IS NULL
  AND @t36_exact_snapshot_safe = 1
  AND NOT EXISTS (
    SELECT 1 FROM notify_message_recipient mr
    WHERE mr.message_id=@t36_intent_id AND mr.user_id=@t36_user_id
  )
  AND NOT EXISTS (
    SELECT 1 FROM notify_message m
    JOIN notify_intent i ON i.intent_id=m.message_id
    WHERE m.message_id=@t36_intent_id
      AND (NOT (m.title <=> i.title_snapshot)
        OR NOT (m.content <=> i.content_snapshot)
        OR NOT (m.path <=> i.path_snapshot))
  );
SET @t36_delivery_rows := ROW_COUNT();
SELECT @t36_delivery_rows AS delivery_rows; -- 必须是 1，否则 ROLLBACK

UPDATE notify_outbox o
SET o.status='READY',
    o.available_at=UTC_TIMESTAMP(6),
    o.next_attempt_at=UTC_TIMESTAMP(6)
WHERE o.outbox_id=@t36_outbox_id
  AND o.intent_id=@t36_intent_id
  AND o.delivery_id=@t36_delivery_id
  AND o.status='WAITING_RECEIPT'
  AND o.attempt_count < o.max_attempts
  AND o.lease_owner IS NULL
  AND o.lease_token IS NULL
  AND o.lease_until IS NULL;
SET @t36_outbox_rows := ROW_COUNT();
SELECT @t36_outbox_rows AS outbox_rows; -- 必须是 1，否则 ROLLBACK
```

聚合状态必须与真实 `NotificationAggregatePolicy.aggregate` 等价，不能固定写 QUEUED。以下按所有 delivery 当前状态重新算：全部取消→CANCELLED；有 PENDING→PROCESSING；全有效 DELIVERED→DELIVERED；有 UNKNOWN/WAITING_RECEIPT→UNKNOWN；成功+失败→PARTIAL_FAILURE；全有效成功→ACCEPTED；其他→FAILED。目标 delivery 至少一行，因此 `COUNT(*)=0` 仅作防御。先保留锁后旧 intent.status 到 `@t36_old_intent_status`（下例的第一个 SELECT 必须在 UPDATE intent 前执行），在同事务内设置新状态；MySQL 同值 UPDATE 可报 0 changed rows，故不把聚合的 ROW_COUNT=0 当故障，而以锁后值和后验值核对。

```sql
SELECT status INTO @t36_old_intent_status
FROM notify_intent WHERE intent_id=@t36_intent_id;

SELECT CASE
  WHEN COUNT(*)=0 THEN 'UNKNOWN'
  WHEN SUM(status<>'CANCELLED')=0 THEN 'CANCELLED'
  WHEN SUM(status='PENDING')>0 THEN 'PROCESSING'
  WHEN SUM(status='DELIVERED')=SUM(status<>'CANCELLED') THEN 'DELIVERED'
  WHEN SUM(status IN ('UNKNOWN','WAITING_RECEIPT'))>0 THEN 'UNKNOWN'
  WHEN SUM(status IN ('ACCEPTED','DELIVERED'))>0
   AND SUM(status IN ('FAILED','UNDELIVERABLE'))>0 THEN 'PARTIAL_FAILURE'
  WHEN SUM(status IN ('ACCEPTED','DELIVERED'))=SUM(status<>'CANCELLED')
    THEN 'ACCEPTED'
  ELSE 'FAILED' END
INTO @t36_new_intent_status
FROM notify_delivery WHERE intent_id=@t36_intent_id;

UPDATE notify_intent
SET status=@t36_new_intent_status
WHERE intent_id=@t36_intent_id AND status=@t36_old_intent_status;

SELECT status, @t36_new_intent_status AS expected_status
FROM notify_intent WHERE intent_id=@t36_intent_id;
```

提交前须在同事务检查：`@t36_exact_snapshot_safe=1`、目标 delivery=PENDING、目标 outbox=READY、聚合等于计算值、message/recipient 尚未被本操作写入、其它 delivery（尤其 SMS/MAIL）与初筛完全一致、`@t36_delivery_rows=@t36_outbox_rows=1`。不符即 `ROLLBACK`；全部符合才 `COMMIT`。由于 SQL 客户端手工执行无法自动防止误点 COMMIT，生产若实际批准，应先把这些检查和异常自动 ROLLBACK 封装进一次性受审驱动并在隔离库演练，不能只靠操作员记忆。这两条差异 SQL 本身可执行，但**不构成未经验证的自动化修复能力**。

COMMIT 返回成功后，新 Worker 只按常规 claim/fence 消费这个 READY IN_APP outbox，由 T-36 原子事务补齐消息/关系/结果；实时提示只在提交后。主库后验读取目标和同 intent 的外部 delivery；重复执行见到 PENDING/READY、PROCESSING 或 DELIVERED+关系时只读判定已前进，不再次更新。COMMIT ACK 丢失时先停止，重新在主库查 delivery/outbox/count、message/recipient、Attempt 与聚合；若出现 PENDING/READY 或后续已 claim/DELIVERED，视为已前进；若仍 UNKNOWN/WAITING，但 Attempt/预算/时间等任一事实变化，视为不确定并人工调查。只有能够独立证明原事务未提交、或经再次审批的完整预检且数据完全未变，才可启动新的单 ID 事务；不能按异常直接重放。

`DELIVERED` 缺关系与 UNKNOWN 但已有关系均**不适用**以上分支。前者不是重排可修复状态：当前 dispatcher 会关闭非 PENDING 任务，需另一批审批/隔离演练的受控关系修复；本稿明确留待处理，不能以此 SQL 改为 PENDING。外部 SMS/MAIL UNKNOWN 一律只读，不进入输入清单或任何 DML。
