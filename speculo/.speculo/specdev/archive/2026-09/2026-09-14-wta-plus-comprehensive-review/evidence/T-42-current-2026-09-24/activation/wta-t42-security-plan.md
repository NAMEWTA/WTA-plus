# T42 附件合同安全/事务设计预审（只读）

固定产品输入：`5417c257130216e2b283ca933d9496d76c10f46a`；输入 Ticket 42、`/tmp/wta-t42-current-seams.md`、`/tmp/wta-t42-activation-audit.md`。本报告是实施前审查，未修改仓库、构建或运行服务，未宣称 AC-042 已通过。

## 必须锁定的边界

1. **无环归属。** Notify 拥有 `notify_intent`、新增真实物理关系行、原提交者身份和快照状态；System 拥有 `sys_oss`、`sys_oss_ref`、按原 service 的 OSS 复制/物化。Notify 的 adapter 只依赖 `wta-api` 的受限 System 端口；System 不导入 Notify Mapper/Entity，common-notify 不导入 System 实现。`NotifyLogIdGenerator` 的旧日志 ID 不能充当 `sys_oss_ref` 的 owner，必须改为实际已提交的关系主键。公开 Java SPI/DTO 与所有调用者按 `java-api-compatibility` 同批迁移。
2. **身份来源。** 非空附件在入口由可信登录上下文取 user ID 和 Client **PK**，持久化为不可变授权事实。`NotificationCommand` 的 map、metadata、`appId`、请求体中的 user/client 值、worker 线程的空 `NotifyContext` 均不能充当授权依据。Demo 菜单 `system:oss:download` 仅是入口权限，不证明某个 OSS ID 属于此 user+Client。未定义可信服务主体合同时，非登录的非空附件请求失败关闭；无附件的既有可信内部通知路径不被牵连。System 的受限 API 在源 OSS 行锁下比较上传者 user+Client、`ACTIVE` 生命周期、存储 service/对象身份及已批准的共享规则，然后才绑定引用或复制；来源 Client 事实缺失不得退化为全局可读。Worker 只使用已持久 actor，并对源事实再次核验。
3. **不能把通用 API 当授权 API。** `OssService.objectMetadata` 明言不授权，现实现仅拒绝 `PENDING`；`OssLifecycleManager.bind` 在绑定时甚至会将 `PENDING`/临时态恢复。因此“先 objectMetadata、后 reconcileReferences”存在检查与绑定间竞态，也可能复活不可用对象。最小 System 新方法应把授权、状态核查及源引用绑定放在同一 OSS 行锁和提交事务内，要求 `refType=notify_intent_attachment`、`refId=真实关系 PK`；Notify 的 Intent/关系写入与该绑定处于同一动态数据源事务，任何一步失败整笔回滚。按正整数、有界数量、去重后保序规范化附件 ID；多个 OSS 行按 ID 排序获取锁，避免反向锁序。
4. **临时来源与快照引用。** 在提交 Intent/Outbox 时先创建每个关系行，再在同一事务绑定各来源引用；成功提交后来源即使到期，临时清理也应被 `sys_oss_ref` 阻止。复制完成并持久化私有快照 OSS 行/真实关系引用前不得释放来源。结果不确定、部分完成或待恢复时保留可核对的来源/快照引用；只有能证明不会再被 worker/Provider 使用的终态清理才逐个解除。快照须属于独立私有通知对象，不能把原来源 URL 或现默认 OSS 配置当附件快照。
5. **两条重复提交出口。** `NotificationApplicationRuntimeService.submit` 的前置 `findDuplicate` 和 `DuplicateKeyException` 回读都在新校验/持久化前返回 receipt。两处必须用同一个判定：持久 actor user+Client、规范化有序附件 IDs（及既有命令幂等合同）与新请求一致，且调用者当前仍具所需权限，才返回原 receipt；否则冲突/拒绝，不能借同一 key 获取他人 Intent 或快照。普通无附件幂等行为应保留。通知通用 Redis digest 含附件 ID，但不能代替这里的数据库判定。

## 跨 MySQL/OSS 的最小安全阶段

| 阶段 | 持久事实与动作 | 失败/并发判定 |
|---|---|---|
| 提交 | 单事务写 Intent、ordered 关系行及来源引用、Outbox；记录可信 actor 和源身份。无附件零 System OSS 调用。 | 权限/状态/任一绑定失败全部回滚，零 Outbox；不能先外部复制再提交 owner。 |
| 复制预留 | Worker 用 Intent→相关任务/关系锁及 owner/CAS 领取同一关系，持久预留从关系 PK 与 generation 导出的目标 key 和待复制状态，提交后再做外部 I/O。来源仍有引用。 | 同一关系并发只能有一个当前 owner；旧 owner 完成须 CAS=0 且不得覆盖。若旧 I/O 是否仍在运行无法证明，不启动可能覆盖同一 key 的新复制。 |
| 外部复制/物化 | System 按**来源原 service**读取，经受限端口再次核对 actor、源物理身份/size/content type；复制到独立 private key。返回精确目标对象身份并实际读出 bytes 给物理 MAIL 适配器。 | 任一附件失败则 SMTP 调用为 0；不能把仅生成 URL 或 SPI mock 算生产闭环。 |
| 完成 | 短事务 CAS 将同一关系标记为已验证快照，持久化快照 OSS 行及 `sys_oss_ref`；源引用仅在已确认快照安全独立存在且后续恢复不再需要源时释放。 | copy 成功而 DB 提交失败/ACK 未知：读取已提交关系及确定 key，HEAD/内容或强身份校验后复用；不盲目再 copy、不盲目 DELETE。无匹配或状态不确定保留 `UNKNOWN`/待人工核对。 |
| Provider 结果与回收 | 同一 Intent 的快照跨安全重试复用。ACCEPTED/Provider UNKNOWN 保留引用；明确未发送的终态才按关系 owner 做受控补偿。 | 现 `NotifyDispatcher.cleanupSnapshots` 在全部 `UNSENT_RETRYABLE` 时调用且吞异常；生产适配必须以持久关系判断能否清理，不能依据本次 Java List 任意删仍被 Intent/未知 Provider 使用的快照，补偿失败须留可查询恢复事实。 |

稳定 key 单独并不足以证明可覆盖/删除：恢复须同时匹配关系 PK、代际/owner、源对象身份、预期私有 service、目标 bytes 或强校验值。若 DB 里没有已提交预留行，复制本不应开始；若外部已产生孤儿，需按明确 owner/key 的审阅式清理，绝不可按前缀扫删。多附件只要一项未验证完整，整封 MAIL 不得进入物理发送。Retired `sys_notify_log` 不能恢复为关系 owner。

## 最少失败矩阵与证据

以 fresh 六 SQL MySQL、owned Redis/MinIO、完整 Spring Notify+System Bean 和读取真实附件 bytes 的假物理 MAIL sender 验证；假 sender 只替代 SMTP，不替代生产快照 SPI/System 授权/真实对象。每例记录数据库 Intent/关系/sys_oss_ref/状态、对象 HEAD/GET/bytes、sender 次数及 fresh 测试计数，零 skip；私有凭据和签名 URL 不进证据。

| 用例 | 必须断言 |
|---|---|
| body-only `demo-mail`，以及 notice/workflow 空 path | 前者发出正文且 OSS lookup/copy/materialize 全零；后两者保持拒绝。 |
| 同一 user+Client 的 1/2 个私有临时来源 | submit 后引用使到期清理不删来源；worker 从原 service 复制，邮件 bytes/name/order 正确，私有快照和真实关系 PK 引用均存在。 |
| 他人 user、同 user 异 Client、缺 Client、PENDING/缺失/错误 service；submit 后撤销源可用性 | 授权或生命周期失败时零邮件；提交前失败零 Intent/Outbox；worker 后置变化失败关闭，不通过 `bind` 复活 PENDING。 |
| 同幂等 key 的两条竞争/重复返回 | 相同 actor+有序附件只有一个 Intent/一组快照；不同 user、Client、附件列表分别在前置命中和唯一键冲突回读路径拒绝，零越权 receipt。 |
| 两附件第二次复制失败、目标物化失败 | 邮件零次，原来源不删除；已建目标要么安全补偿并证实引用为零，要么有可查关系/待恢复状态。 |
| 复制成功后关系/引用 DB 提交失败及 ACK 未知、旧 owner 迟到 | 再进入同一预留行核 target bytes/状态后收敛；不重复创建不同快照、不删未知目标、旧 owner CAS 不覆盖。 |
| Provider 明确未发送可重试、UNKNOWN、受理后结果提交失败 | 安全重试复用同一快照，数量不增长；UNKNOWN/受理不自动再发或清理；最终受控回收只解除确切 owner 引用。 |

实施写集预警：Ticket 覆盖 Notify runtime/DAO/adapter、System `oss/`、`OssService`、common attachment/core、六 SQL 和测试；若新关系使用 `wta-notify/.../mapper/` 或 XML、变更 `SysOssServiceImpl.java`、`NotifyDeliveryEvent.java`、其他 `NotificationCommand` 生产调用者/OpenAPI 生成物，须在编辑前逐路径登记。当前固定输入上不存在生产 Snapshot SPI Bean，故没有任何本票当前完成结论。
