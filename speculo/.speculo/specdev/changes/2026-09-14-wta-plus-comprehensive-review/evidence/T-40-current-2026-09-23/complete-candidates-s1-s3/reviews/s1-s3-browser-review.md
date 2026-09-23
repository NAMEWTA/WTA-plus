# T-40 S1 浏览器恢复差异只读审查

固定候选：`70bb5996b20ab39708ccc115cef8908403a67cf8`；相对 R3 `378f8481deae56bf7b6971ef1b1b2e66c47ca8fe` 只改 `frontend/e2e/notice-retraction-real.e2e.ts`，候选文件 SHA-256 `d29510be8442c8f6d7176f8102358b57ff61a907313669e4febca0df6fac3814`。**静态结论：未见阻断，真实 S1 Chrome/后验 seed/cleanup 仍待 Lead 验收。** 本审查未运行测试、构建或服务；该路径 `git diff --check` exit 0，原 runner/config 未改。

- 原同页迟到响应门禁未减：A 的第一个 `route.fetch()` 现额外要求 HTTP 200，保存确切 `route.request()`，待 legacy 正文显示后才释放；`waitForResponse` 改按同一 Request 身份，并有界要求 route 完成、浏览器收到响应、双帧渲染后仍只显示 legacy（新 spec `113-160`）。这比旧 URL 匹配更严格。
- 跨会话旧 A 请求：第二个 route 同样先实取 HTTP 200，才记录 `oldFetched` 并持有字节；在注销前注册 `response`/`requestfailed` 监听器，二者仅对保存的 **A Request 对象**计数/记录。B 登录后确认返回收件箱，并实际打开 B 的 `bControl` 正文；释放后有界等待 route handler 结束、精确旧 Request 报 `net::ERR_ABORTED`、旧 Request 的 Response 数为零，再验 B 正文仍在且 A 正文不存在（新 spec `178-249`）。因此 B 重定向产生的同 URL 新请求不会误当 A 响应；仅无 A 文本的空页面也不能通过。未知失败码、旧 A 真正 Response、route.fetch/fulfill 非预期错误均失败。
- 取消后的 `route.fulfill()` 仍要求 handler outcome=`fulfilled` 是有意 fail-closed：本仓固定 Playwright core 1.62.1 `coreBundle.js` 的 Chromium RouteImpl.fulfill 经 `catchDisallowedErrors()` 处理已失效的 CDP Request，只有非法 HTTP status/unsafe header 等明确错误仍重抛。若真实 S1 暴露别的合法取消异常，应按确切运行证据复核，而非预先放宽 handler 结果。
- `bounded()` 给 held route、响应、绘制与清理设置 15 秒上限，避免再用全局测试超时定位 finally；`finally` 始终释放 held promise、移除本用例事件监听器、解除 route 并关闭 context。`cleanupPreserving()` 的 AggregateError 仅在 primary 与 cleanup 同时失败时用于原始 Playwright JSON；原 runner `run-notice-retraction-real.py:1398-1409,1439-1454` 只留受控数字位置/计数，删除 raw JSON、stderr raw log 和 artifacts，最终 `result.json` 不含嵌套异常正文，清理失败继续阻止验收。

限制：静态代码只能证明身份匹配和等待条件，不证明 Chromium 在该注销路径实际给旧 Request 产生 `net::ERR_ABORTED`、route fulfill 的实际 outcome 或 S1 两例通过。若真实结果不满足，应保留失败与源/资源证明再定向修正，不删同页旧响应或 B 正控制。

## S3 固定增量复核（保留上方 S1 时点结论）

S1/S2 的 lint 失败历史继续保留。S3 固定 `ddc0b51d6c3278f9e55e9c0aa21bde614958e6cb`（tree `67eeb3947790d010933d43e566d40ec6491a950a`），相对 S1 **只改同一 e2e 文件中 `cleanupPreserving()` 一处**：先以命名 `combined` 构造 `AggregateError([primary,error],固定文本,{cause:error})`，随后抛出；文件 SHA-256 `c7f14634dd11a3018238c4080511533e123d7e2bb5d8989f9b0576e6dd25ce2f`。其余 exact Request、no-UA/登录、同页 response、B 正控制、route/ctx 清理与两 case 没有差异。私有 `/tmp/wta-t40/lint-cause-diagnostic/result.json` 的三例表明直接 `throw new AggregateError(...)` 被 `preserve-caught-error` 误报，而命名变量形式 exit 0；`/tmp/wta-t40/s2-aggregate-semantics.json` 的 Node 合成检查 exit 0，确认 `errors=[primary,cleanup]` 且 `cause=cleanup`。Lead 报告 S3 E2E strict typecheck、architecture、OpenAPI、lint 已通过；本审查未重跑这些门禁。静态功能结论可映射 S3，真实浏览器与后续门禁仍以 Lead 的固定 S3 验收为准。
