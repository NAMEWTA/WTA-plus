# T-46 最终合同审查：PASS

固定候选 `4ecd45d7207429e78d767b281851eafb1ba0f955`，tree `38f4411a53af1096388dbe4afae610f72eda8365`，基线 `3ba164f7`。总 diff 为 19 个产品/工程路径（18 个 backend 路径及 system module fact）；`c30391d0..4ecd45d7` 的 backend 仅改 `OssStorageMigrationIntegrationTest.java`，生产 `src/main/java` 逐字等价（`git diff --quiet` 退出 0）。本报告复用前两次固定审查 `/tmp/wta-t46-source-security-f6f8dd8.md` 和 `/tmp/wta-t46-source-security-c30391d0.md` 的源码依据，补核最终 ACK fixture 与真实证据；不把旧失败改写为通过。

**结论：固定 `4ecd45d7` 的 AC-046 无阻断，安全/事务审查通过。** 下面区分最终候选的真实执行、输入等价复用，以及隔离验收不能证明的部署行为。早期 4f8c/f6 的 request changes 与失败证据仍保留原时点；后续补修和重验不追认旧候选。

## 合同结论

- `OssMigrationAtomicService.createItem/claim/running/restore/reserveCleanup/finalizeCleanup` 分别在代理 public `@DSTransactional` 短事务内对状态做 Object→Item 加锁和 version/status CAS；建单另先按配置主键序锁配置。指针切换与工单保存同事务，任何一处 0 行都会抛错回滚。恢复获锁先提交时，随后 cleanup 不能取得 CLEANUP_ELIGIBLE；清理先提交 `FAILED + COMPLETED + CLEANUP_OUTCOME_UNKNOWN` 时，unpublish/rollback/新工单/自动 retry 均不能绕过。该 UNKNOWN 代表已获远端 DELETE 执行权，不代表 DELETE 成功。
- `OssStorageMigrationService.cleanup` 仅在 reservation 事务已返回后于锁外对单一来源对象发有界 DELETE；晚到完成、超时、提交 ACK 丢失不触发盲重发。显式再次 cleanup 只用有界 HEAD 证明源对象缺失且目标可达，再在同一对象/工单锁序与原版本下 finalize；源仍存在或任一查询未知维持栅栏。`c30391d0` 修复了前一候选的模糊 404：新增有界 `headObject` 只在源对象 HEAD 404 后用同一剩余预算 HEAD Bucket，Bucket 200 才返回 `OBJECT_NOT_FOUND`，缺桶/403/超时均为 UNKNOWN。普通无 Duration 的 HEAD 及其他 OSS 公共调用保持原义。缺少 `HeadBucket` 权限会保守阻止自动收敛；这是安全限制，已记 backend README/module fact。
- `SysOssConfigMapper.countOssReferences` 包含当前对象和未终结工单的 source/target（FAILED UNKNOWN 仍计入）；配置编辑/批删在引用读之前按稳定顺序持配置锁，创建工单先锁配置再 Object→Item，并核预检物理身份。Region 原值 null/blank 统一按客户端实际 `us-east-1` 解释，允许同存储身份凭据轮换；endpoint/bucket/accessPolicy/isHttps 等物理路由仍失败关闭。f6 单文件修正已由后续定向绿灯覆盖。
- 新公共 Java API 仅 `OssClient.headObject(key,Duration)` 与 `delete(key,Duration)`；只有迁移恢复/清理调用新重载。SDK 请求配置 api-call/attempt timeout，`await` 超时/中断会 cancel Future，但代码和文档均未把 cancel 视为远端删除撤销。剩余预算计算对顺序网络等待有界；构造异步请求的微小本地开销不构成绝对墙钟硬上限，显式 interrupted case 未单独测试。既有复制/校验仍为事务外旧路径，本票未扩全 OSS I/O 平台。

## 最终 ACK 夹具与真实证据

- 最终提交只改变 real integration fixture：`OssStorageMigrationIntegrationTest:119-160,201-225,682-690` 在 UNKNOWN 状态 SQL CAS 成功后记录当前动态事务 XID，仅对该 XID 的首次真实 JDBC `commit()` 先执行数据库提交，再抛专用 `SQLRecoverableException` 模拟确认丢失。断言武装 1 次、命中 1 次、错误原因精确、数据库 UNKNOWN 已持久化且 DELETE 计数 0；后续手动删除/确认目标/再 cleanup 则不重发 DELETE。这比旧“任意下一次 commit”夹具确实绑定了 reservation 事务，未降低原断言。其余真实双连接 `FOR UPDATE NOWAIT` 阻塞、旧 owner、跨对象推进、真实 MinIO 私/公对象 HEAD 与匿名 GET、缺 bucket 保留 UNKNOWN 等断言仍在。
- `/tmp/wta-t46/oss-migration-runs/e9768dda90c986f5/result.json`：最终 `4ecd45d7` source before/after 同 clean/tree，Maven 0，fresh `OssStorageMigrationIntegrationTest` XML 独立解析为 **2 tests / 0 failures / 0 errors / 0 skipped**，两预期方法均执行；`acceptance=true`，进程组无成员、owned 容器/卷/loopback 端口全清、cleanup errors 空。测试使用 owned MySQL 两物理连接和 MinIO 对象，但 Spring/dynamic datasource/MyBatis 是该类手工装配的真实组件与代理，**不是整套应用上下文 HTTP 运行**；不能据此声称真实管理 API/前端 E2E 已跑。本票合同要求的是该类隔离集成，未要求无关浏览器 E2E。
- `/tmp/wta-t46/green03-source-and-counts.json`：`c30391d0` clean before/after，34 fresh OSS 单元类 **175/0/0/0**；在 `4ecd45d7` 只改上述真实夹具、生产输入逐字等价的条件下可按输入等价复用。第一次 4f8c 的 Region 失败、其后的已保留失败证据不被此绿灯倒改。green03 不能替代最终 default/full/core/static。

## 最终门禁与残余边界

- 最终候选 `/tmp/wta-t46/a2-backend-default.json` 记录 `./mvnw test` exit 0；`a2-backend-default-counts.json` 的 262 份 fresh 类报告为 **1145 tests、0 failures、0 errors、217 环境 skip，即 928 执行通过**。`a2-source-before.json` 与 `a2-source-after.json` 均为同一 `4ecd45d7` / `38f4411` 且 clean。默认 reactor 的环境 skip 不替代上面独立 2/0/0/0 的 required MySQL+MinIO 真实验收。
- `a2-full-package.json` 与 `a2-full-bundle.json` 均 exit 0；`a2-full-package-proof.json` 绑定 clean `4ecd45d7`、完整 JAR `e27b3af7b290125da3b7d14b7817c5d738a44ed94550df658a4e020ec17be70a`。`a2core-core-package.json`/`a2core-core-bundle.json` 均 exit 0，`a2core-core-artifact.json` 绑定 clean 同源 core JAR `23975c269806f0f690b834a6e07e75ece15110e0363da47f00881cd79e9cec38`；core 前后 source JSON 亦同 SHA 且 clean。两种不同 bundle 模式产物 hash 不应混称同一 JAR。
- `a2static-{skill-facts,fullstack-facts,notify-layered,fm,handbooks}.json` 五项各 exit 0；`a2static-source.json` 指向最终 HEAD 且前后 clean。`a2-write-scope-and-ancestry.json` 确认 direct-parent、19 个产品路径落在 14 个登记根、`outside_scope=[]`、frontend 未改，且 green03→最终候选仅 ACK 测试夹具变化、生产代码完全不变。green03 的 34 类 175/0/0/0 因此只作为输入等价复用，不冒称在最终 SHA 重跑；最终 SHA 的默认全后端和 real02 是实跑。
- 隔离 owned MinIO + MySQL、手工装配真实 Spring/dynamic datasource/MyBatis 组件与代理，不等于完整应用 HTTP、生产连接、部署 cutover、真实生产桶权限或前端浏览器验证。本票未改前端，也未把无关前端 E2E 作为 AC-046 门禁。缺 `HeadBucket` 权限的身份会让 DELETE 已成功后的自动 finalize 保守停在 UNKNOWN，需继续人工核对，不能通过忽略 403/404 伪造完成。

本审查未执行构建、测试或服务，未修改仓库。
