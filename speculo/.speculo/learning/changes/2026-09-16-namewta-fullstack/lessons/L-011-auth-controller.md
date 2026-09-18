---
lesson_id: L-011
objective_ids: [OBJ-11]
claimed_cells: [A:AuthController.login,logout,register,getClientContext,socialCallback,socialBinding]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: six-doors-on-disk
    minutes: 10
  - segment: deep-explanation
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-002, S-005, S-L011-01, S-L011-02, S-L011-03, S-L011-04, S-L011-05, S-L011-06]
---

# Lesson 011：门厅六扇窗——AuthController

## 学完你能做什么

你能指着 `backend/wta-admin/.../AuthController.java` **口述六扇公开窗**，并说出**谁写会话、谁不写**。不要把「登录」说成一整栋楼，也不要把格子名当成 Controller 自己调用了 `LoginHelper.login`。

**两套数窗，先对齐再背：**

- **OBJ-11 口述五扇：** `login` / `logout` / `register` / `client/context` / `social/callback`。`course.md` 原文到此为止。L-012 说的「L-011 五扇门」指这五条 HTTP 形状。
- **本课 chain / 矩阵 (a) 加认第六扇：** `GET /auth/binding/{source}`（格子名 `socialBinding`，Java 名 `authBinding`）。路条，不写会话。漏这一扇则本格 (a) 不满。
- **同文件第七扇不认：** `DELETE /auth/unlock/{socialId}`（`unlockSocial`）。点到「存在、要已登录」即可，留给 L-022。

矩阵格子名是 `A:AuthController.login,logout,register,getClientContext,socialCallback,socialBinding`。Java 方法名和格子名不完全一样：`getClientContext` 在源码里叫 **`clientContext`**；`socialBinding` 在源码里叫 **`authBinding`**。口述时 HTTP 路径、Java 名、格子名要能对上。

六扇公开映射（本课必须能指）：

1. **`POST /auth/login`**：交卷。缺 Client、grantType 对不上、Client 被封 → **`R.fail`，不进策略、本方法零处 `LoginHelper.login`**。过闸才交给 `IAuthStrategy.login`（写会话在策略里，L-013）。
2. **`POST /auth/logout`**：收通行证。委托 `SysLoginService.logout()`，再 `R.ok("退出成功")`。try 里提前 return **仍会**走进 finally 的 `StpUtil.logout`。
3. **`POST /auth/register`**：办新证。委托 `SysRegisterService.register`。**成功也不写会话**。验证码 Redis 删键**不在** JDBC 事务里。
4. **`GET /auth/client/context`**：问这扇门开不开。查不到 / 没传 Client **仍是 `R.ok`**，只是旗子全关；**不是**登录那条 `R.fail`。
5. **`POST /auth/social/callback`**：已经有通行证的人，把第三方账号**绑到自己身上**。`StpUtil.checkLogin()` 失败就进不去。**不发新 token**。同一用户同一平台已有行时**可以 update**。
6. **`GET /auth/binding/{source}`**：去第三方门口拿一张跳转纸条。平台没配 → `R.fail`。**不写会话**。

本课不宣称你会拆每个 `*AuthStrategy`、验证码 Redis 键形、密码策略字段表、或前端 `IdentityAccessService` 全表。那些是 L-012 / L-013 / L-014。本课要把**门卫自己做的闸**和**控制器合同**讲完：失败不写会话、委托删除、委托办证且无票。

## 先把宏观地图放在桌上

L-006 已经把走廊画过：准备登录 → `POST /auth/login` → Redis 会话 → `GET /system/menu/getRouters` → 领域 GET。本课走进**门厅那一排窗**，不再往菜单和 Mapper 走。

认证入口**不在** `wta-system`。2026-09-16 工作树里，门卫室在组装应用 `wta-admin`：

```text
浏览器 / App
    │  头 clientid（axios adapter）或 query clientId
    v
AuthController          @RequestMapping("/auth")
  类上 @SaIgnore        ← 拦截器不查已有会话
    │
    ├─ GET  /client/context      问门开不开（永远 R.ok 形状）
    ├─ POST /login               交卷；缺门 → R.fail；自己不写会话
    ├─ POST /logout              收通行证（finally 仍撕柜）
    ├─ POST /register            办新证；不发通行证
    ├─ GET  /binding/{source}    去第三方门口的纸条（chain 第六扇）
    ├─ POST /social/callback     已登录者绑定第三方（可 insert 或同人同平台 update）
    └─ DELETE /unlock/{socialId} 解绑（本课不认格子）
```

| HTTP | 动词 | Java 方法 | 矩阵格子名 | 成功时写会话？ |
| --- | --- | --- | --- | --- |
| `/auth/login` | POST | `login` | `login` | **是**（在策略里写；Controller **自己不写**） |
| `/auth/logout` | POST | `logout` | `logout` | 删会话（值班员 finally） |
| `/auth/register` | POST | `register` | `register` | **否**（连成功也不写） |
| `/auth/client/context` | GET | `clientContext` | `getClientContext` | 否 |
| `/auth/social/callback` | POST | `socialCallback` | `socialCallback` | **否**（要求已有会话，绑定，不发新 token） |
| `/auth/binding/{source}` | GET | `authBinding` | `socialBinding` | 否 |
| `/auth/unlock/{socialId}` | DELETE | `unlockSocial` | （未认） | 否；要已登录 |

**控制器合同 / 里屋写入（闭合矩阵 (b) 的门卫口径，不拆策略函数体）：**

| (b) 挂在 Controller 名上的行 | 本方法合同（本课必须能说） | 真正动柜的地点（L-013 才拆） |
| --- | --- | --- |
| login 写会话 / 失败不写 | `AuthController.java` **全文零处** `LoginHelper.login`。缺 Client / grant 不含 / 停用 → **`R.fail` 后 return，不调用** `IAuthStrategy.login`，本次无 token-session。过闸才把 raw body 交给策略；成功返回前会话**已经**在策略里写完。口诀是「失败不写会话」，**不是**「失败零存储」。 | 五种 `*AuthStrategy` 调 `LoginHelper.login` → `StpUtil.login` + token-session `"loginUser"` |
| logout 删会话 | Controller **不碰** `StpUtil`。一行 `loginService.logout()`，信封仍是 `R.ok("退出成功")`。 | `SysLoginService.logout` 的 **finally** `StpUtil.logout`。try 里 `loginUser==null` 提前 return **挡不住** finally。 |
| register 写用户 + 密码策略 | Controller **不查 Client、不验密、不写会话**。只 `registerService.register` + `R.ok()`。`RegisterBody.password` **没有** `@NotBlank`，空密码过得了门卫 `@Validated`。 | JDBC `@Transactional` 包 `registerUser` + `grantUserType`；哈希前 `validateOrThrow`。验证码 Redis 删键与 `LOGIN_FAIL` 事件**不随事务回来**。 |

**类比：** 门厅有六扇窗。一扇只回答「这扇门今天开不开」（context）；一扇收试卷发通行证（login）——门卫自己**不**往保险柜塞通行证，过闸才把试卷递进里屋；一扇只办户口本不发通行证（register）；一扇把通行证撕掉（logout），保安即使发现你没票，离开时仍会伸手摸一下保险柜；一扇给你一张去别的公园的门票（binding）；一扇只让**已经进门的人**把公园年卡贴到自己衣服上（social/callback），衣服上已有同园旧贴可以换成新贴。公园年卡**登录**是另一扇窗：还是 `POST /auth/login`，只是试卷上写 `grantType=social`。不要把「贴年卡」和「用年卡进门」说成同一扇窗。

**类比失效处：**

1. 类上 `@SaIgnore` 不是「这些接口谁都能改别人的账号」。它只表示 **Sa-Token 拦截器不挡**。`socialCallback` / `unlockSocial` 自己再喊 `StpUtil.checkLogin()`。登录、注册、context、binding 本来就不该先有会话。
2. context 查不到 Client **不是失败 JSON**。它是成功信封里塞一张「门关着」的纸条。登录查不到 Client 才是 `R.fail`。
3. 通行证不在 Controller 的抽屉里。Controller 过闸之后把试卷交给策略；`LoginHelper.login` 在策略里（L-013）。门卫失败时**根本不叫策略**，所以 Redis 里不会出现这次 token-session。
4. 这不是 SSO 授权码厅。`ssoAuthorizeUrl` 只是 context 里的一块路牌，指向 sso-web 的 `/authorize`（L-055 起）。
5. 注册失败「不落半用户」只对 **MySQL 用户 + 登录域关系**成立。验证码键已经从 Redis 删掉，审计事件也可能已经发出——那些柜子**不回滚**。

## 六扇窗在磁盘上长什么样

这一节是 deep 的硬证据。2026-09-16 对过 `/srv/WTA-plus` 工作树。先对表，再背口诀。

文件：`backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java`。

类注解：`@Slf4j` `@SaIgnore` `@RequiredArgsConstructor` `@RestController` `@RequestMapping("/auth")`。

构造注入（门卫桌上的电话，不是门卫自己的保险柜）：

| 字段 | 类型 | 本课用途 |
| --- | --- | --- |
| `socialProperties` | `SocialProperties`（`justauth`） | binding 查平台是否配置 |
| `loginService` | `SysLoginService` | logout、socialRegister |
| `registerService` | `SysRegisterService` | register |
| `socialUserService` | `ISysSocialService` | unlock 删除绑定 |
| `clientService` | `ISysClientService` | 按 `clientId` 查 `SysClientVo` |
| `notificationService` | `NotificationApplicationService` | 登录成功后尽力发欢迎通知 |
| `passwordPolicyService` | `PasswordPolicyService` | **只**在 context 里贴公示牌 |
| `ssoProperties` | `SsoProperties` | context 里的 SSO 路牌 |

`AuthController` **全文搜不到** `LoginHelper.login(`。`login` 方法只用 `LoginHelper.getUserId()` 给通知当收件人。公示牌 ≠ 登录时重考旧密码：`validateOrThrow` 不在这扇窗。

### 1. `login`：`POST /auth/login`

注解：`@ApiEncrypt`（请求体可加密；`response` 默认 false，响应不加密）。入参是 **`String body`**，不是 `LoginBody`：过滤器先解密，门卫再 `JsonUtils.parseObject(body, LoginBody.class)`，再 `ValidatorUtils.validate(loginBody)`。

`LoginBody`（`wta-common-core`）此刻只认四件事：`clientId`、`grantType`（都 `@NotBlank`）、可选 `code` / `uuid`。用户名密码在 **`PasswordLoginBody`** 里，由密码策略 Bean 再解析（L-013）。门卫这一枪**只看门牌和试卷种类**。

闸门（按源码顺序）：

1. `clientService.queryByClientId(clientId)`（实现带 `@Cacheable(CacheNames.SYS_CLIENT)`）。
2. Client 为 null，或 `!StringUtils.contains(client.getGrantType(), grantType)` → 打 info 日志，**`return R.fail(MessageUtils.message("auth.grant.type.error"))`**。i18n：`认证权限类型错误`。到这里**结束**：不调用 `IAuthStrategy.login`，因此也**不** `LoginHelper.login`。
3. `!SystemConstants.NORMAL.equals(client.getStatus())`（正常值是 **`"0"`**）→ **`return R.fail(MessageUtils.message("auth.grant.type.blocked"))`**。i18n：`认证权限类型已禁用`。同样**不进策略**。
4. 只有过了这两关：`LoginVo loginVo = IAuthStrategy.login(body, client, grantType)`。静态方法按 `grantType + "AuthStrategy"` 找 Bean。找不到 Bean → `ServiceException("授权类型不正确!")`——这已经进了策略入口，仍不写会话。
5. **`LoginHelper.getUserId()` 在通知 `try` 外。** 然后尽力 `notificationService.submit(...)`（`LOGIN_SUCCESS`、站内信、ASYNC）。`catch (RuntimeException)` **只包 `submit`**，只 `log.warn`，**不能把已经成功的登录改成错误**。若 `getUserId` 自己抛错，HTTP 可能失败，但策略里的 token-session **已经在**。
6. `return R.ok(loginVo)`。密码策略 Bean 通常只填 `access_token` / `expire_in` / `client_id`（`LoginVo` 上的 `@JsonProperty`）。公开 VO **还**有 `refresh_token` / `refresh_expire_in` / `scope` / `openid`；策略没 set 就是 null。不要把口诀三字段说成 VO 只有三字段。

`StringUtils.contains` 是**子串包含**（`Strings.CS.contains`），不是把 `grantTypeList` 当枚举 equals。配置 `"password,sms"` 时，`password` 能过门；短针 `"pass"` / `"s"` 也能过门，走进 `IAuthStrategy.login`，再因无 `passAuthStrategy` Bean 改口「授权类型不正确!」。那**不是** `auth.grant.type.error`。`SysClientVo.grantTypeList` 门卫不用。

失败 2、3 **不调用** `IAuthStrategy.login`，因此 **不** `LoginHelper.login`，Redis 无本次会话，通常也**不**写 `sys_login_info`（审计在策略里才记）。这就是 OBJ 要求你能说的那句：**missing/blocked client → `R.fail` before strategy，no session**。不要把「不写会话」说成「失败路径零存储」：注册验证码失败会删 Redis、发 `LOGIN_FAIL`——那是 register 窗，不是这扇 login 红章。

策略内部怎么验验证码、BCrypt、组 `LoginUser`，留给 L-013。Bean 名只需能数：`passwordAuthStrategy`、`smsAuthStrategy`、`emailAuthStrategy`、`socialAuthStrategy`、`xcxAuthStrategy`。

### 2. `logout`：`POST /auth/logout`

无加密注解。无入参。一行委托：`loginService.logout(); return R.ok("退出成功");`。Controller **自己不调用** `StpUtil.logout`。

`SysLoginService.logout` 的门卫层你要能说到这个粒度（再深的失败计数是 L-013）：

- try：读 `LoginHelper.getLoginUser()`；**`loginUser==null` 就 `return`**；有人就记一条 `LOGOUT` 审计。
- catch `NotLoginException`：吞掉。
- **finally：`StpUtil.logout()`**，再吞一次 `NotLoginException`。

所以：没带 token 来喊 logout，信封仍是成功句；try 里提前 return **挡不住** finally 撕柜。前端 `IdentityAccessService.logout` 还会 `session.clear()`（L-014）；服务端这一枪的合同是：**尽量撕掉当前 Sa-Token 会话，信封仍是成功**。不要口述成「没登录就 401」或「try 里 return 了所以会话还在」。

### 3. `register`：`POST /auth/register`

`@ApiEncrypt` + `@Validated @RequestBody RegisterBody user`。`RegisterBody` 继承 `LoginBody`，构造器里 **`setGrantType("password")`**，避免公开注册被 grantType 必填挡住。前端这一枪可以不传 `grantType`，但仍要 `clientId`。**密码字段没有 `@NotBlank` / `@Length`**：门卫 `@Validated` 放行空密码，闸在服务 `passwordPolicyService.validateOrThrow`。

门卫自己**不再**查 Client。全部交给 `registerService.register(user); return R.ok();`。全路径无 `LoginHelper.login`。办好户口本 ≠ 拿到 `access_token`。

服务里（本课只认副作用边界，字段表见 L-012，落库细节见 L-013）：

- Client 空或停用 → 抛「客户端不存在或已停用」（**异常**，不是 Controller 的 `R.fail`）。
- `registerEnabled` 不是 true → 「当前应用未开放注册」。
- 方法上有 `@Transactional(rollbackFor = Exception.class)`。它回滚的是 **JDBC**：`registerUser` + `grantUserType`。失败不落半用户——**只对这两张表成立**。
- `validateCaptcha` 在落库**前** `RedisUtils.deleteObject(verifyKey)`，失败还 `publishEvent(LOGIN_FAIL)`。Redis 键与审计事件**不随事务回来**。不要说「验证码、重名、策略、哈希、插用户都在同一事务」。
- 哈希前才 `validateOrThrow`。空密码、弱密码在这里挡，不插用户、不写会话。

### 4. `clientContext`：`GET /auth/client/context`

无加密。同时吃两种门牌：

- 查询参数 `clientId`（可选）
- 请求头 `clientid`（可选，OAuth 客户端标识；axios adapter 默认就写这个头）

解析：`resolvedClientId = 非空 query ? query : header`。管理端 `prepareLogin` **不带 query**，靠头。

返回类型 `R<AuthClientContextVo>`。**任何分支都是 `R.ok`**，包括「没传 / 查不到」：

| 条件 | `clientEnabled` | `registerEnabled` | `passwordPolicy` | `ssoEnabled` | `authMode` |
| --- | --- | --- | --- | --- | --- |
| 没解析到 clientId | false | false | null | false | `"local"`（方法开头默认） |
| `queryByClientId` 为 null | 同上，直接 return |  |  |  | `"local"` |
| 查到但 `status != "0"` | false | false | **不贴** | false | Client 的 `ssoAuthMode`，空则 `"both"` |
| 查到且正常 | true | 还要 `registerEnabled==true` | `publicProjection()` | 还要全局 SSO 开 **且** Client `ssoEnabled==true` | 同上 |

SSO 路牌：仅当 `ssoEnabled` 为真且 `ssoProperties.webOrigin` 非空，才把 origin 去掉末尾 `/`，拼 **`{origin}/authorize`**。单测 `AuthClientContextSsoUnitTest` 钉过：`http://127.0.0.1:4176` 与带斜杠的 origin 都会变成 `.../authorize`。

`PasswordPolicyBoundaryUnitTest` 钉过：启用 Client 才带公示牌；JSON **不含** `fixedValue` / `generator`；missing client → `clientEnabled=false` 且 `passwordPolicy==null`。

前端 `parseClientAuthContext`：`clientEnabled` 或 `registerEnabled` **任一不是 boolean** 就抛 `client-context-unavailable`；`clientEnabled` 不是 `true` 也抛。后面的 `login()` 也不会发。后端这里仍然是 **200 + `R.ok`**，两个 boolean 都会给。不要把前端抛错说成后端 `R.fail`。

### 5. `authBinding`：`GET /auth/binding/{source}`

`source` 是路径变量（如 `github`、`gitee`）。`socialProperties.getType().get(source)` 为空 → **`R.fail(source + "平台账号暂不支持")`**。有配置则 `SocialUtils.getAuthRequest` + `authRequest.authorize(AuthStateUtils.createState())`，**`return R.data(authorizeUrl)`**（成功信封，data 是字符串 URL）。

这一枪**不要求登录**（类上 `@SaIgnore`，方法内也不 `checkLogin`）。登录页和资料页都会拿这张 URL 然后 `window.location.href = res.data`。state 进 JustAuth/Redis 缓存，本课不拆 `AuthRedisStateCache`。

OBJ-11 口述可以不点这扇；本课 chain **必须点**，因为它是矩阵 (a) 第六名。

### 6. `socialCallback`：`POST /auth/social/callback`

入参 `@RequestBody SocialLoginBody`（`source` / `socialCode` / `socialState`，继承的 `clientId`/`grantType` 在这一枪**不被 Controller 校验**——方法上没有 `@Validated`，也没有 `ValidatorUtils.validate`）。

顺序：

1. **`StpUtil.checkLogin()`**。没通行证：Sa-Token 异常，进不了绑定。类上 `@SaIgnore` 挡不住这一行。
2. `SocialUtils.loginAuth(source, socialCode, socialState, socialProperties)` 去第三方换 `AuthUser`。
3. `!response.ok()` → **`R.fail(response.getMsg())`**，不写绑定。
4. `loginService.socialRegister(authUserData)`：把第三方身份绑到 **当前** `LoginHelper.getUserId()`。三条柜子，不要合成一句「已绑定就失败」：
   - `selectByAuthId(source+uuid)` **非空**（这个第三方账号已被任何人占用）→ 抛「此三方账号已经被绑定!」。
   - 否则查当前 `userId + source`：列表空 → **`insertByBo`**；已有行 → **`updateByBo`**（换绑更新）。注释里的「此平台账号已经被绑定」是**关掉的**，同用户同平台**可以更新**。
5. **`return R.ok()`**，类型 `R<Void>`。**没有** `access_token`。

前端 `IdentityAccessService.socialCallback` 若在 data 里看见 `access_token` 会 `setToken`；那是兼容分支。当前后端这扇窗**不给新票**。没 token 时，登录页的回调组件会走另一条：`socialLogin` → **`POST /auth/login` + `grantType=social`**（`SocialAuthStrategy`，L-013）。未绑定的登录文案是「你还没有绑定第三方账号，**绑定后才可以登录！**」。`SocialCallback/index.vue` 用 `getToken()` 分流：有票 → callback 绑定；无票 → socialLogin。

## 核心概念与机制

### 直觉讲解

你走到大楼门厅。墙上有六扇小窗，上面都写着 `/auth`，但窗口后面不是同一个人。

先问左边那扇「今天开不开」。保安看一眼花名册（`sys_client`）。没写你的门牌、或门牌停用，他仍笑着递一张纸条：`clientEnabled=false`。这不是把你赶出大楼的红章，只是告诉你别排队。前端看到这张纸条会自己停住，不会去交卷。

交卷窗口更凶。试卷上必须有门牌（`clientId`）和种类（`grantType=password` 等）。花名册没有你、种类不在这扇门允许的逗号列表里、或门牌盖了停用章，保安盖 **红章 `R.fail`**，试卷塞回去，**里屋灯都不开**。保险柜（Redis 会话）连锁都不转。只有种类对、门开着，保安才把试卷递进里屋（`IAuthStrategy`）。里屋验验证码、对密码、把通行证放进保险柜——那是里屋的活（L-013）。门卫自己口袋里**没有**盖通行证的章。里屋办完，门卫再尽量塞一张「欢迎光临」的站内信；信没发出去，**通行证仍然有效**。门卫回头找你工号时如果摔了一跤（`getUserId` 在通知 try 外），信封可能变红，但保险柜里的通行证已经放进去了。

办新证窗口（register）会把表格递给里屋办证员。办成了只给你户口本，**不给通行证**。办证员盖章前会撕掉验证码考卷（Redis 键立刻没了）；如果后来发现重名要退回户口本，**撕掉的考卷不会从碎纸机里回来**。你还得再去交卷窗口排一次。

第三方窗口有两步。第一步（binding）给你一张去 GitHub 的路条，谁都能领。你从 GitHub 回来时：身上**已经有**大楼通行证，就去 callback 窗口把 GitHub 贴纸贴到自己衣服上——衣服上若已有同园旧贴，保安会**揭掉旧的贴新的**；若这张 GitHub 卡已经被别人贴走，才喊「已经被绑定」。身上**没有**通行证，就拿着 GitHub 回执去**交卷窗口**，试卷种类写成 `social`。贴纸窗口不发新通行证。

离开时喊 logout。保安尽量把保险柜里你的那一格清掉。你本来就没票，他也说「退出成功」——而且他伸手摸柜这一下，**就算刚才没找到你的名牌也会做**。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 认证控制器 | AuthController | `wta-admin` 上 `@RequestMapping("/auth")` 的 HTTP 门卫；不是 `wta-system` 的用户 CRUD。**自己不调用** `LoginHelper.login` |
| 忽略登录拦截 | `@SaIgnore` | 类级：Sa-Token 拦截器不要求已登录。方法仍可自己 `StpUtil.checkLogin()` |
| 客户端 | Client / `SysClientVo` | `sys_client` 一行：`clientId`、`grantType` 字符串、`status`、`registerEnabled`、SSO 开关。登录和 context 都靠 `queryByClientId` |
| 授权类型 | grant type | 登录 body 的 `grantType`。门卫用 `StringUtils.contains(client.grantType, grantType)` 做**子串包含**，不是 list equals。过门 ≠ Bean 存在 |
| 客户端上下文 | client context | `GET /auth/client/context` 的公开投影：开不开、能不能注册、密码公示牌、SSO 路牌。始终 `R.ok` |
| 登录 | login | `POST /auth/login`：门卫核 Client 后调用 `IAuthStrategy.login`；成功才有 `LoginVo.access_token`。写会话的是策略，不是本方法 |
| 会话 | session | Sa-Token token-session，策略里 `LoginHelper.login` 写入 Redis。Controller 失败路径不调用它；Controller 成功路径也**不亲自**调用它 |
| 统一响应 | `R` | 成功 `ok`/`data`（HTTP 仍常是 200）；业务失败 `fail`（也常是 200 + 非成功 `code`）。缺 Client 的**登录**用 `fail`；缺 Client 的 **context** 用 `ok` + 关旗 |
| 传输加密 | `@ApiEncrypt` | 标在 login/register 上：请求体可加密。login 因此吃 `String` 再 JSON 解析 |
| 社交绑定跳转 | social binding | `GET /auth/binding/{source}` → 授权 URL。Java 名 `authBinding`。OBJ-11 口述可不提；本课 chain 第六扇 |
| 社交回调绑定 | social callback | `POST /auth/social/callback`：已登录用户绑定第三方。不发新 token。同用户同平台已有行则 update |
| 社交登录 | social login | **不是** callback。`POST /auth/login` 且 `grantType=social` → `SocialAuthStrategy` |
| 公开注册 | register | `POST /auth/register`。成功写用户+登录域，**不**写会话。验证码 Redis 不在 JDBC 事务里 |
| 退出 | logout | `POST /auth/logout` → 委托后 **finally** `StpUtil.logout()`；未登录也回成功句 |
| 密码公示牌 | password policy projection | context 在 Client 启用时附带的非敏感规则。不是 login 的闸 |
| 尽力通知 | best-effort notification | 登录成功后 `submit` 欢迎信；异常只 warn `submit`。`getUserId` 在 try 外，不得回滚已写会话 |
| 控制器合同 | controller contract | 矩阵 (b) 在本课的操作定义：失败不调策略故无本次会话；委托删除；委托办证且无票。看见 `StpUtil.login` 行是 L-013 |

**Login ≠ register。** 一个发通行证，一个办户口。

**Context fail ≠ login fail。** 一个关旗仍 `ok`，一个红章 `fail`。两者都不写会话。

**Callback ≠ social grant。** 一个贴贴纸（可更新旧贴），一个用贴纸进门。

**不写会话 ≠ 零存储。** 门卫红章不写会话、通常不记登录审计；办证窗验证码失败会删 Redis、可能记 `LOGIN_FAIL`。

### 机制/因果链

**A. 管理端密码登录（门卫段）。**

1. App 启动时 axios adapter 记下 `VITE_APP_CLIENT_ID`，之后每枪默认头 `clientid`。
2. `GET /auth/client/context`（无 token）。没头也没 query → 关旗 `ok`。有头但库中无行 / 停用 → 关旗 `ok`。前端 `clientEnabled!==true` 停住，**不发** login。
3. 有人绕过前端直打 `POST /auth/login`：解析 `LoginBody` → 查 Client → 空或 grant 不含 → `R.fail(auth.grant.type.error)` **结束，策略零调用**。停用 → `R.fail(auth.grant.type.blocked)` **结束**。
4. 过闸：`IAuthStrategy.login(rawBody, client, grantType)`。密码种类进 `PasswordAuthStrategy`（L-013）：验证码、用户、BCrypt/临时密码、**`LoginHelper.login`**、组 `LoginVo`。
5. 回到 Controller：`getUserId()`（try 外）→ 尽力发 `login-welcome` 通知（只 catch `submit`）→ `R.ok(loginVo)`。
6. 前端把 `access_token` 放进 `Admin-Token` 抽屉（L-014）。再去 `getInfo` / `getRouters`。

因果：门卫红章发生在策略之前，所以「缺 Client」通常连登录日志都没有，也**一定**没有本次 token-session。里屋验证码失败会写审计、仍不写会话——那是 L-006 已认、L-012 再拆的第二闸，不要和门卫红章揉成「失败零存储」。

**B. 注册（不发通行证）。**

1. 前端必须先 `prepareLogin`，且 `registerEnabled===true`，再本地 `validatePassword`（L-012 / L-014）。
2. `POST /auth/register`（可加密）。Controller 不查 Client，直接 `registerService.register`。
3. 服务再查 Client / 开放注册 / 登录域 / 验证码 / 唯一性 / 策略 / 哈希 / 插入。JDBC 任一步抛错 → **用户表 + 登录域回滚**。验证码键若已删，**不回来**。
4. 成功 `R.ok()`，无 token。用户还要走 A。

因果：把注册成功当成已登录，下一枪 `getRouters` 会没有通行证。把「不落半用户」说成「失败路径零存储」，会被已删的验证码键打脸。

**C. 社交两段（绑定 vs 登录）。**

1. `GET /auth/binding/gitee`（或 github）。没配平台 → `R.fail("gitee平台账号暂不支持")`。有配 → 浏览器跳到第三方。
2. 第三方带着 `code`/`state`/`source` 回到前端 `SocialCallback` 页。
3. **无** `getToken()` → `socialLogin` → `POST /auth/login` body 含 `grantType=social`、`socialCode`、`socialState`、`source`。门卫仍走 A 的 Client 闸，然后 `SocialAuthStrategy` 发通行证（L-013）。未绑定会在策略里抛「你还没有绑定第三方账号，绑定后才可以登录！」。
4. **有** token → `POST /auth/social/callback`。先 `checkLogin`，再换 `AuthUser`，再 `socialRegister`：他人已占 `authId` 则抛；本人该 `source` 已有行则 **update**；否则 insert。`R.ok()` 无 token。

因果：把 callback 当成登录，未登录用户会被 `checkLogin` 挡下；把「同平台已有绑定」当成失败，会把合法的 update 误报成错误。

**D. 退出。**

`POST /auth/logout` → `SysLoginService.logout` → try 可能提前 return → **finally 仍 `StpUtil.logout`** → 成功句。前端再清抽屉。没会话也成功。不要把「logout 200」理解成「刚才一定登录过」，也不要理解成「没找到用户所以没撕柜」。

## 图、表或文本图

**图题 / caption：** 门卫六扇窗与两道 Client 闸。alt：login 缺 Client 走 R.fail 且不进策略；context 缺 Client 走 R.ok 关旗；社交绑定可 update；logout finally 撕柜。

```text
                    ┌──────────────────────────────────────────┐
                    │ AuthController  /auth   @SaIgnore        │
                    │ 全文无 LoginHelper.login(                 │
                    └──────────────────────────────────────────┘
          GET /client/context                 POST /login
          query clientId | header clientid    @ApiEncrypt  String body
                 │                                    │
                 │ 无 id / 查无 / 停用                 │ 解析 LoginBody
                 │ → R.ok 旗全关  ──x── 不写会话        │ queryByClientId
                 │                                    │ 空或不含 grantType
                 │ 正常 Client                        │ → R.fail error ──x── 不调策略
                 │ → 开旗 + 公示牌                     │ status != "0"
                 │   + 可选 SSO 路牌                   │ → R.fail blocked ──x── 不调策略
                 │                                    │
                 │                                    v
                 │                          IAuthStrategy.login   （L-013）
                 │                          LoginHelper.login → Redis
                 │                          回到门卫：getUserId（try 外）
                 │                          尽力通知（只 catch submit）
                 │                          R.ok(LoginVo.access_token)
                 │
 POST /register @ApiEncrypt          POST /logout
 → SysRegisterService                → SysLoginService.logout
   JDBC 事务：用户+登录域              try: 无用户则 return
   Redis 验证码键：先删不回滚          finally: StpUtil.logout  ← 仍执行
   成功不写会话                       信封：退出成功
                 │
 GET /binding/{source}               POST /social/callback
 无配置 → R.fail                      StpUtil.checkLogin()
 有配置 → R.data(authorizeUrl)        SocialUtils.loginAuth
 不写会话                             失败 → R.fail
 （chain 第六扇）                     socialRegister：
                                      他人占 authId → 抛
                                      本人同 source 已有行 → update
                                      否则 insert
                                      R.ok()  无新 token

 另一条（不是 callback 这扇窗）：
 POST /login  grantType=social  → SocialAuthStrategy → 发通行证（L-013）
```

**文字等价物：** 图顶是带 `@SaIgnore` 的 `AuthController`，并且标明全文不调用 `LoginHelper.login`。左边 `GET /auth/client/context` 无论有没有 Client 都回成功信封；没有可用 Client 时旗子关掉，绝不写会话。右边 `POST /auth/login` 在策略之前用红章挡住三种 Client 问题：查无、grantType 不包含、状态不是 `"0"`。只有过闸才进入 `IAuthStrategy`，由策略写 Redis 会话，再回到门卫读 `userId`（在通知 try 外）并发尽力通知。下排注册成功只落用户；验证码 Redis 先删且不随 JDBC 回滚。logout 的 try 提前 return 仍走进 finally 撕会话。社交左侧 binding 只给外链；右侧 callback 要求已登录，成功路径是插入或同用户同平台更新，不发新票。用社交账号进门必须回到 login 窗并写 `grantType=social`。

**图的边界：** 不画验证码 Redis 键形、BCrypt、临时密码、锁定计数（L-012 / L-013）。不画前端 session 钥匙名如何注入（L-010 / L-014）。不画 SSO `/sso/oauth2/authorize` 授权码。不把 `DELETE /auth/unlock/{socialId}` 画成主路径。欢迎通知失败不在图上改登录结果。不把五种策略的 `buildLoginUser` 画进门厅。

**图题 / caption：** 登录失败时哪些柜子动。alt：缺 Client 时会话柜空且策略未调用；策略失败才可能写审计；注册验证码失败会动 Redis。

```text
闸门（本课门卫，login 窗）
  Client 空 / grant 不含 / status 停用
        \____ R.fail
        \____ 不调用 IAuthStrategy.login
        \____ Redis 无本次 token-session
        \____ 通常不写 sys_login_info

闸门（里屋，L-012/L-013，本课只标位置）
  验证码 / 密码 / 锁定 / 无社交绑定
        \____ 仍不 LoginHelper.login
        \____ 可能 recordLoginInfo

闸门（register 窗，别和 login 红章混）
  验证码错 / 过期
        \____ Redis 键已删
        \____ 可能 LOGIN_FAIL 事件
        \____ sys_user 仍空（事务未插或已回滚）

汇：没有 access_token 就不能 getRouters
```

**文字等价物：** 本课认死的 login 失败是门卫红章：请求在策略 Bean 之前退回，会话柜空，审计柜通常也空。里屋还有别的闸，那些闸可能在访客簿上记一笔，但仍然不发通行证。register 验证码失败会动 Redis，那不是 login 这扇窗的柜子。不要用「返回了 JSON」当登录成功——要看是不是 `R.ok` 且带 `access_token`。

**图的边界：** 「登录成功但 Redis 读失败」是 L-084，不是缺 Client。密码策略挡注册是 L-012，本课只要求知道 register 成功也不写会话，且验证码键不在 JDBC 事务里。

## 正例、反例与边界

**正例 1：** 口述登录闸。打开 `AuthController.login`：先 `queryByClientId`，空或不含 grantType → `R.fail("auth.grant.type.error")`；`status` 不是 `SystemConstants.NORMAL`（`"0"`）→ `R.fail("auth.grant.type.blocked")`；然后才 `IAuthStrategy.login`。全文件搜 `LoginHelper.login`：**零处**。缺 Client 的失败**到不了**策略。

**正例 2：** 直打登录缺 Client。不经过前端也能复现：body 里编造 `clientId`，门卫红章，策略 Bean 零调用，无本次 token-session。这是 L-006 失败 A 在函数级的落点。

**正例 3：** context 缺 Client 仍 200。`controller.clientContext("missing", null)` 得到 `clientEnabled=false`、`passwordPolicy=null`（`PasswordPolicyBoundaryUnitTest`）。前端会当成 `client-context-unavailable`，**后端不是 `R.fail`**。

**正例 4：** context 门牌两种吃法。Java 签名：`@RequestParam clientId` 与 `@RequestHeader("clientid")`。query 优先。admin-web 这一枪通常只带头。后端两个 boolean 都会给；前端缺任一 boolean 也抛。

**正例 5：** 启用 Client 才贴公示牌。同上单测：正常且 `registerEnabled=true` 时 JSON 有 `minimumLength`，没有 `fixedValue`。停用/缺失不贴。

**正例 6：** 注册成功无 token，且 Redis 不在 JDBC 事务里。`register` 方法体只有 `registerService.register` + `R.ok()`。`SysRegisterService` 无 `LoginHelper.login`。`validateCaptcha` 先 `deleteObject` 再可能 `LOGIN_FAIL`；`@Transactional` 回滚的是后续 `registerUser` + `grantUserType`。

**正例 7：** logout 委托且 finally 必撕。`logout()` 不自己碰 `StpUtil`；`SysLoginService.logout` 的 try 里 `loginUser==null` 会 return，**finally 仍** `StpUtil.logout`，吞 `NotLoginException`。

**正例 8：** 社交分流 + 同人同平台可更新。`SocialCallback/index.vue`：`getToken()` 空 → `socialLogin`（`/auth/login`）；有票 → `socialCallback`（`/auth/social/callback`）。`socialRegister`：他人占 `authId` 才抛「此三方账号已经被绑定!」；本人该 `source` 已有行走 `updateByBo`。

**正例 9：** 登录欢迎信失败不翻盘，但 `getUserId` 不在 catch 里。`submit` 的 `catch (RuntimeException)` + warn。会话已经在策略里写完。`getUserId` 若抛，HTTP 失败 ≠ 会话被回滚。

**正例 10：** SSO 路牌只出现在 context。`ssoEnabled` 要三重真：Client 正常、`namewta.sso.enabled`、Client `ssoEnabled`。URL 是 `{webOrigin}/authorize`，不是本控制器去跑 OAuth。

**正例 11：** 子串过门 ≠ Bean 存在。`"password,sms".contains("pass")` 为真，门卫不盖 `auth.grant.type.error`，策略入口抛「授权类型不正确!」。

**反例 1：** 「`/auth/*` 都在 `wta-system`。」磁盘在 `wta-admin`。system 模块管用户/角色/Client **配置**，不管这六扇窗。

**反例 2：** 「context 查无 Client 应 `R.fail`，和 login 一样。」源码对 context 是 `R.ok` 关旗。前端停住是前端的事。

**反例 3：** 「`@SaIgnore` 所以 callback 不需要登录。」方法第一行 `StpUtil.checkLogin()`。忽略拦截 ≠ 忽略方法内检查。

**反例 4：** 「`POST /auth/social/callback` 会返回 `access_token` 并登录。」返回 `R<Void>`。发通行证的是 `POST /auth/login` + `grantType=social`。

**反例 5：** 「注册成功就已登录。」没有 `access_token`，前端也没有在 `register()` 里 `setToken`。

**反例 6：** 「login 里会跑密码策略。」Controller 只在 context 调 `publicProjection()`。`validateOrThrow` 在注册（和改密）路径。已有用户登录比对的是哈希/临时密码（L-013）。

**反例 7：** 「把门卫写成 `LoginHelper.login`。」那一行在策略实现里，例如 `PasswordAuthStrategy` 在 `buildLoginUser` 之后。缺 Client 的失败**到不了**那里。矩阵 (b) 的「login 写会话」指控制器合同：成功返回前由策略写入；不是本方法体里有这一行。

**反例 8：** 「binding 需要 token，因为它在资料页出现。」登录页 `doSocialLogin` 同样打 `/auth/binding/{source}`。方法内无 `checkLogin`。

**反例 9：** 「六个 Java 名就是格子名。」`clientContext` ≠ `getClientContext`；`authBinding` ≠ `socialBinding`。HTTP 才是跨语言合同。

**反例 10：** 「把 `DELETE /auth/unlock/{socialId}` 说成本课认过的格子。」它在同文件，要登录，失败句是「取消授权失败」。矩阵本课只认六扇。解绑列表页是 L-022。

**反例 11：** 「grantType 用 list equals 匹配，错种类一定是 `auth.grant.type.error`。」门卫读的是 `client.getGrantType()` **字符串**，`StringUtils.contains`。短针过门后文案变成「授权类型不正确!」。

**反例 12：** 「通知失败等于登录失败。」源码注释写明 best effort。把欢迎信当事务参与者，会把已经发出的通行证当错误回滚——Controller **故意不这么做**。`getUserId` 也不在那个 catch 里。

**反例 13：** 「验证码、插用户、授登录域都在同一事务，失败零存储。」JDBC 事务只管用户+登录域。验证码键先删不回滚；`LOGIN_FAIL` 事件也不回滚。

**反例 14：** 「callback 成功 = 插入；已有绑定 = 失败。」他人占 `authId` 才失败。本人同平台已有行是 **update**。

**反例 15：** 「OBJ-11 的五扇 = 矩阵六名，所以可以不提 binding。」OBJ 口述五扇；本课 chain **加认** binding。漏第六扇则 (a) 不满。

**边界：**

- sso-web 的 `POST /sso/login` 不是这六扇窗（L-057）。context 里的 `ssoAuthorizeUrl` 只是路牌。
- home-web 同样打 `/auth/client/context` 与 `/auth/login`，Client 可能是另一块门牌；门卫逻辑相同。
- Captcha 三个 code 入口是 `CaptchaController`，L-012。
- `IAuthStrategy.buildLoginParameter`、锁定计数、`buildLoginUser`、`LoginHelper.login` 函数体是 L-013。
- 前端 `prepareLogin` / session 钥匙是 L-014。
- OpenAPI 生成类型里的 `socialCallback` 名字不能代替源码返回 `Void` 这一事实。
- 矩阵 (b) 若被理解成「必须看见 `StpUtil.login` 那一行」，那一行在 L-013；本课闭合的是**控制器合同**。

## 变式与迁移

- **变式 A：新加一种登录种类（例如新的 OTP）。** 先在 Client 的 `grantType` 字符串里允许它，再提供 `"{grantType}AuthStrategy"` Bean。不要在 `AuthController.login` 里写 if-else 分支。门卫只核门牌和种类是否包含。短针会被 contains 放进策略入口——Bean 名要精确，不要靠「看起来像子串」。
- **变式 B：某个 App 不允许密码登录、只允许 SSO。** 改 Client 的 `ssoAuthMode` / `ssoEnabled` 与全局 `namewta.sso.enabled`。context 会把路牌和 `authMode` 交给前端。不要删 `POST /auth/login`。
- **变式 C：关闭某 Client 的公开注册。** `registerEnabled=false`。context 的 `registerEnabled` 变 false；服务里再挡一层。前端应不发 register。不要靠「藏按钮」当唯一闸。
- **变式 D：停用 Client。** `status` 调成非 `"0"`。login 红章 `blocked`；context 关旗但仍 `ok`。缓存键是 `SYS_CLIENT`+clientId，改完要知道缓存可能短时旧值（实现细节归 Client 课 L-018）。
- **变式 E：只绑定、不提供社交登录。** 资料页走 binding + callback。登录页不要放社交按钮，或让 Client 的 grantType **不含** `social`，让社交 login 在门卫就被 `contains` 挡住。同用户换绑走 callback 的 update，不要当成失败。
- **变式 F：登录请求要加密。** 保持方法上 `@ApiEncrypt` 和 `String body`。改成直接 `LoginBody` 参数会在加密过滤器之后对不上原文。前端 `isEncrypt` 与 App 的 `VITE_APP_ENCRYPT` 对齐（L-007 / L-014）。
- **变式 G：欢迎通知不想发了。** 那是 Controller 里的尽力调用，不是策略写会话的一部分。关掉或失败不得让 `R.ok(loginVo)` 变成 fail。也不要把 `getUserId` 塞进「失败就当没登录」。
- **变式 H：注册要「失败一切还原」。** 当前做不到：验证码 Redis 先删。若产品要「错一次码还能再用同一张图」，得改 `validateCaptcha` 的删键时机，而不是给 Controller 加事务。
- **迁移口诀：** 先问门（context，ok 关旗）→ 再交卷（login，fail 则无会话、且门卫自己不写会话）→ 里屋写通行证（L-013）→ 注册只办证不发通行证，验证码键不回滚 → 社交纸条（binding）与贴纸（callback，可 update）分窗 → 用贴纸进门仍走 login → logout 的 finally 必撕柜。跳步就会出现「前端以为失败、后端其实 ok」或「callback 被当成登录」或「Controller 里找不到 `LoginHelper.login` 就以为课讲错」。

## 常见误区

1. **「认证在 system 模块。」** HTTP 门卫在 `wta-admin` 的 `AuthController`。system 提供 Client/用户/社交表的服务。
2. **「缺 Client 一律 `R.fail`。」** 只对 **login**（以及 register 服务抛异常）成立。**context 缺 Client 是 `R.ok` + 关旗。**
3. **「`@SaIgnore` = 匿名可绑定别人的社交账号。」** callback/unlock 自己检查登录。
4. **「六扇窗都会写会话；或 login 这扇窗自己写会话。」** 会写的只有 login **成功**，而且写在策略里。Controller 零处 `LoginHelper.login`。register/context/binding/callback 都不发新会话；logout 删除。
5. **「`social/callback` 就是社交登录。」** 登录是 `grantType=social` 的 `/auth/login`。callback 是已登录绑定。
6. **「格子名 `getClientContext` / `socialBinding` 就是 Java 方法名。」** Java 是 `clientContext` / `authBinding`。
7. **「logout 没登录应 401；try 里 return 了会话还在。」** 当前实现回「退出成功」；finally 仍 `StpUtil.logout`。
8. **「login 返回了就等于通知一定进了收件箱。」** 通知 ASYNC 且 catch 掉 `submit`。`getUserId` 还不在 catch 里。
9. **「context 的公示牌会在登录时强制校验。」** 公示牌给页面画规则；登录 Controller 不调 `validateOrThrow`。
10. **「binding 的 `R.data(url)` 和 login 的 `R.ok(vo)` 一种东西，都算登录成功。」** 前者 data 是外链字符串，没有 `access_token`。
11. **「前端 `socialCallback` 测试里出现过 `access_token`，所以后端也会发。」** 那是前端兼容假响应。当前 Java 返回 `R.ok()` 无 data token。
12. **「header 必须叫 `clientId`。」** context 认的头是小写 **`clientid`**。query 才是 `clientId`。login body JSON 字段是 `clientId`。
13. **「把 `IAuthStrategy` 的 Bean 选择说成 Controller 的 if。」** Controller 只传 `grantType` 字符串。找不到 Bean 是策略静态方法里的 `ServiceException`。
14. **「注册 body 必须带 grantType。」** `RegisterBody` 构造器默认 `"password"`，就是为了不让继承来的必填挡住公开注册。密码够不够长不在门卫校验。
15. **「失败不写会话 = 失败零存储；验证码在同一事务。」** login 红章通常连审计都没有；register 验证码失败会删 Redis、发事件。
16. **「同用户同平台再绑一次会失败。」** `socialRegister` 走 `updateByBo`。他人已占 `authId` 才抛。
17. **「OBJ-11 没写 binding，所以本课可以不认。」** 本课 chain / 矩阵 (a) **加认**第六扇。OBJ 口述五扇仍然对。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `AuthController.java` 类注解。数清 `@RequestMapping("/auth")` 下每一对 HTTP 映射。把 Java 方法名写在左边，矩阵格子名写在右边，圈出两个对不上的名字。再在纸上标：哪五扇是 OBJ-11 口述，哪一扇是 chain 加认，哪一扇本课不认。
2. 用手指划 `login`：从 `parseObject` 到第一个 `IAuthStrategy.login` 之间，有几个 `return R.fail`。对每个 fail，说出会不会调用 `LoginHelper.login`（文件里搜得到吗？）。再看 `getUserId()` 在不在 `try` 里。
3. 对照 `clientContext` 的两个入口参数和默认 VO。把「没传 clientId」和「login 没传 clientId」的信封差别说出来：谁是 `ok` 关旗，谁会在 validate / fail 处停住。
4. 打开 `SocialCallback/index.vue` 的 `getToken()` 分支，对照 `socialCallback` 与 `socialLogin` 两个 URL。打开 `socialRegister`：圈出 `selectByAuthId` 抛错、`insertByBo`、`updateByBo` 三岔口。再打开 `authBinding` 确认它只返回 URL。
5. 打开 `SysRegisterService.register` 搜 `LoginHelper` 和 `RedisUtils.deleteObject`。打开 `SysLoginService.logout` 看 finally。想一句：哪扇窗发通行证，通行证是谁写的，哪扇窗撕通行证（finally 挡不挡得住），哪扇窗只办户口，验证码键回不回滚。

## 总结、词汇表与下一步

- **门卫在 `wta-admin` 的 `AuthController`，前缀 `/auth`，类上 `@SaIgnore`。** 六扇认过的窗：login / logout / register / client/context / social/callback / binding/{source}。OBJ-11 口述前五扇；本课 chain **加认** binding。同文件 `unlockSocial` 不认格子。
- **login：** 核 Client 与 grantType；缺或封 → `R.fail`，**不进策略、不写会话**。本方法 **零处** `LoginHelper.login`。过闸 → `IAuthStrategy.login`（L-013 写会话）→ `getUserId` 在通知 try 外 → 尽力 `submit` → `R.ok(LoginVo)`。
- **client/context：** 始终 `R.ok`。没门牌或门关着只关旗，贴不贴公示牌取决于 Client 是否正常。头名 `clientid`，query 名 `clientId`。
- **register：** 委托服务；成功落用户，**不发 token**。JDBC 失败不落半用户；验证码 Redis 删键与 `LOGIN_FAIL` **不回滚**。
- **logout：** 委托删除；**finally 仍** `StpUtil.logout`，未登录也成功句。
- **binding：** 没配平台 `R.fail`；有配给授权 URL。**callback：** 要已登录；他人占 `authId` 则抛；同用户同平台已有行则 **update**；否则 insert；`R.ok()` 无新票。社交**登录**仍走 login + `grantType=social`。
- 格子名 `getClientContext` / `socialBinding` 对源码 `clientContext` / `authBinding`。矩阵 (b) 本课按**控制器合同**闭合，不把五种策略函数体搬进来。

词汇表：AuthController / `@SaIgnore` / Client / grant type / client context / login / session / controller contract / `R.fail` / `@ApiEncrypt` / social binding / social callback / social login / register / logout / password policy projection / best-effort notification。

下一步：L-012 拆 `CaptchaController` 三个 code 入口和密码策略如何挡住非法注册。L-013 进里屋：`IAuthStrategy` / `SysLoginService` / `SysRegisterService` 的副作用与失败路径——包括 `LoginHelper.login` 函数体。L-014 从前端 `IdentityAccessService` 再打回这六扇窗。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-002 | `backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | 六扇窗映射；login 两道 `R.fail` 在 `IAuthStrategy.login` 之前；全文无 `LoginHelper.login`；`getUserId` 在通知 try 外；context 始终 `ok`；callback `checkLogin`；binding 无配置 `fail`；register/logout 委托；`@SaIgnore` / `@ApiEncrypt`；第七扇 `unlockSocial` 不认 | 类与各方法 | 2026-09-16 |
| S-005 | `frontend/apps/admin-web` 的 `views/login.vue`、`layout/components/SocialCallback/index.vue`、`views/system/user/profile/thirdParty.vue` | 登录页打 binding；回调页按 `getToken()` 分流 callback vs socialLogin；资料页 binding/unlock | 对应函数 | 2026-09-16 |
| S-L011-01 | `AuthClientContextVo.java`、`LoginVo.java`、`LoginBody.java`、`RegisterBody.java`、`SocialLoginBody.java`、`SysClientVo.java` | 上下文字段；token JSON 名（含未填则为 null 的 refresh/scope/openid）；register 默认 grantType；password 无 `@NotBlank`；Client status/grantType/registerEnabled/sso 字段 | 各类型声明 | 2026-09-16 |
| S-L011-02 | `backend/wta-admin/src/main/java/org/namewta/web/service/IAuthStrategy.java` 及 `impl/*AuthStrategy.java` 的 `@Service` 名 | 门卫把 raw body + client + grantType 交给 `grantType + "AuthStrategy"`；无 Bean →「授权类型不正确!」；`LoginHelper.login` 只在策略实现里；本课不拆里屋 | `BASE_NAME`、`login` 静态方法、五份 `LoginHelper.login` | 2026-09-16 |
| S-L011-03 | `SysLoginService.java`（`logout` / `socialRegister`）、`SysRegisterService.java`（`register` / `validateCaptcha`） | logout 的 finally 必 `StpUtil.logout`；callback 绑定：`authId` 冲突抛错、同用户同平台 `updateByBo`；register JDBC 事务包用户+登录域，Redis 删键与 `LOGIN_FAIL` 不回滚；无 `LoginHelper.login` | 对应方法 | 2026-09-16 |
| S-L011-04 | `AuthClientContextSsoUnitTest.java`、`PasswordPolicyBoundaryUnitTest.java` | SSO URL 去斜杠；missing client 关旗无公示牌；启用 Client 才带 `publicProjection` | `@Test` 方法名 | 2026-09-16 |
| S-L011-05 | `frontend/packages/domains/admin/src/index.ts`、`index.test.ts`、`transport.ts` | 前端 URL 表；`parseClientAuthContext` 要求两个 boolean，关旗当错误；callback 兼容 token；register 不 setToken | `createIdentityAccessService` | 2026-09-16 |
| S-L011-06 | `SocialUtils.java`、`SocialProperties.java`、`SsoProperties.java`、`SystemConstants.java`、`R.java`、`StringUtils.contains`、i18n `auth.grant.type.*`、`LoginHelper.login` | JustAuth 换票；`justauth` 配置 map；SSO origin；`NORMAL="0"`；`R.ok`/`fail`/`data`；红章文案；子串包含；会话写入点不在 Controller | 各类型与 `messages.properties` | 2026-09-16 |
