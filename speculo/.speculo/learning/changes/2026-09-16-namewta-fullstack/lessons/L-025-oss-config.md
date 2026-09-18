---
lesson_id: L-025
objective_ids: [OBJ-25]
claimed_cells:
  - A:SysOssConfigController.*
  - B:ISysOssConfigService.updateOssConfigStatus
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 8
  - segment: switch-side-effects
    minutes: 12
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L025-01, S-L025-02, S-L025-03, S-L025-04, S-L025-05, S-L025-06, S-L025-07, S-L025-08]
---

# Lesson 025：哪间仓库当正门——OSS 配置切换的副作用

## 学完你能做什么

打开 `wta-system` 里**一份** Controller，你能**口述六扇公开窗**，并单独把 OBJ-25 点名的那句说完：**`POST /resource/oss/config/changeStatus` 不是启停开关，它只认主键，强制把这一行变成唯一默认，提交后再改 Redis 正门指针；已存对象不搬家，工厂里的旧客户端也不关。** 不要把 `status` 说成用户那种 0/1 启停，也不要把切换说成「刷新 OSS 配置缓存」。

本课认矩阵两格，方法名以**磁盘**为准：

1. **`A:SysOssConfigController.*`**（`@RequestMapping("/resource/oss/config")`）：`list` / `getInfo` / `add` / `edit` / `remove` / `changeStatus`。没有 export，没有 refreshCache，没有 PUT/DELETE。
2. **`B:ISysOssConfigService.updateOssConfigStatus`**：唯一被 `changeStatus` 调用的切换方法。接口上还有 `init` / `queryById` / `queryPageList` / `insertByBo` / `updateByBo` / `deleteWithValidByIds`，那些是别的窗和启动器的，口试切换副作用时不要把它们说成同一扇 HTTP。

模块模式是 **classic**：`Controller → ISysOssConfigService → SysOssConfigServiceImpl → SysOssConfigMapper`。登记表把 `wta-modules/wta-system` 标 classic。磁盘上**没有** UseCase、**没有** DAO。Mapper XML 是空壳；切换用的 `selectByIdForUpdate` / `clearOtherDefaultStatuses` 写在 Mapper 接口的 `@Select` / `@Update` 上。

本课不宣称你会拆 `SysOssController` 列表/下载/删除（L-026）、直传票据（L-027）、对象迁移（L-028）、或浏览器 `createOssUploadClient`（L-029）。`OssFactory`、`OssConfigChangeListener`、readiness 只作为切换的**下游副作用**出现，用来解释「库已经换正门，为什么上传还可能走进旧仓库 / 健康检查还要再扫一遍」。

## 先把宏观地图放在桌上

L-005 已经把 HTTP 路径和基座表钉成公共合同。L-021 的 `status` 是参数/字典里另一回事。本课走进**仓库名牌柜台**：表 `sys_oss_config` 可以挂很多间仓库（MinIO / 七牛 / 阿里云 / 腾讯云 / 自建 `image`），但全楼**同时只准一间当正门**。正门指针不在 Spring Cache 的 `sys_oss_config` 里，而在一只独立 Redis 桶 `global:sys_oss:default_config`。没写 `configKey` 的上传走 `OssFactory.instance()`，先看这只桶，再按名牌去墙上拿 JSON 图纸。

2026-09-16 工作树：Java 类在 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/SysOssConfigController.java`。`extends BaseController`。只注入 `ISysOssConfigService`。启动时 `SystemApplicationRunner` 调 `init()`，再 `readinessService.refresh()`。前端工厂在 `createSystemResourceService().ossConfigs`，页面是 admin-web 的 OSS 配置表；格子仍归后续 OSS 前端课，本课只借它们证明 HTTP 动词和「取消默认」文案对不上实现。

```text
管理员浏览器 / 已登录调用方
    │
    └─ /resource/oss/config/*     SysOssConfigController     六扇
                │
                v
        ISysOssConfigService / SysOssConfigServiceImpl
                │  classic：ServiceImpl 直接持有 Mapper
                ├─ SysOssConfigMapper
                │     XML 空；切换 SQL 在注解上
                v
        表 sys_oss_config          status='Y' 必须恰好一行
                │
                │  写成功后 Spring 事件（本进程）
                v
        OssConfigChangeEvent
                │  @DsTxEventListener 提交后才跑
                v
        OssConfigChangeListener.refreshOssConfig
                ├─ useDefault：只写 Redis 正门指针，然后 return
                ├─ save/remove：才动 CacheUtils SYS_OSS_CONFIG + OssFactory.remove
                └─ finally：OssStorageReadinessService.refresh()
                │
                v
        Redis 桶  global:sys_oss:default_config  = configKey 字符串
        Spring Cache 名 sys_oss_config（无 TTL）键=configKey 值=整行 JSON
        进程内 Map    OssFactory.CLIENT_CACHE     键=configKey 值=OssClient
```

**类比：** 公司在工业园租了好几间仓库。前台只挂一块「正门」牌子（Redis 默认键）。每间仓库的门锁图纸贴在墙上的格子里（`sys_oss_config` JSON）。搬运工口袋里还揣着已经打开的旧钥匙串（`OssFactory` 进程缓存）。管理员在柜台把正门牌子换到另一间：抽屉里的「是否默认」贴纸会撕来贴去，前台牌子会换。已经堆在旧仓库里的箱子（`sys_oss.service`）**不会**自己走到新仓库。口袋里那串旧钥匙也**不会**被没收。

**类比失效处：**

1. `status` 不是「这间仓库开门还是关门」。它只表示是不是正门。`Y`/`N` 必须全局恰好一个 `Y`。
2. 切换窗**不**重贴墙上的 JSON 图纸，也**不**没收钥匙。那是 `save` / `remove` 事件的活。
3. 事件是本进程 Spring 事件，不是 Redis 广播。别的楼层看见新正门，是因为它们每次找默认实例都**现读** Redis 桶。
4. 六扇窗都在 `wta-system` classic，不是 layered 的 UseCase。
5. 前端开关文案有「取消默认」；服务端 `updateOssConfigStatus` **从不读** body 里的 `status`，永远把这一行打成 `Y`。

## 核心概念与机制

### 直觉讲解

先记住三张纸条，再背路径：

- **名牌 `configKey`。** 热路径和 `sys_oss.service` 用它，不是主键 `ossConfigId`。墙上 JSON 的键也是它。
- **正门贴纸 `status`。** 只允许 `Y` 或 `N`。`Y` = 默认配置。不是用户表那种停用。
- **门锁类型 `accessPolicy`。** 只允许 `0=PRIVATE` 或 `2=PUBLIC_READ`。**默认配置必须 PRIVATE。** 公开桶可以存在，但不能当正门。

再记住三扇写窗和一把锁：

- 新增 / 修改：`POST /resource/oss/config`、`POST /resource/oss/config/edit`。可以在 body 里带 `status=Y` 把新行（或这行）变成正门，但**不能**用普通编辑把当前正门改成 `N`。
- 删除：`POST /resource/oss/config/remove/{ids}`。正门不能删；内置四行不能删；`sys_oss` 里还有对象引用的不能删。
- 切换：`POST /resource/oss/config/changeStatus`。只带主键就够。实现**忽略** `status` / `configKey` / `accessPolicy`。行锁这一行，把别的 `Y` 打成 `N`，自己写成 `Y`，发 `useDefault` 事件。

「点了切换为什么上传还走旧桶 / 为什么取消默认没取消」先问五句话，不要先怪 MinIO：

1. 这次 HTTP 是 `changeStatus`、`edit` 带 `Y`，还是 SQL 直接改表？
2. body 里的 `status=N` 你以为是取消，服务端有没有读它？
3. 事务提交了没有？事件是 `@DsTxEventListener`，回滚则 Redis 正门不动。
4. 这次是 `useDefault` 还是 `save`？只有 `save`/`remove` 才动 JSON 墙和 `OssFactory.remove`。
5. 你上传的是「未指定配置」的默认实例，还是已经落在 `sys_oss.service=旧 configKey` 的对象？

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 对象存储配置 | OSS config | 实体 `SysOssConfig`，表 `sys_oss_config` |
| 配置主键 | `ossConfigId` | 路径变量 / 切换 body 真正用的字段 |
| 配置名牌 | `configKey` | 缓存键、`sys_oss.service`、正门 Redis 值 |
| 是否默认 | default flag | 列 `status`；`SystemConstants.YES/NO` = `"Y"` / `"N"` |
| 桶权限 | access policy | 列 `accessPolicy`；`0` PRIVATE，`2` PUBLIC_READ |
| 切换默认 | change status / switch default | `POST .../changeStatus` → `updateOssConfigStatus` |
| 启用停用（误称） | enable/disable | **本窗不是这个意思**；实现永远激活 |
| 正门指针 | default config key | `OssConstant.DEFAULT_CONFIG_KEY` = `global:sys_oss:default_config` |
| 图纸墙 | OSS config cache | `CacheNames.SYS_OSS_CONFIG` = `"sys_oss_config"`（无 TTL） |
| 钥匙串 | client factory cache | `OssFactory.CLIENT_CACHE`，进程内 `ConcurrentHashMap` |
| 默认实例 | `OssFactory.instance()` | 先读正门指针，再 `instance(configKey)` |
| 指定实例 | `OssFactory.instance(configKey)` | 直传/迁移/已存对象按名牌取客户端 |
| 保存事件 | `OssConfigChangeEvent.save` | 带 JSON；`defaultConfig=false` |
| 删除事件 | `OssConfigChangeEvent.remove` | JSON 空；驱逐墙 + `OssFactory.remove` |
| 正门事件 | `OssConfigChangeEvent.useDefault` | `defaultConfig=true`；监听器写 Redis 后 **return** |
| 提交后监听 | `@DsTxEventListener` | 动态数据源事务提交后；不是 Spring `@TransactionalEventListener` |
| 动态事务 | `@DSTransactional` | 增删改和切换四段写路径都打了 |
| 行锁 | `SELECT ... FOR UPDATE` | `selectByIdForUpdate` |
| 清其它正门 | clear other defaults | `update sys_oss_config set status='N' where status='Y' and oss_config_id <> ?` |
| 系统内置 | system data ids | `OssConstant.SYSTEM_DATA_IDS` 四行种子主键 |
| 对象引用 | OSS references | `select count(*) from sys_oss where service = #{configKey}` |
| 就绪快照 | storage readiness | `OssStorageReadinessService.refresh()`；默认行的来源标签是 `DEFAULT` |
| 经典分层 | classic | Controller → ServiceImpl → Mapper |
| 防重复提交 | `@RepeatSubmit` | add / edit / changeStatus 默认 5000ms；remove 没有 |
| 密钥不回显 | `@JsonIgnore` | `SysOssConfigVo.secretKey` |

权限串不要混：六扇都挂 `system:ossConfig:*`。`list` **和** `getInfo` 都是 `system:ossConfig:list`，没有 `query`。`changeStatus` 挂 **edit** 不是 list。`remove` 挂 remove。页面表格上的开关**没有** `v-hasPermi`，只有修改/删除按钮有；只有 list 权限的人点开关会 403，前端 `catch` 再把开关拨回去。

HTTP 合同（API-005）：查询 GET，变更 POST。本 Controller **零**个 `@PutMapping` / `@DeleteMapping`，单测 `OssConfigHttpContractUnitTest` 钉死。写窗都有 `@Log`。add / edit / changeStatus 的 `isSaveRequestData = false`（body 里有 secret）；remove 才存请求数据。

`changeStatus` **没有** `@Validated`，也没有 `AddGroup` / `EditGroup`。BO 上那些 `@NotBlank configKey` / `@Pattern status` **管不到**这扇窗。

### 机制/因果链

#### 1. 六扇窗在磁盘上的真实映射

文件：

- `.../controller/system/SysOssConfigController.java`
- `.../service/ISysOssConfigService.java`
- `.../service/impl/SysOssConfigServiceImpl.java`

类注解：`@Validated` `@RequiredArgsConstructor` `@RestController`。

| HTTP | 动词 | Java | 权限 | 写库？ | 切换相关副作用 |
| --- | --- | --- | --- | --- | --- |
| `/resource/oss/config/list` | GET | `list` | `system:ossConfig:list` | 否 | 不读 Redis 正门；`queryPageList` 走表 |
| `/resource/oss/config/{ossConfigId}` | GET | `getInfo` | **list**（不是 query） | 否 | 主键；VO 的 `secretKey` `@JsonIgnore` |
| `/resource/oss/config` | POST | `add` | `system:ossConfig:add` | 是 | 空 status 归一成 `N`；`Y` 会 `clearOtherDefaultStatuses`；发 **save**，若是默认再发 **useDefault** |
| `/resource/oss/config/edit` | POST | `edit` | `system:ossConfig:edit` | 是 | 当前 `Y` 改成 `N` 直接抛「请使用默认配置切换操作」；`Y` 会清其它正门；save ± useDefault |
| `/resource/oss/config/remove/{ossConfigIds}` | POST | `remove` | `system:ossConfig:remove` | 是（物理删） | 内置 / 正门 / 引用 / 「必须恰好一个默认」四道闸；成功发 **remove** |
| `/resource/oss/config/changeStatus` | POST | `changeStatus` | `system:ossConfig:edit` | 是 | **只** `updateOssConfigStatus`；忽略 body 的 status；只发 **useDefault** |

`init()` 不是 HTTP。启动失败条件：表里 `status='Y'` 的行数 **不是 1**，文案「OSS配置必须且只能存在一个默认配置」。然后校验每行 accessPolicy ∈ {0,2}，默认行必须 PRIVATE，把默认 `configKey` 写入 Redis 正门，把**每一行** JSON `CacheUtils.put` 进 `sys_oss_config`。

#### 2. OBJ-25 主链：`updateOssConfigStatus` 到底改了什么

方法打 `@DSTransactional`。顺序按磁盘：

1. `ossConfigId` 空 → `ServiceException("OSS配置主键不能为空")`。墙、正门、工厂都不动。
2. `selectByIdForUpdate(id)`。没有行 → 「OSS配置不存在」。
3. `validateAccessPolicy(config)`：不是 `0` 也不是 `2` → 「OSS桶权限只允许0=PRIVATE或2=PUBLIC_READ」。**读的是库里的行**，不是 body。
4. 再加一道比普通校验更窄的闸：`accessPolicy` 不是 `"0"` → 「默认OSS配置必须为PRIVATE」。公开行在这里停。单测 `referencedOrDefaultConfigCannotBeDeletedAndPublicCannotBecomeDefault` 钉死：**此时还没有** `clearOtherDefaultStatuses`。
5. `clearOtherDefaultStatuses(id)`：把**其它** `status='Y'` 打成 `N`。返回清掉的行数。
6. 内存里 `config.setStatus("Y")`，`updateById`。不是 1 行 → 「默认OSS配置切换失败」。因为在同一动态事务里，前面的清 `Y` **会回滚**。单测 `defaultSwitchFailureAfterClearingDefaultsMustAbortTheTransaction` 就是这条：clear 已经返回 1，update 返回 0，必须整段 abort。
7. `SpringUtils.context().publishEvent(OssConfigChangeEvent.useDefault(config.getConfigKey()))`。
8. 返回 `cleared + activated`。Controller `toAjax(int)`：大于 0 才 `R.ok()`。正常至少 `activated=1`。

没有做的事（口试要主动说）：

- 不读 `bo.status`、`bo.configKey`、`bo.accessPolicy`。
- 不 `CacheUtils.put` / `evict` `SYS_OSS_CONFIG`。
- 不 `OssFactory.remove`。
- 不改 `sys_oss` 里已有对象的 `service`。
- 不校验 `SYSTEM_DATA_IDS`（内置行也可以被设为正门）。
- 不跑 `validEntityBeforeSave` 全套（唯一性、status 枚举、PRIVATE 默认那套在 add/edit 里）。

所以：前端 `changeStatus(ossConfigId, status, configKey)` 即使传 `status='N'`、传错 `configKey`，只要主键指向一间 PRIVATE 仓库，服务端仍把它设为正门。

#### 3. 提交之后：`useDefault` 监听器的早退

`OssConfigChangeListener.refreshOssConfig` 标 `@DsTxEventListener`。单测要求：不是 Spring `TransactionalEventListener`；动态事务**提交后**才跑，**回滚后绝不跑**。

```text
try:
  if event.defaultConfig():          // useDefault 走这里
      RedisUtils.setCacheObject(DEFAULT_CONFIG_KEY, event.configKey())
      return                         // 跳过图纸墙和 OssFactory.remove
  if oldConfigKey 改名:
      evict 旧键; OssFactory.remove(旧键)
  if configJson 空: evict 当前键     // remove
  else: CacheUtils.put 当前键 = JSON // save
  OssFactory.remove(当前键)
finally:
  readinessService.refresh()         // 早退也会跑
```

`RedisUtils.setCacheObject(key, value)` 不带 Duration，Redisson `bucket.set`，正门指针**没有 TTL**。

`OssProperties`（工厂拿来建客户端的那张图纸）**没有** `status` 字段。所以墙上 JSON 里那张过期的「是否默认」贴纸，**不会**让客户端建错。工厂认的是：正门指针里的名牌 + 该名牌对应的 endpoint / 密钥 / 桶 / `accessPolicy`。

副作用拼起来：

- **本节点、提交成功：** 表恰好一个 `Y`；Redis 正门指向新 `configKey`；readiness 按新默认重扫（来源标签 `DEFAULT` 换人）；`CLIENT_CACHE` 里旧钥匙还在。
- **其它节点：** 收不到这个 Spring 事件。下次 `OssFactory.instance()` 自己读 Redis 正门，一般立刻跟新。它们口袋里的旧钥匙同样没收。readiness 靠定时（默认间隔 `PT1M`）或它们自己的别的刷新。
- **监听器里 Redis 写失败：** 库已提交。正门指针可能仍是旧的。`finally` 仍按**新库**重扫 readiness。下一次启动 `init()` 会按表把指针纠正。
- **切到已经是正门的那一行：** clear 返回 0，update 仍把 `Y` 写回，再发 `useDefault`。这是一把**修理扳手**：表里若出现两个 `Y`，对着其中一行切一次，其它 `Y` 会被清掉。`init()` 看见两个 `Y` 会直接拒绝启动，修不了。

#### 4. 邻居写窗：什么时候也会动正门

口试不要把切换副作用说成「只有 changeStatus」。三条旁路：

- **add 带 `status=Y`：** 先插入，再 clear 其它正门，发 save（JSON 上墙、`OssFactory.remove` 新键），再 `publishDefaultConfigIfNecessary` → 又一条 useDefault。空表不能只插 `N`：`validateExistingDefaultIfNotSwitching` 要求已有恰好一个默认。
- **edit 带 `status=Y`：** 也能清其它正门并 useDefault。但当前正门在普通编辑里改 `N` 会抛「默认OSS配置不能通过普通编辑取消，请使用默认配置切换操作」。陷阱：切换窗并不能「取消」成零个默认，它只会再指定一个。
- **SQL / 迁移脚本改 `status`：** 没有事件。正门 Redis、图纸墙、工厂、readiness 都还是旧的，直到 `changeStatus`、一次带 `Y` 的 save、或进程重启 `init()`。基座 `50-cde-base-dml.sql` 的 `NAMEWTA-OSS-ACCESS-DML-001` 在改 `access_policy` 前，若默认行数不是 1，会故意插入两行同主键哨兵让整块失败——合同认为「必须恰好一个默认」是后续迁移的前置。

删除路径的对称闸：正门不可删（「请先切换默认配置」）；`SYSTEM_DATA_IDS` 四行不可删；`countOssReferences(configKey)>0` 不可删；若此时表里默认行数已经不是 1，连删非默认都会被「必须且只能存在一个默认配置」挡住。先切换、再删，顺序不能反。

#### 5. 前端开关和 HTTP 合同怎么把人带沟里

`frontend/packages/domains/system/src/resource-service.ts`：

- `changeStatus: (ossConfigId, status, configKey) => POST /resource/oss/config/changeStatus` body `{ ossConfigId, status, configKey }`
- add/update 是 POST；delete 是 POST `/remove/{ids}`。e2e `oss-config-access-policy.spec.ts` 认这三枪都是 POST。

`OssConfigPage.vue` 的开关：`active-value="Y"` / `inactive-value="N"`。PUBLIC_READ 行 disabled。`handleStatusChange`：公开行强制拨回 `N`；否则弹窗文案 `status==='Y' ? '设为默认' : '取消默认'`，然后**照样**调用 `changeOssConfigStatus`。成功后再 `getList()`。

因此「取消默认」的真实结局：服务端把**这一行**再写成正门，列表刷新后开关仍是 `Y`，提示却可能是「取消默认成功」。要从 A 换到 B，正确动作是去点 **B** 的开关（N→Y），不是去关 A。

页面把传输值 `0`/`2` 投影成 `PRIVATE`/`PUBLIC_READ`；服务端仍存 `'0'`/`'2'`。不要把前端枚举名说进 SQL。

### 图、表或文本图

**图 1. 六扇窗、一只抽屉、正门指针和钥匙串**

```text
 /resource/oss/config
  GET  /list                         读表，不读正门
  GET  /{id}                         读表；secret 不进 JSON
  POST /                             add：可带 Y 当正门（save ± useDefault）
  POST /edit                         可带 Y；不能把当前正门改 N
  POST /remove/{ids}                 不能删正门 / 内置 / 引用
  POST /changeStatus                 只认 id；永远 Y；只 useDefault
        │
        v
 ISysOssConfigService.updateOssConfigStatus     ← 矩阵 B
        │  @DSTransactional
        v
  sys_oss_config   status='Y' 恰好一行；默认行 access_policy 必须 '0'
        │  提交后本进程事件
        v
  useDefault ──► Redis  global:sys_oss:default_config
                 （不改 sys_oss_config 墙，不 OssFactory.remove）
        │
        ├─ 未指定配置的上传  OssFactory.instance()  先读正门再拿 JSON
        └─ 已存对象 / 直传策略 / 迁移  OssFactory.instance(configKey)
                 sys_oss.service 仍是旧名牌，不随正门搬家
```

- **alt：** OSS 配置六扇 HTTP 窗写入同一张表；切换只改默认标记和 Redis 正门指针，不改对象行上的服务名牌。
- **caption：** 图 1——OBJ-25 的空间关系。矩阵 A 是六扇窗；矩阵 B 是切换方法。
- **文字等价物：** 管理员改仓库资料走 add/edit，删仓库走 remove，换正门走 changeStatus。列表永远读数据库。没点名的上传问 Redis 正门要名牌，再按名牌去 `sys_oss_config` 缓存拿图纸。已经上传的文件记在 `sys_oss.service` 上，换正门不会改这列。
- **图的边界：** 图上没有直传 init/sign、没有迁移 dry-run、没有 `CacheController`。不要把 `status` 画成「仓库开关」。不要把 PUT/DELETE 画进这六扇。

**图 2. 切换成功才往右；失败停在哪一层**

```text
[权限 ossConfig:edit + 登录] 无权限 → 停；表和正门不动
   v
[RepeatSubmit 5s] 连点 → 重复提交；表不动
   v
[ossConfigId] 空 / 行不存在 → 业务异常；表不动
   v
[accessPolicy] 不是 0/2，或不是 PRIVATE → 停在写库前
   v
[SELECT FOR UPDATE 这一行]
   v
[UPDATE 其它 Y → N]
   v
[本行 status=Y]  0 行 → 抛「切换失败」，事务回滚，其它 Y 恢复
   v
[publish useDefault] 仍在事务内；回滚则监听器不跑
   v
[提交]
   v
[@DsTxEventListener]
   ├─ Redis 正门 = 新 configKey     （无 TTL）
   ├─ 不 put/evict SYS_OSS_CONFIG
   ├─ 不 OssFactory.remove
   └─ finally readiness.refresh()   （本节点立刻；他节点靠定时）
   v
[HTTP 200] 只表示「表的唯一默认」和「本节点正门指针」已换
   不表示旧客户端已关、已存对象已搬家、他节点 readiness 已新
```

- **alt：** 切换先过权限和 PRIVATE 闸，再在事务里清其它默认并点亮目标行；提交后只写 Redis 正门指针并刷新本节点就绪快照。
- **caption：** 图 2——配置切换副作用链。`useDefault` 早退是设计，不是漏写。
- **文字等价物：** 点切换等于「指定这一行当唯一正门」。公开桶进不去。清其它默认和点亮本行是同一事务。提交后监听器只换前台牌子，不重画门锁图纸、不没收旧钥匙。HTTP 200 验收要用「表里是否只剩一个 Y」和「`OssFactory.instance()` 拿到的 clientId 是不是新名牌」，不要用「取消默认」的弹窗文案。
- **图的边界：** add/edit 的 save 事件不走这张早退枝。SQL 改表不出现在这扇门上。集群 Spring Cache 作废是 save/remove 才碰上的，本窗主链用的是 `RedisUtils` 直写。

### 正例、反例与边界

**正例 A：从默认 MinIO 换到另一间 PRIVATE。** 表里 `minio` 为 `Y`，`image` 为 `N` 且 `accessPolicy='0'`。`POST /changeStatus` body `{ "ossConfigId": image的主键 }`。clear 把 minio 打成 `N`，image 打成 `Y`，提交后 Redis 正门变成 `"image"`。下一次未指定配置的 `SysOssServiceImpl.upload` 走 `OssFactory.instance()` 得到 `clientId=image`。原先 `service=minio` 的对象仍用 `OssFactory.instance("minio")` 下载。

**正例 B：对着已经是正门的 PRIVATE 再切一次。** 用来修理「两个 Y」。clear 掉多余的 `Y`，useDefault 再写一遍指针。`init()` 看见两个 Y 只会抛错停机，不会修。

**正例 C：用 add 而不是 changeStatus 指定正门。** 新行 `status=Y`、`accessPolicy=0`。insert 后 clear 其它，save 把 JSON 上墙并 `OssFactory.remove` 新键，再 useDefault。这是旁路，不是矩阵 B，但副作用里「唯一默认」一样成立。

**正例 D：列表验收。** 切换 200 后 `GET /list` 立刻看见新的那个 `Y`，因为它不读 Redis。热路径验收要另开一枪未指定配置的上传或直接读 `global:sys_oss:default_config`。

**反例 1：** 「`changeStatus` 和用户 `changeStatus` 一样，传 `N` 就停用。」用户窗读 status。本窗忽略 status，永远 `Y`。

**反例 2：** 「取消默认会变成零个正门。」实现不允许零个。关当前正门的开关，等于又把当前行指定为正门。

**反例 3：** 「PUBLIC_READ 也能当默认，前端只是把开关灰掉。」服务端硬闸「默认OSS配置必须为PRIVATE」。绕过前端仍失败，且失败在写库前。

**反例 4：** 「切换会 `CacheUtils.put` 新 JSON / `OssFactory.remove`。」那是 save/remove。`useDefault` 早退。

**反例 5：** 「切换会把旧文件迁到新桶。」`sys_oss.service` 不动。搬家是 L-028 的迁移窗。

**反例 6：** 「监听器用 `@TransactionalEventListener(AFTER_COMMIT)`。」磁盘是 `@DsTxEventListener`。动态数据源事务和 Spring 事务不是同一套钩子。

**反例 7：** 「`getInfo` 要 `system:ossConfig:query`。」源码是 list。

**反例 8：** 「改配置是 PUT，删配置是 DELETE，和字典一样。」本 Controller 写窗全是 POST。单测扫过所有方法的 Put/Delete 注解都是 null。

**反例 9：** 「这是 layered，切换应该写 UseCase。」登记表 classic；禁止为切换单开 UseCase。

**反例 10：** 「`init` 失败还能先切一把再启动。」`init` 在 `ApplicationRunner` 里，默认个数不对直接抛，进程起不来。运行期的 changeStatus 才能修 0 个或 2 个默认（0 个时切一行会造出第一个 Y）。

**反例 11：** 「body 带 `accessPolicy=0` 就能把公开行升成默认。」切换读的是 `selectByIdForUpdate` 的行，不把 body 合进实体。

**反例 12：** 「正门 Redis 和 `sys_oss_config` 是同一组 Spring Cache。」正门是 `RedisUtils` 直写的独立键；图纸墙才是 `CacheNames.SYS_OSS_CONFIG`。

**反例 13：** 「内置四行不能被设为默认。」不能**删**。切换不看 `SYSTEM_DATA_IDS`。种子默认本来就是 `minio` 那行内置 id。

**反例 14：** 「矩阵没有 export/refresh 就是漏了。」磁盘就六扇。不要按 ConfigController 的九扇去补。

**边界：**

- 桶权限：运行时只认 `0`/`2`。基座种子插入过非 0 的 `access_policy`，同一份 `50` 脚本后段 `NAMEWTA-OSS-ACCESS-DML-001` 把全部非 `0` 打回 PRIVATE。`AccessPolicy` 枚举没有 type=1。
- `status` 在 add/edit 的 BO 上是 `@Pattern("[YN]")`；切换窗不跑这组校验。
- 编辑省略 `secretKey`：`normalizeUpdatedConfig` 回填旧密钥。切换不碰密钥。
- 被对象引用的配置：普通编辑不能改 `configKey` / `bucketName` / `accessPolicy`；改 endpoint 可以。切换不改这些列。
- `config_key` 应用层查重，DDL **没有**唯一索引。切换不查重。
- 物理删除：`BaseEntity` 无 `del_flag`。
- 行锁范围：FOR UPDATE 只锁**目标行**。两个事务同时把两个不同 id 设为正门，可能互相等对方那一行的 UPDATE，存在死锁窗口；失败应整段回滚，最终仍不该留下两个 `Y`。
- 正门 Redis 键前缀 `global:`，和租户业务键分开。
- `SYS_OSS_CONFIG` 光板名字 → TTL=0、本地 Caffeine 30s。切换主链不 clear 这组。
- readiness：默认行被标 `DEFAULT`；另有上传策略名牌、已存 `sys_oss.service`、以及其它 `OssRequiredConfigContributor`。换正门会改「谁被 DEFAULT 点名」，不会取消「这间仓库还有对象」的必检。
- `toAjax(cleared+activated)`：业务失败走异常，不靠返回 0。

## 变式与迁移

- **变式 A：要把正门从 A 换到 B。** 对 **B** 发 `changeStatus`。不要对 A 发 `status=N`。验收：表一个 Y、Redis 正门=`B`、未指定上传的 `clientId=B`、A 上旧对象仍可读。
- **变式 B：SQL 把 `status` 改乱了。** 没有事件。若还能登录管理端，对正确那一行 `changeStatus`（修理扳手）。若已经两个 Y 导致进程起不来，先把表修回恰好一个 Y 再启动，`init()` 会重写 Redis 和图纸墙。
- **变式 C：改的是 endpoint / 密钥，不是正门。** 走 edit。save 事件才会换 JSON 并 `OssFactory.remove` 该名牌。只点切换，口袋里的旧钥匙仍按旧图纸 `verifyConfig` 通过就会继续用。
- **变式 D：退役一间默认仓库。** 先切到另一间 PRIVATE → 确认没有 `sys_oss.service=旧名牌`（或走 L-028 迁走）→ 再 remove。正门未切时删除会被挡。内置四行永远删不掉。
- **变式 E：新环境空表。** 第一行必须带 `Y` 才能过「已有恰好一个默认」。之后才能加 `N`。公开行只能 `N`。
- **变式 F：集群多节点。** 正门指针在 Redis，他节点 `instance()` 现读。`CLIENT_CACHE` 各进程各有一份，切换不广播关闭。save 才靠 Spring Cache 墙同步图纸。不要把 L-021 的 `refreshCache` 扫帚搬到本窗。
- **变式 G：前端开关灰掉的公开行。** 产品意图和后端闸一致。若有人用 curl 硬切公开行，应拿到「默认OSS配置必须为PRIVATE」，表不变。
- **迁移口诀：** 先问这枪是六扇里哪一扇 → 再问 body 的 status 有没有被读 → 再问事件是 save 还是 useDefault → 图纸墙/钥匙串只在 save/remove 动 → 正门 Redis 在 useDefault 动 → 已存对象名牌永远不随切换搬家。跳步就会把用户启停、字典刷新、对象迁移说成同一按钮。

## 常见误区

1. **「status 是启停。」** 是「是否唯一正门」。
2. **「changeStatus 读 status 字段。」** 只读 `ossConfigId`。
3. **「取消默认 = 清掉 Y。」** 会再写一个 Y。
4. **「切换会刷新 OSS 配置缓存。」** 不 put、不 evict、不 clear。
5. **「切换会重建 S3 客户端。」** 不 `OssFactory.remove`。
6. **「默认上传和旧文件都会去新桶。」** 只有 `OssFactory.instance()` 无参这条跟正门；旧 `service` 跟名牌。
7. **「公开桶当默认，前端拦住就够。」** 后端还有硬闸。
8. **「getInfo 走 query 权限。」** 走 list。
9. **「写操作是 PUT/DELETE。」** 全是 POST。
10. **「事件回滚也会改 Redis。」** `@DsTxEventListener` 提交后才跑。
11. **「classic 所以可以加 OssConfigSwitchUseCase。」** 登记表禁止混模式。
12. **「内置行只读。」** 只禁删除。
13. **「list 旧是因为 Redis 正门没换。」** list 不读正门。
14. **「把 L-021 的 refreshCache 打到 OSS 配置就能换正门。」** 本 Controller 没有那扇窗。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 打开 `SysOssConfigController`。把六对 mapping 抄成一张表：路径、动词、Java 名、权限串、有没有 `@RepeatSubmit` / `@Log` / `@Validated` 分组。圈出 `getInfo` 的权限是 list，以及 `changeStatus` 没有校验分组。
2. 用手指划 `changeStatus` → `updateOssConfigStatus` → `selectByIdForUpdate` → `clearOtherDefaultStatuses` → `updateById` → `OssConfigChangeEvent.useDefault` → 监听器 `if (event.defaultConfig()) return`。在 return 旁边写：Redis 写了什么，JSON 墙没写什么，工厂没关什么。
3. 对照 `OssConfigPage.vue` 的「取消默认」文案和 `resource-service.ts` 的 body 字段。再说一遍：服务端为什么不需要 `status` 也能完成「换正门」。
4. 打开 `SysOssConfigMapper` 三条切换相关 SQL、`OssConstant.DEFAULT_CONFIG_KEY`、`CacheNames.SYS_OSS_CONFIG`、`OssFactory.instance()` 无参/有参。用一句话连接：正门指针、图纸墙、已存对象名牌是三只柜子。
5. 打开 `insertByBo` / `updateByBo` 里「当前默认不能改 N」和 `deleteWithValidByIds` 里「请先切换」。写一句：退役默认仓库的合法顺序。

## 总结、词汇表与下一步

- **六扇窗。** 前缀 `/resource/oss/config`。GET 两扇读表；POST 四扇变更。classic：Controller → ServiceImpl → 空 XML Mapper + 注解 SQL。
- **一只抽屉。** `sys_oss_config`。`status='Y'` 必须恰好一行；那一行 `accessPolicy` 必须 `0`。
- **三只下游柜子。** Redis 正门指针；Spring Cache 图纸墙；进程内 `OssFactory` 钥匙串。切换主链只动第一只，外加本节点 readiness。
- **矩阵 B。** `updateOssConfigStatus` 行锁 → 清其它 Y → 本行 Y → `useDefault`。忽略 body 的 status。公开行写库前失败。update 失败则整段回滚。
- **旁路。** add/edit 带 Y 也能换正门，但会走 save。普通编辑不能把当前正门改 N。删除前必须已经不是正门。
- **不搬家。** `sys_oss.service`、直传策略名牌、旧客户端实例，都不随切换自动清。
- **格子按磁盘全表：** 不要补 export/refresh。不要把切换说成启停或 reload。

词汇表：SysOssConfig / classic / `configKey` / `ossConfigId` / `status` / `accessPolicy` / PRIVATE / PUBLIC_READ / `changeStatus` / `updateOssConfigStatus` / `DEFAULT_CONFIG_KEY` / `SYS_OSS_CONFIG` / `OssFactory` / `OssConfigChangeEvent.useDefault` / `@DsTxEventListener` / `@DSTransactional` / `SELECT FOR UPDATE` / `SYSTEM_DATA_IDS` / readiness / `@RepeatSubmit` / `@JsonIgnore`。

下一步：L-026 走进 `SysOssController` 列表、下载 URL、删除——那些对象身上的 `service` 名牌，就是本课说「不会随正门搬家」的那一列。L-027 才是直传票据按策略选名牌。L-028 才是真的把对象迁到另一间仓库。L-029 才把浏览器 adapter 接到这些 HTTP。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 房间内公开入口；本课 Controller 同模块 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记为 classic；禁止与 layered 混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | `sys_oss_config` 列；种子五行；内置 id；`NAMEWTA-OSS-ACCESS-DML-001` 默认个数前置与 access_policy 打回 0 | `create table sys_oss_config`；OSS 种子与 ACCESS DML 块 | 2026-09-16 |
| S-L025-01 | `SysOssConfigController.java` | 六扇映射、权限、POST 写窗、`changeStatus` 无校验分组、Log 不存 add/edit/changeStatus 请求体 | 类与各方法 | 2026-09-16 |
| S-L025-02 | `ISysOssConfigService.java`、`SysOssConfigServiceImpl.java` | classic 持有 Mapper；`updateOssConfigStatus` 忽略 status、PRIVATE 闸、清其它 Y、失败回滚、只发 useDefault；add/edit/delete 旁路闸 | 接口 `updateOssConfigStatus` 与实现 | 2026-09-16 |
| S-L025-03 | `OssConfigChangeEvent.java`、`OssConfigChangeListener.java` | save/remove/useDefault 工厂方法；`defaultConfig` 早退只写 Redis；finally readiness；`@DsTxEventListener` | 事件 record 与 listener 方法 | 2026-09-16 |
| S-L025-04 | `SysOssConfigMapper.java`、空 `SysOssConfigMapper.xml`、`SysOssConfig.java` / Bo / Vo | FOR UPDATE、clear SQL、引用计数、默认计数；VO 密钥 `@JsonIgnore`；BO 分组校验不管切换窗 | Mapper 注解 SQL、领域类型 | 2026-09-16 |
| S-L025-05 | `OssConstant.java`、`CacheNames.java`、`OssFactory.java`、`OssProperties.java`、`RedisUtils.java` | 正门键 `global:sys_oss:default_config`；图纸墙名；无参 instance 读正门；Properties 无 status 字段；setCacheObject 无 TTL | 常量、工厂、Redis 工具 | 2026-09-16 |
| S-L025-06 | `OssConfigHttpContractUnitTest.java`、`OssConfigGovernanceUnitTest.java` | 查询 GET、变更 POST+Log；公开不能当默认；切换失败须 abort；四段写路径都有 `@DSTransactional`；监听器用 Ds 钩子 | 测试方法名 | 2026-09-16 |
| S-L025-07 | `SystemApplicationRunner.java`、`OssStorageReadinessService.java`、`SysOssServiceImpl.upload`、`AccessPolicy.java` | 启动 init+readiness；默认行标 DEFAULT；未指定上传走 `OssFactory.instance()`；枚举只有 0 和 2 | runner、readiness `requiredConfigs`、upload、enum | 2026-09-16 |
| S-L025-08 | `frontend/packages/domains/system/src/resource-service.ts`、`web-domains/system/src/oss-config/OssConfigPage.vue`、`frontend/e2e/oss-config-access-policy.spec.ts` | POST changeStatus body 带 status/configKey；「取消默认」文案；公开开关 disabled；e2e 认 POST | `createOssConfigService`、`handleStatusChange`、e2e 请求表 | 2026-09-16 |
