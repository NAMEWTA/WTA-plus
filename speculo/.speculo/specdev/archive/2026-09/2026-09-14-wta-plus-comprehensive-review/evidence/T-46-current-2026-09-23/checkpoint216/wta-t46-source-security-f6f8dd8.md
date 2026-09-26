# T-46 固定源码安全/事务审查：f6f8dd8（补修前检查点）

固定产品候选 `4f8c4b4aba1c91e273fa47ef4cad345d32a04062`；Region 单文件增量候选 `f6f8dd8ab0bb102395d7deeeb4c1cd3d4de73733`。基座为 `3ba164f7`；只用固定提交与已读原始 diff，工作树当前 clean。对 19 路径中的生产状态/事务、配置引用与有界 I/O 做审查；未运行构建/服务。首次 dirty 读取时关键文件 SHA256：Atomic `f16eef4817b6197c1ca2cebf678b5cf70d44bc7765e3e90dd61789b6fad6572e`，Service `4685af66990bddbf6b1674dc51dcaf45cb5030c5fd2119bf07c0bcdde9bdeff8`，Store `ec6b32b42d5a79760e46727012533178329078ceaece09f0e4c75bd637302a19`，Common client `89cef43531cd7e5d70ea061bd2e790179cc533a7d94e1f036a6212d9b4e86f23`，均与 4f8c4b4 固定源码一致。f6 的 ConfigService 文件 SHA256 `d8532778a2d7802c2976ccee734ff96d98abdd582b2d04976ba121c63ba3f2b9`。

## 阻断：对象 HEAD 的模糊 404 被当作源对象已删除

`AbstractOssClientImpl:2151-2174` 的既有 `toStorageException` 把所有 `S3Exception.statusCode()==404` 归成 `OBJECT_NOT_FOUND`，而新 `headObject(key,Duration):2012-2026` 直接使用此转换；`DefaultOssMigrationObjectStore.exists(...,Duration):69-78` 见此 code 就返回 `false`。`OssStorageMigrationService.reconcile:217-233` 因此可在源 bucket 不存在/不可辨的 404、目标 HEAD 成功时调用 `atomic.finalizeCleanup`，把来源删除结果的 UNKNOWN 栅栏永久记为 COMPLETED。HEAD 的 404 不能单独证明是 NoSuchKey；AWS [HeadObject API](https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html) 明确错误只有 generic HTTP code，[S3 errors](https://docs.aws.amazon.com/AmazonS3/latest/developerguide/ErrorBestPractices.html) 明确 NoSuchBucket/NoSuchKey 均可为 404。此路径是新恢复/清理代码直接可达，当前测试只覆盖真缺 key，未覆盖缺 bucket。

建议的窄修已交 Lead：只增强新增有界 HEAD 方法，在对象 HEAD 404 后以**同一总预算的剩余时间**对同 bucket 做 HEAD；只有 Bucket HEAD 200 才保留 `OBJECT_NOT_FOUND`，其余状态、超时、中断均保留 UNKNOWN（映射非 OBJECT_NOT_FOUND），旧普通 HEAD 行为不变。AWS [HeadBucket API](https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html) 要求 ListBucket；权限不足时会保守地不自动完成，不能把 403 改作对象不存在。需在 owned MinIO 加真缺 key + bucket 可达正例、缺 bucket 负例、两次 HEAD 共用总预算和零 DELETE 重发断言。修后新固定 SHA 须增量复核，此报告不追认修复。

## 已核不变量与限制

- `OssMigrationAtomicService.createItem/claim/running/restore/reserveCleanup/finalizeCleanup` 把事务操作集中在代理 public `@DSTransactional` 方法，锁序为配置（仅建单）→ Object→Item；Item version/status CAS 影响行数为 1，否则抛错回滚。`switchToTarget` 对象 CAS 与 Item 状态同事务，`restore` 在同锁下拒绝 UNKNOWN/已完成与非最新工单。
- `reserveCleanup:184-211` 在外部 DELETE 前提交 `FAILED + COMPLETED + CLEANUP_OUTCOME_UNKNOWN`；`cleanup:174-215` 只有该调用已返回后才有界 DELETE。DELETE/提交 ACK 不明后只读核对，源仍存在时不重发 DELETE，`finalizeCleanup:213-237` 再锁 Object→Item 并核同版本/来源/目标/key/ACTIVE/target 指针。普通 rollback/unpublish 在锁内拒绝未知栅栏。此安全算法的真实提交 ACK 丢失/双连接/MinIO 结论仍待 Lead 的 fresh 证据。
- `SysOssConfigMapper:18-29` 与 `SysOssConfigServiceImpl` 的配置写事务先锁全部现存配置，再查询对象与所有非终态工单的 source/target 引用。`MybatisOssMigrationStore.lockMigrationConfigs:36-53` 在建单中比较预检的 bucket/endpoint/isHttps/region/accessPolicy 物理身份；FAILED UNKNOWN 仍持引用。Region null/blank 的实际客户端默认同为 `us-east-1`，f6 只对被引用配置的字段比较作这个等价归一，没放宽 endpoint/bucket/policy 等身份。
- `AbstractOssClientImpl` 新 DELETE/HEAD 同时设置 SDK `apiCallTimeout`、`apiCallAttemptTimeout` 并在 `await` timeout/interrupt 时 cancel future；这只限定本地等待，不能当远端 DELETE 已取消。`OssMigrationAtomicService` 的持久 UNKNOWN 栅栏正是必须条件。copy/verify 仍事务外且未在本票扩为有界，符合已确认边界。
- 既有 4f8c4b4 首轮定向 174 例中 1 error 是 Region null/blank 凭据轮换被错误拒绝，Lead 已保存原证据并以 f6 单文件修正；f6 绿灯在本报告时尚未返回。完整默认/全包/真实 MySQL+MinIO/最终 source clean 尚未核验，不能将静态核查当 AC-046 通过。

静态结论：**request changes**，仅上述模糊 404 结算为阻断；Region 修正源码方向通过，运行证据 pending。没有建议新状态机或无关 OSS 行为改动。
