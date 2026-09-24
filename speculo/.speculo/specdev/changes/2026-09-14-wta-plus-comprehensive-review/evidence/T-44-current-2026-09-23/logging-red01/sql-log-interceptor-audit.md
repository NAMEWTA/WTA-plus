# T44 SQL 日志安全只读审查

固定源码：`193579e2b9a0339e798ddd747740bf27c2942a4f`；本审查未修改仓库，未运行 Maven 或服务。

## 确定问题

- `SqlLogInterceptor.java:105-107,177-180` 把 `BoundSql.getSql()` 与参数值拼成完整 SQL；`190-214` 进一步读取参数对象和 additionalParameter。`121-135,298-335` 在 `log` 与 `console` 两模式都输出这些数据。
- 仅去除绑定值也不安全：`BoundSql.getSql()` 可含直接写入的字面量、注释和 `${}` 展开片段。本仓库 `TestDemoMapper.xml:8` 就有 `${ew.customSqlSegment}`。不应写入新的通用 SQL 文本清洗器来推测哪些字节安全。
- `SqlLogInterceptor.java:149-153` 输出异常 message，数据库错误可能含原 SQL 或凭据；改成只输出异常类名。`81-93` 中记录日志一旦再抛错，还可能遮盖 `invocation.proceed()` 的原异常。
- `SqlLogProperties.java:14` 声称“完整 SQL 输出”，整改后需要同步改为命令元数据摘要说明。

## 最小安全合同

`SqlCommandType`、经字符白名单和长度限制的 Mapper ID、非负耗时、结果状态和异常类名即可。可加固定 `SQL_TEXT_OMITTED` 标记。日志路径不要读取 BoundSql SQL、参数对象、异常 message/stack，亦不要把 Throwable 传给 logger。两输出模式只换 sink，内容采用同一安全合同。记录日志异常不能改变查询/更新结果或覆盖原业务异常；成功返回同一对象，失败抛同一 Throwable 实例。

## 有意义的测试

通过真实 `MappedStatement`/`BoundSql` 夹具，在 SQL literal、注释、`${}` 已展开片段、绑定参数、additionalParameter、异常 message 中放互异合成哨兵。分别捕获 `console` 和 `SQL_FULL` logger 输出，验证所有哨兵和 SQL 原文缺席，安全元数据存在。对成功/失败路径分别验证 `Invocation.proceed()` 一次、结果对象或异常对象 identity 不变。另测异常 message 含换行/控制字符、Mapper ID 非法或过长、日志 sink 故障不遮盖原异常。测试不需要真实 OSS 凭据或完整服务。
