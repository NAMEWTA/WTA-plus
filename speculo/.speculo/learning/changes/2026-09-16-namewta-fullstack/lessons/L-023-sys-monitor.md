---
lesson_id: L-023
objective_ids: [OBJ-23]
claimed_cells:
  - A:CacheController.getInfo
  - A:SysLoginInfoController.list, export, remove, clean, unlock
  - A:SysOperlogController.list, export, remove, clean
  - A:SysUserOnlineController.list, forceLogout, getInfo, remove
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: windows-on-disk
    minutes: 11
  - segment: write-paths-and-stores
    minutes: 8
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L023-01, S-L023-02, S-L023-03, S-L023-04, S-L023-05, S-L023-06, S-L023-07, S-L023-08]
---

# Lesson 023：值班室四扇窗——Cache / 登录日志 / 操作日志 / 在线用户

## 学完你能做什么

打开 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/monitor/`，目录里**正好四份** Java。你能**口述这四扇监控窗**：每扇门牌、每个公开方法、读的是 Redis 还是 MySQL、写点在不在本课 Controller 里。口试名单就是矩阵 (a) 这四行，不是通知监控、不是 SnailJob 外链、不是前端 `createMonitorService`：

1. **`A:CacheController.getInfo`**：`GET /monitor/cache`。借一条 Redis 连接，读 `INFO` + `INFO commandstats` + `DBSIZE`，拼成饼图数据就还连接。磁盘上**只有这一扇 HTTP 窗**。
2. **`A:SysLoginInfoController.list, export, remove, clean, unlock`**：门牌 `/monitor/loginInfo`。五扇窗全在。列表/导出读表 `sys_login_info`；删行/清空改这张表；`unlock` **不改这张表**，只擦 Redis `pwd_err_cnt:` + 用户名。
3. **`A:SysOperlogController.list, export, remove, clean`**：门牌 `/monitor/operlog`。四扇窗全在。表是 `sys_oper_log`。服务里还有 `selectOperLogById` / `insertOperlog`，**没有**对应 HTTP。
4. **`A:SysUserOnlineController.list, forceLogout, getInfo, remove`**：门牌 `/monitor/online`。四扇窗全在。没有 Mapper、没有在线用户表。名单来自 Redis `online_tokens:` + Sa-Token 活性；踢人走 `StpUtil.kickoutByTokenValue`。

`wta-system` 在登记表是 **classic**。登录/操作两份是 `Controller → ISys*Service → ServiceImpl → Mapper`。缓存窗**没有** Service。在线窗把扫描、过滤、踢人**写在 Controller 方法体里**，不要口述成 layered UseCase，也不要假装有一张 `sys_user_online`。

本课不宣称你会拆 `createMonitorService` / `createMonitorWebDomain`（L-024）、`NotificationMonitorController`（notify 模块）、SnailJob/Nacos 外链、或把 `PasswordAuthStrategy` 再讲一遍（L-013 已认生产行：写 `online_tokens:`、发 `LoginInfoEvent`、清错次）。今天只认：**值班室怎么看、怎么撕、怎么踢。**

## 先把宏观地图放在桌上

L-013 已经把「进门成功之后贴三张贴纸」钉死：Redis 名牌 `online_tokens:` + token、异步登录日志、用户表最近登录 IP。L-015 的 `GET /system/user/unlock/{userId}` 是人事柜台那把扫帚，擦的也是 `pwd_err_cnt:`。本课站在 **`controller/monitor` 这一头**：同样几只柜子，换成监控权限去看、导出、清空、按用户名解锁、按 token 踢人。

2026-09-16 工作树里，四份类**四块门牌**，不要合成一个 `/monitor` 超级 Controller：

```text
浏览器 / 管理端监控页          （页面与工厂是 L-024，本课只认 HTTP）
        │
        ├─ GET  /monitor/cache              CacheController.getInfo
        │        Redis INFO 仪表，不扫业务键
        │
        ├─ /monitor/loginInfo/*             SysLoginInfoController
        │        GET  /list
        │        POST /export
        │        DELETE /{infoIds}
        │        DELETE /clean
        │        GET  /unlock/{userName}    ← 擦错次，不是删日志行
        │
        ├─ /monitor/operlog/*               SysOperlogController
        │        GET  /list
        │        POST /export
        │        DELETE /{operIds}
        │        DELETE /clean
        │
        └─ /monitor/online/*                SysUserOnlineController
                 GET  /list                 全站有效会话
                 GET  /                     当前账号自己的设备
                 DELETE /{tokenId}          管理员强退任意 token
                 DELETE /myself/{tokenId}   只踢自己的 token
```

| 类 | 磁盘路径 | 公开 HTTP 方法 | 继承 `BaseController`？ | 持久化 |
| --- | --- | --- | --- | --- |
| `CacheController` | `.../controller/monitor/CacheController.java` | **1**：`getInfo` | **否** | 无表；`RedissonConnectionFactory` 借连接 |
| `SysLoginInfoController` | 同目录 | **5**：list / export / remove / clean / unlock | 是 | 表 `sys_login_info`；unlock 只碰 Redis |
| `SysOperlogController` | 同目录 | **4**：list / export / remove / clean | 是 | 表 `sys_oper_log` |
| `SysUserOnlineController` | 同目录 | **4**：list / forceLogout / getInfo / remove | 是 | 无表；读 `online_tokens:`，踢人走 Sa-Token |

矩阵 (a) 这四行的方法名单**与磁盘一致**，没有 L-015 那种漏 `add`/`edit`。漏的是菜单种子里的 **`monitor:online:batchLogout`**：基座 DML 有这颗按钮权限，Controller **没有**批量强退方法。口试按磁盘，不要把菜单行背成「有 batch API」。

**类比：** 学校传达室挂四扇小窗。第一扇是**锅炉房玻璃**（Cache）：隔着玻璃看压力表和「今天烧了几次火」，不给你打开储物柜翻钥匙。第二扇是**访客登记簿**（登录日志）：谁按过门铃、成功还是被拒，本子在档案柜（MySQL）。第三扇是**盖章工作簿**（操作日志）：谁在哪张表格上盖了 `@Log` 章。第四扇是**大厅现挂的名牌**（在线）：谁的牌子还在挂钩上；摘牌子不等于撕掉访客簿。

**类比失效处：**

1. 四扇窗不是四栋楼。都在 `wta-system` classic 房间的 `controller/monitor` 包。通知监控在 `wta-notify`，别把 `NotificationMonitorController` 塞进本目录。
2. 「锅炉房玻璃」不是 RuoYi 老仓库那种「按 cacheName 列键、清某个业务缓存」的工具箱。本树 `CacheController` **只有** `getInfo`。
3. 「访客簿」不是锁。锁是另一把 Redis 钥匙 `pwd_err_cnt:`。清空登录日志**不会**给人开锁。
4. 「工作簿」不是登录。登录失败也会写访客簿，但通常**没有** `@Log`，进不了操作日志。
5. 「名牌」不是表。`SysUserOnline` **没有** `@TableName`。`UserOnlineDTO` 住在 `wta-api`。
6. 人事柜台 `GET /system/user/unlock/{userId}` 和本课 `GET /monitor/loginInfo/unlock/{userName}` 是**两扇窗、同一把错次扫帚**。路径、权限、入参都不同。
7. 前端 `createMonitorService` 还有 SnailJob / Nacos 外链和附件下载意图，那些**不是**这四份 Controller。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **四份源码，四块牌子。** 找映射先打开 `controller/monitor` 这四份，不要在 `controller/system` 里找 Cache。
2. **监控窗大多是读和撕，不是生产行。** 登录行、名牌、操作行的**写入**分别在 L-013 监听链和 `LogAspect`。本课 Controller 不 `insert`。
3. **Cache 只看仪表。** `GET /monitor/cache` 调 Redis `INFO`，把 `cmdstat_*` 削成 `{name, value=calls}` 给饼图。不 `KEYS *`，不 `FLUSHDB`，不按 `CacheNames` 列抽屉。
4. **登录日志是 MySQL 本子。** 表名是 `sys_login_info`（下划线、单数 info），不是老 RuoYi 的 `sys_logininfor`。HTTP 前缀是驼峰 `/monitor/loginInfo`，权限串是小写 `monitor:logininfo:*`。
5. **操作日志是另一本。** 类名 `SysOperlogController`（log 的 L 小写），服务/实体是 `SysOperLog`（L 大写）。门牌 `/monitor/operlog`。
6. **清空是整本撕掉。** `clean` 走 `mapper.lambda().delete()`，没有 WHERE。两份 `clean` 都标了 `@Lock4j`，避免两个人同时撕。
7. **解锁不撕本子。** `unlock` 拼 `pwd_err_cnt:` + 路径上的用户名；键在才 `deleteObject`。没有键也 `R.ok()`。HTTP 是 **GET**。
8. **在线名单是挂钩扫描。** `RedisUtils.keys("online_tokens:*")`（底层是 scan，不是 Redis `KEYS` 命令的字面），再问 Sa-Token「这枚 token 还活着吗」。
9. **活性判断是 `< -1` 就跳过。** 与 `ClientSessionService` 的 `>= -1` 算活着是同一把尺。`-1` 表示没有活动超时（仍算活）；更负才当死票。
10. **管理员踢人 ≠ 自己踢自己。** `DELETE /monitor/online/{tokenId}` 要 `monitor:online:forceLogout`，任意 token。`DELETE /monitor/online/myself/{tokenId}` **没有**权限注解，但只在「当前 loginId 的 token 列表」里找到才踢。
11. **自己的设备列表也没有权限注解。** `GET /monitor/online` 只要求已登录。不要把它和 `GET /monitor/online/list`（要 `monitor:online:list`）说成同一扇。
12. **盖章会写新行。** 你在值班室删操作日志，方法上有 `@Log`，`LogAspect` 会再往 `sys_oper_log` 插一行「我刚删了日志」。撕本子本身也留字。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以工作树为准） |
| --- | --- | --- |
| 缓存监控控制器 | `CacheController` | `@RestController`，`@RequestMapping("/monitor/cache")`，**不**继承 `BaseController`。公开 HTTP 方法只有 `getInfo` |
| 缓存列表信息 | `CacheListInfoVo` | 该类内部的 **public record**：`Properties info`、`Long dbSize`、`List<Map<String,String>> commandStats`。不是独立文件 |
| Redis 信息命令 | Redis `INFO` / `INFO commandstats` / `DBSIZE` | `getInfo` 向当前连接要的三样东西。`commandstats` 里每个 `cmdstat_get` 削掉前缀，取出 `calls=` 与 `,usec` 之间的数字当饼图 value |
| 登录日志控制器 | `SysLoginInfoController` | `@RequestMapping("/monitor/loginInfo")`。五个公开方法与矩阵 (a) 该行同名 |
| 系统访问记录表 | `sys_login_info` | 基座 DDL 与实体 `@TableName`。主键 `info_id`。状态列是 `char(1)`：`Constants.SUCCESS="0"` / `FAIL="1"` |
| 登录事件 | `LoginInfoEvent` | `wta-common-log` 里的 Spring 事件。`SysLoginService.recordLoginInfo` 发布；`SysLoginInfoServiceImpl.recordLoginInfo` 标 `@Async @EventListener` 后落库 |
| 事件状态字 | `Constants.LOGIN_SUCCESS="Success"` 等 | 事件上的 status 是英文词：`Success` / `Logout` / `Register` / `Error`。落库时前三者写成 `"0"`，`LOGIN_FAIL` 写成 `"1"` |
| 账户解锁（监控） | `unlock` | `GET /monitor/loginInfo/unlock/{userName}`，权限 `monitor:logininfo:unlock`。只删 `pwd_err_cnt:{userName}` |
| 操作日志控制器 | `SysOperlogController` | 类名 log 的 L **小写**。`@RequestMapping("/monitor/operlog")`。四个公开方法与矩阵 (a) 该行同名 |
| 操作日志表 | `sys_oper_log` | 基座 DDL。主键 `oper_id`。`business_type` / `status` / `oper_time` 有索引 |
| 操作日志事件 | `OperLogEvent` | 由 `LogAspect` 在 `@Log` 方法返回或抛错后 `publishEvent`。`SysOperLogServiceImpl.recordOper` 异步落库并补 `operLocation` |
| 业务类型 | `BusinessType` | 枚举序数写入 `business_type`。本课会出现 `EXPORT`、`DELETE`、`CLEAN`、`FORCE`、`OTHER` |
| 在线用户控制器 | `SysUserOnlineController` | `@RequestMapping("/monitor/online")`。无注入的在线 Service。四个公开方法与矩阵 (a) 该行同名 |
| 在线名牌键 | `CacheNames.ONLINE_TOKEN_KEY` | 字面量 `"online_tokens:"`。登录成功写入 `online_tokens:` + tokenValue；登出/踢出/顶下由 `UserActionListener` 删 |
| 在线会话 DTO | `UserOnlineDTO` | `wta-api` 的可序列化对象，塞进 Redis。Controller 再 `BeanUtil.copyToList(..., SysUserOnline.class)` |
| 当前在线会话 | `SysUserOnline` | **不是** MyBatis 实体。字段与 DTO 对齐：tokenId / userName / ipaddr / loginTime(Long) 等 |
| 管理员强退 | `forceLogout` | `DELETE /monitor/online/{tokenId}`，权限 `monitor:online:forceLogout`，`@Log FORCE`，`@RepeatSubmit`。`StpUtil.kickoutByTokenValue`；已失效吞 `NotLoginException` 仍 `R.ok()` |
| 自踢设备 | `remove` | Java 方法名 `remove`。`DELETE /monitor/online/myself/{tokenId}`。无 `@SaCheckPermission`。仅当 token 属于 `StpUtil.getTokenValueListByLoginId(当前 loginId)` 才踢 |
| 自己的设备列表 | `getInfo`（在线） | `GET /monitor/online`，无权限注解。不要和 `CacheController.getInfo` 或用户模块两扇 `getInfo` 混名 |
| 清空锁 | `@Lock4j` | 标在两份 `clean` 上。默认锁住方法，避免并发整表删除互踩 |
| 防重复提交 | `@RepeatSubmit` | 默认间隔 5000ms。标在 logininfo `unlock`、online `forceLogout` / `remove` |
| 分页外壳 | `PageResult` | 登录/操作列表走 `PageQuery` + `selectVoPage`。在线列表调用 `PageResult.build(list)`：`total = rows.size()`，**没有**服务端页码裁剪 |

### 机制/因果链

#### 1. `CacheController.getInfo`：借表、读仪表、还表

文件：`CacheController.java`。注入 `RedissonConnectionFactory`。方法签名 `throws Exception`。

```text
GET /monitor/cache
  @SaCheckPermission("monitor:cache:list")
  无 @Log
```

顺序：

1. `connectionFactory.getConnection()`。
2. `connection.commands().info("commandstats")`。键名以 `cmdstat_` 开头；`StringUtils.removeStart` 去掉此外壳当 `name`；`substringBetween(property, "calls=", ",usec")` 当 `value`。`commandStats == null` 则饼图列表为空。
3. 同时取 `info()`（整份 Redis INFO）和 `dbSize()`。
4. `return R.ok(new CacheListInfoVo(info, dbSize, pieList))`。
5. `finally` 里 `RedisConnectionUtils.releaseConnection(connection, connectionFactory)`。注释写明：归还连接给连接池。中途抛错也还。

它**不**走 `RedisUtils`，**不**碰 `CacheNames` 那些业务前缀，**不**提供清缓存 HTTP。权限字符串是 `monitor:cache:list`，菜单种子里缓存页也挂这同一颗 `perms`。

#### 2. 登录日志：事件进表，Controller 只翻/撕/导出/开锁

**生产行（本课认方向，细节已在 L-013）：**

`SysLoginService.recordLoginInfo` 组 `LoginInfoEvent`（用户名、状态词、文案、IP、UA、请求头 `clientid`）→ `publishEvent`。成功登录还经过 `UserLoginSuccessListener` 再调一次 `recordLoginInfo(LOGIN_SUCCESS)`。注册失败/验证码失败走 `SysRegisterService` 的私有同名方法，发的也是这只事件。

`SysLoginInfoServiceImpl.recordLoginInfo`：`@Async @EventListener`。解析 UA 得到 os/browser，`AddressUtils.getRealAddressByIP`，按 `clientId` 查 `ISysClientService.queryByClientId` 填 `clientKey`/`deviceType`，把事件状态词映射成 `"0"`/`"1"`，`insertLoginInfo` 里补 `loginTime = now()` 后 `loginInfoMapper.insert`。

**本课五扇窗：**

| Java | HTTP | 权限 | `@Log` | 做什么 |
| --- | --- | --- | --- | --- |
| `list` | `GET /monitor/loginInfo/list` | `monitor:logininfo:list` | 无 | `selectPageLoginInfoList`。默认 `orderByDesc(infoId)`。条件：IP like、status eq、userName like、`params.beginTime/endTime` between `loginTime` |
| `export` | `POST /monitor/loginInfo/export` | `monitor:logininfo:export` | title=登录日志，EXPORT | **不分页** `selectLoginInfoList`，`ExcelBuilder.of(list, SysLoginInfoVo.class).sheetName("登录日志").toResponse`。返回 `void` |
| `remove` | `DELETE /monitor/loginInfo/{infoIds}` | `monitor:logininfo:remove` | DELETE | `deleteLoginInfoByIds` → `deleteByIds`。`toAjax` |
| `clean` | `DELETE /monitor/loginInfo/clean` | **同样** `monitor:logininfo:remove` | CLEAN + `@Lock4j` | `cleanLoginInfo()` → `loginInfoMapper.lambda().delete()`。然后 `R.ok()`（不看删除行数） |
| `unlock` | `GET /monitor/loginInfo/unlock/{userName}` | `monitor:logininfo:unlock` | title=账户解锁，OTHER + `@RepeatSubmit` | 拼 `PWD_ERR_CNT_KEY + userName`；`hasKey` 才删。**零 SQL** |

对照人事柜台：`SysUserController.unlock` 是 `GET /system/user/unlock/{userId}`，权限 `system:user:edit`，先 `selectUserById`，用户不存在 `R.fail("用户不存在")`，再用**查出来的 userName** 拼错次键。监控窗直接信路径上的字符串，不查 `sys_user`。两扇都不会去删 `auth:temporary-password:user:`。

菜单种子另有 `monitor:logininfo:query`。Controller **没有**方法挂这颗权限。列表用的是 `:list`。

#### 3. 操作日志：`@Log` 盖章进表，Controller 只翻/撕/导出

**生产行：**

`LogAspect.doAround` 切 `@annotation(controllerLog)`。`StopWatch` 包一圈 `proceed()`。成功把返回值、失败把异常交给 `handleLog`，异常仍再抛出。`handleLog` 组 `OperLogEvent`：IP、截断后的 URI（255）、请求头或会话里的 `clientKey`、登录用户的姓名/部门/设备/UA、方法全名 `类.方法()`、HTTP 动词、`@Log` 的 title / businessType.ordinal / operatorType、可选请求体/响应体（内容上限约 3800）、耗时毫秒、成功或失败序数。然后 `publishEvent`。切面自己 catch 组装失败，只打 error，**不**让业务方法因记日志失败而改返回值。

`SysOperLogServiceImpl.recordOper`：`@Async @EventListener`，事件转 `SysOperLogBo`，补 `operLocation`，`insertOperlog` 写 `operTime = now()`。

**本课四扇窗：**

| Java | HTTP | 权限 | `@Log` | 做什么 |
| --- | --- | --- | --- | --- |
| `list` | `GET /monitor/operlog/list` | `monitor:operlog:list` | 无 | `selectPageOperLogList`。默认 `orderByDesc(operId)`。条件含 IP/title like、单个或数组 `businessType`、status、operName、userId、deptId、clientKey、deviceType、browser、os、时间区间 |
| `export` | `POST /monitor/operlog/export` | `monitor:operlog:export` | EXPORT | 不分页列表 + Excel 表名「操作日志」。`void` |
| `remove` | `DELETE /monitor/operlog/{operIds}` | `monitor:operlog:remove` | DELETE | `deleteOperLogByIds` |
| `clean` | `DELETE /monitor/operlog/clean` | **同样** `:remove` | CLEAN + `@Lock4j` | `operLogMapper.lambda().delete()` |

`ISysOperLogService.selectOperLogById` 在接口和实现里都有，**四扇窗都没映射它**。不要把「能按 id 查详情」说成 HTTP 合同。

因果闭环：`remove` / `clean` / `export` 自己带着 `@Log`。你清空操作日志成功后，异步监听仍可能再插入**一行新的 CLEAN 记录**。口试要能说「撕本子会留下撕本子的字」，不要说「clean 之后表一定是空的、永远空」。

#### 4. 在线用户：名牌扫描 + 踢人

**生产行（L-013 已认）：** `UserActionListener.doLogin` 发 `UserLoginSuccessEvent`。`UserLoginSuccessListener` 把 `UserOnlineDTO` 写入 `online_tokens:` + token；TTL 跟 Client `timeout`，`-1` 则不设 TTL。`doLogout` / `doKickout` / `doReplaced` 删同一把键。

**`list`（全站）：**

1. `RedisUtils.keys(ONLINE_TOKEN_KEY + "*")`。实现是 Redisson `getKeysStream`，chunkSize 1000，不是教学里常骂的阻塞 `KEYS`。
2. 每个键用 `substringAfterLast(key, ":")` 取出 token。键格式是 `online_tokens:` + tokenValue；若 token 自己含 `:`，这刀会切错，后面按错 token 去问活性、去 `getCacheObject`。口试记这个解析假设。
3. `StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) < -1` → 当死票，supplier 返回 null。
4. 活票则 `RedisUtils.getCacheObject(ONLINE_TOKEN_KEY + token)` 读 DTO。
5. `ThreadUtils.virtualSubmitAll` 并发取回；`removeAll(singleton(null))` 丢掉空。
6. 过滤：IP 与用户名都给了是 **AND**；只给一个就只比那一个；都不给则全要。
7. `Collections.reverse` 再 copy 成 `SysUserOnline`。`PageResult.build(list)`：所谓分页的 total 就是当前过滤后的条数。

**`getInfo`（自己）：** 不扫 `online_tokens:*`。`StpUtil.getTokenValueListByLoginId(StpUtil.getLoginIdAsString())` 拿到**当前账号**的 token 列表，再走同一套活性 + 读 DTO。无 `@SaCheckPermission`。无 `@Log`。

**`forceLogout`：** 权限 `monitor:online:forceLogout`。直接 `kickoutByTokenValue(tokenId)`。名牌删除靠监听器 `doKickout`，不在 Controller 里 `RedisUtils.deleteObject`。菜单还有 `monitor:online:batchLogout` 和 `monitor:online:query`，Controller **都没挂**。

**`remove`：** 先取自己的 token 列表，`filter(key.equals(tokenId)).findFirst().ifPresent(kickout...)`。别人的 token 在列表里找不到 → **什么都不踢**，仍然 `R.ok()`。不要把 200 说成「一定踢掉了」。

### 图、表或文本图

**图题 / caption：** 值班室四扇窗与两只柜子。alt：Cache 读 Redis INFO；登录/操作日志读写 MySQL；在线名单读 online_tokens；unlock 擦 pwd_err_cnt。

```text
                    ┌──────── Redis ────────┐
                    │ INFO / DBSIZE / stats │◄──── GET /monitor/cache
                    │ online_tokens:{token} │◄──── GET /monitor/online[/list]
                    │                       │      DELETE kickout ──► Sa-Token
                    │                       │         └─ doKickout 删名牌
                    │ pwd_err_cnt:{name}    │◄──── GET /monitor/loginInfo/unlock/{name}
                    └───────────┬───────────┘
                                │ 登录成功写名牌（L-013，非本 Controller）
                                v
        ┌──────── MySQL 基座 10-cde-base-ddl ────────┐
        │ sys_login_info  ◄─ LoginInfoEvent @Async   │
        │   GET list / POST export                   │
        │   DELETE ids / DELETE clean                │
        │ sys_oper_log    ◄─ OperLogEvent @Async     │
        │   GET list / POST export                   │
        │   DELETE ids / DELETE clean                │
        └────────────────────────────────────────────┘

        @Log 方法（含本课 export/remove/clean/unlock/forceLogout/remove）
                │
                v
           LogAspect.around → publish OperLogEvent → 又写 sys_oper_log
```

**文字等价物：** 上半是 Redis 三样互不替代的东西：服务器 INFO 仪表、在线名牌、密码错次计数。Cache 窗只碰第一样。在线窗读第二样、踢人让 Sa-Token 回调去删第二样。登录解锁只碰第三样。下半是两张 MySQL 表，由别的房间发布事件异步插入；本课 Controller 负责分页读、Excel 导出、按主键删、无 WHERE 清空。凡是本课标了 `@Log` 的变更窗，切面还会再往操作日志表写一行。

**图的边界：** 不画用户表 `login_ip`/`login_date`（那是 `updateLastLoginInfo`，L-013）。不画 `createMonitorService` 的 `externalIntent`。不把 `RedisUtils.keys` 画成阻塞 `KEYS`。不保证 `clean` 之后表在下一毫秒仍为空——异步 CLEAN 行可能马上回来。

**图题 / caption：** 两扇解锁窗共用一把错次扫帚。alt：人事按 userId 查姓名再删键；监控按路径用户名直接删键。

```text
人事 L-015                         监控 L-023
GET /system/user/unlock/{userId}   GET /monitor/loginInfo/unlock/{userName}
permission: system:user:edit       permission: monitor:logininfo:unlock
selectUserById → 无用户则 fail     不查 sys_user
键 = pwd_err_cnt: + user.userName  键 = pwd_err_cnt: + 路径字符串
都不删登录表，都不删临时密码键，都不踢在线 token
```

**文字等价物：** 两扇窗最后都是「有这个 Redis 键就删掉，没有也当成功」。人事窗多一次用户存在性检查，用档案里的姓名拼键。监控窗相信管理员在日志页点到的用户名。把其中一扇说成「解锁用户账号 / 作废会话 / 清空日志」都过宽。

**图的边界：** 不把错次 TTL（`lockTime` 分钟，配置默认 10）画进监控 Controller——它只删键，不读阈值。不把短信/邮箱登录的错次说成另一把钥匙：L-013 已钉它们共用 `pwd_err_cnt:`。

### 正例、反例与边界

**正例 1：** 数方法。`CacheController` 1 个映射方法 + 1 个内部 record。`SysLoginInfoController` 5 个。`SysOperlogController` 4 个。`SysUserOnlineController` 4 个。与矩阵 (a) 四行同名。包路径四个文件，没有第五份。

**正例 2：** Cache 还连接。`getInfo` 的 `finally` 调 `RedisConnectionUtils.releaseConnection`。打开文件能用手指点到，不是口嗨「用了 RedisUtils」。

**正例 3：** 命令统计削皮。`cmdstat_get` → name `get`；value 取 `calls=` 与 `,usec` 之间。INFO 原文里 usec 还在，只是饼图不用它。

**正例 4：** 登录列表默认按 `infoId` 倒序。`pageQuery.orderByColumn` 为空才加这条。导出走不分页 `selectVoList`。

**正例 5：** 事件状态词 ≠ 表状态码。`LOGIN_SUCCESS`/`LOGOUT`/`REGISTER` → 表 `"0"`；`LOGIN_FAIL`（`"Error"`）→ 表 `"1"`。其它事件状态词不会进这两个分支，`status` 可能空着插入。

**正例 6：** 两份 `clean` 权限与 `remove` 相同，都是 `:remove`，外加 `@Lock4j` 和 `BusinessType.CLEAN`。

**正例 7：** 在线 `PageResult.build(list)` 的 total 等于过滤后条数。没有 `PageQuery` 参数。

**正例 8：** `forceLogout` 与 `remove` 都 `try/catch NotLoginException` 后 `R.ok()`。目标早已下线仍 200。

**正例 9：** `UserActionListener.doKickout` 才 `deleteObject(online_tokens: + token)`。Controller 踢人方法体里没有这行。

**正例 10：** 基座 DDL：`sys_login_info` 在 `10-cde-base-ddl.sql` 约 280 行；`sys_oper_log` 约 195 行。实体 `@TableName` 与表名一致。

**正例 11：** 菜单 `50-cde-base-dml.sql`：缓存页 `monitor:cache:list`；登录日志按钮含 list/query/remove/export/unlock；操作日志含 list/query/remove/export；在线含 list/query/batchLogout/forceLogout。对照 Controller 注解，能指出 query/batchLogout 没有映射方法。

**正例 12：** classic 登记。`03-backend-module-modes.md`：`wta-modules/wta-system` = classic。监控四类在 `controller/monitor`，登录/操作服务在 `service/impl`，Mapper 空接口继承 `BaseMapperPlus`，无 XML 自定义语句。

**反例 1：** 「矩阵漏了 Cache 的 getNames/clear。」本树没有那些映射。只有 `getInfo`。

**反例 2：** 「`GET /monitor/cache` 会列出 `pwd_err_cnt:` 和 `online_tokens:`。」它读的是 Redis 服务器 INFO，不是按前缀扫业务键。

**反例 3：** 「登录日志表叫 `sys_logininfor`。」工作树是 `sys_login_info`。

**反例 4：** 「HTTP 前缀是 `/monitor/logininfo`（全小写）。」类上是 `/monitor/loginInfo`。权限才是 `monitor:logininfo:*`。

**反例 5：** 「`unlock` 会删 `sys_login_info` 里该用户的行。」零 SQL。只可能删错次键。

**反例 6：** 「`unlock` 是 POST，因为改了 Redis。」映射是 `@GetMapping`。这是存量偏差，口试按磁盘。

**反例 7：** 「清空登录日志等于给人解锁、等于踢下线。」三只柜子。clean 只撕 MySQL 访客簿。

**反例 8：** 「`SysOperlogController` 有详情 `GET /{operId}`，因为 Service 有 `selectOperLogById`。」没有这扇 HTTP 窗。

**反例 9：** 「类名 / 服务名大小写可以混着搜文件。」Controller 文件是 `SysOperlogController.java`；服务是 `ISysOperLogService` / `SysOperLogServiceImpl`；表 `sys_oper_log`。

**反例 10：** 「在线用户在表 `sys_user_online`。」没有这张表。`SysUserOnline` 无 `@TableName`。

**反例 11：** 「`GET /monitor/online` 要 `monitor:online:list`。」那是 `/list`。光秃 `GET /monitor/online` 是 `getInfo`，方法上无权限注解。

**反例 12：** 「`DELETE /monitor/online/myself/{tokenId}` 也能踢别人，因为内部同样 `kickoutByTokenValue`。」先 `getTokenValueListByLoginId` 过滤。别人的 token 进不了 `ifPresent`。

**反例 13：** 「菜单有批量强退，所以有 `DELETE` 批量接口。」种子权限 `monitor:online:batchLogout` 在本课四份 Java 里零引用。

**反例 14：** 「这四份是 layered，UseCase 在 `usecase/monitor`。」`wta-system` 无这条目录。在线逻辑甚至停在 Controller。

**反例 15：** 「`NotificationMonitorController.snapshot` 属于 OBJ-23。」那是 notify 模块，矩阵另一行，课次更靠后。

**反例 16：** 「Cache `getInfo` 和在线 `getInfo` 是同一个 Java 方法。」两个类、两块门牌、两种返回：`R<CacheListInfoVo>` vs `R<PageResult<SysUserOnline>>`。

**反例 17：** 「导出是 GET，因为只读。」两份 export 都是 `@PostMapping("/export")` 且 `@Log EXPORT`。

**反例 18：** 「`@Log` 的 list 也会进操作日志。」list / cache getInfo / online list / online getInfo **没有** `@Log`，切面不切它们。

**边界：**

- API-005 写「变更用 POST + `@Log`」。本课变更窗实际是 DELETE（remove/clean/forceLogout/remove）和 GET（unlock）。口试描述存量，不把偏差改口成规范已经满足。
- `clean` 返回 `R.ok()`，不经 `toAjax`，不把删除行数给前端。
- `remove` 的路径是 `Long[]` / 在线是 `String tokenId`。前端 domain 测试会 `encodeURIComponent` 再 join 逗号；这是 L-024 的编码合同，本课只认后端 `@PathVariable`。
- 登录日志 `status` 是字符串 `"0"`/`"1"`；操作日志 `status` 是整数（`BusinessStatus` 序数）。不要用同一套字典口述两张表。
- `recordLoginInfo` / `recordOper` 是 `@Async`。Controller 查询可能短暂看不到刚发生的登录/操作。不要把「列表立刻有行」说成同步插入。
- `CacheController.getInfo throws Exception`：连接或 INFO 失败会冒到全局异常处理，不是 `R.fail` 包一层业务码。
- 在线过滤是内存 equals，不是 SQL like。IP 必须整段相等。
- `forceLogout` 不校验 token 是否属于某部门数据权限。有这颗权限就能踢名单上出现的任意 token。
- Excel 导出把当前筛选条件下的**全部**行拉进内存。大表是运维风险，不是本课 HTTP 另有流式接口。
- `SysLoginInfo` / `SysOperLog` 都**不是** `BaseEntity` 那套 createBy 审计列。它们是专用日志表。

## 变式与迁移

- **变式 A：只想看 Redis 忙不忙。** 走 `GET /monitor/cache`。不要去在线列表里数名牌当 QPS，也不要扫 `CacheNames`。

- **变式 B：有人被锁在门外。** 先看登录日志里最近是否 `status=1` 且文案是重试上限；再决定打哪扇 unlock。监控窗要用户名；人事窗要 userId。清日志解决不了锁。

- **变式 C：怀疑有人改了资料。** 去操作日志，按 title / businessType / operName / 时间筛。登录日志只证明进没进门。

- **变式 D：踢掉一台还在线的设备。** 管理员用 `forceLogout`；用户自己用 `myself`。不要用登录日志 `remove` 当踢人——那只撕历史行。

- **变式 E：只要自己的多端列表。** `GET /monitor/online`，不要要 `monitor:online:list`。管理页全站名单才是 `/list`。

- **变式 F：产品要「清空日志且不再留下清空记录」。** 今天 `clean` 带着 `@Log CLEAN`，切面仍会写。要改的是切面或这方法的注解，不是口试假装已经静音。

- **变式 G：产品要按 cacheName 清业务缓存。** 本课没有这扇窗。`SysConfigController.refreshCache` / 字典刷新是 L-021，别偷接到 `CacheController`。

- **变式 H：菜单出现批量强退按钮。** 种子有权限串，Controller 无方法。补 API 是新合同；在补上之前不要把 DML 当 HTTP。

- **迁移口诀：** 先数四份类四块牌子 → 分清 Redis 仪表 / 错次键 / 名牌 / 两张 MySQL 本子 → 写入在事件与切面，本课是读和撕 → 两扇 getInfo、两扇 unlock、两扇踢人不要并成一词 → 菜单多出来的 query/batchLogout 不是磁盘方法。跳步会出现「把 INFO 当成键浏览器」「用清空日志给人开锁」「把 myself 说成管理员强退」。

## 常见误区

1. **「OBJ-23 还包含 `createMonitorService`。」** 那是 OBJ-24 / L-024。本课只认四份 Controller。
2. **「Cache 能 flush、能 getValue。」** 只有 `getInfo`。
3. **「`/monitor/loginInfo` 和权限 `monitor:logininfo` 大小写可以随便写。」** URL 驼峰 I；权限全小写。
4. **「解锁 = 删登录行 = 踢会话 = 作废临时密码。」** 四件不同的事，四把（或更多）钥匙。
5. **「在线列表是分页 SQL。」** 扫 Redis + 内存过滤 + `total=size`。
6. **「`SysUserOnline` 是表实体。」** 无 `@TableName`。
7. **「`GET /monitor/online` 与 `/list` 权限相同。」** 前者无权限注解，后者 `monitor:online:list`。
8. **「`clean` 是 TRUNCATE，所以 `@Log` 插不进去。」** 源码是 MyBatis `lambda().delete()`；随后切面仍可 insert。
9. **「Service 有 `selectOperLogById` 所以有详情 API。」** 无映射。
10. **「四份 Controller 都继承 `BaseController`。」** Cache 没有。
11. **「登录日志写入在 `SysLoginInfoController.insert`。」** 没有 insert 方法。写入在 `@EventListener`。
12. **「操作日志写入在 Controller 里 `operLogService.insertOperlog`。」** Controller 不调 insert；`LogAspect` 发事件。
13. **「`forceLogout` 方法里删了 `online_tokens:`。」** 删键在 `UserActionListener.doKickout`。
14. **「batchLogout 在 `SysUserOnlineController`。」** 磁盘无此方法。
15. **「`wta-system` 监控应按 layered 加 UseCase。」** 登记表 classic；本课不发动重构。
16. **「`unlock` 的 GET 已经符合 API-005。」** 规范要变更走 POST；这是存量 GET 变更窗。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏。

1. 打开目录 `controller/monitor/`。确认只有四份 Java。在每份类上圈 `@RequestMapping`。
2. 在 `CacheController.getInfo` 圈 `info("commandstats")`、`dbSize`、`releaseConnection`。确认没有第二个 `@GetMapping`/`@DeleteMapping`。
3. 在 `SysLoginInfoController` 五指点 list/export/remove/clean/unlock。圈 unlock 的 `@GetMapping` 和 `PWD_ERR_CNT_KEY`。确认方法体没有 Mapper 调用。
4. 在 `SysOperlogController` 圈四扇窗。打开 `ISysOperLogService`，把 `selectOperLogById` 和 `insertOperlog` 标成「无 HTTP」。
5. 在 `SysUserOnlineController` 把 `GET /list`、`GET /`、`DELETE /{tokenId}`、`DELETE /myself/{tokenId}` 分成四格。圈两处「无 `@SaCheckPermission`」。圈 `getTokenActiveTimeoutByToken(token) < -1`。
6. 打开 `50-cde-base-dml.sql` 搜 `monitor:online:batchLogout`，再回到 Controller 全文搜索同一字符串，看搜不到。

## 总结、词汇表与下一步

- **四块牌子：** `/monitor/cache`、`/monitor/loginInfo`、`/monitor/operlog`、`/monitor/online`。矩阵 (a) 四行方法名单与磁盘一致。
- **三只 Redis 东西不要并：** INFO 仪表（Cache）、`online_tokens:` 名牌（在线）、`pwd_err_cnt:` 错次（unlock）。
- **两本 MySQL：** `sys_login_info` 访客簿，`sys_oper_log` 盖章簿。写入分别是 `LoginInfoEvent` 与 `OperLogEvent`，异步。本课 Controller 不 insert。
- **两扇 getInfo：** Cache 的仪表；在线的「我的设备」。
- **两扇 unlock：** 人事按 userId（L-015）；监控按 userName（本课）。扫帚相同，门牌不同。
- **两扇踢人：** 管理员 `forceLogout`；自己 `remove`（`/myself/`）。监听器才摘名牌。
- **菜单多余项：** `*:query`、`monitor:online:batchLogout` 不是本课 HTTP。

词汇表：`CacheController` / `CacheListInfoVo` / `SysLoginInfoController` / `sys_login_info` / `LoginInfoEvent` / `pwd_err_cnt:` / `SysOperlogController` / `sys_oper_log` / `OperLogEvent` / `LogAspect` / `BusinessType` / `SysUserOnlineController` / `UserOnlineDTO` / `online_tokens:` / `forceLogout` / `kickoutByTokenValue` / classic。

下一步：L-024 才把 `createMonitorService` 与 web-domain 四页对上这些 URL。L-021 才是配置/字典的 `refreshCache`。L-013 已经讲过名牌怎么贴上。本课结束仍不是掌握证明；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller | 业务模块公开入口；本课四份类在 `wta-system` 的 `controller/monitor` | 四份 `*Controller.java` | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` = classic；不按 layered 口述 | 登记表 classic 行 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 表 `sys_login_info` / `sys_oper_log`；菜单权限含 list/query/export/remove/unlock/forceLogout/batchLogout/cache:list | DDL 约 195、280 行；DML 监控菜单段 | 2026-09-16 |
| S-L023-01 | `CacheController.java` | 唯一 HTTP `getInfo`；`/monitor/cache`；权限 `monitor:cache:list`；INFO/commandstats/dbSize；finally 还连接；内部 record `CacheListInfoVo`；不继承 `BaseController` | 类与 `getInfo` | 2026-09-16 |
| S-L023-02 | `SysLoginInfoController.java`；`ISysLoginInfoService.java`；`SysLoginInfoServiceImpl.java`；`SysLoginInfo.java` | 五扇窗；表名；异步 `LoginInfoEvent` 落库；clean 无 WHERE；unlock 只删 `pwd_err_cnt:`；GET 变更 | `/monitor/loginInfo` 各映射；`recordLoginInfo`；`cleanLoginInfo` | 2026-09-16 |
| S-L023-03 | `SysOperlogController.java`；`ISysOperLogService.java`；`SysOperLogServiceImpl.java`；`SysOperLog.java` | 四扇窗；类名大小写；`selectOperLogById` 无 HTTP；异步 `recordOper`；clean+Lock4j | `/monitor/operlog`；服务接口 | 2026-09-16 |
| S-L023-04 | `SysUserOnlineController.java`；`UserOnlineDTO.java`；`SysUserOnline.java` | 四扇窗；扫 `online_tokens:*`；`< -1` 跳过；`PageResult.build(list)`；forceLogout vs myself；两处无权限注解 | 类上 `/monitor/online` | 2026-09-16 |
| S-L023-05 | `LogAspect.java`；`OperLogEvent.java`；`BusinessType.java` | `@Log` 切面发事件；EXPORT/DELETE/CLEAN/FORCE/OTHER；list 无 @Log 不记 | `doAround` / `handleLog` | 2026-09-16 |
| S-L023-06 | `SysLoginService.recordLoginInfo`；`UserLoginSuccessListener.java`；`UserActionListener.java`；`LoginInfoEvent.java`；`CacheNames.java` | 登录事件生产；名牌写入/删除；键前缀 `online_tokens:` 与 `pwd_err_cnt:` | L-013 写入链在本课的对接点 | 2026-09-16 |
| S-L023-07 | `SysUserController.unlock`；`Constants.java` | 第二扇解锁；SUCCESS/FAIL 与 LOGIN_* 状态词 | `GET /system/user/unlock/{userId}`；常量字段 | 2026-09-16 |
| S-L023-08 | `frontend/packages/domains/system/src/monitor/index.ts`（仅边界） | 前端工厂打的 URL 与本课 HTTP 对齐，但 OBJ-24 才认格子；另有外链意图不属于这四份 Controller | `createMonitorService` 的 cache/loginInfo/online/operationLogs | 2026-09-16 |
