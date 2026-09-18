---
lesson_id: L-037
objective_ids: [OBJ-37]
claimed_cells:
  - A:PersonMaterialAdminController.*
  - A:PersonMaterialSelfController.*
  - A:MaterialTagController.*
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: three-windows-and-owners
    minutes: 9
  - segment: attach-detach-causal
    minutes: 10
  - segment: tag-tree-and-visuals
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-015, S-L037-01, S-L037-02, S-L037-03, S-L037-04, S-L037-05, S-L037-06, S-L037-07, S-L037-08]
---

# Lesson 037：宏观三扇材料窗——口述个人 attach/detach 与 `MaterialTagController`

## 学完你能做什么

打开 `backend/wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/controller/` 里**三份** Java，你能**口述个人材料怎么挂、怎么摘、标签树怎么改**，而不是把「材料」说成 OSS 上传，也不是把档案审核窗 `PersonAdminController` 的 access-url 说成本课门牌。

口试名单就是矩阵 (a) 三行，方法名以**磁盘**为准：

1. **`A:PersonMaterialAdminController.list, accessUrl`**：同一门牌 `GET /profile/person/materials/{ownerType}/{ownerId}` 与 `GET .../{materialRefId}/access-url`。类名带 Admin，权限却 **OR** 开了申请人 `profile:person:material`。只读。没有 `@Log`。
2. **`A:PersonMaterialSelfController.attach, detach`**：同一门牌上的 **POST**。`attach` 挂一张已入库的 OSS 对象；`detach` 只改引用状态，**不**删对象、**不**走 `DELETE`。
3. **`A:MaterialTagController.tree, update, status, archive`**：另一块门牌 `/profile/material-tags`。矩阵这一行写成四扇；磁盘上还有第五扇 **`create`**。口试按磁盘，不要把漏行背成「不能新增节点」。

2026-09-16 工作树先钉死**三句话**，口试先数门牌，再数方法：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 一份 `PersonMaterialController` | **没有这个 Java 类。** 前端资源标签写成 `PersonMaterialController`，后端是 Admin + Self **两份**类，共用 `/profile/person/materials` |
| 管理端也能 POST attach | **Self 才映射 POST。** 管理员要挂材料，走的是 **SOURCE** owner + `profile:person:override`，不是在 Admin 类上多一扇窗 |
| detach = 删文件 / `DELETE` | **POST `.../detach`。** 行变成 `DETACHED`；OSS 箱子还在；`@Log` 的 `businessType` 是 `UPDATE` |
| 标签树在企业模块也有一份 Controller | **没有。** 全仓库只有 person 包里这一份 `MaterialTagController`；`scope=ENTERPRISE/COMMON` 也打这里 |
| `/profile/person/archive/.../access-url` 是本课 | **那是 L-034** `PersonAdminController.reviewMaterial` / `material` |

`wta-profile` 在登记表是 **layered**：`Controller -> UseCase -> Service -> DAO -> Mapper -> XML`。材料这条河还多三根电线，仍不是 classic：`ProfileMaterialAccessPolicy`、`ProfileMaterialOwnerContributor`、跨模块 `OssService`。不要口述成 `Controller → ServiceImpl → Mapper`。

本课**不宣称**你会拆个人档案审核/绑定（L-034）、自助申请 current/submit（L-035）、换绑（L-036）、核身回调（L-038）、厨房 `createProfileService` 与 web-domain（L-039），或把企业材料 Admin/Self 再讲一遍（L-043）。今天只认：**挂钩分类牌怎么改，草稿板上怎么挂/摘一张已经进仓库的照片。**

## 先把宏观地图放在桌上

L-003 已经把 `wta-profile` 钉在 layered 列。L-026 把仓库门口的卡片柜讲成「列表涂黑、出门条另开、删除盖 PENDING」。本课站在 **Profile 自己的挂钩** 这一头：照片必须先经 OSS 直传进仓（L-027 / L-029），这里只把 `ossId` **别到**某个人的某次草稿/快照上，并盖一枚稳定标签码。

2026-09-16 工作树：个人材料 HTTP 是**两块门牌、三份 Controller**：

```text
已登录的人（浏览器，Admin-Token）
        │
        ├─ /profile/person/materials          ← 本课材料引用
        │     GET  /{ownerType}/{ownerId}                         PersonMaterialAdminController.list
        │     GET  /{ownerType}/{ownerId}/{materialRefId}/access-url
        │                                                         PersonMaterialAdminController.accessUrl
        │     POST /{ownerType}/{ownerId}                         PersonMaterialSelfController.attach
        │     POST /{ownerType}/{ownerId}/{materialRefId}/detach  PersonMaterialSelfController.detach
        │
        ├─ /profile/material-tags             ← 本课分类牌
        │     GET  /tree
        │     POST /                                          create   ← 矩阵漏写，磁盘有
        │     POST /{materialNodeId}
        │     POST /{materialNodeId}/status
        │     POST /{materialNodeId}/archive
        │
        ├─ /profile/person/archive            PersonAdminController     L-034
        │     GET  /application/{id}/material/{ref}/access-url
        │     GET  /{profileId}/material/{ref}/access-url
        │
        └─ /profile/enterprise/materials      Enterprise*Material*      L-043
```

往下走不要跳层：

```text
三份 Controller
    └─ ProfileMaterialUseCase / ProfileMaterialUseCaseImpl   @DSTransactional
          └─ ProfileMaterialService  （concrete；也实现 IProfileMaterialService = ProfileMaterialPort）
                ├─ ProfileMaterialDao -> ProfileMaterialMapper.xml
                │      profile_material_node / profile_material_ref / profile_material_requirement
                ├─ OssService  （wta-api；objectMetadata / reconcileReferences / resolveAccessUrl）
                ├─ ProfileMaterialAccessPolicy  ← adapter/security/SaTokenProfileMaterialAccessPolicy
                └─ Map<ProfileType, ProfileMaterialOwnerContributor>
                      PERSON → PersonProfileMaterialOwnerContributor
                            → PersonProfileApiUseCase.lockMaterialOwner / isWorkingEditable
                            → PersonApplicationMapper 锁申请/提交/来源/版本行
```

`ProfileMaterialExceptionHandler` 是 `@RestControllerAdvice`，包范围同时罩 person **和** enterprise 的 admin/self。业务失败走 `R.fail(category)`：**HTTP 仍可能是 200**，`msg` 才是 `MATERIAL_ACCESS_DENIED` 这类码。E2E 把「先鉴权、后签发下载地址」钉死：拒绝时 **不再** 调 `resolveAccessUrl` / `presignDownload`。

## 核心概念与机制

### 直觉讲解

把这件事想成**衣帽间挂钩**，不是仓库碎纸机。

- **分类牌（material tag / node）**：墙上的挂钩分区。「居民身份证人像面」「国徽面」是预先钉好的牌子。牌子可以改名字、停用、归档，但系统必传牌不能拆。
- **挂钩上的夹子（material ref）**：把一张**已经进仓库**的照片夹到某个挂钩上。夹子记下：哪张 OSS、哪个标签码、文件名、大小、MIME。摘夹子 = 夹子变成「已摘」，照片还在仓库货架上。
- **四块板（owner type）**：
  - `WORKING`：你正在改的草稿板。还能挂、还能摘。
  - `SUBMISSION`：交上去那一瞬复印并塑封的板。
  - `VERSION`：档案柜里的正式版本，也是塑封件。
  - `SOURCE`：管理员直接塞进柜子的塑封件（自己的 OSS 对象，一挂就不可变）。

**类比失效边界：** 衣帽间类比**不**覆盖「照片怎么进仓库」。进仓是 OSS 直传。本课只认夹子。类比也**不**等于「摘夹子 = 仓库删箱子」——`detach` 明确调用 `ossService.reconcileReferences(..., Set.of(ossId), Set.of())`，是从借出簿去掉这一行引用，不是 `objectStore.delete`。类比还不等于「一块板上同一张照片可以夹两个挂钩」——表上生成列 `active_owner_oss_key` 保证：**同一 owner 上，同一 `ossId` 同时只能有一条 ATTACHED**。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 材料目录节点 | material node | 表 `profile_material_node`；领域 `MaterialNode`；对外 `MaterialNodeView` |
| 节点类型 | `CATEGORY` / `TAG` | 枚举 `MaterialNodeType` |
| 适用范围 | `PERSON` / `ENTERPRISE` / `COMMON` | 枚举 `MaterialScope`；列名仍叫 `profile_type` |
| 材料引用 | material reference | 表 `profile_material_ref`；`MaterialReference` / `MaterialReferenceView` |
| 归属钥匙 | owner key | `MaterialOwnerKey(profileType, ownerType, ownerId)`；`ownerId` 必须为正 |
| 归属种类 | `WORKING` / `SUBMISSION` / `SOURCE` / `VERSION` | 枚举 `MaterialOwnerType` |
| 挂接 | attach | `POST .../{ownerType}/{ownerId}`；`MaterialAttachCommand(owner, ossId, materialNodeId)` |
| 解除挂接 | detach | `POST .../{materialRefId}/detach`；状态 `ATTACHED` → `DETACHED` |
| 出门条 | access URL | HTTP 返回 `PersonProfileAccessUrl`，内部先拿 `OssService.OssAccessUrl` 再投影，避免把 system 类型漏到 Profile HTTP |
| 不可变证据 | immutable evidence | 列 `immutable_flag`；SOURCE 挂上即 `Y`；快照复制也是 `Y` |
| 乐观锁 | expected version | 标签变更体带 `expectedVersion`；改 0 行 → `MATERIAL_VERSION_CONFLICT` |
| 共享端口 | `ProfileMaterialPort` | `wta-api`；person/enterprise 各自实现，互不读对方表 |
| 所有者贡献者 | `ProfileMaterialOwnerContributor` | person 只锁 PERSON 行；企业 owner 塞进来会 `IllegalArgumentException` |

Controller 公开 HTTP 以磁盘为准（含矩阵漏写的 `create`）：

| Java 方法 | HTTP | 权限 | `@Log` | 返回 |
| --- | --- | --- | --- | --- |
| `PersonMaterialAdminController.list` | `GET /profile/person/materials/{ownerType}/{ownerId}` | `profile:person:material` \| `query` \| `review` \| `manage` \| `override`（OR） | 无 | `R<List<MaterialReferenceView>>` |
| `PersonMaterialAdminController.accessUrl` | `GET .../{materialRefId}/access-url` | 同上 | 无 | `R<PersonProfileAccessUrl>` |
| `PersonMaterialSelfController.attach` | `POST /profile/person/materials/{ownerType}/{ownerId}` | `profile:person:material` | INSERT，**不**记请求/响应体 | `R<MaterialReferenceView>` |
| `PersonMaterialSelfController.detach` | `POST .../{materialRefId}/detach` | `profile:person:material` | UPDATE，**不**记体 | `R<Void>` |
| `MaterialTagController.tree` | `GET /profile/material-tags/tree?scope=&includeDisabled=` | `profile:material-tag:query` \| `person:material` \| `enterprise:material` \| 两边 `override`（OR） | 无 | `R<List<MaterialNodeView>>` |
| `MaterialTagController.create` | `POST /profile/material-tags` | `profile:material-tag:manage` | INSERT，不记体 | `R<MaterialNodeView>` |
| `MaterialTagController.update` | `POST /profile/material-tags/{materialNodeId}` | `:manage` | UPDATE，不记体 | `R<MaterialNodeView>` |
| `MaterialTagController.status` | `POST .../{materialNodeId}/status` | `:manage` | UPDATE，不记体 | `R<Void>` |
| `MaterialTagController.archive` | `POST .../{materialNodeId}/archive` | `:manage` | UPDATE，不记体 | `R<Void>` |

合同测试 `ProfileMaterialHttpContractTest` 锁死：**三份类都没有 `@DeleteMapping`**；每个 `@PostMapping` 都必须有 `@Log` 且 `isSaveRequestData=false`、`isSaveResponseData=false`。材料是证件照片，日志里不许把请求体抄走。

两份材料 Controller 的 `owner(...)` **写死** `ProfileType.PERSON`。路径上的 `ownerType` 只是 `WORKING/SUBMISSION/SOURCE/VERSION`，不能靠 URL 把档案类型改成企业。

UseCase 上还有 **`validateRequired`** 与 **`snapshotImmutable`**，**没有**本课 HTTP。提交申请、发布版本会在别处调用它们。口试可以说「挂钩服务会复印塑封」，不要说「有一扇 snapshot 窗」。

`ProfileMaterialUseCase.accessUrlView` 在接口上是 **default**，旧适配器直接扔 `UnsupportedOperationException("旧适配器不支持文件访问地址入口")`。生产 `ProfileMaterialUseCaseImpl` 覆盖它，转 `PersonProfileAccessUrl`。

### 机制/因果链

**1. 先锁人，再动夹子。**

`list` / `accessUrl` / `attach` / `detach` 都先 `lockOwner`。PERSON 的贡献者：

- `WORKING`：`SELECT applicant_user_id FROM profile_person_application ... FOR UPDATE`
- `SUBMISSION`：提交表 join 申请表，同样 `FOR UPDATE`，带回申请人
- `SOURCE` / `VERSION`：锁 `profile_person_source` / `profile_person_version`，**申请人字段为 null**

找不到行 → `MATERIAL_OWNER_NOT_FOUND`。UseCase 每个方法都标了 `@DSTransactional`，读路径也是事务：因为后面有 `FOR UPDATE`。测试 `ownerLockingReadOperationsKeepTransactionBoundaries` 点名 `list` / `accessUrlView` / `validateRequired`。

**2. 权限是第二道门，注解只是第一道。**

HTTP 注解过了，还要 `SaTokenProfileMaterialAccessPolicy`：

| 动作 | 策略方法 | 谁过 |
| --- | --- | --- |
| 挂 WORKING | `requireAttach` | 当前用户 == 申请人，且有 `profile:person:material` |
| 挂 SOURCE | `requireAttach` | 有 `profile:person:override`（管理员用**自己的** OSS） |
| 摘 | `requireWrite` | **只**申请人 + `:material`。override **不能**摘 |
| 列表 / 出门条 | `requireRead` | 管理四权（query/review/manage/override）**或**申请人 + `:material` |
| 改分类牌 | `requireCatalogManage` | `profile:material-tag:manage` |
| 看停用节点 | `requireCatalogRead` | 仅当 `includeDisabled=true`；要 `:query` 或 `:manage` |

所以：**类名叫 Admin 的 GET，申请人拿着 `:material` 也能打。** 前端 `person.materials.list('VERSION', ...)` 打的就是这扇 GET。**管理员不能靠 override 去 POST detach。** 管理员要补材料，走 SOURCE attach，夹子一挂 `immutableEvidence=true`。

WORKING 还要 `isWorkingEditable`：申请状态必须是 `DRAFT` / `BACK` / `CANCEL`。已提交的草稿板 → `MATERIAL_OWNER_READ_ONLY`。SOURCE 挂接**不**查这扇门。

**3. attach：校验全部通过才插行，才改借出簿。**

顺序（失败就停，测试要求 **零** `insertReference`、**零** `reconcileReferences`）：

1. owner 只能是 `WORKING` 或 `SOURCE`，否则 `IMMUTABLE_MATERIAL_OWNER`
2. 权限 +（WORKING 时）可编辑
3. 节点必须是启用中的 `TAG`，且 scope 是 `COMMON` 或与档案类型同名，否则 `MATERIAL_TAG_UNAVAILABLE` / `MATERIAL_TAG_NOT_APPLICABLE`
4. 该 owner 已 ATTACHED 条数 `< 10`，否则 `MATERIAL_COUNT_LIMIT`
5. `ossService.objectMetadata(ossId)`；`uploaderUserId` 必须等于策略返回的当前用户，否则 `MATERIAL_OSS_OWNER_MISMATCH`
6. 大小 `(0, 10MiB]`；扩展名与库里 suffix 一致；MIME 只能是 jpeg/png/pdf 的白名单，否则 `MATERIAL_FILE_TOO_LARGE` / `MATERIAL_FILE_TYPE_INVALID`
7. 插入引用：WORKING 的 `immutable_flag=N`，SOURCE 的 `Y`；状态 `ATTACHED`
8. 唯一键冲突（同一 owner 上同一 oss 仍 ATTACHED）→ `MATERIAL_ALREADY_ATTACHED`
9. `ossService.reconcileReferences("profile_material_ref", refId, Set.of(), Set.of(ossId))` —— 告诉仓库：这只箱子现在被这只夹子借走

请求体只有 `{ ossId, materialNodeId }`。没有文件字节，没有 multipart。

**4. detach：只摘可变的草稿夹子。**

1. `requireWrite` + owner 必须是 WORKING，否则 `IMMUTABLE_MATERIAL`
2. 可编辑
3. `lockReference ... FOR UPDATE`；夹子必须属于这把 owner 钥匙，否则对外仍说 `MATERIAL_NOT_FOUND`（不泄露别人的 refId）
4. `immutableEvidence` → `IMMUTABLE_MATERIAL`
5. 已经 DETACHED → **直接 return**（幂等，不打 OSS）
6. SQL：`status='DETACHED'`，写下 `detached_time`，且 **只更新** `ATTACHED + immutable_flag='N'` 的行
7. `reconcileReferences(..., Set.of(ossId), Set.of())` —— 从借出簿去掉，**不是**删对象

E2E：WORKING 摘掉之后，SUBMISSION 那条塑封复制仍是 `ATTACHED`；同一 `oss_id` 会留下 **两行** 引用。摘草稿 ≠ 撕掉已交卷的复印件。

**5. list / accessUrl。**

`list` 按 owner 拉全部未删引用，**含 DETACHED 历史**，按挂接时间排序。`accessUrl` 再锁夹子、对 owner；WORKING 上已摘的夹子 → `MATERIAL_NOT_ATTACHED`。通过后才 `ossService.resolveAccessUrl(ossId)`，再投影成 `PersonProfileAccessUrl(accessType, url, expiresAt, fileName)`。出门条的 PUBLIC/PRIVATE 规则是 OSS 课的；本课只保证：**没过 Profile 权限，根本不向仓库要地址。**

**6. 标签树：三层受限树，不是随便文件夹。**

基座 `chk_profile_material_node_shape` 与 Java `shape()` 一起守：

- 根：`parentId=0`、`CATEGORY`、`depth=1`、无 tag 码、非系统必传。种子已经有三棵根：个人材料 / 企业材料 / 通用材料。**HTTP create 不能再种根**（`MATERIAL_ROOT_SCOPE_FIXED`）
- 二级分类：挂在根下，`CATEGORY`、`depth=2`、无 tag 码
- 标签：挂在分类下，`TAG`、`depth` 2 或 3、编码匹配 `[A-Z][A-Z0-9_]{1,63}`
- 父必须是启用中的 CATEGORY，且 scope 一致
- 不能改 `nodeType`、不能改 `scope`、TAG 不能改 `materialTagCode`
- `systemRequired=true` 只能保护已有系统标签，**不能**用 create 新建系统牌，也**不能**把系统牌改成非系统
- 停用：系统牌或 depth=1 → `SYSTEM_MATERIAL_TAG_PROTECTED`；分类还有孩子 → `MATERIAL_CATEGORY_NOT_EMPTY`
- 归档：同样保护根/系统牌；有孩子 **或任何引用（含 DETACHED）** → `MATERIAL_NODE_IN_USE`；成功则 `del_flag=1` 且 `status=1`，不是物理 DELETE
- `tree(scope, includeDisabled)`：SQL 会把 `COMMON` 一并带上（查 PERSON 时也能看到通用牌）；默认只返回 `status='0'`

`update` / `status` / `archive` 都 `lockNode ... FOR UPDATE`，用 `expectedVersion` 做乐观锁。

**7. 本课 HTTP 碰不到、但挂钩服务会做的两件事。**

- `validateRequired`：按证件类型 + 条件读 `profile_material_requirement`，数 ATTACHED 标签。大陆身份证种子要求人像面 **和** 国徽面各至少 1。缺哪枚就 `MISSING_REQUIRED_MATERIAL:TAG_CODE`
- `snapshotImmutable`：只允许 WORKING→SUBMISSION、SUBMISSION→VERSION、SOURCE→VERSION，且贡献者承认这对关系；把源上仍 ATTACHED 的夹子 **复制** 成目标上的不可变夹子，并为每个新 ref 再 reconcile。这是交卷/发布，不是用户点的第四扇窗

### 图、表或文本图

**图 1 标题 / caption：** 宏观三扇窗与四块板。一张草稿板上的可变夹子，如何变成提交板上的塑封件；分类牌是另一块门。

**alt：** 左列为浏览器四个 HTTP，中列为 UseCase/Service，右列为四张 owner 板和 OSS 仓库。

```text
  浏览器                              Profile 挂钩房                         仓库
  --------                            --------------                         ----
  GET  /materials/{type}/{id}  ──►  lockOwner + requireRead  ──►  列出夹子
  GET  /.../access-url         ──►  同上，再 resolveAccessUrl ──►  短时出门条
  POST /materials/{type}/{id}  ──►  attach WORKING|SOURCE
        {ossId, materialNodeId}         │
                                        ├─ 白名单/大小/本人上传
                                        ├─ insert ATTACHED 夹子
                                        └─ reconcile +ossId ─────────────► 借出簿 +1
  POST /.../{refId}/detach     ──►  仅 WORKING 可变夹子
                                        ├─ status=DETACHED
                                        └─ reconcile -ossId ─────────────► 借出簿 -1
                                                                        箱子仍在货架

  GET  /material-tags/tree     ──►  分类牌（含 COMMON）
  POST /material-tags[...]     ──►  manage + 乐观锁 + 树形约束

  WORKING 草稿板  ==snapshot==►  SUBMISSION 塑封  ==snapshot==►  VERSION 档案柜
  SOURCE 管理员塑封  ========================================►  VERSION
```

**文字等价物：** 调用方从不把文件字节打进 `/profile/person/materials`。`GET list` 和 `GET access-url` 共用 Admin 类、只读、权限 OR。`POST attach` / `POST detach` 共用 Self 类；挂只能打在 WORKING 或 SOURCE 板上，摘只能打在 WORKING 且非塑封夹子上。挂成功会在 `profile_material_ref` 插入 ATTACHED 行，并通知 OSS 借出簿增加该 `ossId`；摘只把该行标 DETACHED 并从借出簿去掉，仓库对象仍在。分类牌是另一块 URL `/profile/material-tags`。WORKING 板上的夹子可以在交卷时被复印到 SUBMISSION，再复印到 VERSION；管理员 SOURCE 也可以直接复印到 VERSION。复印件默认不可变。本图不画企业材料门牌，不画档案审核窗，不画直传 init/complete。

**图的边界：** 不保证 `generated/openapi.ts` 与 `openapi/current.json` 同步——2026-09-16 工作树里 **生成文件已有** `/profile/person/materials` 四条路径，`current.json` **搜不到** `/profile/`。厨房 `createPersonMaterialService` 自己拼 URL。不要把 OpenAPI 快照说成这份 HTTP 的权威源。不保证业务失败是 HTTP 4xx：Handler 返回 `R.fail`，E2E 看到的是 200 + `msg`。

**图 2 标题 / caption：** 种子个人身份证明子树（只画 PERSON 相关，企业牌留给 L-043）。

```text
个人材料 (CATEGORY, depth=1, PERSON)
  └─ 个人身份证明 (CATEGORY, depth=2)
        ├─ PERSON_ID_CARD_PORTRAIT     人像面   systemRequired=Y
        ├─ PERSON_ID_CARD_EMBLEM       国徽面   systemRequired=Y
        ├─ PERSON_IDENTITY_FRONT       身份证明正面
        ├─ PERSON_IDENTITY_BACK        身份证明背面
        └─ PERSON_PASSPORT_DATA_PAGE   护照资料页
```

**文字等价物：** 种子在 `50-cde-base-dml.sql` 插入三棵根和这五枚个人 TAG。大陆身份证必传规则绑的是人像面 + 国徽面。这些系统牌停用/归档都会被 Java 拒绝。口试说到「身份证两面」时，指的是两枚 **不同 tag code**，不是同一张 OSS 夹两次——同一 `ossId` 在同一 owner 上不能同时 ATTACHED 两次。

### 正例、反例与边界

**正例 1：** 申请人草稿 `DRAFT`，已直传一张自己的 `front.jpg`（jpeg，≤10MiB）。`POST /profile/person/materials/WORKING/{applicationId}`，body `{ ossId, materialNodeId: 人像面 }`。得到 `MaterialReferenceView`，`attached=true`，`immutableEvidence=false`。仓库借出簿多一条 `refType` 意义上的 `profile_material_ref`。

**正例 2：** 同一草稿再挂国徽面（**另一** ossId）。`validateRequired(..., "CN_RESIDENT_ID", ALWAYS)` 才能过。E2E 只挂人像面时，缺的是 `MISSING_REQUIRED_MATERIAL:PERSON_ID_CARD_EMBLEM`。

**正例 3：** 申请人 `POST .../{workingRefId}/detach`。WORKING 行变 DETACHED；若已经 snapshot 到 SUBMISSION，复印件仍 ATTACHED。再 `GET .../SUBMISSION/{submissionId}/{immutableRefId}/access-url` 仍能拿到 `PRIVATE` 出门条。

**正例 4：** 管理员用自己上传的对象 `POST .../SOURCE/{sourceId}`，且持有 `profile:person:override`。夹子 `immutableEvidence=true`。不检查申请是否可编辑。

**正例 5：** `GET /profile/material-tags/tree?scope=PERSON&includeDisabled=false`。返回个人树 + 通用树的启用节点。持有 `:material` 即可（HTTP OR）。把 `includeDisabled=true` 时，Service 还要 `material-tag:query|manage`。

**正例 6：** 已摘的 WORKING 夹子再 detach 一次：Service 发现 `!attached()`，直接返回，不碰 OSS。

**反例 1：** 「Admin 类也能 attach。」磁盘 Admin 只有 GET。POST 在 Self 类。

**反例 2：** 「detach 是 `DELETE /materials/{id}`，文件从 MinIO 消失。」合同测试禁止 DeleteMapping。对象仍在。L-026 的 PENDING 删除是仓库管理员通道，不是这扇窗。

**反例 3：** 「列表 VO 的 url 就能预览。」本课 list 返回的是引用视图（文件名、tag、ossId），可用地址只在 access-url。不要把 L-026 管理列表涂黑和这里混成一句。

**反例 4：** 「`/profile/person/archive/{profileId}/material/{ref}/access-url` 是 Admin 材料窗。」那是 L-034，权限还写成单独的 `profile:person:material`，门牌是 archive。

**反例 5：** 「企业材料 POST `/profile/person/materials/WORKING/...` 把 profileType 改成 ENTERPRISE。」person Controller 写死 PERSON。企业是 `/profile/enterprise/materials`（L-043），另一份 UseCase。

**反例 6：** 「管理员 override 可以帮申请人摘 WORKING 夹子。」`requireWrite` 只要申请人。override 只能 SOURCE attach。

**反例 7：** 「同一张证件照片夹到人像面和国徽面。」`uk_profile_material_ref_active_owner_oss`：同一 owner + 同一 oss 在 ATTACHED 时不能两行。Schema 测试先插入人像面，再插同一 oss 到国徽面会 SQLException；先 DETACHED 才能再挂。

**反例 8：** 「挂别人的 OSS。」`MATERIAL_OSS_OWNER_MISMATCH`。元数据里的上传者必须是当前登录用户。

**反例 9：** 「提交后还能在 WORKING 上补图。」`countEditableMaterialWorkingOwner` 只要 `DRAFT|BACK|CANCEL`。否则 `MATERIAL_OWNER_READ_ONLY`。

**反例 10：** 「归档标签 = DELETE 节点，历史夹子不管。」`countReferences` 不滤状态；DETACHED 历史也占着牌。`archive` 的 HTTP 是 POST，SQL 改 `del_flag`。

**反例 11：** 「create 可以再种一棵 PERSON 根，或新建 systemRequired 标签。」`MATERIAL_ROOT_SCOPE_FIXED`；新建带 `systemRequired` → `SYSTEM_MATERIAL_TAG_PROTECTED`。

**反例 12：** 「改标签编码、改 PERSON 为 COMMON。」码不可变；scope 不可变。

**反例 13：** 「业务失败一定 403/404。」材料业务码走 `ProfileMaterialException` → `R.fail(message)`。E2E 对拒绝下载的断言是 **200 + `MATERIAL_ACCESS_DENIED`**。

**反例 14：** 「矩阵没写 create，所以没有新增。」磁盘有 `POST /profile/material-tags`。口试按磁盘。

**反例 15：** 「前端资源名 `PersonMaterialController` 对应一个后端类。」只是 domain 资源标签。

**边界：**

- 文件白名单只认 `.jpg` / `.jpeg` / `.png` / `.pdf` 与对应 MIME。gif/heic/zip 走 `MATERIAL_FILE_TYPE_INVALID`。扩展名必须与 OSS 存的 suffix 一致（含点、小写）。
- 每块板最多 10 条 **当前 ATTACHED**，不是历史上曾经挂过的总数。
- `list` 含 DETACHED；`countAttached` / 必传校验只数 ATTACHED。
- `accessUrl` 对非 WORKING 的未挂状态没有同样的 `MATERIAL_NOT_ATTACHED` 短路；正常路径上不可变板也不该被 detach。
- 标签 `material_tag_code` 全表唯一，不是「每棵树内唯一」。
- `IProfileMaterialService` 仍在生产目录且未被 `@Deprecated`；Controller/UseCase **注入的是** `ProfileMaterialUseCase` / 具体 `ProfileMaterialService`。测试夹具 `src/test/.../ProfileMaterialServiceImpl` 才同时实现 UseCase，不得回流生产。
- `PersonMaterialSelfController` 的 `ownerId` **没有** `@Positive`；企业 Self 有。非法 id 会在 `MaterialOwnerKey` compact 或后续 `requirePositive` 爆。不要把两边校验说成同一份。
- 厨房会 `encodeURIComponent` 路径段；后端 path 是枚举 + Long。前端测试里 `'profile/1'` 只锁编码，不是说 ownerId 真是斜杠字符串。

## 变式与迁移

- **变式 A：申请人要预览自己刚挂的人像面。** `GET .../WORKING/{appId}/{refId}/access-url`。不要打 L-026 的 `/resource/oss/{ossId}/download-url`（那是 `system:oss:download` 仓库管理员通道）。
- **变式 B：审核员要看提交件。** 可以打本课 `GET .../SUBMISSION/{submissionId}/.../access-url`（持 review/query/manage/override 即可过 `requireRead`），也可以走 L-034 的 archive 审核窗。两扇门牌，一种出门条投影。不要说只有一扇。
- **变式 C：补一张拍错的照片。** 先 detach 旧 WORKING 夹子（同一 oss 才能再次 ATTACHED），再 attach 新 ossId。不要指望「同一 oss 改挂到另一 tag」在 ATTACHED 时成功。
- **变式 D：管理员发现申请人没图，要直接建档。** SOURCE attach + 日后 `snapshotImmutable(SOURCE → VERSION)`。不要在 WORKING 上用 override 硬挂——策略不允许。
- **变式 E：只要分类牌给上传页下拉。** `GET /tree?scope=PERSON`。不要 `includeDisabled=true`，除非持有标签查询权；停用牌本来就不能 attach（`MATERIAL_TAG_UNAVAILABLE`）。
- **变式 F：产品要「删标签」。** 今天只有 archive（软删）且引用占用即拒绝。历史 DETACHED 也算占用。要先让引用自然老化或改产品规则，不能口试假装 POST archive 是物理删除。
- **变式 G：企业营业执照。** URL 换成 `/profile/enterprise/materials`，权限前缀换成 `profile:enterprise:*`，标签 scope 走 ENTERPRISE/COMMON。分类牌仍打 person 模块这棵 `MaterialTagController`。细节留给 L-043。
- **变式 H：对照 OSS 卡片柜。** 仓库 list 涂黑、download-url、PENDING 删除是 L-026。本课 list 是夹子清单；access-url 是夹子授权之后才向 `OssService` 借出门条；detach 是还夹子不是盖 PENDING。
- **迁移口诀：** 先数两块门牌三份类 → GET 只读在 Admin、POST 挂摘在 Self → 四块板只有 WORKING 可变、SOURCE 一挂即塑封 → 摘夹子不砸箱 → 分类牌受限树 + 乐观锁 + 无 DELETE → 必传与塑封复印没有本课 HTTP。跳步会出现「把 archive 审核窗当材料窗」「把 detach 说成删 MinIO」「把 Admin 类说成不能给申请人 list」。

## 常见误区

1. **「OBJ-37 还包含 `createProfileService`。」** 那是 OBJ-39。本课可以拿厨房 URL 当对照，格子不认工厂。
2. **「一份 Controller 管 list+attach。」** 两份类，按 HTTP 方法切开，门牌相同。
3. **「材料上传走这扇 POST。」** body 只有 ossId + materialNodeId。字节在 OSS 直传。
4. **「detach / archive / 删标签 都是 DELETE。」** 全是 POST；合同测试禁止 DeleteMapping。
5. **「Admin 就是管理员专用。」** GET 的 OR 含 `:material`；策略 `requireRead` 也给申请人。
6. **「override 万能。」** 能 SOURCE attach 和读；不能 `requireWrite` detach；不能靠 HTTP 注解绕过 Service。
7. **「树接口 includeDisabled 只看 HTTP 注解。」** 开停用节点时 Service 另要 `material-tag:query|manage`。
8. **「企业也有 MaterialTagController。」** 全仓一份，住在 person 包。
9. **「`PersonMaterialController` 在 Java 里。」** 只有前端资源字符串。
10. **「失败就是 403。」** 材料业务码经常是 200 + `R.fail`。鉴权注解失败才是 Sa-Token 那条路。
11. **「快照窗在本课。」** `snapshotImmutable` 无 HTTP。
12. **「10 号基座没有材料表。」** 有 `profile_material_node` / `requirement` / `ref`，还有树形 CHECK 和生成唯一列。
13. **「OpenAPI current.json 就是权威。」** 快照缺 `/profile/`；生成文件与厨房硬编码才看得到这些路径。把冲突说出来，不要选边假装没有。
14. **「挂接会改 `sys_oss.url`。」** 只 reconcile 引用。出门条规则仍在 OSS 管家。
15. **「layered 所以 Controller 可以碰 Mapper。」** 生产 Controller 只持 UseCase。测试桥才把 Mapper 塞进夹具 Service。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `PersonMaterialAdminController.java` 与 `PersonMaterialSelfController.java`。圈两份类上同一个 `@RequestMapping("/profile/person/materials")`。圈 Admin 只有 GET、Self 只有 POST。圈 `owner(...)` 写死 `ProfileType.PERSON`。圈 `@Log` 的 `isSaveRequestData = false`。
2. 打开 `MaterialTagController.java`。数五个映射，把 `create` 标成「矩阵漏、磁盘有」。圈 `tree` 的 OR 权限串和 `includeDisabled` 默认 `false`。圈 `StatusCommand` / `VersionCommand`。
3. 打开 `ProfileMaterialHttpContractTest.java`。圈「无 DeleteMapping」和「每个 PostMapping 都有安全日志」。
4. 打开 `ProfileMaterialService.java` 的 `attach` / `detach` / `accessUrl`。顺着 `lockOwner` → 策略 → 可编辑 → 白名单 → `insertReference` / `detachReference` → `reconcileReferences`。圈 `MAX_FILE_SIZE`、`MAX_FILE_COUNT`、`ALLOWED_TYPES`、`IMMUTABLE_MATERIAL_OWNER`。
5. 打开 `SaTokenProfileMaterialAccessPolicy.java`。把 `requireAttach` 的 WORKING 申请人 vs SOURCE 管理员两条路标出来；圈 `requireWrite` **没有** override。
6. 打开 `ProfileMaterialMapper.xml` 的 `detachReference`、`archiveNode`、`selectNodes`（COMMON 并入）、以及 `10-cde-base-ddl.sql` 的 `active_owner_oss_key` 与树形 CHECK。打开 `PersonApplicationMapper.xml` 的 `countEditableMaterialWorkingOwner`（`DRAFT,BACK,CANCEL`）。
7. 打开 `ProfileMaterialMySqlE2ETest.persistsMaterialLifecycleAndKeepsAuthorizationAheadOfDownloadResolution`。圈：先 attach WORKING，再 snapshot SUBMISSION，再 access-url，再 detach WORKING，复印件仍 ATTACHED；拒绝后 `never() presignDownload`。
8. 打开前端 `person/materials/service.ts` 与 `material-tags/service.ts`、`service.test.ts` 的 lifecycle 用例。圈 GET list / POST attach / POST detach / 标签五枪。当作对照，不把 L-039 格子算进本课。

## 总结、词汇表与下一步

- **宏观三扇窗：** 只读两扇在 `PersonMaterialAdminController`，挂摘两扇在 `PersonMaterialSelfController`，分类牌五扇（含 create）在 `MaterialTagController`。两块门牌，不是三块。
- **夹子不是箱子：** attach/detach 操作 `profile_material_ref` 与 OSS 借出簿；不上传、不砸 MinIO。
- **四块板：** 只有 WORKING 可变；SOURCE 一挂即塑封；SUBMISSION/VERSION 靠无 HTTP 的 snapshot 复印。
- **两道权限：** 注解 OR + 策略按申请人/管理员/目录权再滤一遍。Admin 类名不等于「申请人进不来」。
- **受限树：** 根已种子化；系统牌保护；乐观锁；归档是软删且历史引用也挡着。
- **失败关闭：** 没过策略就不向仓库要出门条；业务码走 `R.fail`，不要默认 4xx。
- **layered 五层 + 三根电线：** UseCase 事务编排；Service 规则；DAO/XML 锁与 SQL；Policy / OwnerContributor / OssService 是端口，不是第二套 Controller。

词汇表：`PersonMaterialAdminController` / `PersonMaterialSelfController` / `MaterialTagController` / `ProfileMaterialUseCase` / `ProfileMaterialPort` / `MaterialOwnerKey` / `MaterialOwnerType` / `MaterialScope` / `MaterialNodeType` / `attach` / `detach` / `accessUrl` / `PersonProfileAccessUrl` / `immutable_flag` / `active_owner_oss_key` / `reconcileReferences` / `ProfileMaterialAccessPolicy` / `ProfileMaterialOwnerContributor` / `profile:person:material` / `profile:material-tag:manage` / layered。

下一步：个人档案审核窗与 archive 门牌是 OBJ-34。申请 current/submit 何时调用 `validateRequired` / `snapshotImmutable` 是 OBJ-35。厨房与 web-domain 把三扇窗接到页面是 OBJ-39。企业材料对称切片是 OBJ-43。OSS 直传与出门条规则分别是 OBJ-27 / OBJ-26。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / UseCase / `wta-api` | 三份公开入口与 `ProfileMaterialPort` | person `controller/{admin,self}`；`wta-api` `profile.api.material` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-profile` = layered；按五层口述 | 登记表 layered 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 三张材料表、树形 CHECK、生成唯一列、种子树与必传规则、菜单权限串 | DDL `profile_material_*`；DML 约 1377–1422、1509、1519 行 | 2026-09-16 |
| S-015 | `backend/wta-api/src/main/java/org/namewta/profile/api/material/` | 跨模块端口、owner 贡献者、快照关系只允许三种迁移 | `ProfileMaterialPort.java`；`ProfileMaterialOwnerContributor.java` | 2026-09-16 |
| S-L037-01 | `PersonMaterialAdminController.java`；`PersonMaterialSelfController.java` | 共用门牌、GET/POST 切开、写死 PERSON、attach body、detach 路径、权限串、日志不记体 | 类与四个公开方法 | 2026-09-16 |
| S-L037-02 | `MaterialTagController.java`；`ProfileMaterialHttpContractTest.java` | 五扇映射、tree OR 权限、无 DELETE、安全 `@Log` | `tree/create/update/status/archive`；三个 `@Test` | 2026-09-16 |
| S-L037-03 | `ProfileMaterialUseCase.java`；`ProfileMaterialUseCaseImpl.java`；`ProfileMaterialService.java`；`IProfileMaterialService.java` | 五层编排、`@DSTransactional`、attach/detach/tree 规则、default `accessUrlView` | UseCase 接口与实现；Service 约 81–272 行 | 2026-09-16 |
| S-L037-04 | `SaTokenProfileMaterialAccessPolicy.java`；`PersonProfileMaterialOwnerContributor.java`；`PersonProfileApiService.java`；`PersonApplicationMapper.xml` | 两道权限；WORKING 锁申请行；可编辑状态集合；SOURCE/VERSION 无申请人 | `requireAttach/Write/Read`；`lockMaterialWorkingOwner`；`countEditableMaterialWorkingOwner` | 2026-09-16 |
| S-L037-05 | `ProfileMaterialDao.java`；`ProfileMaterialMapper.xml`；`ProfileMaterialExceptionHandler.java`；`PersonProfileAccessUrl.java` | FOR UPDATE、detach SQL 约束、archive 软删、COMMON 并入 tree、`R.fail`、HTTP 不暴露 `OssAccessUrl` 类型 | XML `detachReference`/`archiveNode`/`selectNodes`；Handler `basePackages` | 2026-09-16 |
| S-L037-06 | `ProfileMaterialServiceTest.java`；`ProfileMaterialMySqlE2ETest.java`；`ProfileSchemaMySqlIntegrationTest.java` | 挂接拒绝链、SOURCE 不可变、摘夹子不删对象、鉴权先于出门条、单 oss 单 ATTACHED、非法 depth 被 CHECK 拦 | `attachesOneValidatedFile...`；`persistsMaterialLifecycle...`；`assertSingleActiveMaterialTag` | 2026-09-16 |
| S-L037-07 | `frontend/packages/domains/profile/src/person/materials/service.ts`；`material-tags/service.ts`；`service.test.ts`；`permissions.ts` | 厨房四枪/五枪 URL 与方法；资源标签名；权限字典（对照，不认 OBJ-39 格子） | `createPersonMaterialService`；lifecycle `it` | 2026-09-16 |
| S-L037-08 | `PersonAdminController.java`（仅边界）；`EnterpriseMaterialSelfController.java`（仅边界）；`frontend/packages/api-contracts/generated/openapi.ts` 与 `openapi/current.json` | archive 出门条是隔壁门牌；企业 Self 对称但另一前缀；生成 OpenAPI 有路径、current.json 无 | archive `@GetMapping` material access-url；openapi 路径 vs 快照缺席 | 2026-09-16 |
