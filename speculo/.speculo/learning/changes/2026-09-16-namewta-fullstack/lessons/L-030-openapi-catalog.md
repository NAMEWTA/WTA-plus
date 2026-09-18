---
lesson_id: L-030
objective_ids: [OBJ-30]
claimed_cells:
  - A:SysOpenApiCatalogController.selfInterfaces,selfInterface,userInterfaces,userInterface
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: four-windows-on-disk
    minutes: 8
  - segment: snapshot-filter-and-examples
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L030-01, S-L030-02, S-L030-03, S-L030-04, S-L030-05, S-L030-06, S-L030-07, S-L030-08]
---

# Lesson 030：宏观货架——自有与他人的 OpenAPI 接口目录

## 学完你能做什么

打开 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/openapi/SysOpenApiCatalogController.java`。这份类**正好四个**公开 HTTP 方法，没有第五个，没有 POST，没有凭据，没有 HMAC。你能**口述这四扇目录窗**：哪两扇看**自己**能调哪些机器接口，哪两扇只许超级管理员看**别人**能调哪些；每扇挂哪颗权限；列表和详情分别把 `userId` 从哪儿取。口试名单就是矩阵 (a) 这一行：

1. **`selfInterfaces`**：`GET /system/openApi/self/interfaces`。目标人 = `LoginHelper.getUserId()`。返回 `List<OpenApiCatalogItem>`。
2. **`selfInterface`**：`GET /system/openApi/self/interfaces/{interfaceId}`。目标人仍是自己。返回一条；看不见就当不存在。
3. **`userInterfaces`**：`GET /system/openApi/users/{userId}/interfaces`。目标人 = 路径上的 `userId`。先 `requireSuperAdmin()`，再列那个人的货架。
4. **`userInterface`**：`GET /system/openApi/users/{userId}/interfaces/{interfaceId}`。目标人 = 路径 `userId`。同样先超级管理员闸，再取一条。

OBJ-30 要你当场说完的那句是：**目录是按目标人的授权快照过滤后的机器接口货架，不读凭据、不写会话、不签发签名。自有两扇用当前登录人；他人两扇路径带别人的 `userId`，权限串过了还要再查一遍调用方是不是超级管理员。**

`wta-system` 在登记表是 **classic**。本课不是 `ISys*Service` 那条老 CRUD 链，也**没有** UseCase、**没有** `BaseController`。磁盘是 `SysOpenApiCatalogController → SystemOpenApiCatalogService → OpenApiOperationRegistry + OpenApiAuthorizationResolver + OpenApiAuthorizationMatcher`。授权快照的 SQL 在 `SysOpenApiAuthorizationMapper.xml`。不要口述成 layered。

本课不宣称你会拆 `SysOpenApiCredentialController` 创建/重置/启停/删除（L-031）、`wta-common-openapi` 网关 HMAC / 机器会话（L-032）、或前端 `createOpenApiService` / `openApi.vue`（L-033）。今天只认：**货架上摆什么、谁有资格来看、看自己和看别人差在哪一扇门。**

## 先把宏观地图放在桌上

L-005 已经把「OpenAPI」这个词钉成**浏览器河上的传输合同**：`packages/api-contracts` 里从管理端 HTTP 快照生成的 TypeScript。本课走进另一条河：**机器调用方**用 NAMEWTA v1 签名头去打真正挂了 `@OpenApi` 的 MVC 方法。两条河都叫 OpenAPI，但货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| 前端 `openapi/current.json` / `generated/openapi.ts` | 管理端浏览器 HTTP 的快照复印件 | 邻居证据：四扇目录窗会出现在这份快照里 |
| `SysOpenApiCatalogController` | 给人看的**机器接口货架** | **本课** |
| `sys_open_api_credential` | 每人一把 appKey/密文 | L-031 |
| `OpenApiGatewayFilter` | 核签名、防重放、建机器会话、再用**同一把** matcher 放行 | L-032 |

2026-09-16 工作树：目录类在 `.../controller/system/openapi/SysOpenApiCatalogController.java`。类注释写的是 *Credential-independent self and super-admin catalog endpoints*。`@RequestMapping("/system/openApi")` 和凭据 Controller **共用门牌**，方法路径不打架。整份类和 `SystemOpenApiCatalogService` 都挂 `@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")`。`application.yml` 默认 `OPENAPI_ENABLED=true`；关掉之后这两颗 Bean 不进容器，四扇窗 404，不是空列表。

```text
已登录的人（浏览器 / 管理端）          机器调用方（HMAC，L-032）
        │                                      │
        ├─ GET /system/openApi/self/interfaces*
        │        权限 system:openApi:self
        │        目标人 = 当前登录 userId
        │
        └─ GET /system/openApi/users/{userId}/interfaces*
                 权限 list / query
                 再 requireSuperAdmin()          不走这四扇
                 目标人 = 路径 userId                    │
                          │                              │
                          v                              v
                 SystemOpenApiCatalogService      OpenApiGatewayFilter
                          │                              │
                          ├─ resolve(targetUserId)  同一 SPI
                          │     读用户 / 合法 Client / 角色 / 菜单权
                          │     合成 LoginUser（userType=openapi，无 Client）
                          ├─ registry.all() / find(interfaceId)
                          │     启动时从真实 MVC + 方法级 @OpenApi 扫出来
                          └─ matcher.matches(快照, accessRule)
                                目录预览和网关放行共用这一把
```

**类比：** 图书馆大厅有一面**按借书证过滤的货架**。货架上不是全馆藏书，只是「这张证现在能借走的书」。每本书脊上印着：书号（`interfaceId`）、书名（`summary`）、怎么填借条（curl / Java 示例）。自己走「我的货架」通道；馆长才能走到**别人**的货架前面看。书号是开馆那天按「真实的门 + 真实的走法」烙出来的，不是谁在本子上手抄的书目。

**类比失效处：**

1. 货架**不是**一张 `sys_open_api_*` 目录表。库里只有凭据表 `sys_open_api_credential`。目录是进程启动时扫 MVC 得到的内存注册表。
2. 馆长去看小明的货架，看见的是**小明**能借的书，不是馆长自己那张 `*:*:*` 全能证上的全馆书。
3. 货架上的借条示例写的是 `https://api.example.com` 和 `<APP_KEY>` / `<NAMEWTA_V1_SIGNATURE>` 占位符，**不是**你的真实密钥，也不是本楼真实主机名。
4. 这四扇窗自己**没有** `@OpenApi`，所以它们不会摆上货架。人用登录会话来看货架；机器用签名头去打货架上的那些门。
5. 书在货架上，不表示你已经有钥匙（凭据）或已经进过门（网关）。目录只回答「按这张证，哪些门 theoretically 会放行」。

## 核心概念与机制

### 直觉讲解

先记住三张纸条，再背路径：

- **目标人 `targetUserId`。** 自有两扇从登录会话取；他人两扇从 URL 取。过滤永远按**目标人**，不按「我此刻菜单里有什么」。
- **接口号 `interfaceId`。** `SHA-256(METHOD + "\n" + path)` 的前 12 字节十六进制，固定 24 个 `0-9a-f`。不是 Java 方法名，不是 `@OpenApi` 的摘要，也不是菜单 path。
- **授权快照。** 每次请求现查库：人还在不在、有没有至少一扇合法 Client、角色并集、菜单权并集。超级管理员目标人会**再贴** `superadmin` 和 `*:*:*`。快照的 `userType` 写成 `"openapi"`，`clientPk` / `clientKey` 为 null——这是全局身份，不是某把 Client 的登录态。

再记住两把锁叠在他人窗上：

- **Sa-Token 权限串。** 自有：`system:openApi:self`。他人列表：`system:openApi:list`。他人详情：`system:openApi:query`。这把锁看的是**调用方**的管理端会话。
- **`requireSuperAdmin()`。** 只看 `LoginHelper.isSuperAdmin()` → `userId == 1761100000000000001L`。有 list/query 权限但不是这个主键，文案是 `OpenAPI target catalog is unavailable`。单测名字就叫 *RechecksAdminServerSide*。

「为什么货架是空的 / 为什么详情 业务异常」先问五句话，不要先怪网关：

1. `openapi.enabled` 是不是 true？假则 Bean 不在，不是空数组。
2. 这枪是 `/self/interfaces` 还是 `/users/{id}/interfaces`？后者调用方是不是超级管理员主键？
3. 目标人在 `sys_user` 里是不是 `status='0'` 且未删？有没有至少一扇合法 Client（启用的 Client × 启用的用户类型 × 该人启用的类型关系）？
4. 那扇业务门上有没有方法级 `@OpenApi`？启动扫描时有没有被算进 `rejectedMappings`？
5. 目标人快照过不过这扇门上抄下来的 `SaCheckPermission` / `SaCheckRole`？看不见和「不存在」是同一句 `OpenAPI interface is unavailable`。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 自有目录列表 | self interfaces | `selfInterfaces` → `GET /system/openApi/self/interfaces` |
| 自有目录详情 | self interface | `selfInterface` → `GET .../self/interfaces/{interfaceId}` |
| 他人目录列表 | target-user interfaces | `userInterfaces` → `GET /system/openApi/users/{userId}/interfaces` |
| 他人目录详情 | target-user interface | `userInterface` → `GET .../users/{userId}/interfaces/{interfaceId}` |
| 机器接口货架项 | catalog item | `OpenApiCatalogItem` record（common-openapi） |
| 接口号 | interface identity | 24 hex；`OpenApiOperationRegistry.interfaceId` |
| 操作定义 | operation definition | `OpenApiOperationDefinition`：启动时不可变 |
| 显式曝光 | `@OpenApi` | 只允许打在**方法**上；`value()` 是给人看的 summary |
| 访问规则 | access rule | 从类/方法上的 `SaCheckPermission` / `SaCheckRole` 抄来 |
| 授权快照 | authorization snapshot | `OpenApiAuthorizationResolver.resolve(userId)` → `LoginUser` |
| 合法 Client | legal client | `countLegalClients`：启用 Client + 启用用户类型 + 该人类型关系 |
| 匹配器 | authorization matcher | `OpenApiAuthorizationMatcher`；目录和网关各持同一类 |
| 超级管理员闸 | super-admin recheck | `requireSuperAdmin()`；主键 `1761100000000000001L` |
| 凭据无关 | credential-independent | 四扇窗零次读 `sys_open_api_credential` |
| 开关属性 | `openapi.enabled` | Controller、Service、`OpenApiAutoConfiguration` 三道门 |
| 经典分层 | classic | 登记表 `wta-system`；本切片无 UseCase |
| 占位示例 | curl / Java example | 服务端拼出来；含 `<APP_KEY>` 等，不含 secret |

权限串不要混：自有两扇都是 `system:openApi:self`。他人**列表**是 `list`，他人**详情**是 `query`。不要把 OSS 那种「getInfo 也挂 list」搬过来。菜单种子在 `50-cde-base-dml.sql`：`system:openApi:list|query|add|edit|remove|self`。`add/edit/remove` 是凭据写窗的，本课四扇用不到。

HTTP 合同（API-005）：查询 GET，变更 POST。本 Controller **零**个 `@PostMapping` / `@PutMapping` / `@DeleteMapping`，因此也**零**个 `@Log`、**零**个 `@RepeatSubmit`。前端 `createOpenApiService` 把自有列表/详情映成 `currentUser.listInterfaces` / `getInterface`，他人映成 `targetUser.listInterfaces` / `getInterface`，动词都是 `get`；`interfaceId` 走 `encodeURIComponent`。

### 机制/因果链

#### 1. 四扇窗在磁盘上的真实映射

文件：

- `.../controller/system/openapi/SysOpenApiCatalogController.java`
- `.../openapi/catalog/SystemOpenApiCatalogService.java`

类注解：`@RestController` `@RequiredArgsConstructor` `@ConditionalOnProperty(openapi.enabled=true)` `@RequestMapping("/system/openApi")`。只注入 `SystemOpenApiCatalogService`。**不** `extends BaseController`。

| HTTP | 动词 | Java | 调用方权限 | 目标人 | 写库？ |
| --- | --- | --- | --- | --- | --- |
| `/system/openApi/self/interfaces` | GET | `selfInterfaces` | `system:openApi:self` | `LoginHelper.getUserId()` | 否 |
| `/system/openApi/self/interfaces/{interfaceId}` | GET | `selfInterface` | `system:openApi:self` | 同上 | 否 |
| `/system/openApi/users/{userId}/interfaces` | GET | `userInterfaces` | `system:openApi:list` + 超级管理员主键 | 路径 `userId` | 否 |
| `/system/openApi/users/{userId}/interfaces/{interfaceId}` | GET | `userInterface` | `system:openApi:query` + 超级管理员主键 | 路径 `userId` | 否 |

四扇最后都进同一对服务方法：`list(targetUserId)` / `detail(targetUserId, interfaceId)`。Controller 不再做过滤。他人两扇在进服务前调用同一个私有方法 `requireSuperAdmin()`；单测对非超管只打了 `userInterfaces(9L)`，`userInterface` 走同一句，磁盘上没有第二条实现。

#### 2. 列表：先拍快照，再扫货架

`SystemOpenApiCatalogService.list` 按磁盘顺序：

1. `LoginUser target = authorizationResolver.resolve(targetUserId)`。人没有、停用、或 `countLegalClients==0` → `ServiceException("OpenAPI authorization snapshot is unavailable")`。`userId==null` 同样这句。
2. `registry.all().stream()`。注册表在启动 `afterPropertiesSet` 里建成，之后只读。排序键是 path，再 method。
3. `.filter(op -> authorizationMatcher.matches(target, op.accessRule()))`。过不去的书**直接不出现**，不是标灰。
4. `.map(toCatalogItem)`。补 curl / Java 示例。
5. `.toList()`。不可变列表语义由 stream 终端操作给出；注册表自己的 `all()` 返回 `List.copyOf`，往里面 add 会 `UnsupportedOperationException`。

单测 `filtersOnlyByTargetAuthorizationWithoutCredentialOrSessionState` 钉死：目标人只有 `orders:read` 时，货架上只有 `allowed`，没有 `denied`；三次 `list/detail` 都 `resolve(41L)`，**不**碰凭据，**不**碰会话 store。

#### 3. 详情：找不到和没权限说同一句话

`detail`：

1. 同样先 `resolve(targetUserId)`。快照失败时，**还没**去 `registry.find`。
2. `OpenApiOperationDefinition operation = registry.find(interfaceId)`。没有这个号，或 matcher 不过 → **同一句** `OpenAPI interface is unavailable`。
3. 过了才 `toCatalogItem`。

口试要主动说：详情不会告诉你「这个号其实在全馆书目里，只是你没权」。枚举攻击和「书号写错」对外一个样。

#### 4. 授权快照到底并了什么

实现类 `SystemOpenApiAuthorizationResolver`（wta-system，SPI 在 common）。SQL 全在 `SysOpenApiAuthorizationMapper.xml`，**不是** `BaseMapperPlus`。

合法 Client 片段 `legalClientJoin`：

- `sys_client`：`status='0'` 且 `del_flag='0'`
- 该 Client 的 `user_type_id` 对应 `sys_user_type` 启用且未删
- `sys_user_type_rel` 把**这个** `userId` 和该类型连上，关系 `status='0'`

角色：在合法 Client 上，启用未删的 `sys_role`，且（该角色是 Client 的 `default_role_id` **或** `sys_user_role` 显式授了）。权限：这些角色再经 `sys_role_menu` → `sys_menu.perms`，菜单必须 `status='0'` 且 `client_id` 对得上角色的 Client。空 perms 丢掉。

合成后的 `LoginUser`：

- `userType = "openapi"`（常量 `MACHINE_USER_TYPE`）
- `clientPk` / `clientKey` = null（测试名：*without Client fallback*）
- `rolePermission` / `menuPermission` 不可变 `LinkedHashSet`
- `dataScopeRoleMap`：每个权限对应哪些 `roleId`（目录 matcher **不读**这张图；给以后机器会话里的数据范围用）
- `posts` 空列表
- 若 `userId` 等于超级管理员主键：再 `add` `superadmin` 和 `*:*:*`

matcher 用 `SaStrategy.instance.hasElement`，所以目标人快照带 `*:*:*` 时，带权限串的门会过。馆长看**自己**的自有货架，通常几乎是全注册表；馆长看小明，仍按小明的并集滤。

#### 5. 货架从哪来：真实 MVC，不是手抄书目

`OpenApiOperationRegistry` 在 `openapi.enabled=true` 时由 `OpenApiAutoConfiguration` 注册。启动扫描 `RequestMappingHandlerMapping.getHandlerMethods()`：

- 方法上**没有** `@OpenApi` → 跳过（目录窗自己就是这种）
- 有注解：按该 mapping 的每个 path × 每个 HTTP method 生成一条；`interfaceId = hex(SHA-256(method + "\n" + path)[0..12])`
- 同一 `interfaceId` 撞车、缺 path、缺 method、类/方法上有 `SaIgnore` / `SaCheckOr`、非默认 login type → 抛运行时异常，**计入** `rejectedMappings`，这条 handler 不进货架。进程**不**因为单条失败而停机
- 访问规则只抄类 + 方法上的 `SaCheckPermission` / `SaCheckRole`。`@OpenApi` 的 `value()` 只当 summary
- 参数 / 请求体 / 响应体 schema 问 `SpringDocOperationSchemaResolver`

工作树里业务演示门：`OpenApiDemoController.echo`，`GET /demo/openapi/echo`，`@OpenApi("OpenAPI 回显演示")`，**没有** Sa-Token 注解。空规则对任何非 null 快照 `matches==true`。所以：只要目标人能拍出快照，这本演示书就会出现在他的货架上。按公式算，这扇门的 `interfaceId` 是 `2766c4b3fe40d2486d3ce0dc`。

#### 6. 示例字符串：给人抄作业，不给人密钥

`toCatalogItem` 把定义原样搬进 record，再拼两段示例。curl 固定带：

- `X-OpenAPI-Version: v1`（`OpenApiCanonicalizer.VERSION`）
- `X-App-Key: <APP_KEY>`
- `X-Timestamp: <UNIX_SECONDS>`
- `X-Nonce: <BASE64URL_128_BIT_NONCE>`
- `X-Signature: <NAMEWTA_V1_SIGNATURE>`
- 若 `requestSchema != null`，再加 `Content-Type: application/json` 和 `--data '<JSON_BODY>'`

Java 示例同样用 `HttpRequest.newBuilder`，body 要么 `noBody()` 要么 `ofString("<JSON_BODY>")`。主机名写死 `https://api.example.com`。单测断言示例**含** `<APP_KEY>` / `<NAMEWTA_V1_SIGNATURE>`，**不含** `secret`。

副作用拼起来：四扇窗对 MySQL 只**读**授权投影（用户/Client/角色/菜单），不改凭据，不写 Redis nonce，不建 Sa-Token 机器会话，不发调用事件。HTTP 200 只表示「按这张快照，货架长这样」。

### 图、表或文本图

**图 1. 宏观四扇窗和两道河**

```text
        /system/openApi
          GET  /self/interfaces                      自有列表
          GET  /self/interfaces/{interfaceId}        自有详情
          GET  /users/{userId}/interfaces            他人列表（list + 超管主键）
          GET  /users/{userId}/interfaces/{id}       他人详情（query + 超管主键）
                    │
                    v
          list(target) / detail(target, id)
                    │
          ┌─────────┴──────────┐
          v                    v
   resolve(target)        registry.all()/find
   读库拍快照              启动期 MVC+@OpenApi
          │                    │
          └─────────┬──────────┘
                    v
          matcher.matches(快照, accessRule)
                    │
                    ├─ 过：OpenApiCatalogItem + 占位示例
                    └─ 不过：列表省略 / 详情同一句 unavailable

   另一条河（本课不认完成）：
   HMAC 头齐全 → Gateway 验签 → 同一 matcher → 业务方法
```

- **alt：** 四扇只读 HTTP 窗按目标人快照过滤启动期注册表；网关调用走另一条签名河，但用同一把匹配器。
- **caption：** 图 1——OBJ-30 的空间关系。矩阵 A 就是这四扇。凭据和网关在图外。
- **文字等价物：** 人登录后来看货架。自己的货架用 self 路径；别人的货架必须是超级管理员，并且 URL 里写明那个 `userId`。货架内容 = 启动时扫到的 `@OpenApi` 门 ∩ 目标人现在并起来的权限。机器稍后打那些门时，网关会再用同一把尺子量一次。
- **图的边界：** 图上没有 create/reset 凭据，没有 nonce Redis，没有 `createOpenApiService` 的页面分组。不要把 `generated/openapi.ts` 画成机器货架的立法原件。

**图 2. 失败停在哪一层**

```text
[openapi.enabled=true]  假 → 无 Bean → 404（不是空货架）
   v
[已登录管理端会话]
   v
[自有] SaCheckPermission self
[他人] SaCheckPermission list 或 query
   v
[他人] requireSuperAdmin
        不是 1761100000000000001 → "OpenAPI target catalog is unavailable"
        表不动、注册表不动
   v
[resolve(target)]
        人停用 / 无合法 Client / userId 空
        → "OpenAPI authorization snapshot is unavailable"
   v
[list]  扫注册表，matcher 不过则省略
[detail] 号不存在或 matcher 不过
        → "OpenAPI interface is unavailable"
   v
[HTTP 200]  只表示这一枪读到的货架/条目
        不表示调用方有 appSecret
        不表示网关现在会放行（还要签名、时钟、nonce、限流）
```

- **alt：** 开关、登录、权限串、超管主键、快照、匹配器层层失败关闭；成功也不等于能发出机器调用。
- **caption：** 图 2——目录窗失败关闭链。三句业务文案不要对调。
- **文字等价物：** 关了总开关就没有这四个地址。他人窗不是「有 OpenAPI 菜单就能看同事」。目标人没有合法 Client 时，连空货架都不给，直接业务异常。详情把「没有这扇门」和「这人没权」说成一句。200 只保证货架文本，不保证密钥和签名调用。
- **图的边界：** 网关 401/403/限流是 L-032。凭据明文不回显是 L-031。本图不把那些状态码画进目录窗。

### 正例、反例与边界

**正例 A：普通人看自己的货架。** 调用方 userId=41，有 `system:openApi:self`，自身启用且至少一扇合法 Client，菜单权含 `orders:read`。注册表里两扇门：`allowed` 要 `orders:read`，`denied` 要 `orders:write`。`GET /self/interfaces` 只回 `allowed`。`GET /self/interfaces/allowed` 200 带 curl 占位符。`GET /self/interfaces/denied` 抛 `OpenAPI interface is unavailable`。

**正例 B：超级管理员看别人。** 调用方主键 `1761100000000000001`，权限串有 `system:openApi:list`。`GET /users/9/interfaces` 先过超管闸，再 `resolve(9)`。返回的是用户 9 的并集，不是超管的 `*:*:*` 全表。单测用 `LoginHelper.getUserId()=7` 且 `isSuperAdmin=true` 打 `/users/9/interfaces`，服务被问的是 `list(9L)` 不是 `list(7L)`。

**正例 C：演示回显出现在货架上。** `GET /demo/openapi/echo` 有 `@OpenApi`、无 Sa 注解。任意能拍快照的目标人，列表里都会有 `interfaceId=2766c4b3fe40d2486d3ce0dc`，summary=`OpenAPI 回显演示`，method=`GET`，path=`/demo/openapi/echo`。这扇门**不是**目录 Controller 自己。

**正例 D：前端 URL 合同。** `currentUser.listInterfaces()` → `GET /system/openApi/self/interfaces`。`currentUser.getInterface('order/query')` → `GET /system/openApi/self/interfaces/order%2Fquery`。真实号是 24 hex，斜杠编码这条是运输层契约：路径变量按段编码，服务端再解。

**反例 1：** 「目录存在 `sys_open_api_catalog` 表。」没有这张表。10 号脚本只有 `sys_open_api_credential`。

**反例 2：** 「超管看他人 = 把自己的全能货架复制过去。」过滤键是路径上的 `userId`。

**反例 3：** 「有 `system:openApi:list` 就能看同事。」还差主键闸。文案是 target catalog unavailable，不是 Sa-Token 那句权限不足。

**反例 4：** 「自有详情 404 表示书号写错，403 表示没权。」服务端合成一句 unavailable。HTTP 层若未登录会先被 Sa-Token 拦，那是会话问题，不是 matcher。

**反例 5：** 「货架上的 curl 能直接贴到生产打通。」主机是 example.com；密钥是占位符；还缺真实 nonce/时钟/HMAC。L-032 才核那些头。

**反例 6：** 「这四扇也会出现在机器货架上，因为它们在 SpringDoc 快照里。」管理端快照（L-005 那条河）确实给浏览器生成了 TypeScript。机器货架只收方法级 `@OpenApi`。目录方法没有这注解。

**反例 7：** 「空货架 = OpenAPI 关掉了。」关掉是 Bean 不存在。空数组表示快照成功但 matcher 全滤掉（演示回显仍会在，除非扫描失败把它拒了）。

**反例 8：** 「`interfaceId` 等于 Java 方法名 / `@OpenApi` 文案。」是 method+path 的 SHA-256 前 24 hex。registry 测试要求匹配 `[0-9a-f]{24}`。

**反例 9：** 「这是 layered，目录应该写 UseCase。」登记表 classic。本切片连 `ISysOpenApiCatalogService` 都没有。

**反例 10：** 「列表会读当前 Sa-Token 会话里的 menuPermission。」`list` 每次 `resolve(target)`。自有窗的目标恰好是当前人，但快照是**库里的并集**（跨合法 Client），不是「这把浏览器 Client 登录时带进来的那一包」。

**边界：**

- `openapi.enabled` 不是 true：Controller、Service、网关装配整组缺席。
- 注册表拒绝项只留计数 `rejectedMappings()`，目录 API **不**把拒绝原因回给浏览器。
- `SaCheckOr` / `SaIgnore` / 非空 login type 的 `@OpenApi` 方法进不了货架。
- matcher 对 `user==null` 直接 false；网关侧会变成 forbidden。目录侧在这之前 `resolve` 已经失败关闭。
- 他人窗的超级管理员判定**只认用户主键**，不认角色 key。快照里的 `superadmin` 角色是给 matcher 用的，不是 `requireSuperAdmin()` 用的。
- 数据范围图在快照里，本课四扇不拿它过滤路径。
- 前端 `groupOpenApiCatalog` 按 path 第一段分组，是 domain 投影，**不是**服务端行为。
- 工作树 `OpenApiAutoConfiguration` 类注释写 *Default-off assembly*，admin 的 `application.yml` 默认却是 true。以 yml + `@ConditionalOnProperty(havingValue="true")` 为准：仓库默认开，显式 `false` 才卸。

## 变式与迁移

- **变式 A：给自己核对「机器身份能打哪些门」。** 走 self 两扇。验收：列表 ⊆ 注册表；详情对列表里没有的号 unavailable；示例无 secret。不要用管理端当前菜单树当答案——快照并的是所有合法 Client。
- **变式 B：帮同事排障「为什么 HMAC 403」。** 超管走他人详情。若他人列表里根本没有那扇门，网关用同一 matcher 也会拒。若货架上有、调用仍 401，再去 L-032 查签名/时钟/nonce，不要在目录窗上改权限。
- **变式 C：目标人刚被停用或抽掉最后一扇 Client。** `resolve` 失败，连空货架都不给。这和「权限被收回后货架变空」不是同一层。
- **变式 D：新挂一扇 `@OpenApi` 门。** 改的是 MVC 方法，不是目录 Controller。必须重启（或至少重建注册表 Bean）后货架才出现新书。只改菜单 perms、不改注解，书可能还在，但 matcher 会把它从某人货架上拿掉。
- **变式 E：把演示回显当最小验收。** 目标人能拍快照 ⇒ 列表含 `2766c4b3fe40d2486d3ce0dc`。这不能代替「业务门的权限抄没抄对」。
- **变式 F：前端误把他人 URL 打成 self。** self 永远读当前登录人，路径里没有 userId。超管若想看自己，走 self；看别人，必须 `/users/{id}/interfaces`。
- **迁移口诀：** 先问这枪是四扇里哪一扇 → 再问目标人是会话还是路径 → 再问调用方权限串和超管主键 → 再问快照有没有拍成 → 最后才问 matcher / 注册表。跳步就会把「浏览器 OpenAPI 快照」「凭据」「网关签名」说成同一面货架。

## 常见误区

1. **「OpenAPI 目录 = `api-contracts` 生成物。」** 那是管理端 HTTP 复印件。本课是机器接口货架。
2. **「目录存在数据库。」** 注册表在内存；库只提供授权投影。
3. **「看他人等于看全部。」** 看的是那个 `userId` 的并集。
4. **「list 权限就能看同事。」** 还要超级管理员主键。
5. **「self 读当前会话权限包。」** 每次按目标人现查库并跨 Client 求并。
6. **「详情会区分 不存在 / 没权限。」** 同一句 unavailable。
7. **「curl 示例里有密钥。」** 占位符；单测禁止出现 `secret`。
8. **「目录窗自己也是机器可调接口。」** 没有 `@OpenApi`。
9. **「这是 layered UseCase。」** classic，专用 Service。
10. **「关掉开关会返回空数组。」** Bean 消失。
11. **「`interfaceId` 是方法名。」** 24 hex。
12. **「货架 200 就能 HMAC 打通。」** 还缺凭据、签名、时钟、nonce、限流。
13. **「他人详情权限也是 list。」** 是 `system:openApi:query`。
14. **「把 L-021 的 refreshCache 打到 OpenAPI 就能刷新货架。」** 本 Controller 没有写窗，注册表只在启动扫描。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `SysOpenApiCatalogController`。把四对 mapping 抄成一张表：路径、动词、Java 名、权限串、目标人从哪来、有没有 `@Log`。圈出他人两扇的 `requireSuperAdmin()`，以及类上的 `openapi.enabled`。
2. 用手指划 `selfInterfaces` → `LoginHelper.getUserId()` → `catalogService.list` → `resolve` → `registry.all` → `matcher.matches` → `toCatalogItem`。在 `toCatalogItem` 旁边写：示例里出现了哪些头，哪些字故意不是真密钥。
3. 打开 `SysOpenApiAuthorizationMapper.xml` 的 `legalClientJoin` 和 `selectActivePermissions`。写一句：没有合法 Client 时，目录连空列表都不给。
4. 打开 `OpenApiOperationRegistry.interfaceId` 和 `OpenApiDemoController.echo`。对照本课写出的 24 hex，看它是不是 `GET` + `"\n"` + `/demo/openapi/echo`。
5. 打开 `OpenApiGatewayFilter` 里 `authorizationMatcher.matches` 那一行，再回到目录 Service 的 filter。写一句：预览尺子和放行尺子为什么必须是同一把。

## 总结、词汇表与下一步

- **四扇窗。** 前缀 `/system/openApi`。GET 四扇全是读。自有 self + `system:openApi:self`；他人 list/query + 超级管理员主键。
- **一个目标人。** 过滤键永远是 `targetUserId`。自有从会话取，他人从路径取。
- **一张启动期货架。** 真实 MVC × 方法级 `@OpenApi` → 24 hex 书号。目录窗自己不上架。
- **一次现查快照。** 启用用户 + 合法 Client + 角色/菜单并集；超管目标人再贴 `*:*:*`。不读凭据表。
- **一把共用尺子。** `OpenApiAuthorizationMatcher` 给目录预览和网关放行共用。详情失败关闭成同一句 unavailable。
- **格子按磁盘全表：** 不要补 POST，不要把凭据生命周期说进本课，不要把 `createOpenApiService` 认成矩阵 A。

词汇表：`SysOpenApiCatalogController` / `selfInterfaces` / `selfInterface` / `userInterfaces` / `userInterface` / `SystemOpenApiCatalogService` / `OpenApiCatalogItem` / `interfaceId` / `@OpenApi` / `OpenApiOperationRegistry` / `OpenApiAuthorizationResolver` / `OpenApiAuthorizationMatcher` / `requireSuperAdmin` / `system:openApi:self|list|query` / `openapi.enabled` / `LoginUser.userType=openapi` / NAMEWTA v1 头占位符 / classic。

下一步：L-031 走进同一门牌下的凭据窗——创建/重置/启停/删除，以及明文只在签发瞬间出现。L-032 才把货架上的书号连到 HMAC 网关和机器会话的失败关闭。L-033 才把 `createOpenApiService` 与 admin-web 页面接到这四扇 GET。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 房间内公开入口；本课 Controller 同模块 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记为 classic；禁止与 layered 混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 无目录表；有 `sys_open_api_credential`；菜单权限 `system:openApi:self\|list\|query` | DDL 凭据表；DML OpenAPI 菜单 | 2026-09-16 |
| S-L030-01 | `SysOpenApiCatalogController.java` | 四扇映射、self/list/query 权限、超管闸文案、`openapi.enabled`、不继承 BaseController、无 POST/@Log | 类与四个公开方法 | 2026-09-16 |
| S-L030-02 | `SystemOpenApiCatalogService.java` | `list`/`detail` 只按目标人快照过滤；详情同一句 unavailable；curl/Java 占位示例；不读凭据 | `list`/`detail`/`toCatalogItem` | 2026-09-16 |
| S-L030-03 | `SystemOpenApiAuthorizationResolver.java`、`SysOpenApiAuthorizationMapper.xml` | 合法 Client 闸；角色默认+显式并集；超管贴 `*:*:*`；`userType=openapi`；无 Client 回退 | `resolve` 与四条 SQL | 2026-09-16 |
| S-L030-04 | `OpenApiOperationRegistry.java`、`OpenApi.java`、`OpenApiDemoController.java` | 只收方法级 `@OpenApi`；24 hex 书号；拒绝计数；演示回显无 Sa 注解 | `afterPropertiesSet` / `interfaceId` / `echo` | 2026-09-16 |
| S-L030-05 | `OpenApiAuthorizationMatcher.java`、`OpenApiGatewayFilter.java` | 目录与网关共用 matcher；网关在会话里再量一次 accessRule | `matches`；filter `doFilterInternal` | 2026-09-16 |
| S-L030-06 | `SystemOpenApiCatalogTest.java`、`SystemOpenApiAuthorizationResolverTest.java`、`OpenApiOperationRegistryTest.java` | 不碰凭据；超管服务端重查；快照失败关闭；interfaceId `[0-9a-f]{24}` | 测试方法名 | 2026-09-16 |
| S-L030-07 | `application.yml` `openapi:`、`OpenApiAutoConfiguration.java`、`OpenApiHeaders.java`、`OpenApiCanonicalizer.java` | 默认 enabled；装配条件；示例头名与 `v1` | admin yml；common-openapi config/protocol | 2026-09-16 |
| S-L030-08 | `frontend/packages/domains/system/src/open-api/service.ts`、`types.ts`、`index.test.ts` | self/users GET 合同；`encodeURIComponent`；本课不认工厂格子 | `currentUser`/`targetUser` 的 list/getInterface | 2026-09-16 |
