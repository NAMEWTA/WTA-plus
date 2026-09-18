---
lesson_id: L-084
objective_ids: [OBJ-84]
claimed_cells:
  - C:Redis 8
  - B:LoginHelper / StpUtil
  - D:fail-session-lost
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: write-path-on-disk
    minutes: 9
  - segment: fail-session-lost
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-002, S-L006-01, S-L013-01, S-L014-01, S-L023-01, S-L032-01, S-L084-01, S-L084-02, S-L084-03, S-L084-04, S-L084-05, S-L084-06, S-L084-07, S-L084-08, S-L084-09, S-L084-10, S-L084-11]
---

# Lesson 084：宏观保险柜——LoginHelper / StpUtil 怎样把会话写进 Redis 8，以及登录成功后柜子丢了算哪边

## 学完你能做什么

站在 `backend/wta-common/wta-common-satoken/`，你能**口述这块宏观保险柜**：人已经在门厅过了闸（L-011 / L-013），盖章口喊 `LoginHelper.login`，`StpUtil` 把通行证写进 **Redis 8**；下一次请求再从同一只柜子读。你还能把 OBJ-84 那句说完：

> **登录成功但会话丢失，算失败路径 `D:fail-session-lost`，不算 L-006 的 `D:fail-auth`。** 门厅拒绝（缺 Client / 验证码 / 密码策略）是「根本没开柜」。这里是「柜已经开过、口袋里有 `access_token`，再去读 Redis / Sa-Token 时柜子空了或读挂了」。

口试名单就是本课认的三格，符号以**磁盘和矩阵原文**为准：

1. **`C:Redis 8`**：基础设施容器。工作树镜像是 `redis:8.6.3`，容器名 `namewta-redis`。NAMEWTA 把会话、锁、限流都塞进这只柜子。本课把柜子认出来，并深挖**会话抽屉**；锁和限流只点名同住，不把 L-032 / `@RateLimiter` / Lock4j 的方法格再标一遍 covered。
2. **`B:LoginHelper / StpUtil`**：写柜的两只手。`LoginHelper.login` 先 `StpUtil.login(loginId, extras)`，再 `StpUtil.getTokenSession().set("loginUser", loginUser)`。真正落 Redis 的是同模块 `PlusSaTokenDao`（`RedisUtils` + 5 秒 Caffeine）。`StpUtil` 是厂商入口，仓库里没有第二份同名包装类。
3. **`D:fail-session-lost`**：登录已经返回过 `access_token`，后续 Redis / Sa-Token **读失败或读空**。汇不是「从未登录」，也不是「验证码没过」。

本课**不宣称**你会拆五只 `*AuthStrategy`（L-013）、门厅六扇窗（L-011）、前端 `Admin-Token` 抽屉（L-014）、在线用户踢人窗（L-023）、OpenAPI 机器会话（L-032）、SSO 手环 `sso:session:`（L-057）。那些会**借用同一只 Redis**，但格子不是今天的。

2026-09-17 工作树先钉死**谁写、谁不写**：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `SysLoginService.login` 写会话 | **没有这个方法。** 值班员不调用 `LoginHelper.login` / `StpUtil.login`（L-013） |
| `AuthController.login` 自己开柜 | **自己不写。** `@SaIgnore` 的门厅只把卷子交给策略 |
| JWT 简单模式 = 无 Redis | **不是。** `SaTokenConfig` 注入 `StpLogicJwtForSimple`：纸条是 JWT，**档案袋仍进 Redis** |
| `online_tokens:` 就是 token-session | **贴纸，不是通行证。** 监听器后贴；撕掉名牌 ≠ 没登录 |
| SSO Cookie `Sso-Token` / `sso:session:` | **另一只手环**（L-057）。业务票仍走今天这只柜 |
| OpenAPI 机器会话走 `LoginHelper.login` | **不走。** L-032 用 `StpLogic.createLoginSession`，且 `isWriteHeader=false` |
| Redis 挂了会变成漂亮的 401 | **常常不是。** `RedisExceptionHandler` 只接 Lock4j；连接异常落到全局「未知异常」 |

## 先把宏观地图放在桌上

L-006 画过走廊：`POST /auth/login` → Redis 会话 → `GET /system/menu/getRouters`。它把「登录成功但 Redis 读失败」明确留给本课。L-013 把盖章口指到 `LoginHelper.login`。今天走进**工具间和柜子本身**。

```text
浏览器口袋
  localStorage['Admin-Token'] / ['Home-Token']     ← L-014；本课不认格子
        │  头 Authorization: Bearer <jwt>
        │  头 clientid
        v
SaInterceptor（wta-common-security）
  StpUtil.checkLogin()
  LoginHelper.getLoginUser()
  JWT extra 里的 clientid 必须对上
        │
        ├─ 读：PlusSaTokenDao.get / getObject
        │      Caffeine 5s → RedisUtils.getCacheObject
        │      柜子在 redis:8.6.3 / namewta-redis
        │
        v
业务窗（例：SysMenuController.getRouters）
  再 LoginHelper.getLoginUser()，要 LoginUser.clientPk

写点（登录成功，L-013 喊，本课认手）：
  *AuthStrategy / SaTokenSsoBusinessTokenAdapter
        │
        v
  LoginHelper.login(loginUser, SaLoginParameter)
        │  1. fillRequestContext（IP / UA / 设备）
        │  2. StpUtil.login(userType:userId, extras)
        │  3. StpUtil.getTokenSession().set("loginUser", loginUser)
        v
  PlusSaTokenDao.writeValue → RedisUtils.setCacheObject
  UserActionListener.doLogin → online_tokens: 贴纸
```

**类比：** 夜店门口盖完章，给你一张手腕纸条（JWT `access_token`），同时在保险柜里放档案袋（token-session 里的 `LoginUser`，带着菜单权）。第二天你只出示纸条。保安先看纸条还算不算数，再开柜取档案。档案在，才能带你上楼。

**类比失效处：**

1. 纸条**不是** Redis 里的值本身。简单模式把 loginId 和 extra 编进 JWT；档案袋太大，只住 Redis。
2. 柜里还有别人的抽屉：图形验证码、`pwd_err_cnt:`、锁、限流、SSO 手环。不要说「Redis 里的东西都是登录会话」。
3. 访客簿 `sys_login_info` 在 MySQL，是审计，不是柜。L-006 已经钉过：日志有行 ≠ 已登录。
4. 前端抽屉和后端柜子是两件事。口袋里还有票，柜子可以已经空了——这正是本课失败路径。

## 核心概念与机制

### 直觉讲解

把「登录成功」拆成三张收据，不要揉成一句「已经登录」：

| 收据 | 在哪 | 谁写 | 丢了像什么 |
| --- | --- | --- | --- |
| 口袋纸条 | 浏览器 `access_token` | 策略返回 `LoginVo`，前端 `setToken` | 人以为自己还在店里 |
| 柜里档案袋 | Redis token-session 键 `"loginUser"` | `LoginHelper.login` 第三步 | `getRouters` 说缺少客户端上下文；权限表变空 |
| 柜里名牌 | Redis `online_tokens:` + token | `UserLoginSuccessListener` | 在线用户名单少一行；通行证仍可能在 |

OBJ-84 要你判断的「哪边」：

- **`D:fail-auth`（L-006）：** 三张收据都没有。`LoginHelper.login` 没被调用。
- **`D:fail-session-lost`（本课）：** 至少口袋纸条发出过（登录 HTTP 已经把 `access_token` 给出去了），再去开柜时档案袋没了、或 Redis 根本打不开。
- **写柜当下 Redis 就挂：** 登录 HTTP 自己炸，前端通常拿不到票。那是基础设施拒绝登录，**还没发出成功收据**，不要硬塞进会话丢失。口试可以说「写失败不是丢失；丢失发生在成功之后的读」。

### 精确定义与 English term

| 中文 | English | 磁盘上的一句话 |
| --- | --- | --- |
| 登录助手 | `LoginHelper` | `org.namewta.common.satoken.utils.LoginHelper`，私有构造，全静态 |
| 厂商门面 | `StpUtil` | Sa-Token 1.45.0 静态入口；`login` / `logout` / `checkLogin` / `getTokenSession` / `getExtra` |
| 登录标识 | login id | `LoginUser.getLoginId()` = `userType + ":" + userId`；缺一抛 `IllegalArgumentException` |
| 令牌会话 | token-session | 按 token 分配的 `SaSession`；本仓把整份 `LoginUser` 放在键 `loginUser` |
| 账号会话 | account-session | 按 loginId 分配；踢人、列设备会用到；本课不把它说成 `LoginUser` 的家 |
| JWT 简单模式 | JWT simple | `new StpLogicJwtForSimple()`：`createTokenValue` 产出 JWT；`getExtra` **不查 Redis**；`isSupportShareToken()` **恒 false** |
| 持久层 | `SaTokenDao` | 本仓实现 `PlusSaTokenDao implements SaTokenDaoBySessionFollowObject`，会话方法跟随 object 方法 |
| 一级影子 | L1 Caffeine | 写入后 5 秒过期，最多 1000 条；命名空间 `"sa-token"` |
| 二级柜子 | L2 Redis | `RedisUtils` → Redisson；key 前缀配置是 `redisson.keyPrefix: WTA` |
| 会话丢失 | session lost | 登录已成功发过票，再读 Sa-Token / Redis 失败或读空 |
| 认证拒绝 | auth rejected | 写柜之前就失败（L-006） |
| 踢下线 | kickout | `StpUtil.kickoutByTokenValue`；异常类型 `NotLoginException.KICK_OUT` |
| 顶下线 | replaced | `BE_REPLACED`；本仓 `is-concurrent: true`，新登录**默认不挤**旧登录 |
| Redis 8 容器 | Redis 8 container | `release-artifacts/docker/docker-compose-infrastructure.yml` 服务 `redis` |

`LoginHelper` 常量（口试能对上 extra / session 键）：

| 常量 | 值 | 住哪 |
| --- | --- | --- |
| `LOGIN_USER_KEY` | `"loginUser"` | **token-session**（Redis 对象） |
| `USER_KEY` / `USER_NAME_KEY` | `"userId"` / `"userName"` | JWT extra |
| `DEPT_KEY` / `DEPT_NAME_KEY` / `DEPT_CATEGORY_KEY` | `"deptId"` / `"deptName"` / `"deptCategory"` | JWT extra |
| `USER_TYPE_KEY` | `"userType"` | JWT extra |
| `CLIENT_KEY` | `"clientid"` | JWT extra（策略 `buildLoginParameter` 先放） |
| `CLIENT_PK_KEY` | `"clientPk"` | JWT extra |
| `CLIENT_ACCESS_PATH_KEY` / `CLIENT_IP_WHITELIST_KEY` | `"clientAccessPath"` / `"clientIpWhitelist"` | JWT extra |

**两层身份不要对调：** extra 够拦截器核 Client、够 `getUserId()`；菜单权 / 角色权 / 岗位只在 `LoginUser` 档案袋里。`SaPermissionImpl` **只读当前 token-session 的快照**，禁止按 userId 回库查全局权限。档案袋丢了，权限列表就是空的。

### 机制/因果链

#### 1. 工具间在磁盘上只有五份业务 Java

路径：`backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/`。

| 文件 | 角色 |
| --- | --- |
| `config/SaTokenConfig.java` | `@AutoConfiguration` + `@PropertySource classpath:common-satoken.yml` |
| `utils/LoginHelper.java` | 写 / 读登录态的项目入口 |
| `core/dao/PlusSaTokenDao.java` | Sa-Token → Redis |
| `core/service/SaPermissionImpl.java` | `StpInterface`：权限 / 角色来自当前 `LoginUser` |
| `handler/SaTokenExceptionHandler.java` | `NotLoginException` / `NotPermissionException` / `NotRoleException` |

`pom.xml`：依赖 `wta-common-core`、`wta-common-redis`、`wta-api`、`sa-token-spring-boot4-starter`、`sa-token-jwt`、`caffeine`。版本钉在后端根 POM `satoken.version=1.45.0`。没有本模块单测目录。

`SaTokenConfig` 四颗 Bean（口试按这个数）：

1. `StpLogic getStpLogicJwt()` → `new StpLogicJwtForSimple()`。注释写「简单模式」。
2. `StpInterface stpInterface()` → `new SaPermissionImpl()`。
3. `SaTokenDao saTokenDao(ClusterCacheInvalidationCoordinator)` → `new PlusSaTokenDao(...)`。
4. `SaTokenExceptionHandler saTokenExceptionHandler()`。

`common-satoken.yml` 自称「内置配置不允许修改」，要改去 Nacos 覆盖。本 Goal 把 Nacos 标了 deferred，口试以这份内置为准：

- `dynamic-active-timeout: true`
- `is-read-body: true`、`is-read-header: true`
- **`is-read-cookie: false`**（关 Cookie 鉴权）
- `token-prefix: "Bearer"`

`application.yml` 再叠：`token-name: Authorization`、`is-concurrent: true`、`is-share: false`、`jwt-secret-key: abcdefghijklmnopqrstuvwxyz`。JWT 简单模式里 `isSupportShareToken()` 本来就恒 false，yaml 的 `is-share` 再写 true 也不共用旧票。

#### 2. `LoginHelper.login`：两步写，不是一步

```text
fillRequestContext
  有 HttpServletRequest 才补 ipaddr / loginLocation / browser / os / deviceType
        │
        v
StpUtil.login(loginUser.getLoginId(), model.setExtra(...))
  extra 叠上 userId / userName / dept* / userType
  （clientid / clientPk / accessPath / ipWhitelist 策略已经放进 model）
        │
        v
StpUtil.getTokenSession().set("loginUser", loginUser)
```

副作用：不纯。每次登录因简单模式不共用 token，会发**新** JWT。`is-concurrent: true` 时旧票默认继续活着，直到各自 TTL 或被踢。

失败：`getLoginId()` 在 `userType` 或 `userId` 为空时抛。L-013 的小程序策略若没填这两样，开柜会在这里炸——那是写失败，不是丢失。

`login` **没有**自己 try/catch Redis。DAO 抛错就让登录 HTTP 失败。

读路径的不对称：

| 方法 | 失败时 |
| --- | --- |
| `getLoginUser()` / `getLoginUser(token)` | **只** catch `NotLoginException` → `null`。Redis 连接异常冒泡 |
| `getUserId` 等经私有 `getExtra` | catch **任何** `Exception` → `null` |
| `getUserType()` | 直接 `StpUtil.getExtra`，**没有**那层 catch |
| `isLogin()` | `StpUtil.isLogin()`，不包一层 |

`getLoginUser()` 走 `StpUtil.getTokenSession()`（要当前票的会话对象）。会话对象在、里面没有 `"loginUser"` → 返回 `null`，**不**变成 `NotLoginException`。这是「纸条还算数、档案袋空」的口子。

#### 3. `PlusSaTokenDao`：Redis 才是家，Caffeine 是 5 秒影子

写：`writeValue`。

- `timeout == 0` 或 `timeout <= NOT_VALUE_EXPIRE` → **直接 return，不写 Redis。** Client 若把 timeout 配成 0，DAO 会拒绝持久化。
- `timeout == NEVER_EXPIRE`（-1）→ `RedisUtils.setCacheObject(key, value)` 无 TTL。
- 否则 → `setCacheObject(key, value, Duration.ofSeconds(timeout))`。
- 写完立刻 `invalidationCoordinator.invalidate("sa-token", key)`，清本机和其他节点的 L1。

读：`caffeine.get(key, RedisUtils::getCacheObject)`。Redis 没有键时 loader 得到 `null`，Caffeine **会把这次空结果也缓存最多 5 秒**。刚删的键在本节点可能仍「像在」或「像不在」，取决于删的时候有没有走 `invalidate`。

改：`update` / `updateObject` **先** `RedisUtils.hasKey`；没有键就什么都不做。丢了的档案袋不能靠 update 补回来。

删：`RedisUtils.deleteObject` + `invalidate`。

TTL：Redis 给毫秒，Sa-Token 要秒；`toTimeoutSeconds` 做 `/1000 + 1` 的补偿，注释写明是为了秒级精度。

集群失效：`ClusterCacheInvalidationCoordinator` 用 Redis pub/sub 通知指纹。没有订阅者或 2 秒收不齐 ACK → `ClusterCacheInvalidationException`。这发生在 **writeValue 已经 set 过 Redis 之后**。登录 HTTP 可能因此失败，但柜里已经留下键——那是「写路径的孤儿键」，不是本课 `fail-session-lost` 的标准故事。口试点到「失效广播失败会抛，不要把它说成读丢失」即可。

本课**没有**在运行中的 Redis 里 SCAN 过键名。口试不要假装背过 `WTA:Authorization:login:token-session:<jwt>`。权威句子是：DAO 把 Sa-Token 给的 key 交给 `RedisUtils`；Redisson `KeyPrefixHandler` 在非空时加 `WTA:`。

JWT 1.45.0 的一个硬边界（对照 `StpLogicJwtForSimple.createTokenValue`）：它调用 `SaJwtUtil.createToken(loginType, loginId, extraData, jwtSecretKey())`，**没有**把 `timeout` 传进去。口袋纸条的「看起来还是 JWT」**不能**代替 Redis TTL。有效期的权威在柜。

#### 4. 读请求怎样发现柜子丢了

`SecurityConfig.addInterceptors` 里的 `SaInterceptor`（排除 `security.excludes`，`/auth/**` 因 `@SaIgnore` 不查已有会话）：

1. `StpUtil.checkLogin()`。票无效 / 映射没了 / 过期 / 被踢 → `NotLoginException` → `SaTokenExceptionHandler`：
   - `TOKEN_TIMEOUT` / `TOKEN_FREEZE` → 「登录已过期，请重新登录」
   - `BE_REPLACED` → 「当前账号已在其他设备登录，您已被强制下线」
   - `KICK_OUT` → 「账号已被管理员强制下线」
   - 默认 → 「登录状态异常，请重新登录」
   - JSON `code` 走 Hutool `HTTP_UNAUTHORIZED`（401）
2. `LoginHelper.getLoginUser()`。档案袋空 → `null`，**拦截器不因此 401**。
3. 非 OpenAPI 机器请求：`StpUtil.getExtra("clientid").toString()` 必须等于头或参数里的 `clientid`。对不上 → 手造 `NotLoginException` type `"-100"`，文案「客户端ID与Token不匹配」，落到默认 401 句。extra 为 null 时 `.toString()` 会 NPE，变成全局未知异常，不是 401。
4. 再核 JWT extra 里的访问路径 / IP 白名单；失败是 `NotPermissionException` → 403「没有访问权限，请联系管理员授权」。

过了拦截器之后：

- `GET /system/menu/getRouters`：`LoginUser == null` 或 `clientPk == null` → `R.fail("当前登录缺少客户端上下文")`（默认 JSON `code=500`）。**这不是 401。** 这是档案袋丢了最像的业务回执。
- `@SaCheckPermission`：`SaPermissionImpl` 拿不到匹配的 `LoginUser` 就返回空列表 → 403，文案像「没授权」。误诊点。

Redis **进程挂了**（连接超时 / 拒绝）：`getCacheObject` 抛运行时异常。`RedisExceptionHandler` **只**处理 `LockFailureException`（503「业务处理中，请稍后再试...」）。会话读取挂掉 → `GlobalExceptionHandler` 的 `RuntimeException` 席：`R.fail("发生未知异常，请联系管理员 [错误编号: 8 位数字]")`，JSON `code=500`。**不要口述成 401。**

#### 5. Redis 8 这只容器长什么样

矩阵 (c) 这一格是容器，不是某一个 Java 类。

工作树 `release-artifacts/docker/docker-compose-infrastructure.yml`：

| 项 | 值 |
| --- | --- |
| 镜像 | `redis:8.6.3` |
| 容器名 | `namewta-redis` |
| 端口 | 宿主机 `${NAMEWTA_BIND_HOST:-127.0.0.1}:46379` → 容器 `6379` |
| 持久化 | `redis-server --appendonly yes --requirepass $REDIS_PASSWORD` |
| 健康检查 | `redis-cli -a $REDIS_PASSWORD ping` 要看到 `PONG` |
| 网络 | `namewta` |

后端组装 `docker-compose-backend.yml`：`SPRING_DATA_REDIS_HOST: redis`，端口 6379，密码同一只 `REDIS_PASSWORD`。

这**不是** `application-dev.yml` 那份本机 overlay：`localhost:6379`、密码 `wta123`。口试先问「你连的是容器还是本机」，再谈会话丢了。

同一只容器里的邻居抽屉（点名，不盖它们的函数格）：

| 抽屉 | 前缀 / 入口 | 哪一课 |
| --- | --- | --- |
| 图形 / 短信 / 邮箱码 | `CAPTCHA_CODE_KEY` 等 | L-012 |
| 密码错次 | `pwd_err_cnt:` | L-013 / L-023 unlock |
| 在线名牌 | `online_tokens:` | L-013 写、L-023 读/踢 |
| SSO 手环 | `sso:session:` | L-057 |
| OpenAPI nonce / 限流 | Redisson SET NX / 原子桶 | L-032 |
| `@RateLimiter` / `@RepeatSubmit` / Lock4j | `wta-common-redis` | 本课只认「住在这只柜」 |

AOF 开着，**正常重启不应当**把会话抽屉清空。丢失更像：`FLUSHALL`、卷被删、密码错连到空实例、TTL 到了、踢人、或进程挂掉读不出来。

#### 6. 谁还有第二把钥匙开同一只柜

本课格子是 LoginHelper / StpUtil，但写手不止密码策略：

| 写手 | 调用 | 本课？ |
| --- | --- | --- |
| 五只 `*AuthStrategy` | `LoginHelper.login` 然后 `StpUtil.getTokenValue()` 填 `LoginVo` | 写点；策略细节 L-013 |
| `SaTokenSsoBusinessTokenAdapter.issue` | `LoginHelper.login` + `StpUtil.getTokenValue()` | 同一只柜的另一把钥匙；发码/换票 L-056 |
| `SaTokenSsoBusinessTokenAdapter.revoke` | `StpUtil.logoutByTokenValue` | 撕柜 |
| `SysLoginService.logout` | finally `StpUtil.logout()` | L-013 值班员；本课认它是**删**不是写 |
| `SysUserOnlineController.forceLogout` | `StpUtil.kickoutByTokenValue` | L-023 |
| OpenAPI 机器会话 | `stpLogic.createLoginSession`，**不**经 `LoginHelper.login` | L-032 |

`LoginVo` JSON 蛇形：`access_token` / `expire_in` / `client_id`。`expire_in` 来自 `StpUtil.getTokenTimeout()`。前端只在解析到非空 `access_token` 后 `setToken`（L-014）。

### 图、表或文本图

**图题 / caption：** 宏观保险柜：纸条、档案袋、名牌不是同一格。alt：JWT 在浏览器；LoginUser 在 Redis token-session；online_tokens 是后贴的名牌。

```text
口袋（浏览器）
  access_token = JWT          extra: userId, clientid, userType...
        │
        │  丢失：人还拿着票
        v
Redis 8  namewta-redis
  ┌─ token 映射 / 活性 ────────── checkLogin 看这里
  ├─ token-session["loginUser"] ─ 档案袋 LoginUser（菜单权）
  ├─ account-session ──────────── 设备列表 / 踢人
  ├─ online_tokens:<token> ────── 名牌（监听器）
  └─ 邻居：验证码 / 错次 / 锁 / 限流 / sso:session:
```

**文字等价物：** 登录成功会发出口袋纸条，并在 Redis 里放档案袋。名牌是后贴的。读请求先核纸条对应的柜映射，再取档案袋。只丢名牌，监控室少一行；丢档案袋，路由窗说缺少客户端上下文；映射没了，拦截器 401。

**图题 / caption：** 失败路径算哪边。alt：左边 fail-auth 从未开柜；右边 fail-session-lost 开过柜再读失败。

```text
D:fail-auth（L-006）              D:fail-session-lost（本课）
缺 Client / 验证码 / 菜谱              已经 LoginHelper.login 过
不调用 LoginHelper.login              口袋里有 access_token
Redis 无本次 token-session            再读：空键 / 连接挂 / 被踢
前端不 setToken                       前端抽屉往往还在
汇：没登录                             汇：登录过，会话没了
```

**文字等价物：** 判断口诀只有一句：`LoginHelper.login` 有没有成功返回过票。没有，算认证拒绝；有了再读失败，算会话丢失。不要用「页面白了」或「JSON 失败了」当分类。

**图题 / caption：** `LoginHelper` / `StpUtil` 方法性状。alt：写、读、删、纯/不纯、失败。

| 符号 | 纯？ | 副作用 | 失败 |
| --- | --- | --- | --- |
| `LoginHelper.login` | 不纯 | `StpUtil.login` + token-session 放 `LoginUser`；触发 `doLogin` | Redis 写抛则登录 HTTP 失败；timeout≤0 时 DAO **跳过写** |
| `StpUtil.login` | 不纯 | 发 JWT、写柜映射 / 会话 | 基础设施异常冒泡 |
| `StpUtil.getTokenSession().set` | 不纯 | 档案袋 | 同上 |
| `LoginHelper.getLoginUser` | 不纯读 | 可能触达 Redis / 5s L1 | `NotLoginException`→null；连接异常冒泡；无 `loginUser` 键→null |
| `LoginHelper.getUserId` 等 | 读 extra | JWT 简单模式**可以不打 Redis** | 私有 `getExtra` 吞异常→null |
| `LoginHelper.isLogin` | 读 | `StpUtil.isLogin` | false，不抛 |
| `StpUtil.checkLogin` | 读+断言 | 无写 | `NotLoginException` → 401 句 |
| `StpUtil.logout` / `logoutByTokenValue` | 不纯 | 删映射与 token-session；监听器删名牌 | 未登录吞掉（值班员 finally） |
| `StpUtil.kickoutByTokenValue` | 不纯 | 踢；`KICK_OUT` | 已无效时 L-023 吞 `NotLoginException` 仍 `R.ok()` |
| `SaPermissionImpl.getPermissionList` | 读快照 | 不回库 | 无当前 `LoginUser` 或 loginId 对不上 → 空列表 |

**文字等价物：** 写只有 `login` 那两步（外加监听器名牌）。读分 JWT extra 和 Redis 档案袋。权限实现故意不回库，所以档案袋一丢，权限就变空，看起来像没授权。

**图题 / caption：** 同一请求上三种丢失的回执。alt：401 未登录异常；业务 500 缺客户端上下文；500 未知异常。

```text
Redis 进程挂          → 未知异常 + 错误编号          code 500
checkLogin 失败       → 过期 / 被踢 / 状态异常      code 401
checkLogin 过、档案空 → getRouters 缺客户端上下文   code 500
                      → @SaCheckPermission 空权    code 403
```

**文字等价物：** 会话丢失不是一种 HTTP。先问 Redis 还活着吗，再问 checkLogin 过了没有，再问 `LoginUser` 在不在。三种回执都可能，不要背成「一律 401」。

**图的边界：** 不画 Nacos 覆盖怎么配。不画 Redisson 集群拓扑（yaml 是单节点 `singleServerConfig`）。不保证 JWT payload 里有与 Client timeout 一致的 `exp`（1.45.0 simple 的 `createTokenValue` 没传 timeout）。不把 `WTA:` 前缀的完整键名当成已 SCAN 证据。

## 正例、反例与边界

**正例 1 — 指工具间。** 打开 `wta-common-satoken/src/main/java`，数到五份业务 Java。指出 `LoginHelper.login` 的两步和 `PlusSaTokenDao`。OBJ-84 的「Sa-Token 会话写入」这一句就有落点。

**正例 2 — 指 Redis 8 容器。** 打开 `docker-compose-infrastructure.yml` 服务 `redis`：镜像 `redis:8.6.3`、容器 `namewta-redis`、AOF、密码、宿主机 `46379`。再打开 `docker-compose-backend.yml` 的 `SPRING_DATA_REDIS_HOST: redis`。

**正例 3 — 一次成功密码登录。** 策略 `authenticate` 过了才 `LoginHelper.login`。返回 `LoginVo.accessToken = StpUtil.getTokenValue()`。前端解析到 `access_token` 才 `setToken`。柜里此时应有档案袋；随后监听器再贴 `online_tokens:`。

**正例 4 — extra 与档案袋分工。** 拦截器核 `clientid` 走 `StpUtil.getExtra`（JWT）。`getRouters` 要 `LoginUser.clientPk` 走档案袋。只核 extra 过关，不等于菜单树能拼出来。

**正例 5 — 踢人。** 管理员 `DELETE /monitor/online/{tokenId}` → `StpUtil.kickoutByTokenValue`。被踢者下一枪 `checkLogin` 走 `KICK_OUT` → 「账号已被管理员强制下线」。这是会话丢失的一种**有人动手**的子类，格子仍是 `fail-session-lost`，函数格仍归 L-023。

**正例 6 — 退出。** `SysLoginService.logout` 的 finally 一定 `StpUtil.logout`。名牌监听器 `doLogout` 删 `online_tokens:`。若管理端厅堂在 HTTP 失败时没清 `Admin-Token`（L-014 对照），口袋还在、柜子已空：下一枪 401。分类：会话丢失，不是从未登录。

**正例 7 — Redis 挂在读路径。** 登录早已成功。之后 Redis 进程停。`getRouters` 打 DAO 读，连接异常，全局未知异常席。这是矩阵原文「登录成功但 Redis/Sa-Token 读失败」。

**正例 8 — 档案袋单独丢了。** `checkLogin` 仍过（映射还在），`getTokenSession` 里没有 `loginUser`。`getRouters` 回「当前登录缺少客户端上下文」。权限注解则 403。口试要能说出这两种回执都不是 fail-auth。

**正例 9 — DAO 拒绝 timeout 0。** `writeValue` 在 `timeout == 0` 时不写。Client 把超时配成 0，持久化被跳过。若上层仍把 JWT 塞进 `LoginVo`，下一枪就是标准丢失。本课没有集成测试把「timeout=0 一定返回了票」钉死；口试说「DAO 这一闸在磁盘上」，不要假装见过那次 HTTP。

**正例 10 — 权限实现不回库。** `SaPermissionImpl.resolvePermissionList`：当前 `LoginHelper.getLoginUser()` 为空，或 `loginUser.getLoginId()` 对不上参数 `loginId`，返回空 `ArrayList`。档案袋丢失不会偷偷用 userId 把菜单权再查出来。

**反例 1 — 「白页就是没登录成功。」** 先看口袋有没有 `access_token`。有票再白，优先本课，不是 L-006。

**反例 2 — 「`SysLoginService` 写 Redis 会话。」** 它写错次、发审计、退出时 `StpUtil.logout`。写会话的是 `LoginHelper`。

**反例 3 — 「简单模式不用 Redis。」** 那是 Stateless。本仓注入 Simple，DAO 是 Redis。

**反例 4 — 「JWT 还没过期所以会话还在。」** 1.45.0 `createTokenValue` 没把 timeout 编进 JWT。权威是 Redis TTL 和 checkLogin。

**反例 5 — 「`online_tokens:` 没了就是没登录。」** 那是名牌。`getRouters` 不读这把键。

**反例 6 — 「SSO 手环丢了等于业务会话丢了。」** `sso:session:` 是认人厅。业务票在今天这只柜。手环丢了不会自动 `StpUtil.logout`（L-057）。

**反例 7 — 「会话丢失一律 401。」** 见上图三种回执。

**反例 8 — 「Redis 挂了走 `RedisExceptionHandler` 变 503。」** 那一席只接锁失败。

**反例 9 — 「`getUserId()` 空了就证明没登录。」** extra 读失败被吞成 null；`isLogin()` / `checkLogin()` / 档案袋是三条不同的尺。

**反例 10 — 「本课把锁和限流的函数格也 covered。」** 容器格认 Redis 8；OpenAPI 限流仍是 L-032，错次仍是 L-013。

**反例 11 — 「OpenAPI 机器票也是 `LoginHelper.login`。」** 磁盘上不是。

**反例 12 — 「Cookie 里的 Authorization 就是会话。」** `is-read-cookie: false`。票走 header / body，前缀 Bearer。

**反例 13 — 「Caffeine 是第二只 Redis。」** 进程内 5 秒影子，最多 1000 条。重启 JVM 影子全没；Redis 才跨进程。

**反例 14 — 「update 能把丢了的会话写回来。」** `hasKey` 为假则 no-op。

**边界 1 — 写失败 ≠ 丢失。** 登录当下 Redis 拒绝，通常没有成功收据。丢失从「已经把票交给客户端」算起。

**边界 2 — 孤儿键 ≠ 丢失。** 失效广播在 set 之后抛，柜里可能有键、客户端没票。方向相反。

**边界 3 — 5 秒影子。** 踢人走 `delete`+`invalidate`，影子该立刻没。只在 Redis 里 DEL、没走 DAO，本节点最多再信 5 秒。

**边界 4 — `is-concurrent: true`。** 新登录不默认顶旧登录。`BE_REPLACED` 那句中文在处理器里，但本仓默认走不到「后登录挤前登录」。不要把产品口述成单端互斥。

**边界 5 — 本机 yaml ≠ 容器。** `localhost:6379` / `wta123` 不是 `namewta-redis:6379` / `REDIS_PASSWORD`。连错空实例，所有票都会变成丢失。

**边界 6 — AOF 开着。** 正常重启不是丢失的默认故事。卷被删或 FLUSH 才是。

**边界 7 — `getLoginUser(token)` 空 token 直接 null。** 不打 Redis。

**边界 8 — 超级管理员。** `isSuperAdmin` 只比 `userId == 1L`（`SystemConstants.SUPER_ADMIN_USER_ID`）。档案袋丢了 `getUserId()` 也可能 null，超管判断会是 false。不要靠它当会话还在的证据。

## 变式与迁移

1. **和 L-006 对照。** 主路径仍是登录写柜再 `getRouters`。L-006 的失败是写柜前。本课的失败是写柜后读。两格不要互相盖章。
2. **和 L-013 对照。** 盖章口、值班员、办证窗。本课接过「谁喊 `LoginHelper.login`」之后的柜子。贴纸失败会不会回滚 token，L-013 已经拒绝提前宣布；本课也不发明回滚——监听器在 `doLogin` 之后，磁盘没有「贴纸失败则 `StpUtil.logout`」。
3. **和 L-014 对照。** 前端抽屉 `Admin-Token` / `Home-Token`。口袋和柜子可以不同步。排查白页先看抽屉有没有票，再问柜里有没有档案袋。
4. **和 L-023 对照。** 在线窗读名牌、踢人走 `kickoutByTokenValue`。踢人是本课失败路径的主动子类。`GET /monitor/cache` 看 INFO，不扫会话键。
5. **和 L-032 对照。** 机器会话也住 Sa-Token + Redis，但是 `createLoginSession`，失败关闭是 503。浏览器票走大厅拦截器。两扇门不准叠在一次请求上。
6. **和 L-057 对照。** SSO 手环 Cookie + `sso:session:`。换业务票才进今天这只柜（`SaTokenSsoBusinessTokenAdapter`）。
7. **和 L-012 对照。** 验证码 Redis 键先删再比。那是考卷抽屉，不是档案袋。验证码失败从不 `LoginHelper.login`。
8. **以后若要把 JWT 改成 Stateless。** 会拆掉踢人、档案袋、在线名单。本仓权限实现依赖档案袋，不能只换 `StpLogic` Bean。
9. **以后若要单端登录。** 改 `is-concurrent: false` 才会走到顶下线。先改 yaml，再改口试故事。
10. **以后若要会话进 MySQL。** 违反本仓「会话在 Redis 8」的容器合同。不要把 `sys_login_info` 当会话表。
11. **多节点。** L1 靠 `"sa-token"` 命名空间的失效广播。广播失败会让写路径抛，不保证「最终一致还继续登录」。
12. **迁移口诀：** 先数 Redis 8 容器（镜像、容器名、AOF、46379）→ 再数 `LoginHelper.login` 两步 → 再数 DAO 跳过 timeout 0、5 秒影子、WTA 前缀 → 再数读路径三种回执 → 最后判「有没有成功发过票」。跳步会出现「把验证码失败说成会话丢失」「把 JWT 说成无状态」「把 403 空权当成没配菜单」。

## 常见误区

1. **「OBJ-84 只讲 Redis 配置。」** 原文要会话写入 **和** 失败路径「登录成功但会话丢失算哪边」。
2. **「算 fail-auth。」** 那格是没开柜。
3. **「`AuthController` 调用 `LoginHelper.login`。」** 策略才调用。
4. **「`SysLoginService.login`。」** 没有。
5. **「简单模式无 Redis。」** Simple ≠ Stateless。
6. **「JWT 有效就是已登录。」** 档案袋和 checkLogin 才是读尺。
7. **「`online_tokens:` = 会话。」** 名牌。
8. **「丢失一律 401。」** 还有缺上下文、未知异常、空权 403。
9. **「Redis 挂了走 503 锁席。」** 锁席不接连接异常。
10. **「Cookie 会话。」** `is-read-cookie: false`。
11. **「本课覆盖 OpenAPI nonce。」** 同柜不同格。
12. **「Caffeine 跨节点。」** 不跨；靠失效广播。
13. **「update 能复活。」** 不能。
14. **「`getUserType` 和 `getUserId` 一样吞异常。」** 前者不吞。
15. **「SSO 手环丢了业务票也没了。」** 不会自动联动。
16. **「超管不受会话丢失影响。」** `getUserId()` 空则超管判断 false。
17. **「键名我能默写完整。」** 本课没有 SCAN 证据。
18. **「Nacos 里才有真正配置。」** 内置 yml + application.yml 是工作树证据；Nacos 本 Goal deferred。
19. **「并发登录会互挤。」** 默认 `is-concurrent: true`。
20. **「把 `PlusSaTokenDao` 写成矩阵 (a) 公开 API。」** 本课 (b) 是 LoginHelper / StpUtil；DAO 是机制。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `LoginHelper.java`。圈 `login` 里 `StpUtil.login` 和 `getTokenSession().set(LOGIN_USER_KEY, loginUser)` 两步。圈 `getLoginUser` 只 catch `NotLoginException`。圈 `getUserType` 没有私有 `getExtra` 那层保护。
2. 打开 `PlusSaTokenDao.java`。圈 Caffeine `expireAfterWrite(5, SECONDS)`、`INVALIDATION_NAMESPACE = "sa-token"`、`writeValue` 对 timeout 0 / NEVER_EXPIRE 的分支、`update` 的 `hasKey` 闸。
3. 打开 `SaTokenConfig.java` 与 `common-satoken.yml`。圈 `StpLogicJwtForSimple`、`token-prefix: "Bearer"`、`is-read-cookie: false`。打开 `application.yml` 的 `sa-token` 段，圈 `token-name`、`is-concurrent`、`jwt-secret-key`。
4. 打开 `SaTokenExceptionHandler.java` 与 `SecurityConfig.java` 拦截器。圈 `checkLogin` → `getLoginUser` → `getExtra(CLIENT_KEY).toString()`。圈 401 四句中文。打开 `RedisExceptionHandler.java`，确认只有 `LockFailureException`。
5. 打开 `SysMenuController.getRouters`。圈 `loginUser == null || loginUser.getClientPk() == null` 的文案。打开 `SaPermissionImpl.java`，圈空列表分支。
6. 打开 `docker-compose-infrastructure.yml` 的 `redis:` 服务与 `docker-compose-backend.yml` 的 `SPRING_DATA_REDIS_HOST`。对照 `application-dev.yml` 的 `localhost:6379`。打开 `UserLoginSuccessListener`，圈 `online_tokens:` 是后贴。

## 总结、词汇表与下一步

- **宏观保险柜：** Redis 8（`redis:8.6.3` / `namewta-redis`）是容器。`LoginHelper` / `StpUtil` 是开柜的手。档案袋是 token-session 里的 `LoginUser`。口袋 JWT 不是柜子。
- **(c) Redis 8：** 会话、锁、限流同住。本课深挖会话抽屉；锁 / 限流 / 验证码 / SSO 手环只点名邻居。
- **(b) LoginHelper / StpUtil：** `login` 两步写；读分 extra 与档案袋；DAO 才是 Redis 适配器；简单模式仍落 Redis。
- **(d) fail-session-lost：** 已经发过 `access_token`，再读空或读挂。不算 fail-auth。回执可能是 401 / 403 / 缺上下文 / 未知异常，先看 Redis 活着没有。

词汇表：`LoginHelper` / `StpUtil` / `StpLogicJwtForSimple` / token-session / `loginUser` / extra / `PlusSaTokenDao` / Caffeine L1 / `RedisUtils` / `namewta-redis` / `fail-session-lost` / `fail-auth` / kickout / `online_tokens:` / `SaPermissionImpl` / `NotLoginException`。

下一步：错次和解锁仍是 L-013 / L-023。前端抽屉是 L-014。机器会话是 L-032。SSO 手环是 L-057。MySQL 基座是 L-085。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-002 | `AuthController.java` | 门厅 `@SaIgnore`；自己不写会话 | `backend/wta-admin/.../AuthController.java` | 2026-09-17 |
| S-L006-01 | L-006 | 主路径写 Redis；fail-auth 在写柜前；会话丢失留给本课 | `children/.../L-006-request-walkthrough.md` | 2026-09-17 |
| S-L013-01 | L-013 | 策略才 `LoginHelper.login`；值班员无 `login` 方法；贴纸在监听器 | `lessons/L-013-auth-strategy.md` | 2026-09-17 |
| S-L014-01 | L-014 | 前端 `access_token` → `Admin-Token` / `Home-Token` | `lessons/L-014-identity-access-frontend.md` | 2026-09-17 |
| S-L023-01 | L-023 | 在线窗读 `online_tokens:`；踢人 `kickoutByTokenValue` | `lessons/L-023-sys-monitor.md` | 2026-09-17 |
| S-L032-01 | L-032 | 机器会话不经 `LoginHelper.login`；Redis 挂 fail-closed | `lessons/L-032-openapi-common.md` | 2026-09-17 |
| S-L084-01 | `LoginHelper.java` | 两步写；读 catch 范围；常量键 | `wta-common-satoken/.../utils/LoginHelper.java` | 2026-09-17 |
| S-L084-02 | `PlusSaTokenDao.java` | RedisUtils + 5s Caffeine；timeout 0 跳过；hasKey 闸 | `.../core/dao/PlusSaTokenDao.java` | 2026-09-17 |
| S-L084-03 | `SaTokenConfig.java` + `common-satoken.yml` | JWT simple；Bearer；关 Cookie | 同模块 `config/` 与 `resources/` | 2026-09-17 |
| S-L084-04 | `SaTokenExceptionHandler.java` | 401 四句；403 权限 | `.../handler/SaTokenExceptionHandler.java` | 2026-09-17 |
| S-L084-05 | `SaPermissionImpl.java` | 只读当前快照；空则空列表 | `.../core/service/SaPermissionImpl.java` | 2026-09-17 |
| S-L084-06 | `SecurityConfig.java` | checkLogin → getLoginUser → extra clientid | `wta-common-security/.../SecurityConfig.java` | 2026-09-17 |
| S-L084-07 | `docker-compose-infrastructure.yml` + backend compose | Redis 8.6.3 / `namewta-redis` / AOF / `SPRING_DATA_REDIS_HOST` | `release-artifacts/docker/` | 2026-09-17 |
| S-L084-08 | `SysMenuController.getRouters` | 档案袋空 → 「当前登录缺少客户端上下文」 | `wta-system/.../SysMenuController.java` | 2026-09-17 |
| S-L084-09 | `UserActionListener` + `UserLoginSuccessListener` | 名牌 `online_tokens:`；踢/退删名牌 | `wta-admin/.../listener/` | 2026-09-17 |
| S-L084-10 | `PasswordAuthStrategy` + `IAuthStrategy` + `LoginVo` | 写点在策略；`access_token` / `expire_in` | `wta-admin/.../web/` | 2026-09-17 |
| S-L084-11 | `RedisExceptionHandler` + `GlobalExceptionHandler` + `RedisConfig` | 锁席 ≠ 连接席；未知异常；`WTA` 前缀 | `wta-common-redis` / `wta-common-web` | 2026-09-17 |

厂商对照（非本仓文件，版本与 POM 对齐）：Sa-Token 1.45.0 `StpLogicJwtForSimple.createTokenValue` 不把 `timeout` 传入 `SaJwtUtil.createToken`。本课把它当 JWT 简单模式的边界，不当前端合同。

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：NAMEWTA 的登录通行证分两截。一截是浏览器口袋里的 JWT（`access_token`，头 `Authorization: Bearer`），extra 里带着 userId 和 clientid。另一截是 Redis 8 容器 `namewta-redis` 里的 token-session 档案袋，键叫 `loginUser`，里面才有菜单权。`LoginHelper.login` 先 `StpUtil.login` 再 `getTokenSession().set`。真正 set 的是 `PlusSaTokenDao`，前面挡着 5 秒 Caffeine。登录当时没开柜，算 L-006 认证拒绝。已经把票给出去，再读空、被踢、或 Redis 进程挂了，算本课会话丢失。丢失的回执可能是 401 登录状态异常、403 空权、`getRouters` 缺少客户端上下文、或全局未知异常，不要背成一种。名牌 `online_tokens:`、验证码、错次、锁、限流、SSO 手环同住这只柜，但不是档案袋本身。
