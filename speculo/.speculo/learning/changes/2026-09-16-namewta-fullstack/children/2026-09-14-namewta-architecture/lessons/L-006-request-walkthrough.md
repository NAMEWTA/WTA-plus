---
lesson_id: L-006
objective_ids: [OBJ-06]
claimed_cells: [D:happy-login-query, D:fail-auth]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 14
  - segment: fail-path-and-storage
    minutes: 7
  - segment: visuals-and-anti-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 3
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-005, S-006, S-008, S-020, S-021, S-022, S-023, S-024, S-025, S-026, S-027, S-028]
---

# Lesson 006：主路径与失败路径：从登录走到 Mapper

## 学完你能做什么

你能按层口述一条**管理端主路径**：浏览器准备登录 → `GET/POST /auth/*` → 成功才写 Sa-Token 会话（Redis）→ **`GET /system/menu/getRouters`** 拉动态路由 → 领域列表 `GET` → Controller →（UseCase|）Service → Mapper → MySQL。

矩阵格子里的前缀 `GET /system/menu` **不是**本课菜单跳的操作定义。拼动态路由的那一枪是 `SysMenuController` 的 **`GET /system/menu/getRouters`**。菜单管理 CRUD 才是 `GET /system/menu/list`（还要 `system:menu:list`）。不要把前缀说成已经走到了菜单。

领域列表的层链不因菜单跳而缩短：登录之后仍然是 Controller → Service 或 UseCase → Mapper → MySQL。本课主走廊用 layered 的 `GET /notify/notice/list`（core 剖面也有）。classic 的 `GET /demo/demo/list` 是**可选第二走廊**（classpath 有 `wta-demo` 时才走）；`/list` 走 wrapper，不是 XML 里的 `customPageList`。

你还能指出**失败路径的存储点**：缺 Client、验证码失败、注册上的密码策略拒绝时**不得写 Sa-Token 会话**。验证码失败仍可能记登录日志（`sys_login_info`）——那是**审计柜**，不是 token-session。你至少能说出一种层间抄近路（页面自己 axios，或 Controller 注入 Mapper）。

本课覆盖格子是 `D:happy-login-query` 与 `D:fail-auth`。本课不宣称你已经会改 SSO PKCE、验证码限流内部、或 `IAuthStrategy` 每一个 grantType。那些是 L-011 起的切片。

## 先把宏观地图放在桌上

前面五课认房间、楼层、合同。本课把它们串成一条**会动的走廊**。Goal 的主路径不是「随便一条 GET」，而是：

```text
准备登录（client/context + 验证码）
    → POST /auth/login
    → 会话进 Redis
    → GET /system/menu/getRouters（动态路由；不是 /system/menu/list）
    → 领域列表 GET
         主走廊：GET /notify/notice/list
         可选第二 classic：GET /demo/demo/list（有 wta-demo 时）
    → Mapper（列表常走 wrapper；XML 可能是另一条路径）
    → MySQL
    → web-domain 表格
```

认证入口不在 `wta-system`。2026-09-16 工作树里，`AuthController` 与 `CaptchaController` 都住在 `backend/wta-admin`：

| HTTP | 方法 | 类 | 磁盘位置 |
| --- | --- | --- | --- |
| `/auth/login` | POST | `AuthController.login` | `wta-admin/.../AuthController.java` |
| `/auth/logout` | POST | `AuthController.logout` | 同上 |
| `/auth/register` | POST | `AuthController.register` | 同上 |
| `/auth/client/context` | GET | `AuthController.clientContext` | 同上 |
| `/auth/social/callback` | POST | `AuthController.socialCallback` | 同上 |
| `/auth/binding/{source}` | GET | `AuthController.authBinding` | 同文件，本课不展开 |
| `/auth/code` | GET | `CaptchaController.getCode` | `wta-admin/.../CaptchaController.java` |
| `/resource/sms/code` | GET | `CaptchaController.smsCode` | 同上 |
| `/resource/email/code` | GET | `CaptchaController.emailCode` | 同上 |

动态路由是 **`GET /system/menu/getRouters`**（`SysMenuController`，`wta-system`，classic）。同一控制器上的 `GET /system/menu/list` 是菜单管理，权限注解是 `@SaCheckPermission("system:menu:list")`；`getRouters` **没有**这条权限注解，只读已登录的 `LoginUser` 与 `clientPk`。

领域列表两条对照：

- **主走廊（layered）：** `GET /notify/notice/list`。Controller 只持 UseCase。列表 SQL 是 DAO wrapper，不是 XML 里那条 `selectByIdForUpdate`。
- **可选第二走廊（classic）：** `GET /demo/demo/list`。层少，适合对照 classic。`/list` 走 `selectVoPage`（wrapper + `@DataPermission`）；XML 的 `customPageList` 属于 **`GET /demo/demo/page`**。`bundle-core` 可能不组装 `wta-demo`，所以它是可选，不是人人必走的枪。

**类比：** 进大楼要先在门厅换通行证（登录），再拿楼层导览图（`getRouters`），才能进一间办公室查档案（领域 GET）。通行证放在门厅保险柜（Redis 会话），档案在地下仓库（MySQL）。门厅还可能在访客簿上记一笔失败（登录日志）——记过不等于发了通行证。

**类比失效处：** 真实请求还有加密登录体、数据权限、分页包装、通知 Outbox。本课把它们标成走廊墙上的消防栓。不要把「换通行证」理解成 SSO 授权码；那是另一栋厅（`sso-web` / `/sso/oauth2`）。也不要把 `GET /system/menu/list` 当成登录后拉动态路由的那一跳。不要把「走到 Mapper XML」理解成「这条列表 GET 必须执行 XML 里的某个 `<select>`」——合同面是 Mapper；列表枪常常走 wrapper。

## 核心概念与机制

### 直觉讲解

你打开管理端，先填用户名密码。页面**不会**自己冲到数据库，也**不会**在验证码还没对上时先发一张通行证。

它像传话：

1. 门厅先问：「这扇门（Client）开不开？验证码画板上有没有题？」→ `GET /auth/client/context`、`GET /auth/code`。Client 标识走请求头 `clientid`（App 的 axios adapter 注入），不是 domain 把 `clientId` 写进 URL。
2. 你交卷 → `POST /auth/login`。门卫先核对 Client 和 grantType。不对，当场退回，**保险柜不上锁、不放通行证**。
3. 密码策略这条规矩主要管「办新证」（注册/写密码），不在已有用户登录时再把旧密码拿去重考一遍。注册被拒同样**不写会话、不落半个用户**。注册**成功**也只办好用户证，**不会**顺便塞一张通行证。
4. 全对了，门卫把通行证放进 Redis 保险柜，把令牌纸条给你。
5. 你拿纸条去要导览图 → **`GET /system/menu/getRouters`**。
6. 点开公告列表 → web-domain 喊 domain → **`GET /notify/notice/list`** → 门卫（Controller）只找管家（UseCase）→ 厨师（Service）→ 仓库管理员（DAO）→ Mapper → MySQL。
7. 若这套包还装着演示模块，可以再走可选第二走廊：`listDemo` → `GET /demo/demo/list` → classic 厨师（ServiceImpl）→ Mapper 的 wrapper。别把 XML 里的 `customPageList` 说成这一枪。

如果你让厅堂直接对数据库喊，或让门卫跳过厨师去开仓库，传话会快一秒，明天别人不知道该改哪一站。

### 精确定义与 English term

| 中文 | English | 精确定义（本课走查，以工作树为准） |
| --- | --- | --- |
| 主路径 | happy path | 登录成功写会话后，能拉动态菜单并完成一条只读领域 GET，直到 Mapper 碰 MySQL |
| 失败路径 | fail path | 缺 Client / 验证码失败 / 密码策略拒绝写密：认证失败，**不**调用 `LoginHelper.login`，Redis 里不出现该次登录会话 |
| 客户端上下文 | client context | `GET /auth/client/context` 返回的 `clientEnabled` / `registerEnabled` / 密码策略投影；前端 `prepareLogin` 必须先拿到且 `clientEnabled=true` |
| 图形验证码 | captcha | `GET /auth/code` 把答案写入 Redis；登录时按 uuid 取出并删除。开关在 `CaptchaProperties` |
| 会话 | session | Sa-Token：`LoginHelper.login` → `StpUtil.login` + token session 里放 `LoginUser`；持久化走 `PlusSaTokenDao`（Redis + 短时 Caffeine） |
| 动态路由 | dynamic routers | 登录后 **`GET /system/menu/getRouters`**；按当前用户与 `clientPk` 组菜单树，**不是**菜单管理的 `/list`，也不是前缀 `/system/menu` |
| 领域列表 | domain list GET | 无界面 domain 服务发出的只读查询。本课主走廊：`GET /notify/notice/list`。可选 classic：`listDemo` → `GET /demo/demo/list` |
| 映射器 / XML | Mapper / Mapper XML | 持久化合同的主人。SQL 可能在 XML，也可能在 Mapper 接口的 wrapper / QueryBuilder。**「走到 Mapper XML」= 碰到该 namespace 的 SQL 合同面**，不是「这条 GET 必须执行 XML 里的某个 `<select>`」。空 XML ≠ 不碰库 |
| 抄近路 | layer skip | 跳过合法主人：页面直写 URL、Controller 注入 Mapper、App 复制 domain 类型 |
| 存储点 | storage point | 本课要能指出来的柜子：Redis 验证码、Redis 会话、MySQL 用户行、MySQL 业务行、前端 session store。登录日志 `sys_login_info` 是**审计柜**，失败时仍可能写入，**不等于**会话 |
| 登录日志 | login-info audit | `recordLoginInfo` → 事件 → `SysLoginInfoServiceImpl.insertLoginInfo` 落 `sys_login_info`。验证码失败会写；缺 Client 在 Controller 直接 `R.fail` 时通常不写。它不是 token-session |

**Walkthrough** 不是调试器截图。它是你能用层名讲完、并能指到工作树文件的一条因果链，并且知道失败时哪一个柜子保持空（会话柜），哪一个柜子仍可能有字（审计柜）。

第三闸的操作定义是：**任何被密码策略拒绝的写密（本课点注册）都不会出现新会话**。不是「登录请求被策略拒绝」——`AuthController.login` **没有**调用 `passwordPolicyService.validateOrThrow`。格子名叫认证拒绝，注册失败算进「不会出现新会话」这一判决，不要把它说成登录时重验旧密码。

### 机制/因果链

#### 主路径 `D:happy-login-query`

按点击时间顺序。App 组合已经在启动时发生：`admin-web` 的 `application/services.ts` 创建 `identityAccessService`（注入 `systemService.identity` 与 `session`）以及通知、演示等 domain。没有这一步，登录页和菜单都接不上。

1. **准备登录。** 前端 `createIdentityAccessService().prepareLogin()` 连续两枪，都带 `isToken: false`：
   - `GET /auth/client/context` → `AuthController.clientContext`。后端同时接受查询参数 `clientId` 或请求头 `clientid`。**管理端这一枪不带 query**：`admin-web` 的 `application/http.ts` 把 `VITE_APP_CLIENT_ID` 交给 `createAxiosBrowserAdapter({ client })`，适配器默认头就是 `clientid`。找不到 Client 时返回 `clientEnabled=false`，**不是** 500。启用时附带 `passwordPolicyService.publicProjection()`。
   - `GET /auth/code` → `CaptchaController.getCode`。开关关闭则 `{ captchaEnabled: false }`；开启则生成 uuid，答案写入 Redis 键 `CAPTCHA_CODE_KEY + uuid`。
   - 前端 `parseClientAuthContext`：`clientEnabled` 不是 `true` 就抛 `client-context-unavailable`，后面的 `login()` 也不会发。`login()` 还要求 `prepared===true`（必须先 `prepareLogin`）。

2. **提交登录。** `POST /auth/login`，body 含 `username` / `password` / 可选 `code`+`uuid` / `clientId` / `grantType: 'password'`。类上有 `@SaIgnore`，所以这一枪不带已有会话。`@ApiEncrypt` 表示传输可加密，本课不拆算法。

3. **门卫先核 Client。** `AuthController.login` 解析 `LoginBody` 后：
   - `clientService.queryByClientId(clientId)` 为空，或 `grantType` 不在 Client 配置里 → `R.fail(auth.grant.type.error)`，**到此为止**。
   - Client `status` 不是正常 → `R.fail(auth.grant.type.blocked)`，**到此为止**。
   - 只有过了这两关才 `IAuthStrategy.login(body, client, grantType)`。Bean 名是 `grantType + "AuthStrategy"`，密码登录是 `PasswordAuthStrategy`。

4. **策略里再核验证码和人。** `PasswordAuthStrategy.login`：校验 body → 若验证码开关开着则 `validateCaptcha`（读 Redis、**立刻删键**、比对）→ 按用户名加载用户 → `authenticate`（BCrypt 或临时密码、Client 登录域）→ `loginService.buildLoginUser` → **这时才** `LoginHelper.login(loginUser, model)`。

   加载用户时，`PasswordAuthStrategy` **直接持有** `SysUserMapper`（`loadUserByUsername` 走 `userMapper.lambda()`）。这是认证段存量，**不是**业务模块入口可以直连仓库的模板。业务抄近路教材仍是 `TestBatchController`，两处不要混成一句「入口都可以拿 Mapper」。

5. **会话写入（成功时的存储点）。** `LoginHelper.login` 调用 `StpUtil.login(...)`，再 `StpUtil.getTokenSession().set("loginUser", loginUser)`。`PlusSaTokenDao` 用 `RedisUtils` 写 Redis，Caffeine 只做秒级本地缓冲。返回的 `LoginVo` 给前端 `access_token`（JSON 字段名是 `access_token`）。前端 `session.setToken(accessToken)`。登录成功后 `AuthController` 还会尽力 `notificationService.submit` 一条欢迎通知；失败只打 warn，**不能把已经成功的登录改成错误**。成功路径另外会记 `LOGIN_SUCCESS` 登录日志——那是审计，会话已经写过了。

6. **动态路由。** e2e 合同顺序是 `login → getInfo → getRouters`。身份：`GET /system/user/getInfo`（`systemService.identity.loadInfo`）。菜单：`identityAccessService.getMenus()` → `loadMenus()` → **`GET /system/menu/getRouters`**。`SysMenuController.getRouters` 读 `LoginHelper.getLoginUser()`；缺少 `clientPk` 直接 `R.fail("当前登录缺少客户端上下文")`。然后 `menuService.selectMenuTreeByUserId(userId, clientPk)` → `SysMenuMapper`（classic：ServiceImpl 持 Mapper）。

   树查询写在 Java 的 QueryBuilder join（菜单/角色菜单/用户角色/角色）。超级管理员走 `selectMenuTreeAll`。普通用户还会 **`mergeMenus`**：把自己角色菜单与该 Client 的**默认角色**菜单并在一起（`defaultRoleResolverService.resolveRoleId(clientId)`）。缺默认角色时，已登录也可能白页——先问默认角色，不要先改 Mapper XML。

   `SysMenuMapper.xml` 在 `wta-system/src/main/resources/mapper/system/`，是**空 mapper 壳**，不在 Java 源文件「同目录」。空 XML 不是「菜单不碰库」。JSON 回家后，前端才能按 manifest 把菜单键解析成 web-domain 页面。

7. **领域只读列表（layered 主走廊）。** 用户打开公告页，点搜索。页面调用注入的 notify domain，**不是**自己写 axios。domain 发出 **`GET /notify/notice/list`**。后端 `NotifyNoticeController.list`：`@SaCheckPermission("notify:notice:list")`，只调 `NotifyNoticeUseCase.list` → `NotifyNoticeService.page` → `NotifyPersistenceDao.page`（`LambdaQueryWrapper` + `noticeMapper.selectPage`）→ MySQL `notify_notice`。`NotifyNoticeMapper.xml` 里可见的 `<select>` 是 `selectByIdForUpdate`（详情加锁），**不是**这条列表 GET。列表仍然碰到了该 Mapper 的 SQL 合同面。不要把 demo 的 ServiceImpl 抄进 notify。

8. **可选第二走廊（classic demo）。** classpath 有 `wta-demo` 时：`DemoPage.vue` 调 `demoService.listDemo` → **`GET /demo/demo/list`** → `TestDemoController.list` → `testDemoService.queryPageList` → `TestDemoMapper.selectVoPage`（wrapper + `@DataPermission`）→ MySQL `test_demo`。同 Mapper 上 XML 的 `customPageList` 挂在 **`GET /demo/demo/page`**。口述 `/list` 时不要把 XML 那条 `<select>` 说成已经执行。`@DataPermission` 是走廊上的闸机，本课点到为止。

9. **JSON 回家。** domain 映射 → 页面表格。字段名仍是 L-005 的合同。

#### 失败路径 `D:fail-auth`（不写会话）

下面三种失败都发生在 `LoginHelper.login` **之前**。存储点的判决是：Redis 里**没有**这次 `StpUtil.login` 写出来的 token / token-session。前端也**不会** `session.setToken`。

**失败时并非所有柜子都空。** 验证码失败仍会 `loginService.recordLoginInfo(..., LOGIN_FAIL)`，异步插入 `sys_login_info`。那是审计，**不是**会话。缺 Client 在 `AuthController.login` 直接 `R.fail` 时，通常连这条审计也不写。`GET /auth/client/context` 仍会读 Client 表，但返回的是 `R.ok` + `clientEnabled=false`。

**失败 A：缺 Client / Client 不可用。**

- 前端：`GET /auth/client/context` 得到 `clientEnabled=false`（没传 clientId、查无此 Client、或 Client 停用）。`parseClientAuthContext` 抛错。`login()` 若 `prepared` 为 false，同样抛 `client-context-unavailable`。**不会**发出 `POST /auth/login`。
- 后端：即便有人绕过前端直打 `POST /auth/login`，`AuthController.login` 在 Client 为空、grantType 不包含、或 Client 被封时 `return R.fail(...)`，**不进入** `IAuthStrategy.login`，**不** `LoginHelper.login`。

**失败 B：验证码失败。**

- 开关打开时，`PasswordAuthStrategy.validateCaptcha`：Redis 没有该 uuid → `CaptchaExpireException`；值不匹配 → `CaptchaException`。两种都会 **`recordLoginInfo`（LOGIN_FAIL）**，再抛异常。键在比对前已删除，所以不能拿同一张图重试。**不**调用 `LoginHelper.login`。审计柜可能有字，会话柜仍空。
- 短信/邮箱验证码是另一条预备枪：`GET /resource/sms/code`、`GET /resource/email/code` 把 4 位码写入 Redis，供 sms/email 策略使用。它们同样**只写验证码，不写会话**。本课主走廊是密码登录 + `/auth/code`。限流键留给 L-012。

**失败 C：密码策略拒绝（写密码/注册，不是旧密码登录重验）。**

- **工作树精确位置：** `AuthController.login` **没有**调用 `passwordPolicyService.validateOrThrow`。已有用户登录比的是 BCrypt / 临时密码，不是再跑一遍长度与字符类。
- 策略挡住的是**写密码**路径。前端 `register()`：从 client context 取政策，`validatePassword` 有违规则抛 `password-policy-violation`，**不发** `POST /auth/register`。后端 `SysRegisterService.register` 在 uniqueness 检查之后、`BCrypt.hashpw` 之前 `passwordPolicyService.validateOrThrow`；失败抛 `ServiceException("密码不符合安全策略")`，事务回滚，**不** `registerUser`，更**不** `LoginHelper.login`。
- **注册成功也不写会话。** `SysRegisterService.register` 成功只 `registerUser` + `grantUserType` + 记 REGISTER 日志（仍是 `sys_login_info` 审计）。全文件无 `LoginHelper.login`。办好用户证 ≠ 拿到 `access_token`。
- `GET /auth/client/context` 在 Client 启用时附带 `publicProjection()`，那是给登录/注册页看的公示牌，不是登录成功的凭证。策略字段表留给 L-012。

三种失败的共同汇：会话柜子保持空。不要用「返回了 JSON」或「日志表多了一行」当成已经登录——没有 `access_token`、没有 Redis token session，后续 **`GET /system/menu/getRouters`** 过不了登录闸。

## 图、表或文本图

**图题 / caption：** 管理端主路径：登录写会话，再拉 `getRouters`，再走领域 GET 到 MySQL（alt：从浏览器经 /auth、Redis 会话、/system/menu/getRouters、领域列表到 Mapper 的箭头图）。

```text
[admin-web 登录页]
        |
        |  prepareLogin()
        |  adapter 默认头 clientid = VITE_APP_CLIENT_ID
        v
 GET /auth/client/context     GET /auth/code
 AuthController               CaptchaController
        |                            |
        |                     Redis: 验证码 uuid→code
        v
 POST /auth/login  (grantType=password)
 AuthController.login
   Client 不存在/停用/grant 不对? --yes--> R.fail  --x--> 不写会话
        | no
        v
 PasswordAuthStrategy
   验证码失败? --yes--> 异常 + 可能写 sys_login_info  --x--> 不写 LoginHelper.login
        | no
        v
 LoginHelper.login → StpUtil.login
 PlusSaTokenDao → Redis 会话 + token
        |
        |  前端 session.setToken(access_token)
        v
 GET /system/user/getInfo
 GET /system/menu/getRouters     ← 不是 /system/menu，也不是 /list
 SysMenuController → SysMenuServiceImpl → SysMenuMapper
   （XML 空壳在 resources/mapper/system/；SQL 在 QueryBuilder）
   普通用户 mergeMenus(用户角色, 默认角色)
        |
        |  动态路由落到页面
        v
 主走廊 layered:
   GET /notify/notice/list
   NotifyNoticeController → UseCase → Service → DAO → Mapper
   （列表 wrapper；XML 的 selectByIdForUpdate 不是这一枪）
        v
 MySQL notify_notice  →  JSON  →  表格

 可选第二走廊 classic（有 wta-demo 时）:
   DemoPage.vue --listDemo--> GET /demo/demo/list
   TestDemoController → TestDemoServiceImpl → selectVoPage（wrapper）
   TestDemoMapper.xml 的 customPageList 只给 GET /demo/demo/page
        v
 MySQL test_demo

虚线抄近路（不要画实线）：
  页面 ──x──> axios('/notify/notice/list' 或 '/demo/demo/list')  跳过 domain
  Controller ──x──> Mapper                    跳过 Service/UseCase
  新模块 ──x──> 复制 ServiceImpl 持 Mapper      跳过登记表（应用 layered）
```

**文字等价物：** 实线从管理端登录页出发。浏览器先问 Client 是否可用、再取验证码；这两枪都不写登录会话。Client 标识由 App 的 axios adapter 放进请求头 `clientid`，不是 domain 把 query 写进 URL。只有 `POST /auth/login` 在 Client 合法、验证码通过、用户可登录之后，才由 `LoginHelper.login` 把 Sa-Token 会话写入 Redis，并把 `access_token` 交给前端 session。之后必须再 **`GET /system/menu/getRouters`** 才能拼出动态路由；`/system/menu/list` 是另一扇菜单管理的门。点开公告列表时，web-domain 调用 domain，发出 `GET /notify/notice/list`，进入 layered 的 Controller → UseCase → Service → DAO → Mapper，读 MySQL。菜单树的 SQL 在 `SysMenuMapper` 的 Java QueryBuilder 里，XML 空壳在 `resources/mapper/system/`。演示模块若在 classpath 上，可选再走 `GET /demo/demo/list`：classic 的 Controller → ServiceImpl → wrapper `selectVoPage`；XML `customPageList` 属于 `/page`。三条虚线禁止：页面自己打 URL；控制器直接拿 Mapper；新业务模块照抄 ServiceImpl。

**图的边界：** 不画 SSO 授权码、OpenAPI HMAC、OSS 直传。`bundle-core` 可能不组装 `wta-demo`，可选第二走廊在 core 剖面不存在。修改接口在工作树里仍可能是 PUT，那是棘轮，不是本课 GET 走廊。登录欢迎通知是尽力而为，失败不影响会话已经写入这一事实。默认角色解析器内部留给别课，本课只要求你知道 `getRouters` 会并一刀默认角色菜单。

**图题 / caption：** 失败路径存储点：三个闸门都在写会话之前；审计柜仍可能有字（alt：Client、验证码、密码策略拦住时 Redis 会话为空，登录日志可能仍写入）。

```text
闸门1 Client
  前端 clientEnabled=false 或未 prepareLogin
  或 AuthController.login 直接 R.fail
        \____ 不调用 IAuthStrategy.login
        \____ 通常也不写登录日志

闸门2 验证码
  PasswordAuthStrategy.validateCaptcha 抛过期/不匹配
  （Redis 验证码键已删）
  仍可能 recordLoginInfo → sys_login_info（审计，不是会话）
        \____ 不调用 LoginHelper.login

闸门3 密码策略（写密码/注册，不是旧密码登录重验）
  前端 register() violations → 不 POST
  或 SysRegisterService.validateOrThrow → 事务回滚
        \____ 不落 sys_user，不写会话
  注册成功：落用户 + REGISTER 日志，仍然不 LoginHelper.login

汇：Redis 无本次 token-session；前端 session 无 token
    GET /system/menu/getRouters 到不了「已登录用户的菜单树」
    审计柜（sys_login_info）可能有字，不要当成通行证
```

**文字等价物：** 缺 Client 时，请求在策略 Bean 之前就被退回。验证码失败时，策略已经开始跑，但还没调用 `LoginHelper.login`；验证码缓存键会被删掉，登录日志柜仍可能插入一行失败记录。密码策略挡的是注册和新密码，挡下时既没有新用户行，也没有会话。注册成功同样不发通行证。三种失败情况的汇都是：会话柜子空着，后面的 `getRouters` 没有合法通行证。日志表多一行不是登录成功。

**图的边界：** 本图不覆盖「登录成功但 Redis 读失败」——那是会话丢失格子，不在本课 `D:fail-auth`。也不覆盖密码输错（BCrypt 不匹配）：那同样不写会话，但矩阵这一格点名的是 Client / 验证码 / 策略。验证码限流键、策略字段表留给 L-012。

## 正例、反例与边界

**正例 1（主路径）：** `prepareLogin` 看到 `clientEnabled=true`，验证码关闭或填写正确，`POST /auth/login` 返回 `access_token`。随后 **`GET /system/menu/getRouters`** 带上该 token，再 **`GET /notify/notice/list`**。全程只读查询保持 GET；登录是 POST，因为它改变了会话状态。

**正例 2（菜单所有权）：** 动态路由走 `/system/menu/getRouters`，由已登录的 `LoginUser.clientPk` 过滤，无 `system:menu:list` 注解。菜单管理列表是另一个 GET：`/system/menu/list`，还要 `system:menu:list`。不要把两个 GET 说成同一跳，也不要把矩阵前缀 `/system/menu` 当成已经闭合。

**正例 3（layered 主走廊）：** 领域列表 `GET /notify/notice/list` 口述成 Controller → UseCase → Service → DAO → Mapper。入口仍然不抓仓库钥匙。列表走 wrapper；XML 合同面仍在，只是这条 GET 不执行 `selectByIdForUpdate`。

**正例 4（可选 classic 第二走廊）：** 有 `wta-demo` 时，`GET /demo/demo/list` 口述成 Controller → ServiceImpl → `selectVoPage`。要指 XML 时，指到 **`GET /demo/demo/page`** 的 `customPageList`，不要把两条 GET 说成一枪。

**正例 5（失败不写会话）：** 用错误 grantType 打 `POST /auth/login`。`AuthController.login` 返回失败信息。`PasswordAuthStrategy` 不会被调用，`PlusSaTokenDao` 不会为这次尝试 `set` 新的登录 token。

**正例 6（注册策略）：** 明文密码缺字符类。前端 `register()` 先拒绝；若直打后端，`validateOrThrow` 拒绝且事务回滚。`sys_user` 无新行，Redis 无新会话。注册成功也只落用户，仍要再走登录才能拿到 `access_token`。

**正例 7（审计 ≠ 会话）：** 验证码填错。Redis 验证码键已删，`sys_login_info` 可能多一行 LOGIN_FAIL。前端没有 token，`getRouters` 过不了闸。你指着日志表不能说「已经登录」。

**反例 1（抄近路，页面跳过 domain）：** 在页面写死 `axios.get('/notify/notice/list')` 或 `axios.get('/demo/demo/list')`。第二个终端无法复用 transport 映射和错误处理；URL 成为页面私产。这是 ARCH-003 前端边界失败。

**反例 2（抄近路，Controller 注入 Mapper）：** 给业务 Controller 加上 Mapper 字段，「列表很简单不用 Service」。工作树里 `TestBatchController` 为了演示批量插入**故意** `private final TestDemoMapper testDemoMapper`，注释写「为了便于测试 直接引入mapper」。那是 demo 能力展示，不是业务模板。`TestDemoController` 自己只依赖 `ITestDemoService`；`NotifyNoticeController` 只依赖 `NotifyNoticeUseCase`。闸机后面没有厨师，校验和包装会散落。

**反例 3（抄近路，新模块照抄 classic）：** 新建业务模块时把 `TestDemoServiceImpl` 连同直接持 Mapper 一起粘贴。demo / system 是 classic 存量。未登记新模块默认 layered（L-003）。

**反例 4（失败路径抄近路）：** 验证码没过就先 `StpUtil.login` 再返回错误，或注册策略失败仍 `registerUser`。存储点被污染：柜子里留下不该存在的通行证或半个用户。本仓库的顺序禁止这样做。

**反例 5（合同顺序）：** 前端先给列表加 `color`，后端 VO 还没有该字段（L-005）。走查会在 JSON 这一站断掉，看起来像「Mapper 没查到」，其实是合同先裂了。

**反例 6（把认证段存量当成业务模板）：** 看见 `PasswordAuthStrategy` 持 `SysUserMapper`，就说「入口可以直连仓库，demo 也这样」。认证策略不是业务 Controller；业务入口的反例教材是 `TestBatchController`。两句话分开说。

**边界：**

- `v-hasPermi="['notify:notice:list']"` 或 `['demo:demo:list']` 只藏按钮。没有权限的人仍可能打 GET；服务端 `@SaCheckPermission` 才是闸机。
- `@SaIgnore` 在 `AuthController` / `CaptchaController` 上：这两扇门允许匿名。菜单和领域列表不行。
- 验证码开关关闭时，失败 B 不触发；失败 A、C 仍在。
- `client/context` 查无 Client 时返回「关闭」而不是炸：这是公开探测，不能当成「已经登录」。
- 本课领域 GET 主走廊是 notify 列表；demo 是可选第二 classic 走廊。core 包没装 `wta-demo` 时不要假装那一枪存在。
- 密码输错（BCrypt 不匹配）同样不写会话，但不在本格点名的三闸里。

## 变式与迁移

- **变式 A：同一资源的详情。** `GET /notify/notice/{id}` 或 `GET /demo/demo/{id}`。登录和 `getRouters` 段不变，只是路径多主键。notify 详情仍经 UseCase；不要为了「就一行」让 Controller 拿 Mapper。
- **变式 B：换到 classic GET。** 口述里的 UseCase + DAO 换成 ServiceImpl 持 Mapper。App 组合、domain HTTP、权限注解、会话柜子仍在。demo `/list` 仍是 wrapper，不要顺手把 XML `/page` 接上去。
- **变式 C：短信/邮箱登录。** 预备枪换成 `/resource/sms/code` 或 `/resource/email/code`，策略 Bean 换成 `smsAuthStrategy` / `emailAuthStrategy`。Client 闸门仍在 `AuthController.login` 最前面。失败时同样不写会话。
- **变式 D：社交回调。** `POST /auth/social/callback` 先 `StpUtil.checkLogin()`——它假定**已经有会话**，用来绑第三方，不是本课「从匿名到有会话」的主走廊。社交登录走 `POST /auth/login` 且 `grantType=social`。
- **变式 E：core 包。** 走查失败时先问 classpath 有没有对应模块（尤其是可选的 `wta-demo`），再问代码写没写。
- **迁移到改列表：** 改列 = 可能改 VO + SQL/XML 或 wrapper + transport 映射 + 页面。从 L-005 的顺序走，再回到本课检查有没有跳层、有没有在未登录时假定菜单已存在。
- **迁移到认证切片：** 把 `AuthController` 五个公开入口逐方法拆开是 L-011；验证码三个 code、限流键与 `PasswordPolicyService` 字段是 L-012；`IAuthStrategy` / `SysLoginService` 副作用是 L-013；前端 `IdentityAccessService` 与 session 命名空间是 L-014。本课只要求你能把它们安在走廊上的正确一站。
- **迁移到失败排查：** 菜单白页先问：有没有 token？Redis 会话还在不在？`getRouters` 有没有 `clientPk`？普通用户有没有并上默认角色菜单？不要先改 Mapper XML，也不要把 `sys_login_info` 有行当成已登录。

## 常见误区

1. **「页面能看到按钮 = 有权限。」** 按钮是化妆品；Controller 注解才是门禁。
2. **「classic 已经持有 Mapper，Controller 再拿一次也没关系。」** 入口越层会让校验和事务失去唯一位置。`TestBatchController` 是反例教材，不是样板。
3. **「demo 好抄，所以新模块先 classic。」** 登记表禁止把未登记新模块默认成 classic。demo 在本课只是可选第二走廊。
4. **「走查等于已经掌握。」** 本课只训练口述、指文件、指存储点。没有作业、没有延迟复习，能力仍未验证。
5. **「`GET /system/menu` 就是动态路由。」** 登录后拼路由的是 **`/system/menu/getRouters`**。`/list` 是菜单管理。矩阵前缀不能代替这一跳。
6. **「登录失败也会先写会话再删。」** 工作树不是这样。Client 失败根本进不了策略；验证码失败进不了 `LoginHelper.login`。
7. **「密码策略在每次登录时重验旧密码。」** `login` 不调用 `validateOrThrow`。策略在注册、改密、导入、重置这些写密码入口。把它说成登录闸门时，要说清实际挡住的是注册/写密，效果仍是「不会出现新会话」。
8. **「Mapper XML 空着就等于没有 SQL。」** `SysMenuMapper.xml` 在 `resources/mapper/system/` 是空壳，树查询在 Java QueryBuilder。空 XML 不是「菜单不碰库」。
9. **「AuthController 在 wta-system。」** 它在 `wta-admin`。system 提供 Client、用户、菜单、密码策略服务，不拥有 `/auth/login` 这个 HTTP 门面。
10. **「失败路径零存储」或「日志表有行 = 已登录」。** 会话柜空着是判决。验证码失败仍可能写 `sys_login_info`，那是审计。
11. **「领域列表 GET 一定执行 XML 里的 `<select>`。」** `/notify/notice/list` 和 `/demo/demo/list` 都走 wrapper。demo 的 XML `customPageList` 属于 `/page`。合同面仍是 Mapper。
12. **「注册成功 = 已经拿到通行证。」** `SysRegisterService.register` 从不 `LoginHelper.login`。还要再走登录。
13. **「`PasswordAuthStrategy` 持 Mapper，所以业务 Controller 也可以。」** 认证段存量 ≠ 业务模板。业务反例仍是 `TestBatchController`。

## 非评分暂停

在心里把主路径按点击顺序排一下（不要写成答卷）：prepareLogin、POST login、Redis 会话、**getRouters**、notify 列表、Controller、UseCase、Service、DAO、Mapper、MySQL。少了「会话」或「getRouters」的人，通常会在「为什么登录成功仍是白页」那里迷路。有 `wta-demo` 时，再在旁边标一条可选 classic：`/list` 走 wrapper，XML 留给 `/page`。

再选一个失败：缺 Client、验证码错、注册密码太弱。说一句：**哪个柜子必须保持空**？会话柜。若你选了验证码错，补半句：审计柜仍可能有字。

再把其中一站改成虚线抄近路，说一句会坏什么。

## 总结、词汇表与下一步

- 主路径：`GET /auth/client/context` + `GET /auth/code` → `POST /auth/login` → Redis 中的 Sa-Token 会话 → **`GET /system/menu/getRouters`** → 领域 GET（主走廊 `/notify/notice/list`）→ Controller → UseCase → Service → DAO → Mapper → MySQL。
- 可选第二 classic 走廊：`GET /demo/demo/list` → Controller → ServiceImpl → wrapper `selectVoPage`。XML `customPageList` 属于 `/page`。
- 「走到 Mapper XML」= 碰到 Mapper 的 SQL 合同面（wrapper / QueryBuilder / 或 XML）。空 XML ≠ 不碰库；列表 GET 不一定执行某个 `<select>`。
- 失败路径：缺 Client、验证码失败、密码策略拒绝（注册/写密）→ **不** `LoginHelper.login` → Redis 无本次会话。验证码失败仍可能写 `sys_login_info`（审计，不是会话）。注册成功也不写会话。
- `AuthController` 公开入口在 `wta-admin`：`/auth/login`、`/logout`、`/register`、`/client/context`、`/social/callback`。验证码：`/auth/code`、`/resource/sms/code`、`/resource/email/code`。
- 至少记住一种抄近路：页面直打 URL，或 Controller 注入 Mapper（`TestBatchController` 是示意，不是业务模板）。`PasswordAuthStrategy` 持 `SysUserMapper` 是认证段存量，分开记。
- 词汇：happy path、fail path、client context、captcha、session、dynamic routers、Mapper XML、layer skip、storage point、login-info audit。
- 本课结束仍不是掌握证明；要练习请之后主动激活 Homework。函数级拆 `AuthController` 是 L-011；限流键与策略字段是 L-012。

标准路径 L-001～L-006 的总览地图到这里合龙。下一步由你选：继续 Goal 的 L-007（admin-web 组合）或认证切片 L-011。本课不自动串联。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | demo 为前端/后端 CRUD 基线；App 显式组合 | 实现基线表 | 2026-09-16 |
| S-005 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | demo=classic；notify=layered；system=classic | 当前登记 | 2026-09-16 |
| S-006 | `.agents/skills/engineering-standards/references/rules/architecture-and-boundaries.md` | 前端边界；入口禁止直连 Mapper | ARCH-003、ARCH-002A | 2026-09-16 |
| S-008 | `frontend/apps/admin-web/src/application/services.ts`；`application/http.ts` | `identityAccessService` 接线；`createAxiosBrowserAdapter` 默认头 `clientid` | 工作树 | 2026-09-16 |
| S-020 | `backend/wta-admin/.../AuthController.java` | `/auth/login\|logout\|register\|client/context\|social/callback`；缺 Client 时 `R.fail` 且不调策略；`login` 不调 `validateOrThrow` | `@RequestMapping("/auth")` 与 `login` / `clientContext` | 2026-09-16 |
| S-021 | `backend/wta-admin/.../CaptchaController.java` | `GET /auth/code`、`/resource/sms/code`、`/resource/email/code`；验证码写入 Redis | 三个 `@GetMapping` | 2026-09-16 |
| S-022 | `PasswordAuthStrategy.java`、`IAuthStrategy.java` | 验证码失败抛异常并 `recordLoginInfo`；成功才 `LoginHelper.login`；直持 `SysUserMapper` | `login` / `validateCaptcha` / `loadUserByUsername` | 2026-09-16 |
| S-023 | `LoginHelper.java`、`PlusSaTokenDao.java` | 成功路径写 Sa-Token 会话到 Redis | `login`；`RedisUtils.setCacheObject` | 2026-09-16 |
| S-024 | `SysRegisterService.java`、`PasswordPolicyService.java` | 策略拒绝不落用户、不写会话；成功也不 `LoginHelper.login`；`login` 不调用 `validateOrThrow` | `register` / `validateOrThrow` | 2026-09-16 |
| S-025 | `SysMenuController.java`、`SysMenuServiceImpl.java`、`SysMenuMapper.java`、`resources/mapper/system/SysMenuMapper.xml` | `GET /system/menu/getRouters` 与 `/list` 分家；XML 空壳在 resources；树查询在 QueryBuilder；普通用户 `mergeMenus` 默认角色 | `getRouters`；`selectMenuTreeByUserId` | 2026-09-16 |
| S-026 | `frontend/packages/domains/admin/src/index.ts`；`domains/system/src/service.ts` | `prepareLogin` / `login` / `register` / `getMenus`；`loadMenus` → `/system/menu/getRouters`；缺 Client 与策略违规不发写会话请求 | `createIdentityAccessService`；`identity.loadMenus` | 2026-09-16 |
| S-027 | `TestDemoController`、`TestDemoServiceImpl`、`TestDemoMapper.xml`、`DemoPage.vue`、`domains/demo` | 可选 classic：`/demo/demo/list` 走 `selectVoPage`；XML `customPageList` 给 `/page`；页面走 `listDemo` | `/list` 与 `/page` | 2026-09-16 |
| S-028 | `NotifyNoticeController.java`、`NotifyNoticeUseCase`、`NotifyPersistenceDao`、`NotifyNoticeMapper.xml`；`TestBatchController.java`；`SysLoginInfo.java` | layered 列表只注入 UseCase，列表 wrapper；XML `selectByIdForUpdate` 非列表；Controller 直接持 Mapper 的反例；审计表 `sys_login_info` | `/notify/notice/list`；`/demo/batch`；`@TableName` | 2026-09-16 |
