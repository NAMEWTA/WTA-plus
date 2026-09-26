# T-40 R3 合成 seed 时间锚点方案只读审查

输入：R2 固定 source `8a387192e5a7787d1c51731dfc982506207c5482`、`/tmp/wta-t40/browser-runs/318e1b1d8873fa80/result.json`、`/tmp/wta-t40-r2-seed-diagnosis.md`。**结论：认可仅在现有 runner/离线测试内，把合成 22 行的 `@stamp` 锚定为精确 V1+A 已持久收件关系 `create_time + 1 SECOND`；须按下述 fail-closed 检查实现后再固定 R3。** 本审查未修改仓库、运行测试或启动服务。

## 已证与未证边界

R2 真实 HTTP 保存/发布、Worker 向 A 投递 V1、HTTP 撤回、A/B 两个管理接口 403、三次无 UA 及 Chrome 控制登录均通过；失败在 `verify_seed()` 的合并条件（runner `669-689`），Chrome 两例还未启动。`verify_seed()` 同时检查 `22/1/22/1/0`、20 行排序、V1 不在最新 20、旧链接在前 10，因此本次不能实证断言“时区错配就是唯一失败子条件”；安全结果没有保存 MySQL 会话时区、具体时间或各子条件。R2 失败与完整清理证据应原样保留。

## SQL 与 API 合同

- 生产本人列表在 `NotifyNotificationDao.java:248-255` 按 **关系** `r.create_time DESC, r.message_id DESC` 排序，`InAppNotificationService.java:63-71` 给真实 A 关系显式 `LocalDateTime.now()`；`notify_message_recipient.create_time` 是 `DATETIME NOT NULL`，且 `(message_id,user_id)` 有唯一键（`10-cde-base-ddl.sql:1560-1573`）。所以精确关系时间 `DATE_ADD(r.create_time, INTERVAL 1 SECOND)` 与公开 API/`verify_seed()` 的排序列同源，不依赖 MySQL CLI 和后端 JVM 的时区相等。此处无需改 `notify_message.create_time`、业务排序、全局时区或真实 V1 行。
- `verify_real_delivery()` 已从本次 Notice V1 幂等键/Intent、Snapshot、DONE Outbox、DELIVERED IN_APP、消息及 A=1/B=0 关系求得 `v1_message_id`（runner `609-635`）。在写合成行**之前**再以该 `message_id` 和 `a_user` 精确查询 `COUNT(*)=1` 且 `create_time IS NOT NULL`；缺失、歧义、时间空均拒绝，不用全局 `MAX(create_time)`、任意用户关系或 `COALESCE(NOW())` 回退。新增 MySQL stage 若需要，必须进入固定 `MYSQL_STAGES` 白名单。
- 保持 `seed_plan()` 只构造合成 SQL：在其现有 `START TRANSACTION` 后以精确 `(message_id,user_id)` 的标量子查询设置 `@stamp = DATE_ADD(r.create_time, INTERVAL 1 SECOND)`，让原 22 条 `notify_message` 和 22 条关系共用此值，后续原 `COMMIT` 不变。前置查询验证数量和时间；标量子查询遇多行会失败，**在该查询时**缺行得到 NULL 而 `create_time NOT NULL` 写入失败。owned fresh 库没有其他删除 V1 的执行者；不要把这种夹具隔离推导成通用并发删除保证。不要从 Python 读取并公开原时间，也不要改真实 V1 记录。
- 现有 `verify_seed()` 的 `22/1/22/1/0`、A 最新 20 不含 V1、旧链接前 10、B 单独一行及缺失 ID 仍必须原样作为 gate；`verify_after_browser()` 对真实 V1/撤回栅栏及 seed 的后验复查也保留。合成同秒行靠 `message_id DESC` 决胜，A 的 21 条合成比 V1 晚至少 1 秒，最新 20 应为 legacy 与 19 个 filler；这是固定期望，不依赖保存具体时间。

## 安全诊断与离线可验证性

若为区分 R2 这类合并条件失败而扩诊断，只允许固定字段的有限计数和布尔值，例如 `a_rows,b_rows,synthetic_rows,real_v1_rows,absent_rows,top20_count,v1_off_page,legacy_in_top10,synthetic_after_v1`；不保存 ID、标题、正文、原 SQL、时区/时间值、token 或任意异常消息。若保留计数需验证整数范围并避免原始数据库输出进入 `result.json`。这些安全字段仅解释失败，不改变任何条件或将失败升级成通过。

离线测试至少覆盖：生成 SQL 使用精确 V1+A 标量关系时间、无 `NOW(0)`/全局 MAX/时区覆盖且不插入或更新 V1；wrong-clock 反例中旧 MySQL `NOW()+1s` 排在 Java V1 之前而关系锚点排在之后；关系缺失、多行（含模拟唯一约束失效）、时间 NULL 均 fail-closed；原 `22/1/22/1/0`、前 20、前 10 任一错误仍失败；安全诊断不含 canary。离线模型只能证明方案与本地断言，**不能替代** Lead 在新固定源码、fresh owned MySQL/Redis/MinIO/full JAR 上完成 2 个 Chrome case 与全部资源/源码门禁。
