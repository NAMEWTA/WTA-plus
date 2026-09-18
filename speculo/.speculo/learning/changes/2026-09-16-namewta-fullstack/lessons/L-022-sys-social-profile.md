---
lesson_id: L-022
objective_ids: [OBJ-22]
claimed_cells: [A:SysSocialController.list, A:SysProfileController.profile,updatePwd]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-windows-on-disk
    minutes: 10
  - segment: write-paths-and-side-doors
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L022-01, S-L022-02, S-L022-03, S-L022-04, S-L022-05, S-L022-06, S-L022-07]
---

# Lesson 022：自己的镜子和贴纸本——SysSocial 与 SysProfile

## 学完你能做什么

打开 `wta-system` 里两份 Controller，你能**口述账号资料页的公开窗**，并一口咬定：这是 **system 房间里「我自己」的柜台**，不是 `wta-profile` 那栋档案楼，也不是人事科给别人改锁的窗。

本课认矩阵 (a) 两行，方法名以源码为准：

1. **`SysSocialController`**（`@RequestMapping("/system/social")`）：只有 **`list`**。`GET /system/social/list` → 当前登录者在 `sys_social` 上的绑定行。
2. **`SysProfileController`**（`@RequestMapping("/system/user/profile")`）：矩阵写了 **`profile` / `updatePwd`**。磁盘上还有第三扇 **`updateProfile`**（`PUT /system/user/profile`）。口试按磁盘，不要把漏行背成「资料页不能改昵称/手机/头像」。

OBJ-22 特别点名：**账号资料页，不是 profile 模块。** `wta-profile` 的 Person / Enterprise（L-034 起）是 layered 五层档案；home-web 路由 `/profile` 打的是 `createProfileService`。本课两份 Java 在 `backend/wta-modules/wta-system/.../controller/system/`。

你还要能把三句「改密码 / 贴贴纸 / 看贴纸」拆开，不揉成一个按钮：

- **自己改锁 ≠ 管理员换锁。** `updatePwd` 要旧密码、验菜谱、自己 BCrypt 再写 `sys_user.password`。`SysUserController.resetPwd`（L-015）不要旧密码，权限是 `system:user:resetPwd`。
- **看贴纸本 ≠ 贴/撕贴纸。** `list` 只读当前 `userId`。去第三方门口、回调写入、按主键删除，都在 `AuthController`（L-011）：`GET /auth/binding/{source}`、`POST /auth/social/callback`、`DELETE /auth/unlock/{socialId}`。
- **资料页 GET ≠ 身份口 `getInfo`。** `GET /system/user/profile` 回 `ProfileVo`（`ProfileUserVo` + 角色组/岗位组字符串）。`GET /system/user/getInfo` 回权限集合，给壳用，不在本课格子。

模块模式是 **classic**：`Controller → ISysSocialService / ISysUserService → ServiceImpl → Mapper`。登记表把 `wta-modules/wta-system` 标 classic。磁盘上**没有** UseCase、**没有** DAO。`SysSocialMapper.xml` 是空壳。不要把这两份类口述成 `ProfileUseCase`。

本课不宣称你会拆 `createSystemService` 全表（L-019）、monitor 在线设备（L-023）、OpenAPI 凭据页（L-030 / L-031）、OSS 直传（L-026 / L-029）、`SocialAuthStrategy` 发通行证（L-013），或 Person 档案申请（L-034）。本课要把**两份 Controller 的 HTTP 合同**和**写库/不写库**讲完。

## 先把宏观地图放在桌上

L-015 已经把人事科门口那块「用户」牌子钉死：三份 Java 共用 `/system/user`，资料页被明确划走。L-011 把门厅社交三扇（binding / callback / unlock）讲完，并把 `list` 留给本课。L-012 点名过 `updatePwd` 也走 `PasswordPolicyService.validateOrThrow`，格子在这里。

2026-09-16 工作树：两份类都 `extends BaseController`，都 **没有** `@SaIgnore`、都 **没有** `@SaCheckPermission`。要登录，不要菜单权限串。谁进了大楼，谁就能打自己的镜子和自己的贴纸本。

```text
管理员浏览器  /user/profile（admin-web 个人中心，hidden 路由）
        │
        ├─ GET  /system/user/profile            SysProfileController.profile
        ├─ PUT  /system/user/profile            SysProfileController.updateProfile   ← 矩阵漏写
        ├─ PUT  /system/user/profile/updatePwd  SysProfileController.updatePwd
        └─ GET  /system/social/list             SysSocialController.list
                │
                ├─ ISysUserService              classic；写 sys_user
                └─ ISysSocialService            classic；读 sys_social
                        │
        门厅（不是本课格子，但贴纸本的胶水在那里）
                ├─ GET    /auth/binding/{source}
                ├─ POST   /auth/social/callback     insert/update sys_social
                └─ DELETE /auth/unlock/{socialId}   deleteById
```

| 类 | 磁盘路径 | 公开映射 | 写哪张表？ | 权限注解 |
| --- | --- | --- | --- | --- |
| `SysSocialController` | `.../controller/system/SysSocialController.java` | 1：`list` | **否**（只读） | 无 `@SaCheckPermission` |
| `SysProfileController` | 同目录 sibling | 3：`profile` / `updateProfile` / `updatePwd` | `sys_user`（后两扇） | 无 `@SaCheckPermission` |

**类比：** 大楼里有一面**只照你自己的镜子**（资料页）和一本**只夹你自己贴纸的相册**（社交 list）。镜子可以改发型（昵称/手机/邮箱/性别/头像 OSS id），也可以用**旧钥匙**换锁（`updatePwd`）。相册这一层柜台**只许翻页**；去街上领新贴纸、回来贴上、把某张撕掉，都要走门厅那三扇窗。隔壁还有一栋叫 `wta-profile` 的档案楼，办的是身份证/营业执照，门牌完全不是 `/system/user/profile`。

**类比失效处：**

1. 镜子不是人事科。人事科 `resetPwd` 不需要你出示旧钥匙，还要 `system:user:resetPwd`。
2. 相册不是档案楼。`sys_social` 一行是第三方绑定，不是 Person 档案。
3. 翻相册会把夹层里的**令牌纸条**（`accessToken` 等）一起带出来——`SysSocialVo` 没有 `@JsonIgnore`。前端表格只画 `source` / `avatar` / `userName` / `createTime`，不表示 JSON 里没有令牌。
4. 撕贴纸的窗（`unlockSocial`）按**主键**删，**不**再核对这张贴纸是不是你的。list 按 `userId` 过滤；删除不过同一道滤网。
5. 这不是 layered。不要在嘴里长出 `SysProfileUseCase`。

## 核心概念与机制

### 直觉讲解

先记住四个盒子，再背路径：

- **当前登录者。** 两份 Controller 的主键都不从 URL 来。`LoginHelper.getUserId()` 是唯一「这是谁」的来源。你不能在资料页 URL 里塞别人的 `userId`。
- **未脱敏镜子。** `profile()` 先查出 `SysUserVo`，再 `BeanUtil.toBean` 成 `ProfileUserVo`。注释写明「避免数据被脱敏」：管理端 `SysUserVo` 的邮箱/手机带 `@Sensitive`（缺 `system:user:edit` 会打码）；个人中心必须看见自己的真号码。`ProfileUserVo` **没有** password 字段；密码哈希留在内存里的 `SysUserVo` 上，专给 `updatePwd` 对旧锁用。
- **自己写行要绕数据权限。** `SysUserMapper.update` / `updateById` 带 `@DataPermission`（部门 + `create_by`）。`lambda().updateCount()` 最终走进这扇 `update`。自己改资料/改密若不过 ignore，可能 `rows=0`，信封变成「请联系管理员」。所以 `updateProfile` / `updatePwd` 把写库包进 `DataPermissionHelper.ignore`。`GET profile` 走 `selectVoById`，**没有**这条注解，所以 GET **不**包 ignore。
- **社交写入不在这扇窗。** `ISysSocialService` 磁盘上还有 `insertByBo` / `updateByBo` / `deleteWithValidById` / `selectByAuthId`。本课 Controller **一扇都不映射**。门厅 `socialRegister` 和 `unlockSocial` 才打电话给它们。

「改密」三条走廊，口试必须能指到不同的门牌：

| 走廊 | HTTP | 要旧密码？ | 验菜谱？ | 写 `sys_user.password`？ |
| --- | --- | --- | --- | --- |
| 自己改锁 | `PUT /system/user/profile/updatePwd` | **要**，BCrypt 对不上 → `R.fail("修改密码失败，旧密码错误")` | **要**，在对上旧锁且新旧不同之后 | **是** |
| 管理员换锁 | `PUT /system/user/resetPwd` | 不要 | 要 | 是 |
| 临时纸条 | `POST /system/user/temporaryPassword` | 不要 | 生成时走菜谱 | **否**（Redis） |

`updatePwd` 成功**不**踢 Sa-Token。旧票还能用到过期。单测 `adminAddAndResetValidateBeforeWritingAndNeverKickSessions` 钉的是管理端 reset；资料页同样零 `kickout`。不要把「改密」说成「全设备下线」。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 账号资料页 | account profile (system) | `SysProfileController`，前缀 `/system/user/profile` |
| 档案模块 | profile module | `wta-modules/wta-profile`；Person/Enterprise；**不是本课** |
| 社会化关系 | social binding | 实体 `SysSocial`，表 `sys_social` |
| 贴纸列表 | social list | `GET /system/social/list` → `queryListByUserId(LoginHelper.getUserId())` |
| 未脱敏用户视图 | profile user view | `ProfileUserVo`；含 `avatarUrl` 翻译，不含 password |
| 资料页信封 | `ProfileVo` | record：`user` + `roleGroup` + `postGroup` |
| 资料表单 | profile form | `SysUserProfileBo`：nickName / email / phoneNumber / gender / avatar |
| 自己改锁 | self password change | `updatePwd` + 内部 record `SysUserPasswordBo(oldPassword, newPassword)` |
| 管理员换锁 | admin reset | L-015 的 `resetPwd` |
| 忽略数据权限 | data-permission ignore | `DataPermissionHelper.ignore`；写自己的行时打开，finally 关掉 |
| 传输加密 | `@ApiEncrypt` | 标在 `updatePwd` 上；前端 `headers.isEncrypt = true` |
| 防重复提交 | `@RepeatSubmit` | 默认 5000ms；标在 `updateProfile` / `updatePwd`，**不**标 list / profile GET |
| 菜谱闸 | password policy | `PasswordPolicyService.validateOrThrow`；弱密码 `ServiceException("密码不符合安全策略")`，`setData(violations)` |
| 头像 OSS 对账 | avatar reconcile | `updateUserProfile` 在 `rows>0` 且新 avatar 非空时 `reconcileAvatarReferences` |
| 昵称缓存 | nickname cache | `CacheNames.SYS_NICKNAME` = `"sys_nickname#30d"`；`updateUserProfile` `@CacheEvict`；**改密不驱逐** |
| 经典分层 | classic | Controller → Service → ServiceImpl → Mapper |
| 空 Mapper XML | empty mapper XML | `SysSocialMapper.xml` 只有 namespace；查询走 `BaseMapperPlus` + `lambda()` |

权限串：本课两份 Controller **方法上没有** `system:user:*` / `system:social:*`。登录拦截器过了就能打。不要把「个人中心菜单」说成服务端 `@SaCheckPermission`。

### 机制/因果链

#### 1. 两扇窗在磁盘上的真实映射

文件：

- `.../controller/system/SysSocialController.java`
- `.../controller/system/SysProfileController.java`

类注解两边都是 `@Validated` `@RequiredArgsConstructor` `@RestController`。注入：Social 只注入 `ISysSocialService`；Profile 注入 `ISysUserService` + `PasswordPolicyService`。

**Social 一扇（矩阵第一行，与磁盘一致）：**

| HTTP | 动词 | Java | 写库？ | 本课必须能说的失败/形状 |
| --- | --- | --- | --- | --- |
| `/system/social/list` | GET | `list` | 否 | 无查询参数。`R.ok(List<SysSocialVo>)`。按当前 `userId` 全量列出，无分页 |

**Profile 三扇（矩阵写两扇；第三扇按磁盘补进口试）：**

| HTTP | 动词 | Java | 写库？ | 本课必须能说的失败/副作用 |
| --- | --- | --- | --- | --- |
| `/system/user/profile` | GET | `profile` | 否 | `selectUserById` **不** ignore。用户空 → 下一行 `user.getUserId()` **NPE**（对比 `getInfo` 会 `R.fail("没有权限访问用户数据!")`）。角色组按**当前 Client** 拼角色名；岗位组不按 Client 过滤 |
| `/system/user/profile` | PUT | `updateProfile` | 是 | `@Validated` `SysUserProfileBo`。`userId` 强制覆盖为当前登录者。手机/邮箱非空则查唯一（排除自己）。`ignore` + `updateUserProfile`。`rows>0` → `R.ok`；否则 `R.fail("修改个人信息异常，请联系管理员")` |
| `/system/user/profile/updatePwd` | PUT | `updatePwd` | 是 | `@ApiEncrypt`；`@Log` 请求/响应都不落。旧锁错 / 新旧相同 → `R.fail` **不**调菜谱、**不**写库。弱密码抛策略异常（常变成 业务错误，不是这句 R.fail）。写库同样 `ignore` + 已哈希的 `resetUserPwd`。`rows==0` → `R.fail("修改密码异常，请联系管理员")` |

`updateProfile` / `updatePwd` 带 `@RepeatSubmit`（默认 5000ms）。GET 两扇不带。

前端厨房（格子仍归 L-019，本课只用来对路径）：`createSystemService().users.profile / updateProfile / updatePassword`；社交 list 在 `createSystemResourceService().social.list`，**不**在 `users` 上。解绑走 `identityAccessService.social.unlock` → `/auth/unlock/{id}`。

#### 2. `profile`：镜子怎么拼出来

```text
LoginHelper.getUserId()
    → ISysUserService.selectUserById          无 @DataPermission
    → selectUserRoleGroup(userId)             roleMapper.selectRolesByUserId(..., resolveLoginClientId())
    → selectUserPostGroup(userId)             postMapper.selectPostsByUserId
    → BeanUtil.toBean(SysUserVo, ProfileUserVo)
    → new ProfileVo(profileUser, roleGroup, postGroup)
    → R.ok
```

`selectUserById` 还会填 `roles` 和登录域。这些字段在 `SysUserVo` 上；拷进 `ProfileUserVo` 时对不上的属性会丢掉。`ProfileUserVo` 有 `userId / deptId / userName / nickName / email / phoneNumber / gender / avatar / avatarUrl / loginIp / loginDate / deptName`。页面左侧还读 `createTime`——该字段**不在** `ProfileUserVo` 上，工作树这一枪不会把它带回镜子。

`avatarUrl` 靠 `@Translation(OSS_ID_TO_URL)`。头像列在 `sys_user` 是 **OSS id（bigint）**，不是 URL 字符串。第三方贴纸的 `sys_social.avatar` 才是 **varchar URL**。两列不要念成同一种东西。

#### 3. `updateProfile`：改发型，不改锁

1. Body → `SysUserProfileBo`。`@Xss` 昵称；邮箱 `ValidationFormat.EMAIL`；手机 `MAINLAND_MOBILE`；`@Size` 昵称 ≤30、邮箱 ≤50。`@Sensitive` 在 BO 上，挡的是出站打码，**不是**入站校验。
2. `BeanUtil.toBean` 成 `SysUserBo`，**立刻** `setUserId(LoginHelper.getUserId())`。请求体里就算塞了别人的 id 也会被盖掉。
3. 手机非空且 `!checkPhoneUnique` → `R.fail("修改用户'" + 当前用户名 + "'失败，手机号码已存在")`。邮箱同形。唯一性用 `neIfPresent(userId)`，自己的旧号码能过。
4. `DataPermissionHelper.ignore(() -> updateUserProfile(user))`。
5. Service：`@CacheEvict(SYS_NICKNAME, key="#user.userId")` + `@DSTransactional`。`setIfPresent` 昵称/头像/手机/邮箱/性别。`null` 字段不覆盖；空字符串会写进去（「present」不是「非空白」）。
6. 仅当 `avatar != null` 且 `rows>0`：先读旧头像 id，再 `ossService.reconcileReferences`。基本资料页提交**不带** avatar；头像页只提交 `{ avatar: ossId }`。两枪不会互相清空。

失败：校验注解失败进全局校验；唯一性 `R.fail`；写 0 行 `R.fail` 联系管理员。不踢会话、不改密码列。

#### 4. `updatePwd`：旧钥匙换锁芯

1. `@Validated` record：`oldPassword` / `newPassword` 都 `@NotBlank`。
2. `selectUserById(当前 userId)`——这一枪**不** ignore。读到内存里的哈希（`SysUserVo.password` 有 `@JsonIgnore`，但这里不当 HTTP 往外吐）。
3. `BCrypt.checkpw(old, stored)` 失败 → `R.fail("修改密码失败，旧密码错误")`。菜谱零调用，库零写入。
4. `BCrypt.checkpw(new, stored)` 成功 → `R.fail("新密码不能与旧密码相同")`。仍不调菜谱。
5. `passwordPolicyService.validateOrThrow(newPassword)`。违规抛 `ServiceException("密码不符合安全策略")` 并带 `violations`。单测 `profileChangeRejectsWeakValueBeforeHashingOrWriting`：弱密码在这一步倒下，`resetUserPwd` **never**。
6. `BCrypt.hashpw(newPassword)` 之后才 `ignore(() -> resetUserPwd(userId, hash))`。`resetUserPwd` 只 `set password where userId`，**没有** `@CacheEvict`，**没有** `@DSTransactional`，**没有**踢票。
7. `rows>0` → `R.ok()`；否则联系管理员。

`@Log(title="个人信息", UPDATE, isSaveRequestData=false, isSaveResponseData=false)`。`PasswordCandidateContractUnitTest.candidateAuditNeverPersistsRequestOrResponseBodies` 把这四个 false 钉死。对比管理端 `resetPwd` 是 `excludeParamNames = "password"`，请求体其它字段仍可能进日志。

前端 `resetPwd.vue` 先 `requirePasswordPolicy(getClientContext())` 做本地菜谱，再 `users.updatePassword`；按钮在 `policyState !== 'available'` 时禁用。绕过前端直打后端，第 5 步仍在。

#### 5. `list`：只翻自己的贴纸本

`list()` 一行委托：`socialUserService.queryListByUserId(LoginHelper.getUserId())` → `socialMapper.lambda().eq(SysSocial::getUserId, userId).voList()`。

classic 里屋 `SysSocialServiceImpl` 自己持有 `SysSocialMapper`。XML 空。`validEntityBeforeSave` 是空 TODO，没有唯一约束校验——唯一性闸在门厅 `socialRegister`：`selectByAuthId(source+uuid)` 非空就抛「此三方账号已经被绑定!」；本人同 `source` 已有行则 `updateByBo`。

表 `sys_social`（`10-cde-base-ddl.sql`）有 `del_flag`。Java 实体 **没有** `delFlag`、**没有** `@TableLogic`。`deleteWithValidById` 是 `socialMapper.deleteById`——按工作树是**物理删**，不是用户表那种逻辑删。本课 Controller 不调用删除；门厅 unlock 才调用。口试不要说「资料页能解绑」。

`SysSocialVo` 含 `accessToken` / `refreshToken` / `idToken` 等。list 成功响应把它们放进 JSON。这是合同事实，不是「前端没用就等于没返回」。

#### 6. 门厅侧门（点到为止，格子已在 L-011）

资料页「第三方应用」Tab：`getAuths` 打本课 `list`；点平台走 `bindingUrl`；点解绑走 `unlock(row.id)`。写路径：

- 已登录 + callback → `SysLoginService.socialRegister`（`@Lock4j`）insert 或同人同平台 update。
- 未登录 + 第三方回执 → `POST /auth/login` `grantType=social` → `SocialAuthStrategy`：`selectByAuthId` 空则「你还没有绑定第三方账号，绑定后才可以登录！」。**不**在本课 Controller 发通行证。

`unlockSocial`：`StpUtil.checkLogin()` 后 `deleteWithValidById(socialId)`，**不**查 `userId` 是否等于当前登录者。list 过滤不能当成删除过滤。

### 图、表或文本图

**图 1. 三栋楼，不要走错门牌**

```text
[admin-web /user/profile]          [home-web /profile]           [人事科 /system/user]
  账号资料页（本课）                  档案自助（L-034+）              给别人办用户
        │                                │                              │
        v                                v                              v
 SysProfileController              wta-profile layered            SysUserController
 SysSocialController.list          Person/Enterprise              resetPwd / candidate / issue
        │                                │                              │
        v                                v                              v
   sys_user 自己那一行              profile_* 档案表                 sys_user 任意一行
   sys_social 自己的贴纸            （五层 UseCase）                 Redis 临时纸条
```

- **alt：** 账号资料、档案模块、人事用户管理三套门牌指向三套代码。
- **caption：** 图 1——OBJ-22 的空间关系。本课只占左列。
- **文字等价物：** 个人中心 HTTP 在 `wta-system` 的 `SysProfileController` / `SysSocialController`。home-web 的 `/profile` 是 `wta-profile` 自助档案。人事科改别人密码走 `SysUserController`。三套不要互相冒名。
- **图的边界：** 不画 OSS 直传内部、不画 OpenAPI Tab、不画在线设备。左列 PUT 是真实动词，不要画成「全部 POST」。

**图 2. 改锁因果（成功才往右）**

```text
[已登录]
   │  无会话 → 拦截器挡；本方法无 @SaIgnore
   v
[读自己的哈希] selectUserById(当前 userId)
   │
[对旧钥匙] BCrypt.checkpw(old, stored)
   │  不对 → R.fail 旧密码错误；菜谱不跑；库不动
   v
[新旧不同] BCrypt.checkpw(new, stored) 为真 → R.fail 不能相同
   v
[菜谱] validateOrThrow(new)
   │  弱 → ServiceException 策略；resetUserPwd never
   v
[哈希] BCrypt.hashpw(new)
   v
[ignore 写库] resetUserPwd(userId, hash)
   │  rows==0 → R.fail 联系管理员
   v
 R.ok   旧票仍有效；不驱逐昵称缓存
```

- **alt：** 自己改密必须先对旧锁，再过菜谱，最后 ignore 写哈希；失败在每一步停。
- **caption：** 图 2——`updatePwd` 的闸顺序。管理员 `resetPwd` 没有「旧钥匙」这一格。
- **文字等价物：** 旧密码错或新旧相同都在菜谱之前失败。弱密码在写库之前失败。成功只改 `sys_user.password`，不踢会话。
- **图的边界：** 不画验证码、不画临时 Redis、不画 `PasswordAuthStrategy` 登录对哈希。菜谱内容字段表在 L-012。

**图 3. 贴纸本：看、贴、撕不在同一柜台**

```text
资料页 Tab「第三方应用」
   │
   ├─ 打开页：GET /system/social/list     ← 本课；只读 userId
   ├─ 点 GitHub：GET /auth/binding/github ← L-011；路条
   ├─ 已登录回来：POST /auth/social/callback
   │                 socialRegister → insert 或 update sys_social
   └─ 点解绑：DELETE /auth/unlock/{id}
                     deleteById；不核对归属
```

- **alt：** 列表在 system Controller；绑定与解绑在 AuthController。
- **caption：** 图 3——OBJ-22 的社交格子只有 list。
- **文字等价物：** 个人中心刷新绑定表打 `/system/social/list`。真正改 `sys_social` 的 HTTP 在 `/auth/*`。用社交账号进门还要再走 `POST /auth/login` 且 `grantType=social`。
- **图的边界：** 不把 JustAuth 配置项、state 校验、第三方 HTTP 超时画进本课。`SocialAuthStrategy` 发通行证是 L-013。

### 正例、反例与边界

**正例 A：打开个人中心。** 已登录打 `GET /system/user/profile`。响应 `data.user` 是未脱敏的邮箱/手机，`roleGroup` / `postGroup` 是逗号拼起来的名字。同一次 `onMounted` 再打 `GET /system/social/list` 填第三方 Tab。

**正例 B：改昵称。** `PUT /system/user/profile` `{"nickName":"新名字","phoneNumber":"...","email":"...","gender":"0"}`。`userId` 被覆盖成自己。唯一性过了，ignore 写行，驱逐 `sys_nickname#30d`。密码列不动。

**正例 C：换头像。** 裁剪后 `ossUploadClient.upload(..., policy: 'avatar')` 拿到 OSS id，再 `updateProfile({ avatar: id })`。Service 对账旧 id → 新 id。基本资料那一枪不带 avatar，不会把头像写成 null。

**正例 D：自己改密。** 旧锁对、新旧不同、菜谱过，`ignore` 写 BCrypt。操作日志不存请求体。前端 header `isEncrypt: true`。弱密码单测保证 `resetUserPwd` 不被调用。

**正例 E：只看贴纸。** `list` 返回当前用户全部 `SysSocialVo`。页面用 `source` / `avatar` / `userName` / `createTime` 画表。

**反例 1：** 「`SysProfileController` 在 `wta-profile`。」磁盘在 `wta-system`。`wta-profile` 是 Person/Enterprise 五层。

**反例 2：** 「资料页改密就是 `PUT /system/user/resetPwd`。」那是管理员窗，不要旧密码，要 `system:user:resetPwd`。

**反例 3：** 「矩阵没写 `updateProfile` 所以没有这扇窗。」`@PutMapping` 无 path，和 GET 同门牌不同动词。前端 `users.updateProfile` 和头像页都打它。

**反例 4：** 「`GET /system/social/list` 能解绑。」list 只读。解绑是 `DELETE /auth/unlock/{socialId}`。

**反例 5：** 「两份 Controller 都要 `system:user:edit`。」方法上没有 `@SaCheckPermission`。

**反例 6：** 「`profile()` 也 `DataPermissionHelper.ignore`，和 `getInfo` 一样。」`getInfo` 包了 ignore 且空用户 `R.fail`。`profile()` 不包 ignore；空用户 NPE。

**反例 7：** 「改密会踢掉所有设备 / 作废临时密码 / 清错次。」三件都不做。临时键和解锁键是 L-015 的柜子。

**反例 8：** 「`list` 走分页 `queryPageList`。」没有 page 参数，`voList()` 全量。

**反例 9：** 「`SysSocial` 有 UseCase。」classic ServiceImpl + 空 XML。

**反例 10：** 「home-web `/profile` 打本课 URL。」home 组装的是 `createProfileService`。本课页面在 admin-web `views/system/user/profile/index.vue`，路由 `/user/profile`。

**反例 11：** 「社交 list 的 avatar 和用户头像都是 OSS id。」用户头像是 bigint OSS id；社交 avatar 是第三方 URL 字符串。

**反例 12：** 「`updatePwd` 失败文案只有那两句 R.fail。」校验空密码走 `@NotBlank`；弱密码走 `ServiceException`；写 0 行走第三句联系管理员。

**反例 13：** 「解绑会先确认贴纸属于我。」`deleteWithValidById` 只认主键。

**反例 14：** 「`ISysSocialService.insertByBo` 是本课公开 HTTP。」接口在，Controller 不暴露。

**边界：**

- `SysUserPasswordBo` 是 Controller **内部 record**，不是 `domain.bo` 包里的类。
- `setIfPresent`：只提交 `{avatar}` 时其它列保持。提交空字符串昵称会写入空串。
- 角色组跟当前登录 Client 走；换 Client 再 GET，镜子上的角色名可能变。岗位组不跟 Client。
- `sys_social.del_flag` 在 DDL 里；实体未映射。不要把 unlock 说成逻辑删，除非以后补了 `@TableLogic`。
- 前端 `SocialAuthVO` 类型只有 `id/source/avatar/userName`；运行时 JSON 仍是整份 Vo。
- 个人中心还有「在线设备」「OpenAPI」Tab：分别打 monitor / OpenAPI，不是本课两份类。
- `updateUserProfile` 驱逐昵称缓存；`updatePwd` 不驱逐。翻译昵称的热路径改密后仍可能看到旧名，直到 TTL 或资料更新。
- classic 房间允许 ServiceImpl 持有 Mapper；本课不发动迁 layered。

## 变式与迁移

- **变式 A：用户忘了旧密码。** 资料页 `updatePwd` 走不通。要管理员 `resetPwd` 或临时纸条（L-015）。不要给资料页加「无旧密重置」却仍打 `updatePwd`。
- **变式 B：只换头像。** OSS 直传成功后再 `updateProfile({avatar})`。不要发明 `/system/user/profile/avatar`——工作树没有这扇独立窗。
- **变式 C：手机号被别人占用。** 唯一性在写库前 `R.fail`。ignore 不会让你覆盖别人的行。
- **变式 D：给自己绑 Gitee。** 资料页只负责刷新 list。绑定必须已登录走 callback；用 Gitee 进门必须再走 social grant（L-011 / L-013）。
- **变式 E：怀疑贴纸令牌泄露。** list 的 Vo 带 token。产品若要「列表不回令牌」，要改 Vo/`@JsonIgnore`，不是改前端表格列。本课不擅自改合同。
- **变式 F：解绑后社交登录。** `selectByAuthId` 变空，下一次 `grantType=social` 会听到「还没有绑定」。资料页 list 变空。两件事同一张表，两扇不同的窗。
- **变式 G：数据权限很严的普通用户改自己的资料。** 没有 ignore 时 `update` 可能 0 行。看见「请联系管理员」先问是不是数据权限吃掉了自己这一行，而不是「用户不存在」。
- **迁移口诀：** 先问是不是 `wta-profile` 档案 → 再问是自己的镜子还是人事科 → 改密有没有旧钥匙 → 社交是看、贴还是撕 → 写 `sys_user` 才 ignore。跳步会把三栋楼说成一个按钮。

## 常见误区

1. **「账号资料 = profile 模块。」** 模块名撞车。OBJ-22 括号已经写了「不是 profile 模块」。
2. **「`SysProfileController` 只有矩阵两个方法。」** 磁盘三个。补上 `updateProfile`。
3. **「社交 Controller 能 bind / unbind。」** 只有 `list`。
4. **「资料页 GET 和 `getInfo` 是同一枪。」** 门牌、返回类型、是否 ignore、空用户处理都不同。
5. **「自己改密和管理员重置同一条 Service 方法所以闸一样。」** 都可能调用 `resetUserPwd`；闸在 Controller。一个要对旧锁，一个要权限+`checkUserAllowed`。
6. **「`@Sensitive` 的 Profile BO 会把入库手机打码。」** 打码是出站。入库仍是明文列。
7. **「改密失败都是 `R.fail` 那两句中文。」** 策略异常是另一条。
8. **「list 安全因为只查自己。」** 读路径按 userId；删路径按 id；Vo 含令牌。
9. **「要加 UseCase 才符合规范。」** 登记表 classic；本课不重构。
10. **「前端 `resources.social.list` 属于 `users`。」** 它在 resource service。`users.profile*` 才是资料页。
11. **「`updatePwd` 会 `@CacheEvict` 昵称。」** 只有 `updateUserProfile` 会。
12. **「空 XML 等于没有表。」** 表在基座 DDL；CRUD 在 `BaseMapperPlus`。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 并排打开 `SysSocialController.java` 与 `SysProfileController.java`。把每一个 `@GetMapping` / `@PutMapping` 抄成一张表：路径、动词、Java 名。圈出矩阵没写的 `updateProfile`。确认没有任何 `@SaCheckPermission`。
2. 在 `updatePwd` 用手指划：读哈希 → 旧锁 → 相同拒绝 → `validateOrThrow` → `hashpw` → `ignore` → `resetUserPwd`。对照 `SysUserController.resetPwd`：哪几步没有。
3. 打开 `SysUserMapper.update` 的 `@DataPermission`，再打开 `profile()` 的 `selectUserById`。用一句话说清：为什么写要 ignore、读不要。
4. 打开 `AuthController.unlockSocial` 与 `SysSocialServiceImpl.deleteWithValidById`。再打开 `list` 的 `eq(userId)`。把「读过滤」和「删过滤」是不是同一道闸说出来。
5. 打开空的 `SysSocialMapper.xml`、实体 `@TableName("sys_social")`、登记表 classic 行、`wta-profile` 目录。用一句话连接：为什么本课不许把这两份类叫成 profile 模块，也不许发明 UseCase。

## 总结、词汇表与下一步

- **两份 system Controller，一面自己的镜子，一本自己的贴纸本。** Social 一扇只读 list；Profile 三扇：GET 镜子、PUT 改资料、PUT 带旧钥匙改密。classic：Controller → ServiceImpl → Mapper（Social XML 空）。
- **认格子：** `A:SysSocialController.list`；`A:SysProfileController.profile,updatePwd`。口试额外补磁盘上的 `updateProfile`。
- **不是 profile 模块，不是人事 resetPwd，不是门厅 bind/unlock。**
- **写自己的 `sys_user` 行要 `DataPermissionHelper.ignore`。** GET 镜子不 ignore；空用户会 NPE。
- **`updatePwd` 闸序：** 旧锁 → 不能相同 → 菜谱 → 哈希 → ignore 写列。不踢票。日志不存体。
- **社交写入在 `/auth/*`。** list 可能带回令牌字段；删除按主键。

词汇表：SysSocial / SysProfile / `sys_social` / `sys_user` / ProfileUserVo / ProfileVo / SysUserProfileBo / SysUserPasswordBo / `updatePwd` / `updateProfile` / `queryListByUserId` / `DataPermissionHelper.ignore` / `@ApiEncrypt` / `@RepeatSubmit` / `validateOrThrow` / `resetUserPwd` / `SYS_NICKNAME` / classic / wta-profile（对照物）.

下一步：L-019 指着 `users.profile / updateProfile / updatePassword` 和 `resources.social.list` 把 URL 接到前端厨房。L-023 才走个人中心「在线设备」。L-034 才走进真正的 profile 模块。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 房间内公开入口；本课两份 Controller 同模块，不在 wta-profile | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记为 classic；`wta-profile` 为 layered；禁止混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql` | `sys_social` 列（含 access_token、del_flag）；`sys_user.avatar` 为 bigint、`password` 为 varchar | `create table sys_social` / `sys_user` | 2026-09-16 |
| S-L022-01 | `SysSocialController.java` | 唯一映射 `GET /list`；无权限注解；委托 `queryListByUserId(LoginHelper.getUserId())` | 类与 `list` | 2026-09-16 |
| S-L022-02 | `SysProfileController.java` | 三扇：GET `/`、PUT `/`、PUT `/updatePwd`；ignore；旧锁/菜谱/`@ApiEncrypt`/`@Log` 不存体；内部 record | 类与各方法 | 2026-09-16 |
| S-L022-03 | `ISysSocialService.java`、`SysSocialServiceImpl.java`、`SysSocialMapper.java`、`SysSocialMapper.xml`、`SysSocial.java`、`SysSocialVo.java` | classic 持有 Mapper；list 按 userId；insert/update/delete 供门厅；空 XML；实体无 `@TableLogic`；Vo 含令牌字段 | 接口/实现/空 mapper/实体 | 2026-09-16 |
| S-L022-04 | `ISysUserService` / `SysUserServiceImpl.updateUserProfile` / `resetUserPwd` / `SysUserProfileBo` / `ProfileUserVo` / `SysUserMapper.update` | setIfPresent；昵称缓存驱逐；头像对账；resetUserPwd 只写密码列；Mapper update 带 `@DataPermission` | 服务与 VO/BO/Mapper | 2026-09-16 |
| S-L022-05 | `AuthController` social 三扇；`SysLoginService.socialRegister`；`SocialAuthStrategy.login` | 贴/撕/社交登录不在本课 Controller；authId 占用抛错；同人同平台可 update；未绑定不能 social grant | `/auth/binding` `/social/callback` `/unlock/{socialId}` | 2026-09-16 |
| S-L022-06 | `PasswordWritePathUnitTest.profileChangeRejectsWeakValueBeforeHashingOrWriting`；`PasswordCandidateContractUnitTest` 对 `updatePwd` 的 `@Log` | 弱密码不写库；改密日志不存请求/响应体 | `@Test` 方法名 | 2026-09-16 |
| S-L022-07 | `frontend/packages/domains/system/src/service.ts`、`resource-service.ts`、`apps/admin-web/src/views/system/user/profile/*`、`apps/home-web` 路由 | URL 与本课映射一致（前端格子仍归 L-019）；解绑走 identity social.unlock；home `/profile` 是档案模块 | `users.profile*` / `resources.social.list` / 个人中心页 | 2026-09-16 |
