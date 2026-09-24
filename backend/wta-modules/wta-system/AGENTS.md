# wta-system 调用规范

本文件适用于 `wta-modules/wta-system/**`。上级 `AGENTS.md` 与父工作区工程 Skill 仍然生效；发生冲突时，以更严格的安全、权限和模块边界要求为准。

## 模块边界

- `wta-system` 拥有用户、组织、角色、菜单、权限、Client、系统资源、OSS 和消息等实现。Controller、`ISys*`、Mapper、entity、BO、VO 及实现类均为模块内部合同。
- 其他业务模块只依赖 `wta-api` 和所需的 `wta-common-*`，通过构造器注入 `org.namewta.system.api.*`。不得依赖 `wta-system`，不得注入 `ISys*`、Mapper，或导入本模块的 domain/BO/VO/Controller。
- `wta-admin` 只负责应用组装。其穿透调用是部署入口的例外，不得复制为普通业务模块的实现范式。
- 同一 JVM 内调用公开 Service；不得为调用本模块而新增 Feign、Dubbo，或从后端内部绕行 `/system`、`/resource` HTTP 接口。
- 跨模块新能力先判断 owner；确需公开时，将最小稳定合同放入 `wta-api`，使用 DTO/record 表达，不泄露持久化对象、Mapper 或可变内部集合。

## 通用接口与权限

- Controller 负责参数校验、`@SaCheckPermission`、传输映射与操作日志；Service 负责业务规则、事务、缓存和关系维护。前端可见性不能替代服务端授权。
- 只读查询使用 GET；业务变更使用 POST，并添加语义准确且不记录密钥、Token、签名 URL 等敏感信息的 `@Log`。存量接口按触及范围迁移，不得作为新接口样例。
- 调用公开 Service 前，业务 owner 必须先完成本业务对象的读取/下载/修改权限和数据范围校验。不得提供仅凭外部传入 ID 即可匿名读取系统资源的通用入口。
- 新建或实质修改的业务事务使用 `@DSTransactional`；缓存、关系和事务事件必须与当前数据源事务保持一致。

## OSS 调用

### 依赖与访问地址

- 其他模块注入 `org.namewta.system.api.OssService`；不得注入 `ISysOssService`、`SysOssMapper`、`OssLifecycleManager` 或直接调用 `OssFactory` 读取业务附件。
- 业务表只保存 `ossId`，不得保存公共 URL、预签名 URL 或其到期时间作为资源身份。`sys_oss.service` 是对象所属存储配置的权威路由，读取时不得改用当前默认配置。
- 新代码在业务授权后优先调用 `OssService.resolveAccessUrl(ossId)`：`accessType=PUBLIC` 时 URL 可直接访问且 `expiresAt=null`；`accessType=PRIVATE` 时 URL 为短时签名且必须返回 `expiresAt`。调用方不得缓存私有 URL，不得假设 URL 永久有效。
- 只有明确需要私有下载语义时才调用 `presignDownload`。公共对象调用该方法会失败；命名策略只能由服务端代码选择，客户端不得提交任意 TTL 或策略名。
- `selectUrlByIds`、`selectByIds` 仅保留给翻译器和存量调用方兼容。它们会隐藏 PUBLIC/PRIVATE 与到期时间语义，新接口、新业务代码和新 HTTP 合同不得优先使用。

### 上传与引用生命周期

- 浏览器上传复用 `/resource/oss/uploads` 协议或现有前端上传组件。客户端只提交服务端允许的上传策略名和文件元数据；不得提交或覆盖 `configKey`、Bucket、`accessPolicy`、签名 TTL、AccessKey/SecretKey。
- 上传完成后以返回的 `ossId` 关联业务数据。创建、修改、删除业务记录时，在同一个 `@DSTransactional` 业务事务内调用 `reconcileReferences(refType, refId, previousOssIds, currentOssIds)`。
- `refType` 必须是真实物理表名，`refId` 必须是真实业务主键；不得使用 Java 类名、显示名称、临时 UUID 或伪造 owner。删除业务数据必须同步解除引用。
- 不得直接删除仍被业务引用的对象，不得绕过临时对象、引用计数和两阶段删除状态。清理任务默认先 dry-run；迁移、重试、回滚和 cleanup 必须经过显式权限与人工确认，不得在启动时自动执行生产变更。

### 配置与运行状态

- 配置管理成功写入必须且只能保留一个默认 OSS 配置，且默认配置必须为 `PRIVATE`；公共桶使用非默认 `PUBLIC_READ` 配置。启动读取历史库时允许 0 个、多个或损坏默认行，但不设置默认指针，新文件操作明确失败，不能因可选 OSS 问题阻断核心。`status` 只表示是否为默认配置，不表示启停状态。
- 已被对象或未终结迁移工单引用的配置不得通过普通编辑修改 `configKey`、Bucket、`accessPolicy`、`endpoint`、`isHttps` 或 `region`；同一物理存储身份下可轮换凭据。配置写事务按配置主键顺序先锁所有现有配置，建迁移工单也先锁配置再锁对象和工单；不得在这些短事务内执行供应商网络调用。
- 来源清理先持久化 `FAILED/COMPLETED/CLEANUP_OUTCOME_UNKNOWN` 执行权，再在事务外发起一次有界 DELETE。超时或提交回执不确定时，恢复、回滚、重试和新工单均不得绕过该栅栏；后续对象 HEAD 返回 404 还须在同一剩余预算内确认 Bucket 可达，才能证明来源对象不存在，目标仍存在时方可完成原工单，绝不重发 DELETE。Bucket 无法确认时维持未知。
- OSS 诊断是管理员显式调用的观察事实，不参与业务访问与核心健康门禁，也不负责创建 Bucket 或修改云端 Policy。签发访问 URL 前仍须校验对象 ACTIVE、对象自己的 service、请求者权限与当前配置访问类型；远端调用按实际结果反馈。启动从 DB 重建可用配置缓存，只在唯一合法 PRIVATE 默认存在时设置默认指针；坏的可选配置不阻断核心。
- 下载有效期由 `oss.lifecycle.download-ttl` 及服务端命名策略控制，并受 `download-ttl-min`/`download-ttl-max` 约束；不得从请求参数直接决定有效期。

## 修改与验证

- 修改公开 `wta-api` 合同时，同步 Javadoc、调用方、契约测试和父工作区 `.agents/skills/wta-module-guide/references/modules/system/`。
- OSS 变更至少运行受影响的 OSS contract/architecture 单元测试和 Maven 编译；涉及 Provider、Redis、MySQL 或 MinIO 的行为时，再运行相应属性门控集成测试并如实记录外部服务条件。
- 不提交密钥、本地 endpoint、签名 URL、`application-local.yml`、`target/**` 或 `.flattened-pom.xml`。
