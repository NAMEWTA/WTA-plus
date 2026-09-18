---
lesson_id: L-002
objective_ids: [OBJ-02]
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
source_ids: [S-3RD-02, S-3RD-05, S-3RD-06, S-3RD-07, S-3RD-08, S-3RD-09, S-3RD-10]
---

# Lesson 002：Endpoint 是一扇必须报出门牌的门

## 学完你能做什么

你能把 Endpoint 管理切片的六条映射从 `ThirdEndpointController` 说到 `ThirdEndpointDao` / `ThirdEndpointMapper`，列出 UseCaseImpl 和 Service 的全部 `private final` 协作方，并说明 **网关以后不会“挑一扇合适的门”**：`ThirdPartyRequest` 必须同时给出 `providerCode` 与 `endpointCode`，`ThirdConfigCacheAdapter.get` 按这对编码取出 `ThirdConfigSnapshot`。

## 先把宏观地图放在桌上

L-001 的供应商是大楼。Endpoint 是楼上**贴了名字的那扇门**：HTTP 方法、相对路径、请求/响应模式、哪些 query/header/body 名字合法、要不要重试、敏感字段、可选的 SPI 适配器代号。

```text
HTTP /third/endpoint
  ThirdEndpointController         字段 useCase : ThirdEndpointUseCase
       -> ThirdEndpointUseCase    list/get/save/changeStatus/remove
            -> ThirdEndpointUseCaseImpl    字段 service : ThirdEndpointService
                 -> ThirdEndpointService
                      字段 endpointDao, providerDao, configCache, adapterRegistry
                      -> ThirdEndpointDao  字段 endpointMapper : ThirdEndpointMapper
                           -> BaseMapper SQL

以后出站（本课只点名，细节 L-005）：
  ThirdPartyRequest(providerCode, endpointCode, ...)
       -> ThirdGatewayAdapter.execute
            -> configCache.get(providerCode, endpointCode)  => ThirdConfigSnapshot
```

**类比：** 你去办公楼取件，前台不会替你猜“应该是财务室还是仓库”。你必须说出公司名和房间号。

**类比失效处：** 真实前台有时会根据业务帮你指路。`ThirdGatewayAdapter` 不会。缺 `endpointCode` 时 `ThirdPartyRequest` 的紧凑构造器直接抛 `"endpointCode is required"`。管理列表可以按 `providerId` 过滤，但运行时选择只认编码对，不认“该供应商下 status=0 的第一扇门”。

## 核心概念与机制

### 直觉讲解

管理端这扇门的按钮和 Provider 几乎一个模子：列表、详情、新增、保存、改状态、删除。Controller 同样把 add/save 都推进 `useCase.save`。

多出来的脑子在 `ThirdEndpointService`：它要先确认大楼还在（`providerDao.findActiveById`），确认你填的 `providerCode` 和 id 是一家，确认门牌在这家楼下不重复，把方法/路径/JSON 元数据全部交给 `ThirdEndpointSecurity` 安检，还要让 `ThirdProviderAdapterRegistry.find` 提前确认：如果这扇门声明了 `adapterCode`，对应的 `ThirdProviderAdapter` 必须存在、必须属于这家供应商、必须 `supportsEndpoint`。

保存成功后 `configCache.evict(providerCode, endpointCode)`——这次带上门牌，只作废这一对，而不是整栋楼（和 Provider 的 `evict(code, null)` 不同）。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 调用点 | Endpoint | 表 `third_endpoint` 上的 `ThirdEndpoint`：相对路径 + 元数据，不是完整 URL |
| 管理控制器 | `ThirdEndpointController` | `/third/endpoint`；唯一协作方 `private final ThirdEndpointUseCase useCase` |
| 用例 | `ThirdEndpointUseCase` / `ThirdEndpointUseCaseImpl` | Impl 唯一协作方 `private final ThirdEndpointService service` |
| 领域服务 | `ThirdEndpointService` | 协作方：`ThirdEndpointDao endpointDao`、`ThirdProviderDao providerDao`、`ThirdConfigSnapshotPort configCache`、`ThirdProviderAdapterRegistry adapterRegistry` |
| DAO | `ThirdEndpointDao` | 实现 `ThirdEndpointConfigStore`；唯一 Mapper 协作方 `ThirdEndpointMapper endpointMapper` |
| 入参/出参 | `ThirdEndpointBo` / `ThirdEndpointVo` | Bo 带校验注解；Vo 是 record，含 schema JSON 与 `adapterCode` |
| 请求模式 | requestMode | `JSON` / `QUERY` / `FORM`（`validateRequestMode`） |
| 响应模式 | responseMode | `JSON` / `TEXT` / `BYTES` |
| 允许名列表 | allowed names | schema JSON 必须是带 `allowed` 数组的对象，或顶层就是数组；`parseAllowedNames` |
| 幂等 | `idempotent` | 只有 `true` 才允许 `retryCount > 0`；重试次数封顶 3 |
| 运行时快照 | `ThirdConfigSnapshot` | 同时抱着 `ThirdProvider` 和 `ThirdEndpoint` 两行 |
| 点名选择 | endpoint selection | `configCache.get(providerCode, endpointCode)`，不是扫描 |

### 机制/因果链

#### 管理六映射

和 Provider 对齐，便于背：

| HTTP | 权限 | Controller 方法 | UseCase | 事务 |
| --- | --- | --- | --- | --- |
| `GET /third/endpoint/list` | `third:endpoint:list` | `list(providerId?, keyword?)` | `list` | 无 |
| `GET /third/endpoint/{endpointId}` | `third:endpoint:query` | `get` | `get` | 无 |
| `POST /third/endpoint` | `third:endpoint:add` INSERT | `add` | `save` | `@DSTransactional` |
| `POST /third/endpoint/save` | `third:endpoint:edit` UPDATE | `save` | `save` | 同上 |
| `POST /third/endpoint/{endpointId}/status` | `third:endpoint:edit` | `status` | `changeStatus` | 同上 |
| `POST /third/endpoint/{endpointId}/remove` | `third:endpoint:remove` DELETE | `remove` | `remove` | 同上 |

`list`：`endpointDao.findActive(providerId, keyword)`。`delFlag=0`，按 `endpointId` 升序；`providerId != null` 时加等值条件；keyword 对 `endpointCode` 或 `endpointName` like。

`get`：`required` → `findActiveById`，没有则 `"Endpoint not found"`。

`changeStatus`：`"1".equals(status) ? "1" : "0"`，update，`evict(providerCode, endpointCode)`。

`remove`：必须先停用（status 已是 `"1"`），否则 `"Disable endpoint before deleting"`；然后 `delFlag="1"`，再 `evict`。没有“先清凭证”检查——凭证切片自己软删（L-003）。

#### `save` 里每一道安检（按源码顺序）

1. `providerDao.findActiveById(bo.getProviderId())`，空则 `"Provider not found"`。  
2. 新建则 `new ThirdEndpoint()`，否则 `required(endpointId)`。  
3. `validateIdentifier` 处理 `providerCode` 与 `endpointCode`。  
4. Bo 里的 providerCode 必须等于查出的 Provider 的 code，否则 `"Provider code does not match provider id"`。  
5. 已有行若 `providerId` 变了 → `"Endpoint does not belong to provider"`。门不能搬家。  
6. `endpointDao.existsCode(providerCode, endpointCode, endpointId)`：同一供应商下门牌唯一。  
7. 新建发 `IdGeneratorUtil.nextLongId()`，`version=0`，`delFlag="0"`。  
8. 方法：`validateMethod`，只许 GET/POST/PUT/PATCH/DELETE。  
9. 路径：`validateRelativePath`——必须以 `/` 开头，禁止 `..`、`\\`、`//`、绝对 URI、authority、query、fragment，模板变量必须 `{Name}` 这种标识符。  
10. 六个 JSON 字段走 `validateMetadataJson`（禁止 spel/script/expression/reflect/class，禁止文本里 `#{}` `${}` `spel:` `javascript:`）。`overrideJson` 再走 `validateOverrideJson`：只允许键 `headers`。  
11. `parseAllowedNames` 预解析 path/query/header/body schema；`parseSensitiveFields` 要求敏感字段是名字数组。  
12. status 只能 `"0"`/`"1"`。  
13. `retryCount > 0` 且非幂等 → `"Only idempotent endpoints may retry"`。限额被 `capped(..., provider.getRateLimit())` 卡在供应商上限之内（供应商限额 `null` 或 `<=0` 则不封顶）。重试次数 `min(非负, 幂等?3:0)`。  
14. `adapterCode` 空白则存 `null`，否则当标识符校验。  
15. **`adapterRegistry.find(providerCode, endpointCode, adapterCode)`**：  
    - 没有该供应商的 `ThirdProviderAdapter` Bean，且没配 adapterCode → 返回 null，允许纯 HTTP。  
    - 没 Bean 但配了 adapterCode → `"Third endpoint adapter is unavailable"`。  
    - Bean 的 `adapterCode()` 与配置不一致 → `"Third endpoint adapter is not owned by provider"`。  
    - `supportsEndpoint` 为 false → `"Third endpoint adapter does not support endpoint"`。  
16. insert/update 必须影响 1 行，然后 `evict(providerCode, endpointCode)`。

启动时 `ThirdProviderAdapterStartupValidator` 会把 `endpointStore.findAllWithAdapter()`（`adapterCode` 非空）再跑一遍 `adapterRegistry.find`。管理保存和管理启动用同一把尺子。

#### 网关以后如何选中这扇门

业务模块调用 `ThirdPartyGateway.execute(new ThirdPartyRequest(providerCode, endpointCode, path, query, headers, body))`。

1. `ThirdGatewayAdapter` 先 `configCache.get(request.providerCode(), request.endpointCode())`。  
2. `ThirdConfigCacheAdapter` 的 Redis 键是 `"third:config:" + providerCode + ":" + endpointCode`。命中则反序列化 `ThirdConfigSnapshot`。  
3. 未命中：`providerStore.findActiveByCode`（即 `ThirdProviderDao.findActiveByCode`）再 `endpointStore.findActiveByProviderAndCode(providerId, endpointCode)`。任缺或 `providerId` 对不上 → `"第三方接口配置不存在"`。  
4. 网关接着看 snapshot 里 **那一行** Endpoint 的 method、relativePath、schema、adapterCode、status。没有“负载均衡选门”“选最近更新的门”。  
5. 供应商 `status != "0"` 优先变成 `PROVIDER_DISABLED`；门 `status != "0"` 才是 `ENDPOINT_DISABLED`（与 Skill 句子一致，且对应当前 Java 的先后顺序）。

Skill `third/index.md` 把 gateway 写进 service 职责。工作树里管理 Service 不发 HTTP；发 HTTP 的是 `adapter/gateway/ThirdGatewayAdapter`。本课按 Java 教。

### 图、表或文本图

**图题 / caption：** 管理写入如何变成网关可点名的那一扇门。

```text
管理 POST /third/endpoint[/save]
        |
        v
ThirdEndpointService.save
        |  1. providerDao.findActiveById
        |  2. ThirdEndpointSecurity (method/path/json/override)
        |  3. adapterRegistry.find(provider, endpoint, adapterCode)
        |  4. endpointDao.insert|update
        |  5. configCache.evict(providerCode, endpointCode)
        v
third_endpoint 行（del_flag=0, 有 endpoint_code）
        |
        |  运行时（另一条时间线）
        v
ThirdPartyRequest.providerCode + .endpointCode
        v
ThirdConfigCacheAdapter.get  -->  ThirdConfigSnapshot(provider, endpoint)
        v
ThirdGatewayAdapter 使用 snapshot.getEndpoint() 的
        relativePath / httpMethod / schemas / adapterCode / limits
```

**文字等价物：** 管理员保存时，Service 把这扇门绑死在一个还活着的 Provider 上，把相对路径和允许的参数名写成元数据，并在声明了适配器时立刻核对 SPI 注册表。写入成功后，按“供应商编码 + 门编码”作废缓存。之后业务模块出站时必须原样报出这两个编码；缓存适配器按这对键取出同一对实体。网关只使用快照里那一个 `ThirdEndpoint`，不会在同供应商下再搜索。

**图的边界：** 本图不展开限流租约、凭证解密、RestClient 重试和观测写入（L-005 / L-004）。`ThirdEndpointRow` 类型存在，但当前 `ThirdEndpointMapper` 没有 XML 查询，管理路径全是 `BaseMapper`。`findAllByProviderCode` 给缓存整供应商失效用，不给 list API 用。

### 正例、反例与边界

**正例 1：** 列表 `GET /third/endpoint/list?providerId=9&keyword=refund`。只看见这家楼下、编码或名称模糊匹配、未删除的门。

**正例 2：** 保存 `httpMethod=POST`，`relativePath=/v1/orders/{orderId}`，`pathSchemaJson` 声明 `allowed: ["orderId"]`，`idempotent=true`，`retryCount=2`。Service 接受；网关以后 `expandPath` 必须拿到 `orderId`，缺了会 `IllegalArgumentException` → `REJECTED`。

**正例 3：** `adapterCode` 留空。`adapterRegistry.find` 在没有对应 Provider Bean 时返回 null。这扇门以后走纯 `RestClient` 管道。

**反例 1：** `relativePath=https://evil.example/x` 或 `/../secret`。`validateRelativePath` 失败。完整 URL 不属于 Endpoint。

**反例 2：** 非幂等却 `retryCount=1`。保存阶段就被拒绝，不会等到网关。

**反例 3：** 以为网关会“选一个启用的 Endpoint”。`ThirdPartyRequest` 构造时 endpointCode 为空直接炸；缓存 get 也需要两段编码。

**边界：**

- Endpoint 限额不能超过 Provider 限额（Provider 限额为正数时 `Math.min`）。  
- `overrideJson` 只能覆盖 headers，不能改 baseUrl 或 method。  
- 调用方请求头还要过 `validateHeaderName`（比配置头更严，会拦 `Authorization` 等）；那是网关运行时的事，管理保存只校验 schema 形状。  
- 停用的门仍能被 `get` 到（`findActiveById` 只看 delFlag），但网关会 `ENDPOINT_DISABLED`。  
- `ThirdEndpointDao.findAllWithAdapter` 给启动校验用，管理 list 不用。

## 变式与迁移

- **变式 A：** 只改 `endpointName` 或备注式字段，仍走完整 `save` 安检和 `adapterRegistry.find`，仍 `evict` 这一对编码。  
- **变式 B：** 把 `adapterCode` 从空改成有值。若注册表没有该供应商适配器，保存失败——不会留下“运行时才发现缺 Bean”的行（启动校验也会再挡一次存量行）。  
- **迁移路径：** 先有 L-001 的 Provider，再创建 Endpoint，再（可选）L-003 凭证，最后业务模块按编码调用网关。反过来先调网关会得到配置不存在 / `CONFIG_UNAVAILABLE`。  
- **与 Provider 删除的衔接：** L-001 的 `countActiveEndpoints` 数的就是这些 `delFlag=0` 的门。要拆楼先拆门。

## 常见误区

1. **“Endpoint 里写完整 URL 更灵活。”** `baseUrl` 在 Provider；门只有相对路径。这是路径安全合同，不是风格问题。  
2. **“Service 可以自己 new RestClient 试连通。”** `ThirdEndpointService` 没有 HTTP 工厂。试连通属于网关，不在本切片。  
3. **“list 不传 providerId 就不该返回数据。”** `providerId` 可选；null 时 DAO 不加该条件，返回所有未删除门。  
4. **“adapterCode 填类名。”** 它是标识符，和 `ThirdProviderAdapter.adapterCode()`（默认等于 `providerCode()`）对齐，禁止反射类名（Skill 与 Java 在这点一致）。  
5. **“UseCaseImpl 还注入了 Registry。”** 没有。Registry 在 Service 上。UseCaseImpl 只有 `service`。

## 非评分暂停

看 `ThirdEndpointController` 的六个方法和 `ThirdEndpointUseCaseImpl` 的 `private final ThirdEndpointService service`。再看 `ThirdEndpointService` 四个字段。用一句话回答：网关选门时用的是 `endpointId` 还是 `providerCode+endpointCode`？

不要打分。能拒绝“自动挑门”这个念头，OBJ-02 的关键钉子就钉上了。

## 总结、词汇表与下一步

- 协作方：Controller.`useCase`；UseCaseImpl.`service`；Service.`endpointDao` + `providerDao` + `configCache` + `adapterRegistry`；DAO.`endpointMapper`。  
- 六映射与 Provider 同构；`save` 多了归属、schema、幂等重试、适配器预检。  
- 网关选择 = 编码点名 + `ThirdConfigSnapshot`，不是搜索。

下一步：L-003 给这扇门配加密胸牌，列表不得回显明文。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-3RD-02 | `ThirdEndpointController.java` | 六映射、权限、协作方 `useCase` | `controller/admin/ThirdEndpointController.java` | 2026-09-14 |
| S-3RD-05 | `ThirdEndpointUseCase` / `Impl` | 五方法、事务、协作方 `service` | `usecase/` | 2026-09-14 |
| S-3RD-06 | `ThirdEndpointService` / `ThirdEndpointDao` / Mapper | 安检顺序、DAO 查询、`toVo` | `service/` `dao/` `mapper/` | 2026-09-14 |
| S-3RD-07 | `ThirdPartyRequest` / `ThirdGatewayAdapter` / `ThirdConfigCacheAdapter` | 点名选择与快照键 | `wta-api` 与 `adapter/` | 2026-09-14 |
| S-3RD-08 | `ThirdProviderAdapterRegistry` / `ThirdProviderAdapterStartupValidator` | 保存期与启动期适配器校验 | `spi/` | 2026-09-14 |
| S-3RD-09 | `ThirdEndpointSecurity` | 方法、路径、JSON、override、allowed、sensitive | `support/ThirdEndpointSecurity.java` | 2026-09-14 |
| S-3RD-10 | 登记表 / third 模块指南 | 未点名默认 layered；gateway 不在 Service | `03-backend-module-modes.md`；`modules/third/index.md` | 2026-09-14 |
