---
lesson_id: L-001
objective_ids: [OBJ-01]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 16
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-3RD-01, S-3RD-05, S-3RD-06, S-3RD-09, S-3RD-10]
---

# Lesson 001：供应商 Provider，一本只能改封面不能改门牌的通讯录

## 学完你能做什么

你能顺着一条管理请求，从 `ThirdProviderController` 走到 `ThirdProviderUseCase` / `ThirdProviderUseCaseImpl`、`ThirdProviderService`、`ThirdProviderDao`、`ThirdProviderMapper`，并指出 `list` / `get` / `add` / `save` / `status` / `remove` 各自的 HTTP 路径、权限、日志、事务和路径安全校验。你还能说出：为什么删供应商前必须先停用、必须先清 Endpoint，以及为什么改完配置要 `ThirdConfigSnapshotPort.evict`。

## 先把宏观地图放在桌上

把第三方公司想成一栋楼。Provider 只登记：**楼在哪（`baseUrl`）、门牌号（`providerCode`）、这栋楼允许多快（超时 / 限流 / 并发）、大家共用的门卫贴纸（`sharedHeadersJson`）**。它不负责“敲哪一扇门”——那是 Endpoint（L-002）；也不负责“胸牌”——那是 Credential（L-003）。

管理端改这本通讯录，必须走五层，不能抄 classic 的 `ServiceImpl` 直接抱 Mapper：

```text
HTTP /third/provider
  ThirdProviderController          字段 useCase : ThirdProviderUseCase
       -> ThirdProviderUseCase     接口：list/get/save/changeStatus/remove
            -> ThirdProviderUseCaseImpl   字段 service : ThirdProviderService
                 -> ThirdProviderService  字段 providerDao, configCache
                      -> ThirdProviderDao 字段 providerMapper, endpointMapper
                           -> ThirdProviderMapper / ThirdEndpointMapper
                                -> BaseMapper SQL；XML 另有 selectByProviderCode
```

**类比：** Provider 像学校通讯录里“某某公司总部”这一行。小孩子改地址，只改这一行。

**类比失效处：** 通讯录改完，电话不会自动打出去。出站调用走 `ThirdPartyGateway` / `ThirdGatewayAdapter`（L-005），用的是 `providerCode` + `endpointCode` 拼出来的快照，不是管理端这个 Controller。另外，Skill 模块地图把 wta-third 写成 layered 出站模块，但 `03-backend-module-modes.md` **没有逐行点名** `wta-modules/wta-third`。本课以工作树为准：它是 Controller → UseCase → Service → DAO → Mapper，不是 classic。未登记业务模块默认 layered（S-3RD-10）。

## 核心概念与机制

### 直觉讲解

管理员打开页面，要看供应商列表、点开详情、新建、保存、停用/启用、删除。这六件事看起来像六种按钮，Java 里却只有 **五条 UseCase 方法**：`add` 和 `save` 都进 `useCase.save`。区别在门口：权限字和 `@Log` 的 `BusinessType` 不同。

管家（UseCase）几乎只转发。真正检查“门牌能不能改、URL 安不安全、有没有重名、删之前楼里还有没有门”的，是厨师 `ThirdProviderService`。仓库管理员 `ThirdProviderDao` 才碰 Mapper。厨师手里还有一把“作废缓存”的喇叭：`ThirdConfigSnapshotPort configCache`，改库成功后喊 `evict(providerCode, null)`，免得网关还拿着旧地址出门。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，对应当前 Java） |
| --- | --- | --- |
| 供应商 | Provider | 表 `third_provider` 上的 `ThirdProvider`：编码、名称、HTTPS 源、超时、限额、共享头 |
| 管理控制器 | `ThirdProviderController` | `@RequestMapping("/third/provider")`；唯一协作方 `private final ThirdProviderUseCase useCase` |
| 用例接口 | `ThirdProviderUseCase` | `list` / `get` / `save` / `changeStatus` / `remove` |
| 用例实现 | `ThirdProviderUseCaseImpl` | 唯一协作方 `private final ThirdProviderService service`；写操作带 `@DSTransactional` |
| 领域服务 | `ThirdProviderService` | 规则与校验；协作方 `ThirdProviderDao providerDao`、`ThirdConfigSnapshotPort configCache` |
| 数据访问 | `ThirdProviderDao` | 实现 `ThirdProviderConfigStore`；协作方 `ThirdProviderMapper providerMapper`、`ThirdEndpointMapper endpointMapper` |
| 映射器 | `ThirdProviderMapper` | `extends BaseMapper<ThirdProvider>`，另有 `selectByProviderCode` 返回 `ThirdProviderRow` |
| 写入对象 | `ThirdProviderBo` | 管理端 JSON 入参 |
| 读出对象 | `ThirdProviderVo` | 管理端 JSON 出参；不含删除标记明文密钥（Provider 本就没有 secret） |
| 启用状态 | status `"0"` / `"1"` | Service 常量 `ENABLED = "0"`；`"1"` 为停用 |
| 软删除 | `delFlag` | `"0"` 在用，`"1"` 删除；`remove` 不物理删行 |
| 路径安全 | `ThirdEndpointSecurity` | 本切片用 `validateIdentifier`、`validateBaseUrl`、`validateSharedHeadersJson` |

### 机制/因果链

把六条 HTTP 映射逐条钉死。Controller 只做：权限、校验、记日志、包 `R.ok`。

1. **list**  
   `GET /third/provider/list`，`@SaCheckPermission("third:provider:list")`，可选 `keyword`。  
   `useCase.list` → `service.list` → `providerDao.findActive(keyword)` → `providerMapper.selectList`。  
   DAO 固定 `delFlag = "0"`，按 `providerId` 升序。keyword 非空则 `trim` 后对 `providerCode` **或** `providerName` 做 like。Service 用私有 `toVo` 映成 `ThirdProviderVo`。无事务。

2. **get**  
   `GET /third/provider/{providerId}`，权限 `third:provider:query`。  
   `useCase.get` → `service.get` → `required(providerId)` → `findActiveById` → `selectOne`（id + `delFlag=0`）。找不到抛 `ServiceException("Provider not found")`，再 `toVo`。

3. **add**  
   `POST /third/provider`，权限 `third:provider:add`，`@Log(title = "第三方供应商", businessType = BusinessType.INSERT)`，body 为 `@Valid ThirdProviderBo`。  
   方法名是 `add`，调用的是 `useCase.save(bo)`。  
   UseCaseImpl：`@DSTransactional public void save` → `service.save`。

4. **save**  
   `POST /third/provider/save`，权限 `third:provider:edit`，`@Log` 的 `BusinessType.UPDATE`。  
   同样 `useCase.save(bo)`。和 add 共用一条业务链。

5. **status**  
   `POST /third/provider/{providerId}/status?status=`，权限 `third:provider:edit`，`@Log` 标题「第三方供应商状态」。  
   `useCase.changeStatus`（事务）→ `service.changeStatus`：先 `required`，再 `entity.setStatus("1".equals(status) ? "1" : ENABLED)`，`providerDao.update`，然后 `configCache.evict(code, null)`。  
   注意：查询参数只要不是字符串 `"1"`，就会被写成启用 `"0"`。没有第三种状态。

6. **remove**  
   `POST /third/provider/{providerId}/remove`，权限 `third:provider:remove`，`@Log` `BusinessType.DELETE`。  
   `useCase.remove`（事务）→ `service.remove`：必须已停用（`status` 已是 `"1"`），否则 `"Disable provider before deleting"`；再 `providerDao.countActiveEndpoints(providerId)`，用 **`endpointMapper.selectCount`** 数 `ThirdEndpoint` 且 `delFlag=0`，大于 0 则 `"Remove endpoints before deleting provider"`；通过后 `delFlag="1"` 再 `update`，最后 `evict`。

`service.save` 的内部因果（add/save 共用）：

1. `providerId == null` → 新建；否则 `required` 取出当前行。  
2. `ThirdEndpointSecurity.validateIdentifier(bo.getProviderCode(), "Provider code")`。已存在行若编码变了 → `"Provider code cannot be changed"`。  
3. `providerDao.existsCode(code, bo.getProviderId())` 排除自身后仍撞码 → `"Provider code already exists"`。  
4. 新建：`IdGeneratorUtil.nextLongId()`，`version=0`，`delFlag="0"`。  
5. 名称 `trim`；`baseUrl` 走 `validateBaseUrl`（只允许 http(s) origin，无 userInfo / query / fragment，路径只能空或 `/`，去掉尾斜杠）。  
6. `normalizeStatus`：只能 `"0"` 或 `"1"`。  
7. 连接超时默认 3000、读超时默认 10000，显式值必须 `>= 100`；限流/并发 `null` 当 0，负数非法。  
8. `validateSharedHeadersJson`：必须是对象，键走 `validateConfiguredHeaderName`，值必须是标量。  
9. `insert` 或 `update` 的影响行数必须是 1，否则 `"Provider save failed"`。  
10. `configCache.evict(entity.getProviderCode(), null)`：`endpointCode` 传 `null` 表示整家供应商的快照都作废（实现细节见 L-005 的 `ThirdConfigCacheAdapter`）。

DAO 为什么同时持有 `ThirdEndpointMapper`？因为删供应商前要数门。这不是 Service 去 import Mapper，仍是 DAO 独占 MyBatis。

### 图、表或文本图

**图题 / caption：** Provider 管理切片六条映射与协作方（工作树 2026-09-14）。

```text
浏览器/管理端
    |  GET  /third/provider/list            third:provider:list
    |  GET  /third/provider/{id}            third:provider:query
    |  POST /third/provider                 third:provider:add     INSERT  -> save
    |  POST /third/provider/save            third:provider:edit    UPDATE  -> save
    |  POST /third/provider/{id}/status     third:provider:edit    UPDATE  -> changeStatus
    |  POST /third/provider/{id}/remove     third:provider:remove  DELETE  -> remove
    v
ThirdProviderController.useCase
    v
ThirdProviderUseCaseImpl.service     [save/changeStatus/remove 有 @DSTransactional]
    v
ThirdProviderService
    |-- providerDao.findActive / findActiveById / existsCode / insert / update / countActiveEndpoints
    |-- ThirdEndpointSecurity.validateIdentifier|validateBaseUrl|validateSharedHeadersJson
    \-- configCache.evict(providerCode, null)
            |
            v
ThirdProviderDao
    |-- providerMapper.selectList / selectOne / selectCount / insert / updateById
    \-- endpointMapper.selectCount   (只为 countActiveEndpoints)
```

**文字等价物：** 六条管理 URL 全部进同一个控制器。列表和详情是 GET，不写 `@Log`。四条 POST 都有 `@Log`。`add` 与 `save` 在 UseCase 层合并为 `save`。UseCaseImpl 把读操作原样转给 Service，把写操作放进数据源事务。Service 用 `ThirdEndpointSecurity` 卡住编码和源站 URL，用 DAO 读写 `ThirdProvider`，删之前用 Endpoint 的 Mapper 计数。成功写入后通过 `ThirdConfigSnapshotPort` 作废该 `providerCode` 下的配置缓存。DAO 是本切片唯一持有两个 Mapper 的类。

**图的边界：** 本图不画出 `ThirdGatewayAdapter.execute` 如何用快照发 HTTP，不画出凭证加密，不解释 Redis 失效广播的实现。`ThirdProviderMapper.selectByProviderCode` 和 `ThirdProviderRow` 存在于 XML，但 **本课六条管理映射不走这条 XML**，走的是 `LambdaQueryWrapper` + `BaseMapper`。出站读配置走 `ThirdProviderConfigStore.findActiveByCode`（仍是 DAO 的 `selectOne`），不是管理 Controller。

### 正例、反例与边界

**正例 1：** `GET /third/provider/list?keyword=pay`。DAO 生成 `del_flag=0 AND (code LIKE 或 name LIKE)`，Service 映射为 `ThirdProviderVo` 列表，Controller 包进 `R.ok`。

**正例 2：** 新建时 `providerId` 为空。Service 发新 ID，插入一行，`evict("AcmePay", null)`。编码一旦写入，以后 `POST /save` 带同一 id 但改 code，会被拒绝。

**正例 3：** 删除前先 `POST .../status?status=1` 把状态写成 `"1"`，再保证没有 `delFlag=0` 的 Endpoint，才能 `remove` 把 `delFlag` 写成 `"1"`。列表的 `findActive` 立刻看不见它。

**反例 1：** 在 `ThirdProviderController` 里直接注入 `ThirdProviderMapper` 或 `ThirdProviderService`。工作树只允许 `ThirdProviderUseCase`。这会把 layered 打回 classic 灵魂。

**反例 2：** 启用中的供应商直接 `remove`。Service 先看 status，不是 `"1"` 就抛 `"Disable provider before deleting"`。这是故意的双确认，不是漏写物理删除。

**反例 3：** `baseUrl` 写成带路径的 `https://api.example.com/v1` 或带 `user:pass@`。`validateBaseUrl` 只要 host 之外还有 query/fragment/userInfo，或 path 不是空/`/`，就失败。源站只能是 origin。

**边界：**

- `add` 的 body 如果自带已存在的 `providerId`，`save` 会走更新分支，只要调用方有 `third:provider:add`。权限字和真正 insert/update 不是同一把锁。  
- `changeStatus` 把一切非 `"1"` 的字符串都写成启用，包括空串、`"0"`、`"true"`。  
- 超时入参在 Bo 上已有 `@Min(100)`，Service 的 `positive` 再挡一层，`null` 才回落到 3000/10000。  
- `rateLimit` / `concurrencyLimit` 为 0 表示“本切片不设限额”，不是“每秒 0 次”。网关侧 0 会被 `ThirdResiliencePolicyAdapter` 当成未启用（L-005）。  
- 本切片不回显、不保存凭证。`ThirdProvider` 没有 ciphertext 字段。

## 变式与迁移

- **变式 A（只改备注）：** 仍走 `POST /save` → 同一条 `save` 链。编码不变，所以过得了“code cannot be changed”。仍会 `evict`，因为共享头或超时也可能一起提交。  
- **变式 B（停用但不删）：** 只打 `status`。行还在 `findActive` 里（`delFlag` 仍是 `"0"`），但网关看到 `status != "0"` 会返回 `PROVIDER_DISABLED`（L-005）。管理列表仍能看见它，才能再启用或删除。  
- **变式 C（有 Endpoint 的供应商）：** `countActiveEndpoints > 0` 时删除失败。正确迁移是先按 L-002 停用并删除 Endpoint，再回到本课的 `remove`。  
- **与登记表冲突时怎么迁：** Skill 若把 third 说成 classic，或登记表漏行，**按 Java 五层写**，并记 S-3RD-10。不要为了迎合摘要去新建 `service/impl`。

## 常见误区

1. **“add 和 save 是两套 Service 方法。”** Controller 有两个方法，UseCase 只有 `save`。  
2. **“UseCaseImpl 自己写校验。”** 它几乎是转发器；路径安全和业务规则在 `ThirdProviderService`。  
3. **“删除就是 DELETE HTTP。”** 本模块变更用 POST；删除路径是 `/{providerId}/remove`，且是软删除。  
4. **“DAO 不该碰 Endpoint 表。”** 工作树就是 `ThirdProviderDao` 持有 `ThirdEndpointMapper` 做计数。跨表计数仍在 DAO，不在 Service。  
5. **“改完库网关会自己刷新。”** 必须 `configCache.evict`。漏掉会让出站继续用 Redis 里最多 10 分钟的旧快照（L-005）。

## 非评分暂停

打开 `ThirdProviderController`，用手指点六个方法，说出它们最终进 UseCase 的哪一个名字。再打开 `ThirdProviderUseCaseImpl`，确认唯一字段就是 `service`。最后看 `ThirdProviderService.remove`：停用检查和 Endpoint 计数哪一句先执行。

不要在本课给自己打分。能把六条映射和两层 `private final` 协作方说全，就够进 L-002。

## 总结、词汇表与下一步

- 协作方（必须能口述）：Controller → `useCase`；UseCaseImpl → `service`；Service → `providerDao` + `configCache`；DAO → `providerMapper` + `endpointMapper`。  
- 六映射：list/get 只读；add/save 共用 `save`；status 只改 `"0"`/`"1"`；remove 先停用再清门再软删。  
- 路径安全：`providerCode` 标识符、`baseUrl` 纯 origin、共享头 JSON。  
- 写成功必 `evict(providerCode, null)`。

下一步：L-002 给这栋楼装门（Endpoint），并看网关以后如何用 `providerCode` + `endpointCode` **点名**选中那扇门。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-3RD-01 | `ThirdProviderController.java` | 六条 HTTP 映射、权限、`@Log`、协作方 `useCase` | `controller/admin/ThirdProviderController.java` | 2026-09-14 |
| S-3RD-05 | `ThirdProviderUseCase.java` / `ThirdProviderUseCaseImpl.java` | 五方法合同、`@DSTransactional`、协作方 `service` | `usecase/` 与 `usecase/impl/` | 2026-09-14 |
| S-3RD-06 | `ThirdProviderService.java`、`ThirdProviderDao.java`、`ThirdProviderMapper.java` | 校验、软删、计数、`toVo`、Mapper 方法 | `service/` `dao/` `mapper/` | 2026-09-14 |
| S-3RD-09 | `ThirdEndpointSecurity.java` | `validateIdentifier` / `validateBaseUrl` / `validateSharedHeadersJson` | `support/ThirdEndpointSecurity.java` | 2026-09-14 |
| S-3RD-10 | 登记表与模块地图 | 未逐行点名时以源码五层为准；禁止按 classic 教 | `03-backend-module-modes.md`；`wta-module-guide` third/index.md | 2026-09-14 |
