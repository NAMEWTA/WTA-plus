# T42 A1 产品合同独立静态审查

固定输入：parent `b2c56b67`，candidate `0595e2ccbd03474bced92809b2ac9056c9a15751`。全部源码结论按 `git show 0595e2cc:<path>` / `git diff b2c56b67 0595e2cc` 固定读取；未读取变化中的产品树作为证据。票据为 `ticket/42-mail-attachment-contract.md` revision226，派单为 `evidence/dispatch-T-42-implementation.md`。范围是该提交 65 路径（含测试/文档/六SQL），重点 Notify、System OSS、common Mail/OSS 的授权与事务。**结论：request changes；当前不能把 A1 称为 AC-042 完成。** Lead 的 Maven/真实服务验收正在进行，本审查没有运行构建、服务或测试。

## 阻断产品问题

**P1：取消后的旧 SMTP 在途任务可被误判“明确未发送”，继而释放共享源与快照引用。** `NotificationApplicationRuntimeService.cancel` 可把仍为 `PENDING` 的 Mail Delivery 直接置为 `CANCELLED`（`.../NotificationApplicationRuntimeService.java:352-366`），没有阻止已通过发信前 DB gate、正在 `MailNotificationSender.send` 的旧 Worker。租约为 60 秒（`.../NotifyOutboxClaimService.java:27-42`）；到期 `NotifyOutboxMapper.xml:4-24` 允许另一 Worker 重领原 `PROCESSING`。重领者在 `DispatchNotificationService.java:75-80` 看到 Delivery 已非 PENDING 后调用 CLOSE，`NotifyDispatchResultService.java:74-76` 把 Outbox 结为 `DONE`、清租约。此时旧 sender 仍可能在途，但新的 `NotifyAttachmentSnapshotTransactions.releaseIfSafe` 仅凭 `CANCELLED`、无当前租约、无 `providerMessageId/acceptedAt` 就释放真实引用（`:85-113`）。旧结果因 owner/token 失效可能无法提交，数据库中的空回执不是未发证明。必须给回收增加**持久的、能证明本任务未进入 Provider 的事实**；对于曾可能进入 Provider 的重领/取消路径保守保留。用阻塞物理 sender、两 Worker/两连接、租约过期+取消+重领+回收的真实数据库反例验证引用不被解除，不能仅测短时未领取的取消正例。

**P2：无邮箱的 USER+MAIL 终态永久占用通知源引用。** 提交先插入附件关系并绑定来源 `sys_oss_ref`（`.../NotificationApplicationRuntimeService.java:122-153`）；用户邮箱缺失时 Mail Delivery 被设为 `UNDELIVERABLE`，不创建 Outbox（同文件 `:180-193`）。这是可证明未发的终态，但 `releaseIfSafe` 只接受 Mail `CANCELLED` 或错误码为 `NOTIFICATION_EXPIRED`/`NOTICE_RETRACTED` 的 `FAILED`（`.../NotifyAttachmentSnapshotTransactions.java:95-105`）。定时任务继续扫描该 `QUEUED` 关系，却永远不能解引用。把 `UNDELIVERABLE` 纳入严格未发分支，同时保持跨其他 Mail Delivery、COPY_UNKNOWN、活租约和已进入 Provider 的保守拒绝；加 fresh MySQL 用例覆盖 USER 没邮箱、无 Outbox、原源引用最终受控解除。

**P3：新自有附件表的实体未实现 PERSIST-003 乐观锁与逻辑删除映射。** `NotifyIntentAttachment.java:8-28` 有普通 `version`/`delFlag` 字段，却没有 `@Version`/`@TableLogic`；`NotifyNotificationDao.java:57-59` 的 `saveAttachment` 只是 `attachmentMapper.updateById(value)`，并无手工 `WHERE version = old` 比较。`NotifyAttachmentSnapshotTransactions.java:43,63,75,111` 先把版本加一再调用该 update，写入新数字不构成 CAS；与 DDL 已有的 `version`/`del_flag` 两列及工程标准 `PERSIST-003` 不符。补两个字段注解，并删去四处手工 `version+1`，由 MyBatis-Plus 乐观锁插件生成旧版本谓词和单次版本增长；所有更新继续要求 rowcount=1。用真实 MySQL 验证旧版本写 0 行并回滚，以及逻辑删除关系不再作为可发送/可回收行；不能只校验七列存在。

**P1 的最小修正设计核对（本段为 A1 审查后建议，不是 A1 已实现事实）。** 现有 `claimedFromReady` 是领取时瞬态，`DEADLINE_UNSENT_READY`/零 attempt/无回执没有记录 Provider 开始时刻，因此不能代替持久证明。可给每条 `notify_intent_attachment` 增单向 `send_reserved`，初始 false；在 `NotifyDispatchResultUseCase` 专用 `beginMailProviderSend(lease)` 短事务中按 Intent→Outbox→Delivery→附件关系锁序，先执行原 `deadlineGateLocked`，只在活 MAIL 任务且全部关系 READY/未 RELEASED 时把全部关系置 true，并在该事务确认提交后才进入物理 Mail sender。提交失败或 ACK 未知不得发信；如 DB 实已提交，true 标记仍保守保留。`releaseIfSafe` 须要求所有关系明确 false，null 或任一 true 均保留；无附件继续走原 gate/零 OSS。这样真正未发取消和 UNDELIVERABLE 可释放，曾取得发送权的 Intent 即使取消、租约到期或回执丢失也不误释放。必须以跨租约取消/迟到 sender、事务回滚或提交 ACK 未知、未发终态和同 Intent 多 Mail delivery 的 fresh DB 测试证实；同时保留 P2 的终态修复。

## 静态已核对的实现合同

- `NotificationCommand.attachmentOssIds` 是可空缺省空的 `List<String>`；`normalizeAttachmentIds` 严格正十进制、Long 范围、去重保序且限去重后件数（`.../NotificationApplicationRuntimeService.java:424-449`）。原提交者在提交线程由 `SessionNotifyAttachmentActorAdapter` 读取 LoginUser 的 userId/Client PK，Intent 持久保存；Worker 使用该 Intent，不读自己的登录上下文（`.../SessionNotifyAttachmentActorAdapter.java:10-15`、`.../DispatchNotificationService.java:117-135`）。System 在配置→源对象锁下核 user/Client、正常登录域、ACTIVE、PRIVATE/PUBLIC_READ及元数据并绑定真实关系 PK（`.../SysOssServiceImpl.java:380-397,609-657`）；`status` 是默认配置位，不能误判为配置启停。无附件路径在提交/Worker 均不调用 System OSS。
- 关系先持久化，源引用与 Intent/Outbox 同事务；快照以 Intent→关系锁预约真实 `NOT_READY` 目标和引用，再在事务外按总 30 秒预算执行源 GET、目标 PUT、目标回读 SHA-256，确认目标 ACTIVE 与关系 READY 同事务（`.../NotifyAttachmentSnapshotTransactions.java:25-77`、`.../SysOssServiceImpl.java:399-529`）。COPYING/ACK 未知转 `COPY_UNKNOWN`，保留稳定关系、键、源/目标引用；READY 复用在物化时重新核 actor、两引用、目标 PRIVATE 及摘要（`.../NotifyAttachmentSnapshotAdapter.java:26-77`、`.../SysOssServiceImpl.java:531-578`）。普通元数据/下载 URL 拒绝 NOT_READY。common `OssClient` 新增方法保留旧签名，SDK Get Publisher 累计字节限额和剩余时限、Put 有界请求；超时不被当成远端未写证明。
- Demo `demo-mail` 模板只要求 title/content，不借用仍必填 path 的 notice/workflow；Demo 附件由 typed 字段提交，旧 `templateParams.attachmentOssIds` 路径已移除。Mail 在全部附件物化后、物理 sender 前执行活租约/持久截止 gate；闭合异常及 DB 异常位于 SMTP UNKNOWN catch 外，Common Dispatcher 释放本次 Redis 幂等 owner 并抛出原 gate 异常（`.../MailNotifyChannelAdapter.java:58-92`、`.../NotifyDispatcher.java:94-112`、`.../DispatchNotificationService.java:111-147`）。内部回调被排除在 FULL/REDACT 监控事件及 JSON 请求外。

这些是静态调用链事实，不代替真实环境验证；上述回收两个缺陷仍阻断合同。

## 15 个 full-context 用例与当前证据边界

`NotifyMailAttachmentIntegrationTest.java` 有 15 个 `@Test`，以完整 `NamewtaApplication`/生产 Snapshot SPI、System/Notify DAO、真实 MySQL/Redis/MinIO 与仅物理 Mail sender 替身为目标；源码覆盖无附件零 OSS、单/双附件字节与顺序、跨 User/Client、排队后 PENDING/登录域撤销、同 Intent 多收件人复用、关系 READY 写失败、2xx PUT ACK 丢失、复制跨租约/截止、PUBLIC→PRIVATE、同幂等键并发、READY 私有策略重检及未领取取消回收。没有实际 fresh XML/零 skip 结果前不能说这些已通过。

当前 A1 测试/工具还存在确定的执行不一致：三个 hold 场景先 `/release-hold` 转为 `NORMAL`，在 `finally` 又要求 `/disarm` 返回 200（测试 `:392-397,419-428,526-533`）；冻结 v4 私有 proxy 的 `/disarm` 只接受 DROP 或 HOLD_CANCELLED，故会返回 409。Lead/ops 已选择准备 v5 使 NORMAL 且无活 hold 的清理命令幂等，A1 产品测试不应靠放宽源代码 gate 绕过。三个场景的异步 `runAsync(worker::poll)` 在异常路径没有一律等待/终止，可能污染随后按顺序执行的用例；需在工具/测试夹具中收束。v4/v5 均不是产品源码通过证据。

即使这 15 个用例变绿，票据与派单列出的若干必要观察仍需补足或给同一固定源码的其他 fresh 证据：实际 HTTP 对 quoted `9007199254740993` 与正式 OpenAPI `items.type=string`、非 required（当前只有单元 JSON roundtrip，未作 HTTP）；第 N 个附件失败后前面 READY 快照和全部引用仍有主、SMTP=0；持久 ACK 未知/DB 结果提交异常与安全回收；缺失来源/失效配置在提交及复制时的拒绝；两个幂等出口中特别是唯一键竞争回读及换 actor/Client 的负例（`CountDownLatch` 同时启动不保证走唯一键冲突）；前述无邮箱及取消跨旧 sender 在途反例。不要把 unit/mock、控制代理的计数或原始测试文件存在夸大为这些运行事实。

最终审查应等 Lead 的定向/默认/full、104 表六 SQL、真实 Mail+OSS 驱动、完整当前 OpenAPI 捕获/生成、精确 clean source 与清理证据。当前未验证生产部署或真实 SMTP；物理 sender 替身仅证明进入适配器前的字节与调用次数。
