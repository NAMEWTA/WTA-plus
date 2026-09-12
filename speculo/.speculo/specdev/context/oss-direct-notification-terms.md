# OSS 直传与统一通知术语

- **Status:** Current
- **Date:** 2026-08-23
- **Source:** `2026-08-21-oss-direct-unified-notification` CONTEXT

## 平台与 Client

**Platform Client**：同一大平台中某个 APP 前端的登录与授权上下文，用于确定登录域、角色、菜单、权限字符、数据权限和动态路由；不是 tenant，不自动形成数据所有权或业务表行级分区。
_Avoid_: 租户、数据分区、按 client_pk 自动隔离全部业务数据

**Client Authorization**：后端使用用户在当前 Client 下获得的权限字符与数据权限做授权；Client 参与权限求值，但不替代具体业务授权，也不得跨 Client 合并权限。
_Avoid_: 用 Client 数据过滤代替权限检查、跨 Client 权限并集

**Client Identifier**：登录/注册 body 与 `clientid` header 中的 `clientId` 是 OAuth 字符串；Token/RBAC 上下文中的 `clientPk/clientId` 是 `sys_client.id` Long 主键。
_Avoid_: 隐式转换 OAuth clientId 与 sys_client 主键

**UserType Login Domain**：由 Client 动态关联并写入 Token extra 的登录域，用于登录准入和会话语义；不是固定枚举、tenant 或数据分区键。
_Avoid_: 恢复 sys_user.user_type、把 userType 用作业务数据隔离

## OSS

**OSS Control Plane**：后端负责认证授权、对象 Key、UploadTicket、签名、完成校验、元数据和业务访问控制，但不代理浏览器文件字节。
_Avoid_: 后端中转浏览器上传或下载

**OSS Data Plane**：浏览器使用短时预签名请求直接向 OSS 上传 Part 或下载对象；业务数据库只持久化 `ossId`。
_Avoid_: 永久下载 URL、业务表保存对象 URL

**Local MinIO CORS Default**：`release-artifacts` 中 `MINIO_API_CORS_ALLOW_ORIGIN` 的本地/联调默认值是 `*`，用来避免前端 Origin 变化导致预检失败；共享或生产必须覆盖为稳定 HTTPS 域名白名单。
_Avoid_: 把 `*` 拷进生产 env、用固定前端 IP 作为本地默认白名单

**UploadTicket**：保存在 Redis 的上传会话，默认续传窗口 24 小时，绑定对象 Key、Provider uploadId、文件指纹、上传策略和审计上下文；完成校验后才生成 `ossId`。
_Avoid_: sys_oss PENDING 上传记录

**Upload Policy**：启动时校验的命名服务端策略，规定类型、大小、对象前缀、上传模式、Multipart 阈值、权限和可选 Client 准入范围；Client 只控制入口资格。
_Avoid_: 客户端自报限制、用 bizType 或 Client 作为对象所有权

**Temporary OSS Object**：已完成上传并获得 `ossId`、但当前没有 `sys_oss_ref` 引用的对象；默认从 Complete 或最后解绑起保留 24 小时。
_Avoid_: 在 ext1 隐藏状态、把 Complete 直接视为永久资产

**OSS Business Reference**：`sys_oss_ref` 中以唯一 `oss_id + ref_type + ref_id` 表达的轻量反向定位关系；`ref_type` 是真实物理表名，`ref_id` 是真实主键。
_Avoid_: 抽象业务名、动态外键、反射查表、通用业务回调

**Global OSS Metadata**：`sys_oss` 与 `sys_oss_ref` 是全局资源元数据，不记录 Client 归属，也不按 Client/userType 自动过滤；引用不是 ACL。
_Avoid_: 把全局元数据理解为公开访问

**Business OSS Owner**：持久化 ossId 的业务写入 module；它在同一 `@DSTransactional` 中拥有业务保存、附件差异和引用转换，并按真实恢复合同处理删除。Owner 清单与合同测试是交付门禁，不是运行时注册中心。
_Avoid_: 中央业务协调器、动态业务回调、由 common 推断业务权限

## Notify

**Notify Channel**：mail、sms 及未来 wechat、feishu 等可插拔物理通讯渠道；每个 NotifyRequest 只选择一个 Channel。
_Avoid_: 将多渠道业务编排塞入 common-notify

**Notify Provider**：Channel 的具体供应商或基础设施配置；默认使用全局 Provider，也可由 `providerKey` 覆盖。
_Avoid_: 用 Client/client_pk 代替 providerKey、common 自动故障切换

**Physical Target**：调用方通过公开用户 API 或领域服务解析出的 PHONE、EMAIL、OPEN_ID 等实际地址；一个请求可含多个目标。
_Avoid_: common-notify 解析 SysUser、common 读取 system Mapper

**Notify Delivery Exception**：Dispatcher 尝试全部合法目标后，只要至少一个失败就抛出的 typed exception；携带完整 NotifyResult。
_Avoid_: 首目标失败即停止、丢弃部分成功结果

**Notification Idempotency Key**：调用方可选的业务去重键；Redis 作用域为 Channel 与业务 Key，默认窗口 5 分钟，Client 不进入 Key。
_Avoid_: SYSTEM/Client 幂等作用域、将幂等描述为 exactly-once

**Notify Context Resolver**：common-notify 定义、应用实现的审计上下文 SPI；在同步线程解析 userId、traceId 和可空 `client_pk`，发布 Event 前形成快照。
_Avoid_: 异步 Listener 重新读取 ThreadLocal、用 client_pk 形成通知归属

**Notification Log**：`sys_notify_log` 中的一次逻辑通知及审计快照；属于全局监控数据，只受功能权限控制，不按 `client_pk` 隔离。
_Avoid_: SysMessage、站内信、SYSTEM scope、Client 数据分区

**Delivery Log**：`sys_notify_delivery_log` 中某个物理目标的 Provider attempt、状态、耗时、错误和第三方消息标识。
_Avoid_: 将 ACCEPTED 称为 DELIVERED、为幂等跳过创建虚假 Delivery

**Notification Client Audit**：通知日志以 `client_pk` 记录请求 Token 的 `sys_client.id` Long 主键；无 Client 的 Job 记录 null，只用于来源审计。
_Avoid_: SYSTEM scope、把 client_pk 当作访问边界、混用 OAuth clientId

**Notification Monitor**：由数据库、后端 API 和前端页面组成的全局通知监控面；列表目标脱敏，详情按查询权限展示，HTML 不执行。
_Avoid_: 浏览器收到明文后仅用 CSS 脱敏、直接执行通知 HTML

**Notification Attachment Snapshot**：Dispatcher 在 Provider 调用前复制并用于实际发送的通知专用 OSS 对象；以 `sys_notify_log` 和真实 notify log 主键建立引用。
_Avoid_: 每个收件人重复复制、发送后异步复制、依赖源文件长期存在
