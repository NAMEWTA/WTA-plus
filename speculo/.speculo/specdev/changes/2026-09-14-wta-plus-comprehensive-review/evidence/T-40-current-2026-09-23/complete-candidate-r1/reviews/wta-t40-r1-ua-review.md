# T40 R1 UA 窄修只读复核

固定候选 `c860480914b5fc563dd119e760e37d5eb99064e3`，生产增量相对 `b3aef077`，测试增量相对 `b7a12bc1`。**静态结论：无阻断；目标绿灯和整票验收待 Lead 真实运行。** 本轮只用固定 `git diff/show` 读源码，未构建、测试、启动服务或修改仓库。Lead 已告知隔离红灯固定点 `828ad32f` 为 10 项、6 个预期 NPE、4 个正例；此事实不等于 R1 绿灯。

## 生产行为

- `backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java:85–93`：仅 browser/os 两个补全表达式在 parser 返回 null 时用 `Unknown`。原“已有值不覆盖”、IP/位置、deviceType、`StpUtil.login` 顺序与会话写入均未变。缺失或空白 UA 不再阻断令牌入口。
- `backend/wta-admin/src/main/java/org/namewta/web/listener/UserLoginSuccessListener.java:42–66`：只改在线 DTO 的 browser/os 两字段；原同步事件处理、IP/位置、token/Client/device/部门、Redis online 记录、`recordLoginInfo` 和最近登录更新均保留。缺 UA 不再在 Sa-Token 成功监听器中断链。
- `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysLoginInfoServiceImpl.java:53–93`：只改异步登录日志对象的 os/browser 两局部值；原 Client 查询、消息/status、IP/地址、mapper.insert 均保留。缺 UA 时仍应生成审计行。
- 本地固定 Hutool `5.8.47` 的 `UserAgentParser.parse` 对 null/blank 返回 null；非空 UA 生成对象，并在无法识别浏览器/OS 时提供其 `Unknown` 枚举。因此当前 `userAgent == null ? "Unknown" : ...` 仅覆盖已证实空值边界，不更改正常 UA 分类。字符串与 Hutool `UserAgentInfo.NameUnknown` 一致。三文件没有新增认证、Client、权限、事件或公开 DTO/API 语义。

## 测试夹具增量

- `LoginUserAgentUnitTest.java:163–168` 在 child 失败结果中追加最多八层 cause 的异常**类名、栈帧类名/方法名/行号**，不读异常消息、请求体、token 或凭据。父断言仍要求 exit 0 且结果精确等于 `OK`；新增诊断只出现在失败时，无法将异常判绿。栈帧数量未另设上限，但该隔离合成用例的固定异常链与 45 秒子进程边界使其为诊断限制而非当前安全阻断。若保存证据，只保留该安全结果，不拷贝原始 child 日志。
- `:198–203` 临时 context 改为注册真实生成的 `SysLoginInfoBoToSysLoginInfoMapperImpl`，并以 `new Converter(new SpringConverterFactory(context))` 装配，符合本地 mapstruct-plus Spring 自动配置的 `ConverterFactory` 选型；比原 `new Converter()` 更接近生产映射。该 context 仍仅在 child main 内创建；父 Surefire JVM 不初始化 `RedisUtils.CLIENT` 或 `MapstructUtils.CONVERTER`，前次 static-final 污染阻断保持关闭。Lead 提供的有效隔离红灯已覆盖当前生成类存在/编译的基本事实；R1 自身还待绿灯验证。
- 原十案及字段断言不变：缺头、空白、Chrome 三组真实消费者直接方法调用和预填 browser/os，在线 DTO 的 Client/device/部门、审计 status/Client/message 等均未删。`@Async` 代理、真实 Redis/MySQL 和完整 HTTP 登录仍须以 Lead 隔离服务矩阵证明，不能由此单元夹具代替。

## 验证边界

R1 应先在固定 clean 源码上看到目标 10 项零失败/零 skip，再核默认 dev reactor 同 JVM 无静态单例污染，以及合法 HTTP 客户端**不加 UA**的控制登录成功、在线态和最终 `sys_login_info` 行；空白头与 Chrome 正控制也应维持。真实运行的命令、退出码、fresh XML、source 前后与资源清理均由 Lead 记录。本报告是源码结论，不宣称这些门禁已通过。
