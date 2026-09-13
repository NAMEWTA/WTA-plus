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
