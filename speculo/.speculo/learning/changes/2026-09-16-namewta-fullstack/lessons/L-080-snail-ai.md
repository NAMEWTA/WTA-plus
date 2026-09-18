---
lesson_id: L-080
objective_ids: [OBJ-80]
claimed_cells:
  - A:SnailAiController.registerCurrentUser
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: one-window-on-disk
    minutes: 8
  - segment: switches-envelope-and-client
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-007, S-008, S-010, S-L002-01, S-L003-01, S-L080-01, S-L080-02, S-L080-03, S-L080-04, S-L080-05, S-L080-06, S-L080-07, S-L080-08, S-L080-09]
---

# Lesson 080：宏观一扇注册窗——`SnailAiController.registerCurrentUser`

## 学完你能做什么

打开 `backend/wta-modules/wta-ai/src/main/java/org/namewta/ai/controller/SnailAiController.java`，你能**口述这块宏观一扇窗**：NAMEWTA 已经认过你是谁，这扇窗只负责去对面 Snail AI 楼里**换一枚 OpenAPI 工牌**（`openId`），再把工牌装进我们自己的信封 `R` 交回去。口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

**`A:SnailAiController.registerCurrentUser`**：门牌 `/snail-ai` 上，**正好 1 个** Java 公开映射方法。路径是 **`POST /snail-ai/user/register`**。没有 list、没有 get、没有 chat、没有 body。口试按磁盘，不要把厨房 `createAiService.registerCurrentSnailUser`、厅堂 iframe、或监控菜单「AI控制台」背成第二扇 HTTP。

OBJ-80 原文只要你能口述 `SnailAiController.registerCurrentUser`。本课还要把 **换工牌这条链**讲完，否则「能口述」会退化成背路径：

- 登记表：`wta-ai` 是 **classic**。调用链是 **Controller → 厂商 `OpenApiUserClient.register`**。没有 Service、没有 ServiceImpl、没有 Mapper、没有 XML、没有 UseCase、没有 DAO。`src/main/java` 里**只有这一份**业务 Java。
- 类上 `@ConditionalOnProperty(prefix = "snail-ai.open-api", name = "enabled", havingValue = "true")`：这把钥匙对不上，这扇窗整扇不进 Spring，不是返回空用户。
- 身份不从 JSON 读。`LoginHelper.getUserId()` / `getLoginUser()` 取出当前会话，填 `externalId` + `nickname`，再打电话给对面。
- 对面回的是厂商信封 `Result`（`status` **1 成功 / 0 失败**）。我们只在 `status == 1` 且 `data != null` 时 `R.ok(vo)`。`R.code` 成功是 **200**，不要和 1 对调。
- 失败抛 `SnailAiException`（厂商包名拼写是 `execption`）。`GlobalExceptionHandler` **没有**专席，会落到 `RuntimeException` 那一档：HTTP 500，文案是「发生未知异常，请联系管理员 [错误编号: …]」，**不是**控制器里那句中文。

本课**不宣称**你会拆 `createAiService` / `createAiWebDomain` / `AiChatPage`（L-081）、监控 iframe `monitor/snailai/index`（L-024）、`bundle-full` 插入 `wta-ai` 的装配细节（对照 L-002 / 与 job 的 L-079 同类问题）、或把 `wta-snailai-server` 厂商引擎内部标 covered。厨房 URL 只当**对照**：证明这一扇被浏览器扣了扳机。

2026-09-17 工作树先钉死**包边界**（口试先数窗，再数钥匙）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第二扇 `GET /user`、`POST /chat`、`/agents` | **没有。** 厂商示例 `OpenApiDemoController` 才有那些；我们的柜台只公开 `registerCurrentUser` |
| `@SaCheckPermission("ai:…")` | **没有。** 类和方法都没贴权限字。登录拦截要过；菜单「AI会话」种子权限字段是空串 |
| `@SaIgnore` / 匿名可调 | **没有。** `/snail-ai/**` **不在** `security.excludes`。排除的是 `/snail-chat/**` 与 `/api/snail/chat/**` |
| 请求体里传 `userId` / `openId` | **没有。** OpenAPI 合同 `requestBody?: never`。身份只来自当前 Token |
| `R` 的成功码是 1 | **不是。** `R` 成功码 200。1 是厂商 `Result.status`，常量名 `SNAIL_AI_SUCCESS` |
| Controller 自己实现 HTTP 客户端 | **没有。** 注入 `OpenApiUserClient`，JDK 动态代理再发 HTTP |
| ServiceImpl / Mapper / `src/main/resources` | **没有。** 模块只有 `controller/SnailAiController.java`。classic 脚本缺 resources 会退出码 1，**失败不改登记** |
| 本课 Java 测试 | **没有。** 2026-09-17 无 `src/test`。前端厨房测试是 L-081 |
| `snail-ai.enabled` 就是这扇窗的电闸 | **不是。** 那把钥匙管 `SnailAiConfig`（Agent + `@EnableSnailAiOpenApi`）。窗上写的是 **`snail-ai.open-api.enabled`** |
| yml 的 `open-api` 等于厂商的 `openapi` | **不是同一把钥匙。** 见机制段。口试要能指两个前缀 |
| iframe「AI控制台」就是本窗 | **不是。** `monitor/snailai/index` 是 L-024 外置玻璃，嵌的是 **Snail AI 服务器后台** |
| 菜单「AI会话」`ai/chat/index` 就是本窗 | **不是。** 那是 L-081 的嵌入页；它**会扣**本窗的扳机，但本格不覆盖那一页 |
| nginx `/snail-ai/` 就是 `POST /snail-ai/user/register` | **不是同一条河。** LB `/snail-ai/` 转到 `namewta-snailai-server:8900`。浏览器注册走的是 **`/{env}-api/snail-ai/user/register` → admin** |
| 给这间房加 UseCase/DAO 才算 classic | **本课不发动。** 登记 classic 保护存量形状；新的独立业务能力要另登 layered |

## 先把宏观地图放在桌上

L-002 已经把 `wta-ai` 钉成「AI 业务薄封装」：依赖 `wta-api` + `wta-common-ai` 等，**只在 `bundle-full` 进 admin**。L-003 把它放进 classic 列，并写明家具是 **Controller + `OpenApiUserClient`**，没有 ServiceImpl、没有 Mapper。本课走进**同一份 Controller 的唯一公开方法**，把「换工牌」从一句话变成能指字段、能指钥匙、能指信封的口试。

把 NAMEWTA 想成酒店前台。你已经用房卡进了酒店（Sa-Token 登录）。对面另有一栋 **Snail AI 楼**（`wta-snailai-server`，端口 8900，context-path `/snail-ai`）。那栋楼不认酒店房卡，只认自己发的工牌 `openId`。本课这一扇窗就在酒店里：看一眼你的房卡，用你的用户号当 `externalId`、昵称当 `nickname`，打电话到对面「给这位客人办工牌」。办过就幂等拿回同一枚；没办过就新建。酒店再把工牌塞进自己的回执 `R`。

**类比失效边界：** 「宏观一扇注册窗」**不**等于「打开这扇窗就能聊天」。聊天 iframe、`/snail-chat/`、`trustedCredential` 是 L-081。类比也**不**等于「yml 写了 `open-api.enabled: true` 对面就一定能接通」——厂商客户端另有一把 `snail-ai.openapi.enabled`，默认 **false**。类比更**不**等于「登记 classic 就必须画出 Mapper」——这间房没有库表，工牌住在对面楼。

四条河都可能被随口叫「snail-ai」，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `SnailAiController.registerCurrentUser` | admin 进程里 `POST /snail-ai/user/register` | **本课 (a)** |
| `OpenApiUserClient.register` | 厂商客户端，打对面 `/openapi/v1/user/register` | 本课机制（电话），不是第二扇我们的 HTTP |
| `SnailAiConfig` | `wta-common-ai`；`snail-ai.enabled=true` 才 `@EnableSnailAiAgent` + `@EnableSnailAiOpenApi` | 对照电闸，不是本窗 |
| `wta-snailai-server` / `SnailAiServerApplication` | 另一 JVM；HTTP 8900 + gRPC 18888 | 对照楼，厂商内部 deferred |
| 厨房 `createAiService.registerCurrentSnailUser` | `POST /snail-ai/user/register` + `repeatSubmit: false` | L-081 对照 |
| 菜单 `ai/chat/index` / `AiChatPage` | 嵌入聊天页，先调本窗再拼 iframe | L-081 |
| 菜单 `monitor/snailai/index` | 外置监控玻璃 `snail-ai` | L-024 |
| nginx `/snail-ai/` | LB 保留前缀 → snailai-server:8900 | 部署对照，不是本窗 |

2026-09-17 工作树：`wta-ai` 的 Java 树只有 `org.namewta.ai.controller.SnailAiController`。POM 依赖 `wta-common-core`、`wta-api`、`wta-common-ai`、`wta-common-satoken`、`wta-common-web`。`wta-common-ai` 再拉三份厂商 starter：`snail-ai-agent-chat-starter` / `snail-ai-agent-executor-starter` / `snail-ai-openapi-starter`（父 POM `${snailai.version}` = **1.1.1**）。

```text
浏览器 / admin-web
  Authorization + clientid
  POST /{env}-api/snail-ai/user/register     ← 本课这一扇
        │  无 body
        v
admin 进程  NamewtaApplication
  SaInterceptor.checkLogin                   ← /snail-ai 不在 excludes
        │
        v  @ConditionalOnProperty snail-ai.open-api.enabled=true
  SnailAiController.registerCurrentUser
        │  LoginHelper → externalId / nickname
        v  OpenApiUserClient（厂商代理）
  HTTP  POST http://{host}:{web-port}/snail-ai/openapi/v1/user/register
        头：Snail-Ai-App-Id / Snail-Ai-Token
        │
        v
另一进程  wta-snailai-server :8900
  context-path /snail-ai
  厂商 OpenAPI 用户窗（幂等：appId + externalId）
        │
        v  Result{status:1, data: OpenApiUserVO}
  R.ok(vo)  code=200
```

往下走不要跳层：

```text
已登录会话（Sa-Token）
    └─ 本课窗口（只换工牌，不聊天）
          └─ 厂商 OpenAPI 客户端（另一信封 Result）
                └─ 对面楼的用户映射（openId）
```

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **这间房只有一扇窗。** 公开方法就叫 `registerCurrentUser`。路径 `POST /snail-ai/user/register`。
2. **窗上写着「当前用户」。** 不要自己填别人的 id。Token 里是谁，就给谁换工牌。
3. **酒店房卡换对面工牌。** `userId` → `externalId` 字符串；`nickname` → `nickname`。头像字段磁盘上**没填**。
4. **对面成功数字是 1，我们成功数字是 200。** 先看 `Result.status`，再包 `R`。
5. **同一人再按一次，应该还是那枚工牌。** 厂商按 `appId + externalId` 幂等；`created` 第一次 true，以后 false。本课柜台两边都原样交回。
6. **没登录进不了这扇窗。** 拦截器先 `checkLogin`。真进了方法却拿不到人，才抛「当前登录用户为空」。
7. **抛出去的中文，全局处理器多半不会原样给你。** `SnailAiException` 走未知运行时异常席。
8. **三把（其实四把）钥匙不是一扇窗。** 类在不在 jar 是 bundle；窗在不在 Spring 是 `snail-ai.open-api.enabled`；电话在不在是 `snail-ai.openapi.enabled`；Agent/gRPC 是 `snail-ai.enabled`。
9. **`open-api` 和 `openapi` 差一个连字符。** 我们的 Controller 和 dev/prod yml 用带连字符的；厂商自动配置用**不带**的。口试要能指。
10. **classic 但没有 Mapper 链。** 不要在这间房找 `ISnailAiService`。新的碰库 AI 业务不要塞进来当阁楼。
11. **聊天页会按铃，监控玻璃不会。** `ai/chat/index` 会 POST 本窗。`monitor/snailai/index` 看的是对面楼大厅。
12. **local 配置几乎把窗关死。** `application-local.yml` 只有 `snail-ai.enabled: false`，没有 `open-api` 段。

### 精确定义与 English term

| 中文说法 | English term | 磁盘落点 |
| --- | --- | --- |
| 当前用户注册窗 | current-user OpenAPI registration | `SnailAiController.registerCurrentUser` → `POST /snail-ai/user/register` |
| 外部用户标识 | external id | `OpenApiUserRegisterRequest.externalId` = `String.valueOf(userId)` |
| 开放用户标识 | open id | `OpenApiUserVO.openId`；嵌入页后来拿它拼 iframe |
| 是否本次新建 | created flag | `OpenApiUserVO.created`；厂商幂等已存在则为 false |
| 条件装配 | conditional on property | 类上 `snail-ai.open-api.enabled=true` |
| 厂商客户端 | OpenAPI user client | `com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient` |
| 厂商成功码 | vendor result status | `Result.status`；1 成功 0 失败；本课常量 `SNAIL_AI_SUCCESS = 1` |
| 平台信封 | platform envelope | `org.namewta.common.core.domain.R`；成功 `code=200` |
| 登录助手 | login helper | `LoginHelper.getUserId()` / `getLoginUser()` |
| 幂等登记 | idempotent register | 厂商按应用 + `externalId` 命中则直接返回 |
| 应用凭证头 | app id / token headers | 客户端请求头 `Snail-Ai-App-Id`、`Snail-Ai-Token`，值来自 `snail-ai.app-id` / `snail-ai.token` |

不要把本课的 **Snail AI OpenAPI 用户**说成 L-030…L-033 那套 **NAMEWTA 机器调用 OpenAPI**（签名、凭据、`/system/openapi`）。两边都写 OpenAPI，楼不一样。

### 机制/因果链

按一次真实 POST 实际发生的顺序走。

#### 1. 类进不进进程

`wta-admin/pom.xml` 默认 profile `bundle-full`（`activeByDefault=true`）插入 `wta-ai`。显式 `-Pbundle-core` **不插** job / ai / demo / workflow。core 包里没有这份 Controller 类，Spring 扫描 `org.namewta` 也扫不到它。装配细节对照 L-002；本课只要求你能说：**窗在不在，先问 jar 带没带这间房。**

`wta-common-ai` 的 POM 只被 `wta-ai` 依赖（BOM 只锁版本）。core 包通常连 `SnailAiConfig` 都没有。

#### 2. 窗进不进 Spring

```text
@RestController
@RequestMapping("/snail-ai")
@ConditionalOnProperty(prefix = "snail-ai.open-api", name = "enabled", havingValue = "true")
public class SnailAiController extends BaseController
```

`matchIfMissing` 默认 false：缺属性 = 不装配。没有 `@SaCheckPermission`、没有 `@SaIgnore`、没有 `@Log`、没有 `@RepeatSubmit`、没有 `@DataPermission`。类上 `@Validated` 对这扇窗几乎没活——方法**没有**入参。

2026-09-17 三份 admin 配置：

| 文件 | `snail-ai.enabled` | `snail-ai.open-api.enabled` | `snail-ai.openapi.enabled` |
| --- | --- | --- | --- |
| `application-local.yml` | `false` | **未写** | **未写** |
| `application-dev.yml` | `false` | `true`（段名 `open-api`） | **未写** |
| `application-prod.yml` | `false` | `true`（段名 `open-api`） | **未写** |

local：窗不进容器。dev/prod：窗上的钥匙是 **true**。

#### 3. 电话进不进 Spring（和窗不是一把钥匙）

`wta-common-ai` 的 `SnailAiConfig`：

```text
@ConditionalOnProperty(prefix = "snail-ai", name = "enabled", havingValue = "true")
@EnableSnailAiAgent
@EnableSnailAiOpenApi
```

三份 yml 的 `snail-ai.enabled` 都是 **false**，所以这份配置**不进**。`@EnableSnailAiOpenApi` 只是 `@Import(SnailAiOpenApiAutoConfiguration)`。

厂商 starter **另外**在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记了同一份 `SnailAiOpenApiAutoConfiguration`。它自己还贴着：

```text
@ConditionalOnProperty(prefix = "snail-ai.openapi", name = "enabled", havingValue = "true", matchIfMissing = false)
```

属性类前缀也是 **`snail-ai.openapi`**（无连字符），`enabled` 默认 **false**，`webPort` 默认 **8080**（不是 8900）。

Spring 把 `open-api` 绑成 `openApi`，把 `openapi` 当成一个词。口试要能指：**Controller 听 `snail-ai.open-api.enabled`；厂商客户端听 `snail-ai.openapi.enabled`。dev/prod yml 只写了带连字符的那把。** 本课**未实跑** `NamewtaApplication` 启动。按注解，full + dev 可能出现「窗要进、电话 Bean 不在、构造器注入失败」；不要把 yml 注释「启用 OpenAPI Client」当成已经接通的证据。

`OpenApiUserClient` 是接口。自动配置用 `RequestBuilder.createProxy` 做成 JDK 动态代理。本课柜台只调 `register`，不调同接口上的 `getUser`。

#### 4. 登录门卫

`SecurityConfig` 对未排除路径 `StpUtil.checkLogin()`，再核对 header/param 的 `clientid` 与 Token extra。`application.yml` 的 excludes 含 `/snail-chat/**`、`/api/snail/chat/**`，**不含** `/snail-ai/**`。没带合法 Admin-Token 的请求到不了方法体。OpenAPI 合同给了 401 槽，和这道门卫一致。

`LoginHelper.getLoginUser()` 内部吞掉 `NotLoginException` 返回 null。`getUserId()` 经 `getExtra`，异常也变 null。方法体因此写成：

```text
if (loginUser == null || userId == null) {
    throw new SnailAiException("当前登录用户为空");
}
```

正常登录路径上，这一支是「会话坏了 / extra 丢了」的补刀，不是匿名访问的主路。匿名主路在拦截器。

#### 5. 填表、打电话、翻译信封

`ensureOpenApiUser`（private）：

1. `new OpenApiUserRegisterRequest()`
2. `setExternalId(String.valueOf(userId))` —— 数字用户号变字符串，没有前缀
3. `setNickname(loginUser.getNickname())` —— 用昵称，**不是** `username`；`avatarUrl` 不设
4. `userClient.register(registerRequest)` → 厂商 `Result<OpenApiUserVO>`
5. `registerResult == null` → 「注册 OpenAPI 用户失败，返回为空」
6. `getStatus() != 1` → `throw new SnailAiException(registerResult.getMessage())`
7. `getData() == null` → 又一句「注册 OpenAPI 用户失败，返回为空」
8. 否则把 `OpenApiUserVO` 交回；公开方法 `return R.ok(ensureOpenApiUser())`

代理侧 URL 形状（厂商 1.1.1）：

```text
{http|https}://{host}:{web-port}/{prefix}{/openapi/v1/user/register}
```

host 优先 `snail-ai.openapi.server-host`，否则 `snail-ai.server.host`（dev/prod 写了 `127.0.0.1`）。prefix 默认 `snail-ai`。完整路径常量 `OPEN_API_USER_REGISTER = "/openapi/v1/user/register"`。请求头带 `Snail-Ai-App-Id` / `Snail-Ai-Token`（`snail-ai.app-id` / `snail-ai.token`；dev/prod 种子 `app-id: 1`、`token: SAI_566a6bfbc26e4998b4841cc927d50c5d`）。HTTP ≥400 时代理自己抛 `SnailAiException("OpenAPI request failed: HTTP …")`。

对面楼（厂商 `OpenApiUserService.register`，**不**标本格 covered）：同一 `appId` 下 `externalId` 已存在则幂等返回，`created=false`；否则新建平台用户 + 映射，`openId` 为去横线 UUID，`created=true`。本课只要你能说：**我们每次都喊 register，不先 getUser；是否新建看返回的 `created`。**

`OpenApiUserVO` 字段：`openId` / `externalId` / `nickname` / `avatarUrl` / `created`。生成合同 `ROpenApiUserVO` 包一层我们的 `code/msg/data/error`。厨房 `projectAiUserTransport` 会丢掉 `avatarUrl`——那是 L-081 的边界，本课只提醒：后端 VO 有这个字段，本窗没填它。

#### 6. 失败信封落到哪一席

`SnailAiException extends BaseSnailAiException extends RuntimeException`。`GlobalExceptionHandler` 有 `ServiceException` / `BaseException` 专席，**没有** `SnailAiException`。于是：

- 空用户 / 空 Result / status≠1 / 空 data / 代理 HTTP 失败 → 方法内或代理抛厂商异常
- `@ExceptionHandler(RuntimeException.class)` + `@ResponseStatus(500)` → `R.fail("发生未知异常，请联系管理员 [错误编号: " + 8 位数字 + "]")`

口试不要说「前端能看到『当前登录用户为空』」。那句写在 throw 里，默认回执里看不到。

### 图、表或文本图

**图题 / caption：** 宏观一扇窗与四把钥匙。alt：浏览器只打 admin 的 POST；admin 再打对面 OpenAPI；四把钥匙分别管 jar、窗、电话、Agent。

```text
钥匙 A  bundle-full 插入 wta-ai          类在不在 fat jar
钥匙 B  snail-ai.open-api.enabled=true   本课窗口进不进 Spring
钥匙 C  snail-ai.openapi.enabled=true    OpenApiUserClient 进不进 Spring（厂商前缀）
钥匙 D  snail-ai.enabled=true            SnailAiConfig：Agent gRPC + @EnableSnailAiOpenApi

浏览器 --POST /{env}-api/snail-ai/user/register--> 本课窗口
本课窗口 --POST /snail-ai/openapi/v1/user/register--> 对面楼:8900
LB /snail-ai/ -------------------------------------> 对面楼大厅（不是本窗）
浏览器 iframe monitor/snailai -----------------------> L-024 玻璃
浏览器 ai/chat/index --先按本窗铃，再开 /snail-chat/--> L-081
```

**文字等价物：** 人在管理端点「AI会话」时，真正打到 NAMEWTA 的只有这一扇 POST。对面楼的管理后台走 nginx 保留前缀 `/snail-ai/`，和本窗不是一条 HTTP。四把钥匙分开说：没进 jar、没开窗、没装电话、没开 Agent，症状不一样。

**图题 / caption：** 这一扇的方法性状。alt：单行表列出 HTTP、权限、入参、出参、失败。

| 方法 | HTTP | 权限注解 | 入参 | 成功信封 | 失败 |
| --- | --- | --- | --- | --- | --- |
| `registerCurrentUser` | `POST /snail-ai/user/register` | 无；须登录 | 无 body；身份来自 `LoginHelper` | `R<OpenApiUserVO>`，`code=200` | `SnailAiException` → 全局 500 未知异常席 |

**文字等价物：** 口试按这张表念：一个 POST，没有权限字，没有 JSON 入参，成功用我们的 `R`，失败不要指望中文原句。

**图题 / caption：** 两套信封对照。alt：左列厂商 Result；右列 NAMEWTA R。

```text
厂商 Result          我们的 R
status  1 / 0        code   200 / 500…
message              msg
data    OpenApiUserVO data  同一份 VO
                     error  可选
```

**文字等价物：** 控制器是翻译官。它不把 `Result` 原样交给浏览器。`status != 1` 被翻译成抛异常，而不是 `R.fail(message)`。

### 正例、反例与边界

**正例 1 — 指入口。** 打开 `wta-ai/src/main/java`，数到只有 `SnailAiController.java`。指出唯一 `@PostMapping("/user/register")` 和 `registerCurrentUser`。OBJ-80 这一句就成立。

**正例 2 — 指身份字段。** 手指点 `setExternalId(String.valueOf(userId))` 和 `setNickname(loginUser.getNickname())`。不要说成用户名，不要发明请求体。

**正例 3 — 第一次换牌。** 对面没有这名 `externalId`。返回 `created=true`，新 `openId`。`R.ok` 把整份 VO 放进 `data`。

**正例 4 — 再按一次。** 同一登录用户再 POST。厂商命中映射，`created=false`，`openId` 仍是那一串。本课没有「先查再注册」的第二扇窗。

**正例 5 — 指成功数字。** 打开常量 `SNAIL_AI_SUCCESS = 1`，再打开 `R.ok` 走 `HttpStatus.SUCCESS = 200`。口试把两个数字都说出来。

**正例 6 — 指排除名单。** 打开 `application.yml` 的 `security.excludes`，圈 `/snail-chat/**`，确认没有 `/snail-ai/**`。再打开 Controller，确认没有 `@SaIgnore`。

**正例 7 — 指菜单对照。** `50-cde-base-dml.sql`：一级「AI会话」`ai/chat/index` 权限字段 `''`；监控下「AI控制台」`monitor/snailai/index` 权限 `monitor:snailai:list`。前者会按本窗的铃；后者是玻璃。

**正例 8 — 指厨房对照（不盖章）。** `createAiService` 只发 `{ url: '/snail-ai/user/register', method: 'post', headers: { repeatSubmit: false } }`。没有 body。这证明浏览器扣的就是这一扇。L-081 才覆盖工厂。

**正例 9 — local 关窗。** `application-local.yml` 只有 `snail-ai.enabled: false`。按类上条件，local 默认没有这扇映射。不要说成「local 也能换牌」。

**正例 10 — core 包没有这间房。** `-Pbundle-core` 的 admin 依赖列表没有 `wta-ai`。类不在 jar，条件注解没有机会生效。

**反例 1 — 「`wta-ai` 有一套 AI CRUD。」** 只有注册。没有对话 HTTP。

**反例 2 — 「这就是 Snail AI 服务器。」** 服务器是 `wta-extend/wta-snailai-server`。本课在 admin 里。

**反例 3 — 「`snail-ai.enabled: false` 所以这扇窗一定不在。」** 窗听的是 `open-api.enabled`。Agent 总闸是另一把。

**反例 4 — 「yml 已经启用 OpenAPI Client。」** 注释写在 `open-api` 段下；厂商 Bean 听 `openapi`。不要把注释当接通证据。

**反例 5 — 「失败时浏览器显示控制器里的中文。」** 默认全局席会换成带错误编号的未知异常。

**反例 6 — 「`status=1` 就是 HTTP 200，所以可以不检查 data。」** 代码三条都查：null Result、status、null data。

**反例 7 — 「classic 必须有 ServiceImpl。」** 登记的是模块模式。这间房的家具是薄封装。

**反例 8 — 「没有 Mapper 就不是 classic，可以改 layered 当日常。」** 登记表有这一行。新独立业务另开模块。

**反例 9 — 「监控 AI 控制台会调用 `registerCurrentUser`。」** 那是 iframe 对面楼；权限字都不是同一套。

**反例 10 — 「nginx `/snail-ai/` 会进这个 Controller。」** LB 把 `/snail-ai/` 指到 snailai-server:8900。本窗挂在 admin 的 `/{env}-api` 后面。

**反例 11 — 「可以在 body 里帮同事注册。」** 合同无 body。谁登录换谁的牌。

**反例 12 — 「本课覆盖 `createAiService`。」** OBJ-81 / L-081。

**反例 13 — 「`OpenApiUserClient.getUser` 也是本窗。」** 接口有这个方法；本课 Java **零调用**。

**反例 14 — 「`@Validated` 会校验昵称非空。」** 方法无入参，Request 对象是方法内 new 的，不走 MVC 校验。

**反例 15 — 「厂商 `Result` 会原样给前端。」** 前端厨房按 `code/data` 的 `R` 形状测。

**边界 1 — 前缀连字符。** 本课把 `open-api` ≠ `openapi` 当作必须能指的磁盘事实。未实跑启动；若 Nacos 另有覆盖，以运行时 Environment 为准，先打开当时的配置源再开口。

**边界 2 — 厂商版本。** 父 POM `snailai.version=1.1.1`，与 GitHub `aizuda/snail-ai` 标签 v1.1.1 一致（2026-09-17）。引擎表结构、密码盐、RoleEnum 仍出范围。

**边界 3 — 无模块测试。** 证据是生产源码 + 配置 + 生成合同 + 前端对照。不要假装有 `SnailAiControllerTest`。

**边界 4 — classic 脚本退出码 1。** 缺 `src/main/resources`。人看登记表。L-003 已钉。

**边界 5 — `created` 是 boolean 基本类型。** 生成 TypeScript 是 `created?: boolean`。厨房允许缺省。

**边界 6 — 昵称为 null。** 磁盘照样 set。对面是否接受空昵称属厂商；本课不发明校验。

**边界 7 — 头像。** Request 有 `avatarUrl`，本窗不设。已存在用户若对面存过头像，VO 可能带回来；本窗不保证。

**边界 8 — 机器 OpenAPI 登录。** 门卫对已验证的 openapi 机器身份跳过 clientid 核对。本窗仍会用那条会话的 `userId` 去换牌。本课不把机器调用当主路径。

**边界 9 — RepeatSubmit。** Java 没贴注解。厨房 header `repeatSubmit: false` 是浏览器侧防抖开关，不是后端幂等。后端幂等在对面 `externalId`。

**边界 10 — 包名 `execption`。** 少一个 e。搜 `exception` 会漏。这是厂商拼写，不要「帮它改」当本课作业。

## 变式与迁移

1. **和 L-002 对照。** 变式里 ai 只在 full 进 admin，extend 里另有 `SnailAiServerApplication`。本课把「薄封装」展开成能指的一扇 HTTP。不要把「模块没进 bundle」和「对面楼没启动」说成一件事。
2. **和 L-003 对照。** classic 列不是一种家具。demo/system/workflow 有 ServiceImpl→Mapper。ai 没有这条链，**仍是 classic 行**。脚本缺 resources 不能拿来改登记。
3. **和 L-078 对照。** job 同样瘦、同样 classic，但 **零扇 HTTP**。ai **有**这一扇。不要把 job 的「入口是类」说成 ai 也没有 Controller。
4. **和 L-024 对照。** 外置键 `snail-ai` 是监控玻璃，权限 `monitor:snailai:list`。本窗没有权限字。
5. **和 L-030…L-033 对照。** 系统 OpenAPI 是 NAMEWTA 给机器调用自己的签名协议。本课 OpenAPI 是 Snail AI 厂商给外部应用换用户的那套头 `Snail-Ai-App-Id`。口试先说哪栋楼。
6. **和 L-081 对照。** 厨房只有这一枪；页面用返回的 `openId` 拼 `/snail-chat/?openId=&trustedCredential=`。本课到 `R.ok(vo)` 为止。
7. **迁移：真要加对话/智能体 HTTP。** 先问是不是独立新业务。是：新模块默认 layered，另登记，不要在 `SnailAiController` 上继续堆厂商客户端调用（classic 入口继续膨胀不是存量保护）。只是再薄包一扇：可以加映射，但钥匙、信封翻译、登录门卫要按本课同一套口吻说清。
8. **迁移：想在 local 换牌。** 至少要对上**窗的前缀** `snail-ai.open-api.enabled`，以及厂商电话前缀 `snail-ai.openapi.enabled`，再保证对面进程、`app-id`/`token`、host/port。缺一把就不要说「代码写了所以能通」。

## 常见误区

1. **「没看到 Service 就是模块坏了。」** 这间房的公开入口就是这一扇 Controller。
2. **「classic 必须有 Mapper。」** 登记的是兼容窗口，不是家具清单。
3. **「`/snail-ai` 三条河是同一条。」** admin 窗、nginx 保留前缀、对面 context-path，主机和进程都可能不同。
4. **「成功码 1 和 200 谁方便用谁。」** 检查厂商用 1；回浏览器用 200。
5. **「把 `Result` 直接 return。」** 返回类型是 `R<OpenApiUserVO>`。
6. **「失败 `R.fail(registerResult.getMessage())`。」** 磁盘是 throw。
7. **「匿名可注册，因为没权限字。」** 没权限字 ≠ 没登录。
8. **「local 和 dev 行为一样。」** local 没写 `open-api` 段。
9. **「`open-api` 写了等于客户端 Bean 在。」** 厂商前缀少了连字符。
10. **「iframe 任务调度/AI 控制台会打这扇窗。」** 玻璃连的是对面后台 URL。
11. **「本课讲完就等于 ai 模块全覆盖。」** 前端是 L-081。厂商引擎内部保持 deferred。
12. **「可以 import `SysUserMapper` 补头像。」** 本课没有这条链；跨房间要走 `wta-api`。现有代码不补头像。
13. **「`ensureOpenApiUser` 也是一扇 HTTP。」** 它是 private 帮手。
14. **「OBJ-80 要口述 chat SSE。」** OBJ-80 是注册窗。没有流。
15. **「菜单空权限等于任何人。」** 还要先有这份菜单路由，并且过登录与 Client 核对。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `backend/wta-modules/wta-ai/src/main/java/org/namewta/ai/controller/SnailAiController.java`。数公开映射方法。圈 `@RequestMapping("/snail-ai")`、`@PostMapping("/user/register")`、`SNAIL_AI_SUCCESS`、三处 `SnailAiException`、`userClient.register`。
2. 打开 `wta-ai/pom.xml` 与模块目录。确认五份依赖、没有 `src/main/resources`、没有 `src/test`、没有 `service`/`mapper`。
3. 打开 `wta-common-ai/.../SnailAiConfig.java`。圈 `snail-ai.enabled` 与两枚 `@Enable*`。打开 `application-local.yml` / `application-dev.yml` 的 `snail-ai` 段。圈 `enabled: false`、dev 的 `open-api.enabled: true`、以及**有没有** `openapi.enabled`。
4. 打开 `application.yml` 的 `security.excludes`。圈 `/snail-chat/**`。打开 `SecurityConfig` 回想 `checkLogin`。打开 `GlobalExceptionHandler` 的 `RuntimeException` 席，对照本课有没有专席。
5. 打开生成合同 `frontend/packages/api-contracts/generated/openapi.ts` 的 `"/snail-ai/user/register"` 与 `OpenApiUserVO`。圈 `requestBody?: never`、`ROpenApiUserVO`。打开 `50-cde-base-dml.sql` 圈「AI会话」与「AI控制台」两行。不要改这些文件。

## 总结、词汇表与下一步

- **宏观一扇注册窗：** `wta-ai` 的公开 HTTP 只有 `SnailAiController.registerCurrentUser` = `POST /snail-ai/user/register`。没有 Mapper，没有聊天接口。
- **(a) 换工牌：** 当前 `userId`/`nickname` → 厂商 `externalId`/`nickname` → `OpenApiUserVO`（`openId` + `created`）→ `R.ok`。
- **两套信封。** 厂商 `Result.status==1`；我们 `R.code==200`。失败默认变成全局未知异常，不回控制器中文。
- **钥匙分开说。** jar（bundle-full）≠ 窗（`open-api.enabled`）≠ 电话（`openapi.enabled`）≠ Agent（`snail-ai.enabled`）。连字符是考点。
- **classic 仍成立。** 没有 ServiceImpl 不是逃出登记表的借口；新的碰库/对话业务不要塞进这一扇窗。

词汇表：`wta-ai` / `SnailAiController` / `registerCurrentUser` / `ensureOpenApiUser` / `OpenApiUserClient` / `OpenApiUserRegisterRequest` / `OpenApiUserVO` / `externalId` / `openId` / `created` / `Result` / `SNAIL_AI_SUCCESS` / `R` / `SnailAiException` / `execption` / `LoginHelper` / `snail-ai.open-api.enabled` / `snail-ai.openapi.enabled` / `snail-ai.enabled` / `SnailAiConfig` / `bundle-full` / `wta-snailai-server` / `/snail-chat` / classic。

下一步：L-081 才覆盖 `createAiService` 与 `createAiWebDomain`（嵌入页如何按铃、如何拿 `openId` 拼 iframe）。不要把厨房和页面说成本课已经 covered。监控玻璃保持 L-024。厂商引擎内部保持 deferred。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` | 业务模块公开入口；本课入口是 `wta-ai` 唯一 Controller | `SnailAiController` | 2026-09-17 |
| S-007 | `01-module-map.md`；`wta-modules/pom.xml`；`wta-admin/pom.xml` | `wta-ai` 是 AI 业务能力；full 才插入 admin | 模块地图 ai 行；`<module>wta-ai`；profile `bundle-full` | 2026-09-17 |
| S-008 | `03-backend-module-modes.md` | `wta-ai` = classic；第三方 starter 适配遵守模块边界 | 登记表 ai 行 | 2026-09-17 |
| S-010 | `50-cde-base-dml.sql` | 菜单「AI会话」`ai/chat/index` 权限空串；「AI控制台」`monitor:snailai:list` | 约第 42、69 行 | 2026-09-17 |
| S-L002-01 | 子课 L-002 | 薄封装；full 插 ai；extend 另有 `SnailAiServerApplication` | `lessons/L-002-backend-assembly.md` | 2026-09-17 |
| S-L003-01 | 子课 L-003 | classic 列不是一种家具；ai 无 ServiceImpl/Mapper；脚本退出码 1 | `lessons/L-003-layered-vs-classic.md` | 2026-09-17 |
| S-L080-01 | `SnailAiController.java` | 唯一 POST；条件前缀 `snail-ai.open-api`；LoginHelper；status==1；三处异常 | `org.namewta.ai.controller` | 2026-09-17 |
| S-L080-02 | `wta-ai/pom.xml`；模块目录 | 五依赖；无 resources；无 test；无 Service/Mapper | artifact `wta-ai` | 2026-09-17 |
| S-L080-03 | `SnailAiConfig.java`；`wta-common-ai/pom.xml` | 总闸 `snail-ai.enabled`；三份厂商 starter；`${snailai.version}=1.1.1` | `org.namewta.common.ai.config`；父 POM | 2026-09-17 |
| S-L080-04 | `application-{local,dev,prod}.yml`；`application.yml` excludes | local 关总闸且无 open-api 段；dev/prod `open-api.enabled: true`；排除 `/snail-chat/**` 不含 `/snail-ai/**` | admin 配置 | 2026-09-17 |
| S-L080-05 | `LoginHelper.java`；`SecurityConfig.java`；`GlobalExceptionHandler.java`；`R.java` | 登录可空；checkLogin；RuntimeException 席；成功码 200 | satoken / security / web / core | 2026-09-17 |
| S-L080-06 | 生成合同 `openapi.ts`；`OpenApiUserVO` / `registerCurrentUser` | 无 requestBody；`ROpenApiUserVO`；401 槽 | `frontend/packages/api-contracts/generated/openapi.ts` | 2026-09-17 |
| S-L080-07 | 厂商 v1.1.1：`OpenApiUserClient`；`SnailAiOpenApiAutoConfiguration`；`SnailAiOpenApiProperties`；`Result`；`OpenApiPathConstants`；`OpenApiHttpInvokeHandler` | 客户端前缀 `snail-ai.openapi`；路径 `/openapi/v1/user/register`；头 App-Id/Token；status 1/0 | GitHub `aizuda/snail-ai` 与 POM 版本一致 | 2026-09-17 |
| S-L080-08 | 厂商 `OpenApiUserService.register`（对照，非本格 covered） | 按 appId+externalId 幂等；`created` 含义 | snail-ai-server-openapi | 2026-09-17 |
| S-L080-09 | 前端对照：`domains/ai/src/index.ts`；`web-domains/ai`；`adminManifestRegistry.ts`；nginx architecture；compose snailai-server | 厨房只打本窗；聊天页/监控玻璃/LB `/snail-ai/` 分流 | L-081 / L-024 对照，不盖章 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：`wta-ai` 是一间只有一扇 HTTP 窗的 classic 房间。公开入口是 `SnailAiController.registerCurrentUser`，路径 `POST /snail-ai/user/register`，没有请求体，没有权限注解，但必须先登录。它从 `LoginHelper` 取出当前用户号和昵称，打给厂商 `OpenApiUserClient.register`，用 `externalId` 在对面楼换 `openId`。厂商成功数字是 1，我们回给浏览器的成功数字是 200。同一用户再来是幂等，看 `created`。失败抛厂商异常，全局处理器多半变成带编号的未知错误。类在不在 jar、窗开不开、电话 Bean 在不在、Agent 开不开，是四道闸；后两道的配置键差一个连字符。浏览器聊天页会按这扇铃，监控 AI 控制台和 nginx `/snail-ai/` 看的是对面那栋楼。本课只把这一扇注册窗说完。
