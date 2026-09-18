---
lesson_id: L-059
objective_ids: [OBJ-59]
claimed_cells:
  - A:SsoClientCatalog
  - A:SsoIdentityService
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-contracts-on-disk
    minutes: 8
  - segment: identity-runtime-and-ports
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-012, S-015, S-L059-01, S-L059-02, S-L059-03, S-L059-04, S-L059-05, S-L059-06, S-L059-07, S-L059-08]
---

# Lesson 059：宏观只读目录——`SsoClientCatalog` 与 `SsoIdentityService`

## 学完你能做什么

打开 `backend/wta-api/src/main/java/org/namewta/sso/api/`，这个分包**正好四份** Java：`SsoClientCatalog`、`SsoClientView`、`SsoIdentityService`、`SsoAuthenticatedUser`。没有第五份接口，没有 `ISysClientService`，没有 Mapper，没有 HTTP。你能**口述认人厅怎样隔着这扇合同窗去读用户与 Client**，而不去翻 system 房间的抽屉。

口试名单就是矩阵 **(a)** 这一行，符号以**磁盘**为准：

1. **`A:SsoClientCatalog`**：唯一方法 `findByClientId(String clientId)`。找不到返回 `null`。实现全仓只有 `SystemSsoClientCatalog`（`wta-system`）。视图是 `SsoClientView`。
2. **`A:SsoIdentityService`**：正好三法——`verifyPassword`、`assertClientAccess`、`buildLoginUser`。实现全仓只有 `AdminSsoIdentityService`（`wta-admin`）。认人结果是 `SsoAuthenticatedUser`（`userId` + `username`）；组装结果才是 `LoginUser`。

OBJ-59 要你当场说完的那句是：**`wta-sso` 读用户、读 Client、做登录域准入，只经 `wta-api` 的这两份接口。它的 POM 不依赖 `wta-system`。它不注入 `ISysClientService`，不碰 `SysUserMapper`。目录不抄 SSO 哈希，认人不签发业务 Token，换票时禁止目标 Client 的 `clientKey` 为 `sso`。**

登记表把 `wta-sso` 标 **layered**，旁注就是「仅经 wta-api 读取用户与 Client」。`wta-sso/AGENTS.md` 同一句。本课把这句钉到**方法、字段、实现落点**，不是再背一遍五层目录。

本课不宣称你会拆 `GET /sso/oauth2/authorize` 的 PKCE/发码（L-055 / 子课 L-001）、`POST /sso/oauth2/token` 与 `/revoke`（L-056 / 子课 L-002）、`POST /sso/login` Cookie 会话（L-057 / 子课 L-003）、sso-web 五件事（L-058）、或 `SysClientController` / `SysSsoAppController` 十四扇管理窗（L-018）。今天只认：**合同窗上有什么、谁填窗、认人厅怎样伸手、伸手时碰不碰抽屉里的密钥。**

## 先把宏观地图放在桌上

L-002 已经说过：`wta-api` 是后厨之间的点菜单，实现可以落在模块或主机。L-005 点过 `SsoIdentityService` 这个名字，没拆方法。L-018 管的是 `sys_client` 那只抽屉的两扇管理窗，并明确把「邻居只读适配器」推到本课。L-015 点过 SSO 认人共用临时密码柜，但消费顺序夹不夹 Client 准入是本课的格子。

四栋楼、两扇合同窗：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `ISysClientService` | system 房间内部 Service，**不在** `wta-api` | 邻居禁区；本课只认「不能注入」 |
| `SysClientController` / `SysSsoAppController` | 管理员改抽屉 | L-018 |
| `SsoClientCatalog` + `SsoClientView` | 给 SSO 看的**一层应用目录** | **本课** |
| `SsoIdentityService` + `SsoAuthenticatedUser` | 给 SSO 验密、准入、组装登录态 | **本课** |
| `SsoClientCatalogPort` / `SsoIdentityPort` | `wta-sso` 自己的门缝，测试可假扮 | 本课认「适配器怎么转交」；不把 Port 当成第二份 `wta-api` |
| `SsoOAuthController` / `SsoSessionController` | 认人厅对外 HTTP | L-055…L-057 |
| `ssoApi.ts` | 浏览器认人厅 | L-058 |
| `UserService`（`org.namewta.system.api`） | 别的模块查人、翻译名字 | **另一扇** api 窗；SSO **不**走它 |

2026-09-17 工作树：`wta-api` 的 `org.namewta.sso.api` 只有上述四份类型。`wta-sso/pom.xml` 依赖 `wta-api`，**没有** `wta-system`。`wta-sso` 生产代码对 `org.namewta.system.*` 的 import **只有** `LoginUser`（它住在 `wta-api`，不在 system 模块）。

```text
管理员改抽屉（L-018）              认人厅 HTTP（L-055…L-057）
 /system/client  /system/ssoApp         /sso/oauth2/*  /sso/login
        │                                      │
        v                                      v
 ISysClientService                    SsoAuthorizationService
 SysUserMapper                        SsoSessionService
        │                                      │
        │  填窗                                │  只伸手到 Port
        v                                      v
 ┌────────────── wta-api/sso.api ──────────────┐
 │  SsoClientCatalog.findByClientId            │
 │  SsoIdentityService.verifyPassword          │
 │                 .assertClientAccess         │
 │                 .buildLoginUser             │
 └──────────────┬───────────────────┬──────────┘
                │                   │
                v                   v
 SystemSsoClientCatalog     AdminSsoIdentityService
 (wta-system, classic)      (wta-admin, 主机)
 queryByClientId            SysUserMapper + ISysClientService
 不抄 ssoSecretHash         + TemporaryPasswordService
                            + SysLoginService
```

**类比：** 小区门口的保安亭要核对「这家店今晚开不开、这个人是不是这家店的会员」。保安亭自己**没有**会员名册，也**没有**店里保险柜的密码。它只能按对讲机（`wta-api`）问两句话：

1. **电话簿**（`SsoClientCatalog`）：按门牌 `client_id` 给我一张**可以贴在玻璃上的店卡**——店开不开、SSO 开不开、回调白名单、PKCE、超时、路径白名单。卡上**没有**保险柜密码。
2. **值班前台**（`SsoIdentityService`）：核身份证（验密）、问这人能不能进**那一家**店（登录域）、最后帮他填一张**那一家店的入场腕带底单**（`LoginUser`）。前台自己**不盖**游乐项目章（不签发 Sa-Token）。

**类比失效处：**

1. 「只读」指的是 **SSO 模块不写 `sys_user` / `sys_client`，也不持有这两张表的 Mapper**。不是「验密零副作用」：临时密码柜仍可能被撕掉，错次计数仍可能被清掉。
2. 电话簿**不是**一张 `sso_client` 表。货还在 `sys_client`。目录是 system 房间把 VO **抄窄**之后递出来的视图。
3. 值班前台的实现落在 **主机 `wta-admin`**，不在 `wta-sso`，也不在 `wta-system` 的 `implements SsoIdentityService`。L-002 已划过：主机可以填 api 窗，不等于 admin 拥有用户领域。
4. 店卡上的 `ssoSecretHash` **字段在 Java 里有**，但唯一实现**从不赋值**；`fillView` 还会先把 VO 上的哈希清掉。不要把「类上有 getter」说成「换票能拿到哈希」。
5. 保安亭换票用的是 **PKCE verifier**，`SsoTokenBo` 里没有 `client_secret`。`wta-sso` 源码搜不到 `ssoSecret` / `client_secret`。
6. 入场腕带底单（`LoginUser`）≠ 认人手环（`SsoAuthenticatedUser`）≠ 业务 Sa-Token。三张纸，三个时刻。

## 核心概念与机制

### 直觉讲解

先记住三张纸条，再背方法名：

- **店卡门牌 `clientId`。** OAuth 查询参数 / 换票 body 上的那串 MD5，不是 Java 主键 `id`，也常常不是 `clientKey`（`pc` / `home` / `sso`）。种子厅堂 Client 的 `clientKey` 才是 `"sso"`，它的 `clientId` 是 `0ae7adbebb81e87a9735ed0fba0a1135`。
- **认人手环 `SsoAuthenticatedUser`。** 只有 `userId` 和 `username`。Redis 键 `sso:session:` 后面存的就是它。注释写明：不含业务 Token。
- **入场腕带底单 `LoginUser`。** 部门、角色、菜单权、`clientPk` / `clientKey` / 登录域。只在**换票**那一刻由 `buildLoginUser` 填好，再交给 `SsoBusinessTokenPort.issue`。会话登录路径的测试甚至让假 `buildLoginUser` 直接抛「SSO session must not issue a business token」。

再记住两把锁叠在「读 Client」上：

- **目录锁。** `SsoAuthorizationService.requireEnabledClient`：`findByClientId` 非空、`status` 为 `SystemConstants.NORMAL`（`"0"`）、`ssoEnabled == Boolean.TRUE`。失败文案「客户端不可用」或「客户端未启用 SSO」。这里**不**禁 `clientKey=sso`。种子厅堂 Client `sso_enabled=0`、`sso_auth_mode=local`、回调为空，会在这一关被挡。
- **认人锁。** `assertClientAccess` 再查一遍 Client（走 `ISysClientService.queryByClientId`，不是复用店卡对象），再 `ClientUserTypeAccessService.requireLoginAccess`。`buildLoginUser` 在准入之前先禁 `clientKey` 忽略大小写等于 `"sso"`。`SsoTokenExtras.bind` 再禁一次：`clientKey` **或** `clientId` 为 `"sso"`。

小孩子版只记十二句：

1. **先数包，再数方法。** `org.namewta.sso.api` 四份类型；目录 1 法，认人 3 法。
2. **先问谁填窗。** 目录 = `wta-system` 的 `SystemSsoClientCatalog`；认人 = `wta-admin` 的 `AdminSsoIdentityService`。`wta-sso` **两份都不 implements**。
3. **认人厅只伸手到自己的 Port。** `SsoClientCatalogPort` / `SsoIdentityPort`。适配器各十来行，原样转交 `wta-api`。
4. **POM 就是墙。** `wta-sso` 没有 `wta-system` 依赖。编译期伸不到 `SysUserMapper`。
5. **店卡不带保险柜密码。** `SystemSsoClientCatalog` 手工 set 字段，名单里没有 `ssoSecretHash`，也没有 `clientSecret`。
6. **`fillView` 会把 VO 哈希清掉。** 即使有人以后在目录里多写一行 set，热路径 `queryByClientId` 缓存里也已经是 `null`。
7. **验密不带 Client。** `verifyPassword(username, password)` 两个参数。不查登录域，不签发 Token，不写验证码。
8. **临时密码柜与门厅共用。** 永久 `BCrypt.checkpw` 成功则从不 `verify`。永久失败才 `verify` → **立刻** `consume`。中间**没有** `requireLoginAccess`。这和 `PasswordAuthStrategy` 相反。
9. **准入是另一枪。** 发码前 `assertClientAccess(userId, clientId)`。换票不再喊它，改喊 `buildLoginUser`（里面会再准入一次）。
10. **组装不是登录。** `buildLoginUser` 调用 `SysLoginService.buildLoginUser`，**不**调用 `LoginHelper.login`。盖章的是 `SaTokenSsoBusinessTokenAdapter`。
11. **禁止厅堂 Client 当业务票。** 种子 `clientKey=sso` 的那一行是认人厅自己的门牌，不是 admin/home 的入场腕带。
12. **换票不交 client_secret。** PKCE S256。目录即使将来填了哈希，今天的 `exchange` 也不会去 `matches`。

**类比失效边界：** 电话簿类比**不**覆盖「管理员 rotate 之后缓存还在」。rotate 不 `@CacheEvict`；但目录本来就不读哈希，所以「缓存里没有可校验哈希」对换票无影响。类比也**不**等于「验密等于 `/auth/login`」——门厅要图形验证码、要 Client、要当场发 Sa-Token；SSO 验密三件都不做。类比还不等于「`UserService` 也能拿来验密」——那扇窗没有密码方法。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 一层应用目录 | client catalog | `SsoClientCatalog.findByClientId` |
| 目录视图 / 店卡 | catalog view | `SsoClientView`；可序列化 JavaBean，不是 record |
| 认人合同 | identity service | `SsoIdentityService` 三法 |
| 认人结果 / 手环 | authenticated user | `SsoAuthenticatedUser(userId, username)` |
| 业务登录态 / 腕带底单 | login user | `org.namewta.system.api.model.LoginUser` |
| 模块门缝 | outbound port | `SsoClientCatalogPort` / `SsoIdentityPort`（`org.namewta.sso.port`） |
| 合同适配器 | API adapter | `WtaApiSsoClientCatalogAdapter` / `WtaApiSsoIdentityAdapter` |
| 目录实现 | system catalog | `SystemSsoClientCatalog`；`@Service` |
| 认人实现 | host identity | `AdminSsoIdentityService`；`org.namewta.web.sso` |
| 登录域准入 | login-domain access | `ClientUserTypeAccessService.requireLoginAccess` |
| 热路径缓存 | client cache | `CacheNames.SYS_CLIENT` = `sys_client#30d`；键是门牌字符串 |
| 回填清哈希 | fill view | `SsoClientFieldsSupport.fillView`：`ssoSecretHash=null` |
| home 路径并入 | access-path union | `ClientAccessPaths.resolve`；只对 `clientKey==home` |
| 中心 Client 禁票 | forbid center extras | `buildLoginUser` 与 `SsoTokenExtras.bind` |
| 跨模块合同面 | `wta-api` | POM 只依赖 core / json / spring-core |
| 分层登记 | layered SSO | 登记表：仅经 wta-api 读用户与 Client |

方法签名不要混：

| 合同方法 | 参数 | 返回 | 写 `sys_*` 表？ | 主要副作用 |
| --- | --- | --- | --- | --- |
| `findByClientId` | OAuth `clientId` | `SsoClientView` 或 `null` | 否 | 可能命中 30 天 Client 缓存 |
| `verifyPassword` | 用户名、密码 | `SsoAuthenticatedUser` | 否 | 可能 `consume` 临时密码；成功删 `pwd_err_cnt:`；失败加错次 |
| `assertClientAccess` | `userId`、`clientId` | `void` | 否 | 抛「客户端不可用」或登录域文案 |
| `buildLoginUser` | `userId`、目标业务 `clientId` | `LoginUser` | 否 | 读角色/菜单/岗位；禁 `clientKey=sso` |

HTTP 合同（API-005）管的是浏览器窗。这两份接口**没有** `@RequestMapping`。浏览器碰到的是 `/sso/*`（L-055…L-057）和 `/system/client`（L-018），不是 `SsoClientCatalog` 这个 Java 名。

### 机制/因果链

#### 1. 四份类型在磁盘上的真实位置

文件（`wta-api`，无 Spring 注解）：

- `.../sso/api/SsoClientCatalog.java` — 1 个方法
- `.../sso/api/SsoClientView.java` — 字段含 `ssoSecretHash` getter/setter，但那是空槽
- `.../sso/api/SsoIdentityService.java` — 3 个方法；`buildLoginUser` 的返回类型跨包引用 `LoginUser`
- `.../sso/api/SsoAuthenticatedUser.java` — 无参构造 + `(userId, username)` 构造

实现与适配：

| 角色 | 类 | 模块 | 注入什么 |
| --- | --- | --- | --- |
| 填目录窗 | `SystemSsoClientCatalog` | `wta-system` | `ISysClientService` |
| 填认人窗 | `AdminSsoIdentityService` | `wta-admin` | `SysUserMapper`、`ISysClientService`、`ClientUserTypeAccessService`、`TemporaryPasswordService`、`SysLoginService` |
| 转交目录 | `WtaApiSsoClientCatalogAdapter` | `wta-sso` | `SsoClientCatalog` |
| 转交认人 | `WtaApiSsoIdentityAdapter` | `wta-sso` | `SsoIdentityService` |

`SsoAuthorizationService` 的构造参数是 **Port**，不是 `wta-api` 接口。`SsoServiceConfiguration` 把 Port 和 DAO、TokenPort、Clock、`codeTtl` 焊进这颗 Bean。`SsoSessionService` 只注入 `SsoIdentityPort` + `SsoSessionPort`。分层纪律：Service 看见 Port；Adapter 才看见 `wta-api`。

全仓 `implements SsoIdentityService` **只有** `AdminSsoIdentityService`。全仓 `implements SsoClientCatalog` **只有** `SystemSsoClientCatalog`。不要把 Adapter 的 `implements SsoClientCatalogPort` 说成第二份目录实现。

#### 2. 目录字段怎么抄、哈希为什么是空

`SystemSsoClientCatalog.findByClientId`：

1. `clientService.queryByClientId(clientId)`。这是 `@Cacheable(cacheNames = "sys_client#30d", key = "#clientId")`。
2. `vo == null` → 返回 `null`（不是抛错）。抛「客户端不可用」的是认人厅的 `requireEnabledClient` / `requireClient`。
3. `new SsoClientView()`，然后**白名单赋值**。

抄过去的字段：`id`、`clientId`、`clientKey`、`status`、`ssoEnabled`、`ssoAuthMode`、`ssoClientKind`、`redirectUris`（来自 `vo.getSsoRedirectUriList()`，不是生字符串）、`pkceRequired`、`autoConsent`、`scope`、`deviceType`、`timeout`、`activeTimeout`、`accessPath`、`ipWhitelist`、`userTypeId`。

`accessPath` 不是原样：`ClientAccessPaths.resolve(vo.getClientKey(), vo.getAccessPath())`。仅当 `clientKey` 忽略大小写为 `home` 且白名单非空时，并入 `/system/user/getInfo`、`/system/menu/getRouters`、`/auth/logout`、`/profile/**`。其它 Client 原样返回。单测 `catalogExposesExpandedHomeAccessPathForSsoTokenExtras` 用真实种子门牌 `428a8310cd442757ae699df5d894f051` 锁死这件事。

**不抄**的（口试容易漏）：

- `ssoSecretHash` — 视图类有字段，实现不 set
- `clientSecret` — 登录明文密钥，目录类上根本没有这个字段
- `grantType` / 默认角色 / 是否允许注册 / `ssoSecretConfigured` / `ssoSecretOnce`

更早一步，`queryByClientId` 在进缓存前就走 `fillClientRuleFields` → `SsoClientFieldsSupport.fillView(vo, hashed)`。`fillView` **第一件事**是 `vo.setSsoSecretHash(null)`，再用布尔 `ssoSecretConfigured` 告诉管理窗「有没有配过」。所以目录即使想抄，热路径 VO 上也已经是 `null`。L-018 说 rotate 不驱逐这层缓存；对本课的含义是：**换票不靠这层缓存里的哈希，因为哈希根本不在店卡上，exchange 也不读它。**

`requireEnabledClient` 用店卡做三件事：存在且 `status=0`、`ssoEnabled` 为 true、授权时再 `requireExactRedirect`（精确相等、禁止 `*`）。它**不**读 `userTypeId`。登录域是认人窗的职责。于是同一门牌会被读**两遍**：一遍经目录抄店卡（协议字段），一遍经认人 `requireClient` 拿完整 `SysClientVo`（准入 + 组装）。两边都走 `queryByClientId`，缓存键相同。

#### 3. 认人三法：验密 / 准入 / 组装

**`verifyPassword`（建立 SSO 会话时喊）：**

1. `loadUserByUsername`：`userMapper.lambda().eq(SysUser::getUserName, username).voOne()`。没有人 → `UserException("user.not.exists")`。`status` 为停用 `"1"` → `user.blocked`。
2. `loginService.checkLoginAllowed(LoginType.PASSWORD, username)`：错次键 `pwd_err_cnt:` + 用户名，满了就锁。
3. `BCrypt.checkpw(password, user.getPassword())`。成功则跳过临时柜。
4. 永久失败：`temporaryPasswordService.verify(userId, password)`，对不上 → `loginFailed`（错次 +1）。对上则 **立刻** `consume`；CAS 输家也 `loginFailed`。
5. `loginSucceeded(username)`：只删错次键。**不** `updateLastLoginInfo`，**不**发 Token，**不**查 Client。
6. `return new SsoAuthenticatedUser(user.getUserId(), user.getUserName())`。

对照门厅 `PasswordAuthStrategy.authenticate`（L-013 / L-015）：永久失败后先 `requireLoginAccess`，准入失败**不**撕临时柜；成功才 `consume`。SSO 把准入拆到另一枪，所以**验密成功临时柜已空，稍后 authorize 若登录域失败，纸条不会回来**。Redis 是同一只柜，Java 不是同一段。

SSO 验密也**没有**图形验证码。`POST /sso/login` 的 body 只有用户名密码。

**`assertClientAccess`（发码前、已有手环时喊）：**

1. `requireClient(clientId)`：`queryByClientId`，空或状态不是 `"0"` → `ServiceException("客户端不可用")`。这里**不**检查 `ssoEnabled`。SSO 开关在目录那一关。
2. `clientUserTypeAccessService.requireLoginAccess(userId, client)`：Client 必须有 `userTypeId`；该登录域存在且启用；`sys_user_type_rel` 里这个人有这条关系。否则「客户端未配置登录域 / 登录域不存在 / 登录域已停用 / 当前账号不具备该应用的登录域」。
3. **不**禁 `clientKey=sso`。厅堂 Client 若有人改成 `ssoEnabled=true` 并补了回调，发码关的 `assertClientAccess` 不会单凭 key 拒绝；挡业务票的是下一枪。

**`buildLoginUser`（换票时喊）：**

1. 再 `requireClient`。
2. `"sso".equalsIgnoreCase(client.getClientKey())` → `ServiceException("禁止签发 SSO 中心 Client 业务票")`。只看 **key**，不看门牌是不是那串 MD5。
3. `userMapper.selectVoById(userId)`。空或停用 → `user.blocked`（参数是 userId 字符串）。
4. 再 `requireLoginAccess`，拿到 `SysUserTypeVo`。
5. `loginService.buildLoginUser(user, client, userType)`：填部门名、并行拉菜单权 / 角色权 / 角色列表 / 岗位，权限按 **Client 主键** 裁。返回 `LoginUser`。**到此为止。**

盖章在 Adapter：`LoginHelper.login(user, SsoTokenExtras.bind(client))`，再取出 `StpUtil.getTokenValue()`。`SsoTokenExtras.bind` 把 extras 写成目标业务的 `clientId`（门牌）、`id`、解析后的 `accessPath`、`ipWhitelist`，并再禁一次 `clientKey` 或 `clientId` 为 `"sso"`。单测 `bindsBusinessClientAndRejectsSsoCenter`：admin 门牌 `e5cd7e4891bf95d1d19206ce24a7b32e` extras 不是 `"sso"`；手造 `clientId=sso` 的视图会抛。

#### 4. 谁在什么时候喊这三法

只认调用关系，不把 HTTP 五层再走一遍（那是 L-055…L-057）：

| 时刻 | 喊目录？ | 喊认人哪一法？ | 结果纸 |
| --- | --- | --- | --- |
| `POST /sso/login` | 否 | `verifyPassword` | 手环进 Redis；Cookie 只持 sessionId |
| `GET /sso/session` | 否 | 否 | 从 Redis 读手环；响应再 `new` 一份只含两字段 |
| `POST /sso/logout` | 否 | 否 | 删 Redis；Cookie 过期 |
| `GET /sso/oauth2/authorize` 且无手环 | `findByClientId`（校验店） | 否（早退 `needsLogin`） | 不写授权码 |
| `GET /sso/oauth2/authorize` 且有手环 | 同上 | `assertClientAccess` | 写一次性 code |
| `POST /sso/oauth2/token` | 再 `findByClientId` | `buildLoginUser` | 业务 Sa-Token；PKCE，无 secret |
| `POST /sso/oauth2/revoke` | 否 | 否 | 只 `tokenPort.revoke` |

`SsoSessionService.login` 空白用户名或密码直接「用户名或密码错误」，**不**进 `verifyPassword`。授权码路径在 `user == null` 时**不**喊准入，避免没登录的人去撞登录域。

换票 `SsoTokenBo` 字段：`grant_type`、`code`、`redirect_uri`、`client_id`、`code_verifier`、以及撤销用的 `token`。没有 secret 字段。

### 图、表或文本图

**图 1：宏观四栋楼、两扇合同窗**

```text
  wta-admin (主机)
    AuthController / SysLoginService     AdminSsoIdentityService
    PasswordAuthStrategy                 implements SsoIdentityService
         │                                    ▲
         │  门厅发业务票                       │  填认人窗
         │                                    │
         ▼                                    │
  wta-api  org.namewta.sso.api                │
    SsoClientCatalog ──findByClientId──► SsoClientView
    SsoIdentityService ─┬─verifyPassword─► SsoAuthenticatedUser
                        ├─assertClientAccess
                        └─buildLoginUser──► LoginUser
         ▲                                    │
         │  Adapter 原样转交                   │
  wta-sso (layered)                           │
    WtaApiSso*Adapter implements *Port        │
    SsoSessionService / SsoAuthorizationService
         │
         │  不依赖 wta-system
         ▼
  wta-system (classic)
    SystemSsoClientCatalog implements SsoClientCatalog
    ISysClientService.queryByClientId → sys_client
```

- **alt：** 主机填认人窗，system 填目录窗，sso 模块只经 api 与 Port 伸手，不碰 Mapper。
- **caption：** 图 1——OBJ-59 的空间关系。两份实现不在 `wta-sso` 包内。
- **文字等价物：** 认人厅要看店卡就问 `SsoClientCatalog`，要核人就问 `SsoIdentityService`。真正查表的人分别坐在 system 房间和 admin 主机。认人厅自己的 Service 只认识 Port。浏览器既 import 不到这些 Java 接口，也不会把它们当成 HTTP 路径。
- **图的边界：** 不画 PKCE 公式、不画授权码表行、不画 sso-web 五个函数。不把 `UserService` 画进这张窗。不保证以后会增加 `client_secret` 校验。

**图 2：店卡抄了什么 / 没抄什么**

| `SysClientVo` / 表列 | 进不进 `SsoClientView` | 谁再用 |
| --- | --- | --- |
| `id` / `clientId` / `clientKey` / `status` | 进 | 禁票看 key；启停看 status |
| `ssoEnabled` / `ssoAuthMode` / `ssoClientKind` | 进 | `requireEnabledClient` 只硬检查 enabled |
| `ssoRedirectUriList` | 进，改名叫 `redirectUris` | 精确回调 |
| `ssoPkceRequired` / `ssoAutoConsent` / `ssoScope` | 进 | 今天 `authorize` 仍强制 S256 挑战，不按这三字段放行 plain |
| `deviceType` / `timeout` / `activeTimeout` | 进 | `SsoTokenExtras.bind` 写入 Sa-Token |
| `accessPath` | 进，home 会并入身份 API | Token extras |
| `ipWhitelist` / `userTypeId` | 进 | extras / 视图上有；登录域实际用认人再查的 VO |
| `ssoSecretHash` | **视图有槽，实现不填；VO 已被 fillView 清空** | **无人读** |
| `clientSecret` | 目录类型无此字段 | 门厅登录用，不是 SSO |

- **alt：** 目录是窄视图；哈希和登录明文都不出 system 房间。
- **caption：** 图 2——「只读 Client」读的是协议字段，不是信封。
- **文字等价物：** 店卡够认人厅判断「这家店开不开 SSO、回调是不是白名单里那一个、票该绑哪扇门」。保险柜密码留在 `sys_client.sso_secret_hash`，管理窗只回 `ssoSecretConfigured` / 一次性 `ssoSecretOnce`。换票用 PKCE，不拆信封。
- **图的边界：** `ssoPkceRequired` 抄进视图，不代表协议层会按 false 去接受 `plain`。磁盘上 `PkceS256.requireS256` 仍然硬闸。不要把「字段在店卡上」说成「协议读了它」。

**图 3：三张纸的因果（成功才往右）**

```text
[验密] verifyPassword(username, password)
   │  人没有 / 停用 / 锁住 / 密码不对 → 停
   │  临时柜：永久失败才 verify；成功立刻 consume（无准入夹心）
   v
 SsoAuthenticatedUser  →  Redis sso:session:*  （手环）
   │
[看店] findByClientId(client_id)
   │  null / 停用 / 未启用 SSO / 回调不精确 / 缺 PKCE / 缺 state → 停
   │  无手环 → needsLogin（不写 code）
   v
[准入] assertClientAccess(userId, clientId)
   │  登录域不合格 → 停；不看 clientKey 是否 sso
   v
 一次性授权码
   │
[组装] buildLoginUser(userId, clientId)
   │  clientKey=sso → 「禁止签发 SSO 中心 Client 业务票」
   │  人停用 / 登录域失败 → 停
   v
 LoginUser 底单
   │
[盖章] SsoTokenExtras.bind + LoginHelper.login
   │  clientKey 或 clientId 为 sso → 再停一次
   v
 目标业务 Client 的 Sa-Token（extras.clientId = 业务门牌，不是 "sso"）
```

- **alt：** 先手环，再店卡，再登录域，再底单，最后才是业务票；厅堂 Client 在组装和盖章两处被挡。
- **caption：** 图 3——OBJ-59 的时间关系。HTTP 路径只作为时刻标签。
- **文字等价物：** 登录认人厅只产生手环。看见业务店之后才问登录域。换票才填带权限的 `LoginUser` 并盖 Sa-Token。任何一步把厅堂 Client 当成目标业务，组装或 extras 会拒绝。
- **图的边界：** 不画 Cookie 的 SameSite、不画授权码 TTL 默认 5 分钟、不画并发 consume 的 DAO 版本号。那些是 L-055 / L-056 / L-057。本图只保证「哪一步碰到哪份 api 方法」。

## 正例、反例与边界

**正例 A：home 店卡带上身份 API。** `findByClientId("428a8310cd442757ae699df5d894f051")`。种子 `clientKey=home`，`access_path` 若是 `/home/**`，视图上的 `accessPath` 会并入 getInfo / getRouters / logout / profile。之后 extras 按这串规则放行门户身份枪，仍不放行 `/system/client/list`。

**正例 B：SSO 登录只得手环。** `verifyPassword("WTA", 正确密码)` 返回 `(userId, "WTA")`。`SsoSessionService` 把它交给 Redis。响应 JSON 没有 token。`buildLoginUser` 这条路径上不被调用。

**正例 C：发码前再问登录域。** 手环已在。`authorize` 先 `requireEnabledClient`（目录），再 `assertClientAccess`（认人）。超管种子另有用户端登录域关系（`NAMEWTA-SSO-DSL-001`），所以同一人可以进 home 门牌。换票时 `buildLoginUser` 会按**目标** Client 的 `userTypeId` 再裁权限，不是按厅堂 Client。

**正例 D：换票 extras 钉死业务门牌。** 授权码是 admin 门牌 `e5cd7e4891bf95d1d19206ce24a7b32e` 签发的。`exchange` 调 `buildLoginUser(userId, 该门牌)`。`SsoAuthorizationServiceTest.successfulExchangeUsesBusinessClientNotSsoAndRejectsNegatives` 断言发出去的 `clientId` 不是 `"sso"`。

**反例 1：** 「`wta-sso` 里有一份 `SsoClientCatalog` 实现。」没有。它只有 Port 和 Adapter。

**反例 2：** 「`ISysClientService` 就是目录合同。」它在 `org.namewta.system.service`，admin 主机可以注入，邻居不可以。目录合同是 `org.namewta.sso.api.SsoClientCatalog`。

**反例 3：** 「认人实现也在 system，跟目录放一起。」`AdminSsoIdentityService` 在 `wta-admin/.../web/sso/`。它要复用 `SysLoginService` 和临时密码柜，那些是主机接线。

**反例 4：** 「`findByClientId` 会带回可校验的 SSO 哈希。」热路径 VO 已清哈希；目录不 set；`wta-sso` 也不读 getter。

**反例 5：** 「换票 body 要带 `client_secret`。」`SsoTokenBo` 没有这个字段。协议是 authorization_code + PKCE S256。

**反例 6：** 「`verifyPassword` 等于 `/auth/login`。」没有验证码，没有 Client，没有 `LoginHelper.login`，返回值只有两字段。

**反例 7：** 「SSO 验密也是先准入再撕临时柜。」磁盘顺序是验密当场 consume；准入在 `assertClientAccess` / `buildLoginUser`。

**反例 8：** 「`assertClientAccess` 会拒绝 `clientKey=sso`。」它不会。禁票在 `buildLoginUser` 和 `SsoTokenExtras`。

**反例 9：** 「`SsoAuthenticatedUser` 就是 `LoginUser`。」手环两字段；底单带权限和 Client。会话测试禁止在登录路径调用 `buildLoginUser`。

**反例 10：** 「`UserService` 能替代 `SsoIdentityService`。」`UserService` 没有验密、没有 `assertClientAccess`。通知模块查收件人走那扇窗；认人厅走这一扇。

**反例 11：** 「目录 `userTypeId` 就是准入。」认人实现丢开店卡对象，自己再 `queryByClientId` 拿 `SysClientVo` 做 `requireLoginAccess`。

**反例 12：** 「只读所以验密不会动 Redis。」错次键和临时密码键都会动。只读的是**用户表和 Client 表**。

**反例 13：** 「Adapter 是业务规则。」两份 Adapter 每个方法一行委托。规则在 `SystemSsoClientCatalog` / `AdminSsoIdentityService` / `SsoAuthorizationService`。

**反例 14：** 「种子 `clientId=sso`。」种子厅堂行的 **key** 是 `sso`，**门牌**是 `0ae7adbebb81e87a9735ed0fba0a1135`，`sso_enabled=0`。`SsoTokenExtras` 对 `clientId` 字符串 `"sso"` 的拒绝是额外闸，不是种子门牌本身。

**边界：**

- 目录找不到：返回 `null`。认人 `requireClient` 找不到或停用：抛「客户端不可用」。两套语义，不要说成同一种。
- `ssoEnabled` 检查只在 `requireEnabledClient`。`assertClientAccess` / `buildLoginUser` 不看这个布尔。
- home 并入只发生在**目录抄写**时。管理窗 `queryById` 看到的仍是库里的原始 `access_path`。
- `fillView` 默认：`ssoEnabled` 空则 false；`ssoAuthMode` 空则按是否启用填 `both`/`local`；`ssoClientKind` 空则 `public`；PKCE / autoConsent 空则 true。这些默认发生在 VO，目录再抄。
- 主机可以碰 `SysUserMapper`；`wta-notify` / `wta-sso` 不可以。这是 L-002 的邻居禁令，本课用认人实现当正例。
- `namewta.sso.enabled` 默认 true。关掉之后是 Controller Bean 不进容器（L-055…L-057），不是目录接口消失。api 类型始终在 classpath。

## 变式与迁移

1. **变式 A：confidential 合作方。** L-018 会把哈希写入 `sys_client`。对本课：店卡仍然不带哈希，换票仍然不验 `client_secret`。不要为了「kind=confidential」去改 `SsoClientCatalog` 抄哈希，除非另开协议课并改 `SsoTokenBo`。今天的活样本是 public + PKCE。
2. **变式 B：临时密码走 SSO 门。** 人先 `POST /sso/login` 用临时纸条成功，柜已空。再到 home 门牌 `authorize`。若该人没有 home 登录域，手环在、发码失败，纸条不回来。运维口诀：SSO 验密成功 ≠ 目标业务一定放行。
3. **变式 C：把认人实现挪进 system。** 编译能过，但会拖着 `SysLoginService`（住在 admin）或复制一份错次/组票逻辑。2026-09-17 的落点是主机，不是「api 实现必须在对应 `wta-modules/*`」。
4. **变式 D：新写一个 layered 模块也要读 Client。** 学本课：POM 只加 `wta-api`，自己做 Port + Adapter，不要注入 `ISysClientService`。不要让第二个模块去依赖 `wta-sso` 的 Port——那是认人厅的门缝，不是公共合同。
5. **变式 E：rotate 之后换票。** 管理详情能看见新的 `ssoSecretConfigured`；`queryByClientId` 缓存可能仍是旧 VO。对本课无害，因为哈希不在店卡上。若有人把「读哈希」加进目录却忘记 `@CacheEvict`，才会变成真缺陷——那是改合同，不是本课现状。
6. **变式 F：只想要店卡、不要验密。** 授权码路上已经这样：有手环才 `assertClientAccess`，无手环只 `findByClientId` 后早退。不要在 `needsLogin` 分支调用 `verifyPassword`。
7. **变式 G：把 extras 的 `CLIENT_KEY` 写成 `clientKey` 字符串 `pc`。** 磁盘写的是 **门牌 `clientId`**。测试钉死 extras 等于 `e5cd7e4891bf95d1d19206ce24a7b32e`。口试不要把 Java 字段 `clientKey` 和 Sa-Token extra 的 `CLIENT_KEY` 当成同一个值。
8. **变式 H：门户身份 404。** 先看目录有没有给 home 并入四条身份规则，再看 Token extras 有没有带上解析后的 `accessPath`。不要先怪 `SsoIdentityService`——它不改路径白名单。
9. **以后若要 SLO。** `revoke` 只作废提交的那一张业务票，不删 SSO Redis 手环，也不踢其它 Client。那是 L-056 的格子；本课合同窗没有 `logoutAll`。
10. **以后若要图形验证码进 SSO。** 应加在会话 HTTP 或认人实现，而不是塞进 `SsoClientCatalog`。目录不认人。
11. **换模块模式。** 给 `wta-sso` 加「直接注入 `SysUserMapper`」会同时违反登记表、AGENTS.md 和本课 OBJ。要扩能力，先扩 `wta-api` 方法，再改唯一实现，再改 Adapter。
12. **迁移口诀：** 先数 `sso.api` 四份类型 → 再数谁 implements（system 填目录、admin 填认人、sso 只 Adapter）→ 再数店卡抄了哪些字段（哈希不在内）→ 再数三法谁在何时喊（登录验密、发码准入、换票组装）→ 最后数禁票两道闸。跳步会出现「sso 模块查了 sys_user」「换票带 client_secret」「手环就是 Admin-Token」。

## 常见误区

1. **「OBJ-59 包含 `SsoOAuthController`。」** 那是 OBJ-55 / OBJ-56。本课认 api 窗与填窗人。
2. **「OBJ-59 包含 sso-web。」** 那是 OBJ-58。
3. **「把 authorize / token / session 三行标 covered。」** 本课只给 `A:SsoClientCatalog` 与 `A:SsoIdentityService`。
4. **「`SsoClientCatalogPort` 是第二份 wta-api。」** 它是模块内 Port，包名 `org.namewta.sso.port`。
5. **「目录实现住在 `wta-sso`。」** 住在 `wta-system`。
6. **「认人实现住在 `wta-system`。」** 住在 `wta-admin`。
7. **「`ISysClientService` 在 api 包。」** 不在。L-002 / L-018 已钉过。
8. **「店卡带哈希所以 confidential 能验 secret。」** 不带；也不验。
9. **「`verifyPassword` 接收 `clientId`。」** 不接收。
10. **「只读等于零 Redis。」** 临时柜和错次键会动。
11. **「`LoginUser` 在会话 Cookie 里。」** Cookie 是 sessionId；Redis 是手环两字段。
12. **「禁票只靠种子 `sso_enabled=0`。」** 那是第一关。组装和 extras 还有硬编码 `sso` key。
13. **「home 并入发生在 `SysClientController.edit`。」** 发生在目录 `findByClientId` 抄写时。
14. **「Adapter 会补字段。」** 不会。哈希空着进去，空着出来。
15. **「浏览器能 import `org.namewta.sso.api`。」** 不能。那是 JVM 合同。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `backend/wta-api/src/main/java/org/namewta/sso/api/`。数文件必须是四份。圈 `SsoClientCatalog` 的唯一方法、`SsoIdentityService` 的三个方法、`SsoClientView` 上的 `ssoSecretHash` 字段、`SsoAuthenticatedUser` 的两个成员。
2. 打开 `wta-sso/pom.xml`。确认依赖列表有 `wta-api`、没有 `wta-system`。再在 `wta-sso/src/main/java` 搜 `SysUserMapper` 和 `ISysClientService`，确认生产代码为零命中。
3. 打开 `SystemSsoClientCatalog.findByClientId`。用笔把每一行 `view.set*` 勾掉。确认没有 `setSsoSecretHash`。再打开 `SsoClientFieldsSupport.fillView` 第一行 `setSsoSecretHash(null)`。
4. 打开 `AdminSsoIdentityService.verifyPassword`。按行标：`checkpw` → 可能 `verify` → **立刻** `consume` → `loginSucceeded` → `new SsoAuthenticatedUser`。对照 `PasswordAuthStrategy.authenticate` 里 `requireLoginAccess` 夹在 `verify` 与 `consume` 之间。
5. 打开 `buildLoginUser` 与 `SsoTokenExtras.bind`。圈两处「禁止签发 SSO 中心 Client 业务票」。再打开 `assertClientAccess`，确认它没有这句。
6. 打开 `WtaApiSsoClientCatalogAdapter` 与 `WtaApiSsoIdentityAdapter`。每个方法应是一行 `return` / 委托。再打开 `SsoAuthorizationService` 字段类型，确认是 `*Port` 不是 `SsoClientCatalog` / `SsoIdentityService`。
7. 打开 `SsoTokenBo`。圈存在的 JSON 名，确认没有 secret。打开 `ClientAccessPathsTest.catalogExposesExpandedHomeAccessPathForSsoTokenExtras`，圈种子 home 门牌。

## 总结、词汇表与下一步

- **宏观只读目录：** SSO 模块隔着 `wta-api` 的 `org.namewta.sso.api` 读用户与 Client。目录 1 法，认人 3 法。实现分别在 system 与 admin 主机。认人厅 POM 伸不到邻居 Mapper。
- **(a) `SsoClientCatalog`：** `findByClientId` 返回窄视图或 `null`。抄协议字段和 home 并入后的路径；不抄哈希、不抄登录明文。热路径 VO 进缓存前已被 `fillView` 清哈希。
- **(a) `SsoIdentityService`：** `verifyPassword` 出两字段手环，可撕临时柜，不带 Client；`assertClientAccess` 查登录域；`buildLoginUser` 按目标业务 Client 组装 `LoginUser` 并禁 `clientKey=sso`。三法都不 `LoginHelper.login`。
- **只读的精确含义：** 不写 `sys_user` / `sys_client`。不是零副作用。不是「类上没有 `ssoSecretHash` 字段」。不是「Port 可以代替 api 给别的模块用」。

词汇表：`SsoClientCatalog` / `SsoClientView` / `SsoIdentityService` / `SsoAuthenticatedUser` / `LoginUser` / `SsoClientCatalogPort` / `SsoIdentityPort` / `SystemSsoClientCatalog` / `AdminSsoIdentityService` / `queryByClientId` / `fillView` / `ClientAccessPaths` / `requireLoginAccess` / `SsoTokenExtras` / `clientKey=sso` / PKCE / `sys_client#30d` / layered / `wta-api`。

下一步：管理窗 bind / rotate 是 OBJ-18。授权码门口是 OBJ-55，换票与撤销是 OBJ-56，SSO Cookie 会话是 OBJ-57，sso-web 五件事是 OBJ-58。门厅密码策略与临时柜签发是 OBJ-12 / OBJ-15。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` 与 `wta-api` | 跨模块合同面；sso 分包四份类型 | `org.namewta.sso.api` | 2026-09-17 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-sso` layered；仅经 wta-api 读用户与 Client | 登记表 sso 行 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | admin/home 打开 SSO；厅堂 Client `client_key=sso`、`sso_enabled=0`、门牌 `0ae7adbebb81e87a9735ed0fba0a1135` | `NAMEWTA-SSO-DSL-001` | 2026-09-17 |
| S-012 | child `2026-09-14-wta-sso` course / L-001…L-003 | 认人厅 HTTP 与 Port 调用时刻；本课只对照不复述五层 | `children/2026-09-14-wta-sso/` | 2026-09-17 |
| S-015 | `backend/wta-api/src/main/java/org/namewta/sso/api` | 合同窗不在 system.api；`LoginUser` 跨包引用 | 四份 `.java`；`SsoIdentityService` import | 2026-09-17 |
| S-L059-01 | `SsoClientCatalog.java`；`SsoClientView.java` | 1 法；视图含空槽 `ssoSecretHash`；找不到返回 null | 接口 Javadoc 与字段 | 2026-09-17 |
| S-L059-02 | `SsoIdentityService.java`；`SsoAuthenticatedUser.java` | 三法签名；手环两字段；不签发业务 Token | 方法注释 | 2026-09-17 |
| S-L059-03 | `SystemSsoClientCatalog.java`；`SsoClientFieldsSupport.fillView`；`ClientAccessPaths.java` 与 `ClientAccessPathsTest` | 白名单抄字段；不 set 哈希；home 并入；`queryByClientId` 缓存 | `findByClientId`；fillView 首行；catalog 单测 | 2026-09-17 |
| S-L059-04 | `AdminSsoIdentityService.java`；`PasswordAuthStrategy.authenticate`；`ClientUserTypeAccessService` | 验密当场 consume；准入与组装分枪；禁 `clientKey=sso`；主机注入 Mapper | `verifyPassword` 44–54 行；`buildLoginUser` 70–80 行 | 2026-09-17 |
| S-L059-05 | `WtaApiSsoClientCatalogAdapter`；`WtaApiSsoIdentityAdapter`；`SsoClientCatalogPort`；`SsoIdentityPort`；`wta-sso/pom.xml`；`wta-sso/AGENTS.md` | 原样转交；Service 只见 Port；POM 无 wta-system | adapter 方法体；pom dependencies | 2026-09-17 |
| S-L059-06 | `SsoAuthorizationService`；`SsoSessionService`；`SsoSessionController`；`SsoOAuthController`；`SsoTokenBo` | 何时喊哪一法；换票无 secret；会话测试禁 `buildLoginUser` | `requireEnabledClient`；`login`/`authorize`/`exchange`；`SsoSessionServiceTest` | 2026-09-17 |
| S-L059-07 | `SsoTokenExtras.java`；`SsoTokenExtrasTest`；`SaTokenSsoBusinessTokenAdapter`；`SsoAuthorizationServiceTest` | extras 写业务门牌；再禁 sso；exchange 断言不是 `"sso"` | `bind`；`bindsBusinessClientAndRejectsSsoCenter`；`successfulExchangeUsesBusinessClientNotSsoAndRejectsNegatives` | 2026-09-17 |
| S-L059-08 | `SysClientServiceImpl.queryByClientId`；`CacheNames.SYS_CLIENT` | 30 天热路径；fillView 进缓存前清哈希；rotate 不驱逐对本课无害 | `@Cacheable`；`sys_client#30d` | 2026-09-17 |
