# Change Context：WTA-plus 全面审查

## 领域词汇

- **Client**：当前登录域/业务租户上下文；权限、角色、菜单和会话必须绑定它。
- **SSO**：第一方 Authorization Code + PKCE 和独立 Web Origin；匿名授权入口与业务 token 交换分开。
- **通知**：Intent/Delivery/Attempt/Outbox 与 provider callback 的持久状态机；Redis wake/claim/lease/fence 是唤醒和并发控制，不是跨表事务替代。
- **Profile**：个人/企业资料、认证申请、必填材料、OSS 引用和工作流审核的业务聚合。
- **发布**：前端 App、后端 bundle、MySQL 基座、Nginx/Compose 与 provenance manifest 的完整 artifact set。

## 架构词汇

严格使用 module/interface/depth/seam/adapter/leverage/locality。module 拥有 interface 和 implementation；seam 是可替换行为位置；adapter 填充 interface；deep module 用小 interface 隐藏大量行为，为调用者提供 leverage、为维护者提供 locality。不要把所有 `service`、`component`、`boundary` 或文件夹都当成这些概念。

## 当前事实

- monorepo 当前拥有 frontend/backend；初始 HEAD、branch、逐文件SHA在 `<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/worktree-baseline.json</Path>`。
- 结构清单在 `<Path>{roots.state}/specdev/changes/2026-09-14-wta-plus-comprehensive-review/evidence/repository-inventory.json</Path>`，包含 50 POM、247 backend Java test source、3 App、130 Vue 和大型文件清单；另有 1 个 speculo example test source。
- 证据按 backend/frontend/docs/platform 分报告，所有 runtime 未验证项必须保持 not-run/needs-runtime。

## 设计目标

删除真正消失的复杂性：日志规则、请求正文 owner、可信来源、lease、认证材料纵切片、workflow task generation、发布清单与重复事实分别拥有单一 seam。保留真实的外部变化：第三方 adapter、MySQL owner、Sa-Token/PKCE/HMAC/Outbox/OSS 保护和历史 archive。

## 用户交付状态

用户要求只写 change，之后亲自审核，可能交给其他 AI 实施。所有 Ticket/Spec/ADR 均是 draft；未提供用户选择或实施授权。