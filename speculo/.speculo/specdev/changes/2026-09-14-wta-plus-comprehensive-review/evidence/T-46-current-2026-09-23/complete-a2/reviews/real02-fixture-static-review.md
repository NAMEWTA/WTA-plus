# T46 real02 ACK fixture 固定增量静态审查

固定候选 `4ecd45d7207429e78d767b281851eafb1ba0f955`，对比 real01 受测 `c30391d0a559bed30a81b04058fbe4c13d563544`。只审产品源精确增量及既有 v2 runner，未运行构建/服务、未读取 raw secrets/props。Lead 的 real02 尚在运行，本稿非 runtime PASS。

`OssStorageMigrationIntegrationTest.java` SHA256 `46c71b5b438cf17680d4456e9209b0ae06e56e77a351a4942e2fa8316f805ba8`。修复仅属测试 fixture：`updateItem:153-162` 先由真实 store 成功 CAS 更新，确认 `errorMessage=CLEANUP_OUTCOME_UNKNOWN` 且当前 DS XID 非空，再一次性保存该 XID 与 arm 次数；连接包装器 `:119-141` 在取得连接时固定 `connectionXid`，只有这个 XID 的物理 `commit()` 才先调用真实 `connection.commit()`，增加 hit，然后抛受控 `SQLRecoverableException`。清理前全局置位已移除，故 `cleanup` 的非事务 batch/item 预读不能消耗注入。`catchThrowable` 后对异常链精确查受控类与消息，再断言 arm/hit 各 1、持久 UNKNOWN，未放宽原业务断言。此绑定与本机 DS 4.5.0 `ConnectionFactory.notify → ConnectionProxy.notify` 的物理提交路径一致；真正完成写库后丢 ACK 还须 real02 证实。

第二并发方法及其晚到 DELETE/另对象进度/桶未知负例均未变。类仍精确两项 `@Test`；原 `/tmp/wta-t46/run-oss-migration-integration-v2.py` SHA256 `427a32985ebff608bc3359893a2d6e6a98f64f841c9a4a877d91f247289b11cf` 的方法与 clean source 门禁适用新 `--expected-head`，无需改 runner。旧 real01 的 2/1/0/0 是失败事实，不能被新静态审查追认为通过。当前未见这 38 行 fixture 增量中的确定静态阻断。
