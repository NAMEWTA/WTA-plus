---
lesson_id: L-079
objective_ids: [OBJ-79]
claimed_cells:
  - C:bundle-full-job
estimated_minutes: 38
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: two-profiles-on-pom
    minutes: 11
  - segment: classpath-scan-and-gates
    minutes: 10
  - segment: visuals-and-worked-examples
    minutes: 6
  - segment: pause
    minutes: 3
  - segment: recap-and-transfer
    minutes: 4
expression_level: eli5
coverage_depth: deep
source_ids: [S-001, S-007, S-L002-01, S-L078-06, S-L079-01, S-L079-02, S-L079-03, S-L079-04, S-L079-05, S-L079-06, S-L079-07, S-L079-08]
---

# Lesson 079：宏观插座板——`bundle-full` 把 `wta-job` 插进 admin jar，`bundle-core` 把它拔掉

## 学完你能做什么

打开 `backend/wta-admin/pom.xml` 的 `<profiles>`，你能**口述这块宏观插座板**：默认那根插排叫 `bundle-full`，上面有一颗标记「调度任务模块」的插头，artifact 就是 `wta-job`；另一根插排叫 `bundle-core`，同一颗插头**不在**。OBJ-79 只要这一句能对照 POM 说清楚：**job 只在 `bundle-full` 进入 admin jar，`bundle-core` 排除。**

口试名单就是矩阵 **(c)** 这一格，符号以**磁盘**为准：

**`C:bundle-full-job`**

2026-09-17 工作树先钉死**房间形状**（口试先指数组，再数 jar 名）：

| 你可能以为的名字 | 磁盘事实 |
| --- | --- |
| `wta-job` 写在 admin 顶部常驻 `<dependencies>` | **没有。** 常驻只有 api / system / third / notify / sso 和若干 common |
| `bundle-core` 也插 `wta-job`，只是关掉 `snail-job.enabled` | **没有。** core 剖面的 `<dependencies>` 只有两份档案，没有 job |
| 父 POM `dev` / `prod` 就是 bundle | **不是。** 那是 `profiles.active`，用来挑 `application-*.yml` |
| reactor 里有 `wta-job`，fat jar 就一定有 | **不是。** `wta-modules/pom.xml` 永远列出该模块；admin 的 Boot jar 只打包**自己的依赖** |
| `ProfileModuleGraphContractTest` 已经断言 job | **没有。** 它只断言档案两个剖面都在、workflow 只在 full |
| 本课把 ai / demo / workflow 的 bundle 格也标 covered | **不是。** 它们和 job 同坐 full 插排，但矩阵格子是 `C:bundle-full-job` |
| 本课把九个工人类标 covered | **不是。** 那是 OBJ-78 / L-078 |
| 把 `wta-snailjob-server` 插进 admin 就算接线 job | **不是。** 主任是 extend 另一进程 |
| 前端 `createJobService` / `web-domains/job` | **没有。** 2026-09-17 无此包 |

本课**不宣称**你会背九个 `@JobExecutor` 花名（L-078）、拆 `SnailJobConfig` 的 appender（对照用）、口述 `SnailAiController`（L-080）、或把监控 iframe 当本格 covered（L-024）。插座板只回答：**类进不进 `wta-admin.jar`。**

## 先把宏观地图放在桌上

L-002 已经把 `wta-admin` 钉成可部署进程：组装启动，把选中的模块插进同一个 classpath。它画过两根插排：`bundle-full` 默认亮，另插 job / ai / demo / workflow 和两份档案；显式 `-Pbundle-core` 拔掉前四个，档案仍在。L-078 走进 `wta-job` 那间没有 HTTP 窗的车间，把九个工人指全，并把「类在不在 jar」留给本课。今天只认这一颗插头。

把 admin 想成饭店总台后面的**插座板**。墙上永远插着的电器是平台常驻：菜单用户（`wta-system`）、通知控制面（`wta-notify`）、第一方 SSO（`wta-sso`）、第三方网关（`wta-third`），外加菜单合同 `wta-api`。过年全开的那根插排（`bundle-full`）再插四件热闹电器：调度工人、AI、demo、工作流，外加两份档案。平时只开核心店（`bundle-core`）时，档案还要给客人填表，所以档案仍插着；调度工人这件后厨电器拔掉。

工人进的是 **admin 这一个 JVM**。另一栋楼里的调度主任（`wta-extend/wta-snailjob-server`）永远不插在这块板上。浏览器没有 job 厨房。YAML 里即使还写着 `snail-job:`，那只是说明书上的开关画，不等于电器在插座上。

```text
Maven 剖面（本课格子）
  wta-admin/pom.xml
    常驻插头  ── 两个 bundle 都有
    bundle-full（默认亮）── 插上 wta-job
    bundle-core（显式 -P）── 不插 wta-job
          │
          v  spring-boot-maven-plugin repackage
    backend/wta-admin/target/wta-admin.jar
      BOOT-INF/lib/wta-job-${revision}.jar     ← full 才有
          │
          v  NamewtaApplication 扫 org.namewta
    org.namewta.job.snailjob.*                 ← 类在 classpath 才会变成 Bean

对照，不是本格：
  snail-job.enabled          ← 客户端电闸（默认 false）
  wta-snailjob-server        ← 主任另一进程
  L-078 九个工人怎么干活
```

往下走不要跳层：

```text
选哪根插排（Maven profile）
    └─ admin 的编译/运行 classpath 有没有 artifact wta-job
          └─ Boot fat jar 的 BOOT-INF/lib 有没有 wta-job-*
                └─ Spring 能不能扫到 org.namewta.job.snailjob
```

**类比失效边界：** 「宏观插座板」**不**等于「插上就会跑任务」。jar 在只是第一道闸。客户端还要 `snail-job.enabled=true`，主任进程还要在，花名册还要写对 `executor_info`。类比也**不**等于「core 把 SnailJob 服务器删了」——服务器根本不在 admin 依赖里。类比更**不**等于「frontend 也有 dual bundle」——前端 App 组合是另一块板（L-007），本课只认后端 fat jar。

## 核心概念与机制

### 直觉讲解

小孩子版只记十二句：

1. **插座在 `wta-admin/pom.xml`，不在 `wta-job/pom.xml`。** 工人房间自己不会跳进大厅。大厅的 POM 决定请不请它。
2. **常驻名单没有 job。** 打开 admin POM 上半段 `<dependencies>`，数得到 system / notify / sso / third / api，数不到 `wta-job`。
3. **默认那根插排叫 `bundle-full`。** `<id>bundle-full</id>`，`<activeByDefault>true</activeByDefault>`。注释第一句就是「调度任务模块」，artifact `wta-job`。
4. **另一根叫 `bundle-core`。** 没有 `activeByDefault`。里面只有 `wta-profile-person` 和 `wta-profile-enterprise`。注释写：显式激活后停用 full，仅保留上方平台基础依赖。
5. **「停用」不是自写插件。** 是 Maven 同一 POM 的规矩：你显式点亮了另一根，默认那根就灭。
6. **环境剖面是另一排开关。** 父 POM 的 `local` / `dev` / `prod` 只改 `profiles.active`，用来选 YAML。它们不插、不拔 `wta-job`。
7. **编译图不是外卖盒。** `wta-modules` 永远有 `<module>wta-job</module>`。`./mvnw test` 仍会编译工人。客人拿到的 `wta-admin.jar` 却可能没有这颗糖。
8. **Boot 只打包自己的依赖。** `spring-boot-maven-plugin` 的 `repackage` 在 admin。`BOOT-INF/lib/wta-job-` 有没有，就是口试要指的那一行。
9. **工人 jar 会捎上客户端工具。** `wta-job` 只依赖 `wta-common-json` + `wta-common-job`。全仓业务 POM 里，**只有** `wta-job` 声明 `wta-common-job`。core 拔掉 job，客户端自动配置通常一起走。
10. **YAML 开关还在。** `application-{local,dev,prod}.yml` 都有 `snail-job.enabled: false`。core 包里这些键仍在资源文件里，只是没有对应的自动配置类可加载。
11. **门禁用 `jar tf` 数名字。** `scripts/ci/verify-admin-bundle.sh full` 要求看到 `wta-job`；`core` 看到就失败。两次打包都要先 `clean`。
12. **本格只认 job 这一颗。** full 插排上还有 ai / demo / workflow / 档案。档案两个剖面都在。不要把整根插排说成本课全覆盖。

### 精确定义与 English term

| 中文口头 | English term | 精确定义（本课，以 2026-09-17 工作树为准） |
| --- | --- | --- |
| 组装入口 | `wta-admin` | `backend/wta-admin`。`packaging` 为 jar。启动类 `org.namewta.NamewtaApplication`。负责把选中模块打进**一个**可部署进程 |
| 组装剖面 | bundle profile | admin POM 里的 Maven `<profile>`。本课两根：`bundle-full`、`bundle-core`。只改 admin 的依赖集合，不改箭头方向 |
| 默认全量组合 | `bundle-full` | `<id>bundle-full</id>` 且 `<activeByDefault>true</activeByDefault>`。额外依赖：`wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`、`wta-profile-person`、`wta-profile-enterprise` |
| 核心平台组合 | `bundle-core` | `<id>bundle-core</id>`，无 activation。额外依赖只有两份档案。注释承诺显式激活后停用 full |
| 本课格子 | `C:bundle-full-job` | 组件级事实：job **只**经 full 剖面进入 admin classpath / fat jar。不是九个工人的方法表 |
| 常驻依赖 | always-on dependencies | admin POM 顶部、`<profiles>` 之外的业务插头：`wta-api`、`wta-system`、`wta-third`、`wta-notify`、`wta-sso`。两个 bundle 都有 |
| 任务调度模块 | `wta-job` | `backend/wta-modules/wta-job`。artifactId `wta-job`。描述「任务调度」。本课认它**是否被 admin 声明为依赖** |
| 版本锁定 | dependencyManagement | 父 POM `backend/pom.xml` 为 `wta-job` 写了 `${revision}`（工作树 `6.0.0`）。admin 剖面里的依赖**不写 version**，靠这张锁 |
| 可执行胖 jar | Spring Boot fat jar | `wta-admin/target/wta-admin.jar`（`<finalName>${project.artifactId}</finalName>`）。依赖落在 `BOOT-INF/lib/` |
| 默认激活 | `activeByDefault` | Maven：该 profile 在「同一 POM 没有别的 profile 被激活」时自动亮。被同 POM 的显式 `-P` 挤灭 |
| 环境剖面 | env profile | 父 POM 的 `local` / `dev` / `prod`。写 `profiles.active`，并给 Surefire `<groups>`。`dev` 也是 `activeByDefault`，但在**父** POM |
| 反应器 | Maven reactor | 根 `backend/pom.xml` 的 `<modules>`：admin / common / extend / modules / api。`wta-modules` 子列表含 `wta-job`。编译图 ≠ 外卖盒 |
| 客户端工具间 | `wta-common-job` | 自动配置类 `SnailJobConfig`。`AutoConfiguration.imports` 登记该类。业务侧**只有** `wta-job` 的 POM 依赖它 |
| 客户端电闸 | `snail-job.enabled` | `SnailJobConfig` 上 `@ConditionalOnProperty(..., havingValue="true")`。三份 yml 默认 `false`。本课对照，不当 covered |
| 调度服务器 | `wta-snailjob-server` | extend 独立进程，启动类 `org.namewta.snailjob.SnailJobServerApplication`。不在任一 bundle 的 admin 依赖里 |
| 产物门禁 | `verify-admin-bundle.sh` | 对已生成的 `wta-admin.jar` 做 `jar tf`。full 必须含 `BOOT-INF/lib/wta-job-`；core 含了就失败 |
| 跳过测试的两种写法 | `skipTests` vs `maven.test.skip` | 项目画像：full 打包用 `-DskipTests`；core 打包用 `-Dmaven.test.skip=true`。后者连测试类都不编译。都要求先有独立测试证据 |

### 机制/因果链

#### 1. 先数三份名单，不要凭记忆

文件：`backend/wta-admin/pom.xml`。

**常驻（`<profiles>` 之前）：** MySQL 驱动；admin 直接声明的 common（doc / social / mail / notify / nacos / mcp / openapi）；`wta-api`；`wta-system`；`wta-third`；`wta-notify`；`wta-sso`；Spring Boot Admin 客户端；测试依赖。这里**没有** `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`、两份档案。

**`bundle-full`：** 注释「默认全量部署组合；显式激活其他 bundle-* profile 时自动停用」。依赖六件，磁盘顺序：

1. `wta-job`（注释：调度任务模块）
2. `wta-ai`
3. `wta-demo`
4. `wta-workflow`
5. `wta-profile-person`
6. `wta-profile-enterprise`

**`bundle-core`：** 注释「显式激活后停用 bundle-full，仅保留上方平台基础依赖。」然后又写：「Core 保留档案查询与草稿能力，但不装配工作流引擎。」依赖两件：person、enterprise。没有 job。

口试时手指必须同时点到：**job 不在常驻、在 full、不在 core。** 只说「core 更瘦」不算满格——档案在 core 并不瘦掉。

#### 2. Maven 怎样熄灭默认灯

`bundle-full` 的灯是 `activeByDefault`。`bundle-core` **没有** `<activation>`，只能靠命令行 `-Pbundle-core`。

Maven 的规矩（和 POM 注释一致）：**同一份 POM 里**，一旦另有 profile 被显式激活，所有 `activeByDefault` 的灯灭。所以：

| 你敲的命令（在 `backend/`） | 父 POM 环境灯 | admin 组装灯 | `wta-job` 进不进 admin 依赖 |
| --- | --- | --- | --- |
| `./mvnw clean package -DskipTests` | `dev` 默认亮（`profiles.active=dev`） | `bundle-full` 默认亮 | **进** |
| `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true` | `dev` 仍在父 POM 默认亮 | core 显式亮，full 灭 | **不进** |
| `./mvnw clean package -Pprod`（发布 full 常用） | `prod` 亮，父 POM 的 `dev` 灭 | full 仍在**子** POM 默认亮 | **进** |
| `./mvnw ... -Pprod,bundle-core`（`release-manage.sh` 的非 full） | `prod` | core 亮，full 灭 | **不进** |
| `./mvnw ... -Pbundle-full`（发布 README 显式写法） | 视是否另带环境剖面 | full 被**显式**点亮 | **进** |
| `./mvnw ... -Pbundle-full,bundle-core` | 视环境 | **两根都亮**，依赖集合并 | **进**（core 挡不住） |

没有 enforcer、没有互斥插件。互斥靠「默认灯 vs 显式另一根」，不是靠「两根不能同时写」。把两根都写上 `-P`，job 会回来。口试若说「core 会把 full 的依赖删掉」，改成「core 亮时默认 full 灭；两根都显式亮则合并」。

环境剖面和组装剖面必须分开说。父 POM `dev` 也是 `activeByDefault`，但它只管 `profiles.active` 和 Surefire 的 `<groups>${profiles.active}</groups>`。它**不是** bundle。`-Pprod` 不会因为「我已经 -P 了」就在 admin POM 里熄灭 full——`prod` 不在那份 POM 里。

#### 3. 反应器仍编译工人，外卖盒可以没有糖

`backend/wta-modules/pom.xml` 的 `<modules>` 第二行就是 `wta-job`。根反应器永远认识这间房。从 `backend/` 跑 `./mvnw test` 或 `./mvnw package`，Maven 仍会走进 `wta-modules/wta-job` 做编译（它是反应器成员，不是「core 就当这间房不存在」）。

进不进 **admin 的运行时**，只看 admin 当时亮着的依赖。`spring-boot-maven-plugin` 配在 admin，目标 `repackage`。产物文件名 `wta-admin.jar`。依赖以 `BOOT-INF/lib/<artifactId>-` 前缀出现。

因此会出现这种看起来像矛盾、其实分层不同的现象：

- `backend/wta-modules/wta-job/target/wta-job-6.0.0.jar` 作为**模块自己的** jar 可能刚编出来；
- `backend/wta-admin/target/wta-admin.jar` 里面却没有 `BOOT-INF/lib/wta-job-`。

这就是 L-002 那句「reactor ≠ fat jar」落到 job 上的样子。本课口试要能指这两条路径。

`-pl wta-admin -am -Pbundle-core` 更瘦：`-am` 只补 admin **当时的**依赖。core 下 job 不是依赖，这条命令可以根本不编 `wta-job`。全反应器 `package` 仍会编。不要把两种命令说成同一件事。

#### 4. 类在 classpath，扫描才会看见工人

`NamewtaApplication` 在包 `org.namewta`，`@SpringBootApplication` **没有** `scanBasePackages`。默认扫启动类所在包及其子包，因此 `org.namewta.job.snailjob` 的 `@Component` 在 **job jar 已在 classpath** 时会进同一容器。

因果单向：

```text
POM 声明 wta-job
  → 编译/运行 classpath 有 org.namewta.job.*
    → 扫描到九个工人 Bean
      → 仍要等 SnailJob 客户端醒、主任点名
```

反方向不成立：YAML 里有 `snail-job` 段，**不能**让不存在的类出现。core 包里工人类不在，扫描写得再宽也扫不到。

L-078 已钉：九个 `@Component` 自己**没有**贴 `snail-job.enabled`。电闸贴在 `SnailJobConfig` 上。所以 full 包 + `enabled: false` 的日常开发形态是：**类可能在容器里闲着，主任这头仍点不着。** 本课只要你能把「类在不在」和「活干不干」拆开。

#### 5. 捎带关系：拔掉 job，客户端工具通常一起走

`wta-job/pom.xml` 依赖恰好两行：`wta-common-json`、`wta-common-job`。没有 web、没有 mybatis、没有 `wta-api`。

2026-09-17 全仓 `**/pom.xml` 里，**作为依赖消费** `wta-common-job` 的业务/组装 POM 只有 `wta-job`。`wta-common/pom.xml` 只是把它列为子模块；`wta-common-bom` 只锁版本。admin 常驻名单也没有它。

所以 core 剖面下，按依赖图：

- 没有 `wta-job` jar
- 通常也没有 `wta-common-job` jar
- 通常也没有 `snail-job-client-starter` / `snail-job-client-job-core` 被这根线拉进来
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 里那行 `SnailJobConfig` 不会随 admin 启动被加载

这是**对照**，用来解释为什么 core 不只是「少几个 `@Component`」。不要把 `wta-common-job` 本身标成本格 covered——工具间的所有权是 common 课的事。本课只要求：能说出这根传递边，并且不发明「system 也会把 job 客户端带进来」。

`wta-common-json` 不一样：别的模块也会用 JSON。core 拔掉 job **不会**把 json 工具间从 admin 拿走。口试不要说成「core 把 job 的两个依赖都删了」。

#### 6. 三道电闸，本课只守第一道

| 闸 | 磁盘开关 | 开了怎样 | 关了怎样 | 谁的课 |
| --- | --- | --- | --- | --- |
| 1. 组装 | `bundle-full` vs `bundle-core` | `BOOT-INF/lib/wta-job-*` 在 | 工人类不在 admin 进程 | **本课** |
| 2. 客户端 | `snail-job.enabled` | `SnailJobConfig` 进容器，`@EnableSnailJob` | 工人 Bean 仍可能在（若闸 1 开着），主任点不着 | 对照；配置在 admin yml |
| 3. 主任 | extend 进程是否启动 | 有人按花名册点名 | 客户端醒了也没人派活 | 范围外内部；L-002 变式 D 点名 |

日常工作树三份 yml 闸 2 都是 `false`。所以「我用默认 full 打了包」**不等于**「演示任务会跑」。OBJ-79 的动词是「进入 admin jar / 排除」，不是「任务跑起来」。

闸 3 的 artifact 是 `wta-snailjob-server`，依赖 `snail-job-server-starter`。它在 `backend/wta-extend/pom.xml` 的 `<modules>` 里，跟 admin 的 bundle **平行**。把它写进 admin `<dependencies>` 会把另一进程的启动类煮进 `org.namewta` 扫描——L-002 已禁。本课重复这句，是怕有人把「接线 wta-job」理解成「把服务器塞进 admin」。

#### 7. 门禁怎么数，测试怎么没数 job

**脚本** `scripts/ci/verify-admin-bundle.sh`：

- 参数只许 `full` 或 `core`，否则退出码 2
- 默认读 `$workspace_root/backend/wta-admin/target/wta-admin.jar`，可用 `ADMIN_ARTIFACT` 覆盖
- 没有产物：退出码 1
- `jar tf` 后用子串 `BOOT-INF/lib/$1-` 判断
- **两个剖面都必须有：** `wta-system`、`wta-common-notify`、`wta-common-oss`、`wta-third`、`wta-sso`
- **full 必须有、core 不得有：** `wta-job`、`wta-ai`、`wta-demo`、`wta-workflow`

`wta-common-oss` 不在 admin 常驻名单里，它是 `wta-system` 的直接依赖，经传递进 fat jar。脚本认传递边。它**不**检查 `wta-notify` 这个业务模块名（它检查的是 `wta-common-notify`），**不**检查两份档案，**不**检查 `wta-common-job`。本格的硬证据是脚本对 `wta-job` 的那两行 if。

`scripts/README.md` 的表格把「必须包含」写成三件，漏了脚本里的 `wta-third` / `wta-sso`。口试以**脚本源码**为准。

**Java 测试** `ProfileModuleGraphContractTest`（`@Tag("dev")`）：

- 读 `wta-admin/pom.xml` 文本，按 `<id>bundle-full</id>` / `<id>bundle-core</id>` 切出剖面 XML
- 断言 full 含 person、enterprise、**workflow**
- 断言 core 含 person、enterprise，**不含 workflow**
- **一句都没有写 `wta-job`**

默认 `./mvnw test` 时父 POM `dev` 亮，Surefire `<groups>dev</groups>`，这个测试会跑。它保护的是档案+工作流的组装差，是邻居。不要把它说成 OBJ-79 的断言。

项目画像要求两次打包都 `clean`，避免上一次 profile 的 `wta-admin.jar` 留在 `target/` 里骗过脚本。full 命令是 `./mvnw clean package -DskipTests`；core 是 `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`。顺序约定：完整测试先过，再打 core 包。

#### 8. 发布脚本怎样拼 `-P`

`release-artifacts/scripts/release-manage.sh` 的 `package_backend`：

```text
maven_profiles = env_name          # full
maven_profiles = env_name,bundle-core   # 非 full
./mvnw clean package -DskipTests -P${maven_profiles}
```

full 发布**不写** `bundle-full`，靠子 POM 默认灯。core 发布必须把 `bundle-core` 和 `dev`/`prod` 写在同一串 `-P` 里：环境灯在父 POM，组装灯在子 POM，互不挤灭。

`release-artifacts/README.md` 另给一条显式 `./mvnw -f backend/pom.xml -Pbundle-full -DskipTests package`。那是把默认灯改成显式灯，结果仍是 job 进盒。两种写法都合法；不要发明第三种「不写任何 profile 就不组装」——不写 bundle 时 full 仍然亮。

运行时代码生成器不在 Maven 模块图，也不在任一 bundle。不要在插座板上找 `wta-gen`。

## 图、表或文本图

**图题 / caption：** 2026-09-17 宏观插座板：两根插排，job 只插在默认那根。alt：上为常驻电器；中为 bundle-full 含 wta-job；下为 bundle-core 无 wta-job，档案仍在。

```text
wta-admin 常驻
  wta-api  wta-system  wta-notify  wta-sso  wta-third
  （common-doc/social/mail/notify/nacos/mcp/openapi …）

        bundle-full [默认亮]
          + wta-job      ← 本课这一颗
          + wta-ai
          + wta-demo
          + wta-workflow
          + person / enterprise

        bundle-core [ -P 才亮，同时灭掉上面那根默认灯 ]
          + person / enterprise
          （没有 job / ai / demo / workflow）

        永远不在这板子上
          wta-snailjob-server   （另一进程）
          运行时代码生成器       （已删除）
```

**文字等价物：** 大厅常驻四间业务房加 api。过年插排再加 job、AI、demo、流程和档案。核心店插排只加档案。job 不在核心店。调度服务器不插这板。

**图题 / caption：** 从 `-P` 到 `BOOT-INF/lib`。alt：左列命令；中列亮着的 profile；右列 jar 里有没有 wta-job-。

```text
不带 bundle -P
    父 dev 默认 + 子 full 默认
    → wta-admin.jar 含 BOOT-INF/lib/wta-job-6.0.0.jar

-Pbundle-core
    父 dev 默认 + 子 core（full 灭）
    → 不含 wta-job-

-Pprod
    父 prod + 子 full 默认
    → 含 wta-job-     （生产 full）

-Pprod,bundle-core
    父 prod + 子 core
    → 不含 wta-job-   （发布脚本的非 full）

-Pbundle-full,bundle-core
    两根都显式亮，依赖合并
    → 含 wta-job-     （脚会把 core 校验打红）
```

**文字等价物：** 默认或显式 full 都把 job 打进盒。只点 core 时盒里没有。环境和组装是两排开关。两根组装插排一起点，job 会回来。

**图题 / caption：** 三道闸不是一扇窗。alt：第一闸 Maven；第二闸 yml；第三闸 extend 进程。

```text
[1] bundle-full ? ──否──> admin 进程没有工人类     ← 本课
        │是
        v
[2] snail-job.enabled=true ? ──否──> 类在、客户端不醒
        │是
        v
[3] SnailJob Server 进程活着？ ──否──> 没人点名
        │是
        v
    按 executor_info 拍工人肩膀     ← L-078 的车间
```

**文字等价物：** 本课只回答第 1 问。后两问开着，core 包仍然没有工人。第 1 问开着、后两问关着，是仓库默认的 full + `enabled: false`。

## 正例、反例与边界

**正例 1 — 指 POM。** 打开 `wta-admin/pom.xml`。上半段 `<dependencies>` 搜不到 `<artifactId>wta-job</artifactId>`。`bundle-full` 剖面第一颗就是它。`bundle-core` 剖面两颗都是档案。OBJ-79 这一句成立。

**正例 2 — 默认打包。** 在 `backend/` 执行画像中的 `./mvnw clean package -DskipTests`。不写 `-P`。子 POM 的 full 灯亮。产物 `wta-admin/target/wta-admin.jar` 经 `jar tf` 能看到 `BOOT-INF/lib/wta-job-`。再跑 `scripts/ci/verify-admin-bundle.sh full`，脚本承认这颗糖。

**正例 3 — 显式 core。** `./mvnw clean package -Pbundle-core -Dmaven.test.skip=true`。full 灯灭。同一条 `jar tf` 看不到 `BOOT-INF/lib/wta-job-`。`verify-admin-bundle.sh core` 若仍看见它，会打印 `core bundle unexpectedly contains: wta-job` 并失败。

**正例 4 — 生产 full 只带环境灯。** `-Pprod` 不带 bundle 名。父 POM 换成 prod YAML，子 POM 的 job 插头仍在。这就是发布脚本 `bundle=full` 的路径。

**正例 5 — 生产 core 两盏灯。** `-Pprod,bundle-core`。环境是 prod，组装是 core。job 不进盒。档案仍进盒。

**正例 6 — 反应器里的工人还在。** core 打包之后，`wta-modules/wta-job` 作为模块仍在反应器名单里。你能编译它，能在磁盘上找到模块自己的 jar。你不能从 core 的 admin fat jar 里 `jar tf` 出工人。

**正例 7 — 传递客户端。** 打开 `wta-job/pom.xml` 圈 `wta-common-job`。再全仓搜消费它的业务 POM，只有这一处。core 拔掉 job 之后，不要指望 admin 里还有 `SnailJobConfig`。

**正例 8 — 常驻对照。** core 包仍必须通过脚本对 `wta-system` / `wta-third` / `wta-sso` 的检查。拔掉 job 不是把大厅拆了。

**正例 9 — 档案对照。** 用同一份 POM 说明：person / enterprise 在 full **和** core。job 只在 full。口试能把这两颗插头分开，才算真懂「排除」二字。

**正例 10 — clean。** 先打 full，再打 core 却忘了 `clean`。`target/wta-admin.jar` 可能仍是上一份。脚本会在 core 校验里喊「unexpectedly contains: wta-job」。项目 README 把 `clean` 写成每次 bundle 构建的硬步骤。

**反例 1 — 「`wta-job` 是 admin 常驻模块。」** 常驻名单没有它。

**反例 2 — 「`-Pbundle-core` 只是把 `snail-job.enabled` 改成 false。」** core 改的是 classpath。yml 两个剖面都是 false，那是闸 2，不是闸 1。

**反例 3 — 「reactor 有 job，运行时就有工人。」** 编译图不是外卖盒。

**反例 4 — 「core 连档案、通知、SSO 一起拔掉。」** 档案在 core。通知 / SSO / third / system 是常驻。

**反例 5 — 「把 `wta-snailjob-server` 写进 admin POM 就算接线 job。」** 那是另一进程。硬插会把主任启动类煮进扫描。

**反例 6 — 「`ProfileModuleGraphContractTest` 失败说明 job 没打进包。」** 该测试不读 `wta-job` 四个字。

**反例 7 — 「frontend README 写了访问 `wta-job`，所以浏览器有 job 厨房。」** 2026-09-17 没有 `packages/domains/job`，也没有 `web-domains/job`。后端装配是 POM，不是 Vue 工厂。

**反例 8 — 「`-Pbundle-full,bundle-core` 会得到纯 core。」** 两根都显式亮，job 在。

**反例 9 — 「父 POM 的 `-Pprod` 会熄灭 bundle-full。」** `prod` 不在 admin POM。full 默认灯继续亮。

**反例 10 — 「本课覆盖 ai / demo / workflow 的 bundle 格。」** 同坐一根插排，矩阵只点了 job。

**反例 11 — 「YAML 还有 `snail-job:`，core 里客户端一定还在。」** 配置键可以空放着。类不在就不加载 `SnailJobConfig`。

**反例 12 — 「`wta-common-json` 也会随 job 被 core 拿掉。」** json 还有别的消费者。被捎走的是 **只** 给 job 用的 `wta-common-job`。

**反例 13 — 「verify 脚本检查 `wta-notify` 模块。」** 它检查 `wta-common-notify`。业务模块 `wta-notify` 是常驻，但不在脚本那份 required 数组里。

**反例 14 — 「不写 `-P` 就是 core。」** 不写 bundle 名时 full 亮，job 在。

**反例 15 — 「OBJ-79 要口述 `AlipayBillTask` 怎么加金额。」** 那是 L-078。本课指插头。

**边界 1 — 脚本不检查档案。** person / enterprise 两个剖面都该在，但 `verify-admin-bundle.sh` 不读这两个名字。档案的 POM 断言在 `ProfileModuleGraphContractTest`。job 的 jar 断言在脚本。各管各的。

**边界 2 — 版本号来自父锁。** 剖面依赖不写 `<version>`。`BOOT-INF/lib/wta-job-6.0.0.jar` 里的 `6.0.0` 是根 POM `<revision>`。revision 变了，脚本仍只匹配前缀 `wta-job-`。

**边界 3 — `@Tag("dev")` 与环境灯绑在一起。** 默认测试跑 `ProfileModuleGraphContractTest`。若有人只用 `-Pprod` 跑测试，Surefire groups 变成 `prod`，这个邻居测试可能根本不进本次 Surefire。它仍然**不是** job 的 jar 门禁。

**边界 4 — 本课未把「已打出的 jar」当现场证据。** 口试证据链是 POM + 脚本 + 画像命令。真正的 `jar tf` 要在 `clean package` 之后做。没有产物时脚本直接失败。

**边界 5 — `maven.test.skip` 与 `skipTests`。** 画像给 core 用前者，给 full 用后者。不要把两条命令抄反，也不要说成「core 打包会跑测试」。测试应由前置 `./mvnw test` 承担。

**边界 6 — 传递 `wta-common-oss`。** 脚本对两个剖面都要看到它。它来自 system，与 job 无关。用来提醒：fat jar 里的名字可以是传递依赖。job 不是传递来的，是 full 剖面**直接**声明的。

**边界 7 — 没有第三根 `bundle-*`。** 磁盘上 admin POM 只有 full 和 core。不要发明 `bundle-job`。

**边界 8 — classic 登记不随剖面改变。** `03-backend-module-modes.md` 里 `wta-job` 仍是 classic，无论它进不进某个 fat jar。拔掉插头不是改模式。

**边界 9 — 前端 dual-bundle 不存在。** admin-web 的 `services.ts` 组合的是浏览器厨房。后端 core 不会让前端自动少一个 Vue 包。菜单若仍指向任务调度，那是 iframe 看主任楼（L-024），不是工人进了浏览器。

**边界 10 — Skill 摘要与 POM。** 模块地图写「默认 bundle-full 接入 job/ai/demo/workflow/profile，显式 bundle-core 排除 job/ai/demo/workflow、保留 profile」。本课以 POM 逐条核对后，这句话对 job 成立。若摘要以后改了，以当时 `wta-admin/pom.xml` 为准。

## 变式与迁移

1. **和 L-002 对照。** 总览课把两根插排画在 C4 容器上，本课把 job 这一颗插头放大到能指 XML、能指 `BOOT-INF/lib`、能指脚本。箭头方向不变。变式 D「工人在 admin、主任在 extend」仍成立：本课只决定工人**在不在** admin。
2. **和 L-078 对照。** 车间里的九个类、花名、DAG 袋子是上一课。没有 full 插头，那些类进不了这个进程。有插头、电闸关着，类可以闲着。两课不要并成一句「job 模块没启用」。
3. **和 L-003 对照。** 登记 classic 不取决于 bundle。core 包里没有工人，不是「job 改成了 layered」或「模块被注销」。
4. **和 L-024 对照。** 菜单「任务调度」是 iframe。core 拔掉工人，玻璃仍可能在——它连的是主任楼的 URL。不要用「页面还在」证明 job 打进了 jar。
5. **和 L-080 对照。** `wta-ai` 也只在 full。它**有** `SnailAiController` 这扇 HTTP 窗。job 没有窗。不要因为「都是 full-only」就把 ai 的注册接口说成 job 也有。ai 的格子不是本课。
6. **和发布对照。** `release-manage.sh` 用环境灯 + 可选 `bundle-core`，不用第三种 profile 名。迁移发布流程时，先问 full 还是 core，再决定要不要把 `bundle-core` 拼进 `-P`。
7. **迁移：想在 core 也跑定时。** 那是改组装合同，不是改 yml。要把 `wta-job` 写进 core 剖面或常驻名单，同时改 `verify-admin-bundle.sh` 对 core 的「不得包含」——否则门禁会打红。先问这是不是真的要把调度工人变成平台常驻。
8. **迁移：只想要客户端、不要九个演示工人。** 磁盘上客户端跟工人绑在同一根 `wta-job` → `wta-common-job` 边上。不要偷偷让 admin 直接依赖 `wta-common-job` 来「绕过 bundle」——那会让 core 也加载 `SnailJobConfig`，和本课契约分叉。新能力默认另登 layered 模块，不要在 classic 房间开阁楼。
9. **迁移：新增一个 full-only 业务模块。** 学 job 的写法：不要放进常驻名单；放进 `bundle-full`；确认 `bundle-core` 不声明它；扩展 verify 脚本的 `optional` 数组；反应器的 `wta-modules` 仍要有 `<module>`。本课不替 ai/demo/workflow 盖章。

## 常见误区

1. **「admin POM 很长，所以 job 一定常驻。」** 长度来自常驻平台 + 两根插排。job 在默认那根里。
2. **「core 就是把 enabled 关掉。」** core 是不请这件电器。电闸是另一道。
3. **「`-P` 了任意名字，full 就会灭。」** 要灭的是**同一 POM** 里的默认灯。父 POM 的 `prod` 灭不了子 POM 的 full。
4. **「模块列表里划掉 wta-job 才叫 core。」** 不要改 `wta-modules/pom.xml` 来模拟剖面。那会让反应器丢房间。
5. **「fat jar 里没有 job，所以源码被删了。」** 源码仍在 `wta-modules/wta-job`。只是这趟外卖没装这颗糖。
6. **「脚本绿了就证明任务能跑。」** 脚本只数 jar 名。它不启动主任，不改 `enabled`。
7. **「Java 契约测试已经覆盖 job。」** 没有。它覆盖档案与 workflow。
8. **「README 表格和脚本完全一样。」** 表格缩写了 required 列表。以脚本数组为准。
9. **「把两根 -P 写一起更保险。」** 更保险的是只写 core。一起写会把 job 请回来。
10. **「前端也会按 bundle-core 少打一个包。」** 前端没有这根 Maven 插排。
11. **「`wta-common-job` 是 admin 直接依赖。」** 不是。它跟工人走。
12. **「iframe 还能打开，说明 core 仍含 wta-job。」** 玻璃看的是主任楼。
13. **「本课讲完 job 域就全覆盖。」** 工人是 L-078。厂商引擎内部仍出范围。
14. **「常驻里的 `wta-common-notify` 等于 `wta-job` 这种可选件。」** 通知工具间是常驻；调度工人是 full-only。
15. **「OBJ-79 要默写九个类名。」** OBJ-79 的动词是对照 pom：进 jar / 排除。

## 非评分暂停

打开磁盘，不要凭记忆默写路径。不要改文件。没有标准答案栏，没有分数。

1. 打开 `backend/wta-admin/pom.xml`。在顶部 `<dependencies>` 确认没有 `wta-job`。在 `<profile><id>bundle-full</id>` 圈出第一颗 `wta-job` 和 `activeByDefault`。在 `<id>bundle-core</id>` 确认只有两份档案、没有 job。圈那句「显式激活后停用 bundle-full」。
2. 打开 `backend/pom.xml`。圈 `<revision>6.0.0</revision>`、`dependencyManagement` 里的 `wta-job`、父剖面 `local`/`dev`/`prod`（`dev` 的 `activeByDefault`）。确认它们不声明 `wta-job` 为依赖，只锁版本或改 `profiles.active`。
3. 打开 `backend/wta-modules/pom.xml` 圈 `<module>wta-job</module>`。打开 `wta-job/pom.xml` 圈 `wta-common-json` 与 `wta-common-job`。打开 `scripts/ci/verify-admin-bundle.sh` 圈 `optional=(wta-job …)` 以及 full / core 两支 if。打开 `ProfileModuleGraphContractTest` 确认断言列表没有 job。
4. 打开 `release-artifacts/scripts/release-manage.sh` 的 `package_backend`。圈 `bundle == full` 时只用 `env_name`，否则 `env_name,bundle-core`。打开 `NamewtaApplication` 确认无 `scanBasePackages`。打开任一份 `application-*.yml` 圈 `snail-job.enabled: false`。不要改这些文件。

## 总结、词汇表与下一步

- **宏观插座板：** `wta-admin` 用两根 Maven 插排决定请哪些业务房间进同一个进程。job 这颗插头只焊在默认的 `bundle-full` 上。
- **OBJ-79：** 对照 POM 能说明——`bundle-full` 把 `wta-job` 打进 admin jar；显式 `bundle-core` 把它排除。常驻名单和 core 剖面都没有这颗插头。
- **默认灯怎么灭：** 同一 POM 里显式点亮 core，full 的 `activeByDefault` 熄灭。父 POM 的 `dev`/`prod` 是环境灯，灭不了这根组装灯。两根组装灯一起显式亮，依赖合并，job 会回来。
- **反应器 ≠ 外卖盒。** `wta-modules` 永远编译得起工人；`wta-admin.jar` 的 `BOOT-INF/lib/wta-job-` 才是客人盘子。门禁脚本数的是盘子。
- **三道闸分开说。** 本课守「类在不在 jar」。客户端醒不醒是 yml。主任在不在是 extend。默认工作树常常是闸 1 开、闸 2 关。
- **本格只盖 job。** 同坐 full 插排的 ai / demo / workflow / 档案，以及九个工人怎么干活，都不在 `C:bundle-full-job`。

词汇表：`wta-admin` / `wta-job` / `bundle-full` / `bundle-core` / `activeByDefault` / always-on / `dependencyManagement` / `${revision}` / reactor / fat jar / `BOOT-INF/lib` / `spring-boot-maven-plugin` / `repackage` / `NamewtaApplication` / `wta-common-job` / `SnailJobConfig` / `snail-job.enabled` / `verify-admin-bundle.sh` / `ProfileModuleGraphContractTest` / `profiles.active` / `skipTests` / `maven.test.skip` / `wta-snailjob-server` / `C:bundle-full-job`。

下一步：L-080 才是 `SnailAiController.registerCurrentUser` 那扇真正的 HTTP，同样只在 full 进厅，但本课不盖章。工人怎么被点名保持 L-078。监控 iframe 保持 L-024。厂商调度内核保持 deferred。本课结束不发作业、不打分；要练习请之后主动激活 Homework。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-001 | `NamewtaApplication.java` | 启动类在 `org.namewta`；无 `scanBasePackages`；job 包能被扫到的前提是 jar 在 classpath | `backend/wta-admin/src/main/java/org/namewta/NamewtaApplication.java` | 2026-09-17 |
| S-007 | `01-module-map.md` | admin 组装；默认 full 接入 job/ai/demo/workflow/profile；显式 core 排除 job/ai/demo/workflow、保留 profile | 「依赖方向」段 | 2026-09-17 |
| S-L002-01 | 子课 L-002 | 常驻 vs full vs core 的插排图；reactor ≠ fat jar；变式 D 工人在 admin、主任在 extend | `children/2026-09-14-namewta-architecture/lessons/L-002-backend-assembly.md` | 2026-09-17 |
| S-L078-06 | `wta-job/pom.xml`；模块目录 | 只依赖 json + common-job；无 HTTP 窗。本课用它解释传递边 | artifact `wta-job` | 2026-09-17 |
| S-L079-01 | `backend/wta-admin/pom.xml` | 常驻无 job；full 直接声明 `wta-job` 且默认激活；core 无 job、有档案；注释写明显式激活停用 full | `<dependencies>`、`<profiles>` | 2026-09-17 |
| S-L079-02 | `scripts/ci/verify-admin-bundle.sh`；`scripts/README.md` | `jar tf` 契约：full 必须含 `wta-job`，core 不得含；required 以脚本数组为准；须先有产物 | `optional=(wta-job …)`；full/core 两支 if | 2026-09-17 |
| S-L079-03 | `ProfileModuleGraphContractTest.java` | 邻居测试只断言档案 + workflow，不断言 job；`@Tag("dev")` | `fullAndCoreBundlesBothAssembleProfileWhileWorkflowRemainsFullOnly` | 2026-09-17 |
| S-L079-04 | `backend/pom.xml`；`wta-modules/pom.xml` | revision / dependencyManagement 锁 `wta-job`；父环境剖面；反应器永远含 job 模块 | `<revision>`；`<modules>`；`local`/`dev`/`prod` | 2026-09-17 |
| S-L079-05 | `SnailJobConfig.java`；`AutoConfiguration.imports`；`application-{local,dev,prod}.yml` | 闸 2 默认 false；自动配置在 common-job；仅 job 消费该 common | `wta-common-job`；admin 配置 | 2026-09-17 |
| S-L079-06 | `release-manage.sh`；`release-artifacts/README.md` | full 只带环境 `-P`；非 full 拼 `bundle-core`；另有显式 `-Pbundle-full` | `package_backend`；构建示例 | 2026-09-17 |
| S-L079-07 | `00-project-profile.md`；`backend/README.md`；根 `README.md` | 双 bundle 打包命令；core 用 `maven.test.skip`；每次必须 `clean` | 门禁表；「构建与验证」 | 2026-09-17 |
| S-L079-08 | `wta-extend/pom.xml`；`SnailJobServerApplication`；admin-web README | 主任另一进程；前端 README 提到 `wta-job` 但无 job domain 包 | extend 模块列表；`frontend/apps/admin-web/README.md` 「后端映射」 | 2026-09-17 |

## 文字等价物

本课所有 ASCII 图与对照表都可以用这段话代替：`wta-admin` 是一块宏观插座板。常驻插着 system / notify / sso / third 和 api。默认那根叫 `bundle-full` 的插排再插上 `wta-job`（以及 ai、demo、workflow 和两份档案）。显式点亮 `bundle-core` 时，同一 POM 里的默认灯灭，job 不进 admin 的依赖，打出来的 `wta-admin.jar` 的 `BOOT-INF/lib` 里不该出现 `wta-job-`。父 POM 的 `dev`/`prod` 只选 YAML，不插这颗插头。反应器照样能编译 `wta-modules/wta-job`，那不证明客人盘子里有糖。`wta-job` 会捎上 `wta-common-job`；core 通常连客户端自动配置一起没有。类在不在 jar、客户端开不开、主任在不在，是三道闸。本课只把第一道闸认全。矩阵格子是 `C:bundle-full-job`，不要把同排的别的模块或九个工人算进来。
