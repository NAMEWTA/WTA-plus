# T46 集成测试写入中快照复核（非最终验收）

固定审查快照：`/tmp/wta-t46/latest-integration-review-source.java`，SHA256 `85e273553e87871623b79db5bd3c23757cd28478d3530e26a1e0c497f5173c7b`；复制时仓库文件同 SHA，仓库 HEAD `6b8ceb04c5c865d7512290b2e1f0961375ad97c4`，测试文件尚未提交。旧草稿 `/tmp/wta-t46/draft-integration-review.md` 的缺口按此新快照重核。只读，没有运行 Maven、MySQL、MinIO 或服务；不能给 PASS。

固定候选补记：现 HEAD 为 `4f8c4b4aba1c91e273fa47ef4cad345d32a04062`，clean；该提交中的 IntegrationTest SHA256 仍为 `85e273553e87871623b79db5bd3c23757cd28478d3530e26a1e0c497f5173c7b`，与下述审查快照字节相同。以下静态结论可直接绑定固定候选；实际 Maven/owned MinIO 运行尚待 Lead。

## 旧草稿四项缺口的新静态状态

1. **晚到物理 DELETE 与独立对象进度**：`:349-376` 的测试对象存储把实际 `super.delete` 留在独立 provider 线程，调用端 `pending.get(150ms)` 先超时抛错；此时 `releaseDelete` 尚未放行。`:420-426` 要求持久 `CLEANUP_OUTCOME_UNKNOWN`，`:432-446` 在仍未放行的窗口中让对象 102 新建工单/切换到目标，并以配置行 `FOR UPDATE NOWAIT` 错误 3572 证明其短事务确持锁。随后 `:447-458` 才放行 provider 执行**真实 MinIO DELETE**、等待结束，复调用 cleanup 只核对 HEAD，断言只进入一次 delete、来源不存在、目标仍存在。此夹具模拟的是“调用者已失去结果、远端操作稍后才发生”，并非网络层真实 timeout；静态上足以检验持久栅栏与迟到删除结果处理，实际时序须运行证明。
2. **过期预读恢复**：`:413-419` 的 `service.unpublish(101)` 先取得可恢复 item 并在来源 HEAD 之后暂停；`:420-426` 的 cleanup 先领取未知删除栅栏；`:427-431` 放行旧恢复并要求被拒。按 `OssStorageMigrationService.unpublish:54-63`，HEAD 后下一步是 proxied `atomic.restore`；后者 `OssMigrationAtomicService.restore:153-181` 先锁对象、再锁 item/版本并拒 UNKNOWN。因此旧恢复确会走锁内复核，而非在 `findLatestRestorable` 即被过滤。`:385-403` 的另一场景还用真实第二 MySQL 连接、不同 `connection_id()` 和 NOWAIT 3572 验证对象行锁。
3. **CAS=0 回滚和配置保护**：`:222-237` 将 item 更新 CAS 与对象指针 CAS 分别注入一次 0 行，核对事务异常后 `sys_oss.service` 仍为来源、item stage/version 未前进；真实 `@DSTransactional` 代理见 `:538-543`，Mapper 经 `SqlSessionTemplate`/`SpringManagedTransactionFactory` 装配见 `:121-135,521-535`。`:192-203` 在两路对象仍有引用时，调用真实 `SysOssConfigServiceImpl.updateByBo` 修改 endpoint 并要求物理身份保护拒绝，同时 provider delete 数为 0。上述证明仍依赖实际隔离 DB 运行，手装 Spring 代理不等同于完整应用 Bean 选取。
4. **线程与资源**：所有并发 `Future` 都有有界 `get`，`finally :459-466` 无条件放行四个 latch、关闭两个 executor 并要求各自在 10 秒内终止；外层还清理桶/表并关闭数据源。若故障发生在 executor 建立前或 `finally` 的首个终止断言失败，后续清理仍可能被遮盖；这是失败路径卫生的非阻断残余限制，正式 runner 的 owned 容器/卷/端口清理仍需另行取证。

静态审查未发现此快照中能确定阻断 T46 合同的测试缺口。类内仍精确两个 `@Test` 方法：`migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack` 与 `restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource`。冻结 runner 的 2-method selector 必须以未来**固定提交**再次核 hash 和方法清单；本稿不能据此宣布真实测试通过。
