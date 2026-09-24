# T-43 批量写入与动态事务只读审计

固定输入：clean `dd250b947576505425b67ebac0a559c56616952c`，Ticket `ticket/43-measure-notify-fanout.md`/AC-043。读取了 engineering-standards 的项目画像、模块模式、事务/通知规则及 fullstack/module 导航；下述依赖行为来自本机 **MyBatis-Plus 3.5.17** 和 **dynamic-datasource 4.5.0** sources.jar。没有修改仓库、运行构建/测试/服务或测量性能，因此这里是可验证的实现风险与测试设计，不是 T-43 的性能或原子性通过记录。

## 当前调用与成本

- `NotificationApplicationUseCase.java:24-27` 的 public `submit` 是 `@DSTransactional` 代理入口。`NotificationApplicationRuntimeService.java:82-84,111-122,159-216` 插入 Intent 后，对去重用户逐个插入 Recipient，逐渠道插入 Delivery，仅 PENDING 才插 Outbox；缺目标的 Delivery 在插入后再 UPDATE `TARGET_UNAVAILABLE`。现有 `NotifyNotificationDao.java:175-178,243` 均是单行 Mapper 调用。ID 在 Java 侧以 `IdGeneratorUtil.nextLongId()` 明确生成（Runtime:84,166,178,198），无需依赖 JDBC generated-key 回填。Recipient、Delivery、Outbox 的 `@TableId` 见各实体:16/17/21。
- ALL 路径每页读 1000 人、累积 **超过** 100000 才拒绝（Runtime:493-504）。USER/PHONE/EMAIL 显式列表没有该统一人数上限（:486-513）；故 AC 的“现有单次上限”不能被描述成所有入口已有 100000 上限。100/1000/10000 人均在 ALL 上限内，但最高三渠道可形成约 3R 个 Delivery 和最多 3R 个 Outbox。R 个均有效单渠道的插入数源码下界是 `1+3R`，三渠道是 `1+7R`；这些是逻辑调用数，不是实测 SQL 或网络往返。
- `NotifyDispatchResultService.java:363-400,427-438` 每次结果事务更新 Delivery/Attempt/Outbox 后，持 Intent 锁再锁定读取该 Intent **全部** Delivery，用 `NotificationAggregatePolicy` 重算并更新 Intent。回调、settle、deadline 与站内结果也走该重算；`NotificationApplicationRuntimeService.java:233,265-267,355-369,525-533` 的查询、重试、取消及回执消费聚合状态。将它改成普通查询、无锁 GROUP BY 或不精确的增量计数会改变 RR 当前读和这些消费者的语义。若每条结果都扫描 D 条，完整 D 次完成可能形成 O(D²) 读取工作量；是否为实际瓶颈仍待测量。

## `insertBatch → Db.saveBatch` 的精确源码路径

本仓 `BaseMapperPlus.java:142-143,173-174` 的两个 `insertBatch` 默认方法只是转发 `Db.saveBatch(list[,size])`。Notify 的 Delivery Mapper 继承该基类，Recipient/Outbox Mapper 目前继承 MP `BaseMapper`，见 `Notify{Delivery,Recipient,Outbox}Mapper.java:11,9,13`；不能假定三者已有同名便利方法。

本机 source jar：`mybatis-plus-extension-3.5.17-sources.jar` SHA256 `e8a26a7cbb19afa5a6ef938aaa38687465c8317e7b218e3a00ce958d42d3c2fe`。其 `Db.java:74-90` 对空集合返回 false；非空时按实体类取得 Mapper，调用 `baseMapper.insert(entityList,batchSize)` 并把 `BatchResult` 转成 boolean。`SqlHelper.java:129-130` 的 boolean 只检验每个 update count 为正或 `SUCCESS_NO_INFO`；空 `BatchResult` 的 `allMatch` 也为 true，且此返回值不包含期望实体数/实际行数。SQL 异常原则上会抛出，不应被 DAO catch 后当作可继续成功。

`mybatis-plus-core-3.5.17-sources.jar` SHA256 `73b3bc62a892fd08fee44ea80efb1a5312199600ced6bb4207d6a37aefcc7515`：`BaseMapper.java:526-542` 通过当前 Mapper 的 SqlSessionFactory 调用 `MybatisBatchUtils.execute`；`MybatisBatchUtils.java:58-60` 创建 `MybatisBatch`；`MybatisBatch.java:141-155` **另开** `ExecutorType.BATCH` session，逐实体调用映射的 INSERT，每块 `flushStatements()`，在 `autoCommit=false` 时每块调用 `sqlSession.commit()` 并最终 close。默认路径由 `MybatisBatch.java:85-86` 选择 `autoCommit=false`。这是批处理执行与块级 session commit 的源码事实，不能把它说成单个多 VALUES SQL，亦不能直接将 flush 次数当网络往返数。

`dynamic-datasource-spring-4.5.0-sources.jar` SHA256 `8d0725c9538d7ef6204cd670535808072e976c117090c49bc5bbffaed4b9f40a`：`DSTransactional.java:37,51` 默认 `rollbackFor=Exception`、REQUIRED；`TransactionalTemplate.java:107-133` 启动线程 XID，在外层正常返回后 commit、匹配异常后 rollback；`ds/AbstractRoutingDataSource.java:51-60,77-80` 按同线程 XID 和 datasource 名缓存/返回 `ConnectionProxy`；`tx/ConnectionProxy.java:59-90` 的 JDBC `commit/rollback/close` 是空操作，只有外层 `notify` 对真实连接提交/回滚并关闭；`ConnectionFactory.java:102-164` 完成该外层通知。因此**在相同线程、同一个动态路由 DataSource/SqlSessionFactory、同一个 datasource key 且外层代理确已激活时**，独立 BATCH session 应取得同一个 XID 的连接代理，中途 session commit 不会物理提交。这是源码条件推论，尚未证明本项目实际 Bean/路由、异常翻译和所有注入点满足条件；特别不能仅凭 `Db.saveBatch=true` 保证同事务、完整行数或正确 ID 关系。MyBatis Spring `SpringCompatibleSet.java:52-58` 为 `SqlHelper.execute` 获取普通 SqlSession，但其 `executeBatch` 方法不是这里 `BaseMapper.insert(Collection)` 使用的执行路径，不能混淆。

## 关键风险与最小可执行验证

1. **原子性为实施阻断门槛。** 在真实 full-context `NotificationApplicationUseCase.submit` 的动态代理下，以 owned MySQL 记录同一调用的实际 JDBC `CONNECTION_ID()`/datasource key：Intent 单行前后、Recipient/Delivery/Outbox 的 BATCH flush 以及后续读取应在同一物理连接/XID；另一个连接在提交前看不到批量行。分别在非首块 Recipient、Delivery、Outbox 的 `executeBatch` 或约束冲突注入一次确定失败；完整提交异常后，第二连接应见 Intent/三表/附件关系与源引用均零残留，幂等键可安全再交，无 AFTER_COMMIT wake 或 Provider 调用。还要在批量写完、外层返回前注入失败，以证明块级 `sqlSession.commit()` 没有提前物理提交。必要时用窄 JDBC delegate 记录连接 ID/提交/回滚事件，不能用只观察业务 mock 的单测替代。
2. **返回值与关系完整性。** 不把 `Db.saveBatch` 的 boolean 当精确 rowcount；若改用 `BaseMapper.insert(Collection,size)` 的 `BatchResult`，至少核 update-count 项目总数等于输入数、每项为正或 `SUCCESS_NO_INFO`，并以同事务内及提交后的 `COUNT`/唯一键/外键事实验证 R 个 Recipient、R×C 个 Delivery、正确的 PENDING 数对应 Outbox，所有预分配 ID 与关系 FK 精确一致。`SUCCESS_NO_INFO` 不能自行证明每项恰好一行。缺目标仍需 `UNDELIVERABLE` + `TARGET_UNAVAILABLE` 且零 Outbox；批量化可在插入前形成该最终状态，但不能忘记当前 UPDATE 所设的错误字段或外部 UNSENT marker。
3. **聚合、锁和幂等回归。** 保留 `Intent→Outbox→Delivery` 及聚合的 Intent→全部 Delivery 锁序、租约 token/version 谓词、RR 当前读。真实两连接测试要让一个线程持 Intent 锁，另一个结果事务可证明等待并在释放后读到最新 Delivery；旧 owner、取消与回调交错、UNKNOWN/ACCEPTED/DELIVERED/FAILED 混合、任一写阶段回滚都与当前聚合策略和公开 query/receipt/retry/cancel 一致。重复提交同幂等键或收件人/渠道唯一键碰撞不得被 `INSERT IGNORE`/吞异常变成部分成功。若批量提交已经显著减少成本，可保留现有聚合实现；未测前不要加新计数状态机。
4. **测量而非推测加速。** 同一真实 MySQL/Connector/J 版本、同一 JDBC 选项和合成数据，先测 dd250b 基线，再测候选 100/1000/10000 人及至少全有效、缺目标两种组合。记录 client execute/executeBatch 与 server statement/round-trip 指标、提交耗时分位、事务/Intent 锁等待、峰值内存、预期/实得各表与聚合状态。`application-dev.yml:56`、`application-prod.yml:59` 和 Docker 后端 URL 都含 `rewriteBatchedStatements=true`，但 owned 测试 URL 必须实际带同一选项并在证据中脱敏记录；不能把生产配置自动套到私有 runner。没有竞争负载时零锁等待只说明该样本未观察到等待。`ALL` 的 100001 边界应在写 Intent 前拒绝；如本票要覆盖显式 USER/PHONE/EMAIL 的统一上限，先明确该合同和测试，而不是在报告中假称现有实现已有此限制。

当前结论：`Db.saveBatch` **可能**在既有动态事务内安全工作，源码给出有条件的连接共享机制；仍必须以真实 MySQL 的 mid-batch/late-failure、跨连接可见性及 ID/聚合断言闭环。未经该验证，不建议将 `insertBatch=true` 直接认作 T-43 的原子批量写入；本审计没有运行任何门禁，AC-043 仍未验收。

## ALL 人数边界与混合目标的最小补充矩阵

现有 `resolveUsers` 在每次 `selectAllActiveUsers(offset,1000)` **加入本页后**判断 `recipients.size()>100000`（Runtime:493-503），且未见 100000/100001 的现行边界用例。以可控 `UserService` 替身按 offset 返回排序稳定且 ID 唯一的 1000 人页，测试须调用完整 `NotificationApplicationUseCase.submit`/Runtime.submit，而非反射私有 `resolveUsers`：

- **100001 拒绝**：offset 0..99000 各返回1000，offset100000返回1。命令选 ALL、无显式 recipientIds、单渠道、未来计划时间；完整 submit 应抛“超过单次发送上限”。在 owned MySQL 的另一连接核 Intent/Recipient/Delivery/Outbox 对该幂等键及测试 ID 都为0，并核无 AFTER_COMMIT wake/Provider；用 DAO spy 时至少断言四表 insert 方法均未调用。该拒绝发生于任何 Intent insert 前，因而是确认“零业务写入”最直接的边界用例。不要把前面的用户分页 SELECT 计入“零业务 SQL”。
- **100000 保留**：恰好100页×1000，offset100000再返回空页；同样经完整 submit，不能把 `>=100000` 误当超限。最严格的端到端方案是在 owned MySQL 以单渠道/未来任务实际提交后核100000 Recipient、100000 Delivery、相应 Outbox 与一条 Intent，全部 ID/FK 唯一；这是一个重负载**正确性**用例，应有专门资源/超时预算，时延不进入100/1000/10000性能比较。若先用 recording DAO 做轻量 full-method 边界测试，只能证明业务分支接受了100000，不能称数据库规模原子性已验证；真实规模验收仍需独立保留。
- **混合 UNDELIVERABLE 控制**：另用少量 EMAIL 收件人、`channels=[MAIL,SMS]`，其中 MAIL 地址有效、SMS 目标为空。每人应有1 Recipient、2 Delivery（MAIL `PENDING` 与 SMS `UNDELIVERABLE` 且 `TARGET_UNAVAILABLE`）、仅1个 MAIL Outbox；无 SMS Outbox、无错误目标外发。对比批量前后持久行数、回执顺序及聚合状态，尤其不能因只批插入 PENDING 行漏掉 UNDELIVERABLE 或错误码。这个小样本用于语义控制，不代替10000人批量成本测量。
