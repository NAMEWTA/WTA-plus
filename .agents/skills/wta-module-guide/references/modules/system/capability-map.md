# wta-system 调用面与事件

条目描述不够明确时，按路径读取源码，不得凭空推断。路径相对工作区，前缀 `wta-vue-plus-namewta/`。

## 目录

1. [模块位置](#模块位置)
2. [稳定跨模块 API（wta-api）](#稳定跨模块-apiwta-api)
3. [模块内 ISys*（仅 system / admin）](#模块内-isys仅-system--admin)
4. [common SPI（system 实现）](#common-spisystem-实现)
5. [HTTP 管理面（仅前端）](#http-管理面仅前端)
6. [事件 / 监听器 / Runner](#事件--监听器--runner)
7. [非稳定面](#非稳定面)

## 模块位置

| 项 | 路径 / 事实 |
|---|---|
| Maven 模块 | `wta-modules/wta-system/pom.xml`（`org.namewta:wta-system`，父模块 `wta-modules`） |
| 直接 POM 依赖方 | 仅 `wta-admin/pom.xml` 声明 `wta-system` |
| 自身依赖 | 仅 common 能力 + `wta-api`，见 `wta-modules/wta-system/pom.xml`：`wta-common-core`、`wta-api`、`wta-common-doc`、`wta-common-mybatis`、`wta-common-translation`、`wta-common-oss`、`wta-common-notify`、`wta-common-log`、`wta-common-excel`、`wta-common-sms`、`wta-common-security`、`wta-common-web`、`wta-common-sensitive`、`wta-common-encrypt`、`wta-common-push`、`wta-common-openapi` |
| 包根 | `wta-modules/wta-system/src/main/java/org/namewta/system/`：`controller/system`、`controller/monitor`、`service`、`service/impl`、`domain`、`domain/bo`、`domain/vo`、`domain/constant`、`mapper`、`event`、`listener`、`runner`、`openapi` |
| Mapper XML | `wta-modules/wta-system/src/main/resources/mapper/system/` |
| 租户 | 无 `tenant` 包或类 |

其他业务模块 POM 不要依赖 `wta-system` 来换取上列 common 能力。

## 稳定跨模块 API（wta-api）

接口在 `wta-api/src/main/java/org/namewta/system/api/`。system 实现类同时实现模块内 `I*Service` 与 api 接口（`TaskAssigneeService` 例外：只实现 api）。调用方式：同一 JVM Spring 注入，无 `@Dubbo` / `@FeignClient`。

| 接口 | 实现 | 方法（以接口为准） |
|---|---|---|
| `UserService.java` | `wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java`（`implements ISysUserService, UserService`） | `selectUserNameById(Long)` / `selectNicknameById(Long)` / `selectPhonenumberById(Long)` / `selectEmailById(Long)` → `String`；`selectNicknameByIds(String)` 逗号分隔 ID → 昵称串；`selectById(Long)` → `UserDTO`；`selectListByIds(Collection<Long>)` → `List<UserDTO>`；`selectUserIdsByRoleIds` → `List<Long>`；`selectUsersByRoleIds` / `selectUsersByDeptIds` / `selectUsersByPostIds` → `List<UserDTO>`；`selectUserNicksByIds` → `Map<Long,String>` |
| `DeptService.java` | `service/impl/SysDeptServiceImpl.java`（`implements ISysDeptService, DeptService`） | `selectDeptNameByIds(String)` 逗号分隔 ID → 名称串；`selectDeptLeaderById(Long)` → 负责人用户 ID；`selectDeptsByList()` → `List<DeptDTO>`；`selectDeptNamesByIds` → `Map<Long,String>` |
| `RoleService.java` | `service/impl/SysRoleServiceImpl.java`（`implements ISysRoleService, RoleService`） | 仅 `selectRoleNamesByIds(Collection<Long>)` → `Map<Long,String>` |
| `PostService.java` | `service/impl/SysPostServiceImpl.java`（`implements ISysPostService, PostService`） | 仅 `selectPostNamesByIds(Collection<Long>)` → `Map<Long,String>` |
| `ConfigService.java` | `service/impl/SysConfigServiceImpl.java`（`implements ISysConfigService, ConfigService`） | `getConfigValue(String)`；接口 default：`getConfigBool/Int/Long/Decimal`；`getConfigMap` → Hutool `Dict`；`getConfigArrayMap` → `List<Dict>`；`getConfigObject(key, Class)`；`getConfigArray(key, Class)`。system 外注入点未找到。 |
| `OssService.java` | `service/impl/SysOssServiceImpl.java`（`implements ISysOssService, OssService`） | `selectUrlByIds(String)` 逗号分隔 ossId → url 串；`selectByIds(String)` → `List<OssDTO>` |
| `NotificationApplicationService.java` | `wta-modules/wta-notify/service/runtime/NotificationApplicationServiceImpl.java` | 统一提交通知意图；支持 `ALL`、`USER`、`PHONE`、`EMAIL` 接收者和 `IN_APP`、`SMS`、`MAIL` 渠道。system 只提供 `UserService` 用户解析，不持有通知实现。 |
| `TaskAssigneeService.java` | `service/impl/SysTaskAssigneeServiceImpl.java`（只实现 api，不实现 `ISys*`） | `selectRoles/Posts/Depts/UsersByTaskAssigneeList(TaskAssigneeBody)` → `TaskAssigneeDTO` |

配套类型：

- DTO：`wta-api/src/main/java/org/namewta/system/api/domain/` — `UserDTO`、`DeptDTO`、`RoleDTO`、`PostDTO`、`OssDTO`、`TaskAssigneeDTO`、`PushPayloadDTO`、`UserOnlineDTO`
- 模型：`wta-api/src/main/java/org/namewta/system/api/model/` — `LoginUser`（含 `userType` / `userTypeId` / `clientPk`）、`XcxLoginUser`、`PasswordLoginBody`、`SmsLoginBody`、`EmailLoginBody`、`SocialLoginBody`、`XcxLoginBody`、`RegisterBody`、`TaskAssigneeBody`

## 模块内 ISys*（仅 system / admin）

路径：`wta-modules/wta-system/src/main/java/org/namewta/system/service/`。主要供 system Controller 与 `wta-admin` 登录链路使用，不是 workflow/demo 等业务模块的稳定合同。未列出的方法以接口源码为准。

| 接口 | 职责要点 |
|---|---|
| `ISysUserService.java` | 分页 `selectPageUserList`；导出 `selectUserExportList`；已/未分配角色用户 `selectAllocatedList` / `selectUnallocatedList`；按用户名/手机/ID 查；唯一性校验；`insertUser` / `registerUser` / `updateUser`；按客户端授权角色 `insertUserAuth(userId, roleIds, clientId)`（只替换该客户端显式角色）；`updateUserProfile` / `resetUserPwd` / `updateUserStatus` |
| `ISysRoleService.java` | 角色 CRUD；`selectRolesByUserId(userId, clientId)` / `selectRolePermissionByUserId(userId, clientId)`；`updateRoleBaseInfo` 与 `updateRolePermission` 分开；踢在线用户 `cleanOnlineUserByRole(Long)` / `cleanOnlineUser(Collection<Long>)` |
| `ISysMenuService.java` | `selectMenuPermsByUserId(userId, clientId)`；`selectMenuTreeByUserId(userId, clientId)`；`buildMenus(List<SysMenu>)` → `List<RouterVo>` |
| `ISysDeptService.java` | 部门树 CRUD；`checkDeptDataScope(Long)` |
| `ISysDictTypeService.java` | 字典类型 CRUD；`selectDictDataByType`；`resetDictCache()`（缓存重置只在此接口） |
| `ISysDictDataService.java` | 字典数据 CRUD；`selectDictLabel(dictType, dictValue)`；无 `resetDictCache` |
| `ISysConfigService.java` | 参数 CRUD、`selectConfigByKey`、`resetConfigCache` |
| `ISysOssService.java` | `queryPageList`、`listByIds`、`getById`、`upload(MultipartFile\|File, SysOssExt)`、`download`、`deleteWithValidByIds` |
| `ISysOssConfigService.java` | 配置 CRUD；启动 `init()`；`updateOssConfigStatus` |
| `ISysNoticeService.java` | 通知公告 CRUD：`selectPageNoticeList` / `insertNotice` / `updateNotice` / `deleteNoticeByIds` |
| `ISysPostService.java` | 岗位 CRUD；`selectPostsByUserId` / `selectPostListByUserId` |
| `ISysClientService.java` | 客户端 CRUD、`queryByClientId(String)` |
| `ISysSocialService.java` | 社交绑定：`queryListByUserId` / `selectByAuthId` / `insertByBo` |
| `ISysPermissionService.java` | `getRolePermission(userId, clientId)` / `getMenuPermission(userId, clientId)`、`getDataScopeRoleMap(List<RoleDTO>)` |
| `ISysDataScopeService.java` | `getRoleCustom(roleId)` / `getDeptAndChild(deptId)` → 逗号分隔部门 id 串 |
| `UserService.java` | `searchActiveUsers`、`selectNotificationUsers`、`selectAllActiveUsers`、`selectUsersByUserTypeIds`；通知模块通过这些公开方法解析并校验接收者。 |
| `ISysOperLogService.java` | 操作日志分页/清空；落库入口是实现类的 `@EventListener recordOper` |
| `ISysLoginInfoService.java` | 登录日志分页/清空；落库入口是实现类的 `@EventListener recordLoginInfo` |
| `ISysUserTypeService.java` | 登录域 CRUD：`queryById` / `queryByCode` / `insertByBo` / `updateByBo` / `options`。实现 `service/impl/SysUserTypeServiceImpl.java` |
| `ISysUserTypeRelService.java` | 用户-登录域：`hasUserType`、`getActiveUserType`、`coverUserTypes`、`grantUserType`、`deleteByUserIds` |

无 `I*` 前缀的内部服务（具体类，跨模块引用会硬耦合 system 实现）：

- `service/ClientSessionService.java`：`kickoutUserType(userId, userTypeCode)`（userId 可空则踢该登录域全部 Token）、`kickoutClient(clientId)`、`kickoutUserClient(userId, clientId)`
- `service/ClientUserTypeAccessService.java`：`requireLoginAccess(userId, SysClientVo)` → 活动 `SysUserTypeVo`；客户端未配登录域、登录域停用、用户无该登录域时抛 `ServiceException`

## common SPI（system 实现）

这些不是 `wta-api`，但由 system 实现、供 common 层调用：

| SPI | 声明 | 实现 |
|---|---|---|
| `DictService` | `wta-common/wta-common-core/src/main/java/org/namewta/common/core/service/DictService.java` | `service/impl/SysDictTypeServiceImpl.java`（`implements ISysDictTypeService, DictService`）。方法：`getDictLabel` / `getDictValue`（含分隔符重载）、`getAllDictByDictType`、`getDictType`、`getDictData` |
| `PermissionService` | `wta-common/wta-common-core/src/main/java/org/namewta/common/core/service/PermissionService.java` | `service/impl/SysPermissionServiceImpl.java`（`implements ISysPermissionService, PermissionService`）。方法：`getRolePermission(userId, clientId)`、`getMenuPermission(userId, clientId)` |
| `SensitiveService` | `wta-common/wta-common-sensitive/src/main/java/org/namewta/common/sensitive/core/SensitiveService.java` | `service/impl/SysSensitiveServiceImpl.java`。`isSensitive(roleKey[], perms[])`：未登录要脱敏；角色/权限命中或超管则不脱敏 |
| `OpenApiAuthorizationResolver` | `wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/spi/OpenApiAuthorizationResolver.java` | `openapi/authorization/SystemOpenApiAuthorizationResolver.java`：只读聚合所有合法 Client 下的有效授权快照 |
| `OpenApiCredentialResolver` | `wta-common/wta-common-openapi/src/main/java/org/namewta/common/openapi/spi/OpenApiCredentialResolver.java` | `openapi/credential/service/SystemOpenApiCredentialResolver.java`：读取启用且未过期凭据并解密一次调用所需 secret |

字典校验：`wta-common/wta-common-core/src/main/java/org/namewta/common/core/validate/dicts/DictPatternValidator.java` 经 `SpringUtils.getBean(DictService.class).getDictLabel(...)` 判断值是否合法。注解 `DictPattern.java` 同目录。

数据权限：`service/impl/SysDataScopeServiceImpl.java` 以 `@Service("sdss")` 注册。`wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/enums/DataScopeType.java` 的 SpEL：

- `CUSTOM`：`#{@sdss.getRoleCustom(#roleId)}`
- `DEPT_AND_CHILD` / `DEPT_AND_CHILD_OR_SELF`：`#{@sdss.getDeptAndChild(#user.deptId)}`

改 Bean 名会静默破坏数据权限 SQL。

## HTTP 管理面（仅前端）

认证登录不在 system，而在 `wta-admin/src/main/java/org/namewta/web/controller/AuthController.java`（`@RequestMapping("/auth")`）。未逐方法枚举全部 REST 动作。

| 前缀 | Controller | 路径 |
|---|---|---|
| `/system/user` | `SysUserController` | `controller/system/SysUserController.java` |
| `/system/user/profile` | `SysProfileController` | `controller/system/SysProfileController.java`（注入 `ISysUserService`） |
| `/system/role` | `SysRoleController` | `controller/system/SysRoleController.java` |
| `/system/menu` | `SysMenuController` | `controller/system/SysMenuController.java` |
| `/system/dept` | `SysDeptController` | `controller/system/SysDeptController.java` |
| `/system/dict/type` | `SysDictTypeController` | `controller/system/SysDictTypeController.java` |
| `/system/dict/data` | `SysDictDataController` | `controller/system/SysDictDataController.java` |
| `/system/config` | `SysConfigController` | `controller/system/SysConfigController.java` |
| `/system/post` | `SysPostController` | `controller/system/SysPostController.java` |
| `/system/client` | `SysClientController` | `controller/system/SysClientController.java` |
| `/system/social` | `SysSocialController` | `controller/system/SysSocialController.java` |
| `/notify/notice` | `NotifyNoticeController` | `wta-notify/controller/admin/NotifyNoticeController.java`（通过公告 UseCase 提交统一通知） |
| `/system/openApi` | `SysOpenApiCredentialController` / `SysOpenApiCatalogController` | `controller/system/openapi/`；本人凭据、管理员目标用户凭据与按目标授权过滤的接口目录 |
| `/system/userType` | `SysUserTypeController` | `controller/system/SysUserTypeController.java` |
| `/resource/oss` | `SysOssController` | `controller/system/SysOssController.java` |
| `/resource/oss/config` | `SysOssConfigController` | `controller/system/SysOssConfigController.java` |
| `/notify/inbox` | `NotifyInboxController` | `wta-modules/wta-notify/controller/admin/NotifyInboxController.java` |
| `/monitor/online` | `SysUserOnlineController` | `controller/monitor/SysUserOnlineController.java`（无 Service 字段；用 Sa-Token `StpUtil` 与 Redis） |
| `/monitor/loginInfo` | `SysLoginInfoController` | `controller/monitor/SysLoginInfoController.java` |
| `/monitor/operlog` | `SysOperlogController` | `controller/monitor/SysOperlogController.java` |
| `/monitor/cache` | `CacheController` | `controller/monitor/CacheController.java`（注入 `RedissonConnectionFactory`，直连 Redis，无 Service） |

## 事件 / 监听器 / Runner

| 类型 | 路径 | 行为 |
|---|---|---|
| `OssConfigChangeEvent` | `event/OssConfigChangeEvent.java` | record；工厂 `save/remove/useDefault`。由 `service/impl/SysOssConfigServiceImpl.java` `publishEvent` |
| `OssConfigChangeListener` | `listener/OssConfigChangeListener.java` | `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` 的 `refreshOssConfig`：刷新 OSS 缓存与 `OssFactory` |
| `OnlineUserCleanEvent` | `event/OnlineUserCleanEvent.java` | record；工厂 `byRole/byUsers`。已索引源码中无 `publishEvent`（工厂方法本身不算发布） |
| `OnlineUserCleanListener` | `listener/OnlineUserCleanListener.java` | `@Async @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` 调 `ISysRoleService.cleanOnlineUser*` |
| `OperLogEvent` | 定义 `wta-common/wta-common-log/src/main/java/org/namewta/common/log/event/OperLogEvent.java`；发布 `wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java`；落库 `service/impl/SysOperLogServiceImpl.java` 的 `recordOper`（`@Async @EventListener`） | 操作日志 |
| `LoginInfoEvent` | 定义 `wta-common/wta-common-log/src/main/java/org/namewta/common/log/event/LoginInfoEvent.java`；由 `wta-admin/src/main/java/org/namewta/web/service/SysLoginService.java`、`SysRegisterService.java` 发布；落库 `service/impl/SysLoginInfoServiceImpl.java` 的 `recordLoginInfo`（`@Async @EventListener`） | 登录日志 |
| `SystemApplicationRunner` | `runner/SystemApplicationRunner.java` | `ApplicationRunner`；启动时 `ossConfigService.init()` |
| 非 Spring 事件 | `listener/SysUserImportListener.java`（Excel `AnalysisEventListener`，内部用 `ISysConfigService.selectConfigByKey("sys.user.initPassword")`）；`listener/DeptExcelConverter.java` | 不要当领域事件 |

## 非稳定面

禁止新业务模块依赖：全部 `controller.*`、`service.impl.*`、`mapper.*`、`resources/mapper/system/*.xml`、`domain`/`bo`/`vo`、`ClientSessionService`、`ClientUserTypeAccessService`、`CacheController`、Excel listener/converter。
