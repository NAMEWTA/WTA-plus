# wta-system 域能力

条目描述不够明确时，按路径读取对应接口与实现，不得凭空推断。路径相对工作区，前缀 `wta-vue-plus-namewta/`。未列出的方法以接口源码为准。

业务模块只使用「跨模块」列；CRUD/权限管理走 ISys* 仅限 system Controller 与 admin。

## 目录

1. [用户 / 角色 / 菜单 / 部门 / 岗位 / 权限](#用户--角色--菜单--部门--岗位--权限)
2. [客户端 / 社交 / 个人信息 / 公告](#客户端--社交--个人信息--公告)
3. [登录域（非租户）](#登录域非租户)
4. [字典 / 参数配置](#字典--参数配置)
5. [OSS / 通知与实时推送](#oss--通知与实时推送)
6. [监控与日志](#监控与日志)
7. [脱敏](#脱敏)

## 用户 / 角色 / 菜单 / 部门 / 岗位 / 权限

### 用户

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/UserService.java` → 按 ID 查账号/昵称/手机/邮箱/`UserDTO`，按角色/部门/岗位查用户。实现 `wta-modules/wta-system/src/main/java/org/dromara/system/service/impl/SysUserServiceImpl.java`。
- 管理面：`service/ISysUserService.java` — 分页、导入导出、注册、改密、`insertUserAuth(userId, roleIds, clientId)`（只替换该客户端显式角色；空 `roleIds` 撤销该客户端全部显式角色）。HTTP `controller/system/SysUserController.java` `/system/user`。
- 翻译样例：`wta-common/wta-common-translation/src/main/java/org/dromara/common/translation/core/impl/UserNameTranslationImpl.java`；`wta-common/wta-common-translation/src/main/java/org/dromara/common/translation/core/impl/NicknameTranslationImpl.java`。

### 角色

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/RoleService.java` 仅 `selectRoleNamesByIds`。实现 `service/impl/SysRoleServiceImpl.java`。
- 管理面：`service/ISysRoleService.java` — CRUD；`selectRolesByUserId(userId, clientId)` / `selectRolePermissionByUserId(userId, clientId)`；基础信息与权限分开更新（`updateRoleBaseInfo` / `updateRolePermission`）；踢在线用户 `cleanOnlineUserByRole` / `cleanOnlineUser`。HTTP `controller/system/SysRoleController.java` `/system/role`。
- 外部名称回显目前集中在 `wta-modules/wta-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskAssigneeServiceImpl.java`。

### 菜单

- 无 wta-api 菜单接口。菜单树与路由只在 system/admin。
- 管理面：`service/ISysMenuService.java` — `selectMenuPermsByUserId(userId, clientId)`、`selectMenuTreeByUserId(userId, clientId)`、`buildMenus`。HTTP `controller/system/SysMenuController.java` `/system/menu`。
- 权限聚合：`service/ISysPermissionService.java` 的 `getMenuPermission(userId, clientId)`；common SPI `PermissionService` 由 `service/impl/SysPermissionServiceImpl.java` 实现（超管会写入 `superadmin` / `*:*:*`，再叠加当前 Client 权限；读实现确认）。

### 部门

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/DeptService.java` — 部门名串、负责人 ID、部门列表、ID→名称 Map。实现 `service/impl/SysDeptServiceImpl.java`。
- 管理面：`service/ISysDeptService.java` — 树 CRUD、`checkDeptDataScope`。HTTP `controller/system/SysDeptController.java` `/system/dept`。
- 样例：`wta-modules/wta-workflow/src/main/java/org/dromara/workflow/rule/SpelRuleComponent.java` 调 `selectDeptLeaderById`；翻译 `wta-common/wta-common-translation/src/main/java/org/dromara/common/translation/core/impl/DeptNameTranslationImpl.java`。

### 岗位

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/PostService.java` 仅 `selectPostNamesByIds`。实现 `service/impl/SysPostServiceImpl.java`。
- 管理面：`service/ISysPostService.java` — CRUD 与用户岗位（`selectPostsByUserId` / `selectPostListByUserId`）。HTTP `controller/system/SysPostController.java` `/system/post`。

### 数据权限

- `service/ISysDataScopeService.java`：`getRoleCustom`、`getDeptAndChild`。
- 实现 `service/impl/SysDataScopeServiceImpl.java` `@Service("sdss")`。注释写明：此 Service 内不允许调用带数据权限注解的方法，否则循环解析。
- SpEL 模板 `wta-common/wta-common-mybatis/src/main/java/org/dromara/common/mybatis/enums/DataScopeType.java`。新增范围必须保持 Bean 名 `sdss`，或同步改枚举模板。

### 流程办理人

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/TaskAssigneeService.java`，实现 `service/impl/SysTaskAssigneeServiceImpl.java`（内部再调 ISysUser/Role/Dept/Post）。调用方 `wta-modules/wta-workflow/src/main/java/org/dromara/workflow/service/impl/FlwTaskAssigneeServiceImpl.java`。

## 客户端 / 社交 / 个人信息 / 公告

| 域 | 管理接口 | HTTP | 说明 |
|---|---|---|---|
| 客户端 | `service/ISysClientService.java`（`queryByClientId(String)`） | `controller/system/SysClientController.java` `/system/client` | 登录组装由 admin 使用；无 wta-api ClientService |
| 社交绑定 | `service/ISysSocialService.java`（`queryListByUserId` / `selectByAuthId`） | `controller/system/SysSocialController.java` `/system/social` | admin `AuthController` / `SysLoginService` 注入 |
| 个人信息 | `ISysUserService` 的 `updateUserProfile` / `resetUserPwd` | `controller/system/SysProfileController.java` `/system/user/profile` | 当前登录用户；Controller 只注入 `ISysUserService` |
| 通知公告 | `wta-notify` 的 `NotifyNoticeUseCase` | `controller/admin/NotifyNoticeController.java` `/notify/notice` | 公告保存、发布、撤回和删除统一经过通知 UseCase 与异步 Outbox |

会话踢出：`service/ClientSessionService.java`（具体类）。在线用户监控 HTTP `controller/monitor/SysUserOnlineController.java` `/monitor/online`，直连 Sa-Token/Redis，无独立 Service 字段。

## 登录域（非租户）

本 fork 无 tenant。NAMEWTA 用「登录域 UserType + Client」替代单值 `user_type` 枚举。不要把登录域写成租户。

| 能力 | 路径 |
|---|---|
| 登录域 CRUD | `service/ISysUserTypeService.java`；实现 `service/impl/SysUserTypeServiceImpl.java`；HTTP `controller/system/SysUserTypeController.java` `/system/userType` |
| 用户-登录域关系 | `service/ISysUserTypeRelService.java`（`hasUserType`、`getActiveUserType`、`coverUserTypes(userId, userTypeIds, grantSource)`、`grantUserType`） |
| 登录准入 | `service/ClientUserTypeAccessService.java` `requireLoginAccess(userId, client)`：校验客户端存在、已配 `userTypeId`、登录域启用、用户拥有该登录域。admin `service/impl/PasswordAuthStrategy.java` 等策略调用 |
| 会话模型 | `wta-api/src/main/java/org/dromara/system/api/model/LoginUser.java` 持有 `userType`（登录域编码）、`userTypeId`、`clientPk` |

部分登录域文件可能不被 glob/grep 索引。描述不清时直接 Read 上列路径，不要只依赖检索。

## 字典 / 参数配置

### 字典

- 前后端回显合同、`dictValue -> dictLabel` 示例、`listClass/cssClass` 规则、缓存和验收清单见 [dictionary-management.md](dictionary-management.md)；涉及字典显示时必须同时读取该文件。
- 跨模块/翻译：注入 `org.namewta.common.core.service.DictService`（`wta-common/wta-common-core/src/main/java/org/dromara/common/core/service/DictService.java`），不是 ISysDict*。实现 `service/impl/SysDictTypeServiceImpl.java`。
- 方法：`getDictLabel` / `getDictValue`、`getAllDictByDictType`、`getDictType`、`getDictData`。
- 管理面：`service/ISysDictTypeService.java`（含 `resetDictCache`）、`ISysDictDataService.java`。HTTP `/system/dict/type`、`/system/dict/data`。
- 使用方：`wta-common/wta-common-translation/src/main/java/org/dromara/common/translation/core/impl/DictTypeTranslationImpl.java`；`wta-common/wta-common-excel/src/main/java/org/dromara/common/excel/convert/ExcelDictConvert.java`；`wta-common/wta-common-excel/src/main/java/org/dromara/common/excel/core/ExcelDownHandler.java`；`wta-common/wta-common-core/src/main/java/org/dromara/common/core/validate/dicts/DictPatternValidator.java`。

### 参数配置

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/ConfigService.java` — `getConfigValue` 及类型化 default 方法、`getConfigMap/ArrayMap/Object/Array`。实现 `service/impl/SysConfigServiceImpl.java`。
- 管理面：`service/ISysConfigService.java`（`selectConfigByKey`、`resetConfigCache`）。HTTP `controller/system/SysConfigController.java` `/system/config`。
- 缺口：system 外未找到 `ConfigService` 注入点。模块内导入用户用 `ISysConfigService.selectConfigByKey("sys.user.initPassword")`：`listener/SysUserImportListener.java`。

## OSS / 通知与实时推送

### OSS

- 跨模块：`wta-api/src/main/java/org/dromara/system/api/OssService.java`。新代码在业务授权后使用 `resolveAccessUrl` 获取无签名到期时间的 PUBLIC 地址，或带 `expiresAt` 的 PRIVATE 短时签名；明确需要私有下载时使用 `presignDownload`，命名策略只能由服务端选择。`selectUrlByIds`、`selectByIds` 已标记为兼容接口，仅供翻译器和存量调用方，不作为新接口范式。
- 引用生命周期：业务表只保存 `ossId`，在保存业务数据的同一个 `@DSTransactional` 中调用 `reconcileReferences`；`snapshot` 用于查看临时状态和真实物理表引用。实现位于 `service/impl/SysOssServiceImpl.java` 与 `oss/service/OssLifecycleManager.java`。
- 管理面：`service/ISysOssService.java` 提供管理查询与删除；HTTP `/resource/oss`。浏览器直传使用 `/resource/oss/uploads` 固定 JSON 协议，客户端只提交服务端策略名和文件元数据。
- 配置面：`ISysOssConfigService.java` 与 HTTP `/resource/oss/config`。必须且只能有一个 PRIVATE 默认配置；PUBLIC_READ 配置必须非默认；`sys_oss.service` 是对象存储路由权威。
- 启动与 readiness：`runner/SystemApplicationRunner.java` 调 `ossConfigService.init()`；readiness 只诊断 Bucket、Policy、域名和 Provider 能力，不创建 Bucket、不修改 Policy，未达到 serving 时拒绝签发访问 URL。
- 配置变更：`event/OssConfigChangeEvent.java` + `listener/OssConfigChangeListener.java`，发布于 `service/impl/SysOssConfigServiceImpl.java`。被对象引用的配置不能通过普通编辑修改 configKey、Bucket 或访问策略，需走受控迁移。
- 翻译兼容：`wta-common/wta-common-translation/src/main/java/org/dromara/common/translation/core/impl/OssUrlTranslationImpl.java` 仍调用旧批量接口；私有 URL 会过期，不得将翻译结果持久化或缓存为资源身份。

### 通知与实时推送

- 业务通知跨模块入口：`wta-api/src/main/java/org/dromara/notify/api/NotificationApplicationService.java`。调用方提交 `NotificationCommand`，由 `wta-notify` 的 UseCase 校验目标、保存意图并通过 Outbox 异步投递。
- 渠道、目标、幂等键和回调合同见 [Notify 模块事实](../notify/index.md)。站内信统一查询 HTTP `/notify/inbox`；不要新建第二套消息盒子或通知收件箱。
- 实时连接仍由 `wta-common-push` 的 `PushHelper`、SSE/WebSocket 适配器承载；它只负责在线事件传输，不负责通知落库、权限或投递状态。

## 监控与日志

| 能力 | 路径 | 说明 |
|---|---|---|
| 在线用户 | `controller/monitor/SysUserOnlineController.java` `/monitor/online` | 无独立 Service；用 Sa-Token 与 Redis |
| 缓存监控 | `controller/monitor/CacheController.java` `/monitor/cache` | 直连 Redis（`RedissonConnectionFactory`），无 Service |
| 操作日志 | `service/ISysOperLogService.java`；HTTP `controller/monitor/SysOperlogController.java` `/monitor/operlog` | 监听 `OperLogEvent`：`service/impl/SysOperLogServiceImpl.java` `recordOper` |
| 登录日志 | `service/ISysLoginInfoService.java`；HTTP `controller/monitor/SysLoginInfoController.java` `/monitor/loginInfo` | 监听 `LoginInfoEvent`：`service/impl/SysLoginInfoServiceImpl.java` `recordLoginInfo` |

在线用户清理事件：`event/OnlineUserCleanEvent.java` + `listener/OnlineUserCleanListener.java`。发布方未在已索引源码中找到；需要踢人时可直接走 `ISysRoleService.cleanOnlineUser*`（仅 system/admin），或 `ClientSessionService` 按登录域/客户端踢 Token。

## 脱敏

- SPI：`wta-common/wta-common-sensitive/src/main/java/org/dromara/common/sensitive/core/SensitiveService.java` `isSensitive(roleKey[], perms[])`。
- 实现：`service/impl/SysSensitiveServiceImpl.java`。未登录返回要脱敏；角色与权限同时配置时需两者都命中才不脱敏；超管（`LoginHelper.isSuperAdmin()`）不脱敏。
- 业务模块不要直接注入该实现；走 common-sensitive 注解链路。
