# wta-oidc

第三方 OIDC 提供方，所有身份来自 WTA 正常账户。

## 结构

`controller/adapter -> usecase -> service -> dao -> mapper -> XML`，公共身份与 SSO 会话只经 wta-api。独立 OIDC 应用不使用第一方 Client 准入。协议为机密 Web 授权码，默认 PKCE S256，不支持 refresh、Broker 或动态注册。

## 安全与验证

精确协议安全链与 Sa-Token 共存；数据库原子消费授权码，完整授权状态使用部署独立 AES-GCM 密钥加密。签名读取持久 JWKSet 与 activeKid，缺少配置停止签发。禁止记录协议正文、查询参数或个人声明。

在 backend 运行 `./mvnw -pl wta-modules/wta-oidc -am test`；根目录运行模块 layered 检查。
