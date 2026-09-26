# T46 real01 ACK 注入失败只读诊断

固定受测源码：`c30391d0a559bed30a81b04058fbe4c13d563544`。输入仅 `/tmp/wta-t46/oss-migration-runs/60ecc38cb9c20055/result.json`、同目录**脱敏** XML/`maven.log`，固定源码 `git show c30391d0:...` 和本机已安装 `dynamic-datasource-spring 4.5.0`、MyBatis Spring / MyBatis 源码 JAR。未读取原始 secret/props，未运行 Maven、DB、MinIO 或服务，未改仓库。当前 HEAD 后来进入治理提交，不影响此 run 的固定源。

## 实证范围

real01 源前后同 clean c30391d0；JUnit 两方法新鲜执行，`restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource` 通过，首方法第 191 行失败：预期 `CLEANUP_OUTCOME_UNKNOWN`，DB 实得 `null`。总计 2 tests/1 failure/0 error/0 skip，Maven 1，所有 owned 容器/卷/端口/进程 cleanup 无错误。首方法在第 188 行 `assertThatThrownBy(service.cleanup(...))` 已接受某个 RuntimeException，但现有脱敏 XML**没有记录这个被断言捕获的异常类型/调用阶段**；因此“提前耗掉注入”是强机制推断，不是日志直接证实的具体 commit 栈。

## 为什么当前故障开关会落在错误提交

首方法 `:118-132` 把所有物理连接的 `commit()` 包装为全局 `loseCommitAcknowledgement.compareAndSet(true,false)`，并在调用 `service.cleanup` **之前** `:187` 置位。`OssStorageMigrationService.cleanup:178-179` 首先在原子短事务外执行 `requireBatch`/`store.listItems`；真正写 UNKNOWN 的 `atomic.reserveCleanup` 要到 `:195-196` 才执行。

本机安装源组成一个可重复解释：

1. DS 4.5.0 `AbstractRoutingDataSource.getConnection` 仅在有 XID 时返回 `ConnectionProxy`；`ConnectionFactory.putConnection` 对底层连接执行 `setAutoCommit(false)`；`LocalTxUtil.commit` 最终调用 `ConnectionProxy.notify` 中的**物理** `connection.commit()`。
2. MyBatis `PooledDataSource.pushConnection` 在回池时对 `!autoCommit` 只做 `rollback()`，**没有重置为 autoCommit=true**。前面的真实迁移短事务留下这种可复用连接。`popConnection` 再取它时也不恢复 autoCommit。
3. MyBatis Spring `SqlSessionTemplate.SqlSessionInterceptor` 对非 Spring 事务的 mapper 调用强制 `sqlSession.commit(true)`；`SpringManagedTransaction.commit()` 在连接不属于 Spring TX 且 `autoCommit=false` 时对该物理连接执行 `commit()`。于是上述 cleanup 前的批次/工单预读足以触发 wrapper，将全局注入开关消费并抛异常，`assertThatThrownBy` 仍满足，而 UNKNOWN 更新尚未执行。生产环境若使用会重置状态的连接池，其触发机会可能不同；这仍是夹具注入时机不精确的问题，不能据此判断产品 reserve 事务有错。

## 最小正确修复条件（测试夹具，不改业务语义）

- 将 `:187` 的直接置位改为 `armOnUnknownPersist.set(true)`，只在本测试 `MybatisOssMigrationStore.updateItem` override 中，**`super.updateItem` 返回 true 之后**，且 item 的 `lastErrorStage=COMPLETED`、`errorMessage=CLEANUP_OUTCOME_UNKNOWN`、当前 DS XID 非空时，单次将 `loseCommitAcknowledgement` 置 true。这样写入已在持久化 reservation 的真实事务中完成，下一次物理 commit 就是该事务 ACK。不要在 `getBatch`/`listItems` 预读处 arm，也不要仅通过延后到 `service.cleanup` 内某个行号猜测。
- 为使反例具判别力，夹具分别记录“目标 marker 更新成功并 arm 一次”和“目标事务物理 commit 被 wrapper 注入一次”的安全计数/布尔；wrapper 在注入分支要求 XID 非空，必要时记录 owned DB 的 `connection_id()` 并与成功更新后的同事务连接核同一 ID。不要记录凭据、SQL/对象键，也不要把 `assertThatThrownBy` 放宽成任意提前 RuntimeException。
- 保留现有 `UNKNOWN` 持久标记、provider DELETE=0、复核后的手动源缺失/目标存在、CAS 回滚及第二并发方法全部断言。再次真实执行时，若仍是 null，须保存安全的被捕异常类名和是否到达 marker-update/commit 的阶段计数，以区分 fixture 与真实产品问题；旧 real01 失败不可追认。

这只是诊断和修复条件，不是新候选的运行结果。
