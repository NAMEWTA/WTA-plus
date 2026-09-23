# T-36 dynamic-datasource 4.5.0 事务事件遗留同步：只读专项预审

范围：仅审当前已装依赖与 T-36 dispatch 的局部修复位置；未改 repo、未运行 Maven/Docker/服务。实际 Maven 依赖版本来自 `backend/pom.xml:32,204-208`。核对的源码包 `/root/.m2/repository/com/baomidou/dynamic-datasource-spring/4.5.0/dynamic-datasource-spring-4.5.0-sources.jar` SHA256 `8d0725c9538d7ef6204cd670535808072e976c117090c49bc5bbffaed4b9f40a`，实际 class jar SHA256 `c1d8d6b965cf5a21107ecbe427cf11dff340141551549ad16c2223395ad43d64`；同时对 class jar 执行 `javap -c -p` 核对同一控制流。源码行号以下均指 sources jar 内类。

## 根因确认

- `TransactionalTemplate.doExecute:107-131` 在 `LocalTxUtil.startTransaction()` 后以 `TransactionContext.getSynchronizations().isEmpty()` 一次性计算 `shouldInvokeAction` (`:116`)。`LocalTxUtil.commit(xid)` 位于 `invokeAfterCommit` 和 `invokeAfterCompletion` 之前 (`:126-128`)；`invokeAfterCompletion:278-283` 才执行 `TransactionContext.removeSynchronizations()`，且只有 `shouldInvokeAction=true` 才执行。class jar 的 `javap` 对应 bci 46–54、81–99 及 152–176，确认非 sources jar 单独漂移。
- `LocalTxUtil.commit:88-98` 在 top-level `finally` 移除 XID，即使 `ConnectionFactory.notify`/JDBC commit 抛异常也移除；class jar `javap` bci 5–59 同样显示异常表 finally 中 `TransactionContext.remove()`。`TransactionContext:38,80-112` 的 XID 和同步集合是两个独立 ThreadLocal，`getSynchronizations` 读旧集合，`removeSynchronizations` 才清它。若 commit 抛错，`TransactionalTemplate` 跳过 `invokeAfterCompletion`，于是同线程留下 **XID=null、同步集合非空**。
- 下一笔 top-level `doExecute` 的 `shouldInvokeAction=false`，即使新事务中 `DsTxListenerMethodAdapter.onApplicationEvent:44-47` 又注册新事件，提交后 `invokeAfterCommit(false)` 与 `invokeAfterCompletion(false)` 均不执行；旧集合持续滞留。这足以解释同线程后续站内 `@DsTxEventListener(AFTER_COMMIT)` 提示为零，但不能单凭静态源码断言当前 3 个失败全由此造成。

## T-36 局部修复的安全边界

1. **只在本地 dispatch 对被代理事务调用的失败出口清理。** `DispatchNotificationService.dispatchInApp` 当前 `resultPort.beginInAppAttempt` / `resultPort.completeInApp` 在 `:207-209`，其中 `completeInApp` 会于结果事务内发布 `InAppDeliveryCommittedEvent`。在进入单次代理调用之前记录“调用前无活 XID 且同步集合为空”；仅该调用异常越过代理后，且调用后 `TransactionContext.getXID()==null` 时，若同步集合非空则 `TransactionContext.removeSynchronizations()`，然后**原样重抛原异常**。这样不会把 SQL/提交不确定性误写为 FAILED/UNKNOWN，也不改独立预算和持久结果。预算调用如统一封装可套相同门槛；它目前不发布事件，最直接污染源是 `completeInApp` 的失败提交。清理失败只作为原异常的 suppressed，不以清理异常遮蔽提交异常。
2. **活 XID 或调用前已有同步，绝不能清。** `REQUIRED` 参加外层事务、`NESTED` savepoint、`REQUIRES_NEW` 恢复外层 XID 时，调用后 XID 可能非空；此时同步集合可能属于仍有效的外层事务。即使 `getXID()==null`，也不代表同步一定遗留：正常 top-level `LocalTxUtil.commit` 先清 XID，随后正在运行 `AFTER_COMMIT`/`AFTER_COMPLETION` 回调。回调内重入 dispatch 时，若在入口按“无 XID”泛清，会删除当前合法同步。因此不要在通用 dispatch 入口、`@BeforeEach` 之外的全局拦截器或 `getXID()==null` 的所有路径无条件清理。
3. **不手动移除/重绑 XID，不吞回调。** `LocalTxUtil` 本身负责 XID；局部代码仅移除本次失败造成的同步 ThreadLocal。不要手动调用旧 `afterCommit` 或 `afterCompletion`：BEFORE 情况数据库已回滚，AFTER 情况数据库可能已提交，但提交 ACK 不确定，回调不能从异常安全推断。在线提示是可丢的提交后副作用，持久消息/关系和 outbox 的事实才是恢复依据。
4. **夹具隔离与回归。** 在 `NotifyAtomicResultIntegrationTest` 的每例 finally/teardown，只在无活 XID 时清该测试线程的遗留同步集合，防止一个 commit-fault case 污染后续用例；有活 XID 则先报告测试失败，不能掩盖事务泄漏。另在**同一个测试线程、同一个测试方法**执行 commit BEFORE/AFTER 故障→原异常外溢→确认同步集合已清→下一笔独立 IN_APP 正常提交并触发恰一次新 AFTER_COMMIT；旧失败事务事件绝不迟发。分别断言 BEFORE 零持久消息，AFTER 已持久且不重发，下一笔回调为新消息/用户。仅 `@AfterEach` 清理不能替代这个产品回归。

## 本票保证与残余影响

该局部 catch 只能消除 **T-36 自己在无外层事务、调用前同步集合干净时**因代理提交失败而遗留的同步，保护同 worker 线程下一笔 dispatch。它不是 dynamic-datasource 的全局事务修复。其他 UseCase（例如 callback）若直接提交失败，也会留下无 XID 同步；下一笔恰好落在同线程的 dispatch，若入口不清理，仍可能使 `shouldInvokeAction=false`。不能为覆盖此情况在 dispatch 入口盲清，因为 no-XID+sync 可是合法的 AFTER_COMMIT 阶段。应在 T-22/T-30 的最终复验或独立框架修复记录此残余，并由该范围设计可识别事务 ownership 的全局恢复机制。T-36 如需防止继承旧污染后静默漏提示，可以在已有无 XID+非空同步的入口**拒绝新 IN_APP 执行并记录安全类别**，但不能把它当作成功恢复，也不应在未扩写集/测试前插入广域清理。

结论：writer 所述根因由已装 4.5.0 sources jar 和实际字节码双重证实。建议只实施带“调用前干净 + 失败后无活 XID”的窄清理，原异常外溢；用同线程故障→下一笔真实 AFTER_COMMIT 验证。这个专项预审不对候选2做 PASS 判断。
