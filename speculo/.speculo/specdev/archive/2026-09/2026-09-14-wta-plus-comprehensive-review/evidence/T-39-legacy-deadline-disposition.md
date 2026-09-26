# T-39：无截止历史验证码的只读分类与发布处置稿

本稿针对 `auth-captcha` 场景、`expires_at IS NULL` 的历史通知。当前源码的验证码有效期为两分钟；历史 `create_time` 不等于验证码缓存的实际创建时刻，也未证明采用 UTC。不能以 `create_time + 2 minutes` 推断旧验证码仍然可用。PersonRebind 是安全告知，不纳入 OTP 清理。Enterprise 转移已有自己的挑战期限，按原期限判断，不能套用验证码时长。

**执行状态：仅制定处置稿，未查询或修改生产数据库，未检查真实收件地址或验证码缓存。** 本票的真实数据库测试只覆盖隔离合成库。以下 SQL 仅供后续发布负责人在确认目标环境后只读盘点，不代表已取得生产结果；禁止把空测试库的结果视为生产无遗留。

## 最小只读盘点

先记录目标环境、候选提交、会话/数据库时区与历史应用时间配置，不输出连接凭据。SQL 输出只含内部主键、时间、状态和计数，禁止输出 `template_params_json`、`metadata_json`、`biz_id`、幂等 key、手机号/邮箱、正文或验证码。

```sql
SELECT @@session.time_zone AS session_time_zone,
       @@global.time_zone AS global_time_zone,
       UTC_TIMESTAMP(6) AS observed_utc;

SELECT app_id, scene_code, status, COUNT(*) AS intent_count,
       MIN(create_time) AS first_recorded_create_time,
       MAX(create_time) AS last_recorded_create_time
FROM notify_intent
WHERE scene_code = 'auth-captcha' AND expires_at IS NULL
GROUP BY app_id, scene_code, status;

SELECT i.intent_id, i.create_time, i.status AS intent_status,
       d.delivery_id, d.channel, d.status AS delivery_status,
       d.attempt_count AS delivery_attempts,
       CASE WHEN d.provider_message_id IS NULL THEN 0 ELSE 1 END AS has_provider_reference,
       o.outbox_id, o.status AS outbox_status, o.attempt_count AS outbox_attempts,
       o.available_at, o.next_attempt_at, o.lease_until
FROM notify_intent i
LEFT JOIN notify_delivery d ON d.intent_id = i.intent_id
LEFT JOIN notify_outbox o ON o.intent_id = i.intent_id AND o.delivery_id = d.delivery_id
WHERE i.scene_code = 'auth-captcha' AND i.expires_at IS NULL
  AND i.intent_id > 0
ORDER BY i.intent_id, d.delivery_id, o.outbox_id
LIMIT 500;
```

第二段明细是首批查看模板，不能因 LIMIT 500 就认定全量完成。大库应按已观察到的完整 Intent 主键批次分页，处理完某 Intent 的全部 delivery/outbox 后再推进水位；不得在同一 Intent 的中途跨页而漏项。汇总总数与遍历到的唯一 Intent 数核对。只读记录保留执行时间、环境标识、源码版本和脱敏计数。

## 分类与处置边界

| 当前事实 | 处置要求 |
|---|---|
| READY/PENDING，且没有外呼证据 | 发布前隔离旧任务的领取，形成明确主键处置清单；获真实数据操作授权后，采用既有锁序/fence/事务进行取消或过期收敛。不能临时补一个未来 expires_at 或重发旧码。 |
| PROCESSING 或存在活/已过期租约 | 可能已开始外呼；仅停止新领取不能撤回在途请求。核对 Worker 和持久结果后再裁决，不按未发送批量改 FAILED。 |
| UNKNOWN / WAITING_RECEIPT / 已有供应商受理证据 | 保留防重与原事实；只能核对回执或授权的供应商只读状态，不能再次发验证码证明它是否可用。 |
| ACCEPTED / DELIVERED / 已终结任务 | 保留审计；不修改已发送事实，不恢复或延长验证码缓存。 |
| 时间来源、尝试史或关联行不完整 | 标为待人工核对；不从最新错误码、null expiry 或缺供应商流水号推断整段历史从未外呼。 |

本次代码只为新命令写入并执行明确截止，普通无期限通知继续支持 null；因此**新代码上线本身不能证明历史无截止 OTP 已安全处置**。正式发布前须有生产只读清单及对应处置/风险决定，再释放相关旧队列。此项作为 T-30 发布与最终验收交接，不由本票擅自执行生产写入。

## 恢复与验收

保留处置前主键/状态的受控备份和批准记录；恢复不能重新启用旧 OTP、清空供应商防重或盲重发 UNKNOWN。让用户重新申请新验证码，走新截止合同。发布记录应说明历史总数、各分类数量、批准的动作、剩余未决数，以及 Worker 恢复后的状态计数；不得把本稿、脚本存在或隔离测试通过写成生产处置完成。
