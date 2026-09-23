# T40 S3 Chrome A 超时只读诊断（2026-09-23）

## 已观察事实与证据范围

- 安全结果 `/tmp/wta-t40/browser-runs/434baf70aa25766e/result.json` 记录：S3 `ddc0b51d` 输入与 JAR 一致；真实发布、Worker 送达、撤回、私有 seed 与排序门禁均成功；两条 Chrome 中 A `timedOut`、B `passed`；owned 进程、容器、卷和端口清理无错误。
- A 的公开 `assertion_location` 为用例第 22 行的 `AggregateError` 构造处。该通用清理位置不能定位原始超时动作，也不能证明某一浏览器事件已发生。原始 reporter 已按安全规则删除；本审查没有读取或恢复原始网络正文、令牌或日志。
- 源码 `InboxPage.vue:215-225` 在等待 `runtime.service.inbox.detail(id)` 前同步置 `detailVisible=true`、`detailLoading=true`；模板 `:56-75` 的 `el-dialog` 显示加载状态及独立“关闭”按钮。现用例 `notice-retraction-real.e2e.ts:216-219` 在取得旧请求的真实后端字节 (`oldFetched===1`) 后未关闭该弹窗，直接点击弹窗之外的 `.avatar-wrapper`。模态遮罩可阻止该点击；这与等待至总用例超时相符，但 S3 安全摘要不足以把它认定为唯一根因。
- 产品 `openDetail` 在详情响应后才检查 `detailVisible` 并决定是否填充内容；关闭弹窗仅令 `detailVisible=false`，没有取消 `runtime.service.inbox.detail`。因此先关闭弹窗并保持 Playwright `pending` latch 不释放，仍能检验随后 logout 对精确 A Request 的取消。

## 最小修正建议（下一派单，当前不写仓库）

1. A 的 Page 建立后设置 `page.setDefaultTimeout(15_000)`，使头像、弹窗关闭、菜单等动作在清理前有界失败；不提高用例总超时，也不改变导航/业务断言。
2. `oldFetched===1` 且 `oldRequest` 已捕获后，先断言“通知详情”可见及“正在加载详情…”，点击此弹窗的 `{ name: '关闭', exact: true }` 按钮，断言弹窗隐藏。整个过程不调用 `unblock()`。
3. 关闭后断言 `oldResponses===0`、`oldFailure===undefined`、旧 route handler 尚未完成（现有 `sessionFinished` 可用显式同步 `sessionSettled` 标志）；若关闭意外取消请求，必须失败，不把它算作 logout 取消证据。然后按现有顺序真实 logout、B 登录并读取 B 本人详情、释放 A 已取得的后端响应，严格要求同一 A Request 的 `net::ERR_ABORTED`、零 Page.Response，B 内容不被旧 A 覆盖。
4. 同页迟到成功场景及 B 的外人/不存在同形场景保持原断言，不改生产代码、不增加宽泛 catch 或 sleep。

## 安全阶段诊断（不记录异常消息）

- Playwright 1.62.1 已安装的 JSON reporter 在 `frontend/node_modules/.pnpm/playwright@1.62.1/node_modules/playwright/lib/runner/index.js:4140-4165` 将 annotations 同时序列化到 `tests[0].annotations` 和 `tests[0].results[0].annotations`；runner 的 test-end 路径 `:218-225` 会接收 worker 的 annotations。可在 A case 用 `test.info().annotations` 保留**一个**固定类型 `t40-phase`，description 仅取短白名单，例如 `close_loading_dialog`、`logout`、`login_b`、`release_old_route`、`verify_old_cancel`。进入动作前更新，故阶段只表示 `last_started`，绝不声明动作已经通过。每次更新只替换该类型，不放入 URL、ID、内容、错误文本或时间。
- 现有 Python `playwright_diagnostic(data)` 仅识别精确测试文件、标题及 Chromium，并安全提取正整数源码位置。可优先从单次 `results[0].annotations`，仅在该字段缺失时从同 case 的 `annotations` 读取；只接受单个 `type==='t40-phase'` 且 description 属固定枚举的 annotation，并将其映射为 `last_started_phase`。缺失、重复、其他类型/值、非目标 case 一律记 `null`。保留现有原始 reporter 删除、附件禁用及 2 case/0 skip 门禁。
- 离线正反例应证明合法阶段可保存，包含 token/content canary 的 description、任意额外字段、重复 phase 和伪造测试身份均不能进入安全摘要。超时 worker 若未交付 annotations，`phase=null` 是诚实结果，不靠异常文本填补。

## 待实测

弹窗关闭后旧请求是否仍 pending、logout 的 `requestfailed` 是否稳定为精确 A Request 的 `net::ERR_ABORTED`、以及阶段 annotation 在超时结果中的实际可用性，都需要 Lead 下一固定候选真实 Chrome 验证。当前没有运行构建、测试或服务，也没有修改仓库。
