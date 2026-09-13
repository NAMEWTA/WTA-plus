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
| 外部系统 App | 接入本 SSO 的 RP / 业务 Client | 产品范围含之；P0 = 管理面可登记（D-111=B）；运行时 Code 流先自有 App；confidential 后置（D-116=B） |
| 外置 IdP 产品 | Keycloak / Casdoor / Logto / Hydra | 禁止作为用户目录 / 身份源 |

一句话防误读：**禁止再用「无外置 IdP」否定「可被其他 App 当第三方登录来接」；也禁止把「第三方登录提供方」理解成「用户目录外包」。**

账号归一与票据隔离写在同一句：同一套本仓账号密码，多端登录；换票后仍按**目标业务 Client** 签发 Sa-Token。登录归一化 ≠ 共享一张业务 Token。
