# Admin 动态路由与权限为什么仍有代码放在 App

这篇图解回答三个问题：

1. 动态路由和角色、按钮权限是不是所有前端 App 都能复用？
2. 既然能复用，为什么 `directive`、`router`、`permission store` 和路由守卫还在 `admin-web`？
3. 将来新增 App 时，是不是还要把这些文件重新复制一遍？

先说结论：**权限规则和路由恢复算法可以复用，但每个 App 如何把它们接到自己的 Router、Store、布局、白名单和界面上，仍然属于该 App 的装配。** 当前代码已经把一部分真正通用的核心放进 `packages/platform/*`；`admin-web` 中留下的是“Admin 接线”和少量仍可继续提炼的混合代码。

```text
可以共享的不是“整个目录原封不动搬走”，
而是目录里面不认识 Admin 的那部分规则和流程。
```

## 先看全图

把整套能力想成“通用发动机 + 每辆车自己的驾驶舱”：

```text
                         后端
                          |
             +------------+------------+
             |                         |
             v                         v
      [当前用户角色和权限]       [当前 Client 的菜单树]
             |                         |
             v                         v
  [Admin 用户 Store 保存快照]   [Admin 权限 Store 接收菜单]
             |                         |
             v                         v
 [通用权限求值器判断真假]       [通用路由装配器解析组件键]
             |                         |
       +-----+-----+              +----+----------------+
       |           |              |                     |
       v           v              v                     v
[Admin 权限指令] [页面内判断] [Admin 所选领域清单] [Admin 布局和本地页面]
       |                          |                     |
       v                          +----------+----------+
[隐藏无权限按钮]                            |
                                            v
                                  [Admin Router.addRoute]
                                            |
                                            v
                                  [Admin 侧栏、顶栏、页面]
```

这里有四层，不能因为它们都叫“权限”就放进同一个公共包：

```text
第一层：通用算法
  输入角色和权限码，回答“允许还是拒绝”

第二层：通用流程
  先取身份，再取菜单，再注册路由，再回到原目标

第三层：Web 框架接线
  把算法接到 Vue 指令，把菜单接到 Vue Router

第四层：具体 App 策略
  Admin 使用哪个 Client、布局、白名单、Store、领域页面和部署路径
```

当前仓库大致这样分布：

```text
[通用算法和流程]
packages/platform/permission
packages/platform/app-runtime
packages/domains/admin

                 由 Admin 注入实际依赖
                          |
                          v
[Admin 接线和策略]
apps/admin-web/src/application
apps/admin-web/src/directive
apps/admin-web/src/router
apps/admin-web/src/store
apps/admin-web/src/permission.ts
```

所以，“文件在 App 中”不等于“里面的每一行都只能由 Admin 使用”；它表示这个文件当前负责完成 Admin 的最终组装。

## 一步一步看

### 第一步：先分清三种容易混在一起的“权限”

当前前端同时处理三件不同的事：

```text
[页面路由权限]
后端返回哪些菜单，前端就为当前 App 注册哪些页面路线

[按钮和操作可见性]
当前角色、权限码是否允许显示某个按钮或执行某个前端动作

[后端真实鉴权]
即使有人绕过页面直接调用接口，后端仍独立决定允许或拒绝
```

`directive/permission` 处理的是第二件事，不负责动态路由。动态路由主要由路由守卫、权限 Store、领域页面清单和 Vue Router 共同完成。

```text
v-hasPermi / v-hasRoles  -> 按钮和元素是否留在页面

getRouters / addRoute    -> 页面路线是否进入当前 Router

后端鉴权注解             -> 接口是否真的允许执行
```

前端隐藏按钮只是界面体验和失败关闭，不是最终安全边界。

### 第二步：权限“算法”其实已经抽到了公共包

真正的通用规则位于 `<Path>plus-ui-namewta/packages/platform/permission/src/index.ts</Path>`：

```text
[权限快照]
permissions = 当前会话的权限码
roles       = 当前会话的角色标识
      |
      v
[createAccessEvaluator]
      |
      +-> hasPermission
      +-> hasAnyPermission
      +-> hasRole
      +-> hasAnyRole
      +-> 识别超管和通配权限
```

这个包不知道下面这些东西：

```text
不知道 Admin 的 Pinia Store
不知道 Vue 指令
不知道页面 DOM
不知道 Vue Router
不知道 Element Plus
不知道后端 Controller
```

因此它才是真正可以让不同 App 复用的“权限发动机”。

Admin 中的 `<Path>plus-ui-namewta/apps/admin-web/src/application/access.ts</Path>` 只有一个很薄的职责：

```text
[Admin 用户 Store 中的 roles 和 permissions]
                     |
                     v
          [通用 createAccessEvaluator]
                     |
                     v
             [Admin 权限求值器]
```

未来另一个 App 可以从自己的会话状态创建求值器，而不需要依赖 Admin Store。

### 第三步：为什么权限指令还在 `admin-web`

`<Path>plus-ui-namewta/apps/admin-web/src/directive/permission/index.ts</Path>` 做了两件事：

```text
事情 A：读取 v-hasPermi 或 v-hasRoles 的参数
事情 B：调用 Admin 权限求值器，无权限时移除 HTML 元素
```

事情 A 和“把求值器包装成 Vue 指令”的办法具有 Web 复用潜力；事情 B 当前明确依赖 `createAdminAccessEvaluator`，所以它还是 Admin 适配器。

`<Path>plus-ui-namewta/apps/admin-web/src/directive/index.ts</Path>` 还会把指令注册到具体的 Vue App：

```text
[Admin createApp]
      |
      v
[directive(app)]
      |
      +-> 注册 copyText
      +-> 注册 hasPermi
      +-> 注册 hasRoles
```

“注册哪些全局指令”本来就是每个 App 的启动选择。移动端或小程序甚至可能没有相同的 HTML 元素移除方式，也不一定使用 Vue DOM 指令。

未来若出现第二个真实 Vue Web App，并且它采用完全相同的指令语义，可以提炼成下面的形式：

```text
[共享 createPermissionDirectives(access)]
                     ^                 |
                     |                 v
       每个 App 注入自己的求值器     生成 Vue 指令
```

但是 App 的 `directive/index.ts` 仍会存在，因为各 App 要决定是否注册这些指令。当前只有一个激活 App，立即为了假想消费者建立完整 Web 指令包，会增加尚未被真实需求证明的抽象。

### 第四步：为什么 `router` 必须有一部分属于 App

`<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>` 不只是“路由算法”，它直接声明：

```text
Admin 使用哪种浏览器 History
Admin 部署在哪个 context path
Admin 的登录、注册、个人中心和错误页
Admin 的 Layout 组件
Admin 首页是什么
Admin 的滚动恢复规则
```

另一 App 很可能不同：

```text
Admin Web                 未来 Client Web
---------                 ---------------
/system/user              可能没有系统管理
后台管理 Layout           品牌门户 Layout
左侧菜单和顶栏            底部导航或普通页头
/register 是否开放        由自己的 Client 决定
Admin 首页                Client 自己的首页
独立 context path         独立部署路径
```

因此每个 App 都应该拥有自己的 Router 实例和静态路由。公共包不能替所有 App 决定这些产品行为。

真正通用的“把后端组件键变成页面加载器”已经位于 `<Path>plus-ui-namewta/packages/platform/app-runtime/src/routeAssembler.ts</Path>`：

```text
[后端菜单中的 component 字符串]
                 |
                 v
       [assembleServerRoutes]
                 |
        +--------+--------+
        |                 |
        v                 v
[特殊宿主组件]      [所选领域 manifest]
 Layout 等               |
        |                 v
        |           [真实 Vue 页面]
        +--------+--------+
                 |
                 v
             [可注册路由]
```

通用装配器负责遍历、解析与失败诊断；Admin 负责提供自己的 `Layout`、`ParentView`、`InnerLink` 和所选领域清单。这种边界是合理的。

### 第五步：为什么 `permission store` 不能整块变成公共 Store

`<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>` 混合了两类内容。

第一类具有复用价值：

```text
请求服务端菜单
递归处理菜单树
调用通用 route assembler
检查重复路由名称
```

第二类明确属于 Admin：

```text
Pinia Store 名称和生命周期
constantRoutes 与 dynamicRoutes
Admin 的 sidebarRouters
Admin 的 topbarRouters
Admin 的 defaultRoutes
Admin Layout / ParentView / InnerLink
Admin 本地 views 查找表
Admin manifest registry
Admin Router.addRoute
```

如果直接把整个 Store 搬进公共包，公共包就必须认识 Admin 的侧栏、顶栏、Layout 和页面目录。那不叫复用，而是把 Admin 换了一个目录存放。

```text
错误的公共化：

[公共 Store]
   |
   +-> 强依赖 Admin Layout
   +-> 强依赖 Admin 侧栏
   +-> 强依赖 Admin views
   +-> 强依赖 Admin Router

结果：未来 App 仍被迫长得像 Admin
```

更合适的方向是继续让 Store 属于 App，同时把可独立输入、输出的纯流程保持在 `platform-app-runtime`：

```text
[未来 App 自己的 Store 或状态容器]
                 |
                 +-> 提供自己的菜单服务
                 +-> 提供自己的组件清单
                 +-> 提供自己的特殊布局组件
                 +-> 调用共享菜单/路由装配流程
                 +-> 保存自己需要的导航状态
```

Admin 需要五组路由状态，不代表移动端、小程序或门户 App 也需要同样的五组状态。

### 第六步：为什么根级 `permission.ts` 仍在 App

`<Path>plus-ui-namewta/apps/admin-web/src/permission.ts</Path>` 是 Admin Router 的全局守卫。它决定：

```text
是否展示 Admin 的页面进度条
哪些 Admin 路径免登录
登录后访问 /login 要去哪里
什么时候读取 Admin 用户 Store
失败时用哪种界面提示
退出后跳到哪里
页面标题如何更新
使用哪个 Router 实例
```

这些都是 App 策略。但其中“恢复受保护页面”的顺序已经抽到 `<Path>plus-ui-namewta/packages/platform/app-runtime/src/navigationRecovery.ts</Path>`：

```text
[loadIdentity 读取身份]
          |
          v
[loadRoutes 读取并组装菜单]
          |
          v
[逐条 addRoute]
          |
          v
[replace 回原目标，避免守卫循环]
```

Admin 守卫只负责把自己的实现注入进去：

```text
loadIdentity -> Admin userStore.getInfo
loadRoutes   -> Admin permissionStore.generateRoutes
addRoute     -> Admin router.addRoute
错误提示     -> Element Plus
```

这说明当前方向不是“所有逻辑都困在 Admin”，而是“通用流程已共享，具体导航政策由 App 拥有”。

### 第七步：后端菜单到前端页面的完整调用链

用户登录后，当前真实流程是：

```text
[用户访问受保护的 Admin 地址]
                 |
                 v
[Admin permission.ts 检查本 App 的 Token]
                 |
                 v
[Admin userStore.getInfo]
                 |
                 v
[domain-admin / system identity 读取用户、角色、权限]
                 |
                 v
[Admin userStore 保存 roles 和 permissions]
                 |
                 v
[Admin permissionStore.generateRoutes]
                 |
                 v
[domain-admin 请求当前 Client 的服务端菜单]
                 |
                 v
[platform route assembler 遍历菜单组件键]
                 |
       +---------+----------+
       |                    |
       v                    v
[Admin 特殊布局]      [Admin 所选 web-domain manifest]
       |                    |
       +---------+----------+
                 |
                 v
[Admin Router.addRoute 注册动态页面]
                 |
                 v
[replace 回用户最初想去的地址]
```

按钮检查走另一条较短的链：

```text
[后端 getInfo 返回权限码和角色]
                 |
                 v
[Admin userStore 保存权限快照]
                 |
                 v
[platform-permission 统一判断]
                 |
       +---------+---------+
       |                   |
       v                   v
[v-hasPermi 指令]    [页面命令式检查]
       |                   |
       v                   v
[隐藏按钮或元素]     [禁止前端动作]
```

### 第八步：新增 App 时到底要写什么

新 App 不需要重新实现后端 API、菜单模型和权限算法。它需要做的是明确选择并接线：

```text
直接复用
  +-> domain-admin 的登录、身份和菜单服务
  +-> platform-permission 的统一求值器
  +-> platform-app-runtime 的菜单装配与导航恢复
  +-> 所选 web-domain 的页面 manifest

新 App 自己拥有
  +-> ClientId 和会话命名空间
  +-> Router 实例和静态路由
  +-> 布局、首页、登录白名单和部署路径
  +-> 选择哪些 domain / web-domain
  +-> 自己需要的导航状态
  +-> 将共享求值器接到 Vue、Taro 或原生界面的适配层
```

未来如果新增的是另一个 Vue Web App，它可能会有少量与 Admin 形状相似的接线文件，但这些文件应该很薄，是“配置共享能力”，不是复制算法。

```text
合理重复：每个 App 各有一个 20 行左右的装配入口

不合理重复：每个 App 各复制一套菜单解析、权限通配、错误判断和恢复算法
```

### 第九步：对当前目录的判断

当前边界不是完全没有优化空间，但总体原则正确：

| 当前内容 | 当前判断 | 原因 |
| --- | --- | --- |
| `platform-permission` | 已正确共享 | 是无 Vue、无 Store、无 App 的权限算法 |
| `platform-app-runtime` | 已正确共享 | 是菜单装配和导航恢复的通用流程 |
| `domain-admin` 身份与菜单服务 | 已正确共享 | 统一后端 API、响应校验和领域合同 |
| `admin-web/application/access.ts` | 应留在 App | 把共享求值器接到 Admin 用户 Store |
| `admin-web/router/index.ts` | 应留在 App | 拥有 Admin 静态页面、布局、History 和部署路径 |
| `admin-web/router/adminManifestRegistry.ts` | 应留在 App | 表达 Admin 编译期选择了哪些领域页面 |
| `admin-web/permission.ts` | 应留薄壳，可继续观察 | App 策略必须留下；共享恢复流程已经提取 |
| `admin-web/directive/permission` | 有潜在 Web 复用价值 | 目前只有一个真实消费者，且仍绑定 Admin Store |
| `admin-web/store/modules/permission.ts` | 不能整块共享，可继续拆纯流程 | 同时拥有 Admin 导航状态和部分通用树处理 |

最稳妥的目标不是把这些目录全部删除，而是：

```text
App 中保留“我选择什么、我接到哪里、我怎样展示”

共享包中保留“输入什么、按什么规则处理、输出什么”
```

当第二个真实 App 出现后，可以用两份接线代码做对比，只提取确实相同的部分。这样不会为了假想的移动端或小程序，提前把 Vue Router、DOM 指令和 Admin 侧栏错误地定义成“全终端通用”。

## 术语小词典

- 通用发动机（平台能力）：只根据输入执行稳定规则，不认识某个具体 App 的 Store、布局或品牌。
- 接线（适配、装配）：把公共能力连接到当前 App 使用的 Router、Store、界面组件和环境配置。
- 权限快照：登录后得到的一组角色标识和权限码，供前端判断页面元素是否可见。
- 权限求值器：接收权限快照和要求，回答允许或拒绝的小型规则引擎。
- 动态路由：登录后把服务端返回的菜单树转换成当前 App 可以访问的页面路线。
- 路由守卫：每次导航前执行的检查点，负责登录检查、身份恢复和跳转策略。
- 页面清单（Web domain manifest）：某个领域公开给 App 的页面组件键、加载器、语言和权限贡献清单。
- App 策略：某个终端自己决定的 Client、首页、白名单、布局、导航方式、错误提示和部署方式。
- 失败关闭：缺少 Token、身份、菜单、组件或权限时默认拒绝，不猜测为允许。
- 过早抽象：还没有多个真实使用者，就提前假定它们需求完全相同并建立公共框架。

## 你现在能复述什么

1. 动态路由与权限确实是跨 App 能力，但应该共享的是无 App 依赖的算法、合同和流程，不是把 Admin 的 Router 与 Store 整块改名为公共包。
2. 当前权限判断已经由 `platform-permission` 统一；Admin 只把自己的用户 Store 接进去。
3. 当前菜单装配与登录后导航恢复已经由 `platform-app-runtime` 统一；Admin 仍必须提供自己的 Router、布局、页面清单和导航状态。
4. `directive/permission` 处理按钮和元素可见性，不等于动态路由；它有 Web 复用潜力，但现在仍是绑定 Admin Store 的 Vue 适配器。
5. `permission store` 同时包含通用树处理和 Admin 侧栏、顶栏、布局、页面目录等状态，因此不能整块共享，只适合继续提炼其中独立的纯流程。
6. 新 App 不需要重新对接后端 API 和权限算法，只需要拥有自己的终端策略，并通过薄装配层组合已有 domain、platform 和 web-domain。

事实依据：`<Path>plus-ui-namewta/apps/admin-web/src/main.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/permission.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/directive/permission/index.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/router/index.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/store/modules/permission.ts</Path>`、`<Path>plus-ui-namewta/apps/admin-web/src/application/access.ts</Path>`、`<Path>plus-ui-namewta/packages/domains/admin/src/index.ts</Path>`、`<Path>plus-ui-namewta/packages/platform/permission/src/index.ts</Path>` 与 `<Path>plus-ui-namewta/packages/platform/app-runtime/src/</Path>`。
