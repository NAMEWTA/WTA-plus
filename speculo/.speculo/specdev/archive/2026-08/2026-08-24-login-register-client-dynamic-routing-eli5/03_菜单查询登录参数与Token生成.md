# 菜单查询、登录参数与 Token 生成

这篇图解回答 5 个连续问题：

1. `SysMenuMapper.selectMenuPermsByUserId` 怎样从 4 张表找到权限码？
2. 为什么菜单 Service 显式补默认角色，而角色 Service 表面上没有补？
3. `IAuthStrategy.buildLoginParameter` 构建的登录参数有什么作用？
4. 为什么 `LoginHelper.login` 还要继续向同一个 `model` 追加 `setExtra`？
5. 登录成功后，`getTokenValue()` 和 `getTokenTimeout()` 的值从哪里来？

## 先看全图

```text
第一段：装配权限

[当前 userId + 当前 clientId]
              |
              v
[用户角色关系] -> [角色] -> [角色菜单关系] -> [菜单 perms]
              |                                  |
              |                                  v
              |                         [菜单权限码集合]
              |
              +-> [显式角色列表]
                         |
[Client 默认角色] -------+
                         |
                         v
                  [角色 roleKey 集合]


第二段：生成登录会话

[Client 配置] --------------------+
                                 |
                                 v
                        [SaLoginParameter model]
                                 ^
                                 |
[认证成功后的 LoginUser] --------+
                                 |
                                 v
                         [StpUtil.login]
                                 |
                    +------------+-------------+
                    |                          |
                    v                          v
          [生成签名 JWT Token]       [Redis 保存 Token 映射和 TTL]
                    |                          |
                    +------------+-------------+
                                 |
                                 v
                     [当前请求上下文记住 Token]
                                 |
                    +------------+-------------+
                    |                          |
                    v                          v
          [getTokenValue()]          [getTokenTimeout()]
            返回原始 JWT              返回 Redis 剩余 TTL 秒数
```

最重要的 3 个结论是：

```text
菜单默认角色：在菜单 Service 中显式补查

角色默认角色：在 selectRolesByUserId 内部已经合并，外层看不到而已

Token：JWT 字符串负责携带签名身份；Redis TTL 负责固定有效期
```

## 一步一步看

### 问题一：`SysMenuMapper.java:34-51` 到底在查什么

目标方法位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/mapper/SysMenuMapper.java</Path>`：

```java
default Set<String> selectMenuPermsByUserId(Long userId, Long clientId) {
    if (userId == null || clientId == null) {
        return Set.of();
    }
    List<SysMenu> list = this.selectJoinList(
        SysMenu.class,
        QueryBuilder.lambdaJoin("m", SysMenu.class)
            // 后面省略
            .build()
    );
    return ...;
}
```

它要做的事情可以用一句话表达：

> 找出“当前用户在当前 Client 的正常角色”关联到的所有菜单，然后只取这些菜单的 `perms` 权限码。

#### 1. 为什么需要 4 张表

数据库没有在用户表里直接保存权限码，而是通过两张关系表连接：

```text
[sys_user 用户]
       |
       | 一个用户可以有多个角色
       v
[sys_user_role 用户角色关系]
       |
       v
[sys_role 角色]
       |
       | 一个角色可以有多个菜单
       v
[sys_role_menu 角色菜单关系]
       |
       v
[sys_menu 菜单或按钮]
       |
       v
[perms 权限码]
```

本方法已经拿到了 `userId`，不需要再连接 `sys_user` 表。因此真实参与 SQL 的 4 张表是：

```text
m   -> sys_menu
srm -> sys_role_menu
sur -> sys_user_role
sr  -> sys_role
```

缩写只是 SQL 别名，方便后面指明某个字段属于哪张表。

#### 2. 空参数为什么直接返回空集合

```java
if (userId == null || clientId == null) {
    return Set.of();
}
```

没有用户或没有当前 Client，就不能安全地查权限：

```text
userId 为空   -> 不知道查谁
clientId 为空 -> 无法保证 Client 隔离
                    |
                    v
                 返回空集
```

这里没有把缺失的 `clientId` 当成“查所有 Client”，因此不会因为上下文缺失产生跨 Client 权限并集。

#### 3. 主表和别名

```java
QueryBuilder.lambdaJoin("m", SysMenu.class)
```

意思是以 `SysMenu` 对应的 `sys_menu` 作为主表，并给它取别名 `m`：

```sql
FROM sys_menu m
```

`QueryBuilder` 和 MyBatis-Plus Join 会根据 Java 实体映射出真实表名、列名以及参数占位符。

#### 4. `distinct()` 为什么要去重

```java
.distinct()
```

假设用户有两个角色，而两个角色都授予了 `system:user:list`：

```text
角色 A -> system:user:list
角色 B -> system:user:list
```

连接查询会产生两行相同的权限码。`DISTINCT` 让数据库先合并重复行：

```sql
SELECT DISTINCT ...
```

#### 5. 为什么只 `select(SysMenu::getPerms)`

```java
.select(SysMenu::getPerms)
```

这个方法只想要权限字符串，不需要菜单名称、图标、路由组件等其他列。因此等价于：

```sql
SELECT DISTINCT m.perms
```

返回类型暂时仍是 `List<SysMenu>`，但每个 `SysMenu` 对象主要只有 `perms` 字段被填充。

#### 6. 第一张关系表：菜单连到角色

```java
.leftJoin(
    SysRoleMenu.class,
    "srm",
    SysRoleMenu::getMenuId,
    SysMenu::getMenuId
)
```

等价关系是：

```sql
LEFT JOIN sys_role_menu srm
       ON srm.menu_id = m.menu_id
```

图解：

```text
[菜单 m.menu_id]
       |
       | 等于
       v
[角色菜单关系 srm.menu_id]
```

现在能够知道“这个菜单被哪些角色拥有”。

#### 7. 第二张关系表：角色连到用户

```java
.leftJoin(
    SysUserRole.class,
    "sur",
    SysUserRole::getRoleId,
    SysRoleMenu::getRoleId
)
```

等价关系是：

```sql
LEFT JOIN sys_user_role sur
       ON sur.role_id = srm.role_id
```

图解：

```text
[角色菜单关系 srm.role_id]
       |
       | 等于
       v
[用户角色关系 sur.role_id]
       |
       v
[找到拥有该角色的用户]
```

#### 8. 第三张表：连接真正的角色记录

```java
.leftJoin(
    SysRole.class,
    "sr",
    SysRole::getRoleId,
    SysRoleMenu::getRoleId
)
```

等价于：

```sql
LEFT JOIN sys_role sr
       ON sr.role_id = srm.role_id
```

连接 `sys_role` 的主要目的不是再找一次角色 ID，而是检查角色状态和角色所属 Client。

#### 9. 限定当前用户

```java
.eq("sur", SysUserRole::getUserId, userId)
```

等价于：

```sql
WHERE sur.user_id = #{userId}
```

这一步把所有用户的角色关系缩小为当前用户的关系。

#### 10. 只接受正常状态角色

```java
.eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
```

等价于：

```sql
AND sr.status = '0'
```

停用角色关联的菜单权限不会通过这条查询进入结果。

#### 11. 为什么角色和菜单都要检查 `clientId`

```java
.eq("sr", SysRole::getClientId, clientId)
.eq("m", SysMenu::getClientId, clientId)
```

等价于：

```sql
AND sr.client_id = #{clientId}
AND m.client_id  = #{clientId}
```

这是双重 Client 隔离：

```text
[角色属于当前 Client] --必须同时满足-- [菜单属于当前 Client]
                         |
                         v
                    [权限才进入结果]
```

即使关系表里意外出现“Client A 的角色连接到 Client B 菜单”的错误数据，这条查询也不会把它当成当前 Client 的正常权限。

#### 12. `isNotNull` 和 Java 的 `isNotBlank` 为什么都有

SQL 先过滤数据库的 `NULL`：

```java
.isNotNull("m", SysMenu::getPerms)
```

等价于：

```sql
AND m.perms IS NOT NULL
```

但 `IS NOT NULL` 仍允许空字符串和只含空格的字符串，所以 Java 又做一次：

```java
StreamUtils.toList(list, SysMenu::getPerms)
```

先把：

```text
List<SysMenu>
```

转换成：

```text
List<String> perms
```

然后：

```java
StreamUtils.filter(..., StringUtils::isNotBlank)
```

去掉 `null`、`""` 和只含空白的权限码。

最后：

```java
new HashSet<>(...)
```

再次确保 Java 结果集合不重复。

#### 13. 把整个查询翻译成普通 SQL

忽略框架自动处理的参数语法后，可以把它理解成：

```sql
SELECT DISTINCT m.perms
FROM sys_menu m
LEFT JOIN sys_role_menu srm
       ON srm.menu_id = m.menu_id
LEFT JOIN sys_user_role sur
       ON sur.role_id = srm.role_id
LEFT JOIN sys_role sr
       ON sr.role_id = srm.role_id
WHERE sur.user_id = ?
  AND sr.status = '0'
  AND sr.client_id = ?
  AND m.client_id = ?
  AND m.perms IS NOT NULL;
```

三个 `?` 依次使用：

```text
当前 userId
当前 clientId
当前 clientId
```

虽然代码使用 `LEFT JOIN`，但是 `WHERE` 又要求 `sur` 和 `sr` 的字段必须满足条件，所以没有匹配关系的空行最终会被排除。就本查询结果而言，这些连接表现得接近 `INNER JOIN`。

这条查询没有检查 `m.status` 或 `m.visible`，它只检查角色状态、Client 和 `perms`。这正是当前源码的实际条件，不能从方法名额外推断不存在的过滤。

### 问题二：角色权限为什么看起来没有补默认角色

菜单 Service 的代码很直观：

```java
Set<String> perms = new HashSet<>(
    menuMapper.selectMenuPermsByUserId(userId, clientId)
);
Long defaultRoleId = resolveDefaultRoleId(clientId);
if (defaultRoleId != null) {
    perms.addAll(menuMapper.selectMenuPermsByRoleId(defaultRoleId));
}
return perms;
```

为什么需要显式补？因为上一节的 Mapper 路径必须经过 `sys_user_role`：

```text
[用户] -> [sys_user_role] -> [角色] -> [菜单权限]
```

而 Client 默认角色的设计是“不写入 `sys_user_role`”：

```text
[sys_client.default_role_id] -> [默认角色]

并不存在：
[每个用户] -> [一条默认角色 sys_user_role 记录]
```

所以第一次 Mapper 查询只能得到用户显式角色的菜单权限。Service 必须读取 `defaultRoleId`，再按角色 ID 单独补查默认角色菜单权限。

角色权限看起来没有补，是因为补默认角色的动作藏在它调用的下一层：

```text
selectRolePermissionByUserId(userId, clientId)
                |
                | 第 159 行
                v
selectRolesByUserId(userId, clientId)
                |
                +-> requireActiveClient(clientId)
                |
                +-> roleMapper.selectRolesByUserId(...)
                |      查询用户显式角色
                |
                +-> mergeDefaultRole(roles, clientId)
                       合并 Client 默认角色
                |
                v
       [已经包含默认角色的角色列表]
                |
                v
       [提取每个角色的 roleKey]
```

对应源码是：

```java
public List<SysRoleVo> selectRolesByUserId(Long userId, Long clientId) {
    requireActiveClient(clientId);
    List<SysRoleVo> roles = new ArrayList<>(
        roleMapper.selectRolesByUserId(userId, clientId)
    );
    mergeDefaultRole(roles, clientId);
    return roles;
}
```

`mergeDefaultRole` 会：

```text
[校验 Client 和登录域可用]
              |
              v
[读取 client.default_role_id]
              |
              v
[校验默认角色启用且属于当前 Client]
              |
        +-----+-----+
        |           |
        v           v
[列表已有它]   [列表没有它]
        |           |
[标记默认]     [追加并标记默认]
        |           |
        +-----+-----+
              |
              v
        [返回完整角色列表]
```

因此 `selectRolePermissionByUserId` 第 163 行拆分 `roleKey` 时，遍历的列表已经包含默认角色。

可以用下面的对照记忆：

```text
菜单权限返回 Set<String>
  -> 主查询只认 sys_user_role
  -> Service 显式按 defaultRoleId 再查权限字符串

角色权限返回 Set<String>
  -> 先调用返回 List<SysRoleVo> 的 selectRolesByUserId
  -> 这个方法内部已把默认角色对象放进列表
  -> 外层只需提取 roleKey
```

超级管理员的 `superadmin` 角色标识还会在更外层的 `SysPermissionServiceImpl.getRolePermission` 中追加；这与 Client 默认角色是两件不同的事。

### 问题三：`IAuthStrategy.java:58-71` 构建参数有什么用

目标代码位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/IAuthStrategy.java</Path>`。

`SaLoginParameter` 可以理解成“交给 Sa-Token 的本次登录说明书”：

```text
[系统已经确认账号密码正确]
              |
              v
[SaLoginParameter 登录说明书]
              |
              +-> 用什么设备类型登记
              +-> Token 最长活多久
              +-> 多久不操作算不活跃
              +-> JWT 里放哪些附加身份信息
              |
              v
[Sa-Token 创建会话和 Token]
```

#### 1. `new SaLoginParameter()`

```java
SaLoginParameter model = new SaLoginParameter();
```

构造时先从 Sa-Token 全局配置复制默认值，例如是否允许并发登录、是否共享 Token、最大登录数量和 Cookie 行为。

本项目随后用当前 `sys_client` 的配置覆盖其中一部分，使不同 Client 可以拥有不同登录策略。

#### 2. `deviceType`

```java
model.setDeviceType(client.getDeviceType());
```

它告诉 Sa-Token 本次登录属于哪种终端，例如 `pc`、`android`、`ios`。

Sa-Token 会把它记录到账号会话的终端信息中：

```text
[同一个 loginId]
      |
      +-> PC 终端：token A
      +-> Android 终端：token B
      +-> iOS 终端：token C
```

后续可以按设备类型查看或清理会话。

#### 3. 固定超时 `timeout`

```java
model.setTimeout(client.getTimeout());
```

单位是秒，表示这个 Token 从创建开始最多能存在多久。

当前初始化数据的常见值是：

```text
604800 秒 = 7 天
```

即使用户一直操作，超过固定有效期后也必须重新登录。

#### 4. 活跃超时 `activeTimeout`

```java
model.setActiveTimeout(client.getActiveTimeout());
```

单位也是秒，表示用户连续多久没有有效操作后，Token 会因不活跃而失效或冻结。

当前初始化数据的常见值是：

```text
1800 秒 = 30 分钟
```

两种时间不是一回事：

```text
固定 timeout：
登录 ------------------------------------------> 最晚 7 天到期

活跃 activeTimeout：
最后一次操作 ------------> 30 分钟没有操作就失效
```

本项目配置了 `dynamic-active-timeout: true`，所以不同 Client 传入的活跃超时可以按 Token 单独保存。

#### 5. Client 的 4 个扩展字段

```java
model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
model.setExtra(LoginHelper.CLIENT_PK_KEY, client.getId());
model.setExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY, client.getAccessPath());
model.setExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY, client.getIpWhitelist());
```

它们会进入 JWT 的签名载荷：

```text
clientid
  -> OAuth 字符串 Client 标识
  -> 后续请求必须与 header 或 param 的 clientid 一致

clientPk
  -> sys_client.id 的 Long 主键
  -> 与字符串 clientid 语义不同

clientAccessPath
  -> 当前 Client 允许访问的接口路径规则
  -> SecurityConfig 在每次受保护请求中检查

clientIpWhitelist
  -> 当前 Client 的来源 IP 白名单
  -> SecurityConfig 在每次受保护请求中检查
```

这些扩展字段是已签名但不是加密的数据。JWT payload 能被拿到 Token 的人解码查看，因此不能把 Client Secret 或密码放进 `setExtra`。

#### 6. `customizer` 是什么

```java
if (ObjectUtil.isNotNull(customizer)) {
    customizer.accept(model);
}
```

这是一个预留的定制入口：调用者可以在公共 Client 参数建好后继续修改同一个 `model`。

```text
[建立公共参数]
       |
       v
[执行可选 customizer]
       |
       v
[返回最终 model]
```

当前 5 种认证策略都使用无 customizer 的重载，所以这个入口目前没有生产调用点。

### 问题四：为什么 `LoginHelper.login` 又调用 `setExtra`

`PasswordAuthStrategy` 第 76 行已经得到一个 `model`：

```java
SaLoginParameter model = IAuthStrategy.buildLoginParameter(client);
```

然后 `LoginHelper.login` 又执行：

```java
model.setExtra(USER_KEY, loginUser.getUserId())
    .setExtra(USER_NAME_KEY, loginUser.getUsername())
    .setExtra(DEPT_KEY, loginUser.getDeptId())
    .setExtra(DEPT_NAME_KEY, loginUser.getDeptName())
    .setExtra(DEPT_CATEGORY_KEY, loginUser.getDeptCategory())
    .setExtra(USER_TYPE_KEY, loginUser.getUserType());
```

这里没有新建第二个 `model`。`setExtra` 的实现是向同一个 `LinkedHashMap` 放入键值，然后返回 `this`，所以才能一直链式调用：

```text
同一个 SaLoginParameter model
  |
  +-- 先放 Client 来源字段
  |     clientid、clientPk、路径、IP 白名单
  |
  +-- 再放认证用户来源字段
        userId、username、deptId、部门、userType
```

#### 为什么要分两层装配

第一层只依赖 Client：

```text
IAuthStrategy.buildLoginParameter(client)
  -> 所有认证方式共用的 Client 策略
```

第二层依赖已经认证成功的 `LoginUser`：

```text
LoginHelper.login(loginUser, model)
  -> 所有认证方式共用的用户身份字段
  -> 真正调用 StpUtil.login
  -> 保存完整 LoginUser 到 Token Session
```

密码、短信、邮箱、社交和小程序登录都走这两个公共入口：

```text
[PasswordAuthStrategy] --+
[SmsAuthStrategy] -------+
[EmailAuthStrategy] -----+-> [buildLoginParameter] -> [LoginHelper.login]
[SocialAuthStrategy] ----+
[XcxAuthStrategy] -------+
```

如果把所有用户字段都写在 `PasswordAuthStrategy`，其他 4 种登录方式也要复制一遍，而且很容易漏字段或使用不同 key。

技术上当然可以在策略里手工把所有字段一次性放完，但当前分层更明确：

```text
认证策略
  -> 负责验证密码、短信、邮箱或第三方身份

IAuthStrategy 公共构造器
  -> 负责 Client 登录策略

LoginHelper
  -> 负责把 LoginUser 变成统一的 Sa-Token 登录上下文
```

#### 这些用户 Extra 后面怎样使用

`LoginHelper.getUserId()`、`getUsername()`、`getDeptId()` 等方法会调用：

```java
StpUtil.getExtra(key)
```

因为本项目使用 JWT Simple 模式，这些值会从 JWT 的签名 payload 读取。

登录成功事件也会直接从同一个 `SaLoginParameter` 读取用户名、Client、部门和用户 ID，用于写在线用户记录、登录日志和最近登录信息。

#### Extra 和完整 `LoginUser` 有什么区别

第 58-65 行把少量常用字段放进 JWT Extra；第 66 行又把完整对象放进 Token Session：

```java
StpUtil.getTokenSession().set("loginUser", loginUser);
```

它们的职责不同：

```text
[JWT Extra]
  -> 少量、常用、可签名验证的身份字段
  -> userId、Client、部门、登录域、路径规则等
  -> 可通过 StpUtil.getExtra 快速读取

[Token Session 中的 LoginUser]
  -> 完整登录快照
  -> 菜单权限、角色权限、角色对象、岗位、数据权限映射等
  -> 通过 LoginHelper.getLoginUser 读取
```

不把完整角色和权限对象全部塞进 JWT，可以避免 Token 体积过大；不把 Client Secret 放进任何一处，可以避免敏感值泄漏。

### 问题五：`getTokenValue()` 和 `getTokenTimeout()` 的值怎样产生

目标代码位于 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/java/org/dromara/web/service/impl/PasswordAuthStrategy.java</Path>`：

```java
loginVo.setAccessToken(StpUtil.getTokenValue());
loginVo.setExpireIn(StpUtil.getTokenTimeout());
```

这两行不是现场第一次创建 Token。真正的创建已经发生在上一行：

```java
LoginHelper.login(loginUser, model);
```

#### 1. 先生成 Sa-Token 使用的 loginId

`LoginHelper.login` 传入：

```java
loginUser.getLoginId()
```

`LoginUser.getLoginId()` 返回：

```text
userType + ":" + userId
```

例如：

```text
pc:10001
app:10001
```

同一个数据库用户在不同登录域下拥有不同 Sa-Token loginId。

#### 2. `StpUtil.login` 创建登录会话

Sa-Token 的主流程是：

```text
[检查 loginId 和 model]
          |
          v
[分配一个可用 Token]
          |
          v
[创建或更新账号 Session]
          |
          v
[记录设备终端信息]
          |
          v
[保存 Token -> loginId 映射和固定 TTL]
          |
          v
[保存最后活跃时间和 activeTimeout]
          |
          v
[发布登录成功事件]
          |
          v
[把新 Token 写入当前 HTTP 请求上下文]
```

#### 3. 本项目的 TokenValue 是怎样生成的

项目在 `<Path>ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-satoken/src/main/java/org/dromara/common/satoken/config/SaTokenConfig.java</Path>` 注册了：

```java
new StpLogicJwtForSimple()
```

因此 Sa-Token 1.45.0 会生成 JWT Simple Token，而不是普通随机 UUID。

JWT 的内容结构可以理解成：

```text
[Header]
   算法和类型
       .
[Payload]
   loginType
   loginId
   随机字符串 rnStr
   Client Extra
   用户 Extra
       .
[Signature]
   使用配置的 JWT Secret 做 HS256 签名
```

最终 Token 是：

```text
base64url(header).base64url(payload).signature
```

随机 `rnStr` 保证同一个用户连续登录时仍能生成不同 Token。JWT 签名用于发现内容是否被篡改，不代表 payload 被加密。

Sa-Token 还会检查新 Token 是否已有对应登录映射；如果碰撞，就按最大尝试次数重新生成。本项目配置允许并发登录、禁止共享 Token，而 JWT Simple 逻辑本身也强制每次登录创建新 Token。

#### 4. 固定有效期保存在哪里

当前 JWT Simple 的 `createTokenValue` 调用的是不带 timeout 的简单 JWT 生成方法，因此这个 JWT payload 本身没有写入固定到期字段。

固定有效期由服务端保存：

```text
[原始 JWT Token]
        |
        v
[Redis key: Authorization:login:token:<Token>]
        |
        +-> value = loginId
        |
        +-> TTL = client.timeout 秒
```

项目自定义的 `PlusSaTokenDao` 通过 Redis 写入这条映射。Token 即使签名正确，只要服务端映射不存在或已过期，也不能继续作为有效登录会话使用。

#### 5. `getTokenValue()` 为什么立刻就能拿到新 Token

`StpUtil.login` 创建 Token 后，会把它写进当前 HTTP 请求的 `SaStorage`：

```text
[新生成的原始 JWT]
         |
         +-> 保存一份原始值
         |
         +-> 按配置保存一份 "Bearer <JWT>"
```

紧接着调用 `StpUtil.getTokenValue()` 时，读取顺序的第一项就是当前请求 Storage，因此不用等前端把 Token 再发回来。

项目配置了 `token-prefix: Bearer`。`getTokenValue()` 读取带前缀值后会裁掉 `Bearer `，所以返回给 `access_token` 的是原始 JWT，不带前缀。

前端保存原始 JWT，后续请求时再拼成：

```text
Authorization: Bearer <access_token>
```

#### 6. `getTokenTimeout()` 返回什么

调用链是：

```text
StpUtil.getTokenTimeout()
        |
        v
StpLogic.getTokenTimeout()
        |
        +-> 先从当前请求取得原始 Token
        |
        v
StpLogic.getTokenTimeout(token)
        |
        +-> 拼出 Token -> loginId 的 Redis key
        |
        v
PlusSaTokenDao.getTimeout(key)
        |
        +-> RedisUtils.getTimeToLive(key)
        |
        v
[把 Redis 毫秒 TTL 换算为秒]
```

因此 `expire_in` 是“读取这一刻还剩多少秒”，不是简单把 `client.getTimeout()` 原样复制到响应。

登录后立刻读取时，两者通常非常接近：

```text
client.timeout = 604800 秒

Redis 写入 TTL -> 604800 秒
立即查询剩余 TTL -> 大约 604800 秒
稍后再查询       -> 604799、604798、...
```

项目的 DAO 在把 Redis 毫秒 TTL 转成秒时会加 1，补偿毫秒到秒的精度损失，所以边界上可能看到取整后的数值。

Sa-Token 的特殊返回值是：

```text
-1 -> 永久有效
-2 -> 没有这条 Token 值或已失效
```

#### 7. `expire_in` 为什么不等于“保证还能使用这么久”

`getTokenTimeout()` 返回的是固定有效期剩余时间，而当前 Client 还有活跃超时：

```text
固定剩余时间：还有 6 天

但如果：连续 30 分钟没有有效操作

结果：可能先因 activeTimeout 失效
```

所以判断 Token 是否可用要同时考虑：

```text
[固定 TTL 尚未到期]
        并且
[没有超过活跃超时]
        并且
[没有被踢出、顶下线或注销]
        并且
[Client、路径、IP 等请求规则通过]
```

### 把 5 个问题串成一次完整登录

```text
1. 用户密码验证成功
          |
          v
2. buildLoginUser 查询当前 Client 的权限
          |
          +-> 显式角色菜单权限
          +-> Client 默认角色菜单权限
          +-> 显式角色 + 默认角色的 roleKey
          |
          v
3. buildLoginParameter 读取 Client 登录策略
          |
          +-> deviceType
          +-> timeout
          +-> activeTimeout
          +-> Client Extra
          |
          v
4. LoginHelper 追加用户 Extra
          |
          v
5. StpUtil.login 生成签名 JWT
          |
          +-> Redis 保存 Token 映射和固定 TTL
          +-> 保存活跃时间
          +-> 当前请求 Storage 记住新 Token
          |
          v
6. getTokenValue 读取原始 JWT
          |
          v
7. getTokenTimeout 读取 Redis 剩余 TTL 秒数
          |
          v
8. LoginVo 返回 access_token + expire_in + client_id
```

## 术语小词典

- 关系表：只保存“谁和谁有关”的表，例如 `sys_user_role` 保存用户 ID 与角色 ID。
- SQL 别名：给表起的短名字，例如 `m` 代表 `sys_menu`，方便写连接条件。
- 连接查询（Join）：根据相同 ID 把多张表的一行拼起来。
- 主表：连接查询的起点；这里是 `sys_menu m`。
- 去重（`DISTINCT` / `Set`）：多个角色给出相同权限时，只保留一份权限码。
- 权限码（`perms`）：用于判断接口或按钮权限的字符串，例如 `system:user:list`。
- 角色标识（`roleKey`）：用于判断角色身份的字符串，例如 `manager`。
- 默认角色：配置在 Client 上、登录时自动拥有但不写入用户角色关系表的角色。
- 登录参数（`SaLoginParameter`）：交给 Sa-Token 的本次登录策略和附加数据集合。
- 固定超时（`timeout`）：Token 从创建开始允许存在的最长时间。
- 活跃超时（`activeTimeout`）：连续多久没有有效操作后，Token 会因不活跃而失效。
- Extra：附加在 JWT payload 中的少量键值数据，只签名、不加密。
- JWT：由 Header、Payload 和 Signature 三段组成的签名 Token。
- Payload：JWT 中携带身份和 Extra 的数据部分，可以解码查看。
- Signature：使用 Secret 对 JWT 内容计算的签名，用于发现篡改。
- Token Session：服务端按 Token 保存完整 `LoginUser` 登录快照的会话。
- Redis TTL：Redis 中某个 key 距离自动过期还剩多少时间。
- loginId：Sa-Token 识别账号的值；本项目由 `userType:userId` 组成。
- 当前请求 Storage：Sa-Token 在一次 HTTP 请求期间保存刚创建 Token 等临时上下文的地方。
- `expire_in`：登录响应中固定有效期的剩余秒数，不是活跃超时剩余值。

## 你现在能复述什么

1. `SysMenuMapper` 从菜单出发，连接角色菜单、用户角色和角色表，以 `userId` 找到当前用户，以角色和菜单的两个 `clientId` 条件保证 Client 隔离，最后只返回非空、去重的 `perms`。
2. 默认角色没有写入 `sys_user_role`，所以菜单权限必须显式按默认角色 ID 补查；角色权限通过 `selectRolesByUserId -> mergeDefaultRole` 已经间接完成同一件事。
3. `IAuthStrategy.buildLoginParameter` 负责 Client 来源的登录策略，`LoginHelper.login` 负责认证用户来源的身份数据；两者修改的是同一个 `SaLoginParameter`。
4. `setExtra` 在本项目的 JWT Simple 模式下会进入签名 payload；完整 `LoginUser` 则单独放进 Token Session，承担角色和权限快照。
5. `StpUtil.login` 先生成签名 JWT，再把 Token 到 loginId 的映射按 `client.timeout` 写入 Redis；`getTokenValue()` 从当前请求上下文取得原始 JWT，`getTokenTimeout()` 查询 Redis 当前剩余 TTL 秒数。
6. `expire_in` 只表示固定 TTL，Token 仍可能因为活跃超时、注销、被踢下线或 Client 请求规则不通过而更早失效。
