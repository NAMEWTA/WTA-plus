---
lesson_id: L-003
objective_ids: [OBJ-03]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 5
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-SSO-02, S-SSO-04, S-SSO-05, S-SSO-07]
---

# Lesson 003：认人厅手环：`login` / `session` / `logout`

## 学完你能做什么

你能口述 SSO 域会话的三条公开 HTTP：`SsoSessionController.login`、`session`、`logout` 如何进入 `SsoSessionUseCase` / `SsoSessionUseCaseImpl`，再进入 `SsoSessionService`，最后落到 `SsoIdentityPort` 与 `SsoSessionPort`（工作树里是 `WtaApiSsoIdentityAdapter` 和 `RedisSsoSessionStore`）。你能指明 Cookie 由谁写入（`writeCookie` + `SsoSessionCookie.create`）、由谁读取（两个控制器各自的 `readSessionId`），以及登录**不签发**业务 Token。本课不把 OAuth 发码或换票当成已经会改。

## 先把宏观地图放在桌上

认人厅发的是**手环编号**，不是游乐项目的座位票。手环编号放在 HttpOnly Cookie 里，默认名字 `Sso-Token`。人是谁放在 Redis 键 `sso:session:` 后面。业务 Sa-Token 是 L-002 的事。

`SsoSessionController` 映射在 `/sso`（注意：不是 `/sso/oauth2`），同样 `@SaIgnore`、同样 `@ConditionalOnProperty(prefix="namewta.sso", name="enabled", havingValue="true", matchIfMissing=true)`。它只注入两个合作者：`SsoSessionUseCase sessionUseCase` 和 `SsoProperties properties`。没有 `SsoOAuthUseCase`。

三条公开映射，一层层对齐：

| HTTP | 控制器方法 | UseCase 方法 | Service 方法 | 端口 / 适配器 |
| --- | --- | --- | --- | --- |
| `POST /sso/login` | `login` | `SsoSessionUseCase.login` | `SsoSessionService.login` | `SsoIdentityPort.verifyPassword` → `SsoSessionPort.create` |
| `GET /sso/session` | `session` | `SsoSessionUseCase.current` | `SsoSessionService.current` | `SsoSessionPort.find` |
| `POST /sso/logout` | `logout` | `SsoSessionUseCase.logout` | `SsoSessionService.logout` | `SsoSessionPort.delete` |

名字陷阱：HTTP 叫 `session`，用例却叫 `current`。不要说 `SsoSessionUseCase.session`——没有这个方法。

**类比失效处：** 游乐场手环有时也能直接刷项目。这里不行。`SsoSessionService` 的类注释写明：只认本仓密码，**不签发业务 Token**。单元测试里的假 `SsoIdentityPort.buildLoginUser` 甚至直接抛 `UnsupportedOperationException("SSO session must not issue a business token")`——会话路径根本不调用它。手环丢了（logout）也不会去 `SsoBusinessTokenPort.revoke` 收座位票。

**图题 / caption：** SSO 域会话三条路径与 Cookie / Redis 的分工。

```text
POST /sso/login
  JSON SsoLoginBo { username, password }   @Valid @NotBlank
        |
        v
SsoSessionController.login
  @Log(title="SSO登录", excludeParamNames={password}, isSaveResponseData=false)
        |
        |  sessionUseCase.login(username, password)
        v
SsoSessionUseCaseImpl.login
        v
SsoSessionService.login
        |-- 用户名或密码空白 → "用户名或密码错误"
        |-- identityPort.verifyPassword
        |     WtaApiSsoIdentityAdapter.verifyPassword
        |     SsoIdentityService.verifyPassword
        |-- sessionPort.create(user)
              RedisSsoSessionStore.create
                sessionId = 两段 unsigned hex long
                RedisUtils.setCacheObject("sso:session:"+id, user, sessionTtl)
        |
        |  writeCookie(response, sessionId, properties.getSessionTtl())
        |     SsoSessionCookie.create(cookieName, value, maxAge)
        |     ResponseCookie: HttpOnly, path=/, SameSite=Lax
        |     response.addHeader(SET_COOKIE, ...)
        v
R.ok()   响应 JSON 里没有 sessionId

GET /sso/session
        |
        v
SsoSessionController.session
        |  readSessionId(request)  --Cookie 名--> properties.getCookieName()
        |  sessionUseCase.current(sessionId)
        v
SsoSessionUseCaseImpl.current → SsoSessionService.current → RedisSsoSessionStore.find
        |
        +-- user == null → R.fail("未登录")
        +-- user != null → new SsoAuthenticatedUser(userId, username) → R.ok(safe)

POST /sso/logout
        |
        v
SsoSessionController.logout
  @Log(title="SSO注销")
        |  sessionUseCase.logout(readSessionId(request))
        v
SsoSessionUseCaseImpl.logout → SsoSessionService.logout → RedisSsoSessionStore.delete
        |
        |  writeCookie(response, "", Duration.ZERO)   过期手环 Cookie
        v
R.ok()
```

**文字等价物：** 登录从 `SsoSessionController.login` 进，校验 `SsoLoginBo` 的用户名密码非空，调用 `SsoSessionUseCase.login`。`SsoSessionUseCaseImpl` 转给 `SsoSessionService.login`：再拦一层空白，然后 `WtaApiSsoIdentityAdapter.verifyPassword` 认人，`RedisSsoSessionStore.create` 把 `SsoAuthenticatedUser` 写入 Redis 并返回会话标识。控制器用私有 `writeCookie` 调 `SsoSessionCookie.create`，通过 `Set-Cookie` 把头环编号交给浏览器；JSON 成功体是空的。查询当前会话走 `SsoSessionController.session` → `current` → `find`；没有用户就 `R.fail("未登录")`，有用户就拷贝一个只含 `userId`/`username` 的安全对象。注销走 `logout` → `SsoSessionPort.delete`，再把 Cookie 写成空值、`maxAge=0`。三条路都不碰授权码表，也不碰 `SsoBusinessTokenPort`。

**图的边界：** `SsoProperties.webOrigin` 本控制器不读。`SsoSessionCookie.create` 不设置 `Secure`、也不设置 `Domain`。生产是否在 HTTPS 上补 Secure，源码此刻没有做。本图也不包含 `SsoOAuthController.readSessionId`——那是授权入口借用同一 Cookie 名的只读逻辑，写 Cookie 仍只发生在本控制器。

## 核心概念与机制

### 直觉讲解

把 Redis 想成认人厅后面的挂钩墙。挂钩编号（sessionId）很长、看不出是谁。你手腕上的塑料环（Cookie）只刻着编号。工作人员要确认你是谁，就拿编号去墙上取挂牌（`SsoAuthenticatedUser`）。

登录 = 验密码 + 挂上一块新牌 + 给你一只新环。  
问“我是谁” = 看环上的号去墙上找牌；找不到就说未登录。  
注销 = 摘掉那块牌，并且把环作废（Cookie 立刻过期）。

密码验证走 `SsoIdentityPort.verifyPassword`，不是 SSO 模块自己搓一套用户表。适配器 `WtaApiSsoIdentityAdapter` 把活交给 `wta-api` 里的 `SsoIdentityService`。

**类比失效处：** 挂钩墙上的牌默认挂 8 小时（`SsoProperties.sessionTtl`，`Duration.ofHours(8)`），不是“直到你关浏览器”。Cookie 的 `maxAge` 用的是同一个 TTL。另外，塑料环标了 HttpOnly：页面上的脚本不能假装自己是工作人员去改环。这不是“前端 localStorage 存 token”的模型。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| SSO 域会话 | SSO session | Redis 对象，键前缀 `sso:session:`，值是 `SsoAuthenticatedUser` |
| 会话标识 | session id | `RedisSsoSessionStore.create` 生成的 hex 串；只出现在 Cookie，不出现在登录 JSON |
| 会话 Cookie | session cookie | `SsoSessionCookie.create`：HttpOnly、`path=/`、`SameSite=Lax` |
| 认人 | identity verification | `SsoIdentityPort.verifyPassword`；登录路径只调用这一个身份方法 |
| 当前会话 | current session | `SsoSessionUseCase.current` / `SsoSessionPort.find` |
| 注销 | logout | 删 Redis 键 + 写空 Cookie；不是 SLO，不撤销业务 Token |
| 已认证用户 | authenticated user | `SsoAuthenticatedUser`：`userId` + `username`，不含密码、不含 Sa-Token |

`SsoLoginBo` 用 `@NotBlank` 管用户名和密码。控制器 `@Valid` 会在进 UseCase 前挡空串。服务层 `SsoSessionService.login` 又用 `StringUtils.isBlank` 挡一次，失败文案统一 `"用户名或密码错误"`——不区分“没填”和“填错”，避免帮人猜账号。

### 机制/因果链

`SsoSessionUseCaseImpl` 三个方法都是一行转交 `SsoSessionService`。可以把它想成 layered 的薄封面。`SsoSessionService` 才是规则：构造器注入 `SsoIdentityPort` 与 `SsoSessionPort`。

#### 1. `POST /sso/login` → `SsoSessionController.login`

1. 参数：`@Valid @RequestBody SsoLoginBo bo`，`HttpServletResponse response`。
2. `String sessionId = sessionUseCase.login(bo.getUsername(), bo.getPassword());`
3. `SsoSessionUseCaseImpl.login` → `SsoSessionService.login`：
   - 空白用户名或密码 → `ServiceException("用户名或密码错误")`；
   - `SsoAuthenticatedUser user = identityPort.verifyPassword(username, password)`；
   - 适配器 `WtaApiSsoIdentityAdapter.verifyPassword` → `SsoIdentityService.verifyPassword`；
   - `return sessionPort.create(user)`。
4. `RedisSsoSessionStore.create`：
   - `sessionId = Long.toUnsignedString(IdGeneratorUtil.nextLongId(), 16) + Long.toUnsignedString(IdGeneratorUtil.nextLongId(), 16)`（两段拼起来，降低猜中概率）；
   - `RedisUtils.setCacheObject("sso:session:" + sessionId, user, properties.getSessionTtl())`；
   - 返回 sessionId。
5. 控制器私有 `writeCookie(response, sessionId, properties.getSessionTtl())`：
   - `SsoSessionCookie.create(properties.getCookieName(), value, maxAge)`；
   - `maxAge == null` 时当 `Duration.ZERO`；`value == null` 当 `""`；
   - `.httpOnly(true).path("/").sameSite("Lax").maxAge(age)`；
   - `response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())`。
6. `return R.ok();` 类型 `R<Void>`。前端不能从 JSON 里抄 sessionId 去冒充。

`@Log`：`title = "SSO登录"`，`excludeParamNames = {"password"}`，`isSaveResponseData = false`。

#### 2. `GET /sso/session` → `SsoSessionController.session`

1. 没有请求体。私有 `readSessionId(request)` 与 OAuth 控制器那份同形状：Cookie 数组为空则 `null`；否则找 `properties.getCookieName()`。
2. `sessionUseCase.current(sessionId)` → `SsoSessionUseCaseImpl.current` → `SsoSessionService.current` → `sessionPort.find(sessionId)`。
3. `RedisSsoSessionStore.find`：`sessionId` 空白直接 `null`（所以没 Cookie、空 Cookie 都是未登录）；否则 `RedisUtils.getCacheObject("sso:session:" + sessionId)`。
4. `user == null` → `R.fail("未登录")`。注意：这不是 `R.ok` 加布尔字段。和 L-001 的 `loginRequired` 包装不一样。
5. 否则 `new SsoAuthenticatedUser(user.getUserId(), user.getUsername())` 再 `R.ok(safe)`。即使 Redis 里将来多了字段，HTTP 也只回这两项。

`GET /session` 没有 `@Log`（只读查询）。

#### 3. `POST /sso/logout` → `SsoSessionController.logout`

1. `sessionUseCase.logout(readSessionId(request))`。
2. `SsoSessionService.logout` → `sessionPort.delete(sessionId)`。
3. `RedisSsoSessionStore.delete`：空白 sessionId **直接 return**，不打 Redis；否则 `RedisUtils.deleteObject("sso:session:" + sessionId)`。
4. 无论 Redis 里原来有没有键，控制器都会 `writeCookie(response, "", Duration.ZERO)`，让浏览器丢掉手环。
5. `R.ok()`。`@Log(title = "SSO注销")`。

没有 Cookie 时 logout 仍返回 ok：UseCase 会把 `null` 传下去，存储层当空操作。浏览器这边 Cookie 仍被写成空。这是“尽量清干净”，不是“没登录就报错”。

#### 配置与装配

- Cookie 名默认 `"Sso-Token"`（`SsoProperties.cookieName`）。
- 会话 TTL 默认 8 小时；授权码 TTL 默认 5 分钟——两套钟，不要混。
- `SsoSessionService` 带 `@Service`，不像 `SsoAuthorizationService` 那样走 `SsoServiceConfiguration` 手工 `new`。
- `SsoAutoConfiguration` 只 `@EnableConfigurationProperties(SsoProperties.class)`，不创建会话 Bean。
- `RedisSsoSessionStore` 的前缀常量是私有 `KEY_PREFIX = "sso:session:"`。

L-001 的 `SsoOAuthController.authorize` 会调用本课的 `SsoSessionUseCase.current`，但它**不写** Cookie。完整“先登录再授权”的人体动作是：本课 `login` 成功 → 浏览器存 Cookie → 再打 `GET /sso/oauth2/authorize`。

### 图、表或文本图

**图题 / caption：** 两枚票不能互相替代：手环在 Redis，座位票在 Sa-Token。

```text
[SSO 域]                          [业务 App]
 SsoSessionController             SsoOAuthController.token / 业务请求
   login  --> Redis 会话          exchange --> Sa-Token (IssuedToken)
   session --> 读 Redis
   logout --> 删 Redis            revoke  --> logoutByTokenValue
 Cookie: Sso-Token                Header/存储: access_token
 不持有 LoginUser 业务 extras     SsoTokenExtras.bind(目标 Client)
```

**文字等价物：** 左边三方法只移动 SSO 会话。右边换票/撤销只移动业务 Token。`logout` 不会调用 `SsoBusinessTokenPort.revoke`；`revoke` 也不会调用 `SsoSessionPort.delete`。Cookie 名和 access_token 不是同一个字符串空间。

**图的边界：** 图不承诺“登出认人厅后所有业务 Token 立刻失效”。当前工作树没有把它们绑成一条事务。若产品要跨 App 单点登出，那是新工作，不是这三条映射已经做了的事。

### 正例、反例与边界

**正例 1（密码登录）。** `SsoLoginBo` 用户名 `WTA`、密码非空且能通过 `verifyPassword`。`create` 写入 Redis，`Set-Cookie` 带 HttpOnly 的 `Sso-Token`，JSON 为成功空体。随后 `GET /sso/session` 读到同一用户名。

**正例 2（查询未登录）。** 没有 Cookie，或 Redis 键过期。`current` 得到 `null`，`R.fail("未登录")`。

**正例 3（注销）。** 有 Cookie：Redis 键被 `delete`，响应里 Cookie `maxAge=0`、值为空。再 `GET /sso/session` 应为未登录。已发出的业务 Token 仍按自己的超时活着，除非另走 L-002 的 `revoke`。

**反例 1（空白密码）。** 控制器校验或服务空白检查失败。测试 `passwordLoginCreatesSsoSessionNotBusinessToken` 对空密码期望 `ServiceException`。失败路径不得调用 `buildLoginUser`。

**反例 2（前端自己 `document.cookie` 写一个假 Sso-Token）。** 即使名字对了，Redis 没有对应挂牌，`find` 仍是 `null`。HttpOnly 还阻止脚本读取真正的值，但不能阻止有人在非浏览器客户端随便带一个 Cookie 头——真正的门是 Redis 里有没有对象。

**反例 3（把登录 JSON 当成 Token 接口）。** `login` 返回 `R<Void>`。没有 `access_token`。要座位票必须走授权码 + `exchange`。

**边界：**

- `verifyPassword` 的具体盐、锁定、用户类型不在 `wta-sso` 模块内部展开；本课只要求你指到端口方法。
- `SsoSessionCookie` 没有 `secure(true)`。在纯 HTTP 本地（默认 `webOrigin` 是 `http://127.0.0.1:4176`）这能跑通；不要把它说成已经做了全站 Secure Cookie。
- `readSessionId` 在 `SsoSessionController` 与 `SsoOAuthController` 各有一份私有实现，没有抽公共类。改 Cookie 名只改 `SsoProperties` 即可让两边一起变；改读取算法要改两处。

## 变式与迁移

- **变式 A：登录后立刻授权。** 浏览器带着新 Cookie 打 L-001 的 `authorize`。`user` 不再是 `null`，才能 `insert` 授权码。迁移到别的“先会话后 code”协议时，记住：会话 API 和授权 API 是两个控制器。
- **变式 B：只想踢掉一个业务 App。** 不要用本课 `logout`。用 L-002 `revoke`。`logout` 是摘手环。
- **变式 C：换 Redis 实现。** 只要仍实现 `SsoSessionPort` 的 `create`/`find`/`delete`，`SsoSessionService` 不用改。不要让 Service 直接调用 `RedisUtils`。
- **变式 D：把 sessionId 放进 JSON 给 SPA 存 localStorage。** 与当前设计相反。当前刻意只 Set-Cookie 且 HttpOnly。迁移时要先改安全假设，不能偷偷加一个返回字段了事。

**迁移口诀：** 认人（`verifyPassword`）→ 建会话（`create`）→ 写环（`SsoSessionCookie.create`）。问人只 `find`。走人是 `delete` + 空 Cookie。永远不问 `SsoBusinessTokenPort`。

## 常见误区

1. **误区：`Sso-Token` Cookie 就是 Sa-Token。** 名字像，空间不是同一个。一个是 SSO 会话标识，一个是 `IssuedToken.accessToken`。
2. **误区：`SsoSessionController.session` 对应 UseCase.session。** UseCase 方法是 `current`。
3. **误区：登录会调用 `SsoTokenExtras.bind`。** 那是换票签发路径。会话服务显式不发业务票。
4. **误区：logout 会把所有业务 Token 登出。** 它只 `SsoSessionPort.delete` 加空 Cookie。
5. **误区：授权控制器用 `SsoSessionCookie` 读 Cookie。** 两边都是手写 `readSessionId` 循环。`SsoSessionCookie` 只有 `create`。
6. **误区：`GET /sso/session` 的未登录和 authorize 的 `loginRequired` 是同一种返回。** 前者 `R.fail`，后者 `R.ok` 加布尔。前端不能用同一套判断。
7. **误区：sso-web 页面实现等于本课。** 课程范围外：sso-web 是认人厅的浏览器皮肤，本课不走组件树。

## 非评分暂停

用手指点方法名（不记分）：

- `SsoSessionController.login` 调用的 UseCase 方法、Service 方法、两个端口方法分别是什么？
- `RedisSsoSessionStore.create` 的键前缀和 TTL 从哪来？
- `logout` 在 sessionId 为 `null` 时会不会仍写 `Set-Cookie`？
- 为什么 `GET /sso/session` 要 `new SsoAuthenticatedUser(...)` 而不是直接把 Redis 对象塞回去？

卡住就回到三条映射表和机制第 1～3 节。

## 总结、词汇表与下一步

SSO 域会话是认人厅手环。`SsoSessionController` 三方法分别对应 `SsoSessionUseCase.login` / `current` / `logout`，实现类转给 `SsoSessionService`，再分别调用 `verifyPassword`+`create`、`find`、`delete`。Cookie 由 `SsoSessionCookie.create` 写成 HttpOnly；登录成功不在 JSON 里发会话标识。这与 L-001 发码、L-002 换票/撕票是三间分开的房间。

| 词 | 记住哪一个方法 |
| --- | --- |
| 戴手环 | `SsoSessionController.login` → `SsoSessionService.login` → `RedisSsoSessionStore.create` |
| 看手环 | `SsoSessionController.session` → `SsoSessionUseCase.current` → `RedisSsoSessionStore.find` |
| 摘手环 | `SsoSessionController.logout` → `SsoSessionService.logout` → `RedisSsoSessionStore.delete` |
| 塑料环规格 | `SsoSessionCookie.create` |

三课合在一起的口述顺序：先本课登录，再 L-001 授权，再 L-002 换票；注销手环和撤销业务票要分开说。作业不在本课生成。

## 来源与引用

权威是当前工作树 Java。访问日期 2026-09-14。

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-SSO-02 | `SsoSessionController.java` | `/sso` 的 `login`/`session`/`logout`；`writeCookie`/`readSessionId`；登录不返回 sessionId | `controller/anonymous/SsoSessionController.java` | 2026-09-14 |
| S-SSO-04 | `SsoSessionUseCase` / `SsoSessionUseCaseImpl` | `login`/`current`/`logout` 委托 `SsoSessionService` | `usecase/` | 2026-09-14 |
| S-SSO-05 | `SsoSessionService` | 空白密码拒绝；只 `verifyPassword`+`create`；`current`/`logout` 只碰会话端口 | `service/SsoSessionService.java` | 2026-09-14 |
| S-SSO-07 | `SsoSessionCookie`、`SsoSessionPort`、`RedisSsoSessionStore`、`WtaApiSsoIdentityAdapter`、`SsoProperties` | HttpOnly Cookie；Redis 键前缀与 TTL；认人适配器 | `support/`、`port/`、`adapter/`、`config/` | 2026-09-14 |

未决（open/uncertain）：生产环境 SSO 是否已部署。Cookie 未设 Secure 是否被运行时环境另行处理，工作树本模块未写。
