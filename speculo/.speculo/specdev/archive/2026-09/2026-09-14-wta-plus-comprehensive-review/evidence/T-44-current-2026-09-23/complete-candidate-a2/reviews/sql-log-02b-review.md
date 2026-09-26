# T44 02B SQL 日志补修独立只读审查

审查时 HEAD：`f296b7854dcbc01136797075fc1e54fcfcccfe44`。Lead 冻结提交：`958aad6174c880fbf8c7afbfb8f657d37c968979`。下列四文件在冻结提交中的 SHA-256 与审查时逐一相同：

| 文件 | SHA-256 |
| --- | --- |
| `backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/interceptor/SqlLogInterceptor.java` | `e623195277d8f9254741f4aafd768b5492551fbdc92ee5665090e0682614f4aa` |
| `backend/wta-common/wta-common-mybatis/src/main/java/org/namewta/common/mybatis/config/properties/SqlLogProperties.java` | `e26f9b32cc2caea9c17a713c883623f998af5e523d5c9be9edd37e602d5673e7` |
| `backend/wta-admin/src/main/resources/application-dev.yml` | `e25658cc7585ed52749f51d7704f21edce05daa023bd73c6b5294d908336b354` |
| `backend/wta-admin/src/test/java/org/namewta/test/oss/config/OssSqlLogRedactionUnitTest.java` | `dc0520e9610b293cbec5b842aeaf02508b9547bdd6b7e848b8127cd94a1b2e7f` |

**结论：无阻断项。** 拦截器的日志路径不再读取 `BoundSql`、绑定参数或原异常消息；只输出受限 Mapper ID、`SqlCommandType`、耗时及异常类名。`console`/`log` 两模式共用安全消息；`enabled` 仍由原配置条件控制，没有公共签名或依赖变化。`invocation.proceed()` 成功结果、普通异常原对象均保留，元数据提取或输出抛 `RuntimeException` 时不改变业务结果。测试使用真实 MyBatis `BoundSql`，在 SQL 字面量、注释、展开片段、绑定值、additional parameter 和异常消息放合成哨兵，覆盖两模式不泄漏与继续执行。

非阻断边界：日志设施抛 `Error` 不在旁路捕获范围内，符合不吞 JVM `Error` 的工程规则；测试未专门模拟输出设施故障，也未分别断言两种 sink 的选路。`MybatisPlusConfig.java:89` 的旧注释仍称“完整 SQL 日志拦截器”，需后续同步文案，不影响实际输出。此为源码审查，未运行 Maven、测试或服务。
