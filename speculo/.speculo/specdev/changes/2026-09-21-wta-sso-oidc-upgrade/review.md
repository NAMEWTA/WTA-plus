# WTA SSO 当前实现评审

评审日期：2026-09-21。基线 HEAD：`2b4fd1f4b0e8f8c3520788f35a08476d5ecb0257`；审查对象为当前工作树，包含已有工程画像更新。本轮为源码及协议文档审查，未运行产品测试、未验证线上环境。

**结论：方向可行，而且账户与 App 入口已有可复用基础；当前实现尚不能作为通用 OIDC Provider 对外使用。** 主要工作是将已有私有授权码适配升级为标准协议，补齐注册、客户端认证、会话失效和真实用户管理闭环。不是重新开发账户系统，也不是再添加一套同名登录按钮。

用户提供的 Claude artifact 未能读取正文。本文件是当前实现评审，不是对未知原方案逐条审阅后的最终结论。原方案的保留/修正/拒绝/待定对照仍缺输入。

## 已有能力与复用结论

| 范围 | 当前事实 | 复用方式与证据 |
|---|---|---|
| 账户认证 | SSO 已经读取 sys_user、校验 BCrypt、复用失败计数及临时密码 | 保留 System 账户 owner；<Path>backend/wta-admin/src/main/java/org/namewta/web/sso/AdminSsoIdentityService.java</Path>，约 41、70 行 |
| 注册 | 现有注册检查 Client 注册开关、登录域、手机号、验证码、密码策略；事务写 sys_user 和关系 | 将同一用例通过最小公开 API 提供给 SSO，不复制实现；<Path>backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java</Path>，约 57–104 行 |
| 授权码安全 | 已有随机 code、精确回调、S256、Client/verifier 绑定、过期及一次性消费 | 保留外部行为与负向测试，协议引擎替换时不保留两套可独立签发的授权核心；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>，约 71–143 行 |
| 会话隔离 | SSO 使用独立 Redis 会话和 host-only HttpOnly Secure Cookie，业务会话按 Client 签发 | 保留安全边界，补生命周期；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/adapter/http/SsoSessionCookie.java</Path>，约 24 行 |
| Admin/Home | 登录页已有 WTA SSO 入口和回调页，共用 PKCE/state 工具，各自存储 Token | 升级 OIDC 消费者；<Path>frontend/apps/admin-web/src/views/login.vue</Path>，约 80、303 行；<Path>frontend/packages/web-domains/admin/src/auth/LoginPage.vue</Path>，约 10 行；<Path>frontend/packages/platform/auth/src/index.ts</Path>，约 145 行 |
| 管理面 | 已有独立 SSO 应用管理、Client 配置、回调和密钥交付 | 扩展当前管理面；<Path>frontend/packages/web-domains/system/src/sso-app/SsoAppPage.vue</Path>，约 59、176 行 |
| 发布 | sso-web 是已登记的第三个 App，具有独立 HTTPS Origin 和 Nginx | 扩展现有三 App 发布合同；<Path>release-artifacts/apps.json</Path>，约 37 行。登记不表示已部署 |

## 必须处理的发现

以下 P0 表示对外开放前必须关闭的门槛；P1 表示本次完整升级范围中的重要缺口。静态可确认的实现缺口与未运行的攻击验证分开记录。

### F-01 · P0 · 当前 HTTP 和令牌合同不是标准 OIDC

authorize 返回 `R<{loginRequired,redirectUri}>`，token 接收 JSON 并返回项目 R 包装；字段为 `access_token/expire_in/client_id`。缺少标准 `token_type/expires_in/id_token`，没有发现 issuer、discovery、JWKS、UserInfo、nonce 和 scope 授权处理。

证据：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>，约 55–88 行；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/domain/vo/SsoTokenVo.java</Path>；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/domain/SsoOAuthCommands.java</Path>。

影响：通用 OIDC 客户端无法直接接入，增加一个 JWT 字段不能解决协议互通。建议使用维护中的标准协议引擎，提供标准重定向、表单换票、错误、ID Token 和发现能力；业务接口才保留项目 R 合同。外部规范依据见 <Url>https://openid.net/specs/openid-connect-core-1_0.html</Url> 与 <Url>https://openid.net/specs/openid-connect-discovery-1_0.html</Url>。

### F-02 · P0 · confidential 密钥配置尚未形成换票认证

管理面可创建 confidential 应用并保存/轮换 secret hash；目录适配没有填充该 hash，换票函数没有 Client 认证步骤，实际只凭 code 与 verifier。配置的 kind 并未产生相应安全保证。

证据：<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/sso/SsoClientFieldsSupport.java</Path>，约 91 行；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/sso/SystemSsoClientCatalog.java</Path>，约 28–45 行；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>，约 111–143 行。

建议：confidential 必须执行登记的标准认证方法；public 不放置所谓保密 secret。提供最小服务端验证接口，避免将密钥材料放到展示 DTO。必须有缺少密钥、错误密钥、其他 Client 密钥和轮换后的负向测试；PKCE 不能替代 confidential 客户端认证。

### F-03 · P0 · 配置项与运行行为不一致

授权代码未消费 scope、autoConsent；PKCE 实际始终要求，与配置开关语义不一致。当前本地 `/auth/login` 未以 `ssoAuthMode` 拒绝登录；把前端隐藏视为强制 SSO 会造成误解。管理页还把 public 标为“自有”、confidential 标为“外部”。

证据：<Path>backend/wta-admin/src/main/java/org/namewta/web/controller/AuthController.java</Path>，约 75 行；<Path>frontend/packages/web-domains/system/src/sso-app/SsoAppPage.vue</Path>，约 67–70、124–135 行。

建议：应用归属、客户端保密能力、允许的认证 UI、是否需要同意分别建模；每个管理字段必须有运行时效果和拒绝测试。`authMode=sso` 如被定义为强制入口必须由后端约束。表单里的历史 `grantTypeList=['password']` 是 WTA 本地登录策略，不得直接解释成允许 OIDC password grant。

### F-04 · P0 · SSO 注册与用户管理尚无完整旅程

已有注册确实写入 sys_user，但 SSO 公开 identity 合同没有注册，sso-web 只有密码授权页。用户管理列表具有部门/创建者数据权限；自助注册通常 `createBy=0`，不能承诺每个普通管理员都自动看到。

证据：<Path>backend/wta-api/src/main/java/org/namewta/sso/api/SsoIdentityService.java</Path>；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java</Path>，约 403–405 行；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/mapper/SysUserMapper.java</Path>，约 37 行；<Path>frontend/apps/sso-web/src/main.ts</Path>，约 8 行。

建议：统一注册用例的目标 Client 来自已验证的认证事务，不信任表单提交的 userType、role 或管理域。验收使用具备相应管理范围的管理员；必要的可见范围通过明确权限配置实现，不削弱全部数据权限。

### F-05 · P0 · 并发注册的数据库唯一性不足

现有注册先查用户名/手机号/邮箱，再插入；基座 `sys_user` 的用户名、手机号为普通索引，`sys_client.client_id` 也没有唯一约束。应用层查重不能证明并发唯一。

证据：<Path>backend/wta-admin/src/main/java/org/namewta/web/service/SysRegisterService.java</Path>，约 89–100 行；<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>，约 63–88、352 行。

建议：先固定用户名规范化、大小写、手机号/邮箱空值和软删除后的重用规则；扫描存量重复，再用数据库约束兜底并将冲突映射为安全的注册错误。不能直接对大量空字符串添加唯一索引，也不能自动合并不同人的历史账户。本轮未查询线上数据，因此不声称已有实际重复。

### F-06 · P0 · 改密与 SSO 会话失效不闭合

SSO Redis 只保存 sid → 用户，没有用户安全版本；current 直接读缓存。用户停用/删除清理已有业务会话，但未覆盖独立 SSO 会话；密码重置函数只更新密码。换票时重新查询状态会阻止已停用/删除用户获票，但不能补偿改密后旧 SSO 会话仍可使用，也不能保证恢复启用后旧会话不复活。

证据：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/adapter/store/RedisSsoSessionStore.java</Path>，约 27–41 行；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoSessionService.java</Path>，约 51 行；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysUserServiceImpl.java</Path>，约 480、534、717 行；<Path>backend/wta-admin/src/main/java/org/namewta/web/sso/AdminSsoIdentityService.java</Path>，约 70 行。

建议：账户安全版本或等价失效机制，加 user/client/session 关联，覆盖 sid、code、refresh family 和 WTA 业务会话。数据库提交与缓存事件的失败恢复必须有保障，不能只依赖可能丢失的异步通知。

### F-07 · P0 · 撤销和注销语义不足

revoke 直接按提交 Token 注销，没有校验 Client 归属；SSO logout 仅删除当前 SSO session；App logout 仅退出当前业务会话。它们均不能代表完整单点退出。Client 的删除、scope/回调/密钥变化也没有统一策略版本失效链。

证据：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/controller/anonymous/SsoOAuthController.java</Path>，约 97–101 行；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoSessionService.java</Path>，约 60 行；<Path>frontend/packages/domains/admin/src/index.ts</Path>，约 531 行；<Path>backend/wta-modules/wta-system/src/main/java/org/namewta/system/service/impl/SysClientServiceImpl.java</Path>，约 246、440 行。

建议：区分退出本应用、退出中央会话、撤销应用授权、全部设备退出；标准撤销检查调用 Client，保持不泄露 token 是否存在。外部 RP 的本地会话清理由 RP 协作，不能承诺一键物理删除外部 Cookie。

### F-08 · P0 · 自定义 UI 必须有明确的凭据与会话合同

用户已确认 UI 自主性，但尚未确认任意第三方都能采集统一账户密码。标准 OIDC 规定的是客户端授权交互，登录注册界面的内部交互需要另行设计。不能把直接提交密码换业务 Token 改名为 OIDC；密码 grant 已被当前安全最佳实践禁止。

建议比较：受控 SSO Origin 上的定制页面；经审核的自托管 headless 认证交互；任意第三方直接收集密码。最后一种风险最大，不作为推荐默认。受信自托管模式必须保持完整授权事务绑定，完成后仍经 SSO Origin 完成中央会话及 code 回调；浏览器第三方 Cookie 不能作为可靠前提。依据：<Url>https://www.rfc-editor.org/rfc/rfc9700.html#section-2.4</Url>。具体取舍由 D-005 决定。

### F-09 · P0 · 当前 Nginx 边界不能直接支持外部浏览器互通

独立 SSO Nginx 对 `/sso/` 请求统一检查 Origin，仅允许自身 Origin 或没有 Origin；根其他路径 404。新协议的 discovery/JWKS、外部 SPA token/UserInfo、认证交互路由必须同步调整。若 issuer 选择根 Origin，根发现路径需要显式路由；如果 issuer 带路径，发现地址按规范从该 issuer 派生，不能硬编码成根路径。

证据：<Path>release-artifacts/docker/frontend/nginx/apps/nginx-sso-web-tls.conf.template</Path>，约 6–10、49–60、76–77 行。

建议：端点级 CORS/Origin 矩阵；授权端点通过顶层导航，Cookie 会话端点保持严格 CSRF/Origin 检查，token/UserInfo 按登记的浏览器 Origin 开放所需跨域。保留 HTTPS、host-only Cookie，不能放开整个 `/sso/**` 的跨站凭据访问。

### F-10 · P1 · 三个前端还需完整认证旅程

sso-web 只有一个密码页面，没有注册、找回、同意、切换账户、退出及失效恢复流程；默认用户名为 `WTA`。Admin/Home 的 PKCE 封装没有 nonce、openid scope、ID Token 验证；Home 的 SSO 按钮还受旧本地验证码准备状态影响。

证据：<Path>frontend/apps/sso-web/src/views/AuthorizePage.vue</Path>，约 9、29 行；<Path>frontend/apps/sso-web/src/ssoApi.ts</Path>，约 40–58 行；<Path>frontend/packages/platform/auth/src/index.ts</Path>，约 56、122 行；<Path>frontend/packages/web-domains/admin/src/loginState.ts</Path>，约 28 行；<Path>frontend/packages/domains/admin/src/index.ts</Path>，约 419 行。

建议：现有 App 组合和 UI 结构复用，协议解析交由成熟客户端能力/适配器；OIDC 协议不能由每个 App 重写一份。纯 SSO 跳转不依赖本地验证码可用性。

### F-11 · P1 · 公开认证入口需要补齐滥用防护与严格验证

现有 SSO 密码入口没有复用本地验证码分支；错误区分用户不存在和停用；回调允许任意 HTTP；PKCE challenge 仅检查非空，verifier 未完整检查字符集；授权码明文落库。

证据：<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/domain/bo/SsoLoginBo.java</Path>；<Path>backend/wta-admin/src/main/java/org/namewta/web/sso/AdminSsoIdentityService.java</Path>，约 91–100 行；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/support/PkceS256.java</Path>，约 57–73 行；<Path>backend/wta-modules/wta-sso/src/main/java/org/namewta/sso/service/SsoAuthorizationService.java</Path>，约 183 行；<Path>release-artifacts/docker/infrastructure/mysql/init/10-cde-base-ddl.sql</Path>，约 1631 行。

建议：复用验证码/密码策略/限流；统一对外认证错误并保留脱敏审计；生产 HTTPS 与原生 loopback 例外分开校验；code/refresh 摘要存储和有界清理；处理重复参数、长度、非法字符、CSRF、开放重定向和登录事务重放。

### F-12 · P0 · 既有硬 E2E 不证明最终账户目标

普通 Playwright 配置排除 SSO 专用用例；已有 SSO HTTPS 和 release-origin fixture 虽使用真实 SSO/Redis/MySQL/浏览器，但 System 用户密码和业务 Token 签发由替身提供，不能证明真实注册、RBAC、账户管理或 OIDC 互通。

证据：<Path>frontend/playwright.config.ts</Path>，约 7 行；<Path>backend/wta-admin/src/test/java/org/namewta/test/sso/SsoHttpsSessionIntegrationTest.java</Path>，约 104、180、190、427 行；<Path>release-artifacts/tests/fixtures/sso-release-origin.py</Path>，文件头。

建议：保留这些有价值的隔离测试，增加真实 System + Sa-Token + 管理页面 + 外部标准 RP 的全旅程。不能用删除 fixture 测试或普通 E2E 全绿冒充目标完成。

## 历史决定的处理

- 保留：sys_* 账户、sys_client 注册目录、WTA Client 隔离、独立认证 Origin、PKCE 与禁用 password grant。
- 升级：历史 OIDC 延期范围，按用户本次要求进入当前 change。
- 待选择：现有 access_token 即 Sa-Token 的兼容路径、自托管认证 UI、完整生命周期和独立进程要求。
- 永久 ADR/context 本轮只读；本 change 的决定不冒充已经实现的项目永久知识。

## 结论的实际限制

未取得用户参考全文；没有确认生产域名、已登记外部消费者、存量账户重复情况、真实部署状态及 Token 撤销时延。本轮未测试任何用户密码、未修改线上账户，也未执行数据库迁移。推荐方案和验收清单见 <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/proposal.md</Path> 与 <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/acceptance.md</Path>。
