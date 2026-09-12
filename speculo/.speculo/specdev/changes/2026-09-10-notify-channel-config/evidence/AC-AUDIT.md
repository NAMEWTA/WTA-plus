# AC-001–AC-018 对抗核验

对照 shipped 测试、SQL、页面源码与 scratch 日志。全新库用 `50/60`；已有库用 `evidence/upgrade-existing-mysql.sql`（不得重放基座全文）。

| ID | 结果 | 证据 |
|---|---|---|
| AC-001 | pass | 菜单 `notify/config/index` + `notify:config:*`；web-domain registration/permissions；`index.test.ts` |
| AC-002 | pass | 配置 API 新增账号；`NotifyConfigServiceTest.accountVoOmitsSecretFields` |
| AC-003 | pass | `disabledAccountFailsClosedWithoutProviderSend`；`changeStatus(N)` 对 SMS 走 `smsBlendRegistry.remove`，`disablingSmsAccountUnregistersBlend` |
| AC-004 | pass | `boundSmsUsesVendorTemplateOnBoundAccountOnly`；`Sms4jBlendRegistryTest` 对 alibaba/tencent 真实 `upsert`，blend 按 configKey 可取，无 CCE |
| AC-005 | pass | unbound MAIL/SMS Dispatch 测试，无 `NotifyClient.send` |
| AC-006 | pass | `mailBindingRejectsRenamedRequiredTokenAndAcceptsMovedToken` |
| AC-007 | pass | SMS `NotifyTemplateContent` Dispatch 测试 |
| AC-008 | pass | `secondSendWithinAccountMinuteCapFailsClosed` |
| AC-009 | pass | `recipientMinuteCapIsIsolatedByScene` |
| AC-010 | pass | `templateMinuteMaxCannotExceedAccountCap` |
| AC-011 | pass | `NotifyCallerMigrationContractUnitTest` 覆盖 captcha/换绑/企业转移/workflow/notice；SMS 只传变量；IN_APP SAFE_TEXT 不在本期 MAIL/SMS 范围 |
| AC-012 | pass | Captcha `templateCode=auth-captcha` + `code` |
| AC-013 | pass | `noticePublishedMailRendersWrapperNotCallerSnapshot`；workflow/notice 包装变量 |
| AC-014 | pass | VO 无 secret；空白编辑保持原值 |
| AC-015 | pass | YAML 无 blends/from/minute-max；无绑定失败关闭 |
| AC-016 | pass | `NotifyTestSendServiceTest` 停用/未绑定失败；提交走 `NotificationApplicationService` |
| AC-017 | pass | `NotifyConfigControllerContractTest` GET/POST + `@Log(isSaveRequestData=false)` |
| AC-018 | pass | notification.md / notify index / common mail-sms 事实；`validate-skill-facts.mjs` |
