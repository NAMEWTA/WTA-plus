# T-36 功能/工程轴只读审查：request-changes

固定输入：base `5118051403de0648540646d98536d8c4f3f9f26d`，head `54cf715c2829ff95cf994401ff52f02668c1e171`，tree `fb42e3016b15f5f3b9e741d90c4f9803925d651d`；`git diff base...head`，仅审 `/tmp/wta-t36-c1/path-audit.json` 列出的 21 个产品路径及必要调用者、DDL、适用规则。提交列表仅一项 `54cf715 fix(notify): atomically persist in-app deliveries with bounded attempts`。审查前后 `git status --porcelain=v1` 空，`git diff --check base...head` 退出 0。未改 repo，未运行 Maven、Docker 或服务。本报告为功能/工程轴，不读取另一审查轴结论。

权威合同：当前 change `spec.md` AC-036、`ticket/36-atomic-in-app-delivery.md` revision154/155；仓根及 `backend/{AGENTS.md,wta-modules/AGENTS.md,wta-modules/wta-notify/AGENTS.md}`；`engineering-standards` 的 Notify 分层、PERSIST-001/002、TEST-001/003/004、SEC-004；`java-api-compatibility` 对 `wta-api` 端口的调用面核对。源码的端口签名未删除，新增内部结果端口已扫描调用者；DDL 只改字段注释，仍在唯一六 SQL 基座中。审查技能：`speculo/workflows/specdev/common/skills/code-review/SKILL.md` 及 source-discovery、fowler-smells、risk-review、reviewer-contracts。

## 阻断发现

1. **P1：共享消息的多渠道 JSON 快照会被误判不一致。** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/InAppNotificationService.java:47,54-59` 将新快照 `channels` 通过 Jackson 序列化后写入 `notify_message.channels_json`（`release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql:1542` 是 `JSON` 列）；第二个收件人到来时，直接把 JDBC 读回的 JSON 文本与重新序列化文本做 `Objects.equals`。MySQL 8.4 会规范化 JSON 并在显示/读取文本的逗号后插入空格，参见 [MySQL 8.4 JSON 数据类型手册的 Normalization 段](https://dev.mysql.com/doc/refman/8.4/en/json.html)。本仓 `JsonUtils.toJsonString` 使用默认紧凑的 `JsonMapper.writeValueAsString`，没有 pretty 配置。对 `channels=["IN_APP","SMS"]`，首位用户存入的 JSON 文本读回格式与紧凑再序列化不同；同 intent 后续用户在 `:58-59` 抛错，独立预留预算仍已消耗，最终可能 DEAD_LETTER、缺少其收件关系。`NotifyNoticePublisherService.java:59-64` 实际支持并提交多渠道公告。当前真实双用户测试 `NotifyAtomicResultIntegrationTest.java:300-328` 的 fixture `template_params_json='{}'`，只生成单渠道数组，不能覆盖此反例。修复条件：以解析后有序渠道列表/JSON 值作语义比较，仍拒绝真正不同的快照；真实 MySQL 用 IN_APP+SMS 两渠道、两用户同 intent 并发，断言一消息/两关系/两 DELIVERED、无重复事件及错误预算。不要通过取消快照一致性检查绕过。

2. **P2：可持久的意图标题在站内消息列越界时被当作临时 SQL 故障。** `10-cde-base-ddl.sql:1406` 的 `notify_intent.title_snapshot` 为 `varchar(500)`，同文件 `:1545` 的 `notify_message.title` 为 `varchar(255)`；`InAppNotificationService.java:48,53` 直接复制标题。`NotificationApplicationRuntimeService.java:71` 可把命令 `templateParams.title` 写入意图，当前 `DispatchNotificationService.java:189-205` 的确定参数预检没有站内列长度校验。因此 256–500 字标题可成功提交意图和 Outbox，却在每次 `completeInApp` 插入消息时遇到确定性的列约束失败；`:207-209` 已先提交一次预算，重领后反复耗尽，最后显示 `IN_APP_RETRY_EXHAUSTED`，而 T-36 合同要求“确定参数失败终结、本地暂时失败有限重试”。`noticeType` 的通用通知输入也未校验消息列 `varchar(10)`；公告 BO 自身更严格，故标题是充分的通用 API 反例。修复条件：在预算事务之前对站内可持久字段做明确边界判定并给本地终态，或在提交入口统一收敛长度且证明存量兼容；保持 `content` 全文和现有 1000 码点摘要合同。用真实 MySQL 覆盖 256 字标题及 noticeType 越界，确认不进入 WAITING_RECEIPT、不多次预留预算，且没有消息/结果部分提交。

## 已核对且暂无新增 finding 的路径

- `NotifyDispatchResultUseCase.java:25-34` 在 Spring 可代理 public 边界以 `@DSTransactional` 分别执行预算和 IN_APP 结果；没有 private/self-invocation 伪事务。`NotifyDispatchResultService.java:26-42,61-105` 统一 Intent→Outbox→Delivery 锁序，锁后用数据库时钟和 owner/token 检查；IN_APP `completeInApp` 内同事务执行消息、关系、Delivery、Attempt、Outbox、聚合，SQL 零行显式抛错。`complete.java:122-127` 拒绝通用结果端口绕过站内事实，确定本地参数失败例外不得在预算预留后调用。
- `NotifyOutboxMapper.xml:14-35,47-55` 在新 claim 只为 IN_APP 清内部标记，预算更新仍检查数据库租约、上限及同 lease 预留标记，finish 再以数据库时间 fence。`NotifyOutboxClaimService.java:27-42` 在已锁候选后普通批读 Delivery 渠道，不在 Outbox 锁内增加对 Delivery 的锁定子查询；生产 Delivery 渠道由 `NotificationApplicationRuntimeService.java:103-115` 插入后不再修改。外部 MAIL/SMS 分支没有被新标记清理，原 I/O 仍在结果事务外。
- `InAppNotificationService.java:39-72` 对两次 DAO insert 都要求恰好 1 行，关系受 `(message_id,user_id)` 唯一键保护；仅新增关系登记事件。`InAppCommittedPushListener.java:19-27` 使用 `@DsTxEventListener(AFTER_COMMIT)`，事件只携带 ID；`InAppCommittedPushUseCase` 经业务 Service 重读关系/消息再推给指定用户，实时异常不写回结果。`NotifyAtomicResultIntegrationTest.java:148-175` 装配真实动态事务代理、事务事件工厂和两连接接缝；新增预算、六 SQL 阶段、提交前/后断连、重复任务和两用户并发用例，静态结构与 AC-036 匹配。
- `wta-api` 的 `InAppNotificationPort` 仅改既有方法的事务语义 Javadoc，签名未变；仓内实现仍为 Notify 的 `InAppNotificationService`，调用者 `DispatchNotificationService` 已迁移至短事务端口。Callback、monitor、SMS/MAIL 结果仍使用通用 `complete` 并依据非 IN_APP 渠道进入原分支。`NotifyCallbackProcessProbe` 的 owned 数据库凭据改为子进程环境变量，argv 无密码。

## 当前真实门禁与限制

Lead 的固定源码隔离运行记录 `/tmp/wta-t36/runs/0e830cfa64189d7f/result.json` 显示 source before/after 为该 head、clean，六 SQL 导入成功，Atomic 61 例中 3 failures/0 errors/0 skipped，Wake 1 例通过，Maven exit 1，owned 容器/端口/进程组清理成功。两类失败涉及提交后推送计数和取消期间 outbox 状态；由 writer 继续定位，本轴没有把症状推断为已确定的产品根因，也不把该运行报告成通过。SMS 外部渠道及其余质量门禁不在本轴执行；须以 Lead 后续固定新候选的当前证据复验。即使上述两个源码 finding 先修复，当前候选也因真实门禁 3 failures 不能接收。

结论：**request-changes**。对下一固定候选复核两项可达反例、提交后事件/取消测试修复和完整真实验收；此结论仅绑定 `54cf715c2829ff95cf994401ff52f02668c1e171`。
