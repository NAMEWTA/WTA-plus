# T-39 C2 固定增量：安全与事务轴审查

## 固定输入与结论

- C1 `c2ab3d63225d2d1406aa9ecc9c072fecc83da297` → C2 `6e1d7f8e9f3d67495a361634779b7f7f050e68ab`，C2 tree `602161ee794da46728f76a140b6617b9b3e010f8`。C2 的直接父提交就是 C1。`git diff C1...C2` 仅两处 `backend/wta-admin/src/test/java/org/namewta/test/notify/*IntegrationTest.java`，37 行新增、4 行删除；`git diff --check` 退出 0。生产源码、规范、Ticket 与 C1 逐字不变，固定 C1 静态安全结论沿用 `/tmp/wta-t39/candidate-c1-security-review.md`。
- **静态增量审查通过，无新安全/事务阻断。** 两个旧夹具错误被精确修复，原验收断言保留且增加明确 UNKNOWN/WAITING_RECEIPT 负例。Lead 后续已在固定 C2 完成真实八类重验：driver/Maven exit 0、118 tests、0 failure/error/skip。C1 的八类真实结果 exit 1（Enterprise 1 failure、Atomic 2 failures）仍保留为历史失败；C2 证据单独列于下文。

## 两处差异

1. `EnterpriseQueuedNotificationIntegrationTest.java:190-235`：原测试把 `NotifyStatus.FAILED + empty deliveries` 错当成最终失败；C2 改成从真实 `NotifyRequest` 取唯一目标，返回 `NotifyTargetResult.unsentTerminal`，再验证 challenge 为 FAILED、Delivery FAILED、Outbox DONE、可重发新 challenge。它没有改 `DispatchNotificationService.providerOutcome` 对模糊外部结果的 UNKNOWN 规则。新增 `genericFailedWithoutSingleTargetRemainsUnknownAndCannotActivate` 保留原 `FAILED + empty` 反例，断言 challenge QUEUED、绑定不变、Delivery UNKNOWN、Outbox WAITING_RECEIPT、Provider 恰好调用一次。`EnterpriseTransferService.java:152-164` 对通知 UNKNOWN 返回 QUEUED、对真正 FAILED 拒绝激活，故两支断言与产品合同一致。旧测试的成功路径/外部事务边界断言未删。
2. `NotifyAtomicResultIntegrationTest.java:503-546`：在故障提交后合成第二个 PENDING Delivery/PROCESSING Outbox 时，额外把同 Intent 聚合置 `PROCESSING` 并断言该状态；原先已 DELIVERED 的 Intent 与新增 PENDING Delivery 矛盾，触发 T-39 正确终态 guard。`NotificationAggregatePolicy.java:20-27` 对“已有 DELIVERED + 新 PENDING”的真实聚合结果正是 PROCESSING。C2 保留两种故障 phase、同线程正常后续提交必须触发 AFTER_COMMIT push、本人关系数变 2 的原断言；未禁用或弱化 `NotifyDispatchResultService.java:88-91` 的终态保护。

## 真实证据、历史边界与剩余门禁

- 已只读核对 `/tmp/wta-t39/runs/0990e739cc0c555b/result.json`：`acceptance=true`，Maven 与 driver `exit_code=0`，source before/after 均 clean C2 `6e1d7f8e9f3d67495a361634779b7f7f050e68ab` / tree `602161ee794da46728f76a140b6617b9b3e010f8`；owned MySQL/Redis 容器、匿名卷、loopback 32828/32829 端口与 Maven 进程组均无遗留。六份真实 SQL baseline import 均 exit 0。该结果由 Lead 执行，我只审查其清洗后记录。
- 独立解析 `/tmp/wta-t39/c2-real-reports/` 八份 fresh Surefire XML 的 `tests/failures/errors/skipped` 属性，合计 **118/0/0/0**：Deadline 15、Enterprise 8、Wake 1、Redis absolute expiry 2、Atomic 66、ManualRetry 13、SMS 10、Redis idempotency 3。八份 XML 的 SHA-256 均与 `manifest.json` 匹配，manifest 绑定同一 C2 clean 源码。没有将 XML 内可能包含的环境信息复制进本报告。

- 旧外部 READY、attempt 0、无新 marker，即使尚未到期，也不能从这些字段反推“从未调用 Provider”。T-39 revision 167 **明确只对过期终结使用新 marker + 本次领取前来源证明**，并保留未到期 READY 的既有 T-37/T-38 重试合同；C2 没有声称重构或证明全部历史轨迹。发布前要先停旧 Worker，按 `T-39-legacy-deadline-disposition.md` 对旧无截止 OTP/队列只读分类并做受控接管与风险裁决；该处置尚非本地测试结果。
- 其他适用后端门禁仍由 Lead 串行执行与汇总；本报告不将八类通过外推为全部门禁或部署通过。此次仅做固定源码与清洗证据只读 review，未运行 Maven、Docker、服务或测试，未写仓库文件。
