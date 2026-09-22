# 当前 change 的架构决定

本文件中的 accepted 仅指用户已明确的设计合同，不表示代码已实现。尚需选择的方案记录在设计树和评审建议中，不作为 accepted ADR。

## ADR-001: 系统账户是唯一用户真相源

**Status:** accepted
**Source:** LOG-003 / 用户原始请求及补充
**Supersedes:** none

### Context

SSO 为外部应用服务容易诱发独立用户目录或双写账户，导致密码、停用状态和管理操作分叉。用户明确要求继续使用现有 sys_* 体系。

### Decision

身份、密码及账户生命周期继续由现有 System 账户能力拥有。SSO 注册复用统一注册用例并写入 sys_user 和必要的现有关系，不创建 sso_user、不复制密码、不依赖异步同步才能在用户管理出现。SSO 协议对象不属于第二套账户，可按实际需要存储授权、同意和会话引用。

### Trade-off

接受通过公开 API 适配现有账户规则的成本，放弃重新建立独立身份目录带来的实现便利。账户一致性优先于局部模块独立性。

### Consequences

用户管理的查看、停用、删除、密码重置必须与 SSO 生效闭环一致；保留 Client 准入、RBAC 和数据权限，不能通过自动授予管理域解决“账户可管理”。跨业务模块仅使用 wta-api 或明确 common SPI。

### Verification / Migration

真实数据库注册后在用户管理中查询同一个 user_id；停用、重置及删除联动认证与会话；并发注册不能创建两份账户。现有账户不复制、不批量重建。

## ADR-002: 内外应用统一 OIDC 协议

**Status:** accepted
**Source:** LOG-001 / 用户原始请求
**Supersedes:** none；本 change 演进历史延期范围，见下文

### Context

历史版本只提供项目内部可消费的 code + PKCE + Sa-Token。用户现在要求内部 App 与外部平台都能接入同一套 OIDC 认证服务。

### Decision

内部与外部使用同一个授权服务器协议合同和账户认证核心，均通过授权码流程接入；不能让内部走专用密码换票、外部另建身份服务后声称协议统一。public/confidential 由客户端保密能力决定，不能等同于自有/外部。不同 Client 的权限、准入、scope、同意与密钥策略可以不同，不能因此分叉协议。

### Trade-off

接受标准协议、令牌验证、客户端迁移和互操作验收成本，避免只包装现有 JSON 接口而使外部依赖 WTA 私有 SDK。

### Consequences

历史永久决策 <Path>{roots.state}/specdev/adr/0069-oauth-authorization-code-pkce-s256.md</Path> 中 OIDC 延后、<Path>{roots.state}/specdev/adr/0073-sso-phasing-independent-sso-web-origin.md</Path> 中 OIDC 不在当前阶段的范围，在本 change 被用户新要求推进；保留授权码、PKCE、账户归属和安全隔离。永久文件本轮只读。

现有 access_token 为目标 Client Sa-Token 的合同来自 <Path>{roots.state}/specdev/adr/0070-access-token-is-sa-token-target-client-extras.md</Path>，本决定没有自行撤销它。标准 OIDC 允许 opaque access token；是否新增业务会话桥接及资源权限模型必须显式决定并验证。

### Verification / Migration

Admin、Home、外部 public RP 和 confidential RP 使用相同 issuer、标准协议及统一账户，配合 Client 认证和越权负向测试。旧消费者迁移必须有兼容窗口或明确同版切换条件。
