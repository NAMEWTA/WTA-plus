---
lesson_id: L-005
objective_ids: [OBJ-05]
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
source_ids: [S-3RD-07, S-3RD-08, S-3RD-09, S-3RD-06]
---

# Lesson 005：出站只准走大门，专科医生只能在门口盖章

## 学完你能做什么

你能口述一次出站调用：业务模块拿 `ThirdPartyGateway`（`wta-api`），实现类 `ThirdGatewayAdapter.execute` 依次做 **快照、校验、SPI 查找、状态门、限流租约、拼装头与路径、解密凭证头、`ThirdProviderAdapter.prepare`、`ThirdHttpClientFactory` 发 HTTP、可选 `mapResponse`、记录器**。你能列出 Adapter 的全部 `private final` 协作方，并能把失败分类 `ThirdPartyFailureCategory` 对上抛出处。

## 先把宏观地图放在桌上

业务模块不许自己 new `RestClient` 去打第三方。唯一公开出口是：

```text
org.namewta.third.api.ThirdPartyGateway
    execute(ThirdPartyRequest)                              -> ThirdPartyResponse<Object>
    execute(ThirdPartyRequest, Class<T>)                    -> ThirdPartyResponse<T>
    execute(ThirdPartyRequest, ParameterizedTypeReference<T>)
```

实现：

```text
ThirdGatewayAdapter  implements ThirdPartyGateway
  字段：
    configCache        : ThirdConfigSnapshotPort      -> ThirdConfigCacheAdapter
    adapterRegistry    : ThirdProviderAdapterRegistry
    resiliencePolicy   : ThirdResiliencePort          -> ThirdResiliencePolicyAdapter
    invocationRecorder : ThirdInvocationRecorderPort  -> ThirdInvocationRecorderAdapter
    credentialStore    : ThirdCredentialStore         -> ThirdCredentialDao
    credentialCrypto   : ThirdCredentialCryptoPort    -> ThirdCredentialCryptoAdapter
    clientFactory      : ThirdHttpClientFactory
```

**类比：** 学校规定寄信只能交出站收发室。收发室先查通讯录快照，看这栋楼和这扇门还开不开，领一张限流号牌，把允许的地址写在信封上，从锁盒取出贴纸贴到信封（不让你看盒内），必要时请“懂这家公司章法的专科医生”盖章（SPI），再交给邮车（HTTP 工厂）。回来后专科医生可以翻译回执；监控室记一笔（尽力而为）。

**类比失效处：** 真收发室有时会帮你改地址。这里路径和头必须是元数据声明过的，多一个名字就 `REJECTED`。专科医生默认 `prepare` 原样返回、`mapResponse` 返回 null——没有 Bean 时走纯 HTTP。`ThirdHttpClientFactory.createTyped` 和标记接口 `ThirdTypedHttpExchange` **存在**，但 `execute` 主路径用的是 `create(...)` 得到的 `RestClient.method(...)`，不是 `@HttpExchange` 代理。Skill 写“fixed typed contracts use @HttpExchange”；教 Java 时把 typed 当作旁路能力，不要说 execute 已经走它。

## 核心概念与机制

### 直觉讲解

`ThirdPartyRequest` 是一张填空单：两段编码 + path/query/headers 图 + 可选 JSON body。编码空了构造器就拒绝。网关生成 `requestId`（UUID），用 nanoTime 计时，然后尽量把一切失败收成 `ThirdPartyResponse`（带分类），而不是让业务去 catch 一堆 HTTP 异常。

管道是一根直管子，顺序固定。前半段任何配置问题都变成 `CONFIG_UNAVAILABLE`（快照抛错、校验抛错、适配器注册表抛错）。然后看 status。然后抢限流/并发租约，抢不到按 `ThirdRejectedException.category()` 返回（可能是 `RATE_LIMITED`、`REJECTED`、或限流组件不可用时的 `CONFIG_UNAVAILABLE`）。租约是 `ThirdLimitLease`，`finally` 里 `close()` 释放信号量。

真正发信前：展开路径、校验 query/body 名字、合并四层头、可选 SPI `prepare`、工厂建 `RestClient`（跟随重定向 = NEVER）。循环次数来自 `resiliencePolicy.maxAttempts`：非幂等只有 1 次；幂等为 `1 + min(retryCount, 3)`。HTTP 4xx/5xx（`RestClientResponseException`）不重试；`ResourceAccessException` 才可能再试，并区分 TIMEOUT / TRANSPORT。

回来 2xx：按 responseMode 解码 BYTES/TEXT/JSON。SPI `mapResponse` 若返回非 null，就用它（再 record）。否则 `JsonUtils.convertValue` 成调用方要的类型。解码失败走内部 `ThirdDecodeException` → `DECODE`。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 统一出口 | `ThirdPartyGateway` | wta-api 接口，三个 `execute` 重载 |
| 网关适配器 | `ThirdGatewayAdapter` | 模块内唯一实现；编排整条管道 |
| 请求/响应 | `ThirdPartyRequest` / `ThirdPartyResponse<T>` | 响应含 requestId、httpStatus、category、providerMessage、body |
| 失败分类 | `ThirdPartyFailureCategory` | NONE、PROVIDER_DISABLED、ENDPOINT_DISABLED、RATE_LIMITED、CONFIG_UNAVAILABLE、REJECTED、TRANSPORT、TIMEOUT、HTTP、PROVIDER、DECODE |
| 配置快照 | `ThirdConfigSnapshot` | 运行时同时持有 Provider 与 Endpoint 实体 |
| 快照端口 | `ThirdConfigSnapshotPort` | `get` / `evict`；实现带 Redis 10 分钟与集群失效 |
| 弹性端口 | `ThirdResiliencePort` | `acquire` 返回 `ThirdLimitLease`；`maxAttempts` |
| 供应商 SPI | `ThirdProviderAdapter` | `providerCode` / `adapterCode` / `supportsEndpoint` / `prepare` / `mapResponse` |
| SPI 入出 | `ThirdAdapterRequest` / `ThirdAdapterResponse` | prepare 前后的头与 body；map 前的解码 body |
| HTTP 工厂 | `ThirdHttpClientFactory` | `create(baseUrl, connectMs, readMs)` 建 RestClient；另有 `createTyped` 旁路 |
| 记录器 | `ThirdInvocationRecorderPort` | 逻辑 `record` + 尝试 `recordAttempt`；失败吞掉 |

### 机制/因果链

按 `executeInternal` 的真实顺序。三个公开 `execute` 都进这里；带 `ParameterizedTypeReference` 的那个先拒绝 null type，再把 genericType 传进去。

1. **requestId + 计时。**  
2. **快照** `configCache.get(providerCode, endpointCode)`。抛错 → `CONFIG_UNAVAILABLE`，attempts=0。Redis 读失败同样 fail-closed（`ThirdConfigCacheAdapter` 抛 `"第三方配置缓存不可用"`）。未命中则 `ThirdProviderConfigStore.findActiveByCode` + `ThirdEndpointConfigStore.findActiveByProviderAndCode`。  
3. **validateSnapshot**：再次跑 `ThirdEndpointSecurity` 全套（baseUrl、共享头、方法、相对路径、adapter 标识、模式、六个 JSON、override、敏感字段、allowed 名）。配置被污染也不能靠缓存跳过安检。失败同样 `CONFIG_UNAVAILABLE`。敏感字段集合留下给脱敏用。  
4. **SPI 查找** `adapterRegistry.find(provider, endpoint, snapshot.endpoint.adapterCode)`。规则与 L-002 保存期相同。失败 → `CONFIG_UNAVAILABLE`。返回值可为 null（纯 HTTP）。  
5. **状态门**：Provider `status != "0"` → `PROVIDER_DISABLED`；否则 Endpoint `status != "0"` → `ENDPOINT_DISABLED`。供应商优先，与 Skill 句子及 Java if 顺序一致。  
6. **弹性** `resiliencePolicy.acquire(provider, endpoint)`。`ThirdResiliencePolicyAdapter`：  
   - 供应商 QPS：`third:rate:provider:{code}`  
   - 门 QPS：`third:rate:endpoint:{code}:{endpoint}`  
   - 供应商并发信号量、门并发信号量  
   限额 `<=0` 跳过。QPS 超了 `RATE_LIMITED`；信号量拿不到 `REJECTED`；Redisson 抛错 `CONFIG_UNAVAILABLE`（"limit service unavailable"）。成功返回 lambda `ThirdLimitLease`，finally 释放。  
7. **拼装（仍在租约 try 里）**  
   - `expandPath`：模板 `{name}`，schema 若声明则名字必须在 allowed（小写比较）；未知/缺失模板都是 `IllegalArgumentException` → 稍后变 `REJECTED`。  
   - query：有值则 schema 必在，名字必允许。  
   - body：必须是 JSON 对象；字段名必须在 body schema。  
   - 头四层：`declaredHeaders`（调用方，走更严的 `validateHeaderName`）→ `mergeSharedHeaders`（`putIfAbsent`）→ `mergeEndpointOverrides`（覆盖）→ `mergeCredentialHeaders`（再覆盖）。  
8. **凭证** `credentialStore.findByScopes(providerId, endpointId)`：供应商级（endpointId 空）与该门级都取出。按 `credentialType` 先 putIfAbsent 再让 **门级覆盖**。过期 → `CONFIG_UNAVAILABLE` "credential expired"。`credentialCrypto.decrypt` 后只读取 JSON 对象的 `headers` 子对象；解密失败同样 CONFIG_UNAVAILABLE。本课不展示解密结果样例。  
9. **SPI prepare** `new ThirdAdapterRequest(request, snapshot, headers, body)`；`adapter != null` 则 `adapter.prepare`。默认实现原样返回。  
10. **HTTP 工厂** `clientFactory.create(baseUrl, connectTimeoutMs, readTimeoutMs)`。连接超时至少 100ms，`followRedirects(NEVER)`，读超时至少 100ms。  
11. **循环发送** `maxAttempts`。非 GET 且有 body 时补 Content-Type：FORM 用表单编码，否则 JSON。FORM 的 body 必须是 JSON 对象，键值转成 `MultiValueMap`。每次循环：先 `recordAttempt`（completed=false），`retrieve().toEntity(byte[].class)`，成功则 completed=true 并 break；`RestClientResponseException` 记录后 **抛出**（不重试）；`ResourceAccessException` 记录 TIMEOUT 或 TRANSPORT，若还有次数则继续。  
12. **响应** 非 2xx → `HTTP`。2xx 则 `decode`。`adapter.mapResponse` 非 null 则强转返回（调用方 Class 与 SPI 返回类型不一致时这是尖角）。否则 `convert`；失败 `DECODE`。  
13. **其他 catch**：`ThirdRejectedException` 用它自带 category；`IllegalArgumentException` → `REJECTED`；其他 `RestClientException` → `TRANSPORT`；剩下 `RuntimeException` → `PROVIDER`。  
14. **record**：成功失败都 `invocationRecorder.record`；内部异常忽略。尝试级 `recordAttempt` 同样忽略。

`ThirdProviderAdapterRegistry` 构造时把所有 `ThirdProviderAdapter` Bean 按 `providerCode()` 放进不可变 Map，重复编码直接炸启动。`adapterCode()` 默认等于 `providerCode()`。`ThirdProviderAdapterStartupValidator` 在单例就绪后对所有声明了 adapterCode 的门再 `find` 一次。

缓存 `evict(provider, null)` 会对该供应商每个 endpoint 删键并 `invalidationCoordinator.clear`；`evict(provider, endpoint)` 只删一对并 `invalidate`。管理切片的保存/删除就是这样通知网关的。

### 图、表或文本图

**图题 / caption：** 一次 `execute` 的直管道（工作树 2026-09-14）。

```text
业务模块
  ThirdPartyGateway.execute(request[, type])
        v
ThirdGatewayAdapter.executeInternal
  [1] configCache.get(provider, endpoint)           CONFIG_UNAVAILABLE?
  [2] validateSnapshot + sensitive fields           CONFIG_UNAVAILABLE?
  [3] adapterRegistry.find(...)                     CONFIG_UNAVAILABLE?
  [4] provider status / endpoint status             DISABLED?
  [5] resiliencePolicy.acquire  --> ThirdLimitLease RATE_LIMITED/REJECTED?
        try
  [6] expandPath / validate query&body / merge headers
  [7] mergeCredentialHeaders (decrypt, no plaintext to caller)
  [8] adapter.prepare (optional)
  [9] clientFactory.create -> RestClient
 [10] for attempt in 1..maxAttempts
          recordAttempt(START)
          spec.retrieve().toEntity(byte[])
          recordAttempt(FINISH)   retry only ResourceAccessException
 [11] decode BYTES|TEXT|JSON
 [12] adapter.mapResponse or convert
 [13] invocationRecorder.record
        finally lease.close()
        v
ThirdPartyResponse(requestId, codes, httpStatus, category, message, body)
```

**文字等价物：** 业务只调用 `ThirdPartyGateway` 的三个 execute。实现类先取 Provider+Endpoint 快照并重新安检，再按编码找可选 SPI。楼或门停用立即分类返回。接着用 Redisson 限额拿到必须在 finally 释放的租约。租约内把路径和声明过的参数拼好，按调用方头 → 共享头（不覆盖）→ 门覆盖 → 凭证头（覆盖）合并，解密只发生在网关内存。SPI 可改准备好的请求。工厂按该供应商的 origin 与超时建 **禁止跟随重定向** 的 RestClient。物理发送次数受幂等与 retryCount 限制；传输类错误可重试，HTTP 状态错误不可。响应按模式解码后，SPI 可提供已映射的 `ThirdPartyResponse`，否则按调用方类型转换。全过程用记录器尽力写回放，但记录失败不替换已经决定的分类。

**图的边界：** 本图不教某个具体供应商如何签名（那是某个 `ThirdProviderAdapter` 实现，本 Change 范围外）。不把 `createTyped` 画进主路径。不展开 Redis 失效指纹算法。不列出 Nacos/OSS。`ThirdDecodeException` 是 Adapter 的私有静态内部类，不是独立顶层类型——对外只表现为 `DECODE`。

### 正例、反例与边界

**正例 1：** `ThirdPartyRequest.of("AcmePay", "CreateOrder")` 再带声明过的 body。快照命中，楼和门都是 `"0"`，限额 0 跳过 Redisson，无 SPI Bean，JSON POST 一次 2xx，返回 `category=NONE` 且 `isSuccess()==true`，管理端过几秒能在 invocation 列表看见 SUCCESS。

**正例 2：** 门 `idempotent=true`、`retryCount=2`。第一次 `ResourceAccessException` 且 cause 是 `SocketTimeoutException`：记录 TIMEOUT 尝试，再试。第二次 200：逻辑行 attempts=2。若两次都超时：逻辑分类 TIMEOUT。

**正例 3：** 配置了 adapterCode 且 Registry 找到 Bean。`prepare` 给 headers 加签名字段（具体算法范围外）。`mapResponse` 把供应商业务错误变成 `category=PROVIDER` 的 `ThirdPartyResponse`。网关不再走默认 convert。

**反例 1：** 业务模块在自己的 Service 里 `RestClient.create()`。这绕过快照、限额、凭证、观测。课程目标就是能拒绝这条近路。

**反例 2：** 调用方 header 带 `Authorization`。`validateHeaderName` 把它列入 CALLER_BLOCKED。凭证贴纸可以经 `validateConfiguredHeaderName` 合并（配置头比调用方头松），但那是锁盒路径，不是让业务把秘密放进 `ThirdPartyRequest.headers`。

**反例 3：** 以为 `ParameterizedTypeReference` 重载会把 genericType 传进 HTTP 层。源码在进内部时仍 `executeInternal(..., Object.class, responseType.getType())`，HTTP 始终收 `byte[]`，泛型只用于事后 `convert`。

**边界：**

- 跟随重定向关闭：3xx 不会自动改 host。这是防跨源跳转，不是丢了功能。  
- FORM 模式把 JSON 对象拍扁成文本表单，嵌套对象会变成它们的 `asText()`。  
- `mapResponse` 返回类型与 `Class<T>` 不一致时有未检查强转——SPI 作者必须守约。  
- 快照缓存 10 分钟，但管理 `evict` 会主动删。缓存组件不可用是关闭，不是降级打裸 HTTP。  
- `failure(..., message)` 的 message 参数在当前 `new ThirdPartyResponse` 调用里经常传 `null`；分类靠 category 而不是靠文案。  
- 限流键与并发键名字见上，0 限额表示关闭该闸，不是拒绝一切。

## 变式与迁移

- **变式 A（只读 GET 门）：** requestMode 常为 QUERY；body 为 null 则 `validateBody` 直接返回。不要给 GET 硬塞 body：发送循环对 GET 不写 spec.body。  
- **变式 B（无 SPI 的纯 HTTP 门）：** `adapter == null`，跳过 prepare/mapResponse，解码后 convert。这是默认。  
- **变式 C（凭证过期）：** 合并头阶段抛 `ThirdRejectedException(CONFIG_UNAVAILABLE)`，被 catch 转成同分类响应，租约仍释放。  
- **迁移：** 新供应商先登 Provider（L-001）→ Endpoint（L-002）→ 可选 Credential（L-003）→ 若需签名再提供一个 `ThirdProviderAdapter` Bean（`providerCode()` 唯一）→ 业务只依赖 `ThirdPartyGateway`。观测在 L-004 回看。

## 常见误区

1. **“Gateway 是 Third*Service 的一个方法。”** 实现类在 `adapter/gateway`，实现的是 wta-api 接口。Skill 把 gateway 写进 service 职责，与 Java 冲突，以 Java 为准。  
2. **“Registry.find 找不到适配器就调不成。”** 没配 adapterCode 时允许 null，走纯 HTTP。配了才强制 Bean。  
3. **“HTTP 500 会按 retryCount 重试。”** 只有 `ResourceAccessException` 会 continue。  
4. **“记录器挂了调用就失败。”** record 的 catch 明确忽略。  
5. **“createTyped 就是 execute 内部在用的。”** 主路径是 `RestClient` + `byte[]`。typed 是工厂上的另一方法。  
6. **“业务可以传完整 URL。”** URL 来自快照 origin + 相对路径展开。请求对象没有 url 字段。

## 非评分暂停

打开 `ThirdGatewayAdapter`，把七个 `private final` 字段读出声。再用手指顺着 `executeInternal`：get 快照 → validate → registry → status → acquire → 拼装/凭证/prepare → factory → 循环 → decode/map → record → finally close。问自己：哪一步会变成 `CONFIG_UNAVAILABLE`，哪一步才是 `HTTP`。

不要打分。能按顺序讲完且不发明类型，OBJ-05 的出站钉子就钉上了。

## 总结、词汇表与下一步

- 协作方七个：`configCache`、`adapterRegistry`、`resiliencePolicy`、`invocationRecorder`、`credentialStore`、`credentialCrypto`、`clientFactory`。  
- 顺序：快照与安检 → SPI 查找 → 启停门 → 租约 → 声明式拼装与凭证解密 → prepare → RestClient → map/convert → 尽力记录。  
- 业务只认 `ThirdPartyGateway` + `ThirdPartyRequest` / `ThirdPartyResponse` / `ThirdPartyFailureCategory`。  
- 管理四切片（L-001～L-004）是给这根管子填燃料和看仪表，不是第二根管子。

本 Change 的五课到此结束。作业不会自动出现；若要练习，需另开 H-homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-3RD-07 | `ThirdPartyGateway.java` / `ThirdPartyRequest.java` / `ThirdPartyResponse.java` / `ThirdPartyFailureCategory.java` / `ThirdGatewayAdapter.java` | 公开合同、execute 管道、失败分类 | `backend/wta-api/.../third/api/`；`adapter/gateway/ThirdGatewayAdapter.java` | 2026-09-14 |
| S-3RD-08 | SPI、crypto、resilience、recorder、cache | Registry、Adapter 默认方法、限流键、AES-GCM 解密点、upsert、Redis 键 | `spi/` `adapter/` | 2026-09-14 |
| S-3RD-09 | `ThirdHttpClientFactory.java` / `ThirdTypedHttpExchange.java` / `ThirdEndpointSecurity.java` | 工厂、禁止重定向、typed 旁路、运行时安检 | `http/` `support/` | 2026-09-14 |
| S-3RD-06 | `ThirdCredentialDao.findByScopes` 等存储 | 凭证范围读取与 DAO 实现端口 | `dao/` `port/` | 2026-09-14 |
