# 统一通知规范

本文件是业务通知的唯一实现约定。通知需求应先读取本文件，再按模块加载 `wta-module-guide` 的 Notify 事实地图；不要重新创建消息盒子、站内信表或渠道 SDK 包装。

## 调用入口

业务模块只能依赖 `wta-api` 的 `org.namewta.notify.api.NotificationApplicationService`，通过 `NotificationCommand` 提交通知意图。构造命令时必须明确 `appId`、`sceneCode`、`bizType`、`recipientType`、`recipientIds`、`channels` 和稳定的 `idempotencyKey`；模板参数放入 `templateParams`，不要把手机号、邮箱或供应商密钥写入日志。

```java
notificationService.submit(new NotificationCommand(
    "profile", "person-rebind", "PERSON_REBIND",
    userId.toString(), "USER", List.of(userId.toString()),
    "person-rebind", Map.of("name", displayName),
    List.of(NotificationChannel.IN_APP), NotificationStrategy.ALL,
    NotificationMode.ASYNC, 0, null, null,
    "profile:person-rebind:" + userId + ":" + requestId, Map.of()));
```

`NotificationApplicationService` 还提供查询、失败重试和未完成通知取消。Controller、Workflow 页面和其他业务实现不得直接依赖通知 Mapper、ServiceImpl、Outbox 或渠道客户端。

## 目标与渠道

- `recipientType=ALL`：发布时解析当前正常且未删除用户，不把全量用户加载到前端。
- `recipientType=USER`：`recipientIds` 必须为用户 ID；服务端在保存和发布时校验数据权限、状态和删除标记。
- `recipientType=PHONE` / `EMAIL`：只用于明确的外部收件人场景，输入必须经过业务授权和格式校验。
- 渠道仅使用已实现的 `IN_APP`、`SMS`、`MAIL`；未知历史值只能按“其他”展示，不能生成投递任务。
- MAIL/SMS 正文权威在通知中心「通知配置」的场景绑定：邮件热配 `${name}` 文案，短信只绑定供应商模板码与参数映射。调用方不得传 `providerKey`，也不得把完整句子写入 `templateParams.content`。
- SMTP 发件账号和短信厂商凭据保存在 `notify_channel_account`；YAML 不再作为发件人、`sms.blends` 或全局 `restricted`/`minute-max`/`account-max` 的运行时来源。
- 业务通知默认异步。站内信、通知收件箱和消息盒子读取同一 `/notify/inbox` 数据源；SSE/WebSocket 只发送刷新事件，不承担持久化。

## 后端分层与状态

`controller -> usecase -> service -> dao -> mapper/XML` 是 `wta-notify` 的固定链路。UseCase 负责事务、目标解析、幂等和状态迁移；Service 负责规则；DAO 封装 MyBatis-Plus Wrapper、分页、锁和批量更新；Provider/Outbox Worker/Callback 只能通过端口或事件接入。

发布必须在同一业务事务内写入通知意图、接收者快照和 Outbox。Worker 使用租约 owner/token 更新，续租失败时禁止继续写入投递结果。供应商回调必须验证 HMAC 原文、`providerKey`、`eventId` 和时间窗，状态只能单向升级，重复事件不重复刷新聚合状态。

## 前端与权限

通知 Web Domain 提供类型化 API 和目标选择器；App 只组合运行时目录端口。保存草稿与发布分为两个操作，发布前展示标题、渠道和目标范围；所有异步请求需要 loading、错误恢复、重复点击保护和过期响应保护。权限指令只控制可见性，后端仍必须执行权限和数据范围校验。

通知配置页 `notify/config/index` 使用权限 `notify:config:list|query|add|edit|remove|test`。密钥只写不读；短信 TAB 不得提供自由正文框。

## 验证

修改通知域后至少执行：

```bash
./mvnw -pl wta-modules/wta-notify -am test
node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs backend/wta-modules/wta-notify --mode layered
pnpm --dir frontend typecheck
node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs
```

同时验证 OpenAPI、权限菜单、通知收件箱/消息盒子一致性，以及 ALL、USER、USER_TYPE、重复发布、回调重放和租约失效场景。
