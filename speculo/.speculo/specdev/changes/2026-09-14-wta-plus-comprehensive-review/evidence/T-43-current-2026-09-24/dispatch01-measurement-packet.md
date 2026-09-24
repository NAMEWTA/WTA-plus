# T43 Dispatch01：先测量，后决定优化（只读派单草稿）

固定输入：clean `d95b464e46ec73d7a913809f4c84332a7f2eeffb`；Ticket `ticket/43-measure-notify-fanout.md`、AC-043 与当前代码。此稿没有运行测量、构建或服务，也没有基线数值、改进百分比或新 SLA。第一批**只新增测试/私有驱动，不改生产写入与聚合**。预检 `/tmp/wta-t43-current-preflight.md` 所述逐行写入仍在；d95 相对旧预检基线仅触及 runtime 附件分支，空附件样本不经过 OSS。

## 首个可执行切片与精确路径

现有 Ticket 写根 `backend/wta-admin/src/test/java/org/namewta/test/notify/` 足以容纳唯一新增仓内文件：`backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyFanoutMeasurementIntegrationTest.java`。建议 Lead 在 Dispatch01 的精确清单中点名此路径；其中放私有 `OwnedJdbcProbe`、只在此类生效的 MyBatis 计数器、合成数据与故障夹具，暂不扩生产、POM、SQL 基座、HTTP/API 或监控路径。独占容器驱动只写 `/tmp/wta-t43/`，与 T42 owned full-app runner 相同的 source-before/after、镜像/SQL哈希、子进程属性文件0600、PGID/端口/卷与 cleanup 证明；不把私有工具当仓内产品。若以后测量表明必须触及 `NotifyNoticePublisherService`、`NotifyNoticeUseCase` 或 System 用户目录实现，它们**不在本票当前生产写集**，须先报告并登记。

新类采用 `@SpringBootTest(NamewtaApplication, webEnvironment=MOCK)`、`@EnabledIfSystemProperty(named="notify.fanout.measurement",matches="true")`，注入真实 `NotifyNoticeUseCase`、`NotificationApplicationService`、`NotifyOutboxClaimPort`、`NotifyDispatchPort`、`DynamicRoutingDataSource`、`JdbcTemplate`。不 mock 这些链路，不直接调用 DAO 伪装发布。复用 T42 full-context 成熟的 owned datasource 校验和 Redis 接线；测试入口先确认 `t43.owned.run`、固定 loopback `t43.mysql.url`/Redis、六 SQL 新库当前104表。`NotifyOutboxWakeSubscriber.destroy()` 加 `notify.outbox.poll-delay-ms=3600000`，只在该独占测试 JVM 手工 claim/dispatch，避免异步 Worker 混入读数；只用 `IN_APP` 发布，不接 SMTP/SMS/OSS。若 Boot 初始化需要 OSS 配置，提供 owned inert endpoint 并断言测试阶段零 OSS 请求，不能替换通知上游 Bean。所有合成用户及公告仅存在本次独占库。

第一批实际方法（JUnit XML 必须逐名非零执行、0 skip）：

1. `measurePublishedAllInAppFanout()`：读取 `t43.fanout.size`，仅接受 **100/1000/10000**；在新库先记录六 SQL 自带的正常用户 ID，按固定 ID 区间用 JDBC batch **在计时窗外**补到总正常且未删用户数恰 N，核提交前实际目录集合恰 N。插入 `ALL`、`IN_APP` 草稿，然后仅计时一次真实 `NotifyNoticeUseCase.publish(id)` 及其事务返回。测试 SQL 包含合成标题/邮件但日志与结果只输出类别/计数。每档测后核 Notice PUBLISHED、V1 Snapshot=1、Intent=1、Recipient=N 且 distinct(user_id/key)=N、Delivery=N、Outbox=N、Attempt=0、聚合 QUEUED；三处 IN_APP path 仍指本人 Intent `messageId`，没有提前持久 IN_APP 消息。对 `notify_recipient`/delivery/outbox 显式 ID 唯一性和无孤行用 SQL COUNT 校验，不把 Mapper 调用数冒充 JDBC 往返。无附件时引用/OSS 请求均零。
2. `measureBoundedInAppResultAggregation()`：在同样 N 的已发布独占样本上，经真实 claim port 领取，再经真实 dispatch port 完成固定 **10 个** IN_APP delivery；只计这 10 次完成窗口，逐次核有效 owner/token、消息/本人关系各增一、无重复推送，Intent 聚合保持与当前全部 Delivery 状态一致。JDBC 探针记录每次 `lockDeliveries` 返回行数、实际 `SELECT ... FOR UPDATE` 执行次数/耗时和总映射行数。**10 次样本不是全部 N 个完成成本**；额外 N=100 全量 drain 的正确性用例或既有原子结果回归证明最终 DELIVERED，不从 10 次外推虚假实测总耗时。
3. `failedFanoutInsertRollsBackNoticeAndWake()`：独占 MySQL 在 `notify_recipient` 针对固定第 1001 个合成 user_id 的 BEFORE INSERT trigger `SIGNAL`；从真实 Notice UseCase 发布 N=10000，故障在已写多行之后产生。异常应沿原动态事务外溢；Notice 留 DRAFT，Snapshot/Intent/Recipient/Delivery/Outbox/Attempt 的本次键均零，Redis wake publisher 的 AFTER_COMMIT 方法无本次调用。finally 删除 owned trigger，即使断言失败也不污染后续样本。B 的批大小确定后，在同一方法参数/独立测试中再核首行、batch 尾及下一批首行、末批（和 Delivery/Outbox 中途错误），不得让 `insertBatch` 的布尔返回或 flush 成功掩盖部分提交。

选择器示例：`-Dtest=NotifyFanoutMeasurementIntegrationTest#measurePublishedAllInAppFanout`、`#measureBoundedInAppResultAggregation`、`#failedFanoutInsertRollsBackNoticeAndWake`，并设置 `-Dnotify.fanout.measurement=true -Dt43.fanout.size=<N>`；由 Lead 按项目 `./mvnw`/私有 runner 串行运行，测试自身不启动 Docker。另在现有 admin notify 测试根补一条小规模负例或复用已登记 runtime 测试：相同公告版本/幂等键重复发布仍只一 Intent/一组关系；100000 有效目标上限保持、100001 被拒且零业务写入。超限目录可用受控 `UserService` 分页替身测**边界语义**，不能把该替身的耗时报成真实目录成本。混合 `IN_APP+MAIL`（部分缺 email，产生 `UNDELIVERABLE` 和额外更新）作为第二份同源 A/B 矩阵，先在 N=100 执行，确保未来批量实现没有漏 D/Q/U；不物理调用 MAIL sender。

## 测量钩子与安全记录

测试私有 `OwnedJdbcProbe` 仿照现有 `OwnedAttachmentJdbcFaults` 的安全接缝：在无活跃 Worker 的专用 JVM 中，以 `DynamicRoutingDataSource.getDataSources().replace("master",old,probe)` 安装 `DelegatingDataSource`，仅拦本次 Connection/PreparedStatement/ResultSet，`finally` CAS 还原，**不关闭原池**。阶段 ThreadLocal 区分 SETUP、PUBLISH、RESULT、ASSERT/CLEANUP；只统计 PUBLISH/RESULT。预处理 SQL 仅在内存按受限表名和 DML/SELECT/FOR UPDATE 类别归类，不输出 SQL、BoundSql、绑定值、正文、邮箱或异常消息；拦 `executeQuery`/`executeUpdate`/`executeBatch`、`addBatch`、commit/rollback 与 `ResultSet.next`，区分真实 execute 往返、batch flush 数量/每批参数项、数据库返回行数。另在本类的真实 MyBatis Configuration 安装仅测量期的 `Executor` Interceptor，按白名单 Mapper ID/SqlCommandType 统计方法调用与返回 List.size；启动探针后执行一条已知 Mapper 查询作正控制，若 Mapper/JDBC 两层计数任何一层为零或与 SQL 类别无法对应，则**测试失败而非报告零成本**。A/B 使用同一测试文件与探针哈希。

`System.nanoTime` 计真实发布事务返回、10 次结果完成和各阶段 SQL 延迟直方；固定 JVM 堆，记录 GC 次数/暂停差量、堆峰值，若 `ThreadMXBean` allocated bytes 可用则记录，否则明确 unavailable。独占 MySQL 在阶段前后读取 `Innodb_row_lock_waits/time` 及 Server 版本、事务隔离级别；这些是**实例阶段差量**而非单条事务的精确锁等待。可追加单独双连接 Intent `FOR UPDATE` 屏障验证真实锁顺序，不掺进发布成本样本。若性能_schema/全局状态不可读，记 null/原因并补受控只读 DBA sidecar，不填伪零、不声称锁等待已测。安全 JSON 每档只含 N/R/D/Q/U、执行/批次数、返回行数、延迟和内存数字、枚举环境，保留 source/SQL/driver SHA 与 fresh XML 计数；不存原始 SQL、目标地址或 token。

## 同输入 A/B 与继续施工门槛

**A** 用未优化 d95（或 Lead 激活时记录的同源更新 SHA）先跑；不先改生产。每档 fresh 独占 MySQL 8.4/必要 Redis，六 SQL 哈希、镜像 digest、JDK/堆/连接池、隔离级别、系统时区、合成用户 ID/状态、公告正文长度/渠道、测量探针 SHA、warm-up 及有界超时一致。每档先做小样本 unmeasured warm-up；A/B 对 N=100/1000/10000 各至少三个全新库重复，记录逐次值/中位数/范围，不把极少重复包装成 SLA/p95。B 只能在 A 结果显示可定位瓶颈后作非空产品候选，运行**同一字节测试与驱动**；失败或超时作为该档结果保留，不挑成功样本或改变 N。第一轮 `IN_APP` 纯扇出为主，N=100 混合渠道作为 D/Q/U 结构控制；若主要成本实测在结果全量锁读，先评估锁正确性与可优化性，不凭静态 O(D²) 直接改聚合。

当前 Ticket 已登记未来可能的 `NotificationApplicationRuntimeService.java`、`NotifyDispatchResultService.java`、`NotifyNotificationDao.java`、Notify Mapper/XML 和两测试根。只有 A 的 JDBC/Mapper分层数字表明插入往返占主因时，才在这些路径选择 Recipient/Delivery/Outbox 分批写，并以显式 Snowflake ID、原稳定顺序、同一 `@DSTransactional`、UNDELIVERABLE 状态更新、所有 batch flush/受影响行失败回滚为硬门禁。`BaseMapperPlus.insertBatch` 是候选，须用真实 JDBC 批次计数与中途异常验证，不能把它的 `boolean` 当作已证明全量插入。若测量不足，不改聚合/新增计数表，只报告观测成本与上限。
