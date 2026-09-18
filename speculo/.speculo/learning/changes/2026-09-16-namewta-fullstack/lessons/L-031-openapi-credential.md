---
lesson_id: L-031
objective_ids: [OBJ-31]
claimed_cells:
  - A:SysOpenApiCredentialController.*
  - B:SysOpenApiCredentialController.create/reset
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 8
  - segment: create-reset-ciphertext
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L031-01, S-L031-02, S-L031-03, S-L031-04, S-L031-05, S-L031-06, S-L031-07, S-L031-08]
---

# Lesson 031：宏观上看，钥匙只给一次——OpenAPI 凭据创建/重置/启停/删除

## 学完你能做什么

打开 `wta-system` 里**一份** Controller，你能**口述十三条公开窗**，并单独把 OBJ-31 点名的那句说完：**创建 / 重置会把 `appSecret` 明文只放进一次性签发盒 `OpenApiCredentialIssued`；库里只留 AES-256-GCM 密文；列表、详情、启停永远走没有秘密字段的 `OpenApiCredentialSummary`；删除不回任何秘密。** 不要把「明文不回显」说成「创建也绝不返回明文」，也不要把启停说成 OSS 那种换正门。

本课认矩阵两格，方法名以**磁盘**为准：

1. **`A:SysOpenApiCredentialController.*`**（`@RequestMapping("/system/openApi")`）：`getSelfCredential` / `createSelf` / `resetSelf` / `enableSelf` / `disableSelf` / `deleteSelf` / `users` / `getUserCredential` / `createUser` / `resetUser` / `enableUser` / `disableUser` / `deleteUser`。没有 export，没有 PUT/DELETE，没有 catalog 的 `/interfaces`。
2. **`B:SysOpenApiCredentialController.create/reset`**：`createSelf`/`createUser` 与 `resetSelf`/`resetUser` 四扇窗共用 `SystemOpenApiCredentialService.create` / `reset`。性状是**密文存储、列表不回显**；签发响应里的明文只出现这一次，并且带 `Cache-Control: no-store`。

模块模式是 **classic**：`Controller → SystemOpenApiCredentialService → SysOpenApiCredentialMapper`。登记表把 `wta-modules/wta-system` 标 classic。磁盘上**没有** UseCase、**没有** DAO、**没有** `ISysOpenApiCredentialService`、**没有** `SysOpenApiCredentialMapper.xml`。查询 SQL 写在 Mapper 接口的 `@Select` 上。类都挂 `@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")`。

本课不宣称你会拆 `SysOpenApiCatalogController` 自有/他人接口目录（L-030）、`wta-common-openapi` 网关 / HMAC / 机器会话怎么验签（L-032）、或 `createOpenApiService` 与 `openApi.vue`（L-033）。`OpenApiMachineSessionInvalidator.invalidateByUserId`、`SystemOpenApiCredentialResolver.decrypt` 只作为凭据写路径的**下游副作用 / 为什么必须存密文**出现，用来解释「重置之后旧机器人为什么进不来 / 管理端 GET 为什么从不解密」。

## 先把宏观地图放在桌上

L-005 已经把 HTTP 路径和基座表钉成公共合同。L-030 是同一前缀下的**目录窗**（哪些接口能调）。本课走进**钥匙柜台**：每一个登录用户在表 `sys_open_api_credential` 里最多有**一行未删除凭据**。对外公开的是 `appKey`（柜子编号）；真正开门的 `appSecret` 只在创建或重置成功的那一次放进信封。抽屉里锁着的是密文四件套：`secret_ciphertext` / `secret_nonce` / `secret_tag` / `kek_version`。列表再查、启停再查，信封不会再出现。

2026-09-16 工作树：Java 类在 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/openapi/SysOpenApiCredentialController.java`。**不** `extends BaseController`。只注入 `SystemOpenApiCredentialService`。自有窗的主人只来自 `LoginHelper.getUserId()`；他人窗先 `LoginHelper.isSuperAdmin()`，再走路径上的 `userId`。创建请求体 `OpenApiCredentialCreateRequest` **故意没有 owner 字段**。前端工厂在 `createOpenApiService()`，页面是 admin-web 的开放应用；格子仍归 L-033，本课只借它们证明 HTTP 动词、`no-store` 头、以及 Summary 投影会拒绝任何带 `secret` 的键。

```text
管理员浏览器 / 已登录调用方
    │
    ├─ /system/openApi/self/credential*     自己的钥匙（权限 system:openApi:self）
    └─ /system/openApi/users...             超管替别人办（list/query/add/edit/remove + 超管闸）
                │
                v
        SysOpenApiCredentialController      十三扇；无 PUT/DELETE
                │
                v
        SystemOpenApiCredentialService      classic：Service 直接持有 Mapper
                │  create/reset：生成明文 → AES-256-GCM → 只把密文落库
                │  enable/disable/delete：改状态或逻辑删，不回明文
                ├─ SysOpenApiCredentialMapper   无 XML；@Select
                v
        表 sys_open_api_credential
                活行：uk_active_owner 保证每用户一行
                密文四列 NOT NULL；app_key 全局唯一（含已删行）
                │
                ├─ 写成功后（reset/enable/disable/delete）
                │     OpenApiMachineSessionInvalidator.invalidateByUserId
                │     （create 不调用）
                └─ 机器调用时才解密（本课不讲 HMAC）
                      SystemOpenApiCredentialResolver → 内存里的 OpenApiCredential.secret
```

**类比：** 每人只准有一只活着的铁皮柜。柜门上喷着编号 `appKey`，谁都能念。真正的钥匙 `appSecret` 只在开柜或换锁那天装进信封交给你。柜子里不放钥匙，只放一只用大楼总钥匙（KEK）封好的铁盒。前台再问「我的柜还在吗 / 先停用 / 再启用」，只给你看柜门编号和名牌，绝不拆铁盒。换锁时编号不变，旧钥匙作废，信封再给你一次。拆柜（逻辑删除）会把「每人一只活柜」的名额腾出来，但旧编号还占着全局唯一格子，新开的柜会拿到新编号。

**类比失效处：**

1. 这不是 OSS 的 `status=Y/N` 正门贴纸。这里的 `status` 真的是启停：`'0'` 启用、`'1'` 停用。
2. 这不是 L-018 的 SSO 密钥。SSO 库里是 BCrypt **单向哈希**；这里是 AES-256-GCM **可逆密文**，机器网关以后要解密才能算 HMAC（L-032）。管理端 GET **仍然不解密**。
3. 「钥匙只给一次」不是「创建接口也不返回明文」。签发盒 `OpenApiCredentialIssued` **必须**带 `appSecret`。不回显指的是：表、Summary、列表、启停、日志、GET 合同。
4. 超管窗不是万能钥匙：超管也拿不到密文回显，只是能替别人 create/reset 再收一次信封。
5. 封盒用的 KEK 不在 Java 源码里写死；本地 `application.yml` 给了 `OPENAPI_KEK` 的开发默认值。换 owner / 改 tag / 缺旧版 KEK，解密会关死成同一句 `OpenAPI credential crypto unavailable`。

## 核心概念与机制

### 直觉讲解

先记住两只盒子，再背路径：

- **名牌盒 `OpenApiCredentialSummary`。** 注释原文：*Persisted credential fields safe for repeated transport*。字段只有 `credentialId` / `ownerUserId` / `appKey` / `appName` / `status` / `expiresAt` / `remark` / 时间。没有 `appSecret`，没有密文四列。GET、启停、用户列表里的嵌套凭据都用它。
- **信封盒 `OpenApiCredentialIssued`。** 注释原文：*One-time create/reset result. AppSecret is never part of a summary model.* 只比 Summary 多一个 `appSecret`。只有 create / reset 的返回类型是它。

再记住两扇柜台和一把锁：

- **自己的柜台 `/self/credential*`。** 主人 = 当前登录用户。五扇写窗权限全是 `system:openApi:self`。
- **超管柜台 `/users/{userId}/credential*` 加 `/users`。** 权限按 list/query/add/edit/remove 切开，但 Java 里每一扇还要再过 `requireSuperAdmin()`。只有权限串、不是超管 → 文案 `OpenAPI credential management is unavailable`（这扇**没**写 404 码）。
- **每人一行活凭据。** 生成列 `active_owner_user_id`：未删除时等于 `owner_user_id`，删除后变 `NULL`。唯一索引 `uk_sys_open_api_credential_active_owner` 卡住「活着的柜」。`app_key` 另有全局唯一索引，**不**随删除释放。

「点了详情为什么看不见密钥 / 重置会不会换 appKey / 停用算不算删」先问五句话，不要先怪前端没渲染：

1. 这次 HTTP 返回类型是 `Issued` 还是 `Summary`？
2. 这扇窗有没有 `Cache-Control: no-store`？（只有 create/reset 设）
3. `@Log` 的 `isSaveRequestData` / `isSaveResponseData` 是不是都是 `false`？（十扇写窗全是）
4. 库里那一行是密文四列，还是你以为会有 `app_secret` 明文列？
5. 机器会话作废发生在 reset/enable/disable/delete；**create 不调用** invalidator。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 开放凭据 | OpenAPI credential | 实体 `SysOpenApiCredential`，表 `sys_open_api_credential` |
| 凭据主键 | `openApiCredentialId` | 列 `open_api_credential_id`；`IdentifierGenerator.nextId` |
| 所属用户 | owner user | 列 `owner_user_id`；自有窗来自登录，他人窗来自路径 |
| 活所属唯一键 | active owner | 生成列 `active_owner_user_id`；`FieldStrategy.NEVER`，禁止 MP 写入 |
| 公开编号 | `appKey` | 16 字节随机 → Base64URL 无填充；reset **保持** |
| 一次性钥匙 | `appSecret` | 32 字节随机 → Base64URL 无填充；只出现在 `Issued` |
| 安全摘要 | summary | record `OpenApiCredentialSummary`；可重复运输 |
| 一次性签发 | issued | record `OpenApiCredentialIssued`；create/reset 专用 |
| 创建请求 | create request | `OpenApiCredentialCreateRequest`：`appName` 必填，**无 owner** |
| 密文四件套 | ciphertext bundle | `secretCiphertext` + `secretNonce`(12) + `secretTag`(16) + `kekVersion` |
| 信封算法 | AES-256-GCM | `Cipher.getInstance("AES/GCM/NoPadding")`；AAD 绑 owner/appKey/version |
| 总钥匙 | KEK | `OpenApiKekProvider`；现行 `openapi.kek` + `openapi.kek-version`；旧版 `openapi.keks.<ver>` |
| 启用停用 | enable / disable | `status` `'0'` / `'1'`；**不是** OSS 的 Y/N |
| 逻辑删除 | `@TableLogic` | `del_flag`；`deleteById` 把活唯一键变成 NULL |
| 乐观锁 | `@Version` | 列 `version`；`updateById != 1` → 409 |
| 冲突 | conflict | `ServiceException(..., HttpStatus.CONFLICT)` = 409，文案 `OpenAPI credential already changed` |
| 找不到 | unavailable | `ServiceException(..., HttpStatus.NOT_FOUND)` = 404，文案 `OpenAPI credential is unavailable` |
| 机器会话作废 | machine session invalidation | `OpenApiMachineSessionInvalidator.invalidateByUserId`；L-032 才讲会话结构 |
| 凭据解析 SPI | credential resolver | `SystemOpenApiCredentialResolver` 实现 common SPI；机器路径才 `decrypt` |
| 禁止缓存信封 | `Cache-Control: no-store` | create/reset 的 `HttpServletResponse` 头 |
| 操作日志脱敏 | safe `@Log` | 十扇写窗 `isSaveRequestData=false` 且 `isSaveResponseData=false` |
| 开关属性 | feature flag | `@ConditionalOnProperty` `openapi.enabled=true` |
| 经典分层 | classic | Controller → Service → Mapper；本切片无 XML |
| 超管闸 | super-admin gate | `LoginHelper.isSuperAdmin()`；权限串不够 |

权限串不要混：自有六扇（含 GET）全挂 `system:openApi:self`。超管：`users` 挂 **list**，`getUserCredential` 挂 **query**，create 挂 **add**，reset/enable/disable 挂 **edit**，delete 挂 **remove**。基座 `50-cde-base-dml.sql` 的 `NAMEWTA-OPENAPI-CREDENTIAL-DML-001` 种子了这六串和菜单 `system/openApi/index`。

HTTP 合同（API-005）：查询 GET，变更 POST。本 Controller **零**个 `@PutMapping` / `@DeleteMapping`。单测 `everyWriteUsesPostExactPermissionAndSafeOperationLogging` 钉死十扇写窗的 POST 路径后缀 `/credential/{create|reset|enable|disable|delete}`。没有 `@RepeatSubmit`。

`OpenApiProperties` Java 字段 `enabled` 默认是 `false`（未绑定前）；`wta-admin` 的 `application.yml` 写成 `openapi.enabled: ${OPENAPI_ENABLED:true}`。口试以**运行配置 + `@ConditionalOnProperty(havingValue="true")`** 为准：关掉以后这些 Bean 不进容器，不是返回空 Summary。

### 机制/因果链

#### 1. 十三扇窗在磁盘上的真实映射

文件：

- `.../controller/system/openapi/SysOpenApiCredentialController.java`
- `.../openapi/credential/service/SystemOpenApiCredentialService.java`
- `.../openapi/credential/mapper/SysOpenApiCredentialMapper.java`

类注解：`@Validated` `@RequiredArgsConstructor` `@RestController` `@ConditionalOnProperty`。同一 URL 前缀上还有 `SysOpenApiCatalogController`（L-030），不要把 `/self/interfaces` 说进本课格子。

| HTTP | 动词 | Java | 权限 | 超管闸 | 返回 | 写库？ | 回显明文？ |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `/system/openApi/self/credential` | GET | `getSelfCredential` | self | 否 | `Summary` | 否 | 否；null → 404 |
| `/system/openApi/self/credential/create` | POST | `createSelf` | self | 否 | `Issued` | 是 | **一次**；`no-store` |
| `/system/openApi/self/credential/reset` | POST | `resetSelf` | self | 否 | `Issued` | 是 | **一次**；`no-store` |
| `/system/openApi/self/credential/enable` | POST | `enableSelf` | self | 否 | `Summary` | 是 | 否；会作废会话 |
| `/system/openApi/self/credential/disable` | POST | `disableSelf` | self | 否 | `Summary` | 是 | 否；会作废会话 |
| `/system/openApi/self/credential/delete` | POST | `deleteSelf` | self | 否 | `R<Void>` | 是（逻辑删） | 否；会作废会话 |
| `/system/openApi/users` | GET | `users` | list | **是** | `List<UserSummary>` | 否 | 否；嵌套 Summary |
| `/system/openApi/users/{userId}/credential` | GET | `getUserCredential` | query | **是** | `Summary` | 否 | 否；null → 404 |
| `/system/openApi/users/{userId}/credential/create` | POST | `createUser` | add | **是** | `Issued` | 是 | **一次**；`no-store` |
| `/system/openApi/users/{userId}/credential/reset` | POST | `resetUser` | edit | **是** | `Issued` | 是 | **一次**；`no-store` |
| `/system/openApi/users/{userId}/credential/enable` | POST | `enableUser` | edit | **是** | `Summary` | 是 | 否 |
| `/system/openApi/users/{userId}/credential/disable` | POST | `disableUser` | edit | **是** | `Summary` | 是 | 否 |
| `/system/openApi/users/{userId}/credential/delete` | POST | `deleteUser` | remove | **是** | `R<Void>` | 是 | 否 |

自有写窗的 owner **只**来自 `LoginHelper.getUserId()`。合同测试 `selfOwnerComesOnlyFromLoginContextAndAdminRechecksSuperAdmin`：`createSelf` 调 `service.create(41L, request)`，响应头 `Cache-Control=no-store`；非超管调 `getUserCredential(42L)` 直接抛管理不可用。

`users` 的 `limit` 默认 50。Service 再夹成 `Math.max(1, Math.min(requestedLimit, 100))`。SQL **不选**密文列：只投影用户三列 + 凭据 id/appKey/appName/status/expires/remark/时间。没有凭据时 `credential` 为 null。`where` 只滤 `u.del_flag='0'`，**不过** `u.status='0'`，所以停用账号仍可能出现在超管名单里。

#### 2. OBJ-31 主链 A：create 如何把明文变成密文

`create(Long ownerUserId, OpenApiCredentialCreateRequest request)` 打 `@DSTransactional`。顺序按磁盘：

1. `requiredOwner`：`ownerUserId == null` → 404 `OpenAPI credential is unavailable`。密钥材料还没生成。
2. `existsActiveOwner(owner)`：`sys_user` 必须 `status='0'` 且 `del_flag='0'`。失败 → 同样 404。单测 `createRejectsAnUnknownOrDisabledOwnerBeforeGeneratingMaterial` 钉死：**此时 `crypto.generate` 一次都不会被调用**。
3. `selectByOwnerUserId(owner) != null` → 409。已经有活凭据，不能再开一只柜。
4. `crypto.generate(owner)`：16 字节 `appKey` + 32 字节 `appSecret`，立刻 `encrypt(owner, appKey, appSecret)`。
5. 新实体：主键 `identifierGenerator.nextId`，`appName=request.appName().trim()`，`status=ENABLED("0")`，`expiresAt` / `remark`（空白 remark 收成 null），`version=0`，`delFlag="0"`。`applyEncrypted` 只写密文四列。
6. `mapper.insert != 1` 或 `DuplicateKeyException`（活所属冲突或 `app_key` 撞车）→ 409。
7. 返回 `issued(credential, generated.appSecret())`。明文来自**刚才生成的内存字符串**，不是 `decrypt` 回读。

没有做的事（口试要主动说）：

- 不把 `appSecret` 写成表列。DDL 没有 `app_secret`。
- 不调用 `sessionInvalidator`。新柜还没有对应的机器会话要杀。
- 不改 `appKey` 之外的公开编号策略：编号在这一步定终身，直到这行逻辑删除后再创建才会拿新号。
- 不信任请求体里的 owner。`CreateRequest` 只有 `appName` / `expiresAt` / `remark`；`expiresAt` 有 `@Future`。

加密细节（矩阵 B 要能说到列，不必背 HMAC）：

- 算法 AES-256-GCM，nonce 12 字节，tag 16 字节，KEK 必须恰好 32 字节。
- AAD = ASCII `NAMEWTA-OPENAPI-CREDENTIAL-V1` + `ownerUserId` + `appKey` + `kekVersion`。换 owner 或改 tag 都会变成 `OpenApiCredentialCryptoException("OpenAPI credential crypto unavailable")`，没有诊断细节。
- `finally` 里把 KEK 拷贝和明文字节数组 `fill(0)`。这是内存擦除，不是「HTTP 不回显」的那一层。
- 现行 KEK 来自 `openapi.kek` + `openapi.kek-version`；解密旧行可走 `openapi.keks.<version>`。Java `PropertyOpenApiKekProvider` **没有**源码级默认密钥。

#### 3. OBJ-31 主链 B：reset 换钥匙、不换编号

`reset(ownerUserId)` 同样 `@DSTransactional`：

1. `requiredCredential`：按 owner 取活行，没有 → 404。
2. `crypto.generateSecret()` 只做新 `appSecret`，**不** `generate()` 新 `appKey`。
3. `crypto.encrypt(ownerUserId, 旧 appKey, 新 secret)`。KEK 用**当前** `activeVersion()`，所以重置也能把旧版密文迁到新 KEK。
4. `updateAndInvalidate`：`updateById != 1` → 409（乐观锁/并发）；成功则 `invalidateByUserId(owner)`。
5. 返回 `issued(credential, secret)`，再次带明文，再次由 Controller 设 `no-store`。

`appName` / `expiresAt` / `remark` / `appKey` 原样留下。口试不要把 reset 说成「重新创建」。单测 `resetKeepsAppKeyAndEveryStateMutationInvalidatesMachineSession`：reset 后 `appKey` 仍是 `app-key`，新秘密是 `new-secret`；reset + disable + enable + delete 合计 **4** 次 `invalidateByUserId`。

#### 4. 启停与删除：不回明文，但会踢机器会话

`enable` / `disable` 都进 `changeStatus(owner, "0"|"1")`：取出活行，改 `status`，再 `updateAndInvalidate`，返回 `summary(...)`。即使本来就是启用，再 enable 一次仍会 `updateById` + 作废会话。返回类型是 Summary，JSON 里不该出现 `appSecret`。

`delete`：`requiredCredential` → `mapper.deleteById`（`@TableLogic`，不是物理删）→ 不是 1 行则 409 → `invalidateByUserId`。Controller 返回 `R.ok()`，没有 data 盒子。逻辑删除后 `active_owner_user_id` 变成 NULL，同一用户可以再 create；新行会拿到**新** `appKey`，因为旧 `app_key` 仍占着 `uk_sys_open_api_credential_app_key`。

`updateAndInvalidate` 把作废放在**同一** `@DSTransactional` 里、写库成功之后。单测 `invalidationFailureFailsTheTransactionalWriteExplicitly`：invalidator 抛 `IllegalStateException("redis down")`，disable 把异常原样抛出——库写入要跟着回滚，不能出现「库已停用、机器会话还活着」。create 不走这条链。

`get` / `users` 不是写方法，不打 `@DSTransactional`，不解密，不碰 invalidator。Controller 的 GET 把 Service 返回的 `null` 再包成 404；Service `get` 本身只 `summary(select...)`，空行就是 `null`。

#### 5. 「列表不回显」叠了几道闸，不是一句注释

按从里到外：

1. **表。** 明文列不存在。`secret_ciphertext varbinary(512)` / `secret_nonce binary(12)` / `secret_tag binary(16)` / `kek_version` 全 NOT NULL。
2. **查询。** `selectUserSummaries` 不选密文列。`selectByOwnerUserId` 虽然 `select *`，但下一层不把它放进 HTTP 模型。
3. **record。** `summary()` / `userSummary()` 构造函数参数表里没有 secret。单测断言 `get()` 的 record 组件名 **不含** `appSecret` / `secretCiphertext` / `secretNonce` / `secretTag` / `kekVersion`。
4. **HTTP。** GET `/self/credential` 的 MockMvc 断言 `$.data.appSecret` 不存在、`$.data.secretCiphertext` 不存在；POST create 才有 `$.data.appSecret=one-time-secret` 且 `Cache-Control=no-store`。测试方法名叫 `transportShowsSecretOnlyForCreateAndUsesNoStore`——名字没写 reset，但 Controller 上 `resetSelf`/`resetUser` 的返回类型同样是 `Issued`，不要被测试名带窄。
5. **日志。** 十扇写窗 `@Log(..., isSaveRequestData=false, isSaveResponseData=false)`，避免操作日志把信封抄走。
6. **前端投影（L-033 的篱笆，本课只作旁证）。** `projectOpenApiCredentialSummary` 看见任何键名包含 `secret` 就抛 `OpenAPI 响应不可用`；`projectOpenApiCredentialIssued` 只允许键 `appSecret`，空串也拒。

机器路径的 `SystemOpenApiCredentialResolver.resolve(appKey)` **会**解密，把明文放进 common SPI 的 `OpenApiCredential`。那是网关内存对象，不是这十三条 HTTP。未知 / 停用 / 过期 / 解密失败在解析器里收成同一类 `OpenApiAuthenticationException`（L-032 的失败关闭）。管理端 GET 根本不走解析器。

### 图、表或文本图

**图 1. 十三扇窗、两只盒子、一只抽屉**

```text
 /system/openApi
  GET  /self/credential                         Summary；无明文；空 → 404
  POST /self/credential/create                  Issued + no-store
  POST /self/credential/reset                   Issued + no-store；appKey 不变
  POST /self/credential/enable|disable          Summary；作废机器会话
  POST /self/credential/delete                  Void；逻辑删 + 作废
  GET  /users                                   超管；UserSummary.credential 仍是 Summary
  GET  /users/{id}/credential                   超管 + query
  POST /users/{id}/credential/create|reset|...  与 self 同一 Service 方法
        │
        v
 SystemOpenApiCredentialService
        │  create / reset = 矩阵 B
        v
  sys_open_api_credential
        app_key 公开
        secret_ciphertext/nonce/tag/kek_version  密文四件套
        status '0'|'1'
        active_owner_user_id  生成列，活行唯一
        │
        ├─ GET/列表/启停 ──► OpenApiCredentialSummary   （没有 appSecret）
        ├─ create/reset ──► OpenApiCredentialIssued     （多一个 appSecret）
        └─ 机器调用（L-032）──► Resolver.decrypt 内存明文
```

- **alt：** OpenAPI 凭据十三条 HTTP 窗写入同一张表；只有 create/reset 返回一次性明文，表中只存 GCM 密文。
- **caption：** 图 1——OBJ-31 的空间关系。矩阵 A 是十三扇窗；矩阵 B 是 create/reset 的密文与不回显。
- **文字等价物：** 自己的钥匙走 `/self/credential*`，超管替别人走 `/users/{id}/credential*`。两边的 Java 方法最后都打电话给同一份 Service。持久化只有 `sys_open_api_credential`。公开编号是 `appKey`。明文没有自己的列。列表和详情用 Summary；只有签发那一次出现在 JSON 的 `appSecret`。
- **图的边界：** 图上没有 `/self/interfaces`、没有 HMAC 规范化、没有 nonce 限流、没有 `openApi.vue`。不要把 PUT/DELETE 画进这十三扇。不要把 `status` 画成 OSS 正门。

**图 2. 签发成功才把信封递出；失败停在哪一层**

```text
create:
  owner 空/用户停用或已删 ──► 404，不 generate
  已有活凭据 ──► 409，不 generate
  generate + encrypt ──► insert
       insert 撞唯一键 ──► 409
       成功 ──► Issued(appSecret) + no-store
       不作废会话

reset:
  无活行 ──► 404
  新 secret + 旧 appKey 再加密 ──► update
       update != 1 ──► 409
       成功 ──► invalidateByUserId ──► Issued(appSecret) + no-store
       invalidate 抛错 ──► 整段事务失败（库回滚）

enable/disable/delete:
  无活行 ──► 404
  改 status 或逻辑删 ──► update/delete != 1 ──► 409
  成功 ──► invalidate ──► Summary 或 Void（无 appSecret）
```

- **alt：** 创建在生成密钥前先查用户和是否已有凭据；重置保持 appKey；作废会话失败会回滚写库。
- **caption：** 图 2——create/reset/启停/删除的失败停点。
- **文字等价物：** 给停用用户办新钥匙，服务端在随机数出现之前就 404。已经有活柜再 create 是 409。重置成功会同时换密文和踢掉旧机器人会话；Redis 作废失败时，表里的旧密文也应留下（事务回滚）。启停和删除把信封收起来，只改状态或 del_flag。
- **图的边界：** 不保证 invalidator 的 Redis 键长什么样（L-032）。不保证前端弹窗如何展示一次性密钥（L-033）。

### 正例、反例与边界

**正例 1：** 用户 41 没有活凭据，且 `sys_user` 启用未删。`POST /system/openApi/self/credential/create` body `{"appName":"billing"}`。库插入密文四列、`status='0'`、`app_key` 新值。响应 `data.appSecret` 有值，响应头 `Cache-Control: no-store`。随后 `GET /self/credential` 仍能看见同一个 `appKey`，但 JSON **没有** `appSecret`。

**正例 2：** 同一行再 `POST .../reset`。响应又有新 `appSecret`，`appKey` 不变。`invalidateByUserId(41)` 被调用。旧信封作废。

**正例 3：** 超管对用户 42 `POST /users/42/credential/disable`。返回 Summary，`status='1'`。解析器侧 `selectUsableByAppKey` 即使还能联到用户，也会因 `status != '0'` 在解密前失败（单测 `resolverRejectsUnknownDisabledExpiredAndCryptoFailuresWithOneCategory`：停用行 `crypto.decrypt` **never**）。

**正例 4：** `users?keyword=demo&limit=20` 的 SQL 左连凭据但不选密文。没有凭据的用户 `credential=null`。Service 把 limit 夹到 1..100。

**正例 5：** 十扇写窗的 `@Log` 都不存请求/响应体。合同测试按方法名断言 `BusinessType.INSERT/UPDATE/DELETE` 与权限串一一对应。

**反例 1：** 把 GET 详情当成「再看一次密钥」。类型是 Summary；MockMvc 断言 `appSecret` 不存在。要新密钥只能 reset，并且会作废机器会话。

**反例 2：** 第二次 create。`selectByOwnerUserId` 已有行 → 409 `OpenAPI credential already changed`。并发下 insert 撞 `uk_active_owner` 同样 409。

**反例 3：** 给停用/删除/不存在的 owner create。404，且 `crypto.generate` 不被调用。不要说「先生成再发现用户不合法」。

**反例 4：** 非超管带 `system:openApi:query` 去打 `/users/42/credential`。`requireSuperAdmin()` 仍拒绝。权限串和超管闸是 **且**，不是或。

**反例 5：** 在 `Issued` 和 `Summary` 之间混用前端投影。Summary 投影看见 `appSecret` 会当响应不可用扔掉。这是 L-033 的篱笆，但能解释「后端已经不回显，前端再收到 secret 字段也会炸」。

**反例 6：** 把 create 成功理解成「也要踢会话」。磁盘上 create 不调 invalidator。旧会话若还在，只可能来自**上一行已被删除的凭据**——那次 delete 才该已经踢过。

**反例 7：** 手改 `generated/openapi.ts` 给 Summary 加 `appSecret`。运输箱的 schema 注释已经写 *AppSecret is never part of a summary model*。手改会被 L-010 的 check 判漂移。

**边界：**

- `status` 运行时只当 `'0'`/`'1'` 用。前端 Summary 投影也只放行这两个字符。
- `expiresAt` 空 = 永久。解析器判断过期是 `expiresAt != null && !expiresAt.isAfter(now)`。create 校验 `@Future`，reset **不改**过期时间。
- 逻辑删除释放的是 `active_owner_user_id`，不是 `app_key`。重建一定换编号。
- `selectByOwnerUserId` 不 join 用户状态；停用账号的活凭据，超管仍能 GET/reset/启停/删除。机器调用 `selectUsableByAppKey` 才要求用户 `status='0' AND del_flag='0'`。
- `selectUsableByAppKey` 不在本 Controller 调用链上。本课点名它，只为说明「停用后网关侧不解密」。
- 乐观锁：并发 reset 与 disable，后到的 `updateById != 1` → 409。
- KEK 不是 32 字节、版本名不匹配 `[A-Za-z0-9._-]{1,64}`、旧版 `openapi.keks.*` 缺失：加密/解密都关死成同一句，不把密钥诊断写进 HTTP。
- `OPENAPI_ENABLED=false`：Controller/Service/Crypto/Resolver Bean 都不注册。不是返回空列表。
- 用户被禁用、角色被改、用户被删时，`SysUserServiceImpl` / `SysRoleServiceImpl` 也会 `invalidateByUserId`（邻居课）。本课 HTTP 不是唯一踢会话入口。
- 本切片无 `@RepeatSubmit`。前端 create/reset 自己带 `repeatSubmit: false` 与 `Cache-Control: no-store`（L-033）。
- classic 允许 Service 持有 Mapper；不要为这十三条新建 UseCase 或 DAO。

## 变式与迁移

- **变式 A：自己办钥匙 vs 超管代办。** 同一 `create/reset/enable/disable/delete`。差别只在 owner 来源和权限+超管闸。验收：自有窗即使用 body 伪造别人的 userId 也没用，请求体没有这个字段。
- **变式 B：密钥泄露。** 走 reset，不要 delete+create——除非你故意要换 `appKey`。reset 保持编号、换密文、踢会话、再给一次信封。delete+create 会换编号，旧 `app_key` 永久占坑。
- **变式 C：临时冻结机器人。** disable。Summary 回 `status='1'`，会话作废，密文仍在。再 enable 仍不回明文；机器人必须用**原来那把还没 reset 的钥匙**重新走网关（L-032）。
- **变式 D：对照 L-018 SSO 轮换。** SSO：库里 BCrypt 哈希，VO 用 `ssoSecretOnce` 一次性贴明文，列表只留 `ssoSecretConfigured`。OpenAPI：库里 GCM 密文，HTTP 用另一个 record 携带 `appSecret`，列表模型连「已配置」布尔值都没有——有行就有密文，因为四列 NOT NULL。
- **变式 E：对照 L-025 OSS `changeStatus`。** OSS 的 status 是唯一正门，实现忽略 body 的 N。OpenAPI 的 enable/disable 真的写 `'0'`/`'1'`。不要把「启停」这词在两课里用成同一个抽屉。
- **变式 F：KEK 轮换。** 旧密文仍按行上的 `kek_version` 解密。reset 会按现行版本重加密。缺旧版密钥时，管理端 GET 仍能显示 Summary（不解密）；倒霉的是机器解析器，会关死。不要指望点一次详情来「检查密钥还在不在」。
- **变式 G：开关关掉 OpenAPI。** `OPENAPI_ENABLED=false`。凭据窗和目录窗的 Bean 一起消失。这不是本 Controller 里的 if。L-002 提过关的是协议网关；本课补一句：管理端这十三条也绑在同一个属性上。
- **迁移口诀：** 先问这枪是十三扇里哪一扇 → 再问返回类型是 Issued 还是 Summary → 再问表里有没有明文列（没有）→ create/reset 才 generate/encrypt 并 `no-store` → GET/列表/启停不解密 → reset/启停/删除才 invalidate，create 不 invalidate → 每人一行活柜，编号全局唯一含已删。跳步就会把目录接口、HMAC、前端页、OSS 正门说成同一按钮。

## 常见误区

1. **「明文不回显 = create 也不返回 appSecret。」** 签发盒必须返回。不回显的是表、Summary、列表、启停、日志、GET。
2. **「reset 会换 appKey。」** 只换 secret 和密文四列。
3. **「GET 会 decrypt 再藏起来。」** GET 构造 Summary，不碰 Crypto。
4. **「列表 SQL 只是忘了选 secret 列。」** 模型层就没有这个字段；合同测试还检查 JSON 缺省。
5. **「status 是 OSS 那种默认正门。」** 这里是启用/停用。
6. **「超管可以读出密钥。」** 超管也只在代 create/reset 时收信封；GET 他人凭据仍是 Summary。
7. **「有 `system:openApi:query` 就能打他人窗。」** 还要 `isSuperAdmin()`。
8. **「写操作是 PUT/DELETE。」** 全是 POST，路径里带动词。
9. **「create 也会踢机器会话。」** 不会。reset/enable/disable/delete 会。
10. **「作废会话失败库已经改完了。」** 同一 `@DSTransactional`；invalidator 抛错则写失败。
11. **「逻辑删除会释放 app_key。」** 只释放 `active_owner_user_id`。
12. **「这是 layered，该有 UseCase。」** `wta-system` 登记 classic；本切片 Service 直接持 Mapper。
13. **「`/self/interfaces` 也是凭据课。」** 那是 Catalog，L-030。
14. **「跟 L-018 一样是 BCrypt。」** 这里是 AES-256-GCM，因为网关以后要还原明文做签名。
15. **「本地 yml 有默认 KEK，所以 Java 也写死了密钥。」** Provider 从配置读；源码默认不存在。生产仍须注入。
16. **「合同测试名叫 OnlyForCreate，所以 reset 不回密钥。」** 看返回类型 `OpenApiCredentialIssued`，不要看测试方法名的省略。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `SysOpenApiCredentialController`。把十三对 mapping 抄成一张表：路径、动词、Java 名、权限串、返回类型、有没有 `no-store`、有没有超管闸。圈出：GET 两扇是 Summary；create/reset 四扇是 Issued；delete 两扇是 Void。
2. 用手指划 `createSelf` → `credentialService.create(LoginHelper.getUserId(), request)` → `existsActiveOwner` → `generate` → `applyEncrypted` → `insert` → `issued(..., generated.appSecret())`。在 `issued` 旁边写：明文从哪来（内存），表写了哪四列，有没有 invalidate。
3. 再划 `resetSelf` → `generateSecret` → `encrypt(旧 appKey)` → `updateAndInvalidate`。对照单测 `resetKeepsAppKeyAndEveryStateMutationInvalidatesMachineSession` 的 4 次 invalidate。
4. 打开 DDL `NAMEWTA-OPENAPI-CREDENTIAL-DDL-001`。抄下 `active_owner_user_id` 的生成表达式、两个 unique key、密文四列类型。对照实体上 `FieldStrategy.NEVER` 与 `@TableLogic`。
5. 打开 `OpenApiCredentialSummary` 和 `OpenApiCredentialIssued` 两个 record。数字段差几个。再打开合同测试 `transportShowsSecretOnlyForCreateAndUsesNoStore` 的两条 JSON 断言。把测试方法名的省略补成：reset 的返回类型同样是 Issued。

## 总结、词汇表与下一步

- **十三扇窗。** 前缀 `/system/openApi`。GET 三扇只读（自己凭据 / 他人凭据 / 用户名单）；POST 十扇变更。classic：Controller → Service → 注解 Mapper。
- **两只盒子。** Summary 可重复运输，没有秘密。Issued 只给 create/reset，多一个 `appSecret`，并 `no-store`。
- **一只抽屉。** `sys_open_api_credential`。活行每人一行；`app_key` 全局唯一含已删；密文四列 NOT NULL；`status` `'0'`/`'1'`。
- **矩阵 B。** create：校验用户 → 拒重 → 生成编号和钥匙 → GCM 落库 → 内存明文进 Issued，不作废会话。reset：保持编号 → 新钥匙再加密 → update + invalidate → 再给一次信封。
- **启停/删除。** 回 Summary 或 Void；同一事务里踢机器会话；作废失败则回滚。
- **不回显是叠闸。** 无明文列；列表 SQL 不选密文；Summary 无该字段；GET JSON 断言缺省；写窗日志不存包体。机器解析器才解密，不在这十三条 HTTP 上。
- **格子按磁盘全表：** 不要把 catalog `/interfaces` 补进来。不要把 HMAC 验签说成本课主链。不要把「明文不回显」说成签发接口无密钥。

词汇表：SysOpenApiCredential / classic / `appKey` / `appSecret` / `OpenApiCredentialSummary` / `OpenApiCredentialIssued` / AES-256-GCM / KEK / AAD / `kek_version` / `active_owner_user_id` / `@TableLogic` / `@Version` / `@DSTransactional` / `invalidateByUserId` / `Cache-Control: no-store` / `system:openApi:self` / `requireSuperAdmin` / `openapi.enabled`。

下一步：L-032 走进 `wta-common-openapi` 网关、HMAC 规范化、nonce、限流、Sa-Token 机器会话——那些会话正是本课 reset/启停/删除要作废的对象，解密发生在解析器而不是管理端 GET。L-033 才把 `createOpenApiService` 与 admin-web 页面接到这十三条 HTTP，并讲解 Summary 投影如何拒绝秘密字段。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 房间内公开入口；本课 Controller 同模块 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记为 classic；禁止与 layered 混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `sys_open_api_credential` 密文列、生成列、两个 unique；菜单与 `system:openApi:*` 六串种子 | `NAMEWTA-OPENAPI-CREDENTIAL-DDL-001` / `DML-001` | 2026-09-16 |
| S-L031-01 | `SysOpenApiCredentialController.java` | 十三扇映射、权限、POST 写窗、自有 owner 来自登录、他人窗超管闸、create/reset 的 Issued + `no-store`、GET 空行 404、写窗 `@Log` 双 false | 类与各方法 | 2026-09-16 |
| S-L031-02 | `SystemOpenApiCredentialService.java` | create 先查用户再生成；reset 保持 appKey；密文四列；Summary/Issued 分流；enable/disable/delete 走 `updateAndInvalidate`；create 不 invalidate；`@DSTransactional` | `create` / `reset` / `changeStatus` / `delete` | 2026-09-16 |
| S-L031-03 | `SysOpenApiCredential.java`、`SysOpenApiCredentialMapper.java` | 表名、`FieldStrategy.NEVER` 生成列、`@Version`、`@TableLogic`；`selectByOwnerUserId` / `existsActiveOwner` / `selectUserSummaries` 不选密文；无 XML | 实体与 Mapper 注解 SQL | 2026-09-16 |
| S-L031-04 | `OpenApiCredentialIssued.java`、`OpenApiCredentialSummary.java`、`OpenApiCredentialCreateRequest.java`、`OpenApiCredentialUserSummary.java` | Issued 注释「secret 从不进 Summary」；CreateRequest 无 owner；`@Future` / `@NotBlank appName` | 四个 record | 2026-09-16 |
| S-L031-05 | `OpenApiCredentialCrypto.java`、`PropertyOpenApiKekProvider.java`、`OpenApiCredentialCryptoException.java` | AES-256-GCM、12/16、AAD 上下文、无源码默认 KEK、失败文案单一、旧版 `openapi.keks.*` | `encrypt` / `generate` / `keyForVersion` | 2026-09-16 |
| S-L031-06 | `SysOpenApiCredentialControllerContractTest.java`、`SystemOpenApiCredentialServiceTest.java`、`OpenApiCredentialCryptoTest.java`、`OpenApiCredentialSqlContractTest.java` | GET 无 appSecret；create 一次明文 + no-store；reset 保持 appKey 且四次 invalidate；作废失败炸事务；停用用户不 generate；DDL/权限种子 | 测试方法名 | 2026-09-16 |
| S-L031-07 | `SystemOpenApiCredentialResolver.java`、`OpenApiMachineSessionInvalidator.java`、`application.yml` `openapi:` 段 | 机器路径才 decrypt；停用不解密；invalidator 函数接口；`OPENAPI_ENABLED` 默认 true | `resolve`；yml 51–60 行附近 | 2026-09-16 |
| S-L031-08 | `frontend/packages/api-contracts/generated/openapi.ts` schema 注释；`domains/system/src/open-api/transport.ts`（仅旁证） | 运输箱 Issued vs Summary；Summary 投影拒绝 secret 键。不认 L-033 格子 | `OpenApiCredentialIssued` 描述；`projectCredential` | 2026-09-16 |
