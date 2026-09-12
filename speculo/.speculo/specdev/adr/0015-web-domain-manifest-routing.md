# ADR-0015: 使用 WebDomainManifest 组装动态路由

- **Status:** Accepted
- **Date:** 2026-08-28
- **Source:** `2026-08-25-plus-ui-multi-app-domain-architecture` ADR-004

## Context

单一 `src/views` 全仓 glob 无法支持多个 App 只打包各自选择的页面，也无法在包边界下可靠诊断重复或缺失组件映射。后端菜单中的 component 字符串仍需要保持稳定。

## Decision

每个 Web Domain 通过公开入口导出 `WebDomainManifest`，提供 component key 到懒加载视图的映射及该领域的路由贡献。App 在编译期显式选择并汇总 manifest。后端 component 字符串是稳定业务键，不是 npm 包物理路径。

## Consequences

新增页面需要显式注册，但不同 App 能选择不同 Web Domain，并保持动态 import/code split。重复 key、未知 key 和未选择领域必须可诊断且 fail visibly；keep-alive 组件名语义继续保留。
