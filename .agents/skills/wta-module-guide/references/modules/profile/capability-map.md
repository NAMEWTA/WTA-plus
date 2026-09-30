# Profile capability map

## Modules

- `wta-profile` 是聚合 POM，包含 `wta-profile-person`、`wta-profile-enterprise` 和 BOM。
- person 拥有个人申请、认证、材料、档案投影、重新绑定和通知。
- enterprise 拥有企业申请、认证、材料、档案投影和转移。
- enterprise transfer 只能通过 `wta-api` 的 `PersonIdentityLookupService` 查个人精确匹配，不读取 person 实现或数据库。

## Public contracts

- `org.namewta.profile.api.ProfileService`
- `org.namewta.profile.api.ProfileDisclosureService`：按固定字段集合读取当前有效个人/企业档案，完整证件须显式选择，不包含材料、历史或审核；旧非敏感摘要保持原合同。
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

## 个人中心与任务审核

- `GET /profile/{person|enterprise}/application/summary` 只接受当前会话用户，返回进行中申请、退回原因及有效绑定的当前认证资料；FINISH 不返回可编辑 current。
- `GET /profile/{person|enterprise}/review/tasks/{taskId}` 与材料子路径只接受本端本人当前或历史已办任务，读取对应 submissionId/snapshotVersion，不用最新申请替换历史。
- `POST /profile/{person|enterprise}/review/tasks/{taskId}/decision` 使用 `APPROVE|RETURN`、reason、snapshotVersion；RETURN 保存退回原因并经公开 Workflow 合同退回申请节点。
- 任务权限为 `profile:{person|enterprise}:task-review`，不授予 archive 全局读取或管理员覆盖。材料经公开 `ProfileTaskMaterialPort` 进入材料 UseCase，复核任务/流程/提交归属。
- 两套认证种子均为 START → 申请人 BETWEEN → 人工审核 BETWEEN → END；只有最后审核通过才发布档案。跨端依靠节点 Client 和角色/用户配置，不是给每端复制业务申请。
