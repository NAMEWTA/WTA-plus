# T-44 writer 交接方案（只读，2026-09-23）

固定核查输入：`2c8047a2bfca47389557e4261ff5a55036e1c72e` 的 T-44 Ticket、`/tmp/wta-t44-activation-audit.md`、现有 System/OSS/Notify 源码与 Skill。此稿未修改仓库、未运行构建/测试/服务；T-40 C2 门禁仍由 Lead 串行执行。实施前以最终 dispatch base 重核。

## 最小生产切片

1. **文件业务不读诊断快照。** `OssLifecycleManager.requireDownloadable` 仅接受存在且 `deleteState=ACTIVE`、`service` 非空的对象；按对象自己的 `service` 调当前 `OssFactory` 配置和 `objectStore.accessPolicy`。PUBLIC_READ 只返回非空公共 URL、无私签/到期；PRIVATE 只返回受服务端 TTL 限制的非空签名及真实 `expiresAt`，错误类型/缺配置/坏 URL 失败关闭。调用方现有业务 owner/Client 校验及管理 `system:oss:download` 仍是授权权威，不能仅凭 `ossId` 放行。直传 `OssUploadService` 移除 registry 前置，保留当前身份、Client、策略、大小/类型和固定 ticket route；`DefaultOssUploadObjectStore.prepare` 已在实际客户端验证 policy、Bucket、Multipart 能力。迁移 `OssStorageMigrationService.requireRoute` 改为当前配置检查，不依赖 serving 快照；保留 ACTIVE、源 PRIVATE、目标 PUBLIC_READ、不同 service、HEAD/内容冲突及 T-46 以外的现有锁。`SysOssServiceImpl` 的列表/元数据保持纯 DB，证明零对象存储 I/O。
2. **核心启动不做远端 OSS I/O。** `SystemApplicationRunner` 只执行 DB 配置缓存初始化，不调用 `readinessService.refresh()`。`SysOssConfigServiceImpl.init()` 对空表及坏的非默认行可继续启动：DB-only 有界读取，隔离并失效已知坏 key 的缓存/客户端；默认指针只在 DB **恰好一个** `status=Y` 且该行有效 PRIVATE 时写入，否则删除旧 `DEFAULT_CONFIG_KEY`，不得用残留 Redis key 复活旧客户端。缺默认时文件功能明确失败，合法非默认历史对象仍按自身 service；管理新增/切换对唯一 PRIVATE 默认的强校验不放松。DB 本身不可用仍是核心依赖错误，不伪装 OSS 诊断降级。`OssUploadDiagnostics` 去掉 `ApplicationReadyEvent` 远端 Bucket 检查。
3. **诊断只由管理员主动触发。** 删除 `OssStorageReadinessService.refresh()` 的每分钟 `@Scheduled`；保留 registry/indicator 作为可选诊断事实。新增受 `system:ossConfig:list`（或更严格已存在管理权限）保护的单配置 POST 诊断入口，`@Log` 不保存配置密钥/URL/凭据/签名正文；service 按受限配置 key、固定最大超时和一次客户端诊断更新该 key 快照，返回仅 status/reason/checkedAt 等去敏事实。缺配置、坏 policy、远端超时显式 NOT_SERVING/UNVERIFIED，不传播到业务门禁。`OssConfigChangeListener` 提交后仍更新/失效配置缓存与 `OssFactory`，但只使相应诊断快照失效，绝不在提交回调同步全量远端刷新。`OssStorageReadinessProperties` 的可选诊断参数（含无法转换的 Duration）不得使核心启动失败；用安全有界默认并让诊断报告配置无效，不允许无界 I/O。
4. **健康组分离，调度仍全局启用。** 配置显式 core readiness/liveness 与独立 OSS 诊断组；核心组只纳入真实核心依赖，OSS DOWN/缺 canary/过期不改变核心 readiness。现有 Docker 后端 healthcheck 只探 TCP 8080，不可声称它已经验证 HTTP readiness；文档/部署探针按实际组路径及现有 Actuator Basic Auth 校验。`OssStorageReadinessSchedulingConfiguration` 是目前唯一无条件 `@EnableScheduling`，SnailJob 的注解仅在启用 job 时存在；删除 OSS 巡检时应把无条件 `@EnableScheduling` 移到始终加载的 `NamewtaApplication`（或同等非 OSS app config），绝不禁用 `NotifyOutboxWorker` 的 `${notify.outbox.poll-delay-ms:60000}` 兜底。
5. 同步 `wta-system/AGENTS.md`、System Skill `domains.md`、`backend/README.md` 与适用部署说明：将“readiness SERVING 才签 URL”的旧硬规则替换为对象/业务授权、当前配置、预期访问类型和真实远端结果；诊断只供管理员判断，不创建 Bucket/修改 ACL/Policy。`wta-api`、common OSS、六 SQL、T-45 策略解释、T-46 清理锁均不改。

## 实施前须补登记的精确写集

当前 Ticket 的 OSS `service/readiness/upload/migration`、`SystemApplicationRunner.java`、`application.yml`、admin OSS 测试根、文档/Skill、`release-artifacts/docker/**` 已覆盖。还须登记：

- `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssConfigServiceImpl.java`：容错启动缓存与安全默认键。
- `backend/wta-modules/wta-system/src/main/java/org/namewta/system/listener/OssConfigChangeListener.java`：提交后只失效诊断，保留配置缓存/客户端失效。
- `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java`：受权限保护的单配置诊断入口；可直接注入已有 readiness service，无需扩 `ISysOssConfigService` 或 public `wta-api`。
- `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java`：从 OSS 专属配置迁出无条件 `@EnableScheduling`。若 Lead 选择独立常驻 config，改为预登记该 config 精确路径，不得两处重复启用。

新诊断返回 DTO 若需要，放既有已授权 `oss/readiness/**`；先核真实 controller 传输/API/日志规则。上述 4 个文件是当前必要扩写集，不先改后报。

## 红灯与验证

- **先红的定向用例**：`OssLifecycleManagerUnitTest`、`OssUploadServiceUnitTest`、`OssStorageMigrationServiceUnitTest` 分别用合法当前配置/owner、空或过期 registry，要求 URL、init、dry-run 走实际客户端；旧代码会以 STORAGE_NOT_SERVING 拒绝。并列负例：他人/非 ACTIVE/空 service/缺配置/错 PRIVATE-PUBLIC_READ/multipart 不支持，在签名、ticket、copy 前拒绝；DB 元数据 spy 的 OSS 调用为零。更新 `OssAccessUrlArchitectureUnitTest` 与 `OssStorageReadinessSchedulingUnitTest` 的旧硬编码断言，不删除测试。`OssConfigGovernanceUnitTest` 补空表、坏非默认、重复/坏默认、陈旧 Redis key、管理端第二默认拒绝；配置变更监听证明无远端 refresh。HTTP 诊断无权 403/零诊断调用、有权单 key、错误去敏和超时有界；核心/OSS health 分组单测。
- **真实服务由 Lead 验收**：隔离 MySQL 8.4 六 SQL、Redis、最小权限 MinIO/loopback full app；空配置、有效 PRIVATE 默认+坏非默认、缺 canary、过期快照、MinIO 离线时，启动/登录/菜单/IN_APP 与 core readiness 可用，启动期远端 OSS 诊断调用计数 0；独立 OSS group 可 DOWN/UNKNOWN。无默认不能复用旧 Redis pointer。最小权限 app 身份不能管理 Bucket ACL/Policy：PRIVATE 签名 TTL+真实 GET 200，PUBLIC 匿名 URL GET 200 且不私签，浏览器直传真实 init→PUT→complete/HEAD；离线、403、缺对象按实际步骤失败。历史对象在默认切换后仍走 `sys_oss.service`。复用/扩充 `OssAccessUrlMinioIntegrationTest`、`OssUploadStorageRoutingMinioIntegrationTest`、`OssStorageReadinessMinioIntegrationTest`，要求真实启用且精确 XML 0 skip，不以原高权凭据夹具冒充最小权限。
- **Notify 回归必须真实触发**：在 full app 设置 `snail-job.enabled=false`、短正数 poll delay，丢失/关闭 Redis wake 后提交 IN_APP Outbox，观察 `@Scheduled` fallback Worker 最终落本人消息、Attempt/Delivery/Outbox 正确；仅反射 `@EnableScheduling` 或 `NotifyOutboxWorkerWakePollTest` 不足以证明。admin OSS 定向单测、System/Notify 受影响集与全后端 test/package、Skill/模块事实、去敏日志及 clean source 由 Lead 在固定 commit 串行执行。

残余边界：诊断不会证明未来一次 GET/PUT 必然成功；预签名产生与远端可读是不同事实。T-44 不部署、不清理真实 OSS 数据，也不承诺 T-45/T-46/T-49 完成。
