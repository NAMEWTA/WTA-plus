# T40 三次真实浏览器候选后的登录阻断复盘（只读）

固定输入：C3 HEAD `d5b4f5f83e43afdc30ad30b3577a1531b030fc11`，`/tmp/wta-t40/browser-runs/630336ccabc8052b/result.json`。本次只读仓库与本地依赖，另在 `/tmp` 编译运行无服务、无凭据的 5.8.47 User-Agent 离线探针；未改仓库、未重跑服务或浏览器。

## 已证事实与结论边界

- C3 在 `login_control` 失败：真实后端探针 HTTP 200、后端仍存活；首次 WTA 控制登录 HTTP 200、`R.code=500`、`login_rejected`，A/B 尚未登录，真实公告发布及 Chrome 两案例均未开始。before/after HEAD、JAR 相同且 clean，owned 资源清理 `errors=[]`。因此三次候选**没有证明 T40 浏览器业务失败**；失败在控制认证前置。C3 安全结果不含服务端异常类型，单凭 `R.code=500` 不能直接断言唯一异常。
- T40 `frontend/e2e/run-notice-retraction-real.py:690-733` 的 `http.client.HTTPConnection` 直连 `/auth/login`，仅显式传 `clientid`、`Content-Type`；Python 3 的 `HTTPConnection.request/_send_request/putrequest` 源码没有默认 `User-Agent`。T41 `frontend/e2e/inbox-paged-real.e2e.ts:39-49` 用 Chromium 页面表单登录，浏览器天然发送非空 UA。两者使用相同 Admin client ID `e5cd7e4891bf95d1d19206ce24a7b32e`；T40 body `username,password,clientId,grantType=password` 与前端 `frontend/packages/domains/admin/src/index.ts:429-442` 一致；24 字符随机密码满足 `PasswordLoginBody` 5–30 字符约束，T41 已成功用同类 fresh-schema BCrypt 轮换。没有证据表明 Origin 或 clientId 是此次 R500 根因。
- 直接可执行的故障链：`AuthController.login` → `PasswordAuthStrategy.login` → `SysLoginService.buildLoginUser`（browser/os 初始为空）→ `LoginHelper.login` → `fillRequestContext`。`backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java:85-90` 对 `UserAgentUtil.parse(request.getHeader("User-Agent"))` 的返回值直接 `getBrowser/getOs`。本地 `hutool-http-5.8.47-sources.jar` 的 `UserAgentParser.parse` 对 blank/null 第一分支明确返回 `null`；离线同版 JAR 实际运行也复现 null 解引用 NPE。`GlobalExceptionHandler.handleRuntimeException` 返回 HTTP 200 的 `R.fail`，与 C3 HTTP 200/R500 相符。这里是**高置信度、可证伪的源码根因**，但仍需一次受控真实登录回归来证明 C3 的服务器异常确为该处。
- 第一处修复后仍有第二个**同步**阻断：`backend/wta-admin/src/main/java/org/namewta/web/listener/UserActionListener.java:24-26` 的 SaToken `doLogin` 通过 Spring `publishEvent`，`backend/wta-admin/src/main/java/org/namewta/web/listener/UserLoginSuccessListener.java:42-51` 同样对空 UA 解析结果直接取 browser/os。若只修 `LoginHelper`，发 token 时同步成功监听器仍会 NPE。第三处是**异步审计**：`backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysLoginInfoServiceImpl.java:52-70` 对 `LoginInfoEvent.userAgent` 也无空值保护；`SysLoginService.recordLoginInfo:133-143` 会原样从请求取该头。异步异常不一定造成当前 HTTP R500，但会丢失成功登录审计，不能以主请求通过作为完整修复。

离线证据：`/tmp/wta-t40-ua-probe/WtaT40UaProbe.java` SHA-256 `9206a22fe4100de7e2253fcdaee7739219e3c5231b5babe325e86fc99a07c0b0`；同版 `hutool-http` JAR SHA-256 `0f1ef06be5773b5e65f6ecbdf660506d08e9802c41a008380505195202c02ca1`，`hutool-core` SHA-256 `7cc076ad4ed9846dc129edcd2a5e4e01b61a9d715bf0d3b9a7fa707d4f637266`。命令：`javac -cp <上述两 JAR> -d /tmp/wta-t40-ua-probe /tmp/wta-t40-ua-probe/WtaT40UaProbe.java`，随后 `java -cp /tmp/wta-t40-ua-probe:<上述两 JAR> WtaT40UaProbe`，exit 0，输出仅 `absent_is_null=true`、`chrome_browser=Chrome`、`chrome_os=Linux`、`absent_dereference=NullPointerException`。

## 最小修复边界及需预登记路径

保持 T40 runner **不加 User-Agent**，让控制登录成为真实无 UA 回归；不改变认证、Token、Client、权限、密码或成功事件触发顺序。仅在以下三个现有生产边界将空/blank UA 解析为固定 `Unknown` browser/os，正常非空 UA 仍沿 Hutool 的实际 Browser/OS 结果：

1. `backend/wta-common/wta-common-satoken/src/main/java/org/namewta/common/satoken/utils/LoginHelper.java`：`fillRequestContext` 仅在 browser/os 需填充时使用 null-safe 名称；已有 `LoginUser` 字段值和 `deviceType/IP` 语义不变。
2. `backend/wta-admin/src/main/java/org/namewta/web/listener/UserLoginSuccessListener.java`：在线 DTO 保存 `Unknown`，继续写原 Token 在线信息、成功日志与最近登录；不跳过监听器。
3. `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysLoginInfoServiceImpl.java`：异步登录审计行 browser/os 保存 `Unknown`，保留原 status、username、client/IP 等字段。

建议新增/授权一个精准真实消费者测试 `backend/wta-admin/src/test/java/org/namewta/test/auth/HeadlessPasswordLoginIntegrationTest.java`（或现有 admin auth 集成测试根中同等方法），由 owned MySQL/Redis 及真实 HTTP `/auth/login` 执行无 UA 和固定非秘密 Chrome UA 两个登录；验证码按隔离测试配置关闭。无 UA 请求须 HTTP 200/R200、签发可用 token、在线缓存 browser/os=`Unknown`、`sys_login_info` 成功审计行 browser/os=`Unknown`；Chrome 对照须保留实际浏览器/OS 分类与审计。测试只在进程内比较随机口令/token，报告只留状态/计数/固定分类，不打印响应体、Token 或口令。若异步审计需要轮询，使用有界等待与精确本次 user/client/time 范围，不因日志插入暂迟而跳过断言。该一条真实链覆盖三处消费者；如需更快定位，可补不启动服务的 null/blank/normal 参数化单元方法，但不能用它替代真实 HTTP 断言。

## 下一次受控诊断与证伪条件

Lead 完成写集/Skill 登记后，先以无 UA 的真实 HTTP 测试作修复前行为红灯（如运行条件允许），再改三处并跑同一测试绿灯；所有错误原始记录保留。随后只做**一次** owned full-JAR/browser 候选：沿 T40 原 control_request 无 UA，先要求 WTA/A/B 三个真实 login 均 R200，并核在线信息/审计；再继续真实 save→publish→Worker→retract 和两 Chrome case。runner 只保存 allowlisted phase、HTTP/R 数字码、固定失败类型及资源清理；不保存登录请求/响应正文、UA 任意字符串、Bearer、密码或 raw log。若无 UA 仍 R500，则以精确新 failure phase 和服务端仅固定异常类型/源码位置诊断另一异常；若加固定 UA 的对照仍失败，推翻“仅空 UA”解释并检查 client/密码/数据库会话链。不得把添加 UA 或忽略 R500 当作通过，也不得把此次 C3 计作已验证浏览器业务。

## 单测与 runner 接缝复核（后续只读补充）

Lead 拟预登记 `backend/wta-admin/src/test/java/org/namewta/test/auth/LoginUserAgentUnitTest.java` 而非第二套完整服务夹具，此方案可行，但测试应**直接调用三处真实消费者**，不只测 Hutool 解析器：

- `LoginHelper.login(loginUser, model)` 为 public；绑定 `MockHttpServletRequest` 的 `RequestContextHolder`，以 Mockito static mock 仅拦外部 `StpUtil.login/getTokenSession`，保留真实 `fillRequestContext` 与 `UserAgentUtil.parse`，分别断言缺头、blank、固定 Chrome UA 时 `loginUser.browser/os` 是 `Unknown/Unknown` 或 `Chrome/Linux`。`SysLoginService.buildLoginUser` 未设置 browser/os，正是线上首次登录形状。
- 直接调用 `new UserLoginSuccessListener(mock(SysLoginService)).handleLoginSuccess(event)`，真实解析请求 UA，以 static mock 隔离 `RedisUtils.setCacheObject` 并捕获原 `UserOnlineDTO`，断言 `browser/os`、在线缓存仍执行、登录成功审计事件与最近登录更新仍触发。`RedisUtils.CLIENT` 是 static final，类初始化即从 `SpringUtils` 取 `RedissonClient`；此测试须在首次触发前安装类级 fake Spring context/mock Redisson（参考已有 `GenericApplicationContext` 装配），否则 `mockStatic(RedisUtils.class)` 也可能因初始化失败而只测到夹具错误。
- 直接调用 `SysLoginInfoServiceImpl.recordLoginInfo(LoginInfoEvent)`（不经 `@Async` 代理），mock `ISysClientService`/`SysLoginInfoMapper`，捕获真实 `SysLoginInfo` 插入对象，断言缺头/blank/Chrome 对照下 browser/os 与原 `status=0`、username、client 字段。`LoginInfoEvent` 有公开 setter，`SysLoginInfoMapper.insert` 是可拦截的 DAO 边界。

现有更窄真实取证入口是 `backend/wta-modules/wta-system/src/main/java/org/namewta/system/controller/monitor/SysUserOnlineController.java:92-113` 的 `GET /monitor/online`：本人已登录即可调用，无 `@SaCheckPermission`，但仍受全局 Client 校验；结果 `R<PageResult<SysUserOnline>>` 的 `data.rows` 每项有 `tokenId/browser/os/userName`。T40 runner 可新增固定白名单 stage，拿登录 token **只在内存**与 `tokenId` 比对后检查 `Unknown/Unknown`，报告仅固定布尔值/计数，不写 token、完整 row 或 HTTP body；A/B 也可同样验证。异步审计用 owned MySQL `sys_login_info`（DDL `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql:280-294`）按 `user_name`、`status='0'` 在本轮登录前后计数，并有界轮询新行 browser/os；无 UA 应 `Unknown/Unknown`，固定非秘密 Chrome UA 应 `Chrome/Linux`。查询与结果证据只保留新增行数、固定分类是否匹配，不保存用户名之外的自由文本、消息、IP、token 或原始 SQL 失败正文。此方式复用当前 owned full-JAR/六 SQL/Redis 环境，不增加独立 SpringBoot 容器。
