---
lesson_id: L-003
objective_ids: [OBJ-03]
estimated_minutes: 35
time_budget:
  - segment: orientation-and-map
    minutes: 4
  - segment: deep-explanation
    minutes: 14
  - segment: visuals-and-worked-examples
    minutes: 8
  - segment: pause
    minutes: 4
  - segment: recap-and-transfer
    minutes: 5
expression_level: eli5
coverage_depth: deep
source_ids: [S-N-03, S-N-09, S-N-10]
---

# Lesson 003：账号、场景绑定和测试发送

## 学完你能做什么

能把 `/notify/config` 分成三组映射：**账号**走 `NotifyConfigUseCase` → `NotifyConfigService`；**场景**同样进 ConfigService；**测试发送**进另一个字段 `NotifyTestSendService`。能说出密钥只写不读、邮件热配文案、短信只绑模板码、测试走 `NotificationApplicationService.submit` 且 `NotificationMode.SYNC`。

## 先把宏观地图放在桌上

把渠道配置当成「邮局柜台的公章和信纸」。账号是哪台 SMTP / 哪家短信厂商的钥匙；场景绑定是「验证码这封信用哪把钥匙、信纸上印什么」。测试发送是用同一柜台交一封真正会排队的信，不是 Controller 里直接 `JavaMailSender.send`。

```text
/notify/config
  账号：
    GET  /account/list              pageAccounts
    GET  /account/{accountId}       getAccount
    POST /account                   addAccount
    POST /account/edit              updateAccount
    POST /account/changeStatus      changeStatus
    POST /account/remove            removeAccount
  场景：
    GET  /scene/list                listScenes
    POST /scene/save                saveBinding
  测试：
    POST /test/account              testAccount -> testSendService.sendAccount
    POST /test/template             testTemplate -> testSendService.sendTemplate
           |
           v
NotifyConfigController  --inject-->  NotifyConfigUseCase
                                       configService:   NotifyConfigService
                                       testSendService: NotifyTestSendService
```

**类比失效处：** 公章比喻不包含限额扣减和 Outbox Worker。测试 submit 之后仍要走领取（L-008）。`SYNC` 不会让 `NotificationApplicationRuntimeService.submit` 在同一线程里打供应商。

## 核心概念与机制

### 直觉讲解

`NotifyConfigController` 是配置页窗口。写操作带 `@Log(..., isSaveRequestData = false)`，避免日志里出现密码。权限前缀 `notify:config:*`：list/query/add/edit/remove/test。

UseCase 对账号与绑定的写方法都标 `@DSTransactional`，然后原样调用 ConfigService。测试两个方法也是事务，但干活的是 `NotifyTestSendService.sendAccount` / `sendTemplate`。

`NotifyConfigService` 还实现 `MailAccountResolver`：投递邮件时按 `configKey` 从库里拼 `MailAccount`。启动时 `@PostConstruct loadEnabledSmsAccounts` 把已启用短信账号 `smsBlendRegistry.upsert`；缺表只打日志，不阻止进程启动。

### 精确定义与 English term

| 中文 | English | 精确定义（本课） |
| --- | --- | --- |
| 渠道账号 | channel account | `NotifyChannelAccount`：MAIL 的 SMTP 或 SMS 的厂商密钥；`configKey` 渠道内唯一 |
| 场景绑定 | scene binding | `NotifySceneBinding`：某个 `sceneCode` + `channel` 使用哪个 `accountId`、邮件文案或短信模板映射 |
| 逻辑场景目录 | scene catalog | `NotifySceneCatalog.sceneCodes()` 代码播种：`auth-captcha`、`person-rebind`、`enterprise-transfer`、`workflow-task`、`notice-published` |
| 密钥只写 | write-only secret | Vo 用 `mailPassSet` / `accessKeySecretSet` 布尔，不回传明文；更新时空串表示保持原值 |
| 测试发送 | test send | `NotifyTestSendService` 构造 `NotificationCommand`，`appId=notify`，`bizType=NOTIFY_CONFIG_TEST`，`mode=SYNC` |

规范：YAML 不再作为发件人、`sms.blends` 或全局限额的运行时来源。Java：`resolve(providerKey)` 读 `dao.findAccount("MAIL", providerKey)`；限额字段在账号 `minuteMax` 与绑定 `templateMinuteMax`。

### 机制/因果链

**账号**

1. `pageAccounts` / `getAccount`：DAO 分页或按 ID；`toAccountVo` 填 `mailPassSet` 而非密码。
2. `addAccount`：渠道必须 `MAIL` 或 `SMS`；`(channel, configKey)` 不得重复；`minuteMax >= 1`；默认 `enabled=N`；若启用则 `validateEnabledCredentials`（MAIL 要 host/port/from/pass，SMS 要 supplier/accessKeyId/secret）→ insert → `registerSms`。
3. `updateAccount`：空密码/空 secret 拷贝旧值；改 channel+key 时检查冲突；再校验、update、重新 registerSms。
4. `changeStatus`：只接受 `Y`/`N`；启用前同样校验凭据。
5. `removeAccount`：`countBindings > 0` 拒绝；SMS 成功删除后 `smsBlendRegistry.remove(configKey)`。

**场景**

6. `listScenes(channel)`：不扫数据库当目录。对 Catalog 里每一个 sceneCode 查 `dao.findBinding(sceneCode, channel)`，拼 Vo（含变量名/是否必填/样例）。
7. `saveBinding`：未知 sceneCode 拒绝。绑定账号必须同渠道且 `enabled=Y`。`templateMinuteMax` 不能超过账号 `minuteMax`。MAIL：若已绑定或填了主题/正文，则 `NotifyTemplateRenderer.validate` 主题+正文，只允许 Catalog 变量。SMS：绑定账号时 `smsTemplateCode` 必填，必填变量必须出现在 `smsParamMapping`，禁止映射未声明变量。无现有行则 insert，有则 update。

**测试**

8. `testAccount`：账号存在且 `enabled=Y`，target 非空。`listBindingsByAccount`；未给 sceneCode 就取第一条绑定。然后 `submit(scene, channel, target)`。
9. `testTemplate`：`findBinding(scene, channel)` 必须已有 accountId，账号启用。
10. `submit` 内部：MAIL → `recipientType=EMAIL` + `NotificationChannel.MAIL`；否则 PHONE + SMS。参数来自 `NotifySceneCatalog.examples(sceneCode)`。`idempotencyKey` 含 `System.currentTimeMillis()`，所以每次测试都是新意图。`metadata.audit=TEST`。返回 `receipt.status().name()` 或 `"UNKNOWN"`。

### 图、表或文本图

**图题 / caption：** 配置页三组 HTTP 如何分叉到两个 Service。

```text
NotifyConfigUseCase
        |
        +-- 账号 CRUD / 场景 list+save ------> NotifyConfigService
        |                                         dao: NotifyConfigDao
        |                                         smsBlendRegistry: SmsBlendRegistryPort
        |                                         implements MailAccountResolver.resolve
        |
        +-- testAccount / testTemplate -------> NotifyTestSendService
                                                  dao.findAccount / findBinding
                                                  notifications.submit(NotificationCommand)
                                                        |
                                                        v
                                                  与生产相同的 Runtime submit（L-007）
```

**文字等价物：** 配置用例有两个下游字段。改钥匙和信纸走 ConfigService，并在启用短信时登记 SMS4J 端口。点「试发」不走 ConfigService 的 SMTP 方法，而是构造一条正式 `NotificationCommand` 交给统一通知接口。试发成功只表示意图已提交（返回状态名），不表示供应商已回执。

**图的边界：** 不画出 Redis 限额实现 `RedisNotifyQuotaAdapter` 的 key 设计。不画出厂商控制台。

### 正例、反例与边界

**正例 1：** 新增 MAIL 账号但 `enabled=N`，密码可后补。启用时若缺 host/port/from/pass，`changeStatus` 失败，不会把半成品标成可用。

**正例 2：** 保存 `notice-published` + MAIL：账号已启用，主题正文里用 `${title}` `${content}` `${path}`。Renderer 按 Catalog 允许列表检查。

**正例 3：** `POST /test/template` 对 `auth-captcha` + SMS，target 为手机号。Command 的 `recipientIds` 就是这个手机号，渠道只有 SMS。

**反例 1：** 前端把密钥回显后再原样 POST。详情接口根本不给明文；正确做法是留空表示不改。

**反例 2：** 删除仍被场景引用的账号。`账号仍被场景绑定，不能删除`。

**反例 3：** 在测试接口里传 `providerKey` 或完整短信句子。`NotifyTestSendService.submit` 不接收这些字段；短信正文权威在绑定的模板码与映射。

**边界：** `person-rebind` 的 Catalog 变量列表为空，测试参数 Map 也空。`listScenes` 不会列出 Catalog 之外的历史 scene。Config 渠道校验只有 MAIL/SMS，没有 IN_APP 账号表。

**与规范的对齐/冲突：** 「密钥只写不读」「短信无自由正文」与 Java 一致。规范未写测试使用 `SYNC`；Java 明确 `NotificationMode.SYNC`。但 L-007 会看到 Runtime `submit` 仍然只写 Outbox——`SYNC` 只影响回执上的 `queued` 标志和存盘 mode，不是「在 UseCase 线程里 send」。

## 变式与迁移

- **变式 A：账号级试发不传 sceneCode。** 使用该账号第一条绑定。没有绑定则「账号未绑定逻辑场景」。
- **变式 B：启动时短信表不存在。** `loadEnabledSmsAccounts` catch 后 warn，进程继续；发送路径会因无注册/无绑定而失败关闭。
- **迁移到公告：** 公告 Publisher 的 scene 是 `notice-published`。没绑定 MAIL 账号时，IN_APP 仍可投递，MAIL 计划会 `UNBOUND_CHANNEL`（投递课，不在本 Controller）。
- **迁移到限额：** 绑定上的 `restricted` / `recipientMinuteMax` / `recipientDayMax` 在保存时写入；真正扣减发生在 `NotifySendPlanner` + `NotifyQuotaPort`，本课 HTTP 只负责把数字存进去。

## 常见误区

1. **「测试发送是 ConfigService 的私有 SMTP。」** 字段是 `NotifyTestSendService`，终点是 `notifications.submit`。
2. **「场景列表来自绑定表全表扫描。」** 列表骨架是 `NotifySceneCatalog.sceneCodes()`。
3. **「YAML 里还能写发件人兜底。」** 运行时 `MailAccountResolver` 实现读的是账号表；规范与 Java 在此一致。
4. **「enabled=Y 可以先启用再补密钥。」** `validateEnabledCredentials` 在 add/update/changeStatus 三条写路径都会挡。
5. **「Controller 注入了两个 UseCase。」** 只有 `NotifyConfigUseCase configUseCase`。分叉在 UseCase 字段，不在 Controller。

## 非评分暂停

想一想：为什么 `editAccount` 在密码留空时要拷贝 `current.getMailPass()`，而详情 Vo 只给 `mailPassSet`？若拷贝漏了，下一次保存会怎样？

再想：测试幂等键带当前毫秒。这对「连点两次试发」意味着什么？和公告 `notice-published:{id}:{version}` 有什么不同？

## 总结、词汇表与下一步

- 账号与场景：`NotifyConfigUseCase` → `NotifyConfigService` → `NotifyConfigDao`。
- 测试：`testAccount`/`testTemplate` → `NotifyTestSendService` → `NotificationApplicationService.submit`。
- 渠道仅 MAIL/SMS；密钥不回显；场景来自代码目录。
- 词汇：channel account、scene binding、write-only secret、catalog、test send。

下一步：L-004 公告编辑器如何搜索用户，而不把 system 用户合同暴露给 HTTP。

## 来源与引用

| Source ID | 来源 | 支持的 claim/段落 | 定位 | 访问日期 |
| --- | --- | --- | --- | --- |
| S-N-03 | `NotifyConfigController.java`、`NotifyConfigUseCase.java` | 路径映射、事务、两个下游字段 | `controller/admin`、`usecase` | 2026-09-14 |
| S-N-09 | `NotifyConfigService.java`、`NotifyTestSendService.java`、`NotifySceneCatalog.java`、`NotifyConfigDao.java` | 密钥布尔、绑定校验、测试 Command | `service/`、`support/`、`dao/` | 2026-09-14 |
| S-N-10 | `notification.md` | YAML 不再作运行时发件来源；权限 `notify:config:*`；密钥只写 | 「目标与渠道」「前端与权限」 | 2026-09-14 |
