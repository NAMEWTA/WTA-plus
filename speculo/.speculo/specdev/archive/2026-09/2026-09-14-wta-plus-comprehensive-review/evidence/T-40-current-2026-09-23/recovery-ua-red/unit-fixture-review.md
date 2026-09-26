# T40 UA 单元红灯夹具审查（固定 6eb5783c）

结论：**行为断言方向可接受；当前夹具不能进入默认 reactor 同 JVM 验收，需先隔离静态单例。** 只读审查 `backend/wta-admin/src/test/java/org/namewta/test/auth/LoginUserAgentUnitTest.java` 于 `6eb5783c`，未构建、运行或改仓库。Lead 已提供该固定点目标红灯 10 tests、6 NPE、4 positive 的事实；这证明目标缺陷可触达，但不消除以下全套测试污染风险。

## 断言覆盖与边界

- 第 87–92 行参数化为缺头 `null`、空白头、Chrome 正控制；第 95–119 行直接调用真实 `LoginHelper.login`，断言令牌入口被调用、会话写入与原 IP/位置/device 不变；第 121–137 行验证预存 browser/os 不被覆盖。这些与窄修合同吻合，但 `StpUtil` 是 static mock，不能将其称为完整 Sa-Token 登录。
- 第 140–166 行直接调用真实 `UserLoginSuccessListener.handleLoginSuccess`，核在线 DTO 的 browser/os/token/Client/device/部门，以及 `recordLoginInfo`、最近登录更新调用；Redis 被 static mock，只证明调用与 DTO，不证明 Redis 真写入或同步事件发布链。
- 第 169–196 行直接调用真实 `SysLoginInfoServiceImpl.recordLoginInfo`，捕获 mapper.insert 实体并核 status、Client、message 和 UA 描述；`@Async` 代理与真实 MySQL 均未进入，不能替代 Lead 独立 JVM 的 HTTP/DB 矩阵。`withRequest` 第 198–215 行有 `finally` 恢复原 RequestAttributes，局部线程状态处理可接受。
- 3×3 参数化加 1 预存值用例共 10 个 JUnit 项；当前红灯的六个 NPE 与三处 UA 消费者的 null/blank 断言相符。Chrome 正控制及预存值通过可区分修复边界。

## 阻断：跨测试 JVM 的静态 final 污染

- 第 62–75 行 `@BeforeAll` 把 Hutool `SpringUtil` 全局 ApplicationContext/BeanFactory 改成临时 `GenericApplicationContext`，注册 mock `RedissonClient` 和 `new Converter()`。第 77–85 行 `@AfterAll` 仅恢复 `SpringUtil` 两个可变静态引用；本地 Hutool 5.8.47 `SpringUtil.java:40–55` 确实只是赋值。
- 生产 `RedisUtils.java:29` 的 `private static final RedissonClient CLIENT = SpringUtils.getBean(...)` 与 `MapstructUtils.java:20` 的 `private static final Converter CONVERTER = SpringUtils.getBean(...)` 在各类**首次初始化**时固定对象。第 152 行 `mockStatic(RedisUtils.class)` 有初始化路径；第 186 行真实 `recordLoginInfo` 进入 `insertLoginInfo`（生产 `SysLoginInfoServiceImpl.java:131–135`），必经 `MapstructUtils.convert`。在临时 context 下先加载时，两者会把 mock Redis/临时 Converter 保留到此 JVM 结束，`@AfterAll` 无法回滚；若先由其他测试加载，则行为反过来依赖测试顺序。此问题不需等待一个后续测试实际失败才成立。
- 临时 `new Converter()` 也不同于生产 Spring 的 `MapstructAutoConfiguration`：后者构造 `new Converter(SpringConverterFactory(applicationContext))`，前者用 `DefaultConverterFactory`。因此该测试自己的审计映射不完全等同生产装配。测试可以作为窄单元断言，真实映射/DB 仍由隔离矩阵证明。
- 第 52 行 `@Tag("dev")` **不会排除默认门禁**：`backend/pom.xml:93–104` 的 `dev` profile 默认激活，`:552–559` Surefire `groups=${profiles.active}`，所以默认 reactor 会运行该类。独立目标红灯 JVM 通过/失败不会证明默认 reactor 无污染。

## 建议的最小修复与验证

接受将三个真实消费者用例的临时 Spring/Redis/Converter 夹具放入**有界子 JVM**，父 JUnit 类仅保留 10 项 case/断言与子进程结果核验，且父进程不得在 `@BeforeAll` 初始化临时 SpringUtils 或触发上述两个 static final。无需修改生产 POM/Surefire 配置，也不要减少 null/blank/Chrome、预存字段、Client/status/在线 DTO 的正负断言。必要时 child 每次只运行一个 case；断言在 child 失败时以非零退出码传播给 parent，父进程不能仅检查“启动成功”。

classpath 优先 `System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"))`；仓内 `ThirdResilienceRecoveryIntegrationTest.java:56–59` 已用此模式。仅用 `java.class.path` 在 Surefire fork 可能得到 booter classpath，必须通过当前目标命令验证。子进程把 stdout/stderr 重定向至有界临时输出避免管道卡住，30–60 秒明确超时；`finally` 对仍存活的 child（以及如有的 descendants）执行终止与有限等待，断言实际 exit/timeout。临时输出只能含合成标识和失败栈，不写凭据/token。子 JVM 内的临时 Spring context、RedisUtils/MapstructUtils 生命周期可随进程结束，不污染默认 reactor 的其他类。

修夹具后需重跑目标红灯，确保仍是预期的 6 个产品空值失败、4 个正例，并在生产窄修后同源 10/0/0/0；再由 Lead 串行完整默认 reactor 和独立真实 HTTP/DB 验证。当前固定点**不应标记默认门禁通过**。本报告未执行这些命令。
