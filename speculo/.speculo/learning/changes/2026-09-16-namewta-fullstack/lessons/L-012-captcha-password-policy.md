---
lesson_id: L-012
objective_ids: [OBJ-12]
claimed_cells: [A:CaptchaController.sms,email,authCode, B:PasswordPolicyService]
estimated_minutes: 37
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-code-doors
    minutes: 10
  - segment: password-policy-service
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-002, S-003, S-L012-01, S-L012-02, S-L012-03, S-L012-04, S-L012-05, S-L012-06, S-L012-07, S-L012-08]
---

# Lesson 012：三张考卷和食堂菜谱

## 学完你能做什么

你能指着 `CaptchaController` 说出**三扇匿名 GET 门**各自写什么、限什么流、给谁用，而不是把它们揉成「验证码接口」：

1. **`GET /auth/code`**：图形验证码。给**密码登录**和**注册**当考卷。开关关着就交白卷（`captchaEnabled: false`），不写 Redis。
2. **`GET /resource/sms/code`**：短信 4 位码。给 **`SmsAuthStrategy` 登录**当考卷（也是凭据）。**不是**注册入口。
3. **`GET /resource/email/code`**：邮箱 4 位码。给 **`EmailAuthStrategy` 登录**当考卷。同样**不是**注册入口。

你还能指着 `PasswordPolicyService` 说出它**怎样挡住非法注册**、**怎样不挡住已有账号登录**：

- 它**不是**纯函数：每次先读 `sys_config` 的 `sys.user.passwordPolicy`（`selectConfigByKey` 上 `@Cacheable(SYS_CONFIG)`，缓存名无 TTL），再验明文。
- 写密码（注册是本课硬闸；管理端新增/重置/改密点名、格子在 L-015）走 `validateOrThrow`：弱密码抛 `ServiceException("密码不符合安全策略")`，**不哈希、不落用户、不写会话**。
- `POST /auth/login` **从不**调用 `validate` / `validateOrThrow`。`AuthController.login` 方法体零次 `passwordPolicyService`；`PasswordAuthStrategy` 全文**不注入**菜谱服务。登录走廊上菜谱只在 `GET /auth/client/context`（Client 启用时）贴一张**公示牌** `publicProjection()`。旧密码只要 BCrypt（或临时密码）对上、验证码过关，照样进门。

两套名字不要混：`PasswordAuthStrategy` 是密码**登录**策略（核图形码、对哈希）；`PasswordPolicyService` 是密码**菜谱**服务（读配置、挡写密）。口表「登录时删键」说的是前者和 `SysRegisterService.validateCaptcha`，不是后者。后者全文不碰 `CAPTCHA_CODE_KEY`。

注册是**双闸**：前端 `validatePassword` 挡住弱密码 POST；后端 uniqueness 之后 `validateOrThrow` 挡住落库。两道都过了仍不写会话。绕过前端直打后端，后闸仍在。

本课认格子：`A:CaptchaController.sms,email,authCode`、`B:PasswordPolicyService`。L-011 认 `AuthController` 五扇门的公开形状；L-013 认策略枢纽副作用（删键细节点名、格子在那边）；L-014 认前端 `IdentityAccessService` 全表；`generate*` 的 HTTP 调用方点名、格子在 L-015。本课只把**三张考卷怎么进 Redis**、**菜谱怎么挡住新灶台**讲完。

口诀里若出现「登录时再跑一遍密码策略」「注册用短信验证码」「三扇门都走 `cacheCaptchaCode`」「口表密码策略就是菜谱服务」，那是过期简称。2026-09-16 工作树以字节为准。

## 先把宏观地图放在桌上

L-006 已经把走廊画过：进大楼先换通行证，失败时**会话柜空着**。本课走进门厅墙上的两样东西：三张考卷打印机，和一张食堂菜谱。

```text
  浏览器 / 脚本
       │
       ├─ GET /auth/code              图形考卷 → Redis  global:captcha_codes:{uuid}
       ├─ GET /resource/sms/code      短信考卷 → Redis  global:captcha_codes:{手机号}
       └─ GET /resource/email/code    邮箱考卷 → Redis  global:captcha_codes:{邮箱}
                    │
                    │  只写验证码，不写会话
                    v
  POST /auth/login | POST /auth/register
       │                 │
       │  图形码（开关开）   │  图形码（开关开）
       │  PasswordAuthStrategy
       │  BCrypt / 临时码    │  uniqueness
       │  ✗ validateOrThrow  │  前端 validatePassword
       │                     │  后端 validateOrThrow  ← 菜谱双闸
       v                 v
  LoginHelper.login    registerUser + grantUserType
  （才写会话）          （仍不写会话）
```

三张考卷住在同一间控制室：`backend/wta-admin/.../CaptchaController.java`。类上 `@SaIgnore` + `@RestController`，**没有**类级 `@RequestMapping`，所以三条路径是绝对路径，不挂在 `/auth` 底下——只有图形那条碰巧以 `/auth/code` 开头。

菜谱住在另一栋楼：`backend/wta-modules/wta-system/.../password/PasswordPolicyService.java`。`wta-admin` 的 `AuthController` / `SysRegisterService` **注入**它；它自己读 system 的 `ISysConfigService`。

**类比失效处：** 「考卷」不像学校考卷只盖章。短信/邮箱码**既是考卷也是登录密码**——`grantType=sms|email` 时，对上 Redis 里那串数字就能换通行证。图形码才是纯门禁章：密码登录还要另交用户名密码。

失效点还有六个：

1. 发码成功 ≠ 登录成功。三扇门只往 Redis 塞答案，**从不**调用 `LoginHelper.login`。
2. 注册成功 ≠ 登录成功。`SysRegisterService.register` 全文件没有 `LoginHelper.login`。
3. 前端 `domain-admin` 今天只敲 `/auth/code`。OpenAPI 里有短信/邮箱 path，身份服务**没有**去 GET 它们。
4. 本地 profile `application-local.yml` 把 `captcha.enable` 设成 `false`。你在本机看不到图形码，不代表生产默认关——`application.yml` 默认 `enable: true`、`type: math`。
5. 三台打印机**不共用**同一台盖章机：图形答案内联 `setCacheObject`；短信/邮箱才走 `cacheCaptchaCode`。
6. 核图的门卫 `PasswordAuthStrategy` ≠ 后厨菜谱 `PasswordPolicyService`。登录从不 `validateOrThrow`。注册才是双闸。

## 三扇 code 门在磁盘上长什么样

这一节是 deep 的硬证据。2026-09-16 在 `/srv/WTA-plus` 对过工作树。先对方法，再背口诀。

### 共用底座

| 零件 | 工作树位置 | 本课取值 |
| --- | --- | --- |
| Redis 答案前缀 | `GlobalConstants.CAPTCHA_CODE_KEY` | `global:captcha_codes:` |
| 有效期 | `Constants.CAPTCHA_EXPIRATION` | **2 分钟** |
| 限流前缀 | `GlobalConstants.RATE_LIMIT_KEY` | `global:rate_limit:` + URI + `:` +（可选 IP）+ 业务 key |
| 开关与题型 | `CaptchaProperties`（`captcha.*`，可被 Nacos 覆盖） | `enable` / `type`（`math` 或其它） / `numberLength` / `charLength` |
| 匿名 | 类上 `@SaIgnore` | 未登录可打这三枪 |
| 发码渠道 | `NotificationApplicationService.submit` | 仅短信、邮箱两条；图形码自己画图 |

Redis 写入有**两条路径**，不要合成一句「三扇门都走 `cacheCaptchaCode`」：

| 谁写入 | 生产写入点 | `CaptchaNotifyCallerUnitTest` 钩子 |
| --- | --- | --- |
| 图形 `/auth/code` | `getCodeImpl(Snapshot)` **内联** `RedisUtils.setCacheObject(verifyKey, code, Duration.ofMinutes(2))` | **看不见**。测试只覆盖 `cacheCaptchaCode` |
| 短信 / 邮箱 | 通知成功后才 `cacheCaptchaCode(CAPTCHA_CODE_KEY + 手机号或邮箱, code)` | **看得见**。`protected` 好让测试替换；生产实现同样是 `setCacheObject` + 2 分钟 |

改测试钩子不等于改了图形柜。图形答案柜的生产写入点在 Snapshot 重载里，不经过 `cacheCaptchaCode`。

### 1. `GET /auth/code` → `getCode` / `getCodeImpl`（图形）

公开方法先拍一张**不可变快照** `captchaProperties.currentSnapshot()`，再用快照决定走哪条：

- `enable` 不是 `Boolean.TRUE`：立刻 `R.ok(new CaptchaVo(false, null, null))`。**不**进限流，**不**写 Redis，**不**画图。
- 否则经 `SpringUtils.getAopProxy(this).getCodeImpl(settings)`，好让 `@RateLimiter` 切面生效。

限流钉在 **impl** 上，不是公开 GET 上：`time = 60`、`count = 10`、`limitType = IP`。同一 IP 一分钟最多 10 张图。切面拼出的键形态是 `global:rate_limit:/auth/code:{ip}:`。

生产 GET 只代理 **`getCodeImpl(Snapshot)`**。同文件还有无参 `getCodeImpl()`，也钉着同一套 `@RateLimiter`，但工作树**零调用方**。它会重新 `currentSnapshot()`；若有人从 Snapshot 重载里自调用它，第二层切面不生效。口试只认公开 GET 代理的 Snapshot 重载。

生成步骤（`getCodeImpl(Snapshot)`）：

1. `uuid = IdUtil.simpleUUID()`（无横线）。
2. Redis 键 = `global:captcha_codes:` + uuid。
3. `type == "math"`：`MathGenerator(numberLength, false)` 出一道带 `=` 的算式；`captcha.getCode()` 拿到题目后，**用 SpEL 把 `=` 去掉再求值**，Redis 里存的是**得数**，不是 `"1+2="`。
4. 其它类型：`RandomGenerator(charLength)`，Redis 存字符本身。
5. 画 `WaveAndCircleCaptcha(160, 60)`，Arial Bold 45。
6. **内联** `RedisUtils.setCacheObject(verifyKey, code, Duration.ofMinutes(2))`。**不**调 `cacheCaptchaCode`。
7. 响应 `CaptchaVo(true, uuid, img)`。

谁来对答案（本课点名，删键细节副作用归 L-013）：

- `PasswordAuthStrategy.validateCaptcha`：开关开着才跑。读键 → **立刻删键** → 空则 `CaptchaExpireException`，不等则 `CaptchaException`。比对是 `equalsIgnoreCase`。两种失败都 `recordLoginInfo(..., LOGIN_FAIL)`，**不** `LoginHelper.login`。这是密码**登录**策略，不是菜谱服务。
- `SysRegisterService.validateCaptcha`：同一套「读、删、过期/错误」，注册开关同样看 `captchaProperties.getEnable()`。
- `PasswordPolicyService` **全文不碰** `CAPTCHA_CODE_KEY`。不要把对照表「登录时删键」读成菜谱服务删验证码。

**挡住非法登录/注册的机制：** 没有 uuid、答案过期、看错图、想拿同一张图重试（键已删）——全部停在 `PasswordAuthStrategy` / `SysRegisterService` 的核图，会话柜空。开关关闭等于这扇门摘掉，密码登录只剩 Client + 密码。菜谱服务此时仍不露面。

Nacos 合同（`captchaBehaviorChangesOnTheNextCallAndDeletionRestoresLocalValues`）：把快照换成 `enable=false` 后，**这一枪** `getCode` 返回空盘；`accessor.clear()` 之后断言的是 `currentSnapshot()` 回到本地 `enable=true, type=math`，**没有**第二枪 `getCode`。`getEnable()` 本身也走 `currentSnapshot()`。本课认「快照按调用取」，不认 Nacos 运维。

### 2. `GET /resource/sms/code` → `smsCode`（短信）

参数 `phoneNumber`：`@NotBlank`，再 `RegexValidator.isMobile`。格式不对返回 `R.fail("请输入正确的手机号！")`。

限流钉在**公开 GET 上**：`@RateLimiter(key = "#phoneNumber", time = 60, count = 1)`，`limitType` 默认 `DEFAULT`（按目标全局，**不**按 IP）。同一手机号 60 秒 1 次。格式校验在限流切面**之后**，所以能通过 `@NotBlank` 的垃圾号也会占一次名额。

码：`RandomUtil.randomNumbers(4)`。然后 `notificationService.submit(...)`：

| 字段 | 值 |
| --- | --- |
| 来源 / 业务 | `admin-web` / `auth-captcha` |
| 模板 | `auth_captcha` |
| 收件人类型 | `PHONE`，id 就是手机号 |
| 渠道 | `NotificationChannel.SMS` |
| 模式 | `NotificationMode.SYNC` |
| 模板参数 | `code`、`expireMinutes=2`（**没有** `content` 明文） |
| 幂等键 | `captcha:sms:{手机号}:{当前分钟}` |
| 审计 | `audit=REDACT_SENSITIVE` |

发送失败：打日志（只打异常类名），`R.fail("验证码短信发送失败")`，**不缓存**。`CaptchaNotifyCallerUnitTest` 的标题就是 `smsCaptchaReturnsFailureAndDoesNotCacheWhenProviderRejects`。

发送成功：**之后**才 `cacheCaptchaCode(CAPTCHA_CODE_KEY + phoneNumber, code)`。

谁来对答案：`SmsAuthStrategy.validateSmsCode`。键是 `CAPTCHA_CODE_KEY + phoneNumber`。空/空白 → 过期异常。匹配用 `code.equals(smsCode)`（**区分大小写**；这里本来就是数字）。**工作树这一枪不删 Redis 键**——和图形码「比对前先删」不同。TTL 2 分钟内，同一短信码在策略层仍可能被再次读到。不要把图形码的一次性口诀抄到短信上。

`SmsAuthStrategy.login` 把校验包进 `loginService.checkLogin(LoginType.SMS, username, () -> !validateSmsCode(...))`：匹配失败走重试计数；过期是直接抛，不走 `loginFailed` 递增。通过后才 `LoginHelper.login`。发码接口本身到不了这里。

### 3. `GET /resource/email/code` → `emailCode` / `emailCodeImpl`（邮箱）

公开 GET：`@NotBlank` + `RegexValidator.isEmail`，错了 `R.fail("请输入正确的邮箱地址！")`。对了才 `getAopProxy(this).emailCodeImpl(email)`，最后 `R.ok()`。

限流钉在 **impl** 上：`@RateLimiter(key = "#email", time = 60, count = 1)`，同样 `DEFAULT` 不按 IP。注释写「拆分出来避免开关关闭时仍触发限流」——这是图形码那套拆法的口吻。**工作树里邮箱发送没有 `captcha.enable` 闸**；拆分的实际效果是：格式错误的邮箱**不会**打到限流切面，合法邮箱才会。不要把注释里的「开关」说成已经存在的 enable 字段。

码同样 4 位数字。通知命令与短信对称，差别是：

- 收件人类型 `EMAIL`，渠道 `NotificationChannel.MAIL`
- 幂等键前缀 `captcha:mail:{邮箱}:{分钟}`（枚举名是 `MAIL` 不是 `EMAIL`）

发送失败：**抛** `ServiceException("验证码邮件发送失败")`，不缓存。和短信「返回 `R.fail`」不是同一种信封。成功则在同一 try 里缓存 `CAPTCHA_CODE_KEY + email`。

谁来对答案：`EmailAuthStrategy.validateEmailCode`，形状与短信相同：读键、过期抛、`equals` 匹配、**不删键**。

### 三扇门对照表（口述用）

| | `/auth/code` | `/resource/sms/code` | `/resource/email/code` |
| --- | --- | --- | --- |
| 方法 | `getCode` → `getCodeImpl(Snapshot)` | `smsCode` | `emailCode` → `emailCodeImpl` |
| 答案长什么样 | math 得数或随机字符 | 4 位数字 | 4 位数字 |
| Redis 键尾巴 | uuid | 手机号 | 邮箱 |
| Redis 写入点 | **内联** `RedisUtils.setCacheObject`；**不**走 `cacheCaptchaCode` | 通知成功后 `cacheCaptchaCode` | 通知成功后 `cacheCaptchaCode`（try 内） |
| 限流 | IP / 60s / 10 次，仅开关开；钉在 Snapshot 重载。无参 `getCodeImpl()` 也有注解但零调用 | 手机号 / 60s / 1 次（公开 GET，`DEFAULT` 不按 IP） | 邮箱 / 60s / 1 次（impl，`DEFAULT` 不按 IP） |
| enable 闸 | 有。关则空 Vo | **无** | **无**（注释里的「开关」在工作树无对应字段） |
| 发通知 | 无，自己画图 | SMS，成功才缓存 | MAIL，成功才缓存 |
| 登录/注册时删键 | **删**（`PasswordAuthStrategy.validateCaptcha` / `SysRegisterService.validateCaptcha`）。**不是** `PasswordPolicyService` | **不删**（`SmsAuthStrategy.validateSmsCode`） | **不删**（`EmailAuthStrategy.validateEmailCode`） |
| 当前 domain-admin | `getVerification` 会 GET | 不调用 | 不调用 |
| 挡住什么 | 密码登录/注册的机器人刷图 | 短信登录刷号、无码登录 | 邮箱登录刷号、无码登录 |

## 密码策略服务在磁盘上长什么样

### 它不是计算器

`PasswordPolicyRules.validate(policy, password)` 才是纯函数：给定策略对象和明文，固定顺序吐出 `PasswordViolation(reason, message)` 列表。

`PasswordPolicyService` **不是**。`validate` / `validateOrThrow` / `publicProjection` / `generate*` 每次都先 `currentPolicy()`：

1. `configService.selectConfigByKey(PasswordPolicy.CONFIG_KEY)`，键名 **`sys.user.passwordPolicy`**。实现上 `@Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "#configKey")`。`SYS_CONFIG` 常量是 `"sys_config"`，**没有** `#ttl` 段，默认不过期；`PlusSpringCacheManager.resolveLocal` 缺省为 1（本地一级缓存开）。改 JSON 而不走 `updateConfig`（`@CachePut`）或 `resetConfigCache`，Service 仍吃旧菜谱。这不是每次直读 MySQL。
2. `PasswordPolicyConfigParser.parse`：空、超 500 字符、非 JSON、`version != 1`、长度越界（最短 ≥ 8、最长 ≤ 30、最短 > 最长）、四类字符有一类没强制、特殊字符池不合法、生成器长度/字符池不合法、缺 `defaultPassword.mode`、`RANDOM` 却带 `fixedValue`（`random mode cannot contain fixed value`）、FIXED 默认值本身不合规 → 打 warn（**不打印配置正文**），抛 `ServiceException("PASSWORD_POLICY_UNAVAILABLE")`。
3. 得到 `PasswordPolicy` 以后，才把明文送进 Rules。

所以同一句 `validate("Abcd1234!")`，配置被改成最短 12 位之后，答案会变。这就是矩阵那句「拒绝弱密码；**非纯函数**」。

前端 `@namewta/platform-validation` 的 `validatePassword(policy, password)` **是**纯函数。`domain-admin` 用它挡注册 POST。两边 reason 字符串对齐（`PASSWORD_TOO_SHORT` 等七个），但前端函数**不读** `sys_config`。公示牌从 HTTP 来；计算器在浏览器里跑。

### 公开 API（本课认的）

| 方法 | 做什么 | 挡住非法注册/登录？ | 本课格子？ |
| --- | --- | --- | --- |
| `currentPolicy()` | 读 Spring Cache 再 Parser | 配置坏了直接 `PASSWORD_POLICY_UNAVAILABLE`，注册页拿不到公示牌 | **是**。非纯函数的存储点 |
| `validate(password)` | 返回全部违规，**固定顺序** | 给调用方列清单；自己不抛 | **是** |
| `validateOrThrow(password)` | 有违规就抛 `ServiceException("密码不符合安全策略").setData({violations})` | **注册写密的硬闸**。`login` **从不**调用 | **是** |
| `publicProjection()` | 只露出 min/max、四类必含、允许的特殊字符 | 贴在 `client/context` 上，给前端先挡；**不是**登录凭证 | **是** |
| `generateDefaultPassword()` | FIXED：Parser 已验过 `fixedValue`，Service **直接 return**，不再 `validate`。RANDOM：走 `generate()` 末尾的 Rules 检查 | 不挡登录。候选 HTTP 在 L-015：无 userId 的 `SysUserController.getInfo`、`SysUserCredentialController.candidate`、导入监听 | **点名，格子在 L-015**。`SysUserController.add` 验的是请求体明文，**不**调 generate |
| `generateTemporaryPassword()` | **永远**随机，不走 FIXED；RANDOM 路径末尾 Rules 检查 | 不挡登录；登录消费归 L-015 | **点名，格子在 L-015** |

`publicProjection` 的四类列表在 Service 里写死为 UPPERCASE / LOWERCASE / DIGIT / SPECIAL。Parser 也强制四类全开，所以投影和配置不会出现「配置说不要大写、投影却要」——配不齐四类根本 parse 不过。

基座种子（`50-cde-base-dml.sql`）写入的 v1 JSON：最短 8、最长 30、特殊字符 `@$!%*?&`、生成器长度 12、`defaultPassword.mode = RANDOM`。`sys.user.initPassword` 旧键被随机化退役，不再当现行菜谱。

违规顺序（`PasswordPolicyContractUnitTest` 用 `"a "` 钉死）：

1. `PASSWORD_TOO_SHORT`
2. `PASSWORD_TOO_LONG`（这个例子没有）
3. `PASSWORD_MISSING_UPPERCASE`
4. `PASSWORD_MISSING_LOWERCASE`（这个例子没有）
5. `PASSWORD_MISSING_DIGIT`
6. `PASSWORD_MISSING_SPECIAL`
7. `PASSWORD_CONTAINS_DISALLOWED_CHARACTER`（空格）

`GlobalExceptionHandler` 把 `validateOrThrow` 映成 `R.msg = 密码不符合安全策略`，`data.violations` 仍是那张清单。普通 `ServiceException` 没有这份 data。

### 登录路径实际怎么用它（不要发明）

`AuthController` **注入了** `PasswordPolicyService`。注入不是调用。全文检索调用点：

- **有：** `clientContext` 在 `clientEnabled == true` 时 `vo.setPasswordPolicy(passwordPolicyService.publicProjection())`。Client 停用/找不到时**不**贴公示牌。
- **没有：** `login(...)` 方法体零次 `passwordPolicyService`，零次 `validate` / `validateOrThrow`。

`PasswordAuthStrategy` 是密码**登录**策略，不是菜谱服务。它的构造器依赖 `CaptchaProperties` / `SysLoginService` / `SysUserMapper` / `ClientUserTypeAccessService` / `TemporaryPasswordService`，**没有** `PasswordPolicyService`。`login` 顺序：验证码（若开，`validateCaptcha` 读后删）→ 按用户名加载 → `BCrypt.checkpw` 或临时密码 → Client 登录域 → `LoginHelper.login`。明文密码只拿去对哈希，不拿去对菜谱。

前端 `createIdentityAccessService().login()`：只要 `prepared`，就把用户名密码（及可选 code/uuid）POST 出去，`grantType` 写死 `'password'`。**不**调用 `validatePassword`。

所以「密码策略挡住非法登录」这句话，在工作树上要改口：

- `PasswordPolicyService` 挡住的是**用弱密码办新证**（本课：注册双闸；L-015：管理端写密）。它**不**挡住已有账号登录。
- 挡住密码登录的是：`AuthController` 的 Client 闸、`PasswordAuthStrategy` 的图形考卷和 BCrypt/临时码、登录域、重试锁。短信/邮箱登录另走对应 AuthStrategy。
- 菜谱在登录走廊上只做一件事：让登录/注册页**看见**规矩（`publicProjection`），并让注册按钮在前端就停住。

### 注册路径两道闸（本课主菜）

非法注册被**两道菜谱闸**先后挡住。第一道在浏览器，第二道在 `SysRegisterService`。缺一道都不算本课讲完。登录路径**没有**这两道闸。

**前闸** `register()`（`domains/admin/src/index.ts`），在 `POST /auth/register` **之前**：

1. 必须已经 `prepareLogin()`（拿过 context + `/auth/code`）。
2. `context.registerEnabled` 否则 `registration-disabled`，**不 POST**。
3. `requirePasswordPolicy(context)` 缺公示牌 → `password-policy-unavailable`，**不 POST**。
4. `validatePassword(policy, password)` 有违规 → `password-policy-violation` + violations，**不 POST**。测试 `returns stable policy violations without sending a weak registration request` 断言请求数组仍只有 `/auth/client/context` 和 `/auth/code`。
5. `confirmPassword` 不一致 → `invalid-credentials`，**不 POST**。

`login()` 没有第 3、4 步。弱密码登录照样 POST。

**后闸** `SysRegisterService.register` 顺序（事务，失败回滚）：

1. Client 存在且 NORMAL，否则「客户端不存在或已停用」。
2. `registerEnabled`，否则「当前应用未开放注册」。
3. 配置了登录域且登录域 NORMAL。
4. `captcha.enable` 则校验**图形码**（uuid），不是短信/邮箱码。核图是 `validateCaptcha`（读后删），不是 `validateOrThrow`。
5. 用户名 / 手机 / 邮箱 uniqueness。撞名时**还没**跑菜谱。
6. **`passwordPolicyService.validateOrThrow(password)`** ← 后闸。
7. `BCrypt.hashpw` → `registerUser` → `grantUserType(..., SELF_REGISTER)` → 记 REGISTER 审计。

第 6 步失败：不哈希、不 `registerUser`、不写会话。绕过前端直打后端，后闸仍然在。两道闸都过了：落用户、授登录域、可能写 REGISTER 审计，**仍然没有** `LoginHelper.login`。

其它写密（点名，不认格子）：`SysUserController.add` / `resetPwd` 对**请求体明文** `validateOrThrow`（`add` **不**调 `generateDefaultPassword`）；无 userId 的 `getInfo` 才把 generate 填进 Vo 当候选；导入监听是调用方 generate 后再 `validateOrThrow`。那些是 L-015。本课只要知道：**凡写永久密码，入口都该经过这个 Service**；登录读旧哈希不经过。

## 核心概念与机制

### 直觉讲解

把登录想象成进食堂。

门口三台打印机。第一台印**看图算数**的小纸条，背面写着一个 uuid；你把纸条上的得数连同饭卡密码一起交给门卫。第二台往你手机发 4 个数字。第三台往邮箱发 4 个数字。短信和邮箱那两张纸条**本身就是饭卡**：交对数字就能进。看图那张只是「先证明你不是传送带上的机器人」，饭卡还是用户名密码。

每张纸条塞进门口一个带 2 分钟闹钟的小柜子（Redis）。看图柜子的钥匙是 uuid；短信柜子的钥匙是手机号；邮箱柜子的钥匙是邮箱。看图打印机把答案**直接塞进柜子**（`RedisUtils.setCacheObject`），不经过短信/邮箱那台共用的「盖章机」`cacheCaptchaCode`。改盖章机的测试钩子，看图柜子不动。

门卫核对看图纸条时会**把柜子拆掉**，所以同一张图不能刷第二次。这个门卫叫 `PasswordAuthStrategy`（密码登录策略），旁边办新证的窗口叫 `SysRegisterService.validateCaptcha`，也拆柜。后厨的菜谱先生 `PasswordPolicyService` **从不**摸这些柜子。短信/邮箱柜子今天的门卫只看一眼、不拆柜，闹钟到了自己消失。

打印机旁边有个「不要按太快」的戳：同一手机号/邮箱 60 秒只印一次；同一 IP 看图 60 秒最多 10 张。看图打印机还可以被总开关拔掉——拔掉后面包空盘子 `{ captchaEnabled: false }`，门卫也不再要看图。短信和邮箱打印机**没有**这只总开关。

墙上还贴着食堂菜谱公示牌：密码至少 8 位、四类字符、这些特殊符号才算菜。公示牌故意不写后厨的随机生成器和默认汤底。办**新**饭卡（注册）时是**双闸**：前台先按公示牌量一次（`validatePassword`），不合格连申请表都不递；后厨再按保险柜里的正式菜谱量一次（`validateOrThrow`），不合格不烧菜、不发卡。已经有饭卡的人进门，门卫 `PasswordAuthStrategy` 只尝这张卡是不是真的（BCrypt），**不会**喊菜谱先生过来 `validateOrThrow`，也**不会**因为菜谱今年改了、你的旧汤不合新规就把你拦在门外。

菜谱不在门卫口袋里的小抄上。每次烧新菜，后厨都要打开保险柜（`sys_config` 的 Spring Cache，名 `sys_config`，无 TTL）看今天的菜谱。有人只改了纸上的 JSON、没按铃刷新缓存，后厨仍按旧菜谱烧。柜子坏了或菜谱缺页（包括 `RANDOM` 却写了 `fixedValue`、缺 `defaultPassword.mode`），后厨喊 `PASSWORD_POLICY_UNAVAILABLE`，而不是悄悄改用「123456」。所以后厨不是一台只吃「明文进、布尔出」的计算器。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 图形验证码入口 | image captcha / `GET /auth/code` | `CaptchaController.getCode`：按快照决定是否出题；答案由 `getCodeImpl(Snapshot)` **内联** `setCacheObject` 写入 `CAPTCHA_CODE_KEY + uuid` |
| 短信验证码入口 | SMS code / `GET /resource/sms/code` | `smsCode`：校验手机号、限流、同步短信、成功后 `cacheCaptchaCode` |
| 邮箱验证码入口 | email code / `GET /resource/email/code` | `emailCode` → `emailCodeImpl`：先校验邮箱再限流；MAIL 通道；成功后 `cacheCaptchaCode` |
| 验证码写入钩子 | `cacheCaptchaCode` | 仅短信/邮箱生产路径与 `CaptchaNotifyCallerUnitTest` 覆盖。图形路径绕开它 |
| 验证码开关 | captcha enable | `CaptchaProperties` 的 `enable`。只闸图形出题和密码登录/注册核图。不闸短信/邮箱发送 |
| 快照 | snapshot | 一次调用开始时冻结的 `enable/type/numberLength/charLength`，避免中途刷新拼出半套题 |
| 限流 | rate limit | `@RateLimiter` 切面：令牌不足抛 `ServiceException`；键含 URI 与业务 key 或 IP |
| 幂等键 | idempotency key | 通知命令 `captcha:{sms\|mail}:{target}:{分钟}`，减轻同一分钟重复投递；不是 Redis 答案键 |
| 一次性消费 | one-time consume | 图形码：读后删键。短信/邮箱登录：**工作树未删键** |
| 密码登录策略 | `PasswordAuthStrategy` | 密码 grant 的登录实现：核图形码（读后删）、对 BCrypt/临时码、写会话。**不**调 `validateOrThrow` |
| 密码策略服务 | `PasswordPolicyService` | 读配置 + 校验 + 投影 + 生成的共用入口；非纯函数。全文不碰验证码 Redis |
| 规则内核 | `PasswordPolicyRules` | `(policy, password) → List<PasswordViolation>` 的纯函数 |
| 公示投影 | `PasswordPolicyProjection` / `publicProjection()` | 可给未登录客户端看的 min/max/四类/特殊字符；不含 generator、默认值 |
| 硬拒绝 | `validateOrThrow` | 违规抛带 `violations` 的业务错误；注册在 uniqueness 之后、哈希之前。`login` 从不调用 |
| 注册双闸 | register dual gate | 前闸：前端 `validatePassword` 弱密码不 POST。后闸：`SysRegisterService` uniqueness 之后 `validateOrThrow`。两道都过仍不写会话 |
| 前端纯校验 | `validatePassword`（platform-validation） | 纯函数；只出现在 `register()`，不出现在 `login()` |
| 配置不可用 | `PASSWORD_POLICY_UNAVAILABLE` | 解析/生成失败的稳定对外消息，不泄漏字符池或 fixedValue |
| 写密路径 | password write path | 注册、新增用户、重置、改密、导入候选：要过策略 |
| 登录读密路径 | password verify path | BCrypt / 临时密码：不过 `validateOrThrow` |

**图形码 ≠ 短信码。** 一个按 uuid 索引，一个按手机号索引；一个给 password/register，一个给 sms grant。

**内联 `setCacheObject` ≠ `cacheCaptchaCode`。** 图形走前者；短信/邮箱走后者。测试钩子只看见后者。

**`PasswordAuthStrategy` ≠ `PasswordPolicyService`。** 一个核图并对哈希；一个读配置挡写密。口表「密码策略」若指删键，必须说登录策略类名。

**公示牌 ≠ 登录成功。** `client/context` 带 `passwordPolicy` 只说明 Client 启用且策略能投影。

**Service ≠ Rules。** 口试时说「策略服务读配置再验」；不要说「它是纯函数」。

**前端校验 ≠ 后端权威。** 注册双闸：前端不 POST 是体验和少打一枪；绕过前端仍会被 `validateOrThrow` 挡住。登录没有这两道闸。

### 机制/因果链

**A. 密码登录被图形码挡住（非法登录，会话不写）。**

1. 前端 `prepareLogin`：`GET /auth/client/context`（可能带公示牌）+ `GET /auth/code`。图形答案由 `getCodeImpl(Snapshot)` 内联写入 Redis，不经 `cacheCaptchaCode`。
2. 开关关：图形 Vo 的 `captchaEnabled=false`，登录 body 可以不带 code/uuid；`PasswordAuthStrategy` 跳过 `validateCaptcha`。
3. 开关开：用户交 `code` + `uuid`。**`PasswordAuthStrategy.validateCaptcha`** 读 `global:captcha_codes:{uuid}`，**删除**，再比对。
4. 空 → 过期异常 + 登录失败审计；不等 → 错误异常 + 审计。两条都在 `LoginHelper.login` 前返回。
5. 对了才去 BCrypt。`PasswordPolicyService` 全程不露面：无 `validate`、无 `validateOrThrow`。

因果：刷图受 IP 限流；猜对一张不能重放；关开关等于拆掉这道闸，其它闸还在。菜谱改严不会把旧哈希踢出登录。

**B. 短信/邮箱登录被 4 位码挡住。**

1. 调用方 GET 对应 resource code（今日 `domain-admin` 不走这两枪；脚本或其它客户端可以）。
2. 限流通过、格式对、通知成功，Redis 才有答案。
3. `POST /auth/login` 且 Client 的 grantType 含 `sms` / `email`，策略读手机号/邮箱键。
4. 没码或过期：过期异常，不写会话。码错：`checkLogin` 计一次失败。码对：写会话。
5. 发码失败不缓存，登录侧看起来像「没有考卷」。

因果：没有先打 code 入口，登录枪没有答案可对。通知失败不能靠「先写 Redis 再补发」蒙混——测试禁止这种顺序。

**C. 弱密码注册被两道菜谱挡住（非法注册，不落用户、不写会话）。**

1. `prepareLogin` 拿到 `registerEnabled` 与 `passwordPolicy`。
2. **前闸：** 前端 `validatePassword` 非空 → 抛 `password-policy-violation`，HTTP 请求停在准备阶段那两枪，**没有** `/auth/register`。
3. **后闸：** 有人绕过前端 POST `/auth/register`：注册服务过 Client、开放注册、登录域、可选图形码（`SysRegisterService.validateCaptcha` 读后删）、uniqueness，然后 `passwordPolicyService.validateOrThrow`。
4. 违规：异常 + 事务回滚，不哈希、不 `registerUser`。合规：哈希、落用户、授登录域、记 REGISTER 审计。**仍然不登录。**

因果：非法注册在「新用户行」之前就停。双闸缺一不可：只讲前端是体验；只讲后端会漏「弱密码根本没 POST」。登录走廊不会因为菜谱改写而把老用户踢出去——`login` 从不 `validateOrThrow`。

**D. 公示牌从保险柜走到玻璃窗。**

1. Client 启用 → `publicProjection()`。
2. Parser 失败 → 这一枪失败（`PASSWORD_POLICY_UNAVAILABLE`），登录页会表现为策略不可用，前端注册拒绝发送。
3. 投影不含生成器字符池；`toString` 测试断言不泄漏 `ABCDEFG` / `fixedValue`。

因果：未登录的人看得到规矩，看不到后厨随机源。

## 图、表或文本图

**图题 / caption：** 三张考卷进 Redis（图形内联 set，短信/邮箱走钩子），两道菜谱挡住新灶台；登录不 `validateOrThrow`。alt：三条 GET 验证码写入不同 Redis 键；图形绕开 cacheCaptchaCode；PasswordAuthStrategy 核图删键；注册前端+后端双闸；login 不重验菜谱。

```text
                    @SaIgnore  CaptchaController（wta-admin）
                    ┌─────────────────────────────────────────┐
 GET /auth/code     │ enable? --否--> CaptchaVo(false)        │
   IP 10/min        │   是 --> uuid + 图；内联 setCacheObject │
                    │        Redis[uuid]=得数（绕开钩子）      │
                    │                                          │
 GET /resource/sms  │ 手机号 1/60s --> notify SMS --成功-->    │
   /code            │              cacheCaptchaCode(手机号)    │
                    │                                          │
 GET /resource/     │ 先校验邮箱，再 1/60s --> notify MAIL     │
   email/code       │              成功 --> cacheCaptchaCode   │
                    └─────────────────────────────────────────┘
                         │ 只写验证码
                         v
  PasswordAuthStrategy          SmsAuthStrategy / EmailAuthStrategy
  读 uuid，删键，ignoreCase      读 手机/邮箱，不删键，equals
  再 BCrypt / 临时码             再 checkLogin 重试窗
  ✗ validateOrThrow              ✗ validateOrThrow
                         │
                         v
                   LoginHelper.login   ← 只有这里写会话

  AuthController.clientContext (Client 启用)
        └── passwordPolicyService.publicProjection()   公示牌

  前端 register()                 SysRegisterService.register
  validatePassword 纯函数         uniqueness 之后
  违规：不 POST                   validateOrThrow ──✗── 不 hash / 不落用户
  合规：POST /auth/register       合规：hash + registerUser
                                  ✗ LoginHelper.login
```

**文字等价物：** 顶上一块是 `CaptchaController` 三扇匿名 GET。左边图形码带 enable 闸和 IP 限流，答案按 uuid **内联** `setCacheObject` 进 Redis，绕开 `cacheCaptchaCode`。中间短信按手机号限流，通知成功才走钩子缓存。右边邮箱先检查格式再限流，MAIL 通道成功才走钩子缓存。三条都只写验证码。向下左边是 `PasswordAuthStrategy`（密码登录策略）：核图时删键，再对哈希，**不**调用 `PasswordPolicyService`，**不** `validateOrThrow`。向下右边短信/邮箱策略：按手机或邮箱读码且不删键，错了走重试计数。只有它们成功才写会话。另有一条水平线：Client 上下文把策略投影贴给前端。最底下注册是双闸，前端纯函数挡 POST，后端 uniqueness 之后 `validateOrThrow` 挡落库，成功也不发通行证。

**图的边界：** 不画社交登录、SSO 授权码、临时密码签发状态机、通知 Outbox 内部、`generate*` HTTP、重试锁内部。不保证短信/邮箱登录已被 admin-web 页面使用。不把 `application-local.yml` 的 `captcha.enable: false` 画成全局默认。限流键的精确 Redis 拼法以 `RateLimiterAspect.getCombineKey` 为准，图上只写「URI + IP 或业务 key」。无参 `getCodeImpl()` 不画进生产 GET。

**图题 / caption：** 注册写密顺序，策略夹在 uniqueness 和哈希之间。alt：Client 与验证码在前，策略在哈希前，成功不写会话。

```text
 Client 可用? → 开放注册? → 登录域可用?
        → captcha.enable? ──是──► 图形码（uuid，读后删）
        → 用户名/手机/邮箱 unique?
        → PasswordPolicyService.validateOrThrow   ← 后闸（前闸在前端 validatePassword）
        → BCrypt.hashpw
        → registerUser + grantUserType(SELF_REGISTER)
        → recordLoginInfo(REGISTER)
        → （没有 LoginHelper.login）
```

**文字等价物：** 先确认这扇门允许办新证，再（可选）收图形考卷，再查重，再按保险柜菜谱量密码（后闸；前闸已在浏览器挡住弱密码 POST），最后才烧哈希、落用户、授登录域。审计柜可能写「注册成功」。通行证柜子仍空。策略闸既不在最前（撞名时你先看到账号已存在），也不在哈希之后（失败的明文不会先被存成哈希再回滚靠运气）。这条链没有 `validateOrThrow` 的登录兄弟——`POST /auth/login` 不走这里。

**图的边界：** uniqueness 的具体 SQL 归用户模块。图形码失败仍可能写 LOGIN_FAIL 审计，与注册成功的 REGISTER 审计不是同一句话。不要把短信码画进这条链。

## 正例、反例与边界

**正例 1：** 图形开关关闭。`getCode` 返回 `{ captchaEnabled: false, uuid: null, img: null }`。Nacos 测试把快照换成 `enable=false` 后打 `getCode` 就是这只空盘。`accessor.clear()` 之后断言的是 `currentSnapshot()`，不是第二枪 `getCode`。限流切面未触发。

**正例 2：** 图形开关打开、类型 math。`getCodeImpl(Snapshot)` 内联 `RedisUtils.setCacheObject` 存 SpEL 算出的得数。用户交数字。`PasswordAuthStrategy.validateCaptcha` 删键后 `equalsIgnoreCase`。`PasswordPolicyService` 不在这条链上。

**正例 3：** 短信发送失败不缓存。`smsCode("13812345678")` 在 `submit` 抛错时 `R.fail("验证码短信发送失败")`，测试里 `cachedKey` 仍是 null。该测试覆盖的是 `cacheCaptchaCode` 钩子，**看不见** `/auth/code`。

**正例 4：** 邮箱幂等键以 `captcha:mail:` 开头。测试钉死 `NotificationChannel.MAIL` 的小写名，不是 `email`。

**正例 5：** 前端弱密码 `weak` 注册：violations 含 TOO_SHORT、MISSING_UPPERCASE、MISSING_DIGIT、MISSING_SPECIAL，请求列表没有 `/auth/register`。

**正例 6：** 后端 `"a "` 一次返回五条 reason（短、缺大写、缺数字、缺特殊、非法字符空格），`validateOrThrow` 的 HTTP 消息仍是那句固定中文，清单在 `data.violations`。

**正例 7：** `clientContext` 仅在 Client 启用时贴投影。`AuthController` 注入了 `PasswordPolicyService` ≠ `login` 会调用它。`PasswordAuthStrategy` 连注入都没有。

**正例 8：** 基座 JSON 与 Parser 的 v1 下限一致：8–30、四类全开、`@$!%*?&`。

**正例 9：** FIXED 默认值 `Abcd1234!`：`generateDefaultPassword()` 直接返回该字符串，Service 内不再 `validate`。弱固定值 `123456` 在 Parser 就失败。`generateTemporaryPassword()` 仍随机。

**正例 10：** `SysUserController.add` 对请求体 `validateOrThrow`，不调 `generateDefaultPassword`。无 userId 的 `getInfo` 才把 generate 填进 Vo。

**反例 1：** 把三扇门都说成 `/auth/code` 的子路径。短信和邮箱在 `/resource/**`。类上没有 `@RequestMapping("/auth")`。

**反例 2：** 「注册要先收短信验证码。」`SysRegisterService` 只核 uuid 图形码。短信码给 `SmsAuthStrategy`。

**反例 3：** 「`login` 会 `validateOrThrow`，所以旧弱密码登不进去。」工作树 `AuthController.login` 方法体零次 `passwordPolicyService`；`PasswordAuthStrategy` 不注入菜谱服务。旧哈希对上就能进。

**反例 4：** 「`PasswordPolicyService.validate` 是纯函数，单测不用 mock 配置。」构造器依赖 `ISysConfigService`；契约测试每次 stub `selectConfigByKey`。生产还吃 `@Cacheable(SYS_CONFIG)` 无 TTL 的缓存。

**反例 5：** 「前端过了策略，后端可以不再验。」权威在 `validateOrThrow`。前端是第一道闸，不是唯一闸。

**反例 6：** 「短信登录也会删 Redis 码，和图形一样一次性。」`validateSmsCode` / `validateEmailCode` 没有 `deleteObject`。

**反例 7：** 「短信发送失败仍先缓存，保证用户能登录。」测试明确失败不缓存。

**反例 8：** 「邮箱限流在格式校验之前。」相反：公开方法先 `isEmail`，再代理到带 `@RateLimiter` 的 impl。

**反例 9：** 「`domain-admin` 的 `captcha/` 覆盖三个入口。」`adminCaptchaResource.basePath` 只有 `/auth/code`。

**反例 10：** 「公示牌带 generator 字符池，方便前端生成候选。」`PasswordPolicyProjection` 只有四段：min、max、requiredCharacterClasses、allowedSpecialCharacters。

**反例 11：** 「策略不可用时静默用 123456。」Parser 失败抛 `PASSWORD_POLICY_UNAVAILABLE`；FIXED 默认值本身不合规也解析失败。种子把旧 `initPassword` 随机化退役。

**反例 12：** 「本地关掉 captcha 就等于策略关掉。」`captcha.enable` 不管 `sys.user.passwordPolicy`。本地能无图注册，弱密码仍会被前后端双闸拒绝。

**反例 13：** 「三扇门共用 `cacheCaptchaCode`，改测试钩子就改了图形柜。」图形 `getCodeImpl(Snapshot)` 内联 `setCacheObject`。`CaptchaNotifyCallerUnitTest` 只看见 SMS/MAIL。

**反例 14：** 「对照表『登录时删键（密码策略）』就是 `PasswordPolicyService` 删验证码。」删键的是 `PasswordAuthStrategy.validateCaptcha` 与 `SysRegisterService.validateCaptcha`。菜谱服务全文不碰 `CAPTCHA_CODE_KEY`。

**反例 15：** 「无参 `getCodeImpl()` 就是生产 GET 走的那条。」生产 GET 代理 `getCodeImpl(Snapshot)`。无参重载零调用，且会重新拍快照。

**反例 16：** 「管理端新增用户走 `generateDefaultPassword`，生成后再 Service 内 `validate`。」`add` 验请求体。FIXED 分支直接 return；RANDOM 才在 `generate()` 末尾检查。导入监听才是调用方再 `validateOrThrow`。

**边界：**

- 社交回调、SSO、临时密码消费窗口是别的课。临时密码**生成**走 `generateTemporaryPassword`，仍受同一菜谱，但登录核验不走 `validateOrThrow`。
- 通知模板、Outbox、渠道供应商是 notify 切片。本课只认 Captcha 作为 caller：SYNC、模板码 `auth_captcha`、失败不缓存。
- `RateLimiter` 令牌不足的文案走国际化 `{rate.limiter.message}`，不是验证码错误文案。
- math 题 Redis 存得数：用户交 `3` 而不是 `1+2=`。把题目原文拿去比对会失败。
- 注册 uniqueness 先于策略：已存在账号用弱密码再注册，你先看到「该账号已存在」，看不到策略清单。这不是策略失灵。
- `application-local.yml` `captcha.enable: false` 是本地便利，不是合同默认。

## 变式与迁移

- **变式 A：关图形码做自动化。** 改 `captcha.enable`（或 Nacos 快照）。短信/邮箱发送仍可打，注册/密码登录不再核图。不要删 `CaptchaController`，也不要顺便关掉密码策略。不要把 `cacheCaptchaCode` 测试钩子当成关图开关。
- **变式 B：题型从 math 换成 char。** 改 `captcha.type` 与 `charLength`。Redis 将存字符而不是得数。前端仍只展示 `img` + 提交用户输入，不必知道 SpEL。
- **变式 C：新产品要短信注册。** 今天没有这扇门。需要新合同：注册体收短信码、注册服务按手机号读 `CAPTCHA_CODE_KEY`。不得假装现有 `register` 已经核短信。
- **变式 D：前端以后要发短信码。** 在 domain-admin 增加资源，而不是让页面直写 `/resource/sms/code`。后端限流键已经按手机号钉死。
- **变式 E：加严最短长度到 12。** 改 `sys_config` JSON **并**走 `updateConfig` / `resetConfigCache`。只改库不刷新，`@Cacheable(SYS_CONFIG)` 无 TTL，Service 仍吃旧菜谱。Parser 仍要求 ≥ 8、≤ 30、四类全开。已有用户登录不受影响（`login` 从不 `validateOrThrow`）；下次改密/注册受影响。公示牌下一枪 `client/context` 才会变。
- **变式 F：FIXED 默认密码。** 固定值必须自身合规，否则整份配置不可用。`generateTemporaryPassword` 仍然随机。不要把 FIXED 当成登录后门。
- **变式 G：有人直打 `/auth/register` 绕过前端。** 预期撞上后端 Client / 图形码 / uniqueness / `validateOrThrow`。补洞方向是后端，不是再加一层前端提示。
- **变式 H：策略配置损坏。** 注册页应表现为策略不可用（前端 `password-policy-unavailable` 或后端 `PASSWORD_POLICY_UNAVAILABLE`），而不是放行弱密码。先修 JSON 与缓存，不要在 Service 里写死一套「兜底弱规则」。
- **迁移口诀：** 发码门只写 Redis；图形走内联 set、短信邮箱走 `cacheCaptchaCode`；图形码挡密码登录/注册机器人（`PasswordAuthStrategy` / `SysRegisterService` 删键）；短信邮箱码挡对应 grant 的登录；菜谱挡**写密**（注册双闸）；公示牌给人看；登录尝哈希不尝菜谱、从不 `validateOrThrow`。跳步就会把「旧密码登不进」「注册等于已登录」「口表密码策略删了验证码」说成事实。

## 常见误区

1. **「三个 code 都在 AuthController。」** 三扇门在 `CaptchaController`。`AuthController` 消费图形码（间接）和策略投影。
2. **「验证码写入就是登录。」** 写入的是答案柜。会话柜只在策略里的 `LoginHelper.login`。
3. **「限流键就是验证码键。」** 限流前缀 `global:rate_limit:`；答案前缀 `global:captcha_codes:`。一个管刷接口，一个管对答案。
4. **「邮箱也看 captcha.enable。」** 没有。只有图形出题和密码登录/注册核图看开关。
5. **「短信失败会抛 ServiceException，和邮箱一样。」** 短信 `R.fail`；邮箱 `ServiceException`。口述错误信封时不要混。
6. **「login 注入了 PasswordPolicyService，所以登录会验复杂度。」** Java 注入不是调用。打开 `AuthController.login` 方法体：零次。`PasswordAuthStrategy` 连注入都没有。
7. **「platform-validation 就是 PasswordPolicyService 的前端副本。」** 一个纯函数，一个读配置的 Spring `@Service`。投影字段对齐，生命周期不对齐。
8. **「违规只返回第一条。」** `validate` 收集全部，顺序稳定。前端弱密码测试一次拿出四条 reason。
9. **「特殊字符随便写。」** 必须落在配置的 `allowedSpecialCharacters` 里；其它符号是 `PASSWORD_CONTAINS_DISALLOWED_CHARACTER`。Parser 还要求特殊字符是唯一 ASCII 标点。
10. **「domain-admin 已经对接三个 code。」** 只对接 `/auth/code`。OpenAPI 有另外两条，不代表身份服务调用了它们。
11. **「注册成功会返回 access_token。」** 返回 `R.ok()` 空体。要通行证还得再走登录。
12. **「把 token helpers 和验证码 Redis 说成同一个抽屉。」** 前端令牌在 adapter-storage（L-010）；验证码在服务端 Redis。本课抽屉是 `global:captcha_codes:`。
13. **「三扇门共用 `cacheCaptchaCode`。」** 只有短信/邮箱走钩子。图形是 `getCodeImpl(Snapshot)` 内联 `setCacheObject`。改测试钩子改不到 `/auth/code`。
14. **「口表『密码策略』就是 `PasswordPolicyService`。」** 删键发生在 `PasswordAuthStrategy` / `SysRegisterService`。菜谱服务不碰验证码键，也不出现在 `login` 上。
15. **「注册只靠前端 `validatePassword`。」** 双闸。绕过前端仍撞 `validateOrThrow`。登录没有这两道闸。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把地图钉住，没有标准答案栏。

1. 打开 `backend/wta-admin/.../CaptchaController.java`。圈出三个 `@GetMapping`。在纸上写下每条路径、限流注解在公开方法还是 impl、Redis 键用 uuid 还是手机号/邮箱。再圈两处写入：`getCodeImpl(Snapshot)` 的 `RedisUtils.setCacheObject`，以及短信/邮箱的 `cacheCaptchaCode`。确认无参 `getCodeImpl()` 没有调用方。
2. 对照 `PasswordAuthStrategy.validateCaptcha` 与 `SmsAuthStrategy.validateSmsCode`：谁 `deleteObject`，谁 `equalsIgnoreCase`，谁 `equals`。再看 `SysRegisterService.validateCaptcha` 核的是 uuid 还是手机号。打开 `PasswordPolicyService`，确认没有 `CAPTCHA_CODE_KEY` / `deleteObject`。
3. 打开 `AuthController.login` 和 `clientContext`。数 `passwordPolicyService` 出现几次、出现在哪。不要凭注入字段下结论。`login` 应为零次。再打开 `PasswordAuthStrategy` 构造器，确认没有菜谱服务。
4. 打开 `SysRegisterService.register`，从 Client 检查数到 `BCrypt.hashpw`，在 `validateOrThrow` 上下各标一行「此时还没有用户行 / 此时才有哈希」。确认文件中没有 `LoginHelper.login`。这是后闸。
5. 打开 `domains/admin/src/index.ts` 的 `register` 与 `login`。只在 `register` 看到 `validatePassword`（前闸）。再打开 `index.test.ts` 里 weak 注册那则，看请求 URL 列表停在哪两枪。
6. 打开 `PasswordPolicyService`、`PasswordPolicyConfigParser`、`SysConfigServiceImpl.selectConfigByKey`、`50-cde-base-dml.sql` 里 `sys.user.passwordPolicy` 那行 JSON。对照最短 8、四类全开、键名、`@Cacheable(SYS_CONFIG)`。想清楚：改 JSON 而不刷新配置缓存时，Service 还在吃哪一份。FIXED 分支是否再 `validate`。

## 总结、词汇表与下一步

- **三扇 code 门**都在 `wta-admin` 的 `CaptchaController`，匿名 GET，只写 `global:captcha_codes:`，TTL 2 分钟，不写会话。
- **写入分叉：** 图形 `getCodeImpl(Snapshot)` 内联 `RedisUtils.setCacheObject`；短信/邮箱通知成功后 `cacheCaptchaCode`。测试钩子看不见图形柜。生产 GET 只代理 Snapshot 重载；无参 `getCodeImpl()` 零调用。
- **`/auth/code`**：图形；enable 闸；IP 10 次/分；math 存得数；`PasswordAuthStrategy` / `SysRegisterService` 读 uuid **并删键**。不是 `PasswordPolicyService` 删键。
- **`/resource/sms/code`**、**`/resource/email/code`**：4 位数字；通知成功才缓存；给 sms/email 登录策略；限流按目标 1 次/60 秒；登录核验**不删键**；今日 domain-admin 不调用。
- **`PasswordPolicyService`**：拒绝弱密码的写密入口，**非纯函数**（读 `@Cacheable(SYS_CONFIG)` 的 `sys.user.passwordPolicy`）。`validateOrThrow` 是注册后闸；`publicProjection` 贴玻璃窗；`AuthController.login` 与 `PasswordAuthStrategy` **从不** `validateOrThrow`。`generate*` 点名、格子在 L-015；FIXED 直接 return。
- **注册双闸：** 前端 `validatePassword` 不 POST + 后端 uniqueness 之后不 hash/不 `registerUser`/不发通行证。非法密码登录：考卷失败或哈希失败，不发通行证。非法短信/邮箱登录：没有先发码或码不对。
- 过期口诀（登录再跑策略、注册用短信码、Service 是纯函数、三个入口都在 `/auth`、三扇门共用 `cacheCaptchaCode`、口表密码策略删验证码）以工作树为准，不改字节去迎合摘要。

词汇表：image captcha / SMS code / email code / `cacheCaptchaCode` / snapshot / rate limit / one-time consume / `PasswordAuthStrategy` / `PasswordPolicyService` / `PasswordPolicyRules` / public projection / `validateOrThrow` / `PASSWORD_POLICY_UNAVAILABLE` / password write path / register dual gate。

下一步：L-013 把 `IAuthStrategy` / `SysLoginService` / `SysRegisterService` 的会话写入、重试锁、审计柜讲完。L-014 把 `IdentityAccessService` 与 session 命名空间对齐。管理端新增用户/重置/临时密码的写密链是 L-015。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-002 | `backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | `login` 方法体零次 `passwordPolicyService`，从不 `validateOrThrow`；`clientContext` 在 Client 启用时 `publicProjection()`；`register` 委托 `SysRegisterService` | `login` / `clientContext` / `register` 方法体 | 2026-09-16 |
| S-003 | `backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java` | 三个 `@GetMapping`；图形 `getCodeImpl(Snapshot)` 内联 `setCacheObject`（约 182 行），短信/邮箱走 `cacheCaptchaCode`；无参 `getCodeImpl()` 零调用；限流拆分；math SpEL | `/resource/sms/code`、`/resource/email/code`、`/auth/code`；`getCodeImpl` 两重载 | 2026-09-16 |
| S-L012-01 | `CaptchaProperties.java`；`application.yml`；`application-local.yml`；`NacosLiveRefreshContractUnitTest` | enable/type/length；本地 profile 关图形；`enable=false` 后一枪 `getCode` 空盘；clear 后断言 `currentSnapshot()`，无第二枪 `getCode` | `captcha.*`；测试 `captchaBehaviorChangesOnTheNextCallAndDeletionRestoresLocalValues` | 2026-09-16 |
| S-L012-02 | `GlobalConstants.java`；`Constants.java`；`RateLimiter.java`；`RateLimiterAspect.java` | 答案键前缀、2 分钟 TTL、限流键拼 URI/IP/参数 | `CAPTCHA_CODE_KEY`；`CAPTCHA_EXPIRATION`；`getCombineKey` | 2026-09-16 |
| S-L012-03 | `CaptchaNotifyCallerUnitTest.java`；`NotificationChannel.java` | 成功才缓存；失败不缓存；短信 `R.fail` vs 邮箱抛错；幂等键 `captcha:sms:` / `captcha:mail:`；钩子只覆盖 SMS/MAIL，email 测试直打 `emailCodeImpl` | 四则 `@Test`；`RecordingCaptchaController.cacheCaptchaCode`；枚举 `SMS`/`MAIL` | 2026-09-16 |
| S-L012-04 | `PasswordAuthStrategy.java`；`SmsAuthStrategy.java`；`EmailAuthStrategy.java`；`SysRegisterService.java` | 图形读后删（登录策略，非菜谱服务）；短信/邮箱不删；`PasswordAuthStrategy` 不注入 `PasswordPolicyService`；注册核图形码再 `validateOrThrow`；无 `LoginHelper.login` | `validateCaptcha` / `validateSmsCode` / `validateEmailCode` / `register`；`PasswordAuthStrategy` 字段列表 | 2026-09-16 |
| S-L012-05 | `PasswordPolicyService.java`；`PasswordPolicyRules.java`；`PasswordPolicyConfigParser.java`；`PasswordPolicyProjection.java`；`PasswordPolicy.java`；`SysConfigServiceImpl.selectConfigByKey`；`CacheNames.SYS_CONFIG` | 非纯函数；`@Cacheable(SYS_CONFIG)` 无 TTL；硬拒绝；投影字段；Parser 含 RANDOM+fixedValue / 缺 mode；FIXED generate 直接 return | `CONFIG_KEY`；`validateOrThrow`；`publicProjection`；`generateDefaultPassword` FIXED 分支；Parser `validate` | 2026-09-16 |
| S-L012-06 | `PasswordPolicyContractUnitTest.java`；`50-cde-base-dml.sql` 密码策略块 | 违规顺序；对外消息；种子 JSON v1；FIXED vs RANDOM | `"a "` 用例；`sys.user.passwordPolicy` insert | 2026-09-16 |
| S-L012-07 | `frontend/packages/domains/admin/src/index.ts`；`password-policy.ts`；`captcha/index.ts`；`index.test.ts`；`platform/validation/src/index.ts` | 注册双闸前闸：`validatePassword` 且弱密码不 POST；`login` 不验策略且 `grantType:'password'`；captcha 资源只有 `/auth/code`；platform-validation 是纯函数 | `register` / `login` / `adminCaptchaResource` / `validatePassword` | 2026-09-16 |
| S-L012-08 | `SysUserController.add`/`resetPwd`/`getInfo`；`SysUserCredentialController.candidate`；`SysUserImportListener`；`SysProfileController.updatePwd`；`TemporaryPasswordService` | 其它写密也走同一 Service；`add` 验请求体不 generate；无 userId `getInfo` 才 generate；导入是调用方再 `validateOrThrow`；点名不认格子（L-015） | `validateOrThrow` / `generateDefaultPassword` / `generateTemporaryPassword` | 2026-09-16 |
