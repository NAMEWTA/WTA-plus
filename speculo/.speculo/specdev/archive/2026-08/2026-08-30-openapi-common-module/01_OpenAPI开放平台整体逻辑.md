# OpenAPI 开放平台整体逻辑：从创建密钥到业务方法执行

这次开发交付的不是“再做一套登录”，而是一座机器调用桥：外部程序每次用 AppKey/AppSecret 对请求签名，网关验证请求确实来自凭据持有者，再把凭据所属用户当前已有的权限临时装进标准 Sa-Token 上下文，最后继续执行原来的 Controller、权限注解和数据权限逻辑。

先记住三个结论：

1. AppKey/AppSecret 只证明“这是哪个用户的机器凭据”，不会自动给用户增加任何权限。
2. `@OpenApi` 只决定“这个方法能否进入机器调用入口”；`@SaCheckPermission`、`@SaCheckRole` 和数据权限仍决定“这个用户能否调用”。
3. 管理页面、接口目录和签名调用是三条不同的数据流，但它们共享同一凭据事实、同一开放方法注册表和同一授权快照算法。

> 当前最终源码中的一个重要事实：生产代码还没有任何业务方法标注 `@OpenApi`，该注解目前只出现在测试用例中。因此平台能力已经完成，但第一次启用后接口目录会是空的；后续必须由业务模块逐个明确开放合适的方法。

## 先看全图

```text
                         管理面（浏览器 Token）
+----------------+    +-------------------------+    +------------------------+
| 个人中心 Tab    | -> | OpenApiWorkspace        | -> | /system/openApi/self/* |
| 系统管理页      | -> | 同一 domain/service/UI  | -> | /system/openApi/users/*|
+----------------+    +-------------------------+    +-----------+------------+
                                                               |
                                                               v
                                                    +------------------------+
                                                    | ruoyi-system           |
                                                    | 凭据 / 目录 / 授权快照 |
                                                    +-----+-------------+----+
                                                          |             |
                                        AES-GCM 加密存储   |             | 读取当前 RBAC
                                                          v             v
                                               +-------------+   +----------------+
                                               | MySQL 凭据表 |   | user/client/   |
                                               +-------------+   | role/menu 表    |
                                                                 +-------+--------+
                                                                         |
                                                                         v
调用面（无浏览器 Token）                                        标准 LoginUser 快照
+----------------+       5 个签名头       +------------------------------+
| 外部机器程序    | --------------------> | OpenApiGatewayFilter         |
| AppKey/Secret  |                       | 方法 -> 验签 -> 防重放 -> 限流 |
+----------------+                       +---------------+--------------+
                                                        |
                                  +---------------------+--------------------+
                                  |                                          |
                                  v                                          v
                       +----------------------+                    +------------------+
                       | Redis                |                    | Sa-Token 机器会话 |
                       | nonce / 两级限流     |                    | 缓存 LoginUser    |
                       +----------------------+                    +---------+--------+
                                                                            |
                                                                            v
                                                           +-----------------------------+
                                                           | 原 Controller 方法          |
                                                           | 原权限注解 + 原数据权限逻辑 |
                                                           +-----------------------------+
```

可以把各模块理解成以下分工：

| 模块 | 负责什么 | 明确不负责什么 |
| --- | --- | --- |
| `ruoyi-common-openapi` | 协议、验签网关、方法注册表、防重放、限流、机器会话桥、窄 SPI | 不直接查 system 数据库，不授予权限 |
| `ruoyi-system` | 凭据生命周期、AES-GCM 加解密、授权快照、目录 API、失效机器会话 | 不复制一套网关，不返回内部机器 Token |
| `ruoyi-common-security` | 让已验证的机器身份进入原 Sa-Token 拦截链 | 不放松普通浏览器登录的 Client 校验 |
| `ruoyi-common-web` | 对 HTTP 头、参数、JSON 正文中的敏感信息脱敏 | 不记录原始 AppSecret 或签名 |
| `system` 前端 domain/web-domain | 双 scope API、响应投影、工作区状态、两个入口复用 | 不保存 AppSecret，不在前端决定最终授权 |

## 一步一步看

### 1. 系统怎样开启

功能默认关闭：

```text
OPENAPI_ENABLED=false
        |
        +-> 不装配网关
        +-> 不创建 OpenAPI system service/controller
        +-> 原系统按原样运行
```

只有 `openapi.enabled=true` 时，`OpenApiAutoConfiguration` 才会装配完整链路。启动阶段会检查：

- KEK 版本只能包含 1 到 64 个字母、数字、点、下划线或连字符。
- KEK 必须是标准 Base64 编码的 32 字节密钥。
- 时钟容差、nonce TTL、机器会话 TTL 和两级限流额度都必须大于 0。
- Redis、Sa-Token、Spring MVC 映射表、唯一的凭据解析器和授权解析器都必须存在。

任一条件不满足就停止启动，没有内存版 nonce、内存限流或弱化认证作为后备。这叫“失败关闭”：安全依赖坏了，宁可暂不可用，也不降低验证标准。

主要配置默认值：

| 配置 | 默认值 | 作用 |
| --- | --- | --- |
| `openapi.clock-skew` | `60s` | 接受客户端时间与服务端时间的最大偏差 |
| `openapi.nonce-ttl` | `60s` | 同一 AppKey + nonce 的防重放窗口 |
| `openapi.app-rate-limit-per-minute` | `1000` | 单 AppKey 每分钟总额度 |
| `openapi.interface-rate-limit-per-minute` | `100` | 单 AppKey 对单接口每分钟额度 |
| `openapi.machine-session-ttl` | `8h` | 授权快照在 Sa-Token 中的缓存时间 |

生产发布顺序是：先备份数据库并执行 DDL/DML，再以 `OPENAPI_ENABLED=false` 部署，配置 Redis 和 KEK，最后经过单独审批打开功能。故障恢复时第一步也是先关闭开关。

### 2. 一个业务方法怎样成为开放接口

业务开发者必须在具体 Spring MVC 方法上显式加注解：

```java
@OpenApi("查询订单")
@SaCheckPermission("order:query")
@GetMapping("/orders/{id}")
public R<OrderVo> getOrder(@PathVariable Long id) {
    return R.ok(orderService.get(id));
}
```

这里的两把锁作用不同：

```text
@OpenApi              @SaCheckPermission
    |                         |
    v                         v
允许进入开放注册表      用户是否有 order:query
    |                         |
    +----------- AND --------+
                  |
                  v
              才能实际调用
```

类级 `@OpenApi` 不存在，避免无意中一次开放整个 Controller。启动时 `OpenApiOperationRegistry` 遍历真实 `RequestMappingHandlerMapping`，只读取方法自己声明的注解，并完成以下工作：

1. 取真实 HTTP method 和路径模板。
2. 根据 `SHA-256(method + "\n" + path)` 的前 12 字节生成稳定 `interfaceId`。
3. 解析类和方法上的 `@SaCheckPermission`、`@SaCheckRole`。
4. 用 SpringDoc 同源的 `ModelConverters` 生成参数、请求体和响应 JSON Schema。
5. 按路径和方法排序，生成不可变注册表。

没有明确 HTTP method、没有可用路径、接口标识重复、Schema 无法解析，或使用了首期不支持的 Sa-Token 注解组合时，该映射不会进入开放目录，并计入 `rejectedMappings`。

首期只能开放“Client 无关”的方法。机器身份故意没有 `clientPk/clientKey`；任何业务逻辑如果必须知道唯一前端 Client，就不应在这一期加 `@OpenApi`。

### 3. 用户怎样创建和管理凭据

系统有两个页面入口，但只实现了一套能力：

```text
个人中心 > OpenAPI Tab
        -> scope = current-user
        -> /system/openApi/self/*

系统管理 > 应用开放管理
        -> 先明确选择目标用户
        -> scope = target-user
        -> /system/openApi/users/{userId}/*
```

个人入口要求 `system:openApi:self`。管理员入口的查询、新增、修改、删除分别要求对应按钮权限；后端还额外执行 `LoginHelper.isSuperAdmin()`，所以仅伪造前端按钮权限不能管理他人的凭据。

创建流程如下：

```text
表单 appName/expiresAt/remark
        |
        v
校验目标用户有效、当前没有未删除凭据
        |
        v
随机 16 字节 -> Base64URL AppKey
随机 32 字节 -> Base64URL AppSecret
        |
        v
AES-256-GCM(AppSecret, KEK, 随机 12 字节 nonce, AAD)
        |
        +-> ciphertext + nonce + 16 字节 tag + kekVersion -> MySQL
        |
        +-> AppSecret -> 仅本次 HTTP 响应 -> 一次性弹窗
```

AAD 绑定了固定协议上下文、ownerUserId、AppKey 和 KEK 版本。因此把一行密文搬到另一个用户、换一个 AppKey 或伪造 KEK 版本，GCM 认证都会失败。

数据库表的几个关键约束：

- `app_key` 全局唯一。
- 生成列 `active_owner_user_id` 对所有未逻辑删除记录保持每用户唯一。
- `version` 是乐观锁；并发操作只允许一个成功，其他请求得到 409 冲突。
- 删除是逻辑删除，删除后同一用户才可以重新创建新凭据。
- 停用不是删除，仍然占用该用户的唯一凭据位置。

创建与重置返回 `OpenApiCredentialIssued`，其中才有 `appSecret`；普通查询只返回 `OpenApiCredentialSummary`，模型中根本没有 secret 字段。重置只更换 AppSecret，AppKey 保持不变，旧签名立即失效。

前后端都给创建/重置响应加了 `Cache-Control: no-store`；操作日志也明确关闭请求和响应正文保存。前端一次性弹窗关闭后会清掉组件本地的 `issued` 对象，不写 Pinia、localStorage 或公共缓存。

### 4. 前端数据怎样流动

前端不是“页面直接拼 URL”，而是四层窄链路：

```text
App 入口
  |
  v
web-domain: OpenApiAdminPage / OpenApiWorkspace
  |
  v
workflow: scope、权限、并发状态、错误分类、一次性 secret
  |
  v
domain service: 参数校验、URL 编码、currentUser/targetUser API
  |
  v
transport projector: 把 unknown 响应严格投影为不可变领域对象
  |
  v
adminHttp -> 后端
```

`OpenApiWorkspace` 同时服务两个入口。它收到的 scope 只有两种：当前用户，或已经明确选择的目标用户。管理员页不会偷偷使用当前登录用户替代目标用户。

页面加载时，凭据摘要和可调用接口目录并行请求：

```text
controller.load()
      |
      +-> getCredential() ----+
      |                        +-> Promise.allSettled -> 合并界面状态
      +-> listInterfaces() ----+
```

如果凭据查询是 404、但目录查询成功，界面解释为“还没创建凭据”，而不是整个功能失败。目录本来就不依赖凭据存在。`generation` 计数还会丢弃 scope 切换前返回的旧请求，避免快速换用户时串数据。

transport 层把后端响应当作 `unknown`：标识、状态、HTTP method、规则、Schema 都逐字段检查；摘要响应只要混入任何 secret 字段就拒绝，签发响应也只允许一个名为 `appSecret` 的 secret 字段。这是在 TypeScript 类型之外再加一道运行时边界。

### 5. 接口目录怎样计算

目录不是数据库里再维护一张“开放接口表”，而是两份实时事实的交集：

```text
启动时开放方法注册表                    目标用户当前授权快照
method/path/schema/accessRule          roles/permissions/dataScope
              |                                  |
              +---------------+------------------+
                              |
                              v
                 OpenApiAuthorizationMatcher
                              |
                              v
                    目标用户可调用接口目录
```

个人预览使用本人 userId；超级管理员预览某个用户时使用目标 userId，不继承管理员自己的权限，也不会创建机器会话。

目录详情包含 method、path、参数位置、请求/响应 Schema、权限规则以及 cURL/Java 请求骨架。示例中的签名仍是占位符，调用方必须按 NAMEWTA v1 协议自己计算真正签名。

### 6. 全局授权快照怎样构建

普通浏览器登录只属于一个 Client；OpenAPI 凭据只属于用户，不属于某个 Client。因此 system 需要构建该用户的“合法 Client 权限并集”。

合法 Client 必须同时满足：

1. `sys_client` 正常且未删除。
2. 对应 `sys_user_type` 正常且未删除。
3. 用户存在一条状态正常的 `sys_user_type_rel`。

然后每个合法 Client 贡献：

- 该 Client 的正常默认角色。
- 用户在该 Client 下显式拥有的正常角色。
- 这些角色关联、且 `menu.client_id == role.client_id` 的正常菜单权限字符。
- 每个权限由哪些 roleId 提供，用于后续数据权限计算。

```text
Client A: 默认角色 + 显式角色 + 菜单权限 --+
Client B: 默认角色 + 显式角色 + 菜单权限 --+-> 去重并集 -> LoginUser
Client C: 用户无 user_type_rel ---------------X  不参与
```

构造出的标准 `LoginUser` 有这些特殊点：

- `userType = "openapi"`，用于与浏览器身份严格区分。
- `clientPk = null`、`clientKey = null`，不假装来自任何前端 Client。
- `rolePermission`、`menuPermission`、`roles` 和 `dataScopeRoleMap` 都来自权威数据库的只读投影。
- 不加载动态路由树和岗位列表。
- 超级管理员补入 `admin` 角色语义和 `*:*:*` 权限。

如果用户停用/删除、没有任何合法 Client，或快照与目标 userId 不一致，就拒绝构建，不会生成一个空权限但可登录的机器用户。

### 7. 客户端怎样计算签名

每次请求必须发送五个头：

```text
X-OpenAPI-Version: v1
X-App-Key:          <Base64URL AppKey>
X-Timestamp:        <Unix 秒>
X-Nonce:            <至少 16 字节随机值的 Base64URL，无 = 填充>
X-Signature:        <HMAC-SHA256 结果的 Base64URL，无 = 填充>
```

签名前先构造九行 canonical request：

```text
NAMEWTA-HMAC-SHA256
v1
<appKey>
<timestamp>
<nonce>
<UPPERCASE_METHOD>
<canonical_path>
<sorted_canonical_query>
<lowercase_sha256_hex_of_exact_body_bytes>
```

然后计算：

```text
key       = Base64URL_decode(appSecret)
signature = Base64URL_no_padding(HMAC_SHA256(key, UTF8(canonicalRequest)))
```

规范化有几个容易出错的地方：

- path 按 UTF-8 字节重新百分号编码，保留 `/`，十六进制使用大写。
- query 把每个名称和值解码后重新编码，再按名称、值排序；重复参数不能丢。
- `+` 是字面加号，不按表单规则变成空格，规范化后是 `%2B`。
- body 签的是网络上实际发送的字节，不是“等价 JSON 对象”。空 body 也有固定 SHA-256。

因此客户端不能签完一个 JSON 字符串后，又让 HTTP 库换缩进、换编码或改字段顺序。网关用 `ReplayableOpenApiRequest` 先保存精确 body 字节用于验签，再把同一批字节交给 MVC 读取。

### 8. 一次签名请求的精确执行顺序

```text
外部请求
  |
  v
[0] 五个签名头一个都没有？ ---- 是 ---> 当普通浏览器请求继续
  |
  否
  v
[1] 五个头是否齐全，是否混入 Authorization/Token/Cookie？
  | 失败 -> 401 OPENAPI_AUTHENTICATION_FAILED
  v
[2] 真实 MVC handler 是否是注册过的 @OpenApi 方法？
  | 失败 -> 403 OPENAPI_FORBIDDEN
  v
[3] 版本、Base64URL、时间窗口、凭据启用/过期、HMAC 是否正确？
  | 失败 -> 401 OPENAPI_AUTHENTICATION_FAILED
  v
[4] Redis 原子登记 AppKey + nonce
  | 重复 -> 401；Redis 故障 -> 503
  v
[5] AppKey 总限流 + AppKey/接口限流
  | 任一超限 -> 429 OPENAPI_RATE_LIMITED
  v
[6] 查找或重建 Sa-Token 机器会话
  | 故障/快照非法 -> 503 OPENAPI_UNAVAILABLE
  v
[7] 用标准 LoginUser 匹配原权限/角色规则
  | 不匹配 -> 403 OPENAPI_FORBIDDEN
  v
[8] 标记 verifiedRequest，执行原 Controller 和业务链
  |
  v
[9] 清理请求内机器身份，发布不含敏感信息的计量事件
```

只要出现任意一个签名头，网关就接管请求；部分签名头不会退回普通登录。反过来，一个签名请求如果同时带浏览器 `Authorization`、`Token` 参数或认证 Cookie，也会直接 401，防止两种身份发生混淆。

所有认证阶段错误统一暴露为 `OPENAPI_AUTHENTICATION_FAILED`，不会告诉攻击者到底是 AppKey 不存在、凭据过期、密文解不开还是签名不对。

### 9. 防重放和限流为什么在验签之后

只有签名正确的请求才能占用 nonce 和额度，否则攻击者只凭一个公开 AppKey 就能消耗合法调用方资源。

Redis nonce key 使用 `SHA-256(appKey + nonce)`，不会把 AppKey 和 nonce 原文放进 Redis key。`setIfAbsent` 保证集群内只有第一次登记成功。

nonce 只解决“同一签名请求不能再放一次”，不解决业务幂等：

```text
第一次 POST: nonce-A -> 创建订单 1001
网络超时后重试:
  复用 nonce-A -> 被防重放拒绝
  使用 nonce-B -> 是新的合法请求，可能再创建订单 1002
```

需要“重试仍只产生一笔业务结果”时，业务接口仍要设计自己的 Idempotency-Key 或业务唯一约束。重试 OpenAPI 请求必须生成新的 timestamp、nonce 和 signature。

限流同时申请两个 Redis 原子额度：`AppKey` 总额度和 `AppKey + interfaceId` 单接口额度。两者都通过才能继续，既防止一个应用压垮全站，也防止它把全部额度集中打向一个接口。

### 10. 机器会话为什么存在

每次请求都必须重新 HMAC 验签；机器会话不是让调用方跳过验签，而是缓存“这个用户当前拥有哪些权限”，从而复用原 Sa-Token 体系。

```text
验签得到 credentialId + ownerUserId
                 |
                 v
查 Sa-Token: loginId=openapi:{userId}, device=openapi:credential:{credentialId}
                 |
        +--------+---------+
        | 命中且身份一致    | 未命中
        v                  v
直接复用 LoginUser     取得 userId 分布式锁
                           |
                           v
                    再查一次 -> 仍未命中
                           |
                           v
                    从 system 重建快照
                           |
                           v
                    创建 TTL 机器 Session
```

双重检查加 userId 分布式锁避免同一用户并发创建多份机器会话。发现同一 credential 匹配多个有效 Token，或 Session sidecar 中的 credentialId/userId 不一致，也会拒绝使用。

执行 Controller 前，桥接层只把内部 Token 写入当前 Servlet 请求的 Sa-Token Storage；它不会写响应头、Cookie 或返回体。执行完无论成功、403 还是下游异常，都会恢复进入前的 Storage，防止机器身份泄漏到同线程的其他逻辑。

`SecurityConfig` 仍先执行 `checkLogin()`。只有同时满足以下四点，才跳过普通浏览器专属的 clientid、路径和 IP 规则：

1. 请求上有网关临时写入的 `verifiedRequest=true`。
2. 当前 `LoginUser.userType == openapi`。
3. `clientPk == null`。
4. `clientKey == null`。

所以单独伪造请求 attribute、单独拿到内部 Token，或把普通用户改个类型，都不能构成完整旁路条件。

### 11. 权限变化怎样立即生效

机器会话有 8 小时 TTL，如果只等自然过期，停用用户或删除权限后可能继续使用旧快照。因此 system 的权威写路径接入了 `OpenApiMachineSessionInvalidator`：

```text
凭据重置/启停/删除
用户资料/状态/删除
用户与登录域关系变化
Client 或用户类型变化
用户角色、角色菜单、角色状态变化
菜单权限或菜单状态变化
             |
             v
按受影响 userId 获取同一把分布式锁
             |
             v
注销该用户所有 openapi:* 机器终端
             |
             v
下一次“有效验签”只能 cache miss -> 从数据库重建快照
```

失效与创建使用同一 userId 锁，避免“刚删完旧会话，另一个节点又用旧数据创建新会话”的直接竞态。注销继续使用 Sa-Token DAO，因此沿用项目既有的 Redis 和跨节点本地缓存失效机制，而不是直接手删 Redis key。

当 OpenAPI 功能关闭时，不存在 invalidator Bean；原 system service 使用 no-op 默认实现，保证开关关闭不会破坏普通用户、角色和菜单操作。

### 12. 原权限注解和数据权限怎样复用

注册表在启动时把原 `@SaCheckPermission` 和 `@SaCheckRole` 转成 `OpenApiAccessRule`。目录预览和真实调用都交给同一个 `OpenApiAuthorizationMatcher`，并通过 Sa-Token 的 `SaStrategy.hasElement` 保留通配符匹配语义。

权限快照同时携带：

- `menuPermission`：方法级权限字符判断。
- `rolePermission`：角色判断和 `orRole` 备选判断。
- `roles`：包含各角色的数据范围类型。
- `dataScopeRoleMap`：一个权限字符由哪些角色授予。

当业务 Service/Mapper 继续走项目原数据权限组件时，它读到的是同一个标准 `LoginUser`，因此不是“网关验证通过就能看所有数据”，而是继续按该用户角色的数据范围过滤。

### 13. 日志和计量怎样避免泄密

系统 HTTP 日志在副本上递归脱敏，不修改业务实际收到的数据：

- `X-App-Key`、`X-Signature`、Authorization、Cookie 等敏感请求/响应头写成 `[REDACTED]`。
- JSON 字段名经过去横线、下划线和小写化后，匹配 `appSecret`、`secret`、`signature`、`token`、`credential` 等集合就递归脱敏。
- JSON 非法或正文被截断而无法安全解析时，整个正文写成 `[REDACTED]`。
- 非 JSON、multipart、二进制、文件、音视频和流式正文只记元数据。

创建/重置 Controller 还显式禁用业务操作日志的请求与响应数据保存，形成额外保护。

网关调用事件只包含 credentialId、ownerUserId、interfaceId、method、path、HTTP status、耗时和时间，不包含 AppKey、AppSecret、签名、nonce 或正文。事件发布器默认是 no-op；即使扩展的计量发布失败，也不能改变已经确定的 HTTP 响应。

### 14. 状态、错误与边界怎样理解

| 情况 | 结果 | 原因 |
| --- | --- | --- |
| 没有任何签名头 | 进入普通请求链 | OpenAPI 不抢普通浏览器请求 |
| 签名头不完整或混入浏览器认证 | 401 | 身份模式不明确 |
| AppKey/签名/时间/nonce 错误 | 401 | 统一隐藏具体认证失败点 |
| 签名请求访问未开放方法 | 403 | `@OpenApi` 是显式入口锁 |
| 用户没有原方法权限 | 403 | 凭据不产生权限 |
| 超过任一级额度 | 429 | 两级限流必须同时通过 |
| Redis、机器会话或内部状态失败 | 503 | 安全状态不可靠时失败关闭 |
| Controller 自己抛异常 | 保留原下游异常语义 | 网关不把业务错误伪装成认证错误 |
| 计量发布失败 | 不改变响应 | 计量是可选旁路能力 |

### 15. 实际使用的最短路径

平台管理员：

1. 执行凭据表 DDL 和菜单 DML。
2. 通过密钥管理系统提供 KEK，准备 Redis，再打开 OpenAPI 开关。
3. 为适合机器调用且不依赖单一 Client 的业务方法逐个加 `@OpenApi`。
4. 给用户保留真实业务权限；凭据不会补权。
5. 用户本人在个人中心创建，或超级管理员在系统管理中为明确目标用户创建凭据。
6. 创建/重置后立即把一次性 AppSecret 放入调用方的密钥管理系统。

调用方：

1. 从目录读取 method、path、Schema 和请求示例。
2. 用实际发送的 method/path/query/body 构造 canonical request。
3. 每次生成新 timestamp 和 nonce，用 AppSecret 做 HMAC-SHA256。
4. 只发送五个 OpenAPI 签名头，不发送浏览器 Token、clientid 或认证 Cookie。
5. 网络重试时重新生成 timestamp、nonce 和签名；写操作另行提供业务幂等键。
6. 实际部署应使用 HTTPS，HMAC 保证完整性和来源认证，不负责隐藏传输内容。

### 16. 源码导航

按阅读价值排序，核心入口如下：

| 想看什么 | 源码位置 |
| --- | --- |
| 条件装配和 Filter 顺序 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/config/OpenApiAutoConfiguration.java</Path> |
| 整条调用校验顺序 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/gateway/OpenApiGatewayFilter.java</Path> |
| canonical request 规则 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/protocol/OpenApiCanonicalizer.java</Path> |
| HMAC 签名与常量时间比较 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/protocol/OpenApiSigner.java</Path> |
| 方法扫描和接口注册表 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/registry/OpenApiOperationRegistry.java</Path> |
| 机器会话 cache-aside 桥 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/OpenApiMachineSessionBridge.java</Path> |
| Sa-Token 内部 Token 操作 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-openapi/src/main/java/org/dromara/common/openapi/session/SaTokenOpenApiMachineSessionOperations.java</Path> |
| 全局权限并集算法 | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/authorization/SystemOpenApiAuthorizationResolver.java</Path> |
| 权限并集 SQL | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/resources/mapper/system/SysOpenApiAuthorizationMapper.xml</Path> |
| 凭据创建、重置、启停、删除 | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/credential/service/SystemOpenApiCredentialService.java</Path> |
| AES-256-GCM 实现 | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/credential/crypto/OpenApiCredentialCrypto.java</Path> |
| 自己/目标用户管理 API | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/openapi/SysOpenApiCredentialController.java</Path> |
| 实时接口目录 | <Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/openapi/catalog/SystemOpenApiCatalogService.java</Path> |
| 机器身份进入原安全链的条件 | <Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-security/src/main/java/org/dromara/common/security/config/SecurityConfig.java</Path> |
| 前端领域 API 和输入校验 | <Path>plus-ui-namewta/packages/domains/system/src/open-api/service.ts</Path> |
| 前端运行时响应校验 | <Path>plus-ui-namewta/packages/domains/system/src/open-api/transport.ts</Path> |
| 双 scope 工作流 | <Path>plus-ui-namewta/packages/web-domains/system/src/open-api/workflow.ts</Path> |
| 双入口复用 UI | <Path>plus-ui-namewta/packages/web-domains/system/src/open-api/OpenApiWorkspace.vue</Path> |
| 数据库表与菜单权限 | <Path>ruoyi-vue-plus-namewta/script/sql/namewta/DDL.sql</Path>、<Path>ruoyi-vue-plus-namewta/script/sql/namewta/DML.sql</Path> |

### 17. 当前实现没有承诺什么

- 当前没有生产业务方法实际标注 `@OpenApi`，所以还没有开箱即用的业务调用目录。
- 目录里的 Java/cURL 是请求骨架，不是完整客户端 SDK；签名仍需调用方实现。
- 首期门禁使用模块测试、MockMvc 和基础设施替身；没有把真实 MySQL 唯一约束、真实 Redis、多进程集群和完整浏览器 Playwright 作为强制验证。这些仍是上线前环境验证重点。
- OpenAPI 不是 OAuth、Bearer Token 或浏览器登录替代品，也不支持调用方指定 Client。
- HMAC 不是加密；如果不用 HTTPS，请求正文仍可能被链路观察者看到。
- nonce 不是业务幂等键，机器会话也不是跳过每次签名的长期登录态。

## 术语小词典

| 术语 | 通俗解释 |
| --- | --- |
| AppKey | 可以公开传输的凭据编号，用来找到哪条密钥记录 |
| AppSecret | 只有调用方和服务端知道的 HMAC 密钥，只在创建或重置时展示一次 |
| KEK | 服务端用于加密数据库中 AppSecret 的 32 字节主密钥 |
| HMAC-SHA256 | 用共同密钥为请求内容制作不可伪造校验码的算法 |
| canonical request | 把同一个 HTTP 请求整理成唯一九行文本，避免双方对“签了什么”理解不同 |
| nonce | 每次请求的新随机数，用于识别签名请求是否被重复播放 |
| interfaceId | 由 HTTP method 和路径模板稳定计算的开放接口标识 |
| 开放注册表 | 启动时从真实 MVC 方法和 `@OpenApi` 构建的不可变接口清单 |
| 授权快照 | 从数据库复制出的用户当前角色、权限和数据范围，只读且不会补权 |
| 机器会话 | 服务端内部缓存授权快照的 Sa-Token Session，不返回给调用方 |
| cache-aside | 先查缓存；未命中时从权威数据库重建并写回缓存 |
| 失败关闭 | Redis、密钥、会话等安全状态不可靠时拒绝请求，而不是降低校验标准 |
| current-user scope | 个人中心只操作当前登录用户自己的凭据与目录 |
| target-user scope | 超级管理员明确选择用户后，操作该目标用户的凭据与目录 |
| 管理面 | 使用浏览器 Token 创建和维护凭据、查看目录的 API |
| 调用面 | 外部机器用五个签名头调用已开放业务方法的链路 |

## 你现在能复述什么

读完后，应该能用自己的话回答下面这些问题：

1. 为什么 `@OpenApi` 和 `@SaCheckPermission` 缺一不可？
2. 为什么一个用户只有一条全局凭据，却仍会聚合多个合法 Client 下的权限？
3. 为什么机器 `LoginUser` 的 `clientPk/clientKey` 必须为空？
4. 九行 canonical request 分别是什么，body 为什么必须使用精确字节？
5. 一次请求为什么按“验签 -> nonce -> 两级限流 -> 机器会话 -> 权限 -> Controller”的顺序执行？
6. 为什么每次仍需验签，但授权快照可以缓存 8 小时？
7. 角色、菜单、用户或凭据变化后，旧快照怎样被注销？
8. 为什么目录预览不需要先创建凭据，也不会使用管理员本人的权限？
9. 为什么重试必须换 nonce 和签名，但仍可能需要业务 Idempotency-Key？
10. 为什么开发已经完成，当前生产目录仍会为空？

最简复述版本是：

```text
业务明确用 @OpenApi 开门
        -> 用户创建一次性展示的加密凭据
        -> 调用方对每个请求做 HMAC 签名
        -> 网关验方法、验签、防重放、限流
        -> 服务端恢复用户当前权限快照
        -> 原权限注解和原业务代码执行
        -> 权限变化就注销快照，下次从数据库重建
```
