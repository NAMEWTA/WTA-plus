---
lesson_id: L-032
objective_ids: [OBJ-32]
claimed_cells:
  - B:OpenApiSigner / OpenApiCanonicalizer
  - B:SaTokenOpenApiMachineSessionOperations
  - B:RedissonOpenApiNonceStore / RedissonOpenApiRateLimiter
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: fail-closed-and-gateway
    minutes: 8
  - segment: hmac-and-canonicalizer
    minutes: 8
  - segment: session-nonce-rate
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 5
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 2
expression_level: eli5
coverage_depth: deep
source_ids: [S-007, S-L032-01, S-L032-02, S-L032-03, S-L032-04, S-L032-05, S-L032-06, S-L032-07, S-L032-08, S-L032-09]
---

# Lesson 032：机器门卫的宏观关闸——HMAC、Sa-Token 会话与失败关闭

## 学完你能做什么

打开 `backend/wta-common/wta-common-openapi/`。你能**说明这间工具间怎么当机器门卫**，并且把 OBJ-32 那句说完：

> `wta-common-openapi` 是机器调用运行时。网关只认五颗签名头；HMAC 验签、nonce 一次性、限流、Sa-Token 机器会话，任何一层配不齐或基础设施挂了，都是**失败关闭**（fail closed）：启动直接起不来，请求直接 401/403/429/503，**没有**内存里的「先放行再说」。

本课认矩阵 **(b)** 三格，符号以磁盘类名为准，不是凭据柜台、不是目录页、不是前端 `openApi.vue`：

1. **`B:OpenApiSigner / OpenApiCanonicalizer`**：把请求压成 NAMEWTA v1 规范字符串，再用 HMAC-SHA256 签名/验签。规范化坏了或密钥不够长，不给「差不多对」。
2. **`B:SaTokenOpenApiMachineSessionOperations`**：验签通过后，在服务器里建/复用机器会话，把令牌塞进当前请求的 Sa-Token 口袋，**绝不**写回响应头。作废只踢 `openapi:credential:` 通道。
3. **`B:RedissonOpenApiNonceStore / RedissonOpenApiRateLimiter`**：nonce 用 Redis `SET NX` 只登记一次；限流用 Redisson 原子桶。Redis 抛错变成 `OpenApiStateStoreException`，网关写成 503，不改成「限流器坏了就放行」。

口试还要能指出三层开关，不要并成一句「默认开启」：

| 层 | 磁盘事实（2026-09-16） | 开还是关 |
| --- | --- | --- |
| 库本身 | `OpenApiProperties.enabled` 布尔默认 `false`；`OpenApiAutoConfiguration` 要 `openapi.enabled=true` 才装配 | **默认关**。装配测试 `remainsCompletelyUnassembledByDefault` |
| admin YAML | `backend/wta-admin/src/main/resources/application.yml`：`enabled: ${OPENAPI_ENABLED:true}` | **本机默认开**。注释写「默认开启；KEK 只能由部署密钥系统注入」 |
| Docker 后端 | `release-artifacts/docker/docker-compose-backend.yml`：`OPENAPI_ENABLED: "${OPENAPI_ENABLED:-false}"` | **容器默认关**。要显式注入才开 |

模块地图若写「默认开启、可由 `OPENAPI_ENABLED=false` 关掉」，那是在说 **admin 组装**，不是库类字段的默认值。以工作树三层为准。

本课不宣称你会拆 `SysOpenApiCatalogController`（OBJ-30）、`SysOpenApiCredentialController` 创建/重置/明文不回显（OBJ-31）、或 `createOpenApiService` / `openApi.vue`（OBJ-33）。SPI 实现住在 `wta-system`，本课只认 common 门口：接口叫什么、网关何时调用、失败时关哪扇门。

## 先把宏观地图放在桌上

L-005 的 OpenAPI 是前端运输箱（`current.json` 指针 / `source.json` / `generated/openapi.ts`）。本课的 OpenAPI 是另一扇门：**外面的程序**拿着 AppKey 和密钥，给 HTTP 请求盖章，才能敲已经贴了 `@OpenApi` 的方法。浏览器 Cookie 登录走 Sa-Token 大厅；机器盖章走这间门卫室。两扇门不准叠在同一次请求上。

2026-09-16 工作树里，这间工具间的牌子、依赖和装配入口如下。路径相对 `backend/`。

```text
外部程序 / 脚本                         （本课只认签名 HTTP；管理页是 L-033）
        │
        │  五颗头：X-OpenAPI-Version / X-App-Key / X-Timestamp
        │          X-Nonce / X-Signature
        v
OpenApiGatewayFilter                    /*  ; order = HIGHEST_PRECEDENCE+10
        │  0 颗头 → 放行给浏览器链
        │  1–4 颗或夹带浏览器票 → 401
        │
        ├─ 1. 对上 MVC + 方法上有 @OpenApi     否则 403
        ├─ 2. OpenApiSigner.verify              否则 401（不说哪一步坏）
        ├─ 3. RedissonOpenApiNonceStore         重复 401；Redis 挂 503
        ├─ 4. RedissonOpenApiRateLimiter        超限 429；Redis 挂 503
        ├─ 5. OpenApiMachineSessionBridge
        │        SaTokenOpenApiMachineSessionOperations
        │        会话不可用 → 503
        ├─ 6. OpenApiAuthorizationMatcher       权限不够 403
        └─ 7. 下游 Controller                   异常原样抛出；口袋里的机器票会清掉
```

| 类 / 合同 | 磁盘路径 | 本课角色 |
| --- | --- | --- |
| `wta-common-openapi` | `wta-common/wta-common-openapi/pom.xml` | description「machine invocation runtime」。显式 common：core + redis + satoken + doc |
| `OpenApiAutoConfiguration` | `.../config/OpenApiAutoConfiguration.java` | `@ConditionalOnProperty(prefix="openapi", name="enabled", havingValue="true")`。AutoConfiguration.imports 只列这一份 |
| `OpenApiStartupValidator` | 同包 | 启用后校验 KEK / TTL / 限流；坏了 `IllegalStateException`，进程起不来 |
| `OpenApiGatewayFilter` | `.../gateway/OpenApiGatewayFilter.java` | Servlet 边界。JavaDoc：「fully signed NAMEWTA v1 machine requests」 |
| `OpenApiCanonicalizer` / `OpenApiSigner` | `.../protocol/` | 规范字符串 + HMAC-SHA256 |
| `RedissonOpenApiNonceStore` | `.../nonce/` | Redis SET-if-absent；键里没有明文 AppKey/nonce |
| `RedissonOpenApiRateLimiter` | `.../ratelimit/` | Redisson `RRateLimiter`；键同样哈希 |
| `SaTokenOpenApiMachineSessionOperations` | `.../session/` | 服务器内部机器会话；`isWriteHeader=false` |
| `OpenApiCredentialResolver` / `OpenApiAuthorizationResolver` | `.../spi/` | 接口在 common；实现在 system。装配时必须各恰好一颗 Bean |
| `@OpenApi` | `.../annotation/OpenApi.java` | **只能贴方法**。仓库业务侧活样本：`OpenApiDemoController.echo` |

**类比：** 学校后门专给送货卡车。司机要在纸条上写「今天几点、这趟独一无二的流水号、去哪间库房、车上有什么」，再用一把只有学校和这家货运公司才有的印章盖章（HMAC）。门卫先看章，再把流水号写进值班本（nonce），再看这辆车这一分钟是不是来得太勤（限流），最后发一枚**不给司机看**的内部工牌（机器会话），让他在楼里走已经贴了「准许货车」标签的房间。值班本丢了、印章机坏了、工牌打印机卡纸，门卫的规矩是：**关门**，不是「先让车进去再补登记」。

**类比失效处：**

1. 不是「学校默认后门永远开着」。库类默认关；admin YAML 本机默认开；Docker compose 默认关。三层不要并成一句。
2. 印章用的是**这张凭据的 appSecret**（URL-safe Base64，至少 32 字节），不是启动时那把 KEK。KEK 是 system 用来给库里的密钥上锁的；common 启动校验仍要 KEK 合法，但 HMAC 不拿 KEK 当 HMAC 密钥。
3. 流水号一旦写进值班本，即使后面限流红灯，**也不会擦掉**。同一颗 nonce 再来是 401，不是 429。
4. 内部工牌不贴在车窗上。`setIsWriteHeader(false)`；调用方永远拿不到这枚 Sa-Token。
5. 没贴 `@OpenApi` 的房间，就算章盖得完美也是 403，而且 **nonce 还没登记**（先解析操作，再验签，再登记 nonce）。
6. 浏览器学生证和货车印章不能同时亮。请求里只要出现 `Authorization` / `Token` 头、同名 Cookie 或同名 query，一律 401。
7. Skill/模块地图的「默认开启」不能盖过装配测试：不设 `openapi.enabled=true` 时，网关 Bean 根本不存在。

## 核心概念与机制

### 直觉讲解

小孩子版只记十四句：

1. **这是机器门，不是文档生成器。** `wta-common-doc` 管 SpringDoc；`wta-common-openapi` 管盖章调用。前端 `generated/openapi.ts` 是复印件，不验 HMAC。
2. **五颗头要么全来，要么全不来。** 全不来：当普通浏览器请求。来几颗但不齐、或夹带浏览器票：401 `OPENAPI_AUTHENTICATION_FAILED`。
3. **失败关闭有两段。** 启动段：KEK/TTL/限流非法、缺 Redis、缺或重复 SPI → 进程起不来。请求段：验签失败 401、未开放 403、超限 429、Redis/会话挂 503。没有「降级成匿名」。
4. **401 故意不说哪一步坏。** 缺头、时钟歪、密钥错、nonce 重放、凭据过期，对外都是同一句 `OPENAPI_AUTHENTICATION_FAILED`。
5. **先对房间，再对章，再记流水号。** `resolveOperation` → `authenticate` → `enforceReplayAndRates`。没 `@OpenApi` 的签名请求 403，且不写 nonce。
6. **规范字符串是唯一立法。** 算法名 `NAMEWTA-HMAC-SHA256`，版本 `v1`，九行用 `\n` 拼。方法强制大写；path/query 按本模块规则编码；body 是 SHA-256 小写十六进制，不是原文。
7. **验签用恒定时间比较。** `MessageDigest.isEqual`。签名不是合法 Base64 就 `verify=false`，不抛给调用方看解码细节。
8. **密钥用完当场抹掉。** `decodeSecret` 得到的字节在 `finally` 里 `Arrays.fill(key, 0)`。长度小于 32 字节直接 `OpenApiAuthenticationException`。
9. **nonce 是 SET NX，不是「先 GET 再 SET」。** 同一 `(appKey, nonce)` 第二次 `register` 返回 false → 401。键名是 `openapi:nonce:` + SHA-256(appKey + 换行 + nonce)，测试断言键里看不到明文。
10. **限流两把尺子。** `app:{appKey}` 默认每分钟 1000；`interface:{appKey}:{interfaceId}` 默认每分钟 100。任一 `tryAcquire` 失败 → 429。Redis 抛错 → 503。
11. **机器会话的 loginId 是 `openapi:{userId}`，设备是 `openapi:credential:{credentialId}`。** `userType` 必须是 `"openapi"`，且不准带浏览器 Client。快照不合格，会话不建。
12. **工牌只在这一次请求的口袋里。** `inRequestScope` 把内部 token 写进 `SaStorage`，`finally` 再把浏览器原值摆回去。套娃或抛错都要还原。
13. **作废只踢货车通道。** `invalidateByUserId` 只 `logout` 那些 `deviceType` 以 `openapi:credential:` 开头的终端，浏览器 `web` 票留下。
14. **计量坏了不能改判决。** `OpenApiCallEventPublisher` 缺省是空操作；E2E 里 publisher 抛错，HTTP 仍是原来的 200/500。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 机器调用运行时 | machine invocation runtime | POM description。不是 `wta-api`，不是 SpringDoc，不是前端 transport |
| 失败关闭 | fail closed | 配置/SPI/Redis/会话不可用时拒绝服务或拒绝启动；**不**提供内存 nonce、内存限流、匿名放行 |
| 规范化 | canonicalization | `OpenApiCanonicalizer.canonicalize`：把 `OpenApiRequest` 压成唯一字符串，作为 HMAC 输入 |
| HMAC 签名器 | `OpenApiSigner` | `HmacSHA256(canonical UTF-8 bytes, decoded appSecret)`，输出 URL-safe Base64 无 padding |
| 线格式版本 | `OpenApiCanonicalizer.VERSION` | 常量 `"v1"`。头 `X-OpenAPI-Version` 必须等于它 |
| 算法名 | `OpenApiCanonicalizer.ALGORITHM` | 常量 `"NAMEWTA-HMAC-SHA256"`。出现在规范字符串第一行，不是 HTTP 头 |
| 网关过滤器 | `OpenApiGatewayFilter` | `OncePerRequestFilter`。URL `/*`，`DispatcherType.REQUEST`，`asyncSupported=true`，`order=HIGHEST_PRECEDENCE+10` |
| 可重放请求 | `ReplayableOpenApiRequest` | 先把 body 读进字节数组，验签和 MVC 用同一份 |
| 一次性流水号库 | `OpenApiNonceStore` / `RedissonOpenApiNonceStore` | `register(appKey, nonce, ttl)`：成功 true，重复 false，基础设施错抛 `OpenApiStateStoreException` |
| 限流器 | `OpenApiRateLimiter` / `RedissonOpenApiRateLimiter` | `acquire(scope, limit, interval)`；内部 `trySetRate(OVERALL)` 再 `tryAcquire` |
| 机器会话操作 | `OpenApiMachineSessionOperations` | 内部接口。JavaDoc：「deliberately never expose a machine token」 |
| Sa-Token 适配 | `SaTokenOpenApiMachineSessionOperations` | 上一个接口的默认实现。`create` / `find` / `inRequestScope` / `withUserLock` / `invalidateByUserId` |
| 会话桥 | `OpenApiMachineSessionBridge` | cache-aside：命中就用；未命中时对 userId 加锁，拉授权快照再 `create` |
| 已验证身份 | `VerifiedOpenApiIdentity` | record `(credentialId, ownerUserId)`。只能在验签成功后构造 |
| 凭据解析 SPI | `OpenApiCredentialResolver` | `resolve(appKey)`。失败原因不对外；返回 null/过期/字段不齐 → 401 |
| 授权快照 SPI | `OpenApiAuthorizationResolver` | `resolve(userId)` → `LoginUser`。必须 `userType=openapi` 且 Client 为空 |
| 调用计量 | `OpenApiCallEventPublisher` | `@ConditionalOnMissingBean` 缺省空实现。抛错被网关吞掉 |
| 认证失败 | `OpenApiAuthenticationException` | 对外码 `OPENAPI_AUTHENTICATION_FAILED`，HTTP 401 |
| 网关拒绝 | `OpenApiGatewayException` | `OPENAPI_FORBIDDEN` 403 / `OPENAPI_RATE_LIMITED` 429 / `OPENAPI_UNAVAILABLE` 503 |
| 状态仓库失败 | `OpenApiStateStoreException` | nonce/限流 Redis 错。网关映射 503 |
| 会话失败 | `OpenApiMachineSessionException` | 「OpenAPI machine session is unavailable」。网关映射 503 |
| 显式开放 | `@OpenApi` | 方法级。`value()` 是目录摘要。类上贴了也不算 |
| 接口身份 | `interfaceId` | `SHA-256(method + "\n" + path)` 的前 12 字节十六进制，24 个 `[0-9a-f]` |

### 机制/因果链

**A. 启动关闸（还没有请求）**

1. 没有 `openapi.enabled=true`：`OpenApiAutoConfiguration` 整组不装。网关、nonce、注册表、会话 Bean 都不在。装配测试对无属性的 `ApplicationContextRunner` 断言「完全未装配」。
2. 属性为 true 之后，`OpenApiStartupValidator.afterPropertiesSet` 立刻检查：
   - `kek-version` 匹配 `[A-Za-z0-9._-]{1,64}`
   - `kek` 是 **标准 Base64**（`Base64.getDecoder()`）且恰好 32 字节；解码后 `Arrays.fill` 抹掉
   - `clock-skew` / `nonce-ttl` / `machine-session-ttl` 必须为正 Duration
   - 两个 per-minute 限流必须 `> 0`
3. 过滤器构造还要注入：唯一的 `OpenApiCredentialResolver`、`OpenApiAuthorizationResolver`、`RedissonClient`、`StpLogic`、名为 `requestMappingHandlerMapping` 的 MVC 表。缺 Redis 或凭据 SPI、SPI 两颗同名冲突，上下文启动失败。测试名就是 `failsClosedWhenRedisOrCredentialSpiIsMissing` / `failsClosedWhenCredentialSpiIsAmbiguous`。
4. **没有内存后备。** 模块 README 写明：启用后缺 Redis / Sa-Token / MVC 注册表 / 唯一 SPI，就失败关闭。
5. `OpenApiOperationRegistry.afterPropertiesSet` 只收录方法上真有 `@OpenApi` 的 MVC 映射。单条解析抛错会 `rejectedMappings++`，**不会**因为某一条 schema 坏了就杀死进程。但 **interfaceId 重复**会 `IllegalStateException("Duplicate OpenAPI interface identity")`。`SaIgnore` / `SaCheckOr` / 非默认 login type 视为不支持，这条映射被拒。
6. 过滤器登记：`urlPatterns=["/*"]`，只处理 `DispatcherType.REQUEST`。Sa-Token 上下文过滤器是 `HIGHEST_PRECEDENCE`；本网关是 `+10`，所以口袋先准备好，再验章。

**B. 一次盖章请求**

1. 数五颗头。0 颗：`filterChain.doFilter` 原样下去，连操作解析都不做。
2. 头数不是 5，或 `hasBrowserAuthentication`：抛认证失败。浏览器票包括：`Authorization`/`Token` 请求头、同名 Cookie、query 名解码后是这两个词。
3. `ReplayableOpenApiRequest` 读尽 body。后面 HMAC 和 Controller `@RequestBody` 看到同一串字节。
4. `resolveOperation`：`handlerMapping.getHandler` 必须是 `HandlerMethod`，且 **方法声明上的** `@OpenApi` 非空。再用匹配 path + method 到注册表，恰好一条。0 条或多条 → 403。解析过程的意外 → 503。
5. `authenticate`：
   - 版本必须 `v1`
   - `appKey` / `nonce` 至少 16 字节 URL-safe Base64 无 `=`；`signature` 至少 32 字节；编码必须是「解再编一模一样」
   - 时间戳是 `0` 或无前导零的十进制秒；必须落在 `now ± clockSkew`（默认 60s）
   - `credentialResolver.resolve(appKey)` 非空，且 `appKey`/`credentialId`/`ownerUserId`/`appSecret` 齐；`expiresAt` 若有必须严格晚于现在
   - `signer.verify(OpenApiRequest, appSecret, signature)` 为 true
   - 任何一步失败都是同一 401，不回「过期」或「密钥错」
6. `enforceReplayAndRates`：先 `nonceStore.register(appKey, nonce, nonceTtl)`。false → 401。然后两把限流，间隔固定 `Duration.ofMinutes(1)`。超限 429。其它运行时 → `OpenApiStateStoreException` → 503。
7. 构造 `VerifiedOpenApiIdentity`，`sessionBridge.execute`：
   - `find` 命中：直接 `inRequestScope`
   - 未命中：`withUserLock(ownerUserId)` 双检，再 `authorizationResolver.resolve`，校验快照后 `create`
   - 快照 `userType` 不是 `openapi`、带 `clientPk`/`clientKey`、userId 对不上 → `OpenApiMachineSessionException` → 网关 503
8. 口袋里的 `LoginUser` 拿去 `authorizationMatcher.matches(loginUser, accessRule)`。false → 403。true 才设 `org.namewta.openapi.verifiedRequest=true`，把可重放请求交给下游。
9. `SecurityConfig` 的登录拦截器看到这个属性和合格机器 `LoginUser`，**跳过**浏览器 ClientId 对齐。属性字符串在 security 模块里是复制的常量，因为 `wta-common-security` **不依赖** openapi。E2E 断言：带 `clientPk` 或 `userType=web` 或没属性，谓词都是 false。
10. `finally` 里若凭据和操作都已解析，发 `OpenApiCallEvent`（无密钥、无 body）。publisher 抛错被吞。下游 Controller 自己抛的异常会再抛出去，事件里 status 记 500，但响应不是网关那张 503 JSON。

**C. HMAC 因果**

规范字符串九行：

```text
NAMEWTA-HMAC-SHA256
v1
{appKey}
{timestamp}
{nonce}
{METHOD 大写}
{canonicalizePath(rawPath)}
{canonicalizeQuery(rawQuery)}
{sha256Hex(body)}
```

path：必须以 `/` 开头。按 `/` 分段；`%HH` 先解码再按 unreserved 重编码（`A-Z a-z 0-9 - . _ ~` 不编码，其余 `%` + 大写十六进制）。双斜杠保留。残缺 `%GG` 或半截 `%` → 认证异常。

query：按 `&` 切开（保留空段）。无 `=` 的键当成值为空。解码后再编码，**`+` 不是空格**，会变成 `%2B`。按 (name, value) 排序后用 `&` 接回。空 query 就是空行。

body：即使是空数组也要哈希。空 body 的 SHA-256 是 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`。

`sign`：密钥 URL-safe Base64 解码，`< 32` 字节拒绝。HMAC 输出同样 URL-safe 无 padding。`verify` 先 `sign` 再两边解码，`MessageDigest.isEqual`；解码失败返回 false。

**D. 机器会话因果**

`create` 调 `stpLogic.createLoginSession("openapi:"+ownerUserId, parameter)`，其中：

- `deviceType` = `deviceId` = `openapi:credential:{credentialId}`
- `isShare=true`，`timeout` 至少 1 秒（Duration 向下取整秒）
- `rightNowCreateTokenSession=true`
- `isWriteHeader=false`
- extra 里塞 `LoginHelper` 那组 user/dept/userType 键
- token session 再塞 `LOGIN_USER_KEY`、`openapiCredentialId`、`openapiOwnerUserId`

`findToken` 扫账号会话终端：设备名匹配、token 仍有效、token session 里的凭据/用户/userType/Client 都对。匹配 **多于 1 个** → 会话异常（失败关闭，不随便挑一个）。

`inRequestScope`：找到 token 后写入 `SaStorage` 的 just-created 键，并删掉 active-timeout 已检查标记；`finally` 按进门时的快照还原。套娃测试：外层抛错后，浏览器原 token 必须回来。

`invalidateByUserId`：`loginId=openapi:{userId}` 的账号会话上，所有机器设备终端 `logoutByTokenValue`。`DefaultOpenApiMachineSessionInvalidator` 先抢同一把用户锁再作废，避免和 `create` 打架。锁键：`openapi:machine-session:user:{userId}`。

### 图、表或文本图

**图 1：一次请求的关闸梯子（ASCII）**

```text
  头计数
    │ 0 ──────────────────────────────► 浏览器链（本课不管）
    │ 1-4 或双身份 ───────────────────► 401 AUTHENTICATION_FAILED
    v 5
  MVC + @OpenApi 方法？ ──否──────────► 403 FORBIDDEN
    │是
    v
  时钟 / 凭据 / HMAC.verify ──否──────► 401 AUTHENTICATION_FAILED
    │是
    v
  nonce SET NX ──重复────────────────► 401 AUTHENTICATION_FAILED
    │ Redis 抛错 ─────────────────────► 503 UNAVAILABLE
    v 第一次
  应用限流 AND 接口限流 ──超限────────► 429 RATE_LIMITED
    │ Redis 抛错 ─────────────────────► 503 UNAVAILABLE
    v
  机器会话 find/create + 口袋 ──失败──► 503 UNAVAILABLE
    v
  权限匹配 ──否───────────────────────► 403 FORBIDDEN
    v
  Controller（异常原样抛；finally 清口袋）
```

**alt：** 从五颗签名头开始的决策树，叶子是放行、401、403、429、503。

**caption：** NAMEWTA v1 网关请求关闸。nonce 在限流之前登记。

**文字等价物：** 没有签名头的请求当浏览器请求。签名头不齐或和浏览器票混用是认证失败。先确认方法被 `@OpenApi` 显式开放，再验时钟、凭据和 HMAC，再把 nonce 原子写入 Redis，再扣两份额度，再建立或复用服务器内部机器会话，最后用快照上的权限/角色匹配访问规则。Redis 或会话基础设施失败是 503，不是改走本地 Map。Controller 自己的异常不会被网关改写成 503 JSON。

**图的边界：** 这张梯子不画凭据如何入库（L-031），不画目录列表 HTTP（L-030），不画 admin-web 按钮（L-033）。KEK 校验发生在启动，不在这张请求图上。`rejectedMappings` 发生在启动扫表，也不在这张图上。

**图 2：对外稳定码**

| HTTP | `msg` 字段 | 何时 | 会不会告诉你哪一步 |
| --- | --- | --- | --- |
| 401 | `OPENAPI_AUTHENTICATION_FAILED` | 头不齐、双身份、版本/编码/时钟、凭据、HMAC、nonce 重放、运行时 clockSkew 非正 | 不会 |
| 403 | `OPENAPI_FORBIDDEN` | 无 `@OpenApi`、注册表对不上、权限/角色不匹配 | 只说禁止 |
| 429 | `OPENAPI_RATE_LIMITED` | 应用或接口额度用尽 | 不说是哪把尺子 |
| 503 | `OPENAPI_UNAVAILABLE` | 状态仓库异常、会话异常、解析操作时的意外、其它 RuntimeException | 不回 Redis 原文 |
| 下游原样 | （不是这张 JSON） | Controller 抛错 | 网关不包一层 |

响应体形状固定为 `{"code":<status>,"msg":"<ERROR_CODE>","data":null}`。已提交的响应不再改。

**alt：** 网关四类稳定错误码与 HTTP 状态对照表。

**caption：** 失败关闭对外只暴露粗分类。

**文字等价物：** 认证问题共用一个 401 码；没开放或没权限是 403；额度是 429；基础设施是 503。body 是三字段 JSON，不是业务 `R` 的成功壳。计量事件失败不能改这些码。

### 正例、反例与边界

**正例 1（发布向量）：** `OpenApiProtocolTest.matchesPublishedUnicodeAndRepeatedQueryVector`。method `post` 规范化成 `POST`。path `/system//openApi/%7euser/文件` 变成 `/system//openApi/~user/%E6%96%87%E4%BB%B6`（`%7e`→`~`，汉字 UTF-8 大写百分号，双斜杠留下）。query `tag=z&empty=&tag=%E4%B8%AD%E6%96%87&plus=a+b&bare` 变成 `bare=&empty=&plus=a%2Bb&tag=%E4%B8%AD%E6%96%87&tag=z`。签名固定为 `ZDWDgeVlxdBvYm9jdzsbJYfITNdMmcxbXZmfBB5IJ5Y`。这是线格式立法，不是「看起来像 AWS 签名所以随便抄」。

**正例 2（空 body）：** GET `/health/`、空 query、空字节。规范字符串以 `/health/\n\n` + 空哈希结尾。

**正例 3（装配齐全）：** `assemblesExactlyOneCompleteGatewayWhenEnabled` 在 `openapi.enabled=true` 且 KEK 合法、SPI/Redis/StpLogic/MVC 齐备时，nonce、限流、会话操作、桥、作废器、注册表、计量、过滤器登记各恰好一颗。`urlPatterns` 就是 `/*`。

**正例 4（E2E 通车）：** 签名 POST `/gateway/echo`，body 原样回。nonce 记一次；限流 scopes 长度为 2；事件里 credentialId=7、ownerUserId=9、status=200；请求结束后 `currentUser` 为空。

**正例 5（只作废机器通道）：** 账号会话上同时有 `openapi:credential:7`、`openapi:credential:8`、`web`。`invalidateByUserId(9)` 返回 2，只 logout 前两枚，从不碰 `browser`。

**正例 6（并发未命中收敛）：** 十六个线程同时 `bridge.execute`，授权 SPI 只跑 1 次，`create` 只 1 次。用户锁把惊群收成一次建会话。

**反例 1：** 「库默认开启，只要依赖了 jar 就有网关。」不设 `openapi.enabled=true` 时过滤器 Bean 不存在。Docker 默认还把环境变量设成 false。

**反例 2：** 「KEK 就是 HMAC 密钥。」HMAC 用凭据 `appSecret`（URL-safe，≥32 字节）。KEK 是标准 Base64 恰好 32 字节，给 system 加密库存密钥，并在启用时被 `OpenApiStartupValidator` 卡死。编码函数都不是同一个 Decoder。

**反例 3：** 「验签失败返回 400 并指出是 nonce 还是时钟。」全部 401 同一码。`OpenApiAuthenticationException` 的 JavaDoc 就是 hide the rejected stage。

**反例 4：** 「Redis 挂了就改用本机 Set 记 nonce。」`RedissonOpenApiNonceStore` 把运行时包成 `OpenApiStateStoreException`。E2E：nonce 抛 `redis-down` → 503 `OPENAPI_UNAVAILABLE`，会话口袋仍是空。

**反例 5：** 「限流拒绝后可以用同一颗 nonce 再试。」nonce 先登记。重放是 401，不是 429。

**反例 6：** 「机器令牌会出现在 `Authorization` 响应头里。」`setIsWriteHeader(false)`；测试 `never().setTokenValueToResponseHeader`。接口 JavaDoc：never expose a machine token。

**反例 7：** 「机器 `LoginUser` 可以复用浏览器 Client。」桥和 `loginUser(...)` 都拒绝非空 `clientPk`/`clientKey`。带 Client 的快照 `create` 次数为 0。

**反例 8：** 「给没注解的 URL 盖章，会进 Controller 再 401。」E2E：签名打 `/gateway/browser` → 403，Controller 的 browser 计数仍是那次未签名请求的 1，nonce 集合为空。

**反例 9：** 「类上贴 `@OpenApi` 就开放整类。」`@Target(METHOD)`。测试 `annotationCanOnlyTargetMethods`。注册表读的是 `getMethod().getDeclaredAnnotation`。

**反例 10：** 「启动时某一条 schema 解析失败，整个应用挂掉。」注册表吞掉单条 `RuntimeException` 并计数。真正一票否决的是重复 `interfaceId`、以及启用后的 KEK/SPI/Redis。

**反例 11：** 「`openapi.kek` 非法时异常消息会把密钥打印出来。」装配测试：`openapi.kek=not-a-key` 的失败文本不含 `not-a-key`。

**反例 12：** 「计量 SPI 必须实现，否则网关不装。」`@ConditionalOnMissingBean` 给空 lambda。E2E publisher 故意抛 `metering-down`，通车仍 200。

**反例 13：** 「改限流数字仍用同一把 Redis 键。」键哈希包含 `scope + limit + intervalMillis`。测试里 limit 10 与 11 不是同一键。

**反例 14：** 「这就是 `wta-api`。」`wta-api` 是 Java 跨模块合同。本课 jar 是机器协议运行时。浏览器 JSON 又是第三张合同。

**反例 15：** 「`OpenApiDemoController` 证明所有业务接口都已开放。」磁盘上业务 `@OpenApi` 活样本目前就是 demo 回显。其它方法要自己贴注解才会进注册表。

**边界：**

- admin YAML 给了 `kek-version` 默认 `dev-local` 和一枚本地 KEK 回落值；README 表格写生产默认空、必须由密钥系统注入。口试同时说这两句：本机能靠 YAML 回落启动；生产按 README 不要把密钥当仓库默认。本课不把那串 Base64 当教材抄出来用。
- `PropertyOpenApiKekProvider` 在 system，不在本课 jar。历史版本可走 `openapi.keks.{version}`。那是凭据加密（L-031），不要说成 HMAC。
- 过滤器只登记 `DispatcherType.REQUEST`。ASYNC/ERROR 不会再跑一遍验签。Sa-Token 上下文过滤器则包含 ASYNC/ERROR。
- `clockSkew` 在请求时如果被改成非正，`requirePositive` 会变成 401，不是启动那次 `IllegalStateException`。启动校验挡住的是进程起来的那份配置。
- `create` 的 TTL 用 `Math.max(1, ttl.toSeconds())`。不足一秒会被抬到 1 秒。桥的构造函数则直接拒绝非正 Duration。
- 多 path 映射：E2E `/gateway/multi/{id}` 与 `/gateway/alias/{id}` 是同一方法两个 path，两次调用的 `interfaceId` 不同。注册表按 method+path 哈希，不是按 Java 方法对象。
- 下游失败：`/gateway/fail` 的异常穿透 MockMvc；口袋仍被清掉；事件 status=500。
- 权限拒绝也会清口袋，Controller 计数为 0。
- common **不**依赖 `wta-modules`。会话类型 `LoginUser` 来自 `wta-api`（经 satoken 传递）。SPI 实现必须由组装应用提供。
- 教学正文不是生产授权。启用机器门、下发 KEK、打开 Docker 的 `OPENAPI_ENABLED`，都要环境负责人另批。

## 变式与迁移

- **变式 A：本机要关机器门。** 设 `OPENAPI_ENABLED=false`（或 YAML `openapi.enabled=false`）。装配消失，带五颗头的请求不会被本过滤器拦。Docker 已经默认这条。不要去删 jar 依赖。

- **变式 B：启用但 KEK 没注入。** 启动失败，消息围绕 `openapi.kek`，不含密钥原文。修的是密钥系统，不是把校验改成警告日志。

- **变式 C：调用方改了一个 query 值。** 规范字符串变了，`verify` false，401。正例里 `id=1` 改 `id=2` 即如此。不要靠「网关再帮你重排一次就不验了」——重排发生在验签前，但签名必须按同一规则覆盖重排后的串。

- **变式 D：同一秒同一 nonce 打两次。** 第一次 200 并 SET NX。第二次 401。调用方必须换 nonce，不能靠重试同一个信封。

- **变式 E：Redis 闪断发生在限流。** `tryAcquire` 抛错 → `OpenApiStateStoreException` → 503。产品若想「Redis 挂了放开」，那是改失败关闭合同，不是当前实现。

- **变式 F：权限快照过期、角色刚被收回。** 桥在会话 TTL 内会复用 token session 里的 `LoginUser`。收回权限后要靠 `OpenApiMachineSessionInvalidator.invalidateByUserId` 踢机器终端（凭据重置/授权变更走 L-031 的写路径）。本课只保证作废器会加同一把用户锁。

- **变式 G：浏览器请求误加了 `X-App-Key`。** 五颗头不齐 → 401，原登录链走不到。少加头比「加一颗试试」更安全。

- **变式 H：要开放一个业务方法。** 在**方法**上贴 `@OpenApi("摘要")`，并保持 Sa-Token 注解是注册表认识的形态。不要贴类、不要 `SaIgnore` 混用。然后走 L-030 的目录、L-031 的授权，不是只改 YAML。

- **迁移口诀：** 先分三层开关 → 再分启动关闸和请求关闸 → HMAC 只认九行规范串 → nonce 先于限流且不退款 → 机器票只在服务器口袋里 → Redis/会话挂了是 503。跳步会出现「把文档生成器当成验签」「把 KEK 当 HMAC 密钥」「把限流失败当可重放」「把机器 token 当成要发给客户端的 Bearer」。

## 常见误区

1. **「OBJ-32 就是 SpringDoc / `wta-common-doc`。」** doc 出文档；本模块验机器章。
2. **「`current.json` 管 HMAC。」** 那是前端修订指针。HMAC 立法在 `OpenApiCanonicalizer`。
3. **「默认开启」不分层。** 库关、admin YAML 开、Docker 关。
4. **「失败关闭 = 任何注册表脏数据都杀进程。」** 单条映射拒绝只计数；杀进程的是启用后的 KEK/TTL/限流/SPI/Redis，以及重复 interfaceId。
5. **「401 会告诉你是过期还是签名错。」** 故意不说。
6. **「nonce 存在 Redis 里能看见 AppKey。」** 键是哈希。测试禁止明文出现。
7. **「限流用 `wta-common-redis` 的 `@RateLimiter`。」** 机器门自己的 `RedissonOpenApiRateLimiter`，键前缀 `openapi:rate:`。
8. **「机器会话 loginId 就是用户数字 id。」** 是 `openapi:{userId}`，避免和浏览器登录会话撞车。
9. **「`inRequestScope` 可以不还原。」** 不还原会把内部 token 留给后续浏览器请求。测试专门打套娃和异常。
10. **「security 模块 import 了网关类。」** 它复制属性名字符串，因为不能反向依赖 openapi。
11. **「KEK 用 URL-safe Base64。」** 启动校验用标准 Base64。HMAC 密钥才是 URL-safe。
12. **「query 里的 `+` 是空格。」** 本规范化把 `+` 当成加号，输出 `%2B`。
13. **「把 `@OpenApi` 理解成 SpringDoc `@Operation`。」** 本注解只做机器开放登记；摘要给目录用。
14. **「common 里能 new 凭据实现。」** 实现在 system。common 只声明 SPI。
15. **「计量失败应 503。」** 计量可选，不能改已经决定的 HTTP。
16. **「这课覆盖凭据明文不回显。」** 那是 OBJ-31。本课只在验签时看到解密后的 `OpenApiCredential`，不讲列表 VO。

## 非评分暂停

打开磁盘，不要凭记忆默写。不要改文件。没有标准答案栏、没有分数。

1. 打开 `OpenApiAutoConfiguration.java`。圈 `@ConditionalOnProperty(..., havingValue = "true")`。圈 nonce/限流/会话三个 `@Bean` 的实现类名。圈过滤器 `HIGHEST_PRECEDENCE + 10` 和 `/*`。
2. 打开 `OpenApiStartupValidator.java`。圈 KEK 32 字节、`Base64.getDecoder()`、`Arrays.fill`。圈五个 `requirePositive`。
3. 打开 `application.yml` 的 `openapi:` 段与 `docker-compose-backend.yml` 的 `OPENAPI_ENABLED`。对照库字段默认 `false`。
4. 打开 `OpenApiCanonicalizer.java` 的九行 `String.join`。打开 `OpenApiProtocolTest` 那份 Unicode 向量，顺着 path/query/签名对一遍。
5. 打开 `OpenApiSigner.java`。圈 `HmacSHA256`、`withoutPadding`、`MessageDigest.isEqual`、`key.length < 32`、`finally` 抹键。
6. 打开 `OpenApiGatewayFilter.doFilterInternal`。按头计数 → 双身份 → 解析 → 验签 → nonce → 限流 → 会话 → 权限的顺序用笔点。圈 `writeError` 的 JSON 形状。
7. 打开 `RedissonOpenApiNonceStore` 与 `RedissonOpenApiRateLimiter`。圈 `setIfAbsent`、`openapi:nonce:` / `openapi:rate:`、两处 `OpenApiStateStoreException`。
8. 打开 `SaTokenOpenApiMachineSessionOperations`。圈 `MACHINE_DEVICE_PREFIX`、`isWriteHeader(false)`、`loginId`、`inRequestScope` 的 `finally`、`invalidateByUserId` 的 `startsWith` 过滤。

## 总结、词汇表与下一步

- **三层开关：** 库默认关；admin YAML 本机默认开；Docker 默认关。
- **两段失败关闭：** 启动卡 KEK/TTL/SPI/Redis；请求卡章、nonce、额度、会话。没有内存放行。
- **HMAC 立法：** `NAMEWTA-HMAC-SHA256` + `v1` 九行串；URL-safe 密钥；恒定时间比较。
- **nonce 不退款：** SET NX 在限流前；重放 401；Redis 挂 503。
- **机器票不出门：** Sa-Token 会话只在服务器口袋；作废只踢 `openapi:credential:`。
- **对外四码：** 401 认证、403 禁止、429 额度、503 不可用。401 不拆原因。
- **矩阵 (b)：** Signer/Canonicalizer、SaToken 机器会话、Redisson nonce/限流。本课到此。

词汇表：`wta-common-openapi` / `OpenApiGatewayFilter` / `OpenApiCanonicalizer` / `OpenApiSigner` / `OpenApiRequest` / `NAMEWTA-HMAC-SHA256` / `RedissonOpenApiNonceStore` / `RedissonOpenApiRateLimiter` / `SaTokenOpenApiMachineSessionOperations` / `OpenApiMachineSessionBridge` / `VerifiedOpenApiIdentity` / `OpenApiCredentialResolver` / fail closed / `@OpenApi` / `interfaceId`。

下一步：凭据创建/重置/启停且明文不回显是 OBJ-31；目录自有/他人接口是 OBJ-30；管理端 `createOpenApiService` 与 `openApi.vue` 是 OBJ-33。浏览器登录写会话是 OBJ-11/OBJ-13，不要和本课机器口袋并成一次登录。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md` | 模块职责写成机器协议/注册表/网关/Sa-Token 桥；「默认开启」相对 admin 组装，库默认以源码为准 | `wta-common-openapi` 行 | 2026-09-16 |
| S-L032-01 | `wta-common-openapi/pom.xml`；`OpenApiAutoConfiguration.java`；`AutoConfiguration.imports` | artifact 描述；`enabled=true` 才装配；Bean 名单；过滤器 `/*` 与 order | POM；配置类全文；imports 一行 | 2026-09-16 |
| S-L032-02 | `OpenApiProperties.java`；`application.yml`；`docker-compose-backend.yml`；模块 `README.md` | 库字段默认 false；admin `${OPENAPI_ENABLED:true}`；compose 默认 false；README 失败关闭与无内存后备 | properties 字段；YAML `openapi:`；compose `x-admin-environment`；README 配置表 | 2026-09-16 |
| S-L032-03 | `OpenApiStartupValidator.java`；`OpenApiAssemblyContextTest.java` | KEK/TTL/限流启动校验；缺 Redis/SPI、SPI 歧义、非法 KEK 不回显；默认完全不装配 | validator 全文；装配测试各 `failsClosed*` / `remainsCompletelyUnassembledByDefault` | 2026-09-16 |
| S-L032-04 | `OpenApiCanonicalizer.java`；`OpenApiSigner.java`；`OpenApiRequest.java`；`OpenApiHeaders.java`；`OpenApiProtocolTest.java` | 九行规范串；path/query/body 规则；HMAC 与恒定比较；发布向量与篡改拒绝 | protocol 包；测试三个方法 | 2026-09-16 |
| S-L032-05 | `OpenApiGatewayFilter.java`；`ReplayableOpenApiRequest.java`；`OpenApiGatewayException.java`；`OpenApiAuthenticationException.java`；`OpenApiGatewayE2ETest.java` | 头计数、双身份、顺序、错误 JSON、401/403/429/503、nonce 先于限流、计量不影响响应、下游异常穿透 | filter `doFilterInternal` 与 helpers；E2E 各场景 | 2026-09-16 |
| S-L032-06 | `RedissonOpenApiNonceStore.java`；`RedissonOpenApiRateLimiter.java`；`OpenApiGatewayStateStoreTest.java` | SET NX；键哈希；`trySetRate`+`tryAcquire`；Redis 错 → `OpenApiStateStoreException` | nonce/ratelimit 包；状态仓库测试两则 | 2026-09-16 |
| S-L032-07 | `SaTokenOpenApiMachineSessionOperations.java`；`OpenApiMachineSessionBridge.java`；`DefaultOpenApiMachineSessionInvalidator.java`；对应两份 session 测试 | 服务器内部票；loginId/设备前缀；口袋还原；只作废机器通道；锁上的双检 create；拒绝浏览器 Client 快照 | session 包与测试 | 2026-09-16 |
| S-L032-08 | `OpenApiOperationRegistry.java`；`OpenApi.java`；`OpenApiAuthorizationMatcher.java`；`OpenApiDemoController.java`；`OpenApiOperationRegistryTest.java` | 方法级开放；interfaceId 24 hex；拒绝 SaIgnore/重复身份；demo 回显活样本 | registry/annotation；demo controller；registry 测试 | 2026-09-16 |
| S-L032-09 | `SecurityConfig.java`；`OpenApiCredentialResolver.java`；`OpenApiAuthorizationResolver.java`；`OpenApiCredential.java`；`PropertyOpenApiKekProvider.java`（仅边界） | 复制属性名跳过 Client 校验；SPI 在 common、KEK 提供器在 system；security 不依赖 openapi | `isVerifiedOpenApiRequest`；spi 包；system kek provider | 2026-09-16 |
