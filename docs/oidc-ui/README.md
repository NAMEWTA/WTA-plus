# 第三方统一登录与共享 UI

WTA 是账户和用户资料的权威来源。第三方系统作为机密 Web 客户端，通过授权码流程登录；不需要在原有 `sys_client`、UserType 或角色目录中为第三方再建立一套登录域。

## 管理员使用

进入后台「系统管理 → 单点登录」，创建应用时填写名称和第三方提供的完整 HTTPS 回调地址即可。系统生成 Client ID 和 Client Secret；Secret 只在创建或重置当次显示，关闭窗口后不能再次读取。接入配置同时给出 Issuer、Discovery、scope 和客户端认证方式。

默认开放登录名、昵称、头像和邮箱。展开「可获取的用户信息」可以逐项选择账户、个人档案、企业档案字段。完整个人证件号和完整法人证件号必须单独勾选，批量选择不会开启它们。应用没有获准的完整证件字段不会被查询进应用内存。

高级设置支持多个退出完成地址、`client_secret_basic` / `client_secret_post` 和 PKCE 开关。默认使用 Basic 与 PKCE S256。关闭 PKCE 仅用于不支持它的传统后端客户端，客户端密钥仍然必需。

停用、删除、重置密钥和修改回调/认证方式/PKCE 会撤销原授权。修改字段白名单则立即缩小 UserInfo 的可见范围；新增字段须重新授权，不会扩张已有令牌权限。配置写入带版本检查，过期表单不能覆盖新设置。

原「SSO 应用」和 `/sso/oauth2/*` 合同保留供兼容接入；Admin/Home 新入口已改为标准 OIDC，详见 [接入说明](../oidc-app-integration/README.md)。

## 第三方接入合同

| 项目 | 合同 |
| --- | --- |
| Discovery | `{issuer}/.well-known/openid-configuration` |
| Authorization | `{issuer}/oidc/authorize`，`response_type=code` |
| Token | `{issuer}/oidc/token`，后端用密钥认证与 `code_verifier` 兑换 |
| JWKS | `{issuer}/oidc/jwks`，仅公开 RSA 公钥 |
| UserInfo | `{issuer}/oidc/userinfo`，Bearer opaque access token |
| Introspection / Revocation | `{issuer}/oidc/introspect`、`{issuer}/oidc/revoke`，仅操作本应用令牌 |
| RP 退出 | `{issuer}/oidc/logout`，真实 `id_token_hint`、当前浏览器会话及确认页 CSRF 校验 |
| 默认有效期 | code 300 秒；access / ID token 最长 600 秒，并受原 SSO 会话到期时间约束 |
| 签名 | RS256；RP 固定 issuer、audience、算法并校验签名、有效期、nonce |
| 主体 | 持久 UUID `sub`，与用户名修改无关，各应用引用同一 WTA 主体 |

RP 应自行生成和验证 `state`、`nonce` 及 S256 PKCE verifier。回调地址精确匹配，不支持通配符。只支持 `authorization_code`，不提供 refresh token、implicit、password grant、公开 SPA 客户端；外部身份源接入另由业务 social 编排处理。

ID Token 携带最小身份及 `auth_time` / `sid`；可选资料通过 UserInfo 获取。UserInfo 总是有 `sub`，其他字段同时满足 **授权时快照 ∩ 当前应用配置 ∩ 已授予 scope**。账户停用/删除、中央会话失效、应用停用或授权撤销都会阻止继续取资料。业务 Sa-Token 与 OIDC access token 不能互换使用。

| scope | UserInfo 字段 |
| --- | --- |
| `openid` | `sub` |
| `profile` | `preferred_username`、`nickname`、`picture` |
| `email` | `email` |
| `phone` | `phone_number` |
| `wta_person` | 嵌套对象 `wta_person`：认证状态/时间、档案编号、姓名、性别、出生日期、证件类型、脱敏/完整证件号、有效期 |
| `wta_enterprise` | 嵌套对象 `wta_enterprise`：认证状态/时间、档案编号、企业名称、信用代码、单位性质、法人姓名/证件、成立日期、营业期限、注册地址、经营范围、联系信息、注册资本、行业和网站 |

自定义对象内字段名去掉管理目录中的分组前缀，例如配置字段 `person_document_number_masked` 输出为 `wta_person.document_number_masked`，`enterprise_legal_document_number` 输出为 `wta_enterprise.legal_document_number`。未开放或不存在的资料省略，不伪造邮箱/手机已验证标记。个人与企业各读取当前有效的一份档案；不发布附件、草稿、历史、审核意见、内部角色或权限。

退出范围是当前中央登录会话及其关联应用会话。支持标准后台退出通知与持久重试任务；第三方 RP 须登记回调并处理已验证的 Logout Token。

## 运维配置

OIDC 默认关闭。全部业务配置、版本化签名私钥和状态密钥现由 MySQL 管理、Redis 缓存，环境只保留 `AUTH_CONFIG_ROOT_KEY`。旧 `OIDC_*` 和 `namewta.oidc.*` 不再生效。首次安装、维护重启、Admin/Home 两应用登记及旧配置迁移请按 [当前操作说明](../oidc-app-integration/README.md) 执行；旧验收工作记录仅代表对应历史版本。

新业务基座共 116 张表。已有数据库只应用审阅后的 canonical SQL 差异，不重放完整基座。迁移需安排旧中央会话退出后重新认证。

SSO Nginx 仅将明确的协议路径反代到后端，管理路径不经 SSO 站点暴露。协议响应不套业务 `R` 包装，且不记录请求/响应载荷；管理 POST 仍使用权限与审计控制。Secret 保存 BCrypt 摘要，访问凭据查询使用摘要，框架持久状态使用 AES-GCM 加密。

授权记录在原中央会话到期后保留 24 小时，由每分钟最多 500 条的有界清理任务删除；已撤销和未完成兑换的记录同样清理。稳定主体映射 `oidc_subject` 不参与清理。

## 代码与 UI 边界

- `wta-api`：账户、中央会话、资料投影公共合同。
- `wta-oidc`：应用策略、稳定主体、授权持久化与 Spring Authorization Server 协议适配；纳入 core/full 两种组合。
- `domains/oidc`：生成 OpenAPI 类型、传输边界校验和管理服务。
- `web-domains/oidc`：管理页面与表单，权限/提示/剪贴板由 Admin host 注入。
- `web-kit/ui-element`：共享主题、AuthPanel、StatusPanel；Admin/Home/SSO 共同消费，默认浅色并支持暗色。

新增提供方依赖为现有 Spring Boot BOM 管理的 `spring-boot-starter-security-oauth2-authorization-server`（其 POM 声明 Apache License 2.0），未另设版本覆盖或引入独立身份服务器。它增加 Spring Security 协议过滤链及相关运行库，core/full 均验证过打包和模块组成，full 产物完成实际启动验收；已有 Sa-Token 继续负责业务 API。MySQL、Redis、Element Plus 均复用现有依赖。授权码验证、客户端认证和令牌框架采用该组件，WTA 自身负责账户准入、字段策略和持久状态一致性。

品牌色为 `#409EFF`；承载白色正文的实底主要按钮使用满足对比度的 `#2B6BD3`。页面使用共享语义变量，跟随已有 Admin 设置，避免三套独立配色。

参考仓库为用户指定的 `cde-oidc-login`，审阅版本 `8cdc1cb`；实现按本仓账户、档案和分层合同重新组织。协议依据 [OIDC Core](https://openid.net/specs/openid-connect-core-1_0.html)、[Discovery](https://openid.net/specs/openid-connect-discovery-1_0.html)、[RP-Initiated Logout](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) 和 [OAuth 安全实践 RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)。

验收命令、执行结果及未执行项记录在 [工作记录](worklog.md)。
