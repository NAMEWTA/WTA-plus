# T-36 C3 安全／规格轴固定增量复核（只读）

**结论：C2→C3 增量静态 PASS；整票最终仍须以 Lead 的 C3 真实验收裁决。** 固定 C2 `a2749a7dcc0bbc5c0643e07eba938446611c995a` → C3 `64d67d5fb150620b25ada107115ea2207736c039`，C3 tree `f35cb3d1797e45be931e5950058f38d5bd843ec1`。三点 diff 只有 `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java:531-533` 一处产品测试增量（注释和 `duringProvider = () -> {};`）；其余三路径为 change 治理，**生产源码、SQL、Mapper、事件监听器和公共合同均未变**。本审查未改仓库或运行 Maven/Docker/服务。

C2 的 `uncertainResultCommitUsesDurableFactsToPreventDuplicateInbox` 在首笔故障时将 `duringProvider` 设为设置 `commitFault` 的回调（测试行 504-508）。首笔预期抛错、预算及持久事实判定、BEFORE/AFTER 两分支的消息／关系／Attempt 计数断言仍在 C3 代码行 508-530；C3 在**这些断言之后**清掉夹具故障回调，下一笔新 delivery 在 532-541 行继续要求实际 dispatch，并断言 AFTER_COMMIT 推送计数增加 1、本人关系增至 2。它只停止向“下一笔正常提交”误重复注入 ACK 丢失，不删除、不放宽首笔提交不确定的安全断言，也不跳过下一笔实时断言。C2 的真实 Atomic 结果为 **66 tests、0 failures、1 error、0 skipped**，唯一错误正是 AFTER 参数分支在下一笔 538 行再次触发 `owned lost commit acknowledgement`；证据 `/tmp/wta-t36-c2/real-atomic-wake-counts.json`。Lead 另报 C2 Unit178、Wake1、SMS8 通过且 clean；本人未独立执行这些门禁。C2 不是合格最终候选，C3 修复需由当前运行中的同选择器真实 Atomic/Wake 结果证明。

C2 未变静态范围复用 `/tmp/wta-t36-c2/review-security.md`：独立预算与固定 marker、Intent→Outbox→Delivery 锁序、活 lease/owner/token fence、消息关系结果短事务、提交后定向提示、双用户 JSON 列表语义比较、列长度预算前预检、外部 SMS/MAIL 保守边界。C3 没改这些生产路径；静态上 C1 三个原真实失败的修复路径保持，但是否全部在 C3 上关闭仍要看 Lead 的固定 commit/tree、准确 66 项及零 skip/零 fail/error、资源清理与 source-after 证据，不能以 C2 的 1 error 或本报告取代。

`/tmp/wta-t36/recovery-by-id-reviewed-draft.md` 明示尚未隔离演练／未执行生产 SQL；按单个批准 delivery ID、解析后有序 channels 列表与标题/正文/path/noticeType、码点上限、唯一 WAITING Outbox、无活 lease、CAS 两行和 ACK 未知只读判定设计。它不是存量数据修复证据；DELIVERED 缺关系、已有关系的 UNKNOWN 及 SMS/MAIL 均排除。最终是否将该操作稿作为 T-36 所需“只读清单＋指定 ID 操作稿”的证据，由 Lead 对最终附件版本与治理合同复核。
