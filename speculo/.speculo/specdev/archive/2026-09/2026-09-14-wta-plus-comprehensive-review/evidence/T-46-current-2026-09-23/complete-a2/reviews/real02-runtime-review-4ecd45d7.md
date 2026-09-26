# T46 real02 隔离真实验收只读复核

固定产品源 `4ecd45d7207429e78d767b281851eafb1ba0f955`、tree `38f4411a53af1096388dbe4afae610f72eda8365`；测试文件 SHA256 `46c71b5b438cf17680d4456e9209b0ae06e56e77a351a4942e2fa8316f805ba8`。输入为 `/tmp/wta-t46/oss-migration-runs/e9768dda90c986f5/result.json`、同 run 的**脱敏** XML/`maven.log`、固定 Git 源；未读原始凭据/props，未启动额外服务或构建、未改仓库。本次判定仅针对 T46 精确两方法真实 runner，不替代正在进行的 default/full/core/static 门禁。

## 结果与可归因范围

- runner `acceptance=true`、exit0、Maven exit0；脱敏 log 显示该类 2 tests/0 failures/0 errors/0 skipped 与 BUILD SUCCESS。fresh Surefire XML suite 名、私有 run marker、**两个确切方法**均匹配，XML 无 failure/error/skipped 节点。脱敏 XML SHA 与 result 所记一致，其 mtime 晚于本轮 start。
- source before/after 均为同一 clean HEAD/tree。MySQL/MinIO 是本轮 owner 的随机回环端口/空专库，runner 记录的两个完整容器 ID 已回收；两匿名卷逐名不存在，两个回环端口关闭，Maven 进程组与 escaped owned 子进程均空，`cleanup.errors=[]`。这些是 runner 自身记录并由脱敏结果交叉核对；没有把旧 real01 的资源或测试数混入。
- 固定测试源的**首方法通过**意味着依次越过全部断言：真实迁移/延时窗口；UNKNOWN CAS 成功后 XID 单次 arm、目标连接的物理 commit 单次命中且异常链含受控 `SQLRecoverableException`；DB 已持久 UNKNOWN 时 provider DELETE=0；配置物理身份修改被拒；来源经独立删除后的核对式完成；回滚；Item CAS=0 与对象指针 CAS=0 后指针/工单版本保持。XML 本身仅给方法通过，细粒度事实来自固定源码中未被条件跳过的断言顺序，非独立逐项日志。
- **第二并发方法通过**意味着它执行了两个连接 ID 不同、对象/配置行 NOWAIT 3572 锁证据；旧恢复预读后 cleanup 领取 UNKNOWN，旧恢复锁内拒绝；未创建 owned 桶的 HEAD 404 不能清 UNKNOWN/重发 DELETE；在 101 迟到物理 DELETE 放行前，102 的新工单已切换；放行后真实 MinIO 来源对象缺失、目标仍在、物理 DELETE 计数 1、后续 cleanup 只复核。此测试模拟调用者超时而提供者稍后执行，并不宣称网络层取消了请求。

## 与 real01 的关系及余项

real01 `60ecc38cb9c20055` 在 c30391d0 是 2 tests/1 failure：首方法第191行 UNKNOWN 为 null；第二方法通过。该失败保留为历史，不能追认。`4ecd45d7` 仅测试 fixture 将 ACK 注入绑定到成功写 UNKNOWN 的 XID；本轮 real02 对此修正给出真实正证，不意味着此前推断的**具体提前 commit 位置**已被 real01 日志直接证明。最强机制解释仍为 MyBatis pool 复用 autoCommit=false 连接、cleanup 非事务预读可能先物理 commit；新注入避免了对该具体位置的依赖。

此处 T46 真实两方法门禁可判通过；整票收口仍取决于 Lead 的当前候选 default/full、消费者和静态门禁及治理归集结果。
