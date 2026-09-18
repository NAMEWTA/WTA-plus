---
lesson_id: L-085
objective_ids: [OBJ-85]
claimed_cells:
  - C:mysql-base
  - C:MySQL 8.4
  - D:存储点 MySQL
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: six-files-and-owned-baseline
    minutes: 10
  - segment: init-upgrade-and-DS
    minutes: 11
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-009, S-010, S-L005-01, S-L085-01, S-L085-02, S-L085-03, S-L085-04, S-L085-05, S-L085-06, S-L085-07, S-L085-08, S-L085-09, S-L085-10, S-L085-11]
---

# Lesson 085：宏观一座 MySQL 8.4 仓库——10/50 是自有货架，`@DS` 是仓库门牌

## 学完你能做什么

打开 `release-artifacts/docker/infrastructure/mysql/init/`，你能**口述这座宏观仓库**：NAMEWTA 只验收 **MySQL 8.4**；空库按六份数字前缀脚本长出来；**自有产品表**只登记在 `10-cde-base-ddl.sql`，**自有产品数据**（种子、菜单、回填）只登记在 `50-cde-base-dml.sql`。再打开 `wta-admin` 的 `application-dev.yml` / `application-prod.yml`，你能指着 `spring.datasource.dynamic` 说：**活着的门牌只有 `master`**；`@DS` 是换门牌的字，`@DSTransactional` 是同一趟采购的收据，所有权不在 Mapper，也不在 Controller。

口试名单就是这三格，符号以**磁盘**为准：

**`C:MySQL 8.4`**（矩阵印「MySQL 8.4 基座」，类型 Container）——镜像、库名、六份 init 脚本这座柜子。

**`C:mysql-base`**（Component）——自有表/数据权威在 10/50，外加动态数据源门牌归谁写。

**`D:存储点 MySQL`**（矩阵原文「业务表只进 10/50 基座」；chain 别名 `D:happy-store-mysql`，**同一格**）——新业务行、新列、新菜单往哪张纸上写。

OBJ-85 原文：能指出 MySQL `10-cde-base-ddl.sql` / `50-cde-base-dml.sql` 是自有表唯一基座，以及动态数据源 `@DS` 的所有权。前置是 L-005：那边已经把 10/50 钉成公共合同里的「存储格」，并把 DSL / 升级演练 / Tag 差异稿**推给本课**。本课把那句口号走成能指文件、能指语句类型、能指门牌、能指「已有库不许重放」的口试。

本课**不宣称**你会拆 `BaseMapperPlus` / QueryBuilder / DAO 持 Mapper（L-083）、Sa-Token 写 Redis（L-084）、`NotifyDispatcher`（L-082）、MinIO 直传走廊（L-025…L-029 已 covered）、或把 `20`/`30`/`40`/`60` 上游快照内部当本格 covered。那四份文件**存在**，本课只认它们的邻居身份：不是自有产品表登记处。

2026-09-17 工作树先钉死**负空间**（口试先数文件，再数门牌）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| 第七份 `migrate/V2026….sql` | **没有。** 目录恰好六份。`stage-mysql` 按 `10→20→30→40→50→60` 的文件名排序，多一份就失败 |
| 后端 `backend/script/` 或模块私有 SQL 副本 | **没有。** 2026-09-17 `backend/` 根下无 `script/`。测试用 `SqlBaselinePaths` 去读父仓那六份 |
| PostgreSQL / Oracle / SQL Server 产品基座 | **没有。** yaml 里有注释掉的方言样例，不是许可证 |
| `10` 里有 `INSERT` | **没有。** 70 张 `CREATE TABLE`，0 条 insert，0 条 `ALTER TABLE` |
| `50` 里有产品 `CREATE TABLE` | **没有。** 4 张 `create temporary table …_preflight`，会话结束即消失 |
| 活数据源名叫 `slave` / `oracle` | **没有。** `primary: master`，`strict: true`；`slave` 整段注释掉 |
| 生产代码里有 `@DS("slave")` | **没有。** 只在 `TestTreeServiceImpl`、`TestBatchController` 里**注释**着 |
| `@DS` 贴在 Mapper / Controller 上就算所有权 | **不是。** 门牌目录在 admin yaml；换门牌贴在被代理的 public 方法；事务边界是 `@DSTransactional` |
| 已有库再跑一遍 10/50 就升级了 | **禁止。** 50 文件头写死：历史 DSL 标识只留追溯，不是升级步骤 |
| `test_leave` 在 10 里 | **不在。** 它和 `flow_category` 住在 `30-cde-workflow.sql`。存量夹带，不是新表许可证 |
| `EXPECTED_TABLES=125` 就是表权威 | **不是。** 2026-09-17 对 10+20+30+40 的 `CREATE TABLE` 合计 **127**；脚本断言仍写 125。口试数 CREATE |
| 容器永远叫 `namewta-data-mysql` | **不是。** compose 服务容器名是 `namewta-mysql`；保护脚本默认另一个名字，调用必须 `--container` 对上 |
| 本课把 Redis 8 / MinIO 容器标 covered | **不是。** 本课只在 OSS 种子行上碰到 MinIO 配置，柜子不是本格 |

## 先把宏观地图放在桌上

L-001 已经把 MySQL 8.4 画成大楼里的**业务行柜子**。L-005 把 10/50 认成四张公共合同里的 SQL 条约。本课走进柜子内部：货架怎么编号、谁能往货架上钉木板、进门的牌子写谁的名字。

把空环境想成一间还没上货的仓库。仓库的墙是 **MySQL 8.4.9**（Compose `image: mysql:8.4.9`，`character-set-server=utf8mb4`，`collation-server=utf8mb4_general_ci`，`lower_case_table_names=1`）。仓库里有两间库房：

- 业务库房牌子写 **`wta-plus`**（名字带连字符，JDBC 也是这个库名）。WTA、SnailJob、Warm-Flow、Snail AI 的表**挤在这一间**，不按模块再拆库。
- 配套库房牌子写 **`nacos`**。只由 `60-cde-nacos.sql` 初始化。不得把 Nacos 十张表建进 `wta-plus`。

进业务库房要按六张清单的数字顺序上货。本课口试只把 **10 和 50** 认成 NAMEWTA 自己的货架图纸和货物清单。20/30/40 是厂商送来的整箱家具，拆箱放进同一间 `wta-plus`；60 是另一间库房的说明书。

**类比失效边界：** 「一座仓库」**不**等于「一张表一个库」。失效点有五：

1. 六份脚本都要 Git 跟踪，空环境六份都跑；口试格子却只把**自有产品**钉在 10/50。
2. 50 里能看见 `create temporary table`，那不是产品表混进了数据文件，是回填前的预检夹具。
3. 30 里已经住着 `flow_category`、`test_leave` 和工作流菜单 INSERT。那是上游快照夹带的存量，**不是**「新业务表可以写进 30」的许可证。
4. `@DS("slave")` 写在注释里，看起来像第二扇门。yaml 没挂 `slave` 时，揭开注释会因为 `strict: true` 直接报错。
5. 仓库一旦上过货，禁止把六张清单当「再刷一遍的油漆」。升级是两枚 Git Tag 之间的**差异稿**，稿子进被忽略的 `temp/release/`，不进 init 目录当第七份基座。

```text
空磁盘 / 空数据卷
        │
        v  mysql:8.4.9
   ┌─────────────────────────────────────────┐
   │  业务库 wta-plus                         │
   │    10  自有结构（70 张表）               │
   │    20  SnailJob 上游（23 张 sj_*）      │
   │    30  Warm-Flow 上游（11 张，含夹带）  │
   │    40  Snail AI 上游（23 张 sai_*）     │
   │    50  自有数据（种子/菜单/回填/DSL）    │
   └─────────────────────────────────────────┘
   ┌─────────────────────────────────────────┐
   │  配套库 nacos  ← 仅 60（10 张表）        │
   └─────────────────────────────────────────┘
        │
        v  运行时
   jdbc …/wta-plus   门牌 primary = master
   @DS 换门牌（今天没有第二扇活门）
   @DSTransactional 一趟采购一张收据
```

## 核心概念与机制

### 直觉讲解

小孩子搭积木城堡：底座图纸只有一张，积木清单只有一张。底座图纸改了，城堡形状变；清单改了，城堡里出现哪些小人变。你不会在说明书第 20 页偷偷画一个自家阳台，也不会把阳台图纸写进「小人清单」。NAMEWTA 的底座图纸是 `10-cde-base-ddl.sql`，小人清单是 `50-cde-base-dml.sql`。

进仓库的门上钉着一块铜牌：**master**。大家默认走这扇门。有人在树上练习卡（`TestTreeServiceImpl`）用铅笔写了「也可以走 slave」，但仓库没给 slave 装门。铜牌目录写在 admin 的 yaml 里，不写在某张 Mapper 上。

你进门买一篮子东西（改公告、改档案、改第三方端点），出门要一张**收据**，保证篮子里的东西要么全买成、要么全放回。这张收据的印泥是 dynamic-datasource 的 `@DSTransactional`，不是 Spring 那枚旧章 `@Transactional`。新业务盖新章。旧楼层（workflow、部分 system）还盖着旧章，那是棘轮，不是样板。

**类比失效处：** 积木图纸不是每天撕一页往日记末尾贴的「迁移日志」。10 和 50 永远是**当前完整可重建**的样子。已有城堡要加一根柱子，你比较旧图纸和新图纸，写出只动那根柱子的纸条，先在沙盘演练，再动真城堡。把整本新图纸往真城堡上重放，会把客人已经摆好的家具砸掉。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| MySQL 8.4 基座 | MySQL 8.4 baseline container | 当前唯一支持并验收的数据库；Compose 镜像 `mysql:8.4.9`；业务库 `wta-plus`，配套库 `nacos` |
| 六文件基座 | six-file init baseline | `release-artifacts/docker/infrastructure/mysql/init/` 恰好六份 Git 跟踪 SQL，空环境按文件名前缀 `10→20→30→40→50→60` 执行 |
| 自有表基座 | owned-table baseline / `C:mysql-base` | NAMEWTA 自己的产品 `CREATE TABLE`（含索引、约束、注释）只直接改 `10-cde-base-ddl.sql` |
| 自有数据基座 | owned-data baseline | 初始化数据、菜单、权限串、回填只直接改 `50-cde-base-dml.sql` |
| 存储点 MySQL | store-at-MySQL / `D:存储点 MySQL` | 新业务表、新列、新种子/菜单的落点：结构进 10，数据进 50；不进模块 `script/`，不进 20/30/40/60 |
| 上游快照 | upstream snapshot | `20` job / `30` workflow / `40` ai / `60` nacos：厂商或配套 schema；无产品需求不改；不是自有表登记处 |
| 完整可重建 | rebuildable current baseline | 10/50 始终表示「今天空库应长成什么样」，不是按日期无限追加的 migration 条 |
| DSL 块 | named DML block | 50 后半带 `NAMEWTA-*-DSL/DML-*` 标识的回填/补偿；fresh 初始化会执行；**不是**已有库的升级步骤 |
| 预检临时表 | preflight temporary table | `create temporary table …_preflight` + `CHECK`：前置不符则本块失败，避免半写入；不是产品 DDL |
| 差异稿 | tag-diff SQL | 已有库：源 Git Tag × 目标 Git Tag 比较六份基座，形成临时 SQL；写入被忽略的 `temp/release/`，不提交为第七份 |
| 动态数据源 | dynamic-datasource 4.5.0 | `wta-common-mybatis` 引入 `dynamic-datasource-spring-boot4-starter`；门牌目录在 `wta-admin` 的 `spring.datasource.dynamic` |
| 主库门牌 | `primary: master` | 当前唯一启用的数据源键；JDBC 指向库 `wta-plus`；`strict: true` 时未知键直接失败 |
| `@DS` | `@DS("name")` | 换门牌：`name` 必须是 yaml 里的数据源键；由该事务内**被代理**的 public service/mapper 持有 |
| `@DSTransactional` | DS transaction | 业务事务边界；默认 `rollbackFor = Exception.class`、`propagation = REQUIRED`。layered 贴 UseCase 命令法；classic 贴 ServiceImpl 命令法 |
| `@DsTxEventListener` | DS tx event listener | `@DSTransactional` 提交后再干活（缓存、唤醒）。不要用 Spring `@TransactionalEventListener(fallbackExecution = true)` 冒充 AFTER_COMMIT |
| 七字段基线 | PERSIST-003 seven columns | 新建自有表以 `test_demo` 的 `version/create_dept/create_time/create_by/update_time/update_by/del_flag` 为基线 |
| 棘轮 | Ratchet | `MIG-BE-DS-TX`：存量 Spring `@Transactional` 触及再迁；`MIG-BE-DDL-BASE`：旧表缺七字段不借机全仓补列 |

**Owned-table baseline 的检验：** 另一台空库、下一班值班的人，只靠 Git 里的 10 和 50，能不能长出这张 NAMEWTA 表和它的种子？能，才叫基座。模块旁边再藏一份方言，空库长不出来，那份就不是权威。

**`@DS` 所有权的检验：** 谁有权发明数据源名字？只有 admin yaml。谁有权换门牌？被 Spring AOP 代理到的 public 方法，且名字已在 yaml 挂上。谁有权开收据？layered 的 UseCase 命令、classic 的 ServiceImpl 命令。Controller、DAO、private/self-invocation **没有**这枚章。

**10 ≠ 50。** 10 是形状，50 是货物。形状写进 50、货物写进 10，空库要么缺表，要么缺行，口试这一格直接不及格。

**Pointer from L-005 ≠ 本课。** L-005 认「10/50 是存储格」。本课认「怎么改、怎么初始化、怎么升级、门牌归谁」。

### 机制/因果链

#### 1. 目录合同：恰好六份，数字即顺序

磁盘路径：`release-artifacts/docker/infrastructure/mysql/init/`。2026-09-17 listing 正好六份，没有第七份：

| 文件 | 行数（约） | 角色 | 本课认不认成自有基座 |
| --- | ---: | --- | --- |
| `10-cde-base-ddl.sql` | 1725 | `wta-plus` 完整最新结构 | **认。** 产品 DDL 只改它 |
| `20-cde-job.sql` | 540 | SnailJob `sj_*` 快照（CREATE+INSERT 混排） | 邻居。无产品需求不改 |
| `30-cde-workflow.sql` | 340 | Warm-Flow `flow_*` 快照；夹带 `flow_category` / `flow_spel` / `test_leave` / 工作流菜单 | 邻居。新 NAMEWTA 表不要再往这里塞 |
| `40-cde-ai.sql` | 574 | Snail AI `sai_*` 快照 | 邻居 |
| `50-cde-base-dml.sql` | 1805 | `wta-plus` 完整最新数据 | **认。** 产品 DML 只改它 |
| `60-cde-nacos.sql` | 196 | 独立库 `nacos`，上游 2.5.4 字节级 vendoring | 另一间库房。Nacos 表不进 `wta-plus` |

`release-manage.sh stage-mysql` **只校验**：六份都存在、非空、可读；`find … -name '[0-9][0-9]-*.sql' | sort` 的名字和顺序必须恰好这六个。它不复制、不改写、不执行 SQL。多一份 `70-hotfix.sql`，校验失败。

#### 2. 10：七十张自有结构，零条 INSERT

文件头两行口试要能指：

```text
-- NAMEWTA / WTA 业务库结构快照（完整最新基座，直接修改本文件）
-- 来源：10-wta-base.sql 的 CREATE TABLE + 50-namewta-ddl.sql 的 CREATE（ALTER 已折入平台表）
```

「ALTER 已折入」的意思是：你在 10 里看不到 `ALTER TABLE`。列要长在哪，直接改 `CREATE TABLE`。这是完整基座，不是「先 CREATE 再追 ALTER」的流水账。

2026-09-17 前缀盘点（70 张，全部 `engine=innodb` 一类）：

| 前缀 | 张数 | 例子 |
| --- | ---: | --- |
| `sys_*` | 26 | 用户/角色/菜单/Client/OSS/字典/开放凭据/迁移批次 |
| `profile_*` | 24 | 个人/企业档案、材料、核身、转移 |
| `notify_*` | 11 | 公告、意图、投递、Outbox、渠道账号 |
| `third_*` | 5 | provider / endpoint / credential / invocation / statistic |
| `test_*` | 3 | `test_rich_text`、`test_demo`、`test_tree` |
| `sso_*` | 1 | `sso_authorization_code` |

抽查 `test_demo`（约 L1691）：七个基础字段 `version / create_dept / create_time / create_by / update_time / update_by / del_flag` 齐。这是 **PERSIST-003** 的建表尺子。

抽查 `sys_oss_config.access_policy` 注释：**`0=PRIVATE`，`2=PUBLIC_READ`**。没有合法的「1 当默认公开」。50 前半种子曾写入 `'1'`，同文件后半 `NAMEWTA-OSS-ACCESS-DML-001` 再把非 0 回填成 `'0'`。口试要能说：种子历史和补偿都在 50 里，不要另开 migrate 把补偿撕走。

抽查 `sys_dept` / `sys_user`：**没有** `version`。这是 `MIG-BE-DDL-BASE` 棘轮现场。新表抄 `test_demo` 的七字段，不要抄 `sys_user` 的缺列当「平台惯例」。

主键命名：PERSIST-004 说新表不要裸 `id`，尺子是 `flow_category.category_id`。`test_demo.id` 是七字段样板，**不是**主键命名样板。两把尺子别混。

#### 3. 50：货物清单 + 带标识的回填，不是升级剧本

文件头：

```text
-- NAMEWTA / WTA 业务库数据快照（完整最新基座，直接修改本文件）
```

后半还有一段必须会指（约 L391–L397）：

```text
-- 本文件是可直接修改的当前完整 MySQL 8.4 数据基座，仅用于全新数据库初始化。
-- 历史变更标识和执行说明只保留追溯语义，不是已有数据库的升级步骤。
-- 已有数据库必须按源/目标 Git Tag 生成并评审差异，禁止重放本文件。
```

前半是平台种子：部门树、登录用户 **`WTA`**（`user_id=1761100000000000001`）、岗位、超管角色、系统菜单、OSS 五行配置、两个 Client 等。fresh 库的管理员账号从这里长出来，不是从 Nacos，也不是从 Java `CommandLineRunner`。

后半是带变更标识的 DSL/DML 块。2026-09-17 能点名的包括：

`NAMEWTA-BASE-DSL-001`（登录域/Client/角色菜单）、`NAMEWTA-BASE-DSL-002`（历史补偿，当前基座不再执行旧菜单块）、`NAMEWTA-OSS-NOTIFY-DSL-001`、`NAMEWTA-BASE-DSL-004`、`NAMEWTA-PASSWORD-DSL-001`、`NAMEWTA-RUNTIME-GEN-RETIRE-DML-001`、`NAMEWTA-OPENAPI-CREDENTIAL-DML-001`、`NAMEWTA-OSS-ACCESS-DML-001`、`NAMEWTA-NACOS-CONSOLE-DML-001`、`NAMEWTA-ADMIN-RUNTIME-RECONCILE-DML-001`、`NAMEWTA-PROFILE-DML-001`、`NAMEWTA-THIRD-MENU-DML-001`、`NAMEWTA-RICHTEXT-DML-001`、`NAMEWTA-SSO-DSL-001`、`NAMEWTA-SSO-MENU-001`。

这些块**看起来像迁移**，fresh 初始化时它们就是 50 的一部分。已有库若再跑整份 50，会把固定主键的 INSERT 砸进已经有客人数据的表。所以文件头才把「重复执行说明」降成追溯语义。

预检怎么停住半写入：以 `NAMEWTA-PASSWORD-DSL-001` 为例——`create temporary table …_preflight (preflight_ok … check (preflight_ok = 1))`，再 `insert … select if(一串 AND, 1, 0)`。前置不符时 CHECK 失败，本块在改 `sys_config` **之前**停住。四张临时表名字都以 `_preflight` 结尾。口试：看见 `CREATE` 先问「产品表还是预检夹具」。

密码块另外两句口试：旧键 `sys.user.initPassword` 首次执行时被随机化并写进 remark；**回滚说明写死「绝不修改 `sys_user.password`」**。登录哈希和策略键不是同一件货。

`docs/fm/sql/mysql.sql.ftl` 只吐菜单 DML **片段**。渲染结果必须并进 50，禁止生成 `FooMenu.sql` 当第七份部署文件。NAMEWTA 不生成 Oracle/PG/SQL Server 脚本。

#### 4. 空库怎么长出来：两条河，一个事实源

**河 A — Compose 第一卷。** `docker-compose-infrastructure.yml` 把 `./infrastructure/mysql/init` 只读挂到 `/docker-entrypoint-initdb.d`。官方镜像只在**数据目录为空**时按文件名顺序执行 `.sql`。环境变量会先建 `MYSQL_DATABASE=wta-plus` 和 `MYSQL_USER`。`60` 自己 `CREATE DATABASE nacos; USE nacos;`，所以 Nacos 表不会进 `wta-plus`。这一河**不会**跑 `init-mysql-container.sh` 末尾那段用 `.env` 覆盖 MinIO 账密的 `UPDATE`；50 自己的 OSS-ACCESS 块已经把 `access_policy` 打成 `'0'`。

**河 B — 已有空容器、库还不存在。** `init-mysql-container.sh`：

1. 拒绝库名不是 `wta-plus`。
2. 拒绝已存在的 `wta-plus` 或已存在的应用账号 `user@%`。
3. `CREATE DATABASE wta-plus … utf8mb4_general_ci`，再 `CREATE USER` + `GRANT ALL ON wta-plus.*`。
4. 按六份顺序 `mysql --database=wta-plus < file`（60 内部会 `USE nacos`）。
5. 用 `.env` 的 MinIO 值更新 `sys_oss_config` 的 `minio`/`image`：`access_policy='0'`，且**唯一** `status='Y'` 必须是 `minio`。
6. 断言 `wta-plus` 表数量等于脚本里的 `EXPECTED_TABLES`（2026-09-17 源码写 **125**；CREATE 清单是 **127**。口试数 CREATE，不要把过期断言背成 schema）。
7. 失败只 `DROP` 本次新建的库和账号，不动别人的库。

默认容器名是 `namewta-data-mysql`；compose 里是 `namewta-mysql`。口试：调用脚本时 `--container` 必须对上真实容器，不要背错名字。

两条河都**只读消费** Git 里那六份。`stage-mysql` 是第三件事：上货前点名，不上货。

#### 5. 已有库：禁止重放，只比 Tag

PERSIST-006 / SEC-004 / 50 文件头同一句话：已有环境不得把基座再跑一遍。升级动作是：

1. 写下源 Git Tag、目标 Git Tag。
2. 备份。
3. 比较两 Tag 下**全部六份**（不要只 diff 10/50，20/30/40/60 也可能变）。
4. 形成临时差异 SQL，评审，隔离副本演练升级和回滚。
5. 差异稿、账密、备份位置、执行记录进被忽略的 `temp/release/`，**不提交进 init/**。

破坏性变更（删列、改类型、改唯一键、改 `del_flag` 语义）要有 expand / migrate / contract 或明确回滚。ARCH-004：schema 和初始化 SQL 都是兼容合同。

#### 6. `@DS` 所有权：门牌目录、换牌、收据

依赖：父 POM `dynamic-ds.version=4.5.0`；真正引进 starter 的是 `wta-common-mybatis`。业务模块不要自己再引一套。

门牌目录（`application-dev.yml` / `application-prod.yml` 同构，`application-local.yml` 更短）：

```text
spring.datasource.dynamic.primary: master
spring.datasource.dynamic.strict: true
spring.datasource.dynamic.datasource.master:  jdbc:mysql://…/wta-plus …
# slave / oracle / postgres / sqlserver 整段注释
```

`strict: true` = yaml 里没有的名字，`@DS("那个名字")` 直接失败。今天没有第二扇活门。

换牌注解 `@DS("slave")` 在磁盘上只作为**注释**出现：`TestTreeServiceImpl` 类上与 `queryList` 上各一处；`TestBatchController` 三处。揭开它却不在 yaml 挂 `slave`，查询会炸。本课不把「解开注释」当作业。

收据注解 `@DSTransactional` 的所有权按登记表：

| 模块模式 | 谁持收据 | 2026-09-17 活样本 |
| --- | --- | --- |
| layered | UseCase 的 public 命令 | `NotifyNoticeUseCase.save/publish/retract/remove`；third 三个 UseCaseImpl；profile 各 UseCaseImpl |
| classic（新改/实质修改） | ServiceImpl 的 public 命令 | 如 OSS / 凭据服务上的 `@DSTransactional` |
| 禁止 | Controller、DAO、Mapper、private、自调用 | 代理进不去，章等于没盖 |
| 棘轮 | 存量 Spring `@Transactional` | `FlwDefinitionController`、`FlwTaskServiceImpl`、`TestLeaveServiceImpl`、`SysDeptServiceImpl`、`SysRegisterService` |

提交后的副作用用 `@DsTxEventListener`。活样本：`NotifyOutboxWakePublisher.publishAfterCommit` 显式 `phase = AFTER_COMMIT`；`OssConfigChangeListener.refreshOssConfig`。`OnlineUserCleanListener` 仍是 Spring `@TransactionalEventListener(fallbackExecution = true)`——那是棘轮，不是新代码样板：没有 Spring 事务时 fallback 会**立刻**执行，不是「等提交」。

跨库：dynamic-datasource 能在一趟收据里协调多个 JDBC connection，**不是** XA/Seata。不能靠加一个 `@DS("slave")` 宣称分布式强一致。今天连 slave 都没挂，更不要口头发明双写。

layered 校验脚本会抓「UseCase 以外的 `@DSTransactional`」和「新模块还在用 Spring `@Transactional`」。口试：事务章跟登记表走，不跟「谁离 Mapper 近」走。

## 图、表或文本图

**图题 / caption：** 空库上货顺序与运行时门牌。alt：六份脚本进入 wta-plus/nacos，应用只认 master。

```text
Git init/ 六份 SQL
        │  stage-mysql 点名（不执行）
        v
  河 A 空数据卷 / 河 B init-mysql-container.sh
        │
        ├─ 10 CREATE ×70     ─┐
        ├─ 20 sj_* ×23        │  业务库 wta-plus
        ├─ 30 flow_* + 夹带   │  （脚本断言 125；CREATE 合计 127）
        ├─ 40 sai_* ×23       │
        └─ 50 种子+DSL        ┘
        └─ 60 USE nacos ×10     配套库 nacos

应用启动
  dynamic.primary = master ──► Hikari ──► jdbc …/wta-plus
  命令方法 @DSTransactional ──► 同一收据
  （可选）被代理方法 @DS("yaml键") ──► 换门牌
  提交后 @DsTxEventListener ──► 缓存 / Outbox 唤醒
```

**文字等价物：** 先点名六份文件，再按数字上货。NAMEWTA 自己的木板只钉在 10，自己的货物只写在 50。20/30/40 是同一间库房里的厂商家具。60 去另一间叫 nacos 的库房。应用进门只认铜牌 master。换铜牌要 yaml 先有那块牌；开收据是 UseCase 或 classic ServiceImpl 的事。提交之后才允许发「货已进仓」的广播。已有库房禁止把六张清单当油漆重刷，只能比两枚标签的差异。

**图的边界：** 不画 Redis 键、不画 MinIO PUT、不画 Mapper XML。那些是别的格子。本图只回答「表从哪份 SQL 来」和「运行时走哪扇门」。

**第二张图：加一列 `color` 时纸落在哪。**

```text
  要进表吗？──是──► 改 10 的 CREATE（及 entity）
       │
       v 要进种子/菜单吗？──是──► 改 50（不要新文件）
       │
       v 已有环境？──是──► Tag 差异稿 + 备份 + 演练
                       └──否──► 空库重放六份即可看到新列
       │
       v HTTP / OpenAPI / 页面     ← 交还 L-005 的合同顺序
```

**文字等价物：** 存储点这一格在问「列写进哪份 SQL」。答「写进 Mapper XML」或「写进 30」都算走错货架。空库靠基座重建；已有库靠差异稿。HTTP 怎么暴露这列，是 L-005 的河，本课只保证货架上真有这根木板。

## 正例、反例与边界

**正例 1：** 新产品表。`CREATE TABLE` 写进 `10-cde-base-ddl.sql`，带中文表/列注释，带七个基础字段，主键按模块前缀而不是裸 `id`。种子和菜单写进 `50-cde-base-dml.sql`。后端测试用 `SqlBaselinePaths.file("10-cde-base-ddl.sql")` 读父仓，不在 `wta-demo` 旁再放一份。

**正例 2：** `test_demo` 的 CREATE 在 10，`INSERT INTO test_demo` 在 50。L-005 已经用它当字段方言样本；本课用它当「结构/数据分家」样本。

**正例 3：** 密码 DSL 的预检临时表。前置用 CHECK 卡住，再随机化 `sys.user.initPassword`、插入 `sys.user.passwordPolicy`。fresh 跑 50 会执行；已有库要升级时，比 Tag，不要整文件重放。

**正例 4：** OSS 访问回填。50 前半种子曾写 `access_policy='1'`，同文件 `NAMEWTA-OSS-ACCESS-DML-001` 用「默认配置必须恰好一行」的哨兵 INSERT（冲突主键）保证异常时在 UPDATE 前失败，再把非 0 打成 `'0'`。河 B 还会用 `.env` 把 `minio` 设成唯一启用的 PRIVATE 默认。默认私桶不是 `PUBLIC_READ`（`'2'`）。

**正例 5：** layered 收据。`NotifyNoticeUseCase` 查询方法不贴事务；`save` / `publish` / `retract` / `remove` 贴 `@DSTransactional`。Controller 只打电话给 UseCase。

**正例 6：** `stage-mysql` 点名六份。缺文件、空文件、多一个 `15-foo.sql`、顺序被改名打乱，都会失败。这是发布资产的门禁，不是「执行升级」。

**反例 1：** 在 `20-cde-job.sql` 或 `backend/wta-modules/wta-xxx/script.sql` 新建 NAMEWTA 业务表。自有结构只有 10。

**反例 2：** 在 50 里 `CREATE TABLE` 一张业务表，理由是「反正也是 SQL」。产品表仍只在 10。临时预检表不是缺口。

**反例 3：** 新增 `release-artifacts/.../init/55-my-change.sql` 或 `mysql/migrate/`。PERSIST-006 禁止第七份。已有库的差异稿去 `temp/release/`。

**反例 4：** 对已经有客人数据的 `wta-plus` 再跑 `init-mysql-container.sh` 或重放 50。脚本应拒绝已存在的库；若有人绕过脚本手动 source，固定主键 INSERT 会撞车或覆盖。

**反例 5：** 新 UseCase 贴 Spring `@Transactional`，或把 `@DSTransactional` 贴到 DAO/Mapper。所有权错位。`ProfileStructureContractTest` 会把 Spring 事务当违规。

**反例 6：** 解开 `@DS("slave")` 却不在 yaml 挂 `slave`。`strict: true` 下这不是「读写分离」，这是启动后第一查询爆炸。

**反例 7：** 在 Controller 上贴 `@DSTransactional`，以为 HTTP 方法就是事务边界。代理边界在 UseCase / ServiceImpl。

**反例 8：** 把 30 里已有的 `test_leave` 当成「workflow 相关表都写 30」。新 NAMEWTA 表仍然进 10；30 无产品需求不改。

**反例 9：** 宣称项目「也支持 PostgreSQL」，因为 yaml 注释里有 postgres 样例。产品基座只有 MySQL 8.4。

**反例 10：** 手改 60 里 vendoring 的 Nacos schema 字节。60 头写了上游 Tag `2.5.4` 和 SHA-256；要换就整份替换并改 digest，不要本地改一列。

**边界 1 — 本课三格，不是六份内部课。** 20/30/40/60 的表字段不在本格 covered。口试能指「邻居、夹带、另一间库房」即可。

**边界 2 — 表计数。** 保护脚本 `EXPECTED_TABLES=125` 统计的是 `wta-plus` 的 `information_schema.TABLES`。60 的 10 张表在 `nacos`，不计入。CREATE 清单 10+20+30+40=127 与断言 125 在 2026-09-17 **不一致**。以 CREATE 为准讲结构；以脚本为准讲「它现在会断言什么」。不要用本课去改脚本。

**边界 3 — 容器名。** compose `container_name: namewta-mysql`；`init-mysql-container.sh` 默认 `namewta-data-mysql`；Nacos 相关脚本又用 `namewta-mysql`。口试说「以实际容器为准，脚本参数对上」。

**边界 4 — 七字段尺子不是全仓现状。** `sys_*` 大量缺 `version`；`test_leave` 无 `version`/`del_flag`。新表立即执行 PERSIST-003；旧表走棘轮。

**边界 5 — `@DS` 今天几乎是「所有权课」不是「切换课」。** 活配置只有 master。课要你能指出谁可以发明第二扇门（yaml），不是要你现场接从库。

**边界 6 — 事务事件。** Outbox 唤醒、OSS 缓存刷新是提交后副作用，本课只认印泥是 `@DsTxEventListener`。Outbox 领取算法是 L-052；Dispatcher 是 L-082。

**边界 7 — 测试读基座。** `SqlBaselinePaths` 从 `user.dir` 向上找 `mvnw`，再 `../release-artifacts/docker/infrastructure/mysql/init`。独立检出后端时用 `-Dnamewta.sql.root=…`。后端不保存 SQL 副本。

**边界 8 — 密钥。** 六份 SQL 不写运行密钥。河 B 的 MinIO 账密来自权限 `0600` 的 `.env`。不要把 `application-local.yml` 里的口令抄进笔记当「基座的一部分」。

**边界 9 — 工作流菜单在 30。** `insert into sys_menu` 的工作流段住在上游快照里。产品菜单的**默认落点**仍是 50（通知、档案、SSO、富文本、third 都在 50 的 DSL）。不要因为 30 已有菜单就往 30 加 NAMEWTA 新菜单。

**边界 10 — Mapper / XML。** 运行时 SQL 的所有权是 L-083。本课只划：运行时查询不是初始化基座；两套 SQL 不要抄错抽屉。

## 变式与迁移

1. **和 L-005 对照。** L-005 认四张合同和「表先行」。本课把 SQL 合同拆成：10 形状、50 货物、六份顺序、已有库差异稿、`@DS` 门牌。不要用本课再讲 OpenAPI 指针。
2. **和 L-001 对照。** L-001 只说柜子是 MySQL 8.4、权威在 release-artifacts。本课把柜子门打开。
3. **和 L-003 对照。** 事务章跟 layered/classic 登记走：UseCase vs ServiceImpl。不要给 demo 加 UseCase 只为了贴 `@DSTransactional`。
4. **和 L-025 对照。** OSS 默认私桶、`access_policy=0`、唯一启用 minio，是 50 + 河 B 的数据事实。对象直传走廊不是本格。
5. **和 L-083 对照。** 「谁持 Mapper」是下一课。本课只保证表在 10 里存在。
6. **变式：只改菜单文案/图标。** 仍是 50。`sys_menu.icon` 新 NAMEWTA 展示菜单优先 `tabler:name`，功能节点 `#`。不要新图标表。
7. **变式：只改查询，不改表。** 不动 10/50。那是 Mapper/QueryBuilder，L-083。
8. **变式：要接只读从库。** 先在 admin yaml 挂上 `slave` 的 JDBC，再在**被代理**的读方法上 `@DS("slave")`，读路径仍要有测试。不要先揭 demo 注释。跨库写入另开一致性方案。
9. **变式：存量 Spring 事务。** 触及 `FlwTaskServiceImpl` 一类文件时，把该调用链迁到 `@DSTransactional` + 需要的 `@DsTxEventListener`，验证代理/回滚。不发动无需求的全仓替换（`MIG-BE-DS-TX`）。
10. **变式：已有库加列。** 改 10 的 `CREATE`（让下一台空库正确），再从 Tag 差异抽出 `ALTER` 差异稿给已有库。不要只改 10 却对已有库重放 10。
11. **迁移口诀：** 先问空库还是已有库 → 再问形状还是货物 → 形状进 10、货物进 50 → 邻居快照无产品需求不碰 → 门牌在 yaml、收据在 UseCase/ServiceImpl → 已有库只走 Tag 差异。跳步会出现「模块里有 SQL、空库没有表」「解开 slave 注释就炸」「把 50 当 migrate 重放」。

## 常见误区

1. **「OBJ-85 是六份文件每一张表都 covered。」** 自有基座是 10/50。其余是邻居。
2. **「六份都可以加 NAMEWTA 新表。」** 只有 10。
3. **「50 里有 CREATE 就说明规则破了。」** 看是不是 `_preflight` 临时表。
4. **「DSL 块就是 migrate，已有库也要按块重跑。」** 文件头禁止。已有库比 Tag。
5. **「后端模块放一份 SQL 更方便。」** 空库不读那份。权威在父仓六份。
6. **「`@DS` 贴在 Mapper 上才算用了动态数据源。」** 今天连第二扇门都没有。所有权先是 yaml 的 `master`。
7. **「`@DSTransactional` 就是 Spring `@Transactional` 换了个名。」** 印泥不同；事务事件监听也不同。混用同一调用链是违规。
8. **「Controller 开事务更直观。」** 登记表不让。
9. **「strict: false 就能随便 `@DS`。」** 磁盘是 `true`。未知键应失败。
10. **「yaml 注释里的 oracle 说明还支持 Oracle。」** 不支持。
11. **「`test_leave` 在 30，所以请假是上游表。」** 它是 NAMEWTA 示例表住错了抽屉的存量。新表不要学它。
12. **「`EXPECTED_TABLES=125` 背下来。」** 先数 CREATE。断言可能过期。
13. **「compose 挂了 init 目录，每次 up 都会重建库。」** 官方入口只在数据目录为空时跑。不要 `docker compose down -v` 当日常动作。
14. **「init 脚本失败会把整台 MySQL 清掉。」** 只丢本次新建的 `wta-plus` 和本次新建的应用账号。
15. **「Nacos 表建在 wta-plus 更省事。」** 60 明确 `CREATE DATABASE nacos`。PERSIST-006 禁止混进业务库。
16. **「默认 OSS 种子是公开桶。」** 列注释只有 0/2；50 后半把非 0 打成 0；河 B 再强制 minio PRIVATE。
17. **「登录用户 WTA 是 Nacos 配的。」** 种子在 50 的 `sys_user`。
18. **「本课包含 Redis 会话丢失。」** 那是 L-084。
19. **「改了 10 就等于已有库加列。」** 只等于下一台空库有列。
20. **「动态数据源等于读写分离已经上线。」** 没有 slave 门牌，就没有读写分离。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 列出 `release-artifacts/docker/infrastructure/mysql/init/` 六个文件名。圈 10 与 50 为自有基座。打开 10 文件头「完整最新基座，直接修改本文件」；确认没有 `INSERT`、没有 `ALTER TABLE`。数一张你熟悉的表（`test_demo` 或 `notify_outbox`）是否在 10。
2. 打开 50 文件头与 L391 附近「仅用于全新数据库初始化 / 禁止重放」。找到一处 `create temporary table …_preflight`，标成预检不是产品表。找到 `INSERT` 用户 `WTA`。找到 `NAMEWTA-OSS-ACCESS-DML-001` 把 `access_policy` 打成 `'0'`。
3. 打开 30，圈 `create table test_leave` 与 `create table flow_category`。在纸上写「存量夹带，不是新表登记处」。
4. 打开 `release-artifacts/scripts/release-manage.sh` 的 `stage_mysql_init`，圈六份数组和 `find | sort` 必须 1:1。打开 `init-mysql-container.sh`，圈拒绝已存在库、`EXPECTED_TABLES`、MinIO `access_policy='0'`、默认容器名。打开 compose，圈 `image: mysql:8.4.9` 与 `container_name: namewta-mysql`。
5. 打开 `application-dev.yml` 的 `spring.datasource.dynamic`：圈 `primary: master`、`strict: true`、被注释的 `slave`。打开 `TestTreeServiceImpl` 圈注释掉的 `@DS("slave")`。打开 `NotifyNoticeUseCase` 圈四条命令上的 `@DSTransactional`。打开 `NotifyOutboxWakePublisher` 圈 `@DsTxEventListener(phase = AFTER_COMMIT)`。不要改这些文件。

## 总结、词汇表与下一步

- **宏观一座 MySQL 8.4 仓库：** 唯一验收的数据库；业务库 `wta-plus`；配套库 `nacos`；空库按六份数字前缀重建。
- **`(c) C:MySQL 8.4`：** 柜子本身——镜像 8.4.9、init 目录、库名。不是 Redis，不是 MinIO。
- **`(c) C:mysql-base`：** 自有结构权威 10（70 张 CREATE），自有数据权威 50（种子 + DSL + 预检临时表）；动态数据源门牌目录在 admin yaml 的 `master`。
- **`(d) D:存储点 MySQL`：** 新业务表/列进 10，新种子/菜单/回填进 50。已有库走 Tag 差异稿，禁止重放基座。chain 别名 `happy-store-mysql` 是同一格。
- **`@DS` 所有权：** 名字归 yaml；换牌归被代理的 public 方法；收据归 UseCase（layered）或 ServiceImpl（classic）；提交后副作用归 `@DsTxEventListener`。今天没有第二扇活门。
- **邻居不是登记处。** 20/30/40/60 存在且必须跑；`test_leave` 住在 30 不构成新表许可证。

词汇表：`10-cde-base-ddl.sql` / `50-cde-base-dml.sql` / `wta-plus` / `nacos` / `mysql:8.4.9` / owned-table baseline / DSL 块 / preflight / `stage-mysql` / `init-mysql-container.sh` / `primary: master` / `strict` / `@DS` / `@DSTransactional` / `@DsTxEventListener` / `SqlBaselinePaths` / `MIG-BE-DS-TX` / `MIG-BE-DDL-BASE` / PERSIST-006。

下一步：L-083 才把 Mapper/DAO 所有权说完。L-084 才把会话写进 Redis。L-082 才是通知同步入口。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-009 | `.agents/skills/engineering-standards/references/project/00-project-profile.md` | 六份基座唯一事实源；自有表进 10、数据进 50；dynamic-datasource 4.5.0 | 「事实来源」「排除与冻结」 | 2026-09-17 |
| S-010 | `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql`、`50-cde-base-dml.sql` | 自有结构/数据权威；文件头「完整最新基座」 | 目录六份；10 L1–L4；50 L1–L4、L391–L397 | 2026-09-17 |
| S-L005-01 | 子课 L-005；course.md OBJ-85；coverage-matrix；chain L-085 | 10/50 为存储格；DSL/升级推给本课；三格符号 | L-005 SQL 节；OBJ-85；矩阵 Container/Component/(d) | 2026-09-17 |
| S-L085-01 | `init/` 六份 listing 与各自文件头 | 恰好六份；20/30/40 上游；60 独立 `nacos` + digest | `20` 无产品头；`30` warm-flow 注释；`40` Snail AI；`60` L1–L16 | 2026-09-17 |
| S-L085-02 | `10-cde-base-ddl.sql` CREATE 清单 | 70 张；前缀 26/24/11/5/3/1；无 INSERT/ALTER；`test_demo` 七字段；`access_policy` 0/2 | CREATE 扫描；L1691；L327–L348 | 2026-09-17 |
| S-L085-03 | `50-cde-base-dml.sql` | WTA 种子；四张 `_preflight`；DSL 标识列表；OSS 回填；SSO 块 | L21；L554+；L828–L853；L1725+ | 2026-09-17 |
| S-L085-04 | `30-cde-workflow.sql` | `flow_category`/`test_leave`/工作流菜单夹带 | L168–L258；L260+ | 2026-09-17 |
| S-L085-05 | `docker-compose-infrastructure.yml`；`init-mysql-container.sh`；`release-manage.sh` `stage_mysql_init` | 镜像 8.4.9；挂 init；拒绝已有库；OSS PRIVATE；stage 只点名；容器名分叉；断言 125 | compose L8–L30；init 脚本 L10–L205；release-manage L424–L456 | 2026-09-17 |
| S-L085-06 | `persistence-transactions-and-ddl.md` PERSIST-001…007；`security-and-data.md` SEC-004 | 事务印泥、七字段、六文件合同、已有库差异、只支持 8.4 | PERSIST 全文；SEC-004 | 2026-09-17 |
| S-L085-07 | `application-{dev,prod,local}.yml`；`backend/pom.xml` `dynamic-ds.version`；`wta-common-mybatis/pom.xml` | `primary/master`、`strict`、slave 注释；starter 4.5.0 | datasource.dynamic 段；父 POM L32 | 2026-09-17 |
| S-L085-08 | `TestTreeServiceImpl.java`；`TestBatchController.java`；`NotifyNoticeUseCase.java`；`NotifyOutboxWakePublisher.java`；`OssConfigChangeListener.java`；`FlwTaskServiceImpl.java`；`OnlineUserCleanListener.java` | 注释 `@DS("slave")`；UseCase 收据；AFTER_COMMIT 监听；Spring 事务棘轮 | 各类注解 | 2026-09-17 |
| S-L085-09 | `SqlBaselinePaths.java`；`backend/README.md`；契约测试读 10/50 | 测试消费父仓基座；`-Dnamewta.sql.root` | `wta-admin/.../support/SqlBaselinePaths.java` | 2026-09-17 |
| S-L085-10 | `02-decisions-and-exceptions.md` `MIG-BE-DS-TX` / `MIG-BE-DDL-BASE`；`docs/fm/context-contract.md` | 事务棘轮；七字段棘轮；ftl 片段并进 10/50；UseCase 持 `@DSTransactional` | 状态表；fm 输出集成 | 2026-09-17 |
| S-L085-11 | `release-artifacts/README.md`；`mapper-and-sql.md`「运行时 SQL 与初始化基座」 | 唯一 SQL 事实源；fresh 顺序；已有库 Tag 差异；运行时 SQL ≠ 基座 | README「MySQL 单库初始化」 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：NAMEWTA 只认 MySQL 8.4 这一座仓库。空房间按六份带数字的清单上货，业务都进 `wta-plus`，Nacos 进另一间 `nacos`。属于我们自己的木板只钉在 `10-cde-base-ddl.sql`，属于我们自己的货物只写在 `50-cde-base-dml.sql`。10 里没有货物，50 里没有产品木板；50 里那些临时桌子只是进货前的安检，安检完就拆。20、30、40 是厂商整箱家具，30 箱子里还夹着请假表示例和工作流菜单，那不是让你继续往箱子里塞自家阳台。已经住了客人的房间禁止把六张清单当油漆重刷，只能比较两枚 Git 标签，写出差异纸条，先在沙盘演练。应用进门只认铜牌 `master`，铜牌目录写在 admin 的 yaml 里；`@DS` 是换铜牌的字，今天没有第二扇活门；`@DSTransactional` 是这一篮子采购的收据，layered 由 UseCase 盖章，classic 由 ServiceImpl 盖章，提交之后才允许广播。这就是宏观的 MySQL 8.4 基座、mysql-base 组件，以及存储点「业务表只进 10/50」。
