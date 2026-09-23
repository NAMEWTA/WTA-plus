# T-41 B 固定候选实现轴审查

审查输入：base `d7d534cb03aa000d603a53c092c4bf1d48bb5cc3`，B `05d0f3443dc3364917a8149ce9290f7dc6785670`，tree `f1672c953958ec94d3c20546d0b1a4e336e78825`；前端增量 parent `98ba20ffc17a63ce6bd9325799f5185e298436a9`。以 `git show`/三点 diff 固定源码审查，未运行构建、测试、Docker、浏览器或服务，未修改仓库。审查依据为现行 T-41 ticket/AC-041、工程与全栈 Skill、T-41 安全设计输入及此前浏览器预审。结论：**产品实现静态审查未发现新的确定性合同缺口；B 不能验收通过**，因为本轮前端 `pnpm test` exit 1，且 B 自身的严格 clean full-JAR/真实浏览器验收尚未完成。

## 可定位的实现事实

- `NotifyInboxController.java:25-57` 的 GET 分页和本人详情均取 `LoginHelper.getUserId()`，写入口保留登录和 `seen/read` 权限，并禁止 `@Log` 保存请求/响应正文。`NotifyInboxService.java:25-42` 默认 1/20、上限 100，详情无本人关系与不存在返回相同错误。`NotifyNotificationDao.java:207-253` 对 rows、total、unreadTotal 共用本人有效消息 JOIN；页内固定 `r.create_time DESC,r.message_id DESC`，先用 long 偏移及 total 短路极大页码，列表投影不含正文，详情单 JOIN 才取正文。`markMessage/markAllMessages` 在 `:275-297` 用限定 user_id 与 COALESCE 保留首次时间；全部已读没有页限制。三次独立 SELECT 是同谓词的最终一致读取，不宣称并发同一快照。
- 真实 HTTP/MySQL 类 `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyInboxPagingIntegrationTest.java:106-288` 有四例，覆盖 A501/B2、末页、正文仅详情、未登录及 B-only、写权限、全读/单读幂等和首次时间、孤儿关系及极大/非法分页。其夹具是本地 SaInterceptor/真实 DAO，不等于生产 Client 策略全覆盖。Source A `9d748aa...` 的隔离结果 `/tmp/wta-t41/runs/ae58665d5b552a2b/result.json` 记 4/0 fail/0 error/0 skip、owned cleanup 空；B 相较 A 的生产 backend 和该类未变，仅 HTTPS 兼容测试夹具响应形状迁移。该结果是**等价输入的历史运行**，不是 B 的独立运行。
- `frontend/packages/domains/notify/src/transport.ts:19-44,82-99` 使用生成的 `NotifyInboxPageVo`/`NotifyInboxMessageVo`，GET 携带 pageNum/pageSize，验证 rows 与安全非负整数计数，ID 字符串化以保留 64 位值，详情独立 GET，写仍为 POST。B 的 api-contracts 四个生成/来源文件与已核 live source `42f07eb...` 字节未变；先前 `/tmp/wta-t41-live-contract-review.md` 已核 437 paths/447 schemas、仅授权旧列表响应退役与新增详情、正式 provenance/TS 同源。不可把该旧 live 捕获写成 B 重新构建捕获。
- `InboxPage.vue:5-75,141-268` 使用 20 条页码、全局 unreadTotal、明确“包括历史消息”，详情独立请求，分页时关闭旧详情；epoch/generation 分别阻断旧身份和同身份旧分页/详情的 success、error、finally。`runtime.ts` 与 Admin `adminManifestRegistry.ts:268-308` 新增只读会话快照和通知刷新端口，代次先于 token 清除时暂停旧身份。`InboxPage.test.ts` 七例包含第26页、旧请求/错误/卸载、页切换与只读权限。Admin `push.ts:101-149` 顶部仅取 1/10 并用服务端 unreadTotal 作 badge；`notice/index.vue:140-203` 的详情/已读/全读按身份版本、token 和会话代次隔离，旧请求不能回填下一身份。旧受控 E2E 的 `/notify/inbox` mock 已从数组迁成 `{rows,total,unreadTotal}`，T-34 真实浏览器仍保留消息与零 push 断言，只改响应解包。
- 新 `inbox-paged-real.e2e.ts:51-136` 两个独立 Context 真登录，断言顶部10条/481 badge、A 页26单条、B 两条隔离、详情 HTTP200、A 全读后0/B仍2与零 push。其 seed 由运行器 `:370-421` 造 A501/B2、共享与 B-only；顶端20条预先已读，最旧只已见。运行器 `:460-493` 的 SQL 后检核 A 501/0、B 2/2，并逐项比较旧时间。browser config 关闭 trace/video/screenshot；runner 固定 clean HEAD/full JAR/package proof、随机 owned MySQL/Redis/MinIO、app-only账号、双标签全ID/捕获匿名卷/进程组/端口、0600私有配置，所有清理失败使 acceptance=false。失败诊断仅从可信 reporter 提取本测试文件的数字行列和固定状态，原始 reporter/log/artifact 最终删除；若真实 reporter 不提供可信位置则诊断为 null，不降低门禁。

## 浏览器私有预审与 B 对比

此前 `/tmp/wta-t41-browser-product-prereview.md` 的四个功能文件与 B SHA-256 完全相同：runner `bd1798bba7a3ea78640e054e11f04905faf56ca38b8594196e18fc379c4c1f88`、离线测试 `96295d752946205f604c1a7223f549650368f44d11d9fd9e388fe607dc2537c5`、spec `bc2905b9a1e0ddb5db949d4eb3b2f94cdc4ecf512f768541a12d294412636603`、config `860f0254ef131fa9c8a42eaf83850fe2352ac323de9d1031f7b8bab6a8186545`。说明文档仅由私有草稿路径改为产品路径，B SHA `2c598914fda32003cf7f4eb237bca283fd781b615e2af9ae59815d0140178c8c`。预审静态安全结论可继承，但它未实际启动浏览器，也不构成 B 验收。

## 当前阻断与后续验收边界

本轮 B `/tmp/wta-t41/b-frontend-{architecture,lint,typecheck}.json` 均 exit 0；`b-frontend-test.json` exit 1。`b-frontend-test.log:386-423` 显示 Admin 的五个新增 Notice 测试在缺失 SSR Context 的人工 renderer 下 setup 抛错，既有 registry 测试因新增直接导入 user store 引到真实 router，出现 `window is not defined`，0 项实际执行；Admin 另有 68 项通过。此为测试装配阻断，**不能解释为五项业务断言失败，也不能把前端全量测试记为通过**。Lead 已限定修复两个测试夹具；后续固定 C 需按真实新提交增量复审，重跑前端测试并保持原断言。B 的真实浏览器 clean full-JAR/owned SQL/Playwright、其他串行门禁及归档状态均不能由本静态审查代称通过。
