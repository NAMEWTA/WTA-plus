# WTA-plus 代码审查报告

**审查主题：现有功能正确性、notify 全链路、OSS/MinIO 依赖与权限、启动和配置简化**

| 项目 | 说明 |
|---|---|
| 仓库 | `NAMEWTA/WTA-plus` |
| 审查基线 | `main@64b4ea70b9be34ff8d3c63027a922cea884a84de` |
| 基线提交 | `feat: add OSS publish restore flows and local CORS wildcard`，2026-09-22 |
| 对照范围 | 基线提交与父提交 `2b4fd1f4b0e8f8c3520788f35a08476d5ecb0257` 的变更，并沿调用链检查基线实现 |
| 审查日期 | 2026-09-22 |
| 实施状态 | **审查报告；未修改仓库，未执行数据修复** |
| 验证方式 | 固定提交的源码、配置、调用方、前端、SQL Mapper 和既有测试源码交叉核查 |
| 未执行 | 仓库完整构建、项目测试运行、真实 MySQL/Redis/MinIO 联调、浏览器 E2E、供应商短信/邮件请求 |
| SSO 边界 | 不审计当前 SSO 实现、不把 pending change 当成已实现；仅整理作者要求的后续架构合同 |

> 本报告的“确认”指可以由所列源码与明确触发条件推出，不代表已经在作者部署环境动态复现。所有代码链接固定到上述提交。没有用“类很多”“代码长”代替缺陷证据，也没有把可选建议混进确定 BUG。未逐文件审计工作流、实名业务、第三方业务等全仓所有功能，因此不能据此宣称全仓已无其他缺陷。

## 阅读导航

[核心结论](#summary) · [notify 业务链路](#notify-flow) · [notify 问题与方案](#notify-findings) · [OSS 问题与方案](#oss-findings) · [安全配置](#security) · [启动与配置简化](#simplification) · [消息盒子只读排查](#diagnosis) · [实施顺序与测试](#delivery-plan) · [SSO 边界](#sso-boundary) · [覆盖范围](#coverage) · [源码索引](#sources)

<a id="summary"></a>

## 1. 核心结论

**项目不是缺少基础能力，而是若干已有能力在边界处没有接通，以及诊断/编排机制承担了超过必要范围的职责。建议修补合同并收缩依赖，不建议重写架构。**

最直接的发现是：**admin-web 的开发、生产环境文件都默认设置 `VITE_APP_MESSAGE_ENABLED=false`；`initMessageBox()` 又把它当作收件箱总开关，直接清空消息并跳过 REST 查询。** 在没有环境覆盖的构建中，后端即使已经把公告投递给当前用户，顶部消息盒子也不会显示。这不是对现场的猜测，而是配置与代码构成的确定路径。详见 **N-01**。[S01] [S02] [S03]

另外几项优先处理的问题是：短信请求的内容快照与下层校验相冲突；本地站内信异常被送进不存在回执来源的等待状态；失败结果的 Redis 幂等缓存使 Outbox 重试不真正调用供应商；OSS 上传/下载被诊断对象和诊断快照硬性阻断；公开仓库中的本地配置包含未脱敏凭据。详见 **N-02、N-03、N-04、O-01、S-01**。

### 1.1 问题总览

以下按问题根因归组，不把同一个原因的多个症状拆成多个“发现”来增加数量。

| 编号 | 优先级 | 性质 | 结论与适用条件 |
|---|---|---|---|
| [N-01](#n-01) | P1 | 确定缺陷，默认触发 | 开发/生产默认关闭实时消息，连带关闭消息盒子的持久化收件箱查询 |
| [N-02](#n-02) | P1 | 确定合同冲突 | SMS 空 `contentSnapshot` 在供应商调用前被 common-notify 拒绝 |
| [N-03](#n-03) | P1 | 确定异常路径；并发条件明确 | IN_APP 本地失败进入 `WAITING_RECEIPT`；共享消息“先查后插”可触发该路径 |
| [N-04](#n-04) | P1 | 确定重试冲突 | 下层缓存失败结果，使用相同 delivery ID 的上层重试在窗口内重放旧失败 |
| [N-05](#n-05) | P2 | 确定接口缺陷 | 重试忽略指定 delivery、无任务也改为 QUEUED、路径 ID 可被请求体覆盖 |
| [N-06](#n-06) | P1（时效消息） | 确定执行合同缺口 | 截止时间未在领取/投递中执行；验证码调用也未设置该时间 |
| [N-07](#n-07) | P2 | 确定交互不一致 | 公告“撤回”不停止待发任务；跳转链接指向不消费 noticeId 的管理列表 |
| [N-08](#n-08) | P2 | 确定读取边界问题 | 收件箱只取最近 500 条、无后续分页；消息盒子存在重复正文和空/失败状态混淆 |
| [N-09](#n-09) | P2 | 确定调用方合同冲突 | 现有演示邮件被必填 path 拦截，附件参数也未映射到物理发送请求 |
| [N-10](#n-10) | P2 | 代码可推导的规模风险 | 全量公告逐条落库、每次结果重读全部 delivery；不是已测得的性能事故 |
| [O-01](#o-01) | P1 | 确定依赖设计问题 | OSS 诊断成为上传、下载及整体健康的硬前置条件 |
| [O-02](#o-02) | P1（相关存储配置） | 确定诊断逻辑缺陷 | 桶策略/ACL 不可读被当作否定事实，错误拒绝公开桶并高估私有桶安全验证 |
| [O-03](#o-03) | P1（并发管理操作） | 确定竞争窗口 | 清理来源与恢复私有未统一互斥，可删除刚恢复为当前来源的对象 |
| [O-04](#o-04) | P2 | 确定前端竞争窗口 | OSS 列表的 generation 检查在状态写入之后，旧响应可覆盖新查询 |
| [S-01](#s-01) | P1，先处置 | 确定凭据披露；有效性未验证 | 公开版本控制中存在未脱敏数据库/Redis 凭据 |
| [S-02](#s-02) | P1 | 确定不安全默认值 | 通配 CORS + credentials 进入公共配置，未限定为本地环境 |

P1 表示应优先修复，不表示上述问题全部已在生产发生，更不等于已经确认遭到攻击。P2 仍是应处理的问题，但不应因此引入大型新基础设施。这张表包含确定缺陷与明确标注的条件性风险，不代表 16 起已经复现的生产故障；尤其 N-10 应先测量再决定优化投入。**D-01、D-02 是后文的复杂度改进建议，不计入确定 BUG。**

### 1.2 已经做对、应当保留的部分

| 已核对的实现 | 审查判断 |
|---|---|
| 草稿保存与发布分开；已发布内容禁止直接编辑；发布创建快照 | 正确，不应为“少几个步骤”改成保存即发送。[S10] [S11] [S12] |
| 发布、意图、接收人和 Outbox 通过用例事务连接 | 有真实一致性价值；不是无理由的分层。[S12] [S13] [S14] |
| Outbox 使用 owner/token/lease 与条件更新，供应商 I/O 在结果事务外 | 保留，尤其是外部短信/邮件；不要把所有异常都简单重发。[S15] [S17] [S20] |
| 收件箱按 `LoginHelper.getUserId()` 查询，不以公告管理权限作为阅读前提 | 阅读授权边界正确；消息盒子为空不能直接归罪于该查询权限。[S07] [S08] |
| IN_APP 已落库后，实时推送失败不会把投递改成失败 | 正确；实时提醒不是站内信的事实来源。[S15] |
| 自定义回执验原始正文、事件去重、不同事实冲突拒绝、提交失败返回 503 | 有价值的安全/可靠性措施，不建议删除。[S36] [S37] [S38] |
| 当前 `objectMetadata()` 和生命周期 `snapshot()` 只读数据库 | 已符合“元数据不访问对象存储”的边界，不应重复报为旧问题。[S40] [S41] |
| 管理列表不直接返回可用下载 URL，按专用入口申请授权 | 正确；授权访问与元数据展示应该分开。[S40] [S54] |
| 最新提交移除了各直传策略里写死的 `minio` 路由 | 有效简化；剩余的 YAML 诊断对象绑定问题应另行处理。[S48] [S55] [S56] |
| 待删除恢复按当前引用重算，整批先校验再修改，且不调用对象存储 | 正确，应保留。[S41] |
| `DefaultOssClientImpl.doInitialize()` 创建客户端，并未在该方法创建桶或设置桶策略 | 不能指控当前客户端初始化会自动申请桶管理写权限。[S50] |
| 已有真实 MySQL/Redis 结果事务集成测试 | 应扩展现有测试基座，不另建“通知测试平台”。本次没有执行这些测试。[S61] |

<a id="notify-flow"></a>

## 2. notify 当前业务实现：先把事实链路讲清楚

### 2.1 公告发布到消息盒子的链路

```text
admin-web / NoticePage
  ├─ 保存草稿 → notify_notice（不投递）
  └─ 点击发布 → NotifyNoticeUseCase.publish（数据库事务）
       ├─ 锁定公告并改变发布状态
       ├─ 创建 notify_notice_snapshot
       └─ NotificationApplicationService.submit
            ├─ 固化逻辑接收人及物理地址
            ├─ notify_intent
            ├─ notify_recipient
            ├─ notify_delivery（每个接收人 × 渠道）
            └─ notify_outbox（可投递项）
                  ↓ 提交后唤醒；轮询作为丢失唤醒后的恢复路径
             NotifyOutboxWorker → claim / lease
                  ├─ IN_APP → notify_message + notify_message_recipient
                  └─ MAIL / SMS → 场景绑定 → 渠道账号 → 限额 → common-notify → Provider
                  ↓
             结果事务：delivery / attempt / outbox / intent 聚合
                  ↓
             实时事件只是提示刷新
                  ↓
             admin-web REST GET /notify/inbox → 当前用户消息列表
```

主要证据：[S06] [S10] [S11] [S12] [S14] [S15] [S16] [S17] [S18] [S20] [S25]。

**必须区分三种状态：** 公告“已发布”是业务发布事实；通知 `QUEUED/PROCESSING` 是后台投递状态；用户消息关系已经落库，才是该用户收件箱中可查询的事实。发布接口成功不等于全部收件人已经送达，邮件/SMS 的供应商 `ACCEPTED` 也不等于最终送达。[S14] [S17] [S32]

### 2.2 接收者与账号配置

`ALL` 在提交时分页读取正常且未删除的系统用户，并有单次总量上限；指定用户与用户类型会做相应有效性/数据范围校验。作者本人不会因为是发布者而自动收到；只有进入接收人集合才会收到。未来才注册的用户也不会凭空获得过去发布时的接收人关系。这是“发布时快照”的合同，不能在没有产品要求时认定为 BUG。[S11] [S14] [S33]

MAIL/SMS 的发送账号、场景绑定和模板来自通知配置数据库。生产配置已经注明不再从 YAML 读取 SMTP 发件账号；这一方向是对的。IN_APP 不需要 SMTP、SMS 厂商、MinIO 或自定义回执密钥，修复时必须保持这些功能独立。[S34] [S25] [S57]

### 2.3 哪些“统一”合理，哪些统一过头

让业务统一提交 `NotificationCommand`、由 notify 解析用户并选择渠道，是合理统一。让 common-notify 只面对物理目标和厂商适配，也合理。

问题在于：本地数据库失败与外部网络“发送结果未知”被统一成一种 UNKNOWN；上层 Outbox 和下层 Redis 完成态缓存同时决定一次投递是否允许重试；IN_APP 本地持久化被当成一个外部 Provider 副作用处理。这些是**业务语义不同却被强行套进相同机制**，应收缩，而不是继续加补丁状态。[S15] [S16] [S17] [S21] [S24]

<a id="notify-findings"></a>

## 3. notify 详细问题与实施方案

<a id="n-01"></a>

### N-01｜消息盒子的持久化查询被实时推送开关一起关掉

**优先级：P1。确认程度：默认配置下确定触发；现场是否存在构建环境覆盖，需要检查实际部署。**

**代码证据。** `admin-web/.env.development` 和 `.env.production` 都设置 `VITE_APP_MESSAGE_ENABLED=false`；`src/utils/push.ts` 的 `initMessageBox()` 在消息开关关闭时清空消息并提前返回，不调用收件箱接口。实时连接初始化与持久化列表加载共用了一个开关。[S01] [S02] [S03]

```text
已有 notify_message_recipient(user_id = 当前用户)
    + VITE_APP_MESSAGE_ENABLED=false
    → initMessageBox 提前返回
    → 浏览器不发 GET /notify/inbox
    → 顶部消息盒子为空
```

因此，仅把后端 `message.enabled` 打开、检查 SSE 连接、重发公告、补公告权限，都不能解决这个前端默认路径。`VITE_*` 是构建配置，生产环境不能只改服务器上的一份未参与构建的 `.env` 就期待现有浏览器产物改变。[S01] [S02] [S03]

**最小技术方案。** 将 `initMessageBox()` 的前置条件收缩为“当前存在有效登录会话”，只有注销或切换身份时清空旧用户数据；它始终可以调用 REST。`VITE_APP_MESSAGE_ENABLED` 只控制 `initPush()` 的 SSE/WebSocket 连接。登录完成、消息盒子打开以及已有消息事件触发时刷新持久化列表，保留现有 generation/token 防止旧会话响应污染新会话的保护。不需要新增轮询服务，更不需要同时维持 WebSocket 与 SSE 两套在线通道。

临时排障可以将该开关设为 `true` 后重新启动开发服务或重新构建前端，用来验证这一条因果链；但这只是绕过错误耦合，不是最终修复。最终应允许“关闭实时推送，但正常使用站内信”。

**回归验收。** 关闭开关，给当前用户发布 IN_APP 公告，等待其投递完成后打开消息盒子，应有 REST 请求并显示内容；开启开关时结果相同，只增加实时刷新；退出 A 后登录 B，延迟返回的 A 请求不得覆盖 B；后端消息实时服务关闭时，收件箱列表仍能正常加载。

<a id="n-02"></a>

### N-02｜SMS 请求与 common-notify 的内容快照合同互相冲突

**优先级：P1。确认程度：通过当前统一路径的短信存在确定冲突。**

**代码证据。** `DispatchNotificationService.toContent()` 对短信构造：

```java
new NotifyTemplateContent("sms", plan.smsTemplateCode(), plan.smsParams(), "")
```

`NotifyDispatcher.send()` 首先调用 `validateRequest()`，而它明确拒绝模板内容的空 `contentSnapshot`，抛出 `CONTENT_SNAPSHOT_REQUIRED`。`NotifyTemplateContent` 构造器只规范化参数 Map，没有替短信填充快照。[S15] [S21] [S22]

即使账号启用、短信模板映射和额度都正确，走到这里也会在实际厂商适配器调用前失败。随后通用异常处理还会把该确定的本地校验错误变成 UNKNOWN，进一步进入等待回执路径。这不是“短信厂商偶发失败”。

**最小技术方案。** 在场景发送计划中形成真实的逻辑内容快照，并传给 common-notify。短信供应商模板码、参数映射和逻辑内容快照是三个不同概念：快照应能解释本次通知意图，不应填一个无意义字符串只为绕过校验。对验证码等敏感场景，传输所需值与审计可见值要分开，延续现有脱敏合同，不要把修复变成向普通日志输出验证码。

提交前能够确定的模板缺失、变量缺失、未绑定账号应返回明确业务错误；执行阶段遇到本地合同错误应终止为明确失败，不能等待供应商回执。不要通过放开 common-notify 的所有校验来“修好短信”。

**回归验收。** 用真实 `DispatchNotificationService`、真实 `NotifyDispatcher` 和假厂商适配器串联测试；有效短信应恰好调用适配器一次，并携带正确模板参数与非空快照。空快照仍应在下层负向测试中被拒绝。验证码场景的审计输出不得出现验证码明文。仅 mock `NotifyClient.send()` 返回成功无法捕获本问题。

<a id="n-03"></a>

### N-03｜站内信本地失败被当成外部结果未知，共享消息写入又不够幂等

**优先级：P1。确认程度：异常分类问题确定；重复主键触发要求多个 Worker 并发投递同一 intent 的不同收件人。**

**代码证据。** 每个收件人的 IN_APP delivery 使用同一个 `intentId` 作为消息主键。`InAppNotificationService.persist()` 对 `notify_message` 使用“查询不存在，再普通 insert”，之后才插入该用户的消息关系。这个流程没有数据库原子 upsert，也没有由该方法自身建立的完整本地事务。[S15] [S16] [S09]

两个应用实例可以分别领取同一公告的两个不同 delivery；消息级查重不在 delivery 租约的保护范围内。二者都读到消息不存在时，一个插入成功，另一个可能在插入共享消息时遇到重复键，尚未为自己的用户插入关系。即使没有这个并发条件，普通的本地数据库异常也会触发同一分类错误：外层将其标记 UNKNOWN，结果服务将 Outbox 改成 `WAITING_RECEIPT`。实际回执通道只支持 MAIL/SMS，IN_APP 没有供应商会在稍后补一个送达回执。[S17] [S18] [S20] [S38]

**最小技术方案。** 给共享消息和用户消息关系建立明确的数据库幂等写入：确认并复用消息主键及 `(message_id, user_id)` 唯一约束，缺失约束时先检查存量重复，再补迁移；使用针对该唯一约束的原子写入或只捕获已知重复键并核对同一业务事实。消息和本次收件人关系的写入放入短本地事务，不用捕获所有 SQL 异常后假装成功。

错误分类必须分渠道：本地明确回滚的暂时性数据库异常允许有限重试；参数/装配等确定错误明确失败；只有外部请求可能已被接受、又无法确认的情况，才使用 UNKNOWN。更清楚的最终边界，是将 IN_APP 落库与相应投递状态在本地结果事务内完成，实时事件提交后发送；这可以逐步收敛，不要求本次大规模重构所有 Provider。

**回归验收。** 两个真实数据库连接并发处理同一 intent 的不同用户，最终只有一条消息、每个用户一条关系且均可查到；数据库暂时失败后可重新投递；重复任务不会增加关系；实时发送抛异常不撤销已落库站内信；IN_APP 不得因本地失败永久停在等供应商回执的状态。

**不要做的事。** 不新增“站内信回执查询补偿器”，也不在消息盒子中根据公告表临时伪造收件人关系。两者都绕过真实根因。

<a id="n-04"></a>

### N-04｜Outbox 想重试，Redis 完成态却一直重放旧失败

**优先级：P1。确认程度：在下层幂等窗口内确定发生。**

**代码证据。** `DispatchNotificationService` 将 delivery ID 同时作为 `requestId` 和 `idempotencyKey`。`NotifyDispatcher` 对非成功结果也调用 `complete()`；`RedisNotifyIdempotencyStore.complete()` 不区分结果是否成功，均保存为 `COMPLETED`。下次相同请求取得 `Completed` 后直接执行 `requireAccepted(completed.result())`，失败结果再次抛出，不会调用渠道适配器。[S15] [S21] [S23] [S24]

所以外层看到的是“又尝试了一次”，厂商看到的却可能只有第一次请求。有限重试额度可能被同一份失败缓存消耗完；在同一个 delivery 下修改模板等参数，还可能转为幂等摘要冲突。这是两套机制各自合理、组合后合同错误的典型例子。

**最小技术方案。** 先为当前调用链确立一个重试决策者：Outbox 拥有尝试次数与重试节奏；下层幂等只负责阻止已接受/结果不确定的外部请求被盲目重复。对于明确未发送、可重试的失败，释放当前持有的 claim，或记录可再次尝试的状态，不能与已接受结果同样封存在 `COMPLETED`。外部超时等未知结果不能通过“释放所有失败缓存”一刀切，否则可能重复发短信和计费。

长期收缩方案是让统一编排路径的渠道适配层尽量只做一次物理发送，幂等和重试事实归到统一控制面；若还有合法的独立同步调用方需要 common-notify 幂等，可保留该能力，但必须显式区分使用方式，不能让双方同时控制同一 delivery 的生命周期。此次先修状态合同即可，不必立即删除整个 Redis 实现。

**回归验收。** 假供应商第一次明确拒绝且可重试、第二次接受：两个实际 Provider 调用，最终成功；已接受结果重复领取：不得再次发送；模拟外部响应丢失：不得自动当作明确未发送；Redis 完成态写入失败不能被误记为业务送达。测试应覆盖真实 common-notify，而不是只统计 Outbox 次数。

<a id="n-05"></a>

### N-05｜重试接口的指定目标、空操作和资源 ID 不一致

**优先级：P2。确认程度：确定的接口行为偏差。**

**代码证据。** `NotificationRetryCommand` 提供 `deliveryId`，运行时 `retry()` 却遍历该通知下所有可重试投递，未按它过滤。即使没有任何符合条件的 delivery，仍把 intent 更新为 `QUEUED`。`NotificationController` 的 retry/cancel 在请求体存在时直接使用体内命令，使 URL 的 `notificationId` 不再是实际操作对象。[S28] [S14] [S27]

这里不将其夸大为已证明的越权漏洞：接口已有相应权限检查。真实问题是 API 表意和实际操作不一致、审计难以解释，并且产生“显示排队但实际没有任务”的状态。

**最小技术方案。** URL ID 作为唯一资源标识，体内只接受操作参数；兼容旧请求时，ID 不同应明确拒绝。指定 delivery 时验证它属于该 intent，只处理这一条；未指定才使用通知级重试。没有可重试项时返回当前真实状态和零重试数，不改变聚合状态；存在任务时才根据实际 requeue 的成功结果更新。对已经暴露但尚未使用的重试幂等字段，明确实现或明确拒绝不支持的输入，不继续静默忽略。

手工重试 UNKNOWN 还应明确区分本地可安全重做与外部可能重复发送，不能把本节修复变成一个“所有异常一键重发”的开关。

**回归验收。** 一条通知有 A/B 两个失败 delivery，指定 A 只产生 A 的重试；全部已完成时重试保持完成态；不属于该通知的 delivery 被拒绝；URL/body ID 冲突被拒绝；重复提交同一次重试不得制造多份活跃任务。

<a id="n-06"></a>

### N-06｜消息截止时间没有执行，时效性通知可能在失效后继续发送

**优先级：P1（验证码等时效消息）；普通通知的影响取决于使用合同。**

**代码证据。** `NotificationCommand` 有 `expiresAt`，提交时检查它与 `scheduledAt` 的先后关系；但领取 SQL 只判断开始时间、重试时间和租约，投递执行也没有在 Provider I/O 前检查截止时间。验证码调用传入的 `expiresAt` 是 null，而 Redis 中验证码按既有有效期过期。[S29] [S14] [S20] [S15] [S30]

只要队列积压或渠道长时间不可用，用户就可能收到早已不可使用的验证码。此处不是要求引入复杂高可用短信路由，而是必须尊重已有命令的时间合同。

**最小技术方案。** 验证码生成时计算一次明确的截止时刻，缓存 TTL 与提交命令使用同一个截止时刻；提交和手工重试拒绝已经过期的请求。Worker 在实际发送前再次以统一时钟检查截止时间，过期后结束该 delivery/outbox，不再调用供应商。可以先沿用明确失败状态并记录 `NOTIFICATION_EXPIRED` 错误码；不必为了这个修复扩展一套新的大状态机。结果聚合、回执和用户提示需要区分“受理排队”与“已经送达”。

对于此前已经入队但没有截止时间的验证码，不能仅靠新代码自动推断它们仍有效。发布时应通过场景、创建时间和现有验证码规则制定受控处置，不批量盲目重发旧验证码。缓存写入与提交先后的现有窗口，也应在调用方测试中覆盖；不要把网络发送拉入用户注册/登录的长数据库事务。

**回归验收。** 未来计划消息未到时不执行；超过截止时刻的消息实际 Provider 调用次数为零；重试不延长业务有效期；队列延迟超过验证码有效期后用户不会再收到该旧任务；刚好位于截止边界时使用一致的时钟判定。

**同类合同收缩。** 当前命令还接收 mode、priority 和编排策略，但领取 SQL 没有使用 priority，执行路径也不能据此保证同步完成。`ESCALATION` 在前序已送达时仍可能持续 WAIT；当前 WAIT 路径没有靠截止时间终止。对尚无真实需求的模式，最小方案是校验并拒绝不支持的取值，而不是“既然已经有枚举，就补齐所有高级编排”。抽查业务调用主要使用 ALL/ASYNC，不应为未来假设新增工作流引擎。[S14] [S15] [S17] [S20]

<a id="n-07"></a>

### N-07｜公告“撤回”与收件人跳转没有形成闭环

**优先级：P2。确认程度：代码行为确定；撤回已送达消息的产品语义需要明确，不能假定可以撤回外部邮件/SMS。**

**代码证据。** `NotifyNoticeService.retract()` 修改公告生命周期，`NotifyNoticeUseCase` 没有把这个动作连接到通知取消、待发送 delivery 停止或收件人消息状态。发布快照的跳转路径是 `/notify/notice?noticeId=...`；但当前 `NoticePage.vue` 是管理列表，并不消费这个 query 参数来打开该通知，已发布条目也不能通过编辑入口查看完整内容。[S10] [S12] [S11] [S06]

这意味着用户点击“撤回”后，尚在队列里的消息仍可能发送；收件人点击业务跳转后，又可能只看到管理列表，或者根本没有管理菜单访问权限。不能通过给全部收件人授予公告管理权限修复阅读体验。

**最小技术方案。** 先给现有撤回按钮一个可执行合同：停止该公告相应发布版本尚未发送的任务；保留发布快照和已经发生的投递审计。对于已经接受的邮件/SMS，明确提示无法撤销。取消操作应复用现有通知取消能力并与公告状态更新在同一数据库事务中完成，Worker 在即将发送时遵守取消状态；已进入外部 I/O 的请求存在不可撤销边界，不能在 UI 宣称“所有消息均已追回”。不能只改 intent 的显示状态而不改变可执行任务。

收件人阅读优先使用已有收件箱中的不可变快照：消息盒子打开该消息详情，或者跳到现有收件箱对应内容，而不是跳到管理员编辑页面。确有业务详情页面时，跳转到该业务的阅读入口，并继续由业务检查当前用户权限。发布者管理页面可对已发布快照提供只读查看，复用详情展示，不另建一套公告前台。

**回归验收。** 发布后、Worker 执行前撤回，后续没有新的外部请求；撤回不会修改旧快照正文；已接受外部消息有准确提示；普通接收用户无需管理权限即可阅读自己的消息；点击消息不再只到达忽略 noticeId 的列表。

<a id="n-08"></a>

### N-08｜收件箱的 500 条截断与消息盒子呈现影响完整性和可解释性

**优先级：P2。确认程度：截断与前端行为由代码确认；不是所有空列表都由这一项造成。**

**代码证据。** 当前用户消息关系查询最多取最近 500 条，REST 返回普通 List，前端没有“下一页”合同。`readAll()` 操作的是当前用户全部未读关系，而不是仅这 500 条。消息盒子默认页签为系统分类，页签数量来自已装载消息集合；正文同时进入 message/content 后，展示存在重复内容的路径。加载失败与真正没有消息也没有充分区分。[S09] [S08] [S07] [S04] [S05] [S16] [S03]

这里真正需要修的是：完整收件箱不能在没有说明的情况下把可读取范围固定截断；不能让“未加载/加载失败”和“没有消息”呈现为同一结论。若产品明确只提供最近 500 条，应把这个限制写进接口与页面合同，而不是把它当作无限历史列表。默认页签为空还可能掩盖其他分类中的消息，但这不是数据库丢消息，应与 N-01 分开诊断。**分类页签展示总条数本身并没有错，本报告不把它强行认定为未读计数 BUG。**

**最小技术方案。** 沿用项目已有分页模式，给收件箱增加明确的分页或游标合同；初次只取较小一页，继续阅读时再加载，保留稳定排序和消息 ID 作为同时间排序补充。分页后已有数量展示要明确是本页/分类总数；需要展示未读总量的地方应按当前用户统计，不能从这一页的长度猜测。无需为了本轮修复另加一个未读统计面板。顶部消息盒子只展示最近摘要，完整历史走已有收件箱页面，避免一次拉完全部历史。

“全部已读”应明确为当前用户所有未读消息，并展示这一语义；若只想操作当前页，改按钮文案及接口参数，而不是静默改变范围。摘要使用一份截断正文，详情只展示一次完整正文。加载中、加载失败可重试、确实为空分别显示；不为这些状态建立新的全局事件总线。页签可默认展示全部或现有非空分类，但不要偷偷更改后端消息分类规则。

**回归验收。** 生成 501 条当前用户消息，第 501 条可以通过后续页面取得；未读数与实际用户关系匹配；另一用户不能出现在列表或被标记；请求失败显示失败而非“暂无消息”；正文只出现一次；已读操作后顶部计数和完整收件箱一致。

<a id="n-09"></a>

### N-09｜现有邮件演示接口的无链接正文与附件在统一迁移中丢失合同

**优先级：P2。确认程度：限定在已核查的调用方，不据此宣称所有业务邮件都失效。**

**代码证据。** `MailSendController` 的普通邮件及带附件邮件都使用 `notice-published` 场景，并传入 `path=""`；`NotifySceneCatalog` 对该场景要求非空 path，`NotifySendPlanner` 在实际渲染前就会返回 `MISSING_VARIABLE`。另一方面，附件 ID 被放在模板参数 `attachmentOssIds` 中，`DispatchNotificationService` 构造物理 `NotifyRequest` 时没有将它们映射到附件字段，因此下层的附件快照能力收不到这些 ID。[S31] [S26] [S25] [S15]

这两个问题有先后关系：只修附件映射，邮件仍可能被空 path 拦住；只放开 path，附件仍不会出现。

**最小技术方案。** 不要求一个原本只发送主题/正文的邮件接口伪造业务路径。由实际模板决定链接变量是否必需，或把可选 path 从所有邮件共用的强制变量合同中分离；保留真正需要业务链接的场景校验。附件应进入受验证的统一命令字段，或先在兼容入口集中解析现有参数，再传给物理请求，不让每个 Provider 从任意 Map 里猜字段。

有附件时在业务授权后复用现有附件快照/物化能力，无附件时必须提前返回空附件集合，不访问 OSS。`NotifyDispatcher.createSnapshots()` 已有无附件快速返回，应当保留，不要在“为了统一检查”时给普通邮件也附加 MinIO 依赖。[S21] 对无权限、不存在或正在删除的附件明确失败，不能静默丢掉附件后仍向调用者报告发送成功。

**回归验收。** 普通无链接邮件正常提交并发送；带一个及多个附件的接口确实将对应快照交给适配器；无附件邮件对 OSS 的调用次数为零；非法或无权附件得到明确错误；现有需要 path 的工作流通知仍进行相应校验。不要新增邮件营销、批量模板平台等与本次修复无关功能。

<a id="n-10"></a>

### N-10｜全量公告的逐行写入与每投递一次全量聚合，需要在现有规模下收敛

**优先级：P2，按实际规模验证。性质：静态可推导的复杂度风险，不是已经测得的慢查询事故。**

**代码证据。** 提交时解析并收集接收人，为接收人、delivery、outbox 逐条插入；每个投递结果都锁定 intent，再读取该 intent 下所有 delivery 计算聚合状态。[S14] [S09] [S17] [S32] 对 R 个接收人、D 个可投递项，仅这三类记录的主要写入就约为 R + 2D；逐次全量聚合的读取规模随 D 呈二次增长。这个数量关系是代码推导，不是吞吐量、延迟或硬件需求的实测值。

**实施顺序。** 先用当前常见公告规模记录 SQL 次数、提交耗时、结果事务锁等待，不为假设的百万用户建新系统。第一步可直接使用现有 MyBatis 批量能力，减少逐行往返；明确单次公告的合理上限，避免把多个供应商调用塞入发布事务。

聚合部分先决定状态的真实消费需求：若状态仅用于查询，优先在读取时按 delivery 计算，减少每条结果重复全量装载；若有业务依赖持久化聚合态，按 intent 合并同批已提交结果的聚合更新，并保持最终查询可核实真实 delivery。SQL `GROUP BY status` 可以减少网络传输和 Java 对象，但每次仍扫描全部 delivery 时，并没有从根本上消除二次扫描，不能把它宣传为完整修复。增量计数需要处理重复结果和回滚，只有实测必要才引入。

**回归验收。** 保留现有结果事务原子性和租约测试；增加代表性公告规模的 SQL/耗时基线，证明减少往返且没有丢收件人、重复关系或错误聚合。若当前规模没有瓶颈，先保留清晰上限与测试即可，不把本项当作重写 Outbox 或引入 Kafka 的理由。

<a id="oss-findings"></a>

## 4. OSS / MinIO：实际问题与应收缩的依赖

<a id="o-01"></a>

### O-01｜只读诊断被提升为业务可用性的强制门禁

**优先级：P1。确认程度：依赖链确定；是否造成容器重启取决于实际部署探针，不能仅从代码断言。**

**代码证据。** 启动 `SystemApplicationRunner` 同步执行 `readinessService.refresh()`；诊断会遍历存储配置，要求 `diagnosticObjects[configKey]` 指向存在的诊断对象。当前本地配置又把诊断对象绑定到 `minio` 这个配置键。注册表的 `requireServing()` 拒绝缺失、失败或过期快照；下载路径的 `requireDownloadable()` 在生成访问 URL 前调用它，直传初始化也要求目标条目是 SERVING。相关健康指标会因必需存储不满足而 DOWN。[S46] [S42] [S45] [S56] [S43] [S41] [S48] [S44]

```text
一个真实业务对象可以正常 GetObject
    ↓ 但：未人工准备 canary / DB 配置键改名而 YAML 未跟着改 / 诊断过期
readiness = NOT_SERVING
    ↓
下载 URL 生成被拒绝；直传初始化被拒绝
```

这里必须准确区分：客户端初始化并没有自动创建桶；辅助 CORS/Lifecycle 检查失败主要记录警告；诊断失败也不等价于 JVM 一定退出。实际问题是启动存在同步远端检查、整体健康被存储诊断影响，且正常上传/下载直接依赖该诊断结果。非必需配置未通过不一定导致整体 DOWN，但仍被遍历检查。[S42] [S47] [S50]

**最小技术方案。** 将三种职责拆开，而不是再添几个 `ignore-*` 配置绕过：

| 职责 | 应做什么 | 不应做什么 |
|---|---|---|
| 业务请求前的本地校验 | 检查当前配置存在、目标桶/endpoint 合法、预期访问类型及用户权限 | 不要求先读桶策略、ACL、CORS、Lifecycle 或独立诊断对象 |
| 真实上传/下载操作 | 执行所需对象 API；保留可解释的超时、权限及不存在错误 | 不把某个 canary 的状态当成所有对象可用性的真相 |
| 管理员诊断 | 按选定配置主动检查，并区分已验证/不确定/失败 | 不阻断没有使用存储的登录、菜单、普通站内信和无附件邮件 |

从 `requireDownloadable()` 和直传路由中移除对全局诊断快照的硬依赖，改读本地权威配置并保留业务安全校验。应用启动只做必要的配置结构校验；取消全配置同步远程巡检。诊断默认按管理操作触发，确有运行监测需求时只监测明确需要的配置，不能为了几分钟快照年龄又加第二个自愈调度器。

健康端点分清存活、核心服务就绪与可选存储诊断；采用怎样的探针组应与实际部署保持一致，不能让一个没使用到的 MinIO 桶导致整个业务实例被摘除。诊断对象不应是正常接入的必填 YAML；管理员可以选择一个现有对象进行只读检测，或者使用可选测试对象，而不是每换一个 DB 配置键都重新部署应用。

**回归验收。** 空存储配置、非默认坏配置、缺 canary、桶策略读取 403、MinIO 离线等情况下，核心应用仍能按其真实依赖启动；普通站内信和无附件邮件不访问存储；有合法配置时可生成预签名，不因诊断过期失败；真实文件请求失败仍返回明确错误；权限校验和 PRIVATE 预期不能因去掉诊断而失效。

<a id="o-02"></a>

### O-02｜策略“不可知”被当作“没有允许”，导致错误拒绝与错误安全结论

**优先级：P1（命中该权限模型的配置）。确认程度：确定逻辑缺陷。**

**代码证据。** `AbstractOssClientImpl.diagnoseAccess()` 对 `GetBucketPolicy` 403 返回空 policy，对 `GetBucketAcl` 403 返回空 grants。`PublicAccess.from()` 将二者解释成 `readAllowed=false, writeAllowed=false`。即使匿名 HEAD/GET 实际成功，PUBLIC_READ 配置仍会在 `publicReadDeclared != expectedRead` 分支得到 `POLICY_MISMATCH`。这与“最小权限账号不可读策略时继续看匿名访问”的注释并不一致。[S49]

另一面，对于私有桶，不可读策略不应被解释成“已经证明匿名写被禁止”。当前简化 parser 只看 Allow、通配 Principal 和若干 Action，不能完整表达 Resource、Condition、显式 Deny 等访问控制语义。它不适合作为整个桶的安全裁决器，更不适合作为生产访问的强制门禁。

**最小技术方案。** 先实施 O-01，使诊断错误不再阻断正常业务。诊断结果采用三种事实：已观察到允许、已观察到拒绝、没有权限或无法判断。某个对象的匿名读探测只能证明该对象在该时刻的读行为，不能推导整个桶所有前缀都安全，也不能推导匿名写被禁止。策略/ACL 读取被拒绝时显示“策略不可验证”，而不是伪造空策略。

对能直接识别的危险公开写配置可以给出告警，但不要继续补齐一个自研 IAM 解释器来处理所有供应商差异。复杂条件策略交给存储侧的策略管理和测试；应用只报告自己确实观察到的事实。不为验证匿名写而向真实业务桶执行破坏性匿名 PUT/DELETE。

**回归验收。** PUBLIC_READ：策略和 ACL 都 403、匿名读取成功，不能再误报为确定 POLICY_MISMATCH；PRIVATE：策略不可读时不能声称已完整证明无匿名写；带前缀/条件策略只能做有边界的结论；最小权限应用凭据不需要因诊断而扩大到桶管理权限。

<a id="o-03"></a>

### O-03｜恢复私有来源与清理来源并发时，可能删掉当前仍在使用的对象

**优先级：P1。确认程度：源码存在确定竞争窗口；未在真实 MinIO 动态复现。**

**代码证据。** `OssStorageMigrationService.unpublish()` 先确认来源对象存在，再把 `sys_oss.service` 从目标切回来源，然后将工单保存为 ROLLED_BACK。`cleanup()` 提前读入工单并检查状态/安全窗口，随后删除来源对象，没有与 unpublish 使用同一对象级互斥，也没有在删除前重新锁定并检查当前 `sys_oss.service`。`MybatisOssMigrationStore.saveItem()` 还没有检查 `updateById()` 影响行数。[S52] [S53]

```text
清理请求 A：读到工单 CLEANUP_ELIGIBLE，安全窗口已结束 → 暂停
恢复请求 B：确认来源存在 → service 从公开目标切回私有来源 → 返回成功
清理请求 A：继续按旧工单删除来源对象
结果：当前 sys_oss 指向的来源对象已被删除
```

审批按钮和延迟清理窗口可以降低误操作概率，但不能消除这条竞争。这里值得保留一个小而明确的互斥规则，因为影响是实际数据可用性；不需要为此建立跨服务 Saga 平台。

**最小技术方案。** 对会改变对象存储指针或清理来源的管理操作，统一以相同顺序锁定 `sys_oss` 与对应迁移工单。unpublish 在同一个短事务里重新确认工单状态、来源可恢复、指针预期，并原子更新指针与工单；严格检查条件更新影响行数。

cleanup 必须在相同互斥下重新确认“对象仍指向目标配置、当前工单仍允许清理、窗口已结束”。对当前低频单对象管理操作，可以选择每对象一段有严格超时的事务，在对象锁内做幂等远程删除并记录结果；这是持有行锁跨短暂 I/O 的明确取舍，不能扩大到整个批次或普通请求。若现有业务量不允许这种方式，再复用已有工单做条件领取，阻止领取期间恢复；不要先引入全新分布式锁和补偿状态机。

所有能切换这个指针的相关入口应遵守同一规则，不能只给一个按钮加前端禁用。对象已被删除而数据库确认丢失时，重试将“不存在”视为该清理目标已达到，再补齐工单；此时当前指针必须仍指向保留的目标对象。

**回归验收。** 用同步屏障固定上述并发顺序；恢复先成功则清理不得删除当前来源，清理先取得合法执行权则恢复必须明确失败；指针更新失败不得把工单标为恢复成功；重复清理幂等；任一对象失败不持有整个批次的锁。

**明确不报错的边界。** 当前 UI 已提示“公开桶里已复制的文件不会删除”，实现也如此。因此本报告不把 unpublish 保留公开副本另报为隐藏漏洞。它是“记录切回私有来源”，不是“撤销所有已分发公开 URL”。成功文案最好也保留这一限定，避免操作完成后又简称为完全私有。[S54]

<a id="o-04"></a>

### O-04｜OSS 列表的过期响应保护放在状态写入之后

**优先级：P2。确认程度：确定的前端竞争窗口。**

**代码证据。** `OssPage.vue.getList()` 递增 `listGeneration`，但在 `withLoading()` 内 await 后直接写 `ossList`、`total` 等状态；退出后才判断 generation 是否过期。这只能阻止旧请求继续加载预览 URL，不能阻止旧列表覆盖新列表。[S54]

**最小技术方案。** 先将配置及列表响应放在本次调用的局部变量里，在修改页面状态之前验证 generation 与组件存活状态。行数据、总数和该批预览标识应作为同一代结果更新；loading 也只由当前代请求结束。已有 generation 足以处理这个问题，不必增加一套统一请求仲裁框架；可选取消请求只是节省资源，不能替代提交状态前的检查。

**回归验收。** 查询 A 后立即查询 B，控制 A 最后返回：最终行数据、总数、预览都必须属于 B；组件卸载后不再回填；旧请求异常不覆盖新请求已成功的页面。复用通知页面已经采用的“先检查、再赋值”习惯即可。[S06]

<a id="security"></a>

## 5. 安全配置：两项应先处理的明确问题

<a id="s-01"></a>

### S-01｜公开仓库提交了本地数据库和 Redis 凭据

**优先级：P1，先处置。确认程度：未脱敏值存在于公开版本控制；未验证是否仍有效、是否能从公网连接。**

`application-local.yml` 的注释写着不要提交 secret，但实际包含非占位的数据库和 Redis 连接凭据。该文件也包含特定内网地址，说明它并非一份可直接分享的通用示例。[S56] 本报告不复述这些值，也没有尝试使用它们连接任何服务。内网地址并不能让已经公开的凭据重新变成秘密。

**实施方案。** 将仍有效的相关凭据视为已披露并先轮换，同时检查使用它们的应用连接与必要的访问日志。然后移出版本控制，提交不含真实凭据的 `.example`，实际配置从环境或未跟踪本地文件注入。补 `.gitignore` 不会自动取消已经被跟踪的文件，必须明确取消跟踪；清理 Git 历史是后续治理，不替代轮换，也不能保证外部副本已经消失。

正常启动不应强制要求仓库内这份真实本地文件存在。生产必要的 secret 缺失可以明确失败，本地示例则说明需要哪些值；不要用新的加密配置平台替代一个本来只需移出 Git 的问题。CI/提交检查至少覆盖实际数据库/Redis 密码、云厂商密钥和私钥，不把所有公开 client ID 或示例文本都判成 secret。

**验收。** 新凭据生效、旧凭据被撤销；全新 clone 不含真实 secret；已跟踪文件清单不再包含实际本地配置；构建产物和报告也不含这些值；检查不会因普通公开参数误报而被长期关闭。回滚应用代码时不得恢复旧凭据。

<a id="s-02"></a>

### S-02｜“本地临时通配”实际变成带凭证的全局 CORS 默认值

**优先级：P1。确认程度：配置与过滤器行为确定；不能据此直接宣称攻击者能拿到 localStorage 中的 Bearer token。**

公共 `application.yml` 给出了通配来源默认值，`CorsProperties.allowCredentials` 默认 true；`ResourcesConfig` 遇到 `*` 时改用 `allowedOriginPatterns("*")`，从而允许任意请求 Origin 配合凭证跨域。这个行为没有限定在 local profile。[S55] [S58] [S59]

风险取决于端点是否使用浏览器自动携带的凭证、Cookie 的实际属性、CSRF 保护和部署覆盖。CORS 本身不会把另一站点 localStorage 里的 token 交给攻击者，因此本报告不作这样的错误推断。但这依然是不应进入公共生产基线的信任边界回退。

**最小技术方案。** 公共配置默认不允许跨来源，或仅允许明确的受信来源；开发优先走已经存在的 Vite 同源代理，确需跨域时在 local profile 中配置实际开发 Origin。带凭证情况下拒绝生产通配来源；真正开放的无身份公共资源应在明确端点范围使用不带凭证的规则，不能为了它放开整个应用。

CORS 与 MinIO 桶 CORS 是两个不同配置域：前者控制浏览器访问后端，后者控制浏览器直传/读取对象存储。不要通过放宽后端 CORS 去处理错误的 MinIO CORS，也不要从不可信请求的 Origin 自动生成信任白名单。

**验收。** 恶意来源不能取得许可响应头；明确允许的业务来源正常访问；同源代理开发无需全局通配；测试带凭证和不带凭证两种情形。生产部署存在显式白名单覆盖时，可降低即时暴露，但仍应修正仓库的不安全默认值。

<a id="simplification"></a>

## 6. 复杂度审查：应删减什么，不应删减什么

<a id="d-01"></a>

### D-01｜把日常启动、构建修复和深度诊断分开

**性质：设计改进建议；不是“脚本长就是错误”。**

当前 `scripts/start-dev.sh` 同时处理前后端菜单、多种清理模式、平台路径差异、端口查找、Vite env 解析、YAML 端口解析、Maven 构建锁、classpath 解析、target/本地仓库两份 JAR 的哨兵类校验及实际启动。即使选择直接启动，仍要经过相当多的产物验证。[S60]

其中缓存删除白名单、拒绝越界路径、避免误杀占用端口进程、必要的 Windows 兼容都值得保留。问题是所有使用者被迫理解构建内部状态，日常路径也承担了一次“自检排障”。脚本还要求存在非空的 `application-local.yml`，自行解析字面量 `server.port`，随后通过命令行覆盖 Spring 端口；合法的环境变量表达式和正常配置优先级因此受到限制。[S60] [S56]

**建议的最小交付形态：** 一个常用启动入口、一个显式构建入口、一个可选 repair/doctor 入口。常用入口只检查必要工具与配置是否具备，然后交给框架启动；源码变更后的增量构建由已有 Maven/IDE 流程负责。清空 target、重装依赖、JAR 哨兵比对、缓存深度清理留给明确修复入口，不应成为每次启动的强制前奏。支持直接命令参数，交互菜单可以保留为薄包装，不能只有菜单才可运行。

不要在 Bash 中再实现 YAML 或 Vite 的完整配置解析器。后端端口交给 Spring 的配置系统；前端环境交给 Vite。示例默认值使用本机依赖与环境注入，不绑定作者的内网主机。启动日志说明实际生效 profile/地址，但不得打印 secret。

**特别避免一条错误简化。** 现有脚本说明了仓内 BOM 与 reactor 的限制，不能直接把后端替换成聚合根 `spring-boot:run -am`：无主类依赖模块也可能执行该目标。应保留必要的构建/安装与单模块运行分工，先在当前 POM 下验证，再简化外层脚本。[S60]

MapStruct 旧生成源若确有增量构建问题，应通过可重复的“两次构建”用例定位构建配置；本次没有审计完相关 annotation processor 配置，不能把脚本注释当作已证明的编译器缺陷。先把 clean 修复留在可选路径，比每次建议全量重建更合理。

**实施验收。** 全新环境按说明可启动；正常二次启动不做深度 JAR 校验；从终端环境设置端口不被脚本静默覆盖；缺工具和缺必要 secret 有单一明确错误；repair 的安全删除保护仍通过测试；Windows/Linux 分别验证当前项目实际承诺支持的路径。

<a id="d-02"></a>

### D-02｜配置只保留真实部署差异；业务常量和权威事实不要重复配置

当前项目已有正确方向：通知发件账号迁入 DB、上传使用当前默认存储而不是每个策略固定 minio。仍需清理的是“同一事实有多个来源”和“所有默认值都要求部署者理解”。[S34] [S48] [S55] [S56]

| 内容 | 建议唯一归属 | 本次处理方式 |
|---|---|---|
| 默认存储配置键 | 现有 OSS 配置数据库及其受控缓存 | 不在每个上传 policy 或另一份 YAML 再配置一次 |
| 历史对象属于哪个存储 | `sys_oss.service` | 不因当前默认值变化而把旧对象自动路由到新桶 |
| endpoint、bucket、凭据、必要 region/domain 差异 | OSS 配置实体；secret 安全注入/存储 | 保留真实环境差异，不能指望凭空推断所有第三方 endpoint |
| 业务上传策略名称、允许类型、典型大小上限、对象前缀 | 小型代码策略目录 | 代码提供可工作的安全默认值，只有实际需要的限额保留覆盖入口 |
| SINGLE 模式用不到的分片参数 | 不要求每个 SINGLE policy 配置 | 采用公共默认或仅 MULTIPART 读取，不复制无效参数 |
| canary 对象及桶策略读取 | 可选管理员诊断 | 从正常启动和访问硬条件中移除，不按 DB configKey 强制写 YAML |
| 公共访问域名 | 能由 endpoint/bucket 合理派生时派生，确有 CDN/反向代理差异时显式配置 | 不强迫每种部署填写重复域名，也不错误重写私有签名地址 |
| 桶 CORS、生命周期和匿名访问策略 | 存储部署/管理员职责 | 应用文档说明所需效果，运行账号不负责管理它们 |
| 消息实时传输 | 一个传输选项/开关 | 不决定持久化收件箱是否存在 |
| OIDC issuer、client、redirect URI | 客户端真实信任与注册合同 | 不能作为“配置太多”随意删除或从请求猜测 |

例如小文件 SINGLE 策略配置了远高于文件上限的分片阈值，这些值即使运行时无害，也会让维护者误以为必须调参。可以从模板配置中移除，保留代码默认，而不是为了校验这些死参数又增加一层规则引擎。[S55] [S48]

**读取 OSS 配置也要分场景。** 只展示文件名/大小/引用，应查 DB；需要给私有对象生成签名，应读取该对象实际所属配置及凭据，这是必要读取；普通站内信、无附件邮件不应因此创建 OSS 客户端。`OssFactory.instance()` 当前会读取缓存配置并核对客户端配置，是否进一步减少缓存读取应在调用频次实测后决定，不要直接删掉配置变更失效机制。[S40] [S41] [S51]

### 6.1 最小 MinIO/S3 权限边界

下面按当前功能所需的 API 效果划分，不冒充对作者部署版本的实时权限验证。不同兼容服务的策略名称、错误码和实现细节应在该部署版本验收；本次没有运行对象存储权限测试。

| 使用场景 | 应用运行身份实际需要 | 不应为该场景额外要求 |
|---|---|---|
| 管理列表、文件元数据、引用/待删除恢复 | 数据库读取或事务 | 任何对象存储 API、桶策略读取 |
| 私有下载签名 | 相应配置及签名凭据；签名最终允许读取目标对象 | 为生成 URL 先读桶 ACL/CORS/Lifecycle 或独立 canary |
| 单文件上传与完成校验 | 指定桶/前缀的对象写入、必要 HEAD/少量读取 | 创建桶、修改匿名策略、管理其他桶 |
| 分片上传与恢复 | 对象写入、查询本对象的分片、终止本对象的未完成分片 | 无差别列举所有桶、全账号管理权限 |
| 过期对象清理 | 明确目标对象的删除；按实际实现终止未完成分片 | 删除桶或修改整个桶生命周期规则 |
| 文件公开复制/验证 | 读取特定来源、写特定目标、验证该对象，按现有流程清理来源 | 应用自动将原私有桶整体改为公开 |
| 管理员部署桶 | 配置特定桶 CORS、生命周期、公开/私有策略 | 不应把这一身份的高权限凭据交给常驻业务进程 |

S3 风格最小策略通常从限定对象资源的 `GetObject`、`PutObject`、必要 `DeleteObject` 开始；分片按实际使用增加 `AbortMultipartUpload`、`ListMultipartUploadParts`。不能把 API 名称如 CreateMultipartUpload 直接当作所有厂商都存在的独立 IAM action。是否需要桶级列举，只按代码真实使用的列举功能授权；当前 DB 元数据列表不应因此要求 ListAllMyBuckets。[S48] [S49] [S50]

浏览器直传真正需要配置桶 CORS：允许实际前端 Origin 和用到的方法/请求头；分片恢复需要浏览器读到 ETag 时，应暴露 ETag。生命周期终止未完成分片可以作为存储侧清理保障，但它是否配置成功不应要求日常业务凭据能读取/修改桶生命周期。不要用更宽的凭据换取“所有诊断都是绿色”。[S47] [S49]

### 6.2 保留与删除的决策标准

| 保留 | 收缩/移出默认路径 | 暂不引入 |
|---|---|---|
| DB 唯一约束、事务、必要 CAS | 双层各自决定重试的完成态机制 | 新 MQ、分布式事务平台 |
| 外部发送租约/fencing、明确 UNKNOWN | IN_APP 模仿供应商回执模型 | 为 IN_APP 增加回执查询补偿器 |
| 真实对象授权与 PRIVATE 预期 | 全配置启动诊断、必填 canary | 自研完整 IAM 策略解释器 |
| 回执验签、重放/事实冲突校验 | 已接受但未实现的通用编排参数 | 全量实现 SYNC/ESCALATION/多级灾备 |
| 上传票据归属、分片校验、完成幂等 | 每个业务重复的无效 YAML 默认参数 | 动态策略配置平台 |
| 安全删除缓存的路径保护 | 每次正常启动的构建排障流程 | 另一套跨平台进程管理框架 |

“千分之一”本身不是删除保护的依据：概率未经观测不能声称精确为某个数。应比较触发条件、损失和方案成本。阻止一次当前来源被并发删除，只需一个统一对象级互斥规则，值得做；为了可选诊断覆盖所有桶策略形态，却让所有启动和下载受阻，不值得继续扩展。

<a id="diagnosis"></a>

## 7. “发布了，但当前用户消息盒子为空”的只读排查流程

本节用于定位部署现场，不要求先修改 DB。先验证 N-01，再沿持久化事实向下查；不要同时改几十个开关，导致无法确认哪一步真正生效。

### 7.1 浏览器与配置：先判断是否发起了查询

在当前登录用户会话中打开 Network 面板，查看是否请求 `GET /notify/inbox`。没有请求时，优先检查实际构建使用的 `VITE_APP_MESSAGE_ENABLED`、消息盒子初始化及当前登录状态。仓库默认 false 已确认，但 `.env.*.local`、构建流水线环境可能覆盖它，必须以实际产物行为为准。[S01] [S02] [S03]

有请求且返回数据、页面仍为空时，查 store、分类页签和加载异常；请求失败时保留真实错误，不继续显示为普通空列表；请求成功返回空集合时，再查当前用户是否有消息关系。SSE 是否连通不是收件箱 REST 是否有数据的前置证明。

当前用户 ID 应使用登录结果对应的系统 user ID，不要用用户名、client ID、clientPk 或发布者 ID 代替。收件箱接口实际使用 `LoginHelper.getUserId()`。[S07]

### 7.2 数据库核对 SQL

以下仅查询业务数据；`SET` 只设当前连接的会话变量。把两个 0 替换为真实正整数 ID。不要输出 template_params_json、验证码、完整目标地址或渠道 secret。示例按本次 Mapper/实体使用的表关系编写，执行前以部署库迁移版本核对；本次未连接作者数据库执行。[S09] [S11] [S14] [S16] [S20]

```sql
-- 替换为本次公告与当前登录用户的真实 ID。
SET @notice_id = 0;
SET @user_id = 0;

-- A. 是草稿还是已发布/已撤回？保存草稿不产生收件人投递。
SELECT notice_id, lifecycle
FROM notify_notice
WHERE notice_id = @notice_id;

-- B. 查该公告实际生成的通知意图，避免把“公告已发布”和“已投递”混淆。
-- 重新发布可能对应不同 intent；不要只凭标题定位。
SELECT intent_id, app_id, scene_code, biz_type, biz_id, status, create_time
FROM notify_intent
WHERE biz_type = 'NOTICE_PUBLISHED'
  AND biz_id = CAST(@notice_id AS CHAR)
ORDER BY intent_id DESC;

-- C. 当前用户是否进入了该公告的投递集合？是否包含 IN_APP？
SELECT d.intent_id, d.delivery_id, d.user_id, d.channel, d.status
FROM notify_delivery d
JOIN notify_intent i ON i.intent_id = d.intent_id
WHERE i.biz_type = 'NOTICE_PUBLISHED'
  AND i.biz_id = CAST(@notice_id AS CHAR)
  AND d.user_id = @user_id
ORDER BY d.intent_id DESC, d.delivery_id;

-- D. 当前用户对应的 Outbox 在等调度、等回执还是已经关闭？
-- 仅看错误码，避免直接导出可能含敏感上下文的错误文本。
SELECT o.intent_id, o.delivery_id, o.outbox_id, o.status,
       o.available_at, o.lease_until, o.last_error_code
FROM notify_outbox o
JOIN notify_delivery d ON d.delivery_id = o.delivery_id
JOIN notify_intent i ON i.intent_id = d.intent_id
WHERE i.biz_type = 'NOTICE_PUBLISHED'
  AND i.biz_id = CAST(@notice_id AS CHAR)
  AND d.user_id = @user_id
ORDER BY o.outbox_id DESC;

-- E. 共享消息与当前用户的关系是否都已经存在？
-- 当前 IN_APP 实现使用 intent_id 作为 message_id。
SELECT i.intent_id, i.status AS intent_status,
       m.message_id, mr.user_id AS inbox_user_id
FROM notify_intent i
LEFT JOIN notify_message m ON m.message_id = i.intent_id
LEFT JOIN notify_message_recipient mr
       ON mr.message_id = m.message_id AND mr.user_id = @user_id
WHERE i.biz_type = 'NOTICE_PUBLISHED'
  AND i.biz_id = CAST(@notice_id AS CHAR)
ORDER BY i.intent_id DESC;

-- F. 评估 N-03 的约束修复前，先检查已有唯一索引，不能重复盲目建索引。
SHOW INDEX FROM notify_message_recipient;
```

| 观察结果 | 更可能的层次 | 正确处理 |
|---|---|---|
| 浏览器没有 inbox 请求 | N-01 / 登录初始化 | 先修前端调用，不往 DB 补消息 |
| 当前用户没有 IN_APP delivery | 接收人选择或未选站内信渠道 | 核对发布时快照、用户有效状态，不认为发布者天然应收到 |
| 有 PENDING delivery，Outbox 一直 READY | Worker/唤醒/数据库领取 | 检查调度运行及领取错误；不先清空租约表 |
| IN_APP UNKNOWN + WAITING_RECEIPT | N-03，或本地合同异常 | 修幂等与分类后，受控重放确认未完成的项 |
| message 有，用户关系没有 | 接收人链路/幂等写入中断 | 核对 delivery，再用修复后的幂等流程重做 |
| 关系有，API 返回空 | 用户 ID/部署库/读取边界 | 核实请求真实身份和应用所连库，而非直接放宽权限 |
| API 有数据，盒子为空 | 前端 store/页签/渲染 | 单独测列表回填、分类与错误状态 |

**存量修复原则。** 在 N-01 下不要重复发布：前端恢复读取即可显示已有关系。IN_APP 的存量缺关系可以按明确 intent/delivery 逐批通过修复后的幂等服务恢复，并保留审计；MAIL/SMS 的 UNKNOWN 不能用同一批 SQL 全部重置后重发，因为有些请求可能已被厂商接受。先做只读清单，再按渠道和错误事实处理。

<a id="delivery-plan"></a>

## 8. 分批实施顺序与回滚边界

以下是建议的独立变更单，不是已经完成的代码修改。每批应能在当前仓库测试基座上独立验证，避免一次“架构优化 PR”混入全部事项。

| 批次 | 变更范围 | 完成标志 | 回滚/发布注意 |
|---|---|---|---|
| A：安全处置 | S-01 凭据轮换与取消跟踪；S-02 公共 CORS 安全默认 | 新凭据连接正常，可信前端正常访问，恶意 Origin 不获许可 | 不允许回滚到已披露凭据；CORS 改动先核对真实业务 Origin |
| B：恢复站内信可见性 | N-01；N-08 中重复正文/加载状态等无数据库变更项 | 关闭实时服务仍能读自己的站内信 | 单独发前端即可验证，不需要重发公告 |
| C：通知基本合同 | N-02、N-03、N-04、N-05、N-06 | 短信真实适配链能通，本地错误可收敛，重试不重放旧失败，过期不发送 | 唯一约束先查重复；只受控修复已确认的存量任务；不要全量清 Redis 幂等数据 |
| D：取消与调用方闭环 | N-07、N-09；N-08 分页接口与前端配套 | 撤回停止未发送项，普通用户可读快照，邮件正文/附件完整 | API/前端同版或兼容切换；保留旧发布快照，不以物理删历史代替撤回 |
| E：OSS 基本访问解耦 | O-01、O-02、O-04 | 无 canary 仍可正常操作合法对象，诊断不要求高权限，列表不被旧响应覆盖 | 移除门禁但保留用户授权和预期访问类型；诊断结果由强制阻断改为有界报告 |
| F：OSS 管理操作一致性 | O-03 | 恢复/清理的竞争测试通过，当前来源不被删除 | 先部署互斥规则再恢复相关管理操作；不自动批量清理旧迁移来源 |
| G：去掉日常复杂度 | D-01、D-02；N-10 按测量需要实施 | 新环境步骤减少，普通启动不做深度修复，配置单一来源 | 先保留可选 repair 路径；删配置前确认旧部署有兼容默认与迁移说明 |

不要把 SSO 协议迁移混进以上批次。它会改变认证边界、登录和部署验收，本次已明确暂不处理其实现。

### 8.1 必须补的测试：用现有基座，不另建平台

`NotifyAtomicResultIntegrationTest` 已包含真实数据库、动态事务代理、Redis 外部副作用与提交故障测试，且通过系统属性显式启用。它证明项目已有集成测试基础；其中的 NotifyClient mock 则说明这些 IN_APP 事务测试不能替代真实短信合同测试。[S61]

下表是建议新增/扩展的测试场景名称，不表示仓库已经存在这些测试，亦不表示本次已经运行通过。

| 测试场景 | 真实执行到的边界 | 核心断言 |
|---|---|---|
| InboxWithoutRealtime | 前端 store + REST 假响应 | flag=false 仍读取，身份切换不串用户 |
| NoticePublishToCurrentUser | 发布用例 + 真实 MySQL + Worker + inbox 查询 | 当前有效接收人有且仅有一条关系 |
| ConcurrentRecipientsShareMessage | 两连接处理同 intent 不同 delivery | 不因共享消息重复键丢收件人 |
| SmsCrossLayerContract | runtime + common dispatcher + 假厂商 | 不被空快照挡住，参数映射正确 |
| DefiniteFailureThenRetry | 真实下层幂等与上层 Outbox | 第二次真实调用 Provider，而非重放缓存失败 |
| UnknownExternalOutcome | 外部响应丢失与重复领取 | 不盲目重复发送，不假装已送达 |
| RetryTargetAndNoop | retry Controller/UseCase + DB | 指定目标、路径一致、零任务状态真实 |
| ExpiredCaptchaNotDispatched | 可控制时钟 + Worker | 截止后 Provider 调用为零 |
| RetractPendingNotice | 发布后暂停 Worker，再撤回 | 后续无新投递，快照审计保留 |
| InboxHistoryBeyondLimit | 超过 500 条用户关系 | 后续历史可读，未读总数正确 |
| MailWithAndWithoutAttachment | 统一命令 + 假邮件适配器/OSS 端口 | 无附件零 OSS 调用；有附件确实传递 |
| OptionalOssUnavailable | 应用最小配置 + 不可用存储替身 | 非存储业务可运行，无启动全量诊断依赖 |
| LeastPrivilegeOssDiagnostic | Policy/ACL 403 + 可读对象替身 | 不把未知当 false，不要求扩大权限 |
| RestoreRacesSourceCleanup | 两连接/线程 + 可阻塞对象存储替身 | 当前存储指针指向的对象不会被清理 |
| OssLatestRequestWins | 两个反序完成的前端请求 | rows/total/preview 都属于最后一次查询 |
| ProductionCorsBoundary | 过滤器 HTTP 测试 | 不反射任意 Origin 携带 credentials |

测试供应商使用假适配器，避免真实短信计费；数据库并发使用现有隔离测试库，不能对开发共享库或生产库执行故障触发器。测试的“真实”应指真正经过需要验证的合同层，不等于所有外部服务都必须在线。

现有租约过期、旧 owner 不得完成、提交前后故障、回执重复与事实冲突的测试必须保留。修复正常链路不应以牺牲这些保护为代价。若做 N-10 聚合优化，尤其要保留“attempt、delivery、outbox 与聚合可解释性”的回归约束。

### 8.2 完成定义

这轮优化完成，不应以“删除了多少行代码”“新增了多少测试”衡量，而应同时满足：默认环境当前用户能收到自己的公告；SMS/MAIL/附件与站内信合同一致；状态能够收敛、不伪造排队/送达；没有使用存储的功能不为 MinIO 诊断付出启动和权限成本；日常启动无需理解构建修复内部细节；安全边界没有因为简化而放宽。

<a id="sso-boundary"></a>

## 9. SSO：仅记录后续验收合同，不审计本轮实现

当前 change 的 ADR 已明确“accepted 仅代表用户确定的设计，不代表代码已实现”；其中统一内部/外部 OIDC 协议、复用现有账户能力的方向与作者要求一致。本报告没有把 OIDC 尚未完成、旧 SSO 仍存在等事实报成当前缺陷。[S62]

需要将作者本次强调的边界明确写入后续验收：**内置授权服务器是可选子模块；业务 App 是标准客户端，不因授权服务器恰好同进程部署就走内部密码换票、私有直登或共享会话捷径。** 内置 SSO 关闭时，业务 App 仍能使用外部 IdP；打开时，只是指向本项目提供的 issuer，交互仍走相同的授权码流程和适用的 PKCE、重定向与令牌校验。

客户端协议配置与内置授权服务器自身配置应分离。public/confidential 按客户端保密能力决定，而不是按“自己开发/外部开发”决定；本地业务账户/权限与外部身份映射保持清楚，不能仅凭邮箱相同就无条件合并身份。后续拆分时，不应要求业务服务直接读取 SSO 的密码库或内部 session 存储。

也要澄清一个协议术语：**OAuth/OIDC 不定义统一的最终用户注册 API。** 可以统一由 IdP 承担注册页面与账户验证，然后回到标准登录授权流程；OIDC 的客户端动态注册不是用户注册。不能为了追求“注册也统一”再自造一个要求所有外部 IdP 实现的 WTA 私有注册协议。OIDC 也不要求 access token 必须是 JWT，不能仅因现有访问令牌是 opaque 就判定不符合标准。

后续最小验收矩阵就是：内置 SSO 关 + 外部 IdP；内置 SSO 开 + 自有 App；独立部署的同一 SSO + 同一 App。比较标准客户端的配置差异和实际授权流程，而不是逐步补出两套登录 SDK。此处不展开实现和供应商选型。

协议参考：[OAuth 2.0 授权码流程（RFC 6749）](https://www.rfc-editor.org/rfc/rfc6749)、[PKCE（RFC 7636）](https://www.rfc-editor.org/rfc/rfc7636)、[OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)。这些是协议概念参考，不是本次在线符合性测试或最新依赖版本验证。

<a id="coverage"></a>

## 10. 审查覆盖、证据等级与未确认事项

### 10.1 本次实际覆盖

| 范围 | 覆盖方式 | 可以支持的结论 |
|---|---|---|
| 基线与最近 OSS 变更 | main 固定 SHA、近期提交信息、基线与父提交 diff | 本次版本仍存在/已改善哪些路径；不是全部历史提交逐一审计 |
| notify 管理与执行 | 发布用例、命令、接收人解析、DAO、Worker、租约、调度、结果、回执、账号/场景等关键实现 | 本报告列出的跨层状态与合同问题 |
| notify 前端 | admin-web env、消息初始化、消息盒子、收件箱、公告管理页面 | 默认开关根因、读取及交互问题；未作真实浏览器视觉验收 |
| notify 调用方 | 验证码、演示邮件、测试发送及其他调用点定位 | 已核查调用方的参数合同；未逐一跑完所有业务模块流程 |
| OSS | 元数据/生命周期、上传入口、客户端/工厂、readiness、迁移/恢复/清理、管理页面 | 依赖与权限问题、诊断误判、相关竞争窗口 |
| 配置与启动 | 公共/local/prod 配置相关段、CORS 过滤器、完整 start-dev 脚本 | 默认配置和启动流程问题；未验证所有平台及完整发布脚本 |
| 测试 | 既有通知集成测试源码及相关调用定位 | 测试基础与本轮应补场景；不能声称测试运行通过 |
| SSO | pending change ADR 与作者本次要求 | 仅后续架构验收边界，不评价当前实现完整性 |

本次没有拿到作者机器的未提交工作树、部署日志、生产环境变量、实际登录响应、实际 DB 行或 MinIO 策略。因此，默认开关路径已经足以解释一种确定的空消息盒子情况，但不能宣称它一定是作者现场唯一根因；现场可以直接用第 7 节验证。

### 10.2 明确排除的误报和扩大化结论

没有因为主键/唯一约束存在，就误以为“先查后插”不存在竞态；也没有反过来因为 ClaimService 自身没有事务注解，就忽视外层 UseCase 的事务而判定领取完全无事务。外部 I/O 位于短结果事务之外有其必要性，但 IN_APP 本地写入应单独判断。[S13] [S19] [S61]

没有把当前 DB-only 的对象元数据接口再报为“不必要访问 OSS”；没有把辅助 CORS/Lifecycle 告警说成必然启动失败；没有声称 SDK 初始化会自动创建桶；没有把 UI 已明说保留公开副本的 unpublish 行为另算作秘密暴露漏洞。[S40] [S41] [S47] [S50] [S54]

没有把所有通知都说成失效：短信的空快照是统一短信路径问题，演示邮件的空 path 是已定位调用方问题，持久化站内信和实时消息开关是另一层问题。也没有把未运行的代码检查写成 E2E 已复现。

### 10.3 综合建议

优先减少三类复杂性：**同一事实多份配置、同一结果多套状态机、可选能力成为核心功能前置条件。** 优先补齐三类简单闭环：**用户操作得到对应结果、队列状态最终可解释、已有数据有稳定读取入口。**

项目当前已经有足够的模块边界和可靠性基础。最有价值的工作不是增加功能，而是让这些边界只承担自己的职责：收件箱负责持久消息，实时服务负责提醒；Outbox 负责统一重试，Provider 负责物理发送；对象存储操作负责真实对象访问，诊断负责报告；启动负责运行，repair 负责排障。按这个方向修复，比再叠一层统一平台更稳固。

<a id="sources"></a>

## 11. 固定提交源码索引

下面列出本文使用的证据文件。链接定位到固定提交的完整文件；正文提供具体方法/配置键，便于搜索。部分大文件只沿相关方法分段核查，文件列入索引不代表其每一行均已完成审计。

- **[S01]** `frontend/apps/admin-web/.env.development`
- **[S02]** `frontend/apps/admin-web/.env.production`
- **[S03]** `frontend/apps/admin-web/src/utils/push.ts`
- **[S04]** `frontend/apps/admin-web/src/layout/components/notice/index.vue`
- **[S05]** `frontend/packages/web-domains/notify/src/InboxPage.vue`
- **[S06]** `frontend/packages/web-domains/notify/src/NoticePage.vue`
- **[S07]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java`
- **[S08]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyInboxService.java`
- **[S09]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java`
- **[S10]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java`
- **[S11]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java`
- **[S12]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java`
- **[S13]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationApplicationUseCase.java`
- **[S14]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java`
- **[S15]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java`
- **[S16]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/InAppNotificationService.java`
- **[S17]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java`
- **[S18]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/worker/NotifyOutboxWorker.java`
- **[S19]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyOutboxClaimService.java`
- **[S20]** `backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml`
- **[S21]** `backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java`
- **[S22]** `backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/NotifyTemplateContent.java`
- **[S23]** `backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/NotifyIdempotencyCoordinator.java`
- **[S24]** `backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/RedisNotifyIdempotencyStore.java`
- **[S25]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySendPlanner.java`
- **[S26]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySceneCatalog.java`
- **[S27]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java`
- **[S28]** `backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java`
- **[S29]** `backend/wta-api/src/main/java/org/namewta/notify/api/NotificationCommand.java`
- **[S30]** `backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java`
- **[S31]** `backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java`
- **[S32]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/policy/NotificationAggregatePolicy.java`
- **[S33]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java`
- **[S34]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyConfigService.java`
- **[S36]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/anonymous/ProviderCallbackController.java`
- **[S37]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java`
- **[S38]** `backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java`
- **[S40]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssServiceImpl.java`
- **[S41]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/OssLifecycleManager.java`
- **[S42]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessService.java`
- **[S43]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessRegistry.java`
- **[S44]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessHealthIndicator.java`
- **[S45]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessProperties.java`
- **[S46]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/runner/SystemApplicationRunner.java`
- **[S47]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/OssUploadDiagnostics.java`
- **[S48]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/OssUploadService.java`
- **[S49]** `backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java`
- **[S50]** `backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/DefaultOssClientImpl.java`
- **[S51]** `backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/factory/OssFactory.java`
- **[S52]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/OssStorageMigrationService.java`
- **[S53]** `backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/MybatisOssMigrationStore.java`
- **[S54]** `frontend/packages/web-domains/system/src/oss/OssPage.vue`
- **[S55]** `backend/wta-admin/src/main/resources/application.yml`
- **[S56]** `backend/wta-admin/src/main/resources/application-local.yml`
- **[S57]** `backend/wta-admin/src/main/resources/application-prod.yml`
- **[S58]** `backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java`
- **[S59]** `backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java`
- **[S60]** `scripts/start-dev.sh`
- **[S61]** `backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java`
- **[S62]** `speculo/.speculo/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/ADR.md`

[S01]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/apps/admin-web/.env.development
[S02]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/apps/admin-web/.env.production
[S03]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/apps/admin-web/src/utils/push.ts
[S04]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/apps/admin-web/src/layout/components/notice/index.vue
[S05]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/packages/web-domains/notify/src/InboxPage.vue
[S06]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/packages/web-domains/notify/src/NoticePage.vue
[S07]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotifyInboxController.java
[S08]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyInboxService.java
[S09]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/dao/NotifyNotificationDao.java
[S10]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticeService.java
[S11]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyNoticePublisherService.java
[S12]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotifyNoticeUseCase.java
[S13]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/NotificationApplicationUseCase.java
[S14]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotificationApplicationRuntimeService.java
[S15]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/DispatchNotificationService.java
[S16]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/InAppNotificationService.java
[S17]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyDispatchResultService.java
[S18]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/adapter/worker/NotifyOutboxWorker.java
[S19]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/NotifyOutboxClaimService.java
[S20]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/resources/mapper/notify/NotifyOutboxMapper.xml
[S21]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/core/NotifyDispatcher.java
[S22]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/model/NotifyTemplateContent.java
[S23]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/NotifyIdempotencyCoordinator.java
[S24]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-notify/src/main/java/org/namewta/common/notify/idempotency/RedisNotifyIdempotencyStore.java
[S25]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySendPlanner.java
[S26]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/support/NotifySceneCatalog.java
[S27]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/admin/NotificationController.java
[S28]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-api/src/main/java/org/namewta/notify/api/NotificationRetryCommand.java
[S29]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-api/src/main/java/org/namewta/notify/api/NotificationCommand.java
[S30]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-admin/src/main/java/org/namewta/web/controller/CaptchaController.java
[S31]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-demo/src/main/java/org/namewta/demo/controller/MailSendController.java
[S32]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/domain/policy/NotificationAggregatePolicy.java
[S33]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java
[S34]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/NotifyConfigService.java
[S36]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/controller/anonymous/ProviderCallbackController.java
[S37]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/usecase/ProviderCallbackUseCase.java
[S38]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-notify/src/main/java/org/namewta/notify/service/runtime/ProviderCallbackService.java
[S40]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysOssServiceImpl.java
[S41]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/service/OssLifecycleManager.java
[S42]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessService.java
[S43]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessRegistry.java
[S44]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessHealthIndicator.java
[S45]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/readiness/OssStorageReadinessProperties.java
[S46]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/runner/SystemApplicationRunner.java
[S47]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/OssUploadDiagnostics.java
[S48]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/upload/OssUploadService.java
[S49]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/AbstractOssClientImpl.java
[S50]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/client/DefaultOssClientImpl.java
[S51]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-oss/src/main/java/org/namewta/common/oss/factory/OssFactory.java
[S52]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/OssStorageMigrationService.java
[S53]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-modules/wta-system/src/main/java/org/namewta/system/oss/migration/MybatisOssMigrationStore.java
[S54]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/frontend/packages/web-domains/system/src/oss/OssPage.vue
[S55]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-admin/src/main/resources/application.yml
[S56]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-admin/src/main/resources/application-local.yml
[S57]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-admin/src/main/resources/application-prod.yml
[S58]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/ResourcesConfig.java
[S59]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-common/wta-common-web/src/main/java/org/namewta/common/web/config/properties/CorsProperties.java
[S60]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/scripts/start-dev.sh
[S61]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/backend/wta-admin/src/test/java/org/namewta/test/notify/NotifyAtomicResultIntegrationTest.java
[S62]: https://github.com/NAMEWTA/WTA-plus/blob/64b4ea70b9be34ff8d3c63027a922cea884a84de/speculo/.speculo/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/ADR.md

---

**报告结束。所有实施方案和回归验收项均为建议，未作为本次已执行修改或已通过测试陈述。**
