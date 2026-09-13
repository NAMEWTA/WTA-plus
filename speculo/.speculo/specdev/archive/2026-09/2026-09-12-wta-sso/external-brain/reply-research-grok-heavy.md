# WTA SSO · CTO 最新口径对照评审（Grok Heavy）

> 权威：CTO BRIEF 2026-09-13 t155u。冲突一律取最新口径。本文只写最新态，不堆演变史。  
> 对照范围：change `2026-09-12-wta-sso` 的 CONTEXT / goal-plan / ADR / source / LOG（事实锚点） / external-brain / design-tree / `.status.json`。  
> 阶段门禁：仍停在 **G-grill-with-docs**。不得催进 S-spec / 实现，不得改产品代码。

## 0. 总判

- 符合度：**部分符合**
- 理由：协议脊柱、模块落地、三步接入骨架、工程不变量与阶段门禁与最新口径可并存——P0 先做 Authorization Code + PKCE、新建 `wta-sso` / `sso-web`、创建应用 → 配回调 → 交付 client/密钥、身份与 RBAC 不外包给 Keycloak 类产品、`ready_for_execution=false` 且不得进 S-spec。但产品叙事整段偏了：文档把本期写成「第一方 SSO / NoExternalIdP / 仅第一方 / P2 才真正外部第三方」，最新口径把本期写成「第三方登录体系里、自建且默认已接通、排序最前的登录提供方」；接入面明确含外部系统 App，实现目标是登录归一化。协议没写反，定位、范围、默认路径三条缺位或打架，故不是「符合」，也未到「不符」。

## 1. 前 5 条关键偏差（摘要表）

| # | 偏差 | 位置 | 严重度 |
|---|---|---|---|
| 1 | 产品定位写成「仅第一方 / NoExternalIdP」，未把自建 SSO 放进第三方登录目录第一槽位 | CONTEXT 概念卡 `NoExternalIdP`；goal-plan Outcome / IdP；ADR-001；design-tree D-001 | 严重 |
| 2 | 接入范围以 admin-web / home-web 为中心，外部系统 App 被推到 P2「真正外部第三方」 | CONTEXT Phasing；goal-plan Non-goals / P2；source 分期；ADR-005 | 严重 |
| 3 | 「默认已接通 + 排最前 + 默认路径走通」零产品合同 | CONTEXT / goal-plan / ADR / design-tree 全缺 | 严重 |
| 4 | 实现目标未升格为「登录归一化」；硬验收只证明 Token 隔离，不证明多端同一套账号 | goal-plan Outcome / P0 hard acceptance；ADR-002；D-011 | 高 |
| 5 | 现有 social（Mask / GitHub，`IAuthStrategy` 已有）与自建 SSO 两套登录故事并存、未同槽 | source 现状判断 vs CONTEXT/ADR「无外置 IdP」；goal-plan False completion | 高 |

## 2. 完整偏差清单

按严重度排序。每条：偏差点 → 位置 → 为何冲突 → 建议最新表述。

### D-1｜产品定位：第一方独占 vs 第三方登录目录的默认第一提供方（严重）

- **偏差点：** Glossary 把「SSO（本期）」定义为「第一方统一登录」；概念卡 `NoExternalIdP` 把「不外包身份源」写成产品定位；Outcome 写「交付第一方 SSO」；多处口号「无外置 IdP」。
- **位置：** CONTEXT.md Glossary / `NoExternalIdP`；goal-plan.md §1 Outcome、Protocol「IdP」、False completion、Constraints；ADR-001 Decision；design-tree D-001 answer；tickets-map 总体实施背景；external-brain/reply.md「第一方 browser SSO」。
- **为何冲突：** 最新口径第 3 条：本期是「第三方登录」体系的一部分；自建 SSO **也是一种第三方登录提供方**，自己实现、默认已接通、排最前。旧文把「身份源不外包」与「产品上不是第三方登录」捆成一句，读者会以为本期与 Mask/GitHub 体系无关、外部 App 不能当 RP 来接。
- **建议最新表述：** 使用单一词条 `FirstPartySsoProvider`（见 §3）。产品面 = 第三方登录目录的默认第一提供方；身份面 = 本仓用户 / 账号密码 / RBAC 仍是真相源。两面必须写在同一句里，禁止再分「仅第一方」与「第三方登录」两套口径。

### D-2｜接入范围：自有双 App vs 自有各 App + 外部系统 App（严重）

- **偏差点：** 可观察对象几乎只有 `admin-web` / `home-web`。P2 写成「真正外部第三方」，把「外部系统作为接入方」和「外置产品作为身份源」捆死。
- **位置：** CONTEXT 现状锚点 / Phasing；goal-plan Outcome P0 可观察、Non-goals「P0 不做真正外部第三方」、Phasing P2；source 推荐架构图与分期；ADR-005 P2；tickets-map 硬验收只写双 App。
- **为何冲突：** 最新口径第 1 条：其他 APP（自有前端各 App + **外部系统 App**）都可接入本单点登录服务。外部系统 App 是 **Relying Party / 业务 Client**，不是 Keycloak。把它们放进 P2 等于改写接入范围。
- **建议最新表述：** `接入方 App = 自有前端各 App + 外部系统 App`，共用一层 `sys_client` 目录与同一套 (a)(b)(c) 流程。P0 必须保证目录模型可登记外部 App；外部 App 的 **运行时深度**（是否 P0 就跑通 confidential）进 Grill，而不是用「P2 外部第三方」一笔勾销。

### D-3｜「默认已接通 / 排最前 / 默认路径走通」缺失（严重）

- **偏差点：** 最新口径第 3 条的三个可观察产品属性在 CONTEXT、goal-plan、ADR、design-tree 均无节点。现有决策只覆盖协议、模块、authMode、域名、Cookie owner。
- **位置：** 全套规划工件；最接近的是 ADR-004 `authMode=both` 与 `GET /auth/client/context` 扩展，但「both」只说明本地登录仍在，并不等于「第三方登录区第一项默认接通且默认路径走通」。
- **为何冲突：** 这是产品合同，不是修辞。缺了之后，实现既可以做成「各 App 登录页第一颗 WTA SSO 按钮」，也可以做成「整页跳 sso-web」，也可以做成「开关默认关、要手配才通」——三种都吃不下「默认路径都能走通」。
- **建议最新表述：** P0 产品合同增加一条：`FirstPartySsoProvider` 在第三方登录目录预置、排序第一、默认启用；自有 App 走「直接读配置」即可发起授权码流，无需运营手工粘贴才能点通。具体 UX 形态（按钮 / 整页跳转 / 两者）见 Grill P0-1，未答前不得当已锁定。

### D-4｜实现目标未写成「登录归一化」（高）

- **偏差点：** Outcome 与硬验收的主语是「统一登录页 + 换票 + Token extras 绑定目标 Client + 双 App clientid 互拒」。最新口径第 2 条的主语是「同一套账号密码，多端登录」。
- **位置：** goal-plan Outcome / P0 hard acceptance / Success；ADR-002 Consequences；design-tree D-011（P0-SSO-REUSE + P0-CLIENT-ISOLATION）。
- **为何冲突：** 工程不变量正确，但不能顶替产品 Outcome。只验「两张 Token 不同且互拒」，本地各登一次也能混过隔离门；只验 SSO-REUSE，仍未声明「账号归一 ≠ Token 共用」。读者可能把「登录归一化」误读成共享一张业务票。
- **建议最新表述：** Outcome 第一句改为登录归一化。工程门保持两门：`P0-SSO-REUSE`（第二次业务 App 授权不得再要本仓密码）+ `P0-CLIENT-ISOLATION`（目标 Client 的 Sa-Token 互拒）。另增产品门：`P0-DEFAULT-PROVIDER-PATH`（默认第三方登录入口可走通）。三条并列，互不替代。

### D-5｜与已有 social IdP 未同槽（高）

- **偏差点：** source 已写现行链路 `IAuthStrategy` 含 `password / sms / email / social / xcx`，并点名可接 Mask、GitHub。规划文随即改口「无外置 IdP」，False completion 写成「引入 Keycloak/Casdoor/…等外置 IdP 作为身份源」——外延过宽，易误伤已存在的 social。
- **位置：** source.md 现状判断 vs 协议选型；goal-plan False completion / Constraints / DoD #6；tickets-map「禁止误接第三方 IdP SDK」；CONTEXT 无 social 槽位术语。
- **为何冲突：** 最新口径明确「除了外接第三方 IdP（如已有 Mask、GitHub 等 social），还提供自建的第一方 SSO」。两套都要在，而且自建 SSO 排最前。旧文把「不外包用户目录」写成「系统里不能有第三方登录提供方」。
- **建议最新表述：** 第三方登录目录 = `{ FirstPartySsoProvider（第一） , Mask, GitHub, … }`。social 仍是同目录后续槽位，不是本期身份源，也不与自建 SSO 互斥。禁止句收窄为：「禁止把本仓用户目录替换/外包给 Keycloak、Casdoor、Logto、Hydra 等外置 IdP 产品」。

### D-6｜操作流程未升格为 Outcome 合同（中高）

- **偏差点：** 最新口径第 5 条 (a) 创建应用 (b) 配回调 (c) 把 client/密钥交给应用、自有 App 也可直接读配置。source「管理端」有能力清单，CONTEXT / Outcome 没有把它写成产品主路径。
- **位置：** source 管理端；goal-plan Module landing 只在技术表里出现 `GET /auth/client/context`；CONTEXT 无「操作流程」卡。
- **为何冲突：** 自有 App「直接读配置」与外部 App「被交付密钥」是两条交接面。文档只隐写了前者，外部 App 的 P0 交付物（密钥一次性展示、轮换、文档化 onboarding）空缺。
- **建议最新表述：** 把 (a)(b)(c) 写入 Outcome 与 CONTEXT 产品定位。自有 App：读 `/auth/client/context`（或等价配置面）即可拉到 `ssoEnabled / ssoAuthorizeUrl / authMode`，禁止要求运营把密钥贴进前端包。外部 App：管理面生成 client + `sso_secret`（明文只一次）+ 回调白名单，由对方自行配置。

### D-7｜D-015 confidential 与「外部系统 App 都可接入」未咬合（中高）

- **偏差点：** design-tree D-015 / ADR-001：第一方 SPA = public，强制 PKCE，禁止 browser 持有 secret；confidential + `sso_secret_hash`「P0 可有管理项但第一方 SPA 不用」。
- **位置：** design-tree D-015；ADR-001 Consequences；external-brain F-07（压缩 P0 vs 管理即运行）。
- **为何冲突：** 外部系统 App 很大概率是服务端 / confidential。若 P0「不用」confidential 运行时，则「外部系统 App 都可接入」在协议层落空，只剩登记字段。这不是推翻 D-015 的 public SPA 结论，而是缺一张「外部 RP 要不要在 P0 跑通」的产品决定。
- **建议最新表述：** 第一方 SPA 维持 public + PKCE、密钥不得进浏览器。另开 Pending：外部系统 App 是否在 P0 就必须以 confidential client 完成 token 鉴权；若否，P0 只保证目录可登记 + 文档化交接，运行时放到下一阶段——必须由 CTO 点头，不能默认「管理项可建=已接入」。

### D-8｜分期用词「P2 = 真正外部第三方」与新口径互消（中）

- **偏差点：** CONTEXT Phasing、goal-plan Phasing、source 分期、ADR-005 均把「外部第三方」当作 P2 范围。
- **为何冲突：** 在新口径下，「外部」已裂成三件不同的事：① 外接 social IdP（已有）；② 自建 SSO 作为提供方（本期）；③ 外部系统作为接入方（产品范围含之）。继续写「P2 真正外部第三方」会把 ①③ 再度赶出本期。
- **建议最新表述：** P0 = Code 流 + 默认提供方槽位 + 应用接入 + sso-web + 本地并存；P1 = OIDC / refresh / SLO / 同意页；P2 = 独立进程 / MFA 收敛。删除「真正外部第三方」这一栏。

### D-9｜开放问题表过期，两套 blockers 并存（中）

- **偏差点：** CONTEXT「开放语义见 Goal Plan CTO-Q1…Q4」；goal-plan §7 仍把 Q1–Q4 列为 Pending Decisions。design-tree 11/11 已 answered（Q1 整包、Q2 环境矩阵可占位、Q3 默认 both + 可选直连、Q4 A1+B2 同进程+独立 Origin）。`.status.json` blockers 正确停在「不得进 S-spec」，但开放问题内容没换。
- **位置：** CONTEXT 开放语义；goal-plan §7；tickets-map「开放：CTO-Q1…Q4」；external-brain/notes.md「仍开放 D-002/D-004」（相对 LOG 已过时）。
- **为何冲突：** 最新口径要求只写最新态。继续把已答的协议/域名/authMode/部署问当待决，会把 Grill 拉回旧题，挤掉定位/槽位/外部 RP 这些真正未问的问题。
- **建议最新表述：** 旧 Q1–Q4 视为已答事实，不再当 Pending。本轮 Pending 换成 §4 清单。design-tree `status=consensus` 只表示旧 frontier 收口，**不等于**新口径已锁定，更不等于可进 S-spec。

### D-10｜外脑叙事与最新口径冲突，不得当权威（中）

- **偏差点：** external-brain/reply.md、notes.md 仍以「第一方 browser SSO / 无外置 IdP / 尚不能宣称 consensus」为结论框架。
- **位置：** `external-brain/reply.md` Summary 与 F-07；`external-brain/notes.md`。
- **为何冲突：** BRIEF 写明旧 ChatGPT 外脑冲突点由 CTO 最新口径覆盖。外脑在 PKCE 负向合同、SSO-REUSE 拆门、模块 Port、Cookie owner、platform/adapters 分层上仍可作候选技术意见；在产品定位、接入范围、提供方槽位上不再引用。
- **建议最新表述：** 外脑技术候选可保留在 Grill 附录；产品句以 §3 统一口径为准。禁止把 reply 当 accepted ADR 或 Ready Spec。

### D-11｜CONTEXT 过薄，缺新产品术语（中）

- **偏差点：** CONTEXT 无「第三方登录体系 / 提供方 / 接入方 App / 登录归一化 / 默认接通」词条；开放语义仍指向旧四问。
- **建议最新表述：** 直接替换为 §5.1。删除 `NoExternalIdP` 词条名（约束并入 `FirstPartySsoProvider`）。

### 明确保持、不视为偏差的部分

下列与最新口径兼容，Grill 中不要重开成「要不要做」：

- P0 协议 = OAuth 2.0 Authorization Code + PKCE S256；禁 Implicit / password grant / SAML；OIDC → P1
- 新建子模块 `backend/wta-modules/wta-sso` + `frontend/apps/sso-web`
- `access_token` = 现有 Sa-Token；extras = 目标业务 Client，禁止写成 `sso` 中心 Client
- 一层应用目录：扩展 `sys_client`；新表只放 code / refresh / consent
- 现有 `client_secret` ≠ OAuth 密钥；用 `sso_secret_hash`
- 回调白名单精确匹配，禁止 `*`
- 两层会话：SSO 域 HttpOnly Cookie（后端 Set-Cookie）；业务 App Header Bearer
- `authMode = local | sso | both`；保留 `POST /auth/login`
- 第一方 SPA = public，禁止 browser 持有 `sso_secret`
- `revoke ≠ SLO`；SLO → P1
- 生产：同进程组装 + `sso-web` 独立 Web Origin（A1+B2）
- 阶段：G-grill-with-docs；`ready_for_execution=false`；`implementation_commit=not-authorized`；不改产品代码、不 push/PR、不催 S-spec

## 3. NoExternalIdP ↔「默认第三方登录提供方」统一表述

**一条口径，废止双轨。删除作为产品定位的 `NoExternalIdP` /「仅第一方」。**

**FirstPartySsoProvider（取代 `NoExternalIdP` 与「仅第一方」）：**

自建 SSO 是「第三方登录」目录里的**默认第一提供方**：自己实现、默认已接通、排序最前、默认路径可走通。对接入方（自有前端各 App + 外部系统 App）它就是一种第三方登录——(a) 创建应用、(b) 配置回调、(c) 交付 client/密钥（自有 App 可直接读配置），P0 只跑 Authorization Code。它与已有 Mask / GitHub 等 social IdP **同槽**，排在它们前面；不是另起一套「与第三方登录无关」的体系。

身份与账号密码、RBAC 真相源仍在本仓。禁止把用户目录外包给 Keycloak / Casdoor / Logto / Hydra。旧词 `NoExternalIdP` **只保留这一条架构约束**，不再当产品定位口号。

三个禁止混淆的角色：

| 角色 | 是什么 | 本期怎么处理 |
|---|---|---|
| 自建 SSO 提供方 | 本仓实现的登录提供方（OP） | 默认第一槽位，P0 主交付 |
| 外接第三方 IdP | Mask / GitHub 等 social | 同目录后续槽位，已存在；不替换、不互斥 |
| 外部系统 App | 接入本 SSO 的 RP / 业务 Client | 产品范围含之；P0 运行时深度待 Grill |
| 外置 IdP 产品 | Keycloak / Casdoor / Logto / Hydra | 禁止作为用户目录 / 身份源 |

一句话防误读：**禁止再用「无外置 IdP」否定「可被其他 App 当第三方登录来接」；也禁止把「第三方登录提供方」理解成「用户目录外包」。**

账号归一与票据隔离写在同一句：同一套本仓账号密码，多端登录；换票后仍按**目标业务 Client** 签发 Sa-Token。登录归一化 ≠ 共享一张业务 Token。

## 4. Grill 开放问题（按优先级）

面向 CTO 可直接问。旧 CTO-Q1…Q4（协议包、域名矩阵、authMode=both、同进程+独立 Origin）已答，不再重复。

### P0-1｜默认第一提供方的产品表面（最高）

「排在第三方登录最前、默认路径走通」用户看见的是哪一种？

- A. 各 App 登录页第三方登录区第一项按钮（与 Mask / GitHub 并列，自建 SSO 第一）
- B. 各 App 入口整页跳转 `sso-web` 做授权码
- C. A + B 都要：登录页第一按钮，点下去走授权码到 `sso-web`
- D. 其它（请说）

「默认已接通」指：预置 Provider 记录且默认启用 / 自有 App 免配自动读配置 / 用户免选直达，还是三者都要？

### P0-2｜外部系统 App 的 P0 深度

「外部系统 App 都可接入」在本期的最小可观察结果是？

- A. P0 必须让仓外系统走完 (a)(b)(c) + Authorization Code（含 confidential token 鉴权）
- B. P0 只保证管理面可登记（回调 + client + 密钥），运行时先打通自有 App
- C. P0 连登记模型都不做，外部接入整体后置（若选 C，等于改写最新口径第 1 条，需明示）

### P0-3｜sso-web 与 Mask / GitHub 的关系

`sso-web` 上可以出现什么？

- A. 只收本仓账号密码（social 仍只出现在各业务 App 登录页，且排在自建 SSO 之后）
- B. `sso-web` 本身也展示 Mask / GitHub，自建 SSO 是外壳
- C. 自建 SSO 与 social 平级槽位，但物理上不共享同一登录页
- D. 其它

### P0-4｜「同一套账号密码」的账户边界

登录归一化的「账号密码」是否 = 现有 `sys_user` 密码？

- 仅 social、无密码的用户如何归一到多端？
- sms / email / xcx 策略是否纳入「同一套账号」，还是本期只归一 password + 自建 SSO？
- 请书面确认：账号归一 **不等于** 各 App 共用一张 Sa-Token（仍按目标 Client 换票）

### P0-5｜默认证通的配置面与 authMode

「默认已接通、排序第一」写在哪？

- A. 现有 social / IdP 配置表（与 Mask/GitHub 同目录）
- B. `sys_client` 的 SSO 开关 + 排序字段
- C. 新建 provider 目录
- D. 硬编码第一槽位，不做配置面

当 `authMode=both`（已答）时：本地密码框是否仍在登录页主路径？自建 SSO 是否仍然算「默认路径最前」？两者如何不互相否定？

自有 App「直接读配置」是否就是扩展后的 `GET /auth/client/context`（`ssoEnabled / ssoAuthorizeUrl / authMode`），无需把密钥贴进前端？

### P0-6｜产品验收是否升格

是否把下列升为 P0 产品硬验收，与工程两门并列？

1. 同一套本仓账号，可在 admin-web 与 home-web（及演示用外部登记 App，若 P0-2 选 A）完成登录
2. 默认第三方登录入口不经额外开通即可走通授权码主路径

工程两门建议保留：`P0-SSO-REUSE`、`P0-CLIENT-ISOLATION`。

### P0-7｜confidential 是否服务外部 App

D-015「第一方 SPA = public」保持。请拍：

- A. P0 必须跑通 confidential client 的 token 鉴权（服务外部系统 App）
- B. P0 管理面可建 confidential 字段，运行时后置
- C. 其它

### P1（可后问，不挡本轮 Grill 收口）

- 授权页形态：整页跳转 / 弹窗 / iframe 嵌入
- 外部 App onboarding 手册是否进本期文档（非代码）
- P2 栏正式改名为「独立进程 / MFA」，不再出现「真正外部第三方」
- 本期是否维持只做 Authorization Code 类（revoke 语义、OIDC、SLO 仍 P1）——建议确认保持

## 5. 可晋升片段

以下为可直接粘贴的最新态正文。删除被取代的 `NoExternalIdP` /「仅第一方」 / 「P2 真正外部第三方」 / 「开放问题 = CTO-Q1…Q4」。

### 5.1 CONTEXT 最新态（可粘贴）

```markdown
# WTA SSO — 领域上下文

> **Status:** proposed / draft。G-grill 中，未 locked。  
> **Authority:** CTO 书面决定（含 2026-09-13 t155u）> 本 CONTEXT（经 Grill 确认后）> Spec > Goal Plan。  
> 阶段：G-grill-with-docs。不得催进 S-spec / 实现。  
> 冲突以 CTO 最新口径为准。旧词 `NoExternalIdP` /「仅第一方」已废止，由 FirstPartySsoProvider 取代。

## Glossary

| Term | Meaning |
|---|---|
| **第三方登录体系** | 登录页上可插拔的登录提供方目录。已有 Mask、GitHub 等 social；本期把**自建 SSO**放进同一目录，且排第一。 |
| **FirstPartySsoProvider** | 自建、自己实现的登录提供方：默认已接通、排序最前、默认路径可走通。对接入方等同「第三方登录」；身份真相源仍在本仓。取代旧词 `NoExternalIdP` /「仅第一方」。 |
| **登录归一化** | 同一套本仓账号密码，多端（自有前端各 App + 外部系统 App）登录。账号归一 ≠ Token 共用。 |
| **SSO（本期）** | 自建 SSO 服务：人在 `sso-web` 用本仓账号认出，经 Authorization Code + PKCE 换得**目标业务 Client** 的现有 Sa-Token。 |
| **接入方 App** | 自有前端各 App（admin-web / home-web / 后续自有 App）以及外部系统 App；均可按同一套流程接入。 |
| **Sa-Token（access_token）** | 现有 JWT Simple + Redis 会话；`loginId=userType:userId`；extra 含 `clientid` 与 `clientPk`。 |
| **业务 Client** | `sys_client` 行（admin / home / 外部登记应用）；Token extras 必须指向它，而非 SSO 中心 Client。 |
| **SSO Client** | ClientId=`sso`；会话键=`Sso-Token`；仅 SSO 域会话，**不**作为业务 API Token extras。 |
| **authMode** | 每 Client：`local` / `sso` / `both`；保留 `POST /auth/login`。与「SSO 排在第三方登录最前」同时成立，具体默认 UX 待 Grill。 |
| **PKCE S256** | Authorization Code 流强制；禁 Implicit / password grant。OIDC → 后期。 |
| **sso_secret_hash** | OAuth 客户端密钥哈希；**不等于**现有 `client_secret`。自有 App 直接读配置；外部 App 由管理面交付 client/密钥。 |
| **两层会话** | SSO 域可 HttpOnly Cookie（后端 Set-Cookie）；业务 App 仅 Header Bearer + 自有存储键。 |

## 概念卡

**LoginNormalization：** 同一套本仓账号密码，多端登录（自有 App + 外部系统 App）。账号归一 ≠ Token 共用。

**ThirdPartySlotFirst：** 自建 SSO 是第三方登录目录的第一槽位：默认已接通、排序最前、默认路径可走通；Mask / GitHub 等仍在其后。不是「取消第三方登录」，也不是「再引入外置用户目录」。

**FirstPartySsoProvider：** 产品面 = 第三方登录提供方；身份面 = 本仓用户与 RBAC 仍是真相源。禁止把用户目录外包给 Keycloak / Casdoor / Logto / Hydra；禁止用「无外置 IdP」否定外部 App 接入。

**AccessScope：** 自有前端各 App + 外部系统 App 均可接入。外部系统 App = RP/Client，不是外置身份源。

**OpsFlow：** (a) 创建应用 (b) 配置回调/返回地址 (c) 把 client/密钥交给应用；自有 App 也可直接读配置。

**WhyNotPseudoSso：** 跨域种根 Cookie 或复用 Admin-Token 会因 `SecurityConfig` 要求请求 `clientid` == Token extra 而失败。真正 SSO 是「统一认人 + 按目标 Client 换票」。

**TokenInvariant：** `access_token` **就是**现有 Sa-Token；换票不得签发「SSO 中心 Client」的业务票。

**SingleAppDirectory：** P0 扩展 `sys_client`（回调白名单、public/confidential、SSO 开关、PKCE、自动同意、scope、密钥轮换）；一次性 code / refresh / consent 才可新表。外部系统 App 与自有 App 共用这一层目录。

**Phasing：** P0 = Authorization Code + sso-web + 应用接入（a 创建应用 / b 回调 / c 交付或读取 client 配置）+ 默认提供方槽位走通 + 本地登录并存；P1 = OIDC / refresh / SLO / 同意；P2 = 独立进程 / MFA 收敛。不把「外部第三方」单列为本期禁区。

## 产品定位

本期是「第三方登录」体系的一部分：除已接通的 Mask / GitHub 等外接 IdP，再提供自建第一方 SSO，并把它当作默认、排序最前、路径已通的第三方登录提供方。接入范围 = 自有前端各 App + 外部系统 App。实现目标 = 登录归一化。模块 = `wta-sso`（先只做 Authorization Code 类）。操作流程：(a) 创建应用 (b) 配置回调/返回地址 (c) 把 client/密钥交给应用；自有 App 也可直接读配置。

## 现状锚点（intake）

- 已激活终端：`admin-web` / `home-web`；外部系统 App 为明确接入对象，P0 运行时深度待 Grill
- 已有 social：`IAuthStrategy` 含 password / sms / email / social / xcx（Mask、GitHub 等）
- Cookie 默认关闭；业务走 Bearer
- `/auth/login` **不**校验 `client_secret` 作为 OAuth secret
- Baseline：CTO `b06d161`；规划冻结 HEAD `d1ce372`

## 开放语义（待本轮 Grill）

见 Goal Plan Pending：默认槽位 UX、外部系统 P0 深度、SSO 与 social 关系、账号边界、「默认接通」配置面、产品验收是否升格、confidential 是否服务外部 App。
```

### 5.2 goal-plan 最新态要点（可粘贴）

```markdown
## Outcome

在 WTA-plus 的「第三方登录」体系中交付 **FirstPartySsoProvider**（自建、默认已接通、排序最前、默认路径可走通）：自有前端各 App + 外部系统 App 均可接入；同一套本仓账号密码多端登录；P0 只实现 Authorization Code + PKCE；换票后仍签发**目标业务 Client** 的现有 Sa-Token（账号归一 ≠ Token 共用）。

P0 可观察结果：

1. 协议：`OAuth 2.0 Authorization Code + PKCE (S256)`；禁 Implicit / password grant / SAML；OIDC → P1。身份真相源仍在本仓，不引入 Keycloak / Casdoor / Logto / Hydra 替代用户目录。
2. 产品槽位：自建 SSO 作为第三方登录目录第一提供方，默认已接通、默认路径可走通；Mask / GitHub 等同目录其后，不互斥。
3. 模块：`backend/wta-modules/wta-sso` + `frontend/apps/sso-web`；扩展 `sys_client` 为一层应用目录（不另建平行 OAuth 应用表）。
4. 接入流程：(a) 创建应用 (b) 精确回调白名单（禁 `*`） (c) 交付 client/密钥；自有 App 可直接读配置（`/auth/client/context` 或等价面）。
5. 登录模式并存：按 Client 配置 `local` / `sso` / `both`；保留 `POST /auth/login`。
6. 工程不变量：同一浏览器 SSO → `admin-web` → `home-web`；两张 Token 的 `clientid` **必须不同**，互打接口 **必须被拒**；第二次业务 App 授权不得再要本仓密码（SSO-REUSE）。
7. 产品验收（待 Grill 确认是否升格为硬门）：默认第三方登录入口不经额外开通即可走完授权码主路径。

## Success and False Completion

**Success（本阶段）：** 最新口径写入 CONTEXT / Goal Plan；Grill 开放问题（提供方 UX、外部 App P0 深度、social 同槽、账号边界、配置面、验收升格、confidential）有书面答或明确 defer；下一 Work 仍为 G-grill 收口或 CTO 另行开放 S-spec；`ready_for_execution=false`。

**False completion（禁止宣称完成）：**

- 声称可进 S-spec / 可执行 / 已上线（Grill 收口 ≠ S 授权）
- 用根域 Cookie / 复用 Admin-Token 做伪 SSO
- 签发 extras=`sso` 中心 Client 而非目标业务 Client
- 把本仓用户目录外包给 Keycloak / Casdoor / Logto / Hydra
- 把「禁止外包身份源」写成「系统不得存在任何第三方登录」
- 把本期写成「仅第一方 / NoExternalIdP / 与第三方登录无关」或「P0 禁止外部系统 App 接入」
- 采用 Implicit / password grant / SAML
- 造假 `ticket/*.md` 糊绿 validate
- 改 `2026-09-10-notify-channel-config` 或推送 / PR

## Non-goals

- 本期不改业务/产品代码、不 push、不 PR；不催派 RVP·规格
- P0 不做 OIDC discovery / id_token / userinfo、refresh、SLO、完整同意页（→ P1）
- P0 不把用户目录外包给外置 IdP 产品；不替换现有 Sa-Token；不关闭业务 App Header Bearer
- 外部系统 App 的 **运行时深度**（confidential token 鉴权是否 P0 必须跑通）待 Grill，不在本文写成「P0 不做外部第三方」
- social IdP（Mask / GitHub）已存在，P0 不重做、不拆除；只要求自建 SSO 进入同一目录并排第一
- 独立 SSO 进程、MFA 收敛到 SSO → P2

## Phasing

| Phase | Scope |
|---|---|
| **P0** | Authorization Code + PKCE + `sso-web` + 默认提供方槽位接通 + 应用接入 (a)(b)(c) + Client 管理扩展 + admin/home 可走通 + 本地登录并存 |
| **P1** | OIDC discovery / id_token / userinfo、refresh、SLO、同意页 |
| **P2** | 独立进程、MFA 收敛 |

## Pending Decisions

旧 CTO-Q1…Q4 已答，不再列入。本轮 Grill：

1. 默认第一提供方 UX：登录页第一按钮 / 整页跳 `sso-web` / 两者都要？「默认接通」的最小含义？
2. 外部系统 App：P0 只登记，还是必须跑通 Authorization Code（含 confidential）？
3. `sso-web` 与 Mask / GitHub：只本仓密码 / SSO 页也挂 social / 平级不同页？
4. 「同一套账号密码」= 现有 `sys_user` 密码？无密码/仅 social 用户如何归一？书面确认账号归一 ≠ 共享 Token。
5. 「默认已接通、排第一」落在 social 目录、`sys_client` 开关，还是新建 provider 表？`authMode=both` 时本地密码框与「默认路径最前」如何并存？
6. 是否把「多端同一账号可登 + 默认第三方入口走通」升为 P0 产品硬验收？
7. confidential 运行时是否服务外部系统 App（第一方 SPA 维持 public）？
```

## 6. 门禁建议（仍停在 Grill）

1. **阶段：** 当前唯一合法阶段是 `G-grill-with-docs`。design-tree 旧 frontier 的 consensus **可保留为已答记录**，不等于新口径已锁定，更不等于授权 S-spec。`.status.json` 现有 blockers（不得进 S-spec、不得实现、不得碰 notify change）保持。
2. **本轮允许做的事：** 用 §3 / §5 替换 CONTEXT 与 goal-plan 的最新态段落；把 §4 写进开放问题表；在 Grill 访谈中向 CTO 逐条提问并记录书面答。
3. **本轮明确不要做的事：**
   - 不改产品代码、不 push、不 PR
   - 不声称可进 S-spec / T-tickets / I-implement
   - 不把外脑 reply 当 accepted ADR
   - 不保留「仅第一方 / NoExternalIdP 当产品定位 / P2 真正外部第三方 / Pending=Q1–Q4」作为有效最新态
   - 不写历史演变对照长文进 CONTEXT / goal-plan 正文
4. **晋升条件（仍非本轮）：** CTO 书面回答 §4 中 P0-1…P0-7 的足够子集（至少 P0-1、P0-2、P0-3、P0-4），CONTEXT 按 §5.1 升为 Grill-confirmed，然后才由 CTO **另行开放** S-spec。未开放前 `ready_for_execution` 必须为 `false`。
5. **评审结论一句话：** 协议模块能用，产品槽位要改写；先问清默认提供方怎么被看见、外部 App 接到哪一步，再谈 Spec。