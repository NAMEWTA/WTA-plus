# 多 App 的动态路由、菜单与权限怎样运转

这份图解回答三个问题：

1. 用户登录后，后端返回的菜单怎样变成当前 App 中真正可访问的页面？
2. 多个 App 共用同一个后台时，为什么不会拿错 Client、Token、菜单和权限？
3. 哪些能力已经共享，哪些接线仍必须由各个 App 自己完成？

先说最重要的结论：

```text
后端决定“这个 Client 下的这个用户可以得到哪些菜单和接口权限”
                              +
共享包决定“怎样请求、校验、解析菜单和判断权限字符”
                              +
每个 App 决定“选择哪些页面、用什么 Router、布局和导航呈现出来”
```

因此，新增 Web App 不需要重写登录、`getInfo`、`getRouters`、组件键解析和权限判断算法，但仍要为自己的 Router、状态、布局和领域选择做一次显式装配。

## 先看全图

### 完整登录与动态菜单时序

```text
[用户打开受保护页面]
          |
          v
[App 路由守卫检查自己的 Token]
          |
          +-- 没有 Token --> [带 redirect 跳到本 App 登录页]
          |
          v
[GET /system/user/getInfo]
          |
          +-- user
          +-- roles
          +-- permissions
          |
          v
[GET /system/menu/getRouters]
          |
          v
[后端按当前 Client + 当前用户生成菜单树]
          |
          v
[共享 route assembler 递归处理菜单树]
          |
          +-- Layout 等特殊壳组件
          +-- component key -> 当前 App 已选 Manifest 中的 Vue 页面
          +-- 缺失 key -> 明确诊断页面
          |
          v
[当前 App 的 router.addRoute]
          |
          v
[replace 回用户原来要访问的地址]
          |
          +--> [侧栏、顶栏、面包屑读取 App 的路由状态]
          |
          +--> [按钮指令读取 roles / permissions 决定是否呈现]
```

### 多个 App 不是共用一份运行时状态

```text
                         [同一个后台]
                              |
             +----------------+----------------+
             |                                 |
             v                                 v
        [admin-web]                        [client-web]
        clientId=A                         clientId=B
        Admin-Token                        独立会话键
        Admin Router                       Client Router
        选择 7 个领域                      选择 2 个领域
        Admin 布局                         Client 布局
             |                                 |
             +----------复用下面这些------------+
                              |
                 identity-access 领域服务
                 Axios browser adapter 工厂
                 app-runtime 路由装配算法
                 permission 权限求值器
                 被各 App 选中的 web-domain 页面
```

正确理解“复用”的方式是：

```text
复用同一个工厂和规则
        !=
所有 App 共用同一个已经创建好的对象
```

每个 App 都会创建自己的 HTTP、Session、Router 和组合清单，所以状态不会因为共享代码而串线。

### 三种权限不是一件事

```text
[菜单权限]
后端决定返回哪些菜单
前端把菜单变成可导航页面

[页面内按钮权限]
getInfo 返回 permissions / roles
前端决定按钮是否显示

[真正的接口安全]
每次请求仍由后端鉴权
前端隐藏按钮不能代替后端权限检查
```

## 一步一步看

### 第一步：每个 App 先声明自己的身份

每个 App 都必须有自己的 OAuth `clientId`。浏览器请求适配器会把它写入 `clientid` 请求头，登录请求也会在请求体中明确发送 `clientId`。

```text
[admin-web 环境配置]
VITE_APP_CLIENT_ID=A
        |
        v
[Admin HttpClient]
所有请求携带 clientid=A


[client-web 环境配置]
VITE_CLIENT_WEB_CLIENT_ID=B
        |
        v
[Client HttpClient]
所有请求携带 clientid=B
```

Admin 的装配入口位于：

- `<Path>plus-ui-namewta/apps/admin-web/src/application/http.ts</Path>`
- `<Path>plus-ui-namewta/apps/admin-web/src/application/session.ts</Path>`
- `<Path>plus-ui-namewta/apps/admin-web/src/application/services.ts</Path>`

Client Web 的独立装配位于：

- `<Path>plus-ui-namewta/apps/client-web/src/config.ts</Path>`
- `<Path>plus-ui-namewta/apps/client-web/src/application.ts</Path>`

Client Web 的会话键按 App 与 Client 命名：

```text
namewta:client-web:<clientId>:access-token
```

Admin 当前继续使用自己的 `Admin-Token` 键。两者没有共用 Token 单例。

### 第二步：登录前先确认当前 Client 可用

共享的 `identity-access` 领域服务负责认证用例。登录页先准备当前 Client 的认证上下文与验证码，再允许提交账号密码。

```text
[登录页]
    |
    v
identityAccessService.prepareLogin()
    |
    +--> /auth/client/context
    +--> /auth/code
    |
    v
[Client 可用且响应格式有效]
    |
    v
identityAccessService.login(...)
    |
    v
POST /auth/login
    |
    v
[保存当前 App 自己的 access token]
```

如果 Client 缺失、被禁用或关键字段格式错误，流程会失败关闭，不会猜一个默认 Client，也不会退回其他 App 的会话。

共享实现位于 `<Path>plus-ui-namewta/packages/domains/identity-access/src/index.ts</Path>`。它不依赖 Vue Router、Pinia、Element Plus 或某个 App 的全局变量，因此能被不同终端复用。

### 第三步：Admin 路由守卫识别是否需要恢复会话

Admin 的入口在 `<Path>plus-ui-namewta/apps/admin-web/src/permission.ts</Path>`。

```text
[发生页面导航]
      |
      +-- 无 Token + 白名单页面 --> 放行
      |
      +-- 无 Token + 受保护页面 --> /login?redirect=原地址
      |
      +-- 有 Token + /login --> 回首页
      |
      +-- 有 Token + 已有 roles --> 放行
      |
      v
[有 Token，但内存中的 roles 为空]
说明可能刚登录或刷新后需要恢复身份与路由
```

当前 Admin 使用 `roles.length === 0` 作为“尚未完成受保护导航恢复”的标记。这个判断属于 Admin 当前状态模型，不是要求所有 App 都使用同一个 Pinia Store。

### 第四步：必须先取身份，再取菜单

共享恢复函数 `restoreProtectedNavigation` 固定下面的顺序：

```text
1. loadIdentity()
       |
       v
2. loadRoutes()
       |
       v
3. router.addRoute(...)
       |
       v
4. replace 当前目标
```

对应 Admin 的真实调用是：

```text
useUserStore().getInfo()
       |
       v
identityAccessService.getInfo()
       |
       v
GET /system/user/getInfo

然后

usePermissionStore().generateRoutes()
       |
       v
identityAccessService.getMenus()
       |
       v
GET /system/menu/getRouters
```

不能颠倒顺序。页面和按钮权限判断依赖先得到的 `roles` 与 `permissions`，路由恢复也需要一个已经成立的用户会话。

### 第五步：`getInfo` 返回的是权限快照

Admin 的用户 Store 位于 `<Path>plus-ui-namewta/apps/admin-web/src/store/modules/user.ts</Path>`。它把响应保存为当前 Admin 会话的内存状态：

```text
getInfo 响应
   |
   +-- user ----------> 用户名、昵称、头像、用户 ID
   |
   +-- roles ---------> 页面或动作的角色判断
   |
   +-- permissions ---> 按钮和命令的权限字符判断
```

如果普通用户的角色列表为空，Admin 会写入 `ROLE_DEFAULT`，避免刷新时把“合法但无业务角色”误判为“身份尚未恢复”。

### 第六步：后端先完成 Client 与用户菜单裁剪

`getRouters` 返回的不是全系统菜单让前端自己猜，而是后端基于当前认证上下文生成的菜单树。

```text
[请求中的 clientid]
          +
[Token 对应的当前用户]
          +
[后端角色、菜单和权限关系]
          |
          v
[该 App、该用户应得到的菜单树]
```

前端遵守一个重要不变量：

```text
不再执行第二轮“跨 Client 菜单过滤”
```

原因是后端才掌握完整授权关系，也是安全权威。前端只负责校验响应结构、映射组件并注册路由。否则前后端各写一套 Client 过滤规则，迟早会出现不一致。

### 第七步：Admin 为不同导航区域准备路由副本

Admin 的权限 Store 位于 `<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>`。它对后端菜单做结构化克隆，然后生成几种用途不同的状态：

```text
[同一棵后端菜单树]
       |
       +--> sidebarRouters  侧栏使用
       |
       +--> topbarRouters   顶栏使用
       |
       +--> defaultRoutes  默认导航使用
       |
       +--> addRoutes      交给 Router 注册
```

这些数组属于 Admin 的布局状态，所以保留在 Admin Store。未来手机 App 可能使用底部标签或原生导航，不应该被迫拥有 `sidebarRouters`。

### 第八步：把后端 component 字符串变成真正页面

后端菜单节点中的 `component` 是逻辑组件键，不是让浏览器直接读取的磁盘路径。

```text
后端 component = system/user/index
                 |
                 v
assembleServerRoutes(...)
                 |
                 v
当前 App 的组件登记表查找
                 |
                 v
packages/web-domains/system-admin 中的 UserPage.vue
```

共享递归算法位于 `<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path>`，它负责：

1. 递归保留菜单树结构。
2. 把 `Layout`、`ParentView`、`InnerLink` 换成当前 App 注入的特殊组件。
3. 按 component key 查询当前 App 已组合的 Manifest 登记。
4. 找不到页面时创建明确的诊断组件，而不是静默显示空白页。

这里的 component key 是稳定合同。例如 `system/user/index` 是一个逻辑编号，不要求真实文件继续放在 `admin-web/src/views/system/user/index.vue`。

### 第九步：当前 App 只能解析自己明确选择的页面

Admin 在 `<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>` 中明确组合：

```text
identity-access
demo
devtools
workflow
system-admin
ai
operations
```

每个 Web-domain Manifest 会登记自己拥有的 component key 与懒加载页面。Admin runtime 汇总这些登记后，才形成当前 App 的组件注册表。

```text
[后端返回 component key]
             |
             v
[当前 App 是否选择了拥有该 key 的 Manifest？]
       |                               |
      是                              否
       |                               |
       v                               v
[加载共享 Vue 页面]              [显示可定位的诊断页]
```

因此，一个 App 安装了 Monorepo 包，不代表它自动暴露所有页面；只有写入该 App 组合清单的领域才会进入运行时。

Admin 还保留一个 App 本地页面兜底查找，只能搜索 `<Path>plus-ui-namewta/apps/admin-web/src/views</Path>`。它用于 Admin 专属页面，不会扫描另一个 App 的目录，也不会绕过 Manifest 去加载未选择领域。

### 第十步：动态注册后必须重新进入原目标

路由守卫拿到装配后的服务端路由后，对非 HTTP 外链执行：

```text
router.addRoute(route)
```

注册完成后，不是简单地继续第一次导航，而是生成一个 `replace: true` 的目标，重新进入用户原本要访问的地址：

```text
第一次访问 /system/user
       |
       +-- 此时动态路由还没注册
       |
       v
[加载身份和菜单 + addRoute]
       |
       v
replace /system/user
       |
       v
[Vue Router 这次能够匹配 UserPage]
```

`replace` 还避免浏览器历史中留下一个多余的恢复步骤。

### 第十一步：菜单呈现与页面访问来自同一批路由状态

Admin 布局读取 `sidebarRouters`、`topbarRouters` 等状态来画侧栏和顶栏；Router 使用 `addRoutes` 完成页面匹配。两者都来自同一次 `getRouters` 响应，只是为布局与路由匹配生成了不同视图。

```text
                 [getRouters 菜单树]
                         |
              +----------+----------+
              |                     |
              v                     v
       [导航菜单状态]          [Router 动态记录]
       用户看到入口            URL 能匹配页面
```

这能避免“侧栏显示了菜单，但 Router 没有页面”或者“URL 能打开页面，但导航没有入口”的两套数据源漂移。

### 第十二步：按钮权限使用同一份权限快照

共享权限求值器位于 `<Path>plus-ui-namewta/packages/platform/permission/src/index.ts</Path>`。Admin 用 `<Path>plus-ui-namewta/apps/admin-web/src/application/access.ts</Path>` 把自己的 Store 快照接进去。

页面中的按钮可以这样声明：

```text
v-hasPermi="['system:user:add']"
v-hasRoles="['superadmin']"
```

真正执行过程如下：

```text
[Vue 页面挂载按钮]
       |
       v
[hasPermi / hasRoles 指令]
       |
       v
[createAdminAccessEvaluator]
       |
       v
[读取当前 Admin 的 permissions / roles]
       |
       +-- 任一要求满足 --> 保留元素
       |
       +-- 全部不满足 --> 从 DOM 移除元素
```

统一求值器支持：

- 权限通配符 `*:*:*`。
- 规范超管角色 `superadmin`。
- 当前迁移期保留的旧角色别名 `admin`。
- “满足任意一个”与“必须全部满足”两组方法。

除了 Vue 指令，共享页面中的命令、外链和操作也能通过注入的 `hasPermission` 使用同一个求值器，因此不会再维护第二套 `$auth` 插件规则。

### 第十三步：本地动态路由权限是另一条小分支

Admin 还保留 `dynamicRoutes` 与 `filterDynamicRoutes`，用于过滤前端本地声明且带有 `permissions` 或 `roles` 的路由。

```text
[前端本地 dynamicRoutes]
       |
       v
[共享 AccessEvaluator 判断]
       |
       v
[有权的记录才 addRoute]
```

当前 `<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>` 中的 `dynamicRoutes` 是空数组，所以 Admin 的主要业务菜单来源仍是后端 `getRouters`。保留这条分支是为了显式本地路由能力，不应误认为系统页面仍硬编码在 App Router 中。

### 第十四步：401 与退出会清理当前 App 的会话

HTTP 适配器遇到 401 后，会调用当前 App 注入的失效处理：

```text
[后端返回 401]
       |
       v
[当前 App 的 onUnauthorized]
       |
       +-- 清理当前 App Token
       +-- 清理用户角色和权限状态
       +-- 执行当前 App 的退出编排
       +-- 跳转当前 App 登录页并保留 redirect
```

共享适配器只定义失效流程的公共机制，不会直接 import Admin Router。Admin 与 Client Web 注入各自的跳转和清理实现。

### 第十五步：`client-web` 当前完成到哪一步

这里必须区分“架构能力已经可复用”和“某个 App 已经启用完整能力”。

当前 Client Web 是第二 App 的最小组合证明：

```text
已经完成
  |
  +-- 独立 clientId
  +-- 独立 HTTP 与会话
  +-- 复用 identity-access 登录
  +-- 选择 identity-access 与 demo Manifest
  +-- 独立 Client Shell 和主题
  +-- 静态 /login、/demo、/diagnostic 路由
  +-- 未选择领域的明确诊断
```

但它目前没有执行下面这条生产级动态菜单链：

```text
getInfo -> getRouters -> assembleServerRoutes -> addRoute
```

它的路由定义位于 `<Path>plus-ui-namewta/apps/client-web/src/router.ts</Path>`，当前只有显式静态路由。`permissionProof.ts` 中的 `hasPermi` 也是用于证明 App 级指令隔离的测试接线，会移除被标记元素，不是正式的 Client 权限快照求值器。

所以当前真实状态应表达为：

| App | 当前菜单方式 | 当前权限状态 | 定位 |
| --- | --- | --- | --- |
| admin-web | 后端 `getRouters` 动态菜单 | 完整 `getInfo`、角色、按钮、路由恢复 | 正式管理端 |
| client-web | `/login`、`/demo` 等静态路由 | 登录会话与隔离证明，尚未接正式权限快照 | 最小第二 App 证明 |

### 第十六步：把 Client Web 升级为正式业务 App 时怎么接

不需要复制 Admin 整棵 Store 和 `permission.ts`。应当在 Client App 内完成下面的宿主接线：

```text
1. 建立 Client 自己的 identity state
   保存 user、roles、permissions 和恢复状态
            |
            v
2. 路由守卫调用共享 identityService.getInfo()
            |
            v
3. 调用共享 identityService.getMenus()
            |
            v
4. 用共享 assembleServerRoutes 解析菜单
   resolveRegistration 接到 clientComposition runtime
            |
            v
5. 把 Client 自己的 Layout、特殊组件和诊断页注入
            |
            v
6. 用 Client Router 执行 addRoute 与 replace
            |
            v
7. 用 Client 自己的导航 Store 或 computed 状态画菜单
            |
            v
8. 用共享 createAccessEvaluator 接上 Client 权限快照
```

可以复用的代码包括：

- `identityAccessService.getInfo()` 与 `getMenus()`。
- `restoreProtectedNavigation()` 的顺序编排。
- `assembleServerRoutes()` 的菜单递归与组件解析。
- `createAccessEvaluator()` 的角色与权限字符语义。
- 被 Client 明确选择的 Web-domain Manifest 与页面。

仍由 Client App 自己拥有的内容包括：

- 哪些地址是登录白名单。
- 登录成功跳到哪里。
- Router 实例与 history 模式。
- Layout、首页、404、导航形式和主题。
- 用 Pinia、Vue composable 还是其他方式保存路由状态。
- 选择哪些 domain 与 web-domain。

这正是“共享内核、App 显式组合”，而不是复制 Admin。

### 第十七步：未来移动端和小程序怎样理解这套流程

移动 Web 如果仍使用 Vue Router，可以复用大部分 Web 动态路由装配，只替换壳层、导航和页面选择。

原生移动端或小程序不一定存在 `router.addRoute`，此时复用边界会变成：

```text
[identity-access 获取身份与服务端能力]
                    |
                    v
[终端无关 roles / permissions / menu 数据]
                    |
          +---------+---------+
          |                   |
          v                   v
[小程序页面表映射]       [原生移动导航映射]
```

它们继续复用 domain、ClientContext、权限语义和适配器合同，但实现自己的页面注册和导航端口，不依赖 Vue Router 与 DOM 指令。

## 容易误解的地方

### 误解一：前端应该再次按 Client 过滤后端菜单

不是。后端负责按当前 Client 与用户裁剪菜单；前端负责把结果装成当前 App 的路由。前端只会因为当前 App 未选择相应 Manifest 而显示配置诊断，不会把它当成第二套授权算法。

### 误解二：共享动态路由就是所有 App 共用一个 Permission Store

不是。共享的是请求、恢复顺序、菜单装配和权限求值规则。每个 App 的 Router、导航状态、布局和白名单仍然独立。

### 误解三：Manifest 决定用户有没有权限

不是。Manifest 只回答“这个 App 有没有登记该 component key 对应的页面”。用户有没有权得到菜单，仍由后端决定。

### 误解四：看不见按钮就代表接口安全

不是。按钮隐藏只改善用户界面。用户仍可能手工发送请求，所以后端必须对每个受保护接口继续鉴权。

### 误解五：`client-web` 已经完整实现动态菜单

不是。它证明了独立 Client、会话、登录、壳层和领域选择，但当前使用静态路由。正式产品化时应按共享流程增加 App 自己的身份状态、菜单状态和 Router 接线。

### 误解六：后端 component key 就是物理文件路径

不是。它是稳定逻辑键，由当前 App 选择的 Web-domain Manifest 映射到真实 Vue 页面。页面移动目录不需要同步修改全部后端菜单键。

## 你现在能复述什么

```text
问题一：Admin 登录后为什么能看到自己的菜单？

答案：请求携带 Admin 的 clientId 和 Token；
      先 getInfo 得到角色与权限，
      再 getRouters 得到后端已裁剪菜单，
      最后用 Manifest 把组件键装成路由并 addRoute。


问题二：新 App 是否要复制 Admin 的 permission.ts 和 Store？

答案：不复制整套文件。
      复用 identity service、恢复流程、路由装配器和权限求值器；
      新 App 只实现自己的 Router、状态、布局和领域选择接线。


问题三：Manifest 与权限分别负责什么？

答案：后端权限决定用户能得到什么；
      Manifest 决定当前 App 能把 component key 解析成哪个页面；
      两者不匹配时明确报诊断，不能静默越权或空白。


问题四：Client Web 现在是否已经是完整动态菜单 App？

答案：还不是。
      它是最小组合证明，当前使用静态路由；
      共享核心已经具备，正式化时还要补 Client 自己的动态路由宿主接线。
```

最短记忆方式：

```text
clientId 区分 App 身份
        +
后端裁剪菜单和授权
        +
Manifest 找到当前 App 的页面
        +
App Router 注册并呈现
        +
后端接口鉴权最终兜底
```

## 术语小词典

- OAuth Client：后端登记的一类前端客户端身份，由 `clientId` 标识；它不是用户、租户或 App 代码目录。
- ClientContext：当前 App 注入给认证和请求能力的 Client 配置，至少包含有效 `clientId`。
- Token：用户登录后得到的访问令牌；每个 App 使用自己的会话存储实例。
- 路由守卫：页面跳转前运行的检查与恢复逻辑，决定放行、登录跳转或加载动态路由。
- 身份快照：`getInfo` 返回的 user、roles 与 permissions，供当前会话展示与导航判断使用。
- 动态菜单：后端 `getRouters` 返回的菜单树，前端运行时把它转换为路由和导航。
- component key：后端菜单中的逻辑页面编号，例如 `system/user/index`，不等于物理文件路径。
- WebDomainManifest：Web 领域公开的页面登记表，记录 component key 可以加载哪个 Vue 组件。
- App runtime：汇总当前 App 已选择领域和 Manifest 的运行时注册表。
- route assembler：把后端菜单树、特殊布局组件和 Manifest 登记组合成前端路由记录的共享算法。
- `router.addRoute`：Vue Router 在运行时注册一条路由的方法。
- 权限字符：表示具体操作能力的字符串，例如 `system:user:add`。
- 角色：一组用户身份标签，例如 `superadmin`；它可以参与页面或动作判断。
- 失败关闭：配置、身份或权限数据无效时拒绝继续，而不是猜测默认值或放宽访问。
- 宿主接线：某个 App 把自己的 Router、Store、布局、会话和交互实现注入共享能力的过程。
- 服务端安全权威：真正允许或拒绝接口访问的是后端；前端菜单和按钮控制不能替代它。
