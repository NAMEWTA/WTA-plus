---
schema_version: 3
artifact: spec
change: 2026-09-10-notify-channel-config
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-09-10-activate-S-spec-as-consensus
  - USER-DECISION:Q1-A-ops-surface
  - USER-DECISION:account-independent-enable-mandatory-routing
  - USER-DECISION:db-sole-authority-yaml-no-accounts
  - USER-DECISION:hot-config-bodies-variable-slots
  - USER-DECISION:per-scene-recipient-intercept
  - ADR-001
  - ADR-002
  - ADR-003
  - ADR-004
  - ADR-005
  - ADR-006
  - ADR-007
  - ADR-008
  - ADR-010
  - ADR-011
  - ADR-012
  - ADR-013
  - ADR-014
  - ADR-015
  - ADR-016
  - CODE:ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify
  - CODE:ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-mail
  - CODE:ruoyi-vue-plus-namewta/ruoyi-common/ruoyi-common-sms
  - CODE:plus-ui-namewta/packages/web-domains/notify/src/index.ts
  - CODE:ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml
---

# Spec: 通知中心邮件/短信配置管理

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-10-notify-channel-config/spec.md</Path>`
- **当前 ADR：** `<Path>{roots.state}/specdev/changes/2026-09-10-notify-channel-config/ADR.md</Path>`
- **当前领域上下文：** `<Path>{roots.state}/specdev/changes/2026-09-10-notify-channel-config/CONTEXT.md</Path>`

## 1. 问题与目标

### 问题陈述

当前邮件发件人和短信供应商写在 YAML，场景正文和验证码模板码写在 Java。控制面投递只用调用方 `title`/`content` 快照当纯文本，SMS4J 的供应商模板路径存在但从未走。管理员无法在通知中心配置、启用或关闭渠道账号，也无法热配邮件文案或按场景绑定短信模板码。验证码还提交空 `templateCode`，走通知栈会被拒绝。YAML 全局 `minute-max` 把所有场景绑在同一套号码限额上。

### 目标用户与场景

- **通知配置管理员**在 Admin Web 通知中心维护邮件 SMTP 账号、短信厂商账号、场景绑定、文案、限额，并启停账号、测试发送。
- **业务调用方**（验证码、换绑、企业转移、工作流、公告发布）只提交场景编码和变量值，不写句子、不选供应商。
- **收件用户**收到与当前热配模板、正确发件人和正确短信模板码一致的邮件或短信。
- **排障人员**在现有通知监控中看到失败原因（缺绑定、账号停用、配额用尽），看不到 secret。

### 成功标准

- 管理员能在「通知配置」页的邮件/短信 TAB 完成账号 CRUD、独立启停、场景绑定、邮件文案热配、短信模板码与参数映射、三层限额，以及账号级/模板级测试发送。
- 完成后 YAML 不再留存发件人、供应商账号、`restricted` / `minute-max` / `account-max`；运行时只读数据库。
- MAIL/SMS 发送按场景绑定解析唯一账号；调用方不传 `providerKey`；无隐式默认账号。
- 生产 MAIL/SMS 调用方不再包含硬编码正文；验证码有稳定 `templateCode` 且只传变量。
- 变量名不能在管理端增删改；占用位置在配置页可见。
- 配额或拦截用尽、绑定缺失、账号停用时该渠道失败关闭，不调用供应商（或供应商调用前拒绝），不改选其他账号。
- 接口不回显 secret；写操作日志不保存请求体中的密钥。
- `.agents/skills` 中通知/邮件/短信相关事实与中文注释与实现一致。

### 非目标

- **OOS-001**：不复活 `notify_app`、KEK 加密热发布、路由灰度/故障切换、不可变模板版本全集。
- **OOS-002**：不把 `NotificationMode.SYNC` 改成请求线程内调用供应商。
- **OOS-003**：本期 TAB 不含站内信；`login-welcome` 等 IN_APP 文案仍在代码内。
- **OOS-004**：管理员不能新建逻辑场景或改变量名。
- **OOS-005**：不把频控做成延期队列或自动改选备用账号。
- **OOS-006**：不把邮件/短信配置放进 `ruoyi-system` 或 `common-notify`。

## 2. 解决方案与外部行为

### 解决方案摘要

在通知中心增加「通知配置」：单一菜单，页内 TAB 为邮件配置与短信配置。`ruoyi-notify` 拥有渠道账号、逻辑场景渠道绑定、管理 API 和前端页面。`common-mail` / `common-sms` 只提供按解析结果发送的运行时 SPI。数据库是账号、文案、配额与收件人拦截的唯一运行时权威。

发送时控制面按 `sceneCode + channel` 读取绑定：解析渠道账号、渲染邮件或绑定短信供应商模板码、检查三层限额，再调用现有 `NotifyClient`。调用方只传场景、逻辑模板码和变量。

### 主要流程

**管理账号**

1. 管理员打开通知中心 → 通知配置，切换邮件或短信 TAB。
2. 列表展示该渠道下的渠道账号：标识、供应商/SMTP 摘要、启用状态、每分钟账号吞吐；secret 不出现。
3. 新增/编辑填写连接字段；编辑时 secret 留空表示保持原值。
4. 独立启用或停用某个账号；可同时启用多个。停用不删除记录。
5. 删除受约束：被场景绑定引用的账号不能删。

**管理场景绑定与文案**

1. 页面列出代码播种的逻辑场景：`auth-captcha`、`person-rebind`、`enterprise-transfer`、`workflow-task`、`notice-published`。
2. 只读展示该场景变量清单（名称、必填、示例）。
3. 邮件绑定：选择启用中的 SMTP 账号、编辑主题/正文（必填变量以只读 `${name}` token 插入）、配置模板每分钟吞吐和收件人拦截（按邮箱）。
4. 短信绑定：选择启用中的短信账号、填写该账号下的供应商模板码、用映射表把逻辑变量对应到厂商参数位置、配置模板吞吐和收件人拦截（按手机号）。
5. 保存时校验：必填变量都出现在邮件 token 或短信映射中；模板每分钟吞吐不超过所选账号上限；绑定账号必须启用。

**发送**

1. 业务调用 `NotificationApplicationService.submit`，携带 `sceneCode`、`templateCode`、变量 `templateParams` 和渠道。
2. 对每个 MAIL/SMS 投递，控制面解析场景渠道绑定。缺绑定、账号停用、渲染/映射失败或任一配额用尽 → 该渠道失败关闭，写入可观察失败原因，不改选账号。
3. 邮件：用绑定文案渲染后发送，发件人来自绑定账号。
4. 短信：使用绑定账号与供应商模板码、已映射参数发送，不走自由文本。
5. 快照写入 intent/delivery 供监控；监控不执行 HTML，不展示 secret。

**包装模板**

- `notice-published`、`workflow-task` 的 MAIL/SMS 是外壳模板，必须保留已声明变量（至少 `${title}`、`${content}`，以及已声明的 `${path}`）。
- 公告编辑器或流程运行时只提供这些变量的值。

**测试发送**

- 账号级：向指定测试收件人发一条，验证凭据；走该账号，不绕过停用。
- 模板级：用样例变量走真实绑定；样例只能使用已声明变量。
- 测试发送占用与生产相同的限额语义。

### 边界、失败与稳定错误行为

- **无绑定 / 账号停用**：该渠道投递失败，不调用供应商，不改选其他启用账号。
- **配额用尽**：账号每分钟吞吐、模板每分钟吞吐、收件人分钟限额或日限额任一用尽 → 该渠道立即失败关闭，不延期。
- **收件人拦截关闭**：该场景绑定 `restricted=false` 时不按收件人计数，账号/模板吞吐仍生效。
- **变量缺失**：发送时缺少必填变量 → 该渠道失败，不调用供应商。保存配置时缺少必填占用位置 → 拒绝保存。
- **非法变量名**：管理端不能新增、改名或删除必填变量。
- **secret**：GET 详情/列表不回显；日志与异常不含 secret；空白 secret 表示保持原值。
- **YAML 无账号**：应用仍启动；发送只读库，库中无有效绑定则失败关闭。
- **验证码**：必须使用稳定 `templateCode`（与 `auth-captcha` 一致）。`SYNC` 仍只表示提交语义，不保证请求线程内已调用供应商。
- **IN_APP**：本期行为不变，不读邮件/短信模板。
- 用户可见失败与投递监控中的错误分类必须可区分上述原因；不得把超限或未绑定记为成功入队。

### 状态转换与不变量

- 渠道账号：`DISABLED ↔ ENABLED`；允许同时多个 ENABLED。被绑定引用时不可删除。
- 场景：仅代码播种，无管理端创建/删除。
- 每个 `sceneCode + channel` 至多一条绑定，且绑定零或一个账号。
- 发送身份不可默认：不存在「任意启用账号」回退。
- 模板每分钟上限 ≤ 所属账号每分钟上限。
- 三层限额同时生效，超限不改选账号。
- MAIL/SMS 正文权威在配置页，不在调用方字符串。
- YAML 不是账号、文案或拦截的运行时权威。

## 3. 用户故事

- **US-001**：作为通知配置管理员，我希望在通知中心用 TAB 维护邮件和短信渠道账号并独立启停，以便不改 YAML、不重启就能切换发件身份。
- **US-002**：作为通知配置管理员，我希望为播种场景绑定账号、热配邮件文案并看到变量占用位置，以便改句子而不改变量名。
- **US-003**：作为通知配置管理员，我希望为短信场景绑定供应商模板码和参数映射，以便按厂商审核模板发送而不是自由正文。
- **US-004**：作为通知配置管理员，我希望按账号和按场景配置每分钟吞吐，并按场景配置收件人拦截，以便验证码和公告使用不同限额。
- **US-005**：作为通知配置管理员，我希望对账号和模板分别测试发送，以便启停前确认凭据和绑定。
- **US-006**：作为业务调用方，我希望只传场景和变量，以便 MAIL/SMS 句子全部来自热配。
- **US-007**：作为验证码用户，我希望短信/邮件验证码走已配置的模板和账号，以便空 templateCode 不再导致发送失败。
- **US-008**：作为公告/流程运营，我希望 MAIL/SMS 使用包装模板包住 `${title}` `${content}`，以便既改外壳又不失去公告编辑器和流程正文。
- **US-009**：作为排障人员，我希望缺绑定、停用账号和配额用尽在监控中显示为失败且未改选账号，以便解释发送身份。
- **US-010**：作为安全管理员，我希望 secret 不回显、不进操作日志，YAML 不再保存账号密钥。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 管理员具有配置权限 | 打开通知配置 | 通知中心下单一菜单，TAB 为邮件/短信；无配置权限不可见或接口 403 | 菜单 DML + 前端 manifest + 权限 API |
| AC-002 | 邮件 TAB | 新增 SMTP 账号并启用 | 列表出现该账号；secret 不在列表/详情 JSON 中 | 配置 API 测试 |
| AC-003 | 已有启用账号 | 停用该账号 | 状态为停用；绑定该账号的后续 MAIL/SMS 失败且不调用供应商 | UseCase + Dispatch 测试 |
| AC-004 | 两个启用短信账号 | 场景只绑定账号 A | 发送使用 A 的供应商与模板码，绝不使用 B | Dispatch/Adapter 测试 |
| AC-005 | 场景未绑定或绑定账号已停用 | submit MAIL 或 SMS | 该渠道失败关闭，无供应商 HTTP/SMTP | Dispatch 测试 |
| AC-006 | 播种场景邮件模板 | 编辑主题/正文，移动 `${code}`，尝试改名或删除必填 token | 移动成功可保存；改名/删除必填 token 被拒绝 | 配置 API 测试 |
| AC-007 | 短信场景 | 保存供应商模板码和参数映射 | 发送走 `NotifyTemplateContent`，不走纯文本 | Adapter/Dispatch 测试 |
| AC-008 | 账号每分钟上限 1，同账号两秒内两次发送 | 第二次发送 | 第二次该渠道失败，原因可观察，不改选其他账号 | 限额测试 |
| AC-009 | 场景收件人 minute-max=1 | 同一手机号/邮箱两秒内两次该场景发送 | 第二次失败；其他场景不受该限额 | 限额测试 |
| AC-010 | 模板每分钟上限 > 账号上限 | 保存绑定 | 拒绝保存 | 配置 API 测试 |
| AC-011 | 生产调用方源码 | 静态/契约检查 | Captcha/换绑/企业转移/工作流/公告的 MAIL/SMS 不提交硬编码句子，只传变量；验证码 `templateCode` 非空 | caller 契约测试 |
| AC-012 | `auth-captcha` 已绑定 | 请求短信/邮件验证码 | submit 使用稳定 templateCode 与 `code` 变量，不再空模板码 | Captcha 契约测试 |
| AC-013 | 公告发布 / 工作流 MAIL/SMS | 提交运行时 title/content | 发出的是包装模板渲染结果，必填变量仍在；不是裸业务正文也不是 Java 硬编码句 | 发布/工作流契约测试 |
| AC-014 | 已保存 secret | GET 详情；编辑空白 secret 提交 | 响应无明文 secret；原 secret 保持 | 配置 API 测试 |
| AC-015 | YAML 无 mail 账号段、无 sms.blends、无全局 minute-max | 启动并发送 | 进程可启动；发送只读库；无绑定则失败 | 配置加载 + 发送测试 |
| AC-016 | 账号级/模板级测试发送权限 | 测试发送 | 走真实绑定或指定账号；占用相同限额；结果进入监控语义 | 测试发送 API 测试 |
| AC-017 | POST 配置写接口 | 调用 | 使用 POST + `@Log`；密钥类请求不保存 request body | Controller 契约 |
| AC-018 | Skill 与注释 | 对照实现 | `<Path>.agents/skills/</Path>` 通知/邮件/短信事实不再把 YAML 当账号权威 | Skill 事实校验 + review |

## 5. 范围

### IN

- `ruoyi-notify` layered 控制面：渠道账号、场景渠道绑定、配置 UseCase/API、限额检查、发送前路由与渲染。
- `ruoyi-common-mail` / `ruoyi-common-sms` 运行时按账号发送的 SPI；SMS 走供应商模板。
- `DispatchNotificationService` 对 MAIL/SMS 改为模板权威，并写入解析后的 providerKey。
- 生产 MAIL/SMS 调用方改为只传变量；验证码补 `templateCode`。
- Admin 通知配置页、权限菜单、domain/web-domain 注册。
- MySQL 基座 DDL/DML 播种五个场景绑定结构（账号可空，场景行必须存在）。
- YAML 删除账号与全局拦截项。
- 同步通知相关 Skill 与中文注释。

### REUSE

- 现有 `NotificationApplicationService` / `NotificationCommand`（不新增 `providerKey` 字段）。
- `ruoyi-common-notify` Dispatcher、Channel SPI、`NotifyTemplateContent`。
- 现有 Outbox Worker、通知监控、公告、收件箱。
- OSS 配置的 secret 不回显、空白保持原值、`isSaveRequestData=false` 模式。
- 现有权限/动态路由、`@Log`、layered 模块结构。
- SMS4J 作为短信发送实现；线程池等进程配置可留 YAML。

### OUT

- **OOS-001**：完整通知平台（`notify_app`、KEK 热发布、灰度故障切换、不可变版本发布链）。
- **OOS-002**：SYNC 请求线程内 Provider I/O。
- **OOS-003**：IN_APP 文案热配与站内信 TAB。
- **OOS-004**：管理员新建场景、自定义变量名、自由短信正文。
- **OOS-005**：超限延期队列、跨账号故障切换。
- **OOS-006**：system 模块邮件短信配置页。

## 6. 已锁定实现约束

- **DEC-001**：本期只做通知渠道配置运维面。来源：`ADR-001`。
- **DEC-002**：`ruoyi-notify` 拥有控制面；common-mail/sms 只提供运行时 SPI；common-notify 不建模板中心。来源：`ADR-002`。
- **DEC-003**：数据库是账号、文案、配额、收件人拦截的唯一运行时权威；YAML 删除发件人、供应商账号、`restricted`/`minute-max`/`account-max`。来源：`ADR-003`、`ADR-012`、`ADR-014`。
- **DEC-004**：账号独立启停，可同时启用多个；发送必须按场景绑定选账号，禁止隐式默认。来源：`ADR-004`、`ADR-006`。
- **DEC-005**：逻辑场景代码播种；管理员改文案和绑定，不能增删场景、不能改变量名。来源：`ADR-005`。
- **DEC-006**：普通调用不传 `providerKey`；控制面解析后写入投递。来源：`ADR-006`。
- **DEC-007**：secret 库内明文、接口不回显、空白保持原值、写日志不存请求体。来源：`ADR-007`。
- **DEC-008**：账号级 + 模板级测试发送。来源：`ADR-008`。
- **DEC-009**：MAIL/SMS 发送内容以场景模板为权威；本期清除硬编码正文。来源：`ADR-010`、`ADR-013`。
- **DEC-010**：验证码补稳定 `templateCode`；不改 SYNC 真同步。来源：`ADR-011`。
- **DEC-011**：账号每分钟吞吐 + 模板每分钟吞吐（≤ 账号）+ 按场景的收件人拦截；超限立即失败。来源：`ADR-012`、`ADR-014`、`ADR-015`。
- **DEC-012**：邮件占用位置是正文内只读 token；短信占用位置是参数映射表。来源：`ADR-013`。
- **DEC-013**：`notice-published` 与 `workflow-task` 使用包装模板。来源：`ADR-016`。
- **DEC-014**：配置 HTTP 前缀为 `/notify/config`；查询 GET、变更 POST；每个 POST 使用 `@Log`；权限字符使用 `notify:config:list|query|add|edit|remove|test`。来源：项目 API-005 与现有 notify 权限惯例。
- **DEC-015**：前端在 `@namewta/domain-notify` 与 `@namewta/web-domain-notify` 注册 `notify/config/index`。来源：`ADR-002`。
- **DEC-016**：实现必须更新 `<Path>.agents/skills/engineering-standards/references/notification.md</Path>`、`<Path>.agents/skills/ruoyi-module-guide/references/modules/notify/index.md</Path>` 及 mail/sms 相关 common Skill 事实，并补齐中文注释。来源：`ADR-003`、用户决定。

## 7. 数据、接口与兼容

- **公共接口变化：** 新增通知配置管理 API（`/notify/config/**`）。`NotificationCommand` 不增加 `providerKey`；`templateParams` 对 MAIL/SMS 只承载变量。业务模块仍只依赖 `NotificationApplicationService`。
- **数据模型与持久化：** Notify 库新增渠道账号与场景渠道绑定（含邮件文案、短信模板码、映射、账号/模板吞吐、收件人拦截）。场景与变量 schema 由代码播种并在初始化数据中有对应行。secret 列不出现在 VO。
- **兼容要求：** 无外部兼容窗口。内部调用方必须同期改为只传变量；不能保留 YAML 账号双读。
- **迁移要求：** 基座 DDL/DML 增加表和五个场景；`<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-dev.yml</Path>` 与 `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/main/resources/application-prod.yml</Path>` 删除账号与全局拦截。允许一次性把非占位 YAML 值迁入库，占位密钥不得自动启用。现有环境升级脚本与 `<Path>release-artifacts/docker/infrastructure/mysql/init/</Path>` 必须一致。
- **发布或运维影响：** 部署后管理员需在通知配置中启用账号并完成场景绑定，否则 MAIL/SMS 失败关闭。Nacos 不作为账号源。菜单、权限、前端 manifest 必须一起发布。

## 8. 非功能要求

- **NFR-001 安全与隐私：** secret 与 SMTP 密码不回显、不进操作日志、不进异常消息。测试发送与生产发送均需配置权限。邮件 HTML 在监控中不执行。收件人联系方式保持现有投递快照与脱敏合同。
- **NFR-002 性能与容量：** 限额计数必须在供应商调用前完成；计数器跨实例一致（复用现有 Redis）。不在本 Spec 虚构 QPS 数字。
- **NFR-003 可用性与可靠性：** 配置热刷新在保存成功后对该进程立即生效；多实例必须在保存后可见新绑定或明确失败，不得一半实例仍用旧 YAML。Outbox 异步投递保持现状。配置不可用时失败关闭，不回退 YAML。
- **NFR-004 可观测性与运营：** 缺绑定、停用、配额用尽必须在 `notify_delivery` / 监控页可查询。测试发送进入同一监控语义。Skill 与注释必须区分账号吞吐与收件人拦截。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 配置 UseCase/API | 单元/API | AC-002, AC-006, AC-010, AC-014, AC-017 | `<Path>ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify/src/test/java/org/dromara/notify/usecase/NotifyNoticeUseCaseTest.java</Path>` | 测试 |
| 发送路由与模板 | 单元 | AC-003, AC-004, AC-005, AC-007, AC-008, AC-009 | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/</Path>` | 测试 |
| 调用方契约 | 单元/静态 | AC-011, AC-012, AC-013 | `<Path>ruoyi-vue-plus-namewta/ruoyi-admin/src/test/java/org/dromara/test/notify/caller/CaptchaNotifyCallerUnitTest.java</Path>` | 测试 |
| 模块分层 | 静态 | DEC-002 | `node .agents/skills/namewta-fullstack-development/scripts/validate-module-mode.mjs ruoyi-vue-plus-namewta/ruoyi-modules/ruoyi-notify --mode layered` | 命令 |
| 后端测试 | 模块测试 | 受影响 notify 合同 | `./mvnw -pl ruoyi-modules/ruoyi-notify -am test` | 命令 |
| 前端类型与页面 | typecheck/单测 | AC-001, DEC-015 | `pnpm --dir plus-ui-namewta typecheck`；notify web-domain 测试 | 命令 |
| 基座 SQL | 契约测试 | AC-001, AC-015, 播种场景 | `<Path>release-artifacts/tests/</Path>` notify/菜单基线 | 测试 |
| Skill 事实 | 静态 | AC-018, DEC-016 | `node .agents/skills/engineering-standards/scripts/validate-skill-facts.mjs` | 命令 |
| 管理员 E2E | 浏览器 | AC-001, AC-002, 启停与 TAB | 参照 `<Path>plus-ui-namewta/e2e/oss-config-access-policy.spec.ts</Path>` | E2E（有浏览器工具时） |

## 10. 风险、假设与未决问题

### 风险

- 升级后若管理员未绑定场景，既有验证码/公告/流程 MAIL/SMS 会立即失败关闭。必须在发布说明和初始化数据中给出可启用路径。
- 限额从全局号码拦截改为按场景后，未正确播种的场景可能过宽或过严。
- SMS4J 从 yaml blends 改为接口配置，若刷新 SPI 不完整会出现多实例配置漂移。
- 一次性清理调用方正文时，工作流/公告若漏包装模板会发出空外壳或校验失败。

### 已采用的低影响假设

- **H-001**：权限字符为 `notify:config:list|query|add|edit|remove|test`，菜单组件 `notify/config/index`。验证：菜单 DML 与 manifest 测试。
- **H-002**：`auth-captcha` 收件人拦截默认 `restricted=true`、`minute-max=1`、`account-max=30`，以保持当前 YAML 验证码防刷量级；其他播种场景必须有显式默认值并在 DML 中可见。验证：DML 断言。
- **H-003**：邮件正文允许 HTML，监控侧不执行。验证：监控既有 HTML 不执行合同 + 模板保存测试。
- **H-004**：限额计数使用现有 Redis。验证：限额测试在多 key 隔离下通过。
- **H-005**：demo MAIL/SMS 改为只传变量或不再直发纯文本；不进入管理员 TAB。验证：caller 契约测试。

### 未决问题

无。
