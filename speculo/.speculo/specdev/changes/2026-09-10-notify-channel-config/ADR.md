# Change Architecture Decisions

## ADR-001: 本期建设通知渠道配置运维面

**Status:** accepted
**Source:** LOG-002 / USER-DECISION:2026-09-10 Q1=A
**Supersedes:** none

### Context
仓库已有通知控制面（公告、收件箱、监控、Outbox），但邮件/短信凭据仍在 YAML，场景正文写在调用方。归档 `2026-08-24-notify-eli5` 还设计过 `notify_app`、KEK 热发布、不可变模板版本和路由故障切换，那些能力没有落地。若把本期当成完整通知平台，会远超出「可配置发件人、供应商、模板码和邮件文案」的请求。

### Decision
本期只建设通知中心下的邮件/短信配置运维面：渠道账号管理 + 场景文案/供应商模板绑定。不复活 `notify_app`、KEK 加密热发布、路由灰度/故障切换、不可变模板版本全集。

### Trade-off
完整平台能一次补齐公司级治理，但事故半径过大且与当前请求不对齐。只做账号或只做文案会把同一条发送链路拆成两次互相卡住的改造。运维面较小，换取可落地的配置闭环。

### Consequences
后续路由、模板、密钥方案必须服务这个较窄范围，不能把归档 ADR 自动当作本 change 合同。永久 ADR-0006（common-notify 薄契约）仍然有效。

## ADR-002: ruoyi-notify 拥有配置控制面

**Status:** accepted
**Source:** LOG-005 / USER-DECISION:2026-09-10 Q4=A
**Supersedes:** none

### Context
OSS 配置在 `ruoyi-system` 的 classic CRUD 中。通知中心已经是 `ruoyi-notify` 域。永久 ADR-0006 禁止把模板中心放进 `ruoyi-common-notify`。

### Decision
`ruoyi-notify`（layered）拥有配置表、管理 API、权限菜单和前端页面。`ruoyi-common-mail` / `ruoyi-common-sms` 只增加运行时读取与刷新 SPI。`ruoyi-common-notify` 不建模板中心，不拥有账号表。

### Trade-off
拷贝 OSS 放到 system 改动表面更少，但会形成第二套通知控制面。把账号放进 common-mail/sms 会让公共模块依赖管理实体。多一层 SPI 换取依赖方向不变。

### Consequences
前端落在 `packages/domains/notify` 与 `packages/web-domains/notify`。system 不新增邮件/短信配置控制器。Dispatcher 仍通过数据面 Adapter 发送，由控制面在调用前解析账号与内容。

## ADR-003: 数据库是邮件短信账号的唯一运行时权威

**Status:** accepted
**Source:** LOG-004, LOG-009, LOG-010 / USER-DECISION:2026-09-10 Q3=A
**Supersedes:** none

### Context
当前 `mail.*` 与 `sms.blends` 写在 `application-dev.yml` / `application-prod.yml`。Nacos live 参与者不包含 mail/sms。用户要求完成后 YAML 不再留存这些发件人/供应商内容。

### Decision
邮件与短信账号的运行时权威是数据库，经缓存或热刷新生效。本 change 完成后，YAML 删除发件人与供应商账号内容，不再保留占位并行权威。占位密钥不得自动启用。实现必须同步 `<Path>.agents/skills/</Path>` 中相关 Skill，并补齐中文注释。

### Trade-off
保留 YAML 占位便于本地空库启动，但会造成双权威。Nacos 可热更新却扩大密钥面，且当前不是 mail/sms 的 live 参与者。以库为准增加迁移，换取与启停/路由同一数据源。

### Consequences
`MailConfig` 不再用 `mail.enabled` 决定是否装配发送能力；Adapter 必须能按控制面解析出的账号发送。SMS4J 不能继续以 yaml blends 为账号源。非账号基础设施留在 YAML，见 ADR-009。

### Verification / Migration
验收须证明：YAML 无账号段时发送仍只读库；库中无启用绑定则失败关闭；Skill 与注释描述与代码一致。

## ADR-004: 渠道账号独立启停，发送必须按路由选账号

**Status:** accepted
**Source:** LOG-006, LOG-007, LOG-008 / USER-DECISION:2026-09-10 Q5=B
**Supersedes:** none

### Context
OSS 配置用恰好一个默认桶，不能关掉当前默认。邮件发件人、短信供应商与供应商模板码是发送正确性的关键，不能因为「有一个启用账号」就隐式选用。用户要求每个账号独立启停，可同时启用多个，发送必须按路由选择对应账号。

### Decision
渠道账号独立启用或停用，允许同时启用多个。发送必须按路由解析到明确账号：短信同时使用该账号下的模板码，邮件使用该账号的发件人。禁止隐式默认账号，禁止把「任意启用账号」当作回退。本期路由不是健康度故障切换或灰度。不设独立的渠道总开关。

### Trade-off
OSS 式唯一默认实现更少，但会发错签名、模板码或发件人。完整故障切换能提高表面成功率，却超出本期运维面并可能重复计费。强制路由增加绑定配置，换取可解释的发送身份。

### Consequences
控制面必须在 Provider 调用前写入解析后的 `providerKey` / 发件账号。普通业务调用仍不应靠硬编码供应商来「碰巧发对」。绑定落点见 ADR-006。

## ADR-005: 逻辑场景由代码播种，变量名随模板锁定

**Status:** accepted
**Source:** LOG-011, LOG-016, LOG-017 / USER-DECISION:2026-09-10 round2 Q1=A
**Supersedes:** none

### Context
用户要求邮件可改发送内容，但变量不能改。补充原因：短信模板或代码模板一旦确定，变量名也随之确定，并随编码进入程序。若允许管理端增删场景或改变量名，调用方 `templateParams` 与供应商审核模板会立即失配。

### Decision
逻辑场景由代码播种。管理员可以修改邮件主题/正文、绑定渠道账号和短信供应商模板码，不能增删场景，不能改变量名。短信页不提供自由正文编辑器，只绑定供应商模板码并只读展示变量。邮件变量以 `${name}` 只读插入，保存时校验必填变量名未被改写。

### Trade-off
完整 CRUD 模板更灵活，但会把未编码的变量送进程序。不可变版本发布链更可审计，却超出本期运维面。代码拥有场景与变量契约，换取发送合同稳定。

### Consequences
首期必须播种现有生产场景，不能做一个空白模板市场。调用方继续按代码里的变量名传参。管理端改文案不得改 schema。

## ADR-006: 场景在渠道上绑定唯一账号，调用方不传供应商

**Status:** accepted
**Source:** LOG-012, LOG-018 / USER-DECISION:2026-09-10 round2 Q2=A
**Supersedes:** none

### Context
账号可同时启用多个，发送又不能隐式默认。短信模板码属于具体厂商账号，邮件发件人属于具体 SMTP 账号。独立路由表会把同一事实写两遍；调用方传 providerKey 会把供应商硬编码退回业务。

### Decision
逻辑场景在每个渠道上绑定唯一渠道账号；短信再绑定该账号下的供应商模板码。普通 `NotificationCommand` 不传 `providerKey`。绑定缺失或账号停用时，该渠道失败关闭，不改选其他启用账号；同一通知的其他渠道仍按各自绑定发送。

### Trade-off
独立路由表可让同一文案换发件人而不改模板行，但运营要维护两份绑定。调用方指定账号最直接，却破坏平台选路。场景绑定增加配置项，换取供应商、模板码、发件人一致。

### Consequences
控制面按 scene + channel 解析账号后再调用数据面，并设置解析得到的 providerKey。测试发送的模板级路径必须走同一绑定。

## ADR-007: 渠道密钥跟随 OSS 的明文存储与不回显

**Status:** accepted
**Source:** LOG-013 / USER-DECISION:2026-09-10 round2 Q3=A
**Supersedes:** none

### Context
YAML 将不再保存账号密钥。归档方案要求 KEK/AES-GCM，OSS 现网则是库内明文、VO 不回显、空白保持原值。本期范围是运维面，不是密钥平台。

### Decision
SMTP 密码与短信 AK/SK 库内明文存储。管理接口不回显 secret；编辑时空白表示保持原值；写操作不把请求体写入操作日志。HTTP、业务日志和异常不得包含 secret。

### Trade-off
加密存储泄露面更小，但要 KEK 轮换、发布快照和安全测试连接，超出本期。明文跟 OSS 一致，换取可实现的最小合同；这是对归档加密方案的有意收窄。

### Consequences
前端不得要求用户每次编辑都重填密钥。后续若做 KEK，另开 change，不在本期假装已加密。

## ADR-008: 管理页提供账号级与模板级测试发送

**Status:** accepted
**Source:** LOG-014 / USER-DECISION:2026-09-10 round2 Q4=A
**Supersedes:** none

### Context
没有隐式默认账号后，启停是否可用不能靠生产流量验证。OSS 有后台 readiness，邮件/短信没有等价诊断。

### Decision
通知配置提供两类测试发送：账号级验证 SMTP 或短信厂商凭据；模板级用样例变量走该场景的真实渠道绑定。测试发送使用独立权限，结果进入投递监控。

### Trade-off
只测账号无法证明场景绑定正确；不提供测试按钮会让运维在生产上试错。双测试增加接口和频控，换取可安全启停。

### Consequences
测试发送不得绕过账号停用、绑定缺失和变量契约。样例变量只能使用该场景已声明的变量名。

## ADR-009: YAML 只删除账号段，保留 SMS4J 非账号项

**Status:** superseded
**Source:** LOG-015 / USER-DECISION:2026-09-10 round2 Q6=B
**Supersedes:** none

### Context
用户要求完成后 YAML 不留发件人/供应商。SMS4J 还有线程池、restricted、minute-max 等进程级设置。把它们做成配置页会变成限流产品。

### Decision
删除 YAML 中的 `mail` 账号字段与 `sms.blends`。保留 SMS4J 线程池、restricted 等非账号基础设施。频控不进入通知配置页。

### Trade-off
删掉整个 `sms:` 最干净，但会失去现有拦截阈值。频控进管理页更灵活，却扩大本期范围。账号进库、基础设施留 YAML，保持单一账号权威。

### Consequences
`sms.config-type` 必须改为从接口/数据库读账号，而不是 yaml blends。文档和 Skill 须写明：账号在通知配置，线程池/拦截仍在 YAML。已被 ADR-012 替代「频控不进入通知配置页」；YAML 删除账号段与线程池保留仍然有效。

## ADR-010: MAIL/SMS 发送内容以场景模板为权威

**Status:** accepted
**Source:** LOG-019 / USER-DECISION:2026-09-10 round3 Q2=A
**Supersedes:** none

### Context
今天控制面把调用方 `templateParams.title/content` 做成快照再当纯文本发出。若配置页改了邮件正文或短信模板码，投递仍会发出 Java 硬编码字符串。

### Decision
MAIL/SMS 发送时，控制面按逻辑场景读取已配置模板，用调用方 `templateParams` 渲染邮件或绑定短信供应商模板。渲染结果写入 intent 快照供审计。调用方 `title`/`content` 不是发送权威。IN_APP 本期仍用代码内文案。

### Trade-off
继续以调用方正文为准实现更少，但配置页没有意义。允许调用方覆盖会让变量契约失效。模板权威增加渲染步骤，换取配置闭环。

### Consequences
`DispatchNotificationService` 必须对 SMS 使用 `NotifyTemplateContent`，对 MAIL 使用渲染后的主题/正文。验证码等调用方应只传变量。

## ADR-011: 验证码补 templateCode；SYNC 不同步改造

**Status:** accepted
**Source:** LOG-022 / USER-DECISION:2026-09-10 round3 Q1=A
**Supersedes:** none

### Context
Captcha 提交空 `templateCode`，控制面会拒绝。`NotificationMode.SYNC` 只入队，请求线程不调用供应商。前者会让验证码场景无法使用配置页。

### Decision
本期给验证码补上稳定 `templateCode`（与 `auth-captcha` 场景一致），使其走模板与路由。不把 SYNC 改成请求线程内调用供应商；Outbox 异步投递保持现状，真正同步发送另开 change。

### Trade-off
只加配置页不修 templateCode，验证码配了也发不成。本期改 SYNC 能让验证码接口等到供应商回执，但会显著扩大投递语义与测试矩阵。

### Consequences
验证码接口在 submit 成功后仍可能尚未真正送达供应商。文档不得把本期 SYNC 字段解释为已同步发送。

## ADR-012: 渠道账号与短信模板配置每分钟发送配额

**Status:** accepted
**Source:** LOG-023 / USER-DECISION:2026-09-10 round3 附加
**Supersedes:** ADR-009

### Context
用户要求短信和邮件发送按账户级限额，短信模板还要更细的每分钟条数。ADR-009 曾规定频控不进通知配置页，并把 YAML `minute-max` 当作可保留的基础设施。那些 YAML 数字实际是「单手机号防刷」，不是厂商账号吞吐。

### Decision
在通知配置中管理发送配额：每个邮件/短信渠道账号有每分钟发送上限；短信逻辑场景绑定可设置更细的每分钟条数，且不得超过所属账号上限。这替代 ADR-009「频控不进入通知配置页」。YAML 仍删除账号段，SMS4J 线程池仍可留在 YAML。单手机号拦截与配额用尽行为由后续节点决定。

### Trade-off
只用全局 YAML 限额实现更少，但多账号会互相抢同一计数器，也无法按模板保护验证码配额。完整限流产品（延期队列、突发桶）过重。账号+可选模板上限换取可解释的供应商配额。

### Consequences
控制面在调用供应商前检查配额；超限不得改选其他账号。测试发送是否占用配额须在实现时明确并写入验收。Skill 须区分账号吞吐与号码防刷。收件人拦截已改到场景绑定，见 ADR-014。

## ADR-013: 本期清除 MAIL/SMS 硬编码正文，变量占用位置在配置页

**Status:** accepted
**Source:** LOG-024, LOG-027 / USER-DECISION:2026-09-10 round4 Q1=B
**Supersedes:** none

### Context
变量名与发送路由属于代码合同。短信/邮件句子写在 Java 里则无法热配。用户要求本期一次性去掉这些硬编码正文，并让变量占用位置出现在配置页上。

### Decision
本期删除生产 MAIL/SMS 调用方中的硬编码正文。调用方只提交 `sceneCode`、`templateCode` 和变量值。配置页热配邮件主题/正文，变量以只读 token 插入正文，位置可随文案调整，名称不可改；保存时校验必填变量都出现。短信页不提供自由正文，用参数映射表展示逻辑变量对应的供应商模板参数位置。IN_APP 本期不改正文。demo 的 MAIL/SMS 直发同步改为只传变量，不得保留第二套硬编码句子。

### Trade-off
调用方分批清理实现更小，但会留下两套正文来源。一次性清扫扩大改动面，换取配置页是唯一文案权威。

### Consequences
Captcha、换绑、企业转移等必须改为只传变量。公告与工作流若仍有运行时正文，只能作为已声明变量填入包装模板，见未关闭的 D-033。监控快照保存渲染后的邮件或供应商模板码+参数。

## ADR-014: 收件人拦截按场景渠道绑定配置

**Status:** accepted
**Source:** LOG-025, LOG-028 / USER-DECISION:2026-09-10 round4 Q2=B
**Supersedes:** none

### Context
不同短信/邮件对应不同应用场景，验证码与公告不能共用一套「每号每分钟 1 条」。YAML 全局 `restricted` / `minute-max` / `account-max` 无法表达这种差异。用户要求号码级配置按模板区分。

### Decision
每个逻辑场景的 MAIL 与 SMS 渠道绑定各自拥有 `restricted`、`minute-max`、`account-max`。SMS 按手机号计数，MAIL 按邮箱计数。YAML 删除这些运行时项。账号吞吐限额（ADR-012）仍然存在，与收件人拦截同时生效。

### Trade-off
全局拦截最简单，但会让低频公告和高频验证码互相伤害。按场景配置增加字段，换取场景隔离。

### Consequences
控制面在供应商调用前同时检查：账号每分钟吞吐、模板每分钟吞吐、该收件人在该模板下的分钟/日限额。任一失败则该渠道失败关闭。Skill 必须写明三套计数器的含义。

## ADR-015: 任何配额用尽都立即失败关闭该渠道

**Status:** accepted
**Source:** LOG-026 / USER-DECISION:2026-09-10 round4 Q3=A
**Supersedes:** none

### Context
账号吞吐、模板吞吐和收件人拦截都可能用尽。延期到下一分钟会让验证码静默变慢，也会做成第二套队列。

### Decision
上述任一配额用尽时，该渠道投递立即失败关闭，写入明确失败原因，不改选其他账号，不把任务延期到下一时间窗。同一通知的其他渠道仍按各自绑定继续。

### Trade-off
延期能提高最终发出率，但不可解释且超出本期。失败关闭让监控和调用方可观测。

### Consequences
验证码在限额用尽时会发送失败。测试发送走同一套限额语义。不得把超限当成成功入队。

## ADR-016: 公告与工作流使用包装模板

**Status:** accepted
**Source:** LOG-029 / USER-DECISION:2026-09-10 round5 Q1=A
**Supersedes:** none

### Context
验证码、换绑、企业转移的句子来自 Java，可以整段热配。公告标题正文来自公告编辑器，工作流 subject/message 来自流程运行时。若把这两类也改成静态句子，公告编辑器和流程通知会失效；若允许它们绕过模板直接发送，又会留下第二套正文权威。

### Decision
`notice-published` 与 `workflow-task` 的 MAIL/SMS 使用包装模板。配置页编辑外壳文案，必须保留代码声明的变量（至少包括 `${title}`、`${content}`，以及已声明的 `${path}` 等）。运行时由公告快照或流程参数填入变量值。外壳可热配，变量名不能改。调用方只传变量，不把 title/content 当作发送权威。

### Trade-off
业务正文直发实现更少，但配置页无法改外壳，也破坏「调用方不写句子」。取消自定义正文会毁掉现有公告/流程产品。包装模板增加必填变量校验，换取两类场景与静态场景同一套热配合同。

### Consequences
播种这两类场景时必须带包装模板与必填变量。保存配置时若删掉必填 token 则拒绝。短信侧把 title/content 映射到供应商模板参数，而不是把整段公告当自由短信。
