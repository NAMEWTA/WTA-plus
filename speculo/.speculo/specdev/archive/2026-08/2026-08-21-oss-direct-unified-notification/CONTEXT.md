# OSS 直传与统一对外通知

## 平台与 Client

**Platform Client**：同一大平台中某个 APP 前端的登录与授权上下文，用于确定登录域、角色、菜单、权限字符、数据权限和动态路由。所有 APP 共用后端底座、业务逻辑、用户与数据资源；Client 不是 tenant，不自动形成数据所有权或业务表行级分区。
_Avoid_: 租户、数据分区、按 client_pk 自动隔离全部业务数据

**Client Authorization**：用户访问资源或接口时，后端使用其在当前 Client 下获得的权限字符与数据权限做授权；Client 只参与权限求值，不替代具体业务授权，也不得跨 Client 合并角色、菜单或权限。
_Avoid_: 用 Client 数据过滤代替权限检查、跨 Client 权限并集

**Client Identifier**：登录/注册 body 与 `clientid` header 中的 `clientId` 是 OAuth 字符串；Token extra、角色菜单与 RBAC 上下文中的 `clientPk/clientId` 是 `sys_client.id` Long 主键。两者不可互换。
_Avoid_: 隐式转换 OAuth clientId 与 sys_client 主键

**UserType Login Domain**：由 Client 动态关联并写入 Token extra 的登录域，用于登录准入和会话语义；它不是 `sys_user` 单值字段、固定枚举、tenant 或数据分区键。
_Avoid_: 恢复 sys_user.user_type、把 userType 用作业务数据隔离

## OSS

**OSS Control Plane**：后端负责认证授权、对象 Key、UploadTicket、签名、完成校验、元数据和业务访问控制，但不代理浏览器文件字节。
_Avoid_: 后端中转浏览器上传或下载

**OSS Data Plane**：浏览器使用短时预签名请求直接向 OSS 上传 Part 或下载对象；业务数据库只持久化 `ossId`，临时 URL 不是稳定业务数据。
_Avoid_: 永久下载 URL、业务表保存对象 URL

**UploadTicket**：保存在 Redis 的上传会话，默认续传窗口 24 小时，绑定对象 Key、Provider uploadId、文件指纹、上传策略和调用审计上下文；完成并校验后才生成 `ossId`。
_Avoid_: sys_oss PENDING 上传记录

**Upload Policy**：从 YAML/环境变量加载并在启动时校验的命名服务端策略，规定类型、大小、对象前缀、上传模式、Multipart 阈值、requiredPermission 和可选 Client 准入范围。Client 范围只控制某 APP 是否可调用策略，不形成对象数据隔离。
_Avoid_: 客户端自报限制、用 bizType 或 Client 作为对象所有权

**Temporary OSS Object**：已完成 Provider 上传并获得 `ossId`、但当前没有 `sys_oss_ref` 引用的对象。`sys_oss` 保存可索引 TEMP 状态与过期时间，默认从 Complete 或最后解绑起保留 24 小时。
_Avoid_: 在 ext1 隐藏状态、把 Complete 直接视为永久资产

**OSS Business Reference**：`sys_oss_ref` 中以唯一 `oss_id + ref_type + ref_id` 表达的轻量反向定位关系；`ref_type` 是实际物理表名，`ref_id` 是该表真实主键。同一 ossId 可以有多条引用。
_Avoid_: 把 ref_type/ref_id 塞进 sys_oss、抽象业务名、动态外键、反射查表、通用业务回调

**Global OSS Metadata**：`sys_oss` 与 `sys_oss_ref` 是全局资源元数据，不记录 Client 数据归属，也不按 Client/userType 自动过滤。引用关系不是用户 ACL；普通业务仍须完成对象级权限和数据权限校验。
_Avoid_: 把全局元数据理解为公开访问

**Business OSS Owner**：持久化 ossId 的业务写入 module；它在同一 `@DSTransactional` 中 fail-closed 地拥有业务记录保存、旧新附件差异和 OSS 引用转换，并按真实恢复合同决定逻辑删除时解绑或保留。每个 owner 必须登记显式清单并通过 insert/update/delete/restore（适用时）合同测试；该清单是交付门禁而非运行时注册中心。跨业务共享能力只承载集合差异与引用转换的机械规则，不读取、回调或反射业务表。
_Avoid_: 中央业务协调器、动态业务回调、由 common 推断业务权限

## Notify

**Notify Channel**：`mail`、`sms` 及未来 `wechat`、`feishu` 等可插拔物理通讯渠道；每个 `NotifyRequest` 只选择一个 Channel。
_Avoid_: 将多渠道业务编排塞入 common-notify

**Notify Provider**：某个 Channel 的具体供应商或基础设施配置；默认使用全局 Provider，也可用 `providerKey` 显式覆盖。Client 不参与 Provider 路由。
_Avoid_: 用 Client 或审计 client_pk 代替 providerKey、common 自动故障切换

**Physical Target**：调用方通过公开用户 API 或自身领域服务解析完成的 `PHONE`、`EMAIL`、`OPEN_ID` 等实际地址；一个请求可以包含多个目标和邮件 TO/CC/BCC。
_Avoid_: common-notify 解析 SysUser、common 读取 system Mapper

**Notify Delivery Exception**：Dispatcher 尝试全部合法目标后，只要至少一个目标失败就抛出的 typed exception；异常携带完整 `NotifyResult`，由业务边界决定传播或容忍。
_Avoid_: 首目标失败即停止、丢弃部分成功结果

**Notification Idempotency Key**：调用方可选提供的业务去重键；Redis 作用域为 Channel 与业务 Key，默认窗口 5 分钟且可在受控范围覆盖，审计 `client_pk` 不进入 Key。发送中命中抛出 `NotifyInProgressException`，完成后命中复用首次结果并记录 `SKIPPED_DUPLICATE`。
_Avoid_: SYSTEM/Client 幂等作用域、将幂等描述为可靠队列或 exactly-once

**Notify Context Resolver**：由 common-notify 定义、应用装配实现的审计上下文 SPI；在同步线程解析 userId、traceId 和可为空的来源 `client_pk`，并在发布 Event 前形成快照。
_Avoid_: 异步 Listener 重新读取 ThreadLocal、用 client_pk 形成通知归属

**Notification Log**：`sys_notify_log` 中的一次逻辑通知及完整正文快照；内容明文永久保存，不加密、不自动清理。通知日志是全局监控数据，只受功能权限控制，不按审计 `client_pk` 隔离。
_Avoid_: SysMessage、站内信、SYSTEM scope、Client 数据分区

**Delivery Log**：`sys_notify_delivery_log` 中某个物理目标的 Provider attempt、状态、耗时、错误和第三方消息标识。
_Avoid_: 将 ACCEPTED 称为 DELIVERED、为幂等跳过创建虚假 Delivery

**Notification Client Audit**：通知日志以 `client_pk` 记录请求 Token 中的 `sys_client.id` Long 主键；无 Client 上下文的 Job 记录 null。该字段只用于审计展示，不参与 Provider、权限、所有权、查询隔离或幂等，不保存 OAuth clientId 字符串。
_Avoid_: SYSTEM scope、把来源 client_pk 当作访问边界、混用 OAuth clientId

**Notification Monitor**：由数据库、后端 API 和前端页面组成的全局通知监控面；列表目标脱敏，详情按同一查询权限展示完整目标与正文，并支持权限化人工删除。
_Avoid_: 浏览器收到明文后仅用 CSS 脱敏、直接执行通知 HTML

**Notification Attachment Snapshot**：Dispatcher 在 Provider 调用前复制并作为实际发送内容使用的通知专用 OSS 对象；使用 `ref_type=sys_notify_log` 与真实 `notify_log_id` 建立引用，同一逻辑通知的 Delivery 共享一组快照。
_Avoid_: 每个收件人重复复制、发送后异步复制、依赖源文件长期存在
