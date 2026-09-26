# 2026-09-23 当前源码与报告逐项对照

报告固定点64b4ea7；当前HEAD `1264980c74e594bc594e88561bb292fbe5d968a1`。60个引用已逐文件比对：50相同，10仅删除作者/联系元数据。当前受审行为没有被这些差异修复。可定位hash见<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/current-source-audit.json</Path>。

本轮执行源码读取、调用扫描、Git比对与文档校验；**没有运行业务测试、浏览器、真实DB/Redis/MinIO或供应商请求**。源码可推导的异常不是生产事故声明。静态确认15项问题路径、1项N-10条件规模风险，D-01/D-02是改进建议，保持原报告分类。

| 报告项 | 当前结论 | 实施责任/AC | 直接证据 |
|---|---|---|---|
| R64-S-01 | 源码路径仍存在；未动态复现 | T-32/AC-032 | <Path>backend/wta-admin/src/main/resources/application-local.yml</Path> |
| R64-S-02 | 源码路径仍存在；未动态复现 | T-33/AC-033 | <Path>backend/wta-admin/src/main/resources/application.yml</Path>、<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java</Path>、<Path>backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java</Path> |
| R64-N-01 | 源码路径仍存在；未动态复现 | T-34/AC-034 | <Path>frontend/apps/admin-web/.env.development</Path>、<Path>frontend/apps/admin-web/.env.production</Path>、<Path>frontend/apps/admin-web/src/utils/push.ts</Path> |
| R64-N-02 | 源码路径仍存在；未动态复现 | T-35/AC-035 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>、<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java</Path>、<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/NotifyTemplateContent.java</Path> |
| R64-N-03 | 源码路径仍存在；未动态复现 | T-36/AC-036 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/InAppNotificationService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>、<Path>backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java</Path> |
| R64-N-04 | 源码路径仍存在；未动态复现 | T-37/AC-037 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>、<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java</Path>、<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/RedisNotifyIdempotencyStore.java</Path> |
| R64-N-05 | 源码路径仍存在；未动态复现 | T-38/AC-038 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java</Path>、<Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java</Path> |
| R64-N-06 | 源码路径仍存在；未动态复现 | T-39/AC-039; T-50/AC-050 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml</Path>、<Path>backend/wta-api/src/main/java/org/namewta/notify/api/NotificationCommand.java</Path> |
| R64-N-07 | 源码路径仍存在；未动态复现 | T-40/AC-040 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java</Path>、<Path>frontend/packages/web-domains/notify/src/NoticePage.vue</Path> |
| R64-N-08 | 源码路径仍存在；未动态复现 | T-34/AC-034; T-41/AC-041 | <Path>frontend/apps/admin-web/src/utils/push.ts</Path>、<Path>frontend/apps/admin-web/src/layout/components/notice/index.vue</Path>、<Path>frontend/packages/web-domains/notify/src/InboxPage.vue</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path> |
| R64-N-09 | 源码路径仍存在；未动态复现 | T-42/AC-042 | <Path>backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySceneCatalog.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySendPlanner.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java</Path> |
| R64-N-10 | 条件规模风险，未实测 | T-43/AC-043 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java</Path>、<Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/policy/NotificationAggregatePolicy.java</Path> |
| R64-O-01 | 源码路径仍存在；未动态复现 | T-44/AC-044 | <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssServiceImpl.java</Path>、<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/OssLifecycleManager.java</Path>、<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessService.java</Path>、<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessRegistry.java</Path> |
| R64-O-02 | 源码路径仍存在；未动态复现 | T-45/AC-045 | <Path>backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java</Path> |
| R64-O-03 | 源码路径仍存在；未动态复现 | T-46/AC-046 | <Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/OssStorageMigrationService.java</Path>、<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/MybatisOssMigrationStore.java</Path> |
| R64-O-04 | 源码路径仍存在；未动态复现 | T-47/AC-047 | <Path>frontend/packages/web-domains/system/src/oss/OssPage.vue</Path> |
| R64-D-01 | 建议纳入；非已测缺陷 | T-48/AC-048 | <Path>scripts/start-dev.sh</Path> |
| R64-D-02 | 建议纳入；非已测缺陷 | T-49/AC-049 | <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyConfigService.java</Path>、<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/OssUploadService.java</Path>、<Path>backend/wta-admin/src/main/resources/application.yml</Path>、<Path>backend/wta-admin/src/main/resources/application-local.yml</Path> |

## 比报告进一步核实的施工影响

1. 消息关系唯一键已在<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>存在：不盲目补索引，T-36修原子写入及真实事务边界。
2. 非零priority生产调用存在于验证码、Auth、Workflow、Profile、Demo和公告，T-50必须与拒绝未支持值同批迁移。
3. 真实硬约束来源为<Path>backend/wta-modules/wta-system/AGENTS.md</Path>。readiness门禁目前为MUST，D-008未确认不能直接删除；T-44同步规范与安全替代测试。
4. 附件SPI只有测试替身，<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/config/NotifyAutoConfiguration.java</Path>允许未装配。<Path>backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java</Path>会拒绝有附件但无服务。T-42须生产装配＋授权上下文＋持久owner；只补attachmentOssIds不足。
5. <Path>backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java</Path>的seen/read/readAll缺安全@Log，在T-41触及合同中补齐。
6. <Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/reviews/legacy-ticket-audit.json</Path>核实全部31个历史implementation commit存在且为当前HEAD祖先；这只证明提交链，不证明本次动态正确性。旧共同result不能冒充现行逐票clean验收。

## 保留与排除误报

保留发布快照/接收人快照、owner/token/lease、外部I/O事务外、HMAC原始正文和持久receipt、DB-only元数据、专用访问URL授权、客户端初始化不自动建桶。未把unpublish保留公开副本报成隐秘泄漏；未把作者未收消息等同后端鉴权失效；未把辅助CORS/Lifecycle警告说成必然JVM退出。普通站内信不依赖邮件/SMS/MinIO。

## SSO交接

本报告第9节只作为<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/ADR.md</Path>的待核对合同来源，本轮不跨写、不实现OIDC、不把pending设计当已落地。内置issuer可选、客户端标准流、关闭内置可接外部、信任配置分离与本地身份映射必须由该change负责。

## 旧票路径漂移核对

原写集中的37份AGENTS已在历史T-29删除，本轮从活动写集移除，删除证据及规则归宿保留在before快照；不重建这些手册。verify-submodules.sh已退役，不作为当前门禁。release-state/add_app由Python迁至mjs，NotifyOutboxWakePublisher位于adapter/event，System组合函数位于web-domains/system/src/composables.ts，HTTP契约位于platform/http/src/index.ts；旧票和Map已改用当前落点。application-local.example.yml是明确计划新增文件。
