---
schema_version: 3
artifact: spec
change: 2026-08-27-plus-ui-backend-aligned-domains
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:前端 domain 第一层与后端模块一致，资源目录可从 Controller 稳定定位
  - CODE:<Path>plus-ui-namewta/packages/domains/**</Path>
  - CODE:<Path>ruoyi-vue-plus-namewta/ruoyi-modules/**/controller/**</Path>
---

# Spec: 前端领域按后端模块与 Controller 资源对齐

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-08-27-plus-ui-backend-aligned-domains/spec.md</Path>`

## 1. 问题与目标

### 问题陈述

当前前端领域第一层混合使用后端模块名、业务语义名和管理端用途名。开发者从 `ruoyi-system`、`ruoyi-gen` 或具体 Controller 出发时，无法确定性定位其 API、领域模型和 Vue 页面。

### 目标用户与场景

前后端开发者需要从后端 Maven 模块、Controller 基础路径或动态菜单组件键，直接定位唯一的 domain、web-domain 与 App 组合入口。

### 成功标准

- domain/web-domain 第一层使用去掉 `ruoyi-` 前缀后的后端模块标识。
- Controller HTTP 资源在包内拥有唯一 kebab-case 目录和显式公开入口。
- Admin 与 Client 既有登录、菜单、权限、页面和请求行为不变。
- 旧语义包和兼容门面清零并删除。

### 非目标

- 不修改后端接口、HTTP method、权限标识或菜单组件键。
- 不激活移动端、小程序或 Taro 占位包。
- 不重新设计页面布局、样式或业务交互。

## 2. 解决方案与外部行为

### 解决方案摘要

采用“一个后端模块对应一个 domain npm 包；一个 Controller HTTP 资源对应包内一个目录”。`admin/system/gen/workflow/demo/ai` 成为 canonical domain 标识；`web-domains` 使用同名标识并镜像资源目录。跨模块登录流程通过注入端口组合，不再由一个多后端归属包直接拥有全部请求。

### 主要流程

开发者从 `/system/client` 或 `SysClientController` 定位到 `domain-system/client`，从动态组件键 `system/client/index` 定位到 `web-domain-system/client`；App 仍显式选择 domain 和 manifest。

### 边界、失败与稳定错误行为

未知组件键、未选择领域、重复注册和缺少权限继续失败关闭。后端仍是最终授权边界，前端目录变化不得改变任何认证或授权语义。

### 状态转换与不变量

迁移前后 HTTP path/method、请求字段、响应映射、Client header、会话命名空间、动态菜单组件键、权限字符串与页面行为保持不变。

## 3. 用户故事

- **US-001**：作为前后端开发者，我希望后端模块与前端 domain 一一对应，以便无需记忆语义别名。
- **US-002**：作为前端 App 开发者，我希望按资源子路径组合能力，以便只选择终端需要的 API 和页面。
- **US-003**：作为维护者，我希望架构检查阻止多后端归属 domain、旧包回流和深层导入。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 查看工作区包 | 枚举 domains/web-domains | canonical 第一层为 admin/system/gen/workflow/demo/ai，旧 identity-access/system-admin/devtools/operations 不存在 | workspace 与 architecture tests |
| AC-002 | 给定后端模块和 Controller base path | 查找前端实现 | 每个已迁移资源可确定定位到唯一 domain 目录、类型、API 和 web 页面 | resource manifest 与目录测试 |
| AC-003 | Admin 登录并加载后端菜单 | 完成身份、菜单与路由装配 | 路由、页面、权限和失败关闭行为与迁移前一致 | unit、build、Playwright |
| AC-004 | Client App 启动 | 组合登录与 demo 能力 | 不依赖 Admin App，Client 会话与组合仍独立 | architecture、build、Playwright |
| AC-005 | 扫描 API 合同 | 对照 OpenAPI 与 transport tests | HTTP path/method、字段与映射无行为变化 | openapi check 与 domain tests |
| AC-006 | 阅读仓库文档与 Skill | 按新增领域流程导航 | 中文文档只描述新包名、资源映射与组合入口 | 文档扫描与人工 review |

## 5. 范围

### IN

前端 domain/web-domain 包重命名与资源拆分、App 组合迁移、包 exports、锁文件、架构检查、测试、中文 README 与前端开发 Skill。

### REUSE

现有平台端口、浏览器适配器、OpenAPI 生成物、动态路由装配器、权限求值器、页面实现和 E2E fixtures。

### OUT

- **OOS-001**：不改变后端 Java、数据库、OpenAPI 来源或部署配置。
- **OOS-002**：不为旧包名提供 facade、re-export 或过渡依赖。
- **OOS-003**：不提取只有 Admin 一个消费者的外部控制台页面到通用包。

## 6. 已锁定实现约束

- **DEC-001**：目录对应 Controller 的稳定 HTTP 资源，不复制 `Sys`、`Flw` 等 Java 实现前缀。
- **DEC-002**：npm 包粒度停留在后端模块级，不创建一 Controller 一包。
- **DEC-003**：资源目录只创建实际需要的 `api/types/mapper/service/index` 文件，不生成空骨架。
- **DEC-004**：公开入口使用显式 exports，禁止 `./*` 与包间深层导入。
- **DEC-005**：迁移不保留兼容门面；全部消费者在同一候选中切换并删除旧包。

## 7. 数据、接口与兼容

- **公共接口变化：** 前端 workspace 包名与 exports 发生破坏性重命名；产品 HTTP 接口不变。
- **数据模型与持久化：** 不变。
- **兼容要求：** 不兼容旧前端包名；产品运行行为保持兼容。
- **迁移要求：** 原子迁移全部工作区消费者并证明旧包引用为零。
- **发布或运维影响：** 前端重新构建部署；无后端或数据迁移。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 登录、Client、Token、权限和安全 URL 校验不得弱化。
- **NFR-002 性能与容量：** 动态 import/code split 保留，不把所有页面改为 eager import。
- **NFR-003 可用性与可靠性：** 未选择和未知菜单继续失败关闭。
- **NFR-004 可观测性与运营：** 架构检查与资源映射成为可重复审计证据。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| workspace/architecture | 静态架构 | AC-001、AC-002、AC-004 | `pnpm architecture:check && pnpm architecture:test` | 命令结果 |
| OpenAPI/domain tests | 合同与单元 | AC-002、AC-005 | `pnpm openapi:check && pnpm test` | 测试结果 |
| type/lint/build | 集成 | AC-003、AC-004 | `pnpm lint && pnpm typecheck && pnpm build:prod` | 命令结果 |
| 双 App Playwright | E2E | AC-003、AC-004 | `pnpm test:e2e` | 浏览器结果 |
| 文档与旧引用扫描 | 静态审计 | AC-001、AC-006 | `rg` 包名与英文文档扫描 | 扫描结果 |

## 10. 风险、假设与未决问题

### 风险

破坏性包重命名触及锁文件、App 组合、跨 domain public ports 和动态 manifest，必须在隔离候选中全量验证。

### 已采用的低影响假设

后端模块规范名采用去掉 `ruoyi-` 前缀后的 kebab-case；Controller 资源名优先来自稳定 base path。

### 未决问题

无。
