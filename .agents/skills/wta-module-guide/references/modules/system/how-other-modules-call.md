# 其他模块如何调用 wta-system

条目描述不够明确时，按路径读取源码，不得凭空推断。路径相对工作区，前缀 `wta-vue-plus-namewta/`。

## 目录

1. [标准范式（业务模块）](#标准范式业务模块)
2. [已核实调用方](#已核实调用方)
3. [代表调用（复制前先读源码）](#代表调用复制前先读源码)
4. [组装例外：仅 wta-admin](#组装例外仅-wta-admin)
5. [选型](#选型)

## 标准范式（业务模块）

1. 模块 POM 只加 `wta-api`，不要加 `wta-system`。对照 `wta-modules/wta-workflow/pom.xml`、`wta-modules/wta-demo/pom.xml`。
2. 构造器注入 `org.namewta.system.api.*`（需要字典时再注入 `org.namewta.common.core.service.DictService`）。
3. 运行时由 `wta-admin` 把 system 实现 Bean 装进同一 Spring 容器，接口才能解析。

```xml
<dependency>
    <groupId>org.namewta</groupId>
    <artifactId>wta-api</artifactId>
</dependency>
```

```java
@RequiredArgsConstructor
@Service
public class ExampleService {
    private final UserService userService;
    private final DeptService deptService;
}
```

禁止：Feign/Dubbo 客户端、HTTP 调 `/system`、注入 `ISys*` / Mapper / system `domain`。

## 已核实调用方

| 调用方 | POM | 注入 | 代表路径 |
|---|---|---|---|
| workflow | `wta-modules/wta-workflow/pom.xml` 依赖 `wta-api`，无 `wta-system` | `UserService`、`TaskAssigneeService`、`DeptService`、`RoleService`、`PostService`、`NotificationApplicationService`；部分类用 `DictService` | `wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwTaskServiceImpl.java`（`UserService`）；`.../FlwTaskAssigneeServiceImpl.java`（`TaskAssigneeService` + User/Dept/Role/Post）；流程通知统一走 `NotificationApplicationService`；`.../SpelRuleComponent.java`（`DeptService.selectDeptLeaderById`） |
| demo | `wta-modules/wta-demo/pom.xml` 依赖 `wta-api` | `PushHelper` 仅用于实时演示 | `wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/WebSocketController.java`：在线广播不落通知业务数据 |
| ai | 使用 `LoginUser` 模型 | 不注入 system Service | `wta-modules/wta-ai/src/main/java/org/namewta/ai/controller/SnailAiController.java` |
| job / gen | 无 `org.namewta.system` 业务调用 | — | gen 仅生成器配置默认包名 `org.namewta.system`，不是运行时调用 |
| common-translation | 依赖 `wta-api` | `UserService`、`DeptService`、`OssService`、`DictService` | `wta-common/wta-common-translation/src/main/java/org/namewta/common/translation/core/impl/UserNameTranslationImpl.java`（`selectUserNameById`）；同目录 `NicknameTranslationImpl.java`、`DeptNameTranslationImpl.java`、`OssUrlTranslationImpl.java`、`DictTypeTranslationImpl.java` |
| common-excel | 经 Spring 取 SPI | `DictService` | `wta-common/wta-common-excel/src/main/java/org/namewta/common/excel/convert/ExcelDictConvert.java`；`wta-common/wta-common-excel/src/main/java/org/namewta/common/excel/core/ExcelDownHandler.java`（`SpringUtils.getBean(DictService.class)`） |
| common-core 校验 | 取 SPI | `DictService` | `wta-common/wta-common-core/src/main/java/org/namewta/common/core/validate/dicts/DictPatternValidator.java`（`SpringUtils.getBean(DictService.class).getDictLabel`） |
| common-mybatis | 依赖 `wta-api` | `LoginUser` / `RoleDTO`；SpEL `@sdss` | `wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/handler/PlusDataPermissionHandler.java`；`wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/handler/InjectionMetaObjectHandler.java`；`wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/enums/DataScopeType.java` |
| common-satoken | 模型 | `LoginUser` | `wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/core/service/SaPermissionImpl.java`；`wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java` |
| common-log | 模型 + 事件 | `LoginUser`；发布 `OperLogEvent` | `wta-common/wta-common-log/src/main/java/org/namewta/common/log/aspect/LogAspect.java` |
| common-push | DTO | 不直接调 system Service | `wta-api/src/main/java/org/namewta/system/api/domain/PushPayloadDTO.java` |

`PostService` / `RoleService` 的外部调用目前集中在 workflow 办理人回显。`ConfigService` 外部注入未证实（system 内导入用户用的是 `ISysConfigService.selectConfigByKey`，不是 api）。

## 代表调用（复制前先读源码）

抄送人解析：`FlwTaskServiceImpl` 调 `userService.selectListByIds(...)`，再交给 workflow 的通知适配器构造 `NotificationCommand`；持久化、幂等和渠道投递统一由 `NotificationApplicationService` 负责。

办理人查询：`FlwTaskAssigneeServiceImpl.fetchTaskAssigneeData` 按类型调 `taskAssigneeService.selectUsers/Roles/Depts/PostsByTaskAssigneeList`。

部门负责人：`SpelRuleComponent.selectDeptLeaderById` → `deptService.selectDeptLeaderById`。

实时推送：`WebSocketController.send` → `userId == null` 时 `PushHelper.publishAll(payload)`，否则 `PushHelper.publishMessage(List.of(userId), payload)`；这条路径不替代通知业务合同。

用户名翻译：`UserNameTranslationImpl.translation` → `userService.selectUserNameById`；批量走 `selectListByIds`。

## 组装例外：仅 wta-admin

`wta-admin/pom.xml` 同时依赖 `wta-api` 与 `wta-system`，可穿透注入 `ISys*`、Mapper、domain VO。新业务模块禁止照抄。

| 类 | 路径 | 注入 |
|---|---|---|
| `SysLoginService` | `wta-admin/src/main/java/org/namewta/web/service/SysLoginService.java` | `ISysPermissionService`、`ISysSocialService`、`ISysRoleService`、`ISysDeptService`、`ISysPostService`、`SysUserMapper` |
| `SysRegisterService` | `wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java` | `ISysUserService`、`ISysClientService`、`ISysUserTypeService`、`ISysUserTypeRelService`。注册时按客户端 `userTypeId` 写入登录域关系 |
| `AuthController` | `wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | `ISysClientService`、`ISysSocialService`；`@RequestMapping("/auth")` |
| `PasswordAuthStrategy`（Sms/Email/Social/Xcx 同类） | `wta-admin/src/main/java/org/namewta/web/service/impl/PasswordAuthStrategy.java` | `SysUserMapper`、`ClientUserTypeAccessService`；读取 `SysUser` / `SysUserVo` / `SysClientVo`。登录成功前调 `clientUserTypeAccessService.requireLoginAccess(userId, client)` |

登录准入必须走 `ClientUserTypeAccessService.requireLoginAccess(userId, client)`（admin 策略内）。权限与菜单查询必须带客户端主键。

## 选型

| 需求 | 注入 |
|---|---|
| 用户名/昵称/手机/邮箱、按角色/部门/岗位查用户 | `UserService` |
| 部门名、部门负责人 | `DeptService` |
| 角色名 / 岗位名 | `RoleService` / `PostService` |
| 参数值 | `ConfigService`（外部使用未证实，先读接口再决定） |
| OSS URL | `OssService` |
| 业务通知 | `NotificationApplicationService`；实时广播才使用 `PushHelper` |
| 流程办理人候选 | `TaskAssigneeService` |
| 字典标签/值 | `DictService`（common SPI） |
| 会话身份 | `LoginUser`（不要引入 `SysUser`） |
| 数据权限范围 | 保持 `@sdss`，不要注入 `ISysDataScopeService` |
| 脱敏开关 | 不要注入 `SensitiveService`；由敏感字段注解走 common-sensitive 实现 |
