---
lesson_id: L-083
objective_ids: [OBJ-83]
claimed_cells:
  - C:dao-mapper-ownership
  - B:BaseMapperPlus / QueryBuilder
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 5
  - segment: holders-and-ownership
    minutes: 9
  - segment: three-types-ladder-and-fill
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 7
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-007, S-008, S-L003-01, S-L083-01, S-L083-02, S-L083-03, S-L083-04, S-L083-05, S-L083-06, S-L083-07, S-L083-08, S-L083-09]
---

# Lesson 083：宏观仓库钥匙——只有 DAO 或 classic ServiceImpl 才许抱 `BaseMapperPlus` / `QueryBuilder`

## 学完你能做什么

打开 `backend/wta-common/wta-common-mybatis/`，你能**口述这间工具间的三件家具，以及谁才许把仓库钥匙揣进口袋**。口试名单就是矩阵 **(c)** 这一格加上 **(b)** 这一行，符号以磁盘为准：

1. **`C:dao-mapper-ownership`**：业务层里，**只有** layered 的 DAO，或 classic 房间的 Service（常见名字是 `*ServiceImpl`）才许持有 Mapper。UseCase、layered Service、Controller **不得** `import` 本模块 `mapper.`，也不得自己 new `QueryBuilder` / `LambdaQueryWrapper`。这是所有权，不是「谁碰巧写了 SQL」。
2. **`B:BaseMapperPlus / QueryBuilder`**：这两件东西**不是纯查询玩具**。`BaseMapperPlus` 能 `insertBatch` / `updateBatchById` / `lambda().delete()`；`QueryBuilder.lambdaJoin(...).list()` 会自己开火。口号就是矩阵那句：**不纯查询；DAO 或 classic ServiceImpl 才持有。**

OBJ-83 原文还要你能说明 **`BaseEntity`**。它不是第三张矩阵行，但是同一把宏观钥匙上的标签贴纸：项目自有实体继承它，拿到 `createDept / createBy / createTime / updateBy / updateTime` 五枚自动填充章。`version` 和 `delFlag` **不在**基类上，写在具体实体上。

口试还要能把「层」和「文件名」分开：

| 你可能以为的名字 | 磁盘事实（2026-09-17） |
| --- | --- |
| 只有文件名带 `ServiceImpl` 的类才许抱 Mapper | **层才算。** classic 的 `SystemOpenApiCredentialService`、`OssStorageReadinessService` 不叫 Impl，仍持 Mapper。layered 的任何 Service **都不许**，哪怕你给它改名叫 Impl |
| DAO 必须叫 `XxxDao`，不能叫 `XxxDaoImpl` | **可以。** `SsoAuthorizationCodeDaoImpl` 仍是 `@Repository` DAO，仍是唯一持 Mapper 的那一层 |
| 全仓 Mapper 都是 `BaseMapperPlus<Entity, Vo>` | **不是。** classic CRUD 常常 `<Entity, Vo>`；profile / sso 常把两个泛型都写成 Entity；notify 运行时几张表还停在裸 `BaseMapper<Entity>`；third 五张都是裸 `BaseMapper` |
| `QueryBuilder` 已经进了每一间 DAO | **没有。** 2026-09-17 全仓 `*Dao*.java` **零**处 `import QueryBuilder`。layered DAO 现用 `LambdaQueryWrapper` 或同名 XML。所有权规则是「谁许持有」，不是「今天谁用了这个类名」 |
| 新模块可以 `extends ServiceImpl` / `implements IService` | **全仓生产代码没有这两句。** DEC-009：DAO/Service 都不继承 MP 的 `IService` |
| `wta-job` / `wta-ai` 也有一把 Mapper 钥匙 | **没有 Mapper。** 它们仍是登记表 classic 行（L-003），本课不发明链条 |
| 门卫抱 Mapper 也算 classic 存量允许 | **不算。** `TestBatchController` 注释写「为了便于测试 直接引入mapper」——L-003 反例，棘轮拧松，不是模板 |

本课**不宣称**你会拆 `NotifyClient.send` / `NotifyDispatcher`（OBJ-82）、Sa-Token 写 Redis 失败算哪边（OBJ-84）、或 `10-cde-base-ddl.sql` / `@DS` 所有权（OBJ-85）。数据权限 SQL 模板怎么从角色 `data_scope` 拼出来，L-016 已经走查过；本课只认：**贴纸贴在真正执行 SQL 的 Mapper 方法上，切面靠 Spring 代理，所以 `lambda()` 要先把 Mapper 换成容器里的代理。**

## 先把宏观地图放在桌上

L-003 已经把登记表钉死：layered 三行（profile / notify / sso；third 磁盘已五层但表上没行）、classic 五行（system / workflow / job / demo / ai）。本课走进 **MyBatis 工具间**，看三件家具和两把合法钥匙。

把数据库想成一间仓库。门口的保安是 Controller。管家是 UseCase。厨师是 Service。仓库管理员是 DAO。旧房子（classic CRUD）厨房门常常直接通仓库，厨师自己拿钥匙——那把合法钥匙就叫 **classic ServiceImpl**。新房子规定厨师不许进仓库，只许仓库管理员拿钥匙——那把合法钥匙就叫 **DAO**。

**类比失效处：**

1. 钥匙不是 Mapper 接口自己。接口 `extends BaseMapperPlus` 是**叉车型号**；持有者是注入这台叉车的业务类。
2. Mapper 上的 `default` 方法可以当场 `QueryBuilder.lambdaJoin`（`SysUserMapper.selectUserExportList`）。那是叉车自己的按钮，仍算 SQL 面，不是 Controller 的借口。
3. `QueryBuilder.lambdaJoin(...).list(Vo.class)` **不需要**你再声明一个 Mapper 字段就会开火。口袋里只有 QueryBuilder、没有 Mapper，照样是在持有持久化枪。
4. WarmFlow 的 `FlowTaskMapper` 来自 `org.dromara.warm.flow.orm.mapper`，不是本课这台 `BaseMapperPlus`。workflow LiteFlow 节点（例如 `CompleteAutoPassComponent`）既注入上游 Mapper 又 `import QueryBuilder`——这是 classic 房间的存量泄漏，**新模块不许抄**。
5. `EsCrudController` 抱的是 `DocumentMapper`（Elasticsearch 演示），矩阵 deferred，不是本课 MyBatis 钥匙。

2026-09-17 工具间牌子：

```text
backend/wta-common/wta-common-mybatis/
  artifactId: wta-common-mybatis
  装配入口: MybatisPlusConfig  （AutoConfiguration.imports 就这一行）
  扫描: @MapperScan("${mybatis-plus.mapperPackage}")
        admin YAML: org.namewta.**.mapper
  依赖: wta-common-core + wta-common-satoken + wta-api
        + dynamic-datasource-spring-boot4-starter
        + mybatis-plus-spring-boot4-starter + jsqlparser
        + mybatis-plus-join-boot-starter
  内置 yml: common-mybatis.yml
        idType=ASSIGN_ID；logicDeleteValue=1 / logicNotDeleteValue=0
        insert/update/where Strategy=NOT_NULL

三件家具（本课认）:
  BaseEntity          标签贴纸（五枚填充章）
  BaseMapperPlus<T,V> 叉车（读 + 写 + VO 转换 + lambda 链）
  QueryBuilder        每次出门新填的购物单（lambda / lambdaJoin）

两把合法钥匙:
  layered DAO            @Repository；字段是本模块 Mapper
  classic Service / Impl 字段是本模块 Mapper；可在方法里 QueryBuilder.lambda(...)

谁不许拿:
  Controller / Listener / API Adapter
  UseCase / UseCaseImpl
  layered Service（任何文件名）
  新模块的 LiteFlow 节点、support、policy（不能自己碰库）
```

往下走不要跳层：先认持有者，再认三件家具怎么咬合，再认查询阶梯，最后认正反例。

## 核心概念与机制

### 直觉讲解

小孩子会问：为什么不能谁方便谁写 SQL？因为 SQL 条件、分页、行锁、逻辑删除、数据权限，全是「仓库怎么进货」的事。把它写进管家（UseCase）或厨师（layered Service），下一次改 WHERE，事务边界和业务规则一起抖。旧房子已经把厨师和仓库打通了，登记表允许它们保持；新房子把钥匙收回仓库管理员。

`BaseMapperPlus` 看起来像「多几个 selectVo」。打开源码会发现它还会 `Db.saveBatch`、`lambda().delete()`。矩阵才写「不纯查询」：你把它当只读镜头，就会在「只是查一下」的方法里删行。

`QueryBuilder` 是工厂，私有构造，不许 `new QueryBuilder()`。每次调用 `lambda(Entity.class)` 或 `lambdaJoin("u", Entity.class)` 都是**一张新表**。表上有勾选格：`eqIfPresent`（值不是 null 才写等于）、`eqIfText` / `likeIfText`（字符串不是空白才写）、`betweenParams`（从 `params` 地图取 begin/end）。这张表是可变的。跨请求复用、塞进 Spring Bean 字段、两个线程抢同一张表，都会把别人的 WHERE 粘到你的 SQL 上。

`BaseEntity` 更像仓库收货时自动盖的章。你不用在每个 insert 里手写 `createBy`。`InjectionMetaObjectHandler` 看见实体是 `BaseEntity`，插入时盖创建/更新时间和人；没登录就盖 `-1L`。更新时**无论**你带没带 `updateTime`，都盖成现在。这不是业务规则，是基础设施。别在 Service 里再抄一遍「当前用户写进 createBy」，除非你有意覆盖。

### 精确定义与 English term

| 中文 | English | 精确定义（本课，以磁盘为准） |
| --- | --- | --- |
| 实体基类 | `BaseEntity` | `org.namewta.common.mybatis.core.domain.BaseEntity`：五字段 `createDept/createBy/createTime/updateBy/updateTime`，`@TableField(fill=INSERT)` 或 `INSERT_UPDATE`。**不含** `@Version`、**不含** `@TableLogic` |
| Mapper 基类 | `BaseMapperPlus<T,V>` | 扩展 MP `BaseMapper<T>` 的项目 Mapper 接口。`T` 是表实体；`V` 是 `selectVo*` 的默认转换目标（VO、Row、或同一实体） |
| 查询构造入口 | `QueryBuilder` | 工具入口类。`lambda` → `LambdaQueryBuilder`（底层 `AggregateLambdaQueryWrapper`）；`lambdaJoin` / `lambdaJoin(alias, class)` → `LambdaJoinQueryBuilder`（底层 MPJ `MPJLambdaWrapper`） |
| Mapper 链式 CRUD | `LambdaCrudChainWrapper` | `mapper.lambda()` 返回。能 `voList` / `voPage`，也能 `update` / `delete`。每次 `lambda()` 都是新实例 |
| 新鲜包装器 | fresh wrapper | 每个请求/每个独立操作 `new` 或 `QueryBuilder.lambda` / `mapper.lambda()` 一次；禁止当 Bean 字段缓存 |
| 数据访问对象 | DAO | layered 业务里**唯一**持有 Mapper 与 MyBatis 类型（Wrapper、`Page`、`PageQuery`、QueryBuilder）的层；`@Repository` |
| 存量厨师+仓库 | classic ServiceImpl | 登记为 classic 的 CRUD 房间允许 Service 持 Mapper 并在 Service 里组 Wrapper；不得借新功能把 Mapper 再交给 Controller |
| 读模型 | Row | layered `domain/model/read`；Mapper XML 的结果类型。模板第二泛型是 Row；profile 现网常把第二泛型写成 Entity，Row 走**具名 XML** |
| 查询阶梯 | query ladder | `BaseMapperPlus` 内建 → fresh wrapper/`QueryBuilder` → MPJ `lambdaJoin` → Mapper XML；短静态注解 SQL 是窄例外 |
| 数据权限贴纸 | `@DataPermission` / `@DataColumn` | 贴在**真正执行**查询/更新/删除的 Mapper 方法上。`key` 对模板占位符（`deptName`/`userName`），`value` 对 SQL 列或 `alias.column` |
| 填充器 | `InjectionMetaObjectHandler` | MP `MetaObjectHandler`。插入盖五枚章；更新盖 `updateTime`/`updateBy`；无登录用户 id=`-1L` |
| 分页信封 | `PageQuery` | 页码/页大小/排序列。`build()` 得到 MP `Page`。排序列走 `SqlUtil.escapeOrderBySql` 白名单。layered：**Controller 拆成整数再往下传**，Service/UseCase 不得 import |

### 机制/因果链

动手前先走这根链，不要先在 Controller 里 `new LambdaQueryWrapper`。

1. **查登记表，决定钥匙在哪一层。** layered：DAO 持 Mapper。classic CRUD：ServiceImpl 持 Mapper。classic 但没有表的房间（job / ai）本课没有钥匙可讲。
2. **实体先继承 `BaseEntity`，再自己声明主键、`@Version`、`@TableLogic`。** `TestDemo` 是样板：`extends BaseEntity`，`@TableId("id")`，`@Version Long version`，`@TableLogic Long delFlag`。`NotifyIntent` 也 `extends BaseEntity`，但 `version` 只是 `Integer` 字段、**没有** `@Version`——存量，不是新表范例。
3. **Mapper 接口声明叉车型号。** Target：`extends BaseMapperPlus<Entity, Vo>`（classic 模板）或 `BaseMapperPlus<Entity, Row>`（layered 模板）。工作树偏差见反例表，新代码按 Target，不把 notify/third 的裸 `BaseMapper` 抄进新模块。
4. **选择查询阶梯，不要跳级炫技。**
   - 主键/批量/简单 VO：`selectVoById` / `selectVoList` / `selectVoPage` / `insertBatch`。
   - 动态 AND 条件：`QueryBuilder.lambda(Entity.class).eqIfText(...).build()` 交给 `selectVoPage`，或 `mapper.lambda().eqIfText(...).voPage(page)`。
   - 多表、别名、选到另一个 VO：`QueryBuilder.lambdaJoin("u", SysUser.class)`，alias 必须和 `@DataColumn(value="d.dept_id")` 一致。
   - 行锁、复杂子查询、聚合、批量特性：XML，`namespace` = Mapper 全名，`id` = 方法名。
   - 一行静态 SQL：同模块已有证据才允许 `@Select`（`SysOssConfigMapper.selectByIdForUpdate`）。长 SQL、动态 if、复杂 join **禁止**拆成 Java 字符串。
5. **layered 的 XML 合同更严。** person / enterprise 的架构测试要求：Mapper **声明的方法**与 XML `id` 一一对应；禁止 `@Select/@Insert/@Update/@Delete`；禁止 Mapper 面向 `domain.vo`。profile DAO 因此几乎是「同名转发 + 锁语义」，不在 DAO 里调用继承的 `selectById` 绕过 XML。notify / sso / third 的 DAO **会**调用继承 CRUD + `LambdaQueryWrapper`——这是五层房间里的棘轮，Target 仍是「业务调用落到同名 XML」。
6. **填充发生在 MP 写库时，不发生在你 `new Entity()`。** `insertFill`：创建/更新时间同一时刻；`createBy` 为空才填登录人，否则尊重调用方；没登录填 `-1L`。`updateFill`：总是覆盖 `updateTime`，更新人取登录人或 `-1L`。非 `BaseEntity` 走 `strictInsertFill` 的 `createTime/updateTime`（兼容 `Date`）。
7. **数据权限不是 Controller 的 `@SaCheckPermission`。** 方法上的 `@DataPermission` 被 `DataPermissionPointcut` 认出来（**不沿继承链找父接口方法**）。`BaseMapperPlus.lambda()` 先 `mapperProxy()`：从 Spring 容器取出 Mapper 接口 Bean，让贴纸还在。如果你手里拿的是「未托管的 Mapper 对象」，切面会哑火，SQL 不再带部门条件。超级管理员 `LoginHelper.isSuperAdmin()` 在 `PlusDataPermissionHandler` 里整段跳过。
8. **分页排序是白名单，不是前端字段直拼。** `PageQuery.buildOrderItem` 先 `SqlUtil.escapeOrderBySql`（只许字母数字下划线空格逗号点），再下划线化，再认 `asc/desc` 或 Element 的 `ascending/descending`。列数和方向数对不上，或方向不是 asc/desc → `ServiceException("排序参数有误")`。默认 `pageSize=Integer.MAX_VALUE`（「不传就查全部」），不是 10。
9. **拦截器链在 `MybatisPlusConfig.mybatisPlusInterceptor`：数据权限 → 分页（`overflow=true`）→ 乐观锁。** 注释里写了「全表更新删除阻断」，**当前 Bean 没有注册** `BlockAttackInnerInterceptor`。不要口试说「框架会挡住 `delete` 不带 WHERE」。逻辑删除总开关是 `mybatis-plus.enableLogicDelete`，默认 true；设 false 时 `PlusPostInitTableInfoHandler` 把表信息上的 `withLogicDelete` 拧掉。
10. **为什么 mybatis 依赖 `wta-api` 和 satoken。** 填充器和数据权限要读 `LoginHelper.getLoginUser()` / `LoginUser` / `RoleDTO`。这是工具间 → 对讲机的合法方向，不是业务模块去 import 邻居 Mapper。

### 图、表或文本图

**图题 / caption：** 两把合法钥匙，三件家具，一条查询阶梯。alt：上半 layered 只有 DAO 碰叉车；下半 classic CRUD 由 ServiceImpl 碰叉车；右侧工具间家具；下方阶梯从内建方法走到 XML。

```text
浏览器
  │
  v
Controller  ──不得 import mapper / QueryBuilder / LambdaQueryWrapper
  │
  ├─ layered ─────────────────────────────────────────────┐
  │     UseCase     不得 import dao/mapper/MyBatis        │
  │       → Service 不得 import mapper/PageQuery/Wrapper  │
  │         → DAO @Repository                             │
  │              字段: XxxMapper                          │  ← 合法钥匙 1
  │              可: QueryBuilder / Wrapper / Page / 锁    │
  │              → Mapper extends BaseMapperPlus<T, V>    │
  │                 → XML  mapper/<module>/XxxMapper.xml  │
  └───────────────────────────────────────────────────────┘
  │
  └─ classic CRUD（system / demo / workflow 的表） ───────┐
        Service / ServiceImpl                             │  ← 合法钥匙 2
           字段: XxxMapper                                │
           可: QueryBuilder.lambda / Wrappers.lambdaQuery │
           → Mapper →（空 XML 或自定义 XML）              │
        门卫仍不得抱 Mapper                               │
  └───────────────────────────────────────────────────────┘

工具间 wta-common-mybatis
  BaseEntity ──insert/update 时 InjectionMetaObjectHandler 盖章
  BaseMapperPlus.lambda() ──每次新的 LambdaCrudChainWrapper（能删能改）
  QueryBuilder.lambda() ──每次新的 LambdaQueryBuilder.build() → Wrapper
  QueryBuilder.lambdaJoin() ──可 .list/.page/.count 自己开火
  PlusDataPermissionInterceptor ──只改带贴纸的 SELECT/UPDATE/DELETE
```

**文字等价物：** 请求先到门卫。layered 门卫只认识管家；管家吩咐厨师；厨师只叫自己的仓库管理员；管理员才启动叉车、填写购物单、必要时打开 XML 说明书。classic CRUD 房子里厨师自己启动叉车。无论哪间房，购物单都是一次性的；叉车既能搬货进来也能把货叉走；标签贴纸在进门和出门时由填充器盖章，不由门卫盖。数据权限是贴在叉车按钮上的限位开关，不是大门的工牌。

**图的边界：** 本图不画 `@DS` 多数据源（L-085）、不画 Redis 会话（L-084）、不画 Outbox 领取 SQL（L-052）。job/ai 没有这条 Mapper 链。LiteFlow 节点画在图外：它们不是合法钥匙。

### 正例、反例与边界

**正例 1 — classic ServiceImpl 抱 Mapper + 内建 VO 分页（demo）。** `TestDemoServiceImpl` 唯一字段 `TestDemoMapper demoMapper`。`queryById` → `selectVoById`（`selectById` + `MapstructUtils.convert`）。`queryPageList` 用 `Wrappers.lambdaQuery()` 组条件，再 `selectVoPage`。这是登记表允许的 classic 钥匙。本课不把八扇 HTTP 标进自己的 (a)——那是 L-074。

**正例 2 — classic ServiceImpl 用 `QueryBuilder.lambda`（system 操作日志）。** `SysOperLogServiceImpl.buildQueryWrapper`：

```text
QueryBuilder.lambda(SysOperLog.class)
  .likeIfText(SysOperLog::getTitle, operLog.getTitle())
  .eqIfPresent(SysOperLog::getStatus, operLog.getStatus())
  .betweenParams(SysOperLog::getOperTime, params, "beginTime", "endTime")
  .build();
```

空白标题不会变成 `LIKE %%`。null 状态不会写成 `status IS NULL`（那是 `isNull`，不是 `eqIfPresent`）。这张单用完即丢。

**正例 3 — Mapper default 方法里的 `lambdaJoin`（仍在 SQL 面）。** `SysUserMapper.selectUserExportList`：`QueryBuilder.lambdaJoin("u", SysUser.class)`，左连 `SysDept` 别名 `d`、再连领导用户 `u1`。`@DataColumn(value="d.dept_id")` 与 `"u.create_by"` 和 alias **同一套字母**。这不是 Service 越层，是叉车自己的联表按钮。

**正例 4 — layered DAO 是唯一持有者（profile）。** `PersonApplicationDao`：`@Repository`，字段 `PersonApplicationMapper mapper`，方法把锁查询/插入转发到同名 Mapper 方法。`PersonModuleArchitectureTest.enforcesTheFiveLayerDependencyDirection` 断言：Service 源码不得出现 `import ...mapper.` / `IService` / `BaseMapper` / `QueryWrapper`；UseCase 不得 import dao/mapper；DAO 必须 import mapper、必须 `@Repository`、不得 import service/usecase；Controller 只 import usecase。enterprise 测试同构。

**正例 5 — layered DAO 注释把边界写在类上（notify）。** `NotifyNotificationDao` 第一句 Javadoc：「通知运行时持久化边界，业务服务不得直接依赖 MyBatis Mapper。」它持有七个 Mapper。现网用 `new LambdaQueryWrapper<>()` 而不是 `QueryBuilder`——所有权仍在 DAO，查询入口类名是棘轮，不是反例。

**正例 6 — `selectVo*` 是转换不是投影。** `selectVoById`：先 `selectById`，null 则 null，否则 `MapstructUtils.convert(obj, voClass)`。SQL 仍是 `SELECT *` 实体行。别指望它帮你少查一列。真要投影，走 XML 的 Row 或 `lambdaJoin` 的 `selectAs`。

**反例 1 — 门卫抱 Mapper。** `TestBatchController`：`private final TestDemoMapper testDemoMapper`，注释承认「为了便于测试」。`insertBatch` 直接从 HTTP 打进仓库。classic **允许的是 ServiceImpl**，不是入口。新代码复制这扇窗 = 扩大越层。

**反例 2 — UseCase / layered Service 组 Wrapper。** person/enterprise 架构测试会红。登记表原文：「Service/UseCase 不得导入 MyBatis、Mapper、`IService` 或 `ServiceImpl`」。

**反例 3 — 把 `QueryBuilder` 放进 Spring 单例字段。** wrapper 可变。第二次请求会带着第一次的 `eq`。`LambdaCrudChainWrapper.clear()` 存在，但所有权规则要求的是**每个操作新实例**，不是「记得 clear」。

**反例 4 — 新模块 `extends ServiceImpl<XxxMapper, Xxx>`。** 全仓搜不到。DEC-009 禁止。那会把 MP 的 CRUD 服务当成业务层，钥匙直接焊在厨师身上，五层作废。

**反例 5 — 给 getInfo 以为已经有数据权限。** 贴纸不沿继承生效。`TestDemoMapper` 给 `selectVoPage` / `selectVoList` / `selectByIds` / `updateById` 贴了；`selectVoById` 走 `selectById`，**没贴**。详情窗可以读到列表窗读不到的行。要补权限，得 override 真正执行的那一个方法。

**反例 6 — alias 和数据权限列各写各的。** `lambdaJoin("u", ...)` 却把 `@DataColumn` 写成 `dept_id` 无前缀，join 后歧义或贴错表。

**边界 1 — 裸 `BaseMapper`。** notify 的 Intent/Recipient/Delivery/Attempt/Outbox，以及 third 全部 Mapper，2026-09-17 仍 `extends BaseMapper<Entity>`。它们照样只能被 DAO 持有。不要把「没 Plus」说成「没所有权规则」。也不要在新模块继续裸奔。

**边界 2 — 第二泛型不是 Vo。** `PersonApplicationMapper extends BaseMapperPlus<ProfilePersonApplication, ProfilePersonApplication>`。架构测试还禁止 Mapper 引用 `domain.vo`。Row 在 XML 的 `resultType` 里。

**边界 3 — LiteFlow 节点持 QueryBuilder。** `CompleteAutoPassComponent.selectByInstId`：`flowTaskMapper.selectList(QueryBuilder.lambda(FlowTask.class).eq(...).build())`。workflow 是 classic，但这是执行器节点不是 ServiceImpl。口试把它标成**存量泄漏**；新能力应收回 ServiceImpl 或将来的 DAO。

**边界 4 — `lambdaJoin.list()` 自己开火。** 持有 QueryBuilder 就已经持有持久化能力。不要说「我没注入 Mapper 所以没越层」。

**边界 5 — 无登录盖 `-1L`。** 定时任务、回调、匿名核身若 insert `BaseEntity`，创建人是 -1，不是 null。查询「创建人为空」会漏掉这些行。

**边界 6 — `PageQuery` 默认查全部。** 忘了传 pageSize，就是 `Integer.MAX_VALUE`。layered 公告把 pageSize 在门口 clamp；classic 日志页靠前端传。工具间自己不 clamp 业务上限。

**边界 7 — 逻辑删除值在 yml 是 1/0，字段类型在 demo 是 `Long`。** 不要口试成布尔。全局开关 `enableLogicDelete` 能一次性拆掉所有 `@TableLogic` 行为。

**边界 8 — 架构测试是模块自己的，不是 common 里的 ArchUnit。** person/enterprise 读源码字符串。notify 的所有权主要靠纪律和 DAO 注释。没有一张全仓测试把「所有 Controller 都不 import Mapper」扫一遍——所以 `TestBatchController` 还能活着。

## 变式与迁移

1. **和 L-003 对照。** 本课把「DAO 才碰 Mapper / classic ServiceImpl 可以碰」展开成三件家具。不改登记表。不把 third 未发表的登记当成第三种模式。
2. **和 L-006 对照。** 主路径走到 Mapper XML 时，先问这把钥匙在 DAO 还是 ServiceImpl。菜单查询是 system classic：钥匙在 `SysMenuServiceImpl` / `SysMenuMapper`。
3. **和 L-074 对照。** demo 正例只借「ServiceImpl 字段是 Mapper」「`selectVoPage` 不执行 XML」。八扇 HTTP 仍是那一格。
4. **和 L-015 / L-016 对照。** 用户列表的 `@DataPermission` 贴在 `SysUserMapper` 的 default 方法上。本课认贴纸位置和代理；角色 `data_scope` 怎么变成 SQL 是 L-016。
5. **和 L-025 对照。** `SysOssConfigMapper` 的 `@Select ... for update` 是阶梯最窄的例外：短、静、无动态拼接。不要用它证明「注解 SQL 开放了」。
6. **和 L-034 / L-042 对照。** profile DAO 持 Mapper、生产 Service 不 import Mapper、SQL 在 XML——那是本课 (c) 的现场举例。那些课不盖本格。
7. **和 L-047 / L-051 对照。** notify 配置 Mapper 甚至没有 XML、DAO 用 Wrapper；运行时 DAO 持多个 Mapper。所有权正例，XML 完备性不是本格。
8. **新模块。** 默认 layered。建 `dao/` + `@Repository`；Mapper 用 layered 模板（第二泛型 Row，方法对 XML）；Service 零 MyBatis import。想用 `QueryBuilder` 就写在 DAO 里，不要因为「system 都写在 ServiceImpl」而抄错层。
9. **把 classic 迁 layered。** 先把 `buildQueryWrapper` 整段搬进 DAO，再让 Service 只收 `PageResult<Row>` / Entity。Mapper 第二泛型从 Vo 换成 Row 时，所有 `selectVo*` 调用都要改名或停用，否则 VO 会从 DAO 漏到 HTTP。
10. **触及 LiteFlow 节点里的 QueryBuilder。** 按棘轮：能收回 ServiceImpl 就收回；不能收回就不要再复制到第二个节点当「惯例」。
11. **notify/third 裸 `BaseMapper`。** 新 Mapper 直接 `BaseMapperPlus`。旧的不借本课全仓改名。
12. **迁移口诀：** 先问登记表钥匙在哪一层 → 再问口袋里的类型是不是 Mapper/Wrapper/QueryBuilder/PageQuery → 再问这次查询停在阶梯哪一级 → 再问贴纸贴在不在真正执行的方法 → 最后问购物单是不是新的。跳步会出现「UseCase 里 LIKE」「Controller 里 lambdaJoin.list」「详情当列表一样有数据权限」。

## 常见误区

1. **「OBJ-83 是把所有 Mapper XML 讲一遍。」** 格子是所有权 + 两件不纯查询家具。具体业务 SQL 在各切片课。
2. **「`BaseMapperPlus` 等于只读。」** 能批量写、能 `lambda().delete()`。
3. **「`QueryBuilder` 是 Mapper。」** 它是工厂。`lambda()` 产出 Wrapper；`lambdaJoin()` 的 `list/page/count` 才会执行。
4. **「只有 `*ServiceImpl` 能持 Mapper。」** 看层。classic 不带 Impl 的 `@Service` 也可以；layered 带 Impl 也不可以。
5. **「DAO 里没用 QueryBuilder 就没遵守所有权。」** 遵守的是「谁持有」；notify/sso 现用 `LambdaQueryWrapper` 仍算 DAO 持有。
6. **「`selectVoById` 查的是 VO 表。」** 查实体，再 Mapstruct。
7. **「`BaseEntity` 含 version / delFlag。」** 不含。看 `TestDemo` 自己的字段。
8. **「填充器会覆盖我手动 set 的 createBy。」** 插入时 createBy 非空就尊重你；updateTime 在更新时**总是**覆盖。
9. **「无登录 insert 会失败。」** 盖 `-1L`，不抛。抛的是填充过程自己炸了：`ServiceException("自动注入异常 => ...")`。
10. **「数据权限贴纸贴在接口上，子方法都有。」** Pointcut 写明不对继承生效。每个真正执行的方法自己贴，或贴在 Mapper 类型上。
11. **「`mapper.lambda()` 一定带数据权限。」** 只有拿到 Spring 代理才带。测试里 new 出来的 Mapper 可能哑火。
12. **「分页 overflow 会 404。」** `overflow=true`，页码过大被合理化，不是失败。
13. **「框架挡住全表删除。」** 注释有，Bean 没有。
14. **「`PageQuery` 可以进 layered Service。」** 禁止。门口拆成 pageNum/pageSize。
15. **「frontend 字段名直接 order by。」** 先白名单，再下划线。非法字符 `IllegalArgumentException("参数不符合规范，不能进行查询")`。
16. **「wta-common-mybatis 按五层建目录。」** 登记表：common 不是业务五层。它提供叉车，不提供 DAO。
17. **「job/ai 没有 Mapper 所以本课失败。」** 它们没有这把钥匙；所有权规则对「有 Mapper 的房间」生效。
18. **「third 表上没登记，可以 classic 持 Mapper。」** 未登记默认 layered。third 磁盘已经是 DAO 持有。
19. **「本课覆盖 `@DS` / 六份 SQL。」** L-085。
20. **「本课覆盖 LoginHelper 写 Redis。」** L-084。填充器只**读** LoginUser。
21. **「`NotifyIntentMapper extends BaseMapper` 所以 notify 不是 layered。」** layered 看调用链和 DAO，不看第二基类名字。
22. **「在 Mapper 注解里拼接 `${ew.customSqlSegment}` 可以塞前端字符串。」** 只许 MP 受控 wrapper。

## 非评分暂停

打开磁盘，不要凭记忆默写。不要改文件。没有标准答案栏、没有分数。

1. 打开 `03-backend-module-modes.md`。圈 layered「DAO 是业务持久化唯一入口」和 classic「ServiceImpl 可以直接持有 Mapper」。
2. 打开 `BaseEntity.java`。数字段：必须是五个填充字段。确认没有 `version`、没有 `delFlag`。打开 `TestDemo.java` 对照 `@Version` / `@TableLogic` 写在子类。
3. 打开 `BaseMapperPlus.java`。圈 `insertBatch`、`lambda()`、`mapperProxy()`、`selectVoById` 的 `selectById` + `MapstructUtils.convert`。用手指点一次 `lambda().delete()` 在 `LambdaCrudChainWrapper`。
4. 打开 `QueryBuilder.java`。圈私有构造、`lambda`、两个 `lambdaJoin`。打开 `SysOperLogServiceImpl.buildQueryWrapper` 和 `SysUserMapper.selectUserExportList`，把 `IfText` / `betweenParams` / alias `"u"` 勾出来。
5. 打开 `PersonApplicationDao.java` 与 `PersonModuleArchitectureTest` 的五层断言。再打开 `NotifyNotificationDao` 类注释。再打开 `TestBatchController` 的 Mapper 字段。把三份放在桌上：合法 layered、合法 layered（Wrapper 风格）、非法入口。
6. 打开 `InjectionMetaObjectHandler`。圈 `DEFAULT_USER_ID`、插入时 `createBy` 为空才填、更新时 `updateTime` 总是现在。打开 `MybatisPlusConfig.mybatisPlusInterceptor`，确认只有数据权限、分页、乐观锁三节，没有全表阻断。
7. 打开 `DataPermissionPointcut`。圈「不对继承生效」。打开 `TestDemoMapper`，勾有贴纸的方法和没有贴纸的 `selectVoById` 路径。
8. 全仓搜索 `import org.namewta.common.mybatis.core.query.QueryBuilder`：确认 `dao/`、`usecase/`、`controller/` 为零；`service/impl` 只出现在 system 与 workflow。不要改搜索结果。

## 总结、词汇表与下一步

- **宏观仓库钥匙：** 业务代码里，Mapper / Wrapper / QueryBuilder / `PageQuery` 只许待在 layered DAO 或 classic Service。门卫、管家、新房子厨师不许拿。
- **(c) `dao-mapper-ownership`：** 架构测试在 profile 把 import 方向锁死；notify DAO 用注释和字段锁死；全仓没有 IService。入口抱 Mapper 是拧松，不是存量允许。
- **(b) `BaseMapperPlus / QueryBuilder`：** 不纯查询。Plus 能写能删能转 VO；QueryBuilder 的 join 链能自己开火。每次新实例。阶梯从内建方法走到 XML，注解 SQL 是窄例外。
- **`BaseEntity`：** 五枚章，不含乐观锁和逻辑删除。没登录盖 `-1L`。更新时间总是覆盖。
- **工作树偏差要当面说：** notify/third 裸 `BaseMapper`；DAO 尚未普遍改用 QueryBuilder；workflow LiteFlow 节点持 QueryBuilder；`TestBatchController` 越层。新代码按 Target，不把偏差当模板。

词汇表：`BaseEntity` / `BaseMapperPlus` / `QueryBuilder` / `LambdaQueryBuilder` / `LambdaJoinQueryBuilder` / `LambdaCrudChainWrapper` / `InjectionMetaObjectHandler` / `@DataPermission` / `@DataColumn` / `PageQuery` / `fresh wrapper` / DAO / classic ServiceImpl / `selectVo*` / `eqIfText` / `eqIfPresent` / `lambdaJoin` / `ASSIGN_ID` / `-1L`。

下一步：L-084 讲登录成功但 Redis/Sa-Token 读失败算哪边（填充器读 LoginUser 依赖那条河，但不负责写）。L-085 讲六份 SQL 基座和 `@DS`。L-082 是通知同步入口，不是 Mapper。本课结束不发作业、不打分、不宣称掌握；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-008 | `.agents/skills/engineering-standards/references/project/03-backend-module-modes.md` | layered DAO 唯一入口；classic ServiceImpl 可持 Mapper；common 不套五层 | 模式定义表；DEC 同行 | 2026-09-17 |
| S-007 | `.agents/skills/engineering-standards/references/project/01-module-map.md`；`engineering-standards/SKILL.md` | mybatis 工具间职责；硬约束「DAO 唯一持有 Mapper」 | 数据访问行；固定实现合同 | 2026-09-17 |
| S-004 | `backend/wta-modules/**` | 各模块 Mapper/DAO/ServiceImpl 持有关系 | 见下列 S-L083 | 2026-09-17 |
| S-L003-01 | `children/2026-09-14-namewta-architecture/lessons/L-003-layered-vs-classic.md` | 登记表两列；DAO 定义；TestBatch 越层；job/ai 无 Mapper 链 | 宏观地图与机制链 | 2026-09-17 |
| S-L083-01 | `wta-common-mybatis`：`BaseEntity.java`；`InjectionMetaObjectHandler.java`；`PlusPostInitTableInfoHandler.java`；`common-mybatis.yml`；`MybatisPlusConfig.java`；`pom.xml`；`AutoConfiguration.imports`；`application.yml` `mapperPackage` | 五字段；-1L；更新覆盖时间；逻辑删除开关与 1/0；拦截器链无全表阻断；ASSIGN_ID；扫描包；依赖 api/satoken | 各文件全文/配置段 | 2026-09-17 |
| S-L083-02 | `BaseMapperPlus.java`；`LambdaCrudChainWrapper.java` | 泛型 T/V；`lambda`/`mapperProxy`；`selectVo*` 为查询+转换；`insertBatch`；链上 `delete`/`update`/`clear` | Plus 37–407；Chain 63–907 | 2026-09-17 |
| S-L083-03 | `QueryBuilder.java`；`LambdaQueryBuilder.java`；`LambdaJoinQueryBuilder.java`；`LambdaQueryCondition.java` | 工厂两入口；`build()` 交 Wrapper；join 的 `list/page/count` 自执行；`eqIfPresent`/`eqIfText`/`likeIfText`/`betweenParams` | query 包 | 2026-09-17 |
| S-L083-04 | `TestDemo.java`；`TestDemoServiceImpl.java`；`TestDemoMapper.java`；`TestBatchController.java` | 子类 version/delFlag；classic 持 Mapper + Wrappers；贴纸范围不含 selectById；入口越层 | demo 模块所列类 | 2026-09-17 |
| S-L083-05 | `SysOperLogServiceImpl.java`；`SysUserMapper.java`；`SysOssConfigMapper.java`；`SystemOpenApiCredentialService.java` | QueryBuilder.lambda 正例；lambdaJoin+alias；短注解 SQL；classic 不带 Impl 仍持 Mapper | 各方法如上 | 2026-09-17 |
| S-L083-06 | `PersonApplicationDao.java`；`PersonApplicationMapper.java`；`PersonModuleArchitectureTest.java`；`EnterpriseModuleArchitectureTest.java` | DAO 唯一持有；XML 与方法 1:1；禁止注解 SQL；Service/UseCase 不得 mapper | dao/mapper/test | 2026-09-17 |
| S-L083-07 | `NotifyNotificationDao.java`；`NotifyIntentMapper.java` 及 notify/third 各 Mapper | DAO 边界注释；裸 BaseMapper 偏差；DAO 用 LambdaQueryWrapper 而非 QueryBuilder | notify/third mapper+dao | 2026-09-17 |
| S-L083-08 | `SsoAuthorizationCodeDaoImpl.java`；`CompleteAutoPassComponent.java`；workflow/system `service/impl` 的 QueryBuilder import 表 | DAO 可叫 DaoImpl；LiteFlow 泄漏；QueryBuilder 只出现在 classic service/impl 与若干 Mapper/LiteFlow，dao/usecase/controller 为零 | 2026-09-17 工作树搜索 | 2026-09-17 |
| S-L083-09 | `PageQuery.java`；`SqlUtil.java`；`DataPermission.java`；`DataColumn.java`；`DataPermissionPointcut.java`；`PlusDataPermissionInterceptor.java`；`PlusDataPermissionHandler.java`；`DataScopeType.java`；`docs/fm/java/{mapper,serviceImpl,domain}.java.ftl`；`docs/fm/java/layered/{mapper,dao}.java.ftl`；DEC-009 | 默认整表分页；排序白名单；贴纸不继承；超管跳过；模板第二泛型 Vo vs Row；禁止 IService | 各文件 | 2026-09-17 |
