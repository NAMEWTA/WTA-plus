---
schema_version: 3
artifact: spec
change: 2026-08-21-oss-direct-unified-notification
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-08-21-to-2026-08-22-design-consensus
  - ADR-001
  - ADR-002
  - ADR-003
  - ADR-004
  - ADR-005
  - ADR-006
  - ADR-007
  - ADR-008
  - ADR-009
  - CODE:ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-oss/src/main/java/org/dromara/common/oss/client/OssClient.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysOssController.java
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysOssServiceImpl.java
  - CODE:plus-ui-namewta/src/components/FileUpload/index.vue
  - CODE:plus-ui-namewta/src/components/ImageUpload/index.vue
  - CODE:plus-ui-namewta/src/components/Editor/index.vue
---

# Spec: OSS 浏览器直传与统一对外通知

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-08-21-oss-direct-unified-notification/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

当前浏览器上传仍将文件作为 Multipart 请求发送给后端，后端再把文件流上传到 OSS；下载则由后端从 OSS 读取完整对象并以 `byte[]` 返回。该路径重复消耗后端入口、出口和 JVM 内存，无法发挥对象存储的数据面能力，也不适合大文件与断点续传。现有 `OssClient` 虽已具备普通 PUT/GET 预签名能力，但缺少浏览器直传需要的 Multipart、对象 HEAD、完成确认和生命周期合同。

邮件、短信目前由业务代码直接调用各自 Builder 或 Provider SDK。调用方耦合渠道实现，未来接入微信、飞书等渠道时会继续扩散；同时缺少统一的发送结果、幂等、附件、事件和监控模型。现有操作日志已经证明 Spring Event 可解耦监控落库，但 Application Event 不能被误用为可靠发送队列。

平台中的 Client 是登录与授权上下文，不是租户或数据归属边界。本 change 必须防止 OSS 与通知设计把 `client_pk` 扩展为通用行级隔离、Provider 路由或幂等作用域。

### 目标用户与场景

- 使用 FileUpload、ImageUpload 或 Editor 的业务用户：直接向 OSS 上传文件，看到进度，可取消，并在刷新或断网后重新选择同一文件续传。
- 保存带附件业务数据的后端业务 Service：在完成自身权限和数据权限校验后，以 `ossId` 绑定或解除文件引用。
- 下载业务附件的最终用户：经业务授权取得短时 URL，文件字节由 OSS 直接返回。
- OSS 管理员：查看对象元数据、TEMP 状态及其实际来源表/记录，下载有权管理的对象，并避免删除仍被业务引用的文件。
- 通知调用方：通过一个渠道无关入口发送 Mail/SMS，获得逐目标结果和稳定的失败语义。
- 通知运维人员：查询一次逻辑通知及各目标 Delivery，查看完整正文和目标，并按权限人工删除或清空日志。
- 部署运维人员：配置命名上传策略、Bucket CORS/Lifecycle、Provider 与清理任务，并能发现错误配置。

### 成功标准

- 浏览器上传和下载的文件字节不再经过业务后端；完成上传后的业务返回值仅为 `ossId`。
- 单 PUT 与 Multipart 共用后端控制面；Multipart 支持批量签名、失败 Part 重试、重新选择同一文件后续传、完成确认和取消。
- 未完成 Multipart 与无业务引用的 TEMP 对象能够被幂等清理；被引用对象不会被通用清理或删除误删。
- 业务文件通过真实物理表名和真实主键反向定位，但引用关系不参与 ACL 或动态查表。
- Mail/SMS 的已知直接调用统一收口到 `NotifyClient`；业务不再依赖具体 Builder/SDK。
- 发送结果可表达多目标部分成功；可选 Redis 幂等不会重复调用 Provider。
- 通知正文、目标、模板快照和逐目标结果进入两层全局监控；异步监听失败不改变同步发送结果。
- Client 继续只影响登录准入、角色、菜单、权限、数据权限和动态路由，不产生 OSS/通知数据隔离。

### 非目标

- 不建设完整通知中心，不包含数据库模板管理、用户偏好、多渠道编排、Provider 自动故障切换、自动重试或最终必达。
- 不把站内消息、`SysMessage` 或 `common-push` 纳入统一对外通知。
- 不提供跨设备无感续传，不承诺 Redis Ticket 丢失后的会话恢复。
- 不在首版建设杀毒、内容隔离或恶意文件扫描。
- 不把 OSS 引用关系升级为动态外键、通用业务回调、反射查表或权限引擎。

## 2. 解决方案与外部行为

### 解决方案摘要

OSS 采用控制面/数据面分离。后端控制面负责认证授权、命名策略、对象 Key、UploadTicket、预签名、完成校验、`sys_oss` 元数据、TEMP 生命周期、业务引用和下载授权；浏览器通过只含签名必需 Header 的独立 transport 与 OSS 直接交换字节。

通知新增薄的 `ruoyi-common-notify`。它定义 `NotifyClient`、Dispatcher、Channel Adapter SPI、请求/结果/异常、审计上下文 SPI、附件快照 SPI 与 Delivery Event；`common-mail`、`common-sms` 保持原子模块并实现 Adapter。Dispatcher 同步调用 Provider，再用 Event 异步记录全局监控。

### 主要流程

#### OSS 初始化、上传与完成

1. 浏览器提交命名 `uploadPolicy`、原始文件名、大小、Content-Type 和续传指纹，请求初始化上传。
2. 后端从类型化配置加载策略，校验当前用户权限、可选 Client 准入、文件元数据和策略模式；对象 Key、Provider、Bucket 与前缀只由服务端决定。
3. 后端创建 Redis UploadTicket。SINGLE 模式直接返回短时预签名请求；MULTIPART 模式创建 Provider uploadId，并返回 Part 大小、数量和会话到期时间。
4. 浏览器使用独立 OSS transport 直接 PUT。MULTIPART 按并发窗口批量申请 Part 签名，记录每个 `partNumber + ETag`，失败只重传相应 Part。
5. 刷新或断网后，用户重新选择同一文件；浏览器用保存的 uploadToken、重新计算的指纹和 ListParts 结果恢复缺失 Part。指纹不一致时拒绝复用会话。
6. 浏览器请求 Complete。后端校验 Ticket 所有权和冻结策略；MULTIPART 交叉校验 Part 后由后端调用 Complete；随后执行 HEAD、最终大小、Content-Type、适用 magic bytes 和可用 checksum 校验。
7. 校验成功后写入 `sys_oss` TEMP 元数据并返回单一 `ossId`。重复 Complete 在可重试窗口内返回同一 `ossId`，不得创建重复对象或重复元数据。
8. 校验失败时不生成 `ossId`，并幂等删除对象、Abort Multipart 或登记补偿清理。

#### OSS 引用、清理与下载

1. 完成后的对象默认处于 TEMP，过期时间为 Complete 后 24 小时。
2. 业务 Service 保存业务记录后，在自身授权与事务边界内登记 `(ossId, refType, refId)`；`refType` 必须是实际物理表名，`refId` 必须是该表真实主键。
3. 新增首条引用使对象退出待清理状态；移除最后一条引用使对象重新进入 TEMP，并从解绑时重新获得 24 小时宽限期。
4. TEMP 清理任务只删除到期且仍无引用的对象，并以可重试、幂等方式协调 Provider 对象与数据库元数据。
5. 通用 OSS 删除遇到任一有效引用时拒绝。需要删除业务文件时，由业务 Service 先完成自身数据授权和引用变更。
6. OSS 管理面在 `system:oss:download` 授权后生成短时下载 URL。普通业务不调用通用 URL 接口，而由对应业务 Service 在完成对象权限和数据权限检查后内部生成短时 URL。
7. 浏览器导航或下载短时 URL，文件字节直接来自 OSS；业务数据只保存 `ossId`，不保存临时 URL。

#### 统一通知发送

1. 调用方将用户或领域对象解析为 `PHONE`、`EMAIL`、`OPEN_ID` 等物理目标，再构造一个 Channel 的 `NotifyRequest`；common-notify 不读取 SysUser。
2. 请求可包含多个物理目标。邮件保留 TO/CC/BCC 语义；一次 Provider 调用无法区分单目标结果时，各 Delivery 共享该调用结果和 Provider Message ID。
3. Provider 默认使用 Channel 的全局配置；可选 `providerKey` 显式覆盖。Client 不参与 Provider 选择。
4. 模板通知必须同时提供 Provider 模板编码、参数和应用认知的完整 `contentSnapshot`；缺少快照时在 Provider 调用前失败。
5. 请求只通过 `attachmentOssIds` 表达附件。Dispatcher 预生成 `notifyLogId`，校验源附件并在 Provider 调用前复制一组通知专用快照；实际发送必须使用快照对象。
6. Dispatcher 尝试全部合法目标。全部成功时返回 `NotifyResult`；任何目标失败时，在其余目标尝试完成后抛出携带完整结果的 `NotifyDeliveryException`。
7. Provider 接受请求只记为 `ACCEPTED`，不得称为 `DELIVERED`。首版没有 Provider 回执状态更新。
8. 成功、部分失败、完全失败或幂等跳过后发布带完整同步上下文快照的 Event。异步 Listener 只负责写监控，不改变同步返回或异常。

#### 通知幂等

1. `idempotencyKey` 可选；未提供时直接执行同步发送。
2. 提供 Key 时，以 `Channel + 业务 Key` 为作用域执行 Redis 原子占位，默认窗口 5 分钟；允许在配置的上下限内覆盖。
3. 同 Key、同请求摘要且原请求仍在执行时，立即抛出 `NotifyInProgressException`，不等待也不调用 Provider。
4. 同 Key、同摘要且原请求已完成时，复用首次 `NotifyResult`，新增关联 `originalRequestId` 的 `SKIPPED_DUPLICATE` 逻辑日志，不创建 Delivery attempt。
5. 同 Key、不同摘要时拒绝，不调用 Provider。
6. Redis 不可用时，带 Key 请求 fail-closed 且不调用 Provider；无 Key 请求不受影响。该失败必须可与 Provider 失败区分。

#### 通知监控与人工删除

1. `sys_notify_log` 每行表示一次逻辑通知，保存最终 subject/content、模板编码、参数、contentSnapshot、附件快照 ossId、业务关联、状态、来源 userId/traceId/`client_pk` 等。
2. `sys_notify_delivery_log` 每行表示一个物理目标的 Provider attempt，保存完整目标、目标角色、状态、耗时、错误和 Provider Message ID。
3. 普通业务通知使用 FULL 审计并保留正文和完整目标；OTP、重置 Token 等 credential-like 通知必须使用 REDACT_SENSITIVE，不保存内容字段且目标只保存脱敏值。
4. 监控列表只返回脱敏目标；详情按实际审计策略返回可用字段、Delivery 和附件快照。HTML 正文只能安全文本展示或隔离预览，不能直接作为可信 DOM 执行。
5. 拥有 `system:notify:remove` 的管理员可单条、批量删除或清空。操作记录 OperLog；父子日志按固定批次物理删除，并解除通知附件引用。零引用附件重新进入 TEMP 宽限期。
6. 通知日志是全局监控数据。`client_pk` 只展示请求来自哪个 `sys_client.id`，不得自动成为查询条件、所有权或访问边界；后台 Job 可记录 null。

### 边界、失败与稳定错误行为

| 边界或失败 | 稳定行为 |
|---|---|
| uploadPolicy 不存在、配置无效或启动校验失败 | 无效应用配置阻止正常启用对应能力；请求不得静默退回宽松默认策略。 |
| 当前用户缺 requiredPermission 或不在策略允许的 Client 入口 | init 拒绝，不创建 Ticket 或 Provider 上传。Client 检查只是策略准入。 |
| 客户端伪造大小、类型、前缀、objectKey、Provider 或 uploadId | 后端忽略或拒绝；安全字段只能由策略与 Ticket 恢复。 |
| uploadToken 过期、非本人会话、冻结上下文不匹配 | resume/sign/complete/abort 拒绝，不泄露对象 Key 或 Provider uploadId。 |
| Part URL 过期 | 前端重新申请缺失 Part 的短时签名；不重新创建整个对象。 |
| Complete 的 Part、HEAD、size、magic bytes 或策略校验失败 | 不生成 ossId；清理对象/Ticket，返回可识别的上传完成失败。 |
| 未完成 Multipart 超时 | 应用主动 Abort；Provider Lifecycle 作为更晚的兜底。 |
| TEMP 清理与业务绑定并发 | 统一生命周期 Service 保证有效引用不会被清理；冲突结果可重试且无半绑定。 |
| 通用删除仍有引用的 ossId | 明确拒绝并保留对象；引用不是权限证明，但构成生命周期保护。 |
| 普通业务仅提交任意 ossId 请求下载 | 业务 Service 必须完成对象授权；无法判断权限时拒绝，不签发 URL。 |
| NotifyRequest 含 USER 逻辑目标 | common-notify 拒绝；调用方必须先解析为物理目标。 |
| 模板通知缺 contentSnapshot | Provider 调用前拒绝。 |
| 附件缺失、无权、复制失败或部分复制 | 整次通知失败，不调用 Provider；已生成副本幂等删除或由 TEMP 清理。 |
| Provider 对部分目标失败 | 继续尝试其余合法目标，最后抛出含完整结果的 NotifyDeliveryException。 |
| 异步监控落库失败 | 不回滚或改写已完成的 Provider 结果；附件快照保持 TEMP，24 小时后清理。 |
| 日志查询权限被授予 | 可查看 FULL 通知详情；credential-like 通知原文不落库，无法通过查询恢复。 |

错误响应继续使用项目现有统一响应与全局异常映射。实现必须提供可区分的 typed exception 或稳定错误类别来表达：上传策略/会话/完成失败、OSS 引用冲突、通知验证失败、幂等冲突、发送中、幂等基础设施不可用、附件快照失败和 Provider Delivery 失败；本 Spec 不虚构数值错误码。

### 状态转换与不变量

```text
UploadTicket: INITIALIZED -> UPLOADING -> COMPLETING -> COMPLETED
                                 |             |
                                 +-> ABORTED <-+
                                 +-> EXPIRED

OSS object: provider-completed -> TEMP(unreferenced) -> REFERENCED
                                      ^                    |
                                      +---- last unbind ----+
                                      |
                                      +-> expired cleanup -> DELETED

Notify idempotency: ABSENT -> IN_PROGRESS -> COMPLETED
                                     |           |
                                     |           +-> duplicate reuses result
                                     +-> same-key conflict/in-progress rejection
```

- 只有 Complete 校验成功才允许创建一个稳定 `ossId`。
- objectKey、Bucket、Provider uploadId、策略快照和签名 Header 不接受客户端权威值。
- 一个有效 OSS 引用唯一标识为 `(oss_id, ref_type, ref_id)`；同一对象可被多条业务数据引用。
- `ref_type/ref_id` 只用于反向定位和生命周期，不验证业务行存在性，也不授予查看或下载权限。
- 一个 `NotifyRequest` 只选择一个 Channel，但可含多个物理目标。
- `NotifyResult` 必须保留每个目标的结果；异常不能丢失已成功目标。
- Event 发布前必须完成 userId、traceId、可空 `client_pk` 和发送内容快照；异步线程不读取请求 ThreadLocal。
- OAuth `clientId` 字符串与 `sys_client.id` Long 严格区分；审计只记录后者。

## 3. 用户故事

- **US-001**：作为业务用户，我希望文件直接上传到 OSS 并得到 `ossId`，以便上传不占用后端文件带宽和内存。
- **US-002**：作为上传大文件的用户，我希望失败 Part 可重试，并在重新选择同一文件后继续上传，以便网络中断不必从头开始。
- **US-003**：作为业务 Service 开发者，我希望在保存业务记录后用真实表名和主键绑定文件，以便文件获得长期生命周期且可从 OSS 管理面反向定位。
- **US-004**：作为文件访问者，我希望经业务授权后直接从 OSS 下载，以便保持权限边界并避免后端转发字节。
- **US-005**：作为 OSS 管理员，我希望识别 TEMP/被引用对象并安全删除，以便孤儿文件可回收而业务附件不被误删。
- **US-006**：作为通知调用方，我希望通过统一 `NotifyClient` 发送 Mail/SMS，以便业务不依赖 Provider SDK，并能扩展未来渠道。
- **US-007**：作为批量通知调用方，我希望收到逐目标结果和部分失败异常，以便业务正确处理已成功和失败目标。
- **US-008**：作为会重复触发通知的业务，我希望可选使用五分钟 Redis 幂等，以便同一业务动作不重复调用 Provider。
- **US-009**：作为发送带附件邮件的业务，我希望实际发送和历史查询使用独立附件快照，以便源附件变化或删除不改变已发通知。
- **US-010**：作为通知运维人员，我希望查看逻辑通知、完整正文和逐目标 Delivery，以便排查 Provider 调用结果。
- **US-011**：作为授权管理员，我希望删除或清空通知日志并同步处理附件引用，以便执行人工数据维护。
- **US-012**：作为平台安全负责人，我希望 Client 只参与认证授权求值而不成为 OSS/通知租户键，以便统一平台资源不会被错误分区。

## 4. 验收合同

用户故事覆盖映射：US-001 -> AC-001/AC-014；US-002 -> AC-002/AC-003/AC-004；US-003 -> AC-009/AC-010；US-004 -> AC-012/AC-013；US-005 -> AC-007/AC-008/AC-011；US-006 -> AC-015/AC-017；US-007 -> AC-016；US-008 -> AC-018 至 AC-022；US-009 -> AC-023 至 AC-026；US-010 -> AC-025/AC-027/AC-028；US-011 -> AC-029；US-012 -> AC-030/AC-031。

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 用户有命名策略要求的权限，策略允许文件 | 初始化 SINGLE 上传并完成浏览器 PUT | 后端只处理 JSON 控制请求；浏览器网络显示文件字节发往 OSS；Complete 只返回一个字符串 ossId | ruoyi-admin 集成测试 + 浏览器人工网络验收 |
| AC-002 | 策略选择 MULTIPART | 初始化并按窗口申请 Part 签名 | 返回 uploadToken、partSize/partCount/expiry；签名按请求批次生成，不一次返回全部 Part | OSS Service/Client 合同测试 |
| AC-003 | 部分 Part 已成功 | 某 Part 失败或签名过期后重试 | 只重新签名并上传缺失/失败 Part，已完成 Part 不重传 | 浏览器人工网络验收 + Provider 集成 |
| AC-004 | 有效 Ticket 和同一文件 | 刷新后重新选择文件并恢复 | 指纹与 ListParts 一致时从缺失 Part 继续；不同文件被拒绝 | ruoyi-admin UploadTicket 集成 + 浏览器人工验收 |
| AC-005 | 全部 Part 已上传 | 首次与重复调用 Complete | 后端完成 Provider Multipart、HEAD/策略校验，只创建一个 sys_oss，并对重复请求返回同一 ossId | OSS Service + DB 集成 |
| AC-006 | Complete 后最终 size 或 magic bytes 不符 | 请求 Complete | 无 ossId、无可用 sys_oss；对象被删除或进入可观察补偿清理 | OSS 失败路径集成 |
| AC-007 | Ticket 超时或用户取消 | Cleanup 或 Abort 执行 | Provider Multipart 被 Abort；重复执行无副作用；Lifecycle 可兜底遗漏 | Cleanup 集成 + 运维检查 |
| AC-008 | Complete 成功且尚未绑定 | 查询对象并等待 24 小时边界 | 对象显示 TEMP 和过期时间；到期仍无引用时 Provider 对象及元数据被清理 | DB/定时任务集成 |
| AC-009 | 业务已授权使用 ossId | 保存业务记录并登记引用 | sys_oss_ref 存在真实表名与真实主键；对象退出 TEMP；重复绑定不产生重复有效引用 | 业务 Service + DB 集成 |
| AC-010 | 一个 ossId 有两条引用 | 解除其中一条，再解除最后一条 | 第一条解除后对象保持；最后一条解除后重新 TEMP，过期时间为解绑后 24 小时 | 生命周期 Service 集成 |
| AC-011 | ossId 仍有引用 | 管理员调用通用删除 | 请求被拒绝，对象、sys_oss 与引用均保留 | OSS Controller 权限/服务测试 |
| AC-012 | 管理员有 system:oss:download | 请求管理下载 URL | 返回短时 URL/expiry/下载文件名；随后字节直接来自 OSS | ruoyi-admin Controller 集成 + 浏览器人工验收 |
| AC-013 | 普通业务用户只有业务对象权限 | 通过业务接口访问附件 | 业务授权通过才返回短时 URL；仅猜中 ossId 或无数据权限时拒绝 | 业务 API 权限矩阵 |
| AC-014 | 前端运行 FileUpload、ImageUpload、Editor 或下载插件 | 上传、预览、下载 | 不再请求旧 upload/download 字节协议；组件保留进度、取消、回显和错误恢复 | 前端门禁 + 浏览器人工验收 + 调用点扫描 |
| AC-015 | 调用方提供一个 Channel 与合法物理目标 | NotifyClient 同步发送成功 | 使用默认或显式 Provider，返回逐目标 ACCEPTED 结果；Event 监控不改变返回 | Dispatcher 单元 + Adapter 集成 |
| AC-016 | 请求包含多个目标且部分 Provider 调用失败 | NotifyClient 发送 | 所有合法目标均被尝试；最终抛 NotifyDeliveryException，异常中的结果同时含成功和失败项 | Dispatcher 单元测试 |
| AC-017 | 调用方传 USER 目标或模板缺 contentSnapshot | NotifyClient 发送 | Provider 未被调用，并返回可区分验证失败 | Dispatcher 单元测试 |
| AC-018 | 首次使用 idempotencyKey 且 Redis 可用 | 发送完成 | Provider 只调用一次，结果在 Channel+业务 Key 作用域保存默认 5 分钟 | Redis/Dispatcher 集成 |
| AC-019 | 同 Key、同摘要请求仍在执行 | 再次发送 | 立即抛 NotifyInProgressException，不等待、不调用 Provider | 并发集成测试 |
| AC-020 | 同 Key、同摘要请求已完成 | 再次发送 | 复用首次结果，记录 SKIPPED_DUPLICATE 和 originalRequestId，不创建 Delivery | Redis/Event/DB 集成 |
| AC-021 | 同 Key 但摘要不同，或 Redis 不可用 | 带 Key 发送 | 请求 fail-closed，Provider 未调用；错误可区别于 Provider FAILED | Dispatcher/Redis 失败测试 |
| AC-022 | Redis 不可用 | 不带 Key 发送 | 正常调用 Provider，不因可选幂等基础设施失败而阻断 | Dispatcher 单元测试 |
| AC-023 | 通知带多个 attachmentOssIds | 开始发送 | 每个源附件只复制一个本通知快照；全部 Delivery 使用同一组快照；Provider 在复制全部成功后才调用 | 附件快照集成 |
| AC-024 | 附件部分复制失败 | 开始发送 | Provider 零调用；已复制副本幂等删除或保持 TEMP 等待清理 | 附件失败集成 |
| AC-025 | Provider 成功、部分失败或失败 | 异步监听正常 | 一条 sys_notify_log 按审计策略保存内容，每个实际目标 attempt 有一条 Delivery；credential-like 内容不落库；ACCEPTED 不显示为 DELIVERED | Event/DB 集成 |
| AC-026 | 异步日志监听失败 | Provider 已返回结果 | 同步结果不改变；未绑定附件快照保持 TEMP 并可在 24 小时后清理 | Listener 失败注入测试 |
| AC-027 | 运维人员有通知列表权限 | 查询分页列表 | 可按业务、Channel、Provider、状态、时间等筛选；目标只以服务端脱敏值返回；不按 client_pk 自动过滤 | Monitor Controller + 前端页面 |
| AC-028 | 运维人员有通知查询权限 | 打开详情 | FULL 通知返回正文、模板参数、完整目标、Delivery、错误和附件；REDACT_SENSITIVE 只返回脱敏审计；HTML 不在主页面直接执行 | Controller 权限 + UI 安全测试 |
| AC-029 | 管理员有 system:notify:remove | 单删、批删或清空 | 父子日志固定批次物理删除、OperLog 可查、附件引用解除且零引用对象重新 TEMP | Monitor/DB 集成 |
| AC-030 | 两个 Client 对同一用户产生不同权限 | 访问 OSS 或通知监控 | 后端按当前 Client 的权限/数据权限决定访问；不合并权限，也不按 client_pk 形成资源行隔离 | 多 Client 权限矩阵 |
| AC-031 | 后台 Job 无 Token Client | 发送通知 | 可正常发送；日志 client_pk 为 null，不要求 SYSTEM scope | Context Resolver 单元/集成 |
| AC-032 | 启动配置含无效上传策略或不安全 Bucket 配置 | 应用启动或诊断 | 类型化策略校验明确失败；诊断指出 CORS/Lifecycle 问题且不输出 Secret、不自动改 Bucket policy | ApplicationContext + 运维检查 |

## 5. 范围

### IN

- `ruoyi-common-oss` 的 S3 兼容对象 HEAD、结构化预签名请求、Multipart create/sign/list/complete/abort、对象复制和能力探测合同。
- 后端 OSS 上传控制面、Redis UploadTicket、命名上传策略、完成校验、短时下载授权、TEMP 生命周期和清理。
- `sys_oss` 可索引生命周期字段与 `sys_oss_ref` 轻量反向引用。
- FileUpload、ImageUpload、Editor、下载插件及全部已知旧浏览器 OSS 调用迁移。
- `ruoyi-common-notify` 契约层，Mail/SMS Adapter，已知 Captcha、Workflow、Demo 调用迁移。
- 可选 Redis 幂等、附件发送前快照、同步结果与 typed exception。
- `sys_notify_log`、`sys_notify_delivery_log`、异步 Event 监听、后端监控 API、权限、菜单与前端监控页面。
- MySQL NAMEWTA 增量 DDL/DSL、配置示例、运维说明与适用验证。

### REUSE

- 复用 `OssFactory`、现有 `OssClient` S3 SDK/Presigner/自定义域名能力并做向后兼容扩展，不把 AWS SDK model 暴露到 system。
- 复用 RedisUtils/Redisson、Sa-Token 权限、LoginHelper/LoginUser、现有统一响应和异常映射。
- 复用 `ruoyi-api` 的 UserService/OssService 跨模块边界；业务模块不得依赖 system 实现。
- 复用 `common-mail`、`common-sms` 的原子发送能力；只在 Adapter 内直接调用 Builder/SDK。
- 复用 common-log 的 Event 解耦思想、OperLog 删除审计和 monitor CRUD 页面惯例，但不复制 Log AOP 入口。
- 复用现有前端 API 类型、权限指令、分页/详情/删除交互和 request 错误处理；OSS 数据面使用无业务拦截器的独立 transport。

### OUT

- **OOS-001**：不保留 `POST /resource/oss/upload` 和 `GET /resource/oss/download/{ossId}` 的浏览器兼容协议；前后端与已知调用一次迁移。
- **OOS-002**：不支持跨设备无感 Multipart 续传或 Redis Ticket 丢失恢复。
- **OOS-003**：不建设病毒扫描、隔离区或内容审核流水线。
- **OOS-004**：不允许浏览器通用 bind，不依据 `ref_type/ref_id` 动态访问业务表或推导 ACL。
- **OOS-005**：不建设数据库 uploadPolicy 管理后台；策略来自 YAML/环境变量。
- **OOS-006**：不建设多渠道 fan-out、模板中心、用户偏好、Provider failover、自动重试、回执 Webhook、Outbox 或 durable queue。
- **OOS-007**：不把 SysMessage/common-push 迁入 common-notify。
- **OOS-008**：不加密、脱敏存储或自动清理通知日志正文和完整目标；仅列表响应脱敏。
- **OOS-009**：不按 Client/userType 对 sys_oss、sys_oss_ref 或通知日志做租户式数据隔离。

## 6. 已锁定实现约束

- **DEC-001**：Client/userType 是认证授权上下文而非 tenant；`client_pk` 只可作为不可伪造的来源审计。来源：`ADR-001`。
- **DEC-002**：浏览器与 OSS 直接交换文件字节，后端只承担控制面；旧浏览器字节代理协议移除。来源：`ADR-002`。
- **DEC-003**：单 PUT 与 Multipart 使用 Redis UploadTicket；续传和 TEMP 默认窗口 24 小时，Part 签名分钟级，应用清理加 Bucket Lifecycle 双层保护。来源：`ADR-003`。
- **DEC-004**：命名 uploadPolicy 由类型化静态配置提供并在启动时验证；安全限制和对象 Key 由服务端控制。来源：`ADR-004`。
- **DEC-005**：sys_oss 保存可索引 TEMP 生命周期，sys_oss_ref 使用真实物理表名和真实主键表达多业务引用；引用不是 ACL。来源：`ADR-005`。
- **DEC-006**：common-notify 保持薄契约层；一个请求一个 Channel、可多物理目标；调用方负责 USER 解析，Provider 全局默认且可显式覆盖。来源：`ADR-006`。
- **DEC-007**：Provider 同步发送；可选 Redis 幂等默认五分钟，多目标部分失败通过携带完整结果的 typed exception 表达。来源：`ADR-007`。
- **DEC-008**：Spring Event 只承担 best-effort 全局监控；普通通知可 FULL 审计，credential-like 通知必须 REDACT_SENSITIVE；人工删除记录 OperLog。来源：`ADR-008`、`CR-001`、用户接受修复决定。
- **DEC-009**：通知附件在 Provider 调用前复制为独立快照，实际发送使用快照并绑定真实 `sys_notify_log` 主键。来源：`ADR-009`。

## 7. 数据、接口与兼容

### 公共接口变化

OSS 浏览器控制面使用现有 `R<T>` 包装，固定以下操作合同：

| HTTP | 用途 | 关键请求/响应 |
|---|---|---|
| `POST /resource/oss/uploads` | 初始化 SINGLE/MULTIPART | 请求：policy、fileName、fileSize、contentType、fingerprint；响应：uploadToken、mode、expiresAt，以及 SINGLE presignedRequest 或 MULTIPART partSize/partCount |
| `POST /resource/oss/uploads/{uploadToken}/parts/sign` | 批量签 Part | 请求 partNumbers；响应每 Part 的 partNumber、method、url、requiredHeaders、expiresAt |
| `GET /resource/oss/uploads/{uploadToken}/parts` | 恢复会话/ListParts | 校验 fingerprint；返回已完成 partNumber/ETag 与会话元数据 |
| `POST /resource/oss/uploads/{uploadToken}/complete` | 完成并登记对象 | SINGLE 不传 Part；MULTIPART 传有序 partNumber/ETag；成功 data 仅为字符串 ossId |
| `DELETE /resource/oss/uploads/{uploadToken}` | 主动取消 | 幂等 Abort/清理 Ticket，不生成 ossId |
| `GET /resource/oss/{ossId}/download-url` | OSS 管理下载授权 | 需要 system:oss:download；返回 url、expiresAt、fileName |
| `GET /monitor/notify/{notifyLogId}/attachments/{ossId}/download-url` | 通知附件下载授权 | 需要 system:notify:query，并校验附件属于该通知 |
| `GET /system/notice/{noticeId}/attachments/download-urls` | 公告富文本附件解析 | 需要 system:notice:query，只返回公告正文实际引用对象 |

普通业务下载、业务引用新增/解除、通知附件快照通过 `ruoyi-api` 或应用装配的稳定 Service/SPI 提供，不开放前端通用 bind。跨模块公共模型不得暴露 system entity、Mapper 或 AWS SDK 类型。

统一通知公开合同至少包含：

- `NotifyClient.send(NotifyRequest) -> NotifyResult`。
- `NotifyRequest`：request/biz 关联、单一 Channel、可选 providerKey、物理 targets、typed content、可选 attachmentOssIds、FULL/REDACT_SENSITIVE 审计策略、可选 idempotencyKey/窗口和非敏感扩展元数据；不得让业务请求写审计 client_pk。
- `NotifyTarget`：类型、完整物理值及邮件 TO/CC/BCC 等角色；不接受 USER 作为 common 层物理目标。
- `NotifyContent`：文本/富文本或模板内容；模板内容必须含 providerTemplateCode、params、contentSnapshot。
- `NotifyResult`：逻辑 requestId、Provider、总体状态和逐目标结果；重复完成请求返回首次结果。
- `NotifyDeliveryException`、`NotifyInProgressException` 及可区分的验证/幂等基础设施/附件快照异常。
- `NotifyChannelAdapter`、`NotifyContextResolver`、附件资源/快照 SPI 和 `NotifyDeliveryEvent`；common-notify 不依赖 ruoyi-system。

通知监控 API 使用 `/monitor/notify` 前缀：

- 分页列表：`GET /monitor/notify/list`，权限 `system:notify:list`。
- 逻辑通知详情及 Delivery：`GET /monitor/notify/{notifyLogId}`，权限 `system:notify:query`。
- 单条/批量删除：`DELETE /monitor/notify/{notifyLogIds}`，权限 `system:notify:remove`。
- 清空：`DELETE /monitor/notify/clean`，权限 `system:notify:remove`。

### 数据模型与持久化

- `sys_oss` 增加独立、可查询和可索引的 TEMP 标识与过期时间；生命周期状态不得只藏在 `ext1`。现有 fileSize/contentType 等扩展元数据可继续复用 `SysOssExt`，但 `refType/refId/isTemp` 不再作为引用和清理权威。
- 新建 `sys_oss_ref`，主键命名遵循模块表规则；至少保存 oss_id、ref_type、ref_id 及项目要求的 version/create_dept/create_time/create_by/update_time/update_by/del_flag。有效 `(oss_id, ref_type, ref_id)` 唯一。`sys_oss.delete_state` 使用 ACTIVE/PENDING 表达可重试 Provider 删除。
- 新建 `sys_notify_log` 保存逻辑通知、完整内容快照、业务关联、全局状态、附件快照、originalRequestId、userId、traceId 和可空 client_pk。
- 新建 `sys_notify_delivery_log` 保存 notify_log_id、目标类型/角色、完整目标、Provider attempt 状态、耗时、错误和 Provider Message ID。
- 两张通知表都是全局监控表，不附加 Client 数据权限过滤。项目自有新表都包含七个基础字段并使用逻辑删除合同；父子与 OSS 引用删除必须保持一致。
- 普通通知内容可按 FULL 策略保存；credential-like 通知不得保存内容原文，目标必须脱敏。Provider Secret、Authorization、签名 URL、AccessKey/SecretKey、堆栈和无关内部异常不得写入通知表。

### 兼容要求

- 后端内部非浏览器 File/InputStream OSS 能力可以保留供受控服务使用；本 change 删除的是旧浏览器上传/下载协议。
- `OssClient` 现有普通上传、下载和预签名方法保持源兼容；新增结构化预签名与 Multipart 模型由旧方法委托或并存。
- FileUpload、ImageUpload 与 Editor 的 `modelValue` 继续以 ossId/ossId 集合作为持久化合同；显示 URL 运行时解析。
- 现有 Captcha、Workflow、Demo 的业务成功/失败语义保持，但底层入口改为 NotifyClient。
- 未列出的第三方旧 OSS HTTP 调用不提供兼容层；发布前必须通过仓库调用点扫描与接口变更说明暴露破坏性变化。

### 迁移要求

- NAMEWTA 只在 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>` 和 `<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path>` 末尾追加；不修改冻结的 `<Path>ruoyi-vue-plus-namewta/script/sql/ry_vue.sql</Path>`，不新增其他 SQL 文件。
- DDL 追加 sys_oss 生命周期字段、三张项目自有表及索引；DSL 追加通知监控菜单、权限和必要字典数据。所有物理表/字段提供中文注释。
- Fresh install 固定先执行 DDL 再执行 DSL；既有环境只执行本 change 新增块。必须提供已有 sys_oss 行的保守回填，使历史对象不因迁移立即成为到期 TEMP。
- 前后端作为一次破坏性协议迁移发布；Bucket CORS 和 Lifecycle 在启用 Direct Upload 前由运维完成。

### 发布或运维影响

- 部署需要明确前端 Origin 的 OSS CORS，允许实际 PUT/HEAD 所需方法/Header，并向浏览器暴露 ETag；不得允许任意 Origin 携带无关凭据。
- Provider 支持时配置 AbortIncompleteMultipartUpload Lifecycle，期限晚于应用主动清理和重试窗口；应用不自动修改 Bucket policy。
- 配置命名 uploadPolicy、Ticket/TEMP 清理、Part/Download 签名时长、幂等窗口上下限和 Mail/SMS 默认 Provider；Secret 只来自环境或 Secret 管理。
- 数据库容量必须考虑 FULL 正文、目标和附件保留；管理员可通过审计后的删除/清空物理回收通知日志。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 服务端不信任浏览器声明的对象 Key、类型、大小或上传结果；Ticket 绑定初始化身份与冻结策略。OSS transport 不携带业务 Authorization、clientid 或全局 Axios 拦截器 Header，只发送预签名明确要求的 Header。下载必须先通过管理权限或业务对象授权。OTP、重置 Token 等 credential-like 通知必须最小化审计，列表服务端脱敏，HTML 不得直接执行；Provider 凭据、Access Token 和签名 URL 不得入库。
- **NFR-002 性能与容量：** 浏览器文件字节不得经过后端；Multipart 签名按有限窗口批量生成，不能在 init 返回无界 URL 列表。下载不在 JVM 聚合对象字节。清理查询使用可索引 TEMP/expiry/ref 字段。通知多目标尝试与附件复制必须有明确超时和有界资源使用，不得在数据库事务内无界等待 Provider。
- **NFR-003 可用性与可靠性：** Complete、Abort、TEMP 清理、引用新增/解除和人工日志删除必须幂等。Provider 发送同步、无自动重试；Event 监控是 best-effort，允许宕机窗口丢日志。带幂等 Key 时 Redis 故障 fail-closed；无 Key 请求和已完成 OSS 下载不依赖 Redis 幂等。
- **NFR-004 可观测性与运营：** 监控能够按 requestId、bizType/bizId、Channel、Provider、状态、Provider Message ID、traceId、时间和来源 client_pk 排查；幂等基础设施失败与 Provider 失败可区分。UploadTicket/TEMP 清理需要成功、失败、重试和遗留数量的可观察记录，诊断不得输出 Secret。
- **NFR-005 平台边界：** 同一用户在不同 Client 的角色、菜单、权限字符和数据权限不得合并。Client/userType 不得自动生成 OSS/通知行级过滤、Provider 选择、所有权或幂等分区。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| UploadPolicy、Ticket、生命周期、Dispatcher、幂等状态机 | Java 单元测试 | AC-002, AC-004, AC-005, AC-008, AC-010, AC-016-AC-024, AC-031 | 新增定向测试；`./mvnw -Dmaven.test.skip=false test` | test output |
| OssClient S3 兼容能力与 Redis/DB/Event 协作 | 后端集成测试 | AC-001-AC-013, AC-018-AC-026, AC-032 | 本地 S3-compatible/测试替身、Redis、MySQL；受影响 Maven reactor 测试 | integration output |
| OSS/Monitor Controller 权限与错误映射 | Spring MVC/安全集成 | AC-011-AC-013, AC-027-AC-030 | Sa-Token 权限矩阵和统一 R/异常映射 | test output |
| NAMEWTA DDL/DSL | MySQL migration | AC-008-AC-011, AC-025, AC-029 | Fresh DDL->DSL、既有库仅追加块、schema/index/注释查询 | query transcript |
| FileUpload/ImageUpload/Editor/下载插件 | Vue/浏览器行为 | AC-001, AC-003, AC-004, AC-012-AC-014 | 组件人工矩阵；`pnpm lint`; `pnpm exec vue-tsc --noEmit`; `pnpm build:prod` | command + screenshot/network evidence |
| 通知监控页面 | Vue/HTTP 集成 | AC-027-AC-029 | 权限账号、分页/详情/删除/清空、HTML 安全内容 | screenshot + API evidence |
| Direct OSS 真实链路 | 浏览器人工验收（不建设 E2E 测试套件） | AC-001, AC-003-AC-007, AC-012-AC-014 | 桌面浏览器网络面板：后端仅控制请求，OSS 承载字节；断网/刷新/取消/CORS/过期签名矩阵 | manual browser evidence |
| 多 Client 授权矩阵 | 跨端权限验证 | AC-013, AC-027-AC-032 | 两个 Client、同一用户不同权限、后台 Job 无 Token Client | API/UI matrix |
| 后端回归构建 | Maven reactor | 全部后端合同 | `<Path>ruoyi-vue-plus-namewta/mvnw</Path> clean package`；注意默认跳过测试，不能替代 opt-in test | build output |
| 旧调用点收口扫描 | 静态仓库检查 | AC-014, AC-015 | 扫描旧 OSS URL 与 Adapter 外 MailBuilder/SmsFactory/SMS SDK 引用 | scan output |

## 10. 风险、假设与未决问题

### 风险

- FULL 通知仍可能包含敏感业务正文与完整目标；必须限制查询权限并审计删除。OTP、Token 等 credential-like 请求使用 REDACT_SENSITIVE 降低数据库泄露与权限误授影响。
- Spring Event 监控允许应用宕机窗口丢失通知日志；它不能作为发送可靠性证据。
- 不同 S3-compatible Provider 对 Multipart、checksum、ETag、自定义域名签名和 CopyObject 的兼容细节可能不同；实现需通过 capability 和目标 Provider 验证，不得把 Multipart ETag 当整文件 MD5。
- `sys_oss_ref` 是无数据库外键的多态引用；业务表改名或数据绕过 Service 删除可能留下错误反向定位，需要发布治理和定向核对。
- 旧 OSS HTTP 协议直接删除会中断仓库外调用方；本 change 接受破坏性升级，但发布说明必须明确。
- 异步通知日志落库失败时，预生成 notifyLogId 可能形成编号空洞；附件 TEMP 清理优先于编号连续性。

### 已采用的低影响假设

- REST 路径、响应字段与权限键采用本 Spec 第 7 节合同；局部 Java 包名、record/class 拆分和 Redis key 格式由 Tickets 按现有模块惯例确定。
- 文件续传指纹采用文件名、大小、lastModified 与首尾分块摘要的组合，不预扫描整个大文件；验证以“同文件可恢复、不同文件拒绝”为合同，不以某个哈希拼接格式为公共 API。
- 目标 Provider 只保证 S3 兼容基线；标准 checksum 是能力增强而非启用门槛。
- 通知监控首版不提供 Excel 导出，因为用户只确认列表、详情、删除与清空。

### 未决问题

无。
