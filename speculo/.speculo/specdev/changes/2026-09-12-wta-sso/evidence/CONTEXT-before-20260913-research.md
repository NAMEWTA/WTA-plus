# WTA SSO — 领域上下文（最新态 · BRIEF 2026-09-13）

> **Authority:** CTO BRIEF `/workspace/share-research/inbox/CTO-BRIEF-20260913.md` > 本 CONTEXT > Spec > Goal Plan。  
> 冲突旧表述（含「NoExternalIdP / 仅第一方、无外置 IdP」）**作废**，不以历史演变叙述保留。

## Glossary

| Term | Meaning |
|---|---|
| **单点登录服务（自建）** | 本仓实现的第一方 SSO：统一账号多端登录；以 Authorization Code 类接入 |
| **第三方登录体系** | 登录提供方目录：含外接 social（如 Mask、GitHub）与**自建 SSO 提供方**；自建默认已接通、排最前 |
| **接入方** | 自有前端 App **与** 外部系统 App，均可注册为 Client 接入 |
| **Sa-Token（access_token）** | 换票后仍签发目标业务 Client 的现有 Sa-Token（若与 BRIEF 后续 Research 结论冲突，以 Research+CTO 晋升稿为准） |
| **运营三步** | 创建应用 → 配置回调 → 交付 client/密钥（自有 App 可读配置） |

## 概念卡

**DefaultFirstProvider：** 自建 SSO 是第三方登录列表中的默认优先提供方，不是「禁止一切外置 IdP」。

**MultiAudience：** 同一套账号密码服务多端；Client 可来自自有或外部系统。

**CodeFirst：** 新建子模块；P0 先主流 Authorization Code（+PKCE 等安全合同见已拍 F 项，Round3 待确认是否原样保留）。

## 开放语义

见 Round3 design-tree：`D-100`…`D-104`。进 S-spec 仍否（CTO 门禁），除非 CTO 另令。
