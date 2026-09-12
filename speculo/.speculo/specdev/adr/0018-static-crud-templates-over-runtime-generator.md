# ADR-0018: 静态 CRUD 模板取代运行时代码生成器

- **Status:** Accepted
- **Date:** 2026-08-29
- **Source:** `2026-08-28-retire-runtime-code-generator` ADR-001 与 T-01 至 T-05 Evidence

## Context

父仓库 `<Path>docs/fm/</Path>` 已承载 Java、Vue、React、XML 和 SQL 的完整 CRUD 静态模板与开发约束。继续维护后端生成模块、前端管理界面、在线接口、菜单权限和生成器元数据，会形成第二套模板解释、运行维护面与合同漂移源。

## Decision

产品运行时不提供代码生成器。CRUD 开发由 AI 与开发者参考 `<Path>docs/fm/</Path>` 和工程规范完成；静态模板不进入产品 classpath，也不提供在线配置、预览或代码下载接口。重新引入在线生成能力必须形成新的架构决定，并为源码、合同、UI、权限、数据和模板建立完整 owner。

## Consequences

项目不再提供在线低代码体验，但消除了运行时代码、生成元数据和静态模板之间的重复能力与漂移成本。`docs/fm` 必须保持可校验并与当前 HTTP、数据和工程合同一致；产品构建图、API、UI、schema、菜单权限和当前 OpenAPI 不得重新引入旧生成器。
