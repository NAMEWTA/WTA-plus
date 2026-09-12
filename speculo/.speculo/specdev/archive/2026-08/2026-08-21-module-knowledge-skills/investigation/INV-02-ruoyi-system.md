---
artifact: wayfinder-ticket
id: INV-02
name: ruoyi-system 对外能力与调用方式
parent_map: <Path>{roots.state}/specdev/changes/2026-08-21-module-knowledge-skills/wayfinder-map.md</Path>
label: wayfinder:research
status: closed
blocked_by: []
resolution: answered
---

# ruoyi-system 对外能力与调用方式

## 问题

ruoyi-system 对外公共服务能力是什么，其他模块如何调用？

## Research: ruoyi-system 对外能力地图
- Decision / target: 产出 `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system` 的证据化能力目录：整体用法、其他模块可调用的公共服务、以及实际调用方式。供后续 Skill 使用，本次不创建 Skill。
- Scope / version: 后端子模块 `ruoyi-vue-plus-namewta`（磁盘目录亦可能显示为 `RuoYi-Vue-Plus-namewta`，大小写不敏感文件系统下为同一路径）。根 POM `revision=6.0.0`，Java 21，Spring Boot 4.1.0。源码核对日期 2026-08-21。不含 Dubbo/Feign。不含独立 tenant 模块。
- Stop condition: 能力目录含真实代码路径；至少一条跨模块调用实例；未知项已列出。

### R-001
- Claim: `ruoyi-system` 是 Maven 业务模块 `org.dromara:ruoyi-system`，父模块为 `ruoyi-modules`。它只被可部署应用 `ruoyi-admin` 直接依赖；`ruoyi-workflow` / `ruoyi-demo` / `ruoyi-ai` / `ruoyi-job` / `ruoyi-gen` 的 POM 均不声明 `ruoyi-system`。
- Type: code fact
- Source: `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml`；`ruoyi-vue-plus-namewta/ruoyi-admin/pom.xml`（`<artifactId>ruoyi-system</artifactId>`）；`ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-workflow/pom.xml`、`ruoyi-demo/pom.xml`、`ruoyi-ai/pom.xml`、`ruoyi-job/pom.xml`、`ruoyi-gen/pom.xml`；全仓 `pom.xml` 检索 `ruoyi-system` 仅命中 system 自身、modules 聚合、根 BOM 与 admin。
- Confidence: high
- Limits: 运行时仍由 `ruoyi-admin` 把 system 与其他模块装进同一 Spring 容器，因此 workflow 等可通过 `ruoyi-api` 接口拿到 system 的实现 Bean。
- Artifact impact: Skill 必须把「POM 依赖」和「运行时 Bean 可见」分开写。

### R-002
- Claim: `ruoyi-system` 的 POM 依赖仅 common 能力与 `ruoyi-api`，不依赖其他业务模块。依赖清单：`ruoyi-common-core`、`ruoyi-api`、`ruoyi-common-doc`、`ruoyi-common-mybatis`、`ruoyi-common-translation`、`ruoyi-common-oss`、`ruoyi-common-log`、`ruoyi-common-excel`、`ruoyi-common-sms`、`ruoyi-common-security`、`ruoyi-common-web`、`ruoyi-common-sensitive`、`ruoyi-common-encrypt`、`ruoyi-common-push`。
- Type: code fact
- Source: `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/pom.xml`
- Confidence: high
- Limits: 传递依赖（如 satoken/redis）未在本 POM 显式列出。
- Artifact impact: 其他业务模块不应再依赖 `ruoyi-system` 以换取这些 common 能力。

### R-003
- Claim: `org.dromara.system` 包布局为标准分层，另有 NAMEWTA 登录域扩展。Java 源码按包：`controller/system`、`controller/monitor`、`service`、`service/impl`、`domain`、`domain/bo`、`domain/vo`、`domain/constant`、`mapper`、`event`、`listener`、`runner`。资源层 `src/main/resources/mapper/system/*.xml`。未发现 `tenant` 包或类。
- Type: code fact
- Source: `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/**` 文件枚举（glob 可见约 148 个 Java 文件，另有部分 NAMEWTA 登录域文件可读但未进入该 glob）；`src/main/resources/mapper/system/`；`*Tenant*` glob 结果为 0。
- Confidence: high
- Limits: `ISysUserTypeService`、`SysUserTypeController`、`ClientSessionService`、`ClientUserTypeAccessService` 等登录域文件可直接读取，但部分检索工具未索引它们（推断为 ignore 规则）。Skill 描述不清时必须按路径 Read，不能只依赖 glob。
- Artifact impact: 能力地图按包列出；登录域单独成域，不要写成「租户」。

### R-004
- Claim: HTTP 管理面由 Controller 暴露，前缀分三类：`/system/*`（用户/角色/菜单/部门/字典/配置/岗位/客户端/社交/公告/个人信息/登录域）、`/resource/*`（OSS、OSS 配置、消息盒子）、`/monitor/*`（在线用户、登录日志、操作日志、缓存）。认证登录不在 system controller，而在 `ruoyi-admin` 的 `AuthController`。
- Type: code fact
- Source: 各 Controller `@RequestMapping`：`SysUserController` `/system/user`；`SysRoleController` `/system/role`；`SysMenuController` `/system/menu`；`SysDeptController` `/system/dept`；`SysDictTypeController` `/system/dict/type`；`SysDictDataController` `/system/dict/data`；`SysConfigController` `/system/config`；`SysPostController` `/system/post`；`SysClientController` `/system/client`；`SysSocialController` `/system/social`；`SysNoticeController` `/system/notice`；`SysProfileController` `/system/user/profile`；`SysUserTypeController` `/system/userType`；`SysOssController` `/resource/oss`；`SysOssConfigController` `/resource/oss/config`；`SysMessageController` `/resource/message`；`SysUserOnlineController` `/monitor/online`；`SysLoginInfoController` `/monitor/loginInfo`；`SysOperlogController` `/monitor/operlog`；`CacheController` `/monitor/cache`；`ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java`。
- Confidence: high
- Limits: 未逐方法枚举全部 REST 动作；前端契约以这些前缀为入口。
- Artifact impact: Controller 是前端 HTTP API，不是其他 Java 模块的稳定调用面。

### R-005
- Claim: 跨业务模块的稳定公共服务不在 `org.dromara.system.service.I*`，而在 `ruoyi-api` 的 `org.dromara.system.api.*`。system 实现类同时实现模块内 `I*Service` 与 api 接口。全仓无 `@Dubbo` / `@FeignClient` / `org.apache.dubbo` / OpenFeign。调用方式是同一 JVM 内 Spring 注入接口。
- Type: code fact
- Source: `ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/{User,Dept,Role,Post,Config,Oss,Message,TaskAssignee}Service.java`；实现类：`SysUserServiceImpl implements ISysUserService, UserService`；`SysDeptServiceImpl implements ISysDeptService, DeptService`；`SysRoleServiceImpl implements ISysRoleService, RoleService`；`SysPostServiceImpl implements ISysPostService, PostService`；`SysConfigServiceImpl implements ISysConfigService, ConfigService`；`SysOssServiceImpl implements ISysOssService, OssService`；`SysMessageServiceImpl implements ISysMessageService, MessageService`；`SysTaskAssigneeServiceImpl implements TaskAssigneeService`。Grep `@Dubbo|@FeignClient|org.apache.dubbo|OpenFeign` 无匹配。
- Confidence: high
- Limits: 单体组装假设成立；若拆成微服务，这些接口目前没有远程适配层。
- Artifact impact: Skill 应指导其他模块只依赖 `ruoyi-api` 并注入 `org.dromara.system.api.*`，禁止直接依赖 `ruoyi-system`。

### R-006
- Claim: `org.dromara.system.api` 公共服务接口与关键方法如下（FQN + 方法）。
  1. `org.dromara.system.api.UserService`：`selectUserNameById`、`selectNicknameById`、`selectNicknameByIds`、`selectPhonenumberById`、`selectEmailById`、`selectById`、`selectListByIds`、`selectUserIdsByRoleIds`、`selectUsersByRoleIds`、`selectUsersByDeptIds`、`selectUsersByPostIds`、`selectUserNicksByIds`。
  2. `org.dromara.system.api.DeptService`：`selectDeptNameByIds`、`selectDeptLeaderById`、`selectDeptsByList`、`selectDeptNamesByIds`。
  3. `org.dromara.system.api.RoleService`：`selectRoleNamesByIds`。
  4. `org.dromara.system.api.PostService`：`selectPostNamesByIds`。
  5. `org.dromara.system.api.ConfigService`：`getConfigValue`，以及 default `getConfigBool/Int/Long/Decimal`；`getConfigMap`、`getConfigArrayMap`、`getConfigObject`、`getConfigArray`。
  6. `org.dromara.system.api.OssService`：`selectUrlByIds`、`selectByIds`。
  7. `org.dromara.system.api.MessageService`：`sendMessage(userId, text|payload)`、`sendMessage(text|payload)` 广播、`publishMessage`、`publishAll(text|payload)`。
  8. `org.dromara.system.api.TaskAssigneeService`：`selectRoles/Posts/Depts/UsersByTaskAssigneeList`。
  配套 DTO/模型：`api.domain.{UserDTO,DeptDTO,RoleDTO,PostDTO,OssDTO,TaskAssigneeDTO,PushPayloadDTO,UserOnlineDTO}`；`api.model.{LoginUser,XcxLoginUser,PasswordLoginBody,SmsLoginBody,EmailLoginBody,SocialLoginBody,XcxLoginBody,RegisterBody,TaskAssigneeBody}`。
- Type: code fact
- Source: 上列接口源文件；`ruoyi-vue-plus-namewta/ruoyi-api/src/main/java/org/dromara/system/api/**`
- Confidence: high
- Limits: 方法语义以 JavaDoc 与实现为准；此处不复制实现体。
- Artifact impact: 这是其他子模块应使用的公共 API 清单。

### R-007
- Claim: 模块内 `org.dromara.system.service.I*Service` 是 CRUD/权限管理接口，主要供 system Controller 与 `ruoyi-admin` 登录链路使用，不是给 workflow/demo 等业务模块的稳定合同。接口清单与职责：
  - `ISysUserService`：用户分页/导入导出/注册/改密/按客户端授权角色 `insertUserAuth(userId, roleIds, clientId)`。
  - `ISysRoleService`：角色 CRUD；按 `userId+clientId` 查角色与权限；踢在线用户 `cleanOnlineUserByRole` / `cleanOnlineUser`。
  - `ISysMenuService`：菜单树/路由 `buildMenus`；按 `userId+clientId` 查权限与菜单树。
  - `ISysDeptService`：部门树 CRUD 与数据权限校验。
  - `ISysDictTypeService` / `ISysDictDataService`：字典类型/数据 CRUD 与缓存重置。
  - `ISysConfigService`：参数 CRUD、`selectConfigByKey`、重置缓存。
  - `ISysOssService` / `ISysOssConfigService`：对象存储上传下载与配置 `init()`。
  - `ISysNoticeService`：通知公告 CRUD。
  - `ISysPostService`：岗位 CRUD 与用户岗位。
  - `ISysClientService`：客户端 CRUD、`queryByClientId`。
  - `ISysSocialService`：社交绑定。
  - `ISysPermissionService`：`getRolePermission` / `getMenuPermission(userId, clientId)`、`getDataScopeRoleMap`。
  - `ISysDataScopeService`：`getRoleCustom` / `getDeptAndChild`。
  - `ISysMessageService`：消息盒子与推送存储。
  - `ISysOperLogService` / `ISysLoginInfoService`：操作/登录日志。
  - NAMEWTA：`ISysUserTypeService`、`ISysUserTypeRelService`（登录域及其用户关系）。
- Type: code fact
- Source: `ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/ISys*.java`；`ISysUserTypeService.java`；`ISysUserTypeRelService.java`
- Confidence: high
- Limits: glob 未列出全部登录域接口文件，但 Read 已确认内容。
- Artifact impact: Skill 应标明 I* 仅限 system/admin 内部；业务模块走 `org.dromara.system.api.*`。

### R-008
- Claim: 另有不带 `I*` 前缀的 system 内服务，以及 common 侧由 system 实现的门面：
  - `org.dromara.system.service.ClientSessionService`（`@Service` 具体类）：按登录域/客户端踢 Token。
  - `org.dromara.system.service.ClientUserTypeAccessService`（`@Service` 具体类）：校验用户是否具备客户端要求的登录域。
  - `SysSensitiveServiceImpl implements org.dromara.common.sensitive.core.SensitiveService`。
  - `SysDictTypeServiceImpl implements ISysDictTypeService, org.dromara.common.core.service.DictService`。
  - `SysPermissionServiceImpl implements ISysPermissionService, org.dromara.common.core.service.PermissionService`。
  - `SysDataScopeServiceImpl` 以 Bean 名 `sdss` 注册，供数据权限 SpEL 调用。
- Type: code fact
- Source: `ClientSessionService.java`；`ClientUserTypeAccessService.java`；`service/impl/SysSensitiveServiceImpl.java`；`SysDictTypeServiceImpl.java`；`SysPermissionServiceImpl.java`；`SysDataScopeServiceImpl.java`（`@Service("sdss")`）；`ruoyi-common-core/.../DictService.java`；`PermissionService.java`；`ruoyi-common-sensitive/.../SensitiveService.java`
- Confidence: high
- Limits: `ClientSessionService` / `ClientUserTypeAccessService` 是具体类而非接口，跨模块引用它们会形成对 system 实现的硬耦合。
- Artifact impact: 脱敏、字典翻译、数据权限属于 common 门面 + system 实现；登录域准入属于 NAMEWTA 内部服务。

### R-009
- Claim: 其他子模块调用 `ruoyi-system` 的主路径是 Spring 注入 `org.dromara.system.api.*`（或 common 的 `DictService`），而不是注入 `ISys*Service`。已核实的外部调用方：
  - `ruoyi-workflow`：依赖 `ruoyi-api`；`FlwTaskServiceImpl` 注入 `UserService`；`FlwTaskAssigneeServiceImpl` 注入 `TaskAssigneeService`、`UserService`、`DeptService`、`RoleService`、`PostService`；`SpelRuleComponent` 注入 `DeptService`；另有 `WorkflowGlobalListener` / `WorkflowSideEffectListener` / `TaskOpNotifyComponent` / `FlwChartExtServiceImpl` / `FlwNodeExtServiceImpl` 使用 `UserService` 或 `DictService`。
  - `ruoyi-demo`：`WebSocketController` 注入 `MessageService`。
  - `ruoyi-ai`：`SnailAiController` 只使用 `LoginUser` 模型，不注入 system Service。
  - `ruoyi-common-translation`：依赖 `ruoyi-api`；`UserNameTranslationImpl`/`NicknameTranslationImpl` 注入 `UserService`；`DeptNameTranslationImpl` 注入 `DeptService`；`OssUrlTranslationImpl` 注入 `OssService`；`DictTypeTranslationImpl` 注入 `DictService`。
  - `ruoyi-common-excel`：`ExcelDictConvert` / `ExcelDownHandler` 通过 `SpringUtils.getBean(DictService.class)`。
  - `ruoyi-common-core`：`DictPatternValidator` 取 `DictService`。
  - `ruoyi-common-mybatis`：依赖 `ruoyi-api`；`DataScopeType` SpEL 调用 `@sdss`；`PlusDataPermissionHandler` / `InjectionMetaObjectHandler` 使用 `LoginUser`/`RoleDTO`。
  - `ruoyi-common-satoken`：`SaPermissionImpl`、`LoginHelper` 使用 `LoginUser`。
  - `ruoyi-common-log`：`LogAspect` 使用 `LoginUser` 并发布 `OperLogEvent`。
  - `ruoyi-common-push`：使用 `PushPayloadDTO`，不直接调 system Service。
  - `ruoyi-job` / `ruoyi-gen`：无 `org.dromara.system` 业务调用（gen 仅 `generator.yml` 的默认 `packageName: org.dromara.system`）。
- Type: code fact
- Source: 上列 Java 文件的 import 与字段注入；各模块 `pom.xml`；全仓 `import org.dromara.system.api` 文件列表。
- Confidence: high
- Limits: `ConfigService` 在 system 外未找到调用点；`PostService`/`RoleService` 的外部调用目前集中在 workflow 办理人回显。
- Artifact impact: 「如何被其他模块调用」的标准答案是：依赖 `ruoyi-api`，构造器注入 api 接口。

### R-010
- Claim: `ruoyi-admin` 是唯一在 POM 上依赖 `ruoyi-system` 并穿透注入 `I*Service`、Mapper、domain VO 的模块。登录/注册链路直接使用 system 内部类型：`SysLoginService` 注入 `ISysPermissionService`、`ISysSocialService`、`ISysRoleService`、`ISysDeptService`、`ISysPostService`、`SysUserMapper`；`SysRegisterService` 注入 `ISysUserService`、`ISysClientService`；`AuthController` 注入 `ISysClientService`、`ISysSocialService`、`MessageService`；五个 `*AuthStrategy` 注入 `SysUserMapper` 与 `ClientUserTypeAccessService`，并读取 `SysUser`/`SysUserVo`/`SysClientVo`。
- Type: code fact
- Source: `ruoyi-admin/pom.xml`；`ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java`；`SysRegisterService.java`；`controller/AuthController.java`；`service/impl/PasswordAuthStrategy.java`（及 Sms/Email/Social/Xcx 同类 import）。
- Confidence: high
- Limits: 这是组装层耦合，不是可复制的业务模块范式。新业务模块不应仿效 admin 直接依赖 Mapper。
- Artifact impact: Skill 应把 admin 标为「组装例外」，业务模块禁止照抄。

### R-011
- Claim: 具体跨模块调用实例（workflow → system api）：`FlwTaskServiceImpl` 在抄送时调用 `userService.selectListByIds(...)` 解析抄送人，再交给 `IFlwCommonService.sendMessage`。`FlwTaskAssigneeServiceImpl.fetchTaskAssigneeData` 按办理人类型调用 `taskAssigneeService.selectUsers/Roles/Depts/PostsByTaskAssigneeList`。`SpelRuleComponent.selectDeptLeaderById` 调用 `deptService.selectDeptLeaderById`。demo：`WebSocketController.send` 调用 `messageService.publishAll` 或 `publishMessage`。translation：`UserNameTranslationImpl.translation` 调用 `userService.selectUserNameById`。
- Type: code fact
- Source: `ruoyi-modules/ruoyi-workflow/.../FlwTaskServiceImpl.java` 约 L90、L177；`FlwTaskAssigneeServiceImpl.java` L46–50、L128–131、L262–287；`SpelRuleComponent.java` L25、L34；`ruoyi-modules/ruoyi-demo/.../WebSocketController.java` L27、L44–47；`ruoyi-common-translation/.../UserNameTranslationImpl.java` L25、L36。
- Confidence: high
- Limits: 仅抽样关键调用点，不是每个方法的全量引用图。
- Artifact impact: Skill 示例应使用这些路径，而不是虚构 Feign 调用。

### R-012
- Claim: `ruoyi-common-*` 不是对 system CRUD 的封装层，而是：① 声明可由 system 实现的 SPI（`DictService`、`PermissionService`、`SensitiveService`）；② 通过 `ruoyi-api` 做翻译/数据权限/推送 DTO。数据权限运行时由 MyBatis 插件解析 `DataScopeType` 中的 SpEL `#{@sdss.getRoleCustom(#roleId)}` / `#{@sdss.getDeptAndChild(#user.deptId)}`，间接调用 `ISysDataScopeService`。
- Type: code fact
- Source: `ruoyi-common-core/.../DictService.java`、`PermissionService.java`；`ruoyi-common-mybatis/.../DataScopeType.java` L14–18、L36、L46、L56；`SysDataScopeServiceImpl.java` `@Service("sdss")`；`ruoyi-common-translation/pom.xml` 依赖 `ruoyi-api`。
- Confidence: high
- Limits: SpEL Bean 名 `sdss` 是约定；改名会静默破坏数据权限 SQL。
- Artifact impact: 新增数据权限范围必须保持 `sdss` Bean 名，或同步改枚举模板。

### R-013
- Claim: 事件/监听器/Runner 合同如下。
  1. `org.dromara.system.event.OssConfigChangeEvent`（record）：工厂方法 `save/remove/useDefault`。由 `SysOssConfigServiceImpl` `publishEvent`；`OssConfigChangeListener.refreshOssConfig` 以 `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` 刷新 OSS 缓存与 `OssFactory`。
  2. `org.dromara.system.event.OnlineUserCleanEvent`（record）：工厂 `byRole/byUsers`。`OnlineUserCleanListener.cleanOnlineUser` 以 `@Async @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` 调 `ISysRoleService.cleanOnlineUser*`。在已索引源码中未找到 `publishEvent(OnlineUserCleanEvent...)`。
  3. common-log 事件（定义在 common，落库在 system）：`OperLogEvent` 由 `LogAspect.handleLog` `publishEvent`；`SysOperLogServiceImpl.recordOper` `@Async @EventListener`。`LoginInfoEvent` 由 `SysLoginService`/`SysRegisterService` 发布；`SysLoginInfoServiceImpl.recordLoginInfo` `@Async @EventListener`。
  4. `SystemApplicationRunner` 实现 `ApplicationRunner`，启动时 `ossConfigService.init()`。
  5. 名为 listener 但不是 Spring 事件：`SysUserImportListener`（Excel `AnalysisEventListener`）；`DeptExcelConverter` / `DeptExcelOptions`（Excel 部门转换）。
- Type: code fact（发布点缺失部分见 Unknowns）
- Source: `event/OssConfigChangeEvent.java`；`event/OnlineUserCleanEvent.java`；`listener/OssConfigChangeListener.java`；`listener/OnlineUserCleanListener.java`；`service/impl/SysOssConfigServiceImpl.java` L187/L220/L232；`runner/SystemApplicationRunner.java`；`ruoyi-common-log/.../LogAspect.java` L130；`SysOperLogServiceImpl.java` L44–46；`SysLoginInfoServiceImpl.java` L52–54；`listener/SysUserImportListener.java`；`listener/DeptExcelConverter.java`
- Confidence: high（事件类型与监听器）；medium（OnlineUserCleanEvent 是否已接入发布）
- Limits: 未做运行时事件追踪。
- Artifact impact: OSS 配置变更与日志落库是稳定事件合同；在线用户清理事件目前更像已接线但未找到发布方。

### R-014
- Claim: 本仓库没有独立租户（tenant）实现。NAMEWTA 用「登录域 UserType + Client」替代用户单值 `user_type` 枚举。登录准入必须走 `ClientUserTypeAccessService.requireLoginAccess(userId, client)`；权限与菜单查询必须带客户端主键。
- Type: code fact
- Source: `*Tenant*` glob=0；`ISysUserTypeService.java`；`ISysUserTypeRelService.java`；`SysUserTypeController.java` `/system/userType`；`ClientUserTypeAccessService.java`；`PasswordAuthStrategy.java` L74；`docs/upstream/customization-map.md`「UserType / 登录准入 / 权限」行。
- Confidence: high
- Limits: 上游 RuoYi-Vue-Plus 其他发行版可能有 tenant 模块；本 fork 事实以当前源码为准。
- Artifact impact: Skill 禁止把 tenant 写成 ruoyi-system 能力；应写登录域。

### R-015
- Claim: 下列内容不是稳定跨模块公共 API：① 全部 `controller.*`（HTTP 管理面，带权限注解）；② `service.impl.*` 实现类；③ `mapper.*` 与 `resources/mapper/system/*.xml`；④ `domain`/`domain.bo`/`domain.vo`（admin 登录链路在用，业务模块不应依赖）；⑤ 具体类 `ClientSessionService`、`ClientUserTypeAccessService`；⑥ `CacheController`（直连 Redis，无 Service）；⑦ Excel listener/converter。稳定面是 `ruoyi-api` 的 `org.dromara.system.api.*` 与 common SPI（`DictService`、`PermissionService`、`SensitiveService`）。
- Type: inference（基于依赖方向与现有调用点归纳；分类本身不是编译器强制）
- Source: R-001/R-005/R-009/R-010 的依赖与 import 证据；工程规范 `/.agents/skills/engineering-standards/references/project/01-module-map.md`（`ruoyi-api` 为跨业务合同面）。
- Confidence: high
- Limits: `ruoyi-admin` 已违反「只依赖 api」；这是组装层既成事实，不是新模块许可。
- Artifact impact: Skill 必须明确禁止新代码依赖 I*、Mapper、domain、Controller。

### Conflicts and Unknowns
- 冲突：模块地图写「业务模块可依赖 `ruoyi-api`」，而 `ruoyi-admin` 同时依赖 `ruoyi-system` 并注入 I*/Mapper。按代码事实，admin 是组装应用而非业务子模块；与「其他子模块走 api」不矛盾，但容易被误仿。
- 冲突：`OnlineUserCleanEvent` + `OnlineUserCleanListener` 已存在，但已索引源码中无 `publishEvent`。可能发布代码在未索引的 NAMEWTA 文件中，或事件尚未接线。调用 `ISysRoleService.cleanOnlineUser*` 仍可直接生效。
- 未知：`org.dromara.system.api.ConfigService` 除自身实现外，未找到其他模块注入点；接口存在，外部使用未证实。
- 未知：登录域相关部分文件未被 glob/grep 索引，完整文件集合可能大于 148。描述不清时必须按路径 Read `service/ISysUserType*.java`、`controller/system/SysUserTypeController.java`、`service/Client*.java`。
- 未知：未统计每个 api 方法的全部调用点；R-011 只保证存在代表性路径。
- 未知：未验证拆分成微服务后这些本地 Spring 接口如何暴露；当前仓库无 Dubbo/Feign。
- 未知：`ruoyi-gen` 默认生成包名 `org.dromara.system` 是否仍被项目采用；这是生成器配置，不是运行时调用。

### Recommendation
- 其他子模块调用 ruoyi-system：在模块 POM 加 `ruoyi-api`，Spring 注入 `org.dromara.system.api.{User,Dept,Role,Post,Config,Oss,Message,TaskAssignee}Service`（以及需要时的 `org.dromara.common.core.service.DictService`）。不要依赖 `ruoyi-system`，不要注入 `ISys*`、Mapper 或 domain。
- 用户/部门/岗位/角色名称与列表、OSS URL、消息推送、流程办理人查询走 api 接口；字典翻译走 `DictService`；数据权限走 `@sdss`；登录会话模型走 `LoginUser`。
- HTTP 管理面（`/system` `/resource` `/monitor`）只给前端。认证组装只存在于 `ruoyi-admin`。
- 本 fork 无 tenant；登录隔离能力是 Client + UserType（登录域）。
- 后续 Skill 每条能力必须附仓库路径；描述不够明确时按该路径读取源码，不得凭空推断。
