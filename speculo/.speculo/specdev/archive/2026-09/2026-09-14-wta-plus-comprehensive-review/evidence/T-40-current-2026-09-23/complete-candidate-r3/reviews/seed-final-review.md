# T-40 R3 seed 草案三文件只读审查

输入为 R2 固定 `8a387192e5a7787d1c51731dfc982506207c5482` 对当前工作树三文件 diff；本报告绑定所读字节的 SHA-256：runner `078b0f3a393641d1d174b9bd5400f9acff2815daa815bc41dd19d384656412a5`、离线测试 `ab1bcb6431b0c356142a0a9592347c2afe127f57dc1b3b02f0160583e8190033`、README `28e58620d8f86374982c0f8c6b132e62833690fa5d56f845608720d16a508665`。本轮仅审这三路径；并行治理 Evidence 改动不在此结论。**静态结论：未见阻断，可交 Lead 固定候选并执行原严格验收；目前没有 R3 真实运行结果。** `git diff --check` 在这三路径 exit 0；未运行测试、构建或服务。

- 精确时间锚点：`run-notice-retraction-real.py:612-624` 的 `@stamp` 在原 `START TRANSACTION` 后取 `notify_message_recipient` 中固定 `v1_message_id` 与 A `user_id` 的 `create_time + 1 SECOND`，原 22 条消息与 22 条关系继续共用 `@stamp`，仍 `COMMIT`；不再用 MySQL `NOW(0)`、全局 MAX 或修改真实 V1。`verify_seed_anchor():639-649` 在插入前先验证两个 ID 是正的 Java long，精确查询 `COUNT(*)=1, COUNT(create_time)=1`，缺失、多行、空时间、输出异常一律失败；该 SQL stage 已加入固定白名单。`verify_real_delivery()` 在此前仍证本次 Notice V1、DONE/DELIVERED、A=1/B=0。标量子查询遇多行报错、遇缺行令非空时间插入失败，不能用错误时钟继续验收。
- 排序与门禁：生产 Inbox 的 `NotifyNotificationDao.java:248-255` 和 `verify_seed()` 均按关系 `create_time DESC,message_id DESC` 排序。R3 `verify_seed():710-744` 原 `22/1/22/1/0`、前 20 不含 V1、旧链接前 10 条条件未弱化；`verify_after_browser()` 真实 V1 撤回/关系事实和两 Chrome case/config 对 R2 无差异。R2 合并失败不能证明实际就是时区子条件；R3 的修复能消除这类时钟来源不一致，但需真实浏览器复跑才能判定全部条件。
- 失败输出：新 `OwnedSeedFailure` 对计数字段只接受真正的 `int` 且 `0..1000`（top20 为 `0..20`），布尔字段只接受真正 `bool`，键名固定，其他值变 `None`；`safe_failure()` 只附这一安全字典，不传播异常正文、原行、ID、标题、正文、SQL、时间或 token。`verify_seed()` 的原失败继续抛异常并使 acceptance=false；只将原来不可区分的合并失败拆成有限诊断。原 source/JAR、Playwright 2/0skip、容器/卷/端口/进程组及 raw reporter/log 清理代码均未改。
- 离线覆盖：新增 SQL 精确锚点/no-NOW/no-MAX/no-V1-UPDATE 断言、旧 MySQL 时钟落后 8 小时的排序反例、anchor 缺失/歧义/空时间、错误 counts/top20/V1/legacy，以及伪造 canary、bool 冒充数字、越界数量和额外键不得流出。这里确认的是测试源码与断言质量；尚未把这些测试说成实际执行通过。

候选冻结后应复核三文件 hash 与本报告一致，运行离线测试、fresh full JAR、真实 owned Chrome 两例及全部清理/source 前后门禁；任何失败继续阻止本票验收。没有证据允许推断 R2 MySQL 会话的实际时区值。
