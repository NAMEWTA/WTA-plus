# T-50：历史未支持通知值的只读分类与发布处置稿

**执行状态：处置稿，未查询或变更生产数据库。** 隔离集成测试只证明合成样本的代码行为，不能证明生产存量为零。当前公开策略枚举保留历史值；可执行提交合同只有 ALL、ASYNC、priority=0。旧数据不批量归零后重放，也不恢复已失效验证码。

## 只读盘点

发布负责人先记录目标环境、候选、时间与时区及旧Worker停止情况；使用只读身份。输出仅含内部主键、状态、时间、计数和“事实是否存在”，禁止正文、模板参数、地址、幂等key、供应商流水值或凭据。

```sql
SELECT strategy, mode, priority, status, COUNT(*) AS intent_count
FROM notify_intent
WHERE NOT (strategy <=> 'ALL') OR NOT (mode <=> 'ASYNC') OR NOT (priority <=> 0)
GROUP BY strategy, mode, priority, status;

SELECT i.intent_id, i.strategy, i.mode, i.priority, i.status AS intent_status,
       i.scheduled_at, i.expires_at,
       d.delivery_id, d.channel, d.status AS delivery_status,
       d.attempt_count AS delivery_attempts,
       CASE WHEN d.provider_message_id IS NULL THEN 0 ELSE 1 END AS has_provider_reference,
       CASE WHEN d.accepted_at IS NULL THEN 0 ELSE 1 END AS has_accepted_time,
       CASE WHEN d.delivered_at IS NULL THEN 0 ELSE 1 END AS has_delivered_time,
       o.outbox_id, o.status AS outbox_status, o.attempt_count AS outbox_attempts,
       CASE WHEN o.last_error_code = 'DEADLINE_UNSENT_READY' THEN 1 ELSE 0 END AS has_new_unsent_marker,
       o.available_at, o.next_attempt_at, o.lease_until
FROM notify_intent i
LEFT JOIN notify_delivery d ON d.intent_id = i.intent_id
LEFT JOIN notify_outbox o ON o.intent_id = i.intent_id AND o.delivery_id = d.delivery_id
WHERE (NOT (i.strategy <=> 'ALL') OR NOT (i.mode <=> 'ASYNC') OR NOT (i.priority <=> 0))
  AND i.intent_id > 0
ORDER BY i.intent_id, d.delivery_id, o.outbox_id
LIMIT 500;
```

明细是首批模板；完整分页必须读取某Intent的全部关联行后才能推进Intent水位，并核对唯一Intent总数与汇总。并发环境应记录一致性时点与期间变化，不能将首批或一次空查询当作全量清单。claimedFromReady是领取时内存证据，静态SQL不能重构；新标记有无只能辅助分类，不能单独授权数据修改。

## 分类与受控动作

| 事实 | 处理边界 |
|---|---|
| 可领取READY、外部PENDING、有新未发送标记和零尝试 | 新Worker在实际锁定Intent→Outbox→Delivery、核对本次READY来源及有效lease后才能证明未发送并关闭；不能只凭静态清单批量写FAILED。 |
| 旧无标记、重领PROCESSING、曾尝试或缺失来源 | 保持外部结果不确定，进入UNKNOWN/WAITING_RECEIPT待核对；不回READY反复轮询，不清除Redis防重，不重发验证结果。 |
| ACCEPTED、DELIVERED或已有回执事实 | 保持实际投递事实；停止不支持的新发送不意味着追回既有外部消息。 |
| IN_APP消息与本人关系已落库 | 保持或恢复送达事实，不再次创建消息/关系或发送push。只有本地事实确证不存在时才关闭未发送；孤儿关系标记本地异常，不能进入供应商等待。 |
| CANCELLED/EXPIRED等终态或失效租约 | 不复活；旧owner不得写结果或延长期限。 |
| UNKNOWN/WAITING_RECEIPT、不可领取或无Outbox记录 | 进入人工核对清单；新Worker不会自动处理所有静态异常，不宣称启动一次即可消除遗留。 |

上线前先停旧Worker，核对在途请求与租约后再切换。需要修改真实数据时，提供精确主键、原状态、拟动作、备份和回滚限制，再依据独立授权执行；不重放六SQL基座，不把旧priority改0后重发，也不为旧验证码补未来截止。历史通知仍可查询/取消；retry不能绕过当前支持合同。已发送事实不能通过恢复旧版本抹除。

T30发布交接持有目标环境的实际盘点与处置/风险决定，记录总数、各分类、待决数和恢复Worker后的状态。该稿及本地代码/测试不构成生产处置完成或发布授权。
