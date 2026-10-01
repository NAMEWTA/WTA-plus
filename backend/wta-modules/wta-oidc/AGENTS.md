# wta-oidc

第三方 OIDC 提供方，所有身份来自 WTA 正常账户。

## 结构

`controller/adapter -> usecase -> service -> dao -> mapper -> XML`，公共身份与 SSO 会话只经 wta-api。独立 OIDC 应用不使用第一方 Client 准入。协议为机密 Web 授权码，默认 PKCE S256，不支持 refresh、Broker 或动态注册。

## 安全与验证

精确协议安全链与 Sa-Token 共存；数据库原子消费授权码，完整授权状态使用独立版本化 AES-GCM 状态键加密。签名键/状态键在管理页生成或导入，以启动根密钥 `AUTH_CONFIG_ROOT_KEY` 认证加密后保存 `oidc_key_material`；轮换保留旧公钥和解密键，不提供私钥读取/删除接口。缺少配置停止签发。禁止记录协议正文、查询参数或个人声明。

在 backend 运行 `./mvnw -pl wta-modules/wta-oidc -am test`；根目录运行模块 layered 检查。

## 数据库配置与退出

`/oidc/admin/service` 聚合 Provider 与中央 SSO 配置，SSO owner 通过公开 API 保存自身设置。`oidc_service_config` 是 Provider 参数唯一事实源；Redis 缓存与数据库版本/内容校验配合，旧 YAML 的 OIDC 业务参数不再生效。发行方、SSO页面来源和Cookie结构只在维护重启生效，`AuthorizationServerSettings` 与公开发现文档使用同一启动快照。启停与新凭据TTL在每个HTTP请求内冻结读取，已持久化授权截止不随配置变化延长。

首次无Issuer/密钥时仍允许普通本地登录和管理页配置；基座不携带默认私钥。管理者保持服务停用，保存结构参数并生成两种用途密钥，维护重启后再启用。已运行服务变更结构前先停止新认证并执行 `/oidc/admin/service/prepare-restart`，结束旧中央会话，等待关联退出任务完成。状态密文使用带keyId的版本化封装；发行方仍参与AAD，禁止把旧Issuer密文作为新发行方授权继续使用。

`sid` 为中央随机Cookie的SHA256摘要；公开ID Token、持久授权和Logout Token使用相同值。全退在SSO主库事务中撤销中央会话，通过同步公共SPI撤销OIDC授权并写 `oidc_logout_outbox`。外部RP只使用标准HTTP/JWKS，不访问内部数据库。应用配置增加 `backchannel_logout_uri`；未登记地址的第三方应用无法获得后端退出通知，管理接入时需填写可由Provider访问的地址。

退出worker发送短期签名 `logout_token`，含iss/aud/iat/exp/jti/events/sid；200/204完成，网络/429/5xx按退避重试，永久错误与次数耗尽保留FAILED供管理页显式重试。任务领取使用可恢复租约与结果CAS，旧工作者不能覆盖新租约。任务保留完整目标快照，不依赖已清理授权；授权只有中央全退已登记或从未签发ID Token时才允许到期清理。停止新签发不停止JWKS、退出、撤销和投递任务。

新增测试包含版本化状态键轮换及真实HTTP标准Logout Token发送；实际MySQL原子撤销/并发/租约验收由Admin组合测试 `SsoOidcLogoutMySqlIntegrationTest` 承担。测试必须显式使用自建隔离库；默认跳过外部依赖的用例不计为已验证。
