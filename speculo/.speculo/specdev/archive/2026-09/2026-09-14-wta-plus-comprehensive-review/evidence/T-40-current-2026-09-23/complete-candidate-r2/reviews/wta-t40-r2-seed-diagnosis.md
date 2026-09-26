# T40 R2 合成 seed 排序失败只读诊断（2026-09-23）

输入：`/tmp/wta-t40/browser-runs/318e1b1d8873fa80/result.json`，固定 source `8a387192e5a7787d1c51731dfc982506207c5482`、full JAR `c2b023231d0713b1c17ca9940b94a6937af53b627205d574151ad589126d79a9`。只读安全结果、runner 与生产源码；未读取 raw log、修改仓库、运行测试或启动服务。

## 已证事实及尚不可证部分

- 三次无 User-Agent 登录的本人在线会话和异步审计、固定 Chrome UA 控制登录、真实 HTTP 保存/发布、Worker 为 A 送达 V1、真实 HTTP 撤回，以及普通 A/B 对公告管理 GET 的两个 403 均已通过。本轮不会再归因于版本栅栏或登录。
- 失败发生在合成补充行插入后的 `verify_seed`（runner 第 689 行），其条件合并检查 A/B 行数、合成 ID 区间行数、真实 V1/缺失 ID、A 最新 20 条排序、旧链接是否在前 10 条；安全结果未保留各子条件或时间戳，故**不能声称已实测**哪一子条件为 false。
- 资源全清；源码/JAR 前后一致。两个 Chrome case 尚未执行。

## 时钟来源错配是高置信候选

生产 `InAppNotificationService.persist` 给新本人关系显式 `setCreateTime(LocalDateTime.now())`；消息的 `BaseEntity.createTime` 由 `InjectionMetaObjectHandler.insertFill` 在未设置时取 `LocalDateTime.now()`。本轮后端 Java 子进程由 runner 显式设 `TZ=Asia/Shanghai`。两列是 MySQL `datetime`，保存无时区的本地时间值。

合成 seed 则由 Python 通过 `docker exec` 内的 MySQL CLI 发送 `SET @stamp = DATE_ADD(NOW(0), INTERVAL 1 SECOND)`，同一 `@stamp` 写 22 条合成消息及关系。owned MySQL `docker run mysql:8.4.9` 没有传 `TZ`、`--default-time-zone` 或 `SET time_zone`；六 SQL 初始化脚本也没有改变时区。`NOW()` 取该 SQL 会话时区，而不是 Java 进程的 `TZ`。生产 Outbox 的数据库时钟又明确使用 `utc_timestamp(6)`，但本次 inbox 关系时间使用 Java `LocalDateTime.now()`。由此，若容器/CLI 会话为常见 UTC，真实 V1 关系 `create_time` 比合成 `NOW()+1s` 晚约 8 小时，V1 会进入 A 的最新 20 条，触发 `str(v1) in rows`，与失败位置相符。

安全结果没有保留 `@@session.time_zone`、`@@system_time_zone` 或两类 `create_time`；不能把“容器/CLI 确为 UTC”提升为本轮实证。`serverTimezone=Asia/Shanghai` 只出现在**后端 JDBC URL**，并非 Python 的 `docker exec mysql` 会话配置；不能据此认定两个写时钟相同。其他 count/seed 子条件仍保留为未排除候选。

## 最小 R3 runner-only 修正建议

保留真实 V1 由 HTTP 发布与 Worker 持久化，既不改业务时间、全局时区，也不改 V1 关系。合成 seed 的 `@stamp` 改为从**已持久化且归属 A 的真实 V1 关系**取时间：

```sql
SET @stamp = (
  SELECT DATE_ADD(MAX(create_time), INTERVAL 1 SECOND)
  FROM notify_message_recipient
  WHERE message_id = <本次真实 V1 messageId> AND user_id = <A userId>
);
```

前置已有 `verify_real_delivery` 严格证明此 messageId 来自当前 Notice V1 且只有 A 本人关系、B 无关系。仍建议在 seed 同一事务中对该精确关系 `COUNT(*)=1`、`create_time IS NOT NULL`、`@stamp IS NOT NULL` fail-closed 后才插入；不要使用任意全局 MAX 或 `COALESCE(NOW())` 掩盖缺失 V1。关系写于消息之后，因此关系时间是可靠的最新下界；若要完全抵抗将来消息时间实现改变，可锚定 `GREATEST(message.create_time,recipient.create_time)+1s`，但必须保持精确 V1 与 A 归属 JOIN。

原有 `verify_seed` 的 `22/1/22/1/0` 行数、A 最新 20 条不含真实 V1、旧链接前 10 条和 B 隔离断言须原样保留。离线测试应断言 seed SQL 使用精确 V1/A 关系锚点、不再调用 `NOW(0)`，并给合成时钟与 V1 相差 8 小时的排序反例。失败安全证据若要增诊断，只留各子条件的布尔值/有限计数与 `synthetic_after_v1` 布尔值，不保留原始行、ID、正文、token、SQL 或时间值。无需新增生产、DDL/迁移、配置或时区写集；现有 runner 与同文件 Python 离线测试两路径足够。
