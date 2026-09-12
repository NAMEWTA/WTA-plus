# 登录、注册、Client 与动态路由总览

先说结论：相对 upstream，NAMEWTA 不再把登录、注册、角色和菜单看成一套全平台共享的入口规则，而是先确定用户从哪个 Client 进入，再只装配这个 Client 对应的登录域、角色、权限和路由。

## 先看全图

```text
[浏览器打开某个 APP]
          |
          v
[读取这个 APP 的 Client 上下文]
          |
          +-- Client 是否启用？
          +-- 是否允许注册？
          +-- 使用哪个登录域？
          +-- 注册后给哪个默认角色？
          |
          v
[登录或注册]
          |
          v
[只装配当前 Client 的角色、菜单和权限]
          |
          v
[生成带 Client 主键和登录域的 Token 会话]
          |
          v
[后端返回当前 Client 可见的菜单树]
          |
          v
[前端把菜单树变成动态路由]
```

可以把 Client 理解成一座大楼里的“入口门”。同一个人可以从不同入口进入，但每个入口只发放自己的门禁卡、楼层目录和操作权限。

这里的 Client 不是租户。它约束登录准入、角色、菜单、权限和路由上下文，不自动把所有业务数据按 Client 分表或过滤。

## 一步一步看

### 第一步：Client 从一条配置记录变成登录入口

upstream 已经有 Client 配置，但当前版本进一步让 Client 真正参与认证授权：

```text
[OAuth clientId 字符串]
          |
          v
[找到 sys_client 记录]
          |
          +-- 得到 Client 数据库主键 clientPk
          +-- 得到登录域 userType
          +-- 得到注册开关
          +-- 得到默认角色
          |
          v
[形成这一次登录的入口上下文]
```

要注意两个容易混淆的值：

- `clientId` 是前端配置和登录请求携带的 OAuth Client 标识。
- `clientPk` 是 `sys_client.id` 数据库主键，进入会话后用于角色、菜单和权限查询。

找不到 Client、Client 被停用或登录域无效时，系统会直接拒绝，不退回一套更宽松的全局权限。

### 第二步：登录域从用户单值变成可维护关系

登录人群分组（登录域，专业名 `userType`）回答的是：“这个用户属于哪类登录人群，能否从这个 Client 进入？”

```text
[用户]
  |
  +-- [用户与登录域关系] -- [管理端授予]
  |                         [用户注册获得]
  |
  v
[Client 指定一个登录域]
  |
  v
[用户必须拥有该登录域，才能从此入口登录]
```

当前版本增加了登录域定义和用户关系，不再只依赖用户表中的一个固定字符串。管理员可以维护登录域，也可以给一个用户授予多个登录域。

### 第三步：登录页和注册页先读取公开 Client 上下文

浏览器还没有 Token 时，也需要知道当前入口是否可用。因此登录页和注册页先读取一个不要求登录的 Client 上下文。

```text
[登录页 / 注册页]
          |
          v
[请求当前 Client 的公开上下文]
          |
          +-- clientEnabled 必须精确等于 true
          +-- registerEnabled 必须精确等于 true
          |
          v
[决定是否开放登录或注册动作]
```

前端不把 `1`、`"true"` 或非空字符串猜成布尔真值。只有后端返回真正的 `true` 才开放相应入口；上下文缺失或格式错误时默认关闭。

### 第四步：注册策略从全局开关移到 Client

upstream 的注册更像一个平台级开关。当前版本把它放到具体 Client：

```text
[用户从 Client A 注册]
          |
          +-- A 是否启用注册？
          +-- A 指定的登录域是否有效？
          +-- A 的默认角色是否有效且属于 A？
          |
          v
[创建用户]
          |
          +-- 建立用户与登录域关系
          +-- 分配 Client A 的默认角色
          |
          v
[用户只能按 Client A 的合同进入]
```

因此不同 APP 可以分别决定是否开放注册、注册到哪个登录域以及注册后的起始角色，不再共享一个模糊的默认值。

### 第五步：登录成功时只装配当前 Client 的权限

验证用户名、密码或其他认证凭据只是第一步。确认用户身份后，系统还会并行查询当前 Client 下的四类信息：

```text
                     +--> [菜单权限码]
                     |
[userId + clientPk] -+--> [角色标识]
                     |
                     +--> [完整角色与数据权限]
                     |
                     +--> [岗位]
                              |
                              v
                        [组装 LoginUser]
```

同一个用户从 Client A 登录时，不会把 Client B 的角色、菜单或权限一起装进会话。完整细节见本 change 的第 02、03 篇图解。

### 第六步：Token 保存的不只是“谁登录了”

登录参数会把这次入口的关键上下文交给 Sa-Token：

```text
[LoginUser]
    |
    +-- userId
    +-- clientPk
    +-- userType
    +-- 当前 Client 的角色和权限
    |
    v
[SaLoginParameter]
    |
    v
[生成 Token + Redis 会话]
```

之后每个请求都可以从登录会话恢复当前 Client 和登录域，不需要靠前端每次重新声明权限边界。

### 第七步：动态路由来自当前 Client 的菜单树

登录成功后，前端不会把全部页面一次性写死进路由表，而是请求当前用户可见的菜单：

```text
[前端请求 getRouters]
          |
          v
[后端从 Token 读取 userId + clientPk]
          |
          v
[查询当前 Client 下、当前用户可见的菜单树]
          |
          v
[返回 RouterVo 列表]
          |
          v
[前端 filterAsyncRouter 转成 Vue 路由]
          |
          v
[addRoute 动态加入路由器]
```

菜单权限码和页面路由有关，但不是同一个东西：权限码用于按钮或接口授权；RouterVo 用来决定左侧菜单和可进入的页面。

### 第八步：配置变化后会精确清理旧会话

如果管理员修改了用户、角色、Client 或登录域，旧 Token 里可能还保存旧权限。当前版本增加了更精确的会话失效：

```text
[角色、用户、Client 或登录域发生关键变化]
                    |
                    v
[定位受影响的 userId + clientPk + userType]
                    |
                    v
[清理对应会话]
                    |
                    v
[用户重新登录，获得最新权限]
```

目标不是踢掉所有在线用户，而是尽量只让真正受影响的入口会话失效。

### 相对 upstream 的主要文件范围

| 范围 | 主要变化 |
| --- | --- |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java</Path>` | 登录前解析 Client，公开 Client 上下文，并把 Client 交给认证策略。 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java</Path>` | 按 userId + clientPk 装配菜单、角色、数据权限和岗位。 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/SysRegisterService.java</Path>` | 按 Client 注册开关、登录域和默认角色完成注册。 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysClient.java</Path>` | Client 增加登录域、注册策略和默认角色配置。 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysUserType.java</Path>` 与 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysUserTypeRel.java</Path>` | 新增登录域定义和用户关系。 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysPermissionServiceImpl.java</Path>` | 权限查询增加 clientPk 维度。 |
| `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/controller/system/SysMenuController.java</Path>` | `getRouters` 按当前会话的 userId + clientPk 查询菜单。 |
| `<Path>plus-ui-namewta/src/views/login.vue</Path>` 与 `<Path>plus-ui-namewta/src/views/register.vue</Path>` | 先读取 Client 上下文，并按严格布尔值开放登录/注册。 |
| `<Path>plus-ui-namewta/src/store/modules/permission.ts</Path>` | 将后端菜单转换并注册为当前会话的动态路由。 |
| `<Path>plus-ui-namewta/src/views/system/userType/index.vue</Path>` | 新增登录域管理页面。 |

## 术语小词典

- 客户端入口（Client）：一个 APP、小程序或管理端的登录入口，不是浏览器软件。
- OAuth Client 标识（clientId）：登录请求用来找到 Client 配置的字符串。
- Client 数据库主键（clientPk）：`sys_client.id`，进入会话后用于角色、菜单和权限查询。
- 登录人群分组（登录域、userType）：规定一个 Client 面向哪一类用户。
- 角色权限控制（RBAC）：先给角色分配菜单和操作，再把角色分给用户。
- 默认拒绝（失败关闭、fail-closed）：缺少关键 Client、登录域或权限上下文时直接拒绝，不猜一个宽松结果。
- 登录会话（Token Session）：服务端为一次登录保存的身份与权限快照。
- 动态路由：登录后根据服务端返回的菜单临时加入前端的页面路线。
- 菜单权限码：控制按钮或操作的字符串，例如某个新增、修改权限；它不等于页面路由。
- 精确会话失效：只清理受某个用户、Client 或登录域变化影响的 Token。

## 你现在能复述什么

1. Client 是登录入口和认证授权上下文，不是完整租户边界。
2. 登录页和注册页先读取 Client 上下文；注册开关、登录域和默认角色都属于具体 Client。
3. 登录成功后，后端只装配当前 userId + clientPk 下的角色、菜单、权限和数据权限，并把它们保存进 Token 会话。
4. 前端再请求当前 Client 可见的菜单树，把 RouterVo 转成动态路由；不会加载其他 Client 的页面。
5. 用户、角色、Client 或登录域改变后，系统会清理受影响的旧会话，让用户重新取得最新权限。

事实依据：`<Path>docs/upstream/customization-map.md</Path>`、当前认证/注册/Client/角色/菜单源码，以及已归档的 Client RBAC Spec 与 Evidence。
