# T-40 恢复02B：无 User-Agent 浏览器驱动证明预审

固定只读基线 `6eb5783cee5d29dc6f7ffa9d61d92774143dcc73`；writer 正在修改的工作树未作为输入。本报告只审生产 HTTP/在线/登录审计合同，未执行服务、测试或数据库操作，不是 02B 差异终审。

## 三处空 UA 接缝

`PasswordAuthStrategy.java:72-83` 在 `LoginHelper.login()` 后取 token。`LoginHelper.java:73-91` 读请求 `User-Agent` 并立即访问解析结果的 Browser/OS；Sa-Token `UserActionListener.java:23-26` 同步发布登录成功事件，`UserLoginSuccessListener.java:39-66` 又解析同一 header、写 Redis 在线 DTO、发布登录日志事件。`SysLoginInfoServiceImpl.java:50-93` 的审计监听器是 `@Async`，也解析事件中的 UA 并落 `sys_login_info`。因此只修其中一处不够：无 UA 的 `/auth/login` 必须返回实际 token，在线缓存可读，异步审计最终插入；不能给 Python `http.client` 请求补 UA 来绕过原路径。现 runner `control_request()` 未设置 User-Agent，三次 control/A/B 登录恰为真实空 UA；Chrome 后续登录自带普通 UA，可兼证非空路径未退化。

## 在线 HTTP 的准确 shape 与权限

`SysUserOnlineController.java:33-75` 的 `GET /monitor/online/list` 需要 `monitor:online:list`，不宜用普通 A/B 做证明。`GET /monitor/online`（100–115）是“当前账号在线设备”，没有额外列表权限注解，仍需要当前登录态；返回 `R<PageResult<SysUserOnline>>`，即 HTTP 200、业务 `code=200`、`data.rows` 集合和 `data.total` 数字（`PageResult.java:16-69`）。每行含 `tokenId,userName,clientKey,deviceType,browser,os,loginTime`（`SysUserOnline.java:9-59`）。它从当前 loginId 的 token 列表筛有效 token，再读 `ONLINE_TOKEN_KEY+token`；所以建议用三个刚签发 token **分别** GET 本人接口，在内存断言各自确有 `tokenId == issued token` 的有效行且账号/Client 与该身份一致，只在公开结果保留 `3/3` 布尔/计数，绝不落 token、完整响应、URL、Cookie 或 body。

`application.yml:270-276` 为 `is-concurrent=true,is-share=false`：同一账号后续 Chrome 再登录会得到另一个 token，不应使早期 token 失效，也不能断言某账号的 `data.total == 1`。`AuthController.java:168-172` 的 logout 只退出当前会话；browser A/B 的后续登录不会自动证明早期会话已失效。验证应查“精确本次 token 在本人列表中”及 A/B 不能用自己的 token 看到他人的在线行；不要把总在线数等于一个硬编码值当 Gate。若业务决定清理本次合成 token，应仅针对本次 owned token，不踢全账号其他会话。

## `sys_login_info` 异步审计的可区分证明

`SysLoginService.java:133-145` 从原 HTTP request 取 UA、IP、clientid 后发布 `LoginInfoEvent`；`SysLoginInfoServiceImpl.java:50-93,131-135` 异步解析 UA，写 `user_name,client_key,device_type,ipaddr,browser,os,status,msg,login_time`。表的 `info_id` 为主键且 `status='0'` 表成功（`10-cde-base-ddl.sql:280-295`）。建议在**三次无 UA 登录之前、同一新建 owned DB** 精确采集有关用户的已有 `info_id` 集合/最大 ID 及 count，公开记录只保留基线数量/边界数字。三次登录后、启动 Chrome 前，使用有界轮询读取 A/B/control 用户的新行，按基线 ID 集合差值锁定恰好这三次；要求每个用户各 1、`status='0'`、预期 Admin Client key、browser/os 为代码约定的安全非空 fallback，且没有新增失败行。不要只靠 `login_time` 秒级时间或 `COUNT(*)`，也不要把异步尚未提交当成同步失败；达到超时仍应 Gate 失败并留阶段/行数/错误码而非原始行或 header。Chrome 登录发生后会再增行，因此此验证须在 Chrome 前完成；若在后验复查，需以先前捕获的三个精确 `info_id` 比对而非再次要求增量恰好 3。

无需更改 `sys_login_info` schema：它不保存 token，在线接口保存 tokenId 但证据只能留摘要。测试原有 no-UA 三登录及真实发布/撤回、2 个 Chrome case、source/JAR clean 与容器/卷/进程组清理门禁均不能弱化；扩 runner 如需新增 HTTP/SQL stage，必须入固定白名单，异常摘要只留有限阶段/数字/本脚本行号，不持久化 `msg`、User-Agent、响应正文或 token。
