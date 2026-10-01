# 当前账户档案与认证门禁

其他模块通过 `wta-api` 的 `ProfileService` 查询有效档案摘要，通过 `ProfileDisclosureService` 读取明确核准的字段。需要当前登录账户和认证门禁时，使用 `wta-common-satoken` 的 `org.namewta.common.satoken.profile.ProfileAccess`、`ProfileHelper` 和 `profile.annotation` 下的两种注解。

本次没有新增业务模块、数据库表、HTTP 接口或页面，也没有为既有业务接口自动增加实名要求。

## 查询和静态调用

`ProfileAccess` 是可构造器注入的 Spring Bean；`ProfileHelper` 是同名方法的薄静态入口，每次解析当前容器 Bean，不缓存 Bean、用户或认证结果。

```java
ProfileSummary summary = ProfileHelper.currentSummary();
boolean personVerified = summary.personVerified();
boolean enterpriseVerified = summary.enterpriseVerified();

// 同时满足两项；成功返回本次查询的摘要，缺少认证时抛业务异常。
ProfileSummary verified = ProfileHelper.requireVerified(
    ProfileType.PERSON, ProfileType.ENTERPRISE);

// 字段集合由后端业务核准，不能直接透传前端任意字段列表。
ProfileDisclosure details = ProfileHelper.currentDisclosure(EnumSet.of(
    ProfileDisclosureField.ENTERPRISE_NAME,
    ProfileDisclosureField.ENTERPRISE_CREDIT_CODE));
EnterpriseDisclosure enterprise = details.enterprise();
```

`isPersonVerified()` / `isEnterpriseVerified()` 适合仅需单项判断的场景；同一处需要两种状态时读取一次 `currentSummary()`。`requireVerified` 至少指定一种类型，多个类型按全部满足处理，重复类型归一化。

所有当前账户方法都先验证登录，再检查 `LoginUser` 与当前 Token 的身份、登录域及 Client 主键一致。`LoginUser.clientKey` 是 App 键，Token 的 `clientid` 是 OAuth 字符串，不能互相比较或混作 `clientPk`。缺少普通用户上下文、机器身份、过期 Token 或不一致身份都不能用于当前账户访问；请求中的 `userId` 不会替换当前身份。

Controller、安全适配器可以使用当前账户入口。layered 业务的 UseCase/Service 不在方法体中隐式读取会话；后台、异步任务和操作其他账户的用例显式传递已核验的操作者及目标账户，通过既有公共 API 或所属 Gateway/Port 查询。`ProfileService.findByUserIds` 继续提供批量摘要，避免列表循环逐账户查询。

## 声明认证要求

```java
import org.namewta.common.satoken.profile.annotation.RequirePersonVerified;
import org.namewta.common.satoken.profile.annotation.RequireEnterpriseVerified;

@RequirePersonVerified
@RequireEnterpriseVerified
public void submitEnterpriseBusiness(SubmitCommand command) {
    // 正常业务处理，原有权限和目标资源归属校验仍然执行。
}
```

两种注解可标注类或公开实例方法，类、方法、父类和接口声明累加；同时要求两种认证时一次查询摘要后判断。超管不豁免，`@SaIgnore` 不会取消显式档案认证要求。它们不替代 Token、Client、角色权限或目标资源归属校验。不要给实名认证/企业认证申请入口加上相应的“必须已经认证”限制。

注解通过一个 Spring AOP Advisor 执行，实际调用时才解析 `ProfileAccess`，避免代理创建期间提前实例化业务贡献者。必须通过 Spring 代理调用；直接 `new`、静态/私有方法、同类 `this` 自调用不能依赖它。CGLIB 无法拦截 final 方法；JDK 接口代理以实际代理入口为准。后台或异步线程没有当前登录上下文时，使用显式账户 API，不能假定 HTTP 上下文会自动传播。代理边界参考 [Spring 官方说明](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)。

## 认证和数据语义

认证依据仍是绑定 `ACTIVE`、档案 `ACTIVE`、当前版本 `CURRENT`，并且相关记录未删除。首次申请的草稿/待审/退回不等于已认证；已有有效档案时，新申请状态也不会替代有效绑定判断。暂停、解绑、撤销和负责人转移提交后，下一次查询读取当前事实。

企业认证表示当前账户是有效企业档案的负责人。现有模型每个账户最多一个有效企业绑定；它不代表法人或员工身份，也不隐含个人已实名。两项都需要的业务应同时声明两个注解。

现有有效绑定查询不按证件到期日或营业期限自动失效，本次不改变该规则。用户/Client 是否可登录、接口权限和目标业务数据权限继续由各自机制验证。同一本地账户跨 Client 查询认证状态一致；独立业务数据库中的档案不会因共用 SSO 自动同步。

摘要包含账号、有效认证标志及档案 ID、类型、认证时间；详细字段通过既有 `ProfileDisclosure`、`PersonDisclosure`、`EnterpriseDisclosure` 返回。未核准字段保持 null；没有有效绑定时正文为空，已请求的认证字段为 false。未请求某类字段或未装配对应贡献者时，该类投影可为 null。空字段集合不查询档案详情。完整证件与脱敏证件分别授权；材料、历史版本和审核记录不由当前详情入口返回。`sensitive=false` 不代表字段可以随意外发。

没有新增长期认证缓存，也不把认证状态固化进 Token。入口检查不是从检查到业务提交的全程锁；涉及转移、解绑等关键写入，仍在所属业务事务中使用现有锁和版本机制复核。

## 错误与装配兼容

- 未登录：沿用现有登录异常及业务码 401；缺少或不一致的普通账户上下文使用业务码 401，`data.reason=PROFILE_LOGIN_CONTEXT_INVALID`。
- 缺少认证：`ServiceException` 业务码 403，`data.reason` 为 `PERSON_VERIFICATION_REQUIRED`、`ENTERPRISE_VERIFICATION_REQUIRED` 或同时缺失时的 `PROFILE_VERIFICATION_REQUIRED`，并提供所需/缺失类型。
- 查询故障或公共服务返回错误主体：明确失败，不转换成未认证或继续放行。

以上是现有 `R.code` 合同，不表示本次修改全局 HTTP 状态码。认证不足的提示由调用方处理，业务错误不应被当作 Token 过期强制退出。

`ProfileQueryAutoConfiguration` 在中立位置装配 `ProfileService` 与 `ProfileDisclosureService`；分别使用 `@ConditionalOnMissingBean` 允许替换。没有贡献者时保留现有未认证/空投影语义；只装配一个子域时，另一类没有认证证据，门禁拒绝。替代服务的异常继续传播。

这里的零/单子域支持指公共查询组合器可以处理缺失的贡献者，不表示完整业务模块可以无条件单独启动；例如企业业务现有的个人身份、材料等服务依赖仍须由应用装配提供。

个人模块的 `ProfileApiConfiguration` 保留为兼容导入桥，以延迟导入保证应用自定义 `@Bean` 优先；两个旧工厂方法保留签名并标记弃用，不再注册重复 Bean。既有公共 API、DTO、OIDC 字段发布合同不变。无需数据库迁移；回退时先移除新增调用/注解，再回退对应构建。

执行证据见 [worklog.md](worklog.md)。
