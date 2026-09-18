---
lesson_id: L-044
objective_ids: [OBJ-44]
claimed_cells:
  - A:createProfileWebDomain enterprise
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: contribution-and-runtime
    minutes: 9
  - segment: pages-consume-kitchen
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-010, S-L039-01, S-L039-04, S-L044-01, S-L044-02, S-L044-03, S-L044-04, S-L044-05, S-L044-06, S-L044-07]
---

# Lesson 044：宏观企业三键菜单——`createProfileWebDomain` 的 enterprise 贡献如何消费 `profileService`

## 学完你能做什么

打开厅堂工厂 `frontend/packages/web-domains/profile/src/index.ts` 的 `createProfileWebDomain`，再打开它拼进去的企业贡献 `src/enterprise/registration.ts`，你能**口述浏览器怎么把企业档案纸条贴到墙上**：管理员三键喊的是 `runtime.service.enterprise.archive.*`，新建时顺手喊 `materialTags.tree('ENTERPRISE'|'COMMON')`，找人走厅堂 `findUsers('ENTERPRISE')`。不是 system 账号资料页，不是个人三页（L-039），也不是「厨房被裁过所以没有 transfer」。

口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

1. **`A:createProfileWebDomain enterprise`**（包 `@namewta/web-domain-profile`）：管理端工厂 `createProfileWebDomain` 的 id 仍是 `web-domain-profile`。它一次拼三份贡献。本课只认 **enterprise** 那一份：`createEnterpriseWebContribution` 冻住三键 `profile/enterprise/index`、`profile/enterprise/detail`、`profile/enterprise/review`，权限摊平 `profilePermissions.enterprise` 六串。前四键（material-tag + person 三页）工厂**会一并注册**，页面细节已经在 L-039；本课不把那四页再讲一遍。

OBJ-44 还要你顺着企业门脸，把厨房方法喊到人脸上：

- **管理端名册** `EnterpriseProfilePage`：`enterprise.archive.page` / `create`；材料下拉 `materialTags.tree`；上传走 `runtime.fileUpload`；找人走 `runtime.findUsers('ENTERPRISE', …)`。
- **管理端详情** `EnterpriseProfileDetailPanel`（列表抽屉与 `EnterpriseProfileDetailPage` 共用）：`archive.detail` / `material` / `revise` / `assign` / `manageBinding` / `revoke`。
- **管理端审核** `EnterpriseProfileReviewPage`：`archive.review` / `reviewMaterial`；流程按钮 `runtime.completeWorkflowTask`；覆盖决定才 `archive.decide`，且 UI 的 `APPROVE` 要映射成 `APPROVED`。
- **用户端企业认证**（L-039 把表单字段留给本课）：self 工厂第三键 `profile/enterprise/application` 的 `EnterpriseVerificationPage` 喊 `enterprise.application.current/save/submit`。键在 `createProfileSelfWebDomain`，**不是**本课格子里的 admin 工厂；HTTP 消费仍是同一座 `profileService`。

2026-09-17 工作树先钉死**包边界**（口试先数包，再数函数）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `createSystemService.profile` / `SysProfileController` | **另一栋楼。** classic `wta-system` 账号资料页。home 的 `/profile/enterprise` 是档案楼 self 企业认证 |
| 一份 `createEnterpriseWebDomain` | **没有这个工厂。** 企业三键是 `createProfileWebDomain` 里的 `createEnterpriseWebContribution` |
| home 调 `createProfileWebDomain` | **home 调的是 `createProfileSelfWebDomain`。** admin 才 `selectedManifestIds: ['web-domain-profile']` |
| 页面自己拼 `/profile/enterprise/archive` | **厨房才拼。** Vue 只喊 `runtime.service.enterprise.archive.page(...)` |
| 一份 Java 类 `EnterpriseMaterialController` | **没有这个 Java 类。** 前端资源标签 `profileEnterpriseMaterialsResource.controller` 写成这个字符串；后端是 Admin + Self 两份类（L-043） |
| 管理端新建企业档案会调用 `enterprise.materials.attach` | **2026-09-17 的 Vue 不喊这四枪。** 直传走 `runtime.fileUpload`，材料数组塞进 `enterprise.archive.create` |
| self 企业认证页会 `application.probe` / `transfer.send` | **厨房有。** 认证页只 current/save/submit；全仓 Vue 搜不到 probe / sendTransfer |
| `@namewta/domain-profile-enterprise` | **没有这个包。** 厨房仍是 `@namewta/domain-profile`，子路径 `./enterprise/archive` 等 |
| 法定代表人 = 认证负责人账户 | **不是。** 详情页写死：法定代表人是企业法定字段；认证负责人是当前绑定的系统账户 |

本课**不宣称**你会拆 `EnterpriseAdminController` 十二扇（L-040）、申请 current/submit 五层与匿名回调（L-041）、转移三枪（L-042）、材料 attach/detach 与标签树 HTTP（L-043），或把个人 archive/self 三页再讲一遍（L-039）。今天只认：**管理端工厂里的 enterprise 贡献、三张企业页怎么消费 `profileService`，以及用户端企业认证表单实际扣了哪几枪、厨房里哪些企业抽屉还没人扣扳机。**

## 先把宏观地图放在桌上

L-007 已经把插头插进厅堂：`export const profileService = createProfileService(domainHttp)`。L-008 对照过 home 也插同一座工厂，墙上却是 self 三键。L-039 把厨房 facade、管理端七键顺序、person/self/material-tag 门脸钉死，并明确：**企业三页与企业认证表单留给本课。** L-040 … L-042 是后端企业四扇门里已经写过的三扇（材料总库是 L-043）。本课站在**已经登录的浏览器**这一头：管理员拿 `Admin-Token` 进企业柜台，门户用户拿 `Home-Token` 交企业认证。

三条河都叫 enterprise，货不一样：

| 名字听起来像 | 实际是什么 | 本课认不认 |
| --- | --- | --- |
| `SysProfileController` / `views/system/user/profile` | system 账号资料页（L-022） | 邻居：不要走错楼 |
| `createProfileService.enterprise` | 浏览器档案厨房的企业抽屉：application / archive / materials / transfer | **厨房在 L-039 已认工厂**；本课认页面怎么喊它 |
| `createProfileWebDomain` 的 enterprise 贡献 | 管理端菜单后三键：企业名册 / 详情 / 审核 | **本课格子** |
| `createProfileSelfWebDomain` 第三键 | 用户端企业认证表单 | **本课认表单如何消费 `enterprise.application`**；self 工厂格子仍归 L-039 |
| `EnterpriseAdminController` | 后端 layered 十二扇 archive | L-040 |
| `EnterpriseApplicationController` / 匿名 callback | 自助 current/save/submit/probe 与供应商门铃 | L-041；前端**没有** callback 页 |
| `EnterpriseTransferController` | send/confirm/unbind | L-042；**没有** Vue 调用 |
| `EnterpriseMaterial*Controller` | 材料总库 admin/self | L-043；厨房有四枪，页面没用 |

2026-09-17 工作树：权威厨房仍是 `frontend/packages/domains/profile/`。权威菜单包是 `frontend/packages/web-domains/profile/`，企业页面 owner 在 `src/enterprise/`，用户端企业认证在 `src/self/EnterpriseVerificationPage.vue`。厅堂接线是 `apps/admin-web/src/application/services.ts` 的 `profileService` 与 `apps/home-web/src/application/services.ts` 第 16 行。菜单种子在 `50-cde-base-dml.sql`：管理端 `profile/enterprise/index`（perms `profile:enterprise:query`）；用户端 `profile/enterprise/application`（perms `profile:enterprise:apply`）；工作流表单键 `profile/enterprise/review`。详情键 `profile/enterprise/detail` **没有**独立菜单种子，给深链 / 工作流，和 L-039 的 person/detail 同一套路。

```text
已登录的人
        │
        ├─ admin-web（Admin-Token）
        │     profileService = createProfileService(domainHttp)     ← 厨房全集
        │     createProfileWebDomain(adminProfileWebRuntime)
        │           id = web-domain-profile
        │           键：materialTag/index + person/{index,detail,review}   ← L-039
        │               + enterprise/{index,detail,review}                 ← 本课
        │
        └─ home-web（Home-Token）
              profileService = createProfileService(domainHttp)     ← 还是全集
              createProfileSelfWebDomain({ service: profileService, ... })
                    id = web-domain-profile-self
                    键：center/index + person/application           ← L-039
                        + enterprise/application                    ← 本课认表单
```

往下走不要跳层：

```text
Vue 企业页（只收 runtime）
    └─ web-domain 外包一层 h(page, { runtime })
          └─ domain 工厂拼 URL / method / encodeURIComponent
                └─ App 的 domainHttp（axios + 各厅堂 Token）
                      └─ 后端 EnterpriseAdmin / Application / Transfer / Material（L-040…L-043）
```

**类比失效边界：** 「宏观企业三键菜单」**不**等于「厨房里只有这三道菜」。`profileService.enterprise` 上还有 application / materials / transfer。产品闸在 **manifest 选哪些 componentKey、页面喊哪些方法**，不在工厂里 `delete` 抽屉。类比也**不**等于「home 选了 `web-domain-profile` 就会少两道企业菜」——home 根本不选这份 id。谁在页面里喊了 `profileService.enterprise.archive.page`，卡车照样开。

## 核心概念与机制

### 直觉讲解

把 `createProfileWebDomain` 想成**管理员大厅的一本总菜单**，企业贡献是夹在后面的**三张企业点菜单**，不是另开一家店。

- **总菜单（`createProfileWebDomain`）**：进门先检查托盘齐不齐（`requireProfileWebRuntime`），再把材料标签、个人、企业三份贡献订成一本。封面 id 永远是 `web-domain-profile`。你不能只订「只要企业」——今天这份工厂会把七键一起冻住。
- **企业点菜单（`createEnterpriseWebContribution`）**：三行：名册、详情、审核。每一行写死 `componentKey`，load 时用 `h(page, { runtime })` 把托盘塞进页面。页面自己**不** import `profileService` 单例。
- **厨房抽屉（`runtime.service.enterprise`）**：服务员（页面）只喊「来一份 page / create / detail」。地址写在 `createEnterpriseArchiveService`：`/profile/enterprise/archive`。找人这道菜更绕：页面喊厅堂 `findUsers('ENTERPRISE')`，厅堂再转 `enterprise.archive.eligibleUsers`。
- **备用抽屉：** `enterprise.materials` 四枪、`enterprise.transfer` 三枪、`enterprise.application.probe` 都在厨房里。2026-09-17 **没有服务员去喊**。墙上没挂「转移」「挂材料」这两道菜，不代表厨房把刀收了。

**类比失效边界：** 点菜单类比**不**覆盖「营业执照照片怎么进 MinIO」。上传控件是 L-029 的 `FileUpload`，经 `runtime.fileUpload` 注入。本课厨房的 `enterprise.materials.attach` 只夹已经进仓的 `ossId`；直建甚至不喊 attach，把 `ossId` 放进 `archive.create.materials`。类比也**不**等于「法定代表人那一格就是登录账号」——执照页上的人可以和绑定账户不是同一个人。类比还不等于「用户大厅那本总菜单被撕掉了管理页」——home 的 `profileService.enterprise.archive` 还在，只是 self 工厂没挂那三键。

### 精确定义与 English term

| 中文口头 | English term | 磁盘落点 |
| --- | --- | --- |
| 管理端菜单工厂 | `createProfileWebDomain` | `web-domains/profile/src/index.ts`；manifest id `web-domain-profile` |
| 企业页面贡献 | enterprise web contribution | `createEnterpriseWebContribution`；`src/enterprise/registration.ts` |
| 组件键 | `componentKey` | 与 `sys_menu.component` 对表：`profile/enterprise/index` 等 |
| 运行时托盘 | `ProfileWebRuntime` | `src/runtime.ts`；host 九口 + `fileUpload` + 厨房 |
| 失败关闭 | fail-closed | `requireProfileWebRuntime` 缺方法就扔 `'ProfileWebRuntime is required'` |
| 档案厨房企业抽屉 | `ProfileService.enterprise` | `domains/profile/src/service.ts`：`application` / `archive` / `materials` / `transfer` |
| 资源标签 | resource metadata | `profileEnterpriseArchiveResource.controller = 'EnterpriseAdminController'` |
| 兼容门面 | compatibility facade | `enterprise.application.sendTransfer === enterprise.transfer.send` |
| 权限字典 | `profilePermissions.enterprise` | 六串：`apply` `manage` `material` `override` `query` `review` |
| 法定代表人 | legal representative | 身份字段 `legalRepresentativeName`，写在执照页 |
| 认证负责人 | responsible account | 绑定字段 `bindingUserId` / `bindingStatus`，钉在系统账户上 |
| 动作矩阵 | `enterpriseActionMatrix` | `src/enterprise/logic.ts`；REVOKED 四钮全关 |
| 隐私投影 | privacy projection | 仅 `application.probe` 走 `projectStatusProbeResponse`；页面目前不喊 |
| 用户端菜单工厂 | `createProfileSelfWebDomain` | 第三键 `profile/enterprise/application`；id `web-domain-profile-self` |

厨房企业子路径（`package.json` `exports`）与 Controller 门牌对齐：

| 子路径 | basePath | 本课页面用不用 |
| --- | --- | --- |
| `./enterprise/archive` | `/profile/enterprise/archive` | **用。** 管理端三页走十二枪里页面扣得着的那些（`eligibleUsers` 经 `findUsers`） |
| `./enterprise/application` | `/profile/enterprise/application` | **self 认证页用 current/save/submit。** `probe` 无 Vue 调用 |
| `./enterprise/transfer` | `/profile/enterprise/transfer` | 厨房有三枪 + 旧名挂在 `application.*Transfer`。**无 Vue 调用** |
| `./enterprise/materials` | `/profile/enterprise/materials` | 厨房有 list/attach/detach/accessUrl。**无 Vue 调用** |
| `./material-tags` | `/profile/material-tags` | **管理端新建档案用。** `tree('ENTERPRISE')` + `tree('COMMON')`。标签管理页本身是 L-039 |

`profileDomainModule.capabilities` 里的 `enterprise-application` / `enterprise-transfer` / `enterprise-material` / `enterprise-archive` 是名片，**不是** componentKey，也不决定 App 挂哪几页。

runtime 校验写死的 `archiveMethods` **十一**口（person 与 enterprise **同一份名单**）：`assign` `create` `decide` `detail` `manageBinding` `material` `page` `review` `reviewMaterial` `revise` `revoke`。**没有** `eligibleUsers`。缺 application / materials / transfer 仍能通过 `createProfileWebDomain`。

### 机制/因果链

**1. 工厂把企业贡献订进同一本菜单，缺托盘就整本关门。**

`createProfileWebDomain(runtimeInput)` 先 `requireProfileWebRuntime`。企业抽屉必须有那十一口 archive。然后：

```ts
const materialTag = createMaterialTagWebContribution(runtime);
const person = createPersonWebContribution(runtime);
const enterprise = createEnterpriseWebContribution(runtime);
```

`permissions` 与 `registrations` 按这个顺序摊平。测试锁死七键顺序，后三行才是企业：

5. `profile/enterprise/index` → `EnterpriseProfile`
6. `profile/enterprise/detail` → `EnterpriseProfileDetail`
7. `profile/enterprise/review` → `EnterpriseProfileReview`

`createEnterpriseWebContribution(undefined)` 自己也会 throw。企业贡献不是「工厂过了校验，贡献可以偷懒」。

权限目录把 `Object.values(profilePermissions.enterprise)` 六串全部冻进 id `profile-enterprise`。其中 `apply` 进了管理端权限目录，但管理端三页**没有**「提交认证」按钮；`apply` 真正给按钮用的是 home 中心页的字面量。页面按钮另用 `runtime.hasPermission(...)` 做当前用户投影；**后端仍是最终授权者**。

`composeAppRuntime` 只有 `selectedManifestIds` 含 `web-domain-profile` 时才把键交给导航。测试：空 selected 的 fixture App `componentKeys()` 是 `[]`。admin 厅堂这份 id 在 `adminManifestRegistry.ts` 的 `selectedManifestIds` 里；home **不**选它。

**2. 厅堂托盘把找人、上传、办流程从厨房方法里拆出去。**

`adminProfileWebRuntime` 的 `service` 就是那座 `profileService`。额外几口不是厨房自己长的：

| runtime 口 | 厅堂接到哪 | 企业页怎么用 |
| --- | --- | --- |
| `fileUpload` | 厅堂 `WorkflowFileUpload`（L-029） | 新建档案每行一个上传控件，`v-model` 得到 `ossId` |
| `findUsers(profileType, keyword)` | `profileType === 'PERSON' ? person.archive : enterprise.archive` 再 `eligibleUsers` | 企业页写死 `'ENTERPRISE'` |
| `completeWorkflowTask` | `workflowService.completeTask({ taskId, message: comment, variables })` | 审核页「提交流程审核」 |
| `downloadMaterial` | 造 `<a href={access.url} download={fileName}>` | 详情 / 审核点「查看」 |
| `closeCurrentPage` | 厅堂关页签 | 审核提交成功后关页 |
| `hasPermission` | `createAdminAccessEvaluator()` | 新建按钮、覆盖决定、动作矩阵 |

口试不要说「页面直接喊 `enterprise.archive.eligibleUsers`」。源码契约测试锁的是 `findUsers('ENTERPRISE'`。nickName 空时标签退化成 `owner (owner)`。

**3. 名册页走 archive.page / create，材料不走 materials 四枪。**

`EnterpriseProfilePage`：

- `archive.page({ enterpriseName, unifiedCreditCode, status, pageNum, pageSize })`。空状态筛选项把 `status` 收成 `undefined`，不传空串当枚举。
- 持 `profilePermissions.enterprise.override` 才见「新建档案」。
- 检索账户：`runtime.findUsers('ENTERPRISE', keyword)`。可留空，`bindUserId` 允许 `null`。
- 打开新建时 `materialTags.tree('ENTERPRISE')` 与 `tree('COMMON')` 拼下拉。`flattenEnterpriseMaterialOptions` 只收启用中的 `TAG`，且 scope 是 ENTERPRISE 或 COMMON。PERSON 牌进不了这张下拉。
- 单文件 10MB、最多 10 份、类型白名单在页面：`png/jpg/jpeg/pdf/doc/docx`。这是 UI 闸；后端材料闸仍是 L-043。
- 确认后 `archive.create({ identity, bindUserId, reason, materials })`。测试禁止源码出现 `materials: []`。过滤后条数必须等于行数且大于 0，否则 warning。
- `isEnterpriseIdentityComplete` 只盯九个法定字段（名称、信用代码、类型、法人姓名/证件、成立日期、注册地址、经营范围）。联系人、电话、邮箱、注册资本**不是**这道前端闸。营业期限可空。

`EnterpriseIdentityForm` 的证件类型写了十三项（含居住证、通行证、多地护照）。这是管理端表单自己的硬编码，不是 `runtime.dicts`，也不是 self 认证页那五选项。

**4. 详情页把「执照上的人」和「钉子上的账号」拆开，高风险动作先 confirm。**

`EnterpriseProfileDetailPage` 是薄壳：`route.query.id` 交给同一份 Panel。列表页抽屉也复用 Panel。detail 键给深链，index 抽屉给柜台点开。

`EnterpriseProfileDetailPanel`：`archive.detail(profileId)`。下载走 `archive.material`（archive 门牌上的 access-url，L-040），再 `runtime.downloadMaterial`。当前版本取 `versions` 里 `status === 'CURRENT'`，没有就退到 `versions[0]`。修订/注销的 `expectedVersion` 取 `currentVersion.versionNo`，再没有就 `0`。

`enterpriseActionMatrix`：档案 `REVOKED` 则四钮全关。`assign` / `revise` / `revoke` 还要 `profile:enterprise:override`。`manageBinding` 要 `profile:enterprise:manage`，且存在 `ACTIVE` 或 `SUSPENDED` 绑定。`enterpriseBindingAction`：`ACTIVE` → 默认 `SUSPEND`，`SUSPENDED` → `RESUME`，其余 → `UNBIND`。绑定提交带 `expectedBindingVersion`。

页面用告警条钉死两套身份：**法定代表人是企业法定字段；认证负责人是当前绑定的系统账户，两者不会自动等同。** `safeEnterpriseErrorMessage` 吞掉信用代码，只回「操作失败，请刷新后重试」。源码契约还禁止 detail 出现 `delete` / `export` / `console.`。

**5. 审核页两条河：流程变量用 APPROVE，覆盖决定用 APPROVED。**

`EnterpriseProfileReviewPage`：`archive.review` 打的是 `GET .../application/{id}/review-context`（Java 方法 `reviewContext`，厨房名 `review`）。材料预览 `archive.reviewMaterial`。两条提交河：

| 按钮 | 前端调用 | 磁盘含义 |
| --- | --- | --- |
| 提交流程审核 | `runtime.completeWorkflowTask({ taskId, comment, variables: { profileDecision: 'APPROVE'\|'REJECT' } })` | 厅堂转 `workflowService.completeTask`；**不**打 archive `/decision` |
| 管理员覆盖决定 | `archive.decide(..., { decision: 'APPROVED'\|'REJECTED', reason })` | 才是 L-040 那扇 POST。UI 的 APPROVE 要映射成 APPROVED |

缺 `query.taskId` 时流程按钮只 `warning('缺少流程任务编号')`，不发 HTTP。覆盖按钮看 `profilePermissions.enterprise.override`。成功后 `closeCurrentPage`。

**6. 用户端企业认证只打 application 三枪，闸和证件列表都与管理端不同。**

`EnterpriseVerificationPage` 吃的是 `ProfileSelfWebRuntime`，**没有** `fileUpload` / `findUsers` / `completeWorkflowTask`。`onMounted` → `enterprise.application.current()`。保存 `save({ ...identity, handlerIsLegalRepresentative, expectedVersion })`。提交先 `confirm`，再 **先 save 再 `submit(version)`**，版本取 save 返回的 `data.version`。没有材料行，没有 `attach`，没有 `probe`，没有 transfer。

状态文案：`DRAFT` 草稿、`BACK` 已退回、`WAITING` 审核中、`FINISH` 已完成。证件类型只五选项：居民身份证 / 港澳台身份证 / 中国护照。管理端那十三项不要背到这张表上。`valid()` 比管理端 `isEnterpriseIdentityComplete` **多要** `contactName` 与 `contactPhone`。另有开关 `handlerIsLegalRepresentative`，管理端身份表没有这一项。

失败文案走 `error instanceof Error ? error.message : '…'`，**会**把厨房抛出的原文送到提示里。管理端 `safeEnterpriseErrorMessage` 不会。两套页面不要说成同一条隐私策略。

`ProfileCenterPage`：路径是 `/profile` 时展示两张卡片；企业那张写死 `runtime.hasPermission('profile:enterprise:apply')`，`router.push('/profile/enterprise')`。**没有** import `profilePermissions.enterprise.apply`。

**7. 厨房有枪、页面没扣扳机的企业抽屉。**

`createProfileService` 冻住：

- `enterprise.application.probe` → `POST /profile/enterprise/application/probe`，再 `projectStatusProbeResponse`（`data` 只许 `{ status }`）。
- `enterprise.application.sendTransfer === enterprise.transfer.send` 等三只旧名，URL 在 `/profile/enterprise/transfer/{send,confirm,unbind}`。
- `enterprise.materials.list/attach/detach/accessUrl`，门牌 `/profile/enterprise/materials/{ownerType}/{ownerId}`。

2026-09-17 在 `web-domains/profile` 的 Vue 里搜 `enterprise.application` 只有 self 认证三枪；搜 `materials` / `transfer` / `probe` 不是页面调用。口试可以说「厨房订进总菜单」；不可以说「企业页会 probe 信用代码」或「转移挑战有一张 Vue」。

匿名核身 `POST /profile/enterprise/verification/providers/{providerCode}/callback` 在 OpenAPI 快照里。README：无页面的匿名验证不建空目录。不要为 callback 找 `src/enterprise/anonymous/`。

**8. OpenAPI 快照与页面类型不要背反。**

生成文件 `frontend/packages/api-contracts/generated/openapi.ts` **已经**有 `/profile/enterprise/archive`、`/profile/enterprise/application`、`/profile/enterprise/transfer`、`/profile/enterprise/materials`。页面与 domain service **仍不** import generated 类型；URL 合同的测试锁在 `domains/profile/src/service.test.ts`。斜杠编号走 `encodeURIComponent`：`reviewMaterial('app/2', 'ref/2')` 变成 `.../application/app%2F2/material/ref%2F2/access-url`。

### 图、表或文本图

```text
 sys_menu.component                 种子
   profile/enterprise/index         管理端 Client（query）
   profile/enterprise/review        工作流表单键
   profile/enterprise/application   home Client（apply）
   （detail 无独立菜单行，给深链）
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
                ├─ 管理端：fileUpload / findUsers / completeWorkflowTask / downloadMaterial
                ├─ 用户端：只有 confirm / error / success / warning / hasPermission
                └─ 页面只喊方法名，不拼 URL
```

**图题 / caption：** 宏观同一厨房、企业三键在管理端菜单后部、认证键在 self 菜单。alt：菜单键来自 sys_menu；admin 与 home 选不同 manifest id；企业页只收到 runtime；HTTP 字符串在 domain 工厂。

**文字等价物：** 人先碰到动态菜单里的 component 字符串。App 用已选 manifest 把字符串换成带 runtime 的 Vue 页。管理端企业页面向 `runtime.service.enterprise.archive` 喊方法，新建时还喊标签树和厅堂找人/上传。用户端企业认证只喊 `enterprise.application` 三枪。厨房把方法换成 GET/POST 和 `/profile/enterprise/...`。home 即使厨房里有 archive，墙上没有 `profile/enterprise/index`，导航就不会挂那页。

第二张表把「厨房有枪 / 本课页面扣扳机」钉死：

| 厨房方法 | HTTP（动词以测试为准） | 2026-09-17 enterprise 页面 |
| --- | --- | --- |
| `enterprise.archive.page/create/detail/material/review/reviewMaterial/revise/assign/manageBinding/revoke/decide` | GET 名册/详情/审核/出门条；POST 直建/修订/绑定/注销/决定 | **EnterpriseProfile\*** 使用。`eligibleUsers` 不直接喊，经 `findUsers` |
| `enterprise.archive.eligibleUsers` | GET `.../eligible-users` | 厅堂 `adminProfileWebRuntime.findUsers('ENTERPRISE', keyword)` |
| `materialTags.tree('ENTERPRISE'\|'COMMON')` | GET `/profile/material-tags/tree` | **EnterpriseProfilePage** 新建下拉。标签管理页仍是 L-039 |
| `enterprise.application.current/save/submit` | GET 根；POST 根；POST `/submit` | **EnterpriseVerificationPage** |
| `enterprise.application.probe` | POST `/probe`，再隐私投影 | **无 Vue 调用** |
| `enterprise.application.*Transfer` / `enterprise.transfer.*` | 三枪全 POST `/profile/enterprise/transfer/...` | **无 Vue 调用** |
| `enterprise.materials.list/attach/detach/accessUrl` | GET list 与 access-url；POST attach 与 `.../detach` | **无 Vue 调用**。下载走 `archive.material` / `reviewMaterial` |

**图题 / caption：** 厨房企业方法全集不等于已挂页面的调用集。alt：archive 与 application 三枪有页面；probe、transfer、materials 四枪目前只有测试。

**文字等价物：** 管理员三页消耗 archive 命令集和标签树。用户认证页消耗 application 的 current/save/submit。probe、转移、材料总库四枪在厨房对象上，本课页面源码里没有对应调用。不要把「工厂注册了企业三键」说成「所有企业 HTTP 都有按钮」。

**图的边界：** 不画 Java 五层（L-040…L-043）。不画 person 三页模板。不保证以后产品会补转移页。不把 `capabilities` 名片画成菜单。

### 正例、反例与边界

**正例 1 — 管理员翻企业名册。** 有 `profile:enterprise:query` 的人打开菜单 `profile/enterprise/index`。页 `archive.page({ enterpriseName, unifiedCreditCode, status, pageNum, pageSize })`。厨房 `GET /profile/enterprise/archive`。状态筛「有效」传 `ACTIVE`，「已注销」传 `REVOKED`；清空后 `status` 是 `undefined`。

**正例 2 — 管理员直建一本企业档案。** 持 `profile:enterprise:override` 才见「新建档案」。检索账户时 `runtime.findUsers('ENTERPRISE', 'own')` → 厅堂 `enterprise.archive.eligibleUsers('own')` → `GET /profile/enterprise/archive/eligible-users?keyword=own`。上传控件 `v-model` 得到 `ossId`。`create` POST `/profile/enterprise/archive/admin-create`，body 带 `identity`、`bindUserId`（可 null）、`reason`、`materials: [{ materialNodeId, ossId }]`。**没有** `POST /profile/enterprise/materials/WORKING/...`。

**正例 3 — 审核员看企业申请袋。** 工作流表单键 `profile/enterprise/review`，query 带 `id` 与 `taskId`。`archive.review` → `GET .../application/{id}/review-context`。点材料「查看」→ `archive.reviewMaterial` → 厅堂造下载链。点「提交流程审核」只 `completeWorkflowTask`，变量 `profileDecision: 'APPROVE'|'REJECT'`。点「管理员覆盖决定」才 `decide`，且 `APPROVE` 变成 `APPROVED`。

**正例 4 — 修订执照页，不改绑定账户。** 详情里点「修订核心字段」。`isEnterpriseIdentityComplete` 过了，填原因，`confirm` 后 `archive.revise(profileId, { identity, reason, expectedVersion })` → `POST .../{id}/revision`。法人姓名改了，不会自动改 `bindingUserId`。

**正例 5 — 处置负责人钉子。** 档案仍是 ACTIVE，存在 ACTIVE 绑定，持 `profile:enterprise:manage`。打开「处置负责人绑定」，默认动作是 `SUSPEND`。提交 `archive.manageBinding(..., { action, reason, expectedBindingVersion })` → `POST .../{id}/binding`。这不是 L-042 的 `transfer.unbind`，也不是 `archive.revoke`。

**正例 6 — 门户用户交企业认证。** home 登录后进 `/profile`。有 `profile:enterprise:apply` 才见企业「开始认证」，跳 `/profile/enterprise`。`GET /profile/enterprise/application`。填主体/法人/联系人，保存 POST 根（带 `handlerIsLegalRepresentative`）；提交先 save 再 POST `/profile/enterprise/application/submit`，`data: { expectedVersion }`。

**正例 7 — 斜杠编号。** 测试 `archive.reviewMaterial('app/2', 'ref/2')` 的 URL 是 `/profile/enterprise/archive/application/app%2F2/material/ref%2F2/access-url`。不要把斜杠当多一级路径。

**正例 8 — 旧名与新名是同一只手。** `service.enterprise.application.sendTransfer === service.enterprise.transfer.send`。新页面应喊 `enterprise.transfer`；旧调用面仍打同一 URL。今天两面都没有 Vue 调用。

**反例 1 — 「home 的 profileService 没有 enterprise.archive。」** 对象上有。L-008 / L-039 已经挖过这句。本课再钉：差在 `createProfileSelfWebDomain` 的三键，不在工厂。

**反例 2 — 「资料页 `/user/profile` 就是企业档案楼。」** admin 个人中心 hidden 路由是 system 资料页（L-022）。企业档案管理在动态菜单 `profile/enterprise/index`。home 的 `/profile/enterprise` 是 self 企业认证。

**反例 3 — 「页面 attach 企业材料。」** 全仓 Vue 搜不到 `enterprise.materials.attach`。直建走 `archive.create.materials`；自助认证页目前连材料行都没有。

**反例 4 — 「审核通过 = `decide({ decision: 'APPROVE' })」。** 覆盖决定要 `APPROVED` / `REJECTED`。流程变量才用 `APPROVE` / `REJECT`。混用会把错码送给后端。

**反例 5 — 「`createEnterpriseWebDomain` 是第九个 web-domain 工厂。」** 没有这份函数。九厂矩阵行里的 profile 是 `createProfileWebDomain` 与 `createProfileSelfWebDomain`。GP-L-020：不要把九个工厂一行标 covered。本课只给 enterprise 切片当证据。

**反例 6 — 「`createProfileWebDomain(undefined)` 会给出空企业菜单。」** 立刻 throw。`createEnterpriseWebContribution(undefined)` 同样 fail-closed。

**反例 7 — 「前端资源名 `EnterpriseMaterialController` 对应一个 Java 类。」** 只是 domain 标签。后端两份类共用门牌（L-043）。

**反例 8 — 「厨房会 HMAC / 会读 Token。」** 只收 `HttpClient`。登录票在 App HTTP。

**反例 9 — 「注销是 DELETE，导出在 archive.export。」** 测试：方法不是 get/post 的枪不存在；URL 不含 `export`。注销是 `POST .../revoke`。

**反例 10 — 「转移挑战页喊 `archive.manageBinding('UNBIND')`。」** 2026-09-17 没有转移页。管理端解绑是 archive 的 binding 窗；本人自助解绑是 L-042 的 `POST /profile/enterprise/transfer/unbind`。两扇门不要画成一个按钮。

**反例 11 — 「self 企业页会先 probe 信用代码。」** 厨房有 probe。认证页 `valid()` 只看必填字符串，不打 `/probe`。占用冲突发生在后端 save/submit（L-041）。

**边界 1 — runtime 校验名单 ≠ 厨房方法全集。** 管理端校验不看 `enterprise.materials`、不看 `transfer`、不看 `eligibleUsers`、不看 `application`。缺这些方法仍能通过 `createProfileWebDomain`，页面一喊才会在运行时失败。

**边界 2 — 自助页没有材料 UI ≠ 后端不校验必传。** 提交后 ALWAYS 材料闸是 L-041 / L-043。本课只陈述：self 企业页 2026-09-17 不调用 attach。缺图时失败发生在后端，不是厨房少枪。

**边界 3 — `EnterpriseProfilePage` 的文件类型含 doc/docx。** 材料总库白名单以 L-043 / 后端为准。直建走 archive 门，两套闸不要背成一句。

**边界 4 — 标签页能切 ENTERPRISE scope。** 那仍是 material-tag owner（L-039），HTTP 仍打 `/profile/material-tags`。不要说「切到企业就是本课的企业档案页」。本课新建档案只是**消费**那棵树的 ENTERPRISE+COMMON 叶子。

**边界 5 — 管理端法定完整性 ≠ self `valid()`。** 管理端九字段；self 另要联系人、联系电话，并带 `handlerIsLegalRepresentative`。证件下拉数量也不同（十三 vs 五）。

**边界 6 — `downloadMaterial` 信任厨房返回的 `url`。** 厅堂用 `<a href={access.url} download={fileName}>`。授权在后端签发出门条；前端不二次鉴权。

**边界 7 — 匿名回调无页面。** README：无页面的匿名验证不建空目录。不要为 `EnterpriseVerificationAnonymousController` 找 Vue。

**边界 8 — 字典硬编码。** 两张企业身份表的证件类型写在 Vue 里，不是 `runtime.dicts`。system 字典合同（L-021）不要套到这张表单上。

**边界 9 — 错误文案策略分裂。** 管理端企业页一律「操作失败，请刷新后重试」。self 企业认证可能把 `Error.message` 原样弹出。不要说「档案楼统一吞证件号」。

**边界 10 — `bindUserId` 可空只在直建。** 新建允许不选负责人。详情「指定认证负责人」则 `userId === ''` 会 warning，不会发 `assign`。

## 变式与迁移

1. **和 L-039 对照。** 同一座厨房、同一份管理端工厂、同一份 runtime 校验。本课换的是后三键、企业身份字段、`findUsers('ENTERPRISE')`、`tree('ENTERPRISE')`，以及「法人 ≠ 负责人」这道篱笆。不要把 person 七字段和 `person.rebind` 再抄一遍。
2. **和 L-040 对照。** 十二扇 HTTP 在 Java。本课页面没有把 `eligibleUsers` 直接喊出来；`review` 的 URL 仍是 `/review-context`；`decide` 的码要从 UI 的 APPROVE 映射到 APPROVED。注销仍是 POST revoke，不是 DELETE。
3. **和 L-041 对照。** self 三枪对上 current/save/submit。页面不打 probe，也不处理匿名 callback。submit 成功只表示申请进审核，不是立刻有 `profile_enterprise` 档案。
4. **和 L-042 对照。** 厨房已经把 send/confirm/unbind 订进 facade，旧名挂在 `application`。2026-09-17 没有 web-domain 转移页。以后若要做「交钉子」，应喊 `runtime.service.enterprise.transfer.*`，不要在 Vue 里手写 `/profile/enterprise/transfer`，也不要复用 `archive.manageBinding`。
5. **和 L-029 对照。** 新建档案的字节走 `runtime.fileUpload`。厨房 `attach` 只夹 `ossId`。现在直建甚至不喊 attach，把 `ossId` 放进 `archive.create`。
6. **和 L-020 对照。** `createSystemWebDomain` 是另一份菜谱板。本课管理端工厂 7 键里的企业 3 键。导航 host 仍是 `getRouters` → `resolve*Registration` → `addRoute`。profile 不另写一套路由恢复。
7. **以后若自助页要补营业执照照片。** 应喊 `runtime.service.enterprise.materials.attach('WORKING', applicationId, { ossId, materialNodeId })`，并给 self runtime 补上传口；不要在 Vue 里手写 `/profile/enterprise/materials`。那是产品增量，不是本课缺口补丁。
8. **以后若要独立的「只要企业」菜单。** 今天的 `createProfileWebDomain` **会**把 material-tag/person/enterprise 七键一起注册。不能靠「只用企业页」让工厂少返回四键。要瘦菜单，得新写 contribution 组合，或让 App 不选那些菜单种子。
9. **换 App。** 第三份 App 若只要企业审核页，仍须 `createProfileService` + 一份含 enterprise review 的 manifest + 能办流程的 runtime。把 admin runtime 塞进 self 工厂、或反过来，口试先数校验函数：admin 要十一口 archive 和 `fileUpload`；self 只拒 `undefined`。
10. **迁移口诀：** 先数管理端工厂七键里的后三键 → 再数页面真正喊出的 `enterprise.archive.*` → 再数 self 认证三枪 → 最后数厨房里还没扣扳机的 probe/transfer/materials。跳步会出现「把 home 说成裁过的服务」「把 attach 说成新建档案的那一枪」「把转移说成已经有 Vue」。

## 常见误区

1. **「OBJ-44 包含 `EnterpriseAdminController`。」** 那是 OBJ-40。本课认 web-domain enterprise 贡献与页面如何消费 `profileService`。
2. **「`createProfileWebDomain` 等于 `createProfileSelfWebDomain`。」** id、键、runtime、权限目录、校验强度全不同。本课格子只切 admin 工厂的 enterprise 贡献。
3. **「企业贡献是第四个包 `@namewta/web-domain-profile-enterprise`。」** 子路径 `./enterprise`，还在同一个 web-domain 包里。
4. **「厨房会按 App 裁掉 enterprise.archive。」** 不会。
5. **「详情下载走 `enterprise.materials.accessUrl`。」** 走 `enterprise.archive.material`。
6. **「流程审核按钮打 `/decision`。」** 默认走 `completeWorkflowTask`。覆盖决定才 `decide`。
7. **「`review` 的 URL 是 `/review`。」** 是 `/review-context`。
8. **「页面 `findUsers` 就是厨房方法。」** 是 runtime 口。厅堂按 profileType 转 `eligibleUsers`。
9. **「前端授权。」** `hasPermission` 藏按钮；HTTP 仍可能 403。厨房把错误原样抛出。
10. **「`capabilities` 有 enterprise-transfer 所以一定有转移页。」** 名片有能力，manifest 可以不挂页。今天就是这样。
11. **「管理端证件十三项 = 用户端证件五项。」** 两张表各自硬编码。
12. **「中心页 import 了 `profilePermissions`。」** 没有。模板写字面量 `'profile:enterprise:apply'`。
13. **「本课覆盖 person 三页。」** L-039。本课只承认管理端工厂会**一并注册**那四键。
14. **「匿名核身也有一张 Vue。」** 没有。L-041。
15. **「法定代表人账户就是认证负责人。」** 页面专门写了相反的那句话。
16. **「self 校验和 admin 一样严。」** self 工厂只拒 `undefined`；企业认证页甚至不校验厨房有没有 `application.current`。
17. **「把九个 web-domain 工厂一行标 covered。」** GP-L-020。本课只给 `A:createProfileWebDomain enterprise`。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `frontend/packages/web-domains/profile/src/index.ts`。圈 `createEnterpriseWebContribution` 拼进 registrations 的位置、七键里后三行、id `web-domain-profile`。打开 `index.test.ts`，核对顺序与 `createEnterpriseWebContribution(undefined)` 的 throw。
2. 打开 `src/runtime.ts`。圈 `archiveMethods` 十一项（没有 `eligibleUsers`）、`enterprise.archive` 与 `person.archive` 共用这份名单、缺 runtime 的 throw。
3. 打开 `src/enterprise/registration.ts`、`EnterpriseProfilePage.vue`、`EnterpriseProfileDetailPanel.vue`、`EnterpriseProfileReviewPage.vue`、`logic.ts`。圈各自真正喊出的 `runtime.service.enterprise.archive.*`、`materialTags.tree('ENTERPRISE')`、`findUsers('ENTERPRISE'`、`completeWorkflowTask`、`APPROVE`→`APPROVED`。
4. 打开 `apps/admin-web/src/router/adminManifestRegistry.ts` 的 `adminProfileWebRuntime`。圈 `findUsers` 在 `ENTERPRISE` 时转 `profileService.enterprise.archive.eligibleUsers`、`completeWorkflowTask` 转 `workflowService.completeTask`、`downloadMaterial` 的 `<a>`。打开 `adminManifestRegistry.test.ts` 对一下 `'own'`。
5. 打开 `src/self/EnterpriseVerificationPage.vue` 与 `ProfileCenterPage.vue`。圈 current/save/submit、五证件、`handlerIsLegalRepresentative`、字面量 `'profile:enterprise:apply'`。确认没有 probe / attach / transfer。
6. 打开 `domains/profile/src/service.ts` 与 `service.test.ts`。圈 `sendTransfer === transfer.send`、probe URL、materials access-url、`%2F`。在 web-domains 里搜 `enterprise.materials` 与 `enterprise.transfer`，确认不是页面调用。

## 总结、词汇表与下一步

- **宏观企业三键菜单：** `createProfileWebDomain` 把 enterprise 贡献订进 `web-domain-profile`。墙上后三键是企业名册 / 详情 / 审核。厨房方法全集 ≠ 墙上的菜 ≠ 页面已扣的扳机。
- **(a) `createProfileWebDomain enterprise`：** 贡献工厂冻住三键与六串 `profile:enterprise:*`。页面经 runtime 喊 `enterprise.archive` 与标签树；找人、上传、办流程是厅堂口。self 企业认证消费同一厨房的 `enterprise.application` 三枪，但不在本课格子的 admin 工厂里。
- **页面实际调用：** 管理端走 archive 命令 + `materialTags.tree` + `findUsers('ENTERPRISE')`。审核分流工作流 / `decide`（APPROVE→APPROVED）。自助走 application 三枪。`probe`、`transfer`、`enterprise.materials` 四枪目前只有测试锁 URL。
- **法人 ≠ 负责人。** 注销作废整本；解绑只拔钉子；转移三枪还没有 Vue。不是 system 资料页，不是后端五层复述。

词汇表：`createProfileWebDomain` / `createEnterpriseWebContribution` / `ProfileWebRuntime` / `componentKey` / `profilePermissions.enterprise` / `enterprise.archive` / `enterprise.application` / `findUsers` / `eligibleUsers` / `completeWorkflowTask` / `enterpriseActionMatrix` / legal representative / responsible account / fail-closed runtime / compatibility facade / privacy projection。

下一步：管理端档案十二扇与决定码是 OBJ-40。自助 current/submit/probe 与核身回调是 OBJ-41。转移 send/confirm/unbind 是 OBJ-42。材料总库 admin/self 是 OBJ-43。个人前端切片是 OBJ-39。厅堂插头是 OBJ-07 / OBJ-08。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web}` | 两厅堂都 `createProfileService(domainHttp)`；admin 选 `web-domain-profile`，home 选 `web-domain-profile-self` | `application/services.ts`；`adminManifestRegistry.ts`；`homeManifestRegistry.ts` | 2026-09-17 |
| S-006 | `frontend/packages/{domains,web-domains}/profile` | 厨房企业抽屉、web-domain 工厂、`./enterprise` 与 `./self` 子路径 | 各包 `package.json` `exports` 与 `src` | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 菜单键 `profile/enterprise/index`、`profile/enterprise/application`；工作流表单 `profile/enterprise/review` | 档案菜单与流程节点行 | 2026-09-17 |
| S-L039-01 | `packages/domains/profile/src/service.ts`；`service.test.ts`；`permissions.ts` | 企业四抽屉；旧名 `===` 新名；probe/transfer/materials URL；无 export | `createProfileService`；enterprise 相关 `it` | 2026-09-17 |
| S-L039-04 | `web-domains/profile/src/index.ts`；`runtime.ts`；`index.test.ts` | 七键顺序；enterprise 后三键；缺 runtime throw；权限摊平含 enterprise 六串 | `createProfileWebDomain`；`archiveMethods` | 2026-09-17 |
| S-L044-01 | `src/enterprise/registration.ts` | 三键 `profile/enterprise/{index,detail,review}`；permissions 取 `profilePermissions.enterprise`；fail-closed | `createEnterpriseWebContribution` | 2026-09-17 |
| S-L044-02 | `EnterpriseProfilePage.vue`；`EnterpriseIdentityForm.vue`；`components.test.ts` | page/create；`tree('ENTERPRISE')`；fileUpload；禁止 `materials: []`；十三证件 | 模板与 script；两个 `it` | 2026-09-17 |
| S-L044-03 | `EnterpriseProfileDetailPanel.vue`；`EnterpriseProfileDetailPage.vue`；`logic.ts`；`logic.test.ts` | detail/material/revise/assign/manageBinding/revoke；法人≠负责人；REVOKED 只读；吞错误 | 告警条；`enterpriseActionMatrix` | 2026-09-17 |
| S-L044-04 | `EnterpriseProfileReviewPage.vue` | `archive.review` / `reviewMaterial`；`completeWorkflowTask`；APPROVE→APPROVED | 模板与 script；源码契约 `it` | 2026-09-17 |
| S-L044-05 | `src/self/EnterpriseVerificationPage.vue`；`ProfileCenterPage.vue`；`self/registration.ts` | current/save/submit；五证件；`handlerIsLegalRepresentative`；中心页字面量 apply | self 第三键 `profile/enterprise/application` | 2026-09-17 |
| S-L044-06 | `apps/admin-web/src/router/adminManifestRegistry.ts` 与 `.test.ts` | findUsers('ENTERPRISE')→eligibleUsers；completeTask 接线；三企业 componentName | `adminProfileWebRuntime`；resolve 三个 `it` | 2026-09-17 |
| S-L044-07 | `domains/profile/src/enterprise/{archive,application,transfer,materials}/service.ts` 与各 `index.ts` 的 controller 字符串；`generated/openapi.ts` `/profile/enterprise/**`；web-domain README | 十二枪 archive、四枪 application、三枪 transfer、四枪 materials；`EnterpriseMaterialController` 只是标签；无匿名页目录 | `base` 常量；资源 `controller` 字段；README 第 7 行 | 2026-09-17 |
