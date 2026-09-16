---
lesson_id: L-001
objective_ids: [OBJ-01]
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
source_ids: [S-SSO-01, S-SSO-03, S-SSO-04, S-SSO-05, S-SSO-06, S-SSO-07, S-SSO-08]
---

# Lesson 001：授权码门口：`GET /sso/oauth2/authorize`

## 学完你能做什么

你能对着 `backend/wta-modules/wta-sso/src/main/java` 口述：浏览器打 `GET /sso/oauth2/authorize` 时，`SsoOAuthController.authorize` 怎么读 Cookie、怎么问会话、怎么把查询参数塞进 `SsoOAuthCommands.AuthorizeCommand`，再经 `SsoOAuthUseCase.authorize` / `SsoOAuthUseCaseImpl.authorize` 交给 `SsoAuthorizationService.authorize`。你还能指出这条路上 PKCE、Client 目录、登录分支和授权码写入分别发生在哪。本课不把换票、撤销或 sso-web 页面树当成已经会改。

## 先把宏观地图放在桌上

把 SSO 想成一栋楼的**认人厅**。业务 App（管理端、门户）不能共用同一张业务通行证。访客先到认人厅证明“我是谁”，厅里发一张**很短、一次性、只能给指定门口用的纸条**——授权码（authorization code）。纸条本身还不能进业务房间。

`SsoOAuthController` 是认人厅对外的三扇门，都挂在 `/sso/oauth2` 上，并且标了 `@SaIgnore`（不先查业务 Sa-Token）：

| HTTP | 控制器方法 | 本课深度 |
| --- | --- | --- |
| `GET /sso/oauth2/authorize` | `SsoOAuthController.authorize` | 本课走完 |
| `POST /sso/oauth2/token` | `SsoOAuthController.token` | 只点名，细节在 L-002 |
| `POST /sso/oauth2/revoke` | `SsoOAuthController.revoke` | 只点名，细节在 L-002 |

同一控制器还注入了三个合作者：`SsoOAuthUseCase oauthUseCase`、`SsoSessionUseCase sessionUseCase`、`SsoProperties properties`。授权这条路**两个都用**：Cookie 名来自 `properties.getCookieName()`，当前人来自 `sessionUseCase.current(...)`，真正发码来自 `oauthUseCase.authorize(...)`。

**类比失效处：** 认人厅不是整栋楼的万能通行证。厅里有人（SSO 会话）不等于业务 App 已经拿到 Sa-Token。授权成功时控制器返回的是 JSON 里的回调地址，不是服务器自己 `302` 跳走。纸条（code）默认大约 5 分钟过期，不是“一直放在抽屉里”。

**图题 / caption：** `GET /sso/oauth2/authorize` 从 Cookie 到授权码表的接力。虚线是“还没登录”早退。

```text
浏览器 / sso-web
  |  GET /sso/oauth2/authorize
  |  query: response_type, client_id, redirect_uri,
  |         state?, code_challenge?, code_challenge_method?
  v
SsoOAuthController.authorize          (@SaIgnore, /sso/oauth2)
  |  readSessionId(request)  --用-->  SsoProperties.getCookieName()
  |  sessionUseCase.current(sessionId)
  v
SsoSessionUseCaseImpl.current
  v
SsoSessionService.current
  v
SsoSessionPort.find  =  RedisSsoSessionStore.find
  |  key = "sso:session:" + sessionId
  v
SsoAuthenticatedUser 或 null
  |
  |  组装 SsoOAuthCommands.AuthorizeCommand(user 可空)
  v
SsoOAuthUseCase.authorize
  v
SsoOAuthUseCaseImpl.authorize         (只转交)
  v
SsoAuthorizationService.authorize
  |-- requireEnabledClient --> SsoClientCatalogPort.findByClientId
  |                            WtaApiSsoClientCatalogAdapter.findByClientId
  |-- requireExactRedirect
  |-- PkceS256.requireChallenge / requireS256
  |-- state 非空
  |
  +-- user == null --> AuthorizeResult.needsLogin()  .... 早退，不写库
  |
  +-- user != null
        |-- SsoIdentityPort.assertClientAccess
        |   WtaApiSsoIdentityAdapter.assertClientAccess
        |-- newCode() + 填 SsoAuthorizationCode
        |-- SsoAuthorizationCodeDao.insert
        |   SsoAuthorizationCodeDaoImpl.insert
        |   SsoAuthorizationCodeMapper.insert --> MySQL sso_authorization_code
        +-- AuthorizeResult.redirect(appendQuery(...))
  v
SsoAuthorizeVo { loginRequired, redirectUri } 包进 R.ok
```

**文字等价物：** 请求先进入 `SsoOAuthController.authorize`。它用私有方法 `readSessionId` 在 Cookie 里找 `SsoProperties` 配置的名字（默认 `Sso-Token`），再调用 `SsoSessionUseCase.current`。这条会话链经过 `SsoSessionUseCaseImpl.current`、`SsoSessionService.current`、`RedisSsoSessionStore.find`，从 Redis 键 `sso:session:` 前缀取出 `SsoAuthenticatedUser`，没有 Cookie 或键不存在就是 `null`。控制器把查询参数和这个用户（可空）放进 `AuthorizeCommand`，调用 `SsoOAuthUseCase.authorize`。实现类 `SsoOAuthUseCaseImpl` 只有一个合作者 `SsoAuthorizationService`，原样转交。服务里先查 Client、对回调地址做精确白名单、强制 PKCE S256、强制 `state`。若用户仍是空，返回 `needsLogin()`，不写授权码。若用户在，先 `assertClientAccess`，再 `newCode` 生成一次性码，经 `SsoAuthorizationCodeDao.insert` 落到表 `sso_authorization_code`，最后用 `appendQuery` 把 `code` 和 `state` 拼进回调地址。控制器把结果抄到 `SsoAuthorizeVo` 后 `R.ok` 回去。

**图的边界：** 本图不包含 `POST /token` 如何验 verifier，也不包含 `SsoSessionCookie.create`——写 Cookie 是 `SsoSessionController` 的事，授权入口只读。`webOrigin` 字段在 `SsoProperties` 里存在，但本方法不读它。生产环境是否已部署 SSO 仍是未决项，本课不关闭。

## 核心概念与机制

### 直觉讲解

想象你要进游乐场的某个具体项目（业务 Client）。门口保安不看你口袋里别的公园年卡，只认两件事：

1. 你有没有在**这个公园的认人厅**戴上手环（SSO 域会话，Cookie 里只有手环编号）。
2. 你要去的项目是不是允许这个回调门口，以及你是不是带着 PKCE 暗号。

暗号怎么理解：你先在心里想一句很长的秘密（`code_verifier`），把它搅成一团看不懂的字（`code_challenge` = SHA-256 再 BASE64URL）交给保安。保安把这团字和一次性纸条一起锁进抽屉。以后换票的人必须拿出那句秘密，搅出来必须一模一样。这就是 **PKCE S256**。

如果手环还没戴上，保安不会把纸条塞进抽屉。他会说“先去登录”。在本实现里，这句话是 JSON 字段 `loginRequired=true`，不是服务器直接把你 302 到登录页。

**类比失效处：** 真实游乐场保安可能口头通融。这里不行：`code_challenge_method` 必须是 `S256`，`plain` 会被 `PkceS256.requireS256` 拒绝；回调地址必须和目录里登记的字符串**完全相等**，带 `*` 直接失败。HTTP 层把 `state` / `code_challenge` 标成可选查询参数，但服务层缺了一样就抛 `ServiceException`。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，对着源码） |
| --- | --- | --- |
| 授权码请求 | authorization request | `GET /sso/oauth2/authorize`，由 `SsoOAuthController.authorize` 接收 |
| 授权码 | authorization code | 表 `sso_authorization_code` 里一行一次性短码，字段 `authorizationCode`，由 `newCode()` 生成 |
| 证明密钥交换 | PKCE S256 | `PkceS256`：挑战必须存在且 method 为 `S256`；换票时再 `verify` |
| SSO 域会话 | SSO session | Redis 中的认人状态；授权只通过 `SsoSessionUseCase.current` 读取，不在本方法里创建 |
| 登录必要 | login required | `SsoOAuthCommands.AuthorizeResult.needsLogin()`：`loginRequired=true`，`redirectUri=null`，不写库 |
| 回调白名单 | redirect URI allow-list | `requireExactRedirect`：禁止通配符，必须等于 `SsoClientView.getRedirectUris()` 中某一条 |
| Client 目录 | client catalog | `SsoClientCatalogPort.findByClientId`，适配器 `WtaApiSsoClientCatalogAdapter` |
| 准入 | client access | `SsoIdentityPort.assertClientAccess(userId, clientId)`，在发码之前 |
| 用例接口 | use case | `SsoOAuthUseCase.authorize`；实现 `SsoOAuthUseCaseImpl` 只委托服务 |

`SsoOAuthUseCaseImpl` 是 layered 模块里很薄的一页纸：三个方法各自一行，全部进 `SsoAuthorizationService`。规则不写在控制器里。`wta-sso` 在模块模式表上登记为 **layered**（来源 S-SSO-08）。

### 机制/因果链

按真实调用顺序。每一步都能指到方法名。

**0. 开关与匿名。** `SsoOAuthController` 带 `@ConditionalOnProperty(prefix = "namewta.sso", name = "enabled", havingValue = "true", matchIfMissing = true)`。缺省当作启用。`@SaIgnore` 表示这条 HTTP 不先要业务 Token。`SsoOAuthUseCaseImpl` 使用同一条件。`SsoAuthorizationService` 本身不是 `@Service`，而是 `SsoServiceConfiguration.ssoAuthorizationService` 这个 `@Bean` 手工 `new` 出来的，TTL 取 `properties.getCodeTtl()`（默认 5 分钟），时钟取 `ssoClock()` = `Clock.systemDefaultZone()`。

**1. 查询参数进控制器。** `authorize` 的形参：

- `response_type` → `responseType`（必填）
- `client_id` → `clientId`（必填）
- `redirect_uri` → `redirectUri`（必填）
- `state`（HTTP `required = false`）
- `code_challenge`（HTTP `required = false`）
- `code_challenge_method`（HTTP `required = false`）
- `HttpServletRequest request`（只为读 Cookie）

**2. 读会话 Cookie。** 私有方法 `SsoOAuthController.readSessionId`：`request.getCookies()` 为 `null` 则返回 `null`；否则遍历，名字等于 `properties.getCookieName()` 时返回 `cookie.getValue()`。默认 Cookie 名是 `Sso-Token`。这里**不调用** `SsoSessionCookie`。

**3. 会话用例。** `sessionUseCase.current(readSessionId(request))`。接口方法是 `SsoSessionUseCase.current`。实现 `SsoSessionUseCaseImpl.current` 转给 `SsoSessionService.current`，再转给 `SsoSessionPort.find`。工作树适配器是 `RedisSsoSessionStore.find`：会话标识空白则 `null`；否则 `RedisUtils.getCacheObject("sso:session:" + sessionId)`。返回类型是 `SsoAuthenticatedUser`（`userId` + `username`），没有业务 Token。

**4. 组装命令。** `new SsoOAuthCommands.AuthorizeCommand(responseType, clientId, redirectUri, state, codeChallenge, codeChallengeMethod, user)`。`user` 可以是 `null`。然后 `oauthUseCase.authorize(command)`。

**5. 用例实现。** `SsoOAuthUseCaseImpl.authorize` 的方法体就是 `return authorizationService.authorize(command);`。合作者只有 `SsoAuthorizationService`。

**6. 服务负向检查（无论有没有登录都做）。** `SsoAuthorizationService.authorize`：

1. `command == null` → `ServiceException("授权请求无效")`。
2. `responseType` 去掉空白后必须忽略大小写等于 `"code"`，否则 `"只支持 response_type=code"`。
3. 私有 `requireEnabledClient(command.clientId())`：
   - 空 `clientId` → `"缺少 client_id"`；
   - `clientCatalog.findByClientId`；适配器 `WtaApiSsoClientCatalogAdapter.findByClientId` 再调 `SsoClientCatalog.findByClientId`；
   - `client == null` 或 `status` 不是 `SystemConstants.NORMAL` → `"客户端不可用"`；
   - `ssoEnabled` 不是 `Boolean.TRUE` → `"客户端未启用 SSO"`。
4. 私有 `requireExactRedirect(client, redirectUri)`：
   - 空 → `"缺少 redirect_uri"`；
   - 含 `*` → `"SSO 回调白名单禁止通配符"`；
   - `client.getRedirectUris()` 里没有任何一条 `equals` 这次 trim 后的值 → `"回调地址未登记"`。
5. `PkceS256.requireChallenge(command.codeChallenge())`：空白 → `"缺少 code_challenge"`。
6. `PkceS256.requireS256(command.codeChallengeMethod())`：空白或不是 `S256`（trim 后大写比较 `PkceS256.METHOD`）→ `"code_challenge_method 必须为 S256"`。
7. `state` 空白 → `"缺少 state"`。

**7. 登录分支。** `if (command.user() == null) return SsoOAuthCommands.AuthorizeResult.needsLogin();`。工厂方法得到 `(loginRequired=true, redirectUri=null)`。**到这里为止还没有 `insert`。** 这是本课最容易说错的分叉。

**8. 已登录才发码。** `identityPort.assertClientAccess(command.user().getUserId(), client.getClientId())`。适配器 `WtaApiSsoIdentityAdapter.assertClientAccess` 转到 `SsoIdentityService.assertClientAccess`。过了才 `new SsoAuthorizationCode()` 并填字段：

- `setAuthorizationCodeId(IdGeneratorUtil.nextLongId())`
- `setAuthorizationCode(newCode())`：32 字节 `ThreadLocalRandom`，`Base64.getUrlEncoder().withoutPadding()`
- `setClientId(client.getClientId())`
- `setRedirectUri(command.redirectUri().trim())`
- `setCodeChallenge(command.codeChallenge().trim())`
- `setState(command.state().trim())`
- `setUserId` / `setUsername` 来自当前 `SsoAuthenticatedUser`
- `setConsumed(Boolean.FALSE)`
- `setExpireTime(LocalDateTime.now(clock).plus(codeTtl))`
- `setVersion(0)`

然后 `codeDao.insert(code)` → `SsoAuthorizationCodeDaoImpl.insert` → `SsoAuthorizationCodeMapper.insert`，表名 `sso_authorization_code`。实体带 `@Version` 和 `@TableLogic`，给换票时的乐观锁和逻辑删除预留，本方法只写入。

**9. 拼回调。** 私有 `appendQuery(redirectUri, code, state)`：原地址已有 `?` 就用 `&`，否则用 `?`，拼 `code=` 和 `&state=`。`AuthorizeResult.redirect(...)` 得到 `(loginRequired=false, redirectUri=带查询串的地址)`。

**10. 控制器出参。** `SsoAuthorizeVo.setLoginRequired(result.loginRequired())`，`setRedirectUri(result.redirectUri())`，`return R.ok(vo)`。没有 `Set-Cookie`，没有 HTTP 重定向状态码。

同控制器另外两扇门（本课只记名字，因果在 L-002）：

- `SsoOAuthController.token` → `SsoOAuthUseCase.exchange` → `SsoOAuthUseCaseImpl.exchange` → `SsoAuthorizationService.exchange` → `SsoAuthorizationCodeDao.findByCode` / `consumeIfUnconsumed`、`PkceS256.verify`、`SsoIdentityPort.buildLoginUser`、`SsoBusinessTokenPort.issue`（`SaTokenSsoBusinessTokenAdapter.issue`）。
- `SsoOAuthController.revoke` → `SsoOAuthUseCase.revoke` → `SsoOAuthUseCaseImpl.revoke` → `SsoAuthorizationService.revoke` → `SsoBusinessTokenPort.revoke`（`SaTokenSsoBusinessTokenAdapter.revoke`）。授权路径**不调用**这两条。

### 图、表或文本图

**图题 / caption：** 已登录与未登录两条出口，检查顺序相同。

```text
AuthorizeCommand
      |
      v
[协议与目录检查]  response_type=code
                  requireEnabledClient
                  requireExactRedirect
                  PkceS256.requireChallenge + requireS256
                  state 非空
      |
      +-- user == null ----> needsLogin() ----> JSON loginRequired=true
      |
      +-- user != null
            assertClientAccess
            insert 授权码
            redirect(appendQuery) ----> JSON loginRequired=false, redirectUri=...?code=&state=
```

**文字等价物：** 两支都要先过 Client、回调、PKCE、state。区别只在最后：没有 `SsoAuthenticatedUser` 就 `needsLogin` 并且不写 MySQL；有用户就准入检查、插入 `SsoAuthorizationCode`、返回带 `code` 与 `state` 的回调串。控制器始终 `R.ok`，用字段告诉前端下一步。

**图的边界：** 图不表示 HTTP 302。也不表示“未登录时先发码、登录后再绑定用户”——源码没有这条路径。

### 正例、反例与边界

**正例 1（已登录发码）。** Cookie 里有有效 `Sso-Token`，`RedisSsoSessionStore.find` 得到用户。`response_type=code`，`client_id` 对应 `status=NORMAL` 且 `ssoEnabled=true` 的 Client，`redirect_uri` 与目录精确相等，`code_challenge_method=S256`，`state` 非空。`assertClientAccess` 通过后 `insert`，`loginRequired` 为 false，`redirectUri` 含本次 `newCode()` 的值和原 `state`。

**正例 2（未登录早退）。** 同样通过 Client / PKCE / state 检查，但 `readSessionId` 得到 `null` 或 Redis 里没有键。`user` 为 `null`，返回 `needsLogin()`。表里**不会**多一行授权码。前端应先走 `POST /sso/login`（L-003），再带着 Cookie 重打 authorize。

**反例 1（HTTP 可选 ≠ 服务可选）。** 查询串省略 `code_challenge`。Spring 不会在参数绑定时报错（`required = false`），但 `PkceS256.requireChallenge` 会抛 `"缺少 code_challenge"`。`state` 同理。

**反例 2（通配符回调）。** `redirect_uri` 带 `*`，或和目录里登记的串差一个斜杠。`requireExactRedirect` 拒绝。不存在“前缀匹配”。

**反例 3（错误的 response_type）。** `token` 或空串。服务在查目录之前就会说只支持 `code`。本模块不做 implicit flow。

**边界：** `GET` 在已登录成功支会写入数据库。这是 OAuth 授权端点的常见形状，和仓库里“变更用 POST”的业务 CRUD 习惯不一样。不要把它抄成普通资源的 GET 写库模板。`@Log` 没有打在 `authorize` 上，打在 `token` / `revoke` 上。

## 变式与迁移

把同一套“门口发一次性纸条”迁到别的问题上时，只迁机制，不迁字段名：

- **变式 A：同一用户、另一个 Client。** 必须重新 `authorize`。code 行上绑死了 `clientId` 和 `redirectUri`。L-002 的 `exchange` 会核对这两项。
- **变式 B：登录页回来之后。** 数据流仍是本课这条 GET。变的是 Cookie 从无到有。`SsoOAuthController.authorize` 自己不登录。
- **变式 C：把规则塞进控制器。** 迁移失败。控制器只做读 Cookie、组命令、抄 VO。PKCE、白名单、发码全在 `SsoAuthorizationService`。
- **变式 D：业务 App 直接读 `Sso-Token` 当登录态。** 这会拆掉 Client 隔离。授权码的存在就是为了以后换成**目标业务 Client** 的票，而不是复用认人厅手环。

**迁移口诀：** 问三句——谁读会话？谁发码？码绑了哪些字段？答案必须分别能指到 `SsoSessionUseCase.current`、`SsoAuthorizationService.authorize`、`SsoAuthorizationCode` 的 `clientId`/`redirectUri`/`codeChallenge`/`userId`。

## 常见误区

1. **误区：authorize 会 Set-Cookie。** 不会。写 Cookie 的是 `SsoSessionController.writeCookie` + `SsoSessionCookie.create`。本方法只 `readSessionId`。
2. **误区：`loginRequired` 等于 HTTP 401 或 302。** 源码是 `R.ok` 包着 VO。成功和“请先登录”都走 ok 包装，靠布尔字段区分。
3. **误区：没登录就谈不上 PKCE。** 正好相反：没登录也要先过 challenge / S256 / state / Client。非法 Client 不会得到一个“去登录”的温柔出口。
4. **误区：`SsoOAuthUseCaseImpl` 里有 PKCE 算法。** 算法在 `PkceS256`，规则在 `SsoAuthorizationService`。Impl 只转交。
5. **误区：授权码就是业务 Token。** 码进 MySQL；业务 Token 要到 `exchange` 才由 `SsoBusinessTokenPort.issue` 签发。本课路径不碰 `SsoBusinessTokenPort`。
6. **误区：Cookie 里装着用户名密码或 Sa-Token。** Cookie 值是会话标识。用户对象在 Redis。密码验证属于 L-003 的 `login`。

## 非评分暂停

用手指着源码，不出声把这条链走一遍（不记分、不对照标准答法）：

- `SsoOAuthController` 为了授权，调用了哪两个 UseCase 方法？
- `user == null` 时，`SsoAuthorizationCodeDao.insert` 会不会执行？
- `readSessionId` 用的 Cookie 名从哪来？默认字符串是什么？
- 私有方法 `requireExactRedirect` 对 `*` 说了哪句中文异常？

卡住就回到上面的因果链第 2、第 6、第 7 步，不要发明新方法名。

## 总结、词汇表与下一步

授权入口是 **JSON 协议端点**，不是浏览器重定向引擎。`SsoOAuthController.authorize` 读 SSO Cookie，问 `SsoSessionUseCase.current`，把可空用户交给 `SsoOAuthUseCase.authorize`。`SsoOAuthUseCaseImpl` 把它交给 `SsoAuthorizationService.authorize`。服务先锁死 Client、回调、PKCE S256 和 `state`，再分叉：没人就 `needsLogin`；有人就 `assertClientAccess`、`newCode`、`SsoAuthorizationCodeDao.insert`、`appendQuery`。

| 词 | 记住哪一个方法 |
| --- | --- |
| 读手环编号 | `SsoOAuthController.readSessionId` |
| 问认人厅谁在 | `SsoSessionUseCase.current` → `RedisSsoSessionStore.find` |
| 发一次性纸条 | `SsoAuthorizationService.authorize` + `SsoAuthorizationCodeDao.insert` |
| 搅秘密 | `PkceS256.requireChallenge` / `requireS256`（验证在 L-002 的 `verify`） |

下一步：L-002 用同一对 `SsoOAuthController` + `SsoOAuthUseCaseImpl`，看 `token` / `revoke` 怎样把纸条换成业务 Sa-Token，以及撤销为什么不是跨 App 单点登出。

## 来源与引用

权威是当前工作树 Java，不是 Skill 摘要。访问日期 2026-09-14。

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-SSO-01 | `SsoOAuthController.java` | `/sso/oauth2` 三方法；`authorize` 调 `sessionUseCase.current` 与 `oauthUseCase.authorize`；`readSessionId` 用 `SsoProperties.getCookieName()` | `controller/anonymous/SsoOAuthController.java` | 2026-09-14 |
| S-SSO-03 | `SsoOAuthUseCase` / `SsoOAuthUseCaseImpl` | `authorize` 委托 `SsoAuthorizationService.authorize` | `usecase/` 与 `usecase/impl/` | 2026-09-14 |
| S-SSO-04 | `SsoSessionUseCase` / `SsoSessionUseCaseImpl` | 授权前读会话走 `current` | `usecase/` | 2026-09-14 |
| S-SSO-05 | `SsoAuthorizationService` / `SsoSessionService` | 发码规则、登录分叉、`current` 只 `sessionPort.find` | `service/` | 2026-09-14 |
| S-SSO-06 | `SsoAuthorizationCodeDao*` / `SsoAuthorizationCodeMapper` | `insert` 持久化一次性码 | `dao/`、`mapper/` | 2026-09-14 |
| S-SSO-07 | `PkceS256`、端口与适配器、`SsoProperties`、`SsoServiceConfiguration` | PKCE 强制 S256；目录与认人端口；Bean 注入 `codeTtl` | `support/`、`port/`、`adapter/`、`config/` | 2026-09-14 |
| S-SSO-08 | 后端模块模式登记 | `wta-sso` 为 layered：Controller → UseCase → Service → DAO | `03-backend-module-modes.md` | 2026-09-14 |

未决（open/uncertain）：生产环境 SSO 是否已部署。本课只讲工作树调用链。
