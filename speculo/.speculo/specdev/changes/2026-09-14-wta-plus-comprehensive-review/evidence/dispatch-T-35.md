# Dispatch Packet T-35-20260923-01

operation=dispatch；task_kind=implementation；native gpt-6-sol/xhigh；owner=cors_audit；Lead=single-agent；current/main/direct-parent，无新worktree。固定base `8b758ea8074a63835659b9731e3a4c74c5c029c5`；先Map→绑定Skill/相关硬规则→Ticket/AC035及/tmp/wta-t35-audit.md。

产品写集：
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySendPlanner.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifyTemplateRenderer.java
- backend/wta-modules/wta-notify/src/test/
- backend/wta-admin/src/test/java/org/namewta/test/notify/
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java
- backend/wta-common/wta-common-sms/src/main/java/org/namewta/common/sms/notify/SmsNotifyChannelAdapter.java
- backend/wta-common/wta-common-sms/src/test/

真实runtime/dispatcher/SMSadapter跨层先红灯，稳定非敏感快照、提交前纯校验/发送前额度、确定本地失败DONE及真实I/O未知区分，审计策略实际传入；只替换供应商端口的最终真实MySQL/Redis状态验收由Lead执行。遵循Ticket新增边界，T36/T37不越界提前实施。

禁止SpecDev/Skill/其他生产路径/commit/推送/部署/服务器/私有temp读取或写入。允许精确受影响Maven测试与无写入式分层门禁，先告知精确选择器防资源冲突；测试日志/tmp/wta-t35。不得自行启动Docker/真实服务；先提供测试环境属性/DDL/启动清理所需清单，由Lead按单owner准备隔离资源。公共签名或其他生产路径扩展先报Lead，不先改。最终返回diff、真实红绿命令/cwd/exit/count/skip、所需剩余真实验收与写锁，不自行Done/commit。

## revision151预声明安全扩写集

- backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java
- backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/event/NotifyDeliveryEvent.java
- backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/NotifyAuditPolicy.java
- backend/wta-common/wta-common-json/src/main/java/org/namewta/common/json/utils/LogSanitizer.java
- backend/wta-common/wta-common-json/src/test/java/org/namewta/common/json/utils/LogSanitizerTest.java

原admin notify测试根保持；java-api-compatibility实施/验证绑定，见Ticket新增安全契约。不改Controller/Filter生产路径；若需补对应真实调用测试先登记路径。

## revision152 预声明管理投影写集

- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationMonitorService.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationMonitorUseCase.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/vo/NotificationDeliveryView.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java
- backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifyAuditSupport.java
- backend/wta-api/src/main/java/org/namewta/notify/api/NotificationReceipt.java

REDACT_SENSITIVE 通知的供应商消息标识仅保留内部持久化用于回执关联；query、重复提交 receipt 和 monitor 公开投影隐藏该值，FULL 原行为保持。监控查询按本次有界结果批量读取 Intent 审计策略，不引入逐行查询；空ID集合不扫描全表。新增 NotifyAuditSupport 可统一策略与公开投影判断，NotificationReceipt 仅补公开字段的安全语义说明，不改签名。测试覆盖真实供应商返回手机号/验证码作为ID、内部值保留与公开值隐藏、FULL、重复提交及回执关联。

## 候选1返工派遣

T35第1候选41882b54a2b644f7ae661b84205cfcc9d1b72886未通过：真实隔离5例均在夹具插入已有auth-captcha/SMS种子时失败；独立安全审查发现发送前Redis配额异常仍归UNKNOWN/WAITING_RECEIPT。产品writer cors_audit在原写集内修复夹具与SMS发送阶段分类，已知校验终态与可能发送后的UNKNOWN保持，发送前可恢复暂态使用现有有界重试，不变更IN_APP/MAIL。单测实际37类163通过/0skip，缺一个文件名与类名不同的选择器已修正，未宣称完整38类通过。运行bdea78c404fd83db的两个自有容器和进程组已退出，两端口关闭；clean后HEAD/tree与验收前相同。证据在/tmp/wta-t35，正式关闭时保留本次失败。当前仍5done/1cancelled/1in_progress/43ready。
