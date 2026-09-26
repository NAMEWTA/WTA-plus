# T-40 R1 浏览器恢复候选只读审查

固定输入：`b3aef077..c860480914b5fc563dd119e760e37d5eb99064e3`，候选 tree `dae5c3199921e07d1e2e27d2ecf259c3833dc037`。本轮审查两份 Python、README 与支撑其预期的三处 Java 增量；`notice-retraction-real.e2e.ts` 和 Playwright config 对基线无差异，原两例及 reporter 限制没有改变。结论：**静态审查未见阻断；真实 UA、Python 离线、默认/full 与 owned 浏览器验收仍须以 Lead 的同源运行结果裁决。** 本审查未运行测试、构建或服务。

## 合同与证据

- 空 UA 原路径保留：`run-notice-retraction-real.py:693-709,731-741,1248-1263` 的 control/A/B 三次 `/auth/login` 未传 `User-Agent`；传 UA 只限新增 `login_chrome`，且值被固定常量与阶段双重校验。`LoginHelper.java:85-90`、`UserLoginSuccessListener.java:42,50-51`、`SysLoginInfoServiceImpl.java:53,71-73` 分别对解析结果为 null 做 `Unknown` 回退，三处同步/异步接缝均覆盖；没有用 UA 绕开空 UA 失败。`test_run_notice_retraction_real.py` 新增无 UA header 缺席、仅固定 Chrome header 存在、错误阶段/值拒绝的离线负例。
- 在线字段区分准确：`IAuthStrategy.java:62` 将 `client.getClientId()` 放入 `LoginHelper.CLIENT_KEY` extra，`UserLoginSuccessListener.java:55` 将该 extra 写入在线 DTO 的 `clientKey`；因此 `verify_online_login()` 在 `run-notice-retraction-real.py:800-819` 用 `ADMIN_CLIENT_ID` 比较此字段，同时按**本次实际 token**、本人用户名、设备类型及 browser/os 精确找一行。它调用无需管理列表权限的 `GET /monitor/online`，而非 `/monitor/online/list`；不假设同账号只存在一个 token。相反，异步审计 `SysLoginInfoServiceImpl.java:56-79` 将 `client.getClientKey()` 写入 `sys_login_info.client_key`；runner 的 `audit_client_identity()` 从 owned `sys_client` 精确读取此值及 deviceType，审计 SQL 用它比较，未混用两个身份值。
- 审计时序与正控制：`run-notice-retraction-real.py:764-797,822-833,1248-1271` 在每次 HTTP 登录前按用户名取最大 `info_id`，登录后以 `info_id>baseline` 有界轮询，要求新增总数与 `status='0'`、Client/设备/Browser/OS 匹配数均**恰为 1**；多行、单行不匹配和超时都失败。三次空 UA 的在线/审计均成功后，另取新基线做控制账号固定 Chrome UA 正控制，期望 `Chrome/Linux`，验证其新 token 的在线行与审计；随后将 `control_token` 改成**新 token**才调用 `real_notice_control()` 保存、发布、撤回。A/B 空 UA token 仍用于原管理接口 `code=403` 负例。新增离线测试覆盖查询 `info_id` 边界、状态、一次/零次/多次结果及登录→在线→审计顺序。
- 失败与资源门禁未减：新 SQL/HTTP 阶段进入固定白名单；HTTP/SQL 失败只生成受控阶段、数字代码和本脚本行号，在线响应与审计原值只留内存，四个 token 在取得后进入 redaction 列表。`safe_failure()` 不传播任意异常正文；原始 Playwright reporter 与日志清理、双标签完整容器 ID/匿名卷/进程组/端口/source+JAR 前后精确检查，以及两例 Chrome 正数零 skip 的门禁，本增量未改。README 与实现对空 UA 三登录、Chrome 正控制和异步审计的描述一致。

限制：静态代码不能证明实际 `UserAgentUtil.parse`、Redis 在线会话和异步数据库消费在 full JAR 中运行成功，也不能代替空 UA 三登录及 Chrome 正控制的真实 HTTP/SQL 记录。若 Lead 运行中任何一个阶段未达到上述断言，本候选不得按此静态结论判为通过。
