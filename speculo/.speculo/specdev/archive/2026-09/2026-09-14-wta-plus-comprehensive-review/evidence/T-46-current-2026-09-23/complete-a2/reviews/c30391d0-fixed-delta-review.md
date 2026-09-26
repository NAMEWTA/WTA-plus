# T46 固定候选 c30391d0 增量审查与 runner 冻结增补

固定 HEAD `c30391d0a559bed30a81b04058fbe4c13d563544`，工作树 clean；基线 `4f8c4b4aba1c91e273fa47ef4cad345d32a04062`。只读 `git diff base...head`，未运行 Maven、MinIO、MySQL 或产品服务。T46 全量静态意见仍见 `/tmp/wta-t46/latest-integration-static-review.md`；本稿只裁增量，不宣称真实通过。

## 产品与测试增量

- `AbstractOssClientImpl.headObject(key,timeout):2013-2053` 在对象 HEAD 404 后仅以同一 `System.nanoTime()` 截止的剩余预算发 `HeadBucket`，成功后才把原 404 映射为 `OBJECT_NOT_FOUND`。桶 HEAD 的 404/403/超时/不足 1ms 都归 `PROVIDER_ERROR`；没有把任何 Bucket 不明当成对象缺失。`await:1432-1445` 对每步有限 `get`，超时时取消本地 future；此取消**不是**远端请求撤销证明，cleanup 仍依赖既有持久 `CLEANUP_OUTCOME_UNKNOWN` 栅栏。接口文档 `OssClient:758-766` 同步声明缺 Bucket HEAD 权限时保守未知。`OssClientProviderUnitTest:220-249` 有对象404+桶存在、桶404、桶403、桶超时四个定向反例；是否真执行由 Lead 绿门禁确认。
- `OssStorageMigrationIntegrationTest` SHA256 `7ae2a14502828d2eb9d522528696cd38d98076d8cba10e2dc128c316006df13f`。第二方法 `:258-269` 创建**未创建的本次 owned 桶**客户端；`:427-435` 在 cleanup 持久 UNKNOWN 后，以该客户端发起同一工单重试，要求抛 `OssMigrationException`、UNKNOWN 不消失、物理 DELETE 仍只进入 1 次。之后仍要放行晚到 DELETE、核真实 MinIO 来源缺失/目标存在。这个反例实测时能证明桶404不会误完成工单；当前静态只能确认断言存在。
- `SysOssConfigServiceImpl:370-372` 将空白 Region 与 `us-east-1` 作为相同有效物理地域比较，与 `DefaultOssClientImpl` 的缺省 Region 一致；非等效 Region 仍不同。`requireMigrationTimeout:2071-2076` 限 1ms–30s，与有界删除/HEAD 合同相符。未发现本增量中明确的静态阻断；完整合同仍须真实运行及全量审查。

## 既有 v2 runner 兼容性

候选仍精确两个 `@Test`：`migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack`、`restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource`；新断言嵌在第二方法。`/tmp/wta-t46/run-oss-migration-integration-v2.py` SHA256 `427a32985ebff608bc3359893a2d6e6a98f64f841c9a4a877d91f247289b11cf` 保持原字节，不需 v3。其固定 helper `frontend/e2e/run-notice-retraction-real.py` 实际 SHA256 `7afb43db7d0086aeda0778389ad2fba95252dddf221eec406024101662c3bf46` 与预检 pin 相符；新候选 clean HEAD 将由现有 `--expected-head` 强绑。私有纯离线 `python3 -B /tmp/wta-t46/test_run_oss_migration_offline_v2.py` 于此 HEAD exit 0，7/7；没有启动服务或构建。

Lead 独占的真实命令：

```text
python3 -B /tmp/wta-t46/run-oss-migration-integration-v2.py --execute --expected-head c30391d0a559bed30a81b04058fbe4c13d563544 --expected-methods migratesWithProductionStoreAndDualBucketsThenCleansUpOrRollsBack,restoreAndCleanupSerializeOnRealObjectRowAndPreserveCurrentSource
```

结果应在 `/tmp/wta-t46/oss-migration-runs/<run-id>/result.json`，要求 Maven 0、同次新鲜 2 方法 0 failure/error/skip、source before/after 同 clean HEAD、MySQL/MinIO full-ID/卷/端口/进程全部回收。早先 4f8 候选与其证据保持历史，不追认成此候选的真实运行。
