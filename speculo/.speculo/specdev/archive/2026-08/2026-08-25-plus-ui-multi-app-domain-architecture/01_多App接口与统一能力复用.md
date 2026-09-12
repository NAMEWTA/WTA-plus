# 多 App 的接口与统一能力怎样复用

先说结论：新建 App 时，不应该复制 `admin-web/src/api`、重新声明后端接口和数据模型，也不应该把整个 Admin 的 Store、路由守卫和插件原样搬过去。

> 2026-08-27 contract 阶段已完成：`apps/admin-web/src/api`、领域页面 wrapper、旧 `$auth` 插件和无调用工具已删除。Admin 页面、Store 与宿主组件现在直接消费 `src/application/services.ts` 装配的正式 domain service；架构检查会拒绝任何已激活 App 重新创建 `src/api`。

当前架构采用的不是一个“全局 API 总管文件”，而是“共享核心 + App 显式装配”：

- `api-contracts` 保存后端传输数据的生成合同。
- `domains` 保存后端模块对应的接口、稳定模型和业务服务。
- `platform` 保存认证、权限、路由装配等与具体页面无关的规则。
- `adapters` 保存 Axios、浏览器存储、浏览器加密等具体实现。
- `web-domains` 保存可在多个 Vue Web App 复用的页面、组件和交互运行时合同。
- `apps` 只选择自己需要的领域，并把自己的 Client、Token、Router、Pinia、布局和提示组件接到共享能力上。

Admin 自己的 Client、HTTP、Session、权限状态和退出编排位于 `apps/admin-web/src/application` 与 App Store；它们是宿主装配，不是第二套后端 API。

## 先看全图

```text
                           [同一个后端]
                                 |
              +------------------+------------------+
              |                  |                  |
              v                  v                  v
        [身份认证接口]       [系统管理接口]       [工作流接口]
              |                  |                  |
              +------------------+------------------+
                                 |
                                 v
                    [生成的后端传输数据合同]
                           api-contracts
                                 |
                                 v
               [领域接口、稳定模型、映射和服务]
                             domains
                                 |
                   +-------------+-------------+
                   |                           |
                   v                           v
       [认证、权限、路由等规则]       [可复用的 Vue 页面和组件]
              platform                    web-domains
                   |                           |
                   +-------------+-------------+
                                 |
                                 v
                  [浏览器或小程序的具体适配器]
                            adapters
                                 |
              +------------------+------------------+
              |                  |                  |
              v                  v                  v
        [admin-web]         [client-web]       [未来小程序]
       选择全部管理域       只选择所需领域      选择无头领域
       使用管理端布局       使用独立布局主题      使用 Taro 适配器
```

一次 Web 请求的真实调用方向如下：

```text
[页面或组件]
      |
      v
[领域服务中的方法]
      |
      v
[通用 HttpClient 接口]
      |
      v
[App 创建的 Axios 浏览器适配器]
      |
      +-- 注入当前 App 的 clientId
      +-- 读取当前 App 的 Token
      +-- 按当前 App 配置加密
      +-- 使用当前 App 的错误提示和登录跳转
      |
      v
[后端 API]
```

Admin 与新 App 现在走同一种正式调用路径：

```text
[Admin 页面、Store 或宿主组件]
      |
      v
[application/services.ts]
  选择并创建当前 App 需要的 domain service
      |
      v
[@namewta/domain-demo]
  真正拥有接口、模型和服务
      |
      v
[application/http.ts] -> [后端]
```

新 App 应走更短的路径：

```text
[新 App]
      |
      +-> 创建自己的 Http、Session、Crypto 适配器
      |
      +-> 直接创建需要的 domain service
      |
      +-> 选择需要的 web-domain manifest
      |
      +-> 注入 Router、布局、提示、下载等宿主能力
      |
      v
[完成 App 装配]
```

## 一步一步看

### 第一步：后端接口没有放在每个 App 里重新维护

以 Demo 为例，真正的接口和数据模型位于：

```text
packages/domains/demo/src/index.ts
```

这个文件拥有：

```text
DemoVO、DemoForm、DemoQuery
        +
listDemo、getDemo、addDemo、updateDemo、deleteDemo
        +
/demo/demo/list、/demo/demo/{id} 等后端路径
```

`createDemoService(http)` 不依赖 Axios、Vue、Pinia、Element Plus 或浏览器。它只依赖一个很小的 `HttpClient` 合同。因此，任何 App 只要提供符合合同的请求实现，就能调用同一套 Demo 接口。

Demo 还展示了生成合同和稳定领域模型的关系：

```text
[后端 OpenAPI]
       |
       v
[api-contracts 中的 TestDemoVo]
       |
       v
[projectDemoTransport 映射]
       |
       v
[domain-demo 对外的 DemoVO]
```

这样做的原因是：后端生成模型可以随接口描述变化，而页面使用的领域模型需要保持清楚和稳定。App 不应该直接到处依赖生成文件。

### 第二步：Admin 的旧 API 门面已经清零

迁移期曾有 70 个 `admin-web/src/api` 文件，用于转发 domain service、重导出类型或包装旧 `RuoYiAjaxResult`。基座不需要兼容旧调用方，因此 contract 阶段已完成消费者迁移并删除整棵目录。

现在的唯一规则是：

```text
后端路径、传输模型、稳定业务服务 -> packages/domains
Vue Web 领域页面和组件          -> packages/web-domains
Admin 选择服务和注入宿主能力    -> apps/admin-web/src/application
Admin 布局、路由和页面状态       -> apps/admin-web/src/layout、router、store
```

Admin 注销时关闭推送与 SSE，仍属于 Admin 宿主编排；它直接调用 `identityAccessService.logout()`，不再伪装成通用登录 API。

### 第三步：统一请求能力已经复用，但每个 App 必须单独配置

`admin-web/src/application/http.ts` 负责 Admin 的请求装配，真正的通用实现来自：

```text
@namewta/adapter-axios-browser
@namewta/adapter-crypto-browser
@namewta/platform-auth
```

Admin 文件做的是下面这些装配：

```text
[通用 Axios 适配器]
      |
      +-- baseURL = Admin 的环境配置
      +-- clientId = Admin 的 Client
      +-- getToken = Admin 的 Token 存储
      +-- getLanguage = Admin 的语言
      +-- errorPresenter = Element Plus 提示
      +-- onUnauthorized = Admin Router 跳登录页
      +-- repeatSubmissions = Admin session cache
      |
      v
[Admin 专用的 HttpClient 实例]
```

`client-web/src/application.ts` 已经证明第二个 App 无需重写拦截器算法。它同样调用 `createAxiosBrowserAdapter`，但注入自己的 Client、Session key、错误展示和 401 跳转。

不能让所有 App 共用同一个已经创建好的全局请求单例，原因如下：

```text
[admin-web]  clientId=A，Token=A，登录页=/login

[client-web] clientId=B，Token=B，登录页=/login

如果共享同一个实例
       |
       v
Client、Token 或 401 跳转可能串线
```

应当复用“怎样发请求”的工厂和算法，但为每个 App 创建独立实例。

### 第四步：动态路由的算法可复用，实际路由表必须由 App 拥有

`admin-web/src/store/modules/permission.ts` 中同时存在共享部分和 App 部分。

共享部分已经提取到 `platform-app-runtime`：

```text
后端菜单字符串
      |
      v
assembleServerRoutes
      |
      +-- 按 domainId 和 componentKey 找 manifest 注册
      +-- 检查未选择领域、缺失组件和重复注册
      |
      v
可交给 Vue Router 的路由记录
```

留在 Admin Store 中的是：

```text
Admin 的 constantRoutes 和 dynamicRoutes
Admin 的 Layout、ParentView、InnerLink
Admin 的 Router 实例和 router.addRoute
Admin 左侧栏、顶部栏、默认路由的 Pinia 状态
Admin 自有静态 views 的兜底查找
Admin 的重复路由 UI 提示
```

这些内容不是后端 API 合同，而是管理端壳层的状态。手机端可能没有侧边栏，小程序也不使用 Vue Router；即使两个 Web App 都用 Vue Router，它们的布局、白名单和首页也可能不同。

正确复用方式是：

```text
[共享路由装配规则]
         |
         +-> [admin-web 注入管理端布局和 Router]
         |
         +-> [client-web 注入客户端布局和 Router]
         |
         +-> [未来移动端注入自己的导航实现]
```

所以 `permission.ts` 不应整体搬入 `platform`。其中的纯算法应复用，具体 Router、布局和 Store 状态应继续由 App 拥有。

### 第五步：`permission.ts` 路由守卫也是“共享流程 + App 接线”

Admin 的顶层 `permission.ts` 使用了共享的 `restoreProtectedNavigation`，但仍需要在 App 内注册 `router.beforeEach`，因为它拥有：

- Admin 的免登录白名单。
- Admin 的页面标题和进度条。
- Admin 的用户 Store、权限 Store 和 Router。
- Admin 加载身份失败后的退出和提示方式。
- Admin 对外链路由的判断。

共享流程只规定：

```text
[已有 Token 但身份和路由尚未恢复]
            |
            v
[加载身份] -> [加载服务端路由] -> [逐条 addRoute] -> [替换当前导航]
```

每个 App 决定“如何加载”“加到哪个 Router”“失败后去哪里”。这就是依赖注入，而不是重复实现。

### 第六步：按钮权限和角色权限已经复用核心判断

`admin-web/src/application/access.ts` 是 Admin 唯一的命令式权限绑定：

```text
[Admin user Store 中的 roles 和 permissions]
                     |
                     v
[@namewta/platform-permission 的 createAccessEvaluator]
                     |
                     v
[Admin 路由、指令和页面使用同一 AccessEvaluator]
```

权限集合的判断算法是共享的。Admin 文件留下来，是因为它要从 Admin 的 Pinia Store 取得当前用户数据。

新 App 可以复用 `createAccessEvaluator`，但不复制 Admin 的 `application/access.ts`。它应从自己的会话状态取角色权限，并按终端需要提供 Vue 指令、组件属性或导航守卫。

### 第七步：用户 Store 可复用身份服务，不必强制共用同一个 Store

`admin-web/src/store/modules/user.ts` 负责：

```text
Admin 页面需要的 token、name、nickname、avatar
roles、permissions、userId
登录后怎样更新这些 Ref
退出后怎样清理 Admin 会话
Admin 默认头像和展示状态
```

其中登录、退出、获取用户信息的后端调用来自 `domain-identity-access`，已经能够复用。Store 本身描述的是 Admin 的前端状态形状和生命周期，不是后端接口模型。

如果以后多个 Vue Web App 的用户状态需求完全一致，可以在 `web-domain-identity-access` 或专门的 Vue 状态包中抽取 Store 工厂。但不宜现在把 Admin Store 直接宣布为全终端通用 Store：小程序的存储、页面生命周期、导航和用户展示字段都可能不同。

### 第八步：字典分成数据、缓存和响应式展示三层

字典并不是一个只能放在 `utils` 的整体能力。它至少包含三层：

```text
[字典后端接口和 DictDataVO]
          domains/system-admin
                    |
                    v
[把字典结果变成 Vue Ref 的桥接]
       web-domains/system-admin
                    |
                    v
[Admin Pinia 缓存、并发去重和 useDict 旧入口]
        apps/admin-web/src/utils/dict.ts
```

目前字典接口和模型已经进入 `domain-system-admin`，`createLiveSystemDictRefs` 也已进入 `web-domain-system-admin`。Admin 的 manifest runtime 仍通过 `@/utils/dict` 注入旧缓存实现。

因此这里存在继续优化的空间：

```text
如果多个 Vue Web App 都需要相同缓存语义
        |
        v
把缓存和 useDict 工厂提到 web-domain 或共享 Vue 工具包

如果只有 Admin 需要当前 Pinia 缓存形状
        |
        v
继续留在 Admin，但只作为 runtime 的一种实现
```

无论哪种选择，都不应把 Vue `reactive`、Pinia Store 和 Element Plus 类型塞进无头的 `domains/system-admin`。

### 第九步：加密需要区分通信加密和普通工具函数

请求与登录通信所需的 RSA、AES 能力已经由浏览器加密适配器提供，再由每个 App 按环境配置决定是否启用。

`admin-web/src/utils/crypto.ts` 是另一组旧 CryptoJS 工具。当前生产源码搜索只找到它自己的定义，没有找到调用方。这说明它是很强的清理候选，而不是新 App 应该复制的公共能力。

清理前仍应执行完整类型检查、单元测试和构建，确认没有自动导入、生成代码或运行时字符串引用。若未来确有领域无关的调用者，应把经过测试的最小能力放入 `adapter-crypto-browser` 或独立加密包，而不是继续堆在 Admin 的 `utils`。

### 第十步：OSS 的后端服务可复用，浏览器交互按层放置

OSS 当前已经有较清楚的边界：

```text
[OSS 类型、列表、下载地址、分片初始化、签名、续传、完成]
                 domains/system-admin
                            |
                            v
[OSS 管理页面和它需要的 runtime 合同]
               web-domains/system-admin
                            |
                            v
[Admin 的确认框、下载器、上传 Header、图片预览、页面关闭]
                  admin-web 注入
```

Admin 中仍有 `hooks/oss` 和 `utils/oss` 形式的浏览器上传实现。若其他 Web App 也需要完全相同的直传和断点续传行为，可以把与 Admin UI 无关的上传状态机提到 adapter 或 web-domain，把确认框、进度展示和文件选择继续作为宿主端口。小程序则复用领域接口，但使用 Taro 网络和文件 API 的适配器。

### 第十一步：`plugins`、`store`、`utils` 不能按目录名整体搬家

判断一段代码能否复用，应该看它“拥有谁的状态和依赖”，而不是看目录叫不叫 `utils`。

| 代码性质 | 应放位置 | 例子 |
| --- | --- | --- |
| 后端模块的接口、模型、映射和服务 | `packages/domains/<domain>` | Demo、系统字典、OSS、登录服务 |
| Vue 页面、组件、Hook 和宿主端口合同 | `packages/web-domains/<domain>` | 系统管理页面、字典 Ref、OSS 页面 |
| 与具体业务无关的规则和抽象接口 | `packages/platform/*` | 权限判断、路由装配、认证恢复 |
| 浏览器或小程序的具体实现 | `packages/adapters/*` | Axios、浏览器存储、浏览器加密、Taro 请求 |
| 多个 Web 领域都使用的视觉基础设施 | `packages/web-kit/*` | 通用表格、工具栏、设计令牌 |
| App 的实例、配置、导航、布局和状态绑定 | `apps/<app>` | Router、Pinia、白名单、主题、Element 提示 |
| 为旧调用方保留的临时门牌号 | 旧 App 内的 facade | `@/api/demo/demo`、旧页面 wrapper |

例如：

```text
modal 插件中的“用 Element Plus 弹窗” -> Web 宿主实现
某领域要求“操作前确认”              -> web-domain runtime 端口

cache 插件中的具体 sessionStorage     -> 浏览器适配器或 App 实例
缓存接口合同                          -> platform contract

tab 插件中的“关闭当前 Admin 标签页”   -> Admin 壳层行为
某页面完成后希望关闭                  -> web-domain 调用宿主端口
```

共享的是合同和稳定算法，App 保留最终实例和体验决定。

### 第十二步：新建 Web App 时应该怎样接入

`client-web` 已经给出可以照用的最小模式：

```text
1. 在 apps/new-web 中定义 App 配置和 ClientContext
                  |
                  v
2. 创建独立 SessionStore、CryptoPort 和 HttpClient
                  |
                  v
3. 从 domains 直接创建所需服务
                  |
                  v
4. 为所需 web-domains 提供 runtime 依赖
                  |
                  v
5. 在 composition.ts 显式列出 selectedDomainIds
                  |
                  v
6. 创建自己的 Router、Store、布局、主题和入口
```

例如一个只需要登录和 Demo 的 App：

```text
selectedDomainIds
  +-- identity-access
  +-- demo

selectedManifestIds
  +-- web-domain-identity-access
  +-- web-domain-demo
```

它不会自动携带 workflow、system-admin 或 ai，也不需要复制这些领域的 API 文件。

### 第十三步：未来小程序和移动端怎样复用

小程序或原生移动端可以复用无头部分：

```text
可以直接复用
  +-- api-contracts
  +-- domains
  +-- platform 中不依赖浏览器和 Vue 的合同与算法

需要换实现
  +-- Axios 浏览器适配器 -> Taro request 或原生网络适配器
  +-- localStorage       -> Taro storage 或原生安全存储
  +-- Vue Router         -> 小程序导航或原生导航
  +-- Element Plus 页面  -> 小程序或移动端自己的 UI
```

这正是 `domains` 与 `web-domains` 分开的价值：同一套后端接口和领域模型可以跨终端复用，Vue Web 页面不会反向绑住小程序。

### 第十四步：contract 阶段已经完成了哪些收缩

本轮按引用和行为测试完成了以下收缩：

```text
[删除 src/api 和领域页面 wrapper]
       + [request/auth/plugin 移入 application]
       + [权限求值统一到 application/access]
       + [删除旧 crypto、jsencrypt、i18n、utils 大杂烩]
       + [增加 app-api-facade 架构规则]
       -> [lint + typecheck + test + build]
```

后续仍按真实复用证据推进：字典缓存、OSS 直传状态机和基础 UI 只有出现第二个稳定消费者后，才评估进入 platform、adapter 或 web-kit；Store、布局和 Router 继续由 App 拥有。

## 判断清单

看到一段位于 App 内的代码时，可以按下面顺序判断：

```text
它是否声明后端路径或后端数据模型？
  |
  +-- 是 -> 应优先进入对应 domain
  |
  +-- 否
       |
       v
它是否依赖 Vue 页面、Ref、组件或交互？
  |
  +-- 是，且多个 Web App 可用 -> web-domain 或 web-kit
  |
  +-- 否
       |
       v
它是否是跨业务的纯规则或端口？
  |
  +-- 是 -> platform
  |
  +-- 否
       |
       v
它是否实现浏览器、Taro 或原生能力？
  |
  +-- 是 -> adapter
  |
  +-- 否
       |
       v
它是否绑定当前 App 的 Router、Store、Client、布局或提示？
  |
  +-- 是 -> 留在 App 作为装配
  |
  +-- 否
       |
       v
它是否只是旧导入路径或已经没有调用者？
  |
  +-- 旧路径 -> 标记 facade，消费者清零后删除
  +-- 无调用 -> 经测试验证后删除
```

新 App 的禁止事项：

- 不复制 `admin-web/src/api` 作为新 API 层。
- 不复制 Admin 的全局 request 单例。
- 不与 Admin 共用同一个 Token key 或可变请求实例。
- 不把 Admin 的 Pinia Store 当作所有终端的领域模型。
- 不把 Vue、Element Plus、Router 或浏览器 API 放进无头 domain。
- 不靠扫描目录自动加载全部领域；每个 App 应显式选择领域和 manifest。

新 App 的正确动作：

- 复用 `domains` 的服务和模型。
- Web App 按需复用 `web-domains` 的页面、组件和 Hook。
- 复用 `platform` 的权限、认证和路由规则。
- 按终端选择或实现 adapter。
- 在 App 内创建独立 ClientContext、Session、Router 和 UI 端口。
- 只组合这个 App 真正需要的领域。

## 你现在能复述什么

读完后，应当能复述下面三个答案：

```text
问题一：新 App 是否要重新实现后端接口和数据模型？

答案：不需要。
      直接复用 api-contracts 和 domains，Web 页面按需复用 web-domains。
      Admin 已不存在 src/api；App 直接装配并调用 domain service。

问题二：认证、权限、动态路由、字典、加密和 OSS 是否能够复用？

答案：能够复用稳定合同、领域服务和通用算法。
      但每个 App 仍要注入自己的 Client、Token、Router、Store、布局和交互。

问题三：为什么 plugins、store、utils 和 permission.ts 还在 App 里？

答案：留下的内容拥有 Admin 的运行时实例和体验，应该留在 App；
      可复用接口、模型和领域页面已经进入共享包；
      新的提取必须由真实多消费者证据驱动。
```

最短记忆方式是：

```text
共享“做事规则和后端合同”
        +
App 自己拥有“实际实例和用户体验”
        +
App 内不再保留旧门面，也不允许新建 src/api
```

## 术语小词典

- 共享核心（shared core）：多个 App 都能使用、又不绑定某个 App 页面和实例的代码。
- 显式装配（composition）：App 清楚列出自己选择哪些领域，并把 Router、Client、Token、提示框等实际对象接上去。
- 无头领域（headless domain）：只处理接口、数据和业务规则，不知道页面、浏览器或 UI 框架的领域包。
- Web 领域（web-domain）：面向 Vue Web App 的页面、组件、Hook 和运行时端口；不承诺直接运行在小程序或原生端。
- 适配器（adapter）：把统一合同接到具体技术上的实现，例如把 `HttpClient` 接到 Axios，把存储合同接到 localStorage。
- 端口（port）：共享代码提出的“我需要一个什么能力”的小接口，App 决定用什么工具实现它。
- 兼容门面（compatibility facade）：本次迁移期曾使用、现已从 Admin 删除的临时旧路径；架构规则禁止在已激活 App 中恢复。
- 传输模型（transport model）：后端在线路上实际发送的数据形状，通常由 OpenAPI 合同描述。
- 领域模型（domain model）：前端领域对页面和业务公开的稳定数据形状，可通过映射与传输模型隔离。
- ClientContext：当前 App 的后端客户端身份，至少包含 `clientId`；它参与登录、请求头、权限和会话隔离。
- Manifest：Web 领域提交给 App 的清单，列出页面组件键、权限贡献和语言消息；App 只有显式选择后才会装载。
- 宿主（host）：最终运行某个 App 的外壳，它拥有 Router、Pinia、布局、主题、弹窗和环境配置。
