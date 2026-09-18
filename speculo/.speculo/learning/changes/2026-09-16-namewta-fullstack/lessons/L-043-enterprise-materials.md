---
lesson_id: L-043
objective_ids: [OBJ-43]
claimed_cells:
  - A:EnterpriseMaterialAdminController.*
  - A:EnterpriseMaterialSelfController.*
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-windows-four-mappings
    minutes: 8
  - segment: attach-detach-causal
    minutes: 10
  - segment: owner-lock-and-required-tags
    minutes: 9
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-015, S-L037-01, S-L040-01, S-L041-01, S-L043-01, S-L043-02, S-L043-03, S-L043-04, S-L043-05, S-L043-06, S-L043-07, S-L043-08]
---

# Lesson 043：宏观两扇企业材料窗——口述 admin list/access-url 与 self attach/detach

## 学完你能做什么

打开 `backend/wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/controller/` 里**两份** Java，你能**口述企业认证材料怎么列、怎么看出门条、怎么挂、怎么摘**。不要把这件事说成 OSS 上传，也不要把档案审核窗 `EnterpriseAdminController` 的两条 access-url 说成本课门牌。

口试名单就是矩阵 (a) 两行，方法名以**磁盘**为准：

1. **`A:EnterpriseMaterialAdminController.list, accessUrl`**：同一门牌 `GET /profile/enterprise/materials/{ownerType}/{ownerId}` 与 `GET .../{materialRefId}/access-url`。类名带 Admin，权限却 **OR** 开了申请人 `profile:enterprise:material`。只读。没有 `@Log`。没有 POST。
2. **`A:EnterpriseMaterialSelfController.attach, detach`**：同一门牌上的 **POST**。`attach` 挂一张已入库的 OSS 对象；`detach` 只改引用状态，**不**删对象、**不**走 `DELETE`。权限只认 `profile:enterprise:material`。两扇都有 `@Log`，且 **不**记请求/响应体。

2026-09-16 工作树先钉死**两句话**，口试先数门牌，再数方法：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 一份 `EnterpriseMaterialController` | **没有这个 Java 类。** 前端资源标签写成 `EnterpriseMaterialController`，后端是 Admin + Self **两份**类，共用 `/profile/enterprise/materials` |
| 管理端也能 POST attach | **Self 才映射 POST。** 管理员要挂材料，走的是 **SOURCE** owner + `profile:enterprise:override`，而且常常是 L-040 `admin-create` 在 Service 里调端口，不是在 Admin 材料类上多一扇窗 |
| detach = 删文件 / `DELETE` | **POST `.../detach`。** 行变成 `DETACHED`；OSS 箱子还在；`@Log` 的 `businessType` 是 `UPDATE` |
| 企业模块也有 `MaterialTagController` | **没有。** 全仓库只有 person 包里那一份；`scope=ENTERPRISE/COMMON` 也打 `/profile/material-tags`（L-037） |
| `/profile/enterprise/archive/.../access-url` 是本课 | **那是 L-040** `EnterpriseAdminController.reviewMaterial` / `material`。路径写死 SUBMISSION / VERSION |
| 企业自己有材料 Mapper / DAO | **没有。** `wta-profile-enterprise` 的 POM **不**依赖 `wta-profile-person`。夹子表由共享 `ProfileMaterialPort` 实现（人模块 `ProfileMaterialService`）写；企业只贡献 owner 锁 |

`wta-profile` 在登记表是 **layered**：`Controller -> UseCase -> Service -> DAO -> Mapper -> XML`。企业材料这条河还多三根电线，仍不是 classic：`ProfileMaterialAccessPolicy`、`ProfileMaterialOwnerContributor`、跨模块 `OssService`。不要口述成 `Controller → ServiceImpl → Mapper`。Controller 字段名叫 `materialPort`，类型是 **`ProfileMaterialUseCase`**（企业包那份），不是 `EnterpriseMaterialService`。

本课**不宣称**你会拆企业档案审核/直建（L-040）、自助申请 current/submit（L-041）、转移（L-042）、厨房 `createEnterpriseMaterialService` 与 web-domain（L-044），或把个人材料三份 Controller 再讲一遍（L-037）。今天只认：**企业挂钩房的四扇 HTTP，以及它们怎么把 `ProfileType.ENTERPRISE` 钉死在钥匙上。**

## 先把宏观地图放在桌上

L-003 已经把 `wta-profile` 钉在 layered 列。L-026 把仓库门口的卡片柜讲成「列表涂黑、出门条另开、删除盖 PENDING」。L-037 把**个人**挂钩房讲完了。本课站在 **企业楼自己的挂钩** 这一头：营业执照照片必须先经 OSS 直传进仓（L-027 / L-029），这里只把 `ossId` **别到**某次企业草稿/快照上，并盖一枚企业标签码。

2026-09-16 工作树：企业材料 HTTP 是**一块门牌、两份 Controller、正好四扇窗**：

```text
已登录的人（浏览器，Admin-Token）
        │
        ├─ /profile/enterprise/materials          ← 本课材料引用
        │     GET  /{ownerType}/{ownerId}                         EnterpriseMaterialAdminController.list
        │     GET  /{ownerType}/{ownerId}/{materialRefId}/access-url
        │                                                         EnterpriseMaterialAdminController.accessUrl
        │     POST /{ownerType}/{ownerId}                         EnterpriseMaterialSelfController.attach
        │     POST /{ownerType}/{ownerId}/{materialRefId}/detach  EnterpriseMaterialSelfController.detach
        │
        ├─ /profile/enterprise/archive            EnterpriseAdminController     L-040
        │     GET  /application/{id}/material/{ref}/access-url     写死 SUBMISSION
        │     GET  /{profileId}/material/{ref}/access-url          写死 VERSION
        │
        ├─ /profile/enterprise/application        EnterpriseApplicationController  L-041
        │     submit 会喊 validateRequired / snapshotImmutable，没有材料 HTTP
        │
        ├─ /profile/material-tags                 MaterialTagController         L-037
        │     企业 scope 的分类牌也打这里
        │
        └─ /profile/person/materials              Person*Material*              L-037
```

往下走不要跳层：

```text
两份 Controller（只 import usecase，架构测试禁止 import service）
    └─ org.namewta.profile.enterprise.usecase.ProfileMaterialUseCase
          └─ EnterpriseProfileMaterialUseCaseImpl     每个方法 @DSTransactional
                └─ EnterpriseMaterialService          薄转发器；注入 ProfileMaterialPort
                      └─ ProfileMaterialPort（wta-api）
                            └─ 运行时实现：person 模块 ProfileMaterialService
                                  ├─ ProfileMaterialDao -> ProfileMaterialMapper.xml
                                  │      profile_material_node / profile_material_ref / profile_material_requirement
                                  ├─ OssService  （objectMetadata / reconcileReferences / resolveAccessUrl）
                                  ├─ ProfileMaterialAccessPolicy  ← SaTokenProfileMaterialAccessPolicy
                                  └─ Map<ProfileType, ProfileMaterialOwnerContributor>
                                        ENTERPRISE → EnterpriseMaterialOwnerContributor
                                              → EnterpriseProfileApiUseCase.lockMaterialOwner / isWorkingEditable
                                              → EnterpriseApplicationMapper 锁申请/提交/来源/版本行
```

`ProfileMaterialExceptionHandler` 住在 **person** 包 `controller.admin`，`@RestControllerAdvice` 的 `basePackages` 同时罩 person **和** enterprise 的 admin/self。企业模块**没有**自己的材料 Advice。业务失败走 `R.fail(category)`：**HTTP 仍可能是 200**，`msg` 才是 `MATERIAL_ACCESS_DENIED` 这类码。

企业 POM 只依赖 `wta-api`，**不** Maven 依赖 `wta-profile-person`。编译期看见的是端口；运行期主应用把两个模块装在一起。口试可以说「企业 HTTP 不碰夹子表」，不要说「企业模块里另有一套材料 ServiceImpl」。

## 核心概念与机制

### 直觉讲解

把这件事想成**公司衣帽间挂钩**，不是仓库碎纸机，也不是工商科柜台。

- **分类牌（material tag / node）**：墙上分区。「营业执照」「法定代表人身份证明」「企业授权委托书」是预先钉好的牌子。牌子的增改停用归档，窗口在隔壁个人楼的 `/profile/material-tags`，本课 HTTP **碰不到**。
- **挂钩上的夹子（material ref）**：把一张**已经进仓库**的执照照片夹到某个挂钩上。夹子记下：哪张 OSS、哪个标签码、文件名、大小、MIME。摘夹子 = 夹子变成「已摘」，照片还在仓库货架上。
- **四块板（owner type）**：
  - `WORKING`：企业申请草稿板。还能挂、还能摘。
  - `SUBMISSION`：交上去那一瞬复印并塑封的板。
  - `VERSION`：执照柜里的正式版本，也是塑封件。
  - `SOURCE`：管理员直接塞进柜子的塑封件（自己的 OSS 对象，一挂就不可变）。

**类比失效边界：** 衣帽间类比**不**覆盖「照片怎么进仓库」。进仓是 OSS 直传。本课只认夹子。类比也**不**等于「摘夹子 = 仓库删箱子」——`detach` 最终调用 `ossService.reconcileReferences(..., Set.of(ossId), Set.of())`，是从借出簿去掉这一行引用，不是 `objectStore.delete`。类比还不等于「一块板上同一张照片可以夹两个挂钩」——表上生成列 `active_owner_oss_key` 保证：**同一 owner 上，同一 `ossId` 同时只能有一条 ATTACHED**。类比更不等于「URL 里的 ownerType 能把档案改成个人」——两份 Controller 的 `owner(...)` **写死** `ProfileType.ENTERPRISE`。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 企业材料只读窗 | `EnterpriseMaterialAdminController` | `controller/admin/`；`@RequestMapping("/profile/enterprise/materials")`；两扇 GET |
| 企业材料自助窗 | `EnterpriseMaterialSelfController` | `controller/self/`；同一前缀；两扇 POST |
| 材料目录节点 | material node | 表 `profile_material_node`；领域在 person 模块；对外 `MaterialNodeView` |
| 适用范围 | `PERSON` / `ENTERPRISE` / `COMMON` | 枚举 `MaterialScope`；列名仍叫 `profile_type` |
| 材料引用 | material reference | 表 `profile_material_ref`；`MaterialReferenceView` |
| 归属钥匙 | owner key | `MaterialOwnerKey(profileType, ownerType, ownerId)`；`ownerId` 必须为正 |
| 归属种类 | `WORKING` / `SUBMISSION` / `SOURCE` / `VERSION` | 枚举 `MaterialOwnerType` |
| 挂接 | attach | `POST .../{ownerType}/{ownerId}`；`MaterialAttachCommand(owner, ossId, materialNodeId)` |
| 解除挂接 | detach | `POST .../{materialRefId}/detach`；状态 `ATTACHED` → `DETACHED` |
| 出门条 | access URL | HTTP 返回 `EnterpriseProfileAccessUrl`；内部先拿 `OssService.OssAccessUrl` 再投影 |
| 不可变证据 | immutable evidence | 列 `immutable_flag`；SOURCE 挂上即 `Y`；快照复制也是 `Y` |
| 共享端口 | `ProfileMaterialPort` | `wta-api`；person 实现写表；enterprise HTTP / Admin Service / Application Service 都调它 |
| 所有者贡献者 | `ProfileMaterialOwnerContributor` | 企业实现 `EnterpriseMaterialOwnerContributor`；PERSON 钥匙进来会 `IllegalArgumentException` |
| 企业用例合同 | `org.namewta.profile.enterprise.usecase.ProfileMaterialUseCase` | 与 person 包**同名不同包**；default `accessUrlView` 抛「旧适配器不支持文件访问地址入口」 |

Controller 公开 HTTP 以磁盘为准（矩阵两行，正好四扇；**没有** tree/create/snapshot 窗）：

| Java 方法 | HTTP | 权限 | `@Log` | 返回 |
| --- | --- | --- | --- | --- |
| `EnterpriseMaterialAdminController.list` | `GET /profile/enterprise/materials/{ownerType}/{ownerId}` | `profile:enterprise:material` \| `query` \| `review` \| `manage` \| `override`（OR） | 无 | `R<List<MaterialReferenceView>>` |
| `EnterpriseMaterialAdminController.accessUrl` | `GET .../{materialRefId}/access-url` | 同上 | 无 | `R<EnterpriseProfileAccessUrl>` |
| `EnterpriseMaterialSelfController.attach` | `POST /profile/enterprise/materials/{ownerType}/{ownerId}` | `profile:enterprise:material` | INSERT，title「挂接企业认证材料」，**不**记体 | `R<MaterialReferenceView>` |
| `EnterpriseMaterialSelfController.detach` | `POST .../{materialRefId}/detach` | `profile:enterprise:material` | UPDATE，title「解除企业认证材料」，**不**记体 | `R<Void>` |

合同测试锁死：

- Admin：**正好两扇 GET**，**零** POST、**零** DELETE；OR 权限串顺序不限、集合固定五枚。
- Self：**正好两扇 POST**，**零** GET、**零** DELETE；每扇都有 `@Log` 且 `isSaveRequestData=false`、`isSaveResponseData=false`。
- 两边 `profileType()` 都是 `ProfileType.ENTERPRISE`。
- Bean 校验：Admin 的 `ownerId` / `materialRefId` 带 `@Positive`；Self 的 path `ownerId` / `materialRefId` 也带 `@Positive`；`AttachRequest(ossId, materialNodeId)` 两字段都 `@Positive`。个人 Self 的 `AttachRequest` **没有** `@Positive`——不要把两边说成同一份。

UseCase 上还有 **`tree` / `createNode` / `updateNode` / `changeStatus` / `archiveNode` / `validateRequired` / `snapshotImmutable`**，**没有**本课 HTTP。分类牌走 L-037；提交申请、管理员直建会在别处调用必传校验和塑封复印。口试可以说「挂钩服务会复印塑封」，不要说「企业材料 Controller 有一扇 snapshot 窗」。

`EnterpriseMaterialService` **不**实现 `ProfileMaterialPort`，也**没有** `IEnterpriseMaterialService`。它只是把调用转给端口，并在 `accessUrlView` 里把 `OssAccessUrl` 投影成 `EnterpriseProfileAccessUrl(accessType, url, expiresAt, fileName)`，避免把 system 类型漏到 Profile HTTP。

### 机制/因果链

**1. 先锁企业行，再动夹子。**

`list` / `accessUrl` / `attach` / `detach` 都先 `lockOwner`。钥匙上的 `profileType` 必须是 ENTERPRISE，否则贡献者直接 `IllegalArgumentException("material owner must use ENTERPRISE profileType")`。企业 SQL：

| ownerType | XML id | 锁哪一行 | 带回什么 |
| --- | --- | --- | --- |
| `WORKING` | `lockMaterialWorkingOwner` | `profile_enterprise_application` `FOR UPDATE` | `applicant_user_id` |
| `SUBMISSION` | `lockMaterialSubmissionOwner` | 提交表 join 申请表 `FOR UPDATE` | 申请人 |
| `SOURCE` | `lockMaterialSourceOwner` | `profile_enterprise_source` `FOR UPDATE` | **申请人字段为 null**（SQL 选的是 `enterprise_source_id`，Service 丢掉当 id） |
| `VERSION` | `lockMaterialVersionOwner` | `profile_enterprise_version` `FOR UPDATE` | **申请人字段为 null** |

找不到行 → `MATERIAL_OWNER_NOT_FOUND`。企业这边是 **四条** 独立 lock，不像个人 SOURCE/VERSION 合成 `lockMaterialImmutableOwner`。UseCase 每个方法都标了 `@DSTransactional`，读路径也是事务：因为后面有 `FOR UPDATE`。`EnterpriseProfileApiUseCaseImpl.lockMaterialOwner` 自己也有 `@DSTransactional`，给端口从企业侧锁行时用。

**2. 权限是第二道门，注解只是第一道。**

HTTP 注解过了，还要 `SaTokenProfileMaterialAccessPolicy`。前缀按钥匙生成：`profile:` + `profileType.name().toLowerCase()`，企业就是 `profile:enterprise`：

| 动作 | 策略方法 | 谁过 |
| --- | --- | --- |
| 挂 WORKING | `requireAttach` | 当前用户 == 申请人，且有 `profile:enterprise:material` |
| 挂 SOURCE | `requireAttach` | 有 `profile:enterprise:override`（管理员用**自己的** OSS） |
| 摘 | `requireWrite` | **只**申请人 + `:material`。override **不能**摘 |
| 列表 / 出门条 | `requireRead` | 管理四权（query/review/manage/override）**或**申请人 + `:material` |

所以：**类名叫 Admin 的 GET，申请人拿着 `:material` 也能打。** 前端 `enterprise.materials.list('VERSION', ...)` 打的就是这扇 GET。**管理员不能靠 override 去 POST detach。** 管理员要补材料，走 SOURCE attach，夹子一挂 `immutableEvidence=true`。

WORKING 还要 `isWorkingEditable`：申请状态必须是 `DRAFT` / `BACK` / `CANCEL`（XML `countEditableMaterialWorkingOwner`）。已提交的草稿板 → `MATERIAL_OWNER_READ_ONLY`。SOURCE 挂接**不**查这扇门。对 VERSION 喊 `isWorkingEditable` → `IllegalArgumentException("editable owner must use WORKING")`。

**3. attach：校验全部通过才插行，才改借出簿。**

顺序（失败就停，共享 Service 测试要求 **零** `insertReference`、**零** `reconcileReferences`）：

1. owner 只能是 `WORKING` 或 `SOURCE`，否则 `IMMUTABLE_MATERIAL_OWNER`
2. 权限 +（WORKING 时）可编辑
3. 节点必须是启用中的 `TAG`，且 scope 是 `COMMON` 或 `ENTERPRISE`，否则 `MATERIAL_TAG_UNAVAILABLE` / `MATERIAL_TAG_NOT_APPLICABLE`（把个人身份证 TAG 夹到企业板上会走后一个）
4. 该 owner 已 ATTACHED 条数 `< 10`，否则 `MATERIAL_COUNT_LIMIT`
5. `ossService.objectMetadata(ossId)`；`uploaderUserId` 必须等于策略返回的当前用户，否则 `MATERIAL_OSS_OWNER_MISMATCH`
6. 大小 `(0, 10MiB]`；扩展名与库里 suffix 一致；MIME 只能是 jpeg/png/pdf 的白名单，否则 `MATERIAL_FILE_TOO_LARGE` / `MATERIAL_FILE_TYPE_INVALID`
7. 插入引用：WORKING 的 `immutable_flag=N`，SOURCE 的 `Y`；状态 `ATTACHED`
8. 唯一键冲突（同一 owner 上同一 oss 仍 ATTACHED）→ `MATERIAL_ALREADY_ATTACHED`
9. `ossService.reconcileReferences("profile_material_ref", refId, Set.of(), Set.of(ossId))`

请求体只有 `{ ossId, materialNodeId }`。没有文件字节，没有 multipart。Self 的 `AttachRequest` 是 Controller 内部 record，不是 `EnterpriseAdminMaterialBo`——后者给 L-040 直建用。

**4. detach：只摘可变的草稿夹子。**

1. `requireWrite` + owner 必须是 WORKING，否则 `IMMUTABLE_MATERIAL`
2. 可编辑
3. 锁夹子；夹子必须属于这把 owner 钥匙，否则对外仍说 `MATERIAL_NOT_FOUND`（不泄露别人的 refId）
4. `immutableEvidence` → `IMMUTABLE_MATERIAL`
5. 已经 DETACHED → **直接 return**（幂等，不打 OSS）
6. SQL：`status='DETACHED'`，写下 `detached_time`，且 **只更新** `ATTACHED + immutable_flag='N'` 的行
7. `reconcileReferences(..., Set.of(ossId), Set.of())` —— 从借出簿去掉，**不是**删对象

摘 WORKING ≠ 撕掉已交卷的 SUBMISSION 复印件。同一 `oss_id` 会留下两行引用。

**5. list / accessUrl。**

`list` 按 owner 拉全部未删引用，**含 DETACHED 历史**，按挂接时间排序。`accessUrl` 再锁夹子、对 owner；WORKING 上已摘的夹子 → `MATERIAL_NOT_ATTACHED`。通过后才 `ossService.resolveAccessUrl(ossId)`，再投影成 `EnterpriseProfileAccessUrl`。出门条的 PUBLIC/PRIVATE 规则是 OSS 课的；本课只保证：**没过 Profile 权限，根本不向仓库要地址。**

隔壁 L-040 的两条 archive 出门条**不经过**本课 Controller：`reviewMaterial` 自己把 owner 钉成 SUBMISSION，`material` 钉成当前 VERSION，再直接调 `ProfileMaterialPort.accessUrl`。同一张投影类型，两块门牌。

**6. 本课 HTTP 碰不到、但挂钩服务会做的两件事。**

- `validateRequired`：按 `profile_type=ENTERPRISE` 读 `profile_material_requirement`。种子用 `document_type_code='*'`，所以提交传 `"*"`、管理员直建传法人证件类型码，**两条 ALWAYS 都会命中**。大陆企业默认要：营业执照 **和** 法定代表人身份证明各至少 1。办理人不是法人时，申请 submit 再加条件 `HANDLER_NOT_LEGAL_REPRESENTATIVE`，才要求授权委托书。缺哪枚就 `MISSING_REQUIRED_MATERIAL:TAG_CODE`。
- `snapshotImmutable`：只允许 WORKING→SUBMISSION、SUBMISSION→VERSION、SOURCE→VERSION，且贡献者承认这对关系（XML `countWorkingSubmissionRelationship` / `countSubmissionVersionRelationship` / `countSourceVersionRelationship`）。把源上仍 ATTACHED 的夹子 **复制** 成目标上的不可变夹子。这是交卷/发布/直建，不是用户点的第五扇窗。

### 图、表或文本图

**图 1 标题 / caption：** 宏观两扇企业材料窗与四块板。一块门牌、两份类、四扇 HTTP；分类牌和档案审核窗都在隔壁。

**alt：** 左列为浏览器四个 HTTP，中列为企业 UseCase 薄转发到共享 Port，右列为四张企业 owner 板和 OSS 仓库。

```text
  浏览器                                    企业挂钩房                              仓库
  --------                                  ----------                              ----
  GET  /enterprise/materials/{type}/{id}
        Admin.list                 ──►  lock 企业行 + requireRead  ──►  列出夹子
  GET  /.../access-url
        Admin.accessUrl            ──►  同上，再 resolveAccessUrl ──►  短时出门条
                                        投影 EnterpriseProfileAccessUrl
  POST /enterprise/materials/{type}/{id}
        Self.attach                ──►  attach WORKING|SOURCE
        {ossId, materialNodeId}           │
                                          ├─ 白名单/大小/本人上传
                                          ├─ insert ATTACHED 夹子
                                          └─ reconcile +ossId ─────────────► 借出簿 +1
  POST /.../{refId}/detach
        Self.detach                ──►  仅 WORKING 可变夹子
                                          ├─ status=DETACHED
                                          └─ reconcile -ossId ─────────────► 借出簿 -1
                                                                          箱子仍在货架

  不在本课：
  GET  /material-tags/tree         MaterialTagController              L-037
  GET  /enterprise/archive/.../access-url   Admin 档案窗              L-040
  POST /enterprise/application/submit       喊 validate/snapshot      L-041

  WORKING 草稿板  ==snapshot==►  SUBMISSION 塑封  ==snapshot==►  VERSION 执照柜
  SOURCE 管理员塑封  ========================================►  VERSION
```

**文字等价物：** 调用方从不把文件字节打进 `/profile/enterprise/materials`。`GET list` 和 `GET access-url` 共用 Admin 类、只读、权限 OR 五枚。`POST attach` / `POST detach` 共用 Self 类；挂只能打在 WORKING 或 SOURCE 板上，摘只能打在 WORKING 且非塑封夹子上。两份类都把钥匙的档案类型写成 ENTERPRISE。挂成功会在共享表 `profile_material_ref` 插入 ATTACHED 行，并通知 OSS 借出簿增加该 `ossId`；摘只把该行标 DETACHED 并从借出簿去掉，仓库对象仍在。分类牌仍是 `/profile/material-tags`。WORKING 板上的夹子可以在交卷时被复印到 SUBMISSION，再复印到 VERSION；管理员 SOURCE 也可以直接复印到 VERSION。复印件默认不可变。本图不画档案审核窗，不画直传 init/complete，不画转移门。

**图的边界：** 不保证 `openapi/current.json` 与生成文件同步——2026-09-16 工作树里 **生成 `openapi.ts` 已有** 企业材料四条路径，`current.json` **搜不到** `/profile/`。厨房 `createEnterpriseMaterialService` 自己拼 URL。不要把 OpenAPI 快照说成这份 HTTP 的权威源。不保证业务失败是 HTTP 4xx：Handler 返回 `R.fail`。不保证企业模块自己写夹子 SQL：DAO/XML 在 person。

**图 2 标题 / caption：** 种子企业材料子树（只画 ENTERPRISE 相关；个人牌在 L-037）。

```text
企业材料 (CATEGORY, depth=1, ENTERPRISE)
  ├─ 企业主体证明 (CATEGORY, depth=2)
  │     ├─ ENTERPRISE_BUSINESS_LICENSE                 营业执照           systemRequired=Y
  │     └─ ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT    法定代表人身份证明 systemRequired=Y
  └─ 企业办理证明 (CATEGORY, depth=2)
        └─ ENTERPRISE_AUTHORIZATION_LETTER             企业授权委托书     systemRequired=Y
```

**文字等价物：** 种子在 `50-cde-base-dml.sql` 插入三棵根；企业根下两棵二级分类、三枚 TAG，全部 `systemRequired=Y`。必传规则三条都挂 `profile_type=ENTERPRISE` 且 `document_type_code='*'`：ALWAYS 要执照 + 法人证件；办理人不是法人时再要授权书。这些系统牌停用/归档都会被共享 Service 拒绝。口试说到「营业执照」时，指的是 tag code `ENTERPRISE_BUSINESS_LICENSE`，不是同一张 OSS 夹两次。

### 正例、反例与边界

**正例 1：** 申请人企业草稿 `DRAFT`，已直传一张自己的 `license.pdf`（pdf，≤10MiB）。`POST /profile/enterprise/materials/WORKING/{applicationId}`，body `{ ossId, materialNodeId: 营业执照 }`。得到 `MaterialReferenceView`，`attached=true`，`immutableEvidence=false`。仓库借出簿多一条 `profile_material_ref`。

**正例 2：** 同一草稿再挂法人证件（**另一** ossId）。`validateRequired(..., "*", {ALWAYS})` 才能过。只挂执照时，缺的是 `MISSING_REQUIRED_MATERIAL:ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT`。

**正例 3：** 办理人不是法人，submit 还会把 `HANDLER_NOT_LEGAL_REPRESENTATIVE` 放进条件。缺授权书 → `MISSING_REQUIRED_MATERIAL:ENTERPRISE_AUTHORIZATION_LETTER`。这枪在 L-041 的 submit，不在本课 HTTP。

**正例 4：** 申请人 `POST .../{workingRefId}/detach`。WORKING 行变 DETACHED；若已经 snapshot 到 SUBMISSION，复印件仍 ATTACHED。再 `GET .../SUBMISSION/{submissionId}/{immutableRefId}/access-url` 仍能拿到出门条（持 `:material` 或管理四权）。

**正例 5：** 管理员用自己上传的对象走 SOURCE attach，且持有 `profile:enterprise:override`。夹子 `immutableEvidence=true`。不检查申请是否可编辑。本课 Self 类**可以**打这条 POST；L-040 直建则是 Service 循环 `EnterpriseAdminMaterialBo` 调端口，不经过 Self Controller。

**正例 6：** 已摘的 WORKING 夹子再 detach 一次：共享 Service 发现 `!attached()`，直接返回，不碰 OSS。

**正例 7：** 申请人 `GET /profile/enterprise/materials/WORKING/{applicationId}`。Admin 类的 OR 含 `:material`，策略 `requireRead` 也给申请人。返回含 DETACHED 历史。

**反例 1：** 「Admin 类也能 attach。」磁盘 Admin 只有 GET。合同测试 `noneMatch PostMapping || DeleteMapping`。

**反例 2：** 「detach 是 `DELETE /materials/{id}`，文件从 MinIO 消失。」合同测试禁止 DeleteMapping。对象仍在。L-026 的 PENDING 删除是仓库管理员通道，不是这扇窗。

**反例 3：** 「列表 VO 的 url 就能预览。」本课 list 返回的是引用视图（文件名、tag、ossId），可用地址只在 access-url。不要把 L-026 管理列表涂黑和这里混成一句。

**反例 4：** 「`/profile/enterprise/archive/{profileId}/material/{ref}/access-url` 是 Admin 材料窗。」那是 L-040，权限还写成单独的 `profile:enterprise:material`，门牌是 archive，owner 写死 VERSION。

**反例 5：** 「企业材料 POST `/profile/person/materials/WORKING/...` 把 profileType 改成 ENTERPRISE。」person Controller 写死 PERSON。企业是 `/profile/enterprise/materials`。两把钥匙进错贡献者会 `IllegalArgumentException`。

**反例 6：** 「管理员 override 可以帮申请人摘 WORKING 夹子。」`requireWrite` 只要申请人。override 只能 SOURCE attach。

**反例 7：** 「同一张执照照片夹到营业执照和法人证件。」`uk_profile_material_ref_active_owner_oss`：同一 owner + 同一 oss 在 ATTACHED 时不能两行。先 DETACHED 才能再挂。

**反例 8：** 「挂别人的 OSS。」`MATERIAL_OSS_OWNER_MISMATCH`。元数据里的上传者必须是当前登录用户。

**反例 9：** 「提交后还能在 WORKING 上补图。」`countEditableMaterialWorkingOwner` 只要 `DRAFT|BACK|CANCEL`。否则 `MATERIAL_OWNER_READ_ONLY`。

**反例 10：** 「企业也有 MaterialTagController / 材料 Mapper。」分类牌在 person 包；企业 POM 看不见 person 的 Mapper。

**反例 11：** 「前端资源名 `EnterpriseMaterialController` 对应一个后端类。」只是 `profileEnterpriseMaterialsResource.controller` 字符串。

**反例 12：** 「业务失败一定 403/404。」材料业务码走 `ProfileMaterialException` → `R.fail(message)`。鉴权注解失败才是 Sa-Token 那条路。

**反例 13：** 「`EnterpriseMaterialService` 就是 `ProfileMaterialPort` 实现。」它是转发器。实现类在 person 的 `ProfileMaterialService implements IProfileMaterialService`。

**反例 14：** 「UseCase 字段名叫 materialPort，所以 Controller 直接拿着跨模块端口。」类型是企业包 `ProfileMaterialUseCase`。架构测试禁止 Controller import `service`。

**反例 15：** 「snapshot / validateRequired 是本课第四、第五扇窗。」UseCase 有方法，本课 Controller **没有**映射。

**边界：**

- 文件白名单只认 `.jpg` / `.jpeg` / `.png` / `.pdf` 与对应 MIME。gif/heic/zip 走 `MATERIAL_FILE_TYPE_INVALID`。
- 每块板最多 10 条 **当前 ATTACHED**，不是历史上曾经挂过的总数。
- `list` 含 DETACHED；`countAttached` / 必传校验只数 ATTACHED。
- `accessUrl` 对非 WORKING 的未挂状态没有同样的 `MATERIAL_NOT_ATTACHED` 短路；正常路径上不可变板也不该被 detach。
- 企业 TAG 三枚都是系统必传牌；停用/归档会被 `SYSTEM_MATERIAL_TAG_PROTECTED` 挡住——但这是标签窗的规则，不是本课四扇 HTTP。
- 管理员直建的 `validateRequired` 只传 `ALWAYS`，**不会**因为办理人不是法人而要授权书；自助 submit 才会加第三枚条件。
- 厨房会 `encodeURIComponent` 路径段；后端 path 是枚举 + Long。`service.test.ts` 里 `'VERSION', 9, 10` 打的是 `GET /profile/enterprise/materials/VERSION/9/10/access-url`，当作对照，不把 L-044 格子算进本课。

## 变式与迁移

- **变式 A：申请人要预览自己刚挂的营业执照。** `GET .../WORKING/{appId}/{refId}/access-url`。不要打 L-026 的 `/resource/oss/{ossId}/download-url`（那是 `system:oss:download` 仓库管理员通道）。也不要打 archive 窗——那扇不认 WORKING。
- **变式 B：审核员要看提交件。** 可以打本课 `GET .../SUBMISSION/{submissionId}/.../access-url`（持 review/query/manage/override 即可过 `requireRead`），也可以走 L-040 的 archive 审核窗。两扇门牌，一种出门条投影 `EnterpriseProfileAccessUrl`。不要说只有一扇。
- **变式 C：补一张拍错的执照。** 先 detach 旧 WORKING 夹子（同一 oss 才能再次 ATTACHED），再 attach 新 ossId。不要指望「同一 oss 改挂到另一 tag」在 ATTACHED 时成功。
- **变式 D：管理员发现申请人没图，要直接建档。** L-040 `POST /admin-create` 的 `materials[]` 走 SOURCE attach + `snapshotImmutable(SOURCE → VERSION)`。不要在 WORKING 上用 override 硬挂——策略不允许。也不要以为本课 Admin 类多了一扇 POST。
- **变式 E：只要分类牌给上传页下拉。** `GET /profile/material-tags/tree?scope=ENTERPRISE`。窗口在 L-037。树 SQL 会把 `COMMON` 一并带上。
- **变式 F：个人身份证材料。** URL 换成 `/profile/person/materials`，权限前缀换成 `profile:person:*`，`owner(...)` 写死 PERSON。分类牌仍是同一棵 `MaterialTagController`。细节在 L-037。
- **变式 G：对照 OSS 卡片柜。** 仓库 list 涂黑、download-url、PENDING 删除是 L-026。本课 list 是夹子清单；access-url 是夹子授权之后才向 `OssService` 借出门条；detach 是还夹子不是盖 PENDING。
- **变式 H：submit 报缺材料。** 先在本课 WORKING 板上挂齐 ALWAYS 两枚；办理人不是法人再挂授权书。不要在 application Controller 上找 attach。
- **迁移口诀：** 先数一块门牌两份类四扇窗 → GET 只读在 Admin、POST 挂摘在 Self → 钥匙写死 ENTERPRISE → 四块板只有 WORKING 可变、SOURCE 一挂即塑封 → 摘夹子不砸箱 → 分类牌仍在 person 楼 → 必传与塑封复印没有本课 HTTP → 企业不自写夹子 Mapper。跳步会出现「把 archive 审核窗当材料窗」「把 detach 说成删 MinIO」「把 Admin 类说成不能给申请人 list」「把企业 POM 说成依赖 person」。

## 常见误区

1. **「OBJ-43 还包含 `createEnterpriseMaterialService` / web-domain。」** 那是 OBJ-44。本课可以拿厨房 URL 当对照，格子不认工厂。
2. **「一份 Controller 管 list+attach。」** 两份类，按 HTTP 方法切开，门牌相同。
3. **「材料上传走这扇 POST。」** body 只有 ossId + materialNodeId。字节在 OSS 直传。
4. **「detach / 删材料 是 DELETE。」** POST；合同测试禁止 DeleteMapping。
5. **「Admin 就是管理员专用。」** GET 的 OR 含 `:material`；策略 `requireRead` 也给申请人。
6. **「override 万能。」** 能 SOURCE attach 和读；不能 `requireWrite` detach；不能靠 HTTP 注解绕过 Service。
7. **「企业也有 MaterialTagController。」** 全仓一份，住在 person 包。
8. **「`EnterpriseMaterialController` 在 Java 里。」** 只有前端资源字符串。
9. **「失败就是 403。」** 材料业务码经常是 200 + `R.fail`。Handler 还住在 person 包。
10. **「快照窗在本课。」** `snapshotImmutable` 无本课 HTTP。
11. **「企业模块自己实现了夹子表。」** 三张 `profile_material_*` 由 person DAO 读写；企业 XML 只锁申请/提交/来源/版本。
12. **「OpenAPI current.json 就是权威。」** 快照缺 `/profile/`；生成文件与厨房硬编码才看得到这些路径。把冲突说出来，不要选边假装没有。
13. **「挂接会改 `sys_oss.url`。」** 只 reconcile 引用。出门条规则仍在 OSS 管家。
14. **「layered 所以 Controller 可以碰 Mapper。」** 生产 Controller 只持 UseCase。企业甚至没有材料 Mapper 可碰。
15. **「个人 Self 和企业 Self 校验一样。」** 企业 path 与 `AttachRequest` 都有 `@Positive`；个人 `AttachRequest` 没有。
16. **「apply 权限也能挂材料。」** 申请桌是 `profile:enterprise:apply`（L-041）。材料自助是 `:material`。两把通行证。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `EnterpriseMaterialAdminController.java` 与 `EnterpriseMaterialSelfController.java`。圈两份类上同一个 `@RequestMapping("/profile/enterprise/materials")`。圈 Admin 只有 GET、Self 只有 POST。圈 `owner(...)` 写死 `ProfileType.ENTERPRISE`。圈 Self `@Log` 的 title 与 `isSaveRequestData = false`。圈 Self `ownerId` / `AttachRequest` 的 `@Positive`。
2. 打开两份 `*Material*ControllerContractTest`。圈「Admin 两扇 GET、零 POST/DELETE」「Self 两扇 POST、零 GET/DELETE」「OR 五枚权限」「`profileType()==ENTERPRISE`」。
3. 打开 `EnterpriseModuleArchitectureTest.enforcesTheFiveLayerDependencyDirection`。圈 Controller 必须 import usecase、禁止 import service。再看企业 `service/`：没有 `EnterpriseMaterialServiceImpl`，没有材料 Mapper。
4. 打开 `EnterpriseMaterialService` 与 `EnterpriseProfileMaterialUseCaseImpl`。顺着 `accessUrlView` 投影 `EnterpriseProfileAccessUrl`；圈每个 UseCase 方法的 `@DSTransactional`；圈 default 那句「旧适配器不支持文件访问地址入口」被 Impl 覆盖。
5. 打开 `EnterpriseMaterialOwnerContributor` 与 `EnterpriseApplicationMapper.xml` 四条 lock + `countEditableMaterialWorkingOwner`（`DRAFT,BACK,CANCEL`）。对照 `EnterpriseMaterialOwnerContributorTest`：PERSON 钥匙进企业贡献者会抛错；SOURCE/VERSION 的 `applicantUserId` 为 null。
6. 打开 `SaTokenProfileMaterialAccessPolicy`。把 `prefix` 圈成 `profile:` + 小写 profileType。把 `requireAttach` 的 WORKING 申请人 vs SOURCE 管理员两条路标出来；圈 `requireWrite` **没有** override。
7. 打开 `50-cde-base-dml.sql` 企业三枚 TAG 与三条 `profile_material_requirement`。把 `document_type_code='*'` 和 `HANDLER_NOT_LEGAL_REPRESENTATIVE` 写在执照旁边。
8. 打开 `EnterpriseAdminController` 两条 archive access-url，以及前端 `enterprise/materials/service.ts` 的四枪。当作对照：门牌不同的不要画进本课格子。

## 总结、词汇表与下一步

- **宏观两扇窗：** 只读两扇在 `EnterpriseMaterialAdminController`，挂摘两扇在 `EnterpriseMaterialSelfController`。一块门牌 `/profile/enterprise/materials`，不是两块，也不是三块。
- **夹子不是箱子：** attach/detach 操作共享表 `profile_material_ref` 与 OSS 借出簿；不上传、不砸 MinIO。
- **四块板：** 只有 WORKING 可变；SOURCE 一挂即塑封；SUBMISSION/VERSION 靠无本课 HTTP 的 snapshot 复印。
- **两道权限：** 注解 OR + 策略按申请人/管理员再滤一遍。前缀是 `profile:enterprise`。Admin 类名不等于「申请人进不来」。
- **分层加电线：** Controller → 企业 `ProfileMaterialUseCase`（事务）→ 薄 `EnterpriseMaterialService` → `ProfileMaterialPort` → person `ProfileMaterialService`。企业贡献者只锁企业表。没有企业材料 Mapper。
- **失败关闭：** 没过策略就不向仓库要出门条；业务码走 person 包里的 `ProfileMaterialExceptionHandler` → `R.fail`，不要默认 4xx。
- **种子三枚牌：** 执照、法人证件永远要；授权书只在办理人不是法人时要。改牌子走 L-037。

词汇表：`EnterpriseMaterialAdminController` / `EnterpriseMaterialSelfController` / `ProfileMaterialUseCase` / `EnterpriseProfileMaterialUseCaseImpl` / `EnterpriseMaterialService` / `ProfileMaterialPort` / `MaterialOwnerKey` / `MaterialOwnerType` / `EnterpriseProfileAccessUrl` / `EnterpriseMaterialOwnerContributor` / `attach` / `detach` / `accessUrl` / `immutable_flag` / `active_owner_oss_key` / `reconcileReferences` / `ENTERPRISE_BUSINESS_LICENSE` / `HANDLER_NOT_LEGAL_REPRESENTATIVE` / `profile:enterprise:material` / layered。

下一步：档案审核窗与 archive 出门条是 OBJ-40。申请 current/submit 何时调用 `validateRequired` / `snapshotImmutable` 是 OBJ-41。厨房与 web-domain 把四扇窗接到页面是 OBJ-44。个人材料对称切片是 OBJ-37。OSS 直传与出门条规则分别是 OBJ-27 / OBJ-26。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / UseCase / `wta-api` | 两份公开入口与 `ProfileMaterialPort` | enterprise `controller/{admin,self}`；`wta-api` `profile.api.material` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-profile` = layered；按五层口述 | 登记表 layered 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 三张材料表、生成唯一列、企业种子树与必传规则、菜单 `profile:enterprise:material` | DDL `profile_material_*`；DML 约 1377–1422、1515 行 | 2026-09-16 |
| S-015 | `backend/wta-api/src/main/java/org/namewta/profile/api/material/` | 跨模块端口、owner 贡献者、快照关系只允许三种迁移 | `ProfileMaterialPort.java`；`ProfileMaterialOwnerContributor.java` | 2026-09-16 |
| S-L037-01 | `lessons/L-037-person-materials.md`；person 材料三份 Controller | 个人对称切片；分类牌全仓一份；企业 Self 有 `@Positive`、个人 AttachRequest 没有 | OBJ-37；person `AttachRequest` | 2026-09-16 |
| S-L040-01 | `EnterpriseAdminController.java`；`EnterpriseAdminService.reviewMaterial` / `material` / `create` | archive 出门条写死 SUBMISSION/VERSION；直建 SOURCE attach 不经本课 Controller | archive `@GetMapping` material access-url；`create` 循环 `materials.attach` | 2026-09-16 |
| S-L041-01 | `EnterpriseApplicationService.submit` | submit 调 `validateRequired(working, "*", qualifiers)` 与 `snapshotImmutable`；无材料 HTTP | `HANDLER_NOT_LEGAL_REPRESENTATIVE` 条件 | 2026-09-16 |
| S-L043-01 | `EnterpriseMaterialAdminController.java`；`EnterpriseMaterialSelfController.java` | 共用门牌、GET/POST 切开、写死 ENTERPRISE、attach body、detach 路径、权限串、日志不记体 | 类与四个公开方法；内部 `AttachRequest` | 2026-09-16 |
| S-L043-02 | `EnterpriseMaterialAdminControllerContractTest.java`；`EnterpriseMaterialSelfControllerContractTest.java`；`EnterpriseBeanValidationContractTest.java` | 两扇/两扇、无 DELETE、安全 `@Log`、OR 五枚、`@Positive` | 测试方法名 | 2026-09-16 |
| S-L043-03 | `usecase/ProfileMaterialUseCase.java`；`EnterpriseProfileMaterialUseCaseImpl.java`；`EnterpriseMaterialService.java` | 五层编排、`@DSTransactional`、薄转发、default `accessUrlView`、投影企业出门条 | UseCase 接口与实现；Service 全文转发 | 2026-09-16 |
| S-L043-04 | `EnterpriseMaterialOwnerContributor.java`；`EnterpriseProfileApiService.java`；`EnterpriseApplicationMapper.xml` | 两道权限的 owner 锁；WORKING 锁申请行；可编辑状态集合；SOURCE/VERSION 无申请人 | 四条 lock；`countEditableMaterialWorkingOwner` | 2026-09-16 |
| S-L043-05 | `SaTokenProfileMaterialAccessPolicy.java`；`ProfileMaterialService.java`；`ProfileMaterialExceptionHandler.java` | 前缀按 profileType；attach/detach/list 规则；Advice 罩 enterprise 包；`R.fail` | `prefix` / `requireAttach` / `requireWrite` / `requireRead`；Handler `basePackages` | 2026-09-16 |
| S-L043-06 | `EnterpriseMaterialOwnerContributorTest.java`；`EnterpriseModuleArchitectureTest.java`；`wta-profile-enterprise/pom.xml` | PERSON 钥匙拒绝；无材料 Mapper；Controller 不 import service；POM 不依赖 person | 测试方法；`<artifactId>wta-api</artifactId>` | 2026-09-16 |
| S-L043-07 | `frontend/packages/domains/profile/src/enterprise/materials/{service,index}.ts`；`service.test.ts`；`permissions.ts` | 厨房四枪 URL 与方法；资源标签名；权限字典（对照，不认 OBJ-44 格子） | `createEnterpriseMaterialService`；lifecycle 末枪 access-url | 2026-09-16 |
| S-L043-08 | `frontend/packages/api-contracts/generated/openapi.ts` 与 `openapi/current.json` | 生成文件有企业材料四条路径；current.json 无 `/profile/` | `attach_1` / `list_1` / `detach_1` / `accessUrl_1` vs 快照缺席 | 2026-09-16 |
