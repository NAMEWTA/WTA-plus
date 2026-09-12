# ADR-0020: App 自有导航状态与共享菜单纯运行时分层

- **Status:** Accepted
- **Date:** 2026-08-29
- **Source:** `2026-08-28-plus-ui-shared-navigation-permission-runtime` ADR-002、T-03、T-04、T-07

## Context

服务端菜单的递归缩窄、ParentView 展平、空 children 清理、组件键装配和重复名称诊断遵循同一后台菜单合同，可以跨 App 复用；Router、Pinia、侧栏、顶栏、布局、白名单和反馈呈现则属于具体 App。共享完整 Store 会强迫所有终端采用 Admin 导航形态，在每个 App 复制菜单算法又会造成协议漂移。

## Decision

Platform App Runtime 只提供无 Pinia、无 Router 单例、无 UI 依赖的服务端菜单递归模型、纯转换和稳定诊断。具体 App 通过显式 adapter 将共享投影连接到自己的 Router，并继续拥有 navigation Store、布局投影、manifest 选择、特殊宿主组件和诊断呈现。不得建立公共 Router、公共 Pinia navigation Store，或让 Platform 返回框架 Router 类型。

## Consequences

共享菜单流程具有确定性输入输出和独立测试，不同 App 可以选择自己的状态容器与导航形态。代价是每个 App 保留一层明确的 Router adapter 和少量组合代码；领域输入缩窄必须在 Domain 边界完成，不能以双重断言穿透到 Router。
