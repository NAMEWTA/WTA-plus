# Profile capability map

## Modules

- `wta-profile` 是聚合 POM，包含 `wta-profile-person`、`wta-profile-enterprise` 和 BOM。
- person 拥有个人申请、认证、材料、档案投影、重新绑定和通知。
- enterprise 拥有企业申请、认证、材料、档案投影和转移。
- enterprise transfer 只能通过 `wta-api` 的 `PersonIdentityLookupService` 查个人精确匹配，不读取 person 实现或数据库。

## Public contracts

- `org.namewta.profile.api.ProfileService`
- `org.namewta.profile.api.ProfileProjectionContributor`
- `org.namewta.profile.api.material.ProfileMaterialPort`
- `org.namewta.profile.api.person.PersonIdentityLookupService`

重构只能替换实现内部结构；方法、返回字段、批量语义、锁内复核和敏感字段最小化保持不变。

## External contracts

- system：`org.namewta.system.api.UserService`、`ConfigService`、`OssService` 及 common SPI；通知统一依赖 `org.namewta.notify.api.NotificationApplicationService`。
- workflow：`org.namewta.workflow.api.WorkflowService`、`ProcessEvent`、`ProcessTaskEvent`、`ProcessDeleteEvent`。
- Redis challenge：`EnterpriseTransferChallengeStore` 是 Store，不是 DAO；合同属于 `port/store`，实现属于 `adapter/store`，不得调用 Mapper。
- verification：`<Person|Enterprise>VerificationProvider` 只负责 provider 认证和规范化证据，不直接发布档案或修改绑定；合同属于 `port/provider`，实现属于 `adapter/provider`。

## Entry surfaces

- `controller/admin`：登录管理端。
- `controller/self`：已登录自服务。
- `controller/anonymous`：回调/公网入口，保留 `@SaIgnore`、签名、nonce、重放、幂等、限流和审计。
- Listener 和公共 API Adapter 同样走 UseCase，不直接调用 Service/DAO。
