# T-41 red checkpoint 固定源码审查（只读）

固定输入：`8a90e40`（`test(notify): expose missing inbox history pagination over real HTTP`），仅新增 `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyInboxPagingIntegrationTest.java` 271 行；对照同 SHA 的 `NotifyInboxController`、`NotifyInboxService`、`NotifyNotificationDao`、`LoginHelper` 与 Sa-Token 1.45.0 本地源码。本报告是静态审查；Lead 的 one-class owned 运行当时尚在进行，未读到结果，本轮没有运行 Maven/Docker/服务或写仓库、target。

**静态结论：red 源码按预期指向分页业务缺口，真实 HTTP/JWT/LoginHelper 本人身份接缝成立；运行结果与最终 T-41 权限矩阵仍待验。** 不以本审查替代实际 fresh XML 或把现有负向覆盖扩称为全部生产鉴权。

## 身份与 HTTP 接缝

- 新类第 59–86 行以精确 opt-in `notify.inbox.paging.integration=true` 启动，并核对 `T41_MYSQL_PASSWORD=T36_MYSQL_PASSWORD`、owned loopback JDBC/Redis 属性；实际调用 `NotifyAtomicResultIntegrationTest.open()`，该 fixture 连接真实独占 MySQL/Redis 和 Mapper，不是内存 DAO。第 105–109、187–220 行注册真正的 `NotifyInboxController`、Spring MVC `DispatcherServlet`、Jetty 随机 `127.0.0.1` 端口，`HttpClient` 发送 Authorization Bearer token；Controller 第 20、25–26 行带 `@SaCheckLogin` 并从 `LoginHelper.getUserId()` 取查询 owner。Sa-Token 本地 `SaInterceptor` 源码默认执行 HandlerMethod 注解鉴权；测试第 183–184 行确实注册该拦截器。
- 第 229–269 行用随机 32 字节 secret 和 `StpLogicJwtForSimple` 签 A/B 两个用户的 token，私有 SaToken DAO，登录前在 `LoginUser` 填 userId/userType，`LoginHelper.login` 把 userId 写入 token extra；第 258 行对 `LoginHelper.getUserId()` 作显式断言。第 110–117 行还以 `getLoginIdByToken`、B 的仅本人两条及不可见 A-only 消息作正反控制；第 119–123 行 A 页不含 B-only。HTTP 成功/业务码断言使旧实现不能靠直调 Service、无鉴权、404 或假分页蒙混过关。
- **范围限制（非本 red 源码阻断）：** 夹具用默认 `new SaInterceptor()`，没有装生产 `SecurityConfig.addInterceptors` 的 `clientid` header/param 与 token Client 匹配、Client access rules；`LoginUser` 也未填 clientPk/clientKey。它证明真实 HTTP 注解登录和本用户 JWT extra 接缝，不证明完整生产 Client 策略或缺失 clientPk 的拒绝。最终 AC-041 的真实 HTTP/浏览器矩阵仍要加入未登录、跨 Client/身份、B-only 详情/读写拒绝及数据库零修改，而不能仅凭此 red 用例宣称权限全绿。

## 红灯是否在分页 data 形状

- 第 142–173 行向独占库插入 502 条消息、503 条关系：A 恰 501、B 两条（其中一条共享 A 的第 251 条），全部关系同时间，A 一条已读，预期 A `unreadTotal=500`。第 119–123 行先证明 A 首页有最大 ID 且不含 B-only；第 125–138 行请求 `pageNum=26&pageSize=20`，先要求 HTTP 200/业务码 200，再断言 `data` 是含 `rows,total,unreadTotal` 的对象、`total=501`、`rows` 恰 1 且 ID 为最小 A 消息。这不是仅测试类名或首 500 条。
- 固定旧 Controller 的 `list()` 无 page 参数，返回 `R<List<NotifyInboxMessageVo>>`；旧 Service 固定 `dao.messageRecipients(userId,500)`；DAO 固定关系 `create_time DESC,message_id DESC` 并最多 500 行。同时间数据中，旧 A 首页包含第 501 条最大 ID，但 `pageNum=26` 被忽略，仍返回 500 数组。若环境和鉴权正常，首个预期业务失败在新类第 129–130 行 `data instanceof Map`，错误信息明确指向第 26 页对象形状。Lead 仍须以实际 fresh XML 的失败方法/行号和 Maven exit 判定自然 red；若失败发生在数据库、HTTP、JWT 或启动，则此 checkpoint 不能称有效行为红灯。

## 清理与残余限制

- 第 89–101 行先只删本类固定 messageId 范围内的本人/他人关系，再删对应消息，随后无条件调用 fixture.close()；数据库是隔离 owned 库，fixture 自己清其 Intent/Outbox 等固定资源。`OwnedHttp.close()` 第 223–226 行停止 Jetty，关闭 Spring context/HttpClient；构造失败时第 194–213 行也尝试关闭。try-with-resources 在第 107 行按逆序先关闭 HTTP、再关闭 `OwnedSaSession`。
- `OwnedSaSession.close()` 第 262–269 行恢复此前的 RequestContextHolder、SaManager config/context/DAO 与 StpUtil logic。随机 JWT secret 不写日志或 fixture 文件，token 仅存在局部变量/HTTP Authorization header。异常若发生在会话构造期间某个全局 setter 之后、对象尚未交付 try-with-resources，理论上可能留全局状态；正常构造与测试主体失败时能够恢复。该小缺口不会把业务 red 误判为绿，但若后续复用此 JVM 内其他类或并行 Sa-Token 用例，宜让构造器失败也回滚全局，并禁止同 JVM 并行修改 SaManager 全局。当前 runner `forkCount=1,reuseForks=false` 且仅选本类，风险被限制。
- 第 116–117、123 行以字符串是否包含 ID 作早期隔离控制，可能受非结构化字段中同数字影响；核心第 129–138 行解析分页对象与行 ID、DB 精确计数，比字符串断言强。后续最终权限测试应解析 rows 和详情 JSON 并直接查 DB，不靠全文 `contains` 代替越权证明。Jetty 随机端口由测试自身 close，runner 的 owned 端口清理列表只包含 MySQL/Redis；Maven fork 进程组收尾可兜底，最终执行仍应核 no live group/port。

需等待 Lead 的固定 `8a90e40` one-class 实际 result、fresh XML 和清理证据；此处没有任何运行通过或预期失败已发生的声明。

## 事后实测附记（不改变上述静态审查时点）

Lead 后续提供 `/tmp/wta-t41/runs/afc15f3575e3b1f6/result.json` 和保留的 `xml/TEST-org.namewta.test.notify.NotifyInboxPagingIntegrationTest.xml`。只读复核：命令只选本类且启用 `notify.inbox.paging.integration=true`；Maven exit 1，fresh XML 的 `authenticatedOwnerCanPageToMessage501WithoutLosingTotal` 为 **1 test / 1 failure / 0 error / 0 skip**，`AssertionFailedError` 位于 `NotifyInboxPagingIntegrationTest.java:130`，文本为第 26 页应有 `rows,total,unreadTotal` 而旧 data 是数组。`result.json` 保持 `acceptance=false,exit_code=1`，未误判通过。source before/after 同 clean `8a90e404fcd899aa68482f3f52bb902521dc6aa0` / tree `392b77364be5798c7e111879391fa522c43f9ba8`；PGID 无活成员、双标签容器无残留、匿名卷已无、32844/32845 端口已关、`cleanup.errors=[]`。这是有效业务红灯与资源清理证据，不是 T-41 功能通过。
