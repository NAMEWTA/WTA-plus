---
schema_version: 3
artifact: spec
change: 2026-09-08-richtext-third-oss
status: ready
ready_for_tickets: true
sources:
  - USER-DECISION:2026-09-08 activate T-tickets for rich-text, third observability and OSS CORS
  - DIAG-2026-09-08-richtext-third-oss
---

# Spec: 富文本、三方调用明细/统计、OSS 直传稳定性修复

- **Spec：** `<Path>{roots.state}/specdev/changes/2026-09-08-richtext-third-oss/spec.md</Path>`

## 1. 问题与目标

### 问题陈述

1. 富文本演示页在列表刷新或保存后会把会话上下文问题放大成未处理的 500 和 `TransportError`。
2. 三方调用明细和调用统计页把空筛选条件当成错误，直接进入 500。
3. OSS 直传和预览在开发环境里需要针对前端 Origin 手工改 MinIO CORS，前后端地址变化后反复调参。

### 目标用户与场景

- 富文本演示使用者希望在会话上下文不足时看到明确错误，而不是页面抛出未处理异常。
- 三方管理使用者希望直接打开调用明细和调用统计页，空筛选也能看到最近数据。
- 本地开发和联调使用者希望前端 Origin 变化时，不必反复修改 MinIO 的 IP 绑定式 CORS 配置。

### 成功标准

- 富文本页在缺少登录 Client 时返回可识别的业务错误，前端不再出现未处理 promise rejection。
- 调用明细和调用统计页在 `providerCode` 为空时依然返回 200，并展示最近数据。
- 本地/开发 OSS 直传不再依赖固定前端 IP；调试与预览可以通过配置默认值或文档约定直接工作。

### 非目标

- 不重做登录体系。
- 不改三方供应商、Endpoint 或统计模型。
- 不变更 OSS 对象生命周期、Bucket 权限模型或生产发布流程。

## 2. 解决方案与外部行为

### 解决方案摘要

- 富文本：把会话上下文缺失收口为明确业务错误，同时让页面捕获加载/保存失败，避免未处理异常。
- 三方：把调用明细和统计的 `providerCode` 语义改成可选过滤，空值表示不筛选 provider。
- OSS：让开发环境的 MinIO CORS 默认值更宽松，并把稳定 `domainUrl`/生产白名单要求写清楚。

### 主要流程

- 打开富文本页时，页面尝试加载列表；若缺少 Client，上层展示业务错误并保持页面稳定。
- 打开调用明细/统计页时，页面直接加载最近数据；若输入了 `providerCode`，再按 provider 过滤。
- 本地开发环境中，前端 Origin 变化后无需手工改 IP 白名单；生产环境仍通过显式域名和白名单收口。

### 边界、失败与稳定错误行为

- 富文本缺少 Client 时必须是稳定的业务错误，不得再是泛化 500。
- 三方调用明细/统计缺少 `providerCode` 时不得失败。
- OSS 若使用生产环境显式白名单，仍应按最小权限收口；本次不改变生产私有/公共对象语义。

### 状态转换与不变量

- 富文本仍按当前用户 + Client 归属隔离。
- 三方调用明细和统计仍只读，不产生写口。
- OSS 仍保持预签名、私有对象和公共对象的既有生命周期不变量。

## 3. 用户故事

- **US-001**：作为富文本演示使用者，我希望页面在上下文不完整时给出明确错误，以便我知道是登录/Client 问题而不是页面崩溃。
- **US-002**：作为三方管理使用者，我希望直接打开调用明细和统计页时就能看到最近数据，以便我不用先补筛选条件。
- **US-003**：作为本地开发者，我希望前端 Origin 变化时不用反复修改 MinIO IP 白名单，以便联调更快。

## 4. 验收合同

| ID | 前置条件 | 动作或事件 | 可观察结果 | 验证接缝 |
|---|---|---|---|---|
| AC-001 | 当前会话缺少 Client 或用户上下文不完整 | 打开或保存富文本页 | 返回明确业务错误，前端不再出现未处理 promise rejection | 富文本页面 + `TestRichTextServiceImpl` |
| AC-002 | 调用明细/统计页未输入 providerCode | 直接进入页面 | 接口返回 200 并展示最近数据 | `ThirdObservabilityController` + 现有 Playwright 页面流 |
| AC-003 | 本地开发环境前端 Origin 改变 | 直接上传或预览 OSS 对象 | 不再要求每次手工修改 IP 绑定式 MinIO CORS 白名单 | release-artifacts 配置 + 浏览器预检 |

## 5. 范围

### IN

- 富文本服务的会话上下文校验和页面错误捕获。
- 三方调用明细/统计的空筛选语义。
- 开发环境 OSS CORS 默认值与说明文档。

### REUSE

- 现有富文本归属模型与 OSS 引用对账。
- 现有三方统计表和调用明细表。
- 现有 `domainUrl` / `endpoint` 分离语义。

### OUT

- **OOS-001**：不改登录域、Client 绑定或用户体系的基础设计。
- **OOS-002**：不做 Bucket policy 迁移或对象清理。
- **OOS-003**：不引入新的第三方 API。

## 6. 已锁定实现约束

- **DEC-001**：富文本仍按当前用户 + Client 做归属隔离。来源：`DIAG-2026-09-08-richtext-third-oss`。
- **DEC-002**：`providerCode` 为空时，调用明细/统计按“最近数据且不过滤 provider”解释。来源：`DIAG-2026-09-08-richtext-third-oss`。
- **DEC-003**：OSS 开发环境可以采用更宽松的 CORS 默认值；生产仍保持显式域名/白名单。来源：`DIAG-2026-09-08-richtext-third-oss`。

## 7. 数据、接口与兼容

- **公共接口变化：** 三方调用明细/统计的查询参数语义改为可选过滤；其余接口不变。
- **参数兼容细节：** controller、use case、service、DAO 和 Mapper 必须完整贯通可选参数；`null`、空串和全空白统一视为无过滤，有值先 `strip()` 后精确匹配。
- **数据模型与持久化：** 不改表结构。
- **兼容要求：** 旧的 providerCode 传参方式仍可用；富文本与 OSS 的既有数据仍有效。
- **迁移要求：** 无结构迁移。
- **发布或运维影响：** 仅更新开发环境 CORS 默认值与文档说明；生产由显式配置控制。

## 8. 非功能要求

- **NFR-001 安全与隐私：** 不放宽富文本和 OSS 的生产侧权限模型。
- **NFR-002 性能与容量：** 不引入额外查询风暴；空 providerCode 仅影响过滤条件。
- **NFR-003 可用性与可靠性：** 用户态错误必须可诊断，不得再抛未处理异常。
- **NFR-004 可观测性与运营：** 维持现有日志和错误编号机制，不记录密钥或完整签名 URL。

## 9. 验证策略

| 接缝 | 层级 | 覆盖合同 | 现有先例或命令 | Evidence 类型 |
|---|---|---|---|---|
| 富文本服务 | 后端单元 | AC-001 | `mvn -pl ruoyi-modules/ruoyi-demo -am -Dtest=TestRichTextServiceImplTest test` | Ticket Evidence |
| 富文本页面 | 浏览器/页面 | AC-001 | 新增 Playwright / smoke 场景 | Ticket Evidence |
| 三方观测接口 | 后端单元/契约 | AC-002 | `mvn -pl ruoyi-modules/ruoyi-third -am -Dtest=ThirdObservabilityControllerContractTest,ThirdObservabilityServiceTest test` | Ticket Evidence |
| 三方页面 | 浏览器回归 | AC-002 | `pnpm exec playwright test e2e/third-party-management.spec.ts` | Ticket Evidence |
| OSS 配置与文档 | 配置/运维 | AC-003 | `docker compose config --quiet` + 本地浏览器预检 | Ticket Evidence |

## 10. 风险、假设与未决问题

### 风险

- OSS 若把宽松 CORS 默认值误用于生产，会放大攻击面。
- 三方空筛选语义若解释不清，可能让运营误以为只查某个 provider。

### 已采用的低影响假设

- 开发环境更关注联调便利，生产环境继续由显式白名单控制。
- 调用明细/统计页空筛选默认展示最近数据，而不是报错。

### 未决问题

无。
