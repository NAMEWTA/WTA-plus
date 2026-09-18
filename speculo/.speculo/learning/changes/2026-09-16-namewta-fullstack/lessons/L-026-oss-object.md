---
lesson_id: L-026
objective_ids: [OBJ-26]
claimed_cells: [A:SysOssController.list,listByIds,downloadUrl,remove]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: four-windows-on-disk
    minutes: 10
  - segment: url-strip-and-pending-delete
    minutes: 9
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L026-01, S-L026-02, S-L026-03, S-L026-04, S-L026-05, S-L026-06, S-L026-07, S-L026-08]
---

# Lesson 026：仓库门口的卡片柜——`SysOssController` 列表 / 下载 URL / 删除

## 学完你能做什么

打开 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssController.java`，你能**口述这扇管理卡片窗**：门牌、四扇公开 HTTP、谁给可用地址、谁只给黑掉地址的卡片、删除会不会立刻砸仓库里的箱子。口试名单就是矩阵 (a) 这一行，方法名以**磁盘**为准：

**`A:SysOssController.list,listByIds,downloadUrl,remove`**

2026-09-16 工作树里，这个类**正好四扇窗**，没有第五扇：

| Java 方法 | HTTP | 权限 | `@Log` | 返回 |
| --- | --- | --- | --- | --- |
| `list` | `GET /resource/oss/list` | `system:oss:list` | 无 | `R<PageResult<SysOssVo>>` |
| `listByIds` | `GET /resource/oss/listByIds/{ossIds}` | `system:oss:query` | 无 | `R<List<SysOssVo>>` |
| `downloadUrl` | `GET /resource/oss/{ossId}/download-url` | `system:oss:download` | 无 | `R<OssService.OssAccessUrl>` |
| `remove` | `DELETE /resource/oss/{ossIds}` | `system:oss:remove` | title=OSS对象存储，`DELETE` | `R<Void>`（`toAjax`） |

`OssProtocolCutoverUnitTest` 把旧协议钉死：**没有** `upload`、**没有** `download`、**没有** `MultipartFile`、**没有** `ResponseEntity` 字节流。菜单种子里的 `system:oss:upload` 挂在隔壁 `SysOssUploadController`，不是本课这四扇。

`wta-system` 在登记表是 **classic**：`Controller → ISysOssService / OssService → SysOssServiceImpl → Mapper`。房间里多了一位管家 `OssLifecycleManager`，仍不是 layered UseCase。不要口述成五层。

本课**不宣称**你会拆 OSS 配置切换（L-025 / `SysOssConfigController`）、直传票据五步（L-027）、迁移批（L-028）、浏览器 `createOssUploadClient`（L-029），或把 `createSystemService.resources.oss` 全表再讲一遍。今天只认：**卡片柜怎么翻、怎么领短时出门条、怎么盖「待撕」章。**

## 先把宏观地图放在桌上

L-001 已经把文件柜子指到 MinIO/OSS。L-005 把 HTTP/JSON 和 `wta-api` 拆成两条河：浏览器走 `/resource/oss/*`，业务模块走 Java `OssService`。L-007 把厨房抽屉 `systemService.resources.oss` 插进厅堂，但那张抽屉还含直传口，本课只对上**卡片柜这四枪**。L-025 才是换默认仓库钥匙；本课假定仓库已经能服务。

2026-09-16 工作树：OSS 管理面是**四份 Controller、四块门牌**，不要合成一个超级 `/resource/oss`：

```text
管理员浏览器 / 已登录调用方
        │
        ├─ /resource/oss                 SysOssController          ← 本课四扇
        │     GET    /list
        │     GET    /listByIds/{ossIds}
        │     GET    /{ossId}/download-url
        │     DELETE /{ossIds}
        │
        ├─ /resource/oss/config          SysOssConfigController    L-025
        ├─ /resource/oss/uploads         SysOssUploadController    L-027
        └─ /resource/oss/migrations      SysOssMigrationController L-028
```

本课这一头再往下走：

```text
SysOssController
  ├─ ISysOssService ossService
  │     list / listByIds / remove
  │     （同接口还有 getById、upload(File)，本类零调用）
  └─ OssService publicOssService
        downloadUrl → resolveAccessUrl
              │
              v
SysOssServiceImpl  （一份 @Service，同时 implements 两个接口）
  ├─ SysOssMapper            表 sys_oss
  └─ OssLifecycleManager     行锁、引用、访问 URL、盖 PENDING
        ├─ SysOssRefMapper   表 sys_oss_ref
        └─ OssObjectStore    按 AccessPolicy 给公共地址或短时签名
```

**类比：** 把对象存储想成冷库。`sys_oss` 是门口**卡片柜**（每张卡片写着箱子编号、原名、哪把仓库钥匙）。`sys_oss_ref` 是**借出登记簿**（哪张业务表、哪个主键正占用这只箱子）。MinIO/云桶才是冷库里的纸箱。本课这扇窗是传达室：你可以翻卡片、可以领一张短时出门条、可以把没人借走的卡片盖「待撕」章。你**不能**从这扇窗把整箱货扛走，也不能从这扇窗把货推进库——进库走直传售票口（L-027）。

**类比失效处：**

1. 传达室**会改卡片**。`remove` 会把 `delete_state` 写成 `PENDING`，不是只看不写。
2. 卡片上的「地址」栏对管理员是**涂黑的**。列表两扇窗把 `url` 设成 `null`。出门条是另一扇专用窗。
3. 「待撕」不是立刻用碎纸机。HTTP 删除**不**调 `objectStore.delete`。真正砸箱子在过期清理，而且清理默认关着。
4. 业务模块不要来敲这扇管理窗领文件。注释写明：普通业务先做自己的权限校验，再调内部 `OssService`。管理窗的权限是 `system:oss:*`，不是业务 owner 授权。
5. 四块门牌都在 `wta-system` classic 房间，不是四个模块。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **先数四扇，再背路径。** 打开 `SysOssController.java`，公开映射就是 list / listByIds / downloadUrl / remove。测试禁止再出现 `upload`、`download`、匿名 `public/{ossId}`。
2. **门牌是 `/resource/oss`，不是 `/system/oss`。** 菜单组件是 `system/oss/index`，HTTP 前缀是 `/resource/oss`。口试两套不要并。
3. **两根导线，一份实现。** 字段 `ossService` 类型 `ISysOssService`；字段 `publicOssService` 类型 `OssService`。磁盘上 `SysOssServiceImpl implements ISysOssService, OssService`。下载走第二条线，是为了让管理窗和跨模块 SPI 共用 `resolveAccessUrl`。
4. **列表是目录，不是下载。** `queryPageList` 查完每一行都跑 `managementView`：`url = null`，再补引用数。管理员看见「有这个文件」，拿不到能用的链接。
5. **按 id 列表也是目录。** `listByIds` 对每个 id 走缓存版 `getById`，找不到就丢弃（结果里没有 null）。同样涂黑 URL。
6. **出门条单独一扇。** `GET /{ossId}/download-url` 才调用 `resolveAccessUrl`。公共对象给稳定 URL、`expiresAt=null`；私有对象给短时签名，默认 TTL **2 分钟**。
7. **权限四颗，不要混。** 翻页 `system:oss:list`；按 id 查 `system:oss:query`；出门条 `system:oss:download`；删除 `system:oss:remove`。菜单另有 `system:oss:upload`，本类零引用。
8. **只有删除盖章。** 四扇里唯有 `remove` 带 `@Log`。列表和出门条不进操作日志。
9. **删除先问借出簿。** 还有 `sys_oss_ref` 有效行 → 抛 `OBJECT_REFERENCED`，仓库零动作。没人借才盖 `PENDING`。
10. **盖章 ≠ 砸箱。** `deleteObjects` 只 `markDeletePending`。单测名字就叫 `manualDeleteShouldOnlyPersistPendingState`。
11. **盖了章就领不出门条。** `requireDownloadable` 看见 `PENDING` 抛 `OBJECT_DELETE_PENDING`。列表却**不**按这个状态过滤，卡片可能还在柜上。
12. **旧的「逗号拼 URL」是过期兼容。** `OssService.selectUrlByIds` / `selectByIds` 标 `@Deprecated(since="6.0.0")`。新代码逐个 `resolveAccessUrl`。管理 HTTP **没有**这两扇。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| OSS 对象控制器 | `SysOssController` | `@RestController`，`@RequestMapping("/resource/oss")`，`extends BaseController`。公开 HTTP 方法四个，与矩阵 (a) 该行同名 |
| 管理列表 | `list` | `GET /list`。入参 `SysOssBo` + `PageQuery`，`@Validated(QueryGroup.class)`。BO 字段上**没有** QueryGroup 注解，校验组几乎是空壳 |
| 按 id 列表 | `listByIds` | `GET /listByIds/{ossIds}`。`@PathVariable Long[]`，`@NotEmpty`。内部 `Arrays.asList` 再交给服务 |
| 管理面下载授权 | `downloadUrl` | `GET /{ossId}/download-url`。返回 `OssService.OssAccessUrl`，不是文件字节 |
| 访问地址 | `OssAccessUrl` | record：`accessType`、`url`、`expiresAt`、`fileName`。`accessType` 工作树是 `"PUBLIC"` 或 `"PRIVATE"` |
| 私有下载授权 | `OssDownloadUrl` | record：`url`、`expiresAt`、`fileName`。`presignDownload` 用；管理窗 `downloadUrl` **不**直接返回这个类型，私有路径会把它折进 `OssAccessUrl` |
| 对象存储表 | `sys_oss` | 实体 `SysOss`，`@TableId ossId`。列含 fileName（对象键）、originalName、url、service（配置 key）、isTemp、expireTime、deleteState |
| 对象视图 | `SysOssVo` | 列表返回体。多 `createByName`（`@Translation` 用户名）、`referenceCount`、`references`。管理查询把 `url` 抹掉 |
| 查询对象 | `SysOssBo` | 筛 fileName/originalName like、suffix/url/service/isTemp/createBy eq、`params.beginCreateTime/endCreateTime` |
| 模块内服务 | `ISysOssService` | `queryPageList`、`listByIds`、`getById`、`upload(File, SysOssExt)`、`deleteWithValidByIds`。本课 HTTP 只用前两个和最后一个 |
| 跨模块 OSS 合同 | `OssService` | `wta-api`。管理窗只用 `resolveAccessUrl`。另有引用协调、快照、元数据、`presignDownload`、两只 deprecated 批量 URL |
| 生命周期管家 | `OssLifecycleManager` | 行锁、`sys_oss_ref` 绑定、访问分类、盖 PENDING、过期清理。Controller 不直接注入它 |
| 管理视图投影 | `managementView` | 复制 VO → `setUrl(null)` → `snapshot` 填引用。列表两扇必经 |
| 对象缓存 | `CacheNames.SYS_OSS` | 字面量 `"sys_oss#30d"`。`getById` 标 `@Cacheable`。删除路径**未见** `@CacheEvict` |
| 业务引用表 | `sys_oss_ref` | 实体 `SysOssRef`。`refType` 必须是物理表名，`refId` 是真实主键。引用**不**当权限 |
| 删除中状态 | `PENDING` | `OssLifecycleManager.DELETE_PENDING`。`markDeletePending` 同时把 `is_temp='Y'` |
| 访问策略 | `AccessPolicy` | 发布收缩测试只认 `PRIVATE`、`PUBLIC_READ`。解析时 PUBLIC_READ → `accessType="PUBLIC"` |
| 存储就绪 | `OssStorageReadinessRegistry.requireServing` | 下载前按 `sys_oss.service` 检查该配置能否服务。失败 `STORAGE_NOT_SERVING` |
| 资源锚点 | `systemOssResource` | 前端 `packages/domains/system/src/oss/index.ts`：`{ controller: 'SysOssController', basePath: '/resource/oss' }`。本课不认厨房方法表 |

### 机制/因果链

#### 1. 两根导线怎么接到同一份实现

类上两个 `private final`：

- `ISysOssService ossService` → `list` / `listByIds` / `remove`
- `OssService publicOssService` → 只有 `downloadUrl`

`SysOssServiceImpl` 一份 `@Service` 同时实现两个接口。Spring 按类型注入，运行时是同一个对象的两张脸。口试不要说「有两个 OSS 服务类」。

`ISysOssService` 里还有：

- `getById`：给 `listByIds` 走缓存，**没有** `GET /resource/oss/{ossId}` 详情窗。
- `upload(File, SysOssExt)`：服务端文件上传遗留口。本 Controller **零调用**；协议测试禁止 `MultipartFile`。浏览器直传是 L-027。

`OssService` 里还有 `reconcileReferences` / `snapshot` / `objectMetadata` / `presignDownload`。那些是业务模块内部合同，**没有**对应本课 HTTP。`bind` / `unbind` 不在 `OssService` 上，只活在管家里。

#### 2. `list`：翻页目录，地址涂黑

```text
GET /resource/oss/list
  @SaCheckPermission("system:oss:list")
  无 @Log
  SysOssBo @Validated(QueryGroup.class) + PageQuery
```

`queryPageList`：

1. `buildQueryWrapper`：`QueryBuilder.lambda(SysOss.class)`。fileName / originalName **like**；suffix / url / service / isTemp **eq 文本**；createBy **eq**；创建时间吃 `params.beginCreateTime` / `endCreateTime`；`orderByAsc(ossId)`。
2. **不**按 `delete_state` 过滤。PENDING 行仍可出现。
3. `ossMapper.selectVoPage`。
4. 每条 `managementView`。
5. `PageResult.build(records, total)`。

`managementView` 三步：Bean 拷贝 → `view.setUrl(null)` → `lifecycleManager.snapshot(ossId)` 写入 `referenceCount` 和 `references`。单测 `managementListNeverReturnsAccessUrl` 锁的就是「库里就算存了 https，列表也是 null」。

这是设计，不是漏字段。注释原句：管理查询不返回可直接使用的 URL；下载必须经过专用权限入口。

#### 3. `listByIds`：按号抽卡片，缺号就丢

```text
GET /resource/oss/listByIds/{ossIds}
  @SaCheckPermission("system:oss:query")
  无 @Log
  @NotEmpty Long[] ossIds
```

服务侧：

1. `SpringUtils.getAopProxy(this)`，为了打到 `getById` 的 `@Cacheable`。
2. 每个 id 一个 `Supplier`：缓存或 `selectVoById`；非空再 `managementView`；空则 null。
3. `ThreadUtils.virtualSubmitAll` 并行。
4. `removeAll(singleton(null))`。不存在的 id **静默消失**，不 404。

权限是 `:query`，不是 `:list`。菜单种子两颗都有。口试按方法上的注解。

#### 4. `downloadUrl`：唯一发可用地址的管理窗

```text
GET /resource/oss/{ossId}/download-url
  @SaCheckPermission("system:oss:download")
  无 @Log
  返回 R<OssService.OssAccessUrl>
```

方法体一行：`publicOssService.resolveAccessUrl(ossId)`。类注释写明这是**管理面**短时下载授权；普通业务应先校验自身业务权限，再调用内部 `OssService`。

管家 `resolveAccessUrl`：

1. `requireDownloadable`：行不存在 → `OBJECT_NOT_FOUND`；`delete_state=PENDING` → `OBJECT_DELETE_PENDING`；`requireServing(service)` 失败 → `STORAGE_NOT_SERVING`。
2. `objectStore.accessPolicy(oss)`。
3. `PUBLIC_READ`：`objectStore.publicUrl`，组装 `OssAccessUrl("PUBLIC", url, null, originalName)`。空 URL 收成 `PROVIDER_ACCESS_FAILED`。**不** presign。
4. 否则当私有：`privateDownload` + 默认 TTL（`oss.lifecycle.downloadTtl`，代码默认 2 分钟，允许区间 1–10 分钟），组装 `OssAccessUrl("PRIVATE", signed.url, signed.expiresAt, fileName)`。

`presignDownload` 是另一条内部路：公共对象调它会 `PUBLIC_PRESIGN_FORBIDDEN`。管理窗走的是 `resolveAccessUrl`，公共对象不会去签名。

架构测试锁死：Controller 源码必须含 `system:oss:download` 和 `resolveAccessUrl`，不得出现 `anonymous`、`public/{ossId}`。URL 分类只准走管家这一条。

#### 5. `remove`：盖待撕章，不砸箱子

```text
DELETE /resource/oss/{ossIds}
  @SaCheckPermission("system:oss:remove")
  @Log(title="OSS对象存储", businessType=DELETE)
  @NotEmpty Long[] ossIds
  return toAjax(ossService.deleteWithValidByIds(List.of(ossIds), true))
```

`isValid=true` 走进空 `if`：注释写「做一些业务上的校验」，方法体是空块。真正规则全在管家。

`deleteObjects`：

1. 空集合 → `false` → `toAjax` 变成 `R.fail()`。
2. id **去重排序**，`selectByIdsForUpdate`（`FOR UPDATE`）。行数对不上 → `OBJECT_NOT_FOUND`，「部分 OSS 对象不存在」。
3. 任一 id `countActiveByOssId > 0` → `OBJECT_REFERENCED`，**零** `objectStore` 调用，也**不** `deleteByIds`。
4. 全部通过：逐个 `markDeletePending(ossId, now)`。XML 把 `is_temp='Y'`、`expire_time=now`、`delete_state='PENDING'`。
5. 返回 `true` → `R.ok()`。

单测：引用阻塞时 `verifyNoInteractions(objectStore)`；手动删除只 persist pending。把 HTTP 删除说成「已经从 MinIO 删掉」，口试直接判错。

物理删除在 `cleanupExpired`：先确认仍是临时且到期、引用为 0；若还不是 PENDING 就再盖一次章并返回；**已经 PENDING** 才 `objectStore.delete` 然后 `ossMapper.deleteById`。`OssLifecycleProperties.cleanupEnabled` 默认 `false`，`cleanupDryRun` 默认 `true`。本课 HTTP **不**触发清理循环。

`BaseController.toAjax(boolean)`：true → `R.ok()`，false → `R.fail()`。异常（引用占用、缺行）不会走到 false，会冒成生命周期错误。

API-005 写变更用 POST + `@Log`。本扇是 **DELETE + `@Log`**。口试描述存量，不把规范说成已经改完。列表三扇 GET 无 `@Log`，切面不记。

## 图、表或文本图

```text
[管理员]
   │ ① GET /list 或 /listByIds/{ids}     权限 list / query
   │ ② GET /{id}/download-url            权限 download
   │ ③ DELETE /{ids}                     权限 remove + @Log
   v
SysOssController
   │
   ├─①→ ISysOssService.queryPageList / listByIds
   │         selectVoPage / getById(@Cacheable 30d)
   │         managementView: url=null + snapshot(refs)
   │         不滤 PENDING
   │
   ├─②→ OssService.resolveAccessUrl
   │         requireDownloadable
   │            PENDING? 拒绝
   │            配置不可服务? 拒绝
   │         PUBLIC_READ → 稳定 URL, expiresAt=null
   │         PRIVATE     → 签名 URL, 默认 2 分钟
   │
   └─③→ deleteWithValidByIds(ids, true)
            isValid 空块
            行锁 sys_oss
            有 sys_oss_ref 有效行? 拒绝，不碰桶
            否则 markDeletePending
            此时 MinIO 箱子还在
                 │
                 v  （不是本课 HTTP；默认清理关闭）
            cleanupExpired → objectStore.delete → deleteById
```

**文字等价物：** 管理员只有三条合法动作。第一条翻卡片，卡片上的仓库地址被涂黑，同时注明谁借走了。第二条单独申请出门条：公共货给街道地址且没有过期时刻；私有货给会过期的临时条；正在待撕或仓库停业则拒绝。第三条在借出簿为空时盖待撕章，纸箱暂时仍在冷库；真正砸箱是另一条默认关闭的清理链。四邻（配置 / 直传 / 迁移）不画进这张图。

**图的边界：** 不画直传 init/complete、不画配置 `changeStatus`、不画迁移 dry-run。不保证 `sys_oss.url` 列被物理清空——涂黑发生在 VO，表里的 url 列仍可能有旧值。不保证删除后 30 天对象缓存已经失效：`getById` 有 `@Cacheable`，删除未见 evict。不把前端厨房「listByIds 后再打 download-url 填 url」画进后端行为。

**基座冲突（要说出来）：** `10-cde-base-ddl.sql` 的 `CREATE TABLE sys_oss` 有 `is_temp` / `expire_time`，**没有** `delete_state`。实体、VO、Mapper XML、H2 集成测试 DDL 都认 `delete_state`。L-005 规定产品表权威在 10。口试讲 Java 行为用 PENDING；讲空环境建表要承认 10 还缺列。不要假装已经对齐。

## 正例、反例与边界

**正例 1：** 管理员打开文件管理页。厨房打 `GET /resource/oss/list`。表格有文件名、上传人、引用数，**没有**能点开的对象地址。要对某一行预览，再打 `GET /resource/oss/{ossId}/download-url`。

**正例 2：** 业务表保存前后调用 `OssService.reconcileReferences("sys_user", userId, oldIds, newIds)`。这不是本课 HTTP。之后管理员再 list，`references` 里能看见 `refType=sys_user`。此时 `DELETE` 同一 ossId 会 `OBJECT_REFERENCED`。

**正例 3：** 对象挂在 `PUBLIC_READ` 配置上。`downloadUrl` 返回 `accessType=PUBLIC`、`expiresAt=null`，管家不 presign。对象挂在 `PRIVATE` 上，返回 `PRIVATE` 和 Provider 给的真实过期时刻。

**正例 4：** 无引用对象 `DELETE /resource/oss/10`。库行还在，`delete_state=PENDING`。立刻再 `download-url` 失败。列表仍可能看到这张卡片。

**反例 1：** 「`SysOssController` 还有 upload / 字节 download，因为服务有 `upload(File)`。」本类方法名测试禁止 `upload`、`download`。字节协议已切走。

**反例 2：** 「列表 VO 的 url 就是下载地址。」管理两扇把 url 置 null。前端 list 还会再写成空串；那是厨房，不是本类。

**反例 3：** 「`listByIds` 缺一个 id 就整包 404。」缺的丢掉，其余照返回。

**反例 4：** 「`GET /resource/oss/{id}` 是详情。」没有这扇。详情字段散落在 list 行里；可用地址只在 download-url。

**反例 5：** 「删除 = `objectStore.delete` = 行从 `sys_oss` 消失。」HTTP 路径只 `markDeletePending`。

**反例 6：** 「被引用也能删，反正是管理员。」引用检查在 Provider 调用之前，有引用即拒绝。

**反例 7：** 「业务页面也可以打 `/resource/oss/{id}/download-url`，因为已经登录。」那颗权限是 `system:oss:download`。业务应在自己的授权之后调 `OssService`。控制器注释就是这么写的。

**反例 8：** 「`selectUrlByIds` 是本课第四扇窗。」它在 `OssService` 上且 deprecated，本 Controller 无映射。

**反例 9：** 「HTTP 前缀 `/system/oss`，因为菜单在 system。」类上是 `/resource/oss`。

**反例 10：** 「四扇都有 `@Log`。」只有 `remove`。

**反例 11：** 「`list` 和 `listByIds` 同一颗权限。」分别是 `:list` 和 `:query`。

**反例 12：** 「`wta-system` OSS 应按 layered 加 UseCase。」登记表 classic。管家是多出来的 `@Service`，不是 DAO/UseCase 分层。

**反例 13：** 「`ISysOssService.upload` 没了，所以上传不存在。」接口方法还在；**本课 HTTP** 没有它。上传窗是 `/resource/oss/uploads`。

**反例 14：** 「匿名 `GET /resource/oss/public/{ossId}` 给 CDN。」架构测试禁止这类路径。

**边界：**

- `QueryGroup` 在 BO 上没有约束字段，坏查询主要靠类型转换和分页默认值，不是一组业务校验。
- `listByIds` 并行提交，不要把返回顺序说成「一定与路径 id 顺序一致」（工作树未在本课测试里锁顺序）。
- `SYS_OSS` 缓存 30 天。删除不 evict。`list` 走分页 SQL 能看见 PENDING；`listByIds` 可能仍吃到删除前的 VO，再经 `snapshot` 补引用。不要把两扇说成同一缓存视图。
- `toAjax(false)` 只在管家对空集合返回 false 时出现；缺行/占用走异常。
- `AccessPolicy` 只剩两枚。对象上的策略来自**当前配置**的 `objectStore.accessPolicy(oss)`，不是 `sys_oss.url` 字符串长什么样。
- 下载依赖 `sys_oss.service` 这把配置 key 仍在服务。L-025 切换默认仓，旧对象未必自动换 key。
- 前端 `createOssService.listByIds` 会**再打** download-url 把 url 填回去。那是厨房补丁，后端 listByIds 仍然涂黑。格子留给资源服务课，不要说后端已经返回可用 url。

## 变式与迁移

- **变式 A：只想确认库里有没有这批 id。** 走 `listByIds`。不要用 download-url 当存在性检查——PENDING / 停业也会失败，失败原因不是「没这行」。

- **变式 B：预览或下载一个私有文件。** 管理面走 `downloadUrl`。过期后要再打一枪，不要把列表里的空 url 当永久链接。

- **变式 C：公共读桶上的对象。** 同一扇 `downloadUrl`，但 `accessType=PUBLIC`、`expiresAt=null`。不要再调内部 `presignDownload`，那扇对公共对象是禁的。

- **变式 D：页面上删一个还被头像/附件引用的文件。** 期望失败 `OBJECT_REFERENCED`。要先让业务 `reconcileReferences` 解绑，或走业务自己的保存链。管理员硬删不是绕过借出簿的后门。

- **变式 E：以为删完桶里立刻没了，去云控制台核对。** 箱子还在。查 `delete_state`（Java 路径）或等清理；清理默认关。不要把「接口 200」说成 Provider 已删。

- **变式 F：业务模块要给已授权用户看附件。** 不要复用 `system:oss:download`。在业务权限通过后调 `OssService.resolveAccessUrl` / `presignDownload`。管理窗是仓库管理员通道。

- **变式 G：产品要「列表直接出可点击链接」。** 今天会被 `managementView` 抹掉。要改的是这条投影和权限模型，不是口试假装列表已经带签名。

- **变式 H：看到菜单「文件上传」。** 那是 `system:oss:upload` → `/resource/oss/uploads`（L-027）。本课四扇打不上去。

- **迁移口诀：** 先数四扇四权限 → 列表涂黑、出门条才给地址 → 删除盖 PENDING、引用拦截、桶后删 → 两根导线一份实现 → 直传/配置/迁移是隔壁门牌。跳步会出现「把列表 url 当下载」「把 200 删除说成砸箱」「把 upload 安回这张类」。

## 常见误区

1. **「OBJ-26 还包含 init/signParts/complete。」** 那是 OBJ-27 / `SysOssUploadController`。本课只有四扇。
2. **「`/resource/oss` 和 `/system/oss` 可以混着写。」** HTTP 是 resource；菜单组件是 system/oss。
3. **「list 的 url 能下载。」** 管理投影置 null。
4. **「list 与 listByIds 权限相同。」** `:list` vs `:query`。
5. **「删除立刻少一行、桶里少一个 key。」** 行还在，状态 PENDING；桶后删。
6. **「管理员可以删被引用对象。」** 管家先数引用。
7. **「`downloadUrl` 返回文件流。」** 返回 JSON 里的 `OssAccessUrl`。
8. **「公共对象也要签名。」** `resolveAccessUrl` 对 PUBLIC_READ 走 `publicUrl`。
9. **「`OssService` 和 `ISysOssService` 是两个类。」** 一个实现类，两张接口。
10. **「本课要把 `selectUrlByIds` 讲成现行合同。」** deprecated 兼容，新代码不用，管理 HTTP 没有。
11. **「`getById` 既有 HTTP。」** 只有给 listByIds 的缓存读。
12. **「四扇都符合 API-005 的 POST 变更。」** 变更窗是 DELETE；读窗是 GET。存量如此。
13. **「`isValid=true` 会做一套完整校验。」** 空块。规则在 `deleteObjects`。
14. **「10 号基座已经有 delete_state。」** CREATE 没有；Java/XML 有。要报冲突，不要圆过去。
15. **「前端 listByIds 填了 url，所以后端也返回 url。」** 厨房自己又打了 download-url。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏。

1. 打开 `SysOssController.java`。圈类上 `@RequestMapping("/resource/oss")`。数公开映射，确认只有四扇。圈两根注入：`ISysOssService` 与 `OssService`。
2. 在 `list` / `listByIds` 圈权限串分别是 `system:oss:list` 与 `system:oss:query`。打开 `managementView`，圈 `setUrl(null)` 和 `snapshot`。
3. 在 `downloadUrl` 圈 `@GetMapping("/{ossId}/download-url")`、`system:oss:download`、`resolveAccessUrl`。确认没有 `MultipartFile`、没有 `ResponseEntity`。
4. 在 `remove` 圈 `@DeleteMapping`、`@Log DELETE`、`deleteWithValidByIds(..., true)`。打开 `deleteWithValidByIds` 看空的 `isValid` 块，再打开 `OssLifecycleManager.deleteObjects`，圈 `countActiveByOssId` 和 `markDeletePending`。确认方法体没有 `objectStore.delete`。
5. 打开 `OssProtocolCutoverUnitTest` 与 `OssAccessUrlArchitectureUnitTest`，对照「无 upload/download/匿名发现窗」。
6. 打开 `10-cde-base-ddl.sql` 的 `create table sys_oss`，再打开 `SysOssMapper.xml` 的 `markDeletePending`。把「基座缺 `delete_state`、XML 在写它」标成冲突，而不是选一边假装没有。

## 总结、词汇表与下一步

- **一扇卡片柜，四扇窗：** list / listByIds / downloadUrl / remove。矩阵 (a) 该行与磁盘一致。
- **两根导线一份实现：** 模块内 `ISysOssService` 管目录和盖章；跨模块 `OssService` 管出门条。实现类就一个。
- **目录涂黑，出门条另开。** 管理列表不给可用 URL；`OssAccessUrl` 才带 `PUBLIC`/`PRIVATE` 和过期时刻。
- **删除是待撕，不是碎纸。** 有引用拒绝；无引用 `PENDING`；砸箱在默认关闭的清理链。
- **邻居不要并进来：** config / uploads / migrations 三块门牌。菜单 upload 权限不属于本类。
- **两张表：** `sys_oss` 卡片，`sys_oss_ref` 借出簿。引用不当权限。

词汇表：`SysOssController` / `ISysOssService` / `OssService` / `SysOssServiceImpl` / `OssLifecycleManager` / `SysOss` / `SysOssVo` / `managementView` / `OssAccessUrl` / `resolveAccessUrl` / `sys_oss` / `sys_oss_ref` / `PENDING` / `markDeletePending` / `AccessPolicy` / `system:oss:list|query|download|remove` / classic。

下一步：L-027 才把直传 `init/signParts/parts/complete/abort` 和失败回滚讲完。L-028 才是迁移批。L-029 才把厨房 `resources.oss` 和浏览器直传客户端对上。L-025 才是换默认仓库的副作用。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课类在 `wta-system` 的 `controller/system` | `SysOssController.java` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` = classic；不按 layered 口述 | 登记表 classic 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `sys_oss` / `sys_oss_ref`；菜单 `system:oss:list/query/upload/download/remove`；**10 的 CREATE 无 `delete_state`** | DDL 约 304、437 行；DML 文件管理菜单段 | 2026-09-16 |
| S-L026-01 | `SysOssController.java` | 四扇映射、两根注入、权限串、仅 remove 有 `@Log`、downloadUrl 走 `resolveAccessUrl` | 类与四个方法 | 2026-09-16 |
| S-L026-02 | `ISysOssService.java`；`SysOssServiceImpl.java` | 列表涂黑、listByIds 缓存+丢 null、delete 空 isValid、同实现也是 `OssService`；`upload(File)` 无本课 HTTP | 接口五方法；`managementView`；`deleteWithValidByIds` | 2026-09-16 |
| S-L026-03 | `OssService.java` | `OssAccessUrl` / `OssDownloadUrl` / `OssReference`；`resolveAccessUrl`；deprecated `selectUrlByIds`/`selectByIds` | `wta-api` 接口与 record | 2026-09-16 |
| S-L026-04 | `OssLifecycleManager.java`；`SysOssMapper.xml` | 下载分类；PENDING 拒绝；删除只盖章；引用拦截；`FOR UPDATE` | `resolveAccessUrl`；`deleteObjects`；`markDeletePending` | 2026-09-16 |
| S-L026-05 | `SysOss.java`；`SysOssVo.java`；`SysOssBo.java`；`SysOssRef.java` | 表字段、管理 VO 引用摘要、查询条件、引用不当权限 | 实体/VO/BO | 2026-09-16 |
| S-L026-06 | `OssProtocolCutoverUnitTest.java`；`OssAccessUrlArchitectureUnitTest.java`；`OssAccessUrlContractUnitTest.java`；`OssReleaseContractionUnitTest.java` | 无字节协议、无匿名发现、download-url 合同、四扇均有权限注解、门牌 `/resource/oss` | `wta-admin/src/test/java/org/namewta/test/oss/**` | 2026-09-16 |
| S-L026-07 | `OssLifecycleManagerUnitTest.java`；`OssAccessUrlServiceUnitTest.java`；`OssLifecycleProperties.java`；`CacheNames.java` | 手动删除只 pending；列表永不返回访问 URL；默认下载 TTL 2 分钟；`SYS_OSS` 30 天 | 对应测试方法与常量 | 2026-09-16 |
| S-L026-08 | `SysOssUploadController.java`；`SysOssConfigController.java`；`SysOssMigrationController.java`；`frontend/packages/domains/system/src/{oss/index.ts,resource-service.ts}`（仅边界） | 三块隔壁门牌；前端锚点与厨房会再打 download-url，本课不认厨房格子 | `/uploads` `/config` `/migrations`；`createOssService` | 2026-09-16 |
