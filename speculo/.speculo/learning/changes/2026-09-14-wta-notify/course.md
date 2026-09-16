# 课程设计：wta-notify 统一通知切片

## 目标与期望效果

学完后能口述公告、收件箱、配置、收件人、监控、供应商回调、应用提交、Outbox 领取这八条切片的真实函数链。不教“大概有个消息队列”这种空话。

## 学习者与表达/深度配置

| 字段 | 值 |
| --- | --- |
| 交互语言 | `zh-CN` |
| expression_level | `eli5` |
| coverage_depth | `deep` |
| Lesson 时长 | `35` 分钟 |

## 目标合同

工作树 Controller / UseCase（实现与接口算同一切片）：

| 切片 | Controller | UseCase |
| --- | --- | --- |
| 公告 | `NotifyNoticeController` | `NotifyNoticeUseCase` |
| 收件箱 | `NotifyInboxController` | `NotifyInboxUseCase` |
| 配置 | `NotifyConfigController` | `NotifyConfigUseCase` |
| 收件人 | `NotifyRecipientController` | `NotifyRecipientUseCase` |
| 监控 | `NotificationMonitorController` | `NotificationMonitorUseCase` |
| 回调 | `ProviderCallbackController` | `ProviderCallbackUseCase` |
| 应用提交 | `NotificationController` | `NotificationApplicationUseCase` |
| Outbox | （无 HTTP Controller） | `NotifyOutboxClaimUseCase` |

| ID | 可观察目标 | Lesson |
| --- | --- | --- |
| OBJ-01 | 口述 `/notify/notice` 列表/详情/保存/发布/撤回/删除：Controller → UseCase → `NotifyNoticeService` / `NotifyNoticePublisherService` → DAO | L-001 |
| OBJ-02 | 口述 `/notify/inbox` 列表与已读：`NotifyInboxUseCase` → `NotifyInboxService` | L-002 |
| OBJ-03 | 口述 `/notify/config` 账号与场景及测试发送：`NotifyConfigUseCase` → `NotifyConfigService` / `NotifyTestSendService` | L-003 |
| OBJ-04 | 口述 `/notify/recipients` 搜索：`NotifyRecipientUseCase` → `NotifyRecipientDirectoryPort` | L-004 |
| OBJ-05 | 口述 `/notify/monitor` snapshot/deliveries：`NotificationMonitorUseCase` | L-005 |
| OBJ-06 | 口述 `POST /notify/callback/{channel}`：`ProviderCallbackUseCase` → `ProviderCallbackPort` | L-006 |
| OBJ-07 | 口述 `/notify/notification` submit/query/retry/cancel：`NotificationApplicationUseCase` → `NotificationApplicationRuntimeService` | L-007 |
| OBJ-08 | 口述 Outbox 领取：`NotifyOutboxClaimUseCase` → `NotifyOutboxClaimService`，以及 worker/wake 如何接到这条链 | L-008 |

## 课程地图

L-001 公告 → L-002 收件箱 → L-003 配置 → L-004 收件人 → L-005 监控 → L-006 回调 → L-007 应用 API → L-008 Outbox。

## 成功证据与范围外

成功：能点名方法。范围外：短信网关厂商控制台、邮件 DKIM 运维、SSO、third HTTP。

## Revision 记录

| 时间 | 变化 |
| --- | --- |
| 2026-09-14T07:42:38.672Z | 初版八切片 |
