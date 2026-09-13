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
