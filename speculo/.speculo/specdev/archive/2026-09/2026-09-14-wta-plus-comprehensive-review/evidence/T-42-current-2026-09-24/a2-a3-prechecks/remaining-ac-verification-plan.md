# T42 A1 后续验收最小方案（只读设计）

固定产品输入 `0595e2ccbd03474bced92809b2ac9056c9a15751`；下列是补足 revision227 已列证据缺口的建议，不代表该候选通过。只在 owned MySQL/Redis/MinIO 与 loopback HTTP 执行；Lead 串行运行、保留 fresh XML/安全汇总、精确 clean source 和资源清理。测试增量落已授权 `backend/wta-admin/src/test/java/org/namewta/test/notify/`，不需要生产观测端口或新状态平台。

## 真实 HTTP 字符串 ID 与缺省/非法值

复用 `NotifySupportedModeIntegrationTest.java:293-341,472-536` 的真实 loopback Jetty/DispatcherServlet/SaToken 权限入口，或复用 `/tmp/wta-t44/run-minio-real-v8.py` 的完整 JAR、真实登录和 `/resource/oss/uploads` owned 对象夹具；两者不能混称同一种覆盖。**优先完整 JAR**：fresh 六 SQL 104 表，登录拥有 `notify:notification:submit` 的 owned 管理用户，上传得到其真实源 OSS ID（必须超过 `2^53`、DB `sys_oss.create_by`/`ext1.uploaderClientPk` 与登录身份一致），请求 `POST /notify/notification`，场景 `demo-mail`，`channels=["MAIL"]`、`strategy=ALL`、`mode=ASYNC`、显式 `priority=0`、title/content、远未来计划时间避免 Worker 抢跑。正文 `attachmentOssIds:["9007199254740993"]` 的特定数字若非真实已授权源必须先以该 owned ID 建对象；否则使用实际生成的大 ID 验证成功，并另以固定 quoted 9007199254740993 建精确 owned 行。响应 `R.code=200`、通知 ID 是字符串，DB 关系的 `source_oss_id` 十进制精确相等、身份源自服务端、不发生舍入或额外引用。正式 live OpenAPI/生成物须 `attachmentOssIds` 非 required 且 `items.type=string`。

同一真实 HTTP 链再提交省略字段和显式 `[]` 两例，均成功且关系数 0、OSS 代理计数提交阶段不变。对 `["0"]`、`["-1"]`、`["1.5"]`、`["9223372036854775808"]`、空白/前导零作逐例负向，`R.code != 200`，并以 app/key 定位五张 Notify 相关表、`sys_oss_ref` 与 S3 计数零增；错误不回显私有源/凭据。Java `List<String>` 的 Jackson 是否接受**非字符串 JSON 数字**要单独观察，不能仅从 OpenAPI 推断运行时拒绝；revision223 明定传输为字符串，若实际被强制转换，记录为独立合同判断，不把字符串格式负例冒充类型负例。无权 token/未登录也必须分别拒绝且零写。公开 `NotificationController.java:25-30` 走真实权限；`NotificationCommand.java:30-45` 缺省空、`NotificationApplicationRuntimeService.java:424-438` 严格解析是需要被 HTTP 证实的源码路径。

## 提交 ACK 未知：复用真实 JDBC 代理

`NotifyAtomicResultIntegrationTest.java:91-138,923-936` 已有真实 MySQL `DelegatingDataSource`/`Connection.commit` 故障注入：`BEFORE` 杀 owned 会话，`AFTER` 真 `connection.commit()` 后抛 `SQLRecoverableException`。`OssStorageMigrationIntegrationTest.java:119-138` 还有按动态事务 XID 定位单笔提交的实例。把这一局部测试接缝复用到 T42 full-context 夹具，且以唯一 run ID/一次性 arming 定位目标事务，不拦截所有 JDBC commit。两条最有价值的分支：

1. 对提交 Intent+关系+来源引用那一笔 AFTER 注入：API 抛出 ACK 未知；独立新连接查明实际只提交 1 Intent、每来源 1 关系/1 ref，使用相同 actor、client、app/key 再提交返回原 ID，不能插第二份；BEFORE 分支查无部分行/ref，后续同键正常成功。此处目标是提交幂等与真实 DB 原子性，不声称模拟真实网络不确定的概率。
2. 对修复后的 `beginMailProviderSend` 单笔 AFTER 注入：必须在关系 READY、活 MAIL lease 且物理 sender 尚未进入时 arm；方法抛出，`CaptureSender` 次数 0，新连接看到 `send_reserved=true` 后回收仍保留 refs；BEFORE/确定回滚则 marker=false、sender 0。若通过 `worker.poll()` 触发，代理只在观察到本连接对 `notify_intent_attachment.send_reserved` 的目标更新后拦该 XID 的 commit，避免错误命中早期复制事务。不要用现有 `READY` 更新触发器（`NotifyMailAttachmentIntegrationTest.java:325-340`）冒充 ACK 未知，它只证明确定回滚。

故障代理必须在 finally 撤销 arming、关闭连接/线程；SQL 异常仅写固定类别和阶段，不把 JDBC URL、token、私有源键进公开日志。提交异常后绝不直接重发 SMTP 来“探测”。

## 唯一键冲突回读必须确实命中

现有 `NotifyMailAttachmentIntegrationTest.java:483-509` 的 `CountDownLatch` 只保证两个线程同时开始，可能第二线程在首个事务提交后走前置 `findDuplicate`，不能证明 `NotificationApplicationRuntimeService.java:109-119` 的 `DuplicateKeyException`→`lockIntentByIdempotency` 分支。复用上面的 owned `DelegatingDataSource`：线程 A 按真实 Service 提交并在其目标 XID 的 **commit 调用前**由一次性 latch 挂起；线程 B 在 A 未提交期间完成前置普通 SELECT（观察该 SQL 已执行且无行）并进入同一 `(app_id,idempotency_key)` 的 INSERT，代理在调用 JDBC insert 前发出 B-stage signal；然后释放 A commit。B 由 MySQL 唯一索引 `uk_notify_intent_app_idempotency` 返回 DuplicateKeyException，并用 `SELECT ... FOR UPDATE` 当前读得到 A 的 Intent/关系；记录该当前读实际执行的计数/阶段（`NotifyNotificationDao.java:47-52,71-75`）。断言两 receipt 同一 ID、source/actor/Client/顺序完全相同、Intent/关系/ref 各一份；改变附件顺序或身份时 B 必须拒绝，不得把碰撞当幂等成功。所有 latch 都有短超时和 finally 释放，两个 Future 必须 join/terminate。不能在无 B 已进入 INSERT 证据时释放 A 后宣布覆盖冲突分支。

## 换 actor/Client

先由受信登录态 A+Client A 提交同 key/附件，再分别用真实有效 A+Client B、B+Client A 的新登录态提交**同 key/同附件**。验证两次均在原 intent/关系的 `attachment_actor_user_id` / `attachment_actor_client_pk` 精确比较处拒绝，不能靠客户端 actor JSON（API 无此字段）或源对象授权拒绝替代；Intent/关系/ref/Outbox 数和旧身份快照不变。`NotificationApplicationRuntimeService.java:441-465` 是待验证入口。优先用完整 JAR owned HTTP 两个用户与两个有效 Client 签发 session；如短期只能用 `NotifyMailAttachmentIntegrationTest.java:611-625` 的 `LoginHelper` 受信 fixture，也要 seed 对应 DB user/client 且分别断言实际 LoginHelper 返回身份，并在报告中明确它未证明 HTTP 登录/Client 切换。

这些补测与首候选 A1 的 P1/P2/P3 修复测试不同：后者需单独证明在途取消保留、UNDELIVERABLE 释放及 `@Version/@TableLogic` 真实数据库语义。所有观察结果都必须绑定后续固定 SHA；本文件没有执行 Maven、服务或实际 HTTP。
