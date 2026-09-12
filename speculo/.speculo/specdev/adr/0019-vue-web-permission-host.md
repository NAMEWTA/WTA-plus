# ADR-0019: Vue Web 权限宿主与跨终端权限算法分层

- **Status:** Accepted
- **Date:** 2026-08-29
- **Source:** `2026-08-28-plus-ui-shared-navigation-permission-runtime` ADR-001、T-02、T-06

## Context

多个 Web Domain 页面依赖全局 `v-hasPermi` 与 `v-hasRoles`，但跨终端权限算法还需要被未来移动端和小程序复用。若 Vue 指令直接读取 Admin Store，Web Domain 会依赖具体 App；若指令进入 Platform，又会把 Vue、DOM 和 Store 语义带入跨终端层。权限配置异常还必须同时保持界面失败关闭和显式诊断。

## Decision

跨终端角色与权限求值继续由无 Vue、无 DOM、无 Store 的 Platform Permission 提供。独立 Web Kit 通过注入 `AccessEvaluator` 创建并注册 Vue 权限指令，不读取具体 App Store；具体 Vue App 从自己的会话状态构造求值器并显式安装宿主。非法绑定或 evaluator 缺失时，指令先从宿主树移除受限元素，再原样抛出稳定错误。

## Consequences

Vue Web App 可以复用同一权限宿主，非 Web 终端只依赖权限算法，Web Domain 不再指向 Admin 私有实现。代价是每个 App 需要少量显式安装和会话接线，并维护一个独立 Web Kit 包。前端可见性仍不替代后端鉴权。
