# T46 真实事务与迁移竞争验收接缝（只读准备）

固定源码：`4a8fea8c19392972ee5cfef4263962ff6b0e3bfb`。依据 Ticket 46/AC-046、`/tmp/wta-t46-implementation-seams.md`、现有 `OssStorageMigrationIntegrationTest` 和已验收的 T45 owned JUnit runner。本文未实施产品、未运行服务或 Maven，也不是验收结果。

## 当前测试能证明什么，缺什么

`backend/wta-admin/src/test/java/org/namewta/test/oss/migration/OssStorageMigrationIntegrationTest.java:57-150` 只有一条 opt-in 测试。它创建真实 MySQL 的五张相关表和两个 MinIO bucket，验证迁移后的源/目标 HEAD、公开 GET、清理与回滚。它把所有 mapper 绑定到 **一个** `factory.openSession(true)` 自动提交 Session，并 `new OssStorageMigrationService(...)`（:94-117）；因此没有 Spring 代理、`@DSTransactional`、跨 mapper 原子提交，也没有两条会争同一 `sys_oss` 行的物理连接。当前顺序调用 `cleanup`/`rollback`（:123-139）不能证明竞争的任一方向。现 `OssMigrationObjectStore.delete` 无超时参数，`DefaultOssMigrationObjectStore.delete` 直接调用 OSS client（分别见接口:9及实现:75），无法可控地暂停/迟到删除。

## 新真实测试最小接缝

1. 仍扩展同一 `OssStorageMigrationIntegrationTest`，保留旧真实双桶正例，新增独立测试方法而不是把旧自动提交 Session 当事务证据。测试构造至少两条实际不同 `CONNECTION_ID()` 的 MySQL 连接，使用 Spring `SqlSessionTemplate`/`SpringManagedTransactionFactory` 绑定 mapper 到动态数据源。代理必须从 Spring/AOP proxy 取得并验证代理身份；公开 `@DSTransactional` 方法经代理调用，不从同类方法自调用。仓内可参考 `NotifyAtomicResultIntegrationTest:100-152,1252-1254` 的 `DynamicRoutingDataSource` 和 `ProxyFactory`/`DynamicDataSourceAnnotationAdvisor(new DynamicLocalTransactionInterceptor(true), DSTransactional.class)` 测试夹具；正式测试应绑定**生产**迁移事务边界、MyBatis store 和 SQL，而非只对一个测试替身加注解。
2. 在事务内 Object→Item 顺序锁定并重读，使用现成 `SysOssMapper.selectByIdForUpdate`（XML:5-9）。新增的 item 锁/CAS 接缝以最终产品实现为准。屏障要证明线程 B 已进入 SQL 并被同一对象行锁阻塞：分别记录连接 ID、有限 `innodb_lock_wait_timeout`/performance-schema 等可用锁等待证据和释放后结果；仅 `CountDownLatch` 证明线程排程不足。两竞争次序都要独立测：恢复先提交则清理零 DELETE；清理预约先提交则 unpublish/rollback/process 不能重挂可能被迟到删除的来源。另测同批不同对象在一个对象 DELETE 延迟时可推进，确认没有整批持锁。
3. 用测试局部的 `OssMigrationObjectStore` decorator 包裹真实 `DefaultOssMigrationObjectStore`，只拦本次 owned 对象的 `delete`，用两个 latch 表示到达/放行。对“调用者超时而远端仍可能晚执行”保留一个独立 worker，在超时后放行一次真实 owned MinIO DELETE；不得把 `Future.cancel` 当成远端撤销证明。测试需在放行前后从新连接读工单持久 UNKNOWN fence、当前 `sys_oss.service`，并以 owned MinIO HEAD/GET 验证目标副本仍可读；未获明确预约提交 ACK 时 DELETE 调用数为零。失败 ACK/零行 CAS 注入分别验证指针和工单同事务回滚，完成 ACK 丢失只能只读核对，不盲目重删或恢复。无须另建 HTTP 延迟代理才能覆盖这些边界，除非产品最终接缝无法由 decorator 拦截。
4. 真实 SDK 单对象 timeout 如扩展到 `wta-common-oss`，须另有针对 request override 的单元断言及 owned 延迟/超时用例；测试只声称有界等待与 UNKNOWN 持久拒绝，不能声称取消使远端 DELETE 必然不执行。Ticket 46 的“明确成功/明确未发/结果不明”须分别断言，确认删除后 DB 提交失败仍保守留 fence。

## 最小 owned runner（待新方法/属性固定后才编写）

复用 `/tmp/wta-t45/run-oss-two-integration.py` 已验收的生命周期，不另建框架：固定 clean HEAD/tree 前后；仅随机 127.0.0.1 端口、双 owner/run label、完整容器 ID 与匿名卷名；MySQL 8.4.9 **专用空库**、MinIO 固定 digest 与本次双 bucket，必要时沿用独占 Redis 8.6.3 以满足 Ticket 环境要求，绝不连接 `/srv/ops`。现有测试 `prepareDatabase()` 自建/删除五张迁移表（:203-214），所以该 selector 本身无需导入六 SQL/103 表；若新 Spring 测试装配依赖完整应用，须先由源码明确再改为 fresh 六 SQL，不能在现测试库上混做。DB app 用户仅本空库 CREATE/DROP/INDEX/SELECT/INSERT/UPDATE/DELETE；MinIO 独占合成 bootstrap 密钥及两随机桶。数据库外连接应与测试 DB 同一 owned 实例。

现有属性键准确为 `oss.migration.mysql.integration.url/.username/.password`、`oss.minio.integration.endpoint/.access-key/.secret-key`，另 runner 的本次 `t46.owned.run` marker 需测试读取并在 fresh XML properties 校验；若测试最终改用 `T46_MYSQL_PASSWORD` 环境入口，需与 Lead/writer 确认后同步，不能猜。当前测试有不安全默认 username/root、MinIO 测试值，正式 runner 必须强制设置每项合成值并在预检拒绝缺项。秘密只进 mode 0600 `surefire.systemPropertiesFile`/容器私有 env-file，不进入 Maven argv、`JAVA_TOOL_OPTIONS` 或公开结果；T44 私有离线 probe 已证 Surefire 3.5.5 属性文件可到 fork。

建议准确选择器：`org.namewta.test.oss.migration.OssStorageMigrationIntegrationTest`（最终新增方法后核名称）；命令形状为 `cd backend && ./mvnw -B -ntp -o -Pdev -pl wta-admin -am -Dtest=OssStorageMigrationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=1 -DreuseForks=false -Dsurefire.systemPropertiesFile=<本次0600文件> test`。不能把“默认跳过”算通过。截取本次启动时点后的 fresh Surefire XML，suite 名完全匹配、每个期望方法正执行、fail/error/skip 全零；旧正例与新双连接/迟到删除方法逐一计数，不把一个方法的绿代替 AC-046。原始 XML/日志保留本次私有 owner 目录；脱敏副本记录原 SHA、脱敏 SHA/次数，已知 secret、签名 query 不外泄，错误类型/方法名可保留供归因。

runner `finally` 延续已验证的进程组 TERM→KILL、派生进程/本次临时目录、全 ID+双 label 容器与预捕获匿名卷逐名不存在、端口关闭、source exact-clean 后验；任一 cleanup/source/fresh XML 门禁失败均 acceptance=false。单次真实执行由 Lead 派单并独占；不内建自动重试。T45 runner 固定两类/各 1 test 的门禁需调整为本类的**实际**方法数，不能原样复制。当前仍缺最终生产事务类/方法、测试方法/marker 与 Redis 是否必要，故不冻结可执行 runner、不宣称通过。
