# Summary

**结论：当前 change 可以正式进入/继续 `G-grill-with-docs` 决策访谈，但尚不能宣称 Grill consensus，更不能进入实施。`ready_for_execution=false` 必须继续保持。**

本轮重点核查后，我建议 CTO 对 P0 采用以下总体拍板组合：

| 决策          | 推荐拍板                                                                                                             |
| ----------- | ---------------------------------------------------------------------------------------------------------------- |
| **CTO-Q1**  | **批准，但以“带 F-02～F-08 安全/边界补充的 P0”批准**：Authorization Code + PKCE S256 + `sso-web` + 扩展 `sys_client` + 本地登录并存       |
| **CTO-Q2**  | 每环境明确 **SSO Web Origin + SSO/Auth Origin + admin callback + home callback**；callback 必须精确匹配，生产 HTTPS，禁止 wildcard |
| **CTO-Q3**  | **P0 admin-web 与 home-web 均默认 `both`**，先灰度验证；稳定后再按 Client 切 `sso`                                                |
| **CTO-Q4a** | **P0 `wta-sso` 与 `wta-admin` 同 JVM / 同进程组装**；独立后端进程维持 P2                                                         |
| **CTO-Q4b** | **P0 `sso-web` 从一开始使用独立 Web Origin/DNS**；后端仍可同进程，通过入口代理形成独立 SSO 公网 Origin                                        |

这是一个很重要的拆分：

```text
Q4a 后端进程拓扑
wta-admin JVM
   └── wta-sso module                 ← P0 推荐：同进程

Q4b 浏览器 Web Origin
admin.example   home.example   sso.example
                         \       /
                          SSO Origin          ← P0 推荐：独立 Origin
                              |
                    reverse proxy / gateway
                              |
                     同一个 wta-admin JVM
```

因此，“**后端同进程**”与“**SSO 前端独立域名/Origin**”完全不冲突。现有 Goal Plan 把二者混在原 CTO-Q4 中，是本轮应立即纠正的决策建模问题。永久 ADR-0012 已允许不同 App 独立构建和部署，而 ADR-0013 又明确要求 browser/storage/runtime implementation 与 platform contract 分离，这与上述组合相容。

同时，本包的 change 目录中**没有 `design-tree.json`**。而 `G-grill-with-docs` 明确把 design tree、LOG、CONTEXT、ADR 作为 Grill 恢复与 consensus 的核心工件。因此即使 CTO 下一轮一次性接受全部推荐，在 design-tree 被本地 owner 正式建立/恢复、LOG 逐项记录并重新校验之前，也不应写成“Grill 已完成/consensus”。

---

# Findings / recommendations (evidence-backed)

## 1. CTO-Q1 — P0 总体方案

❓ **Q1 — 是否批准当前 P0 基线？**

### Option A — 原样批准当前草案

批准：

* Authorization Code + PKCE S256
* `wta-sso`
* `sso-web`
* 扩展 `sys_client`
* 保留 `/auth/login`
* `local | sso | both`

但不新增 F-02～F-08 所要求的安全和架构合同。

**不推荐。**

原因是现有草案能证明“选择了 Code+PKCE”，但还不能证明实现无法绕过 PKCE、无法重放 code、不会形成开放重定向，也没有锁定中央 SSO Session、跨模块调用和 revoke 语义。前一轮外脑已将这些定为 P0 缺口。

### Option B — 批准 P0 基线，同时把 F-02～F-08 纳入 P0 合同

**推荐。**

➡️ **建议 CTO 拍板：Q1 = YES，但批准的是增强后的 P0，而不是当前未闭合版本。**

P0 的产品方向不需要推倒重来。`source.md`、`ADR-001`～`ADR-005` 与 Goal Plan 已经形成正确主轴：

```text
统一认人
   ↓
Authorization Code + PKCE
   ↓
按目标业务 Client 换票
   ↓
仍签发现有 Sa-Token
   ↓
admin/home Token 保持 Client 隔离
```

真正需要补的是“判卷合同”和“模块边界”。

---

## 2. F-02 — PKCE 必须从“技术选型”升级为“不可绕过的安全合同”

现有 `ADR-001` 已明确 Authorization Code + PKCE S256，但 Goal Plan 的 Contract Coverage 仍只是 `planned`，不足以作为认证系统 P0 Gate。

❓ **F-02 — P0 是否把 PKCE/code 生命周期的负向行为全部定义为硬 AC？**

### Option A — 只验证正常授权成功

不推荐。

### Option B — 正向 + 负向 + 重放/并发矩阵全部成为 P0 AC

**推荐。**

➡️ **建议拍板 F-02 = Option B。**

S-spec 后续至少应具有以下硬判卷面：

| 合同                      | P0 必须结果                                 |
| ----------------------- | --------------------------------------- |
| `code_challenge`        | 缺失 → 拒绝                                 |
| `code_challenge_method` | 只接受 S256；`plain` / 未知值 → 拒绝             |
| `code_verifier`         | 错误 → 换票失败                               |
| 单次使用                    | 同一 code 第二次兑换 → 失败                      |
| 并发兑换                    | 同 code 并发最多一个成功                         |
| TTL                     | 过期 code → 失败                            |
| Client binding          | code 只能由原 `client_id` 兑换                |
| Redirect binding        | token 阶段必须匹配 authorize 时绑定的 redirect    |
| Redirect whitelist      | 精确 URI 匹配；禁止 `*`、prefix-match           |
| Challenge binding       | code 绑定原 challenge + method             |
| callback `state`        | 不可预测、严格校验、一次性消费                         |
| 敏感日志                    | code/verifier/token/secret 不进入普通日志和错误输出 |

这与项目 `SEC-005` 的 redirect/URL 负向测试和 `TEST-002` 的认证多 Client 负向矩阵要求一致。

**建议不要把具体 code TTL 秒数在 G 阶段凭空写死**；可以在 Spec 中形成可配置但有明确上限的行为合同。

---

## 3. F-03 — 当前“双 Client 互拒”只能证明隔离，不能证明 SSO

现有硬验收是：

> 同浏览器先进入 admin，再进入 home；两张 Token 的 `clientid` 不同，跨 Client 调用拒绝。

这个验收是必要的，但不足够。

攻击式反例非常简单：

```text
admin -> 用户输入一次密码 -> Admin Token
home  -> 用户再次输入密码 -> Home Token
```

最终依然可以得到“两 Token 不同 + 互拒”，却根本没有实现 Single Sign-On。

❓ **F-03 — 是否将 P0 硬验收拆成两个独立 Gate？**

### Option A — 保持当前单一硬验收

不推荐。

### Option B — SSO reuse 与 Client isolation 分别验收

**推荐。**

➡️ **建议拍板 F-03 = Option B。**

正式 Gate 应至少拆为：

**P0-SSO-REUSE**

```text
用户第一次在 SSO Origin 完成身份认证
        ↓
进入 admin 授权完成
        ↓
随后从 home 发起授权
        ↓
不得再次要求用户名/密码
```

**P0-CLIENT-ISOLATION**

```text
admin -> Admin Client Sa-Token
home  -> Home Client Sa-Token

admin.clientid != home.clientid
admin.clientPk != home.clientPk

Admin Token -> Home-only API = reject
Home Token  -> Admin-only API = reject
```

这样才同时证明：

1. 中央身份确实被复用；
2. 第二个 App 确实完成了换票；
3. 原有 Client 隔离没有被 SSO 破坏。

这也与永久 `ADR-0001` 的 Client authentication/authorization context 及 Client-scoped session invalidation 一致。

---

## 4. F-04 — `wta-sso` ↔ `wta-system` ↔ `wta-admin` 边界必须现在锁定

这是本轮最重要的架构拍板之一。

项目规则已经很明确：

* 新业务模块默认 `layered`；
* 新模块不得把 `wta-admin` 的组装例外复制过去；
* 业务模块跨 system 调用应依赖 `wta-api`；
* `ISys*`、Mapper、Entity、Controller、`ClientSessionService`、`ClientUserTypeAccessService` 都属于 system 内部实现面；
* `wta-admin` 可以穿透 system，但这是**仅限组装层的例外**。

因此以下实现必须被明确禁止：

```text
wta-sso -> SysClientMapper
wta-sso -> SysUserMapper
wta-sso -> ISysClientService
wta-sso -> ClientUserTypeAccessService
wta-sso -> PasswordAuthStrategy
wta-sso -> SysLoginService
```

❓ **F-04 — SSO 如何取得用户、Client 和登录准入能力？**

### Option A — `wta-sso` 直接调用 system/admin 内部服务

不推荐，违反现有模块边界。

### Option B — 稳定 API/Port + composition adapter

**推荐。**

➡️ **建议拍板 F-04 = Option B。**

推荐依赖模型：

```text
wta-sso
  Entry
    ↓
  UseCase
    ↓
  Service
    ├── 本模块 DAO
    ├── ClientRegistryPort
    ├── Identity/Auth Port
    └── Session/Token Port
             ↑
       stable wta-api / SPI
       or app-level adapter
             ↑
         wta-system / wta-admin
```

核心规则：

* `wta-system` 继续拥有 user / client / RBAC 数据；
* `wta-sso` 不拥有这些 system 数据；
* 跨 system 的稳定能力通过 `wta-api` / SPI / 明确 Port；
* `wta-admin` 只作为 composition root 负责装配 Bean；
* 如现有密码认证目前只能由 admin 组装层实现，可由 `wta-admin` 提供 `wta-sso` Port 的 adapter，而不是让 `wta-sso` 依赖 admin implementation；
* `LoginHelper` 等 `wta-common-satoken` 公共入口可按项目 common 合同复用，但不能因此绕开目标 Client 准入或 system public contract。

这项决定建议进入 change ADR，而不能留给实现 Ticket 自由发挥。

---

## 5. F-05 — SSO 中央会话必须有明确 backend owner

当前 CONTEXT 一方面说：

> `sso-web` ClientId=`sso`，会话键=`Sso-Token`

另一方面又说：

> SSO 域允许 HttpOnly Cookie。

这两句话目前没有真正闭合。

如果 SPA 通过 JS 得到 `Sso-Token` 再放到 localStorage/sessionStorage，它就**不是 HttpOnly Cookie**。

❓ **F-05 — 中央 SSO Session 谁创建、保存和读取？**

### Option A — `sso-web` 在 JS storage 保存 `Sso-Token`

不推荐。

会使“HttpOnly 中央会话”原则失效。

### Option B — `wta-sso` backend 创建中央 session，并通过 `Set-Cookie` 管理

**推荐。**

➡️ **建议拍板 F-05 = Option B。**

推荐合同：

```text
SSO 登录成功
     ↓
wta-sso backend
     ↓
创建 SSO 中央 Session
     ↓
Set-Cookie
  HttpOnly
  Secure (production)
  Host-only
  Path=/
  SameSite=Lax（基于 top-level redirect 模式的推荐默认）
     ↓
/oauth2/authorize 读取中央 Session
```

关键不变量：

* **SSO Cookie 不应设成父根域共享 Cookie**；
* admin/home 不读取中央 Cookie 作为 API Token；
* 业务 App 仍保持 Header Bearer；
* 中央 Cookie 不向 JS 暴露；
* `/oauth2/token` 换得的业务 access token 仍是目标 Client 的 Sa-Token；
* SSO Cookie 与 Admin/Home Token 生命周期分离。

Cookie 名称属于低影响实现细节，可在 Spec/Ticket 决定；但 **owner、HttpOnly、host-only、读取链路和生命周期语义必须在 G/S 阶段确定。**

---

## 6. F-06 — `packages/platform/auth` 不能拥有浏览器实现

永久 ADR-0013 已明确：

```text
platform = runtime-neutral ports/contracts
adapters = browser/runtime implementations
apps     = ClientContext / routing / deployment / composition
```

当前 Goal Plan 却直接写：

```text
packages/platform/auth
  -> startSsoLogin()
  -> handleCallback()
```

只要这些函数里面出现 `window.location`、storage、Web Crypto、URL navigation 或具体 HTTP client，就违反现有架构。

❓ **F-06 — OAuth/PKCE 前端能力如何分层？**

### Option A — 所有 OAuth browser logic 都塞 `platform/auth`

不推荐。

### Option B — platform 纯合同，adapter 浏览器实现，App 组合

**推荐。**

➡️ **建议拍板 F-06 = Option B。**

推荐模型：

```text
packages/platform/auth
  - OAuth / PKCE types
  - state machine / pure validation
  - ports
  - runtime-neutral pure logic

packages/adapters/*
  - Web Crypto
  - sessionStorage / transient state
  - HTTP transport
  - redirect/navigation adapter
  - browser URL parsing

apps/admin-web
apps/home-web
apps/sso-web
  - ClientId
  - callback route
  - ClientContext
  - concrete adapter composition
  - auth lifecycle
```

不强制现在新建某个固定 package 名称；真正硬约束是 **platform 不得反向拥有 browser implementation**。

---

## 7. F-07 — public / confidential 必须从 UI 字段变成协议语义

目前 intake 已要求客户端管理支持：

* public/confidential；
* SSO 开关；
* PKCE；
* secret 生成/重置；
* `sso_secret_hash`。

但还没有回答：admin/home 到底是什么类型，以及 confidential 在 `/token` 上如何认证。

### 首先可直接锁定的一点

`admin-web`、`home-web` 都是浏览器 SPA。

➡️ **建议明确：admin-web / home-web 在 OAuth 意义上都是 `public client`。**

因此：

```text
browser bundle 中：
  禁止 client secret
  PKCE S256 mandatory
```

这与 `SEC-003` “secret 不进入 browser bundle”完全一致。

❓ **F-07 — P0 是否真正支持 confidential runtime？**

### Option A — P0 只运行 public SPA client

* admin/home = public；
* confidential 字段可以先保留为模型能力；
* 真正 confidential token-endpoint authentication 延后到有 server-side/external consumer 的阶段。

**优点：** P0 更小、更符合当前“第一方 browser SSO”范围。

### Option B — P0 同时支持 public + confidential

* admin/home 仍是 public；
* confidential Client 必须在 token endpoint 做 server-side client authentication；
* secret 只保留 hash；
* 明文只在生成/轮换时展示一次；
* PKCE 对 confidential 同样保持，不允许它成为绕过 PKCE 的路径；
* 浏览器绝不能携带 confidential secret。

**如果 CTO 坚持 P0 客户端管理即要求 confidential 可运行，我推荐 Option B。**

### 本轮主推荐

➡️ **如果目标严格坚持当前 intake 的 Client 管理能力，建议 F-07 = Option B；如果目标是尽量压缩第一方 P0，则 Option A 更小。**

无论选 A 还是 B，下列结论都应立即 locked：

```text
admin-web = public
home-web  = public
browser secret = forbidden
```

另需把 `sso` Client 与 relying OAuth Client 区分：

> `sso` 是中央登录 Session 的身份上下文 Client，不应因为存在这条 Client 记录，就把它当作 admin/home 的目标业务 OAuth Client。

目标业务 Token extras 仍绝不能写成 `sso`。

---

## 8. F-08 — `/oauth2/revoke` 不能偷偷变成 SLO

当前 P0 包含 `/oauth2/revoke`，同时 SLO 明确在 P1。

因此必须回答：

```text
revoke(Admin Token)
到底删除什么？
```

❓ **F-08 — revoke 作用域**

### Option A — 注销该用户所有 Client + 中央 SSO Session

不推荐。

这实际上提前实现了 SLO，并与永久 ADR-0001 “只失效目标 Client 会话，其他 Client 保留”的既有原则产生冲突。

### Option B — 只撤销被指定的业务 Client token/session

**推荐。**

➡️ **建议拍板 F-08 = Option B。**

语义：

```text
revoke(Admin Token)
  -> Admin Token/session invalid

Home Token
  -> 保持有效

SSO central session
  -> 保持有效

再次进入 Admin
  -> 可以利用仍存在的 SSO Session 重新授权
```

而：

```text
中央 SSO Session
 + 所有业务 Client Session
 一并注销
```

属于 **P1 SLO**。

这既使 P0 的 revoke 行为可判卷，也与现有 Client-scoped invalidation 永久 ADR 保持一致。

---

# CTO Q2–Q4 可直接拍板版本

## CTO-Q2 — 环境 Origin / Callback 矩阵

原 Q2 仅写“SSO 对外域名和各环境 callback URL”，信息粒度仍不够。

建议把答案格式改为：

| Env     | SSO Web Origin | SSO/Auth Origin | admin callback | home callback | TLS               |
| ------- | -------------- | --------------- | -------------- | ------------- | ----------------- |
| local   | 待填写            | 待填写             | 待填写            | 待填写           | local policy      |
| dev     | 待填写            | 待填写             | 待填写            | 待填写           | HTTPS recommended |
| staging | 待填写            | 待填写             | 待填写            | 待填写           | HTTPS             |
| prod    | 待填写            | 待填写             | 待填写            | 待填写           | HTTPS mandatory   |

**当前上传包没有这些环境的权威 URL，因此本次外脑不能安全替 CTO/运维编造 literal domain。**

但可以先拍板下面的政策：

➡️ **推荐 Q2-policy：**

1. 每个环境有明确 SSO Web Origin；
2. callback 使用完整 URI 精确登记；
3. 禁止 `*`、通配子域和 prefix-match；
4. 生产只接受 HTTPS；
5. admin/home callback 独立登记；
6. redirect URI 变更后旧 code 不应获得扩大权限；
7. SSO Web Origin 和 Auth Origin 必须分别建模，即使最终二者相同。

---

## CTO-Q3 — admin/home 默认 authMode

### Option A — admin=`both`，home=`both`

**推荐 P0。**

优点：

* 最低切换事故半径；
* SSO 故障时仍有原路径；
* 能先验证 F-02/F-03 的完整协议；
* 可以单 Client 灰度；
* 不需要在 P0 第一天进行强制身份入口切换。

### Option B — 一个 `both`，一个直接 `sso`

适用于 CTO 明确要求尽快强制某端统一登录。

### Option C — 两个直接 `sso`

不建议作为 P0 初始默认。

➡️ **建议拍板：Q3 = admin `both` + home `both`。**

并同时定义三种模式的失败语义：

```text
local:
  本地登录允许
  SSO entry 不展示/不允许

both:
  本地 + SSO 均允许

sso:
  业务 Client 的本地 /auth/login 必须 fail-closed
  用户通过 SSO authorization flow 登录
```

这样“保留本地登录”表示系统整体仍支持 local/both，并不意味着 `authMode=sso` 的某个 Client 仍能绕过配置直接密码登录。

---

## CTO-Q4a — 后端进程拓扑

### Option A — P0 与 `wta-admin` 同进程

**推荐。**

理由：

* 当前 `wta-system` public APIs 本身即按同 JVM Spring 注入方式设计；
* Goal Plan 已把“独立 SSO 进程”放在 P2；
* P0 的真正风险在协议正确性、Client isolation 与 session semantics，不需要同时增加远程服务发现、RPC、部署和网络边界；
* `namewta.sso.enabled` 可继续作为组装开关。

➡️ **建议拍板：Q4a = P0 same-process。**

这不代表未来不能独立服务化，只是明确独立进程不是 P0 Gate。

---

## CTO-Q4b — `sso-web` Web Origin

### Option A — 与 admin/home 共用现有 Origin 的路径

例如概念上：

```text
https://existing.example/sso/
```

实现简单，但 SSO 身份边界、Cookie owner、独立发布和未来多 App 扩展更容易纠缠。

### Option B — P0 开始即独立 Origin

概念上：

```text
https://<sso-origin>/
```

**推荐。**

➡️ **建议拍板：Q4b = 独立 Web Origin/DNS。**

但必须注意：

**“sso-web 独立 Origin”不意味着“wta-sso 必须独立 JVM”。**

推荐生产拓扑：

```text
https://sso.<domain>
     |
     ├── static sso-web
     |
     └── /oauth2/* / SSO session routes
             |
       reverse proxy
             |
       wta-admin JVM
         + wta-sso
```

这样：

* 中央 Cookie 可以是 SSO Origin 的 host-only Cookie；
* admin/home 永远拿不到该 Cookie；
* admin/home 只接收自己的业务 Token；
* P0 后端仍保持同进程；
* 将来 P2 把 backend SSO 拆成独立服务时，不需要改变浏览器看到的 SSO Origin。

这也是我认为 Q4a=`same-process`、Q4b=`independent-origin` 最有价值的组合。

---

# 建议的 Grill frontier

如果本地 Lead 要把本报告真正转成 G-grill design tree，我建议本轮 frontier 至少形成以下节点，而不是只保留原始四问：

```text
D-Q1   P0 baseline approval
  ├─ D-F02 Code/PKCE security contract
  ├─ D-F03 true SSO reuse acceptance
  ├─ D-F04 module/API boundary
  ├─ D-F07 public/confidential semantics
  └─ D-F08 revoke scope

D-Q2   environment Origin/callback matrix
  └─ D-F05 SSO session/Cookie owner

D-Q3   authMode defaults and fail-closed semantics

D-Q4a  backend process topology
D-Q4b  sso-web Web Origin
  └─ D-F05 Cookie Domain/SameSite/origin behavior

D-F06  frontend platform/adapters/apps ownership
```

依赖关系的关键点是：**F-05 的 Cookie 具体行为依赖 Q2 与 Q4b；不要在这两个问题仍开放时假装 Cookie 已经完全确定。**

---

# Proposed artifacts to promote into SpecDev (paths relative to change/)

| Path                      | 建议修改/新增                                                                                                                                         | 当前状态                                   |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| `design-tree.json`        | **必须创建/恢复**；建立 CTO Q1–Q3、Q4a/Q4b、F-02～F-08 节点、依赖和推荐答案                                                                                           | 当前 pack 中缺失；这是 Grill consensus blocker |
| `LOG.md`                  | CTO 每个实际拍板分别追加一个 LOG；不要把 10+ 个决定压成一条                                                                                                            | 当前只有 precursor/external-brain 记录       |
| `ADR.md`                  | ADR-001 补 Code/PKCE 生命周期约束；ADR-003 补跨模块边界；ADR-005 把 Q4 拆成 process topology / Web Origin；新增或拆分 Cookie Session、public/confidential、revoke/SLO ADR | 仍应保持 proposed，直到用户/CTO实际确认             |
| `CONTEXT.md`              | 增加 `Authorization Code`、`SSO Session`、`Business Session`、`Public Client`、`Confidential Client`、`SSO Origin`、`Auth Origin`、`revoke`、`SLO` 等规范术语  | 当前 glossary 尚不足以承载 F-02～F-08           |
| `spec.md`                 | **后续 S-spec 创建**：承载 PKCE 负向矩阵、SSO reuse、Client isolation、redirect/state、authMode、Cookie、revoke、Client type 等可观察 AC                              | 现在不要伪造                                 |
| `tickets-map.md`          | T-tickets 后再把 AC 映射到真实 Ticket/Evidence                                                                                                          | 当前 outline 状态合理                        |
| `goal-plan.md`            | 把原 Q4 改为 Q4a/Q4b；Q2 改成环境 Origin/Callback 矩阵；Hard Gate 拆 SSO-REUSE + CLIENT-ISOLATION                                                            | 当前仍是 precursor                         |
| `.status.json`            | 继续保持 `current_work=G-grill-with-docs`、implementation not-authorized、ready false；design-tree/CTO 决策闭合前不得声称 consensus                             | 当前总体方向正确                               |
| `external-brain/reply.md` | 可晋升保存本轮报告作为 Grill 输入证据；**不自动令 ADR accepted**                                                                                                    | 外脑证据，不是决策权威                            |

建议避免创建另一个平行的“SSO 总技术方案.md”作为长期权威，否则会与 ADR/CONTEXT/Spec/Goal Plan 产生双写和漂移。

---

# Risks / open questions

**P0 blocker 1 — `design-tree.json` 缺失。**
按照当前 `G-grill-with-docs` 工作流，这是恢复完整 frontier、记录依赖、判断 consensus 的核心工件。没有它，本轮最多只能说“形成外部 Grill 建议”，不能说 Grill 已走完。

**P0 blocker 2 — Q2 literal 环境值未提供。**
本包没有权威的 dev/staging/prod DNS、callback URL 或 TLS 入口数据，因此只能确定规则和矩阵结构，不能安全填写真实 URL。

**P0 blocker 3 — Cookie public topology 仍需与 Q2 literal values 对齐。**
如果最终 `SSO Web Origin` 与 `SSO/Auth Origin` 不同，那么 CORS、SameSite、credential mode 和 callback handling 都会改变。因此不要只记录“sso.example.com”，还必须记录 Auth endpoint 的浏览器 Origin。

**P0 blocker 4 — confidential 的 P0 范围需 CTO拍板。**
admin/home 可以直接确定为 public；但“P0 是否真正运行 confidential client authentication”仍存在两个合理选项。若本期没有 server-side relying client，压缩为 public-only runtime 会更小；若 CTO 要求 Client 管理在 P0 就完整支持 confidential，则必须同时定义 token endpoint client authentication。

**P0 blocker 5 — 现有 credential authentication 暴露面。**
上传材料显示当前 password strategy 位于 `wta-admin` 组装例外，而新 `wta-sso` 不得直接依赖它。正式 S-spec/T-ticket 前需要从真实源码确认可复用 public API/Port 是否已经存在；若没有，需要把新增稳定 Port 纳入设计，而不是复制 `PasswordAuthStrategy`。

**P1 / later — consent/scope。**
P0 intake 已出现 scope/auto-consent，而完整 consent UI 延后 P1。应明确第一方 P0 的 auto-consent policy，否则 scope 字段只有数据模型没有行为语义。

---

# Explicit "not verified locally" notes

* **not verified locally：** 本次仅静态阅读上传 ZIP 中的 `DISPATCH.md`、`MANIFEST.md`、`AGENTS.md`、SpecDev G-grill 工作流、相关 project Skills、change 工件和少量相关永久 ADR；没有把附件内容当成新的系统级指令。
* **not verified locally：** 未运行 `validate-specdev.mjs --stage grill`；由于 pack 中未见 `design-tree.json`，也不声称 Grill schema/frontier 已通过校验。
* **not verified locally：** 未运行 Maven、pnpm、单测、集成测试、Playwright 或任何 E2E，因此不声明 P0 hard acceptance、SSO reuse 或 Client isolation 已真实通过。
* **not verified locally：** 未运行 Git 命令核验 `b06d161`、`d1ce372`、祖先关系、branch、commit、worktree 或 cleanliness；这些仅是上传工件内的声明。
* **not verified locally：** MANIFEST 明确排除了 I-implement source trees，因此本报告不能从真实 Java/TypeScript 源码重新证明 `SecurityConfig`、`LoginHelper`、`SysLoginService`、实际 `sys_client` schema、当前 CORS/Cookie 配置或 build graph；相关事实目前以 intake、Skills 和已毕业 ADR 为证据边界。
* **not verified locally：** 未执行 commit、push、PR、merge、部署、DNS/TLS 修改、数据库迁移或远程环境操作。
* **not verified locally：** 未索取或读取 `.env`、API key、PEM/KEY、token、`~/.codex/auth.json` 或任何 secret。
* **not verified locally：** 本报告不能代表 CTO 已接受上述推荐，也不能代表 Lead acceptance、G-grill consensus、S-spec Ready、T-tickets Ready 或 I-implement 授权已经完成。

## 最终 G-grill 门禁意见

**可以继续 G-grill；不可进入 S-spec Ready，更不可进入实现。**

如果 CTO 一次性接受本报告中的推荐组合，则建议本地 owner 将其转录为：

```text
Q1   = YES, with F-02..F-08 hard contracts
Q2   = dedicated per-env SSO/Web/Auth + exact callback matrix
Q3   = admin both / home both for P0
Q4a  = wta-sso same process with wta-admin for P0
Q4b  = sso-web independent Web Origin from P0

F-02 = strict PKCE/code/state negative contract
F-03 = SSO-REUSE + CLIENT-ISOLATION separate hard gates
F-04 = wta-api/Port boundary; no system/admin implementation dependency
F-05 = backend-owned HttpOnly host-only SSO session cookie
F-06 = platform contracts / adapters browser runtime / apps composition
F-07 = admin/home public; browser secret forbidden;
       confidential runtime explicitly choose P0-support vs defer
F-08 = revoke one business Client only; SLO remains P1
```

完成 design-tree、逐项 LOG、ADR/CONTEXT 同步并得到真实 CTO/用户确认之后，才具备把 Grill 标为 consensus 并路由到 `S-spec` 的条件。
