# T-40 R3 跨会话浏览器失败独立只读诊断

固定输入：`378f8481deae56bf7b6971ef1b1b2e66c47ca8fe`、`frontend/e2e/notice-retraction-real.e2e.ts` SHA-256 `41a0c019e18bf9c0071d048a62ce5c9f953f19d85c0d2bd1fc98907fd805d12b`、安全结果 `/tmp/wta-t40/browser-runs/f3ff781212c46752/result.json`。R3 已证 real V1 发布→Worker 送达→撤回、seed `22/1/22/1/0` 与离页/前十成立；Chrome 两例为 1 pass、1 timedOut、0 skip/retry，A 的 reporter 最后定位 `notice-retraction-real.e2e.ts:175`，B 通过，source/JAR 前后同值，所有 owned 清理成功。原始 Playwright reporter/日志已按安全规则删除；不能从最后定位推断根异常、B 是否在 A 用例内完成登录，或 A 旧请求是否已被浏览器取消。本报告未运行服务、浏览器、测试或修改仓库。

## 确定的时序冲突与假阳性风险

1. A 的第二个拦截在 spec `147-158` 先 `route.fetch()`，`oldFetched++` 后等待 `pending`；`161` 只证明真实后端响应已被 Playwright 的 route-fetch 取得，**尚未证明浏览器 XHR 收到该响应**。随后 `162-166` 在同一 page 注销并以 B 登录。产品 `user.ts:81-95` 在 logout 开始和本地清理两处调用 `adminHttp.cancelPending()`；`axios-browser/src/index.ts:211-245` 给请求绑定当前 `AbortController.signal`，`cancelPending()` abort 旧 scope。因此旧 A 浏览器请求在 B 登录前可能正常地触发 `requestfailed` 而永远没有 `Response` 事件。`InboxPage.vue:215-239,290-299` 另以会话 epoch/detailGeneration 阻止旧完成写回；其已存在的 `InboxPage.test.ts:252-269` 验证旧成功/失败无法写回新身份。
2. spec `167-171` 在 B 登录后才注册 `page.waitForResponse()`，随后 `unblock()` 并无界等 `sessionFinished`；如果旧 XHR 已取消，`route.fulfill()` 可能失败、旧浏览器 `Response` 可能永不出现。`page.waitForResponse()` 的默认超时与 `sessionFinished` 的无界等待可耗尽 `test.setTimeout(120_000)`。`finally` 的 `page.unroute()` 在第 175 行成为 reporter 最后位置，不证明它本身造成超时。固定本地 Playwright 1.62.1 `_unrouteInternal(...,'default')` 不等待进行中的 handler，只更新 interception patterns；不过该命令在总测试时限耗尽后也可能报错/遮蔽先前等待。
3. URL 匹配还有独立假阳性：`Navbar.vue:153-159` 在注销时把当前 V1 深链放入登录 redirect，`login.vue:252-253` 让 B 登录后导航回同一深链。B 可能发出**新请求** `GET /notify/inbox/<V1>` 并获本人权限拒绝；spec `167-168` 只看 method+URL，可能把 B 的响应误认成旧 A 拦截请求。仅靠旧 URL、HTTP 状态或内容不可区分两个身份；必须捕获 A 拦截时的 `route.request()` 对象，并以该 Request 对象匹配 `response`/`requestfailed`。

## 可证伪的最小恢复条件

- 保留第一段同一 A 会话中真正迟到的 V1 响应与切到 legacy 后内容不被覆盖（spec `84-117`）；保留 `InboxPage.test.ts:252-269` 的跨 epoch 旧成功/失败归属门禁，以及 B 负例的独立 Chrome case。跨会话真实浏览器段改为证明**旧 A 请求被 logout 取消**：在注销前记录该次 `route.request()` 身份并订阅它的 `requestfailed` 与 `response`，先证 `route.fetch()` 已完成；logout 后要求该精确 Request 出现取消型失败且没有浏览器 `response`，再以 B 真实登录打开 B 自己的消息详情并断言无 A 的 V1 正文。Playwright failure text 只在内存与固定取消码白名单比较，未知错误不得当取消通过，不保存原错误、URL、body、token。若实际同一旧 Request 反而产生 Response，应另走严格“B 已登录后旧响应到达且不泄露”分支；不能将 B 同 URL 新请求混入。
- 当前 `route.fulfill()` 在原 XHR 被取消后允许出现**已证取消所致**的拒绝；只在精确 Request 的取消已确认时消化该预期拒绝，其他 route 错误继续失败。避免把取消分支称作已实际递送迟到字节：它证明真实传输取消，迟到成功仍由同页浏览器用例和跨会话单测覆盖。B 自己的详情必须实际成功，不能只看“没有 A 文本”这一空断言。
- 在每个关键转移放小于总 120 秒时限的明确界限（例如旧 fetch/注销/新登录/取消事件/B 详情各 10–15 秒），并记录一个固定阶段枚举：`a_fetch_completed`、`logout_completed`、`old_request_failed`、`b_login_completed`、`released`、`b_detail_visible` 等；安全 reporter 只允许该枚举及原数字源位置，不收任意异常 message/stack/URL/请求体。所有事件监听器在 `finally` 移除，先 `unblock()`，再有界结束 route handler 与 `unroute()`，最后原 `context.close()`；主错误与清理错误分别以固定布尔/阶段记录，清理失败仍使测试失败，但不得让 `finally` 的次生超时抹掉主阶段。对 exact Request 出现 response、未知 requestfailed、未释放 route、B 详情不成功及 A 正文泄露都应保留失败。

上述是需由唯一产品 writer 在既有 e2e 写集内实施并离线/真实验证的**可证伪方案**，不是对 R3 已发生根因的最终断言。不得因 A 用例超时而降低两例/零 skip/零 retry、真实发布撤回、source/JAR 或 owned cleanup 门禁。
