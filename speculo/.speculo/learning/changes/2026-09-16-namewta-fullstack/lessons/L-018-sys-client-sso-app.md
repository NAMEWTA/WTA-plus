---
lesson_id: L-018
objective_ids: [OBJ-18]
claimed_cells: [A:SysClientController.list,export,getInfo,add,edit,rotateSsoSecret,bindSso,changeStatus,remove, A:SysSsoAppController.list,getInfo,add,edit,rotateSecret]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-windows-on-disk
    minutes: 10
  - segment: bind-rotate-and-secrets
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L018-01, S-L018-02, S-L018-03, S-L018-04, S-L018-05, S-L018-06, S-L018-07]
---

# Lesson 018：同一只抽屉，两扇窗——SysClient 与 SysSsoApp

## 学完你能做什么

打开 `wta-system` 里两份 Controller，你能**口述十四扇公开窗**，并说出它们其实都把纸塞进**同一张表** `sys_client`。不要把「客户端」和「SSO 应用」说成两张表、两个 Service、两套密钥。

本课认矩阵 (a) 两行，方法名以源码为准：

1. **`SysClientController`**（`@RequestMapping("/system/client")`）：`list` / `export` / `getInfo` / `add` / `edit` / `rotateSsoSecret` / `bindSso` / `changeStatus` / `remove`。
2. **`SysSsoAppController`**（`@RequestMapping("/system/ssoApp")`）：`list` / `getInfo` / `add` / `edit` / `rotateSecret`。

OBJ-18 特别点名的两扇：**SSO bind** 只在 Client 窗（`POST /system/client/sso/bind` → `bindSso`）；**rotate** 有两扇门牌、同一把里屋钥匙（Client 的 `rotateSsoSecret` 与 SsoApp 的 `rotateSecret` 都调用 `ISysClientService.rotateSsoSecret`）。

模块模式是 **classic**：`Controller → ISysClientService → SysClientServiceImpl → SysClientMapper → sys_client`。登记表把 `wta-modules/wta-system` 标 classic。磁盘上**没有** UseCase、**没有** DAO。Mapper XML 是空壳，查询走 `QueryBuilder` + `BaseMapperPlus`。

本课不宣称你会拆 `GET /sso/oauth2/authorize`、PKCE、token 换票（L-055 / L-056）、`SsoClientCatalog` 合同字段（L-059）、或前端 `createSystemService.clients / ssoApps` 全表（L-019）。本课要把**两扇管理窗的 HTTP 合同**和**密钥/接入副作用**讲完。

## 先把宏观地图放在桌上

L-011 的门卫 `AuthController` 用 `ISysClientService.queryByClientId` 查「这扇门开不开」。那一枪读的就是本课这张表。L-003 已经说过 system 是 classic 房间。本课走进房间里**管门牌的柜台**。

2026-09-16 工作树：两个 Java 类都在 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/`。两份都 `extends BaseController`，都只注入 **同一个** `ISysClientService`。`ISysClientService` 在 `org.namewta.system.service`，**不在** `wta-api`。主机 `wta-admin` 可以注入它（L-002）；邻居 `wta-sso` 只能经 `SsoClientCatalog` 只读（L-059）。

```text
管理员浏览器
    │
    ├─ /system/client/*     SysClientController     终端门牌窗
    └─ /system/ssoApp/*     SysSsoAppController     SSO 登记/交付窗
                │
                v
        ISysClientService
                │
                v
        SysClientServiceImpl          ← classic ServiceImpl，自己持有 Mapper
                │
                ├─ SysClientMapper    BaseMapperPlus；XML 空
                ├─ SsoAppRegistration / SsoClientFieldsSupport / SsoSecretHasher
                ├─ SsoAccessSupport   只给 bind 用
                ├─ ClientSessionService.kickoutClient   停用/换登录域才踢票
                └─ OpenApiMachineSessionInvalidator     可选；改登录域/状态时
                v
            表 sys_client             一行既是登录 Client，也是 SSO 应用
```

**类比：** 传达室有两扇窗，后面是**同一排铁皮抽屉**。左边窗给大楼发门牌（谁可以用密码登录、路径白名单、启停）。右边窗给来访公司办登记，并塞一个**只打开一次的信封**（SSO 密钥明文）。左边窗还可以在「回调地址已经写在抽屉里」之后，盖一枚「已接入」章（bind）。两扇窗都说「轮换密钥」时，里屋是同一把剪刀。

**类比失效处：**

1. 两扇窗不是两个仓库。`SysSsoAppController` 的注释写明「数据仍落 sys_client」。
2. 门牌上的 `clientSecret`（登录用、库里明文）不是信封里的 SSO 密钥（只存 BCrypt 哈希）。
3. 右边窗「创建应用」已经把 `ssoEnabled=true` 写进抽屉；bind **不是**创建，也**不是**授权码厅。
4. 右边窗的列表**没有**在服务端过滤「只显示 SSO 应用」。分页条件跟左边窗同一套 `queryPageList`。
5. 这不是 layered。不要在嘴里长出 `SsoAppUseCase`。

## 核心概念与机制

### 直觉讲解

先记住三个盒子，再背路径：

- **门牌 `clientId`：** 新增时用 `MD5(clientKey + clientSecret)` 算出来，入库后一般不再改。登录头上的 client 指这个字符串，不是表主键 `id`。
- **登录密钥 `clientSecret`：** 算门牌用。`sys_client.client_secret` 存的是原文。导出 Excel 的 `SysClientVo` 带这一列。操作日志在 add/edit 上 `excludeParamNames` 含 `clientSecret`，但**响应和表格仍可能看见它**。
- **SSO 密钥：** 没有明文列。只有 `sso_secret_hash`。HTTP 视图用 `@JsonIgnore` 的 `ssoSecretHash` 进内存，`fillView` 再把它清掉，改挂 `ssoSecretConfigured`。真正的明文只在创建/轮换成功的那一次，贴在 `ssoSecretOnce` 上。

「登记」和「接入」也是两步，不要并成一句 SSO：

- **登记：** 抽屉里已经有**精确回调** `ssoRedirectUris`（禁止 `*`、必须 http/https、要有 host、不能有 fragment）。SSO 管理窗的 `add`/`edit` 会走 `SsoAppRegistration.prepare`，强制 `ssoEnabled=true`，空回调直接拒。
- **接入（bind）：** Client 窗检查「已经登记」，再把 `ssoEnabled=true` 且 `ssoAuthMode` 写成 `sso` 或 `both`（空则默认 `both`）。不写回调、不签发密钥、不踢登录会话。

前端 Client 页用同一套规则画红绿：`ssoAccessState` 要 `ssoEnabled===true` **并且** 回调非空 **并且** 模式是 `sso`/`both`，才显示「已接入」，否则「没有接入」。那是展示，不是第二套后端状态机。

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 终端 Client / 门牌 | OAuth / login **client** | 实体 `SysClient`，表 `sys_client` |
| 主键 | primary key | `id`（`Long`）。getInfo / bind / rotate 的入参是它 |
| 门牌号 | `clientId` | `MD5(clientKey + clientSecret)`；`changeStatus` 和缓存键用它 |
| 客户端 key | `clientKey` | 人类可读名；唯一性校验方法名叫 `checkClickKeyUnique`（历史拼写 Click） |
| 登录密钥 | `clientSecret` | 明文列；**不是** SSO client secret |
| SSO 应用登记窗 | SSO app admin | `SysSsoAppController`，前缀 `/system/ssoApp` |
| 精确回调白名单 | exact redirect URI allow-list | `ssoRedirectUris` / `ssoRedirectUriList`；`SsoRedirectUris.validateExact` |
| 接入成功态 | bound / own-app SSO access | `SsoAccessSupport.isBound`；HTTP 是 `bindSso` |
| 一次性明文 | one-time plaintext | `ssoSecretOnce`；不是表字段 |
| 密钥哈希 | BCrypt hash | `ssoSecretHash`；`SsoSecretHasher.hash` |
| 轮换 | rotate secret | `rotateSsoSecret` / `rotateSecret` → 同一 Service 方法 |
| 登录模式 | auth mode | `local` / `sso` / `both` |
| 客户端种类 | client kind | `public` / `confidential` |
| 经典分层 | classic | Controller → Service → ServiceImpl → Mapper |
| 缓存名 | cache name | `CacheNames.SYS_CLIENT` = `"sys_client#30d"`，键是门牌 `clientId` |

权限串也是两套，不要混用：`system:client:list|query|add|edit|export|remove` 对左边窗；`system:ssoApp:list|query|add|edit` 对右边窗。基座 DML 里还有 `system:ssoApp:remove` 菜单点，但 **`SysSsoAppController` 没有 delete 映射**，前端 `ssoApps` 也没有 `delete`。口试若说「SSO 应用也能按菜单删」，那是菜单种子，不是本课 HTTP 合同。

### 机制/因果链

#### 1. 两扇窗在磁盘上的真实映射

文件：

- `.../controller/system/SysClientController.java`
- `.../controller/system/SysSsoAppController.java`

类注解两边都是 `@Validated` `@RequiredArgsConstructor` `@RestController`。没有 `@SaIgnore`：这些窗要登录，并且每扇再挂 `@SaCheckPermission`。

**Client 九扇（矩阵第一行）：**

| HTTP | 动词 | Java | 权限 | 写库？ | 本课必须能说的失败/副作用 |
| --- | --- | --- | --- | --- | --- |
| `/system/client/list` | GET | `list` | `system:client:list` | 否 | `queryPageList`；条件只有 clientId / clientKey / clientSecret / userTypeId / status |
| `/system/client/export` | POST | `export` | `system:client:export` | 否 | `@Log EXPORT`；`ExcelBuilder` 用 `SysClientVo`，含 `clientSecret` 列 |
| `/system/client/{id}` | GET | `getInfo` | `system:client:query` | 否 | 路径是主键 `Long id`，不是门牌；`@NotNull` |
| `/system/client` | POST | `add` | `system:client:add` | 是 | `@Validated(AddGroup)`；key 重复 → `R.fail` 文案带 key；insert 失败 → `R.fail("新增客户端失败")`；成功把 `ssoSecretOnce` 从 BO 贴回 VO |
| `/system/client` | PUT | `edit` | `system:client:edit` | 是 | `@Validated(EditGroup)`；`id` 必填；缓存驱逐键是 **`bo.clientId`** |
| `/system/client/sso/rotate-secret` | POST | `rotateSsoSecret` | `system:client:edit` | 是 | body 只要 `id`；`id==null` → `R.fail("主键不能为空")`；`@Log` `isSaveResponseData=false` |
| `/system/client/sso/bind` | POST | `bindSso` | `system:client:edit` | 是 | body：`id` + 可选 `ssoAuthMode`；未登记抛「未在 SSO 管理登记，无法接入」 |
| `/system/client/changeStatus` | PUT | `changeStatus` | `system:client:edit` | 是 | body 用 **门牌 `clientId` + `status`**，不是主键；`toAjax` |
| `/system/client/{ids}` | DELETE | `remove` | `system:client:remove` | 是（逻辑删） | `@NotEmpty` 的 `Long[]`；`deleteWithValidByIds(..., true)` 的 `isValid` **方法体未用** |

`add` / `edit` / `rotateSsoSecret` / `bindSso` 带 `@RepeatSubmit()`（默认 5000ms）。`list` / `getInfo` / `export` / `changeStatus` / `remove` 不带。

**SsoApp 五扇（矩阵第二行）：**

| HTTP | 动词 | Java | 权限 | 写库？ | 本课必须能说的差别 |
| --- | --- | --- | --- | --- | --- |
| `/system/ssoApp/list` | GET | `list` | `system:ssoApp:list` | 否 | **同一** `queryPageList`。Controller **不**加 `ssoEnabled=true` 条件 |
| `/system/ssoApp/{id}` | GET | `getInfo` | `system:ssoApp:query` | 否 | 同一 `queryById`；哈希不回显 |
| `/system/ssoApp` | POST | `add` | `system:ssoApp:add` | 是 | 先 `SsoAppRegistration.prepare`；空 `ssoSecret` 则 `SsoSecretHasher.generatePlaintext()`（48 位）；再 unique + `insertByBo`；`@Log` 排除密钥且 `isSaveResponseData=false` |
| `/system/ssoApp/update` | POST | `edit` | `system:ssoApp:edit` | 是 | **不是 PUT**。仍 `prepare`（强制启用 SSO）。成功同样可贴一次 `ssoSecretOnce` |
| `/system/ssoApp/rotateSecret` | POST | `rotateSecret` | `system:ssoApp:edit` | 是 | 路径是 camelCase `rotateSecret`，不是 kebab。里屋与 Client 窗同一方法 |

右边窗**没有** export、bind、changeStatus、remove。要停用或删除一行，走左边窗。

#### 2. classic 里屋：`insertByBo` / `updateByBo`

`SysClientServiceImpl` 实现 `ISysClientService`，字段注入 `SysClientMapper`（以及角色、登录域、会话、用户类型关系 Mapper）。这就是 classic：ServiceImpl 直接持有 Mapper。

新增因果链（两扇窗的 add 最后都进这里，SsoApp 只是先 `prepare` + 可能预填明文）：

1. `SsoClientFieldsSupport.validate(bo)`。`ssoEnabled==true` 时回调不能空、不能通配。
2. MapStruct 转 `SysClient`。
3. `validClientPolicy`：登录域必有且状态 `"0"`。**新增时只要带了 `defaultRoleId` 一律抛「默认角色必须属于当前客户端」**（`isAdd==true` 使该分支恒真）。想挂默认角色，只能先落行，再改。
4. `registerEnabled` 空则 `false`。
5. `grantType` 用 `grantTypeList` 按逗号拼起来（`StringUtils.SEPARATOR` 是 `","`）。
6. 路径/IP 规则归一：`*` 或 `/**` 写成 `/**`；路径没前导 `/` 就补上。
7. `SsoClientFieldsSupport.apply`：写 SSO 字段；有明文就哈希；confidential 且库里还没有哈希则现场生成。
8. **`clientId = SecureUtil.md5(clientKey + clientSecret)`**。只在 insert。改 `clientSecret` 不会重算门牌。
9. `clientMapper.insert`。成功才 `bo.setId` / `bo.setSsoSecretOnce`，并按登录域作废 OpenAPI 机器会话。
10. Controller 再 `queryById`，把一次性明文贴到 VO 上返回。

修改链类似，但：

- 用已有行的 `ssoSecretHash` 当 `existingHash`；空明文就保留旧哈希。
- `@CacheEvict(SYS_CLIENT, key="#bo.clientId")`。表单没带回门牌字符串时，驱逐键是 null。
- 登录域变了，或状态变成停用 `"1"`：`clientSessionService.kickoutClient(db.getId())`——这里的参数是**主键**，按 `LoginUser.clientPk` 踢 Sa-Token。
- 登录域 / 默认角色 / 状态任一变化：再作废该登录域下用户的 OpenAPI 会话。
- **只改注册开关不清 Token**（方法注释写明）。

查询链：`queryById` / `queryPageList` / `queryList` 都 `fillClientRuleFields`：拆 `grantTypeList`，规范化路径列表，`fillView` 清哈希、标 `ssoSecretConfigured`，再补登录域名和默认角色名。`queryByClientId` 另加 `@Cacheable(SYS_CLIENT, key="#clientId")`，给门卫和 SSO 目录热路径用。

#### 3. bind：盖「已接入」章

`POST /system/client/sso/bind` → `bindSso` → `bindSsoAccess(id, ssoAuthMode)`：

1. `selectById`。没有行 → `requireRegistered` 抛「客户端不存在」。
2. `ssoRedirectUris` 空白（含只有空格）→ 「未在 SSO 管理登记，无法接入」。单测 `bindSsoAccessRejectsUnregisteredClient` 钉的就是这句。
3. `requireBindMode`：空当 `"both"`；只接受 `"sso"` / `"both"`。`"local"` → 「接入成功态要求登录模式为 sso 或 both」。
4. `lambda set ssoEnabled=true, ssoAuthMode=mode where id`。
5. `@CacheEvict(SYS_CLIENT, allEntries=true)`。
6. 返回 `queryById`。**不**生成密钥，**不**改回调，**不** `kickoutClient`。

SSO 管理窗的 add 已经 `prepare` 把 `ssoEnabled=true`、默认 `both`。所以右边窗创建成功后，前端 `ssoAccessState` 往往已经是 `bound`，e2e 才会在 Client 列表上直接看到「已接入」。bind 是留给「回调已经在抽屉里、但还没盖成功章」的终端（例如左边窗先以 `ssoEnabled=false` 写下合法回调，再点接入）。

#### 4. rotate：两扇门牌，一把剪刀

两条 HTTP：

- `POST /system/client/sso/rotate-secret`（kebab）
- `POST /system/ssoApp/rotateSecret`（camel）

都是：`id==null` 则 Controller 自己 `R.fail`；否则 `rotateSsoSecret(id)`：

1. 没有行 → `ServiceException("客户端不存在")`。
2. **不**检查是否已登记、是否 `ssoEnabled`、是不是 confidential。本地 `local` 终端也能轮换。
3. `SsoClientFieldsSupport.rotate`：`RandomUtil.randomString(48)` → BCrypt → 写 `ssoSecretRotatedAt=now`。
4. `updateById`。
5. `queryById`（不走 `queryByClientId` 缓存）+ `vo.setSsoSecretOnce(issued)`。

方法上有 `@DSTransactional`，**没有** `@CacheEvict`。门卫热路径 `queryByClientId` 的 30 天缓存不会因为轮换而按键失效。`fillView` 会把缓存对象上的哈希清掉，所以缓存里本就没有可校验的哈希；SSO 换票怎么读哈希是 L-055 / L-059 的格子。本课要能说：**轮换成功返回的明文只这一次；列表再查只有 `ssoSecretConfigured`。**

SsoApp 的 add 还有一层：Controller 在进 Service 之前若 `ssoSecret` 空白就先生成明文。于是 **public** 应用经右边窗创建时，也会带哈希——尽管 `SsoClientFieldsSupport.apply` 单独面对 public 且无明文时**可以**不哈希。左边窗 add 不预填，public + 无明文 → 库里可以没有哈希。口试不要把 support 类的 public 默认，说成 SSO 管理窗的行为。

#### 5. 启停与删除的柜子

`changeStatus(clientId, status)`：按**门牌字符串**更新 `status`。`rows>0` 且新状态是 `SystemConstants.DISABLE`（`"1"`）才 `kickoutClient(主键)`。状态真的变了才作废 OpenAPI。`@CacheEvict` 键是门牌。

`remove`：逻辑删除（`@TableLogic delFlag`）。作废 OpenAPI。**不**调用 `kickoutClient`。Sa-Token 票可以留到自己过期。`isValid` 参数是死的：没有「内置 pc 不能删」的服务端闸。

`export` 是 POST + `@Log`，符合变更用 POST 的追踪；`edit` / `changeStatus` 仍是存量 classic 的 PUT，`remove` 仍是 DELETE。本课按磁盘口述动词，不把它们改口成 layered 新模块。

### 图、表或文本图

**图 1. 两扇窗、一只抽屉、两把钥匙**

```text
        ┌──────────── 管理员 ────────────┐
        │                                │
        v                                v
 /system/client                      /system/ssoApp
 九扇（含 bind、kebab 轮换）          五扇（含 camel 轮换）
        │                                │
        │    同一 ISysClientService      │
        └────────────┬───────────────────┘
                     v
              sys_client 一行
     ┌───────────────┼────────────────┐
     v               v                v
 clientId        client_secret    sso_secret_hash
 (MD5 门牌)      (登录明文)       (BCrypt；VO 不回显)
                     │
                     v
              ssoSecretOnce
           (仅本次 HTTP 响应)
```

- **alt：** 左右两个管理入口汇入同一张 `sys_client`，门牌哈希、登录明文密钥和 SSO 哈希分三格，一次性明文只出现在响应上。
- **caption：** 图 1——OBJ-18 的空间关系。bind 只画在左窗；rotate 两窗都有箭头指向同一格 `sso_secret_hash`。
- **文字等价物：** 管理员改终端走 `/system/client`，登记 SSO 应用走 `/system/ssoApp`。两边的 Java Controller 都只打电话给 `ISysClientService`。持久化只有 `sys_client`。登录用的 `clientSecret` 与 SSO 用的哈希不是同一列。明文 SSO 密钥没有自己的列，只在签发那一次出现在 JSON 的 `ssoSecretOnce`。
- **图的边界：** 图上没有 `wta-sso` 的 `/sso/oauth2/*`，没有 Redis 会话柜内部结构，没有前端 `createSystemService` 工厂。左窗的 PUT/DELETE 是真实动词，不要画成「全部 POST」。

**图 2. bind 与 rotate 的因果（成功才往右）**

```text
[登记] 回调精确写入 ssoRedirectUris
   │  空 / 通配 / 非 http(s) → 停
   v
[可选 bind] ssoEnabled=true 且 mode∈{sso,both}
   │  无行 / 无回调 / mode=local → 停；不签发密钥
   v
[展示] ssoAccessState = bound
   │
[rotate] 新 48 位明文 → BCrypt → ssoSecretOnce
   │  无主键 / 无行 → 停；不要求已 bind
   v
 旧明文作废（库中只留新哈希）
```

- **alt：** 先精确回调，再可选盖接入章，轮换可独立发生并只回显一次明文。
- **caption：** 图 2——登记、接入、轮换不是同一个按钮。
- **文字等价物：** 没有精确回调就不能 bind。bind 成功只改两个 SSO 标志位。轮换不看这两个标志位，只看主键是否存在。轮换后旧信封作废。
- **图的边界：** 种子 SQL 可以直接把 pc/app 写成 `sso_enabled=1` 加回调，绕过 HTTP bind。那是基座 DML，不是 Controller 的第三条路径。

### 正例、反例与边界

**正例 A：SSO 管理创建。** `POST /system/ssoApp`，body 带 `clientKey`、`clientSecret`、`grantTypeList`、`userTypeId`、精确回调。`prepare` 把 SSO 打开。空白 `ssoSecret` 时 Controller 生成 48 位。unique 过了才 insert。响应 VO 里有新 `clientId`（MD5）和这一次的 `ssoSecretOnce`。操作日志不存响应体。

**正例 B：Client 列表 bind。** 某行已有回调、仍是「没有接入」。`POST /system/client/sso/bind` `{"id":9,"ssoAuthMode":"both"}`。库中该主键 `sso_enabled=1`、`sso_auth_mode=both`。全量驱逐 `sys_client` 缓存。返回的 VO 没有新明文。

**正例 C：任一门牌轮换。** 给已有主键 `POST .../rotate-secret` 或 `.../rotateSecret`。新哈希落地，`ssoSecretOnce` 出现一次。再 GET 详情，只有 `ssoSecretConfigured=true`。

**正例 D：停用踢票。** `PUT /system/client/changeStatus` `clientId=<门牌>` `status=1`。更新行后 `kickoutClient(主键)`。登录域用户的 OpenAPI 机器会话一并作废。

**反例 1：** 「`/system/ssoApp` 是 `wta-sso` 模块。」磁盘在 `wta-system`。`wta-sso` 是 layered 认人厅，前缀 `/sso`。

**反例 2：** 「两扇窗两张表。」一张表。SsoApp 的 list 甚至可能列出还没登记回调的终端。

**反例 3：** 「bind 会发 SSO 密钥、写回调、踢全部门票。」三件都不做。

**反例 4：** 「rotate 的两条 URL 是两个 Service。」一个方法。差别只在路径形状和权限串（`system:client:edit` vs `system:ssoApp:edit`）。

**反例 5：** 「`clientSecret` 就是 SSO client_secret。」前者明文存库、参与 MD5 门牌；后者只哈希。混用会把登录密钥当成 OAuth 信封。

**反例 6：** 「getInfo 用门牌当路径。」路径变量是 `Long id`。门牌出现在 `changeStatus` 和缓存键。

**反例 7：** 「新增时一起选默认角色。」`validClientPolicy(..., true)` 只要 `defaultRoleId` 非空就抛。角色还要求 `role.clientId` 等于**当前客户端主键**，新增时主键刚生成，对不上。

**反例 8：** 「SSO 管理 list 只返回 ssoEnabled 的行。」`buildQueryWrapper` 没有 SSO 字段。页面 `queryParams` 默认只有分页。

**反例 9：** 「菜单有 `system:ssoApp:remove` 所以有删除接口。」Controller 无 `@DeleteMapping`。

**反例 10：** 「改 `clientSecret` 会换门牌。」门牌只在 insert 算一次。

**反例 11：** 「删除 Client 等于立刻登出所有人。」删除不作 `kickoutClient`。停用才会。

**反例 12：** 「SsoApp 的 edit 是 PUT，和 Client 一样。」它是 `POST /system/ssoApp/update`。

**反例 13：** 「`checkClientKeyUnique`。」源码方法名是 `checkClickKeyUnique`。

**反例 14：** 「public 应用一定没有哈希。」经 SsoApp add 预填明文后会有。经 Client add 且不传 `ssoSecret` 的 public 可以没有。

**边界：**

- 回调校验：启用 SSO 时列表不能空；任何写入的单条 URI 禁止 `*`、必须 http/https、要有 host、禁止 fragment。单测 `insertByBoRejectsWildcardRedirect` / `enabledClientRejectsWildcardRedirect`。
- `authMode` 集合：`local|sso|both`。bind 成功态子集只有 `sso|both`。
- `kind` 集合：`public|confidential`。confidential 在 apply 结束时必须已有哈希。
- 缓存名带 `#30d`。`queryByClientId` 写入；`updateByBo` / `updateClientStatus` 按门牌驱逐；`delete` / `bindSsoAccess` 全表驱逐；**rotate 不驱逐**。
- `@Log excludeParamNames`：`ssoSecret` / `ssoSecretOnce` / `clientSecret`。rotate 再关响应落日志。
- `ISysClientService` 不是 `wta-api`。邻居读目录走 `SystemSsoClientCatalog`（本课点到「只读适配器在 system 包」即可）。
- 空 Mapper XML 不是「没有持久化」，是「没有手写 SQL」。CRUD 在 `BaseMapperPlus`。

## 变式与迁移

- **变式 A：给已有终端补 SSO。** 不要再 `ssoApps.add` 一行（会算出新门牌）。用 SSO 窗 `POST /update` 写精确回调（`prepare` 会打开 SSO），或用 Client 窗先写下回调再 `bind`。新行会让 Auth 看见一个新的 `clientId`。
- **变式 B：只要 SSO 登录、关掉密码窗。** 这是 `ssoAuthMode` / `grantType` 与 L-011 门卫 `contains` 的组合。本课只负责把字段写进 `sys_client`。不要删 `POST /auth/login`。
- **变式 C：confidential 合作方。** 经 SsoApp add 或 Client add 带 `ssoClientKind=confidential`。没有明文时 support 会生成。把 `ssoSecretOnce` 交给对方一次。以后只 rotate，不提供「再查明文」。
- **变式 D：泄露后轮换。** 走两条 rotate URL 之一。旧信封立即不可用（库中哈希已换）。不要去改 `clientSecret` 当 SSO 轮换——门牌不会变，SSO 哈希也不会换。
- **变式 E：停用而不是删除。** `changeStatus` 会踢该主键下的登录票。删除只藏行（`del_flag`），票可能还在。运维口诀：先停再用。
- **变式 F：home 终端的路径白名单。** 写入时 `normalizeAccessPath`。给 SSO 目录看时 `ClientAccessPaths.resolve` 只对 `clientKey==home` 并入身份/档案 GET。本课不把 extras 说成会话本身（L-013 已划界）。
- **变式 G：缓存看起来「改了没生效」。** 先看改的是 `queryByClientId` 热路径还是 `queryById` 管理路径。管理 GET 不走这层缓存。轮换尤其不会按门牌驱逐。
- **迁移口诀：** 先问哪扇窗（client vs ssoApp）→ 再问主键还是门牌 → 再问动的是明文登录密钥还是 SSO 哈希 → bind 只盖章 → rotate 两 URL 一方法 → 停用才踢票。跳步就会把认人厅、终端管理、信封交付说成同一按钮。

## 常见误区

1. **「SSO 应用是 sso 模块的表。」** 管理 HTTP 在 system；数据在 `sys_client`。sso 模块经 api 只读。
2. **「bind = 创建应用。」** 创建在 SsoApp `add`。bind 要求回调已在。
3. **「两扇 rotate 不一样。」** HTTP 形状和权限不一样；Service 方法同一个。
4. **「列表接口各自过滤自己的资源。」** 过滤条件同一套，且不含 SSO 开关。
5. **「密钥轮换会登出用户。」** rotate 不 `kickoutClient`。停用/换登录域才踢。
6. **「导出是安全的，日志都 exclude 了。」** 日志排除请求里的密钥名；Excel 仍有 `clientSecret` 列；SSO 明文靠 `isSaveResponseData=false` 和 VO 清哈希。
7. **「classic 所以可以再加一个 UseCase 只给 SsoApp。」** 登记表禁止同一模块混模式。
8. **「`changeStatus` 的 body.id 就是主键。」** 它读 `bo.clientId` 字符串。
9. **「SsoApp 也能 DELETE。」** 菜单种子有 remove 字，Controller 没有。
10. **「public 永不存哈希。」** SsoApp add 预填明文后会存。
11. **「把 `checkClickKeyUnique` 念成拼写错误就当它不存在。」** 它就是唯一性闸，失败文案仍说「客户端key已存在」。
12. **「前端 `ssoAccessState` 是第三张表。」** 纯函数，读 VO 三个字段。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 并排打开两份 Controller。把每一对 `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` 抄成一张表：路径、动词、Java 名、权限串。圈出只有一边有的方法（bind、export、changeStatus、remove）和两边都有但路径形状不同的 rotate。
2. 在 `SysSsoAppController.add` 用手指划：`prepare` → 可能 `generatePlaintext` → `checkClickKeyUnique` → `insertByBo` → `queryById` → `setSsoSecretOnce`。再划 `SysClientController.add`：没有 `prepare`，没有预填 `ssoSecret`。
3. 打开 `SsoAccessSupport` 和 `bindSsoAccess`。对照「未登记」抛错条件与 `ssoAccessState` 变绿条件，看它们是不是同一组字段。
4. 打开 `SysClientServiceImpl.rotateSsoSecret`，搜 `@CacheEvict`。再搜 `queryByClientId` 的 `@Cacheable`。把「管理详情」和「门卫热路径」哪条会看见旧缓存说出来。
5. 打开空的 `SysClientMapper.xml`、实体 `@TableName("sys_client")`、登记表 classic 行。用一句话连接：为什么本课不许发明 `SsoAppUseCase`。

## 总结、词汇表与下一步

- **两扇窗、一只抽屉。** `SysClientController` 九扇，前缀 `/system/client`；`SysSsoAppController` 五扇，前缀 `/system/ssoApp`。都委托 `ISysClientService`，都落 `sys_client`。classic：Controller → ServiceImpl → Mapper（空 XML）。
- **Client 九扇：** list GET；export POST 出 Excel（含登录密钥列）；getInfo GET 主键；add POST 算 MD5 门牌；edit PUT；`rotateSsoSecret` POST kebab；`bindSso` POST 盖接入章；changeStatus PUT 用门牌；remove DELETE 逻辑删且不踢 Sa-Token。
- **SsoApp 五扇：** list/get 同库；add 先 `prepare` 并可能预填 SSO 明文；edit 是 **POST `/update`**；`rotateSecret` POST camel。无 bind/export/停用/删除 HTTP。
- **bind：** 必须已有精确回调；模式 `sso`/`both`；只改启用位；全量驱逐 Client 缓存。
- **rotate：** 两 URL 一方法；48 位明文只回一次；不要求已接入；方法上无 `@CacheEvict`。
- **两把密钥：** `clientSecret` 明文 + 门牌 MD5；SSO 只存 BCrypt。VO 清哈希，用 `ssoSecretConfigured` / `ssoSecretOnce`。
- 格子名与 Java 名本课对齐：`bindSso`、`rotateSsoSecret`、`rotateSecret`。不要把它们说成 authorize / token。

词汇表：SysClient / SysSsoApp / classic / `sys_client` / clientId / clientKey / clientSecret / ssoSecretHash / ssoSecretOnce / bind / rotate / exact redirect URI / authMode / public / confidential / `@Cacheable` / `@CacheEvict` / `@RepeatSubmit` / `@Log` / `checkClickKeyUnique` / `SsoAppRegistration` / `SsoAccessSupport`。

下一步：L-019 指着 `createSystemService.clients / ssoApps` 把本课 URL 接到前端厨房。L-055 起才走进 `/sso/oauth2/authorize` 的 PKCE 链。L-059 才把 `SsoClientCatalog` 当跨模块只读合同讲完。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 房间内公开入口；本课两份 Controller 同模块 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记为 classic；禁止与 layered 混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | `sys_client` 列（含 SSO 哈希/回调）；种子 pc/app 后被写成 both+精确回调；`sso` 厅堂 Client 仍 local 无回调；菜单 `system:ssoApp:remove` 种子 | `create table sys_client`；`NAMEWTA-SSO-DSL-001`；ssoApp 菜单 | 2026-09-16 |
| S-L018-01 | `SysClientController.java` | 九扇映射、权限、`@Log`/`@RepeatSubmit`、bind/rotate 只收主键、PUT edit 与 DELETE remove、导出 Excel | 类与各方法 | 2026-09-16 |
| S-L018-02 | `SysSsoAppController.java` | 五扇映射；`prepare`+预填明文；`POST /update`；camel `rotateSecret`；无 delete | 类与各方法 | 2026-09-16 |
| S-L018-03 | `ISysClientService.java`、`SysClientServiceImpl.java`、`SysClientMapper.java`、`SysClientMapper.xml` | classic 持有 Mapper；insert MD5 门牌；policy/踢票/OpenAPI 作废；bind 全量驱逐；rotate 无 CacheEvict；空 XML | 接口与实现、空 mapper | 2026-09-16 |
| S-L018-04 | `SsoAppRegistration.java`、`SsoClientFieldsSupport.java`、`SsoSecretHasher.java`、`SsoAccessSupport.java`、`SsoRedirectUris.java`、`ClientAccessPaths.java` | 强制启用 SSO；哈希与一次性明文；bind 闸；精确回调；home 路径并入 | 各 `org.namewta.system.sso` 类型 | 2026-09-16 |
| S-L018-05 | `SysClient.java`、`SysClientBo.java`、`SysClientVo.java`、`CacheNames.SYS_CLIENT` | 表字段；Add/Edit 校验组；VO `@JsonIgnore` 哈希 + `ssoSecretOnce`；缓存名 `#30d` | 领域类型与常量 | 2026-09-16 |
| S-L018-06 | `SysClientServiceSsoUnitTest.java`、`SsoClientFieldsSupportTest.java` | 通配回调拒写；未登记不能 bind；public 可不哈希；confidential 生成哈希；fillView 清哈希 | `@Test` 方法名 | 2026-09-16 |
| S-L018-07 | `frontend/packages/domains/system/src/service.ts`、`client/index.ts`、`client/sso-access.ts`、`web-domains/system/src/sso-app/SsoAppPage.vue` | URL 与本课映射一致（格子仍归 L-019）；`ssoAccessState`；SSO 页不承担 Client 创建主路径 | `clients`/`ssoApps` 工厂与页面文案 | 2026-09-16 |
