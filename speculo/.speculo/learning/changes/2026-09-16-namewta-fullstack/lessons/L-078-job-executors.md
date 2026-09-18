---
lesson_id: L-078
objective_ids: [OBJ-78]
claimed_cells:
  - A:AlipayBillTask
  - A:WechatBillTask
  - A:SummaryBillTask
  - A:TestAnnoJobExecutor
  - A:TestBroadcastJob
  - A:TestClassJobExecutor
  - A:TestMapJobAnnotation
  - A:TestMapReduceAnnotation1
  - A:TestStaticShardingJob
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: nine-workers-on-disk
    minutes: 11
  - segment: dag-and-dispatch-shapes
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-004, S-007, S-008, S-010, S-L002-01, S-L003-01, S-L078-01, S-L078-02, S-L078-03, S-L078-04, S-L078-05, S-L078-06, S-L078-07, S-L078-08]
---

# Lesson 078：宏观九个工人——`wta-job` 的 SnailJob 执行器入口，不是 HTTP 窗

## 学完你能做什么

打开 `backend/wta-modules/wta-job/src/main/java/org/namewta/job/snailjob/`，你能**口述这块宏观调度车间**：墙上挂的是九个**等人拍肩膀的工人**，不是给人敲的 HTTP 窗。口试名单就是矩阵 **(a)** 这一格，符号以**磁盘**为准：

**`A:AlipayBillTask` / `A:WechatBillTask` / `A:SummaryBillTask` / `A:TestAnnoJobExecutor` / `A:TestBroadcastJob` / `A:TestClassJobExecutor` / `A:TestMapJobAnnotation` / `A:TestMapReduceAnnotation1` / `A:TestStaticShardingJob`**

chain 把前四个当脊柱写进课表；矩阵把九个写在**同一行**。本课九个都要能指类。OBJ-78 原文只要你能指出：这些 SnailJob 执行器类**就是入口**，**不是** HTTP Controller。

2026-09-17 工作树先钉死**房间形状**（口试先数类，再数注解名）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `JobController` / `@RestController` / `@RequestMapping` | **没有。** `wta-job` 全模块搜不到这些注解 |
| `createJobService` / `web-domain-job` | **没有。** `frontend/packages/domains` 与 `web-domains` 都没有 job 包 |
| Java 类名 = 控制台执行器名 | **不一定。** `TestAnnoJobExecutor` 的注解名是 **`testJobExecutor`**，不是类名 |
| `TestClassJobExecutor` 也贴了 `@JobExecutor` | **没有。** 九个里唯一靠继承 `AbstractJobExecutor`，覆盖 `doJobExecute` |
| 九个工人自己去敲支付宝/微信 HTTP | **没有。** 账单三件套是写死的 `BillDTO`，不发外网 |
| `src/main/resources`、Mapper、XML、UseCase | **没有。** 模块只有 `src/main/java` |
| `wta-job` 依赖 `wta-api` | **没有。** POM 只有 `wta-common-json` + `wta-common-job` |
| 本课 Java 测试类 | **没有。** 2026-09-17 无 `src/test` |
| iframe `monitor/snailjob/index` 就是这些工人 | **不是。** 那是 L-024 外置板，嵌的是 **SnailJob 服务器后台** |
| `sj_job` 种子九行对应九个工人 | **不是。** 种子只有一行 `demo-job`，`executor_info='testJobExecutor'` |
| 本课把 `bundle-full` / `bundle-core` 标 covered | **不是。** 那是 OBJ-79 / L-079。本课只认工人类 |

本课**不宣称**你会对照 `wta-admin/pom.xml` 的 profile 把 job 插进/拔出 admin jar（L-079）、拆 `wta-snailjob-server` 厂商调度内核（Goal 范围外）、或把监控 iframe / `externalIntent('snail-job')` 当本格 covered（L-024）。`SnailJobConfig` 是 **common 里的客户端电闸**，用来解释工人为什么会醒，不是本课格子。

## 先把宏观地图放在桌上

L-002 已经把 `wta-job` 钉成「业务侧 Job 执行器」：只依赖 json + `wta-common-job`，只在 **bundle-full** 进 admin，并且画过**变式 D**：工人进的是 **admin 进程**，`wta-snailjob-server` 是 **另一个进程**。L-003 把 `wta-job` 登在 classic 列，并写明：它是 `snailjob/*` 执行器，没有业务 Controller、没有 Mapper、没有 `src/main/resources`；脚本 `--mode classic` 会因缺 resources 退出码 1，**失败不证明它不是 classic**。本课走进**同一间房的九个工人**，把「不是 HTTP」从一句话变成能指类的口试。

把调度想成工厂。大厅前面的办事窗是别的课（system / workflow / demo）。这间房没有窗。工人坐在后车间。另一栋楼里有个**调度主任**（SnailJob Server）。主任按自己的花名册点名：「`testJobExecutor`，干活。」admin 进程里的客户端把这句话递到对应的 Spring `@Component`。工人干完交一张 `ExecuteResult`。浏览器如果打开「任务调度」菜单，看到的是主任那栋楼的玻璃（iframe），不是这九个工人的脸。

2026-09-17 工作树：`wta-job/src/main/java/org/namewta/job/` 只有两层——`entity/BillDTO.java` 一张账单卡片，`snailjob/` 九个工人。`package-info.java` 只有包声明。

```text
浏览器
  │  没有 /job/* 业务窗
  │
  ├─ admin-web  iframe  monitor/snailjob/index     ← L-024 外置板
  │       VITE_APP_SNAILJOB_ADMIN
  │       看的是主任楼，不是工人
  │
  v
SnailJob Server 进程
  wta-extend/wta-snailjob-server
  SnailJobServerApplication
  表在 20-cde-job.sql（sj_*，上游快照）
        │  点名 executor_info
        v
admin 进程  NamewtaApplication（org.namewta 扫描）
  SnailJobConfig   snail-job.enabled=true 才 @EnableSnailJob
        │
        v  九个 @Component，本课格子
  org.namewta.job.snailjob.*
```

往下走不要跳层：

```text
调度主任点名（厂商协议，不是我们的 @GetMapping）
    └─ SnailJob 客户端（wta-common-job）
          └─ 工人方法：jobExecute / doJobExecute / @MapExecutor / @ReduceExecutor
                └─ 交 ExecuteResult.success 或 failure，或抛 RuntimeException
```

**类比失效边界：** 「宏观九个工人」**不**等于「九个都会在种子里被点名」。种子只点了 `testJobExecutor`。类比也**不**等于「类进了 jar 就会跑」——还要 `snail-job.enabled`、主任进程、花名册上的 `executor_info`。类比更**不**等于 Warm-Flow 的请假大厅：账单三件套注释里的「DAG 工作流」是 **SnailJob 自己的任务图**，表在 `sj_workflow*`，不是 `wta-workflow` 的 `flow_*`。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **这间房没有窗。** 没有 Controller，没有 `/job`，没有 Sa-Token 权限字。入口是类，不是路径。
2. **九个工人，一张卡片。** 九份 Java 在 `snailjob/`；`BillDTO` 只给账单 DAG 用。
3. **八个贴纸，一个继承。** 八个类贴 `@JobExecutor(name=...)` + `@Component`。`TestClassJobExecutor` 只 `@Component`，继承 `AbstractJobExecutor`。
4. **干活方法几乎都叫 `jobExecute`。** 入参 `JobArgs`，出参 `ExecuteResult`。继承那份覆盖的是 `doJobExecute`。Map 两份用 `@MapExecutor` / `@ReduceExecutor`，不叫 `jobExecute`。
5. **注解名才是花名。** 主任点的是 `name = "testJobExecutor"` 这种字符串，不是 Java 文件名。
6. **正常任务最小样板是 `TestAnnoJobExecutor`。** 打两条日志（LOCAL + REMOTE），回 `"测试成功"`。种子 `demo-job` 点的就是它。
7. **账单三件套是一条 DAG。** 支付宝、微信先把 JSON 塞进上下文；汇总再加起来。金额是写死的演示数。
8. **广播会掷骰子。** `TestBroadcastJob` 随机数小于 50 就抛异常；注释说要加两个数，磁盘没加。
9. **静态分片读逗号。** `TestStaticShardingJob` 把 `jobParams` 拆成 `fromId,toId`，睡 3 秒假装加密，**不碰库**。
10. **Map 切，MapReduce 再收。** 都把 1…200 按 50 切四片。Map 只切；MapReduce 多一个 `@ReduceExecutor` 把分片和再加总。
11. **classic 但没有 Mapper 链。** 登记表仍是 classic。不要在这间房找 `ServiceImpl`。新的独立业务能力要另登 layered，不要在这里加阁楼。
12. **三道电闸不是一扇窗。** jar 在不在是 L-079；客户端醒不醒是 `snail-job.enabled`（默认 **false**）；主任在不在是 extend 进程。本课只认工人。

**类比失效边界：** 「工人」**不**覆盖 SnailJob 怎么路由、重试、分片到哪台机器——那是厂商引擎，Goal 划出范围。类比也**不**等于「管理端有任务 CRUD 厨房」。类比还不等于「`enabled: false` 时 `@Component` 会从 Spring 消失」——电闸贴在 `SnailJobConfig` 上，不是贴在这九个类上。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 任务调度模块 | `wta-job` | `backend/wta-modules/wta-job`。POM 描述「任务调度」。无 `src/main/resources`，无测试目录 |
| 执行器 / 工人 | job executor | 被 SnailJob 客户端调用的 Spring Bean。本课九个类。不是 `@RestController` |
| 注解执行器 | `@JobExecutor` | `com.aizuda.snailjob.client.job.core.annotation.JobExecutor`。类上 `name` 是控制台要填的执行器名 |
| 类执行器 | `AbstractJobExecutor` | `TestClassJobExecutor` 继承它，覆盖 `doJobExecute(JobArgs)`。**无** `@JobExecutor` |
| 任务参数 | `JobArgs` | 客户端传入。本课用到 `getJobParams()`、`getWfContext()` / `getWfContext(key)`、`appendContext(key, value)` |
| 执行结果 | `ExecuteResult` | `success(...)` / `failure("任务执行失败")`。广播那份失败走的是 **抛 `RuntimeException`**，不是 `failure` |
| 工作流上下文 | `wfContext` | SnailJob DAG 节点之间传字符串的袋子。账单三件套用键 `settlementDate` / `alipay` / `wechat` |
| 账单卡片 | `BillDTO` | `org.namewta.job.entity` 的 Java **record**：`billId` / `billChannel` / `billDate` / `billAmount` |
| Map 分片 | `@MapExecutor` + `MapHandler.doMap` | 根方法把集合切成片，点名下一个 `taskName` |
| Reduce 汇总 | `@ReduceExecutor` + `ReduceArgs` | 只出现在 `TestMapReduceAnnotation1`。把各片结果再加总 |
| 广播任务 | broadcast job | `TestBroadcastJob`。读 `${snail-job.port}`。随机成败。不是 HTTP 广播接口 |
| 静态分片 | static sharding | `TestStaticShardingJob`。范围写在任务参数字符串里，工人自己 `split(",")` |
| 远程日志 | `SnailJobLog.REMOTE` | 打到调度侧。`SnailJobConfig` 在客户端启动事件里给 root logger 挂 `SnailLogbackAppender` |
| 本地日志 | `SnailJobLog.LOCAL` | 只打本进程。`TestAnnoJobExecutor` 与静态分片开头用了 |
| 客户端电闸 | `snail-job.enabled` | `SnailJobConfig` 上 `@ConditionalOnProperty(..., havingValue="true")`。local/dev/prod **默认 false** |
| 接入组 | `snail-job.group` | 配置值 `"wta_group"`，与 `20-cde-job.sql` 的 `sj_group_config` 种子一致 |
| 客户端端口 | `snail-job.port` | `2${server.port}`。只有广播工人 `@Value` 注入它 |
| 调度服务器 | `wta-snailjob-server` | extend 独立进程，启动类 `org.namewta.snailjob.SnailJobServerApplication`。本课不拆内部 |
| 执行器花名表 | `sj_job.executor_info` | 种子一行 `'testJobExecutor'`。`sj_job_executor` 表有 DDL、**无**九行 INSERT |
| 经典登记 | classic | 登记表：`wta-modules/wta-job`。「既有任务业务能力；保持现状；新增独立业务能力需另行登记为 layered」 |

### 机制/因果链

#### 1. 房间盘点：入口是类，走廊上没有窗

文件根：`backend/wta-modules/wta-job/`。

- `pom.xml`：`artifactId` `wta-job`。依赖恰好两行：`wta-common-json`、`wta-common-job`。没有 web、没有 mybatis、没有 satoken、没有 `wta-api`。
- Java 一共 **11** 个文件：空的 `package-info.java`、`BillDTO.java`、九个工人。
- `src/main/` 只有 `java/`。没有 `resources/`。所以 L-003 那条 classic 脚本缺目录，退出码 1。
- 全模块没有 `@RestController`、`@Controller`、`@RequestMapping`、`@GetMapping`、`@PostMapping`。口试若说出 `/job/list`，格子立刻不满。

Spring 怎么看见工人：`NamewtaApplication` 在 `org.namewta`，`@SpringBootApplication` 默认扫描子包，因此 `org.namewta.job.snailjob` 的 `@Component` 会进 **admin 进程**——前提是 L-079 说的 bundle 把 `wta-job` 插进了 classpath。本课只需要记住：**扫到 Bean ≠ 被主任点名**。

#### 2. 两套挂号方式

**贴纸挂号（八个）：** 类上同时有 `@Component` 和 `@JobExecutor(name = "...")`。公开干活方法叫 `jobExecute`（Map 两份除外）。`name` 才是花名。

**继承挂号（一个）：** `TestClassJobExecutor extends AbstractJobExecutor`。覆盖 `protected ExecuteResult doJobExecute(JobArgs jobArgs)`，固定回 `"TestJobExecutor测试成功"`。厂商如何把 FQCN / Bean 名对上控制台，本课不拆；口试只要求：能指出它**没有** `@JobExecutor`，方法名也**不是** `jobExecute`。

#### 3. `TestAnnoJobExecutor`：最小正常任务

文件：`TestAnnoJobExecutor.java`。

- `@JobExecutor(name = "testJobExecutor")`
- `jobExecute(JobArgs)`：用厂商 `JsonUtil.toJsonString(jobArgs)` 打 **LOCAL + REMOTE** 两条 info，然后 `ExecuteResult.success("测试成功")`
- 不读上下文，不改世界，不睡

`20-cde-job.sql` 唯一的 `sj_job` 种子：

- `namespace_id='dev'`，`biz_id='demo-job'`，`group_name='wta_group'`，`job_name='demo-job'`
- `executor_info='testJobExecutor'`
- `task_type=1`（注释：1 集群 / 2 广播 / 3 切片），`trigger_type=2`，`trigger_interval='60'`

口试三件套不要混：

| 名字 | 值 |
| --- | --- |
| Java 类 | `TestAnnoJobExecutor` |
| 注解花名 / `executor_info` | `testJobExecutor` |
| 种子任务名 `job_name` | `demo-job` |

local 配置 `snail-job.namespace: local`，种子命名空间却是 `dev` / `prod`。本课不把「local 能否点到 demo-job」说成已验证；只要求能指出这两份文件对不上。

#### 4. 账单 DAG：`AlipayBillTask` / `WechatBillTask` / `SummaryBillTask`

三份类注释都写「DAG工作流任务」，链到同一篇掘金文。它们**不是** Warm-Flow。

共用卡片 `BillDTO(billId, billChannel, billDate, billAmount)`。金额类型 `BigDecimal`。JSON 走 **项目** `JsonUtils`，不是厂商 `JsonUtil`。

**支付宝工人** `name = "alipayBillTask"`：

1. `jobArgs.getWfContext().get("settlementDate")` 取清算日
2. 字符串等于 `"sysdate"` 时换成 `DateUtils.now()`（`DateUtils` 继承 Hutool `DateUtil`）
3. 写死 `new BillDTO(23456789L, "alipay", settlementDate, new BigDecimal("2345.67"))`
4. `jobArgs.appendContext("alipay", JsonUtils.toJsonString(billDTO))`
5. REMOTE 打整袋上下文；`success(billDTO)`
6. 方法签名 `throws InterruptedException`，方法体**没有** sleep

**微信工人** `name = "wechatBillTask"`：同一套日期规则；写死 `123456789L` / `"wechat"` / `"1234.56"`；上下文键是 **`wechat`**。

**汇总工人** `name = "summaryBillTask"`：

1. `(String) jobArgs.getWfContext("wechat")` ——注意这里走的是 **按键取值**，不是先拿整袋再 `.get`
2. 空白则微信金额 0；否则 `JsonUtils.parseObject(wechat, BillDTO.class).billAmount()`
3. 支付宝键 `"alipay"` 同样处理
4. `wechatAmount.add(alipayAmount)`，REMOTE 打「总金额」，`success(totalAmount)`
5. 也声明了 `throws InterruptedException`，同样没有 sleep

因果：主任先跑两个上游节点（可并行），再跑汇总。袋子里没有键，汇总当 0，**不会**抛。两边都在时，写死金额相加是 `2345.67 + 1234.56 = 3580.23`。这是演示加法，不是对账接口。

#### 5. 广播与静态分片

**`TestBroadcastJob`** `name = "testBroadcastJob"`：

- `@Value("${snail-job.port}")` 注入客户端端口
- `RandomUtil.randomInt(100)`：`< 50` 抛 `RuntimeException("随机数小于50，收集日志任务执行失败")`；否则 `success("随机数大于50，收集日志任务执行成功")`
- slf4j `log.info` + `SnailJobLog.REMOTE`
- 注释写「获得 jobArgs 中传入的相加的两个数」，方法体**没有**读 `jobArgs` 的参数去做加法

失败形态和静态分片不同：广播是**扔异常**；静态分片中断时是 **`ExecuteResult.failure`**。口试不要说成同一种信封。

**`TestStaticShardingJob`** `name = "testStaticShardingJob"`：

1. `Convert.toStr(jobArgs.getJobParams())`，按逗号拆
2. `split[0]` → `fromId`，`split[1]` → `toId`（`Long.parseLong`，拆不出两个数会在工人线程炸）
3. LOCAL 打开始；REMOTE 打「开始对 id 范围加密」；`Thread.sleep(3000)`；REMOTE 打完成
4. 注释写「模拟数据库操作」，磁盘**没有** Mapper、没有加密函数
5. `InterruptedException` → `failure("任务执行失败")`；正常 → `success("执行分片任务完成")`

切片边界由**主任下发的参数字符串**决定，不是工人自己切 1…200。那是下面 Map 的活。

#### 6. Map 与 MapReduce

两份都 `@SuppressWarnings({"unchecked","rawtypes"})`，因为分片结果按裸 `List` 取。

共同的切法：

```text
IntStream.rangeClosed(1, 200)
  StreamUtils.groupByKey(i -> (i - 1) / 50)
  → 四片：1–50 / 51–100 / 101–150 / 151–200
mapHandler.doMap(partition, "doCalc")
```

`StreamUtils.groupByKey` 是 `wta-common-core` 的分组，底层 `Collectors.groupingBy` + `LinkedHashMap`。

**`TestMapJobAnnotation`** `name = "testMapJobAnnotation"`。类注释：「只分片不关注结果」。

- 无 `taskName` 的 `@MapExecutor`：`doJobMapExecute(MapArgs, MapHandler)`，REMOTE 打 `server.port`，然后 `doMap(..., "doCalc")`
- `@MapExecutor(taskName = "doCalc")`：把 `mapArgs.getMapResult()` 当成 `List<Integer>` 求和，睡 3 秒，REMOTE 打 `partitionTotal`，`success(partitionTotal)`
- **没有** Reduce。各片的和不会在本类再加总

**`TestMapReduceAnnotation1`** `name = "testMapReduceAnnotation1"`。切法与 `doCalc` 几乎同一份。多出来的是：

```text
@ReduceExecutor
reduceExecute(ReduceArgs reduceArgs)
  reduceArgs.getMapResult()
    .stream()
    .mapToInt(i -> Integer.parseInt((String) i))
    .sum()
```

磁盘把各片结果**先当 String 再 parseInt**。不要发明「Reduce 直接拿 Integer」。根方法名这里叫 `rootMapExecute`，Map-only 那份叫 `doJobMapExecute`——口试按磁盘，不要背成同一个方法名。

#### 7. 邻居：电闸、主任、玻璃窗、装配（只标位置）

| 邻居 | 磁盘位置 | 本课认不认 |
| --- | --- | --- |
| 客户端自动配置 | `wta-common-job` → `SnailJobConfig`；`AutoConfiguration.imports` 登记该类 | **对照。** `@EnableSnailJob` + 启动时挂 REMOTE appender。电闸 `snail-job.enabled=true` |
| 配置值 | `application-{local,dev,prod}.yml` 的 `snail-job` 段 | **对照。** 三份都是 `enabled: false`，`group: wta_group`，`server.port: 17888`，`port: 2${server.port}` |
| 令牌 / 组种子 | `20-cde-job.sql` 的 `sj_group_config` / `sj_namespace` | **对照。** 文档写明是上游快照，不是 `10-cde-base-ddl` 产品基座 |
| 主任进程 | `wta-extend/wta-snailjob-server` | **对照。** 独立启动类。内部 filter/SecurityConfig 不拆 |
| 玻璃窗 | `views/monitor/external/index.vue` 的 `'snail-job'` | **L-024。** `monitorService.externalIntent`，权限 `monitor:snailjob:list` |
| 装配 profile | `wta-admin/pom.xml` `bundle-full` 插入 `wta-job`；`bundle-core` 没有 | **L-079。** 本课不把 `C:bundle-full-job` 标 covered |

`SnailJobConfig` 还贴了 `@EnableScheduling`。那是 Spring 自己的调度开关，**不是**这九个工人被点名的原因。工人等的是 SnailJob 客户端，不是 `@Scheduled`。

## 图、表或文本图

**图题 / caption：** 2026-09-17 宏观车间：九个工人在 admin 进程，主任在另一进程，浏览器没有 `/job/*` 窗。alt：上为 iframe 看主任楼；中为 SnailJob Server 点名；下为 wta-job 九个 @Component。

```text
admin-web
  monitor/snailjob/index  → iframe → SnailJob Server 后台     （L-024，不是工人）
                                                         
SnailJob Server  (extend)
  sj_job.executor_info = 'testJobExecutor'   ← 种子只这一行
                          │
                          v  客户端协议（不是 @GetMapping）
admin  NamewtaApplication
  snail-job.enabled ? SnailJobConfig.@EnableSnailJob : 客户端不醒
                          │
                          v
        snailjob/  九个工人（本课）
```

**文字等价物：** 人在浏览器里看不到这九个类的 URL。菜单那块玻璃看的是调度服务器。真正被点名的是 admin 进程里的 Spring Bean。种子目前只写了花名 `testJobExecutor`。

**图题 / caption：** 九个工人对照表。alt：左列 Java 类；中列注解花名；右列干活方法和副作用。

| Java 类 | `@JobExecutor` name | 入口方法 | 磁盘上实际做的事 |
| --- | --- | --- | --- |
| `TestAnnoJobExecutor` | `testJobExecutor` | `jobExecute` | 打 LOCAL+REMOTE，success「测试成功」。种子点它 |
| `TestClassJobExecutor` | **无注解** | `doJobExecute` | 固定 success「TestJobExecutor测试成功」 |
| `AlipayBillTask` | `alipayBillTask` | `jobExecute` | 写死支付宝账单，塞上下文键 `alipay` |
| `WechatBillTask` | `wechatBillTask` | `jobExecute` | 写死微信账单，塞上下文键 `wechat` |
| `SummaryBillTask` | `summaryBillTask` | `jobExecute` | 读两键 JSON，金额相加 |
| `TestBroadcastJob` | `testBroadcastJob` | `jobExecute` | 读客户端端口；随机数小于 50 抛异常 |
| `TestStaticShardingJob` | `testStaticShardingJob` | `jobExecute` | 解析 `from,to`，睡 3 秒，假装加密 |
| `TestMapJobAnnotation` | `testMapJobAnnotation` | `@MapExecutor` ×2 | 1…200 切四片，每片求和；无 Reduce |
| `TestMapReduceAnnotation1` | `testMapReduceAnnotation1` | Map×2 + `@ReduceExecutor` | 同上再把各片 String 解析后加总 |

**文字等价物：** 口试按这张表念：类、花名、方法、副作用。把类名说成花名、把继承工人说成贴纸工人、把 Map 说成 HTTP 分片接口，格子不满。

**图题 / caption：** 账单 DAG 上下文。alt：清算日进袋子；两个上游各塞一条 JSON；汇总取出相加。

```text
wfContext["settlementDate"]  ──┐
  "sysdate" → DateUtils.now()  │
                               v
        AlipayBillTask                 WechatBillTask
        BillDTO alipay 2345.67         BillDTO wechat 1234.56
        appendContext("alipay", json)  appendContext("wechat", json)
                               \       /
                                \     /
                             SummaryBillTask
                             blank → 0
                             total = wechat + alipay
                             success(totalAmount)
```

**文字等价物：** 三个工人不发外网。他们只改 SnailJob 那只袋子里的字符串。缺键当 0。这和请假流程的 `flow_*` 表不是一条河。

## 正例、反例与边界

**正例 1 — 指入口。** 打开 `snailjob/` 目录，数出九个 `.java`。再打开任意一个，指出它是 `@Component`，没有 `@RequestMapping`。OBJ-78 这一句就成立。

**正例 2 — 念花名。** 打开 `TestAnnoJobExecutor`，手指点 `name = "testJobExecutor"`，再打开 `20-cde-job.sql` 那一行 `executor_info='testJobExecutor'`。类名、花名、任务名三套嘴。

**正例 3 — 走账单袋子。** 假设上下文已有 `settlementDate=sysdate`。支付宝工人换成本地当前时间字符串，塞 `alipay` JSON。微信同样塞 `wechat`。汇总读到两条，REMOTE 打总金额 `3580.23`。

**正例 4 — 缺一条上游。** 袋子里只有 `alipay`，没有 `wechat`。汇总微信侧走 `StringUtils.isNotBlank` 失败分支，金额 0，总金额等于支付宝那张卡。不抛。

**正例 5 — 最小成功工人。** `TestClassJobExecutor.doJobExecute` 不读参数，永远 success 那句中文。用来对照「贴纸 vs 继承」。

**正例 6 — Map 切四片。** 1…200、片大小 50，四次 `doCalc`，每次睡 3 秒并打该片和。Map-only 类到此结束。

**正例 7 — MapReduce 再收。** 四片和作为 String 进 `reduceExecute`，`parseInt` 后求和，REMOTE 打 `reduceTotal`。

**正例 8 — 静态分片参数。** 主任下发 `"100,199"`。工人 LOCAL 打参数，REMOTE 打范围 `100-199`，睡 3 秒，success「执行分片任务完成」。

**正例 9 — 广播失败。** 随机数 0…49。抛 `RuntimeException`。REMOTE 已经打了随机数和端口。没有 `ExecuteResult.failure`。

**正例 10 — 关客户端。** `application-local.yml` `snail-job.enabled: false`。`SnailJobConfig` 不进容器，`@EnableSnailJob` 不生效。九个 `@Component` 仍可能被扫到，但主任这头点不着活。不要说成「类从磁盘消失」。

**反例 1 — 「`wta-job` 有一套 `/job` CRUD。」** 没有 Controller。

**反例 2 — 「执行器名就是类名。」** `TestAnnoJobExecutor` ≠ `testJobExecutor` ≠ `demo-job`。

**反例 3 — 「九个都贴了 `@JobExecutor`。」** 继承那份没有。

**反例 4 — 「账单工人会调支付宝 SDK / 微信 SDK。」** 构造函数写死 `BigDecimal`。

**反例 5 — 「这是 Warm-Flow 的第四种节点。」** DAG 注释属于 SnailJob；流程楼是 L-067…L-073。

**反例 6 — 「Map 和静态分片是同一种切法。」** 静态分片吃 `jobParams` 逗号；Map 自己生成 1…200。

**反例 7 — 「广播注释说加法，所以会把两个参数加起来。」** 磁盘只掷骰子。

**反例 8 — 「失败都走 `ExecuteResult.failure`。」** 广播走抛异常；账单缺键走 0；静态分片中断才 `failure`。

**反例 9 — 「iframe 任务调度页就是这些工人的前端。」** 那是主任楼的玻璃，L-024。

**反例 10 — 「本课覆盖 bundle-full。」** OBJ-79。不要把 `C:bundle-full-job` 算进本格。

**反例 11 — 「没有 Mapper 就不是 classic。」** 登记表有这一行。形状是执行器，不是 ServiceImpl 链。

**反例 12 — 「给这间房加 UseCase/DAO 才算现代化。」** 登记策略：新的独立业务能力**另行登记 layered**，不是在 classic 房间开阁楼。

**反例 13 — 「`SnailJobLog.REMOTE` 就是 logback 控制台。」** REMOTE 要客户端启动时挂上的 appender 才能送到主任侧。LOCAL 才是本进程。

**反例 14 — 「`sj_job_executor` 种子已经登记九个花名。」** 表有，INSERT 没有。运行时由客户端上报，本课不拆上报协议。

**反例 15 — 「OBJ-78 包含 `createMonitorService.externalIntent`。」** 那是监控厨房的安全尺，L-024。

**边界 1 — 声明了不睡的 InterruptedException。** 账单三份方法签名带 `throws InterruptedException`，方法体无 sleep。静态分片才真睡。不要把签名当成副作用。

**边界 2 — 汇总取值 API 不对称。** 上游用 `getWfContext().get("settlementDate")`；汇总用 `getWfContext("wechat")` / `getWfContext("alipay")`。口试按调用写。

**边界 3 — Reduce 当 String。** `Integer.parseInt((String) i)`。若厂商某版本改成直接给 Integer，以当时磁盘为准；2026-09-17 这份源码按 String 写。

**边界 4 — 默认客户端关闭。** local/dev/prod 三份 yml 都是 `enabled: false`。full bundle 只解决「类在不在 jar」，不解决「活干不干」。

**边界 5 — namespace 对不上。** 种子 `dev`/`prod`；local yml 写 `namespace: local`；dev/prod yml 写 `${spring.profiles.active}`。本课不声称已连上实跑。

**边界 6 — 无模块测试。** 证据是生产源码 + 配置 + `20-cde-job.sql` + 前端对照（没有厨房）。不要假装有 `AlipayBillTaskTest`。

**边界 7 — classic 脚本退出码 1。** 缺 `src/main/resources`。人看登记表，不看脚本当判决书。L-003 已钉。

**边界 8 — 厂商版本。** 父 POM `snailjob.version` = `2.0.2`。client-starter / job-core 走这个版本。引擎内部仍出范围。

**边界 9 — `@EnableScheduling` ≠ 这九个工人。** 它在 `SnailJobConfig` 上。工人入口仍是 SnailJob 点名。

**边界 10 — 账单 JSON 字段名。** record 组件名 `billId` / `billChannel` / `billDate` / `billAmount`。汇总按这个类型反序列化。不要发明 `amount` 这种别名。

## 变式与迁移

1. **和 L-002 对照。** 变式 D 已经说过：工人在 admin，主任在 extend。本课把「执行器客户端」展开成九个能指的类。不要把「模块没进 bundle」和「主任没启动」说成一件事。装配细节交给 L-079。
2. **和 L-003 对照。** classic 列不是一种家具。demo/system/workflow 有 ServiceImpl→Mapper。job 没有这条链，**仍是 classic 行**。脚本缺 resources 不能拿来改登记。
3. **和 L-024 对照。** 系统监控四页吃 `/monitor/*` HTTP。外置四键（admin/snailjob/snailai/nacos）是 iframe。本课工人不在那四页，也不在那四键的厨房对象上。
4. **和 L-067…L-073 对照。** 流程楼是 Warm-Flow + 浏览器厨房。本课 DAG 是 SnailJob `sj_workflow*` 那条河。两边都有「工作流」三个字，口试要先说哪栋楼。
5. **和 L-074 对照。** demo 有八扇 HTTP 窗。job 零扇。不要把 classic CRUD 口诀搬进这间房。
6. **和 L-080 对照。** `wta-ai` 同样登记 classic、同样瘦，但它**有** `SnailAiController`。job 连这一扇 HTTP 都没有。不要把 ai 的注册窗说成 job 也有。
7. **迁移：加一个真会碰库的定时能力。** 先问是不是「独立新业务」。是：新模块默认 layered，另登记，不要在 `snailjob/` 里直接抱 Mapper（classic 入口抱 Mapper 是扩大越层）。只是再演示一种调度形状：可以再加一个 `@JobExecutor` 工人，花名不要和类名混着起。
8. **迁移：想给工人做管理页。** 不要在 `wta-job` 里长 Controller 当「终于有窗了」。调度台是主任楼的产品 UI；我们的管理端只嵌玻璃。产品表不进 `20-cde-job.sql`（上游快照）。

## 常见误区

1. **「没看到 Controller 就是模块坏了。」** 这间房的公开入口就是执行器类。
2. **「classic 必须有 ServiceImpl。」** 登记的是模块模式，不是家具清单。
3. **「`testJobExecutor` 是类名。」** 那是注解 `name`，也是种子 `executor_info`。
4. **「九个种子任务。」** 只有 `demo-job` 一行。
5. **「账单是支付对账生产代码。」** 写死金额的教学 DAG。
6. **「Summary 读不到键会失败。」** 当 0。
7. **「广播失败返回 `R.fail`。」** 这里没有 `R`。要么 success 字符串，要么扔异常。
8. **「静态分片真加密、真 update。」** sleep + 日志。
9. **「MapReduce 的根方法也叫 `jobExecute`。」** 叫 `rootMapExecute` / `doJobMapExecute`。
10. **「iframe 能直接调 `AlipayBillTask`。」** iframe 连的是主任后台 URL。
11. **「`enabled: false` 时九个类不会被编译。」** 编译是 Maven；电闸是运行时配置。
12. **「本课讲完就等于 job 模块全覆盖。」** bundle 是 L-079。厂商引擎内部保持 deferred。
13. **「`wta-job` 可以 import `UserService`。」** POM 没有 `wta-api`。跨房间合同本课不发明。
14. **「`package-info` 里写了模式。」** 只有一行 `package org.namewta.job;`。
15. **「OBJ-78 要口述 HTTP 方法性状。」** OBJ-78 是指类、指入口。没有 HTTP 方法表。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `backend/wta-modules/wta-job/src/main/java/org/namewta/job/snailjob/`。数九个文件。确认没有 `controller` 目录。
2. 打开 `AlipayBillTask` / `WechatBillTask` / `SummaryBillTask`。圈三个 `name`、两处 `appendContext`、汇总的 `getWfContext("wechat")`、两笔写死金额。
3. 打开 `TestAnnoJobExecutor`。圈 `name = "testJobExecutor"`、`jobExecute`、LOCAL 与 REMOTE。打开 `20-cde-job.sql` 圈同一字符串。
4. 打开 `TestClassJobExecutor`。确认**没有** `@JobExecutor`，方法是 `doJobExecute`。
5. 打开 `TestBroadcastJob` 与 `TestStaticShardingJob`。圈随机抛异常 vs `ExecuteResult.failure`；圈广播没读 `jobArgs` 做加法；圈静态分片的 `split(",")` 与 3 秒 sleep。
6. 打开两份 Map 类。圈 `doMap(..., "doCalc")`、`@ReduceExecutor` 只出现一次、`parseInt((String) i)`。打开 `BillDTO` 圈四个 record 组件。打开 `wta-job/pom.xml` 圈两个依赖。不要改这些文件。

## 总结、词汇表与下一步

- **宏观九个工人：** `wta-job` 的公开入口是 `snailjob/` 下九个执行器类，外加一张 `BillDTO`。没有 HTTP 窗，没有 Mapper，没有前端厨房。
- **(a) 脊柱四类：** `AlipayBillTask` / `WechatBillTask` / `SummaryBillTask` 用上下文拼演示账单；`TestAnnoJobExecutor` 是最小贴纸工人，种子 `demo-job` 点它的花名 `testJobExecutor`。
- **(a) 其余五类：** 继承工人、广播掷骰子、静态分片吃逗号、Map 只切、MapReduce 再收。同一矩阵行，口试要能指。
- **三道电闸分开说。** 类在不在 jar → L-079；客户端醒不醒 → `snail-job.enabled`；主任在不在 → extend 进程。本课只把工人认全。
- **classic 仍成立。** 没有 ServiceImpl 不是逃出登记表的借口；新的碰库业务不要塞进这间房。

词汇表：`wta-job` / `@JobExecutor` / `AbstractJobExecutor` / `jobExecute` / `doJobExecute` / `JobArgs` / `ExecuteResult` / `wfContext` / `BillDTO` / `AlipayBillTask` / `WechatBillTask` / `SummaryBillTask` / `TestAnnoJobExecutor` / `testJobExecutor` / `TestBroadcastJob` / `TestClassJobExecutor` / `TestMapJobAnnotation` / `TestMapReduceAnnotation1` / `TestStaticShardingJob` / `@MapExecutor` / `@ReduceExecutor` / `SnailJobLog` / `SnailJobConfig` / `snail-job.enabled` / `wta_group` / `sj_job` / `wta-snailjob-server` / classic。

下一步：L-079 对照 `bundle-full` 插入 `wta-job`、`bundle-core` 排除，不要把装配说成本课已经 covered。L-080 才是 `SnailAiController.registerCurrentUser` 那扇真正的 HTTP。监控 iframe 保持 L-024。厂商引擎内部保持 deferred。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-004 | `backend/wta-modules/**` | 业务模块公开入口；本课入口在 `wta-job/snailjob`，无 Controller | 九个执行器类 | 2026-09-17 |
| S-007 | `01-module-map.md`；`wta-modules/pom.xml`；`wta-admin/pom.xml` | `wta-job` 是业务任务执行器；full 才插入 admin（装配细节 L-079） | 模块地图 job 行；`<module>wta-job`；profile `bundle-full` | 2026-09-17 |
| S-008 | `03-backend-module-modes.md` | `wta-job` = classic；新增独立业务另登 layered | 登记表 job 行 | 2026-09-17 |
| S-010 | `20-cde-job.sql`（上游快照，非 10-cde-base） | `sj_namespace`/`sj_group_config`/`sj_job` 种子；`executor_info='testJobExecutor'`；`sj_job_executor` 无 INSERT | 第 18–42、320、525–540 行附近 | 2026-09-17 |
| S-L002-01 | 子课 L-002 | 最小 common；变式 D：工人在 admin，主任在 extend | `lessons/L-002-backend-assembly.md` | 2026-09-17 |
| S-L003-01 | 子课 L-003 | classic 列不是一种家具；job 无 Controller/Mapper/resources；脚本退出码 1 | `lessons/L-003-layered-vs-classic.md` | 2026-09-17 |
| S-L078-01 | `AlipayBillTask.java`；`WechatBillTask.java`；`SummaryBillTask.java`；`BillDTO.java` | DAG 三工人；写死金额；上下文键；缺键当 0 | `org.namewta.job.snailjob`；`entity/BillDTO` | 2026-09-17 |
| S-L078-02 | `TestAnnoJobExecutor.java` | 花名 `testJobExecutor`；LOCAL+REMOTE；success「测试成功」 | 类上注解与 `jobExecute` | 2026-09-17 |
| S-L078-03 | `TestClassJobExecutor.java` | 无 `@JobExecutor`；`doJobExecute` | 继承 `AbstractJobExecutor` | 2026-09-17 |
| S-L078-04 | `TestBroadcastJob.java`；`TestStaticShardingJob.java` | 随机抛异常 vs failure；`snail-job.port`；逗号范围；3 秒 sleep | 两个类的 `jobExecute` | 2026-09-17 |
| S-L078-05 | `TestMapJobAnnotation.java`；`TestMapReduceAnnotation1.java`；`StreamUtils.groupByKey` | 1…200 按 50 切；`doCalc`；Reduce 按 String parseInt | 两个类 + `wta-common-core` | 2026-09-17 |
| S-L078-06 | `wta-job/pom.xml`；模块目录 | 只依赖 json + common-job；无 resources；无 test；无 HTTP 注解 | artifact `wta-job` | 2026-09-17 |
| S-L078-07 | `SnailJobConfig.java`；`application-{local,dev,prod}.yml` | 电闸默认 false；group/token/port；启动挂 REMOTE appender | `wta-common-job`；admin 配置 | 2026-09-17 |
| S-L078-08 | `SnailJobServerApplication.java`；`external/index.vue`；`domains/system/src/monitor/index.ts` | 主任另一进程；iframe 目标 `snail-job`；权限 `monitor:snailjob:list` | extend；L-024 对照 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：`wta-job` 是一间没有 HTTP 窗的 classic 房间。公开入口是 `org.namewta.job.snailjob` 里九个 Spring `@Component` 工人，外加一张账单 record。八个工人用 `@JobExecutor(name=...)` 挂号，花名才是调度台要点的字符串；剩下一个继承 `AbstractJobExecutor`。`TestAnnoJobExecutor` 的花名 `testJobExecutor` 对上种子任务 `demo-job`。支付宝/微信/汇总三个工人用工作流上下文传 JSON，金额是写死的演示数，不是支付通道。广播掷骰子，静态分片解析逗号并睡觉，Map 把 1 到 200 切成四片，MapReduce 再把各片和加总。浏览器没有 job 厨房；菜单里的任务调度页是 iframe 看另一栋 SnailJob 服务器。类在不在 jar、客户端开不开、主任在不在，是三道闸。本课只把工人指全。
