---
lesson_id: L-019
objective_ids: [OBJ-19]
claimed_cells: [A:createSystemService.users,roles,menus,departments,posts,clients,userTypes,ssoApps]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: eight-drawers-by-resource
    minutes: 11
  - segment: mapping-traps
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-L019-01, S-L019-02, S-L019-03, S-L019-04, S-L019-05, S-L019-06, S-L019-07, S-L019-08]
---

# Lesson 019：厨房八个抽屉——`createSystemService` 怎么写成 HTTP

## 学完你能做什么

打开 `frontend/packages/domains/system/src/service.ts` 的 `createSystemService`，你能**指着八个资源口**说出：页面调哪个抽屉、抽屉填哪张纸条（`url` / `method` / `params` 还是 `data`）、卡车开到哪扇后端窗。口试名单就是矩阵 (a) 这一行的**八口**，不是整座厨房：

`A:createSystemService.users,roles,menus,departments,posts,clients,userTypes,ssoApps`

OBJ-19 原文点了前七个名字。第八口 **`ssoApps`** 是 L-018 那扇 SSO 登记窗的厨房孪生，chain 写进本格，口试要能把它和 `clients` 拆开，不要推给下一课。

你还能把三句分清，不揉成「system 服务等于一张方法表」：

1. **厨房 ≠ 厅堂，厨房 ≠ 页面。** 工厂只认 `HttpClient.request`。axios、token 头、下载框在 App；Vue 页走 `runtime.service.*`。home-web 也会 `createSystemService`，但产品只把 `identity` 喂给登录，不组装这八口管理页。
2. **八口 ≠ 工厂上每一个键。** 同函数还冻着 `identity`、`resources`、`publicUsers`。`identity` 的两个 URL 已由 L-014 认过；字典/配置/OSS/社交在 `resources`（L-021 起）；`createOpenApiService` / `createMonitorService` 是**另一座工厂**。本格不把它们标 covered。
3. **抽屉名 ≠ 后端一份 Java。** `users` 一出口打 L-015 三份 Controller，外加资料页 `/system/user/profile*`（L-022）。`clients` 与 `ssoApps` 两出口打同一张表 `sys_client`（L-018）。

本课不宣称你会拆 RBAC 里屋（L-016）、部门岗位登录域（L-017）、菜单怎么变成路由（L-020）、资料页改自己的密（L-022），或把 API-005「变更一律 POST」说成这份工厂已经改完。口试是**对照磁盘的 HTTP 映射**，按资源分组，不默写七十行。

## 先把宏观地图放在桌上

L-007 把插头插进 admin 厅堂：`systemService = createSystemService(domainHttp)`。L-015…L-018 已经认过后端窗。本课站在**厨房这一头**，把八个抽屉的纸条贴到那些窗上。

2026-09-16 工作树：权威文件就是 `frontend/packages/domains/system/src/service.ts`。`index.ts` 再导出工厂。测试在 `src/index.test.ts`。类型 `SystemService` 写在同一份 `service.ts`。

```text
admin-web 页面 / web-domain-system
        │  runtime.service.users.list(...)
        v
createSystemService(http)            ← domain-system 一份厨房
        │  每个方法 ≈ http.request({ url, method, params?, data?, headers? })
        v
厅堂 HttpClient（adminHttp / homeHttp 的 request）
        │
        ├─ /system/user/*        users
        ├─ /system/role/*        roles
        ├─ /system/menu/*        menus
        ├─ /system/dept/*        departments
        ├─ /system/post/*        posts
        ├─ /system/client/*      clients
        ├─ /system/ssoApp/*      ssoApps
        └─ /system/userType/*    userTypes

同工厂但本格不口试：
        identity     GET /system/user/getInfo 、 GET /system/menu/getRouters
        resources    dict / config / oss / social
        publicUsers  再打一遍 user list/options/deptTree，但投影成摘要
```

| 抽屉 | 前缀 | 列表长什么样 | 写库常用动词（磁盘） | 本课要能指的特殊枪 |
| --- | --- | --- | --- | --- |
| `users` | `/system/user` | **分页** `PageResult<UserVO>` | POST 增、PUT 改/停用/授权/永久重置、DELETE 批量 | 候选/临时是 POST；解锁是 GET；`get()` 无 id 打 `/system/user/`；资料三枪 `/profile*` |
| `roles` | `/system/role` | 分页 | POST 增、PUT 改/权限/停用/授权用户、DELETE 批量 | 已授/未授用户两份 list；`cancelUsers`/`selectUsers` 走 **params** |
| `menus` | `/system/menu` | **数组** `MenuVO[]`，不是 rows | POST 增、PUT 改、DELETE 单 id | `tree` / `roleTree`；另有 `cascadeDelete`；**没有** `getRouters` |
| `departments` | `/system/dept` | **数组** `DeptVO[]` | POST 增、PUT 改、DELETE 单 id | `excludeChildren`；`options` 把 id 拼进 URL |
| `posts` | `/system/post` | 分页 | POST 增、PUT 改、DELETE 批量 | `options` 用 **params** `{ deptId, postIds }`；自己的 `deptTree` |
| `clients` | `/system/client` | 分页 | POST 增、PUT 改/停用、DELETE 批量 | bind/rotate 是 POST；`options` **不再发新 URL** |
| `ssoApps` | `/system/ssoApp` | 分页 | POST 增、**POST `/update`**、POST 轮换 | **没有** delete / changeStatus / bind |
| `userTypes` | `/system/userType` | 分页 | POST 增、PUT 改、DELETE 批量 | `options` 是独立 GET `/options` |

**类比：** 把 `createSystemService` 想成邮局后面一排**贴了标签的抽屉**。窗口服务员（页面）不自己跑去车库开车。她把「用户列表」这张单子塞进标着 `users` 的抽屉。抽屉里已经印好：去 `/system/user/list`，盖 **GET** 章，查询条件放信封正面（`params`）。车库司机（厅堂 `HttpClient`）只负责把印好的信封送走。八个抽屉不是八家邮局：司机是同一个，车库也是同一座厅堂。

**类比失效处：**

1. 厨房里还有没贴在口试名单上的抽屉（`identity` / `resources` / `publicUsers`）。看见工厂别说「只有八口」。
2. 有的抽屉印的是「整袋分页」，有的印的是「一棵树／一张名单」。不要对 `menus.list` 去找 `data.rows`。
3. 标签都叫 `options` 时，做法可以完全不一样：有的拼进 URL，有的放 params，有的根本不发新请求。
4. 新楼规矩（API-005 / FE-CRUD-002）说「改东西用 POST」。这排抽屉**今天仍盖 PUT/DELETE**，因为后面的窗还是 RuoYi 风格。不要把厨房先改成 POST 而窗还是 PUT。
5. 司机不是厨房雇的。换 App 只换司机和钥匙，不换这八张印好的单子。

## 核心概念与机制

### 直觉讲解

小孩子版只记十句：

1. **先找工厂，再找抽屉。** `export function createSystemService(http: HttpClient)`。参数只有 `http`。没有 axios 类型。
2. **一张纸条四个格子。** 几乎每个方法都是 `request({ url, method, params?, data?, headers? })`。`method` 是小写 `'get' | 'post' | 'put' | 'delete'`（`platform-contracts` 的 `HttpMethod` 还有 `'patch'`，本工厂没用）。
3. **返回值默认是整封回信。** `ApiResponse<T>`：`code` / `data` / `msg`。只有 `identity.load*` 拆开 `.data` 再往外递；`clients.options` 拆开 `.data.rows`。
4. **分页抽屉看 `data.rows` 和 `data.total`。** 用户/角色/岗位/客户端/SSO 应用/用户类型。菜单和部门的 `list` 直接是数组。
5. **路径里的 id 要先盖印章。** 本地函数 `segment`：每个 id `encodeURIComponent`，再用逗号拼。测试故意用 `'client/1'`，路上变成 `client%2F1`。
6. **查询放 `params`，本体放 `data`。** 例外要单记：改用户角色、角色里「选全部 / 取消全部」走 `params`。
7. **`users.get` 不是「我是谁」。** 没带 id → `GET /system/user/`（表单初始化，L-015 那扇会打印默认密码的窗）。「我是谁」是 `identity.loadInfo` → `GET /system/user/getInfo`。
8. **`menus` 不管动态路由。** 路由菜单是 `identity.loadMenus` → `GET /system/menu/getRouters`。管理树是 `menus.tree` → `GET /system/menu/treeselect`。
9. **客户端 twin。** `clients.update` 是 PUT `/system/client`；`ssoApps.update` 是 POST `/system/ssoApp/update`。轮换两条路：kebab `/system/client/sso/rotate-secret` 对 `rotateSsoSecret`，camel `/system/ssoApp/rotateSecret` 对 `rotateSecret`。
10. **工厂没有的枪，页面会绕路。** 用户导入导出不在 `users.*`。`UserPage` 用 `runtime.download('system/user/export')` 和拼出来的 `/system/user/importData` 上传地址。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 系统领域工厂 | `createSystemService` | `frontend/packages/domains/system/src/service.ts` 导出函数。入参 `HttpClient`，返回 `Object.freeze` 的 `SystemService` |
| 系统领域服务 | `SystemService` | 同一文件里的接口。口试八口是它的资源字段，不是全部字段 |
| HTTP 端口 | `HttpClient` | `platform-contracts`：`request<T>(HttpRequest): Promise<T>`。domain 只依赖这个口 |
| 请求纸条 | `HttpRequest` | `url` + 小写 `method` + 可选 `params` / `data` / `headers` / `responseType` / `timeout` |
| 统一回信 | `ApiResponse<T>` | `code?`、必有 `data: T`、`msg?`、`error?`。定义在 `src/types.ts` |
| 分页袋 | `PageResult<T>` | `{ rows: T[]; total: number }`。只有声明了它的 `list` 才是分页袋 |
| 路径段编码 | `segment` | 工厂文件顶部局部函数。数组或单值 → `encodeURIComponent` → 逗号连接 |
| 资源抽屉 | resource port | `users` / `roles` / `menus` / `departments` / `posts` / `clients` / `ssoApps` / `userTypes` 八个 freeze 对象 |
| 共用 CRUD 形状 | `CrudService` | 同文件内部 interface：`list` / `get` / `add` / `update` / `delete`，可选 `changeStatus`。`roles` / `posts` / `clients` / `userTypes` 用它交差；`users` / `menus` / `departments` / `ssoApps` **手写**，不要假装它们签名一样 |
| 身份小口 | `identity` | `loadInfo` / `loadMenus`。解开 `.data`。L-014 已认；本格不 covered |
| 邻居查询口 | `publicUsers` / `UserQueryPort` | `src/user/public.ts`。URL 与 `users.list/options/departmentTree` 相同，但把 `SysUserVo` **投影**成 `UserSummary` |
| 传输投影 | transport projection | `projectResetPasswordCandidateTransport` / `projectTemporaryPasswordTransport`：只留下密码（临时还要正整数 `expiresInSeconds`），丢掉多余字段 |
| 选择项三种做法 | options flavors | URL 查询串 / `params` 对象 / 本地包装 `list` |
| 目标动词合同 | API-005 / FE-CRUD-002 | 新代码：查询 GET、变更 POST。本工厂存量仍 PUT/DELETE，与后端窗对齐 |

### 机制/因果链

#### 1. 厅堂怎么把卡车钥匙塞进厨房

admin：`application/services.ts` 先做 `domainHttp = { request: config => adminHttp.request(config) }`，再 `createSystemService(domainHttp)`。这层薄包装切断初始化环（L-007）。home 同样包一层 `homeHttp`，但后面**没有**把 `systemService.users` 交给任何 web-domain。

web-domain 不 import 工厂。`SystemWebRuntime.service: SystemService`。`createSystemWebDomain(runtime)` 只登记页面。页面写 `runtime.service.users.list`。所以改 URL 只改厨房；改谁开车只改 App。

厨房自己：`const request = <T = unknown>(config: HttpRequest) => http.request<ApiResponse<T>>(config)`。失败不吞。`index.test.ts` 有一则：`users.list` 在 `request` reject 时把**同一只** Error 抛出去，不填空表。

整座服务和每个抽屉都 `Object.freeze`。测试可以替换 `http.request`，不能运行时给 `service.users` 再挂一个方法。

#### 2. 八个抽屉，按资源看形状（不默写每一枪）

下面每口只记**形状 + 必须能指的例外**。完整 URL 快照在 `index.test.ts` 那则 71 条 `it`；口试按组，不按 71。

**`users`（前缀 `/system/user`）**

- 分页名单：`list(query)` → GET `/list` + `params`。
- 详情两态：`get(id?, clientId?)` → GET `/system/user/` 或 `/system/user/{id}`；有 `clientId` 才带 params。这是管理表单，不是身份口。
- 写：`add` POST 根路径；`update` PUT 根路径；`delete` DELETE 拼 id 列表。
- 状态：`changeStatus` PUT `/changeStatus`，body `{ userId, status }`。
- 授权：`authRoles` GET `/authRole/{id}?clientId=`；`updateAuthRoles` PUT `/authRole`，注意是 **`params`**。
- 部门旁路：`listByDepartment` GET `/list/dept/{id}`；`departmentTree` GET `/deptTree`。岗位抽屉另有自己的 `/system/post/deptTree`，不是同一枪。
- 钥匙三枪（L-015 已讲窗，本课认厨房纸条）：`resetPassword` PUT `/resetPwd`，头 `isEncrypt: true`、`repeatSubmit: false`；`passwordResetCandidate` POST `/resetPwd/candidate`，头 `Cache-Control: no-store` + `repeatSubmit: false`，再 `projectResetPasswordCandidateTransport`；`issueTemporaryPassword` POST `/temporaryPassword`，同样 no-store，再投影，要求 `expiresInSeconds` 是正整数。
- 解锁：`unlock` GET `/unlock/{id}`。动词是 GET，副作用在后端擦错次键（L-015）。厨房没有把它改成 POST。
- 资料三枪（后端类是 `SysProfileController`，L-022）：`profile` GET `/profile`；`updateProfile` PUT `/profile`；`updatePassword` PUT `/profile/updatePwd`，加密头与重置永久密相同。它们**长在 `users` 对象上**，口试时要能指 URL，但不要把资料页说成本课已经讲完改密策略。
- `options(ids)` GET，把 id 拼进 URL：`/optionselect?userIds=`。

**`roles`（前缀 `/system/role`）**

- 标准分页 CRUD + `changeStatus` PUT `/changeStatus` body `{ roleId, status }`。
- 权限单独一枪：`updatePermission` PUT `/permission`，`data` 是 `RoleForm`。
- 人与角色：`allocatedUsers` / `unallocatedUsers` 两份 GET list。单人取消 `cancelUser` PUT `/authUser/cancel` 走 **`data`**；批量取消 `cancelUsers`、批量选择 `selectUsers` 走 **`params`**（`/cancelAll`、`/selectAll`）。不要说「授权全是 data」。
- 数据范围树：`departmentTree(roleId)` GET `/deptTree/{roleId}`，返回 `RoleDeptTree`，不是部门管理那份数组。

**`menus`（前缀 `/system/menu`）**

- `list` 是数组，类型测试锁死 `Promise<ApiResponse<MenuVO[]>>`。
- `tree(clientId?)` GET `/treeselect`；`roleTree(roleId)` GET `/roleMenuTreeselect/{id}`。角色页点菜单树走后者。
- 删除两枪：`delete` 单 id；`cascadeDelete` DELETE `/cascade/{ids}`。
- **没有** `getRouters`。那一枪在 `identity.loadMenus`。

**`departments`（前缀 `/system/dept`）**

- `list` 也是数组。树在页面侧用这份名单长出来，厨房不先构树。
- `excludeChildren(id)` GET `/list/exclude/{id}`：编辑时不让选自己的子孙当新父节点。
- `delete` 单 id，不像用户那样一次逗号删一串。
- `options` 与用户/角色同一套路：id 进 URL 查询串 `?deptIds=`。

**`posts`（前缀 `/system/post`）**

- 分页 CRUD，批量 DELETE。
- `options(deptId?, postIds?)` GET `/optionselect`，条件在 **params 对象**里，不拼进 URL 字符串。用户页选岗位走这枪。
- `departmentTree` GET `/post/deptTree`。和 `users.departmentTree` 前缀不同。

**`clients`（前缀 `/system/client`）**

- 分页 CRUD；`changeStatus` PUT，body 用 **门牌字符串 `clientId`**，不是主键（L-018）。
- SSO：`bindSsoAccess` POST `/sso/bind` body `{ id, ssoAuthMode }`；`rotateSsoSecret` POST `/sso/rotate-secret` body `{ id }`。厨房**有**轮换这一枪；2026-09-16 的 `ClientPage.vue` **没调用它**，SSO 页走 `ssoApps.rotateSecret`。
- `options`：`clients.list({ pageNum: 1, pageSize: 1000 })`，然后 `data?.rows ?? []`。返回 `ClientVO[]`，不是 `ApiResponse`。菜单/角色/用户页用它填客户端下拉。HTTP 失败仍会抛；只有 `rows` 空才变成 `[]`。

**`ssoApps`（前缀 `/system/ssoApp`）**

- 五枪，和后端五扇窗对齐：list GET `/list`；get GET `/{id}`；add POST 根；update **POST `/update`**（不是 PUT）；rotateSecret POST `/rotateSecret`。
- 没有 delete、没有 changeStatus、没有 bind。停用/删除走 `clients`。菜单种子里的 `system:ssoApp:remove` 不是本工厂方法。
- `SsoAppPage.vue` 调这五枪；创建/修改成功后的一次性密钥在 VO 上，厨房不另做投影函数。

**`userTypes`（前缀 `/system/userType`）**

- 分页 CRUD。
- `options()` GET `/system/userType/options`，独立 URL，返回 `ApiResponse<UserTypeVO[]>`。admin 的 `notificationDirectory.userTypes` 直接借这一枪。

#### 3. 共用机关：编码、动词、投影、缺口

**路径编码。** `segment(['user/1','user/2'])` → `user%2F1,user%2F2`。71 条快照把这件事钉死。不要把 id 直接叠进模板字符串。

**动词存量。** 查询 GET。新增 POST 根路径。修改在七口里六口是 PUT 根路径；**唯独 `ssoApps.update` 是 POST `/update`**。删除 DELETE。停用 PUT `changeStatus`。SSO bind/rotate、用户候选/临时是 POST。解锁 GET。这是对照后端的抄写，不是新楼规范已经落地。

**params 还是 data。** 列表、带查询的授权名单 → `params`。JSON 本体（表单、停用、单人取消角色、bind/rotate、候选）→ `data`。`users.updateAuthRoles`、`roles.cancelUsers`、`roles.selectUsers` → `params`。看错格子会让后端拿不到 `userIds`。

**投影只发生在少数枪。** 候选/临时会丢掉 `ignored` 一类运输字段。`users.list` **不**投影，页面拿到的是 `UserVO`。`publicUsers.list` 才 `map(projectUserSummary)`。两口 URL 相同，返回形状不同。

**工厂缺口（有窗无抽屉方法）。** 用户 `export` / `importData` / `importTemplate` 在 Controller 上（L-015），厨房没有 `users.export`。客户端 `export` 同样不在 `clients`。页面走 `runtime.download` 或上传组件。补方法是以后的映射工作，不要因为工厂没有就否认后端窗。

**71 条不是全集。** `index.test.ts` 「preserves every migrated endpoint…」`expect(cases).toHaveLength(71)`。这 71 条覆盖八口里大多数 URL，但**不包括** `clients.rotateSsoSecret`、`users.passwordResetCandidate`、`users.issueTemporaryPassword`（后两枪另有 no-store 测试）、也不包括 `clients.options` 的包装行为（另有 rows 测试）。口试若说「71 = 工厂全部方法」，与磁盘不符。

### 图、表或文本图

**图题 / caption：** 一份厨房，八个口试抽屉，三只旁边的盒子不标 covered。alt：createSystemService 把 HttpClient 接到 users/roles/menus/departments/posts/clients/ssoApps/userTypes；identity、resources、publicUsers 画成虚线。

```text
                    App domainHttp.request
                              │
                 createSystemService(http)
                              │
     ┌────────┬────────┬──────┴──────┬────────┬────────┐
     │        │        │             │        │        │
   users    roles    menus      departments  posts   userTypes
 /system/user  /role  /menu      /dept        /post   /userType
     │                                              │
     │         clients ──────── ssoApps             │
     │        /client            /ssoApp            │
     │           │                  │               │
     │           └──── 同一张 sys_client 表（后端）─┘
     │
     ├─(虚线) identity.loadInfo  → GET /system/user/getInfo
     ├─(虚线) identity.loadMenus → GET /system/menu/getRouters
     ├─(虚线) publicUsers.list   → GET /system/user/list（投影摘要）
     └─(虚线) resources.*        → dict/config/oss/social
```

**文字等价物：** 图顶是厅堂塞进来的 `request`。中间八个实线盒子是本课格子。`clients` 与 `ssoApps` 在厨房是两个盒子，后端却是同一张表。虚线三只盒子仍由同一函数创建：身份口把 `.data` 拆掉给登录；`publicUsers` 复用用户名单 URL 但改形状；`resources` 是字典和文件柜，留给后面的课。图上没有 `createOpenApiService`、没有 monitor 子路径工厂。

**图的边界：** 不画 Vue 组件树。不保证每个 App 都调用八口——home 只接虚线身份口。不把 `getRouters` 画进 `menus`。不把导入导出画进 `users`。

**图题 / caption：** 同一句 `options`，三种信封。alt：users/roles/departments 把 id 拼进 URL；posts/userTypes 用 params 或独立路径；clients.options 本地调 list。

```text
「给我选项」

 A 拼进 URL
   users.options(['a/1'])     GET /system/user/optionselect?userIds=a%2F1
   roles.options              GET /system/role/optionselect?roleIds=...
   departments.options        GET /system/dept/optionselect?deptIds=...

 B params / 独立路径
   posts.options(deptId, ids) GET /system/post/optionselect   params: { deptId, postIds }
   userTypes.options()        GET /system/userType/options

 C 不新开窗
   clients.options()          内部 GET /system/client/list?pageNum=1&pageSize=1000
                              返回 rows 数组，不是 ApiResponse
```

**文字等价物：** 名字都叫 options，司机收到的信封不一样。A 把逗号分隔的编码 id 写在问号后面，厨房自己做字符串，不走 `params` 对象。B 要么把过滤条件放 params，要么打一条专门的 options 路径。C 根本不增加后端窗，只是把已经存在的分页名单拆开；页面拿到的是数组，调用方不要再写 `.data.rows`。

**图的边界：** 不保证 `pageSize: 1000` 永远够用——这是当前源码常量，不是后端上限合同。不把 `publicUsers.options` 画进 C：它仍打 `/optionselect`，然后投影摘要。

**图题 / caption：** 三扇「用户」窗，厨房分属两个抽屉。alt：getInfo 在 identity；/system/user/ 在 users.get；/profile 在 users.profile。

```text
「我是谁 / 这个人是谁 / 我的资料」

 identity.loadInfo     GET /system/user/getInfo     → 解开 .data 给登录后会话
 users.get()           GET /system/user/            → 管理端新建表单（可能带默认密码）
 users.get(id)         GET /system/user/{id}        → 管理端编辑表单
 users.profile         GET /system/user/profile     → 自己改资料（L-022 的窗）
```

**文字等价物：** 四条 URL 都在 `/system/user` 底下，但厨房入口不同。登录后问「我是谁」必须走身份口，L-014 的 `getInfo` 就是它。管理页问「这个人」走 `users.get`。自己改头像/昵称/自己的密走 `profile*`。把 `users.get()` 说成身份口，会打到会打印候选密码的那扇管理窗。

**图的边界：** 不展开 `getInfo` 的权限与 DataPermission（L-015）。不展开 `updatePwd` 要旧密码（L-022）。不把 `GET /unlock/{id}` 画进这四扇。

### 正例、反例与边界

**正例 1：** 打开 `service.ts` 的 `createSystemService`。用手指点返回对象的八个实线键：`users` `roles` `menus` `departments` `posts` `clients` `ssoApps` `userTypes`。再点三个本格不口试的键：`identity` `resources` `publicUsers`。

**正例 2：** 71 条快照。`index.test.ts` 从 `service.clients.list` 走到 `service.posts.departmentTree`，`expect(requests).toEqual(cases.map(...))` 且长度为 71。把 `'client/1'` → `client%2F1` 圈出来。把 `ssoApps.update` 的 `method: 'post'` 和 url `/system/ssoApp/update` 圈出来。

**正例 3：** 分页形状锁在类型上。同文件 `retains concrete DTO...`：`users.list` / `roles.list` / `posts.list` / `userTypes.list` / `clients.list` 都是 `PageResult`；`menus.list` 是 `MenuVO[]`；`departments.list` 是 `DeptVO[]`。

**正例 4：** `clients.options` 拆袋。`keeps table responses at response.data.rows`：`list` 仍是 `{ data: page }`；`options()` 等于 `page.rows`。

**正例 5：** 候选/临时投影。`uses no-store POST...`：请求头含 `Cache-Control: no-store`；响应里的 `ignored` 不会出现在 `data`。这是厨房唯一对用户凭据做的字段收窄。

**正例 6：** 失败不编造。`does not rewrite a failed user query with fallback data`：reject 原 Error。不要把 `clients.options` 的 `?? []` 推广成「列表失败就空表」。

**正例 7：** 页面按抽屉调用。`UserPage.vue` 把 `users.list/get/add/update/delete/resetPassword/changeStatus/unlock/departmentTree` 起本地别名；凭据对话框走 `passwordResetCandidate` / `resetPassword` / `issueTemporaryPassword`。`SsoAppPage.vue` 走 `ssoApps.list/get/add/update/rotateSecret`。`ClientPage.vue` 走 `bindSsoAccess`，**不**走 `rotateSsoSecret`。

**正例 8：** 导入导出绕过厨房。`UserPage.vue`：`requestDownload('system/user/export', ...)`；上传 `url: import.meta.env.VITE_APP_BASE_API + '/system/user/importData'`。`SystemService.users` 类型上没有 export/import。

**正例 9：** home 创建工厂却不用八口。`home-web/.../services.ts` 只有 `identity: systemService.identity`。仓库内 home-web 没有 `systemService.users`。

**正例 10：** App 组件也可以直接叫厨房。`admin-web/src/components/RoleSelect/index.vue` import `systemService`，打 `roles.list` / `roles.options`，不经过 `createSystemWebDomain`。厅堂允许这样接；厨房仍然是唯一写 URL 的地方。

**反例 1：** 「OBJ-19 没写 `ssoApps`，所以那口不算。」chain 与本课 `claimed_cells` 含它。它和 `clients` 的动词差（POST `/update`、无 delete）正是对照题。

**反例 2：** 「`createSystemService` 的方法都在 71 条里。」磁盘上 `rotateSsoSecret`、两支凭据 POST 不在那则 `cases` 数组。

**反例 3：** 「`users.get()` 就是登录后的 getInfo。」URL 差一个 `getInfo` 路径段，返回值也不拆 `.data`。

**反例 4：** 「`menus.list` 取 `res.data.rows`。」类型是数组。角色菜单树要 `menus.roleTree`，不是 `list`。

**反例 5：** 「改菜单路由调 `menus.update`。」动态路由来自 `getRouters`，厨房在 `identity`，消费在 L-020。

**反例 6：** 「`clients.options` 是 GET `/system/client/options`。」没有这条 URL。

**反例 7：** 「八口都实现 `CrudService`。」`users.get` 的 id 可空；`menus`/`departments` 的 list 不是 `PageResult`；`ssoApps` 没有 `delete`，update 不是 PUT。

**反例 8：** 「变更已经全部 POST，因为 FE-CRUD-002 这么写。」Skill 是目标合同；本文件存量 PUT/DELETE 与 Controller 一致。改动词必须前后端一起改，不是厨房单方面「纠正」。

**反例 9：** 「`publicUsers` 就是 `users` 的别名。」同 URL，投影不同，类型在 `user/public.ts`。本格不 covered。

**反例 10：** 「OpenAPI / 监控也在 `createSystemService` 里。」`createOpenApiService` 同包另一工厂；监控是 `@namewta/domain-system/monitor` 的 `createMonitorService`（L-033 / L-024）。

**边界：** 本课不验证运行中的网关鉴权是否拒绝 PUT——那是后端窗的权限串。本课不保证 `pageSize: 1000` 的 options 包装在大数据量下够用。本课不把 web-domain 的 `permissions` 清单（含 `ssoApp: remove`）说成厨房有 delete。

## 变式与迁移

- **变式 A：要加一枪管理 API。** 先看后端窗的动词和路径，再在对应抽屉加一行 `request({ url, method, ... })`，再往 71 条快照（或专门测试）加一条。不要在 Vue 里手写 axios。不要先改 `CrudService` 硬塞不合适的签名。

- **变式 B：第二个 App 只要身份。** 学 home：仍然可以 `createSystemService`，只把 `.identity` 插到 `createIdentityAccessService`。不要为了「工厂里有 `users`」去挂系统管理页。

- **变式 C：下拉框要客户端名单。** 用 `clients.options`（包装 list），不要发明 `/client/options`。若 1000 行不够，那是改包装策略，不是后端已经有第三条 list。

- **变式 D：角色里给一群人授权。** 对 `selectUsers` / `cancelUsers` 检查调用方传的是对象字段，因为厨房放进 `params`。单人取消才是 `data`。

- **变式 E：SSO 轮换按钮做在哪一页。** 两枪都在厨房：`clients.rotateSsoSecret` 与 `ssoApps.rotateSecret`。当前 SSO 管理页走后者。不要说厨房缺 Client 轮换；缺的是页面调用。

- **变式 F：导入导出。** 继续走 host `download` / 上传地址，或以后显式加工厂方法。不要把「L-015 有 export」说成「`users.export` 已经存在」。

- **变式 G：邻居只要用户摘要。** 走 `publicUsers`（投影），不要让通知/选人控件依赖完整 `UserVO`。URL 仍是 `/system/user/list`。

- **变式 H：API-005 迁移某一口。** 必须同时改 Controller 动词、OpenAPI、本工厂 `method`、71 条快照、页面若写死了 method 的测试。只改厨房会 405。

- **迁移口诀：** 先问哪个抽屉 → 再问 list 是袋还是数组 → 再问 id 进路径还是进 params → 再问这枪是不是身份/资料/导入导出那些旁路。跳步就会把 `get()` 当成 getInfo，把 options 当成同一条 URL，把 PUT 说成已经全部变成 POST。

## 常见误区

1. **「`createSystemService` 口试等于矩阵那一行上的 `identity, resources`。」** 本课 claimed 八口。身份已由 L-014 认；resources 留给配置/OSS 课。
2. **「八个抽屉覆盖 wta-system 全部 HTTP。」** 缺导入导出、缺 monitor、缺 OpenAPI、缺社交列表（在 `resources.social` / L-022）。
3. **「`users` 只打 `SysUserController`。」** 候选、临时、资料页都在同一前缀不同类。
4. **「`ssoApps.update` 也是 PUT。」** POST `/system/ssoApp/update`。
5. **「`clients.options` 会打 options 路径。」** 包装 `list`。
6. **「菜单 `list` 能当动态路由。」** 路由是 `getRouters`。
7. **「所有 delete 都接受 id 数组。」** 菜单、部门是单 id；SSO 应用没有 delete。
8. **「`departmentTree` 只有一枪。」** 用户、岗位、角色三套路径，返回类型也不一样。
9. **「加密头只出现在登录。」** `users.resetPassword` 与 `users.updatePassword` 也带 `isEncrypt: true`。
10. **「71 条测试包含轮换密钥和临时密码。」** 不包含 `rotateSsoSecret`；临时/候选另测。
11. **「web-domain 自己拼 `/system/user/list`。」** 页面调抽屉。例外是 download/upload 的宿主旁路。
12. **「home 没有 `createSystemService`。」** 有，只喂身份。
13. **「`CrudService` 是运行时基类。」** 只是 TypeScript 交差类型，编译期形状。
14. **「FE-CRUD-002 已经让本文件没有 PUT。」** 打开源码，`method: 'put'` 仍在。

## 非评分暂停

打开磁盘，不要凭记忆默写七十条 URL。不要改文件。没有标准答案栏。

1. 打开 `frontend/packages/domains/system/src/service.ts`。在 `return Object.freeze({...})` 上把八个口试键圈出来，把 `identity` / `resources` / `publicUsers` 另外做记号。
2. 在每个口试抽屉里只圈三件事：`list` 的 url 与返回类型（分页还是数组）、`add`/`update` 的 method、有没有 `delete`。特别把 `ssoApps.update` 的 POST `/update` 写下。
3. 打开 `src/index.test.ts`。确认 71 条长度。搜 `rotate-secret`、`resetPwd/candidate`、`temporaryPassword` 是否出现在那则 `cases` 里。再看 no-store 那则 `it`。
4. 对照三扇用户窗：`identity.loadInfo`、`users.get`、`users.profile` 三条 url。对照两扇菜单窗：`identity.loadMenus` 与 `menus.tree`。
5. 打开 `UserPage.vue` 的 `handleExport` / 上传 `url`，确认它们不经过 `users.export`。打开 `ClientPage.vue` 解构列表，确认有 `bindSsoAccess`、没有 `rotateSsoSecret`。打开 `SsoAppPage.vue` 确认 `rotateSecret`。
6. 打开 `platform/contracts` 的 `HttpMethod`。再在 `service.ts` 搜 `method: '`，看实际出现了哪些小写动词。

## 总结、词汇表与下一步

- **工厂在 `frontend/packages/domains/system/src/service.ts`。** `createSystemService(http)` 冻住一座厨房。厅堂只注入 `HttpClient`。
- **口试八口：** `users` `/system/user`；`roles` `/system/role`；`menus` `/system/menu`（list 是数组，不管 getRouters）；`departments` `/system/dept`（list 是数组）；`posts` `/system/post`；`clients` `/system/client`；`userTypes` `/system/userType`；`ssoApps` `/system/ssoApp`（update 为 POST，无 delete）。
- **映射习惯：** id 走 `segment`；查询 `params`、本体 `data`（授权那几枪例外）；分页看 `rows/total`；`options` 有三种信封；凭据两枪 no-store 并投影。
- **不要吞并的旁路：** `identity` 两 URL；`users.profile*`；download/import；`publicUsers` 投影；`resources`；另一座 OpenAPI/monitor 工厂。
- **动词：** 磁盘仍是 GET/POST/PUT/DELETE 混用，对齐 classic 窗，不是 API-005 已完成的证据。

词汇表：`createSystemService` / `SystemService` / `HttpClient` / `HttpRequest` / `ApiResponse` / `PageResult` / `segment` / `CrudService` / resource port / `identity` / `publicUsers` / transport projection / options flavors / `bindSsoAccess` / `rotateSsoSecret` / `rotateSecret`。

下一步：L-020 看 `createSystemWebDomain` 和 admin 导航 host 怎样消费 `getMenus`。L-021 起才打开 `resources` 里的配置和字典。L-022 才把 `users.profile*` 对上 `SysProfileController`。L-024 / L-033 才是 monitor 与 OpenAPI 那两座工厂。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/{admin-web,home-web}` | admin 组合 `createSystemService`；home 同工厂只喂 `identity`；RoleSelect 直接打 `roles.*` | `application/services.ts`；`components/RoleSelect` | 2026-09-16 |
| S-006 | `frontend/packages/{domains,web-domains,platform}` | domain 厨房、web-domain runtime、平台 HTTP 端口 | `domains/system`；`web-domains/system`；`platform/contracts` | 2026-09-16 |
| S-L019-01 | `frontend/packages/domains/system/src/service.ts` | 八口 URL/动词；`segment`；`CrudService` 交差；identity 解 `.data`；`clients.options` 包装 list；凭据头与投影调用；`ssoApps.update` POST | `createSystemService`；`SystemService` | 2026-09-16 |
| S-L019-02 | `frontend/packages/domains/system/src/index.test.ts` | 71 条快照与编码；rows/total；失败不填空；no-store 投影；list 返回类型；`systemSsoAppResource` | 各 `it(...)` | 2026-09-16 |
| S-L019-03 | `frontend/packages/domains/system/src/transport.ts` | 候选只留 `password`；临时还要正整数 `expiresInSeconds`；否则抛「用户凭据响应不可用」 | `projectResetPasswordCandidateTransport` / `projectTemporaryPasswordTransport` | 2026-09-16 |
| S-L019-04 | `frontend/packages/domains/system/src/user/public.ts` | `publicUsers` 同 URL 投影 `UserSummary`；本格不 covered | `createUserQueryPort` | 2026-09-16 |
| S-L019-05 | `frontend/packages/domains/system/src/index.ts`；`src/client/index.ts` | 工厂导出；`createOpenApiService` 是另一导出；`systemSsoAppResource.basePath='/system/ssoApp'` | 根 barrel；`systemSsoAppResource` | 2026-09-16 |
| S-L019-06 | `frontend/packages/web-domains/system/src/{runtime.ts,index.ts,user/UserPage.vue,user/UserCredentialDialogs.vue,client/ClientPage.vue,sso-app/SsoAppPage.vue}` | 页面只拿 `runtime.service`；export/import 旁路；Client 调 bind 不调 client rotate；SsoApp 调五枪 | `SystemWebRuntime.service`；各页方法别名 | 2026-09-16 |
| S-L019-07 | `frontend/packages/platform/contracts/src/index.ts` | `HttpMethod` 小写五值；`HttpClient.request` | `HttpRequest` / `HttpClient` | 2026-09-16 |
| S-L019-08 | `.agents/skills/engineering-standards/references/typescript/crud-api-and-pages.md` FE-CRUD-002；L-015 / L-018 课文 | 变更 POST 是目标合同；用户三份 Controller 与 Client/SsoApp 窗的真实动词；本工厂按窗抄写 | FE-CRUD-002；L-015 变式 G；L-018 两窗表 | 2026-09-16 |
