# T-36 C3 功能/工程轴差异审查：静态通过，真实结果待绑定

固定输入：base `5118051403de0648540646d98536d8c4f3f9f26d`，C2 `a2749a7dcc0bbc5c0643e07eba938446611c995a`，C3 `64d67d5fb150620b25ada107115ea2207736c039`，C3 tree `f35cb3d1797e45be931e5950058f38d5bd843ec1`。以 `git diff C2...C3` 和 C3 固定源码审查；检查时 `HEAD=C3`、工作树 clean、`git diff --check base...C3` 退出 0。未改 repo、未运行 Maven/Docker/服务。C2 功能/工程完整静态结论及限制见 `/tmp/wta-t36-c2/review-functional.md`；本报告只补审 C3 差异，不借用另一审查轴结论。

C2→C3 只有一处产品路径 `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java:531-532`，新增注释及 `duringProvider = () -> {};`。生产代码、SQL、端口和其他测试源码均逐字未变；另三处变更是本 change 的状态、dispatch Evidence 与 worklog，不属于本功能轴产品差异。该赋值位于首笔 BEFORE/AFTER commit 故障及持久事实断言**之后**、第二笔同线程“正常提交”创建和 dispatch **之前**。BEFORE 分支原先已在 `:523` 清注入器；本次将 AFTER 分支也在同一汇合位置清除，避免新正常 Delivery 被旧注入器再次打断。`assertThatThrownBy`、失败后同步集合为空、预算/消息/关系/Attempt 计数、再次送达、下一笔实时事件恰增一次及第二收件关系恰为 2 的所有断言仍保持原样 (`:508-543`)。没有减少选择器、case 或测试严格度，也没有改变首次提交故障注入覆盖。

因此 C2 固定候选那一处 `uncertainResultCommitUsesDurableFactsToPreventDuplicateInbox(AFTER)` 的夹具错误有明确修复，C1 两项产品阻断及 C2 的事务同步/取消修复仍按前两份报告的静态复核结论成立。C2 的 Unit178 与 SMS8 证据若通过，只能准确表述为**C2 SHA 上**运行，因 C3 对其运行输入（生产/对应单测源码）未变可作为回归参考，不得改写成 C3 新运行。

本报告生成时可见的最新真实隔离结果仍为 C2 `/tmp/wta-t36/runs/909ae637d59b2ebe/result.json`（Atomic66: 0 failure/1 error，Wake1 pass，exit 1）；未见 C3 的不可变 result 路径，故**不能判定 C3 整票验收通过**。Lead 须提供 C3 source-before/after clean、精确 Atomic/Wake XML 执行数/零 skip/exit 0、owned 资源清理及适用门禁结果，再由最终验收写 Evidence。其他 UseCase 提交故障造成的 dynamic-datasource 同步遗留仍是 `/tmp/wta-t36-c1/ds-event-cleanup-review.md` 标明的框架残余，不因本次测试调整消失。

结论：**C3 差异静态审查 pass；整票接收状态 pending 当前 C3 真实验收。**
