# T-36 指定 ID 的存量 IN_APP UNKNOWN 恢复操作稿（待审批、待演练）

**性质：审阅稿；未在隔离库演练，也未执行任何生产 SQL。** 保留原始草稿 `/tmp/wta-t36/recovery-by-id-sql-draft.md`。本稿只覆盖经只读盘点和单独审批的**一个** `delivery_id`：IN_APP、`UNKNOWN`、本人收件关系不存在、唯一 Outbox 为 `WAITING_RECEIPT` 且无 lease。`DELIVERED` 缺关系、UNKNOWN 但已有关系、多个 Outbox、其他渠道、其他状态均退出本流程，另立风险裁决。不可用通知级 retry API 代替本流程。

## 执行前提与冻结记录

- 先确定源/目标产品 Git SHA、实际部署版本、唯一六 SQL 基座版本、目标数据库和维护窗口；备份经独立恢复验证。新版本 Worker 已部署且是目标环境唯一可处理该业务的 Worker；旧版 Worker 全部退出，暂停其他修复作业。由环境负责人另行批准真实数据写入，本稿本身不构成授权。
- 审批单逐个记录正整数 `intent_id`、`delivery_id`、`outbox_id`、`recipient_id`、`user_id` 及预期状态/预算和非敏感业务归属。每个 ID 使用独立主库连接和短事务；不批量拼接 SQL，不在读副本作写前判定，不跨 ID 复用会话变量。受控工具使用 PreparedStatement/等价绑定参数；日志只写 ID、状态、行数、布尔核验和提交结果，不写标题、正文、联系方式、原始 JSON 或凭据。
- 冻结与最终部署版本一致的 C2 快照归一化实现：`template_params_json` 为 SQL/JSON null 时 `noticeType=null`；空对象或缺 `noticeType` 时空串；显式 JSON null 时按 Java 原行为为字面量 `"null"`。`channels` 缺失、null 或空数组时按当前 delivery 渠道回退到 `["IN_APP"]`，其余按生产 `JsonUtils` 解析为**有序 `List<String>`**。不以 JSON 文本/空格比较，也不以手写 SQL `JSON_EXTRACT` 猜测 Java 的 `String.valueOf`；其他合法类型的处理必须调用同版本生产归一化函数或经同版本对照测试。校验 title≤255、noticeType≤10、path≤500 **Unicode 码点**；完整 content 保留，不把 1000 码点消息摘要当正文。归一化抛错或超限即剔除本恢复分支。

## 只读初筛：生成单 ID 计划

主库以绑定 ID 查询 `notify_intent`、目标 `notify_delivery`、其 `notify_recipient`、该 delivery **全部** `notify_outbox`、对应 `notify_message` 和 `(message_id,user_id)` 关系、同 intent 所有 delivery 的 ID/channel/status，以及目标 Attempt 数/最大编号。敏感快照仅停留在受控工具内存。至少断言：

1. 所有主键/外键恰好匹配审批 tuple；recipient 属于同 intent/用户且仍可投递；`user_id>0`，目标 `channel='IN_APP'`、`status='UNKNOWN'`、`delivered_at IS NULL`。Intent 未取消，`expires_at` 为空或晚于主库当前 UTC，当前聚合与同 intent delivery 状态按 `NotificationAggregatePolicy.aggregate` 一致。
2. 该 delivery 的 Outbox 总数**恰好 1**，且就是审批 ID；`status='WAITING_RECEIPT'`，owner/token/lease_until 均 NULL，`0<=attempt_count<max_attempts` 且 `max_attempts>0`，没有仍为 `IN_APP_ATTEMPT_RESERVED` 的活动标记。不得重置 `attempt_count`、`max_attempts`、Attempt、历史错误或审计列。
3. 本人 `(intent_id,user_id)` 关系不存在。若消息不存在，可由新 Worker 原子创建；若消息已存在，则以内存中的同版本 C2 snapshot 与数据库消息逐项比较 title/content/path/noticeType 和**解析后的有序 channels 列表**，必须完全相等。比较失败即人工调查，不能覆盖旧消息或忽略差异。其他用户关系保持原样。
4. 保存同 intent 的 SMS/MAIL delivery ID、status、attempt_count 及目标的所有非敏感前态作为不可变对照。任何外部 UNKNOWN 只读，不加入 DML。初筛成功只产生计划，不授权执行；计划列出每项 gate 的 true/false、预期聚合变化和两条更新的参数，不输出快照正文。

## 单 ID 短事务：锁内重验、两条 CAS、聚合

受审一次性工具在同一主库连接上关闭自动提交并开始事务，按**Intent→Outbox→Delivery** 顺序执行锁定当前读：

```sql
SELECT intent_id,status,expires_at FROM notify_intent WHERE intent_id=? FOR UPDATE;
SELECT outbox_id,intent_id,delivery_id,status,attempt_count,max_attempts,
       lease_owner,lease_token,lease_until,last_error_code
  FROM notify_outbox WHERE delivery_id=? ORDER BY outbox_id FOR UPDATE;
SELECT delivery_id,intent_id,recipient_id,user_id,channel,status,delivered_at,
       attempt_count,error_code,error_message
  FROM notify_delivery WHERE delivery_id=? FOR UPDATE;
```

必须分别得到 1 个 Intent、**全部 Outbox 且恰为批准的 1 行**、1 个目标 Delivery；不得只按 `outbox_id` 锁住一个而忽略同 delivery 其他行。随后在锁内读取当前 `notify_recipient`、消息/本人关系及同 intent 的所有 Delivery（按 ID 顺序当前读；按需 `FOR UPDATE`），重新运行初筛的全部身份、状态、有效期、预算、快照及外部渠道不变断言。锁内不得调用外部服务、等待人工确认或执行耗时全文输出。任一断言失败 `ROLLBACK` 并报告只读差异。

通过后只做两条限定旧状态/审批 ID 的参数化更新；下方 `?` 均为绑定值，工具把锁后读到的 attempt/max/error 作为 CAS 旧值，同时继续要求关系缺失、消息快照 gate 为 true：

```sql
UPDATE notify_delivery
   SET status='PENDING', error_code=NULL, error_message=NULL
 WHERE delivery_id=? AND intent_id=? AND recipient_id=? AND user_id=?
   AND channel='IN_APP' AND status='UNKNOWN' AND delivered_at IS NULL;

UPDATE notify_outbox
   SET status='READY', available_at=UTC_TIMESTAMP(6), next_attempt_at=UTC_TIMESTAMP(6)
 WHERE outbox_id=? AND intent_id=? AND delivery_id=?
   AND status='WAITING_RECEIPT'
   AND attempt_count=? AND max_attempts=? AND attempt_count<max_attempts
   AND lease_owner IS NULL AND lease_token IS NULL AND lease_until IS NULL
   AND last_error_code <=> ?;
```

每条 `executeUpdate()` **必须恰好为 1**，否则立即 `ROLLBACK`；不把“SQL 执行无异常”当成功。预检 gate 由受控工具在同一事务中强制，不允许手动跳过。两条 UPDATE 不修改 `attempt_count`、`max_attempts`、Attempt、消息或关系，也不覆盖 Outbox 历史错误码。工作者后续正常 claim 会按 C2 的 IN_APP 规则清内部预留标记。

在仍持有 Intent 锁时，以锁内所有 delivery 的当前状态（目标替为 PENDING）调用**同版本** `NotificationAggregatePolicy.aggregate` 算新聚合；不在操作稿另写可能漂移的 SQL CASE。若新聚合不同于锁定的 `intent.status`，以 `WHERE intent_id=? AND status=?` 更新并要求 `executeUpdate()==1`；若相同，不写 Intent 并确认仍相同。提交前再次在本事务核对目标 PENDING、Outbox READY/预算原值、聚合匹配、message/recipient 未被此修复改写、Attempt 数与全部其他 delivery 尤其 SMS/MAIL 对照不变。任何失败或 SQL 异常 `ROLLBACK`，整笔不得留下单边状态。所有断言通过才 `COMMIT`；工具不得把不可判定的 commit 异常标记为 rollback 成功。

## 提交后判读与恢复边界

收到明确 commit 成功后，主库新连接只读确认目标、Outbox、聚合、预算、消息/关系/Attempt 和外部 delivery 对照；随后只允许新 Worker 依原 claim/lease/fence 消费 READY IN_APP。重复运行若见 PENDING/READY、PROCESSING 或 DELIVERED 且已有关系，视为已前进，只读结束，不再次更新。

若 COMMIT ACK 丢失或连接断开，立刻停止该 ID 写入；**不能**依据抛错推断未提交，也不能重放两条 UPDATE。用新主库连接只读核状态和计数：已是 PENDING/READY 或被新 Worker claim/送达则按已前进处理；若仍 UNKNOWN/WAITING，但预算、Attempt、时间、消息/关系或其他事实任一变化，则归为不确定并人工调查。只有独立证明前次未提交且完整锁内资格仍成立、并获得再次审批，才能新开一个单 ID 事务；提交未知期间不可自动补发。

`DELIVERED` 缺关系不是本流程：新 dispatcher 会关闭非 PENDING 任务，强改回 PENDING 有重复通知风险，需独立审批与隔离演练的受控关系修复。UNKNOWN 但关系已存在也不得用本稿。SMS/MAIL 状态与任务全程只作对照，绝不更新；不删除 Attempt、不重置预算、不复活旧 Worker。真实演练结果、审批、备份/恢复证明、操作人及最终版本另行记录；本稿无任何已演练或已执行声明。
