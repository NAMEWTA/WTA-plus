---
lesson_id: L-021
objective_ids: [OBJ-21]
claimed_cells: [A:SysConfigController.*, A:SysDictTypeController.*, A:SysDictDataController.*]
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: method-table-on-disk
    minutes: 10
  - segment: cache-refresh-failure-paths
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-008, S-010, S-L021-01, S-L021-02, S-L021-03, S-L021-04, S-L021-05, S-L021-06, S-L021-07, S-L021-08]
---

# Lesson 021：墙上的便签——配置、字典和刷新失败

## 学完你能做什么

打开 `wta-system` 里**三份** Controller，你能**口述二十四扇公开窗**，并单独把 OBJ-21 点名的那句说完：**缓存刷新失败时，库已经改了，热路径还在吃旧纸条（或一张空白复印件）。** 不要把「参数」「字典类型」「字典数据」说成一个 CRUD，也不要把 `refreshCache` 说成「从数据库重新灌进 Redis」。

本课认矩阵 (a) 三行，方法名以**磁盘**为准。矩阵行名把三份类的 `add` / `edit` 写窄了——源码有这两扇，本课 `*` **认全表**：

1. **`SysConfigController`**（`@RequestMapping("/system/config")`）：`list` / `export` / `getInfo` / `getConfigKey` / `add` / `edit` / `updateByKey` / `remove` / `refreshCache`。
2. **`SysDictTypeController`**（`@RequestMapping("/system/dict/type")`）：`list` / `export` / `getInfo` / `add` / `edit` / `remove` / `refreshCache` / `optionselect`。
3. **`SysDictDataController`**（`@RequestMapping("/system/dict/data")`）：`list` / `export` / `getInfo` / `dictType` / `add` / `edit` / `remove`。**没有**刷新窗。

OBJ-21 特别点名的两扇刷新门：

- `DELETE /system/config/refreshCache` → `resetConfigCache()` → `CacheUtils.clear("sys_config")`
- `DELETE /system/dict/type/refreshCache` → `resetDictCache()` → 先 `clear("sys_dict")` 再 `clear("sys_dict_type")`

模块模式是 **classic**：`Controller → ISys*Service → *ServiceImpl → Mapper`。登记表把 `wta-modules/wta-system` 标 classic。磁盘上**没有** UseCase、**没有** DAO。三份 Mapper XML 都是空壳，查询走 `QueryBuilder` + `BaseMapperPlus`。

本课不宣称你会拆 `CacheController` 监控柜（L-023）、`createSystemService.resources` 全表（L-019）、密码策略解析全文（L-012 已认 `PasswordPolicyService` 吃 `selectConfigByKey`）、或 `DictPatternValidator` 的注解用法。邻居 SPI（`ConfigService` / `DictService`）只作为热路径的**读者**，用来解释「刷新失败为什么全楼都看见旧值」。

## 先把宏观地图放在桌上

L-003 已经把 `wta-system` 钉在 classic 列。L-012 说过：改 `sys.user.passwordPolicy` 的 JSON 若既不 `@CachePut` 也不 `resetConfigCache`，策略服务仍吃 `@Cacheable(SYS_CONFIG)` 的旧菜谱。本课走进**贴便签的柜台**，把那张无 TTL 的复印件从哪扇窗写、哪扇窗撕，说清楚。

2026-09-16 工作树：三个 Java 类都在 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/system/`。都 `extends BaseController`。配置只注入 `ISysConfigService`。字典类型只注入 `ISysDictTypeService`。字典数据注入 **两份**：`ISysDictDataService` 管 CRUD，`ISysDictTypeService` 管热路径 `selectDictDataByType`。

```text
管理员浏览器 / 已登录调用方
    │
    ├─ /system/config/*          SysConfigController     九扇（含 refreshCache）
    ├─ /system/dict/type/*       SysDictTypeController   八扇（含 refreshCache + Lock4j）
    └─ /system/dict/data/*       SysDictDataController   七扇（无刷新门）
                │
                ├─ ISysConfigService / SysConfigServiceImpl
                │     也 implements wta-api 的 ConfigService
                ├─ ISysDictTypeService / SysDictTypeServiceImpl
                │     也 implements common-core 的 DictService
                └─ ISysDictDataService / SysDictDataServiceImpl
                │
                ├─ SysConfigMapper / SysDictTypeMapper / SysDictDataMapper
                │     XML 空；CRUD 在 BaseMapperPlus
                v
        表 sys_config / sys_dict_type / sys_dict_data
                │
                v
        Spring Cache 名（无 #ttl → Redis Map 不过期；默认开本地 Caffeine）
            sys_config      键 = configKey     值 = String（键值）
            sys_dict        键 = dictType      值 = List<SysDictDataVo>
            sys_dict_type   键 = dictType      值 = SysDictTypeVo
```

**类比：** 仓库里有两只铁皮抽屉（参数一张表，字典类型+数据两张表）。大厅墙上贴着**复印件**（Redis）。每个柜台职员桌上还有一张 **30 秒便利贴**（Caffeine 一级缓存）。管理员改抽屉里的纸，柜台会尽量换墙上那张复印件。两把扫帚（refresh）不负责重新抄写抽屉，只负责**把墙上的复印件撕光**，并喊别的楼层把便利贴扔掉。下一个人来问，才再从抽屉抄一张。

**类比失效处：**

1. 三份 Controller 不是三个模块。都在 `wta-system`，都是 classic。
2. 字典热路径不在 Data Service：`GET /system/dict/data/type/{dictType}` 打电话给 **Type** 服务的 `@Cacheable`。
3. 刷新不是「reload」。方法名叫 `reset*Cache` / `refreshCache`，实现是 `CacheUtils.clear`。
4. 管理列表 `list` / 按主键 `getInfo` **不走**这三组缓存。所以表格里看见新值、热路径仍吐旧值，可以同时成立。
5. 数据窗没有扫帚。改完一条字典数据，靠 `@CachePut` 换 `sys_dict` 那一格；扫帚只挂在类型窗。

## 核心概念与机制

### 直觉讲解

先记住三种纸条，再背路径：

- **参数便签 `configKey → configValue`。** 热读只缓存**字符串**。缺行时缓存的是 `""`，不是「没有」。空白复印件会挡住以后才插入的真纸条。
- **字典分类标签 `dictType`。** 类型表有库级 `unique (dict_type)`。类型窗的刷新会清 **两** 组缓存：数据列表 + 类型 VO。
- **字典贴纸。** 同一 `dictType` 下多行，按 `dictSort` 排队。热读缓存的是**整列贴纸**，不是单行。

再记住两把扫帚和一把锁：

- 配置扫帚：`system:config:remove` + `@Log CLEAN`。**没有** `@Lock4j`。
- 字典扫帚：`system:dict:remove` + `@Log CLEAN` + **`@Lock4j`**。第二个人同时扫，拿不到锁就 503「业务处理中，请稍后再试...」。
- 数据窗只有增删改，扫帚借类型窗的。

「改了为什么还是旧的」先问四句话，不要先怪前端：

1. 改的是管理详情（按 id）还是热路径（按 key / 按 type）？
2. 这次写入有没有 `@CachePut` / `CacheUtils.evict`，还是只改了 SQL？
3. 刷新 HTTP 有没有真的 200？集群作废有没有在 2 秒内收到回执？
4. 别的进程桌上那张 30 秒便利贴过期了没有？

### 精确定义与 English term

| 中文口诀 | English term | 磁盘上的东西 |
| --- | --- | --- |
| 参数配置 | system config | 实体 `SysConfig`，表 `sys_config` |
| 参数键 | `configKey` | 热路径和缓存键；**不是**主键 `configId` |
| 参数值 | `configValue` | `@Cacheable` / `@CachePut` 真正放入缓存的 String |
| 系统内置 | built-in flag | `configType`；`SystemConstants.YES` = `"Y"` 不能删 |
| 字典类型 | dict type | 实体 `SysDictType`，表 `sys_dict_type` |
| 字典数据 | dict data | 实体 `SysDictData`，表 `sys_dict_data` |
| 字典类型字符串 | `dictType` | 小写字母开头 `[a-z][a-z0-9_]*`；三组缓存都用它当键（配置除外） |
| 字典编码 | `dictCode` | 数据主键；getInfo / remove 的路径变量 |
| 热路径 | hot read | `selectConfigByKey` / `selectDictDataByType` / `selectDictTypeByType` |
| 按键改值 | update by key | `PUT /system/config/updateByKey`；**无** `@Validated`、**无**唯一性闸 |
| 刷新缓存 | refresh / reset cache | 只 `CacheUtils.clear`；不预加载 |
| 写入回填缓存 | `@CachePut` | 成功返回值写进对应 cacheNames |
| 单键驱逐 | `@CacheEvict` / `CacheUtils.evict` | 配置改名、删除、按键更新的旧键 |
| 缓存组名 | cache name | `CacheNames.SYS_CONFIG` = `"sys_config"`（无 TTL） |
| 本地一级 | Caffeine | 写入/访问后 30s 过期；全应用共用 maxSize 1000 |
| 集群作废 | cluster invalidation | `ClusterCacheInvalidationCoordinator`；2s 等回执 |
| 经典分层 | classic | Controller → ServiceImpl → Mapper |
| 跨模块读配置 | `ConfigService` | `wta-api`；实现类就是 `SysConfigServiceImpl` |
| 跨模块读字典 | `DictService` | `wta-common-core`；实现类就是 `SysDictTypeServiceImpl` |

权限串不要混：配置是 `system:config:list|query|add|edit|export|remove`；字典类型和数据**共用** `system:dict:list|query|add|edit|export|remove`。刷新两扇都挂 **remove**，不是 edit。前端配置页 / 字典页的刷新按钮也是 `v-hasPermi` 同一串 remove。

三扇**没有** `@SaCheckPermission` 的窗（仍要登录，没有 `@SaIgnore`）：

- `GET /system/config/configKey/{configKey}`
- `GET /system/dict/data/type/{dictType}`
- `GET /system/dict/type/optionselect`

口试不要把它们说成匿名接口，也不要说成「和 list 一样要 dict:list」。

### 机制/因果链

#### 1. 三扇窗在磁盘上的真实映射

文件：

- `.../controller/system/SysConfigController.java`
- `.../controller/system/SysDictTypeController.java`
- `.../controller/system/SysDictDataController.java`

类注解三边都是 `@Validated` `@RequiredArgsConstructor` `@RestController`。

**配置九扇（矩阵第一行 + 磁盘补全的 add/edit）：**

| HTTP | 动词 | Java | 权限 | 写库？ | 缓存动作 / 失败 |
| --- | --- | --- | --- | --- | --- |
| `/system/config/list` | GET | `list` | `system:config:list` | 否 | 不走缓存；`selectPageConfigList` |
| `/system/config/export` | POST | `export` | `system:config:export` | 否 | `@Log EXPORT`；`ExcelBuilder` 用 `SysConfigVo` |
| `/system/config/{configId}` | GET | `getInfo` | `system:config:query` | 否 | 主键；不走 `SYS_CONFIG` |
| `/system/config/configKey/{configKey}` | GET | `getConfigKey` | **无权限注解** | 否 | 热路径；`R.data`；`@Cacheable` |
| `/system/config` | POST | `add` | `system:config:add` | 是 | 键重复 → `R.fail("新增参数'…'失败，参数键名已存在")`；成功 `@CachePut` |
| `/system/config` | PUT | `edit` | `system:config:edit` | 是 | 同上唯一性文案换「修改」；改键会 evict 旧键再 `@CachePut` 新键 |
| `/system/config/updateByKey` | PUT | `updateByKey` | `system:config:edit` | 是 | **跳过** Controller 唯一性闸和 `@Validated`；失败 `ServiceException("操作失败")` |
| `/system/config/{configIds}` | DELETE | `remove` | `system:config:remove` | 是（物理删） | 内置 Y 抛「内置参数【{}】不能删除」；先 evict 再 `deleteByIds` |
| `/system/config/refreshCache` | DELETE | `refreshCache` | `system:config:remove` | 否 | 只 clear；无 Lock4j；无 RepeatSubmit |

`add` / `edit` / `updateByKey` 带 `@RepeatSubmit()`（默认 5000ms）。`refreshCache` 不带。`list` / `getInfo` / `getConfigKey` / `export` / `remove` 不带。

**字典类型八扇：**

| HTTP | 动词 | Java | 权限 | 写库？ | 缓存动作 / 失败 |
| --- | --- | --- | --- | --- | --- |
| `/system/dict/type/list` | GET | `list` | `system:dict:list` | 否 | 不走缓存 |
| `/system/dict/type/export` | POST | `export` | `system:dict:export` | 否 | `@Log EXPORT`；sheet「字典类型」 |
| `/system/dict/type/{dictId}` | GET | `getInfo` | `system:dict:query` | 否 | 主键 `dictId`，不是 `dictType` 字符串 |
| `/system/dict/type` | POST | `add` | `system:dict:add` | 是 | 类型重复 → `R.fail("新增字典'…'失败，字典类型已存在")`；`@CachePut SYS_DICT` 空列表防穿透 |
| `/system/dict/type` | PUT | `edit` | `system:dict:edit` | 是 | `@Transactional`；改类型字符串会改写所有数据行的 `dict_type`；evict 旧键 |
| `/system/dict/type/{dictIds}` | DELETE | `remove` | `system:dict:remove` | 是（物理删） | 仍有数据 → 「{}已分配,不能删除」；先删库再 evict 两组缓存 |
| `/system/dict/type/refreshCache` | DELETE | `refreshCache` | `system:dict:remove` | 否 | `@Lock4j`；clear `sys_dict` **然后** `sys_dict_type` |
| `/system/dict/type/optionselect` | GET | `optionselect` | **无权限注解** | 否 | `selectDictTypeAll`；不走缓存 |

**字典数据七扇（无 refreshCache）：**

| HTTP | 动词 | Java | 权限 | 写库？ | 缓存动作 / 失败 |
| --- | --- | --- | --- | --- | --- |
| `/system/dict/data/list` | GET | `list` | `system:dict:list` | 否 | 不走缓存；条件含 sort / label / type |
| `/system/dict/data/export` | POST | `export` | `system:dict:export` | 否 | sheet「字典数据」 |
| `/system/dict/data/{dictCode}` | GET | `getInfo` | `system:dict:query` | 否 | 主键；不走 `SYS_DICT` |
| `/system/dict/data/type/{dictType}` | GET | `dictType` | **无权限注解** | 否 | **Type 服务** `@Cacheable`；`null` 才换成 `new ArrayList<>()`（当前实现返回 emptyList，这枝几乎不走） |
| `/system/dict/data` | POST | `add` | `system:dict:add` | 是 | 同类型下值重复 → `R.fail("新增字典数据'…'失败，字典键值已存在")`；`@CachePut SYS_DICT` 整列 |
| `/system/dict/data` | PUT | `edit` | `system:dict:edit` | 是 | 同上；`@CachePut` 键是 **新** `bo.dictType` |
| `/system/dict/data/{dictCodes}` | DELETE | `remove` | `system:dict:remove` | 是（物理删） | **先删库再 evict**；进程死在中间 → 墙上仍是旧列 |

`add` / `edit` 带 `@RepeatSubmit()`。`DELETE /refreshCache` 是字面路径，Spring 不会把它当成 `/{dictIds}` 的 `Long[]`。

#### 2. classic 里屋：配置怎么写进 `sys_config` 和墙上

`SysConfigServiceImpl` 实现 `ISysConfigService` **和** `org.namewta.system.api.ConfigService`，字段注入 `SysConfigMapper` + `PasswordPolicyConfigParser`。这就是 classic：ServiceImpl 直接持有 Mapper。

热读因果链：

1. `GET /configKey/{configKey}` 或 `ConfigService.getConfigValue`（内部 `SpringUtils.getAopProxy(this).selectConfigByKey`，为了打到缓存代理）。
2. `@Cacheable(cacheNames = "sys_config", key = "#configKey")`。
3. 未命中才 `configMapper.lambda().eq(configKey).one()`。
4. 没有行 → `StringUtils.EMPTY`，**仍然放入缓存**。

写入：

1. Controller `add`/`edit` 先 `checkConfigKeyUnique`（同键且主键不同 → 视为存在）。这闸和 insert **不是**同一事务。
2. `insertConfig` / `updateConfig` 先 `validatePasswordPolicy`：仅当键等于 `PasswordPolicy.CONFIG_KEY`（`sys.user.passwordPolicy`）才 `parse`。坏 JSON / 超 500 字 / 缺四类字符 → `ServiceException("PASSWORD_POLICY_UNAVAILABLE")`，**不写库、不 CachePut**。日志只记异常类名，不回显配置正文。
3. `@CachePut(SYS_CONFIG, key="#bo.configKey")` 缓存的是方法**返回的字符串**（`configValue`），和热读类型一致。
4. insert 行数不大于 0 → `ServiceException("操作失败")`，CachePut 不发生。
5. update 若带 `configId`：读旧行，键变了就 `CacheUtils.evict` 旧键，再 `updateById`。若不带 id（`updateByKey` 这条）：**先 evict 当前键**，再按键 `lambda update`。0 行同样抛「操作失败」。此时键已经从墙上撕掉；下一次热读会把「抽屉里没有」复印成 `""`。
6. 删除：遍历行，`configType=="Y"` 立刻抛；否则 evict 该键。全部检查完才 `deleteByIds`。基座没有 `del_flag`，这是物理删。DDL **没有** `config_key` 唯一约束，并发双 insert 可能两行同键，随后 `.one()` 会炸。

`resetConfigCache` 只有一行：`CacheUtils.clear(CacheNames.SYS_CONFIG)`。不读表，不 `CachePut` 全量。

#### 3. 字典两张表、三组缓存、一扇热窗

`SysDictTypeServiceImpl` 持有 Type Mapper **和** Data Mapper，并 `implements DictService`。Excel 下拉、`@Translation`、`DictPatternValidator` 都经 `getDictLabel` / `getAllDictByDictType` 走进 **同一** `selectDictDataByType`（AOP 代理 + `@Cacheable SYS_DICT`）。

`selectDictDataByType`：Mapper `eq dictType orderBy dictSort` → 空则 `Collections.emptyList()`，不是 null。空列表也会被缓存（类型 `add` 还故意 `@CachePut` 空列表「防止缓存穿透」）。

`selectDictTypeByType`：另一组 `@Cacheable SYS_DICT_TYPE`。类型 `add` **不**预填这组；第一次 SPI `getDictType` 才写入。

类型 `updateDictType`（`@Transactional`）：

1. 把旧 `dictType` 下所有数据行的 `dict_type` 改成新字符串。
2. `updateById` 类型行。
3. 成功才 evict **旧** `SYS_DICT` 和 **旧** `SYS_DICT_TYPE`，并 `@CachePut SYS_DICT` 新键 = 新类型下的数据列表。
4. 旧 id 不存在 → `oldDict` 空指针（在写数据之前）。行数 0 → 回滚 + 「操作失败」。

类型删除：先对每个 id 查「是否还有数据」，有就抛，**一组里任何一个失败则整批不删**。通过后 `deleteByIds`，再 evict 两组。顺序是**先库后墙**。

数据 `insertDictData` / `updateDictData`：`@CachePut SYS_DICT` 键 = `bo.dictType`，值 = 该类型当前全列。数据 `deleteDictDataByIds`：先 `deleteByIds`，再按每行旧 `dictType` evict。**没有** `@Transactional`。

把一条数据的 `dictType` 改到另一个分类：新分类那一格被 CachePut 成新列；**旧分类那一格不会 evict**。墙上旧分类仍夹着这张已搬走的贴纸。这是数据窗没有扫帚时，必须借用类型窗 `refreshCache` 的原因之一。

#### 4. 刷新失败路径（OBJ-21 主链）

刷新 HTTP 自己几乎不写业务 if。失败发生在锁、权限、集群作废和「清完不等于看见新值」。

**A. 进门失败（缓存原封不动）**

1. 未登录 / 无 `system:config:remove` 或 `system:dict:remove`。有 edit 能 CachePut 单键，但不能扫整组。
2. 字典刷新抢锁失败：`Lock4jConfig` 抛 `ServiceException(..., 503)`，或 `LockFailureException` 被 `RedisExceptionHandler` 收成同一句「业务处理中，请稍后再试...」。配置刷新没有这把锁。
3. 错动词：刷新是 **DELETE**。前端 `resource-service` 的 `configs.refreshCache` / `dictTypes.refreshCache` 已写成 delete。GET/POST 打到这路径会 405。

**B. 扫帚扫到一半（墙已经空了一块）**

`CaffeineCacheDecorator.clear()` 的顺序是：先 `cache.clear()`（Redis Map），再 `invalidateClusterNamespace()`。协调器**先在本节点 apply**（撕本地便利贴），再发布消息，最多等 **2 秒**回执。

失败形态（单测钉在 `ClusterCacheInvalidationCoordinatorUnitTest`）：

- 没有订阅者 → `ClusterCacheInvalidationException("…without an active subscriber")`
- 回执超时 → `…acknowledgements timed out for namespace …`
- Redis 发布失败 → cause 包一层同一异常
- 本节点 handler 自己抛 → 同样包装

`ClusterCacheInvalidationException` **没有**专用 `@ExceptionHandler`。它是 `RuntimeException`，走进 `GlobalExceptionHandler` 的「未知异常」+ 8 位错误编号。管理员看见的不是「集群便利贴没撕干净」。

此时常见事实组合：

- **本节点**：Redis 已空，本地便利贴已撕。下一枪热读从抽屉重抄。刷新 HTTP 却是失败。
- **其他节点**：可能没收到消息。Caffeine 仍持有旧 `ValueWrapper`，**不回源 Redis**，直到写入后 30 秒过期，或那条被 1000 容量挤掉。
- 字典刷新是**两次** `clear`。第一次 `sys_dict` 成功、第二次 `sys_dict_type` 抛：数据热路径已空，类型 VO 热路径仍可能旧。

**C. 刷新「成功」但仍像失败（名字骗人）**

1. 实现只撕复印件，不预热。下一枪才读库。并发读在 clear 与下一次 put 之间，各自 Cacheable，一般最终一致，不是「已经灌好全表」。
2. 抽屉里的值本来就是错的（SQL 改坏了密码策略 JSON）。刷新只会让热路径更快吃到坏 JSON。写入闸只挡 HTTP insert/update。
3. 基座 DML 在 `sys.user.passwordPolicy` 的 remark 写明：「保存后必须刷新 sys_config 集群缓存」。那是给**绕过 HTTP 的 SQL** 看的。走 `edit`/`updateByKey` 且 parse 通过时，`@CachePut` 已经换键值；仍可能要刷新，是为了撕掉**别的节点桌上**那张 30 秒便利贴——如果集群作废失败，就会落到 B。
4. 管理 `list` 从来不读墙。刷新成功与否，表格都可以是新的。要用 `GET /configKey/…` 或 `GET /dict/data/type/…` 验收热路径。
5. 浏览器还有第三只柜子：admin-web `dictCache.clean()`。字典页刷新按钮在 HTTP 成功后会清 Pinia；配置页刷新**不会**清字典柜。本课点到为止，工厂全表仍归 L-019。

**D. 不点刷新、写入链自己留下的坑**

这些不是 refresh 方法抛错，但口试要能说「为什么最后还是要扫一次」：

1. 缺键被热读过 → 墙上是 `""` / 空列表。之后用 SQL 补行，热路径仍空白，直到 clear 或一次成功的 CachePut。
2. `updateByKey` 打到不存在的键：先 evict，0 行抛「操作失败」，下一次热读再把空白缓存起来。
3. 数据删除先库后墙；数据改分类只更新新键。
4. 类型新增 CachePut 的是空 `SYS_DICT`，不填 `SYS_DICT_TYPE`。
5. 配置 Controller 的唯一性闸与 insert 非原子；表上无 `config_key` 唯一索引。

### 图、表或文本图

**图 1. 三扇窗、两只抽屉、三组墙贴**

```text
 /system/config                /system/dict/type             /system/dict/data
 九扇（含 DELETE refresh）      八扇（含 DELETE refresh+锁）    七扇（无 refresh）
        │                              │                           │
        v                              v                           v
 ISysConfigService              ISysDictTypeService          ISysDictDataService
  + ConfigService                + DictService                只 classic CRUD
        │                              │                           │
        v                              v                           v
   sys_config                   sys_dict_type ──唯一 dict_type── sys_dict_data
        │                              │                           │
        v                              └──────────┬────────────────┘
   Redis Map sys_config                           │
   键=configKey 值=String              ┌──────────┴──────────┐
   （缺行也缓存 ""）                    v                     v
                                 Redis Map sys_dict     Redis Map sys_dict_type
                                 键=dictType            键=dictType
                                 值=List<DataVo>        值=TypeVo
                                        │
                                        v
                               每进程 Caffeine 便利贴（30s）
                               集群作废要 2s 回执
```

- **alt：** 配置、字典类型、字典数据三扇管理窗分别写入两只抽屉；热路径读的是三组 Redis Map 加每进程 30 秒本地缓存。
- **caption：** 图 1——OBJ-21 的空间关系。刷新门只画在配置窗和类型窗；数据热读箭头指向 Type 服务的 `sys_dict`。
- **文字等价物：** 管理员改参数走 `/system/config`，改字典分类走 `/system/dict/type`，改某类下的条目走 `/system/dict/data`。热读配置按键，热读字典按类型字符串。墙上三组名字分别是 `sys_config`、`sys_dict`、`sys_dict_type`，都没有 `#30d` 那种 TTL 后缀。
- **图的边界：** 图上没有 `CacheController` 的 Redis 浏览器，没有密码策略 Parser 的字段表，没有 Pinia `dictCache` 的内部 map。PUT/DELETE 是真实动词，不要画成「全部 POST」。

**图 2. 刷新成功才往右；失败停在哪一层**

```text
[权限 remove] 无权限 / 未登录 → 停；墙不动
   v
[字典才有 Lock4j] 抢锁失败 → 503 业务处理中；墙不动
   v
[CacheUtils.clear]
   ├─ Redis Map 撕光
   ├─ 本节点 Caffeine 撕光
   └─ 发布集群作废，等 2s 回执
         │  无订阅 / 超时 / Redis 发布失败
         v  ClusterCacheInvalidationException
            → 未知异常[错误编号]
            本节点墙可能已空；他节点便利贴最多再活 30s
   v
[HTTP 200] 只表示「撕复印件」成功
   │  不表示已经从抽屉抄好新墙
   v
下一枪 GET /configKey 或 GET /dict/data/type
   未命中 → 读表 → 再贴墙
   表里没有 → 贴 "" 或 []
```

- **alt：** 刷新先过权限和字典锁，再清 Redis 与本地缓存并等待集群回执；失败时本节点与他节点可能不一致。
- **caption：** 图 2——刷新失败路径。扫帚不负责预热。
- **文字等价物：** 刷新不是 reload。HTTP 失败时 Redis 可能已经空了。他节点 30 秒内仍可能吐旧值。缺行会被缓存成空白，看起来像「刷新了还是没有」。
- **图的边界：** 单键 `@CachePut` / `evict` 不走这张全图的 clear。SQL 改库不出现在这张门上，除非有人再按扫帚。

### 正例、反例与边界

**正例 A：改参数并被热路径看见。** `PUT /system/config`，body 带 `configId` + 新 `configValue`。唯一性过了，`updateConfig` `@CachePut` 该 `configKey`。同节点立刻 `GET /system/config/configKey/{key}` 得到新字符串。`list` 本来就能看见，因为它不读墙。

**正例 B：扫配置墙。** 运维用 SQL 改了 `sys.user.passwordPolicy` 的 JSON（基座 remark 要求刷新）。`DELETE /system/config/refreshCache` 200。各节点便利贴被作废或 30 秒内过期。`PasswordPolicyService` 下一次 `selectConfigByKey` 从抽屉重抄。

**正例 C：新建字典类型。** `POST /system/dict/type`。`insertDictType` 成功后墙上 `sys_dict[{新类型}]=[]`。`GET /system/dict/data/type/{新类型}` 命中空列表，不会误把「没有分类」和「分类下没条目」搞混成 null（服务端已经 emptyList）。

**正例 D：字典刷新。** `DELETE /system/dict/type/refreshCache`。锁拿到，两组 Map 都 clear。字典页再 `runtime.dictCache.clean()`。之后 `GET /type/{dictType}` 重新从 `sys_dict_data` 按 sort 抄列。

**反例 1：** 「`refreshCache` 会把表全量载入 Redis。」只 clear。

**反例 2：** 「字典数据也有 refreshCache。」Controller 没有；前端 `dictData` 工厂也没有。扫帚在类型窗。

**反例 3：** 「`GET /dict/data/type/x` 打 DataService。」打 Type 服务的 `@Cacheable`。

**反例 4：** 「管理 getInfo 走缓存，所以改完列表还旧。」相反：getInfo/list 不走这层；旧的是热路径。

**反例 5：** 「缺键返回 null，所以不会污染缓存。」缺配置键返回 `""` 并缓存；缺字典数据返回 `[]` 并缓存。

**反例 6：** 「刷新失败 = 墙完全没动。」B 路径下本节点 Redis 往往已经空了。

**反例 7：** 「`updateByKey` 和 `edit` 同一套校验。」`updateByKey` 无 `@Validated`、无 unique。密码策略键仍会在 Service 里 parse。

**反例 8：** 「内置参数不能改、只能刷新。」不能**删**（`configType=Y`）。改值走 edit/updateByKey。种子 `sys.user.initPassword` / `sys.oss.previewListResource` / `sys.user.passwordPolicy` 都是 Y。

**反例 9：** 「有数据的字典类型会级联删数据。」会抛「已分配,不能删除」。要先删数据行。

**反例 10：** 「改一条数据的 dictType，两边缓存都会更新。」只 CachePut 新类型。

**反例 11：** 「`sys_config` 和 `sys_client` 一样 30 天过期。」`SYS_CLIENT` 才是 `"sys_client#30d"`。配置/字典名没有 `#`，Redis 侧不过期。

**反例 12：** 「这是 layered，刷新应该写在 UseCase。」登记表 classic；禁止为刷新单开 UseCase。

**反例 13：** 「`optionselect` 要 `system:dict:list`。」源码无权限注解。

**反例 14：** 「矩阵没有 add/edit 就是没有这两扇。」口试按磁盘 9+8+7。

**边界：**

- 字典类型正则：`RegexConstants.DICTIONARY_TYPE` = `^[a-z][a-z0-9_]*$`。
- 配置值 `@Size max=500`（BO）；密码策略 Parser 同样 500。`updateByKey` 不跑 BO 校验。
- 缓存名格式：`cacheNames#ttl#maxIdle#maxSize#local`。这三组只有光板名字 → TTL=0、本地=1。
- Caffeine：`expireAfterWrite(30s)`、`maximumSize(1000)`，**所有** Spring Cache 本地条目共用这一只。
- 集群作废消息只带 SHA-256 指纹，不带原始 key。
- `allowNullValues=true` 且 `transactionAware=true`。`updateDictType` 的 put/evict 等事务提交后才上墙；刷新方法本身无 `@Transactional`，clear 立即执行。
- 物理删除：三张表的 `BaseEntity` 没有 `delFlag`。
- `R.data` 与 `R.ok` 都是成功码「操作成功」；`getConfigKey` 用 `R.data` 只是写法。

## 变式与迁移

- **变式 A：改密码策略。** 走配置 `edit`（带 id）或 `updateByKey`。Parser 挡住非法 JSON。HTTP 成功已 `@CachePut`。若别的节点仍吐旧菜谱，再扫 `config/refreshCache`，不要去改 `sys.user.initPassword` 当策略（L-012 已退役该键的职责）。
- **变式 B：SQL / 迁移脚本改了 `sys_config`。** 没有 CachePut。必须 DELETE refresh。基座 PASSWORD-DSL 块的回滚注释第 4 步就是「刷新 sys_config、菜单和权限的 Redis/JVM 缓存」。
- **变式 C：把字典分类改名。** 走类型 `PUT /system/dict/type`，不要自己 UPDATE 两张表。Service 会改数据行并 evict 旧键。失败回滚。
- **变式 D：条目搬到另一个分类。** 数据 `edit` 换 `dictType` 后，对旧分类再扫一次类型刷新，或至少确认旧键被手撕。不要假设 CachePut 两边都做了。
- **变式 E：并发两人点字典刷新。** 第二人 503。墙可能正在被第一人撕。不要立刻再 SQL 灌数据当「失败重试」。
- **变式 F：只想改一个参数、怕扫全组。** 用 edit/updateByKey 的单键 CachePut。全组 refresh 会让所有键同时 miss，下一波热读打库。
- **变式 G：热路径像没改、表格已经改。** 先用无权限注解的 GET by key / by type 验证，不要用 list。再问是本节点还是他节点、便利贴 30 秒到了没、刷新 HTTP 是不是「未知异常」那枝。
- **迁移口诀：** 先问哪扇窗（config / dict type / dict data）→ 再问主键还是键名/类型字符串 → 再问这枪走不走墙 → 写入靠 CachePut/evict，扫帚只 clear → 字典扫帚带锁、配置扫帚不带 → HTTP 失败仍可能本节点已空、他节点 30 秒旧值。跳步就会把监控柜、SPI 翻译、前端 Pinia 说成同一按钮。

## 常见误区

1. **「刷新 = reload。」** 只 clear。
2. **「三份 Controller 三张缓存一一对应。」** 数据窗写入的是类型热路径那组 `sys_dict`；类型窗还多一组 `sys_dict_type`。
3. **「有 edit 就能刷新。」** 刷新挂 remove。
4. **「配置刷新也有 Lock4j。」** 只有字典类型那扇有。
5. **「缺键不会占缓存。」** 空白复印件正是穿透保护，也是 SQL 后补行看不见的原因。
6. **「`updateByKey` 比较安全，字段少。」** 少的是校验。
7. **「内置 Y 完全只读。」** 只禁删除。
8. **「字典删除是逻辑删。」** 这三张表物理删。
9. **「集群失败就是 Redis 没清。」** 常常是 Redis 清了、回执没齐。
10. **「未知异常编号和业务文案一样能当验收。」** 集群作废失败走通用 Runtime 处理器，文案不提 cache。
11. **「classic 所以可以加 ConfigRefreshUseCase。」** 登记表禁止混模式。
12. **「前端 refresh 成功等于全楼热路径已新。」** 还要过集群便利贴和浏览器 dictCache。

## 非评分暂停

打开磁盘，不要凭记忆，也不要交卷。下列动作用来把门钉住，没有标准答案栏。

1. 并排打开三份 Controller。把每一对 mapping 抄成一张表：路径、动词、Java 名、权限串、有没有 `@Lock4j` / `@RepeatSubmit` / `@Log`。圈出矩阵漏掉的 `add`/`edit`，以及只有类型窗才有的 refresh。
2. 用手指划 `SysConfigController.refreshCache` → `resetConfigCache` → `CacheUtils.clear` → `CaffeineCacheDecorator.clear` → `ClusterCacheInvalidationCoordinator.clear`。在「先 Redis、再本节点、再等回执」三步旁标注：哪一步抛了，墙上还剩什么。
3. 对照 `SysDictDataController.dictType` 与 `SysDictTypeServiceImpl.selectDictDataByType` 的注解。再说一遍：数据热窗为什么不注入 DataService 来读缓存。
4. 打开 `updateConfig` 的 id 分支和 else 分支、`deleteDictDataByIds` 的删/evict 顺序、`updateDictData` 的 CachePut 键。各写一句「什么时候必须借类型窗的扫帚」。
5. 打开空的三份 Mapper XML、实体 `@TableName`、登记表 classic 行、`CacheNames` 里 `SYS_CONFIG` 与 `SYS_CLIENT` 的字符串差别。用一句话连接：为什么本课不许发明 UseCase，以及为什么配置墙不会 30 天自己掉。

## 总结、词汇表与下一步

- **三扇窗。** 配置九扇，前缀 `/system/config`；类型八扇，前缀 `/system/dict/type`；数据七扇，前缀 `/system/dict/data`。classic：Controller → ServiceImpl → 空 XML Mapper。
- **两只抽屉。** `sys_config` 一张；字典 `sys_dict_type` + `sys_dict_data`。类型字符串库级唯一；配置键只在应用层查重。
- **三组墙。** `sys_config` 存字符串；`sys_dict` 存整列数据；`sys_dict_type` 存类型 VO。无 TTL。本地 Caffeine 30 秒。
- **热路径。** `getConfigKey` / `dictType` / SPI `getConfigValue` / `getDictLabel`。管理 list/getInfo 不走墙。
- **写入。** 成功才 `@CachePut`。配置改键 evict 旧键。类型改名搬数据并 evict 旧键。数据改分类只更新新键。数据删除先库后墙。
- **扫帚。** 两扇 DELETE refresh，权限都是 remove。只 clear。字典带 Lock4j。集群回执失败走未知异常；本节点可能已空，他节点便利贴最多 30 秒。
- **空白复印件。** 缺行缓存 `""` / `[]`。SQL 后补或 `updateByKey` 打空键，看起来像刷新失败。
- 格子按磁盘全表：不要把矩阵漏掉的 `add`/`edit` 背成不存在。不要把 refresh 说成 reload。

词汇表：SysConfig / SysDictType / SysDictData / classic / `configKey` / `dictType` / `dictCode` / `configType` / `@Cacheable` / `@CachePut` / `CacheUtils.clear` / `refreshCache` / `resetConfigCache` / `resetDictCache` / Caffeine / cluster invalidation / `@Lock4j` / `@RepeatSubmit` / `ConfigService` / `DictService` / `PASSWORD_POLICY_UNAVAILABLE`。

下一步：L-022 走进账号资料页 `SysProfileController` 与社交绑定列表。L-023 才打开监控柜 `CacheController`（那是看 Redis 的窗，不是本课两把扫帚）。L-019 才把 `resources.configs / dictTypes / dictData` 接到前端厨房。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` Controller / Service | system 房间内公开入口；本课三份 Controller 同模块 | 模块树 | 2026-09-16 |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | `wta-system` 登记为 classic；禁止与 layered 混用 | 当前登记表 | 2026-09-16 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 三张表列；`unique (dict_type)`；`sys_config` 无键唯一索引；种子 Y 参数；密码策略 remark 要求刷新集群缓存 | `create table sys_config/sys_dict_*`；`NAMEWTA-PASSWORD-DSL-001` | 2026-09-16 |
| S-L021-01 | `SysConfigController.java` | 九扇映射、权限、`updateByKey` 无校验、refresh 无锁、`getConfigKey` 无权限注解 | 类与各方法 | 2026-09-16 |
| S-L021-02 | `SysDictTypeController.java` | 八扇映射；refresh `@Lock4j`；optionselect 无权限；add/edit 唯一性文案 | 类与各方法 | 2026-09-16 |
| S-L021-03 | `SysDictDataController.java` | 七扇；`dictType` 委托 Type 服务；无 refresh；null 转空列表 | 类与各方法 | 2026-09-16 |
| S-L021-04 | `ISysConfigService.java`、`SysConfigServiceImpl.java`、`ConfigService.java`、`PasswordPolicyConfigParser.java`、`PasswordPolicy.java` | classic 持有 Mapper；`@Cacheable`/`@CachePut`；缺行缓存空串；内置不可删；按键更新先 evict；策略 parse 失败不写 | 接口与实现、`CONFIG_KEY` | 2026-09-16 |
| S-L021-05 | `ISysDictTypeService.java`、`ISysDictDataService.java`、`SysDictTypeServiceImpl.java`、`SysDictDataServiceImpl.java`、`DictService.java`、`SysDictDataMapper.java` | Type 实现 SPI；`selectDictDataByType` 缓存整列；类型改名搬数据；数据删先库后墙；改分类只 put 新键 | 接口与实现、默认 `selectDictDataByType` | 2026-09-16 |
| S-L021-06 | `CacheNames.java`、`CacheUtils.java`、`PlusSpringCacheManager.java`、`CaffeineCacheDecorator.java`、`CacheConfig.java`、`ClusterCacheInvalidationCoordinator.java`、`GlobalExceptionHandler.java`、`RedisExceptionHandler.java`、`Lock4jConfig.java`、`ClusterCacheInvalidationCoordinatorUnitTest.java` | 无 TTL 名；clear/evict；本地 30s/1000；2s 回执；未知异常；锁失败 503 | 缓存名常量、clear 顺序、单元测试方法名 | 2026-09-16 |
| S-L021-07 | `SysConfig.java` / `SysDictType.java` / `SysDictData.java` 及对应 Bo/Vo；三份空 Mapper XML；`RegexConstants.DICTIONARY_TYPE` | 表字段；BO 校验；类型正则；空 XML 不是没有持久化 | 领域类型与 mapper 资源 | 2026-09-16 |
| S-L021-08 | `frontend/packages/domains/system/src/resource-service.ts`、`web-domains/system/src/config/ConfigPage.vue`、`dict-type/DictPage.vue`、`admin-web/.../adminManifestRegistry.ts` | URL 与本课映射一致（格子仍归 L-019）；刷新 DELETE；字典页额外 `dictCache.clean` | `configs`/`dictTypes`/`dictData` 工厂与页面处理函数 | 2026-09-16 |
