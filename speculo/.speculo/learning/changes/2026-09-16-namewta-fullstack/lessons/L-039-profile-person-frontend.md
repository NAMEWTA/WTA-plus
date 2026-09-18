---
lesson_id: L-039
objective_ids: [OBJ-39]
claimed_cells:
  - A:createProfileService
  - A:createProfileWebDomain person/self
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: kitchen-facade
    minutes: 9
  - segment: two-manifests-and-pages
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-L039-01, S-L039-02, S-L039-03, S-L039-04, S-L039-05, S-L039-06, S-L039-07, S-L039-08]
---

# Lesson 039：宏观同一厨房两份菜单——`createProfileService` 与 person / self / material-tag

## 学完你能做什么

打开厨房 `frontend/packages/domains/profile/src/service.ts` 的 `createProfileService`，再打开厅堂两份菜单工厂 `createProfileWebDomain` / `createProfileSelfWebDomain`，你能**口述浏览器怎么把个人档案纸条贴到墙上**，而不是把「档案」说成 system 资料页，也不是把 home 的三道菜说成厨房被裁过。

口试名单就是矩阵 **(a)** 两格，外加 OBJ-39 点名的三块 web-domain；符号以**磁盘**为准：

1. **`A:createProfileService`**（包 `@namewta/domain-profile`，实现在 `src/service.ts`）：工厂只认 `HttpClient`，返回冻住的 `ProfileService`。上面有三只抽屉：`materialTags`、`person`（`application` / `archive` / `materials` / `rebind`）、`enterprise`（`application` / `archive` / `materials` / `transfer`）。**不**自己画 Vue，**不**读 `Admin-Token` / `Home-Token`，**不**裁方法。admin-web 与 home-web **各调一次同一工厂**，对象方法全集一样。
2. **`A:createProfileWebDomain person/self`**（包 `@namewta/web-domain-profile`）：OBJ-39 的 web-domain 切片是 **person / self / material-tag**，不是矩阵那一行九个工厂。磁盘上管理端工厂是 `createProfileWebDomain`（id `web-domain-profile`），用户端工厂是旁边的 `createProfileSelfWebDomain`（id `web-domain-profile-self`）。本课要能拆开两份菜单，格子仍按 chain 写成 person/self；**不**把 `createSystemWebDomain` 那八个邻居标成 covered。

OBJ-39 还要你顺着三块页面 owner 把纸条贴到人脸上：

- **`./person`**：管理端三键 `profile/person/index`、`profile/person/detail`、`profile/person/review`。页面喊 `runtime.service.person.archive.*` 和 `runtime.service.materialTags.tree`。
- **`./self`**：用户端三键 `profile/center/index`、`profile/person/application`、`profile/enterprise/application`。个人认证页喊 `runtime.service.person.application.current/save/submit`。
- **`./material-tag`**：管理端一键 `profile/materialTag/index`。目录名是单数 `material-tag`，领域合同与 URL 仍是复数 `material-tags`。这是稳定别名，不是写错。

2026-09-16 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createSystemService.profile` / `SysProfileController` | **另一栋楼。** classic `wta-system` 账号资料页（昵称、改自己的登录密码）。home 路由 `/profile` 打的是档案楼 self 菜单 |
| home 的 `profileService` 没有 archive / rebind | **有。** 同一座厨房，方法全集在。产品差在 registry 只挂 self 三键 |
| 一份 `createProfileWebDomain` 同时给 home 用 | **home 调的是 `createProfileSelfWebDomain`。** admin 才 `selectedManifestIds: ['web-domain-profile']` |
| 页面自己拼 `/profile/person/archive` | **厨房才拼。** Vue 只喊 `runtime.service.person.archive.page(...)` |
| 一份 Java 类 `PersonMaterialController` | **没有这个 Java 类。** 前端资源标签 `profilePersonMaterialsResource.controller` 写成这个字符串；后端是 Admin + Self 两份类（L-037） |
| 管理端新建档案会调用 `person.materials.attach` | **2026-09-16 的 Vue 不喊这四枪。** 直传走 `runtime.fileUpload`，材料数组塞进 `person.archive.create` |
| `@namewta/domain-profile-person` | **没有这个包。** 厨房一个包，子路径 `./person/archive` 等 |

本课**不宣称**你会拆 `PersonAdminController` 十二扇（L-034）、申请 current/submit 五层（L-035）、换绑五枪（L-036）、材料 attach/detach 与标签树 HTTP（L-037）、核身匿名回调（L-038），或把企业 archive/transfer 页面再讲一遍（L-044）。今天只认：**浏览器这一头的聚合厨房、两份菜单工厂、person/self/material-tag 三块门脸，以及厨房有枪但页面还没扣扳机的那些备用抽屉。**

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const profileService = createProfileService(domainHttp)`。L-008 对照过 home 也插同一座工厂，墙上却是 self 三键。L-004 把方向钉成 `App → web-domain → domain → platform`。L-034 … L-037 是后端四扇门。本课站在**已经登录的浏览器**这一头：管理员拿 `Admin-Token` 进档案柜台，门户用户拿 `Home-Token` 进档案中心。

三条河都叫 profile，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `SysProfileController` / `views/system/user/profile` | system 账号资料页（L-022） | 邻居：不要走错楼 |
| `createProfileService` | 浏览器档案厨房：个人 + 企业 + 标签树全部 HTTP 纸条 | **本课格子** |
| `createProfileWebDomain` | 管理端菜单：材料标签 + 个人三页 + 企业三页 | **本课认 person + material-tag**；企业三页是 L-044 |
| `createProfileSelfWebDomain` | 用户端菜单：档案中心 + 个人认证 + 企业认证 | **本课认 self**；企业认证表单字段留给 L-044 |
| `Person*Controller` / `MaterialTagController` | 后端 layered 五层 | L-034 … L-037 |
| `PersonVerificationAnonymousController` | `@SaIgnore` 供应商回调 | L-038；**没有**前端资源目录 |

2026-09-16 工作树：权威厨房是 `frontend/packages/domains/profile/`（根 facade + 九个资源子路径）。权威菜单包是 `frontend/packages/web-domains/profile/`（`src/person/`、`src/self/`、`src/material-tag/`、`src/enterprise/`、两份 runtime）。厅堂接线是 `apps/admin-web/src/application/services.ts` 第 53 行与 `apps/home-web/src/application/services.ts` 第 16 行。菜单种子在 `50-cde-base-dml.sql`：管理端 `profile/person/index`、`profile/materialTag/index`；用户端 `profile/center/index`、`profile/person/application`。

```text
已登录的人
        │
        ├─ admin-web（Admin-Token）
        │     profileService = createProfileService(domainHttp)     ← 厨房全集
        │     createProfileWebDomain(adminProfileWebRuntime)
        │           id = web-domain-profile
        │           键：materialTag/index + person/{index,detail,review}
        │               + enterprise/{index,detail,review}          ← L-044
        │
        └─ home-web（Home-Token）
              profileService = createProfileService(domainHttp)     ← 还是全集
              createProfileSelfWebDomain({ service: profileService, ... })
                    id = web-domain-profile-self
                    键：center/index + person/application
                        + enterprise/application                    ← 注册在 self；表单 L-044
```

往下走不要跳层：

```text
Vue 页（只收 runtime）
    └─ web-domain 外包一层 h(page, { runtime })
          └─ domain 工厂拼 URL / method / encodeURIComponent
                └─ App 的 domainHttp（axios + 各厅堂 Token）
                      └─ 后端 Person* / MaterialTag Controller（L-034…L-037）
```

**类比失效边界：** 「同一厨房两份菜单」**不**等于「home 的 HTTP 客户端会拒绝 archive 请求」。厨房对象上的函数还在；只是墙上没挂那道菜。谁在页面里喊了 `profileService.person.archive.page`，卡车照样开。产品闸在 **manifest 选哪些 componentKey**，不在工厂里 `delete` 方法。

## 核心概念与机制

### 直觉讲解

把 `createProfileService` 想成**中央厨房的点菜单**，不是餐厅大厅。

- **纸条（resource service）**：每一张纸条写死门牌和动词。`createPersonArchiveService` 写 `/profile/person/archive`；`createMaterialTagService` 写 `/profile/material-tags`。厨师（页面）只喊「来一份 page」，不自己拿笔改路径。
- **总菜单（`createProfileService`）**：把九张纸条订成一本。封面还留了旧写法：`person.application.probeRebind` 其实就是旁边那张 `person.rebind.probe` 的**同一只手**，不是第二份拷贝。
- **大厅挂牌（web-domain）**：管理员大厅挂七块牌（本课认四块：标签 + 个人三页）。用户大厅挂三块牌（本课认档案中心 + 个人认证）。挂牌的人是 App：缺 runtime 就**当场关门**，不会先画出一个空壳再报错。
- **厅堂托盘（runtime）**：确认框、成功/失败提示、权限尺、找人、下载出门条、办工作流、上传控件，都是 App 塞进来的。页面包不拥有 Router 单例，也不拥有 Token 抽屉。

**类比失效边界：** 点菜单类比**不**覆盖「照片字节怎么进 MinIO」。上传控件是 L-029 的 `FileUpload`，经 `runtime.fileUpload` 注入。本课厨房的 `person.materials.attach` 只夹已经进仓的 `ossId`。类比也**不**等于「用户大厅那本总菜单被撕掉了管理页」——home 的 `profileService.person.archive` 还在。类比还不等于「两份菜单工厂可以混用」：admin 的 `requireProfileWebRuntime` 要 `fileUpload` 和 archive 十一枪；self 的 `requireProfileSelfWebRuntime` 只检查 runtime 不是 `undefined`。把 admin runtime 塞进 self 工厂、或反过来，口试先数校验函数。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 档案领域厨房 | `createProfileService` / `ProfileService` | `packages/domains/profile/src/service.ts`；包 `@namewta/domain-profile` |
| 兼容门面 | compatibility facade | 根 `index.ts` re-export 工厂与类型；新代码优先子路径 `./person/archive` |
| 资源标签 | resource metadata | 如 `profilePersonArchiveResource = { controller: 'PersonAdminController', basePath: '/profile/person/archive' }` |
| 管理端菜单工厂 | `createProfileWebDomain` | `web-domains/profile/src/index.ts`；manifest id `web-domain-profile` |
| 用户端菜单工厂 | `createProfileSelfWebDomain` | `src/self/registration.ts`；manifest id `web-domain-profile-self` |
| 页面贡献 | web contribution | `createPersonWebContribution` / `createMaterialTagWebContribution`；权限数组 + registrations |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对表的字符串，如 `profile/person/index` |
| 运行时托盘 | `ProfileWebRuntime` / `ProfileSelfWebRuntime` | 管理端要 host 九口 + `fileUpload` + 厨房；用户端六口，无上传/工作流/找人 |
| 失败关闭 | fail-closed | `requireProfileWebRuntime` 缺方法就扔 `'ProfileWebRuntime is required'` |
| 权限字典 | `profilePermissions` | `src/permissions.ts`；页面用它，不在 Vue 里发明第二份串 |
| 领域模块名片 | `profileDomainModule` | `id: 'profile'`，`backendModules: ['wta-profile']`，九项 capabilities |
| 路径编码 | `encodeURIComponent` | 厨房 `segment()`；测试里 `'app/1'` 变成 `app%2F1` |
| 隐私投影 | privacy projection | 仅 `rebind.probe` / `rebind.match` 走 `projectStatusProbeResponse` / `projectPersonMatchResponse` |
| 页面 owner 别名 | `material-tag` vs `material-tags` | 目录与 export `./material-tag`；HTTP `/profile/material-tags` |

厨房公开子路径（`package.json` `exports`）与 Controller 门牌对齐：

| 子路径 | basePath | 本课页面用不用 |
| --- | --- | --- |
| `./material-tags` | `/profile/material-tags` | **用。** 标签页五枪；个人新建档案拉 PERSON+COMMON 树 |
| `./person/application` | `/profile/person/application` | **用。** self 个人认证三枪 |
| `./person/rebind` | `/profile/person/rebind` | 厨房有五枪 + 旧名挂在 `application.*Rebind`。**2026-09-16 无 Vue 调用** |
| `./person/materials` | `/profile/person/materials` | 厨房有 list/attach/detach/accessUrl。**2026-09-16 无 Vue 调用** |
| `./person/archive` | `/profile/person/archive` | **用。** 管理端名册/详情/审核/直建 |
| `./enterprise/*` | `/profile/enterprise/*` | 厨房订进总菜单；页面 L-044。self 仍注册企业认证键 |

`profileDomainModule.capabilities` 九项：`material-tag`、`person-application`、`person-rebind`、`person-material`、`person-archive`、以及对称四项 enterprise。capability 名片**不是** componentKey，也不决定 App 挂哪几页。

### 机制/因果链

**1. 工厂只订书，不裁页。**

`createProfileService(http)` 内部依次 `createPersonApplicationService`、`createPersonRebindService`、再 `Object.freeze` 出带旧名的 `person.application`。`probeRebind` **就是** `personRebind.probe`（`===`，测试锁死）。企业 `application.sendTransfer` 同理指向 `transfer.send`。最后冻住三只抽屉返回。没有 `if (appId === 'home-web')` 这种分支。

admin 与 home 都是：

```ts
export const profileService = createProfileService(domainHttp);
```

`domainHttp` 只是把各厅堂 axios `request` 包一层，避免初始化环。Token 在 HTTP 适配器里，不在厨房。

**2. 管理端菜单工厂把三份贡献拼成一本，缺托盘就关门。**

`createProfileWebDomain` 先 `requireProfileWebRuntime`。校验清单是写死的：

- host 九口必须是函数：`closeCurrentPage`、`completeWorkflowTask`、`confirm`、`downloadMaterial`、`error`、`findUsers`、`hasPermission`、`success`、`warning`
- 必须有 `fileUpload` 组件
- `service.materialTags` 五口：`archive` / `changeStatus` / `create` / `tree` / `update`
- `service.person.archive` **十一**口：`assign` `create` `decide` `detail` `manageBinding` `material` `page` `review` `reviewMaterial` `revise` `revoke`
- `service.enterprise.archive` 同样十一口

**没有**把 `eligibleUsers` 写进校验数组。找人走 App 的 `runtime.findUsers`，厅堂再转 `archive.eligibleUsers`。口试不要说「runtime 校验了 eligibleUsers」。

拼出来的 registrations **固定七行**（测试锁死顺序）：

1. `profile/materialTag/index` → `ProfileMaterialTag`
2. `profile/person/index` → `PersonProfile`
3. `profile/person/detail` → `PersonProfileDetail`
4. `profile/person/review` → `PersonProfileReview`
5. 企业三键（L-044）

权限目录是 `profilePermissions.materialTag` 两串 + `person` 六串 + `enterprise` 六串，全部摊平。页面按钮另用 `runtime.hasPermission(...)` 做当前用户投影；**后端仍是最终授权者**。

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-profile` 时才把键交给导航。测试：空 selected 的 fixture App `componentKeys()` 是 `[]`。

**3. 用户端菜单工厂是另一份 id，校验更瘦。**

`createProfileSelfWebDomain` 的 `permissions` 是 **空数组**。不是「用户没有权限」，是这份 manifest 不往平台权限目录里登记。中心页按钮写死 `runtime.hasPermission('profile:person:apply')` 字符串。种子菜单 `sys_menu.perms` 也是 `profile:person:apply`。

self runtime **没有** `fileUpload` / `findUsers` / `completeWorkflowTask` / `downloadMaterial` / `closeCurrentPage`。`requireProfileSelfWebRuntime` 只判断 `if (!runtime) throw`。测试里甚至可以塞 `service: {} as ProfileService` 就通过注册。口试：self 工厂**不**在创建时核对厨房方法；页面一喊缺失方法会在点击时炸，不是在挂牌时炸。

三键：`profile/center/index`、`profile/person/application`、`profile/enterprise/application`。home `selectedManifestIds` 是 `web-domain-profile-self`，**不是** `web-domain-profile`。登录成功 `onAuthenticated` 把人送到 `` `${VITE_APP_CONTEXT_PATH}profile` ``。

**4. 管理端个人页走 archive，不走 materials 四枪。**

`PersonProfilePage`：`archive.page` 拉名册；新建时 `materialTags.tree('PERSON')` 与 `tree('COMMON')` 拼下拉，`flattenPersonMaterialOptions` 只收启用中的 `TAG` 且 scope 是 PERSON 或 COMMON；`runtime.fileUpload` 把 `ossId` 写进行；确认后 `archive.create({ identity, bindUserId, reason, materials })`。测试禁止源码出现 `materials: []`。单文件 10MB、最多 10 份、类型白名单在页面：`png/jpg/jpeg/pdf/doc/docx`。这是 UI 闸；后端材料闸仍是 L-037。

`PersonProfileDetailPanel`：`archive.detail`；下载走 `archive.material`（archive 门牌上的 access-url，L-034），再 `runtime.downloadMaterial` 造 `<a download>`。高风险动作先 `confirm` + 原因：`revise` / `assign` / `manageBinding` / `revoke`。`personActionMatrix`：档案 `REVOKED` 则四钮全关；`manageBinding` 还要存在 `ACTIVE` 或 `SUSPENDED` 绑定，且持 `profile:person:manage`。`safeErrorMessage` 吞掉证件号，只回「操作失败，请刷新后重试」。

`PersonProfileReviewPage`：`archive.review` 打的是 `GET .../application/{id}/review-context`（Java 方法 `reviewContext`，厨房名 `review`）。材料预览 `archive.reviewMaterial`。两条提交河：

| 按钮 | 前端调用 | 磁盘含义 |
| --- | --- | --- |
| 提交流程审核 | `runtime.completeWorkflowTask({ taskId, comment, variables: { profileDecision: 'APPROVE'\|'REJECT' } })` | 厅堂转 `workflowService.completeTask`；**不**打 archive `/decision` |
| 管理员覆盖决定 | `archive.decide(..., { decision: 'APPROVED'\|'REJECTED', reason })` | 才是 L-034 那扇 POST。UI 的 APPROVE 要映射成 APPROVED |

缺 `query.taskId` 时流程按钮只 `warning`，不发 HTTP。

`PersonProfileDetailPage` 是薄壳：`route.query.id` 交给同一份 Panel。列表页抽屉也复用 Panel。detail 键给工作流/深链，index 抽屉给柜台点开。

**5. 用户端个人认证只打 application 三枪。**

`PersonVerificationPage`：`onMounted` → `person.application.current()`。保存 `save({ ...identity, expectedVersion })`。提交先 `confirm`，再 **先 save 再 `submit(version)`**，版本取 save 返回的 `data.version`。没有材料行，没有 `attach`，没有 rebind。状态文案：`DRAFT` 草稿、`BACK` 已退回、`WAITING` 审核中、`FINISH` 已完成。

`ProfileCenterPage`：路径是 `/profile` 时展示两张卡片；`router.push('/profile/person')` 与 `/profile/enterprise`。子路由 `<router-view />`。权限串写死在模板，没用 `profilePermissions.person.apply` 常量——字典仍在厨房里，这页抄了字面量。

**6. 标签页吃厨房五枪，树规则在 logic.ts。**

`MaterialTagPage`：`materialTags.tree(scope, includeDisabled)`，scope 三段 `PERSON` / `ENTERPRISE` / `COMMON` 仍是**这一页**，不是企业档案页。`create` / `update` / `changeStatus` / `archive` 都走厨房 POST。`allowedChildTypes`：一层 CATEGORY 可挂 CATEGORY 或 TAG；二层只能 TAG；TAG 不能再挂。`systemRequired` 锁编码与启停/归档。`executeMaterialCommand` 失败不 reload，冲突时树上还是旧节点。

**7. 隐私投影只罩探测，不罩档案详情。**

`rebind.probe` / `match` 用 `http.request<unknown>` 再投影。普通 probe 的 `data` **只许** `{ status }`；多 `profileId` / `fullName` / `documentNumber` / `phone` 就扔「Profile 响应不可用」。match 只许多一个 `maskedPhone` 字符串或 null，再多 `userId` 同样关闸。`archive.detail` 等枪**不**走这套投影，直接 `ApiResponse<T>`。厨房把 HTTP 层错误原样 `rejects.toBe(forbidden)`，不把 403 翻译成空名册。

**8. OpenAPI 快照与 README 不要背反。**

`domains/profile/README.md` 仍写「当前提交的 OpenAPI 快照尚无 `/profile/**`」。2026-09-16 工作树 `frontend/packages/api-contracts/generated/openapi.ts` **已经**有 `/profile/person/archive`、`/profile/material-tags/tree` 等路径。口试以生成文件为准，并补一句：页面与 domain service **仍不** import generated 类型；URL 合同的测试锁在 `service.test.ts`。匿名回调路径在快照里，前端**没有**对应资源目录。

### 图、表或文本图

```text
 sys_menu.component                 种子
   profile/materialTag/index        管理端 Client
   profile/person/index
   profile/person/review            工作流表单键
   profile/center/index             home Client
   profile/person/application
        │
        v
 GET /system/menu/getRouters        已按 Client + 权限裁剪
        │
        v
 App composeAppRuntime
   admin  selected: web-domain-profile
   home   selected: web-domain-profile-self
        │
        ├─ 键在已选 manifest → registration.load
        │     defineComponent({ setup: () => () => h(Page, { runtime }) })
        └─ 键不在已选清单 → 解析失败关闭，不会 glob 扫 views/
                │
                v
         Page props.runtime
                │
                ├─ runtime.service = createProfileService(...)   厨房全集
                ├─ runtime.hasPermission / confirm / error
                ├─ admin 另有 fileUpload / findUsers / completeWorkflowTask / downloadMaterial
                └─ 页面只喊方法名，不拼 URL
```

**图题 / caption：** 宏观同一厨房、两份已选菜单。alt：菜单键来自 sys_menu；admin 与 home 选不同 manifest id；页面只收到 runtime；HTTP 字符串在 domain 工厂。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。页面向厨房喊方法。厨房把方法换成 GET/POST 和 `/profile/...`。home 即使厨房里有 archive，墙上没有 `profile/person/index`，导航就不会挂那页。

第二张表把「厨房有枪 / 本课页面扣扳机」钉死：

| 厨房方法 | HTTP（动词以测试为准） | 2026-09-16 person/self/material-tag 页面 |
| --- | --- | --- |
| `materialTags.tree/create/update/changeStatus/archive` | GET tree；其余 POST | **MaterialTagPage** 五枪；**PersonProfilePage** 只 tree |
| `person.archive.page/create/detail/material/review/reviewMaterial/revise/assign/manageBinding/revoke/decide` | GET 名册/详情/审核/出门条；POST 直建/修订/绑定/注销/决定 | **PersonProfile\*** 使用。`eligibleUsers` 不直接喊，经 `findUsers` |
| `person.archive.eligibleUsers` | GET `.../eligible-users` | 厅堂 `adminProfileWebRuntime.findUsers('PERSON', keyword)` |
| `person.application.current/save/submit` | GET 根；POST 根；POST `/submit` | **PersonVerificationPage** |
| `person.application.*Rebind` / `person.rebind.*` | 五枪全 POST `/profile/person/rebind/...` | **无 Vue 调用** |
| `person.materials.list/attach/detach/accessUrl` | GET list 与 access-url；POST attach 与 `.../detach` | **无 Vue 调用**。下载走 `archive.material` / `reviewMaterial` |
| `enterprise.*` | `/profile/enterprise/**` | 厨房有；本课页面不认（L-044）。self 只注册企业认证键 |

**图题 / caption：** 厨房方法全集不等于已挂页面的调用集。alt：标签与 archive 与 application 三枪有页面；rebind 与 person.materials 四枪目前只有测试。

### 正例、反例与边界

**正例 1 — 管理员翻个人名册。** 有 `profile:person:query` 的人打开菜单 `profile/person/index`。页 `archive.page({ fullName, documentNumber, status, pageNum, pageSize })`。厨房 `GET /profile/person/archive`。空状态筛选项会把 `status` 收成 `undefined`，不传空串当枚举。

**正例 2 — 管理员直建一本档案。** 持 `profile:person:override` 才见「新建档案」。检索账户时 `runtime.findUsers('PERSON', 'ali')` → 厅堂 `person.archive.eligibleUsers('ali')` → `GET /profile/person/archive/eligible-users?keyword=ali`。上传控件 `v-model` 得到 `ossId`。`create` POST `/profile/person/archive/admin-create`，body 带 `identity`、`bindUserId`、`reason`、`materials: [{ materialNodeId, ossId }]`。**没有** `POST /profile/person/materials/WORKING/...`。

**正例 3 — 审核员看申请袋。** 工作流表单键 `profile/person/review`，query 带 `id` 与 `taskId`。`archive.review` → `GET .../application/{id}/review-context`。点材料「查看」→ `archive.reviewMaterial` → 厅堂造下载链。点「提交流程审核」只 `completeWorkflowTask`。点「管理员覆盖决定」才 `decide`，且 `APPROVE` 变成 `APPROVED`。

**正例 4 — 改分类牌。** 打开 `profile/materialTag/index`，切到「个人」，不勾显示停用。`GET /profile/material-tags/tree?scope=PERSON&includeDisabled=false`。在一层分类下新增 TAG，编码可填；系统必传行开关禁用。归档 POST `.../{id}/archive`，body `{ expectedVersion }`。

**正例 5 — 门户用户交个人认证。** home 登录后进 `/profile`。有 `profile:person:apply` 才见「开始认证」，跳 `/profile/person`。`GET /profile/person/application`。填七个身份字段，保存 POST 根；提交先 save 再 POST `/profile/person/application/submit`，`data: { expectedVersion }`。

**正例 6 — 斜杠编号。** 测试 `archive.review('app/1')` 的 URL 是 `/profile/person/archive/application/app%2F1/review-context`。`materialTags.update('tag/1', ...)` 是 `/profile/material-tags/tag%2F1`。不要把斜杠当多一级路径。

**正例 7 — 旧名与新名是同一只手。** `service.person.application.probeRebind === service.person.rebind.probe`。新页面应喊 `person.rebind`；旧调用面仍打同一 URL。

**反例 1 — 「home 的 profileService 没有 archive。」** 对象上有。L-008 已经挖过这句。本课再钉：差在 `createProfileSelfWebDomain` 的三键，不在工厂。

**反例 2 — 「资料页 `/user/profile` 就是档案楼。」** admin 个人中心 hidden 路由是 system 资料页（L-022）。档案管理在动态菜单 `profile/person/index`。home 的 `/profile` 是 self 档案中心。

**反例 3 — 「页面 attach 材料。」** 全仓 Vue 搜不到 `person.materials.attach`。直建走 `archive.create.materials`；自助认证页目前连材料行都没有。

**反例 4 — 「审核通过 = `decide({ decision: 'APPROVE' })」。** 覆盖决定要 `APPROVED` / `REJECTED`。流程变量才用 `APPROVE` / `REJECT`。混用会把错码送给后端。

**反例 5 — 「self manifest 登记了 `profilePermissions`。」** `permissions: Object.freeze([])`。中心页自己查 `hasPermission`。

**反例 6 — 「`createProfileWebDomain(undefined)` 会给出空菜单。」** 立刻 throw。material-tag / person / enterprise 三份 contribution 同样 fail-closed。

**反例 7 — 「前端资源名 `PersonMaterialController` 对应一个 Java 类。」** 只是 domain 标签。后端两份类共用门牌（L-037）。

**反例 8 — 「厨房会 HMAC / 会读 Token。」** 只收 `HttpClient`。登录票在 App HTTP。

**反例 9 — 「注销是 DELETE，导出在 archive.export。」** 测试：方法不是 get/post 的枪不存在；URL 不含 `export`。注销是 `POST .../revoke`。

**反例 10 — 「把九个 web-domain 工厂一行标 covered。」** GP-L-020 警告过。本课只给 profile 的 person/self/material-tag 切片当证据。

**边界 1 — runtime 校验名单 ≠ 厨房方法全集。** 管理端校验不看 `person.materials`、不看 `rebind`、不看 `eligibleUsers`、不看 `application`。缺这些方法仍能通过 `createProfileWebDomain`，页面一喊才会在运行时失败。

**边界 2 — 自助页没有材料 UI ≠ 后端不校验必传。** 提交后 ALWAYS 材料闸是 L-035 / L-037。本课只陈述：self 个人页 2026-09-16 不调用 attach。缺图时失败发生在后端，不是厨房少枪。

**边界 3 — `PersonProfilePage` 的文件类型含 doc/docx。** 材料总库白名单是 jpg/jpeg/png/pdf（L-037）。直建走 archive 门，两套闸不要背成一句。

**边界 4 — 标签页能切 ENTERPRISE scope。** 那仍是 material-tag owner，HTTP 仍打 `/profile/material-tags`。不要说「切到企业就是 L-044 的企业档案页」。

**边界 5 — self 第三键是企业认证。** 工厂注册属于本课 self 切片；表单字段与 `enterprise.application` HTTP 留给 L-044。口试要能指键名，不要把企业身份页讲成个人七字段。

**边界 6 — `downloadMaterial` 信任厨房返回的 `url`。** 厅堂用 `<a href={access.url} download={fileName}>`。授权在后端签发出门条；前端不二次鉴权。

**边界 7 — 匿名回调无页面。** README：无页面的匿名验证不建空目录。不要为 `PersonVerificationAnonymousController` 找 `src/anonymous/`。

**边界 8 — 字典硬编码。** 个人认证页的证件类型七选项写在 Vue 里，不是 `runtime.dicts`。system 字典合同（L-021）不要套到这张表单上。

## 变式与迁移

1. **和 L-033 `createOpenApiService` 对照。** OpenAPI 是 system 包里另一座厨房，两扇柜台 `currentUser` / `targetUser`。Profile 是独立包 `@namewta/domain-profile`，按**资源门牌**切抽屉，不是按「自己/别人」切。不要在 `systemService` 上找 `profile`。
2. **和 L-008 对照。** home 接线板已经点过「身份 + 资料」。本课把资料那头拆开：同一 `createProfileService`，不同 web-domain 工厂，不同 runtime 校验强度。
3. **和 L-020 对照。** `createSystemWebDomain` 是 15 键菜谱板。本课管理端工厂 7 键，用户端工厂 3 键。导航 host 仍是 `getRouters` → `resolve*Registration` → `addRoute`。profile 不另写一套路由恢复。
4. **和 L-029 对照。** 新建档案的字节走 `runtime.fileUpload`（厅堂 `FileUpload` → `ossUploadClient.upload`）。厨房 `attach` 只夹 `ossId`。现在直建甚至不喊 attach，把 `ossId` 放进 `archive.create`。
5. **和 L-034 / L-037 对照。** 出门条有两扇门牌：archive 上的 `.../material/{ref}/access-url`（本课页面在用），材料总库上的 `.../materials/{ownerType}/{ownerId}/{ref}/access-url`（厨房有，页面没用）。挂摘四枪同样：厨房有，管理端 Vue 没用。
6. **以后若自助页要补证件照片。** 应喊 `runtime.service.person.materials.attach('WORKING', applicationId, { ossId, materialNodeId })`，并给 self runtime 补上传口；不要在 Vue 里手写 `/profile/person/materials`。那是产品增量，不是本课缺口补丁。
7. **以后若换绑要有页面。** 厨房五枪与隐私投影已经在。应走 `person.rebind.*` 子路径，不要继续往 `application` 堆旧名。
8. **换 App。** 第三份 App 若只要标签树，仍须 `createProfileService` + 一份只选 material-tag contribution 的 manifest。今天的 `createProfileWebDomain` **会**把 person/enterprise 七键一起注册；不能靠「只用标签页」让工厂少返回四键。要瘦菜单，得新写 contribution 组合，或让 App 不选那些菜单种子。
9. **迁移口诀：** 先数两座工厂两个 manifest id → 再数厨房三只抽屉方法全集 → 再数墙上已挂 componentKey → 最后数页面真正喊出的方法。跳步会出现「把 home 说成裁过的服务」「把 attach 说成新建档案的那一枪」「把 self 空 permissions 说成用户没有 apply 权」。

## 常见误区

1. **「OBJ-39 包含 `PersonAdminController`。」** 那是 OBJ-34。本课认前端工厂与三块页面 owner。
2. **「`createProfileWebDomain` 等于 `createProfileSelfWebDomain`。」** id、键、runtime、权限目录、校验强度全不同。
3. **「material-tag 目录名写错了，应该改成 material-tags。」** README 写明这是稳定页面 owner 别名。组件键保持 `profile/materialTag/index`。
4. **「厨房会按 App 裁掉 archive。」** 不会。
5. **「详情下载走 `person.materials.accessUrl`。」** 走 `person.archive.material`。
6. **「流程审核按钮打 `/decision`。」** 默认走 `completeWorkflowTask`。覆盖决定才 `decide`。
7. **「`review` 的 URL 是 `/review`。」** 是 `/review-context`。
8. **「self 校验和 admin 一样严。」** self 只拒 `undefined`。
9. **「前端授权。」** `hasPermission` 藏按钮；HTTP 仍可能 403。厨房把错误原样抛出。
10. **「OpenAPI 还没有 `/profile`，所以 URL 是临时的。」** 生成快照 2026-09-16 已有路径；页面仍不引用 generated。以 service 测试为准。
11. **「`capabilities` 有 person-rebind 所以一定有换绑页。」** 名片有能力，manifest 可以不挂页。今天就是这样。
12. **「`PersonProfilePage` 的 doc 白名单 = 材料总库白名单。」** 不是同一扇门。
13. **「中心页 import 了 `profilePermissions`。」** 没有。模板写字面量 `'profile:person:apply'`。
14. **「本课覆盖企业 archive 十二枪页面。」** L-044。本课只承认管理端工厂会**一并注册**那三键，以及 self 会注册企业认证键。
15. **「匿名核身也有一张 Vue。」** 没有。L-038。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/domains/profile/src/service.ts`。圈 `createProfileService`、三只冻住的抽屉、`probeRebind: personRebind.probe` 这种别名。打开 `permissions.ts` 与根 `index.ts` 的 `profileDomainModule.capabilities`。
2. 打开 `person/archive/service.ts`、`person/application/service.ts`、`person/rebind/service.ts`、`person/materials/service.ts`、`material-tags/service.ts`。顺着方法把 URL 与 `get`/`post` 点完。圈 `segment = encodeURIComponent`。打开 `service.test.ts` 对一下 `%2F` 与「无 export」。
3. 打开 `person/rebind/transport.ts` 与 `transport.test.ts`。圈 probe 只许 `status`、match 只许多 `maskedPhone`。
4. 打开 `web-domains/profile/src/index.ts` 与 `runtime.ts`。圈七键顺序、`archiveMethods` 十一项（没有 `eligibleUsers`）、缺 runtime 的 throw。打开 `src/self/registration.ts`：三键、空 `permissions`、id `web-domain-profile-self`。
5. 打开 `apps/admin-web/src/application/services.ts` 第 53 行与 `router/adminManifestRegistry.ts` 的 `adminProfileWebRuntime`。圈 `fileUpload`、`findUsers` 转 `eligibleUsers`、`completeWorkflowTask` 转 `workflowService.completeTask`、`downloadMaterial` 的 `<a>`。打开 home 的 `services.ts` 第 16 行与 `homeManifestRegistry.ts` 的 `selectedManifestIds`。
6. 打开 `PersonProfilePage.vue` / `PersonProfileReviewPage.vue` / `PersonVerificationPage.vue` / `MaterialTagPage.vue`。圈各自真正喊出的 `runtime.service.*`。在 web-domains 里搜 `person.materials` 与 `person.rebind`，确认不是页面调用。

## 总结、词汇表与下一步

- **宏观同一厨房两份菜单：** `createProfileService` 订出个人/企业/标签全部纸条。admin 挂 `web-domain-profile`（本课认 material-tag + person 三页）。home 挂 `web-domain-profile-self`（本课认中心 + 个人认证）。厨房方法全集 ≠ 墙上的菜 ≠ 页面已扣的扳机。
- **(a) `createProfileService`：** 只吃 `HttpClient`，冻住 facade，GET 读 POST 写，路径编码，rebind 探测失败关闭，错误原样抛。
- **(a) person/self 切片：** 管理端工厂 7 键里的 4 键 + 用户端工厂 3 键里的个人两键；标签页是同一管理端工厂的 material-tag 贡献。九厂矩阵行不因本课整行闭合。
- **页面实际调用：** 管理端个人走 `person.archive` + 标签 `tree`；审核分流工作流 / `decide`；自助走 `person.application` 三枪。`rebind` 与 `person.materials` 四枪目前只有测试锁 URL。
- **不是 system 资料页，不是后端五层复述。** Token、菜单恢复、OSS 直传、Java Controller 分给邻居课。

词汇表：`createProfileService` / `ProfileService` / `createProfileWebDomain` / `createProfileSelfWebDomain` / `ProfileWebRuntime` / `ProfileSelfWebRuntime` / `componentKey` / `profilePermissions` / `profileDomainModule` / `material-tag` / `material-tags` / `person.archive.review` / `completeWorkflowTask` / `eligibleUsers` / `findUsers` / fail-closed runtime / compatibility facade / privacy projection。

下一步：管理端档案十二扇与决定码是 OBJ-34。自助 current/submit 五层与材料闸是 OBJ-35。换绑五枪是 OBJ-36。材料总库 attach/detach 是 OBJ-37。核身回调是 OBJ-38。企业页面如何消费同一厨房是 OBJ-44。厅堂插头是 OBJ-07 / OBJ-08。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web}` | 两厅堂都 `createProfileService(domainHttp)`；admin 选 `web-domain-profile`，home 选 `web-domain-profile-self` | `application/services.ts`；`adminManifestRegistry.ts`；`homeManifestRegistry.ts` | 2026-09-16 |
| S-006 | `frontend/packages/{domains,web-domains}/profile` | 厨房 facade、九个子路径、两份菜单工厂、runtime 校验差 | 各包 `package.json` `exports` 与 `src` | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 菜单键 `profile/person/index`、`profile/materialTag/index`、`profile/center/index`、`profile/person/application`；工作流表单 `profile/person/review` | 档案菜单与流程节点行 | 2026-09-16 |
| S-L039-01 | `packages/domains/profile/src/service.ts`；`service.test.ts`；`permissions.ts`；`index.ts` | 三抽屉；旧名 `===` 新名；URL 表；无 export；403 原样拒绝；capabilities 九项 | `createProfileService`；四个 `it` | 2026-09-16 |
| S-L039-02 | `person/{archive,application,rebind,materials}/service.ts`；`material-tags/service.ts`；各资源 `index.ts` 的 controller 字符串 | 十二枪 archive、三枪 application、五枪 rebind、四枪 materials、五枪 tags；`PersonMaterialController` 只是标签 | `base` 常量；`profilePersonMaterialsResource` | 2026-09-16 |
| S-L039-03 | `transport.ts`；`person/rebind/transport.ts`；`transport-support.ts`；`transport.test.ts` | probe/match 投影失败关闭；只允 maskedPhone | `strictProfileRecord`；两个 `it` | 2026-09-16 |
| S-L039-04 | `web-domains/profile/src/index.ts`；`runtime.ts`；`index.test.ts` | 七键顺序；缺 runtime throw；权限摊平 materialTag+person+enterprise；未选 manifest 无键 | `createProfileWebDomain`；`archiveMethods` | 2026-09-16 |
| S-L039-05 | `src/person/registration.ts`；`PersonProfilePage.vue`；`PersonProfileDetailPanel.vue`；`PersonProfileReviewPage.vue`；`logic.ts`；`components.test.ts` | 三键；page/create/detail/material/review/decide；fileUpload；APPROVE→APPROVED；REVOKED 只读 | 模板与 script；三个 `it` | 2026-09-16 |
| S-L039-06 | `src/self/registration.ts`；`registration.test.ts`；`runtime.ts`；`ProfileCenterPage.vue`；`PersonVerificationPage.vue` | self 三键；空 permissions；current/save/submit；中心页字面量 apply | `createProfileSelfWebDomain` | 2026-09-16 |
| S-L039-07 | `src/material-tag/registration.ts`；`MaterialTagPage.vue`；`logic.ts`；`logic.test.ts` | 键 `profile/materialTag/index`；五枪；树深度与 systemRequired | `createMaterialTagWebContribution` | 2026-09-16 |
| S-L039-08 | `apps/admin-web/src/router/adminManifestRegistry.ts` `adminProfileWebRuntime`；`generated/openapi.ts` `/profile/**`；`domains/profile/README.md` | findUsers→eligibleUsers；completeTask 接线；快照已有 /profile 与 README 旧句并存；页面不引用 generated | runtime 对象；openapi 路径键；README 第 19 行 | 2026-09-16 |
