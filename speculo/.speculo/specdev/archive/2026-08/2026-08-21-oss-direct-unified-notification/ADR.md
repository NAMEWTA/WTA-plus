# Change Architecture Decisions

本文件只保留当前有效的 change 级架构合同。访谈过程中被替代的方案不在此重复，节点级来源见 `LOG.md`。

## ADR-001: Client 是认证授权上下文而不是租户边界

**Status:** accepted
**Source:** LOG-008, LOG-010, LOG-031, LOG-037, LOG-041, LOG-048, LOG-055, user clarification
**Supersedes:** none

### Context
平台使用统一后端底座、统一业务逻辑和共享数据资源。多个 APP 前端通过不同 Client 进入各自业务域，但所有用户仍属于同一个大平台。

### Decision
Client 参与登录准入、角色、菜单、权限字符、数据权限和动态路由计算；userType 是由 Client 关联并写入会话的动态登录域。资源与接口是否可访问，由当前用户在当前 Client 下获得的权限和数据权限决定；Client 与 userType 都不构成 tenant，不生成通用数据所有权，也不要求业务表按 `client_pk` 分区过滤。

当前 Token 中的 `sys_client.id` Long 主键可以作为调用来源审计字段写入通知日志或 UploadTicket，建议字段命名 `client_pk`；它不得参与 OSS/通知数据隔离、Provider 路由或通知幂等作用域。无 Client 上下文的后台任务不需要伪造 SYSTEM scope。

### Trade-off
统一平台可以共享用户与业务资源，不会把 APP 入口误建模为租户；代价是每个业务接口必须正确执行权限字符和数据权限校验，不能依赖 Client 行级过滤兜底。

### Consequences
`clientId` OAuth 字符串与 Token/RBAC 中的 `clientPk` Long 主键仍须严格区分。任何新增 `client_pk` 业务数据隔离都必须由独立领域需求明确授权，不能从认证 Client 自动推导。

### Verification / Migration
验收覆盖：不同 Client 获得不同角色、菜单、权限和路由；同一共享资源是否可访问由权限与数据权限决定；不得出现 tenant 式自动 Client 数据过滤或跨 Client 权限并集。

## ADR-002: OSS 分离控制面与数据面

**Status:** accepted
**Source:** LOG-001, LOG-002, LOG-003, LOG-025, LOG-048
**Supersedes:** none

### Context
现有上传和下载由后端代理文件字节，下载还会把完整对象读入 JVM `byte[]`，造成带宽与内存压力。

### Decision
浏览器使用短时预签名请求直接与 OSS 交换字节；后端只负责认证授权、对象 Key、签名、UploadTicket、完成校验、`sys_oss` 元数据和业务访问控制。旧浏览器上传、下载 HTTP 协议直接移除，业务只持久化 `ossId`。

管理面下载继续要求 `system:oss:download`。普通业务由自身 Service 完成权限与数据权限校验后内部生成短时下载 URL；知道 `ossId` 不代表拥有访问权。

### Trade-off
消除后端数据中转，但需要前后端同步破坏性迁移，并要求 Bucket CORS 正确配置。

### Consequences
`sys_oss` 是全局资源元数据，不增加 `client_pk`，也不按 Client/userType 过滤。全局元数据不等于公开下载。

### Verification / Migration
验证后端不再接收浏览器文件流或返回对象 `byte[]`，并覆盖越权 ossId、签名过期、取消和已知前端调用全部迁移。

## ADR-003: Multipart 使用 Redis UploadTicket 与双层清理

**Status:** accepted
**Source:** LOG-001, LOG-002, LOG-011, LOG-012, LOG-013, LOG-024, LOG-032, LOG-033
**Supersedes:** none

### Context
浏览器大文件上传需要保存 uploadId、对象 Key、Part 与恢复信息，但不应给 `sys_oss` 引入 PENDING 上传状态。

### Decision
单 PUT 与 Multipart 都由 Redis UploadTicket 管理；Ticket 默认续传窗口 24 小时，Part URL 为分钟级短时签名并按并发窗口批量申请。重新选择同一文件后可基于指纹与 ListParts 继续上传。

应用通过过期索引主动 Abort 未完成 Multipart，Bucket Lifecycle 作为兜底。CORS 与 Lifecycle 由部署配置，应用只诊断，不修改 Bucket policy。

### Trade-off
对业务表侵入小且支持断点续传，但 Redis Ticket 丢失后不能自动恢复会话，并需要清理重试与完成幂等。

### Consequences
完整性以 HTTPS、Part ETag、Complete 和最终 HEAD size 为兼容基线；标准 checksum 仅在 Provider 支持时增强，Multipart ETag 不作为整文件 MD5。

## ADR-004: 上传安全由静态命名策略控制

**Status:** accepted
**Source:** LOG-034, LOG-035, LOG-039, LOG-044
**Supersedes:** none

### Context
Direct Upload 的文件字节不经过 Controller，前端组件的扩展名和大小检查不能构成安全边界。

### Decision
服务端通过类型化 `ConfigurationProperties` 从 YAML/环境变量加载命名 `uploadPolicy`，启动时校验。策略规定允许类型、最大大小、对象前缀、SINGLE/MULTIPART 模式、阈值、`requiredPermission` 和可选 Client 准入范围；这里的 Client 仅用于决定某 APP 入口是否能调用该策略，不形成对象数据隔离。

前端只提交策略键。init 时校验权限；resume、part sign 和 complete 使用 Ticket 中冻结的策略。Complete 后执行 HEAD、策略约束和适用的 magic bytes 校验；首版不建设杀毒或隔离状态。

### Trade-off
策略变更需要配置发布，且有限文件头校验不能替代恶意文件检测；换取了可审查且不信任客户端的上传边界。

### Consequences
未知或无权策略拒绝。校验失败删除对象、清理 Ticket 且不生成 `ossId`。配置错误必须阻止启动或明确禁用对应策略。

## ADR-005: OSS 使用 TEMP 状态与轻量多业务引用

**Status:** accepted
**Source:** LOG-040, LOG-045, LOG-046, LOG-047, LOG-050, LOG-051, LOG-053
**Supersedes:** none

### Context
上传 Complete 与业务数据保存之间存在孤儿窗口，同一物理对象还可能被多条业务数据引用。公共 OSS 层不应理解各业务表的字段和权限规则。

### Decision
通过校验的新对象写入 `sys_oss` 时为 TEMP，并设置可索引的过期时间；默认从 Complete 起保留 24 小时。新建 `sys_oss_ref` 保存多条轻量反向定位关系，唯一粒度为 `oss_id + ref_type + ref_id`：`ref_type` 必须是实际物理表名，`ref_id` 必须是该表真实主键，不记录字段槽位。

需要长期保留文件的业务在保存成功后登记引用。最后一条引用解除时，对象重新进入 TEMP 并从解绑时获得新的 24 小时宽限期。关系只用于 OSS 管理展示和基础生命周期判断，不建立跨表外键、反射查表、通用业务回调或权限推导。

### Trade-off
OSS 管理面可以定位文件来自哪张表、哪条数据，并安全支持共享对象；代价是数据库不能验证多态引用，表名变更需要同步迁移引用值。

### Consequences
前端没有通用 bind 接口。后端业务 Service 在确认用户有权使用文件后调用内部引用能力。通用 OSS 删除拒绝仍有引用的对象。无引用对象先在独立事务中持久化 `delete_state=PENDING` 和到期 TEMP 状态，后续清理幂等删除 Provider 对象再删除元数据；Provider 或数据库失败均保留 PENDING 供重试，新的业务 bind 在行锁内取消 PENDING。`sys_oss_ref` 是项目自有表，遵守七个基础字段和 NAMEWTA DDL/DSL 只追加合同。

## ADR-006: common-notify 是薄的渠道契约层

**Status:** accepted
**Source:** LOG-004, LOG-005, LOG-009, LOG-014, LOG-015, LOG-021, LOG-022, LOG-027
**Supersedes:** none

### Context
Mail、SMS 是原子基础设施，站内消息由 `SysMessage + common-push` 承担。完整通知中心会重复现有领域并扩大上游差异。

### Decision
新增 `ruoyi-common-notify`，只拥有 `NotifyClient`、Dispatcher、Channel SPI、请求/结果模型、上下文 SPI 和 Delivery Event。Mail、SMS 保留原子模块并提供 Adapter；站内消息不纳入本次统一通知。

一个 `NotifyRequest` 只对应一个 Channel，但允许多个物理 Target 并保留邮件 TO/CC/BCC。调用方通过 `ruoyi-api` 或自身服务先解析 PHONE、EMAIL、OPEN_ID；common 不解析 USER。Provider 使用全局默认配置，可由 `providerKey` 覆盖，不按 Client 路由，也不自动故障切换。

### Trade-off
依赖方向清晰并方便扩展微信、飞书等渠道；模板中心、用户偏好、多渠道编排、可靠重试、回执与站内信统一延后。

### Consequences
Captcha、Workflow、Demo 等已知 Mail/SMS 调用一次迁移到 `NotifyClient`，原子 Builder/SDK 只允许在 Adapter 内使用。common 不得反向依赖 system。

## ADR-007: Provider 同步发送并提供可选 Redis 幂等

**Status:** accepted
**Source:** LOG-006, LOG-026, LOG-036, LOG-041, LOG-042, LOG-043, LOG-049
**Supersedes:** none

### Context
业务需要同步知道 Provider 结果，同时内部 Service、Job 和 Workflow 不能依赖 Controller `@RepeatSubmit` 防止重复发送。

### Decision
Dispatcher 同步调用 Provider。多目标请求尝试全部合法目标；任一失败时抛出携带完整 `NotifyResult` 的 `NotifyDeliveryException`，业务边界自行决定传播或容忍。

`idempotencyKey` 可选，Redis 作用域为 `Channel + 业务 Key`，默认窗口 5 分钟并允许在配置上下限内覆盖，审计 `client_pk` 不进入 Key。同 Key 不同摘要拒绝；IN_PROGRESS 命中抛出 `NotifyInProgressException`；完成后复用首次结果并写 `SKIPPED_DUPLICATE` 逻辑日志，不创建 Delivery attempt。Redis 不可用时，带 Key 请求 fail-closed 且不调用 Provider；无 Key 请求正常发送。

### Trade-off
调用方能明确处理部分成功和重复状态，但同步发送会承担 Provider 时延；Redis 幂等不是 durable exactly-once，也不提供服务重启后的可靠重试。

### Consequences
幂等不触发 Provider failover 或自动重试。未来需要最终必达时引入 Outbox 或 durable queue，不改变当前同步合同。

## ADR-008: Event 只承担全局通知监控

**Status:** accepted
**Source:** LOG-007, LOG-016, LOG-017, LOG-018, LOG-019, LOG-020, LOG-023, LOG-029, LOG-030, LOG-031, LOG-037, LOG-038, LOG-055
**Supersedes:** none

### Context
Spring Event 适合解耦监控落库，但不提供可靠队列语义。用户要求完整保留通知内容并提供可运维的管理面。

### Decision
Dispatcher 在成功、失败或幂等跳过后发布携带同步上下文快照的 Event；`@Async + EventListener` 只做 best-effort 监控。`NotifyContextResolver` 在发布前解析 userId、traceId 和可为空的来源 `client_pk`；异步 Listener 不读取 ThreadLocal。`client_pk` 只作审计展示，通知日志是全局监控数据，不按 Client 隔离，也不存在 SYSTEM scope。

`sys_notify_log` 保存一次逻辑通知及正文，`sys_notify_delivery_log` 保存逐目标 attempt、状态、耗时、错误和 Provider Message ID。普通业务通知默认使用 `FULL` 审计策略；验证码、重置 Token 等 credential-like 通知必须显式使用 `REDACT_SENSITIVE`，不保存主题、正文、模板参数或内容快照，物理目标只保存脱敏值。模板通知缺少 contentSnapshot 时仍不得发送。

首版交付数据库、分页/详情/删除 API 和前端监控页面。列表 Target 脱敏，FULL 详情在相同查询权限下返回明文；REDACT_SENSITIVE 数据不可恢复。`system:notify:remove` 支持删除、批量删除和清空并记录 OperLog；清理固定批次物理删除父子日志。HTML 正文只能安全文本展示或隔离预览。

### Trade-off
监控查询直接且普通通知内容可完整追溯；应用宕机窗口可能丢日志。credential-like 通知因最小化审计无法恢复原文，这是降低数据库泄露与权限误授影响面的明确取舍。

### Consequences
Provider Secret、Authorization 和无关内部异常仍不得入库。两张项目自有表遵守七个基础字段与 NAMEWTA DDL/DSL 只追加合同。

## ADR-009: 通知附件使用发送前独立快照

**Status:** accepted
**Source:** LOG-028, LOG-052, LOG-054
**Supersedes:** none

### Context
直接引用源业务附件会使历史通知随源文件删除或变化，发送后复制又无法证明快照就是 Provider 实际使用的内容。

### Decision
业务请求只传 `attachmentOssIds`。Dispatcher 预生成 `notifyLogId`，在调用 Provider 前复制一组通知专用 OSS 对象，并使用快照执行实际发送。一次逻辑通知的所有 Delivery 共用该组快照；以 `ref_type=sys_notify_log` 和真实 `notify_log_id` 建立引用。

任何复制失败都终止通知且不调用 Provider；Provider 失败仍保留快照。异步日志落库失败时快照保持 TEMP 并在 24 小时后清理；部分复制失败幂等删除已创建副本。删除通知日志时解除快照引用，零引用对象重新进入 TEMP 宽限期。

### Trade-off
发送内容与归档快照一致，且源业务生命周期不影响历史附件；代价是同步复制增加时延与存储，永久通知日志默认也永久保留附件快照。

### Consequences
`common-notify` 通过资源/快照 SPI 使用附件，不向业务暴露 `File`、`InputStream` 或 system 实体。附件缺失、无权或复制失败不得静默降级为无附件发送。

## ADR-010: OSS 引用生命周期由业务写入 module 拥有

**Status:** accepted
**Source:** LOG-059, LOG-060, LOG-061, LOG-062, LOG-063
**Supersedes:** none

### Context
TEMP 对象只有在业务记录建立 `sys_oss_ref` 后才获得长期生命周期。用户头像与公告已经分别需要协调旧新 ossId、业务写入和 `bind/unbind`；把这类协调完全交给中央 module 会迫使它理解业务授权、事务、表结构与删除恢复语义。

### Decision
每个持久化 ossId 的业务写入 module 是该引用的 Business OSS Owner。它通过自身稳定 interface 完成授权、业务记录保存、旧新附件差异和引用生命周期；跨业务共享的内部 module 只承载集合差异与引用转换的机械规则，不读取、回调或反射业务表，也不推断 ACL。

业务记录保存与引用转换处于同一 `@DSTransactional` 并 fail-closed；任一步失败回滚全部数据库变化，不建设 best-effort 引用修复队列。逻辑删除后的附件保留由 owner 的真实恢复合同决定：无恢复合同则在删除事务内立即解绑；有明确恢复能力则保留引用直到不可恢复清理，不能从 `@TableLogic` 自动推断统一策略。

当前项目是没有历史数据负担的基座系统。建立全仓显式 owner 清单并直接验证 fresh baseline，不建设存量回填、兼容窗口或旧数据迁移。所有新基线数据从创建时建立引用。

owner 清单同时是未来变化的 ratchet：每个新增持久化 ossId owner 必须显式登记，并通过聚焦架构/契约测试证明 insert/update/delete/restore（适用时）的引用转换与失败回滚。清单和测试只在设计与交付阶段提供门禁，不成为运行时注册中心；不采用注解扫描或动态业务回调。

### Trade-off
相比中央协调、注解驱动或提交后修复方案，业务 modules 仍保留少量显式 ownership 代码；换取授权、事务、失败和恢复语义的 locality，并避免 common 或 system 基础能力反向理解业务表。

### Consequences
新增 ossId 持久化所有者必须进入显式清单并提供 insert/update/delete/restore（适用时）引用合同证据。引用转换失败必须证明业务事务回滚。真实物理表名和主键、引用非 ACL、无前端通用 bind、无动态查表等 ADR-005 决定保持不变。

### Verification / Migration
只验证 fresh install 与 fresh data，不执行历史兼容或回填。TEMP 主动清理在 owner 清单、聚焦合同测试和 dry-run 核对通过前保持 disabled；启用清理仍是独立发布批准点。
