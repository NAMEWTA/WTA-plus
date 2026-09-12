# Admin 页面搬到哪里了

这份图解回答三个问题：

1. 为什么 `admin-web/src/views` 里看不到原来的系统、工作流、演示等页面？
2. 这些页面现在放在哪里？
3. 后端动态菜单怎样找到已经搬走的 Vue 页面？

## 先看全图

页面没有消失。变化是从“每个 App 自己保存一份页面”，变成“可复用页面放进共享 Web 领域，App 只负责选择和组装”。

```text
重构前

[admin-web]
  |
  +-- src/views/system       系统管理页面
  +-- src/views/workflow     工作流页面
  +-- src/views/demo         演示页面
  +-- src/views/monitor      运维页面
  +-- src/views/ai           AI 页面
  +-- src/views/tool         代码生成页面


重构后

[admin-web：应用外壳]
  |
  +-- src/views              只保留 Admin 自己拥有的页面
  +-- src/application        组装服务、请求、会话和宿主能力
  +-- src/router             选择需要的 Web 领域
  |
  +----选择----> [packages/web-domains/system-admin]
  +----选择----> [packages/web-domains/workflow]
  +----选择----> [packages/web-domains/operations]
  +----选择----> [packages/web-domains/demo]
  +----选择----> [packages/web-domains/devtools]
  +----选择----> [packages/web-domains/ai]
```

最重要的一句话是：

```text
admin-web/src/views 变少
        !=
用户能看到的页面变少
```

## 一步一步看

### 第一步：哪些页面仍属于 Admin 自己

当前 `<Path>plus-ui-namewta/apps/admin-web/src/views</Path>` 保留的是应用专属页面：

```text
admin-web/src/views
  |
  +-- login.vue                         Admin 自己的登录外观
  +-- register.vue                      Admin 自己的注册外观
  +-- index.vue                         Admin 首页
  +-- error/401.vue                     Admin 错误页
  +-- error/404.vue                     Admin 错误页
  +-- redirect/index.vue                Admin 跳转页
  +-- system/user/profile/*             当前 Admin 的个人中心组合
```

这些页面直接拥有 Admin 的布局、交互或产品体验，所以仍留在 App 内。

### 第二步：原来的领域页面搬到了哪里

可被多个 Web App 使用的 Vue 页面，位于 `<Path>plus-ui-namewta/packages/web-domains</Path>`：

| 原来的页面类别 | 现在的位置 | 例子 |
| --- | --- | --- |
| 系统管理 | `packages/web-domains/system-admin/src/views` | 用户、角色、菜单、部门、字典、参数、OSS |
| 工作流 | `packages/web-domains/workflow/src/views` | 分类、定义、设计器、任务、实例、请假 |
| 工作流组件 | `packages/web-domains/workflow/src/components` | 审批按钮、流程图、用户选择、审批记录 |
| 运维监控 | `packages/web-domains/operations/src/views` | 在线用户、缓存、日志、通知、外部监控 |
| 演示功能 | `packages/web-domains/demo/src/views` | 测试单、测试树 |
| 代码生成 | `packages/web-domains/devtools/src/views` | 生成器列表、编辑页、导入表弹窗 |
| AI | `packages/web-domains/ai/src/views` | AI 聊天页面 |

例如，用户管理页面的真实文件现在是：

```text
packages/web-domains/system-admin/src/views/UserPage.vue
```

工作流任务页面的真实文件现在是：

```text
packages/web-domains/workflow/src/views/TaskListPage.vue
```

它们不再需要复制回 `admin-web/src/views`。

### 第三步：后端菜单字符串现在是“取件号码”

后端仍会返回熟悉的字符串，例如：

```text
system/user/index
workflow/task/taskWaiting
monitor/online/index
demo/demo/index
```

重构前，这些字符串很像磁盘文件路径。重构后，它们是逻辑组件键，可以把它理解成取件号码：

```text
[后端菜单]
component = "system/user/index"
             |
             v
[组件登记表查号码]
"system/user/index" -> UserPage.vue
             |
             v
[浏览器显示用户管理页面]
```

所以不能再用下面的方式判断页面是否存在：

```text
错误判断：
没找到 admin-web/src/views/system/user/index.vue
                         |
                         v
                    页面被删了
```

正确判断是：

```text
正确判断：
查看 web-domain 的组件登记表
              |
              v
逻辑组件键是否登记到真实 Vue 页面
```

### 第四步：Web 领域怎样登记页面

每个 Web 领域都有一个清单（专业名：`WebDomainManifest`）。

系统管理领域在 `<Path>plus-ui-namewta/packages/web-domains/system-admin/src/index.ts</Path>` 中登记了类似关系：

```text
system/client/index   -> ClientPage.vue
system/user/index     -> UserPage.vue
system/role/index     -> RolePage.vue
system/menu/index     -> MenuPage.vue
system/dict/index     -> DictPage.vue
system/oss/index      -> OssPage.vue
```

工作流、运维、演示、AI 和代码生成也各自登记自己的组件键。

清单不是后端接口，也不是页面副本。它只负责说明：

```text
这个领域叫什么
      +
它提供哪些页面键
      +
每个键应懒加载哪个 Vue 页面
      +
它贡献哪些权限和语言消息
```

### 第五步：Admin 明确选择需要哪些领域

`<Path>plus-ui-namewta/apps/admin-web/src/router/adminManifestRegistry.ts</Path>` 是 Admin 的 Web 领域组合处。

当前 Admin 明确选择了：

```text
[admin-web]
  |
  +-- identity-access
  +-- demo
  +-- devtools
  +-- workflow
  +-- system-admin
  +-- ai
  +-- operations
```

这意味着页面不是全局自动出现，而是由 App 主动选入：

```text
App 选择了某个 Web 领域
          |
          v
该领域的页面登记表进入当前 App
          |
          v
后端菜单可以通过组件键找到页面
```

另一个 App 可以只选择登录和 Demo，而不携带系统管理、工作流或 AI。

### 第六步：一次动态菜单加载的完整过程

下面以用户管理页面为例：

```text
[用户登录]
    |
    v
[后端按 Client 返回菜单]
component = "system/user/index"
    |
    v
[permission Store 接收菜单]
    |
    v
[assembleServerRoutes 读取组件键]
    |
    v
[adminManifestRegistry 查找登记]
    |
    v
[web-domain-system-admin 找到 UserPage.vue]
    |
    v
[Admin 注入服务、字典、提示框等宿主能力]
    |
    v
[Vue Router 注册路由]
    |
    v
[用户看到用户管理界面]
```

其中：

- `permission Store` 负责接收后端菜单并组装路由。
- `adminManifestRegistry` 负责汇总 Admin 已选择的页面登记表。
- `web-domain-system-admin` 拥有用户管理页面。
- `application/services.ts` 提供真正调用后端的领域服务实例。
- Admin 继续提供自己的弹窗、下载、Router、Store 和布局。

### 第七步：为什么旧 wrapper 必须删除

迁移期间曾出现这种结构：

```text
admin-web/src/views/system/user/index.vue
                |
                v
只转发到 web-domain 的 UserPage.vue
```

它只是一个旧门牌号，没有自己的业务价值。如果永久保留，会让开发者误以为有两个页面实现：

```text
[Admin wrapper] + [Web-domain 页面]
        |
        v
新 App 不知道应该复制哪一个
```

现在删除 wrapper 后，所有 App 都面对同一个正式入口：

```text
[web-domain 页面]
      ^       ^
      |       |
[admin-web] [未来其他 Web App]
```

这正是“基座无需兼容”的含义：不保留已经没有调用者的旧路径。

### 第八步：如果组件键写错会怎样

系统不会静默显示空白页。

```text
[后端返回未知组件键]
             |
             v
[Manifest 没有登记，本地 App 页面也不存在]
             |
             v
[显示页面加载失败诊断]
App + domain + component key
```

例如 `system/user/not-a-physical-facade` 不存在时，会显示可定位的缺失组件信息。这样比保留大量空 wrapper 更容易发现菜单配置错误。

### 第九步：新增 App 时怎样复用这些界面

新 App 不需要重建同名 `src/views`：

```text
[新 Web App]
    |
    +-- 选择需要的 domain service
    |
    +-- 选择需要的 web-domain manifest
    |
    +-- 注入自己的 Client、会话、Router、布局和提示框
    |
    v
[直接得到所选领域的共享页面]
```

只有当新 App 需要完全不同的用户体验时，才实现自己的页面，并继续复用同一个无头 domain 的接口、模型和服务。

## 容易误解的地方

### 误解一：所有 Vue 页面都应该放在 App 的 views

不是。只有 App 专属页面放在 App；跨 Web App 可复用的业务页面放在 web-domain。

### 误解二：后端 component 字符串必须对应物理文件路径

不是。它现在是稳定的逻辑键，由 manifest 映射到真实文件。

### 误解三：页面移到 packages 后就不能定制

不是。Web-domain 页面通过 runtime 接收宿主能力；不同 App 可以注入不同布局、反馈、下载、导航和会话实例。需要完全不同页面时，也可以只复用 domain service。

### 误解四：Admin 的 views 越少，Admin 功能就越少

不是。Admin 的最终页面集合等于：

```text
Admin 专属页面
      +
Admin 选择的全部 Web 领域页面
      =
Admin 实际可用页面
```

当前 47 个端到端场景已经验证登录、动态菜单、系统管理、工作流、运维、Demo、AI、代码生成和多 App 隔离仍能工作。

## 你现在能复述什么

```text
问题一：页面是不是被删没了？

答案：没有。
      删除的是 Admin 内重复的旧路径和 wrapper；
      正式页面已经移动到 packages/web-domains。


问题二：后端菜单怎样找到搬走的页面？

答案：后端 component 是逻辑组件键；
      Admin 汇总所选 Web-domain manifest，
      再把组件键解析为真正的 Vue 页面。


问题三：为什么还保留少量 admin-web/src/views？

答案：登录外观、首页、错误页、跳转页和当前个人中心
      仍拥有 Admin 专属体验，所以由 Admin 自己负责。
```

最短记忆方式：

```text
App 保存自己的外壳和专属页面
          +
Web-domain 保存可复用的 Vue 业务页面
          +
Manifest 把后端组件键接到真实页面
```

## 术语小词典

- 应用专属页面（App-owned view）：只属于某个 App 的页面，例如 Admin 的首页、错误页和定制登录页。
- 共享 Web 页面（Web-domain view）：能够被多个 Vue Web App 组合使用的业务页面。
- 页面登记表（WebDomainManifest）：记录“组件键对应哪个页面”的公开清单。
- 逻辑组件键（component key）：后端菜单返回的稳定页面编号，例如 `system/user/index`；它不再等于磁盘文件路径。
- 组装（composition）：App 明确选择所需领域，并把自己的 Router、Client、会话、布局和交互能力接上去。
- 宿主能力（runtime port）：共享页面提出的能力要求，例如“显示成功提示”或“关闭当前标签页”，由具体 App 提供实现。
- 懒加载（lazy loading）：进入某个路由时才加载对应页面，避免启动时加载所有界面。
- 包装页（wrapper）：自身几乎没有逻辑，只把旧路径转发到新页面的临时文件；本次零兼容收口已经删除。
