---
lesson_id: L-013
objective_ids: [OBJ-13]
claimed_cells: [B:IAuthStrategy, B:SysLoginService, B:SysRegisterService]
estimated_minutes: 37
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-hubs-on-disk
    minutes: 9
  - segment: deep-explanation
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-002, S-L013-01, S-L013-02, S-L013-03, S-L013-04, S-L013-05, S-L013-06, S-L013-07, S-L013-08]
---

# Lesson 013：盖章口、值班员、办证窗

## 学完你能做什么

站在 `wta-admin` 的认证主机里，你能把三个枢纽的**副作用**分开讲，而不是揉成一句「登录服务写会话」：

1. **`IAuthStrategy`**：按 `grantType + "AuthStrategy"` 找到盖章口；五种 Bean 自己验人。只有策略调用了 `LoginHelper.login` 之后，才出现 token-session。
2. **`SysLoginService`**：值班员。拼 `LoginUser`、管 Redis 错次、记审计事件、退出时 `StpUtil.logout`、已登录后绑社交。它**没有** `login(...)`，也**不**调用 `LoginHelper.login`。
3. **`SysRegisterService`**：办证窗。验 Client / 验证码 / 唯一 / 密码策略，再 `registerUser` + `grantUserType`。全文件无 `LoginHelper.login`。办好用户证 ≠ 拿到 `access_token`。

你还能指失败时哪个柜子动了：

| 柜子 | 成功登录 | 登录失败（策略内） | 注册失败（落库前） | 注册成功 |
| --- | --- | --- | --- | --- |
| Redis token-session | 有 | 无 | 无 | 无 |
| Redis `pwd_err_cnt:` | 删 | 可能 +1 / 已锁则只抛 | 不动 | 不动 |
| Redis 图形验证码键 | 已删 | 已删（先删再比） | 已删（先删再比） | 已删 |
| MySQL `sys_user` | 只改最近登录 IP/时间 | 不改 | 不插 | 插入 |
| MySQL 登录域关系 | 不改 | 不改 | 不插 | `SELF_REGISTER` |
| `sys_login_info` 审计 | `Success` | 常有 `Error` | 验证码失败常有 `Error` | `Register` |

本课认格子：`B:IAuthStrategy`、`B:SysLoginService`、`B:SysRegisterService`。HTTP 五个入口是 L-011；验证码三个 code 与策略字段表是 L-012；前端 `IdentityAccessService` 是 L-014；「登录成功但 Redis 会话丢失」是 L-084；在线用户/登录日志查询是 L-023。

口诀里若出现「`SysLoginService.login` 写会话」，工作树里没有这个方法。2026-09-16 以字节为准。

## 先把宏观地图放在桌上

L-006 已经把走廊画过：门卫核 Client → 策略验人 → `LoginHelper.login` 才写会话。L-011 认门上的五支枪。本课走进门后的三张桌子。

```text
  POST /auth/login                         POST /auth/register
  AuthController.login                     AuthController.register
  （Client / grant / 停用？L-011 的门）        （只转发，不写会话）
           |                                        |
           v                                        v
  IAuthStrategy.login(body, client, grantType)   SysRegisterService.register
           |                                        |
           |  Bean = grantType + "AuthStrategy"     |  落用户 + 登录域
           v                                        |  从不 LoginHelper.login
  Password / Sms / Email / Social / Xcx             v
           |                                   sys_user + sys_user_type_rel
           |  验人通过后
           v
  LoginHelper.login  ──►  StpUtil.login + token-session["loginUser"]
           |
           +── SysLoginService.buildLoginUser / checkLogin* / recordLoginInfo
           +── 退出走 SysLoginService.logout → StpUtil.logout
```

三张桌子不是同一类活：

| 桌子 | 卖什么 | 不卖什么 |
| --- | --- | --- |
| `IAuthStrategy` | 选盖章口、造 `SaLoginParameter`、各 grant 验人，成功则请 `LoginHelper` 开柜 | HTTP、Client 是否存在、传输加密、注册落用户 |
| `SysLoginService` | 拼登录态、错次锁、审计事件、退出清柜、已登录绑社交、更新最近登录 | 自己 `StpUtil.login`、自己插 `sys_user` |
| `SysRegisterService` | 开放注册时办用户证 + 授登录域 + 记 REGISTER | 通行证、`access_token`、错次锁 |

**类比失效处：** 夜店门口盖章、前台办会员、存包处开柜子，不是同一只手。本仓库也没有一个叫「登录服务」的总开关替你写会话。盖章口找错（没有这个 Bean）只扔 `ServiceException("授权类型不正确!")`，不会开柜子。办会员成功，当晚仍要再走登录才能拿到柜钥匙。

失效点还有四个：

1. **门卫不在本课格子里。** `AuthController.login` 在 Client 为空、grant 不含、Client 停用时直接 `R.fail`，**不进入** `IAuthStrategy.login`。传输加密是方法上的 `@ApiEncrypt`，策略拿到的已经是解密后的 JSON 字符串。
2. **值班员会被别人复用。** `AdminSsoIdentityService` 同样调用 `checkLoginAllowed` / `loginFailed` / `loginSucceeded` / `buildLoginUser`，但走 SSO 认人，不走 `IAuthStrategy.login`。SSO 发业务票是后课。
3. **审计柜 ≠ 会话柜。** `recordLoginInfo` 发 `LoginInfoEvent`，`SysLoginInfoServiceImpl.recordLoginInfo` 是 `@Async @EventListener`，往 `sys_login_info` 插行。失败登录常有字；没有 token-session 仍算没登录。
4. **小程序盖章口是半成品。** `XcxAuthStrategy.loadUserByOpenid` 仍是 `todo`，`new SysUserVo()` 不是查库。不要把它口述成已经能发小程序票。

## 三个枢纽在磁盘上长什么样

这一节是 deep 的硬证据。2026-09-16 在 `/srv/WTA-plus` 对过工作树。先 `rg`，再背口诀。三个类都住在 `backend/wta-admin/.../web/service/`，**不是** `wta-system` 的业务房间。

### 1. `IAuthStrategy`：调度器 + 五只盖章口

文件：`backend/wta-admin/src/main/java/org/namewta/web/service/IAuthStrategy.java`。

静态入口 `login(String body, SysClientVo client, String grantType)`：

1. `beanName = grantType + "AuthStrategy"`（常量 `BASE_NAME = "AuthStrategy"`）。
2. `SpringUtils.containsBean` 为假 → `ServiceException("授权类型不正确!")`。**无会话、无错次、无用户行。**
3. `getBean` 后调实例 `login(body, client)`。注意：这里**不再**传 `grantType`，每种策略自己解析 body。

`buildLoginParameter(client)` 把 Client 配置抄进 `SaLoginParameter`，还不碰 Redis：

| extra 键（`LoginHelper` 常量） | 来源 |
| --- | --- |
| `clientid` | `client.getClientId()` |
| `clientPk` | `client.getId()` |
| `clientAccessPath` | `ClientAccessPaths.resolve(clientKey, accessPath)`（home 会并入 getInfo / getRouters / logout / profile） |
| `clientIpWhitelist` | `client.getIpWhitelist()` |
| device / timeout / activeTimeout | Client 字段 |

工作树里五只实现都调用无 customizer 的重载。`HomeClientLoginAccessPathTest` 只测 home 的 extras，不测写会话。

五只 Bean（`@Service(grant + BASE_NAME)`）：

| Bean 名 | 类 | 凭据 | 何时 `LoginHelper.login` |
| --- | --- | --- | --- |
| `passwordAuthStrategy` | `PasswordAuthStrategy` | 用户名 + 永久 BCrypt 或临时密码；可选图形验证码 | `authenticate` 成功且拼好 `LoginUser` 之后 |
| `smsAuthStrategy` | `SmsAuthStrategy` | 手机号 + Redis 短信码 | `checkLogin` 成功且 `requireLoginAccess` 之后 |
| `emailAuthStrategy` | `EmailAuthStrategy` | 邮箱 + Redis 邮箱码 | 同上 |
| `socialAuthStrategy` | `SocialAuthStrategy` | 第三方 code；必须**已经绑定** | 找到绑定用户且准入之后 |
| `xcxAuthStrategy` | `XcxAuthStrategy` | 小程序 `xcxCode` | 源码仍会调用，但 `loadUserByOpenid` 未接库 |

密码策略的关键顺序在 `PasswordAuthStrategy.authenticate`（包可见，单测直接打这一段）：

1. `checkLoginAllowed`：Redis `pwd_err_cnt:` + 用户名已达 `user.password.maxRetryCount`（默认 5）→ 记 `LOGIN_FAIL` 并抛，**不再比密码**。
2. 永久密码 `BCrypt.checkpw`。不对则 `temporaryPasswordService.verify`；再没有 → `loginFailed`（计数 +1，TTL `lockTime` 分钟，默认 10）。
3. `clientUserTypeAccessService.requireLoginAccess`：用户必须拥有该 Client 配置的正常登录域。失败时**不** `consume` 临时密码（单测钉死），也**不** `loginSucceeded` / `loginFailed`。
4. 临时密码在准入**之后**才 `consume`；CAS 失败算一次 `loginFailed`。
5. 这才 `loginSucceeded`（删错次键）。
6. 回到 `login`：`buildLoginUser` → `buildLoginParameter` → **`LoginHelper.login(loginUser, model)`** → `LoginVo.access_token` / `expire_in` / `client_id`。

图形验证码在 `authenticate` **之前**：开关开着就 `validateCaptcha`。键是 `CAPTCHA_CODE_KEY + uuid`，**先 `deleteObject` 再比对**。过期 / 不匹配都 `recordLoginInfo(..., LOGIN_FAIL)` 后抛，**不** `LoginHelper.login`。同一张图不能重试。

短信 / 邮箱与密码有一处顺序差，不要混：

- 它们用 `checkLogin(type, username, () -> !codeEquals)`：码对了就在**准入之前** `loginSucceeded` 清空错次。
- 码错：`loginFailed` +1。码在 Redis 里过期：`validateSmsCode` / `validateEmailCode` 自己抛 `CaptchaExpireException`，**不走** `loginFailed`（错次不 +1），但仍可能已写一条过期审计。
- 策略里对短信/邮箱码是 **GET 比对，不删键**。和图形验证码「先删再比」不是同一把锁。限流键与发码入口留给 L-012。

社交登录：`selectByAuthId(source + uuid)` 为空 → `ServiceException("你还没有绑定第三方账号，绑定后才可以登录！")`，不写会话。绑定是另一条枪：已登录的 `POST /auth/social/callback` → `SysLoginService.socialRegister`（下面第 2 节）。先有密码柜，才能把社交徽章别上去；不能靠社交登录「凭空办证」。

小程序：`loadUserByOpenid` 注释写着 `todo 自行实现`。`new SysUserVo()` 不是 null，后面的「用户不存在」分支是死代码。`LoginUser.getLoginId()` 需要非空 `userType` 与 `userId`。不要把这只 Bean 的存在说成产品路径已通。

### 2. `SysLoginService`：值班员的副作用清单

文件：`backend/wta-admin/src/main/java/org/namewta/web/service/SysLoginService.java`。全文件搜不到 `LoginHelper.login` 或 `StpUtil.login`。会话写入不在这里。

| 方法 | 副作用 | 失败时 |
| --- | --- | --- |
| `buildLoginUser` | **读**：部门名、菜单权、角色权、角色列表、数据范围、岗位；虚拟线程并行四段。写入的是内存里的 `LoginUser` | 抛错则策略还没调用 `LoginHelper.login`（密码路径此时错次键可能已被清） |
| `checkLoginAllowed` | 读 Redis 错次；已锁则审计 `LOGIN_FAIL` + `UserException` | 不 +1（已经锁了） |
| `loginFailed` | 错次 +1 写回 Redis（TTL = `lockTime` 分钟）；审计 `LOGIN_FAIL`；返回（不是抛）`UserException` | 调用方负责 `throw` |
| `loginSucceeded` | **删** `pwd_err_cnt:` + 用户名 | 删失败只影响下次是否还锁，不补写会话 |
| `checkLogin` | 先 `checkLoginAllowed`，supplier 为 true 则 `throw loginFailed(...)`，否则 `loginSucceeded` | supplier 自己抛（如验证码过期）则**跳过** `loginFailed` |
| `recordLoginInfo` | `publishEvent(LoginInfoEvent)`，带 IP / UA / 头 `clientid` | 事件异步落库；它不是 token |
| `updateLastLoginInfo` | `DataPermissionHelper.ignore` 下 `userMapper.updateById`：`loginIp` / `loginDate` | 登录已经成功；这是事后贴纸 |
| `logout` | try 里若能拿到 `LoginUser` 则记 `LOGOUT`；**finally 里** `StpUtil.logout()` | 未登录也尽量 logout；`NotLoginException` 吞掉 |
| `socialRegister` | `@Lock4j`；`LoginHelper.getUserId()` 必须已有会话；插或改 `sys_social` | authId 已被别人绑 → 抛，不改会话 |

`LoginUser.getLoginId()` 是 `userType + ":" + userId`。`buildLoginUser` 必须带上 `requireLoginAccess` 返回的登录域，否则 `LoginHelper.login` 会在 `getLoginId()` 处炸，柜子开不成。

退出是本课里 **SysLoginService 唯一直接碰 `StpUtil` 的写路径**：删的是当前 token 会话，不是插新的。Sa-Token 监听器 `UserActionListener.doLogout` 还会删 Redis `online_tokens:` + token。

登录成功之后的「贴纸」不在 `SysLoginService.login`（没有这个方法），而在监听链：

1. `LoginHelper.login` → `StpUtil.login` → `getTokenSession().set("loginUser", loginUser)`。
2. `UserActionListener.doLogin`（实现 `SaTokenListener`）发 `UserLoginSuccessEvent`。
3. `UserLoginSuccessListener`：写 `online_tokens:` + token（TTL 跟 Client `timeout`，-1 则无 TTL）→ `recordLoginInfo(LOGIN_SUCCESS)` → `updateLastLoginInfo`。

因果：会话先写，贴纸后贴。贴纸失败（审计异步、在线缓存、最近登录 IP）**不能**用本课的字节证明会回滚 token。那是 L-084 的「成功但会话丢失」邻格，不要提前宣布。

配置钉在 `backend/wta-admin/src/main/resources/application.yml`：`user.password.maxRetryCount: 5`，`lockTime: 10`。错次键前缀是 `CacheNames.PWD_ERR_CNT_KEY = "pwd_err_cnt:"`，短信/邮箱错次**共用这把钥匙**，只是 `LoginType` 的文案不同。

### 3. `SysRegisterService`：办证窗，不发通行证

文件：`backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java`。方法 `register` 标了 Spring `@Transactional(rollbackFor = Exception.class)`。全文件无 `LoginHelper.login` / `StpUtil.login`。

顺序（落库前全部失败 = 没有新用户行，这是源码顺序 + `PasswordWritePathUnitTest` 能证明的部分）：

1. Client 必须存在、状态正常、`registerEnabled == true`、配置了 `userTypeId`；登录域必须存在且正常。否则 `ServiceException`，不碰验证码、不插用户。
2. 验证码开关开着：`validateCaptcha` 与密码登录相同——**先删 Redis 键再比对**。失败记 `LOGIN_FAIL`（不是 Register）后抛。
3. 用户名 / 可选手机 / 可选邮箱唯一。失败「该账号已存在，请登录」等，**此时验证码已经烧掉**。
4. `passwordPolicyService.validateOrThrow(plaintext)`。失败 `ServiceException("密码不符合安全策略")`，**尚未** `BCrypt.hashpw`，**尚未** `registerUser`。空密码按策略当成 `""` 打违规，不是 NPE。单测钉死：先 `validateOrThrow`，再 `registerUser`。
5. `BCrypt.hashpw` 后 `userService.registerUser`。内部 `@DSTransactional`，`insert sys_user`，成功则回填 `userId`。返回 false → `UserException("user.register.error")`，没有 userId 就不会 grant。
6. `userTypeRelService.grantUserType(userId, client.userTypeId, SELF_REGISTER)`。内部同样 `@DSTransactional`。
7. 最后 `recordLoginInfo(..., REGISTER, user.register.success)`。异步审计。返回 `void`；`AuthController.register` 只 `R.ok()`，body 里没有 token。

**半用户（half user）怎么讲，必须以源码能证明的为准：**

- **落库前失败（Client / 验证码 / 唯一 / 策略 / `registerUser==false`）：** 不会留下新的 `sys_user`。策略拒绝发生在 insert 之前，事务回不回滚都不需要：根本还没写。
- **`registerUser` 已 insert 成功，随后 `grantUserType` 抛错：** 外层注解的意图是整方法回滚。但 `registerUser` 与 `grantUserType` 各自还有 `@DSTransactional`。工程规范 PERSIST-001：同一调用链不得把 Spring `@Transactional` 和 `@DSTransactional` 静默混用。工作树里**没有**集成测试证明 grant 失败会把用户行一起撤掉。不要把「一定没有半用户」说成已验证事实；也不要抄这套混用去写新代码。新边界用 `@DSTransactional`。
- **审计失败：** 异步监听，用户行已经在。缺的是日志，不是半用户。
- **成功：** 有用户、有登录域、有 REGISTER 审计，**仍然没有** token-session。要通行证，再 `POST /auth/login`。

`RegisterBody` 构造器把 `grantType` 默认成 `"password"`，避免继承 `LoginBody` 的必填挡住公开注册。这与「注册成功自动按 password 策略登录」无关：注册方法从不调策略 Bean。

## 核心概念与机制

### 直觉讲解

把后台想成一家夜店。

**盖章口（`IAuthStrategy`）** 门口有五扇小窗：密码、短信、邮箱、社交、小程序。你报一个 `grantType`，店里就去找叫 `passwordAuthStrategy` 这种名字的窗口。窗户不存在，保安直接说「授权类型不正确」，不会给你柜子。窗户在，才验你是谁。验过了，才请存包处开柜。

**存包处（`LoginHelper.login` / `StpUtil`）** 才是柜子。钥匙条叫 `access_token`，柜子夹层里放一份 `loginUser` 档案。盖章口负责喊「开柜」；值班员负责把档案填厚（部门、角色、菜单权），不负责拧柜子锁。

**值班员（`SysLoginService`）** 还有一本涂鸦本：密码连错就在 Redis 小黑板上画正字，满五笔关十分钟。进出门在另一本**访客登记（`sys_login_info`）**上记一笔。登记本可以写「来过但被拒」；那不是柜子钥匙。

**办证窗（`SysRegisterService`）** 在白天办公楼。它给合格的人做会员卡（`sys_user`）并盖一个「这个 App 的登录域」章。办完证，当晚进夜店仍要再走盖章口。办证处绝不发存包钥匙。

**类比失效处：** 真夜店可能办证时顺便给你手环。这里不会。社交窗口还规定：没先把徽章别在已有会员卡上，不能从社交门进来。小程序窗口的牌子挂着，里面的查人手续还是 `todo`。访客登记是异步的，写失败不会把已经打开的柜子再锁上——那一格留给 L-084，本课只认「谁喊开柜」。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 授权策略 | auth strategy | `IAuthStrategy` 及其 `grantType + "AuthStrategy"` Bean。负责验人并在成功时调用 `LoginHelper.login` |
| 授权类型 | grant type | 请求体里的 `grantType` 字符串，必须等于 Bean 前缀，且已被门卫确认出现在 Client 配置里 |
| 登录参数 | login parameter | `SaLoginParameter`：超时、设备、Client extras。还不是会话 |
| 登录用户 | login user | 内存档案 `LoginUser`；真正进柜是 token-session 键 `loginUser` |
| 登录标识 | login id | `userType + ":" + userId`，Sa-Token 的 `StpUtil.login` 主键 |
| 会话写入 | session write | `LoginHelper.login`：`StpUtil.login(...)` + `getTokenSession().set("loginUser", ...)`。持久化细节归 L-084 |
| 值班员 | login service | `SysLoginService`：拼档案、错次、审计、退出、绑社交。不开柜 |
| 错次锁 | retry lock | Redis `pwd_err_cnt:` + 用户名；阈值 `maxRetryCount`，TTL `lockTime` 分钟 |
| 审计柜 | login-info audit | `LoginInfoEvent` → 异步插入 `sys_login_info`。`Success` / `Error` / `Logout` / `Register` |
| 办证 | register | `SysRegisterService.register`：插用户 + 授登录域 + REGISTER 审计。不写会话 |
| 登录域准入 | client user-type access | `ClientUserTypeAccessService.requireLoginAccess`：用户必须拥有 Client 配置的正常 `userTypeId` |
| 临时密码消费 | consume temporary password | 仅密码策略：准入通过后才 `consume`；准入失败不消费 |
| 半用户 | half user | insert 已发生但登录域未授（或事务未一起撤）。落库前失败不是半用户；grant 失败是否回滚**未**被本仓测试钉死 |

**Strategy ≠ Controller。** 策略不读 HTTP 注解，不决定 Client 是否存在。

**Login service ≠ session writer。** 写会话的英文名字在磁盘上叫 `LoginHelper.login`。

**Register ≠ login。** 注册成功的 HTTP 是 `R.ok()` 空数据，不是 `access_token`。

**Audit ≠ session。** `Constants.LOGIN_FAIL = "Error"` 写的是日志行。

**Captcha consume ≠ retry increment。** 图形码先删键；错次键是另一把。

### 机制/因果链

**A. 选盖章口（还没有柜子）。**

1. 门卫已经确认 Client 正常且 grant 字符串在配置里（L-011）。
2. `IAuthStrategy.login` 拼 Bean 名。没有 Bean → 抛错，停。
3. 实例再 parse 成 `PasswordLoginBody` / `SmsLoginBody` / …，`ValidatorUtils.validate`。字段不齐 → 抛错，停。

因果：写错 `grantType` 大小写（`Password` 对不上 `passwordAuthStrategy`）在这里就被挡。不会误开密码柜。

**B. 密码主路径写会话。**

1. 可选：烧掉图形验证码键并比对。
2. `SysUserMapper` 按用户名加载。不存在 / 停用 → `UserException`，错次键不动（人还没开始比密码）。策略直接持 Mapper，这是认证段存量，不是业务模块入口模板。
3. `authenticate`：锁检查 → 永久或临时密码 → 登录域准入 → 可能消费临时密码 → 清空错次。
4. `buildLoginUser` 把权限快照填进内存。
5. `buildLoginParameter` 把 Client 超时和 accessPath 放进 extras。
6. **`LoginHelper.login`**：补 IP / UA；`StpUtil.login(loginId, extras)`；token-session 放入 `loginUser`。
7. 策略读 `StpUtil.getTokenValue()` / `getTokenTimeout()` 填 `LoginVo`。
8. 监听器发成功事件：在线缓存、`LOGIN_SUCCESS` 审计、更新 `sys_user` 最近登录。
9. 回到 `AuthController.login` 才尽力发欢迎通知；通知失败只 warn，**不得**把已经成功的登录改成错误（L-011 的门，本课只要知道会话在第 6 步已经写下）。

因果：第 6 步之前任何抛错，Redis 里都没有这次 token-session。第 6 步之后，审计和通知都是贴纸。

**C. 短信/邮箱路径的错次时机。**

1. 先加载用户（不存在则直接 `user.not.exists`）。
2. `checkLogin`：码错 +1；码对 **立刻** 清空错次。
3. 然后才 `requireLoginAccess`。域不对时：会话仍不写，但密码路径不会在这个时刻清空错次，短信路径**已经清空了**。

因果：不要把「`checkLogin` 成功」说成「已经登录」。它只处理错次键。柜子还在后面。

**D. 注册因果。**

1. Client 未开放注册 → 无用户。
2. 验证码失败 → 无用户，验证码键已删，可能有 `Error` 审计。
3. 唯一性失败 → 无新用户，验证码已删。
4. 策略拒绝 → 无用户、无哈希落库。
5. insert 失败返回 false → 无 grant。
6. 两步都成功 → 有用户、有 `SELF_REGISTER` 关系、有 Register 审计、**无会话**。

因果：办证窗的产品合同是「人进花名册」，不是「人进店」。前端若把 `POST /auth/register` 的 200 当成已登录，会在下一枪 `getRouters` 上撞未登录。

**E. 退出。**

1. 能读到 `LoginUser` 就记 Logout 审计。
2. finally 里 `StpUtil.logout()`。监听器删 `online_tokens:`。
3. 未登录调用 logout：审计可能没有，logout 仍尽量执行。

因果：退出的写路径在值班员，不在策略。策略只负责进门开柜。

## 图、表或文本图

**图题 / caption：** 三张桌子与谁开柜。alt：登录请求经 IAuthStrategy 才到 LoginHelper；注册经 SysRegisterService 不经过 LoginHelper。

```text
          POST /auth/login                         POST /auth/register
                 |                                         |
                 |  Client 门卫（L-011）                    |
                 v                                         v
        IAuthStrategy.login(static)              SysRegisterService.register
        bean = grantType + AuthStrategy          @Transactional（混有内层 @DSTransactional）
                 |                                         |
        ┌────────┼────────┬─────────┬────────┐             |
        v        v        v         v        v             |
     password   sms     email    social     xcx            |
        |        |        |         |        |             |
        |        |        |      需已绑定    todo 查人        |
        +--------+--------+---------+--------+             |
                          |                                |
              验人失败 ──x── 不调用 LoginHelper.login        |
                          |                                |
                          v                                |
              SysLoginService.buildLoginUser               |
              （读权限；不开柜）                             |
                          |                                |
                          v                                |
              LoginHelper.login                            |
              StpUtil.login + session[loginUser]           |
                          |                                |
                          v                                |
              UserActionListener.doLogin                   |
              online_tokens + LOGIN_SUCCESS + 最近登录      |
                                                           |
              SysLoginService.logout ──► StpUtil.logout    |
                                                           |
              注册成功：sys_user + grantUserType            |
                        + REGISTER 审计                    |
                        × LoginHelper.login  ◄─────────────┘
```

**文字等价物：** 左列是登录。请求先经过本课不展开的 Client 门卫，再由 `IAuthStrategy` 按 grantType 找 Bean。五只策略验人；社交必须先有绑定，小程序查人仍是待办。验人失败的箭头在 `LoginHelper.login` 之前就结束。成功则值班员只负责把 `LoginUser` 填厚，存包处 `LoginHelper` 才 `StpUtil.login` 并在 token-session 放入 `loginUser`。之后监听器写在线缓存、成功审计和最近登录字段。退出单独从 `SysLoginService.logout` 指向 `StpUtil.logout`。右列是注册：只指向用户表和登录域关系加 REGISTER 审计，并明确画叉：不调用 `LoginHelper.login`。

**图的边界：** 不画 `@ApiEncrypt` 信封、不画 CaptchaController 发码、不画前端 `Admin-Token`。不保证 Redis 在 `StpUtil.login` 之后仍可读（L-084）。不把 xcx 画成实线产品。home 的 accessPath 并入发生在 `buildLoginParameter`，图上不展开每一条 path。

**图题 / caption：** 密码登录失败点与柜子。alt：验证码、锁、密码、登录域、开柜之前各失败对应 Redis 会话仍空。

```text
validateCaptcha?  --fail--> 删验证码键 + LOGIN_FAIL 审计  --x--> 无会话、错次不加
        |
loadUser         --无/停用--> UserException                 --x--> 无会话、错次不加
        |
checkLoginAllowed --已锁--> LOGIN_FAIL 审计 + 抛             --x--> 无会话、错次不加
        |
BCrypt / 临时密码 --错--> loginFailed（错次+1）              --x--> 无会话
        |
requireLoginAccess --无域--> 抛（临时密码不 consume）         --x--> 无会话、错次不加
        |
consume 临时密码  --CAS 失败--> loginFailed                  --x--> 无会话
        |
loginSucceeded  删 pwd_err_cnt
        |
buildLoginUser / LoginHelper.login
        |
      Redis token-session 这才有字
```

**文字等价物：** 从上往下，每一道失败都在开柜之前。图形验证码失败会烧掉验证码并可能写审计，但不记密码错次。用户不存在或停用不记错次。已经锁上只重复那条「超过次数」的失败审计。密码不对才 +1。登录域不对时临时密码留着还能下次用，错次也不加。临时密码消费撞车才再记一次失败。只有走到 `LoginHelper.login`，会话柜才有字。

**图的边界：** 短信/邮箱把「清空错次」放在登录域检查之前，不要把这张密码图套到 sms Bean 上。社交失败（未绑定）不走错次键。注册失败不走这张图，走办证顺序。

**图题 / caption：** 注册落库前闸门。alt：Client、验证码、唯一、密码策略都在 insert 之前；成功仍无 token。

```text
Client 停用 / 未开放注册 / 无登录域  --x--> 无用户、无会话
        |
validateCaptcha（先删键）失败      --x--> 无用户、无会话、可能 Error 审计
        |
用户名/手机/邮箱不唯一             --x--> 无新用户、验证码已烧掉
        |
validateOrThrow 明文失败          --x--> 无用户、无哈希行
        |
registerUser insert               --false--> 无 grant、无会话
        |
grantUserType(SELF_REGISTER)      --抛错--> 外层意图回滚；与内层 @DSTransactional
        |                                 混用，半用户是否留下 = 未验证
        v
REGISTER 审计（异步）
        |
      仍无 LoginHelper.login
```

**文字等价物：** 办证窗前四道闸都在插入 `sys_user` 之前，失败不会出现新会员卡。insert 返回 false 也不会授登录域。grant 这一步的回滚承诺被两套事务注解搅浑，本课标成未验证，而不是「肯定没有半用户」或「肯定留下半用户」。无论办证是否成功，存包处都不开门。

**图的边界：** 不展开 `PasswordPolicy` 字段表（L-012）。不把管理员 `SysUserController.add` 算进公开注册。不把 SSO 创建用户算进这扇窗。

## 正例、反例与边界

**正例 1：** `AuthController.login` 过门后只调 `IAuthStrategy.login(body, client, grantType)`。密码 Bean 名是 `"password" + "AuthStrategy"`。成功路径末尾才 `LoginHelper.login`。

**正例 2：** `LoginHelper.login` 先 `StpUtil.login(loginUser.getLoginId(), extras)`，再 `StpUtil.getTokenSession().set("loginUser", loginUser)`。这是本课「会话写入」的操作定义。`PlusSaTokenDao` 怎么落到 Redis，留给 L-084。

**正例 3：** `SysLoginService.logout` 用 finally 调用 `StpUtil.logout()`。未登录或不带 `LoginUser` 也不跳过清柜尝试。

**正例 4：** `SysRegisterService.register` 在 uniqueness 之后、`BCrypt.hashpw` 之前 `validateOrThrow`。`PasswordWritePathUnitTest.registrationValidatesPlaintextBeforeHashingAndPersistence` 证明顺序，并且 `registerUser` 返回 false 时抛 `UserException`。

**正例 5：** 临时密码：`PasswordAuthStrategyTemporaryUnitTest` 三连——永久密码胜利则从不读临时值；准入失败不 `consume`、不 `loginSucceeded`、不 `loginFailed`；`consume` 失败才 `loginFailed`。

**正例 6：** `IAuthStrategy.buildLoginParameter` 给 home Client 并入身份 API。`HomeClientLoginAccessPathTest` 断言允许 `/system/user/getInfo`、`/system/menu/getRouters`，不允许 `/system/client/list`。这是 extras，不是已经登录。

**正例 7：** 注册成功路径仍无 `LoginHelper.login`。`AuthController.register` 返回 `R<Void>`。要菜单必须再登录。

**反例 1：** 在 `SysLoginService` 里新写一个 `login()` 去调 `StpUtil.login`，「值班员应该负责开柜」。现有五只策略会重复开柜，SSO 复用值班员时也会被绑死。

**反例 2：** 注册成功后在 `SysRegisterService` 末尾补 `LoginHelper.login`，「体验好一点」。格子合同是办证 ≠ 通行证。要自动登录应走显式登录策略，并处理验证码已删、错次键、Client 准入。

**反例 3：** 把 `recordLoginInfo` 当成「已经登录」。验证码错误也会记 `Error`。审计柜有字，会话柜仍空。

**反例 4：** 社交登录里若未绑定就 `registerUser` 顺手办证。当前策略直接抛「还没有绑定」。办证窗是 `/auth/register`；绑定窗是已登录的 `/auth/social/callback`。

**反例 5：** 看见 `xcxAuthStrategy` Bean 就对接小程序发码。`loadUserByOpenid` 仍是 todo。先接库再宣称副作用。

**反例 6：** 密码策略失败仍去 `hashpw` 再 insert，「先落再改」。源码与单测都禁止。明文违规不得变成哈希行。

**反例 7：** 把短信 `checkLogin` 成功口述成已写会话。那一步只清错次。没有登录域的用户会在下一行 `requireLoginAccess` 被挡，柜子仍空。

**反例 8：** 新代码在同一方法上叠 `@Transactional` 和内层 `@DSTransactional`，并口头保证半用户不会出现。PERSIST-001 禁止新混用；本课也不把 grant 失败的回滚当已证。

**反例 9：** 登录失败去 `updateLastLoginInfo`。该方法只挂在成功监听器上。失败路径最多动错次键和审计。

**反例 10：** 业务模块 `import org.namewta.web.service.IAuthStrategy`。这三个枢纽是 admin 主机接线，不是 `wta-api` 合同。跨模块认人走已有 SPI（例如 `SsoIdentityService`），不要把盖章口当成公共库。

**边界：**

- Client 是否存在、grant 是否包含、`@ApiEncrypt`、欢迎通知尽力而为：L-011。本课只要求：那些失败进不了策略，通知失败改不了已写会话。
- 发码、限流键、`PasswordPolicy` 字段：L-012。本课只要求注册在 insert 前 `validateOrThrow`，登录不重验旧密码复杂度。
- 前端 session 钥匙名 `Admin-Token` / `Home-Token`：L-014 / L-010。本课的会话是服务端 Sa-Token。
- Redis 会话丢失、Caffeine 5 秒缓冲：L-084。
- 在线用户列表、登录日志查询页：L-023。本课只生产那些表/缓存里的行，不讲怎么查。
- `AdminSsoIdentityService` 复用值班员：变式，不是本课认领的格子。

## 变式与迁移

- **变式 A：新加一种 grant。** 新增 `@Service("fooAuthStrategy")` 实现 `IAuthStrategy`。验人失败不得调用 `LoginHelper.login`。成功路径：准入 → `buildLoginUser` → `buildLoginParameter` → `LoginHelper.login` → 填 `LoginVo`。Client 配置必须包含 `foo`。不要去改 `SysLoginService` 增加 `login()`。
- **变式 B：某 Client 关掉密码、只留短信。** 门卫按 Client.grantType 拒绝 password。不要删 `PasswordAuthStrategy` Bean。Bean 在，只是这扇门不让走。
- **变式 C：注册后立刻进店。** 产品若真要，应在 Controller 成功后显式再走策略或独立发票，并写测试：验证码已消耗、不会留下半会话。不要在办证窗里偷偷 `StpUtil.login`。
- **变式 D：临时密码发给未授该 App 登录域的人。** 当前密码策略会在 `consume` 前被 `requireLoginAccess` 挡住，临时值保留。若改成先消费再准入，单测会红，一次性票会作废。
- **变式 E：把 `SysRegisterService` 的事务改成 `@DSTransactional`。** 这是朝 PERSIST-001 的迁移，不是行为课作业。迁移时要补「insert 成功、grant 失败」的回滚测试，才能把半用户从「未验证」改成「已证不会留下」或「已证会留下并如何补偿」。
- **变式 F：SSO 认人。** 复用 `SysLoginService` 的错次与 `buildLoginUser`，不走 `IAuthStrategy`。开柜发生在 SSO 发业务票的那条链（L-055 / L-057），不要把 `verifyPassword` 说成本课的 `LoginHelper.login`。
- **变式 G：图形验证码关闭。** `CaptchaProperties.enable=false` 时密码策略跳过 `validateCaptcha`，仍要走锁、密码、登录域、开柜。关闭验证码 ≠ 关闭错次锁。
- **迁移口诀：** 门卫（Client）→ 盖章口（策略验人）→ 值班员填档案 / 管错次 → 存包处开柜；办证窗平行，从不并入开柜。跳步就会出现「日志有字当已登录」或「注册 200 当有 token」。

## 常见误区

1. **「`SysLoginService` 负责登录。」** 它负责登录的**杂务**。开柜在 `LoginHelper.login`，由策略喊。
2. **「`IAuthStrategy` 检查 Client。」** Client 空/停用/grant 不含在 `AuthController.login` 就返回了。策略假定 Client 已经合法，再查**用户有没有这个登录域**。
3. **「注册成功 = 已登录。」** 源码连 `LoginHelper` 的 login 方法都不调。
4. **「失败就不写任何库。」** 失败常写 `sys_login_info`，还可能 +1 错次、烧掉验证码。不写的是 token-session 和新用户行（落库前）。
5. **「五种 grant 的失败副作用一样。」** 图形码先删；短信码策略内不删。密码在准入后才清错次；短信在准入前就清。社交不走错次键。xcx 还没真正查人。
6. **「`checkLogin` 就是登录。」** 它是错次状态机。开柜是后面的 `LoginHelper.login`。
7. **「临时密码一验对就烧掉。」** 先准入，再 `consume`。单测就是为这句存在的。
8. **「外层有 `@Transactional`，所以注册绝无半用户。」** 落库前可以这么说。insert 之后混用 `@DSTransactional`，本课标未验证。
9. **「`updateLastLoginInfo` 在策略里。」** 在成功监听器里，开柜之后。
10. **「`socialRegister` 会登录。」** 它要求已经 `getUserId()`。它写的是社交绑定表。
11. **「加密登录是策略做的。」** `@ApiEncrypt` 在 HTTP 方法上。策略看见的是字符串 body。
12. **「错次键只给密码用。」** 键名是 `pwd_err_cnt:`，短信/邮箱 `LoginType` 也用 `SysLoginService` 这套计数。
13. **「`LoginVo.refresh_token` 一定有值。」** 策略目前只 set `accessToken` / `expireIn` / `clientId`（xcx 加 `openid`）。不要把字段存在说成已经签发刷新令牌。
14. **「主机里的策略可以直接当业务模板，Service 持 Mapper。」** L-006 已警告：这是认证段存量。新业务模块按登记表走分层，不要抄 `PasswordAuthStrategy` 的 Mapper 注入。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把副作用钉住，没有标准答案栏。

1. 打开 `IAuthStrategy.java` 的静态 `login`。写下 Bean 名怎么拼。故意在脑子里把 grantType 改成 `Password`，看它还能不能 `containsBean`。
2. 在 `SysLoginService.java` 全文搜 `LoginHelper.login` 和 `StpUtil.login`。预期：搜不到 login。再搜 `StpUtil.logout`：只应出现在 `logout` 的 finally。
3. 对照 `PasswordAuthStrategy.authenticate` 与 `SmsAuthStrategy.login`：圈出 `loginSucceeded` 相对 `requireLoginAccess` 谁先谁后。
4. 打开 `SysRegisterService.register`，从 `validateOrThrow` 画到 `registerUser` 再到 `grantUserType`。确认没有 `LoginHelper.login`。再看方法上的 `@Transactional` 和 `SysUserServiceImpl.registerUser` 上的 `@DSTransactional`。
5. 打开 `LoginHelper.login`：数清楚先 `StpUtil.login` 还是先 `getTokenSession().set`。再打开 `UserActionListener.doLogin`，看成功贴纸从哪一枪开始。

## 总结、词汇表与下一步

- **`IAuthStrategy`**：盖章口调度器。Bean 名 `grantType + "AuthStrategy"`。五只实现自己验人；成功才请 `LoginHelper.login` 开柜。没有 Bean、验人失败、登录域失败：无 token-session。
- **`SysLoginService`**：值班员。拼 `LoginUser`、Redis 错次锁、审计事件、`StpUtil.logout`、已登录绑社交、成功后由监听器更新最近登录与在线缓存。不开柜。
- **`SysRegisterService`**：办证窗。策略拒绝与唯一性失败发生在 insert 之前，不会留下新用户。成功写入用户 + `SELF_REGISTER` 登录域 + REGISTER 审计，仍不发通行证。insert 之后 grant 失败的半用户回滚**未**被测试钉死；不要抄 Spring / DS 事务混用。
- **会话写入的操作定义：** `LoginHelper.login` → `StpUtil.login` + token-session `loginUser`。审计、在线缓存、欢迎通知都不是这一定义。
- **失败汇：** 会话柜空。审计柜和错次黑板仍可能有字。

词汇表：auth strategy / grant type / login parameter / login user / login id / session write / retry lock / login-info audit / register / client user-type access / consume temporary password / half user。

下一步：L-014 从前端 `IdentityAccessService` 打到这些入口，并认 `Admin-Token` / `Home-Token` 命名空间。L-012 把验证码三个 code 和密码策略字段补全。L-023 去看本课写下的登录日志和在线缓存怎么被查出来。L-084 再问：开柜成功但 Redis 读失败算哪一边。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-002 | `backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | 门卫之后才 `IAuthStrategy.login`；register 只转发；logout 调 `loginService.logout`；通知失败不推翻登录 | `login` / `register` / `logout` / `socialCallback` | 2026-09-16 |
| S-L013-01 | `backend/wta-admin/src/main/java/org/namewta/web/service/IAuthStrategy.java` | Bean 名 `grantType + AuthStrategy`；无 Bean 抛错；`buildLoginParameter` extras | `login` / `buildLoginParameter` | 2026-09-16 |
| S-L013-02 | `.../impl/{Password,Sms,Email,Social,Xcx}AuthStrategy.java` | 五只 Bean；谁在何时 `LoginHelper.login`；图形码先删；短信码不删；社交需绑定；xcx todo | 各 `login` / `authenticate` / `validate*` / `loadUser*` | 2026-09-16 |
| S-L013-03 | `backend/wta-admin/src/main/java/org/namewta/web/service/SysLoginService.java` | 无 `LoginHelper.login`；错次键；审计事件；`logout` finally `StpUtil.logout`；`socialRegister` 需已登录；`buildLoginUser` 只读拼装 | 全文件方法表 | 2026-09-16 |
| S-L013-04 | `backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java`；`SysUserServiceImpl.registerUser`；`SysUserTypeRelServiceImpl.grantUserType` | 落库前闸门；`validateOrThrow` 在 hash/insert 前；成功不写会话；内外事务注解混用 | `register`；`registerUser`；`grantUserType` | 2026-09-16 |
| S-L013-05 | `backend/wta-common/wta-common-satoken/.../LoginHelper.java`；`UserActionListener.java`；`UserLoginSuccessListener.java` | 会话写入两步；成功事件写 `online_tokens`、LOGIN_SUCCESS、最近登录 | `LoginHelper.login`；`doLogin`；`handleLoginSuccess` | 2026-09-16 |
| S-L013-06 | `ClientUserTypeAccessService.java`；`LoginUser.getLoginId`；`application.yml` user.password.*；`CacheNames.PWD_ERR_CNT_KEY` | 登录域准入；loginId 形状；默认 5 次 / 10 分钟；错次键前缀 | `requireLoginAccess`；`getLoginId`；yml 213–215 行 | 2026-09-16 |
| S-L013-07 | `PasswordAuthStrategyTemporaryUnitTest.java`；`PasswordWritePathUnitTest.java`；`HomeClientLoginAccessPathTest.java` | 临时密码消费时机；注册先策略后持久化；home extras 并入身份 API | 各 `@Test` | 2026-09-16 |
| S-L013-08 | `.agents/skills/engineering-standards/references/java/persistence-transactions-and-ddl.md` PERSIST-001；`SysLoginInfoServiceImpl.recordLoginInfo` | 禁止新混用 Spring / DS 事务；审计异步，不等于会话 | PERSIST-001；`@Async @EventListener` | 2026-09-16 |
