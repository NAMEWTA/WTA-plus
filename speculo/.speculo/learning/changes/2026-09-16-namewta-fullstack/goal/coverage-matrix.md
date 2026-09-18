# Coverage Matrix: NAMEWTA 前后端全模块

状态只允许：`uncovered` | `covered` | `deferred(reason)` | `covered-by-parent`。

默认档：架构/数据流 = Relational；范围内函数/方法 = Unistructural + explained。`coverage_depth=deep`。

现有四门课已嵌入 `children/`。U1…U9 均已升级/新写并挖掘。Lead D 结束于 `2026-09-17T14:40:00.000Z`：范围内格子全部 `covered` / `deferred(reason)` / `covered-by-parent`。

replan `2026-09-16T08:47:16.000Z`：覆盖波 2；mine unit ≤15。

## (c) 业务架构

| 单元 | 类型 | 状态 | 证据 | 备注 |
| --- | --- | --- | --- | --- |
| NAMEWTA 系统 Context | Context | covered | L-001 + GP-L-001-b01 | C4 人/机器/外部渠道 |
| admin-web | Container | covered | L-004 + GP-L-004-b01 | 组装全部业务 web-domain |
| home-web | Container | covered | L-004 + GP-L-004-b01 | 身份+资料子集 |
| sso-web | Container | covered | L-004 + GP-L-004-b01 | 认人厅 |
| wta-admin | Container | covered | L-002 + GP-L-002-b01 | AuthController + 模块组装 |
| wta-api | Container | covered | L-002 + GP-L-002-b01 | 跨模块 Java 合同 |
| wta-modules 业务房间 | Container | covered | L-002 + L-003 + GP-L-002-b01 + GP-L-003-b01 | layered/classic 登记表 |
| wta-common 工具间（主路径枢纽） | Container | covered | L-002 + GP-L-002-b01 | notify/mybatis/satoken/openapi |
| MySQL 8.4 基座 | Container | covered | L-085 + GP-L-085-b01 | 10-cde-base-ddl / 50-cde-base-dml |
| Redis 8 | Container | covered | L-084 + GP-L-084-b01 | 会话抽屉已讲；锁/限流同住，函数格仍 L-012/L-032 |
| MinIO/OSS | Container | covered | L-025…L-029 + GP-L-025…L-029-b01 | 直传对象 |
| repo-ownership frontend/backend/docs/release-artifacts/skills | Component | covered | L-001 + GP-L-001-b01 | L-001 |
| layered-classic 登记 | Component | covered | L-003 + GP-L-003-b01 | L-003 |
| frontend-direction App→web-domain→domain→platform | Component | covered | L-004 + GP-L-004-b01 | L-004 |
| public-contracts HTTP/SQL/OpenAPI/wta-api | Component | covered | L-005 + GP-L-005-b01 | L-005 |
| admin-web-composition services.ts | Component | covered | L-007 + GP-L-007-b01 | L-007 |
| dao-mapper-ownership | Component | covered | L-083 + GP-L-083-b01 | DAO 或 classic Service 才持 Mapper |
| mysql-base | Component | covered | L-085 + GP-L-085-b01 | 自有表/数据权威在 10/50；活门牌 master |
| bundle-full-job | Component | covered | L-079 + GP-L-079-b01 | L-079 |

## (d) 数据流

| 路径 | 源 → 汇 | 状态 | 证据 | 备注 |
| --- | --- | --- | --- | --- |
| 主路径 登录后查询 | GET/POST /auth/* → 会话 → GET /system/menu → domain GET → Mapper XML → MySQL | covered | L-006 + GP-L-006-b01 | 菜单枪为 GET /system/menu/getRouters |
| 失败路径 认证拒绝 | 缺 Client / 验证码 / 密码策略 → 不写会话 | covered | L-006 + GP-L-006-b01 | 函数级 Captcha/密码策略仍由 L-012 解释 |
| 失败路径 会话丢失 | 登录成功但 Redis/Sa-Token 读失败 | covered | L-084 + GP-L-084-b01 | 已发票再读失败；≠ L-006 认证拒绝 |
| 主路径 OSS 直传 | init ticket → 浏览器 PUT 对象 → complete → 业务引用 | covered | L-027 + GP-L-027-b01 | 浏览器 PUT 细节 L-029 |
| 失败路径 OSS abort | complete 失败 → abort，不留业务引用 | covered | L-027 + GP-L-027-b01 | L-027 |
| 主路径 SSO 授权码 | authorize PKCE → login 会话 → token 换业务令牌 | covered | L-055 + L-056 + L-057 + GP-L-055-b01 + GP-L-056-b01 + GP-L-057-b01 | 发码+手环+换票 |
| 失败路径 SSO PKCE | code_verifier 不匹配 → 不发令牌 | covered | L-055 + L-056 + GP-L-055-b01 + GP-L-056-b01 | 授权时拒挑战；换票 verify |
| 主路径 通知提交 | NotificationController.submit → Outbox → claim → 渠道 | covered | L-051 + L-052 + GP-L-051-b01 + GP-L-052-b01 | submit 写篮；claim 领走 |
| 失败路径 Outbox claim | 领取失败 → 保持待领取 | covered | L-052 + GP-L-052-b01 | 失败不打终态 |
| 主路径 third 出站 | ThirdPartyGateway.execute → 限流/凭证/HTTP/SPI → 记录 | covered | L-064 + GP-L-064-b01 | L-064 |
| 失败路径 third HTTP | 超时/拒绝/凭据缺失 → 失败分类入库 | covered | L-064 + GP-L-064-b01 | L-064 |
| 主路径 工作流请假 | TestLeave.submitAndFlowStart → 任务完成 | covered | L-072 + L-070 + GP-L-072-b01 + GP-L-070-b01 | 申请人首枪被 startCompleteTask 办掉 |
| 失败路径 工作流驳回/终止 | backProcess / terminationTask | covered | L-070 + GP-L-070-b01 | L-070 |
| 主路径 profile 申请 | PersonApplication.submit → admin decide | covered | L-035 + L-034 + GP-L-035-b01 + GP-L-034-b01 | submit 半段 + decide 汇 |
| 失败路径 profile 换绑 | rebind match/confirm 失败不改绑定 | covered | L-036 + GP-L-036-b01 | L-036 |
| 合同顺序 | 后端兼容合同先行，再更新 frontend domain/transport | covered | L-005 + GP-L-005-b01 | L-005 |
| 存储点 MySQL | 业务表只进 10/50 基座 | covered | L-085 + GP-L-085-b01 | 新表进 10；种子/回填进 50；已有库不重放 |

## (a) 函数目的

| 符号 | 模块 | 状态 | 证据 | 备注 |
| --- | --- | --- | --- | --- |
| AuthController.login, logout, register, getClientContext, socialCallback, socialBinding | wta-admin | covered | L-011 + GP-L-011-b01 | 公开认证 HTTP；会话写点在策略（L-013） |
| CaptchaController auth/code, resource/sms/code, resource/email/code | wta-admin | covered | L-012 + GP-L-012-b01 | 验证码入口 |
| SysUserController.list, export, importData, importTemplate, getInfo, remove, optionselect, resetPwd, changeStatus, unlock, authRole, insertAuthRole, deptTree, listByDept | wta-system | covered | L-015 + GP-L-015-b01 | 用户管理；add/edit 在课内函数表 |
| SysUserCredentialController.candidate | wta-system | covered | L-015 + GP-L-015-b01 | 重置候选 |
| SysTemporaryPasswordController.issue | wta-system | covered | L-015 + GP-L-015-b01 | 临时密码 |
| SysRoleController.list, export, getInfo, editPermission, changeStatus, remove, optionselect, allocatedList, unallocatedList, cancelAuthUser, cancelAuthUserAll, selectAuthUserAll, roleDeptTreeselect | wta-system | covered | L-016 + GP-L-016-b01 | 角色；add/edit 在课内 |
| SysMenuController.getRouters, list, getInfo, treeselect, roleMenuTreeselect, remove, cascadeRemove | wta-system | covered | L-016 + GP-L-016-b01 | 菜单/动态路由 |
| SysDeptController.list, excludeChild, getInfo, remove, optionselect | wta-system | covered | L-017 + GP-L-017-b01 | 部门 |
| SysPostController.list, export, getInfo, remove, optionselect, deptTree | wta-system | covered | L-017 + GP-L-017-b01 | 岗位 |
| SysUserTypeController.list, export, getInfo, changeStatus, remove, options, listByUser | wta-system | covered | L-017 + GP-L-017-b01 | 用户类型 |
| SysClientController.list, export, getInfo, add, edit, rotateSsoSecret, bindSso, changeStatus, remove | wta-system | covered | L-018 + GP-L-018-b01 | 终端 Client |
| SysSsoAppController.list, getInfo, add, edit, rotateSecret | wta-system | covered | L-018 + GP-L-018-b01 | SSO 应用登记 |
| SysConfigController.list, export, getInfo, getConfigKey, updateByKey, remove, refreshCache | wta-system | covered | L-021 + GP-L-021-b01 | 参数 |
| SysDictTypeController.list, export, getInfo, remove, refreshCache, optionselect | wta-system | covered | L-021 + GP-L-021-b01 | 字典类型 |
| SysDictDataController.list, export, getInfo, dictType, remove | wta-system | covered | L-021 + GP-L-021-b01 | 字典数据 |
| SysSocialController.list | wta-system | covered | L-022 + GP-L-022-b01 | 社交绑定列表 |
| SysProfileController.profile, updatePwd | wta-system | covered | L-022 + GP-L-022-b01 | 账号资料页（非 profile 模块） |
| CacheController.getInfo | wta-system | covered | L-023 + GP-L-023-b01 | 缓存监控 |
| SysLoginInfoController.list, export, remove, clean, unlock | wta-system | covered | L-023 + GP-L-023-b01 | 登录日志 |
| SysOperlogController.list, export, remove, clean | wta-system | covered | L-023 + GP-L-023-b01 | 操作日志 |
| SysUserOnlineController.list, forceLogout, getInfo, remove | wta-system | covered | L-023 + GP-L-023-b01 | 在线用户 |
| SysOssConfigController.list, getInfo, add, edit, remove, changeStatus | wta-system | covered | L-025 + GP-L-025-b01 | OSS 配置 |
| SysOssController.list, listByIds, downloadUrl, remove | wta-system | covered | L-026 + GP-L-026-b01 | OSS 对象 |
| SysOssUploadController.init, signParts, parts, complete, abort | wta-system | covered | L-027 + GP-L-027-b01 | 直传 |
| SysOssMigrationController.batch, items, dryRun, start, retry, rollback, cleanup | wta-system | covered | L-028 + GP-L-028-b01 | OSS 迁移 |
| SysOpenApiCatalogController.selfInterfaces, selfInterface, userInterfaces, userInterface | wta-system | covered | L-030 + GP-L-030-b01 | OpenAPI 目录 |
| SysOpenApiCredentialController.getSelfCredential, createSelf, resetSelf, enableSelf, disableSelf, deleteSelf, users, getUserCredential, createUser, resetUser, enableUser, disableUser, deleteUser | wta-system | covered | L-031 + GP-L-031-b01 | OpenAPI 凭据 |
| PersonAdminController.page, eligibleUsers, detail, reviewContext, reviewMaterial, material, decide, create, revise, assign, manageBinding, revoke | wta-profile-person | covered | L-034 + GP-L-034-b01 | 管理端个人档案 |
| PersonApplicationController.current, submit | wta-profile-person | covered | L-035 + GP-L-035-b01 | 自助申请 |
| PersonRebindController.probe, match, confirm, submit, unbind | wta-profile-person | covered | L-036 + GP-L-036-b01 | 换绑 |
| PersonMaterialAdminController.list, accessUrl | wta-profile-person | covered | L-037 + GP-L-037-b01 | 管理端材料 |
| PersonMaterialSelfController.attach, detach | wta-profile-person | covered | L-037 + GP-L-037-b01 | 自助材料 |
| MaterialTagController.tree, update, status, archive | wta-profile-person | covered | L-037 + GP-L-037-b01 | 材料标签；课内含 create |
| PersonVerificationAnonymousController.callback | wta-profile-person | covered | L-038 + GP-L-038-b01 | 核身回调 |
| EnterpriseAdminController.page, eligibleUsers, detail, reviewContext, reviewMaterial, material, decide, create, revise, assign, manageBinding, revoke | wta-profile-enterprise | covered | L-040 + GP-L-040-b01 | 管理端企业档案 |
| EnterpriseApplicationController.current, save, submit, probe | wta-profile-enterprise | covered | L-041 + GP-L-041-b01 | 自助申请 |
| EnterpriseVerificationAnonymousController.callback | wta-profile-enterprise | covered | L-041 + GP-L-041-b01 | 核身回调 |
| EnterpriseTransferController.send, confirm, unbind | wta-profile-enterprise | covered | L-042 + GP-L-042-b01 | 企业转移 |
| EnterpriseMaterialAdminController.list, accessUrl | wta-profile-enterprise | covered | L-043 + GP-L-043-b01 | 企业材料管理 |
| EnterpriseMaterialSelfController.attach, detach | wta-profile-enterprise | covered | L-043 + GP-L-043-b01 | 企业材料自助 |
| NotifyNoticeController.list, get, save, publish, retract, remove | wta-notify | covered | L-045 + GP-L-045-b01 | 公告 |
| NotifyNoticeUseCase（与 Impl 同切片） | wta-notify | covered | L-045 + GP-L-045-b01 | layered UseCase |
| NotifyInboxController.list, seen, read, readAll | wta-notify | covered | L-046 + GP-L-046-b01 | 收件箱 |
| NotifyInboxUseCase | wta-notify | covered | L-046 + GP-L-046-b01 |  |
| NotifyConfigController.listAccounts, getAccount, addAccount, editAccount, changeStatus, removeAccount, listScenes, saveBinding, testAccount, testTemplate | wta-notify | covered | L-047 + GP-L-047-b01 | 配置 |
| NotifyConfigUseCase | wta-notify | covered | L-047 + GP-L-047-b01 |  |
| NotifyRecipientController.search, byIds | wta-notify | covered | L-048 + GP-L-048-b01 | 收件人目录 |
| NotifyRecipientUseCase | wta-notify | covered | L-048 + GP-L-048-b01 |  |
| NotificationMonitorController.snapshot, deliveries | wta-notify | covered | L-049 + GP-L-049-b01 | 监控 |
| NotificationMonitorUseCase | wta-notify | covered | L-049 + GP-L-049-b01 |  |
| ProviderCallbackController.callback | wta-notify | covered | L-050 + GP-L-050-b01 | 供应商回调 |
| ProviderCallbackUseCase | wta-notify | covered | L-050 + GP-L-050-b01 |  |
| NotificationController.submit, query, retry, cancel | wta-notify | covered | L-051 + GP-L-051-b01 | 应用通知 API |
| NotificationApplicationUseCase / NotificationApplicationService | wta-notify | covered | L-051 + GP-L-051-b01 | wta-api + UseCase |
| NotifyOutboxClaimUseCase | wta-notify | covered | L-052 + GP-L-052-b01 | 无 HTTP Controller |
| SsoOAuthController.authorize, token, revoke | wta-sso | covered | L-055 + L-056 + GP-L-055-b01 + GP-L-056-b01 | OAuth |
| SsoOAuthUseCase.authorize, exchange, revoke | wta-sso | covered | L-055 + L-056 + GP-L-055-b01 + GP-L-056-b01 |  |
| SsoSessionController.login, session, logout | wta-sso | covered | L-057 + GP-L-057-b01 | SSO 域会话 |
| SsoSessionUseCase | wta-sso | covered | L-057 + GP-L-057-b01 |  |
| SsoClientCatalog, SsoIdentityService | wta-api/sso | covered | L-059 + GP-L-059-b01 | 只读用户与 Client |
| ThirdProviderController.list, get, add, save, status, remove | wta-third | covered | L-060 + GP-L-060-b01 |  |
| ThirdProviderUseCase | wta-third | covered | L-060 + GP-L-060-b01 |  |
| ThirdEndpointController.list, get, add, save, status, remove | wta-third | covered | L-061 + GP-L-061-b01 |  |
| ThirdEndpointUseCase | wta-third | covered | L-061 + GP-L-061-b01 |  |
| ThirdCredentialController.list, add, remove | wta-third | covered | L-062 + GP-L-062-b01 |  |
| ThirdCredentialUseCase | wta-third | covered | L-062 + GP-L-062-b01 |  |
| ThirdObservabilityController.invocation/list, statistics/list | wta-third | covered | L-063 + GP-L-063-b01 |  |
| ThirdObservabilityUseCase | wta-third | covered | L-063 + GP-L-063-b01 |  |
| FlwCategoryController.list, export, getInfo, add, edit, remove, categoryTree | wta-workflow | covered | L-067 + GP-L-067-b01 |  |
| FlwDefinitionController.list, unPublishList, getInfo, add, edit, publish, unPublish, remove, copy, importDef, exportDef, xmlString, active | wta-workflow | covered | L-068 + GP-L-068-b01 |  |
| FlwInstanceController.selectRunningInstanceList, selectFinishInstanceList, getInfo, deleteByBusinessIds, deleteByInstanceIds, deleteHisByInstanceIds, cancelProcessApply, active, pageByCurrent, flowHisTaskList, instanceVariable, updateVariable, invalid | wta-workflow | covered | L-069 + GP-L-069-b01 |  |
| FlwTaskController.startWorkFlow, completeTask, pageByTaskWait, pageByTaskFinish, pageByAllTaskWait, pageByAllTaskFinish, pageByTaskCopy, getTask, getNextNodeList, terminationTask, taskOperation, updateAssignee, backProcess, getBackTaskNode, currentTaskAllUser, urgeTask | wta-workflow | covered | L-070 + GP-L-070-b01 |  |
| FlwSpelController.list, getInfo, add, edit, remove | wta-workflow | covered | L-071 + GP-L-071-b01 |  |
| TestLeaveController.list, export, get, add, submitAndFlowStart, edit, remove | wta-workflow | covered | L-072 + GP-L-072-b01 | 示例流 |
| WorkflowService.deleteInstance, terminateInstance, getBusinessStatusByTaskId, getBusinessStatus, setVariable, instanceVariable, getInstanceIdByBusinessId, startWorkFlow, completeTask, startCompleteTask | wta-api/workflow | covered | L-069 + GP-L-069-b01 | 跨模块合同 |
| TestDemoController.list, page, importData, export, getInfo, add, edit, remove | wta-demo | covered | L-074 + GP-L-074-b01 | 成熟 CRUD |
| TestTreeController.list, export, getInfo, add, edit, remove | wta-demo | covered | L-075 + GP-L-075-b01 | 树表 |
| TestRichTextController.list, create, update, remove, assets, get | wta-demo | covered | L-076 + GP-L-076-b01 | 富文本+OSS |
| SnailAiController.registerCurrentUser | wta-ai | covered | L-080 + GP-L-080-b01 |  |
| AlipayBillTask, WechatBillTask, SummaryBillTask, TestAnnoJobExecutor, TestBroadcastJob, TestClassJobExecutor, TestMapJobAnnotation, TestMapReduceAnnotation1, TestStaticShardingJob | wta-job | covered | L-078 + GP-L-078-b01 | Job 入口不是 HTTP |
| createIdentityAccessService / IdentityAccessService.login, prepareLogin, logout, register, getInfo, getMenus, social* | domains/admin | covered | L-014 + GP-L-014-b01 | 六枪 covered；social* 细节 L-022 |
| createSystemService.users, roles, menus, departments, posts, clients, ssoApps, userTypes, identity, resources | domains/system | covered | L-019 + GP-L-019-b01 | identity 细节 L-014；resources 分给 OSS/OpenAPI 课 |
| createMonitorService | domains/system/monitor | covered | L-024 + GP-L-024-b01 |  |
| createOpenApiService | domains/system | covered | L-033 + GP-L-033-b01 |  |
| createProfileService | domains/profile | covered | L-039 + GP-L-039-b01 | person/self 厨房；enterprise 页面 L-044 |
| createNotificationService, notificationDirectory.searchUsers, usersByIds | domains/notify | covered | L-053 + GP-L-053-b01 |  |
| createThirdService | domains/third | covered | L-065 + GP-L-065-b01 |  |
| createWorkflowDefinitionService | domains/workflow | covered | L-073 + GP-L-073-b01 |  |
| createDemoService, createRichTextService | domains/demo | covered | L-077 + GP-L-077-b01 |  |
| createAiService | domains/ai | covered | L-081 + GP-L-081-b01 |  |
| createAdminWebDomain, createSystemWebDomain, createProfileWebDomain, createNotifyWebDomain, createThirdWebDomain, createWorkflowWebDomain, createDemoWebDomain, createAiWebDomain, createProfileSelfWebDomain | web-domains/* | covered | L-020 + L-039 + L-044 + L-054 + L-066 + L-073 + L-077 + L-081 + GP-L-020-b01 + GP-L-039-b01 + GP-L-044-b01 + GP-L-054-b01 + GP-L-066-b01 + GP-L-073-b01 + GP-L-077-b01 + GP-L-081-b01 | 各切片已讲；admin-web 是 App 组合不是第九个 web-domain 工厂 |
| admin-web application/services.ts 组合点 | apps/admin-web | covered | L-007 + GP-L-007-b01 |  |
| home-web application/services.ts 组合点 | apps/home-web | covered | L-008 + GP-L-008-b01 |  |
| sso-web parseAuthorizeQuery, loginWithPassword, requestAuthorize, fetchSession, apiUrl | apps/sso-web | covered | L-008 + GP-L-008-b01 | PKCE 细节 L-058 |
| createOssUploadClient.upload | adapters/oss-upload-browser | covered | L-029 + GP-L-029-b01 |  |
| UserService, RoleService, DeptService, PostService, ConfigService, OssService, TaskAssigneeService | wta-api/system | covered-by-parent | L-015…L-026 | 跨模块 Java；由 system/OSS 资源课覆盖 |
| CompositeProfileService, ProfileService, PersonIdentityLookupService, ProfileMaterialPort | wta-api/profile | covered-by-parent | L-034…L-043 | 由 profile 资源课覆盖 |
| NotifyDispatcher 之外的 DTO | wta-api/notify | covered-by-parent | L-051 | covered-by-parent of application API |
| BO/VO getter、converter、support 包私有 helper | 各业务模块 | covered-by-parent |  | 由对应资源课覆盖 |
| Mapper XML 中与 Controller 同资源的 SQL | 各业务模块 | covered-by-parent |  | 由对应资源课覆盖，不单独开课 |
| 测试夹具 *Test.java / *.test.ts | 各模块 | deferred | deferred(test-fixture) |  |
| Vite auto-import d.ts、api-contracts generated 快照字节 | frontend | deferred | deferred(generated) |  |
| demo Redis/MQTT/MCP/Excel/ES/WebSocket/限流/加密/敏感/SaToken文档/邮件短信演示/队列 Controller | wta-demo | deferred | deferred(capability-demo-not-product-room) | 模式由 TestDemo covered-by-parent |
| wta-extend 独立进程内部 | wta-extend | deferred | deferred(separate-process) |  |
| WarmFlow/SnailJob/SnailAI 厂商引擎内部 | vendor | deferred | deferred(vendor-engine) |  |
| wta-common-nacos 客户端叠加 | wta-common-nacos | deferred | deferred(optional-overlay) |  |
| 未在主路径点名的 wta-common-* 叶子（mail/sms/mqtt/mcp/elasticsearch/liteflow/excel/encrypt/sensitive/translation/push/job/ai/doc/web 非枢纽） | wta-common | deferred | deferred(not-on-happy-path-hub) | 命中业务切片时由父课带过 |

## (b) 方法性状

| 符号 | 性状 | 状态 | 证据 | 备注 |
| --- | --- | --- | --- | --- |
| AuthController.login | 写 Sa-Token 会话 / 失败不写会话 | covered | L-011 + L-013 + GP-L-011-b01 + GP-L-013-b01 | 门卫失败在 Controller；写会话在策略 |
| AuthController.logout | 删会话 | covered | L-011 + L-013 + GP-L-011-b01 | finally StpUtil.logout |
| AuthController.register | 写用户 + 密码策略；失败不落半用户 | covered | L-011 + L-013 + GP-L-013-b01 | 不写会话；grant 回滚标 unverified |
| IAuthStrategy / SysLoginService | 策略选择、Client 校验、加密登录 | covered | L-013 + GP-L-013-b01 | Client/@ApiEncrypt 在 L-011 |
| PasswordPolicyService | 拒绝弱密码；非纯函数 | covered | L-012 + GP-L-012-b01 | login 不 validateOrThrow |
| SysTemporaryPasswordController.issue | 一次性临时密码；Redis 单次 | covered | L-015 + GP-L-015-b01 | consume 在登录 CAS |
| ISysPermissionService.getRolePermission, getMenuPermission, getDataScopeRoleMap | 读权限快照 | covered | L-016 + GP-L-016-b01 | 登录时建造快照 |
| ISysOssConfigService.updateOssConfigStatus | 切换默认存储 | covered | L-025 + GP-L-025-b01 |  |
| SysOssUploadController.complete / abort | complete 落引用；abort 清票据 | covered | L-027 + GP-L-027-b01 |  |
| SysOpenApiCredentialController.create/reset | 密文存储、列表不回显 | covered | L-031 + GP-L-031-b01 |  |
| OpenApiSigner / OpenApiCanonicalizer | HMAC 签名与规范化；失败关闭 | covered | L-032 + GP-L-032-b01 |  |
| SaTokenOpenApiMachineSessionOperations | 机器会话建立/作废 | covered | L-032 + GP-L-032-b01 |  |
| RedissonOpenApiNonceStore / RedissonOpenApiRateLimiter | nonce 一次性；限流拒绝 | covered | L-032 + GP-L-032-b01 |  |
| PersonRebindController.confirm | 改绑定；失败保持原绑定 | covered | L-036 + GP-L-036-b01 | confirm 冻申请栅栏，失败保持 ACTIVE |
| EnterpriseTransferController.confirm | 改企业归属 | covered | L-042 + GP-L-042-b01 | 当场改 binding |
| NotifyNoticeController.publish/retract | 公告状态机 | covered | L-045 + GP-L-045-b01 | 已发布 skip Publisher；撤回不 cancel |
| NotificationController.submit/retry/cancel | 写 Outbox / 取消 | covered | L-051 + GP-L-051-b01 |  |
| NotifyOutboxClaimUseCase | 领取与唤醒；失败保持待领取 | covered | L-052 + GP-L-052-b01 |  |
| ProviderCallbackController.callback | 幂等回调 | covered | L-050 + GP-L-050-b01 |  |
| NotifyClient.send / NotifyDispatcher.send | 同步渠道入口 | covered | L-082 + GP-L-082-b01 | Bean 口 NotifyClient；MAIL/SMS 才 send |
| SsoOAuthController.authorize | 写授权码；PKCE | covered | L-055 + GP-L-055-b01 |  |
| SsoOAuthController.token | 验 code 发业务令牌 | covered | L-056 + GP-L-056-b01 |  |
| SsoOAuthController.revoke | 作废令牌 | covered | L-056 + GP-L-056-b01 |  |
| SsoSessionController.login/logout | SSO 域 Redis 会话 | covered | L-057 + GP-L-057-b01 |  |
| ThirdPartyGateway.execute | 出站 HTTP、限流、记 invocation | covered | L-064 + GP-L-064-b01 |  |
| ThirdCredential 保存 | 加密；列表不回显 | covered | L-062 + GP-L-062-b01 |  |
| IFlwTaskService.startWorkFlow, completeTask, backProcess, urgeTask, terminationTask | 流程副作用 | covered | L-070 + GP-L-070-b01 |  |
| TestLeaveController.submitAndFlowStart | 写请假并发起流程 | covered | L-072 + GP-L-072-b01 |  |
| IdentityAccessService.login | 写前端 session store | covered | L-014 + GP-L-014-b01 | 解析到票才 setToken |
| createOssUploadClient.upload | 浏览器直传；progress；abort | covered | L-029 + GP-L-029-b01 |  |
| LoginHelper / StpUtil | 会话写入 Redis | covered | L-084 + GP-L-084-b01 | login = StpUtil.login + tokenSession loginUser |
| BaseMapperPlus / QueryBuilder | 不纯查询；DAO 或 classic ServiceImpl 才持有 | covered | L-083 + GP-L-083-b01 | Plus 能写删；QueryBuilder 可开火 |

## 闭合统计

- in-scope 格子总数：175
- covered：163
- deferred：7
- covered-by-parent：5
- uncovered：0

统计只含本表行。同一 Controller 多方法聚合为一行，Lesson 函数表仍要逐方法解释。
