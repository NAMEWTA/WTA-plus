---
lesson_id: L-003
objective_ids: [OBJ-03]
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
source_ids: [S-3RD-03, S-3RD-05, S-3RD-06, S-3RD-08, S-3RD-09]
---

# Lesson 003：Credential 是上锁的盒子，柜台只展示盒号

## 学完你能做什么

你能口述 Credential 管理切片：`ThirdCredentialController` → `ThirdCredentialUseCase` / `ThirdCredentialUseCaseImpl` → `ThirdCredentialService` → `ThirdCredentialDao` / `ThirdCredentialMapper`，以及加密边界 `ThirdCredentialCryptoPort` / `ThirdCredentialCryptoAdapter`。你能指出列表 **没有** secret、密文、nonce、authTag；保存时明文只进 `encrypt`；删除是软删并 `evict`。本课不出现任何真实密钥、口令或样例 token 字符串。

## 先把宏观地图放在桌上

胸牌不能摊在桌上给参观者看。管理员可以：按供应商（可选门牌）列出盒子、提交一份新的/更新的秘密信封、按 id 扔掉盒子。没有 get-by-id，没有“再显示一次明文”的接口。

```text
HTTP /third/credential
  ThirdCredentialController
       字段 credentialUseCase : ThirdCredentialUseCase
       GET  /list                         list
       POST /                             save   INSERT  third:credential:add
       POST /{credentialId}/remove        remove DELETE  third:credential:remove
            |
            v
  ThirdCredentialUseCaseImpl
       字段 service : ThirdCredentialService
       list 无事务；save/remove 有 @DSTransactional
            |
            v
  ThirdCredentialService
       字段 credentialDao, providerDao, endpointDao, crypto, configCache
            |
            +--> crypto.encrypt(...)     ThirdCredentialCryptoPort
            |         实现：ThirdCredentialCryptoAdapter
            +--> credentialDao.insert|update|findByScope|findById
            \--> configCache.evict
```

**类比：** 银行柜台让你看保险箱编号、开户行、到期日，绝不把箱里的纸再递回给你。存的时候你把纸塞进窗口，窗口当场锁进盒子。

**类比失效处：** 真银行可能允许持证人在隔间再看一次内容。本切片 **没有** 解密给管理端的 API。`decrypt` 只给网关出站（L-005 的 `mergeCredentialHeaders`）用。把“列表 VO 的 `enabled` 字段”理解成“开关明文”也不对：Service 往这个 record 组件填的是 `c.getDelFlag()`。

## 核心概念与机制

### 直觉讲解

Controller 的字段不叫 `useCase`，叫 **`credentialUseCase`**。这是源码名字，口述时要说真名，不要“统一改成 useCase”。

列表必须带 `providerCode`，`endpointCode` 可选。Service 先把供应商找活，再决定范围：没有门牌 → 只列出 `endpointId IS NULL` 的供应商级盒子（`scopeType` 在保存时写成 `"PROVIDER"`）；有门牌 → 列出该门的盒子（保存时 `"ENDPOINT"`）。`findByScope` **另外**过滤 `delFlag="0"`。映射成 `ThirdCredentialVo` 时带 id、两个编码、scope、类型、`kekVersion`、到期、版本、以及 delFlag——**不带** `secretJson`、`ciphertext`、`nonce`、`authTag`。

保存时 Bo 里的 `secretJson` 是必填的。Service 立刻调用 `crypto.encrypt(scope, credentialType, bo.getSecretJson())`，只把返回的 `EncryptedSecret` 的字节放进实体。然后 `kekVersion` 写成 `"v1"`。`bo.getEnabled()==true` 则 `delFlag="0"`，否则 `"1"`。成功后 `evict`。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 凭据 | Credential | `ThirdCredential`：范围 + 类型 + 密文字节，不是明文头 |
| 管理控制器 | `ThirdCredentialController` | `/third/credential`；协作方 `private final ThirdCredentialUseCase credentialUseCase` |
| 用例 | `ThirdCredentialUseCase` / `Impl` | 方法只有 `list` / `save` / `remove`；Impl 协作方 `private final ThirdCredentialService service` |
| 领域服务 | `ThirdCredentialService` | 协作方：`ThirdCredentialDao credentialDao`、`ThirdProviderDao providerDao`、`ThirdEndpointDao endpointDao`、`ThirdCredentialCryptoPort crypto`、`ThirdConfigSnapshotPort configCache` |
| 加密端口 | `ThirdCredentialCryptoPort` | `encrypt` 返回 `EncryptedSecret(ciphertext, nonce, authTag)`；`decrypt(ThirdCredential)` |
| 加密适配器 | `ThirdCredentialCryptoAdapter` | 实现端口；协作方 `ThirdCryptoProperties properties`；算法 `AES/GCM/NoPadding` |
| 主密钥配置 | `ThirdCryptoProperties` | 前缀 `third.crypto` 的 `masterKey`；由 `ThirdConfiguration` `@EnableConfigurationProperties` 打开 |
| 写入 Bo | `ThirdCredentialBo` | 含 `secretJson`、`enabled`、`expiresAt`；**本课不举例该 JSON 内容** |
| 读出 Vo | `ThirdCredentialVo` | record 九个组件，无秘密字段 |
| 范围 | `scopeType` | `"PROVIDER"` 或 `"ENDPOINT"`，由有没有 endpoint 决定，不能事后改归属 |
| 附加认证数据 | AAD | `scopeType + ":" + credentialType` 的 UTF-8 字节，绑进 GCM |

### 机制/因果链

#### 三条 HTTP 映射（没有 get / status / 双 save）

1. **list**  
   `GET /third/credential/list?providerCode=&endpointCode=`  
   权限 `third:credential:list`。`providerCode` 必填。  
   `credentialUseCase.list` → `service.list`：  
   - `provider(code)` → `providerDao.findActiveByCode`，空码或找不到都失败。  
   - endpointCode 空白 → `endpointId=null`；否则 `endpoint(providerId, code)` → `endpointDao.findActiveByProviderAndCode`。  
   - `credentialDao.findByScope(providerId, endpointId)`。endpointId 为 null 时 SQL 是 `endpoint_id IS NULL`，**不会**顺便列出该供应商下各门的盒子。  
   - Vo 使用传入的 `endpointCode` 参数（供应商级列表时这个参数本身就是空）。

2. **save**  
   `POST /third/credential`，`@Log` INSERT，权限 `third:credential:add`。更新也走这个 POST，没有 `/save`，也没有 `third:credential:edit`。  
   UseCaseImpl `@DSTransactional save` → `service.save`：  
   - 解析 provider / 可选 endpoint，得到 `scope = endpoint==null ? "PROVIDER" : "ENDPOINT"`。  
   - `credentialType` 走 `validateIdentifier(..., "Credential type")`。  
   - `secretJson` 空白 → `"Credential secret is required"`。  
   - 有 id 则 `findById`；找不到或 `delFlag != "0"` → `"Credential not found"`。  
   - 已有行的 `providerId` 必须仍是这家；`endpointId` 必须仍等于这次解析结果，否则 `"Credential scope cannot be changed"`。  
   - 新行发 id；`encrypt`；写入 ciphertext/nonce/authTag；`kekVersion="v1"`；`version` 空则 1 否则 +1；`delFlag` 由 `enabled` 决定。  
   - insert/update 必须 1 行；`evict(providerCode, endpointCode或null)`。

3. **remove**  
   `POST /third/credential/{credentialId}/remove`，`@Log` DELETE，权限 `third:credential:remove`。  
   先 `findById`，没有或已删都报 `"Credential not found"`（不区分这两种，避免探测）。  
   `delFlag="1"` 后 update。再查出还活着的 Provider；若凭证绑了 endpoint 再查门，取出 `endpointCode`（门已删则为 null）。最后 `evict`。Provider 此时找不到会 `"Provider not found"`——缓存作废依赖还活着的供应商编码。

#### 加密适配器在做什么（不写密钥材料）

`ThirdCredentialCryptoAdapter.encrypt`：

1. JSON 空白 → 中文 `"凭据内容不能为空"`。  
2. 12 字节随机 nonce。  
3. `Cipher.getInstance("AES/GCM/NoPadding")`，128 bit tag，AAD = `scopeType:credentialType`。  
4. `doFinal` 后切开 ciphertext 与 authTag。  
5. 失败 → `"凭据加密失败"`。

`decrypt`（管理切片不调用，但同一适配器）：要求 `kekVersion` 必须是 `"v1"`，nonce 12 字节，tag 16 字节，密文非空；否则 `"凭据不可用"`。解不开 → `"凭据解密失败"`。

`masterKey()` 从 `ThirdCryptoProperties.getMasterKey()` 读配置：先尝试 Base64，否则 UTF-8 字节；长度必须是 16/24/32。未配置或长度不对会抛中文 ServiceException。本课不讨论如何生成该配置值。

Skill `third/index.md` 把 crypto 写进 service 层职责。Java 里 Service 只持有端口 `ThirdCredentialCryptoPort`，实现类在 `adapter/security/ThirdCredentialCryptoAdapter`。按 Java 教。

### 图、表或文本图

**图题 / caption：** 明文只在保存请求内存里活一瞬间。

```text
POST /third/credential   ThirdCredentialBo.secretJson
        |
        v
ThirdCredentialService.save
        |
        |  crypto.encrypt(scopeType, credentialType, secretJson)
        |         AES-GCM + AAD(scopeType:credentialType)
        v
ThirdCredential 行：ciphertext / nonce / authTag / kekVersion=v1
        |
        |  列表 GET /list
        v
ThirdCredentialVo（无 secretJson，无三组字节）
        |
        |  出站（L-005，本课只标出口）
        v
credentialCrypto.decrypt(row)  --仅网关 mergeCredentialHeaders
```

**文字等价物：** 管理保存把 Bo 里的秘密 JSON 交给加密端口，数据库只留密文、nonce、认证标签和版本号 `"v1"`。管理列表从 DAO 取出未删除行后，构造 Vo 时丢掉全部秘密字段。同一适配器的解密方法不挂在 Controller 上。网关出站时才会按范围取出凭据并解密，把其中声明的 headers 合并进请求（L-005）。删除只把 `delFlag` 置 `"1"`，同样作废配置缓存。

**图的边界：** 本图不画 Redis 快照内容（快照是 Provider+Endpoint，**不含**凭据明文）。不解释 GCM 数学。不给 `secretJson` 示例键名以外的真实取值；即便提到结构，也只说网关认顶层对象里的 `headers` 对象——那是出站契约，不是让你在 Lesson 里填值。`ThirdCredentialMapper` 没有自定义 XML，全是 `BaseMapper`。

### 正例、反例与边界

**正例 1：** `GET /third/credential/list?providerCode=AcmePay`。只返回这家、`endpointId` 为空、`delFlag=0` 的盒子。Vo 里能看到 `scopeType=PROVIDER`、`kekVersion=v1`，看不到密文。

**正例 2：** 保存时带 endpointCode。Service 把 `scopeType` 写成 `ENDPOINT`，AAD 绑的是这个 scope 和类型。以后若用另一 scope 去 decrypt，GCM 认证会失败，适配器报凭据解密失败——这是故意绑死，不是事故。

**正例 3：** `remove` 对已 `delFlag=1` 的 id 再打一次，仍然 `"Credential not found"`。列表也已经看不见它。

**反例 1：** 想做 `GET /third/credential/{id}` 把 secret 再读出来。工作树没有这个方法。不要在 Lesson 或代码里补“调试用明文回显”。

**反例 2：** 把已有盒子从供应商级改成某扇门。`Credential scope cannot be changed`。该新建而不是改归属。

**反例 3：** 列表不传 `providerCode`。Controller 的 `@RequestParam String providerCode` 没有 `required=false`，请求在绑定阶段失败。

**边界：**

- 保存权限字只有 `add`。更新已有 `credentialId` 仍走 INSERT 日志和 add 权限。这是当前 Java，不是文档笔误。  
- `enabled=false` 的保存会把 `delFlag` 写成 `"1"`，效果接近删除：列表的 `findByScope` 立刻过滤掉。没有单独的 status API。  
- Vo 组件名叫 `enabled`，值却是 delFlag。读列表时看到 `"0"` 表示未删除行，不要把它解释成“开关打开的明文”。  
- `findByScopes`（注意复数）给网关用：一次取出供应商级 **或** 该门级。管理 `list` 用的是单数 `findByScope`，范围更窄。不要把两个方法记成同一个。  
- 本课禁止编造主密钥、Authorization 值、示例口令。

## 变式与迁移

- **变式 A（轮换秘密）：** 带上已有 `credentialId` 再 POST，仍必须提交一份新的 `secretJson`（Bo 字段 `@NotBlank`）。version +1，密文整段替换，nonce 新随机。旧密文不留历史表。  
- **变式 B（只想停用）：** 没有 status 接口。可 `save` 且 `enabled=false`，或走 `remove`。两者都把 delFlag 置 `"1"`。  
- **变式 C（门已经删了，盒子还在）：** `remove` 时 endpoint 查不到，`endpointCode` 按 null 去 `evict`。列表若按该门过滤会先因 `"Endpoint not found"` 失败。  
- **迁移到出站：** 盒子存在且未过期，网关才会 `decrypt`。过期抛 `ThirdRejectedException(CONFIG_UNAVAILABLE, "Third-party credential expired")`（L-005）。

## 常见误区

1. **“列表里的 enabled 是真正开关，secret 只是没画在表格上。”** Vo 构造函数根本没接 secret；数据库列也不会被 toVo。  
2. **“Service 自己做 AES。”** Service 只调端口。算法、nonce 长度、AAD 在适配器。  
3. **“Controller 字段叫 useCase，和其他切片一样。”** 本切片叫 `credentialUseCase`。  
4. **“供应商级列表会看到下属所有门的盒子。”** `findByScope(..., null)` 是 `isNull(endpointId)`。  
5. **“KekVersion 可以随便填 v2 做升级。”** 解密硬编码只认 `"v1"`。管理保存也写死 `"v1"`。没有本课范围外的密钥轮换协议。

## 非评分暂停

打开 `ThirdCredentialController`，数方法是不是只有三个，字段是不是 `credentialUseCase`。打开 `ThirdCredentialVo`，用手指点每一个组件，确认没有 ciphertext。打开 `ThirdCredentialService.list` 的 `new ThirdCredentialVo(...)`，看最后一项是不是 `c.getDelFlag()`。

不要打分，也不要在笔记里抄任何 secret 样例。

## 总结、词汇表与下一步

- 协作方：Controller.`credentialUseCase`；UseCaseImpl.`service`；Service.`credentialDao` + `providerDao` + `endpointDao` + `crypto` + `configCache`；DAO.`credentialMapper`；适配器.`properties`。  
- 三条映射：list 不回显明文；save 必加密；remove 软删。  
- 加密：AES-GCM、AAD 绑 scope 与类型、kek `v1`。  
- 明文不得出现在 Lesson、日志思路或 Vo 里。

下一步：L-004 看调用记录与统计——那是监控室，不是再开一扇改配置的门。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-3RD-03 | `ThirdCredentialController.java` | 三映射、权限、协作方 `credentialUseCase` | `controller/admin/ThirdCredentialController.java` | 2026-09-14 |
| S-3RD-05 | `ThirdCredentialUseCase` / `Impl` | list/save/remove、事务、协作方 `service` | `usecase/` | 2026-09-14 |
| S-3RD-06 | `ThirdCredentialService` / `Dao` / `Mapper` / Bo / Vo | 范围、不回显、软删、`findByScope` | `service/` `dao/` `mapper/` `domain/` | 2026-09-14 |
| S-3RD-08 | `ThirdCredentialCryptoAdapter` / `ThirdCryptoProperties` / `ThirdConfiguration` | AES-GCM、AAD、kek v1、配置前缀 | `adapter/security/` `config/` | 2026-09-14 |
| S-3RD-09 | `ThirdEndpointSecurity.validateIdentifier` | credentialType 标识符规则 | `support/ThirdEndpointSecurity.java` | 2026-09-14 |
