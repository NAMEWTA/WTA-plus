# T-38 Source A 安全/事务审查（固定提交，运行待验）

固定 base `b47ff8b91cfef02a9f28de0e201edcd1575a950f`，Source A `7a6f75ac9238399daf7936797d07da141f0f5a03`，tree `3c519c0cad48d11ec1331a92884332a0ca230f4b`，直接父 `9f8fa2b1e36c851be8cee6763a84ebf6e2072928`。以 `git show A:path` 和 `git diff base...A` 固定审查；未用开发工作树代替源码、未自行构建/运行服务或测试、未写仓库。Lead 的 13 个真实 DB 测试与 Wake owned driver 后续已完成并保存证据（下文）；OpenAPI 生成最终 B 尚未固定。此报告是 Source A 审查，不宣称最终票通过。

## 安全与事务结论

**Source A 静态安全轴：通过，未见阻断；对应真实选集已通过，最终 B 仍待核。** 前次预审列出的三个高风险验证缺口（批量外部 UNKNOWN、IN_APP 缺关系、确定性并发）在 A 中均已加真实 SQL/事务测试；下文逐项列明它们的断言与 Lead 实际运行结果。

- **HTTP/权限/身份。** `NotificationController.java:42–70` 保留独立 `@SaCheckPermission("notify:notification:retry|cancel")` 与 POST `@Log`；body ID 与 URL 冲突时服务调用前拒绝，一致/空 body 统一以 URL ID 重建命令。`NotificationRetryControllerContractTest.java:40–75` 测冲突及空 body；新增 77–128 行把真实 `SaInterceptor` 装入 MVC，模拟无权限、仅 retry、仅 cancel 三组，逐接口检验 403/200，并恢复 Sa-Token 全局状态。它验证方法权限，不单独验证 `SecurityConfig` 的 ClientID/token/access_path；后者未随 A 改动，需依既有全局合同证据。`intent.appId` 仍是业务应用命名空间，不是登录 Client owner；T-38 的对象归属按 Delivery→Intent 比对。
- **锁序与归属。** `NotificationApplicationRuntimeService.java:177–205` 在动态事务 `NotificationApplicationUseCase.java:34–38` 中先锁 Intent；指定 delivery 的普通读只作预检，随后通过 DAO 47–70 行按 Outbox ID 排序锁 Outbox、再锁 Delivery，最终重验 `delivery.intentId`。foreign ID 抛业务错误；同 Intent 的整批 delivery 集合由锁定当前读取得。`RuntimeService.java:189–233` 对已取消/过期/完成 Intent 返回其真实状态和零任务，对范围内外部 UNKNOWN 在任何写操作前抛错；指定安全 sibling 可单独重试。没有按泛 FAILED 或 `appId=ClientID` 猜归属。
- **安全 allowlist。** `RuntimeService.java:40–44,255–261` 只认当前 `NotifySendPlanner` 在 `NotifyClient.send` 前产生的精确错误码：`UNBOUND_CHANNEL`、账号/变量/额度和 SMS 模板参数相关码；IN_APP 只认本地快照/端口预检的 `LOCAL_DISPATCH_ERROR`。旧 provider `FAILED`、`PROVIDER_UNSENT_TERMINAL`、外部 UNKNOWN、`WAITING_RECEIPT` 不因状态或文案获得重发权。需唯一 Outbox、无 providerMessageId、Delivery/Outbox 错误码一致、无租约、`attempt_count < max_attempts`（RuntimeService 216–232）。这是较窄的已证未外呼恢复路径；不重置预算。
- **CAS 与活任务。** `RuntimeService.java:237–252` 先有界收集资格，再同事务逐个 CAS Outbox 与 Delivery，任一零行即抛错回滚；仅实际成功时改 Intent=QUEUED、发提交后 wake、回传实际 `queuedCount`。`NotifyOutboxMapper.xml:56–65` 按 outbox/intent/delivery ID、原状态/错误码、无 lease、剩余预算更新，只允许 DONE/WAITING_RECEIPT，保持尝试次数；`NotifyNotificationDao.java:152–161` 的 Delivery CAS 同时限定 intent/status/error/provider ID。重复请求见 PENDING/READY 后不再合格，不插入新 Outbox。`RetryReceipt.java:5–10` 把零任务语义公开。
- **IN_APP UNKNOWN。** `RuntimeService.java:229–232,264–269` 仅固定 `DISPATCH_ERROR`+`WAITING_RECEIPT` 且同 Intent 本人关系可安全复用时放行；存在 orphan recipient 而无消息拒绝。复用 T-36 `InAppNotificationService.persist` 的 intent 消息主键、本人关系去重和新增关系 AFTER_COMMIT push；`NotifyDispatchResultService.completeInApp` 保持消息/结果同短事务，预算在原租约预留。没有把外部 UNKNOWN 映射进此本地恢复路径。

## 13 个真实测试的源码覆盖与运行结果

`NotifyManualRetryIntegrationTest.java:53–54` 要求 `-Dnotify.manual.retry.integration=true`；69–89 行要求 owned loopback MySQL/Redis，并复用 `NotifyAtomicResultIntegrationTest` 的六 SQL/MyBatis/动态事务/Redis 装配；91–111 行仅清理自身测试 ID、绑定、账号和触发器。13 个 `@Test` 分别覆盖：

1. 114–140：指定 A/B 中 A，重复请求零任务、活租约不被抢。
2. 142–159：跨 Intent delivery 和指定外部 UNKNOWN 拒绝。
3. 161–177：**新增**无 deliveryId 的安全 FAILED + 外部 UNKNOWN 同批，先拒绝且两任务/聚合不变、零 wake；然后只选安全 sibling 能排队。
4. 179–200：预算耗尽和同 Delivery 两 Outbox 不生成第三任务、不动活租约。
5. 202–219：DELIVERED/CANCELLED/不支持的旧 FAILED 均零任务。
6. 221–234：数据库 BEFORE UPDATE trigger 令 Delivery CAS 失败，验证前序 Outbox CAS 回滚、Intent 原状、零 wake，移除触发器后可成功；这是真实 SQL 回滚，不是 mock。
7. 236–274：**增强**两线程重试；首线程取得 Intent 行锁后测试闩暂停，第二线程进入 `lockIntent`，释放后只一个 queuedCount/一份 Outbox/一唤醒。比预审的同起点测试有确定持锁交错。
8. 276–309：重试事务中 Outbox 已改但未提交时的 claim 返回空；提交后仅一份可 claim，活 lease 不被后续重试改写。
9. 311–329：已存在 IN_APP 消息/本人关系时重放，不新增关系、零重复 push。
10. 331–350：**新增**已有消息但缺本人关系，补唯一关系与一次 push，重复重试/旧 lease dispatch 不追加 push。
11. 352–364：**新增**orphan 本人关系而无消息，零排队、零写、零 push。
12. 366–381：**新增**消息与关系都缺失，原子补一份并只 push 一次，重复旧 lease 不追加。
13. 383–422：真实 Planner 首次 UNBOUND 返回 DONE/FAILED 且 provider 调用零次；修复隔离绑定后同原 Outbox 重试并受理一次，计两条 Attempt。

`NotificationRetryControllerContractTest.java:77–128` 的方法级权限测试不属于上述 13 个真实 DB 方法，需另外计数。Lead 的不可变 `/tmp/wta-t38/runs/83150f82cac60fb5/result.json` 记录 driver exit 0：`NotifyManualRetryIntegrationTest` **13 tests / 0 failures / 0 errors / 0 skipped**，`NotifyWakeIntegrationTest` **1 / 0 / 0 / 0**；六份 SQL 导入均 exit 0。source before/after 均为 A `7a6f75ac` / tree `3c519c0c` / clean=true；Maven 进程组 186545 无成员，owned 容器与精确匿名卷已清，loopback 32816/32817 已关闭，cleanup errors 空。以上为 Lead 执行、我只读核对的实际证据；方法级权限测试需引用其另一个精确运行计数，不能混入这 14 项。尚未见发事件后回滚的专属新断言，但 A 的回滚测试在事件发布前失败，既有 AFTER_COMMIT 合同可由 Wake 既有测试及代码审查支撑，不因缺一条重复测试阻断此安全轴。

## 残余边界与 B 增量

`NotificationPage.vue` 仍对所有 FAILED 显示重试入口，服务端只对安全 allowlist 排队，不安全者返回零任务；这是可解释的宽提示、不是越权或安全绕过。T-39 负责 `expiresAt` 生效及取消/过期 Worker 收敛；A 当前 retry 未读截止时间，按 Ticket DAG 留给 T-39，不能误标 T-38 未完成。固定 Source B 若只新增 OpenAPI 生成物且 A 生产/测试文件哈希相同，本安全轴可复用并增量检查生成合同 ID/queuedCount 与类型消费者；若 B 改生产/测试，重新审受影响差异。最终 B checkpoint 与默认后端全量门禁仍待 Lead 完成。
