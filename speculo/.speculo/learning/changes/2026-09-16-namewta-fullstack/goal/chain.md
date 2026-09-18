# Chain: NAMEWTA 前后端全模块

本文件是 Goal 的课程序列投影，不是 C-consolidate，不搬迁 Change。权威 OBJ 仍在 `course.md`。mine unit 切分写在本文件 Units 表，不要另造权威文件。

四门原料课已嵌入 `children/`。「将写入」列是本树内路径。现有文件对本 Goal 仍是 `planned`：必须升级/补前端后经本单元阶段 M 才能改矩阵。

## DAG

```text
Wave 1 (架构与数据流)
  L-001 → L-002 → L-003
  L-001 → L-004
  L-002 + L-004 → L-005 → L-006

Wave 2 (公开 API / 方法性状)
  L-006 → L-011 → L-012
          L-011 → L-013 → L-014
  L-004 → L-007 → L-008
          L-007 → L-009 → L-010
          L-007 → L-014
  L-003 → L-015 → L-016 / L-017 / L-018 → L-019 → L-020
          L-015 → L-022
          L-003 → L-021
          L-013 → L-023 → L-024
  L-005 → L-025 → L-026 → L-027 → L-029
                      L-026 → L-028
          L-005 → L-030 → L-031 → L-032 / L-033
  L-003 → L-034 → L-035 → L-036 / L-038
          L-034 → L-037 → L-039
          L-034 → L-040 → L-041 → L-042 → L-044
                      L-040 → L-043
  L-003 → L-045 → L-046 / L-047 / L-048 / L-049 / L-051
          L-047 → L-050
          L-051 → L-052 → L-082
          L-045 + L-051 → L-053 → L-054
  L-018 → L-055 → L-056 / L-057 → L-058
          L-055 + L-018 → L-059
  L-003 → L-060 → L-061 → L-063 / L-064
          L-060 → L-062
          L-060…L-063 → L-065 → L-066
  L-003 → L-067 → L-068 → L-069 → L-070 → L-072
                      L-068 → L-071
          L-067 + L-070 + L-072 → L-073
  L-003 → L-074 → L-075 / L-076 → L-077
          L-029 → L-076
  L-002 → L-078 → L-079
  L-002 → L-080 → L-081
  L-003 → L-083
  L-013 → L-084
  L-005 → L-085
```

## Lessons

| ID | 标题 | OBJ | 覆盖格子 | 前置 | 估时 | 将写入 | 状态 | 子 Change |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L-001 | 仓库地图：谁拥有什么 | OBJ-01 | C:context, C:repo-ownership | — | 35 | `children/2026-09-14-namewta-architecture/lessons/L-001-repo-map.md` | mined | overview |
| L-002 | 后端组装与依赖方向 | OBJ-02 | C:wta-admin, C:wta-api, C:wta-modules, C:wta-common | L-001 | 35 | `children/2026-09-14-namewta-architecture/lessons/L-002-backend-assembly.md` | revised | overview |
| L-003 | layered vs classic | OBJ-03 | C:layered-classic | L-002 | 35 | `children/2026-09-14-namewta-architecture/lessons/L-003-layered-vs-classic.md` | revised | overview |
| L-004 | 前端所有权与三 App | OBJ-04 | C:admin-web, C:home-web, C:sso-web, C:frontend-direction | L-001 | 35 | `children/2026-09-14-namewta-architecture/lessons/L-004-frontend-ownership.md` | revised | overview |
| L-005 | 公共合同 HTTP/SQL/OpenAPI/wta-api | OBJ-05 | C:public-contracts, D:contract-order | L-002, L-004 | 35 | `children/2026-09-14-namewta-architecture/lessons/L-005-public-contracts.md` | revised | overview |
| L-006 | 主路径与失败路径走查 | OBJ-06 | D:happy-login-query, D:fail-auth | L-003, L-004, L-005 | 35 | `children/2026-09-14-namewta-architecture/lessons/L-006-request-walkthrough.md` | revised | overview |
| L-007 | admin-web 服务组合 | OBJ-07 | A:admin-web.services, C:admin-web-composition | L-004 | 35 | `lessons/L-007-admin-web-composition.md` | mined | platform-frontend |
| L-008 | home-web 与 sso-web 组装差 | OBJ-08 | A:home-web.services, A:sso-web.ssoApi, C:home-web, C:sso-web | L-007 | 35 | `lessons/L-008-home-sso-apps.md` | mined | platform-frontend |
| L-009 | platform 端口 | OBJ-09 | A:platform-http, A:platform-auth, A:platform-permission, A:platform-app-runtime, A:platform-contracts | L-007 | 35 | `lessons/L-009-platform-ports.md` | mined | platform-frontend |
| L-010 | adapters / web-kit / api-contracts | OBJ-10 | A:adapters, A:web-kit, A:api-contracts | L-009 | 35 | `lessons/L-010-adapters-webkit-contracts.md` | revised | platform-frontend |
| L-011 | AuthController 公开入口 | OBJ-11 | A:AuthController.login,logout,register,getClientContext,socialCallback,socialBinding | L-006 | 35 | `lessons/L-011-auth-controller.md` | revised | slice-auth |
| L-012 | Captcha 与密码策略 | OBJ-12 | A:CaptchaController.sms,email,authCode B:PasswordPolicyService | L-011 | 35 | `lessons/L-012-captcha-password-policy.md` | revised | slice-auth |
| L-013 | 登录策略枢纽 | OBJ-13 | B:IAuthStrategy, B:SysLoginService, B:SysRegisterService | L-011 | 35 | `lessons/L-013-auth-strategy.md` | mined | slice-auth |
| L-014 | IdentityAccessService 与会话 | OBJ-14 | A:IdentityAccessService.login,prepareLogin,logout,register,getInfo,getMenus B:session-store | L-011, L-007 | 35 | `lessons/L-014-identity-access-frontend.md` | revised | slice-auth |
| L-015 | SysUser 与凭据 | OBJ-15 | A:SysUserController.* A:SysUserCredentialController.candidate A:SysTemporaryPasswordController.issue | L-003 | 35 | `lessons/L-015-sys-user.md` | mined | module-system |
| L-016 | Role Menu Permission | OBJ-16 | A:SysRoleController.* A:SysMenuController.* B:ISysPermissionService | L-015 | 35 | `lessons/L-016-sys-role-menu.md` | mined | module-system |
| L-017 | Dept Post UserType | OBJ-17 | A:SysDeptController.* A:SysPostController.* A:SysUserTypeController.* | L-015 | 35 | `lessons/L-017-sys-dept-post-usertype.md` | mined | module-system |
| L-018 | Client 与 SsoApp | OBJ-18 | A:SysClientController.* A:SysSsoAppController.* | L-015 | 35 | `lessons/L-018-sys-client-sso-app.md` | mined | module-system |
| L-019 | createSystemService 映射 | OBJ-19 | A:createSystemService.users,roles,menus,departments,posts,clients,userTypes,ssoApps | L-015,L-016,L-017,L-018 | 35 | `lessons/L-019-system-domain-service.md` | mined | module-system |
| L-020 | system web-domain 与导航 host | OBJ-20 | A:createSystemWebDomain B:admin-web navigation host | L-019, L-009 | 35 | `lessons/L-020-system-web-domain.md` | mined | module-system |
| L-021 | Config 与字典 | OBJ-21 | A:SysConfigController.* A:SysDictTypeController.* A:SysDictDataController.* | L-003 | 35 | `lessons/L-021-sys-config-dict.md` | mined | module-system |
| L-022 | Social 与 SysProfile | OBJ-22 | A:SysSocialController.list A:SysProfileController.profile,updatePwd | L-015 | 35 | `lessons/L-022-sys-social-profile.md` | mined | module-system |
| L-023 | system monitor HTTP | OBJ-23 | A:CacheController.getInfo A:SysLoginInfoController.* A:SysOperlogController.* A:SysUserOnlineController.* | L-013 | 35 | `lessons/L-023-sys-monitor.md` | mined | module-system |
| L-024 | monitor 前端 | OBJ-24 | A:createMonitorService A:system web-domain monitor pages | L-023 | 35 | `lessons/L-024-monitor-frontend.md` | mined | module-system |
| L-025 | OSS 配置 | OBJ-25 | A:SysOssConfigController.* B:ISysOssConfigService.updateOssConfigStatus | L-005 | 35 | `lessons/L-025-oss-config.md` | mined | slice-oss |
| L-026 | OSS 对象 | OBJ-26 | A:SysOssController.list,listByIds,downloadUrl,remove B:ISysOssService | L-025 | 35 | `lessons/L-026-oss-object.md` | mined | slice-oss |
| L-027 | OSS 直传票据 | OBJ-27 | A:SysOssUploadController.init,signParts,parts,complete,abort | L-026 | 35 | `lessons/L-027-oss-upload.md` | mined | slice-oss |
| L-028 | OSS 迁移 | OBJ-28 | A:SysOssMigrationController.batch,items,dryRun,start,retry,rollback,cleanup | L-026 | 35 | `lessons/L-028-oss-migration.md` | mined | slice-oss |
| L-029 | OSS 浏览器 adapter | OBJ-29 | A:createOssUploadClient.upload A:useDirectOssUpload | L-027, L-007 | 35 | `lessons/L-029-oss-frontend.md` | mined | slice-oss |
| L-030 | OpenAPI 目录 | OBJ-30 | A:SysOpenApiCatalogController.selfInterfaces,selfInterface,userInterfaces,userInterface | L-005 | 35 | `lessons/L-030-openapi-catalog.md` | mined | slice-openapi |
| L-031 | OpenAPI 凭据 | OBJ-31 | A:SysOpenApiCredentialController.* | L-030 | 35 | `lessons/L-031-openapi-credential.md` | mined | slice-openapi |
| L-032 | common-openapi 网关 | OBJ-32 | B:OpenApi gateway HMAC session | L-031 | 35 | `lessons/L-032-openapi-common.md` | mined | slice-openapi |
| L-033 | OpenAPI 前端 | OBJ-33 | A:createOpenApiService | L-031 | 35 | `lessons/L-033-openapi-frontend.md` | mined | slice-openapi |
| L-034 | Person 管理端档案 | OBJ-34 | A:PersonAdminController.* | L-003 | 35 | `lessons/L-034-person-admin.md` | mined | module-profile |
| L-035 | Person 自助申请 | OBJ-35 | A:PersonApplicationController.current,submit | L-034 | 35 | `lessons/L-035-person-application.md` | mined | module-profile |
| L-036 | Person 换绑 | OBJ-36 | A:PersonRebindController.probe,match,confirm,submit,unbind | L-035 | 35 | `lessons/L-036-person-rebind.md` | mined | module-profile |
| L-037 | Person 材料与标签 | OBJ-37 | A:PersonMaterialAdminController.* A:PersonMaterialSelfController.* A:MaterialTagController.* | L-034 | 35 | `lessons/L-037-person-materials.md` | mined | module-profile |
| L-038 | Person 核身回调 | OBJ-38 | A:PersonVerificationAnonymousController.callback | L-035 | 35 | `lessons/L-038-person-verification.md` | mined | module-profile |
| L-039 | profile person 前端 | OBJ-39 | A:createProfileService A:createProfileWebDomain person/self | L-034,L-035,L-036,L-037 | 35 | `lessons/L-039-profile-person-frontend.md` | mined | module-profile |
| L-040 | Enterprise 管理端档案 | OBJ-40 | A:EnterpriseAdminController.* | L-034 | 35 | `lessons/L-040-enterprise-admin.md` | mined | module-profile |
| L-041 | Enterprise 自助申请 | OBJ-41 | A:EnterpriseApplicationController.current,save,submit,probe A:EnterpriseVerificationAnonymousController.callback | L-040 | 35 | `lessons/L-041-enterprise-application.md` | mined | module-profile |
| L-042 | Enterprise 转移 | OBJ-42 | A:EnterpriseTransferController.send,confirm,unbind | L-041 | 35 | `lessons/L-042-enterprise-transfer.md` | mined | module-profile |
| L-043 | Enterprise 材料 | OBJ-43 | A:EnterpriseMaterialAdminController.* A:EnterpriseMaterialSelfController.* | L-040 | 35 | `lessons/L-043-enterprise-materials.md` | mined | module-profile |
| L-044 | profile enterprise 前端 | OBJ-44 | A:createProfileWebDomain enterprise | L-040,L-041,L-042,L-043 | 35 | `lessons/L-044-profile-enterprise-frontend.md` | mined | module-profile |
| L-045 | notify 公告 | OBJ-45 | A:NotifyNoticeController.* A:NotifyNoticeUseCase.* | L-003 | 35 | `children/2026-09-14-wta-notify/lessons/L-001-notify-notice.md` | mined | domain-notify |
| L-046 | notify 收件箱 | OBJ-46 | A:NotifyInboxController.* A:NotifyInboxUseCase.* | L-045 | 35 | `children/2026-09-14-wta-notify/lessons/L-002-notify-inbox.md` | mined | domain-notify |
| L-047 | notify 配置 | OBJ-47 | A:NotifyConfigController.* A:NotifyConfigUseCase.* | L-045 | 35 | `children/2026-09-14-wta-notify/lessons/L-003-notify-config.md` | mined | domain-notify |
| L-048 | notify 收件人 | OBJ-48 | A:NotifyRecipientController.search,byIds A:NotifyRecipientUseCase.* | L-045 | 35 | `children/2026-09-14-wta-notify/lessons/L-004-notify-recipients.md` | mined | domain-notify |
| L-049 | notify 监控 | OBJ-49 | A:NotificationMonitorController.snapshot,deliveries | L-045 | 35 | `children/2026-09-14-wta-notify/lessons/L-005-notify-monitor.md` | mined | domain-notify |
| L-050 | notify 供应商回调 | OBJ-50 | A:ProviderCallbackController.callback B:ProviderCallbackUseCase | L-047 | 35 | `children/2026-09-14-wta-notify/lessons/L-006-notify-callback.md` | mined | domain-notify |
| L-051 | notify 应用提交 | OBJ-51 | A:NotificationController.submit,query,retry,cancel A:NotificationApplicationService | L-045 | 35 | `children/2026-09-14-wta-notify/lessons/L-007-notify-application.md` | mined | domain-notify |
| L-052 | notify Outbox 领取 | OBJ-52 | B:NotifyOutboxClaimUseCase D:fail-outbox | L-051 | 35 | `children/2026-09-14-wta-notify/lessons/L-008-notify-outbox.md` | mined | domain-notify |
| L-053 | notify domain 前端 | OBJ-53 | A:createNotificationService A:notificationDirectory | L-045,L-051 | 35 | `lessons/L-053-notify-domain-frontend.md` | mined | domain-notify |
| L-054 | notify web-domain | OBJ-54 | A:createNotifyWebDomain | L-053 | 35 | `lessons/L-054-notify-web-domain.md` | mined | domain-notify |
| L-055 | SSO authorize | OBJ-55 | A:SsoOAuthController.authorize A:SsoOAuthUseCase.authorize D:fail-pkce | L-018 | 35 | `children/2026-09-14-wta-sso/lessons/L-001-sso-authorize.md` | mined | domain-sso |
| L-056 | SSO token/revoke | OBJ-56 | A:SsoOAuthController.token,revoke B:SsoOAuthUseCase.exchange,revoke | L-055 | 35 | `children/2026-09-14-wta-sso/lessons/L-002-sso-token-revoke.md` | mined | domain-sso |
| L-057 | SSO 域会话 | OBJ-57 | A:SsoSessionController.login,session,logout | L-055 | 35 | `children/2026-09-14-wta-sso/lessons/L-003-sso-session.md` | mined | domain-sso |
| L-058 | sso-web 认人厅 | OBJ-58 | A:parseAuthorizeQuery A:loginWithPassword A:requestAuthorize A:fetchSession | L-055,L-056,L-057 | 35 | `lessons/L-058-sso-web.md` | mined | domain-sso |
| L-059 | SSO wta-api 目录 | OBJ-59 | A:SsoClientCatalog A:SsoIdentityService | L-055,L-018 | 35 | `lessons/L-059-sso-api-catalog.md` | mined | domain-sso |
| L-060 | third Provider | OBJ-60 | A:ThirdProviderController.list,get,add,save,status,remove | L-003 | 35 | `children/2026-09-14-wta-third/lessons/L-001-third-provider.md` | mined | domain-third |
| L-061 | third Endpoint | OBJ-61 | A:ThirdEndpointController.list,get,add,save,status,remove | L-060 | 35 | `children/2026-09-14-wta-third/lessons/L-002-third-endpoint.md` | mined | domain-third |
| L-062 | third Credential | OBJ-62 | A:ThirdCredentialController.list,add,remove | L-060 | 35 | `children/2026-09-14-wta-third/lessons/L-003-third-credential.md` | mined | domain-third |
| L-063 | third Observability | OBJ-63 | A:ThirdObservabilityController.invocationList,statisticsList | L-061 | 35 | `children/2026-09-14-wta-third/lessons/L-004-third-observability.md` | mined | domain-third |
| L-064 | ThirdPartyGateway | OBJ-64 | B:ThirdPartyGateway.execute D:fail-third-http | L-061,L-062 | 35 | `children/2026-09-14-wta-third/lessons/L-005-third-gateway-spi.md` | mined | domain-third |
| L-065 | third domain 前端 | OBJ-65 | A:createThirdService | L-060,L-061,L-062,L-063 | 35 | `lessons/L-065-third-domain-frontend.md` | mined | domain-third |
| L-066 | third web-domain | OBJ-66 | A:createThirdWebDomain | L-065 | 35 | `lessons/L-066-third-web-domain.md` | mined | domain-third |
| L-067 | workflow 分类 | OBJ-67 | A:FlwCategoryController.* | L-003 | 35 | `lessons/L-067-flw-category.md` | mined | module-workflow |
| L-068 | workflow 定义 | OBJ-68 | A:FlwDefinitionController.* B:IFlwDefinitionService.publish,importJson,removeDef | L-067 | 35 | `lessons/L-068-flw-definition.md` | mined | module-workflow |
| L-069 | workflow 实例 | OBJ-69 | A:FlwInstanceController.* A:WorkflowService.* | L-068 | 35 | `lessons/L-069-flw-instance.md` | mined | module-workflow |
| L-070 | workflow 任务枢纽 | OBJ-70 | A:FlwTaskController.* B:IFlwTaskService.startWorkFlow,completeTask,backProcess,urgeTask,terminationTask | L-069 | 35 | `lessons/L-070-flw-task.md` | mined | module-workflow |
| L-071 | workflow SpEL | OBJ-71 | A:FlwSpelController.* | L-068 | 35 | `lessons/L-071-flw-spel.md` | mined | module-workflow |
| L-072 | 请假示例流 | OBJ-72 | A:TestLeaveController.list,export,get,add,submitAndFlowStart,edit,remove | L-070 | 35 | `lessons/L-072-test-leave.md` | mined | module-workflow |
| L-073 | workflow 前端 | OBJ-73 | A:createWorkflowDefinitionService A:createWorkflowWebDomain | L-067,L-070,L-072 | 35 | `lessons/L-073-workflow-frontend.md` | mined | module-workflow |
| L-074 | TestDemo classic CRUD | OBJ-74 | A:TestDemoController.list,page,importData,export,getInfo,add,edit,remove | L-003 | 35 | `lessons/L-074-test-demo.md` | mined | module-demo |
| L-075 | TestTree 树表 | OBJ-75 | A:TestTreeController.list,export,getInfo,add,edit,remove | L-074 | 35 | `lessons/L-075-test-tree.md` | mined | module-demo |
| L-076 | TestRichText 与 OSS 资产 | OBJ-76 | A:TestRichTextController.list,create,update,remove,assets,get | L-074,L-029 | 35 | `lessons/L-076-test-rich-text.md` | mined | module-demo |
| L-077 | demo 前端 | OBJ-77 | A:createDemoService A:createRichTextService A:createDemoWebDomain | L-074,L-075,L-076 | 35 | `lessons/L-077-demo-frontend.md` | mined | module-demo |
| L-078 | job 执行器入口 | OBJ-78 | A:AlipayBillTask A:WechatBillTask A:SummaryBillTask A:TestAnnoJobExecutor | L-002 | 35 | `lessons/L-078-job-executors.md` | mined | module-job |
| L-079 | job bundle 接线 | OBJ-79 | C:bundle-full-job | L-078 | 35 | `lessons/L-079-job-bundle.md` | mined | module-job |
| L-080 | SnailAi 注册 | OBJ-80 | A:SnailAiController.registerCurrentUser | L-002 | 35 | `lessons/L-080-snail-ai.md` | mined | module-ai |
| L-081 | ai 前端 | OBJ-81 | A:createAiService A:createAiWebDomain | L-080 | 35 | `lessons/L-081-ai-frontend.md` | mined | module-ai |
| L-082 | NotifyDispatcher | OBJ-82 | B:NotifyClient.send B:NotifyDispatcher.send | L-051 | 35 | `lessons/L-082-notify-dispatcher.md` | mined | platform-common |
| L-083 | MyBatis 基类所有权 | OBJ-83 | B:BaseMapperPlus B:QueryBuilder C:dao-mapper-ownership | L-003 | 35 | `lessons/L-083-mybatis-base.md` | mined | platform-common |
| L-084 | Sa-Token 与 Redis 会话 | OBJ-84 | B:LoginHelper B:StpUtil D:fail-session-lost | L-013 | 35 | `lessons/L-084-satoken-redis.md` | mined | platform-common |
| L-085 | MySQL 基座与 @DS | OBJ-85 | C:mysql-base D:happy-store-mysql | L-005 | 35 | `lessons/L-085-mysql-baseline.md` | mined | platform-common |

状态：`planned` | `writing` | `written` | `mined` | `split_proposed` | `split` | `revised` | `deferred`。

## Units

| unit_id | Lessons | 上限 | 状态 |
| --- | --- | --- | --- |
| U1 | L-001, L-002, L-003, L-004, L-005, L-006 | ≤15 | mined |
| U2 | L-007, L-008, L-009, L-010, L-011, L-012, L-013, L-014 | ≤15 | mined |
| U3 | L-015, L-016, L-017, L-018, L-019, L-020, L-021, L-022, L-023, L-024 | ≤15 | mined |
| U4 | L-025, L-026, L-027, L-028, L-029, L-030, L-031, L-032, L-033 | ≤15 | mined |
| U5 | L-034, L-035, L-036, L-037, L-038, L-039, L-040, L-041, L-042, L-043, L-044 | ≤15 | mined |
| U6 | L-045, L-046, L-047, L-048, L-049, L-050, L-051, L-052, L-053, L-054 | ≤15 | mined |
| U7 | L-055, L-056, L-057, L-058, L-059, L-060, L-061, L-062, L-063, L-064, L-065, L-066 | ≤15 | mined |
| U8 | L-067, L-068, L-069, L-070, L-071, L-072, L-073, L-074, L-075, L-076, L-077, L-078, L-079, L-080, L-081 | ≤15 | mined |
| U9 | L-082, L-083, L-084, L-085 | ≤15 | mined |

任何 unit 课数不得超过 15。15 是上界，不是配额。一课不得同时属于两个 unit。后一单元的阶段 T 等前一单元 D 结束，或至少等该课前置已 `mined` / `revised`。U8 恰好 15 节，本 unit 不得再追加 split 子节点；新课进新 unit。

## Waves

| Wave | 焦点 | Lessons | 分组尺 | Gate |
| --- | --- | --- | --- | --- |
| 1 | C4 Context + Container + 主数据流 | L-001, L-002, L-003, L-004, L-005, L-006 | 架构与数据流 | G1/G2/G3 |
| 2 | 范围内公开 API 与方法性状 | L-007, L-008, L-009, L-010, L-011, L-012, L-013, L-014, L-015, L-016, L-017, L-018, L-019, L-020, L-021, L-022, L-023, L-024, L-025, L-026, L-027, L-028, L-029, L-030, L-031, L-032, L-033, L-034, L-035, L-036, L-037, L-038, L-039, L-040, L-041, L-042, L-043, L-044, L-045, L-046, L-047, L-048, L-049, L-050, L-051, L-052, L-053, L-054, L-055, L-056, L-057, L-058, L-059, L-060, L-061, L-062, L-063, L-064, L-065, L-066, L-067, L-068, L-069, L-070, L-071, L-072, L-073, L-074, L-075, L-076, L-077, L-078, L-079, L-080, L-081, L-082, L-083, L-084, L-085 | 公开 API | G1/G2/G3 |

Wave 是覆盖尺上的内容分组，不是挖掘门，也不是并发授权。不改变任何覆盖格子的节点不要写入本表。
