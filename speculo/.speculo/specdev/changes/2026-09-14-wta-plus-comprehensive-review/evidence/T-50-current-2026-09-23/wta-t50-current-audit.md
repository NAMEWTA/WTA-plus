# T-50 收缩未兑现通知模式并迁移现有调用方：当前只读审计

固定产品输入：`7a6f75ac9238399daf7936797d07da141f0f5a03`。票为 `ticket/50-reject-unimplemented-notify-modes.md`，`blocked_by: T-39`；T-40 是另一张公告撤回票。本次只读使用固定 commit 的 `git show`/`git grep`，未读变化中的工作树，未编辑仓库或 SpecDev，未构建、测试或启动服务。实施前须以 T-39 完成后的精确 HEAD 重新核对 deadline 路径。

## 当前合同与真实缺口

`NotificationCommand`（`backend/wta-api/src/main/java/org/namewta/notify/api/NotificationCommand.java:27-39`）保留 `strategy/mode/int priority`，null 策略与模式分别归一为 ALL/ASYNC，缺省 primitive 优先级为 0。`NotificationStrategy` 仍有 `ALL/ORDERED_FALLBACK/ESCALATION`；`NotificationMode` **当前只有 ASYNC**，既有 `NotificationModeOpenApiTest:19-24` 已断言该枚举，生成 TS 合同 `frontend/packages/api-contracts/generated/openapi.ts:10988-10998` 也呈现三策略、仅 ASYNC、任意 number 优先级。不能把历史“公开 SYNC”说成当前事实，更无需添加 SYNC 再拒绝。

真正的提交缺口在 `NotificationApplicationRuntimeService.java:51-58,299-331`：`validate` 未限制策略/模式/优先级，随后先查重复、再 `validateSubmission` 并持久化；`71-73` 原样写 strategy/mode/priority。因此当前合法构造的 ORDERED_FALLBACK、ESCALATION 或非零优先级可以进 Intent/Outbox，重复键甚至可返回旧回执。应在 `findDuplicate` 之前明确拒绝任何非 ALL、非 ASYNC 或非 0 的命令，负向断言同时核对 Intent、Delivery、Outbox 都未增加。普通模式仍保留 constructor 的 null→ALL/ASYNC 与缺省 0。

当前 `NotifyOutboxMapper.xml:4-11` 按 `outbox_id` claim，没有 priority 排序；DDL `release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql:1399-1403` 虽 `priority int not null default 0`，运行时显式写优先级，SQL 默认未兑现业务优先级。T-50 没有理由改 DDL、增加队列或迁移历史数值。`DispatchNotificationService.java:325-339` 对旧 ORDERED_FALLBACK/ESCALATION 仍有 WAIT 分支；`NotifyDispatchResultService.java:177-191` 的 WAIT 使 Outbox READY、+30 秒，故特定旧记录可无限轮询。`NotificationController.java:25-30` 是权限 `notify:notification:submit` 的 POST 公共 HTTP 入口，应作真实负向合同，不绕过权限测试。

## 固定 SHA 全部仓内生产构造点

`git grep 'new NotificationCommand'` 限定 `backend/**/src/main/java/**/*.java` 找到 **12 次，10 文件**。全部已经 ALL/ASYNC；非零优先级清单如下，均需与服务校验同一变更改成 0，不能仅改入口使业务停发。

| 文件（均在 backend） | 行 | 当前 priority | 保留事项 |
|---|---:|---:|---|
| `wta-admin/src/main/java/org/namewta/web/controller/AuthController.java` | 94-99 | 20 | 登录通知参数与权限 |
| `wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java` | 69-73、107-111 | 80、80 | SMS/MAIL OTP；T-39 将补期限，T-50 复核不得抹掉 |
| `wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java` | 78-81 | 20 | 邮件 demo 模板 |
| `wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/SmsController.java` | 91-95 | 20 | 短信 demo 模板 |
| `wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/WebSocketController.java` | 41-44 | 10 | 站内 demo 目标 |
| `wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java` | 61-65 | 50 | `notice-published:<noticeId>:<version>` 幂等键 |
| `wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyTestSendService.java` | 83-88 | 20 | 测试发送的权限/模板 |
| `wta-modules/wta-profile/wta-profile-enterprise/src/main/java/org/namewta/profile/enterprise/service/EnterpriseTransferService.java` | 115-119 | 80 | **现有非空 expiresAt** 与稳定幂等键 |
| `wta-modules/wta-profile/wta-profile-person/src/main/java/org/namewta/profile/person/service/PersonRebindNotificationService.java` | 125-129、150-154 | 60、60 | 站内与 SMS 安全提醒及其幂等键 |
| `wta-modules/wta-workflow/src/main/java/org/namewta/workflow/service/impl/FlwCommonServiceImpl.java` | 171-174 | 40 | 流程业务幂等/模板 |

现有 `NotifySmsDispatchIntegrationTest:571`、`NotifyWakeIntegrationTest:168`、`NotifyOutboxWakePublisherTest:360` 用 0，无法替生产调用改值提供反例。仅 static grep 也不能证明真实 HTTP、验证码和历史 WAIT 收敛。

## 最小实施与写集

1. 在既有 `NotificationApplicationRuntimeService.validate` 增加严格策略/模式/优先级拒绝，并更新 `NotificationCommand`/`NotificationApplicationService` Javadoc。仅更改受支持语义，保持公开历史 enum 的二进制/JSON 值；OpenAPI 可经现有生成流程更新描述，已发布 strategy enum 是否保持历史值需按票的兼容裁决，但不能暗示这些值仍可提交。
2. 逐一改上表 12 个构造点 `priority=0`，字段旁边的期限、幂等、接收者、模板与权限逻辑原样保留。Ticket 的预登记生产写集覆盖全部 10 文件与 API/runtime；未发现额外生产调用点。T-39 完成后重新枚举构造点，防止新增调用遗漏。
3. 对历史记录先只读分类，再单独裁决受控处置。旧 WAIT 不得单纯继续每 30 秒重排，也不得将 `ACCEPTED/UNKNOWN` 视为可重发。若要在 worker 自动终结未开始的旧任务，使用原 Intent→Outbox→Delivery 锁序、当前 owner/token/lease fence、锁后 DB 时钟及有诊断码的结果事务；保留已发生的外部事实。现有 `NotifyDispatchResultPort` 仅 `WAIT/SKIP/CLOSE`（`port/NotifyDispatchResultPort.java:37-51`），SKIP 的 CANCELLED 不记原因、CLOSE 只关 outbox；若设计需要新的 typed disposition/API，**port 目录不在 T-50 预登记写集，须先扩登记**。也可只做 inventory 与需另批准的逐 ID 处置稿；这不是历史样本已终结的证据，AC 仍待验收。T-39 后可能已有 expiry 分支，不预设其最终签名。
4. 文档/合同边界：`engineering-standards/references/notification.md`、`frontend/packages/api-contracts/` 与 `frontend/tooling/openapi/` 已在票内；不需 SQL DDL 写集。若新增 DAO/Mapper 状态查询或 result port 改动，先扩对应精确写集，不能借 `runtime/` 范围越界。文档要明确 strategy 历史 enum 与实际支持能力的差别。

## 红灯与验收选择

- **提交负例**：在已授权 HTTP POST 与直接公共 service 各测 ORDERED_FALLBACK、ESCALATION、priority `1/-1`，含既存相同幂等键；断言明确失败、Intent/Delivery/Outbox 数量不变，HTTP 权限拒绝仍有效。ASYNC 是现有唯一 enum；HTTP 未知模式字符串应验证 binding 拒绝且无写入，不造一个 SYNC Java 枚举。
- **生产调用回归**：按 10 文件/12 点覆盖登录、SMS/MAIL 验证码、Notice、test-send、3 demo、Workflow、Person 两支、Enterprise；正向提交持久化 priority=0，仍保持 template/target/idempotency/expiresAt。优先在现有对应测试加字段断言，至少每类真实入口有一条可观察验证；只跑现有 `NotificationCommand(priority=0)` 测试不足。
- **历史 WAIT**：用六 SQL 隔离 MySQL/Redis fixture 造未开始的旧 ORDERED_FALLBACK/ESCALATION 及已有 `ACCEPTED`、`UNKNOWN` 混合记录；断言前者有受控终结/人工处置路径、后两者无盲重发、原外部状态不倒退，重入/过期 lease 无写入。`NotifyAtomicResultIntegrationTest` 的锁/结果事务 fixture 可复用；`NotifyWakeIntegrationTest` 可测轮询不再永久 READY。T-38 驱动可复用隔离资源做法，不把其 13 项作为 T-50 通过。
- **只读存量清单**：在获准环境按 `strategy <> 'ALL' OR mode <> 'ASYNC' OR priority <> 0` 与 Outbox/Delivery 状态分类，按 ID/状态/数量/最近尝试时间统计；不导出正文、模板变量、收件地址、供应商 ID。当前没有生产查询证据，不能推断存量为零。区别仍在途、DONE/DEAD_LETTER、UNKNOWN/ACCEPTED 与纯 WAIT，逐类保留处理决策和审阅记录。
- **门禁**：当前 ticket 规划 `cd backend && ./mvnw -pl wta-modules/wta-notify,wta-admin -am test` 和 `pnpm --dir frontend --filter @namewta/tooling-openapi openapi:check`，再跑受影响 profile/workflow/demo 消费者与真实隔离 E2E，记录精确类/项/skip/资源清理/源码 SHA。以上本轮均**未执行**。

结论：T-50 需要新产品实现；当前代码不符合 AC-050。最小施工是入口前置拒绝 + 12 生产调用归零 + 历史未支持任务的有 fence 处置 + 合同/真实验证。T-39 为硬前置，本审计是 `7a6f75ac` 的实施输入，不是验收或归档证据。
