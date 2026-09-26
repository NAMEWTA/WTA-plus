# T-44 激活前只读安全/实施核查

固定产品输入 `28f707a208405ac09764582c9e8dc12316535b87`；`5118051403de0648540646d98536d8c4f3f9f26d..28f707a2` 的本票生产路径无差异。依据 T-44/AC-044、已接受 ADR-CR-017、R64-O-01 和 `/tmp/wta-t44-current-audit.md`，以固定 commit 的 `git show` 核实，未读取变化中的 T-40 产品工作树。本轮只读；无构建、测试、服务、数据库或仓库修改。本报告是派单输入，不是验收结果。

## 当前确定缺口与最小生产规则

| 接缝 | 固定源码事实 | T-44 最小改动/保留的安全条件 |
|---|---|---|
| 下载授权 | `OssLifecycleManager.java:133-161,279-316` 在实际对象/配置检查前要求 `readinessRegistry.requireServing(oss.service)`；快照缺失/过期会挡住合法文件。 | 移除此门禁及无用依赖；对象必须存在且 `deleteState=ACTIVE`，`service` 非空，按该对象的 service 经 `OssFactory.instance(service)` 取当前配置及有效 PRIVATE/PUBLIC_READ 类型，缺配置/未知类型失败关闭。PUBLIC_READ 只给公共 URL，不私签；PRIVATE 只给带受控 TTL、非空 URL/真实到期的签名。保留管理 `SysOssController.java:69-75` 的 `system:oss:download` 与业务调用者自身 owner/Client/关系授权；不可把仅有 ossId 当权限。远端 GET 失败按实际错误回报，预签名本身不冒充对象已可读。 |
| 直传 | `OssUploadService.java:51-77,96-113` 已先验证当前用户/Client、策略、尺寸与类型，却以诊断快照决定存储可用和 policy。`DefaultOssUploadObjectStore.java:25-47` 已对实际客户端配置校验 expectedAccessPolicy、Bucket 和 MULTIPART 能力。 | 删除服务层快照门禁，沿现有 store 的本地当前配置比较失败关闭；不能删身份/Policy/大小/Content-Type 校验。默认键不存在/无有效配置仍拒绝，Ticket 在服务端固定原 storage service，后续 sign/complete 不随默认切换重路由。真实浏览器 PUT/完成 HEAD 失败如实回报，不因能产生本地签名就称上传成功。 |
| 迁移 dry-run | `OssStorageMigrationService.java:75-117,327-335` 的源/目标 policy 仍来自 readiness；`DefaultOssMigrationObjectStore` 已能按真实配置和对象做检查。 | 移除快照判断，保留当前对象 ACTIVE、源 PRIVATE/目标 PUBLIC_READ、来源≠目标、HEAD/内容冲突和 copy 前失败关闭。T-46 的迁移/清理共同锁与删除确认不在本票重做。 |
| DB 元数据 | `SysOssServiceImpl.java:77-146,327-350` 的列表/元数据用 mapper、配置表和迁移表，没有对象 store 调用。 | 用 spy/真实 DB 证明在诊断 DOWN、MinIO 离线时仍零 OSS 调用；不要为了本票改公开 `OssService` 或该存量 service。 |

**启动需连同缓存一起解耦。** `SystemApplicationRunner.java:27-31` 先 `ossConfigService.init()` 再同步 `readinessService.refresh()`；后者 `OssStorageReadinessService.java:39-110` 全配置扫描并做远端 canary/policy/ACL/匿名读。直接删 `refresh()` 仍不够：`SysOssConfigServiceImpl.java:53-69` 对空配置/默认数量异常及任一坏非默认访问类型抛错，核心启动失败。最小拆法是启动只从 DB 做有界结构读取，缓存有效配置，不在启动调用远端；空库可启动但文件业务明确失败。坏非默认配置隔离并报告，不污染有效配置；默认键只有**唯一且有效的 PRIVATE 默认配置**才写入 Redis。若零/多/无效默认，必须清除旧 `DEFAULT_CONFIG_KEY`（`OssFactory.instance()` 和直传默认键均读它），避免重启后使用旧 Redis 指针；配置新增/切换的“恰好一个 PRIVATE 默认”写入校验仍严格保留。旧配置缓存和客户端不能被坏行复活，需对相应键失效。此项需要 `SysOssConfigServiceImpl.java`，不在当前 T-44 写集。

`OssUploadDiagnostics.java:33-60` 的 `ApplicationReadyEvent` 又取默认键并远程检查 CORS/Lifecycle；默认键读取在 try 外，且检查会拖慢启动。T-44 应移出启动事件，保留仅管理员可触发的只读诊断/要求说明。`OssStorageReadinessService.java:39-40` 当前每分钟调度全配置、`OssConfigChangeListener.java:29-52` 在管理配置变更的提交后同步全量 refresh。R64-O-01 的接受规则是诊断默认由管理操作触发，不以第二个自愈调度器维护 5 分钟快照；因此不能保留默认全量调度作为业务前置或核心就绪条件。最窄做法是取消自动全量刷新，保留诊断 registry/只读 provider；配置变更只失效受影响快照/缓存而不阻塞管理请求。如果本票需让管理员主动选择配置/对象即时诊断，当前没有相应 Controller 入口，须先登记 `SysOssConfigController` 与相关接口路径，限制权限、单配置与有界 I/O；不要把现有全量 `refresh()` 包在无鉴权 GET 后伪称独立诊断。诊断事实三态准确性留给 T-45。

**全局调度开启不可随 readiness 调度一起删。** 固定 `d4000a859ead9394b189364952321a094f16030e` 核查表明 `OssStorageReadinessSchedulingConfiguration.java:9-11` 是无条件 `@EnableScheduling`；另一个 `SnailJobConfig.java:20-24` 只在 `snail-job.enabled=true` 才开启，而常见隔离/日常启动会关闭 Snail Job。`NotifyOutboxWorker.java:29-35` 的兜底 `@Scheduled(fixedDelayString="${notify.outbox.poll-delay-ms:60000}")` 依赖应用全局调度开启；Redis wake 发布/订阅失败时只能靠它恢复。T-44 去掉 OSS 全量巡检时不得误删或条件化唯一全局调度启用，应在非 OSS、始终装配的配置处保留 `@EnableScheduling`，并以实际 full-app 中 `snail-job.enabled=false`、Redis wake 故障/丢失、短正数 poll delay 的真实 Outbox→Worker 送达来验收。`message.enabled=false` 仅关实时页面功能，不是 Worker 开关；不能以一个 Java 注解存在或单测反射代替实际 fallback 送达证据。

**核心探针与可选探针必须在实际 HTTP 中分离。** 当前 `OssStorageReadinessHealthIndicator.java:14-33` 对必需配置不可用返回 DOWN；`application.yml:366-375` 没有 core/OSS 分组。保留单独 OSS 诊断健康结果，但核心 readiness/liveness 不得包含它；空/坏可选存储和诊断快照过期时核心仍能登录/菜单/站内信。`OssStorageReadinessProperties.java:29-49` 对畸形可选诊断参数直接抛异常，也会在核心启动前失败；若仍可由用户配置，应把它归为诊断不可用并安全限制 I/O 参数，而不是放宽无界超时或让可选诊断阻断核心。改变 health 组后核对部署探针实际路径；不能只测 Java indicator。

## 事前写集核对

当前 Ticket 已覆盖 `oss/service/**`、`oss/upload/**`、`oss/migration/**`、`oss/readiness/**`、`runner/SystemApplicationRunner.java`、`wta-admin/application.yml`、system `AGENTS.md`、admin OSS 测试根、`backend/README.md`、`release-artifacts/docker/**` 和 System Skill refs。**必须先扩** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java`：启动空/坏配置及默认键陈旧处理不能只靠已有目录。若按已接受“管理触发、取消默认全量巡检”完整实施，还须扩 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/OssConfigChangeListener.java`；需要显式单配置诊断 HTTP 时再精确扩 `controller/system/SysOssConfigController.java` 与 `service/ISysOssConfigService.java`（若选择经该接口），并核权限/GET 只读及 OpenAPI 生成输入。不要预改 `wta-api`、`wta-common-oss`、六 SQL、T-45 的策略解释器或 T-46 共锁。`backend/wta-modules/wta-system/AGENTS.md:37-42` 与 `.agents/skills/wta-module-guide/references/modules/system/domains.md:105-107` 仍要求 readiness 达标才签发 URL，须在本票明确替换为对象/归属/当前配置/预期类型检查，避免硬规则与产品相反。

## 能产生真实红灯的最小验收

1. **先红。** 在现有 `OssLifecycleManagerUnitTest`、`OssUploadServiceUnitTest` 中用有效对象/配置/本人身份和空或过期诊断 registry，要求 URL/直传 init 成功；旧实现会被 `STORAGE_NOT_SERVING` 挡下。相同夹具再给他人、未知/非 ACTIVE 对象、错 policy、缺默认或失效配置，要求在 Provider/Ticket 前拒绝。`OssStorageMigrationServiceUnitTest` 用可检查的真实源/目标配置与诊断 DOWN，要求仅合法 dry-run 可进入对象检查；保留冲突/类型负例。`SysOssServiceImpl` 列表/`objectMetadata` 在空 registry 下断言对象 store 调用零次。
2. **真实核心启动。** 用本票拥有的随机 MySQL 8.4 库按六 SQL 建基座、Redis 和 loopback MinIO，分别注入空 `sys_oss_config`、有效默认+坏非默认、缺 canary、过期快照、MinIO 离线。保存真实 `/actuator/health` 与 core/OSS group HTTP status/body（去敏），证明核心就绪、登录/菜单/站内信不访问 OSS，OSS 诊断可为 DOWN/UNKNOWN；启动/ready 期间远端诊断调用计数为 0。有效默认能按需建客户端；无默认不能复用 Redis 旧键，文件请求明确失败。真实配置管理仍拒绝第二个默认和 PUBLIC_READ 默认。
3. **最小权限 MinIO。** 用独立 bootstrap 身份建随机私有/公共桶和测试对象，应用身份只给需要的对象读写/签名能力，不给 bucket policy/ACL 管理权限。registry 无 canary/过期时，PRIVATE 返回有到期的短时 URL 且 HTTP GET 实际 200；PUBLIC 返回不带签名的公共 URL 且匿名 GET 实际 200；PUBLIC 私签、错类型、他人、删除中对象拒绝。直传真实 init→HTTP PUT→complete/HEAD 成功；MinIO 离线/403/不存在时按发生步骤返回真实失败，不报“诊断未服务”。历史对象按 `sys_oss.service` 走原桶，改变默认不重路由。可复用 `OssAccessUrlMinioIntegrationTest`、`OssUploadStorageRoutingMinioIntegrationTest`、`OssStorageReadinessMinioIntegrationTest`，但它们现在人工塞 SERVING 且原用单一高权凭据；新验收必须去该前置并用分离身份、随机桶及精确 zero-skip fresh XML，不能把默认 Maven 环境跳过计为通过。

旧 `/tmp/wta-t44-current-audit.md` 的旧固定点仅供定位；以上结论仍须在 T-44 writer 接管时以最终 base/head 重核。T-44 验收不包含生产部署或真实存储修复，也不宣称 T-45/46/49 已完成。
