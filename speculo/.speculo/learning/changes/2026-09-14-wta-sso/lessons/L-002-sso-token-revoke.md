---
lesson_id: L-002
objective_ids: [OBJ-02]
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
source_ids: [S-SSO-01, S-SSO-03, S-SSO-05, S-SSO-06, S-SSO-07]
---

# Lesson 002：换票与撕票：`POST /sso/oauth2/token` 与 `/revoke`

## 学完你能做什么

你能口述 `SsoOAuthController.token` 如何把 `SsoTokenBo` 变成 `SsoOAuthCommands.TokenCommand`，经 `SsoOAuthUseCase.exchange` / `SsoOAuthUseCaseImpl.exchange` 进入 `SsoAuthorizationService.exchange`：查码、验 PKCE、原子消费、按目标 Client 组装 `LoginUser`、再经 `SsoBusinessTokenPort.issue` 发出业务 Sa-Token。你也能口述 `SsoOAuthController.revoke` → `SsoOAuthUseCase.revoke` → `SsoAuthorizationService.revoke` → `SsoBusinessTokenPort.revoke` 只作废**提交的那一张票**，不是跨 App 单点登出。本课不重讲 authorize 发码细节，但会接上 L-001 已经写入的那一行授权码。

## 先把宏观地图放在桌上

还是那栋楼。L-001 给你一张一次性纸条（code）。本课两件事：

1. **换票：** 拿纸条 + 那句秘密（`code_verifier`）去窗口，换成**某一个具体项目**的座位票（业务 Sa-Token）。
2. **撕票：** 把已经拿到的座位票交回去，窗口只撕这一张。大厅手环（SSO 会话）还在；别的项目座位票也不动。

入口仍是 `SsoOAuthController`（`@RequestMapping("/sso/oauth2")`，`@SaIgnore`）。合作者仍是 `SsoOAuthUseCase`、`SsoSessionUseCase`、`SsoProperties`。**换票和撤销这两条路径不读 Cookie、不调 `sessionUseCase`。** 它们只调 `oauthUseCase`。`SsoOAuthUseCaseImpl` 的唯一合作者仍是 `SsoAuthorizationService`。

同控制器三扇门在本课的职责：

| HTTP | 控制器方法 | UseCase 方法 | Service 方法 | 本课 |
| --- | --- | --- | --- | --- |
| `GET /authorize` | `authorize` | `authorize` | `authorize` | 只回看：码从哪来 |
| `POST /token` | `token` | `exchange` | `exchange` | 走完 |
| `POST /revoke` | `revoke` | `revoke` | `revoke` | 走完 |

名字陷阱：HTTP 方法叫 `token`，用例和方法却叫 `exchange`。口述时不要说“UseCase.token”——源码里没有这个方法。

**类比失效处：** 电影院“换票”故事里，工作人员可能看你脸就放行。这里必须 `PkceS256.verify` 成功，且 `client_id`、`redirect_uri` 必须和发码那一行**字符串相等**。撕一张票不等于清场：`revoke` 注释写明“不等于跨 App SLO”，实现也只 `tokenPort.revoke(token)`。

**图题 / caption：** 换票从 JSON 体到 Sa-Token；撤销从 JSON 体到 `StpUtil.logoutByTokenValue`。

```text
POST /sso/oauth2/token
  body SsoTokenBo { grant_type, code, redirect_uri, client_id, code_verifier }
        |
        v
SsoOAuthController.token
  @Log(title="SSO换票", excludeParamNames={code, codeVerifier, code_verifier})
        |
        |  oauthUseCase.exchange(new TokenCommand(...))
        v
SsoOAuthUseCaseImpl.exchange
        v
SsoAuthorizationService.exchange
        |-- codeDao.findByCode                 SsoAuthorizationCodeDaoImpl.findByCode
        |                                      mapper.selectOne(... limit 1)
        |-- 负向：空 / 已消费 / 过期 / client 不符 / redirect 不符
        |-- PkceS256.verify(verifier, 行上的 codeChallenge)
        |     \- 内部再调 PkceS256.challenge
        |-- codeDao.consumeIfUnconsumed(code, version)
        |     SsoAuthorizationCodeDaoImpl.consumeIfUnconsumed
        |     SsoAuthorizationCodeMapper.consumeIfUnconsumed
        |     XML: consumed=0 AND version=? 才 update
        |-- requireEnabledClient(行上的 clientId)
        |     SsoClientCatalogPort.findByClientId
        |     WtaApiSsoClientCatalogAdapter.findByClientId
        |-- identityPort.buildLoginUser(userId, clientId)
        |     WtaApiSsoIdentityAdapter.buildLoginUser
        |-- tokenPort.issue(loginUser, client)
              SaTokenSsoBusinessTokenAdapter.issue
                SsoTokenExtras.bind(client)   禁止 clientId/clientKey = "sso"
                LoginHelper.login(user, extras)
                IssuedToken(StpUtil.getTokenValue(), StpUtil.getTokenTimeout(), clientId)
        v
SsoTokenVo { access_token, expire_in, client_id }  → R.ok

POST /sso/oauth2/revoke
  body SsoTokenBo { token }
        |
        v
SsoOAuthController.revoke
  @Log(title="SSO撤销令牌", excludeParamNames={token})
        |
        |  oauthUseCase.revoke(bo.getToken())
        v
SsoOAuthUseCaseImpl.revoke
        v
SsoAuthorizationService.revoke
        v
SsoBusinessTokenPort.revoke
        v
SaTokenSsoBusinessTokenAdapter.revoke
        |-- 空 token → "缺少 token"
        +-- StpUtil.logoutByTokenValue(token)
```

**文字等价物：** 换票请求进 `SsoOAuthController.token`，敏感字段被 `@Log` 排除。控制器不读会话，直接 `oauthUseCase.exchange`。`SsoOAuthUseCaseImpl.exchange` 转给 `SsoAuthorizationService.exchange`。服务只接受 `grant_type=authorization_code`，用 `SsoAuthorizationCodeDao.findByCode` 取出 L-001 写入的那一行，检查未消费、未过期、Client 与回调一致，再 `PkceS256.verify`。通过后用带乐观锁的 `consumeIfUnconsumed` 把 `consumed` 打成 1。然后再次确认 Client 仍启用，用 `SsoIdentityPort.buildLoginUser` 按**目标业务 Client** 组装 `LoginUser`，交给 `SaTokenSsoBusinessTokenAdapter.issue`：`SsoTokenExtras.bind` 写入 extras，禁止以 `sso` 为业务票身份，`LoginHelper.login` 后取出 `StpUtil.getTokenValue()` 和超时秒数。控制器抄进 `SsoTokenVo`。撤销路径更短：`SsoOAuthController.revoke` → `SsoOAuthUseCase.revoke` → `SsoAuthorizationService.revoke` → `SaTokenSsoBusinessTokenAdapter.revoke` → `StpUtil.logoutByTokenValue`。它不删 Redis 会话，不改授权码表。

**图的边界：** 图不包含 SSO Cookie。`SsoTokenBo` 虽有 `token` 字段，换票方法不读它；撤销方法不读 `code`。并发下 16 个线程换同一码，测试期望最多成功一次——图只画单请求。

## 核心概念与机制

### 直觉讲解

换票像用**寄存柜号码 + 你自己设的密码**取回行李：

- 号码是 `code`（别人可能偷看到 URL）。
- 密码是 `code_verifier`（授权时没有在网上走明文，网上走的是搅过的 `code_challenge`）。
- 柜子上还贴着：这是哪家店的（`clientId`）、货要从哪个门拿走（`redirectUri`）。贴条对不上就不开柜。
- 柜门只能开一次。两个人同时拧把手，数据库用 `version` 和 `consumed=0` 保证最多一人拿走。

撕票像把已经盖章的入场券交给检票员作废。检票员不把你请出整座商场，也不去别的影厅收票。

**类比失效处：** 生活里寄存柜密码可能是 4 位数字。这里 `PkceS256.verify` 要求 verifier 长度 43 到 128，并且 `challenge(verifier)` 必须等于行上保存的挑战。RFC 7636 附录 B 的样例在测试里：verifier `dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk` 对应挑战 `E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM`。另外，撕票**不会**自动注销 `Sso-Token` 手环。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 换票 | token exchange / authorization code grant | `SsoOAuthController.token` 调 `SsoOAuthUseCase.exchange`；`grant_type` 必须是 `authorization_code` |
| 授权码消费 | consume | `SsoAuthorizationCodeDao.consumeIfUnconsumed`：仅当 `consumed=0` 且 `version` 匹配时更新 |
| 证明密钥校验 | PKCE verification | `PkceS256.verify(codeVerifier, record.getCodeChallenge())` |
| 业务令牌 | business token | `SsoBusinessTokenPort.IssuedToken`：`accessToken`、`expireIn`、`clientId`，由现有 Sa-Token 签发 |
| 签发适配器 | token adapter | `SaTokenSsoBusinessTokenAdapter.issue` / `revoke` |
| 令牌附加数据 | token extras | `SsoTokenExtras.bind`：写入目标 Client 的 extras，拒绝 SSO 中心 Client |
| 撤销 | revoke | 只对提交的字符串调用 `StpUtil.logoutByTokenValue` |
| 单点登出 | SLO (single logout) | 本路径**不做**；注释写明不等于跨 App SLO |

`IssuedToken` 是 `SsoBusinessTokenPort` 里的 record，不是另一张表。`SsoTokenVo` 用 `@JsonProperty` 把字段对齐现有登录 VO：`access_token`、`expire_in`、`client_id`。

### 机制/因果链

#### A. `POST /sso/oauth2/token`

**1. 控制器。** `SsoOAuthController.token(@RequestBody SsoTokenBo bo)`。`SsoTokenBo` 字段：

- `grantType` JSON 名 `grant_type`
- `code`
- `redirectUri` JSON 名 `redirect_uri`
- `clientId` JSON 名 `client_id`
- `codeVerifier` JSON 名 `code_verifier`
- `token`（本方法不用）

`@Log` 的 `excludeParamNames` 包含 `code`、`codeVerifier`、`code_verifier`，避免日志泄漏一次性秘密。然后：

```text
oauthUseCase.exchange(new SsoOAuthCommands.TokenCommand(
    bo.getGrantType(), bo.getCode(), bo.getRedirectUri(), bo.getClientId(), bo.getCodeVerifier()))
```

出参：`issued.accessToken()` / `expireIn()` / `clientId()` 写入 `SsoTokenVo`，`R.ok(vo)`。

**2. 用例。** `SsoOAuthUseCase.exchange` 返回 `SsoBusinessTokenPort.IssuedToken`。`SsoOAuthUseCaseImpl.exchange` 一行：`return authorizationService.exchange(command);`。

**3. 服务 `SsoAuthorizationService.exchange` 的闸门（按源码顺序）：**

1. `command == null` → `"换票请求无效"`。
2. `grantType` trim 后忽略大小写必须是 `"authorization_code"`，否则 `"只支持 grant_type=authorization_code"`。没有 refresh_token 分支。
3. `code` 空白 → `"缺少 code"`。
4. `codeDao.findByCode(command.code())`。实现：`SsoAuthorizationCodeDaoImpl.findByCode` 用 `LambdaQueryWrapper` 按 `SsoAuthorizationCode::getAuthorizationCode` 相等，`.last("limit 1")`，`mapper.selectOne`。找不到 → `"授权码无效"`。
5. `Boolean.TRUE.equals(record.getConsumed())` → `"授权码已使用"`（先读后写的第一道门）。
6. `expireTime == null` 或 `!expireTime.isAfter(LocalDateTime.now(clock))` → `"授权码已过期"`。TTL 来自组装 Bean 时的 `properties.getCodeTtl()`，默认 5 分钟。测试把钟拨快 6 分钟后同一码失败。
7. `record.getClientId()` 与 `command.clientId()` 必须 `StringUtils.equals`，否则 `"授权码与客户端不匹配"`。把 admin 的码拿到 home Client 去换会失败。
8. `record.getRedirectUri()` 与 `StringUtils.trim(command.redirectUri())` 必须相等，否则 `"授权码与回调地址不匹配"`。
9. `PkceS256.verify(command.codeVerifier(), record.getCodeChallenge())`：
   - verifier 或 challenge 空白 → `"PKCE 校验失败"`；
   - 长度不在 `[43, 128]` → 同一句；
   - `challenge(verifier)` 与保存值不等 → 同一句。
   - `challenge` 内部：`MessageDigest.getInstance("SHA-256")`，对 US_ASCII 字节做摘要，再 URL Base64 无填充。空白 verifier 在 `challenge` 里会说 `"缺少 code_verifier"`，但 `verify` 先拦住空白。
10. `codeDao.consumeIfUnconsumed(record.getAuthorizationCode(), record.getVersion())` 必须为 true，否则再报 `"授权码已使用"`。这是第二道门，防并发双花。
11. `requireEnabledClient(record.getClientId())` 再查一遍目录（状态、`ssoEnabled`），适配器仍是 `WtaApiSsoClientCatalogAdapter`。
12. `identityPort.buildLoginUser(record.getUserId(), client.getClientId())` → `WtaApiSsoIdentityAdapter.buildLoginUser`。这里才按**目标业务 Client** 长出 `LoginUser`，不是 SSO 中心身份。
13. `return tokenPort.issue(loginUser, client)`。

**4. DAO 原子消费。** `SsoAuthorizationCodeMapper.consumeIfUnconsumed` 对应 XML：

```text
update sso_authorization_code
   set consumed = 1, version = version + 1, update_time = now()
 where authorization_code = #{authorizationCode}
   and consumed = 0
   and del_flag = '0'
   and version = #{version}
```

`SsoAuthorizationCodeDaoImpl.consumeIfUnconsumed` 把更新行数 `> 0` 当成成功。`@Version` 与 XML 手写 version 条件一起拦住两个人同时拿到 `consumed=0` 的快照。

**5. 签发适配器。** `SaTokenSsoBusinessTokenAdapter.issue`：

1. `SsoTokenExtras.bind(client)`：
   - client 空或 `clientId` 空白 → `"目标业务 Client 不能为空"`；
   - `clientKey` 或 `clientId` 忽略大小写等于 `"sso"` → `"禁止签发 SSO 中心 Client 业务票"`；
   - 把 `deviceType` / `timeout` / `activeTimeout` 拷到 `SaLoginParameter`；
   - extras：`LoginHelper.CLIENT_KEY` ← `clientId`，`CLIENT_PK_KEY` ← `id`，`CLIENT_ACCESS_PATH_KEY` ← `accessPath`，`CLIENT_IP_WHITELIST_KEY` ← `ipWhitelist`。
2. `LoginHelper.login(user, 那个 SaLoginParameter)`。
3. `new IssuedToken(StpUtil.getTokenValue(), StpUtil.getTokenTimeout(), client.getClientId())`。

测试强调：发出去的 `clientId` 不是 `"sso"`。

#### B. `POST /sso/oauth2/revoke`

**1. 控制器。** `SsoOAuthController.revoke(@RequestBody SsoTokenBo bo)` 只取 `bo.getToken()`，然后 `oauthUseCase.revoke(...)`，`R.ok()` 无体。`@Log` 排除 `token`。

**2. 用例 / 服务。** `SsoOAuthUseCaseImpl.revoke` → `authorizationService.revoke(token)`。`SsoAuthorizationService.revoke` 的方法体是 `tokenPort.revoke(token);`。它**不**打开授权码 DAO，**不**调 `SsoSessionPort.delete`。

**3. 适配器。** `SaTokenSsoBusinessTokenAdapter.revoke`：空白 → `"缺少 token"`；否则 `StpUtil.logoutByTokenValue(token)`。测试 `revokeOnlyTouchesSubmittedToken`：列表里两张票，撤销一张，另一张还在。

### 图、表或文本图

**图题 / caption：** 授权码行上绑定的四件事，换票时必须对上三件再验 PKCE。

```text
sso_authorization_code 一行
  authorizationCode  ---- 请求 code
  clientId           ---- 请求 client_id
  redirectUri        ---- 请求 redirect_uri
  codeChallenge      ---- SHA256(code_verifier) 的 BASE64URL
  userId             ---- 稍后 buildLoginUser
  consumed / version ---- consumeIfUnconsumed
  expireTime         ---- clock.now 之前无效
```

**文字等价物：** 换票不是“有码就给票”。码必须仍存在、未消费、未过期，并且请求里的 Client、回调与行上一致，PKCE 挑战能用 verifier 再现。消费成功后才用行上的 `userId` 和 Client 去签发。撤销连这张表都不看，只认提交的业务 Token 字符串。

**图的边界：** `state` 存在行上，但 `exchange` **不再校验 state**。state 的战场在授权请求与浏览器回调，不在 token 端点。不要在换票体里找 `state` 字段——`TokenCommand` 没有它。

### 正例、反例与边界

**正例 1（一次换票）。** 刚从 L-001 拿到的 code，5 分钟内，`grant_type=authorization_code`，`client_id` 与 `redirect_uri` 与发码时相同，verifier 能还原 challenge。`consumeIfUnconsumed` 成功，`issue` 返回目标 Client 的 `IssuedToken`。再换一次同一 code → `"授权码已使用"`。

**正例 2（只撕提交的票）。** `revoke` 收到 admin 的 access token，home 的 token 仍可用。SSO Cookie 不受影响。

**反例 1（verifier 差一个字符）。** `PkceS256.verify` 抛 `"PKCE 校验失败"`。码此时**尚未消费**（verify 在 consume 之前）。攻击者乱猜会失败，合法客户端仍可再试——但不要把这个细节当成“可以无限重试过期码”；过期检查在 verify 之前。

**反例 2（换绑 Client）。** 用 home 的 `client_id` 去换 admin 发出的 code → `"授权码与客户端不匹配"`。换一个登记过但不是发码时那个回调 → `"授权码与回调地址不匹配"`。

**反例 3（把 SSO 中心当成业务 Client 签发）。** 即便有人把 `sso` 塞进 `SsoClientView`，`SsoTokenExtras.bind` 也会拒绝。认人厅不给自己发业务座位票。

**边界：**

- 换票不要求当前请求带 SSO Cookie。code 行已经记下 `userId`。
- `grant_type` 没有别的合法值。
- `SsoAuthorizationService` 在 `SsoServiceConfiguration` 里装配，测试里可以直接 `new` 并塞入内存 DAO——生产路径的 DAO 是 MyBatis。
- 空 token 的撤销在适配器层才失败，服务层不做空白检查。

## 变式与迁移

- **变式 A：过期后再换。** 把钟拨过 `codeTtl`。应失败在过期检查，不走到 `issue`。迁移到别的一次性凭证时，过期必须用同一时钟源（这里是注入的 `Clock`）。
- **变式 B：并发双花。** 多个 `exchange` 同时持有同一 `version`。XML 条件让只有一行更新成功。迁移到别的“一次性”资源时，不要只在 Java 里 `if (consumed) throw` 然后 update——那是窗口期。
- **变式 C：登出业务 App。** 应走 `revoke`（或业务自己的 logout），不要幻想它会清 SSO 会话。要清手环，走 L-003 的 `SsoSessionController.logout`。
- **变式 D：把 `token` 方法误叫成登录。** 登录是 `POST /sso/login`。换票是已经认过人之后的 Client 隔离步骤。

**迁移口诀：** 换票三核对（码、Client、回调）+ 一证明（PKCE）+ 一原子写（consume）+ 一签发（`tokenPort.issue`）。撤销一句话：只 `tokenPort.revoke`。

## 常见误区

1. **误区：控制器方法名 `token` 就是 UseCase 方法名。** UseCase 叫 `exchange`。
2. **误区：revoke 等于 SLO。** 源码注释和实现都否定。它甚至不碰 `SsoSessionPort`。
3. **误区：换票会再读一遍 Cookie 确认还是同一个人。** 不会。身份已经冻在授权码行的 `userId` / `username`。
4. **误区：`findByCode` 失败和 `consume` 失败是同一件事。** 前者是“没这行”，后者是“有这行但没抢到未消费版本”。
5. **误区：`SsoTokenBo.token` 也能拿来换票。** `TokenCommand` 没有 token 字段。反过来，撤销也不看 code。
6. **误区：业务 Token 的 `client_id` 可以是认人厅自己。** `SsoTokenExtras.bind` 禁止 `sso`。
7. **误区：PKCE 的 `plain` 在换票时可以凑合。** 授权阶段 `requireS256` 已经挡住；换票阶段只 `verify` S256 挑战，没有 method 字段。

## 非评分暂停

对着源码用手指划（不记分）：

- 从 `SsoOAuthController.token` 到 `StpUtil.getTokenValue`，中间经过哪些接口方法？
- `PkceS256.verify` 和 `consumeIfUnconsumed` 谁先谁后？先失败时码会不会被打成已使用？
- `SsoOAuthController.revoke` 有没有调用 `sessionUseCase`？
- `SsoAuthorizationService.revoke` 有没有调用 DAO？

卡住就回到机制 A 第 3 步的编号列表和机制 B。

## 总结、词汇表与下一步

`SsoOAuthController` 与 `SsoOAuthUseCaseImpl` 在换票/撤销里再次出现，但合作者换成了码表、PKCE 校验和 `SsoBusinessTokenPort`。`token` 方法名对 `exchange`；`revoke` 三层同名。换票把 L-001 的一次性行消费掉，签发的是目标业务 Client 的 Sa-Token。撤销只对提交字符串调用 `logoutByTokenValue`。

| 词 | 记住哪一个方法 |
| --- | --- |
| 换票入口 | `SsoOAuthController.token` → `SsoOAuthUseCase.exchange` |
| 找码 / 吃码 | `SsoAuthorizationCodeDao.findByCode` / `consumeIfUnconsumed` |
| 对暗号 | `PkceS256.verify` |
| 发座位票 | `SaTokenSsoBusinessTokenAdapter.issue` + `SsoTokenExtras.bind` |
| 撕座位票 | `SaTokenSsoBusinessTokenAdapter.revoke` |

下一步：L-003 看认人厅自己的手环——`SsoSessionController` 的 `login` / `session` / `logout`，以及 Redis 里的 `sso:session:`。

## 来源与引用

权威是当前工作树 Java。访问日期 2026-09-14。

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-SSO-01 | `SsoOAuthController.java` | `token`/`revoke` 映射；`exchange`/`revoke` 调用；`@Log` 排除敏感字段；不读 Cookie | `controller/anonymous/SsoOAuthController.java` | 2026-09-14 |
| S-SSO-03 | `SsoOAuthUseCase` / `SsoOAuthUseCaseImpl` | `exchange`/`revoke` 全委托 `SsoAuthorizationService` | `usecase/` | 2026-09-14 |
| S-SSO-05 | `SsoAuthorizationService` | 换票闸门顺序、`revoke` 只调 `tokenPort` | `service/SsoAuthorizationService.java` | 2026-09-14 |
| S-SSO-06 | DAO / Mapper / XML | `findByCode`、`consumeIfUnconsumed` 乐观锁更新 | `dao/`、`mapper/`、`mapper/sso/SsoAuthorizationCodeMapper.xml` | 2026-09-14 |
| S-SSO-07 | `PkceS256`、`SsoBusinessTokenPort`、`SaTokenSsoBusinessTokenAdapter`、`SsoTokenExtras` | S256 校验；签发/撤销 Sa-Token；禁止 sso 中心 Client | `support/`、`port/`、`adapter/gateway/` | 2026-09-14 |

未决（open/uncertain）：生产环境 SSO 是否已部署。本课不关闭该缺口。
