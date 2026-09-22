---
schema_version: 1
artifact: source
change: 2026-09-21-wta-sso-oidc-upgrade
source_type: conversation
canonical_locator: "conversation:current-user-request-and-followup"
captured_at: "2026-09-21T13:44:45.097878+00:00"
content_sha256: "d2b0649888883449e5dd770dbb2d98f48fe8e8e93d92f9072a19d7c319b8f982"
remote_state: not-applicable
close_capability: not-applicable
---

# WTA SSO OIDC 升级来源

## Capture Metadata

content_sha256 对应原始会话请求与补充的 UTF-8 字节快照：<Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/user-request.txt</Path>；该摘要不代表尚未取得的 Claude artifact。

- 捕获时间：2026-09-21，Asia/Shanghai。
- 来源：当前会话用户请求及补充；外部参考 <Url>https://claude.ai/artifact/Hch35DbeS3JLRob7fJbCBz</Url>。
- 激活入口：<Path>{roots.workflows}/specdev/G-grill-with-docs/G-grill-with-docs.md</Path>。
- 本轮授权：创建新 change、读取来源、审查当前前后端和账户体系、持久化评审及完整改造方案。
- 原文完整性：会话请求已捕获；Claude artifact 正文暂未取得，不能把本地推导称为对该原方案的完整 review。

## Original Content

> 激活 speculo/workflows/specdev/G-grill-with-docs/G-grill-with-docs.md 创建一个新的change， 随后请你读取 https://claude.ai/artifact/Hch35DbeS3JLRob7fJbCBz 这里的完整内容，这是我完整规划的wta-sso的完整的优化改造升级计划，请你结合当前的实际情况。进行全面的review，随后进行落盘。最后能够达到我的要求。形成一个sso服务。即可提供给app使用同样也可以提供给外部的第三方平台进行使用。不管是内部用还是外部用。本质来说整体的登入注册认证流程和后端sso的交互都是一样的，没有任何区别。唯一的区别就是能否自己创建构建自己的登入注册界面{这个应该是开放的，可以运行第三方或者app构建自己的登入注册界面，只不过本质是走的SSO认证OIDC这一套，也可以通过跳转的方式到sso-web上进行认证鉴权OIDC。将这些确定的内容以及相关的方案，我给你的参考，经过你的review形成的最终的内容都持久化落盘到新的change里。这个change是完整的，前后端都完整构建了。并且将 admin-web home-web 里对应的第三方都是增加了wta-sso sso-web服务，点击之后，就是跳转到对应的sso-web进行相关的认证鉴权账密输入之后。就进行登入。}注意一下，wta-sso这套使用的账户体系等，都还是使用现在原本的 sys_* 这一套，没有区别。不需要额外创建。账户体系都是共通的，对于当前系统而言。避免重复开发，导致整体过于冗余。也就是说通过wta-sso登入注册的用户，理论上应该也会

> 在对应的sys 系统用户表等地方显示出来。可以对这个用户进行管理。

原文中的路径和 URL 保留原样用于输入保真，不作为规范化工件引用。

## Source Comments

### 已确认的输入含义

1. 内部 App 和外部平台都使用 WTA SSO 的同一 OIDC 认证核心。
2. 提供默认 sso-web，也允许构建自定义登录注册界面；凭据托管、跨域会话和受信 UI 的具体边界待审查后决定。
3. 账户真相源仍为 sys_user 及既有 sys_* 关系；SSO 注册后应在有相应权限的系统用户管理中可见、可管理，不另建 SSO 用户库。
4. 此 change 的目标交付范围覆盖后端、sso-web、Admin/Home 入口、管理面和完整验收。
5. 本轮使用 G 工作流先形成评审与设计工件；是否完成产品实现必须由真实源码和测试证明。

### 来源摄入缺口

工具读取外部链接失败，HTTP 和 Chrome 正常页面导航均返回 HTTP 403 / Cloudflare 访问验证页。已请求用户粘贴正文后逐项对照；本轮尚未收到。不得凭链接标题、旧 change 或助手推荐补造用户参考原文。实际访问记录见 <Path>{roots.state}/specdev/changes/2026-09-21-wta-sso-oidc-upgrade/evidence/source-access.json</Path>。
