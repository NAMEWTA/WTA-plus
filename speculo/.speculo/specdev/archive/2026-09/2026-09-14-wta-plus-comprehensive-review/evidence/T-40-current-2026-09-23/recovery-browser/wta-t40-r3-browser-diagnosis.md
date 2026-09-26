# T40 R3 Chrome A 用例超时：只读诊断

输入：固定 `378f8481deae56bf7b6971ef1b1b2e66c47ca8fe`、安全结果 `/tmp/wta-t40/browser-runs/f3ff781212c46752/result.json`、当前同 SHA 的 `frontend/e2e/notice-retraction-real.e2e.ts` 及本地 Playwright 1.62.1/应用源码。未读取已删除的原始 reporter、HTTP 正文或凭据；未改仓库、运行测试或启动服务。

## 已观测

- 真实发布、Worker 送达、撤回、A/B 公告管理 GET 403，以及 seed 的 `22/1/22`、V1 离开第一页、旧链接前十均已通过。Chrome 两例均仅运行一次、零 skip：B 用例 passed，A 用例 `timedOut`。安全定位是 A 用例第 175 行的 `finally` 内 `page.unroute(...)`；不能仅凭此定位认定 `unroute` 自身是根因。源码/JAR 前后同值、资源清理错误为空。
- A 用例第 84–117 行的同页迟到响应是独立场景：旧 V1 的 `route.fetch()` 后暂停、切到本人 legacy、放行旧响应后仍显示 legacy。若该场景中途失败，定位会落在其第 116 行清理；本轮安全定位落在第二个会话场景第 175 行，但安全结果不含每一步完成标志，不能冒称前半每项断言均已实测。
- 第二个场景第 147–158 行同样 `route.fetch()` 真实后端字节后暂停；第 161 行确认 `oldFetched=1` 才继续。第 162–166 行真实 SPA 注销、B 登录；第 167 行之后以**仅 URL**注册 `waitForResponse`，第 169 行才释放旧 route，第 170 行等待没有自身终止条件的 `sessionFinished`，第 171 行再等待该 response。

## 源码确定的矛盾与尚未观测的细节

- `user.ts:84–95` 的 `logout()` 在同步增加 `sessionGeneration` 后立即调用 `adminHttp.cancelPending()`；`clearLocalSession()` 又取消一次。生产 Axios adapter 的 `cancelPending()` 会 abort 旧 request scope；已有 `axios-chain.test.ts` 证明已排队请求取消及已完成旧 adapter 响应不能再触发会话恢复。因而注销后**不应要求 A 的旧浏览器 XHR 一定再发 `Page.Response`**。`route.fetch()` 是 Playwright 独立取得的后端响应，不等于浏览器中已被取消的 XHR 收到该响应。当前第 167–171 行将两者混作一个必达事件，是测试合同问题的强证据；实际是否卡在 `route.fulfill`、`sessionFinished` 或 `waitForResponse`，安全结果尚不能区分。
- Playwright 本地 `coreBundle.js` 的 `Page.waitForResponse()` 只监听 `Page.Response`；`Page.unroute()` 默认路径不等待活跃 handler，仅更新拦截规则。因此第 175 行的 `timedOut` 定位更可能是超时后的清理现场，不能据此宣布 `unroute` 死锁。第 147 行 handler 还可能在取消后的 `route.fulfill` 等待或抛错；本轮未保留 handler 终态。
- `Navbar.vue:148–160` 注销把当前 `fullPath` 放进登录 `redirect`，本场景当前 path 带 V1 `messageId`。B 登录后可能对**相同 URL**发一个新的、由 B 身份持有的 GET；本人关系规则应拒绝它。第 167 行的 URL-only response 谓词不绑定第 147 行捕获的 A `route.request()`，即使它返回，也可能误把 B 的 403 当作 A 迟到响应；同时注册时机在 B 登录之后，也可能已错过 B 的响应。这是确定的匹配缺陷，是否实际发生未被安全结果记录。
- `InboxPage.vue:117,215–238,290–301` 对详情使用 session epoch 与 `owns()` 检查，失活会 reset；现有真实 SFC 用例覆盖旧 promise 在 epoch 切换后成功/失败都不能回填。当前 R3 不证明这些防线失效。B 独立 Chrome 用例已证明其外人 V1 与不存在 ID 的本人详情呈相同失败形状，但它不能代替同一 SPA 实例的 A→B 竞态。

## 必要的实质修正（尚未授权实施）

1. 保留第 84–117 行**同页** `route.fetch → route.fulfill → Page.Response → legacy 正文保持` 断言；这是实际迟到成功响应进入仍存活浏览器页后的隔离证明，不改变超时。
2. 会话段在发起 A V1 请求时捕获**该次 `route.request()` 对象**，并在注销前对该对象注册 `requestfailed`（以及仅用于识别异常响应的精确对象 `response` 监听），不能再以 URL 判断旧 A。保留 `route.fetch()` 完成的后端正控制。注销必须观察该精确 A 请求失败，严格核其 Chromium 取消原因；如果未取消、收到 A 页面响应或失败原因不是预期 abort，测试失败。不得用 `catch(() => undefined)`、新增宽松超时或任意失败当成功。
3. 在明确旧请求已取消后释放其拦截 latch，令 handler 有可观察终态并完成 route 清理；仅在已证实该精确请求取消时处理取消引起的 route 终态，其他 `fetch/fulfill/abort` 错误外抛。不要再要求注销后已取消请求触发 `waitForResponse`。应以 Playwright 的 `requestfailed` 和 handler 终态证明清理，而非无限等待裸 `sessionFinished`。
4. 同一 browser context 继续真实登录 B，打开其本人 `bControl.messageId`，确认 B 正文可见、A V1 正文/详情不可见；若登录 redirect 保留 V1，还要将任何 V1 请求按 **request 对象与 B 当前身份**区分，并保持 B 对外人 V1 的拒绝。这样保留同页旧成功响应与跨会话取消两种不同且必要的合同，不靠 B 独立用例替代。

若 Lead 后续授权一次受控修正/验收，安全报告可增加固定布尔或阶段枚举，例如 `a_backend_fetched`、`a_request_aborted`、`handler_settled`、`b_own_detail_visible`；不得保留 URL/ID/token/响应体/任意异常消息。恢复批三次失败需原样归档，当前不得直接重跑。
