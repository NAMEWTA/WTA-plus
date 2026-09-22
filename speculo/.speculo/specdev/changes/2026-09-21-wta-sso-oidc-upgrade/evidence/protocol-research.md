# 协议内核与自定义认证 UI 研究

- Owner / caller：当前 change 的 G 主代理。
- 决定范围：标准 OP 内核、System 账户复用、Sa-Token 兼容、自定义 UI。
- 日期与版本：2026-09-21；Java 21、Spring Boot 4.1.0、Sa-Token 1.45.0；该 Boot BOM 管理 Security 7.1.0。
- 停止条件：取得一手证据、推荐与明确待验证项；本轮不加依赖、不启动组合应用、不声称通过兼容验收。

## R-001 · 标准协议内核

官方事实：Spring Security 7 已包含 Authorization Server。Boot 4.1.0 推荐 `spring-boot-starter-security-oauth2-authorization-server`，旧 `spring-boot-starter-oauth2-authorization-server` 已在源码标为 deprecated。在线滚动文档可能展示 7.1.1，实现应以当前 BOM 解析版本为准。

来源：<Url>https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/</Url>；<Url>https://github.com/spring-projects/spring-boot/blob/v4.1.0/starter/spring-boot-starter-security-oauth2-authorization-server/build.gradle</Url>；<Url>https://github.com/spring-projects/spring-boot/blob/v4.1.0/starter/spring-boot-starter-oauth2-authorization-server/build.gradle</Url>。仓内版本源：<Path>backend/pom.xml</Path>。

建议：优先适配维护中的协议内核，减少手写 OIDC；不同时加入旧 SAS 1.5 与 Security 7 两套内核。置信度：框架能力高；本仓组合尚未运行。

## R-002 · System 身份不需迁移到新用户库

官方事实：AuthenticationProvider 可接入自有认证服务，示例内存用户并非框架要求。代码事实：现有 SsoIdentityService 已提供密码验证、Client 准入和业务用户构建。

来源：<Url>https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html</Url>；<Url>https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/getting-started.html</Url>；<Path>backend/wta-api/src/main/java/org/namewta/sso/api/SsoIdentityService.java</Path>。

建议：继续复用 sys_*；注册和生命周期补充最小公开合同。不能将引入认证框架误解为新建用户表。置信度高。

## R-003 · opaque token 与协议状态

官方事实：框架支持 REFERENCE token 与自定义 token generator；RegisteredClientRepository、OAuth2AuthorizationService 和 ConsentService 可以适配现有存储。

来源：<Url>https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/core-model-components.html</Url>。

推断与建议：Sa-Token 字符串可能作为 opaque token 适配，但生成字符串不等于已经实现 scope/resource、UserInfo、introspection、refresh 和撤销。客户端映射 sys_client；授权/consent/refresh 数据可新增，不能产生第二套账户或客户端真相源。生产使用持久密钥，不能照搬启动时随机生成 key 的演示配置。置信度：扩展接缝高，实际 Sa-Token 兼容中，须实验。

## R-004 · UserInfo 与 public refresh 的真实限制

官方事实：默认 OIDC 配置的 UserInfo 配套 JwtDecoder，现有 opaque Sa-Token 不能直接假定可用；7.1.0 UserInfo provider 接受 AbstractOAuth2TokenAuthenticationToken 并查询授权记录，提供适配可能。7.1.0 默认 RefreshTokenGenerator 不为 `authorization_code + client authentication none` 签发 refresh token。

来源：<Url>https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/configuration-model.html</Url>；<Url>https://github.com/spring-projects/spring-security/blob/7.1.0/oauth2/oauth2-authorization-server/src/main/java/org/springframework/security/oauth2/server/authorization/oidc/authentication/OidcUserInfoAuthenticationProvider.java</Url>；<Url>https://github.com/spring-projects/spring-security/blob/7.1.0/oauth2/oauth2-authorization-server/src/main/java/org/springframework/security/oauth2/server/authorization/token/OAuth2RefreshTokenGenerator.java</Url>。

建议：若承诺 SPA/native refresh，先验证生成、刷新客户端处理、rotation/replay 的完整链，或显式采用 BFF。不可给 browser 分发保密 secret 以绕开限制。置信度高；未在本仓运行。

## R-005 · 自定义 UI 的标准边界

官方事实：OIDC 定义 OP 与 RP 的认证请求/响应，不规定 OP 内部如何完成用户认证。认证页面可自定义；任意第三方页面采集密码的通用 headless API 不属于 OIDC Core。

来源：<Url>https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest</Url>。

推断与建议：开放受控 OP 界面开发与开放 RP 直接收集统一密码是不同信任决定。后一种即使最终仍返回 code，也属于额外认证交互合同；它不自动等同于 password grant，但不能用“标准 OIDC”掩盖凭据风险。置信度高。

## R-006 · OAuth 与原生应用安全基线

官方事实：RFC 9700 禁止 resource owner password grant，授权端点不应为 XHR 开 CORS；RFC 8252 的原生应用最佳实践使用外部 user-agent 与 PKCE。

来源：<Url>https://www.rfc-editor.org/rfc/rfc9700.html#section-2.4</Url>；<Url>https://www.rfc-editor.org/rfc/rfc9700.html#section-2.6</Url>；<Url>https://www.rfc-editor.org/rfc/rfc8252.html#section-5</Url>。

建议：默认浏览器跳转和原生系统认证会话。OP 自己的密码登录不是 password grant，安全 browser tab 也不等于可读取页面/密码的应用 WebView；文档须准确区分。置信度高。

## R-007 · 本仓嵌入与审计 Gate

代码事实：现有 SecurityConfig 已有 Sa-Token 全局上下文 Filter、MVC 登录与 Client 拦截；引入 Spring Security 会影响拦截顺序与会话来源。

来源：<Path>backend/wta-common/wta-common-security/src/main/java/org/namewta/common/security/config/SecurityConfig.java</Path>，约 68、86、143 行；项目 API-005 见 <Path>.agents/skills/engineering-standards/references/rules/api-errors-resources.md</Path>。

必须验证：协议与业务 URL 精确分流；业务 clientPk 隔离不回归；协议响应不被 R/全局异常重写；Cookie/CSRF/session fixation；Boot 4/Jackson 3 授权记录和 principal 序列化；真实数据库原子消费；Filter 所有的敏感 POST 审计满足工程约束。不能简单声称框架端点已加 @Log，也不能未经裁决豁免。此为规范适配验证，未发生规则放宽。

## 尚未闭合的技术事实

Sa-Token 与授权记录之间签发失败补偿、单一失效来源、public refresh、OP 会话与 prompt/max_age/auth_time、真实多进程密钥/存储行为尚未实验。未执行 OIDC conformance 或外部 RP 互通。框架官方能力与当前项目已实现能力必须分别描述。
