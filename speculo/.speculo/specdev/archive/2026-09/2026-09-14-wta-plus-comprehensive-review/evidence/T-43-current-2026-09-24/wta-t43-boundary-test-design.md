# T-43 边界与批量结果校验最小测试设计（只读）

固定基座：clean `dd250b947576505425b67ebac0a559c56616952c`。本文件只设计未来测试；未修改仓库或运行测试。目标仅为 `NotificationApplicationRuntimeService` 的 ALL 100000/100001 既有边界，以及未来批量 DAO 的 `BatchResult` 更新计数解释；真实批量事务、性能和 10 万行持久化须另验。

## ALL 既有边界：截在第一次 Intent 写入

`NotificationApplicationRuntimeService.java:82,493-504` 对 ALL 每次向 `UserService.selectAllActiveUsers(offset,1000)` 取一页，加入 `recipients` 后以 `size()>100000` 拒绝，空页终止；`submit:111-112` 是解析结束后的第一笔 Intent 写入。构造单渠道、ALL、空 `recipientIds`、无附件、ALL+ASYNC+priority0、无截止的有效命令；让 `dispatchService.validateSubmission` 正常返回，幂等键留空以避开旧收据分支。为页数据生成连续且唯一的正 `userId`（其他字段固定有效），不要预先分配额外 10 万条模拟持久化对象。`UserService` 替身仅根据 `offset,limit` 生成页，并记录调用顺序和限制值：

1. **100000 放行到写入边界**：offset 0..99000 共100个非空、各1000人，offset100000返回空。对 `dao.insert(NotifyIntent)` 设置记录型 Answer：记录 Intent 的 ID/状态/提交命令关联后立即抛测试专用 `ReachedFirstIntentWrite` 哨兵。调用完整 `runtime.submit(command)` 并只接受该哨兵；断言101次分页调用、每次 `limit=1000`、最后 offset100000；第一次 Intent insert 恰一次，Recipient/Delivery/Outbox/Attachment insert 均零次。该结果证明**现有严格大于边界允许100000继续至第一次业务写入**，不证明其成功插入、更不证明10万真实持久化或事务性能。若 `dao.insert(intent)` 签名需要返回 `int`，Answer 在返回前抛出即可。
2. **100001 前置拒绝**：前100页同上，offset100000只返回第100001人。完整调用 `runtime.submit(command)` 应抛准确的 `ServiceException` 超限分类；分页调用101次、最后 offset100000，不再查空页。所有业务 insert/update 录制计数必须为0，也无 `requestOutboxWake` 或 Provider 调用。由于这是 recording DAO 单元接缝，表述为“未调用业务写入口”，不能称真实数据库已证零行。之后的真实 owned MySQL 集成可另用少量样本及非首块故障核物理回滚。

两个测试可复用同一个按 offset 生成页的替身，但要重置调用记录，防止第一个测试缓存影响第二个。不要用运行耗时当 SQL 往返或性能基线；该用例的 100000 用户对象构造、去重和 GC 开销只服务边界正确性。显式 USER/PHONE/EMAIL 没有同一人数上限，不能从该测试推断全入口统一限制。若 `submit` 前置验证新增数据源读取，允许只读调用，但写入口仍须零调用于拒绝路径。

## 未来批量 DAO 的结果计数矩阵

若实施选择 MyBatis-Plus `BaseMapper.insert(Collection,batchSize)`，DAO 应接收 `List<BatchResult>`，按**实际输入实体数**校验展平后各 `updateCounts`；不能只用 `Db.saveBatch` boolean，因为 `SqlHelper.retBool(List)` 对空更新数组的 `allMatch` 也可能为 true。用窄 DAO 单元测试构造/伪造 `BatchResult`，同时保留真实 MySQL 证明事务与物理行数：

| 返回更新计数 | DAO 应有的判定 | 必需断言 |
|---|---|---|
| `1` 每实体 | 接受 | 跨多个 BatchResult/批块展平后总项目数恰等于输入数；ID/FK仍由调用方预生成。 |
| `-2` = `Statement.SUCCESS_NO_INFO` | 可接受为“执行成功但受影响行数未知”，**不得**称精确1行 | 项目总数仍须等于输入数；真实 MySQL 同事务/提交后按 intent 核 Recipient/Delivery/Outbox 实际行数与唯一关系。 |
| `0` | 拒绝并抛异常 | 即使其余项目成功，也由外层 `@DSTransactional` 回滚；不返回成功、不发布 wake。 |
| `-3` = `Statement.EXECUTE_FAILED` | 拒绝并抛异常 | 不把它等同 `-2`；若驱动改为抛 `BatchUpdateException`，异常直接传播并验证物理回滚。 |
| 缺项/空列表/null/count 数组长度不足 | 拒绝并抛异常 | 输入非空时不能因 vacuous `allMatch` 或部分结果而成功。多一个计数项也拒绝，以免结果与实体无法对应。 |

对单实体 INSERT，正计数应为 `1`；异常的 `>1` 不应被宽泛 `>0` 视为准确成功。空输入若是合法 no-op，须在 DAO 入口显式返回且不调用 Mapper；它不是非空批次成功的证明。上述是未来代码的校验建议，不能替代真实 `NotificationApplicationUseCase.submit` 的中途失败全回滚、第二连接可见性、同一动态 XID/连接及 `SUCCESS_NO_INFO` 时的实际行数测试。
