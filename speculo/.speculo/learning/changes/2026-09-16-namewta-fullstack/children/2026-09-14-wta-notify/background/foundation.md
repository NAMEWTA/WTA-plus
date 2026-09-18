# 总体背景：wta-notify

## 为什么这个主题重要

业务模块只应调用统一通知 public API，不应自己发邮件/短信。公告是运营草稿；应用通知是程序提交；Outbox 是投递领取。把三者混成“发个消息”会把幂等和回调弄丢。

## 宏观地图

```text
运营：NotifyNoticeController → NotifyNoticeUseCase → 草稿/发布
用户：NotifyInboxController → 站内信已读
配置：NotifyConfigController → 账号/场景（运行时以数据库为准）
程序：NotificationController → NotificationApplicationUseCase → runtime submit
      → Outbox → NotifyOutboxClaimUseCase / Worker → 渠道适配
回调：ProviderCallbackController → 回写投递状态
监控：NotificationMonitorController
```

## 核心概念与关系

| 中文 | English |
| --- | --- |
| 意图 | Intent |
| 发件箱 | Outbox |
| 投递 | Delivery |
| 场景绑定 | Scene binding |
| 配额 | Quota (`NotifyQuotaPort`) |

## 先决知识与缺口

需要 layered 五层。不要求已掌握 sms4j。

## 术语表

`NotificationApplicationService` 是 wta-api 上的统一提交合同，由 `NotificationApplicationUseCase` 实现。

## 来源与不确定性

以 `wta-notify` 源码与 `engineering-standards` 的 notification 规范对读；冲突以源码为准并记入 Lesson。
