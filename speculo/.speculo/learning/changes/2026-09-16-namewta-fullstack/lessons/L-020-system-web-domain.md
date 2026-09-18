---
lesson_id: L-020
objective_ids: [OBJ-20]
claimed_cells: [A:createSystemWebDomain, B:admin-web navigation host]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: factory-and-keys
    minutes: 9
  - segment: host-consumes-menus
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-005, S-006, S-007, S-010, S-L020-01, S-L020-02, S-L020-03, S-L020-04, S-L020-05, S-L020-06, S-L020-07]
---

# Lesson 020：菜谱板与前台挂牌——createSystemWebDomain 怎样被导航/权限 host 吃掉

## 学完你能做什么

打开 `frontend/packages/web-domains/system/src/index.ts` 的 `createSystemWebDomain`，再打开管理端那三处 host，你能**口述服务端菜单怎么变成可点的页**，而不是把「页面包」和「路由表」说成同一件事。

本课认两格：

1. **`A:createSystemWebDomain`**：工厂返回冻住的 `WebDomainManifest`。磁盘上 **15** 条 `componentKey`（不是 L-009 随口说的「十条」），外加一份权限**目录**。它**不** `addRoute`，**不**读 `Admin-Token`，**不**自己打 `/system/menu/getRouters`。
2. **`B:admin-web navigation host`**：厅堂三件套一起消费服务端菜单与会话权限。口试要能指文件，不要合成一个叫 `navigationHost` 的函数——磁盘上没有这个符号。

厅堂三件套（本课统称 host，名字以磁盘为准）：

| 厅堂文件 | 它真正干的事 |
| --- | --- |
| `apps/admin-web/src/permission.ts` | `restoreProtectedNavigation`：先 `getInfo`，再拉菜单投影，非外链 `addRoute`，最后 `replace` |
| `apps/admin-web/src/store/modules/navigation.ts` | `generateRoutes()`：`identityAccessService.getMenus()` → `projectServerRoutes` → `resolveAdminWebRegistration` → `adaptServerMenuRoutes` |
| `apps/admin-web/src/application/access.ts` + `directive/index.ts` | 权限 host：`createAccessEvaluator` 喂给 `installWebPermissionHost`（`v-hasPermi` / `v-hasRoles`）；同一把尺再经 runtime 口喂给页面 |

还有两份**同名容易撞车**的文件，口试必须拆开：

- `apps/admin-web/src/application/host/navigation.ts` 是 **tab 托盘**（关页/开页/刷新），被 `SystemWebRuntime.closeCurrentPage` / `closeAndOpenPage` 调用。它**不**拉 `getRouters`。
- `createMonitorWebDomain` 住在同一包 `src/monitor/index.ts`，manifest id 是 `web-domain-system-monitor`。监控四页是 L-024，**不是**本工厂的 15 键。

本课**不讲** `createSystemService.users/roles/menus/...` 的 HTTP 全表（L-019）、`SysMenuController` 除 `getRouters` 以外的 CRUD（L-016）、OpenAPI 工作区字段（L-033）、OSS 直传（L-029）。今天只认：**菜谱板怎么挂、服务员怎么按客单上菜、按钮怎么按优惠券藏起来。**

## 先把宏观地图放在桌上

L-007 把厨房插头插进 `services.ts`。L-009 把 `composeAppRuntime` / `restoreProtectedNavigation` / `createAccessEvaluator` 认成端口。L-014 已经走到 `getMenus()` = `GET /system/menu/getRouters`。本课站在 **web-domain 这一头**：菜单 JSON 里的 `component` 字符串，怎样对上 Vue 页。

```text
sys_menu.component          种子：'system/user/index'
        │
        v
GET /system/menu/getRouters  已按 userId + clientPk 裁剪（后端授权）
        │  RouterVo.component 仍是那串键
        v
identityAccessService.getMenus()   ← domain-admin 解析冻结为 ServerMenuNode
        │
        v
navigation.generateRoutes()
  projectServerRoutes({
    specialComponents: Layout / ParentView / InnerLink,
    resolveRegistration → resolveAdminWebRegistration
  })
        │
        ├─ 键是 'Layout'/'ParentView'/'InnerLink' → 厅堂壳组件
        ├─ 键在已选 manifest 里 → registration.load（页面外包一层 runtime）
        └─ 键找不到 → ManifestRouteDiagnostic，不是 glob 扫 views/
        v
permission.ts restoreProtectedNavigation
  getInfo → generateRoutes → addRoute(非 http) → replace
        │
        v
页面 props.runtime
  service / openApi / hasPermission / 弹窗 / 下载 / 字典 / tab 托盘
  按钮：v-hasPermi 或 runtime.hasPermission
        │
        v
权限快照来自 getInfo.roles / getInfo.permissions
  不是来自 manifest.permissions，也不是来自 RouterVo（后端 RouterVo 根本没有 permissions 字段）
```

NAMEWTA 把旧式「`import.meta.glob('/src/views/**')` + `filterAsyncRouter` + `usePermissionStore`」拆掉了。门禁 `admin-navigation-boundary` 禁止这些退役符号回到 `apps/admin-web/src/`。动态页必须走**已选** manifest。

**类比：** 把 `createSystemWebDomain` 想成中央厨房贴在墙上的 **菜谱板**：上面写「我们会做这 15 道菜」，每道菜有编号（`componentKey`）和过敏原清单（权限字符串目录）。前台（admin-web）决定把哪几块菜谱板挂出来。客人进门后，后厨按 **这张客单**（`getRouters`，已经按人和 Client 裁过）出菜。前台拿客单上的菜名去对菜谱板：对得上就上那道菜；对不上就摆一张「本店不供应」的告示牌（诊断页），**不会**跑进仓库把隔壁店的菜偷出来。优惠券（`getInfo` 的 permissions）决定桌上要不要摆「改密」「删除」这些小按钮。

**类比失效处：**

1. 菜谱板**不是**今天的客单。工厂不会因为你登录了就少贴两道菜。少的是 `getRouters` 返回值。
2. 过敏原清单（`manifest.permissions`）**不是**客人身上的券。`composeAppRuntime.permissionContributions()` 只是目录；活快照在 Pinia `user.permissions`。
3. tab 托盘（`application/host/navigation.ts`）不是服务员点菜。关页/开页发生在菜已经上桌之后。
4. home-web 也有 `restoreProtectedNavigation`，但它**没选** `web-domain-system`。同一张 `system/user/index` 客单在门户对不上菜。
5. 「失败关闭」不是「缺键就抛错、整次登录作废」。缺键会变成诊断组件，`generateRoutes` 仍然返回、`addRoute` 仍然挂上。身份解析失败、菜单 JSON 坏了，才会走 `logout`。

## 核心概念与机制

### 直觉讲解

小孩子版只记八句：

1. **厨房出品页，厅堂挂路由。** `createSystemWebDomain` 只登记「这串键对应哪张 Vue」。Vue Router 在 App。
2. **键是合同，不是文件路径。** 种子 `sys_menu.component`、后端 `RouterVo.component`、前端 `componentKey` 必须是同一串，例如 `system/user/index`。文件可以搬家到 `src/user/UserPage.vue`，键不许跟着机械改。
3. **先选板，再对键。** `composeAppRuntime` 的 `selectedManifestIds` 不含 `web-domain-system`，这 15 键对管理端等于不存在。
4. **客单已经裁过。** `getRouters` 用当前登录的 `userId` + `clientPk`。前端**不再**按角色把路由滤一遍。
5. **特殊壳先认。** `Layout` / `ParentView` / `InnerLink` 是厅堂组件名，不是 web-domain 键。
6. **按钮有两扇门。** 模板走 `v-hasPermi`（App 装的指令）；脚本走 `runtime.hasPermission`。两扇门敲同一把尺。下拉延迟节点指令不生效，必须走脚本。
7. **刷新只恢复一次。** 守卫看见 `roles.length === 0` 才跑恢复链。`getInfo` 会把 roles 写成至少 `ROLE_DEFAULT`，后面的跳转不再重拉菜单。
8. **监控是隔壁板。** 同一 npm 包里还有 `createMonitorWebDomain`。不要把 `monitor/online/index` 说成本课 15 键。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 系统 Web 领域工厂 | `createSystemWebDomain` | `web-domains/system/src/index.ts` 导出。吃 `SystemWebRuntime`，返回 `Object.freeze` 的 `WebDomainManifest<Component>`。id 固定 `web-domain-system`，`domainId` 固定 `system` |
| Web 领域清单 | `WebDomainManifest` | `id` / `domainId` / `messages` / `permissions` / `registrations`。platform-app-runtime 的形状，不是 Vue Router 的 `RouteRecordRaw` |
| 组件键 | `componentKey` | 与后端菜单 `component` 对齐的稳定字符串。本工厂 15 条，见下表 |
| 登记项 | `WebRegistration` | `{ id, componentKey, componentName, load }`。`load` 是懒加载函数，**组合时不调用** |
| 运行时注入包装 | `runtimeView` | 工厂内部：`defineComponent({ name, setup: () => () => h(page, { runtime }) })`。页面用 `defineProps<{ runtime: SystemWebRuntime }>()` 接收 |
| 系统页面运行时 | `SystemWebRuntime` | 厅堂填的端口袋：`service`、`openApi`、壳组件、弹窗、下载、字典、tab、配置、`hasPermission`、密码策略、剪贴板、上传头。页面不 import App |
| 应用运行时 | `AppRuntime` | `composeAppRuntime` 的冻结结果。只收 `selectedDomainIds` × `selectedManifestIds`。`resolve({ componentKey, domainId })` **按键查找**；`domainId` 主要用于缺键时报错 |
| 领域模块描述 | `systemDomainModule` | `domains/system` 的 `{ id:'system', backendModules:['wta-system'], capabilities:[...] }`。capabilities **没有** `openApi` / `ssoApp` 这两项，不要拿它当 15 键对照表 |
| 服务端菜单节点 | `ServerMenuNode` | domain-admin 解析 `getRouters` 后的冻结树。`path` 必填非空；`component` 可选字符串 |
| 导航投影 | `projectServerRoutes` | platform：先按需拍平 `ParentView`，再 `assembleServerRoutes` 把字符串组件换成壳或 `load` 或诊断 |
| 领域推断 | `inferDomainId` | `componentKey.split('/').filter(Boolean)[0]`。`system/user/index` → `system`；`monitor/online/index` → `monitor`（即使监控 manifest 的 `domainId` 是 `system`） |
| 导航恢复 | `restoreProtectedNavigation` | 端口化顺序：`loadIdentity` → `loadRoutes` → 非外链 `addRoute` → `createReplacement`。一步抛错整段失败 |
| 导航 Store | `useNavigationStore` | Pinia `navigation`。权威动作是 `generateRoutes`。退役名 `usePermissionStore` 不得回来 |
| 权限求值器 | `AccessEvaluator` | `createAccessEvaluator({ permissions, roles })` 的纯查询。超管角色 `superadmin` 或遗留 `admin` 对**角色**询问放行；权限询问认 `*:*:*` 或精确字符串 |
| 权限指令宿主 | `installWebPermissionHost` | web-kit：注册 `v-hasPermi` / `v-hasRoles`。provider 每次 mounted 现取 evaluator，不缓存快照 |
| 权限贡献 | `WebPermissionContribution` | manifest 上的目录 `{ id, permissions[] }`。本课 13 组。**不是**当前用户已授权集合 |
| 诊断页 | `ManifestRouteDiagnostic` | 缺键时挂上的告示组件，带 `appId` / `code` / `componentKey` / `domainId` |
| tab 托盘 | `application/host/navigation.ts` | `refreshPage` / `closePage` / `closeOpenPage` / …。消费 tagsView + router，不消费 `getRouters` |

**createSystemWebDomain ≠ createSystemService。** 一个出品 Vue 页清单，一个出品 HTTP 方法。厅堂把后者塞进前者的 `runtime.service`。

**createSystemWebDomain ≠ createMonitorWebDomain。** 两个工厂、两个 manifest id、两套键。管理端 `selectedManifestIds` 两个都选了。本课口试只考前者。

**getRouters ≠ getInfo。** 前者是菜单树（导航 host 的食材）；后者是身份 + roles + permissions（权限 host 的食材）。恢复链两个都要，但不要说「菜单接口带回按钮权限」。

**`v-hasPermi` ≠ `runtime.hasPermission`。** 一个是指令拆 DOM，一个是脚本布尔。尺是同一把。

### 机制/因果链

#### A. 工厂只贴菜谱板

`createSystemWebDomain(runtime)` 先写死一张 15 行表，再冻成 manifest。每一行是 `[registrationId, componentKey, componentName, () => import(页面)]`。`load` 真正执行时才 `runtimeView`：动态 import 页面，包一层只负责 `h(page, { runtime })` 的组件。

磁盘 15 键（测试 `publishes all server-facing governance keys` 的期望顺序，不要重排）：

| registration id | componentKey | componentName | 页面 |
| --- | --- | --- | --- |
| `system-client` | `system/client/index` | `Client` | `client/ClientPage.vue` |
| `system-sso-app` | `system/ssoApp/index` | `SsoApp` | `sso-app/SsoAppPage.vue` |
| `system-user` | `system/user/index` | `User` | `user/UserPage.vue` |
| `system-user-auth-role` | `system/user/authRole` | `AuthRole` | `user/UserAuthRolePage.vue` |
| `system-user-type` | `system/userType/index` | `UserType` | `user-type/UserTypePage.vue` |
| `system-role` | `system/role/index` | `Role` | `role/RolePage.vue` |
| `system-role-auth-user` | `system/role/authUser` | `AuthUser` | `role/RoleAuthUserPage.vue` |
| `system-menu` | `system/menu/index` | `Menu` | `menu/MenuPage.vue` |
| `system-dept` | `system/dept/index` | `Dept` | `dept/DepartmentPage.vue` |
| `system-post` | `system/post/index` | `Post` | `post/PostPage.vue` |
| `system-dict` | `system/dict/index` | `Dict` | `dict-type/DictPage.vue` |
| `system-config` | `system/config/index` | `Config` | `config/ConfigPage.vue` |
| `system-open-api` | `system/openApi/index` | `OpenApi` | `open-api/OpenApiAdminPage.vue` |
| `system-oss` | `system/oss/index` | `Oss` | `oss/OssPage.vue` |
| `system-oss-config` | `system/oss/config` | `OssConfig` | `oss-config/OssConfigPage.vue` |

这 15 串在 `50-cde-base-dml.sql` 的 `sys_menu.component` 里都能找到。合同是这串键，不是 `src/user/UserPage.vue`。

权限目录 13 组（`authRole` / `authUser` **没有**独立组，它们复用 `system:user:*` / `system:role:*`）：

| 组 id | 动作 → 权限串 |
| --- | --- |
| `system-client` | list/query/add/edit/remove/export → `system:client:*` |
| `system-ssoApp` | list/query/add/edit/remove |
| `system-user` | list/query/add/edit/remove/export/**import**/**resetPwd**/**temporaryPassword** |
| `system-userType` | list/query/add/edit/remove/export |
| `system-role` | list/query/add/edit/remove/export |
| `system-menu` | list/query/add/edit/remove（无 export） |
| `system-dept` | list/query/add/edit/remove |
| `system-post` | list/query/add/edit/remove/export |
| `system-dict` | list/query/add/edit/remove/export |
| `system-config` | list/query/add/edit/remove/export |
| `system-openApi` | **self**/list/query/add/edit/remove |
| `system-oss` | list/query/**upload**/**download**/edit/remove |
| `system-ossConfig` | list/query/add/edit/remove |

文案贡献只有命名空间 `systemAdmin`：`title` / `client` / `organization`。管理端主 i18n 仍是 `apps/admin-web/src/lang/`。不要把这三句说成「系统页的全部中文」。

因果：工厂在**组合期**就把键和权限目录冻住。测试「registers only for an explicitly selected App composition」证明：`selectedManifestIds: ['web-domain-system']` 时 `componentKeys()` 含 `system/user/index`；空选择集得到 `[]`。`load` 在 `composeAppRuntime` 时**不会**被调用。

#### B. 厅堂把 runtime 塞进工厂，再把工厂塞进 compose

`adminManifestRegistry.ts` 造 `adminSystemWebRuntime`，再：

```ts
const systemManifest = createSystemWebDomain(adminSystemWebRuntime);
```

runtime 袋里与本课有关的口：

- `service: systemService`、`openApi: openApiService`（L-007 接线板上已经造好的单例）
- `hasPermission: permission => createAdminAccessEvaluator().hasPermission(permission)` —— **现取** Pinia，不冻一份旧快照
- `closeCurrentPage` / `closeAndOpenPage` → **tab 托盘** `application/host/navigation.ts`
- `passwordPolicy.load` → `requirePasswordPolicy(await identityAccessService.getClientContext())`
- `copyText` → `navigator.clipboard.writeText`（没有 Clipboard API 就抛）
- 弹窗/下载/字典/OSS 内容替换/上传头：全是厅堂实现，页面只认口

然后 `composeAppRuntime({ appId: 'admin-web', ..., selectedManifestIds: [..., 'web-domain-system', 'web-domain-system-monitor', ...] })`。

`resolveAdminWebRegistration(componentKey, domainId)` 调 `runtime.resolve`。缺键或（防御性）`unselected-domain` 时返回 `undefined`，让投影层去挂诊断；别的 `AppRuntimeError` 继续抛。工作树里 `composeAppRuntime.resolve` **实际只按 `componentKey` 查找**，缺了只抛 `missing-component-key`。`domainId` 对不上也不会在这一层拒绝——`system/user/index` 用 `'foo'` 当 domainId 仍能拿到 User 页。口试不要发明「domainId 必须匹配才解析」。

#### C. 服务端菜单怎么变成 Vue 路由

1. 后端 `SysMenuController.getRouters`：没有 `LoginUser` 或没有 `clientPk` → `R.fail("当前登录缺少客户端上下文")`。有则 `selectMenuTreeByUserId(userId, clientPk)`，再 `buildMenus`。`SysMenu.getComponentInfo()` 决定 `RouterVo.component`：有菜单组件且不是 menu-frame 就用列上的键；空组件的目录可能变成 `ParentView`；一级目录默认 `Layout`；内链 `InnerLink`。
2. `RouterVo` **没有** `permissions` 字段。前端 `ServerMenuNode.permissions` 是解析器预留的可选字段；真实 `getRouters` JSON 通常不带它。导航测试里的 `permissions: ['server:metadata-only']` 是夹具，证明投影**不删**服务端元数据，不是证明后端会发按钮权限。
3. `identityAccessService.getMenus()` 走注入的 `identity.loadMenus()` → `GET /system/menu/getRouters`，坏树抛 `invalid-menu-response`（L-014）。
4. `generateRoutes` 把同一棵树投影 **三次**：
   - `sidebarRoutes = projectMenus(menus)`（`flattenParentView=false`）→ 侧栏树，保留 `ParentView` 分组
   - `rewriteRoutes = projectMenus(menus, true)` → **拍平** `ParentView`，子路径拼成 `group/leaf`，这是 `addRoute` 的食材
   - `projectedDefaultRoutes = projectMenus(menus)` → 顶栏
5. `setRoutes(rewriteRoutes)` 会把结果拼到 `constantRoutes` 后面。`constantRoutes` 是静态白名单：登录/注册/404/401/首页/个人中心等。个人中心 `views/system/user/profile` **不是** 15 键之一（L-022）。
6. `assembleServerRoutes` 遇到字符串 `component`：先查 `specialComponents`（`Layout` / `ParentView` / `InnerLink`），再 `inferDomainId` + `resolveRegistration`。解析不到就 `createManifestRouteDiagnostic`。
7. `adaptServerMenuRoutes` 把终端无关的投影拷成 `RouteRecordRaw`。若投影之后 `component` 仍是字符串，**抛** `Server menu component is unresolved`。正常路径不会落到这里。
8. 重复 `name`：`findDuplicateRouteNames([constantRoutes, sidebarRoutes])` → `presentDuplicateRouteNameDiagnostics` 弹通知。**不抛、不撤回**已经投好的路由。

因果：客单决定有哪些路；菜谱板决定路通向哪张页；厅堂壳决定目录怎么 nested。缺键走告示牌，不走 `views/**` glob。

#### D. 守卫怎样消费这棵树

`main.ts` 副作用 import `./permission`。`router.beforeEach`：

1. 没有 `Admin-Token`：白名单（`/login` `/register` `/social-callback` `/sso/callback` 及 register 通配）放行；否则 `/login?redirect=`。
2. 有票且去 `/login`：改去 `/`。
3. 有票、`roles.length === 0`：`restoreProtectedNavigation({ loadIdentity: user.getInfo, loadRoutes: navigation.generateRoutes, isExternal: isHttp(route.path), addRoute: router.addRoute, createReplacement: replace 当前 to })`。
4. `isHttp` 是路径里是否出现 `http://` 或 `https://`。外链菜单**不** `addRoute`，留给侧栏当链接。
5. 任一步抛错：`logout`，非已处理错误再 `ElMessage`，回 `/`。
6. `roles.length !== 0`：直接 `true`，不再拉菜单。

测试 `restores getInfo -> getRouters -> addRoute -> replace` 把事件顺序钉死。`loadRoutes` 在 admin 里叫 `generateRoutes`，里面才是 `getMenus()`；事件名写 `getRouters` 是合同顺序，不是函数名。

#### E. 权限 host 怎样消费「菜单旁边的券」

按钮权限**不**走 `getRouters`。走 `getInfo` 写入的 `user.permissions` / `user.roles`。

- App 指令：`directive/index.ts` → `installWebPermissionHost(app, createAdminAccessEvaluator)`。`v-hasPermi="['system:user:edit']"` 在 mounted 时 `hasAnyPermission`；不配则 `parentNode.removeChild`。绑定不是非空字符串数组 → 拆 DOM 并抛。`v-has-permi` 与 `v-hasPermi` 是同一指令的模板写法。
- 页面脚本：`UserPage` 的 `checkPermi` = `permissions.some(p => runtime.hasPermission(p))`。注释写明 `el-dropdown-item` 延迟加载，指令不生效，导入/导出必须 `v-if="checkPermi(...)"`。
- OpenAPI 页：`runtime.hasPermission('system:openApi:list')` 为假则不打目标用户查询，本地写错误文案。

`createAdminAccessEvaluator` 每次 new 一把尺，读**当前** Pinia。这与 web-kit「provider 在指令执行时读取当前会话，不缓存快照」对齐。

超管：角色询问认 `superadmin` 或遗留 `admin`；权限询问认快照里的 `*:*:*` 或精确串。前端藏按钮 ≠ 后端放行。`SysUserController` 上的 `@SaCheckPermission` 仍在（L-015）。

manifest 的 13 组权限贡献：`composeAppRuntime` 会收进 `permissionContributions()`，admin 导航/指令**没有**读这个数组来决定显隐。它是菜谱板上印的过敏原目录，方便测试「这页声称认识哪些串」，不是会话授权。

## 图、表或文本图

**图题 / caption：** 服务端菜单进入 admin-web。alt：getRouters 已裁剪；createSystemWebDomain 只提供 15 键；投影把键换成页或诊断；权限快照走 getInfo。

```text
  [sys_menu 按 userId+clientPk 裁剪]
                 │  GET /system/menu/getRouters
                 v
        ServerMenuNode 树（component 仍是字符串）
                 │
                 │  generateRoutes 投影 ×3
                 │    flatten=false → sidebar / topbar
                 │    flatten=true  → rewriteRoutes → addRoute
                 v
     projectServerRoutes / assembleServerRoutes
         │
         ├─ 'Layout'|'ParentView'|'InnerLink' → 厅堂壳
         │
         ├─ resolveAdminWebRegistration(key, inferDomainId(key))
         │        │
         │        v
         │   composeAppRuntime（已选 web-domain-system 等）
         │        │
         │        ├─ 命中 createSystemWebDomain 的 15 键
         │        │     load() → runtimeView → UserPage({ runtime })
         │        └─ 未命中 → ManifestRouteDiagnostic
         │
         └─ 外链 http(s) → 不 addRoute

  平行的权限轨（不是 RouterVo 字段）：
    getInfo.permissions/roles
         → createAdminAccessEvaluator
              ├─ installWebPermissionHost → v-hasPermi
              └─ SystemWebRuntime.hasPermission → checkPermi / 脚本闸
```

**文字等价物：** 图上半是菜单轨。后端先按人和 Client 裁出一棵树，树上每张叶子的 `component` 还是字符串。管理端导航 Store 把这棵树投影三次：侧栏和顶栏保留分组；真正交给 Vue Router 的那份会把 `ParentView` 拍平。投影时先认三个厅堂壳名，再拿键去已选 manifest 里找。`createSystemWebDomain` 贡献 15 个系统治理键；命中则懒加载页面并注入 runtime；未命中挂诊断页。外链不进 Router。图下半是平行的权限轨：按钮券来自 `getInfo`，经求值器同时供给指令和 runtime 口。两轨在恢复链里先后发生（先身份后菜单），但菜单 JSON 并不携带按钮权限字段。

**图的边界：** 不画 `createMonitorWebDomain` 的四键（L-024），只承认管理端选择集里还有那块板。不画 home-web 的恢复链细节，只承认它不选 `web-domain-system`。不保证每条种子菜单的 `icon` 都符合 Iconify 协议（那是改菜单时的验收项，不是本课口试）。不把 `manifest.permissions` 画进 Pinia。不把 tab 托盘画进 getRouters 箭头。

**图题 / caption：** 15 键菜谱板与三块厅堂 host。alt：工厂、compose、导航 Store、权限指令、tab 托盘各管一段。

```text
  web-domains/system
    createSystemWebDomain(runtime)
      15 componentKey + 13 permission groups + runtimeView
                    │
                    v
  adminManifestRegistry
    adminSystemWebRuntime ──► 工厂
    composeAppRuntime selected + web-domain-system
    resolveAdminWebRegistration
                    │
      ┌─────────────┼──────────────────┐
      v             v                  v
 permission.ts   navigation.ts     access.ts
 restoreProtected  generateRoutes   createAdminAccessEvaluator
 getInfo→addRoute  getMenus()+投影  directive: v-hasPermi
                                    runtime.hasPermission
      旁路（不是菜单轨）：
      application/host/navigation.ts  tab close/open
```

**文字等价物：** 左边厨房工厂只在拿到 runtime 之后才贴出 15 键。中间 registry 是厅堂电工：填 runtime、调用工厂、把清单放进 compose 的选择集，并对外提供按键解析。右边三块 host 分工：守卫负责恢复顺序；Store 负责把 getMenus 投影成路由；access + web-kit 负责按钮尺。tab 文件挂在 runtime 口上，是上菜之后撤盘子的工具。

**图的边界：** 不画出 `services.ts` 全部工厂（L-007）。不保证 `permissionContributions()` 被任何 UI 读取——工作树消费者是 runtime API 与测试。

## 正例、反例与边界

**正例 1：** 口述 15 键。打开 `web-domains/system/src/index.ts` 与 `index.test.ts` 的 `toEqual([...])`，两份名单一致。不要凭 L-009「十条」默写。

**正例 2：** 键对种子。`50-cde-base-dml.sql` 里 `system/user/index`、`system/role/authUser`、`system/oss/config`、`system/ssoApp/index`、`system/openApi/index` 与工厂同一串。

**正例 3：** 厅堂显式选择。`adminManifestRegistry.ts` 的 `selectedManifestIds` 含 `web-domain-system`。`resolveAdminWebRegistration('system/user/index', 'system')` 得到 `componentName: 'User'`。同测试：`system/devtools/index`、`tool/gen/index` 为 `undefined`（未选/已删除）。

**正例 4：** 导航 Store 只调 `getMenus`。`store/modules/navigation.ts` 的 `generateRoutes` 第一行 `await identityAccessService.getMenus()`，没有手写 axios，没有 `import.meta.glob`。

**正例 5：** 缺键变诊断。导航测试「uses a diagnostic component when a component key is absent」：`unselected/report/index` 投影后 `component.name === 'ManifestRouteDiagnostic'`，details 含 `appId:'admin-web'`、`code:'missing-component-key'`。`generateRoutes` **没有** throw。

**正例 6：** 恢复顺序。platform `navigationRecovery.test.ts` 事件数组恰好 `getInfo`、`getRouters`、`addRoute:/internal`、`replace`。外链 `https://external.example` 不出现在 `addRoute`。

**正例 7：** 拍平 ParentView。`routeAssembler.test.ts`：`flattenParentView:true` 时子路径变成 `group/leaf`，父 `ParentView` 节点消失；`false` 时保留分组。输入树不被原地修改。

**正例 8：** 双门权限。`UserPage.vue`：删除按钮 `v-has-permi="['system:user:remove']"`；导入 `v-if="checkPermi(['system:user:import'])"`，`checkPermi` 转调 `runtime.hasPermission`。OpenAPI 管理页用脚本闸挡住 `system:openApi:list`。

**正例 9：** 权限宿主安装点。`directive/index.ts` 只有 `installWebPermissionHost(app, createAdminAccessEvaluator)`，没有本地 `app.directive('hasPermi', ...)`。门禁禁止私装。

**正例 10：** runtime 注入。`UserPage` 第一句业务 props 是 `defineProps<{ runtime: SystemWebRuntime }>()`，HTTP 走 `runtime.service.users.*`，不 import `@/application/services`。

**反例 1：** 「`createSystemWebDomain` 会 `router.addRoute`。」工厂返回 manifest。`addRoute` 在 `permission.ts` 的端口回调里。

**反例 2：** 「15 键是 `src/views/system/**` glob 扫出来的。」门禁禁止 admin 对 `/views/` 做 `import.meta.glob`。个人中心那页才住在 App `views/`，而且不在 15 键里。

**反例 3：** 「`getRouters` 返回的 `permissions` 决定按钮。」`RouterVo` 无此字段。按钮看 `getInfo`。

**反例 4：** 「`manifest.permissions` 就是当前用户权限。」那是目录。活快照在 Pinia。

**反例 5：** 「`application/host/navigation.ts` 就是导航 host 的菜单入口。」它是 tab 托盘。菜单入口是 `store/modules/navigation.ts`。

**反例 6：** 「`monitor/online/index` 是 `createSystemWebDomain` 登记的。」那是 `createMonitorWebDomain`。registry 测试用 domainId `'system'` 能解析 Online，是因为监控 manifest 的 `domainId` 也是 `system`、且管理端选了第二块板。

**反例 7：** 「`inferDomainId` 必须等于 manifest.domainId 才能 resolve。」查找只认键。监控键推断出 `'monitor'`，manifest.domainId 仍是 `'system'`，照样命中。

**反例 8：** 「home-web 也能打开用户管理，因为工厂在 monorepo 里。」home 的 compose 不选 `web-domain-system`。工厂存在 ≠ 该厅堂挂了板。

**反例 9：** 「缺键会让登录失败并 logout。」缺键 → 诊断页。logout 发生在 `getInfo` / `getMenus` 解析抛错，或恢复链里其它异常。

**反例 10：** 「前端还要用角色把动态路由再滤一遍（`filterDynamicRoutes`）。」退役符号。后端已经裁菜单；前端只做键解析。

**反例 11：** 「在 web-domain 里 `import { useUserStore }` 读 permissions。」页面走 `runtime.hasPermission`。Store 属于 App。

**反例 12：** 「`systemDomainModule.capabilities` 列出的就是 15 键。」capabilities 有 monitor-* 和 social，没有 openApi/ssoApp。对照表用工厂数组，不用 capabilities。

**反例 13：** 「`createOpenApiService` 是本课格子。」OpenAPI 页挂在本工厂键 `system/openApi/index` 上，HTTP 工厂是 L-019 / L-033。本课只认键和 runtime.openApi 这只口。

**边界：**

- `flattenParentView` 默认 `false`；admin 只对 rewrite/addRoute 那份传 `true`。侧栏必须保留分组，否则多级目录扁成一片。
- 重复路由名只通知，页面可能 404。这是诊断，不是恢复失败。
- `adaptServerMenuRoutes` 在「无 component 且无 redirect」时抛。空目录被 `normalizeProjection` 删掉 children/redirect，正常客单不应落到这个 throw。
- Clipboard 口在无 `navigator.clipboard.writeText` 时抛。测试覆盖「host 必须提供」；运行时失败是厅堂环境问题，不是菜单投影失败。
- `currentUserId` 读 `getActivePinia()?.state.value.user.userId`。没有 Pinia 就 `undefined`。
- 指令 `hasAnyPermission`：数组里**任一**串命中即留 DOM。`checkPermi` 同样是 `some`。需要「全部命中」时不要用这两扇门的默认行为。
- 超管角色放行的是 `hasRole`，不是自动拥有每一条 `system:user:remove`。超管常见快照会带 `*:*:*`，那是权限数组的事，不要把两套规则揉成一句「admin 万能」。

## 变式与迁移

- **变式 A：给系统室加一页。** 顺序硬：① 后端菜单种子的 `component` 写成合同键（不要写成文件相对路径）；② 在 `createSystemWebDomain` 的 registrations **和** 权限目录里显式加一行；③ 页面只吃 `runtime`；④ admin 已经选了 `web-domain-system` 则不必改选择集；⑤ 加测试：键出现在 `index.test.ts` 名单，registry 能 `resolveAdminWebRegistration`。不要在 `apps/admin-web/src/views` 新建可复用 CRUD，也不要 `loadView`。

- **变式 B：新厅堂只要用户页。** 复制的是「造 runtime + 调工厂 + 把 manifest id 放进**该厅堂** compose」，不是 import admin 的 `adminManifestRegistry`。可以只选 `web-domain-system` 的子集——但当前工厂**没有**子集参数，它一次贴 15 键。真要子集，要么新写一个更窄的工厂，要么接受 15 键都可解析、靠 `getRouters` 不发其它键。不要在 compose 之后删 `components` Map。

- **变式 C：home-web。** 门户同样 `restoreProtectedNavigation`，但选择集是身份 + 资料自助。`system/user/index` 在那里应走缺键诊断（若客单居然带了这键）或根本不会出现在该 Client 的 `getRouters`。不要为了「也能管用户」把 system web-domain 塞进 home。

- **变式 D：监控页。** 加 `monitor/...` 键请改 `createMonitorWebDomain`（L-024），并确保 `selectedManifestIds` 含 `web-domain-system-monitor`。不要把监控塞进本课 15 行表「图个方便」。

- **变式 E：外链菜单。** `isHttp(route.path)` 为真则不 `addRoute`。侧栏仍可能展示。不要把外链改成诊断页。

- **变式 F：下拉按钮。** 新按钮如果挂在延迟渲染的 `el-dropdown-item` 上，抄 `checkPermi`，不要只写 `v-hasPermi` 然后奇怪「权限没了指令却没拆」。

- **变式 G：拍平与否。** 新的中间目录若依赖 `ParentView`，侧栏要分组、路由要拍平——现成的两次 `projectMenus` 已经做了。不要自己在 Store 里递归改 path。

- **迁移口诀：** 种子键 → 工厂登记 → App 选择 manifest → getInfo 恢复身份 → getRouters 投影 → 特殊壳或 resolve 或诊断 → addRoute → 页面用 runtime 说话 → 按钮用同一把尺。跳步会出现「菜单有了点进去是告示牌」「按钮指令在下拉里失灵」「第二个 App 偷到了管理端路由」。

## 常见误区

1. **「web-domain 就是路由表。」** 它是键到页面的菜谱板。路由表是导航 Store + Vue Router。
2. **「L-009 说十条，所以背十个就行。」** 工作树 15 个。以 `index.ts` / 测试为准。
3. **「`createSystemWebDomain` 包含监控。」** 监控是 `createMonitorWebDomain`。
4. **「`host/navigation.ts` 拉菜单。」** Store 才拉。host 文件管 tab。
5. **「前端要根据 roles 再滤一遍动态路由。」** 退役。客单已裁。
6. **「缺键等于登录失败。」** 缺键是告示牌；身份/菜单解析失败才 logout。
7. **「resolve 要 domainId 匹配。」** 查找只认 `componentKey`。
8. **「`manifest.permissions` 驱动 v-hasPermi。」** 指令读 Pinia 快照。
9. **「`getRouters` 带按钮权限。」** `RouterVo` 没有该字段。
10. **「页面可以 import `systemService`。」** 页面只认 `runtime.service`。单例在 App。
11. **「`import.meta.glob` 扫 views 更省事。」** 门禁 `admin-navigation-boundary` 抓。
12. **「home 没这工厂所以 `getMenus` 不存在。」** 门户仍可 `getMenus`；它缺的是 system 菜谱板。
13. **「`v-hasPermi` 和 `v-has-permi` 是两个指令。」** 同一指令。下拉要用脚本门是因为组件延迟，不是因为横杠。
14. **「capabilities 列表 = 页面列表。」** 对不上 openApi/ssoApp，还多了 monitor/social。
15. **「组合完成 = 用户能看见用户管理。」** 还要：该 Client 的 `sys_menu` 有这键、当前用户被授权、恢复链跑完、按钮快照含对应串。少一环都会「有板无菜」或「有菜无筷」。

## 非评分暂停

打开磁盘，不要凭记忆默写 15 键。不要改文件。没有标准答案栏。

1. 打开 `frontend/packages/web-domains/system/src/index.ts`。用手指点完 15 行 `componentKey`。再点 `id: 'web-domain-system'` 和权限目录里的 `system:user:temporaryPassword`、`system:openApi:self`。确认没有 `monitor/online/index`。
2. 打开 `frontend/apps/admin-web/src/router/adminManifestRegistry.ts`。点 `adminSystemWebRuntime.hasPermission`、`closeAndOpenPage`、`const systemManifest = createSystemWebDomain(...)`，再点 `selectedManifestIds` 里的 `web-domain-system` 与 `web-domain-system-monitor`。
3. 打开 `store/modules/navigation.ts` 的 `generateRoutes`。圈出 `getMenus`、两次 `projectMenus` 的 `flattenParentView` 差、`resolveAdminWebRegistration`、`adaptServerMenuRoutes`。
4. 打开 `permission.ts`。按 `getToken` → `roles.length` → `restoreProtectedNavigation` 的四个端口把恢复链走一遍。圈出 `isHttp` 跳过 `addRoute` 的那一行。
5. 打开 `application/host/navigation.ts` 的文件头。确认没有 `getMenus`。再打开 `application/access.ts` 和 `directive/index.ts`，确认指令宿主和 runtime 口是同一把 `createAdminAccessEvaluator`。
6. 打开 `UserPage.vue`：找一处 `v-hasPermi` / `v-has-permi`，再找 `checkPermi` 的下拉。想一句：为什么导入不能只靠指令。

## 总结、词汇表与下一步

- **`A:createSystemWebDomain`** 贴 15 键菜谱板 + 13 组权限目录，并用 `runtimeView` 把厅堂 runtime 注入页面。它不拥有 Router、会话、`getRouters`。
- **`B:admin-web navigation host`** 不是一个函数名。守卫按 `getInfo → generateRoutes → addRoute → replace` 恢复；Store 用 `projectServerRoutes` 把服务端 `component` 对到已选 manifest；权限 host 用 `getInfo` 快照驱动 `v-hasPermi` 与 `runtime.hasPermission`。tab 托盘是 runtime 口，不是菜单入口。
- 客单（`getRouters`）已经按人+Client 裁剪。前端缺键挂诊断，不 glob，不二次授权。
- 监控、OpenAPI HTTP、OSS 直传、菜单 CRUD、`createSystemService` 方法表，分别留给 L-024 / L-033 / L-029 / L-016 / L-019。

词汇表：`createSystemWebDomain` / `WebDomainManifest` / `componentKey` / `SystemWebRuntime` / `runtimeView` / `composeAppRuntime` / `projectServerRoutes` / `inferDomainId` / `restoreProtectedNavigation` / `AccessEvaluator` / `installWebPermissionHost` / `ManifestRouteDiagnostic` / tab 托盘。

下一步：L-021 参数与字典（本课只借用了 `runtime.dicts` / `dictCache` 这两只口）；L-024 才把监控四页摊开。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-005 | `frontend/apps/admin-web` | 厅堂 compose、守卫、Store、权限安装、tab 托盘、静态 `constantRoutes` | `permission.ts`；`store/modules/navigation.ts`；`router/{adminManifestRegistry,index}.ts`；`application/{access,host/navigation}.ts`；`directive/index.ts` | 2026-09-16 |
| S-006 | `frontend/packages/{web-domains/system,platform/app-runtime,platform/permission,web-kit/permission,domains/admin,domains/system}` | 工厂 15 键与权限目录；投影/恢复/求值器；`ServerMenuNode`；`systemDomainModule` | 各包 `src/index.ts` 与测试 | 2026-09-16 |
| S-007 | `.agents/skills/engineering-standards` 模块地图；`namewta-fullstack-development` 的 `permission-routing.md`；`frontend/tooling/architecture/src/index.mjs` | 动态路由合同顺序；web-domain 不拥有 Router；`admin-navigation-boundary` 退役 glob / `usePermissionStore` | 「正式恢复链路」；`adminNavigationBoundaryViolation` | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/50-cde-base-dml.sql` | 15 键与 `sys_menu.component` 对齐 | `system/user/index` 等 insert | 2026-09-16 |
| S-L020-01 | `frontend/packages/web-domains/system/src/index.ts` 与 `index.test.ts`；`runtime.ts` | 工厂签名、15 键顺序、13 组权限、`runtimeView`、显式选择才暴露键 | `createSystemWebDomain`；`publishes all server-facing governance keys` | 2026-09-16 |
| S-L020-02 | `frontend/apps/admin-web/src/router/adminManifestRegistry.ts` 与 `adminManifestRegistry.test.ts` | runtime 袋；`hasPermission` / tab / 密码策略 / 剪贴板；选择集含 system 与 monitor；未登记键 `undefined` | `adminSystemWebRuntime`；`resolveAdminWebRegistration('system/user/index')` | 2026-09-16 |
| S-L020-03 | `frontend/apps/admin-web/src/store/modules/navigation.ts` 与 `navigation.test.ts`；`router/serverMenuAdapter.ts` | `getMenus` → 投影三次 → adapt；缺键诊断；重复名通知；不 glob | `generateRoutes`；`projectMenus` | 2026-09-16 |
| S-L020-04 | `frontend/apps/admin-web/src/permission.ts`；`platform/app-runtime/src/{navigationRecovery,routeAssembler}.ts` 与测试 | 恢复顺序；外链跳过 addRoute；ParentView 拍平；`inferDomainId`；resolve 按键查找 | `restoreProtectedNavigation`；`assembleServerRoutes` | 2026-09-16 |
| S-L020-05 | `frontend/apps/admin-web/src/application/access.ts`；`directive/index.ts`；`web-kit/permission/src/index.ts`；`platform/permission/src/index.ts`；`UserPage.vue`；`OpenApiAdminPage.vue` | 双门权限；下拉必须脚本门；provider 现取快照；指令失败拆 DOM | `createAdminAccessEvaluator`；`checkPermi`；`installWebPermissionHost` | 2026-09-16 |
| S-L020-06 | `SysMenuController.getRouters`；`SysMenu.getComponentInfo`；`RouterVo.java` | 后端按 userId+clientPk 裁剪；壳名 Layout/ParentView/InnerLink；RouterVo 无 permissions 字段 | `/system/menu/getRouters`；`buildMenus` | 2026-09-16 |
| S-L020-07 | `createMonitorWebDomain`；home-web `router/index.ts`；`application/host/navigation.ts` | 监控是第二工厂；门户同样恢复但不选 system 板；tab 托盘无 getMenus | `web-domain-system-monitor`；home `restoreProtectedNavigation` | 2026-09-16 |
