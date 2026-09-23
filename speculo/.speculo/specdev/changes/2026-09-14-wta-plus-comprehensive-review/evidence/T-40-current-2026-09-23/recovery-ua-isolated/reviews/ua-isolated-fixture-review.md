# T40 UA 隔离测试夹具复审（6eb5783c → ee8e6d83 → b7a12bc1）

结论：**上一轮 static-final 同 JVM 污染阻断已由隔离设计关闭；`ee8e6d83` 本身有 Java 编译错误，`b7a12bc1` 仅修两处字符串字面量后，静态审查无新阻断。** 本报告只复核 `backend/wta-admin/src/test/java/org/namewta/test/auth/LoginUserAgentUnitTest.java` 的固定提交增量，未运行测试/服务，不能代替 Lead 正在执行的目标红灯或随后默认 reactor 验收。

## 隔离与原断言

- `ee8e6d83:67–92` 父 JUnit 仍是三组 `missing/blank/chrome` 参数化测试加一组预填 browser/os 测试，共 **10 项**；第 94–120 行父进程仅创建子 JVM、读取退出码及固定 `OK` 结果。原 `@BeforeAll`/`@AfterAll` 临时 Spring `ApplicationContext` 已从父测试生命周期删除。
- 第 137–202 行临时 `SpringUtils`/mock Redisson/`new Converter()` 仅在 child `main` 中装配与清理。`RedisUtils` 的 static-final `CLIENT`、`MapstructUtils` 的 static-final `CONVERTER` 即使在 child 首次初始化也随该 JVM 结束；父默认 Surefire JVM 不通过本类初始化这两个单例。类引用/import 和 `LoginUserAgentUnitTest.class.getName()` 不执行这些生产方法。上次报告所述跨测试污染已闭合；不需要改 POM 或全局测试标签。
- 第 204–317 行保留原方法体的关键观察：真实 `LoginHelper.login` 方法调用、预填字段保持与 session/令牌入口；真实成功监听器的在线 DTO、Client/device/部门及登录日志/最近登录调用；真实 `SysLoginInfoServiceImpl.recordLoginInfo` 的 mapper 实体、status、Client、message；`RequestContextHolder` 仍在 `finally` 复原。`assertDoesNotThrow` 删除后，未捕获异常自然传出 child，父测试要求进程 exit 0 且结果 `OK`，**未放宽通过条件**。这些仍是直接方法与 mock 持久化测试，`@Async`/真实 Redis/MySQL/完整登录由 Lead 独立实测覆盖。

## 子进程资源与失败闭合

- `ee8e6d83:96–97` 曾以 `'.result'`、`'.log'` 表示多字符 Java 字符常量，**编译失败**；Lead 已保留该真实失败。`b7a12bc1` 仅将其改为 `".result"`、`".log"` 两个字符串字面量，隔离逻辑与断言未变。该 delta 消除这两处已知语法错误，不代称整类已编译/运行通过。
- 第 95–110 行优先 `surefire.test.class.path`，回退 `java.class.path`；仓内已有同样优先级的子 JVM 模式。参数只由固定 consumer/case 枚举构造；使用当前 `java.home`、`-Xmx384m`、`@TempDir` 结果及合并输出文件，避免管道阻塞。结果必须同时满足 `exitValue()==0` 与 `outcome==OK`，缺结果为 `NO_RESULT` 失败。
- 第 105–119 行每案最多等待 45 秒，`finally` 始终调用 `stopChildTree`，并删除本案结果/日志；第 123–135 行强制终止存活 parent、其 descendants 快照，等待 parent 5 秒、descendants 最多 5 秒，尚存活则断言失败。child 测试代码自身没有再启动外部进程，当前快照边界足够；失败/超时不应留下测试 JVM。`@TempDir` 也是每个 JUnit case 自己的临时目录。
- 第 138–169 行 child 只接受固定四种消费者与三种 UA 案例，`existing` 限定 missing；业务断言、NPE、夹具错误或 cleanup 错误均不能生成绿色结果。对于未列入 catch 的 `Error`，`finally` 可能写 `OK`，但 JVM 非零退出仍令父断言失败；这是**非阻断诊断限制**。父测试最终删除 child 原始日志，未来非预期失败只留固定 `NPE/ASSERTION/HARNESS/CLEANUP` 类别和 Surefire 断言；若 Lead 需要更细排障，单独保存安全脱敏日志，不应为了诊断放宽退出门禁。

## 验证边界

仅源码可以判定隔离设计成立；`ee8e6d83` 编译失败必须计入历史，不能作为已跑红灯。还需 Lead 实际核 **`b7a12bc1`** 的目标红灯保持 **10 项、预期六个空 UA NPE、四个正例**，并确认 child classpath、45 秒预算、清理都正常。生产修复后同源十项零失败/零 skip，随后默认 dev reactor、独立真实 HTTP/DB 及 T40 原门禁仍须按当前票执行。不可用本次静态 PASS 追认任何未完成运行。
