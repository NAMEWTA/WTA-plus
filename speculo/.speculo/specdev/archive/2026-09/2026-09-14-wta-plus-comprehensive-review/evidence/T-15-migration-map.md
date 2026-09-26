# T-15 方法迁移与最终核对

输入：当前源码；正式端口、Clock 与工作流能力保留，迁移后实际验证见 T-15.md。

| Owner | 删除旧签名 | 必需显式签名 | 实际调用 / 验证 |
|---|---|---|---|
| PersonApplicationUseCase | `PersonApplicationVo current()` | `PersonApplicationVo current(long userId)` | `PersonApplicationController` → `PersonApplicationUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonApplicationHttpContractTest` 和原 Service 测试 |
| PersonApplicationUseCase | `PersonApplicationVo save(PersonApplicationSaveBo command)` | `PersonApplicationVo save(long userId, PersonApplicationSaveBo command)` | `PersonApplicationController` → `PersonApplicationUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonApplicationHttpContractTest` 和原 Service 测试 |
| PersonApplicationUseCase | `PersonApplicationVo submit(int expectedVersion)` | `PersonApplicationVo submit(long userId, int expectedVersion)` | `PersonApplicationController` → `PersonApplicationUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonApplicationHttpContractTest` 和原 Service 测试 |
| PersonRebindUseCase | `PersonRebindMatchVo match(PersonRebindMatchBo command)` | `PersonRebindMatchVo match(long userId, PersonRebindMatchBo command)` | `PersonRebindController` → `PersonRebindUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonRebindHttpContractTest` 和原 Service 测试 |
| PersonRebindUseCase | `PersonRebindConfirmationVo confirm(PersonRebindConfirmBo command)` | `PersonRebindConfirmationVo confirm(long userId, PersonRebindConfirmBo command)` | `PersonRebindController` → `PersonRebindUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonRebindHttpContractTest` 和原 Service 测试 |
| PersonRebindUseCase | `PersonRebindSubmissionVo submit(PersonRebindSubmitBo command)` | `PersonRebindSubmissionVo submit(long userId, PersonRebindSubmitBo command)` | `PersonRebindController` → `PersonRebindUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonRebindHttpContractTest` 和原 Service 测试 |
| PersonRebindUseCase | `PersonRebindUnbindVo unbind()` | `PersonRebindUnbindVo unbind(long userId)` | `PersonRebindController` → `PersonRebindUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonRebindHttpContractTest` 和原 Service 测试 |
| PersonAdminUseCase | `PersonAdminResultVo decide(long applicationId, PersonAdminDecisionBo command)` | `PersonAdminResultVo decide(long operatorId, long applicationId, PersonAdminDecisionBo command)` | `PersonAdminController` → `PersonAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonAdminHttpContractTest` 和原 Service 测试 |
| PersonAdminUseCase | `PersonAdminResultVo create(PersonAdminCreateBo command)` | `PersonAdminResultVo create(long operatorId, PersonAdminCreateBo command)` | `PersonAdminController` → `PersonAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonAdminHttpContractTest` 和原 Service 测试 |
| PersonAdminUseCase | `PersonAdminResultVo revise(long profileId, PersonAdminReviseBo command)` | `PersonAdminResultVo revise(long operatorId, long profileId, PersonAdminReviseBo command)` | `PersonAdminController` → `PersonAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonAdminHttpContractTest` 和原 Service 测试 |
| PersonAdminUseCase | `PersonAdminResultVo manageBinding(long profileId, PersonAdminBindingBo command)` | `PersonAdminResultVo manageBinding(long operatorId, long profileId, PersonAdminBindingBo command)` | `PersonAdminController` → `PersonAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonAdminHttpContractTest` 和原 Service 测试 |
| PersonAdminUseCase | `PersonAdminResultVo assign(long profileId, PersonAdminAssignBo command)` | `PersonAdminResultVo assign(long operatorId, long profileId, PersonAdminAssignBo command)` | `PersonAdminController` → `PersonAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonAdminHttpContractTest` 和原 Service 测试 |
| PersonAdminUseCase | `PersonAdminResultVo revoke(long profileId, PersonAdminRevokeBo command)` | `PersonAdminResultVo revoke(long operatorId, long profileId, PersonAdminRevokeBo command)` | `PersonAdminController` → `PersonAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`PersonAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseApplicationUseCase | `EnterpriseApplicationVo current()` | `EnterpriseApplicationVo current(long userId)` | `EnterpriseApplicationController` → `EnterpriseApplicationUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseApplicationHttpContractTest` 和原 Service 测试 |
| EnterpriseApplicationUseCase | `EnterpriseApplicationVo save(EnterpriseApplicationSaveBo command)` | `EnterpriseApplicationVo save(long userId, EnterpriseApplicationSaveBo command)` | `EnterpriseApplicationController` → `EnterpriseApplicationUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseApplicationHttpContractTest` 和原 Service 测试 |
| EnterpriseApplicationUseCase | `EnterpriseApplicationVo submit(int expectedVersion)` | `EnterpriseApplicationVo submit(long userId, int expectedVersion)` | `EnterpriseApplicationController` → `EnterpriseApplicationUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseApplicationHttpContractTest` 和原 Service 测试 |
| EnterpriseAdminUseCase | `EnterpriseAdminResultVo decide(long applicationId, EnterpriseAdminDecisionBo command)` | `EnterpriseAdminResultVo decide(long operatorId, long applicationId, EnterpriseAdminDecisionBo command)` | `EnterpriseAdminController` → `EnterpriseAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseAdminUseCase | `EnterpriseAdminResultVo create(EnterpriseAdminCreateBo command)` | `EnterpriseAdminResultVo create(long operatorId, EnterpriseAdminCreateBo command)` | `EnterpriseAdminController` → `EnterpriseAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseAdminUseCase | `EnterpriseAdminResultVo revise(long profileId, EnterpriseAdminReviseBo command)` | `EnterpriseAdminResultVo revise(long operatorId, long profileId, EnterpriseAdminReviseBo command)` | `EnterpriseAdminController` → `EnterpriseAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseAdminUseCase | `EnterpriseAdminResultVo manageBinding(long profileId, EnterpriseAdminBindingBo command)` | `EnterpriseAdminResultVo manageBinding(long operatorId, long profileId, EnterpriseAdminBindingBo command)` | `EnterpriseAdminController` → `EnterpriseAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseAdminUseCase | `EnterpriseAdminResultVo assign(long profileId, EnterpriseAdminAssignBo command)` | `EnterpriseAdminResultVo assign(long operatorId, long profileId, EnterpriseAdminAssignBo command)` | `EnterpriseAdminController` → `EnterpriseAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseAdminUseCase | `EnterpriseAdminResultVo revoke(long profileId, EnterpriseAdminRevokeBo command)` | `EnterpriseAdminResultVo revoke(long operatorId, long profileId, EnterpriseAdminRevokeBo command)` | `EnterpriseAdminController` → `EnterpriseAdminUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseAdminHttpContractTest` 和原 Service 测试 |
| EnterpriseTransferUseCase | `EnterpriseTransferVo send(EnterpriseTransferSendBo command)` | `EnterpriseTransferVo send(long userId, EnterpriseTransferSendBo command)` | `EnterpriseTransferController` → `EnterpriseTransferUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseTransferHttpContractTest` 和原 Service 测试 |
| EnterpriseTransferUseCase | `EnterpriseTransferVo confirm(EnterpriseTransferConfirmBo command)` | `EnterpriseTransferVo confirm(long userId, EnterpriseTransferConfirmBo command)` | `EnterpriseTransferController` → `EnterpriseTransferUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseTransferHttpContractTest` 和原 Service 测试 |
| EnterpriseTransferUseCase | `EnterpriseTransferVo unbind()` | `EnterpriseTransferVo unbind(long userId)` | `EnterpriseTransferController` → `EnterpriseTransferUseCaseImpl`；同模块 `ExplicitProfileIdentityContractTest` 对应方法正负编译、`EnterpriseTransferHttpContractTest` 和原 Service 测试 |

其他逐方法决策：

- Person/EnterpriseApplicationUseCase.handleProcess(command)、PersonRebindUseCase.handleProcess(command)：保留能力，改为抽象必需实现，删除 throwing default。
- Person/EnterpriseWorkflowGateway.terminate 与 persistedSnapshotVersionByInstanceId：保留能力，改为抽象必需实现；persistedSnapshotVersion(ProcessEvent) 是有意义的值转换，保留。
- IPerson/IEnterpriseAdminService 的 page/eligibleUsers/detail/review/reviewMaterial/material/decide/create/revise/manageBinding/assign/revoke：同名正式 AdminUseCase 与真实 Service 均存在，原接口无调用者。删除空置重复合同。
- IPerson/IEnterpriseApplicationService 的 current/save/submit（企业另有 probe）：正式 ApplicationUseCase；继承的 publication 能力由已有 ApplicationPublicationPort 承载。测试 consumers 迁移到正确 port。
- IPersonRebindService 的 probe/match/confirm/submit/unbind/publishApprovedRebind：正式 RebindUseCase/Service，publishApproved 保留用例映射。无引用的旧接口删除。
- IEnterpriseTransferService 的 send/confirm/unbind：正式 TransferUseCase/Service，旧接口无引用，删除。
- IProfileMaterialService：空扩展，ProfileMaterialService 直接 implements ProfileMaterialPort，不改任何材料方法。
- Person/EnterpriseApplicationService：移除空 Object jsonMapper 参数和具体 provider 构造；保留正式 registry port 与可注入 Clock；handleProcessEvent 与其私有重复字段转换删除，测试通过生产 Listener 或正式 command 验证相同行为。
- Person/EnterpriseAdminService：移除 Object jsonMapper/WorkflowService 构造与 legacyWorkflow，测试使用正式 WorkflowGateway port，保留 terminate 行为断言。
- PersonRebindService：移除空 Object jsonMapper 与具体 provider 构造；完整 port+Clock 构造公开用于真实时间及事件验证，保留现有配置/通知/事件能力。
- EnterpriseTransferService：删除具体 CodeGenerator 重载，保留 EnterpriseTransferCodePort+Clock 正式构造。
- Person/EnterpriseVerificationEvidenceCodec(Object)：迁移无参构造；encode/decode 及已存在的证据读取格式行为不变。
- 两侧材料 UseCase.accessUrlView：复核源码为 throwing default（初始清单判断有误），改为抽象必需实现；正式 UseCaseImpl、Service 和测试夹具已具有真正实现，预览能力保留。
- wta-api/ProfileService.findByUserId：具备实际派生行为，不是过渡桥，保留；本票不机械删除所有 default 或 I 命名。

最终引用/JAR 审计见 T-15-legacy-audit.json；最新 clean package 239/239、消费者 679 pass/31 属性门控 skip。25 个缺身份调用均编译失败，显式身份均编译成功。wta-api 无修改。

## 构造逐项对照

每条旧签名来自实施前 T-15-inventory.json；下方现存签名直接提取最终源码。迁移后的调用者由 Profile clean package 与 Admin 消费者 test-compile/test 编译约束验证。

### PersonRebindService

| 原构造签名 | 结果 |
|---|---|
| `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, Object jsonMapper, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, Object jsonMapper, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, ConfigService configService, PersonRebindNotificationPort notifications, ApplicationEventPublisher events)` | 保留正式依赖接缝 |
| `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, Object jsonMapper, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, ConfigService configService, PersonRebindNotificationPort notifications, ApplicationEventPublisher events)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, Object jsonMapper, ProfileMaterialPort materials, org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users)`
- `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, Clock clock)`
- `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, ConfigService configService, PersonRebindNotificationPort notifications, ApplicationEventPublisher events)`
- `public PersonRebindService(PersonRebindDao dao, PersonApplicationDao applicationDao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, UserService users, Clock clock, ConfigService configService, PersonRebindNotificationPort notifications, ApplicationEventPublisher events)`

### PersonAdminService

| 原构造签名 | 结果 |
|---|---|
| `public PersonAdminService(PersonAdminDao dao, PersonApplicationPublicationPort applications, ProfileMaterialPort materials, PersonWorkflowGateway workflow, UserService users)` | 保留正式依赖接缝 |
| `public PersonAdminService(PersonAdminDao dao, PersonApplicationPublicationPort applications, ProfileMaterialPort materials, PersonWorkflowGateway workflow, UserService users, Clock clock)` | 保留正式依赖接缝 |
| `public PersonAdminService(PersonAdminDao dao, Object jsonMapper, PersonApplicationPublicationPort applications, ProfileMaterialPort materials, WorkflowService workflow, UserService users)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public PersonAdminService(PersonAdminDao dao, Object jsonMapper, PersonApplicationPublicationPort applications, ProfileMaterialPort materials, WorkflowService workflow, UserService users, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public PersonAdminService(PersonAdminDao dao, PersonApplicationPublicationPort applications, ProfileMaterialPort materials, PersonWorkflowGateway workflow, UserService users)`
- `public PersonAdminService(PersonAdminDao dao, PersonApplicationPublicationPort applications, ProfileMaterialPort materials, PersonWorkflowGateway workflow, UserService users, Clock clock)`

### PersonApplicationService

| 原构造签名 | 结果 |
|---|---|
| `public PersonApplicationService(PersonApplicationDao dao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, ConfigService configService)` | 保留正式依赖接缝 |
| `public PersonApplicationService(PersonApplicationDao dao, Object jsonMapper, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, ConfigService configService, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public PersonApplicationService(PersonApplicationDao dao, Object jsonMapper, ProfileMaterialPort materials, org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, ConfigService configService, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public PersonApplicationService(PersonApplicationDao dao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, ConfigService configService)`
- `public PersonApplicationService(PersonApplicationDao dao, ProfileMaterialPort materials, PersonVerificationProviderRegistryPort providers, PersonVerificationService attempts, PersonWorkflowGateway workflow, ConfigService configService, Clock clock)`

### PersonVerificationAttemptService

| 原构造签名 | 结果 |
|---|---|
| `public PersonVerificationAttemptService(PersonVerificationProviderRegistryPort providerRegistry, PersonVerificationAttemptDao dao, PersonVerificationEvidenceCodec evidenceCodec)` | 保留正式依赖接缝 |
| `public PersonVerificationAttemptService(PersonVerificationProviderRegistryPort providerRegistry, PersonVerificationAttemptDao dao, PersonVerificationEvidenceCodec evidenceCodec, Object ignoredAuditRecorder)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public PersonVerificationAttemptService(PersonVerificationProviderRegistryPort providerRegistry, PersonVerificationAttemptDao dao, PersonVerificationEvidenceCodec evidenceCodec)`

### PersonVerificationEvidenceCodec

| 原构造签名 | 结果 |
|---|---|
| `public PersonVerificationEvidenceCodec()` | 保留正式依赖接缝 |
| `public PersonVerificationEvidenceCodec(Object ignoredJsonMapper)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public PersonVerificationEvidenceCodec()`

### EnterpriseAdminService

| 原构造签名 | 结果 |
|---|---|
| `public EnterpriseAdminService(EnterpriseAdminDao dao, EnterpriseApplicationPublicationPort applications, ProfileMaterialPort materials, EnterpriseWorkflowGateway workflow, UserService users, ProfileService profiles)` | 保留正式依赖接缝 |
| `public EnterpriseAdminService(EnterpriseAdminDao dao, EnterpriseApplicationPublicationPort applications, ProfileMaterialPort materials, EnterpriseWorkflowGateway workflow, UserService users, ProfileService profiles, Clock clock)` | 保留正式依赖接缝 |
| `public EnterpriseAdminService(EnterpriseAdminDao dao, Object jsonMapper, EnterpriseApplicationPublicationPort applications, ProfileMaterialPort materials, WorkflowService workflow, UserService users, ProfileService profiles)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public EnterpriseAdminService(EnterpriseAdminDao dao, Object jsonMapper, EnterpriseApplicationPublicationPort applications, ProfileMaterialPort materials, WorkflowService workflow, UserService users, ProfileService profiles, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public EnterpriseAdminService(EnterpriseAdminDao dao, EnterpriseApplicationPublicationPort applications, ProfileMaterialPort materials, EnterpriseWorkflowGateway workflow, UserService users, ProfileService profiles)`
- `public EnterpriseAdminService(EnterpriseAdminDao dao, EnterpriseApplicationPublicationPort applications, ProfileMaterialPort materials, EnterpriseWorkflowGateway workflow, UserService users, ProfileService profiles, Clock clock)`

### EnterpriseTransferService

| 原构造签名 | 结果 |
|---|---|
| `public EnterpriseTransferService(EnterpriseTransferDao dao, EnterpriseTransferChallengeStore challenges, EnterpriseTransferCodePort codes, PersonIdentityLookupService personIdentities, UserService users, NotificationApplicationService notify)` | 保留正式依赖接缝 |
| `public EnterpriseTransferService(EnterpriseTransferDao dao, EnterpriseTransferChallengeStore challenges, EnterpriseTransferCodePort codes, PersonIdentityLookupService personIdentities, UserService users, NotificationApplicationService notify, Clock clock)` | 保留正式依赖接缝 |
| `public EnterpriseTransferService(EnterpriseTransferDao dao, EnterpriseTransferChallengeStore challenges, org.namewta.profile.enterprise.adapter.security.EnterpriseTransferCodeGenerator codes, PersonIdentityLookupService personIdentities, UserService users, NotificationApplicationService notify, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public EnterpriseTransferService(EnterpriseTransferDao dao, EnterpriseTransferChallengeStore challenges, EnterpriseTransferCodePort codes, PersonIdentityLookupService personIdentities, UserService users, NotificationApplicationService notify)`
- `public EnterpriseTransferService(EnterpriseTransferDao dao, EnterpriseTransferChallengeStore challenges, EnterpriseTransferCodePort codes, PersonIdentityLookupService personIdentities, UserService users, NotificationApplicationService notify, Clock clock)`

### EnterpriseVerificationAttemptService

| 原构造签名 | 结果 |
|---|---|
| `public EnterpriseVerificationAttemptService(EnterpriseVerificationProviderRegistryPort providerRegistry, EnterpriseVerificationAttemptDao dao, EnterpriseVerificationEvidenceCodec evidenceCodec)` | 保留正式依赖接缝 |
| `public EnterpriseVerificationAttemptService(EnterpriseVerificationProviderRegistryPort providerRegistry, EnterpriseVerificationAttemptDao dao, EnterpriseVerificationEvidenceCodec evidenceCodec, Object ignoredAuditRecorder)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public EnterpriseVerificationAttemptService(EnterpriseVerificationProviderRegistryPort providerRegistry, EnterpriseVerificationAttemptDao dao, EnterpriseVerificationEvidenceCodec evidenceCodec)`

### EnterpriseApplicationService

| 原构造签名 | 结果 |
|---|---|
| `public EnterpriseApplicationService(EnterpriseApplicationDao dao, ProfileMaterialPort materials, EnterpriseVerificationProviderRegistryPort providers, EnterpriseVerificationService attempts, EnterpriseWorkflowGateway workflow, ConfigService configService)` | 保留正式依赖接缝 |
| `public EnterpriseApplicationService(EnterpriseApplicationDao dao, Object jsonMapper, ProfileMaterialPort materials, EnterpriseVerificationProviderRegistryPort providers, EnterpriseVerificationService attempts, EnterpriseWorkflowGateway workflow, ConfigService configService, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |
| `public EnterpriseApplicationService(EnterpriseApplicationDao dao, Object jsonMapper, ProfileMaterialPort materials, org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry providers, EnterpriseVerificationService attempts, EnterpriseWorkflowGateway workflow, ConfigService configService, Clock clock)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public EnterpriseApplicationService(EnterpriseApplicationDao dao, ProfileMaterialPort materials, EnterpriseVerificationProviderRegistryPort providers, EnterpriseVerificationService attempts, EnterpriseWorkflowGateway workflow, ConfigService configService)`
- `public EnterpriseApplicationService(EnterpriseApplicationDao dao, ProfileMaterialPort materials, EnterpriseVerificationProviderRegistryPort providers, EnterpriseVerificationService attempts, EnterpriseWorkflowGateway workflow, ConfigService configService, Clock clock)`

### EnterpriseVerificationEvidenceCodec

| 原构造签名 | 结果 |
|---|---|
| `public EnterpriseVerificationEvidenceCodec()` | 保留正式依赖接缝 |
| `public EnterpriseVerificationEvidenceCodec(Object ignoredJsonMapper)` | 移除旧重载/空参数；调用方采用下列正式构造 |

现存构造：

- `public EnterpriseVerificationEvidenceCodec()`

## 最后发现的审计占位桥

个人/企业 VerificationAttemptService 的四参构造均忽略 Object ignoredAuditRecorder，11 处测试构造调用改为三参正式构造。两份只用于这个占位参数的测试 VerificationSecurityAuditRecorder 辅助类同时删除；没有删除测试用例或业务断言。安全审计继续由生产 Service.recordSecurityAudit → DAO.insertSecurityAudit 执行，原 CallbackContract、AnonymousController 和 AttemptService 测试均在最终 239 项中通过。

## 空实现与重复接口的方法替代

前述七份 I* 接口不因名字而删除：Admin 的每个查询/命令由同名正式 UseCase/Service 实现；Application 发布能力由 PublicationPort 承载；Rebind 的 publishApprovedRebind 由出版端口承载，并保留 UseCase 的 publishApproved 映射；Transfer 的三个方法保留显式用户合同；Material 空扩展改为直接实现 ProfileMaterialPort。没有任何接口的安全、事务或工作流能力被移除。两个 WorkflowGateway 的 terminate / persistedSnapshotVersionByInstanceId 和三处 handleProcess、两处 accessUrlView 从默认失败/空值改为必需实现；生产适配器原有行为保留，遗漏实现现由编译器拒绝。
