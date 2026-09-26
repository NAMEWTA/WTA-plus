# T36 C2：动态事务提交异常后的同步回调证据

- 固定正式候选：`54cf715c2829ff95cf994401ff52f02668c1e171`；真实运行 `/tmp/wta-t36/runs/0e830cfa64189d7f/result.json`。Atomic 61 例中 3 个断言失败、0 error/skip；Wake 1 例通过。日志 `maven.log` 1176/1177 有新线程 listener 调用，主线程另两例实时计数为 0，因此 factory 并非全局缺失。
- 本地 Maven 依赖源码：`/root/.m2/repository/com/baomidou/dynamic-datasource-spring/4.5.0/dynamic-datasource-spring-4.5.0-sources.jar`；读取 `com/baomidou/dynamic/datasource/tx/TransactionalTemplate.java`、`LocalTxUtil.java`、`TransactionContext.java`、`DsTxListenerMethodAdapter.java`，未修改依赖。
- `TransactionalTemplate.doExecute` 的顺序是 `LocalTxUtil.commit(xid)` → `invokeAfterCommit(shouldInvokeAction)` → `invokeAfterCompletion(..., shouldInvokeAction)`。`invokeAfterCompletion` 才调用 `TransactionContext.removeSynchronizations()`。`shouldInvokeAction` 在事务入口读取 `TransactionContext.getSynchronizations().isEmpty()`。若提交在前一步抛出，之后两步均跳过，线程同步集合残留，后续同线程事务的 `shouldInvokeAction=false`。
- `LocalTxUtil.commit` 在 `finally` 中对无 savepoint 的事务调用 `TransactionContext.remove()`，即清除 XID；即便底层 commit 抛错，调用方代理返回异常时也已不再处于动态事务。`DsTxListenerMethodAdapter` 在发布时只在有 XID 时登记 synchronization，实际回调由 `afterCommit()` 调用。**清理还须同时证明调用前无XID且同步集合为空、异常后无XID**：正常 LocalTxUtil 也先清 XID 再调用 AFTER_COMMIT，此时同步集合有合法回调，不能被重入调用清掉。原异常继续外抛，已提交事实不据异常回滚推断。
- 生产修复必须由同线程 ACK 丢失后下一笔正常站内投递的真实 listener 断言证明；仅在测试 teardown 清理不能证明 Worker 长线程恢复。完全 ACK 丢失的这一笔在线提示允许丢失，已提交 DB 事实不得重复持久化。
